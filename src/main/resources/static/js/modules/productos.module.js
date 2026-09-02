import { escapeHtml, filterAndRankItems, showConfirmModal, paginateItems, renderPaginationUI } from '../helpers.js';
import { api } from '../api.js';
import { canDelete } from './auth.module.js';

let currentProducts = [];
let currentPage = 1;
const pageSize = 20;

export function changePage(delta) {
  currentPage += delta;
  filterProductos(document.getElementById('searchProductosInput')?.value || '');
}

export function resetPagination() {
  currentPage = 1;
}

export function renderProductosTable(products = [], searchQuery = '') {
  currentProducts = products || [];
  const searchInput = document.getElementById('searchProductosInput');
  if (searchInput && searchQuery) searchInput.value = searchQuery;
  filterProductos(searchQuery || (searchInput ? searchInput.value : ''));
}

export function filterProductos(queryStr = '') {
  const tbody = document.getElementById('productosTableBody');
  if (!tbody) return;
  tbody.innerHTML = '';

  const filtered = filterAndRankItems(
    currentProducts, 
    queryStr, 
    p => `#${p.id_producto || ''} ${p.id_producto || ''} ${p.nombre_producto || ''} ${p.tipo_producto || p.categoria || ''}`
  );

  const p = paginateItems(filtered, currentPage, pageSize);
  currentPage = p.currentPage;

  renderPaginationUI({
    containerId: 'productosPaginationContainer',
    currentPage: p.currentPage,
    totalPages: p.totalPages,
    totalItems: p.totalItems,
    startIndex: p.startIndex,
    endIndex: p.endIndex,
    onPageChangeName: 'productosModule.changePage'
  });

  if (p.items.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted py-4">No se encontraron productos coincidentes.</td></tr>`;
    return;
  }

  const allowDelete = canDelete();

  p.items.forEach(p => {
    const tipo = p.tipo_producto || p.categoria || 'General';
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td class="fw-bold text-muted">#${p.id_producto}</td>
      <td class="fw-semibold">${escapeHtml(p.nombre_producto)}</td>
      <td><span class="badge bg-primary text-white px-2.5 py-1 fs-8 fw-bold">${escapeHtml(tipo)}</span></td>
      <td><span class="status-badge COMPLETADO">ACTIVO</span></td>
      <!-- 1. Columna EDITAR -->
      <td class="text-center">
        <button type="button" class="btn-action-solid btn-edit" title="Editar producto" onclick="productosModule.openEditProductModal('${p.id_producto || p.id}')">
          <i class="bi bi-pencil-fill text-dark"></i>
        </button>
      </td>
      <!-- 2. Columna ELIMINAR -->
      <td class="text-center">
        ${allowDelete ? `
          <button type="button" class="btn-action-solid btn-delete" title="Eliminar producto" onclick="productosModule.deleteProduct('${p.id_producto || p.id}')">
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
}

export function openNewProductModal() {
  const modalElem = document.getElementById('modalProducto');
  if (!modalElem) return;

  document.getElementById('modalProductoTitle').innerHTML = '<i class="bi bi-box-seam me-2"></i>Registrar Producto';
  document.getElementById('modalProductoId').value = '';
  document.getElementById('modalProductoNombre').value = '';
  document.getElementById('modalProductoTipo').value = '';

  const modal = new bootstrap.Modal(modalElem);
  modal.show();
}

export function openEditProductModal(id) {
  const p = currentProducts.find(item => String(item.id_producto || item.id) === String(id));
  if (!p) return;

  const modalElem = document.getElementById('modalProducto');
  if (!modalElem) return;

  document.getElementById('modalProductoTitle').innerHTML = '<i class="bi bi-pencil-square me-2"></i>Editar Producto';
  document.getElementById('modalProductoId').value = p.id_producto;
  document.getElementById('modalProductoNombre').value = p.nombre_producto;
  
  const currentTipo = (p.tipo_producto || p.categoria || 'FRASCOS').toUpperCase();
  const selectElem = document.getElementById('modalProductoTipo');
  if (selectElem) {
    selectElem.value = currentTipo;
    // If not matched, fallback to FRASCOS
    if (!selectElem.value) {
      selectElem.value = 'FRASCOS';
    }
  }

  const modal = new bootstrap.Modal(modalElem);
  modal.show();
}

export async function saveProductFromModal() {
  const idStr = document.getElementById('modalProductoId')?.value;
  const nombre = document.getElementById('modalProductoNombre')?.value.trim();
  const tipo = document.getElementById('modalProductoTipo')?.value.trim() || 'General';

  if (!nombre) {
    alert('Por favor ingrese el nombre del producto.');
    return;
  }

  const payload = {
    nombre_producto: nombre,
    tipo_producto: tipo,
    categoria: tipo
  };

  const modalElem = document.getElementById('modalProducto');
  const modal = bootstrap.Modal.getInstance(modalElem);

  if (idStr) {
    // Edit existing product
    const id = parseInt(idStr, 10);
    const res = await api.updateProducto(id, payload);
    if (res) {
      const idx = currentProducts.findIndex(p => p.id_producto === id);
      if (idx !== -1) {
        currentProducts[idx].nombre_producto = nombre;
        currentProducts[idx].tipo_producto = tipo;
        currentProducts[idx].categoria = tipo;
      }
    }
  } else {
    // Register new product
    const res = await api.addProducto(payload);
    if (res) {
      currentProducts.unshift({
        id_producto: res.id_producto || (currentProducts.length + 1),
        nombre_producto: nombre,
        tipo_producto: tipo,
        categoria: tipo
      });
    }
  }

  if (modal) modal.hide();

  // Reset modal fields for next product registration
  const idElem = document.getElementById('modalProductoId');
  const nombreElem = document.getElementById('modalProductoNombre');
  if (idElem) idElem.value = '';
  if (nombreElem) nombreElem.value = '';

  filterProductos();
}

export async function deleteProduct(id) {
  if (!canDelete()) {
    await showConfirmModal({
      title: 'Acceso Restringido',
      message: 'El perfil de Operaciones no tiene permisos para eliminar productos.',
      icon: 'bi-shield-lock-fill',
      iconBg: 'rgba(234, 179, 8, 0.15)',
      iconColor: '#eab308',
      confirmText: 'Entendido',
      confirmBtnClass: 'btn-warning text-dark',
      cancelText: 'Cerrar'
    });
    return;
  }
  const p = currentProducts.find(item => String(item.id_producto || item.id) === String(id));
  if (!p) return;

  const confirmed = await showConfirmModal({
    title: '¿Eliminar Producto?',
    message: `¿Está seguro de eliminar el producto #${id} "${p.nombre_producto}"?`,
    icon: 'bi-trash3-fill',
    iconBg: 'rgba(239, 68, 68, 0.15)',
    iconColor: '#ef4444',
    confirmText: 'Eliminar Producto',
    confirmBtnClass: 'btn-danger',
    cancelText: 'Cancelar'
  });

  if (!confirmed) return;

  await api.deleteProducto(id);
  currentProducts = currentProducts.filter(item => String(item.id_producto || item.id) !== String(id));
  filterProductos();
}
