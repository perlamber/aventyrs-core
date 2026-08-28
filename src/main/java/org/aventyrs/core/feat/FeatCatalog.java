package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Every authored Talento, discovered reflectively from {@link Feat}'s own {@code permits}
 * clause — the single place to ask "what Talentos exist?", which no per-tree enum can answer on
 * its own.
 *
 * <h2>Why {@code getPermittedSubclasses()} and not a classpath scan</h2>
 *
 * Both are reflection; only one is trustworthy. Walking the classloader for {@code
 * org/aventyrs/core/feat/*.class} has to special-case exploded directories versus JAR entries
 * versus the module path, does real I/O on first call, and — worst — fails <em>silently and
 * partially</em> when it guesses wrong: a Talento tree that doesn't get scanned just quietly
 * isn't offered to the player.
 *
 * <p>Sealing {@link Feat} moves the same question to the compiler. {@code
 * Feat.class.getPermittedSubclasses()} returns the authoritative list with no I/O and no
 * guessing, and the permits clause is <b>compiler-enforced</b>: a new {@code <Tree>Feat} that
 * forgets to register simply does not compile. The failure mode moves from "a Talento silently
 * disappears at runtime" to "the build stops", which is the whole point.
 *
 * <p>Only permitted subclasses that are <em>enums</em> contribute constants; {@link
 * AbstractFeat} is permitted so consumers can extend it, and contributes nothing, which is
 * correct — see its javadoc.
 *
 * <h2>Initialisation</h2>
 *
 * Discovery lives here rather than as a {@code static} field on {@link Feat} itself, and that is
 * not stylistic. {@code Feat} declares {@code default} methods, so initialising {@code
 * MetamagicoFeat} initialises {@code Feat} first; had {@code Feat}'s own static initialiser
 * called {@code getEnumConstants()} back on {@code MetamagicoFeat}, it would observe that enum
 * mid-initialisation and could read its constants as {@code null}. A separate class breaks the
 * cycle: nothing triggers {@code FeatCatalog}'s initialiser except asking it a question.
 */
public final class FeatCatalog {

    /** Every authored Talento, in permits-clause order then declaration order within each tree. */
    private static final List<Feat> ALL = discover();

    private static final Map<FeatCategory, List<Feat>> BY_CATEGORY = indexByCategory();

    private FeatCatalog() {
    }

    /** Every authored Talento across every tree. */
    public static List<Feat> all() {
        return ALL;
    }

    /** Every authored Talento of one tree — empty for a {@link FeatCategory} with no enum yet. */
    public static List<Feat> in(final FeatCategory category) {
        return BY_CATEGORY.getOrDefault(category, List.of());
    }

    /**
     * Every authored Talento character currently satisfies the prerequisites for and does not
     * already hold — the "which Talentos can I pick?" list.
     *
     * <p>Eligibility only; it says nothing about whether the XP can be afforded. See {@code
     * FeatService#getAffordableFeats} for that, and {@code FeatService#getAvailableFeats} for the
     * same list reached through the service layer.
     */
    public static List<Feat> availableFor(final Character character) {
        return ALL.stream()
                .filter(feat -> !character.getFeats().contains(feat))
                .filter(feat -> feat.isEligible(character))
                .toList();
    }

    private static List<Feat> discover() {
        Class<?>[] permitted = Feat.class.getPermittedSubclasses();
        return Arrays.stream(permitted)
                .filter(Class::isEnum)
                .map(Class::getEnumConstants)
                .flatMap(Arrays::stream)
                .map(Feat.class::cast)
                .toList();
    }

    private static Map<FeatCategory, List<Feat>> indexByCategory() {
        Map<FeatCategory, List<Feat>> index = new EnumMap<>(FeatCategory.class);
        for (Feat feat : ALL) {
            index.computeIfAbsent(feat.getFeatCategory(), category -> new java.util.ArrayList<>()).add(feat);
        }
        index.replaceAll((category, feats) -> List.copyOf(feats));
        return Map.copyOf(index);
    }
}
