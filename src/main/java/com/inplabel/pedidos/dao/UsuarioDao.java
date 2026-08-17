package com.inplabel.pedidos.dao;

import com.inplabel.pedidos.model.Usuario;
import java.util.List;
import java.util.Optional;

public interface UsuarioDao {
    Optional<Usuario> findByUsername(String username);
    List<Usuario> findAll();
    Usuario create(String username, String rawPassword, String nombreCompleto, String rol);
    boolean updatePassword(int idUsuario, String newRawPassword);
    void initDefaultUsers();
}
