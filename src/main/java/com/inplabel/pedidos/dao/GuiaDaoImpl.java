package com.inplabel.pedidos.dao;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class GuiaDaoImpl implements GuiaDao {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    @PostConstruct
    public void initSchema() {
        try {
            jdbcTemplate.execute("ALTER TABLE guias ADD COLUMN doc_referencia VARCHAR(255)");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE guias ADD COLUMN punto_partida TEXT");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE guias ADD COLUMN punto_llegada TEXT");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE guias ADD COLUMN observaciones TEXT");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE guias ADD COLUMN motivo_anulacion TEXT");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE guias ADD COLUMN establecimiento VARCHAR(100)");
        } catch (Exception ignored) {}
        try {
            jdbcTemplate.execute("ALTER TABLE guias ADD COLUMN storage_path VARCHAR(500)");
        } catch (Exception ignored) {}
    }

    @Override
    public List<Map<String, Object>> findAll() {
        List<Map<String, Object>> guias = jdbcTemplate.queryForList(
            "SELECT g.*, c.razon_social AS nombre_cliente, c.nro_documento, c.direccion AS direccion_destino, p.nro_pedido " +
            "FROM guias g LEFT JOIN cliente c ON g.id_cliente = c.id_cliente LEFT JOIN pedido p ON g.id_pedido = p.id_pedido ORDER BY g.id_guia DESC"
        );

        for (Map<String, Object> guia : guias) {
            Integer idGuia = (Integer) guia.get("id_guia");
            List<Map<String, Object>> detalles = jdbcTemplate.queryForList(
                "SELECT d.*, pr.nombre_producto, pr.codigo_producto FROM detalle_guias d " +
                "LEFT JOIN producto pr ON d.id_producto = pr.id_producto WHERE d.id_guia = ?",
                idGuia
            );
            guia.put("detalles", detalles);
        }

        return guias;
    }

    @Override
    public Map<String, String> getNextNumber(String serie) {
        String prefix = serie != null && serie.toUpperCase().startsWith("GR002") ? "GR002" : "GR001";
        String sql = "SELECT nro_guia FROM guias WHERE nro_guia LIKE ? ORDER BY id_guia DESC";
        List<String> list = jdbcTemplate.query(sql, (rs, rowNum) -> rs.getString("nro_guia"), prefix + "-%");
        
        int maxNum = 0;
        for (String lastStr : list) {
            if (lastStr != null) {
                try {
                    String[] parts = lastStr.split("-");
                    if (parts.length > 1) {
                        int num = Integer.parseInt(parts[1]);
                        if (num > maxNum) maxNum = num;
                    }
                } catch (Exception ignored) {}
            }
        }

        String nextNro = String.format("%s-%04d", prefix, maxNum + 1);
        Map<String, String> res = new HashMap<>();
        res.put("next_nro_guia", nextNro);
        res.put("serie", prefix);
        return res;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> save(Map<String, Object> body) {
        Number idClienteNum = (Number) body.get("id_cliente");
        int idCliente = idClienteNum != null ? idClienteNum.intValue() : 0;
        Number idPedidoNum = (Number) body.get("id_pedido");
        Integer idPedido = idPedidoNum != null ? idPedidoNum.intValue() : null;

        String fecha = (String) body.getOrDefault("fecha_guia", LocalDate.now().toString());
        String nroGuia = (String) body.get("nro_guia");
        String establecimiento = (String) body.getOrDefault("establecimiento", "CARABAYLLO");
        String seriePrefix = "CARABAYLLO".equalsIgnoreCase(establecimiento) ? "GR001" : "GR002";

        if (nroGuia == null || nroGuia.isEmpty()) {
            Map<String, String> nextRes = getNextNumber(seriePrefix);
            nroGuia = nextRes.get("next_nro_guia");
        }

        String estado = (String) body.getOrDefault("estado", "EMITIDA");
        String docRef = (String) body.getOrDefault("doc_referencia", "");
        String puntoPartida = (String) body.getOrDefault("punto_partida", "");
        String puntoLlegada = (String) body.getOrDefault("punto_llegada", "");
        String observaciones = (String) body.getOrDefault("observaciones", "");
        String storagePath = (String) body.getOrDefault("storage_path", "C:\\Users\\User\\OneDrive\\Escritorio\\GuiasI");

        List<Map<String, Object>> detalles = (List<Map<String, Object>>) body.get("detalles");

        final String finalNroGuia = nroGuia;
        final String finalDocRef = docRef;
        final String finalPuntoPartida = puntoPartida;
        final String finalPuntoLlegada = puntoLlegada;
        final String finalObs = observaciones;
        final String finalEstab = establecimiento;
        final String finalStorage = storagePath;

        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                "INSERT INTO guias (id_cliente, id_pedido, fecha_guia, nro_guia, estado, activo, doc_referencia, punto_partida, punto_llegada, observaciones, establecimiento, storage_path) " +
                "VALUES (?, ?, ?, ?, ?, TRUE, ?, ?, ?, ?, ?, ?)",
                Statement.RETURN_GENERATED_KEYS
            );
            ps.setInt(1, idCliente);
            if (idPedido != null) ps.setInt(2, idPedido); else ps.setNull(2, java.sql.Types.INTEGER);
            ps.setString(3, fecha);
            ps.setString(4, finalNroGuia);
            ps.setString(5, estado);
            ps.setString(6, finalDocRef);
            ps.setString(7, finalPuntoPartida);
            ps.setString(8, finalPuntoLlegada);
            ps.setString(9, finalObs);
            ps.setString(10, finalEstab);
            ps.setString(11, finalStorage);
            return ps;
        }, keyHolder);

        Number newIdNum = keyHolder.getKey();
        int newId = newIdNum != null ? newIdNum.intValue() : 0;

        if (detalles != null) {
            for (Map<String, Object> item : detalles) {
                Number pIdNum = (Number) item.get("id_producto");
                Number cantNum = (Number) item.get("cantidad");
                if (pIdNum != null) {
                    jdbcTemplate.update(
                        "INSERT INTO detalle_guias (id_guia, id_producto, cantidad) VALUES (?, ?, ?)",
                        newId, pIdNum.intValue(), cantNum != null ? cantNum.intValue() : 1
                    );
                }
            }
        }

        if (idPedido != null) {
            jdbcTemplate.update(
                "UPDATE pedido SET nro_guia = ?, fecha_entrega = ? WHERE id_pedido = ?",
                finalNroGuia, fecha, idPedido
            );
        }

        body.put("id_guia", newId);
        body.put("nro_guia", finalNroGuia);
        body.put("fecha_guia", fecha);
        body.put("estado", estado);
        return body;
    }

    @Override
    public Map<String, Object> anular(int id, String motivo) {
        jdbcTemplate.update(
            "UPDATE guias SET estado = 'ANULADA', motivo_anulacion = ? WHERE id_guia = ?",
            motivo, id
        );

        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("id_guia", id);
        res.put("estado", "ANULADA");
        res.put("motivo_anulacion", motivo);
        return res;
    }

    @Override
    public Map<String, Object> update(int id, Map<String, Object> body) {
        String nroGuia = (String) body.get("nro_guia");
        String estado = (String) body.getOrDefault("estado", "EMITIDA");
        String fecha = (String) body.getOrDefault("fecha_guia", LocalDate.now().toString());

        jdbcTemplate.update(
            "UPDATE guias SET nro_guia = ?, estado = ?, fecha_guia = ? WHERE id_guia = ?",
            nroGuia, estado, fecha, id
        );

        body.put("success", true);
        body.put("id_guia", id);
        return body;
    }
}
