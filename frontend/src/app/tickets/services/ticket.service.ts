import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

import { environment } from '../../../environments/environment';
import { Ticket } from '../models/ticket.models';

@Injectable({ providedIn: 'root' })
export class TicketService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = `${environment.apiUrl}/api/tickets`;

  list(): Observable<Ticket[]> {
    return this.http.get<Ticket[]>(this.baseUrl);
  }

  assignToMe(id: number): Observable<Ticket> {
    return this.http.put<Ticket>(`${this.baseUrl}/${id}/assign-to-me`, {});
  }

  getById(id: number): Observable<Ticket> {
    return this.http.get<Ticket>(`${this.baseUrl}/${id}`);
  }
}
