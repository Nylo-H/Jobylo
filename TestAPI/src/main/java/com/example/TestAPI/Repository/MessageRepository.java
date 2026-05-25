package com.example.TestAPI.Repository;

import com.example.TestAPI.Model.Conversation;
import com.example.TestAPI.Model.Message;
import com.example.TestAPI.Model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MessageRepository extends JpaRepository<Message, UUID> {

    List<Message> findByConversationOrderByTimestampAsc(Conversation conversation);

    List<Message> findByReceiverAndIsReadFalse(User receiver);

    long countByReceiverAndIsReadFalse(User receiver);

    long countByConversationAndReceiverAndIsReadFalse(Conversation conversation, User receiver);
}
