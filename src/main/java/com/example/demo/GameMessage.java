package com.example.demo;

// המבנה הזה יתורגם אוטומטית ל-JSON ולהיפך
public record GameMessage(
        String type,    // סוג הפעולה: "CREATE_ROOM", "JOIN_ROOM", "CHAT", "ATTACK" וכו'
        String roomId,  // קוד החדר שרלוונטי להודעה (למשל "A7B2")
        String sender,  // מי שלח את ההודעה (למשל השם של השחקן)
        String content  // תוכן ההודעה (יכול להיות טקסט חופשי או נתונים נוספים)
) {}