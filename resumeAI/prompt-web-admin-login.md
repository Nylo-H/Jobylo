# Guide Web : Login Admin Dashboard (React + Shadcn)

## Flux d'authentification complet

```
┌─────────────────────────────────────────────────────────────┐
│                    LOGIN FLOW                                │
│                                                             │
│  1. User saisit email + password                            │
│  2. POST /auth/login                                        │
│  3. Backend vérifie credentials                             │
│  4. Retourne LoginResponse { accesstoken, refreshtoken,     │
│                              verified }                     │
│  5. Frontend stocke accessToken dans Zustand                │
│     + localStorage (persistance)                            │
│  6. Frontend appelle GET /auth/me avec le JWT               │
│  7. Backend retourne MeResponse { ..., role, ... }          │
│  8. Si role == "ADMIN" → redirect /admin/dashboard          │
│     Si role == "USER"  → redirect / (app utilisateur)       │
│  9. Refresh token stocké en httpOnly cookie                  │
│     (automatique via Set-Cookie header)                     │
└─────────────────────────────────────────────────────────────┘
```

**Point clé :** `LoginResponse` ne contient **pas** le rôle. Il faut un deuxième appel à `GET /auth/me` pour obtenir `role`.

---

## 1. LoginResponse vs MeResponse

### LoginResponse (POST /auth/login)
```json
{
  "accesstoken": "eyJhbGciOiJI...",
  "refreshtoken": "uuid-string",
  "verified": true
}
```

### MeResponse (GET /auth/me)
```json
{
  "id": "uuid",
  "username": "jean_admin",
  "email": "admin@example.com",
  "role": "ADMIN",
  "verified": true,
  "kycStatus": "VERIFIED",
  "photoProfil": "/uploads/profiles/xxx.jpg",
  "averageRating": 4.5,
  "totalRatings": 12
}
```

---

## 2. Architecture des stores

### auth.store.ts (Zustand)

```ts
import { create } from 'zustand';
import { persist } from 'zustand/middleware';

interface User {
  id: string;
  username: string;
  email: string;
  role: 'USER' | 'ADMIN';
  verified: boolean;
  kycStatus: string;
  photoProfil: string | null;
  averageRating: number | null;
  totalRatings: number | null;
}

interface AuthState {
  user: User | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  isLoading: boolean;

  setAuth: (user: User, accessToken: string) => void;
  setUser: (user: User) => void;
  setAccessToken: (token: string) => void;
  logout: () => void;
  setLoading: (loading: boolean) => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      isAuthenticated: false,
      isAdmin: false,
      isLoading: true,

      setAuth: (user, accessToken) =>
        set({
          user,
          accessToken,
          isAuthenticated: true,
          isAdmin: user.role === 'ADMIN',
          isLoading: false,
        }),

      setUser: (user) =>
        set({ user, isAdmin: user.role === 'ADMIN' }),

      setAccessToken: (accessToken) =>
        set({ accessToken }),

      logout: () =>
        set({
          user: null,
          accessToken: null,
          isAuthenticated: false,
          isAdmin: false,
          isLoading: false,
        }),

      setLoading: (isLoading) => set({ isLoading }),
    }),
    {
      name: 'jobylo-auth',
      partialize: (state) => ({
        accessToken: state.accessToken,
        user: state.user,
      }),
    }
  )
);
```

---

## 3. Hook useAuth (React Query + Zustand)

```ts
// hooks/use-auth.ts
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useAuthStore } from '@/store/auth.store';
import { api } from '@/api/client';
import { useNavigate } from 'react-router-dom';
import { toast } from 'sonner';

export function useLogin() {
  const setAuth = useAuthStore((s) => s.setAuth);
  const navigate = useNavigate();
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: async (data: { email: string; password: string }) => {
      // 1er appel : login
      const { data: loginResp } = await api.post('/auth/login', {
        email: data.email,
        password: data.password,
      });

      // Stocker le token immédiatement pour le prochain appel
      useAuthStore.getState().setAccessToken(loginResp.accesstoken);

      // 2e appel : récupérer le rôle via /me
      const { data: me } = await api.get('/auth/me', {
        headers: { Authorization: `Bearer ${loginResp.accesstoken}` },
      });

      return { loginResp, me };
    },
    onSuccess: ({ loginResp, me }) => {
      setAuth(me, loginResp.accesstoken);
      queryClient.invalidateQueries();

      if (me.role === 'ADMIN') {
        navigate('/admin/dashboard');
        toast.success('Connecté en tant qu\'administrateur');
      } else {
        navigate('/');
        toast.success('Connecté');
      }
    },
    onError: (error: any) => {
      const msg = error?.response?.data?.error || 'Email ou mot de passe incorrect';
      toast.error(msg);
    },
  });
}

export function useLogout() {
  const logout = useAuthStore((s) => s.logout);
  const navigate = useNavigate();

  return () => {
    logout();
    navigate('/login');
  };
}
```

---

## 4. Route Guards

### AdminGuard.tsx

```tsx
// components/AdminGuard.tsx
import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '@/store/auth.store';
import { Loader } from 'lucide-react';

export function AdminGuard() {
  const { isAuthenticated, isAdmin, isLoading } = useAuthStore();

  if (isLoading) {
    return (
      <div className="flex items-center justify-center h-screen bg-background">
        <Loader className="h-8 w-8 animate-spin text-primary" />
      </div>
    );
  }

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  if (!isAdmin) {
    return (
      <div className="flex items-center justify-center h-screen bg-background">
        <div className="text-center">
          <h2 className="text-xl font-bold text-text-primary">Accès refusé</h2>
          <p className="text-text-secondary mt-2">
            Vous n'avez pas les droits d'administration.
          </p>
        </div>
      </div>
    );
  }

  return <Outlet />;
}
```

### Routing

```tsx
<Routes>
  <Route path="/login" element={<LoginPage />} />

  {/* App utilisateur (si role USER ou non-admin) */}
  <Route element={<AuthGuard />}>
    <Route element={<AppLayout />}>
      <Route path="/" element={<HomePage />} />
      ...
    </Route>
  </Route>

  {/* Admin (si role ADMIN) */}
  <Route element={<AdminGuard />}>
    <Route element={<AdminLayout />}>
      <Route path="/admin/dashboard" element={<DashboardPage />} />
      <Route path="/admin/users" element={<UsersListPage />} />
      <Route path="/admin/users/:id" element={<UserDetailPage />} />
      <Route path="/admin/kyc" element={<KycListPage />} />
      <Route path="/admin/audit" element={<AuditLogsPage />} />
      <Route path="/admin/categories" element={<CategoriesPage />} />
    </Route>
  </Route>
</Routes>
```

---

## 5. LoginPage.tsx (composant complet)

```tsx
// pages/LoginPage.tsx
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { z } from 'zod';
import { motion } from 'framer-motion';
import { useLogin } from '@/hooks/use-auth';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Loader, Eye, EyeOff } from 'lucide-react';
import { useState } from 'react';

const loginSchema = z.object({
  email: z.string().email('Email invalide'),
  password: z.string().min(6, 'Minimum 6 caractères'),
});

type LoginForm = z.infer<typeof loginSchema>;

export function LoginPage() {
  const login = useLogin();
  const [showPassword, setShowPassword] = useState(false);

  const { register, handleSubmit, formState: { errors } } = useForm<LoginForm>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = (data: LoginForm) => login.mutate(data);

  return (
    <div className="min-h-screen bg-background flex items-center justify-center p-4">
      <motion.div
        initial={{ opacity: 0, y: 20 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.3 }}
        className="w-full max-w-md"
      >
        <Card>
          <CardHeader className="text-center">
            <div className="flex justify-center mb-4">
              <div className="w-12 h-12 rounded-full bg-primary flex items-center justify-center">
                <span className="text-white font-bold text-xl">J</span>
              </div>
            </div>
            <CardTitle className="text-2xl font-bold text-text-primary">
              Jobylo Admin
            </CardTitle>
            <p className="text-text-secondary text-sm mt-1">
              Connectez-vous à votre tableau de bord
            </p>
          </CardHeader>
          <CardContent>
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-4">
              <div>
                <label className="text-sm font-medium text-text-primary block mb-1">
                  Email
                </label>
                <Input
                  type="email"
                  placeholder="admin@example.com"
                  {...register('email')}
                  className={errors.email ? 'border-error' : ''}
                />
                {errors.email && (
                  <p className="text-error text-xs mt-1">{errors.email.message}</p>
                )}
              </div>

              <div>
                <label className="text-sm font-medium text-text-primary block mb-1">
                  Mot de passe
                </label>
                <div className="relative">
                  <Input
                    type={showPassword ? 'text' : 'password'}
                    placeholder="••••••••"
                    {...register('password')}
                    className={errors.password ? 'border-error pr-10' : 'pr-10'}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 top-1/2 -translate-y-1/2 text-text-hint hover:text-text-secondary"
                  >
                    {showPassword ? <EyeOff size={18} /> : <Eye size={18} />}
                  </button>
                </div>
                {errors.password && (
                  <p className="text-error text-xs mt-1">{errors.password.message}</p>
                )}
              </div>

              {login.isError && (
                <motion.p
                  initial={{ opacity: 0, y: -5 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="text-error text-sm bg-red-50 p-2 rounded"
                >
                  {login.error?.response?.data?.error || 'Identifiants incorrects'}
                </motion.p>
              )}

              <Button
                type="submit"
                className="w-full bg-primary hover:bg-primary-dark"
                disabled={login.isPending}
              >
                {login.isPending ? (
                  <Loader className="h-5 w-5 animate-spin" />
                ) : (
                  'Se connecter'
                )}
              </Button>
            </form>
          </CardContent>
        </Card>
      </motion.div>
    </div>
  );
}
```

---

## 6. Axios Interceptor avec Refresh Token Queue

```ts
// api/client.ts
import axios from 'axios';
import { useAuthStore } from '@/store/auth.store';

const API_BASE = import.meta.env.VITE_API_URL || 'http://localhost:8080/api';

export const api = axios.create({
  baseURL: API_BASE,
  headers: { 'Content-Type': 'application/json' },
  withCredentials: true, // ← important pour le cookie httpOnly refreshToken
});

// Intercepteur Request : ajouter le Bearer token
api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Intercepteur Response : refresh automatique sur 401
let isRefreshing = false;
let failedQueue: Array<{
  resolve: (token: string) => void;
  reject: (err: any) => void;
}> = [];

const processQueue = (error: any, token: string | null = null) => {
  failedQueue.forEach((prom) => {
    if (token) prom.resolve(token);
    else prom.reject(error);
  });
  failedQueue = [];
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Ne pas intercepter les requêtes d'auth
    if (originalRequest.url?.includes('/auth/login') ||
        originalRequest.url?.includes('/auth/refresh')) {
      return Promise.reject(error);
    }

    if (error.response?.status === 401 && !originalRequest._retry) {
      if (isRefreshing) {
        return new Promise((resolve, reject) => {
          failedQueue.push({ resolve, reject });
        }).then((token) => {
          originalRequest.headers.Authorization = `Bearer ${token}`;
          return api(originalRequest);
        });
      }

      originalRequest._retry = true;
      isRefreshing = true;

      try {
        // Le refresh token est envoyé automatiquement via le cookie httpOnly
        const { data } = await axios.post(
          `${API_BASE}/auth/refresh`,
          {},
          { withCredentials: true }
        );

        useAuthStore.getState().setAccessToken(data.accesstoken);
        processQueue(null, data.accesstoken);

        originalRequest.headers.Authorization = `Bearer ${data.accesstoken}`;
        return api(originalRequest);
      } catch (refreshError) {
        processQueue(refreshError, null);
        useAuthStore.getState().logout();
        window.location.href = '/login';
        return Promise.reject(refreshError);
      } finally {
        isRefreshing = false;
      }
    }

    return Promise.reject(error);
  }
);
```

---

## 7. Initialisation de l'App (Session Check)

```tsx
// App.tsx
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { BrowserRouter } from 'react-router-dom';
import { Toaster } from 'sonner';
import { useEffect, useState } from 'react';
import { useAuthStore } from '@/store/auth.store';
import { api } from '@/api/client';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: { retry: 1, staleTime: 30_000, refetchOnWindowFocus: false },
  },
});

function AuthInitializer({ children }: { children: React.ReactNode }) {
  const { accessToken, setAuth, setLoading } = useAuthStore();
  const [ready, setReady] = useState(false);

  useEffect(() => {
    const init = async () => {
      if (!accessToken) {
        setLoading(false);
        setReady(true);
        return;
      }

      try {
        const { data: me } = await api.get('/auth/me');
        setAuth(me, accessToken);
      } catch {
        // Tentative de refresh via cookie
        try {
          const { data } = await api.post('/auth/refresh', {});
          const { data: me } = await api.get('/auth/me', {
            headers: { Authorization: `Bearer ${data.accesstoken}` },
          });
          setAuth(me, data.accesstoken);
        } catch {
          useAuthStore.getState().logout();
        }
      } finally {
        setLoading(false);
        setReady(true);
      }
    };

    init();
  }, []);

  if (!ready) {
    return (
      <div className="flex items-center justify-center h-screen bg-background">
        <div className="text-center">
          <div className="w-12 h-12 rounded-full bg-primary mx-auto flex items-center justify-center animate-pulse">
            <span className="text-white font-bold text-xl">J</span>
          </div>
          <p className="text-text-secondary mt-4">Chargement...</p>
        </div>
      </div>
    );
  }

  return children;
}

export default function App() {
  return (
    <BrowserRouter>
      <QueryClientProvider client={queryClient}>
        <AuthInitializer>
          <RouterRoutes />
          <Toaster position="top-right" richColors />
        </AuthInitializer>
      </QueryClientProvider>
    </BrowserRouter>
  );
}
```

---

## 8. Header Admin : User Menu avec Déconnexion

```tsx
// components/layout/Header.tsx
import { useAuthStore } from '@/store/auth.store';
import { useLogout } from '@/hooks/use-auth';
import {
  DropdownMenu, DropdownMenuContent, DropdownMenuItem,
  DropdownMenuLabel, DropdownMenuSeparator, DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Avatar, AvatarFallback } from '@/components/ui/avatar';
import { Button } from '@/components/ui/button';
import { LogOut, User } from 'lucide-react';

export function Header() {
  const { user } = useAuthStore();
  const logout = useLogout();

  return (
    <header className="h-16 border-b border-border bg-surface flex items-center justify-between px-6">
      <h2 className="text-lg font-semibold text-text-primary">Tableau de bord</h2>

      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button variant="ghost" className="flex items-center gap-2">
            <Avatar className="h-8 w-8">
              {user?.photoProfil ? (
                <img src={user.photoProfil} alt={user.username} />
              ) : (
                <AvatarFallback className="bg-primary text-white text-sm">
                  {user?.username?.charAt(0).toUpperCase()}
                </AvatarFallback>
              )}
            </Avatar>
            <div className="text-left">
              <p className="text-sm font-medium text-text-primary">{user?.username}</p>
              <p className="text-xs text-text-secondary">{user?.email}</p>
            </div>
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="w-56">
          <DropdownMenuLabel>
            <span className="text-xs bg-primary/10 text-primary px-2 py-0.5 rounded">ADMIN</span>
          </DropdownMenuLabel>
          <DropdownMenuSeparator />
          <DropdownMenuItem><User className="mr-2 h-4 w-4" />Mon profil</DropdownMenuItem>
          <DropdownMenuSeparator />
          <DropdownMenuItem onClick={logout} className="text-error focus:text-error">
            <LogOut className="mr-2 h-4 w-4" />Déconnexion
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    </header>
  );
}
```

---

## 9. Résumé : Points Clés

| Point | Détail |
|-------|--------|
| **JWT ne contient pas le rôle** | Appel à `GET /auth/me` **obligatoire** après login |
| **LoginResponse** | `{ accesstoken, refreshtoken, verified }` — pas de `role` |
| **MeResponse** | `{ id, username, email, role, kycStatus, ... }` — contient `role` |
| **Refresh token** | **httpOnly cookie** sur `/auth/refresh`, envoyé automatiquement |
| **withCredentials: true** | Nécessaire dans Axios pour que le cookie soit envoyé |
| **Persistance** | Zustand `persist` + localStorage pour user + accessToken |
| **Queue 401** | Évite les appels concurrents si plusieurs 401 arrivent en même temps |

## 10. Checklist Implémentation

- [ ] Créer `auth.store.ts` (Zustand + persist)
- [ ] Créer `api/client.ts` (Axios + interceptors refresh queue)
- [ ] Créer `hooks/use-auth.ts` (useLogin, useLogout)
- [ ] Créer `pages/LoginPage.tsx` (formulaire + validation zod + framer)
- [ ] Créer `components/AdminGuard.tsx` (route guard)
- [ ] Configurer le routing dans `App.tsx`
- [ ] Créer `AuthInitializer` (vérifie session au démarrage)
- [ ] Ajouter le menu utilisateur dans le Header admin
- [ ] Tester : login → /me → redirect admin → refresh automatique → logout
