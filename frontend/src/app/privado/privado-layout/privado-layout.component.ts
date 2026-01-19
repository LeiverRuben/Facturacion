import { Component } from '@angular/core';
import { Router, RouterModule } from "@angular/router";
import { AuthService } from '../../auth/auth.service';

@Component({
  selector: 'app-privado-layout',
  imports: [RouterModule],
  templateUrl: './privado-layout.component.html',
  styleUrl: './privado-layout.component.css'
})
export class PrivadoLayoutComponent {
   constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
