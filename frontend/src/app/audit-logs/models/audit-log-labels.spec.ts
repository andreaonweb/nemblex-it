import { actionLabel, RESULT_STATUS_LABELS } from './audit-log-labels';

describe('actionLabel', () => {
  it('translates known TicketAction values to Spanish', () => {
    expect(actionLabel('CLOSE')).toBe('Cerrar');
    expect(actionLabel('ESCALATE')).toBe('Escalar');
    expect(actionLabel('REASSIGN')).toBe('Reasignar');
    expect(actionLabel('AI_CLASSIFY')).toBe('Clasificación IA');
  });

  it('returns the raw value unchanged for a free-text action entered by a technician', () => {
    expect(actionLabel('REINICIO_SERVICIO_VPN')).toBe('REINICIO_SERVICIO_VPN');
  });
});

describe('RESULT_STATUS_LABELS', () => {
  it('translates every AuditResultStatus value to Spanish', () => {
    expect(RESULT_STATUS_LABELS).toEqual({
      PENDING: 'Pendiente',
      APPROVED: 'Aprobado',
      REJECTED: 'Rechazado'
    });
  });
});
