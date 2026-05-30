# Prompt : Admin Dashboard — Jobylo Marketplace

## Stack Technique

| Technologie | Usage |
|-------------|-------|
| React 18 + TypeScript | Framework |
| Vite | Bundler |
| Shadcn/ui | Composants (boutons, tableaux, modales, formulaires, badges) |
| Tailwind CSS v3 | Styles utilitaires |
| React Query (TanStack Query v5) | Requêtes API, cache, mutations |
| Zustand | State global (user session, sidebar, filtres) |
| React Router v6 | Routing |
| Framer Motion | Animations |
| Lucide React | Icônes |
| Recharts | Graphiques du dashboard |
| react-hook-form + zod | Formulaires + validation |
| date-fns | Formatage dates |
| Axios | HTTP client |

---

## Palette Couleurs (à intégrer dans `tailwind.config.ts`)

```ts
// tailwind.config.ts
export default {
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#0D47A1',
          light: '#1565C0',
          dark: '#0A3470',
        },
        secondary: '#1976D2',
        background: '#F5F7FA',
        surface: '#FFFFFF',
        'surface-variant': '#F0F4F8',
        'text-primary': '#1A1A2E',
        'text-secondary': '#6B7280',
        'text-hint': '#9CA3AF',
        success: '#10B981',
        error: '#EF4444',
        warning: '#F59E0B',
        info: '#3B82F6',
        urgent: '#FF6B35',
        border: '#E5E7EB',
        'border-light': '#F3F4F6',
        price: '#059669',
        badge: '#EF4444',
        online: '#10B981',
        offline: '#9CA3AF',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
      },
    },
  },
};
```

**Shadcn :** Générer avec `npx shadcn@latest init` en choisissant `New York` style et `zinc` comme base color (puis écraser les tokens CSS avec les couleurs custom dans `globals.css`).

---

## Architecture du projet

```
src/
├── api/
│   ├── client.ts              // Axios instance + interceptors
│   ├── auth.api.ts             // Login, logout, refresh, me
│   ├── users.api.ts            // CRUD users + promote/demote
│   ├── kyc.api.ts              // KYC list, approve, reject
│   ├── audit.api.ts            // Audit logs
│   ├── jobs.api.ts             // Jobs list, stats
│   └── categories.api.ts       // Categories CRUD
├── components/
│   ├── ui/                     // Shadcn components re-exported
│   ├── layout/
│   │   ├── AdminLayout.tsx     // Sidebar + header + content
│   │   ├── Sidebar.tsx         // Navigation sidebar
│   │   └── Header.tsx          // Top bar with user menu
│   ├── dashboard/
│   │   ├── StatCard.tsx
│   │   ├── JobsChart.tsx
│   │   └── RecentActivity.tsx
│   ├── users/
│   │   ├── UserTable.tsx
│   │   ├── UserForm.tsx
│   │   └── UserDetail.tsx
│   ├── kyc/
│   │   ├── KycTable.tsx
│   │   ├── KycDetail.tsx
│   │   └── KycActions.tsx
│   ├── audit/
│   │   └── AuditTable.tsx
│   └── categories/
│       └── CategoryTree.tsx
├── hooks/
│   ├── use-auth.ts
│   ├── use-users.ts           // React Queries for users
│   ├── use-kyc.ts
│   ├── use-audit.ts
│   ├── use-jobs.ts
│   └── use-categories.ts
├── lib/
│   ├── utils.ts               // cn(), formatDate(), formatPrice()
│   └── constants.ts            // API_BASE_URL, query keys
├── store/
│   ├── auth.store.ts           // Zustand : user, token, isAdmin
│   └── ui.store.ts             // Zustand : sidebar open, theme
├── types/
│   ├── user.ts
│   ├── kyc.ts
│   ├── audit.ts
│   ├── job.ts
│   └── api.ts                  // Pagination, generic responses
├── pages/
│   ├── LoginPage.tsx
│   ├── DashboardPage.tsx
│   ├── users/
│   │   ├── UsersListPage.tsx
│   │   └── UserDetailPage.tsx
│   ├── kyc/
│   │   └── KycListPage.tsx
│   ├── audit/
│   │   └── AuditLogsPage.tsx
│   └── categories/
│       └── CategoriesPage.tsx
├── App.tsx
├── main.tsx
└── index.css
```

---

## Routes (React Router)

```
/login                          → LoginPage
/admin                          → AdminLayout (protégé, guard isAdmin)
  /admin/dashboard              → DashboardPage (par défaut)
  /admin/users                  → UsersListPage
  /admin/users/:id              → UserDetailPage
  /admin/kyc                    → KycListPage
  /admin/audit                  → AuditLogsPage
  /admin/categories             → CategoriesPage
```

### Auth Guard

```tsx
// components/AdminGuard.tsx
function AdminGuard({ children }) {
  const { user, isLoading } = useAuthStore();
  const navigate = useNavigate();

  useEffect(() => {
    if (!isLoading && (!user || user.role !== 'ADMIN')) {
      navigate('/login');
    }
  }, [user, isLoading]);

  if (isLoading) return <Loader />;
  return user?.role === 'ADMIN' ? children : null;
}
```

---

## 1. Dashboard Page (`/admin/dashboard`)

### Statistiques (4 StatCards en grid)

Charger depuis plusieurs endpoints (ou idéalement un endpoint `GET /admin/stats` à créer côté backend) :

```ts
// Requêtes parallèles React Query
const { data: userCount } = useQuery({ queryKey: ['admin', 'users', 'count'], queryFn: () => api.get('/users').then(r => r.data.length) });
const { data: jobsPending } = useQuery({ queryKey: ['admin', 'jobs', 'pending'], queryFn: () => api.get('/jobs/available') });
const { data: kycPending } = useQuery({ queryKey: ['admin', 'kyc', 'pending'], queryFn: () => api.get('/kyc/all', { params: { status: 'PENDING' } }) });
const { data: transactions } = useQuery({ queryKey: ['admin', 'transactions'], queryFn: () => api.get('/payments') });
```

Cartes :
```
┌─────────────────────────────────────────────────────────────┐
│  📊 Tableau de Bord                                         │
│                                                             │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │ 👥 Users │  │ 📋 Jobs  │  │ 🪪 KYC   │  │ 💰 CA    │   │
│  │   1 234  │  │  456     │  │  23 en   │  │ 12,5M   │   │
│  │  Total   │  │  Dispos  │  │  attente │  │  Chiffre │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────┘   │
│                                                             │
│  ┌────────────────────────┐  ┌────────────────────────┐    │
│  │ 📈 Jobs par mois       │  │ 🕐 Activité récente    │    │
│  │  (Recharts BarChart)   │  │  (timeline audit logs) │    │
│  └────────────────────────┘  └────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

### Animation StatCards

```tsx
import { motion } from 'framer-motion';

const container = { hidden: {}, show: { transition: { staggerChildren: 0.1 } } };
const item = {
  hidden: { opacity: 0, y: 20 },
  show: { opacity: 1, y: 0, transition: { type: 'spring', stiffness: 100 } },
};

<motion.div variants={container} initial="hidden" animate="show" className="grid grid-cols-4 gap-4">
  {stats.map(stat => (
    <motion.div key={stat.label} variants={item}>
      <StatCard icon={stat.icon} label={stat.label} value={stat.value} trend={stat.trend} />
    </motion.div>
  ))}
</motion.div>
```

---

## 2. Users Management (`/admin/users`)

### Backend : ce qui existe

```
GET    /users          → List<UserResponse>   (⚠️ publique, aucun auth)
GET    /users/{id}     → UserResponse          (⚠️ publique)
PUT    /users/{id}     → UserResponse          (⚠️ publique, met à jour role aussi)
DELETE /users/{id}     → void                  (⚠️ publique)
```

**⚠️ Problème :** Ces endpoints n'ont pas de `@PreAuthorize`. Pour l'admin dashboard, soit :
- **Option A** : Ajouter `@PreAuthorize("hasRole('ADMIN')")` sur ces endpoints côté backend
- **Option B** : Créer un `AdminController` dédié sous `/admin/users`

**Recommandé (Option B)** : Créer un `AdminController` pour ne pas casser l'API publique.

### Endpoints admin à créer côté backend

```
GET    /admin/users                  → List<UserResponse>     (admin only)
GET    /admin/users/{id}             → UserResponse           (admin only)
PUT    /admin/users/{id}             → UserResponse           (admin only, peut changer rôle)
DELETE /admin/users/{id}             → void                   (admin only)
PATCH  /admin/users/{id}/role        → UserResponse           (promote/demote)
  Body: { "role": "ADMIN" | "USER" }
```

### UI : Liste des utilisateurs

```tsx
// Shadcn DataTable avec filtres
<DataTable
  columns={[
    { accessorKey: 'username', header: 'Utilisateur' },
    { accessorKey: 'email', header: 'Email' },
    { accessorKey: 'role', header: 'Rôle', cell: ({ row }) => (
      <Badge variant={row.original.role === 'ADMIN' ? 'default' : 'secondary'}>
        {row.original.role}
      </Badge>
    )},
    { accessorKey: 'verified', header: 'Email vérifié', cell: ({ row }) => (
      row.original.verified ? <CheckCircle2 className="text-success" /> : <XCircle className="text-error" />
    )},
    { accessorKey: 'kycStatus', header: 'KYC', cell: ({ row }) => (
      <KycBadge status={row.original.kycStatus} />
    )},
    { id: 'actions', cell: ({ row }) => (
      <DropdownMenu>
        <DropdownMenuTrigger><MoreHorizontal /></DropdownMenuTrigger>
        <DropdownMenuContent>
          <DropdownMenuItem onClick={() => navigate(`/admin/users/${row.original.id}`)}>
            Détail
          </DropdownMenuItem>
          <DropdownMenuItem onClick={() => toggleRole(row.original)}>
            {row.original.role === 'ADMIN' ? 'Rétrograder en USER' : 'Promouvoir ADMIN'}
          </DropdownMenuItem>
          <DropdownMenuItem className="text-error" onClick={() => deleteUser(row.original.id)}>
            Supprimer
          </DropdownMenuItem>
        </DropdownMenuContent>
      </DropdownMenu>
    )},
  ]}
/>
```

### React Query hooks

```ts
export function useUsers() {
  return useQuery({
    queryKey: ['admin', 'users'],
    queryFn: () => adminApi.getUsers(),
  });
}

export function useDeleteUser() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id: string) => adminApi.deleteUser(id),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }),
  });
}

export function useUpdateUserRole() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, role }: { id: string; role: 'ADMIN' | 'USER' }) =>
      adminApi.updateUserRole(id, role),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }),
  });
}
```

---

## 3. KYC Verification (`/admin/kyc`)

### Backend endpoints (existent déjà)

```
GET    /kyc/all?status=PENDING   → List<KycDocumentResponse>   (admin only)
POST   /kyc/{documentId}/approve  → KycDocumentResponse        (admin only)
POST   /kyc/{documentId}/reject   → KycDocumentResponse        (admin only)
  Body (optionnel): { "rejectionReason": "Document illisible" }
```

### UI : File d'attente KYC

```
┌─────────────────────────────────────────────────────────────┐
│  🪪 Vérifications KYC                         [Filtre statut]│
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │ 🟡 En attente (23)                                   │   │
│  │                                                      │   │
│  │ ┌──────────────────────────────────────┬──────────┐  │   │
│  │ │ Jean Dupont -  jean@mail.com        │ 🟡 PENDING│  │   │
│  │ │ Carte d'identité - 12/05/2026       │ [Voir]    │  │   │
│  │ ├──────────────────────────────────────┼──────────┤  │   │
│  │ │ Marie L. - marie@mail.com           │ 🟡 PENDING│  │   │
│  │ │ Passeport - 12/05/2026              │ [Voir]    │  │   │
│  │ └──────────────────────────────────────┴──────────┘  │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                             │
│  🟢 Vérifiés (156)   🔴 Rejetés (12)                       │
└─────────────────────────────────────────────────────────────┘
```

### Modale de détail KYC

```tsx
function KycDetailDialog({ document, onApprove, onReject }) {
  return (
    <Dialog>
      <DialogContent>
        <DialogHeader>
          <DialogTitle>Document KYC — {document.user?.username}</DialogTitle>
        </DialogHeader>
        <div className="space-y-4">
          <div>
            <Label>Document soumis</Label>
            <img src={document.fileUrl} alt="KYC" className="rounded border max-h-96" />
          </div>
          <div>
            <Label>Type</Label>
            <p>{document.documentType}</p>
          </div>
          <div>
            <Label>Soumis le</Label>
            <p>{format(new Date(document.submittedAt), 'dd/MM/yyyy HH:mm')}</p>
          </div>
          {document.status === 'REJECTED' && document.rejectionReason && (
            <div>
              <Label className="text-error">Motif du rejet</Label>
              <p className="text-error">{document.rejectionReason}</p>
            </div>
          )}

          {document.status === 'PENDING' && (
            <div className="flex gap-2 justify-end">
              <Dialog>
                <DialogTrigger asChild>
                  <Button variant="destructive">Refuser</Button>
                </DialogTrigger>
                <DialogContent>
                  <DialogHeader>Motif du refus</DialogHeader>
                  <Textarea id="reason" placeholder="Document illisible, informations non concordantes..." />
                  <Button variant="destructive" onClick={() => onReject(document.id)}>Confirmer le refus</Button>
                </DialogContent>
              </Dialog>
              <Button onClick={() => onApprove(document.id)}>Approuver</Button>
            </div>
          )}
        </div>
      </DialogContent>
    </Dialog>
  );
}
```

### React Query Hooks KYC

```ts
export function useKycDocuments(status?: KycStatus) {
  return useQuery({
    queryKey: ['admin', 'kyc', status],
    queryFn: () => kycApi.getAll(status),
  });
}

export function useApproveKyc() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (documentId: string) => kycApi.approve(documentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'kyc'] });
      toast.success('KYC approuvé');
    },
  });
}

export function useRejectKyc() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ documentId, reason }: { documentId: string; reason: string }) =>
      kycApi.reject(documentId, reason),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'kyc'] });
      toast.success('KYC refusé');
    },
  });
}
```

---

## 4. Audit Logs (`/admin/audit`)

### Backend endpoints (existent déjà)

```
GET /audit      → List<ActionLog>   (admin only, tous les logs, triés par date desc)
GET /audit/me   → List<ActionLog>   (ses propres logs)
```

`ActionLog` model côté frontend :

```ts
interface ActionLog {
  id: string;
  user: { id: string; username: string; email: string };
  action: ActionType;
  details: string;
  timestamp: string; // ISO date
}
```

### UI : Logs avec filtres

```tsx
// Combobox filtres + DataTable
const ACTION_TYPES = [
  { value: 'REGISTER', label: 'Inscription', icon: UserPlus },
  { value: 'LOGIN', label: 'Connexion', icon: LogIn },
  { value: 'KYC_SUBMITTED', label: 'KYC soumis', icon: FileText },
  { value: 'KYC_APPROVED', label: 'KYC approuvé', icon: CheckCircle },
  { value: 'KYC_REJECTED', label: 'KYC rejeté', icon: XCircle },
  { value: 'PAYMENT_INITIATED', label: 'Paiement initié', icon: CreditCard },
  { value: 'PAYMENT_CONFIRMED', label: 'Paiement confirmé', icon: Check },
  { value: 'SUBMIT_RATING', label: 'Notation', icon: Star },
  // ... tous les ActionType
];
```

```tsx
// Table avec colonnes :
// - Timestamp (formaté)
// - Utilisateur (avatar + nom)
// - Action (badge coloré par type)
// - Détails
// - Filtre par type d'action (combobox)
// - Filtre par utilisateur (combobox search)
// - Pagination (React Query paginated)

<Input placeholder="Rechercher dans les logs..." />
<Select value={filterAction} onValueChange={setFilterAction}>
  <SelectItem value="ALL">Toutes les actions</SelectItem>
  {ACTION_TYPES.map(at => <SelectItem value={at.value}>{at.label}</SelectItem>)}
</Select>

<Table>
  <TableHeader>
    <TableRow>
      <TableHead>Date</TableHead>
      <TableHead>Utilisateur</TableHead>
      <TableHead>Action</TableHead>
      <TableHead>Détails</TableHead>
    </TableRow>
  </TableHeader>
  <TableBody>
    {logs.map(log => (
      <motion.tr
        key={log.id}
        initial={{ opacity: 0 }}
        animate={{ opacity: 1 }}
      >
        <TableCell>{format(new Date(log.timestamp), 'dd/MM/yyyy HH:mm')}</TableCell>
        <TableCell>
          <div className="flex items-center gap-2">
            <Avatar className="h-8 w-8">
              <AvatarFallback>{log.user.username[0].toUpperCase()}</AvatarFallback>
            </Avatar>
            <span>{log.user.username}</span>
          </div>
        </TableCell>
        <TableCell>
          <ActionBadge action={log.action} />
        </TableCell>
        <TableCell className="text-text-secondary text-sm">{log.details}</TableCell>
      </motion.tr>
    ))}
  </TableBody>
</Table>
```

### ActionBadge component

```tsx
const ACTION_STYLES: Record<string, string> = {
  REGISTER: 'bg-blue-100 text-blue-800',
  LOGIN: 'bg-gray-100 text-gray-800',
  KYC_SUBMITTED: 'bg-yellow-100 text-yellow-800',
  KYC_APPROVED: 'bg-green-100 text-green-800',
  KYC_REJECTED: 'bg-red-100 text-red-800',
  PAYMENT_INITIATED: 'bg-purple-100 text-purple-800',
  PAYMENT_CONFIRMED: 'bg-emerald-100 text-emerald-800',
  SUBMIT_RATING: 'bg-amber-100 text-amber-800',
  CREATE_JOB: 'bg-indigo-100 text-indigo-800',
  ASSIGN_JOB: 'bg-cyan-100 text-cyan-800',
  COMPLETE_JOB: 'bg-lime-100 text-lime-800',
  SEND_MESSAGE: 'bg-sky-100 text-sky-800',
};

function ActionBadge({ action }: { action: string }) {
  return (
    <span className={`px-2 py-1 rounded-full text-xs font-medium ${ACTION_STYLES[action] || 'bg-gray-100 text-gray-800'}`}>
      {action.replace(/_/g, ' ')}
    </span>
  );
}
```

---

## 5. Layout & Navigation

### Sidebar

```tsx
// Composants Shadcn : Sheet + ScrollArea + Separator
// Icônes Lucide : LayoutDashboard, Users, ShieldCheck, ScrollText, FolderTree, Settings, LogOut

const NAV_ITEMS = [
  { label: 'Tableau de bord',   icon: LayoutDashboard, path: '/admin/dashboard' },
  { label: 'Utilisateurs',      icon: Users,           path: '/admin/users' },
  { label: 'Vérifications KYC', icon: ShieldCheck,     path: '/admin/kyc', badge: kyPendingCount },
  { label: "Journal d'audit",   icon: ScrollText,      path: '/admin/audit' },
  { label: 'Catégories',        icon: FolderTree,      path: '/admin/categories' },
];
```

### Zustand store

```ts
interface UIStore {
  sidebarOpen: boolean;
  toggleSidebar: () => void;
  setSidebarOpen: (open: boolean) => void;
}

interface AuthStore {
  user: User | null;
  accessToken: string | null;
  isAuthenticated: boolean;
  isAdmin: boolean;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  checkAuth: () => Promise<void>;  // calls GET /auth/me
}
```

---

## 6. Axios Client avec Interceptor

```ts
// api/client.ts
const api = axios.create({
  baseURL: 'http://localhost:8080/api',
  headers: { 'Content-Type': 'application/json' },
});

api.interceptors.request.use((config) => {
  const token = useAuthStore.getState().accessToken;
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

api.interceptors.response.use(
  (res) => res,
  async (error) => {
    if (error.response?.status === 401) {
      try {
        // Tentative de refresh via cookie
        const { data } = await axios.post('http://localhost:8080/api/auth/refresh', {}, { withCredentials: true });
        useAuthStore.getState().setTokens(data.accesstoken, data.refreshtoken);
        error.config.headers.Authorization = `Bearer ${data.accesstoken}`;
        return axios(error.config);
      } catch {
        useAuthStore.getState().logout();
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);
```

---

## 7. Gestion des Erreurs et États

### React Query default options

```tsx
// App.tsx
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
      staleTime: 30_000,     // 30s avant re-fetch
    },
    mutations: {
      onError: (error) => {
        const message = (error as any)?.response?.data?.error || 'Une erreur est survenue';
        toast.error(message);
      },
    },
  },
});
```

### États vides

```tsx
// components/EmptyState.tsx
function EmptyState({ icon: Icon, title, description, action }) {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.95 }}
      animate={{ opacity: 1, scale: 1 }}
      className="flex flex-col items-center justify-center py-16 text-center"
    >
      <Icon className="h-16 w-16 text-text-hint mb-4" />
      <h3 className="text-lg font-medium text-text-primary">{title}</h3>
      <p className="text-text-secondary mt-1 mb-6">{description}</p>
      {action}
    </motion.div>
  );
}
```

---

## 8. Extensions Backend Recommandées

Pour un dashboard admin complet, créer un `AdminController.java` :

```java
@RestController
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    // Stats dashboard
    @GetMapping("/stats")
    public AdminStatsResponse getStats() { ... }

    // Users
    @GetMapping("/users")
    public List<UserResponse> getAllUsers() { ... }

    @GetMapping("/users/{id}")
    public UserResponse getUser(@PathVariable UUID id) { ... }

    @PutMapping("/users/{id}")
    public UserResponse updateUser(@PathVariable UUID id, @RequestBody AdminUpdateUserRequest req) { ... }

    @DeleteMapping("/users/{id}")
    public void deleteUser(@PathVariable UUID id) { ... }

    @PatchMapping("/users/{id}/role")
    public UserResponse updateRole(@PathVariable UUID id, @RequestBody Map<String, String> body) { ... }

    // Jobs (admin overview)
    @GetMapping("/jobs")
    public List<JobResponse> getAllJobs(
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String q
    ) { ... }

    // Transactions
    @GetMapping("/transactions")
    public List<PaymentResponse> getAllTransactions(
        @RequestParam(required = false) String status
    ) { ... }

    // Categories (CRUD)
    @PostMapping("/categories")
    public CategoryResponse createCategory(@RequestBody CreateCategoryRequest req) { ... }

    @PutMapping("/categories/{id}")
    public CategoryResponse updateCategory(@PathVariable UUID id, @RequestBody UpdateCategoryRequest req) { ... }

    @DeleteMapping("/categories/{id}")
    public void deleteCategory(@PathVariable UUID id) { ... }
}

record AdminStatsResponse(
    long totalUsers,
    long totalJobs,
    long pendingKyc,
    long completedTransactions,
    BigDecimal totalRevenue,
    long activeUsersToday
) {}
```

### Champs ajoutés par rapport à `UserResponse`

```java
// AdminUserResponse (étend UserResponse avec + d'infos)
record AdminUserResponse(
    UUID id, String firstName, String lastName, String username, String email,
    String role, boolean verified, KycStatus kycStatus,
    Double averageRating, Integer totalRatings,
    Date lastSeenAt,                 // nouvelle info
    long totalJobsCreated,
    long totalJobsCompleted,
    long totalTransactions
) {}
```

---

## 9. Résumé des Workflows

### Workflow KYC (admin)
```
1. Dashboard voit le badge "KYC en attente : 23"
2. Clique → /admin/kyc
3. Table filtrée par PENDING par défaut
4. Clique sur un document → Dialog avec le document uploadé
5. [Approuver] → useApproveKyc mutation → requête → invalide cache → toast succès
6. [Refuser] → Dialog "Motif du refus" → Textarea → useRejectKyc mutation
```

### Workflow Audit
```
1. Navigation → /admin/audit
2. DataTable paginée, triée par date desc
3. Filtre par type d'action (Select)
4. Filtre par utilisateur (Combobox avec recherche)
5. Chaque ligne animée avec Framer Motion (stagger)
```

### Workflow Users
```
1. Table listant tous les utilisateurs
2. Icônes de statut (email vérifié, KYC)
3. Actions dropdown : Détail | Promouvoir/Rétrograder | Supprimer
4. Confirmation dialog pour suppression
5. Détail utilisateur : page séparée avec tous ses jobs, transactions, logs
```

### Workflow Categories
```
1. Affiche l'arbre des catégories (parent → enfants)
2. Bouton "Ajouter une catégorie"
3. Drag to reorder ou champ displayOrder
4. Édition inline ou modal
5. Suppression avec confirmation
```

---

## 10. Couleurs et Composants Shadcn

### Mapping des couleurs métier vers Shadcn

```css
/* globals.css */
@layer base {
  :root {
    --primary: 213 94% 34%;       /* #0D47A1 */
    --primary-light: 210 79% 42%; /* #1565C0 */
    --primary-dark: 220 93% 23%;  /* #0A3470 */
    --secondary: 210 79% 46%;     /* #1976D2 */
    --destructive: 0 84% 60%;     /* #EF4444 */
    --warning: 38 92% 50%;        /* #F59E0B */
    --success: 160 84% 39%;       /* #10B981 */
    --info: 217 91% 60%;          /* #3B82F6 */
    --background: 210 20% 97%;    /* #F5F7FA */
    --foreground: 240 30% 14%;    /* #1A1A2E */
    --muted: 210 20% 96%;         /* #F0F4F8 */
    --muted-foreground: 218 11% 49%; /* #6B7280 */
    --border: 220 13% 91%;        /* #E5E7EB */
    --card: 0 0% 100%;            /* #FFFFFF */
    --radius: 0.5rem;
  }
}
```

### Composants Shadcn à utiliser

```
Button (variants: default, destructive, outline, ghost, secondary)
Badge (variants: default, secondary, destructive, outline)
Card + CardHeader + CardTitle + CardContent
Dialog + DialogContent + DialogHeader + DialogTitle + DialogDescription
Sheet (sidebar mobile)
Select
Input
Textarea
Avatar + AvatarFallback
DropdownMenu
Table + TableHeader + TableBody + TableRow + TableHead + TableCell
DataTable (via @tanstack/react-table)
Tabs
Separator
ScrollArea
Toast (sonner)
```

---

## 11. Animations (Framer Motion)

### Page transition
```tsx
// Layout transition wrapper
<motion.div
  key={location.pathname}
  initial={{ opacity: 0, x: 10 }}
  animate={{ opacity: 1, x: 0 }}
  exit={{ opacity: 0, x: -10 }}
  transition={{ duration: 0.2 }}
>
  <Outlet />
</motion.div>
```

### Table rows stagger
```tsx
<motion.tbody variants={{
  hidden: {},
  show: { transition: { staggerChildren: 0.03 } },
}}>
  {rows.map((row, i) => (
    <motion.tr
      key={row.id}
      variants={{
        hidden: { opacity: 0, y: 10 },
        show: { opacity: 1, y: 0 },
      }}
    >
      ...
    </motion.tr>
  ))}
</motion.tbody>
```

### Sidebar items
```tsx
// Slide-in on hover / active
<motion.div whileHover={{ x: 4 }} whileTap={{ scale: 0.98 }}>
  <NavLink to={item.path}>
    <item.icon className="h-5 w-5" />
    <span>{item.label}</span>
    {item.badge && <Badge>{item.badge}</Badge>}
  </NavLink>
</motion.div>
```

### StatCard count-up animation
```tsx
function CountUp({ value, duration = 1 }: { value: number; duration?: number }) {
  const [count, setCount] = useState(0);
  useEffect(() => {
    if (value === 0) return;
    const start = performance.now();
    const raf = requestAnimationFrame(function tick(now) {
      const elapsed = (now - start) / 1000;
      const progress = Math.min(elapsed / duration, 1);
      // easeOutCubic
      const eased = 1 - Math.pow(1 - progress, 3);
      setCount(Math.floor(eased * value));
      if (progress < 1) requestAnimationFrame(tick);
    });
    return () => cancelAnimationFrame(raf);
  }, [value, duration]);
  return <span>{count.toLocaleString()}</span>;
}
```

---

## 12. Exemple : Page Users Complete

```tsx
// pages/users/UsersListPage.tsx
export default function UsersListPage() {
  const { data: users, isLoading } = useUsers();
  const deleteMutation = useDeleteUser();
  const roleMutation = useUpdateUserRole();

  const columns: ColumnDef<User>[] = [
    {
      accessorKey: 'username',
      header: 'Utilisateur',
      cell: ({ row }) => (
        <div className="flex items-center gap-2">
          <Avatar className="h-8 w-8">
            <AvatarFallback>{row.original.username[0].toUpperCase()}</AvatarFallback>
          </Avatar>
          <div>
            <p className="font-medium">{row.original.username}</p>
            <p className="text-xs text-text-secondary">{row.original.email}</p>
          </div>
        </div>
      ),
    },
    {
      accessorKey: 'role',
      header: 'Rôle',
      cell: ({ row }) => (
        <Badge variant={row.original.role === 'ADMIN' ? 'default' : 'secondary'}>
          {row.original.role}
        </Badge>
      ),
    },
    {
      accessorKey: 'verified',
      header: 'Email',
      cell: ({ row }) => row.original.verified
        ? <CheckCircle2 className="h-5 w-5 text-success" />
        : <XCircle className="h-5 w-5 text-error" />,
    },
    {
      accessorKey: 'kycStatus',
      header: 'KYC',
      cell: ({ row }) => {
        const status = row.original.kycStatus;
        const colors = { PENDING: 'text-warning', VERIFIED: 'text-success', REJECTED: 'text-error' };
        return <span className={`font-medium ${colors[status] || ''}`}>{status}</span>;
      },
    },
    {
      id: 'actions',
      cell: ({ row }) => (
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon"><MoreHorizontal className="h-4 w-4" /></Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent align="end">
            <DropdownMenuItem onClick={() => navigate(`/admin/users/${row.original.id}`)}>
              <Eye className="mr-2 h-4 w-4" /> Voir détail
            </DropdownMenuItem>
            <DropdownMenuItem onClick={() => roleMutation.mutate({
              id: row.original.id,
              role: row.original.role === 'ADMIN' ? 'USER' : 'ADMIN',
            })}>
              <Shield className="mr-2 h-4 w-4" />
              {row.original.role === 'ADMIN' ? 'Rétrograder' : 'Promouvoir ADMIN'}
            </DropdownMenuItem>
            <DropdownMenuSeparator />
            <DropdownMenuItem
              className="text-error"
              onClick={() => deleteMutation.mutate(row.original.id)}
            >
              <Trash2 className="mr-2 h-4 w-4" /> Supprimer
            </DropdownMenuItem>
          </DropdownMenuContent>
        </DropdownMenu>
      ),
    },
  ];

  return (
    <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }}>
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-text-primary">Utilisateurs</h1>
        <Input placeholder="Rechercher..." className="w-64" />
      </div>
      <Card>
        <CardContent className="p-0">
          <DataTable columns={columns} data={users || []} isLoading={isLoading} />
        </CardContent>
      </Card>
    </motion.div>
  );
}
```

---

## 13. Dépendances package.json

```json
{
  "dependencies": {
    "react": "^18.3",
    "react-dom": "^18.3",
    "react-router-dom": "^6",
    "@tanstack/react-query": "^5",
    "zustand": "^5",
    "axios": "^1.7",
    "framer-motion": "^11",
    "lucide-react": "^0.400",
    "recharts": "^2",
    "react-hook-form": "^7",
    "zod": "^3",
    "@hookform/resolvers": "^3",
    "date-fns": "^4",
    "sonner": "^1",
    "@tanstack/react-table": "^8",
    "class-variance-authority": "^0.7",
    "clsx": "^2",
    "tailwind-merge": "^2"
  },
  "devDependencies": {
    "@shadcn/ui": "latest",
    "typescript": "^5.5",
    "tailwindcss": "^3.4",
    "postcss": "^8",
    "autoprefixer": "^10",
    "@types/react": "^18.3",
    "@types/react-dom": "^18.3",
    "vite": "^5",
    "@vitejs/plugin-react": "^4"
  }
}
```

---

## 14. Ordre d'Implémentation Recommandé

```
Phase 1 — Fondation
  1. Initialiser le projet Vite + React + TypeScript + Tailwind + Shadcn
  2. Configurer les couleurs dans globals.css et tailwind.config.ts
  3. Installer les dépendances (React Query, Zustand, Router, Axios, Framer Motion)
  4. Créer api/client.ts avec intercepteur Axios
  5. Créer store/auth.store.ts et ui.store.ts
  6. Créer LoginPage.tsx + AdminGuard
  7. Créer AdminLayout.tsx (sidebar + header + content)

Phase 2 — Dashboard
  8. DashboardPage.tsx avec 4 StatCards + graphiques Recharts
  9. Animations StatCard (count-up + stagger)

Phase 3 — Users
  10. Créer le AdminController côté backend
  11. UsersListPage avec DataTable
  12. Actions (promote, demote, delete)
  13. UserDetailPage (infos + jobs + transactions + logs)

Phase 4 — KYC
  14. KycListPage avec filtres (PENDING / VERIFIED / REJECTED)
  15. KycDetailDialog (image + approve/reject)
  16. Badge animé du compteur KYC dans la sidebar

Phase 5 — Audit
  17. AuditLogsPage avec DataTable paginée
  18. Filtres (type d'action, utilisateur)
  19. Animations stagger sur les lignes

Phase 6 — Catégories
  20. CategoriesPage avec arbre visuel
  21. CRUD (create, edit, delete) via AdminController
  22. Drag to reorder (optionnel)

Phase 7 — Polish
  23. Page transitions Framer Motion
  24. États vides et loaders
  25. Toast notifications sur les mutations
  26. Responsive mobile (sidebar Sheet)
  27. Tests des flows complets
```
