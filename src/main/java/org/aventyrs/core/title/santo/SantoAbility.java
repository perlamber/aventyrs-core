package org.aventyrs.core.title.santo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitleAbility;

/**
 * Santo's own catalog of Habilidades/Supremas — see each constant's own comment for which
 * pieces of its rules text are mechanically real today versus TODO'd.
 */
@Getter
@AllArgsConstructor
public enum SantoAbility implements AventyrTitleAbility {

    // Requer 1 Especialização de Santo (unenforced, per this codebase's established
    // "no eligibility validation service" restraint — see CLAUDE.md's "Adding a new Perícia"
    // section). Fully TODO'd: needs the Item/Equipamento entity (Armadura/Escudo, an
    // "Abençoado" blessed state — org.aventyrs.core.item.ItemInteraction is still a bare
    // stub, the same gap ResourcesAdvantage.BARGANHISTA/HERANCA_FAMILIAR and
    // ProfissaoCompetencyAbility.FORJA_VULCANA already cite), an Encantamento/Maldição
    // classification (no such tag exists anywhere in this core — see Withering's own "still
    // doesn't exist" citation), and a public duration-mutator (TemporaryEffect#tick only ever
    // decrements by exactly 1, package-private — halving a held effect's remaining duration
    // isn't expressible). Only the identity/cost data below (3PD, 2PA) is real.
    PROTECAO_UNGIDA(
            "Abençoa um item do tipo Armadura ou Escudo que você esteja utilizando por 3 " +
            "Rodadas. Enquanto estiver usando pelo menos um item Abençoado todo o dano que " +
            "seria causado a você é reduzido à metade. Se utilizar uma Armadura e um Escudo " +
            "Ungido ao mesmo tempo a Duração de efeitos nocivos de Encantamentos e Maldições " +
            "são reduzidas pela metade.",
            false, 3, 2, false, Optional.empty()),

    // Requer 1 Especialização e 2 outras habilidades de Santo (unenforced). The self-facing RA
    // half ("Você recebe RA...") is real — see #resolveAbsoluteDamageReduction below, wired
    // into DamageServiceImpl. The ally-facing half ("Aliados adjacentes ... recebem RA") stays
    // TODO'd: every existing cross-character bonus mechanism in this core is either an
    // explicit roll-time grant (ArtesCompetencyAbility.DOM_BARDICO) or an initiative-win-
    // triggered Blessing (InitiativeBlessingService) — nothing supports "my always-on passive
    // continuously grants a bonus to a nearby ally's own damage calculation with no trigger
    // event."
    BASTIAO_DOS_NECESSITADOS(
            "Você recebe RA enquanto estiver adjacente à um aliado com menos PV que você. " +
            "Aliados adjacentes, apenas aqueles com menos PV que você, recebem RA.",
            false, 0, 0, false, Optional.empty()) {
        @Override
        public int resolveAbsoluteDamageReduction(final SceneContext sceneContext, final boolean hasLowerPvAdjacentAlly) {
            return hasLowerPvAdjacentAlly ? DamageService.DEFAULT_DAMAGE_REDUCTION : 0;
        }
    },

    // Requer 1 Especialização e 2 outras Habilidades de Santo (unenforced). Fully TODO'd: this
    // core has no positioning/teleportation concept (it never does geometry — see Range's own
    // javadoc) and no "intercept/redirect an in-progress attack to a different target"
    // mechanism anywhere — DamageService/AbstractSkillInteraction only ever compute a roll/hit
    // once, against data already resolved; nothing models an attack as a stateful transaction
    // another Character can interject into mid-resolution. The "ataque ainda deve superar as
    // suas Defesas" clause additionally needs the still-missing Defesas system (see Santo's own
    // TODO). Only identity/cost data is real (Suprema, 2PD, Reação).
    GUARDA_VIDAS(
            "Você pode se teletransportar para a frente de um aliado em Distância Curta, se " +
            "tornando o alvo do ataque em seu lugar. O ataque ainda deve superar as suas " +
            "Defesas para lhe infligir danos. Esta Habilidade pode ser ativada apenas quando " +
            "um aliado for alvo de um ataque.",
            true, 2, 0, true, Optional.empty()),

    // Requer 1 Especialização e 4 outras Habilidades de Santo (unenforced). Fully TODO'd,
    // three distinct gaps: (1) no floor-at-1PV concept exists (ResourcePool#spend/
    // CharacterSheet#applyDamage have no such clamp), (2) no "redirect an ally's damage to
    // yourself" mechanism exists (the inverse of BASTIAO_DOS_NECESSITADOS' own ally-facing
    // gap — damage flowing to the holder instead of a bonus flowing out), and (3) no "locked,
    // Rest/Roubo-de-Vida-only-recoverable" HP-loss subtype exists (LifeStealService/
    // RestService both exist, but neither models a pool a plain heal can't touch).
    PROTETOR_DA_VIDA_E_DA_MORTE(
            "Enquanto você estiver consciente, os PV de seus aliados em Distância Curta não " +
            "podem ser reduzidos à menos que 1PV. Todo o dano que seria causado aos seus " +
            "aliados, que reduziram os PV deles para zero ou menos é causado a você, pontos " +
            "de vida perdidos desta maneira só podem ser recuperados com Descansos ou Roubo " +
            "de Vida.",
            true, 0, 0, false, Optional.empty());

    private final String description;
    private final boolean supreme;
    private final int PDCost;
    private final int actionPointCost;
    private final boolean reactionActivation;
    private final Optional<Class<? extends Interaction>> interactionClass;
}
