# Guide Frontend — Système de Candidatures (Applications)

## Nouveaux endpoints

| Méthode | Path | Auth | Rôle | Usage |
|---------|------|------|------|-------|
| `POST` | `/jobs/{jobId}/apply` | JWT | Worker | Postuler avec coverLetter optionnelle |
| `GET` | `/jobs/{jobId}/applicants` | JWT | Creator | Liste des candidats |
| `POST` | `/jobs/{jobId}/reject/{workerId}` | JWT | Creator | Rejeter un candidat |
| `GET` | `/applications/mine` | JWT | Worker | Mes candidatures (statuts) |
| `GET` | `/jobs/{jobId}/applicants/count` | JWT | Creator | Nombre de candidats |

## Modèles Dart

```dart
@freezed
class ApplicationResponse with _$ApplicationResponse {
  factory ApplicationResponse({
    required String id,
    required String jobId,
    String? jobTitle,
    required String workerId,
    String? workerUsername,
    String? coverLetter,
    required String status,     // PENDING | ACCEPTED | REJECTED | CANCELLED
    DateTime? createdAt,
  }) = _ApplicationResponse;

  factory ApplicationResponse.fromJson(Map<String, dynamic> json) =>
      _$ApplicationResponseFromJson(json);
}
```

## Workflow UI complet

### Côté Worker

#### 1. Détail du job — Bouton "Postuler"

```
┌─────────────────────────────────────┐
│  Femme de ménage 3h     35 000 XAF  │
│  Publié par Alice                    │
│                                     │
│  [Image]                            │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  📩 Contacter le créateur    │   │  ← déjà existant
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │  ✅ Postuler à cette offre   │   │  ← NOUVEAU
│  └─────────────────────────────┘   │
│                                     │
│  👥 3 personnes ont postulé         │
│                                     │
└─────────────────────────────────────┘
```

**Comportement :**
- Si PAS encore postulé → bouton "✅ Postuler"
- Si déjà postulé (PENDING) → "⏳ Candidature envoyée" (disabled)
- Si déjà postulé (REJECTED) → "❌ Refusée" + bouton "Voir d'autres offres"
- Si l'utilisateur est le créateur du job → cacher le bouton

**Appel API au clic sur "Postuler" :**
```dart
// Option 1 : sans cover letter
final response = await dio.post('/jobs/$jobId/apply');

// Option 2 : avec cover letter (textarea optionnelle)
final response = await dio.post('/jobs/$jobId/apply', data: {
  'coverLetter': 'Bonjour, je suis disponible tout de suite !',
});
```

#### 2. Popup cover letter (optionnelle)

```
┌─────────────────────────────────────┐
│  Postuler à l'offre                  │
├─────────────────────────────────────┤
│                                     │
│  Message au créateur (optionnel) :  │
│  ┌─────────────────────────────────┐│
│  │ Bonjour, je suis disponible     ││
│  │ et j'ai 3 ans d'expérience...   ││
│  │                                 ││
│  └─────────────────────────────────┘│
│                                     │
│  ┌─────────────────────────────┐   │
│  │  ✅ Envoyer ma candidature  │   │
│  └─────────────────────────────┘   │
│  ┌─────────────────────────────┐   │
│  │  ⏭️ Postuler sans message   │   │
│  └─────────────────────────────┘   │
└─────────────────────────────────────┘
```

#### 3. Écran "Mes candidatures"

```
┌─────────────────────────────────────┐
│  ← Mes candidatures                 │
├─────────────────────────────────────┤
│                                     │
│  📋 En attente (3)                  │
│  ─────────────────────────────      │
│                                     │
│  • Femme de ménage - 35 000 XAF    │
│    Alice • Il y a 2h               │
│    Statut : ⏳ En attente           │
│    [Voir l'offre]                   │
│                                     │
│  • Jardinage - 25 000 XAF          │
│    Paul • Il y a 1j                │
│    Statut : ⏳ En attente           │
│    [Voir l'offre]                   │
│                                     │
│  📋 Acceptées (0)                   │
│                                     │
│  📋 Refusées (1)                    │
│  ─────────────────────────────      │
│                                     │
│  • Ménage bureau - 50 000 XAF      │
│    Statut : ❌ Refusée              │
│                                     │
└─────────────────────────────────────┘
```

**Appel API :**
```dart
final response = await dio.get('/applications/mine');
```

**Tri à faire côté frontend :** grouper par `status` (PENDING, ACCEPTED, REJECTED).

---

### Côté Créateur

#### 1. Détail du job — Compteur de candidats

```
┌─────────────────────────────────────┐
│  Femme de ménage 3h     [PENDING]   │
│  35 000 XAF                          │
│                                     │
│  ┌─────────────────────────────┐   │
│  │  👥 Voir les 5 candidatures  │   │  ← NOUVEAU
│  │  3 en attente • 2 refusées   │   │
│  └─────────────────────────────┘   │
│                                     │
│  [Modifier] [Supprimer]             │
└─────────────────────────────────────┘
```

**Appel API pour le compteur :**
```dart
final count = await dio.get('/jobs/$jobId/applicants/count');
// { "count": 3 }
```

#### 2. Écran liste des candidats

```
┌─────────────────────────────────────┐
│  ← Candidatures - Femme de ménage   │
├─────────────────────────────────────┤
│                                     │
│  🔵 En attente (3)                  │
│  ─────────────────────────────      │
│                                     │
│  👤 Jean D.    ★★★★☆  12 missions  │ <- cliquable → profil
│  📝 "Bonjour, je suis disponible"   │ <- coverLetter
│  ┌──────────┐ ┌──────────┐         │
│  │ 📩 Chat  │ │ ✅ Choisir│         │
│  └──────────┘ └──────────┘         │
│  ─────────────────────────────      │
│                                     │
│  👤 Marie L.   ★★★★★  25 missions  │
│  📝 "Expérience en nettoyage..."   │
│  ┌──────────┐ ┌──────────┐         │
│  │ 📩 Chat  │ │ ✅ Choisir│         │
│  └──────────┘ └──────────┘         │
│  ─────────────────────────────      │
│                                     │
│  🔴 Refusés (2)                    │
│  ─────────────────────────────      │
│                                     │
│  👤 Paul K.    ★★★☆☆  3 missions   │ <- grisé
│  ❌ Refusé manuellement             │
│  [📩 Chat]                          │
│                                     │
│  👤 Sophie M.  ★★★★☆  8 missions   │
│  ❌ Offre attribuée à un autre      │ <- automatique
│  [📩 Chat]                          │
│                                     │
└─────────────────────────────────────┘
```

**Appel API :**
```dart
final applicants = await dio.get('/jobs/$jobId/applicants');
```

**Actions sur chaque candidat :**
| Bouton | Action |
|--------|--------|
| `📩 Chat` | Naviguer vers la conversation avec ce worker |
| `✅ Choisir` | `POST /jobs/{jobId}/assign { "workerId": "..." }` |
| `❌ Rejeter` | `POST /jobs/{jobId}/reject/{userId}` |

#### 3. Confirmation avant de choisir

```
┌─────────────────────────────────────┐
│  Confirmer le choix                  │
├─────────────────────────────────────┤
│                                     │
│  Tu es sur le point de choisir      │
│  Jean D. pour "Femme de ménage 3h"  │
│                                     │
│  ⚠️ Les autres candidats seront     │
│  automatiquement notifiés et        │
│  leur candidature sera refusée.     │
│                                     │
│  ┌──────────────┐ ┌──────────────┐ │
│  │  Annuler      │ │  ✅ Confirmer│ │
│  └──────────────┘ └──────────────┘ │
└─────────────────────────────────────┘
```

---

## Workflow complet — Schéma des écrans

```
┌──────────────┐
│  JOB DETAIL   │ ─── worker ───→ [✅ Postuler] → POST /jobs/{id}/apply
│  (worker)     │                                    → Snackbar "Candidature envoyée"
└──────┬───────┘
       │
       │ creator
       ▼
┌──────────────┐
│  JOB DETAIL   │ ─── [👥 Voir les N candidatures]
│  (creator)    │
└──────┬───────┘
       │
       ▼
┌──────────────────┐
│  APPLICANTS LIST  │ ─── [✅ Choisir] → POST /jobs/{id}/assign
│  (creator)        │     → Confirm dialog
│                   │     → Redirect vers la messagerie
│                   │
│                   │ ─── [❌ Rejeter] → POST /jobs/{id}/reject/{userId}
│                   │     → Carte passe en grisé "Refusé"
│                   │
│                   │ ─── [📩 Chat] → ouvre ChatScreen avec ce worker
└──────────────────┘
```

---

## Notifications WebSocket — Nouveaux types

### `NEW_APPLICATION` (vers le créateur)

```json
// Topic: /topic/notifications/{creatorId}
{
  "type": "NEW_APPLICATION",
  "jobId": "uuid",
  "jobTitle": "Femme de ménage 3h",
  "applicantId": "uuid",
  "applicantUsername": "Jean D.",
  "coverLetter": "Bonjour, je suis disponible"
}
```

**Action frontend :** mettre à jour le compteur sur l'offre + snackbar "Jean D. a postulé"

### `APPLICATION_REJECTED` (vers le worker)

```json
// Topic: /topic/notifications/{workerId}
{
  "type": "APPLICATION_REJECTED",
  "jobId": "uuid",
  "jobTitle": "Femme de ménage 3h",
  "message": "L'offre a été attribuée à un autre candidat"
}
```

**Action frontend :** snackbar + mise à jour du statut dans "Mes candidatures"

---

## Résumé des topics WebSocket à souscrire

| Topic | Payload | Usage |
|-------|---------|-------|
| `/topic/notifications/{userId}` | `NEW_APPLICATION` | Créateur : nouvelle candidature |
| `/topic/notifications/{userId}` | `APPLICATION_REJECTED` | Worker : refus auto après assign |

---

## Ajouts à l'interface existante

### 1. `JobDetailScreen` (worker)
- Bouton "✅ Postuler" (état dépend de `GET /applications/mine`)
- Texte "👥 X personnes ont postulé"

### 2. `JobDetailScreen` (creator)
- Carte "👥 Voir les X candidatures" (liée au compteur)

### 3. Nouvel écran `ApplicantsScreen`
- Liste avec statuts (PENDING / ACCEPTED / REJECTED)
- Cover letter affichée
- Profil du worker cliquable
- Boutons Choisir / Rejeter / Chat

### 4. Nouvel écran `MyApplicationsScreen`
- Liste groupée par statut (En attente / Acceptées / Refusées)
- Chaque ligne : titre, prix, créateur, statut, date
- Tap → détails de l'offre

### 5. Navigation
- Barre du bas → onglet "Emplois" → sous-onglet "Mes candidatures" (worker)
- Barre du bas → onglet "Emplois" → sous-onglet "Mes annonces" → tap → candidatures
