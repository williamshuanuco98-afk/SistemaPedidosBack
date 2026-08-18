import { escapeHtml, filterAndRankItems, showConfirmModal } from '../helpers.js';
import { api } from '../api.js';
import { canDelete } from './auth.module.js';

let currentClients = [];
let lastSunatQueryRuc = '';
let editingClientIndex = -1;

export function renderClientesTable(clients = [], searchQuery = '') {
  currentClients = clients || [];
  const searchInput = document.getElementById('searchClientesInput');
  if (searchInput && searchQuery) searchInput.value = searchQuery;
  filterClientes(searchQuery || (searchInput ? searchInput.value : ''));
}

export function filterClientes(queryStr = '') {
  const tbody = document.getElementById('clientesTableBody');
  if (!tbody) return;
  tbody.innerHTML = '';

  const filtered = filterAndRankItems(
    currentClients, 
    queryStr, 
    c => `${c.nro_documento || ''} ${c.nombre_cliente || ''} ${c.direccion || ''}`
  );

  const countBadge = document.getElementById('clientesCountBadge');
  if (countBadge) countBadge.textContent = `${filtered.length} ${filtered.length === 1 ? 'cliente' : 'clientes'}`;

  const allowDelete = canDelete();

  if (filtered.length === 0) {
    tbody.innerHTML = `<tr><td colspan="5" class="text-center text-muted py-4">No se encontraron clientes coincidentes.</td></tr>`;
    return;
  }

  filtered.forEach((c, idx) => {
    const tr = document.createElement('tr');
    const badgeBg = (c.tipo_documento === 'DNI') ? 'bg-info text-dark' : 'bg-primary';
    const clientId = c.id_cliente || c.id;
    tr.innerHTML = `
      <td class="fw-bold text-white">
        <span class="badge ${badgeBg} me-1 fs-8">${escapeHtml(c.tipo_documento || 'RUC')}</span>
        <span>${escapeHtml(c.nro_documento || 'S/D')}</span>
      </td>
      <td class="fw-semibold text-white">${escapeHtml(c.nombre_cliente || '-')}</td>
      <td class="small text-white-50">${escapeHtml(c.direccion || 'No especificada')}</td>
      <!-- 1. Columna EDITAR -->
      <td class="text-center">
        <button type="button" class="btn-action-solid btn-edit" onclick="clientesModule.openEditClientModal(${idx})" title="Modificar Cliente">
          <i class="bi bi-pencil-square text-dark"></i>
        </button>
      </td>
      <!-- 2. Columna ELIMINAR -->
      <td class="text-center">
        ${allowDelete ? `
          <button type="button" class="btn-action-solid btn-delete" onclick="clientesModule.deleteClient(${clientId})" title="Eliminar Cliente">
            <i class="bi bi-trash-fill"></i>
          </button>
        ` : `
          <button type="button" class="btn-action-solid btn-delete" style="opacity: 0.25; cursor: not-allowed;" title="Permiso restringido (Solo Administrador)" disabled>
            <i class="bi bi-lock-fill"></i>
          </button>
        `}
      </td>
    `;
    tbody.appendChild(tr);
  });

  // Re-initialize Bootstrap tooltips with 1-second delay
  if (typeof bootstrap !== 'undefined' && bootstrap.Tooltip) {
    const tooltipTriggers = tbody.querySelectorAll('[data-bs-toggle="tooltip"]');
    tooltipTriggers.forEach(el => new bootstrap.Tooltip(el, { delay: { show: 1000, hide: 100 } }));
  }
}

export function openNewClientModal() {
  editingClientIndex = -1;
  const modalElem = document.getElementById('modalCliente');
  if (!modalElem) return;

  const modalTitle = document.getElementById('modalClienteTitle');
  if (modalTitle) {
    modalTitle.innerHTML = '<i class="bi bi-person-plus me-2 text-primary"></i>Registrar Cliente';
  }

  const idInput = document.getElementById('modalClienteId');
  if (idInput) idInput.value = '';

  const tipoSelect = document.getElementById('modalClienteTipoDoc');
  if (tipoSelect) tipoSelect.value = 'RUC';

  document.getElementById('modalClienteNroDoc').value = '';
  document.getElementById('modalClienteRazonSocial').value = '';
  document.getElementById('modalClienteDireccion').value = '';
  
  onTipoDocChange('RUC');

  lastSunatQueryRuc = '';
  const modal = new bootstrap.Modal(modalElem);
  modal.show();
}

export function openEditClientModal(filteredIndex) {
  const queryStr = document.getElementById('searchClientesInput')?.value || '';
  const filtered = filterAndRankItems(
    currentClients, 
    queryStr, 
    c => `${c.nro_documento || ''} ${c.nombre_cliente || ''} ${c.direccion || ''}`
  );

  const client = filtered[filteredIndex];
  if (!client) return;

  editingClientIndex = currentClients.indexOf(client);

  const modalElem = document.getElementById('modalCliente');
  if (!modalElem) return;

  const modalTitle = document.getElementById('modalClienteTitle');
  if (modalTitle) {
    modalTitle.innerHTML = '<i class="bi bi-pencil-square me-2 text-warning"></i>Modificar Cliente';
  }

  const idInput = document.getElementById('modalClienteId');
  if (idInput) idInput.value = client.id_cliente || client.id || '';

  const tipoSelect = document.getElementById('modalClienteTipoDoc');
  if (tipoSelect) tipoSelect.value = client.tipo_documento || 'RUC';

  document.getElementById('modalClienteNroDoc').value = client.nro_documento || '';
  document.getElementById('modalClienteRazonSocial').value = client.nombre_cliente || '';
  document.getElementById('modalClienteDireccion').value = client.direccion || '';

  onTipoDocChange(client.tipo_documento || 'RUC');

  lastSunatQueryRuc = client.nro_documento || '';
  const modal = new bootstrap.Modal(modalElem);
  modal.show();
}

export async function deleteClient(clientId) {
  if (!canDelete()) {
    await showConfirmModal({
      title: 'Acceso Restringido',
      message: 'El perfil de Operaciones no tiene permisos para eliminar clientes.',
      icon: 'bi-shield-lock-fill',
      iconBg: 'rgba(234, 179, 8, 0.15)',
      iconColor: '#eab308',
      confirmText: 'Entendido',
      confirmBtnClass: 'btn-warning text-dark',
      cancelText: 'Cerrar'
    });
    return;
  }
  if (!clientId) return;
  const client = currentClients.find(c => (c.id_cliente || c.id) === clientId);
  const clientName = client ? client.nombre_cliente : `ID #${clientId}`;

  const confirmed = await showConfirmModal({
    title: '¿Eliminar Cliente?',
    message: `¿Está seguro de eliminar al cliente "${clientName}" de la base de datos MySQL?`,
    icon: 'bi-trash3-fill',
    iconBg: 'rgba(239, 68, 68, 0.15)',
    iconColor: '#ef4444',
    confirmText: 'Eliminar Cliente',
    confirmBtnClass: 'btn-danger',
    cancelText: 'Cancelar'
  });

  if (!confirmed) return;

  const res = await api.deleteCliente(clientId);
  if (res && res.success !== false) {
    // Re-fetch fresh clients from MySQL database
    currentClients = await api.getClientes();
    if (window.app) window.app.clients = [...currentClients];
    filterClientes();
  }
}

export function onTipoDocChange(tipo) {
  const nroInput = document.getElementById('modalClienteNroDoc');
  const feedback = document.getElementById('sunatStatusFeedback');
  const btn = document.getElementById('btnConsultarSunat');

  if (tipo === 'DNI') {
    if (nroInput) {
      nroInput.placeholder = 'Ingrese 8 dígitos de DNI';
      nroInput.maxLength = 8;
    }
    if (btn) btn.innerHTML = '<i class="bi bi-search me-1"></i> DNI';
    if (feedback) {
      feedback.className = 'form-text mt-1 text-muted fs-8';
      feedback.innerHTML = 'Escriba 8 dígitos para consultar RENIEC / DNI automáticamente.';
    }
  } else {
    if (nroInput) {
      nroInput.placeholder = 'Ingrese 11 dígitos de RUC';
      nroInput.maxLength = 11;
    }
    if (btn) btn.innerHTML = '<i class="bi bi-search me-1"></i> SUNAT';
    if (feedback) {
      feedback.className = 'form-text mt-1 text-muted fs-8';
      feedback.innerHTML = 'Escriba 11 dígitos para consultar SUNAT automáticamente.';
    }
  }
}

export function onNroDocInput(val) {
  const cleanVal = (val || '').trim();
  const tipoDoc = document.getElementById('modalClienteTipoDoc')?.value || 'RUC';

  if (tipoDoc === 'RUC' && cleanVal.length === 11 && cleanVal !== lastSunatQueryRuc) {
    consultarSunat(cleanVal);
  } else if (tipoDoc === 'DNI' && cleanVal.length === 8 && cleanVal !== lastSunatQueryRuc) {
    consultarDni(cleanVal);
  }
}

export function consultarSunatManual() {
  const ruc = document.getElementById('modalClienteNroDoc')?.value.trim();
  const tipoDoc = document.getElementById('modalClienteTipoDoc')?.value || 'RUC';

  if (tipoDoc === 'DNI') {
    if (ruc && ruc.length === 8) {
      consultarDni(ruc);
    } else {
      alert('Ingrese un número de DNI válido de 8 dígitos.');
    }
  } else {
    if (ruc && ruc.length === 11) {
      consultarSunat(ruc);
    } else {
      alert('Ingrese un número de RUC válido de 11 dígitos.');
    }
  }
}

export async function consultarSunat(ruc) {
  lastSunatQueryRuc = ruc;
  const feedback = document.getElementById('sunatStatusFeedback');
  const btn = document.getElementById('btnConsultarSunat');

  if (feedback) {
    feedback.className = 'form-text mt-1 text-warning fw-semibold fs-8';
    feedback.innerHTML = '<i class="bi bi-arrow-repeat spin me-1"></i> Consultando SUNAT en tiempo real...';
  }
  if (btn) btn.disabled = true;

  const res = await api.consultarSunatRuc(ruc);

  if (btn) btn.disabled = false;

  if (res && res.success) {
    const razonInput = document.getElementById('modalClienteRazonSocial');
    const dirInput = document.getElementById('modalClienteDireccion');

    if (razonInput && res.nombre_cliente) razonInput.value = res.nombre_cliente;
    if (dirInput && res.direccion) dirInput.value = res.direccion;

    if (feedback) {
      feedback.className = 'form-text mt-1 text-success fw-bold fs-8';
      feedback.innerHTML = `<i class="bi bi-check-circle-fill me-1"></i> SUNAT: ${escapeHtml(res.nombre_cliente)} (${res.estado || 'ACTIVO'} - ${res.condicion || 'HABIDO'})`;
    }
  } else {
    if (feedback) {
      feedback.className = 'form-text mt-1 text-danger fs-8';
      feedback.innerHTML = '<i class="bi bi-exclamation-triangle-fill me-1"></i> No se encontró respuesta de SUNAT. Ingrese los datos manualmente.';
    }
  }
}

export async function consultarDni(dni) {
  lastSunatQueryRuc = dni;
  const feedback = document.getElementById('sunatStatusFeedback');
  const btn = document.getElementById('btnConsultarSunat');

  if (feedback) {
    feedback.className = 'form-text mt-1 text-warning fw-semibold fs-8';
    feedback.innerHTML = '<i class="bi bi-arrow-repeat spin me-1"></i> Consultando DNI en tiempo real...';
  }
  if (btn) btn.disabled = true;

  const res = await api.consultarDni(dni);

  if (btn) btn.disabled = false;

  if (res && res.success) {
    const razonInput = document.getElementById('modalClienteRazonSocial');
    if (razonInput && res.nombre_cliente) razonInput.value = res.nombre_cliente;

    if (feedback) {
      feedback.className = 'form-text mt-1 text-success fw-bold fs-8';
      feedback.innerHTML = `<i class="bi bi-check-circle-fill me-1"></i> RENIEC / DNI: ${escapeHtml(res.nombre_cliente)}`;
    }
  } else {
    if (feedback) {
      feedback.className = 'form-text mt-1 text-danger fs-8';
      feedback.innerHTML = '<i class="bi bi-exclamation-triangle-fill me-1"></i> No se encontró respuesta para el DNI ingresado.';
    }
  }
}

export async function saveClientFromModal() {
  const clientId = document.getElementById('modalClienteId')?.value;
  const tipoDoc = document.getElementById('modalClienteTipoDoc')?.value || 'RUC';
  const nroDoc = document.getElementById('modalClienteNroDoc')?.value.trim();
  const nombre = document.getElementById('modalClienteRazonSocial')?.value.trim();
  const direccion = document.getElementById('modalClienteDireccion')?.value.trim() || 'No especificada';

  if (!nroDoc || !nombre) {
    alert('Por favor ingrese el N° de Documento y la Razón Social / Nombre.');
    return;
  }

  const payload = {
    tipo_documento: tipoDoc,
    nro_documento: nroDoc,
    nombre_cliente: nombre,
    direccion: direccion
  };

  const modalElem = document.getElementById('modalCliente');
  const modal = bootstrap.Modal.getInstance(modalElem);

  if (clientId) {
    // Update existing client in MySQL
    await api.updateCliente(clientId, payload);
  } else {
    // Save new client in MySQL
    await api.addCliente(payload);
  }

  // Re-fetch fresh list directly from MySQL database
  currentClients = await api.getClientes();
  if (window.app) window.app.clients = [...currentClients];

  if (modal) modal.hide();
  filterClientes();
}
