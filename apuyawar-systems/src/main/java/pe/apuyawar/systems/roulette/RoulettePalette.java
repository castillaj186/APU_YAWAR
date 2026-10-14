package pe.apuyawar.systems.roulette;

import java.util.Locale;
/** Orden angular del disco, en sentido horario desde las 12:00. */
public final class RoulettePalette {
    private RoulettePalette() {}
    public static final String[] NAMES = {"rojo", "rosa", "morado", "azul", "turquesa", "verde", "amarillo", "naranja"};
    public static final int[] RGB = {0xE83233, 0xF052AF, 0x8139CE, 0x2780E4, 0x22C6CE, 0x37B752, 0xFFD235, 0xEF8C27};
    public static int indexOf(String input) {
        String color = input.toLowerCase(Locale.ROOT).trim();
        if (color.equals("roja")) color = "rojo";
        if (color.equals("rosada")) color = "rosa";
        if (color.equals("morada")) color = "morado";
        if (color.equals("turqueza") || color.equals("celeste")) color = "turquesa";
        if (color.equals("amarilla")) color = "amarillo";
        for (int i = 0; i < NAMES.length; i++) if (NAMES[i].equals(color)) return i;
        return -1;
    }
}
