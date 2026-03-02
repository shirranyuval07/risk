package Model;

import lombok.Getter;

import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

import java.util.*;

/**
 * לוח המשחק – 42 טריטוריות עם פוליגונים אורגניים בסגנון מפת ריסק קלאסית.
 * כל טריטוריה ביבשת מחוברת לשכנות שלה דרך גבולות משותפים מדויקים.
 * חופים אורגניים, גבולות פנימיים ישרים. Canvas: 1060×700.
 */
public class Board {
    private final Map<Integer, Country> countries = new HashMap<>();
    @Getter
    private final List<Continent> continents = new ArrayList<>();

    public Board() {
        initializeFullWorld();
    }

    private void initializeFullWorld() {
        // שימוש ב-Color.rgb של JavaFX במקום java.awt.Color
        Continent na = new Continent("North America", 5, Color.rgb(65, 135, 210));
        Continent sa = new Continent("South America", 2, Color.rgb(46, 170, 80));
        Continent eu = new Continent("Europe", 5, Color.rgb(140, 80, 190));
        Continent af = new Continent("Africa", 3, Color.rgb(220, 140, 40));
        Continent as = new Continent("Asia", 7, Color.rgb(200, 55, 55));
        Continent au = new Continent("Australia", 2, Color.rgb(210, 175, 45));
        continents.addAll(Arrays.asList(na, sa, eu, af, as, au));

        buildNorthAmerica(na);
        buildSouthAmerica(sa);
        buildEurope(eu);
        buildAfrica(af);
        buildAsia(as);
        buildAustralia(au);
        buildAllConnections();

        applyContinentAdjustments();
    }

    // ================================================================
    // אלגוריתם להקטנת יבשות ויצירת מרווחי אוקיינוס גדולים (מותאם ל-JavaFX)
    // ================================================================
    private void applyContinentAdjustments() {
        adjustContinent(continents.get(0), 0.82, -40, -30); // צפון אמריקה
        adjustContinent(continents.get(1), 0.85, -40, -45);  // דרום אמריקה
        adjustContinent(continents.get(2), 0.85, 0, -5);   // אירופה
        adjustContinent(continents.get(3), 0.85, 15, -35);    // אפריקה
        adjustContinent(continents.get(4), 0.85, -35, -20);  // אסיה
        adjustContinent(continents.get(5), 0.85, 35, -20);   // אוסטרליה
    }

    private void adjustContinent(Continent cont, double scale, int offsetX, int offsetY) {
        // 1. חישוב מרכז היבשת המדויק מתוך הרשימה השטוחה של JavaFX
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (Country c : cont.getCountries()) {
            Polygon p = c.getShape();
            List<Double> points = p.getPoints();

            // עוברים על הנקודות בזוגות (X ואז Y)
            for (int i = 0; i < points.size(); i += 2) {
                double px = points.get(i);
                double py = points.get(i + 1);

                if (px < minX) minX = px;
                if (px > maxX) maxX = px;
                if (py < minY) minY = py;
                if (py > maxY) maxY = py;
            }
        }

        double centerX = (minX + maxX) / 2.0;
        double centerY = (minY + maxY) / 2.0;

        // 2. עדכון נקודות הפוליגון והדיסקיות לכל מדינה ביבשת (משמר את הפאזל הפנימי)
        for (Country c : cont.getCountries()) {
            Polygon p = c.getShape();
            List<Double> points = p.getPoints();

            for (int i = 0; i < points.size(); i += 2) {
                double oldX = points.get(i);
                double oldY = points.get(i + 1);

                // חישוב הקואורדינטות החדשות
                double newX = centerX + (oldX - centerX) * scale + offsetX;
                double newY = centerY + (oldY - centerY) * scale + offsetY;

                // עדכון הרשימה המקורית של הפוליגון
                points.set(i, newX);
                points.set(i + 1, newY);
            }

            // עדכון המיקום של כפתור הצבא (הדיסקית)
            int newCx = (int)(centerX + (c.getX() - centerX) * scale) + offsetX;
            int newCy = (int)(centerY + (c.getY() - centerY) * scale) + offsetY;
            c.setX(newCx);
            c.setY(newCy);
        }
    }

    // ================================================================
    // צפון אמריקה - High Definition
    // ================================================================
    private void buildNorthAmerica(Continent na) {
        addC(na, 1, "Alaska", 80, 100,
                new int[] { 30, 90, 130, 130, 110, 80, 50, 40, 30 },
                new int[] { 50, 40, 60, 130, 150, 130, 160, 200, 150 });
        addC(na, 2, "NW Territory", 210, 90,
                new int[] { 130, 160, 220, 280, 310, 310, 260, 170, 130 },
                new int[] { 60, 40, 40, 60, 90, 140, 150, 140, 130 });
        addC(na, 3, "Greenland", 370, 70,
                new int[] { 320, 370, 430, 440, 410, 350, 310 },
                new int[] { 20, 10, 30, 90, 120, 100, 60 });
        addC(na, 4, "Alberta", 140, 180,
                new int[] { 110, 130, 170, 175, 190, 140, 100, 90 },
                new int[] { 150, 130, 140, 150, 220, 230, 220, 180 });
        addC(na, 5, "Ontario", 230, 190,
                new int[] { 170, 260, 290, 270, 220, 190 },
                new int[] { 140, 150, 180, 240, 250, 220 });
        addC(na, 6, "Quebec", 320, 180,
                new int[] { 260, 310, 350, 380, 370, 300, 290 },
                new int[] { 150, 140, 130, 170, 230, 220, 180 });
        addC(na, 7, "Western US", 140, 280,
                new int[] { 90, 100, 140, 190, 210, 190, 120, 80 },
                new int[] { 195, 220, 230, 220, 290, 320, 330, 280 });
        addC(na, 8, "Eastern US", 240, 300,
                new int[] { 190, 220, 270, 300, 330, 280, 240, 210 },
                new int[] { 220, 250, 240, 220, 280, 350, 330, 290 });
        addC(na, 9, "Central America", 170, 390,
                new int[] { 120, 210, 240, 280, 250, 180, 130 },
                new int[] { 330, 320, 360, 410, 450, 420, 380 });
    }

    private void buildSouthAmerica(Continent sa) {
        addC(sa, 10, "Venezuela", 230, 480,
                new int[] { 170, 180, 260, 300, 240 },
                new int[] { 500, 380, 410, 480, 520 });
        addC(sa, 11, "Peru", 190, 540,
                new int[] { 170, 240, 250, 190, 150 },
                new int[] { 500, 520, 570, 580, 540 });
        addC(sa, 12, "Brazil", 310, 540,
                new int[] { 300, 350, 380, 320, 250, 240 },
                new int[] { 480, 490, 540, 610, 570, 520 });
        addC(sa, 13, "Argentina", 240, 640,
                new int[] { 190, 250, 320, 270, 210, 180, 200 },
                new int[] { 580, 570, 610, 670, 750, 720, 640 });
    }

    private void buildEurope(Continent eu) {
        addC(eu, 14, "Iceland", 420, 90,
                new int[] { 380, 435, 460, 440, 400 },
                new int[] { 80, 60, 120, 130, 130 });
        addC(eu, 15, "Scandinavia", 510, 110,
                new int[] { 480, 520, 550, 560, 550, 500, 480 },
                new int[] { 60, 50, 60, 110, 160, 160, 110 });
        addC(eu, 16, "Ukraine", 630, 170,
                new int[] { 560, 610, 670, 710, 700, 630, 570, 550 },
                new int[] { 90, 80, 90, 140, 250, 260, 230, 160 });
        addC(eu, 17, "Great Britain", 430, 190,
                new int[] { 410, 440, 460, 450, 420, 400 },
                new int[] { 160, 150, 170, 220, 230, 200 });
        addC(eu, 18, "W. Europe", 440, 280,
                new int[] { 400, 430, 495, 490, 460, 410 },
                new int[] { 250, 230, 240, 290, 335, 335 });
        addC(eu, 19, "N. Europe", 510, 200,
                new int[] { 480, 550, 560, 570, 520, 480 },
                new int[] { 160, 160, 190, 230, 250, 240 });
        addC(eu, 20, "S. Europe", 530, 280,
                new int[] { 490, 520, 570, 610, 580, 530, 490 },
                new int[] { 250, 250, 230, 270, 320, 330, 290 });
    }

    private void buildAfrica(Continent af) {
        addC(af, 21, "North Africa", 450, 380,
                new int[] { 380, 440, 510, 530, 550,500, 350, 380 },
                new int[] { 320, 320, 330, 390, 480,460, 520, 390 });
        addC(af, 22, "Egypt", 550, 370,
                new int[] { 510, 570, 600, 610, 560, 550 },
                new int[] { 330, 320, 340, 400, 420, 480 });
        addC(af, 23, "East Africa", 610, 460,
                new int[] { 560, 610, 650, 680, 660, 610, 550 },
                new int[] { 420, 400, 420, 480, 530, 540, 535 });
        addC(af, 24, "Congo", 510, 500,
                new int[] { 350, 500, 550, 550, 520, 460 },
                new int[] { 520, 460, 480, 540, 570, 520 });
        addC(af, 25, "South Africa", 550, 610,
                new int[] { 460, 520, 550, 610, 610, 560, 500 },
                new int[] { 520, 570, 540, 540, 600, 680, 630 });
        addC(af, 26, "Madagascar", 670, 620,
                new int[] { 650, 670, 690, 680, 660, 640 },
                new int[] { 570, 560, 610, 670, 680, 620 });
    }

    private void buildAsia(Continent as) {
        addC(as, 27, "Ural", 740, 150,
                new int[] { 710, 750, 780, 780, 740, 700 },
                new int[] { 90, 80, 100, 225, 210, 190 });
        addC(as, 28, "Siberia", 820, 140,
                new int[] { 780, 830, 870, 880, 830, 790 },
                new int[] { 100, 90, 100, 180, 200, 310 });
        addC(as, 29, "Yakutsk", 910, 120,
                new int[] { 870, 920, 960, 950, 900, 880 },
                new int[] { 100, 80, 100, 160, 170, 180 });
        addC(as, 30, "Kamchatka", 990, 130,
                new int[] { 960, 1000, 1040, 1020, 980, 950 },
                new int[] { 100, 90, 130, 200, 220, 160 });
        addC(as, 31, "Irkutsk", 860, 210,
                new int[] { 830, 880, 950, 930, 860, 840 },
                new int[] { 200, 180, 160, 230, 260, 225 });
        addC(as, 32, "Mongolia", 920, 270,
                new int[] { 860, 930, 970, 950, 880, 865 },
                new int[] { 260, 230, 250, 310, 320, 290 });
        addC(as, 33, "Japan", 1040, 230,
                new int[] { 1020, 1050, 1070, 1050, 1020 },
                new int[] { 180, 170, 220, 290, 270 });
        addC(as, 34, "Afghanistan", 740, 260,
                new int[] { 700, 740, 790, 790, 730, 680 },
                new int[] { 190, 210, 230, 310, 320, 280 });
        addC(as, 35, "China", 870, 360,
                new int[] { 790, 830, 880, 950, 940, 880, 830 },
                new int[] { 310, 200, 320, 310, 400, 430, 390 });
        addC(as, 36, "Middle East", 660, 350,
                new int[] { 610, 680, 730, 710, 660, 610 },
                new int[] { 270, 280, 320, 410, 430, 390 });
        addC(as, 37, "India", 760, 410,
                new int[] { 730, 790, 830, 790, 740 },
                new int[] { 320, 310, 380, 490, 440 });
        addC(as, 38, "SE Asia", 880, 450,
                new int[] { 830, 880, 940, 930, 890, 850 },
                new int[] { 390, 430, 400, 480, 520, 460 });
    }

    private void buildAustralia(Continent au) {
        addC(au, 39, "Indonesia", 880, 560,
                new int[] { 830, 870, 930, 950, 910, 840 },
                new int[] { 520, 500, 530, 580, 610, 580 });
        addC(au, 40, "New Guinea", 980, 530,
                new int[] { 950, 1000, 1030, 1010, 960 },
                new int[] { 500, 490, 540, 580, 560 });
        addC(au, 41, "W. Australia", 890, 670,
                new int[] { 830, 880, 930, 930, 890, 840 },
                new int[] { 620, 600, 630, 720, 740, 690 });
        addC(au, 42, "E. Australia", 980, 670,
                new int[] { 930, 980, 1030, 1010, 950, 930 },
                new int[] { 630, 610, 650, 730, 740, 720 });
    }

    private void buildAllConnections() {
        // חיבורי יבשות (נשאר ללא שינוי)
        connect(1, 2); connect(1, 4); connect(1, 30); connect(2, 3); connect(2, 4);
        connect(2, 5); connect(3, 6); connect(3, 14); connect(4, 5); connect(4, 7);
        connect(5, 6); connect(5, 7); connect(5, 8); connect(6, 8); connect(7, 8);
        connect(7, 9); connect(8, 9); connect(9, 10);

        connect(10, 11); connect(10, 12); connect(11, 12); connect(11, 13);
        connect(12, 13); connect(12, 21);

        connect(14, 15); connect(14, 17); connect(15, 16); connect(15, 17);
        connect(15, 19); connect(16, 19); connect(16, 20); connect(16, 27);
        connect(16, 34); connect(16, 36); connect(17, 18); connect(17, 19);
        connect(18, 19); connect(18, 20); connect(18, 21); connect(19, 20);
        connect(20, 21); connect(20, 22); connect(20, 36);

        connect(21, 22); connect(21, 23); connect(21, 24); connect(22, 23);
        connect(22, 36); connect(23, 24); connect(23, 25); connect(23, 26);
        connect(23, 36); connect(24, 25); connect(25, 26);

        connect(27, 28); connect(27, 31); connect(27, 34); connect(28, 29);
        connect(28, 31); connect(29, 30); connect(29, 31); connect(30, 31);
        connect(30, 32); connect(30, 33); connect(31, 32); connect(32, 33);
        connect(32, 35); connect(34, 35); connect(34, 36); connect(34, 37);
        connect(35, 37); connect(35, 38); connect(36, 37); connect(37, 38);
        connect(38, 39);

        connect(39, 40); connect(39, 41); connect(40, 41); connect(40, 42);
        connect(41, 42);
    }

    // הפונקציה הממירה את מערכי ה-int לפוליגון של JavaFX
    private void addC(Continent cont, int id, String name, int x, int y, int[] px, int[] py) {
        Country c = new Country(id, name, x, y);

        double[] points = new double[px.length * 2];
        for (int i = 0; i < px.length; i++) {
            points[i * 2] = px[i];
            points[i * 2 + 1] = py[i];
        }

        c.setShape(new Polygon(points));
        c.setContinent(cont);
        countries.put(id, c);
        cont.addCountry(c);
    }

    private void connect(int a, int b) {
        Country c1 = countries.get(a), c2 = countries.get(b);
        if (c1 != null && c2 != null) {
            c1.addNeighbor(c2);
            c2.addNeighbor(c1);
        }
    }

    public int calculateContinentBonus(Player p) {
        int t = 0;
        for (Continent c : continents)
            if (c.isOwnedBy(p))
                t += c.getBonusValue();
        return t;
    }

    public Country getCountry(int id) {
        return countries.get(id);
    }

    public java.util.Collection<Country> getCountries() {
        return countries.values();
    }
}