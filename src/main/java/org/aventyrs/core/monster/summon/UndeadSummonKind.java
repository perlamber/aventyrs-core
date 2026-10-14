package org.aventyrs.core.monster.summon;

import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.monster.SummonedMonsterTemplate;

/**
 * The REANIMAR tree's creatures (core 0.1.6.25) — what a summon descriptor names to rebuild one ({@link #restore}),
 * the {@code NatureSummon#restore} twin.
 */
public enum UndeadSummonKind {

    ZUMBI,
    CARNICAL,
    ABANTESMA;

    /**
     * The stat block at its Conjurador's Graduação, raised from a body of bodySize (ignored by the Abantesma, which has
     * none); butcher is Criar Carniçal's Açougueiro.
     */
    public SummonedMonsterTemplate template(final int manaGraduation, final SizeCategory bodySize,
                                            final boolean butcher) {
        SizeCategory body = bodySize == null ? SizeCategory.ZERO : bodySize;
        return switch (this) {
            case ZUMBI -> Zumbi.builder().conjuradorManaGraduation(manaGraduation).bodySizeCategory(body).build();
            case CARNICAL -> Carnical.builder().conjuradorManaGraduation(manaGraduation).bodySizeCategory(body)
                    .butcher(butcher).build();
            case ABANTESMA -> Abantesma.summonedBy(manaGraduation);
        };
    }

    /** {@link #template} by name — the persistence-restore path. */
    public static SummonedMonsterTemplate restore(final String kind, final int manaGraduation, final String bodySize,
                                                  final boolean butcher) {
        return valueOf(kind).template(manaGraduation, bodySize == null ? null : SizeCategory.valueOf(bodySize),
                butcher);
    }
}
