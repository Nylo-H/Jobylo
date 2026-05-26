package com.example.TestAPI.Service.Presence;

import com.example.TestAPI.Model.User;
import com.example.TestAPI.Repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PresenceService {

    private final SimpMessagingTemplate messagingTemplate;
    private final UserRepository userRepository;

    private final Map<UUID, Boolean> onlineUsers = new ConcurrentHashMap<>();

    public void userConnected(User user) {
        UUID userId = user.getId();
        onlineUsers.put(userId, true);
        broadcastPresence(userId, true);
    }

    public void userDisconnected(User user) {
        UUID userId = user.getId();
        onlineUsers.remove(userId);
        user.setLastSeenAt(new Date());
        userRepository.save(user);
        broadcastPresence(userId, false);
    }

    public boolean isOnline(UUID userId) {
        return onlineUsers.getOrDefault(userId, false);
    }

    public Set<UUID> getAllOnline() {
        return onlineUsers.keySet();
    }

    private void broadcastPresence(UUID userId, boolean online) {
        messagingTemplate.convertAndSend("/topic/presence", Map.of(
                "userId", userId.toString(),
                "online", online,
                "lastSeenAt", online ? null : new Date()
        ));
    }
}
