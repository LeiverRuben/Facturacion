import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductoService } from '../../../servicio/producto.service';

@Component({
  selector: 'app-productos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './productos.component.html'
})
export class ProductosComponent implements OnInit {

  cargando = true;

  productos: any[] = [];
  productosFiltrados: any[] = [];
  productosPaginados: any[] = [];

  filtro = '';

  paginaActual = 1;
  itemsPorPagina = 5;
  totalPaginas = 0;
  paginas: number[] = [];

  constructor(private productosService: ProductoService) {}

  ngOnInit(): void {
    this.cargarProductos();
  }

  cargarProductos() {
    this.cargando = true;

    this.productosService.listar().subscribe({
      next: (data) => {
        this.productos = data;
        this.productosFiltrados = [...data];
        this.configurarPaginacion();
        this.cargando = false;
      },
      error: () => {
        this.cargando = false;
      }
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

  cambiarPagina(pagina: number) {
    if (pagina < 1 || pagina > this.totalPaginas) return;
    this.paginaActual = pagina;
    this.actualizarPagina();
  }

  actualizarPagina() {
    const inicio = (this.paginaActual - 1) * this.itemsPorPagina;
    const fin = inicio + this.itemsPorPagina;
    this.productosPaginados = this.productosFiltrados.slice(inicio, fin);
  }
}
