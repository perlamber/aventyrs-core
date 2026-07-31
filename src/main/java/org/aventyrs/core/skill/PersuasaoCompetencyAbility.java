package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;

/**
 * The Habilidades de Competência available to characters trained in Persuasão.
 */
@Getter
@AllArgsConstructor
public enum PersuasaoCompetencyAbility implements SkillCompetencyAbility {

    // TODO: adds half the character's own Força total to this roll, but only for Obter
    // Informações/Intimidação purposes — two separate blockers: (1) the bonus is a
    // per-character variable (half of *that* character's Força), not a fixed constant, and
    // the @Modifier/ModifierResolver mechanism only supports parameterless methods returning
    // a fixed value with no access to the character's own attributes — this would need a
    // new kind of modifier-resolution entirely, not just a missing system; (2) even with
    // that, this codebase doesn't track what a roll is *for*, so scoping to just those two
    // specializations still couldn't be expressed.
    FORCA_INTIMIDADORA("Você pode somar metade de seu valor de Força as rolagens desta " +
            "Perícia para fins de Obter Informações ou Intimidação."),

    // TODO: after succeeding at a Comunicação/Mentir ou Omitir/Intimidação roll, grants
    // Vantagem on similar rolls against other nearby (Distância Curta) characters — needs a
    // roll-resolution-vs-DifficultyLevel engine (to know "success"), a
    // trigger/temporary-buff-after-success mechanism, and a range/proximity system, none of
    // which exist yet.
    ESPALHAR_EMOCOES("Após ser bem-sucedido em rolagens de Comunicação, Mentir ou Omitir " +
            "ou Intimidação, você recebe Vantagem em rolagens semelhantes contra outros " +
            "personagens próximos (Distância Curta)."),

    // TODO: Vantagem scoped to the *target's* gender/attraction toward the character — this
    // depends on a property of the specific other character being rolled against, not
    // something the acting character's own abilities can resolve in isolation (same kind of
    // gap as the NPC-disposition systems ArtesCompetencyAbility.ANIMADOR_DE_TAVERNAS/
    // ESPALHAR_REPUTACAO and EmpatiaSelvagemExcellency.LENDA need); no such
    // cross-character-disposition system exists yet.
    SEDUTOR("Vantagens em rolagens desta Perícia roladas contra personagens do sexo " +
            "oposto, ou contra quaisquer personagens que possam se sentir atraídos por " +
            "você."),

    // Note: scoped to Negociação/Mentir ou Omitir purposes specifically, but this codebase
    // doesn't track what a roll is *for* (same simplification as
    // AttentionCompetencyAbility.PERCEPCAO_DE_FOXM), so it's implemented as an unconditional
    // flat bonus to every Persuasão roll rather than silently narrowed.
    POKERFACE("Vantagem em rolagens para fins de Negociação e Mentir ou Omitir.") {
        @Modifier(ModifierType.SKILL_ROLL_BONUS)
        public int advantageBonus() {
            return Skill.ADVANTAGE_BONUS;
        }
    },

    // TODO: on success, the next Ataque Perícia roll this Rodada gets Vantagem and costs
    // -1PA — needs a roll-resolution-vs-DifficultyLevel engine, a cross-skill
    // (Persuasão-affecting-a-later-Ataque-roll) temporary-buff mechanism, and Rodada-scoped
    // duration tracking, none of which exist yet.
    FINTAR_APRIMORADO("Você pode fazer rolagens para enganar seus oponentes, se for " +
            "bem-sucedido sua próxima rolagens de Perícia de Ataque nesta Rodada recebe " +
            "Vantagem e tem seu Tempo de Ação reduzido em -1PA.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.PERSUASAO;
    }
}
