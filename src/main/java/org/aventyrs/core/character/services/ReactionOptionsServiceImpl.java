package org.aventyrs.core.character.services;

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
        boolean hasReaction = reactionsService.getTotalReactions(
                reactor, context.getTurnNumber(), context.getReactorContext()) >= 1;
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
