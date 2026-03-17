package Model;

import Model.Config.BoardConfig;
import Model.Config.ContinentConfig;
import Model.Config.CountryConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    // Helper map to quickly find continents by name when loading countries
    private final Map<String, Continent> continentMap = new HashMap<>();

    private final Map<String, String> rawSvgData = new HashMap<>();

    public Board() {
        loadSvgData();
        loadBoardFromJson(); // This single method replaces 150+ lines of hardcoded data!
    }

    private void loadSvgData() {
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
            // Throwing an exception here halts the game if the map fails to load (Good Practice!)
            throw new RuntimeException("Fatal error: Could not load SVG data.", e);
        }
    }

    private void loadBoardFromJson() {
        try (InputStream is = getClass().getResourceAsStream("/board.json")) {
            if (is == null) {
                throw new RuntimeException("Cannot find board.json in Resources folder!");
            }

            ObjectMapper mapper = new ObjectMapper();
            BoardConfig config = mapper.readValue(is, BoardConfig.class);

            // 1. Create Continents
            for (ContinentConfig cConfig : config.getContinents()) {
                Continent continent = new Continent(
                        cConfig.getName(),
                        cConfig.getBonusValue(),
                        Color.web(cConfig.getColorHex())
                );
                continents.add(continent);
                continentMap.put(continent.getName(), continent);
            }

            // 2. Create Countries
            for (CountryConfig countryConfig : config.getCountries()) {
                Continent continent = continentMap.get(countryConfig.getContinentName());
                addCountryFromJson(continent, countryConfig);
            }

            // 3. Connect Neighbors
            for (CountryConfig countryConfig : config.getCountries()) {
                Country currentCountry = countries.get(countryConfig.getId());
                for (Integer neighborId : countryConfig.getNeighbors()) {
                    Country neighbor = countries.get(neighborId);
                    if (neighbor != null) {
                        currentCountry.addNeighbor(neighbor);
                    }
                }
            }

            // 4. Apply Adjustments
            for (ContinentConfig cConfig : config.getContinents()) {
                Continent continent = continentMap.get(cConfig.getName());
                adjustContinent(continent, cConfig.getScale(), cConfig.getOffsetX(), cConfig.getOffsetY());
            }

            // 5. Apply Global Scale
            applyGlobalScale(1.35, -250, -150);

        } catch (Exception e) {
            log.error("Failed to load board configuration from JSON", e);
            throw new RuntimeException("Fatal error: Could not load game board.", e);
        }
    }

    private void addCountryFromJson(Continent cont, CountryConfig config) {
        String pathString = rawSvgData.get(config.getSvgId());
        if (pathString == null) {
            log.warn("Could not find path for {} in SVG file.", config.getSvgId());
            pathString = "";
        }

        SVGPath shape = new SVGPath();
        shape.setContent(pathString);

        Bounds bounds = shape.getBoundsInLocal();
        int centerX = (int) (bounds.getMinX() + bounds.getWidth() / 2);
        int centerY = (int) (bounds.getMinY() + bounds.getHeight() / 2);

        Country c = new Country(config.getId(), config.getName(), centerX, centerY);
        c.setShape(shape);
        c.setContinent(cont);

        countries.put(c.getId(), c);
        cont.addCountry(c);
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

    private void adjustContinent(Continent cont, double scale, int offsetX, int offsetY) {
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