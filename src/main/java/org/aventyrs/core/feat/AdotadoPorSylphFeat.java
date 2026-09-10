package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * The acquired, per-character form of {@link FeericoFeat#ADOTADO_POR_SYLPH}, carrying the
 * Perícias the player chose — "uma Perícia para cada Título Aventyr Desperto", from Artes,
 * Atenção e Persuasão. Grant <em>this</em> in {@code Character#feats} in place of the bare enum
 * constant — the same split {@link FocoEmPericiaFeat} keeps against {@code
 * PeritoFeat#FOCO_EM_PERICIA}.
 *
 * <p><b>Both halves are wired.</b> The Vantagem applies to each chosen Perícia; the free
 * Habilidade de Competência from each is carried by {@link #getGrantedSkillTraits} and reaches
 * {@code SkillCompetencyAbility#allFor} like any acquired one.
 *
 * <p><b>Two factories, because the two picks are separate acts.</b> {@link #of(SkillType...)}
 * records the Perícias alone — the Vantagem is owed the moment they are chosen, whether or not
 * the player has yet said <i>which</i> Habilidade de Competência they want from each. {@link
 * #of(SkillCompetencyAbility...)} records the Habilidades and derives the Perícia set from them,
 * which is the finished state. How many Perícias may legally be chosen — one per Título Desperto,
 * a fourth for a Sátiro with an Especialista Título — is not validated here, the same
 * builders-aren't-gatekeepers restraint {@code FeatRequirements} applies everywhere else; a
 * caller passes however many were legally picked.
 */
@Getter
public final class AdotadoPorSylphFeat extends AbstractFeat {

    private final Set<SkillType> chosenSkills;
    private final Set<SkillCompetencyAbility> grantedAbilities;

    public AdotadoPorSylphFeat(@NonNull final Set<SkillType> chosenSkills) {
        this(chosenSkills, Set.of());
    }

    public AdotadoPorSylphFeat(@NonNull final Set<SkillType> chosenSkills,
                               @NonNull final Set<SkillCompetencyAbility> grantedAbilities) {
        super(FeericoFeat.ADOTADO_POR_SYLPH.getFeatCategory(),
                FeericoFeat.ADOTADO_POR_SYLPH.getDescription(),
                FeericoFeat.ADOTADO_POR_SYLPH.getFeatRequirements());
        this.chosenSkills = Set.copyOf(chosenSkills);
        this.grantedAbilities = Set.copyOf(grantedAbilities);
    }

    /** The Perícias alone — the Vantagem half, before the Habilidades have been picked. */
    public static AdotadoPorSylphFeat of(@NonNull final SkillType... chosenSkills) {
        return new AdotadoPorSylphFeat(Set.of(chosenSkills));
    }

    /**
     * The finished pick: one Habilidade de Competência per chosen Perícia, with the Perícia set
     * derived from them, so the Vantagem and the grant can never name different Perícias.
     */
    public static AdotadoPorSylphFeat of(@NonNull final SkillCompetencyAbility... grantedAbilities) {
        return new AdotadoPorSylphFeat(
                java.util.Arrays.stream(grantedAbilities)
                        .map(SkillCompetencyAbility::getSkillType)
                        .collect(Collectors.toUnmodifiableSet()),
                Set.of(grantedAbilities));
    }

    @Override
    public Feat catalogEntry() {
        return FeericoFeat.ADOTADO_POR_SYLPH;
    }

    /** "Você recebe uma Habilidade de Competência de cada Perícia escolhida." */
    @Override
    public List<SkillTrait> getGrantedSkillTraits(final Character character) {
        return List.copyOf(grantedAbilities);
    }

    /** "Vantagem em suas rolagens" of each chosen Perícia — unconditional, no Scene consulted. */
    @Override
    public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                      final SkillTrait requestedAbility, final Character character) {
        return chosenSkills.contains(skillType) ? Skill.ADVANTAGE_BONUS : 0;
    }
}
