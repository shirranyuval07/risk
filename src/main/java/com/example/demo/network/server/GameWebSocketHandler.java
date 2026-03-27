package com.example.demo.network.server;

import com.example.demo.model.Records.BattleResult;
import com.example.demo.network.shared.GameAction;
import com.example.demo.network.shared.GameMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.*;

import static com.example.demo.model.Dice.rollBattle;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final RoomManager roomManager;
    private final ObjectMapper objectMapper = new ObjectMapper();

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

        switch (gameMsg.type()) {

            case CREATE_ROOM -> {
                String newRoomId = roomManager.createRoom();
                roomManager.joinRoom(newRoomId, session);

                Map<String, Object> content = new HashMap<>();
                content.put("roomId", roomManager.getRooms().get(newRoomId).size());

                GameMessage response = new GameMessage(GameAction.ROOM_CREATED, newRoomId, "Server", content);
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
            }

            case JOIN_ROOM -> {
                boolean joined = roomManager.joinRoom(gameMsg.roomId(), session);
                Map<String, Object> content = new HashMap<>();

                if (joined) {
                    int index = roomManager.getRooms().get(gameMsg.roomId()).size();
                    String playerNameWithIndex = gameMsg.sender() + index;

                    content.put("playerID", index);
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                            new GameMessage(GameAction.JOIN_ROOM_SUCCESS, gameMsg.roomId(), "Server", content))));

                    // מילון נפרד להודעת ה-Broadcast כדי לא לערבב נתונים
                    Map<String, Object> broadcastContent = new HashMap<>();
                    broadcastContent.put("PlayerNameWithID", playerNameWithIndex);

                    GameMessage notice = new GameMessage(GameAction.PLAYER_JOINED, gameMsg.roomId(), "Server", broadcastContent);
                    roomManager.broadcastToRoom(gameMsg.roomId(), objectMapper.writeValueAsString(notice));
                } else {
                    content.put("RoomNotFound", "Room not found!");
                    GameMessage error = new GameMessage(GameAction.ERROR, "", "Server", content);
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
                }
            }

            case START_GAME -> {
                GameMessage startNotice = new GameMessage(GameAction.GAME_STARTED, gameMsg.roomId(), "Server", gameMsg.content());
                roomManager.broadcastToRoom(gameMsg.roomId(), objectMapper.writeValueAsString(startNotice));
            }

            case ATTACK_REQ -> {
                Map<String, Object> data = gameMsg.content();

                int attackerArmies = (int) data.get("ATTACKER_ARMIES");
                int defenderArmies = (int) data.get("DEFENDER_ARMIES");

                BattleResult result = rollBattle(attackerArmies, defenderArmies);

                // מכינים את תשובת השרת: מילון שמכיל את תוצאת הקרב ואת ה-IDs
                Map<String, Object> resultContent = new HashMap<>();
                resultContent.put("attackerId", data.get("ATTACK_REQ"));
                resultContent.put("defenderId", data.get("DESTINATION_ID"));
                resultContent.put("battleResult", result);

                GameMessage resultMsg = new GameMessage(GameAction.BATTLE_RESULT, gameMsg.roomId(), "Server", resultContent);
                roomManager.broadcastToRoom(gameMsg.roomId(), objectMapper.writeValueAsString(resultMsg));
            }

            // כל שאר הפעולות פשוט מועברות (Broadcast) לשאר השחקנים כמו שהן
            case SETUP_PLACE, DRAFT, FORTIFY, CONQUEST_MOVE, NEXT_PHASE, NEXT_TURN -> {
                roomManager.broadcastToRoom(gameMsg.roomId(), payload);
            }

            default -> System.out.println("Unhandled action: " + gameMsg.type());
        }
    }
}