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
 * <p>One deliberate difference from {@code FeatRequirements}: this checks {@link
 * org.aventyrs.core.character.AttributeValue#getTotal()}, not {@code getBase()}. Acquiring a
 * Talento is a permanent investment gated on what the character personally invested in;
 * whether an item's Favor applies is a "can I meet this right now" question, so a Bônus Racial
 * or a variable bonus from a spell/feat/other equipment counts toward it the same as base
 * does.
 *
 * <p>A {@code null} {@code attributeDomain} means "no requirement at all" — {@link
 * #isMetBy(Character)} then always holds, same convention every unset {@code FeatRequirements}
 * field already follows.
 */
public record ItemRequirements(AttributeDomain attributeDomain, int requiredAttributeValue) {

    /** Whether character currently satisfies this requirement. */
    public boolean isMetBy(final Character character) {
        return attributeDomain == null
                || character.getAttributes().getAttribute(attributeDomain).getTotal() >= requiredAttributeValue;
    }
}
