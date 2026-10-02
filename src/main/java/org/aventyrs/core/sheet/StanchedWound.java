package org.aventyrs.core.sheet;

/**
 * Estancar's aftermath (core 0.0.94): the hit Procrastinar Ferimento postponed causes no Sangramento — a {@link
 * Bleeding} offered while this is held is refused. Lasts the Rodada of that hit.
 */
public class StanchedWound extends TemporaryEffect {

    public StanchedWound() {
        super(1);
    }

    @Override
    boolean isCumulative() {
        return false;
    }
}
