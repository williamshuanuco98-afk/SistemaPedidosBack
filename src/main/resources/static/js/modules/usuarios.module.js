import { api } from '../api.js';
import { escapeHtml, showBootstrapModal } from '../helpers.js';

const state = {
  usuariosList: [],
  filteredUsuarios: []
};

export async function initUsuariosView() {
  await loadUsuarios();
}

async function loadUsuarios() {
  state.usuariosList = await api.getUsers();
  state.filteredUsuarios = [...state.usuariosList];
  renderUsuariosTable();
}

export function filterUsuarios(query) {
  const q = (query || '').trim().toLowerCase();
  if (!q) {
    state.filteredUsuarios = [...state.usuariosList];
  } else {
    state.filteredUsuarios = state.usuariosList.filter(u => 
      (u.username || '').toLowerCase().includes(q) ||
      (u.nombreCompleto || '').toLowerCase().includes(q) ||
      (u.rol || '').toLowerCase().includes(q)
    );
  }
  renderUsuariosTable();
}

function renderUsuariosTable() {
  const tbody = document.getElementById('usuariosTableBody');
  if (!tbody) return;

  if (state.filteredUsuarios.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted py-4">No se encontraron usuarios registrados.</td></tr>`;
    return;
  }

  tbody.innerHTML = state.filteredUsuarios.map(u => {
    const isActivo = u.activo !== false;
    const badgeStatus = isActivo 
      ? `<span class="badge bg-success-subtle text-success border border-success-subtle px-2 py-1 fs-8">ACTIVO</span>`
      : `<span class="badge bg-danger-subtle text-danger border border-danger-subtle px-2 py-1 fs-8">INACTIVO</span>`;

    const perms = u.permisos || [];
    const permsSummary = perms.length === 14 
      ? `<span class="badge bg-primary-subtle text-primary border border-primary-subtle fs-8">Acceso Total (14/14)</span>`
      : perms.length > 0 
        ? `<span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle fs-8">${perms.length} Permisos Específicos</span>`
        : `<span class="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle fs-8">Sin Permisos</span>`;

    const activeBtnClass = isActivo ? 'btn-outline-danger' : 'btn-outline-success';
    const activeBtnIcon = isActivo ? 'bi-person-x-fill' : 'bi-person-check-fill';
    const activeBtnTitle = isActivo ? 'Desactivar Acceso' : 'Activar Acceso';

    return `
      <tr>
        <td class="fw-bold text-primary fs-7"><i class="bi bi-person-circle me-1"></i> ${escapeHtml(u.username)}</td>
        <td class="fw-semibold text-body fs-7">${escapeHtml(u.nombreCompleto || u.username)}</td>
        <td><span class="badge bg-dark-subtle text-dark border px-2 fs-8">${escapeHtml(u.rol || 'OPERADOR')}</span></td>
        <td>${permsSummary}</td>
        <td class="text-center">${badgeStatus}</td>
        <td class="text-center">
          <div class="d-flex justify-content-center gap-1">
            <button class="btn btn-outline-primary btn-sm py-0 px-2 fs-7" title="Editar Usuario y Permisos" onclick="usuariosModule.editUsuario(${u.idUsuario})">
              <i class="bi bi-pencil-square"></i>
            </button>
            <button class="btn ${activeBtnClass} btn-sm py-0 px-2 fs-7" title="${activeBtnTitle}" onclick="usuariosModule.toggleActive(${u.idUsuario})">
              <i class="bi ${activeBtnIcon}"></i>
            </button>
          </div>
        </td>
      </tr>
    `;
  }).join('');
}

export function openNewUserModal() {
  document.getElementById('usuarioIdInput').value = '';
  document.getElementById('usuarioUsernameInput').value = '';
  document.getElementById('usuarioUsernameInput').readOnly = false;
  document.getElementById('usuarioPasswordInput').value = '';
  document.getElementById('usuarioPasswordInput').required = true;
  document.getElementById('usuarioPasswordHint').innerText = '(Requerida para nuevos)';
  document.getElementById('usuarioNombreInput').value = '';
  document.getElementById('usuarioRolSelect').value = 'OPERADOR';
  document.getElementById('modalUsuarioTitle').innerHTML = `<i class="bi bi-person-plus-fill me-1"></i> Registrar Nuevo Usuario`;

  selectAllPerms(false);
  // Default minimal perms for new operator
  const viewCb = document.getElementById('perm_pedidos_view');
  if (viewCb) viewCb.checked = true;

  showBootstrapModal('modalUsuario');
}

export function editUsuario(id) {
  const u = state.usuariosList.find(x => x.idUsuario === id);
  if (!u) return;

  document.getElementById('usuarioIdInput').value = u.idUsuario;
  document.getElementById('usuarioUsernameInput').value = u.username;
  document.getElementById('usuarioUsernameInput').readOnly = true;
  document.getElementById('usuarioPasswordInput').value = '';
  document.getElementById('usuarioPasswordInput').required = false;
  document.getElementById('usuarioPasswordHint').innerText = '(Dejar en blanco para mantener la actual)';
  document.getElementById('usuarioNombreInput').value = u.nombreCompleto || '';
  document.getElementById('usuarioRolSelect').value = u.rol || 'OPERADOR';
  document.getElementById('modalUsuarioTitle').innerHTML = `<i class="bi bi-person-gear me-1"></i> Editar Usuario: <b>${escapeHtml(u.username)}</b>`;

  // Set checkboxes from u.permisos
  const permsSet = new Set(u.permisos || []);
  document.querySelectorAll('.perm-cb').forEach(cb => {
    cb.checked = permsSet.has(cb.value);
  });

  showBootstrapModal('modalUsuario');
}

export function selectAllPerms(check) {
  document.querySelectorAll('.perm-cb').forEach(cb => {
    cb.checked = check;
  });
}

export function onRolPresetChange(rol) {
  if (rol === 'ADMIN') {
    selectAllPerms(true);
  } else if (rol === 'VENTAS') {
    selectAllPerms(false);
    ['pedidos.view', 'pedidos.create', 'pedidos.edit', 'pedidos.finances', 'clientes.manage'].forEach(p => setPermChecked(p, true));
  } else if (rol === 'PRODUCCION') {
    selectAllPerms(false);
    ['produccion.view', 'productos.manage'].forEach(p => setPermChecked(p, true));
  } else if (rol === 'ALMACEN') {
    selectAllPerms(false);
    ['envios.create', 'envios.view', 'guias.create', 'guias.view'].forEach(p => setPermChecked(p, true));
  }
}

function setPermChecked(permKey, checked) {
  const cb = document.querySelector(`.perm-cb[value="${permKey}"]`);
  if (cb) cb.checked = checked;
}

export async function saveUsuario() {
  const idStr = document.getElementById('usuarioIdInput').value;
  const username = document.getElementById('usuarioUsernameInput').value.trim();
  const password = document.getElementById('usuarioPasswordInput').value;
  const nombreCompleto = document.getElementById('usuarioNombreInput').value.trim();
  const rol = document.getElementById('usuarioRolSelect').value;

  if (!username || (!idStr && !password) || !nombreCompleto) {
    alert('Por favor complete los campos requeridos (*).');
    return;
  }

  // Collect checked permissions
  const permisos = [];
  document.querySelectorAll('.perm-cb:checked').forEach(cb => {
    permisos.push(cb.value);
  });

  const payload = {
    username,
    password: password || null,
    nombreCompleto,
    rol,
    activo: true,
    permisos
  };

  let res;
  if (idStr) {
    res = await api.updateUser(parseInt(idStr, 10), payload);
  } else {
    res = await api.createUser(payload);
  }

  if (res && (res.idUsuario || res.message?.includes('correctamente'))) {
    alert('Usuario guardado exitosamente.');
    const modalEl = document.getElementById('modalUsuario');
    if (modalEl) {
      const modal = bootstrap.Modal.getInstance(modalEl);
      if (modal) modal.hide();
    }
    await loadUsuarios();
  } else {
    alert(res?.message || 'Error al guardar usuario.');
  }
}

export async function toggleActive(id) {
  const u = state.usuariosList.find(x => x.idUsuario === id);
  if (!u) return;

  const actionStr = u.activo !== false ? 'desactivar' : 'activar';
  if (!confirm(`¿Está seguro de que desea ${actionStr} el acceso del usuario "${u.username}"?`)) {
    return;
  }

  const res = await api.toggleUserActive(id);
  if (res && res.message) {
    await loadUsuarios();
  } else {
    alert('Error al cambiar estado del usuario.');
  }
}