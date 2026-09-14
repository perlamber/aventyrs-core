package org.aventyrs.core.magic;

import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_SPELL_HEALING;

/**
 * The Pontos de Vida a Magia's {@code Efeito:} line restores — the structured form of a phrase
 * like {@code "Você pode fazer com que um alvo recupere PV como se passasse por um Descanso
 * Longo"}. The healing twin of {@link SpellDamage}, and authored the same way: a column on
 * {@code SpellData}, resolved against a concrete combatant by {@code
 * org.aventyrs.core.effect.SpellHealingEffect}.
 *
 * <p><b>A healing Magia states a Descanso tier, not a dice figure.</b> Every recovery in the Vida
 * tree reads "como se passasse por um Descanso Mínimo/Longo/Total", and {@link RestType} already
 * carries exactly those four tiers with {@code RestService#getRecoveredHitPoints} computing the
 * number from the <b>target's</b> Vigor — "faça com que <i>um alvo</i> recupere PV como se
 * <i>passasse</i> por um Descanso" reads the recipient, the same way {@code
 * SpellDuration#targetAttribute} does. That is also why the effect calls a bare {@code
 * CombatantSheet#heal} rather than {@code RestService#applyRest}: the rules text says "Este é um
 * efeito similar a Descanso e <b>não substitui Descansos reais</b>", and a real Rest would also
 * restore PM/PD, settle every {@code PendingEgoRecovery} and clear rest cooldowns.
 *
 * <p><b>Exactly one magnitude</b> — either a {@link #restEquivalent} tier or {@link
 * #fullRecovery} ("recupera todos os PV perdidos", Benção da Luz). Stating both or neither is an
 * authoring mistake rather than a value, so it throws here, the same cross-field validation
 * {@link SpellDamage} applies to its own type/element pair and {@link SpellTargeting} to its
 * reach/range/area one.
 *
 * <p>What this deliberately does <b>not</b> hold, and stays prose on the constant: a Corrente's
 * own bonus recovery (<i>Sobrecura</i> is its own {@code EffectChain}), an Efeito Alternativo's
 * different target count (<i>Benção Bifurcada</i>, <i>Cura em Massa</i>), and the recovery of any
 * resource but PV (<i>Fonte da Juventude</i>'s PM and PD).
 *
 * @param restEquivalent   the Descanso tier this Magia heals as much as, or {@code null} when it
 *                         states a {@link #fullRecovery} instead
 * @param fullRecovery     whether it restores every lost PV outright
 * @param halvedForHostiles "Inimigos do conjurador recuperam apenas metade desta quantidade de
 *                         PV" — Nova Rejuvenescedora. Whether a given target <i>is</i> hostile is
 *                         the caller's to say: this core resolves no Área de Efeito footprint.
 * @param condition        the live-state gate, {@link HealingCondition#ALWAYS} for all but one
 */
public record SpellHealing(RestType restEquivalent, boolean fullRecovery,
                           boolean halvedForHostiles, HealingCondition condition) {

    public SpellHealing {
        if (condition == null || (restEquivalent == null) != fullRecovery) {
            throw new IllegalOperationException(INVALID_SPELL_HEALING);
        }
    }

    /** {@code "Seu toque recupera todos os PV perdidos da criatura tocada"} — Benção da Luz. */
    public static final SpellHealing FULL_RECOVERY =
            new SpellHealing(null, true, false, HealingCondition.ALWAYS);

    /** {@code "recupera PV como se passasse por um Descanso <tier>"} — the modal shape. */
    public static SpellHealing restEquivalent(final RestType restType) {
        return new SpellHealing(restType, false, false, HealingCondition.ALWAYS);
    }

    /**
     * This same recovery, halved for a target hostile to the caster — Nova Rejuvenescedora.
     * Named for the act rather than the column so it doesn't clash with the {@link
     * #halvedForHostiles()} accessor, the same way {@code DamageInteraction#halvingDamage()} reads.
     */
    public SpellHealing halvingForHostiles() {
        return new SpellHealing(restEquivalent, fullRecovery, true, condition);
    }

    /** This same recovery, but only for a target who is not bleeding — Aliviar a Dor. */
    public SpellHealing onlyIfNotBleeding() {
        return new SpellHealing(restEquivalent, fullRecovery, halvedForHostiles,
                HealingCondition.ONLY_IF_NOT_BLEEDING);
    }
}
