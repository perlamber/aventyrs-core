package org.aventyrs.core.title.santo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.PDCost;

import static org.aventyrs.core.title.PDCost.fixed;

/**
 * Santo's own catalog of Especializações — exactly two per Título (per this codebase's
 * convention for every Título Aventyr), a player may hold both, one, or neither.
 */
@Getter
@AllArgsConstructor
public enum SantoSpecialization implements AventyrTitleSpecialization {

    // "Apenas 'Santos' podem adquirir esta especialização" — unenforced, per this codebase's
    // established "no eligibility validation service" restraint (see CLAUDE.md's "Adding a
    // new Perícia" section). The "recupera PV como se passasse por um Descanso Curto" branch
    // is real — see #resolveShortRestHealAmount below. The "Remover um Malefício, escolhido
    // entre Doença, Encantamento ou Maldição" branch is partly unblocked: Malefícios are real
    // now (ConditionType/Condition) and CombatantSheet#removeCondition is the removal itself, so
    // two of the three named kinds map straight onto ConditionType.DOENTE and AMALDICOADO.
    // TODO: "Encantamento" is not a Condição at all — it is MagicType.ENCANTAMENTO, so removing
    // one means dispelling an active Magia of that type from the target, and nothing tracks which
    // Magias are currently affecting a combatant (Scene holds only ActiveAreaSpellEffect, keyed
    // to a position rather than to a target).
    // TODO: this Habilidade has no entry point that takes the player's choice of which to remove.
    ABENCOADO_PELA_LUZ(
            "Seu toque tem capacidades curativas, ao tocar outro personagem você pode " +
            "escolher entre fazer com que ele recupere 3+ Quantidade de Habilidades de " +
            "Abençoado pela Luz PV, ou Remover um Malefício, escolhido entre Doença, " +
            "Encantamento, Maldição ou Veneno. Pontos de Vida utilizados para ativar esta " +
            "Habilidade só podem ser recuperados com Descansos Verdadeiros.",
            fixed(0), ActionCost.ofActionPoints(2), Optional.of(AbencoadoPelaLuzInteraction.class)) {
        // "Custo de Ativação: 3PV, Tempo de Ativação: 2PA" — the PDCost is genuinely 0 under V19,
        // which prices this Especialização entirely in PV (paid via AbstractTitleAbilityInteraction
        // #resolveHitPointCost; see AbencoadoPelaLuzInteraction.TOUCH_HIT_POINT_COST), not merely
        // unmodeled. The inherited isPassive() already answers false off the 2PA cost. Kept as an
        // explicit override because this constant's activeness is a fact about its rules text, not
        // something to re-derive if the formula moves again. (Its twin below overrides for a
        // different reason — see there.)
        @Override
        public boolean isPassive() {
            return false;
        }
    },

    // "Apenas 'Santos' podem adquirir esta especialização" — unenforced. "Custo de Ativação:
    // Variável" / "Tempo de Ativação: Variável, conforme o ataque" both refer to Fúria dos
    // Deuses' own PV cost (3 or 4, chosen per attack), not a PD/PA cost at all — PDCost/
    // actionPointCost are 0 here, genuinely, not merely unmodeled. Fúria dos Deuses itself is
    // real now through FuriaDosDeusesInteraction, which prices the three tiers (1PV Vantagem /
    // 3PV GD -1 nível / 5PV both the GD reduction and 1d6+Vigor of dano) and reports what the one
    // attack gains. That closed the "spend a resource for a one-time roll effect" gap this comment
    // used to cite: the spend is an ordinary Título activation and the roll it modifies has not
    // happened yet, so nothing reaches into a resolution in flight. The "Corrente de Efeito" the
    // text names needs no chain of its own either — "o dano deste ataque aumenta 1d6+Vigor" is a
    // flat addition to the same dano roll, reported as dice + flat for the caller to roll.
    // Still TODO: the "recuperados com Descansos Verdadeiros ou efeitos de Roubo de Vida" clause
    // needs the same "locked, Rest/Roubo-de-Vida-only" HP-loss subtype SantoAbility
    // #PROTETOR_DA_VIDA_E_DA_MORTE's own TODO cites.
    ABRACADO_PELA_ESCURIDAO(
            "Sempre que realizar um ataque você pode ativar Fúria dos Deuses. Fúria dos " +
            "Deuses - Sempre que realizar um ataque você pode pagar o Custo de 1PV, 3PV ou " +
            "5PV. Se utilizar 1PV você recebe Vantagem na rolagem de Perícia de Ataque, se " +
            "utilizar 3PV reduz a GD da Rolagem de Perícia de Ataque em -1 Nível, ao utilizar " +
            "5PV, adicionalmente à redução de GD, seu ataque recebe a Corrente de Efeito – " +
            "Fúria dos Deuses Maior: O dano deste ataque aumenta 1d6+Vigor. Pontos de Vida " +
            "perdidos desta forma só podem ser recuperados com Descansos Verdadeiros ou " +
            "efeitos de Roubo de Vida.",
            fixed(0), ActionCost.NONE, Optional.of(FuriaDosDeusesInteraction.class)) {
        // The inherited isPassive() formula (a NONE Tempo de Ativação) would misclassify this
        // as passive — its ActionCost.NONE reflects "cost is entirely PV, not PD/PA" (see the
        // constant's own comment above), not "no cost at all". Fúria dos Deuses is a genuine
        // per-attack active choice, so this overrides the default instead of letting the
        // derived formula guess wrong.
        @Override
        public boolean isPassive() {
            return false;
        }
    };

    /** The "3+" of Abençoado pela Luz's "recupere 3+ Quantidade de Habilidades … PV". */
    static final int BASE_TOUCH_HEAL = 3;

    private final String description;
    private final PDCost PDCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;

    /**
     * The "recupere 3+ Quantidade de Habilidades de Abençoado pela Luz PV" branch of Abençoado
     * pela Luz's touch effect — real, tested arithmetic: a caller applies the returned amount to
     * the touched target via {@code CombatantSheet#heal}.
     *
     * <p>Takes the holder's {@link AventyrTitle} because "Quantidade de Habilidades de Abençoado
     * pela Luz" counts the Habilidades <em>this Santo holds</em> that are gated on this same
     * Especialização, which an enum constant cannot know by itself. The count is taken the same
     * way {@code AventyrTitleAbility#isEligible} scopes its own "outras Habilidades" clause —
     * by each held ability's {@code getRequiredSpecialization()} — so the two readings of "de
     * Abençoado pela Luz" can never drift apart.
     *
     * <p>Only meaningful for {@code ABENCOADO_PELA_LUZ}; returns 0 for {@code
     * ABRACADO_PELA_ESCURIDAO} (which has no equivalent touch-heal clause of its own), rather
     * than throwing, so a caller never needs to guard on which constant it's holding.
     *
     * <p>V19 replaced the previous revision's "recupera PV como se passasse por um Descanso Curto"
     * with this flat count, so nothing here reads {@code RestService} any more.
     */
    public int resolveTouchHealAmount(final AventyrTitle holder) {
        if (this != ABENCOADO_PELA_LUZ || holder == null) {
            return 0;
        }
        long gatedAbilities = holder.getAbilities().stream()
                .filter(ability -> Optional.of(this).equals(ability.getRequiredSpecialization()))
                .count();
        return BASE_TOUCH_HEAL + (int) gatedAbilities;
    }
}
