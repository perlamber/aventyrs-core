package org.aventyrs.core.monster.summon;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.Optional;

/**
 * Everything a Zumbi's Características Especiais contribute to a roll, carried as <b>one
 * instance</b> whose behaviour varies with its Conjurador's Graduação in Domínio do Mana.
 *
 * <h2>Why an instance and not an enum constant</h2>
 *
 * Every other ability in this core is an enum constant, because every other ability grants the
 * same thing to everyone who holds it. A Zumbi's don't: its attack bonus <i>is</i> the
 * Conjurador's Graduação, and three further clauses switch on that same number. An enum constant
 * cannot carry it.
 *
 * <p>The way out is the one {@code ArtesAprimorarComArteAbility} already uses — {@code
 * ModifierResolver} invokes a {@code @Modifier} method <b>on the source instance</b> (it caches
 * reflection per class, not per instance), so a no-arg annotated method is free to return an
 * instance field. That is what makes {@link #conjuradorAttackBonus()} and {@link
 * #encantamentoHitPoints()} possible at all: {@code @Modifier}'s {@link ModifierType} is a
 * compile-time-fixed annotation value, but the <i>value returned</i> is ordinary Java.
 *
 * <h2>The tiers</h2>
 *
 * <ul>
 *   <li><b>Always</b> — Vantagem on Perícias de Ataque against a living target, and a Bônus in
 *   Perícia de Ataque equal to the Conjurador's Graduações in Domínio do Mana.</li>
 *   <li><b>≥ 4</b> — Bônus Mágico (encantamento) of +10PV.</li>
 *   <li><b>≥ 7</b> — Bônus Mágico (encantamento) of +2 Força and Roubo de Vida 1. Neither is
 *   here: Força is folded into the spawned Attributes and the Roubo de Vida is applied to the
 *   sheet, both by {@link Zumbi} itself, because neither is a roll contribution.</li>
 *   <li><b>= 10</b> — reduces the GD of Perícias by one nível.</li>
 * </ul>
 *
 * <p>The ≥7 and =10 thresholds are inclusive and exclusive respectively as the rules text writes
 * them: "7 ou mais" versus a bare "10". Since 10 is also the last tier, the two readings only
 * differ if a Conjurador can exceed Graduação 10 — which a player cannot (the Graduação cap is
 * twice an Attribute base of at most 5), so this is written to match the text rather than
 * guessing at a case that cannot arise. A caller passing 11 by hand gets no GD reduction.
 */
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class ZumbiAbility implements SkillCompetencyAbility {

    /** "Se você possuir 4 ou mais Graduações em Domínio do Mana." */
    public static final int HIT_POINTS_TIER = 4;

    /** "Se você possuir 7 ou mais Graduações em Domínio do Mana." */
    public static final int STRENGTH_AND_LIFE_STEAL_TIER = 7;

    /** "Se você possuir 10 Graduações em Domínio do Mana." */
    public static final int DIFFICULTY_REDUCTION_TIER = 10;

    /** The Bônus Mágico (encantamento) the {@link #HIT_POINTS_TIER} clause grants. */
    public static final int ENCANTAMENTO_HIT_POINTS = 10;

    /** The Bônus Mágico (encantamento) to Força the {@link #STRENGTH_AND_LIFE_STEAL_TIER} clause grants. */
    public static final int ENCANTAMENTO_STRENGTH = 2;

    /** The Roubo de Vida the {@link #STRENGTH_AND_LIFE_STEAL_TIER} clause grants. */
    public static final int LIFE_STEAL = 1;

    private final int conjuradorManaGraduation;

    /**
     * "Recebem Bônus em Perícia de Ataque igual à quantidade de Graduações em Domínio do Mana de
     * seu Conjurador." Zero without a Conjurador, which is the narrative case and correct.
     *
     * <p>Annotated for Ataque Corpo-a-Corpo alone, not the broad {@code SKILL_ROLL_BONUS}: the
     * clause names Perícia de Ataque, and a Zumbi has only the one. There is no umbrella
     * "any attack Perícia" {@link ModifierType} to name — {@code @Modifier}'s value is
     * compile-time-fixed, so a bonus covering both attack Perícias needs one annotated method
     * each. Add the Ataque à Distância twin the day a Zumbi can make a ranged attack; it can't.
     */
    @Modifier(ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS)
    public int conjuradorAttackBonus() {
        return Math.max(0, conjuradorManaGraduation);
    }

    /**
     * "Se você possuir 4 ou mais Graduações em Domínio do Mana recebe Bônus Mágico (encantamento)
     * de +10PV." Flat, so {@link ModifierType#HIT_POINTS} rather than a Life Multiplier uplift —
     * the stated amount must not vary with the Zumbi's Vigor.
     */
    @Modifier(ModifierType.HIT_POINTS)
    public int encantamentoHitPoints() {
        return conjuradorManaGraduation >= HIT_POINTS_TIER ? ENCANTAMENTO_HIT_POINTS : 0;
    }

    /**
     * "Se você possuir 10 Graduações em Domínio do Mana reduz GD de Perícias em -1 Nível (efeito
     * de encantamento)."
     *
     * <p>The rules text says "de Perícias", every Perícia — but this hook is per-ability and
     * {@code AbstractSkillInteraction} sums it for whichever Perícia is being rolled, so a single
     * ability held by the Zumbi applies it to all of them, which is what the text asks for.
     */
    @Override
    public int getDifficultyReduction() {
        return conjuradorManaGraduation == DIFFICULTY_REDUCTION_TIER ? 1 : 0;
    }

    /**
     * "Recebem Vantagem em rolagens de Perícias de Ataque efetuadas contra personagens vivos."
     *
     * <p><b>"Living" is resolved as "not a foe whose stat block declared itself Morto-Vivo."</b>
     * That is exact for every combatant this core can build today — a {@code CharacterSheet} is
     * always a living character, and the only non-living things are foes that say so — but it is
     * a narrowing, not the real classification: {@code org.aventyrs.core.race.CreatureType} has
     * only HUMANOIDE/FEERICO/MONSTRUOSO, none of which is about vitality. The day a player
     * character can be undead, or a construct/elemental must count as non-living without being a
     * Morto-Vivo, this needs a real anatomy tag on {@code Character} instead. See
     * {@code MonsterTemplate#isUndead()}.
     */
    @Override
    public Optional<Integer> resolveAttackRollBonus(final CombatantSheet actor, final CombatantSheet attackTarget) {
        if (attackTarget == null || !isLiving(attackTarget)) {
            return Optional.empty();
        }
        return Optional.of(Skill.ADVANTAGE_BONUS);
    }

    private static boolean isLiving(final CombatantSheet sheet) {
        return !(sheet instanceof MonsterSheet monster) || !monster.isUndead();
    }

    /**
     * Ataque Corpo-a-Corpo — the Zumbi's only Perícia de Ataque, and the one the roll bonus above
     * is annotated for. Unlike {@code AnoesRacialAbility#ABATEDORES_DE_GIGANTES}, this needs no
     * widened {@code matchesSkillType}: a Zumbi cannot make a ranged attack, so there is no
     * second Perícia for a roll to name this ability under.
     */
    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_CORPO_A_CORPO;
    }

    @Override
    public String getDescription() {
        return "Recebem Vantagem em rolagens de Perícias de Ataque efetuadas contra personagens "
                + "vivos. Recebem Bônus em Perícia de Ataque igual à quantidade de Graduações em "
                + "Domínio do Mana de seu Conjurador. Se você possuir 4 ou mais Graduações em "
                + "Domínio do Mana recebe Bônus Mágico (encantamento) de +10PV. Se você possuir 7 "
                + "ou mais Graduações em Domínio do Mana recebe Bônus Mágico (encantamento) de +2 "
                + "em Força e Roubo de Vida 1. Se você possuir 10 Graduações em Domínio do Mana "
                + "reduz GD de Perícias em -1 Nível (efeito de encantamento).";
    }
}
