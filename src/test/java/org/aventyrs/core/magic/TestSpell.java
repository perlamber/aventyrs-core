package org.aventyrs.core.magic;

import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

/**
 * A minimal {@link Spell} for tests. No {@code Spell} implementation exists in main source yet
 * (no Magia catalog does — see {@link SpellCastingService}'s own TODO), and every consumer wired
 * so far reads only which Perícia delivers the Magia, or merely that there <em>is</em> one — so
 * this is a plain hand-written stub rather than a Fixture Factory template or a builder. Lombok
 * is main-source-only in this project, so a test stub has to be written out either way.
 *
 * <p>Describes an ordinary ranged attack Magia: delivered by Ataque à Distância, cast with
 * Domínio do Mana. Every other column returns a neutral value — assert on one only after giving
 * it a real one here.
 */
public class TestSpell implements Spell {

    private final SkillType attackSkillType;

    /** A Magia delivered by Ataque à Distância — the common case for these tests. */
    public TestSpell() {
        this(SkillType.ATAQUE_A_DISTANCIA);
    }

    /** A Magia delivered by attackSkillType — e.g. Ataque Corpo-a-Corpo for a Magia de Toque. */
    public TestSpell(final SkillType attackSkillType) {
        this.attackSkillType = attackSkillType;
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
    public CriticalEffectType getCriticalEffectType() {
        return null;
    }

    @Override
    public SkillType getConjurationSkillType() {
        return SkillType.DOMINIO_DO_MANA;
    }

    @Override
    public BranchLevel getBranchLevel() {
        return BranchLevel.BROTO;
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
    public SpellReach getReach() {
        return null;
    }
}
