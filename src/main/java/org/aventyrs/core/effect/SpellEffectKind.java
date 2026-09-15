package org.aventyrs.core.effect;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * The four categories a {@link SpellEffect} divides into, and <b>the registry of what builds
 * each</b> — one enum serving both roles.
 *
 * <p>Its constants are the complete list {@link SpellEffectFactory} walks, and the vocabulary for
 * naming which category an effect belongs to. It is <b>not</b> a per-cast selector: a Magia and
 * its {@code Efeito Alternativo} carry separate effect columns, so which effect a cast uses is
 * settled by which version it casts — {@code SpellCastRequest#isUseAlternateVersion()} — and each
 * version authors at most one.
 *
 * <p><b>Each constant carries its own builder</b>, rather than anything downstream branching on
 * the constant — the same one-constant-carries-its-own-lookup convention {@code
 * org.aventyrs.core.skill.SkillType} uses for {@code interactionFactory}/{@code excellencyClass},
 * and {@code org.aventyrs.core.character.DefenseType} for its {@code Item} column. A {@link
 * Supplier} specifically, because a builder is constructed fresh per call exactly as {@code
 * SkillType#newInteraction()} builds a fresh Interaction.
 *
 * <p><b>Adding a category is a builder class and a supplier here — nothing else.</b> That is the
 * whole reason this replaced an if-else on {@code SpellCastingServiceImpl}: filling in {@link
 * #OFFENSIVE} used to mean editing a service that orchestrates two rolls and a duration and has no
 * business knowing how a damage effect is constructed.
 *
 * <p>A {@code null} supplier means the category is declared but has no implementation yet. That is
 * deliberate rather than an omission — see each constant, and {@code OffensiveEffect}/{@code
 * InvocationEffect}'s own javadoc for what blocks them. {@code SpellEffectKindTest} guards the
 * list, the way {@code ItemCatalogTest} guards {@code ItemCatalog.CATALOG_ENUMS}.
 */
public enum SpellEffectKind {

    /** Restores Pontos de Vida — {@code Spell#getHealing()}, built by {@link HealingEffectBuilder}. */
    HEALING(HealingEffectBuilder::new),

    /**
     * Protects rather than harms — {@code Spell#getCleansedConditions()}, built by {@link
     * ConditionCleansingEffectBuilder}. A duration-bearing buff belongs on the {@code Blessing}
     * rail instead; see {@link DefensiveEffect}.
     */
    DEFENSIVE(ConditionCleansingEffectBuilder::new),

    /**
     * Damage and debuffs. No builder yet, and the gap is a consumer rather than a mechanism —
     * {@code SpellDamage} already resolves a Magia's damage and {@code applyCondition} already
     * inflicts a Malefício. See {@link OffensiveEffect}.
     */
    OFFENSIVE(null),

    /**
     * Conjures a creature or an item. No builder, and genuinely blocked: {@code Spell} has no
     * column pointing at a {@code MonsterTemplate}. See {@link InvocationEffect}.
     */
    INVOCATION(null);

    private final Supplier<SpellEffectBuilder> builderFactory;

    SpellEffectKind(final Supplier<SpellEffectBuilder> builderFactory) {
        this.builderFactory = builderFactory;
    }

    /**
     * A fresh builder for this category, or {@link Optional#empty()} for a category that declares
     * itself but cannot be built yet.
     */
    public Optional<SpellEffectBuilder> newBuilder() {
        return Optional.ofNullable(builderFactory).map(Supplier::get);
    }

    /** Whether this category can currently produce an effect at all. */
    public boolean isImplemented() {
        return builderFactory != null;
    }
}
