package org.aventyrs.core.magic.catalog;

import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.magic.ActivationTime;
import org.aventyrs.core.magic.AuthoredSpell;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.SpellData;
import org.aventyrs.core.magic.SpellDuration;
import org.aventyrs.core.magic.SpellTargeting;
import org.aventyrs.core.magic.SpellTree;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

/**
 * DOMINIO PRIMORDIAL (Primordial) — six Magias, diverging at Broto for one rung and converging at
 * Muda. Its heading is spelled without the accent the word takes elsewhere ({@code DOMINIO}, not
 * {@code Domínio}); the tree's {@code getName()} carries the accented form, as every other tree
 * name does.
 *
 * <h2>Proteção Primordial demands both ramificações at once</h2>
 *
 * "Apenas personagens capazes de conjurar ‘Projétil Primordial’ e ‘Égide Primordial’ podem
 * conjurar esta magia" — one from each branch. The branch gate makes exactly that combination
 * unreachable ("é necessário escolher um dos Ramos de Especialização, sem possibilidade de
 * adquirir magias do outro ramo"), so on a plain reading the convergence rung of this tree is
 * uncastable by anyone except a Conjurador holding {@code MagiaAlternativaAbility#PRIMORDIAL}.
 *
 * <p>Nothing is bent to resolve that: {@code isEligible} still grants it on the ordinary climb —
 * a foothold at Broto on <em>either</em> branch — and the prerequisite stays prose, because it is
 * a restriction on <em>casting</em> and this core gates acquisition only. Whether the intent was
 * to make Magia Alternativa the price of this tree's depth, or the clause simply predates the
 * branch rule, is a rules question.
 */
public enum DominioPrimordialSpell implements AuthoredSpell {

    /**
     * <b>Its {@code GD da Conjuração:} line is blank in the source document.</b> One of four such
     * blanks; {@code getCastingDifficultyLevel()} is {@code null} rather than inferred.
     */
    ESTALO_PRIMORDIAL(SpellData.builder()
            .name("Estalo Primordial")
            .branchLevel(BranchLevel.SEMENTE)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .description("Ao estalar seus dedos um alvo visível, que esteja próximo do conjurador, sofre dano "
                    + "primordial.")
            .primaryEffectDescription("Escolha um alvo dentro do alcance desta magia, o alvo escolhido sofre "
                    + "2+Metade do Foco pontos de dano. "
                    + "O Dano dessa magia é reduzido em -1 para cada UD entre você e o alvo.")
            .criticalEffectType(CriticalEffectType.EXECUCAO_REAL)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.distancia(Range.DISTANCIA_CURTA))
            .build()),

    /**
     * Its {@code Alcance:} reads {@code Alvo Único Distante} with no band, and its Efeito says
     * "um alvo visível dentro de seu Alcance" — so it is authored at {@link
     * Range#AO_ALCANCE_DOS_OLHOS}, the band that <em>is</em> "limited by eyesight", rather than
     * picking a UD count the document does not give.
     *
     * <p>"Esta magia sempre acerta o alvo, ignorando sua DM" is an auto-success, and the
     * roll-resolution engine has no hook for one — the Margem Crítica half is real, the
     * auto-success half is not.
     */
    PROJETIL_PRIMORDIAL(SpellData.builder()
            .name("Projétil Primordial")
            .branchLevel(BranchLevel.BROTO)
            .branch(MagicBranch.DOMINIO_PRIMORDIAL_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .description("Cria um pequeno projétil que, quando lançado, persegue seu alvo.")
            .primaryEffectDescription("O conjurador desta magia deve escolher um alvo visível dentro de seu Alcance, "
                    + "em seguida um projétil feito de energia primordial é criado e lançado na direção do alvo. "
                    + "Esta magia sempre acerta o alvo, ignorando sua DM, e causa 1d6+Metade do Foco pontos de dano, "
                    + "este dano é reduzido em 1 para cada 2UD entre você e o alvo. "
                    + "Conjuradores com maiores conhecimento de magia causam danos maiores, com 5 graduações em "
                    + "‘Conhecimento Metamágico’ recebem Vantagem na rolagem de Dano, com 10 graduações, ao invés de "
                    + "Vantagem, causam +1d6 pontos de dano.")
            .secondaryEffectDescription("Artilharia Primordial: Ao invés de criar um único projétil, o conjurador "
                    + "pode criar 1d6 projéteis menores, que causam 2 pontos de dano cada. O conjurador deve "
                    + "escolher um alvo para cada projétil, não é possível afetar um mesmo personagem com mais de um "
                    + "projétil, a menos que ele seja uma horda. Este dano é reduzido em -1 se o alvo estiver em uma "
                    + "Distância superior a Curta. "
                    + "Conjuradores com 5 graduações em ‘Conhecimento Metamágico’ criam 2 projéteis adicionais, com "
                    + "10 graduações o total de projéteis muda para 2d6.")
            .criticalEffectType(CriticalEffectType.EXECUCAO_REAL)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.distancia(Range.AO_ALCANCE_DOS_OLHOS))
            .build()),

    /**
     * TODO a barrier occupying a fixed point and blocking line of effect needs geometry, which
     * this core never does — nothing resolves "do outro lado da barreira".
     */
    EGIDE_PRIMORDIAL(SpellData.builder()
            .name("Égide Primordial")
            .branchLevel(BranchLevel.BROTO)
            .branch(MagicBranch.DOMINIO_PRIMORDIAL_ALTERNATIVO)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .description("Cria uma barreira à frente do conjurador, que o protege de magias não primordiais, mas que "
                    + "potencializa magias primordiais.")
            .primaryEffectDescription("O conjurador cria uma barreira feita de Energia Primordial em um ponto "
                    + "adjacente. A barreira não se move, permanecendo estática nesta posição. Não é possível "
                    + "posicionar a barreira em um espaço ocupado por outros personagens. "
                    + "Em função de sua espessura é impossível afetar um personagem do outro lado da barreira com "
                    + "magias de toque, magias que afetariam Alvos Distantes ou de Área de Efeito são bloqueadas, "
                    + "assim como projéteis imbuídos com magia. "
                    + "Magias primordiais que toquem a barreira, ao invés de serem bloqueadas são potencializadas, "
                    + "seu alcance é aumentado em +2UD e seu dano aumentam em +1.")
            .secondaryEffectDescription("Barricada Primordial: O conjurador pode escolher cobrir uma área maior com "
                    + "sua Égide Primordial, para cada UD adicional a Duração desta magia é reduzida em 1 Rodada. É "
                    + "possível fazer proteções curvas ou coberturas superiores desta forma.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.distancia(Range.ADJACENTE))
            .build()),

    /**
     * The convergence rung — and the one that names a Magia from <em>each</em> ramificação as a
     * casting prerequisite. See the class javadoc.
     *
     * <p><b>Its {@code GD da Conjuração:} line is blank in the source document.</b> TODO its Efeito
     * Alternativo Nova Primordial ends this Magia's own Duração early, and nothing lets an effect
     * cancel itself; "empurrados 1UD para trás" is forced movement, which this core never does.
     */
    PROTECAO_PRIMORDIAL(SpellData.builder()
            .name("Proteção Primordial")
            .branchLevel(BranchLevel.MUDA)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .description("Cria anéis de energia que se expandem rapidamente, causando danos a todas as criaturas ao "
                    + "redor enquanto protegem o conjurador.")
            .primaryEffectDescription("Cria um anel de energia primordial ao redor de você, que lhe protege de "
                    + "ataques inimigos. Você recebe Bônus de +2 em suas Defesas. "
                    + "Seu Conhecimento Metamágico é crucial para determinar as capacidades de proteção desta magia, "
                    + "com 5 Graduações o Bônus em Defesas muda para +3, com 10 graduações aumenta para +5. "
                    + "Sempre que você for alvo de um ataque o anel defensivo se expande, ferindo e empurrando os "
                    + "agressores. Todos os inimigos em Distância Curta sofrem uma quantidade de pontos de dano "
                    + "igual à Metade do Bônus Defensivo recebido + Metade do Foco, este dano é reduzido em -1 para "
                    + "cada 2UD entre você e o alvo. Personagens que sofram pelo menos 3 pontos de dano desta forma "
                    + "são empurrados 1UD para trás. "
                    + "Apenas personagens capazes de conjurar ‘Projétil Primordial’ e ‘Égide Primordial’ podem "
                    + "conjurar esta magia.")
            .secondaryEffectDescription("Nova Primordial: Como uma Ação Livre, enquanto Proteção Primordial estiver "
                    + "ativa, você pode fazer com que os anéis mágicos se expandam até se dissiparem, causando "
                    + "2d6+Metade do Foco pontos de dano a todos os inimigos em Distância Longa, o dano é reduzido "
                    + "em -1 para cada UD entre você e os alvos, personagens atingidos em Distância Curta são "
                    + "empurrados 1UD para trás. "
                    + "Após conjurar Nova Primordial a Duração da Proteção Primordial é imediatamente encerrada.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .build()),

    /**
     * A Magia that modifies <em>other</em> Magias cast while it is up — +1PM, the Primordial type,
     * -1 nível de GD, +2 Margem Crítica Menor and an added Execução Real. TODO none of it is
     * reachable: {@code SpellCastingService} resolves neither roll's GD, and a Magia's own columns
     * are authored data with no per-cast override layer.
     */
    ARMAMENTO_PRIMORDIAL(SpellData.builder()
            .name("Armamento Primordial")
            .branchLevel(BranchLevel.EMERGENTE)
            .activationTime(ActivationTime.pa(1))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("O conjurador se cerca de energia primordial, fortalecendo suas magias, dando a elas "
                    + "adicionalmente o tipo Primordial.")
            .primaryEffectDescription("Após conjurar esta magia você fica sobrecarregado de energia Primordial, "
                    + "emitindo uma forte aura. "
                    + "Outras magias que conjurar enquanto estiver sob o efeito de Armamento Primordial custam +1PM "
                    + "para serem conjuradas, recebem o tipo Primordial e tem seu GD reduzido em -1 nível, "
                    + "adicionalmente tem a Margem Crítica Menor aumentada em +2 números e recebem o Efeito Crítico: "
                    + "Execução Real.")
            .criticalEffectType(CriticalEffectType.GUILHOTINA)
            .duration(SpellDuration.rodadas(1))
            .targeting(SpellTargeting.PESSOAL)
            .build()),

    /**
     * Its Efeito carries a rung-to-GD table for the caster's <em>other</em> Magias — Semente
     * always succeeds, Broto Muito Fácil, Muda Fácil, Emergente Médio, Florescente Difícil. That
     * is not this Magia's own {@code GD da Conjuração:} (Improvável), so it is transcribed inline
     * rather than through {@code getCastingDifficultyAgainst}, which answers about the effect a
     * Magia <em>targets</em>.
     *
     * <p>Note {@code Muito Fácil} is {@code DifficultyLevel#VERY_EASY}, a tier no Magia's own GD
     * ever uses.
     */
    CORPO_DE_AETHER(SpellData.builder()
            .name("Corpo de AEther")
            .branchLevel(BranchLevel.FLORESCENTE)
            .activationTime(ActivationTime.pa(4))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .description("Você se transforma em ser feito de Mana e Aether, elevando seu poder ofensivo e defensivo "
                    + "aos limites.")
            .primaryEffectDescription("Uma das mais poderosas magias conhecidas, Corpo do Aether te transforma em um "
                    + "ser feito da mais pura energia do universo, por um curto período sua constituição é "
                    + "comparável à dos Deuses Primordiais. "
                    + "Enquanto nessa forma todas as suas magias são convertidas em magias Primordiais em "
                    + "substituição aos seus tipos e suas magias ofensivas custam +2PM, tem sua Perícia Chave para "
                    + "Conjuração alterada para Domínio do Mana, com GD proporcional ao seu poder e se bem-sucedidas "
                    + "sempre acertam seu alvo. Também recebem os Efeito Crítico ‘Toque do Aether’ e Guilhotina, a "
                    + "Margem Crítica Menor destas magias aumenta em +3 números. "
                    + "O GD proporcional é: Semente — Sempre bem-sucedido; Broto — Muito Fácil; Muda — Fácil; "
                    + "Emergente — Médio; Florescente — Difícil. "
                    + "Você também recebe grande tenacidade, reduzindo todo o dano recebido à metade, exceto danos "
                    + "de fontes Primordiais e de Regalias. "
                    + "Como consequência da imensa quantidade de energia fluindo por seu corpo você perde 1d6PV e "
                    + "2PD por Rodada, caso seus PV ou PD cheguem a zero antes do fim da Duração da magia ela é "
                    + "dissipada precocemente.")
            .criticalEffectType(CriticalEffectType.AMENIZAR)
            .duration(SpellDuration.rodadas(2))
            .targeting(SpellTargeting.PESSOAL)
            .build());

    private final SpellData data;

    DominioPrimordialSpell(final SpellData data) {
        this.data = data;
    }

    @Override
    public SpellData getData() {
        return data;
    }

    @Override
    public SpellTree getTree() {
        return MagicTree.DOMINIO_PRIMORDIAL;
    }
}
