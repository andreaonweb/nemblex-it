import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatToolbarModule } from '@angular/material/toolbar';

import { AuthService } from '../../../auth/services/auth.service';

interface NavItem {
  label: string;
  path: string;
}

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, MatToolbarModule, MatButtonModule, MatIconModule],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss'
})
export class ShellComponent {
  private readonly authService = inject(AuthService);
  private readonly router = inject(Router);

  readonly currentUser = this.authService.currentUser;

  readonly navItems = computed<NavItem[]>(() => {
    const items: NavItem[] = [{ label: 'Incidencias', path: '/tickets' }];
    const role = this.currentUser()?.role;
    if (role === 'SUPERVISOR' || role === 'ADMIN') {
      items.push({ label: 'Aprobaciones', path: '/aprobaciones' });
    }
    return items;
  });

  logout(): void {
    this.authService.logout();
    this.router.navigateByUrl('/login');
  }
}
