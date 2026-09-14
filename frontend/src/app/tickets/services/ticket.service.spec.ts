import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { TicketService } from './ticket.service';
import { PagedResponse } from '../../shared/models/paged-response.model';
import { Ticket, TicketStats } from '../models/ticket.models';
import { environment } from '../../../environments/environment';

describe('TicketService', () => {
  let service: TicketService;
  let httpMock: HttpTestingController;

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
    updatedAt: '2026-09-07T10:00:00'
  };

  const mockPagedResponse: PagedResponse<Ticket> = {
    content: [mockTicket],
    page: 0,
    size: 20,
    totalElements: 1,
    totalPages: 1
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(TicketService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('fetches a page of tickets from GET /api/tickets with no filters', () => {
    let result: PagedResponse<Ticket> | undefined;
    service.list().subscribe((page) => (result = page));

    const req = httpMock.expectOne((r) => r.url === `${environment.apiUrl}/api/tickets`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys().length).toBe(0);
    req.flush(mockPagedResponse);

    expect(result).toEqual(mockPagedResponse);
  });

  it('sends status, priority, search and page params on GET /api/tickets', () => {
    service
      .list({ status: 'NEW', priority: 'HIGH', categoryId: 3, search: 'vpn', page: 2, size: 10 })
      .subscribe();

    const req = httpMock.expectOne((r) => r.url === `${environment.apiUrl}/api/tickets`);
    expect(req.request.params.get('status')).toBe('NEW');
    expect(req.request.params.get('priority')).toBe('HIGH');
    expect(req.request.params.get('categoryId')).toBe('3');
    expect(req.request.params.get('search')).toBe('vpn');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('10');
    req.flush(mockPagedResponse);
  });

  it('fetches global stats from GET /api/tickets/stats', () => {
    const mockStats: TicketStats = { abiertas: 4, criticas: 1, byStatus: { NEW: 2, IN_PROGRESS: 2 } };

    let result: TicketStats | undefined;
    service.getStats().subscribe((stats) => (result = stats));

    const req = httpMock.expectOne(`${environment.apiUrl}/api/tickets/stats`);
    expect(req.request.method).toBe('GET');
    req.flush(mockStats);

    expect(result).toEqual(mockStats);
  });

  it('assigns a ticket to the current user via PUT /api/tickets/{id}/assign-to-me', () => {
    let result: Ticket | undefined;
    service.assignToMe(1).subscribe((ticket) => (result = ticket));

    const req = httpMock.expectOne(`${environment.apiUrl}/api/tickets/1/assign-to-me`);
    expect(req.request.method).toBe('PUT');
    req.flush(mockTicket);

    expect(result).toEqual(mockTicket);
  });

  it('fetches a single ticket from GET /api/tickets/{id}', () => {
    let result: Ticket | undefined;
    service.getById(1).subscribe((ticket) => (result = ticket));

    const req = httpMock.expectOne(`${environment.apiUrl}/api/tickets/1`);
    expect(req.request.method).toBe('GET');
    req.flush(mockTicket);

    expect(result).toEqual(mockTicket);
  });

  it('releases the assignment via PUT /api/tickets/{id}/unassign', () => {
    let result: Ticket | undefined;
    service.unassign(1).subscribe((ticket) => (result = ticket));

    const req = httpMock.expectOne(`${environment.apiUrl}/api/tickets/1/unassign`);
    expect(req.request.method).toBe('PUT');
    req.flush(mockTicket);

    expect(result).toEqual(mockTicket);
  });

  it('creates a ticket via POST /api/tickets', () => {
    let result: Ticket | undefined;
    service.create({ title: 'No puedo acceder al VPN', description: 'Pide credenciales invalidas' }).subscribe((t) => (result = t));

    const req = httpMock.expectOne(`${environment.apiUrl}/api/tickets`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ title: 'No puedo acceder al VPN', description: 'Pide credenciales invalidas' });
    req.flush(mockTicket);

    expect(result).toEqual(mockTicket);
  });

  it('fetches a page of the tickets created by the current user from GET /api/tickets/mine', () => {
    let result: PagedResponse<Ticket> | undefined;
    service.getMine().subscribe((page) => (result = page));

    const req = httpMock.expectOne((r) => r.url === `${environment.apiUrl}/api/tickets/mine`);
    expect(req.request.method).toBe('GET');
    req.flush(mockPagedResponse);

    expect(result).toEqual(mockPagedResponse);
  });

  it('sends status, priority, search and page params on GET /api/tickets/mine', () => {
    service.getMine({ status: 'RESOLVED', priority: 'LOW', search: 'impresora', page: 1, size: 5 }).subscribe();

    const req = httpMock.expectOne((r) => r.url === `${environment.apiUrl}/api/tickets/mine`);
    expect(req.request.params.get('status')).toBe('RESOLVED');
    expect(req.request.params.get('priority')).toBe('LOW');
    expect(req.request.params.get('search')).toBe('impresora');
    expect(req.request.params.get('page')).toBe('1');
    expect(req.request.params.get('size')).toBe('5');
    req.flush(mockPagedResponse);
  });

  it('fetches the current user stats from GET /api/tickets/mine/stats', () => {
    const mockStats: TicketStats = { abiertas: 2, criticas: 0, byStatus: { NEW: 2 } };

    let result: TicketStats | undefined;
    service.getMyStats().subscribe((stats) => (result = stats));

    const req = httpMock.expectOne(`${environment.apiUrl}/api/tickets/mine/stats`);
    expect(req.request.method).toBe('GET');
    req.flush(mockStats);

    expect(result).toEqual(mockStats);
  });
});
