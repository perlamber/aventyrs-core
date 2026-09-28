package org.aventyrs.core.background;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/** The Antecedente catalog — every {@link OriginBackground} then every {@link CareerBackground}. */
public final class Backgrounds {

    private static final List<Background> ALL = Stream.concat(
            Arrays.stream(OriginBackground.values()), Arrays.stream(CareerBackground.values()))
            .map(Background.class::cast)
            .toList();

    private Backgrounds() {
    }

    public static List<Background> all() {
        return ALL;
    }

    public static List<Background> ofKind(final BackgroundKind kind) {
        return ALL.stream().filter(background -> background.getKind() == kind).toList();
    }

    /** The Antecedente persisted under id ({@link Background#name()}), if any. */
    public static Optional<Background> byName(final String id) {
        return ALL.stream().filter(background -> background.name().equals(id)).findFirst();
    }
}
