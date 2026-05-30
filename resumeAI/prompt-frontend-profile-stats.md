# Guide Frontend — Stats du profil

## Endpoint unique

```
GET /users/me/stats
Auth: Bearer JWT
```

### Réponse

```json
{
  "totalJobsCreated": 5,
  "totalJobsInProgress": 1,
  "totalJobsCompleted": 3,
  "averageRating": 4.5,
  "totalRatings": 12,
  "totalApplicationsReceived": 8,
  "totalApplicationsSent": 3
}
```

### Modèle Dart

```dart
@freezed
class UserStatsResponse with _$UserStatsResponse {
  factory UserStatsResponse({
    required int totalJobsCreated,
    required int totalJobsInProgress,
    required int totalJobsCompleted,
    double? averageRating,
    required int totalRatings,
    required int totalApplicationsReceived,
    required int totalApplicationsSent,
  }) = _UserStatsResponse;

  factory UserStatsResponse.fromJson(Map<String, dynamic> json) =>
      _$UserStatsResponseFromJson(json);
}
```

---

## Mapping des stats dans le profil

### Page Profil — maquette

```
┌─────────────────────────────────────┐
│  ← Paramètres        Modifier       │
├─────────────────────────────────────┤
│                                     │
│     [Photo de profil]               │
│       Jean Dupont                   │
│       jean@mail.com                 │
│                                     │
│       ★★★★☆  4.5 (12 avis)         │
│                                     │
│  ┌─────────────────────────────┐   │
│  │ 📊 Mes statistiques          │   │
│  ├─────────────────────────────┤   │
│  │ 📋 Offres créées        5   │   │
│  │ 🔄 En cours              1   │   │
│  │ ✅ Terminées              3   │   │
│  │ 👥 Candidatures reçues   8   │   │
│  │ 📩 Candidatures envoyées 3   │   │
│  └─────────────────────────────┘   │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  Voir mes annonces           │   │  → GET /jobs/my-created
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │  Voir mes candidatures       │   │  → GET /applications/mine
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │  KYC : Verifié ✅             │   │
│  └─────────────────────────────┘   │
│                                     │
└─────────────────────────────────────┘
```

### Code d'appel

```dart
Future<UserStatsResponse> loadStats() async {
  final response = await dio.get('/users/me/stats');
  return UserStatsResponse.fromJson(response.data);
}
```

### Remarque importante

Quand tu consultes **le profil d'un autre utilisateur** (`GET /users/{id}`), les stats ne sont pas incluses. Seul le `UserResponse` est retourné avec `averageRating` et `totalRatings` :

```json
// GET /users/{id}
{
  "id": "uuid",
  "username": "jean",
  "firstName": "Jean",
  "lastName": "Dupont",
  "averageRating": 4.5,
  "totalRatings": 12,
  ...
}
```

Les stats détaillées (`totalJobsCreated`, etc.) sont **privées** — disponibles uniquement pour l'utilisateur connecté via `GET /users/me/stats`.

---

## Optimisation des appels

### Avant (sans stats endpoint)

```
Profil utilisateur :
  1. GET /auth/me           → note moyenne ✅
  2. GET /jobs/my-created   → compter les jobs (liste complète chargée)
  3. GET /applications/mine → compter les candidatures (liste complète)
  4. GET /jobs/my-assigned  → compter les en cours (liste complète)
```

### Après (avec stats endpoint)

```
Profil utilisateur :
  1. GET /users/me/stats    → TOUT en un appel (~200 octets)
```

---

## Navigation recommandée

| Action utilisateur | Endpoint | Écran |
|---|---|---|
| Voir profil | `GET /users/me/stats` | ProfileScreen |
| Voir ses annonces | `GET /jobs/my-created` | MyJobsScreen |
| Voir ses missions | `GET /jobs/my-assigned` | MyJobsScreen (onglet) |
| Voir ses candidatures | `GET /applications/mine` | MyApplicationsScreen |
| Voir profil d'un autre | `GET /users/{id}` | PublicProfileScreen |
