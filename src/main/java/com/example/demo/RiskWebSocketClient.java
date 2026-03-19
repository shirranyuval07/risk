package com.example.demo;

import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;

public class RiskWebSocketClient implements WebSocket.Listener {

    private WebSocket webSocket;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Getter
    @Setter
    private String playerName;
    // תוסיף את השורה הזו ליד המשתנה playerName
    @Setter @Getter
    private String roomId;

    // פונקציה שהמסך (UI) ישתמש בה כדי להגיד ללקוח מה לעשות עם התשובות
    // פה נשמור את הפעולה שהמסך מבקש שנעשה כשמגיעה הודעה
    @Setter
    private Consumer<GameMessage> onMessageReceived;

    public RiskWebSocketClient(String playerName) {
        this.playerName = playerName;
    }
    //"wss://genitourinary-nonburdensome-leola.ngrok-free.dev/risk-ws"
    public void connect() {
        HttpClient client = HttpClient.newHttpClient();
        client.newWebSocketBuilder()
                .header("ngrok-skip-browser-warning", "true") // חובה: מונע את חסימת האזהרה של ngrok
                .buildAsync(URI.create("wss://genitourinary-nonburdensome-leola.ngrok-free.dev/risk-ws"), this)
                .thenAccept(ws -> {
                    this.webSocket = ws;
                    System.out.println("Client connected and ready for UI commands!");
                })
                .exceptionally(ex -> {
                    System.out.println("❌ WebSocket Connection Failed: " + ex.getMessage());
                    // אפשר גם לעדכן את ה-UI כאן בעתיד אם רוצים שגיאה קופצת
                    return null;
                });
    }
    public void sendAction(String type, String roomId, String content) {
        try {
            if (webSocket == null) {
                System.out.println("⚠️ ERROR: Not connected to server! Cannot send action: " + type);
                // כאן אפשר להוסיף הקפצת Alert אם רוצים
                return;
            }

            GameMessage msg = new GameMessage(type, roomId, this.playerName, content);
            String jsonMessage = objectMapper.writeValueAsString(msg);
            webSocket.sendText(jsonMessage, true);
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