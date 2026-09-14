import { Component, OnInit, inject, signal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { catchError, of } from 'rxjs';

import { AuditLogService } from '../../audit-logs/services/audit-log.service';
import { TicketService } from '../../tickets/services/ticket.service';
import { AuditLog } from '../../audit-logs/models/audit-log.models';
import { actionLabel } from '../../audit-logs/models/audit-log-labels';
import { Ticket } from '../../tickets/models/ticket.models';
import { PRIORITY_COLORS, PRIORITY_LABELS } from '../../tickets/models/ticket-labels';

export interface ApprovalCardState {
  log: AuditLog;
  ticket: Ticket | null;
  processing: boolean;
  error: string | null;
}

@Component({
  selector: 'app-approvals-page',
  standalone: true,
  imports: [MatButtonModule, MatCardModule, MatPaginatorModule, MatProgressSpinnerModule],
  templateUrl: './approvals-page.component.html',
  styleUrl: './approvals-page.component.scss'
})
export class ApprovalsPageComponent implements OnInit {
  private readonly auditLogService = inject(AuditLogService);
  private readonly ticketService = inject(TicketService);

  protected readonly priorityLabels = PRIORITY_LABELS;
  protected readonly priorityColors = PRIORITY_COLORS;
  protected readonly pageSizeOptions = [10, 20, 50];
  protected readonly actionLabel = actionLabel;

  readonly cards = signal<ApprovalCardState[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly page = signal(0);
  readonly pageSize = signal(20);
  readonly totalElements = signal(0);

  ngOnInit(): void {
    this.loadPending();
  }

  private loadPending(): void {
    this.loading.set(true);
    this.error.set(null);
    this.auditLogService.listPending(this.page(), this.pageSize()).subscribe({
      next: (response) => {
        this.cards.set(response.content.map((log) => ({ log, ticket: null, processing: false, error: null })));
        this.totalElements.set(response.totalElements);
        this.loading.set(false);
        response.content.forEach((log) => this.loadTicket(log.ticketId));
      },
      error: () => {
        this.error.set('No se pudo cargar la cola de aprobaciones.');
        this.loading.set(false);
      }
    });
  }

  onPage(event: PageEvent): void {
    this.page.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadPending();
  }

  private loadTicket(ticketId: number): void {
    this.ticketService
      .getById(ticketId)
      .pipe(catchError(() => of(null)))
      .subscribe((ticket) => {
        this.cards.update((current) =>
          current.map((card) => (card.log.ticketId === ticketId ? { ...card, ticket } : card))
        );
      });
  }

  approve(card: ApprovalCardState): void {
    this.resolveCard(card, 'APPROVED');
  }

  reject(card: ApprovalCardState): void {
    this.resolveCard(card, 'REJECTED');
  }

  private resolveCard(card: ApprovalCardState, resultStatus: 'APPROVED' | 'REJECTED'): void {
    this.updateCard(card.log.id, { processing: true, error: null });
    this.auditLogService.resolve(card.log.id, { resultStatus }).subscribe({
      next: (updatedLog) => this.updateCard(card.log.id, { log: updatedLog, processing: false }),
      error: () => this.updateCard(card.log.id, { processing: false, error: 'No se pudo registrar la decisión.' })
    });
  }

  undoCard(card: ApprovalCardState): void {
    this.updateCard(card.log.id, { processing: true, error: null });
    this.auditLogService.undo(card.log.id).subscribe({
      next: (updatedLog) => this.updateCard(card.log.id, { log: updatedLog, processing: false }),
      error: () => this.updateCard(card.log.id, { processing: false, error: 'No se pudo deshacer la resolución.' })
    });
  }

  private updateCard(logId: number, changes: Partial<ApprovalCardState>): void {
    this.cards.update((current) => current.map((c) => (c.log.id === logId ? { ...c, ...changes } : c)));
  }
}
