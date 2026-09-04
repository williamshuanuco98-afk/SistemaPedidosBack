// Synchronous Embedded View Templates for 100% Offline / Local / Fetch-Proof Instant Rendering

const EMBEDDED_VIEWS = {
  dashboard: `
<!-- Top Metrics 5 KPI Grid -->
<div class="metrics-grid mb-4">
  <div class="metric-card card-blue">
    <div class="metric-info">
      <h4>Total Pedidos</h4>
      <div id="statTotalOrders" class="metric-value">0</div>
    </div>
    <div class="metric-icon-box">
      <i class="bi bi-cart3 fs-3"></i>
    </div>
  </div>

  <div class="metric-card card-amber">
    <div class="metric-info">
      <h4>Pedidos Pendientes</h4>
      <div id="statPendingOrders" class="metric-value">0</div>
    </div>
    <div class="metric-icon-box">
      <i class="bi bi-clock-history fs-3"></i>
    </div>
  </div>

  <div class="metric-card card-emerald">
    <div class="metric-info">
      <h4>Pedidos Entregados</h4>
      <div id="statCompletedOrders" class="metric-value">0</div>
    </div>
    <div class="metric-icon-box">
      <i class="bi bi-check2-circle fs-3"></i>
    </div>
  </div>

  <div class="metric-card card-cyan">
    <div class="metric-info">
      <h4>Clientes Registrados</h4>
      <div id="statTotalClients" class="metric-value">0</div>
    </div>
    <div class="metric-icon-box">
      <i class="bi bi-people fs-3"></i>
    </div>
  </div>

  <div class="metric-card card-indigo">
    <div class="metric-info">
      <h4>Productos Totales</h4>
      <div id="statTotalProducts" class="metric-value">0</div>
    </div>
    <div class="metric-icon-box">
      <i class="bi bi-box-seam fs-3"></i>
    </div>
  </div>
</div>

<!-- Section Row 1: Cronograma Semanal de Entregas & Pedidos al Mes -->
<div class="row g-4 mb-4">
  <div class="col-lg-6">
    <div class="content-card p-3 h-100 d-flex flex-column justify-content-between">
      <div class="card-header border-0 pb-2 bg-transparent d-flex justify-content-between align-items-center flex-wrap gap-2">
        <h3 class="card-title d-flex align-items-center mb-0 fs-6 fw-bold">
          <i class="bi bi-calendar-week-fill text-primary me-2 fs-5"></i>
          <span>Cronograma de Entregas de la Semana</span>
        </h3>
        <span id="currentWeekRangeLabel" class="badge bg-primary-subtle text-primary border border-primary-subtle fs-8 px-2.5 py-1 fw-semibold">
          Semana Actual
        </span>
      </div>
      
      <div class="p-2 flex-fill d-flex flex-column justify-content-between">
        <!-- 6-Day Interactive Grid -->
        <div id="weeklyScheduleGrid" class="weekly-schedule-grid mb-3">
          <!-- Dynamically generated day cards -->
        </div>
        
        <!-- Summary & Quick Filter Footer -->
        <div class="p-2.5 rounded bg-body-tertiary border d-flex align-items-center justify-content-between flex-wrap gap-2 fs-8">
          <div class="d-flex align-items-center gap-2">
            <span class="badge bg-warning text-dark px-2 py-0.5 fw-bold">HOY</span>
            <span id="todayDeliverySummary" class="text-body fw-semibold">Verificando entregas programadas...</span>
          </div>
          <button type="button" id="btnFilterTodayOrders" class="btn btn-sm btn-outline-primary py-0.5 px-2.5 fs-8" onclick="dashboardModule.filterTodayOrders()">
            <i class="bi bi-funnel-fill me-1"></i> Ver pedidos de hoy
          </button>
        </div>
      </div>
    </div>
  </div>

  <div class="col-lg-6">
    <div class="content-card p-3 h-100">
      <div class="card-header border-0 pb-0 bg-transparent">
        <h3 class="card-title d-flex align-items-center">
          <i class="bi bi-graph-up-arrow text-success me-2"></i>
          <span>Cantidad de Pedidos al Mes</span>
        </h3>
      </div>
      <div class="p-3" style="height: 280px; position: relative;">
        <canvas id="chartMonthlyOrders"></canvas>
      </div>
    </div>
  </div>
</div>

<!-- Charts Section Row 2: Estado Operativo & Productos Más Pedidos -->
<div class="row g-4 mb-4">
  <div class="col-lg-6">
    <div class="content-card p-3 h-100">
      <div class="card-header border-0 pb-0 bg-transparent">
        <h3 class="card-title d-flex align-items-center">
          <i class="bi bi-pie-chart-fill text-warning me-2"></i>
          <span>Distribución de Pedidos por Estado Operativo</span>
        </h3>
      </div>
      <div class="p-3" style="height: 280px; position: relative;">
        <canvas id="chartOrderStatus"></canvas>
      </div>
    </div>
  </div>

  <div class="col-lg-6">
    <div class="content-card p-3 h-100">
      <div class="card-header border-0 pb-0 bg-transparent">
        <h3 class="card-title d-flex align-items-center">
          <i class="bi bi-award-fill text-info me-2"></i>
          <span>Productos Más Solicitados</span>
        </h3>
      </div>
      <div class="p-3" style="height: 280px; position: relative;">
        <canvas id="chartTopProducts"></canvas>
      </div>
    </div>
  </div>
</div>

<!-- Recent Orders Section -->
<div class="row g-4">
  <div class="col-12">
    <div class="content-card h-100">
      <div class="card-header bg-transparent d-flex justify-content-between align-items-center">
        <h3 class="card-title d-flex align-items-center mb-0">
          <i class="bi bi-cart3 text-primary me-2"></i>
          <span>Últimos Pedidos Registrados</span>
        </h3>
        <button class="btn btn-sm btn-outline-secondary" onclick="app.navigateTo('pedidos')">Ver Todos</button>
      </div>
      <div class="table-responsive">
        <table class="table custom-table mb-0 align-middle">
          <thead>
            <tr>
              <th>N° Pedido</th>
              <th>Cliente</th>
              <th>Establecimiento</th>
              <th>Fecha Pedido</th>
              <th>Estado</th>
              <th class="text-center">Acciones</th>
            </tr>
          </thead>
          <tbody id="dashOrdersTable"></tbody>
        </table>
      </div>
    </div>
  </div>
</div>
`,

  pedidos: `
<div class="content-card">
  <!-- Cabecera de la Vista -->
  <div class="card-header flex-wrap gap-2">
    <div class="d-flex align-items-center gap-2">
      <h3 class="card-title mb-0"><i class="bi bi-cart-check text-primary"></i> Listado de Pedidos</h3>
      <span id="ordersCountBadge" class="badge bg-secondary">0 pedidos</span>
    </div>
    <div class="d-flex align-items-center gap-2">
      <button class="btn btn-primary" onclick="app.navigateTo('nuevo-pedido')">
        <i class="bi bi-plus-lg me-1"></i> Ingresar Pedido
      </button>
    </div>
  </div>

  <!-- Barra de Búsqueda y Filtros de Pedidos -->
  <div class="p-3 bg-body-tertiary border-bottom">
    <form id="formSearchPedidos" onsubmit="event.preventDefault(); app.triggerPedidosSearch();">
      <div class="row g-2 align-items-end">
        <!-- Buscar por Cliente o N° Orden -->
        <div class="col-md-3">
          <label for="searchClientNameInput" class="form-label small fw-bold mb-1">Cliente / N° Orden</label>
          <div class="input-group input-group-sm">
            <span class="input-group-text"><i class="bi bi-search"></i></span>
            <input type="text" id="searchClientNameInput" class="form-control"
              placeholder="Buscar (Enter)...">
          </div>
        </div>

        <!-- Fecha Desde -->
        <div class="col-md-2">
          <label for="filterDateFrom" class="form-label small fw-bold mb-1">Desde</label>
          <input type="date" id="filterDateFrom" class="form-control form-control-sm">
        </div>

        <!-- Fecha Hasta -->
        <div class="col-md-2">
          <label for="filterDateTo" class="form-label small fw-bold mb-1">Hasta</label>
          <input type="date" id="filterDateTo" class="form-control form-control-sm">
        </div>

        <!-- Filtro Estado del Pedido -->
        <div class="col-md-2">
          <label for="filterOrderStatus" class="form-label small fw-bold mb-1">Estado</label>
          <select id="filterOrderStatus" class="form-select form-select-sm">
            <option value="ALL" selected>TODOS LOS ESTADOS</option>
            <option value="PENDIENTE">PENDIENTE</option>
            <option value="EN_PROCESO">EN PROCESO</option>
            <option value="FUERA_DE_TIEMPO">FUERA DE TIEMPO</option>
            <option value="COMPLETADO">COMPLETADO</option>
            <option value="FINALIZADO">FINALIZADO</option>
            <option value="CANCELADO">CANCELADO</option>
          </select>
        </div>

        <!-- Seleccionador de Establecimiento (2 opciones) -->
        <div class="col-md-2">
          <label for="filterEstablishment" class="form-label small fw-bold mb-1">Establecimiento</label>
          <select id="filterEstablishment" class="form-select form-select-sm">
            <option value="ALL">Todos los locales</option>
            <option value="COMAS">Planta - Comas</option>
            <option value="CARABAYLLO">Sucursal - Carabayllo</option>
          </select>
        </div>

        <!-- Botón de Búsqueda -->
        <div class="col-lg-1 col-md-2 col-12">
          <button type="submit" class="btn btn-primary btn-sm w-100">
            <i class="bi bi-search me-1"></i> Buscar
          </button>
        </div>
      </div>
    </form>
  </div>

  <!-- Tabla de Pedidos -->
  <div class="table-responsive">
    <table class="table custom-table mb-0">
      <thead>
        <tr>
          <th>N° Pedido</th>
          <th>N° Orden</th>
          <th>Cliente / Razón Social</th>
          <th>Fecha Pedido</th>
          <th>Establecimiento</th>
          <th>Estado</th>
          <th class="text-center" style="width: 140px;">ACCIONES</th>
        </tr>
      </thead>
      <tbody id="pedidosTableBody"></tbody>
    </table>
    <div id="pedidosPaginationContainer"></div>
  </div>
</div>

<!-- Modal Finalizar Orden (Advertencia de Cancelación o Completar con Guía) -->
<div class="modal fade" id="modalFinalizarOrden" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content border-0 shadow">
      <div class="modal-header bg-body-tertiary">
        <h5 class="modal-title fw-bold text-primary">
          <i class="bi bi-flag-fill me-2"></i>Finalizar Orden <span id="finalizarOrderNroBadge" class="badge bg-secondary ms-1"></span>
        </h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body p-4">
        <form id="formFinalizarOrden" onsubmit="event.preventDefault(); pedidosModule.saveFinalizarOrden();">
          <input type="hidden" id="finalizarOrderId">
          
          <div class="alert alert-warning d-flex align-items-center mb-3">
            <i class="bi bi-exclamation-triangle-fill fs-4 me-3 text-warning"></i>
            <div>
              <strong>¿Está seguro de finalizar esta orden?</strong><br>
              Seleccione la resolución final del pedido:
            </div>
          </div>

          <div class="mb-3">
            <label class="form-label fw-bold mb-2">Resolución de la Orden *</label>
            <div class="d-flex flex-wrap gap-3">
              <div class="form-check">
                <input class="form-check-input" type="radio" name="finalizarStatusOption" id="optCompletado" value="COMPLETADO" checked onchange="pedidosModule.toggleFinalizarFields('COMPLETADO')">
                <label class="form-check-label fw-bold text-success" for="optCompletado">
                  <i class="bi bi-check-circle-fill me-1"></i> Completado (100% Entregado)
                </label>
              </div>
              <div class="form-check">
                <input class="form-check-input" type="radio" name="finalizarStatusOption" id="optFinalizado" value="FINALIZADO" onchange="pedidosModule.toggleFinalizarFields('FINALIZADO')">
                <label class="form-check-label fw-bold" style="color:#8b5cf6;" for="optFinalizado">
                  <i class="bi bi-flag-fill me-1"></i> Finalizado (Entrega Parcial / Cierra Saldo)
                </label>
              </div>
              <div class="form-check">
                <input class="form-check-input" type="radio" name="finalizarStatusOption" id="optCancelado" value="CANCELADO" onchange="pedidosModule.toggleFinalizarFields('CANCELADO')">
                <label class="form-check-label fw-semibold text-danger" for="optCancelado">
                  <i class="bi bi-x-circle-fill me-1"></i> Cancelado (Sin Entregas)
                </label>
              </div>
            </div>
          </div>

          <!-- Campos requeridos si es ENTREGADO -->
          <div id="finalizarDeliveryFields" class="card card-body bg-body-tertiary border-0 mb-3">
            <div class="row g-3">
              <div class="col-md-6">
                <label class="form-label fw-semibold fs-7">N° de Guía de Remisión *</label>
                <input type="text" id="finalizarNroGuia" class="form-control form-control-sm" placeholder="Ej: GUI-001-0458">
              </div>
              <div class="col-md-6">
                <label class="form-label fw-semibold fs-7">Fecha de Entrega *</label>
                <input type="date" id="finalizarFechaEntrega" class="form-control form-control-sm">
              </div>
            </div>
          </div>

          <!-- Campo para la Razón / Motivo de Cancelación o Finalización (Solo visible al Finalizar o Cancelar) -->
          <div id="finalizarMotivoContainer" class="mb-3" style="display: none;">
            <label for="finalizarMotivoInput" class="form-label fw-bold fs-7 mb-1 text-warning">
              <i class="bi bi-chat-right-text me-1"></i> Razón o Motivo de Finalización / Cancelación:
            </label>
            <textarea id="finalizarMotivoInput" class="form-control form-control-sm" rows="2"
              placeholder="Escriba la razón o motivo (ej: Solicitud del cliente, entrega parcial aceptada como final, etc.)..."></textarea>
            <div class="form-text fs-8">Esta razón quedará registrada en el historial del pedido.</div>
          </div>

          <div class="d-flex justify-content-end gap-2 mt-4">
            <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancelar</button>
            <button type="submit" class="btn btn-primary btn-sm px-3">
              <i class="bi bi-save me-1"></i> Confirmar y Guardar
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</div>

<!-- Modal Registrar Envío / Entrega Parcial -->
<div class="modal fade" id="modalRegistrarEnvio" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-dialog-centered">
    <div class="modal-content border-0 shadow">
      <div class="modal-header bg-body-tertiary">
        <h5 class="modal-title fw-bold text-primary">
          <i class="bi bi-truck me-2"></i>Registrar Envío de Pedido <span id="envioOrderNroBadge" class="badge bg-secondary ms-1"></span>
        </h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body p-4">
        <form id="formRegistrarEnvio" onsubmit="event.preventDefault(); pedidosModule.saveRegistrarEnvio();">
          <input type="hidden" id="envioOrderId">
          <input type="hidden" id="envioClientId">

          <div class="row g-3 mb-3">
            <div class="col-md-6">
              <label class="form-label fw-semibold">N° de Guía de Remisión *</label>
              <input type="text" id="envioNroGuia" class="form-control" placeholder="Ej: G001-000458" required>
            </div>
            <div class="col-md-6">
              <label class="form-label fw-semibold">Fecha del Envío *</label>
              <input type="date" id="envioFechaGuia" class="form-control" required>
            </div>
          </div>

          <h6 class="fw-bold mt-4 mb-2 text-secondary"><i class="bi bi-box-seam me-1"></i> Cantidad de Productos Enviados en esta Entrega:</h6>
          <div class="table-responsive border rounded mb-3">
            <table class="table custom-table table-sm align-middle mb-0">
              <thead class="bg-body-tertiary">
                <tr>
                  <th>Código / Producto</th>
                  <th class="text-center">Cant. Solicitada</th>
                  <th class="text-center">Entregado Previo</th>
                  <th class="text-center" style="width: 150px;">Cant. a Enviar Ahora *</th>
                </tr>
              </thead>
              <tbody id="envioProductsTableBody"></tbody>
            </table>
          </div>

          <div class="form-check p-3 rounded bg-body-tertiary border mb-3">
            <input class="form-check-input" type="checkbox" id="envioCerrarSaldoCheck">
            <label class="form-check-label fw-bold text-primary" for="envioCerrarSaldoCheck">
              <i class="bi bi-flag-fill text-warning me-1"></i> Dar por FINALIZADO el pedido (Si es una entrega parcial y ya no se enviará más saldo)
            </label>
            <div class="form-text fs-8 mt-0 ms-4">Si se marca, el estado cambiará a <strong>FINALIZADO</strong> en lugar de mantenerse EN PROCESO.</div>
          </div>

          <div class="d-flex justify-content-end gap-2 mt-4">
            <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancelar</button>
            <button type="submit" class="btn btn-warning btn-sm text-dark fw-bold px-3">
              <i class="bi bi-truck me-1"></i> Guardar Envío
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</div>

<!-- Modal Registrar Pago para Pedido Existente -->
<div class="modal fade" id="modalAgregarPagoPedido" tabindex="-1" aria-hidden="true" style="z-index: 1070;">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content border-0 shadow">
      <div class="modal-header bg-success text-white py-3">
        <h5 class="modal-title fw-bold">
          <i class="bi bi-cash-stack me-2"></i> Registrar Pago / Abono <span id="pagoOrderNroBadge" class="badge bg-light text-dark ms-1"></span>
        </h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body p-4">
        <form id="formAgregarPagoPedido" onsubmit="event.preventDefault(); pedidosModule.savePagoPedido();">
          <input type="hidden" id="pagoOrderId">

          <div class="mb-3">
            <label for="pagoBancoSelect" class="form-label fw-bold fs-7">Banco / Medio de Pago *</label>
            <select id="pagoBancoSelect" class="form-select form-select-sm" required>
              <option value="BCP" selected>BCP - Banco de Crédito del Perú</option>
              <option value="BBVA">BBVA Continental</option>
              <option value="INTERBANK">INTERBANK</option>
              <option value="SCOTIABANK">SCOTIABANK</option>
              <option value="YAPE/PLIN">Yape / Plin</option>
              <option value="EFECTIVO">Efectivo / Caja</option>
            </select>
          </div>

          <div class="mb-3">
            <label for="pagoMontoInput" class="form-label fw-bold fs-7">Monto Abonado (S/) *</label>
            <input type="number" id="pagoMontoInput" class="form-control form-control-sm" placeholder="0.00" step="0.01" min="0.10" required>
          </div>

          <div class="mb-3">
            <label for="pagoFechaInput" class="form-label fw-bold fs-7">Fecha del Pago *</label>
            <input type="date" id="pagoFechaInput" class="form-control form-control-sm" required>
          </div>

          <div class="mb-3">
            <label for="pagoVoucherInput" class="form-label fw-bold fs-7">N° de Operación / Voucher (Opcional)</label>
            <input type="text" id="pagoVoucherInput" class="form-control form-control-sm" placeholder="Ej: OP-1234567">
          </div>

          <div class="d-flex justify-content-end gap-2 mt-4">
            <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancelar</button>
            <button type="submit" class="btn btn-success btn-sm px-3">
              <i class="bi bi-save me-1"></i> Guardar Pago
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</div>
`,

  'nuevo-pedido': `
<div class="content-card">
  <div class="card-header d-flex justify-content-between align-items-center">
    <div class="d-flex align-items-center gap-3">
      <button class="btn btn-outline-secondary btn-sm" onclick="app.confirmLeaveNuevoPedido()">
        <i class="bi bi-arrow-left"></i> Volver a Pedidos
      </button>
      <h3 class="card-title mb-0">
        <i class="bi bi-cart-plus text-primary"></i> Registrar Nuevo Pedido
      </h3>
    </div>
    <div>
      <span class="badge bg-primary fs-7">Operaciones Inplabel</span>
    </div>
  </div>

  <div class="p-4">
    <form id="formNuevoPedido" onsubmit="event.preventDefault();">
      <div class="row g-4">
        <div class="col-12">
          <div class="card shadow-sm border">
            <div class="card-header bg-body-tertiary">
              <h5 class="card-title text-primary mb-0 fs-6">
                <i class="bi bi-person-vcard me-2"></i>1. Datos del Cliente y Orden
              </h5>
            </div>
            <div class="card-body">
              <div class="row g-3">
                <div class="col-12 position-relative">
                  <label for="searchClientInput" class="form-label fw-bold">
                    Cliente / Razón Social o RUC <span class="text-danger">*</span>
                  </label>
                  <div class="input-group">
                    <span class="input-group-text"><i class="bi bi-search"></i></span>
                    <input type="text" id="searchClientInput" class="form-control form-control-lg fs-6"
                      placeholder="Escriba RUC o Razón Social (mínimo 3 caracteres)..." autocomplete="off">
                  </div>
                  <ul id="clientSearchResultsList" class="list-group position-absolute w-100 shadow mt-1 d-none"
                    style="z-index: 1050; max-height: 600px; overflow-y: auto;"></ul>
                </div>

                <div class="col-md-2">
                  <label for="establecimientoSelect" class="form-label fw-bold fs-7">
                    Establecimiento <span class="text-danger">*</span>
                  </label>
                  <select id="establecimientoSelect" class="form-select fs-7 px-2">
                    <option value="CARABAYLLO" selected>Sucursal - Carabayllo</option>
                    <option value="COMAS">Planta Principal - Comas</option>
                  </select>
                </div>

                <div class="col-md-2">
                  <label for="nroOrdenCompraInput" class="form-label fw-bold fs-7">N° Orden de Compra</label>
                  <input type="text" id="nroOrdenCompraInput" class="form-control fs-7" placeholder="Ej: OC-2026-089">
                </div>

                <div class="col-md-2">
                  <label for="condicionPagoSelect" class="form-label fw-bold fs-7">
                    Condición de Pago <span class="text-danger">*</span>
                  </label>
                  <select id="condicionPagoSelect" class="form-select fs-7"
                    onchange="nuevoPedidoModule.onCondicionPagoChange(this.value)">
                    <option value="CONTADO" selected>Contado</option>
                    <option value="CREDITO">Crédito</option>
                    <option value="FACTURA_NEGOCIABLE">Factura Negociable</option>
                    <option value="LETRAS">Letras</option>
                    <option value="TRANSFERENCIA">Transferencia</option>
                  </select>
                </div>

                <div class="col-md-2" id="diasCreditoContainer">
                  <label for="diasCreditoInput" class="form-label fw-bold fs-7">
                    Días de Crédito <span class="text-danger">*</span>
                  </label>
                  <input type="number" id="diasCreditoInput" class="form-control fs-7" placeholder="0" min="0" value="0" disabled>
                </div>

                <div class="col-md-2">
                  <label for="fechaIngresoInput" class="form-label fw-bold fs-7">Fecha de Ingreso</label>
                  <input type="date" id="fechaIngresoInput" class="form-control fs-7 px-1">
                </div>

                <div class="col-md-2">
                  <label for="fechaEntregaInput" class="form-label fw-bold fs-7">Fecha de Entrega</label>
                  <input type="date" id="fechaEntregaInput" class="form-control fs-7 px-1">
                </div>

                <div class="col-12 pt-3 border-top mt-3">
                  <div class="d-flex justify-content-between align-items-center bg-body-tertiary p-3 rounded border">
                    <div class="d-flex align-items-center gap-2">
                      <i class="bi bi-wallet2 text-primary fs-5"></i>
                      <div>
                        <strong class="d-block text-primary fs-7">Adelantos de Pago</strong>
                        <span class="text-muted small">(Opcional - Presione el botón para registrar abonos o pagos por adelantado)</span>
                      </div>
                    </div>
                    <button type="button" class="btn btn-outline-primary btn-sm px-3"
                      onclick="nuevoPedidoModule.openAdelantoModal()">
                      <i class="bi bi-plus-circle me-1"></i> Agregar Adelanto
                    </button>
                  </div>

                  <div id="containerAdelantosTabla" class="table-responsive mt-3 d-none">
                    <table class="table table-sm custom-table mb-0 border">
                      <thead>
                        <tr>
                          <th>Fecha Pago</th>
                          <th>Banco</th>
                          <th>N° Operación / Voucher</th>
                          <th class="text-end">Monto (S/)</th>
                          <th class="text-center">Acción</th>
                        </tr>
                      </thead>
                      <tbody id="tableAdelantosBody"></tbody>
                      <tfoot>
                        <tr class="table-active">
                          <td colspan="3" class="text-end fw-bold">Total Adelantado:</td>
                          <td class="text-end fw-bold text-success" id="totalAdelantoCell">S/ 0.00</td>
                          <td></td>
                        </tr>
                      </tfoot>
                    </table>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="col-12">
          <div class="card shadow-sm border">
            <div class="card-header bg-body-tertiary">
              <h5 class="card-title text-primary mb-0 fs-6">
                <i class="bi bi-box-seam me-2"></i>2. Búsqueda y Selección de Productos
              </h5>
            </div>
            <div class="card-body">
              <div class="position-relative mb-3">
                <label for="searchProductInput" class="form-label fw-bold">
                  Buscar Producto / Insumo <span class="text-danger">*</span>
                </label>
                <div class="input-group">
                  <span class="input-group-text"><i class="bi bi-search"></i></span>
                  <input type="text" id="searchProductInput" class="form-control"
                    placeholder="Escriba nombre o código del producto (mínimo 3 caracteres)..." autocomplete="off">
                </div>
                <ul id="productSearchResultsList" class="list-group position-absolute w-100 shadow mt-1 d-none"
                  style="z-index: 1050; max-height: 600px; overflow-y: auto;"></ul>
              </div>

              <div class="table-responsive border rounded mt-3">
                <table class="table custom-table mb-0">
                  <thead>
                    <tr>
                      <th style="width: 110px;">Código</th>
                      <th>Producto / Insumo Solicitado</th>
                      <th style="width: 140px;">Cantidad</th>
                      <th class="text-center" style="width: 90px;">Acción</th>
                    </tr>
                  </thead>
                  <tbody id="tableProductosBody">
                    <tr>
                      <td colspan="4" class="text-center text-muted py-4">No se han agregado productos a la tabla.</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
        </div>

        <div class="col-12">
          <div class="card shadow-sm border">
            <div class="card-header bg-body-tertiary">
              <h5 class="card-title text-primary mb-0 fs-6">
                <i class="bi bi-journal-text me-2"></i>3. Observaciones y Documentos Adjuntos
              </h5>
            </div>
            <div class="card-body">
              <div class="row g-3">
                <div class="col-md-6">
                  <label for="observacionesInput" class="form-label fw-bold">Observaciones del Pedido</label>
                  <textarea id="observacionesInput" class="form-control" rows="4"
                    placeholder="Escriba aquí cualquier comentario u observación relevante..."></textarea>
                </div>

                <div class="col-md-6">
                  <label for="archivosAdjuntosInput" class="form-label fw-bold">
                    <i class="bi bi-paperclip me-1"></i>Agregar Documentos Adjuntos
                  </label>
                  <input type="file" id="archivosAdjuntosInput" class="form-control" multiple
                    accept=".pdf,.doc,.docx,.png,.jpg,.jpeg,.xlsx"
                    onchange="nuevoPedidoModule.handleFilesAttached(this.files)">
                  <span class="form-text fs-7 text-muted d-block mt-1">
                    Adjunte archivos relevantes como Orden de Compra en PDF, bocetos o fichas técnicas.
                  </span>
                  <div id="listaArchivosAdjuntos" class="mt-3"></div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="col-12 d-flex justify-content-end gap-3 pt-2">
          <button type="button" class="btn btn-outline-secondary px-4" onclick="app.confirmLeaveNuevoPedido()">
            <i class="bi bi-x-circle me-1"></i> Cancelar
          </button>
          <button type="button" id="btnGuardarPedido" class="btn btn-primary px-4"
            onclick="nuevoPedidoModule.submitNuevoPedido()">
            <i class="bi bi-check-circle me-1"></i> Guardar Pedido
          </button>
        </div>
      </div>
    </form>
  </div>
</div>

<!-- MODAL EMERGENTE: REGISTRAR ADELANTO DE PAGO -->
<div class="modal fade" id="modalAdelantoPago" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title"><i class="bi bi-cash-coin text-primary me-2"></i> Registrar Adelanto de Pago</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">
        <form id="formModalAdelanto" onsubmit="event.preventDefault(); nuevoPedidoModule.addAdelantoFromModal();">
          <div class="mb-3">
            <label for="modalBancoSelect" class="form-label fw-bold">Banco / Medio de Pago <span class="text-danger">*</span></label>
            <select id="modalBancoSelect" class="form-select" required>
              <option value="BCP" selected>BCP - Banco de Crédito del Perú</option>
              <option value="BBVA">BBVA Continental</option>
              <option value="INTERBANK">INTERBANK</option>
              <option value="SCOTIABANK">SCOTIABANK</option>
              <option value="YAPE/PLIN">Yape / Plin</option>
              <option value="EFECTIVO">Efectivo / Caja</option>
            </select>
          </div>

          <div class="mb-3">
            <label for="modalMontoInput" class="form-label fw-bold">Monto del Adelanto (S/) <span class="text-danger">*</span></label>
            <input type="number" id="modalMontoInput" class="form-control" placeholder="0.00" step="0.01" min="0.10" required>
          </div>

          <div class="mb-3">
            <label for="modalFechaPagoInput" class="form-label fw-bold">Fecha del Pago <span class="text-danger">*</span></label>
            <input type="date" id="modalFechaPagoInput" class="form-control" required>
          </div>

          <div class="mb-3">
            <label for="modalVoucherInput" class="form-label fw-bold">N° de Operación / Voucher (Opcional)</label>
            <input type="text" id="modalVoucherInput" class="form-control" placeholder="Ejemplo: OP-9876543">
          </div>

          <div class="d-flex justify-content-end gap-2 mt-4">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
            <button type="submit" class="btn btn-primary"><i class="bi bi-plus-circle me-1"></i> Agregar Adelanto</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</div>
`,

  envios: `
<div class="content-card">
  <!-- Cabecera de la Vista -->
  <div class="card-header flex-wrap gap-2">
    <div class="d-flex align-items-center gap-2">
      <h3 class="card-title mb-0"><i class="bi bi-truck text-primary"></i> Listado de Guías de Remisión</h3>
      <span id="shipmentsCountBadge" class="badge bg-secondary">0 guías</span>
    </div>
    <div class="d-flex align-items-center gap-2">
      <button class="btn btn-primary" onclick="app.navigateTo('nueva-guia')">
        <i class="bi bi-plus-lg me-1"></i> Generar Nueva Guía
      </button>
    </div>
  </div>

  <!-- Barra de Búsqueda y Filtros de Guías -->
  <div class="p-3 bg-body-tertiary border-bottom">
    <form id="formSearchGuias" onsubmit="event.preventDefault(); app.triggerGuiasSearch();">
      <div class="row g-2 align-items-end">
        <!-- Buscar por N° Guía, Cliente o RUC/DNI -->
        <div class="col-md-4">
          <label for="searchGuiaInput" class="form-label small fw-bold mb-1">Cliente, N° Guía o Doc</label>
          <div class="input-group input-group-sm">
            <span class="input-group-text"><i class="bi bi-search"></i></span>
            <input type="text" id="searchGuiaInput" class="form-control" placeholder="Buscar cliente, N° guía (GR001-...) (Enter)...">
          </div>
        </div>

        <!-- Fecha Desde -->
        <div class="col-md-2">
          <label for="filterGuiaDateFrom" class="form-label small fw-bold mb-1">Desde</label>
          <input type="date" id="filterGuiaDateFrom" class="form-control form-control-sm">
        </div>

        <!-- Fecha Hasta -->
        <div class="col-md-2">
          <label for="filterGuiaDateTo" class="form-label small fw-bold mb-1">Hasta</label>
          <input type="date" id="filterGuiaDateTo" class="form-control form-control-sm">
        </div>

        <!-- Seleccionador de Local (Establecimiento) -->
        <div class="col-md-3">
          <label for="filterGuiaEstablishment" class="form-label small fw-bold mb-1">Local / Serie</label>
          <select id="filterGuiaEstablishment" class="form-select form-select-sm">
            <option value="ALL" selected>Todos los locales</option>
            <option value="CARABAYLLO">Carabayllo (Serie GR001)</option>
            <option value="COMAS">Comas (Serie GR002)</option>
          </select>
        </div>

        <!-- Botón de Búsqueda -->
        <div class="col-md-1">
          <button type="submit" class="btn btn-primary btn-sm w-100">
            <i class="bi bi-search me-1"></i> Buscar
          </button>
        </div>
      </div>
    </form>
  </div>

  <!-- Tabla de Guías -->
  <div class="table-responsive">
    <table class="table custom-table mb-0">
      <thead>
        <tr>
          <th>N° Guía</th>
          <th>Fecha</th>
          <th>Cliente</th>
          <th>N° Doc (RUC/DNI)</th>
          <th>Estado</th>
          <th class="text-center" style="width: 80px;">DETALLES</th>
          <th class="text-center" style="width: 70px;">PDF</th>
          <th class="text-center" style="width: 70px;">PRINT</th>
          <th class="text-center" style="width: 70px;">ANULAR</th>
        </tr>
      </thead>
      <tbody id="enviosTableBody"></tbody>
    </table>
    <div id="enviosPaginationContainer"></div>
  </div>
</div>

<!-- Modal Detalle de Guía -->
<div class="modal fade" id="guiaDetailModal" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-lg modal-dialog-centered">
    <div class="modal-content">
      <div class="modal-header bg-primary text-white py-3">
        <h5 class="modal-title fw-bold" id="guiaDetailTitle">
          <i class="bi bi-truck text-white me-2"></i> Detalle de Guía de Remisión
        </h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" onclick="hideBootstrapModal('guiaDetailModal')" aria-label="Close"></button>
      </div>
      <div class="modal-body p-4" id="guiaDetailBody">
        <!-- Dynamic content filled by viewGuiaDetail -->
      </div>
      <div class="modal-footer bg-body-tertiary">
        <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal" onclick="hideBootstrapModal('guiaDetailModal')">Cerrar</button>
        <button type="button" class="btn btn-success btn-sm" id="btnPrintGuiaModal">
          <i class="bi bi-printer me-1"></i> Imprimir Guía
        </button>
      </div>
    </div>
  </div>
</div>

<!-- Modal para Dar de Baja (Anular Guía) -->
<div class="modal fade" id="modalAnularGuia" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <div class="modal-header bg-danger text-white py-3">
        <h5 class="modal-title fw-bold">
          <i class="bi bi-exclamation-triangle-fill me-2"></i> Dar de Baja / Anular Guía
        </h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body p-4">
        <div class="alert alert-warning d-flex align-items-center mb-3 fs-7" role="alert">
          <i class="bi bi-exclamation-circle-fill me-2 fs-5"></i>
          <div>Esta acción registrará la guía como <strong>ANULADA</strong> en el sistema.</div>
        </div>
        <input type="hidden" id="anularGuiaIdInput">
        <div class="mb-3">
          <label class="form-label fs-7 fw-semibold">N° de Guía:</label>
          <div class="form-control-plaintext font-monospace fw-bold fs-6 text-primary" id="anularNroGuiaLabel">-</div>
        </div>
        <div class="mb-3">
          <label for="motivoAnulacionInput" class="form-label fs-7 fw-semibold">Motivo de Anulación / Baja:</label>
          <textarea class="form-control" id="motivoAnulacionInput" rows="3" placeholder="Ej: Error en datos del cliente o productos despachados..."></textarea>
        </div>
      </div>
      <div class="modal-footer bg-body-tertiary">
        <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancelar</button>
        <button type="button" class="btn btn-danger btn-sm" onclick="enviosModule.confirmAnularGuia()">
          <i class="bi bi-check-circle me-1"></i> Confirmar Anulación
        </button>
      </div>
    </div>
  </div>
</div>
`,

  'nueva-guia': `
<div class="content-card">
  <!-- Cabecera de la Vista -->
  <div class="card-header flex-wrap gap-2">
    <div class="d-flex align-items-center gap-2">
      <h3 class="card-title mb-0">
        <i class="bi bi-file-earmark-plus text-primary me-1"></i> Generar Nueva Guía de Remisión
      </h3>
    </div>
    <div class="d-flex align-items-center gap-2">
      <button type="button" class="btn btn-outline-secondary btn-sm" onclick="app.navigateTo('envios')">
        <i class="bi bi-arrow-left me-1"></i> Volver a Listado de Guías
      </button>
    </div>
  </div>

  <div class="card-body p-4">
    <form id="formNuevaGuia" onsubmit="event.preventDefault(); nuevaGuiaModule.submitNuevaGuia();">
      
      <!-- Fila 1: 1. N° Guía, 2. Documento de Referencia, 3. Local de Salida, 4. Dirección del Punto de Partida -->
      <div class="row g-2 mb-3 p-3 border rounded bg-body-tertiary align-items-end">
        <!-- 1. N° Guía -->
        <div class="col-xl-2 col-lg-2 col-md-3 col-6">
          <label for="nroGuiaInput" class="form-label fw-bold small mb-1 text-nowrap">N° Guía <span class="text-danger">*</span></label>
          <input type="text" id="nroGuiaInput" class="form-control form-control-sm fw-bold text-primary" readonly>
        </div>

        <!-- 2. Documento de Referencia -->
        <div class="col-xl-2 col-lg-2 col-md-3 col-6">
          <label for="docReferenciaGuiaInput" class="form-label fw-bold small mb-1 text-nowrap">Doc. Referencia</label>
          <input type="text" id="docReferenciaGuiaInput" class="form-control form-control-sm" placeholder="Ej: OC-1024">
        </div>

        <!-- 3. Local de Salida -->
        <div class="col-xl-2 col-lg-2 col-md-3 col-12">
          <label for="selectLocalGuia" class="form-label fw-bold small mb-1 text-nowrap">Local Salida <span class="text-danger">*</span></label>
          <select id="selectLocalGuia" class="form-select form-select-sm px-1" onchange="nuevaGuiaModule.onLocalChanged(this.value)">
            <option value="CARABAYLLO" selected>Carabayllo (GR001)</option>
            <option value="COMAS">Comas (GR002)</option>
          </select>
        </div>

        <!-- 4. Dirección del Punto de Partida -->
        <div class="col-xl-6 col-lg-6 col-md-3 col-12">
          <label for="puntoPartidaInput" class="form-label fw-bold small mb-1 text-nowrap">Dirección del Punto de Partida</label>
          <input type="text" id="puntoPartidaInput" class="form-control form-control-sm" readonly>
        </div>
      </div>

      <!-- Fila 2: Datos del Cliente, Fecha de Emisión y Punto de Llegada -->
      <div class="row g-2 mb-3 p-3 border rounded bg-body-tertiary">
        <!-- Buscador Interactivo de Cliente -->
        <div class="col-md-5 position-relative">
          <label for="searchClienteGuiaInput" class="form-label fw-bold small mb-1">Cliente / Razón Social <span class="text-danger">*</span></label>
          <div class="input-group input-group-sm">
            <span class="input-group-text"><i class="bi bi-person-search"></i></span>
            <input type="text" id="searchClienteGuiaInput" class="form-control" autocomplete="off">
          </div>
          <!-- Dropdown Autocompletado de Clientes -->
          <ul id="clientSearchResultsGuiaList" class="list-group position-absolute w-100 shadow-sm d-none z-3" style="max-height: 260px; overflow-y: auto;"></ul>
        </div>

        <div class="col-md-3">
          <label for="rucDniGuiaInput" class="form-label fw-bold small mb-1">RUC / DNI del Cliente</label>
          <input type="text" id="rucDniGuiaInput" class="form-control form-control-sm" readonly>
        </div>

        <div class="col-md-4">
          <label for="fechaEmisionGuiaInput" class="form-label fw-bold small mb-1">Fecha de Emisión <span class="text-danger">*</span></label>
          <input type="date" id="fechaEmisionGuiaInput" class="form-control form-control-sm">
        </div>

        <!-- Punto de Llegada (Solo Lectura) -->
        <div class="col-12 mt-2">
          <label for="puntoLlegadaGuiaInput" class="form-label fw-bold small mb-1">Punto de Llegada (Dirección del Cliente) <span class="text-danger">*</span></label>
          <input type="text" id="puntoLlegadaGuiaInput" class="form-control form-control-sm bg-body-secondary" readonly>
        </div>
      </div>

      <!-- Fila 3: Buscador y Selección de Productos -->
      <div class="mb-4">
        <h5 class="card-title fs-6 mb-2 text-secondary">
          <i class="bi bi-box-seam me-1"></i> Productos a Enviar en la Guía
        </h5>

        <!-- Buscador de Productos -->
        <div class="row g-2 mb-3">
          <div class="col-md-7 position-relative">
            <div class="input-group input-group-sm">
              <span class="input-group-text"><i class="bi bi-search"></i></span>
              <input type="text" id="searchProductGuiaInput" class="form-control" autocomplete="off">
            </div>
            <!-- Autocompletado Productos -->
            <ul id="productSearchResultsGuiaList" class="list-group position-absolute w-100 shadow mt-1 d-none" style="z-index: 1050; max-height: 600px; overflow-y: auto;"></ul>
          </div>
        </div>

        <!-- Tabla de Productos Seleccionados -->
        <div class="table-responsive border rounded">
          <table class="table custom-table table-sm align-middle mb-0">
            <thead class="bg-body-tertiary">
              <tr>
                <th style="width: 120px;">Código</th>
                <th>Descripción del Producto</th>
                <th style="width: 150px;" class="text-center">Cantidad Enviada</th>
                <th style="width: 80px;" class="text-center">Acción</th>
              </tr>
            </thead>
            <tbody id="tableProductosGuiaBody">
              <tr>
                <td colspan="4" class="text-center text-muted py-4">No se han agregado productos a la guía.</td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>

      <!-- Fila 4: Observaciones -->
      <div class="mb-4">
        <label for="observacionesGuiaInput" class="form-label fw-bold small mb-1">Observaciones</label>
        <textarea id="observacionesGuiaInput" class="form-control form-control-sm" rows="2"></textarea>
      </div>

      <!-- Botones de Acción -->
      <div class="d-flex justify-content-end gap-2 border-top pt-3">
        <button type="button" class="btn btn-outline-secondary" onclick="app.navigateTo('envios')">Cancelar</button>
        <button type="submit" id="btnGuardarGuia" class="btn btn-primary px-4">
          <i class="bi bi-check-circle me-1"></i> Emitir Guía de Remisión
        </button>
      </div>

    </form>
  </div>
</div>
`,

  clientes: `
<div class="content-card">
  <div class="card-header d-flex justify-content-between align-items-center">
    <h3 class="card-title mb-0"><i class="bi bi-people text-primary me-1"></i> Directorio de Clientes (MySQL)</h3>
    <button class="btn btn-primary btn-sm" onclick="clientesModule.openNewClientModal()">
      <i class="bi bi-person-plus me-1"></i> Registrar Cliente
    </button>
  </div>

  <!-- Cuadro de búsqueda ubicado debajo del título -->
  <div class="p-3 bg-body-tertiary border-bottom">
    <div class="input-group">
      <span class="input-group-text"><i class="bi bi-search"></i></span>
      <input type="text" id="searchClientesInput" class="form-control"
        placeholder="Buscar por RUC/DNI, Razón Social o Domicilio Fiscal..."
        oninput="clientesModule.filterClientes(this.value)">
    </div>
  </div>

  <!-- Tabla de Clientes con Columnas Separadas -->
  <div class="table-responsive">
    <table class="table custom-table mb-0 align-middle">
      <thead>
        <tr>
          <th style="width: 200px;">Tipo y N° Documento</th>
          <th>Razón Social / Nombre</th>
          <th>Domicilio Fiscal</th>
          <th style="width: 75px;" class="text-center">EDITAR</th>
          <th style="width: 75px;" class="text-center">ELIMINAR</th>
        </tr>
      </thead>
      <tbody id="clientesTableBody"></tbody>
    </table>
    <div id="clientesPaginationContainer"></div>
  </div>
</div>

<!-- Modal Registrar / Modificar Cliente con Consulta SUNAT Automática -->
<div class="modal fade" id="modalCliente" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title fw-bold" id="modalClienteTitle"><i class="bi bi-person-plus me-2 text-primary"></i>Registrar Cliente</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">
        <form id="formCliente" onsubmit="event.preventDefault(); clientesModule.saveClientFromModal();">
          <input type="hidden" id="modalClienteId" value="">
          <div class="row g-3">
            <div class="col-md-4">
              <label class="form-label fw-semibold">Tipo Doc. *</label>
              <select id="modalClienteTipoDoc" class="form-select" onchange="clientesModule.onTipoDocChange(this.value)">
                <option value="RUC" selected>RUC (11 dígitos)</option>
                <option value="DNI">DNI (8 dígitos)</option>
              </select>
            </div>
            <div class="col-md-8">
              <label class="form-label fw-semibold">N° Documento *</label>
              <div class="input-group">
                <input type="text" id="modalClienteNroDoc" class="form-control" placeholder="Ingrese 11 dígitos de RUC"
                  maxlength="11" required oninput="clientesModule.onNroDocInput(this.value)">
                <button type="button" class="btn btn-outline-primary" id="btnConsultarSunat" onclick="clientesModule.consultarSunatManual()">
                  <i class="bi bi-search me-1"></i> SUNAT
                </button>
              </div>
              <div id="sunatStatusFeedback" class="form-text mt-1 text-muted fs-8">
                Escriba 11 dígitos para consultar SUNAT automáticamente.
              </div>
            </div>
            <div class="col-12">
              <label class="form-label fw-semibold">Razón Social / Nombre Completo *</label>
              <input type="text" id="modalClienteRazonSocial" class="form-control" placeholder="Razón social del cliente" required>
            </div>
            <div class="col-12">
              <label class="form-label fw-semibold">Domicilio Fiscal</label>
              <textarea id="modalClienteDireccion" class="form-control" rows="2" placeholder="Domicilio fiscal completo devuelto por SUNAT"></textarea>
            </div>
          </div>
          <div class="d-flex justify-content-end gap-2 mt-4">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
            <button type="submit" id="btnSaveClienteModal" class="btn btn-primary"><i class="bi bi-save me-1"></i> Guardar Cliente</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</div>
`,

  productos: `
<div class="content-card">
  <div class="card-header d-flex justify-content-between align-items-center">
    <h3 class="card-title mb-0"><i class="bi bi-box-seam text-primary me-1"></i> Catálogo de Productos (MySQL)</h3>
    <button class="btn btn-primary btn-sm" onclick="productosModule.openNewProductModal()">
      <i class="bi bi-plus-lg me-1"></i> Registrar Producto
    </button>
  </div>

  <div class="p-3 bg-body-tertiary border-bottom">
    <div class="input-group">
      <span class="input-group-text"><i class="bi bi-search"></i></span>
      <input type="text" id="searchProductosInput" class="form-control"
        placeholder="Buscar por ID (#438), Nombre de producto o Tipo/Categoría..."
        oninput="productosModule.filterProductos(this.value)">
    </div>
  </div>

  <div class="table-responsive">
    <table class="table custom-table mb-0">
      <thead>
        <tr>
          <th style="width: 90px;">ID</th>
          <th>Nombre del Producto / Insumo</th>
          <th>Tipo de Producto</th>
          <th style="width: 100px;">Estado</th>
          <th style="width: 75px;" class="text-center">EDITAR</th>
          <th style="width: 75px;" class="text-center">ELIMINAR</th>
        </tr>
      </thead>
      <tbody id="productosTableBody"></tbody>
    </table>
    <div id="productosPaginationContainer"></div>
  </div>
</div>

<div class="modal fade" id="modalProducto" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content">
      <div class="modal-header">
        <h5 class="modal-title fw-bold" id="modalProductoTitle"><i class="bi bi-box-seam me-2"></i>Registrar Producto</h5>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body">
        <form id="formProducto" onsubmit="event.preventDefault(); productosModule.saveProductFromModal();">
          <input type="hidden" id="modalProductoId">
          <div class="mb-3">
            <label class="form-label fw-semibold">Nombre del Producto / Insumo *</label>
            <input type="text" id="modalProductoNombre" class="form-control" placeholder="Ej. BALDE INDUSTRIAL 4 LT C/BLANCO" required>
          </div>
          <div class="mb-3">
            <label class="form-label fw-semibold">Tipo / Categoría de Producto *</label>
            <select id="modalProductoTipo" class="form-select" required>
              <option value="" disabled selected>-- Seleccione una categoría --</option>
              <option value="BALDES">BALDES</option>
              <option value="FRASCOS">FRASCOS</option>
              <option value="GALONES">GALONES</option>
              <option value="TAPAS">TAPAS</option>
              <option value="ASAS">ASAS</option>
              <option value="PRODUCTOS COMPLEMENTARIOS">PRODUCTOS COMPLEMENTARIOS</option>
            </select>
          </div>
          <div class="d-flex justify-content-end gap-2 mt-4">
            <button type="button" class="btn btn-secondary" data-bs-dismiss="modal">Cancelar</button>
            <button type="submit" class="btn btn-primary"><i class="bi bi-save me-1"></i> Guardar Producto</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</div>
`,

  produccion: `
<div class="content-card">
  <!-- Cabecera de la Vista -->
  <div class="card-header flex-wrap gap-2">
    <div class="d-flex align-items-center gap-2">
      <h3 class="card-title mb-0"><i class="bi bi-gear-wide-connected text-primary"></i> Control de Producción</h3>
      <span id="produccionCountBadge" class="badge bg-secondary">0 productos</span>
    </div>
  </div>

  <div class="p-3">
    <!-- Indicadores Rápidos / 6 KPI Cards de Producción -->
    <div class="metrics-grid mb-4">
      <div class="metric-card card-blue">
        <div class="metric-info">
          <h4>Ítems Distintos</h4>
          <div id="statProdItemsCount" class="metric-value">0</div>
        </div>
        <div class="metric-icon-box">
          <i class="bi bi-box-seam fs-3"></i>
        </div>
      </div>

      <div class="metric-card card-indigo">
        <div class="metric-info">
          <h4>Más Solicitado</h4>
          <div id="statProdTopName" class="fs-7 fw-bold text-white text-truncate" style="max-width: 140px;" title="-">-</div>
          <div id="statProdTopQty" class="metric-value fs-5 mt-1">0 und</div>
        </div>
        <div class="metric-icon-box">
          <i class="bi bi-trophy fs-3"></i>
        </div>
      </div>

      <div class="metric-card card-cyan">
        <div class="metric-info">
          <h4>Cant. Clientes</h4>
          <div id="statProdClientsCount" class="metric-value">0</div>
        </div>
        <div class="metric-icon-box">
          <i class="bi bi-people fs-3"></i>
        </div>
      </div>

      <div class="metric-card card-emerald" style="cursor: pointer;" onclick="produccionModule.onCategoryFilterChange('FRASCOS')" title="Filtrar por Frascos">
        <div class="metric-info">
          <h4>Cant. Frascos</h4>
          <div id="statProdFrascosCount" class="metric-value">0</div>
        </div>
        <div class="metric-icon-box">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor">
            <rect x="9.5" y="1" width="5" height="2.5" rx="0.6"/>
            <path fill-rule="evenodd" clip-rule="evenodd" d="M10 3.5h4v1.8c2.2 1 3.5 3 3.5 6.2v9a1.5 1.5 0 0 1-1.5 1.5h-8A1.5 1.5 0 0 1 6.5 20.5V11.5c0-3.2 1.3-5.2 3.5-6.2V3.5zm4 6.8v6.2c1.3-.4 1.8-1.5 1.8-3.1 0-1.6-.5-2.7-1.8-3.1z"/>
          </svg>
        </div>
      </div>

      <div class="metric-card card-amber" style="cursor: pointer;" onclick="produccionModule.onCategoryFilterChange('GALONERAS')" title="Filtrar por Galoneras">
        <div class="metric-info">
          <h4>Cant. Galoneras</h4>
          <div id="statProdGalonesCount" class="metric-value">0</div>
        </div>
        <div class="metric-icon-box">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor">
            <rect x="4" y="1.5" width="4.5" height="2.5" rx="0.5"/>
            <path fill-rule="evenodd" clip-rule="evenodd" d="M4 4h5v1.2c.9-.4 2-.7 3.5-.7h5c2 0 3.5 1.5 3.5 3.5v12.5a1.5 1.5 0 0 1-1.5 1.5h-16A1.5 1.5 0 0 1 2 20.5V6C2 4.9 2.9 4 4 4zm7 2.5h6.5c.8 0 1.5.7 1.5 1.5s-.7 1.5-1.5 1.5H11c-.8 0-1.5-.7-1.5-1.5s.7-1.5 1.5-1.5z"/>
          </svg>
        </div>
      </div>

      <div class="metric-card card-purple" style="cursor: pointer;" onclick="produccionModule.onCategoryFilterChange('TAPAS')" title="Filtrar por Tapas">
        <div class="metric-info">
          <h4>Cant. Tapas</h4>
          <div id="statProdTapasCount" class="metric-value">0</div>
        </div>
        <div class="metric-icon-box">
          <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor">
            <path d="M4 6a2 2 0 0 1 2-2h12a2 2 0 0 1 2 2v2H4V6zm0 4h16v6a4 4 0 0 1-4 4H8a4 4 0 0 1-4-4v-6zm2 2v4a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2v-4H6z"/>
          </svg>
        </div>
      </div>
    </div>

    <!-- Buscador y Filtro por Categoría de Productos en Producción -->
    <div class="row g-2 align-items-center mb-3">
      <div class="col-md-5 col-12">
        <div class="input-group input-group-sm">
          <span class="input-group-text"><i class="bi bi-search"></i></span>
          <input type="text" id="searchProduccionInput" class="form-control"
            placeholder="Buscar por código o nombre de producto..." oninput="produccionModule.onSearchInput(this.value)">
        </div>
      </div>
      <div class="col-md-4 col-12">
        <div class="input-group input-group-sm">
          <label class="input-group-text fw-semibold" for="filterProduccionCategorySelect">
            <i class="bi bi-funnel me-1"></i> Categoría:
          </label>
          <select id="filterProduccionCategorySelect" class="form-select" onchange="produccionModule.onCategoryFilterChange(this.value)">
            <option value="ALL" selected>Todas las Categorías</option>
            <option value="FRASCOS">FRASCOS</option>
            <option value="GALONERAS">GALONERAS</option>
            <option value="TAPAS">TAPAS / TAPONES</option>
            <option value="ASAS">ASAS</option>
            <option value="BALDES">BALDES</option>
            <option value="PRODUCTOS COMPLEMENTARIOS">COMPLEMENTARIOS</option>
            <option value="GENERAL">GENERAL / OTROS</option>
          </select>
        </div>
      </div>
      <div class="col-md-3 col-12 text-md-end text-muted small">
        <i class="bi bi-info-circle me-1"></i> Control de demanda activa.
      </div>
    </div>

    <!-- Tabla de Control de Producción -->
    <div class="table-responsive border rounded">
      <table class="table custom-table align-middle mb-0">
        <thead>
          <tr>
            <th style="width: 120px;">Código</th>
            <th>Nombre del Producto</th>
            <th style="width: 110px;">Categoría</th>
            <th class="text-center" style="width: 130px;">Total Solicitado</th>
            <th class="text-center" style="width: 130px;">Total Entregado</th>
            <th class="text-center" style="width: 130px;">Total Pendiente</th>
            <th class="text-center" style="width: 110px;">Clientes</th>
            <th class="text-center" style="width: 80px;">DETALLE</th>
          </tr>
        </thead>
        <tbody id="produccionTableBody"></tbody>
      </table>
      <div id="produccionPaginationContainer"></div>
    </div>
  </div>
</div>
`,

  config: `
<div class="row g-4 mb-4">
  <!-- Document Storage Folder Configuration Card (Full Width) -->
  <div class="col-12">
    <div class="content-card p-4">
      <div class="d-flex align-items-center gap-3 mb-3">
        <div class="p-3 rounded-circle bg-warning bg-opacity-10 text-warning-emphasis">
          <i class="bi bi-folder-symlink-fill fs-3"></i>
        </div>
        <div>
          <h2 class="h5 fw-bold mb-1">Ruta de Almacenamiento de Archivos en el Equipo</h2>
          <p class="text-muted small mb-0">Configura la carpeta local o de red donde se guardarán las Guías de Remisión y Documentos del sistema</p>
        </div>
      </div>
      <hr>
      
      <form onsubmit="event.preventDefault(); configModule.saveStorageConfig();">
        <div class="row g-3">
          <div class="col-md-6">
            <label for="guiasPdfFolderPathInput" class="form-label fw-semibold fs-7">Ruta de Guardado para Guías de Remisión (PDF) *</label>
            <div class="input-group input-group-sm">
              <span class="input-group-text"><i class="bi bi-truck"></i></span>
              <input type="text" id="guiasPdfFolderPathInput" class="form-control font-monospace" placeholder="Ej: C:\\Inplabel\\Guias" required>
            </div>
            <div class="form-text fs-8">Directorio en el disco local de la PC donde se almacenarán las guías emitidas.</div>
          </div>

          <div class="col-md-6">
            <label for="pdfFolderPathInput" class="form-label fw-semibold fs-7">Ruta de Guardado para Pedidos / Cotizaciones (PDF) *</label>
            <div class="input-group input-group-sm">
              <span class="input-group-text"><i class="bi bi-folder2-open"></i></span>
              <input type="text" id="pdfFolderPathInput" class="form-control font-monospace" placeholder="Ej: C:\\Inplabel\\Pedidos" required>
            </div>
            <div class="form-text fs-8">Directorio en el disco local para exportaciones de órdenes y cotizaciones.</div>
          </div>

          <div class="col-12">
            <div class="form-check form-switch mt-2">
              <input class="form-check-input" type="checkbox" id="pdfSubfolderCheckbox">
              <label class="form-check-label fw-semibold fs-7" for="pdfSubfolderCheckbox">
                Organizar automáticamente en subcarpetas por año y mes (ej. <code>/2026/08/</code>)
              </label>
            </div>
          </div>
        </div>

        <div id="storageConfigFeedback" class="mt-3 d-none"></div>

        <div class="d-flex justify-content-end mt-4 pt-2 border-top">
          <button type="submit" class="btn btn-primary btn-sm px-4">
            <i class="bi bi-save me-1"></i> Guardar Rutas de Almacenamiento
          </button>
        </div>
      </form>
    </div>
  </div>

  <!-- System Information & Database Status Card -->
  <div class="col-12">
    <div class="content-card p-4">
      <div class="d-flex align-items-center gap-3 mb-3">
        <div class="p-3 rounded-circle bg-primary bg-opacity-10 text-primary">
          <i class="bi bi-server fs-3"></i>
        </div>
        <div>
          <h2 class="h5 fw-bold mb-1">Estado del Servidor & Base de Datos</h2>
          <p class="text-muted small mb-0">API REST Spring Boot 3.2.5 + MySQL 8.0 (Host Local)</p>
        </div>
      </div>
      <hr>
      <div class="row g-3">
        <div class="col-md-3 col-6">
          <div class="p-3 bg-body-tertiary rounded border">
            <span class="small text-muted d-block font-monospace">ESTADO CONEXIÓN</span>
            <span class="badge bg-success mt-1">MySQL CONECTADO</span>
          </div>
        </div>
        <div class="col-md-3 col-6">
          <div class="p-3 bg-body-tertiary rounded border">
            <span class="small text-muted d-block font-monospace">ENDPOINT API</span>
            <span class="small fw-semibold text-primary">http://localhost:8080/api</span>
          </div>
        </div>
        <div class="col-md-3 col-6">
          <div class="p-3 bg-body-tertiary rounded border">
            <span class="small text-muted d-block font-monospace">CLIENTES REGISTRADOS</span>
            <strong id="bdClientsCount" class="fs-6 text-primary">340 Clientes</strong>
          </div>
        </div>
        <div class="col-md-3 col-6">
          <div class="p-3 bg-body-tertiary rounded border">
            <span class="small text-muted d-block font-monospace">CATÁLOGO PRODUCTOS</span>
            <strong id="bdProductsCount" class="fs-6 text-success">804 Productos</strong>
          </div>
        </div>
      </div>
    </div>
  </div>
</div>
`,

  letras: `
<div class="content-card">
  <!-- Cabecera de la Vista -->
  <div class="card-header flex-wrap gap-2">
    <div class="d-flex align-items-center gap-2">
      <h3 class="card-title mb-0">
        <i class="bi bi-file-earmark-ruled text-primary me-1"></i> Control de Letras de Cambio
      </h3>
      <span id="letrasCountBadge" class="badge bg-secondary">0 operaciones</span>
    </div>
    <div class="d-flex align-items-center gap-2">
      <button class="btn btn-primary" onclick="letrasModule.openGenerarLetrasModal()">
        <i class="bi bi-plus-lg me-1"></i> Generar Letras de Cambio
      </button>
    </div>
  </div>

  <div class="p-3">
    <!-- Indicadores Rápidos / KPI Cards -->
    <div class="metrics-grid mb-4">
      <!-- 1. Monto Total Emitido -->
      <div class="metric-card card-emerald">
        <div class="metric-info">
          <h4>Monto Total Emitido</h4>
          <div id="statLetrasTotalAmount" class="metric-value fs-5">S/ 0.00</div>
        </div>
        <div class="metric-icon-box">
          <i class="bi bi-cash-stack fs-3"></i>
        </div>
      </div>

      <!-- 2. Total Letras del Mes -->
      <div class="metric-card card-amber">
        <div class="metric-info">
          <h4>Total Letras Emitidas</h4>
          <div id="statLetrasTotalLetrasCount" class="metric-value">0</div>
        </div>
        <div class="metric-icon-box">
          <i class="bi bi-file-earmark-text fs-3"></i>
        </div>
      </div>

      <!-- 3. Cliente con Más Letras -->
      <div class="metric-card card-blue">
        <div class="metric-info" style="max-width: 80%;">
          <h4>Cliente con Más Letras</h4>
          <div id="statLetrasTopClientName" class="metric-value fs-6 text-truncate" title="-">-</div>
          <small id="statLetrasTopClientSub" class="text-muted" style="font-size: 0.75rem;">0 letras</small>
        </div>
        <div class="metric-icon-box">
          <i class="bi bi-trophy fs-3"></i>
        </div>
      </div>

      <!-- 4. Clientes Atendidos -->
      <div class="metric-card card-cyan">
        <div class="metric-info">
          <h4>Clientes Atendidos</h4>
          <div id="statLetrasClientsCount" class="metric-value">0</div>
        </div>
        <div class="metric-icon-box">
          <i class="bi bi-people fs-3"></i>
        </div>
      </div>
    </div>

    <!-- Barra de Búsqueda y Filtros -->
    <div class="p-3 bg-body-tertiary border rounded mb-3">
      <form id="formSearchLetras" onsubmit="event.preventDefault(); letrasModule.triggerSearch();">
        <div class="row g-2 align-items-end">
          <!-- Búsqueda General -->
          <div class="col-md-5">
            <label for="searchLetraInput" class="form-label small fw-bold mb-1">Buscar por Ref. Girador, Cliente o RUC</label>
            <div class="input-group input-group-sm">
              <span class="input-group-text"><i class="bi bi-search"></i></span>
              <input type="text" id="searchLetraInput" class="form-control" placeholder="Buscar por referencia, cliente o RUC...">
            </div>
          </div>

          <!-- Fecha Desde -->
          <div class="col-md-3">
            <label for="filterLetraDateFrom" class="form-label small fw-bold mb-1">Desde</label>
            <input type="date" id="filterLetraDateFrom" class="form-control form-control-sm">
          </div>

          <!-- Fecha Hasta -->
          <div class="col-md-3">
            <label for="filterLetraDateTo" class="form-label small fw-bold mb-1">Hasta</label>
            <input type="date" id="filterLetraDateTo" class="form-control form-control-sm">
          </div>

          <!-- Botón Buscar -->
          <div class="col-md-1">
            <button type="submit" class="btn btn-primary btn-sm w-100">
              <i class="bi bi-search me-1"></i> Buscar
            </button>
          </div>
        </div>
      </form>
    </div>

    <!-- Tabla Principal de Letras (Agrupada por Referencia del Girador + Cliente) -->
    <div class="table-responsive border rounded">
      <table class="table custom-table mb-0 align-middle">
        <thead>
          <tr>
            <th style="white-space: nowrap; width: 120px;">ID Operación</th>
            <th style="white-space: nowrap; width: 140px;">Ref. Girador</th>
            <th style="white-space: nowrap;">Girado a (Cliente)</th>
            <th style="white-space: nowrap; width: 115px;">F. Giro</th>
            <th style="white-space: nowrap; width: 130px;" class="text-center">Cant. Letras</th>
            <th style="white-space: nowrap; width: 180px;">Rango N° Letras</th>
            <th style="white-space: nowrap; width: 160px;" class="text-end">Monto Total (S/)</th>
            <th style="white-space: nowrap; width: 85px;" class="text-center">DETALLES</th>
            <th style="white-space: nowrap; width: 85px;" class="text-center">ANULAR</th>
          </tr>
        </thead>
        <tbody id="letrasTableBody"></tbody>
      </table>
      <div id="letrasPaginationContainer"></div>
    </div>
  </div>
</div>

<div class="modal fade" id="modalGenerarLetras" tabindex="-1" aria-hidden="true" data-bs-backdrop="static" data-bs-keyboard="false">
  <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
    <div class="modal-content border-secondary">
      <div class="modal-header bg-primary text-white py-3">
        <h5 class="modal-title fw-bold">
          <i class="bi bi-file-earmark-ruled-fill me-2"></i> Generación de Letras de Cambio (Multi-Letra)
        </h5>
        <button type="button" class="btn-close btn-close-white" onclick="letrasModule.attemptCloseGenerarModal()" aria-label="Close"></button>
      </div>

      <div class="modal-body p-4">
        <form id="formGenerarLetras" onsubmit="event.preventDefault();">
          
          <div class="p-3 border rounded bg-body-tertiary mb-3">
            <h6 class="fw-bold text-success mb-3">
              <i class="bi bi-person-badge-fill me-1"></i> 1. Datos del Girado (Cliente)
            </h6>

            <div class="mb-3 position-relative">
              <label for="searchClienteLetraInput" class="form-label fw-semibold fs-7 mb-1">Buscar Cliente (RUC o Razón Social)</label>
              <div class="input-group input-group-sm">
                <span class="input-group-text"><i class="bi bi-search"></i></span>
                <input type="text" id="searchClienteLetraInput" class="form-control" autocomplete="off" placeholder="Escriba RUC o Razón Social (ej: Chemifabrik, Viskosil)...">
                <button class="btn btn-outline-secondary" type="button" onclick="letrasModule.clearSelectedClient()">
                  <i class="bi bi-x-lg"></i>
                </button>
              </div>
              <ul id="clientSearchResultsLetraList" class="list-group position-absolute w-100 shadow-lg d-none" style="z-index: 1060; max-height: 280px; overflow-y: auto; top: 100%;"></ul>
            </div>

            <div class="d-flex flex-wrap gap-2 mb-2" style="width: 100%;">
              <div style="flex: 1 1 75%; min-width: 250px;">
                <label class="form-label fw-semibold fs-7 mb-1">Razón Social *</label>
                <input type="text" id="letraClienteNombre" class="form-control form-control-sm fw-bold" readonly required>
              </div>
              <div style="flex: 0 0 22%; min-width: 130px;">
                <label class="form-label fw-semibold fs-7 mb-1">RUC *</label>
                <input type="text" id="letraClienteDoc" class="form-control form-control-sm font-monospace text-center fw-bold" readonly required>
              </div>
            </div>

            <div class="row g-2">
              <div class="col-12">
                <label class="form-label fw-semibold fs-7 mb-1">Dirección / Domicilio Fiscal *</label>
                <input type="text" id="letraClienteDireccion" class="form-control form-control-sm" required>
              </div>
            </div>
          </div>

          <div class="p-3 border rounded bg-body-tertiary mb-3">
            <h6 class="fw-bold text-success mb-3">
              <i class="bi bi-sliders me-1"></i> 2. Parámetros de Emisión & Desglose
            </h6>
            <div class="row g-3 align-items-end">
              <div class="col-md-3">
                <label for="letraRefGirador" class="form-label fw-semibold fs-7">Ref. del Girador (Factura/Orden) *</label>
                <input type="text" id="letraRefGirador" class="form-control form-control-sm text-uppercase font-monospace" required>
              </div>

              <div class="col-md-2">
                <label for="letraLugarGiro" class="form-label fw-semibold fs-7">Lugar de Giro</label>
                <input type="text" id="letraLugarGiro" class="form-control form-control-sm" value="LIMA" required>
              </div>

              <div class="col-md-2">
                <label for="letraFechaGiro" class="form-label fw-semibold fs-7">Fecha de Giro *</label>
                <input type="date" id="letraFechaGiro" class="form-control form-control-sm" onchange="letrasModule.recalcInstallments()" required>
              </div>

              <div class="col-md-5">
                <label class="form-label fw-semibold fs-7">N° de Letra Inicial (Correlativo - Año) *</label>
                <div class="input-group input-group-sm">
                  <input type="number" id="letraCorrelativoInput" class="form-control font-monospace text-center fw-bold" min="1" oninput="letrasModule.recalcInstallments()" required>
                  <span class="input-group-text fw-bold">-</span>
                  <input type="number" id="letraAnioInput" class="form-control font-monospace text-center fw-bold" min="2020" max="2099" oninput="letrasModule.recalcInstallments()" required>
                </div>
              </div>

              <div class="col-md-4">
                <label for="letraMontoTotal" class="form-label fw-semibold fs-7">Monto Total de la Operación (S/) *</label>
                <div class="input-group input-group-sm">
                  <span class="input-group-text fw-bold">S/</span>
                  <input type="number" id="letraMontoTotal" class="form-control font-monospace fw-bold text-end" step="0.01" min="0.01" oninput="letrasModule.recalcInstallments()" onkeydown="if(event.key==='Enter'){event.preventDefault();document.getElementById('letraCantidadCuotas').focus();}" required>
                </div>
              </div>

              <div class="col-md-3">
                <label for="letraCantidadCuotas" class="form-label fw-semibold fs-7">Cantidad de Letras *</label>
                <input type="number" id="letraCantidadCuotas" class="form-control form-control-sm text-center fw-bold" value="1" min="1" max="36" oninput="letrasModule.recalcInstallments()" onkeydown="if(event.key==='Enter'){event.preventDefault();letrasModule.recalcInstallments();}" required>
              </div>

              <div class="col-md-5">
                <button type="button" class="btn btn-outline-primary btn-sm w-100" onclick="letrasModule.recalcInstallments()">
                  <i class="bi bi-arrow-clockwise me-1"></i> Desglosar / Actualizar Cuotas
                </button>
              </div>
            </div>
          </div>

          <div class="p-3 border rounded bg-body-tertiary mb-3 shadow-sm">
            <div class="d-flex justify-content-between align-items-center mb-3">
              <h6 class="fw-bold text-white mb-0" style="color: #60a5fa !important; font-size: 1rem;">
                <i class="bi bi-table me-2 text-primary"></i> 3. Letras Generadas y Distribución de Montos
              </h6>
              <div id="letrasSumValidationBadge" class="badge bg-success fs-7">
                Suma: S/ 0.00
              </div>
            </div>

            <div class="table-responsive">
              <table class="table custom-table table-sm align-middle mb-0">
                <thead>
                  <tr>
                    <th style="width: 35px;" class="text-center">#</th>
                    <th style="width: 115px;">N° Letra</th>
                    <th style="width: 95px;" class="text-center">Días Crédito</th>
                    <th style="width: 135px;">F. Vencimiento</th>
                    <th style="width: 165px;" class="text-end">Monto (S/)</th>
                    <th>Monto en Letras (Automático)</th>
                  </tr>
                </thead>
                <tbody id="installmentRowsTbody"></tbody>
              </table>
            </div>
            <div class="form-text fs-8 mt-2 text-muted">
              <i class="bi bi-info-circle me-1"></i> Puedes escribir directamente los días de crédito de cada letra o editar fechas y montos individualmente. Si la división no es exacta, el centavo mayor queda al final.
            </div>
          </div>

        </form>
      </div>

      <div class="modal-footer bg-body-tertiary">
        <button type="button" class="btn btn-secondary btn-sm" onclick="letrasModule.attemptCloseGenerarModal()">Cancelar</button>
        <button type="button" class="btn btn-primary btn-sm px-4" onclick="letrasModule.submitBatchLetras()">
          <i class="bi bi-check-circle me-1"></i> Emitir y Guardar Letras de Cambio
        </button>
      </div>
    </div>
  </div>
</div>

<div class="modal fade" id="modalConfirmDiscardLetra" tabindex="-1" aria-hidden="true" style="z-index: 1080;">
  <div class="modal-dialog modal-dialog-centered" style="max-width: 420px;">
    <div class="modal-content border-warning shadow-lg">
      <div class="modal-header bg-warning text-dark py-2.5">
        <h6 class="modal-title fw-bold mb-0">
          <i class="bi bi-exclamation-triangle-fill me-2"></i> ¿Descartar datos de la letra?
        </h6>
        <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body p-3 text-center">
        <p class="mb-0 fs-7">Has ingresado datos para la emisión de letras. Si sales ahora, se perderán los cambios.</p>
      </div>
      <div class="modal-footer bg-body-tertiary py-2 justify-content-center">
        <button type="button" class="btn btn-outline-secondary btn-sm px-3" data-bs-dismiss="modal">Seguir Editando</button>
        <button type="button" class="btn btn-danger btn-sm px-3" onclick="letrasModule.forceCloseGenerarModal()">Descartar y Salir</button>
      </div>
    </div>
  </div>
</div>

<div class="modal fade" id="letraDetailModal" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-xl modal-dialog-centered modal-dialog-scrollable">
    <div class="modal-content border-secondary shadow-lg">
      <div class="modal-header bg-primary text-white py-3">
        <h5 class="modal-title fw-bold" id="letraDetailTitle">
          <i class="bi bi-collection me-2"></i> Detalle de Letras de Cambio por Referencia
        </h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body p-4" id="letraDetailBody"></div>
      <div class="modal-footer bg-body-tertiary">
        <button type="button" class="btn btn-secondary btn-sm px-4" data-bs-dismiss="modal">Cerrar</button>
      </div>
    </div>
  </div>
</div>

<div class="modal fade" id="modalAnularLetra" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered">
    <div class="modal-content border-danger">
      <div class="modal-header bg-danger text-white py-3">
        <h5 class="modal-title fw-bold">
          <i class="bi bi-exclamation-triangle-fill me-2"></i> Anular Operación de Letras
        </h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <div class="modal-body p-4">
        <div class="alert alert-warning d-flex align-items-center mb-3 fs-7" role="alert">
          <i class="bi bi-exclamation-circle-fill me-2 fs-5"></i>
          <div>Esta acción marcará como <strong>ANULADAS</strong> todas las letras de cambio asociadas a esta operación.</div>
        </div>
        <input type="hidden" id="anularLoteIdInput">
        <div class="mb-3">
          <label class="form-label fs-7 fw-semibold">Ref. del Girador:</label>
          <div class="form-control-plaintext font-monospace fw-bold fs-6 text-primary" id="anularNroLetraLabel">-</div>
        </div>
        <div class="mb-3">
          <label class="form-label fs-7 fw-semibold">Cliente:</label>
          <div class="form-control-plaintext fw-bold" id="anularLetraClienteLabel">-</div>
        </div>
      </div>
      <div class="modal-footer bg-body-tertiary">
        <button type="button" class="btn btn-secondary btn-sm" data-bs-dismiss="modal">Cancelar</button>
        <button type="button" class="btn btn-danger btn-sm px-3" onclick="letrasModule.confirmAnularLote()">
          <i class="bi bi-check-circle me-1"></i> Confirmar Anulación
        </button>
      </div>
    </div>
  </div>
</div>
`,

  bd: `
<div class="content-card p-4 mb-4">
  <div class="d-flex align-items-center justify-content-between mb-3">
    <div class="d-flex align-items-center gap-3">
      <i class="bi bi-database-check text-primary fs-1"></i>
      <div>
        <h2 class="h4 fw-bold mb-1">Backend Spring Boot 3.2.5: Base de Datos 'inplabel'</h2>
        <p class="text-muted small mb-0">API REST disponible en http://localhost:8080/api</p>
      </div>
    </div>
    <span class="badge bg-success py-2 px-3">CONECTADO A MYSQL 8.0</span>
  </div>

  <div class="row g-3">
    <div class="col-md-4">
      <div class="p-3 bg-light rounded border">
        <span class="small text-muted d-block font-monospace">MOTOR BACKEND</span>
        <strong class="fs-6">Spring Boot + Spring Web + HikariCP</strong>
      </div>
    </div>
    <div class="col-md-4">
      <div class="p-3 bg-light rounded border">
        <span class="small text-muted d-block font-monospace">CLIENTES MYSQL</span>
        <strong id="bdClientsCount" class="fs-6 text-primary">341 Clientes</strong>
      </div>
    </div>
    <div class="col-md-4">
      <div class="p-3 bg-light rounded border">
        <span class="small text-muted d-block font-monospace">PRODUCTOS MYSQL</span>
        <strong id="bdProductsCount" class="fs-6 text-success">706 Productos</strong>
      </div>
    </div>
  </div>
</div>
`,
  login: `
<div class="modern-login-page d-flex align-items-center justify-content-center min-vh-100 p-3 p-md-4" style="background-color: #f8fafc; background-image: radial-gradient(#e2e8f0 1.2px, transparent 1.2px); background-size: 24px 24px; font-family: 'Manrope', 'Sora', -apple-system, sans-serif;">
  
  <style>
    /* Estilos estrictos para inputs en modo claro, ignorando el tema oscuro global */
    .modern-login-page .form-control,
    .modern-login-page .modern-login-input {
      background-color: #f8fafc !important;
      color: #0f172a !important;
      border: 1.5px solid #cbd5e1 !important;
      font-weight: 600 !important;
      font-size: 0.95rem !important;
    }
    .modern-login-page .form-control::placeholder,
    .modern-login-page .modern-login-input::placeholder {
      color: #94a3b8 !important;
      font-weight: 400 !important;
    }
    .modern-login-page .form-control:focus,
    .modern-login-page .modern-login-input:focus {
      background-color: #ffffff !important;
      color: #0f172a !important;
      border-color: #00AF50 !important;
      box-shadow: 0 0 0 4px rgba(0, 175, 80, 0.15) !important;
      outline: none !important;
    }
    .modern-login-page .input-group-text {
      background-color: #f8fafc !important;
      color: #64748b !important;
      border: 1.5px solid #cbd5e1 !important;
      border-right: none !important;
    }
    .modern-login-page .btn-toggle-pass {
      background-color: #f8fafc !important;
      color: #64748b !important;
      border: 1.5px solid #cbd5e1 !important;
      border-left: none !important;
    }
    .modern-login-page .btn-toggle-pass:hover {
      background-color: #f1f5f9 !important;
      color: #0f172a !important;
    }

    /* Neutralizar el fondo y texto del autocompletado del navegador */
    .modern-login-page input:-webkit-autofill,
    .modern-login-page input:-webkit-autofill:hover, 
    .modern-login-page input:-webkit-autofill:focus {
      -webkit-box-shadow: 0 0 0px 1000px #f8fafc inset !important;
      -webkit-text-fill-color: #0f172a !important;
      transition: background-color 5000s ease-in-out 0s;
    }

    .btn-login-cta {
      background: linear-gradient(135deg, #00AF50 0%, #009444 100%);
      color: #ffffff !important;
      border: none;
      font-weight: 700;
      letter-spacing: 0.2px;
      transition: all 0.25s ease;
    }
    .btn-login-cta:hover {
      background: linear-gradient(135deg, #009e47 0%, #00803a 100%);
      transform: translateY(-1px);
      box-shadow: 0 10px 20px -5px rgba(0, 175, 80, 0.35) !important;
    }
    .feature-item {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      padding: 0.65rem 0.85rem;
      background: rgba(255, 255, 255, 0.05);
      border: 1px solid rgba(255, 255, 255, 0.08);
      border-radius: 12px;
      font-size: 0.82rem;
      color: #e2e8f0;
      backdrop-filter: blur(8px);
    }
  </style>

  <!-- Card Principal con Diseño Split Moderno -->
  <div class="card border-0 rounded-4 overflow-hidden shadow-lg" style="width: 100%; max-width: 900px; background: #ffffff; box-shadow: 0 25px 50px -12px rgba(15, 23, 42, 0.12) !important;">
    <div class="row g-0">
      
      <!-- Columna Izquierda: Banner Corporativo Inplabel -->
      <div class="col-lg-5 d-none d-lg-flex flex-column justify-content-between p-4 p-xl-5 text-white" style="background: linear-gradient(145deg, #0f172a 0%, #1e293b 100%); position: relative; overflow: hidden;">
        
        <!-- Elemento de fondo decorativo -->
        <div style="position: absolute; top: -60px; right: -60px; width: 200px; height: 200px; background: radial-gradient(circle, rgba(0, 175, 80, 0.25) 0%, transparent 70%); border-radius: 50%; pointer-events: none;"></div>
        <div style="position: absolute; bottom: -80px; left: -80px; width: 240px; height: 240px; background: radial-gradient(circle, rgba(0, 175, 80, 0.15) 0%, transparent 70%); border-radius: 50%; pointer-events: none;"></div>

        <!-- Top: Título y Presentación del Sistema -->
        <div style="position: relative; z-index: 2;">
          <div class="d-inline-flex align-items-center gap-2 px-3 py-1 rounded-pill mb-3" style="background: rgba(0, 175, 80, 0.15); border: 1px solid rgba(0, 175, 80, 0.3); color: #4ade80; font-size: 0.8rem; font-weight: 700;">
            <i class="bi bi-layers-fill"></i> Sistema Empresarial
          </div>
          <h4 class="fw-bold text-white mb-2" style="font-size: 1.45rem; letter-spacing: -0.3px; line-height: 1.25;">Plataforma de Gestión</h4>
          <p class="text-white-50 small mb-0" style="font-size: 0.88rem; line-height: 1.4;">Sistema Operativo Central de Ventas y Planta Industrial</p>
        </div>

        <!-- Middle: Módulos Activos -->
        <div class="my-4 d-flex flex-column gap-2" style="position: relative; z-index: 2;">
          <div class="feature-item">
            <i class="bi bi-cart-check-fill text-success fs-6"></i>
            <span>Control Integral de Pedidos & Órdenes</span>
          </div>
          <div class="feature-item">
            <i class="bi bi-truck text-info fs-6"></i>
            <span>Guías de Remisión & Despachos</span>
          </div>
          <div class="feature-item">
            <i class="bi bi-file-earmark-ruled-fill text-warning fs-6"></i>
            <span>Letras de Cambio & Líneas de Crédito</span>
          </div>
          <div class="feature-item">
            <i class="bi bi-gear-wide-connected text-primary fs-6"></i>
            <span>Monitoreo de Producción en Planta</span>
          </div>
        </div>

        <!-- Bottom: Seguridad -->
        <div class="pt-3 border-top border-white border-opacity-10 d-flex align-items-center justify-content-between text-white-50 small" style="position: relative; z-index: 2; font-size: 0.76rem;">
          <span><i class="bi bi-shield-lock-fill text-success me-1"></i> Conexión Segura</span>
          <span>MySQL & Spring Boot</span>
        </div>

      </div>

      <!-- Columna Derecha: Formulario de Inicio de Sesión -->
      <div class="col-lg-7 p-4 p-md-5 d-flex flex-column justify-content-center" style="background: #ffffff;">
        
        <!-- Logo de Inplabel en la cabecera del formulario -->
        <div class="text-center text-lg-start mb-4">
          <img src="img/inplabel-logo.png" alt="Inplabel - Industrias plasticos belsa S.A.C" style="max-height: 82px; width: auto; object-fit: contain;" class="mb-3">
          <h3 class="fw-bold mb-1" style="color: #0f172a; font-size: 1.6rem; letter-spacing: -0.4px;">Bienvenido</h3>
          <p class="small mb-0" style="color: #64748b; font-size: 0.88rem;">Ingresa tus credenciales para acceder al sistema.</p>
        </div>

        <!-- Alert Container -->
        <div id="loginAlert" class="alert alert-danger py-2.5 px-3 small d-none mb-3 border-0 rounded-3 d-flex align-items-center gap-2"></div>

        <!-- Formulario -->
        <form id="formLogin" onsubmit="authModule.submitLogin(event)" autocomplete="off">
          
          <!-- Input oculto trampa para que los navegadores no autocompleten -->
          <input type="text" style="display:none" autocomplete="off">
          <input type="password" style="display:none" autocomplete="off">

          <!-- Campo Usuario -->
          <div class="mb-3">
            <label for="loginUsername" class="form-label small fw-bold mb-1.5" style="color: #334155;">Usuario</label>
            <div class="input-group">
              <span class="input-group-text" style="border-radius: 10px 0 0 10px;">
                <i class="bi bi-person-fill fs-6"></i>
              </span>
              <input type="text" id="loginUsername" name="inplabel_user_input" class="form-control modern-login-input py-2.5" placeholder="Escribe tu usuario..." autocomplete="new-password" required autofocus style="border-radius: 0 10px 10px 0;">
            </div>
          </div>

          <!-- Campo Contraseña -->
          <div class="mb-4">
            <div class="d-flex align-items-center justify-content-between mb-1.5">
              <label for="loginPassword" class="form-label small fw-bold mb-0" style="color: #334155;">Contraseña</label>
            </div>
            <div class="input-group">
              <span class="input-group-text" style="border-radius: 10px 0 0 10px;">
                <i class="bi bi-lock-fill fs-6"></i>
              </span>
              <input type="password" id="loginPassword" name="inplabel_pass_input" class="form-control modern-login-input py-2.5" placeholder="Escribe tu contraseña..." autocomplete="new-password" required style="border-radius: 0;">
              <button type="button" class="btn btn-toggle-pass" onclick="authModule.togglePasswordVisibility()" title="Mostrar/Ocultar contraseña" style="border-radius: 0 10px 10px 0;">
                <i id="togglePassIcon" class="bi bi-eye-fill"></i>
              </button>
            </div>
          </div>

          <!-- Botón de Ingreso -->
          <button type="submit" id="btnLoginSubmit" class="btn btn-login-cta w-100 py-2.5 rounded-3 shadow-sm d-flex align-items-center justify-content-center fs-6">
            <i class="bi bi-box-arrow-in-right me-2 fs-5"></i> Iniciar Sesión
          </button>
        </form>

        <!-- Footer -->
        <div class="mt-4 pt-3 border-top text-center" style="border-color: #f1f5f9 !important;">
          <small class="d-block" style="font-size: 0.76rem; color: #94a3b8;">
            © 2026 Inplabel S.A.C. • Todos los derechos reservados
          </small>
        </div>

      </div>

    </div>
  </div>
</div>
`,
usuarios: `
<div class="content-card">
  <div class="card-header d-flex justify-content-between align-items-center">
    <h3 class="card-title mb-0"><i class="bi bi-shield-lock-fill text-primary me-1"></i> Administración de Usuarios y Matriz de Permisos</h3>
    <button class="btn btn-primary btn-sm px-3 fw-bold" onclick="usuariosModule.openNewUserModal()">
      <i class="bi bi-person-plus-fill me-1"></i> Nuevo Usuario
    </button>
  </div>

  <div class="p-3 bg-body-tertiary border-bottom">
    <div class="input-group">
      <span class="input-group-text"><i class="bi bi-search"></i></span>
      <input type="text" id="searchUsuariosInput" class="form-control"
        placeholder="Buscar usuario por nombre, username o rol..."
        oninput="usuariosModule.filterUsuarios(this.value)">
    </div>
  </div>

  <div class="table-responsive">
    <table class="table custom-table mb-0 align-middle">
      <thead>
        <tr>
          <th>Usuario (Username)</th>
          <th>Nombre Completo</th>
          <th>Rol Principal</th>
          <th>Permisos Concedidos</th>
          <th class="text-center">Estado</th>
          <th style="width: 140px;" class="text-center">Acciones</th>
        </tr>
      </thead>
      <tbody id="usuariosTableBody"></tbody>
    </table>
  </div>
</div>

<!-- Modal Crear / Editar Usuario con Matriz Granular de Permisos -->
<div class="modal fade" id="modalUsuario" tabindex="-1" aria-hidden="true">
  <div class="modal-dialog modal-dialog-centered modal-lg">
    <div class="modal-content shadow">
      <div class="modal-header bg-primary text-white py-2.5">
        <h5 class="modal-title fs-6 fw-bold" id="modalUsuarioTitle">
          <i class="bi bi-person-gear me-1"></i> Registrar Nuevo Usuario
        </h5>
        <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
      </div>
      <form id="formUsuario" onsubmit="event.preventDefault(); usuariosModule.saveUsuario();">
        <div class="modal-body p-3">
          <input type="hidden" id="usuarioIdInput" value="">
          
          <div class="row g-3 mb-3">
            <div class="col-md-6">
              <label class="form-label small fw-bold mb-1">Nombre de Usuario (Login) *</label>
              <input type="text" id="usuarioUsernameInput" class="form-control form-control-sm" placeholder="Ej: jsmith, vendedor1" required>
            </div>
            <div class="col-md-6">
              <label class="form-label small fw-bold mb-1">Contraseña <span id="usuarioPasswordHint" class="text-muted fw-normal small">(Requerida para nuevos)</span> *</label>
              <input type="password" id="usuarioPasswordInput" class="form-control form-control-sm" placeholder="••••••••">
            </div>
            <div class="col-md-6">
              <label class="form-label small fw-bold mb-1">Nombre Completo *</label>
              <input type="text" id="usuarioNombreInput" class="form-control form-control-sm" placeholder="Ej: Juan Pérez Morales" required>
            </div>
            <div class="col-md-6">
              <label class="form-label small fw-bold mb-1">Rol de Referencia *</label>
              <select id="usuarioRolSelect" class="form-select form-select-sm" required onchange="usuariosModule.onRolPresetChange(this.value)">
                <option value="OPERADOR">OPERADOR (Personalizado)</option>
                <option value="ADMIN">ADMINISTRADOR (Acceso Total)</option>
                <option value="VENTAS">VENTAS (Pedidos y Clientes)</option>
                <option value="PRODUCCION">PRODUCCIÓN (Control y Productos)</option>
                <option value="ALMACEN">ALMACÉN / DESPACHO (Envíos y Guías)</option>
              </select>
            </div>
          </div>

          <div class="border rounded p-3 bg-body-tertiary mb-2">
            <div class="d-flex align-items-center justify-content-between mb-2">
              <h6 class="fw-bold mb-0 text-primary fs-7">
                <i class="bi bi-ui-checks me-1"></i> Matriz Granular de Permisos (Checkboxes)
              </h6>
              <div class="d-flex gap-2">
                <button type="button" class="btn btn-link btn-sm p-0 text-decoration-none fs-8" onclick="usuariosModule.selectAllPerms(true)">Marcar Todos</button>
                <span class="text-muted">|</span>
                <button type="button" class="btn btn-link btn-sm p-0 text-decoration-none fs-8 text-muted" onclick="usuariosModule.selectAllPerms(false)">Desmarcar Todos</button>
              </div>
            </div>
            <p class="small text-muted mb-3 fs-8">Seleccione las acciones exactas que este usuario tiene permitido realizar en el sistema:</p>

            <div class="row g-3">
              <!-- MÓDULO PEDIDOS -->
              <div class="col-md-6">
                <div class="card border-0 shadow-xs h-100">
                  <div class="card-header py-1.5 px-3 bg-primary-subtle fw-bold fs-8 text-primary">
                    <i class="bi bi-cart-fill me-1"></i> Módulo de Pedidos
                  </div>
                  <div class="card-body p-2 d-flex flex-column gap-1.5">
                    <div class="form-check form-switch fs-8">
                      <input class="form-check-input perm-cb" type="checkbox" id="perm_pedidos_view" value="pedidos.view" checked>
                      <label class="form-check-label" for="perm_pedidos_view">Ver Lista y Ficha de Pedidos</label>
                    </div>
                    <div class="form-check form-switch fs-8">
                      <input class="form-check-input perm-cb" type="checkbox" id="perm_pedidos_create" value="pedidos.create">
                      <label class="form-check-label" for="perm_pedidos_create">Crear Nuevos Pedidos</label>
                    </div>
                    <div class="form-check form-switch fs-8">
                      <input class="form-check-input perm-cb" type="checkbox" id="perm_pedidos_edit" value="pedidos.edit">
                      <label class="form-check-label" for="perm_pedidos_edit">Editar Pedidos Existentes</label>
                    </div>
                    <div class="form-check form-switch fs-8">
                      <input class="form-check-input perm-cb" type="checkbox" id="perm_pedidos_cancel" value="pedidos.cancel">
                      <label class="form-check-label" for="perm_pedidos_cancel">Anular / Cancelar Pedidos</label>
                    </div>
                    <div class="form-check form-switch fs-8">
                      <input class="form-check-input perm-cb" type="checkbox" id="perm_pedidos_finish" value="pedidos.finish">
                      <label class="form-check-label" for="perm_pedidos_finish">Finalizar Orden / Cambiar Estado</label>
                    </div>
                    <div class="form-check form-switch fs-8">
                      <input class="form-check-input perm-cb" type="checkbox" id="perm_pedidos_finances" value="pedidos.finances">
                      <label class="form-check-label text-success fw-semibold" for="perm_pedidos_finances">Ver Adelantos y Dinero (Soles)</label>
                    </div>
                  </div>
                </div>
              </div>

              <!-- MÓDULO ENVÍOS Y DESPACHOS -->
              <div class="col-md-6">
                <div class="card border-0 shadow-xs h-100">
                  <div class="card-header py-1.5 px-3 bg-warning-subtle fw-bold fs-8 text-warning-emphasis">
                    <i class="bi bi-truck me-1"></i> Despachos y Envíos
                  </div>
                  <div class="card-body p-2 d-flex flex-column gap-1.5">
                    <div class="form-check form-switch fs-8">
                      <input class="form-check-input perm-cb" type="checkbox" id="perm_envios_create" value="envios.create">
                      <label class="form-check-label" for="perm_envios_create">Registrar Despacho / Envío Físico</label>
                    </div>
                    <div class="form-check form-switch fs-8">
                      <input class="form-check-input perm-cb" type="checkbox" id="perm_envios_view" value="envios.view">
                      <label class="form-check-label" for="perm_envios_view">Ver Historial de Despachos</label>
                    </div>
                  </div>
                </div>
              </div>

              <!-- MÓDULO GUÍAS DE REMISIÓN -->
              <div class="col-md-6">
                <div class="card border-0 shadow-xs h-100">
                  <div class="card-header py-1.5 px-3 bg-info-subtle fw-bold fs-8 text-info-emphasis">
                    <i class="bi bi-file-earmark-text me-1"></i> Guías de Remisión
                  </div>
                  <div class="card-body p-2 d-flex flex-column gap-1.5">
                    <div class="form-check form-switch fs-8">
                      <input class="form-check-input perm-cb" type="checkbox" id="perm_guias_create" value="guias.create">
                      <label class="form-check-label" for="perm_guias_create">Emitir Guías Oficiales (GR001 / GR002)</label>
                    </div>
                    <div class="form-check form-switch fs-8">
                      <input class="form-check-input perm-cb" type="checkbox" id="perm_guias_view" value="guias.view">
                      <label class="form-check-label" for="perm_guias_view">Ver e Imprimir PDF de Guías</label>
                    </div>
                  </div>
                </div>
              </div>

              <!-- MÓDULO PRODUCCIÓN -->
              <div class="col-md-6">
                <div class="card border-0 shadow-xs h-100">
                  <div class="card-header py-1.5 px-3 bg-success-subtle fw-bold fs-8 text-success">
                    <i class="bi bi-gear-wide-connected me-1"></i> Control de Producción
                  </div>
                  <div class="card-body p-2 d-flex flex-column gap-1.5">
                    <div class="form-check form-switch fs-8">
                      <input class="form-check-input perm-cb" type="checkbox" id="perm_produccion_view" value="produccion.view">
                      <label class="form-check-label" for="perm_produccion_view">Ver Órdenes de Producción Pendientes</label>
                    </div>
                  </div>
                </div>
              </div>

              <!-- CLIENTES & PRODUCTOS -->
              <div class="col-md-6">
                <div class="card border-0 shadow-xs h-100">
                  <div class="card-header py-1.5 px-3 bg-secondary-subtle fw-bold fs-8 text-secondary">
                    <i class="bi bi-boxes me-1"></i> Clientes y Productos
                  </div>
                  <div class="card-body p-2 d-flex flex-column gap-1.5">
                    <div class="form-check form-switch fs-8">
                      <input class="form-check-input perm-cb" type="checkbox" id="perm_clientes_manage" value="clientes.manage">
                      <label class="form-check-label" for="perm_clientes_manage">Crear / Editar Clientes</label>
                    </div>
                    <div class="form-check form-switch fs-8">
                      <input class="form-check-input perm-cb" type="checkbox" id="perm_productos_manage" value="productos.manage">
                      <label class="form-check-label" for="perm_productos_manage">Crear / Editar Productos</label>
                    </div>
                  </div>
                </div>
              </div>

              <!-- GESTIÓN DE USUARIOS -->
              <div class="col-md-6">
                <div class="card border-0 shadow-xs h-100">
                  <div class="card-header py-1.5 px-3 bg-danger-subtle fw-bold fs-8 text-danger">
                    <i class="bi bi-shield-lock me-1"></i> Administración del Sistema
                  </div>
                  <div class="card-body p-2 d-flex flex-column gap-1.5">
                    <div class="form-check form-switch fs-8">
                      <input class="form-check-input perm-cb" type="checkbox" id="perm_usuarios_manage" value="usuarios.manage">
                      <label class="form-check-label text-danger fw-bold" for="perm_usuarios_manage">Administrar Usuarios y Permisos</label>
                    </div>
                  </div>
                </div>
              </div>

            </div>
          </div>
        </div>
        <div class="modal-footer bg-light py-2">
          <button type="button" class="btn btn-outline-secondary btn-sm" data-bs-dismiss="modal">Cancelar</button>
          <button type="submit" class="btn btn-primary btn-sm px-4 fw-bold shadow-sm">
            <i class="bi bi-check-circle-fill me-1"></i> Guardar Usuario
          </button>
        </div>
      </form>
    </div>
  </div>
</div>
`
};

export class Router {
  constructor(onRouteRender) {
    this.currentRoute = 'dashboard';
    this.onRouteRender = onRouteRender;
  }

  async navigateTo(route) {
    // Remove any leftover Bootstrap modal backdrops and restore body state
    document.querySelectorAll('.modal-backdrop').forEach(el => el.remove());
    document.body.classList.remove('modal-open');
    document.body.style.removeProperty('overflow');
    document.body.style.removeProperty('padding-right');
    document.body.style.removeProperty('pointer-events');

    // Verificar si el usuario está autenticado
    const isAuth = window.authModule && typeof window.authModule.isAuthenticated === 'function' 
      ? window.authModule.isAuthenticated() 
      : Boolean(localStorage.getItem('inplabel_user'));

    if (!isAuth && route !== 'login') {
      route = 'login';
    }

    this.currentRoute = route;
    window.location.hash = `#${route}`;

    const sidebarEl = document.getElementById('sidebar');
    const headerEl = document.querySelector('.top-header');
    const mainEl = document.querySelector('.main-content');

    if (route === 'login') {
      if (sidebarEl) sidebarEl.style.display = 'none';
      if (headerEl) headerEl.style.display = 'none';
      if (mainEl) {
        mainEl.style.marginLeft = '0';
        mainEl.style.padding = '0';
        mainEl.style.maxWidth = '100%';
      }
    } else {
      if (sidebarEl) sidebarEl.style.display = '';
      if (headerEl) headerEl.style.display = '';
      if (mainEl) {
        mainEl.style.marginLeft = '';
        mainEl.style.padding = '';
        mainEl.style.maxWidth = '';
      }
      if (window.app && typeof window.app.updateUserUI === 'function') {
        window.app.updateUserUI();
      }
    }

    // Update active nav-item highlight
    document.querySelectorAll('.nav-item').forEach(btn => {
      if (btn.getAttribute('data-route') === route) {
        btn.classList.add('active');
      } else {
        btn.classList.remove('active');
      }
    });

    // Update header title
    const titles = {
      dashboard: '<i class="bi bi-speedometer2 text-primary"></i> Panel General de Control',
      pedidos: '<i class="bi bi-cart-check text-primary"></i> Pedidos',
      'nuevo-pedido': '<i class="bi bi-cart-plus text-primary"></i> Registrar Nuevo Pedido',
      envios: '<i class="bi bi-truck text-primary"></i> Listado de Guías de Remisión',
      'nueva-guia': '<i class="bi bi-file-earmark-plus text-primary"></i> Generar Nueva Guía de Remisión',
      letras: '<i class="bi bi-file-earmark-ruled text-primary"></i> Control de Letras de Cambio',
      clientes: '<i class="bi bi-people text-primary"></i> Clientes',
      productos: '<i class="bi bi-box-seam text-primary"></i> Productos',
      produccion: '<i class="bi bi-gear-wide-connected text-primary"></i> Control de Producción',
      usuarios: '<i class="bi bi-shield-lock-fill text-primary"></i> Administración de Usuarios y Matriz de Permisos',
      config: '<i class="bi bi-gear-fill text-primary"></i> Configuración del Sistema',
      bd: '<i class="bi bi-database-check text-primary"></i> Estado Base de Datos',
      login: '<i class="bi bi-box-arrow-in-right text-primary"></i> Iniciar Sesión'
    };

    const titleElem = document.getElementById('pageTitle');
    if (titleElem) titleElem.innerHTML = titles[route] || 'INPLABEL Pedidos';

    const container = document.getElementById('viewContainer');
    if (!container) return;

    // Use synchronous embedded views for 100% reliability with zero fetch delay or CORS errors
    if (EMBEDDED_VIEWS[route]) {
      container.innerHTML = EMBEDDED_VIEWS[route];
    } else {
      try {
        const res = await fetch(`views/${route}.html`);
        if (res.ok) {
          container.innerHTML = await res.text();
        }
      } catch (e) {
        console.warn("Fetch route error:", e);
      }
    }

    if (typeof this.onRouteRender === 'function') {
      try {
        this.onRouteRender(route);
      } catch (err) {
        console.error(`Error rendering route ${route}:`, err);
      }
    }
  }
}
