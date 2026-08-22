import { escapeHtml, filterAndRankItems, formatDate } from '../helpers.js';
import { api } from '../api.js';

let state = {
  clients: [],
  products: [],
  selectedClient: null,
  advancePayments: [],
  orderItems: [],
  attachedFiles: [],
  activeClientIndex: -1,
  activeProductIndex: -1
};

export function renderNuevoPedidoPage(clients = [], products = []) {
  state.clients = clients || [];
  state.products = products || [];
  state.selectedClient = null;
  state.advancePayments = [];
  state.orderItems = [];
  state.attachedFiles = [];
  state.activeClientIndex = -1;
  state.activeProductIndex = -1;

  // Set default dates
  const todayStr = new Date().toISOString().split('T')[0];
  const fechaIngresoElem = document.getElementById('fechaIngresoInput');
  if (fechaIngresoElem) {
    fechaIngresoElem.value = todayStr;
    fechaIngresoElem.max = todayStr;
  }

  const fechaEntregaElem = document.getElementById('fechaEntregaInput');
  if (fechaEntregaElem) fechaEntregaElem.value = todayStr;

  // Render initial empty tables & file lists
  renderAdelantosTable();
  renderOrderItemsTable();
  renderAttachedFilesList();

  // Setup search input handlers
  setupClientSearch();
  setupProductSearch();
}

function setupClientSearch() {
  const input = document.getElementById('searchClientInput');
  const list = document.getElementById('clientSearchResultsList');
  if (!input || !list) return;

  if (input.dataset.boundClientSearch === 'true') return;
  input.dataset.boundClientSearch = 'true';

  input.addEventListener('input', () => {
    const val = input.value.trim();
    state.activeClientIndex = -1;

    if (!state.selectedClient || !input.value.includes(state.selectedClient.nombre_cliente)) {
      state.selectedClient = null;
    }

    if (!val) {
      list.classList.add('d-none');
      list.innerHTML = '';
      return;
    }

    const matches = filterAndRankItems(
      state.clients, 
      val, 
      c => `${c.nro_documento || ''} ${c.nombre_cliente || ''} ${c.direccion || ''}`
    ).slice(0, 15);

    if (matches.length === 0) {
      list.innerHTML = `<li class="list-group-item text-muted py-2 fs-7">No se encontraron clientes para "${escapeHtml(val)}"</li>`;
      list.classList.remove('d-none');
      return;
    }

    list.innerHTML = matches.map((c, idx) => `
      <li class="list-group-item list-group-item-action py-2 px-3 client-opt-item d-flex align-items-center gap-2 fs-7" data-index="${idx}">
        <span class="fw-bold text-primary">${escapeHtml(c.nro_documento || 'S/D')}</span>
        <span class="fw-semibold text-body">- ${escapeHtml(c.nombre_cliente)}</span>
      </li>
    `).join('');
    list.classList.remove('d-none');

    list.querySelectorAll('.client-opt-item').forEach((item, idx) => {
      item.addEventListener('click', () => {
        selectClient(matches[idx]);
      });
    });
  });

  input.addEventListener('keydown', (e) => {
    const items = list.querySelectorAll('.client-opt-item');
    if (items.length === 0) return;

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      state.activeClientIndex = Math.min(state.activeClientIndex + 1, items.length - 1);
      updateActiveItem(items, state.activeClientIndex);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      state.activeClientIndex = Math.max(state.activeClientIndex - 1, 0);
      updateActiveItem(items, state.activeClientIndex);
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (state.activeClientIndex >= 0 && items[state.activeClientIndex]) {
        items[state.activeClientIndex].click();
      }
    }
  });

  document.addEventListener('click', (e) => {
    if (!input.contains(e.target) && !list.contains(e.target)) {
      list.classList.add('d-none');
    }
  });
}

function selectClient(client) {
  state.selectedClient = client;
  const input = document.getElementById('searchClientInput');
  const list = document.getElementById('clientSearchResultsList');

  if (input) {
    const doc = client.nro_documento ? `${client.nro_documento} - ` : '';
    input.value = `${doc}${client.nombre_cliente}`;
  }
  if (list) list.classList.add('d-none');
}

export function clearSelectedClient() {
  state.selectedClient = null;
  const input = document.getElementById('searchClientInput');
  if (input) input.value = '';
}

function setupProductSearch() {
  const input = document.getElementById('searchProductInput');
  const list = document.getElementById('productSearchResultsList');
  if (!input || !list) return;

  input.addEventListener('input', () => {
    const val = input.value.trim();
    state.activeProductIndex = -1;
    if (!val) {
      list.classList.add('d-none');
      list.innerHTML = '';
      return;
    }

    const matches = filterAndRankItems(
      state.products, 
      val, 
      p => `#${p.id_producto || ''} ${p.id_producto || ''} ${p.nombre_producto || ''} ${p.categoria || ''}`
    ).slice(0, 15);

    if (matches.length === 0) {
      list.innerHTML = `<li class="list-group-item text-muted py-2 fs-7">No se encontraron productos para "${escapeHtml(val)}"</li>`;
      list.classList.remove('d-none');
      return;
    }

    list.innerHTML = matches.map((p, idx) => `
      <li class="list-group-item list-group-item-action py-1 px-3 prod-opt-item d-flex align-items-center gap-2 fs-7" data-index="${idx}">
        <span class="fw-bold text-primary">#${p.id_producto}</span>
        <span class="fw-semibold text-body">- ${escapeHtml(p.nombre_producto)}</span>
      </li>
    `).join('');
    list.classList.remove('d-none');

    list.querySelectorAll('.prod-opt-item').forEach((item, idx) => {
      item.addEventListener('click', () => {
        addProductToOrder(matches[idx]);
      });
    });
  });

  input.addEventListener('keydown', (e) => {
    const items = list.querySelectorAll('.prod-opt-item');
    if (items.length === 0) return;

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      state.activeProductIndex = Math.min(state.activeProductIndex + 1, items.length - 1);
      updateActiveItem(items, state.activeProductIndex);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      state.activeProductIndex = Math.max(state.activeProductIndex - 1, 0);
      updateActiveItem(items, state.activeProductIndex);
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (state.activeProductIndex >= 0 && items[state.activeProductIndex]) {
        items[state.activeProductIndex].click();
      }
    }
  });

  document.addEventListener('click', (e) => {
    if (!input.contains(e.target) && !list.contains(e.target)) {
      list.classList.add('d-none');
    }
  });
}

function addProductToOrder(product) {
  const input = document.getElementById('searchProductInput');
  const list = document.getElementById('productSearchResultsList');
  if (input) input.value = '';
  if (list) list.classList.add('d-none');

  const existing = state.orderItems.find(i => String(i.id_producto) === String(product.id_producto));
  if (existing) {
    existing.cantidad += 1;
  } else {
    state.orderItems.push({
      id_producto: product.id_producto,
      nombre_producto: product.nombre_producto,
      cantidad: 1
    });
  }

  renderOrderItemsTable();
}

function updateActiveItem(items, activeIndex) {
  items.forEach((item, idx) => {
    if (idx === activeIndex) {
      item.classList.add('active');
      item.scrollIntoView({ block: 'nearest' });
    } else {
      item.classList.remove('active');
    }
  });
}

function renderOrderItemsTable() {
  const tbody = document.getElementById('tableProductosBody');
  if (!tbody) return;
  tbody.innerHTML = '';

  if (state.orderItems.length === 0) {
    tbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">No se han agregado productos a la tabla.</td></tr>`;
    return;
  }

  state.orderItems.forEach((item, idx) => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td class="fw-bold">#${item.id_producto}</td>
      <td class="fw-semibold">${escapeHtml(item.nombre_producto)}</td>
      <td>
        <input type="number" class="form-control form-control-sm py-0 px-2 fs-7" value="${item.cantidad}" min="1" onchange="nuevoPedidoModule.updateItemQty(${idx}, this.value)" style="width: 90px; height: 28px;">
      </td>
      <td class="text-center">
        <button type="button" class="btn btn-sm btn-outline-danger py-0 px-2" style="height: 28px;" onclick="nuevoPedidoModule.removeOrderItem(${idx})">
          <i class="bi bi-trash"></i>
        </button>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

export function updateItemQty(index, newQty) {
  const val = parseInt(newQty, 10);
  if (val > 0 && state.orderItems[index]) {
    state.orderItems[index].cantidad = val;
  }
}

export function removeOrderItem(index) {
  state.orderItems.splice(index, 1);
  renderOrderItemsTable();
}

export function openAdelantoModal() {
  const modalElem = document.getElementById('modalAdelantoPago');
  if (!modalElem) return;

  const todayStr = new Date().toISOString().split('T')[0];
  const dateInput = document.getElementById('modalFechaPagoInput');
  const montoInput = document.getElementById('modalMontoInput');
  const voucherInput = document.getElementById('modalVoucherInput');

  if (dateInput) dateInput.value = todayStr;
  if (montoInput) montoInput.value = '';
  if (voucherInput) voucherInput.value = '';

  showBootstrapModal(modalElem);
}

export function addAdelantoFromModal() {
  const banco = document.getElementById('modalBancoSelect')?.value || 'BCP';
  const monto = parseFloat(document.getElementById('modalMontoInput')?.value || 0);
  const fecha = document.getElementById('modalFechaPagoInput')?.value || new Date().toISOString().split('T')[0];
  const voucher = document.getElementById('modalVoucherInput')?.value.trim() || '-';

  if (isNaN(monto) || monto <= 0) {
    alert('Por favor ingrese un monto de adelanto válido.');
    return;
  }

  state.advancePayments.push({
    id: Date.now(),
    banco,
    monto,
    fecha,
    voucher
  });

  renderAdelantosTable();
  hideBootstrapModal('modalAdelantoPago');
}

function renderAdelantosTable() {
  const containerTable = document.getElementById('containerAdelantosTabla');
  const tbody = document.getElementById('tableAdelantosBody');
  const totalCell = document.getElementById('totalAdelantoCell');
  if (!tbody || !containerTable) return;

  tbody.innerHTML = '';

  if (state.advancePayments.length === 0) {
    containerTable.classList.add('d-none');
    if (totalCell) totalCell.textContent = 'S/ 0.00';
    return;
  }

  containerTable.classList.remove('d-none');

  let total = 0;
  state.advancePayments.forEach((adv, idx) => {
    total += adv.monto;
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td class="fw-semibold">${formatDate(adv.fecha)}</td>
      <td><span class="badge bg-primary text-white px-2.5 py-1 fs-8 fw-bold">${escapeHtml(adv.banco)}</span></td>
      <td class="fw-semibold">${escapeHtml(adv.voucher)}</td>
      <td class="text-end fw-bold text-success">S/ ${adv.monto.toFixed(2)}</td>
      <td class="text-center">
        <button type="button" class="btn btn-sm btn-outline-danger py-0 px-2" style="height: 28px;" onclick="nuevoPedidoModule.removeAdelanto(${idx})">
          <i class="bi bi-trash"></i>
        </button>
      </td>
    `;
    tbody.appendChild(tr);
  });

  if (totalCell) totalCell.textContent = `S/ ${total.toFixed(2)}`;
}

export function removeAdelanto(index) {
  state.advancePayments.splice(index, 1);
  renderAdelantosTable();
}

export function onCondicionPagoChange(val) {
  const select = document.getElementById('condicionPagoSelect');
  const input = document.getElementById('diasCreditoInput');

  const selectedVal = val || (select ? select.value : 'CONTADO');

  if (!input) return;

  if (selectedVal === 'CONTADO') {
    input.disabled = true;
    input.value = '0';
  } else {
    input.disabled = false;
    if (input.value === '0' || !input.value) {
      input.value = '30';
    }
    input.focus();
  }
}

export function handleFilesAttached(files) {
  if (!files || files.length === 0) return;
  Array.from(files).forEach(file => {
    const reader = new FileReader();
    const sizeKb = (file.size / 1024).toFixed(1);
    const sizeStr = sizeKb > 1024 ? (sizeKb / 1024).toFixed(1) + ' MB' : sizeKb + ' KB';

    reader.onload = function (e) {
      state.attachedFiles.push({
        id: Date.now() + Math.random(),
        name: file.name,
        size: sizeStr,
        type: file.type || 'application/pdf',
        data: e.target.result
      });
      renderAttachedFilesList();
    };
    reader.readAsDataURL(file);
  });
}

export function removeAttachedFile(index) {
  state.attachedFiles.splice(index, 1);
  renderAttachedFilesList();
}

function renderAttachedFilesList() {
  const container = document.getElementById('listaArchivosAdjuntos');
  if (!container) return;

  if (state.attachedFiles.length === 0) {
    container.innerHTML = '';
    return;
  }

  container.innerHTML = `
    <div class="card p-2 bg-body-tertiary border">
      <div class="d-flex justify-content-between align-items-center mb-2 px-1">
        <span class="fs-7 fw-bold text-secondary">
          <i class="bi bi-paperclip me-1"></i>Archivos Adjuntados (${state.attachedFiles.length})
        </span>
      </div>
      <ul class="list-group list-group-flush border rounded">
        ${state.attachedFiles.map((file, idx) => `
          <li class="list-group-item d-flex justify-content-between align-items-center py-2 px-3">
            <div class="d-flex align-items-center gap-2 overflow-hidden me-2">
              <i class="bi bi-file-earmark-pdf text-danger fs-5"></i>
              <span class="fw-semibold text-truncate fs-7">${escapeHtml(file.name)}</span>
              <span class="badge bg-secondary-subtle text-secondary fs-8">${file.size}</span>
            </div>
            <button type="button" class="btn btn-sm btn-outline-danger border-0 py-0 px-1" onclick="nuevoPedidoModule.removeAttachedFile(${idx})">
              <i class="bi bi-trash"></i>
            </button>
          </li>
        `).join('')}
      </ul>
    </div>
  `;
}

export function hasUnsavedChanges() {
  if (state.selectedClient !== null) return true;
  if (state.advancePayments.length > 0) return true;
  if (state.orderItems.length > 0) return true;
  if (state.attachedFiles.length > 0) return true;

  const clientText = document.getElementById('searchClientInput')?.value.trim();
  if (clientText && clientText.length > 0) return true;

  const obs = document.getElementById('observacionesInput')?.value.trim();
  if (obs && obs.length > 0) return true;

  return false;
}

export async function submitNuevoPedido() {
  if (!state.selectedClient) {
    const rawClientText = document.getElementById('searchClientInput')?.value.trim() || '';
    if (rawClientText.length > 0) {
      state.selectedClient = { id_cliente: 0, nombre_cliente: rawClientText, nro_documento: '' };
    } else {
      alert('Por favor seleccione o escriba un Cliente / Razón Social para continuar.');
      document.getElementById('searchClientInput')?.focus();
      return;
    }
  }

  if (state.orderItems.length === 0) {
    alert('Debe agregar al menos un producto en la tabla.');
    document.getElementById('searchProductInput')?.focus();
    return;
  }

  const establecimiento = document.getElementById('establecimientoSelect')?.value || 'CARABAYLLO';
  const nroOrdenCompra = document.getElementById('nroOrdenCompraInput')?.value.trim() || '';
  const condicionPago = document.getElementById('condicionPagoSelect')?.value || 'CONTADO';
  const diasCredito = condicionPago !== 'CONTADO' ? parseInt(document.getElementById('diasCreditoInput')?.value || 30, 10) : 0;
  const fechaIngreso = document.getElementById('fechaIngresoInput')?.value || new Date().toISOString().split('T')[0];
  const todayStr = new Date().toISOString().split('T')[0];

  if (fechaIngreso > todayStr) {
    alert(`¡Atención! La fecha del pedido no puede ser posterior al día de hoy (${formatDate(todayStr)}). No se pueden registrar pedidos con fecha futura (${formatDate(fechaIngreso)}).`);
    document.getElementById('fechaIngresoInput')?.focus();
    return;
  }

  const fechaEntrega = document.getElementById('fechaEntregaInput')?.value || fechaIngreso;
  const observaciones = document.getElementById('observacionesInput')?.value.trim() || '';
  const storagePath = localStorage.getItem('inplabel_pdf_storage_path') || 'C:\\Users\\User\\OneDrive\\Escritorio\\OrdenesI';
  const useSubfolders = localStorage.getItem('inplabel_pdf_subfolders') === 'true';

  const btnGuardar = document.getElementById('btnGuardarPedido');
  if (btnGuardar) {
    btnGuardar.disabled = true;
    btnGuardar.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Guardando...`;
  }

  const payload = {
    id_cliente: state.selectedClient.id_cliente,
    nombre_cliente: state.selectedClient.nombre_cliente,
    nro_documento: state.selectedClient.nro_documento || '',
    establecimiento: establecimiento,
    nro_orden: nroOrdenCompra,
    nro_orden_compra: nroOrdenCompra,
    fecha_pedido: fechaIngreso,
    fecha_entrega: fechaEntrega,
    estado: 'PENDIENTE',
    condicion_pago: condicionPago,
    dias_credito: diasCredito,
    observaciones: observaciones,
    storage_path: storagePath,
    use_subfolders: useSubfolders,
    adelantos: state.advancePayments,
    adjuntos: state.attachedFiles,
    detalles: state.orderItems.map(item => ({
      id_producto: item.id_producto,
      nombre_producto: item.nombre_producto,
      codigo_producto: item.codigo_producto || '',
      cantidad: item.cantidad,
      cantidad_entregada: 0
    }))
  };

  try {
    const newOrder = await api.createPedido(payload);
    alert(`¡Pedido registrado exitosamente! ${newOrder.nro_pedido ? 'N° Pedido: ' + newOrder.nro_pedido : ''}`);
    
    // Update window.app.orders in memory immediately (no full page reload required)
    if (window.app) {
      if (!Array.isArray(window.app.orders)) window.app.orders = [];
      const exists = window.app.orders.some(o => String(o.id_pedido) === String(newOrder.id_pedido));
      if (!exists) {
        window.app.orders.unshift(newOrder);
      }
    }

    resetNuevoPedidoForm();
    if (window.app) {
      window.app.navigateTo('pedidos');
      window.app.renderCurrentView();
    }
  } catch (err) {
    console.error('Error al registrar pedido:', err);
    alert('Ocurrió un error al guardar el pedido en el servidor. Por favor intente nuevamente.');
  } finally {
    if (btnGuardar) {
      btnGuardar.disabled = false;
      btnGuardar.innerHTML = `<i class="bi bi-check-circle me-1"></i> Guardar Pedido`;
    }
  }
}

export function resetNuevoPedidoForm() {
  state.selectedClient = null;
  state.advancePayments = [];
  state.orderItems = [];
  state.attachedFiles = [];
  state.activeClientIndex = -1;
  state.activeProductIndex = -1;

  const searchClientInput = document.getElementById('searchClientInput');
  if (searchClientInput) searchClientInput.value = '';

  const nroOrdenCompraInput = document.getElementById('nroOrdenCompraInput');
  if (nroOrdenCompraInput) nroOrdenCompraInput.value = '';

  const observacionesInput = document.getElementById('observacionesInput');
  if (observacionesInput) observacionesInput.value = '';

  const archivosAdjuntosInput = document.getElementById('archivosAdjuntosInput');
  if (archivosAdjuntosInput) archivosAdjuntosInput.value = '';

  const searchProductInput = document.getElementById('searchProductInput');
  if (searchProductInput) searchProductInput.value = '';

  const condicionPagoSelect = document.getElementById('condicionPagoSelect');
  if (condicionPagoSelect) condicionPagoSelect.value = 'CONTADO';

  onCondicionPagoChange('CONTADO');
  renderAdelantosTable();
  renderOrderItemsTable();
  renderAttachedFilesList();
}
