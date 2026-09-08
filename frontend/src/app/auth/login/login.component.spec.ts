import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { LoginComponent } from './login.component';
import { AuthService } from '../services/auth.service';
import { CurrentUser, JwtResponse, LoginRequest } from '../models/auth.models';

describe('LoginComponent', () => {
  let authServiceStub: { login: jasmine.Spy; currentUser: () => CurrentUser | null };
  let router: Router;
  let component: LoginComponent;

  beforeEach(() => {
    authServiceStub = { login: jasmine.createSpy('login'), currentUser: () => null };
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [{ provide: AuthService, useValue: authServiceStub }]
    });
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl');
    component = TestBed.createComponent(LoginComponent).componentInstance;
  });

  it('does not call login when the form is invalid', () => {
    component.form.setValue({ email: '', password: '' });

    component.submit();

    expect(authServiceStub.login).not.toHaveBeenCalled();
  });

  it('calls AuthService.login with the form values and navigates to /tickets on success', () => {
    authServiceStub.login.and.returnValue(of({ token: 'x', email: 'ana.torres@nemblex.dev' } as JwtResponse));
    authServiceStub.currentUser = () => ({ id: 1, email: 'ana.torres@nemblex.dev', role: 'TECHNICIAN' });
    component.form.setValue({ email: 'ana.torres@nemblex.dev', password: 'technician123' });

    component.submit();

    const expectedRequest: LoginRequest = { email: 'ana.torres@nemblex.dev', password: 'technician123' };
    expect(authServiceStub.login).toHaveBeenCalledWith(expectedRequest);
    expect(router.navigateByUrl).toHaveBeenCalledWith('/tickets');
    expect(component.errorMessage()).toBeNull();
    expect(component.submitting()).toBeFalse();
  });

  it('navigates an EMPLOYEE to /my-tickets on success', () => {
    authServiceStub.login.and.returnValue(of({ token: 'x', email: 'carlos.mendez@nemblex.dev' } as JwtResponse));
    authServiceStub.currentUser = () => ({ id: 4, email: 'carlos.mendez@nemblex.dev', role: 'EMPLOYEE' });
    component.form.setValue({ email: 'carlos.mendez@nemblex.dev', password: 'employee123' });

    component.submit();

    expect(router.navigateByUrl).toHaveBeenCalledWith('/my-tickets');
  });

  it('shows an error message and stops submitting when the login fails', () => {
    authServiceStub.login.and.returnValue(throwError(() => new Error('invalid credentials')));
    component.form.setValue({ email: 'ana.torres@nemblex.dev', password: 'wrong' });

    component.submit();

    expect(component.errorMessage()).not.toBeNull();
    expect(component.submitting()).toBeFalse();
    expect(router.navigateByUrl).not.toHaveBeenCalled();
  });
});
