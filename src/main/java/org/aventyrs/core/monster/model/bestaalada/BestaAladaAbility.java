package org.aventyrs.core.monster.model.bestaalada;

import lombok.Getter;
import org.aventyrs.core.monster.MonsterCategory;
import org.aventyrs.core.monster.model.MonsterModel;
import org.aventyrs.core.monster.model.MonstrousAbility;

/**
 * The Habilidades Monstruosas of the Modelo Besta Alada — {@code criacao-de-monstros.txt}.
 *
 * <p><b>Catalog entries only</b>: name, tier and rules text. None of their effects is applied by
 * this core yet ({@link MonstrousAbility#isImplemented()} is {@code false}); a monster may hold
 * them, and they count toward its Habilidade budget and its +2PV per Habilidade, but their passives
 * and actives are run at the table by hand. Abençoado de Cireneia
 * ({@code org.aventyrs.core.monster.model.cireneia.CireneiaAbility}) is the worked example to
 * follow when this Modelo is built.
 */
@Getter
public enum BestaAladaAbility implements MonstrousAbility {

    PLANAR_E_PAIRAR("Planar e Pairar", MonsterCategory.PRESA,
            "Efeito Passivo: Adquire Movimento Base de Voo por 2 Rodadas após correr ou saltar.\n"
                + "Efeito Ativo – Mergulho Atroz [3PA, 2PD]: Apenas enquanto voando, este ataque é uma investida e inflige +2d6 pontos de danos. Esta habilidade encerra efeitos de voo atuais. Resfriamento 2.\n"
                + "Aprimoramentos dos Deviantes\n"
                + "• Movimento Base +2UD\n"
                + "Aprimoramentos dos Predadores\n"
                + "• Mergulho Atroz – Corrente de Efeitos Monstruosa – Empurrão Atroz. Este ataque recebe Atordoante como um Efeito Crítico adicional.\n"
                + "“Empurrão Atroz – Alvo é empurrado 2UD e então derrubado.”\n"
                + "Aprimoramentos dos Apex\n"
                + "• Mergulho Atroz – Margem Crítica Menor aumentada em +4 números, recebe Atordoante como Efeito Crítico Adicional."),

    DESFILE_DE_SYLPH("Desfile de Sylph", MonsterCategory.PRESA,
            "Efeito Passivo: Movimento Base de Voo e Movimento Base +2UD enquanto voando.\n"
                + "Efeito Ativo – Investida Aérea [3PA, 2PD]: Investida Área é uma investida cujo ataque não interrompe o movimento, permitindo o deslocamento máximo enquanto inflige danos em um alvo em sua trajetória. O Grau de Dificuldade da Perícia de Ataque aumenta em +1 nível, este ataque recebe a corrente de Efeitos Monstruosa – Abduzir. Resfriamento 2.\n"
                + "“Abduzir – Dano do ataque aumenta em +1d6, o alvo é agarrado e movido juntamente com o Monstro.”\n"
                + "Aprimoramentos dos Deviantes\n"
                + "• Investida Aérea – Movimento Base +3UD.\n"
                + "Aprimoramentos dos Predadores\n"
                + "• Investida Aérea – Margem Crítica Menor +4, Efeito Crítico adicional Guilhotina.\n"
                + "Aprimoramentos dos Apex\n"
                + "• Investida Aérea – Dano +1d6, Tempo de Resfriamento reduzido para 1 Rodada."),

    DOMINIO_DOS_CEUS("Domínio dos Céus", MonsterCategory.DEVIANTE,
            "Efeito Passivo: Perícias Físicas recebe Bônus de +2 enquanto voando.\n"
                + "Efeito Ativo – Domínio dos Céus [Ação Livre, 3PD]: Recebe Bônus de +2PA neste Turno. Resfriamento 3.\n"
                + "Aprimoramentos dos Predadores\n"
                + "• Domínio dos Céus – Resfriamento muda para 2 Rodadas\n"
                + "Aprimoramentos dos Apex\n"
                + "• Domínio dos Céus – Se torna um Efeito Passivo, Sempre ativo."),

    DEFESA_AREA("Defesa Área", MonsterCategory.DEVIANTE,
            "Efeito Passivo: Este Monstro recebe Bônus Racial de +5 em Defesas e RDS enquanto voando.\n"
                + "Efeito Ativo – Defesa Área [Reação, 2PD]: Danos de inimigos que não estejam voando são Reduzidos à Metade (efeito de Meio-Dano). Duração 1 Rodada, Resfriamento 2.\n"
                + "Aprimoramentos dos Predadores\n"
                + "• Defesa Aérea – Tempo de Resfriamento reduzido para 1 Rodada.\n"
                + "Aprimoramentos dos Apex\n"
                + "• Defesa Aérea – Enquanto o Monstro estiver voando esta Habilidade torna-se um Efeito Passivo, sempre ativo."),

    VOO_PERMANENTE("Voo Permanente", MonsterCategory.PREDADOR,
            "Efeito Passivo: Este Monstro está sempre voando, não precisando retornar ao solo mesmo para ações comuns, como comer ou dormir. O Movimento Base de Voo está sempre ativo.\n"
                + "Efeito Ativo – Voo Livre [3PA 5PD]: Se algum Efeito puder impedir este Monstro de voar, então o efeito é anulado. O monstro se torna imune a novas instancias deste mesmo efeito por 3 Rodadas. Resfriamento 1.\n"
                + "Aprimoramento Apex\n"
                + "• Voo Livre – tempo de ativação reduzido para 2PA, custo reduzido para 3PD.\n"
                + "Aprimoramento abominação\n"
                + "• Voo Livre – tempo de ativação reduzido para 1PA, Duração aumentada para 5 Rodadas."),

    SUPERSONICO("Supersônico", MonsterCategory.PREDADOR,
            "Efeito Passivo: Enquanto voando recebe Bônus de +2PA, então o Movimento Base de Voo aumenta em +2UD para cada ação de movimento realizada no mesmo Turno.\n"
                + "Efeito Ativo – Movimento Instantâneo [1PA, 5PD]: enquanto voando, se move instantaneamente para um espaço dentro de seu alcance (Movimento Base de Voo). Esta ação não provoca reações, o local alvo deve ser uma linha reta, sem obstáculos ou barreiras. Resfriamento 2.\n"
                + "Aprimoramento Apex\n"
                + "• Movimento Instantâneo – Tempo de Ação muda para Ação Livre, Custo reduzido para 3PD\n"
                + "Aprimoramento abominação\n"
                + "• Movimento instantâneo – pode ser mover para qualquer ponto em seu raio de visão (limitado ao Movimento Base de Voo), mesmo que não seja uma linha reta, permitindo desviar de barreiras e obstáculos. Tempo de Resfriamento muda para 1 Rodada."),

    PREDILETO_DE_SYLPH("Predileto de Sylph", MonsterCategory.APEX,
            "Efeito Passivo: Sempre sob efeito de Furtividade enquanto voando. Ações ofensivas efetuadas pelo Monstro desativam a Furtividade, retornando ao estado furtivo após 2 Rodadas sem novas ações ofensivas.\n"
                + "Efeito Ativo – Forma de Vento [3PA, 5PD]: Entra imediatamente em Furtividade. Adicionalmente o corpo do Monstro se torna incorpóreo, se tornando imune à efeitos e danos que não sejam Elementais ou Primordiais. O efeito adicional possui Duração de Concentração +2 Rodadas e Resfriamento 2.\n"
                + "Aprimoramento das Abominações\n"
                + "• Forma de Vento – Tempo de Ação reduzido para 1PA, Custo de Ativação reduzido para 3PD, Tempo de Resfriamento reduzido para 1 Rodada."),

    INVOCAR_DE_TEMPESTADES("Invocar de Tempestades", MonsterCategory.APEX,
            "Efeito Passivo: Uma aura de Ventos Caóticos cerca este Monstro. A Área Circular Curta ao redor é sempre terreno difícil, o efeito aumento para Área Circular Média enquanto o monstro estiver voando. Este monstro é imune a Ataques à Distância físicos.\n"
                + "Efeito Ativo - Invocar Tempestade [2PA 3PD]: No ato da Ativação e no início de cada Rodada, Personagens afetados pela aura de Ventos Caóticos são cegados e empurrados 2UD para trás a cada Rodada. Duração 3 Rodadas e Resfriamento 2.\n"
                + "Aprimoramento das Abominações\n"
                + "• Invocar Tempestade – Antes de serem empurrados, os personagens afetados pela aura de Ventos Caóticos sofrem 2d6 pontos de Dano Elemental: Vento. No fim de cada Rodada cada personagem na área sofre 2d6 pontos de Dano Elemental: Eletricidade. Danos causados por Invocar Tempestade são reduzidos em -1 para cada UD entre o personagem e o Monstro.");

    private final String displayName;
    private final MonsterCategory tier;
    private final String description;

    BestaAladaAbility(final String displayName, final MonsterCategory tier, final String description) {
        this.displayName = displayName;
        this.tier = tier;
        this.description = description;
    }

    @Override
    public MonsterModel getModel() {
        return MonsterModel.BESTA_ALADA;
    }
}
