package org.aventyrs.core.character.services;

import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.util.TranslatableMessages;

import java.util.OptionalInt;

public class PreInitiativeServiceImpl implements PreInitiativeService {

    /** The once-per-combat mark — one Rodada 0.0 action, whichever Habilidade paid for it. */
    private static final Object USED = PreInitiativeService.class;

    private final DeterminationPointsService determinationPointsService;

    public PreInitiativeServiceImpl() {
        this(new DeterminationPointsServiceImpl());
    }

    public PreInitiativeServiceImpl(final DeterminationPointsService determinationPointsService) {
        this.determinationPointsService = determinationPointsService;
    }

    @Override
    public int getCost(final CombatantSheet sheet) {
        if (sheet == null || sheet.getCharacter() == null) {
            return 0;
        }
        return sheet.getCharacter().getAttributeAbilities().stream()
                .map(AttributeAbility::resolvePreInitiativeActionCost)
                .filter(OptionalInt::isPresent)
                .mapToInt(OptionalInt::getAsInt)
                .min()
                .orElse(0);
    }

    @Override
    public boolean canTake(final CombatantSheet sheet, final Scene scene) {
        int cost = getCost(sheet);
        return cost > 0
                && (scene == null || scene.isAtCombatStart())
                && !sheet.isAffectedThisCombat(USED)
                && determinationPointsService.getCurrentDeterminationPoints(sheet.getCharacter(), sheet) >= cost;
    }

    @Override
    public int take(final CombatantSheet sheet, final Scene scene) {
        if (!canTake(sheet, scene)) {
            throw new IllegalOperationException(TranslatableMessages.PRE_INITIATIVE_ACTION_UNAVAILABLE);
        }
        int cost = getCost(sheet);
        sheet.spendDeterminationPoints(cost);
        sheet.markAffectedThisCombat(USED);
        return cost;
    }
}
