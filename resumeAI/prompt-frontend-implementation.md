# Prompt Frontend Flutter — Implémentation

## Contexte

Tu dois implémenter côté Flutter les fonctionnalités suivantes en t'appuyant sur l'API REST et WebSocket décrite ci-dessous.

---

## 1. Photo de profil

### Endpoint

```
POST /auth/profile/photo
Auth: Bearer JWT
Content-Type: multipart/form-data
Body: file (MultipartFile)
```

### Réponse

```json
{
  "id": "uuid",
  "username": "alice",
  "email": "alice@email.com",
  "role": "USER",
  "verified": true,
  "kycStatus": "VERIFIED",
  "photoProfil": "/uploads/profiles/uuid.jpg",
  "averageRating": 4.5,
  "totalRatings": 3
}
```

### Comportement attendu

- L'utilisateur peut changer sa photo depuis son profil
- La photo est uploadée via multipart
- La réponse contient le `MeResponse` complet → mettre à jour le state local
- Afficher la photo via `Image.network('$baseUrl${user.photoProfil}')`

### Exemple Dio

```dart
FormData formData = FormData.fromMap({
  'file': await MultipartFile.fromFile(file.path),
});
final response = await dio.post(
  '/auth/profile/photo',
  data: formData,
);
```

---

## 2. Liste des jobs disponibles avec filtres

### Endpoint

```
GET /jobs/available?categoryId=&q=&minPrice=&maxPrice=&sort=
Auth: ❌ (public)
```

| Paramètre | Type | Exemple | Rôle |
|-----------|------|---------|------|
| `categoryId` | UUID | `"uuid"` | Filtrer par sous-catégorie |
| `q` | String | `"ménage"` | Recherche dans le titre |
| `minPrice` | BigDecimal | `10` | Prix minimum |
| `maxPrice` | BigDecimal | `100` | Prix maximum |
| `sort` | String | `"date_desc"` | Tri (date_asc, date_desc, price_asc, price_desc) |

### Réponse

```json
[
  {
    "id": "uuid",
    "title": "Femme de ménage 3h",
    "price": 35.00,
    "creatorUsername": "alice",
    "status": "PENDING",
    "categoryId": "uuid",
    "categoryName": "Ménage",
    "images": ["/uploads/jobs/img1.jpg"],
    "createdAt": "2026-05-25T14:00:00.000+00:00"
  }
]
```

### Comportement attendu

- **Écran d'accueil** : afficher les catégories (GET /categories/tree)
- **Tap sur une catégorie** → naviguer vers la liste des jobs avec `?categoryId=...` et afficher le nom de la catégorie en header
- **Barre de recherche** : taper un mot-clé → filtrer avec `?q=...` (debounce 300ms)
- **Filtre de prix** : slider ou champs min/max → `?minPrice=&maxPrice=`
- **Tri** : bouton de tri (Date ↓, Date ↑, Prix ↓, Prix ↑) → `?sort=...`
- **Pull-to-refresh** : recharger avec les mêmes filtres
- **Infinite scroll** (optionnel) : pagination à ajouter plus tard

### Exemple Dio

```dart
final response = await dio.get('/jobs/available', queryParameters: {
  if (categoryId != null) 'categoryId': categoryId,
  if (q != null) 'q': q,
  if (minPrice != null) 'minPrice': minPrice,
  if (maxPrice != null) 'maxPrice': maxPrice,
  'sort': sort ?? 'date_desc',
});
```

---

## 3. Messagerie temps réel (WebSocket + REST)

### Architecture

- **Envoyer** : `POST /api/messages` (REST)
- **Recevoir en temps réel** : WebSocket STOMP sur `ws://host/api/ws`
- **Topics** :
  - `/topic/messages/{conversationId}` → message brut (quand la conversation est ouverte)
  - `/topic/notifications/{userId}` → notification in-app (pour mettre à jour la liste)

### Initialisation WebSocket

```dart
import 'package:stomp_dart_client/stomp_dart_client.dart';

class WebSocketService {
  late StompClient client;
  final String baseUrl = 'ws://localhost:8080/api/ws';

  void connect(String token) {
    client = StompClient(
      config: StompConfig(
        url: baseUrl,
        onConnect: (frame) {
          // S'abonner aux notifications perso
          client.subscribe(
            destination: '/topic/notifications/${user.id}',
            callback: (frame) {
              final event = NotificationEvent.fromJson(jsonDecode(frame.body!));
              // → mettre à jour la liste des conversations
            },
          );
        },
        onWebSocketError: (error) => print(error),
        stompConnectHeaders: {
          'Authorization': 'Bearer $token',
        },
      ),
    );
    client.activate();
  }
}
```

### Envoyer un message

```dart
// Démarrer une conversation
final response = await dio.post('/messages/start/$jobId', data: {
  'content': 'Bonjour, je suis intéressé',
});

// Envoyer dans une conversation existante
final response = await dio.post('/messages', data: {
  'conversationId': conversationId,
  'content': 'Oui, disponible !',
});
```

### Recevoir les messages d'une conversation

```dart
// Quand l'utilisateur ouvre un chat
client.subscribe(
  destination: '/topic/messages/$conversationId',
  callback: (frame) {
    final message = MessageResponse.fromJson(jsonDecode(frame.body!));
    // → ajouter le message à la liste
  },
);
```

### Notification in-app

```dart
// Sur /topic/notifications/{userId}
// Mettre à jour :
// - La liste des conversations (déplacer en haut, update lastMessage)
// - Le badge de non-lu
// - Le badge de la BottomNav (onglet Messages)
```

### Notification push (quand l'app est en arrière-plan)

- Utiliser **Firebase Cloud Messaging**
- Le serveur backend n'envoie pas encore de push FCM → à implémenter plus tard
- Pour le MVP, seule la notification in-app via WS est active

---

## 4. Cycle de vie des jobs

### Statuts

| Statut | Affichage |
|--------|-----------|
| `PENDING` | Annonce ouverte, bouton "Postuler" pour les autres, "Attribuer" pour le créateur |
| `IN_PROGRESS` | Travail en cours, bouton "Terminer" pour les deux parties |
| `DONE` | Terminé, bouton "Noter" pour les deux parties |

### Actions par rôle

| État | Créateur | Worker |
|------|----------|--------|
| PENDING | Modifier, Supprimer, Attribuer | Contacter |
| IN_PROGRESS | Marquer DONE, Payer, Confirmer livraison | Marquer DONE |
| DONE | Noter le worker, Voir notation | Noter le créateur, Voir notation |

### Navigation recommandée

- **Onglet 1 "Emplois"** : `GET /jobs/available?...` avec filtres
- **Onglet 2 "Mes annonces"** : deux sous-onglets :
  - "Créées" → `GET /jobs/my-created`
  - "En cours" → `GET /jobs/my-assigned`
- **Détail job** : afficher statut, prix, créateur, photos, catégorie
- **Boutons contextuels** selon le statut et le rôle

---

## 5. Notation

### Endpoint

```
POST /ratings
Auth: Bearer JWT (KYC vérifié)
Body: { "jobId": "uuid", "targetUserId": "uuid", "score": 5, "comment": "..." }
```

### Règles

- Uniquement après `DONE`
- Une seule note par personne par job
- Note de 1 à 5
- Pas d'auto-notation
- Le créateur note le worker (`targetType: "WORKER"`)
- Le worker note le créateur (`targetType: "CREATOR"`)

### Affichage

```dart
// Profil public
GET /ratings/user/{userId} → List<RatingResponse>
// Afficher la moyenne : user.averageRating (étoiles)
// Afficher le nombre : user.totalRatings

// Dans le détail du job après DONE
// Vérifier si l'utilisateur a déjà noté
// Si oui : afficher "Vous avez noté X/5"
// Si non : afficher un bouton "Noter" → modal avec étoiles
```

---

## 6. Modèles Dart (Freezed)

### NotificationEvent

```dart
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
    int? unreadCount,
  }) = _NotificationEvent;

  factory NotificationEvent.fromJson(Map<String, dynamic> json) =>
      _$NotificationEventFromJson(json);
}
```

### MessageResponse

```dart
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
```

### RatingResponse

```dart
@freezed
class RatingResponse with _$RatingResponse {
  factory RatingResponse({
    required String id,
    required String jobId,
    String? jobTitle,
    required String raterId,
    String? raterUsername,
    required String targetId,
    String? targetUsername,
    String? targetType,
    required int score,
    String? comment,
    DateTime? createdAt,
  }) = _RatingResponse;

  factory RatingResponse.fromJson(Map<String, dynamic> json) =>
      _$RatingResponseFromJson(json);
}
```

---

## 7. Résumé des endpoints à implémenter côté frontend

| Méthode | Path | Module |
|---------|------|--------|
| `POST` | `/auth/profile/photo` | Profil (multipart) |
| `GET` | `/jobs/available?...` | Liste jobs (filtres) |
| `POST` | `/messages/start/{jobId}` | Chat |
| `POST` | `/messages` | Chat |
| `GET` | `/messages/conversations` | Chat |
| `GET` | `/messages/conversation/{id}` | Chat |
| `GET` | `/messages/unread-count` | Chat |
| `POST` | `/ratings` | Notation |
| `GET` | `/ratings/user/{id}` | Notation |
| `GET` | `/ratings/job/{id}` | Notation |
| `GET` | `/ratings/mine` | Notation |
| `GET` | `/categories/tree` | Catégories |
| `GET` | `/jobs/{id}` | Détail job |
| `GET` | `/users/{id}` | Profil public |
| `GET` | `/auth/me` | Profil connecté |

### Connexion WebSocket

| Topic | Usage |
|-------|-------|
| `/topic/messages/{conversationId}` | Messages en direct dans un chat ouvert |
| `/topic/notifications/{userId}` | Notifications in-app (liste des conversations) |
