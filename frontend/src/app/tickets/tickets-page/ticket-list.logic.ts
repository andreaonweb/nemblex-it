import { Ticket, TicketPriority, TicketStatus } from '../models/ticket.models';

export interface TicketFilters {
  status: TicketStatus | 'Todos';
  priority: TicketPriority | 'Todas';
  search: string;
}

export interface TicketKpis {
  abiertas: number;
  criticas: number;
}

export function filterTickets(tickets: Ticket[], filters: TicketFilters): Ticket[] {
  const needle = filters.search.trim().toLowerCase();
  return tickets
    .filter((t) => {
      if (filters.status !== 'Todos' && t.status !== filters.status) return false;
      if (filters.priority !== 'Todas' && t.priority !== filters.priority) return false;
      if (needle && !`${t.title} ${t.createdByName}`.toLowerCase().includes(needle)) return false;
      return true;
    })
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
}

export function computeKpis(tickets: Ticket[]): TicketKpis {
  const abiertas = tickets.filter((t) => t.status !== 'RESOLVED' && t.status !== 'CLOSED');
  return {
    abiertas: abiertas.length,
    criticas: abiertas.filter((t) => t.priority === 'HIGH').length
  };
}

export function countByStatus(tickets: Ticket[], status: TicketStatus | 'Todos'): number {
  if (status === 'Todos') return tickets.length;
  return tickets.filter((t) => t.status === status).length;
}
