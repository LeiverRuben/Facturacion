import { Routes } from '@angular/router';

import { InicioComponent } from './publico/paginas/inicio/inicio.component';
import { ContactanosComponent } from './publico/paginas/contactanos/contactanos.component';
import { PublicoLayoutComponent } from './publico/layout/publico-layout/publico-layout.component';
import { authGuard } from './auth/auth.guard';
import { PrivadoLayoutComponent } from './privado/privado-layout/privado-layout.component';
import { LoginComponent } from './auth/login/login.component';

// Nuevos Componentes
import { DashboardComponent } from './dashboard/dashboard.component';
import { ProductosComponent } from './productos/productos.component';
import { FacturacionComponent } from './facturacion/facturacion.component';
import { ProveedoresComponent } from './proveedores/proveedores.component';
import { ClientesComponent } from './clientes/clientes.component';
// Nuevos
import { CategoriasComponent } from './categorias/categorias.component';
import { EmpresaComponent } from './empresa/empresa.component';
import { KardexComponent } from './kardex/kardex.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: PublicoLayoutComponent,
    children: [
      { path: '', component: InicioComponent },
      { path: 'catalogo', loadComponent: () => import('./publico/paginas/productos-publico/productos-publico.component').then(m => m.ProductosPublicoComponent) },
      { path: 'contacto', component: ContactanosComponent }
    ]
  },
  {
    path: '',
    component: PrivadoLayoutComponent,
    canActivate: [authGuard],
    children: [
      { path: 'dashboard', component: DashboardComponent },
      { path: 'clientes', component: ClientesComponent },
      { path: 'productos', component: ProductosComponent },
      { path: 'facturacion', loadComponent: () => import('./facturacion/facturacion.component').then(m => m.FacturacionComponent) },
      { path: 'proveedores', loadComponent: () => import('./proveedores/proveedores.component').then(m => m.ProveedoresComponent) },
      { path: 'compras', loadComponent: () => import('./compras/compras.component').then(m => m.ComprasComponent) },
      { path: 'caja', loadComponent: () => import('./caja/caja.component').then(m => m.CajaComponent) },
      { path: 'categorias', component: CategoriasComponent },
      { path: 'empresa', component: EmpresaComponent },

      { path: 'kardex', component: KardexComponent },
      { path: 'retenciones', loadComponent: () => import('./retenciones/retenciones.component').then(m => m.RetencionesComponent) },
      { path: 'guias-remision', loadComponent: () => import('./guias-remision/guias-remision.component').then(m => m.GuiaRemisionComponent) },
      { path: 'guias-remision/crear', loadComponent: () => import('./guias-remision/crear-guia/crear-guia.component').then(m => m.CrearGuiaComponent) },
      { path: 'notas-credito', loadComponent: () => import('./notas-credito/notas-credito.component').then(m => m.NotasCreditoComponent) },
      { path: 'notas-credito/crear', loadComponent: () => import('./notas-credito/crear-nota/crear-nota.component').then(m => m.CrearNotaCreditoComponent) },
      { path: 'liquidaciones', loadComponent: () => import('./liquidaciones/liquidaciones.component').then(m => m.LiquidacionesComponent) },
      { path: 'liquidaciones/crear', loadComponent: () => import('./liquidaciones/crear-liquidacion/crear-liquidacion.component').then(m => m.CrearLiquidacionComponent) },
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: '' }
];
