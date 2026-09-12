package org.aventyrs.core.character;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

/**
 * An attribute's value is the sum of independent components: the natural Base a creature invested
 * in, the Racial Bonus its race confers — in <b>two</b> parts, see below — and a Variable component
 * from spells, feats or equipment.
 *
 * <p><b>The racial half is stored as two fields on purpose.</b> A race grants some bonuses outright
 * ({@link org.aventyrs.core.race.Race#getFixedAttributeBonuses()}) and lets the player spread
 * others freely at creation ({@link
 * org.aventyrs.core.race.Race#getChoosableAttributeBonusPoints()} over {@link
 * org.aventyrs.core.race.Race#getChoosableAttributes()}). Those used to be added together into one
 * {@code racialBonus} int by {@code CharacterCreationService}, which destroyed the provenance: with
 * only the sum, nothing downstream could tell what the race dictated from what its player chose.
 * Keeping them apart is worth doing for its own sake, and it is what lets "abandonando seus traços
 * raciais" ({@link org.aventyrs.core.race.RacialTraitSuppression}) know precisely how much of a
 * total is racial.
 *
 * <p>{@link #getRacialBonus()} still reports the sum, so every reader that only wants "how much of
 * this is racial" is unaffected — including {@code FeatRequirements}' "recebe Bônus Racial" clause,
 * whose question rightly spans both halves.
 */
@Builder(toBuilder = true)
@Getter
public class AttributeValue{
    @Builder.Default
    private int base = 1;

    /**
     * The part of the racial bonus the race dictates — {@code Race#getFixedAttributeBonuses()},
     * e.g. an Anão's Vigor. Not a player choice, and the same for every member of the race.
     */
    @Builder.Default
    private int fixedRacialBonus = 0;

    /**
     * The part of the racial bonus this character's player assigned at creation, from the pool
     * {@code Race#getChoosableAttributeBonusPoints()} allows over {@code
     * Race#getChoosableAttributes()}. Still a racial bonus — a Humano's spare points exist only
     * because they are Humano — but directed rather than dictated.
     */
    @Builder.Default
    private int chosenRacialBonus = 0;

    @Builder.Default
    private int variable = 0;
    @NonNull
    private AttributeDomain domain;

    public int getTotal() {
        return base + getRacialBonus() + variable;
    }

    /**
     * The whole racial contribution, dictated and chosen together — what a caller asking "did this
     * Atributo receive a Bônus Racial, and how much" wants. Derived, so the two halves can never
     * disagree with it.
     */
    public int getRacialBonus() {
        return fixedRacialBonus + chosenRacialBonus;
    }

    public AttributeDomain domain(){
        return domain;
    }
}
