import { Component, EventEmitter, Output, effect, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';

import { TicketService } from '../services/ticket.service';
import { AuditLogService } from '../../audit-logs/services/audit-log.service';
import { Ticket } from '../models/ticket.models';
import { AuditLog } from '../../audit-logs/models/audit-log.models';
import { PRIORITY_COLORS, PRIORITY_LABELS, STATUS_COLORS, STATUS_LABELS } from '../models/ticket-labels';

@Component({
  selector: 'app-ticket-detail',
  standalone: true,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule,
    MatTabsModule
  ],
  templateUrl: './ticket-detail.component.html',
  styleUrl: './ticket-detail.component.scss'
})
export class TicketDetailComponent {
  private readonly ticketService = inject(TicketService);
  private readonly auditLogService = inject(AuditLogService);
  private readonly fb = inject(FormBuilder);

  readonly ticket = input.required<Ticket>();

  @Output() readonly ticketChanged = new EventEmitter<Ticket>();
  @Output() readonly closed = new EventEmitter<void>();

  protected readonly statusLabels = STATUS_LABELS;
  protected readonly statusColors = STATUS_COLORS;
  protected readonly priorityLabels = PRIORITY_LABELS;
  protected readonly priorityColors = PRIORITY_COLORS;

  readonly activity = signal<AuditLog[]>([]);
  readonly loadingActivity = signal(false);

  readonly assigning = signal(false);
  readonly assignError = signal<string | null>(null);

  readonly resolveOpen = signal(false);
  readonly resolving = signal(false);
  readonly resolveError = signal<string | null>(null);

  readonly resolveForm = this.fb.nonNullable.group({
    action: ['', Validators.required],
    reason: ['']
  });

  constructor() {
    effect(
      () => {
        const currentTicket = this.ticket();
        this.loadActivity(currentTicket.id);
        this.resolveOpen.set(false);
        this.resolveForm.reset({ action: '', reason: '' });
      },
      { allowSignalWrites: true }
    );
  }

  private loadActivity(ticketId: number): void {
    this.loadingActivity.set(true);
    this.auditLogService.listByTicket(ticketId).subscribe({
      next: (logs) => {
        this.activity.set(logs);
        this.loadingActivity.set(false);
      },
      error: () => this.loadingActivity.set(false)
    });
  }

  assignToMe(): void {
    this.assigning.set(true);
    this.assignError.set(null);
    this.ticketService.assignToMe(this.ticket().id).subscribe({
      next: (updated) => {
        this.assigning.set(false);
        this.ticketChanged.emit(updated);
      },
      error: () => {
        this.assigning.set(false);
        this.assignError.set('No se pudo asignar la incidencia.');
      }
    });
  }

  toggleResolveForm(): void {
    this.resolveOpen.set(!this.resolveOpen());
    this.resolveError.set(null);
  }

  submitResolve(): void {
    if (this.resolveForm.invalid) {
      this.resolveForm.markAllAsTouched();
      return;
    }

    this.resolving.set(true);
    this.resolveError.set(null);
    const { action, reason } = this.resolveForm.getRawValue();

    this.auditLogService.resolveNow({ ticketId: this.ticket().id, action, reason }).subscribe({
      next: () => {
        this.resolving.set(false);
        this.resolveOpen.set(false);
        this.resolveForm.reset({ action: '', reason: '' });
        this.ticketChanged.emit({ ...this.ticket(), status: 'RESOLVED' });
        this.loadActivity(this.ticket().id);
      },
      error: () => {
        this.resolving.set(false);
        this.resolveError.set('No se pudo resolver la incidencia.');
      }
    });
  }
}
