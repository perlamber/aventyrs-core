package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * Computes a character's total Defesa — DF or DM, per {@link DefenseType} — the first real
 * consumer of {@link ModifierType#DEFESAS}, which until now was a registry entry nothing read.
 *
 * <p>A Defesa is not a passive score an attacker rolls against (see {@link DefenseType}'s own
 * javadoc): it's a pool of bonuses feeding the defender's own Esquiva e Aparar roll. This
 * service computes only the Defesa-typed half of that pool — {@code
 * org.aventyrs.core.skill.esquivaeaparar.EsquivaEApararInteraction}'s 4-arg {@code applyTo}
 * adds it onto everything {@code AbstractSkillInteraction} already sums for the roll, and
 * {@code org.aventyrs.core.combat.AttackReceiver} is the entry point that drives the whole
 * exchange. Nothing here duplicates a {@code SKILL_ROLL_BONUS} the Interaction already counts:
 * the {@link ModifierType}s scanned here are disjoint from that one, so the two add cleanly.
 *
 * <p>Every source is summed for <b>both</b> {@link ModifierType#DEFESAS} (applies to DF and DM
 * alike) and {@link DefenseType#getModifierType()} (this Defesa alone) — additively, not
 * either/or, exactly how {@code AbstractSkillInteraction#sumSkillRollBonusModifiers} combines
 * the generic and per-Perícia roll-bonus types. Sources:
 * <ol>
 *   <li>The standard three-source {@code @Modifier} scan — {@code getAttributeAbilities()},
 *   {@code SkillCompetencyAbility.allFor(character)} (so racial abilities count too), and, per
 *   trained Perícia, that Perícia's unlocked {@code SkillExcellency} tiers.</li>
 *   <li><b>Equipped items</b> ({@code Character#getEquipment()}) — the flat DF/DM column via
 *   {@link DefenseType#columnOf}, plus whatever that item's {@code ItemFavor} grants of either
 *   type once its Requisitos are met. This is what makes {@code ArmorItem}'s DF/DM columns and
 *   {@code ROUPA_PESADA}'s {@code DEFESAS 2} Favor real.</li>
 *   <li><b>{@link #getTotalDefense(CombatantSheet, DefenseType)} only</b> — the sheet's own
 *   {@code TemporaryBonus} pool, which is what finally makes a {@code DEFESAS}-typed {@code
 *   Blessing} (e.g. {@code GritoDeGuerraVulcanoInteraction}'s) do something.</li>
 * </ol>
 *
 * <p><b>Not clamped at 0.</b> Reações/Ações Livres/RD/RA all clamp because they're spendable
 * resources where a negative is meaningless; a Defesa is a comparison value, so a large enough
 * malus leaving it negative is a valid (if dire) state rather than an error — the same
 * reasoning {@link InitiativeService} documents for Iniciativa. This is also what leaves room
 * for the standard "-2 em suas Defesas" penalty {@code org.aventyrs.core.ability.DexterityAbility}
 * cites.
 *
 * <p>Deliberately <b>not</b> scanned: {@code org.aventyrs.core.title.santo.Santo
 * #getDefesasBonus(SceneContext)}. That's a hook on one concrete Título class rather than on the
 * {@code AventyrTitleAbility} interface (unlike the RA hook {@code DamageService} does scan), and
 * its own javadoc names a second missing piece anyway — <i>when</i> each adjacent ally receives
 * it. Wiring it is its own change.
 */
public interface DefenseService {

    /**
     * character's total DF or DM from their abilities and equipped items. Omits the {@code
     * TemporaryBonus} pool, which lives on the sheet rather than the Character — prefer {@link
     * #getTotalDefense(CombatantSheet, DefenseType)} wherever a sheet is available, the same
     * Character-versus-sheet split {@code DamageService} uses for RD/RA.
     */
    int getTotalDefense(Character character, DefenseType defenseType);

    /**
     * Same as {@link #getTotalDefense(Character, DefenseType)}, plus target's currently-active
     * {@code DEFESAS}/scoped {@code TemporaryBonus}es. The overload real callers use.
     */
    int getTotalDefense(CombatantSheet target, DefenseType defenseType);
}
