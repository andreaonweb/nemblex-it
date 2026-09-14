import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { of, throwError } from 'rxjs';

import { MyTicketsPageComponent } from './my-tickets-page.component';
import { TicketService } from '../services/ticket.service';
import { AuditLogService } from '../../audit-logs/services/audit-log.service';
import { PagedResponse } from '../../shared/models/paged-response.model';
import { Ticket } from '../models/ticket.models';
import { AuditLog } from '../../audit-logs/models/audit-log.models';
import { esPaginatorIntl } from '../../shared/i18n/paginator-intl-es';

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

function pagedResponse(content: Ticket[], overrides: Partial<PagedResponse<Ticket>> = {}): PagedResponse<Ticket> {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: 1, ...overrides };
}

function log(overrides: Partial<AuditLog> = {}): AuditLog {
  return {
    id: 1,
    ticketId: 1,
    action: 'CLOSE',
    reason: 'Ticket duplicado',
    employeeMessage: 'Ya identificamos este problema, no necesitás hacer nada más.',
    resultStatus: 'APPROVED',
    approvedByName: 'Beatriz Ruiz',
    createdAt: '2026-09-08T10:05:00',
    ...overrides
  };
}

describe('MyTicketsPageComponent', () => {
  let ticketServiceStub: { getMine: jasmine.Spy; create: jasmine.Spy };
  let auditLogServiceStub: { listByTicket: jasmine.Spy };
  let component: MyTicketsPageComponent;
  let fixture: ReturnType<typeof TestBed.createComponent<MyTicketsPageComponent>>;

  function createComponent(): void {
    TestBed.configureTestingModule({
      imports: [MyTicketsPageComponent],
      providers: [
        provideNoopAnimations(),
        { provide: TicketService, useValue: ticketServiceStub },
        { provide: AuditLogService, useValue: auditLogServiceStub },
        { provide: MatPaginatorIntl, useFactory: esPaginatorIntl }
      ]
    });
    fixture = TestBed.createComponent(MyTicketsPageComponent);
    fixture.detectChanges();
    component = fixture.componentInstance;
  }

  beforeEach(() => {
    ticketServiceStub = {
      getMine: jasmine.createSpy('getMine').and.returnValue(of(pagedResponse([]))),
      create: jasmine.createSpy('create')
    };
    auditLogServiceStub = {
      listByTicket: jasmine.createSpy('listByTicket').and.returnValue(of([] as AuditLog[]))
    };
  });

  it('loads the tickets created by the current user on init', () => {
    ticketServiceStub.getMine.and.returnValue(of(pagedResponse([ticket()])));

    createComponent();

    expect(ticketServiceStub.getMine).toHaveBeenCalled();
    expect(component.tickets()).toEqual([ticket()]);
    expect(component.totalElements()).toBe(1);
    expect(component.loading()).toBeFalse();
  });

  it('shows the paginator labels in Spanish', () => {
    ticketServiceStub.getMine.and.returnValue(of(pagedResponse([ticket()])));

    createComponent();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Elementos por página');
    expect(text).not.toContain('Items per page');
  });

  it('sets an error when loading tickets fails', () => {
    ticketServiceStub.getMine.and.returnValue(throwError(() => new Error('boom')));

    createComponent();

    expect(component.error()).not.toBeNull();
    expect(component.loading()).toBeFalse();
  });

  it('reloads from page 0 when the status filter changes', () => {
    createComponent();
    ticketServiceStub.getMine.calls.reset();

    component.onStatusFilterChange('RESOLVED');

    expect(component.statusFilter()).toBe('RESOLVED');
    expect(component.page()).toBe(0);
    expect(ticketServiceStub.getMine).toHaveBeenCalledWith(
      jasmine.objectContaining({ status: 'RESOLVED', page: 0 })
    );
  });

  it('reloads from page 0 when the priority filter changes', () => {
    createComponent();
    ticketServiceStub.getMine.calls.reset();

    component.onPriorityFilterChange('HIGH');

    expect(component.priorityFilter()).toBe('HIGH');
    expect(ticketServiceStub.getMine).toHaveBeenCalledWith(
      jasmine.objectContaining({ priority: 'HIGH', page: 0 })
    );
  });

  it('debounces search input and reloads from page 0 once settled', fakeAsync(() => {
    createComponent();
    ticketServiceStub.getMine.calls.reset();

    component.onSearchInput('vpn');
    tick(299);
    expect(ticketServiceStub.getMine).not.toHaveBeenCalled();

    tick(1);
    expect(component.search()).toBe('vpn');
    expect(ticketServiceStub.getMine).toHaveBeenCalledWith(jasmine.objectContaining({ search: 'vpn', page: 0 }));
  }));

  it('requests the selected page and size when the paginator changes', () => {
    createComponent();
    ticketServiceStub.getMine.calls.reset();

    component.onPage({ pageIndex: 2, pageSize: 10, length: 30 } as any);

    expect(component.page()).toBe(2);
    expect(component.pageSize()).toBe(10);
    expect(ticketServiceStub.getMine).toHaveBeenCalledWith(jasmine.objectContaining({ page: 2, size: 10 }));
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

  it('expands a ticket and fetches its audit trail', () => {
    ticketServiceStub.getMine.and.returnValue(of(pagedResponse([ticket()])));
    auditLogServiceStub.listByTicket.and.returnValue(of([log()]));
    createComponent();

    component.toggleTicket(1);

    expect(component.expandedTicketId()).toBe(1);
    expect(auditLogServiceStub.listByTicket).toHaveBeenCalledWith(1);
    expect(component.activity()).toEqual([log()]);
    expect(component.activityLoading()).toBeFalse();
  });

  it('collapses an expanded ticket when clicked again', () => {
    ticketServiceStub.getMine.and.returnValue(of(pagedResponse([ticket()])));
    createComponent();
    component.toggleTicket(1);

    component.toggleTicket(1);

    expect(component.expandedTicketId()).toBeNull();
  });

  it('switches to another ticket and refetches its audit trail', () => {
    ticketServiceStub.getMine.and.returnValue(of(pagedResponse([ticket(), ticket({ id: 2 })])));
    auditLogServiceStub.listByTicket.and.returnValue(of([log()]));
    createComponent();
    component.toggleTicket(1);

    component.toggleTicket(2);

    expect(component.expandedTicketId()).toBe(2);
    expect(auditLogServiceStub.listByTicket).toHaveBeenCalledWith(2);
  });

  it('leaves activity empty and stops loading when the audit trail fetch fails', () => {
    ticketServiceStub.getMine.and.returnValue(of(pagedResponse([ticket()])));
    auditLogServiceStub.listByTicket.and.returnValue(throwError(() => new Error('boom')));
    createComponent();

    component.toggleTicket(1);

    expect(component.activity()).toEqual([]);
    expect(component.activityLoading()).toBeFalse();
  });

  it('returns a generic support-team label for approved audit entries', () => {
    createComponent();

    expect(component.supportLabel(log({ resultStatus: 'APPROVED' })))
      .toBe('Revisado y aprobado por el equipo de soporte');
  });

  it('returns null as the support label for pending or rejected entries', () => {
    createComponent();

    expect(component.supportLabel(log({ resultStatus: 'PENDING' }))).toBeNull();
    expect(component.supportLabel(log({ resultStatus: 'REJECTED' }))).toBeNull();
  });

  it('polls again after the wait interval while the ticket still has no audit logs, and stops once logs appear', fakeAsync(() => {
    ticketServiceStub.getMine.and.returnValue(of(pagedResponse([ticket()])));
    auditLogServiceStub.listByTicket.and.returnValues(of([]), of([log()]));
    createComponent();

    component.toggleTicket(1);
    tick(4000);

    expect(auditLogServiceStub.listByTicket).toHaveBeenCalledTimes(2);
    expect(component.activity()).toEqual([log()]);
  }));

  it('stops polling once the ticket panel is collapsed', fakeAsync(() => {
    ticketServiceStub.getMine.and.returnValue(of(pagedResponse([ticket()])));
    auditLogServiceStub.listByTicket.and.returnValue(of([]));
    createComponent();
    component.toggleTicket(1);

    component.toggleTicket(1);
    tick(4000);

    expect(auditLogServiceStub.listByTicket).toHaveBeenCalledTimes(1);
  }));

  it('stops polling the previous ticket when another ticket is expanded', fakeAsync(() => {
    ticketServiceStub.getMine.and.returnValue(of(pagedResponse([ticket(), ticket({ id: 2 })])));
    auditLogServiceStub.listByTicket.and.returnValue(of([]));
    createComponent();
    component.toggleTicket(1);

    component.toggleTicket(2);
    tick(4000);

    expect(auditLogServiceStub.listByTicket).toHaveBeenCalledWith(1);
    expect(auditLogServiceStub.listByTicket).toHaveBeenCalledWith(2);
    expect(auditLogServiceStub.listByTicket).toHaveBeenCalledTimes(3);

    component.ngOnDestroy();
  }));

  it('cancels pending polling on destroy', fakeAsync(() => {
    ticketServiceStub.getMine.and.returnValue(of(pagedResponse([ticket()])));
    auditLogServiceStub.listByTicket.and.returnValue(of([]));
    createComponent();
    component.toggleTicket(1);

    component.ngOnDestroy();
    tick(4000);

    expect(auditLogServiceStub.listByTicket).toHaveBeenCalledTimes(1);
  }));

  it('shows the employee-facing message and never the internal reason', () => {
    ticketServiceStub.getMine.and.returnValue(of(pagedResponse([ticket()])));
    auditLogServiceStub.listByTicket.and.returnValue(of([log({
      reason: 'Duplicado del ticket #8, cierre segun procedimiento R-dup-01',
      employeeMessage: 'Ya identificamos este problema, no necesitás hacer nada más.'
    })]));
    createComponent();

    component.toggleTicket(1);
    fixture.detectChanges();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Ya identificamos este problema, no necesitás hacer nada más.');
    expect(text).not.toContain('Duplicado del ticket #8, cierre segun procedimiento R-dup-01');
  });
});
