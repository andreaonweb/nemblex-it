import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { AuditLog, AuditLogRequest } from '../models/audit-log.models';

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
}
