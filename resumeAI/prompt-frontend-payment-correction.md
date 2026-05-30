# Correction Workflow Paiement : C'est le Créateur qui Paie

## ❌ Avant (incorrect)

```
Worker postule → est assigné → fait le job → marque DONE
→ C'est le WORKER qui paie le créateur  ← FAUX
```

## ✅ Après (corrigé)

```
Worker postule → est assigné → fait le job → marque DONE
→ C'est le CRÉATEUR qui paie le worker  ← CORRECT
```

---

## Changements dans l'UI

### Ancien comportement (à supprimer)

Sur la page du worker après `DONE` :
```dart
// ❌ À SUPPRIMER
ElevatedButton(
  onPressed: () => _initiatePayment(jobId),
  child: Text('Payer le créateur'),
)
```

### Nouveau comportement (à implémenter)

**Côté CRÉATEUR** (vue "Mes jobs créés") :
```dart
// ✅ Si currentUser == job.creatorId && job.status == 'DONE'
if (job.creatorId == myUserId && job.status == 'DONE') {
  ElevatedButton(
    onPressed: () => _initiatePayment(job.id),
    child: Text('Payer le worker (${job.amount} XAF)'),
  );
}
```

**Côté WORKER** (vue "Mes jobs assignés") :
```dart
// ✅ Si currentUser == job.workerId && job.status == 'DONE'
// Afficher simplement "En attente du paiement"
if (job.workerId == myUserId && job.status == 'DONE') {
  Card(
    child: ListTile(
      leading: Icon(Icons.hourglass_empty, color: Colors.orange),
      title: Text('En attente du paiement'),
      subtitle: Text('Le créateur va procéder au paiement sous peu'),
    ),
  );
}
```

---

## Endpoints (inchangés, seule la logique métier a changé)

### 1. Initier le paiement (par le CRÉATEUR)
```
POST /api/payments
Authorization: Bearer <token_du_createur>

Body: { "jobId": "uuid" }

Réponse 201 : PaymentResponse { status: "HELD", ... }
```

**Conditions vérifiées maintenant :**
- ✅ KYC obligatoire
- ✅ Seul le **créateur** du job peut initier
- ✅ Le job doit avoir un **worker assigné**
- ✅ Le job doit être `DONE`
- ✅ Un seul paiement par job

### 2. Confirmer le paiement (par le CRÉATEUR)
```
POST /api/payments/confirm
Authorization: Bearer <token_du_createur>

Body: { "transactionId": "uuid" }

Réponse : PaymentResponse { status: "COMPLETED", ... }
```

**Conditions vérifiées maintenant :**
- ✅ KYC obligatoire
- ✅ Seul l'acheteur (créateur) peut confirmer
- ✅ La transaction doit être en `HELD`

---

## Logique des rôles (buyer / seller)

| Champ | Avant (faux) | Après (correct) |
|-------|--------------|-----------------|
| `buyerId` | ID du worker | ID du **créateur** |
| `buyerUsername` | worker | **créateur** |
| `sellerId` | ID du créateur | ID du **worker** |
| `sellerUsername` | créateur | **worker** |
| `netAmount` | Reçu par le créateur | Reçu par le **worker** |

---

## Impact sur l'affichage Flutter

### Récupérer les transactions du user connecté
```dart
// GET /api/payments retourne les transactions où user est buyer OU seller
final response = await http.get(
  Uri.parse('$baseUrl/payments'),
  headers: { 'Authorization': 'Bearer $token' },
);

final List<dynamic> data = jsonDecode(response.body);

// Filtrer dans le front si besoin :
for (var tx in data) {
  if (tx['buyerId'] == myUserId) {
    // Je suis le créateur → j'ai payé
    print('Payé ${tx['amount']} à ${tx['sellerUsername']}');
  } else if (tx['sellerId'] == myUserId) {
    // Je suis le worker → j'ai reçu
    print('Reçu ${tx['netAmount']} de ${tx['buyerUsername']}');
  }
}
```

### Montant affiché par rôle

```
Créateur (paiement) → Affiche "amount"    (ex: 10 000 XAF)
Worker  (réception) → Affiche "netAmount"  (ex: 9 500 XAF, après commission)
```

---

## Résumé : qui fait quoi ?

| Étape | Action | Qui ? | Route |
|-------|--------|-------|-------|
| 1 | Initier le paiement | **Créateur** | `POST /payments` |
| 2 | Confirmer le paiement | **Créateur** | `POST /payments/confirm` |
| 3 | Recevoir les fonds | **Worker** | Automatique (simulé) |
| 4 | Voir les transactions | Les deux | `GET /payments` |

---

## Checklist de correction

- [ ] Cacher le bouton "Payer" côté worker
- [ ] Ajouter le bouton "Payer le worker" côté créateur (si job DONE + pas de transaction)
- [ ] Ajouter le bouton "Confirmer le paiement" côté créateur (si transaction HELD)
- [ ] Afficher "En attente du paiement" côté worker (si job DONE + pas de transaction COMPLETED)
- [ ] Afficher "Paiement reçu : X XAF" côté worker (si transaction COMPLETED)
- [ ] Vérifier que `buyerUsername` = créateur et `sellerUsername` = worker
