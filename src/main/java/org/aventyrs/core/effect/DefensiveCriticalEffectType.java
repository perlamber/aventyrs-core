package org.aventyrs.core.effect;

/**
 * Which Efeito Crítico <b>Defensivo</b> a piece of defensive gear grants — the other half of the
 * Efeitos Críticos catalog in {@code docs/rules/efeitos-criticos.txt} (L53–116), and a different
 * mechanism from {@link CriticalEffectType} rather than more constants for it.
 *
 * <h2>How it differs from an offensive Efeito Crítico</h2>
 *
 * <table border="1">
 *   <caption>the two mechanisms</caption>
 *   <tr><th></th><th>{@link CriticalEffectType}</th><th>this</th></tr>
 *   <tr><td>trigger</td><td>an Acerto Crítico on the attacker's roll</td>
 *       <td>an Acerto Crítico on the <i>defender's own</i> Defesa roll</td></tr>
 *   <tr><td>source</td><td>the attack — a weapon, a Magia, an ability</td>
 *       <td>the Armadura or Escudo <i>worn</i></td></tr>
 *   <tr><td>affects</td><td>the target</td><td>the attacker, or the defender themselves</td></tr>
 *   <tr><td>stacking</td><td>one per attack</td><td>cumulative across worn gear</td></tr>
 * </table>
 *
 * <p>"Efeitos Críticos Defensivos <b>substituem as falhas críticas inimigas</b> em caso de
 * Sucesso Crítico nas rolagens de Defesas" (L54) — so this is not a bonus layered onto a critical
 * failure the attacker suffered; it stands in place of it.
 *
 * <h2>Why it is a separate enum</h2>
 *
 * {@link CriticalEffectType} exists so that a creature's <i>immunities</i> can name an effect
 * that has no class yet. Nothing is ever immune to a Defensive effect — it is granted by the
 * defender's own equipment to the defender, so an immunity list has nothing to say about it, and
 * {@code CriticalEffect#applicableTo} must never filter one. Merging the two enums would offer
 * every stat block nine immunities that cannot mean anything.
 *
 * <p>These are <b>identities, not implementations</b>: no class produces one and nothing consumes
 * one yet, exactly as most of {@link CriticalEffectType} is. Several need mechanisms this core
 * does not have — forced movement, forced targeting and reactive damage (per-copy item Dureza is
 * built now: see {@code Item#applyDamage}, which Repelir e Suprimir/Retorno de Danos would call) —
 * so authoring the identity now is the "can't apply it yet doesn't mean can't compute it yet"
 * discipline, not a claim that the effect works.
 *
 * <h2>Only Armaduras and Escudos grant these</h2>
 *
 * "outros equipamentos Defensivos não concedem este tipo de benefício" (L55), which is why
 * {@code ArmorItem#getDefensiveCriticalEffect()} is a column on that enum rather than on {@code
 * Item} — the same reason {@code getDamageBase()} lives on {@code Weapon} and not on every
 * pauldron. The source also assigns one to each of six Escudos and five Defesas Naturais,
 * neither of which this core models yet.
 */
public enum DefensiveCriticalEffectType {

    /**
     * Maior: seu Multiplicador de PM aumenta em +1 por 2 Rodadas e o do atacante é reduzido em
     * -1 até que ele passe por um Descanso. Menor: você recupera 2PM e o atacante perde 2PM.
     */
    CHOQUE_DE_AETHER,

    /**
     * Maior: você desfere um contra-ataque com Vantagem na rolagem de Danos e aplica seu Efeito
     * Crítico Menor. Menor: o contra-ataque com Vantagem, sem o Efeito Crítico.
     */
    CONTRA_ATACANTE,

    /**
     * Maior: seu Multiplicador de PD aumenta em +1 por 2 Rodadas e o do atacante é reduzido em
     * -1 até que ele passe por um Descanso. Menor: você recupera 2PD e o atacante perde 2PD.
     */
    FAISCA_DE_DETERMINACAO,

    /**
     * Maior: você empurra o atacante 2UD para trás e torna-se imune aos ataques dele por 1
     * Rodada. Menor: empurra 1UD e recebe Bônus de +2 nas Defesas contra ele por 1 Rodada.
     */
    IMPETO_DEFENSIVO,

    /**
     * Maior: você move-se até 2UD em qualquer direção livre, ignorando Terreno Difícil, e os
     * movimentos do atacante contam como Terreno Difícil por 1 Rodada. Menor: apenas o seu
     * próprio movimento.
     */
    LIBERDADE_DE_ACAO,

    /**
     * Maior: o atacante sofre 3 pontos de Dano Físico Primordial e é desafiado — o próximo
     * ataque dele deve ter você como alvo primário, e sua Margem Crítica Menor para resistir a
     * ele aumenta em +3 números. Menor: 1 ponto de dano e +1 número.
     */
    PROVOCAR,

    /**
     * Maior: a arma do alvo sofre 2d6 pontos de dano e fica inutilizável por 1 Rodada. Menor:
     * 1d6, e por 1 Rodada novos ataques do mesmo tipo custam 1PA adicional. Se o ataque repelido
     * for mágico, o dano recai sobre o conjurador e suas conjurações são suprimidas.
     */
    REPELIR_E_SUPRIMIR,

    /**
     * Maior: o atacante corpo-a-corpo e sua arma sofrem 3d6+Vigor pontos de Dano Físico
     * Primordial. Menor: 1d6+Vigor. Em ambos os casos projéteis são destruídos e não aplicam
     * áreas de efeito.
     */
    RETORNO_DE_DANOS,

    /**
     * Maior: você pode conjurar qualquer Magia imediatamente. Menor: apenas uma Magia Semente ou
     * Broto. Os custos de Bônus Base ainda devem ser cumpridos.
     */
    SURTO_ARCANO
}
