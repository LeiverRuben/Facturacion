import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import Swal from 'sweetalert2';

@Component({
    selector: 'app-empresa',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './empresa.component.html',
    styleUrls: ['./empresa.component.css']
})
export class EmpresaComponent implements OnInit {

    mostrarClave: boolean = false;
    empresa: any = {
        razonSocial: '',
        nombreComercial: '',
        ruc: '',
        dirMatriz: '',
        dirEstablecimiento: '',
        establecimiento: '001',
        puntoEmision: '001',
        obligadoContabilidad: 'NO',
        ambiente: 1,
        rutaFirma: '',
        claveFirma: '',
        correoRemitente: '',
        claveCorreo: ''
    };

    private apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) { }

    ngOnInit() {
        this.cargarEmpresa();
    }

    cargarEmpresa() {
        this.http.get<any>(`${this.apiUrl}/api/empresa`).subscribe({
            next: (data) => {
                this.empresa = data;
                // Limpiar valores por defecto para mostrar placeholders
                if (this.empresa.rutaFirma === 'PENDIENTE') this.empresa.rutaFirma = '';
                if (this.empresa.claveFirma === 'PENDIENTE') this.empresa.claveFirma = '';
                if (this.empresa.razonSocial === 'Mi Empresa S.A.') this.empresa.razonSocial = '';
                if (this.empresa.ruc === '9999999999001') this.empresa.ruc = '';
                if (this.empresa.dirMatriz === 'Av. Principal 123') this.empresa.dirMatriz = '';
            },
            error: (err) => console.error(err)
        });
    }

    onSubmit() {
        if (!this.empresa.razonSocial || !this.empresa.ruc || !this.empresa.dirMatriz || !this.empresa.dirEstablecimiento) {
            Swal.fire({
                icon: 'warning',
                title: 'Faltan Datos',
                text: 'Por favor complete Razón Social, RUC, Dirección Matriz y Dirección Establecimiento.'
            });
            return;
        }

        this.http.put(`${this.apiUrl}/api/empresa`, this.empresa).subscribe({
            next: (res) => {
                Swal.fire('Guardado', 'Datos de empresa actualizados correctamente.', 'success');
            },
            error: (err) => {
                Swal.fire('Error', 'No se pudieron guardar los cambio.', 'error');
            }
        });
    }

    testFirma() {
        if (!this.empresa.rutaFirma || !this.empresa.claveFirma) {
            Swal.fire('Atención', 'Ingrese ruta y contraseña primero.', 'warning');
            return;
        }

        Swal.fire({
            title: 'Verificando...',
            text: 'Leyendo certificado...',
            didOpen: () => Swal.showLoading()
        });

        this.http.post(`${this.apiUrl}/api/empresa/test-firma`, this.empresa, { responseType: 'text' }).subscribe({
            next: (res) => {
                Swal.fire({
                    title: 'Firma Válida',
                    html: `<pre style="text-align:left; font-size: 0.85rem;">${res}</pre>`,
                    icon: 'success'
                });
            },
            error: (err) => {
                const errorBody = typeof err.error === 'string' ? err.error : (err.message || 'Error desconocido');
                Swal.fire('Error de Firma', errorBody, 'error');
            }
        });
    }
}
