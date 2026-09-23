package org.aventyrs.core.title.giganteenfurecido;

/**
 * Gritos de Guerra's three effects — "Ao ativar esta Habilidade escolha um dos efeitos abaixo" — the
 * {@code TitleAbilityActivationRequest#getChoice} of a {@link GritosDeGuerraInteraction}.
 */
public enum GritoDeGuerra {
    /** "Todo o dano causado a você e aos seus aliados, que estejam em Distância Média, é reduzido à metade durante 1 Rodadas". */
    DESDENHO,
    /** "Inimigos em Distância Curta sofrem 1d6 + Metade de seus Multiplicador de PV pontos de Dano Físico Primordial". */
    ESPINHOSO,
    /** "Aliados em Distância Curta recebem os mesmos efeitos de seus Frenesis ativos, exceto Gritos de Guerra, por 1 Rodada". */
    INSPIRADOR
}
