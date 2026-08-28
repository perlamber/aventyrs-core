package org.aventyrs.core.magic;

import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * A minimal {@link Spell} for tests. No {@code Spell} implementation exists in main source yet
 * (no Magia catalog does — see {@link SpellCastingService}'s own TODO), and every consumer wired
 * so far reads only which Perícia delivers the Magia, or merely that there <em>is</em> one — so
 * this is a plain hand-written stub rather than a Fixture Factory template or a builder. Lombok
 * is main-source-only in this project, so a test stub has to be written out either way.
 *
 * <p>Describes an ordinary ranged attack Magia: delivered by Ataque à Distância, cast with
 * Domínio do Mana, reaching a single target at Distância Média, and sitting on {@link
 * TestSpellTree#DIVERGING}'s trunk at SEMENTE. Every column other than those returns a neutral
 * value — assert on one only after giving it a real one here.
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
    public SkillType getAttackSkillType() {
        return attackSkillType;
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
    public String getSecondaryEffectDescription() {
        return null;
    }

    @Override
    public CriticalEffectType getCriticalEffectType() {
        return null;
    }

    @Override
    public SkillType getConjurationSkillType() {
        return SkillType.DOMINIO_DO_MANA;
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
    public int getDuration() {
        return 0;
    }

    @Override
    public SpellTargeting getTargeting() {
        return SpellTargeting.distancia(Range.DISTANCIA_MEDIA);
    }
}
