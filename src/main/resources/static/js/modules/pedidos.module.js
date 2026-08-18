import { escapeHtml, formatDate, showBootstrapModal } from '../helpers.js';
import { api } from '../api.js';

let currentOrders = [];

export function renderPedidosTable(orders = [], searchQuery = '') {
  currentOrders = orders || [];
  const tbody = document.getElementById('pedidosTableBody');
  if (!tbody) return;
  tbody.innerHTML = '';

  setupDefaultDateFilters();

  const searchClientInput = document.getElementById('searchClientNameInput');
  const clientQuery = (searchClientInput?.value || searchQuery || '').trim().toLowerCase();

  const dateFrom = document.getElementById('filterDateFrom')?.value;
  const dateTo = document.getElementById('filterDateTo')?.value;
  const establishment = document.getElementById('filterEstablishment')?.value || 'ALL';
  const orderStatus = document.getElementById('filterOrderStatus')?.value || 'ALL';
  const todayStr = new Date().toISOString().split('T')[0];

  const filtered = currentOrders.filter(o => {
    const matchClient = !clientQuery ||
      (o.nombre_cliente && o.nombre_cliente.toLowerCase().includes(clientQuery)) ||
      (o.nro_pedido && o.nro_pedido.toLowerCase().includes(clientQuery)) ||
      (o.nro_orden && o.nro_orden.toLowerCase().includes(clientQuery));

    let matchDate = true;
    if (o.fecha_pedido) {
      if (dateFrom && o.fecha_pedido < dateFrom) matchDate = false;
      if (dateTo && o.fecha_pedido > dateTo) matchDate = false;
    }

    let matchEstab = true;
    if (establishment && establishment !== 'ALL') {
      const oEstab = (o.establecimiento || 'CARABAYLLO').toUpperCase();
      matchEstab = oEstab.includes(establishment);
    }

    let matchStatus = true;
    if (orderStatus && orderStatus !== 'ALL') {
      const ost = (o.estado || 'PENDIENTE').toUpperCase();
      const isOverdue = o.fecha_entrega && o.fecha_entrega < todayStr && ost !== 'COMPLETADO' && ost !== 'CANCELADO';

      if (orderStatus === 'FUERA_DE_PLAZO') {
        matchStatus = isOverdue;
      } else if (orderStatus === 'EN_PROCESO') {
        matchStatus = (ost === 'EN_PROCESO' || ost === 'PARCIAL');
      } else {
        matchStatus = (ost === orderStatus);
      }
    }

    return matchClient && matchDate && matchEstab && matchStatus;
  });

  const badge = document.getElementById('ordersCountBadge');
  if (badge) badge.textContent = `${filtered.length} pedidos`;

  if (filtered.length === 0) {
    tbody.innerHTML = `<tr><td colspan="9" class="text-center text-muted py-4">No se encontraron pedidos con los filtros aplicados.</td></tr>`;
    return;
  }

  filtered.forEach(o => {
    const tr = document.createElement('tr');
    const estabRaw = (o.establecimiento || 'CARABAYLLO').toUpperCase();
    const estabText = (estabRaw === 'COMAS' || estabRaw.includes('COMAS'))
      ? 'Planta Principal - Comas'
      : 'Sucursal - Carabayllo';
    const estadoClass = (o.estado || 'PENDIENTE').toUpperCase();

    tr.innerHTML = `
      <td class="fw-bold text-primary">${o.nro_pedido || ('PED-' + o.id_pedido)}</td>
      <td class="fw-semibold text-secondary small">${o.nro_orden ? escapeHtml(o.nro_orden) : '<span class="text-muted fs-8">Sin Asignar</span>'}</td>
      <td class="fw-semibold">${escapeHtml(o.nombre_cliente || 'Cliente General')}</td>
      <td>${formatDate(o.fecha_pedido)}</td>
      <td><span class="small text-muted">${estabText}</span></td>
      <td><span class="status-badge ${estadoClass}">${estadoClass}</span></td>
      <!-- 1. Columna FINALIZAR -->
      <td class="text-center">
        <button class="btn-action-solid btn-finish" onclick="pedidosModule.openFinalizarOrdenModal('${o.id_pedido || o.id || o.nro_pedido}')" title="Finalizar Orden (Entregado / Cancelado)">
          <i class="bi bi-flag-fill"></i>
        </button>
      </td>
      <!-- 2. Columna ENVÍO -->
      <td class="text-center">
        <button class="btn-action-solid btn-envio" onclick="pedidosModule.openRegistrarEnvioModal('${o.id_pedido || o.id || o.nro_pedido}')" title="Registrar Envío de Productos">
          <i class="bi bi-truck text-white"></i>
        </button>
      </td>
      <!-- 3. Columna DETALLE -->
      <td class="text-center">
        <button class="btn-action-solid btn-view" onclick="pedidosModule.viewOrderDetail('${o.id_pedido || o.id || o.nro_pedido}')" title="Ver Detalles del Pedido">
          <i class="bi bi-eye-fill"></i>
        </button>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

export function setupDefaultDateFilters() {
  const dateFromElem = document.getElementById('filterDateFrom');
  const dateToElem = document.getElementById('filterDateTo');

  const now = new Date();
  const year = now.getFullYear();
  const month = String(now.getMonth() + 1).padStart(2, '0');
  const day = String(now.getDate()).padStart(2, '0');

  const firstDay = `${year}-${month}-01`;
  const today = `${year}-${month}-${day}`;

  if (dateFromElem && !dateFromElem.value) {
    dateFromElem.value = firstDay;
  }
  if (dateToElem && !dateToElem.value) {
    dateToElem.value = today;
  }
}

// 1. Finalizar Orden Modal Handlers
export function openFinalizarOrdenModal(idPedido) {
  const order = currentOrders.find(o => String(o.id_pedido) === String(idPedido) || String(o.id) === String(idPedido) || String(o.nro_pedido) === String(idPedido));
  if (!order) return;

  document.getElementById('finalizarOrderId').value = idPedido;
  document.getElementById('finalizarOrderNroBadge').textContent = order.nro_pedido || ('PED-' + idPedido);

  document.getElementById('optCompletado').checked = true;
  toggleFinalizarFields('ENTREGADO');

  document.getElementById('finalizarNroGuia').value = order.nro_guia || '';
  document.getElementById('finalizarFechaEntrega').value = order.fecha_entrega || new Date().toISOString().split('T')[0];

  const modalElem = document.getElementById('modalFinalizarOrden');
  if (modalElem) {
    const modal = new bootstrap.Modal(modalElem);
    modal.show();
  }
}

export function toggleFinalizarFields(status) {
  const fields = document.getElementById('finalizarDeliveryFields');
  if (!fields) return;
  if (status === 'ENTREGADO') {
    fields.style.display = 'block';
  } else {
    fields.style.display = 'none';
  }
}

export async function saveFinalizarOrden() {
  const idPedido = parseInt(document.getElementById('finalizarOrderId')?.value);
  if (!idPedido) return;

  const isCompletado = document.getElementById('optCompletado')?.checked;
  const nuevoEstado = isCompletado ? 'ENTREGADO' : 'CANCELADO';

  const nroGuia = isCompletado ? document.getElementById('finalizarNroGuia')?.value.trim() : null;
  const fechaEntrega = isCompletado ? document.getElementById('finalizarFechaEntrega')?.value : null;

  if (isCompletado && !nroGuia) {
    alert('Por favor ingrese el N° de Guía de Remisión para completar la orden.');
    return;
  }

  const payload = {
    estado: nuevoEstado,
    nro_guia: nroGuia,
    fecha_entrega: fechaEntrega
  };

  const res = await api.updatePedidoStatus(idPedido, payload);
  if (res) {
    const localOrder = currentOrders.find(o => o.id_pedido === idPedido);
    if (localOrder) {
      localOrder.estado = nuevoEstado;
      localOrder.nro_guia = nroGuia;
      localOrder.fecha_entrega = fechaEntrega;
    }
  }

  const modalElem = document.getElementById('modalFinalizarOrden');
  const modal = bootstrap.Modal.getInstance(modalElem);
  if (modal) modal.hide();

  renderPedidosTable(currentOrders);
}

// 2. Registrar Envío Modal Handlers
export function openRegistrarEnvioModal(idPedido) {
  const order = currentOrders.find(o => String(o.id_pedido) === String(idPedido) || String(o.id) === String(idPedido) || String(o.nro_pedido) === String(idPedido));
  if (!order) return;

  document.getElementById('envioOrderId').value = idPedido;
  document.getElementById('envioClientId').value = order.id_cliente || 0;
  document.getElementById('envioOrderNroBadge').textContent = order.nro_pedido || ('PED-' + idPedido);
  document.getElementById('envioNroGuia').value = '';
  document.getElementById('envioNroGuia').placeholder = 'Ej: G001-001234';
  document.getElementById('envioFechaGuia').value = new Date().toISOString().split('T')[0];

  const tbody = document.getElementById('envioProductsTableBody');
  if (tbody) {
    tbody.innerHTML = '';
    const items = order.detalles || [];
    if (items.length === 0) {
      tbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-3">No hay productos en esta orden.</td></tr>`;
    } else {
      items.forEach(item => {
        const cantSolicitada = item.cantidad || 0;
        const cantEntregada = item.cantidad_entregada || 0;
        const pendiente = Math.max(0, cantSolicitada - cantEntregada);

        const tr = document.createElement('tr');
        tr.innerHTML = `
          <td>
            <div class="fw-bold">${escapeHtml(item.nombre_producto || 'Producto')}</div>
            <span class="small text-muted">${item.codigo_producto || ''}</span>
          </td>
          <td class="text-center fw-bold">${cantSolicitada}</td>
          <td class="text-center text-muted">${cantEntregada}</td>
          <td class="text-center">
            <input type="number" class="form-control form-control-sm text-center input-envio-cantidad" 
              data-product-id="${item.id_producto}" 
              min="0" max="${cantSolicitada}" value="" placeholder="0">
          </td>
        `;
        tbody.appendChild(tr);
      });
    }
  }

  const modalElem = document.getElementById('modalRegistrarEnvio');
  if (modalElem) {
    const modal = new bootstrap.Modal(modalElem);
    modal.show();
  }
}

export async function saveRegistrarEnvio() {
  const idPedido = parseInt(document.getElementById('envioOrderId')?.value);
  const idCliente = parseInt(document.getElementById('envioClientId')?.value) || 0;
  const nroGuia = document.getElementById('envioNroGuia')?.value.trim();
  const fechaGuia = document.getElementById('envioFechaGuia')?.value;

  if (!nroGuia || !fechaGuia) {
    alert('Ingrese el N° de Guía y la Fecha del Envío.');
    return;
  }

  const quantityInputs = document.querySelectorAll('.input-envio-cantidad');
  const detallesEnvio = [];
  quantityInputs.forEach(input => {
    const idProd = parseInt(input.getAttribute('data-product-id'));
    const cant = parseInt(input.value) || 0;
    if (idProd && cant > 0) {
      detallesEnvio.push({ id_producto: idProd, cantidad: cant });
    }
  });

  if (detallesEnvio.length === 0) {
    alert('Por favor ingrese al menos una cantidad a enviar mayor a 0.');
    return;
  }

  const payload = {
    id_pedido: idPedido,
    id_cliente: idCliente,
    nro_guia: nroGuia,
    fecha_guia: fechaGuia,
    estado: 'ENTREGADO',
    detalles: detallesEnvio
  };

  const res = await api.addGuia(payload);
  if (res) {
    const localOrder = currentOrders.find(o => o.id_pedido === idPedido);
    if (localOrder) {
      localOrder.nro_guia = nroGuia;
      localOrder.fecha_entrega = fechaGuia;

      // Update local order guias list
      if (!localOrder.guias) localOrder.guias = [];
      localOrder.guias.push({
        id_guia: res.id_guia || Date.now(),
        nro_guia: nroGuia,
        fecha_guia: fechaGuia,
        detalles: detallesEnvio
      });

      // Update accumulated delivered quantities
      if (localOrder.detalles) {
        localOrder.detalles.forEach(item => {
          const matchEnvio = detallesEnvio.find(d => d.id_producto === item.id_producto);
          if (matchEnvio) {
            item.cantidad_entregada = (item.cantidad_entregada || 0) + matchEnvio.cantidad;
          }
        });
      }
    }
  }

  const modalElem = document.getElementById('modalRegistrarEnvio');
  const modal = bootstrap.Modal.getInstance(modalElem);
  if (modal) modal.hide();

  renderPedidosTable(currentOrders);
}

// 3. Ver Detalles del Pedido (Modal #orderDetailModal)
export function viewOrderDetail(idPedido) {
  try {
    let order = currentOrders.find(o => String(o.id_pedido) === String(idPedido) || String(o.id) === String(idPedido) || String(o.nro_pedido) === String(idPedido));
    if (!order && window.app && window.app.orders) {
      order = window.app.orders.find(o => String(o.id_pedido) === String(idPedido) || String(o.id) === String(idPedido) || String(o.nro_pedido) === String(idPedido));
    }
    if (!order) {
      console.warn("Order not found for viewOrderDetail:", idPedido);
      alert("No se encontró el detalle de este pedido.");
      return;
    }

    const modalTitle = document.getElementById('orderDetailTitle');
    const modalBody = document.getElementById('orderDetailBody');
    if (!modalBody) {
      console.error("modalBody #orderDetailBody not found!");
      return;
    }

    if (modalTitle) {
      modalTitle.innerHTML = `<i class="bi bi-file-earmark-text me-2 text-primary"></i> Detalle del Pedido ${order.nro_pedido || ('PED-' + idPedido)}`;
    }

    const estadoClass = (order.estado || 'PENDIENTE').toUpperCase();
    const estabRaw = (order.establecimiento || 'CARABAYLLO').toUpperCase();
    const estabText = (estabRaw === 'COMAS' || estabRaw.includes('COMAS'))
      ? 'Planta Principal - Comas'
      : 'Sucursal - Carabayllo';
    const guiasList = order.guias || [];
    const detalles = order.detalles || [];

    // Build Guias Historial HTML
    let guiasHtml = '';
    if (guiasList.length > 0) {
      guiasHtml = `
      <div class="card border-0 bg-body-tertiary mb-3">
        <div class="card-header bg-transparent fw-bold text-success d-flex align-items-center justify-content-between">
          <span><i class="bi bi-truck me-2"></i> Historial de Envíos / Entregas Parciales (${guiasList.length})</span>
        </div>
        <div class="card-body p-2">
          <div class="table-responsive">
            <table class="table custom-table table-sm mb-0">
              <thead>
                <tr>
                  <th>N° Guía</th>
                  <th>Fecha de Envío</th>
                  <th>Productos / Cantidades Entregadas</th>
                </tr>
              </thead>
              <tbody>
                ${guiasList.map(g => `
                  <tr>
                    <td class="fw-bold text-primary">${escapeHtml(g.nro_guia || '-')}</td>
                    <td>${formatDate(g.fecha_guia)}</td>
                    <td>
                      <ul class="mb-0 ps-3 small text-muted">
                        ${(g.detalles || []).map(d => `<li>${escapeHtml(d.nombre_producto || 'Producto')}: <strong>${d.cantidad} un.</strong></li>`).join('')}
                      </ul>
                    </td>
                  </tr>
                `).join('')}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    `;
    }

    // Parse attached files if present
    let filesList = [];
    if (order.adjuntos) {
      try {
        filesList = typeof order.adjuntos === 'string' ? JSON.parse(order.adjuntos) : order.adjuntos;
      } catch (e) {
        console.warn("Error parsing adjuntos JSON:", e);
      }
    }

    let adjuntosHtml = '';
    if (Array.isArray(filesList) && filesList.length > 0) {
      adjuntosHtml = `
      <h6 class="fw-bold mb-2 text-secondary"><i class="bi bi-file-earmark-pdf me-1"></i> Documentos Adjuntos (${filesList.length}):</h6>
      <div class="d-flex flex-column gap-2 mb-3">
        ${filesList.map(f => {
        let fileName = `Orden_Compra_${order.nro_pedido || order.id_pedido}.pdf`;
        let fileData = '#';
        let fileSize = '';

        if (typeof f === 'string') {
          fileName = f;
        } else if (f && typeof f === 'object') {
          fileName = f.name || fileName;
          fileData = f.data || '#';
          fileSize = f.size || '';
        }

        return `
            <div class="d-flex align-items-center justify-content-between p-2.5 border rounded bg-body-tertiary">
              <div class="d-flex align-items-center gap-2 overflow-hidden me-2">
                <i class="bi bi-file-earmark-pdf-fill text-danger fs-4"></i>
                <div class="text-truncate">
                  <div class="fw-semibold small text-truncate">${escapeHtml(fileName)}</div>
                  <div class="text-muted fs-8">${fileSize ? fileSize + ' - ' : ''}Documento Adjunto</div>
                </div>
              </div>
              ${fileData && fileData !== '#' ? `
                <a href="${fileData}" download="${escapeHtml(fileName)}" class="btn btn-sm btn-outline-primary flex-shrink-0">
                  <i class="bi bi-download me-1"></i> Descargar PDF
                </a>
              ` : `
                <span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle flex-shrink-0">
                  <i class="bi bi-file-earmark me-1"></i> ${escapeHtml(fileName)}
                </span>
              `}
            </div>
          `;
      }).join('')}
      </div>
    `;
    } else {
      adjuntosHtml = `
      <h6 class="fw-bold mb-2 text-secondary"><i class="bi bi-file-earmark-pdf me-1"></i> Documentos Adjuntos:</h6>
      <div class="p-3 text-center text-muted border rounded bg-body-tertiary mb-3 fs-7">
        <i class="bi bi-file-earmark-x fs-4 d-block mb-1 text-secondary"></i>
        No se adjuntaron archivos PDF para este pedido.
      </div>
    `;
    }

    const todayStr = new Date().toISOString().split('T')[0];
    const isOverdue = order.fecha_entrega && order.fecha_entrega < todayStr;

    const overdueBanner = (isOverdue && order.estado !== 'COMPLETADO') ? `
    <div class="alert alert-danger d-flex align-items-center py-2 px-3 fs-7 mb-3" role="alert">
      <i class="bi bi-exclamation-triangle-fill me-2 fs-5"></i>
      <div><strong>¡Atención! Fecha de entrega vencida:</strong> La fecha pactada (${formatDate(order.fecha_entrega)}) ya ha pasado.</div>
    </div>
  ` : '';

    // Consolidate duplicate order items by product name
    const consolidatedDetalles = [];
    (detalles || []).forEach(item => {
      const name = (item.nombre_producto || 'Producto').trim();
      const existing = consolidatedDetalles.find(d => (d.nombre_producto || '').trim().toLowerCase() === name.toLowerCase());
      if (existing) {
        existing.cantidad = Number(existing.cantidad || 0) + Number(item.cantidad || 0);
        existing.cantidad_entregada = Number(existing.cantidad_entregada || 0) + Number(item.cantidad_entregada || 0);
      } else {
        consolidatedDetalles.push({
          ...item,
          cantidad: Number(item.cantidad || 0),
          cantidad_entregada: Number(item.cantidad_entregada || 0)
        });
      }
    });

    modalBody.innerHTML = `
    ${overdueBanner}
    <div class="row g-3 mb-3">
      <div class="col-md-6">
        <div class="p-3 border rounded bg-body-tertiary h-100">
          <div class="text-muted small text-uppercase font-monospace fw-bold mb-1">Cliente / Razón Social</div>
          <div class="fw-bold fs-6 mb-1">${escapeHtml(order.nombre_cliente || 'Cliente General')}</div>
          <div class="small text-muted mb-1">RUC/DNI: ${order.nro_documento || 'No registrado'}</div>
          <div class="small text-muted"><strong>Establecimiento:</strong> ${estabText}</div>
        </div>
      </div>
      <div class="col-md-6">
        <div class="p-3 border rounded bg-body-tertiary h-100">
          <div class="d-flex justify-content-between align-items-center mb-1">
            <span class="text-muted small text-uppercase font-monospace fw-bold">Estado Actual</span>
            <span class="status-badge ${estadoClass}">${estadoClass}</span>
          </div>
          <div class="small mb-1"><strong>N° Orden Compra:</strong> ${order.nro_orden ? escapeHtml(order.nro_orden) : '<span class="text-muted">Sin Asignar</span>'}</div>
          <div class="small mb-1"><strong>Fecha Pedido:</strong> ${formatDate(order.fecha_pedido)}</div>
          <div class="small mb-1"><strong>Fecha de Entrega:</strong> ${formatDate(order.fecha_entrega)} ${isOverdue && order.estado !== 'COMPLETADO' ? '<span class="badge bg-danger-subtle text-danger border border-danger-subtle ms-1">Fuera del plazo</span>' : ''}</div>
          ${order.nro_guia ? `<div class="small"><strong>N° Guía Oficial:</strong> ${escapeHtml(order.nro_guia)}</div>` : ''}
        </div>
      </div>
    </div>

    <!-- Tabla de Productos Solicitados y Entregados -->
    <h6 class="fw-bold mb-2 text-secondary"><i class="bi bi-box-seam me-1"></i> Ítems del Pedido:</h6>
    <div class="table-responsive border rounded mb-3">
      <table class="table custom-table table-sm align-middle mb-0">
        <thead class="bg-body-tertiary">
          <tr>
            <th>Producto</th>
            <th class="text-center">Solicitado</th>
            <th class="text-center">Entregado</th>
            <th class="text-center">Estado Entrega</th>
          </tr>
        </thead>
        <tbody>
          ${consolidatedDetalles.length === 0 ? '<tr><td colspan="4" class="text-center text-muted py-3">Sin productos especificados.</td></tr>' :
        consolidatedDetalles.map(item => {
          const sol = item.cantidad || 0;
          const ent = item.cantidad_entregada || 0;
          const isDone = ent >= sol && sol > 0;

          let entregaBadge = '';
          if (isDone) {
            entregaBadge = '<span class="badge bg-success-subtle text-success border border-success-subtle"><i class="bi bi-check-all me-1"></i> Completo</span>';
          } else if (isOverdue) {
            entregaBadge = '<span class="badge bg-danger-subtle text-danger border border-danger-subtle"><i class="bi bi-exclamation-triangle-fill me-1"></i> Fuera del plazo</span>';
          } else if (ent > 0) {
            entregaBadge = '<span class="badge bg-warning-subtle text-warning-emphasis border border-warning-subtle"><i class="bi bi-clock-history me-1"></i> Parcial</span>';
          } else {
            entregaBadge = '<span class="badge bg-secondary-subtle text-secondary border border-secondary-subtle"><i class="bi bi-clock me-1"></i> Pendiente</span>';
          }

          return `
                <tr>
                  <td>
                    <div class="fw-semibold">${escapeHtml(item.nombre_producto || 'Producto')}</div>
                    <span class="small text-muted">${item.codigo_producto || ''}</span>
                  </td>
                  <td class="text-center fw-bold">${sol}</td>
                  <td class="text-center text-success fw-bold">${ent}</td>
                  <td class="text-center">
                    ${entregaBadge}
                  </td>
                </tr>
              `;
        }).join('')
      }
        </tbody>
      </table>
    </div>

    <!-- Observaciones del Pedido -->
    <h6 class="fw-bold mb-2 text-secondary"><i class="bi bi-chat-left-text me-1"></i> Observaciones:</h6>
    <div class="p-3 border rounded bg-body-tertiary mb-3 fs-7">
      ${order.observaciones ? escapeHtml(order.observaciones) : '<span class="text-muted">-</span>'}
    </div>

    ${guiasHtml}

    ${adjuntosHtml}
  `;
  } catch (err) {
    console.error("viewOrderDetail build error:", err);
  }

  showBootstrapModal('orderDetailModal');
}

export function populateClientSelect(clients) { }
export function resetProductRows() { }
export function addOrderProductRow() { }
