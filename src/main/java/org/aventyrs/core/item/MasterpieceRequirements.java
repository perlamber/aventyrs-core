package org.aventyrs.core.item;

import java.util.List;

import org.aventyrs.core.character.Character;

/**
 * The Attribute requirements an item's Masterpiece Favor needs. Every listed requirement must be
 * met; unlike {@link ItemRequirements}, a Masterpiece can name more than one Attribute.
 */
public record MasterpieceRequirements(List<ItemRequirements> attributeRequirements) {

    public MasterpieceRequirements {
        attributeRequirements = List.copyOf(attributeRequirements);
    }

    public boolean isMetBy(final Character character) {
        return attributeRequirements.stream().allMatch(requirement -> requirement.isMetBy(character));
    }
}
