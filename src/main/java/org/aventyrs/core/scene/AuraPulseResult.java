package org.aventyrs.core.scene;

import lombok.NonNull;

import java.util.List;

/**
 * One aura's pulse (core 0.1.7.4): the dice it threw once for everyone, each target's {@link AuraHit}, and whether
 * the holder heals what it dealt ("o Monstro recupera uma quantidade de PV igual ao dano causado"). The caller applies
 * each hit on the sheet it controls and, for a life-stealing pulse, heals the holder by the PV the hits took.
 */
public record AuraPulseResult(@NonNull MonsterAura aura, int rolled, @NonNull List<AuraHit> hits, boolean lifeSteal) {

    public AuraPulseResult {
        hits = List.copyOf(hits);
    }
}
