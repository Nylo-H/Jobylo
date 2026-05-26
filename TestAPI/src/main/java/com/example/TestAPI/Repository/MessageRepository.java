package com.example.TestAPI.Repository;

import com.example.TestAPI.Model.Conversation;
import com.example.TestAPI.Model.Message;
import com.example.TestAPI.Model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    Page<Message> findByConversationOrderByTimestampAsc(Conversation conversation, Pageable pageable);

    List<Message> findByReceiverAndIsReadFalse(User receiver);

    long countByReceiverAndIsReadFalse(User receiver);

    long countByConversationAndReceiverAndIsReadFalse(Conversation conversation, User receiver);

    @Modifying
    @Query("UPDATE Message m SET m.isRead = true WHERE m.conversation = :conversation AND m.receiver = :receiver AND m.isRead = false")
    int markAllAsReadByConversationAndReceiver(@Param("conversation") Conversation conversation, @Param("receiver") User receiver);
}
