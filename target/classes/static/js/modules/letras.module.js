import { escapeHtml, filterAndRankItems, formatDate, paginateItems, renderPaginationUI } from '../helpers.js';
import { api } from '../api.js';

let rawLetrasList = [];
let groupedLetras = [];
let allClients = [];
let selectedClient = null;
let generatedInstallments = [];
let activeClientIndex = -1;

let currentPage = 1;
const pageSize = 20;

export function changePage(delta) {
  currentPage += delta;
  renderLetrasTable(groupedLetras);
}

export function resetPagination() {
  currentPage = 1;
}

// =========================================================================
// 1. NÚMERO A LETRAS (CONVERSOR EN ESPAÑOL PARA SOLES PERUANOS)
// =========================================================================
export function numeroALetras(monto) {
  const num = Math.abs(parseFloat(monto) || 0);
  const entero = Math.floor(num);
  const centavos = Math.round((num - entero) * 100);
  const centavosStr = String(centavos).padStart(2, '0');

  if (entero === 0) {
    return `CERO CON ${centavosStr} / 100 SOLES`;
  }

  function unidades(n) {
    switch (n) {
      case 1: return 'UN';
      case 2: return 'DOS';
      case 3: return 'TRES';
      case 4: return 'CUATRO';
      case 5: return 'CINCO';
      case 6: return 'SEIS';
      case 7: return 'SIETE';
      case 8: return 'OCHO';
      case 9: return 'NUEVE';
      default: return '';
    }
  }

  function decenasY(str, n) {
    if (n > 0) return str + ' Y ' + unidades(n);
    return str;
  }

  function decenas(n) {
    const dec = Math.floor(n / 10);
    const uni = n - dec * 10;
    switch (dec) {
      case 1:
        switch (uni) {
          case 0: return 'DIEZ';
          case 1: return 'ONCE';
          case 2: return 'DOCE';
          case 3: return 'TRECE';
          case 4: return 'CATORCE';
          case 5: return 'QUINCE';
          default: return 'DIECI' + unidades(uni);
        }
      case 2:
        if (uni === 0) return 'VEINTE';
        return 'VEINTI' + unidades(uni);
      case 3: return decenasY('TREINTA', uni);
      case 4: return decenasY('CUARENTA', uni);
      case 5: return decenasY('CINCUENTA', uni);
      case 6: return decenasY('SESENTA', uni);
      case 7: return decenasY('SETENTA', uni);
      case 8: return decenasY('OCHENTA', uni);
      case 9: return decenasY('NOVENTA', uni);
      case 0: return unidades(uni);
      default: return '';
    }
  }

  function centenas(n) {
    const cen = Math.floor(n / 100);
    const dec = n - cen * 100;
    switch (cen) {
      case 1:
        if (dec > 0) return 'CIENTO ' + decenas(dec);
        return 'CIEN';
      case 2: return 'DOSCIENTOS ' + decenas(dec);
      case 3: return 'TRESCIENTOS ' + decenas(dec);
      case 4: return 'CUATROCIENTOS ' + decenas(dec);
      case 5: return 'QUINIENTOS ' + decenas(dec);
      case 6: return 'SEISCIENTOS ' + decenas(dec);
      case 7: return 'SETECIENTOS ' + decenas(dec);
      case 8: return 'OCHOCIENTOS ' + decenas(dec);
      case 9: return 'NOVECIENTOS ' + decenas(dec);
      default: return decenas(dec);
    }
  }

  function seccion(n, divisor, strSingular, strPlural) {
    const cientos = Math.floor(n / divisor);
    const resto = n - cientos * divisor;
    let letras = '';

    if (cientos > 0) {
      if (cientos > 1) {
        letras = centenas(cientos) + ' ' + strPlural;
      } else {
        letras = strSingular;
      }
    }
    if (resto > 0) {
      letras += '';
    }
    return { letras, resto };
  }

  let totalLetras = '';
  let resto = entero;

  // Millones
  if (resto >= 1000000) {
    const resMillones = seccion(resto, 1000000, 'UN MILLON', 'MILLONES');
    totalLetras += resMillones.letras + ' ';
    resto = resMillones.resto;
  }

  // Miles
  if (resto >= 1000) {
    const resMiles = seccion(resto, 1000, 'UN MIL', 'MIL');
    totalLetras += resMiles.letras + ' ';
    resto = resMiles.resto;
  }

  // Cientos
  if (resto > 0) {
    totalLetras += centenas(resto);
  }

  totalLetras = totalLetras.trim().replace(/\s+/g, ' ');
  return `${totalLetras} CON ${centavosStr} / 100 SOLES`.toUpperCase();
}

// =========================================================================
// 2. INICIALIZACIÓN, AGRUPACIÓN Y RENDERIZADO DE LA VISTA
// =========================================================================
export async function initLetrasView() {
  try {
    // Establecer como fecha Desde el inicio del mes actual y Hasta el día de hoy
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const firstDay = `${year}-${month}-01`;
    const today = `${year}-${month}-${day}`;

    const dateFromEl = document.getElementById('filterLetraDateFrom');
    if (dateFromEl && !dateFromEl.value) {
      dateFromEl.value = firstDay;
    }

    const dateToEl = document.getElementById('filterLetraDateTo');
    if (dateToEl && !dateToEl.value) {
      dateToEl.value = today;
    }

    const [letras, clients] = await Promise.all([
      api.getLetras({ dateFrom: firstDay, dateTo: today }),
      api.getClientes()
    ]);
    rawLetrasList = letras || [];
    allClients = clients || [];

    groupedLetras = groupLetrasByBatch(rawLetrasList);
    renderLetrasTable(groupedLetras);
    updateMetrics(rawLetrasList, groupedLetras);
  } catch (err) {
    console.error('Error al inicializar vista de Letras:', err);
  }
}

export function groupLetrasByBatch(list = []) {
  const groupsMap = new Map();

  list.forEach(letra => {
    // Agrupar por id_lote o combinación Ref. Girador + Cliente + Fecha Giro
    const key = letra.id_lote ? letra.id_lote : `${letra.ref_girador || 'S-R'}_${letra.nombre_cliente || ''}_${letra.fecha_giro || ''}`;

    if (!groupsMap.has(key)) {
      groupsMap.set(key, {
        id_lote: letra.id_lote || key,
        ref_girador: letra.ref_girador || 'S/R',
        nombre_cliente: letra.nombre_cliente || 'SIN CLIENTE',
        nro_documento: letra.nro_documento || '',
        direccion_cliente: letra.direccion_cliente || '',
        lugar_giro: letra.lugar_giro || 'LIMA',
        fecha_giro: letra.fecha_giro || '',
        letras: []
      });
    }

    groupsMap.get(key).letras.push(letra);
  });

  const grouped = Array.from(groupsMap.values()).map(group => {
    // Ordenar letras del lote por correlativo o fecha
    group.letras.sort((a, b) => (a.numero_correlativo || 0) - (b.numero_correlativo || 0) || (a.id_letra || 0) - (b.id_letra || 0));

    const totalCuotas = group.letras.length;
    const montoTotal = group.letras.reduce((sum, l) => sum + (parseFloat(l.monto) || 0), 0);

    // Estado general
    const allAnuladas = group.letras.every(l => l.estado === 'ANULADA');
    const allCanceladas = group.letras.every(l => l.estado === 'CANCELADA' || l.estado === 'COBRADA');
    let estadoGeneral = 'PENDIENTE';
    if (allAnuladas) estadoGeneral = 'ANULADA';
    else if (allCanceladas) estadoGeneral = 'CANCELADA';

    const firstLetra = group.letras[0]?.nro_letra || '';
    const lastLetra = group.letras[group.letras.length - 1]?.nro_letra || '';
    const rangoLetras = totalCuotas > 1 ? `${firstLetra} al ${lastLetra}` : firstLetra;

    return {
      ...group,
      total_cuotas: totalCuotas,
      monto_total: montoTotal,
      estado: estadoGeneral,
      rango_letras: rangoLetras
    };
  });

  return grouped;
}

export function renderLetrasTable(groupedList = []) {
  const tbody = document.getElementById('letrasTableBody');
  const countBadge = document.getElementById('letrasCountBadge');
  if (!tbody) return;

  tbody.innerHTML = '';
  if (countBadge) countBadge.textContent = `${groupedList.length} ${groupedList.length === 1 ? 'operación' : 'operaciones'}`;

  const p = paginateItems(groupedList, currentPage, pageSize);
  currentPage = p.currentPage;

  renderPaginationUI({
    containerId: 'letrasPaginationContainer',
    currentPage: p.currentPage,
    totalPages: p.totalPages,
    totalItems: p.totalItems,
    startIndex: p.startIndex,
    endIndex: p.endIndex,
    onPageChangeName: 'letrasModule.changePage'
  });

  if (p.items.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="8" class="text-center py-5 text-muted">
          <i class="bi bi-file-earmark-x fs-1 d-block mb-2 text-secondary opacity-50"></i>
          No se encontraron Operaciones de Letras de Cambio registradas.
        </td>
      </tr>
    `;
    return;
  }

  p.items.forEach(lote => {
    const tr = document.createElement('tr');
    const isAnulada = lote.estado === 'ANULADA';

    const montoFormatted = (parseFloat(lote.monto_total) || 0).toLocaleString('es-PE', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    });

    tr.innerHTML = `
      <td class="font-monospace fw-bold text-primary fs-7">${escapeHtml(lote.ref_girador || '-')}</td>
      <td>
        <div class="fw-bold text-white">${escapeHtml(lote.nombre_cliente || '-')}</div>
        <small class="text-muted font-monospace">${escapeHtml(lote.nro_documento || '')}</small>
      </td>
      <td class="small">${escapeHtml(lote.fecha_giro || '-')}</td>
      <td class="text-center">
        <span class="badge bg-primary fs-8 px-2 py-1">${lote.total_cuotas} ${lote.total_cuotas === 1 ? 'Letra' : 'Letras'}</span>
      </td>
      <td class="font-monospace fw-semibold text-info small">${escapeHtml(lote.rango_letras || '-')}</td>
      <td class="text-end fw-bold font-monospace text-white">S/ ${montoFormatted}</td>
      
      <!-- 1. Columna DETALLES -->
      <td class="text-center">
        <button class="btn-action-solid btn-view" title="Ver Detalle de Letras" onclick="letrasModule.openLoteDetailModal('${escapeHtml(lote.id_lote)}')">
          <i class="bi bi-eye-fill"></i>
        </button>
      </td>

      <!-- 2. Columna ANULAR -->
      <td class="text-center">
        ${isAnulada ? `
          <button class="btn-action-solid btn-cancel" style="opacity: 0.35; cursor: not-allowed;" title="Ya anulado" disabled>
            <i class="bi bi-slash-circle"></i>
          </button>
        ` : `
          <button class="btn-action-solid btn-cancel" title="Anular Operación" onclick="letrasModule.openAnularLoteModal('${escapeHtml(lote.id_lote)}', '${escapeHtml(lote.ref_girador)}', '${escapeHtml(lote.nombre_cliente)}')">
            <i class="bi bi-x-lg"></i>
          </button>
        `}
      </td>
    `;
    tbody.appendChild(tr);
  });
}

export function updateMetrics(rawList = [], groupedList = []) {
  const totalAmountEl = document.getElementById('statLetrasTotalAmount');
  const totalLetrasEl = document.getElementById('statLetrasTotalLetrasCount');
  const topClientNameEl = document.getElementById('statLetrasTopClientName');
  const topClientSubEl = document.getElementById('statLetrasTopClientSub');
  const clientsEl = document.getElementById('statLetrasClientsCount');

  const totalLetras = rawList.length;
  const sumAmount = rawList.reduce((acc, curr) => curr.estado !== 'ANULADA' ? acc + (parseFloat(curr.monto) || 0) : acc, 0);

  // Clientes atendidos únicos
  const clientSet = new Set(rawList.map(l => l.nro_documento || l.nombre_cliente).filter(Boolean));

  // Cliente con más letras / mayor monto
  const clientStats = {};
  rawList.forEach(l => {
    if (l.estado === 'ANULADA') return;
    const name = l.nombre_cliente || 'SIN CLIENTE';
    if (!clientStats[name]) {
      clientStats[name] = { name, count: 0, amount: 0 };
    }
    clientStats[name].count += 1;
    clientStats[name].amount += (parseFloat(l.monto) || 0);
  });

  const topClients = Object.values(clientStats).sort((a, b) => b.count - a.count || b.amount - a.amount);
  const topClient = topClients[0];

  if (totalAmountEl) totalAmountEl.textContent = `S/ ${sumAmount.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
  if (totalLetrasEl) totalLetrasEl.textContent = totalLetras;
  if (clientsEl) clientsEl.textContent = clientSet.size;

  if (topClientNameEl) {
    if (topClient) {
      topClientNameEl.textContent = topClient.name;
      topClientNameEl.title = topClient.name;
      if (topClientSubEl) {
        topClientSubEl.textContent = `${topClient.count} ${topClient.count === 1 ? 'letra' : 'letras'} (S/ ${topClient.amount.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })})`;
      }
    } else {
      topClientNameEl.textContent = '-';
      topClientNameEl.title = '-';
      if (topClientSubEl) topClientSubEl.textContent = '0 letras';
    }
  }

  const pendientes = rawList.filter(l => l.estado === 'PENDIENTE').length;
  const sidebarBadge = document.getElementById('badgeLetras');
  if (sidebarBadge) {
    if (pendientes > 0) {
      sidebarBadge.textContent = pendientes;
      sidebarBadge.classList.remove('d-none');
    } else {
      sidebarBadge.classList.add('d-none');
    }
  }
}

export async function triggerSearch() {
  const search = document.getElementById('searchLetraInput')?.value || '';
  const dateFrom = document.getElementById('filterLetraDateFrom')?.value || '';
  const dateTo = document.getElementById('filterLetraDateTo')?.value || '';

  const results = await api.getLetras({ search, dateFrom, dateTo });
  rawLetrasList = results || [];
  groupedLetras = groupLetrasByBatch(rawLetrasList);
  renderLetrasTable(groupedLetras);
  updateMetrics(rawLetrasList, groupedLetras);
}

// =========================================================================
// 3. GENERACIÓN DE MULTI-LETRAS (BUSCADOR RUC, MODAL & CÁLCULOS)
// =========================================================================
export async function openGenerarLetrasModal() {
  const modalEl = document.getElementById('modalGenerarLetras');
  if (!modalEl) return;

  // Cargar clientes reales directamente de MySQL
  try {
    const clients = await api.getClientes();
    // Filter only clients with RUC (11 digits or tipo_documento RUC)
    allClients = (clients || []).filter(c => {
      const doc = String(c.nro_documento || '').trim();
      return doc.length === 11 || c.tipo_documento === 'RUC';
    });
  } catch (e) {
    allClients = [];
  }

  selectedClient = null;
  activeClientIndex = -1;

  // Reset inputs
  const searchInput = document.getElementById('searchClienteLetraInput');
  if (searchInput) searchInput.value = '';

  document.getElementById('letraClienteDoc').value = '';
  document.getElementById('letraClienteNombre').value = '';
  document.getElementById('letraClienteDireccion').value = '';
  document.getElementById('letraRefGirador').value = '';
  document.getElementById('letraLugarGiro').value = 'LIMA';

  const todayStr = new Date().toISOString().split('T')[0];
  document.getElementById('letraFechaGiro').value = todayStr;
  document.getElementById('letraMontoTotal').value = '';
  document.getElementById('letraCantidadCuotas').value = '1';

  // Obtener sugerencia de correlativo inicial
  const corrData = await api.getNextLetraCorrelativo();
  document.getElementById('letraCorrelativoInput').value = corrData.nextCorrelativo || 1;
  document.getElementById('letraAnioInput').value = corrData.anio || new Date().getFullYear();

  setupClientSearchAutocomplete();
  recalcInstallments();

  const modal = new bootstrap.Modal(modalEl, {
    backdrop: 'static',
    keyboard: false
  });
  modal.show();
}

function setupClientSearchAutocomplete() {
  const input = document.getElementById('searchClienteLetraInput');
  const list = document.getElementById('clientSearchResultsLetraList');
  if (!input || !list) return;

  if (input.dataset.boundSearch === 'true') return;
  input.dataset.boundSearch = 'true';

  input.addEventListener('input', (e) => {
    const val = e.target.value.trim();
    activeClientIndex = -1;

    if (!val) {
      list.classList.add('d-none');
      list.innerHTML = '';
      return;
    }

    const matches = filterAndRankItems(
      allClients,
      val,
      c => `${c.nro_documento || ''} ${c.nombre_cliente || c.razon_social || ''} ${c.direccion || ''}`
    ).slice(0, 15);

    if (matches.length === 0) {
      list.innerHTML = `<li class="list-group-item text-muted py-2 fs-7">No se encontraron clientes para "${escapeHtml(val)}"</li>`;
      list.classList.remove('d-none');
      return;
    }

    list.innerHTML = matches.map((c, idx) => `
      <li class="list-group-item list-group-item-action py-2 px-3 client-opt-letra d-flex align-items-center gap-2 fs-7 text-white" data-index="${idx}">
        <span class="fw-bold text-white">${escapeHtml(c.nro_documento || 'S/D')}</span>
        <span class="fw-semibold text-white">- ${escapeHtml(c.nombre_cliente || c.razon_social)}</span>
      </li>
    `).join('');
    list.classList.remove('d-none');

    list.querySelectorAll('.client-opt-letra').forEach((item, idx) => {
      item.addEventListener('click', () => {
        selectClient(matches[idx]);
      });
    });
  });

  input.addEventListener('keydown', (e) => {
    const items = list.querySelectorAll('.client-opt-letra');
    if (items.length === 0) return;

    if (e.key === 'ArrowDown') {
      e.preventDefault();
      activeClientIndex = Math.min(activeClientIndex + 1, items.length - 1);
      updateActiveItem(items, activeClientIndex);
    } else if (e.key === 'ArrowUp') {
      e.preventDefault();
      activeClientIndex = Math.max(activeClientIndex - 1, 0);
      updateActiveItem(items, activeClientIndex);
    } else if (e.key === 'Enter') {
      e.preventDefault();
      if (activeClientIndex >= 0 && items[activeClientIndex]) {
        items[activeClientIndex].click();
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

function selectClient(c) {
  if (!c) return;
  selectedClient = c;
  const list = document.getElementById('clientSearchResultsLetraList');
  const searchInput = document.getElementById('searchClienteLetraInput');

  const nombre = c.nombre_cliente || c.razon_social || '';
  const doc = c.nro_documento || '';
  const dir = c.direccion || 'LIMA - LIMA';

  if (list) list.classList.add('d-none');
  if (searchInput) searchInput.value = `${doc} - ${nombre}`;

  document.getElementById('letraClienteDoc').value = doc;
  document.getElementById('letraClienteNombre').value = nombre;
  document.getElementById('letraClienteDireccion').value = dir;
}

export function clearSelectedClient() {
  selectedClient = null;
  const searchInput = document.getElementById('searchClienteLetraInput');
  const list = document.getElementById('clientSearchResultsLetraList');

  if (searchInput) searchInput.value = '';
  if (list) list.classList.add('d-none');

  document.getElementById('letraClienteDoc').value = '';
  document.getElementById('letraClienteNombre').value = '';
  document.getElementById('letraClienteDireccion').value = '';
}

export function hasUnsavedFormData() {
  const cliente = document.getElementById('letraClienteNombre')?.value?.trim();
  const ref = document.getElementById('letraRefGirador')?.value?.trim();
  const monto = parseFloat(document.getElementById('letraMontoTotal')?.value) || 0;
  return Boolean(cliente || ref || monto > 0);
}

export function attemptCloseGenerarModal() {
  if (hasUnsavedFormData()) {
    const discardModalEl = document.getElementById('modalConfirmDiscardLetra');
    if (discardModalEl) {
      const modal = new bootstrap.Modal(discardModalEl);
      modal.show();
      return;
    }
  }
  forceCloseGenerarModal();
}

export function forceCloseGenerarModal() {
  const discardModalEl = document.getElementById('modalConfirmDiscardLetra');
  if (discardModalEl) {
    const dModal = bootstrap.Modal.getInstance(discardModalEl);
    if (dModal) dModal.hide();
  }

  const modalEl = document.getElementById('modalGenerarLetras');
  if (modalEl) {
    const modal = bootstrap.Modal.getInstance(modalEl);
    if (modal) modal.hide();
  }
  clearSelectedClient();
}

export function recalcInstallments() {
  const montoTotal = parseFloat(document.getElementById('letraMontoTotal')?.value) || 0;
  const count = parseInt(document.getElementById('letraCantidadCuotas')?.value) || 1;
  const correlativoBase = parseInt(document.getElementById('letraCorrelativoInput')?.value) || 1;
  const anio = parseInt(document.getElementById('letraAnioInput')?.value) || new Date().getFullYear();
  const fechaGiroStr = document.getElementById('letraFechaGiro')?.value || new Date().toISOString().split('T')[0];

  const tbody = document.getElementById('installmentRowsTbody');
  if (!tbody) return;

  if (count <= 0) {
    tbody.innerHTML = '<tr><td colspan="6" class="text-center text-muted">Ingrese una cantidad de letras válida.</td></tr>';
    return;
  }

  const previousDays = generatedInstallments.map(g => g.dias_credito);
  generatedInstallments = [];

  // REGLA DE DIVISIÓN: Asignar centavos restantes al final
  const baseMonto = Math.floor((montoTotal / count) * 100) / 100;
  let runningSum = 0;
  const fechaGiroDate = new Date(fechaGiroStr + 'T00:00:00');

  for (let i = 0; i < count; i++) {
    const isLast = (i === count - 1);
    let cuotaMonto = baseMonto;
    if (isLast) {
      cuotaMonto = Math.round((montoTotal - runningSum) * 100) / 100;
    }
    runningSum += cuotaMonto;

    const cuotaCorr = correlativoBase + i;
    const nroLetra = `${String(cuotaCorr).padStart(3, '0')}-${anio}`;
    const diasCredito = previousDays[i] !== undefined ? previousDays[i] : (i + 1) * 30;

    const vencDate = new Date(fechaGiroDate);
    vencDate.setDate(vencDate.getDate() + diasCredito);
    const vencStr = vencDate.toISOString().split('T')[0];

    const montoEnLetras = numeroALetras(cuotaMonto);

    generatedInstallments.push({
      indice: i + 1,
      numero_correlativo: cuotaCorr,
      anio: anio,
      nro_letra: nroLetra,
      dias_credito: diasCredito,
      fecha_vencimiento: vencStr,
      monto: cuotaMonto,
      monto_letras: montoEnLetras
    });
  }

  renderInstallmentRows();
}

function renderInstallmentRows() {
  const tbody = document.getElementById('installmentRowsTbody');
  const validationBadge = document.getElementById('letrasSumValidationBadge');
  if (!tbody) return;

  tbody.innerHTML = '';
  let sum = 0;

  generatedInstallments.forEach((inst, idx) => {
    sum += (parseFloat(inst.monto) || 0);
    const tr = document.createElement('tr');
    tr.innerHTML = `
      <td class="text-center fw-bold text-muted">${inst.indice}</td>
      <td>
        <input type="text" class="form-control form-control-sm font-monospace fw-bold text-primary" value="${escapeHtml(inst.nro_letra)}" onchange="letrasModule.onInstallmentFieldChange(${idx}, 'nro_letra', this.value)">
      </td>
      <td class="text-center">
        <input type="number" class="form-control form-control-sm text-center fw-bold text-warning font-monospace" value="${inst.dias_credito}" min="1" oninput="letrasModule.onInstallmentDaysChange(${idx}, this.value)">
      </td>
      <td>
        <input type="date" class="form-control form-control-sm" value="${inst.fecha_vencimiento}" onchange="letrasModule.onInstallmentDueDateChange(${idx}, this.value)">
      </td>
      <td>
        <div class="input-group input-group-sm">
          <span class="input-group-text px-1 fw-bold">S/</span>
          <input type="number" step="0.01" class="form-control form-control-sm font-monospace fw-bold text-end px-1" style="font-size: 0.92rem; min-width: 95px;" value="${inst.monto.toFixed(2)}" onchange="letrasModule.onInstallmentAmountChange(${idx}, this.value)">
        </div>
      </td>
      <td>
        <div class="small fw-semibold font-monospace" style="color: #cbd5e1 !important;" id="inst_letras_${idx}">${escapeHtml(inst.monto_letras)}</div>
      </td>
    `;
    tbody.appendChild(tr);
  });

  const totalEsperado = parseFloat(document.getElementById('letraMontoTotal')?.value) || 0;
  const diff = Math.abs(sum - totalEsperado);

  if (validationBadge) {
    if (diff < 0.01 && totalEsperado > 0) {
      validationBadge.className = 'badge bg-success fs-7';
      validationBadge.innerHTML = `<i class="bi bi-check-circle me-1"></i> Suma Cuotas: S/ ${sum.toFixed(2)} (Exacto)`;
    } else {
      validationBadge.className = 'badge bg-warning text-dark fs-7';
      validationBadge.innerHTML = `<i class="bi bi-exclamation-triangle me-1"></i> Suma: S/ ${sum.toFixed(2)} | Total Op: S/ ${totalEsperado.toFixed(2)}`;
    }
  }
}

export function onInstallmentFieldChange(idx, field, value) {
  if (generatedInstallments[idx]) {
    generatedInstallments[idx][field] = value;
  }
}

export function onInstallmentDaysChange(idx, daysVal) {
  const days = parseInt(daysVal) || 0;
  const fechaGiroStr = document.getElementById('letraFechaGiro')?.value || new Date().toISOString().split('T')[0];
  const date = new Date(fechaGiroStr + 'T00:00:00');
  date.setDate(date.getDate() + days);

  if (generatedInstallments[idx]) {
    generatedInstallments[idx].dias_credito = days;
    generatedInstallments[idx].fecha_vencimiento = date.toISOString().split('T')[0];

    const tr = document.getElementById('installmentRowsTbody')?.children[idx];
    if (tr) {
      const dateInput = tr.querySelector('input[type="date"]');
      if (dateInput) dateInput.value = generatedInstallments[idx].fecha_vencimiento;
    }
  }
}

export function onInstallmentDueDateChange(idx, dateVal) {
  const fechaGiroStr = document.getElementById('letraFechaGiro')?.value || new Date().toISOString().split('T')[0];
  const gDate = new Date(fechaGiroStr + 'T00:00:00');
  const vDate = new Date(dateVal + 'T00:00:00');

  const diffTime = vDate - gDate;
  const diffDays = Math.max(0, Math.round(diffTime / (1000 * 60 * 60 * 24)));

  if (generatedInstallments[idx]) {
    generatedInstallments[idx].fecha_vencimiento = dateVal;
    generatedInstallments[idx].dias_credito = diffDays;

    const tr = document.getElementById('installmentRowsTbody')?.children[idx];
    if (tr) {
      const daysInput = tr.querySelector('input[type="number"]');
      if (daysInput) daysInput.value = diffDays;
    }
  }
}

export function onInstallmentAmountChange(idx, amountVal) {
  const m = parseFloat(amountVal) || 0;
  if (generatedInstallments[idx]) {
    generatedInstallments[idx].monto = m;
    generatedInstallments[idx].monto_letras = numeroALetras(m);
    const label = document.getElementById(`inst_letras_${idx}`);
    if (label) label.textContent = generatedInstallments[idx].monto_letras;

    let sum = generatedInstallments.reduce((acc, curr) => acc + (parseFloat(curr.monto) || 0), 0);
    const totalEsperado = parseFloat(document.getElementById('letraMontoTotal')?.value) || 0;
    const diff = Math.abs(sum - totalEsperado);
    const validationBadge = document.getElementById('letrasSumValidationBadge');
    if (validationBadge) {
      if (diff < 0.01 && totalEsperado > 0) {
        validationBadge.className = 'badge bg-success fs-7';
        validationBadge.innerHTML = `<i class="bi bi-check-circle me-1"></i> Suma Cuotas: S/ ${sum.toFixed(2)} (Exacto)`;
      } else {
        validationBadge.className = 'badge bg-warning text-dark fs-7';
        validationBadge.innerHTML = `<i class="bi bi-exclamation-triangle me-1"></i> Suma: S/ ${sum.toFixed(2)} | Total Op: S/ ${totalEsperado.toFixed(2)}`;
      }
    }
  }
}

export async function submitBatchLetras() {
  const clienteNombre = document.getElementById('letraClienteNombre')?.value?.trim();
  const refGirador = document.getElementById('letraRefGirador')?.value?.trim();
  const lugarGiro = document.getElementById('letraLugarGiro')?.value?.trim() || 'LIMA';
  const fechaGiro = document.getElementById('letraFechaGiro')?.value;
  const docCliente = document.getElementById('letraClienteDoc')?.value?.trim();
  const dirCliente = document.getElementById('letraClienteDireccion')?.value?.trim();

  if (!clienteNombre || !docCliente) {
    alert('Por favor seleccione un cliente con RUC válido.');
    return;
  }

  if (!refGirador || !fechaGiro) {
    alert('Por favor complete todos los campos obligatorios (*).');
    return;
  }

  if (!generatedInstallments || generatedInstallments.length === 0) {
    alert('No hay cuotas desglosadas para registrar.');
    return;
  }

  const payloadArray = generatedInstallments.map(inst => ({
    id_cliente: selectedClient?.id_cliente || selectedClient?.id || null,
    nombre_cliente: clienteNombre,
    nro_documento: docCliente,
    direccion_cliente: dirCliente,
    nro_letra: inst.nro_letra,
    numero_correlativo: inst.numero_correlativo,
    anio: inst.anio,
    ref_girador: refGirador.toUpperCase(),
    lugar_giro: lugarGiro.toUpperCase(),
    fecha_giro: fechaGiro,
    dias_credito: inst.dias_credito,
    fecha_vencimiento: inst.fecha_vencimiento,
    moneda: 'SOLES',
    monto: inst.monto,
    monto_letras: inst.monto_letras
  }));

  try {
    const res = await api.createLetrasBatch(payloadArray);
    if (res && res.success) {
      alert(`¡Se emitieron ${res.total_generadas || payloadArray.length} Letras de Cambio exitosamente para la referencia ${refGirador.toUpperCase()}!`);
      forceCloseGenerarModal();
      await initLetrasView();
    } else {
      alert('Error al registrar letras: ' + (res?.error || 'Respuesta inválida'));
    }
  } catch (err) {
    console.error('Error enviando letras batch:', err);
    alert('Error al procesar la solicitud: ' + err.message);
  }
}

// =========================================================================
// 4. DETALLES DE OPERACIÓN / TODAS LAS LETRAS POR REFERENCIA
// =========================================================================
export function openLoteDetailModal(idLote) {
  const lote = groupedLetras.find(g => String(g.id_lote) === String(idLote));
  if (!lote) return;

  const modalEl = document.getElementById('letraDetailModal');
  const bodyEl = document.getElementById('letraDetailBody');
  const titleEl = document.getElementById('letraDetailTitle');
  if (!modalEl || !bodyEl) return;

  if (titleEl) {
    titleEl.innerHTML = `<i class="bi bi-collection-fill me-2"></i> Operación: ${escapeHtml(lote.ref_girador)} - ${escapeHtml(lote.nombre_cliente)}`;
  }

  const montoFormatted = (parseFloat(lote.monto_total) || 0).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

  bodyEl.innerHTML = `
    <!-- Ficha Resumen de la Operación -->
    <div class="p-3 bg-body-tertiary rounded border mb-4">
      <div class="row g-3">
        <div class="col-md-4">
          <label class="small text-muted d-block font-monospace">REF. DEL GIRADOR</label>
          <span class="fs-5 fw-bold text-primary font-monospace">${escapeHtml(lote.ref_girador)}</span>
        </div>
        <div class="col-md-5">
          <label class="small text-muted d-block font-monospace">GIRADO A (CLIENTE)</label>
          <div class="fw-bold text-white fs-6">${escapeHtml(lote.nombre_cliente)}</div>
          <small class="text-warning font-monospace">RUC: ${escapeHtml(lote.nro_documento || '-')}</small>
        </div>
        <div class="col-md-3 text-end">
          <label class="small text-muted d-block font-monospace">MONTO TOTAL OPERACIÓN</label>
          <span class="badge bg-success fs-5 font-monospace">S/ ${montoFormatted}</span>
        </div>

        <div class="col-md-4">
          <label class="small text-muted d-block font-monospace">LUGAR Y FECHA DE GIRO</label>
          <div class="fw-semibold">${escapeHtml(lote.lugar_giro || 'LIMA')} - ${escapeHtml(lote.fecha_giro || '-')}</div>
        </div>
        <div class="col-md-5">
          <label class="small text-muted d-block font-monospace">DIRECCIÓN FISCAL</label>
          <div class="small">${escapeHtml(lote.direccion_cliente || '-')}</div>
        </div>
        <div class="col-md-3 text-end">
          <label class="small text-muted d-block font-monospace">TOTAL LETRAS</label>
          <span class="badge bg-primary fs-6">${lote.total_cuotas} ${lote.total_cuotas === 1 ? 'Letra' : 'Letras'}</span>
        </div>
      </div>
    </div>

    <!-- Tabla con Todas las Letras Desglosadas de la Referencia -->
    <h6 class="fw-bold text-white mb-2" style="color: #60a5fa !important;">
      <i class="bi bi-file-earmark-ruled me-1"></i> Letras de Cambio Emitidas para esta Referencia (${lote.total_cuotas})
    </h6>
    <div class="table-responsive border rounded mb-2">
      <table class="table custom-table table-sm align-middle mb-0">
        <thead>
          <tr>
            <th style="width: 35px;" class="text-center">#</th>
            <th style="width: 120px;">N° Letra</th>
            <th style="width: 100px;" class="text-center">Días Crédito</th>
            <th style="width: 120px;">F. Vencimiento</th>
            <th class="text-end" style="width: 130px;">Monto (S/)</th>
            <th>Monto en Letras</th>
            <th class="text-center" style="width: 70px;">PDF</th>
            <th class="text-center" style="width: 70px;">PRINT</th>
            <th class="text-center" style="width: 70px;">ANULAR</th>
          </tr>
        </thead>
        <tbody>
          ${lote.letras.map((l, idx) => {
    const isAnulada = l.estado === 'ANULADA';
    const mForm = (parseFloat(l.monto) || 0).toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });

    return `
              <tr>
                <td class="text-center fw-bold text-muted">${idx + 1}</td>
                <td class="font-monospace fw-bold text-primary">${escapeHtml(l.nro_letra)}</td>
                <td class="text-center font-monospace fw-bold text-warning">${l.dias_credito || '-'} d</td>
                <td class="small fw-semibold text-danger">${escapeHtml(l.fecha_vencimiento || '-')}</td>
                <td class="text-end fw-bold font-monospace">S/ ${mForm}</td>
                <td><small class="font-monospace text-muted d-block text-truncate" style="max-width: 260px;" title="${escapeHtml(l.monto_letras || '')}">${escapeHtml(l.monto_letras || '-')}</small></td>
                <td class="text-center">
                  <button class="btn-action-solid btn-pdf" title="Descargar PDF Oficial" onclick="letrasModule.downloadPdf(${l.id_letra})">
                    <i class="bi bi-file-earmark-pdf-fill"></i>
                  </button>
                </td>
                <td class="text-center">
                  <button class="btn-action-solid btn-print" title="Imprimir Directamente" onclick="letrasModule.printLetraDirect(${l.id_letra})">
                    <i class="bi bi-printer-fill"></i>
                  </button>
                </td>
                <td class="text-center">
                  ${isAnulada ? `
                    <button class="btn-action-solid btn-cancel" style="opacity: 0.35; cursor: not-allowed;" title="Ya anulada" disabled>
                      <i class="bi bi-slash-circle"></i>
                    </button>
                  ` : `
                    <button class="btn-action-solid btn-cancel" title="Anular Letra Individual" onclick="letrasModule.openAnularLetraModal(${l.id_letra}, '${escapeHtml(l.nro_letra)}')">
                      <i class="bi bi-x-lg"></i>
                    </button>
                  `}
                </td>
              </tr>
            `;
  }).join('')}
        </tbody>
      </table>
    </div>
  `;

  const modal = new bootstrap.Modal(modalEl);
  modal.show();
}

export function downloadPdf(idLetra) {
  const savedPath = localStorage.getItem('inplabel_letras_pdf_storage_path') || 'C:\\Inplabel\\Letras';
  const sub = localStorage.getItem('inplabel_pdf_subfolders') !== 'false';
  const url = `http://localhost:8080/api/letras/${idLetra}/pdf?storageDir=${encodeURIComponent(savedPath)}&useSubfolders=${sub}&_t=${Date.now()}`;
  window.open(url, '_blank');
}

export async function printLetraDirect(idLetra) {
  try {
    const url = `http://localhost:8080/api/letras/${idLetra}/pdf?_t=${Date.now()}`;
    const response = await fetch(url);
    if (!response.ok) {
      throw new Error(`HTTP error! status: ${response.status}`);
    }
    const blob = await response.blob();
    const blobUrl = URL.createObjectURL(blob);

    let printFrame = document.getElementById('silentLetraPrintFrame');
    if (printFrame) {
      printFrame.remove();
    }

    printFrame = document.createElement('iframe');
    printFrame.id = 'silentLetraPrintFrame';
    printFrame.style.position = 'fixed';
    printFrame.style.top = '-9999px';
    printFrame.style.left = '-9999px';
    printFrame.style.width = '100px';
    printFrame.style.height = '100px';
    printFrame.style.border = '0';
    printFrame.src = blobUrl;
    document.body.appendChild(printFrame);

    printFrame.onload = function() {
      setTimeout(() => {
        try {
          printFrame.contentWindow.focus();
          printFrame.contentWindow.print();
        } catch (e) {
          console.warn("Print execution error:", e);
        }
      }, 500);
    };
  } catch (err) {
    console.error("Error al preparar la impresión de la letra:", err);
  }
}

export function generateLetraPrintHTML(letra) {
  const nroLetra = escapeHtml(letra.nro_letra || '261-2025');
  const refGirador = escapeHtml(letra.ref_girador || 'FF02 - 630');
  const lugarGiro = escapeHtml((letra.lugar_giro || 'LIMA').toUpperCase());

  const fechaGiroParts = (letra.fecha_giro || '2026-08-15').split('-');
  const fgDia = fechaGiroParts[2] || '15';
  const fgMes = fechaGiroParts[1] || '08';
  const fgAnio = fechaGiroParts[0] || '2026';

  const fechaVencParts = (letra.fecha_vencimiento || '2026-09-14').split('-');
  const fvDia = fechaVencParts[2] || '14';
  const fvMes = fechaVencParts[1] || '09';
  const fvAnio = fechaVencParts[0] || '2026';

  const mNum = parseFloat(letra.monto) || 0;
  const moneda = (letra.moneda || 'SOLES').toUpperCase();
  const symbol = moneda.includes('DOLAR') || moneda.includes('USD') ? '$' : 'S/';
  const montoFormatted = `${symbol} ${mNum.toLocaleString('es-PE', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;

  const montoLetras = escapeHtml((letra.monto_letras || 'CERO CON 00 / 100 SOLES').toUpperCase());
  const cliente = escapeHtml((letra.nombre_cliente || 'CLIENTE S.A.C.').toUpperCase());
  const ruc = escapeHtml(letra.nro_documento || '-');
  const direccion = escapeHtml((letra.direccion_cliente || 'LIMA - LIMA').toUpperCase());

  return `
<!DOCTYPE html>
<html lang="es">
<head>
<meta charset="UTF-8">
<title>Letra de Cambio - ${nroLetra}</title>
<style>
  @page {
    size: A4 portrait;
    margin: 3mm 4mm;
  }
  * {
    box-sizing: border-box;
    -webkit-print-color-adjust: exact !important;
    print-color-adjust: exact !important;
  }
  html, body {
    margin: 0;
    padding: 0;
    background: #fff;
    font-family: Calibri, 'Segoe UI', Arial, sans-serif;
    color: #141414;
    font-size: 9.5px;
  }
  .sheet {
    width: 100%;
    max-width: 820px;
    margin: 0 auto;
    padding: 2px;
    page-break-inside: avoid;
    break-inside: avoid;
  }

  /* Encabezado Superior con Logo y Dirección */
  .header-top {
    display: flex;
    justify-content: space-between;
    align-items: flex-end;
    margin-bottom: 5px;
  }
  .logo-img {
    height: 40px;
    object-fit: contain;
  }
  .company-address {
    text-align: right;
    font-size: 8.2px;
    line-height: 1.35;
    border-top: 1.2px solid #333;
    padding-top: 2px;
    min-width: 320px;
  }

  /* Estructura general de 3 columnas principales */
  .letra-layout {
    display: flex;
    align-items: stretch;
    gap: 4px;
    page-break-inside: avoid;
    break-inside: avoid;
  }

  /* COLUMNA 1: CLÁUSULAS ESPECIALES COMPLETAS (CALIBRI 7) */
  .clauses-col {
    width: 88px;
    min-width: 88px;
    max-width: 88px;
    display: flex;
    flex-direction: column;
    justify-content: flex-end;
    padding: 1.5px 0;
  }
  .clauses-content {
    writing-mode: vertical-rl;
    transform: rotate(180deg);
    font-family: Calibri, sans-serif;
    font-size: 7pt;
    line-height: 1.25;
    color: #111;
    text-align: left;
  }
  .clauses-title {
    font-weight: bold;
    font-size: 7.2pt;
    margin-bottom: 2px;
  }
  .clause-p {
    margin-bottom: 2px;
    text-align: justify;
  }

  /* COLUMNA 2: LÍNEAS DISCONTINUAS ACEPTANTE Y POR AVAL */
  .tags-col {
    width: 18px;
    min-width: 18px;
    max-width: 18px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    align-items: center;
    padding: 0;
  }
  .tag-group {
    display: flex;
    flex-direction: column;
    align-items: center;
    width: 100%;
  }
  .tag-line {
    border-left: 1.2px dashed #141414;
    width: 1px;
    flex: 1;
    min-height: 35px;
  }
  .tag-text {
    writing-mode: vertical-rl;
    transform: rotate(180deg);
    font-size: 7.5pt;
    font-weight: bold;
    white-space: nowrap;
    margin: 3px 0;
  }

  /* COLUMNA 3: CONTENIDO PRINCIPAL */
  .main-col {
    flex: 1;
    display: flex;
    flex-direction: column;
    min-width: 0;
  }

  /* Tabla Superior de Giro & Vencimiento */
  .top-table {
    border-collapse: collapse;
    width: 100%;
    table-layout: fixed;
    margin-bottom: 0;
  }
  .top-table th, .top-table td {
    border: 1px solid #141414;
    text-align: center;
    padding: 1.5px;
  }
  .top-table th {
    font-size: 7.2pt;
    font-weight: bold;
    line-height: 1.15;
    background: #E2EFDA !important;
  }
  .top-table .subhead {
    font-size: 6.5pt;
    font-weight: bold;
    padding: 1px;
    background: #E2EFDA !important;
  }
  .top-table td.val {
    font-size: 10.5pt;
    font-weight: bold;
    height: 28px;
    padding: 1px;
  }
  .top-table td.val.amount {
    font-size: 13pt;
  }
  .top-table td.val.date-val {
    font-size: 8.8pt;
  }

  /* Frase de pago */
  .pay-phrase {
    border-left: 1px solid #141414;
    border-right: 1px solid #141414;
    padding: 3px 6px;
    font-size: 7.8pt;
  }
  .pay-phrase .beneficiary {
    color: #00AF50;
    font-weight: bold;
  }

  /* Monto en letras */
  .amount-box {
    border: 1px solid #141414;
    padding: 3.5px 6px;
    font-size: 8.6pt;
    font-weight: bold;
    background: #fafafa;
  }

  /* Nota lugar de pago */
  .payplace-note {
    border-left: 1px solid #141414;
    border-right: 1px solid #141414;
    border-bottom: 1px solid #141414;
    padding: 2px 6px;
    font-size: 7.2pt;
  }

  /* Sección Inferior (UN SOLO BLOQUE SIN LÍNEA VERTICAL INTERMEDIA) */
  .lower-box {
    display: flex;
    border: 1px solid #141414;
    border-top: none;
  }

  /* Cuadro Izquierdo (Girado / Avalista) */
  .lower-left {
    width: 55%;
    padding: 4px 7px;
    display: flex;
    flex-direction: column;
    justify-content: space-between;
    font-size: 7.5pt;
  }
  .girado-row {
    margin-bottom: 2.5px;
    line-height: 1.3;
  }
  .girado-row span.lbl {
    display: inline-block;
    min-width: 60px;
    font-weight: bold;
  }
  .girado-divider {
    border-top: 1px solid #333;
    margin: 3px 0;
  }
  .aval-row {
    margin-bottom: 2.5px;
    display: flex;
    align-items: flex-end;
  }
  .aval-row span.lbl {
    min-width: 60px;
    flex-shrink: 0;
    font-weight: bold;
  }
  .aval-dots {
    border-bottom: 1px dotted #333;
    flex: 1;
    height: 10px;
    margin-left: 2px;
  }

  /* Cuadro Derecho (Banco / BELSA / Firma) */
  .lower-right {
    width: 45%;
    padding: 4px 7px;
    display: flex;
    flex-direction: column;
    text-align: center;
  }
  .debit-line {
    font-size: 7.2pt;
    text-align: left;
    margin-bottom: 3px;
  }
  .bank-table {
    width: 100%;
    border-collapse: collapse;
    margin-bottom: 3px;
  }
  .bank-table th, .bank-table td {
    border: 1px solid #141414;
    padding: 1.5px;
    font-size: 6.8pt;
  }
  .bank-table th {
    font-weight: bold;
    background: #E2EFDA !important;
  }
  .bank-table td {
    height: 12px;
  }
  .company-title {
    color: #00AF50;
    font-weight: bold;
    font-size: 9pt;
    margin-top: 2px;
  }
  .company-ruc {
    font-weight: bold;
    font-size: 8pt;
    margin-bottom: 4px;
  }
  .firma-block {
    margin-top: auto;
    text-align: center;
  }
  .sign-line {
    border-bottom: 1px dotted #333;
    width: 75%;
    margin: 4px auto 2px auto;
  }
  .sign-text {
    font-size: 7.5pt;
    font-weight: bold;
  }
  .rep-text {
    font-size: 6.5pt;
    color: #333;
  }
  .doi-text {
    font-size: 7.2pt;
    font-weight: bold;
    text-align: left;
    margin-top: 2px;
  }

  /* Pie de página */
  .footer-line {
    margin-top: 3px;
    font-size: 6.8pt;
    text-align: left;
    color: #444;
    margin-left: 11%;
  }
</style>
</head>
<body>

<div class="sheet">
  <div class="header-top">
    <img src="img/inplabel-logo.png" alt="Inplabel" class="logo-img" onerror="this.style.display='none'">
    <div class="company-address">
      <b>Av. María Parado de Bellido Lte. 5</b><br>
      Lotización Chacra Cerro - Comas - Lima - Lima<br>
      Telf.: (01)557-1526 Claro: 975 564 460 / 983 518 504
    </div>
  </div>

  <div class="letra-layout">
    <!-- COLUMNA 1: CLÁUSULAS -->
    <div class="clauses-col">
      <div class="clauses-content">
        <div class="clauses-title">CLÁUSULAS ESPECIALES:</div>
        <div class="clause-p">(1) En caso de mora , esta letra de cambio generará las tasas de interés compensatorio y moratorio más altas que la ley permita a su último Tenedor.</div>
        <div class="clause-p">(2) El plazo de su vencimiento podrá ser prorrogado por el tenedor, por el plazo que este señale, sin que sea necesario la intervención del obligado principal ni de los solidarios.</div>
        <div class="clause-p">(3) Las partes acuerdan consignar la cláusula "sin protesto" y por tanto no se requerirá de esta diligencia para el ejercicio de las acciones cambiarias.</div>
        <div class="clause-p">(4) Las partes se someten a la competencia de los jueces del Distrito Judicial de Lima.</div>
      </div>
    </div>

    <!-- COLUMNA 2: LÍNEAS DISCONTINUAS ACEPTANTE Y POR AVAL -->
    <div class="tags-col">
      <div class="tag-group" style="height: 52%;">
        <div class="tag-line"></div>
        <span class="tag-text">Aceptante(s)</span>
        <div class="tag-line"></div>
      </div>
      <div class="tag-group" style="height: 44%;">
        <div class="tag-line"></div>
        <span class="tag-text">Por Aval</span>
        <div class="tag-line"></div>
      </div>
    </div>

    <!-- COLUMNA 3: PRINCIPAL -->
    <div class="main-col">
      <!-- 1. SECCION SUPERIOR DE DATOS (CON ROWSPAN=2 EN COLUMNAS DIRECTAS) -->
      <table class="top-table">
        <colgroup>
          <col style="width: 18%;">
          <col style="width: 18%;">
          <col style="width: 14%;">
          <col style="width: 5.7%;"><col style="width: 5.7%;"><col style="width: 5.7%;">
          <col style="width: 5.7%;"><col style="width: 5.7%;"><col style="width: 5.7%;">
          <col style="width: 16.1%;">
        </colgroup>
        <tr>
          <th rowspan="2">NUMERO DE LETRA</th>
          <th rowspan="2">REF. DEL GIRADOR</th>
          <th rowspan="2">LUGAR DE GIRO</th>
          <th colspan="3">FECHA DE GIRO</th>
          <th colspan="3">FECHA DE VENCIMIENTO</th>
          <th rowspan="2">MONEDA E IMPORTE</th>
        </tr>
        <tr>
          <th class="subhead">DIA</th><th class="subhead">MES</th><th class="subhead">AÑO</th>
          <th class="subhead">DIA</th><th class="subhead">MES</th><th class="subhead">AÑO</th>
        </tr>
        <tr>
          <td class="val">${nroLetra}</td>
          <td class="val">${refGirador}</td>
          <td class="val">${lugarGiro}</td>
          <td class="val date-val">${fgDia}</td><td class="val date-val">${fgMes}</td><td class="val date-val">${fgAnio}</td>
          <td class="val date-val">${fvDia}</td><td class="val date-val">${fvMes}</td><td class="val date-val">${fvAnio}</td>
          <td class="val amount">${montoFormatted}</td>
        </tr>
      </table>

      <!-- 2. FRASE DE PAGO -->
      <div class="pay-phrase">
        Por esta <b>LETRA DE CAMBIO</b>, se servirá(n) pagar a la orden de <span class="beneficiary">INDUSTRIAS PLASTICOS BELSA S.A.C.</span> la cantidad de:
      </div>

      <!-- 3. MONTO EN LETRAS -->
      <div class="amount-box">
        ${montoLetras}
      </div>

      <!-- 4. LUGAR DE PAGO -->
      <div class="payplace-note">
        Valor que sentará(n) en cuenta según aviso de sus Ss. Ss. en el siguiente lugar de pago:
      </div>

      <!-- 5. SECCION INFERIOR (GIRADO + BANCO/BELSA/FIRMA) -->
      <div class="lower-box">
        <div class="lower-left">
          <div class="girado-row">
            <span class="lbl">GIRADO A:</span> <b>${cliente}</b>
          </div>
          <div class="girado-row">
            <span class="lbl">RUC:</span> ${ruc}
          </div>
          <div class="girado-row">
            <span class="lbl">DIRECCION:</span> ${direccion}
          </div>
          <div class="girado-divider"></div>
          <div class="aval-row">
            <span class="lbl">AVALISTA:</span> <div class="aval-dots"></div>
          </div>
          <div class="aval-row">
            <span class="lbl">D.I/R.U.C:</span> <div class="aval-dots" style="max-width: 100px;"></div>
            <span style="margin: 0 4px; font-weight: bold;">TELEFONO:</span> <div class="aval-dots"></div>
          </div>
          <div class="aval-row">
            <span class="lbl">DIRECCION:</span> <div class="aval-dots"></div>
          </div>
        </div>

        <div class="lower-right">
          <div class="debit-line">
            Importe a debitar en cuenta del Aceptante del Banco:..................................
          </div>
          <table class="bank-table">
            <tr>
              <th>BANCO</th>
              <th>OFICINA</th>
              <th>NUMERO DE CUENTA</th>
              <th>D.C.</th>
            </tr>
            <tr>
              <td></td>
              <td></td>
              <td></td>
              <td></td>
            </tr>
          </table>
          <div class="company-title">INDUSTRIAS PLASTICOS BELSA S.A.C.</div>
          <div class="company-ruc">RUC: 20544368827</div>
          <div class="firma-block">
            <div class="sign-line"></div>
            <div class="sign-text">FIRMA</div>
            <div class="rep-text">Nombre del representante(S)</div>
            <div class="doi-text">D.O.I</div>
          </div>
        </div>
      </div>

      <!-- 6. PIE DE PÁGINA -->
      <div class="footer-line">
        No escribir ni firmar debajo de esta linea
      </div>
    </div>
  </div>
</div>

</body>
</html>
  `;
}

// =========================================================================
// 5. ANULACIÓN DE LETRAS (INDIVIDUAL Y POR LOTE)
// =========================================================================
export function openAnularLoteModal(idLote, refGirador, cliente) {
  document.getElementById('anularLoteIdInput').value = idLote;
  document.getElementById('anularNroLetraLabel').textContent = refGirador;
  document.getElementById('anularLetraClienteLabel').textContent = cliente;

  const modalEl = document.getElementById('modalAnularLetra');
  if (modalEl) {
    const modal = new bootstrap.Modal(modalEl);
    modal.show();
  }
}

export async function confirmAnularLote() {
  const idLote = document.getElementById('anularLoteIdInput')?.value;
  if (!idLote) return;

  try {
    const res = await api.anularLoteLetras(idLote);
    if (res && res.success) {
      const modalEl = document.getElementById('modalAnularLetra');
      if (modalEl) {
        const modal = bootstrap.Modal.getInstance(modalEl);
        if (modal) modal.hide();
      }
      await initLetrasView();
    } else {
      alert('Error al anular la operación: ' + (res?.error || ''));
    }
  } catch (err) {
    console.error('Error anulando lote:', err);
  }
}

export async function openAnularLetraModal(idLetra, nroLetra) {
  if (!confirm(`¿Está seguro de anular la Letra de Cambio individual N° ${nroLetra}?`)) {
    return;
  }

  try {
    const res = await api.anularLetra(idLetra);
    if (res && res.success) {
      // Refresh current lote details and main view
      const detailModalEl = document.getElementById('letraDetailModal');
      if (detailModalEl) {
        const dModal = bootstrap.Modal.getInstance(detailModalEl);
        if (dModal) dModal.hide();
      }
      await initLetrasView();
    } else {
      alert('Error al anular la letra individual');
    }
  } catch (err) {
    console.error('Error anulando letra:', err);
  }
}
