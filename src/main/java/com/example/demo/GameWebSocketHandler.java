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

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final RoomManager roomManager;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

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

            GameMessage response = new GameMessage("ROOM_CREATED", newRoomId, "Server", ""+roomManager.getRooms().get(newRoomId).size());
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));

        } else if ("JOIN_ROOM".equals(gameMsg.type())) {
            boolean joined = roomManager.joinRoom(gameMsg.roomId(), session);

            if (joined) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                        new GameMessage("JOIN_ROOM_SUCCESS", gameMsg.roomId(), "Server", ""+roomManager.getRooms().get(gameMsg.roomId()).size()))));

                GameMessage notice = new GameMessage("PLAYER_JOINED", gameMsg.roomId(), "Server", gameMsg.sender());
                roomManager.broadcastToRoom(gameMsg.roomId(), objectMapper.writeValueAsString(notice));

            } else {
                GameMessage error = new GameMessage("ERROR", "", "Server", "Room not found!");
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
            }

        } else if ("START_GAME".equals(gameMsg.type())) {
            GameMessage startNotice = new GameMessage("GAME_STARTED", gameMsg.roomId(), "Server", gameMsg.content());
            roomManager.broadcastToRoom(gameMsg.roomId(), objectMapper.writeValueAsString(startNotice));

        } else if ("GAME_ACTION".equals(gameMsg.type())) {

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

                GameMessage resultMsg = new GameMessage("BATTLE_RESULT", gameMsg.roomId(), "Server", content);
                roomManager.broadcastToRoom(gameMsg.roomId(), objectMapper.writeValueAsString(resultMsg));

            } else {
                // All other actions (SETUP_PLACE, DRAFT, FORTIFY, NEXT_PHASE, CONQUEST_MOVE) — broadcast as-is
                roomManager.broadcastToRoom(gameMsg.roomId(), payload);
            }
        }
    }

    /**
     * Pure dice-rolling battle logic — mirrors AttackState.attack() but needs no game state.
     */
    private BattleResult rollBattle(int attackerArmies, int defenderArmies) {
        int aDiceCount = Math.min(3, attackerArmies - 1);
        int dDiceCount = Math.min(2, defenderArmies);

        Integer[] aRolls = rollDice(aDiceCount);
        Integer[] dRolls = rollDice(dDiceCount);

        Arrays.sort(aRolls, Collections.reverseOrder());
        Arrays.sort(dRolls, Collections.reverseOrder());

        int comparisons = Math.min(aDiceCount, dDiceCount);
        int aLoss = 0, dLoss = 0;

        for (int i = 0; i < comparisons; i++) {
            if (aRolls[i] > dRolls[i]) dLoss++;
            else aLoss++;
        }

        boolean conquered = (defenderArmies - dLoss) <= 0;
        int minMove = aDiceCount;
        int maxMove = attackerArmies - 1 - aLoss;

        return new BattleResult(aRolls, dRolls, aLoss, dLoss, conquered, minMove, maxMove);
    }

    private Integer[] rollDice(int count) {
        Integer[] rolls = new Integer[count];
        for (int i = 0; i < count; i++) {
            rolls[i] = random.nextInt(6) + 1;
        }
        return rolls;
    }
}