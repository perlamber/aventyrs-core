package org.aventyrs.core.feat;

import lombok.Getter;
import org.aventyrs.core.defect.QualityClass;

/**
 * The effect of every Qualidade at each class ({@code docs/rules/defeitos-e-qualidades.txt}) —
 * {@code RADIANTE_MENOR} is "Amigável". Talento-shaped like {@link DefeitoFeat}; a held Qualidade
 * Maior folds in <b>both</b> its constants, since "todas as Qualidades Maiores possuem implicitamente,
 * de forma cumulativa, os efeitos da Qualidade Menor de seu tipo".
 *
 * <p>{@link FeatCategory#QUALIDADE}: absent from {@link FeatCatalog}, never bought.
 *
 * <p>TODO (plan Phases 2–3): each constant's effect — see {@code docs/plans/defeitos-e-qualidades-plan.md}.
 * TODO: "podem ser desativadas por Malefícios temporários, como efeitos de Maldições" — blocked until a
 * Maldição carries the suppression (table ruling: out of scope for now).
 */
@Getter
public enum QualidadeFeat implements Feat {

    CENTELHA_MAIOR_MENOR(QualityClass.MENOR, "Pronto para Ação",
            "Em cada Cena de Combate, o Primeiro Efeito de Habilidade de Título ou Talento que você"
                    + " ativar e que possua um custo em PD tem este custo reduzido à zero. Em cada Cena de"
                    + " Combate, o Primeiro Efeito de Habilidade de Título ou Talento que você ativar e que"
                    + " possua um custo em Pontos de Ego tem este custo reduzido à uma unidade."),
    CENTELHA_MAIOR_MAIOR(QualityClass.MAIOR, "Sonho de Gilgamesh",
            "Seu Multiplicador de PD aumenta em +1. Enquanto estiver empunhando pelo menos uma Regalia"
                    + " Verdadeira seu Multiplicador de PD, ao invés disso, aumenta em +2."),
    DESTINADO_A_FORTUNA_MENOR(QualityClass.MENOR, "Saber Investir",
            "Seus pontos temporários e permanentes de Recursos valem +2PE cada, , sempre que recuperar"
                    + " Recursos por conclusão de arco de história você adquire 1 ponto temporário de Recurso"
                    + " adicional."),
    DESTINADO_A_FORTUNA_MAIOR(QualityClass.MAIOR, "Privilegiado",
            "Você adquire 1 Ponto permanente de Recursos."),
    ESCOLHIDO_DA_MAGIA_MENOR(QualityClass.MENOR, "Linhagem Arcana",
            "Você recebe RM e Vantagem em suas rolagens de Domínio do Mana para Conjurar Magias."),
    ESCOLHIDO_DA_MAGIA_MAIOR(QualityClass.MAIOR, "Alma de AEther",
            "Seu multiplicador de PM é aumentado em +1. A primeira vez que você for afetado por uma"
                    + " Magia em cada Cena de Combate você pode escolher negar o Efeito, se o fizer você"
                    + " recupera PM igual a quantidade de PM utilizado na conjuração."),
    INTELECTO_SUPERIOR_MENOR(QualityClass.MENOR, "Memória Eidética",
            "Você raramente se esquece de algo que tenha presenciado e tem facilidade em aprender"
                    + " novas coisas. Ao fim da primeira sessão de jogo você adquire uma nova Especialização de"
                    + " uma Perícia treinada que possuir."),
    INTELECTO_SUPERIOR_MAIOR(QualityClass.MAIOR, "Dominar Padrões",
            "Você nunca se esquece nada e rapidamente aprende novas coisas. Adquirir graduações em"
                    + " Perícias, até a terceira graduação, custa 0.5 EXP a menos. Ao fim da terceira sessão de"
                    + " jogo, você adquire uma Habilidade de Competência de uma Perícia qualquer (mesmo que não"
                    + " seja treinado nela)."),
    OPORTUNISTA_NATO_MENOR(QualityClass.MENOR, "Sorrateiro",
            "Você adquire Vantagem na primeira Rolagem de Perícia que efetuar contra cada personagem"
                    + " Desprevenido em uma Cena."),
    OPORTUNISTA_NATO_MAIOR(QualityClass.MAIOR, "Essência Malandra",
            "Você recebe Vantagem em rolagens de Atenção e Persuasão, também recebe Vantagem em"
                    + " qualquer rolagem de Perícia efetuada contra um alvo Desprevenido (não cumulativo com"
                    + " Sorrateiro)."),
    RADIANTE_MENOR(QualityClass.MENOR, "Amigável",
            "Vantagem em rolagem de Perícias baseadas em Carisma."),
    RADIANTE_MAIOR(QualityClass.MAIOR, "Aura de Confiança",
            "Você pode reduzir o GD de uma rolagem de Perícias a cada Cena. Aura de Confiança afeta"
                    + " apenas Perícias baseada em Carisma e seu uso é restrito a apenas uma vez por Cena."),
    RESILIENCIA_HEROICA_MENOR(QualityClass.MENOR, "Motivado pelo Desafio",
            "Enquanto um de seus aliados próximos estiver Abalado, Assustado ou Apavorado você não"
                    + " pode receber estas Condições."),
    RESILIENCIA_HEROICA_MAIOR(QualityClass.MAIOR, "Inabalável",
            "Você não pode receber os malefícios Assustado e Apavorado, se limitando a Abalado."),
    RESISTENCIA_ATIPICA_MENOR(QualityClass.MENOR, "Herança Dracônica",
            "Você adquire Resistência ao tipo de Energia escolhido (redução de Duração e Meio-Dano)."),
    RESISTENCIA_ATIPICA_MAIOR(QualityClass.MAIOR, "Herança Divina",
            "Você é imune aos Efeitos e danos do tipo de energia escolhido."),
    SAUDE_DE_FERRO_MENOR(QualityClass.MENOR, "Vigoroso",
            "A cada Descanso Longo ou Superior você recupera +1PV adicionais, +2PV se possuir um"
                    + " Título Aventyr (para o máximo de +3PV)."),
    SAUDE_DE_FERRO_MAIOR(QualityClass.MAIOR, "Constituição Inabalável",
            "Seu Multiplicador de PV aumenta em +1, a cada Descanso Longo ou superior você recupera"
                    + " +2PV."),
    SENTIDO_SUPERIOR_MENOR(QualityClass.MENOR, "Sempre Alerta",
            "Vantagem em Rolagens de Atenção, ou em qualquer outra Perícia que possa se beneficiar"
                    + " deste sentido, conforme o Narrador."),
    SENTIDO_SUPERIOR_MAIOR(QualityClass.MAIOR, "Sentir o Todo",
            "Uma vez por sessão de jogo você pode escolher ser bem-sucedido em uma Rolagem de Atenção"
                    + " (ou outra Perícia, se aplicável), em Rolagens que não utilizar este benefício você"
                    + " recebe Vantagem. Esta Vantagem é aplicável apenas quando a rolagem está relacionada ao"
                    + " sentido escolhido."),
    SEXTO_SENTIDO_MENOR(QualityClass.MENOR, "Atenção Sobrenatural",
            "Você recebe Vantagem em suas rolagens de Atenção e Iniciativa."),
    SEXTO_SENTIDO_MAIOR(QualityClass.MAIOR, "Precognição",
            "Você recebe 1 ponto permanente de Inciativa + uma Especialização ou Habilidade de"
                    + " Competência de Atenção."),
    TENDENCIA_ATLETICA_MENOR(QualityClass.MENOR, "Corpo Maleável",
            "Vantagem na primeira rolagem de Perícias baseadas em Força ou Destreza efetuada a cada"
                    + " Rodada par."),
    TENDENCIA_ATLETICA_MAIOR(QualityClass.MAIOR, "Divinal",
            "Escolha entre Força ou Destreza, você adquire uma Habilidade do Atributo escolhido.");

    private final QualityClass qualityClass;
    /** The level's own name — "Amigável", "Aura de Confiança"… */
    private final String levelName;
    private final String description;

    QualidadeFeat(final QualityClass qualityClass, final String levelName, final String description) {
        this.qualityClass = qualityClass;
        this.levelName = levelName;
        this.description = description;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.QUALIDADE;
    }

    @Override
    public FeatRequirements getFeatRequirements() {
        return FeatRequirements.builder().build();
    }

    /** Held only through a Qualidade — never bought. */
    @Override
    public boolean isAcquirableOnlyAtCreation() {
        return true;
    }
}
