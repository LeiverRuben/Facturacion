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
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' }
    ]
  },
  { path: '**', redirectTo: '' }
];
