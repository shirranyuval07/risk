package com.example.demo.network.shared;

import java.util.Map;

// המבנה הזה יתורגם אוטומטית ל-JSON ולהיפך
public record GameMessage(
        GameAction type,    // סוג הפעולה: "CREATE_ROOM", "JOIN_ROOM", "CHAT", "ATTACK" וכו'
        String roomId,  // קוד החדר שרלוונטי להודעה (למשל "A7B2")
        String sender,  // מי שלח את ההודעה (למשל השם של השחקן)
        Map<String, Object> content // תוכן ההודעה (יכול להיות כל אובייקט שהוא)
) {}