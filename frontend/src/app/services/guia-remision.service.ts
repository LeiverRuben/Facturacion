import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class GuiaRemisionService {
    private apiUrl = `${environment.apiUrl}/api/guias-remision`;

    constructor(private http: HttpClient) { }

    listar(): Observable<any[]> {
        return this.http.get<any[]>(this.apiUrl);
    }

    obtenerPorId(id: number): Observable<any> {
        return this.http.get<any>(`${this.apiUrl}/${id}`);
    }

    crear(guia: any): Observable<any> {
        return this.http.post<any>(this.apiUrl, guia);
    }

    enviarSri(id: number): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/enviar-sri/${id}`, {});
    }

    // Helper to open PDF
    descargarPdf(id: number) {
        window.open(`${this.apiUrl}/${id}/pdf`, '_blank');
    }
}
