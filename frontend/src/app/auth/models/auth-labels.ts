import { Role } from './auth.models';

export const ROLE_LABELS: Record<Role, string> = {
  EMPLOYEE: 'Empleado',
  TECHNICIAN: 'Técnico',
  SUPERVISOR: 'Supervisor',
  ADMIN: 'Administrador'
};
