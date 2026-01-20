import { Component } from '@angular/core';
import { Router, RouterModule } from "@angular/router";
import { CommonModule } from '@angular/common';
import { AuthService } from '../../auth/auth.service';
import { ThemeService } from '../../servicio/theme.service';

@Component({
  selector: 'app-privado-layout',
  standalone: true,
  imports: [RouterModule, CommonModule],
  templateUrl: './privado-layout.component.html',
  styleUrl: './privado-layout.component.css'
})
export class PrivadoLayoutComponent {
  isDarkMode: boolean = false;
  showSettingsMenu: boolean = false;

  constructor(
    private authService: AuthService,
    private router: Router,
    public themeService: ThemeService
  ) {
    this.themeService.isDarkMode$.subscribe(dark => {
      this.isDarkMode = dark;
    });
  }

  toggleTheme() {
    this.themeService.toggleTheme();
  }

  toggleSettings() {
    this.showSettingsMenu = !this.showSettingsMenu;
  }

  logout(): void {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
