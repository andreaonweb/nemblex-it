import { Component, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { TicketService } from '../services/ticket.service';
import { AuditLogService } from '../../audit-logs/services/audit-log.service';
import { Ticket } from '../models/ticket.models';
import { AuditLog } from '../../audit-logs/models/audit-log.models';
import { PRIORITY_LABELS, STATUS_COLORS, STATUS_LABELS } from '../models/ticket-labels';

@Component({
  selector: 'app-my-tickets-page',
  standalone: true,
  imports: [
    DatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressSpinnerModule
  ],
  templateUrl: './my-tickets-page.component.html',
  styleUrl: './my-tickets-page.component.scss'
})
export class MyTicketsPageComponent implements OnInit {
  private readonly ticketService = inject(TicketService);
  private readonly auditLogService = inject(AuditLogService);
  private readonly fb = inject(FormBuilder);

  protected readonly statusLabels = STATUS_LABELS;
  protected readonly statusColors = STATUS_COLORS;
  protected readonly priorityLabels = PRIORITY_LABELS;

  readonly tickets = signal<Ticket[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly formOpen = signal(false);
  readonly submitting = signal(false);
  readonly submitError = signal<string | null>(null);

  readonly expandedTicketId = signal<number | null>(null);
  readonly activity = signal<AuditLog[]>([]);
  readonly activityLoading = signal(false);

  readonly form = this.fb.nonNullable.group({
    title: ['', Validators.required],
    description: ['', Validators.required]
  });

  ngOnInit(): void {
    this.loadTickets();
  }

  private loadTickets(): void {
    this.loading.set(true);
    this.error.set(null);
    this.ticketService.getMine().subscribe({
      next: (tickets) => {
        this.tickets.set(tickets);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar tus incidencias.');
        this.loading.set(false);
      }
    });
  }

  toggleForm(): void {
    this.formOpen.set(!this.formOpen());
    this.submitError.set(null);
  }

  submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting.set(true);
    this.submitError.set(null);

    this.ticketService.create(this.form.getRawValue()).subscribe({
      next: (created) => {
        this.submitting.set(false);
        this.formOpen.set(false);
        this.form.reset({ title: '', description: '' });
        this.tickets.update((current) => [created, ...current]);
      },
      error: () => {
        this.submitting.set(false);
        this.submitError.set('No se pudo crear el ticket.');
      }
    });
  }

  toggleTicket(ticketId: number): void {
    if (this.expandedTicketId() === ticketId) {
      this.expandedTicketId.set(null);
      return;
    }

    this.expandedTicketId.set(ticketId);
    this.activity.set([]);
    this.activityLoading.set(true);
    this.auditLogService.listByTicket(ticketId).subscribe({
      next: (logs) => {
        this.activity.set(logs);
        this.activityLoading.set(false);
      },
      error: () => this.activityLoading.set(false)
    });
  }

  supportLabel(entry: AuditLog): string | null {
    return entry.resultStatus === 'APPROVED' ? 'Revisado y aprobado por el equipo de soporte' : null;
  }
}
