package com.example.demo;

import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomManager {

    @Getter
    private final Map<String, List<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    // Also track which room each session belongs to, so we can clean up on disconnect
    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();

    public String createRoom() {
        String roomId = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        rooms.put(roomId, new ArrayList<>());
        System.out.println("Room created with ID: " + roomId);
        return roomId;
    }

    public boolean joinRoom(String roomId, WebSocketSession session) {
        if (rooms.containsKey(roomId)) {
            rooms.get(roomId).add(session);
            sessionToRoom.put(session.getId(), roomId); // remember which room this session is in
            System.out.println("Player " + session.getId() + " joined room " + roomId);
            return true;
        }
        System.out.println("Failed to join: Room " + roomId + " does not exist.");
        return false;
    }

    /**
     * Called when a WebSocket connection closes (normally or due to error).
     * Removes the session from its room and cleans up empty rooms.
     */
    public void handleDisconnect(WebSocketSession session) {
        String roomId = sessionToRoom.remove(session.getId());
        if (roomId == null) return; // session was never in a room

        List<WebSocketSession> sessions = rooms.get(roomId);
        if (sessions != null) {
            sessions.remove(session);
            System.out.println("Player " + session.getId() + " disconnected from room " + roomId);

            // Notify remaining players that someone left
            String notice = buildDisconnectNotice(roomId);
            for (WebSocketSession remaining : sessions) {
                if (remaining.isOpen()) {
                    try {
                        remaining.sendMessage(new TextMessage(notice));
                    } catch (IOException e) {
                        System.out.println("Error notifying disconnect: " + e.getMessage());
                    }
                }
            }

            // Clean up the room entirely if it's now empty
            if (sessions.isEmpty()) {
                rooms.remove(roomId);
                System.out.println("Room " + roomId + " removed (empty).");
            }
        }
    }

    private String buildDisconnectNotice(String roomId) {
        // Simple JSON — reuses the GameMessage structure
        return "{\"type\":\"PLAYER_DISCONNECTED\",\"roomId\":\"" + roomId + "\",\"sender\":\"Server\",\"content\":\"A player has disconnected.\"}";
    }

    public void broadcastToRoom(String roomId, String message) {
        List<WebSocketSession> sessions = rooms.get(roomId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(message));
                    } catch (IOException e) {
                        System.out.println("Error sending message to player " + session.getId() + ": " + e.getMessage());
                    }
                }
            }
        }
    }
}