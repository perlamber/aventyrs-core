package org.aventyrs.core.effect;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.Withering;

import java.util.Optional;

/**
 * Definhar — the first concrete {@link EffectChain} (see {@code
 * org.aventyrs.core.effect} package-info for the pipeline this fits into). Applied to the
 * character receiving the attack — {@link #applyTo}'s {@code target}, never the
 * attacker. Unlike every concrete {@link CriticalEffect} in this package, it isn't gated
 * on a {@link org.aventyrs.core.skill.CriticalResult} — its own rules text has no
 * Maior/Menor split, and it's a Corrente de Efeitos rather than a critical-hit-only
 * consequence, so there's nothing to validate at construction.
 *
 * <p>There's no immediate loss on the Rodada it lands — its own rules text is entirely
 * "1 ponto de Dano Físico Profano por Rodada por Vigor Rodadas", so {@link #applyTo}
 * registers an ongoing {@link Withering} (see {@link CombatantSheet#applyEffect}) and
 * nothing else; {@link InteractionResult#getResourceLossValue()} stays {@code null} since
 * nothing was actually lost yet at the moment this Interaction runs, the same "null when
 * this Interaction doesn't compute it" discipline {@link InteractionResult}'s own javadoc
 * documents. The per-Rodada loss itself is advanced by a caller via {@link
 * CombatantSheet#tickTemporaryEffects()}, same as {@link Sangramento}/{@link ManaPurge}'s
 * own ongoing halves.
 *
 * <p>Its damage is curse damage (bypasses Shield points, see {@link
 * CombatantSheet#applyCurseDamage}) — Definhar's own rules text names it "Dano Físico
 * Profano" and explicitly calls the whole effect an "Efeito de Maldição", so unlike
 * {@link RealExecution}'s own inferred use of curse damage, this one is confirmed
 * directly by the rules text rather than inferred from tone; see {@link Withering}'s own
 * javadoc for the full reasoning.
 *
 * <p>Its duration (Rodadas equal to the target's own Vigor) is fully computable and
 * implemented for real — unlike {@link Sangramento} Maior/{@link ManaPurge} Maior, there's
 * no "até o fim da cena" alternative duration in Definhar's own rules text to leave
 * open-ended.
 *
 * <p>"Não cumulativo" — being hit by Definhar again while already Definhado doesn't stack
 * a second point of per-Rodada loss — is implemented via {@link Withering#isCumulative()}
 * returning false, which {@link CombatantSheet#applyEffect} honors generically by
 * replacing any existing Withering with the new one rather than adding a second.
 */
public class Definhar extends AbstractEffect implements EffectChain {

    private static final int PER_ROUND_DAMAGE = 1;

    @Override
    public String getDescription() {
        return "O Alvo sofre 1 ponto de Dano Físico Profano por Rodada por Vigor Rodadas " +
                "(não cumulativo). Este é um Efeito de Maldição.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        Character affectedCharacter = target.getCharacter();
        int vigorTotal = affectedCharacter.getAttributes().getVigor().getTotal();

        target.applyEffect(new Withering(PER_ROUND_DAMAGE, Optional.of(vigorTotal)));

        return reportChain(InteractionResult.builder()
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
