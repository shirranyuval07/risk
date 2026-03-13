package Model;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import javafx.scene.paint.Color;
import javafx.scene.shape.SVGPath;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;
import javafx.geometry.Bounds;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;
import java.util.*;

@Slf4j
public class Board {
    private final Map<Integer, Country> countries = new HashMap<>();
    @Getter
    private final List<Continent> continents = new ArrayList<>();

    private final Map<String, String> rawSvgData = new HashMap<>();

    public Board()
    {
        loadSvgData();
        initializeFullWorld();
    }

    private void loadSvgData()
    {
        try (InputStream is = getClass().getResourceAsStream("/Risk_board.svg")){
            if (is == null) {
                throw new RuntimeException("Cannot find Risk_board.svg in Resources folder!");
            }

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(is);
            doc.getDocumentElement().normalize();

            NodeList pathNodes = doc.getElementsByTagName("path");

            for (int i = 0; i < pathNodes.getLength(); i++) {
                Element pathElement = (Element) pathNodes.item(i);
                String id = pathElement.getAttribute("id");
                String d = pathElement.getAttribute("d");

                if (!id.isEmpty() && !d.isEmpty()) {
                    rawSvgData.put(id, d);
                }
            }
        } catch (Exception e) {
            log.error("Error loading SVG Data", e);
        }
    }

    private void initializeFullWorld()
    {
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

        applyGlobalScale(1.35, -250, -150);
    }

    private void applyGlobalScale(double scale, double offsetX, double offsetY) {
        for (Country c : countries.values()) {
            SVGPath p = (SVGPath) c.getShape();

            Scale scaleTransform = new Scale(scale, scale, 0, 0);
            Translate translateTransform = new Translate(offsetX, offsetY);

            p.getTransforms().clear();
            p.getTransforms().addAll(translateTransform, scaleTransform);

            Bounds localBounds = p.getBoundsInLocal();
            double originalCenterX = localBounds.getMinX() + (localBounds.getWidth() / 2.0);
            double originalCenterY = localBounds.getMinY() + (localBounds.getHeight() / 2.0);

            int newCx = (int) ((originalCenterX * scale) + offsetX);
            int newCy = (int) ((originalCenterY * scale) + offsetY);

            c.setX(newCx);
            c.setY(newCy);
        }
    }

    private void applyContinentAdjustments() {
        adjustContinent(continents.get(0), 0.82, -40, -30);
        adjustContinent(continents.get(1), 0.85, -40, -45);
        adjustContinent(continents.get(2), 0.85, 0, -5);
        adjustContinent(continents.get(3), 0.85, 15, -35);
        adjustContinent(continents.get(4), 0.85, -35, -20);
        adjustContinent(continents.get(5), 0.85, 35, -20);
    }

    private void adjustContinent(Continent cont, double scale, int offsetX, int offsetY)
    {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minY = Double.MAX_VALUE, maxY = -Double.MAX_VALUE;

        for (Country c : cont.getCountries()) {
            SVGPath p = (SVGPath) c.getShape();
            Bounds b = p.getBoundsInLocal();
            if (b.getMinX() < minX) minX = b.getMinX();
            if (b.getMaxX() > maxX) maxX = b.getMaxX();
            if (b.getMinY() < minY) minY = b.getMinY();
            if (b.getMaxY() > maxY) maxY = b.getMaxY();
        }

        double centerX = (minX + maxX) / 2.0;
        double centerY = (minY + maxY) / 2.0;

        for (Country c : cont.getCountries()) {
            SVGPath p = (SVGPath) c.getShape();

            Scale scaleTransform = new Scale(scale, scale, centerX, centerY);
            Translate translateTransform = new Translate(offsetX, offsetY);
            p.getTransforms().addAll(translateTransform, scaleTransform);

            int newCx = (int)(centerX + (c.getX() - centerX) * scale) + offsetX;
            int newCy = (int)(centerY + (c.getY() - centerY) * scale) + offsetY;
            c.setX(newCx);
            c.setY(newCy);
        }
    }

    private void buildNorthAmerica(Continent na)
    {
        addC(na, 1, "Alaska", "alaska");
        addC(na, 2, "NW Territory", "northwest_territory");
        addC(na, 3, "Greenland", "greenland");
        addC(na, 4, "Alberta", "alberta");
        addC(na, 5, "Ontario", "ontario");
        addC(na, 6, "Quebec", "quebec");
        addC(na, 7, "Western US", "western_united_states");
        addC(na, 8, "Eastern US", "eastern_united_states");
        addC(na, 9, "Central America", "central_america");
    }

    private void buildSouthAmerica(Continent sa)
    {
        addC(sa, 10, "Venezuela", "venezuela");
        addC(sa, 11, "Peru", "peru");
        addC(sa, 12, "Brazil", "brazil");
        addC(sa, 13, "Argentina", "argentina");
    }

    private void buildEurope(Continent eu)
    {
        addC(eu, 14, "Iceland", "iceland");
        addC(eu, 15, "Scandinavia", "scandinavia");
        addC(eu, 16, "Ukraine", "ukraine");
        addC(eu, 17, "Great Britain", "great_britain");
        addC(eu, 18, "W. Europe", "western_europe");
        addC(eu, 19, "N. Europe", "northern_europe");
        addC(eu, 20, "S. Europe", "southern_europe");
    }

    private void buildAfrica(Continent af)
    {
        addC(af, 21, "North Africa", "north_africa");
        addC(af, 22, "Egypt", "egypt");
        addC(af, 23, "East Africa", "east_africa");
        addC(af, 24, "Congo", "congo");
        addC(af, 25, "South Africa", "south_africa");
        addC(af, 26, "Madagascar", "madagascar");
    }

    private void buildAsia(Continent as)
    {
        addC(as, 27, "Ural", "ural");
        addC(as, 28, "Siberia", "siberia");
        addC(as, 29, "Yakutsk", "yakursk");
        addC(as, 30, "Kamchatka", "kamchatka");
        addC(as, 31, "Irkutsk", "irkutsk");
        addC(as, 32, "Mongolia", "mongolia");
        addC(as, 33, "Japan", "japan");
        addC(as, 34, "Afghanistan", "afghanistan");
        addC(as, 35, "China", "china");
        addC(as, 36, "Middle East", "middle_east");
        addC(as, 37, "India", "india");
        addC(as, 38, "SE Asia", "siam");
    }

    private void buildAustralia(Continent au)
    {
        addC(au, 39, "Indonesia", "indonesia");
        addC(au, 40, "New Guinea", "new_guinea");
        addC(au, 41, "W. Australia", "western_australia");
        addC(au, 42, "E. Australia", "eastern_australia");
    }

    private void buildAllConnections()
    {
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

    private void addC(Continent cont, int id, String name, String svgId)
    {
        String pathString = rawSvgData.get(svgId);
        if (pathString == null) {
            log.warn("Could not find path for {} in SVG file.", svgId);
            pathString = "";
        }

        SVGPath shape = new SVGPath();
        shape.setContent(pathString);

        Bounds bounds = shape.getBoundsInLocal();
        int centerX = (int) (bounds.getMinX() + bounds.getWidth() / 2);
        int centerY = (int) (bounds.getMinY() + bounds.getHeight() / 2);

        Country c = new Country(id, name, centerX, centerY);
        c.setShape(shape);
        c.setContinent(cont);

        countries.put(id, c);
        cont.addCountry(c);
    }

    private void connect(int a, int b)
    {
        Country c1 = countries.get(a), c2 = countries.get(b);
        if (c1 != null && c2 != null) {
            c1.addNeighbor(c2);
            c2.addNeighbor(c1);
        }
    }

    public int calculateContinentBonus(Player p)
    {
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