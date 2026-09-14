export type TicketStatus = 'NEW' | 'AI_CLASSIFIED' | 'IN_PROGRESS' | 'PENDING_APPROVAL' | 'RESOLVED' | 'CLOSED';

export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH';

export interface Ticket {
  id: number;
  title: string;
  description: string;
  status: TicketStatus;
  priority: TicketPriority;
  categoryId: number | null;
  categoryName: string | null;
  createdById: number;
  createdByName: string;
  assignedToId: number | null;
  assignedToName: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface TicketRequest {
  title: string;
  description: string;
  categoryId?: number;
}

export interface TicketStats {
  abiertas: number;
  criticas: number;
  byStatus: Partial<Record<TicketStatus, number>>;
}

export interface TicketListParams {
  status?: TicketStatus;
  priority?: TicketPriority;
  categoryId?: number;
  search?: string;
  page?: number;
  size?: number;
}

export interface MyTicketListParams {
  status?: TicketStatus;
  priority?: TicketPriority;
  search?: string;
  page?: number;
  size?: number;
}
