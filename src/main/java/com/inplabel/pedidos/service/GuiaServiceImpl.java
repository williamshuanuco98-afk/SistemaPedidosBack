package com.inplabel.pedidos.service;

import com.inplabel.pedidos.dao.GuiaDao;
import com.inplabel.pedidos.util.GuiaPdfGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GuiaServiceImpl implements GuiaService {

    @Autowired
    private GuiaDao guiaDao;

    @Autowired
    private GuiaPdfGenerator pdfGenerator;

    @Override
    public List<Map<String, Object>> getGuias() {
        return guiaDao.findAll();
    }

    @Override
    public Map<String, Object> getGuiaById(int id) {
        return guiaDao.findById(id);
    }

    @Override
    public Map<String, String> getNextNumber(String serie) {
        return guiaDao.getNextNumber(serie);
    }

    @Override
    @Transactional
    public Map<String, Object> addGuia(Map<String, Object> body) {
        Map<String, Object> res = guiaDao.save(body);
        
        // Auto-generate and save PDF to configured path if provided
        try {
            Number idNum = (Number) res.get("id_guia");
            if (idNum != null) {
                int id = idNum.intValue();
                Map<String, Object> fullGuia = guiaDao.findById(id);
                if (fullGuia != null) {
                    String storageDir = (String) body.getOrDefault("storage_path", "C:\\Inplabel\\Guias");
                    boolean useSub = Boolean.parseBoolean(String.valueOf(body.getOrDefault("use_subfolders", "true")));
                    String savedFile = pdfGenerator.savePdfToDisk(fullGuia, storageDir, useSub);
                    if (savedFile != null) {
                        res.put("pdf_path", savedFile);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Advertencia al auto-generar PDF: " + e.getMessage());
        }

        return res;
    }

    @Override
    @Transactional
    public Map<String, Object> anularGuia(int id, Map<String, Object> body) {
        String motivo = (String) body.getOrDefault("motivo_anulacion", "Anulado por el usuario");
        return guiaDao.anular(id, motivo);
    }

    @Override
    @Transactional
    public Map<String, Object> updateGuia(int id, Map<String, Object> body) {
        return guiaDao.update(id, body);
    }

    @Override
    public byte[] generatePdf(int id, String storageDir, Boolean useSubfolders) {
        Map<String, Object> guia = guiaDao.findById(id);
        if (guia == null) {
            throw new RuntimeException("Guía no encontrada con ID: " + id);
        }
        byte[] bytes = pdfGenerator.generatePdfBytes(guia);

        // Optionally save to disk as well
        if (storageDir != null && !storageDir.trim().isEmpty()) {
            boolean useSub = useSubfolders != null ? useSubfolders : true;
            pdfGenerator.savePdfToDisk(guia, storageDir, useSub);
        }

        return bytes;
    }

    @Override
    public Map<String, Object> savePdf(int id, String storageDir, Boolean useSubfolders) {
        Map<String, Object> guia = guiaDao.findById(id);
        if (guia == null) {
            throw new RuntimeException("Guía no encontrada con ID: " + id);
        }
        boolean useSub = useSubfolders != null ? useSubfolders : true;
        String filePath = pdfGenerator.savePdfToDisk(guia, storageDir, useSub);

        Map<String, Object> res = new HashMap<>();
        res.put("success", filePath != null);
        res.put("filePath", filePath);
        res.put("nro_guia", guia.get("nro_guia"));
        return res;
    }
}
