package com.inplabel.pedidos.util;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import java.util.*;

@Component
public class FileStorageUtil {
    private final SecureStorage storage;
    private final ObjectMapper mapper = new ObjectMapper();
    public FileStorageUtil(SecureStorage storage) { this.storage = storage; }
    @SuppressWarnings("unchecked")
    public String saveAttachedFiles(Object attachments, String ignoredClientPath, boolean subfolders, String folder) {
        if (attachments == null) return "";
        if (!(attachments instanceof List<?> list) || list.size() > 20) throw new IllegalArgumentException("Adjuntos inválidos");
        List<Map<String,Object>> validated = new ArrayList<>();
        List<byte[]> contents = new ArrayList<>();
        long total = 0;
        for (Object item : list) {
            if (!(item instanceof Map<?,?>)) throw new IllegalArgumentException("Adjunto inválido");
            Map<String,Object> file = new HashMap<>((Map<String,Object>) item);
            String name = String.valueOf(file.getOrDefault("name", ""));
            String data = String.valueOf(file.getOrDefault("data", ""));
            if (name.contains("/") || name.contains("\\") || name.contains(":") || name.contains(".."))
                throw new IllegalArgumentException("Nombre de adjunto no permitido");
            String ext = name.contains(".") ? name.substring(name.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT) : "";
            if (!Set.of("pdf", "png", "jpg", "jpeg", "doc", "docx", "xls", "xlsx", "csv", "txt").contains(ext))
                throw new IllegalArgumentException("Tipo de adjunto no permitido");
            if (!data.startsWith("data:") || !data.contains(";base64,") || data.length() > 7_000_000)
                throw new IllegalArgumentException("Adjunto inválido o mayor de 5 MB");
            byte[] bytes = Base64.getDecoder().decode(data.substring(data.indexOf(',') + 1));
            total += bytes.length;
            if (bytes.length > 5 * 1024 * 1024 || total > 10 * 1024 * 1024)
                throw new IllegalArgumentException("Máximo 5 MB por archivo y 10 MB por pedido");
            file.remove("saved_path");
            file.put("stored_name", UUID.randomUUID() + "." + ext);
            validated.add(file); contents.add(bytes);
        }
        try {
            for (int i=0; i<validated.size(); i++) {
                Map<String,Object> file=validated.get(i);
                file.put("saved_path", storage.write("Pedidos", subfolders ? folder : null,
                    (String) file.get("stored_name"), contents.get(i)).toString());
            }
            return mapper.writeValueAsString(validated);
        } catch (java.io.IOException e) { throw new IllegalStateException("No se pudieron guardar los adjuntos", e); }
    }
}
