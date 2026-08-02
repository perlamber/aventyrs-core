package org.aventyrs.core.character;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder(toBuilder = true)
@Getter
public class CharacterAttributes {
    @Builder.Default
    protected AttributeValue vigor = AttributeValue.builder().build();
    @Builder.Default
    protected AttributeValue strength = AttributeValue.builder().build();
    @Builder.Default
    protected AttributeValue dexterity = AttributeValue.builder().build();
    @Builder.Default
    protected AttributeValue focus = AttributeValue.builder().build();
    @Builder.Default
    protected AttributeValue instinct = AttributeValue.builder().build();
    @Builder.Default
    protected AttributeValue gnose = AttributeValue.builder().build();
    @Builder.Default
    protected AttributeValue charisma = AttributeValue.builder().build();
}
