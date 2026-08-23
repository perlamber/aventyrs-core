package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.action.ActionPointsService;
import org.aventyrs.core.action.ActionPointsServiceImpl;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.ACTIVE_ABILITY_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_ACTION_POINTS;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_MAGIC_POINTS;

public class ActiveAbilityServiceImpl implements ActiveAbilityService {

    private final ActionPointsService actionPointsService;
    private final MagicPointsService magicPointsService;

    public ActiveAbilityServiceImpl() {
        this(new ActionPointsServiceImpl(), new MagicPointsServiceImpl());
    }

    public ActiveAbilityServiceImpl(final ActionPointsService actionPointsService, final MagicPointsService magicPointsService) {
        this.actionPointsService = actionPointsService;
        this.magicPointsService = magicPointsService;
    }

    @Override
    public void activate(final Character character, final CombatantSheet characterSheet, final ActiveAbility ability, final int turnNumber) throws IllegalOperationException {
        if (character.getActiveAbilities().stream().noneMatch(held -> held == ability)) {
            throw new IllegalOperationException(ACTIVE_ABILITY_NOT_HELD);
        }
        if (actionPointsService.getMaxActionPoints(character, turnNumber) < ability.getActionPointCost()) {
            throw new IllegalOperationException(NOT_ENOUGH_ACTION_POINTS);
        }
        if (magicPointsService.getCurrentMagicPoints(character, characterSheet) < ability.getMagicPointCost()) {
            throw new IllegalOperationException(NOT_ENOUGH_MAGIC_POINTS);
        }

        characterSheet.spendMagicPoints(ability.getMagicPointCost());
        characterSheet.applyEffect(ability.resolveEffect(character));
    }
}
