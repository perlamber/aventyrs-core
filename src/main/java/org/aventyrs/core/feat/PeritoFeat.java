package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;

/**
 * Talentos de Perito — depth in a chosen Perícia rather than breadth.
 *
 * <p>Almost the whole tree hangs off {@link #FOCO_EM_PERICIA}'s acquisition-time choice of a
 * Perícia. That choice is now recorded: {@link FocoEmPericiaFeat} is the acquired,
 * choice-carrying form (an {@link AbstractFeat} subclass granted in place of the bare constant,
 * the same split {@code ArtesAprimorarComArteAbility} keeps against its enum constant), and the
 * "a Perícia escolhida" constants below read it via {@link FocoEmPericiaFeat#chosenBy}. Only
 * {@link #FOCO_EM_PERICIA} itself is fully unblocked by that — every dependent hits a second
 * blocker of its own, noted per constant.
 *
 * <p>That recurring second blocker is "Vantagem adicional". A plain Vantagem is a flat {@code
 * Skill#ADVANTAGE_BONUS} (+2) and {@link Feat#resolveSkillRollBonus} does reach a Perícia roll
 * for real — {@code AbstractSkillInteraction#sumFeatRollBonuses} scans {@code
 * character.getFeats()} alongside the three ability sources. What is still missing is the
 * <em>adicional</em>: stacking a second Vantagem onto a roll that already has one is not
 * distinguishable from a larger flat bonus, since nothing records that a roll is already
 * advantaged. {@link #LEITURA_COMPORTAMENTAL} is the one constant here needing neither the
 * chosen-Perícia representation nor that distinction, and it is granted for real.
 */
public enum PeritoFeat implements Feat {

    /**
     * "Escolha uma perícia, adquira vantagem nas rolagens da Perícia escolhida."
     *
     * <p><b>Real</b>, through {@link FocoEmPericiaFeat} — the acquired, choice-carrying form
     * granted in {@code Character#feats} in place of this constant, which stays the
     * catalog/rules-text entry. The Vantagem is a flat {@code Skill#ADVANTAGE_BONUS} on the
     * chosen Perícia, summed by {@code AbstractSkillInteraction#sumFeatRollBonuses}.
     */
    // TODO: its Pré-requisito is conditional on the choice itself ("Treinamento na Perícia
    //  escolhida, que não seja de ataque, ou 4 graduações se for uma Perícia de ataque"), so it
    //  cannot be checked before the choice exists; left unset.
    FOCO_EM_PERICIA(
            "Escolha uma perícia, adquira vantagem nas rolagens da Perícia escolhida.",
            FeatRequirements.builder().build()),

    /**
     * "Sempre que efetuar rolagens de uma Perícia que você tenha Foco você adquire Vantagem
     * adicional", provided no ally or neutral is within Distância Curta.
     */
    // TODO: which Perícia a character has Foco in is now readable (FocoEmPericiaFeat#chosenBy).
    //  Two blockers remain: the ally half of the proximity condition is expressible
    //  (Feat#resolveSkillRollBonus receives a SceneContext, so SceneContext#countAlliesWithin is
    //  reachable) but "personagens neutros" is a third allegiance this core does not have —
    //  Scene splits participants into allies and everyone else; and this grants "Vantagem
    //  adicional", the stacking this enum's javadoc describes as unrepresented.
    DISCRETO(
            "Sempre que efetuar rolagens de uma Perícia que você tenha Foco você adquire Vantagem "
                    + "adicional. Este talento só pode ser usado se você não tiver aliados ou "
                    + "personagens neutros em Distâncias Curtas. Se adicionalmente nenhum inimigo "
                    + "puder vê-lo você também recebe Vantagem em rolagens de Dano.",
            FeatRequirements.builder()
                    .requiredFeat(FOCO_EM_PERICIA)
                    .build()),

    /**
     * "Sempre que efetuar rolagens de uma Perícia que você tenha foco em frente a uma plateia
     * você recebe vantagem adicional."
     */
    // TODO: a "plateia" is 4+ intelligent neutral characters — the same missing neutral
    //  allegiance as DISCRETO. Counting them is the only blocker on the condition itself; the
    //  Scene is reachable (Feat#resolveSkillRollBonus takes a SceneContext).
    EXIBICIONISTA(
            "Sempre que efetuar rolagens de uma Perícia que você tenha foco em frente a uma "
                    + "plateia você recebe vantagem adicional. Considere plateia um grupo de 4 ou "
                    + "mais personagens inteligentes neutros a cena.",
            FeatRequirements.builder()
                    .requiredFeat(FOCO_EM_PERICIA)
                    .build()),

    /**
     * "A primeira rolagem da Perícia escolhida que fizer em cada um de seus Turnos tem o Tempo de
     * Ação reduzido em -1PA."
     */
    // TODO: the chosen Perícia is readable now (FocoEmPericiaFeat#chosenBy), but the effect is
    //  still blocked: ModifierType.SKILL_ROLL_COST exists and ActionPointsService reads it, but
    //  only from attributeAbilities — Feat has no SKILL_ROLL_COST hook (the new
    //  resolveAttackCostDifficultyReduction is a GD hook, not that). The action log now records a
    //  roll's Perícia, so "the Turn's first roll of the chosen Perícia" is derivable from it —
    //  only the cost hook is missing.
    PERITO_VELOZ(
            "A primeira rolagem da Perícia escolhida que fizer em cada um de seus Turnos tem o "
                    + "Tempo de Ação reduzido em -1PA.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.GNOSE)
                    .requiredAttributeValue(3)
                    .requiredFeat(FOCO_EM_PERICIA)
                    .build()),

    /**
     * "Na primeira Rodada de cada Cena de Combate você pode optar por utilizar a Perícia escolhida
     * como uma Ação Livre ou como uma Reação."
     */
    // TODO: what a Perícia roll *costs* is the caller's, not this core's — nothing lets a roll be
    //  reclassified as an Ação Livre or a Reação.
    MESTRE_PERITO(
            "Na primeira Rodada de cada Cena de Combate você pode optar por utilizar a Perícia "
                    + "escolhida como uma Ação Livre ou como uma Reação. Nas segundas Rodadas você "
                    + "poderá utilizar a Perícia escolhida como uma Ação Livre. Estes benefícios "
                    + "são restritos a uma utilização por Rodada.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.GNOSE)
                    .requiredAttributeValue(5)
                    .requiredFeat(PERITO_VELOZ)
                    .build()),

    /**
     * "Se não estiver em combate você pode optar por reduzir o GD de suas rolagens em -2 Níveis ao
     * custo de triplicar seu Tempo de Ação."
     */
    // TODO: Feat#resolveDifficultyReduction is real and scanned by
    //  AbstractSkillInteraction#sumFeatDifficultyReductions, but it is documented for an
    //  *unconditional* reduction on a named Perícia, and this one is conditioned on being out of
    //  combat — SceneContext#isCombatScene() would answer that, but resolveDifficultyReduction
    //  takes no SceneContext (unlike resolveSkillRollBonus). "Triplicar seu Tempo de Ação" has no
    //  representation either; what a roll costs is the caller's.
    // TODO: rerolling the lowest die has no representation — this core never rolls dice.
    // TODO: its Pré-requisito counts Habilidades de Competência or Especializações of the chosen
    //  Perícia; FeatRequirements can name one Habilidade, not a count of them, and not either/or.
    MAESTRIA_EM_PERICIA(
            "Se não estiver em combate ou qualquer outra situação de estresse, você pode optar por "
                    + "reduzir o GD de suas rolagens em -2 Níveis ao custo de triplicar seu Tempo "
                    + "de Ação. Sob situações de estresse ou combates, ao invés dos efeitos "
                    + "anteriores, você poderá rolar novamente o dado de menor valor de suas "
                    + "rolagens. Apenas a Perícia escolhida em Foco em Perícia recebe estes "
                    + "benefícios. Este Talento não pode ser usado em conjunto com a perícia "
                    + "Persuasão e Atenção.",
            FeatRequirements.builder()
                    .requiredFeat(FOCO_EM_PERICIA)
                    .build()),

    /**
     * "A Margem Crítica de todas as suas rolagens de Perícias é aumentada em +2", excluding
     * Perícias de Ataque and Esquiva e Aparar.
     *
     * <p><b>Real.</b> Unusually, this one names the Margem Crítica without narrowing to the Menor
     * tier — so it matches {@code SkillRoll#getCriticalResult(int)}'s shape exactly — and the
     * exclusion is expressible via {@code SkillType#isAttackSkill()}. The one Talento in this tree
     * that needs neither the chosen-Perícia representation nor the "adicional" stacking the enum
     * javadoc describes: it names every Perícia and states a plain number.
     */
    // TODO: "4 ou mais Graduações em pelo menos 3 diferentes Perícias" is a count across
    //  Perícias; FeatRequirements names one Perícia and one threshold, so the Pré-requisito is
    //  left unset and this is wrongly open.
    CONTROLE_DA_SITUACAO(
            "A Margem Crítica de todas as suas rolagens de Perícias é aumentada em +2. Este "
                    + "Talento não afeta rolagens de Perícias de Ataque e Esquivar e Aparar.",
            FeatRequirements.builder().build()) {
        /**
         * Unconditional on everything but the Perícia rolled — no Scene and no holder state is
         * consulted, so it applies to a bonuses-only query as readily as to a live roll.
         */
        @Override
        public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                                  final Character character) {
            boolean excluded = skillType.isAttackSkill() || skillType == SkillType.ESQUIVA_E_APARAR;
            return excluded ? 0 : CONTROLE_DA_SITUACAO_MARGIN_INCREASE;
        }
    },

    /**
     * "Após falhar em uma rolagem de Perícia você pode optar por tentar novamente a mesma ação, se
     * o fizer a nova tentativa utilizará apenas 1PA."
     */
    // TODO: retrying a failed roll is the caller's own step — this core never rolls dice, and an
    //  InteractionResult reports a roll without offering to repeat it.
    // TODO: its Pré-requisito is a disjunction (Gnose 3 *ou* Foco 3); modelled as the Gnose
    //  branch only, so the Foco route is wrongly refused.
    LEMBRAR_COMO_SE_FAZ(
            "Após falhar em uma rolagem de Perícia você pode optar por tentar novamente a mesma "
                    + "ação, se o fizer a nova tentativa utilizará apenas 1PA e poderá ser rolada "
                    + "com bônus de Vantagem.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.GNOSE)
                    .requiredAttributeValue(3)
                    .build()),

    /** "Sempre que for beneficiado pelos efeitos de 'Lembrar Como se Faz', a segunda rolagem tem o GD reduzido em -1 nível." */
    // TODO: builds on LEMBRAR_COMO_SE_FAZ, which is itself unbuilt — so although
    //  Feat#resolveDifficultyReduction is real, there is no "segunda rolagem" to reduce the GD of.
    // TODO: disjunctive Pré-requisito (Gnose 5 ou Foco 5) — see LEMBRAR_COMO_SE_FAZ.
    LEMBRAR_REVISAR_E_APRIMORAR(
            "Sempre que for beneficiado pelos efeitos de ‘Lembrar Como se Faz’, a segunda rolagem "
                    + "tem o GD reduzido em -1 nível.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.GNOSE)
                    .requiredAttributeValue(5)
                    .requiredFeat(LEMBRAR_COMO_SE_FAZ)
                    .build()),

    /**
     * "Adquira vantagem em suas rolagens de 'Persuasão'. A GD de suas rolagens de Atenção:
     * Discernir Motivação é reduzida em -1 nível."
     *
     * <p>The Persuasão half is the closest thing in this tree to a plain, unconditional grant —
     * a flat {@code Skill#ADVANTAGE_BONUS} on one <em>named</em> Perícia, which {@code
     * ModifierType#PERSUASAO_ROLL_BONUS} exists precisely to carry.
     */
    // TODO: the Atenção half is scoped to one Especialização (Discernir Motivação). {@code
    //  Feat#resolveDifficultyReduction} takes a SkillType but no SkillTrait, so it cannot see
    //  which Especialização the roll was requested with — unlike resolveSkillRollBonus, which
    //  does receive one. Reducing the GD of *every* Atenção roll would be wider than the clause.
    LEITURA_COMPORTAMENTAL(
            "Adquira vantagem em suas rolagens de ‘Persuasão’. A GD de suas rolagens de Atenção: "
                    + "Discernir Motivação é reduzida em -1 nível.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.INSTINCT)
                    .requiredAttributeValue(3)
                    .build()) {
        /** One named Perícia, no condition — the plain form of this tree's recurring clause. */
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return skillType == SkillType.PERSUASAO ? Skill.ADVANTAGE_BONUS : 0;
        }
    },

    /**
     * "Você recebe Vantagem em suas rolagens de 'Artes' e 'Persuasão', mas apenas quando tiver a
     * intenção de blefar, realizar ações teatrais ou disfarces."
     */
    // TODO: the scope is a narrative *purpose*, which CLAUDE.md records as unmodellable — this
    //  core does not track what a roll is for. Granting it unconditionally would over-grant on
    //  every Artes and Persuasão roll.
    // TODO: "Atuação" is not a SkillType; read as the Artes Perícia, whose Especializações cover
    //  it. The Pré-requisito is modelled against Persuasão alone, the half that maps cleanly.
    MESTRE_EM_ATUACAO(
            "Você recebe Vantagem em suas rolagens de ‘Artes’ e ‘Persuasão’, mas apenas quando "
                    + "tiver a intenção de blefar, realizar ações teatrais ou disfarces.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.PERSUASAO)
                    .requiredSkillGraduation(2)
                    .build()),

    /**
     * "Escolha 3 Perícias que você possua Treinamento. Você pode escolher uma Especialização ou
     * Habilidade de Competência de cada uma destas Perícias."
     */
    // TODO: this is an acquisition-slot grant — the gap catalog records that such traits have no
    //  shape (Elfo's Origem Mística, Anão's Pequenos Gigantes are the same problem).
    // TODO: "Personagens recém-criados" is a creation-time-only restriction with no
    //  representation; FeatRequirements has no notion of when a Talento may be taken.
    TREINADO_EM_PERICIAS(
            "Escolha 3 Perícias que você possua Treinamento. Você pode escolher uma Especialização "
                    + "ou Habilidade de Competência de cada uma destas Perícias.",
            FeatRequirements.builder().build()),

    /**
     * "No início de cada Cena de Combate você pode escolher reduzir seu valor de Iniciativa de
     * modo a agir por último. Enquanto você for o último a agir você recebe uma Ação Livre e
     * Reação adicional."
     */
    // TODO: "Iniciativa 2 ou inferior" is a *maximum*, and on an EgoDomain rather than an
    //  Attribute — FeatRequirements expresses only Attribute minimums, so this is left unset and
    //  is wrongly open to a high-Iniciativa character.
    // TODO: deliberately lowering one's own Iniciativa has no entry point, and "enquanto for o
    //  último a agir" is turn-order position, which Scene resolves for nobody.
    ANALISTA_TATICO(
            "No início de cada Cena de Combate, antes de qualquer ação sua, você pode escolher "
                    + "reduzir seu valor de Iniciativa de modo a agir por último. Enquanto você "
                    + "for o último a agir você recebe uma Ação Livre e Reação adicional.",
            FeatRequirements.builder().build()),

    /**
     * "Enquanto você for o último a agir, sua Margem Crítica Menor aumenta em +1 para cada Título
     * Aventyr Desperto."
     */
    // TODO: same turn-order-position and Iniciativa-maximum blockers as ANALISTA_TATICO —
    //  Feat#resolveCriticalMarginIncrease exists and names the Menor tier this clause wants, but
    //  "enquanto você for o último a agir" has nothing to read: turn order is not live on
    //  SceneContext.
    // TODO: resistance to Correntes de Efeitos is not a stat — EffectChainService compares a
    //  margin, with no per-character resistance term to raise or lower.
    GRANDE_ANALISTA_TATICO(
            "Enquanto você for o último a agir, sua Margem Crítica Menor aumenta em +1 para cada "
                    + "Título Aventyr Desperto. Enquanto você for o último a agir, a sua "
                    + "resistência a Corrente de Efeitos aumenta em +1 e a resistência à Correntes "
                    + "de Efeitos de seus inimigos alvos é reduzida em -1.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Você recebe Vantagem adicional em suas Rolagens da Perícia escolhida enquanto houver pelo
     * menos dois aliados em Distância Curta treinados nesta mesma Perícia."
     */
    // TODO: the chosen Perícia is readable now (FocoEmPericiaFeat#chosenBy), and the proximity
    //  half is expressible — Feat#resolveSkillRollBonus receives the SceneContext,
    //  SceneContext#getAlliesWithin resolves it, and each ally's Graduação in that Perícia is
    //  readable. What still blocks it is the "adicional" stacking this enum's javadoc describes:
    //  nothing records that a roll is already advantaged.
    TRABALHO_EM_EQUIPE(
            "Você recebe Vantagem adicional em suas Rolagens da Perícia escolhida no Talento Foco "
                    + "em Perícia enquanto houver pelo menos dois aliados em Distância Curta "
                    + "treinados nesta mesma Perícia.",
            FeatRequirements.builder()
                    .requiredFeat(FOCO_EM_PERICIA)
                    .build()),

    /** "Ao invés de receber Vantagem adicional a GD de sua rolagem será reduzida em -1 Nível." */
    // TODO: builds on TRABALHO_EM_EQUIPE, which is itself unbuilt (blocked on the "adicional"
    //  stacking) — so although Feat#resolveDifficultyReduction is real, there is no "Vantagem
    //  adicional" trigger for it to replace.
    TRABALHO_EM_EQUIPE_APRIMORADO(
            "Sempre que você puder ser beneficiado pelo Talento Trabalho em Equipe, ao invés de "
                    + "receber Vantagem adicional a GD de sua rolagem será reduzida em -1 Nível.",
            FeatRequirements.builder()
                    .requiredFeat(TRABALHO_EM_EQUIPE)
                    .requiredAwakenedTitles(1)
                    .build()),

    /** "Você pode respirar na água por um curto período, ao custo de 1PD por Rodada." */
    // TODO: nothing tracks breathing (gap catalog, "Fadiga/asfixia"), so there is no drowning to
    //  be exempt from. The PD cost is spendable, but nothing would be bought with it.
    // TODO: its Pré-requisito names an Especialização (Pulmão de Aço) alongside the Habilidade de
    //  Competência; only the Habilidade is expressible.
    CRIANCA_DO_MAR(
            "Você pode respirar na água por um curto período, ao custo de 1PD por Rodada.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATLETISMO)
                    .requiredSkillGraduation(1)
                    .build()),

    /** "Você é capaz de se mover e grudar em paredes e tetos, ao custo de 1PD por Rodada." */
    // TODO: climbing/vertical movement is a separate sub-stat deliberately not wired to
    //  ModifierType.MOVEMENT (see AtletismoCompetencyAbility#ALPINISTA_VELOZ), and this core
    //  never does geometry, so surfaces and orientation have no representation.
    // TODO: Especialização Pré-requisito — see CRIANCA_DO_MAR.
    REI_DA_MONTANHA(
            "Como insetos, você é capaz de se mover e grudar em paredes e tetos, incluindo "
                    + "superfícies lisas e movimentos de cabeça para baixo, ao custo de 1PD por "
                    + "Rodada.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATLETISMO)
                    .requiredSkillGraduation(1)
                    .build());

    /** CONTROLE_DA_SITUACAO's own stated "+2" to the Margem Crítica. */
    private static final int CONTROLE_DA_SITUACAO_MARGIN_INCREASE = 2;

    private final String description;
    private final FeatRequirements featRequirements;

    PeritoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.PERITO;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public FeatRequirements getFeatRequirements() {
        return featRequirements;
    }
}
