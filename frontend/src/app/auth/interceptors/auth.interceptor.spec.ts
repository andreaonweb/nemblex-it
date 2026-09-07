import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';

import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';

describe('authInterceptor', () => {
  let http: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([authInterceptor])), provideHttpClientTesting()]
    });
    http = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
  });

  afterEach(() => {
    httpMock.verify();
    localStorage.clear();
  });

  it('adds the Authorization header when a token is stored', () => {
    localStorage.setItem('nemblex_token', 'stored-token');
    // AuthService reads the token lazily via getToken(), no need to re-instantiate it here.

    http.get('/api/tickets').subscribe();

    const req = httpMock.expectOne('/api/tickets');
    expect(req.request.headers.get('Authorization')).toBe('Bearer stored-token');
    req.flush({});
  });

  it('does not add the Authorization header when there is no token', () => {
    expect(authService.getToken()).toBeNull();

    http.get('/api/tickets').subscribe();

    const req = httpMock.expectOne('/api/tickets');
    expect(req.request.headers.has('Authorization')).toBeFalse();
    req.flush({});
  });
});
