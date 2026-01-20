import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { environment } from '../../environments/environment';
import { Router } from '@angular/router';
import Swal from 'sweetalert2';

interface Cliente {
  clienteId: number;
  clienteNombre: string;
  clienteApellido: string;
  identificacion?: string;
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

interface FormaPago {
  formaPagoId: number;
  nombre: string;
  codigoSri: string;
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
                  {{ cliente.clienteNombre }} {{ cliente.clienteApellido }} {{ cliente.identificacion ? '(' + cliente.identificacion + ')' : '' }}
                </option>
              </select>
            </div>
            
            <div class="form-group">
              <label class="form-label">Fecha de Emisión</label>
              <input type="date" class="form-control" [(ngModel)]="facturaForm.fechaFactura" name="fechaFactura" required>
            </div>

            
            <div class="form-group">
              <label class="form-label">Forma de Pago</label>
              <select class="form-control" [(ngModel)]="selectedFormaPagoId" name="formaPagoId" required (change)="plazo = (selectedFormaPagoId == 6 ? plazo : 0)">
                <option *ngFor="let fp of formasPago" [value]="fp.formaPagoId">
                  {{ fp.nombre }}
                </option>
              </select>
            </div>

            <div class="form-group" *ngIf="selectedFormaPagoId == 6">
              <label class="form-label">Plazo (días)</label>
              <input type="number" class="form-control" [(ngModel)]="plazo" name="plazo" min="0">
            </div>
          </div>

          <!-- Sección de Detalles -->
          <div class="card-premium p-4 mb-4">
             <h4 class="fw-bold fs-6 mb-3 text-secondary" style="font-size: 0.95rem;">AGREGAR PRODUCTOS</h4>
             
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
                <div class="text-end p-3 rounded border" style="min-width: 250px;">
                   <span class="d-block text-muted small">TOTAL A PAGAR</span>
                   <span class="d-block fs-2 fw-bold text-primary" style="font-size:1.5rem;">{{ calcularTotal() | currency:'USD':'symbol':'1.2-2' }}</span>
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

      <!-- Filtros -->
      <div class="mb-3">
        <div class="btn-group" role="group">
          <button type="button" class="btn" [ngClass]="{'btn-primary': filterStatus === 'TODAS', 'btn-outline-primary': filterStatus !== 'TODAS'}" (click)="filterStatus = 'TODAS'">Todas</button>
          <button type="button" class="btn" [ngClass]="{'btn-primary': filterStatus === 'ACTIVAS', 'btn-outline-primary': filterStatus !== 'ACTIVAS'}" (click)="filterStatus = 'ACTIVAS'">Emitidas</button>
          <button type="button" class="btn" [ngClass]="{'btn-primary': filterStatus === 'ANULADAS', 'btn-outline-primary': filterStatus !== 'ANULADAS'}" (click)="filterStatus = 'ANULADAS'">Anuladas</button>
        </div>
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
            <tr *ngFor="let factura of filteredFacturas">
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
                        'badge-danger': factura.estadoSri !== 'AUTORIZADO' && factura.estadoSri !== 'PENDIENTE' && factura.estadoSri !== 'ENVIANDO...' && factura.estadoSri !== 'ANULADA', 
                        'badge-warning': factura.estadoSri === 'PENDIENTE',
                        'badge-info': factura.estadoSri === 'ENVIANDO...',
                        'badge-secondary': factura.estadoSri === 'ANULADA'
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
                    <button class="btn-action btn-primary" *ngIf="factura.estadoSri !== 'AUTORIZADO' && factura.estadoSri !== 'ANULADA'" (click)="enviarSri(factura)" title="Enviar al SRI">
                      <i class='bx bx-send'></i>
                    </button>
                    <button class="btn-action btn-danger" *ngIf="factura.estadoSri !== 'ANULADA'" (click)="anularFactura(factura.facturaId!)" title="Anular Factura" style="background-color: #ef4444; color: white;">
                      <i class='bx bx-x-circle'></i>
                    </button>
                  </div>
                </td>
            </tr>
            <tr *ngIf="filteredFacturas.length === 0">
              <td colspan="6" class="text-center py-5 text-muted">
                <i class='bx bx-receipt fs-1 mb-2'></i>
                <p>No se encontraron facturas.</p>
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

  formasPago: FormaPago[] = [];
  selectedFormaPagoId: number = 1; // Default
  plazo: number = 0;

  filterStatus: 'TODAS' | 'ACTIVAS' | 'ANULADAS' = 'TODAS';

  get filteredFacturas() {
    if (this.filterStatus === 'TODAS') {
      return this.facturas;
    } else if (this.filterStatus === 'ACTIVAS') {
      return this.facturas.filter(f => f.estadoSri !== 'ANULADA');
    } else {
      return this.facturas.filter(f => f.estadoSri === 'ANULADA');
    }
  }

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
    this.loadFormasPago();
    this.loadFacturas();
  }

  checkCajaStatus() {
    this.http.get(`${this.apiUrl}/api/caja/estado`).subscribe({
      next: (res: any) => {
        if (res.estado !== 'ABIERTA') {
          Swal.fire({
            icon: 'warning',
            title: 'Caja Cerrada',
            text: '⚠️ DEBE ABRIR UNA CAJA PARA PODER FACTURAR.',
            confirmButtonText: 'Ir a Caja'
          }).then(() => {
            this.router.navigate(['/caja']);
          });
        }
      },
      error: (err) => {
        console.error('Error verificando caja:', err);
        Swal.fire('Error', 'Error al verificar el estado de la caja.', 'error');
        this.router.navigate(['/caja']);
      }
    });
  }

  // ... (keeping other methods same until saveFactura)

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

  loadFormasPago() {
    this.http.get<FormaPago[]>(`${this.apiUrl}/api/formapago`).subscribe({
      next: (data) => {
        this.formasPago = data;
        // Si no hay seleccionado, seleccionar el primero o el ID 1
        if (!this.selectedFormaPagoId && data.length > 0) {
          this.selectedFormaPagoId = data[0].formaPagoId;
        }
      },
      error: (error) => console.error('Error loading payment methods:', error)
    });
  }

  addDetalle() {
    if (!this.selectedProductoId) return;

    if (!this.selectedCantidad || this.selectedCantidad <= 0) {
      Swal.fire({
        icon: 'warning',
        title: 'Atención',
        text: '¡ELIJA CANTIDAD!',
        confirmButtonColor: '#3085d6',
      });
      return;
    }
    const producto = this.productos.find(p => p.productoId === Number(this.selectedProductoId));
    if (!producto) return;

    // VALIDACIÓN DE STOCK
    if (this.selectedCantidad > producto.productoStock) {
      Swal.fire({
        icon: 'error',
        title: 'Stock Insuficiente',
        html: `Solo tienes <b>${producto.productoStock}</b> unidades disponibles de <br>${producto.productoNombre}.`,
        confirmButtonText: 'Entendido'
      });
      return;
    }

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
          metodoPagoId: this.selectedFormaPagoId,
          total: totalFactura,
          plazo: this.plazo,
          unidadTiempo: "dias"
        }
      ]
    };

    this.http.post<any>(`${this.apiUrl}/api/facturas`, payload).subscribe({
      next: (response) => {
        console.log('Factura creada:', response);
        this.loadFacturas();
        this.cancelFactura();
        Swal.fire({
          icon: 'success',
          title: 'Factura Creada',
          text: 'Factura creada exitosamente. Ahora puedes enviarla al SRI.'
        });
      },
      error: (error) => {
        console.error('Error creating invoice:', error);
        const errorMessage = error.error?.message || error.error || error.message || 'Unknown error';
        Swal.fire('Error', 'Error al crear factura: ' + (typeof errorMessage === 'object' ? JSON.stringify(errorMessage) : errorMessage), 'error');
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
        Swal.fire({
          icon: 'success',
          title: 'Respuesta SRI',
          text: response.mensaje
        });
        this.loadFacturas();
      },
      error: (error) => {
        console.error('Error enviando al SRI:', error);
        factura.estadoSri = 'ERROR';
        Swal.fire('Error SRI', 'Error al enviar al SRI: ' + (error.error || error.message), 'error');
        this.loadFacturas();
      }
    });
  }

  // ... 

  viewFactura(factura: Factura) {
    this.selectedFactura = factura;
  }

  anularFactura(id: number) {
    Swal.fire({
      title: '¿Anular Factura?',
      text: "Esta acción es irreversible y cambiará el estado a ANULADA.",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      cancelButtonColor: '#3085d6',
      confirmButtonText: 'Sí, anular',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        // Solicitamos 'text' para evitar que Angular intente parsear JSON automáticamente y falle si es texto plano.
        this.http.put(`${this.apiUrl}/api/facturas/${id}/anular`, {}, { responseType: 'text' }).subscribe({
          next: (response) => {
            // Si llega aquí, es un 200 OK.
            Swal.fire('Anulada', 'La factura ha sido anulada.', 'success');
            this.loadFacturas();
          },
          error: (error) => {
            console.error('Error anulando factura:', error);
            const msg = error.error || error.message;
            Swal.fire('Error', 'Error al anular factura: ' + msg, 'error');
          }
        });
      }
    });
  }

  cancelFactura() {
    this.facturaForm = {
      clienteId: 0,
      fechaFactura: new Date().toISOString().split('T')[0],
      totalFactura: 0,
      detalles: []
    };
    this.showCreateForm = false;
    this.selectedFormaPagoId = 1;
    this.plazo = 0;
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
      error: (err) => {
        console.error('Error al descargar PDF', err);
        if (err.error instanceof Blob) {
          const reader = new FileReader();
          reader.onload = (e: any) => {
            alert('Error al descargar PDF: ' + e.target.result);
          };
          reader.readAsText(err.error);
        } else {
          alert('Error al descargar PDF: ' + (err.message || 'Error desconocido'));
        }
      }
    });
  }
}
