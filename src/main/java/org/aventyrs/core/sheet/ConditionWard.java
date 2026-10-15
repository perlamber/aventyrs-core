package org.aventyrs.core.sheet;

import lombok.Getter;
import lombok.NonNull;

import java.util.Set;

/**
 * Immunity to some Condições for as long as it is held (core 0.1.7.4) — Dádiva de Gaea's "enquanto sob efeito da Aura
 * também são imunes a efeitos de Medo e a Possessões". Open-ended: granted and taken back by {@code
 * Scene#refreshMonsterAuras} as its holder enters and leaves the aura. Read by {@link
 * CombatantSheet#isImmuneToCondition}.
 */
@Getter
public final class ConditionWard extends TemporaryEffect {

    private final Set<ConditionType> conditions;
    private final String source;

    public ConditionWard(@NonNull final Set<ConditionType> conditions, @NonNull final String source) {
        super((Integer) null);
        this.conditions = Set.copyOf(conditions);
        this.source = source;
    }

    public boolean wards(final ConditionType type) {
        return conditions.contains(type) && !isExpired();
    }
}
