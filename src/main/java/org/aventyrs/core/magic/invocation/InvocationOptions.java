package org.aventyrs.core.magic.invocation;

import lombok.Builder;
import org.aventyrs.core.character.AttributeDomain;

/**
 * The per-cast choices a summoner makes on top of an invocation Magia — Bruxo's Invocação Maior ("+1PA" for the
 * Multiplicador de PV, "+2PD" for an Atributo), Invocação Dupla ("+2PA" for a second creature), and Maldição da
 * Nevasca do Sudoeste's comet. A choice no held trait permits is refused by the Título that reads it.
 *
 * @param extraTimeForLife   Invocação Maior: +1PA, Multiplicador de PV +3
 * @param boostedAttribute   Invocação Maior: +2PD, +4 to Força or Destreza; {@code null} to skip
 * @param doubled            Invocação Dupla: +2PA, a second creature, half the Duração
 * @param comet              Nevasca do Sudoeste: arrive by comet instead of the Domínio do Mana roll
 * @param openSky            the caller's word that the Cena is "de céu aberto" — the comet needs it
 */
@Builder
public record InvocationOptions(boolean extraTimeForLife, AttributeDomain boostedAttribute, boolean doubled,
                                boolean comet, boolean openSky) {

    /** No choice made — the invocation as the Magia states it. */
    public static final InvocationOptions NONE = InvocationOptions.builder().build();
}
