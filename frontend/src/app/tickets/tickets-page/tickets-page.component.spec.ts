import { TestBed, fakeAsync, tick } from '@angular/core/testing';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { MatPaginatorIntl } from '@angular/material/paginator';
import { of, throwError } from 'rxjs';

import { TicketsPageComponent } from './tickets-page.component';
import { TicketService } from '../services/ticket.service';
import { PagedResponse } from '../../shared/models/paged-response.model';
import { Ticket, TicketStats } from '../models/ticket.models';
import { esPaginatorIntl } from '../../shared/i18n/paginator-intl-es';

function ticket(overrides: Partial<Ticket> = {}): Ticket {
  return {
    id: 1,
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

function pagedResponse(content: Ticket[], overrides: Partial<PagedResponse<Ticket>> = {}): PagedResponse<Ticket> {
  return { content, page: 0, size: 20, totalElements: content.length, totalPages: 1, ...overrides };
}

function stats(overrides: Partial<TicketStats> = {}): TicketStats {
  return { abiertas: 3, criticas: 1, byStatus: { NEW: 2, IN_PROGRESS: 1, RESOLVED: 5 }, ...overrides };
}

describe('TicketsPageComponent', () => {
  let ticketServiceStub: { list: jasmine.Spy; getStats: jasmine.Spy };
  let component: TicketsPageComponent;
  let fixture: ReturnType<typeof TestBed.createComponent<TicketsPageComponent>>;

  function createComponent(): void {
    TestBed.configureTestingModule({
      imports: [TicketsPageComponent],
      providers: [
        provideNoopAnimations(),
        { provide: TicketService, useValue: ticketServiceStub },
        { provide: MatPaginatorIntl, useFactory: esPaginatorIntl }
      ]
    });
    fixture = TestBed.createComponent(TicketsPageComponent);
    fixture.detectChanges();
    component = fixture.componentInstance;
  }

  beforeEach(() => {
    ticketServiceStub = {
      list: jasmine.createSpy('list').and.returnValue(of(pagedResponse([]))),
      getStats: jasmine.createSpy('getStats').and.returnValue(of(stats()))
    };
  });

  it('loads a page of tickets and the global stats on init', () => {
    ticketServiceStub.list.and.returnValue(of(pagedResponse([ticket()])));

    createComponent();

    expect(ticketServiceStub.list).toHaveBeenCalledWith(jasmine.objectContaining({ page: 0, size: 20 }));
    expect(ticketServiceStub.getStats).toHaveBeenCalled();
    expect(component.tickets()).toEqual([ticket()]);
    expect(component.totalElements()).toBe(1);
    expect(component.stats()).toEqual(stats());
    expect(component.loading()).toBeFalse();
  });

  it('shows the paginator labels in Spanish', () => {
    ticketServiceStub.list.and.returnValue(of(pagedResponse([ticket()])));

    createComponent();

    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Elementos por página');
    expect(text).not.toContain('Items per page');
  });

  it('sets an error when loading tickets fails', () => {
    ticketServiceStub.list.and.returnValue(throwError(() => new Error('boom')));

    createComponent();

    expect(component.error()).not.toBeNull();
    expect(component.loading()).toBeFalse();
  });

  it('reads dropdown counts from the server-computed stats', () => {
    createComponent();

    expect(component.statusCount('NEW')).toBe(2);
    expect(component.statusCount('IN_PROGRESS')).toBe(1);
    expect(component.statusCount('CLOSED')).toBe(0);
    expect(component.statusCount('Todos')).toBe(8);
  });

  it('reloads from page 0 when the status filter changes', () => {
    createComponent();
    ticketServiceStub.list.calls.reset();

    component.onStatusFilterChange('IN_PROGRESS');

    expect(component.statusFilter()).toBe('IN_PROGRESS');
    expect(component.page()).toBe(0);
    expect(ticketServiceStub.list).toHaveBeenCalledWith(jasmine.objectContaining({ status: 'IN_PROGRESS', page: 0 }));
  });

  it('reloads from page 0 when the priority filter changes', () => {
    createComponent();
    ticketServiceStub.list.calls.reset();

    component.onPriorityFilterChange('HIGH');

    expect(component.priorityFilter()).toBe('HIGH');
    expect(ticketServiceStub.list).toHaveBeenCalledWith(jasmine.objectContaining({ priority: 'HIGH', page: 0 }));
  });

  it('debounces search input and reloads from page 0 once settled', fakeAsync(() => {
    createComponent();
    ticketServiceStub.list.calls.reset();

    component.onSearchInput('vpn');
    tick(299);
    expect(ticketServiceStub.list).not.toHaveBeenCalled();

    tick(1);
    expect(component.search()).toBe('vpn');
    expect(ticketServiceStub.list).toHaveBeenCalledWith(jasmine.objectContaining({ search: 'vpn', page: 0 }));
  }));

  it('requests the selected page and size when the paginator changes', () => {
    createComponent();
    ticketServiceStub.list.calls.reset();

    component.onPage({ pageIndex: 3, pageSize: 50, length: 200 } as any);

    expect(component.page()).toBe(3);
    expect(component.pageSize()).toBe(50);
    expect(ticketServiceStub.list).toHaveBeenCalledWith(jasmine.objectContaining({ page: 3, size: 50 }));
  });

  it('selects and closes a ticket detail', () => {
    ticketServiceStub.list.and.returnValue(of(pagedResponse([ticket()])));
    createComponent();

    component.selectTicket(1);
    expect(component.selectedTicket()).toEqual(ticket());

    component.closeDetail();
    expect(component.selectedTicket()).toBeNull();
  });

  it('updates the local ticket and refreshes stats when a ticket changes', () => {
    ticketServiceStub.list.and.returnValue(of(pagedResponse([ticket()])));
    createComponent();
    ticketServiceStub.getStats.calls.reset();
    const updated = ticket({ status: 'RESOLVED' });

    component.onTicketChanged(updated);

    expect(component.tickets()[0]).toEqual(updated);
    expect(ticketServiceStub.getStats).toHaveBeenCalled();
  });
});
