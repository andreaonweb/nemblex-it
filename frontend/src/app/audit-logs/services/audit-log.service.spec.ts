import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AuditLogService } from './audit-log.service';
import { AuditLog } from '../models/audit-log.models';
import { PagedResponse } from '../../shared/models/paged-response.model';
import { environment } from '../../../environments/environment';

describe('AuditLogService', () => {
  let service: AuditLogService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuditLogService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => httpMock.verify());

  it('fetches the audit trail for a ticket from GET /api/audit-logs/ticket/{id}', () => {
    const mockLogs: AuditLog[] = [
      {
        id: 1,
        ticketId: 9,
        action: 'REINICIO_SERVICIO',
        reason: 'Se reinició el servicio',
        employeeMessage: null,
        resultStatus: 'APPROVED',
        approvedByName: 'Ana Torres',
        createdAt: '2026-09-07T10:00:00'
      }
    ];

    let result: AuditLog[] | undefined;
    service.listByTicket(9).subscribe((logs) => (result = logs));

    const req = httpMock.expectOne(`${environment.apiUrl}/api/audit-logs/ticket/9`);
    expect(req.request.method).toBe('GET');
    req.flush(mockLogs);

    expect(result).toEqual(mockLogs);
  });

  it('posts a manual resolution to POST /api/audit-logs/resolve-now', () => {
    const mockLog: AuditLog = {
      id: 2,
      ticketId: 9,
      action: 'REINICIO_SERVICIO',
      reason: 'Se reinició el servicio',
      employeeMessage: null,
      resultStatus: 'APPROVED',
      approvedByName: 'Ana Torres',
      createdAt: '2026-09-07T10:00:00'
    };

    let result: AuditLog | undefined;
    service.resolveNow({ ticketId: 9, action: 'REINICIO_SERVICIO', reason: 'Se reinició el servicio' }).subscribe((log) => (result = log));

    const req = httpMock.expectOne(`${environment.apiUrl}/api/audit-logs/resolve-now`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ ticketId: 9, action: 'REINICIO_SERVICIO', reason: 'Se reinició el servicio' });
    req.flush(mockLog);

    expect(result).toEqual(mockLog);
  });

  it('fetches a page of pending audit logs from GET /api/audit-logs/pending', () => {
    const mockPage: PagedResponse<AuditLog> = {
      content: [
        {
          id: 3,
          ticketId: 9,
          action: 'PROPUESTA_IA',
          reason: 'Reiniciar el servicio de VPN',
          employeeMessage: null,
          resultStatus: 'PENDING',
          approvedByName: null,
          createdAt: '2026-09-07T10:00:00'
        }
      ],
      page: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1
    };

    let result: PagedResponse<AuditLog> | undefined;
    service.listPending().subscribe((page) => (result = page));

    const req = httpMock.expectOne((r) => r.url === `${environment.apiUrl}/api/audit-logs/pending`);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('size')).toBe('20');
    req.flush(mockPage);

    expect(result).toEqual(mockPage);
  });

  it('sends the requested page and size on GET /api/audit-logs/pending', () => {
    service.listPending(2, 5).subscribe();

    const req = httpMock.expectOne((r) => r.url === `${environment.apiUrl}/api/audit-logs/pending`);
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('5');
    req.flush({ content: [], page: 2, size: 5, totalElements: 0, totalPages: 0 });
  });

  it('approves or rejects a pending log via PUT /api/audit-logs/{id}/resolve', () => {
    const mockLog: AuditLog = {
      id: 3,
      ticketId: 9,
      action: 'PROPUESTA_IA',
      reason: 'Reiniciar el servicio de VPN',
      employeeMessage: null,
      resultStatus: 'APPROVED',
      approvedByName: 'Jefe IT',
      createdAt: '2026-09-07T10:00:00'
    };

    let result: AuditLog | undefined;
    service.resolve(3, { resultStatus: 'APPROVED' }).subscribe((log) => (result = log));

    const req = httpMock.expectOne(`${environment.apiUrl}/api/audit-logs/3/resolve`);
    expect(req.request.method).toBe('PUT');
    expect(req.request.body).toEqual({ resultStatus: 'APPROVED' });
    req.flush(mockLog);

    expect(result).toEqual(mockLog);
  });

  it('undoes a resolved log via PUT /api/audit-logs/{id}/undo', () => {
    const mockLog: AuditLog = {
      id: 3,
      ticketId: 9,
      action: 'PROPUESTA_IA',
      reason: 'Reiniciar el servicio de VPN',
      employeeMessage: null,
      resultStatus: 'PENDING',
      approvedByName: null,
      createdAt: '2026-09-07T10:00:00'
    };

    let result: AuditLog | undefined;
    service.undo(3).subscribe((log) => (result = log));

    const req = httpMock.expectOne(`${environment.apiUrl}/api/audit-logs/3/undo`);
    expect(req.request.method).toBe('PUT');
    req.flush(mockLog);

    expect(result).toEqual(mockLog);
  });
});
