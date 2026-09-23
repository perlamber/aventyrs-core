package org.aventyrs.core.title.senhordabriga;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.PDCost;

import java.util.Optional;

import static org.aventyrs.core.title.PDCost.fixed;

/**
 * Senhor da Briga's own catalog of Especializações — exactly two, a player may hold both, one, or
 * neither. <b>Both are passive</b>, so neither names an Interaction: what each grants is resolved
 * by {@link SenhorDaBriga}'s own scans (Margem Crítica, Defesas), which read {@link
 * SenhorDaBriga#getSpecializations()}.
 */
@Getter
@AllArgsConstructor
public enum SenhorDaBrigaSpecialization implements AventyrTitleSpecialization {

    // "Apenas Senhores da Briga podem adquirir esta especialização" — enforced by
    // SenhorDaBriga#grantSpecialization. "Custo de Ativação: Nenhum, habilidade passiva". Real:
    // "tem a Margem Crítica Menor aumentada em +2 números" with Armas Naturais, through
    // SenhorDaBriga#resolveCriticalMarginIncrease (CriticalServiceImpl's Título scan).
    // "recebem Guilhotina como Efeito Crítico Adicional" is real (0.0.49):
    // SenhorDaBriga#resolveAdditionalCriticalEffects adds GUILHOTINA to every natural-weapon critical.
    PUNHO_INIGUALAVEL(
            "Seus ataques com Armas Naturais recebem Guilhotina como Efeito Crítico Adicional e tem a " +
            "Margem Crítica Menor aumentada em +2 números.",
            fixed(0), ActionCost.NONE, Optional.empty()),

    // "Apenas Senhores da Briga podem adquirir esta especialização" — enforced by
    // SenhorDaBriga#grantSpecialization. The rules text prints "Custo de Ativação: 2PD, Tempo de
    // Ativação: 2PA" and splits the effect into a Passivo and an "Efeito Ativo – Defesa Fantasma",
    // but the table ruling (2026-09-22) is that Defesa Fantasma is a *conditional passive*: its
    // bonuses hold for exactly as long as their conditions do, with nothing to activate. So PDCost
    // and ActionCost are NONE deliberately, not unmodeled. Real, through
    // SenhorDaBriga#resolveBaseDefesasBonus: +1 Defesas always; +1 more while armed only with
    // Armas Naturais; +3 more while wearing no Equipamento Defensivo — read as cumulative, the
    // clauses being written as successive increases of the same "Bônus Defensivos". "Suas rolagens
    // de Defesas tem a Margem Crítica Menor aumentada em +2 números" is real through
    // SenhorDaBriga#resolveCriticalMarginIncrease on Esquiva e Aparar, the Perícia a Defesa rolls.
    // "recebem Ímpeto Defensivo como Efeito Crítico adicional" is real (0.0.49):
    // SenhorDaBriga#resolveAdditionalDefensiveCriticalEffects, applied on a Defesa's Acerto Crítico.
    FANTASMA_DO_RINGUE(
            "Efeito Passivo: Você recebe Bônus de +1 em suas Defesas. Defesa Fantasma: Os Bônus " +
            "Defensivos de Fantasma do Ringue aumentam em +1 enquanto você estiver armado apenas com " +
            "suas Armas Naturais. Os Bônus Defensivos de Fantasma do Ringue aumentam em +3 enquanto você " +
            "não utilizar Equipamentos Defensivos (exceto Defesas Naturais). Suas rolagens de Defesas " +
            "tem a Margem Crítica Menor aumentada em +2 números e recebem Ímpeto Defensivo como Efeito " +
            "Crítico adicional.",
            fixed(0), ActionCost.NONE, Optional.empty());

    private final String description;
    private final PDCost PDCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
}
