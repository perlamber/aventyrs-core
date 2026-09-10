package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;

import java.util.Set;

/**
 * The acquired, per-character form of {@link FeericoFeat#ADOTADO_POR_SYLPH}, carrying the
 * Perícias the player chose — "uma Perícia para cada Título Aventyr Desperto", from Artes,
 * Atenção e Persuasão. Grant <em>this</em> in {@code Character#feats} in place of the bare enum
 * constant — the same split {@link FocoEmPericiaFeat} keeps against {@code
 * PeritoFeat#FOCO_EM_PERICIA}.
 *
 * <p>Only the Vantagem half is wired: the "free Habilidade de Competência per chosen Perícia"
 * half is the acquisition-slot gap (see the gap catalog) and stays unrepresented. How many
 * Perícias may legally be chosen — one per Título Desperto, a fourth for a Sátiro with an
 * Especialista Título — is not validated here, the same builders-aren't-gatekeepers restraint
 * {@code FeatRequirements} applies everywhere else; a caller passes however many were legally
 * picked.
 */
@Getter
public final class AdotadoPorSylphFeat extends AbstractFeat {

    private final Set<SkillType> chosenSkills;

    public AdotadoPorSylphFeat(@NonNull final Set<SkillType> chosenSkills) {
        super(FeericoFeat.ADOTADO_POR_SYLPH.getFeatCategory(),
                FeericoFeat.ADOTADO_POR_SYLPH.getDescription(),
                FeericoFeat.ADOTADO_POR_SYLPH.getFeatRequirements());
        this.chosenSkills = Set.copyOf(chosenSkills);
    }

    public static AdotadoPorSylphFeat of(@NonNull final SkillType... chosenSkills) {
        return new AdotadoPorSylphFeat(Set.of(chosenSkills));
    }

    @Override
    public Feat catalogEntry() {
        return FeericoFeat.ADOTADO_POR_SYLPH;
    }

    /** "Vantagem em suas rolagens" of each chosen Perícia — unconditional, no Scene consulted. */
    @Override
    public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                      final SkillTrait requestedAbility, final Character character) {
        return chosenSkills.contains(skillType) ? Skill.ADVANTAGE_BONUS : 0;
    }
}
