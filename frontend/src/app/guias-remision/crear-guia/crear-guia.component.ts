import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { GuiaRemisionService }
    from '../../services/guia-remision.service';
import { Router } from '@angular/router';
import Swal from 'sweetalert2';

@Component({
    selector: 'app-crear-guia',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, RouterModule],
    template: `
    <div class="page-header slide-in-bottom">
        <div>
            <h1 class="page-title"><i class='bx bx-plus-circle'></i> Nueva Guía de Remisión</h1>
            <p class="page-subtitle">Complete la información para generar el documento electrónico.</p>
        </div>
        <button class="btn-action btn-secondary" routerLink="/guias-remision">
            <i class='bx bx-arrow-back'></i> Volver
        </button>
    </div>

    <form [formGroup]="form" (ngSubmit)="guardar()">
        <!-- Sección Transportista -->
        <div class="card-premium slide-in-bottom mb-4">
            <div class="card-header">
                <h3 class="card-title">Información del Transportista y Traslado</h3>
            </div>
            <div class="card-body">
                <div class="row g-3">
                    <div class="col-md-6">
                        <label class="form-label">Identificación Transportista (RUC/CI)</label>
                        <input type="text" class="form-control" formControlName="transportistaIdentificacion" placeholder="Ej: 1790011223001">
                    </div>
                    <div class="col-md-6">
                        <label class="form-label">Razón Social Transportista</label>
                        <input type="text" class="form-control" formControlName="transportistaRazonSocial" placeholder="Nombre del chofer o empresa">
                    </div>
                    <div class="col-md-4">
                        <label class="form-label">Placa Vehículo</label>
                        <input type="text" class="form-control" formControlName="placa" placeholder="Ej: ABC-1234">
                    </div>
                    <div class="col-md-8">
                        <label class="form-label">Dirección de Partida</label>
                        <input type="text" class="form-control" formControlName="dirPartida" placeholder="Dirección bodega/origen">
                    </div>
                    <div class="col-md-3">
                        <label class="form-label">Fecha Inicio Traslado</label>
                        <input type="date" class="form-control" formControlName="fechaIniTransporte">
                    </div>
                    <div class="col-md-3">
                        <label class="form-label">Fecha Fin Traslado</label>
                        <input type="date" class="form-control" formControlName="fechaFinTransporte">
                    </div>
                </div>
            </div>
        </div>

        <!-- Sección Destinatarios -->
        <div formArrayName="destinatarios">
            <div *ngFor="let dest of destinatarios.controls; let i = index" [formGroupName]="i" class="card-premium slide-in-bottom mb-4">
                <div class="card-header d-flex justify-content-between align-items-center bg-light">
                    <h3 class="card-title text-primary">Destinatario #{{ i + 1 }}</h3>
                    <button type="button" class="btn-icon delete" (click)="eliminarDestinatario(i)" *ngIf="destinatarios.length > 1" title="Eliminar Destinatario">
                        <i class='bx bx-trash'></i>
                    </button>
                </div>
                <div class="card-body">
                    <!-- Info Destinatario -->
                    <div class="row g-3 mb-4">
                        <div class="col-md-3">
                            <label class="form-label">Identificación</label>
                            <input type="text" class="form-control" formControlName="identificacionDestinatario">
                        </div>
                        <div class="col-md-5">
                            <label class="form-label">Razón Social Destinatario</label>
                            <input type="text" class="form-control" formControlName="razonSocialDestinatario">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">Dirección Llegada</label>
                            <input type="text" class="form-control" formControlName="dirDestinatario">
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">Motivo Traslado</label>
                            <input type="text" class="form-control" formControlName="motivoTraslado">
                        </div>
                         <div class="col-md-4">
                            <label class="form-label">Ruta</label>
                            <input type="text" class="form-control" formControlName="ruta" placeholder="Ruta principal">
                        </div>
                    </div>

                    <!-- Doc Sustento -->
                    <h5 class="fw-bold text-muted mb-3 border-bottom pb-2">Documento Sustento</h5>
                    <div class="row g-3 mb-4">
                        <div class="col-md-3">
                            <label class="form-label">Tipo Doc.</label>
                            <select class="form-control" formControlName="codDocSustento">
                                <option value="01">Factura</option>
                            </select>
                        </div>
                        <div class="col-md-4">
                            <label class="form-label">Num. Documento (001-001-000...)</label>
                            <input type="text" class="form-control" formControlName="numDocSustento">
                        </div>
                        <div class="col-md-5">
                            <label class="form-label">Num. Autorización</label>
                            <input type="text" class="form-control" formControlName="numAutDocSustento">
                        </div>
                    </div>

                    <!-- Detalles -->
                    <h5 class="fw-bold text-muted mb-3 border-bottom pb-2">Items a Transportar</h5>
                    <div formArrayName="detalles">
                        <table class="table-modern">
                            <thead>
                                <tr>
                                    <th>Código</th>
                                    <th>Descripción</th>
                                    <th width="120">Cantidad</th>
                                    <th width="50"></th>
                                </tr>
                            </thead>
                            <tbody>
                                <tr *ngFor="let det of getDetalles(i).controls; let j = index" [formGroupName]="j">
                                    <td><input type="text" class="form-control form-control-sm" formControlName="codigoInterno"></td>
                                    <td><input type="text" class="form-control form-control-sm" formControlName="descripcion"></td>
                                    <td><input type="number" class="form-control form-control-sm text-center" formControlName="cantidad"></td>
                                    <td class="text-center">
                                        <button type="button" class="btn-icon delete p-1" style="width:28px;height:28px;" (click)="eliminarDetalle(i, j)">
                                            <i class='bx bx-trash'></i>
                                        </button>
                                    </td>
                                </tr>
                            </tbody>
                        </table>
                        <button type="button" class="btn btn-sm btn-secondary mt-2" (click)="agregarDetalle(i)">
                            <i class='bx bx-plus'></i> Agregar Item
                        </button>
                    </div>
                </div>
            </div>
        </div>

        <div class="mb-4">
            <button type="button" class="btn btn-secondary" (click)="agregarDestinatario()">
                <i class='bx bx-user-plus'></i> Agregar Otro Destinatario
            </button>
        </div>

        <div class="d-flex justify-content-end gap-2 mb-5">
            <button type="button" class="btn-action btn-secondary" routerLink="/guias-remision">Cancelar</button>
            <button type="submit" class="btn-action btn-primary" [disabled]="form.invalid || loading">
                <i class='bx' [ngClass]="loading ? 'bx-loader-alt bx-spin' : 'bx-save'"></i> 
                {{ loading ? 'Guardando...' : 'Guardar Guía' }}
            </button>
        </div>
    </form>
  `
})
export class CrearGuiaComponent implements OnInit {
    form: FormGroup;
    loading = false;

    constructor(private fb: FormBuilder, private guiaService: GuiaRemisionService, private router: Router) {
        this.form = this.fb.group({
            transportistaIdentificacion: ['', Validators.required],
            transportistaRazonSocial: ['', Validators.required],
            placa: ['', Validators.required],
            dirPartida: ['', Validators.required],
            fechaIniTransporte: [new Date().toISOString().split('T')[0], Validators.required],
            fechaFinTransporte: [new Date().toISOString().split('T')[0], Validators.required],
            destinatarios: this.fb.array([])
        });
    }

    ngOnInit(): void {
        this.agregarDestinatario(); // Agregar al menos uno por defecto
    }

    get destinatarios() {
        return this.form.get('destinatarios') as FormArray;
    }

    getDetalles(indexDestinatario: number) {
        return this.destinatarios.at(indexDestinatario).get('detalles') as FormArray;
    }

    nuevoDestinatario(): FormGroup {
        return this.fb.group({
            identificacionDestinatario: ['', Validators.required],
            razonSocialDestinatario: ['', Validators.required],
            dirDestinatario: ['', Validators.required],
            motivoTraslado: ['Venta', Validators.required],
            ruta: [''],
            codDocSustento: ['01'],
            numDocSustento: [''],
            numAutDocSustento: [''],
            detalles: this.fb.array([this.nuevoDetalle()])
        });
    }

    nuevoDetalle(): FormGroup {
        return this.fb.group({
            codigoInterno: ['', Validators.required],
            descripcion: ['', Validators.required],
            cantidad: [1, [Validators.required, Validators.min(0.01)]]
        });
    }

    agregarDestinatario() {
        this.destinatarios.push(this.nuevoDestinatario());
    }

    eliminarDestinatario(index: number) {
        this.destinatarios.removeAt(index);
    }

    agregarDetalle(indexDestinatario: number) {
        this.getDetalles(indexDestinatario).push(this.nuevoDetalle());
    }

    eliminarDetalle(indexDestinatario: number, indexDetalle: number) {
        this.getDetalles(indexDestinatario).removeAt(indexDetalle);
    }

    guardar() {
        if (this.form.invalid) {
            Swal.fire({
                icon: 'warning',
                title: 'Formulario Incompleto',
                text: 'Por favor complete: Transportista, Placa, Fechas y asegúrese de que haya al menos un Destinatario.'
            });
            this.form.markAllAsTouched();
            return;
        }

        if (this.destinatarios.length === 0) {
            Swal.fire('Error', 'Debe agregar al menos un destinatario.', 'warning');
            return;
        }

        this.loading = true;
        this.guiaService.crear(this.form.value).subscribe({
            next: (res) => {
                this.loading = false;
                Swal.fire({
                    title: 'Guardado',
                    text: 'Guía creada exitosamente. ¿Desea enviarla al SRI ahora?',
                    icon: 'success',
                    showCancelButton: true,
                    confirmButtonText: 'Sí, enviar',
                    cancelButtonText: 'Más tarde'
                }).then((result) => {
                    if (result.isConfirmed) {
                        this.enviarAlSri(res.id);
                    } else {
                        this.router.navigate(['/guias-remision']);
                    }
                });
            },
            error: (error) => {
                this.loading = false;
                console.error(error);
                Swal.fire('Error', 'No se pudo guardar la guía.', 'error');
            }
        });
    }

    enviarAlSri(id: number) {
        Swal.fire({
            title: 'Enviando...',
            didOpen: () => Swal.showLoading()
        });
        this.guiaService.enviarSri(id).subscribe({
            next: (res) => {
                Swal.fire('Enviado', `Estado: ${res.estadoSri}`, 'success').then(() => {
                    this.router.navigate(['/guias-remision']);
                });
            },
            error: (error) => {
                Swal.fire('Error', 'Error al enviar al SRI', 'error').then(() => {
                    this.router.navigate(['/guias-remision']);
                });
            }
        });
    }
}
