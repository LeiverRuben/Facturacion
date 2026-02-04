import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { environment } from '../../environments/environment';
import Swal from 'sweetalert2';

interface Proveedor {
    id: number;
    razonSocial: string;
    ruc: string;
}

interface Producto {
    productoId: number;
    productoNombre: string;
    productoCodigo: string;
    productoPrecio: number; // Precio venta ref
}

interface DetalleCompra {
    productoId: number;
    productoNombre: string;
    cantidad: number;
    costoUnitario: number;
    subtotal: number;
}

@Component({
    selector: 'app-compras',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './compras.component.html',
    styleUrls: ['./compras.component.css']
})
export class ComprasComponent implements OnInit {
    // Datos Maestros
    proveedores: Proveedor[] = [];
    productos: Producto[] = [];

    // Formulario Cabecera
    compra = {
        proveedorId: null as number | null,
        numeroComprobante: '',
        fechaEmision: new Date().toISOString().split('T')[0],
        observacion: ''
    };

    // Formulario Detalle
    itemSeleccionado = {
        productoId: null as number | null,
        cantidad: 1,
        costoUnitario: 0
    };

    detalles: DetalleCompra[] = [];

    // Totales
    subtotal: number = 0;
    iva: number = 0;
    total: number = 0;

    // Retención State
    showRetencionModal: boolean = false;
    compraParaRetencion: any = null;
    detallesRetencion: any[] = [];
    retencionItem = {
        codigo: '1', // 1=Renta, 2=IVA
        codigoRetencion: '', // 312, etc
        baseImponible: 0,
        porcentajeRetener: 0
    };

    mensaje: string = '';
    tipoMensaje: string = '';

    // Listado
    comprasRegistradas: any[] = [];
    mostrarFormulario: boolean = false;

    // Filtro
    filtro: string = '';

    private apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) { }

    ngOnInit(): void {
        this.cargarProveedores();
        this.cargarProductos();
        this.listarCompras();
    }

    listarCompras() {
        this.http.get<any[]>(`${this.apiUrl}/api/compras`).subscribe(data => {
            this.comprasRegistradas = data;
            // Ordenar por fecha descendente (más reciente primero)
            this.comprasRegistradas.sort((a, b) => new Date(b.fechaEmision).getTime() - new Date(a.fechaEmision).getTime());
        });
    }

    get comprasFiltradas() {
        if (!this.filtro) return this.comprasRegistradas;
        const busqueda = this.filtro.toLowerCase();
        return this.comprasRegistradas.filter(c =>
            (c.numeroComprobante && c.numeroComprobante.toLowerCase().includes(busqueda)) ||
            (c.proveedor && c.proveedor.razonSocial && c.proveedor.razonSocial.toLowerCase().includes(busqueda))
        );
    }

    toggleVista() {
        this.mostrarFormulario = !this.mostrarFormulario;
        if (!this.mostrarFormulario) {
            this.listarCompras();
        }
    }

    // getHeaders eliminado para usar Interceptor

    cargarProveedores() {
        this.http.get<Proveedor[]>(`${this.apiUrl}/api/proveedores`)
            .subscribe(data => this.proveedores = data);
    }

    cargarProductos() {
        this.http.get<Producto[]>(`${this.apiUrl}/api/productos`)
            .subscribe(data => this.productos = data);
    }

    agregarProducto() {
        if (!this.itemSeleccionado.productoId || this.itemSeleccionado.cantidad <= 0 || this.itemSeleccionado.costoUnitario < 0) {
            this.mostrarMensaje('Seleccione un producto y valores válidos', 'error');
            return;
        }

        const prod = this.productos.find(p => p.productoId == this.itemSeleccionado.productoId);
        if (!prod) return;

        const subtotal = this.itemSeleccionado.cantidad * this.itemSeleccionado.costoUnitario;

        this.detalles.push({
            productoId: prod.productoId,
            productoNombre: prod.productoNombre,
            cantidad: this.itemSeleccionado.cantidad,
            costoUnitario: this.itemSeleccionado.costoUnitario,
            subtotal: subtotal
        });

        this.calcularTotales();
        this.limpiarItem();
    }

    eliminarDetalle(index: number) {
        this.detalles.splice(index, 1);
        this.calcularTotales();
    }

    calcularTotales() {
        this.subtotal = this.detalles.reduce((acc, el) => acc + el.subtotal, 0);
        this.iva = this.subtotal * 0.15; // Parametrizable futuro
        this.total = this.subtotal + this.iva;
    }

    limpiarItem() {
        this.itemSeleccionado = { productoId: null, cantidad: 1, costoUnitario: 0 };
    }

    guardarCompra() {
        if (!this.compra.proveedorId || !this.compra.numeroComprobante || typeof this.compra.proveedorId !== 'number') {
            Swal.fire('Atención', 'Seleccione un proveedor y numero de comprobante', 'warning');
            return;
        }
        if (this.detalles.length === 0) {
            Swal.fire('Atención', 'Agregue al menos un producto', 'warning');
            return;
        }

        // Validar token básica (aunque el interceptor lo maneja, es bueno para UX inmediata)
        if (!localStorage.getItem('token')) {
            this.mostrarMensaje('No hay sesión activa. Por favor inicie sesión nuevamente.', 'error');
            return;
        }

        const payload = {
            proveedor: { id: this.compra.proveedorId },
            numeroComprobante: this.compra.numeroComprobante,
            fechaEmision: this.compra.fechaEmision + 'T00:00:00',
            subtotal: this.subtotal,
            totalIva: this.iva,
            total: this.total,
            estado: 'RECIBIDA',
            detalles: this.detalles.map(d => ({
                producto: { productoId: d.productoId },
                cantidad: d.cantidad,
                costoUnitario: d.costoUnitario,
                subtotal: d.subtotal
            }))
        };

        // Eliminamos { headers: ... } para que el Interceptor actúe
        this.http.post(`${this.apiUrl}/api/compras`, payload).subscribe({
            next: (resp) => {
                this.mostrarMensaje('Compra registrada y Stock actualizado', 'success');
                this.limpiarFormulario();
                this.toggleVista(); // Volver al listado
            },
            error: (err) => {
                console.error(err);
                if (err.status === 403) {
                    this.mostrarMensaje('Acceso Denegado (403). Verifique permisos o inicie sesión.', 'error');
                } else {
                    this.mostrarMensaje(`Error al guardar: ${err.message}`, 'error');
                }
            }
        });
    }

    limpiarFormulario() {
        this.compra = {
            proveedorId: null,
            numeroComprobante: '',
            fechaEmision: new Date().toISOString().split('T')[0],
            observacion: ''
        };
        this.detalles = [];
        this.calcularTotales();
    }

    mostrarMensaje(msg: string, tipo: string) {
        this.mensaje = msg;
        this.tipoMensaje = tipo;
        setTimeout(() => this.mensaje = '', 4000);
    }

    // Validación numérica
    validateNumber(event: any): void {
        const charCode = (event.which) ? event.which : event.keyCode;
        if (charCode > 31 && (charCode < 48 || charCode > 57) && charCode !== 46) {
            event.preventDefault();
        }
    }

    // --- Lógica de Retenciones ---

    abrirModalRetencion(compra: any) {
        this.compraParaRetencion = compra;
        // Pre-cargar detalle por defecto (ej: Renta sobre subtotal)
        this.detallesRetencion = [];
        this.retencionItem = {
            codigo: '1',
            codigoRetencion: '312',
            baseImponible: compra.subtotal,
            porcentajeRetener: 1.75
        };
        this.showRetencionModal = true;
    }

    agregarDetalleRetencion() {
        if (!this.retencionItem.codigoRetencion || this.retencionItem.baseImponible <= 0 || this.retencionItem.porcentajeRetener <= 0) {
            this.mostrarMensaje('Datos de retención inválidos', 'error');
            return;
        }
        this.detallesRetencion.push({ ...this.retencionItem });
        // Limpiar para otro
        this.retencionItem = {
            codigo: this.retencionItem.codigo, // Mantener tipo
            codigoRetencion: '',
            baseImponible: 0,
            porcentajeRetener: 0
        };
    }

    eliminarDetalleRetencion(index: number) {
        this.detallesRetencion.splice(index, 1);
    }

    generarRetencion() {
        if (!this.compraParaRetencion || this.detallesRetencion.length === 0) {
            this.mostrarMensaje('Agregue al menos un impuesto a retener', 'error');
            return;
        }

        const payload = {
            compraId: this.compraParaRetencion.id,
            detalles: this.detallesRetencion
        };

        this.http.post(`${this.apiUrl}/api/retenciones/generar`, payload).subscribe({
            next: (res: any) => {
                this.mostrarMensaje('Retención generada con éxito. Secuencial: ' + res.secuencial, 'success');
                // Auto-descargar PDF al generar
                if (res.id) {
                    this.descargarPdfRetencion(res.id);
                }
                this.showRetencionModal = false;
                this.compraParaRetencion = null;
                this.detallesRetencion = [];
            },
            error: (err) => {
                this.mostrarMensaje('Error al generar retención: ' + (err.error || err.message), 'error');
            }
        });
    }

    descargarPdfRetencion(retencionId: number) {
        // Solicitamos el PDF como Blob
        this.http.get(`${this.apiUrl}/api/retenciones/${retencionId}/pdf`, { responseType: 'blob' }).subscribe({
            next: (blob) => {
                const url = window.URL.createObjectURL(blob);
                window.open(url, '_blank'); // Abrir en nueva pestaña para imprimir
            },
            error: (err) => console.error('Error al descargar PDF', err)
        });
    }
}
