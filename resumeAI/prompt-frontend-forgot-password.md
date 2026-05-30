# Guide Frontend : Mot de Passe Oublié (OTP)

## Flux utilisateur

```
1. User clique "Mot de passe oublié ?" sur la page de connexion
2. Saisit son email
3. [POST /auth/forgot-password] → email reçu avec code OTP à 6 chiffres
4. Saisit le code OTP reçu par email
5. Saisit un nouveau mot de passe (×2 confirmation)
6. [POST /auth/reset-password] → succès → redirection vers la page de connexion
```

---

## Endpoints

### 1. Demander un code de réinitialisation

```
POST /api/auth/forgot-password
Content-Type: application/json

Body:
{
  "email": "user@example.com"
}

Réponse (200):
{
  "message": "Si cet email existe, un code de réinitialisation a été envoyé"
}
```

**Important :** La réponse est **toujours la même** (email existe ou pas). C'est une protection anti-énumération d'utilisateurs.

**Rate limiting :** 1 requête toutes les 60 secondes par email. Si trop tôt :
```json
// Status 429
{
  "error": "Trop de demandes. Réessayez dans 45 secondes."
}
```

### 2. Réinitialiser le mot de passe

```
POST /api/auth/reset-password
Content-Type: application/json

Body:
{
  "email": "user@example.com",
  "otp": "123456",
  "newPassword": "NouveauMot2Passe!"
}

Réponse (200):
{
  "message": "Mot de passe réinitialisé avec succès"
}

Erreur (400):
{
  "error": "Code invalide ou expiré"
}
```

**Contraintes `newPassword` :** minimum 6 caractères (`@Size(min = 6)`).

---

## Implémentation Flutter

### Écran 1 : Saisie email

```dart
class ForgotPasswordScreen extends StatefulWidget { ... }

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  final _emailController = TextEditingController();
  bool _loading = false;
  String? _error;

  Future<void> _sendCode() async {
    setState(() { _loading = true; _error = null; });
    try {
      await http.post(
        Uri.parse('$baseUrl/auth/forgot-password'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({'email': _emailController.text.trim()}),
      );
      // Navigation vers écran suivant même si 200 (réponse identique)
      Navigator.push(context, MaterialPageRoute(
        builder: (_) => ResetPasswordScreen(email: _emailController.text.trim()),
      ));
    } on HttpException catch (e) {
      if (e.response?.statusCode == 429) {
        final body = jsonDecode(e.response!.body);
        setState(() => _error = body['error']);
      } else {
        setState(() => _error = 'Erreur réseau');
      }
    } finally {
      setState(() => _loading = false);
    }
  }
}
```

### Écran 2 : Saisie OTP + nouveau mot de passe

```dart
class ResetPasswordScreen extends StatefulWidget {
  final String email;
  const ResetPasswordScreen({required this.email});
  ...
}

class _ResetPasswordScreenState extends State<ResetPasswordScreen> {
  final _otpController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmController = TextEditingController();
  bool _loading = false;
  String? _error;

  Future<void> _resetPassword() async {
    if (_passwordController.text != _confirmController.text) {
      setState(() => _error = 'Les mots de passe ne correspondent pas');
      return;
    }
    if (_passwordController.text.length < 6) {
      setState(() => _error = 'Minimum 6 caractères');
      return;
    }

    setState(() { _loading = true; _error = null; });
    try {
      final resp = await http.post(
        Uri.parse('$baseUrl/auth/reset-password'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'email': widget.email,
          'otp': _otpController.text.trim(),
          'newPassword': _passwordController.text,
        }),
      );

      if (resp.statusCode == 200) {
        // Succès → retour page connexion avec message
        Navigator.pushAndRemoveUntil(
          context,
          MaterialPageRoute(builder: (_) => LoginScreen(
            message: 'Mot de passe réinitialisé. Connectez-vous.',
          )),
          (route) => false,
        );
      }
    } on HttpException catch (e) {
      if (e.response?.statusCode == 400) {
        final body = jsonDecode(e.response!.body);
        setState(() => _error = body['error'] ?? 'Code invalide');
      } else {
        setState(() => _error = 'Erreur réseau');
      }
    } finally {
      setState(() => _loading = false);
    }
  }
}
```

---

## Navigation UI

```
┌─────────────────────────────────┐
│    🔐 Mot de passe oublié       │
│                                 │
│  ┌───────────────────────────┐  │
│  │ Email                     │  │
│  └───────────────────────────┘  │
│                                 │
│  [  Envoyer le code  ]         │
│                                 │
│  ← Retour à la connexion       │
└─────────────────────────────────┘
         ↓ (si 200 OK)
┌─────────────────────────────────┐
│    ✉️ Code de vérification      │
│                                 │
│  ┌───────────────────────────┐  │
│  │ Code OTP (6 chiffres)     │  │
│  └───────────────────────────┘  │
│  ┌───────────────────────────┐  │
│  │ Nouveau mot de passe      │  │
│  └───────────────────────────┘  │
│  ┌───────────────────────────┐  │
│  │ Confirmer mot de passe    │  │
│  └───────────────────────────┘  │
│                                 │
│  [  Réinitialiser  ]           │
│                                 │
│  ← Retour                      │
└─────────────────────────────────┘
         ↓ (si 200 OK)
┌─────────────────────────────────┐
│    ✅ Succès !                  │
│  Mot de passe réinitialisé.     │
│  Connectez-vous.                │
│                                 │
│  [  Aller à la connexion  ]    │
└─────────────────────────────────┘
```

---

## Points de vigilance

| Point | Détail |
|-------|--------|
| **Anti-énumération** | La réponse 200 est identique email existe ou pas. Le frontend **ne doit pas** afficher "email non trouvé". |
| **Rate limiting** | Gérer le 429 : afficher le message d'erreur avec le temps restant. |
| **OTP à 6 chiffres** | Utiliser un `TextField` avec `keyboardType: TextInputType.number`, `maxLength: 6`. |
| **Expiry OTP** | 5 minutes. Si expiré, l'utilisateur doit refaire un `forgot-password`. |
| **Validation front** | Vérifier que `newPassword` = `confirmPassword` avant d'envoyer. |
| **Sécurité** | Ne **jamais** logguer le code OTP ou le nouveau mot de passe côté frontend. |
| **Loader** | Désactiver le bouton pendant la requête pour éviter les doubles envois. |

---

## Code OTP envoyé par email

```
Objet : Votre code OTP
Corps : Votre code OTP est : 123456
```

Le frontend n'affiche pas l'email — l'utilisateur consulte sa boîte mail (MailDev en dev : http://localhost:8025).
