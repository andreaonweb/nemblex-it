import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSelectModule } from '@angular/material/select';

import { TicketService } from '../services/ticket.service';
import { Ticket, TicketPriority, TicketStatus } from '../models/ticket.models';
import { PRIORITY_COLORS, PRIORITY_LABELS, PRIORITY_ORDER, STATUS_COLORS, STATUS_LABELS, STATUS_ORDER } from '../models/ticket-labels';
import { computeKpis, countByStatus, filterTickets } from './ticket-list.logic';

@Component({
  selector: 'app-tickets-page',
  standalone: true,
  imports: [MatCardModule, MatFormFieldModule, MatInputModule, MatProgressSpinnerModule, MatSelectModule],
  templateUrl: './tickets-page.component.html',
  styleUrl: './tickets-page.component.scss'
})
export class TicketsPageComponent implements OnInit {
  private readonly ticketService = inject(TicketService);

  protected readonly statusLabels = STATUS_LABELS;
  protected readonly statusColors = STATUS_COLORS;
  protected readonly priorityLabels = PRIORITY_LABELS;
  protected readonly priorityColors = PRIORITY_COLORS;
  protected readonly statusOptions: (TicketStatus | 'Todos')[] = ['Todos', ...STATUS_ORDER];
  protected readonly priorityOptions: (TicketPriority | 'Todas')[] = ['Todas', ...PRIORITY_ORDER];

  readonly tickets = signal<Ticket[]>([]);
  readonly loading = signal(false);
  readonly error = signal<string | null>(null);

  readonly statusFilter = signal<TicketStatus | 'Todos'>('Todos');
  readonly priorityFilter = signal<TicketPriority | 'Todas'>('Todas');
  readonly search = signal('');

  readonly kpis = computed(() => computeKpis(this.tickets()));
  readonly filteredTickets = computed(() =>
    filterTickets(this.tickets(), {
      status: this.statusFilter(),
      priority: this.priorityFilter(),
      search: this.search()
    })
  );

  ngOnInit(): void {
    this.loading.set(true);
    this.ticketService.list().subscribe({
      next: (tickets) => {
        this.tickets.set(tickets);
        this.loading.set(false);
      },
      error: () => {
        this.error.set('No se pudieron cargar las incidencias.');
        this.loading.set(false);
      }
    });
  }

  statusCount(status: TicketStatus | 'Todos'): number {
    return countByStatus(this.tickets(), status);
  }
}
