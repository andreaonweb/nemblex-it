import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { signal } from '@angular/core';

import { ShellComponent } from './shell.component';
import { AuthService } from '../../../auth/services/auth.service';
import { CurrentUser } from '../../../auth/models/auth.models';

describe('ShellComponent', () => {
  let authServiceStub: { currentUser: ReturnType<typeof signal<CurrentUser | null>>; logout: jasmine.Spy };
  let router: Router;
  let component: ShellComponent;

  beforeEach(() => {
    authServiceStub = { currentUser: signal<CurrentUser | null>(null), logout: jasmine.createSpy('logout') };
    TestBed.configureTestingModule({
      imports: [ShellComponent],
      providers: [{ provide: AuthService, useValue: authServiceStub }]
    });
    router = TestBed.inject(Router);
    spyOn(router, 'navigateByUrl');
    component = TestBed.createComponent(ShellComponent).componentInstance;
  });

  it('shows only Incidencias for a TECHNICIAN', () => {
    authServiceStub.currentUser.set({ id: 1, email: 'ana.torres@nemblex.dev', role: 'TECHNICIAN' });

    expect(component.navItems().map((i) => i.label)).toEqual(['Incidencias']);
  });

  it('shows Incidencias and Aprobaciones for a SUPERVISOR', () => {
    authServiceStub.currentUser.set({ id: 2, email: 'beatriz.ruiz@nemblex.dev', role: 'SUPERVISOR' });

    expect(component.navItems().map((i) => i.label)).toEqual(['Incidencias', 'Aprobaciones']);
  });

  it('shows Incidencias and Aprobaciones for an ADMIN', () => {
    authServiceStub.currentUser.set({ id: 3, email: 'admin@nemblex.dev', role: 'ADMIN' });

    expect(component.navItems().map((i) => i.label)).toEqual(['Incidencias', 'Aprobaciones']);
  });

  it('shows only Mis tickets for an EMPLOYEE', () => {
    authServiceStub.currentUser.set({ id: 4, email: 'carlos.mendez@nemblex.dev', role: 'EMPLOYEE' });

    expect(component.navItems()).toEqual([{ label: 'Mis tickets', path: '/my-tickets' }]);
  });

  it('logs out and navigates to /login', () => {
    component.logout();

    expect(authServiceStub.logout).toHaveBeenCalled();
    expect(router.navigateByUrl).toHaveBeenCalledWith('/login');
  });
});
