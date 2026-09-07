import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';

import { authGuard } from './auth.guard';
import { AuthService } from '../services/auth.service';

describe('authGuard', () => {
  let authServiceStub: { isAuthenticated: () => boolean };
  let router: Router;

  beforeEach(() => {
    authServiceStub = { isAuthenticated: () => false };
    TestBed.configureTestingModule({
      providers: [{ provide: AuthService, useValue: authServiceStub }]
    });
    router = TestBed.inject(Router);
    spyOn(router, 'navigate');
  });

  function runGuard(): boolean {
    return TestBed.runInInjectionContext(() => authGuard({} as never, {} as never) as boolean);
  }

  it('allows navigation when the user is authenticated', () => {
    authServiceStub.isAuthenticated = () => true;

    expect(runGuard()).toBeTrue();
    expect(router.navigate).not.toHaveBeenCalled();
  });

  it('redirects to /login and blocks navigation when the user is not authenticated', () => {
    authServiceStub.isAuthenticated = () => false;

    expect(runGuard()).toBeFalse();
    expect(router.navigate).toHaveBeenCalledWith(['/login']);
  });
});
