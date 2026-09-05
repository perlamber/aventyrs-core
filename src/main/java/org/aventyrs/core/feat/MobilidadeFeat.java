package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.skill.SkillType;

/**
 * Talentos de Mobilidade — moving further, moving first, and attacking around a move.
 *
 * <p>The one tree so far with genuinely working effects: {@link #MOVIMENTO_RAPIDO} and {@link
 * #VELOCISTA} raise Movimento Base for real through {@link Feat#resolveMovementIncrease},
 * {@link #VELOCISTA}'s cumulative half lands through {@link Feat#resolveRoundMovementIncrease},
 * and {@link #MAIS_VELOZ_QUE_A_VISAO} grants a permanent Ponto de Ação through {@link
 * Feat#resolveActionPointsIncrease}. All three hooks were added for this tree.
 *
 * <p>What blocks the rest is <b>how much of a movement is tracked</b>. The <i>count</i> now is:
 * {@code CombatantSheet#consumeMovementThisRound()} numbers each movement of the Rodada, which
 * is what makes VELOCISTA's "para cada outro movimento feito no mesmo Turno" real. What still
 * isn't recorded is a movement's <i>distance</i> or its <i>direction</i> — so "após se mover por
 * uma Distância Curta" and "para se aproximar de inimigos" remain untestable — nor whether a
 * character moved in a <i>previous</i> Turn, since the counter resets at each Turn's start.
 * Investidas and Reposicionar are likewise unmodelled manoeuvres.
 */
public enum MobilidadeFeat implements Feat {

    /**
     * "Seu Movimento Base aumenta em +2UD, em Rodadas Pares a distância de sua primeira ação para
     * Reposicionar-se aumenta em +1UD."
     *
     * <p><b>The first half is real</b> — an unconditional "+NUD ao Movimento Base", which is
     * exactly the shape {@code ModifierType.MOVEMENT} means (see {@code MovementService}).
     */
    // TODO: the Rodadas Pares half needs Reposicionar as a distinct action with its own
    //  distance; this core has one Movimento Base figure and no per-manoeuvre allowance. The
    //  "primeira ação" half alone would now be expressible (Feat#resolveRoundMovementIncrease,
    //  movementIndex 0), but not the Reposicionar scoping that narrows it.
    MOVIMENTO_RAPIDO(
            "Seu Movimento Base aumenta em +2UD, em Rodadas Pares a distância de sua primeira ação "
                    + "para Reposicionar-se aumenta em +1UD.",
            FeatRequirements.builder().build()) {
        @Override
        public int resolveMovementIncrease(final Character character) {
            return 2;
        }
    },

    /**
     * "Seu Movimento Base aumenta em +1UD, então aumenta cumulativamente em +1UD para cada outro
     * movimento feito no mesmo Turno."
     *
     * <p><b>Both halves are real.</b> The flat "+1UD" is {@link #resolveMovementIncrease}; the
     * cumulative half is {@link #resolveRoundMovementIncrease}, now that {@code
     * CombatantSheet#consumeMovementThisRound()} counts the movements already made and {@code
     * MovementService#getMovementBase(CombatantSheet, int)} resolves against that count.
     *
     * <p>Both are per Ponto de Ação, per {@code MovementService}'s class javadoc.
     */
    VELOCISTA(
            "Seu Movimento Base aumenta em +1UD, então aumenta cumulativamente em +1UD para cada "
                    + "outro movimento feito no mesmo Turno.",
            FeatRequirements.builder()
                    .requiredFeat(MOVIMENTO_RAPIDO)
                    .build()) {
        @Override
        public int resolveMovementIncrease(final Character character) {
            return 1;
        }

        /**
         * "+1UD para cada <em>outro</em> movimento feito no mesmo Turno" — movementIndex is
         * 0-based, so it is already exactly the number of other movements made before this one:
         * 0 on the Rodada's first movement, 1 on the second, and so on.
         */
        @Override
        public int resolveRoundMovementIncrease(final int movementIndex, final Character character) {
            return movementIndex;
        }
    },

    /**
     * "Você adquire permanentemente +1PA."
     *
     * <p><b>Fully real</b>, and the simplest clause in the catalog — a permanent, unconditional
     * Ponto de Ação, summed by {@code ActionPointsService#getMaxActionPoints}.
     */
    MAIS_VELOZ_QUE_A_VISAO(
            "Você adquire permanentemente +1PA.",
            FeatRequirements.builder()
                    .requiredFeatCategory(FeatCategory.MOBILIDADE)
                    .requiredFeatCategoryCount(2)
                    .build()) {
        @Override
        public int resolveActionPointsIncrease(final Character character) {
            return 1;
        }
    },

    /**
     * "Você recebe Bônus de +2 em suas Defesas para resistir à Reações de seus inimigos."
     *
     * <p>One of the five Talentos in the catalog with no Pré-requisito at all.
     */
    // TODO: a Defesa scoped to what is being resisted has no reader — Feat#resolveDefenseBonus is
    //  documented as unconditional only, and nothing classifies an incoming attack as a Reação
    //  (the same blocker EsquivaEApararCompetencyAbility#EVASAO records).
    // TODO: the second clause additionally needs last Turn's movement — see the enum javadoc.
    ESQUIVA(
            "Você recebe Bônus de +2 em suas Defesas para resistir à Reações de seus inimigos. Se "
                    + "você se moveu em seu último Turno, então você recebe Bônus de +1 em suas "
                    + "Defesas para resistir à ataques sofridos fora de seu Turno por 1 Rodada.",
            FeatRequirements.builder().build()),

    /**
     * "Você recebe Vantagem em Rolagens de Perícias de Ataque feitas após se mover por uma
     * Distância Curta ou superior."
     *
     * <p>The source's own entry is defective here: its {@code Descrição} line reads "Talento
     * ‘Esquiva’", which is plainly a Pré-requisito printed under the wrong heading. Modelled as
     * that Pré-requisito, with the remaining lines as the description.
     */
    // TODO: both halves need the distance already moved this Turn — see the enum javadoc.
    SE_MOVER_E_ATACAR(
            "Você recebe Vantagem em Rolagens de Perícias de Ataque feitas após se mover por uma "
                    + "Distância Curta ou superior. Seu Movimento Base aumenta em +2UD para se "
                    + "mover imediatamente após efetuar uma Rolagem de Perícia de Ataque.",
            FeatRequirements.builder()
                    .requiredFeat(ESQUIVA)
                    .build()),

    /**
     * "Você adquire 1 ponto permanente de Iniciativa e então, se sua Iniciativa se tornar 4 ou
     * mais, você adquire uma Vantagem de Iniciativa."
     */
    // TODO: a permanent Ego point is granted through CharacterEgos#withVariableBonus, reached
    //  only by AttributeAbility#resolvePermanentEgoGain — Feat has no equivalent hook, and
    //  adding one means deciding whether a Talento may raise an EgoDomain at all.
    // TODO: granting a Vantagem de Ego is not expressible — a Vantagem is chosen once at
    //  character creation (CharacterCreationService), never awarded later.
    INICIATIVA_APRIMORADA(
            "Você adquire 1 ponto permanente de Iniciativa e então, se sua Iniciativa se tornar 4 "
                    + "ou mais, você adquire uma Vantagem de Iniciativa.",
            FeatRequirements.builder().build()),

    /**
     * "Sempre que ganhar uma rolagem de Iniciativa, nas 2 primeiras Rodadas de cada Cena de
     * Combate você adquire +2PA (não cumulativo)."
     *
     * <p>Both conditions have resolvers — {@code Scene#wonInitiative} and {@code
     * SceneContext#isWithinFirstCombatRounds(2)} — and the grant is a {@code Blessing} of {@code
     * ModifierType.ACTION_POINTS}, which {@code Scene#applyInitiativeBlessings} already applies.
     */
    // TODO: resolveInitiativeBlessings lives on EgoAdvantage/AttributeAbility/
    //  SkillCompetencyAbility, and InitiativeBlessingService scans exactly those three — Feat is
    //  not among them. Promoting it there would make this constant real.
    LIDERAR_O_AVANCO(
            "Sempre que ganhar uma rolagem de Iniciativa, nas 2 primeiras Rodadas de cada Cena de "
                    + "Combate você adquire +2PA (não cumulativo).",
            FeatRequirements.builder()
                    .requiredFeat(INICIATIVA_APRIMORADA)
                    .build()),

    /**
     * "Você pode adicionar Metade de seu valor de Destreza ou do valor de Carisma, a sua escolha,
     * às suas rolagens de Iniciativa."
     */
    // TODO: same missing Feat initiative hook as LIDERAR_O_AVANCO, and the ally-facing half needs
    //  cross-character grants at initiative time plus direction-scoped movement.
    // TODO: its Pré-requisito is a disjunction (Destreza 3 *ou* Carisma 3), which
    //  FeatRequirements cannot express; modelled as the Destreza branch only.
    PORTA_ESTANDARTE(
            "Você pode adicionar Metade de seu valor de Destreza ou do valor de Carisma, a sua "
                    + "escolha, às suas rolagens de Iniciativa. Nas 2 primeiras Rodadas do "
                    + "combate, apenas se você for o primeiro a agir, você e seus aliados recebem "
                    + "Pontos de Ação adicionais, sendo +2PA na primeira Rodada e +1PA na segunda. "
                    + "Nestas Rodadas iniciais seu Movimento Base aumenta em +2UD sempre que se "
                    + "mover em direção a um inimigo e o Movimento Base de seus aliados aumentam "
                    + "em +2UD para se moverem em sua direção.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.DEXTERITY)
                    .requiredAttributeValue(3)
                    .requiredFeat(LIDERAR_O_AVANCO)
                    .build()),

    /**
     * "Você pode se mover enquanto furtivo, mas seu Movimento Base é reduzido à metade", the
     * halving falling away at 7 Graduações em Furtividade.
     */
    // TODO: halving Movimento is the gap catalog's "Multiplicative stages" row — MovementService
    //  sums additively with no halving stage, and a MOVEMENT_HALVED constant is explicitly not
    //  the fix.
    // TODO: "enquanto furtivo" is now ConditionType.ESCONDIDO, but a held trait cannot see its
    //  holder's Condições — Feat#resolveMovementIncrease takes a Character, and a Condition lives
    //  on the CombatantSheet, which no Feat hook receives. Halving has no mechanism either (gap
    //  catalog, "Multiplicative stages"); don't add a MOVEMENT_HALVED constant.
    MOVIMENTO_FURTIVO(
            "Você pode se mover enquanto furtivo, mas seu Movimento Base é reduzido à metade. Se "
                    + "você possuir 7 ou mais Graduações em Furtividade este Talento não mais "
                    + "reduz seu Movimento Base.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.FURTIVIDADE)
                    .requiredSkillGraduation(2)
                    .build()),

    /**
     * "Rolagens de Perícia de Ataque realizadas imediatamente após ser bem-sucedido em rolagens de
     * Atletismo para Natação tem o Tempo de Ação reduzido em -1PA."
     */
    // TODO: this core does not track what a roll was *for*, so "Atletismo para Natação" cannot be
    //  distinguished from any other Atletismo roll, and no roll's outcome feeds the next one's
    //  cost.
    // TODO: Movimento Base de Natação is a separate sub-stat deliberately not wired to
    //  ModifierType.MOVEMENT (see AtletismoCompetencyAbility#ANFIBIO).
    // TODO: its Pré-requisito names a required Especialização (Triatleta); FeatRequirements
    //  models a Habilidade de Competência but not a SkillSpecialization.
    INVESTIDA_AQUATICA(
            "Em combate, rolagens de Perícia de Ataque realizadas imediatamente após ser "
                    + "bem-sucedido em rolagens de Atletismo para Natação tem o Tempo de Ação "
                    + "reduzido em -1PA. Personagens que possuam Movimento Base de Natação tem o "
                    + "Tempo de Ação de investidas sempre reduzidos em -1PA.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATLETISMO)
                    .requiredSkillGraduation(4)
                    .build()),

    /**
     * "Você pode fazer uma acrobacia para se mover de forma segura pelo cenário, sem provocar
     * Reações", up to half Destreza in UD plus 2UD per Título.
     *
     * <p>The source prints "3 Talentos de Mobilidade" under {@code Descrição} with the
     * Pré-requisito line left empty; read as the Pré-requisito.
     */
    // TODO: movement provoking Reações does not exist, so a movement exempt from provoking them
    //  is exempt from nothing (gap catalog, "Movement-triggered Reações").
    // TODO: a distinct per-manoeuvre movement allowance has no representation — see
    //  MOVIMENTO_RAPIDO.
    MOVIMENTO_ACROBATICO(
            "Apenas uma vez por Rodada e apenas quando puder fazer uma Ação de Reposicionar, você "
                    + "pode fazer uma acrobacia para se mover de forma segura pelo cenário, sem "
                    + "provocar Reações. A Distância que seu Movimento Acrobático pode percorrer é "
                    + "igual à Metade da Destreza UD, a distância máxima aumenta em +2UD para cada "
                    + "Título Aventyr que você possuir.",
            FeatRequirements.builder()
                    .requiredFeatCategory(FeatCategory.MOBILIDADE)
                    .requiredFeatCategoryCount(3)
                    .build()),

    /** "Suas Investidas recebem Área de Efeito – Explosão." */
    // TODO: an Investida is an unmodelled manoeuvre, and applying an Área de Efeito to one needs
    //  the footprint resolution the gap catalog's "Area de Efeito" row records as missing.
    INVESTIDA_SELVAGEM(
            "Suas Investidas recebem Área de Efeito – Explosão. Durante o movimento da investida "
                    + "você recebe Redução de Danos Sofridos igual ao número de Títulos Aventyrs "
                    + "que você possuir.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(7)
                    .requiredFeatCategory(FeatCategory.MOBILIDADE)
                    .requiredFeatCategoryCount(3)
                    .build()),

    /**
     * "Se em sua Investida Selvagem você causar danos à 3 ou mais inimigos você recebe Bônus em
     * Defesas igual à 1 + quantidade de Títulos Aventyr Bruto Despertos."
     */
    // TODO: builds on INVESTIDA_SELVAGEM, which is itself unbuilt, and additionally needs the
    //  count of targets an area attack actually damaged.
    // TODO: mutually exclusive with INVESTIDA_SELVAGEM_LUNAR, which FeatRequirements cannot
    //  express — every set clause is combined with and.
    INVESTIDA_SELVAGEM_SOLAR(
            "Se em sua Investida Selvagem você causar danos à 3 ou mais inimigos você recebe Bônus "
                    + "em Defesas igual à 1 + quantidade de Títulos Aventyr Bruto Despertos, "
                    + "Duração em Rodadas igual ao número de inimigos atingidos pela explosão da "
                    + "investida. Os Bônus em Defesas aumentam em +1 por alvo se utilizar um item "
                    + "do tipo Escudo de peso Médio ou Superior. Um mesmo personagem não pode "
                    + "possuir Investida Selvagem Lunar e Investida Selvagem Solar.",
            FeatRequirements.builder()
                    .requiredFeat(INVESTIDA_SELVAGEM)
                    .requiredAwakenedTitles(1)
                    .requiredTitleArchetype(org.aventyrs.core.title.TitleArchetype.BRUTO)
                    .build()),

    /**
     * "Se em sua Investida Selvagem você causar danos à 3 ou mais inimigos, você recebe Roubo de
     * Vida igual à 1 + quantidade de Títulos Aventyr Bruto Despertos."
     */
    // TODO: same blockers as INVESTIDA_SELVAGEM_SOLAR. LifeStealService exists, but no Feat hook
    //  grants Roubo de Vida and nothing scopes it to a Duração in Rodadas.
    INVESTIDA_SELVAGEM_LUNAR(
            "Se em sua Investida Selvagem você causar danos à 3 ou mais inimigos, você recebe "
                    + "Roubo de Vida igual à 1 + quantidade de Títulos Aventyr Bruto Despertos, "
                    + "Duração em Rodadas igual ao número de inimigos atingidos pela explosão da "
                    + "investida. O valor do Roubo de Vida aumenta em +1 se utilizar uma arma de "
                    + "peso Médio ou Superior. Um mesmo personagem não pode possuir Investida "
                    + "Selvagem Lunar e Investida Selvagem Solar.",
            FeatRequirements.builder()
                    .requiredFeat(INVESTIDA_SELVAGEM)
                    .requiredAwakenedTitles(1)
                    .requiredTitleArchetype(org.aventyrs.core.title.TitleArchetype.BRUTO)
                    .build());

    private final String description;
    private final FeatRequirements featRequirements;

    MobilidadeFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.MOBILIDADE;
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
