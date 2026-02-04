import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, FormArray, Validators, ReactiveFormsModule, FormsModule } from '@angular/forms';
import { RouterModule, Router } from '@angular/router';
import { LiquidacionService } from '../../services/liquidacion.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../environments/environment';
import Swal from 'sweetalert2';

@Component({
    selector: 'app-crear-liquidacion',
    standalone: true,
    imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterModule],
    templateUrl: './crear-liquidacion.component.html'
})
export class CrearLiquidacionComponent implements OnInit {
    lcForm: FormGroup;
    procesando = false;

    // Datos para seleccionadores
    proveedores: any[] = [];
    productos: any[] = [];

    constructor(
        private fb: FormBuilder,
        private lcService: LiquidacionService,
        private http: HttpClient,
        private router: Router
    ) {
        this.lcForm = this.fb.group({
            proveedorId: ['', Validators.required],
            detalles: this.fb.array([])
        });
    }

    ngOnInit(): void {
        this.cargarDatos();
        this.agregarFila(); // Empezar con una fila vacía
    }

    cargarDatos() {
        // Cargar Proveedores
        this.http.get<any[]>(`${environment.apiUrl}/api/proveedores`).subscribe(data => {
            this.proveedores = data;
        });

        // Cargar Productos
        this.http.get<any[]>(`${environment.apiUrl}/api/productos`).subscribe(data => {
            this.productos = data.filter(p => p.productoEstado);
        });
    }

    get detalles() {
        return (this.lcForm.get('detalles') as FormArray).controls;
    }

    agregarFila() {
        const fila = this.fb.group({
            productoId: ['', Validators.required],
            cantidad: [1, [Validators.required, Validators.min(0.01)]],
            precioUnitario: [0, [Validators.required, Validators.min(0)]],
            descuento: [0, [Validators.required, Validators.min(0)]],
            subtotal: [{ value: 0, disabled: true }]
        });

        // Escuchar cambios para recalcular subtotal de la fila
        fila.valueChanges.subscribe(val => {
            const cantidad = val.cantidad ?? 0;
            const precioUnitario = val.precioUnitario ?? 0;
            const descuento = val.descuento ?? 0;
            const sub = (cantidad * precioUnitario) - descuento;
            fila.get('subtotal')?.setValue(sub, { emitEvent: false });
        });

        (this.lcForm.get('detalles') as FormArray).push(fila);
    }

    eliminarFila(index: number) {
        (this.lcForm.get('detalles') as FormArray).removeAt(index);
    }

    redondearDetalle(index: number, campo: string) {
        const fila = (this.lcForm.get('detalles') as FormArray).at(index);
        const valor = fila.get(campo)?.value;
        if (valor !== null && valor !== undefined) {
            fila.patchValue({
                [campo]: parseFloat(Number(valor).toFixed(2))
            });
        }
    }

    onProductoChange(index: number) {
        const fila = (this.lcForm.get('detalles') as FormArray).at(index);
        const prodId = fila.get('productoId')?.value;
        const producto = this.productos.find(p => p.productoId == prodId);

        if (producto) {
            fila.patchValue({
                precioUnitario: parseFloat(Number(producto.productoPrecio || 0).toFixed(2))
            });
        }
    }

    calcularTotal() {
        const items = (this.lcForm.get('detalles') as FormArray).value;
        return items.reduce((acc: number, item: any) => acc + (item.cantidad * item.precioUnitario - item.descuento), 0);
    }

    guardar() {
        if (this.lcForm.invalid) {
            Swal.fire({
                icon: 'warning',
                title: 'Formulario Incompleto',
                text: 'Por favor complete todos los campos requeridos (Proveedor, Detalles).'
            });
            this.lcForm.markAllAsTouched();
            return;
        }

        const formVal = this.lcForm.value;
        if (!formVal.detalles || formVal.detalles.length === 0) {
            Swal.fire('Error', 'Debe agregar al menos un item.', 'warning');
            return;
        }

        this.procesando = true;
        this.lcService.crear(formVal).subscribe({
            next: (resp: any) => {
                Swal.fire('Guardado', 'Liquidación de compra generada correctamente', 'success')
                    .then(() => this.router.navigate(['/liquidaciones']));
                this.procesando = false;
            },
            error: (error: any) => {
                console.error(error);
                Swal.fire('Error', 'No se pudo guardar la liquidación', 'error');
                this.procesando = false;
            }
        });
    }
}
