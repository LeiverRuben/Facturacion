import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import Swal from 'sweetalert2';

interface Categoria {
    categoriaId?: number;
    categoriaNombre: string;
    categoriaDescripcion: string;
    estado?: boolean;
}

@Component({
    selector: 'app-categorias',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './categorias.component.html',
    styleUrls: ['./categorias.component.css']
})
export class CategoriasComponent implements OnInit {
    categorias: Categoria[] = [];
    categoriaForm: Categoria = { categoriaNombre: '', categoriaDescripcion: '' };
    isEditing: boolean = false;
    private apiUrl = environment.apiUrl;

    constructor(private http: HttpClient) { }

    ngOnInit(): void {
        this.loadCategorias();
    }

    loadCategorias() {
        this.http.get<Categoria[]>(`${this.apiUrl}/api/categoria`).subscribe({
            next: (data) => this.categorias = data,
            error: (err) => console.error('Error cargando categorias', err)
        });
    }

    onSubmit() {
        if (!this.categoriaForm.categoriaNombre) {
            Swal.fire('Atención', 'El nombre de la categoría es obligatorio', 'warning');
            return;
        }

        if (this.isEditing && this.categoriaForm.categoriaId) {
            this.http.put(`${this.apiUrl}/api/categoria/${this.categoriaForm.categoriaId}`, this.categoriaForm)
                .subscribe({
                    next: () => {
                        Swal.fire('Actualizado', 'Categoría actualizada correctamente', 'success');
                        this.resetForm();
                        this.loadCategorias();
                    },
                    error: (err) => Swal.fire('Error', 'No se pudo actualizar la categoría', 'error')
                });
        } else {
            this.http.post(`${this.apiUrl}/api/categoria`, this.categoriaForm)
                .subscribe({
                    next: () => {
                        Swal.fire('Creado', 'Categoría creada correctamente', 'success');
                        this.resetForm();
                        this.loadCategorias();
                    },
                    error: (err) => Swal.fire('Error', 'No se pudo crear la categoría', 'error')
                });
        }
    }

    editCategoria(cat: Categoria) {
        this.categoriaForm = { ...cat };
        this.isEditing = true;
    }

    deleteCategoria(id: number) {
        Swal.fire({
            title: '¿Estás seguro?',
            text: "No podrás revertir esto",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonText: 'Sí, eliminar',
            cancelButtonText: 'Cancelar'
        }).then((result) => {
            if (result.isConfirmed) {
                this.http.delete(`${this.apiUrl}/api/categoria/${id}`).subscribe({
                    next: () => {
                        Swal.fire('Eliminado', 'Categoría eliminada.', 'success');
                        this.loadCategorias();
                    },
                    error: (err) => Swal.fire('Error', 'No se puede eliminar (posiblemente esté en uso)', 'error')
                });
            }
        });
    }

    resetForm() {
        this.categoriaForm = { categoriaNombre: '', categoriaDescripcion: '' };
        this.isEditing = false;
    }
}
