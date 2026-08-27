package org.aventyrs.core.character;

import lombok.Builder;
import lombok.Getter;

@Builder(toBuilder = true)
@Getter
public class CharacterEgos {
    @Builder.Default
    protected EgoValue autocontrole = EgoValue.builder().build();
    @Builder.Default
    protected EgoValue recursos = EgoValue.builder().build();
    @Builder.Default
    protected EgoValue sorte = EgoValue.builder().build();
    @Builder.Default
    protected EgoValue iniciativa = EgoValue.builder().build();

    /** Mirrors {@link CharacterAttributes#getAttribute(AttributeDomain)} for the Ego side. */
    public EgoValue getEgo(final EgoDomain domain) {
        return switch (domain) {
            case AUTOCONTROLE -> autocontrole;
            case RECURSOS -> recursos;
            case SORTE -> sorte;
            case INICIATIVA -> iniciativa;
        };
    }

    /**
     * A new CharacterEgos with domain's {@link EgoValue#getVariable()} raised by amount —
     * every other domain, and that domain's own base, untouched. This is the mechanism
     * {@link org.aventyrs.core.ability.AttributeAbility#resolvePermanentEgoGain} grants are
     * applied through — see {@code
     * org.aventyrs.core.character.services.AttributeAbilityService#grantAttributeAbility}.
     */
    public CharacterEgos withVariableBonus(final EgoDomain domain, final int amount) {
        EgoValue updated = getEgo(domain).toBuilder()
                .variable(getEgo(domain).getVariable() + amount)
                .build();
        return switch (domain) {
            case AUTOCONTROLE -> toBuilder().autocontrole(updated).build();
            case RECURSOS -> toBuilder().recursos(updated).build();
            case SORTE -> toBuilder().sorte(updated).build();
            case INICIATIVA -> toBuilder().iniciativa(updated).build();
        };
    }
}
