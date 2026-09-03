package org.aventyrs.core.skill.medicinaecura;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

/**
 * The Habilidades de Competência available to characters trained in Medicina e Cura.
 */
@Getter
@AllArgsConstructor
public enum MedicinaECuraCompetencyAbility implements SkillCompetencyAbility {

    // TODO: once per Rodada, -1PA for this Perícia's own uses specifically, conditioned on
    // holding a kit de primeiros socorros — three gaps block this: (1) no
    // Equipamento/inventory tracking exists on Character to know what's currently in hand,
    // (2) ActionPointsServiceImpl.getSkillRollCost is a single character-wide PA cost, not
    // scoped per-Perícia, so even with an item system there's no hook for a
    // Medicina-e-Cura-only discount today, and (3) no once-per-Rodada usage-limiting
    // mechanism exists either.
    BOM_DOUTOR("Se tiver um kit de primeiros socorros em mãos, uma vez por Rodada os usos " +
            "desta Perícia têm o Tempo de Ação reduzido em -1PA."),

    // TODO: an activated ability rolled against GD Média during another creature's Descanso;
    // on success the target recovers 1d6 additional PV/PM/PD at the end of that Descanso —
    // the GD Média check is expressible now (a SkillRoll states its own targetValue), but this
    // core deliberately never rolls dice, so the "1d6 additional" is the caller's — see the
    // skill package-info. The recovery itself, once resolved, would layer on top of
    // org.aventyrs.core.rest.RestService's existing PV/PM/PD recovery, and nothing hooks a
    // Perícia roll's outcome into a Descanso.
    MEDICINA_ALTERNATIVA("Você pode, antes ou durante o descanso de outra criatura, fazer " +
            "massagens, usar de acupuntura ou outros recursos prazerosos e relaxantes, para " +
            "melhorar a qualidade do descanso do alvo. Faça uma rolagem contra GD média, se " +
            "for bem-sucedido o alvo recuperará 1d6PV, PM e PD adicionais ao fim do " +
            "Descanso."),

    // TODO: raising this roll's own GD to Difícil and succeeding grants the target the PV
    // recovery of a Descanso Curto (org.aventyrs.core.rest.RestService
    // #getRecoveredHitPoints/#applyRest already computes that number), limited to once per
    // target until their next Descanso Longo (even after a failed attempt), plus a Corrente
    // de Efeitos — Milagre Maior tier that also recovers PM/PD (RestService#applyRest already
    // does that too). "Raising this roll's own GD and succeeding" is expressible now — a caller
    // states the harder targetValue and InteractionResult reports whether it was beaten. What
    // remains is the per-target "already attempted since last Descanso Longo" tracker, and a way
    // for a Perícia roll's success to hand another character a Descanso Curto's recovery.
    MILAGREIRO("Quando fizer rolagens de Medicina e Cura para tratar e estancar ferimentos " +
            "você pode aumentar o Grau de Dificuldade para Difícil, se o fizer e for " +
            "bem-sucedido os personagens afetados recuperam PV como se passassem por um " +
            "Descanso Curto. Um mesmo personagem não pode ser afetado por Milagreiro uma " +
            "segunda vez até passar por um Descanso Longo, mesmo que a primeira tentativa " +
            "tenha falhado. Esta Habilidade possui a Corrente de Efeitos – Milagre Maior: O " +
            "personagem alvo também recupera PD e PM."),

    // TODO: once per day (renewed after a Descanso Longo, with more uses unlocked at the
    // 5th and 10th graduation), lets this Perícia's roll be made as an Ação Livre instead of
    // costing its normal PA — a different mechanic than a PA discount: there's no way to
    // mark a specific skill use as consuming an Ação Livre instead of going through
    // ActionPointsServiceImpl.getSkillRollCost at all, nor a per-day/renews-on-Descanso-Longo
    // usage-limiting mechanism (same shape as FurtividadeExcellency.FOCADO/LENDA's
    // once/three-times-per-Cena limiter, just scoped to a Descanso instead of a Cena),
    // neither of which exist yet.
    SOCORRO_IMEDIATO("A cada dia você efetuar rolagens desta Perícia como Ação Livre, este " +
            "benefício é renovado após passar por um Descanso Longo. Você ganhar usos " +
            "adicionais deste benefício na 5ª e 10ª Graduação."),

    // TODO: +1 Rodada to potions'/antidotes' Duração, then +1 more at the 5th and 10th
    // graduation — no potion/antidote entity or duration-tracking system exists yet (same
    // gap as DominioDoManaCompetencyAbility.CONJURACAO_DURADOURA).
    ALQUIMIA_MAIOR("Suas poções e antídotos tem a Duração de efeito aumentada em +1 " +
            "Rodada, então em +1 Rodada na 5ª e 10ª Graduação.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.MEDICINA_E_CURA;
    }
}
