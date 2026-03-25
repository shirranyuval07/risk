package Model;

public interface GameUpdateListener {
    // מופעל כשצריך לעדכן את כמות החיילים/תורות במסך
    void onStatsUpdated();

    // מופעל כשרוצים להציג הודעה בפאנל השליטה
    void onGameMessage(String message);
}