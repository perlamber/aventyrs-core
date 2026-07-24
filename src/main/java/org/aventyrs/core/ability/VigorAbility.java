package org.aventyrs.core.ability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;

@Getter
@AllArgsConstructor
public enum VigorAbility implements AttributeAbility {

    SOBRE_HUMANO("Seu Multiplicador de Pontos de Vida é aumentado em +1."),

    SANGUE_DE_GIGANTE("Sua Categoria de Tamanho é aumentada em +1."),

    METABOLISMO_RAPIDO("Descansos Verdadeiros Longos ou superiores permitem que você recupere +3PV adicionais. " +
            "Magias e Habilidades de Títulos Aventyr que te façam recuperar PV recuperam +2PV adicionais (não " +
            "aplicável a efeitos de Roubo de Vida). Personagens sob efeitos de Roubo de Vida têm seu efeito de " +
            "Roubo de Vida aumentado em +1 (não cumulativo)."),

    RIGIDEZ_DA_MONTANHA("Todo o Dano Físico causado a você é reduzido em -1; se o dano for causado por " +
            "personagens de Categorias de Tamanho inferiores à sua, ao invés disso o dano é reduzido em -2."),

    RECUPERACAO_ASSOMBROSA("Os Efeitos Críticos, Correntes de Efeitos, e Efeitos de Magias e de Habilidades " +
            "nocivas que possuam Duração maior do que 3 Rodadas são encerradas em 3 Rodadas. Esta Habilidade não " +
            "afeta efeitos permanentes.");

    private final String description;

    @Override
    public AttributeDomain getAttributeDomain() {
        return AttributeDomain.VIGOR;
    }
}
