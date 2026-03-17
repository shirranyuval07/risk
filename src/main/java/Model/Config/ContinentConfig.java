package Model.Config;

import lombok.Data;

@Data
public class ContinentConfig {
    private String name;
    private int bonusValue;
    private String colorHex;
    private double scale;
    private int offsetX;
    private int offsetY;
}