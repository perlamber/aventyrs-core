package org.aventyrs.core.ability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;

@Getter
@AllArgsConstructor
public enum GnoseAbility implements AttributeAbility {

    // TODO: grants a new Especialização for up to 3 known Perícias — no Perícia/Especialização system exists yet.
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

    // TODO: lets you permanently replace a chosen Perícia's base Atributo with Gnose —
    // persisting *which* Perícia was chosen is no longer the blocker (see
    // org.aventyrs.core.ability.AcquiredChoice / Character#getAbilityChoices /
    // org.aventyrs.core.character.services.AbilityChoiceService#getChoiceFor); what's still
    // missing is the actual Perícia base-Attribute substitution mechanism itself — no
    // <Skill>Interaction reads a per-character override for which Attribute feeds its roll.
    PERITO_TEORICO("Escolha uma Perícia conhecida, você pode substituir o Atributo Base da Perícia escolhida por " +
            "Gnose, esta escolha não pode ser revertida."),

    // TODO: grants training in all untrained Perícias without initial Especializações — no
    // Perícia/Especialização system exists yet.
    RATO_DE_BIBLIOTECA("Você recebe treinamento em todas as Perícias que não for treinado, mas as Perícias " +
            "treinadas desta forma não recebem Especializações iniciais.");

    private final String description;

    @Override
    public AttributeDomain getAttributeDomain() {
        return AttributeDomain.GNOSE;
    }
}
