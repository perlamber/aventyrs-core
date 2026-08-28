package org.aventyrs.core.magic;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.ability.MagiaAlternativaAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * A Magia. It is an {@link AttackSource}: casting one at somebody is an attack delivered by
 * {@link #getAttackSkillType()}, exactly as swinging a {@code Weapon} is — which is what lets a
 * delivery-scoped ability such as {@code AtaqueADistanciaCompetencyAbility#ARREMESSO_PODEROSO}
 * cover "armas de arremessos <b>e magias</b>" without either side knowing about the other.
 * {@code getAttackSkillType()} already existed and needed no change to satisfy that interface.
 */
public interface Spell extends AttackSource {

    DifficultyLevel getCastingDifficultyLevel();

    String getDescription();

    String getPrimaryEffectDescription();

    String getSecondaryEffectDescription();

    CriticalEffectType getCriticalEffectType();

    SkillType getConjurationSkillType();

    @Override
    SkillType getAttackSkillType();

    /**
     * How deep in {@link #getTree()} this Magia sits — both its Mana cost and its rung on the
     * tree, which is what {@link #isEligible}'s cap and climb gates read.
     */
    BranchLevel getBranchLevel();

    /** The Árvore de Magia this Magia belongs to. */
    SpellTree getTree();

    /**
     * Which ramificação of {@link #getTree()} this Magia sits on, or {@link Optional#empty()}
     * when it sits on the <b>trunk</b> — before the tree diverges, or after its branches
     * converge again. A trunk Magia is on every path, which is the whole convergence mechanism;
     * see {@link SpellTree}.
     */
    Optional<SpellBranch> getBranch();

    MagicType getPrimaryType();

    MagicType getSecondaryType();

    int getDuration();

    /**
     * How this Magia reaches what it affects — the {@link SpellReach} together with the
     * {@code Range}/{@code AreaOfEffect} that reach requires. The bare classification is still
     * reachable as {@code getTargeting().reach()}.
     */
    SpellTargeting getTargeting();

    /**
     * Whether character may acquire this Magia right now — <b>three independent gates</b>, all
     * of which must hold, mirroring {@code org.aventyrs.core.feat.Feat#isEligible}/{@code
     * AventyrTitleAbility#isEligible}'s own combine-every-prerequisite shape. Checked by {@code
     * org.aventyrs.core.character.services.SpellService#grantSpell} before granting.
     *
     * <ol>
     *   <li><b>Cap</b> — maxBranchLevel must reach this Magia's own {@link #getBranchLevel()}.
     *       Until a Talento raises it, a Conjurador spends sideways: more Magias at their cap
     *       level, from other Árvores.</li>
     *   <li><b>Climb</b> — unless this is a {@link BranchLevel#SEMENTE} Magia (a tree's entry
     *       point, which rests on nothing), character must already hold a Magia <em>in this same
     *       tree</em> at the immediately shallower rung. A foothold in a different tree never
     *       counts.</li>
     *   <li><b>Branch</b> — character must hold no Magia in this tree on a <em>different</em>
     *       ramificação. Trunk Magias belong to no branch, so they neither commit a Conjurador
     *       nor can ever be refused — which is exactly how a tree's branches converge.</li>
     * </ol>
     *
     * <p>The branch gate alone has an exemption: {@code
     * org.aventyrs.core.ability.MagiaAlternativaAbility} held for this tree's {@link
     * SpellTree#getMagicType()} ("você pode aprender magias de ambas as ramificações dos tipos
     * de magia escolhidos"). It does not loosen the cap or the climb.
     *
     * <p><b>All three are derived, never stored.</b> There is no "chosen branch" field and no
     * unlocked-levels counter — a Conjurador's branch in a tree simply <em>is</em> whichever
     * ramificação their acquired Magias sit on, so the answer changes by itself as they acquire.
     * Same recompute-on-demand discipline as {@code HitPointsService#getStatus} and {@code
     * InitiativeEntry#getEffectiveInitiativeValue}.
     *
     * @param maxBranchLevel the character's general cap, already resolved by {@code
     *     SpellService#getMaxBranchLevel} — passed in rather than scanned here so this stays a
     *     pure function over data in hand, the same shape {@code DamageServiceImpl} uses when it
     *     resolves {@code hasLowerPvAdjacentAlly} for a hook.
     */
    default boolean isEligible(final Character character, final BranchLevel maxBranchLevel) {
        return withinCap(maxBranchLevel)
                && hasFootholdAtPreviousLevel(character)
                && branchIsAvailable(character);
    }

    /** Gate 1 — the character's general cap reaches this Magia's own depth. */
    private boolean withinCap(final BranchLevel maxBranchLevel) {
        return maxBranchLevel != null && maxBranchLevel.isAtLeast(getBranchLevel());
    }

    /** Gate 2 — a Magia of this same tree is already held one rung shallower. */
    private boolean hasFootholdAtPreviousLevel(final Character character) {
        Optional<BranchLevel> requiredLevel = getBranchLevel().previous();
        if (requiredLevel.isEmpty()) {
            return true;
        }
        return spellsInSameTree(character)
                .anyMatch(held -> held.getBranchLevel() == requiredLevel.get());
    }

    /** Gate 3 — no Magia of this tree is held on a different ramificação, or the type is exempt. */
    private boolean branchIsAvailable(final Character character) {
        SpellTree.validateBranches(getTree());

        Optional<SpellBranch> branch = getBranch();
        if (branch.isEmpty() || isExemptFromBranchLock(character)) {
            return true;
        }
        return spellsInSameTree(character)
                .flatMap(held -> held.getBranch().stream())
                .allMatch(held -> held == branch.get());
    }

    /** Whether MAGIA_ALTERNATIVA was chosen for this tree's own Tipo de Magia. */
    private boolean isExemptFromBranchLock(final Character character) {
        return character.getAttributeAbilities().stream()
                .anyMatch(this::exempts);
    }

    private boolean exempts(final AttributeAbility ability) {
        return ability instanceof MagiaAlternativaAbility alternativa
                && alternativa.getMagicType() == getTree().getMagicType();
    }

    private java.util.stream.Stream<Spell> spellsInSameTree(final Character character) {
        return character.getSpells().stream().filter(held -> held.getTree() == getTree());
    }
}
