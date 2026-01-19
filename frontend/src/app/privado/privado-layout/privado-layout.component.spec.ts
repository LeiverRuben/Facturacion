/// <reference types="jasmine" />
import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PrivadoLayoutComponent } from './privado-layout.component';


describe('PrivadoLayoutComponent', () => {
  let component: PrivadoLayoutComponent;
  let fixture: ComponentFixture<PrivadoLayoutComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PrivadoLayoutComponent]
    })
      .compileComponents();

    fixture = TestBed.createComponent(PrivadoLayoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
