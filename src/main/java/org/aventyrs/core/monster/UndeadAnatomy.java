package org.aventyrs.core.monster;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageSanctity;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.util.DiceRoller;

import java.util.Set;

/**
 * <b>Anatomia de Morto-Vivo</b> — the undead body the Modelo Escolhido de Macária describes ({@code
 * docs/rules/criacao-de-monstros.txt}, manual v19 20261013), and the anatomy every spell-raised undead takes in place of
 * its printed "Anatomia de Morto-Vivo Menor" (table ruling, 2026-10-14).
 *
 * <p>A foe has it when its stat block says so ({@link MonsterTemplate#hasUndeadAnatomy()}) or a held Habilidade
 * Monstruosa grants it ({@code MonstrousAbility#grantsUndeadAnatomy()}); {@link CombatantSheet#hasUndeadAnatomy()} is
 * the one question every consumer asks. Clause by clause:
 *
 * <ul>
 *   <li>"imunidade a Efeitos Críticos (apenas Atordoante, Dilacerar, Empalar, Ferida Profunda, Oferenda Maldita e
 *   Sangramento)" — {@link #CRITICAL_EFFECT_IMMUNITIES}, folded into {@code MonsterSheet#getCriticalEffectImmunities}.</li>
 *   <li>"Resistência a Correntes de Efeitos +2" — {@link #EFFECT_CHAIN_RESISTANCE}, added by {@code
 *   EffectChainService#getRequiredMargin}.</li>
 *   <li>"Efeitos diversos de sangramento, doenças, maldições e venenos" — immunity to {@link #CONDITION_IMMUNITIES}
 *   (bleeding is the Sangramento Efeito Crítico, already above).</li>
 *   <li>"Efeitos Profanos adicionalmente curam os mortos em 1d6+ Metade do Vigor, além de não infligir nenhum dano" —
 *   immunity to Profano damage ({@link #isImmuneToDamage}) and to a Magia Profana's damage ({@link
 *   #isImmuneToSpellDamage}), with {@link #profaneHealing} in its place.</li>
 *   <li>"Efeitos Sagrados ou curativos, feitos para os vivos, não recuperam PV, não ressuscitam ou regeneram partes
 *   perdidas, ao invés disso infligem 2d6+ Metade do Vigor pontos de Dano Sagrado" — {@link #invertsHealing} and
 *   {@link #sacredDamage}.</li>
 *   <li>"Habilidades monstruosas de Monstros de outros modelos, podem curar um Morto-Vivo, desde que eles também sejam
 *   Mortos-Vivos" — an undead healer never inverts.</li>
 *   <li>"Os Mortos não precisam dormir, respirar (imune a danos de Asfixia)" — Asfixia damage immunity, and {@code
 *   AsphyxiaService#stopBreathing} does nothing. Nothing tracks sleep.</li>
 * </ul>
 */
public final class UndeadAnatomy {

    public static final Set<CriticalEffectType> CRITICAL_EFFECT_IMMUNITIES = Set.of(
            CriticalEffectType.ATORDOANTE,
            CriticalEffectType.DILACERAR,
            CriticalEffectType.EMPALAR,
            CriticalEffectType.FERIDA_PROFUNDA,
            CriticalEffectType.OFERENDA_MALDITA,
            CriticalEffectType.SANGRAMENTO);

    /** "Resistência a Correntes de Efeitos +2". */
    public static final int EFFECT_CHAIN_RESISTANCE = 2;

    /** Doenças, maldições e venenos. */
    public static final Set<ConditionType> CONDITION_IMMUNITIES = Set.of(
            ConditionType.DOENTE,
            ConditionType.AMALDICOADO,
            ConditionType.ENVENENADO);

    /** "2d6+ Metade do Vigor pontos de Dano Sagrado" — the dice. */
    public static final int SACRED_DAMAGE_DICE = 2;

    private UndeadAnatomy() {
    }

    /** Profano and Asfixia damage reach nothing. */
    public static boolean isImmuneToDamage(final DamageType damageType, final DamageDescriptor descriptor) {
        if (damageType == DamageType.ASFIXIA || descriptor != null && descriptor.damageType() == DamageType.ASFIXIA) {
            return true;
        }
        return descriptor != null && descriptor.sanctity() == DamageSanctity.PROFANO;
    }

    /** A Magia Profana deals no damage to the dead. */
    public static boolean isImmuneToSpellDamage(final Spell spell) {
        return isProfane(spell);
    }

    /** Whether spell is a Magia Profana. */
    public static boolean isProfane(final Spell spell) {
        return spell != null && spell.getTree() != null && spell.getTree().hasMagicType(MagicType.PROFANA);
    }

    /**
     * Whether a heal from source damages the dead instead: a Magia that is not Profana, or a Habilidade de Título — the
     * "efeitos curativos feitos para os vivos". A Descanso, a Regeneração, Roubo de Vida and the dead's own Habilidades
     * are not; and an undead healer never inverts.
     */
    public static boolean invertsHealing(final HealingSource source) {
        if (source == null) {
            return false;
        }
        if (source.healer() != null && source.healer().hasUndeadAnatomy()) {
            return false;
        }
        if (source.spell() != null) {
            return !isProfane(source.spell());
        }
        return source.titleAbility() != null;
    }

    /** "1d6+ Metade do Vigor" — what a Profano effect heals instead of harming. */
    public static int profaneHealing(final CombatantSheet target, final DiceRoller roller) {
        return roller.rollD6() + halfVigor(target);
    }

    /** "2d6+ Metade do Vigor pontos de Dano Sagrado" — what a curative effect deals instead of healing. */
    public static int sacredDamage(final CombatantSheet target, final DiceRoller roller) {
        return roller.rollD6(SACRED_DAMAGE_DICE) + halfVigor(target);
    }

    private static int halfVigor(final CombatantSheet target) {
        return target.getAttributeTotal(AttributeDomain.VIGOR) / 2;
    }
}
