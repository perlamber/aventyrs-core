package org.aventyrs.core.ability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;

@Getter
@AllArgsConstructor
public enum FocusAbility implements AttributeAbility {

    // TODO: activated ability (1PA + 3PM, 2-Round duration) granting half Foco on skill rolls while active.
    CONCENTRACAO_PROFUNDA("Você pode gastar 1PA e 3PM para entrar em estado de Concentração Profunda por 2 " +
            "Rodadas. Enquanto Concentração Profunda estiver ativa você pode adicionar metade do seu valor de " +
            "Foco às suas rolagens de Perícias."),

    CONEXAO_COM_O_MANA("Seu multiplicador de Pontos de Magia é aumentado em +1.") {
        @Modifier(ModifierType.MANA_MULTIPLIER)
        public int manaMultiplierBonus() {
            return 1;
        }
    },

    // TODO: rest/recovery bonus (+2PM on Long or better Rests, +1PM on magic/title healing, +1 non-cumulative Roubo de Mana).
    CANALIZADOR_DE_MANA("Descansos Verdadeiros Longos ou superiores permitem que você recupere +2PM adicionais. " +
            "Magias e Habilidades de Títulos Aventyr que te façam recuperar PV recuperam +1PM adicional (não " +
            "aplicável a efeitos de Roubo de Mana). Caso possua um ou mais efeitos de Roubo de Mana, seu Roubo " +
            "de Mana total aumenta em +1 (não cumulativo)."),

    // TODO: conditional effect — first spell cast each Round uses full Foco instead of half.
    MAGIA_PODEROSA("Algumas de suas magias são mais poderosas que o normal. A cada Rodada, quando conjurar sua " +
            "primeira magia, você pode adicionar seu valor integral de Foco, ao invés da metade, aos seus " +
            "efeitos."),

    // TODO: grants access to a chosen Magic Type's second branch — depends on a Magic/Spell Tree system that doesn't exist yet.
    MAGIA_ALTERNATIVA("Você pode adquirir magias de ramos adicionais de algumas de suas Árvores de Magia. " +
            "Escolha um Tipo de Magia entre Divina, Elemental, Encantamento, Invocação, Temporal, Primordial, " +
            "Profana ou Umbral: você pode aprender magias de ambas as ramificações dos tipos de magia escolhidos.");

    private final String description;

    @Override
    public AttributeDomain getAttributeDomain() {
        return AttributeDomain.FOCUS;
    }
}
