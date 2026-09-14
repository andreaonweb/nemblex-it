import { AuditResultStatus } from './audit-log.models';

export const RESULT_STATUS_LABELS: Record<AuditResultStatus, string> = {
  PENDING: 'Pendiente',
  APPROVED: 'Aprobado',
  REJECTED: 'Rechazado'
};

const ACTION_LABELS: Record<string, string> = {
  CLOSE: 'Cerrar',
  ESCALATE: 'Escalar',
  REASSIGN: 'Reasignar',
  AI_CLASSIFY: 'Clasificación IA'
};

export function actionLabel(action: string): string {
  return ACTION_LABELS[action] ?? action;
}
