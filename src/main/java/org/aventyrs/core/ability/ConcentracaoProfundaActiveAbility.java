package org.aventyrs.core.ability;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.sheet.TemporaryEffect;

/**
 * The {@link ActiveAbility} granted by {@link FocusAbility#CONCENTRACAO_PROFUNDA} the moment
 * it's acquired. No acquisition-time choice feeds this one (unlike, say, {@code
 * ArtesAprimorarComArteAbility}'s chosen Perícia), so a single no-arg instance is enough —
 * {@code getDescription()} still delegates to the catalog constant so the rules text keeps one
 * source of truth.
 */
public class ConcentracaoProfundaActiveAbility implements ActiveAbility {

    @Override
    public String getDescription() {
        return FocusAbility.CONCENTRACAO_PROFUNDA.getDescription();
    }

    @Override
    public int getActionPointCost() {
        return 1;
    }

    @Override
    public int getMagicPointCost() {
        return 3;
    }

    @Override
    public int getDurationInRounds() {
        return 2;
    }

    /**
     * "Metade do seu valor de Foco" — half the character's total Foco (base + racialBonus +
     * variable, {@link org.aventyrs.core.character.AttributeValue#getTotal()}), rounded down
     * (integer division), added as a {@link ModifierType#SKILL_ROLL_BONUS} for {@link
     * #getDurationInRounds()} Rodadas — unrestricted, matching this ability's own "às suas
     * rolagens de Perícias" (every Perícia, not one specific skill).
     */
    @Override
    public TemporaryEffect resolveEffect(final Character character) {
        int halfFocus = character.getAttributes().getAttribute(AttributeDomain.FOCUS).getTotal() / 2;
        return new TemporaryBonus(ModifierType.SKILL_ROLL_BONUS, halfFocus, getDurationInRounds());
    }
}
