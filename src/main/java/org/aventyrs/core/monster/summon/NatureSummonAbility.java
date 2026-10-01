package org.aventyrs.core.monster.summon;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

import java.util.List;

/**
 * Everything a {@link NatureSummon}'s Características Especiais contribute to a roll or to its mitigation — one
 * instance per creature, varying with its kind, its {@link LacertoPower}s and its Conjurador's Graduação in Domínio
 * do Mana, the {@link ZumbiAbility} shape.
 *
 * <ul>
 *   <li>Every kind: "Recebem Bônus em Perícia de Ataque igual à quantidade de Graduações em Domínio do Mana de seu
 *       Conjurador".</li>
 *   <li>Aliado/Predador at {@link NatureSummon#HIT_POINTS_TIER}: "Bônus Mágico (encantamento) de +5PV" (its Defesas
 *       +2 are folded into the template's Defesas).</li>
 *   <li>At {@link NatureSummon#DIFFICULTY_REDUCTION_TIER}: "reduz GD de Perícias em -1 Nível" (-2 for the
 *       Anciente) — a bare "10", as {@link ZumbiAbility} reads it.</li>
 *   <li>"Danos Físicos sofridos reduzidos em -2" (Orgulho) / "-3" (Anciente) — RD, which reaches only physical hits;
 *       Bruto's "Recebe RD" (Experimento) and "Recebe RA" (Orgulho), one instance each. TODO the Orgulho's Bruto "RE"
 *       names no element.</li>
 * </ul>
 */
@Getter
@RequiredArgsConstructor
@EqualsAndHashCode
public class NatureSummonAbility implements SkillCompetencyAbility {

    /** "+5PV" at {@link NatureSummon#HIT_POINTS_TIER}. */
    public static final int ENCANTAMENTO_HIT_POINTS = 5;

    /** The Orgulho de Lacerto's "Danos Físicos sofridos reduzidos em -2". */
    public static final int ORGULHO_DAMAGE_REDUCTION = 2;

    /** The Anciente's "Danos Físicos sofridos reduzidos em -3". */
    public static final int ANCIENTE_DAMAGE_REDUCTION = 3;

    private final NatureSummonKind kind;
    private final List<LacertoPower> powers;
    private final int conjuradorManaGraduation;

    @Modifier(ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS)
    public int conjuradorAttackBonus() {
        return Math.max(0, conjuradorManaGraduation);
    }

    @Modifier(ModifierType.HIT_POINTS)
    public int encantamentoHitPoints() {
        boolean aliado = kind == NatureSummonKind.ALIADO_DA_NATUREZA || kind == NatureSummonKind.PREDADOR_REGIONAL;
        return aliado && conjuradorManaGraduation >= NatureSummon.HIT_POINTS_TIER ? ENCANTAMENTO_HIT_POINTS : 0;
    }

    @Modifier(ModifierType.DAMAGE_REDUCTION)
    public int damageReduction() {
        return switch (kind) {
            case ORGULHO_DE_LACERTO -> ORGULHO_DAMAGE_REDUCTION;
            case ANCIENTE -> ANCIENTE_DAMAGE_REDUCTION;
            case EXPERIMENTO_DE_LACERTO -> powers.contains(LacertoPower.BRUTO) ? DamageService.DEFAULT_DAMAGE_REDUCTION : 0;
            default -> 0;
        };
    }

    @Modifier(ModifierType.ABSOLUTE_DAMAGE_REDUCTION)
    public int absoluteDamageReduction() {
        return kind == NatureSummonKind.ORGULHO_DE_LACERTO && powers.contains(LacertoPower.BRUTO)
                ? DamageService.DEFAULT_DAMAGE_REDUCTION
                : 0;
    }

    @Override
    public int getDifficultyReduction() {
        if (conjuradorManaGraduation != NatureSummon.DIFFICULTY_REDUCTION_TIER) {
            return 0;
        }
        return kind == NatureSummonKind.ANCIENTE ? 2 : 1;
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_CORPO_A_CORPO;
    }

    @Override
    public String getDescription() {
        return "Recebem Bônus em Perícia de Ataque igual à quantidade de Graduações em Domínio do Mana de seu "
                + "Conjurador.";
    }
}
