package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.atletismo.AtletismoSpecialization;

import java.util.List;
import java.util.function.Supplier;

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
 * <b>Reposicionar</b> is modelled ({@code Manoeuvre#REPOSICIONAR}, {@code RepositionService}: 1UD,
 * Ação Livre, no Reações — a table ruling), widened by {@link Feat#resolveRepositionDistanceIncrease},
 * which is how {@link #MOVIMENTO_RAPIDO}'s Rodadas Pares half lands.
 *
 * <p><b>The Investida is modelled now</b> — {@code
 * org.aventyrs.core.character.services.ChargeService} prices and gates it and {@code
 * Manoeuvre#INVESTIDA} marks the attack it carries — which makes {@link #INVESTIDA_AQUATICA}'s
 * second clause real and {@link #INVESTIDA_SELVAGEM}'s Redução de Danos computable. Movement
 * provoking Reações is modelled too, as far as it can be: {@code
 * org.aventyrs.core.character.services.MovementReactionService} resolves <i>who</i> may react, but
 * nothing in this core fires a Reação, so a clause exempting a movement from provoking them still
 * exempts its holder from nothing that happens.
 */
public enum MobilidadeFeat implements Feat {

    /**
     * "Seu Movimento Base aumenta em +2UD, em Rodadas Pares a distância de sua primeira ação para
     * Reposicionar-se aumenta em +1UD."
     *
     * <p><b>Both halves are real.</b> The first is an unconditional "+NUD ao Movimento Base",
     * exactly the shape {@code ModifierType.MOVEMENT} means (see {@code MovementService}); the
     * second is {@link #resolveRepositionDistanceIncrease}, read by {@code RepositionService}.
     */
    MOVIMENTO_RAPIDO(
            "Seu Movimento Base aumenta em +2UD, em Rodadas Pares a distância de sua primeira ação "
                    + "para Reposicionar-se aumenta em +1UD.",
            () -> FeatRequirements.builder().build()) {
        @Override
        public int resolveMovementIncrease(final Character character) {
            return 2;
        }

        /**
         * "Em Rodadas Pares a distância de sua primeira ação para Reposicionar-se aumenta em +1UD"
         * — the table counts Rodadas from 1 and this core from 0, so a table Rodada Par is an odd
         * currentRound.
         */
        @Override
        public int resolveRepositionDistanceIncrease(final int currentRound, final int repositionIndex,
                                                     final Character character) {
            return currentRound % 2 == 1 && repositionIndex == 0 ? 1 : 0;
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
            () -> FeatRequirements.builder()
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
            () -> FeatRequirements.builder()
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
            () -> FeatRequirements.builder().build()),

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
            () -> FeatRequirements.builder()
                    .requiredFeat(ESQUIVA)
                    .build()),

    /**
     * "Você adquire 1 ponto permanente de Iniciativa e então, se sua Iniciativa se tornar 4 ou
     * mais, você adquire uma Vantagem de Iniciativa."
     */
    // The permanent point is real, through Feat#resolveEgoBonus (Character#getEffectiveEgoTotal).
    // TODO: granting a Vantagem de Ego is not expressible — a Vantagem is chosen once at
    //  character creation (CharacterCreationService), never awarded later.
    INICIATIVA_APRIMORADA(
            "Você adquire 1 ponto permanente de Iniciativa e então, se sua Iniciativa se tornar 4 "
                    + "ou mais, você adquire uma Vantagem de Iniciativa.",
            () -> FeatRequirements.builder().build()) {
        @Override
        public int resolveEgoBonus(final EgoDomain domain, final Character character) {
            return domain == EgoDomain.INICIATIVA ? 1 : 0;
        }
    },

    /**
     * "Sempre que ganhar uma rolagem de Iniciativa, nas 2 primeiras Rodadas de cada Cena de
     * Combate você adquire +2PA (não cumulativo)."
     *
     * <p><b>Real</b>: a {@code Blessing} of {@code ModifierType.ACTION_POINTS} (+2, 2 Rodadas,
     * self) from {@link Feat#resolveInitiativeBlessings}, resolved by {@code
     * InitiativeBlessingService} and granted by {@code Scene#applyInitiativeBlessings} to whoever
     * won. "Não cumulativo" is the sourced Blessing's own rule: a second grant from the same
     * Talento renews rather than stacks.
     */
    LIDERAR_O_AVANCO(
            "Sempre que ganhar uma rolagem de Iniciativa, nas 2 primeiras Rodadas de cada Cena de "
                    + "Combate você adquire +2PA (não cumulativo).",
            () -> FeatRequirements.builder()
                    .requiredFeat(INICIATIVA_APRIMORADA)
                    .build()) {
        @Override
        public List<Blessing> resolveInitiativeBlessings() {
            return List.of(new Blessing(ModifierType.ACTION_POINTS, LIDERAR_O_AVANCO_ACTION_POINTS,
                    LIDERAR_O_AVANCO_ROUNDS, TargetScope.SELF, name()));
        }
    },

    /**
     * "Você pode adicionar Metade de seu valor de Destreza ou do valor de Carisma, a sua escolha,
     * às suas rolagens de Iniciativa."
     */
    // The Iniciativa half is real, through Feat#resolveInitiativeBonus. ⚠️ "a sua escolha" is read
    // as the higher of half Destreza and half Carisma: the choice is made per roll, and nothing
    // would pick the lower one.
    // TODO: the PA and Movimento half needs "apenas se você for o primeiro a agir" (turn-order
    //  position, which Scene resolves for nobody), a grant to allies at initiative time that
    //  depends on the holder's position, and direction-scoped movement ("em direção a").
    // The disjunctive Pré-requisito is real — "Destreza 3 *ou* Carisma 3"; the required Talento
    // is common to both branches, so it stays on the outer group.
    PORTA_ESTANDARTE(
            "Você pode adicionar Metade de seu valor de Destreza ou do valor de Carisma, a sua "
                    + "escolha, às suas rolagens de Iniciativa. Nas 2 primeiras Rodadas do "
                    + "combate, apenas se você for o primeiro a agir, você e seus aliados recebem "
                    + "Pontos de Ação adicionais, sendo +2PA na primeira Rodada e +1PA na segunda. "
                    + "Nestas Rodadas iniciais seu Movimento Base aumenta em +2UD sempre que se "
                    + "mover em direção a um inimigo e o Movimento Base de seus aliados aumentam "
                    + "em +2UD para se moverem em sua direção.",
            () -> FeatRequirements.builder()
                    .requiredFeat(LIDERAR_O_AVANCO)
                    .alternative(FeatRequirements.builder()
                            .attributeDomain(AttributeDomain.DEXTERITY)
                            .requiredAttributeValue(3)
                            .build())
                    .alternative(FeatRequirements.builder()
                            .attributeDomain(AttributeDomain.CHARISMA)
                            .requiredAttributeValue(3)
                            .build())
                    .build()) {
        @Override
        public int resolveInitiativeBonus(final Character character) {
            return Math.max(character.getEffectiveAttributeTotal(AttributeDomain.DEXTERITY),
                    character.getEffectiveAttributeTotal(AttributeDomain.CHARISMA)) / 2;
        }
    },

    /**
     * "Você pode se mover enquanto furtivo, mas seu Movimento Base é reduzido à metade", the
     * halving falling away at 7 Graduações em Furtividade.
     *
     * <p><b>The permission is real</b> — through {@link Feat#movesWhileHidden}, which {@code
     * HidingService#reveals} asks before letting {@code RevealTrigger#MOVEMENT} end a
     * concealment. Its holder moves and stays Escondido; everyone else's movement gives them away.
     *
     * <p>The <b>price</b> is not. Halving Movimento Base is the gap catalog's "Multiplicative
     * stages" row — {@code MovementService} sums additively with no halving stage, and a {@code
     * MOVEMENT_HALVED} {@code ModifierType} is explicitly not the fix. So this Talento currently
     * grants its benefit and charges nothing for it, which over-grants; the 7-Graduação clause
     * that <i>removes</i> the price is the half that is accidentally correct.
     */
    // TODO: the "reduzido à metade" price needs the multiplicative-stage mechanism
    //  (MovementService sums additively). Gating it would additionally need "enquanto furtivo" to
    //  be visible from a movement hook — Feat#resolveMovementIncrease takes a Character, and the
    //  Condição lives on the CombatantSheet; Feat#movesWhileHidden is asked by HidingService,
    //  which holds the sheet, but resolveMovementIncrease has no such overload.
    MOVIMENTO_FURTIVO(
            "Você pode se mover enquanto furtivo, mas seu Movimento Base é reduzido à metade. Se "
                    + "você possuir 7 ou mais Graduações em Furtividade este Talento não mais "
                    + "reduz seu Movimento Base.",
            () -> FeatRequirements.builder()
                    .requiredSkillType(SkillType.FURTIVIDADE)
                    .requiredSkillGraduation(2)
                    .build()) {
        @Override
        public boolean movesWhileHidden(final Character character) {
            return true;
        }
    },

    /**
     * "Rolagens de Perícia de Ataque realizadas imediatamente após ser bem-sucedido em rolagens de
     * Atletismo para Natação tem o Tempo de Ação reduzido em -1PA."
     */
    /**
     * <p><b>The second clause is real</b> — "o Tempo de Ação de investidas sempre reduzidos em
     * -1PA", through {@link Feat#resolveChargeActionPointReduction}, which {@code
     * ChargeService#getActionPointCost} subtracts from its 3PA baseline.
     *
     * <p>⚠️ <b>Granted unconditionally, which over-grants.</b> The clause is gated on the holder
     * possessing a <i>Movimento Base de Natação</i>, a sub-stat this core deliberately does not
     * model (see {@code AtletismoCompetencyAbility#ANFIBIO}) — so there is nothing to test and the
     * reduction applies to every Investida. The alternative was to grant nothing at all, which
     * would leave the Talento's one expressible clause inert; narrowing it correctly is what the
     * sub-stat would buy.
     */
    // TODO: the first clause — "rolagens de Perícia de Ataque realizadas imediatamente após ser
    //  bem-sucedido em rolagens de Atletismo para Natação" — needs two things this core lacks:
    //  it does not track what a roll was *for*, so "Atletismo para Natação" cannot be
    //  distinguished from any other Atletismo roll, and no roll's outcome feeds the next one's
    //  cost.
    // TODO: Movimento Base de Natação is a separate sub-stat deliberately not wired to
    //  ModifierType.MOVEMENT (see AtletismoCompetencyAbility#ANFIBIO) — it is what would narrow
    //  the second clause to the swimmers it is written for. See the javadoc above.
    // The Especialização Triatleta Pré-requisito is real — FeatRequirements#requiredSkillTraits.
    INVESTIDA_AQUATICA(
            "Em combate, rolagens de Perícia de Ataque realizadas imediatamente após ser "
                    + "bem-sucedido em rolagens de Atletismo para Natação tem o Tempo de Ação "
                    + "reduzido em -1PA. Personagens que possuam Movimento Base de Natação tem o "
                    + "Tempo de Ação de investidas sempre reduzidos em -1PA.",
            () -> FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATLETISMO)
                    .requiredSkillGraduation(4)
                    .requiredSkillTrait(AtletismoSpecialization.TRI_ATLETA)
                    .build()) {
        @Override
        public int resolveChargeActionPointReduction(final Character character) {
            return CHARGE_ACTION_POINT_REDUCTION;
        }
    },

    /**
     * "Você pode fazer uma acrobacia para se mover de forma segura pelo cenário, sem provocar
     * Reações", up to half Destreza in UD plus 2UD per Título.
     *
     * <p>The source prints "3 Talentos de Mobilidade" under {@code Descrição} with the
     * Pré-requisito line left empty; read as the Pré-requisito.
     */
    // TODO: the Movimento Acrobático is its own manoeuvre (Metade da Destreza UD, +2UD per Título),
    //  gated on being able to Reposicionar (RepositionService#canReposition) — no Manoeuvre
    //  constant or allowance for it yet. "Sem provocar Reações" still exempts from nothing that
    //  happens: nothing fires a Reação.
    MOVIMENTO_ACROBATICO(
            "Apenas uma vez por Rodada e apenas quando puder fazer uma Ação de Reposicionar, você "
                    + "pode fazer uma acrobacia para se mover de forma segura pelo cenário, sem "
                    + "provocar Reações. A Distância que seu Movimento Acrobático pode percorrer é "
                    + "igual à Metade da Destreza UD, a distância máxima aumenta em +2UD para cada "
                    + "Título Aventyr que você possuir.",
            () -> FeatRequirements.builder()
                    .requiredFeatCategory(FeatCategory.MOBILIDADE)
                    .requiredFeatCategoryCount(3)
                    .build()),

    /**
     * "Suas Investidas recebem Área de Efeito – Explosão. Durante o movimento da investida você
     * recebe Redução de Danos Sofridos igual ao número de Títulos Aventyrs que você possuir."
     *
     * <p><b>The second clause is computed for real</b> — {@link
     * Feat#resolveChargeMovementDamageReduction}, one point of RD per Título Aventyr held (a Título
     * is "Desperto" simply by being held, per {@code InstinctAbility#SENTIR_A_INTENCAO}'s own
     * reading of that phrase). <b>Reported, not applied</b>: "durante o movimento da investida" is
     * a window shorter than a Rodada, the shortest a {@code TemporaryBonus} can last, so it rides
     * {@code ChargeResult#unappliedMovementDamageReduction} rather than being granted onto the
     * sheet where it would outlive the charge.
     */
    // TODO: the Área de Efeito clause needs the footprint resolution the gap catalog's "Area de
    //  Efeito" row records as missing — an Investida is a modelled manoeuvre now
    //  (ChargeService), but nothing turns an AreaOfEffect into a set of targets.
    INVESTIDA_SELVAGEM(
            "Suas Investidas recebem Área de Efeito – Explosão. Durante o movimento da investida "
                    + "você recebe Redução de Danos Sofridos igual ao número de Títulos Aventyrs "
                    + "que você possuir.",
            () -> FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(7)
                    .requiredFeatCategory(FeatCategory.MOBILIDADE)
                    .requiredFeatCategoryCount(3)
                    .build()) {
        @Override
        public int resolveChargeMovementDamageReduction(final Character character) {
            return character.getAllTitles().size();
        }
    },

    /**
     * "Se em sua Investida Selvagem você causar danos à 3 ou mais inimigos você recebe Bônus em
     * Defesas igual à 1 + quantidade de Títulos Aventyr Bruto Despertos."
     */
    // TODO: builds on INVESTIDA_SELVAGEM's Área de Efeito half, which is still unbuilt, and
    //  additionally needs the count of targets an area attack actually damaged. The Investida
    //  itself is modelled now (ChargeService); the explosion it is measured over is not.
    INVESTIDA_SELVAGEM_SOLAR(
            "Se em sua Investida Selvagem você causar danos à 3 ou mais inimigos você recebe Bônus "
                    + "em Defesas igual à 1 + quantidade de Títulos Aventyr Bruto Despertos, "
                    + "Duração em Rodadas igual ao número de inimigos atingidos pela explosão da "
                    + "investida. Os Bônus em Defesas aumentam em +1 por alvo se utilizar um item "
                    + "do tipo Escudo de peso Médio ou Superior. Um mesmo personagem não pode "
                    + "possuir Investida Selvagem Lunar e Investida Selvagem Solar.",
            () -> FeatRequirements.builder()
                    .requiredFeat(INVESTIDA_SELVAGEM)
                    .forbiddenFeat(investidaSelvagemLunar())
                    .requiredAwakenedTitles(1)
                    .requiredTitleArchetype(org.aventyrs.core.title.TitleArchetype.BRUTO)
                    .build()),

    /**
     * "Se em sua Investida Selvagem você causar danos à 3 ou mais inimigos, você recebe Roubo de
     * Vida igual à 1 + quantidade de Títulos Aventyr Bruto Despertos."
     */
    // TODO: same blocker as INVESTIDA_SELVAGEM_SOLAR — the Área de Efeito half of
    //  INVESTIDA_SELVAGEM, and the count of enemies it damaged. Additionally, nothing scopes a
    //  Roubo de Vida grant to a Duração in Rodadas: Feat#resolveGrantedLifeSteal is permanent.
    INVESTIDA_SELVAGEM_LUNAR(
            "Se em sua Investida Selvagem você causar danos à 3 ou mais inimigos, você recebe "
                    + "Roubo de Vida igual à 1 + quantidade de Títulos Aventyr Bruto Despertos, "
                    + "Duração em Rodadas igual ao número de inimigos atingidos pela explosão da "
                    + "investida. O valor do Roubo de Vida aumenta em +1 se utilizar uma arma de "
                    + "peso Médio ou Superior. Um mesmo personagem não pode possuir Investida "
                    + "Selvagem Lunar e Investida Selvagem Solar.",
            () -> FeatRequirements.builder()
                    .requiredFeat(INVESTIDA_SELVAGEM)
                    .forbiddenFeat(MobilidadeFeat.INVESTIDA_SELVAGEM_SOLAR)
                    .requiredAwakenedTitles(1)
                    .requiredTitleArchetype(org.aventyrs.core.title.TitleArchetype.BRUTO)
                    .build());

    /**
     * {@link #INVESTIDA_SELVAGEM_LUNAR}, reached through a method rather than named directly: Java forbids
     * referencing a <em>later</em> enum constant from an earlier constant's constructor arguments,
     * and a {@link Supplier} does not lift that — the restriction is on the reference, not on when
     * it is evaluated. A static method body is not an initializer, so the forward reference is
     * legal here. Only the forward half of each mutually-exclusive pair needs one; the constant
     * declared second names its twin directly.
     */
    private static Feat investidaSelvagemLunar() {
        return INVESTIDA_SELVAGEM_LUNAR;
    }

    /** INVESTIDA_AQUATICA's own stated "-1PA" on the Tempo de Ação of an Investida. */
    private static final int CHARGE_ACTION_POINT_REDUCTION = 1;

    /** LIDERAR_O_AVANCO's "+2PA". */
    private static final int LIDERAR_O_AVANCO_ACTION_POINTS = 2;

    /** LIDERAR_O_AVANCO's "nas 2 primeiras Rodadas". */
    private static final int LIDERAR_O_AVANCO_ROUNDS = 2;

    private final String description;
    /**
     * Held as a {@link Supplier} rather than a plain field because this tree's mutually-exclusive
     * Talentos name each <em>other</em> as a {@code forbiddenFeat}, and Java forbids referencing
     * an enum constant from another constant's constructor arguments. Deferring construction to
     * the first {@link #getFeatRequirements()} call sidesteps that, the same way {@code
     * MetamagicoFeat} already does for its own sibling {@code requiredFeat} chain.
     */
    private final Supplier<FeatRequirements> featRequirements;

    MobilidadeFeat(final String description, final Supplier<FeatRequirements> featRequirements) {
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
        return featRequirements.get();
    }
}
