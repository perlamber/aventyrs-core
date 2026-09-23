package org.aventyrs.core.title.giganteenfurecido;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.action.ReactionContext;
import org.aventyrs.core.action.ReactionTrigger;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.EgoCost;
import org.aventyrs.core.title.PDCost;

import java.util.Optional;

import static org.aventyrs.core.title.PDCost.fixed;

/**
 * Gigante Enfurecido's own Habilidades/Supremas — those gated on no one Especialização. Every "Requer"
 * clause is enforced data, checked by {@link AventyrTitleAbility#isEligible} through {@code
 * TitleAbilityService#grantTitleAbility}; the two that name another Habilidade by name (Uno com a Ira,
 * Frenesi Esmeralda) add that check in their own {@link #isEligible(AventyrTitle)}.
 */
@Getter
@AllArgsConstructor
public enum GiganteEnfurecidoAbility implements AventyrTitleAbility {

    // Requer 1 Especialização de Gigante Enfurecido. Real through ProlongarDescontroleInteraction:
    // "+2 Rodadas" on the running Frenzy; "só pode ser ativada durante o efeito de Frenesi" refuses
    // otherwise (FRENZY_REQUIRED).
    PROLONGAR_DESCONTROLE(
            "Você pode aumentar a duração de seu Frenesi em +2 Rodadas. Esta Habilidade só pode ser ativada " +
            "durante o efeito de Frenesi do Gigante Enfurecido.",
            false, fixed(2), EgoCost.NONE, ActionCost.ofActionPoints(2),
            Optional.of(ProlongarDescontroleInteraction.class), 1, 0),

    // Requer 'Prolongar Descontrole' e outras 2 Habilidades — the named half in #isEligible. Passive,
    // real on two fronts: the first 2 temporary Autocontrole spent on Frenesi effects in each Frenesi
    // come back "após 2 Rodadas, mas apenas se seu Frenesi ainda estiver ativo e você estiver
    // consciente" (a guarded recovery DelayedEgoGrant, scheduled by GiganteEnfurecido#recordFrenzySpend);
    // and GiganteEnfurecido#endFrenzyVoluntarily is unlocked, leaving an Exhaustion "até que passe por
    // um Descanso Curto Verdadeiro" (CombatantSheet#applyEffectUntilTrueRest).
    UNO_COM_A_IRA(
            "Os 2 primeiros pontos temporários de 'Autocontrole' que utilizar para ativar efeitos de Frenesi " +
            "são recuperados após 2 Rodadas, mas apenas se seu Frenesi ainda estiver ativo e você estiver " +
            "consciente. Você agora pode encerrar seu Frenesi voluntariamente, mas encerra-lo de " +
            "antecipadamente te deixa física e mentalmente exausto, fazendo-o sofrer Desvantagem em rolagens " +
            "de Perícias e Danos até que passe por um Descanso Curto Verdadeiro.",
            false, fixed(0), EgoCost.NONE, ActionCost.NONE, Optional.empty(), 1, 3),

    // Requer 'Uno com a Ira' — #isEligible. "Custo de Ativação: Sempre Ativo" yet "Enquanto em Frenesi
    // você pode ativar esta Suprema": read as a free switch during a Frenesi, so an Ação Livre costing
    // nothing. Real through FrenesiEsmeraldaInteraction: Força +2, Vantagem (+2) on Atenção and on the
    // Domínio do Mana roll of a damaging Magia (SpellCastingServiceImpl), Movimento +2UD, Defesas −4 —
    // all on the Frenzy, so they end with it.
    FRENESI_ESMERALDA(
            "Você consegue entrar num estado de Frenesi ainda mais profundo, se tornando mais forte, " +
            "incontrolável, irracional e imprudente. Enquanto em Frenesi você pode ativar esta Suprema, se o " +
            "fizer você Bônus de +2 em Força, Vantagem em Rolagens de Atenção e Conjuração de Magias Ofensivas " +
            "(que inflijam danos em seus alvos), seu Movimento Base aumenta em +2UD, mas você sofre Redutor " +
            "de -4 em Defesas.",
            true, fixed(0), EgoCost.NONE, ActionCost.FREE_ACTION,
            Optional.of(FrenesiEsmeraldaInteraction.class), 1, 3),

    // Requer 3 Habilidades de Gigante Enfurecido. Real through FrenesiReativoInteraction: a Reação on
    // ReactionTrigger#SELF_HIT_BY_SUCCESSFUL_ATTACK starting the Frenesi at "+1 ponto temporário de
    // 'Autocontrole'" over its own. "Você pode ativar qualquer número de Especializações, Habilidades e
    // Supremas de Gigante Enfurecido nesta mesma Reação" — the Especializações ride on the request
    // (FrenzyMode choices, as on a plain Frenesi); the Habilidades and Supremas are activated by the
    // caller right after, inside the same Reação: no PA is deducted anywhere in this core.
    FRENESI_REATIVO(
            "Você pode iniciar seu Frenesi em resposta às ações inimigas, mas apenas se você for alvo de um " +
            "ataque bem-sucedido. Você pode ativar qualquer número de Especializações, Habilidades e Supremas " +
            "de Gigante Enfurecido nesta mesma Reação, desde que possa pagar seus Custos de Ativação.",
            true, fixed(0), EgoCost.autocontrole(2), ActionCost.REACTION,
            Optional.of(FrenesiReativoInteraction.class), 0, 3) {
        @Override
        public ReactionTrigger getReactionTrigger() {
            return ReactionTrigger.SELF_HIT_BY_SUCCESSFUL_ATTACK;
        }

        /** "iniciar seu Frenesi" — only while none of the reactor's own is running. */
        @Override
        public boolean isReactionAvailable(final ReactionContext context) {
            return context.getReactor().getOwnFrenzy().isEmpty();
        }
    };

    private final String description;
    private final boolean supreme;
    private final PDCost PDCost;
    private final EgoCost egoCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
    private final int requiredSpecializations;
    private final int requiredOtherAbilities;

    /** The count prerequisite, plus the Habilidade Uno com a Ira and Frenesi Esmeralda name by name. */
    @Override
    public boolean isEligible(final AventyrTitle title) {
        boolean counted = AventyrTitleAbility.super.isEligible(title);
        return switch (this) {
            case UNO_COM_A_IRA -> counted && title.getAbilities().contains(PROLONGAR_DESCONTROLE);
            case FRENESI_ESMERALDA -> counted && title.getAbilities().contains(UNO_COM_A_IRA);
            default -> counted;
        };
    }
}
