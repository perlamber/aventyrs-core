package org.aventyrs.core.feat;

import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Set;

/**
 * Traits a creature's stat block gives it, in a Talento's shape so the roll and price hooks reach them (core 0.0.97) —
 * {@link FeatCategory#CRIATURA}: never bought, never offered, absent from {@link FeatCatalog}. Held through {@code
 * monster.MonsterTemplate#getFeats()}.
 */
public enum CriaturaFeat implements Feat {

    /**
     * Membros Múltiplos (Aliado da Natureza, a Lacerto creature's power): "Podem usar 3PA para realizar 2 ataques com
     * Desvantagem na Rolagem de Perícia de Ataque". Activated on each of the two attacks, like {@code
     * DuelistaFeat#COMBATER_COM_2_ARMAS}: the first of a pair costs {@link #PAIR_COST}PA and the second nothing more,
     * both rolls at Desvantagem. Any weapon, the same one twice included — its limbs are many, its weapons one. The
     * "+1PA em Rodadas pares" half is {@code monster.summon.NatureSummonTrait}.
     */
    MEMBROS_MULTIPLOS("Podem usar 3PA para realizar 2 ataques com Desvantagem na Rolagem de Perícia de Ataque, em "
            + "Rodadas pares recebem +1PA.") {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return skillType.isAttackSkill() && holder != null;
        }

        @Override
        public Integer resolveAttackActionPointOverride(final SkillType skillType, final AttackSource attackSource,
                                                        final CombatantSheet attacker, final Set<Feat> activatedFeats) {
            if (!activatedFeats.contains(this)) {
                return null;
            }
            return pairOpened(attacker) ? 0 : PAIR_COST;
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character,
                                          final AttackSource attackSource, final CombatantSheet holder,
                                          final SkillRoll skillRoll) {
            return skillType.isAttackSkill() && skillRoll != null && skillRoll.activated(this)
                    ? Skill.DISADVANTAGE_MALUS : 0;
        }
    };

    /** "3PA para realizar 2 ataques". */
    public static final int PAIR_COST = 3;

    private final String description;

    CriaturaFeat(final String description) {
        this.description = description;
    }

    /** Whether holder opened a pair this Turn that still owes its second attack — an odd count of activations. */
    public boolean pairOpened(final CombatantSheet holder) {
        if (holder == null) {
            return false;
        }
        List<CombatantAction> pair = holder.getActionsThisTurn().stream()
                .filter(action -> action.activated(this))
                .toList();
        return pair.size() % 2 == 1;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.CRIATURA;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public FeatRequirements getFeatRequirements() {
        return FeatRequirements.builder().build();
    }
}
