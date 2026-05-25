package com.example.TestAPI.Repository;

import com.example.TestAPI.Model.Conversation;
import com.example.TestAPI.Model.JobOffer;
import com.example.TestAPI.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConversationRepository extends JpaRepository<Conversation, UUID> {

    Optional<Conversation> findByJobAndParticipant1AndParticipant2(JobOffer job, User p1, User p2);

    @Query("SELECT c FROM Conversation c LEFT JOIN FETCH c.job WHERE c.participant1 = :user OR c.participant2 = :user ORDER BY c.lastMessageAt DESC")
    List<Conversation> findByParticipantOrderByLastMessageAtDesc(@Param("user") User user);
}
