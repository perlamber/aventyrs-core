package org.aventyrs.core.monster.summon;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.Resistance;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * Everything a Carniçal's Características Especiais contribute to a roll — the {@link ZumbiAbility} shape, one instance
 * carrying its Conjurador's Graduação in Domínio do Mana (core 0.1.6.25). Its +2 Força, Destreza and Vigor at 7
 * Graduações are folded into {@link Carnical#getAttributeBases()} instead, like the Zumbi's Força.
 *
 * <p>Açougueiro's "Vantagem nas rolagens de Perícias de Ataque e Danos" rides here too, when {@code butcher}.
 */
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class CarnicalAbility implements SkillCompetencyAbility {

    /** "Danos Físicos sofridos reduzidos em -2" — authored as Orgulho de Lacerto's identical clause is. */
    public static final int DAMAGE_REDUCTION = 2;

    private final int conjuradorManaGraduation;
    private final boolean butcher;

    /** "Recebem Bônus em Perícia de Ataque igual à quantidade de Graduações em Domínio do Mana de seu Conjurador." */
    @Modifier(ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS)
    public int conjuradorAttackBonus() {
        return Math.max(0, conjuradorManaGraduation) + (butcher ? Skill.ADVANTAGE_BONUS : 0);
    }

    @Modifier(ModifierType.DAMAGE_REDUCTION)
    public int damageReduction() {
        return Resistance.RDS.instances(DAMAGE_REDUCTION);
    }

    /** "Se você possuir 10 Graduações em Domínio do Mana reduz GD de Perícias em -1 Nível (efeito de encantamento)." */
    @Override
    public int getDifficultyReduction() {
        return conjuradorManaGraduation == ZumbiAbility.DIFFICULTY_REDUCTION_TIER ? 1 : 0;
    }

    /** Açougueiro: "Vantagem nas rolagens de … Danos". */
    @Override
    public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType, final SceneContext sceneContext,
                                                    final CombatantSheet attackTarget, final Character actor,
                                                    final AttackSource attackSource, final CombatantSheet holder) {
        return butcher ? Optional.of(new DamageBonus(Skill.ADVANTAGE_BONUS, null)) : Optional.empty();
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_CORPO_A_CORPO;
    }

    @Override
    public String getDescription() {
        return "Recebem Bônus em Perícia de Ataque igual à quantidade de Graduações em Domínio do Mana de seu "
                + "Conjurador. Se você possuir 7 ou mais Graduações em Domínio do Mana recebe Bônus Mágico "
                + "(encantamento) de +2 em Força, Destreza e Vigor. Se você possuir 10 Graduações em Domínio do Mana "
                + "reduz GD de Perícias em -1 Nível (efeito de encantamento). Danos Físicos sofridos reduzidos em -2.";
    }
}
