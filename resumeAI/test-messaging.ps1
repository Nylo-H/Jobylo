# Test Messagerie Temps Réel - Jobylo
# Nécessite : serveur:8080 + MailDev:1025 lancés

$BASE = "http://localhost:8080/api"
$USER1_EMAIL = "alice_test@mail.com"
$USER2_EMAIL = "bob_test@mail.com"
$PASSWORD = "123456"

Write-Host "╔══════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║     TEST MESSAGERIE JOBYLO          ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""

# ────────────── 1. INSCRIPTION ALICE ──────────────
Write-Host "─── 1. Inscription Alice (créatrice) ───" -ForegroundColor Green
$body = @{firstName="Alice";lastName="Test";username="alice_test";email=$USER1_EMAIL;password=$PASSWORD} | ConvertTo-Json
$resp = Invoke-RestMethod -Uri "$BASE/auth/register" -Method Post -Body $body -ContentType "application/json" -ErrorAction SilentlyContinue
if ($resp) { Write-Host "   ✅ Alice créée : $($resp.username)" } else { Write-Host "   ⚠️ Alice existe peut-être déjà" }

# ────────────── 2. INSCRIPTION BOB ──────────────
Write-Host "─── 2. Inscription Bob (worker) ───" -ForegroundColor Green
$body = @{firstName="Bob";lastName="Test";username="bob_test";email=$USER2_EMAIL;password=$PASSWORD} | ConvertTo-Json
$resp = Invoke-RestMethod -Uri "$BASE/auth/register" -Method Post -Body $body -ContentType "application/json" -ErrorAction SilentlyContinue
if ($resp) { Write-Host "   ✅ Bob créé : $($resp.username)" } else { Write-Host "   ⚠️ Bob existe peut-être déjà" }

# ────────────── 3. RÉCUPÉRER OTP VIA MAILDEV ──────────────
Write-Host "─── 3. Récupération des OTP ───" -ForegroundColor Green
Start-Sleep -Seconds 2

function Get-OtpFromMaildev($email) {
    try {
        $mails = Invoke-RestMethod -Uri "http://localhost:1080/email" -Method Get
        $mail = $mails | Where-Object { $_.to[0].address -eq $email } | Select-Object -First 1
        if ($mail) {
            $text = $mail.text
            if ($text -match '(\d{6})') { return $matches[1] }
        }
    } catch { }
    return $null
}

$otp1 = Get-OtpFromMaildev -email $USER1_EMAIL
$otp2 = Get-OtpFromMaildev -email $USER2_EMAIL

if ($otp1) { Write-Host "   ✅ OTP Alice : $otp1" } else { Write-Host "   ⚠️ OTP Alice non trouvé dans MailDev" }
if ($otp2) { Write-Host "   ✅ OTP Bob : $otp2" } else { Write-Host "   ⚠️ OTP Bob non trouvé dans MailDev" }

# ────────────── 4. VÉRIFIER OTP ──────────────
if ($otp1) {
    Write-Host "─── 4. Vérification OTP Alice ───" -ForegroundColor Green
    Invoke-RestMethod -Uri "$BASE/auth/verify-otp" -Method Post -Body (@{email=$USER1_EMAIL;otp=$otp1} | ConvertTo-Json) -ContentType "application/json" | Out-Null
    Write-Host "   ✅ Alice vérifiée"
}
if ($otp2) {
    Write-Host "─── 5. Vérification OTP Bob ───" -ForegroundColor Green
    Invoke-RestMethod -Uri "$BASE/auth/verify-otp" -Method Post -Body (@{email=$USER2_EMAIL;otp=$otp2} | ConvertTo-Json) -ContentType "application/json" | Out-Null
    Write-Host "   ✅ Bob vérifié"
}

# ────────────── 6. LOGIN ALICE ──────────────
Write-Host "─── 6. Login Alice ───" -ForegroundColor Green
$resp = Invoke-RestMethod -Uri "$BASE/auth/login" -Method Post -Body (@{email=$USER1_EMAIL;password=$PASSWORD} | ConvertTo-Json) -ContentType "application/json"
$TOKEN_ALICE = $resp.accesstoken
Write-Host "   ✅ Token Alice obtenu : $($TOKEN_ALICE.Substring(0,20))..."

# ────────────── 7. LOGIN BOB ──────────────
Write-Host "─── 7. Login Bob ───" -ForegroundColor Green
$resp = Invoke-RestMethod -Uri "$BASE/auth/login" -Method Post -Body (@{email=$USER2_EMAIL;password=$PASSWORD} | ConvertTo-Json) -ContentType "application/json"
$TOKEN_BOB = $resp.accesstoken
Write-Host "   ✅ Token Bob obtenu : $($TOKEN_BOB.Substring(0,20))..."

# ────────────── 8. ALICE CRÉE UN JOB ──────────────
Write-Host "─── 8. Alice crée un job ───" -ForegroundColor Green
$headers = @{Authorization = "Bearer $TOKEN_ALICE"}
$body = @{title="Test ménage 3h";description="Test appartement";price=35000;location="Paris 11e"} | ConvertTo-Json
$resp = Invoke-RestMethod -Uri "$BASE/jobs" -Method Post -Body $body -ContentType "application/json" -Headers $headers
$JOB_ID = $resp.id
Write-Host "   ✅ Job créé : $JOB_ID"

# ────────────── 9. BOB DÉMARRE UNE CONVERSATION ──────────────
Write-Host "─── 9. Bob démarre une conversation ───" -ForegroundColor Green
$headers = @{Authorization = "Bearer $TOKEN_BOB"}
$body = @{content="Bonjour Alice, je suis disponible pour ce job !"} | ConvertTo-Json
$resp = Invoke-RestMethod -Uri "$BASE/messages/start/$JOB_ID" -Method Post -Body $body -ContentType "application/json" -Headers $headers -ErrorAction SilentlyContinue
if ($resp) {
    $CONV_ID = $resp.conversationId
    Write-Host "   ✅ Conversation créée : $CONV_ID"
    Write-Host "   ✅ Message : $($resp.content)"
} else {
    Write-Host "   ❌ Échec démarrage conversation" -ForegroundColor Red
    exit 1
}

# ────────────── 10. BOB ENVOIE UN MESSAGE ──────────────
Write-Host "─── 10. Bob envoie un message ───" -ForegroundColor Green
$body = @{conversationId = $CONV_ID; content = "Je peux commencer demain à 9h"} | ConvertTo-Json
$resp = Invoke-RestMethod -Uri "$BASE/messages" -Method Post -Body $body -ContentType "application/json" -Headers $headers
Write-Host "   ✅ Message envoyé : $($resp.content)"
$MSG_ID = $resp.id

# ────────────── 11. ALICE LECTURE DES MESSAGES ──────────────
Write-Host "─── 11. Alice lit les messages ───" -ForegroundColor Green
$headers = @{Authorization = "Bearer $TOKEN_ALICE"}
$resp = Invoke-RestMethod -Uri "$BASE/messages/conversation/$CONV_ID?page=0&size=50" -Method Get -Headers $headers
Write-Host "   ✅ $($resp.count) messages récupérés (page 0, size 50) :"
$resp | ForEach-Object { Write-Host "      [$($_.senderUsername)] $($_.content)" }

# ────────────── 12. ALICE MARQUE LE MESSAGE COMME LU ──────────────
Write-Host "─── 12. Alice marque le message comme lu ───" -ForegroundColor Green
Invoke-RestMethod -Uri "$BASE/messages/$MSG_ID/read" -Method Patch -Headers $headers | Out-Null
Write-Host "   ✅ Message $MSG_ID marqué lu"

# ────────────── 13. VÉRIFIER UNREAD COUNT ALICE ──────────────
Write-Host "─── 13. Unread count d'Alice ───" -ForegroundColor Green
$resp = Invoke-RestMethod -Uri "$BASE/messages/unread-count" -Method Get -Headers $headers
Write-Host "   ✅ Alice a $($resp.unreadCount) messages non lus" -ForegroundColor Yellow

# ────────────── 14. VÉRIFIER UNREAD COUNT BOB ──────────────
Write-Host "─── 14. Unread count de Bob ───" -ForegroundColor Green
$headers = @{Authorization = "Bearer $TOKEN_BOB"}
$resp = Invoke-RestMethod -Uri "$BASE/messages/unread-count" -Method Get -Headers $headers
Write-Host "   ✅ Bob a $($resp.unreadCount) messages non lus" -ForegroundColor Yellow

# ────────────── 15. LISTE DES CONVERSATIONS ──────────────
Write-Host "─── 15. Conversations d'Alice ───" -ForegroundColor Green
$headers = @{Authorization = "Bearer $TOKEN_ALICE"}
$resp = Invoke-RestMethod -Uri "$BASE/messages/conversations" -Method Get -Headers $headers
Write-Host "   ✅ $($resp.Count) conversation(s) :"
$resp | ForEach-Object { Write-Host "      Avec $($_.otherUserUsername) - Dernier msg : $($_.lastMessage) - Non lus : $($_.unreadCount)" }

# ────────────── 16. MARQUER TOUTE LA CONVERSATION COMME LUE ──────────────
Write-Host "─── 16. Alice marque toute la conversation comme lue ───" -ForegroundColor Green
$resp = Invoke-RestMethod -Uri "$BASE/messages/conversation/$CONV_ID/read" -Method Patch -Headers $headers
Write-Host "   ✅ $($resp.markedRead) message(s) marqué(s) lu"

# ────────────── FIN ──────────────
Write-Host ""
Write-Host "╔══════════════════════════════════════╗" -ForegroundColor Cyan
Write-Host "║         TEST TERMINÉ ✅              ║" -ForegroundColor Cyan
Write-Host "╚══════════════════════════════════════╝" -ForegroundColor Cyan
Write-Host ""
Write-Host "Résumé :" -ForegroundColor White
Write-Host "   Job ID        : $JOB_ID"
Write-Host "   Conversation  : $CONV_ID"
Write-Host "   Message test  : $MSG_ID"
Write-Host ""
Write-Host "Pour nettoyer :" -ForegroundColor Yellow
Write-Host "   DELETE $BASE/jobs/$JOB_ID (avec token Alice)"
Write-Host "   Les users Alice/Bob peuvent être supprimés manuellement"
