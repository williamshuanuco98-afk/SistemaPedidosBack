package com.inplabel.pedidos.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.io.File;
import java.nio.file.Files;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
public class FileStorageUtil {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @SuppressWarnings("unchecked")
    public String saveAttachedFiles(Object adjuntosObj, String storagePath, boolean useSubfolders, String folderName) {
        if (adjuntosObj == null) return "";

        String adjuntosJson = "";
        try {
            adjuntosJson = objectMapper.writeValueAsString(adjuntosObj);
        } catch (Exception ignored) {}

        if (adjuntosObj instanceof List) {
            List<Map<String, Object>> filesList = (List<Map<String, Object>>) adjuntosObj;
            for (Map<String, Object> f : filesList) {
                String fileName = (String) f.get("name");
                String base64Data = (String) f.get("data");

                if (fileName != null && base64Data != null && base64Data.contains(",")) {
                    try {
                        String base64Content = base64Data.substring(base64Data.indexOf(",") + 1);
                        byte[] decodedBytes = Base64.getDecoder().decode(base64Content);

                        File targetDir = new File(storagePath);
                        if (useSubfolders && folderName != null && !folderName.isEmpty()) {
                            targetDir = new File(targetDir, folderName);
                        }
                        if (!targetDir.exists()) {
                            targetDir.mkdirs();
                        }

                        File outFile = new File(targetDir, fileName);
                        Files.write(outFile.toPath(), decodedBytes);
                        f.put("saved_path", outFile.getAbsolutePath());
                    } catch (Exception ex) {
                        System.err.println("Error al guardar archivo en disco: " + ex.getMessage());
                    }
                }
            }
            try {
                return objectMapper.writeValueAsString(filesList);
            } catch (Exception e) {
                return adjuntosJson;
            }
        }
        return adjuntosJson;
    }
}
