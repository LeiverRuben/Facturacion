import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { NotaCreditoService } from '../../services/nota-credito.service';
import Swal from 'sweetalert2';

@Component({
    selector: 'app-crear-nota',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule],
    templateUrl: './crear-nota.component.html'
})
export class CrearNotaCreditoComponent implements OnInit {
    // Buscador
    terminoBusqueda: string = '';
    facturasEncontradas: any[] = [];
    mostrarResultados = false;

    // Factura Seleccionada
    facturaSeleccionada: any = null;

    // Formulario
    ncForm: FormGroup;
    procesando = false;

    constructor(
        private fb: FormBuilder,
        private ncService: NotaCreditoService,
        private router: Router
    ) {
        this.ncForm = this.fb.group({
            motivo: ['', Validators.required],
            detalles: this.fb.array([])
        });
    }

    ngOnInit(): void {
    }

    buscarFacturas() {
        if (!this.terminoBusqueda.trim()) return;

        // Aquí idealmente llamaríamos a un endpoint de búsqueda específico.
        // Por MVP, filtramos de la lista total (ojo rendimiento producción).
        this.ncService.listarFacturas().subscribe(facturas => {
            this.facturasEncontradas = facturas.filter(f =>
                (f.secuencial.includes(this.terminoBusqueda) ||
                    f.cliente.identificacion.includes(this.terminoBusqueda) ||
                    f.cliente.clienteNombre.toLowerCase().includes(this.terminoBusqueda.toLowerCase())) &&
                f.estadoSri === 'AUTORIZADO'
            );
            this.mostrarResultados = true;
        });
    }

    seleccionarFactura(factura: any) {
        this.facturaSeleccionada = factura;
        this.mostrarResultados = false;
        this.terminoBusqueda = '';
        this.cargarDetalles(factura);
    }

    cargarDetalles(factura: any) {
        const detallesArray = this.ncForm.get('detalles') as FormArray;
        detallesArray.clear();

        factura.detalles.forEach((det: any) => {
            detallesArray.push(this.fb.group({
                productoId: [det.producto.productoId],
                productoNombre: [det.producto.productoNombre],
                cantidadOriginal: [det.cantidad],
                precioUnitario: [det.precioUnitario],
                cantidadDevolver: [det.cantidad, [Validators.required, Validators.min(0), Validators.max(det.cantidad)]],
                seleccionado: [true] // Por defecto seleccionamos todo para devolver
            }));
        });
    }

    get detalles() {
        return (this.ncForm.get('detalles') as FormArray).controls;
    }

    guardar() {
        if (this.ncForm.invalid) {
            this.ncForm.markAllAsTouched();
            return;
        }

        if (!this.facturaSeleccionada) return;

        const formVal = this.ncForm.value;

        // Filtrar solo seleccionados y con cantidad > 0
        const itemsProcesar = formVal.detalles
            .filter((d: any) => d.seleccionado && d.cantidadDevolver > 0)
            .map((d: any) => ({
                productoId: d.productoId,
                cantidad: d.cantidadDevolver,
                precioUnitario: d.precioUnitario,
                descuento: 0, // Simplificación: asumo descuento proporcional o 0 para MVP
                descripcion: d.productoNombre
            }));

        if (itemsProcesar.length === 0) {
            Swal.fire('Error', 'Debe seleccionar al menos un producto para devolver.', 'warning');
            return;
        }

        const dto = {
            facturaId: this.facturaSeleccionada.id,
            motivo: formVal.motivo,
            detalles: itemsProcesar
        };

        console.log('DTO enviar:', dto);

        this.procesando = true;
        this.ncService.crear(dto).subscribe({
            next: (resp) => {
                Swal.fire('Creada', `Nota de Crédito ${resp.secuencial} creada correctamente`, 'success')
                    .then(() => {
                        this.router.navigate(['/notas-credito']);
                    });
                this.procesando = false;
            },
            error: (err) => {
                console.error(err);
                Swal.fire('Error', 'No se pudo crear la Nota de Crédito', 'error');
                this.procesando = false;
            }
        });
    }
}
