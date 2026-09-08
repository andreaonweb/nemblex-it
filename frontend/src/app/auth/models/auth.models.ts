export type Role = 'TECHNICIAN' | 'SUPERVISOR' | 'ADMIN';

export interface CurrentUser {
  id: number;
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
