import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { environment } from '../../environments/environment';

interface Producto {
  productoId?: number;
  productoNombre: string;
  productoDescripcion: string;
  productoPrecio: number;
  productoStock: number;
  productoEstado: boolean;
  categoriaId: number;
  categoria?: Categoria; // Estructura anidada del backend
}

interface Categoria {
  categoriaId: number;
  categoriaNombre: string;
  categoriaDescripcion?: string;
}

@Component({
  selector: 'app-productos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="productos-container">
      
      <header class="page-header d-flex justify-content-between align-items-center">
        <div>
          <h2 class="page-title">Inventario de Productos</h2>
          <p class="page-subtitle">Gestiona tu catálogo, precios y stock</p>
        </div>
        <button class="btn-action btn-primary" (click)="showCreateForm = !showCreateForm">
          <i class='bx bx-plus'></i> {{ showCreateForm ? 'Cancelar' : 'Nuevo Producto' }}
        </button>
      </header>

      <!-- Formulario de creación/edición -->
      <div class="card-premium" *ngIf="showCreateForm">
        <h3 class="mb-4 fw-bold text-dark">{{ editingProducto ? 'Editar Producto' : 'Registrar Nuevo Producto' }}</h3>
        
        <form (ngSubmit)="saveProducto()">
          <div class="form-grid">
            <div class="form-group">
              <label class="form-label">Nombre del Producto</label>
              <input type="text" class="form-control" [(ngModel)]="productoForm.productoNombre" name="productoNombre" required placeholder="Ej. Laptop Gaming">
            </div>

            <div class="form-group">
              <label class="form-label">Categoría</label>
              <div class="d-flex gap-2">
                 <ng-container *ngIf="!showNewCategoriaInput">
                    <select class="form-control" [(ngModel)]="productoForm.categoriaId" name="categoriaId" required>
                      <option [ngValue]="0">Seleccionar categoría...</option>
                      <option *ngFor="let categoria of categorias" [ngValue]="categoria.categoriaId">
                        {{ categoria.categoriaNombre }}
                      </option>
                    </select>
                    <button type="button" class="btn-action btn-success" (click)="toggleNewCategoria()" title="Nueva Categoría" style="background:#10b981; color:white;">
                      <i class='bx bx-plus'></i>
                    </button>
                 </ng-container>

                 <ng-container *ngIf="showNewCategoriaInput">
                    <input type="text" class="form-control" [(ngModel)]="newCategoriaNombre" name="newCategoriaNombre" placeholder="Nombre nueva categoría">
                    <button type="button" class="btn-action btn-success" (click)="saveNewCategoria()" title="Guardar">
                      <i class='bx bx-check'></i>
                    </button>
                    <button type="button" class="btn-action btn-danger" (click)="toggleNewCategoria()" title="Cancelar">
                      <i class='bx bx-x'></i>
                    </button>
                 </ng-container>
              </div>
            </div>
            
            <div class="form-group">
              <label class="form-label">Precio ($)</label>
              <input type="number" class="form-control" [(ngModel)]="productoForm.productoPrecio" name="productoPrecio" step="0.01" required (keypress)="validateNumber($event)" min="0">
            </div>

            <div class="form-group">
              <label class="form-label">Stock Disponible</label>
              <input type="number" class="form-control" [(ngModel)]="productoForm.productoStock" name="productoStock" required (keypress)="validateNumber($event)" min="0">
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Descripción</label>
            <textarea class="form-control" [(ngModel)]="productoForm.productoDescripcion" name="productoDescripcion" rows="3" placeholder="Detalles del producto..."></textarea>
          </div>

          <div class="d-flex justify-content-end gap-2 mt-4">
            <button type="button" class="btn-action btn-secondary" (click)="cancelEdit()">Cancelar</button>
            <button type="submit" class="btn-action btn-primary">{{ editingProducto ? 'Actualizar Producto' : 'Guardar Producto' }}</button>
          </div>
        </form>
      </div>

      <!-- Lista de productos -->
      <div class="table-container">
        <table class="table-modern">
          <thead>
            <tr>
              <th>ID</th>
              <th>Producto</th>
              <th>Categoría</th>
              <th>Precio</th>
              <th>Stock</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let producto of productos">
              <td>#{{ producto.productoId }}</td>
              <td>
                <div class="fw-bold text-dark">{{ producto.productoNombre }}</div>
                <div class="small text-muted text-truncate" style="max-width: 250px;">{{ producto.productoDescripcion }}</div>
              </td>
              <td>
                <span class="badge badge-info">{{ producto.categoria?.categoriaNombre || 'Sin Categoría' }}</span>
              </td>
              <td class="fw-bold text-dark">{{ producto.productoPrecio | currency:'USD':'symbol':'1.2-2' }}</td>
              <td>
                <span [class.text-danger]="producto.productoStock < 5" [class.fw-bold]="producto.productoStock < 5">
                  {{ producto.productoStock }} unid.
                </span>
              </td>
              <td>
                <span class="badge" [class.badge-success]="producto.productoEstado" [class.badge-danger]="!producto.productoEstado">
                  {{ producto.productoEstado ? 'Activo' : 'Inactivo' }}
                </span>
              </td>
              <td>
                <div class="d-flex gap-2">
                  <button class="btn-action btn-secondary" (click)="editProducto(producto)" title="Editar">
                    <i class='bx bx-edit'></i>
                  </button>
                  <button class="btn-action btn-danger" (click)="deleteProducto(producto.productoId!)" title="Eliminar">
                    <i class='bx bx-trash'></i>
                  </button>
                </div>
              </td>
            </tr>
            <tr *ngIf="productos.length === 0">
              <td colspan="7" class="text-center py-5 text-muted">
                <i class='bx bx-package fs-1 mb-2'></i>
                <p>No tienes productos en tu inventario.</p>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

    </div>
  `,
  styles: [`
    .productos-container {
      max-width: 1200px;
      margin: 0 auto;
    }
  `]
})
export class ProductosComponent implements OnInit {
  private apiUrl = environment.apiUrl;
  productos: Producto[] = [];
  categorias: Categoria[] = [];
  showCreateForm = false;
  editingProducto = false;
  showNewCategoriaInput = false;
  newCategoriaNombre = '';
  productoForm: Producto = {
    productoNombre: '',
    productoDescripcion: '',
    productoPrecio: 0,
    productoStock: 0,
    productoEstado: true,
    categoriaId: 0
  };

  constructor(private http: HttpClient) { }

  ngOnInit() {
    this.loadProductos();
    this.loadCategorias();
  }

  loadProductos() {
    this.http.get<Producto[]>(`${this.apiUrl}/api/productos`).subscribe({
      next: (data) => {
        console.log('Productos cargados:', data); // DEBUG
        this.productos = data;
      },
      error: (error) => {
        console.error('Error loading products:', error);
      }
    });
  }

  loadCategorias() {
    this.http.get<Categoria[]>(`${this.apiUrl}/api/categoria`).subscribe({
      next: (data) => {
        this.categorias = data;
      },
      error: (error) => {
        console.error('Error loading categories:', error);
      }
    });
  }

  toggleNewCategoria() {
    this.showNewCategoriaInput = !this.showNewCategoriaInput;
    this.newCategoriaNombre = '';
  }

  saveNewCategoria() {
    if (!this.newCategoriaNombre.trim()) return;
    const newCategoria: Partial<Categoria> = {
      categoriaNombre: this.newCategoriaNombre,
      // Agregar descripción por si acaso la BD la requiere
      categoriaDescripcion: 'Categoría creada desde el sistema'
    };
    this.http.post<Categoria>(`${this.apiUrl}/api/categoria`, newCategoria).subscribe({
      next: (categoria) => {
        this.loadCategorias();
        this.productoForm.categoriaId = categoria.categoriaId;
        this.toggleNewCategoria();
      },
      error: (error) => {
        console.error('Error creating category:', error);
        alert('Error al crear la categoría');
      }
    });
  }

  saveProducto() {
    // Preparar el objeto explícitamente para asegurar que se envía lo correcto
    const payload: any = {
      productoNombre: this.productoForm.productoNombre,
      productoDescripcion: this.productoForm.productoDescripcion,
      productoPrecio: this.productoForm.productoPrecio,
      productoStock: this.productoForm.productoStock,
      productoEstado: this.productoForm.productoEstado,
      categoria: (this.productoForm.categoriaId && this.productoForm.categoriaId > 0)
        ? { categoriaId: this.productoForm.categoriaId }
        : null
    };

    console.log('Enviando payload explícito:', payload);
    console.log('Enviando payload al backend:', payload); // DEBUG PAYLOAD

    if (this.editingProducto && this.productoForm.productoId) {
      // Actualizar producto existente
      this.http.put<Producto>(`${this.apiUrl}/api/productos/${this.productoForm.productoId}`, payload).subscribe({
        next: () => {
          this.loadProductos();
          this.cancelEdit();
        },
        error: (error) => {
          console.error('Error updating product:', error);
          alert('Error al actualizar producto. Revisa la consola.');
        }
      });
    } else {
      // Crear nuevo producto
      this.http.post<Producto>(`${this.apiUrl}/api/productos`, payload).subscribe({
        next: () => {
          this.loadProductos();
          this.cancelEdit();
        },
        error: (error) => {
          console.error('Error creating product:', error);
          alert('Error al guardar producto. Revisa la consola.');
        }
      });
    }
  }

  editProducto(producto: Producto) {
    this.productoForm = {
      ...producto,
      // Aseguramos que categoriaId tenga un valor, ya que ...producto lo elimina si no viene del backend
      categoriaId: producto.categoria?.categoriaId || 0
    };
    this.editingProducto = true;
    this.showCreateForm = true;
  }

  deleteProducto(id: number) {
    if (confirm('¿Está seguro de que desea eliminar este producto?')) {
      this.http.delete(`${this.apiUrl}/api/productos/${id}`).subscribe({
        next: () => {
          this.loadProductos();
        },
        error: (error) => {
          console.error('Error deleting product:', error);
        }
      });
    }
  }

  validateNumber(event: KeyboardEvent) {
    const charCode = (event.which) ? event.which : event.keyCode;
    // Permitir solo números (48-57) y el punto (46)
    if (charCode > 31 && (charCode < 48 || charCode > 57) && charCode !== 46) {
      event.preventDefault();
    }
  }

  cancelEdit() {
    this.productoForm = {
      productoNombre: '',
      productoDescripcion: '',
      productoPrecio: 0,
      productoStock: 0,
      productoEstado: true,
      categoriaId: 0
    };
    this.editingProducto = false;
    this.showCreateForm = false;
  }
}
