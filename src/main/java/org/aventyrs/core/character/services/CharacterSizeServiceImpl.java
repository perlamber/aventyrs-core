package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;

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
        int bonus = modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.SIZE_CATEGORY);
        bonus += character.getFeats().stream()
                .mapToInt(feat -> feat.resolveSizeCategoryIncrease(character))
                .sum();
        return resolveBaseSizeCategory(character).shift(bonus);
    }

    /**
     * The character's own Categoria de Tamanho, unless a held Talento overrides it outright.
     *
     * <p>Every override in the catalog is race-locked and mutually exclusive with the others, so
     * a character holding two is not a case the rules produce. Rather than pick arbitrarily or
     * throw over an impossible state, the <b>smallest</b> wins: each of these clauses shrinks its
     * holder into something (a Duende, a Pixie), and the smaller shape is the more constraining
     * one — the same "take the stricter reading" restraint every other cap in this core applies.
     */
    private SizeCategory resolveBaseSizeCategory(final Character character) {
        return character.getFeats().stream()
                .map(feat -> feat.resolveSizeCategoryOverride(character))
                .filter(java.util.Objects::nonNull)
                .min(java.util.Comparator.comparingInt(SizeCategory::getCategory))
                .orElseGet(character::getSizeCategory);
    }
}
