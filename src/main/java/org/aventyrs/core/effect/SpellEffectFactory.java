package org.aventyrs.core.effect;

import org.aventyrs.core.magic.Spell;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Builds the {@link SpellEffect} a Magia's authored columns describe — the single entry point for
 * turning a {@link Spell} into something applicable.
 *
 * <p>A thin facade, deliberately: it holds no mapping of its own and knows no effect class. {@link
 * SpellEffectKind} carries which builder serves each category, and each {@link SpellEffectBuilder}
 * knows which column it reads — this is just where a caller asks. Same division {@code
 * org.aventyrs.core.skill.SkillInteractionFactory} keeps over {@code SkillType#newInteraction()},
 * and the reason both are {@code final} with a private constructor.
 *
 * <h2>One version authors one effect, so there is nothing to disambiguate</h2>
 *
 * A Magia and its {@code Efeito Alternativo} are <b>two separate {@link Spell}s</b>, each carrying
 * its own effect columns — an {@code AlternateSpellVersion} reports the alternate's {@code
 * getHealing()}/{@code getCleansedConditions()}, never its parent's. So "which effect does this
 * cast use" is answered entirely by <b>which version is being cast</b>, which is {@code
 * SpellCastRequest#isUseAlternateVersion()}'s job, and by the time a {@link Spell} reaches this
 * factory that choice is already made.
 *
 * <p>That leaves each version authoring exactly one effect, so this simply builds it — no
 * precedence rule, no selector, and no ambiguity to refuse. <b>The invariant is pinned by {@code
 * SpellEffectFactoryTest#noVersionAuthorsMoreThanOneEffect}</b>, which walks every Magia in the
 * catalog <em>and</em> every second version, rather than by a runtime throw: an authoring mistake
 * should stop the build, not a cast. Same discipline {@code ItemCatalogTest} follows for {@code
 * ItemCatalog.CATALOG_ENUMS}.
 *
 * <p><b>Nothing is applied.</b> A built effect mutates a sheet only when a caller runs it through
 * {@code CombatantSheet#receiveInteraction}; see {@link SpellEffect}.
 */
public final class SpellEffectFactory {

    private SpellEffectFactory() {
    }

    /**
     * The effect this version of the Magia authors, or {@link Optional#empty()} when it authors
     * none this core can express yet — still most of the catalog.
     *
     * <p>spell is whichever version is actually being cast: pass {@code
     * Spell#getAlternateVersion()} to build the {@code Efeito Alternativo}'s effect instead of the
     * base one. {@code SpellCastingService#castSpell} resolves that from the request before
     * calling here.
     */
    public static Optional<SpellEffect> create(final Spell spell, final SpellEffectContext context) {
        return Arrays.stream(SpellEffectKind.values())
                .flatMap(kind -> kind.newBuilder().stream())
                .flatMap(builder -> builder.build(spell, context).stream())
                .findFirst();
    }

    /** Every category spell actually authors an effect for — empty for most of the catalog. */
    public static List<SpellEffectKind> authoredKinds(final Spell spell, final SpellEffectContext context) {
        return Arrays.stream(SpellEffectKind.values())
                .filter(kind -> kind.newBuilder()
                        .map(builder -> builder.build(spell, context).isPresent())
                        .orElse(false))
                .toList();
    }
}
