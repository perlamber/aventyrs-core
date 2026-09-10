package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.TerrainType;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.conhecimentos.ConhecimentosSpecialization;

import java.util.Optional;

/**
 * The acquired, per-character form of {@link SobrevivenciaFeat#TERRENO_PREDILETO}, carrying the
 * {@link TerrainType} the player chose. Grant <em>this</em> in {@code Character#feats} in place
 * of the bare enum constant — the same split {@link FocoEmPericiaFeat} keeps against {@code
 * PeritoFeat#FOCO_EM_PERICIA}.
 *
 * <p>The six terrenos the rules name (Aquáticos, Cidades, Desertos, Florestas, Montanhas,
 * Subterrâneo) are exactly {@link TerrainType}'s six constants, so no new enum is needed.
 * "no terreno escolhido" is read off the {@link SceneContext} both hooks receive — a Scene's
 * terrain is one already-resolved fact, the same way {@code ElficoFeat#GUARDIAO_DOS_BOSQUES}
 * reads it.
 */
@Getter
public final class TerrenoPrediletoFeat extends AbstractFeat {

    /** TERRENO_PREDILETO's own stated "+2" to both Defesas while in the chosen terrain. */
    private static final int DEFENSE_BONUS = 2;

    private final TerrainType chosenTerrain;

    public TerrenoPrediletoFeat(@NonNull final TerrainType chosenTerrain) {
        super(SobrevivenciaFeat.TERRENO_PREDILETO.getFeatCategory(),
                SobrevivenciaFeat.TERRENO_PREDILETO.getDescription(),
                SobrevivenciaFeat.TERRENO_PREDILETO.getFeatRequirements());
        this.chosenTerrain = chosenTerrain;
    }

    public static TerrenoPrediletoFeat of(@NonNull final TerrainType chosenTerrain) {
        return new TerrenoPrediletoFeat(chosenTerrain);
    }

    /**
     * The Terreno Predileto a character chose, if they hold this Talento — what
     * {@code SobrevivenciaFeat}'s dependent constants (Mestre de Caça, Protetor Territorialista)
     * read. Mirrors {@link FocoEmPericiaFeat#chosenBy}.
     */
    public static Optional<TerrainType> chosenBy(final Character character) {
        return character.getFeats().stream()
                .filter(TerrenoPrediletoFeat.class::isInstance)
                .map(TerrenoPrediletoFeat.class::cast)
                .map(TerrenoPrediletoFeat::getChosenTerrain)
                .findFirst();
    }

    @Override
    public Feat catalogEntry() {
        return SobrevivenciaFeat.TERRENO_PREDILETO;
    }

    /** "Vantagem em rolagens de Furtividade e de Conhecimentos: Natureza no terreno escolhido." */
    @Override
    public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                      final SkillTrait requestedAbility, final Character character) {
        if (sceneContext == null || !sceneContext.isTerrain(chosenTerrain)) {
            return 0;
        }
        if (skillType == SkillType.FURTIVIDADE) {
            return Skill.ADVANTAGE_BONUS;
        }
        boolean naturezaRequested = skillType == SkillType.CONHECIMENTOS
                && requestedAbility == ConhecimentosSpecialization.NATUREZA;
        return naturezaRequested ? Skill.ADVANTAGE_BONUS : 0;
    }

    /** "+2 em suas Defesas ... no terreno escolhido" — DF and DM alike ("suas Defesas"). */
    @Override
    public int resolveDefenseBonus(final DefenseType defenseType, final Character character,
                                    final SceneContext sceneContext) {
        return sceneContext != null && sceneContext.isTerrain(chosenTerrain) ? DEFENSE_BONUS : 0;
    }
}
