package org.aventyrs.core.sheet;

import lombok.Getter;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.Skill;

/**
 * Ferocidade de Lacerto — the Indômito Característica "a partir do quarto turno de combate … recebem
 * Resistência a Danos (RD), Vantagem em rolagens de Ataque Corpo-a-Corpo e Danos". A state rather
 * than a pair of Blessings because it can be <em>declined</em> and <em>ended</em> ("gastar um ponto
 * temporário de Autocontrole para evitar ou finalizar"), and because {@code
 * BestialFeat#ACEITAR_A_LACERTO} mimics it with an extra clause of its own.
 *
 * <p><b>Its numbers reach play through {@link CombatantSheet#getTemporaryBonus}</b>, like a {@link
 * Frenzy}'s: one instance of RD ({@value #DAMAGE_REDUCTION}, "Resistência a Danos" with no figure),
 * the Vantagem on Ataque Corpo-a-Corpo, and the Vantagem on Danos. ⚠️ "Danos" is read as every
 * dano roll, not only a melee one; the state's own "atacam sempre o personagem mais próximo" keeps
 * its holder in melee anyway.
 *
 * <p>Entered and ended through {@code LacertoFerocityService}; the natural state is open-ended and
 * lasts until the combat does, the mimicked one lasts Instinto Rodadas. A {@linkplain #isMimicked()
 * mimicked} copy adds {@code BestialFeat#ACEITAR_A_LACERTO}'s "+1d6 pontos de dano adicionais" to
 * Armas Naturais ({@link #extraDamageDiceFor}), reported for the caller to throw.
 */
@Getter
public class LacertoFerocity extends TemporaryEffect {

    /** One instance of RD — {@code DamageService#DEFAULT_DAMAGE_REDUCTION}. */
    public static final int DAMAGE_REDUCTION = 2;

    /** Aceitar a Lacerto's "+1d6" on Armas Naturais. */
    public static final int MIMICKED_NATURAL_WEAPON_DICE = 1;

    private final boolean mimicked;

    /** The natural state — open-ended, ended by the combat or by a spent Autocontrole point. */
    public static LacertoFerocity natural() {
        return new LacertoFerocity(null, false);
    }

    /** Aceitar a Lacerto's copy, for rounds Rodadas. */
    public static LacertoFerocity mimicked(final int rounds) {
        return new LacertoFerocity(rounds, true);
    }

    private LacertoFerocity(final Integer rounds, final boolean mimicked) {
        super(rounds, true);
        this.mimicked = mimicked;
    }

    /** RD, and the Vantagem on Ataque Corpo-a-Corpo and on Danos; nothing else. */
    public int bonusFor(final ModifierType type) {
        return switch (type) {
            case DAMAGE_REDUCTION -> DAMAGE_REDUCTION;
            case ATAQUE_CORPO_A_CORPO_ROLL_BONUS, DAMAGE_ROLL_BONUS -> Skill.ADVANTAGE_BONUS;
            default -> 0;
        };
    }

    /** The mimicked copy's +1d6 when attacking with something holder treats as an Arma Natural. */
    public int extraDamageDiceFor(final Character holder, final AttackSource attackSource) {
        return mimicked && holder != null && attackSource instanceof Weapon weapon && holder.treatsAsNaturalWeapon(weapon)
                ? MIMICKED_NATURAL_WEAPON_DICE : 0;
    }

    /** The natural state and a mimicked copy may coexist; neither stacks with itself. */
    @Override
    int maximumSimultaneous() {
        return 1;
    }

    @Override
    Object stackingKey() {
        return mimicked;
    }
}
