package org.aventyrs.core.magic.invocation;

import lombok.Builder;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageSanctity;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.monster.summon.ElementalDischarge;

import java.util.Optional;

/**
 * What a summoner's Títulos add to one invocation — resolved once, when the invocation is planned
 * ({@code AventyrTitle#resolveSummonEnhancement}), carried on the creature's template the way its
 * Conjurador's Graduação is ({@code NatureSummon#withEnhancement}), and persisted with it. Bruxo's
 * Invocação Maior/Dupla and every Iluminado/Oráculo Abissal clause land here.
 *
 * <p>Plain figures and flags only, so an API can store it as it stands. Everything a roll reads is
 * carried by {@link EnhancedSummonFeat}; what the creature's stat block reads ({@link
 * #lifeMultiplierBonus}, {@link #boostedAttribute}, {@link #sizeIncrease}, {@link #extraActionPoints})
 * by {@code NatureSummon}; and what this core cannot deal ({@link #comet}, {@link #profaneAura},
 * {@link #arrivalBurst}, the wings) is reported for the caller.
 *
 * @param lifeMultiplierBonus      Invocação Maior's +1PA: "Multiplicador de PV aumentados em +3"
 * @param boostedAttribute         Invocação Maior's +2PD: "+4 em Força ou Destreza", or {@code null}
 * @param invocacaoMaior           whether either Invocação Maior effect was bought — "uma Invocação Maior"
 * @param attackDifficultyReduction Iluminado: attack-roll GD "-1 nível"
 * @param defenseDifficultyReduction Oráculo Abissal: Defesa-roll GD "-1 nível"
 * @param flightBonusRounds        Vento do Leste: the "+1 Rodada para cada 2" on top of the caller's 1d6,
 *                                 or {@code null} without wings
 * @param regenerationPerRound     Mar do Sul: "2PV … +1PV para cada 2"
 * @param sizeIncrease             Pântano do Sudeste: "+2, então em +1 para cada duas"
 * @param cometDamageBonus         Nevasca do Sudoeste's comet: "+2 para cada Habilidade", or {@code null}
 *                                 when the creature did not arrive by comet
 * @param fireAttacks              Chamas do Norte: attacks against DM, as Fogo, with Fogo Vivo
 * @param damageTakenReduction     Deserto do Oeste: "RDS 2 … +1 para cada"
 * @param cataclysm                Vulcões do Noroeste: Cataclismo and +1d6 on a critical
 * @param criticalMarginIncrease   Vulcões do Noroeste: "+1, então em +1 para cada 2"
 * @param lightning                Raios do Nordeste: the arrival burst, +2PA, Sagrado
 * @param lightningDamageBonus     Raios do Nordeste: "danos adicionais igual ao número de Habilidades"
 * @param doubled                  Invocação Dupla: one of a doubled pair
 */
@Builder(toBuilder = true)
public record SummonEnhancement(int lifeMultiplierBonus, AttributeDomain boostedAttribute, boolean invocacaoMaior,
                                int attackDifficultyReduction, int defenseDifficultyReduction,
                                Integer flightBonusRounds, int regenerationPerRound, int sizeIncrease,
                                Integer cometDamageBonus, boolean fireAttacks, int damageTakenReduction,
                                boolean cataclysm, int criticalMarginIncrease, boolean lightning,
                                int lightningDamageBonus, boolean doubled) {

    /** No enhancement — every creature nobody enhanced. */
    public static final SummonEnhancement NONE = SummonEnhancement.builder().build();

    /** Invocação Maior: "+4 em Força ou Destreza". */
    public static final int ATTRIBUTE_BONUS = 4;

    /** Invocação Maior: "Multiplicador de PV aumentados em +3". */
    public static final int LIFE_MULTIPLIER_BONUS = 3;

    /** Raios do Nordeste: "recebem +2PA". */
    public static final int LIGHTNING_ACTION_POINTS = 2;

    /** Benção do Vento do Leste: "podem utilizar 3PA para criarem asas mágicas". */
    public static final int WINGS_ACTION_POINTS = 3;

    /** The source the wings' flight is granted under — a second pair renews rather than stacks. */
    public static final String WINGS_SOURCE = "Benção do Vento do Leste";

    /** Nevasca do Sudoeste's comet: "uma rolagem de Ataque à Distância – Distância Média". */
    public static final org.aventyrs.core.skill.SkillType COMET_SKILL = org.aventyrs.core.skill.SkillType.ATAQUE_A_DISTANCIA;
    public static final org.aventyrs.core.scene.Range COMET_RANGE = org.aventyrs.core.scene.Range.DISTANCIA_MEDIA;

    /** Nevasca do Sudoeste: "Criaturas invocadas desta forma emitem uma Aura Profana … 3 Pontos". */
    public static final int PROFANE_AURA_DAMAGE = 3;

    public boolean isNone() {
        return equals(NONE);
    }

    /**
     * Benção do Vento do Leste's wings, once the caller has rolled its d6: "recebem Movimento Base de Voo por 1d6
     * Rodadas, a Duração do voo aumenta em +1 Rodada para cada 2 Habilidades ou Suprema de Iluminado" — a timed {@code
     * GRANTS_FLIGHT} Blessing, the one Draconato grants, for the creature to take on itself after paying {@link
     * #WINGS_ACTION_POINTS}PA. Empty without the Habilidade.
     */
    public Optional<org.aventyrs.core.sheet.Blessing> wings(final int rolledD6) {
        if (flightBonusRounds == null) {
            return Optional.empty();
        }
        return Optional.of(new org.aventyrs.core.sheet.Blessing(org.aventyrs.core.modifier.ModifierType.GRANTS_FLIGHT, 1,
                Math.max(1, rolledD6) + flightBonusRounds, org.aventyrs.core.sheet.TargetScope.SELF, WINGS_SOURCE));
    }

    /**
     * Nevasca do Sudoeste: the comet's roll is made "contra GD ou DM dos Alvos, o que for maior" — the value a {@link
     * #COMET_SKILL} total must reach, given the Magia's casting GD and the highest DM among the targets.
     */
    public static int cometTargetValue(final org.aventyrs.core.skill.DifficultyLevel castingDifficulty,
                                       final int highestTargetMagicDefense) {
        int gd = castingDifficulty == null ? 0 : castingDifficulty.getBaseValue();
        return Math.max(gd, highestTargetMagicDefense);
    }

    /**
     * Raios do Nordeste: the arrival burst reaches "todos os seus equipamentos" too — each worn or wielded item of
     * victim takes rolled, mitigated by its own enhancements ({@code Item#applyDamage}). Returns the total that landed.
     */
    public static int damageEquipment(final org.aventyrs.core.character.Character victim, final int rolled) {
        return victim.getEquipment().stream().mapToInt(item -> item.applyDamage(rolled)).sum();
    }

    /** "Invocações Maiores beneficiadas por ambos os efeitos … são imunes a efeitos de Encantamentos Nocivos". */
    public boolean isImmuneToHarmfulEnchantments() {
        return lifeMultiplierBonus > 0 && boostedAttribute != null;
    }

    /** The Atributo bonus Invocação Maior grants to domain. */
    public int attributeBonus(final AttributeDomain domain) {
        return domain == boostedAttribute ? ATTRIBUTE_BONUS : 0;
    }

    /** Raios do Nordeste's "+2PA". */
    public int extraActionPoints() {
        return lightning ? LIGHTNING_ACTION_POINTS : 0;
    }

    /**
     * Nevasca do Sudoeste's comet — "1d6 pontos de dano Mágico Elemental: Gelo, o dano aumenta em +2 para cada
     * Habilidade de Iluminado", landing on the targets and their adjacências. Reported: the caller rolls the Ataque à
     * Distância against the higher of GD and DM and deals it ({@link ElementalDischarge#dealTo}).
     */
    public Optional<ElementalDischarge> comet() {
        return cometDamageBonus == null ? Optional.empty() : Optional.of(new ElementalDischarge(1, cometDamageBonus, 0,
                new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.GELO), 0, 0, 0));
    }

    /**
     * "Criaturas invocadas desta forma emitem uma Aura Profana, personagens adjacentes (exceto você) sofrem 3 Pontos de
     * Danos Mágicos Profanos no início de cada Rodada" — only after a comet arrival. Reported, caller-dealt each Rodada
     * like {@code NatureSummon#aura}.
     */
    public Optional<ElementalDischarge> profaneAura() {
        return cometDamageBonus == null ? Optional.empty() : Optional.of(new ElementalDischarge(0, PROFANE_AURA_DAMAGE, 0,
                new DamageDescriptor(DamageType.MAGICO, null, DamageSanctity.PROFANO), 0, 0, 0));
    }

    /**
     * Raios do Nordeste: "infligindo 1d6 pontos de Dano Mágico Elemental: Eletricidade aos inimigos adjacentes e todos
     * os seus equipamentos (efeito não cumulativo…)" on arrival. Reported, caller-dealt; the equipment half has no
     * automatic caller (CLAUDE.md, "Damage to an item").
     */
    public Optional<ElementalDischarge> arrivalBurst() {
        return lightning ? Optional.of(new ElementalDischarge(1, 0, 0,
                new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.ELETRICIDADE), 0, 0, 0)) : Optional.empty();
    }
}
