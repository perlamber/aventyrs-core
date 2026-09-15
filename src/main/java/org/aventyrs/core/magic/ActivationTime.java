package org.aventyrs.core.magic;

import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_SPELL_ACTIVATION;

/**
 * A Magia's {@code Tempo de Ativação:} descriptor — what casting it costs the Conjurador on
 * their Turn.
 *
 * <p>It is <b>not simply a Pontos de Ação count</b>, which is why this is a record over an
 * {@link ActivationType} rather than an {@code int} on {@link Spell}. Across the 145 complete
 * Magias: 1PA (5×), 2PA (58×), 3PA (53×), 4PA (15×), 5PA (5×) — but also <b>Reação</b> (5×) and
 * <b>Ação Livre</b> (4×), neither of which is a PA count at all. Those two spend from the
 * separate pools {@code ReactionsService} and {@code FreeActionsService} aggregate.
 *
 * <p>{@link #actionPoints()} is the count for {@link ActivationType#PONTOS_DE_ACAO} and
 * {@code 0} for the other two, enforced by the canonical constructor.
 *
 * <h2>An Efeito Alternativo may state its own, and that <em>is</em> modelled</h2>
 *
 * Seven of the catalog's 64 second versions override their parent's activation, and they run in
 * <b>both</b> directions: {@code VidaSpell#ALIVIAR_A_DOR}'s <i>Procrastinar Ferimento</i> turns
 * 2PA into a Reação, while {@code PolimorfismoSpell#REARRANJO_CORPORAL}'s and {@code
 * RegeneracaoSpell#ESCARNIO_DE_HALOI}'s alternates trade a Reação back for Pontos de Ação. That
 * is carried by {@link SpellAlternateEffect#activationTime()} and applied by {@link
 * AlternateSpellVersion} — so an alternate activation is a real, replaceable column rather than
 * prose.
 *
 * <h2>The Graduação-gated case is still authored as its base cost</h2>
 *
 * One Magia reads "2PA, pode ser conjurado como Reação por personagens com Domínio do Mana 5 ou
 * superior ao custo de 3PM". That is <b>not</b> an Efeito Alternativo — it is a threshold on the
 * caster, not a second version of the Magia — so the mechanism above does not reach it: it is
 * authored as its plain {@code pa(2)} with the condition in the Magia's own prose. Modelling it
 * would still mean a Graduação threshold plus a PM override, for exactly one consumer.
 */
public record ActivationTime(ActivationType type, int actionPoints) {

    /** Casting it costs a Reação rather than Pontos de Ação. */
    public static final ActivationTime REACAO = new ActivationTime(ActivationType.REACAO, 0);

    /** Casting it costs an Ação Livre rather than Pontos de Ação. */
    public static final ActivationTime ACAO_LIVRE = new ActivationTime(ActivationType.ACAO_LIVRE, 0);

    public ActivationTime {
        if (type == null || !isLegalCombination(type, actionPoints)) {
            throw new IllegalOperationException(INVALID_SPELL_ACTIVATION);
        }
    }

    private static boolean isLegalCombination(final ActivationType type, final int actionPoints) {
        return switch (type) {
            case PONTOS_DE_ACAO -> actionPoints > 0;
            case REACAO, ACAO_LIVRE -> actionPoints == 0;
        };
    }

    /** {@code NPA} — the ordinary case, 135 of the 145 complete Magias. */
    public static ActivationTime pa(final int actionPoints) {
        return new ActivationTime(ActivationType.PONTOS_DE_ACAO, actionPoints);
    }

    /** e.g. {@code "3PA"}, {@code "Reação"}. */
    @Override
    public String toString() {
        return type == ActivationType.PONTOS_DE_ACAO ? actionPoints + "PA" : type.name();
    }
}
