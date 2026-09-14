import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { signal } from '@angular/core';
import { of, throwError } from 'rxjs';

import { TicketDetailComponent } from './ticket-detail.component';
import { TicketService } from '../services/ticket.service';
import { AuditLogService } from '../../audit-logs/services/audit-log.service';
import { AuthService } from '../../auth/services/auth.service';
import { Ticket } from '../models/ticket.models';
import { AuditLog } from '../../audit-logs/models/audit-log.models';
import { CurrentUser } from '../../auth/models/auth.models';

function ticket(overrides: Partial<Ticket> = {}): Ticket {
  return {
    id: 9,
    title: 'VPN caída',
    description: 'desc',
    status: 'NEW',
    priority: 'HIGH',
    categoryId: null,
    categoryName: null,
    createdById: 1,
    createdByName: 'Ana Torres',
    assignedToId: null,
    assignedToName: null,
    createdAt: '2026-09-07T10:00:00',
    updatedAt: '2026-09-07T10:00:00',
    ...overrides
  };
}

describe('TicketDetailComponent', () => {
  let ticketServiceStub: { assignToMe: jasmine.Spy; unassign: jasmine.Spy };
  let auditLogServiceStub: { listByTicket: jasmine.Spy; resolveNow: jasmine.Spy };
  let authServiceStub: { currentUser: ReturnType<typeof signal<CurrentUser | null>> };
  let component: TicketDetailComponent;

  function createComponent(initialTicket: Ticket = ticket()): void {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [TicketDetailComponent],
      providers: [
        provideNoopAnimations(),
        { provide: TicketService, useValue: ticketServiceStub },
        { provide: AuditLogService, useValue: auditLogServiceStub },
        { provide: AuthService, useValue: authServiceStub }
      ]
    });
    const fixture = TestBed.createComponent(TicketDetailComponent);
    fixture.componentRef.setInput('ticket', initialTicket);
    fixture.detectChanges();
    component = fixture.componentInstance;
  }

  beforeEach(() => {
    ticketServiceStub = {
      assignToMe: jasmine.createSpy('assignToMe'),
      unassign: jasmine.createSpy('unassign')
    };
    auditLogServiceStub = {
      listByTicket: jasmine.createSpy('listByTicket').and.returnValue(of([] as AuditLog[])),
      resolveNow: jasmine.createSpy('resolveNow')
    };
    authServiceStub = { currentUser: signal<CurrentUser | null>({ id: 3, email: 'ana.torres@nemblex.dev', role: 'TECHNICIAN' }) };
    createComponent();
  });

  it('loads the activity trail for the given ticket on init', () => {
    expect(auditLogServiceStub.listByTicket).toHaveBeenCalledWith(9);
  });

  it('emits the updated ticket on successful assignToMe', () => {
    const updated = ticket({ assignedToId: 3, assignedToName: 'Ana Torres' });
    ticketServiceStub.assignToMe.and.returnValue(of(updated));
    let emitted: Ticket | undefined;
    component.ticketChanged.subscribe((t) => (emitted = t));

    component.assignToMe();

    expect(ticketServiceStub.assignToMe).toHaveBeenCalledWith(9);
    expect(emitted).toEqual(updated);
    expect(component.assigning()).toBeFalse();
    expect(component.assignError()).toBeNull();
  });

  it('shows the backend error message when assignToMe fails', () => {
    ticketServiceStub.assignToMe.and.returnValue(
      throwError(() => ({ error: { message: 'Este ticket ya esta asignado a otro tecnico' } }))
    );

    component.assignToMe();

    expect(component.assignError()).toBe('Este ticket ya esta asignado a otro tecnico');
    expect(component.assigning()).toBeFalse();
  });

  it('falls back to a generic message when assignToMe fails without a backend message', () => {
    ticketServiceStub.assignToMe.and.returnValue(throwError(() => new Error('boom')));

    component.assignToMe();

    expect(component.assignError()).not.toBeNull();
    expect(component.assigning()).toBeFalse();
  });

  it('does not call resolveNow when the resolve form is invalid', () => {
    component.resolveForm.setValue({ action: '', reason: '' });

    component.submitResolve();

    expect(auditLogServiceStub.resolveNow).not.toHaveBeenCalled();
  });

  it('resolves the ticket and emits it as RESOLVED on success', () => {
    const resolvedLog: AuditLog = {
      id: 5,
      ticketId: 9,
      action: 'REINICIO_SERVICIO',
      reason: 'Se reinició el servicio',
      employeeMessage: null,
      resultStatus: 'APPROVED',
      approvedByName: 'Ana Torres',
      createdAt: '2026-09-07T10:10:00'
    };
    auditLogServiceStub.resolveNow.and.returnValue(of(resolvedLog));
    let emitted: Ticket | undefined;
    component.ticketChanged.subscribe((t) => (emitted = t));
    component.resolveForm.setValue({ action: 'REINICIO_SERVICIO', reason: 'Se reinició el servicio' });

    component.submitResolve();

    expect(auditLogServiceStub.resolveNow).toHaveBeenCalledWith({
      ticketId: 9,
      action: 'REINICIO_SERVICIO',
      reason: 'Se reinició el servicio'
    });
    expect(emitted?.status).toBe('RESOLVED');
    expect(component.resolving()).toBeFalse();
    expect(component.resolveOpen()).toBeFalse();
  });

  it('sets an error and stops when resolveNow fails', () => {
    auditLogServiceStub.resolveNow.and.returnValue(throwError(() => new Error('boom')));
    component.resolveForm.setValue({ action: 'REINICIO_SERVICIO', reason: '' });

    component.submitResolve();

    expect(component.resolveError()).not.toBeNull();
    expect(component.resolving()).toBeFalse();
  });

  describe('canUnassign', () => {
    it('is false when the ticket has no assignee', () => {
      createComponent(ticket({ assignedToId: null }));

      expect(component.canUnassign()).toBeFalse();
    });

    it('is true when the current user is the assignee', () => {
      createComponent(ticket({ assignedToId: 3 }));

      expect(component.canUnassign()).toBeTrue();
    });

    it('is false when assigned to someone else and the current user is a plain technician', () => {
      createComponent(ticket({ assignedToId: 7 }));

      expect(component.canUnassign()).toBeFalse();
    });

    it('is true when assigned to someone else but the current user is a SUPERVISOR', () => {
      authServiceStub.currentUser.set({ id: 2, email: 'beatriz.ruiz@nemblex.dev', role: 'SUPERVISOR' });
      createComponent(ticket({ assignedToId: 7 }));

      expect(component.canUnassign()).toBeTrue();
    });
  });

  describe('unassignTicket', () => {
    it('emits the updated ticket on success', () => {
      createComponent(ticket({ assignedToId: 3, assignedToName: 'Ana Torres' }));
      const updated = ticket({ assignedToId: null, assignedToName: null });
      ticketServiceStub.unassign.and.returnValue(of(updated));
      let emitted: Ticket | undefined;
      component.ticketChanged.subscribe((t) => (emitted = t));

      component.unassignTicket();

      expect(ticketServiceStub.unassign).toHaveBeenCalledWith(9);
      expect(emitted).toEqual(updated);
      expect(component.unassigning()).toBeFalse();
      expect(component.unassignError()).toBeNull();
    });

    it('sets an error and stops when unassign fails', () => {
      createComponent(ticket({ assignedToId: 3, assignedToName: 'Ana Torres' }));
      ticketServiceStub.unassign.and.returnValue(
        throwError(() => ({ error: { message: 'No tenes permiso para liberar esta asignacion' } }))
      );

      component.unassignTicket();

      expect(component.unassignError()).toBe('No tenes permiso para liberar esta asignacion');
      expect(component.unassigning()).toBeFalse();
    });
  });
});
