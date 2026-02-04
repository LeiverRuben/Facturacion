import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common'; // Importante para *ngIf, *ngFor, pipes
import { environment } from '../../environments/environment';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData, ChartType } from 'chart.js';

interface DashboardStats {
  totalClientes: number;
  totalProductos: number;
  totalFacturas: number;
  ventasUltimaSemana?: { dia: string; total: number }[];
}

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, CommonModule, BaseChartDirective],
  template: `
    <div class="dashboard-container">
      
      <header class="page-header">
        <h2 class="title">Dashboard General</h2>
        <p class="subtitle">Bienvenido al panel de control de tu negocio</p>
      </header>
      
      <!-- Grid de Tarjetas -->
      <div class="stats-grid">
        
        <div class="stat-card blue">
          <div class="stat-icon">
            <i class='bx bxs-user-detail'></i>
          </div>
          <div class="stat-info">
            <span class="label">Total Clientes</span>
            <span class="value">{{ stats.totalClientes }}</span>
          </div>
        </div>

        <div class="stat-card green">
          <div class="stat-icon">
             <i class='bx bxs-package'></i>
          </div>
          <div class="stat-info">
            <span class="label">Productos Activos</span>
            <span class="value">{{ stats.totalProductos }}</span>
          </div>
        </div>

        <div class="stat-card purple">
          <div class="stat-icon">
            <i class='bx bxs-file'></i>
          </div>
          <div class="stat-info">
            <span class="label">Facturas Emitidas</span>
            <span class="value">{{ stats.totalFacturas }}</span>
          </div>
        </div>

      </div>

      <!-- Grid Principal: 2 Columnas (Izquierda: Tablas, Derecha: Gráfico) -->
      <div class="dashboard-main-grid">
        
        <!-- Columna Izquierda -->
        <div class="left-column">
            
            <!-- Últimas Transacciones -->
            <section class="dashboard-section slide-in-bottom">
                <div class="section-header">
                    <h3 class="section-title"><i class='bx bx-receipt'></i> Últimas Facturas</h3>
                    <a routerLink="/facturacion" class="btn-link">Ver todas</a>
                </div>
                <div class="card-premium no-padding">
                    <table class="table-modern">
                        <thead>
                            <tr>
                                <th>#</th>
                                <th>Cliente</th>
                                <th class="text-right">Total</th>
                                <th></th>
                            </tr>
                        </thead>
                        <tbody>
                            <tr *ngFor="let f of ultimasFacturas">
                                <td><span class="badge-neutral">#{{ f.facturaId }}</span></td>
                                <td class="fw-bold">{{ f.cliente?.clienteNombre || 'Consumidor Final' }}</td>
                                <td class="text-right">{{ f.facturaTotal | currency:'USD' }}</td>
                                <td class="text-center"><i class='bx bx-chevron-right text-muted'></i></td>
                            </tr>
                             <tr *ngIf="ultimasFacturas.length === 0">
                                <td colspan="4" class="text-center py-4 text-muted">Sin movimientos recientes</td>
                            </tr>
                        </tbody>
                    </table>
                </div>
            </section>

            <!-- Alerta de Stock Bajo -->
            <section class="dashboard-section slide-in-bottom" style="animation-delay: 0.1s;">
                 <div class="section-header">
                    <h3 class="section-title text-danger"><i class='bx bx-error-circle'></i> Alerta de Stock Bajo</h3>
                    <a routerLink="/productos" class="btn-link">Gestionar Inventario</a>
                </div>
                <div class="stock-grid">
                    <div *ngFor="let p of productosBajoStock" class="stock-card-mini">
                        <div class="stock-info">
                            <span class="product-name">{{ p.productoNombre }}</span>
                            <span class="product-sku">{{ p.productoSerial }}</span>
                        </div>
                        <div class="stock-level">
                            <span class="stock-badge">{{ p.productoStock }} unid.</span>
                        </div>
                    </div>
                     <div *ngIf="productosBajoStock.length === 0" class="text-center py-4 text-muted w-100">
                        ¡Todo en orden! No hay productos con stock crítico.
                    </div>
                </div>
            </section>

        </div>

        <!-- Columna Derecha -->
        <div class="right-column">
            
            <!-- Resumen Financiero -->
             <section class="dashboard-section slide-in-bottom" style="animation-delay: 0.2s;">
                <div class="section-header">
                    <h3 class="section-title">Resumen Semanal</h3>
                </div>
                <div class="card-premium finance-card">
                    <div class="finance-header">
                        <span class="finance-label">Ingresos Totales</span>
                        <h2 class="finance-value">{{ ingresosSemanales | currency:'USD' }}</h2>
                        <span class="finance-trend positive"><i class='bx bx-trending-up'></i> +12% esta semana</span>
                    </div>
                    
                    <!-- Gráfico CSS Simple -->
                    <div class="chart-container">
                        <canvas baseChart
                          [data]="barChartData"
                          [options]="barChartOptions"
                          [type]="barChartType">
                        </canvas>
                    </div>

                </div>
             </section>

        </div>

      </div>

    </div>
  `,
  styles: [`
    .dashboard-container {
      max-width: 1400px;
      margin: 0 auto;
    }
    
    .page-header {
      margin-bottom: 2rem;
    }

    .title {
      font-size: 1.8rem;
      font-weight: 700;
      color: #1e293b;
      margin: 0;
    }

    .subtitle {
      color: #64748b;
      margin-top: 0.5rem;
    }

    /* Stats Cards Top */
    .stats-grid {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
      gap: 1.5rem;
      margin-bottom: 2.5rem;
    }

    .stat-card {
      background: var(--card-bg);
      border-radius: 16px;
      padding: 1.5rem;
      display: flex;
      align-items: center;
      gap: 1.5rem;
      box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
      border: 1px solid var(--border-color);
      transition: transform 0.2s;
    }

    .stat-card:hover { transform: translateY(-3px); }

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.8rem;
    }

    .stat-card.blue .stat-icon { background: #eff6ff; color: #3b82f6; }
    .stat-card.green .stat-icon { background: #f0fdf4; color: #22c55e; }
    .stat-card.purple .stat-icon { background: #f5f3ff; color: #8b5cf6; }

    .stat-info { display: flex; flex-direction: column; }
    .stat-info .label { color: var(--text-muted); font-size: 0.85rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
    .stat-info .value { color: var(--text-main); font-size: 1.8rem; font-weight: 800; line-height: 1.1; margin-top: 4px; }


    /* MAIN GRID LAYOUT */
    .dashboard-main-grid {
        display: grid;
        grid-template-columns: 2fr 1fr;
        gap: 2rem;
    }

    .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1rem;
    }

    .section-title {
        font-size: 1.1rem;
        font-weight: 700;
        color: var(--text-main);
        margin: 0;
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }

    .btn-link {
        color: var(--primary-color);
        text-decoration: none;
        font-size: 0.9rem;
        font-weight: 600;
    }

    .card-premium {
        background: var(--card-bg);
        border-radius: 16px;
        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
        border: 1px solid var(--border-color);
        overflow: hidden;
    }

    .card-premium.no-padding { padding: 0; }

    /* Tables */
    .table-modern { width: 100%; border-collapse: collapse; }
    .table-modern th { background: var(--bg-color); color: var(--text-muted); font-weight: 600; font-size: 0.85rem; padding: 1rem; text-align: left; }
    .table-modern td { padding: 1rem; border-bottom: 1px solid var(--border-color); color: var(--text-main); font-size: 0.95rem; }
    .table-modern tr:last-child td { border-bottom: none; }
    
    .badge-neutral { background: var(--bg-color); color: var(--text-muted); padding: 4px 8px; border-radius: 6px; font-size: 0.8rem; font-weight: 600; }
    .text-right { text-align: right; }
    .text-center { text-align: center; }
    .fw-bold { font-weight: 600; }
    .text-muted { color: var(--text-muted) !important; }

    /* Stock Alerts */
    .stock-grid { display: flex; flex-direction: column; gap: 0.8rem; }
    .stock-card-mini {
        background: var(--card-bg);
        border: 1px solid #fee2e2;
        border-left: 4px solid #ef4444;
        border-radius: 8px;
        padding: 1rem;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }
    .stock-info { display: flex; flex-direction: column; }
    .product-name { font-weight: 600; color: var(--text-main); }
    .product-sku { font-size: 0.8rem; color: var(--text-muted); }

    .stat-card:hover { transform: translateY(-3px); }

    .stat-icon {
      width: 56px;
      height: 56px;
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 1.8rem;
    }

    .stat-card.blue .stat-icon { background: #eff6ff; color: #3b82f6; }
    .stat-card.green .stat-icon { background: #f0fdf4; color: #22c55e; }
    .stat-card.purple .stat-icon { background: #f5f3ff; color: #8b5cf6; }

    .stat-info { display: flex; flex-direction: column; }
    .stat-info .label { color: #64748b; font-size: 0.85rem; font-weight: 600; text-transform: uppercase; letter-spacing: 0.5px; }
    .stat-info .value { color: var(--text-main); font-size: 1.8rem; font-weight: 800; line-height: 1.1; margin-top: 4px; }


    /* MAIN GRID LAYOUT */
    .dashboard-main-grid {
        display: grid;
        grid-template-columns: 2fr 1fr;
        gap: 2rem;
    }

    .section-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        margin-bottom: 1rem;
    }

    .section-title {
        font-size: 1.1rem;
        font-weight: 700;
        color: var(--text-main);
        margin: 0;
        display: flex;
        align-items: center;
        gap: 0.5rem;
    }

    .btn-link {
        color: var(--primary-color);
        text-decoration: none;
        font-size: 0.9rem;
        font-weight: 600;
    }

    .card-premium {
        background: var(--card-bg);
        border-radius: 16px;
        box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);
        border: 1px solid var(--border-color);
        overflow: hidden;
    }

    .card-premium.no-padding { padding: 0; }

    /* Tables */
    .table-modern { width: 100%; border-collapse: collapse; }
    .table-modern th { background: var(--bg-color); color: var(--text-muted); font-weight: 600; font-size: 0.85rem; padding: 1rem; text-align: left; }
    .table-modern td { padding: 1rem; border-bottom: 1px solid var(--border-color); color: var(--text-main); font-size: 0.95rem; }
    .table-modern tr:last-child td { border-bottom: none; }
    
    .badge-neutral { background: var(--bg-color); color: var(--text-muted); padding: 4px 8px; border-radius: 6px; font-size: 0.8rem; font-weight: 600; }
    .text-right { text-align: right; }
    .text-center { text-align: center; }
    .fw-bold { font-weight: 600; }
    .text-muted { color: var(--text-muted) !important; }

    /* Stock Alerts */
    .stock-grid { display: flex; flex-direction: column; gap: 0.8rem; }
    .stock-card-mini {
        background: var(--card-bg);
        border: 1px solid #fee2e2;
        border-left: 4px solid #ef4444;
        border-radius: 8px;
        padding: 1rem;
        display: flex;
        justify-content: space-between;
        align-items: center;
    }
    .stock-info { display: flex; flex-direction: column; }
    .product-name { font-weight: 600; color: var(--text-main); }
    .product-sku { font-size: 0.8rem; color: var(--text-muted); }
    .stock-badge { background: #fef2f2; color: #ef4444; padding: 4px 10px; border-radius: 20px; font-weight: 700; font-size: 0.85rem; }

    /* Financial Chart Widget */
    .finance-card { padding: 1.5rem; background: var(--card-bg); border: 1px solid var(--border-color); border-radius: 16px; }
    .finance-header { text-align: center; margin-bottom: 2rem; }
    .finance-label { color: var(--text-muted); font-size: 0.9rem; font-weight: 500; display: block; }
    .finance-value { font-size: 2.5rem; font-weight: 800; color: var(--text-main); margin: 0.5rem 0; }
    .finance-trend { font-size: 0.9rem; font-weight: 600; display: inline-flex; align-items: center; gap: 4px; }
    .finance-trend.positive { color: #16a34a; background: #dcfce7; padding: 4px 12px; border-radius: 20px; }

    .chart-container {
        display: flex;
        justify-content: space-between;
        align-items: flex-end;
        height: 150px;
        padding-top: 1rem;
    }
    .chart-bar-wrapper {
        display: flex;
        flex-direction: column;
        align-items: center;
        gap: 0.5rem;
        width: 100%;
    }
    .chart-bar {
        width: 8px;
        background: var(--primary-color);
        border-radius: 10px;
        transition: height 1s ease-out, background 0.2s;
        min-height: 10%;
        opacity: 0.8;
    }
    .chart-bar:hover { opacity: 1; transform: scaleX(1.5); }
    .chart-label { font-size: 0.75rem; color: var(--text-muted); font-weight: 600; }

    /* Responsive */
    @media (max-width: 1024px) {
        .dashboard-main-grid { grid-template-columns: 1fr; }
    }



  `]
})
export class DashboardComponent implements OnInit {
  private apiUrl = environment.apiUrl;

  // Propiedad que faltaba
  stats: DashboardStats = {
    totalClientes: 0,
    totalProductos: 0,
    totalFacturas: 0
  };

  // Nuevas Propiedades
  productosBajoStock: any[] = [];
  ultimasFacturas: any[] = [];
  ingresosSemanales: number = 0;

  // Configuracion del Grafico
  public barChartOptions: ChartConfiguration['options'] = {
    responsive: true,
    maintainAspectRatio: false,
    plugins: {
      legend: { display: false }
    },
    scales: {
      y: {
        beginAtZero: true,
        grid: { color: 'rgba(0,0,0,0.05)' }
      },
      x: {
        grid: { display: false }
      }
    }
  };
  public barChartType: ChartType = 'bar';
  public barChartData: ChartData<'bar'> = {
    labels: [],
    datasets: [
      { data: [], label: 'Ventas', backgroundColor: '#3b82f6', borderRadius: 4, hoverBackgroundColor: '#2563eb' }
    ]
  };

  constructor(private http: HttpClient) { }

  ngOnInit() {
    this.loadStats();
  }

  loadStats() {
    this.http.get<any>(`${this.apiUrl}/api/dashboard/stats`).subscribe(data => {
      // 1. Stats Generales
      this.stats = {
        totalClientes: data.totalClientes,
        totalProductos: data.totalProductos,
        totalFacturas: data.totalFacturas
      };

      // 2. Ingresos y Facturas
      this.ingresosSemanales = data.ventasSemana; // Ahora viene directo del backend
      this.ultimasFacturas = data.ultimasFacturas;

      // 3. Procesar Gráfico Real
      if (data.ventasUltimaSemana && Array.isArray(data.ventasUltimaSemana)) {
        this.barChartData = {
          labels: data.ventasUltimaSemana.map((d: any) => d.dia),
          datasets: [
            {
              data: data.ventasUltimaSemana.map((d: any) => d.total),
              label: 'Ventas',
              backgroundColor: '#3b82f6',
              borderRadius: 4,
              hoverBackgroundColor: '#2563eb'
            }
          ]
        };
      }
    });

    // 3. Productos Bajo Stock (Mantenemos la llamada separada o la integramos si el DTO la trae)
    // Por ahora la mantengo separada para no romper esa parte si no la puse en el DTO completo
    this.http.get<any[]>(`${this.apiUrl}/api/productos`).subscribe(productos => {
      this.productosBajoStock = productos
        .filter(p => (p.productoStock || 0) < 10)
        .slice(0, 5);
    });
  }


}
