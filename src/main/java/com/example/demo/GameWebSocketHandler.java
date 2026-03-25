package com.example.demo;

import Model.Records.BattleResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;

import static Model.Dice.rollBattle;

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

        if (GameAction.CREATE_ROOM.equals(gameMsg.type())) {
            String newRoomId = roomManager.createRoom();
            roomManager.joinRoom(newRoomId, session);

            GameMessage response = new GameMessage(GameAction.ROOM_CREATED, newRoomId, "Server", ""+roomManager.getRooms().get(newRoomId).size());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));

        } else if (GameAction.JOIN_ROOM.equals(gameMsg.type())) {
            boolean joined = roomManager.joinRoom(gameMsg.roomId(), session);

            if (joined) {
                int index = roomManager.getRooms().get(gameMsg.roomId()).size();
                String playerNameWithIndex = gameMsg.sender() + index;

                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                        new GameMessage(GameAction.JOIN_ROOM_SUCCESS, gameMsg.roomId(), "Server", "" + index))));

                // broadcast the indexed name, not the raw sender name
                GameMessage notice = new GameMessage(GameAction.PLAYER_JOINED, gameMsg.roomId(), "Server", playerNameWithIndex);
                roomManager.broadcastToRoom(gameMsg.roomId(), objectMapper.writeValueAsString(notice));
            }
            else {
                GameMessage error = new GameMessage(GameAction.ERROR, "", "Server", "Room not found!");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
            }

        } else if (GameAction.START_GAME.equals(gameMsg.type())) {
            GameMessage startNotice = new GameMessage(GameAction.GAME_STARTED, gameMsg.roomId(), "Server", gameMsg.content());
            roomManager.broadcastToRoom(gameMsg.roomId(), objectMapper.writeValueAsString(startNotice));

        } else if (GameAction.GAME_ACTION.equals(gameMsg.type())) {

            if (gameMsg.content().startsWith("ATTACK_REQ:")) {
                // Format: "ATTACK_REQ:<attackerId>-><defenderId>:<attackerArmies>:<defenderArmies>"
                String attackData = gameMsg.content().substring(11);
                String[] parts = attackData.split(":");
                String[] ids = parts[0].split("->");
                int attackerId     = Integer.parseInt(ids[0]);
                int defenderId     = Integer.parseInt(ids[1]);
                int attackerArmies = Integer.parseInt(parts[1]);
                int defenderArmies = Integer.parseInt(parts[2]);

                BattleResult result = rollBattle(attackerArmies, defenderArmies);

                // Format: "<attackerId>:<defenderId>:<BattleResultJson>"
                String battleResultJson = objectMapper.writeValueAsString(result);
                String content = attackerId + ":" + defenderId + ":" + battleResultJson;

                GameMessage resultMsg = new GameMessage(GameAction.BATTLE_RESULT, gameMsg.roomId(), "Server", content);
                roomManager.broadcastToRoom(gameMsg.roomId(), objectMapper.writeValueAsString(resultMsg));

            } else {
                // All other actions (SETUP_PLACE, DRAFT, FORTIFY, NEXT_PHASE, CONQUEST_MOVE) — broadcast as-is
                roomManager.broadcastToRoom(gameMsg.roomId(), payload);
            }
        }
    }
}