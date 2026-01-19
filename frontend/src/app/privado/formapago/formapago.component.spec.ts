import { ComponentFixture, TestBed } from '@angular/core/testing';

import { FormapagoComponent } from './formapago.component';

describe('FormapagoComponent', () => {
  let component: FormapagoComponent;
  let fixture: ComponentFixture<FormapagoComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [FormapagoComponent]
    })
    .compileComponents();

    fixture = TestBed.createComponent(FormapagoComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
