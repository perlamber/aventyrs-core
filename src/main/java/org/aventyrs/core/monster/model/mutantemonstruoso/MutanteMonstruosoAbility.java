package org.aventyrs.core.monster.model.mutantemonstruoso;

import lombok.Getter;
import org.aventyrs.core.monster.MonsterCategory;
import org.aventyrs.core.monster.model.MonsterModel;
import org.aventyrs.core.monster.model.MonstrousAbility;

/**
 * The Habilidades Monstruosas of the Modelo Mutante Monstruoso — {@code criacao-de-monstros.txt}.
 *
 * <p><b>Catalog entries only</b>: name, tier and rules text. None of their effects is applied by
 * this core yet ({@link MonstrousAbility#isImplemented()} is {@code false}); a monster may hold
 * them, and they count toward its Habilidade budget and its +2PV per Habilidade, but their passives
 * and actives are run at the table by hand. Abençoado de Cireneia
 * ({@code org.aventyrs.core.monster.model.cireneia.CireneiaAbility}) is the worked example to
 * follow when this Modelo is built.
 */
@Getter
public enum MutanteMonstruosoAbility implements MonstrousAbility {

    FISIOLOGIA_ESTRANHA("Fisiologia Estranha", MonsterCategory.PRESA,
            "Efeito Passivo: Recebe RDS e Resistência à Críticos, adicionalmente a Resistência à Correntes de Efeitos aumenta em +2.\n"
                + "Efeito Ativo – Fisiologia Estranha [1PA, 3PD]: Danos sofridos reduzidos à Metade (efeito de Meio-Dano) por 2 Rodadas. Resfriamento 1.\n"
                + "Aprimoramento dos Deviante\n"
                + "• Fisiologia Estranha – Defesas +3.\n"
                + "Aprimoramento dos Predadores\n"
                + "• Fisiologia Estranha – Duração aumentada em +1 Rodada.\n"
                + "Aprimoramento dos Apex\n"
                + "• Fisiologia Estranha – Imunidade à Críticos Menores e Correntes de Efeitos."),

    MEMBROS_MULTIPLOS("Membros Múltiplos", MonsterCategory.PRESA,
            "Efeito Passivo - Escolha um membro do corpo, como braços, cabeças etc. Este Monstro recebe um novo conjunto do membro escolhido.\n"
                + "Efeito Ativo – Surto de Ação [Ação Livre, 3PD]: Para cada conjunto de membros adicionais o Monstro recebe Bônus de +1PA neste Turno.\n"
                + "Aprimoramento dos Deviante\n"
                + "• Adquire um novo conjunto de Membros\n"
                + "Aprimoramento dos Predadores\n"
                + "• Adquire um novo conjunto de Membros\n"
                + "• Surto de Ação – Ações com os membros extras tem o Tempo de Ação reduzidos em -1PA. Este efeito desencadeia uma vez para cada conjunto de membro, novas ações com aquele conjunto de Membro tem o Tempo de Ação padrão.\n"
                + "Aprimoramento dos Apex\n"
                + "• Dobra o número de Membros Extras\n"
                + "• Efeito Ativo – Regenerar Membro [3PA, 1PD]: Regenera um membro Perdido."),

    SELECAO_NATURAL_SUPERIOR("Seleção Natural Superior", MonsterCategory.DEVIANTE,
            "Habilidade Passiva: Escolha um Atributo, este Monstro recebe Bônus Racial de +2 no Atributo escolhido.\n"
                + "Habilidade Ativa – Adaptabilidade [3PA, 3PD]: RDS e Multiplicador de PV +3 por 3 Rodadas. Resfriamento 3.\n"
                + "Aprimoramento dos Predadores\n"
                + "• Atributo escolhido recebe Bônus Racial de +2.\n"
                + "• Adaptabilidade – Tempo de Resfriamento reduzido para 2 Rodadas.\n"
                + "Aprimoramento dos Apex\n"
                + "• Multiplicador de PV +1 e RE: Todos os Elementos.\n"
                + "• Adaptabilidade – Multiplicador de PV +1 e Duração +2 Rodadas."),

    ELASTICIDADE("Elasticidade", MonsterCategory.DEVIANTE,
            "Efeito Passivo: Armas Naturais recebem Alcance como aprimoramento de Obra-Prima.\n"
                + "Aprimoramento dos Predadores\n"
                + "• Defesas +2\n"
                + "• Efeito Ativo – Bote Inesperado [+1PA, 1PD]: Como parte da ativação desta Habilidade o Monstro deverá fazer um ataque corpo-a-corpo, o Alcance deste ataque é dobrado. Resfriamento 1.\n"
                + "Aprimoramento dos Apex\n"
                + "• Efeito Ativo – Bote Inesperado: Recebe a Corrente de Efeitos Monstruosa – Abdução.\n"
                + "“Abdução – Alvo é agarrado, então realocado para um ponto adjacente ao Monstro. O Monstro recebe Bônus de +3 em Defesas por 1 Rodada.”"),

    TAMANHO_VARIAVEL("Tamanho Variável", MonsterCategory.PREDADOR,
            "Efeito Passivo: A Categoria de Tamanho desse monstro é permanentemente aumentada ou reduzida em 1 nível.\n"
                + "Efeito Ativo – Tamanho Variável [1PA, 1PD]: Pode temporariamente aumentar ou reduzir Categoria de Tamanho em 2 níveis. Duração de 3 Rodadas, Resfriamento 2.\n"
                + "Aprimoramentos dos Apex\n"
                + "• Bônus Racial de Força +4 e Multiplicador de PV +1.\n"
                + "• Efeito Ativo – Tamanho Variável: Aumento ou redução da Categoria de Tamanho muda para 3 níveis.\n"
                + "Aprimoramentos das Abominações\n"
                + "• Bônus Racial de +2 em dois Atributo à escolha, exceto Força.\n"
                + "• Efeito Ativo – Tamanho Variável: Duração aumenta para 5 Rodadas."),

    CAMUFLAGEM_PERFEITA("Camuflagem Perfeita", MonsterCategory.PREDADOR,
            "Efeito Passivo: GD de Furtividade aumenta em +1 nível.\n"
                + "Efeito Ativo – Camuflagem Perfeita [3PA, 3PD]: Pode efetuar Furtividade mesmo à vista de outros personagens. Resfriamento 2.\n"
                + "Aprimoramentos dos Apex\n"
                + "• Bônus de +4 em Furtividade.\n"
                + "• Camuflagem Perfeita - Pode se mover durante a Furtividade, GD reduzida em -1 nível após se mover.\n"
                + "Aprimoramentos das Abominações\n"
                + "• GD de Furtividade aumenta em +1 nível\n"
                + "• Camuflagem Perfeita - Pode atacar durante a Furtividade, GD reduzida em -2 níveis após atacar."),

    ADAPTACAO_MILAGROSA("Adaptação Milagrosa", MonsterCategory.APEX,
            "Efeito Passiva: RDS. Após sofrer danos recupera 1d6PV durante 2 Rodadas (não cumulativo).\n"
                + "Efeito Ativo – Adaptação Milagrosa [3PA, 6PD]: Recupera 3d6PV. Após sofrer danos se torna imune a mesma fonte de dano ao longo da Duração da Adaptação Milagrosa. Resfriamento 4, Duração 4 Rodadas.\n"
                + "“São fontes de danos as Armas, Armadilhas, Doenças, Habilidades, Magias, Maldições, Venenos, além de efeitos mundanos, como quedas, esmagamentos etc.”\n"
                + "Aprimoramentos das Abominações\n"
                + "• Recuperação de vida pós danos se torna cumulativa, até 3 vezes, mas a Duração não se renova até o fim do Efeito Primário.\n"
                + "• Adaptação Milagrosa – recebe RA, Resistência à Críticos e Resistência à Corrente de Efeitos +2 enquanto ativo."),

    REGENERACAO_MULTIPLICATIVA("Regeneração Multiplicativa", MonsterCategory.APEX,
            "Efeito Passivo: Recupera Vigor PV por Rodada\n"
                + "Efeito Ativo – Regeneração Multiplicativa: Apenas após perder um membro, 2 novos membros do mesmo tipo podem surgir no local. Recebe bônus de +1PA para cada par de membro regenerado desta forma. O novo membro extra, apenas o adicional, definha e cai após 3 Rodadas.\n"
                + "Aprimoramentos das Abominações\n"
                + "• Recuperação de Vida por Rodada aumenta em +1d6.\n"
                + "• Regeneração Multiplicativa – Membros adicionais definham apenas após 5 Rodadas.");

    private final String displayName;
    private final MonsterCategory tier;
    private final String description;

    MutanteMonstruosoAbility(final String displayName, final MonsterCategory tier, final String description) {
        this.displayName = displayName;
        this.tier = tier;
        this.description = description;
    }

    @Override
    public MonsterModel getModel() {
        return MonsterModel.MUTANTE_MONSTRUOSO;
    }
}
