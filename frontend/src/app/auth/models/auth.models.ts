export type Role = 'TECHNICIAN' | 'SUPERVISOR' | 'ADMIN' | 'EMPLOYEE';

export interface CurrentUser {
  email: string;
  role: Role;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface JwtResponse {
  token: string;
  email: string;
}
