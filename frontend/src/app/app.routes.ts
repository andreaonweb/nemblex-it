import { inject } from '@angular/core';
import { Routes } from '@angular/router';

import { authGuard } from './auth/guards/auth.guard';
import { roleGuard, defaultRouteForRole } from './auth/guards/role.guard';
import { AuthService } from './auth/services/auth.service';
import { LoginComponent } from './auth/login/login.component';
import { ShellComponent } from './shared/layout/shell/shell.component';
import { TicketsPageComponent } from './tickets/tickets-page/tickets-page.component';
import { MyTicketsPageComponent } from './tickets/my-tickets-page/my-tickets-page.component';
import { ApprovalsPageComponent } from './approvals/approvals-page/approvals-page.component';

const redirectToHome = () => defaultRouteForRole(inject(AuthService).currentUser()?.role);

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: 'tickets',
        component: TicketsPageComponent,
        canActivate: [roleGuard],
        data: { roles: ['TECHNICIAN', 'SUPERVISOR', 'ADMIN'] }
      },
      { path: 'my-tickets', component: MyTicketsPageComponent },
      {
        path: 'aprobaciones',
        component: ApprovalsPageComponent,
        canActivate: [roleGuard],
        data: { roles: ['SUPERVISOR', 'ADMIN'] }
      },
      { path: '', pathMatch: 'full', redirectTo: redirectToHome }
    ]
  },
  { path: '**', redirectTo: redirectToHome }
];
