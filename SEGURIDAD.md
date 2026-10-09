# Cambios de seguridad — 8 de octubre de 2026

## Reglas de negocio conservadas

- FINALIZADO sigue permitiendo cerrar un pedido con saldo pendiente y motivo. El permiso `pedidos.finish` permite ese cierre; no exige permiso de cancelación.
- Las guías emitidas y la referencia de guía escrita al registrar un envío continúan siendo procesos independientes. No se modificaron PedidoDaoImpl, EnvioPedidoDaoImpl ni GuiaDaoImpl.
- Durante el diagnóstico posterior del acceso se realizaron consultas de lectura y se reinició el servidor de esta carpeta. No se modificaron las credenciales de los usuarios.

## Protección implementada

- Sesión del servidor, cookie HttpOnly y SameSite=Strict; duración de inactividad de dos horas y renovación del identificador al entrar.
- `/api/auth/me` consulta al usuario autenticado; no acepta identidades elegidas mediante parámetros.
- Cierre de sesión en el servidor. Las sesiones dejan de funcionar si el usuario se desactiva o cambia su contraseña. Los permisos se consultan en cada solicitud.
- Denegación de acceso anónimo a la API. Roles y permisos se obtienen de la base; los encabezados X-User-Role/X-Username no autorizan operaciones.
- Autorización por módulo y por acción de pedido. Los anticipos se ocultan sin `pedidos.finances`. La administración de usuarios requiere `usuarios.manage` o rol administrador; la eliminación de clientes/productos se reserva al administrador. Las letras usan `pedidos.finances`; crear/modificar/anular guías usa `guias.create`.
- Se eliminó el login local con contraseñas fijas y el método que creaba cuentas predeterminadas. Un objeto guardado en localStorage ya no autentica la interfaz.
- Sin CORS abierto. Las modificaciones requieren un encabezado no simple y se rechazan solicitudes cross-site; frontend y API deben usar el mismo origen.
- Contraseñas nuevas con PBKDF2-HMAC-SHA256, 600.000 iteraciones, sal aleatoria y comparación de tiempo constante. Las SHA-256 antiguas se verifican y actualizan tras un login válido. Las contraseñas nuevas/cambiadas requieren 12–1024 caracteres.
- Los hashes y sales no se serializan al devolver usuarios.
- Límite separado de login: 20 peticiones/minuto por dirección de conexión, sin confiar en X-Forwarded-For enviado por clientes. Otras llamadas: 600/minuto. Detrás de un proxy, la dirección puede ser compartida; una política de proxy confiable debe configurarse antes de cambiar este criterio.
- Límite de cuerpo real: 8 KiB para login y 15 MiB para otras modificaciones, también sin Content-Length. Adjuntos: hasta 20, máximo 5 MiB por archivo y 10 MiB por pedido.
- Escritura bajo APP_STORAGE_ROOT; nombres de adjuntos generados y extensiones permitidas, rechazo de rutas de escape y enlaces simbólicos. Los directorios enviados por el navegador no controlan el destino. La lista de extensiones no sustituye un antivirus o análisis profundo del contenido.
- Descargas GET de PDF/Excel sin escritura en disco. Las operaciones de creación/guardado usan las carpetas autorizadas.
- Rutas públicas limitadas a la interfaz y sus recursos; no se expone toda la carpeta del repositorio frontend mediante el manejador de archivos externo.
- Errores de autorización, red o respuesta malformada en modificaciones ya no se convierten en un guardado local aparentemente exitoso. Se muestra error y se conserva la separación entre caché y confirmación del servidor.
- Los tokens de consultas RUC/DNI se envían exclusivamente a su proveedor; se desactivó seguir redirecciones automáticas con credenciales. DECOLECTA_API_TOKEN corresponde a Decolecta, SUNAT_API_TOKEN a apis.net.pe y APIPERU_API_TOKEN a apiperu.dev.
- Se retiró la contraseña de base de datos incorporada al archivo de configuración. No se cambió la contraseña real del proveedor ni el historial de Git.

## Arranque y configuración

Usar INICIAR_SISTEMA.bat. Abre el sistema servido por Spring Boot en http://localhost:8080, sin un segundo servidor frontend en el puerto 3000.

Variables:

| Variable | Uso |
|---|---|
| DB_PASSWORD o SPRING_DATASOURCE_PASSWORD | Contraseña de base de datos. Si falta en el entorno, el lanzador lee `.local/db-password.dpapi`, cifrado por Windows para el usuario local; si tampoco existe ese archivo, la solicita de forma oculta. No es la contraseña de admin. |
| APP_STORAGE_ROOT | Carpeta raíz autorizada en el servidor. Por defecto `./storage`, relativa al directorio de arranque del backend. Subcarpetas Pedidos, Guias, Letras y LetrasExcel. |
| SESSION_COOKIE_SECURE | Configurar `true` al servir por HTTPS. En HTTP local se mantiene `false`. |
| DB_URL / DB_USERNAME | Permiten sustituir servidor y usuario de base de datos ya configurados. |

No volver a introducir secretos en los archivos versionados. La credencial previamente incluida debe rotarse en el proveedor si todavía está activa. Las contraseñas actuales de usuarios no se cambian automáticamente: si aún se usan contraseñas de demostración, cambiarlas desde administración.

La interfaz de configuración permite conservar la preferencia de subcarpetas, pero muestra las rutas como configuración del servidor. Los documentos anteriores no se movieron ni se eliminaron. No se modificó el alcance por establecimiento: eso requiere una regla de negocio definida sobre quién debe ver cada sede.

## Qué significa un lote parcial

Ejemplo: se solicitan cinco letras, se insertan dos y la tercera falla. Antes, el controlador capturaba el error dentro del método transaccional sin marcar rollback; las primeras inserciones podían confirmarse aunque la respuesta fuera un error.

Ahora el error marca rollback: se revierte el lote en la base de datos. Esto no modifica la posibilidad de finalizar un pedido incompleto. La transacción SQL no revierte archivos ya escritos; una falla posterior a generar documentos puede dejar archivos huérfanos que no representan letras confirmadas. La prueba añadida comprueba específicamente la reversión SQL, sin escribir en una base real.

## Verificación

- Compilación Java con JDK 21.
- 13 pruebas JUnit de identidad, CSRF, permisos, revocación, migración de contraseña, rutas, límites, cierre FINALIZADO, anticipos y rollback del lote.
- 6 pruebas Node del frontend: rechazo HTTP, fallo de red, ausencia de login local, credenciales/encabezados correctos, caducidad y respuesta de guardado malformada.
- Revisión de sintaxis de JavaScript y de los scripts PowerShell modificados.

Las pruebas usan dobles de base de datos y solicitudes servlet simuladas; no equivalen a una prueba de producción. El ejecutor Maven/Surefire local falló por una clase faltante del plugin. Las pruebas Java se compilaron y ejecutaron directamente con JUnit Platform y las dependencias locales, usando el agente Mockito al inicio para evitar la conexión dinámica de depuración.

Pruebas guardadas en `src/test/java/com/inplabel/pedidos/security/SecurityRegressionTest.java` y `../SistemaWebPedidosFront/tests/security.test.mjs`. En una instalación Maven íntegra pueden ejecutarse con `mvn test`; las del frontend con `node --test tests/security.test.mjs` desde su carpeta. `run_security_audit.ps1` verifica rechazos anónimos contra un servidor ya iniciado; `verify_auth.ps1` pide credenciales y comprueba login/me/logout.

Referencias: [almacenamiento de contraseñas de OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html) y [protección CSRF para APIs de OWASP](https://cheatsheetseries.owasp.org/cheatsheets/Cross-Site_Request_Forgery_Prevention_Cheat_Sheet.html).
