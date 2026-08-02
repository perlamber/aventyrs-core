package org.aventyrs.core.ability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;

@Getter
@AllArgsConstructor
public enum CharismaAbility implements AttributeAbility {

    AGRESSAO_ANUNCIADA("Você adquire Vantagem em rolagens de Perícias baseadas em Força ou Destreza sempre que " +
            "as fizer imediatamente após realizar uma rolagem de Perícia baseada em Carisma."),

    // TODO: grants an Especialização and a Habilidade de Competência for every trained Carisma-based Perícia at
    // the moment of acquisition — no Perícia/Especialização/Habilidade de Competência system exists yet.
    CHARME("Você adquire uma Especialização e uma Habilidade de Competência de cada Perícia baseada em Carisma " +
            "em que for treinado no momento em que adquirir esta Habilidade."),

    // TODO: grants a permanent Sorte point, plus a non-cumulative temporary Sorte and Autocontrole point on any
    // Sucesso Crítico Maior skill roll — no Sorte/Autocontrole/Critical Success tier system exists yet.
    DESTINO_FAVORAVEL("Você adquire um ponto de Sorte permanentemente; sempre que tiver um Sucesso Crítico Maior " +
            "em uma rolagem de Perícia você adquire um ponto temporário, não cumulativo, em Sorte e em " +
            "Autocontrole."),

    VOZ_DE_OURO("Você pode gastar 2PD para reduzir em -1PA o Tempo de qualquer Ação que exija rolagens de " +
            "Perícias baseadas em Carisma."),

    FLOREIO_ARCANO("Você adquire Vantagem em rolagens de Perícias baseadas em Carisma sempre que as fizer " +
            "imediatamente após Conjurar uma Magia.");

    private final String description;

    @Override
    public AttributeDomain getAttributeDomain() {
        return AttributeDomain.CHARISMA;
    }
}
