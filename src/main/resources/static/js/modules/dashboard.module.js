import { escapeHtml, formatDate } from '../helpers.js';

export function renderDashboard(orders = [], shipments = [], clients = [], products = [], statusData = null) {
  const pendingOrders = orders.filter(o => {
    const st = (o.estado || 'PENDIENTE').toUpperCase();
    return st === 'PENDIENTE' || st === 'EN_PROCESO' || st === 'PROCESO' || st === 'PARCIAL' || st === 'FUERA_DE_TIEMPO';
  });
  const completedOrders = orders.filter(o => {
    const st = (o.estado || '').toUpperCase();
    return st === 'ENTREGADO' || st === 'COMPLETADO' || st === 'FINALIZADO';
  });

  let totalProductsCount = (products && Array.isArray(products) && products.length > 0) ? products.length : 0;
  if (!totalProductsCount && statusData && statusData.counts && statusData.counts.productos) {
    totalProductsCount = statusData.counts.productos;
  }
  if (!totalProductsCount && window.app?.statusData?.counts?.productos) {
    totalProductsCount = window.app.statusData.counts.productos;
  }

  let totalClientsCount = (clients && Array.isArray(clients) && clients.length > 0) ? clients.length : 0;
  if (!totalClientsCount && statusData && statusData.counts && statusData.counts.clientes) {
    totalClientsCount = statusData.counts.clientes;
  }
  if (!totalClientsCount && window.app?.statusData?.counts?.clientes) {
    totalClientsCount = window.app.statusData.counts.clientes;
  }

  // Update 5 KPI Cards
  const sTotalO = document.getElementById('statTotalOrders');
  const sPendO = document.getElementById('statPendingOrders');
  const sCompO = document.getElementById('statCompletedOrders');
  const sTotalP = document.getElementById('statTotalProducts');
  const sTotalC = document.getElementById('statTotalClients');

  if (sTotalO) sTotalO.textContent = orders.length;
  if (sPendO) sPendO.textContent = pendingOrders.length;
  if (sCompO) sCompO.textContent = completedOrders.length;
  if (sTotalP) sTotalP.textContent = totalProductsCount;
  if (sTotalC) sTotalC.textContent = totalClientsCount;

  // Render Recent Orders Table
  const tbody = document.getElementById('dashOrdersTable');
  if (tbody) {
    tbody.innerHTML = '';
    const recent = orders.slice(0, 6);
    if (recent.length === 0) {
      tbody.innerHTML = `<tr><td colSpan="6" class="text-center text-muted py-4">No hay pedidos registrados en MySQL.</td></tr>`;
    } else {
      recent.forEach(o => {
        const tr = document.createElement('tr');
        const estLabel = o.establecimiento === 'COMAS' 
          ? '<span class="badge bg-primary fs-8">Planta Comas</span>' 
          : '<span class="badge bg-info text-dark fs-8">Sucursal Carabayllo</span>';
        
        tr.innerHTML = `
          <td class="fw-bold text-primary">${escapeHtml(o.nro_pedido || ('PED-' + o.id_pedido))}</td>
          <td class="fw-semibold text-truncate" style="max-width:200px;">${escapeHtml(o.nombre_cliente || 'Cliente')}</td>
          <td>${estLabel}</td>
          <td>${formatDate(o.fecha_pedido || o.fecha_ingreso)}</td>
          <td><span class="status-badge ${o.estado || 'PENDIENTE'}">${escapeHtml(o.estado || 'PENDIENTE')}</span></td>
          <td class="text-center">
            <button class="btn btn-sm btn-outline-primary py-0 px-2 fs-8" onclick="app.viewOrderDetail('${o.id_pedido || o.id}')">Ver Detalle</button>
          </td>
        `;
        tbody.appendChild(tr);
      });
    }
  }

  // Render 4 Analytics Charts
  renderEstablecimientosChart(orders);
  renderMonthlyOrdersChart(orders);
  renderOrderStatusChart(orders);
  renderTopProductsChart(orders);
}

function getChartColors() {
  const isDark = document.documentElement.getAttribute('data-theme') === 'dark';
  return {
    ticks: isDark ? '#cbd5e1' : '#475569',
    grid: isDark ? '#334155' : '#e2e8f0',
    legend: isDark ? '#f8fafc' : '#0f172a'
  };
}

// 1. Pedidos por Establecimiento (Comas vs Carabayllo)
function renderEstablecimientosChart(orders) {
  try {
    if (typeof Chart === 'undefined') return;
    const canvas = document.getElementById('chartEstablecimientos');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const colors = getChartColors();

    if (window.establecimientosChartInstance) {
      window.establecimientosChartInstance.destroy();
    }

    let comasCount = 0;
    let carabaylloCount = 0;

    orders.forEach(o => {
      const est = (o.establecimiento || 'CARABAYLLO').toUpperCase();
      if (est.includes('COMAS')) {
        comasCount++;
      } else {
        carabaylloCount++;
      }
    });

    if (comasCount === 0 && carabaylloCount === 0 && orders.length === 0) {
      comasCount = 0;
      carabaylloCount = 0;
    }

    window.establecimientosChartInstance = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['Planta Principal - Comas', 'Sucursal - Carabayllo'],
        datasets: [{
          data: [comasCount, carabaylloCount],
          backgroundColor: ['#2563eb', '#06b6d4'],
          borderWidth: 2,
          borderColor: document.documentElement.getAttribute('data-theme') === 'dark' ? '#1e293b' : '#ffffff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              color: colors.legend,
              padding: 15,
              font: { size: 12, weight: 'bold' }
            }
          },
          tooltip: {
            callbacks: {
              label: function(context) {
                const val = context.parsed || 0;
                const total = (comasCount + carabaylloCount) || 1;
                const pct = ((val / total) * 100).toFixed(1);
                return ` ${context.label}: ${val} pedidos (${pct}%)`;
              }
            }
          }
        },
        cutout: '60%'
      }
    });
  } catch (e) {
    console.warn("Error rendering establecimientos chart:", e);
  }
}

// 2. Cantidad de Pedidos al Mes
function renderMonthlyOrdersChart(orders) {
  try {
    if (typeof Chart === 'undefined') return;
    const canvas = document.getElementById('chartMonthlyOrders');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const colors = getChartColors();

    if (window.monthlyOrdersChartInstance) {
      window.monthlyOrdersChartInstance.destroy();
    }

    const months = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
    const monthlyCounts = new Array(12).fill(0);

    orders.forEach(o => {
      const fecha = o.fecha_pedido || o.fecha_ingreso;
      if (fecha) {
        const parts = fecha.split('-');
        if (parts.length >= 2) {
          const monthIdx = parseInt(parts[1], 10) - 1;
          if (monthIdx >= 0 && monthIdx < 12) {
            monthlyCounts[monthIdx]++;
          }
        }
      }
    });

    window.monthlyOrdersChartInstance = new Chart(ctx, {
      type: 'line',
      data: {
        labels: months,
        datasets: [{
          label: 'Pedidos Registrados',
          data: monthlyCounts,
          borderColor: '#10b981',
          backgroundColor: 'rgba(16, 185, 129, 0.18)',
          fill: true,
          tension: 0.35,
          pointBackgroundColor: '#10b981',
          pointRadius: 4,
          pointHoverRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false }
        },
        scales: {
          x: {
            ticks: { color: colors.ticks },
            grid: { color: colors.grid }
          },
          y: {
            beginAtZero: true,
            ticks: { precision: 0, color: colors.ticks },
            grid: { color: colors.grid }
          }
        }
      }
    });
  } catch (e) {
    console.warn("Error rendering monthly orders chart:", e);
  }
}

// 3. Distribución de Pedidos por Estado Operativo
function renderOrderStatusChart(orders) {
  try {
    if (typeof Chart === 'undefined') return;
    const canvas = document.getElementById('chartOrderStatus');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const colors = getChartColors();

    if (window.orderStatusChartInstance) {
      window.orderStatusChartInstance.destroy();
    }

    const todayStr = new Date().toISOString().split('T')[0];

    const statusCounts = {
      'PENDIENTE': 0,
      'EN_PROCESO': 0,
      'FUERA_DE_TIEMPO': 0,
      'COMPLETADO': 0,
      'FINALIZADO': 0,
      'CANCELADO': 0
    };

    orders.forEach(o => {
      const st = (o.estado || 'PENDIENTE').toUpperCase();
      const isCancelled = st === 'ANULADO' || st === 'CANCELADO';
      const isFinalized = st === 'FINALIZADO';

      let sumReq = 0;
      let sumEnt = 0;
      if (o.detalles && Array.isArray(o.detalles)) {
        o.detalles.forEach(d => {
          sumReq += (Number(d.cantidad) || 0);
          sumEnt += (Number(d.cantidad_entregada) || 0);
        });
      }

      const hasShipments = (o.guias && Array.isArray(o.guias) && o.guias.length > 0) || sumEnt > 0;
      const isCompleted = st === 'ENTREGADO' || st === 'COMPLETADO' || (sumReq > 0 && sumEnt >= sumReq);
      const isOverdue = o.fecha_entrega && o.fecha_entrega < todayStr && !isCompleted && !isFinalized && !isCancelled;

      if (isCancelled) {
        statusCounts['CANCELADO']++;
      } else if (isCompleted) {
        statusCounts['COMPLETADO']++;
      } else if (isFinalized) {
        statusCounts['FINALIZADO']++;
      } else if (hasShipments || st === 'EN_PROCESO' || st === 'PROCESO' || st === 'PARCIAL' || st === 'EN PROCESO') {
        statusCounts['EN_PROCESO']++;
      } else if (isOverdue) {
        statusCounts['FUERA_DE_TIEMPO']++;
      } else {
        statusCounts['PENDIENTE']++;
      }
    });

    const labels = ['Pendiente', 'En Proceso', 'Fuera de Tiempo', 'Completado', 'Finalizado (Parcial)', 'Cancelado'];
    const data = [
      statusCounts['PENDIENTE'],
      statusCounts['EN_PROCESO'],
      statusCounts['FUERA_DE_TIEMPO'],
      statusCounts['COMPLETADO'],
      statusCounts['FINALIZADO'],
      statusCounts['CANCELADO']
    ];

    window.orderStatusChartInstance = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: labels,
        datasets: [{
          data: data,
          backgroundColor: ['#f59e0b', '#3b82f6', '#dc2626', '#10b981', '#8b5cf6', '#6b7280'],
          borderWidth: 2,
          borderColor: document.documentElement.getAttribute('data-theme') === 'dark' ? '#1e293b' : '#ffffff'
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: {
            position: 'bottom',
            labels: {
              color: colors.legend,
              padding: 12,
              font: { size: 11, weight: 'bold' }
            }
          }
        },
        cutout: '55%'
      }
    });
  } catch (e) {
    console.warn("Error rendering order status chart:", e);
  }
}

// 4. Productos Más Solicitados
function renderTopProductsChart(orders) {
  try {
    if (typeof Chart === 'undefined') return;
    const canvas = document.getElementById('chartTopProducts');
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    const colors = getChartColors();

    if (window.topProductsChartInstance) {
      window.topProductsChartInstance.destroy();
    }

    const prodCounts = {};
    orders.forEach(o => {
      if (o.detalles && Array.isArray(o.detalles)) {
        o.detalles.forEach(d => {
          const name = d.nombre_producto || 'Producto';
          prodCounts[name] = (prodCounts[name] || 0) + (Number(d.cantidad) || 1);
        });
      }
    });

    const sorted = Object.entries(prodCounts).sort((a, b) => b[1] - a[1]);
    const labels = sorted.slice(0, 5).map(item => item[0]);
    const data = sorted.slice(0, 5).map(item => item[1]);

    if (labels.length === 0) {
      labels.push('Sin Pedidos Registrados');
      data.push(0);
    }

    window.topProductsChartInstance = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: labels,
        datasets: [{
          label: 'Unidades Solicitadas',
          data: data,
          backgroundColor: ['#10b981', '#3b82f6', '#8b5cf6', '#f59e0b', '#ec4899'],
          borderRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        indexAxis: 'y',
        plugins: {
          legend: { display: false }
        },
        scales: {
          x: {
            beginAtZero: true,
            ticks: { precision: 0, color: colors.ticks },
            grid: { color: colors.grid }
          },
          y: {
            ticks: { color: colors.ticks, font: { size: 11 } },
            grid: { display: false }
          }
        }
      }
    });
  } catch (e) {
    console.warn("Error rendering top products chart:", e);
  }
}

export function updateChartThemes() {
  const colors = getChartColors();

  if (window.establecimientosChartInstance) {
    window.establecimientosChartInstance.options.plugins.legend.labels.color = colors.legend;
    window.establecimientosChartInstance.update('none');
  }

  if (window.monthlyOrdersChartInstance) {
    window.monthlyOrdersChartInstance.options.scales.x.ticks.color = colors.ticks;
    window.monthlyOrdersChartInstance.options.scales.x.grid.color = colors.grid;
    window.monthlyOrdersChartInstance.options.scales.y.ticks.color = colors.ticks;
    window.monthlyOrdersChartInstance.options.scales.y.grid.color = colors.grid;
    window.monthlyOrdersChartInstance.update('none');
  }

  if (window.orderStatusChartInstance) {
    window.orderStatusChartInstance.options.plugins.legend.labels.color = colors.legend;
    window.orderStatusChartInstance.update('none');
  }

  if (window.topProductsChartInstance) {
    if (window.topProductsChartInstance.options.scales) {
      window.topProductsChartInstance.options.scales.x.ticks.color = colors.ticks;
      window.topProductsChartInstance.options.scales.x.grid.color = colors.grid;
      window.topProductsChartInstance.options.scales.y.ticks.color = colors.ticks;
    }
    window.topProductsChartInstance.update('none');
  }
}
window.updateChartThemes = updateChartThemes;
