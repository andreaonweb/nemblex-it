import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AuditLogService } from './audit-log.service';
import { AuditLog } from '../models/audit-log.models';
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
});
