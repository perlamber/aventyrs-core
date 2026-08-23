package org.aventyrs.core.character;

import lombok.Builder;

import java.util.Map;
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

    /**
     * Builds a CharacterAttributes from a domain-to-base map — every {@link AttributeDomain}
     * absent from bases keeps {@link AttributeValue}'s own default. The counterpart to {@link
     * #getAttribute}, and the shared home of the switch-onto-the-builder that {@code
     * CharacterCreationServiceImpl} used to keep private: character creation and {@code
     * org.aventyrs.core.monster.MonsterTemplate} both need it, so it lives with the type it
     * builds rather than being duplicated per caller.
     *
     * <p>Sets {@code base} only. Nothing here validates the {@code
     * CharacterAttributeService.MAX_ATTRIBUTE_BASE} cap — deliberately, and consistently with
     * every other builder path in this codebase: the cap is enforced on {@code
     * CharacterAttributeService#upgradeBase}, the XP-spending progression entry point, not on
     * construction. That's exactly what lets a monster be built with a base of 12.
     */
    public static CharacterAttributes of(final Map<AttributeDomain, Integer> bases) {
        CharacterAttributesBuilder builder = builder();
        for (Map.Entry<AttributeDomain, Integer> entry : bases.entrySet()) {
            assign(builder, entry.getKey(),
                    AttributeValue.builder().domain(entry.getKey()).base(entry.getValue()).build());
        }
        return builder.build();
    }

    /** Places value into whichever builder slot domain names. */
    public static void assign(final CharacterAttributesBuilder builder, final AttributeDomain domain, final AttributeValue value) {
        switch (domain) {
            case VIGOR -> builder.vigor(value);
            case STRENGTH -> builder.strength(value);
            case DEXTERITY -> builder.dexterity(value);
            case FOCUS -> builder.focus(value);
            case INSTINCT -> builder.instinct(value);
            case GNOSE -> builder.gnose(value);
            case CHARISMA -> builder.charisma(value);
        }
    }

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
