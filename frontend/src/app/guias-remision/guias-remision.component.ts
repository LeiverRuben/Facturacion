import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { GuiaRemisionService } from '../services/guia-remision.service';
import { Router } from '@angular/router';
import Swal from 'sweetalert2';

@Component({
    selector: 'app-guias-remision',
    standalone: true,
    imports: [CommonModule, FormsModule, RouterModule],
    template: `
    <div class="page-header slide-in-bottom">
        <div>
            <h1 class="page-title"><i class='bx bxs-truck'></i> Guías de Remisión</h1>
            <p class="page-subtitle">Gestiona el transporte de tus mercancías.</p>
        </div>
        <div class="d-flex gap-2">
            <button class="btn-action btn-primary" routerLink="/guias-remision/crear">
                <i class='bx bx-plus'></i> Nueva Guía
            </button>
        </div>
    </div>

    <div class="card-premium slide-in-bottom">
        <div class="card-header d-flex justify-content-between align-items-center">
            <h3 class="card-title">Listado de Guías</h3>
            <div class="input-icon-wrapper" style="width: 300px;">
                <i class='bx bx-search'></i>
                <input type="text" class="form-control" placeholder="Buscar..." [(ngModel)]="filtro" (ngModelChange)="filtrar()">
            </div>
        </div>

        <div class="card-body">
            <div class="table-container">
                <table class="table-modern">
                    <thead>
                        <tr>
                            <th>Secuencial</th>
                            <th>Fecha</th>
                            <th>Transportista</th>
                            <th>Destinatarios</th>
                            <th>Estado SRI</th>
                            <th class="text-center">Acciones</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr *ngFor="let g of guiasFiltradas">
                            <td><span class="badge-pill badge-neutral">{{ g.secuencial }}</span></td>
                            <td>
                                <div class="d-flex flex-column">
                                    <span class="fw-bold">{{ g.fechaEmision | date:'dd/MM/yyyy' }}</span>
                                    <small class="text-muted">{{ g.fechaEmision | date:'HH:mm' }}</small>
                                </div>
                            </td>
                            <td>
                                <div class="fw-bold">{{ g.transportistaRazonSocial }}</div>
                                <small class="text-muted">{{ g.placa }}</small>
                            </td>
                             <td>
                                <div *ngIf="g.destinatarios && g.destinatarios.length > 0">
                                    <span class="fw-bold">{{ g.destinatarios[0].razonSocialDestinatario }}</span>
                                    <span *ngIf="g.destinatarios.length > 1" class="badge-pill badge-neutral ms-1">+{{ g.destinatarios.length - 1 }}</span>
                                </div>
                            </td>
                            <td>
                                <span class="badge-pill" [ngClass]="{
                                    'badge-success': g.estadoSri === 'AUTORIZADO',
                                    'badge-warning': g.estadoSri === 'PENDIENTE' || g.estadoSri === 'EN_PROCESO',
                                    'badge-danger': g.estadoSri === 'DEVUELTA' || g.estadoSri === 'NO AUTORIZADO'
                                }">
                                    {{ g.estadoSri || 'PENDIENTE' }}
                                </span>
                            </td>
                            <td class="text-center">
                                <div class="d-flex justify-content-center gap-2">
                                    <button class="btn-icon" (click)="descargarPdf(g.id)" title="PDF RIDE">
                                        <i class='bx bxs-file-pdf text-danger'></i>
                                    </button>
                                    
                                    <button *ngIf="g.estadoSri !== 'AUTORIZADO'" class="btn-icon" (click)="enviarSri(g.id)" title="Enviar al SRI">
                                        <i class='bx bx-send text-primary'></i>
                                    </button>
                                </div>
                            </td>
                        </tr>
                        <tr *ngIf="guiasFiltradas.length === 0">
                            <td colspan="6" class="text-center py-5 text-muted">
                                <i class='bx bxs-truck fs-1 mb-2'></i>
                                <p>No se encontraron guías.</p>
                            </td>
                        </tr>
                    </tbody>
                </table>
            </div>
        </div>
    </div>
  `,
    styles: [`
    /* Reutilizando estilos globales de la plantilla */
    .badge-pill { padding: 0.35em 0.8em; border-radius: 50rem; font-size: 0.75em; font-weight: 600; }
    .badge-neutral { background-color: #f3f4f6; color: #4b5563; }
    .badge-success { background-color: #d1fae5; color: #065f46; }
    .badge-warning { background-color: #fef3c7; color: #92400e; }
    .badge-danger { background-color: #fee2e2; color: #b91c1c; }
  `]
})
export class GuiaRemisionComponent implements OnInit {
    guias: any[] = [];
    guiasFiltradas: any[] = [];
    filtro: string = '';

    constructor(private guiaService: GuiaRemisionService, private router: Router) { }

    ngOnInit(): void {
        this.cargarGuias();
    }

    cargarGuias() {
        this.guiaService.listar().subscribe({
            next: (res) => {
                this.guias = res;
                this.filtrar();
            },
            error: (err) => console.error(err)
        });
    }

    filtrar() {
        if (!this.filtro) {
            this.guiasFiltradas = this.guias;
        } else {
            const term = this.filtro.toLowerCase();
            this.guiasFiltradas = this.guias.filter(g =>
                g.secuencial.includes(term) ||
                g.transportistaRazonSocial?.toLowerCase().includes(term) ||
                g.placa?.toLowerCase().includes(term)
            );
        }
    }

    enviarSri(id: number) {
        Swal.fire({
            title: 'Enviando...',
            text: 'Conectando con el SRI',
            didOpen: () => Swal.showLoading()
        });

        this.guiaService.enviarSri(id).subscribe({
            next: (res) => {
                Swal.close();
                this.cargarGuias();
                if (res.estadoSri === 'AUTORIZADO') {
                    Swal.fire('Autorizado', 'Guía de remisión autorizada correctamente.', 'success');
                } else if (res.estadoSri === 'EN_PROCESO') {
                    Swal.fire('En Proceso', 'El SRI está procesando el comprobante. Verifique en unos minutos.', 'warning');
                } else {
                    Swal.fire('Respuesta SRI', `Estado: ${res.estadoSri}. ${res.mensajeSri}`, 'info');
                }
            },
            error: (err) => {
                Swal.fire('Error', 'No se pudo enviar al SRI.', 'error');
            }
        });
    }

    descargarPdf(id: number) {
        this.guiaService.descargarPdf(id);
    }
}
