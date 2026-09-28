package org.aventyrs.core.combat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Objects;

/**
 * The retaliation rule both {@link AttackDelivery} and {@link AttackReceiver} apply, written once:
 * what a defender's thorns deal back to whoever just attacked them in melee. See {@link
 * Retaliation}, and {@code AbracadoPelaEscuridaoAbility#ESPINHOS_VENENOS_DE_GAEA}.
 *
 * <p><b>Melee only</b> — "personagens que lhe atacarem Corpo-a-Corpo" — judged by the Perícia the
 * attack was made with, never by the weapon's {@code ItemCategory}, the same reading {@code
 * ChargeService} takes of "Ataque Corpo-a-Corpo". An Ataque à Distância provokes nothing.
 *
 * <p>Read off a round-scoped {@code ModifierType#RETALIATION_DAMAGE} bonus on the defender, so it
 * is present exactly while the Habilidade that granted it is active, and needs no other state.
 */
final class RetaliationResolver {

    /**
     * "Vigor pontos de Dano Físico Elemental: Natural" — the type is fixed because the one source
     * fixes it; a second source naming a different type would make this a column on the grant.
     */
    private static final DamageDescriptor DESCRIPTOR =
            new DamageDescriptor(DamageType.FISICO_ELEMENTAL, ElementalType.NATURAL);

    /** "perdem -1 Multiplicador de Pontos de Vida (Malefício Veneno) por 2 Rodadas". */
    private static final ConditionType CONDITION_ON_DAMAGE = ConditionType.ENVENENADO;
    private static final int CONDITION_ROUNDS = 2;

    private RetaliationResolver() {
    }

    /**
     * What defender deals back to whoever attacked them with attackSkill, or {@code null} when
     * nothing does — no melee attack, or no active thorns.
     */
    static Retaliation resolve(final CombatantSheet defender, final SkillType attackSkill) {
        if (defender == null || attackSkill != SkillType.ATAQUE_CORPO_A_CORPO) {
            return null;
        }
        int damage = defender.getTemporaryBonus(ModifierType.RETALIATION_DAMAGE);
        if (damage <= 0) {
            return null;
        }
        return new Retaliation(damage, DESCRIPTOR, CONDITION_ON_DAMAGE, CONDITION_ROUNDS);
    }

    /**
     * What defender's Talentos deal back to an attacker whose melee attack <b>landed</b>, each
     * one's own {@code Feat#resolveRetaliation}. Empty for an Ataque à Distância, and whenever no
     * held Talento answers. The caller asks only once the hit is known, so a miss never reaches here.
     */
    static List<Retaliation> resolveOnHit(final CombatantSheet defender, final CombatantSheet attacker,
                                          final SkillType attackSkill, final AttackSource attackSource,
                                          final boolean criticalHit) {
        if (defender == null || attackSkill != SkillType.ATAQUE_CORPO_A_CORPO) {
            return List.of();
        }
        Character holder = defender.getCharacter();
        Character attackerCharacter = attacker == null ? null : attacker.getCharacter();
        return holder.getFeats().stream()
                .map(feat -> feat.resolveRetaliation(holder, attackSource, attackerCharacter, criticalHit))
                .filter(Objects::nonNull)
                .toList();
    }
}
