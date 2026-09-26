package org.aventyrs.core.monster;

import lombok.Getter;
import org.aventyrs.core.skill.DifficultyLevel;

/**
 * Regular or Exemplar — "Exemplares, os Monstros Alternativos" in {@code criacao-de-monstros.txt}.
 *
 * <p>A Regular monster is the common one. An Exemplar is unique and named: its Perícias start a
 * step higher, it takes two Talentos more at creation and one more every GP+6, and it may hold
 * everything a Regular is denied ("Monstros não recebem Vantagens de Ego, Habilidades de Atributo,
 * Especializações e Habilidades de Competência de suas Perícias" — "Monstros Exemplares podem").
 * It also has no two-per-Cena cap on Ego effects, and carries Fama.
 *
 * <p><b>Two amounts the text leaves unstated</b> — "recebem Atributos, GD máximo em Perícias …
 * adicionais" gives no number for either. {@link #getBonusMaximumAttribute()} and {@link
 * #getBonusMaximumSkillSteps()} carry a +1 for an Exemplar as a stand-in until the rules name
 * them; change the constants here, nowhere else.
 *
 * <p>Títulos Aventyrs ("despertam Títulos Aventyrs quando se tornam Deviantes e Apex") are not
 * modelled: the Título services take a {@code CharacterSheet}, and a monster isn't one.
 */
@Getter
public enum MonsterKind {

    REGULAR(DifficultyLevel.VERY_EASY, 0, 0, 0, 0, MonsterKind.REGULAR_EGO_EFFECTS_PER_SCENE),
    EXEMPLAR(DifficultyLevel.EASY, 2, 6, 1, 1, Integer.MAX_VALUE);

    /** "Monstros comuns podem utilizar no máximo dois Efeitos de Ego por Cena". */
    public static final int REGULAR_EGO_EFFECTS_PER_SCENE = 2;

    /** The GD every trained Perícia starts at — Muito Fácil for a Regular, Fácil for an Exemplar. */
    private final DifficultyLevel baseSkillLevel;

    /** "iniciam o jogo com 2 Talentos adicionais". */
    private final int bonusStartingFeats;

    /** "Para cada GP+6 recebem um novo Talento, adicional ao padrão"; 0 means no such step. */
    private final int extraFeatPowerDegreeStep;

    /** Stand-in for the unstated "Atributos … adicionais" — added to the Categoria's ceiling. */
    private final int bonusMaximumAttribute;

    /** Stand-in for the unstated "GD máximo em Perícias … adicionais" — steps past the Categoria's ceiling. */
    private final int bonusMaximumSkillSteps;

    /** How many Efeitos de Ego it may use in one Cena. */
    private final int egoEffectsPerScene;

    MonsterKind(final DifficultyLevel baseSkillLevel, final int bonusStartingFeats, final int extraFeatPowerDegreeStep,
                final int bonusMaximumAttribute, final int bonusMaximumSkillSteps, final int egoEffectsPerScene) {
        this.baseSkillLevel = baseSkillLevel;
        this.bonusStartingFeats = bonusStartingFeats;
        this.extraFeatPowerDegreeStep = extraFeatPowerDegreeStep;
        this.bonusMaximumAttribute = bonusMaximumAttribute;
        this.bonusMaximumSkillSteps = bonusMaximumSkillSteps;
        this.egoEffectsPerScene = egoEffectsPerScene;
    }

    /** Whether it may hold Vantagens de Ego, Habilidades de Atributo, Especializações and Habilidades de Competência. */
    public boolean mayHoldCharacterTraits() {
        return this == EXEMPLAR;
    }
}
