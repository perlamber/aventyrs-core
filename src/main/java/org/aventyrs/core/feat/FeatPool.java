package org.aventyrs.core.feat;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.race.Race;
import org.aventyrs.core.sheet.CharacterSheet;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * A set of Talentos a {@link StartingFeatSlot} draws from — "Talentos de Sobrevivência",
 * "Talentos Gerais", "Talentos de Especialista", "Talentos Raciais (conforme sua raça original)".
 *
 * <p>Two questions, kept apart because {@link RacialOf} answers the second one differently:
 * whether a Talento <em>belongs</em> to the pool ({@link #admits}), and whether a given character
 * may take it <em>from</em> this pool ({@link #isEligible}).
 */
public sealed interface FeatPool permits FeatPool.Categories, FeatPool.EspecialistaTagged, FeatPool.Named, FeatPool.RacialOf {

    /** Whether catalogEntry (an authored constant, never an acquired form) belongs to this pool. */
    boolean admits(Feat catalogEntry);

    /**
     * The trees this pool is bounded to — empty for a pool defined by something other than its
     * tree ({@link EspecialistaTagged} spans several, {@link Named} lists constants). What {@code AbstractMesticoRace} reads to
     * learn which racial trees a parent race's grant draws from.
     */
    Set<FeatCategory> categories();

    /** Whether character (and sheet, when given) may take feat from this pool. */
    default boolean isEligible(final Feat feat, final Character character, final CharacterSheet sheet) {
        return feat.isEligible(character, sheet);
    }

    /** Every Talento of one of categories, except the named excluded constants. */
    record Categories(Set<FeatCategory> categories, Set<Feat> excluded) implements FeatPool {

        public Categories {
            categories = Set.copyOf(categories);
            excluded = Set.copyOf(excluded);
        }

        public static Categories of(final FeatCategory... categories) {
            return new Categories(Set.of(categories), Set.of());
        }

        /** Every tree of one {@link FeatCategory.Type} — "Talentos Gerais" / "Talentos Raciais". */
        public static Categories ofType(final FeatCategory.Type type) {
            return new Categories(treesOf(type), Set.of());
        }

        /** This pool minus the named constants — Sátiros' "(exceto Pixie e Asas)". */
        public Categories excluding(final Feat... feats) {
            return new Categories(categories, Set.of(feats));
        }

        @Override
        public boolean admits(final Feat catalogEntry) {
            return categories.contains(catalogEntry.getFeatCategory()) && !excluded.contains(catalogEntry);
        }
    }

    /**
     * Exactly the named Talentos — a grant that names its Talento rather than a tree, like
     * Avianos' "também recebem Ossos Ocos como Talento adicional". A one-constant pool is a slot
     * with a single option; it still goes through that Talento's own eligibility.
     */
    record Named(Set<Feat> feats) implements FeatPool {

        public Named {
            feats = Set.copyOf(feats);
        }

        public static Named of(final Feat... feats) {
            return new Named(Set.of(feats));
        }

        @Override
        public boolean admits(final Feat catalogEntry) {
            return feats.contains(catalogEntry);
        }

        /** Bounded by its constants, not by a tree — so it never reads as "draws from Monstruoso". */
        @Override
        public Set<FeatCategory> categories() {
            return Set.of();
        }
    }

    /** Every Talento whose header carries the {@code Especialista} tag ({@link Feat#isEspecialistaTagged()}). */
    record EspecialistaTagged() implements FeatPool {

        @Override
        public boolean admits(final Feat catalogEntry) {
            return catalogEntry.isEspecialistaTagged();
        }

        @Override
        public Set<FeatCategory> categories() {
            return Set.of();
        }
    }

    /**
     * Talentos Raciais of categories whose <b>Raça clauses are judged against race</b> rather than
     * against the holder's own — Vampiros' "Talentos Raciais (conforme sua raça original)" and the
     * Mestiço Elementais' "você poderá substituir este Talento por um Talento Racial do tipo
     * especificado" by their parent. Neither holder is an instance of that race ({@code
     * AbstractMesticoRace} and {@code Vampiro} hold their parent rather than extending it), so the
     * plain check would refuse every such Talento on {@code requiredRace} alone.
     *
     * <p>Eligibility is asked of a copy of the holder wearing race, so every other clause — and
     * every {@code isEligible} override — is tested exactly as {@code FeatService#grantFeat} would.
     * The copy is only ever read.
     */
    record RacialOf(@NonNull Race race, Set<FeatCategory> categories) implements FeatPool {

        public RacialOf {
            categories = Set.copyOf(categories);
        }

        /** Every racial tree, judged against race. */
        public static RacialOf anyTree(final Race race) {
            return new RacialOf(race, treesOf(FeatCategory.Type.RACIAL));
        }

        @Override
        public boolean admits(final Feat catalogEntry) {
            return catalogEntry.getFeatCategory().getType() == FeatCategory.Type.RACIAL
                    && categories.contains(catalogEntry.getFeatCategory());
        }

        @Override
        public boolean isEligible(final Feat feat, final Character character, final CharacterSheet sheet) {
            return feat.isEligible(character.toBuilder().race(race).build(), sheet);
        }
    }

    private static Set<FeatCategory> treesOf(final FeatCategory.Type type) {
        return Arrays.stream(FeatCategory.values())
                .filter(category -> category.getType() == type)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(FeatCategory.class)));
    }
}
