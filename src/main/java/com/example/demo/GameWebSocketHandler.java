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
        // 1. ממירים את הטקסט שהגיע (JSON) לאובייקט Java
        String payload = message.getPayload();
        GameMessage gameMsg = objectMapper.readValue(payload, GameMessage.class);

        System.out.println("Received action: " + gameMsg.type() + " from " + gameMsg.sender());

        // 2. בודקים מה השחקן רוצה לעשות
        if ("CREATE_ROOM".equals(gameMsg.type())) {
            String newRoomId = roomManager.createRoom();
            roomManager.joinRoom(newRoomId, session); // המארח ישר מצטרף לחדר שהוא יצר

            // שולחים למארח חזרה את הקוד כדי שיוכל להגיד אותו לחברים
            GameMessage response = new GameMessage("ROOM_CREATED", newRoomId, "Server", "Your room is ready!");
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));

        } else if ("JOIN_ROOM".equals(gameMsg.type())) {
            boolean joined = roomManager.joinRoom(gameMsg.roomId(), session);
            if (joined) {
                // השורה החדשה להוסיף: שולחים לשחקן הודעה שהוא הצליח להיכנס
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(new GameMessage("JOIN_ROOM_SUCCESS", gameMsg.roomId(), "Server", "Joined!"))));

                // נודיע לכולם בחדר שמישהו חדש הגיע! (כבר יש לך את הקוד הזה)
                GameMessage notice = new GameMessage("PLAYER_JOINED", gameMsg.roomId(), "Server", gameMsg.sender() + " has joined the room!");
                roomManager.broadcastToRoom(gameMsg.roomId(), objectMapper.writeValueAsString(notice));
            }
            } else {
                // החדר לא קיים
                GameMessage error = new GameMessage("ERROR", "", "Server", "Room not found!");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
            }
        }
    }
