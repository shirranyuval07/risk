package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import lombok.Setter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class RiskWebSocketClient implements WebSocket.Listener {

    private WebSocket webSocket;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Setter
    private String playerName;

    // פונקציה שהמסך (UI) ישתמש בה כדי להגיד ללקוח מה לעשות עם התשובות
    // פה נשמור את הפעולה שהמסך מבקש שנעשה כשמגיעה הודעה
    @Setter
    private Consumer<GameMessage> onMessageReceived;

    public RiskWebSocketClient(String playerName) {
        this.playerName = playerName;
    }

    public void connect() {
        HttpClient client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .buildAsync(URI.create("wss://genitourinary-nonburdensome-leola.ngrok-free.dev/risk-ws"), this)
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    System.out.println("Client connected and ready for UI commands!");
                });
    }

    // פונקציה חדשה שדרכה הכפתורים במסך ישלחו בקשות לשרת
    public void sendAction(String type, String roomId, String content) {
        try {
            GameMessage msg = new GameMessage(type, roomId, this.playerName, content);
            String jsonMessage = objectMapper.writeValueAsString(msg);
            if (webSocket != null) {
                webSocket.sendText(jsonMessage, true);
            }
        } catch (Exception e) {
            System.out.println("Error sending message: " + e.getMessage());
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        WebSocket.Listener.super.onOpen(webSocket);
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        try {
            GameMessage msg = objectMapper.readValue(data.toString(), GameMessage.class);

            // כשהודעה מגיעה, אנחנו מעבירים אותה לחוט של ה-UI כדי שיעדכן את המסך
            if (onMessageReceived != null) {
                Platform.runLater(() -> onMessageReceived.accept(msg));
            }

        } catch (Exception e) {
            System.out.println("Error parsing received JSON");
        }
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }
}