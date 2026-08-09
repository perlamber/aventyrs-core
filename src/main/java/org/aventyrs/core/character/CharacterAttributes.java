package org.aventyrs.core.character;

import java.security.KeyStore;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;

@Builder(toBuilder = true)
@Getter
public class CharacterAttributes {
    @Builder.Default
    protected AttributeValue vigor = AttributeValue.builder().domain(AttributeDomain.VIGOR).build();
    @Builder.Default
    protected AttributeValue strength = AttributeValue.builder().domain(AttributeDomain.STRENGTH).build();
    @Builder.Default
    protected AttributeValue dexterity = AttributeValue.builder().domain(AttributeDomain.DEXTERITY).build();
    @Builder.Default
    protected AttributeValue focus = AttributeValue.builder().domain(AttributeDomain.FOCUS).build();
    @Builder.Default
    protected AttributeValue instinct = AttributeValue.builder().domain(AttributeDomain.INSTINCT).build();
    @Builder.Default
    protected AttributeValue gnose = AttributeValue.builder().domain(AttributeDomain.GNOSE).build();
    @Builder.Default
    protected AttributeValue charisma = AttributeValue.builder().domain(AttributeDomain.CHARISMA).build();

    public AttributeValue getAttribute(final AttributeDomain domain) {
        return switch (domain) {
            case VIGOR -> vigor;
            case STRENGTH -> strength;
            case DEXTERITY -> dexterity;
            case FOCUS -> focus;
            case INSTINCT -> instinct;
            case GNOSE -> gnose;
            case CHARISMA -> charisma;
        };
    }
}
