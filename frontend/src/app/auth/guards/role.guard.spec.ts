import { TestBed } from '@angular/core/testing';
import { ActivatedRouteSnapshot } from '@angular/router';
import { Router } from '@angular/router';

import { roleGuard } from './role.guard';
import { AuthService } from '../services/auth.service';
import { CurrentUser } from '../models/auth.models';

describe('roleGuard', () => {
  let authServiceStub: { currentUser: () => CurrentUser | null };
  let router: Router;

  beforeEach(() => {
    authServiceStub = { currentUser: () => null };
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: authServiceStub }]
    });
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
  });

  function runGuard(allowedRoles: string[]): boolean {
    const route = { data: { roles: allowedRoles } } as unknown as ActivatedRouteSnapshot;
    return TestBed.runInInjectionContext(() => roleGuard(route, {} as never) as boolean);
  }

  it('allows navigation when the current user has one of the allowed roles', () => {
    authServiceStub.currentUser = () => ({ id: 2, email: 'beatriz.ruiz@nemblex.dev', role: 'SUPERVISOR' });

    expect(runGuard(['SUPERVISOR', 'ADMIN'])).toBeTrue();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('redirects to /tickets and blocks navigation when the role is not allowed', () => {
    authServiceStub.currentUser = () => ({ id: 1, email: 'ana.torres@nemblex.dev', role: 'TECHNICIAN' });

    expect(runGuard(['SUPERVISOR', 'ADMIN'])).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/tickets']);
  });

  it('redirects to /tickets when there is no logged-in user', () => {
    authServiceStub.currentUser = () => null;

    expect(runGuard(['SUPERVISOR', 'ADMIN'])).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/tickets']);
  });

  it('redirects an EMPLOYEE to /my-tickets when the role is not allowed', () => {
    authServiceStub.currentUser = () => ({ id: 4, email: 'carlos.mendez@nemblex.dev', role: 'EMPLOYEE' });

    expect(runGuard(['TECHNICIAN', 'SUPERVISOR', 'ADMIN'])).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/my-tickets']);
  });
});
