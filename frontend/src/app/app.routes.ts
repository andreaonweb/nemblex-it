import { Routes } from '@angular/router';

import { authGuard } from './auth/guards/auth.guard';
import { roleGuard } from './auth/guards/role.guard';
import { LoginComponent } from './auth/login/login.component';
import { ShellComponent } from './shared/layout/shell/shell.component';
import { TicketsPageComponent } from './tickets/tickets-page/tickets-page.component';
import { ApprovalsPageComponent } from './approvals/approvals-page/approvals-page.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      { path: 'tickets', component: TicketsPageComponent },
      {
        path: 'aprobaciones',
        component: ApprovalsPageComponent,
        canActivate: [roleGuard],
        data: { roles: ['SUPERVISOR', 'ADMIN'] }
      },
      { path: '', pathMatch: 'full', redirectTo: 'tickets' }
    ]
  },
  { path: '**', redirectTo: 'tickets' }
];
