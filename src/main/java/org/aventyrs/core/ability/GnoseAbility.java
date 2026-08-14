package org.aventyrs.core.ability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;

@Getter
@AllArgsConstructor
public enum GnoseAbility implements AttributeAbility {

    // TODO: grants a new Especialização for up to 3 known Perícias — typed, held
    // SkillSpecializations now exist (org.aventyrs.core.skill.SkillSpecialization,
    // CharacterSkill#getSpecializations()), but there's still no hook for an ability's own
    // acquisition to grant one onto a chosen Perícia's CharacterSkill; same gap
    // ConhecimentosCompetencyAbility.GENERALISTA's own TODO cites.
    DOMINIO_DO_CONHECIMENTO("Você recebe uma nova Especialização de até 3 Perícias conhecidas."),

    // TODO: grants a new Habilidade de Competência for up to 2 known Perícias, still subject to its
    // prerequisites — no Perícia/Habilidade de Competência system exists yet.
    GENIALIDADE_E_ESFORCO("Você adquire uma nova Habilidade de Competência de até 2 Perícias conhecidas, você " +
            "ainda precisa cumprir com seus pré-requisitos."),

    // TODO: grants +1 permanent Autocontrole and a recovery effect the first time Autocontrole hits zero each
    // session — no Autocontrole/Ego system exists yet.
    ESTABILIDADE_EMOCIONAL("Você adquire permanentemente 1 ponto de Autocontrole, a primeira vez em cada sessão " +
            "de jogo que seu Autocontrole for reduzido a zero você receberá 1 ponto temporário neste Ego na " +
            "Rodada seguinte."),

    // The concrete, grantable form of this ability is org.aventyrs.core.ability
    // .PeritoTeoricoAbility — one constant per SkillType, since which Perícia was chosen is
    // which constant a character holds, not a separately-recorded value. This constant stays
    // the catalog/rules-text entry (mirrors ArtesCompetencyAbility#APRIMORAR_COM_ARTE's own
    // redirect-to-instance-class comment).
    PERITO_TEORICO("Escolha uma Perícia conhecida, você pode substituir o Atributo Base da Perícia escolhida por " +
            "Gnose, esta escolha não pode ser revertida."),

    // TODO: grants training in all untrained Perícias without initial Especializações — no
    // hook for granting Perícia training itself at acquisition time exists yet (same "Race has
    // no hook for granting starting Perícia training" gap CLAUDE.md documents for Elfo's Origem
    // Mística); the "without initial Especializações" half is trivially satisfiable once that
    // exists (CharacterSkill.specializations already defaults to an empty list).
    RATO_DE_BIBLIOTECA("Você recebe treinamento em todas as Perícias que não for treinado, mas as Perícias " +
            "treinadas desta forma não recebem Especializações iniciais.");

    private final String description;

    @Override
    public AttributeDomain getAttributeDomain() {
        return AttributeDomain.GNOSE;
    }
}
