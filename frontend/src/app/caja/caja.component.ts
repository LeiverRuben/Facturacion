import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { CommonModule } from '@angular/common'; // Importante
import { FormsModule } from '@angular/forms';     // Importante
import { environment } from '../../environments/environment';

@Component({
    selector: 'app-caja',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './caja.component.html',
    styleUrls: ['./caja.component.css']
})
export class CajaComponent implements OnInit {

    private apiUrl = environment.apiUrl;

    sesionActiva: any = null;
    resumen: any = null; // Guardará: saldoActual, totalIngresos, totalEgresos, totalVentas, movimientos
    historial: any[] = []; // Lista de sesiones pasadas

    // Paginación y Filtros
    page: number = 1;
    pageSize: number = 5;
    searchText: string = '';

    montoInicial: number = 0;
    montoCierre: number = 0;

    // Para registrar movimiento
    showMovimientoForm: boolean = false;
    movimientoData = {
        tipo: 'EGRESO', // o INGRESO
        monto: 0,
        descripcion: ''
    };

    mensaje: string = '';
    tipoMensaje: string = '';

    constructor(private http: HttpClient) { }

    ngOnInit() {
        this.verificarEstadoCaja();
        this.cargarHistorial();
    }

    get filteredHistorial() {
        // 1. Filtrar
        let filtered = this.historial;
        if (this.searchText) {
            const term = this.searchText.toLowerCase();
            filtered = filtered.filter(s =>
                (s.usuario?.nombre || '').toLowerCase().includes(term) ||
                (s.usuario?.username || '').toLowerCase().includes(term) ||
                (s.fechaApertura || '').includes(term)
            );
        }

        // 2. Paginar
        const start = (this.page - 1) * this.pageSize;
        return filtered.slice(start, start + this.pageSize);
    }

    get totalPages() {
        const total = this.historial.length; // Idealmente filtrar también aquí si la paginación dependiera del filtro total
        // Corrección: el total de páginas debe basarse en los items FILTRADOS, no el total absoluto, si queremos ser precisos.
        // Simularemos filtrado rápido:
        let count = this.historial.length;
        if (this.searchText) {
            const term = this.searchText.toLowerCase();
            count = this.historial.filter(s =>
                (s.usuario?.nombre || '').toLowerCase().includes(term) ||
                (s.usuario?.username || '').toLowerCase().includes(term) ||
                (s.fechaApertura || '').includes(term)
            ).length;
        }
        return Math.ceil(count / this.pageSize) || 1;
    }

    get totalAcumuladoVentas() {
        // Suma de ventas de TODAS las sesiones cerradas (filtradas o totales? Usuario pidió "total de las emisiones de cierre")
        // Asumiremos las filtradas para que sea dinámico, o todo el historial.
        // "Para que no parezca información perdida... poner un total" -> Suena a total global.
        // Haremos total global de lo cargado.
        return this.historial
            .filter(s => s.estado === 'CERRADA')
            .reduce((sum, s) => sum + (s.totalVentasEfectivo || 0), 0);
    }

    nextPage() {
        if (this.page < this.totalPages) this.page++;
    }

    prevPage() {
        if (this.page > 1) this.page--;
    }

    cargarHistorial() {
        this.http.get(`${this.apiUrl}/api/caja/historial`).subscribe({
            next: (res: any) => {
                this.historial = res;
            },
            error: (err) => {
                console.error('Error cargando historial', err);
            }
        });
    }

    verificarEstadoCaja() {
        this.http.get(`${this.apiUrl}/api/caja/estado`).subscribe({
            next: (res: any) => {
                if (res && res.estado === 'ABIERTA') {
                    this.sesionActiva = res;
                    this.cargarResumen(res.id); // Cargar detalle completo
                } else {
                    this.sesionActiva = null;
                    this.resumen = null;
                }
            },
            error: (err) => {
                console.error('Error al verificar caja', err);
                this.mostrarMensaje('Error al conectar con el servidor de caja.', 'error');
            }
        });
    }

    cargarResumen(sesionId: number) {
        this.http.get(`${this.apiUrl}/api/caja/resumen/${sesionId}`).subscribe({
            next: (res: any) => {
                this.resumen = res;
            },
            error: (err) => {
                console.error('Error al cargar resumen', err);
            }
        });
    }

    abrirCaja() {
        if (this.montoInicial < 0) {
            this.mostrarMensaje('El monto inicial no puede ser negativo.', 'error');
            return;
        }

        const payload = { montoInicial: this.montoInicial };

        this.http.post(`${this.apiUrl}/api/caja/abrir`, payload).subscribe({
            next: (res: any) => {
                this.mostrarMensaje('Caja abierta correctamente.', 'success');
                this.sesionActiva = res; // Actualiza estado
                this.cargarResumen(res.id); // Inicia resumen
            },
            error: (err) => {
                this.mostrarMensaje('Error al abrir caja: ' + (err.error || err.message), 'error');
            }
        });
    }

    registrarMovimiento() {
        if (this.movimientoData.monto <= 0 || !this.movimientoData.descripcion) {
            this.mostrarMensaje('Monto y descripción son obligatorios.', 'error');
            return;
        }

        const payload = {
            sesionId: this.sesionActiva.id,
            ...this.movimientoData
        };

        this.http.post(`${this.apiUrl}/api/caja/movimiento`, payload).subscribe({
            next: (res: any) => {
                this.mostrarMensaje('Movimiento registrado.', 'success');
                this.showMovimientoForm = false;
                this.movimientoData = { tipo: 'EGRESO', monto: 0, descripcion: '' }; // Reset
                this.cargarResumen(this.sesionActiva.id); // Recargar saldos
            },
            error: (err) => {
                this.mostrarMensaje('Error al registrar movimiento: ' + (err.error || err.message), 'error');
            }
        });
    }

    toggleMovimientoForm(tipo: string) {
        this.movimientoData.tipo = tipo;
        this.showMovimientoForm = !this.showMovimientoForm;
    }

    cerrarCaja() {
        if (!this.sesionActiva) return;

        // Ahora usamos el totalVentas REAL que nos da el backend en el resumen
        const totalVentasReal = this.resumen ? this.resumen.totalVentas : 0;

        const payload = {
            sesionId: this.sesionActiva.id,
            montoFinal: this.montoCierre,
            totalVentas: totalVentasReal // Enviamos el dato correcto
        };

        this.http.post(`${this.apiUrl}/api/caja/cerrar`, payload).subscribe({
            next: (res: any) => {
                this.mostrarMensaje('Caja cerrada con éxito.', 'success');
                this.sesionActiva = null;
                this.resumen = null;
                this.montoInicial = 0;
                this.montoCierre = 0;
                this.cargarHistorial(); // Recargar historial
            },
            error: (err) => {
                this.mostrarMensaje('Error al cerrar caja: ' + (err.error || err.message), 'error');
            }
        });
    }

    eliminarSesion(sesionId: number) {
        if (!confirm('¿Estás seguro de eliminar este registro de caja? Esta acción no se puede deshacer.')) return;

        this.http.delete(`${this.apiUrl}/api/caja/eliminar/${sesionId}`).subscribe({
            next: (res: any) => {
                this.mostrarMensaje('Registro eliminado correctamente.', 'success');
                this.cargarHistorial();
            },
            error: (err) => {
                // Manejo de error si el backend envía un map con "error"
                const errorMsg = err.error?.error || err.error || err.message;
                this.mostrarMensaje('No se pudo eliminar: ' + errorMsg, 'error');
            }
        });
    }

    mostrarMensaje(msg: string, tipo: string) {
        this.mensaje = msg;
        this.tipoMensaje = tipo;
        window.scrollTo({ top: 0, behavior: 'smooth' }); // Asegurar que el usuario vea el mensaje
        setTimeout(() => {
            this.mensaje = '';
            this.tipoMensaje = '';
        }, 5000);
    }

    validarInputNumerico(event: any) {
        const pattern = /[0-9.]/;
        const inputChar = String.fromCharCode(event.charCode);

        if (!pattern.test(inputChar)) {
            // Caracter inválido
            event.preventDefault();
            return;
        }

        // Evitar múltiples puntos decimales
        const currentText = event.target.value;
        if (inputChar === '.' && currentText.includes('.')) {
            event.preventDefault();
        }
    }
}
