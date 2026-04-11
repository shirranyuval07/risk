# Multiplayer Connection Walkthrough 🎮

This document walks through the complete process of creating and joining a multiplayer room.

---

## 📋 Table of Contents
1. [Creating a Room](#creating-a-room)
2. [Joining a Room](#joining-a-room)
3. [Starting the Game](#starting-the-game)

---

# CREATING A ROOM

## 🎬 Scenario: You Click "CREATE ROOM" Button

### Step 1: Click the Button
**File:** `MainMenu.java` line 110-113
```java
Button createBtn = styledButton("CREATE ROOM", "#0055e1", 16);
createBtn.setOnAction(e -> initializeNetworkAndAction(GameAction.CREATE_ROOM, ""));
```

**What happens:** When you click, it calls `initializeNetworkAndAction()` with:
- `action = GameAction.CREATE_ROOM`
- `roomId = ""` (empty string because no room exists yet)

---

### Step 2: Create WebSocket Client
**File:** `MainMenu.java` line 132-157
```java
private void initializeNetworkAndAction(GameAction action, String roomId) {
    // Step 1: Create new client
    RiskWebSocketClient networkClient = new RiskWebSocketClient("Guest");
    
    // Step 2: Set up listener BEFORE connecting
    networkClient.setOnMessageReceived(message -> Platform.runLater(() -> {
        switch (message.type()) {
            case ROOM_CREATED -> openLobby(message, true, networkClient, ...);
            case JOIN_ROOM_SUCCESS -> openLobby(message, false, networkClient, ...);
            case ERROR -> showError(...);
            default -> {}
        }
    }));

    // Step 3: Connect
    networkClient.connect();
    
    // Step 4: Wait for connection, then send
    networkClient.waitForConnection().thenRun(() -> {
        Map<String, Object> payload = new HashMap<>();
        networkClient.sendAction(action, roomId, payload);
    }).exceptionally(ex -> {
        showError("Failed to connect: " + ex.getMessage());
        return null;
    });
}
```

**What's happening:**
1. Create a new `RiskWebSocketClient` with name "Guest"
2. Set up a **listener** that waits for messages from server
3. Call `connect()` to establish WebSocket connection
4. Once connected, send the CREATE_ROOM action

**Key Point:** The listener is set up BEFORE we connect, so it's ready to receive messages

---

### Step 3: Connect to ngrok Server
**File:** `RiskWebSocketClient.java` line 44-50
```java
public void connect() {
    log.info("🔌 Attempting to connect to: " + SERVER_URI);
    HttpClient client = HttpClient.newHttpClient();
    buildWebSocket(client)
            .thenAccept(this::onConnectionSuccess)
            .exceptionally(this::onConnectionFailure);
}
```

**What happens:**
- Tries to connect to: `wss://genitourinary-nonburdensome-leola.ngrok-free.dev/risk-ws`
- If successful → calls `onConnectionSuccess()`
- If fails → calls `onConnectionFailure()`

**Console output:**
```
🔌 Attempting to connect to: wss://genitourinary-nonburdensome-leola.ngrok-free.dev/risk-ws
```

---

### Step 4: Connection Successful
**File:** `RiskWebSocketClient.java` line 62-68
```java
private void onConnectionSuccess(WebSocket ws) {
    this.webSocket = ws;
    log.fine("Client connected and ready for UI commands!");
    // ⭐ REQUEST FIRST MESSAGE
    ws.request(1);
    connectionReady.complete(ws);
}
```

**What happens:**
1. Store the WebSocket connection: `this.webSocket = ws`
2. **⭐ Tell server to send first message:** `ws.request(1)` 
   - This is CRUCIAL - without this, messages won't be delivered!
3. Mark the future as complete: `connectionReady.complete(ws)`
   - This tells the waiting code: "You can proceed now!"

**Console output:**
```
✅ Client connected and ready for UI commands!
```

**Flow timeline:**
```
waitForConnection() (from MainMenu) is waiting...
        ↓
    Connection succeeds
        ↓
    connectionReady.complete(ws) is called
        ↓
    waitForConnection() returns and calls thenRun()
        ↓
    sendAction() is called
```

---

### Step 5: Send CREATE_ROOM Action
**File:** `RiskWebSocketClient.java` line 92-107
```java
public void sendAction(GameAction type, String roomId, Map<String,Object> content) {
    try {
        if (webSocket == null) {
            log.severe("⚠️ ERROR: Not connected to server! Cannot send action: " + type);
            return;
        }

        // Create message
        GameMessage msg = new GameMessage(type, roomId, this.playerName, content);
        String jsonMessage = objectMapper.writeValueAsString(msg);
        
        // Send it
        log.info("📤 Sending action: " + type + " to room: " + roomId);
        webSocket.sendText(jsonMessage, true);
    } catch (Exception e) {
        log.severe("Error sending message: " + e.getMessage());
    }
}
```

**What's sent (as JSON):**
```json
{
  "type": "CREATE_ROOM",
  "roomId": "",
  "sender": "Guest",
  "content": {}
}
```

**Console output:**
```
📤 Sending action: CREATE_ROOM to room: 
```

---

### Step 6: Server Receives CREATE_ROOM
**File:** `GameWebSocketHandler.java` line 40-51
```java
case CREATE_ROOM -> {
    String newRoomId = roomManager.createRoom();  // e.g., "ABCD"
    roomManager.joinRoom(newRoomId, session);     // Add you to room
    
    Map<String, Object> content = new HashMap<>();
    content.put("roomId", roomManager.getRooms().get(newRoomId).size());
    
    GameMessage response = new GameMessage(
        GameAction.ROOM_CREATED, 
        newRoomId, 
        "Server", 
        content
    );
    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(response)));
}
```

**Server's actions:**
1. Generate new room ID (e.g., "ABCD")
2. Add your WebSocket session to that room
3. Create a response message with type `ROOM_CREATED`
4. Send the response back to you

**Server sends back (as JSON):**
```json
{
  "type": "ROOM_CREATED",
  "roomId": "ABCD",
  "sender": "Server",
  "content": {
    "roomId": 1
  }
}
```

---

### Step 7: Client Receives ROOM_CREATED
**File:** `RiskWebSocketClient.java` line 115-130
```java
@Override
public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
    try {
        // Parse the JSON
        GameMessage msg = objectMapper.readValue(data.toString(), GameMessage.class);
        log.info("📨 Received message: " + msg.type() + " from room: " + msg.roomId());
        
        // Call the listener we set up
        if (onMessageReceived != null) {
            Platform.runLater(() -> onMessageReceived.accept(msg));
        }
    } catch (Exception e) {
        log.severe("Error parsing received JSON: " + e.getMessage());
    }
    
    // ⭐ REQUEST NEXT MESSAGE
    webSocket.request(1);
    return CompletableFuture.completedFuture(null);
}
```

**What happens:**
1. Server sends the message over WebSocket
2. `onText()` is automatically called by Java WebSocket API
3. Parse the JSON → convert to `GameMessage` object
4. The message type is `ROOM_CREATED` and roomId is `"ABCD"`
5. Call the listener: `onMessageReceived.accept(msg)`
6. **⭐ Tell server to send next message:** `webSocket.request(1)`

**Console output:**
```
📨 Received message: ROOM_CREATED from room: ABCD
```

---

### Step 8: Listener Processes ROOM_CREATED
**File:** `MainMenu.java` line 137-147
```java
networkClient.setOnMessageReceived(message -> Platform.runLater(() -> {
    switch (message.type()) {
        case ROOM_CREATED -> openLobby(message, true, networkClient, (VBox) getChildren().getFirst());
        case JOIN_ROOM_SUCCESS -> openLobby(message, false, networkClient, (VBox) getChildren().getFirst());
        case ERROR -> {
            Map<String, Object> payload = message.content();
            showError(payload != null ? (String) payload.get("RoomNotFound") : "Unknown Error");
        }
        default -> {}
    }
}));
```

**What happens:**
1. Message type is `ROOM_CREATED` ✅
2. Call `openLobby(message, true, networkClient, ...)`
   - `true` means you are the HOST (you created it)
   - `message` contains the room ID "ABCD"
   - `networkClient` is the connection to keep listening

---

### Step 9: Lobby Opens
**File:** `MainMenu.java` line 172-190
```java
private void openLobby(GameMessage message, boolean isHost,
                       RiskWebSocketClient networkClient, VBox mainContent) {
    String myName = networkClient.getPlayerName();  // "Guest"
    
    // Update name if joining (not creating)
    if (!isHost && message.content() != null) {
        Map<String, Object> payload = message.content();
        if (payload.containsKey("playerID")) {
            myName += payload.get("playerID").toString();
        }
    }
    
    networkClient.setPlayerName(myName);
    
    // Create lobby screen
    LobbyScreen lobby = new LobbyScreen(
        message.roomId(),      // "ABCD"
        isHost,                // true
        myName,                // "Guest"
        networkClient,         // the connection
        onStartGame            // callback when game starts
    );
    
    mainContent.setVisible(false);
    getChildren().add(lobby);
}
```

**What you see on screen:**
```
┌─────────────────────────────────┐
│  Waiting Room: ABCD             │
├─────────────────────────────────┤
│  Waiting for players to join... │
│                                 │
│  Players in room:               │
│  - You (Guest)                  │
│                                 │
│  [START MULTIPLAYER GAME]       │
└─────────────────────────────────┘
```

**The room is created! 🎉**

---

---

# JOINING A ROOM

## 🎬 Scenario: Your Friend Clicks "JOIN ROOM" Button and Enters Code "ABCD"

### Step 1: Click JOIN ROOM Button
**File:** `MainMenu.java` line 114
```java
joinBtn.setOnAction(e -> promptJoinRoom());
```

A dialog appears asking for the room code.

---

### Step 2: Friend Enters Code "ABCD"
**File:** `MainMenu.java` line 121-130
```java
private void promptJoinRoom() {
    TextInputDialog dialog = new TextInputDialog();
    dialog.setTitle("Join Room");
    dialog.setHeaderText("Enter the 4-character Room Code:");
    dialog.showAndWait().ifPresent(code -> {
        if (!code.trim().isEmpty()) {
            initializeNetworkAndAction(GameAction.JOIN_ROOM, code.toUpperCase());
            // Calls with: action = JOIN_ROOM, roomId = "ABCD"
        }
    });
}
```

**Steps 3-7 are the SAME as creating a room!**
- Connect to server
- Set up listener
- Request first message
- Send JOIN_ROOM action

**The key difference is:**
- CREATE_ROOM sends: `action = CREATE_ROOM, roomId = ""`
- JOIN_ROOM sends: `action = JOIN_ROOM, roomId = "ABCD"`

---

### Step 6B: Server Receives JOIN_ROOM
**File:** `GameWebSocketHandler.java` line 53-76
```java
case JOIN_ROOM -> {
    // Try to join the room
    boolean joined = roomManager.joinRoom(gameMsg.roomId(), session);
    
    Map<String, Object> content = new HashMap<>();
    
    if (joined) {
        // Success!
        int index = roomManager.getRooms().get(gameMsg.roomId()).size();
        String playerNameWithIndex = gameMsg.sender() + index;  // "Guest2"
        
        content.put("playerID", index);
        
        // Send success response to friend
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
            new GameMessage(GameAction.JOIN_ROOM_SUCCESS, gameMsg.roomId(), "Server", content)
        )));
        
        // Broadcast to everyone in room
        Map<String, Object> broadcastContent = new HashMap<>();
        broadcastContent.put("PlayerNameWithID", playerNameWithIndex);
        
        GameMessage notice = new GameMessage(GameAction.PLAYER_JOINED, gameMsg.roomId(), "Server", broadcastContent);
        roomManager.broadcastToRoom(gameMsg.roomId(), objectMapper.writeValueAsString(notice));
    } else {
        // Failed - room doesn't exist
        content.put("RoomNotFound", "Room not found!");
        GameMessage error = new GameMessage(GameAction.ERROR, "", "Server", content);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(error)));
    }
}
```

**Server does two things:**

**1. Send to Friend:** "You joined successfully!"
```json
{
  "type": "JOIN_ROOM_SUCCESS",
  "roomId": "ABCD",
  "sender": "Server",
  "content": {
    "playerID": 2
  }
}
```

**2. Broadcast to ALL in room (including YOU):** "A new player joined!"
```json
{
  "type": "PLAYER_JOINED",
  "roomId": "ABCD",
  "sender": "Server",
  "content": {
    "PlayerNameWithID": "Guest2"
  }
}
```

---

### Step 7B: Friend Receives JOIN_ROOM_SUCCESS

Same `onText()` method is called for friend.

**Console output:**
```
📨 Received message: JOIN_ROOM_SUCCESS from room: ABCD
```

Friend's listener processes it:
```java
case JOIN_ROOM_SUCCESS -> openLobby(message, false, networkClient, ...)
```

The lobby opens for friend with `isHost = false` (they're a guest).

---

### Step 7C: YOU (Host) Receive PLAYER_JOINED

Your WebSocket also receives the broadcast:
```
📨 Received message: PLAYER_JOINED from room: ABCD
```

But YOUR listener is looking for ROOM_CREATED or JOIN_ROOM_SUCCESS, not PLAYER_JOINED!

**This is handled by the LobbyScreen's NEW listener:**

**File:** `MainMenu.java` line 265-291
```java
private static class LobbyScreen extends VBox {
    public LobbyScreen(...) {
        // ... UI setup ...
        
        // Set up NEW listener for lobby messages
        networkClient.setOnMessageReceived(message ->
            javafx.application.Platform.runLater(() -> {
                Map<String, Object> payload = message.content();
                
                switch (message.type()) {
                    case PLAYER_JOINED -> {
                        String newPlayer = (String) payload.get("PlayerNameWithID");
                        lobbyPlayers.add(newPlayer);
                        playerList.appendText("- " + newPlayer + " has joined!\n");
                    }
                    
                    case GAME_STARTED -> {
                        networkClient.setRoomId(message.roomId());
                        long seed = ((Number) payload.get("seed")).longValue();
                        List<String> playersList = (List<String>) payload.get("players");
                        networkClient.setGameSeed(seed);
                        onStartGame.accept(buildPlayerSetups(playersList), networkClient);
                    }
                    
                    default -> {}
                }
            })
        );
    }
}
```

**What you (the host) see:**
```
┌─────────────────────────────────┐
│  Waiting Room: ABCD             │
├─────────────────────────────────┤
│  Waiting for players to join... │
│                                 │
│  Players in room:               │
│  - You (Guest)                  │
│  - Guest2 has joined!           │ ← NEW!
│                                 │
│  [START MULTIPLAYER GAME]       │
└─────────────────────────────────┘
```

---

---

# STARTING THE GAME

## 🎬 Scenario: You (Host) Click "START MULTIPLAYER GAME"

### Step 1: Click START Button
**File:** `MainMenu.java` line 251-260
```java
Button startBtn = styledButton("START MULTIPLAYER GAME", "#e13c3c", 20);
startBtn.setVisible(isHost);  // Only host can see this
startBtn.setOnAction(e -> {
    long seed = new java.util.Random().nextLong();  // Generate random seed
    Map<String, Object> payload = new HashMap<>();
    payload.put("seed", seed);
    payload.put("players", lobbyPlayers);  // ["Guest", "Guest2"]
    
    networkClient.sendAction(GameAction.START_GAME, roomCode, payload);
});
```

**What's sent (as JSON):**
```json
{
  "type": "START_GAME",
  "roomId": "ABCD",
  "sender": "Guest",
  "content": {
    "seed": 1234567890,
    "players": ["Guest", "Guest2"]
  }
}
```

---

### Step 2: Server Receives START_GAME

**File:** `GameWebSocketHandler.java` line 78-81
```java
case START_GAME -> {
    GameMessage startNotice = new GameMessage(
        GameAction.GAME_STARTED, 
        gameMsg.roomId(), 
        "Server", 
        gameMsg.content()  // Same seed and players list
    );
    roomManager.broadcastToRoom(gameMsg.roomId(), objectMapper.writeValueAsString(startNotice));
}
```

**Server broadcasts to ALL in room:**
```json
{
  "type": "GAME_STARTED",
  "roomId": "ABCD",
  "sender": "Server",
  "content": {
    "seed": 1234567890,
    "players": ["Guest", "Guest2"]
  }
}
```

---

### Step 3: Both Clients Receive GAME_STARTED

**Console output (both you and friend):**
```
📨 Received message: GAME_STARTED from room: ABCD
```

LobbyScreen listener handles it:
```java
case GAME_STARTED -> {
    networkClient.setRoomId(message.roomId());        // "ABCD"
    long seed = ((Number) payload.get("seed")).longValue();
    List<String> playersList = (List<String>) payload.get("players");
    
    networkClient.setGameSeed(seed);
    onStartGame.accept(buildPlayerSetups(playersList), networkClient);
}
```

---

### Step 4: Game Launches

**File:** `RiskApplication.java` line 71-113
```java
private void startGameWithConfig(List<PlayerSetup> playerSetups, RiskWebSocketClient networkClient) {
    RiskGame game = new RiskGame();
    
    // Add players
    for (MainMenu.PlayerSetup setup : playerSetups) {
        Player p = new Player(setup.name(), setup.color(), false);  // All are Human
        game.addPlayer(p);
    }
    
    // Set game seed so both clients have same random events
    if (networkClient != null) {
        game.setGameSeed(networkClient.getGameSeed());
    }
    
    game.startGame();
    
    // Show game
    GameRoot root = new GameRoot(game);
    new GameController(game, root, networkClient, this::showMainMenu);
    
    Scene gameScene = new Scene(root, 1200, 800);
    primaryStage.setScene(gameScene);
}
```

**What happens:**
1. Create RiskGame instance
2. Add both players to game
3. Set the same seed for both clients (so dice rolls are synchronized)
4. Initialize GameController to handle multiplayer actions
5. Show the game board

**Both players now see the game board and can play together! 🎮**

---

---

## 📊 Complete Message Flow Diagram

```
YOU (Host)                          SERVER                          FRIEND (Guest)
    │                                  │                                  │
    │ Click CREATE ROOM                │                                  │
    ├──────────────────────────────────→ CREATE_ROOM                       │
    │                                  │                                  │
    │ ← ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ROOM_CREATED (ABCD)               │
    │ (Lobby opens)                    │                                  │
    │                                  │                                  │
    │ [Waiting for players...]         │                                  │
    │                                  │                                  │
    │                                  │                     JOIN_ROOM ← ─
    │                                  │ (ABCD from friend)  (code entered)
    │                                  │                                  │
    │ ← ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ PLAYER_JOINED ─ ─ ─ ─ ─ ─ ─ ─ ─→
    │ (Updates player list)            │ (broadcast to room) JOIN_ROOM_SUCCESS ←
    │ "Guest2 has joined!"             │                     (Lobby opens)
    │                                  │                                  │
    │ Click START GAME                 │                                  │
    ├──────────────────────────────────→ START_GAME                       │
    │ (seed + players list)            │                                  │
    │                                  ├─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─→
    │ ← ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ ─ GAME_STARTED         GAME_STARTED
    │ (Game launches)                  │ (broadcast to room) (Game launches)
    │                                  │                                  │
    │═══════════════════════════════════════ GAME IN PROGRESS ═══════════════════
    │ (Both players can now play together with same board state!)
```

---

## 🔑 Key Takeaways

1. **Creating a room:** You send CREATE_ROOM, server assigns an ID, you become host
2. **Joining a room:** Friend sends JOIN_ROOM with code, server verifies it exists
3. **Broadcasting:** Server sends PLAYER_JOINED to all in room so you see friend join
4. **Starting game:** Host sends START_GAME with seed, everyone launches with same seed
5. **WebSocket requests:** CRITICAL - must call `request(1)` to keep listening
6. **Listener switching:** First listener handles room setup, second listener handles game events

---

## 🛠 If Something Goes Wrong

### "Failed to connect to server"
- Check if server is running with `--server` flag
- Check if ngrok URL in `RiskWebSocketClient.java` is correct
- Check console for: `🔌 Attempting to connect to: ...`

### "Lobby doesn't open after clicking CREATE"
- Check console for: `📨 Received message: ROOM_CREATED`
- If not there, the `request(1)` fix wasn't applied
- Make sure listener is set BEFORE connecting

### "Friend can't join room"
- Friend must enter EXACT room code (uppercase)
- Check server console for error handling in JOIN_ROOM case
- Room must exist (host must have created it first)

### "Game launches but no players see each other's moves"
- Check GameController is receiving messages properly
- Check that networkClient is passed to GameController
- All game actions must be sent through WebSocket, not just locally

