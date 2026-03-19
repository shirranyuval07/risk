package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final RoomManager roomManager;

    // במקום לבקש מ-Spring, אנחנו יוצרים את האובייקט בעצמנו:
    private final ObjectMapper objectMapper = new ObjectMapper();

    // הסרנו את ה-ObjectMapper מהבנאי, עכשיו Spring יזריק רק את מנהל החדרים
    public GameWebSocketHandler(RoomManager roomManager) {
        this.roomManager = roomManager;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        System.out.println("Connection opened: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        GameMessage gameMsg = objectMapper.readValue(payload, GameMessage.class);

        System.out.println("Received action: " + gameMsg.type() + " from " + gameMsg.sender());

        if ("CREATE_ROOM".equals(gameMsg.type())) {
            String newRoomId = roomManager.createRoom();
            roomManager.joinRoom(newRoomId, session);

            GameMessage response = new GameMessage("ROOM_CREATED", newRoomId, "Server", "Your room is ready!");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));

        } else if ("JOIN_ROOM".equals(gameMsg.type())) {
            // ניסיון הצטרפות
            boolean joined = roomManager.joinRoom(gameMsg.roomId(), session);

            if (joined) {
                // 1. אישור הצלחה לשחקן שהצטרף
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                        new GameMessage("JOIN_ROOM_SUCCESS", gameMsg.roomId(), "Server", "Joined!"))));

                // 2. עדכון לכולם בחדר (כולל המארח) שמישהו חדש נכנס
                // אנחנו שולחים את השם של מי שהצטרף ב-content
                GameMessage notice = new GameMessage("PLAYER_JOINED", gameMsg.roomId(), "Server", gameMsg.sender());
                roomManager.broadcastToRoom(gameMsg.roomId(), objectMapper.writeValueAsString(notice));

            } else {
                // החדר באמת לא קיים
                GameMessage error = new GameMessage("ERROR", "", "Server", "Room not found!");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
            }
        }
        else if ("START_GAME".equals(gameMsg.type())) {
            // המארח לחץ על כפתור ההתחלה
            GameMessage startNotice = new GameMessage("GAME_STARTED", gameMsg.roomId(), "Server", "The game is starting now!");
            String jsonNotice = objectMapper.writeValueAsString(startNotice);

            // שולחים לכולם בחדר!
            roomManager.broadcastToRoom(gameMsg.roomId(), jsonNotice);
        }
    }
}
