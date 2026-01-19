import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Producto } from '../../modelos/producto';
import { ProductoService } from '../../servicio/producto.service';

@Component({
  selector: 'app-producto',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule,FormsModule],
  templateUrl: './producto.component.html',
  styleUrl: './producto.component.css'
})
export class ProductoComponent implements OnInit {

  productos: Producto[] = [];
  productosFiltrados: Producto[] = [];
  productosPaginados: Producto[] = [];

  form!: FormGroup;
  editando = false;
  productoId?: number;

  
  filtro = '';


  paginaActual = 1;
  itemsPorPagina = 3;
  totalPaginas = 0;
  paginas: number[] = [];

  constructor(
    private productoService: ProductoService,
    private fb: FormBuilder
  ) {}

  ngOnInit(): void {
    this.cargarProductos();

    this.form = this.fb.group({
      productoSerial: ['', Validators.required],
      productoNombre: ['', Validators.required],
      productoPrecio: [0, Validators.required],
      productoStock: [0, Validators.required],
      productoTasa: [12],
      productoCategoria: ['', Validators.required],
      productoEstado: [1]
    });
  }

  cargarProductos() {
    this.productoService.listar().subscribe(data => {
      this.productos = data;
      this.productosFiltrados = [...data];
      this.configurarPaginacion();
    });
  }

  
  aplicarFiltro() {
    const texto = this.filtro.toLowerCase();

    this.productosFiltrados = this.productos.filter(p =>
      p.productoNombre.toLowerCase().includes(texto) ||
      p.productoCategoria.toLowerCase().includes(texto)
    );

    this.paginaActual = 1;
    this.configurarPaginacion();
  }

  
  configurarPaginacion() {
    this.totalPaginas = Math.ceil(this.productosFiltrados.length / this.itemsPorPagina);
    this.paginas = Array.from({ length: this.totalPaginas }, (_, i) => i + 1);
    this.actualizarPagina();
  }

  cambiarPagina(p: number) {
    if (p < 1 || p > this.totalPaginas) return;
    this.paginaActual = p;
    this.actualizarPagina();
  }

  actualizarPagina() {
    const inicio = (this.paginaActual - 1) * this.itemsPorPagina;
    const fin = inicio + this.itemsPorPagina;
    this.productosPaginados = this.productosFiltrados.slice(inicio, fin);
  }

  
  guardar() {
    if (this.form.invalid) return;

    if (this.editando && this.productoId) {
      this.productoService.actualizar(this.productoId, this.form.value)
        .subscribe(() => {
          this.cancelar();
          this.cargarProductos();
        });
    } else {
      this.productoService.crear(this.form.value)
        .subscribe(() => {
          this.form.reset({ productoEstado: 1, productoTasa: 12 });
          this.cargarProductos();
        });
    }
  }

  editar(p: Producto) {
    this.editando = true;
    this.productoId = p.productoId;
    this.form.patchValue(p);
  }

  eliminar(id?: number) {
    if (!id) return;
    if (!confirm('¿Eliminar producto?')) return;

    this.productoService.eliminar(id).subscribe(() => {
      this.cargarProductos();
    });
  }

  cancelar() {
    this.editando = false;
    this.productoId = undefined;
    this.form.reset({ productoEstado: 1, productoTasa: 12 });
  }
}
