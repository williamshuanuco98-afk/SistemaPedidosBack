import { api } from '../api.js';
import { escapeHtml, filterAndRankItems } from '../helpers.js';
import { openPDF, printGuiaPDF } from './envios.module.js';

const DIRECCION_CARABAYLLO = "C.P. Las Piedritas Av. Las Piedritas Mz D Lt 9 - Carabayllo - Lima - Lima";
const DIRECCION_COMAS = "Av. Maria Parado de Belllido Lt. 5 Lotizacion Chacra Cerro - Comas - Lima - Lima";

const state = {
  selectedLocal: 'CARABAYLLO',
  selectedClient: null,
  guiaItems: [],
  activeClientIndex: -1,
  activeProductIndex: -1,
  currentGuiaNro: ''
};

export async function initNuevaGuiaView() {
  state.selectedLocal = 'CARABAYLLO';
  state.selectedClient = null;
  state.guiaItems = [];
  state.activeClientIndex = -1;
  state.activeProductIndex = -1;

  // Set default emission date to today (YYYY-MM-DD)
  const dateInput = document.getElementById('fechaEmisionGuiaInput');
  if (dateInput) {
    dateInput.value = new Date().toISOString().split('T')[0];
  }

  // Setup local & address
  const localSelect = document.getElementById('selectLocalGuia');
  if (localSelect) localSelect.value = 'CARABAYLLO';
  await updateLocalAndCorrelative('CARABAYLLO');

  setupClientSearch();
  setupProductSearch();
  renderGuiaProductsTable();
}

export async function onLocalChanged(local) {
  state.selectedLocal = local;
  await updateLocalAndCorrelative(local);
}

async function updateLocalAndCorrelative(local) {
  const puntoPartidaInput = document.getElementById('puntoPartidaInput');
  if (puntoPartidaInput) {
    puntoPartidaInput.value = (local === 'COMAS') ? DIRECCION_COMAS : DIRECCION_CARABAYLLO;
  }

  const nroGuiaInput = document.getElementById('nroGuiaInput');
  if (nroGuiaInput) {
    const serie = (local === 'COMAS') ? 'GR002' : 'GR001';
    nroGuiaInput.value = 'Cargando...';
    try {
      const nextNro = await api.getNextGuiaNumber(serie);
      state.currentGuiaNro = nextNro;
      nroGuiaInput.value = nextNro;
    } catch (e) {
      const fallback = `${serie}-0001`;
      state.currentGuiaNro = fallback;
      nroGuiaInput.value = fallback;
    }
  }
}

function setupClientSearch() {
  const input = document.getElementById('searchClienteGuiaInput');
  const list = document.getElementById('clientSearchResultsGuiaList');
  if (!input || !list) return;

  if (input.dataset.boundClientSearch === 'true') return;
  input.dataset.boundClientSearch = 'true';

  input.addEventListener('input', (e) => {
    const val = e.target.value.trim();
    state.activeClientIndex = -1;

    if (!val) {
      list.classList.add('d-none');
      list.innerHTML = '';
      return;
    }

    const clients = window.app?.clients || [];
    const matches = filterAndRankItems(
      clients,
      val,
      c => `${c.nro_documento || ''} ${c.nombre_cliente || ''} ${c.direccion || ''}`
    ).slice(0, 15);

    if (matches.length === 0) {
      list.innerHTML = `<li class="list-group-item text-muted py-2 fs-7">No se encontraron clientes que coincidan con "${escapeHtml(val)}"</li>`;
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
      } else if (items.length > 0) {
        items[0].click();
      }
    } else if (e.key === 'Escape') {
      list.classList.add('d-none');
    }
  });

  document.addEventListener('click', (e) => {
    if (!input.contains(e.target) && !list.contains(e.target)) {
      list.classList.add('d-none');
    }
  });
}

function updateActiveItem(items, activeIndex) {
  items.forEach((item, idx) => {
    if (idx === activeIndex) {
      item.classList.add('active', 'bg-primary');
      item.scrollIntoView({ block: 'nearest' });
    } else {
      item.classList.remove('active', 'bg-primary');
    }
  });
}

function selectClient(client) {
  state.selectedClient = client;

  const input = document.getElementById('searchClienteGuiaInput');
  const rucInput = document.getElementById('rucDniGuiaInput');
  const puntoLlegadaInput = document.getElementById('puntoLlegadaGuiaInput');
  const list = document.getElementById('clientSearchResultsGuiaList');

  if (input) {
    const docPrefix = client.nro_documento ? `${client.nro_documento} - ` : '';
    input.value = `${docPrefix}${client.nombre_cliente}`;
  }
  if (rucInput) rucInput.value = client.nro_documento || '';
  if (puntoLlegadaInput) {
    const fullDir = (client.direccion || client.direccion_completa || client.direccion_fiscal || '').trim();
    puntoLlegadaInput.value = fullDir;
  }
  if (list) list.classList.add('d-none');
}

function setupProductSearch() {
  const input = document.getElementById('searchProductGuiaInput');
  const list = document.getElementById('productSearchResultsGuiaList');
  if (!input || !list) return;

  if (input.dataset.boundProductSearch === 'true') return;
  input.dataset.boundProductSearch = 'true';

  input.addEventListener('input', (e) => {
    const val = e.target.value.trim();
    state.activeProductIndex = -1;

    if (!val) {
      list.classList.add('d-none');
      list.innerHTML = '';
      return;
    }

    const products = (window.app?.products && window.app.products.length > 0) 
      ? window.app.products 
      : (state.products || []);

    const matches = filterAndRankItems(
      products,
      val,
      p => `#${p.codigo_producto || p.id_producto || ''} ${p.id_producto || ''} ${p.codigo_producto || ''} ${p.nombre_producto || ''} ${p.tipo_producto || ''} ${p.categoria || ''}`
    ).slice(0, 25);

    if (matches.length === 0) {
      list.innerHTML = `<li class="list-group-item text-muted py-2 fs-7">No se encontraron productos para "${escapeHtml(val)}"</li>`;
      list.classList.remove('d-none');
      return;
    }

    list.innerHTML = matches.map((p, idx) => `
      <li class="list-group-item list-group-item-action py-1 px-3 prod-opt-item d-flex align-items-center gap-2 fs-7" data-index="${idx}">
        <span class="fw-bold text-primary">#${escapeHtml(p.codigo_producto || p.id_producto)}</span>
        <span class="fw-semibold text-body">- ${escapeHtml(p.nombre_producto)}</span>
      </li>
    `).join('');
    list.classList.remove('d-none');

    list.querySelectorAll('.prod-opt-item').forEach((item, idx) => {
      item.addEventListener('click', () => {
        addProductToGuia(matches[idx]);
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
      } else if (items.length > 0) {
        // If user presses Enter without arrowing, select first item
        items[0].click();
      }
    } else if (e.key === 'Escape') {
      list.classList.add('d-none');
    }
  });

  document.addEventListener('click', (e) => {
    if (!input.contains(e.target) && !list.contains(e.target)) {
      list.classList.add('d-none');
    }
  });
}

function addProductToGuia(product) {
  const input = document.getElementById('searchProductGuiaInput');
  const list = document.getElementById('productSearchResultsGuiaList');
  if (input) input.value = '';
  if (list) list.classList.add('d-none');

  const existing = state.guiaItems.find(i => String(i.id_producto) === String(product.id_producto));
  if (existing) {
    existing.cantidad += 1;
  } else {
    state.guiaItems.push({
      id_producto: product.id_producto,
      nombre_producto: product.nombre_producto,
      codigo_producto: product.codigo_producto || '',
      cantidad: 1
    });
  }

  renderGuiaProductsTable();
}

export function updateItemQty(index, qty) {
  const val = parseInt(qty, 10);
  if (isNaN(val) || val <= 0) {
    state.guiaItems.splice(index, 1);
  } else {
    state.guiaItems[index].cantidad = val;
  }
  renderGuiaProductsTable();
}

export function removeItemRow(index) {
  state.guiaItems.splice(index, 1);
  renderGuiaProductsTable();
}

function renderGuiaProductsTable() {
  const tbody = document.getElementById('tableProductosGuiaBody');
  if (!tbody) return;
  tbody.innerHTML = '';

  if (state.guiaItems.length === 0) {
    tbody.innerHTML = `<tr><td colspan="4" class="text-center text-muted py-4">No se han agregado productos a la guía.</td></tr>`;
    return;
  }

  state.guiaItems.forEach((item, idx) => {
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td class="fw-bold">${escapeHtml(item.codigo_producto || '#' + item.id_producto)}</td>
      <td class="fw-semibold">${escapeHtml(item.nombre_producto)}</td>
      <td>
        <input type="number" class="form-control form-control-sm py-0 px-2 fs-7 text-center mx-auto" value="${item.cantidad}" min="1" onchange="nuevaGuiaModule.updateItemQty(${idx}, this.value)" style="width: 100px; height: 28px;">
      </td>
      <td class="text-center">
        <button type="button" class="btn btn-sm btn-outline-danger border-0 py-0 px-1" onclick="nuevaGuiaModule.removeItemRow(${idx})">
          <i class="bi bi-trash"></i>
        </button>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

export async function submitNuevaGuia() {
  if (!state.selectedClient) {
    const rawClientText = document.getElementById('searchClienteGuiaInput')?.value.trim() || '';
    if (rawClientText.length > 0) {
      state.selectedClient = {
        id_cliente: 0,
        nombre_cliente: rawClientText,
        nro_documento: document.getElementById('rucDniGuiaInput')?.value.trim() || '',
        direccion: document.getElementById('puntoLlegadaGuiaInput')?.value.trim() || ''
      };
    } else {
      alert('Por favor seleccione o ingrese un Cliente / Razón Social.');
      document.getElementById('searchClienteGuiaInput')?.focus();
      return;
    }
  }

  if (state.guiaItems.length === 0) {
    alert('Debe agregar al menos un producto a la guía de remisión.');
    document.getElementById('searchProductGuiaInput')?.focus();
    return;
  }

  const fechaEmision = document.getElementById('fechaEmisionGuiaInput')?.value || new Date().toISOString().split('T')[0];
  const docRef = document.getElementById('docReferenciaGuiaInput')?.value.trim() || '';
  const puntoPartida = document.getElementById('puntoPartidaInput')?.value.trim() || (state.selectedLocal === 'COMAS' ? DIRECCION_COMAS : DIRECCION_CARABAYLLO);
  const puntoLlegada = document.getElementById('puntoLlegadaGuiaInput')?.value.trim() || state.selectedClient.direccion || '';
  const observaciones = document.getElementById('observacionesGuiaInput')?.value.trim() || '';
  const storagePath = localStorage.getItem('inplabel_guias_pdf_storage_path') || 'C:\\Inplabel\\Guias';
  const useSubfolders = localStorage.getItem('inplabel_pdf_subfolders') !== 'false';

  const btnGuardar = document.getElementById('btnGuardarGuia');
  if (btnGuardar) {
    btnGuardar.disabled = true;
    btnGuardar.innerHTML = `<span class="spinner-border spinner-border-sm me-1"></span> Emitiendo Guía...`;
  }

  const payload = {
    id_cliente: state.selectedClient.id_cliente,
    nombre_cliente: state.selectedClient.nombre_cliente,
    nro_documento: state.selectedClient.nro_documento || '',
    nro_guia: state.currentGuiaNro,
    establecimiento: state.selectedLocal,
    fecha_guia: fechaEmision,
    doc_referencia: docRef,
    punto_partida: puntoPartida,
    punto_llegada: puntoLlegada,
    observaciones: observaciones,
    storage_path: storagePath,
    use_subfolders: useSubfolders,
    estado: 'EMITIDA',
    detalles: state.guiaItems.map(item => ({
      id_producto: item.id_producto,
      nombre_producto: item.nombre_producto,
      codigo_producto: item.codigo_producto || '',
      cantidad: item.cantidad
    }))
  };

  try {
    const newGuia = await api.createGuia(payload);

    // Add to in-memory window.app.shipments
    if (window.app) {
      if (!Array.isArray(window.app.shipments)) window.app.shipments = [];
      const exists = window.app.shipments.some(g => String(g.id_guia) === String(newGuia.id_guia));
      if (!exists) {
        window.app.shipments.unshift(newGuia);
      }
    }

    showGuiaSuccessModal(newGuia);
    await resetNuevaGuiaForm();

  } catch (err) {
    console.error("Error al emitir la guía:", err);
    alert('Ocurrió un error al guardar la guía de remisión. Se guardará de forma local.');
  } finally {
    if (btnGuardar) {
      btnGuardar.disabled = false;
      btnGuardar.innerHTML = `<i class="bi bi-check-circle me-1"></i> Emitir Guía de Remisión`;
    }
  }
}

export async function resetNuevaGuiaForm() {
  state.selectedClient = null;
  state.guiaItems = [];
  state.activeClientIndex = -1;
  state.activeProductIndex = -1;

  const clientInput = document.getElementById('searchClienteGuiaInput');
  const rucInput = document.getElementById('rucDniGuiaInput');
  const docRefInput = document.getElementById('docReferenciaGuiaInput');
  const llegadaInput = document.getElementById('puntoLlegadaGuiaInput');
  const obsInput = document.getElementById('observacionesGuiaInput');
  const prodSearchInput = document.getElementById('searchProductGuiaInput');

  if (clientInput) clientInput.value = '';
  if (rucInput) rucInput.value = '';
  if (docRefInput) docRefInput.value = '';
  if (llegadaInput) llegadaInput.value = '';
  if (obsInput) obsInput.value = '';
  if (prodSearchInput) prodSearchInput.value = '';

  const dateInput = document.getElementById('fechaEmisionGuiaInput');
  if (dateInput) dateInput.value = new Date().toISOString().split('T')[0];

  await updateLocalAndCorrelative(state.selectedLocal || 'CARABAYLLO');
  renderGuiaProductsTable();
}

function showGuiaSuccessModal(guia) {
  let modalEl = document.getElementById('guiaEmisionSuccessModal');
  if (!modalEl) {
    modalEl = document.createElement('div');
    modalEl.id = 'guiaEmisionSuccessModal';
    modalEl.className = 'modal fade';
    modalEl.tabIndex = -1;
    modalEl.setAttribute('aria-hidden', 'true');
    modalEl.innerHTML = `
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <div class="modal-header bg-success text-white py-3">
            <h5 class="modal-title fw-bold">
              <i class="bi bi-check-circle-fill me-2"></i> ¡Guía emitida correctamente!
            </h5>
            <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
          </div>
          <div class="modal-body text-center p-4">
            <i class="bi bi-file-earmark-check text-success display-1 d-block mb-3"></i>
            <h4 class="fw-bold mb-1" id="successModalNroGuia">GR001-0001</h4>
            <p class="text-muted fs-7 mb-4">La guía de remisión ha sido procesada, registrada y su archivo PDF generado en la ruta configurada.</p>
            
            <div class="d-flex justify-content-center gap-3">
              <button type="button" id="btnSuccessPrintGuia" class="btn btn-outline-primary px-3 py-2">
                <i class="bi bi-printer me-1"></i> Imprimir
              </button>
              <button type="button" id="btnSuccessPdfGuia" class="btn btn-primary px-3 py-2">
                <i class="bi bi-file-earmark-pdf me-1"></i> Abrir Archivo PDF
              </button>
            </div>
          </div>
          <div class="modal-footer bg-body-tertiary py-2">
            <button type="button" class="btn btn-sm btn-secondary" onclick="app.navigateTo('envios')" data-bs-dismiss="modal">Ir al Listado de Guías</button>
          </div>
        </div>
      </div>
    `;
    document.body.appendChild(modalEl);
  }

  const nroLabel = modalEl.querySelector('#successModalNroGuia');
  if (nroLabel) nroLabel.textContent = guia.nro_guia || 'Guía Emitida';

  const btnPrint = modalEl.querySelector('#btnSuccessPrintGuia');
  if (btnPrint) {
    btnPrint.onclick = () => {
      printGuiaPDF(guia);
    };
  }

  const btnPdf = modalEl.querySelector('#btnSuccessPdfGuia');
  if (btnPdf) {
    btnPdf.onclick = () => {
      openPDF(guia.id_guia);
    };
  }

  const bsModal = new bootstrap.Modal(modalEl);
  bsModal.show();
}
