export type AuditResultStatus = 'PENDING' | 'APPROVED' | 'REJECTED';

export interface AuditLog {
  id: number;
  ticketId: number;
  action: string;
  reason: string | null;
  resultStatus: AuditResultStatus;
  approvedByName: string | null;
  createdAt: string;
}

export interface AuditLogRequest {
  ticketId: number;
  action: string;
  reason?: string;
}
