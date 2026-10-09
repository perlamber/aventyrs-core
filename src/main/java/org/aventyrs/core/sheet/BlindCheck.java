package org.aventyrs.core.sheet;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.SkillType;

/**
 * The 1d6 a Cego combatant throws beside a <b>Perícia Física</b> — {@link ConditionType#CEGO}: "Deve
 * rolar 1d6 sempre que efetuar uma rolagem de Perícia Física (baseada em Força ou Destreza). Perícias
 * de efeitos pessoal falham com resultados 2 ou menos … Perícias que afetem outros personagens
 * adjacentes ou o cenário falham com resultados 3 ou menos … Perícias que afetem outros personagens
 * além de adjacente falham com resultados 5 ou menos, independentemente de sucessos na rolagem."
 *
 * <p>Only the table: <b>whether</b> a roller must throw it is {@link
 * CombatantSheet#getBlindCheckThreshold} (Cego, a Força/Destreza roll, and not exempted by a held
 * Talento — {@code DuelistaFeat#COMBATER_AS_CEGAS}), and the die itself rides the roll ({@code
 * SkillRoll#withBlindCheck}). The tier is the roll's <b>reach</b> ({@link Reach}): an attack is
 * judged by the distance to its target; any other roll is read as "de efeito pessoal".
 */
public final class BlindCheck {

    /** How far a blind roll reaches — what the failure face depends on. */
    public enum Reach {
        /** "Perícias de efeitos pessoal falham com resultados 2 ou menos." */
        PERSONAL(2),
        /** "Perícias que afetem outros personagens adjacentes ou o cenário falham com resultados 3 ou menos." */
        ADJACENT_OR_SCENERY(3),
        /** "Perícias que afetem outros personagens além de adjacente falham com resultados 5 ou menos." */
        BEYOND_ADJACENT(5);

        private final int failureFace;

        Reach(final int failureFace) {
            this.failureFace = failureFace;
        }

        /** The highest d6 face that fails a roll of this reach. */
        public int getFailureFace() {
            return failureFace;
        }
    }

    private BlindCheck() {
    }

    /** Whether a roll governed by governing owes the die at all — Força or Destreza only. */
    public static boolean isPhysical(final AttributeDomain governing) {
        return governing == AttributeDomain.STRENGTH || governing == AttributeDomain.DEXTERITY;
    }

    /**
     * The reach of a skillType roll aimed distanceToTarget away: an attack is adjacent or beyond by
     * that distance — or, with none known, by its Perícia (corpo-a-corpo adjacent, à distância
     * beyond); anything else is personal.
     */
    public static Reach reachOf(final SkillType skillType, final Range distanceToTarget) {
        if (!skillType.isAttackSkill()) {
            return Reach.PERSONAL;
        }
        if (distanceToTarget != null) {
            return distanceToTarget.isWithin(Range.ADJACENTE) ? Reach.ADJACENT_OR_SCENERY : Reach.BEYOND_ADJACENT;
        }
        return skillType == SkillType.ATAQUE_CORPO_A_CORPO ? Reach.ADJACENT_OR_SCENERY : Reach.BEYOND_ADJACENT;
    }

    /** The highest d6 face that fails a skillType roll made blind, with no target distance known. */
    public static int failureThresholdFor(final SkillType skillType) {
        return reachOf(skillType, null).getFailureFace();
    }
}
