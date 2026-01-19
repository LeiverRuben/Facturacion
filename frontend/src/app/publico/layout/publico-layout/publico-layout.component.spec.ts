import { ComponentFixture, TestBed } from '@angular/core/testing';

import { PublicoLayoutComponent } from './publico-layout.component';

describe('PublicoLayoutComponent', () => {
  let component: PublicoLayoutComponent;
  let fixture: ComponentFixture<PublicoLayoutComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PublicoLayoutComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(PublicoLayoutComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
