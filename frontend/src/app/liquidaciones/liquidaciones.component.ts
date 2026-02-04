import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LiquidacionService } from '../services/liquidacion.service';
import Swal from 'sweetalert2';

@Component({
    selector: 'app-liquidaciones',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './liquidaciones.component.html'
})
export class LiquidacionesComponent implements OnInit {
    liquidaciones: any[] = [];
    loading = false;

    constructor(private lcService: LiquidacionService) { }

    ngOnInit(): void {
        this.cargarLiquidaciones();
    }

    cargarLiquidaciones() {
        this.loading = true;
        this.lcService.listar().subscribe({
            next: (data) => {
                this.liquidaciones = data;
                this.loading = false;
            },
            error: (err) => {
                console.error(err);
                this.loading = false;
            }
        });
    }

    enviarSri(id: number) {
        Swal.fire({
            title: 'Enviando al SRI...',
            text: 'Por favor espere',
            allowOutsideClick: false,
            didOpen: () => Swal.showLoading()
        });

        this.lcService.enviarSRI(id).subscribe({
            next: (resp) => {
                Swal.fire('Procesado', `Estado: ${resp.estado} - ${resp.mensaje}`, 'success');
                this.cargarLiquidaciones();
            },
            error: (err) => {
                Swal.fire('Error', 'No se pudo enviar al SRI', 'error');
            }
        });
    }

    descargarPdf(id: number) {
        this.lcService.descargarPdf(id);
    }
}
