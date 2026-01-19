import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { environment } from '../../environments/environment';

interface Cliente {
  clienteId?: number;
  clienteNombre: string;
  clienteApellido: string;
  clienteEmail: string;
  clienteTelefono: string;
  clienteDireccion: string;
  clienteEstado: boolean;
}

@Component({
  selector: 'app-clientes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="clientes-container">
      
      <header class="page-header d-flex justify-content-between align-items-center">
        <div>
          <h2 class="page-title">Gestión de Clientes</h2>
          <p class="page-subtitle">Administra tu base de datos de clientes</p>
        </div>
        <button class="btn-action btn-primary" (click)="showCreateForm = !showCreateForm">
          <i class='bx bx-plus'></i> {{ showCreateForm ? 'Cancelar' : 'Nuevo Cliente' }}
        </button>
      </header>

      <!-- Formulario de creación/edición -->
      <div class="card-premium" *ngIf="showCreateForm">
        <h3 class="mb-4 font-bold text-lg text-slate-800">{{ editingCliente ? 'Editar Cliente' : 'Registrar Nuevo Cliente' }}</h3>
        
        <form (ngSubmit)="saveCliente()">
          <div class="form-grid">
            <div class="form-group">
              <label class="form-label">Nombre</label>
              <div class="input-group-modern"> <!-- Using input group for icons if desired, or just form-control -->
                <input type="text" class="form-control" [(ngModel)]="clienteForm.clienteNombre" name="clienteNombre" required placeholder="Ej. Juan">
              </div>
            </div>
            
            <div class="form-group">
              <label class="form-label">Apellido</label>
              <input type="text" class="form-control" [(ngModel)]="clienteForm.clienteApellido" name="clienteApellido" required placeholder="Ej. Pérez">
            </div>

            <div class="form-group">
              <label class="form-label">Email</label>
              <input type="email" class="form-control" [(ngModel)]="clienteForm.clienteEmail" name="clienteEmail" required placeholder="juan@ejemplo.com">
            </div>

            <div class="form-group">
              <label class="form-label">Teléfono</label>
              <input type="text" class="form-control" [(ngModel)]="clienteForm.clienteTelefono" name="clienteTelefono" placeholder="0999999999">
            </div>
          </div>

          <div class="form-group">
            <label class="form-label">Dirección</label>
            <textarea class="form-control" [(ngModel)]="clienteForm.clienteDireccion" name="clienteDireccion" rows="3" placeholder="Dirección completa"></textarea>
          </div>

          <div class="d-flex justify-content-end gap-2 mt-4">
             <button type="button" class="btn-action btn-secondary" (click)="cancelEdit()">Cancelar</button>
             <button type="submit" class="btn-action btn-primary">{{ editingCliente ? 'Actualizar Cliente' : 'Guardar Cliente' }}</button>
          </div>
        </form>
      </div>

      <!-- Lista de clientes -->
      <div class="table-container">
        <table class="table-modern">
          <thead>
            <tr>
              <th>ID</th>
              <th>Cliente</th>
              <th>Contacto</th>
              <th>Dirección</th>
              <th>Estado</th>
              <th>Acciones</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let cliente of clientes">
              <td>#{{ cliente.clienteId }}</td>
              <td>
                <div class="fw-bold text-slate-800">{{ cliente.clienteNombre }} {{ cliente.clienteApellido }}</div>
                <div class="small text-muted">{{ cliente.clienteEmail }}</div>
              </td>
              <td>{{ cliente.clienteTelefono || 'N/A' }}</td>
              <td><span class="text-truncate d-block" style="max-width: 200px;">{{ cliente.clienteDireccion || 'Sin dirección' }}</span></td>
              <td>
                <span class="badge" [class.badge-success]="cliente.clienteEstado" [class.badge-danger]="!cliente.clienteEstado">
                  {{ cliente.clienteEstado ? 'Activo' : 'Inactivo' }}
                </span>
              </td>
              <td>
                <div class="d-flex gap-2">
                  <button class="btn-action btn-secondary" (click)="editCliente(cliente)" title="Editar">
                    <i class='bx bx-edit'></i>
                  </button>
                  <button class="btn-action btn-danger" (click)="deleteCliente(cliente.clienteId!)" title="Eliminar">
                    <i class='bx bx-trash'></i>
                  </button>
                </div>
              </td>
            </tr>
            <tr *ngIf="clientes.length === 0">
              <td colspan="6" class="text-center py-5 text-muted">
                <i class='bx bx-user-x fs-1 mb-2'></i>
                <p>No hay clientes registrados.</p>
              </td>
            </tr>
          </tbody>
        </table>
      </div>

    </div>
  `,
  styles: [`
    .clientes-container {
      max-width: 1200px;
      margin: 0 auto;
    }
  `]
})
export class ClientesComponent implements OnInit {
  private apiUrl = environment.apiUrl;
  clientes: Cliente[] = [];
  showCreateForm = false;
  editingCliente = false;
  clienteForm: Cliente = {
    clienteNombre: '',
    clienteApellido: '',
    clienteEmail: '',
    clienteTelefono: '',
    clienteDireccion: '',
    clienteEstado: true
  };

  constructor(private http: HttpClient) { }

  ngOnInit() {
    this.loadClientes();
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

  saveCliente() {
    if (this.editingCliente && this.clienteForm.clienteId) {
      // Actualizar cliente existente
      this.http.put<Cliente>(`${this.apiUrl}/api/cliente/${this.clienteForm.clienteId}`, this.clienteForm).subscribe({
        next: () => {
          this.loadClientes();
          this.cancelEdit();
        },
        error: (error) => {
          console.error('Error updating client:', error);
        }
      });
    } else {
      // Crear nuevo cliente usando el endpoint simple
      this.http.post<Cliente>(`${this.apiUrl}/api/cliente/simple`, this.clienteForm).subscribe({
        next: () => {
          this.loadClientes();
          this.cancelEdit();
        },
        error: (error) => {
          console.error('Error creating client:', error);
        }
      });
    }
  }

  editCliente(cliente: Cliente) {
    this.clienteForm = { ...cliente };
    this.editingCliente = true;
    this.showCreateForm = true;
  }

  deleteCliente(id: number) {
    if (confirm('¿Está seguro de que desea eliminar este cliente?')) {
      this.http.delete(`${this.apiUrl}/api/cliente/${id}`).subscribe({
        next: () => {
          this.loadClientes();
        },
        error: (error) => {
          console.error('Error deleting client:', error);
        }
      });
    }
  }

  cancelEdit() {
    this.clienteForm = {
      clienteNombre: '',
      clienteApellido: '',
      clienteEmail: '',
      clienteTelefono: '',
      clienteDireccion: '',
      clienteEstado: true
    };
    this.editingCliente = false;
    this.showCreateForm = false;
  }
}
