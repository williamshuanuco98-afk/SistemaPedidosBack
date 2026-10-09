package com.inplabel.pedidos.security;

import com.inplabel.pedidos.controller.*;
import com.inplabel.pedidos.dao.UsuarioDao;
import com.inplabel.pedidos.model.Usuario;
import com.inplabel.pedidos.service.PedidoService;
import com.inplabel.pedidos.util.*;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.*;
import org.springframework.test.util.ReflectionTestUtils;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityRegressionTest {
    UsuarioDao users;
    Usuario user;
    ApiSecurityFilter filter;
    @BeforeEach void setup() {
        users = mock(UsuarioDao.class);
        user = new Usuario(); user.setIdUsuario(7); user.setActivo(true);
        user.setUsername("operador"); user.setRol("OPERACIONES");
        user.setPassword("version1"); user.setPermisos(List.of("pedidos.view", "pedidos.finish"));
        when(users.findById(7)).thenReturn(Optional.of(user));
        filter = new ApiSecurityFilter(users);
    }
    MockHttpServletRequest request(String method, String path, boolean authenticated) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, path);
        req.addHeader("X-Requested-With", "XMLHttpRequest");
        if (authenticated) {
            req.getSession().setAttribute(ApiSecurityFilter.USER_ID, 7);
            req.getSession().setAttribute(ApiSecurityFilter.PASSWORD_VERSION, "version1");
        }
        return req;
    }
    int status(MockHttpServletRequest req) throws Exception {
        MockHttpServletResponse res = new MockHttpServletResponse();
        filter.doFilter(req,res,(a,b)->((jakarta.servlet.http.HttpServletResponse)b).setStatus(204));
        return res.getStatus();
    }
    @Test void anonymousAndForgedRoleAreRejected() throws Exception {
        MockHttpServletRequest req = request("GET", "/api/usuarios", false);
        req.addHeader("X-User-Role", "ADMIN");
        assertEquals(401,status(req));
        req = request("DELETE", "/api/clientes/1", true);
        req.addHeader("X-User-Role", "ADMIN");
        assertEquals(403,status(req));
    }
    @Test void permissionIsCheckedOnServer() throws Exception {
        assertEquals(204,status(request("GET","/api/pedidos",true)));
        assertEquals(403,status(request("POST","/api/usuarios",true)));
        assertEquals(403,status(request("GET","/api/unknown",true)));
        user.setRol("ADMIN");
        assertEquals(204,status(request("POST","/api/usuarios",true)));
    }
    @Test void csrfAndCrossSiteAreRejected() throws Exception {
        MockHttpServletRequest req = request("POST","/api/auth/login",false);
        req.removeHeader("X-Requested-With"); assertEquals(403,status(req));
        req=request("POST","/api/auth/login",false);
        req.addHeader("Sec-Fetch-Site","cross-site"); assertEquals(403,status(req));
    }
    @Test void disabledUserAndPasswordChangeRevokeSession() throws Exception {
        user.setActivo(false); assertEquals(401,status(request("GET","/api/pedidos",true)));
        user.setActivo(true); user.setPassword("changed");
        assertEquals(401,status(request("GET","/api/pedidos",true)));
    }
    @Test void loginRotatesSessionAndMeUsesServerIdentity() {
        AuthController auth = new AuthController(); ReflectionTestUtils.setField(auth,"usuarioDao",users);
        String salt=PasswordUtil.generateSalt(); user.setSalt(salt);
        user.setPassword(PasswordUtil.hashPassword("correct-password",salt));
        when(users.findByUsername("operador")).thenReturn(Optional.of(user));
        AuthController.LoginRequest credentials=new AuthController.LoginRequest();
        credentials.setUsername("operador"); credentials.setPassword("wrong-password");
        assertEquals(401,auth.login(credentials,request("POST","/api/auth/login",false)).getStatusCode().value());
        credentials.setPassword("correct-password");
        MockHttpServletRequest req=request("POST","/api/auth/login",true);
        String old=req.getSession().getId();
        assertEquals(200,auth.login(credentials,req).getStatusCode().value());
        assertNotEquals(old,req.getSession().getId());
        assertEquals(7,req.getSession().getAttribute(ApiSecurityFilter.USER_ID));
        req.setAttribute(ApiSecurityFilter.CURRENT_USER,user); req.addParameter("username","admin");
        assertEquals("operador",((Map<?,?>)auth.getCurrentUser(req).getBody()).get("username"));
        auth.logout(req); assertNull(req.getSession(false));
    }
    @Test void legacyPasswordsRemainVerifiable() throws Exception {
        String salt=PasswordUtil.generateSalt();
        java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
        digest.update(Base64.getDecoder().decode(salt));
        String old=Base64.getEncoder().encodeToString(digest.digest("clave-antigua".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        assertTrue(PasswordUtil.verifyPassword("clave-antigua",salt,old));
        assertTrue(PasswordUtil.needsUpgrade(old));
        String upgraded=PasswordUtil.hashPassword("clave-antigua",salt);
        assertFalse(PasswordUtil.needsUpgrade(upgraded));
        assertTrue(PasswordUtil.verifyPassword("clave-antigua",salt,upgraded));
        assertFalse(PasswordUtil.verifyPassword("otra",salt,upgraded));
        assertFalse(PasswordUtil.verifyPassword("otra","invalid",upgraded));
    }
    @Test void closingWithOutstandingBalanceIsPreserved() {
        PedidoService service=mock(PedidoService.class);
        PedidoController controller=new PedidoController(); ReflectionTestUtils.setField(controller,"pedidoService",service);
        MockHttpServletRequest req=request("PUT","/api/pedidos/9",true);
        req.setAttribute(ApiSecurityFilter.CURRENT_USER,user);
        Map<String,Object> payload=new HashMap<>(Map.of("estado","FINALIZADO","nro_guia","REFERENCIA-MANUAL","motivo_cancelacion","Saldo cerrado por acuerdo","fecha_entrega","2026-10-08"));
        when(service.updatePedido(9,payload)).thenReturn(payload);
        assertEquals("FINALIZADO",controller.updatePedido(9,payload,req).get("estado"));
        verify(service).updatePedido(9,payload);
        payload.put("adelantos",List.of());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->controller.updatePedido(9,payload,req));
    }
    @Test void storageRejectsTraversalAndIgnoresClientDirectory(@TempDir Path root) throws Exception {
        SecureStorage storage=new SecureStorage(root.toString());
        assertThrows(IllegalArgumentException.class,()->storage.write("Pedidos","../../escape","x.txt",new byte[0]));
        assertThrows(IllegalArgumentException.class,()->storage.write("Pedidos",null,"../x.txt",new byte[0]));
        FileStorageUtil attachments=new FileStorageUtil(storage);
        String saved=attachments.saveAttachedFiles(List.of(Map.of("name","nota.txt","data","data:text/plain;base64,aG9sYQ==")),"C:/untrusted",false,"PED-1");
        assertFalse(saved.contains("untrusted"));
        assertEquals(1,Files.list(root.resolve("Pedidos")).count());
        assertThrows(IllegalArgumentException.class,()->attachments.saveAttachedFiles(List.of(Map.of("name","../x.txt","data","data:text/plain;base64,aA==")),"ignored",false,"PED-1"));
        assertThrows(IllegalArgumentException.class,()->attachments.saveAttachedFiles(List.of(Map.of("name","evil.html","data","data:text/html;base64,aA==")),"ignored",false,"PED-1"));
    }
    @Test void oversizedLoginBodyIsRejected() throws Exception {
        RequestSizeFilter sizes=new RequestSizeFilter();
        MockHttpServletRequest req=request("POST","/api/auth/login",false);req.setContent(new byte[8193]);
        MockHttpServletResponse res=new MockHttpServletResponse();FilterChain chain=mock(FilterChain.class);
        sizes.doFilter(req,res,chain);assertEquals(413,res.getStatus());verifyNoInteractions(chain);
    }
    @Test void loginRateLimitIgnoresForgedForwardedIp() throws Exception {
        RateLimitingFilter limiter=new RateLimitingFilter();
        for(int i=0;i<21;i++) {
            MockHttpServletRequest req=request("POST","/api/auth/login",false);
            req.addHeader("X-Forwarded-For","10.0.0."+i);
            MockHttpServletResponse res=new MockHttpServletResponse();
            limiter.doFilter(req,res,(a,b)->{});
            if(i==20)assertEquals(429,res.getStatus());
        }
    }

    @Test void failedBatchRollsBackEarlierInserts() {
        var jdbc=mock(org.springframework.jdbc.core.JdbcTemplate.class);
        var controller=new LetraCambioController(); ReflectionTestUtils.setField(controller,"jdbcTemplate",jdbc);
        var transactions=new org.springframework.transaction.support.AbstractPlatformTransactionManager() {
            int commits=0, rollbacks=0;
            protected Object doGetTransaction(){return new Object();}
            protected void doBegin(Object tx,org.springframework.transaction.TransactionDefinition definition){}
            protected void doCommit(org.springframework.transaction.support.DefaultTransactionStatus status){commits++;}
            protected void doRollback(org.springframework.transaction.support.DefaultTransactionStatus status){rollbacks++;}
        };
        var source=new org.springframework.transaction.interceptor.MatchAlwaysTransactionAttributeSource();
        source.setTransactionAttribute(new org.springframework.transaction.interceptor.DefaultTransactionAttribute());
        var factory=new org.springframework.aop.framework.ProxyFactory(controller);
        factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(transactions,source));
        var transactional=(LetraCambioController)factory.getProxy();
        Map<String,Object> valid=new HashMap<>(Map.of("numero_correlativo",1));
        Map<String,Object> invalid=new HashMap<>(Map.of("numero_correlativo","invalid"));
        var result=transactional.createLetrasBatch(Map.of("id_lote","TEST","letras",List.of(valid,invalid)),null,false);
        assertEquals(500,result.getStatusCode().value());
        verify(jdbc,times(1)).update(any(org.springframework.jdbc.core.PreparedStatementCreator.class),any(org.springframework.jdbc.support.KeyHolder.class));
        assertEquals(0,transactions.commits); assertEquals(1,transactions.rollbacks);
    }

    @Test void paymentsAreHiddenWithoutFinancialPermission() {
        var service=mock(PedidoService.class);
        var controller=new PedidoController();ReflectionTestUtils.setField(controller,"pedidoService",service);
        var req=request("GET","/api/pedidos",true);req.setAttribute(ApiSecurityFilter.CURRENT_USER,user);
        when(service.getPedidos()).thenReturn(List.of(Map.of("id_pedido",1,"adelantos",List.of(100))));
        assertFalse(controller.getPedidos(req).get(0).containsKey("adelantos"));
        user.setRol("ADMIN"); assertTrue(controller.getPedidos(req).get(0).containsKey("adelantos"));
    }

    @Test void providerTokensAreNeverSharedAcrossHosts() {
        var client=new SunatClientUtil();
        ReflectionTestUtils.setField(client,"decolectaToken","decolecta-only");
        ReflectionTestUtils.setField(client,"sunatToken","apis-only");
        assertEquals("decolecta-only",ReflectionTestUtils.invokeMethod(client,"tokenForHost","api.decolecta.com"));
        assertEquals("apis-only",ReflectionTestUtils.invokeMethod(client,"tokenForHost","api.apis.net.pe"));
        assertEquals("",ReflectionTestUtils.invokeMethod(client,"tokenForHost","apiperu.dev"));
        assertEquals("",ReflectionTestUtils.invokeMethod(client,"tokenForHost","other.example"));
    }
}
