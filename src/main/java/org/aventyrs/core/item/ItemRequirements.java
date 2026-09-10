package org.aventyrs.core.item;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;

/**
 * What a character must meet for an {@link Item}'s {@link ItemFavor} to apply to them — an
 * Attribute reaching a value (e.g. "Força 3" on {@code ArmorItem#ARMADURA_COMPLETA}).
 *
 * <p>Mirrors {@code org.aventyrs.core.feat.FeatRequirements}' shape, deliberately narrowed to
 * the one requirement kind an item's own Requisitos column actually uses, rather than reusing
 * that record across packages and carrying a {@code requiredFeat}/{@code requiredSkillType}
 * an item never names. Widen this if a real item ever requires a Perícia/Talento/Título.
 *
 * <p>One deliberate difference from {@code FeatRequirements}: this checks {@code
 * Character#getEffectiveAttributeTotal} (base + racial + variable + any {@code
 * Feat#resolveAttributeBonus} grant), not {@code getBase()}. Acquiring a Talento is a permanent
 * investment gated on what the character personally invested in; whether an item's Favor
 * applies is a "can I meet this right now" question, so a Bônus Racial or a variable bonus from
 * a spell/feat/other equipment counts toward it the same as base does.
 *
 * <p>A {@code null} {@code attributeDomain} means "no requirement at all" — {@link
 * #isMetBy(Character)} then always holds, same convention every unset {@code FeatRequirements}
 * field already follows.
 *
 * <p>{@code alternativeDomain} is the "ou" half of a Requisitos column that names two Atributos
 * ({@code HelmetItem#CHARME_DO_ARTESAO}'s "Car 3/Gno 3"): when set, meeting <em>either</em>
 * domain at {@code requiredAttributeValue} satisfies the requirement. {@code null} for the usual
 * single-Atributo column; the 2-arg constructor is the shorthand for it.
 */
public record ItemRequirements(AttributeDomain attributeDomain, int requiredAttributeValue,
                               AttributeDomain alternativeDomain) {

    public ItemRequirements(final AttributeDomain attributeDomain, final int requiredAttributeValue) {
        this(attributeDomain, requiredAttributeValue, null);
    }

    /** Whether character currently satisfies this requirement. */
    public boolean isMetBy(final Character character) {
        return attributeDomain == null
                || character.getEffectiveAttributeTotal(attributeDomain) >= requiredAttributeValue
                || (alternativeDomain != null
                        && character.getEffectiveAttributeTotal(alternativeDomain) >= requiredAttributeValue);
    }
}
