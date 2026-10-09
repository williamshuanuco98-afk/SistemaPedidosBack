package com.inplabel.pedidos.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.nio.file.*;
import java.time.LocalDate;

/** All disk writes are rooted in a server-controlled directory. */
@Component
public class SecureStorage {
    private final Path root;
    public SecureStorage(@Value("${app.storage.root:./storage}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }
    public synchronized Path write(String category, String folder, String filename, byte[] bytes) throws IOException {
        if (!category.matches("[A-Za-z]+") || !filename.matches("[A-Za-z0-9_.-]+") || filename.contains(".."))
            throw new IllegalArgumentException("Nombre de archivo no permitido");
        Files.createDirectories(root);
        Path base = root.toRealPath();
        Path directory = base.resolve(category);
        if (folder != null && !folder.isBlank()) {
            if (!folder.matches("[A-Za-z0-9_/-]+") || folder.contains(".."))
                throw new IllegalArgumentException("Carpeta no permitida");
            directory = directory.resolve(folder);
        }
        directory = directory.normalize();
        if (!directory.startsWith(base)) throw new IllegalArgumentException("Ruta no permitida");
        Path current = base;
        for (Path segment : base.relativize(directory)) {
            current = current.resolve(segment);
            if (Files.isSymbolicLink(current)) throw new IOException("Enlaces no permitidos");
            Files.createDirectories(current);
            if (!current.toRealPath().startsWith(base)) throw new IOException("Ruta fuera de almacenamiento");
        }
        Path target = directory.resolve(filename);
        if (Files.isSymbolicLink(target)) throw new IOException("Enlaces no permitidos");
        Files.write(target, bytes, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, LinkOption.NOFOLLOW_LINKS);
        return target;
    }
    public String saveDocument(String category, String filename, boolean subfolders, byte[] bytes) throws IOException {
        LocalDate today = LocalDate.now();
        String folder = subfolders ? today.getYear() + "/" + String.format("%02d", today.getMonthValue()) : null;
        return write(category, folder, filename, bytes).toString();
    }
}
