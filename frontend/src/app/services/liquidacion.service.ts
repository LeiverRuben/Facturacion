import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
    providedIn: 'root'
})
export class LiquidacionService {
    private apiUrl = `${environment.apiUrl}/api/liquidaciones-compra`;

    constructor(private http: HttpClient) { }

    listar(): Observable<any[]> {
        return this.http.get<any[]>(this.apiUrl);
    }

    crear(dto: any): Observable<any> {
        return this.http.post<any>(this.apiUrl, dto);
    }

    enviarSRI(id: number): Observable<any> {
        return this.http.post<any>(`${this.apiUrl}/${id}/enviar-sri`, {});
    }

    descargarPdf(id: number) {
        window.open(`${this.apiUrl}/${id}/pdf`, '_blank');
    }
}
