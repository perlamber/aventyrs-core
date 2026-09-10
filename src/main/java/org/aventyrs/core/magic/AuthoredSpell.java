package org.aventyrs.core.magic;

import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * A {@link Spell} backed by a {@link SpellData} block — the whole of the per-tree delegation,
 * written once. A tree's catalog enum implements this, holds its {@code SpellData} and says
 * which tree it is; nothing else:
 *
 * <pre>
 * public enum AnulacaoSpell implements AuthoredSpell {
 *
 *     REMOVER_MALDICAO(SpellData.builder()...build());
 *
 *     private final SpellData data;
 *
 *     AnulacaoSpell(final SpellData data) { this.data = data; }
 *
 *     &#64;Override public SpellData getData() { return data; }
 *
 *     &#64;Override public SpellTree getTree() { return MagicTree.ANULACAO; }
 * }
 * </pre>
 *
 * <p>Twenty trees times fifteen columns is three hundred delegating methods this replaces. It is
 * an interface rather than an abstract class for the one reason that matters here: a catalog
 * entry has to be an <b>enum</b> — a Conjurador's known Magias are persisted by a consumer, and
 * {@code name()} is the stable key that survives a round trip — and an enum can extend nothing.
 *
 * <p>It is public, not package-private, because it is equally the shortest path to a consumer's
 * own homebrew Magia: build a {@code SpellData}, implement two methods. {@link Spell} itself is
 * deliberately not sealed, so implementing it directly stays available too.
 */
public interface AuthoredSpell extends Spell {

    /** This Magia's authored rules-text block. */
    SpellData getData();

    @Override
    default String getName() {
        return getData().getName();
    }

    @Override
    default BranchLevel getBranchLevel() {
        return getData().getBranchLevel();
    }

    @Override
    default Optional<SpellBranch> getBranch() {
        return Optional.ofNullable(getData().getBranch());
    }

    @Override
    default ActivationTime getActivationTime() {
        return getData().getActivationTime();
    }

    @Override
    default SkillType getAttackSkillType() {
        return getData().getAttackSkillType();
    }

    @Override
    default DifficultyLevel getCastingDifficultyLevel() {
        return getData().getCastingDifficultyLevel();
    }

    @Override
    default boolean isCastingDifficultyFlooredByTargetMagicDefense() {
        return getData().isCastingDifficultyFlooredByTargetMagicDefense();
    }

    @Override
    default Optional<DifficultyLevel> getCastingDifficultyAgainst(final BranchLevel targetLevel) {
        return getData().isCastingDifficultyScaledToTargetLevel()
                ? Optional.of(Spell.castingDifficultyAgainst(targetLevel))
                : Optional.empty();
    }

    @Override
    default String getDescription() {
        return getData().getDescription();
    }

    @Override
    default String getPrimaryEffectDescription() {
        return getData().getPrimaryEffectDescription();
    }

    @Override
    default Optional<SpellDamage> getPrimaryDamage() {
        return Optional.ofNullable(getData().getPrimaryDamage());
    }

    @Override
    default String getSecondaryEffectDescription() {
        return getData().getSecondaryEffectDescription();
    }

    @Override
    default String getEffectChainDescription() {
        return getData().getEffectChainDescription();
    }

    @Override
    default CriticalEffectType getCriticalEffectType() {
        return getData().getCriticalEffectType();
    }

    @Override
    default SpellDuration getDuration() {
        return getData().getDuration();
    }

    @Override
    default SpellTargeting getTargeting() {
        return getData().getTargeting();
    }

    @Override
    default Optional<SpellTargeting> getAlternateTargeting() {
        return Optional.ofNullable(getData().getAlternateTargeting());
    }
}
