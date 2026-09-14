import { TestBed } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { of, throwError } from 'rxjs';

import { ApprovalsPageComponent } from './approvals-page.component';
import { AuditLogService } from '../../audit-logs/services/audit-log.service';
import { TicketService } from '../../tickets/services/ticket.service';
import { AuditLog } from '../../audit-logs/models/audit-log.models';
import { PagedResponse } from '../../shared/models/paged-response.model';
import { Ticket } from '../../tickets/models/ticket.models';
import { esPaginatorIntl } from '../../shared/i18n/paginator-intl-es';

function log(overrides: Partial<AuditLog> = {}): AuditLog {
  return {
    id: 3,
    ticketId: 9,
    action: 'PROPUESTA_IA',
    reason: 'Reiniciar el servicio de VPN',
    employeeMessage: null,
    resultStatus: 'PENDING',
    approvedByName: null,
    createdAt: '2026-09-07T10:00:00',
    ...overrides
  };
}

function pagedResponse(content: AuditLog[], overrides: Partial<PagedResponse<AuditLog>> = {}): PagedResponse<AuditLog> {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: 1, ...overrides };
}

function ticket(overrides: Partial<Ticket> = {}): Ticket {
  return {
    id: 9,
    title: 'VPN caída',
    description: 'desc',
    status: 'PENDING_APPROVAL',
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

describe('ApprovalsPageComponent', () => {
  let auditLogServiceStub: { listPending: jasmine.Spy; resolve: jasmine.Spy; undo: jasmine.Spy };
  let ticketServiceStub: { getById: jasmine.Spy };
  let component: ApprovalsPageComponent;
  let fixture: ReturnType<typeof TestBed.createComponent<ApprovalsPageComponent>>;

  function createComponent(): void {
    TestBed.configureTestingModule({
      imports: [ApprovalsPageComponent],
      providers: [
        provideNoopAnimations(),
        { provide: AuditLogService, useValue: auditLogServiceStub },
        { provide: TicketService, useValue: ticketServiceStub },
        { provide: MatPaginatorIntl, useFactory: esPaginatorIntl }
      ]
    });
    fixture = TestBed.createComponent(ApprovalsPageComponent);
    fixture.detectChanges();
    component = fixture.componentInstance;
  }

  beforeEach(() => {
    auditLogServiceStub = {
      listPending: jasmine.createSpy('listPending').and.returnValue(of(pagedResponse([]))),
      resolve: jasmine.createSpy('resolve'),
      undo: jasmine.createSpy('undo')
    };
    ticketServiceStub = { getById: jasmine.createSpy('getById').and.returnValue(of(ticket())) };
  });

  it('loads pending logs and enriches each card with its ticket', () => {
    auditLogServiceStub.listPending.and.returnValue(of(pagedResponse([log()])));

    createComponent();

    expect(auditLogServiceStub.listPending).toHaveBeenCalledWith(0, 20);
    expect(ticketServiceStub.getById).toHaveBeenCalledWith(9);
    expect(component.cards().length).toBe(1);
    expect(component.cards()[0].ticket).toEqual(ticket());
    expect(component.loading()).toBeFalse();
  });

  it('shows the paginator labels in Spanish', () => {
    auditLogServiceStub.listPending.and.returnValue(of(pagedResponse([log()])));

    createComponent();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Elementos por página');
    expect(text).not.toContain('Items per page');
  });

  it('exposes the total element count for the paginator', () => {
    auditLogServiceStub.listPending.and.returnValue(of(pagedResponse([log()], { totalElements: 7 })));

    createComponent();

    expect(component.totalElements()).toBe(7);
  });

  it('requests the selected page and size when the paginator changes', () => {
    createComponent();
    auditLogServiceStub.listPending.calls.reset();

    component.onPage({ pageIndex: 1, pageSize: 10, length: 30 } as any);

    expect(component.page()).toBe(1);
    expect(component.pageSize()).toBe(10);
    expect(auditLogServiceStub.listPending).toHaveBeenCalledWith(1, 10);
  });

  it('shows the Spanish label for a known AI action, not the raw enum value', () => {
    auditLogServiceStub.listPending.and.returnValue(of(pagedResponse([log({ action: 'CLOSE' })])));

    createComponent();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Cerrar');
    expect(text).not.toContain('CLOSE');
  });

  it('shows a free-text technician action unchanged', () => {
    auditLogServiceStub.listPending.and.returnValue(of(pagedResponse([log({ action: 'REINICIO_SERVICIO_VPN' })])));

    createComponent();

    expect(fixture.nativeElement.textContent).toContain('REINICIO_SERVICIO_VPN');
  });

  it('keeps the card usable without ticket data when the ticket fetch fails', () => {
    auditLogServiceStub.listPending.and.returnValue(of(pagedResponse([log()])));
    ticketServiceStub.getById.and.returnValue(throwError(() => new Error('boom')));

    createComponent();

    expect(component.cards().length).toBe(1);
    expect(component.cards()[0].ticket).toBeNull();
  });

  it('sets an error when listPending fails', () => {
    auditLogServiceStub.listPending.and.returnValue(throwError(() => new Error('boom')));

    createComponent();

    expect(component.error()).not.toBeNull();
    expect(component.loading()).toBeFalse();
  });

  it('approves a card, keeps it visible marked as resolved, and offers undo', () => {
    auditLogServiceStub.listPending.and.returnValue(of(pagedResponse([log()])));
    createComponent();
    const resolvedLog = log({ resultStatus: 'APPROVED', approvedByName: 'Jefe IT' });
    auditLogServiceStub.resolve.and.returnValue(of(resolvedLog));

    component.approve(component.cards()[0]);

    expect(auditLogServiceStub.resolve).toHaveBeenCalledWith(3, { resultStatus: 'APPROVED' });
    expect(component.cards().length).toBe(1);
    expect(component.cards()[0].log.resultStatus).toBe('APPROVED');
    expect(component.cards()[0].processing).toBeFalse();
  });

  it('rejects a card and keeps it visible marked as resolved', () => {
    auditLogServiceStub.listPending.and.returnValue(of(pagedResponse([log()])));
    createComponent();
    const resolvedLog = log({ resultStatus: 'REJECTED', approvedByName: 'Jefe IT' });
    auditLogServiceStub.resolve.and.returnValue(of(resolvedLog));

    component.reject(component.cards()[0]);

    expect(auditLogServiceStub.resolve).toHaveBeenCalledWith(3, { resultStatus: 'REJECTED' });
    expect(component.cards()[0].log.resultStatus).toBe('REJECTED');
  });

  it('sets a card-level error and stops when resolve fails', () => {
    auditLogServiceStub.listPending.and.returnValue(of(pagedResponse([log()])));
    createComponent();
    auditLogServiceStub.resolve.and.returnValue(throwError(() => new Error('boom')));

    component.approve(component.cards()[0]);

    expect(component.cards()[0].error).not.toBeNull();
    expect(component.cards()[0].processing).toBeFalse();
    expect(component.cards()[0].log.resultStatus).toBe('PENDING');
  });

  it('undoes a resolved card back to pending', () => {
    auditLogServiceStub.listPending.and.returnValue(
      of(pagedResponse([log({ resultStatus: 'APPROVED', approvedByName: 'Jefe IT' })]))
    );
    createComponent();
    const undoneLog = log({ resultStatus: 'PENDING', approvedByName: null });
    auditLogServiceStub.undo.and.returnValue(of(undoneLog));

    component.undoCard(component.cards()[0]);

    expect(auditLogServiceStub.undo).toHaveBeenCalledWith(3);
    expect(component.cards()[0].log.resultStatus).toBe('PENDING');
  });
});
