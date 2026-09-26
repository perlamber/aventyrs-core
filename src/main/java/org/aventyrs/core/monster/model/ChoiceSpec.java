package org.aventyrs.core.monster.model;

import lombok.NonNull;

import java.util.List;

/**
 * One pick a Habilidade Monstruosa asks for — "Escolha um Elemento", "Aprende 2 Árvores de Magia
 * Elemental", "+1 Reação ou +1 Ação Livre".
 *
 * @param id      the key the picks travel under in {@code MonstrousAbilitySelection#choices()} —
 *                stable, so a stored blueprint keeps its meaning
 * @param options every value it may take, as constant names
 * @param count   exactly how many distinct values it takes; may grow with the Categoria ("uma Árvore
 *                de Magia adicional" at Predador), which is why specs are asked for per Categoria
 */
public record ChoiceSpec(@NonNull String id, @NonNull List<String> options, int count) {

    public static ChoiceSpec one(@NonNull final String id, @NonNull final List<String> options) {
        return new ChoiceSpec(id, options, 1);
    }

    /** Every constant of an enum as options — the common case ("Escolha um Atributo"). */
    public static ChoiceSpec one(@NonNull final String id, @NonNull final Enum<?>[] values) {
        return new ChoiceSpec(id, names(values), 1);
    }

    public static ChoiceSpec many(@NonNull final String id, @NonNull final List<String> options, final int count) {
        return new ChoiceSpec(id, options, count);
    }

    public static List<String> names(final Enum<?>[] values) {
        return java.util.Arrays.stream(values).map(Enum::name).toList();
    }
}
