import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatPaginatorModule, PageEvent } from '@angular/material/paginator';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';
import { Subject } from 'rxjs';
import { debounceTime } from 'rxjs/operators';

import { TicketService } from '../services/ticket.service';
import { Ticket, TicketPriority, TicketStats, TicketStatus } from '../models/ticket.models';
import { PRIORITY_COLORS, PRIORITY_LABELS, PRIORITY_ORDER, STATUS_COLORS, STATUS_LABELS, STATUS_ORDER } from '../models/ticket-labels';
import { TicketDetailComponent } from '../ticket-detail/ticket-detail.component';

const SEARCH_DEBOUNCE_MS = 300;

const EMPTY_STATS: TicketStats = { abiertas: 0, criticas: 0, byStatus: {} };

@Component({
  selector: 'app-tickets-page',
  standalone: true,
  imports: [
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressSpinnerModule,
    MatSelectModule,
    TicketDetailComponent
  ],
  templateUrl: './tickets-page.component.html',
  styleUrl: './tickets-page.component.scss'
})
export class TicketsPageComponent implements OnInit {
  private readonly ticketService = inject(TicketService);
  private readonly searchInput$ = new Subject<string>();

  protected readonly statusLabels = STATUS_LABELS;
  protected readonly statusColors = STATUS_COLORS;
  protected readonly priorityLabels = PRIORITY_LABELS;
  protected readonly priorityColors = PRIORITY_COLORS;
  protected readonly statusOptions: (TicketStatus | 'Todos')[] = ['Todos', ...STATUS_ORDER];
  protected readonly priorityOptions: (TicketPriority | 'Todas')[] = ['Todas', ...PRIORITY_ORDER];
  protected readonly pageSizeOptions = [10, 20, 50];

  readonly tickets = signal<Ticket[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);
  readonly stats = signal<TicketStats>(EMPTY_STATS);

  readonly statusFilter = signal<TicketStatus | 'Todos'>('Todos');
  readonly priorityFilter = signal<TicketPriority | 'Todas'>('Todas');
  readonly search = signal('');
  readonly page = signal(0);
  readonly pageSize = signal(20);
  readonly totalElements = signal(0);
  readonly selectedId = signal<number | null>(null);

  readonly selectedTicket = computed(() => this.tickets().find((t) => t.id === this.selectedId()) ?? null);

  constructor() {
    this.searchInput$.pipe(debounceTime(SEARCH_DEBOUNCE_MS)).subscribe((value) => {
      this.search.set(value);
      this.page.set(0);
      this.loadTickets();
    });
  }

  ngOnInit(): void {
    this.loadStats();
    this.loadTickets();
  }

  private loadTickets(): void {
    this.loading.set(true);
    this.error.set(null);
    const status = this.statusFilter();
    const priority = this.priorityFilter();
    this.ticketService
      .list({
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
          this.error.set('No se pudieron cargar las incidencias.');
          this.loading.set(false);
        }
      });
  }

  private loadStats(): void {
    this.ticketService.getStats().subscribe({ next: (stats) => this.stats.set(stats) });
  }

  statusCount(status: TicketStatus | 'Todos'): number {
    const byStatus = this.stats().byStatus;
    if (status === 'Todos') {
      return Object.values(byStatus).reduce((sum: number, count) => sum + (count ?? 0), 0);
    }
    return byStatus[status] ?? 0;
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

  selectTicket(id: number): void {
    this.selectedId.set(id);
  }

  closeDetail(): void {
    this.selectedId.set(null);
  }

  onTicketChanged(updated: Ticket): void {
    this.tickets.update((current) => current.map((t) => (t.id === updated.id ? updated : t)));
    this.loadStats();
  }
}
