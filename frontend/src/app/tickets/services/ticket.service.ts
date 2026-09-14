import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { PagedResponse } from '../../shared/models/paged-response.model';
import { MyTicketListParams, Ticket, TicketListParams, TicketRequest, TicketStats } from '../models/ticket.models';

@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/tickets`;

  list(params: TicketListParams = {}): Observable<PagedResponse<Ticket>> {
    return this.http.get<PagedResponse<Ticket>>(this.baseUrl, { params: buildParams(params) });
  }

  getStats(): Observable<TicketStats> {
    return this.http.get<TicketStats>(`${this.baseUrl}/stats`);
  }

  assignToMe(id: number): Observable<Ticket> {
    return this.http.put<Ticket>(`${this.baseUrl}/${id}/assign-to-me`, {});
  }

  unassign(id: number): Observable<Ticket> {
    return this.http.put<Ticket>(`${this.baseUrl}/${id}/unassign`, {});
  }

  getById(id: number): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.baseUrl}/${id}`);
  }

  create(request: TicketRequest): Observable<Ticket> {
    return this.http.post<Ticket>(this.baseUrl, request);
  }

  getMine(params: MyTicketListParams = {}): Observable<PagedResponse<Ticket>> {
    return this.http.get<PagedResponse<Ticket>>(`${this.baseUrl}/mine`, { params: buildParams(params) });
  }

  getMyStats(): Observable<TicketStats> {
    return this.http.get<TicketStats>(`${this.baseUrl}/mine/stats`);
  }
}

function buildParams(params: object): HttpParams {
  let httpParams = new HttpParams();
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      httpParams = httpParams.set(key, String(value));
    }
  }
  return httpParams;
}
