package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The Habilidades de Competência available to characters trained in Persuasão.
 */
@Getter
@AllArgsConstructor
public enum PersuasaoCompetencyAbility implements SkillCompetencyAbility {

    // TODO: lets this Perícia use Força instead of its normal base Attribute (Carisma),
    // unconditionally — no Perícia base-Attribute substitution mechanism exists yet (same
    // gap as EmpatiaSelvagemCompetencyAbility.ACADEMICO_SELVAGEM/INSTINTO_ANIMAL /
    // FurtividadeCompetencyAbility.LADINO_TEORICO).
    FORCA_OPRESSORA("Você pode substituir o Atributo Base desta Perícia por Força."),

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
    // gap as the NPC-disposition systems ArtesCompetencyAbility.ESPALHAR_REPUTACAO and
    // EmpatiaSelvagemExcellency.LENDA need); no such cross-character-disposition system
    // exists yet.
    SEDUTOR("Vantagens em rolagens desta Perícia roladas contra personagens do sexo " +
            "oposto, ou contra quaisquer personagens que possam se sentir atraídos por " +
            "você."),

    // TODO: -1PE to Equipamento purchase/production costs — no Item/Equipamento entity,
    // "PE" currency, or cost system exists yet (same gap as
    // ProfissaoExcellency.FOCADO/LENDA).
    CAMBALACHO("O custo de compra e produção de Equipamentos é reduzido em -1PE."),

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
