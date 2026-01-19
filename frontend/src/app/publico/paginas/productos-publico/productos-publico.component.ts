import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

interface Categoria {
    categoriaId: number;
    categoriaNombre: string;
}

interface Producto {
    productoId: number;
    productoNombre: string;
    productoCodigo: string;
    productoPrecio: number;
    productoStock: number;
    productoEstado: boolean;
    categoria?: Categoria;
}

interface CategoriaGroup {
    nombre: string;
    productos: Producto[];
}

@Component({
    selector: 'app-productos-publico',
    standalone: true,
    imports: [CommonModule],
    templateUrl: './productos-publico.component.html',
    styleUrls: ['./productos-publico.component.css']
})
export class ProductosPublicoComponent implements OnInit {

    categorias: CategoriaGroup[] = [];
    isLoading = true;
    private apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) { }

    ngOnInit(): void {
        this.isLoading = true;
        this.http.get<Producto[]>(`${this.apiUrl}/api/productos`)
            .subscribe({
                next: (data) => {
                    const activos = data.filter(p => p.productoEstado);
                    this.agruparPorCategoria(activos);
                    this.isLoading = false;
                },
                error: (err) => {
                    console.error('Error cargando catálogo', err);
                    this.isLoading = false;
                }
            });
    }

    private agruparPorCategoria(productos: Producto[]) {
        const mapa = new Map<String, Producto[]>();

        productos.forEach(prod => {
            const nombreCat = prod.categoria ? prod.categoria.categoriaNombre : 'Otros';
            if (!mapa.has(nombreCat)) {
                mapa.set(nombreCat, []);
            }
            mapa.get(nombreCat)?.push(prod);
        });

        this.categorias = Array.from(mapa.entries()).map(([nombre, prods]) => ({
            nombre: nombre.toString(),
            productos: prods
        }));
    }
}
