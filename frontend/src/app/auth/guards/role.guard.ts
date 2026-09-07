import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';

import { AuthService } from '../services/auth.service';
import { Role } from '../models/auth.models';

export const roleGuard: CanActivateFn = (route) => {
  const allowedRoles = route.data['roles'] as Role[] | undefined;
  const user = inject(AuthService).currentUser();

  if (user && (!allowedRoles || allowedRoles.includes(user.role))) {
    return true;
  }
  inject(Router).navigate(['/tickets']);
  return false;
};
