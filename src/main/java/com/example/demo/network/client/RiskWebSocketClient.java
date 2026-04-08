package com.example.demo.network.client;

import com.example.demo.network.shared.GameAction;
import com.example.demo.network.shared.GameMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import javafx.application.Platform;
import lombok.Getter;
import lombok.Setter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class RiskWebSocketClient implements WebSocket.Listener {

    private static final String SERVER_URI = "wss://genitourinary-nonburdensome-leola.ngrok-free.dev/risk-ws";

    private WebSocket webSocket;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Getter
    @Setter
    private String playerName;
    @Setter @Getter
    private String roomId;

    private final Logger log = Logger.getLogger(RiskWebSocketClient.class.getName());
    @Setter @Getter
    private long gameSeed = 0;

    @Setter
    private Consumer<GameMessage> onMessageReceived;

    private final CompletableFuture<WebSocket> connectionReady = new CompletableFuture<>();

    public RiskWebSocketClient(String playerName) {
        this.playerName = playerName;
    }

    public void connect() {
        HttpClient client = HttpClient.newHttpClient();
        buildWebSocket(client)
                .thenAccept(this::onConnectionSuccess)
                .exceptionally(this::onConnectionFailure);
    }

    public CompletableFuture<WebSocket> waitForConnection() {
        return connectionReady;
    }

    private CompletableFuture<WebSocket> buildWebSocket(HttpClient client) {
        return client.newWebSocketBuilder()
                .header("ngrok-skip-browser-warning", "true")
                .buildAsync(URI.create(SERVER_URI), this);
    }

    private void onConnectionSuccess(WebSocket ws) {
        this.webSocket = ws;
        log.fine("Client connected and ready for UI commands!");
        connectionReady.complete(ws);
    }

    private Void onConnectionFailure(Throwable ex) {
        log.warning("❌ WebSocket Connection Failed: " + ex.getMessage());
        connectionReady.completeExceptionally(ex);
        return null;
    }

    public void disconnect() {
// קודם כל בודקים שבכלל יש חיבור פעיל כדי לא לקבל שגיאת NullPointer
        if (this.webSocket != null) {
            try {
                // שולחים לשרת הודעת סגירה מסודרת.
                // הפקודה NORMAL_CLOSURE (קוד 1000) אומרת לשרת "אני מתנתק מרצוני, הכל בסדר".
                this.webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Returning to main menu")
                        .thenAccept(ws -> {
                            log.info("WebSocket disconnected gracefully.");
                            this.webSocket = null; // מנקים את המשתנה
                        });
            } catch (Exception e) {
                log.warning("Error while disconnecting from WebSocket: " + e.getMessage());
            }
        }
    }
    public void sendAction(GameAction type, String roomId, Map<String,Object> content) {
        try {
            if (webSocket == null) {
                log.severe("⚠️ ERROR: Not connected to server! Cannot send action: " + type);
                // כאן אפשר להוסיף הקפצת Alert אם רוצים
                return;
            }

            GameMessage msg = new GameMessage(type, roomId, this.playerName, content);
            String jsonMessage = objectMapper.writeValueAsString(msg);
            webSocket.sendText(jsonMessage, true);
        } catch (Exception e) {
            log.severe("Error sending message: " + e.getMessage());
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
            log.severe("Error parsing received JSON");
        }
        return WebSocket.Listener.super.onText(webSocket, data, last);
    }

}