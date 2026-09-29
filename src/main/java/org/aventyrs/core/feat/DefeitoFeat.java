package org.aventyrs.core.feat;

import lombok.Getter;
import org.aventyrs.core.defect.DefectSeverity;

/**
 * The effect of every Defeito at every gravidade ({@code docs/rules/defeitos-e-qualidades.txt}) —
 * {@code COMPORTAMENTO_EXCENTRICO_LEVE} is "Chato". Not Talentos in the rules text; they take a
 * {@link Feat}'s shape, as {@link AntecedenteFeat} does, so each rides the Talento hook it needs:
 * {@code Character#getFeats()} folds in the constant of every held {@code HeldDefect}.
 *
 * <p>{@link FeatCategory#DEFEITO}: absent from {@link FeatCatalog}, never bought.
 *
 * <p>TODO (plan Phases 2–3): each constant's effect — see {@code docs/plans/defeitos-e-qualidades-plan.md}'s
 * clause table for the hook each one needs. Until then a held Defeito is recorded, validated and
 * displayed, but changes no figure.
 */
@Getter
public enum DefeitoFeat implements Feat {

    COMPORTAMENTO_EXCENTRICO_LEVE(DefectSeverity.LEVE, "Chato",
            "Desvantagem em rolagens de Perícias baseadas em Carisma"),
    COMPORTAMENTO_EXCENTRICO_MODERADO(DefectSeverity.MODERADO, "Esquisitão",
            "GD das Perícias baseadas em Carisma aumenta em +1 Nível."),
    COMPORTAMENTO_EXCENTRICO_GRAVE(DefectSeverity.GRAVE, "O Louco",
            "Você é incapaz de expressar ideias de maneira clara ou objetiva, para a maioria das"
                    + " pessoas suas falas não fazem sentido, por isso você falha automaticamente em rolagens de"
                    + " perícias baseadas em Carisma."),
    CORPO_FRAGIL_LEVE(DefectSeverity.LEVE, "Saúde Fraca",
            "Multiplicador de PV reduzido em -1."),
    CORPO_FRAGIL_MODERADO(DefectSeverity.MODERADO, "Doença Persistente",
            "Multiplicador de PV reduzido em -2. Os Malefícios “Doença” e “Veneno” tem as Durações"
                    + " dobradas em você."),
    CORPO_FRAGIL_GRAVE(DefectSeverity.GRAVE, "Coração de Vidro",
            "Seu Multiplicador de PV é sempre igual à 1, não é possível aumentar esta quantidade. Os"
                    + " Malefícios “Doença” e “Veneno” tem as Durações maximizadas e dobradas em você."),
    DESAPEGO_MATERIAL_LEVE(DefectSeverity.LEVE, "Minimalista",
            "Seus bens se restringem ao que você puder carregar"),
    DESAPEGO_MATERIAL_MODERADO(DefectSeverity.MODERADO, "Voto de Pobreza",
            "Você não aceita recompensas ou novos equipamentos, mantendo seus itens iniciais enquanto"
                    + " estiverem em condições de uso, substituindo por novos apenas quando eles forem"
                    + " destruídos. Você recusa novos itens mesmo se forem melhores que os seus, enquanto eles"
                    + " estiverem em um estado aceitável."),
    DESAPEGO_MATERIAL_GRAVE(DefectSeverity.GRAVE, "Recusa Material",
            "Você não possui e nem carrega nenhum item que não seja essencial para a vida, possuindo"
                    + " no máximo um Equipamento Ofensivo e um Equipam amento Defensivo (não tecnológicos) e"
                    + " roupas simples. Você recusa novos itens, mesmo que melhores que os seus, enquanto os"
                    + " atuais estiverem em condições de uso."),
    DEFICIENCIA_FISICA_LEVE(DefectSeverity.LEVE, "Dano Permanente",
            "Você sofre Desvantagem em rolagens de Perícias que dependam do membro escolhido. Caso a"
                    + " escolha tenha sido Braços, adicionalmente você sofre Desvantagem em suas rolagens de"
                    + " Danos Físicos. Se a escolha for Pernas, além do efeito comuns, Seu Movimento Base é"
                    + " reduzido em -2UD."),
    DEFICIENCIA_FISICA_MODERADO(DefectSeverity.MODERADO, "Membro Ausente",
            "Você é incapaz de realizar rolagens de Perícias que dependam do membro ausente ou não"
                    + " funcional, caso o membro escolhido seja a pernas, adicionalmente seu Movimento Base é"
                    + " reduzido à metade. Você pode substituir os efeitos deste Defeito pelos efeitos de “Dano"
                    + " Permanente” com uso de aparatos de auxílio, como próteses ou muletas."),
    DEFICIENCIA_FISICA_GRAVE(DefectSeverity.GRAVE, "Dependência",
            "Cumulativamente aos efeitos de Membro Ausente, você não pode adquirir Habilidades de"
                    + " Atributo de Força ou de Destreza (escolhido aleatoriamente)."),
    DEFICIENCIA_SENSORIAL_LEVE(DefectSeverity.LEVE, "Percepção nublada",
            "Seu sentido é pouco mais fraco que o comum, por isso você sofre Desvantagens em rolagens"
                    + " de Atenção - e outras Perícias quando aplicável -, mas apenas enquanto relacionadas ao"
                    + " sentido escolhido."),
    DEFICIENCIA_SENSORIAL_MODERADO(DefectSeverity.MODERADO, "Sentido ineficiente",
            "Um de seus sentidos é muito inferior ao padrão de sua raça, o GD para rolagens de Atenção"
                    + " - ou outras Perícias situacionais -, é aumentada em +1 Nível."),
    DEFICIENCIA_SENSORIAL_GRAVE(DefectSeverity.GRAVE, "Ausência Sensorial",
            "Você não possui o sentido escolhido, por isso falha automaticamente em rolagens de"
                    + " Atenção que envolva este sentido. Este efeito pode ser aplicado a outras perícias quando"
                    + " pertinente, conforme o Narrador."),
    DESCONEXAO_COM_O_AETHER_LEVE(DefectSeverity.LEVE, "Inapto para Magias",
            "Seu Multiplicador de PM é reduzido em -2."),
    DESCONEXAO_COM_O_AETHER_MODERADO(DefectSeverity.MODERADO, "Incompetência Arcana",
            "Seu Multiplicador de PM é igual à 1 e não é possível aumentar esta quantidade."),
    DESCONEXAO_COM_O_AETHER_GRAVE(DefectSeverity.GRAVE, "Nulificador",
            "Seu Multiplicador de PM é igual à 1, não é possível aumentar esta quantidade e você nunca"
                    + " recupera PM com Descansos e efeitos similares."),
    DISTURBIO_DE_ATENCAO_LEVE(DefectSeverity.LEVE, "Desatento",
            "Desvantagem nas rolagens de Iniciativa e Atenção."),
    DISTURBIO_DE_ATENCAO_MODERADO(DefectSeverity.MODERADO, "Sem Foco",
            "O mesmo que Desatento, adicionalmente você sofre Redutor de -1PA em Rodadas Ímpares das"
                    + " Cenas de Combate."),
    DISTURBIO_DE_ATENCAO_GRAVE(DefectSeverity.GRAVE, "Preso a Imaginação",
            "Similar a Sem Foco, adicionalmente você não pode efetuar Ações Livres ou Reações nas"
                    + " Rodadas Ímpares Rodada."),
    FOBIA_LEVE(DefectSeverity.LEVE, "Medo",
            "Enquanto na presença do objeto de sua fobia você recebe a Condição Abalado."),
    FOBIA_MODERADO(DefectSeverity.MODERADO, "Repulsa",
            "Como Medo, mas a Condição recebida é Assustado."),
    FOBIA_GRAVE(DefectSeverity.GRAVE, "Pavor Absoluto",
            "Como Medo, mas a Condição recebida é Apavorado."),
    HERANCA_DE_GILGAMESH_LEVE(DefectSeverity.LEVE, "Vontade Fraca",
            "Seu Multiplicador de PD é reduzido em -1 e é incapaz de Despertar seu Título Secundário."),
    HERANCA_DE_GILGAMESH_MODERADO(DefectSeverity.MODERADO, "Centelha Dormente",
            "Seu Multiplicador de PD é reduzido em -2 e é incapaz de Despertar seu Título Secundário,"
                    + " seu Título Primário (e único) será Desperto em atraso, apenas com 25 EXP."),
    HERANCA_DE_GILGAMESH_GRAVE(DefectSeverity.GRAVE, "Filho de Gilgamesh",
            "Seu Multiplicador de PD é igual à 1 e não é possível aumentar esta quantidade. Você não é"
                    + " capaz de Despertar Títulos Aventyr."),
    MEMORIA_FRACA_LEVE(DefectSeverity.LEVE, "Lapsos de Memória",
            "Você tem dificuldades para se lembrar de eventos passados e esquece as coisas com"
                    + " frequência."),
    MEMORIA_FRACA_MODERADO(DefectSeverity.MODERADO, "Amnésia",
            "Você não se recorda de nenhum evento da sua vida anterior ao início da campanha."),
    MEMORIA_FRACA_GRAVE(DefectSeverity.GRAVE, "Amnésia Recorrente",
            "Após passar por um Descanso Verdadeiro você pode escolher apenas um evento ocorrido, você"
                    + " esquece todos os outros acontecimentos do dia exceto pelo evento escolhido, ainda assim"
                    + " você não se recordará de todos os detalhes. Você também só é capaz de se recordar das"
                    + " pessoas que você com diariamente."),
    VULNERABILIDADE_LEVE(DefectSeverity.LEVE, "Dificuldade para Reagir",
            "Você sofre Desvantagem em rolagens de Esquiva e Aparar para evitar ataques do tipo"
                    + " escolhido, caso não consiga evitar o ataque sofrerá 2 pontos de danos adicionais."),
    VULNERABILIDADE_MODERADO(DefectSeverity.MODERADO, "Ponto Fraco",
            "A GD de Esquiva e Aparar para evitar ataques do tipo escolhido é aumentada em +1 Nível,"
                    + " caso não consiga evitar o ataque sofrerá 3 pontos de danos adicionais."),
    VULNERABILIDADE_GRAVE(DefectSeverity.GRAVE, "Trava Mental",
            "Você não é capaz de se defender de ataques do tipo escolhido, adicionalmente você sofre"
                    + " +1d6 pontos de danos destes ataques."),
    RESTRICAO_MORAL_LEVE(DefectSeverity.LEVE, "Seguidor da Lei",
            "Você sempre cumpre com as leis locais e nunca mente."),
    RESTRICAO_MORAL_MODERADO(DefectSeverity.MODERADO, "Código dos Cavaleiro",
            "Como Seguidor da Lei, mas seu código moral também o impede de se aproveitar de fraquezas"
                    + " alheias, você nunca se beneficia ou ataca personagens claramente mais fracos que você,"
                    + " que estejam rendidos, caídos ou desprevenidos."),
    RESTRICAO_MORAL_GRAVE(DefectSeverity.GRAVE, "Herói Interior",
            "Como Código dos Cavaleiros, porém você também sempre ajuda e protege os mais fracos mesmo"
                    + " que isso o prejudique, nunca ataca alvos em menor número (exceto quando claramente mais"
                    + " poderosos) e não permite que outros quebrem o Código dos Cavaleiro em sua presença.");

    private final DefectSeverity severity;
    /** The level's own name — "Chato", "Coração de Vidro"… */
    private final String levelName;
    private final String description;

    DefeitoFeat(final DefectSeverity severity, final String levelName, final String description) {
        this.severity = severity;
        this.levelName = levelName;
        this.description = description;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.DEFEITO;
    }

    @Override
    public FeatRequirements getFeatRequirements() {
        return FeatRequirements.builder().build();
    }

    /** Held only through a Defeito — never bought. */
    @Override
    public boolean isAcquirableOnlyAtCreation() {
        return true;
    }
}
