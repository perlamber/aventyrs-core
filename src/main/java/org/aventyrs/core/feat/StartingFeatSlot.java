package org.aventyrs.core.feat;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CharacterSheet;

import java.util.List;
import java.util.stream.Stream;

/**
 * One Talento a character picks at creation, free of XP — one of the two General Talentos every
 * character starts with, or one a Raça's "Perícias e Talentos" clause grants. See {@code
 * CharacterCreationService#getStartingFeatSlots}.
 *
 * <p>A slot draws from the <b>union</b> of its pools: a Talento fits when any pool both admits it
 * and judges it eligible for the holder. The union is what "escolhido entre Talentos de
 * Sobrevivência ou Destino" and "um Talento de Mobilidade, que você poderá substituir por um
 * Talento Racial" both are.
 *
 * @param source who grants the slot — for a client's label only; it changes nothing about the pick
 * @param pools  the Talento sets this slot draws from; never empty
 */
public record StartingFeatSlot(@NonNull Source source, List<FeatPool> pools) {

    public StartingFeatSlot {
        pools = List.copyOf(pools);
        if (pools.isEmpty()) {
            throw new IllegalArgumentException("A starting Talento slot must draw from at least one pool");
        }
    }

    /** One of the General Talentos every character starts with. */
    public static StartingFeatSlot defaultGeneral() {
        return new StartingFeatSlot(Source.DEFAULT, List.of(FeatPool.Categories.ofType(FeatCategory.Type.GERAL)));
    }

    /** A slot a Raça grants, drawing from the union of pools. */
    public static StartingFeatSlot race(final FeatPool... pools) {
        return new StartingFeatSlot(Source.RACE, List.of(pools));
    }

    /** A Raça slot drawing from the named trees — the common case. */
    public static StartingFeatSlot race(final FeatCategory... categories) {
        return race(FeatPool.Categories.of(categories));
    }

    /** This slot with one more pool unioned in — a Mestiço's parent-racial substitute. */
    public StartingFeatSlot or(final FeatPool pool) {
        return new StartingFeatSlot(source, Stream.concat(pools.stream(), Stream.of(pool)).toList());
    }

    /** Whether catalogEntry may fill this slot for character. */
    public boolean accepts(final Feat catalogEntry, final Character character, final CharacterSheet sheet) {
        return pools.stream().anyMatch(pool -> pool.admits(catalogEntry) && pool.isEligible(catalogEntry, character, sheet));
    }

    public enum Source {
        /** {@code CharacterCreationService#DEFAULT_GENERAL_FEAT_SLOTS} — every character's. */
        DEFAULT,
        /** {@code Race#getStartingFeatSlots()}. */
        RACE
    }
}
