package org.aventyrs.core.item;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.ability.ItemActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.profissao.ProfissaoSpecialization;

import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.ACTIVE_ABILITY_REQUIRES_REGALIA;
import static org.aventyrs.core.util.TranslatableMessages.CRAFTING_TRADE_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.DUPLICATE_IMPROVEMENT;
import static org.aventyrs.core.util.TranslatableMessages.IMPROVEMENT_SLOTS_FULL;
import static org.aventyrs.core.util.TranslatableMessages.ITEM_NOT_A_MASTERPIECE;
import static org.aventyrs.core.util.TranslatableMessages.MASTERPIECE_GRADUATION_TOO_LOW;
import static org.aventyrs.core.util.TranslatableMessages.REGALIA_CRAFTING_NOT_PERMITTED;
import static org.aventyrs.core.util.TranslatableMessages.REGALIA_DIVINE_DONOR_REQUIRED;
import static org.aventyrs.core.util.TranslatableMessages.REGALIA_DONOR_NOT_WILLING;

/**
 * One attempt at making an {@link Item} copy: an {@link ItemSpecification} (what to make) plus
 * who is making it and what powers the work. Build one with {@link #by} or {@link
 * #donatedByAventyr}, then {@link #validate()} to ask whether it may proceed, or {@link #forge()}
 * to validate and produce the copy.
 *
 * <pre>
 * // a crafter forging a Regalia Menor on a masterwork ring
 * Item regalia = ItemForgery.by(crafter, ProfissaoSpecialization.JOALHERIA,
 *                 ItemSpecification.regalia(RingItem.ANEL_DE_PRATA, RegaliaGrade.MENOR),
 *                 RegaliaDonation.willingDonor())
 *         .forge();
 *
 * // the GM handing the party a Regalia Divina, free
 * Item gift = ItemForgery.donatedByAventyr(
 *         ItemSpecification.regalia(RingItem.ANEL_DE_PRATA, RegaliaGrade.DIVINA)).forge();
 * </pre>
 *
 * <h2>Two ways to make a thing</h2>
 * <ul>
 *   <li><b>{@link #by} — a character forges it.</b> Every gate below applies, and the copy is
 *   stamped with its maker ({@code producedByCharacterId}), scaled by their Dureza abilities, and
 *   — for a Regalia — added to their "criação de 3 ou mais Regalias" history.</li>
 *   <li><b>{@link #donatedByAventyr} — Aventyr gives it.</b> <b>No crafter, no gates, no
 *   costs</b>: no trade, no Talento permission, no Centelha donor, no Obra-Prima Graduação
 *   floor, and no Aprimoramento slot or duplicate check. The copy is marked {@link
 *   Item#isDonatedByAventyr()} and has no maker. This is the GM path the {@code
 *   EquipmentCraftingService} javadoc describes as living outside it — it is deliberately not a
 *   method on that service, because every number that service exists to compute (GD, days, PE)
 *   is exactly what a donation skips.</li>
 * </ul>
 *
 * <h2>What a forge checks</h2>
 * In order, and only on the {@link #by} path: the crafter holds {@code trade} as a Profissão
 * Especialização ({@code CRAFTING_TRADE_NOT_HELD}); for a Regalia, some Talento they hold
 * <b>permits that grade right now</b> ({@code REGALIA_CRAFTING_NOT_PERMITTED}), the donation is
 * willing ({@code REGALIA_DONOR_NOT_WILLING}) and, for a {@link RegaliaGrade#DIVINA}, from a
 * Dragão / Elemental / Abissal / Celestial ({@code REGALIA_DIVINE_DONOR_REQUIRED}); their
 * Profissão Graduação meets the Obra-Prima's Raridade floor ({@code
 * MASTERPIECE_GRADUATION_TOO_LOW}); and the Aprimoramentos asked for fit — an Obra-Prima to host
 * them ({@code ITEM_NOT_A_MASTERPIECE}), within the base's Categoria de Peso cap ({@code
 * IMPROVEMENT_SLOTS_FULL}), none repeated ({@code DUPLICATE_IMPROVEMENT}). Those last three
 * mirror {@code EquipmentCraftingService#installImprovement} exactly, because fitting an
 * Aprimoramento during fabrication and fitting one afterwards are the same rule.
 *
 * <p><b>The permission is one call, and it carries the use-conditions with it.</b> This class
 * never asks "does the crafter hold {@code ARTESAO_DE_REGALIAS_MENOR}" — it asks every held
 * Talento what it permits, and a Talento whose own use-conditions aren't met (no Regalia in
 * possession) answers {@code null}. That is what keeps "what you needed to learn it" and "what
 * you need in hand to use it" from collapsing into one list of prerequisites; see {@code
 * Feat#itsAllowedToCraftRegalia}. It also means an outside {@code Feat} implementation can permit
 * a grade with no change here.
 *
 * <p><b>What it never checks</b>, because this core doesn't model it: the Profissão roll itself
 * (the caller resolves it against {@code EquipmentCraftingService#getFabricationDifficulty} /
 * {@code #getRegaliaCraftingDifficulty}, and for a Regalia Divina must additionally confirm the
 * Acerto Crítico {@link RegaliaGrade#requiresCriticalResult()} demands); the donor's Centelhas
 * actually being spent (untracked — see {@link RegaliaDonation}); the Forja do Olho de Deus a
 * Divina must be made in (this core models no places); and the PE cost (no PE economy).
 *
 * <h2>Building the piece up, and what it costs</h2>
 * A forge is assembled a decision at a time: {@link #setMasterpiece}, {@link #addImprovement} and
 * {@link #setActiveAbility} each add one more thing to the item being made, and each is a
 * <em>separate</em> commissioned piece of work with a price of its own. So every one of them
 * re-totals {@link #getTotalValue()} — the worth of what is being made, in PE: the base
 * Equipamento's Preço plus each fitted Obra-Prima's, Aprimoramento's and item ability's own
 * {@code getPriceModifier()}. {@link #getForgingCost()} is what the crafter pays for it:
 * <b>half that total</b>, once everything has been summed, never below 1 — the same "metade do
 * Preço" rule {@code EquipmentCraftingService#getFabricationCost} states for a bare template,
 * which is exactly this figure when nothing has been added.
 *
 * <p>Order does not matter and nothing is charged twice: the total is <em>recomputed</em> from
 * the whole specification on every change, not accumulated, so replacing an Obra-Prima with a
 * cheaper one lowers the figure again. <b>Halve once, at the end</b> — each part contributes its
 * full Preço to the total and the single halving happens in {@code getForgingCost()}, so adding
 * two 10-PE Aprimoramentos costs 10, never 5+5 rounded twice.
 *
 * <p><b>Every catalog Preço modifier is 0 today</b> — no Obra-Prima, Aprimoramento or item
 * ability authors one (CLAUDE.md's "Item numeric columns" gap), so a real forge's total currently
 * equals its base Preço. The arithmetic is real regardless; authoring a modifier prices every
 * forge that uses it with no further wiring. <b>And the cost is reported, not spent</b>: this
 * core has no PE economy, so nothing deducts it from anyone.
 *
 * <p><b>A forgery is a description, not a receipt.</b> The mutators change what will be made;
 * only {@link #forge()} makes it — and it mutates the crafter, advancing the Regalia history.
 * Calling {@code forge()} twice therefore makes two items, exactly as calling the service twice
 * would. The mutators themselves judge nothing: an Aprimoramento past the weight-class cap is
 * recorded, priced, and refused by {@link #validate()} — the same
 * builders-aren't-gatekeepers split the rest of this codebase keeps.
 */
@Getter
public final class ItemForgery {

    /** The crafter, or {@code null} on the {@link #donatedByAventyr} path. */
    private final Character crafter;

    /** The Profissão Especialização the work is done under, or {@code null} for a donation. */
    private final ProfissaoSpecialization trade;

    /** What is being made — replaced wholesale by each mutator below. */
    private ItemSpecification specification;

    /** The Centelha donation powering a Regalia forge; {@code null} for an ordinary item or a donation. */
    private final RegaliaDonation donation;

    /**
     * What the piece being made is worth in PE right now — see {@link #getTotalValue()}. Held
     * rather than derived on demand because the mutators are where a caller watches it change,
     * and re-totalled by {@link #recalculateTotalValue()} after each of them.
     */
    private int totalValue;

    private ItemForgery(final Character crafter, final ProfissaoSpecialization trade,
                        final ItemSpecification specification, final RegaliaDonation donation) {
        this.crafter = crafter;
        this.trade = trade;
        this.specification = specification;
        this.donation = donation;
        recalculateTotalValue();
    }

    /**
     * A forge worked by crafter in trade, powered by donation — required when {@code
     * specification} names a {@link RegaliaGrade}, and {@code null} otherwise (an ordinary item
     * costs no Centelhas).
     */
    public static ItemForgery by(@NonNull final Character crafter,
                                 @NonNull final ProfissaoSpecialization trade,
                                 @NonNull final ItemSpecification specification,
                                 final RegaliaDonation donation) {
        return new ItemForgery(crafter, trade, specification, donation);
    }

    /** A forge of an ordinary (non-Regalia) item, which needs no Centelha donation. */
    public static ItemForgery by(@NonNull final Character crafter,
                                 @NonNull final ProfissaoSpecialization trade,
                                 @NonNull final ItemSpecification specification) {
        return by(crafter, trade, specification, null);
    }

    /**
     * An item Aventyr itself hands over — the GM path. Validates nothing and costs nothing; the
     * copy is marked {@link Item#isDonatedByAventyr()} and carries no maker, so a Regalia given
     * this way is distinguishable from one a crafter earned, and advances nobody's craft history.
     */
    public static ItemForgery donatedByAventyr(@NonNull final ItemSpecification specification) {
        return new ItemForgery(null, null, specification, null);
    }

    /** Whether this forge is Aventyr's gift rather than a character's work. */
    public boolean isDonation() {
        return crafter == null;
    }

    /**
     * Fabricates the piece as {@code masterpiece} — one commissioned decision, re-priced into
     * {@link #getTotalValue()}. Replaces any Obra-Prima already asked for (an item carries at
     * most one), and {@code null} clears it, dropping its price again.
     *
     * <p>Validates nothing: whether the crafter's Profissão Graduação reaches this Obra-Prima's
     * Raridade is {@link #validate()}'s question, asked once at the end.
     *
     * @return this forgery, so decisions can be chained
     */
    public ItemForgery setMasterpiece(final ItemMasterpiece masterpiece) {
        specification = specification.toBuilder().masterpiece(masterpiece).build();
        recalculateTotalValue();
        return this;
    }

    /**
     * Fits one more Aprimoramento, appending to whatever the specification already named — one
     * commissioned decision, re-priced into {@link #getTotalValue()}.
     *
     * <p>Validates nothing: the Obra-Prima that must host it, the Categoria de Peso cap and the
     * no-duplicates rule are all {@link #validate()}'s, so a caller may add freely and be refused
     * once, coherently, at the end.
     *
     * @return this forgery, so decisions can be chained
     */
    public ItemForgery addImprovement(@NonNull final Improvement improvement) {
        specification = specification.toBuilder().improvement(improvement).build();
        recalculateTotalValue();
        return this;
    }

    /**
     * Binds an {@link ItemActiveAbility} into the piece — one commissioned decision, re-priced
     * into {@link #getTotalValue()}. {@code null} clears it.
     *
     * <p>Only a Regalia may carry one; a forge asking for an ability without a {@link
     * RegaliaGrade} is refused by {@link #validate()} ({@code ACTIVE_ABILITY_REQUIRES_REGALIA}),
     * not here.
     *
     * @return this forgery, so decisions can be chained
     */
    public ItemForgery setActiveAbility(final ItemActiveAbility activeAbility) {
        specification = specification.toBuilder().activeAbility(activeAbility).build();
        recalculateTotalValue();
        return this;
    }

    /**
     * What the piece being made is worth, in PE: the base Equipamento's Preço plus every fitted
     * Obra-Prima's, Aprimoramento's and item ability's own {@code getPriceModifier()}. Recomputed
     * from the whole specification after each mutator, so it never double-counts and always
     * reflects what is currently asked for. See the class javadoc for why it reads as the base
     * Preço alone today.
     */
    public int getTotalValue() {
        return totalValue;
    }

    /**
     * What forging this costs its crafter, in PE — <b>half {@link #getTotalValue()}</b>, floored,
     * never below 1, matching {@code EquipmentCraftingService#getFabricationCost}'s "metade do
     * Preço" for a bare template. The halving happens once, here, over the summed total.
     *
     * <p><b>0 for a donation</b>: Aventyr charges nobody, which is the difference between what a
     * thing is worth (still reported by {@link #getTotalValue()}) and what it costs to have made.
     * And on either path the figure is reported, never spent — no PE economy exists.
     */
    public int getForgingCost() {
        return isDonation() ? 0 : Math.max(1, totalValue / 2);
    }

    /** Sums the base Preço and every fitted part's own modifier. See {@link #getTotalValue()}. */
    private void recalculateTotalValue() {
        int value = specification.getBase().getPrice();
        if (specification.getMasterpiece() != null) {
            value += specification.getMasterpiece().getPriceModifier();
        }
        for (Improvement improvement : specification.getImprovements()) {
            value += improvement.getPriceModifier();
        }
        if (specification.getActiveAbility() != null) {
            value += specification.getActiveAbility().getPriceModifier();
        }
        totalValue = value;
    }

    /**
     * Throws unless this forge may proceed — see the class javadoc for the checks and their
     * messages, in order. Returns silently when everything holds (always, for a donation), so a
     * caller can ask before committing to the days of work {@code EquipmentCraftingService}
     * prices.
     */
    public void validate() throws IllegalOperationException {
        if (isDonation()) {
            return;
        }
        requireTrade();
        if (specification.isRegalia()) {
            requireRegaliaPermissionAndDonation();
        }
        requireMasterpieceSkill();
        requireImprovementsFit();
        requireActiveAbilityHost();
    }

    /**
     * Whether any Talento the crafter holds permits this specification's {@link
     * RegaliaGrade} <em>right now</em> — the permission scan, with each Talento judging its own
     * use-conditions. The non-throwing form of {@code validate()}'s Regalia check, so a UI can
     * grey out a grade rather than offer a forge that will be refused. An ordinary (non-Regalia)
     * specification needs no permission and reports {@code true}; so does a donation.
     */
    public boolean isPermitted() {
        if (isDonation() || !specification.isRegalia()) {
            return true;
        }
        return crafter.getFeats().stream()
                .anyMatch(feat -> feat.itsAllowedToCraftRegalia(crafter) == specification.getRegaliaGrade());
    }

    /**
     * Validates, then produces the copy: built from the specification's base, marked with its
     * {@link RegaliaGrade} if it names one, fitted with its Obra-Prima and Aprimoramentos, its
     * Dureza scaled by every {@code SkillCompetencyAbility#resolveProducedHardnessMultiplier} the
     * crafter holds ({@code AUMENTAR_A_DUREZA}'s +50%) and stamped with {@code
     * producedByCharacterId} — or, for a donation, unscaled, unstamped and marked as Aventyr's.
     * A forged Regalia is recorded on the crafter's Regalia history.
     *
     * <p>The copy is <b>returned, not equipped</b>: what its owner does with it is the caller's
     * business.
     */
    public Item forge() throws IllegalOperationException {
        validate();
        AbstractItem forged = AbstractItem.builderFromTemplate(specification.getBase())
                .hardness((int) Math.floor(specification.getBase().getHardness() * hardnessFactor()))
                .producedByCharacterId(isDonation() ? null : crafter.getId())
                .donatedByAventyr(isDonation())
                .regaliaGrade(specification.getRegaliaGrade())
                .build();
        if (specification.getMasterpiece() != null) {
            forged.setMasterpiece(specification.getMasterpiece());
        }
        specification.getImprovements().forEach(forged::addImprovement);
        if (specification.getActiveAbility() != null) {
            forged.setActiveAbility(specification.getActiveAbility());
        }
        if (!isDonation() && specification.isRegalia()) {
            crafter.recordRegaliaCrafted(specification.getRegaliaGrade());
        }
        return forged;
    }

    private void requireTrade() throws IllegalOperationException {
        CharacterSkill profissao = crafter.getSkills().get(SkillType.PROFISSAO);
        if (profissao == null || !profissao.getSpecializations().contains(trade)) {
            throw new IllegalOperationException(CRAFTING_TRADE_NOT_HELD);
        }
    }

    private void requireRegaliaPermissionAndDonation() throws IllegalOperationException {
        if (!isPermitted()) {
            throw new IllegalOperationException(REGALIA_CRAFTING_NOT_PERMITTED);
        }
        if (donation == null || !donation.willing()) {
            throw new IllegalOperationException(REGALIA_DONOR_NOT_WILLING);
        }
        if (specification.getRegaliaGrade().requiresExternalDonor() && !donation.isDivineDonor()) {
            throw new IllegalOperationException(REGALIA_DIVINE_DONOR_REQUIRED);
        }
    }

    /**
     * An {@link ItemActiveAbility} has nowhere to live on an ordinary item — {@code
     * AbstractItem#setActiveAbility} refuses one on a non-Regalia, and this reports that as a
     * refusal a caller can render rather than letting the setter throw mid-forge. Checked on the
     * crafted path only: a donation skips every gate, and {@link #forge()} orders the two setters
     * so the grade is already on the copy before the ability is.
     */
    private void requireActiveAbilityHost() throws IllegalOperationException {
        if (specification.getActiveAbility() != null && !specification.isRegalia()) {
            throw new IllegalOperationException(ACTIVE_ABILITY_REQUIRES_REGALIA);
        }
    }

    private void requireMasterpieceSkill() throws IllegalOperationException {
        ItemMasterpiece masterpiece = specification.getMasterpiece();
        if (masterpiece == null) {
            return;
        }
        int required = masterpiece.getDefinition().getRarity().getMinimumMasterpieceGraduation();
        CharacterSkill profissao = crafter.getSkills().get(SkillType.PROFISSAO);
        int graduation = profissao == null ? 0 : profissao.getGraduation().getGraduationValue();
        if (graduation < required) {
            throw new IllegalOperationException(MASTERPIECE_GRADUATION_TOO_LOW);
        }
    }

    /**
     * The same three rules {@code EquipmentCraftingService#installImprovement} applies one
     * Aprimoramento at a time, applied to the whole set at once — a specification asking for two
     * of the same Aprimoramento is refused on its own, before any is fitted.
     */
    private void requireImprovementsFit() throws IllegalOperationException {
        List<Improvement> improvements = specification.getImprovements();
        if (improvements.isEmpty()) {
            return;
        }
        if (specification.getMasterpiece() == null) {
            throw new IllegalOperationException(ITEM_NOT_A_MASTERPIECE);
        }
        if (improvements.size() > specification.getBase().getWeightClass().getMaximumImprovements()) {
            throw new IllegalOperationException(IMPROVEMENT_SLOTS_FULL);
        }
        if (improvements.stream().distinct().count() < improvements.size()) {
            throw new IllegalOperationException(DUPLICATE_IMPROVEMENT);
        }
    }

    /** A donation is unscaled: nobody worked it, so no crafter's abilities touch it. */
    private double hardnessFactor() {
        return isDonation() ? 1.0 : SkillCompetencyAbility.allFor(crafter).stream()
                .mapToDouble(SkillCompetencyAbility::resolveProducedHardnessMultiplier)
                .reduce(1.0, (a, b) -> a * b);
    }
}
