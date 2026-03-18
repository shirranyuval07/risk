package com.example.demo;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// האנוטציה הזו אומרת ל-Spring לייצר רק עותק אחד של מנהל החדרים שיהיה זמין לכולם (Singleton)
@Component
public class RoomManager {

    // מילון ששומר לכל קוד חדר את רשימת השחקנים שמחוברים אליו.
    // אנחנו משתמשים ב-ConcurrentHashMap במקום ב-HashMap רגיל כי ברשת כמה שחקנים יכולים לנסות להתחבר בדיוק באותה מילישנייה, וזה מונע התנגשויות (Thread-Safe).
    private final Map<String, List<WebSocketSession>> rooms = new ConcurrentHashMap<>();

    // 1. יצירת חדר חדש
    public String createRoom() {
        // מגרילים קוד ייחודי ולוקחים רק את ה-4 תווים הראשונים שלו כדי שיהיה קל לחברים להקליד
        String roomId = UUID.randomUUID().toString().substring(0, 4).toUpperCase();

        // פותחים רשימה ריקה חדשה עבור החדר הזה
        rooms.put(roomId, new ArrayList<>());
        System.out.println("Room created with ID: " + roomId);

        return roomId;
    }

    // 2. הוספת שחקן לחדר קיים
    public boolean joinRoom(String roomId, WebSocketSession session) {
        if (rooms.containsKey(roomId)) {
            rooms.get(roomId).add(session);
            System.out.println("Player " + session.getId() + " joined room " + roomId);
            return true;
        }
        System.out.println("Failed to join: Room " + roomId + " does not exist.");
        return false;
    }

    // 3. הפצת הודעה לכל השחקנים בחדר (Broadcast)
    public void broadcastToRoom(String roomId, String message) {
        List<WebSocketSession> sessions = rooms.get(roomId);
        if (sessions != null) {
            for (WebSocketSession session : sessions) {
                // בודקים שהחיבור עדיין פתוח ולא התנתק פתאום
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