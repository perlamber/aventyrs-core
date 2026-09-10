package org.aventyrs.core.race;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

/**
 * Habilidades Raciais granted to every Homem-Fera — see {@link Race#getRacialAbilities()} for why
 * these are modeled as ordinary {@link SkillCompetencyAbility} instances rather than a separate
 * type.
 */
@Getter
@AllArgsConstructor
public enum HomensFeraRacialAbility implements SkillCompetencyAbility {

    /**
     * The unconditional half of Fortalecimento Feral — "O Movimento Base aumenta em +1UD". This
     * is granted for real; the parenthetical "(dobro transformados)" is not, because the Forma
     * it doubles under is a state nothing tracks (see {@link HomemFera}'s own javadoc).
     *
     * <p>That split is exactly the shape CLAUDE.md's Movimento Base section calls for. "Seu
     * Movimento Base aumenta em +NUD" is the per-Ponto-de-Ação reading a {@code
     * ModifierType#MOVEMENT} bonus lands correctly — the same shape as {@code
     * AtletismoCompetencyAbility#PASSO_LARGO} and {@code DexterityAbility#PASSOS_LONGOS}, not the
     * one-shot-step shape {@code SorteAdvantage#AS_NA_MANGA} over-grants under. And a racial
     * ability reaches it: {@code MovementServiceImpl#getMovementBase} scans {@code
     * SkillCompetencyAbility.allFor(character)}.
     *
     * <p>Withholding the doubling costs nothing today, since being transformed is inexpressible
     * either way; granting the flat +1UD is strictly closer to the clause than granting nothing.
     * The Armamentos Naturais the same trait grants per {@link HomemFera.EspiritoAnimal} are a
     * separate gap — no weapon catalog is authored, and nothing marks a weapon as an Arma Natural.
     */
    FORTALECIMENTO_FERAL("O Movimento Base aumenta em +1UD (dobro transformados) e enquanto " +
            "transformados recebem armamentos naturais conforme o tipo de Fera.") {
        @Modifier(ModifierType.MOVEMENT)
        public int movementBonus() {
            return MOVEMENT_BONUS;
        }
    };

    private static final int MOVEMENT_BONUS = 1;

    private final String description;

    /**
     * A representative value only — Movimento Base is not scoped to any Perícia, and {@code
     * MovementServiceImpl}'s {@code MOVEMENT} scan applies no per-{@code SkillType} filter. Same
     * situation as {@code GuamposRacialAbility#VIGOR_DE_EPONA}'s own enum-level default;
     * Atletismo is picked here because it is the Perícia this ruleset's own movement clauses
     * cluster around ({@code AtletismoCompetencyAbility#PASSO_LARGO}).
     */
    @Override
    public SkillType getSkillType() {
        return SkillType.ATLETISMO;
    }
}
