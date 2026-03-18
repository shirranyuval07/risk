package Model;

public enum Card {
    INFANTRY, // חייל רגלי
    CAVALRY,  // פרשים
    ARTILLERY; // תותחנים

    public static Card getRandom() {
        return values()[(int) (Math.random() * values().length)];
    }
}