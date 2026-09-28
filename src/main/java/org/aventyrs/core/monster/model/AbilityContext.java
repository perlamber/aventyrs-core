package org.aventyrs.core.monster.model;

import lombok.NonNull;
import org.aventyrs.core.monster.MonsterCategory;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * What every {@link MonstrousAbility} hook is answered against: the holder's current Categoria
 * (which decides the Aprimoramentos in force) and the picks it made when it took the Habilidade.
 *
 * @param choices          the picks per {@link ChoiceSpec#id()}; empty for a Habilidade that asks nothing
 * @param ownsSharedTraits whether this Habilidade is the one that grants the Modelo-wide trait it
 *                         shares with a sibling ({@link MonstrousAbility#sharedTraitKey()}) — false
 *                         for every holder but the first, so Anatomia Vegetal held through both
 *                         Nascido Habilidades still grants its Vigor and RC once
 */
public record AbilityContext(@NonNull MonsterCategory category, @NonNull Map<String, List<String>> choices,
                             boolean ownsSharedTraits) {

    public AbilityContext {
        choices = Map.copyOf(choices);
    }

    public static AbilityContext of(@NonNull final MonsterCategory category) {
        return new AbilityContext(category, Map.of(), true);
    }

    public static AbilityContext of(@NonNull final MonsterCategory category, @NonNull final Map<String, List<String>> choices) {
        return new AbilityContext(category, choices, true);
    }

    /** "Aprimoramentos dos Predadores" reach a Predador and every Categoria past it. */
    public boolean isAtLeast(@NonNull final MonsterCategory other) {
        return category.isAtLeast(other);
    }

    /** Every pick under spec {@code id}, in the order taken. */
    public List<String> picks(@NonNull final String id) {
        return choices.getOrDefault(id, List.of());
    }

    /** The first pick under spec {@code id}, or {@code null} when none was made. */
    public String pick(@NonNull final String id) {
        List<String> picks = picks(id);
        return picks.isEmpty() ? null : picks.get(0);
    }

    /** The pick under {@code id} as a constant of {@code type}, when it names one. */
    public <E extends Enum<E>> Optional<E> pick(@NonNull final String id, @NonNull final Class<E> type) {
        return picks(id, type).stream().findFirst();
    }

    /** Every pick under {@code id} that names a constant of {@code type}; unknown names are skipped. */
    public <E extends Enum<E>> List<E> picks(@NonNull final String id, @NonNull final Class<E> type) {
        return picks(id).stream()
                .flatMap(name -> {
                    try {
                        return java.util.stream.Stream.of(Enum.valueOf(type, name));
                    } catch (IllegalArgumentException unknown) {
                        return java.util.stream.Stream.empty();
                    }
                })
                .toList();
    }
}
