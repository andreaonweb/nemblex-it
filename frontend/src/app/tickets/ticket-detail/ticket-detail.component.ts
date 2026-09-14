import { Component, EventEmitter, Output, computed, effect, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatTabsModule } from '@angular/material/tabs';

import { TicketService } from '../services/ticket.service';
import { AuditLogService } from '../../audit-logs/services/audit-log.service';
import { AuthService } from '../../auth/services/auth.service';
import { Ticket } from '../models/ticket.models';
import { AuditLog } from '../../audit-logs/models/audit-log.models';
import { actionLabel, RESULT_STATUS_LABELS } from '../../audit-logs/models/audit-log-labels';
import { PRIORITY_COLORS, PRIORITY_LABELS, STATUS_COLORS, STATUS_LABELS } from '../models/ticket-labels';

function extractErrorMessage(err: unknown, fallback: string): string {
  const message = (err as { error?: { message?: string } } | undefined)?.error?.message;
  return message ?? fallback;
}

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
  private readonly authService = inject(AuthService);
  private readonly fb = inject(FormBuilder);

  readonly ticket = input.required<Ticket>();

  @Output() readonly ticketChanged = new EventEmitter<Ticket>();
  @Output() readonly closed = new EventEmitter<void>();

  protected readonly statusLabels = STATUS_LABELS;
  protected readonly statusColors = STATUS_COLORS;
  protected readonly priorityLabels = PRIORITY_LABELS;
  protected readonly priorityColors = PRIORITY_COLORS;
  protected readonly resultStatusLabels = RESULT_STATUS_LABELS;
  protected readonly actionLabel = actionLabel;

  readonly activity = signal<AuditLog[]>([]);
  readonly loadingActivity = signal(false);

  readonly assigning = signal(false);
  readonly assignError = signal<string | null>(null);

  readonly unassigning = signal(false);
  readonly unassignError = signal<string | null>(null);

  readonly canUnassign = computed(() => {
    const assignedToId = this.ticket().assignedToId;
    if (assignedToId === null) {
      return false;
    }
    const user = this.authService.currentUser();
    if (!user) {
      return false;
    }
    return user.id === assignedToId || user.role === 'SUPERVISOR' || user.role === 'ADMIN';
  });

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
      error: (err) => {
        this.assigning.set(false);
        this.assignError.set(extractErrorMessage(err, 'No se pudo asignar la incidencia.'));
      }
    });
  }

  unassignTicket(): void {
    this.unassigning.set(true);
    this.unassignError.set(null);
    this.ticketService.unassign(this.ticket().id).subscribe({
      next: (updated) => {
        this.unassigning.set(false);
        this.ticketChanged.emit(updated);
      },
      error: (err) => {
        this.unassigning.set(false);
        this.unassignError.set(extractErrorMessage(err, 'No se pudo liberar la asignación.'));
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
