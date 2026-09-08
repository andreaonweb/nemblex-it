import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { TicketService } from './ticket.service';
import { Ticket } from '../models/ticket.models';
import { environment } from '../../../environments/environment';

describe('TicketService', () => {
  let service: TicketService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(TicketService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('fetches all tickets from GET /api/tickets', () => {
    const mockTickets: Ticket[] = [
      {
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
        updatedAt: '2026-09-07T10:00:00'
      }
    ];

    let result: Ticket[] | undefined;
    service.list().subscribe((tickets) => (result = tickets));

    const req = httpMock.expectOne(`${environment.apiUrl}/api/tickets`);
    expect(req.request.method).toBe('GET');
    req.flush(mockTickets);

    expect(result).toEqual(mockTickets);
  });

  it('assigns a ticket to the current user via PUT /api/tickets/{id}/assign-to-me', () => {
    const mockTicket: Ticket = {
      id: 1,
      title: 'VPN caída',
      description: 'desc',
      status: 'NEW',
      priority: 'HIGH',
      categoryId: null,
      categoryName: null,
      createdById: 1,
      createdByName: 'Ana Torres',
      assignedToId: 3,
      assignedToName: 'Ana Torres',
      createdAt: '2026-09-07T10:00:00',
      updatedAt: '2026-09-07T10:05:00'
    };

    let result: Ticket | undefined;
    service.assignToMe(1).subscribe((ticket) => (result = ticket));

    const req = httpMock.expectOne(`${environment.apiUrl}/api/tickets/1/assign-to-me`);
    expect(req.request.method).toBe('PUT');
    req.flush(mockTicket);

    expect(result).toEqual(mockTicket);
  });

  it('fetches a single ticket from GET /api/tickets/{id}', () => {
    const mockTicket: Ticket = {
      id: 1,
      title: 'VPN caída',
      description: 'desc',
      status: 'PENDING_APPROVAL',
      priority: 'HIGH',
      categoryId: 2,
      categoryName: 'Redes',
      createdById: 1,
      createdByName: 'Ana Torres',
      assignedToId: 3,
      assignedToName: 'Carlos Ruiz',
      createdAt: '2026-09-07T10:00:00',
      updatedAt: '2026-09-07T10:05:00'
    };

    let result: Ticket | undefined;
    service.getById(1).subscribe((ticket) => (result = ticket));

    const req = httpMock.expectOne(`${environment.apiUrl}/api/tickets/1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockTicket);

    expect(result).toEqual(mockTicket);
  });

  it('releases the assignment via PUT /api/tickets/{id}/unassign', () => {
    const mockTicket: Ticket = {
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
      updatedAt: '2026-09-07T10:05:00'
    };

    let result: Ticket | undefined;
    service.unassign(1).subscribe((ticket) => (result = ticket));

    const req = httpMock.expectOne(`${environment.apiUrl}/api/tickets/1/unassign`);
    expect(req.request.method).toBe('PUT');
    req.flush(mockTicket);

    expect(result).toEqual(mockTicket);
  });
});
