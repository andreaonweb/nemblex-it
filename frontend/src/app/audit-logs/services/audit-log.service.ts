import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PagedResponse } from '../../shared/models/paged-response.model';
import { AuditLog, AuditLogApprovalRequest, AuditLogRequest } from '../models/audit-log.models';

@Injectable({ providedIn: 'root' })
export class AuditLogService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/audit-logs`;

  listByTicket(ticketId: number): Observable<AuditLog[]> {
    return this.http.get<AuditLog[]>(`${this.baseUrl}/ticket/${ticketId}`);
  }

  resolveNow(request: AuditLogRequest): Observable<AuditLog> {
    return this.http.post<AuditLog>(`${this.baseUrl}/resolve-now`, request);
  }

  listPending(page = 0, size = 20): Observable<PagedResponse<AuditLog>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http.get<PagedResponse<AuditLog>>(`${this.baseUrl}/pending`, { params });
  }

  resolve(id: number, request: AuditLogApprovalRequest): Observable<AuditLog> {
    return this.http.put<AuditLog>(`${this.baseUrl}/${id}/resolve`, request);
  }

  undo(id: number): Observable<AuditLog> {
    return this.http.put<AuditLog>(`${this.baseUrl}/${id}/undo`, {});
  }
}
