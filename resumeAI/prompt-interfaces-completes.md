# Interfaces Complètes — Jobylo Marketplace App

## Palette Couleurs

```dart
// lib/core/theme/app_colors.dart
class AppColors {
  static const Color primary = Color(0xFF0D47A1);
  static const Color primaryLight = Color(0xFF1565C0);
  static const Color primaryDark = Color(0xFF0A3470);
  static const Color secondary = Color(0xFF1976D2);

  static const Color background = Color(0xFFF5F7FA);
  static const Color surface = Color(0xFFFFFFFF);
  static const Color surfaceVariant = Color(0xFFF0F4F8);

  static const Color textPrimary = Color(0xFF1A1A2E);
  static const Color textSecondary = Color(0xFF6B7280);
  static const Color textHint = Color(0xFF9CA3AF);

  static const Color success = Color(0xFF10B981);
  static const Color error = Color(0xFFEF4444);
  static const Color warning = Color(0xFFF59E0B);
  static const Color info = Color(0xFF3B82F6);
  static const Color urgent = Color(0xFFFF6B35);

  static const Color border = Color(0xFFE5E7EB);
  static const Color borderLight = Color(0xFFF3F4F6);

  static const Color price = Color(0xFF059669);
  static const Color badge = Color(0xFFEF4444);
  static const Color online = Color(0xFF10B981);
  static const Color offline = Color(0xFF9CA3AF);
}
```

---

## Architecture de Navigation

```
MaterialApp
└─ AuthGuard
   ├─ 🔓 Non connecté → AuthStack (Navigator)
   │   ├─ LoginScreen
   │   ├─ RegisterScreen
   │   ├─ ForgotPasswordScreen
   │   ├─ OtpVerificationScreen
   │   └─ ResetPasswordScreen
   │
   └─ 🔐 Connecté → AppShell (BottomNavigationBar, 4 tabs)
       │
       ├─ 📋 Tab 1 : Emplois (JobsStack)
       │   ├─ JobsListScreen (accueil avec filtres)
       │   ├─ JobDetailScreen
       │   ├─ CreateJobScreen
       │   ├─ EditJobScreen
       │   └─ JobApplicantsScreen (créateur)
       │       └─ ApplicantDetailSheet
       │
       ├─ 📁 Tab 2 : Mes annonces (MyJobsStack)
       │   ├─ MyJobsScreen (tab: Créées | Assignées | Candidatures)
       │   ├─ MyApplicationDetailScreen
       │   └─ JobCompletionScreen (marquer DONE)
       │
       ├─ 💬 Tab 3 : Messages (MessagesStack)
       │   ├─ ConversationsListScreen
       │   └─ ChatScreen
       │
       └─ 👤 Tab 4 : Profil (ProfileStack)
           ├─ ProfileScreen (mes infos + stats)
           ├─ EditProfileScreen
           ├─ KycScreen
           ├─ PaymentsHistoryScreen
           ├─ PaymentDetailScreen
           ├─ RatingsListScreen (mes notes reçues)
           ├─ SettingsScreen
           └─ AdminDashboardScreen (si role = ADMIN)
               ├─ AdminUsersScreen
               ├─ AdminKycScreen
               ├─ AdminAuditScreen
               └─ AdminCategoriesScreen
```

---

## 1. Auth Stack (Non connecté)

### 1.1 LoginScreen

```
┌─────────────────────────────────┐
│                                 │
│        🔵 Jobylo                │  ← Logo + app name
│                                 │
│  ┌─────────────────────────┐    │
│  │ 📧 Email                │    │  ← TextFormField, clavier email
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ 🔒 Mot de passe         │    │  ← TextFormField, obscured
│  └─────────────────────────┘    │
│        👁️ show/hide            │  ← Suffix icon
│                                 │
│  ┌─────────────────────────┐    │
│  │     Se connecter        │    │  ← ElevatedButton, primary color
│  └─────────────────────────┘    │
│                                 │
│  Mot de passe oublié ?         │  ← TextButton, navigue ForgotPassword
│                                 │
│  ──── ou ────                   │
│                                 │
│  Créer un compte               │  ← TextButton, navigue Register
│                                 │
└─────────────────────────────────┘

États :
  - Idle : formulaire vide
  - Loading : bouton disabled + CircularProgressIndicator
  - Error : message d'erreur sous le formulaire (ex: "Email ou mot de passe incorrect")
  - Success : navigate vers AppShell

Validations :
  - Email : required, format email
  - Password : required, min 6 caractères

API : POST /auth/login → LoginResponse { accesstoken, refreshtoken, verified }
Si verified == false → navigate vers OtpVerificationScreen(email)
```

### 1.2 RegisterScreen

```
┌─────────────────────────────────┐
│  ← Connexion                   │  ← AppBar avec back
│       Créer un compte           │
│                                 │
│  ┌─────────────────────────┐    │
│  │ Prénom                  │    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ Nom                     │    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ Nom d'utilisateur       │    │  ← unique, sans espaces
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ Email                   │    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ Mot de passe            │    │  ← min 6
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ Confirmer mot de passe  │    │
│  └─────────────────────────┘    │
│                                 │
│  ☑️ J'accepte les conditions   │  ← Checkbox
│                                 │
│  ┌─────────────────────────┐    │
│  │   Créer mon compte      │    │
│  └─────────────────────────┘    │
└─────────────────────────────────┘

API : POST /auth/register → UserResponse
Succès → navigate OtpVerificationScreen(email)

Erreurs :
  - "Nom d'utilisateur déjà utilisé" (409)
  - "Email déjà utilisé" (409)
```

### 1.3 OtpVerificationScreen

```
┌─────────────────────────────────┐
│  ← Retour                      │
│     Vérification email          │
│                                 │
│  Un code a été envoyé à         │
│  jean@example.com               │
│                                 │
│  ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐│
│  │ 1 │ │ 2 │ │ 3 │ │ 4 │ │ 5 ││  ← 6 champs OTP, auto-focus next
│  └───┘ └───┘ └───┘ └───┘ └───┘│
│                                 │
│  ⏱️ Code valable 5 min          │  ← Timer countdown
│                                 │
│  ┌─────────────────────────┐    │
│  │     Vérifier             │    │
│  └─────────────────────────┘    │
│                                 │
│  Renvoyer le code (30s)        │  ← Disabled si cooldown
│                                 │
│  ✉️ Utiliser un autre email    │  ← Navigue back vers Login
└─────────────────────────────────┘

API :
  POST /auth/verify-otp { email, otp } → LoginResponse (auto-login !)
  POST /auth/resend-otp?email=xxx → SuccessResponse

Comportement :
  - Si verified == false dans LoginResponse, ne pas bloquer, juste informer
  - Utiliser un PinPut avec 6 champs, auto-focus
  - Timer de 5 min avec affichage rouge < 60s
  - Resend disabled 30s
```

### 1.4 ForgotPasswordScreen

Voir le guide `prompt-frontend-forgot-password.md` pour le détail complet.

```
┌─────────────────────────────────┐
│  ← Connexion                    │
│     Mot de passe oublié         │
│                                 │
│  Saisissez votre email pour     │
│  recevoir un code OTP           │
│                                 │
│  ┌─────────────────────────┐    │
│  │ Email                   │    │
│  └─────────────────────────┘    │
│                                 │
│  ┌─────────────────────────┐    │
│  │   Envoyer le code       │    │
│  └─────────────────────────┘    │
│                                 │
│  ⚠️ Si le rate limit est       │
│     atteint : message 429       │
└─────────────────────────────────┘

→ Navigue vers ResetPasswordScreen
```

### 1.5 ResetPasswordScreen

```
┌─────────────────────────────────┐
│  ← Retour                       │
│     Nouveau mot de passe        │
│                                 │
│  ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐│
│  │ O │ │ T │ │ P │ │   │ │   ││  ← 6 champs OTP
│  └───┘ └───┘ └───┘ └───┘ └───┘│
│                                 │
│  ┌─────────────────────────┐    │
│  │ Nouveau mot de passe    │    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ Confirmer mot de passe  │    │
│  └─────────────────────────┘    │
│                                 │
│  ┌─────────────────────────┐    │
│  │   Réinitialiser         │    │
│  └─────────────────────────┘    │
└─────────────────────────────────┘

API : POST /auth/reset-password { email, otp, newPassword }
Succès → SnackBar + navigate LoginScreen
```

---

## 2. AppShell (Connecté)

### Bottom Navigation Bar

```
┌──────────┬──────────┬──────────┬──────────┐
│   📋     │   📁     │   💬     │   👤     │
│  Emplois │ Mes jobs │ Messages │  Profil  │
└──────────┴──────────┴──────────┴──────────┘

  Tab 0     Tab 1     Tab 2     Tab 3
```

- Badge rouge sur Messages si unreadCount > 0 (via WS `/topic/notifications`)
- Badge orange sur Profil si KYC status == PENDING

---

## 3. Tab 1 : Emplois (Jobs)

### 3.1 JobsListScreen (page d'accueil)

```
┌─────────────────────────────────┐
│  📍 Paris  ▾        🔍         │  ← Location filter chip + search
│                                 │
│  [Tous] [Développement] [Design]│  ← Horizontal CategoryChips, scrollable
│  [Rédaction] [Marketing]  [+]  │
│                                 │
│  Prix : [1000 ─────●── 50000]  │  ← RangeSlider (optionnel collapse)
│                                 │
│  ┌─────────────────────────┐    │
│  │ 🔵 Tondre la pelouse    │    │  ← JobCard
│  │ 📍 Paris 11e            │    │
│  │ 💰 35 000 XAF           │    │
│  │ 🕐 Il y a 2h            │    │
│  │ 👤 Jean_Dupont  ⭐4.5   │    │
│  │ [🏷️ Jardinage]         │    │  ← Category badge
│  └─────────────────────────┘    │
│                                 │
│  ┌─────────────────────────┐    │
│  │ 🖥️ Développer site web  │    │  ← JobCard
│  │ 📍 Remote               │    │
│  │ 💰 150 000 XAF          │    │
│  │ 🕐 Hier                 │    │
│  │ 👤 Marie_L  ⭐5.0       │    │
│  │ [🏷️ Tech]              │    │
│  └─────────────────────────┘    │
│                                 │
│        [🔽 Charger plus]        │  ← Pagination / infinite scroll
└─────────────────────────────────┘

États :
  - Loading : Shimmer cards (3-4 placeholder cards)
  - Empty : Illustration + "Aucune annonce pour le moment" + CTA "Créer une annonce"
  - Error : Message + "Réessayer"
  - Data : Liste scrollable

Components :
  JobCard : Card avec image (ou placeholder), title, price, location, time, creator, rating
  CategoryChips : Horizontal scrollable list
  FilterSheet : BottomSheet avec tous les filtres

API : GET /jobs/available?categoryId=&q=&minPrice=&maxPrice=&sort=&location=
Actions :
  - Tap card → JobDetailScreen
  - + FAB → CreateJobScreen (si authenticated)
  - Search → Input dans AppBar
```

### 3.2 JobDetailScreen

```
┌─────────────────────────────────┐
│  ← Emplois       ⋮ (menu)      │  ← AppBar
│                                 │
│  ┌─────────────────────────┐    │
│  │    Image principale      │    │  ← PageView si plusieurs images
│  │    ● ○ ○                 │    │  ← Dots indicator
│  └─────────────────────────┘    │
│                                 │
│  💰 35 000 XAF                  │  ← Price tag, large
│  # Tondre la pelouse            │  ← Title, bold
│                                 │
│  📍 Paris 11e                   │
│  🕐 Publié le 30/05/2026       │
│                                 │
│  ──── Description ────          │
│  Besoin de tondre la pelouse    │
│  pour une maison de 100m²...    │
│                                 │
│  ──── Catégorie ────            │
│  🏷️ Services > Jardinage       │
│                                 │
│  ──── Créé par ────             │
│  ┌────────────────────────┐     │
│  │ 🟦 Jean_Dupont         │     │  ← CreatorCard (avatar + nom + note)
│  │ ⭐ 4.5 (12 missions)   │     │
│  └────────────────────────┘     │
│                                 │
│  [ 📩 Postuler ]                │  ← Button, si worker connecté + KYC ok
│  [ 💬 Contacter ]               │  ← Button, si déjà candidaté ou créateur
│  [ ✅ Déjà postulé ]            │  ← Disabled, si déjà postulé
│  [ ✏️ Modifier ]                │  ← Si je suis le créateur + PENDING
│  [ 👥 Voir candidatures  (5) ]  │  ← Si je suis le créateur + badge count
│  [ 🟢 En cours ]                │  ← Badge si IN_PROGRESS
│  [ ✅ Terminé ]                 │  ← Badge si DONE
│                                 │
└─────────────────────────────────┘

BottomSheet "Postuler" :
  - TextArea pour cover letter (optionnel)
  - [Confirmer la candidature] bouton

États :
  - Loading : Shimmer
  - Not found : "Annonce introuvable"
  - Owner view : voir candidatures, éditer
  - Worker view : postuler, ou déjà postulé, ou en cours
  - Visitor view : juste voir, pas de CTA

API :
  GET /jobs/{id}
  POST /jobs/{id}/apply (si worker)
```

### 3.3 CreateJobScreen

```
┌─────────────────────────────────┐
│  ← Retour     Nouvelle annonce  │
│                                 │
│  ┌─────────────────────────┐    │
│  │ Titre de l'annonce *   │    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ Description              │    │  ← Multiline, max 2000
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ Localisation             │    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ Prix (XAF) *            │    │  ← Number keyboard
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ ▾ Catégorie             │    │  ← Dropdown, hiérarchique
│  └─────────────────────────┘    │
│                                 │
│  ┌─────────────────────────┐    │
│  │ + Ajouter des photos    │    │  ← Grid d'images, max 5
│  └─────────────────────────┘    │
│                                 │
│  ┌─────────────────────────┐    │
│  │   Publier l'annonce     │    │
│  └─────────────────────────┘    │
└─────────────────────────────────┘

API : POST /jobs { title, description, location, price, categoryId, images }
     POST /jobs/{id}/images (multipart)

Validation :
  - Titre : required, 5-100 chars
  - Prix : required, > 0
  - Catégorie : optionnelle (dropdown from GET /categories/tree)
```

### 3.4 EditJobScreen

Même layout que CreateJobScreen mais pré-rempli.
Seulement si job.status == PENDING et je suis le créateur.

```
API : PUT /jobs/{id}
     DELETE /jobs/{id}/images?imageUrl=...
     POST /jobs/{id}/images (multipart)
```

### 3.5 JobApplicantsScreen (créateur)

```
┌─────────────────────────────────┐
│  ← Détail    Candidatures (5)   │  ← count from GET /jobs/{id}/applicants/count
│                                 │
│  ┌─────────────────────────┐    │
│  │ 🟡 Marie L.             │    │
│  │ "Motivée, disponible    │    │  ← Cover letter preview
│  │  demain"                │    │
│  │                     [❌] │    │  ← Reject button
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ 🟡 Paul D.              │    │
│  │ "J'ai 5 ans d'exp."     │    │
│  │                     [❌] │    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ 🔴 Bob T. (Refusé)      │    │  ← Greyed out
│  └─────────────────────────┘    │
│                                 │
│  ┌─────────────────────────┐    │
│  │  Attribuer à Marie L.   │    │  ← AssignButton, pour le PENDING choisi
│  └─────────────────────────┘    │
│                                 │
│  ⚠️ En attribuant un, les      │
│     autres seront refusés       │
└─────────────────────────────────┘

API :
  GET /jobs/{id}/applicants
  POST /jobs/{id}/reject/{workerId}
  POST /jobs/{id}/assign { workerId }

ConfirmDialog pour assign :
  "Attribuer ce job à Marie L. ? Les autres candidats seront refusés."
  [Annuler] [Confirmer]
```

---

## 4. Tab 2 : Mes annonces (MyJobs)

### 4.1 MyJobsScreen

```
┌─────────────────────────────────┐
│       Mes annonces              │
│                                 │
│  [ Créées ] [ En cours ] [ Mes candidatures ]
│                                 │
│  ──── Tab : Créées ────         │
│  ┌─────────────────────────┐    │
│  │ Tondre la pelouse       │    │
│  │ 📍 Paris  🟡 PENDING    │    │  ← StatusBadge
│  │ 💰 35 000 XAF           │    │
│  │ 👥 5 candidatures       │    │  ← Tap → JobApplicantsScreen
│  │ [✏️ Modifier] [🗑️]     │    │
│  └─────────────────────────┘    │
│                                 │
│  ──── Tab : En cours ────       │
│  ┌─────────────────────────┐    │
│  │ Développer site web     │    │
│  │ 👤 Worker: Bob T.       │    │
│  │ 🔵 IN_PROGRESS          │    │
│  │ [💬] [✅ Terminer]      │    │  ← Chat + Done button
│  └─────────────────────────┘    │
│                                 │
│  ──── Tab : Mes candidatures ───│
│  ┌─────────────────────────┐    │
│  │ Développer site web     │    │
│  │ 👤 Créateur: Marie L.   │    │
│  │ 🟡 En attente           │    │  ← StatusBadge PENDING
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ Tondre la pelouse       │    │
│  │ 🔴 Refusé               │    │
│  └─────────────────────────┘    │
└─────────────────────────────────┘

API :
  GET /jobs/my-created
  GET /jobs/my-assigned
  GET /applications/mine

Actions :
  - Créées : modifier, supprimer, voir candidatures
  - En cours : chatter, marquer DONE
  - Candidatures : voir statut
```

### 4.2 Marquer DONE (dialog)

```
Dialog :
┌─────────────────────────────────┐
│  Confirmer la fin du job ?      │
│                                 │
│  Une fois terminé, le créateur  │
│  pourra procéder au paiement.   │
│                                 │
│  [Annuler]  [Oui, terminer]     │
└─────────────────────────────────┘

API : PATCH /jobs/{id}/status?status=DONE
```

### 4.3 MyApplicationDetailScreen

```
┌─────────────────────────────────┐
│  ← Mes annonces                 │
│     Candidature                  │
│                                 │
│  Développer site web            │
│  Créateur : Marie L.            │
│                                 │
│  Statut : 🟡 En attente         │
│                                 │
│  Lettre de motivation :         │
│  "Bonjour, je suis développeur  │
│   fullstack avec 5 ans..."      │
│                                 │
│  Soumis le 30/05/2026           │
└─────────────────────────────────┘
```

---

## 5. Tab 3 : Messages

### 5.1 ConversationsListScreen

```
┌─────────────────────────────────┐
│       Messages                  │
│                                 │
│  ┌─────────────────────────┐    │
│  │ 🔍 Rechercher...        │    │  ← SearchBar
│  └─────────────────────────┘    │
│                                 │
│  ┌─────────────────────────┐    │
│  │ 🟦 Bob T.     🟢 EN LIGNE│   │  ← Online indicator via WS
│  │ 📱 Je suis dispo demain  │    │  ← Last message
│  │ 🕐 14:30        [2] 🔴  │    │  ← Unread badge
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ 🟦 Marie L.   🔴 ligne │    │  ← lastSeenAt si offline
│  │ ✅✅ Merci beaucoup !    │    │  ← Read receipt
│  │ 🕐 Hier                 │    │
│  └─────────────────────────┘    │
│                                 │
│  ──── Aucune conversation ────  │
│  Postulez à une annonce pour    │
│  démarrer une conversation      │
└─────────────────────────────────┘

API : GET /messages/conversations

WS : /topic/notifications/{userId} → NEW_MESSAGE → update unread count
WS : /topic/presence → online/offline indicator

Actions :
  - Tap → ChatScreen
  - Swipe to delete (optionnel)
```

### 5.2 ChatScreen

```
┌─────────────────────────────────┐
│  ← Messages   Bob T.    🟢     │  ← Online status
│                                 │
│  ┌─────────────────────────┐    │
│  │   📋 Tondre la pelouse  │    │  ← Job info banner (tap → JobDetail)
│  │   💰 35 000 XAF         │    │
│  └─────────────────────────┘    │
│                                 │
│  ┌─────────────────────────┐    │
│  │             10:30  ┌──┐ │    │
│  │              Moi   │Bonjour!  │
│  │                    └──┘ │    │
│  ├─────────────────────────┤    │
│  │ ┌──┐                    │    │
│  │ │Salut !   Bob T. 10:31│    │  ← Bulle reçue
│  │ └──┘                   │    │
│  ├─────────────────────────┤    │
│  │ ┌──┐                    │    │
│  │ │Je suis dispo demain  │    │
│  │ │       Bob T. 10:32   │    │
│  │ └──┘              ✅✅ │    │  ← Read receipt
│  └─────────────────────────┘    │
│                                 │
│  ┌─────────────────────────┐    │
│  │ Tapez un message...  📎 │    │  ← Input row + send button
│  └─────────────────────────┘    │
└─────────────────────────────────┘

API :
  GET /messages/conversation/{id}?page=0&size=50 (paginated)
  POST /messages { conversationId, content }
  PATCH /messages/conversation/{id}/read (batch mark read)

WS :
  /topic/messages/{id} → nouveaux messages temps réel
  /topic/read/{id} → ✅✅ receipts

Comportement :
  - Auto-scroll vers le bas
  - Load more (pagination) en scrollant vers le haut
  - Chargement initial des messages
  - Mark as read automatique en ouvrant la conversation
  - Affichage des ✅✅ quand l'autre a lu
```

---

## 6. Tab 4 : Profil

### 6.1 ProfileScreen

```
┌─────────────────────────────────┐
│       Mon Profil      ⚙️      │  ← Settings icon
│                                 │
│  ┌─────────────────────────┐    │
│  │       🟦                │    │  ← Avatar (photo or initials)
│  │    Jean Dupont           │    │
│  │    @jean_dupont         │    │
│  │    jean@mail.com        │    │
│  └─────────────────────────┘    │
│                                 │
│  ⭐ 4.5  (12 évaluations)       │
│                                 │
│  ┌──────┐ ┌──────┐ ┌──────┐    │
│  │📋 15 │ │✅ 8  │ │📝 12 │    │  ← Stats cards
│  │Jobs  │ │Fait  │ │Candid│    │
│  └──────┘ └──────┘ └──────┘    │
│                                 │
│  ──── KYC ────                  │
│  🟡 En attente de vérification  │  ← KYC status card
│  [Soumettre un document]        │  ← Si PENDING ou REJECTED
│  🟢 Vérifié                     │  ← Si VERIFIED
│                                 │
│  ──── Actions ────               │
│  📁 Mes transactions            │  → PaymentsHistoryScreen
│  ⭐ Mes évaluations            │  → RatingsListScreen
│  📊 Voir mes statistiques      │  → Stats expandable
│  🛡️ Devenir ADMIN              │  ← Hidden, only if already ADMIN
│                                 │
│  🚪 Déconnexion                 │  ← ConfirmDialog
└─────────────────────────────────┘

API :
  GET /auth/me
  GET /kyc/status
  GET /users/me/stats

KYC Status cards :
  - NONE : "Vérifiez votre identité pour accéder à toutes les fonctionnalités" + [Soumettre]
  - PENDING : "En cours de vérification" + badge jaune
  - VERIFIED : badge vert + "Vérifié ✓"
  - REJECTED : "Document refusé" + motif + [Soumettre à nouveau]
```

### 6.2 EditProfileScreen

```
┌─────────────────────────────────┐
│  ← Profil    Modifier profil    │
│                                 │
│  ┌─────────────────────────┐    │
│  │    🟦  [Changer photo]  │    │  ← Tap to pick image
│  └─────────────────────────┘    │
│                                 │
│  ┌─────────────────────────┐    │
│  │ Prénom                  │    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ Nom                     │    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ Nom d'utilisateur       │    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ Email (non modifiable)  │    │  ← Disabled
│  └─────────────────────────┘    │
│                                 │
│  ┌─────────────────────────┐    │
│  │   Enregistrer           │    │
│  └─────────────────────────┘    │
└─────────────────────────────────┘

API : POST /auth/profile/photo (multipart)
      PUT /users/{id}
```

### 6.3 KycScreen

```
┌─────────────────────────────────┐
│  ← Profil   Vérification KYC    │
│                                 │
│  ┌─────────────────────────┐    │
│  │  Pourquoi vérifier ?    │    │  ← Info card
│  │  La vérification permet │    │
│  │  de...                   │    │
│  └─────────────────────────┘    │
│                                 │
│  Type de document :             │
│  ○ Carte d'identité             │  ← Radio buttons
│  ● Passeport                    │
│  ○ Permis de conduire           │
│                                 │
│  ┌─────────────────────────┐    │
│  │  + Télécharger le       │    │  ← File picker
│  │    document (photo/PDF)  │    │
│  └─────────────────────────┘    │
│  📄 passeport.jpg ✓            │  ← Selected file
│                                 │
│  ┌─────────────────────────┐    │
│  │   Soumettre              │    │
│  └─────────────────────────┘    │
│                                 │
│  ──── Statut actuel ────        │
│  🟡 En attente de vérification  │
└─────────────────────────────────┘

API :
  POST /kyc/upload (multipart)
  POST /kyc/submit { fileUrl, documentType }
  GET /kyc/status
```

### 6.4 PaymentsHistoryScreen

```
┌─────────────────────────────────┐
│  ← Profil    Transactions       │
│                                 │
│  ┌──────────────────────────────│
│  │ 💰 Tondre la pelouse       │
│  │ En tant que : Créateur     │  ← Role badge
│  │ Montant : 35 000 XAF       │
│  │ Commission : -1 750 XAF    │
│  │ Net versé : 33 250 XAF     │
│  │ ✅ Complété le 30/05/2026  │
│  └─────────────────────────────│
│  ┌──────────────────────────────│
│  │ 💰 Développer site web     │
│  │ En tant que : Worker       │
│  │ Net reçu : 142 500 XAF     │
│  │ ✅ Complété le 28/05/2026  │
│  └─────────────────────────────│
│  ┌──────────────────────────────│
│  │ 🔵 Paiement en attente...  │
│  │ Montant : 50 000 XAF       │
│  │ Statut : HELD              │
│  │ [Confirmer le paiement]    │  ← Si je suis le créateur
│  └─────────────────────────────│
└─────────────────────────────────┘

API : GET /payments → liste des transactions
POST /payments/confirm { transactionId } → PaymentResponse
```

### 6.5 RatingsListScreen

```
┌─────────────────────────────────┐
│  ← Profil    Mes évaluations     │
│                                 │
│  ⭐ 4.5 moyenne sur 12 avis     │
│                                 │
│  ┌─────────────────────────┐    │
│  │ ⭐⭐⭐⭐⭐                │    │
│  │ "Excellent travail !"   │    │
│  │ 🟦 Jean D. sur Tondre   │    │
│  │ 🕐 28/05/2026           │    │
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ ⭐⭐⭐⭐                  │    │
│  │ "Bon travail"           │    │
│  │ 🟦 Marie L. sur Site web│    │
│  │ 🕐 25/05/2026           │    │
│  └─────────────────────────┘    │
└─────────────────────────────────┘

API : GET /ratings/mine
     GET /ratings/user/{id} (public)
```

### 6.6 SettingsScreen

```
┌─────────────────────────────────┐
│  ← Profil    Paramètres         │
│                                 │
│  🌙 Mode sombre                 │  ← Switch
│  🔔 Notifications push         │  ← Switch
│  🌐 Langue : Français ▾       │  ← Dropdown
│                                 │
│  ──── Informations ────         │
│  Version : 1.0.0                │
│                                 │
│  🚪 Déconnexion                 │  ← Red, ConfirmDialog
└─────────────────────────────────┘
```

---

## 7. Paiement & Notation (Post-Job)

Ces screens ne sont pas des tabs mais accessibles depuis le job detail ou les notifications.

### 7.1 Paiement : Initier (créateur)

```
┌─────────────────────────────────┐
│  ← Emploi    Paiement           │
│                                 │
│  Job : Tondre la pelouse        │
│  Worker : Bob T.                │
│                                 │
│  ┌─────────────────────────┐    │
│  │ Montant du job          │    │
│  │ 35 000 XAF              │    │
│  │                         │    │
│  │ Commission (5%)         │    │
│  │ -1 750 XAF              │    │
│  │ ─────────────────────   │    │
│  │ Net versé au worker     │    │
│  │ 33 250 XAF              │    │
│  └─────────────────────────┘    │
│                                 │
│  💳 Paiement simulé             │
│                                 │
│  ┌─────────────────────────┐    │
│  │   Payer 35 000 XAF      │    │
│  └─────────────────────────┘    │
└─────────────────────────────────┘

API : POST /payments { jobId }

Conditions : job.status == DONE, je suis le créateur, KYC ok
```

### 7.2 Paiement : Confirmer (créateur)

```
Dialog :
┌─────────────────────────────────┐
│  Confirmer le paiement ?        │
│                                 │
│  33 250 XAF seront versés à     │
│  Bob T.                         │
│                                 │
│  ⚠️ Action irréversible         │
│                                 │
│  [Annuler]  [Confirmer]         │
└─────────────────────────────────┘

API : POST /payments/confirm { transactionId }
```

### 7.3 Noter (créateur ou worker)

```
┌─────────────────────────────────┐
│  ← Emploi    Noter              │
│                                 │
│  Notez Bob T. pour le job       │
│  "Tondre la pelouse"            │
│                                 │
│  ⭐⭐⭐⭐⭐                       │  ← Tap to rate (1-5)
│                                 │
│  ┌─────────────────────────┐    │
│  │ Votre commentaire       │    │  ← TextField optionnel
│  └─────────────────────────┘    │
│                                 │
│  ┌─────────────────────────┐    │
│  │   Soumettre la note     │    │
│  └─────────────────────────┘    │
└─────────────────────────────────┘

API : POST /ratings { jobId, targetUserId, score, comment }

Conditions :
  - job.status == DONE
  - Je suis participant (créateur ou worker)
  - Je note l'autre participant
  - Une seule note par job
```

---

## 8. Admin Dashboard (role = ADMIN)

### 8.1 AdminDashboardScreen

```
┌─────────────────────────────────┐
│  ⚙️ Admin   Tableau de bord     │
│                                 │
│  ┌──────┐ ┌──────┐ ┌──────┐    │
│  │1 234 │ │ 456  │ │  23  │    │  ← StatCards with count-up anim
│  │Users │ │ Jobs │ │KYC 🟡│    │
│  └──────┘ └──────┘ └──────┘    │
│                                 │
│  ┌─────────────────────────┐    │
│  │ 📈 Jobs par mois (chart)│    │  ← Graphique barres
│  └─────────────────────────┘    │
│  ┌─────────────────────────┐    │
│  │ 🕐 Activité récente     │    │  ← Timeline des derniers logs
│  └─────────────────────────┘    │
│                                 │
│  [👥 Users] [🪪 KYC] [📜 Audit] │ ← Quick action buttons
│  [📋 Jobs] [📂 Catégories]      │
└─────────────────────────────────┘

Navigation bottom : Dashboard | Users | KYC | Audit | Catégories
```

### 8.2 AdminUsersScreen (DataTable)

```
┌─────────────────────────────────┐
│  ← Admin   Utilisateurs         │
│                                 │
│  🔍 [Rechercher...]             │
│                                 │
│  ┌──────────┬────────┬────┬────┐│
│  │Utilisateur│ Rôle  │KYC│Act.││  ← Table
│  ├──────────┼────────┼────┼────┤│
│  │Jean D.   │ USER   │🟢 │ ⋮ ││
│  │Marie L.  │ ADMIN  │🟢 │ ⋮ ││
│  │Bob T.    │ USER   │🟡 │ ⋮ ││
│  └──────────┴────────┴────┴────┘│
└─────────────────────────────────┘

Actions dropdown : Voir, Promouvoir ADMIN, Rétrograder USER, Supprimer
```

### 8.3 AdminKycScreen

```
┌─────────────────────────────────┐
│  ← Admin   Vérifications KYC    │
│                                 │
│  [Tous] [🟡 En attente] [🟢 OK] [🔴 Rejeté]
│                                 │
│  ┌─────────────────────────┐    │
│  │ 🟡 Jean D. - Carte ID   │    │
│  │    12/05/2026   [Voir]  │    │
│  │ 🟡 Marie L. - Passeport │    │
│  │    11/05/2026   [Voir]  │    │
│  └─────────────────────────┘    │
│                                 │
│  → Dialog voir :                │
│  ┌─────────────────────────┐    │
│  │  Image du document       │    │
│  │  [Approuver] [Refuser]   │    │
│  └─────────────────────────┘    │
└─────────────────────────────────┘
```

### 8.4 AdminAuditScreen

```
┌─────────────────────────────────┐
│  ← Admin   Journal d'audit      │
│                                 │
│  🔍 [Rechercher...]             │
│  ▾ [Toutes les actions]         │  ← Filtre par type
│  ▾ [Tous les utilisateurs]      │  ← Filtre par user
│                                 │
│  ┌─────────────────────────┐    │
│  │ 30/05 14:30 Jean D.     │    │
│  │ 📝 Inscription          │    │
│  │ 30/05 14:35 Marie L.    │    │
│  │ 🪪 KYC soumis           │    │
│  │ 30/05 15:00 Admin       │    │
│  │ ✅ KYC approuvé         │    │
│  └─────────────────────────┘    │
└─────────────────────────────────┘
```

---

## 9. Composants Transverses

### JobCard (utilisé partout)
```dart
// Props : job, onTap, showStatus, showActions
// Childen : title, price, location, timeAgo, creator, statusBadge, categoryTag
```

### StatusBadge
```dart
// PENDING → 🟡 "En attente" (warning color)
// IN_PROGRESS → 🔵 "En cours" (info color)
// DONE → ✅ "Terminé" (success color)
// ACCEPTED → 🟢 "Accepté"
// REJECTED → 🔴 "Refusé" (error color)
// PENDING KYC → 🟡 "En attente"
// VERIFIED → 🟢 "Vérifié"
// REJECTED KYC → 🔴 "Rejeté"
// HELD → 🔵 "En attente"
// COMPLETED → 🟢 "Complété"
```

### EmptyState
```dart
// Icon + title + description + action button
```

### LoadingShimmer
```dart
// Card placeholder avec animation pulse
```

### ConfirmationDialog
```dart
// Title + message + cancel + confirm (danger variant si destructive)
```

### AvatarWithOnline
```dart
// CircleAvatar + green/gray dot indicator
```

---

## 10. États Global à Gérer

| État | Store | Technologie |
|------|-------|-------------|
| Utilisateur connecté + token | `authStore` | Zustand |
| Unread count messages | `messageStore` | Zustand (mis à jour via WS) |
| Online users map | `presenceStore` | Zustand (via WS `/topic/presence`) |
| Notifications toast | `notificationStore` | Zustand (via WS `/topic/notifications`) |
| KYC status | `authStore` | Inclus dans User |
| Mode sombre | `settingsStore` | SharedPreferences |

---

## 11. Ordre d'Implémentation Recommandé

```
Phase 1 — Auth
  LoginScreen → RegisterScreen → OTP → ForgotPassword → ResetPassword

Phase 2 — Navigation & Profil
  AppShell (BottomNav) → ProfileScreen → EditProfile → KYC

Phase 3 — Emplois
  JobsListScreen → JobDetail → CreateJob → EditJob

Phase 4 — Messages
  ConversationsList → ChatScreen (avec WS temps réel)

Phase 5 — Candidatures & Assignation
  MyJobsScreen (3 tabs) → JobApplicantsScreen → Assign

Phase 6 — Paiement & Notation
  Payment screens → Rating screens → Historique

Phase 7 — Admin
  Admin screens (dashboard, users, kyc, audit)
```
