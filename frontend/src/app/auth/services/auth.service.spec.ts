import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { AuthService } from './auth.service';
import { environment } from '../../../environments/environment';

function fakeToken(email: string, role: string, uid = 1): string {
  const header = btoa(JSON.stringify({ alg: 'HS512', typ: 'JWT' }));
  const payload = btoa(JSON.stringify({ sub: email, role: `ROLE_${role}`, uid }));
  return `${header}.${payload}.fake-signature`;
}

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('has no current user before logging in', () => {
    expect(service.currentUser()).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
  });

  it('posts credentials to /api/auth/login', () => {
    service.login({ email: 'ana.torres@nemblex.dev', password: 'technician123' }).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/api/auth/login`);
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual({ email: 'ana.torres@nemblex.dev', password: 'technician123' });
    req.flush({ token: fakeToken('ana.torres@nemblex.dev', 'TECHNICIAN'), email: 'ana.torres@nemblex.dev' });
  });

  it('decodes the role from the JWT and exposes it as the current user after login', () => {
    service.login({ email: 'beatriz.ruiz@nemblex.dev', password: 'supervisor123' }).subscribe();

    const req = httpMock.expectOne(`${environment.apiUrl}/api/auth/login`);
    req.flush({ token: fakeToken('beatriz.ruiz@nemblex.dev', 'SUPERVISOR', 2), email: 'beatriz.ruiz@nemblex.dev' });

    expect(service.currentUser()).toEqual({ id: 2, email: 'beatriz.ruiz@nemblex.dev', role: 'SUPERVISOR' });
    expect(service.isAuthenticated()).toBeTrue();
  });

  it('persists the token so a new AuthService instance restores the session', () => {
    service.login({ email: 'ana.torres@nemblex.dev', password: 'technician123' }).subscribe();
    httpMock
      .expectOne(`${environment.apiUrl}/api/auth/login`)
      .flush({ token: fakeToken('ana.torres@nemblex.dev', 'TECHNICIAN', 3), email: 'ana.torres@nemblex.dev' });

    const restored = TestBed.inject(AuthService);
    expect(restored.currentUser()).toEqual({ id: 3, email: 'ana.torres@nemblex.dev', role: 'TECHNICIAN' });
  });

  it('clears the session on logout', () => {
    service.login({ email: 'ana.torres@nemblex.dev', password: 'technician123' }).subscribe();
    httpMock
      .expectOne(`${environment.apiUrl}/api/auth/login`)
      .flush({ token: fakeToken('ana.torres@nemblex.dev', 'TECHNICIAN'), email: 'ana.torres@nemblex.dev' });

    service.logout();

    expect(service.currentUser()).toBeNull();
    expect(service.isAuthenticated()).toBeFalse();
    expect(service.getToken()).toBeNull();
  });
});
