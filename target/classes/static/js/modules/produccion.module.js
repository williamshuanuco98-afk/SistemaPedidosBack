import { escapeHtml, formatDate, showBootstrapModal } from '../helpers.js';

let currentOrders = [];
let currentProducts = [];
let currentSearchQuery = '';
let aggregatedProductsMap = {};

function detectCategory(prodName = '', tipoProd = '') {
  const text = (prodName + ' ' + tipoProd).toUpperCase();
  if (text.includes('FRASCO') || text.includes('BOTELLA') || text.includes('PET')) {
    return 'FRASCOS';
  }
  if (text.includes('GALON') || text.includes('BALDE') || text.includes('BIDON')) {
    return 'GALONES';
  }
  if (text.includes('TAPA') || text.includes('LINER') || text.includes('OBTURADOR') || text.includes('ROSCA')) {
    return 'TAPAS';
  }
  if (text.includes('ASA')) {
    return 'ASAS';
  }
  return tipoProd || 'GENERAL';
}

export function renderProduccionTable(orders = [], products = [], searchQuery = '') {
  currentOrders = orders || [];
  currentProducts = products || [];
  if (searchQuery !== undefined) currentSearchQuery = searchQuery;

  // Build product lookup map by id
  const prodCatalogMap = {};
  if (Array.isArray(currentProducts)) {
    currentProducts.forEach(p => {
      const id = p.id_producto || p.id;
      if (id) prodCatalogMap[id] = p;
    });
  }

  // Filter ONLY active orders (completed, delivered or cancelled orders are discounted/excluded)
  const activeOrders = currentOrders.filter(order => {
    const st = (order.estado || 'PENDIENTE').toUpperCase();
    return st !== 'COMPLETADO' && st !== 'ENTREGADO' && st !== 'ANULADO';
  });

  // Build aggregated product map from active orders
  aggregatedProductsMap = {};
  const uniqueClientsSet = new Set();
  let totalFrascos = 0;
  let totalGalones = 0;
  let totalTapas = 0;

  activeOrders.forEach(order => {
    const clientName = order.nombre_cliente || 'Cliente General';
    if (clientName) uniqueClientsSet.add(clientName.trim());

    if (order.detalles && Array.isArray(order.detalles)) {
      const orderDate = order.fecha_pedido || '';
      const deliveryDate = order.fecha_entrega || '';
      const orderNro = order.nro_pedido || ('PED-' + order.id_pedido);
      const poNro = order.nro_orden || '';
      const clientDoc = order.nro_documento || '';
      const estabRaw = (order.establecimiento || 'CARABAYLLO').toUpperCase();
      const estabText = (estabRaw === 'COMAS' || estabRaw.includes('COMAS')) 
        ? 'Planta Principal - Comas' 
        : 'Sucursal - Carabayllo';

      order.detalles.forEach(d => {
        const prodId = d.id_producto || 0;
        const catalogProd = prodCatalogMap[prodId];
        const prodName = d.nombre_producto || catalogProd?.nombre_producto || 'Producto';
        const prodCode = d.codigo_producto || catalogProd?.codigo_producto || ('PROD-' + prodId);
        const tipoProd = catalogProd?.tipo_producto || d.tipo_producto || '';
        const categoria = detectCategory(prodName, tipoProd);

        const sol = Number(d.cantidad) || 0;
        const ent = Number(d.cantidad_entregada) || 0;
        const pend = Math.max(0, sol - ent);

        // Add to category counters based on active demand
        if (categoria === 'FRASCOS') {
          totalFrascos += sol;
        } else if (categoria === 'GALONES') {
          totalGalones += sol;
        } else if (categoria === 'TAPAS') {
          totalTapas += sol;
        }

        if (!aggregatedProductsMap[prodId]) {
          aggregatedProductsMap[prodId] = {
            id_producto: prodId,
            codigo_producto: prodCode,
            nombre_producto: prodName,
            categoria: categoria,
            total_solicitado: 0,
            total_entregado: 0,
            total_pendiente: 0,
            pedidos_count: 0,
            clientes: []
          };
        }

        const entry = aggregatedProductsMap[prodId];
        entry.total_solicitado += sol;
        entry.total_entregado += ent;
        entry.total_pendiente += pend;
        entry.pedidos_count += 1;

        entry.clientes.push({
          id_pedido: order.id_pedido,
          nro_pedido: orderNro,
          nro_orden: poNro,
          nombre_cliente: clientName,
          nro_documento: clientDoc,
          fecha_pedido: orderDate,
          fecha_entrega: deliveryDate,
          establecimiento: estabText,
          estado_pedido: order.estado || 'PENDIENTE',
          cantidad_solicitada: sol,
          cantidad_entregada: ent,
          cantidad_pendiente: pend
        });
      });
    }
  });

  const productList = Object.values(aggregatedProductsMap);

  // Filter list by searchQuery
  const query = (currentSearchQuery || '').toLowerCase().trim();
  const filtered = productList.filter(p => {
    if (!query) return true;
    return (
      (p.nombre_producto && p.nombre_producto.toLowerCase().includes(query)) ||
      (p.codigo_producto && p.codigo_producto.toLowerCase().includes(query)) ||
      (p.categoria && p.categoria.toLowerCase().includes(query))
    );
  });

  // Calculate top product by demand
  let topProdName = '-';
  let topProdQty = 0;
  if (productList.length > 0) {
    const sortedByQty = [...productList].sort((a, b) => b.total_solicitado - a.total_solicitado);
    if (sortedByQty[0]) {
      topProdName = sortedByQty[0].nombre_producto;
      topProdQty = sortedByQty[0].total_solicitado;
    }
  }

  // Update 6 KPI Cards
  const totalItems = filtered.length;
  const countBadge = document.getElementById('produccionCountBadge');
  const sItems = document.getElementById('statProdItemsCount');
  const sTopName = document.getElementById('statProdTopName');
  const sTopQty = document.getElementById('statProdTopQty');
  const sClients = document.getElementById('statProdClientsCount');
  const sFrascos = document.getElementById('statProdFrascosCount');
  const sGalones = document.getElementById('statProdGalonesCount');
  const sTapas = document.getElementById('statProdTapasCount');

  if (countBadge) countBadge.textContent = `${totalItems} productos`;
  if (sItems) sItems.textContent = totalItems;
  if (sTopName) {
    sTopName.textContent = topProdName;
    sTopName.title = topProdName;
  }
  if (sTopQty) sTopQty.textContent = `${topProdQty.toLocaleString()} und`;
  if (sClients) sClients.textContent = uniqueClientsSet.size;
  if (sFrascos) sFrascos.textContent = totalFrascos.toLocaleString();
  if (sGalones) sGalones.textContent = totalGalones.toLocaleString();
  if (sTapas) sTapas.textContent = totalTapas.toLocaleString();

  // Render Table Rows (8 columns)
  const tbody = document.getElementById('produccionTableBody');
  if (!tbody) return;

  if (filtered.length === 0) {
    tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted py-4">No hay productos en demanda de producción activa.</td></tr>`;
    return;
  }

  tbody.innerHTML = filtered.map(p => {
    let catBadge = 'bg-secondary';
    if (p.categoria === 'FRASCOS') catBadge = 'bg-emerald text-white';
    else if (p.categoria === 'GALONES') catBadge = 'bg-amber text-dark';
    else if (p.categoria === 'TAPAS') catBadge = 'bg-purple text-white';
    else if (p.categoria === 'ASAS') catBadge = 'bg-info text-dark';

    return `
      <tr>
        <td class="fw-bold font-monospace text-secondary">${escapeHtml(p.codigo_producto)}</td>
        <td>
          <div class="fw-bold text-white">${escapeHtml(p.nombre_producto)}</div>
        </td>
        <td>
          <span class="badge ${catBadge} fs-8 fw-semibold px-2 py-1">${escapeHtml(p.categoria)}</span>
        </td>
        <td class="text-center fw-bold text-white fs-6">${p.total_solicitado.toLocaleString()}</td>
        <td class="text-center fw-bold text-success fs-6">${p.total_entregado.toLocaleString()}</td>
        <td class="text-center fw-bold text-warning fs-6">${p.total_pendiente.toLocaleString()}</td>
        <td class="text-center">
          <span class="badge bg-primary-subtle text-primary border border-primary-subtle px-2 py-1">
            <i class="bi bi-people-fill me-1"></i> ${p.clientes.length} ${p.clientes.length === 1 ? 'cliente' : 'clientes'}
          </span>
        </td>
        <td class="text-center">
          <button class="btn-action-solid btn-view" onclick="produccionModule.openProductDetailModal('${p.id_producto}')" title="Ver Detalle del Producto">
            <i class="bi bi-eye-fill"></i>
          </button>
        </td>
      </tr>
    `;
  }).join('');
}

export function onSearchInput(val) {
  renderProduccionTable(currentOrders, currentProducts, val);
}

export function openProductDetailModal(idProducto) {
  let product = aggregatedProductsMap[idProducto];
  if (!product) {
    product = Object.values(aggregatedProductsMap).find(p => String(p.id_producto) === String(idProducto));
  }
  if (!product) {
    alert('No se encontró información de este producto en pedidos activos.');
    return;
  }

  const modalTitle = document.getElementById('produccionDetailTitle');
  const modalBody = document.getElementById('produccionDetailBody');

  if (modalTitle) {
    modalTitle.innerHTML = `<i class="bi bi-box-seam text-primary me-2"></i> ${escapeHtml(product.nombre_producto)} <span class="badge bg-secondary font-monospace ms-2">${escapeHtml(product.codigo_producto)}</span>`;
  }

  if (modalBody) {
    const todayStr = new Date().toISOString().split('T')[0];

    modalBody.innerHTML = `
      <!-- Product Summary Cards -->
      <div class="row g-2 mb-3">
        <div class="col-md-4 col-6">
          <div class="p-2 border rounded bg-body-tertiary text-center">
            <span class="text-muted fs-8 d-block text-uppercase fw-semibold">Pedidos Activos</span>
            <strong class="fs-6 text-primary">${product.clientes.length}</strong>
          </div>
        </div>
        <div class="col-md-4 col-6">
          <div class="p-2 border rounded bg-body-tertiary text-center">
            <span class="text-muted fs-8 d-block text-uppercase fw-semibold">Total Solicitado</span>
            <strong class="fs-6 text-white">${product.total_solicitado.toLocaleString()}</strong>
          </div>
        </div>
        <div class="col-md-4 col-12">
          <div class="p-2 border rounded bg-body-tertiary text-center">
            <span class="text-muted fs-8 d-block text-uppercase fw-semibold">Categoría</span>
            <strong class="fs-6 text-info">${escapeHtml(product.categoria)}</strong>
          </div>
        </div>
      </div>

      <!-- Table of Clients and Orders -->
      <h6 class="fw-bold mb-2 text-secondary"><i class="bi bi-people me-1"></i> Clientes y Órdenes que Solicitan este Producto:</h6>
      <div class="table-responsive border rounded mb-2">
        <table class="table custom-table table-sm align-middle mb-0">
          <thead class="bg-body-tertiary">
            <tr>
              <th>Cliente / Razón Social</th>
              <th>N° Pedido / Orden</th>
              <th>Fecha Pedido</th>
              <th>Establecimiento</th>
              <th class="text-center">Cantidad Solicitada</th>
              <th class="text-center">Estado Pedido</th>
            </tr>
          </thead>
          <tbody>
            ${product.clientes.map(c => {
              const sol = c.cantidad_solicitada || 0;
              return `
                <tr>
                  <td>
                    <div class="fw-bold text-white">${escapeHtml(c.nombre_cliente)}</div>
                    ${c.nro_documento ? `<span class="small text-muted">RUC: ${escapeHtml(c.nro_documento)}</span>` : ''}
                  </td>
                  <td>
                    <div class="fw-semibold text-primary">${escapeHtml(c.nro_pedido)}</div>
                    ${c.nro_orden ? `<div class="small text-muted">N° Orden: ${escapeHtml(c.nro_orden)}</div>` : '<div class="small text-muted fs-8">Sin N° Orden</div>'}
                  </td>
                  <td>
                    <div>F. Pedido: ${formatDate(c.fecha_pedido)}</div>
                    <div class="small text-muted">F. Entrega: ${formatDate(c.fecha_entrega)}</div>
                  </td>
                  <td><span class="small text-muted">${escapeHtml(c.establecimiento)}</span></td>
                  <td class="text-center fw-bold text-white fs-6">${sol.toLocaleString()}</td>
                  <td class="text-center"><span class="status-badge ${c.estado_pedido || 'PENDIENTE'}">${escapeHtml(c.estado_pedido || 'PENDIENTE')}</span></td>
                </tr>
              `;
            }).join('')}
          </tbody>
        </table>
      </div>
    `;
  }

  showBootstrapModal('produccionDetailModal');
}
