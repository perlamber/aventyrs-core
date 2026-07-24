package org.aventyrs.core.character;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.Interactable;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.Player;

import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_EXPERIENCE;

@Builder(toBuilder = true) @Getter
public class Character implements Interactable {
    @NonNull
    protected Player player;

    @NonNull
    protected String name;

    @NonNull
    protected Race race;

    @NonNull
    protected CharacterAttributes attributes;

    @NonNull
    @Singular
    protected List<CharacterSkill> skills;

    @NonNull
    @Singular
    protected List<AttributeAbility> attributeAbilities;

    @Builder.Default
    protected SizeCategory sizeCategory = SizeCategory.ZERO;

    @Builder.Default
    CharacterStatus status = CharacterStatus.CLEAN;

    //TODO implement
    @Override
    public CharacterStatus receiveInteraction(Interaction interaction) {
        return null;
    }

    //TODO implement
    @Override
    public CharacterStatus receiveInteraction() {
        return null;
    }



}
