package org.aventyrs.core.magic.invocation;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CriticalDamage;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageSanctity;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.effect.Cataclismo;
import org.aventyrs.core.effect.CriticalEffect;
import org.aventyrs.core.effect.CriticalEffectContext;
import org.aventyrs.core.feat.AbstractFeat;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.feat.FeatRequirements;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Optional;

/**
 * A summoned creature's share of its summoner's {@link SummonEnhancement}, in a Talento's shape so every roll and
 * mitigation hook reaches it — the {@code feat.CriaturaFeat} idea, but carrying per-invocation figures, which an enum
 * constant cannot. {@link FeatCategory#CRIATURA}: never bought, never offered, absent from {@code FeatCatalog}.
 * Held through {@code NatureSummon#getFeats()}.
 */
@Getter
public final class EnhancedSummonFeat extends AbstractFeat {

    private final SummonEnhancement enhancement;

    public EnhancedSummonFeat(@NonNull final SummonEnhancement enhancement) {
        super(FeatCategory.CRIATURA, "O que o Título de seu invocador concede a esta invocação.",
                FeatRequirements.builder().build());
        this.enhancement = enhancement;
    }

    /** Iluminado (attack rolls) and Oráculo Abissal (Esquiva e Aparar, its Defesa roll): "-1 nível". */
    @Override
    public int resolveDifficultyReduction(final SkillType skillType, final Character character) {
        if (skillType.isAttackSkill()) {
            return enhancement.attackDifficultyReduction();
        }
        return skillType == SkillType.ESQUIVA_E_APARAR ? enhancement.defenseDifficultyReduction() : 0;
    }

    /** Deserto do Oeste: "RDS 2 … +1 para cada". */
    @Override
    public int resolveDamageTakenReduction(final Character character) {
        return enhancement.damageTakenReduction();
    }

    /** Vulcões do Noroeste: "A Margem Crítica Menor dos ataques … é aumentada". */
    @Override
    public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                             final Character character) {
        return skillType.isAttackSkill() ? enhancement.criticalMarginIncrease() : 0;
    }

    /**
     * Vulcões do Noroeste: "+1d6 de Dano Físico Elemental: Magma em suas rolagens de Dano Crítico". TODO the die's
     * Magma typing: a {@code CriticalDamage} carries no type, so it lands as the hit's own.
     */
    @Override
    public CriticalDamage resolveCriticalDamage(final SkillType attackSkill, final SceneContext sceneContext,
                                                final Character character, final AttackSource attackSource,
                                                final CriticalResult criticalResult, final SkillRoll skillRoll) {
        return enhancement.cataclysm() ? CriticalDamage.ofDice(1) : CriticalDamage.NONE;
    }

    /** Vulcões do Noroeste: "Cataclismo como Efeito Crítico adicional em seus ataques". */
    @Override
    public List<CriticalEffect> resolveExtraCriticalEffects(final Character attacker, final SkillType attackSkill,
                                                            final AttackSource attackSource,
                                                            final CriticalResult criticalResult) {
        if (!enhancement.cataclysm() || !attackSkill.isAttackSkill() || criticalResult == null
                || !criticalResult.isCriticalSuccess()) {
            return List.of();
        }
        return List.of(new Cataclismo(new CriticalEffectContext(null, attackSource, criticalResult, null)));
    }

    /** Raios do Nordeste: "danos adicionais igual ao número de Habilidades de Oráculo Abissal". */
    @Override
    public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType, final SceneContext sceneContext,
                                                    final CombatantSheet attackTarget, final Character actor) {
        if (enhancement.lightningDamageBonus() <= 0 || !attackingSkillType.isAttackSkill()) {
            return Optional.empty();
        }
        DamageDescriptor type = retype();
        return Optional.of(type == null
                ? new DamageBonus(enhancement.lightningDamageBonus(), DamageType.FISICO)
                : new DamageBonus(enhancement.lightningDamageBonus(), type.damageType(), type.elementalType()));
    }

    /** Chamas do Norte: "Rolagens de Ataque de suas invocações são efetuadas contra a DM de seus alvos". */
    @Override
    public DefenseType resolveTargetDefenseOverride(final SkillType attackSkill, final AttackSource attackSource,
                                                    final CombatantSheet holder, final SkillRoll skillRoll) {
        return enhancement.fireAttacks() && attackSkill.isAttackSkill() ? DefenseType.MAGIC : null;
    }

    /**
     * Chamas do Norte: "o tipo de dano causado muda para Dano Mágico Elemental: Fogo"; with Raios do Nordeste, "causam
     * danos Sagrados em substituição a quaisquer danos Elementais" — ⚠️ read as a Mágico hit of Sagrado nature. Raios
     * alone retypes nothing: an ordinary hit has no Elemental damage to replace. Its "inimigos imunes a fogo ainda
     * sofrem metade dos danos" is {@link #halvesThroughImmunity}.
     */
    @Override
    public DamageDescriptor resolveDamageRetype(final Character attacker, final SkillType attackSkill,
                                                final AttackSource attackSource) {
        return attackSkill.isAttackSkill() ? retype() : null;
    }

    private DamageDescriptor retype() {
        if (!enhancement.fireAttacks()) {
            return null;
        }
        return enhancement.lightning()
                ? new DamageDescriptor(DamageType.MAGICO, null, DamageSanctity.SAGRADO)
                : new DamageDescriptor(DamageType.ELEMENTAL, ElementalType.FOGO);
    }

    /** Chamas do Norte: "inimigos imunes a fogo ainda sofrem metade dos danos" — a Fogo hit, immune or not. */
    @Override
    public boolean halvesThroughImmunity(final DamageDescriptor descriptor) {
        return enhancement.fireAttacks() && descriptor.elementalType() == ElementalType.FOGO;
    }

    /** Invocação Maior: "Você ou seus aliados, enquanto adjacentes à uma Invocação Maior, recebem RM". */
    @Override
    public int resolveAdjacentAllyMagicReduction(final Character holder) {
        return enhancement.invocacaoMaior() ? DamageService.DEFAULT_DAMAGE_REDUCTION : 0;
    }
}
