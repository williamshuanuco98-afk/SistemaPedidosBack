import { api, BASE_URL } from '../api.js';
import { escapeHtml, formatDate, showBootstrapModal, hideBootstrapModal, paginateItems, renderPaginationUI, filterAndRankItems } from '../helpers.js';

let currentShipments = [];
let currentSearchQuery = '';
let currentPage = 1;
const pageSize = 20;

export function changePage(delta) {
  currentPage += delta;
  renderEnviosTable(currentShipments, currentSearchQuery);
}

export function resetPagination() {
  currentPage = 1;
}

export function setupDefaultDateFiltersEnvios() {
  const dateFromElem = document.getElementById('filterGuiaDateFrom');
  const dateToElem = document.getElementById('filterGuiaDateTo');

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

export function renderEnviosTable(shipments = [], searchQuery = '') {
  currentShipments = shipments || [];
  if (searchQuery !== undefined) currentSearchQuery = searchQuery;

  setupDefaultDateFiltersEnvios();

  const countBadge = document.getElementById('shipmentsCountBadge') || document.getElementById('guiasCountBadge');
  const tbody = document.getElementById('enviosTableBody') || document.getElementById('guiasTableBody');
  if (!tbody) return;

  const searchInput = document.getElementById('searchGuiaInput');
  const query = (searchInput?.value || currentSearchQuery || '').toLowerCase().trim();

  const dateFrom = document.getElementById('filterGuiaDateFrom')?.value;
  const dateTo = document.getElementById('filterGuiaDateTo')?.value;
  const establishment = document.getElementById('filterGuiaEstablishment')?.value || 'ALL';

  const filtered = currentShipments.filter(s => {
    // 0. Only show Official Guías de Remisión (GR...) - Exclude order delivery progress vouchers (TT...)
    const nroUpper = (s.nro_guia || '').toUpperCase();
    if (nroUpper.startsWith('TT') || (!nroUpper.startsWith('GR') && s.id_pedido)) {
      return false;
    }

    // 1. Text Search Filter
    let matchQuery = true;
    if (query) {
      const nro = (s.nro_guia || '').toLowerCase();
      const cliente = (s.nombre_cliente || '').toLowerCase();
      const docRef = (s.doc_referencia || '').toLowerCase();
      const ruc = (s.nro_documento || '').toLowerCase();
      matchQuery = nro.includes(query) || cliente.includes(query) || docRef.includes(query) || ruc.includes(query);
    }

    // 2. Date Filter (se ignora si se está buscando por texto específico)
    let matchDate = true;
    const fDate = s.fecha_guia || s.fecha_emision;
    if (!query && fDate) {
      const dateStr = String(fDate).substring(0, 10);
      if (dateFrom && dateStr < dateFrom) matchDate = false;
      if (dateTo && dateStr > dateTo) matchDate = false;
    }

    // 3. Establishment / Local Filter
    let matchEstab = true;
    if (establishment && establishment !== 'ALL') {
      const sEstab = (s.establecimiento || (s.nro_guia && s.nro_guia.startsWith('GR002') ? 'COMAS' : 'CARABAYLLO')).toUpperCase();
      matchEstab = sEstab.includes(establishment.toUpperCase());
    }

    return matchQuery && matchDate && matchEstab;
  });

  if (countBadge) countBadge.textContent = `${filtered.length} guías`;

  const p = paginateItems(filtered, currentPage, pageSize);
  currentPage = p.currentPage;

  renderPaginationUI({
    containerId: 'enviosPaginationContainer',
    currentPage: p.currentPage,
    totalPages: p.totalPages,
    totalItems: p.totalItems,
    startIndex: p.startIndex,
    endIndex: p.endIndex,
    onPageChangeName: 'enviosModule.changePage'
  });

  if (p.items.length === 0) {
    tbody.innerHTML = `<tr><td colspan="10" class="text-center text-muted py-4">No se encontraron guías de remisión registradas para los filtros aplicados.</td></tr>`;
    return;
  }

  tbody.innerHTML = p.items.map(s => {
    const targetId = s.id_guia || s.id;
    const isAnulada = (s.estado === 'ANULADA');
    const badgeClass = isAnulada ? 'CANCELADO' : 'COMPLETADO';
    const statusText = isAnulada ? 'ANULADA' : (s.estado || 'EMITIDA');

    return `
      <tr>
        <td class="fw-bold font-monospace text-primary">${escapeHtml(s.nro_guia || ('GR001-' + String(targetId).padStart(4, '0')))}</td>
        <td>${formatDate(s.fecha_guia || s.fecha_emision)}</td>
        <td>
          <div class="fw-bold text-body">${escapeHtml(s.nombre_cliente || 'Cliente General')}</div>
        </td>
        <td>
          <span class="font-monospace">${escapeHtml(s.nro_documento || '-')}</span>
        </td>
        <td>
          <span class="status-badge ${badgeClass}">${statusText}</span>
        </td>
        <!-- 1. Columna DETALLES -->
        <td class="text-center">
          <button class="btn-action-solid btn-view" title="Ver Detalles" onclick="enviosModule.viewGuiaDetail('${targetId}')">
            <i class="bi bi-eye-fill"></i>
          </button>
        </td>
        <!-- 2. Columna PDF -->
        <td class="text-center">
          <button class="btn-action-solid btn-pdf" title="Ver PDF" onclick="enviosModule.openPDF('${targetId}')">
            <i class="bi bi-file-earmark-pdf-fill"></i>
          </button>
        </td>
        <!-- 3. Columna PRINT -->
        <td class="text-center">
          <button class="btn-action-solid btn-print" title="Imprimir" onclick="enviosModule.printPDF('${targetId}')">
            <i class="bi bi-printer-fill"></i>
          </button>
        </td>
        <!-- 4. Columna EDITAR -->
        <td class="text-center">
          ${!isAnulada ? `
            <button class="btn-action-solid btn-edit" title="Editar Guía" onclick="enviosModule.openEditarGuiaModal('${targetId}')">
              <i class="bi bi-pencil-fill"></i>
            </button>
          ` : `<span class="text-muted small">-</span>`}
        </td>
        <!-- 5. Columna ANULAR -->
        <td class="text-center">
          ${!isAnulada ? `
            <button class="btn-action-solid btn-cancel" title="Anular Guía" onclick="enviosModule.openAnularModal('${targetId}', '${escapeHtml(s.nro_guia || '')}')">
              <i class="bi bi-x-circle-fill"></i>
            </button>
          ` : `<span class="text-muted small">-</span>`}
        </td>
      </tr>
    `;
  }).join('');
}

export function onSearchInput(val) {
  renderEnviosTable(currentShipments, val);
}

export function openAnularModal(idGuia, nroGuia) {
  const modalEl = document.getElementById('modalAnularGuia');
  if (!modalEl) {
    anularGuia(idGuia);
    return;
  }
  const idInput = document.getElementById('anularGuiaIdInput');
  const label = document.getElementById('anularNroGuiaLabel');
  const motivo = document.getElementById('motivoAnulacionInput');
  if (idInput) idInput.value = idGuia;
  if (label) label.textContent = nroGuia || ('ID #' + idGuia);
  if (motivo) motivo.value = '';
  showBootstrapModal('modalAnularGuia');
}

export async function confirmAnularGuia() {
  const idInput = document.getElementById('anularGuiaIdInput');
  const motivoInput = document.getElementById('motivoAnulacionInput');
  const idGuia = idInput?.value;
  const motivo = motivoInput?.value.trim() || 'Anulada por el usuario';

  if (!idGuia) return;

  try {
    await api.anularGuia(idGuia, motivo);
    const idx = currentShipments.findIndex(s => String(s.id_guia) === String(idGuia));
    if (idx !== -1) {
      currentShipments[idx].estado = 'ANULADA';
      currentShipments[idx].motivo_anulacion = motivo;
    }
    hideBootstrapModal('modalAnularGuia');
    renderEnviosTable(currentShipments, currentSearchQuery);
  } catch (err) {
    console.error("Error al anular guía:", err);
    alert('Error al anular la guía: ' + (err.message || 'Error de servidor'));
  }
}

export function viewGuiaDetail(idGuia) {
  const guia = currentShipments.find(s => String(s.id_guia) === String(idGuia));
  if (!guia) {
    alert('No se encontró la información de la guía seleccionada.');
    return;
  }

  const modalTitle = document.getElementById('guiaDetailTitle');
  const modalBody = document.getElementById('guiaDetailBody');

  if (modalTitle) {
    modalTitle.innerHTML = `<i class="bi bi-truck text-primary me-2"></i> Guía de Remisión ${escapeHtml(guia.nro_guia || '')}`;
  }

  if (modalBody) {
    const isAnulada = (guia.estado === 'ANULADA');
    const badgeClass = isAnulada ? 'bg-danger' : 'bg-success';
    const statusText = isAnulada ? 'ANULADA' : 'EMITIDA';

    modalBody.innerHTML = `
      <div class="row g-3 mb-3">
        <div class="col-md-6">
          <div class="p-3 border rounded bg-body-tertiary">
            <span class="text-muted fs-8 d-block text-uppercase fw-semibold">Cliente Destinatario</span>
            <strong class="fs-6 text-primary">${escapeHtml(guia.nombre_cliente || 'Cliente')}</strong>
            <div class="small text-muted mt-1">RUC/DNI: <strong>${escapeHtml(guia.nro_documento || '-')}</strong></div>
            <div class="small text-muted">Punto Llegada: <strong>${escapeHtml(guia.punto_llegada || guia.direccion_destino || '-')}</strong></div>
          </div>
        </div>
        <div class="col-md-6">
          <div class="p-3 border rounded bg-body-tertiary">
            <span class="text-muted fs-8 d-block text-uppercase fw-semibold">Datos de Emisión</span>
            <div>N° Guía: <strong class="text-primary font-monospace">${escapeHtml(guia.nro_guia || '')}</strong></div>
            <div>Fecha Emisión: <strong>${formatDate(guia.fecha_guia || guia.fecha_emision)}</strong></div>
            <div>Punto Partida: <strong>${escapeHtml(guia.punto_partida || 'Carabayllo')}</strong></div>
            <div class="mt-1">Estado: <span class="badge ${badgeClass}">${statusText}</span></div>
          </div>
        </div>
      </div>

      <h6 class="fw-bold mb-2 text-secondary"><i class="bi bi-box-seam me-1"></i> Productos Despachados:</h6>
      <div class="table-responsive border rounded mb-3">
        <table class="table custom-table table-sm align-middle mb-0">
          <thead class="bg-body-tertiary">
            <tr>
              <th style="width: 50px;">#</th>
              <th style="width: 120px;">Código</th>
              <th>Descripción del Producto</th>
              <th class="text-center" style="width: 90px;">U.M.</th>
              <th class="text-center" style="width: 120px;">Cantidad</th>
            </tr>
          </thead>
          <tbody>
            ${(guia.detalles || []).map((d, idx) => `
              <tr>
                <td>${idx + 1}</td>
                <td class="font-monospace text-secondary fw-semibold">${escapeHtml(d.codigo_producto || '#' + d.id_producto)}</td>
                <td class="fw-bold">${escapeHtml(d.nombre_producto || 'Producto')}</td>
                <td class="text-center fw-bold text-secondary">${escapeHtml(d.unidad_medida || 'UNID')}</td>
                <td class="text-center fw-bold text-success fs-6">${Number(d.cantidad || 0).toLocaleString()}</td>
              </tr>
            `).join('')}
          </tbody>
        </table>
      </div>

      ${guia.observaciones ? `
        <div class="p-2 border rounded bg-body-tertiary small">
          <strong>Observaciones:</strong> ${escapeHtml(guia.observaciones)}
        </div>
      ` : ''}
    `;
  }

  const printBtn = document.getElementById('btnPrintGuiaModal');
  if (printBtn) {
    printBtn.onclick = () => printGuiaPDF(guia);
  }

  showBootstrapModal('guiaDetailModal');
}

export function openPDF(idGuia) {
  if (!idGuia || idGuia === 'undefined' || idGuia === 'null') {
    alert('No se pudo determinar el código de la guía para abrir el PDF.');
    return;
  }
  const numId = Number(idGuia);
  if (isNaN(numId) || numId <= 0 || numId > 2147483647) {
    alert('El identificador de la guía (' + idGuia + ') no es válido en el servidor.');
    return;
  }
  const savedPath = localStorage.getItem('inplabel_guias_pdf_storage_path') || 'C:\\Inplabel\\Guias';
  const savedSubfolders = localStorage.getItem('inplabel_pdf_subfolders') !== 'false';
  const pdfUrl = `${BASE_URL}/guias/${numId}/pdf?storageDir=${encodeURIComponent(savedPath)}&useSubfolders=${savedSubfolders}`;
  window.open(pdfUrl, '_blank');
}

export async function printPDF(idGuia) {
  if (!idGuia || idGuia === 'undefined' || idGuia === 'null') {
    alert('No se pudo determinar el código de la guía para imprimir.');
    return;
  }
  const numId = Number(idGuia);
  if (isNaN(numId) || numId <= 0 || numId > 2147483647) {
    alert('El identificador de la guía (' + idGuia + ') no es válido.');
    return;
  }
  let guia = currentShipments.find(s => String(s.id_guia || s.id) === String(numId));
  if (!guia) {
    try {
      guia = await api.getGuiaById(numId);
    } catch (e) {
      console.warn("No se pudo obtener la guía por API:", e);
    }
  }

  if (guia) {
    printGuiaDirectWithoutNewTab(guia);
    return;
  }

  // Fallback: If not in memory, print PDF stream directly via invisible iframe
  const savedPath = localStorage.getItem('inplabel_guias_pdf_storage_path') || 'C:\\Inplabel\\Guias';
  const savedSubfolders = localStorage.getItem('inplabel_pdf_subfolders') !== 'false';
  const pdfUrl = `${BASE_URL}/guias/${numId}/pdf?storageDir=${encodeURIComponent(savedPath)}&useSubfolders=${savedSubfolders}`;

  const iframe = document.createElement('iframe');
  iframe.style.position = 'fixed';
  iframe.style.right = '0';
  iframe.style.bottom = '0';
  iframe.style.width = '0';
  iframe.style.height = '0';
  iframe.style.border = '0';
  iframe.src = pdfUrl;
  document.body.appendChild(iframe);
  iframe.onload = () => {
    try {
      iframe.contentWindow.focus();
      iframe.contentWindow.print();
    } catch (e) {
      console.error("Error al imprimir iframe:", e);
    }
    setTimeout(() => {
      try { document.body.removeChild(iframe); } catch (e) { }
    }, 3000);
  };
}

export async function anularGuia(idGuia) {
  if (!confirm('¿Está seguro de que desea anular esta Guía de Remisión? Esta acción registrará la guía como ANULADA.')) {
    return;
  }

  try {
    const updated = await api.anularGuia(idGuia);
    const idx = currentShipments.findIndex(s => String(s.id_guia) === String(idGuia));
    if (idx !== -1) {
      currentShipments[idx].estado = 'ANULADA';
    }
    renderEnviosTable(currentShipments, currentSearchQuery);
    alert('Guía de Remisión anulada exitosamente.');
  } catch (err) {
    console.error("Error anulando guía:", err);
    alert('Error al anular la guía: ' + (err.message || 'Error de servidor'));
  }
}

function renderGuiaHalfHTML(guia, copiaNombre) {
  const detalles = guia.detalles || [];
  const nroGuia = guia.nro_guia || 'GR001-0001';
  const fechaStr = formatDate(guia.fecha_guia || guia.fecha_emision);
  const clienteNombre = escapeHtml(guia.nombre_cliente || 'Cliente General');
  const clienteRuc = escapeHtml(guia.nro_documento || '-');
  
  const docRefVal = String(guia.doc_referencia || guia.nro_orden || guia.nro_pedido || '').trim();
  const docRef = escapeHtml((docRefVal && docRefVal !== 'null' && docRefVal !== 'undefined') ? docRefVal : '-');

  const puntoPartida = escapeHtml(guia.punto_partida || 'C.P. Las Piedritas Av. Las Piedritas Mz D Lt 9 - CARABAYLLO - LIMA - LIMA');
  const puntoLlegada = escapeHtml(guia.punto_llegada || guia.direccion_destino || 'Dirección del cliente - LIMA - LIMA');
  const observaciones = escapeHtml(guia.observaciones || '');
  const isAnulada = (guia.estado === 'ANULADA');

  const rowsHTML = detalles.map((item, idx) => `
    <tr>
      <td style="text-align: center; font-weight: bold; border: 1px solid #444; padding: 5px 3px;">${idx + 1}</td>
      <td style="border: 1px solid #444; padding: 5px 4px; font-weight: 500;">
        ${escapeHtml(item.nombre_producto || 'Producto')}
      </td>
      <td style="text-align: center; border: 1px solid #444; padding: 5px 3px;">${escapeHtml(item.unidad_medida || 'UNID')}</td>
      <td style="text-align: center; font-weight: bold; border: 1px solid #444; padding: 5px 3px;">${item.cantidad || 1}</td>
    </tr>
  `).join('');

  const emptyStateHTML = `
    <tr>
      <td colspan="4" style="text-align: center; border: 1px solid #444; padding: 8px; color: #666;">Sin productos especificados</td>
    </tr>
  `;

  return `
    <div style="width: 48.5%; box-sizing: border-box; font-family: Arial, Helvetica, sans-serif; font-size: 8.5px; color: #000; position: relative; display: flex; flex-direction: column; justify-content: space-between; min-height: 480px;">
      ${isAnulada ? `<div style="position: absolute; top: 40%; left: 15%; transform: rotate(-30deg); font-size: 36px; font-weight: bold; color: rgba(220, 53, 69, 0.4); border: 3px solid rgba(220, 53, 69, 0.4); padding: 8px 16px; border-radius: 8px; z-index: 100;">GUÍA ANULADA</div>` : ''}

      <div>
        <!-- 1. Cabecera (Logo + Datos Empresa + Caja RUC) -->
        <table style="width: 100%; border-collapse: collapse; margin-bottom: 7px;">
          <tr>
            <td style="width: 22%; vertical-align: middle;">
              <img src="img/inplabel-logo.png" alt="Inplabel" style="max-width: 88px; max-height: 44px; object-fit: contain;">
            </td>
            <td style="width: 46%; vertical-align: middle; padding: 0 4px; font-size: 7.2px; line-height: 1.25;">
              <strong style="font-size: 9.5px; color: #000; display: block; margin-bottom: 1px;">INDUSTRIAS PLASTICOS BELSA S.A.C</strong>
              <div style="font-weight: bold; color: #222; font-size: 6.8px; margin-bottom: 1px;">DISEÑO, FABRICACIÓN Y COMERCIALIZACIÓN DE ENVASES PLÁSTICOS</div>
              <div>Contacto: 983 518 504 - 975 564 460</div>
              <div>www.inplabel.com.pe - ventas@inplabel.com.pe - inplabelsac@gmail.com</div>
              <div><strong>Principal:</strong> Av. Maria Parado de Bellido Lt. 5 Lotizacion Chacra Cerro - Comas - Lima - Lima</div>
              <div><strong>Sucursal:</strong> C.P. Las Piedritas Av. Las Piedritas Mz D Lt 9 - Carabayllo - Lima - Lima</div>
            </td>
            <td style="width: 32%; text-align: center; vertical-align: middle;">
              <div style="border: 1px solid #000; padding: 5px 2px; background: #fff;">
                <div style="font-size: 9px; font-weight: bold; color: #000;">RUC: 20544368827</div>
                <div style="font-size: 8.5px; font-weight: bold; color: #000; margin: 2px 0;">GUIA DE REMISION DE CONTROL INTERNO</div>
                <div style="font-size: 11px; font-weight: bold; color: #000; margin-top: 1px;">${nroGuia}</div>
              </div>
            </td>
          </tr>
        </table>

        <!-- 2. Datos del Destinatario, RUC, Doc Referencia y Fecha -->
        <table style="width: 100%; border-collapse: collapse; margin-bottom: 7px; font-size: 8px;">
          <tr>
            <td style="padding: 2px 0; width: 65%;">
              <strong>DESTINATARIO:</strong> <span style="font-weight: 500;">${clienteNombre}</span>
            </td>
            <td style="padding: 2px 0; width: 35%;">
              <strong>RUC:</strong> <span style="font-weight: 500;">${clienteRuc}</span>
            </td>
          </tr>
          <tr>
            <td style="padding: 2px 0; width: 65%;">
              <strong>DOC. REFERENCIA:</strong> <span style="font-weight: 500;">${docRef}</span>
            </td>
            <td style="padding: 2px 0; width: 35%;">
              <strong>FECHA:</strong> <span style="font-weight: 500;">${fechaStr}</span>
            </td>
          </tr>
        </table>

        <!-- 3. Cajas de Punto de Partida y Punto de Llegada (Sin Ubigeo, Espaciado Limpio) -->
        <table style="width: 100%; border-collapse: collapse; margin-bottom: 8px; font-size: 7.5px;">
          <tr>
            <td style="width: 50%; border: 1px solid #444; padding: 4px; vertical-align: top; line-height: 1.35;">
              <div style="text-align: center; font-weight: bold; font-size: 8px; margin-bottom: 3px;">Punto de partida</div>
              <div><strong>DIRECCIÓN:</strong> ${puntoPartida}</div>
            </td>
            <td style="width: 50%; border: 1px solid #444; padding: 4px; vertical-align: top; line-height: 1.35;">
              <div style="text-align: center; font-weight: bold; font-size: 8px; margin-bottom: 3px;">Punto de Llegada</div>
              <div><strong>DIRECCIÓN:</strong> ${puntoLlegada}</div>
            </td>
          </tr>
        </table>

        <!-- 4. Tabla de Productos con Cabecera Verde (Sin Peso, Filas más altas) -->
        <table style="width: 100%; border-collapse: collapse; border: 1px solid #444; margin-bottom: 8px; font-size: 7.5px;">
          <thead>
            <tr style="background-color: #d1e7dd; color: #000;">
              <th style="border: 1px solid #444; padding: 4px 3px; width: 8%; text-align: center;">ITEM</th>
              <th style="border: 1px solid #444; padding: 4px; width: 68%; text-align: center;">DESCRIPCION</th>
              <th style="border: 1px solid #444; padding: 4px 3px; width: 10%; text-align: center;">U.M.</th>
              <th style="border: 1px solid #444; padding: 4px 3px; width: 14%; text-align: center;">CANTIDAD</th>
            </tr>
          </thead>
          <tbody>
            ${detalles.length > 0 ? rowsHTML : emptyStateHTML}
          </tbody>
        </table>

        <!-- 5. Caja de Observaciones (Sin texto SUNAT) -->
        <div style="border: 1px solid #444; padding: 4px 5px; font-size: 7.5px; background: #fff; margin-bottom: 6px;">
          <div style="font-weight: bold; margin-bottom: 1px;">OBSERVACIONES:</div>
          <div>${observaciones && observaciones !== '-' ? observaciones : '-'}</div>
        </div>
      </div>

      <!-- 6. Dos Reglones para Firmas (posicionados a 70px del fondo, es decir 30px arriba de las leyendas) -->
      <table style="position: absolute; bottom: 70px; left: 0; right: 0; width: 100%; border-collapse: collapse; font-size: 7.5px;">
        <tr>
          <td style="width: 42%; text-align: center; vertical-align: top;">
            <div style="border-bottom: 1px solid #333; width: 85%; margin: 0 auto 3px auto; height: 18px;"></div>
            <div style="font-weight: bold; font-size: 7.5px;">EMISOR</div>
            <div style="color: #555; font-size: 6.8px;">FIRMA</div>
          </td>
          <td style="width: 16%;"></td>
          <td style="width: 42%; text-align: center; vertical-align: top;">
            <div style="border-bottom: 1px solid #333; width: 85%; margin: 0 auto 3px auto; height: 18px;"></div>
            <div style="font-weight: bold; font-size: 7.5px;">DESTINATARIO</div>
            <div style="color: #555; font-size: 6.8px;">FIRMA Y DNI</div>
          </td>
        </tr>
      </table>

      <!-- 7. Leyenda de Copia posicionada a 40px del final de la página (Remitente azul, Destinatario verde) -->
      <div style="position: absolute; bottom: 40px; left: 0; right: 0; text-align: center; font-size: 8.5px; font-weight: bold; color: #555; text-transform: uppercase;">
        [ COPIA: ${copiaNombre.includes('DESTINATARIO') || copiaNombre.includes('CLIENTE')
      ? `<span style="color: #198754; font-weight: 800;">DESTINATARIO</span>`
      : `<span style="color: #0d6efd; font-weight: 800;">REMITENTE</span>`} ]
      </div>
    </div>
  `;
}

export function generateGuiaInnerSheetHTML(guia) {
  return `
    <div style="display: flex; justify-content: space-between; width: 100%; box-sizing: border-box; background: #ffffff; padding: 10px 14px; color: #000; min-height: 520px; position: relative;">
      ${renderGuiaHalfHTML(guia, 'REMITENTE')}
      <div style="width: 1px; border-left: 1px dashed #888; min-height: 500px; margin: 0 10px;"></div>
      ${renderGuiaHalfHTML(guia, 'DESTINATARIO')}
    </div>
  `;
}

export function generateGuiaHTML(guia) {
  const nroGuia = guia.nro_guia || 'GR001-0001';
  return `<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <title>Guía de Remisión ${nroGuia}</title>
  <style>
    @page {
      size: A4 landscape;
      margin: 8mm;
    }
    * {
      box-sizing: border-box;
    }
    body {
      margin: 0;
      padding: 10px;
      background: #ffffff;
      font-family: Arial, Helvetica, sans-serif;
      color: #111;
      -webkit-print-color-adjust: exact;
      print-color-adjust: exact;
    }
  </style>
</head>
<body>
  ${generateGuiaInnerSheetHTML(guia)}
  <script>
    window.addEventListener('load', function() {
      setTimeout(function() {
        window.focus();
        window.print();
      }, 300);
    });
  </script>
</body>
</html>`;
}

export function openGuiaPDFInNewTab(guia) {
  if (guia && (guia.id_guia || guia.id)) {
    openPDF(guia.id_guia || guia.id);
    return;
  }
  const htmlContent = generateGuiaHTML(guia);
  const win = window.open('', '_blank');
  if (win) {
    win.document.open();
    win.document.write(htmlContent);
    win.document.close();
    win.focus();
  }
}

export function printGuiaDirectWithoutNewTab(guia) {
  const htmlContent = generateGuiaHTML(guia);
  const iframe = document.createElement('iframe');
  iframe.style.position = 'fixed';
  iframe.style.right = '0';
  iframe.style.bottom = '0';
  iframe.style.width = '0';
  iframe.style.height = '0';
  iframe.style.border = '0';
  document.body.appendChild(iframe);

  const doc = iframe.contentWindow.document;
  doc.open();
  doc.write(htmlContent);
  doc.close();

  const cleanup = () => {
    try {
      if (document.body.contains(iframe)) {
        document.body.removeChild(iframe);
      }
    } catch (e) {}
  };

  try {
    iframe.contentWindow.addEventListener('afterprint', cleanup);
    iframe.contentWindow.onafterprint = cleanup;
  } catch (e) {}

  setTimeout(() => {
    try {
      iframe.contentWindow.focus();
      iframe.contentWindow.print();
    } catch (e) {
      console.error("Error al disparar impresión nativa:", e);
    }
  }, 350);
}

export function printGuiaPDF(guia) {
  if (guia && (guia.id_guia || guia.id)) {
    openPDF(guia.id_guia || guia.id);
    return;
  }
  printGuiaDirectWithoutNewTab(guia);
}

export async function openEditarGuiaModal(idGuia) {
  let guia = currentShipments.find(s => String(s.id_guia) === String(idGuia));
  if (!guia) {
    try {
      guia = await api.getGuiaById(idGuia);
    } catch (e) {}
  }
  if (!guia) {
    alert("No se encontró la información de la guía seleccionada.");
    return;
  }

  document.getElementById('editGuiaId').value = guia.id_guia || idGuia;
  document.getElementById('editGuiaNroGuia').value = guia.nro_guia || '';
  document.getElementById('editGuiaFecha').value = (guia.fecha_guia || guia.fecha_emision || new Date().toISOString().split('T')[0]).split('T')[0];
  document.getElementById('editGuiaDocRef').value = guia.doc_referencia || '';
  document.getElementById('editGuiaPuntoPartida').value = guia.punto_partida || '';
  document.getElementById('editGuiaPuntoLlegada').value = guia.punto_llegada || '';
  document.getElementById('editGuiaObservaciones').value = guia.observaciones || '';

  // Pre-load products list into window.app.products for quick autocomplete ranking
  if (!window.app?.products || window.app.products.length === 0) {
    try {
      const productsList = await api.getProductos();
      if (productsList && Array.isArray(productsList)) {
        if (!window.app) window.app = {};
        window.app.products = productsList;
      }
    } catch (e) {
      console.warn("Error cargando productos para edición de guía:", e);
    }
  }

  const selectClient = document.getElementById('editGuiaClienteSelect');
  if (selectClient) {
    selectClient.innerHTML = '<option value="">Cargando clientes...</option>';
    try {
      const clients = await api.getClientes();
      if (!window.app) window.app = {};
      window.app.clients = clients;

      // Find the matching client using multiple strategies
      const guiaIdCliente = guia.id_cliente;
      const guiaNombreCliente = (guia.nombre_cliente || '').trim().toLowerCase();
      const guiaNroDoc = (guia.nro_documento || '').trim();

      selectClient.innerHTML = (clients || []).map(c => {
        const clientName = c.nombre_cliente || c.razon_social || c.nombre || '';
        const cId = c.id_cliente;
        const cDoc = (c.nro_documento || '').trim();
        const cName = clientName.trim().toLowerCase();

        // Match by id_cliente first, then by nro_documento, then by nombre
        const isSelected = (guiaIdCliente && String(cId) === String(guiaIdCliente))
          || (!guiaIdCliente && guiaNroDoc && cDoc === guiaNroDoc)
          || (!guiaIdCliente && !guiaNroDoc && guiaNombreCliente && cName === guiaNombreCliente);

        return `
          <option value="${cId}" ${isSelected ? 'selected' : ''}>
            ${escapeHtml(clientName)} (${cDoc})
          </option>
        `;
      }).join('');
    } catch (e) {
      console.warn("Error cargando clientes para edición de guía:", e);
    }
  }

  const tbody = document.getElementById('editGuiaItemsTableBody');
  if (tbody) {
    tbody.innerHTML = '';
    const detalles = guia.detalles || [];
    if (detalles.length === 0) {
      addEditGuiaRow();
    } else {
      detalles.forEach(item => {
        addEditGuiaRow(item);
      });
    }
  }

  showBootstrapModal('modalEditarGuia');
}

export function addEditGuiaRow(itemData = null) {
  const tbody = document.getElementById('editGuiaItemsTableBody');
  if (!tbody) return;

  const rowId = Date.now() + Math.floor(Math.random() * 1000);
  const prodName = itemData ? (itemData.nombre_producto || '') : '';
  const cant = itemData ? (itemData.cantidad || 1) : 1;
  const um = itemData ? (itemData.unidad_medida || 'UNID').toUpperCase() : 'UNID';

  const tr = document.createElement('tr');
  tr.id = `edit-row-${rowId}`;

  tr.innerHTML = `
    <td>
      <div class="position-relative">
        <input type="text" class="form-control form-control-sm edit-item-prod-input" placeholder="Escriba o busque el producto..." value="${escapeHtml(prodName)}" autocomplete="off" required>
        <ul class="list-group position-absolute w-100 shadow edit-item-prod-list d-none" style="z-index: 1090; max-height: 200px; overflow-y: auto; top: 100%; left: 0; box-shadow: 0 8px 24px rgba(0,0,0,0.6);"></ul>
      </div>
    </td>
    <td class="text-center">
      <span class="badge bg-secondary-subtle text-secondary-emphasis border px-2.5 py-1 fw-bold fs-7 edit-item-um-badge">${escapeHtml(um)}</span>
      <input type="hidden" class="edit-item-um-val" value="${escapeHtml(um)}">
    </td>
    <td class="text-center">
      <input type="text" inputmode="numeric" class="form-control form-control-sm text-center edit-item-cant-input" value="${cant}" oninput="this.value = this.value.replace(/[^0-9]/g, '')" required>
    </td>
    <td class="text-center">
      <button type="button" class="btn btn-sm btn-outline-danger py-0.5 px-2" onclick="enviosModule.removeEditGuiaRow(this)">
        <i class="bi bi-trash-fill"></i>
      </button>
    </td>
  `;

  tbody.appendChild(tr);
  setupEditGuiaItemSearch(tr);
}

export function setupEditGuiaItemSearch(tr) {
  const input = tr.querySelector('.edit-item-prod-input');
  const list = tr.querySelector('.edit-item-prod-list');
  if (!input || !list) return;

  let activeIndex = -1;

  const renderDropdown = (query) => {
    const val = (query || '').trim();
    activeIndex = -1;

    if (!val) {
      list.classList.add('d-none');
      list.innerHTML = '';
      return;
    }

    const products = (window.app?.products && window.app.products.length > 0)
      ? window.app.products
      : [];

    const matches = filterAndRankItems(
      products,
      val,
      p => `#${p.codigo_producto || p.id_producto || ''} ${p.id_producto || ''} ${p.codigo_producto || ''} ${p.nombre_producto || ''} ${p.tipo_producto || ''} ${p.categoria || ''} ${p.unidad_medida || ''}`
    ).slice(0, 25);

    if (matches.length === 0) {
      list.innerHTML = `<li class="list-group-item text-muted py-2 fs-7">No se encontraron productos que coincidan con "${escapeHtml(val)}"</li>`;
      list.classList.remove('d-none');
      return;
    }

    list.innerHTML = matches.map((p, idx) => `
      <li class="list-group-item list-group-item-action py-1.5 px-3 prod-opt-item d-flex align-items-center gap-2 fs-7" style="cursor: pointer;" data-index="${idx}">
        <span class="fw-bold text-primary">#${escapeHtml(p.codigo_producto || p.id_producto)}</span>
        <span class="fw-semibold text-body">- ${escapeHtml(p.nombre_producto)}</span>
      </li>
    `).join('');
    list.classList.remove('d-none');

    list.querySelectorAll('.prod-opt-item').forEach((item, idx) => {
      item.addEventListener('click', () => {
        input.value = matches[idx].nombre_producto;
        const umBadge = tr.querySelector('.edit-item-um-badge');
        const umVal = tr.querySelector('.edit-item-um-val');
        const newUM = (matches[idx].unidad_medida || 'UNID').toUpperCase();
        if (umBadge) umBadge.textContent = newUM;
        if (umVal) umVal.value = newUM;
        list.classList.add('d-none');
      });
    });
  };

  input.addEventListener('input', (e) => {
    renderDropdown(e.target.value);
  });

  input.addEventListener('focus', (e) => {
    if (e.target.value.trim()) {
      renderDropdown(e.target.value);
    }
  });

  input.addEventListener('keydown', (e) => {
    const items = list.querySelectorAll('.prod-opt-item');
    if (items.length === 0) return;

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      activeIndex = Math.min(activeIndex + 1, items.length - 1);
      updateActiveRowItem(items, activeIndex);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      activeIndex = Math.max(activeIndex - 1, 0);
      updateActiveRowItem(items, activeIndex);
    } else if (e.key === 'Enter') {
      if (!list.classList.contains('d-none')) {
        e.preventDefault();
        if (activeIndex >= 0 && items[activeIndex]) {
          items[activeIndex].click();
        } else if (items.length > 0) {
          items[0].click();
        }
      }
    } else if (e.key === 'Escape') {
      list.classList.add('d-none');
    }
  });

  document.addEventListener('click', (e) => {
    if (!tr.contains(e.target)) {
      list.classList.add('d-none');
    }
  });
}

function updateActiveRowItem(items, activeIndex) {
  items.forEach((item, idx) => {
    if (idx === activeIndex) {
      item.classList.add('active', 'bg-primary', 'text-white');
      item.scrollIntoView({ block: 'nearest' });
    } else {
      item.classList.remove('active', 'bg-primary', 'text-white');
    }
  });
}

export function removeEditGuiaRow(btn) {
  const tr = btn.closest('tr');
  if (tr) {
    const tbody = tr.parentElement;
    tr.remove();
    if (tbody && tbody.children.length === 0) {
      addEditGuiaRow();
    }
  }
}

export async function saveEditarGuia() {
  const idGuia = document.getElementById('editGuiaId')?.value;
  if (!idGuia) return;

  const nroGuia = document.getElementById('editGuiaNroGuia')?.value.trim();
  const fechaGuia = document.getElementById('editGuiaFecha')?.value;
  const idCliente = document.getElementById('editGuiaClienteSelect')?.value;
  const docRef = document.getElementById('editGuiaDocRef')?.value.trim();
  const puntoPartida = document.getElementById('editGuiaPuntoPartida')?.value.trim();
  const puntoLlegada = document.getElementById('editGuiaPuntoLlegada')?.value.trim();
  const observaciones = document.getElementById('editGuiaObservaciones')?.value.trim();

  if (!nroGuia || !fechaGuia || !idCliente) {
    alert("Por favor complete los campos obligatorios: N° Guía, Fecha y Cliente.");
    return;
  }

  const itemRows = document.querySelectorAll('#editGuiaItemsTableBody tr');
  const detalles = [];
  itemRows.forEach(tr => {
    const inputElem = tr.querySelector('.edit-item-prod-input');
    const pName = inputElem ? inputElem.value.trim() : '';
    const cant = parseInt(tr.querySelector('.edit-item-cant-input')?.value || 0);
    const umVal = tr.querySelector('.edit-item-um-val');
    const um = umVal ? (umVal.value || 'UNID').toUpperCase() : 'UNID';

    if (pName && cant > 0) {
      let pId = 0;
      if (window.app && window.app.products) {
        const match = window.app.products.find(p => (p.nombre_producto || '').trim().toLowerCase() === pName.toLowerCase());
        if (match) pId = match.id_producto;
      }
      detalles.push({
        id_producto: pId,
        cantidad: cant,
        unidad_medida: um,
        nombre_producto: pName
      });
    }
  });

  if (detalles.length === 0) {
    alert("Debe incluir al menos un producto válido con cantidad mayor a 0.");
    return;
  }

  const payload = {
    id_guia: parseInt(idGuia),
    nro_guia: nroGuia,
    fecha_guia: fechaGuia,
    id_cliente: parseInt(idCliente),
    doc_referencia: docRef,
    punto_partida: puntoPartida,
    punto_llegada: puntoLlegada,
    observaciones: observaciones,
    establecimiento: nroGuia.toUpperCase().startsWith('GR002') ? 'COMAS' : 'CARABAYLLO',
    detalles: detalles
  };

  const saveBtn = document.querySelector('#formEditarGuia button[type="submit"]');
  const originalText = saveBtn ? saveBtn.innerHTML : '';
  if (saveBtn) {
    saveBtn.disabled = true;
    saveBtn.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span> Guardando...';
  }

  try {
    const res = await api.updateGuia(idGuia, payload);
    if (res) {
      hideBootstrapModal('modalEditarGuia');

      // Refresh shipments list
      try {
        const freshGuias = await api.getGuias();
        if (Array.isArray(freshGuias) && freshGuias.length > 0) {
          currentShipments = freshGuias;
        } else {
          const idx = currentShipments.findIndex(s => String(s.id_guia) === String(idGuia));
          if (idx !== -1) {
            currentShipments[idx] = { ...currentShipments[idx], ...payload, ...(res.id_guia ? res : {}) };
          }
        }
      } catch (e) {
        const idx = currentShipments.findIndex(s => String(s.id_guia) === String(idGuia));
        if (idx !== -1) {
          currentShipments[idx] = { ...currentShipments[idx], ...payload, ...(res.id_guia ? res : {}) };
        }
      }

      renderEnviosTable(currentShipments, currentSearchQuery);
      alert("¡Guía de Remisión actualizada exitosamente!");
    } else {
      alert("No se pudo actualizar la guía de remisión en el servidor. Verifique la conexión.");
    }
  } catch (err) {
    console.error("Error al guardar edición de guía:", err);
    alert("Error al actualizar la guía de remisión: " + (err.message || "Error del servidor"));
  } finally {
    if (saveBtn) {
      saveBtn.disabled = false;
      saveBtn.innerHTML = originalText;
    }
  }
}

