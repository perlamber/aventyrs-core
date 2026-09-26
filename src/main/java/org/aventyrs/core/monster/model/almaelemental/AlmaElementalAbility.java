package org.aventyrs.core.monster.model.almaelemental;

import lombok.Getter;
import org.aventyrs.core.monster.MonsterCategory;
import org.aventyrs.core.monster.model.MonsterModel;
import org.aventyrs.core.monster.model.MonstrousAbility;

/**
 * The Habilidades Monstruosas of the Modelo Alma Elemental — {@code criacao-de-monstros.txt}.
 *
 * <p><b>Catalog entries only</b>: name, tier and rules text. None of their effects is applied by
 * this core yet ({@link MonstrousAbility#isImplemented()} is {@code false}); a monster may hold
 * them, and they count toward its Habilidade budget and its +2PV per Habilidade, but their passives
 * and actives are run at the table by hand. Abençoado de Cireneia
 * ({@code org.aventyrs.core.monster.model.cireneia.CireneiaAbility}) is the worked example to
 * follow when this Modelo is built.
 */
@Getter
public enum AlmaElementalAbility implements MonstrousAbility {

    SANGUE_ELEMENTAL("Sangue Elemental", MonsterCategory.PRESA,
            "Efeito Passivo – Anatomia Elemental.\n"
                + "“Anatomia Elemental – Escolha um Elemento, este Monstro recebe RE, Resistência a Críticos, Bônus Racial de +2 em Instinto”\n"
                + "Aprimoramentos dos Deviantes\n"
                + "• Danos do Elemento escolhido são reduzidos à metade (efeito de Meio-Dano).\n"
                + "Aprimoramentos dos Predadores\n"
                + "• Bônus Racial de Instinto +2\n"
                + "Aprimoramentos dos Apex\n"
                + "• Imunidade ao Elemento escolhido."),

    ARMAMENTO_ELEMENTAL("Armamento Elemental", MonsterCategory.PRESA,
            "Efeito Passivo – Escolha uma Arma Natural, este Monstro possui a Arma Natural escolhida.\n"
                + "Efeito Ativo – Fúria Elemental [1PA, 2PD]: Escolha um Elemento, durante 3 Rodadas a Arma Natural causa dano Físico Elemental e recebe Cataclismo como Efeito Crítico adicional. Resfriamento 3.\n"
                + "Aprimoramentos dos Deviantes\n"
                + "• Rolagens de Ataques e Danos com a Arma Natural recebem Bônus de +2.\n"
                + "Aprimoramentos dos Predadores\n"
                + "• Fúria Elemental – ataques infligem Danos Mágicos e recebem a Corrente de Efeitos Monstruosa – Marca Elemental.\n"
                + "“Marca Elemental – Alvo sofre 3 pontos de Dano Mágico Elemental por 2+Vigor do Alvo Rodadas (Efeito de Maldição)”\n"
                + "Aprimoramentos dos Apex\n"
                + "• Ataques da Arma natural tem Margem Crítica Menor aumentada em +3, também reduzem a Resistência a Correntes de Efeitos do alvo em -2.\n"
                + "• Fúria Elemental – Duração aumentada em +2 Rodadas, Resfriamento reduzido para 2 Rodadas."),

    SANGUE_ELEMENTAL_DEVIANTE("Sangue Elemental", MonsterCategory.DEVIANTE,
            "Efeito Passivo: Escolha um Elemento; Retribuição Elemental.\n"
                + "“Retribuição Elemental - Atacantes corpo-a-corpo que infligirem danos ao Elemental sofrem 3 pontos de Dano Elemental.”\n"
                + "Efeito Ativo – Aura Elemental: Cria uma Aura Elemental ao redor do Monstro com Área de Efeito Circular Curta. Outros personagens na área sofrem 1d6 pontos de Dano Físico Elemental. Esta Habilidade só pode ser ativada se o Monstro estiver ferido e tem Duração de 2 Rodadas. Resfriamento 2.\n"
                + "Aprimoramentos dos Predadores\n"
                + "• Retribuição Elemental – Dano muda para 1d6 e se torna Mágico Elemental.\n"
                + "• Explosão Pós-Mortis.\n"
                + "“Explosão Pós Mortis – Este Monstro explode quando morre e não pode ser revivido. A explosão tem Área de Efeito Circular Média e inflige 2d6+Instinto pontos de Dano Mágico Elemental.”\n"
                + "Aprimoramentos dos Apex\n"
                + "• Aura Elemental – Alcance muda para Área de Efeito Circular Média e passa a infligir 2d6 pontos de Dano Mágico Elemental."),

    CONJURACAO_ELEMENTAL("Conjuração Elemental", MonsterCategory.DEVIANTE,
            "Efeito Passivo: Foco +2.\n"
                + "Efeito Ativo – Conjuração Elemental: Aprende 2 Árvores de Magia Elemental é capaz de conjurar Sementes, Brotos e Mudas destas Árvores.\n"
                + "Aprimoramentos dos Predadores\n"
                + "• Conjuração Elemental – Aprende uma Árvore de Magia adicional Elemental, Divina ou Profana. É se torna capaz de conjurar Magias Emergentes.\n"
                + "Aprimoramentos dos Apex\n"
                + "• Conjuração Elemental – Aprende uma Árvore de Magia adicional Elemental, Divina ou Profana. É se torna capaz de conjurar Magias Florescentes."),

    SOPRO_ELEMENTAL("Sopro Elemental", MonsterCategory.PREDADOR,
            "Efeito Passivo - Adquire a Arma de Sopro.\n"
                + "Efeito Ativo – Sopro Cataclísmico [+1PA, 2PD]: Dano da Arma de Sopro aumenta em +2d6 neste Turno e Margem Crítica Menor aumenta em +3.\n"
                + "Aprimoramentos dos Apex\n"
                + "• Sopro Cataclísmico –Alcance muda para Cone Longo e o dano não é reduzido pela distância percorrida.\n"
                + "Aprimoramentos das Abominações\n"
                + "• Sopro Cataclísmico – Alcance muda para Cone Muito Longo e aplica a Corrente de Efeito Monstruosa – Sopro Apocalíptico.\n"
                + "“Sopro Apocalíptico – Aplica a Corrente de Efeitos Menor Cataclismo em cada alvo.”"),

    INVOCAR_ELEMENTAL("Invocar Elemental", MonsterCategory.PREDADOR,
            "Efeito Passivo: Este monstro possui um Subordinado que lhe serve ou auxilia.\n"
                + "Efeito Ativo – Invocar Elemental [3PA, 4PD]: Invoca dois Monstros Elementais do tipo Presa para ajudar.\n"
                + "Aprimoramentos dos Apex\n"
                + "• Invocar Elemental – o tipo dos Monstros invocados é Deviante.\n"
                + "Aprimoramentos das Abominações\n"
                + "• Possui um Subordinado adicional\n"
                + "• Invocar Elemental – o tipo dos Monstros invocados é Predador, alternativamente pode optar por invocar um único Monstro do tipo Apex."),

    TORMENTA_CATACLISMICA("Tormenta Cataclísmica", MonsterCategory.APEX,
            "Efeito Passivo – Multiplicador de PD +1.\n"
                + "Efeito Ativo – Tormenta Cataclísmica [4PA, 5PD]: Altera o terreno por 5 Rodadas, criando uma Tormenta Cataclísmica. Enquanto estiver no terreno transformado em Tormenta Cataclísmica recupera 1d6PV por Rodada e seus ataques recebem Roubo de Vida 2.\n"
                + "“Tormenta Cataclísmica – Área Circular Muito Longa, o terreno é tomado por energia elemental, infligindo 1d6+Instinto pontos de Dano Físico Elemental a todos os personagens, Equipamentos e objetos em seu interior.\n"
                + "Aprimoramentos das Abominações\n"
                + "• Tormenta Cataclísmica – Os danos da Tormenta Cataclísmica mudam para Mágico Elemental.\n"
                + "• Tormenta Cataclísmica – Uma tempestade de energia elemental cai sobre o terreno, causando 1d6 Pontos de Dano Mágico Elemental a todos os personagens."),

    ASCENCAO("Ascenção", MonsterCategory.APEX,
            "Efeito Passivo – Adquire Fisiologia Abissal ou Fisiologia Celestial.\n"
                + "“Fisiologia Abissal – Imu4nidade a Doenças e Maldições, Conjuração +2 e Ataques recebem Roubo de Vida 2 e a Corrente de Efeito Monstruosa – Murchar. Danos Elementais se tornam Profanos em substituição aos seus tipos e ignoram RM.”\n"
                + "“Murchar – Alvo sofre Redutor de -2 Multiplicador de PV por 2 Rodadas (efeito de Maldição).\n"
                + "“Fisiologia Celestial – Imunidade a Doenças e Encantamentos Nocivos, Conjuração +2, recuperam 1d6PV a cada Rodada e seus ataques recebem a Corrente de Efeitos Monstruosa - Expurgo. Danos Elementais se tornam Sagrados em substituição aos seus tipos e ignoram RM.”\n"
                + "“Expurgo – Desativa Habilidades Ativas e Encantamentos do alvo.”\n"
                + "Aprimoramentos das Abominações\n"
                + "• Imunidade a Efeitos Críticos e Correntes de Efeitos que não sejam de fontes Abissais, Celestiais ou Primordiais.");

    private final String displayName;
    private final MonsterCategory tier;
    private final String description;

    AlmaElementalAbility(final String displayName, final MonsterCategory tier, final String description) {
        this.displayName = displayName;
        this.tier = tier;
        this.description = description;
    }

    @Override
    public MonsterModel getModel() {
        return MonsterModel.ALMA_ELEMENTAL;
    }
}
