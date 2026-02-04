import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import Swal from 'sweetalert2';

interface Proveedor {
    id?: number;
    razonSocial: string;
    ruc: string;
    email: string;
    telefono: string;
    direccion: string;
}

@Component({
    selector: 'app-proveedores',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './proveedores.component.html',
    styleUrls: ['./proveedores.component.css']
})
export class ProveedoresComponent implements OnInit {
    proveedores: Proveedor[] = [];
    nuevoProveedor: Proveedor = { razonSocial: '', ruc: '', email: '', telefono: '', direccion: '' };
    editando: boolean = false;
    mostrarFormulario: boolean = false;
    mensaje: string = '';
    tipoMensaje: string = ''; // 'success' | 'error'

    private apiUrl = 'http://localhost:9090/api/proveedores';

    constructor(private http: HttpClient) { }

    ngOnInit(): void {
        this.cargarProveedores();
    }

    private getHeaders(): HttpHeaders {
        const token = localStorage.getItem('token');
        return new HttpHeaders({
            'Authorization': `Bearer ${token}`
        });
    }

    cargarProveedores(): void {
        this.http.get<Proveedor[]>(this.apiUrl, { headers: this.getHeaders() }).subscribe({
            next: (data) => this.proveedores = data,
            error: (e) => this.mostrarMensaje('Error al cargar proveedores', 'error')
        });
    }

    guardarProveedor(): void {
        if (!this.nuevoProveedor.razonSocial || !this.nuevoProveedor.ruc || !this.nuevoProveedor.email) {
            Swal.fire('Error', 'Razón Social, RUC y Email son obligatorios', 'warning');
            return;
        }

        const request = this.editando
            ? this.http.put<Proveedor>(`${this.apiUrl}/${this.nuevoProveedor.id}`, this.nuevoProveedor, { headers: this.getHeaders() })
            : this.http.post<Proveedor>(this.apiUrl, this.nuevoProveedor, { headers: this.getHeaders() });

        request.subscribe({
            next: () => {
                Swal.fire('Guardado', this.editando ? 'Proveedor actualizado' : 'Proveedor creado', 'success');
                this.cargarProveedores();
                this.cancelarEdicion();
            },
            error: (e) => Swal.fire('Error', 'Error al guardar proveedor', 'error')
        });
    }

    editarProveedor(p: Proveedor): void {
        this.nuevoProveedor = { ...p };
        this.editando = true;
        this.mostrarFormulario = true;
    }

    eliminarProveedor(id: number): void {
        if (!confirm('¿Está seguro de eliminar este proveedor?')) return;

        this.http.delete(`${this.apiUrl}/${id}`, { headers: this.getHeaders() }).subscribe({
            next: () => {
                this.mostrarMensaje('Proveedor eliminado', 'success');
                this.cargarProveedores();
            },
            error: (e) => this.mostrarMensaje('Error al eliminar', 'error')
        });
    }

    toggleFormulario(): void {
        this.mostrarFormulario = !this.mostrarFormulario;
        if (!this.mostrarFormulario) this.cancelarEdicion();
    }

    cancelarEdicion(): void {
        this.nuevoProveedor = { razonSocial: '', ruc: '', email: '', telefono: '', direccion: '' };
        this.editando = false;
        this.mostrarFormulario = false;
    }

    mostrarMensaje(msg: string, tipo: string): void {
        this.mensaje = msg;
        this.tipoMensaje = tipo;
        setTimeout(() => this.mensaje = '', 3000);
    }
}
