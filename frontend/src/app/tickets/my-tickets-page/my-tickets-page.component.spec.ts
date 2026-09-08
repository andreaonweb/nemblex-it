import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { MyTicketsPageComponent } from './my-tickets-page.component';
import { TicketService } from '../services/ticket.service';
import { Ticket } from '../models/ticket.models';

function ticket(overrides: Partial<Ticket> = {}): Ticket {
  return {
    id: 1,
    title: 'No puedo acceder al VPN',
    description: 'desc',
    status: 'NEW',
    priority: 'MEDIUM',
    categoryId: null,
    categoryName: null,
    createdById: 5,
    createdByName: 'Carlos Mendez',
    assignedToId: null,
    assignedToName: null,
    createdAt: '2026-09-08T10:00:00',
    updatedAt: '2026-09-08T10:00:00',
    ...overrides
  };
}

describe('MyTicketsPageComponent', () => {
  let ticketServiceStub: { getMine: jasmine.Spy; create: jasmine.Spy };
  let component: MyTicketsPageComponent;

  function createComponent(): void {
    TestBed.configureTestingModule({
      imports: [MyTicketsPageComponent],
      providers: [provideNoopAnimations(), { provide: TicketService, useValue: ticketServiceStub }]
    });
    const fixture = TestBed.createComponent(MyTicketsPageComponent);
    fixture.detectChanges();
    component = fixture.componentInstance;
  }

  beforeEach(() => {
    ticketServiceStub = {
      getMine: jasmine.createSpy('getMine').and.returnValue(of([] as Ticket[])),
      create: jasmine.createSpy('create')
    };
  });

  it('loads the tickets created by the current user on init', () => {
    ticketServiceStub.getMine.and.returnValue(of([ticket()]));

    createComponent();

    expect(ticketServiceStub.getMine).toHaveBeenCalled();
    expect(component.tickets()).toEqual([ticket()]);
    expect(component.loading()).toBeFalse();
  });

  it('sets an error when loading tickets fails', () => {
    ticketServiceStub.getMine.and.returnValue(throwError(() => new Error('boom')));

    createComponent();

    expect(component.error()).not.toBeNull();
    expect(component.loading()).toBeFalse();
  });

  it('toggles the new ticket form open and closed', () => {
    createComponent();
    expect(component.formOpen()).toBeFalse();

    component.toggleForm();
    expect(component.formOpen()).toBeTrue();

    component.toggleForm();
    expect(component.formOpen()).toBeFalse();
  });

  it('does not call create when the form is invalid', () => {
    createComponent();
    component.form.setValue({ title: '', description: '' });

    component.submit();

    expect(ticketServiceStub.create).not.toHaveBeenCalled();
  });

  it('creates a ticket, prepends it to the list, and closes the form on success', () => {
    createComponent();
    const created = ticket({ id: 99, title: 'Nuevo pedido' });
    ticketServiceStub.create.and.returnValue(of(created));
    component.formOpen.set(true);
    component.form.setValue({ title: 'Nuevo pedido', description: 'Necesito ayuda' });

    component.submit();

    expect(ticketServiceStub.create).toHaveBeenCalledWith({ title: 'Nuevo pedido', description: 'Necesito ayuda' });
    expect(component.tickets()[0]).toEqual(created);
    expect(component.formOpen()).toBeFalse();
    expect(component.submitting()).toBeFalse();
  });

  it('sets a submit error and stops submitting when create fails', () => {
    createComponent();
    ticketServiceStub.create.and.returnValue(throwError(() => new Error('boom')));
    component.form.setValue({ title: 'Nuevo pedido', description: 'Necesito ayuda' });

    component.submit();

    expect(component.submitError()).not.toBeNull();
    expect(component.submitting()).toBeFalse();
  });
});
