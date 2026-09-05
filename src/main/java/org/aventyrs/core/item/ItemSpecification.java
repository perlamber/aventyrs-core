package org.aventyrs.core.item;

import lombok.Builder;
import org.aventyrs.core.ability.ItemActiveAbility;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;

import java.util.List;

/**
 * Everything needed to produce one owned {@link Item} copy, in one value — the input {@link
 * ItemForgery} works from. An {@link ItemTemplate} alone answers only "which piece of Equipamento
 * is this"; a real forge also decides whether the copy is an Obra-Prima, which Aprimoramentos are
 * fitted to it, and whether it is a Regalia — none of which the catalog entry knows.
 *
 * <pre>
 * ItemSpecification.builder()
 *         .base(ArmorItem.ARMADURA_COMPLETA)
 *         .masterpiece(DefensiveMasterpiece.ACO_VULCANO)
 *         .improvement(new ItemImprovement(DefensiveImprovement.ENCAIXE))
 *         .regaliaGrade(RegaliaGrade.MENOR)
 *         .build();
 * </pre>
 *
 * <p><b>A description, not a copy.</b> Holding one changes nothing and produces nothing — it is
 * the "what to make" half, and {@link ItemForgery} is the "may you, and go" half. The same
 * specification can be forged twice, yielding two independent items, which is what a GM stocking
 * a shop needs.
 *
 * <p><b>What is deliberately absent.</b> Per-copy state that is <i>acquired</i> rather than
 * <i>made</i> stays off this: damage taken (a fresh copy has none), the {@code
 * producedByCharacterId} stamp and the Aventyr-donation marker (the forge decides those from
 * <i>who</i> is forging, not from what is asked for), and a socketed {@link PowerStone} — a stone
 * is fitted to a finished item through {@link AbstractItem#setPowerStone}, which needs the
 * Encaixe Aprimoramento already in place.
 *
 * <p>Like every builder in this codebase it validates nothing (CLAUDE.md's "Builder-bypassable
 * invariants") — a specification naming three Aprimoramentos on a Leve item, or an Obra-Prima its
 * crafter is not skilled enough to fit, builds fine and is refused by {@link
 * ItemForgery#validate()} rather than here.
 */
@Getter
@Builder(toBuilder = true)
public class ItemSpecification {

    /** The catalog entry being made — the only column with no sensible default. */
    @NonNull
    private final ItemTemplate base;

    /**
     * The Obra-Prima this copy is fabricated as, or {@code null} for an ordinary piece. Typed as
     * {@link ItemMasterpiece} — the fitted wrapper carrying its creation choices — rather than
     * the wider {@link Masterpiece}, because only the wrapper knows its catalog definition, and
     * that definition's {@link ItemRarity} is what sets the Profissão Graduação a crafter needs
     * to fit it ({@link ItemForgery#validate()}).
     */
    private final ItemMasterpiece masterpiece;

    /**
     * The Aprimoramentos fitted during fabrication, in order. Capped by the base's Categoria de
     * Peso ({@code 1/2/3}) and requiring an Obra-Prima to host them — both checked by {@link
     * ItemForgery#validate()}, not here.
     */
    @Singular
    private final List<Improvement> improvements;

    /**
     * The {@link RegaliaGrade} this copy is forged as, or {@code null} for an ordinary item.
     * <b>Its presence is what makes a forge a Regalia forge</b>: it is what {@link ItemForgery}
     * asks the crafter's Talentos for permission to make, what demands a Centelha donation, and
     * what the finished copy is marked with.
     */
    private final RegaliaGrade regaliaGrade;

    /**
     * The {@link org.aventyrs.core.ability.ItemActiveAbility} bound into this copy, or {@code
     * null}. Only a Regalia may carry one — a specification naming an ability without a {@link
     * #regaliaGrade} is refused by {@link ItemForgery#validate()} ({@code
     * ACTIVE_ABILITY_REQUIRES_REGALIA}), not here.
     */
    private final ItemActiveAbility activeAbility;

    /** The plainest specification there is: a catalog item, forged as-is. */
    public static ItemSpecification of(@NonNull final ItemTemplate base) {
        return ItemSpecification.builder().base(base).build();
    }

    /** A Regalia of grade built on base, with no Obra-Prima or Aprimoramento of its own. */
    public static ItemSpecification regalia(@NonNull final ItemTemplate base,
                                            @NonNull final RegaliaGrade grade) {
        return ItemSpecification.builder().base(base).regaliaGrade(grade).build();
    }

    /** Whether this describes a Regalia — {@code true} exactly when {@link #getRegaliaGrade()} is set. */
    public boolean isRegalia() {
        return regaliaGrade != null;
    }
}
