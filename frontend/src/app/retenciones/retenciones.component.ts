import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import Swal from 'sweetalert2';

@Component({
    selector: 'app-retenciones',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './retenciones.component.html',
    styleUrls: ['./retenciones.component.css']
})
export class RetencionesComponent implements OnInit {

    retenciones: any[] = [];
    filtro: string = '';
    apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) { }

    ngOnInit() {
        this.cargarRetenciones();
    }

    cargarRetenciones() {
        this.http.get<any[]>(`${this.apiUrl}/api/retenciones`).subscribe({
            next: (data) => {
                this.retenciones = data.sort((a, b) => b.id - a.id);
            },
            error: (err) => console.error(err)
        });
    }

    get retencionesFiltradas() {
        if (!this.filtro) return this.retenciones;
        const term = this.filtro.toLowerCase();
        return this.retenciones.filter(r =>
            (r.secuencial && r.secuencial.includes(term)) ||
            (r.proveedor?.razonSocial && r.proveedor.razonSocial.toLowerCase().includes(term)) ||
            (r.proveedor?.ruc && r.proveedor.ruc.includes(term))
        );
    }

    enviarSri(id: number) {
        Swal.fire({
            title: 'Enviando al SRI...',
            text: 'Espere por favor, esto puede tomar unos segundos.',
            allowOutsideClick: false,
            didOpen: () => Swal.showLoading()
        });

        this.http.post(`${this.apiUrl}/api/retenciones/enviar-sri/${id}`, {}).subscribe({
            next: (res: any) => {
                this.cargarRetenciones();

                let estado = res.estadoSri || 'DESCONOCIDO';
                let mensaje = res.mensajeSri || 'Respuesta sin mensaje detallado.';

                // Mejorar parsing si viene todo junto
                if (mensaje.includes('Estado:')) {
                    const parts = mensaje.split('Estado:');
                    if (parts.length > 1) estado = parts[1].split('.')[0].trim();
                }

                Swal.fire({
                    icon: estado === 'AUTORIZADO' ? 'success' : (estado === 'DESCONOCIDO' ? 'warning' : 'error'),
                    title: `Estado: ${estado}`,
                    text: `El comprobante fue procesado. ${mensaje}`,
                    confirmButtonText: 'Entendido'
                });
            },
            error: (err) => {
                Swal.fire('Error', 'Error de conexión o firma: ' + (err.error?.message || err.message), 'error');
            }
        });
    }

    descargarPdf(id: number) {
        this.http.get(`${this.apiUrl}/api/retenciones/${id}/pdf`, { responseType: 'blob' }).subscribe({
            next: (blob) => {
                const url = window.URL.createObjectURL(blob);
                window.open(url, '_blank');
            },
            error: (err) => Swal.fire('Error', 'No se pudo descargar el PDF', 'error')
        });
    }

    verXml(id: number) {
        this.http.get(`${this.apiUrl}/api/retenciones/${id}/xml`, { responseType: 'text' }).subscribe({
            next: (xmlString) => {
                const isSigned = xmlString.includes('ds:Signature') || xmlString.includes('SignatureValue');

                Swal.fire({
                    title: '<strong>XML Firmado</strong>',
                    width: 800,
                    html: `
                        <div style="text-align: left; margin-bottom: 10px;">
                          ${isSigned
                            ? '<span class="badge badge-success" style="background-color: #28a745; color: white; padding: 5px 10px; border-radius: 4px;">✔ FIRMA ELECTRÓNICA PRESENTE</span>'
                            : '<span class="badge badge-danger" style="background-color: #dc3545; color: white; padding: 5px 10px; border-radius: 4px;">❌ SIN FIRMA</span>'}
                        </div>
                        <textarea readonly class="form-control" style="width: 100%; height: 300px; font-family: monospace; font-size: 12px; border: 1px solid #ddd; padding: 10px; background-color: #1e293b; color: #f8fafc;">${this.escapeHtml(xmlString)}</textarea>
                      `,
                    showCloseButton: true,
                    showCancelButton: true,
                    focusConfirm: false,
                    confirmButtonText: '<i class="bx bx-download"></i> Descargar',
                    confirmButtonAriaLabel: 'Descargar',
                    cancelButtonText: 'Cerrar',
                }).then((result) => {
                    if (result.isConfirmed) {
                        this.downloadString(xmlString, `retencion_${id}.xml`);
                    }
                });
            },
            error: (err) => Swal.fire('Error', 'No se pudo obtener el XML (quizás no existe aún)', 'error')
        });
    }

    escapeHtml(text: string): string {
        return text
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    downloadString(content: string, fileName: string) {
        const blob = new Blob([content], { type: 'text/xml' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = fileName;
        a.click();
        window.URL.revokeObjectURL(url);
    }



    toggleEstadoInterno(retencion: any) {
        const nuevoEstado = retencion.estadoInterno === 'PAGADO' ? 'PENDIENTE' : 'PAGADO';

        this.http.patch(`${this.apiUrl}/api/retenciones/${retencion.id}/estado-interno`, { estado: nuevoEstado }).subscribe({
            next: (res: any) => {
                retencion.estadoInterno = res.estadoInterno;
                const msg = nuevoEstado === 'PAGADO' ? 'Retención marcada como PAGADA' : 'Retención marcada como PENDIENTE';
                const icon = nuevoEstado === 'PAGADO' ? 'success' : 'info';

                // Toast notification instead of heavy alert
                Swal.fire({
                    toast: true,
                    position: 'top-end',
                    icon: icon,
                    title: msg,
                    showConfirmButton: false,
                    timer: 2000
                });
            },
            error: (err) => console.error(err)
        });
    }
}
