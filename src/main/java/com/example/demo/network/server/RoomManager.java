package com.example.demo.network.server;

import com.example.demo.network.shared.GameAction;
import com.example.demo.network.shared.GameMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RoomManager {

    @Getter
    private final Map<String, List<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    private final Map<String, String> sessionToRoom = new ConcurrentHashMap<>();
    private final Map<String, String> sessionToPlayerName = new ConcurrentHashMap<>();

    public void setPlayerName(WebSocketSession session, String playerName)
    {
        sessionToPlayerName.put(session.getId(), playerName);
    }

    public String createRoom()
    {
        String roomId = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        rooms.put(roomId, new ArrayList<>());
        System.out.println("Room created with ID: " + roomId);
        return roomId;
    }

    public boolean joinRoom(String roomId, WebSocketSession session)
    {
        if (rooms.containsKey(roomId))
        {
            rooms.get(roomId).add(session);
            sessionToRoom.put(session.getId(), roomId);
            System.out.println("Player " + session.getId() + " joined room " + roomId);
            return true;
        }
        System.out.println("Failed to join: Room " + roomId + " does not exist.");
        return false;
    }


    public void handleDisconnect(WebSocketSession session)
    {
        String roomId = sessionToRoom.remove(session.getId());
        String playerName = sessionToPlayerName.remove(session.getId()); // שליפת שם השחקן
        if (roomId == null) return;

        List<WebSocketSession> sessions = rooms.get(roomId);
        if (sessions != null)
        {
            sessions.remove(session);
            System.out.println("Player " + playerName + " disconnected from room " + roomId);
            // עדכון השחקנים הנותרים לגבי מי בדיוק עזב
            if (playerName != null && !sessions.isEmpty())
                try
                {
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> content = new HashMap<>();
                    content.put("playerName", playerName);

                    GameMessage noticeMsg = new GameMessage(
                            GameAction.PLAYER_DISCONNECTED, roomId, "Server", content);

                    String notice = mapper.writeValueAsString(noticeMsg);

                    for (WebSocketSession remaining : sessions)
                    {
                        if (remaining.isOpen())
                            remaining.sendMessage(new TextMessage(notice));

                    }
                }
                catch (Exception e)
                {
                    System.out.println("Error notifying disconnect: " + e.getMessage());
                }

            if (sessions.isEmpty())
            {
                rooms.remove(roomId);
                System.out.println("Room " + roomId + " removed (empty).");
            }
        }
    }

    public void broadcastToRoom(String roomId, String message)
    {
        List<WebSocketSession> sessions = rooms.get(roomId);
        if (sessions != null)
            for (WebSocketSession session : sessions)
                if (session.isOpen())
                    try
                    {
                        session.sendMessage(new TextMessage(message));
                    }
                    catch (IOException e)
                    {
                        System.out.println("Error sending message to player " + session.getId() + ": " + e.getMessage());
                    }
    }
}