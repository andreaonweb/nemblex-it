import { Component, OnDestroy, OnInit, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';

import { TicketService } from '../services/ticket.service';
import { AuditLogService } from '../../audit-logs/services/audit-log.service';
import { Ticket, TicketPriority, TicketStatus } from '../models/ticket.models';
import { AuditLog } from '../../audit-logs/models/audit-log.models';
import { PRIORITY_LABELS, PRIORITY_ORDER, STATUS_COLORS, STATUS_LABELS, STATUS_ORDER } from '../models/ticket-labels';

const SEARCH_DEBOUNCE_MS = 300;

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
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSelectModule
  ],
  templateUrl: './my-tickets-page.component.html',
  styleUrl: './my-tickets-page.component.scss'
})
export class MyTicketsPageComponent implements OnInit, OnDestroy {
  private static readonly ACTIVITY_POLL_INTERVAL_MS = 4000;

  private pollTimeoutId: ReturnType<typeof setTimeout> | null = null;
  private readonly searchInput$ = new Subject<string>();

  private readonly ticketService = inject(TicketService);
  private readonly auditLogService = inject(AuditLogService);
  private readonly fb = inject(FormBuilder);

  protected readonly statusLabels = STATUS_LABELS;
  protected readonly statusColors = STATUS_COLORS;
  protected readonly priorityLabels = PRIORITY_LABELS;
  protected readonly statusOptions: (TicketStatus | 'Todos')[] = ['Todos', ...STATUS_ORDER];
  protected readonly priorityOptions: (TicketPriority | 'Todas')[] = ['Todas', ...PRIORITY_ORDER];
  protected readonly pageSizeOptions = [10, 20, 50];

  readonly tickets = signal<Ticket[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly statusFilter = signal<TicketStatus | 'Todos'>('Todos');
  readonly priorityFilter = signal<TicketPriority | 'Todas'>('Todas');
  readonly search = signal('');
  readonly page = signal(0);
  readonly pageSize = signal(20);
  readonly totalElements = signal(0);

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

  constructor() {
    this.searchInput$.pipe(debounceTime(SEARCH_DEBOUNCE_MS)).subscribe((value) => {
      this.search.set(value);
      this.page.set(0);
      this.loadTickets();
    });
  }

  ngOnInit(): void {
    this.loadTickets();
  }

  private loadTickets(): void {
    this.loading.set(true);
    this.error.set(null);
    const status = this.statusFilter();
    const priority = this.priorityFilter();
    this.ticketService
      .getMine({
        status: status === 'Todos' ? undefined : status,
        priority: priority === 'Todas' ? undefined : priority,
        search: this.search() || undefined,
        page: this.page(),
        size: this.pageSize()
      })
      .subscribe({
        next: (response) => {
          this.tickets.set(response.content);
          this.totalElements.set(response.totalElements);
          this.loading.set(false);
        },
        error: () => {
          this.error.set('No se pudieron cargar tus incidencias.');
          this.loading.set(false);
        }
      });
  }

  onStatusFilterChange(value: TicketStatus | 'Todos'): void {
    this.statusFilter.set(value);
    this.page.set(0);
    this.loadTickets();
  }

  onPriorityFilterChange(value: TicketPriority | 'Todas'): void {
    this.priorityFilter.set(value);
    this.page.set(0);
    this.loadTickets();
  }

  onSearchInput(value: string): void {
    this.searchInput$.next(value);
  }

  onPage(event: PageEvent): void {
    this.page.set(event.pageIndex);
    this.pageSize.set(event.pageSize);
    this.loadTickets();
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
        this.totalElements.update((total) => total + 1);
      },
      error: () => {
        this.submitting.set(false);
        this.submitError.set('No se pudo crear el ticket.');
      }
    });
  }

  ngOnDestroy(): void {
    this.cancelPolling();
  }

  toggleTicket(ticketId: number): void {
    this.cancelPolling();

    if (this.expandedTicketId() === ticketId) {
      this.expandedTicketId.set(null);
      return;
    }

    this.expandedTicketId.set(ticketId);
    this.fetchActivity(ticketId);
  }

  supportLabel(entry: AuditLog): string | null {
    return entry.resultStatus === 'APPROVED' ? 'Revisado y aprobado por el equipo de soporte' : null;
  }

  private fetchActivity(ticketId: number, silent = false): void {
    if (!silent) {
      this.activity.set([]);
      this.activityLoading.set(true);
    }

    this.auditLogService.listByTicket(ticketId).subscribe({
      next: (logs) => {
        this.activity.set(logs);
        this.activityLoading.set(false);
        if (logs.length === 0 && this.expandedTicketId() === ticketId) {
          this.schedulePoll(ticketId);
        }
      },
      error: () => this.activityLoading.set(false)
    });
  }

  private schedulePoll(ticketId: number): void {
    this.pollTimeoutId = setTimeout(() => {
      this.pollTimeoutId = null;
      if (this.expandedTicketId() === ticketId) {
        this.fetchActivity(ticketId, true);
      }
    }, MyTicketsPageComponent.ACTIVITY_POLL_INTERVAL_MS);
  }

  private cancelPolling(): void {
    if (this.pollTimeoutId !== null) {
      clearTimeout(this.pollTimeoutId);
      this.pollTimeoutId = null;
    }
  }
}
