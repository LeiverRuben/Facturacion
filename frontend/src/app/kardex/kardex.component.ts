import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';

interface Kardex {
    id: number;
    fecha: string;
    tipoMovimiento: string; // ENTRADA, SALIDA
    detalle: string;
    cantidad: number;
    costoUnitario: number;
    totalMovimiento: number;
    saldoCantidad: number;
    saldoTotal: number;
    producto: {
        productoNombre: string;
        productoId: number;
    };
}

interface Producto {
    productoId: number;
    productoNombre: string;
}

@Component({
    selector: 'app-kardex',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './kardex.component.html',
    styleUrls: ['./kardex.component.css']
})
export class KardexComponent implements OnInit {

    kardex: Kardex[] = [];
    productos: Producto[] = [];

    // Filtros
    selectedProductoId: number = 0;
    fechaInicio: string = '';
    fechaFin: string = '';

    private apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) { }

    ngOnInit() {
        this.loadProductos();
        this.loadKardex();
    }

    loadProductos() {
        this.http.get<Producto[]>(`${this.apiUrl}/api/productos`).subscribe({
            next: (data) => this.productos = data,
            error: (err) => console.error('Error loading products', err)
        });
    }

    loadKardex() {
        let url = `${this.apiUrl}/api/kardex`;

        if (this.selectedProductoId && this.selectedProductoId > 0) {
            url = `${this.apiUrl}/api/kardex/producto/${this.selectedProductoId}`;
        }

        this.http.get<Kardex[]>(url).subscribe({
            next: (data) => {
                // Filtrado por Fechas en cliente (opcional si el backend no lo soporta aun)
                if (this.fechaInicio) {
                    data = data.filter(k => k.fecha >= this.fechaInicio);
                }
                if (this.fechaFin) {
                    // Añadir hora final al día
                    data = data.filter(k => k.fecha.split('T')[0] <= this.fechaFin);
                }
                // Ordenar descendente (más reciente primero)
                this.kardex = data.sort((a, b) => new Date(b.fecha).getTime() - new Date(a.fecha).getTime());
            },
            error: (err) => console.error('Error loading kardex', err)
        });
    }

    limpiarFiltros() {
        this.selectedProductoId = 0;
        this.fechaInicio = '';
        this.fechaFin = '';
        this.loadKardex();
    }

    sincronizar() {
        if (!confirm('¿Desea generar un historial inicial para productos antiguos sin movimientos?')) return;

        this.http.post(`${this.apiUrl}/api/kardex/sincronizar`, {}).subscribe({
            next: () => {
                alert('Sincronización completada.');
                this.loadKardex();
            },
            error: (err) => {
                console.error(err);
                alert('Error al sincronizar.');
            }
        });
    }
}
