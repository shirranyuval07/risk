package com.example.demo;

import Model.Country;
import Model.Records.BattleResult;
import Model.RiskGame;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final RoomManager roomManager;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RiskGame serverGame = new RiskGame();

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
            boolean joined = roomManager.joinRoom(gameMsg.roomId(), session);

            if (joined) {
                session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                        new GameMessage("JOIN_ROOM_SUCCESS", gameMsg.roomId(), "Server", "Joined!"))));

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
                // Format: "ATTACK_REQ:<attackerId>-><defenderId>"
                String[] ids = gameMsg.content().substring(11).split("->");
                int attackerId = Integer.parseInt(ids[0]);
                int defenderId = Integer.parseInt(ids[1]);

                Country attacker = serverGame.getBoard().getCountry(attackerId);
                Country defender = serverGame.getBoard().getCountry(defenderId);

                if (attacker != null && defender != null) {
                    BattleResult result = serverGame.getCurrentState().attack(attacker, defender);

                    if (result != null) {
                        // Handle conquest on the server's authoritative game state
                        if (result.conquered()) {
                            // Default: move the minimum number of armies (clients can request more via CONQUEST_MOVE)
                            serverGame.handleConquest(attacker, defender, result.minMove());
                        }

                        // Encode country IDs into the content alongside the BattleResult JSON
                        // Format: "<attackerId>:<defenderId>:<BattleResultJson>"
                        String battleResultJson = objectMapper.writeValueAsString(result);
                        String content = attackerId + ":" + defenderId + ":" + battleResultJson;

                        GameMessage resultMsg = new GameMessage(
                                "BATTLE_RESULT",
                                gameMsg.roomId(),
                                "Server",
                                content
                        );
                        roomManager.broadcastToRoom(gameMsg.roomId(), objectMapper.writeValueAsString(resultMsg));
                    }
                }

            } else if (gameMsg.content().startsWith("CONQUEST_MOVE:")) {
                // The attacker chose how many armies to move after conquest
                // Format: "CONQUEST_MOVE:<attackerId>:<defenderId>:<amount>"
                // The server already moved minMove armies during ATTACK_REQ, so we move the delta here.
                String[] parts = gameMsg.content().substring(14).split(":");
                int attackerId = Integer.parseInt(parts[0]);
                int defenderId = Integer.parseInt(parts[1]);
                int totalMove = Integer.parseInt(parts[2]);

                Country attacker = serverGame.getBoard().getCountry(attackerId);
                Country defender = serverGame.getBoard().getCountry(defenderId);

                if (attacker != null && defender != null) {
                    // We already moved minMove, so broadcast the final chosen amount to all clients
                    GameMessage moveMsg = new GameMessage(
                            "GAME_ACTION",
                            gameMsg.roomId(),
                            "Server",
                            "CONQUEST_MOVE:" + attackerId + ":" + defenderId + ":" + totalMove
                    );
                    roomManager.broadcastToRoom(gameMsg.roomId(), objectMapper.writeValueAsString(moveMsg));
                }

            } else {
                // All other game actions (SETUP_PLACE, DRAFT, FORTIFY, NEXT_PHASE) — broadcast as-is
                roomManager.broadcastToRoom(gameMsg.roomId(), payload);
            }
        }
    }
}