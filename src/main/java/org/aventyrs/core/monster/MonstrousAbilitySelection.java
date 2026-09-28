package org.aventyrs.core.monster;

import lombok.NonNull;
import org.aventyrs.core.monster.model.AbilityContext;
import org.aventyrs.core.monster.model.ChoiceSpec;
import org.aventyrs.core.monster.model.MonstrousAbility;

import java.util.List;
import java.util.Map;

/**
 * A Habilidade Monstruosa a monster holds, with the picks it made when it took it — keyed by
 * {@link ChoiceSpec#id()}, each spec's picks in order. Empty for a Habilidade that asks nothing.
 */
public record MonstrousAbilitySelection(@NonNull MonstrousAbility ability, @NonNull Map<String, List<String>> choices) {

    public MonstrousAbilitySelection {
        choices = Map.copyOf(choices);
    }

    public static MonstrousAbilitySelection of(@NonNull final MonstrousAbility ability) {
        return new MonstrousAbilitySelection(ability, Map.of());
    }

    /**
     * A Habilidade that asks exactly one thing, with that one pick — the shape every single-choice
     * Habilidade (Relampejante, Sangue Elemental's Elemento) takes. {@code null} picks nothing.
     */
    public static MonstrousAbilitySelection of(@NonNull final MonstrousAbility ability, final String choice) {
        if (choice == null) {
            return of(ability);
        }
        List<ChoiceSpec> specs = ability.getChoiceSpecs(MonsterCategory.ABOMINACAO);
        String id = specs.isEmpty() ? "choice" : specs.get(0).id();
        return new MonstrousAbilitySelection(ability, Map.of(id, List.of(choice)));
    }

    public static MonstrousAbilitySelection of(@NonNull final MonstrousAbility ability,
                                               @NonNull final Map<String, List<String>> choices) {
        return new MonstrousAbilitySelection(ability, choices);
    }

    /**
     * The context this selection's hooks answer against, at category, as if it were the only holder
     * of any shared trait. {@code MonsterBlueprint#contextFor} is the one that knows its siblings.
     */
    public AbilityContext contextAt(@NonNull final MonsterCategory category) {
        return AbilityContext.of(category, choices);
    }
}
