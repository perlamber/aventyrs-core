package org.aventyrs.core.monster.model.aspectohumanoide;

import lombok.Getter;
import org.aventyrs.core.monster.MonsterCategory;
import org.aventyrs.core.monster.model.MonsterModel;
import org.aventyrs.core.monster.model.MonstrousAbility;

/**
 * The Habilidades Monstruosas of the Modelo Aspecto Humanoide — {@code criacao-de-monstros.txt}.
 *
 * <p><b>Catalog entries only</b>: name, tier and rules text. None of their effects is applied by
 * this core yet ({@link MonstrousAbility#isImplemented()} is {@code false}); a monster may hold
 * them, and they count toward its Habilidade budget and its +2PV per Habilidade, but their passives
 * and actives are run at the table by hand. Abençoado de Cireneia
 * ({@code org.aventyrs.core.monster.model.cireneia.CireneiaAbility}) is the worked example to
 * follow when this Modelo is built.
 */
@Getter
public enum AspectoHumanoideAbility implements MonstrousAbility {

    CORPO_HUMANOIDE("Corpo Humanoide", MonsterCategory.PRESA,
            "Efeito Passivo: Recebe um Talento Geral ou Monstruoso adicional.\n"
                + "Efeito Ativo - Mimetizar Competência [2PD, 2PA]: Escolha uma Perícia, este monstro imita perfeitamente como outros Humanoides executam certas tarefas. Escolha uma Perícia, a Perícia escolhida tem o GD aumentado em +1 nível. Duração 3 Rodadas, Resfriamento 2 Rodadas.\n"
                + "Aprimoramento dos Deviante\n"
                + "• Mimetizar Competência - Escolha uma segunda perícia para receber os Benefícios.\n"
                + "Aprimoramento dos Predadores\n"
                + "• Mimetizar Competência – A Duração aumenta em +1 Rodada, o Tempo de Resfriamento é reduzido para 1 Rodada.\n"
                + "Aprimoramento dos Apex\n"
                + "• Mimetizar Competência - Escolha uma terceira perícia para receber os Benefícios."),

    MASCARA_SOCIAL("Máscara Social", MonsterCategory.PRESA,
            "Efeito Passivo: Bônus Racial de Carisma +2. Monstros com essa Habilidade nunca estão sozinho, usando outros personagens ou monstros como escudos, possuem um corpo que aparente ser frágil, escondendo suas verdadeiras capacidades, Mimetizando os efeitos do Talento Aparência Inofensiva, mesmo que não cumpram com o pré-requisitos.\n"
                + "Efeito Ativo - Mimetizar Inocência [1PA, 5PD]: Restauram os efeitos do Talento Aparência Inofensiva, como forma de enganar personagens que já descobriram a verdade. Este Habilidade é um Efeito de Encantamento e possui Resfriamento 3.\n"
                + "“Aparência Inofensiva - A menos que você seja o único alvo disponível, ou já tenha realizado ações ofensivas contra seus inimigos, você nunca será alvo primário de ataques, Magias ou Habilidades inimigas nas duas primeiras Rodadas de um combate.”\n"
                + "Aprimoramento dos Deviante\n"
                + "• Bônus de +2 em Perícias baseadas em Carisma.\n"
                + "Aprimoramento dos Predadores\n"
                + "• Iniciativa +3.\n"
                + "Aprimoramento dos Apex\n"
                + "• Perícias baseadas em Carisma tem o GD aumentado em +1 nível."),

    MIMETIZAR_RACA("Mimetizar Raça", MonsterCategory.DEVIANTE,
            "Efeito Passivo: Escolha uma raça, este monstro tem a aparência e as Características Raciais da raça escolhida.\n"
                + "Efeito Ativo – Transição Racial [3PA, 5PD]: Permite trocar a raça escolhida, alterando a aparência, Caraterísticas Raciais e Talentos Raciais escolhidos.\n"
                + "Aprimoramento dos Predadores\n"
                + "• Recebe um Talento Racial da raça escolhida.\n"
                + "Aprimoramento dos Apex\n"
                + "• Recebe um Talento Racial ou um Talento Racial Aventyr."),

    MIMETIZAR_CENTELHA("Mimetizar Centelha", MonsterCategory.DEVIANTE,
            "Efeito Passivo: O Multiplicador de PD aumenta em +1.\n"
                + "Ativo – Mimetizar Títulos: Escolha um Título, permite mimetizar os efeitos do Título, 1 Especialização e 2 Habilidades da Especialização escolhida.\n"
                + "Aprimoramento dos Predadores\n"
                + "• Mimetizar Centelha – permite mimetizar ambas as especializações do Título escolhido e 2 Habilidades de cada Especialização.\n"
                + "Aprimoramento dos Apex\n"
                + "• Mimetizar Centelha – Permite mimetizar uma Habilidade adicional e uma Suprema do Título escolhido."),

    MIMICO("Mímico", MonsterCategory.DEVIANTE,
            "Efeito Passivo: O GD das Perícias Furtividade e Persuasão aumentam em +1 nível.\n"
                + "Efeito Ativo – Mímico [2PA, 4PD]: Mimetiza os efeitos de uma Habilidade ativada ou Magia conjurada na última Rodada. Supremas e Magias Florescente não podem ser mimetizadas desta forma.\n"
                + "Aprimoramento dos Predadores\n"
                + "• Mímico – Permite mimetizar efeitos das últimas 3 Rodadas.\n"
                + "Aprimoramento dos Apex\n"
                + "• Mímico – Permite mimetizar os efeitos de Supremas e Magias Florescentes."),

    POSSESSAO("Possessão", MonsterCategory.PREDADOR,
            "Efeito Passivo: Ataques deste Monstro recebem a Corrente de Efeito Monstruosa – Enfraquecer o Espírito.\n"
                + "“Enfraquecer o Espírito – Alvo perde 1 ponto temporário de Autocontrole (efeito cumulativo). Pontos de Autocontrole perdidos desta forma são recuperados cada após Descansos Longos.”\n"
                + "Efeito Ativo – Possessão [3PA, 5PD]: Ataque à Distância – Alvo Único Distância Média, o Monstro toma o controle do personagem alvo por 2 Rodadas, apenas personagens com o Autocontrole inferior ao do Monstro pode ser alvo deste Efeito (efeito de Possessão). Alvos possuídos sofrem 1d6 pontos de Dano Mágico Primordial a cada Rodada. Resfriamento 3.\n"
                + "Efeito Ativo Alternativo – Possessão Contínua [10PD]: Apenas fora de combate e apenas enquanto Possessão estiver ativo, a Duração da possessão deixe ter um limite. Alvos possuídos não mais sofrem danos contínuos por Rodada em decorrência da Possessão, ao invés disso perdem 1 Multiplicador de PV por dia, até que morram ou a possessão for encerrada. Caso a Possessão seja encerrada os Multiplicadores de PV perdidos são recuperados 1 a cada Descanso Longo.\n"
                + "Aprimoramento dos Apex\n"
                + "• Autocontrole +1.\n"
                + "• Possessão – Duração dobrada contra alvos com Autocontrole zero.\n"
                + "Aprimoramento das Abominações\n"
                + "• Possessão – Apenas 1 vez por Cena, ao ser destruído (PV reduzido à zero ou menos), pode efetuar Possessão contra o Alvo com o menor Autocontrole em Distância Curta. Este Efeito ignora o Tempo de Resfriamento."),

    TANATOSE("Tanatose", MonsterCategory.APEX,
            "Efeito Passivo: Ao sofrer dano fatal, ao invés disso entra em um estado de torpor ou – quando possível – abandona o corpo em estado de possessão, evitando a morte derradeira.\n"
                + "Efeito Ativo – Mimetizar Ressurreição: Encerra o estado de Torpor ou possui um novo alvo vulnerável em seu alcance. Este efeito só pode ser ativado após 2d6 dias. Após ativer este Efeito, o Monstro perde esta Habilidade por 2d6 meses.\n"
                + "Aprimoramento das Abominação\n"
                + "• Após ativar Mimetizar Ressurreição o monstro retorna mais forte e tomado por desejo de vingança. Suas perícias têm o GD aumentado em +2 níveis contra aqueles que o derrotaram no passado."),

    FORMA_VERDADEIRA("Forma Verdadeira", MonsterCategory.APEX,
            "Efeito Passivo: A Verdadeira Forma deste monstro não é Humanoide, sendo esta forma apenas uma casca conveniente. Ao alternar de Forma, temporariamente, este Monstro adquire um novo Modelo, então recebe uma Habilidade Presa e Deviante dele.\n"
                + "Efeito Ativo – Forma Verdadeira: Ao abandonar sua forma Humanoide, este Monstro recebe Bônus Racial de +2 em todos os Atributos, rolagens de Conjuração e Resistência à Correntes de Efeitos, também recebem RD, RM e Resistência à Críticos. Os Ataques do Monstro enquanto em sua Forma Verdadeira tem a Margem Crítica Menor aumenta em +2 números, reduzem a Resistência à Correntes de Efeitos de seus alvos em -2, então recebem a Corrente de Efeitos Monstruosa – Terror da Forma Verdadeira.\n"
                + "“Terror da Forma Verdadeira – Alvos deste Ataque recebem a Condição Abalado por 2 Rodadas, novas aplicações desta Corrente de Efeitos aumenta a Duração em +1 Rodada e podem progredir a Condição para Amedrontado e Apavorado.”\n"
                + "Aprimoramento das Abominação\n"
                + "• Forma Verdadeira - GD das Rolagens de Perícias de Ataque e Defesas aumentadas em +1 nível.");

    private final String displayName;
    private final MonsterCategory tier;
    private final String description;

    AspectoHumanoideAbility(final String displayName, final MonsterCategory tier, final String description) {
        this.displayName = displayName;
        this.tier = tier;
        this.description = description;
    }

    @Override
    public MonsterModel getModel() {
        return MonsterModel.ASPECTO_HUMANOIDE;
    }
}
