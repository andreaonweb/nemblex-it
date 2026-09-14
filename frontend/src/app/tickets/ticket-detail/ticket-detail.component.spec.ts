import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { of, throwError } from 'rxjs';

import { TicketDetailComponent } from './ticket-detail.component';
import { TicketService } from '../services/ticket.service';
import { AuditLogService } from '../../audit-logs/services/audit-log.service';
import { Ticket } from '../models/ticket.models';
import { AuditLog } from '../../audit-logs/models/audit-log.models';

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
  let ticketServiceStub: { assignToMe: jasmine.Spy };
  let auditLogServiceStub: { listByTicket: jasmine.Spy; resolveNow: jasmine.Spy };
  let component: TicketDetailComponent;

  beforeEach(() => {
    ticketServiceStub = { assignToMe: jasmine.createSpy('assignToMe') };
    auditLogServiceStub = {
      listByTicket: jasmine.createSpy('listByTicket').and.returnValue(of([] as AuditLog[])),
      resolveNow: jasmine.createSpy('resolveNow')
    };
    TestBed.configureTestingModule({
      imports: [TicketDetailComponent],
      providers: [
        provideNoopAnimations(),
        { provide: TicketService, useValue: ticketServiceStub },
        { provide: AuditLogService, useValue: auditLogServiceStub }
      ]
    });
    const fixture = TestBed.createComponent(TicketDetailComponent);
    fixture.componentRef.setInput('ticket', ticket());
    fixture.detectChanges();
    component = fixture.componentInstance;
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

  it('sets an error and stops when assignToMe fails', () => {
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
});
