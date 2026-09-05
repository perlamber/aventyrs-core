package org.aventyrs.core.title.santo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.rest.RestService;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitleSpecialization;

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
            "escolher entre fazer com que ele recupere PV como se passasse por um Descanso " +
            "Curto, ou Remover um Malefício, escolhido entre Doença, Encantamento ou Maldição.",
            1, 2, Optional.of(AbencoadoPelaLuzInteraction.class)){
        // The inherited isPassive() formula (actionPointCost==0 && !reaction && !freeAction)
        // would misclassify this as passive — its 0/0 PD/PA reflects "cost is entirely PV,
        // not PD/PA" (see the constant's own comment above), not "no cost at all". Fúria dos
        // Deuses is a genuine per-attack active choice, so this overrides the default instead
        // of letting the derived formula guess wrong.
        @Override
        public boolean isPassive() {
            return false;
        }
    },

    // "Apenas 'Santos' podem adquirir esta especialização" — unenforced. "Custo de Ativação:
    // Variável" / "Tempo de Ativação: Variável, conforme o ataque" both refer to Fúria dos
    // Deuses' own PV cost (3 or 4, chosen per attack), not a PD/PA cost at all — PDCost/
    // actionPointCost are 0 here, genuinely, not merely unmodeled. Fúria dos Deuses itself is
    // fully TODO'd, in three separate pieces: (1) the "-1 nível" GD reduction needs the same
    // "spend a resource for a one-time roll effect" transaction race/Orc.java's own Agnação
    // Ancestral citation already flags as missing (Pontos de Vida spent on a roll-modifying
    // effect, not just casting, has no equivalent transaction anywhere in this core); (2) "+1d6"
    // via a Corrente de Efeito needs both the still-unbuilt Corrente de Efeitos system
    // (AutocontroleAdvantage#RESOLUTO's own citation) and this core's "never rolls dice"
    // boundary (a d6 bonus has no numeric value this core could compute even if Corrente de
    // Efeitos existed); (3) the "recuperados com Descansos ou Roubo de Vida, mas não com
    // efeitos similares a Descansos" clause needs the same "locked, Rest/Roubo-de-Vida-only"
    // HP-loss subtype SantoAbility#PROTETOR_DA_VIDA_E_DA_MORTE's own TODO already cites — this
    // one is even narrower (explicitly excluding Rest-*like* effects, not just non-Rest
    // recovery in general), a nuance worth keeping in mind once that subtype is built for real.
    ABRACADO_PELA_ESCURIDAO(
            "Sempre que realizar um ataque você pode ativar Fúria dos Deuses. Fúria dos " +
            "Deuses - Sempre que realizar um ataque você pode gastar 3PV ou 4PV. Se gastar " +
            "3PV você pode reduzir a GD da rolagem de Perícia de Ataque -1 nível, se gastar " +
            "4PV adicionalmente seu ataque recebe a Corrente de Efeito – Fúria dos Deuses " +
            "Maior: O dano deste ataque aumenta +1d6. Pontos de Vida perdidos desta forma só " +
            "podem ser recuperados com Descansos ou Roubo de Vida, mas não com efeitos " +
            "similares a Descansos.",
            0, 0, Optional.empty()) {
        // The inherited isPassive() formula (actionPointCost==0 && !reaction && !freeAction)
        // would misclassify this as passive — its 0/0 PD/PA reflects "cost is entirely PV,
        // not PD/PA" (see the constant's own comment above), not "no cost at all". Fúria dos
        // Deuses is a genuine per-attack active choice, so this overrides the default instead
        // of letting the derived formula guess wrong.
        @Override
        public boolean isPassive() {
            return false;
        }
    };

    private final String description;
    private final int PDCost;
    private final int actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;

    /**
     * The "recupera PV como se passasse por um Descanso Curto" branch of Abençoado pela Luz's
     * touch effect — real, tested: {@link RestService#getRecoveredHitPoints} already computes
     * exactly this formula (Vigor total × {@link RestType#CURTO}'s attribute multiplier, plus
     * any {@code AttributeAbility#resolveRestHitPointsBonus} bonus), so this needs no missing
     * system of its own — a caller applies the returned amount to the touched target via
     * {@code CombatantSheet#heal}. Only meaningful for {@code ABENCOADO_PELA_LUZ}; returns 0
     * for {@code ABRACADO_PELA_ESCURIDAO} (which has no equivalent touch-heal clause of its
     * own), rather than throwing, so a caller never needs to guard on which constant it's
     * holding.
     */
    public int resolveShortRestHealAmount(final Character target, final RestService restService) {
        if (this != ABENCOADO_PELA_LUZ) {
            return 0;
        }
        return restService.getRecoveredHitPoints(target, RestType.CURTO);
    }
}
