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
}
