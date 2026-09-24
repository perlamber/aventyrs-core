package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.REPOSITION_AFTER_MOVEMENT;
import static org.aventyrs.core.util.TranslatableMessages.REPOSITION_IN_DIFFICULT_TERRAIN;
import static org.aventyrs.core.util.TranslatableMessages.REPOSITION_MOVEMENT_PREVENTED;
import static org.aventyrs.core.util.TranslatableMessages.REPOSITION_REQUIRES_FREE_ACTION;

public class RepositionServiceImpl implements RepositionService {

    private final FreeActionsService freeActionsService;
    private final ModifierResolver modifierResolver;

    public RepositionServiceImpl() {
        this(new FreeActionsServiceImpl());
    }

    public RepositionServiceImpl(final FreeActionsService freeActionsService) {
        this(freeActionsService, new ModifierResolverImpl());
    }

    public RepositionServiceImpl(final FreeActionsService freeActionsService,
                                 final ModifierResolver modifierResolver) {
        this.freeActionsService = freeActionsService;
        this.modifierResolver = modifierResolver;
    }

    @Override
    public boolean canReposition(@NonNull final CombatantSheet sheet, final int turnNumber,
                                 final SceneContext sceneContext) {
        return refusalFor(sheet, turnNumber, sceneContext) == null;
    }

    @Override
    public int begin(@NonNull final CombatantSheet sheet, final int turnNumber, final SceneContext sceneContext) {
        String refusal = refusalFor(sheet, turnNumber, sceneContext);
        if (refusal != null) {
            throw new IllegalOperationException(refusal);
        }
        int distance = getDistance(sheet, turnNumber);
        sheet.consumeRepositionThisRound();
        return distance;
    }

    @Override
    public int getDistance(@NonNull final CombatantSheet sheet, final int currentRound) {
        Character character = sheet.getCharacter();
        int repositionIndex = sheet.getRepositionsTakenThisRound();
        int bonus = sheet.getTemporaryBonus(ModifierType.REPOSITION_DISTANCE);
        bonus += modifierResolver.sumModifiers(character.getAttributeAbilities(), ModifierType.REPOSITION_DISTANCE);
        bonus += modifierResolver.sumModifiers(SkillCompetencyAbility.allFor(character, sheet),
                ModifierType.REPOSITION_DISTANCE);
        for (Feat feat : character.getFeats()) {
            bonus += feat.resolveRepositionDistanceIncrease(currentRound, repositionIndex, character);
        }
        for (Item item : character.getEquipment()) {
            bonus += item.resolveFavorBonus(ModifierType.REPOSITION_DISTANCE, sheet);
            bonus += item.resolveEnhancementBonus(ModifierType.REPOSITION_DISTANCE, null, character);
        }
        return DISTANCE_UD + Math.max(0, bonus);
    }

    private String refusalFor(final CombatantSheet sheet, final int turnNumber, final SceneContext sceneContext) {
        if (sheet.isMovementPrevented(sceneContext)) {
            return REPOSITION_MOVEMENT_PREVENTED;
        }
        // Spending Pontos de Ação moving and Reposicionar exclude each other (table ruling).
        if (sheet.getMovementsTakenThisRound() > 0) {
            return REPOSITION_AFTER_MOVEMENT;
        }
        // "Entitled to any at all" — no ledger counts Ações Livres spent, as for every other one.
        if (freeActionsService.getTotalFreeActions(sheet, turnNumber, sceneContext) <= 0) {
            return REPOSITION_REQUIRES_FREE_ACTION;
        }
        if (sceneContext != null && sceneContext.getEnvironmentalState().inDifficultTerrain()) {
            return REPOSITION_IN_DIFFICULT_TERRAIN;
        }
        return null;
    }
}
