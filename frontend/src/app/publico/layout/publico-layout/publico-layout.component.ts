import { Component } from '@angular/core';
import { MenuComponent } from "../menu/menu.component";
import { PiepaginaComponent } from "../piepagina/piepagina.component";
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-publico-layout',
  imports: [RouterOutlet, MenuComponent, PiepaginaComponent],
  templateUrl: './publico-layout.component.html',
  styleUrl: './publico-layout.component.css'
})
export class PublicoLayoutComponent {

}
