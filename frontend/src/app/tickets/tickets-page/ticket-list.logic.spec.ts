import { computeKpis, countByStatus, filterTickets, TicketFilters } from './ticket-list.logic';
import { Ticket } from '../models/ticket.models';

function ticket(overrides: Partial<Ticket>): Ticket {
  return {
    id: 1,
    title: 'VPN caída',
    description: 'desc',
    status: 'NEW',
    priority: 'MEDIUM',
    categoryId: null,
    categoryName: null,
    createdById: 1,
    createdByName: 'Ana Torres',
    assignedToId: null,
    assignedToName: null,
    createdAt: '2026-09-07T10:00:00',
    updatedAt: '2026-09-07T10:00:00',
    ...overrides
  };
}

describe('filterTickets', () => {
  const tickets: Ticket[] = [
    ticket({ id: 1, title: 'VPN caída para finanzas', status: 'NEW', priority: 'HIGH' }),
    ticket({ id: 2, title: 'Impresora atascada', status: 'IN_PROGRESS', priority: 'LOW' }),
    ticket({ id: 3, title: 'Correo en cola', status: 'RESOLVED', priority: 'HIGH' })
  ];

  const noFilters: TicketFilters = { status: 'Todos', priority: 'Todas', search: '' };

  it('returns every ticket when no filters are applied', () => {
    expect(filterTickets(tickets, noFilters)).toEqual(tickets);
  });

  it('filters by status', () => {
    const result = filterTickets(tickets, { ...noFilters, status: 'IN_PROGRESS' });
    expect(result.map((t) => t.id)).toEqual([2]);
  });

  it('filters by priority', () => {
    const result = filterTickets(tickets, { ...noFilters, priority: 'HIGH' });
    expect(result.map((t) => t.id)).toEqual([1, 3]);
  });

  it('filters by a case-insensitive search over the title', () => {
    const result = filterTickets(tickets, { ...noFilters, search: 'vpn' });
    expect(result.map((t) => t.id)).toEqual([1]);
  });

  it('combines status, priority and search filters', () => {
    const result = filterTickets(tickets, { status: 'NEW', priority: 'HIGH', search: 'finanzas' });
    expect(result.map((t) => t.id)).toEqual([1]);
  });

  it('sorts the result by newest first', () => {
    const unsorted: Ticket[] = [
      ticket({ id: 1, createdAt: '2026-09-05T10:00:00' }),
      ticket({ id: 2, createdAt: '2026-09-07T10:00:00' }),
      ticket({ id: 3, createdAt: '2026-09-06T10:00:00' })
    ];

    const result = filterTickets(unsorted, noFilters);

    expect(result.map((t) => t.id)).toEqual([2, 3, 1]);
  });
});

describe('computeKpis', () => {
  it('counts open tickets (not RESOLVED nor CLOSED) and critical open tickets', () => {
    const tickets: Ticket[] = [
      ticket({ id: 1, status: 'NEW', priority: 'HIGH' }),
      ticket({ id: 2, status: 'RESOLVED', priority: 'HIGH' }),
      ticket({ id: 3, status: 'IN_PROGRESS', priority: 'MEDIUM' }),
      ticket({ id: 4, status: 'CLOSED', priority: 'HIGH' })
    ];

    expect(computeKpis(tickets)).toEqual({ abiertas: 2, criticas: 1 });
  });
});

describe('countByStatus', () => {
  it('counts tickets matching a given status', () => {
    const tickets: Ticket[] = [
      ticket({ id: 1, status: 'NEW' }),
      ticket({ id: 2, status: 'NEW' }),
      ticket({ id: 3, status: 'RESOLVED' })
    ];

    expect(countByStatus(tickets, 'NEW')).toBe(2);
    expect(countByStatus(tickets, 'RESOLVED')).toBe(1);
    expect(countByStatus(tickets, 'CLOSED')).toBe(0);
  });

  it('counts every ticket for "Todos"', () => {
    const tickets: Ticket[] = [ticket({ id: 1, status: 'NEW' }), ticket({ id: 2, status: 'RESOLVED' })];

    expect(countByStatus(tickets, 'Todos')).toBe(2);
  });
});
