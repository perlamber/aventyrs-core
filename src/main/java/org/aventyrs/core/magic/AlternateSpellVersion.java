package org.aventyrs.core.magic;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.DifficultyLevel;

import java.util.Optional;
import java.util.Set;

/**
 * A Magia's {@code Efeito Alternativo} presented as a {@link Spell} in its own right — its parent
 * with a {@link SpellAlternateEffect} laid over it.
 *
 * <p><b>Being a {@code Spell} is the entire point.</b> Every consumer already written works on an
 * alternate version with no change at all: {@code SpellCastingService#castSpell} validates its
 * reach, {@code resolveEffect} builds its {@code SpellEffect}, {@code SpellHealingEffect} applies
 * its recovery, {@code SpellDurationService} resolves its Duração. A record of overrides that was
 * not a {@code Spell} would have forced every one of those to learn about alternates.
 *
 * <p>Each accessor returns the delta's value when the delta sets one, and delegates to the parent
 * otherwise. Three cases are not that mechanical, and each is a rule rather than an accident:
 *
 * <ul>
 *   <li><b>{@link #getPrimaryEffectDescription()} is the parent's {@code Efeito Alternativo}
 *   prose.</b> This version's {@code Efeito:} <em>is</em> that paragraph, which is why {@link
 *   SpellAlternateEffect} does not carry it — it is already transcribed on the parent for all 64,
 *   and holding it twice would be two sources of one truth.</li>
 *   <li><b>{@link #getAlternateTargeting()} empties once the delta overrides {@link
 *   #getTargeting()}.</b> That column is the parent's dual {@code Alcance: Pessoal ou Toque} line
 *   — nothing to do with this class despite the name — and a version that states its own reach
 *   does not inherit the other half of its parent's. Left inherited when the delta is silent.</li>
 *   <li><b>{@link #getSecondaryEffectDescription()} is {@code null}.</b> An alternate has no
 *   alternate of its own; no Magia in the catalog carries two.</li>
 * </ul>
 *
 * <p><b>The rung, tree and ramificação always delegate.</b> The alternate sits exactly where its
 * parent does — it is the same Magia — and that is what keeps the three acquisition gates honest.
 * It is never acquired separately either: a character who learns the base version "automaticamente
 * aprende sua segunda versão", so {@link #isAlternateVersion()} is what {@code
 * SpellService#grantSpell} refuses, rather than charging experience twice for one Magia.
 */
public class AlternateSpellVersion implements Spell {

    private final Spell parent;
    private final SpellAlternateEffect alternate;

    public AlternateSpellVersion(final Spell parent, final SpellAlternateEffect alternate) {
        this.parent = parent;
        this.alternate = alternate;
    }

    /** The Magia this is the second version of. */
    public Spell getParent() {
        return parent;
    }

    /** The authored overrides this version lays over {@link #getParent()}. */
    public SpellAlternateEffect getAlternateEffect() {
        return alternate;
    }

    @Override
    public boolean isAlternateVersion() {
        return true;
    }

    // --- overridden by the delta, or inherited -------------------------------------------------

    @Override
    public String getName() {
        return alternate.name();
    }

    @Override
    public String getPrimaryEffectDescription() {
        return parent.getSecondaryEffectDescription();
    }

    @Override
    public ActivationTime getActivationTime() {
        return alternate.activationTime() != null ? alternate.activationTime() : parent.getActivationTime();
    }

    @Override
    public SpellTargeting getTargeting() {
        return alternate.targeting() != null ? alternate.targeting() : parent.getTargeting();
    }

    @Override
    public Optional<SpellTargeting> getAlternateTargeting() {
        return alternate.targeting() != null ? Optional.empty() : parent.getAlternateTargeting();
    }

    @Override
    public SpellDuration getDuration() {
        return alternate.duration() != null ? alternate.duration() : parent.getDuration();
    }

    @Override
    public DifficultyLevel getCastingDifficultyLevel() {
        return alternate.castingDifficultyLevel() != null
                ? alternate.castingDifficultyLevel()
                : parent.getCastingDifficultyLevel();
    }

    @Override
    public SkillType getAttackSkillType() {
        return alternate.attackSkillType() != null ? alternate.attackSkillType() : parent.getAttackSkillType();
    }

    @Override
    public boolean isCastingDifficultyFlooredByTargetMagicDefense() {
        return alternate.castingDifficultyFlooredByTargetMagicDefense() != null
                ? alternate.castingDifficultyFlooredByTargetMagicDefense()
                : parent.isCastingDifficultyFlooredByTargetMagicDefense();
    }

    @Override
    public CriticalEffectType getCriticalEffectType() {
        if (alternate.suppressesCriticalEffect()) {
            return null;
        }
        return alternate.criticalEffectType() != null
                ? alternate.criticalEffectType()
                : parent.getCriticalEffectType();
    }

    @Override
    public int getManaCost() {
        return alternate.manaCost() != null ? alternate.manaCost() : parent.getManaCost();
    }

    @Override
    public String getEffectChainDescription() {
        return alternate.effectChainDescription() != null
                ? alternate.effectChainDescription()
                : parent.getEffectChainDescription();
    }

    @Override
    public Optional<SpellHealing> getHealing() {
        return alternate.healing() != null ? Optional.of(alternate.healing()) : Optional.empty();
    }

    @Override
    public Set<ConditionType> getCleansedConditions() {
        return alternate.cleansedConditions();
    }

    @Override
    public Optional<SpellDamage> getPrimaryDamage() {
        return alternate.primaryDamage() != null ? Optional.of(alternate.primaryDamage()) : Optional.empty();
    }

    // --- always the parent's ------------------------------------------------------------------

    @Override
    public String getDescription() {
        return parent.getDescription();
    }

    @Override
    public BranchLevel getBranchLevel() {
        return parent.getBranchLevel();
    }

    @Override
    public SpellTree getTree() {
        return parent.getTree();
    }

    @Override
    public Optional<SpellBranch> getBranch() {
        return parent.getBranch();
    }

    @Override
    public MagicType getPrimaryType() {
        return parent.getPrimaryType();
    }

    @Override
    public MagicType getSecondaryType() {
        return parent.getSecondaryType();
    }

    @Override
    public SkillType getConjurationSkillType() {
        return parent.getConjurationSkillType();
    }

    @Override
    public Optional<DifficultyLevel> getCastingDifficultyAgainst(final BranchLevel branchLevel) {
        return parent.getCastingDifficultyAgainst(branchLevel);
    }

    /** {@code null} — an Efeito Alternativo has none of its own; no Magia here carries two. */
    @Override
    public String getSecondaryEffectDescription() {
        return null;
    }

    /** {@link Optional#empty()} — likewise, an alternate has no second version of its own. */
    @Override
    public Optional<Spell> getAlternateVersion() {
        return Optional.empty();
    }

    @Override
    public boolean isEligible(final Character character, final BranchLevel maxBranchLevel) {
        return parent.isEligible(character, maxBranchLevel);
    }
}
