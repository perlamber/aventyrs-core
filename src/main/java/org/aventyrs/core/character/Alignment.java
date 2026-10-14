package org.aventyrs.core.character;

/**
 * The band of a character's 1–10 tendência de alinhamento, used by Talento acquisition
 * prerequisites. The number itself is what {@link Character#getAlignment()} stores; the band
 * is derived from it ({@link #of(int)}): 1–4 Maligno, 5–6 Neutro, 7–10 Bondoso.
 *
 * <pre>
 * 10, 09 - Bondoso: Samaritano
 * 08, 07 - Bondoso: Benevolente
 * 06, 05 - Neutro
 * 04, 03 - Maligno: Vil
 * 02, 01 - Maligno: Destruidor
 * </pre>
 */
public enum Alignment {
    GOOD,
    NEUTRAL,
    EVIL;

    public static final int MIN = 1;
    public static final int MAX = 10;
    /** A new character's tendência: the upper Neutro value. */
    public static final int DEFAULT = 6;

    /** The band a 1–10 tendência falls in. */
    public static Alignment of(int value) {
        requireInRange(value);
        if (value <= 4) {
            return EVIL;
        }
        return value <= 6 ? NEUTRAL : GOOD;
    }

    /** The tendência's text, e.g. {@code "Bondoso: Samaritano"} for 10. */
    public static String label(int value) {
        requireInRange(value);
        return switch (value) {
            case 10, 9 -> "Bondoso: Samaritano";
            case 8, 7 -> "Bondoso: Benevolente";
            case 6, 5 -> "Neutro";
            case 4, 3 -> "Maligno: Vil";
            default -> "Maligno: Destruidor";
        };
    }

    /** The number followed by its text, e.g. {@code "10 - Bondoso: Samaritano"}. */
    public static String display(int value) {
        return value + " - " + label(value);
    }

    private static void requireInRange(int value) {
        if (value < MIN || value > MAX) {
            throw new IllegalArgumentException(
                    "Tendência must be between " + MIN + " and " + MAX + ", was " + value);
        }
    }
}
