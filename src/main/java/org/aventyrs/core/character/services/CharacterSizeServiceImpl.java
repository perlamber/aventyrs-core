package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;

public class CharacterSizeServiceImpl implements CharacterSizeService {

    private final ModifierResolver modifierResolver;

    public CharacterSizeServiceImpl() {
        this(new ModifierResolverImpl());
    }

    public CharacterSizeServiceImpl(final ModifierResolver modifierResolver) {
        this.modifierResolver = modifierResolver;
    }

    /**
     * Two stages, in this order: whichever {@code Feat} <b>sets</b> a Categoria de Tamanho
     * outright replaces the character's own, then every shift — the {@code
     * ModifierType#SIZE_CATEGORY} {@code @Modifier} scan and every Talento's own {@code
     * resolveSizeCategoryIncrease} — is applied to whatever that left, clamped by {@link
     * SizeCategory#shift}.
     *
     * <p>The order is what the rules text implies: "sua Categoria de Tamanho muda para -2" states
     * what you <em>are</em>, and a later clause raising it by +1 raises that. Reversing the two
     * would let a shift be silently discarded by an override resolved afterwards.
     */
    @Override
    public SizeCategory getEffectiveSizeCategory(final Character character) {
        return resolve(character, 0, false);
    }

    /**
     * Adds the sheet's own round-scoped {@code SIZE_CATEGORY} bonus to the same two shift
     * sources — a Forma's size uplift, which is a {@code TemporaryBonus} rather than a standing
     * trait and so has nowhere to live on the {@code Character} — and is the only overload that
     * can see a Forma <b>suppressing</b> the racial base beneath it.
     */
    @Override
    public SizeCategory getEffectiveSizeCategory(final CombatantSheet sheet) {
        return resolve(sheet.getCharacter(), sheet.getTemporaryBonus(ModifierType.SIZE_CATEGORY),
                sheet.getRacialTraitSuppression().suppressesPhysicalTraits());
    }

    private SizeCategory resolve(final Character character, final int temporaryShift,
                                 final boolean racialBaseSuppressed) {
        int bonus = modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.SIZE_CATEGORY);
        bonus += character.getFeats().stream()
                .mapToInt(feat -> feat.resolveSizeCategoryIncrease(character))
                .sum();
        return resolveBaseSizeCategory(character, racialBaseSuppressed).shift(bonus + temporaryShift);
    }

    /**
     * The character's own Categoria de Tamanho, unless a held Talento overrides it outright.
     *
     * <p>Every override in the catalog is race-locked and mutually exclusive with the others, so
     * a character holding two is not a case the rules produce. Rather than pick arbitrarily or
     * throw over an impossible state, the <b>smallest</b> wins: each of these clauses shrinks its
     * holder into something (a Duende, a Pixie), and the smaller shape is the more constraining
     * one — the same "take the stricter reading" restraint every other cap in this core applies.
     *
     * <p><b>A suppressed racial base is {@link SizeCategory#ZERO}</b> — the Human baseline every
     * race without an override already produces. The stored {@code Character#getSizeCategory()}
     * is written once by {@code Race#generateEmptyCharacter} and never moved again, so it <em>is</em>
     * the racial contribution and there is nothing non-racial in it to preserve. Suppression is
     * applied before the override/shift stages, so an Anão Draconato is human-sized first and then
     * takes the Forma's "+2 para cada Título" on top, rather than carrying its dwarfness into a
     * dragon.
     */
    private SizeCategory resolveBaseSizeCategory(final Character character,
                                                 final boolean racialBaseSuppressed) {
        return character.getFeats().stream()
                .map(feat -> feat.resolveSizeCategoryOverride(character))
                .filter(java.util.Objects::nonNull)
                .min(java.util.Comparator.comparingInt(SizeCategory::getCategory))
                .orElseGet(() -> racialBaseSuppressed ? SizeCategory.ZERO : character.getSizeCategory());
    }
}
