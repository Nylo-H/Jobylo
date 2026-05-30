# Workflow Complet : De la Sélection du Job au Paiement & Notation

## Diagramme du Cycle de Vie

```
           ┌──────────────────────────────────────────────────────┐
           │                  1. PARCOURIR                        │
           │         GET /jobs/available (filtres)                │
           │         GET /jobs/{jobId} (détail)                   │
           └──────────────────────┬───────────────────────────────┘
                                │
                                ▼
           ┌──────────────────────────────────────────────────────┐
           │                  2. POSTULER (Worker)                │
           │         POST /jobs/{jobId}/apply                     │
           │         └→ WS push NEW_APPLICATION au créateur       │
           └──────────────────────┬───────────────────────────────┘
                                │
                    ┌───────────┴───────────┐
                    │                       │
                    ▼                       ▼
     ┌────────────────────────┐  ┌────────────────────────┐
     │ 3a. GÉRER (Créateur)   │  │ 3b. VOIR MES (Worker)  │
     │ GET /jobs/{id}/applicants│  │ GET /applications/mine  │
     │ POST /jobs/{id}/reject  │  │                         │
     │ POST /jobs/{id}/assign  │  │                         │
     └───────────┬────────────┘  └────────────────────────┘
                 │
                 ▼
     ┌──────────────────────────────────────────────────────┐
     │        4. RÉALISATION (IN_PROGRESS)                  │
     │    Chat en temps réel via WebSocket                   │
     │    PATCH /jobs/{id}/status?status=DONE               │
     │    (créateur ou worker peut marquer DONE)            │
     └──────────────────────┬───────────────────────────────┘
                          │
                          ▼
     ┌──────────────────────────────────────────────────────┐
     │        5. PAIEMENT (Créateur → Worker)               │
     │    POST /payments  → status HELD (fonds bloqués)     │
     │    POST /payments/confirm → status COMPLETED         │
     │         (commission 5% retenue, 95% versé au worker)  │
     └──────────────────────┬───────────────────────────────┘
                          │
                          ▼
     ┌──────────────────────────────────────────────────────┐
     │        6. NOTATION (créateur ET worker)              │
     │    POST /ratings  (note 1-5 + commentaire)           │
     │    GET /ratings/user/{id} (voir les notes reçues)    │
     └──────────────────────────────────────────────────────┘
```

---

## 1. Parcourir & Sélectionner un Job

### Lister les jobs disponibles
```
GET /api/jobs/available
       ?categoryId=uuid       (filtre catégorie)
       &q=mot-clé             (recherche texte dans le titre)
       &minPrice=10000        (prix minimum)
       &maxPrice=50000        (prix maximum)
       &sort=date_asc|date_desc|price_asc|price_desc
       &location=Paris        (filtre par texte de lieu)
```

### Détail d'un job
```
GET /api/jobs/{jobId}

Réponse (JobResponse) :
{
  id, title, description, location, price,
  creatorId, creatorUsername,
  workerId, workerUsername,          // null tant que non assigné
  status,                            // PENDING | IN_PROGRESS | DONE
  createdAt, updatedAt,
  images: ["http://..."],
  categoryId, categoryName
}
```

### Mes jobs créés / assignés
```
GET /api/jobs/my-created      (auth requis, créateur)
GET /api/jobs/my-assigned     (auth requis, worker)
```

---

## 2. Postuler (côté Worker)

```
POST /api/jobs/{jobId}/apply
Content-Type: application/json
Authorization: Bearer <token>

Body (optionnel) :
{
  "coverLetter": "Bonjour, je suis très motivé pour ce job..."
}

Réponse (201 Created) : ApplicationResponse

Erreurs possibles :
  400 → Job déjà attribué, ou vous postulez à votre propre job
  403 → KYC non vérifié
  409 → Vous avez déjà postulé
```

**WS push :** Le créateur reçoit sur `/topic/notifications/{creatorId}` :
```json
{
  "type": "NEW_APPLICATION",
  "jobId": "uuid",
  "jobTitle": "Tondre la pelouse",
  "applicantId": "uuid",
  "applicantUsername": "bob_test",
  "coverLetter": "..."
}
```

---

## 3. Gérer les Candidatures (côté Créateur)

### Voir la liste des candidats
```
GET /api/jobs/{jobId}/applicants

Réponse : [ApplicationResponse...]

ApplicationResponse :
{
  id, jobId, jobTitle,
  workerId, workerUsername,
  coverLetter,
  status,        // PENDING | ACCEPTED | REJECTED | CANCELLED
  createdAt
}
```

### Compter les candidats en attente
```
GET /api/jobs/{jobId}/applicants/count
=> { "count": 5 }
```

### Rejeter un candidat
```
POST /api/jobs/{jobId}/reject/{workerId}
=> 204 No Content

Erreurs :
  400 → job déjà attribué ou candidature déjà traitée
  403 → seul le créateur peut rejeter
```
**WS push :** Le worker reçoit sur `/topic/notifications/{workerId}` :
```json
{
  "type": "APPLICATION_REJECTED",
  "jobId": "uuid",
  "jobTitle": "Tondre la pelouse",
  "message": "Votre candidature a été refusée"
}
```

### Assigner un worker (élire le gagnant)
```
POST /api/jobs/{jobId}/assign
Content-Type: application/json

{
  "workerId": "uuid"
}

Réponse : JobResponse (status → IN_PROGRESS, workerId renseigné)
```
**Effets automatiques :**
- L'élu → `application.status = ACCEPTED`
- **Tous les autres PENDING** → `REJECTED` automatiquement
- Chaque worker rejeté reçoit un push `APPLICATION_REJECTED`

### Voir mes candidatures (côté Worker)
```
GET /api/applications/mine
=> [ApplicationResponse...]
```

---

## 4. Phase de Réalisation (IN_PROGRESS)

Une fois le job en `IN_PROGRESS`, le créateur et le worker peuvent :

### 1. Discuter en temps réel
(Voir le guide `prompt-frontend-messaging-applications.md` — topics WS, envoi, lecture)

### 2. Marquer le job comme terminé
```
PATCH /api/jobs/{jobId}/status?status=DONE
Authorization: Bearer <token>
=> 200 JobResponse (status → DONE)
```
**Qui peut ?** Le **créateur** OU le **worker** assigné.
**Transition valide :** IN_PROGRESS → DONE uniquement.
**Audit :** `ActionType.COMPLETE_JOB`

---

## 5. Paiement (Escrow Simulé)

Le paiement se fait **après** que le job est `DONE`.
Le **créateur** est l'acheteur (celui qui paie). Le **worker** est le vendeur (celui qui reçoit).

### Commission
Configurable dans `application.yaml` :
```yaml
payment:
  commission-percentage: 5
```
Exemple : job à 10 000 XAF → commission 500 XAF → le worker reçoit 9 500 XAF.

### Initier le paiement (Créateur)
```
POST /api/payments
Content-Type: application/json
Authorization: Bearer <token>     // token du CRÉATEUR

{
  "jobId": "uuid"
}

Réponse (201 Created) : PaymentResponse (status → "HELD")
```
- KYC obligatoire
- Job doit être `DONE`
- Seul le **créateur du job** peut initier le paiement
- Un seul paiement par job

### Confirmer la livraison (Créateur)
```
POST /api/payments/confirm
Content-Type: application/json
Authorization: Bearer <token>     // token du CRÉATEUR

{
  "transactionId": "uuid"
}

Réponse : PaymentResponse (status → "COMPLETED")
```
- KYC obligatoire
- Seul l'acheteur (créateur) peut confirmer
- Les fonds passent de `HELD` à `COMPLETED` (simulé)
- Audit : `ActionType.PAYMENT_CONFIRMED`

### Voir une transaction
```
GET /api/payments/{transactionId}
GET /api/payments     (mes transactions, côté buyer OU seller)
```

### PaymentResponse (format complet)
```json
{
  "id": "uuid",
  "jobId": "uuid",
  "jobTitle": "Tondre la pelouse",
  "buyerId": "uuid",              // ID du créateur (celui qui paie)
  "buyerUsername": "alice_test",
  "sellerId": "uuid",             // ID du worker (celui qui reçoit)
  "sellerUsername": "bob_test",
  "amount": 10000.00,
  "commissionPercentage": 5.00,
  "commissionAmount": 500.00,
  "netAmount": 9500.00,           // montant versé au worker après commission
  "status": "HELD | COMPLETED | CANCELLED",
  "paymentMethod": "CARD",
  "createdAt": "...",
  "updatedAt": "..."
}
```

---

## 6. Notation (Rating)

La notation se fait **après** que le job est `DONE`.

### Qui note qui ?
| Rôle | Peut noter |
|------|-----------|
| Créateur | Le **worker** (targetType = WORKER) |
| Worker | Le **créateur** (targetType = CREATOR) |

- Note de 1 à 5 (`@Min(1) @Max(5)`)
- Un commentaire optionnel (500 caractères max)
- **Une seule note par personne par job** (contrainte unique `(rater_id, job_id)`)
- Impossible de se noter soi-même

### Soumettre une note
```
POST /api/ratings
Content-Type: application/json
Authorization: Bearer <token>

{
  "jobId": "uuid",
  "targetUserId": "uuid",      // l'ID de la personne notée
  "score": 5,                   // 1-5
  "comment": "Excellent travail !"   // optionnel
}

Réponse (201 Created) : RatingResponse
```
- KYC obligatoire
- Le job doit être `DONE`
- Vous devez être participant au job

### Voir les notes

```
GET /api/ratings/user/{userId}      // notes reçues par un utilisateur (public)
GET /api/ratings/job/{jobId}        // notes liées à un job (auth requis)
GET /api/ratings/mine               // mes notes reçues (auth requis)
```

### RatingResponse
```json
{
  "id": "uuid",
  "jobId": "uuid",
  "jobTitle": "Tondre la pelouse",
  "raterId": "uuid",
  "raterUsername": "alice_test",
  "targetId": "uuid",
  "targetUsername": "bob_test",
  "targetType": "WORKER | CREATOR",
  "score": 5,
  "comment": "Excellent travail !",
  "createdAt": "..."
}
```

### Recalcul automatique
À chaque nouvelle note, `User.averageRating` et `User.totalRatings` sont recalculés et persistés.
Ces champs sont visibles sur :
- `GET /auth/me`
- `GET /users/{id}`
- `GET /users/me/stats`

---

## 7. Résumé des Statuts (State Machine)

```
                  ┌──────────┐
                  │ PENDING  │ ← Création du job + candidatures ouvertes
                  └────┬─────┘
                       │ POST /jobs/{id}/assign
                       ▼
                  ┌──────────┐
                  │IN_PROGRESS│ ← Job attribué, chat + réalisation
                  └────┬─────┘
                       │ PATCH status=DONE (creator ou worker)
                       ▼
                  ┌──────────┐
                  │   DONE   │ ← Terminé, paiement + notation possible
                  └──────────┘
```

ApplicationStatus (pour chaque candidature) :
```
PENDING ──┬──→ ACCEPTED (via assign)
          └──→ REJECTED  (via reject ou assign d'un autre)
```

PaymentStatus :
```
HELD ──→ COMPLETED (via confirm delivery)
     └──→ CANCELLED (non implémenté côté API)
```

---

## 8. Checklist UI par Rôle

### 👤 Visiteur non connecté
- [ ] Parcourir les jobs : `GET /jobs/available`
- [ ] Voir le détail d'un job : `GET /jobs/{jobId}`
- [ ] Voir les notes d'un user : `GET /ratings/user/{userId}`

### 👨‍🎓 Worker connecté
- [ ] Postuler : `POST /jobs/{jobId}/apply`
- [ ] Voir mes candidatures : `GET /applications/mine`
- [ ] Si accepté → discuter avec le créateur via WS
- [ ] Marquer DONE : `PATCH /jobs/{id}/status?status=DONE`
- [ ] Attendre que le créateur initie le paiement
- [ ] Recevoir le paiement après confirmation du créateur
- [ ] Noter le créateur : `POST /ratings` (targetUserId = creatorId)

### 👨‍💼 Créateur connecté
- [ ] Créer un job : `POST /jobs`
- [ ] Voir candidats : `GET /jobs/{id}/applicants`
- [ ] Rejeter un candidat : `POST /jobs/{id}/reject/{workerId}`
- [ ] Assigner : `POST /jobs/{id}/assign`
- [ ] Discuter avec le worker via WS
- [ ] Marquer DONE : `PATCH /jobs/{id}/status?status=DONE`
- [ ] Payer le worker : `POST /payments`
- [ ] Confirmer le paiement : `POST /payments/confirm`
- [ ] Noter le worker : `POST /ratings` (targetUserId = workerId)

---

## 9. Toutes les Routes par Catégorie

### Jobs
| Méthode | Route | Auth | Rôle |
|---------|-------|------|------|
| GET | `/jobs/available` | - | Parcourir |
| GET | `/jobs/{id}` | - | Détail |
| POST | `/jobs` | Oui | Créer (KYC) |
| PUT | `/jobs/{id}` | Oui | Modifier (KYC) |
| DELETE | `/jobs/{id}` | Oui | Supprimer (KYC) |
| GET | `/jobs/my-created` | Oui | Mes jobs créés |
| GET | `/jobs/my-assigned` | Oui | Mes jobs assignés |
| POST | `/jobs/{id}/images` | Oui | Upload image (KYC) |
| DELETE | `/jobs/{id}/images` | Oui | Supprimer image (KYC) |
| POST | `/jobs/{id}/assign` | Oui | Assigner worker (KYC) |
| PATCH | `/jobs/{id}/status` | Oui | Changer statut |

### Candidatures
| Méthode | Route | Auth | Rôle |
|---------|-------|------|------|
| POST | `/jobs/{id}/apply` | Oui | Postuler (KYC) |
| GET | `/jobs/{id}/applicants` | Oui | Voir candidats (créateur) |
| GET | `/jobs/{id}/applicants/count` | Oui | Compter candidats |
| POST | `/jobs/{id}/reject/{workerId}` | Oui | Rejeter (créateur) |
| GET | `/applications/mine` | Oui | Mes candidatures |

### Paiements
| Méthode | Route | Auth | Rôle |
|---------|-------|------|------|
| POST | `/payments` | Oui | Initier le paiement (KYC, créateur) |
| POST | `/payments/confirm` | Oui | Confirmer le paiement (KYC, créateur) |
| GET | `/payments/{txId}` | Oui | Détail transaction |
| GET | `/payments` | Oui | Mes transactions |

### Notes
| Méthode | Route | Auth | Rôle |
|---------|-------|------|------|
| POST | `/ratings` | Oui | Noter (KYC) |
| GET | `/ratings/user/{id}` | - | Notes d'un user |
| GET | `/ratings/job/{id}` | Oui | Notes d'un job |
| GET | `/ratings/mine` | Oui | Mes notes reçues |
