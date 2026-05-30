# Prompt Frontend Flutter — Implémentation Complète

## Contexte

Backend Spring Boot déployé sur `http://10.5.50.5:8080/api/` (context-path `/api`).

## 1. Authentification & OTP

### 1.1 Inscription

```
POST /auth/register
Body: { "firstName": "...", "lastName": "...", "username": "...", "email": "...", "password": "..." }
Réponse: UserResponse (ne contient pas de token)
```

L'OTP est envoyé par email (ou retourné dans la réponse en dev si MailDev est down).

### 1.2 Login — gère le cas non vérifié

```
POST /auth/login
Body: { "email": "...", "password": "..." }
Réponse:
{
  "accesstoken": "jwt...",
  "refreshtoken": "uuid...",
  "verified": false   // ← clé pour le routing
}
```

**Comportement attendu** :
- `verified: true` → rediriger vers l'accueil
- `verified: false` → rediriger vers la page OTP

Le JWT est délivré même si non vérifié (permet d'accéder à la page OTP et aux endpoints non-KYC).

### 1.3 Vérification OTP (auto-login)

```
POST /auth/verify-otp
Body: { "email": "...", "otp": "123456" }
Réponse: LoginResponse { accesstoken, refreshtoken, verified: true }
```

→ Si OTP valide, l'utilisateur est marqué verified **et** reçoit un JWT directement (pas besoin de relogin).

### 1.4 Renvoyer OTP

```
POST /auth/resend-otp?email=...
```

### 1.5 Refresh token

```
POST /auth/refresh
Cookie: refreshToken=...
Réponse: LoginResponse { accesstoken, refreshtoken, verified }
```

## 2. Navigation par statut verified

```
LoginResponse.verified
├── true  → HomeScreen (accès complet)
└── false → OtpScreen (saisie code)
              ├── OTP valide → LoginResponse(verified=true) → HomeScreen
              └── "Renvoyer" → POST /auth/resend-otp
```

L'utilisateur peut aussi accéder à son profil (`GET /auth/me`) et voir `verified` / `kycStatus`.

## 3. Connexion WebSocket STOMP

### Initialisation

```dart
StompClient stompClient = StompClient(
  config: StompConfig(
    url: 'ws://10.5.50.5:8080/api/ws',
    onConnect: onConnect,
    onWebSocketError: print,
    reconnectDelay: Duration(seconds: 5),
    stompConnectHeaders: {
      'Authorization': 'Bearer $jwtToken',
    },
  ),
);
```

### Topics à souscrire

| Topic | Payload | Usage |
|-------|---------|-------|
| `/topic/messages/{conversationId}` | `MessageResponse` | Message temps réel |
| `/topic/notifications/{userId}` | `NotificationEvent` | Badge + notif in-app |
| `/topic/read/{conversationId}` | `ReadReceiptEvent` | ✅✅ double check bleu |
| `/topic/presence` | `{ userId, online, lastSeenAt }` | Point vert / dernier vu |

### Gestion des souscriptions par conversation

```dart
// Ouvrir un chat
void openConversation(String convId) {
  stompClient.subscribe(
    destination: '/topic/messages/$convId',
    callback: (frame) => messagesNotifier.add(MessageResponse.fromJson(jsonDecode(frame.body!))),
  );
  stompClient.subscribe(
    destination: '/topic/read/$convId',
    callback: (frame) {
      final receipt = ReadReceiptEvent.fromJson(jsonDecode(frame.body!));
      // receipt.readByUserId → marquer "lu" les messages envoyés à cet user
      // Afficher ✅✅ (double check bleu) sur les messages concernés
    },
  );
}

// Quitter un chat
void closeConversation(String convId) {
  stompClient.unsubscribe(destination: '/topic/messages/$convId');
  stompClient.unsubscribe(destination: '/topic/read/$convId');
}
```

### Gestion des notifications (pour la liste + badge)

```dart
// À souscrire une seule fois après login
stompClient.subscribe(
  destination: '/topic/notifications/$myUserId',
  callback: (frame) {
    final event = NotificationEvent.fromJson(jsonDecode(frame.body!));
    // event.type == "NEW_MESSAGE"
    // event.unreadCount → badge sur la conversation
    // Déplacer la conversation en haut de la liste
  },
);
```

### Gestion de la présence

```dart
// À souscrire une seule fois
stompClient.subscribe(
  destination: '/topic/presence',
  callback: (frame) {
    final data = jsonDecode(frame.body!);
    // data['userId'], data['online'], data['lastSeenAt']
    // → maj du point vert sur le profil dans les chats
  },
);
```

## 4. Flux d'envoi d'un message

```
1. POST /api/messages
   Body: { "conversationId": "...", "content": "..." }
   → 201 + MessageResponse

2. Le serveur push sur /topic/messages/{conversationId}
   → TOUS les participants recoivent le message

3. Le frontend :
   - Option A (simple) : ignorer la réponse REST, écouter uniquement le WS
   - Option B (optimistic) : afficher immédiatement, puis remplacer par le WS
```

Recommandé : **Option A** — le push WS arrive quasi instantanément.

## 5. Marquage "lu" (Read Receipts)

### Marquer un seul message

```
PATCH /api/messages/{messageId}/read
→ 204

Le serveur push sur /topic/read/{conversationId} :
{
  "conversationId": "uuid",
  "readByUserId": "uuid",
  "readByUsername": "john",
  "readAt": "2026-05-26T06:00:00.000Z"
}
```

### Marquer toute la conversation comme lue (à faire quand on ouvre un chat)

```
PATCH /api/messages/conversation/{conversationId}/read
→ { "markedRead": 5 }
```

Puis le serveur push aussi le `ReadReceiptEvent` sur `/topic/read/{convId}`.

### Statuts d'affichage des messages

| Statut | Icône | Déclencheur |
|--------|-------|-------------|
| Envoyé | ✅ gris | Message envoyé au serveur |
| Lu | ✅✅ bleus | Push `/topic/read/` reçu |

## 6. Pagination des messages

```
GET /api/messages/conversation/{id}?page=0&size=50
Header: Authorization: Bearer <token>
```

Réponse :
```json
{
  "content": [ MessageResponse, ... ],
  "totalElements": 150,
  "totalPages": 3,
  "number": 0,
  "size": 50,
  "last": false
}
```

**Infinite scroll vers le haut** (charger l'historique au scroll) :
```dart
int currentPage = 0;
bool allLoaded = false;

Future<void> loadMore() async {
  if (allLoaded) return;
  final response = await dio.get('/messages/conversation/$convId', queryParameters: {
    'page': currentPage,
    'size': 50,
  });
  final page = PageMessageResponse.fromJson(response.data);
  messages.insertAll(0, page.content);  // insérer en haut
  allLoaded = page.last;
  currentPage++;
}
```

## 7. Modèles Dart

```dart
// ---------- Login ----------
@freezed
class LoginResponse with _$LoginResponse {
  factory LoginResponse({
    required String accesstoken,
    required String refreshtoken,
    required bool verified,
  }) = _LoginResponse;
  factory LoginResponse.fromJson(Map<String, dynamic> json) =>
      _$LoginResponseFromJson(json);
}

// ---------- Message ----------
@freezed
class MessageResponse with _$MessageResponse {
  factory MessageResponse({
    required String id,
    required String conversationId,
    required String senderId,
    required String senderUsername,
    required String receiverId,
    String? receiverUsername,
    required String jobId,
    required String content,
    DateTime? timestamp,
    required bool isRead,
  }) = _MessageResponse;
  factory MessageResponse.fromJson(Map<String, dynamic> json) =>
      _$MessageResponseFromJson(json);
}

// ---------- Notification ----------
@freezed
class NotificationEvent with _$NotificationEvent {
  factory NotificationEvent({
    required String type,
    required String conversationId,
    required String jobId,
    String? jobTitle,
    required String senderId,
    String? senderUsername,
    required String receiverId,
    String? lastMessage,
    DateTime? lastMessageTimestamp,
    required int unreadCount,
  }) = _NotificationEvent;
  factory NotificationEvent.fromJson(Map<String, dynamic> json) =>
      _$NotificationEventFromJson(json);
}

// ---------- Read Receipt ----------
@freezed
class ReadReceiptEvent with _$ReadReceiptEvent {
  factory ReadReceiptEvent({
    required String conversationId,
    required String readByUserId,
    String? readByUsername,
    DateTime? readAt,
  }) = _ReadReceiptEvent;
  factory ReadReceiptEvent.fromJson(Map<String, dynamic> json) =>
      _$ReadReceiptEventFromJson(json);
}

// ---------- Conversation ----------
@freezed
class ConversationResponse with _$ConversationResponse {
  factory ConversationResponse({
    required String conversationId,
    required String jobId,
    String? jobTitle,
    required String otherUserId,
    String? otherUserUsername,
    String? lastMessage,
    DateTime? lastMessageTimestamp,
    required int unreadCount,
  }) = _ConversationResponse;
  factory ConversationResponse.fromJson(Map<String, dynamic> json) =>
      _$ConversationResponseFromJson(json);
}
```

## 8. Résumé des endpoints REST

| Méthode | Path | Auth | Usage |
|---------|------|------|-------|
| `POST` | `/auth/register` | ❌ | Inscription |
| `POST` | `/auth/login` | ❌ | Login → `verified` dans réponse |
| `POST` | `/auth/verify-otp` | ❌ | Valider OTP → auto-login |
| `POST` | `/auth/resend-otp` | ❌ | Renvoyer OTP |
| `POST` | `/auth/refresh` | Cookie | Refresh token |
| `GET` | `/auth/me` | JWT | Profil connecté |
| `POST` | `/auth/profile/photo` | JWT | Upload photo |
| `GET` | `/jobs/available?categoryId=&q=&minPrice=&maxPrice=&sort=` | ❌ | Filtres + tri |
| `GET` | `/jobs/{id}` | ❌ | Détail job |
| `GET` | `/categories/tree` | ❌ | Arbre des catégories |
| `GET` | `/messages/conversations` | JWT | Liste conversations + unread |
| `GET` | `/messages/conversation/{id}?page=0&size=50` | JWT | Messages paginés |
| `POST` | `/messages/start/{jobId}` | JWT | Démarrer conversation |
| `POST` | `/messages` | JWT | Envoyer message |
| `PATCH` | `/messages/{id}/read` | JWT | Marquer un message lu |
| `PATCH` | `/messages/conversation/{id}/read` | JWT | Marquer tout lu |
| `GET` | `/messages/unread-count` | JWT | Total non lus |
| `POST` | `/ratings` | JWT | Noter (score 1-5) |
| `GET` | `/ratings/user/{id}` | JWT | Notes reçues |

## 9. Résumé des topics WebSocket

| Topic | Direction | Quand souscrire |
|-------|-----------|-----------------|
| `/topic/messages/{convId}` | Server → Client | Quand le chat est ouvert |
| `/topic/read/{convId}` | Server → Client | Quand le chat est ouvert |
| `/topic/notifications/{userId}` | Server → Client | Une fois après login |
| `/topic/presence` | Server → Client | Une fois après login |

## 10. Architecture recommandée (services)

```
lib/
├── services/
│   ├── auth_service.dart         // login, register, verifyOtp, refresh
│   ├── stomp_service.dart        // connexion WS, subscribe, unsubscribe, déconnexion auto
│   ├── message_service.dart      // CRUD messages, markAsRead, pagination
│   ├── presence_service.dart     // écoute /topic/presence → expose onlineUsers
│   └── notification_service.dart // écoute /topic/notifications → expose badge count
├── models/                       // Freezed
├── providers/                    // Riverpod / BLoC
└── screens/
    ├── login_screen.dart
    ├── otp_screen.dart           // Saisie code OTP
    ├── home_screen.dart          // TabBar : Emplois | Messages | Profil
    ├── chat_list_screen.dart     // Liste conversations + badge
    ├── chat_screen.dart          // Messages temps réel + ✅✅
    └── profile_screen.dart
```

## 11. Pièges à éviter

| Piège | Solution |
|-------|----------|
| SockJS en web, WS natif en mobile | Utiliser `ws://` en mobile, SockJS `http://` en web |
| Token expiré → déconnexion WS | Intercepter 401 → refresh token → reconnecter WS |
| Double réception message (REST + WS) | Ignorer la réponse REST, ne traiter que le WS |
| Scroll up pour pagination | Insérer les messages au début de la liste, pas à la fin |
| OTP expiré après 5 min | Suggérer "Renvoyer" avec un timer côté UI |
