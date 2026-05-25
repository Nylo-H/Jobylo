-- Migration: Message.conversationId (UUID) → Conversation entity
-- Exécuter AVANT le redémarrage de l'app avec ddl-auto=update
-- (ou désactiver ddl-auto, exécuter ce script, puis réactiver)

BEGIN;

-- 1. Créer la table conversations
CREATE TABLE conversations (
    id UUID NOT NULL PRIMARY KEY,
    job_id UUID NOT NULL,
    participant1_id UUID NOT NULL,
    participant2_id UUID NOT NULL,
    created_at TIMESTAMP,
    last_message_at TIMESTAMP,
    last_message_content VARCHAR(500),
    CONSTRAINT fk_conv_job FOREIGN KEY (job_id) REFERENCES job_offers(id),
    CONSTRAINT fk_conv_p1 FOREIGN KEY (participant1_id) REFERENCES users(id),
    CONSTRAINT fk_conv_p2 FOREIGN KEY (participant2_id) REFERENCES users(id),
    CONSTRAINT uk_job_p1_p2 UNIQUE (job_id, participant1_id, participant2_id)
);

-- 2. Créer une Conversation pour chaque groupe de messages ayant le même conversationId
INSERT INTO conversations (id, job_id, participant1_id, participant2_id, created_at, last_message_at, last_message_content)
SELECT
    gen_random_uuid() AS id,
    sub.job_id,
    sub.p1,
    sub.p2,
    sub.first_ts,
    sub.last_ts,
    sub.last_content
FROM (
    SELECT DISTINCT ON (m.conversation_id)
        m.job_id,
        LEAST(m.sender_id, m.receiver_id) AS p1,
        GREATEST(m.sender_id, m.receiver_id) AS p2,
        first_value(m.timestamp) OVER w AS first_ts,
        last_value(m.timestamp) OVER w AS last_ts,
        last_value(m.content) OVER w AS last_content
    FROM messages m
    WHERE m.conversation_id IS NOT NULL
    WINDOW w AS (PARTITION BY m.conversation_id ORDER BY m.timestamp RANGE BETWEEN UNBOUNDED PRECEDING AND UNBOUNDED FOLLOWING)
) sub;

-- 3. Ajouter la colonne conversation_id (FK) dans messages
ALTER TABLE messages ADD COLUMN new_conversation_id UUID;

-- 4. Remplir new_conversation_id à partir des conversations créées
UPDATE messages m
SET new_conversation_id = c.id
FROM conversations c
WHERE m.job_id = c.job_id
  AND LEAST(m.sender_id, m.receiver_id) = c.participant1_id
  AND GREATEST(m.sender_id, m.receiver_id) = c.participant2_id;

-- 5. Ajouter la contrainte FK et NOT NULL
ALTER TABLE messages ALTER COLUMN new_conversation_id SET NOT NULL;
ALTER TABLE messages ADD CONSTRAINT fk_msg_conv FOREIGN KEY (new_conversation_id) REFERENCES conversations(id);

-- 6. Supprimer l'ancienne colonne conversationId (UUID)
ALTER TABLE messages DROP COLUMN conversation_id;

-- 7. Renommer new_conversation_id en conversation_id
ALTER TABLE messages RENAME COLUMN new_conversation_id TO conversation_id;

COMMIT;
