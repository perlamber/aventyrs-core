package org.aventyrs.core.sheet;

import lombok.Getter;
import org.aventyrs.core.modifier.ModifierType;

/**
 * A bonus a Character's own Vantagem de Ego/Habilidade/Habilidade de Competência grants the
 * moment its holder wins initiative for their group — e.g. {@code
 * org.aventyrs.core.ego.InitiativeAdvantage#POSICIONAMENTO_ESTRATEGICO}'s "o seu Movimento
 * Base e o de seus aliados aumentam em +2UD." Resolved once, at grant-time, by {@code
 * org.aventyrs.core.character.services.InitiativeBlessingService}, then applied by {@code
 * org.aventyrs.core.scene.Scene#applyInitiativeBlessings} as a {@link TemporaryBonus} on
 * every intended recipient's {@code CharacterSheet}.
 *
 * <p>{@code appliesToAllies} is deliberately a plain {@code boolean}, not a {@link
 * TargetScope} — unlike {@code ArtesCompetencyAbility#DOM_BARDICO}'s {@code ALLIES} (which
 * *excludes* the caster: "a eles, mas não a você"), this always applies to the holder
 * themself, and only *additionally* extends to their Scene allies when {@code true}. Reusing
 * {@code TargetScope} here would silently misrepresent that difference.
 */
@Getter
public class InitiativeBlessing {
    private final ModifierType modifierType;
    private final int value;
    private final int rounds;
    private final boolean appliesToAllies;

    public InitiativeBlessing(final ModifierType modifierType, final int value, final int rounds, final boolean appliesToAllies) {
        this.modifierType = modifierType;
        this.value = value;
        this.rounds = rounds;
        this.appliesToAllies = appliesToAllies;
    }
}
