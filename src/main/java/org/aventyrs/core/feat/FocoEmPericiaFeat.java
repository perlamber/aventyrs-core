package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * The acquirable, per-character form of {@link PeritoFeat#FOCO_EM_PERICIA}, carrying the Perícia
 * the player chose when acquiring it. Grant <em>this</em> in {@code Character#feats} instead of
 * the bare enum constant (which stays the catalog/rules-text entry) — the same split {@code
 * org.aventyrs.core.skill.artes.ArtesAprimorarComArteAbility} keeps against {@code
 * ArtesCompetencyAbility#APRIMORAR_COM_ARTE}.
 *
 * <p>Because {@code AbstractSkillInteraction#sumFeatRollBonuses} scans {@code
 * character.getFeats()} calling {@link Feat#resolveSkillRollBonus} on each held Talento, storing
 * the choice on this instance makes the Vantagem Perícia-scoped with no change to any service —
 * the {@code Feat} roll-bonus hook already takes the {@link SkillType} being rolled.
 *
 * <p>The whole rest of the Perito tree conditions on "a Perícia escolhida"; those constants stay
 * plain enum constants and read the choice via {@link #chosenBy(Character)}.
 */
@Getter
public final class FocoEmPericiaFeat extends AbstractFeat {

    private final SkillType chosenSkill;

    public FocoEmPericiaFeat(@NonNull final SkillType chosenSkill) {
        super(PeritoFeat.FOCO_EM_PERICIA.getFeatCategory(),
                PeritoFeat.FOCO_EM_PERICIA.getDescription(),
                PeritoFeat.FOCO_EM_PERICIA.getFeatRequirements());
        this.chosenSkill = chosenSkill;
    }

    public static FocoEmPericiaFeat of(@NonNull final SkillType chosenSkill) {
        return new FocoEmPericiaFeat(chosenSkill);
    }

    /**
     * The Perícia a character has Foco in, if they hold this Talento — what every "da Perícia
     * escolhida" clause in {@link PeritoFeat} reads. Mirrors {@code
     * org.aventyrs.core.ability.PeritoTeoricoAbility#resolveAttributeDomain}'s
     * scan-the-held-list shape.
     */
    public static Optional<SkillType> chosenBy(final Character character) {
        return character.getFeats().stream()
                .filter(FocoEmPericiaFeat.class::isInstance)
                .map(FocoEmPericiaFeat.class::cast)
                .map(FocoEmPericiaFeat::getChosenSkill)
                .findFirst();
    }

    @Override
    public Feat catalogEntry() {
        return PeritoFeat.FOCO_EM_PERICIA;
    }

    /**
     * "adquira vantagem nas rolagens da Perícia escolhida" — a flat {@link Skill#ADVANTAGE_BONUS}
     * on the chosen Perícia, and nothing else. The choice was frozen at acquisition, so neither
     * {@code sceneContext} nor the holder's live state is consulted.
     */
    @Override
    public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                      final SkillTrait requestedAbility, final Character character) {
        return skillType == chosenSkill ? Skill.ADVANTAGE_BONUS : 0;
    }
}
