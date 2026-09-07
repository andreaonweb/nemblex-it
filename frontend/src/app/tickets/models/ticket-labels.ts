import { TicketPriority, TicketStatus } from './ticket.models';

export const STATUS_LABELS: Record<TicketStatus, string> = {
  NEW: 'Nuevo',
  AI_CLASSIFIED: 'Clasificado por IA',
  IN_PROGRESS: 'En curso',
  PENDING_APPROVAL: 'Pendiente de aprobación',
  RESOLVED: 'Resuelto',
  CLOSED: 'Cerrado'
};

export const STATUS_COLORS: Record<TicketStatus, { bg: string; fg: string; bc: string }> = {
  NEW: { bg: '#0f4383', fg: '#ffffff', bc: '#0f4383' },
  AI_CLASSIFIED: { bg: '#fff3dc', fg: '#6b4405', bc: '#e0be7a' },
  IN_PROGRESS: { bg: '#e6edf7', fg: '#0c3b77', bc: '#bfd0e6' },
  PENDING_APPROVAL: { bg: '#fbf1df', fg: '#7a4c05', bc: '#e5cfa3' },
  RESOLVED: { bg: '#f1f3f6', fg: '#3f4b60', bc: '#d3d9e1' },
  CLOSED: { bg: '#e5e7eb', fg: '#3f4b60', bc: '#d3d9e1' }
};

export const PRIORITY_LABELS: Record<TicketPriority, string> = {
  HIGH: 'Alta',
  MEDIUM: 'Media',
  LOW: 'Baja'
};

export const PRIORITY_COLORS: Record<TicketPriority, { fg: string }> = {
  HIGH: { fg: '#8a3b0a' },
  MEDIUM: { fg: '#0c3b77' },
  LOW: { fg: '#4a5568' }
};

export const STATUS_ORDER: TicketStatus[] = [
  'NEW',
  'AI_CLASSIFIED',
  'IN_PROGRESS',
  'PENDING_APPROVAL',
  'RESOLVED',
  'CLOSED'
];

export const PRIORITY_ORDER: TicketPriority[] = ['HIGH', 'MEDIUM', 'LOW'];
