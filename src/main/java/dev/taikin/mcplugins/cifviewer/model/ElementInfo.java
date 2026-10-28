package dev.taikin.mcplugins.cifviewer.model;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class ElementInfo {

    private static final Set<String> ELEMENTS = new HashSet<>();
    private static final Map<String, int[]> COLORS = new HashMap<>();
    private static final Map<String, Double> RADII = new HashMap<>();
    private static final Map<String, Double> VDW_RADII = new HashMap<>();
    private static final Map<String, Double> IONIC_RADII = new HashMap<>();

    static {
        String symbols = "H He Li Be B C N O F Ne Na Mg Al Si P S Cl Ar K Ca Sc Ti V Cr Mn Fe Co Ni Cu Zn "
                + "Ga Ge As Se Br Kr Rb Sr Y Zr Nb Mo Tc Ru Rh Pd Ag Cd In Sn Sb Te I Xe Cs Ba La Ce Pr Nd "
                + "Pm Sm Eu Gd Tb Dy Ho Er Tm Yb Lu Hf Ta W Re Os Ir Pt Au Hg Tl Pb Bi Po At Rn Fr Ra Ac Th Pa "
                + "U Np Pu Am Cm Bk Cf Es Fm Md No Lr Rf Db Sg Bh Hs Mt Ds Rg Cn Nh Fl Mc Lv Ts Og";
        for (String symbol : symbols.split(" ")) {
            ELEMENTS.add(symbol);
        }

        color("H", 255, 255, 255);
        color("He", 217, 255, 255);
        color("Li", 204, 128, 255);
        color("Be", 194, 255, 0);
        color("B", 255, 181, 181);
        color("C", 110, 110, 110);
        color("N", 48, 80, 248);
        color("O", 255, 13, 13);
        color("F", 144, 224, 80);
        color("Ne", 179, 227, 245);
        color("Na", 171, 92, 242);
        color("Mg", 138, 255, 0);
        color("Al", 191, 166, 166);
        color("Si", 240, 200, 160);
        color("P", 255, 128, 0);
        color("S", 255, 200, 50);
        color("Cl", 31, 206, 31);
        color("Ar", 128, 209, 227);
        color("K", 143, 64, 212);
        color("Ca", 61, 255, 0);
        color("Sc", 230, 230, 230);
        color("Ti", 191, 194, 199);
        color("V", 166, 166, 171);
        color("Cr", 138, 153, 199);
        color("Mn", 156, 122, 199);
        color("Fe", 224, 102, 51);
        color("Co", 240, 144, 160);
        color("Ni", 80, 208, 80);
        color("Cu", 200, 128, 51);
        color("Zn", 125, 128, 176);
        color("Ga", 194, 143, 143);
        color("Ge", 102, 143, 143);
        color("As", 189, 128, 227);
        color("Se", 255, 161, 0);
        color("Br", 166, 41, 41);
        color("Kr", 92, 184, 209);
        color("Rb", 112, 46, 176);
        color("Sr", 0, 255, 0);
        color("Y", 148, 255, 255);
        color("Zr", 148, 224, 224);
        color("Nb", 115, 115, 225);
        color("Mo", 84, 181, 181);
        color("Tc", 0, 212, 212);
        color("Ru", 36, 143, 143);
        color("Rh", 10, 125, 140);
        color("Pd", 0, 105, 133);
        color("Ag", 192, 192, 192);
        color("Cd", 255, 217, 143);
        color("In", 166, 117, 115);
        color("Sn", 102, 128, 128);
        color("Sb", 158, 99, 181);
        color("Te", 212, 122, 0);
        color("I", 148, 0, 148);
        color("Xe", 66, 158, 176);
        color("Cs", 87, 23, 143);
        color("Ba", 0, 201, 0);
        color("La", 112, 212, 255);
        color("Ce", 255, 255, 199);
        color("Pr", 217, 255, 199);
        color("Nd", 199, 255, 199);
        color("Pm", 163, 255, 199);
        color("Sm", 143, 255, 199);
        color("Eu", 97, 255, 199);
        color("Gd", 69, 255, 199);
        color("Tb", 48, 255, 199);
        color("Dy", 31, 255, 199);
        color("Ho", 0, 255, 156);
        color("Er", 0, 230, 117);
        color("Tm", 0, 212, 82);
        color("Yb", 0, 191, 56);
        color("Lu", 0, 171, 36);
        color("Hf", 77, 194, 255);
        color("Ta", 77, 166, 255);
        color("W", 33, 148, 214);
        color("Re", 38, 125, 171);
        color("Os", 38, 102, 150);
        color("Ir", 23, 84, 135);
        color("Pt", 208, 208, 224);
        color("Au", 255, 209, 35);
        color("Hg", 184, 184, 208);
        color("Tl", 166, 84, 77);
        color("Pb", 87, 89, 97);
        color("Bi", 158, 79, 181);
        color("Po", 171, 92, 0);
        color("At", 117, 79, 69);
        color("Rn", 66, 130, 150);
        color("Fr", 66, 0, 102);
        color("Ra", 0, 125, 0);
        color("Ac", 112, 171, 250);
        color("Th", 0, 186, 255);
        color("Pa", 0, 161, 255);
        color("U", 0, 143, 255);
        color("Np", 0, 128, 255);
        color("Pu", 0, 107, 255);
        color("Am", 61, 89, 171);
        color("Cm", 0, 84, 153);

        radius("H", 0.31);
        radius("He", 0.28);
        radius("Li", 1.28);
        radius("Be", 0.96);
        radius("B", 0.84);
        radius("C", 0.76);
        radius("N", 0.71);
        radius("O", 0.66);
        radius("F", 0.57);
        radius("Ne", 0.58);
        radius("Na", 1.66);
        radius("Mg", 1.41);
        radius("Al", 1.21);
        radius("Si", 1.11);
        radius("P", 1.07);
        radius("S", 1.05);
        radius("Cl", 1.02);
        radius("Ar", 1.06);
        radius("K", 2.03);
        radius("Ca", 1.76);
        radius("Sc", 1.70);
        radius("Ti", 1.60);
        radius("V", 1.53);
        radius("Cr", 1.39);
        radius("Mn", 1.39);
        radius("Fe", 1.32);
        radius("Co", 1.26);
        radius("Ni", 1.24);
        radius("Cu", 1.32);
        radius("Zn", 1.22);
        radius("Ga", 1.22);
        radius("Ge", 1.20);
        radius("As", 1.19);
        radius("Se", 1.20);
        radius("Br", 1.20);
        radius("Kr", 1.16);
        radius("Rb", 2.20);
        radius("Sr", 1.95);
        radius("Y", 1.90);
        radius("Zr", 1.75);
        radius("Nb", 1.64);
        radius("Mo", 1.54);
        radius("Tc", 1.47);
        radius("Ru", 1.46);
        radius("Rh", 1.42);
        radius("Pd", 1.39);
        radius("Ag", 1.45);
        radius("Cd", 1.44);
        radius("In", 1.42);
        radius("Sn", 1.39);
        radius("Sb", 1.39);
        radius("Te", 1.38);
        radius("I", 1.39);
        radius("Xe", 1.40);
        radius("Cs", 2.44);
        radius("Ba", 2.15);
        radius("La", 2.07);
        radius("Ce", 2.04);
        radius("Pr", 2.03);
        radius("Nd", 2.01);
        radius("Pm", 1.99);
        radius("Sm", 1.98);
        radius("Eu", 1.98);
        radius("Gd", 1.96);
        radius("Tb", 1.94);
        radius("Dy", 1.92);
        radius("Ho", 1.92);
        radius("Er", 1.89);
        radius("Tm", 1.90);
        radius("Yb", 1.87);
        radius("Lu", 1.87);
        radius("Hf", 1.75);
        radius("Ta", 1.70);
        radius("W", 1.62);
        radius("Re", 1.51);
        radius("Os", 1.44);
        radius("Ir", 1.41);
        radius("Pt", 1.36);
        radius("Au", 1.36);
        radius("Hg", 1.32);
        radius("Tl", 1.45);
        radius("Pb", 1.46);
        radius("Bi", 1.48);
        radius("Po", 1.40);
        radius("At", 1.50);
        radius("Rn", 1.50);
        radius("Fr", 2.60);
        radius("Ra", 2.21);
        radius("Ac", 2.15);
        radius("Th", 2.06);
        radius("Pa", 2.00);
        radius("U", 1.96);
        radius("Np", 1.90);
        radius("Pu", 1.87);
        radius("Am", 1.80);
        radius("Cm", 1.69);

        vdw("H", 1.20);
        vdw("He", 1.40);
        vdw("Li", 1.82);
        vdw("Be", 1.53);
        vdw("B", 1.92);
        vdw("C", 1.70);
        vdw("N", 1.55);
        vdw("O", 1.52);
        vdw("F", 1.47);
        vdw("Ne", 1.54);
        vdw("Na", 2.27);
        vdw("Mg", 1.73);
        vdw("Al", 1.84);
        vdw("Si", 2.10);
        vdw("P", 1.80);
        vdw("S", 1.80);
        vdw("Cl", 1.75);
        vdw("Ar", 1.88);
        vdw("K", 2.75);
        vdw("Ca", 2.31);
        vdw("Sc", 2.15);
        vdw("Ti", 2.11);
        vdw("V", 2.07);
        vdw("Cr", 2.06);
        vdw("Mn", 2.05);
        vdw("Fe", 2.04);
        vdw("Co", 2.00);
        vdw("Ni", 1.97);
        vdw("Cu", 1.96);
        vdw("Zn", 2.01);
        vdw("Ga", 1.87);
        vdw("Ge", 2.11);
        vdw("As", 1.85);
        vdw("Se", 1.90);
        vdw("Br", 1.85);
        vdw("Kr", 2.02);
        vdw("Rb", 3.03);
        vdw("Sr", 2.49);
        vdw("Y", 2.32);
        vdw("Zr", 2.23);
        vdw("Nb", 2.18);
        vdw("Mo", 2.17);
        vdw("Tc", 2.16);
        vdw("Ru", 2.13);
        vdw("Rh", 2.10);
        vdw("Pd", 2.05);
        vdw("Ag", 2.03);
        vdw("Cd", 2.18);
        vdw("In", 1.93);
        vdw("Sn", 2.17);
        vdw("Sb", 2.06);
        vdw("Te", 2.06);
        vdw("I", 1.98);
        vdw("Xe", 2.16);
        vdw("Cs", 3.43);
        vdw("Ba", 2.68);
        vdw("La", 2.43);
        vdw("Ce", 2.42);
        vdw("Pr", 2.40);
        vdw("Nd", 2.39);
        vdw("Pm", 2.38);
        vdw("Sm", 2.36);
        vdw("Eu", 2.35);
        vdw("Gd", 2.34);
        vdw("Tb", 2.33);
        vdw("Dy", 2.31);
        vdw("Ho", 2.30);
        vdw("Er", 2.29);
        vdw("Tm", 2.27);
        vdw("Yb", 2.26);
        vdw("Lu", 2.24);
        vdw("Hf", 2.23);
        vdw("Ta", 2.22);
        vdw("W", 2.21);
        vdw("Re", 2.20);
        vdw("Os", 2.19);
        vdw("Ir", 2.18);
        vdw("Pt", 2.13);
        vdw("Au", 2.14);
        vdw("Hg", 2.23);
        vdw("Tl", 1.96);
        vdw("Pb", 2.02);
        vdw("Bi", 2.07);
        vdw("Po", 1.97);
        vdw("At", 2.02);
        vdw("Rn", 2.20);
        vdw("Fr", 3.48);
        vdw("Ra", 2.83);
        vdw("Ac", 2.60);
        vdw("Th", 2.37);
        vdw("Pa", 2.43);
        vdw("U", 2.41);
        vdw("Np", 2.39);
        vdw("Pu", 2.43);
        vdw("Am", 2.44);
        vdw("Cm", 2.45);

        ionicDefault("H", 1.20);
        ionicDefault("Li", 0.76);
        ionicDefault("Na", 1.02);
        ionicDefault("K", 1.38);
        ionicDefault("Rb", 1.52);
        ionicDefault("Cs", 1.67);
        ionicDefault("Be", 0.45);
        ionicDefault("Mg", 0.72);
        ionicDefault("Ca", 1.00);
        ionicDefault("Sr", 1.18);
        ionicDefault("Ba", 1.35);
        ionicDefault("B", 0.27);
        ionicDefault("Al", 0.535);
        ionicDefault("Ga", 0.62);
        ionicDefault("In", 0.80);
        ionicDefault("Tl", 1.50);
        ionic("Tl", 3, 0.885);
        ionic("C", 4, 0.16);
        ionic("C", -4, 2.60);
        ionicDefault("N", 1.46);
        ionic("N", 5, 0.13);
        ionicDefault("O", 1.40);
        ionic("O", -1, 1.76);
        ionicDefault("F", 1.33);
        ionicDefault("Si", 0.40);
        ionicDefault("P", 0.38);
        ionic("P", -3, 2.12);
        ionicDefault("S", 1.84);
        ionic("S", 6, 0.29);
        ionicDefault("Cl", 1.81);
        ionicDefault("Ge", 0.53);
        ionic("Ge", 2, 0.73);
        ionicDefault("As", 0.58);
        ionic("As", -3, 2.22);
        ionic("As", 5, 0.46);
        ionicDefault("Se", 1.98);
        ionic("Se", 4, 0.50);
        ionic("Se", 6, 0.42);
        ionicDefault("Br", 1.96);
        ionicDefault("Te", 2.21);
        ionic("Te", 4, 0.97);
        ionic("Te", 6, 0.56);
        ionicDefault("I", 2.20);
        ionicDefault("Sc", 0.745);
        ionicDefault("Ti", 0.605);
        ionic("Ti", 2, 0.86);
        ionic("Ti", 3, 0.67);
        ionicDefault("V", 0.64);
        ionic("V", 2, 0.79);
        ionic("V", 4, 0.58);
        ionic("V", 5, 0.54);
        ionicDefault("Cr", 0.615);
        ionic("Cr", 2, 0.80);
        ionic("Cr", 6, 0.44);
        ionicDefault("Mn", 0.83);
        ionic("Mn", 3, 0.645);
        ionic("Mn", 4, 0.53);
        ionic("Mn", 7, 0.46);
        ionicDefault("Fe", 0.78);
        ionic("Fe", 3, 0.645);
        ionicDefault("Co", 0.745);
        ionic("Co", 3, 0.545);
        ionicDefault("Ni", 0.69);
        ionic("Ni", 3, 0.56);
        ionicDefault("Cu", 0.73);
        ionic("Cu", 1, 0.77);
        ionicDefault("Zn", 0.74);
        ionicDefault("Y", 0.90);
        ionicDefault("Zr", 0.72);
        ionicDefault("Nb", 0.64);
        ionicDefault("Mo", 0.59);
        ionic("Mo", 4, 0.65);
        ionic("Tc", 4, 0.645);
        ionic("Tc", 7, 0.56);
        ionic("Ru", 3, 0.68);
        ionic("Ru", 4, 0.62);
        ionic("Rh", 3, 0.665);
        ionic("Pd", 2, 0.86);
        ionic("Pd", 4, 0.615);
        ionicDefault("Ag", 1.15);
        ionicDefault("Cd", 0.95);
        ionicDefault("Sn", 0.69);
        ionic("Sn", 2, 1.18);
        ionicDefault("Sb", 0.76);
        ionic("Sb", 5, 0.60);
        ionic("Sb", -3, 2.45);
        ionicDefault("La", 1.032);
        ionicDefault("Ce", 1.01);
        ionic("Ce", 4, 0.87);
        ionicDefault("Pr", 0.99);
        ionicDefault("Nd", 0.983);
        ionicDefault("Pm", 0.97);
        ionicDefault("Sm", 0.958);
        ionicDefault("Eu", 0.947);
        ionic("Eu", 2, 1.17);
        ionicDefault("Gd", 0.938);
        ionicDefault("Tb", 0.923);
        ionic("Tb", 4, 0.76);
        ionicDefault("Dy", 0.912);
        ionicDefault("Ho", 0.901);
        ionicDefault("Er", 0.890);
        ionicDefault("Tm", 0.880);
        ionicDefault("Yb", 0.868);
        ionic("Yb", 2, 1.02);
        ionicDefault("Lu", 0.861);
        ionicDefault("Hf", 0.71);
        ionicDefault("Ta", 0.64);
        ionicDefault("W", 0.60);
        ionic("Re", 4, 0.63);
        ionic("Re", 7, 0.53);
        ionic("Os", 4, 0.63);
        ionic("Ir", 3, 0.68);
        ionic("Ir", 4, 0.625);
        ionic("Pt", 2, 0.80);
        ionic("Pt", 4, 0.625);
        ionicDefault("Au", 0.85);
        ionic("Au", 1, 1.37);
        ionicDefault("Hg", 1.02);
        ionic("Hg", 1, 1.19);
        ionicDefault("Pb", 1.19);
        ionic("Pb", 4, 0.775);
        ionicDefault("Bi", 1.03);
        ionic("Bi", 5, 0.76);
        ionicDefault("Po", 0.94);
        ionicDefault("At", 2.20);
        ionicDefault("Fr", 1.80);
        ionicDefault("Ra", 1.48);
        ionicDefault("Ac", 1.12);
        ionicDefault("Th", 0.94);
        ionicDefault("Pa", 0.78);
        ionicDefault("U", 0.89);
        ionic("U", 6, 0.73);
        ionicDefault("Np", 0.87);
        ionic("Np", 3, 1.01);
        ionicDefault("Pu", 0.86);
        ionic("Pu", 3, 1.00);
        ionicDefault("Am", 0.975);
        ionicDefault("Cm", 0.97);
    }

    private ElementInfo() {
    }

    public static String elementOf(String typeSymbol, String label) {
        String fromType = leadingElement(typeSymbol);
        if (fromType != null) {
            return fromType;
        }
        return leadingElement(label);
    }

    public static String leadingElement(String value) {
        if (value == null) {
            return null;
        }
        String text = value.trim();
        int start = 0;
        while (start < text.length() && !Character.isLetter(text.charAt(start))) {
            start++;
        }
        if (start >= text.length()) {
            return null;
        }
        if (text.length() >= start + 2) {
            char first = Character.toUpperCase(text.charAt(start));
            char second = text.charAt(start + 1);
            if (Character.isLetter(second)) {
                String two = first + String.valueOf(Character.toLowerCase(second));
                if (ELEMENTS.contains(two)) {
                    return two;
                }
            }
        }
        char first = Character.toUpperCase(text.charAt(start));
        if (first == 'D' || first == 'T') {
            return "H";
        }
        String one = String.valueOf(first);
        return ELEMENTS.contains(one) ? one : null;
    }

    public static int[] color(String element) {
        int[] color = COLORS.get(element);
        if (color != null) {
            return color;
        }
        double hue = Math.floorMod(element.toLowerCase(Locale.ROOT).hashCode(), 360);
        return hslToRgb(hue / 360.0, 0.55, 0.62);
    }

    public static double radius(String element) {
        return RADII.getOrDefault(element, 1.40);
    }

    public static double vanDerWaalsRadius(String element) {
        return VDW_RADII.getOrDefault(element, radius(element) * 1.5);
    }

    public static double ionicRadius(String element, int charge) {
        if (charge != 0) {
            Double exact = IONIC_RADII.get(element + ":" + charge);
            if (exact != null) {
                return exact;
            }
        }
        Double common = IONIC_RADII.get(element);
        return common != null ? common : radius(element);
    }

    public static double ionicRadius(String element) {
        return ionicRadius(element, 0);
    }

    public static int oxidationState(String value) {
        if (value == null) {
            return 0;
        }
        String text = value.trim();
        int start = 0;
        while (start < text.length() && !Character.isLetter(text.charAt(start))) {
            start++;
        }
        String element = leadingElement(text);
        if (element == null) {
            return 0;
        }
        int index = start + element.length();
        int digitStart = index;
        while (index < text.length() && Character.isDigit(text.charAt(index))) {
            index++;
        }
        String digits = text.substring(digitStart, index);
        int sign = 0;
        if (index < text.length()) {
            char c = text.charAt(index);
            if (c == '+') {
                sign = 1;
                index++;
            } else if (c == '-') {
                sign = -1;
                index++;
            }
        }
        if (sign == 0) {
            return 0;
        }
        if (digits.isEmpty()) {
            int suffixStart = index;
            while (index < text.length() && Character.isDigit(text.charAt(index))) {
                index++;
            }
            digits = text.substring(suffixStart, index);
        }
        return digits.isEmpty() ? sign : sign * Integer.parseInt(digits);
    }

    public static boolean isElement(String symbol) {
        return ELEMENTS.contains(symbol);
    }

    private static void color(String element, int r, int g, int b) {
        COLORS.put(element, new int[] {r, g, b});
    }

    private static void radius(String element, double radius) {
        RADII.put(element, radius);
    }

    private static void vdw(String element, double radius) {
        VDW_RADII.put(element, radius);
    }

    private static void ionicDefault(String element, double radius) {
        IONIC_RADII.put(element, radius);
    }

    private static void ionic(String element, int charge, double radius) {
        IONIC_RADII.put(element + ":" + charge, radius);
    }

    private static int[] hslToRgb(double h, double s, double l) {
        double c = (1.0 - Math.abs(2.0 * l - 1.0)) * s;
        double x = c * (1.0 - Math.abs((h * 6.0) % 2.0 - 1.0));
        double m = l - c / 2.0;
        double r;
        double g;
        double b;
        if (h < 1.0 / 6.0) {
            r = c;
            g = x;
            b = 0;
        } else if (h < 2.0 / 6.0) {
            r = x;
            g = c;
            b = 0;
        } else if (h < 3.0 / 6.0) {
            r = 0;
            g = c;
            b = x;
        } else if (h < 4.0 / 6.0) {
            r = 0;
            g = x;
            b = c;
        } else if (h < 5.0 / 6.0) {
            r = x;
            g = 0;
            b = c;
        } else {
            r = c;
            g = 0;
            b = x;
        }
        return new int[] {
            (int) Math.round((r + m) * 255.0),
            (int) Math.round((g + m) * 255.0),
            (int) Math.round((b + m) * 255.0)
        };
    }
}
