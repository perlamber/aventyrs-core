package org.aventyrs.core.character.services;

import org.aventyrs.core.action.GenericReaction;
import org.aventyrs.core.action.GenericReactionOption;
import org.aventyrs.core.action.ReactionTrigger;
import org.aventyrs.core.scene.SceneContext;

import lombok.NonNull;
import org.aventyrs.core.action.ReactionContext;
import org.aventyrs.core.action.ReactionOption;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleAbility;

import java.util.ArrayList;
import java.util.List;

public class ReactionOptionsServiceImpl implements ReactionOptionsService {

    private final ReactionsService reactionsService;
    private final DeterminationPointsService determinationPointsService;
    private final AttackRangeService attackRangeService = new AttackRangeServiceImpl();
    private final SacrificeService sacrificeService = new SacrificeServiceImpl();
    private final PreparationService preparationService = new PreparationServiceImpl();

    public ReactionOptionsServiceImpl() {
        this(new ReactionsServiceImpl(), new DeterminationPointsServiceImpl());
    }

    public ReactionOptionsServiceImpl(final ReactionsService reactionsService,
                                      final DeterminationPointsService determinationPointsService) {
        this.reactionsService = reactionsService;
        this.determinationPointsService = determinationPointsService;
    }

    @Override
    public List<ReactionOption> getAvailableReactions(@NonNull final ReactionContext context) {
        if (context.getReactorContext() == null) {
            return List.of();
        }
        CombatantSheet reactor = context.getReactor();
        // Resolved once, not per option: both are facts about the reactor, not about the ability.
        // A held Condição refusing Reações (Confuso, Imobilizado, Desacordado) leaves none to take.
        boolean hasReaction = reactionsService.getRemainingReactions(
                reactor, context.getTurnNumber(), context.getReactorContext()) >= 1
                && !reactor.isActionPrevented(org.aventyrs.core.action.ActionKind.REACTION, context.getReactorContext());
        int availableDeterminationPoints = determinationPointsService.getCurrentDeterminationPoints(
                reactor.getCharacter(), reactor);

        List<ReactionOption> options = new ArrayList<>();
        for (AventyrTitleAbility ability : heldTitleAbilities(reactor)) {
            ActionCost cost = ability.getActionPointCost();
            if (cost.kind() != ActionCost.Kind.REACTION
                    || ability.getReactionTrigger() != context.getTrigger()
                    || !ability.isReactionAvailable(context)) {
                continue;
            }
            boolean affordable = hasReaction
                    && availableDeterminationPoints >= ability.getPDCost().minimum();
            options.add(new ReactionOption(ability, cost, ability.getPDCost(),
                    ability.resolveTeleportation(), affordable));
        }
        return options;
    }

    @Override
    public List<GenericReactionOption> getGenericReactions(@NonNull final ReactionContext context) {
        SceneContext reactorContext = context.getReactorContext();
        if (reactorContext == null) {
            return List.of();
        }
        CombatantSheet reactor = context.getReactor();
        boolean hasReaction = reactionsService.getRemainingReactions(
                reactor, context.getTurnNumber(), reactorContext) >= 1
                && !reactor.isActionPrevented(org.aventyrs.core.action.ActionKind.REACTION, reactorContext);
        ReactionTrigger trigger = context.getTrigger();
        List<GenericReactionOption> options = new ArrayList<>();
        if (GenericReaction.DEFENDER_O_PERIMETRO.answers(trigger) && !meleeWeaponsInHand(reactor).isEmpty()) {
            options.add(new GenericReactionOption(GenericReaction.DEFENDER_O_PERIMETRO, null, ActionCost.REACTION,
                    hasReaction));
        }
        if (GenericReaction.IMPEDIR_CONJURACAO.answers(trigger) && context.getAttacker() != null
                && reaches(reactor, reactorContext.getDistanceTo(context.getAttacker()))) {
            options.add(new GenericReactionOption(GenericReaction.IMPEDIR_CONJURACAO, null, ActionCost.REACTION,
                    hasReaction));
        }
        if (GenericReaction.SACRIFICIO.answers(trigger) && context.getThreatenedAlly() != null) {
            java.util.Optional<String> refusal = sacrificeService.refusalToSacrifice(reactor,
                    context.getThreatenedAlly(), reactorContext, context.getTurnNumber());
            if (refusal.isEmpty() || refusal.get().equals(
                    org.aventyrs.core.util.TranslatableMessages.NO_REACTION_AVAILABLE)) {
                options.add(new GenericReactionOption(GenericReaction.SACRIFICIO, null, ActionCost.REACTION,
                        refusal.isEmpty()));
            }
        }
        for (org.aventyrs.core.magic.Spell spell : reactiveSpells(reactor)) {
            options.add(new GenericReactionOption(GenericReaction.CONJURACAO_REATIVA, spell, ActionCost.REACTION,
                    hasReaction));
        }
        if (preparationService.canFire(reactor, reactorContext)) {
            options.add(new GenericReactionOption(GenericReaction.PREPARACAO, null, ActionCost.REACTION, true));
        }
        return List.copyOf(options);
    }

    /** Drawn weapons and Armas Naturais swung as an Ataque Corpo-a-Corpo — what threatens a melee reach. */
    private static List<org.aventyrs.core.item.Weapon> meleeWeaponsInHand(final CombatantSheet sheet) {
        List<org.aventyrs.core.item.Weapon> inHand = new ArrayList<>(sheet.getCharacter().getDrawnWeapons());
        inHand.addAll(sheet.getNaturalWeapons());
        inHand.removeIf(weapon -> weapon.getAttackSkillType() != org.aventyrs.core.skill.SkillType.ATAQUE_CORPO_A_CORPO);
        return inHand;
    }

    /** Whether any melee weapon in reactor's hand reaches distance. */
    private boolean reaches(final CombatantSheet reactor, final org.aventyrs.core.scene.Range distance) {
        return distance != null && meleeWeaponsInHand(reactor).stream().anyMatch(weapon ->
                distance.isWithin(attackRangeService.getEffectiveRange(reactor.getCharacter(), weapon)));
    }

    /** The reactor's learned and mimetized Magias cast as a Reação — "Conforme Magia Conjurada". */
    private static List<org.aventyrs.core.magic.Spell> reactiveSpells(final CombatantSheet reactor) {
        java.util.LinkedHashSet<org.aventyrs.core.magic.Spell> spells =
                new java.util.LinkedHashSet<>(reactor.getCharacter().getCastableSpells());
        reactor.getCharacter().getMimetizedSpells().forEach(mimetized -> spells.add(mimetized.getSpell()));
        spells.removeIf(spell -> spell.getActivationTime() == null
                || spell.getActivationTime().type() != org.aventyrs.core.magic.ActivationType.REACAO);
        return List.copyOf(spells);
    }

    /**
     * Every Habilidade/Suprema across all three Título slots. Uses {@code
     * AventyrTitle#getAllAbilities()} rather than {@code getAbilities()} so an Especialização's own
     * gated Habilidades are included — they are just as activatable as the base catalog's.
     */
    private List<AventyrTitleAbility> heldTitleAbilities(final CombatantSheet reactor) {
        List<AventyrTitleAbility> abilities = new ArrayList<>();
        for (AventyrTitle title : reactor.getCharacter().getAllTitles()) {
            abilities.addAll(title.getAllAbilities());
        }
        return abilities;
    }
}
