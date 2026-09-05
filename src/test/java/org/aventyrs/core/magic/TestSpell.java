package org.aventyrs.core.magic;

import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * A minimal {@link Spell} for tests, deliberately independent of the authored catalog in {@code
 * org.aventyrs.core.magic.catalog}. The acquisition-gate tests need Magias placed at arbitrary
 * spots in a tree of a known shape, which a real Árvore cannot provide — and pinning those tests
 * to real catalog entries would make a rules revision break the engine's own tests.
 *
 * <p>Describes an ordinary ranged attack Magia: delivered by Ataque à Distância, cast with Domínio
 * do Mana, reaching a single target at Distância Média, and sitting on {@link
 * TestSpellTree#DIVERGING}'s trunk at SEMENTE. Every column other than those takes {@link Spell}'s
 * own default — assert on one only after giving it a real value here.
 *
 * <p>It implements {@link Spell} directly rather than going through {@link AuthoredSpell}/{@link
 * SpellData}, which is what a consumer's homebrew Magia would normally use: {@code Spell} is
 * unsealed precisely so that stays possible, and this stub is the standing proof that it does.
 *
 * <p>Use {@link #at(SpellTree, BranchLevel, SpellBranch)} to place one somewhere specific in a
 * tree; that is what the acquisition-gate tests vary.
 */
public class TestSpell implements Spell {

    private final SkillType attackSkillType;
    private final SpellTree tree;
    private final BranchLevel branchLevel;
    private final SpellBranch branch;

    /** A Magia delivered by Ataque à Distância — the common case for these tests. */
    public TestSpell() {
        this(SkillType.ATAQUE_A_DISTANCIA);
    }

    /** A Magia delivered by attackSkillType — e.g. Ataque Corpo-a-Corpo for a Magia de Toque. */
    public TestSpell(final SkillType attackSkillType) {
        this(attackSkillType, TestSpellTree.DIVERGING, BranchLevel.SEMENTE, null);
    }

    /**
     * A Magia at a specific spot in a tree — a null branch places it on the trunk, which is what
     * every SEMENTE/BROTO/FLORESCENTE Magia of {@link TestSpellTree#DIVERGING} is.
     */
    public static TestSpell at(final SpellTree tree, final BranchLevel branchLevel, final SpellBranch branch) {
        return new TestSpell(SkillType.ATAQUE_A_DISTANCIA, tree, branchLevel, branch);
    }

    public TestSpell(final SkillType attackSkillType, final SpellTree tree,
                     final BranchLevel branchLevel, final SpellBranch branch) {
        this.attackSkillType = attackSkillType;
        this.tree = tree;
        this.branchLevel = branchLevel;
        this.branch = branch;
    }

    @Override
    public String getName() {
        return "Test Magia";
    }

    @Override
    public SkillType getAttackSkillType() {
        return attackSkillType;
    }

    @Override
    public ActivationTime getActivationTime() {
        return ActivationTime.pa(2);
    }

    @Override
    public DifficultyLevel getCastingDifficultyLevel() {
        return DifficultyLevel.MEDIUM;
    }

    @Override
    public String getDescription() {
        return "A test Magia.";
    }

    @Override
    public String getPrimaryEffectDescription() {
        return null;
    }

    @Override
    public BranchLevel getBranchLevel() {
        return branchLevel;
    }

    @Override
    public SpellTree getTree() {
        return tree;
    }

    @Override
    public Optional<SpellBranch> getBranch() {
        return Optional.ofNullable(branch);
    }

    @Override
    public MagicType getPrimaryType() {
        return MagicType.ELEMENTAL;
    }

    @Override
    public MagicType getSecondaryType() {
        return null;
    }

    @Override
    public SpellDuration getDuration() {
        return SpellDuration.INSTANTANEA;
    }

    @Override
    public SpellTargeting getTargeting() {
        return SpellTargeting.distancia(Range.DISTANCIA_MEDIA);
    }
}
