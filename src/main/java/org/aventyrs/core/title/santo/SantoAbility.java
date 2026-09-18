package org.aventyrs.core.title.santo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.action.ReactionContext;
import org.aventyrs.core.action.ReactionTrigger;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.Teleportation;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.PDCost;

import static org.aventyrs.core.title.PDCost.fixed;

/**
 * Santo's own catalog of Habilidades/Supremas — see each constant's own comment for which
 * pieces of its rules text are mechanically real today versus TODO'd. Every constant's own
 * "Requer N Especialização(ões) e M outras Habilidades de Santo" prerequisite is real data
 * (requiredSpecializations/requiredOtherAbilities below), enforced via {@link
 * AventyrTitleAbility#isEligible} by {@code TitleAbilityService#grantTitleAbility} — unlike
 * most "Requer N..." clauses elsewhere in this codebase, which stay documented-but-unenforced.
 */
@Getter
@AllArgsConstructor
public enum SantoAbility implements AventyrTitleAbility {

    // Requer 1 Especialização de Santo — enforced (see class javadoc). The main clause is real
    // through ProtecaoUngidaInteraction: the activation gates on the holder actually using an
    // ItemCategory.ARMOR or SHIELD (Character#getEquipment is real now — the old TODO here citing
    // a missing Item entity was stale), and "todo o dano que seria causado a você é reduzido à
    // metade" is granted for 3 Rodadas as a ModifierType.HALF_DAMAGE Blessing, which
    // DamageServiceImpl reads off the sheet. Which item is blessed is deliberately not recorded —
    // every consequence the text names lands on the wearer (see that class's javadoc).
    // Still TODO: the "Armadura e Escudo Ungido ao mesmo tempo" clause halving the Duração of
    // harmful Encantamentos/Maldições needs two things this core lacks — an Encantamento/Maldição
    // classification of a held effect (no such tag exists; see Withering's own citation) and a
    // public duration mutator (TemporaryEffect#tick only ever decrements by 1, package-private).
    PROTECAO_UNGIDA(
            "Abençoa um item do tipo Armadura ou Escudo que você esteja utilizando por 3 " +
            "Rodadas. Enquanto estiver usando pelo menos um item Abençoado todo o dano que " +
            "seria causado a você é reduzido à metade. Se utilizar uma Armadura e um Escudo " +
            "Ungido ao mesmo tempo a Duração de efeitos nocivos de Encantamentos e Maldições " +
            "são reduzidas pela metade.",
            false, fixed(3), ActionCost.ofActionPoints(2), Optional.of(ProtecaoUngidaInteraction.class), 1, 0),

    // Requer 1 Especialização e 2 outras habilidades de Santo — enforced (see class javadoc).
    // Both halves are real. The self-facing one ("Você recebe RA...") resolves through
    // #resolveAbsoluteDamageReduction; the ally-facing one ("Aliados adjacentes ... recebem
    // RA") through #resolveAllyAbsoluteDamageReduction, which DamageServiceImpl reaches by
    // scanning the target's own adjacent allies for holders rather than by granting anything —
    // see that hook's javadoc for why a continuous proximity buff must not be a TemporaryBonus.
    // The two booleans are the same PV comparison in opposite directions, both resolved by
    // DamageServiceImpl.
    BASTIAO_DOS_NECESSITADOS(
            "Você recebe RA enquanto estiver adjacente à um aliado com menos PV que você. " +
            "Aliados adjacentes, apenas aqueles com menos PV que você, recebem RA.",
            false, fixed(0), ActionCost.NONE, Optional.empty(), 1, 2) {
        @Override
        public int resolveAbsoluteDamageReduction(final SceneContext sceneContext, final boolean hasLowerPvAdjacentAlly) {
            return hasLowerPvAdjacentAlly ? DamageService.DEFAULT_DAMAGE_REDUCTION : 0;
        }

        @Override
        public int resolveAllyAbsoluteDamageReduction(final SceneContext sceneContext, final boolean allyHasLowerPv) {
            return allyHasLowerPv ? DamageService.DEFAULT_DAMAGE_REDUCTION : 0;
        }
    },

    // Requer 1 Especialização e 2 outras Habilidades de Santo — enforced (see class javadoc).
    // Built: the whole clause, through GuardaVidasInteraction. The interception works because it
    // happens *before* the attack is built at all — the client lists this Reação, activates it,
    // and then names the Santo as the attack's defender from the start, so AttackDelivery/
    // AttackReceiver resolve an ordinary attack and never learn a Reação occurred. That is also
    // what satisfies "o ataque ainda deve superar as suas Defesas" by construction: the attack is
    // rolled against the Santo's own sheet, which since 0.0.43 includes Despertar's own
    // +2 Defesas (AventyrTitle#resolveBaseDefesasBonus) like any other source.
    // The teleport is a *reach*, not a move: Teleportation reports how far, and the caller applies
    // position, since this core holds none (see Range's and Teleportation's own javadoc).
    // Still TODO: nothing anywhere counts a Reação as spent, so a Santo who has already used
    // theirs this Rodada is still offered this one — ReactionsService reports a maximum, not a
    // pool. Cite that, not "no interception mechanism exists".
    GUARDA_VIDAS(
            "Você pode se teletransportar para a frente de um aliado em Distância Curta, se " +
            "tornando o alvo do ataque em seu lugar. O ataque ainda deve superar as suas " +
            "Defesas para lhe infligir danos. Esta Habilidade pode ser ativada apenas quando " +
            "um aliado for alvo de um ataque.",
            true, fixed(2), ActionCost.REACTION, Optional.of(GuardaVidasInteraction.class), 1, 2) {
        @Override
        public ReactionTrigger getReactionTrigger() {
            return ReactionTrigger.ALLY_TARGETED_BY_ATTACK;
        }

        /** "para a frente de um aliado em Distância Curta". */
        @Override
        public Teleportation resolveTeleportation() {
            return Teleportation.of(Range.DISTANCIA_CURTA);
        }

        /**
         * Offered only when the threatened combatant really is an ally of this Santo and really is
         * close enough to teleport in front of. Read off the <b>reactor's own</b> SceneContext,
         * which is the correct snapshot — the reach is measured from the Santo.
         */
        @Override
        public boolean isReactionAvailable(final ReactionContext context) {
            CombatantSheet ally = context.getThreatenedAlly();
            SceneContext reactorContext = context.getReactorContext();
            if (ally == null || reactorContext == null || ally.getId().equals(context.getReactor().getId())) {
                return false;
            }
            return reactorContext.getAllies().contains(ally)
                    && resolveTeleportation().reaches(reactorContext.getDistanceTo(ally));
        }
    },

    // Requer 1 Especialização e 4 outras Habilidades de Santo — enforced (see class javadoc).
    // Otherwise fully TODO'd, three distinct gaps: (1) no floor-at-1PV concept exists
    // (ResourcePool#spend/CombatantSheet#applyDamage have no such clamp), (2) no "redirect an
    // ally's damage to yourself" mechanism exists (the inverse of BASTIAO_DOS_NECESSITADOS'
    // own ally-facing gap — damage flowing to the holder instead of a bonus flowing out), and
    // (3) no "locked, Rest/Roubo-de-Vida-only-recoverable" HP-loss subtype exists
    // (LifeStealService/RestService both exist, but neither models a pool a plain heal can't
    // touch).
    PROTETOR_DA_VIDA_E_DA_MORTE(
            "Enquanto você estiver consciente, os PV de seus aliados em Distância Curta não " +
            "podem ser reduzidos à menos que 1PV. Todo o dano que seria causado aos seus " +
            "aliados, que reduziram os PV deles para zero ou menos é causado a você, pontos " +
            "de vida perdidos desta maneira só podem ser recuperados com Descansos ou Roubo " +
            "de Vida.",
            true, fixed(0), ActionCost.NONE, Optional.empty(), 1, 4);

    private final String description;
    private final boolean supreme;
    private final PDCost PDCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
    private final int requiredSpecializations;
    private final int requiredOtherAbilities;
}
