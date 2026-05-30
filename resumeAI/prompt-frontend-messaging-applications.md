# Guide Frontend : Messagerie Temps Réel & Candidatures

## 1. Connexion WebSocket STOMP

### Endpoint
```
ws://localhost:8080/api/ws
```
Avec fallback SockJS : `http://localhost:8080/api/ws`

### Authentification
Header dans la connexion STOMP :
```
Authorization: Bearer <access_token>
```

### Exemple JavaScript (STOMP.js)
```js
const client = new StompJs.Client({
  brokerURL: 'ws://localhost:8080/api/ws',
  connectHeaders: {
    Authorization: 'Bearer ' + token
  }
});
// ou avec SockJS :
// const client = new StompJs.Client({
//   webSocketFactory: () => new SockJS('http://localhost:8080/api/ws'),
//   connectHeaders: { Authorization: 'Bearer ' + token }
// });
```

---

## 2. Topics WebSocket à Souscrire

### 2.1 Messages temps réel — `/topic/messages/{conversationId}`
```js
client.subscribe('/topic/messages/' + convId, (msg) => {
  const data = JSON.parse(msg.body);
  // data = MessageResponse { id, conversationId, senderId, senderUsername,
  //   receiverId, receiverUsername, jobId, content, timestamp, isRead }
});
```
Poussé quand :
- Un message est envoyé (nouveau ou réponse)
- Une conversation est démarrée (premier message)

### 2.2 Read Receipts (✅✅) — `/topic/read/{conversationId}`
```js
client.subscribe('/topic/read/' + convId, (msg) => {
  const data = JSON.parse(msg.body);
  // data = ReadReceiptEvent { conversationId, readByUserId, readByUsername, readAt }
});
```
Poussé quand le destinataire marque un ou plusieurs messages comme lus.

### 2.3 Notifications utilisateur — `/topic/notifications/{userId}`
```js
client.subscribe('/topic/notifications/' + myUserId, (msg) => {
  const data = JSON.parse(msg.body);
  // Types possibles :
  //   NEW_MESSAGE        → NotificationEvent { type, conversationId, jobId, jobTitle,
  //                          senderId, senderUsername, receiverId, lastMessage,
  //                          lastMessageTimestamp, unreadCount }
  //   NEW_APPLICATION    → { type, jobId, jobTitle, applicantId, applicantUsername, coverLetter }
  //   APPLICATION_REJECTED → { type, jobId, jobTitle, message }
});
```

### 2.4 Présence online/offline — `/topic/presence`
```js
client.subscribe('/topic/presence', (msg) => {
  const data = JSON.parse(msg.body);
  // data = { userId: string, online: boolean, lastSeenAt: string|null }
});
```
Poussé automatiquement à la connexion/déconnexion WebSocket.

---

## 3. Routes REST — Messages

### Démarrer une conversation (premier message)
```
POST /api/messages/start/{jobId}
Body: { "content": "Bonjour, je suis intéressé" }
Réponse: MessageResponse (201 Created)
```
- KYC obligatoire
- Impossible de se démarrer une conversation à soi-même
- Si la conversation existe déjà, le message est ajouté à la conversation existante

### Envoyer un message
```
POST /api/messages
Body: { "content": "Texte du message", "conversationId": "uuid" }
Réponse: MessageResponse (201 Created)
```
- KYC obligatoire
- `conversationId` récupéré depuis le `start` ou la liste des conversations

### Récupérer les messages d'une conversation (paginated)
```
GET /api/messages/conversation/{conversationId}?page=0&size=50
Réponse: Page<MessageResponse> {
  content: [MessageResponse...],
  totalElements, totalPages, number, size...
}
```

### Marquer un message comme lu
```
PATCH /api/messages/{messageId}/read
Réponse: 204 No Content
```
Pousse un `ReadReceiptEvent` sur `/topic/read/{conversationId}`.
Seul le destinataire peut marquer.

### Marquer TOUS les messages d'une conversation comme lus (batch)
```
PATCH /api/messages/conversation/{conversationId}/read
Réponse: { "markedRead": 5 }
```
Pousse un seul `ReadReceiptEvent` si au moins 1 message a été marqué.
**✅ À utiliser de préférence** : un seul appel au lieu d'un par message.

### Récupérer la liste des conversations
```
GET /api/messages/conversations
Réponse: [ConversationResponse...]
// Chaque élément : {
//   conversationId, jobId, jobTitle, otherUserId, otherUserUsername,
//   lastMessage, lastMessageTimestamp, unreadCount
// }
```

### Compter les messages non lus (total)
```
GET /api/messages/unread-count
Réponse: { "unreadCount": 3 }
```

---

## 4. Routes REST — Candidatures (Applications)

### Postuler à un job
```
POST /api/jobs/{jobId}/apply
Body (optionnel): { "coverLetter": "Lettre de motivation..." }
Réponse: ApplicationResponse (201 Created)
```
- KYC obligatoire
- Impossible de postuler à son propre job
- Impossible de postuler deux fois
- Impossible si job status != PENDING
- Pousse `NEW_APPLICATION` au créateur du job via WS

### Voir les candidats (créateur du job)
```
GET /api/jobs/{jobId}/applicants
Réponse: [ApplicationResponse...]
```
- Seul le créateur du job peut voir
- Trié par date de création décroissante

### Rejeter un candidat (créateur du job)
```
POST /api/jobs/{jobId}/reject/{workerId}
Réponse: 204 No Content
```
- Seul le créateur peut rejeter
- Impossible si job déjà attribué (status != PENDING)
- Pousse `APPLICATION_REJECTED` au worker via WS

### Voir mes candidatures (worker)
```
GET /api/applications/mine
Réponse: [ApplicationResponse...]
```

### Compter les candidats pour un job (public ? accessible aux participants)
```
GET /api/jobs/{jobId}/applicants/count
Réponse: { "count": 5 }
```
`count` = nombre de candidats PENDING (en attente).

### Assigner un worker (créateur du job)
```
POST /api/jobs/{jobId}/assign
Body: { "workerId": "uuid" }
Réponse: JobResponse (status devient IN_PROGRESS)
```
- KYC obligatoire (créateur + worker)
- Le worker élu voit son application → `ACCEPTED`
- **Tous les autres PENDING sont automatiquement → `REJECTED`**
- Chaque worker rejeté reçoit un push `APPLICATION_REJECTED` via WS

### ApplicationResponse — format complet
```json
{
  "id": "uuid",
  "jobId": "uuid",
  "jobTitle": "string",
  "workerId": "uuid",
  "workerUsername": "string",
  "coverLetter": "string|null",
  "status": "PENDING|ACCEPTED|REJECTED|CANCELLED",
  "createdAt": "2026-05-30T12:00:00.000+00:00"
}
```

---

## 5. Statuts des Jobs (JobStatus)

| Status | Signification | Déclenché par |
|--------|--------------|---------------|
| `PENDING` | En attente d'assignation | Création du job |
| `IN_PROGRESS` | Assigné, en cours | `POST /jobs/{id}/assign` |
| `DONE` | Terminé | `PATCH /jobs/{id}/status?status=DONE` (créateur ou worker) |
| `CANCELLED` | Annulé | `PATCH /jobs/{id}/status?status=CANCELLED` (créateur uniquement) |

Changement de statut :
```
PATCH /api/jobs/{jobId}/status?status=IN_PROGRESS|DONE|CANCELLED
Réponse: JobResponse
```

---

## 6. Workflow Complet (checklist frontend)

### Phase 1 : 🔌 Connexion WebSocket
- [ ] Connecter STOMP avec token JWT
- [ ] Souscrire à `/topic/notifications/{userId}`
- [ ] Souscrire à `/topic/presence`
- [ ] Afficher le statut en ligne des utilisateurs dans l'UI

### Phase 2 : 💬 Messagerie
- [ ] Charger la liste des conversations : `GET /conversations`
- [ ] Afficher le badge `unreadCount` par conversation
- [ ] Pour ouvrir une conversation :
  - [ ] Souscrire à `/topic/messages/{conversationId}`
  - [ ] Souscrire à `/topic/read/{conversationId}`
  - [ ] Charger l'historique : `GET /conversation/{id}?page=0&size=50`
- [ ] Nouveau message : `POST /messages` → reçu via WS dans `/topic/messages/{id}`
- [ ] Marquer comme lu : `PATCH /conversation/{id}/read` (batch)
- [ ] Mettre à jour `unreadCount` quand `NEW_MESSAGE` reçu pour d'autres conversations
- [ ] Afficher ✅✅ quand `ReadReceiptEvent` reçu

### Phase 3 : 📋 Candidatures (côté worker)
- [ ] Bouton "Postuler" sur la fiche du job (si non déjà postulé)
- [ ] `POST /jobs/{jobId}/apply` avec `{ "coverLetter": "..." }`
- [ ] Afficher mes candidatures : `GET /applications/mine`
- [ ] Notification en temps réel si mon application est rejetée

### Phase 4 : 👥 Gestion des candidatures (côté créateur)
- [ ] Voir les candidats : `GET /jobs/{jobId}/applicants`
- [ ] Badge du nombre de candidats : `GET /jobs/{jobId}/applicants/count`
- [ ] Bouton "Accepter" → `POST /jobs/{jobId}/assign { "workerId": "..." }`
- [ ] Bouton "Refuser" → `POST /jobs/{jobId}/reject/{workerId}`
- [ ] Notification temps réel quand quelqu'un postule

### Phase 5 : 🔄 Cycle de vie du job
- [ ] Afficher le statut du job (badge coloré)
- [ ] Créateur/Worker peut marquer `DONE` : `PATCH /jobs/{id}/status?status=DONE`
- [ ] Créateur peut annuler : `PATCH /jobs/{id}/status?status=CANCELLED`

---

## 7. Diagramme de Navigation (UI Suggérée)

```
┌──────────────────────────────────────────┐
│  Liste des Conversations (sidebar)       │
│  ┌────────────────────────────────────┐  │
│  │ Bob_test  "Je suis dispo"  [2] 🔴 │  │  ← unread badge
│  │ Alice     "Merci !"         [0]   │  │
│  │ Jean      "Bonjour"         [1] 🔴 │  │
│  └────────────────────────────────────┘  │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │ 💬 Conversation avec bob_test     │  │
│  │                                    │  │
│  │ 10:30  Moi: Bonjour               │  │
│  │ 10:31  Bob: Salut !          ✅✅ │  │  ← read receipt
│  │ 10:32  Bob: Je suis dispo         │  │
│  │                                    │  │
│  │ ┌──────────────────────────────┐  │  │
│  │ │ Tapez votre message... [➤]   │  │  │
│  │ └──────────────────────────────┘  │  │
│  └────────────────────────────────────┘  │
│                                          │
│  🟢 Bob est en ligne  (via présence)     │
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  📋 Candidatures pour "Job XYZ"          │
│  (créateur view)                         │
│                                          │
│  ┌────────────────────────────────────┐  │
│  │ 🟡 Jean_Dupont  "Motivé !"  [✅][❌]│  │
│  │ 🟡 Marie_L      "Dispo demain"[✅][❌]│  │
│  │ 🔴 Paul (refusé)                    │  │
│  └────────────────────────────────────┘  │
│                                          │
│  [Voir mes candidatures postulées]       │
└──────────────────────────────────────────┘
```

---

## 8. Comportements Temps Réel (Vérification)

| Action | WS Push | Topic |
|--------|---------|-------|
| Alice envoie un message | `MessageResponse` | `/topic/messages/{convId}` des 2 participants |
| Alice envoie un message | `NotificationEvent(NEW_MESSAGE)` | `/topic/notifications/{receiverId}` |
| Bob marque comme lu | `ReadReceiptEvent` | `/topic/read/{convId}` des 2 participants |
| Bob se connecte | `{ userId, online: true }` | `/topic/presence` |
| Bob se déconnecte | `{ userId, online: false, lastSeenAt }` | `/topic/presence` |
| Bob postule | `NotificationEvent(NEW_APPLICATION)` | `/topic/notifications/{creatorId}` |
| Alice rejette Bob | `NotificationEvent(APPLICATION_REJECTED)` | `/topic/notifications/{workerId}` |
| Alice assigne un worker | `APPLICATION_REJECTED` (x N-1 workers) | `/topic/notifications/{eachWorkerId}` |

---

## 9. Dépannage / Points d'Attention

- **401 Unauthorized** : token expiré → refresh token, reconnecter WS
- **403 Forbidden** : KYC non vérifié → rediriger vers page de vérification KYC
- **409 Conflict** : déjà postulé ou conversation déjà existante (cas normal)
- **WebSocket déconnecté** : réessayer avec `client.activate()` ; les messages REST fonctionnent encore
- **Souscrire aux topics après chaque reconnexion** : les souscriptions ne survivent pas à une déconnexion
- **Pagination** : le backend renvoie `Page<MessageResponse>` (Spring Data). Utiliser `content` pour les messages, `totalPages` pour savoir s'il faut charger plus.
- **Ordre** : les messages sont triés par `timestamp ASC` dans la réponse paginée.
