package com.inplabel.pedidos.dao;

import com.inplabel.pedidos.model.Usuario;
import java.util.List;
import java.util.Optional;

public interface UsuarioDao {
    Optional<Usuario> findByUsername(String username);
    Optional<Usuario> findById(int idUsuario);
    List<Usuario> findAll();
    Usuario create(String username, String rawPassword, String nombreCompleto, String rol);
    Usuario createWithPermissions(String username, String rawPassword, String nombreCompleto, String rol, List<String> permisos);
    boolean updateUser(int idUsuario, String username, String nombreCompleto, String rol, Boolean activo, List<String> permisos, String newPassword);
    boolean toggleActive(int idUsuario);
    boolean updatePassword(int idUsuario, String newRawPassword);
    void initDefaultUsers();
}
