import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class NotaCreditoService {

    private apiUrl = `${environment.apiUrl}/api/notas-credito`;
    private facturasUrl = `${environment.apiUrl}/api/facturas`;

    constructor(private http: HttpClient) { }

    listar(): Observable<any[]> {
        return this.http.get<any[]>(this.apiUrl);
    }

    crear(dto: any): Observable<any> {
        return this.http.post(`${this.apiUrl}/crear`, dto);
    }

    enviarSRI(id: number): Observable<any> {
        return this.http.post(`${this.apiUrl}/enviar-sri/${id}`, {});
    }

    obtenerPorId(id: number): Observable<any> {
        return this.http.get(`${this.apiUrl}/${id}`);
    }

    // Helper para buscar facturas (podríamos usar FacturaService pero para encapsular módulo lo hago aquí o reutilizo)
    listarFacturas(): Observable<any[]> {
        return this.http.get<any[]>(this.facturasUrl);
    }
}
