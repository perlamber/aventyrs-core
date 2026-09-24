package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.scene.EnvironmentalState;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.grid.GridPosition;
import org.aventyrs.core.scene.grid.MovementMap;
import org.aventyrs.core.scene.grid.StepRules;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.title.AventyrTitle;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class MovementTerrainServiceImpl implements MovementTerrainService {

    @Override
    public boolean ignoresDifficultTerrain(@NonNull final CombatantSheet sheet, final int movementIndex,
                                           final EnvironmentalState environmentalState) {
        Character character = sheet.getCharacter();
        EnvironmentalState environment = environmentalState == null ? EnvironmentalState.ORDINARY : environmentalState;
        if (character.getAttributeAbilities().stream().anyMatch(ability -> ability.ignoresDifficultTerrain(movementIndex))) {
            return true;
        }
        if (character.getFeats().stream().anyMatch(feat -> feat.ignoresDifficultTerrain(character, sheet))) {
            return true;
        }
        if (character.getEquipment().stream().anyMatch(item -> item.ignoresDifficultTerrain())) {
            return true;
        }
        // Sempre Veloz: "enquanto em terra" — not in the air, not half under water.
        return character.getRace() != null && character.getRace().ignoresDifficultTerrainOnLand()
                && !sheet.getRacialTraitSuppression().suppressesInnateTraits()
                && !environment.flying() && !environment.atLeastHalfSubmerged();
    }

    @Override
    public StepRules stepRules(@NonNull final CombatantSheet mover, @NonNull final MovementMap map,
                               final SceneContext moverContext, final int movementIndex) {
        Set<UUID> enemies = moverContext == null ? Set.of() : moverContext.getEnemies().stream()
                .map(CombatantSheet::getId)
                .collect(Collectors.toSet());
        List<AventyrTitle> titles = mover.getCharacter().getAllTitles();
        boolean passesEnemies = titles.stream().anyMatch(AventyrTitle::passesThroughEnemySpaces);
        boolean ignoring = ignoresDifficultTerrain(mover, movementIndex,
                moverContext == null ? null : moverContext.getEnvironmentalState());
        return new StepRules() {
            @Override
            public boolean canPass(final GridPosition hex) {
                // A defeated foe blocks nobody: its space is Terreno Difícil instead (isDifficult).
                return map.occupantsOf(hex, mover).stream()
                        .allMatch(occupant -> !enemies.contains(occupant.getId()) || passesEnemies
                                || map.isDefeated(occupant));
            }

            @Override
            public boolean canStop(final GridPosition hex) {
                return map.occupantsOf(hex, mover).stream()
                        .allMatch(occupant -> titles.stream().anyMatch(title ->
                                title.mayShareSpaceWith(mover, occupant, enemies.contains(occupant.getId()))));
            }

            @Override
            public boolean isDifficult(final GridPosition hex) {
                return map.isDifficultTerrain(hex) || map.occupantsOf(hex, mover).stream()
                        .filter(occupant -> enemies.contains(occupant.getId()))
                        .anyMatch(occupant -> passesEnemies || map.isDefeated(occupant));
            }

            @Override
            public int enterCost(final GridPosition hex) {
                return isDifficult(hex) && !ignoring ? DIFFICULT_TERRAIN_COST : 1;
            }
        };
    }
}
