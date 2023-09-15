package org.aventyrs.core.character;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import org.aventyrs.core.sheet.Interactable;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.Player;

import java.util.List;

@Builder(toBuilder = true) @Getter
public class Character implements Interactable {
    @NonNull
    protected Player player;

    @NonNull
    protected String name;

    @NonNull
    protected CharacterRace race;

    @NonNull
    @Singular
    protected List<CharacterSkill> skills;
    @Builder.Default
    protected int usedExperience = 0;

    @Override
    public CharacterStatus receiveInteraction(Interaction interaction) {
        return null;
    }

    @Override
    public CharacterStatus receiveInteraction() {
        return null;
    }

    public int accumulateExperience(int experience)
    {
        return usedExperience += experience;
    }

}
