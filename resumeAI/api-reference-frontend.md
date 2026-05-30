# API Jobylo — Référence Frontend

> **Base URL** : `http://localhost:8080/api`  
> **Context-path** : `/api` (tous les endpoints sont préfixés)  
> **Content-Type** : `application/json` (sauf `multipart/form-data` pour les uploads)  
> **Auth** : Bearer JWT dans le header `Authorization: Bearer <token>`  
> **WebSocket** : `ws://localhost:8080/api/ws` (STOMP over SockJS)

---

## 1. Authentification

### 1.1 Inscription

```
POST /auth/register
Auth: ❌ (public)
```

**Body :**
```json
{
  "firstName": "Alice",
  "lastName": "Durand",
  "username": "alice",
  "email": "alice@email.com",
  "password": "monMotDePasse123"
}
```

**Réponse `201 Created` :**
```json
{
  "id": "uuid",
  "firstName": "Alice",
  "lastName": "Durand",
  "username": "alice",
  "email": "alice@email.com",
  "photoProfile": null,
  "role": "USER",
  "verified": false,
  "kycStatus": null,
  "averageRating": null,
  "totalRatings": null
}
```

> Un OTP est envoyé par email à l'adresse fournie (MailDev en dev).

---

### 1.2 Vérification OTP

```
POST /auth/verify-otp
Auth: ❌ (public)
```

**Body :**
```json
{
  "email": "alice@email.com",
  "otp": "123456"
}
```

**Réponse :**
```
Compte vérifié avec succès
```

---

### 1.3 Renvoyer OTP

```
POST /auth/resend-otp?username=alice
Auth: ❌ (public)
```

**Réponse :**
```json
{
  "message": "Nouveau code OTP envoyé"
}
```

---

### 1.4 Connexion

```
POST /auth/login
Auth: ❌ (public)
```

**Body :**
```json
{
  "username": "alice",
  "password": "monMotDePasse123"
}
```

**Réponse :**
```json
{
  "accesstoken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshtoken": null
}
```

> Le `refreshToken` est aussi placé dans un cookie `httpOnly` sur `/auth/refresh`.

---

### 1.5 Rafraîchir le token

```
POST /auth/refresh
Auth: ❌ (public, mais nécessite le cookie httpOnly)
Cookie: refreshToken=...
```

**Réponse :**
```json
{
  "accesstoken": "eyJhbGciOiJIUzI1NiJ9...",
  "refreshtoken": null
}
```

---

### 1.6 Mon profil

```
GET /auth/me
Auth: ✅ Bearer JWT
```

**Réponse :**
```json
{
  "id": "uuid",
  "username": "alice",
  "email": "alice@email.com",
  "role": "USER",
  "verified": true,
  "kycStatus": "VERIFIED",
  "photoProfil": null,
  "averageRating": 4.5,
  "totalRatings": 3
}
```

---

## 2. Catégories

> Les catégories sont **publiques** (aucune authentification requise).

### 2.1 Arbre complet des catégories

```
GET /categories/tree
Auth: ❌ (public)
```

**Réponse :**
```json
[
  {
    "id": "uuid",
    "name": "Services à domicile",
    "description": null,
    "icon": "🧹",
    "displayOrder": 1,
    "subcategories": [
      {
        "id": "uuid",
        "name": "Ménage",
        "description": "Nettoyage complet ou partiel du domicile",
        "icon": null,
        "displayOrder": 1,
        "subcategories": []
      },
      {
        "id": "uuid",
        "name": "Repassage",
        "description": "Repassage et pliage du linge",
        "icon": null,
        "displayOrder": 2,
        "subcategories": []
      }
    ]
  }
]
```

### 2.2 Liste plate

```
GET /categories
Auth: ❌ (public)
```

### 2.3 Sous-catégories d'un parent

```
GET /categories/{parentId}/subcategories
Auth: ❌ (public)
```

### 2.4 Détail d'une catégorie

```
GET /categories/{id}
Auth: ❌ (public)
```

---

## 3. Jobs — Cycle de vie complet

### Statuts d'un job

```
PENDING → IN_PROGRESS → DONE
```

| Statut | Signification |
|--------|---------------|
| `PENDING` | Annonce créée, en attente d'attribution |
| `IN_PROGRESS` | Un worker a été assigné, travail en cours |
| `DONE` | Travail terminé par l'une ou l'autre partie |

---

### 3.1 Créer une annonce

```
POST /jobs
Auth: ✅ Bearer JWT (KYC vérifié requis)
```

**Body :**
```json
{
  "title": "Femme de ménage 3h",
  "description": "Nettoyage appartement 50m², produits fournis",
  "location": "Paris 11e",
  "price": 35.00,
  "images": ["/uploads/jobs/img1.jpg"],
  "categoryId": "uuid-de-la-sous-categorie"
}
```

> `images` peut être une liste d'URLs (uploadées au préalable via `POST /jobs/{id}/images`).  
> `categoryId` doit être l'ID d'une **sous-catégorie** (feuille de l'arbre).

**Réponse `201 Created` :**
```json
{
  "id": "uuid",
  "title": "Femme de ménage 3h",
  "description": "Nettoyage appartement 50m², produits fournis",
  "location": "Paris 11e",
  "price": 35.00,
  "creatorId": "uuid",
  "creatorUsername": "alice",
  "workerId": null,
  "workerUsername": null,
  "status": "PENDING",
  "createdAt": "2026-05-25T14:00:00.000+00:00",
  "updatedAt": "2026-05-25T14:00:00.000+00:00",
  "images": ["/uploads/jobs/img1.jpg"],
  "categoryId": "uuid",
  "categoryName": "Ménage"
}
```

---

### 3.2 Uploader des images (après création)

```
POST /jobs/{jobId}/images
Auth: ✅ Bearer JWT (KYC vérifié, créateur du job)
Content-Type: multipart/form-data
```

**Form data :**
```
file: (fichier image)
```

**Réponse :** `JobResponse` (images mise à jour)

---

### 3.3 Supprimer une image

```
DELETE /jobs/{jobId}/images?imageUrl=/uploads/jobs/img1.jpg
Auth: ✅ Bearer JWT (KYC vérifié, créateur du job)
```

**Réponse :** `JobResponse` (images mise à jour)

---

### 3.4 Lister les annonces disponibles

```
GET /jobs/available
Auth: ❌ (public)
```

**Réponse :**
```json
[
  {
    "id": "uuid",
    "title": "Femme de ménage 3h",
    "price": 35.00,
    "creatorUsername": "alice",
    "status": "PENDING",
    ...
  }
]
```

> Retourne uniquement les jobs `PENDING`.
> **Filtrage par catégorie à venir** : `GET /jobs/available?categoryId=uuid`

---

### 3.5 Mes annonces créées

```
GET /jobs/my-created
Auth: ✅ Bearer JWT
```

Retourne tous les jobs créés par l'utilisateur connecté.

---

### 3.6 Mes missions (jobs assignés)

```
GET /jobs/my-assigned
Auth: ✅ Bearer JWT
```

Retourne tous les jobs où l'utilisateur connecté est le worker.

---

### 3.7 Détail d'un job

```
GET /jobs/{jobId}
Auth: ❌ (public)
```

---

### 3.8 Modifier une annonce

```
PUT /jobs/{jobId}
Auth: ✅ Bearer JWT (KYC vérifié, créateur du job)
```

**Body :**
```json
{
  "title": "Femme de ménage 4h",
  "description": "Nettoyage appartement 60m²",
  "price": 45.00,
  "images": ["/uploads/jobs/img1.jpg"],
  "categoryId": "uuid"
}
```

> Seuls les champs fournis sont modifiés (PATCH-like).  
> Impossible si le job n'est plus `PENDING`.

---

### 3.9 Attribuer un job à un worker

```
POST /jobs/{jobId}/assign
Auth: ✅ Bearer JWT (KYC vérifié, créateur du job)
```

**Body :**
```json
{
  "workerId": "uuid-du-worker"
}
```

| Contrainte | Détail |
|------------|--------|
| Job status | Doit être `PENDING` |
| Worker ≠ créateur | Impossible de s'assigner à soi-même |
| KYC worker | Le worker doit avoir un KYC vérifié |

**Statut après assignation :** `IN_PROGRESS`

---

### 3.10 Marquer comme terminé

```
PATCH /jobs/{jobId}/status?status=DONE
Auth: ✅ Bearer JWT (créateur OU worker du job)
```

| Qui peut le faire | Transition |
|------------------|------------|
| Créateur | `IN_PROGRESS → DONE` |
| Worker | `IN_PROGRESS → DONE` |

> Après DONE, le **worker reste lié** au job (historique + notation possible).  
> Le job n'est plus disponible pour attribution.

---

### 3.11 Supprimer une annonce

```
DELETE /jobs/{jobId}
Auth: ✅ Bearer JWT (KYC vérifié, créateur du job)
```

> Impossible si le job est `IN_PROGRESS`.

---

## 4. Messagerie

### 4.1 Démarrer une conversation

```
POST /messages/start/{jobId}
Auth: ✅ Bearer JWT (KYC vérifié)
```

**Body :**
```json
{
  "content": "Bonjour, je suis intéressé par votre annonce"
}
```

| Contrainte | Détail |
|------------|--------|
| Destinataire | Le créateur du job (ou le worker si l'envoyeur est le créateur) |
| Auto-conversation | Impossible de se démarrer une conversation avec soi-même |
| Doublon | Si une conversation existe déjà sur ce job entre ces 2 participants, le message est ajouté à la conversation existante |

**Réponse `201 Created` :**
```json
{
  "id": "uuid",
  "conversationId": "uuid",
  "senderId": "uuid",
  "senderUsername": "bob",
  "receiverId": "uuid",
  "receiverUsername": "alice",
  "jobId": "uuid",
  "content": "Bonjour, je suis intéressé par votre annonce",
  "timestamp": "2026-05-25T14:30:00.000+00:00",
  "isRead": false
}
```

---

### 4.2 Envoyer un message (dans une conversation existante)

```
POST /messages
Auth: ✅ Bearer JWT (KYC vérifié)
```

**Body :**
```json
{
  "conversationId": "uuid",
  "content": "Oui, c'est toujours disponible !"
}
```

| Contrainte | Détail |
|------------|--------|
| Participant | L'envoyeur doit être participant à la conversation |
| KYC | Vérifié |

---

### 4.3 Lister mes conversations

```
GET /messages/conversations
Auth: ✅ Bearer JWT
```

**Réponse :**
```json
[
  {
    "conversationId": "uuid",
    "jobId": "uuid",
    "jobTitle": "Femme de ménage 3h",
    "otherUserId": "uuid",
    "otherUserUsername": "bob",
    "lastMessage": "Oui, c'est toujours disponible !",
    "lastMessageTimestamp": "2026-05-25T14:35:00.000+00:00",
    "unreadCount": 2
  }
]
```

> Triée par `lastMessageTimestamp` descendant (la plus récente en premier).

---

### 4.4 Messages d'une conversation

```
GET /messages/conversation/{conversationId}
Auth: ✅ Bearer JWT (participant)
```

---

### 4.5 Marquer un message comme lu

```
PATCH /messages/{messageId}/read
Auth: ✅ Bearer JWT (destinataire du message)
```

> Le `receiver` du message peut seul le marquer comme lu.

---

### 4.6 Nombre de messages non lus

```
GET /messages/unread-count
Auth: ✅ Bearer JWT
```

**Réponse :**
```json
{
  "unreadCount": 5
}
```

---

### 4.7 WebSocket temps réel

| Info | Valeur |
|------|--------|
| Endpoint | `ws://localhost:8080/api/ws` (ou SockJS) |
| Protocole | STOMP |
| Auth | Header `Authorization: Bearer <token>` à la connexion |

**Topics à souscrire :**

| Topic | Payload | Quand |
|-------|---------|-------|
| `/topic/messages/{conversationId}` | `MessageResponse` | Nouveau message dans la conversation (si ouverte) |
| `/topic/notifications/{userId}` | `NotificationEvent` | Nouveau message dans n'importe quelle conversation (pour mettre à jour la liste des chats) |

**Exemple `NotificationEvent` :**
```json
{
  "type": "NEW_MESSAGE",
  "conversationId": "uuid",
  "jobId": "uuid",
  "jobTitle": "Femme de ménage 3h",
  "senderId": "uuid",
  "senderUsername": "bob",
  "receiverId": "uuid",
  "lastMessage": "Oui, c'est toujours disponible !",
  "lastMessageTimestamp": "2026-05-25T14:35:00.000+00:00",
  "unreadCount": 2
}
```

**Workflow frontend recommandé :**

```
1. Connexion WS établie avec JWT
2. S'abonner à /topic/notifications/{userId}
3. Charger GET /messages/conversations
4. Quand une notification arrive → mettre à jour la liste des conversations (dernier message, badge, ordre)
5. Quand l'utilisateur ouvre une conversation → s'abonner à /topic/messages/{conversationId}
6. Envoyer les messages via POST /messages (REST)
7. Le message sera automatiquement pushé sur les deux topics
```

---

## 5. Paiement

### 5.1 Initier un paiement

```
POST /payments
Auth: ✅ Bearer JWT (KYC vérifié)
```

**Body :**
```json
{
  "jobId": "uuid"
}
```

**Réponse :**
```json
{
  "id": "uuid",
  "jobId": "uuid",
  "jobTitle": "Femme de ménage 3h",
  "buyerId": "uuid",
  "buyerUsername": "alice",
  "sellerId": "uuid",
  "sellerUsername": "bob",
  "amount": 35.00,
  "commissionPercentage": 5.00,
  "commissionAmount": 1.75,
  "netAmount": 33.25,
  "status": "HELD",
  "paymentMethod": "DEMO",
  "createdAt": "2026-05-25T15:00:00.000+00:00",
  "updatedAt": "2026-05-25T15:00:00.000+00:00"
}
```

| Statut | Signification |
|--------|---------------|
| `HELD` | Montant réservé (escrow simulé) |
| `COMPLETED` | Paiement libéré |

---

### 5.2 Confirmer la livraison

```
POST /payments/confirm
Auth: ✅ Bearer JWT (KYC vérifié)
```

**Body :**
```json
{
  "transactionId": "uuid"
}
```

> Seul le **créateur du job** (buyer) peut confirmer la livraison.  
> Le statut passe à `COMPLETED`.  
> Commission de 5% déduite (simulée).

---

### 5.3 Détail d'une transaction

```
GET /payments/{transactionId}
Auth: ✅ Bearer JWT (participant)
```

---

### 5.4 Historique des paiements

```
GET /payments
Auth: ✅ Bearer JWT
```

Retourne toutes les transactions de l'utilisateur connecté (en tant que buyer).

---

## 6. KYC — Vérification d'identité

### 6.1 Uploader un document

```
POST /kyc/upload
Auth: ✅ Bearer JWT
Content-Type: multipart/form-data
```

**Form data :**
```
file: (fichier image du document, ex: passeport, carte d'identité)
documentType: "PASSPORT" (ou "ID_CARD", "DRIVER_LICENSE")
```

**Réponse `201 Created` :**
```json
{
  "id": "uuid",
  "userId": "uuid",
  "fileUrl": "/uploads/kyc/uuid.jpg",
  "documentType": "PASSPORT",
  "status": "PENDING",
  "verifiedById": null,
  "submittedAt": "2026-05-25T16:00:00.000+00:00",
  "rejectionReason": null
}
```

### 6.2 Soumettre KYC (via URL déjà uploadée)

```
POST /kyc/submit
Auth: ✅ Bearer JWT
```

**Body :**
```json
{
  "fileUrl": "/uploads/kyc/uuid.jpg",
  "documentType": "PASSPORT"
}
```

### 6.3 Voir mon statut KYC

```
GET /kyc/status
Auth: ✅ Bearer JWT
```

### 6.4 Admin : approuver un KYC

```text
POST /kyc/{documentId}/approve
Auth: ✅ Bearer JWT (rôle ADMIN)
```

### 6.5 Admin : rejeter un KYC

```text
POST /kyc/{documentId}/reject
Auth: ✅ Bearer JWT (rôle ADMIN)
```

**Body :**
```json
{
  "reason": "Document illisible"
}
```

### 6.6 Admin : liste des KYC

```
GET /kyc/all
Auth: ✅ Bearer JWT (rôle ADMIN)
```

**Filtre optionnel :**
```
GET /kyc/all?status=PENDING
GET /kyc/all?status=VERIFIED
GET /kyc/all?status=REJECTED
```

---

## 7. Notation

### 7.1 Soumettre une note

```
POST /ratings
Auth: ✅ Bearer JWT (KYC vérifié)
```

**Body :**
```json
{
  "jobId": "uuid",
  "targetUserId": "uuid",
  "score": 5,
  "comment": "Excellent travail, très professionnel"
}
```

| Règle | Détail |
|-------|--------|
| Job status | Doit être `DONE` |
| Participant | Le rater doit être créateur OU worker du job |
| Target | Doit être l'**autre** participant |
| Unicité | Une seule note par personne par job |
| Score | 1 à 5 |
| Auto-notation | Impossible |

**Réponse `201 Created` :**
```json
{
  "id": "uuid",
  "jobId": "uuid",
  "jobTitle": "Femme de ménage 3h",
  "raterId": "uuid",
  "raterUsername": "alice",
  "targetId": "uuid",
  "targetUsername": "bob",
  "targetType": "WORKER",
  "score": 5,
  "comment": "Excellent travail, très professionnel",
  "createdAt": "2026-05-25T17:00:00.000+00:00"
}
```

> `targetType` = `WORKER` (le créateur note le worker) ou `CREATOR` (le worker note le créateur)

---

### 7.2 Notes reçues par un utilisateur

```
GET /ratings/user/{userId}
Auth: ❌ (public)
```

### 7.3 Notes d'un job

```
GET /ratings/job/{jobId}
Auth: ✅ Bearer JWT (participant)
```

### 7.4 Mes notes reçues

```
GET /ratings/mine
Auth: ✅ Bearer JWT
```

---

## 8. Profil utilisateur

### 8.1 Profil public

```
GET /users/{id}
Auth: ❌ (public)
```

**Réponse :**
```json
{
  "id": "uuid",
  "firstName": "Alice",
  "lastName": "Durand",
  "username": "alice",
  "email": "alice@email.com",
  "photoProfile": null,
  "role": "USER",
  "verified": true,
  "kycStatus": "VERIFIED",
  "averageRating": 4.5,
  "totalRatings": 3
}
```

### 8.2 Profil connecté (GET /auth/me)

Voir section 1.6. Retourne les mêmes informations + `role` en enum.

---

## 9. WebSocket — Résumé

| Étape | Action |
|-------|--------|
| 1 | Connexion STOMP à `ws://localhost:8080/api/ws` avec `Authorization: Bearer <token>` |
| 2 | Souscrire à `/topic/notifications/{userId}` (reçu à chaque nouveau message) |
| 3 | Quand une notification arrive : mettre à jour la liste des conversations (dernier message, badge, ordre) |
| 4 | À l'ouverture d'un chat : souscrire à `/topic/messages/{conversationId}` |
| 5 | Quand un message arrive sur le topic : l'ajouter à la liste des messages |
| 6 | Envoyer les messages via `POST /api/messages` (REST, pas STOMP) |

---

## 10. Admin

### 10.1 Audit logs

```
GET /audit/me
Auth: ✅ Bearer JWT
```

Mes actions (authentifié).

```
GET /audit
Auth: ✅ Bearer JWT (rôle ADMIN)
```

Toutes les actions (admin uniquement).

### 10.2 Gestion KYC

Voir section 6.

### 10.3 Endpoints admin dédiés

> Un namespace `/admin/**` existe dans SecurityConfig pour les futures routes admin.

---

## 11. Erreurs

L'API utilise des exceptions métier avec un code dédié :

| Code | Signification | Statut HTTP |
|------|---------------|-------------|
| `FORBIDDEN` | L'utilisateur n'a pas les droits | `403` |
| `NOT_FOUND` | Ressource introuvable | `404` |
| `BAD_REQUEST` | Requête invalide ou règle métier violée | `400` |

**Format standard :**
```json
{
  "message": "Vous n'êtes pas autorisé",
  "errorCode": "FORBIDDEN"
}
```

Les erreurs de validation de champs (`@Valid`) retournent `400` avec les détails de Spring par défaut.

---

## 12. Tableau récapitulatif de tous les endpoints

### Auth

| Méthode | Path | Auth | Body | Retour |
|---------|------|------|------|--------|
| `POST` | `/auth/register` | Public | `{ firstName, lastName, username, email, password }` | `UserResponse` |
| `POST` | `/auth/verify-otp` | Public | `{ email, otp }` | `String` (message) |
| `POST` | `/auth/resend-otp` | Public | `?username=` | `SuccessResponse` |
| `POST` | `/auth/login` | Public | `{ username, password }` | `LoginResponse` (JWT) |
| `POST` | `/auth/refresh` | Cookie | — | `LoginResponse` |
| `GET` | `/auth/me` | JWT | — | `MeResponse` |

### Catégories

| Méthode | Path | Auth | Retour |
|---------|------|------|--------|
| `GET` | `/categories/tree` | Public | `List<CategoryTreeResponse>` |
| `GET` | `/categories` | Public | `List<CategoryResponse>` |
| `GET` | `/categories/{parentId}/subcategories` | Public | `List<CategoryResponse>` |
| `GET` | `/categories/{id}` | Public | `CategoryResponse` |

### Jobs

| Méthode | Path | Auth | Body/Params | Retour |
|---------|------|------|-------------|--------|
| `POST` | `/jobs` | JWT + KYC | `CreateJobRequest` | `JobResponse` |
| `GET` | `/jobs/available` | Public | — | `List<JobResponse>` |
| `GET` | `/jobs/my-created` | JWT | — | `List<JobResponse>` |
| `GET` | `/jobs/my-assigned` | JWT | — | `List<JobResponse>` |
| `GET` | `/jobs/{jobId}` | Public | — | `JobResponse` |
| `PUT` | `/jobs/{jobId}` | JWT + KYC | `UpdateJobRequest` | `JobResponse` |
| `POST` | `/jobs/{jobId}/assign` | JWT + KYC | `AssignJobRequest` | `JobResponse` |
| `PATCH` | `/jobs/{jobId}/status` | JWT | `?status=DONE` | `JobResponse` |
| `DELETE` | `/jobs/{jobId}` | JWT + KYC | — | `204 No Content` |
| `POST` | `/jobs/{jobId}/images` | JWT + KYC | `multipart: file` | `JobResponse` |
| `DELETE` | `/jobs/{jobId}/images` | JWT + KYC | `?imageUrl=` | `JobResponse` |

### Messagerie

| Méthode | Path | Auth | Body | Retour |
|---------|------|------|------|--------|
| `POST` | `/messages/start/{jobId}` | JWT + KYC | `{ content }` | `MessageResponse` |
| `POST` | `/messages` | JWT + KYC | `SendMessageRequest` | `MessageResponse` |
| `GET` | `/messages/conversation/{id}` | JWT | — | `List<MessageResponse>` |
| `PATCH` | `/messages/{id}/read` | JWT | — | `204 No Content` |
| `GET` | `/messages/conversations` | JWT | — | `List<ConversationResponse>` |
| `GET` | `/messages/unread-count` | JWT | — | `{ unreadCount }` |

### Paiement

| Méthode | Path | Auth | Body | Retour |
|---------|------|------|------|--------|
| `POST` | `/payments` | JWT + KYC | `{ jobId }` | `PaymentResponse` |
| `POST` | `/payments/confirm` | JWT + KYC | `{ transactionId }` | `PaymentResponse` |
| `GET` | `/payments/{id}` | JWT | — | `PaymentResponse` |
| `GET` | `/payments` | JWT | — | `List<PaymentResponse>` |

### KYC

| Méthode | Path | Auth | Body/Params | Retour |
|---------|------|------|-------------|--------|
| `POST` | `/kyc/upload` | JWT | `multipart: file, documentType` | `KycDocumentResponse` |
| `POST` | `/kyc/submit` | JWT | `KycSubmissionRequest` | `KycDocumentResponse` |
| `GET` | `/kyc/status` | JWT | — | `KycDocumentResponse` |
| `POST` | `/kyc/{id}/approve` | ADMIN | — | `KycDocumentResponse` |
| `POST` | `/kyc/{id}/reject` | ADMIN | `{ reason }` | `KycDocumentResponse` |
| `GET` | `/kyc/all` | ADMIN | `?status=` | `List<KycDocumentResponse>` |

### Notation

| Méthode | Path | Auth | Body | Retour |
|---------|------|------|------|--------|
| `POST` | `/ratings` | JWT + KYC | `RatingRequest` | `RatingResponse` |
| `GET` | `/ratings/user/{id}` | Public | — | `List<RatingResponse>` |
| `GET` | `/ratings/job/{id}` | JWT | — | `List<RatingResponse>` |
| `GET` | `/ratings/mine` | JWT | — | `List<RatingResponse>` |

### Audit

| Méthode | Path | Auth | Retour |
|---------|------|------|--------|
| `GET` | `/audit/me` | JWT | `List<ActionLog>` |
| `GET` | `/audit` | ADMIN | `List<ActionLog>` |

### Utilisateurs (CRUD basique)

| Méthode | Path | Auth | Retour |
|---------|------|------|--------|
| `POST` | `/users` | Public | `UserResponse` |
| `GET` | `/users/{id}` | Public | `UserResponse` |
| `GET` | `/users` | Public | `List<UserResponse>` |
| `PUT` | `/users/{id}` | Public | `UserResponse` |
| `DELETE` | `/users/{id}` | Public | `204 No Content` |

---

## 13. Workflow type complet (du début à la fin)

```
1. POST /auth/register               → Créer un compte
2. POST /auth/verify-otp             → Vérifier l'email
3. POST /auth/login                   → Obtenir le JWT
4. GET /categories/tree               → Charger les catégories
5. POST /jobs                         → Créer une annonce
6. POST /jobs/{id}/images             → Uploader des photos
7. POST /messages/start/{jobId}       → Démarrer une conversation
   (l'autre partie fait de même)
8. POST /messages                     → Échanger des messages
9. POST /jobs/{id}/assign             → Attribuer un worker
10. ... travail en cours ...
11. PATCH /jobs/{id}/status?status=DONE → Terminer
12. POST /payments                    → Payer (hold)
13. POST /payments/confirm            → Libérer le paiement
14. POST /ratings                     → Noter l'autre partie
```

> Les étapes 4, 7-8 sont possibles dès l'inscription.  
> Les étapes 5, 9, 12, 14 nécessitent un **KYC vérifié**.  
> L'étape 9 (assigner) nécessite aussi le KYC du worker.
