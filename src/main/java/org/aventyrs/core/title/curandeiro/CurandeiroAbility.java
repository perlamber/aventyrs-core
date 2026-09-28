package org.aventyrs.core.title.curandeiro;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.EgoCost;
import org.aventyrs.core.title.PDCost;

import java.util.Optional;

import static org.aventyrs.core.title.PDCost.fixed;

/**
 * Curandeiro's own Habilidades/Supremas — those gated on no one Especialização. Every "Requer"
 * clause is enforced data, checked by {@link AventyrTitleAbility#isEligible} through {@code
 * TitleAbilityService#grantTitleAbility}; the two Supremas that name Levantar os Caídos by name add
 * that check in their own {@link #isEligible(AventyrTitle)}.
 */
@Getter
@AllArgsConstructor
public enum CurandeiroAbility implements AventyrTitleAbility {

    // Requer 1 Especialização de 'Curandeiro'. 2PD, Ação Livre. Real through CurandeiroVelozInteraction:
    // once per Rodada, banks a charge that the next healing Habilidade de Curandeiro (reported
    // actionPointCost) or healing Magia Natural/Divina (SpellCastingResult#getActivationTime) spends
    // for -2PA — "Apenas Magias e Habilidades que permitam que outros personagens recuperarem PV".
    CURANDEIRO_VELOZ(
            "Esta Habilidade pode ser ativada apenas 1 vez a cada Rodada. O Tempo de Ativação próxima " +
            "Habilidade ativa de Curandeiro ou sua próxima Conjuração de Magias Naturais ou Divinas tem seu " +
            "Tempo de Ação ou de Conjuração reduzido em -2PA. Apenas Magias e Habilidades que permitam que " +
            "outros personagens recuperarem PV podem ser afetadas por esta Habilidade.",
            false, fixed(2), EgoCost.NONE, ActionCost.FREE_ACTION,
            Optional.of(CurandeiroVelozInteraction.class), 1, 0),

    // Requer 1 Especialização de 'Curandeiro' e outras 2 Habilidades de Curandeiro. Passive. Real,
    // through Curandeiro#bypassesComaHealingCap: the holder's Magias and Habilidades de Curandeiro
    // ignore the Coma cap (CombatantSheet#COMA_HEAL_CAP per heal effect) on a target whose Coma
    // began in the current Cena — "adquiridos na mesma Cena" read as the Coma being acquired, since
    // a heal is not something one acquires.
    LEVANTAR_OS_CAIDOS(
            "Suas Magias e Habilidades de Curandeiro que permitam a recuperação de PV ignoram as Reduções de " +
            "Cura do estado de Coma, mas apenas quando adquiridos na mesma Cena.",
            false, fixed(0), EgoCost.NONE, ActionCost.NONE, Optional.empty(), 1, 2),

    // Requer 'Levantar os Caídos' — #isEligible. 2PD, "+1PA" riding on the heal it accompanies
    // (reported, never deducted, like every PA here). Real, through CurarOsMortosInteraction and
    // Curandeiro#claimRevival: each activation banks one revival charge, and the next Magia
    // Divina/Natural or Habilidade de Curandeiro heal on a target dead for at most half the holder's
    // Medicina e Cura Graduações in Rodadas spends it and lands. "podem retornar à vida se tiverem PV
    // suficientes" needs nothing more — a heal lifting them above negative max PV is what makes them
    // alive (in Coma); short of that they stay dead, keep the PV, and another activation may finish it.
    CURAR_OS_MORTOS(
            "Você pode afetar com Magias Divinas e Naturais, ou Habilidades de Curandeiro, personagens que " +
            "tenham morrido em até uma quantidade de Rodadas igual à metade de suas Graduações em Medicina e " +
            "Cura, personagens curados desta forma podem retornar à vida se tiverem PV suficientes.",
            true, fixed(2), EgoCost.NONE, ActionCost.ofActionPoints(1),
            Optional.of(CurarOsMortosInteraction.class), 0, 0),

    // Requer 'Levantar os Caídos' — #isEligible. "Custo de Ativação: Nenhum, habilidade passiva" beside
    // "Tempo de Ativação: +1PA" — contradictory; read as passive, the Custo line being the one that says so.
    // Real through Curandeiro#resolveDeterminationCostMultiplier/#resolveManaCostMultiplier: while the
    // holder's PV are 0 or below, for at most half their Medicina e Cura Graduações in Rodadas since they
    // fell (CombatantSheet#getRoundsSinceFallen), their Magias and Habilidades de Curandeiro cost double
    // PM and PD. "mesmo que você esteja inconsciente ou morto" lifts nothing: this core refuses no one an
    // action at 0 PV (table ruling, 2026-09-25 — no global gate), so the permission half needs no code.
    BENCAO_DE_BOROS(
            "Você pode Ativar Habilidades de Curandeiro ou Conjurar Magias que permitam personagens recuperar " +
            "PV, incluindo em você mesmo, mesmo que você esteja inconsciente ou morto. Enquanto nestes estados " +
            "suas Magias e Habilidades de Curandeiros custam o dobro de PM e PD. Este efeito pode ser " +
            "utilizado por uma quantidade de Rodadas igual à metade das suas Graduações em Medicina e Cura.",
            true, fixed(0), EgoCost.NONE, ActionCost.NONE, Optional.empty(), 0, 0);

    private final String description;
    private final boolean supreme;
    private final PDCost PDCost;
    private final EgoCost egoCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
    private final int requiredSpecializations;
    private final int requiredOtherAbilities;

    /** The count prerequisite, plus the Levantar os Caídos both Supremas name by name. */
    @Override
    public boolean isEligible(final AventyrTitle title) {
        boolean counted = AventyrTitleAbility.super.isEligible(title);
        return switch (this) {
            case CURAR_OS_MORTOS, BENCAO_DE_BOROS -> counted && title.getAbilities().contains(LEVANTAR_OS_CAIDOS);
            default -> counted;
        };
    }
}
