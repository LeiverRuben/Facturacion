import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { NotaCreditoService } from '../services/nota-credito.service';
import Swal from 'sweetalert2';

@Component({
    selector: 'app-notas-credito',
    standalone: true,
    imports: [CommonModule, RouterModule],
    templateUrl: './notas-credito.component.html'
})
export class NotasCreditoComponent implements OnInit {
    notas: any[] = [];
    loading = false;

    constructor(private ncService: NotaCreditoService) { }

    ngOnInit(): void {
        this.cargarNotas();
    }

    cargarNotas() {
        this.loading = true;
        this.ncService.listar().subscribe({
            next: (data) => {
                this.notas = data;
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

        this.ncService.enviarSRI(id).subscribe({
            next: (resp) => {
                Swal.fire('Procesado', `Estado: ${resp.estado} - ${resp.mensaje}`, 'success');
                this.cargarNotas();
            },
            error: (err) => {
                Swal.fire('Error', 'No se pudo enviar al SRI', 'error');
            }
        });
    }
}
