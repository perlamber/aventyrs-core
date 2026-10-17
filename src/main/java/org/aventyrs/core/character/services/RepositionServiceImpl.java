package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.MobilidadeFeat;
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
        return getBaseDistance(sheet) + Math.max(0, bonus);
    }

    @Override
    public boolean canMoveAcrobatically(@NonNull final CombatantSheet sheet, final int turnNumber,
                                        final SceneContext sceneContext) {
        return acrobaticRefusalFor(sheet, turnNumber, sceneContext) == null;
    }

    @Override
    public int getAcrobaticDistance(@NonNull final CombatantSheet sheet) {
        return sheet.getAttributeTotal(org.aventyrs.core.character.AttributeDomain.DEXTERITY) / 2
                + ACROBATIC_UD_PER_TITLE * sheet.getCharacter().getAllTitles().size();
    }

    @Override
    public int beginAcrobatic(@NonNull final CombatantSheet sheet, final int turnNumber,
                              final SceneContext sceneContext) {
        String refusal = acrobaticRefusalFor(sheet, turnNumber, sceneContext);
        if (refusal != null) {
            throw new IllegalOperationException(refusal);
        }
        int distance = getAcrobaticDistance(sheet);
        sheet.consumeRepositionThisRound();
        sheet.recordAbilityActivation(MobilidadeFeat.MOVIMENTO_ACROBATICO);
        return distance;
    }

    /** "A distância máxima aumenta em +2UD para cada Título Aventyr que você possuir". */
    private static final int ACROBATIC_UD_PER_TITLE = 2;

    private String acrobaticRefusalFor(final CombatantSheet sheet, final int turnNumber,
                                       final SceneContext sceneContext) {
        if (!sheet.getCharacter().getFeats().contains(MobilidadeFeat.MOVIMENTO_ACROBATICO)) {
            return org.aventyrs.core.util.TranslatableMessages.ACROBATIC_MOVEMENT_NOT_HELD;
        }
        String refusal = refusalFor(sheet, turnNumber, sceneContext);
        if (refusal != null) {
            return refusal;
        }
        // "Apenas uma vez por Rodada" — one Turn per Rodada, so the per-Turn activation count answers it.
        return sheet.countActivationsThisTurn(MobilidadeFeat.MOVIMENTO_ACROBATICO) > 0
                ? org.aventyrs.core.util.TranslatableMessages.ACROBATIC_MOVEMENT_ALREADY_USED : null;
    }

    /**
     * "Move-se … igual à metade da Categoria de Tamanho do Personagem (mínimo 1UD)" (acoes-de-combate.txt 20261013,
     * core 0.1.6.18) — half the effective Categoria, rounded down, never below {@link #DISTANCE_UD}.
     */
    static int getBaseDistance(final CombatantSheet sheet) {
        return Math.max(DISTANCE_UD,
                new CharacterSizeServiceImpl().getEffectiveSizeCategory(sheet).getCategory() / 2);
    }

    private String refusalFor(final CombatantSheet sheet, final int turnNumber, final SceneContext sceneContext) {
        if (sheet.isMovementPrevented(sceneContext)) {
            return REPOSITION_MOVEMENT_PREVENTED;
        }
        // An Ação Livre — Confuso "não pode realizar Ações Livres".
        if (sheet.isActionPrevented(org.aventyrs.core.action.ActionKind.FREE_ACTION, sceneContext)) {
            return org.aventyrs.core.util.TranslatableMessages.ACTION_PREVENTED_BY_CONDITION;
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
