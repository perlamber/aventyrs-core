package org.aventyrs.core.character;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder(toBuilder = true)
@Getter
public class CharacterAttributes {
    @Builder.Default
    protected int vigor = 1;
    @Builder.Default
    protected int strength = 1;
    @Builder.Default
    protected int dexterity = 1;
    @Builder.Default
    protected int focus = 1;
    @Builder.Default
    protected int instinct = 1;
    @Builder.Default
    protected int gnose = 1;
    @Builder.Default
    protected int charisma = 1;
}
