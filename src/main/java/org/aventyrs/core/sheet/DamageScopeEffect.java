package org.aventyrs.core.sheet;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageScope;
import org.aventyrs.core.character.DamageType;

/**
 * A timed Meio-Dano or immunity limited to a {@link DamageScope} — "Danos sofridos reduzidos à
 * Metade por 2 Rodadas", "reduz danos físicos à metade", "imune à efeitos e danos que não sejam
 * Elementais ou Primordiais". The {@code TemporaryBonus} {@code HALF_DAMAGE} twin that can be
 * scoped; read by {@link CombatantSheet#halvesDamage} / {@link CombatantSheet#isImmuneToDamage}.
 *
 * <p>Several may be held at once (one per source), and a {@code null} duration is open-ended —
 * lifted by whoever granted it, as flight lifts its own.
 */
@Getter
public class DamageScopeEffect extends TemporaryEffect {

    public enum Kind {
        /** Meio-Dano: the hit is halved, last, once — see {@code DamageServiceImpl}. */
        HALVES,
        /** The hit deals nothing. */
        IMMUNE
    }

    @NonNull
    private final Kind kind;

    @NonNull
    private final DamageScope scope;

    /** Which trait granted it — what {@link AbstractCombatantSheet#removeEffectsFrom} matches. */
    private final String source;

    public DamageScopeEffect(@NonNull final Kind kind, @NonNull final DamageScope scope, final Integer rounds,
                             final String source) {
        super(rounds);
        this.kind = kind;
        this.scope = scope;
        this.source = source;
    }

    /** Whether this effect, of {@code wanted} kind, reaches a hit of this type and descriptor. */
    boolean covers(final Kind wanted, final DamageType damageType, final DamageDescriptor descriptor) {
        return !isExpired() && kind == wanted && scope.matches(damageType, descriptor);
    }

    @Override
    Object stackingKey() {
        return source;
    }
}
