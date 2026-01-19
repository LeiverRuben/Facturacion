import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { environment } from '../../environments/environment';
import { Router } from '@angular/router';

interface Cliente {
  clienteId: number;
  clienteNombre: string;
  clienteApellido: string;
}

interface Producto {
  productoId: number;
  productoNombre: string;
  productoPrecio: number;
  productoStock: number;
}

interface DetalleFactura {
  productoId?: number;
  producto?: Producto;
  productoNombre?: string; // Mantener opcional por si acaso
  cantidad: number;
  precioUnitario: number;
  subtotal: number;
}

interface Factura {
  facturaId?: number;
  clienteId?: number;
  cliente?: Cliente;
  clienteNombre?: string;
  fechaFactura?: string;
  fechaEmision?: string;
  subtotal12?: number; // Backend field
  totalIva?: number;   // Backend field
  totalFactura: number;
  detalles: DetalleFactura[];
  estadoSri?: string;
  mensajeSri?: string;
}

@Component({
  selector: 'app-facturacion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="facturacion-container">
      
      <header class="page-header d-flex justify-content-between align-items-center">
        <div>
          <h2 class="page-title">Facturación Electrónica</h2>
          <p class="page-subtitle">Emite, autoriza y gestiona tus comprobantes</p>
        </div>
        <button class="btn-action btn-primary" (click)="showCreateForm = !showCreateForm">
          <i class='bx bx-plus'></i> {{ showCreateForm ? 'Cancelar' : 'Nueva Factura' }}
        </button>
      </header>

      <!-- Formulario de creación -->
      <div class="card-premium" *ngIf="showCreateForm">
        <h3 class="mb-4 fw-bold text-dark">Nueva Factura</h3>
        
        <form (ngSubmit)="saveFactura()">
          
          <!-- Cabecera de Factura -->
          <div class="form-grid mb-4">
            <div class="form-group">
              <label class="form-label">Cliente</label>
              <select class="form-control" [(ngModel)]="facturaForm.clienteId" name="clienteId" required>
                <option [ngValue]="0">Seleccionar cliente...</option>
                <option *ngFor="let cliente of clientes" [value]="cliente.clienteId">
                  {{ cliente.clienteNombre }} {{ cliente.clienteApellido }}
                </option>
              </select>
            </div>
            
            <div class="form-group">
              <label class="form-label">Fecha de Emisión</label>
              <input type="date" class="form-control" [(ngModel)]="facturaForm.fechaFactura" name="fechaFactura" required>
            </div>
          </div>

          <!-- Sección de Detalles -->
          <div class="p-4 bg-slate-50 rounded-lg border border-slate-200 mb-4" style="background:#f8fafc; border:1px solid #e2e8f0; border-radius:8px;">
             <h4 class="fw-bold fs-6 mb-3 text-secondary" style="color:#64748b; font-size: 0.95rem;">AGREGAR PRODUCTOS</h4>
             
             <div class="row g-3 align-items-end" style="display:flex; gap:1rem; align-items:flex-end; margin-bottom:1rem;">
                <div class="" style="flex: 2;">
                   <label class="form-label">Producto</label>
                   <select class="form-control" [(ngModel)]="selectedProductoId" name="selectedProductoId">
                      <option value="">Seleccionar producto...</option>
                      <option *ngFor="let producto of productos" [value]="producto.productoId">
                        {{ producto.productoNombre }} - {{ producto.productoPrecio | currency:'USD':'symbol':'1.2-2' }}
                      </option>
                   </select>
                </div>
                <div class="" style="width: 120px;">
                    <label class="form-label">Cantidad</label>
                    <input type="number" class="form-control" [(ngModel)]="selectedCantidad" name="selectedCantidad" min="1" (keypress)="validateNumber($event)">
                </div>
                <div class="">
                    <button type="button" class="btn-action btn-success" (click)="addDetalle()" [disabled]="!selectedProductoId || !selectedCantidad" style="background:#10b981; color:white;">
                      <i class='bx bx-cart-add'></i> Agregar
                    </button>
                </div>
             </div>

             <!-- Tabla de Detalles -->
             <div class="table-container mt-3" *ngIf="facturaForm.detalles.length > 0">
                <table class="table-modern">
                  <thead>
                    <tr>
                      <th>Producto</th>
                      <th class="text-center">Cant.</th>
                      <th class="text-end">P. Unit</th>
                      <th class="text-end">Subtotal</th>
                      <th></th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr *ngFor="let detalle of facturaForm.detalles; let i = index">
                      <td>{{ detalle.productoNombre || detalle.producto?.productoNombre }}</td>
                      <td class="text-center">{{ detalle.cantidad }}</td>
                      <td class="text-end">{{ detalle.precioUnitario | currency:'USD':'symbol':'1.2-2' }}</td>
                      <td class="text-end fw-bold">{{ detalle.subtotal | currency:'USD':'symbol':'1.2-2' }}</td>
                      <td class="text-end">
                        <button type="button" class="btn-action btn-danger" (click)="removeDetalle(i)" style="padding:4px 8px;">
                          <i class='bx bx-trash'></i>
                        </button>
                      </td>
                    </tr>
                  </tbody>
                </table>
             </div>

             <div class="d-flex justify-content-end mt-3" *ngIf="facturaForm.detalles.length > 0">
                <div class="text-end p-3 rounded" style="background:white; border:1px solid #e2e8f0; min-width: 250px;">
                   <span class="d-block text-muted small">TOTAL A PAGAR</span>
                   <span class="d-block fs-2 fw-bold text-primary" style="font-size:1.5rem; color:var(--primary-color);">{{ calcularTotal() | currency:'USD':'symbol':'1.2-2' }}</span>
                </div>
             </div>
          </div>

          <div class="d-flex justify-content-end gap-2">
            <button type="button" class="btn-action btn-secondary" (click)="cancelFactura()">Cancelar</button>
            <button type="submit" class="btn-action btn-primary" [disabled]="facturaForm.detalles.length === 0">
              <i class='bx bx-save'></i> Generar Factura
            </button>
          </div>
        </form>
      </div>

      <!-- Lista de facturas -->
      <div class="table-container">
        <table class="table-modern">
          <thead>
            <tr>
              <th>ID</th>
              <th>Cliente</th>
              <th>Fecha</th>
              <th>Total</th>
              <th>Estado SRI</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let factura of facturas">
              <td>#{{ factura.facturaId }}</td>
              <td>
                <div class="fw-bold text-dark">{{ factura.cliente?.clienteNombre || factura.clienteNombre }} {{ factura.cliente?.clienteApellido }}</div>
              </td>
              <td>{{ (factura.fechaEmision || factura.fechaFactura) | date:'dd/MM/yyyy' }}</td>
              <td class="fw-bold text-dark">{{ factura.totalFactura | currency:'USD':'symbol':'1.2-2' }}</td>
              <td>
                <span class="badge" 
                      [ngClass]="{
                        'badge-success': factura.estadoSri === 'AUTORIZADO', 
                        'badge-danger': factura.estadoSri !== 'AUTORIZADO' && factura.estadoSri !== 'PENDIENTE' && factura.estadoSri !== 'ENVIANDO...', 
                        'badge-warning': factura.estadoSri === 'PENDIENTE',
                        'badge-info': factura.estadoSri === 'ENVIANDO...'
                      }">
                  {{ factura.estadoSri || 'PENDIENTE' }}
                </span>
              </td>
              <td>
                  <div class="d-flex gap-2">
                    <button class="btn-action btn-secondary" (click)="viewFactura(factura)" title="Ver Detalles">
                      <i class='bx bx-show'></i>
                    </button>
                    <!-- PDF Button -->
                    <button class="btn-action btn-secondary" (click)="descargarPdf(factura.facturaId!)" title="Imprimir PDF" style="background: #475569; color: white;">
                      <i class='bx bxs-file-pdf'></i>
                    </button>
                    <button class="btn-action btn-primary" *ngIf="factura.estadoSri !== 'AUTORIZADO'" (click)="enviarSri(factura)" title="Enviar al SRI">
                      <i class='bx bx-send'></i>
                    </button>
                    <button class="btn-action btn-danger" (click)="deleteFactura(factura.facturaId!)" title="Eliminar">
                      <i class='bx bx-trash'></i>
                    </button>
                  </div>
                </td>
            </tr>
            <tr *ngIf="facturas.length === 0">
              <td colspan="6" class="text-center py-5 text-muted">
                <i class='bx bx-receipt fs-1 mb-2'></i>
                <p>No se han emitido facturas.</p>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <!-- Modal con Backdrop -->
      <div class="modal-backdrop" *ngIf="selectedFactura">
         <div class="modal-card card-premium" style="max-width: 800px; width: 90%; margin: 2rem auto; position: relative; z-index: 1050; max-height: 90vh; overflow-y: auto;">
            
            <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3">
               <h3 class="m-0 fw-bold">Factura #{{ selectedFactura.facturaId }}</h3>
               <button class="btn-action btn-secondary" (click)="closeModal()"><i class='bx bx-x fs-4'></i></button>
            </div>

            <div class="row mb-4">
               <div class="col-6">
                  <p class="text-muted small mb-1">CLIENTE</p>
                  <p class="fw-bold">{{ selectedFactura.cliente?.clienteNombre || selectedFactura.clienteNombre }} {{ selectedFactura.cliente?.clienteApellido }}</p>
               </div>
               <div class="col-6 text-end">
                  <p class="text-muted small mb-1">FECHA EMISIÓN</p>
                  <p class="fw-bold">{{ (selectedFactura.fechaEmision || selectedFactura.fechaFactura) | date:'dd/MM/yyyy' }}</p>
               </div>
            </div>

            <div class="alert alert-info mb-4" *ngIf="selectedFactura.mensajeSri" style="background:#e0f2fe; color:#075985; padding:1rem; border-radius:8px;">
               <strong>Mensaje SRI:</strong> {{ selectedFactura.mensajeSri }}
            </div>

            <div class="table-container mb-4">
              <table class="table-modern">
                <thead>
                  <tr>
                    <th>Producto</th>
                    <th>Cant.</th>
                    <th>P. Unit.</th>
                    <th>Subtotal</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let detalle of selectedFactura.detalles">
                    <td>{{ detalle.producto?.productoNombre || detalle.productoNombre }}</td>
                    <td>{{ detalle.cantidad }}</td>
                    <td>{{ detalle.precioUnitario | currency:'USD':'symbol':'1.2-2' }}</td>
                    <td>{{ detalle.subtotal | currency:'USD':'symbol':'1.2-2' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>

            <div class="d-flex justify-content-end">
               <div class="text-end" style="min-width: 200px;">
                  <div class="d-flex justify-content-between py-1 text-muted"><span>Subtotal:</span> <span>{{ (selectedFactura.subtotal12 || 0) | currency:'USD':'symbol':'1.2-2' }}</span></div>
                  <div class="d-flex justify-content-between py-1 text-muted"><span>IVA (12%):</span> <span>{{ (selectedFactura.totalIva || 0) | currency:'USD':'symbol':'1.2-2' }}</span></div>
                  <div class="d-flex justify-content-between py-2 border-top mt-2 fw-bold fs-5"><span>Total:</span> <span>{{ selectedFactura.totalFactura | currency:'USD':'symbol':'1.2-2' }}</span></div>
               </div>
            </div>

         </div>
      </div>

    </div>
  `,
  styles: [`
    .facturacion-container {
      max-width: 1200px;
      margin: 0 auto;
    }
    .modal-backdrop {
      position: fixed;
      top: 0;
      left: 0;
      width: 100vw;
      height: 100vh;
      background: rgba(0,0,0,0.5);
      z-index: 1040;
      display: flex;
      align-items: flex-start;
      justify-content: center;
      padding-top: 2rem;
    }
  `]
})
export class FacturacionComponent implements OnInit {
  // ... (previous variables)
  private apiUrl = environment.apiUrl;
  clientes: Cliente[] = [];
  productos: Producto[] = [];
  facturas: Factura[] = [];
  showCreateForm = false;
  selectedFactura: Factura | null = null;
  selectedProductoId: number | null = null;
  selectedCantidad: number = 1;
  facturaForm: Factura = {
    clienteId: 0,
    fechaFactura: new Date().toISOString().split('T')[0],
    totalFactura: 0,
    detalles: []
  };

  constructor(private http: HttpClient, private router: Router) { } // Inject Router

  ngOnInit() {
    this.checkCajaStatus(); // Verificar Caja primero
    this.loadClientes();
    this.loadProductos();
    this.loadFacturas();
  }

  checkCajaStatus() {
    this.http.get(`${this.apiUrl}/api/caja/estado`).subscribe({
      next: (res: any) => {
        if (res.estado !== 'ABIERTA') {
          alert('⚠️ DEBE ABRIR UNA CAJA PARA PODER FACTURAR.');
          this.router.navigate(['/caja']);
        }
      },
      error: (err) => {
        console.error('Error verificando caja:', err);
        // Optionally, handle error more gracefully, e.g., show a message and redirect
        alert('Error al verificar el estado de la caja. Por favor, intente de nuevo.');
        this.router.navigate(['/caja']);
      }
    });
  }

  loadClientes() {
    this.http.get<Cliente[]>(`${this.apiUrl}/api/cliente`).subscribe({
      next: (data) => {
        this.clientes = data;
      },
      error: (error) => {
        console.error('Error loading clients:', error);
      }
    });
  }

  loadProductos() {
    this.http.get<Producto[]>(`${this.apiUrl}/api/productos`).subscribe({
      next: (data) => {
        this.productos = data;
      },
      error: (error) => {
        console.error('Error loading products:', error);
      }
    });
  }

  loadFacturas() {
    this.http.get<Factura[]>(`${this.apiUrl}/api/facturas`).subscribe({
      next: (data) => {
        this.facturas = data;
      },
      error: (error) => {
        console.error('Error loading invoices:', error);
      }
    });
  }

  addDetalle() {
    if (!this.selectedProductoId || !this.selectedCantidad) return;
    const producto = this.productos.find(p => p.productoId === Number(this.selectedProductoId));
    if (!producto) return;

    const detalle: DetalleFactura = {
      productoId: producto.productoId,
      productoNombre: producto.productoNombre,
      cantidad: this.selectedCantidad,
      precioUnitario: producto.productoPrecio,
      subtotal: producto.productoPrecio * this.selectedCantidad
    };

    this.facturaForm.detalles.push(detalle);
    this.selectedProductoId = null;
    this.selectedCantidad = 1;
  }

  removeDetalle(index: number) {
    this.facturaForm.detalles.splice(index, 1);
  }

  calcularTotal(): number {
    return this.facturaForm.detalles.reduce((total, detalle) => total + detalle.subtotal, 0);
  }

  saveFactura() {
    const subtotal = this.calcularTotal();
    const ivaPercentage = 0.12;
    const totalIva = subtotal * ivaPercentage;
    const totalFactura = subtotal + totalIva;

    const payload = {
      clienteId: this.facturaForm.clienteId,
      empresaId: 1,
      secuencial: Math.floor(Math.random() * 999999999).toString().padStart(9, '0'),
      fechaEmision: new Date(this.facturaForm.fechaFactura || new Date()).toISOString(),
      subtotal12: subtotal,
      subtotal0: 0,
      subtotalExento: 0,
      subtotalNoObjeto: 0,
      totalDescuento: 0,
      totalIva: totalIva,
      totalFactura: totalFactura,
      detalles: this.facturaForm.detalles.map(d => ({
        productoId: d.productoId,
        cantidad: d.cantidad,
        precioUnitario: d.precioUnitario,
        descuento: 0,
        subtotal: d.subtotal,
        impuesto: {
          codigo: "2",
          codigoPorcentaje: 2,
          tarifa: 12,
          baseImponible: d.subtotal,
          valor: d.subtotal * ivaPercentage
        }
      })),
      pagos: [
        {
          metodoPagoId: 1,
          total: totalFactura,
          plazo: 0,
          unidadTiempo: "dias"
        }
      ]
    };

    this.http.post<any>(`${this.apiUrl}/api/facturas`, payload).subscribe({
      next: (response) => {
        console.log('Factura creada:', response);
        this.loadFacturas();
        this.cancelFactura();
        alert('Factura creada exitosamente. Ahora puedes enviarla al SRI desde la lista.');
      },
      error: (error) => {
        console.error('Error creating invoice:', error);
        const errorMessage = error.error?.message || error.error || error.message || 'Unknown error';
        alert('Error al crear factura: ' + (typeof errorMessage === 'object' ? JSON.stringify(errorMessage) : errorMessage));
      }
    });
  }

  enviarSri(factura: Factura) {
    if (!factura.facturaId) return;

    // Feedback visual inmediato
    factura.estadoSri = 'ENVIANDO...';

    this.http.post<any>(`${this.apiUrl}/api/facturas/enviar-sri/${factura.facturaId}`, {}).subscribe({
      next: (response) => {
        console.log('Respuesta SRI:', response);
        alert(response.mensaje);
        this.loadFacturas(); // Recargar para ver el estado actualizado
      },
      error: (error) => {
        console.error('Error enviando al SRI:', error);
        factura.estadoSri = 'ERROR';
        alert('Error al enviar al SRI: ' + (error.error || error.message));
        this.loadFacturas();
      }
    });
  }

  viewFactura(factura: Factura) {
    this.selectedFactura = factura;
  }

  deleteFactura(id: number) {
    if (confirm('¿Está seguro de que desea eliminar esta factura?')) {
      this.http.delete(`${this.apiUrl}/api/facturas/${id}`).subscribe({
        next: () => {
          this.loadFacturas();
        },
        error: (error) => {
          console.error('Error deleting invoice:', error);
        }
      });
    }
  }

  cancelFactura() {
    this.facturaForm = {
      clienteId: 0,
      fechaFactura: new Date().toISOString().split('T')[0],
      totalFactura: 0,
      detalles: []
    };
    this.showCreateForm = false;
    this.selectedProductoId = null;
    this.selectedCantidad = 1;
  }

  validateNumber(event: KeyboardEvent) {
    const charCode = (event.which) ? event.which : event.keyCode;
    if (charCode > 31 && (charCode < 48 || charCode > 57) && charCode !== 46) {
      event.preventDefault();
    }
  }

  closeModal() {
    this.selectedFactura = null;
  }

  descargarPdf(facturaId: number) {
    this.http.get(`${this.apiUrl}/api/facturas/${facturaId}/pdf`, { responseType: 'blob' }).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        window.open(url, '_blank');
      },
      error: (err) => console.error('Error al descargar PDF', err)
    });
  }
}
