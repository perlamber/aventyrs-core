package org.aventyrs.core.ability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;

@Getter
@AllArgsConstructor
public enum InstinctAbility implements AttributeAbility {

    // TODO: conditional roll effect depending on having an Awakened Título Aventyr — no Título system exists yet.
    SENTIR_A_INTENCAO("Você recebe Vantagem em suas rolagens de Perícias baseadas em Instinto, se tiver ao menos " +
            "1 Título Aventyr Desperto; ao invés disso, você reduz a GD da rolagem em -1 nível."),

    // TODO: activated ability (2PD cost, once per Scene) letting you act before Initiative at scene start.
    PARANOIA_SAUDAVEL("Ao custo de 2PD e apenas uma vez a cada Cena, você pode optar por fazer uma ação com " +
            "Tempo de até 2PA, Ação Livre ou Reação no início da cena. Esta Vantagem deve ser ativada no início " +
            "da Cena, antes da ação do primeiro personagem, ignorando as Iniciativas."),

    OBSTINADO("Seu multiplicador de PD aumenta em +1.") {
        @Modifier(ModifierType.DETERMINATION_MULTIPLIER)
        public int determinationMultiplierBonus() {
            return 1;
        }
    },

    // TODO: recovery bonus (+1PD on any PD-recovering effect, +2PD additional on Long or better Rests).
    SUPERMOTIVADO("Efeitos que te permitam recuperar PD o fazem recuperar 1PD adicional. Descansos longos ou " +
            "superiores adicionalmente te permitem recuperar 2PD adicionais (totalizando 3PD adicionais)."),

    // TODO: XP-cost unlock (5 EXP) granting an extra Suprema on one Título Aventyr — no Título/Suprema system exists yet.
    CENTELHA_SUPERIOR("Você pode adquirir uma Suprema adicional, mas apenas de um de seus Títulos Aventyr, ao " +
            "custo de 5 EXP.");

    private final String description;

    @Override
    public AttributeDomain getAttributeDomain() {
        return AttributeDomain.INSTINCT;
    }
}
