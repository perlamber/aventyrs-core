package org.aventyrs.core.monster.model.brotosdemapinguari;

import lombok.Getter;
import org.aventyrs.core.monster.MonsterCategory;
import org.aventyrs.core.monster.model.MonsterModel;
import org.aventyrs.core.monster.model.MonstrousAbility;

/**
 * The Habilidades Monstruosas of the Modelo Brotos de Mapinguari — {@code criacao-de-monstros.txt}.
 *
 * <p><b>Catalog entries only</b>: name, tier and rules text. None of their effects is applied by
 * this core yet ({@link MonstrousAbility#isImplemented()} is {@code false}); a monster may hold
 * them, and they count toward its Habilidade budget and its +2PV per Habilidade, but their passives
 * and actives are run at the table by hand. Abençoado de Cireneia
 * ({@code org.aventyrs.core.monster.model.cireneia.CireneiaAbility}) is the worked example to
 * follow when this Modelo is built.
 */
@Getter
public enum BrotosDeMapinguariAbility implements MonstrousAbility {

    NASCIDO_DAS_LAGRIMAS_DE_FLORA("Nascido das Lágrimas de Flora", MonsterCategory.PRESA,
            "Efeito Passivo: Bônus Racial de +2 em Instinto, Anatomia Vegetal e recupera Metade do Instinto PV a cada Rodada.\n"
                + "Efeito Ativo – Regeneração Selvagem [1PA, 2PD]:  A recuperação de PV deste Monstro muda para 1d6+Metade do Instinto por 2 Rodadas. Resfriamento 3.\n"
                + "Aprimoramento dos Deviantes\n"
                + "• Regeneração Selvagem – Resfriamento muda para 2 Rodadas\n"
                + "Aprimoramento dos Predadores\n"
                + "• Regeneração Selvagem – Recuperação de vida adicional aumenta para +2d6.\n"
                + "Aprimoramento dos Apex\n"
                + "• Regeneração Selvagem – Custo de Ativação aumenta para 3PD, Duração aumenta para 4 Rodadas."),

    NASCIDO_DO_SANGUE_DE_LACERTO("Nascido do Sangue de Lacerto", MonsterCategory.PRESA,
            "Efeito Passivo: Bônus Racial de +2 em Força, Anatomia Vegetal e recebe Roubo de Vida 2.\n"
                + "Ativo – Carnivoria [1PA 3PD]: Ataques recebem a Corrente de Efeitos Monstruosa – Carnivoria por 1 Rodada. Resfriamento 1.\n"
                + "“Carnivoria – Este ataque recebe Roubo de Vida +1d6, o Alvo é perde 1 ponto temporário de Autocontrole (efeito de Veneno). Personagens que tenham o Autocontrole reduzido à zero desta forma sofrem obrigatoriamente o efeito Torpor. Pontos de Autocontrole perdidos desta forma são recuperados cada sempre que o alvo passar por qualquer efeito de Descanso.”\n"
                + "Aprimoramento dos Deviantes\n"
                + "• Carnivoria – Margem Crítica Manor +3, recebe Oferenda Maldita como efeito Crítico adicional.\n"
                + "Aprimoramento dos Predadores\n"
                + "• Carnivoria – Apenas Descansos Verdadeiros recuperam Pontos de Autocontrole perdidos por Carnivoria.\n"
                + "Aprimoramento dos Apex\n"
                + "• Carnivoria – Tempo de Resfriamento aumenta para 2 Rodadas, Duração aumenta para 3 Rodadas."),

    PRIMAVERIL("Primaveril", MonsterCategory.DEVIANTE,
            "Efeito Passivo: A Categoria de Tamanho e o Multiplicador de PV aumentam em +1.\n"
                + "Efeito Ativo – Lançar Esporos: Personagens em Distância Curta sofrem redutor de -1 Multiplicador de PV e -1 Multiplicador de PM (efeito de Veneno) por 2 Rodadas. Ataques de monstros com a Habilidade Primaveril efetuados contra personagens afetados personagens afetados pelo Veneno Primaveril recebem Roubo de Mana 1 e Roubo de Determinação 1. Esta Habilidade afeta apenas personagens vivos que não sejam Feéricos ou outros Brotos de Mapinguari. Resfriamento 2.\n"
                + "Aprimoramento dos Predadores\n"
                + "• Lançar Esporos - Duração do Veneno Primaveril aumenta para 3 Rodadas.\n"
                + "Aprimoramento dos Apex\n"
                + "• Lançar Esporos - Área de Efeito aumenta para Distância Média."),

    OUTONAL("Outonal", MonsterCategory.DEVIANTE,
            "Efeito Passivo: Categoria de Tamanho aumenta em +1, também recebe Bônus Racial de +3 em Defesas e RDS.\n"
                + "Efeito Ativo – Murchar [3PA, 1PD]: Personagens que ataquem o Outonal sofrem redutor de -2 em Força (mínimo 1) e tem efeitos de cura reduzidos à zero (efeitos de Doença). Pontos de Vigor reduzidos por Murchar são recuperados cada sempre que o alvo passar por qualquer efeito de Descanso. Estes efeitos não são cumulativos, personagens afetados por Murchar se tornam imunes a novas instancias deste efeito até passarem por algum efeito de Descanso. Duração de 1 Rodada e Resfriamento 1.\n"
                + "Aprimoramento dos Predadores\n"
                + "• Murchar – Personagens em Distância Muito Curta são obrigados a desferir seu primeiro ataque no Monstro Outonal, este é um efeito de Encantamento e não afeta personagens Feéricos e outros Outonais.\n"
                + "Aprimoramento dos Apex\n"
                + "• Murchar – Personagens sofrem redutor adicional de -2 em Foco e Instinto."),

    RAIZES_LONGAS("Raízes Longas", MonsterCategory.PREDADOR,
            "Efeito Passivo: Os pés desta criatura são enormes raízes que se espalham pelo terreno. A Área Circular Muito Curta ao redor deste Monstro é um Terreno Difícil. Este Monstro não pode ser agarrado, empurrado ou derrubado.\n"
                + "Efeito Ativo – Raízes Longas [1PA, 3PD]: Este Monstro não pode se mover por Concentração +2 Rodadas, enquanto isso recupera 2d6PV por Rodada, recebe RD e reduz danos físicos à metade (efeito de Meio-Dano). Resfriamento 2.\n"
                + "Aprimoramento dos Apex\n"
                + "• Raízes Longas – Ao longo da Duração as raízes aplicam Veneno nos personagens em sua Área de Efeito, personagens envenenados pelas Raízes longas sofrem Redutor de -1PA. Os efeitos do veneno são encerrados após 2 Rodadas fora da Área de Efeito.\n"
                + "Aprimoramento das Abominações\n"
                + "• Raízes Longas – Personagens envenenados pelas Raízes Longas sofrem 1d6 pontos de Dano Elemental: Natural por Rodada."),

    BRUMAS_DE_FLORA("Brumas de Flora", MonsterCategory.PREDADOR,
            "Efeito Passivo: Este monstro é cercado por uma fina e constante camada de névoa, que lhe concede RM e reduz Danos Mágicos à metade (efeito de Metade).\n"
                + "Efeito Ativo – Evocação das Brumas: Pode mimetizar Magias do tipo Muda ou inferior de todas as Árvores Elemental: Natural. Tempo e Custo de conjuração conforme a magia mimetizada.\n"
                + "Aprimoramento Apex\n"
                + "• Bônus de +2 em Conjuração\n"
                + "• Evocação das Brumas – Pode mimetizar magias do tipo Emergente.\n"
                + "Aprimoramento das Abominações\n"
                + "• Bônus em Conjuração muda para +5\n"
                + "• Evocação das Brumas – Pode mimetizar magias do tipo Florescente."),

    FLAGELO_DE_LACERTO("Flagelo de Lacerto", MonsterCategory.APEX,
            "Efeito Passivo: De aspecto retorcido, a presença destes monstros perturba e assusta suas presas. Flagelos de Lacerto aplicam aura de medo (condição abalado) em todos os personagens em Distância Média. Personagens em Distância Curta que sofram danos do Flagelo de Lacerto recebem a condição Assustado por 2 Rodadas. Estes são efeitos de Maldição.\n"
                + "Efeito Ativo – Flagelo de Lacerto [4PA, 5PD]: Personagens na Área que não sejam Brotos de Mapinguari sofrem no início de cada Rodada 2d6 pontos de dano Elemental: Natural, então no fim da Rodada sofrem mais 2d6 pontos de dano Elemental: Gelo. Duração de 3 Rodadas, Resfriamento 2.\n"
                + "Aprimoramento das Abominações\n"
                + "• Flagelo de Lacerto – Tempo de Resfriamento reduzido para 1 Rodada.\n"
                + "• Ao derrotar outros Personagens (PV reduzidos à zero ou menos), Flagelos de Lacerto plantam uma de suas sementes no alvo. A cada Rodada a semente reduz 1 Multiplicador de PV do personagem afetado, personagens mortos desta forma se tornam Árvores Retorcidas. Um Flagelo de Lacerto derrotado pode transferir sua essência vital para uma Árvore Retorcida – Área de Efeito Ao Alcance dos Olhos –, então ressuscitar a partir dela em 3d6 dias.\n"
                + "“Árvore Retorcida - Efeito de Possessão de corpo morto, corpos transformados em Árvores Retorcidas não podem ser ressuscitadas e emitem uma Aura com Área de Efeito Circular Longa que aplica Medo (condição Abalado) em todos os personagens em Distância Curta (efeito de Maldição).”"),

    DADIVA_DE_GAEA("Dádiva de Gaea", MonsterCategory.APEX,
            "Efeito Passivo: Monstros Dádiva de Gaea emitem uma aura (Área de Efeito Circular Média) que cura a si próprio e os seus aliados, permitindo que recuperem PV a cada Rodada como se passassem por um Descanso Curto, enquanto sob efeito da Aura também são imunes a efeitos de Medo e a Possessões.\n"
                + "Efeito Ativo – Dádiva de Gaea [3PA, 4PD]: Encerra todos os efeitos de Maldição, Possessão e Doença sobre este Monstro e todos os seus aliados, a recuperação de PV da aura muda para Descanso Total e ignora o estado de Coma. O Monstro e seus aliados afetados recebem Roubo de Bônus Base 1. Duração de 3 Rodadas e Resfriamento 2.\n"
                + "Aprimoramento das Abominações\n"
                + "• Dádiva de Gaea – Tempo de Resfriamento reduzido para 1 Rodada.\n"
                + "• Aliados do Monstro que morram ou entrem em coma enquanto afetados pela Dádiva de Gaea se tornam Totens do Ciclo Natural. Ao morrer, um Dádiva de Gaea pode transferir sua essência vital para um Totem do Ciclo Natural – Área de Efeito Ao Alcance dos Olhos -, então ressuscitar a partir dele em 3d6 dias (efeito de Possessão).\n"
                + "“Totem do Ciclo Natural – Efeito de Encantamento, corpos transformados em Totens do Ciclo Natural tem seus PV alterados para 1 após 4 Rodadas. Os Totens recuperam PV de qualquer personagem próximo a cada Rodada como se passassem por um Descanso Mínimo. Estes efeitos são aplicados apenas se o Monstro com a Dádiva de Gaea estiver vivo.”");

    private final String displayName;
    private final MonsterCategory tier;
    private final String description;

    BrotosDeMapinguariAbility(final String displayName, final MonsterCategory tier, final String description) {
        this.displayName = displayName;
        this.tier = tier;
        this.description = description;
    }

    @Override
    public MonsterModel getModel() {
        return MonsterModel.BROTOS_DE_MAPINGUARI;
    }
}
