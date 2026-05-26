# Interfaces Flutter — Jobylo

## Auth
| Écran | Description | API |
|-------|------------|-----|
| **Connexion** | Email + mot de passe | POST /auth/login |
| **Inscription** | Prénom, nom, username, email, password | POST /auth/register |

## Navigation principale (BottomNav : 4 onglets)
1. **Emplois** — liste des annonces dispo
2. **Mes annonces** — mes créations + missions en cours
3. **Messages** — liste des conversations
4. **Profil** — profil + stats + KYC + déconnexion

## Emplois
| Écran | Description | API |
|-------|------------|-----|
| **Liste emplois** | Filtres (titre, localisation, prix), cards, pull-to-refresh, infinite scroll | GET /jobs/available |
| **Détail emploi** | Photos, description, prix, créateur, boutons selon contexte | GET /jobs/{id} |
| **Créer annonce** | Formulaire : titre*, description, localisation, prix*, photos (multipart) | POST /jobs |
| **Modifier annonce** | Pré-rempli avec les données existantes | PUT /jobs/{id} + POST /jobs/{id}/images |
| **Attribuer** | Sélectionner un worker pour le job | POST /jobs/{id}/assign |

## Messagerie
| Écran | Description | API |
|-------|------------|-----|
| **Conversations** | Liste avec dernier message, timestamp, badge non-lu | GET /messages/conversations |
| **Chat** | Bulles de messages, input, marquer lu, temps réel via WS | GET /messages/conversation/{id} + POST /messages + WS /topic/messages/{id} |

## Notifications
| Écran | Description | API |
|-------|------------|-----|
| **Toast notification** | Notification in-app quand le WS push sur /topic/notifications/{userId} | WS /topic/notifications/{userId} |

## Paiement
| Écran | Description | API |
|-------|------------|-----|
| **Paiement** | Récapitulatif job, montant + commission 5%, bouton "Payer" | POST /payments/initiate |
| **Confirmation livraison** | Bouton "Libérer le paiement" (créateur) après travail terminé | POST /payments/{id}/confirm |
| **Historique paiements** | Liste des transactions | GET /payments/history |

## KYC
| Écran | Description | API |
|-------|------------|-----|
| **Soumission KYC** | Upload pièce d'identité + type de document (multipart) | POST /kyc/upload |
| **Statut KYC** | Affiche le statut actuel (PENDING / VERIFIED / REJECTED) | GET /kyc/status |

## Profil
| Écran | Description | API |
|-------|------------|-----|
| **Profil public** | Photo, nom, username, note moyenne, nombre de missions | — |
| **Mon profil** | Infos personnelles, stats, bouton KYC | GET /me |

## Évaluations
| Écran | Description | API |
|-------|------------|-----|
| **Noter** | Après job DONE, noter l'autre partie (1-5 étoiles + commentaire) | POST /ratings |
| **Évaluations reçues** | Liste des reviews avec note et commentaire | GET /ratings/user/{id} |

## Admin (rôle ADMIN)
| Écran | Description | API |
|-------|------------|-----|
| **File KYC** | Liste des KYC en attente, approuver/rejeter | GET /kyc/all + POST /kyc/{id}/approve + /reject |
| **Audit logs** | Liste des actions | GET /audit |
