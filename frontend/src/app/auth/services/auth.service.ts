import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

import { environment } from '../../../environments/environment';
import { CurrentUser, JwtResponse, LoginRequest, Role } from '../models/auth.models';

const TOKEN_KEY = 'nemblex_token';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly currentUserSignal = signal<CurrentUser | null>(this.decodeUser(this.readToken()));

  readonly currentUser = this.currentUserSignal.asReadonly();
  readonly isAuthenticated = computed(() => this.currentUserSignal() !== null);

  login(credentials: LoginRequest): Observable<JwtResponse> {
    return this.http
      .post<JwtResponse>(`${environment.apiUrl}/api/auth/login`, credentials)
      .pipe(tap((response) => this.setSession(response.token)));
  }

  logout(): void {
    localStorage.removeItem(TOKEN_KEY);
    this.currentUserSignal.set(null);
  }

  getToken(): string | null {
    return this.readToken();
  }

  private setSession(token: string): void {
    localStorage.setItem(TOKEN_KEY, token);
    this.currentUserSignal.set(this.decodeUser(token));
  }

  private readToken(): string | null {
    return localStorage.getItem(TOKEN_KEY);
  }

  private decodeUser(token: string | null): CurrentUser | null {
    if (!token) {
      return null;
    }
    try {
      const payload = JSON.parse(atob(token.split('.')[1]));
      const role = (payload.role as string | undefined)?.replace('ROLE_', '') as Role | undefined;
      if (!payload.sub || !role) {
        return null;
      }
      return { email: payload.sub, role };
    } catch {
      return null;
    }
  }
}
