package org.aventyrs.core.title.senhordabriga;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.PDCost;

import java.util.Optional;

import static org.aventyrs.core.title.PDCost.fixed;

/**
 * Senhor da Briga's own Habilidades/Supremas — the ones whose prerequisite names "1 Especialização
 * de Senhor da Briga" generically. Every "Requer" clause is real, enforced data
 * (requiredSpecializations/requiredOtherAbilities), checked by {@link AventyrTitleAbility#isEligible}
 * through {@code TitleAbilityService#grantTitleAbility}.
 *
 * <p>The passive ones reach play through {@link SenhorDaBriga}'s scans rather than through code on
 * the constant: each needs to know what else its holder holds (Grande Mestre das Brigas, Malícia de
 * Valentão) or their live sheet, which an enum constant cannot see.
 */
@Getter
@AllArgsConstructor
public enum SenhorDaBrigaAbility implements AventyrTitleAbility {

    // Requer 1 Especialização de Senhor da Briga. Real through FinalizacaoInteraction: the 1PD buys
    // a one-Rodada CombatantSheet#openActivationWindow, which is the "Nesta Rodada" the clause
    // scopes itself to, readable through SenhorDaBriga#isFinalizacaoActive.
    // TODO the Corrente itself — "este ataque aplica o Efeito Crítico Menor de sua Arma Natural" on a
    // non-crit hit, and one extra application on a crit — has nothing to apply: Weapon
    // #getCriticalEffect() is read by no attack path, and of the Armas Naturais' own effects
    // (Atordoante, Dilacerar, Empalar, Estilhaçador, Cataclismo, Sangramento) only Sangramento has a
    // CriticalEffect class.
    FINALIZACAO(
            "Nesta Rodada, ataques bem-sucedidos com Armas Naturais recebem a Corrente de Efeitos – " +
            "Finalização: se este ataque não for um Acerto Crítico este ataque aplica o Efeito Crítico " +
            "Menor de sua Arma Natural, se este ataque for uma Acerto Crítico o Efeito Crítico será " +
            "aplicado uma vez adicional. Apenas Efeitos Críticos Naturais das Armas Naturais são " +
            "afetados por esta Habilidade, Efeitos Críticos adicionais não são desencadeados.",
            false, fixed(1), ActionCost.ofActionPoints(1), Optional.of(FinalizacaoInteraction.class), 1, 0),

    // Requer 1 Especialização de Senhor da Briga. Passive, and real through
    // SenhorDaBriga#resolveChamarPraBriga: after a natural-weapon hit, the caller records it
    // (CombatantSheet#recordNaturalWeaponHit on the attacker's own sheet) and asks; a target hit in
    // this Rodada and the one before, armed ("Apenas personagens armados"), and not already
    // provoked this combat ("não afeta um mesmo alvo duas vezes mesma Cena" —
    // CombatantSheet#markAffectedThisCombat) is cast a 2-Rodada sheet.ForcedTargeting through
    // CombatantSheet#applyEnchantment — "este é um efeito de Encantamento", so the recipient's own
    // immunity and Ungido ward apply exactly as they do to Orgulho Elduriano's.
    CHAMAR_PRA_BRIGA(
            "Personagens que sofram com seus ataques bem-sucedidos, apenas quando efetuados por Armas " +
            "Naturais, por 2 Rodadas seguidas se sentem provocados, sendo obrigados a te atacar pelas " +
            "próximas 2 Rodadas. Apenas personagens armados são afetados por Chamar pra Briga, este é um " +
            "efeito de Encantamento e não afeta um mesmo alvo duas vezes mesma Cena.",
            false, fixed(0), ActionCost.NONE, Optional.empty(), 1, 0),

    // Requer 1 Especialização e 2 Habilidades de Senhor da Briga. Passive, real through
    // SenhorDaBriga#resolveAttackModifiers on the first Arma Natural attack of each Rodada: an
    // "Ímpar" Rodada reports a 1PA reduction, a "Par" one +1d6 of dano. Rodadas are 0-based in this
    // core, so the table's Rodada 1 is round 0 — the same reading DestinoFeat's Ímpar clause takes.
    // The PA are reported, never charged, like every price in this core.
    // TODO "Esta redução de Tempo de Ação pode afetar Habilidades de Senhor da Briga": a Habilidade
    // that *includes* an attack (Agarrar e Derrubar's 3PA) states its own Tempo de Ativação, and
    // nothing prices an activation by the attack inside it — the reduction reaches plain attacks only.
    PUNHO_DE_FERRO(
            "O Tempo de Ação de seu primeiro ataque com Armas Naturais em cada Rodada Ímpar é reduzido " +
            "em -1PA. Esta redução de Tempo de Ação pode afetar Habilidades de Senhor da Briga. O dano " +
            "do primeiro ataque com Armas Naturais em cada Rodada Par é aumentado em +1d6.",
            true, fixed(0), ActionCost.NONE, Optional.empty(), 1, 2),

    // Requer 1 Especialização de Senhor da Briga. Passive. Real: Margem Crítica Menor +1 with Armas
    // Naturais (SenhorDaBriga#resolveCriticalMarginIncrease); Dano Crítico Menor +2 and Maior +1d6
    // (SenhorDaBriga#resolveCriticalDamage, on top of the baseline Vantagem); and "Sempre que
    // desencadear um Acerto Crítico, suas Defesas aumentam em +1 até o final da Cena" — the caller
    // reports each crit through SenhorDaBriga#recordCriticalHit, which advances a combat-scoped
    // counter that SenhorDaBriga#resolveBaseDefesasBonus reads, so the bonus stacks one per crit and
    // lapses at Scene#endCombat (the table's ruling, 2026-09-22).
    // TODO "A Margem Crítica … Maior de suas Armas Naturais aumenta em +1": an Acerto Crítico Maior
    // is always three 6s in this core, so it has no margin to widen.
    CAMPEAO_DA_TAVERNA(
            "A Margem Crítica Menor e Maior de suas Armas Naturais aumenta em +1. Seu Dano Crítico " +
            "Menor aumenta em +2, enquanto seu Dano Crítico Maior aumenta em +1d6. Sempre que " +
            "desencadear um Acerto Crítico, suas Defesas aumentam em +1 até o final da Cena.",
            true, fixed(0), ActionCost.NONE, Optional.empty(), 1, 0);

    private final String description;
    private final boolean supreme;
    private final PDCost PDCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
    private final int requiredSpecializations;
    private final int requiredOtherAbilities;
}
