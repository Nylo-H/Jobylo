# Guide Messagerie Temps Réel — Frontend Flutter

## 1. Connexion WebSocket STOMP

Deux options d'endpoint (au choix) :

| Type | URL |
|------|-----|
| WebSocket natif | `ws://10.5.50.5:8080/api/ws` |
| SockJS (fallback) | `http://10.5.50.5:8080/api/ws` |

Package recommandé : [`stomp_dart_client`](https://pub.dev/packages/stomp_dart_client)

```dart
import 'package:stomp_dart_client/stomp_dart_client.dart';

StompClient stompClient = StompClient(
  config: StompConfig(
    url: 'ws://10.5.50.5:8080/api/ws',
    onConnect: onConnect,
    onWebSocketError: (error) => print(error),
    reconnectDelay: const Duration(seconds: 5),
    // --- AUTH JWT ---
    stompConnectHeaders: {
      'Authorization': 'Bearer $jwtToken',
    },
  ),
);
```

> ⚠️ Le token JWT est envoyé dans le header `Authorization` lors de la frame STOMP `CONNECT`. Sans token valide, la connexion est refusée.

---

## 2. Topics à souscrire

| Topic | Format | Payload | Usage |
|-------|--------|---------|-------|
| Nouveau message | `/topic/messages/{conversationId}` | `MessageResponse` | Afficher le message en temps réel |
| Notification | `/topic/notifications/{userId}` | `NotificationEvent` | Badge de messages non lus |
| Accusé lecture | `/topic/read/{conversationId}` | `ReadReceiptEvent` | Double check bleu ✅✅ |
| Présence | `/topic/presence` | `{ userId, online, lastSeenAt }` | Statut en ligne/dernière vue |

```dart
void onConnect(StompFrame frame) {
  // 1. Messages en temps réel
  stompClient.subscribe(
    destination: '/topic/messages/$conversationId',
    callback: (frame) {
      MessageResponse msg = MessageResponse.fromJson(jsonDecode(frame.body!));
      // → ajouter à la liste des messages
    },
  );

  // 2. Notifications + badge
  stompClient.subscribe(
    destination: '/topic/notifications/$userId',
    callback: (frame) {
      NotificationEvent event = NotificationEvent.fromJson(jsonDecode(frame.body!));
      // event.unreadCount → badge sur l'icône conversation
      // event.type == "NEW_MESSAGE" → alerte
    },
  );

  // 3. Accusés de lecture en temps réel
  stompClient.subscribe(
    destination: '/topic/read/$conversationId',
    callback: (frame) {
      ReadReceiptEvent receipt = ReadReceiptEvent.fromJson(jsonDecode(frame.body!));
      // → marquer tous les messages de l'expéditeur comme "lus"
      // → afficher ✅✅ (double check bleu)
    },
  );

  // 4. Présence online / offline
  stompClient.subscribe(
    destination: '/topic/presence',
    callback: (frame) {
      Map data = jsonDecode(frame.body!);
      String userId = data['userId'];
      bool online = data['online'];
      // → afficher le point vert/gris sur le profil
      // → stocker lastSeenAt si offline
    },
  );
}
```

---

## 3. Flux d'envoi d'un message

```
1. POST /api/messages  (REST)
   Body: { "conversationId": "...", "content": "..." }
   → réponse HTTP 201 + MessageResponse

2. Le serveur pousse le message sur /topic/messages/{conversationId}
   → TOUS les participants (expéditeur inclus) reçoivent le message en temps réel

3. Le serveur pousse une notification sur /topic/notifications/{userId}
   → met à jour le badge unreadCount pour chaque participant
```

> Le frontend doit **écouter** le topic ET gérer l'affichage immédiat (optimistic update) ou attendre le push. Recommandé : **optimistic update** → si pas de retour WS dans 3s, afficher "envoyé" en gris.

---

## 4. Marquage "lu" (Read Receipts)

### 4.1 Marquer un message spécifique

```
PATCH /api/messages/{messageId}/read
Header: Authorization: Bearer <token>
```

→ Serveur met `isRead = true` en base **et** push sur `/topic/read/{conversationId}` :

```json
{
  "conversationId": "uuid",
  "readByUserId": "uuid",
  "readByUsername": "john",
  "readAt": "2026-05-26T05:55:00.000+00:00"
}
```

### 4.2 Marquer toute la conversation comme lue

```
PATCH /api/messages/conversation/{conversationId}/read
Header: Authorization: Bearer <token>
```

→ Marque tous les messages non lus en une requête. Idéal quand l'utilisateur ouvre une conversation.

### 4.3 Comportement attendu (WhatsApp-like)

| Statut | Affichage | Déclencheur |
|--------|-----------|-------------|
| ✅ Envoyé | Un check gris | Message reçu par le serveur (réponse HTTP 201) |
| ✅✅ Delivered | Deux checks gris | (Non implémenté — nécessite ACK client) |
| ✅✅ Lu | Deux checks bleus | Push `/topic/read/` reçu |

---

## 5. Présence (Online / Offline)

La présence est **automatique** : quand l'utilisateur se connecte au WebSocket, un événement `{ online: true }` est pushé sur `/topic/presence`. À la déconnexion, `{ online: false, lastSeenAt: Date }` est pushé.

```dart
// Dans le callback de /topic/presence
Map data = jsonDecode(frame.body!);
String userId = data['userId'];
bool isOnline = data['online'];

if (isOnline) {
  // Afficher le point vert
} else {
  // Afficher "Vu il y a ..." avec lastSeenAt
  DateTime lastSeen = DateTime.parse(data['lastSeenAt']);
}
```

---

## 6. Pagination des messages

```
GET /api/messages/conversation/{conversationId}?page=0&size=50
Header: Authorization: Bearer <token>
```

Réponse Spring Page :
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

Utilisation en Flutter (infinite scroll vers le haut) :
```dart
int currentPage = 0;
bool allLoaded = false;

Future<void> loadMore() async {
  if (allLoaded) return;
  final response = await api.get('/messages/conversation/$id?page=$currentPage&size=50');
  final page = PageMessageResponse.fromJson(response);
  messages.insertAll(0, page.content);   // insérer au début (scroll up)
  allLoaded = page.last;
  currentPage++;
}
```

---

## 7. Résumé des endpoints REST

| Méthode | URL | Usage |
|---------|-----|-------|
| `POST` | `/messages/start/{jobId}` | Démarrer une conversation |
| `POST` | `/messages` | Envoyer un message |
| `GET` | `/messages/conversation/{id}?page=&size=` | Historique paginé |
| `PATCH` | `/messages/{id}/read` | Marquer un message lu |
| `PATCH` | `/messages/conversation/{id}/read` | Marquer tout lu |
| `GET` | `/messages/conversations` | Lister les conversations |
| `GET` | `/messages/unread-count` | Total non lus |

---

## 8. Architecture conseillée (Flutter)

```
┌─────────────────────────────┐
│   StompClient (singleton)   │ ← connexion unique, se reconnecte auto
├─────────────────────────────┤
│   MessageService (Provider) │ ← gère les souscriptions par conversation
├─────────────────────────────┤
│   PresenceService           │ ← écoute /topic/presence
├─────────────────────────────┤
│   NotificationService       │ ← badge global via /topic/notifications/{userId}
└─────────────────────────────┘
```

- Quand l'utilisateur **ouvre** une conversation → subscribe à `/topic/messages/{convId}` + `/topic/read/{convId}`
- Quand l'utilisateur **quitte** une conversation → unsubscribe de ces topics
- À la **fermeture de l'app** → le WebSocket se déconnecte, le serveur pousse automatiquement `{ online: false }`

---

## 9. Pièges à éviter

| Piège | Solution |
|-------|----------|
| SockJS requis en browser mais WS natif sur mobile | Utiliser `ws://` en mobile, `http://` avec SockJS en web |
| Token expiré → déconnexion WS | Intercepter l'erreur 401 → refresh token → reconnexion |
| Double réception du message (REST response + WS push) | Ignorer la réponse REST, ne traiter que le WS push |
| Pagination avec scroll vers le haut | Charger page par page en insérant au début de la liste |
