package org.aventyrs.core.feat;

import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.character.services.MovementServiceImpl;
import org.aventyrs.core.character.MovementMode;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.atletismo.AtletismoSpecialization;

import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Talentos de Mobilidade — moving further, moving first, and attacking around a move.
 *
 * <p><b>Every constant is wired</b> (core 0.1.7.17–0.1.7.22; plan and table rulings in {@code
 * docs/plans/mobilidade-wiring-plan.md}). What the tree needed, beside the Movimento Base hooks it always had:
 * <ul>
 *   <li>a per-Turn movement ledger on the sheet — {@code CombatantSheet#getDistanceMovedThisTurn()}, {@code
 *   #hasMovedThisTurn()} and {@code #getMovementsAtLatestAction()} — fed by the caller, since this core does no
 *   geometry (Esquiva, Se Mover e Atacar);</li>
 *   <li>an attack marked as a Reação ({@code IncomingAttack}/{@code DeliveredAttack#isReaction()}) and {@link
 *   Feat#resolveReactionDefenseBonus} (Esquiva);</li>
 *   <li>a Nadar roll recorded with its manoeuvre ({@code CombatantAction#manoeuvre()}, Investida Aquática);</li>
 *   <li>a live-state halving stage, {@link Feat#halvesCurrentMovementBase} (Movimento Furtivo);</li>
 *   <li>{@code Manoeuvre#MOVIMENTO_ACROBATICO} through {@code RepositionService#beginAcrobatic};</li>
 *   <li>first-to-act Blessings and heading-scoped movement on the {@code Scene} — {@code
 *   Scene#applyFirstToActBlessings}, {@code Scene#getHeadingMovementBonus} over a caller's {@code
 *   MovementHeading} (Porta-Estandarte) — and {@code Scene#applyInitiativeBlessings(Function)} for every winner at
 *   once (Liderar o Avanço);</li>
 *   <li>{@code ChargeService#openMovementWard}/{@code #applySavageAftermath} (Investida Selvagem, Solar, Lunar).</li>
 * </ul>
 * Movement still provokes nothing by itself here: a Reação is fired by the API's Reação window, and a caller says
 * which attacks are Reações.
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
     * "Você recebe Bônus de +2 em suas Defesas para resistir à Reações de seus inimigos." One of the five Talentos in
     * the catalog with no Pré-requisito at all.
     *
     * <p><b>Both clauses are real</b> (core 0.1.7.18). The +2 is {@link #resolveReactionDefenseBonus}, read against
     * an attack the caller marks as a Reação ({@code IncomingAttack#isReaction()}/{@code DeliveredAttack#isReaction()}
     * — Defender o Perímetro, Impedir Conjuração, a fired Preparação, a Título reaction attack). "Se você se moveu em
     * seu último Turno … +1 … para resistir à ataques sofridos fora de seu Turno por 1 Rodada" is a turn-end Blessing,
     * the mirror of {@code EscudeiroFeat#DEFESA_TARTARUGA}: granted as the Turn ends when the holder moved at all
     * ({@code CombatantSheet#hasMovedThisTurn()}), lasting until their next Turn begins — which is exactly the span
     * "fora de seu Turno" names (table reading 2026-10-17).
     */
    ESQUIVA(
            "Você recebe Bônus de +2 em suas Defesas para resistir à Reações de seus inimigos. Se "
                    + "você se moveu em seu último Turno, então você recebe Bônus de +1 em suas "
                    + "Defesas para resistir à ataques sofridos fora de seu Turno por 1 Rodada.",
            () -> FeatRequirements.builder().build()) {
        @Override
        public int resolveReactionDefenseBonus(final DefenseType defenseType, final CombatantSheet holder) {
            return ESQUIVA_REACTION_DEFENSE_BONUS;
        }

        @Override
        public List<Blessing> resolveTurnEndBlessings(final CombatantSheet holder) {
            if (!holder.hasMovedThisTurn()) {
                return List.of();
            }
            return List.of(new Blessing(ModifierType.DEFESAS, ESQUIVA_MOVED_DEFENSE_BONUS, 1, TargetScope.SELF, name())
                    .countingDownAtTurnStart());
        }
    },

    /**
     * "Você recebe Vantagem em Rolagens de Perícias de Ataque feitas após se mover por uma Distância Curta ou
     * superior. Seu Movimento Base aumenta em +2UD para se mover imediatamente após efetuar uma Rolagem de Perícia de
     * Ataque."
     *
     * <p>The source's own entry is defective here: its {@code Descrição} line reads "Talento ‘Esquiva’", which is
     * plainly a Pré-requisito printed under the wrong heading. Modelled as that Pré-requisito, with the remaining
     * lines as the description.
     *
     * <p><b>Both halves are real</b> (core 0.1.7.19; table ruling 2026-10-17). The Vantagem lands on every attack
     * roll once the holder has travelled {@value #SE_MOVER_E_ATACAR_DISTANCE}UD or more this Turn ({@code
     * CombatantSheet#getDistanceMovedThisTurn()} — where Distância Curta begins), whatever kind of movement got them
     * there. The +2UD is per Ponto de Ação of the first movement bought after the Turn's latest action, when that
     * action was an attack roll ({@code CombatantSheet#getMovementsAtLatestAction()}).
     */
    SE_MOVER_E_ATACAR(
            "Você recebe Vantagem em Rolagens de Perícias de Ataque feitas após se mover por uma "
                    + "Distância Curta ou superior. Seu Movimento Base aumenta em +2UD para se "
                    + "mover imediatamente após efetuar uma Rolagem de Perícia de Ataque.",
            () -> FeatRequirements.builder()
                    .requiredFeat(ESQUIVA)
                    .build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                         final SkillTrait requestedAbility, final Character character,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return holder != null && skillType.isAttackSkill()
                    && holder.getDistanceMovedThisTurn() >= SE_MOVER_E_ATACAR_DISTANCE
                    ? Skill.ADVANTAGE_BONUS : 0;
        }

        @Override
        public int resolveRoundMovementIncrease(final int movementIndex, final Character character,
                                                final CombatantSheet holder) {
            if (holder == null || movementIndex != holder.getMovementsAtLatestAction()) {
                return 0;
            }
            List<CombatantAction> turn = holder.getActionsThisTurn();
            CombatantAction latest = turn.isEmpty() ? null : turn.get(turn.size() - 1);
            return latest != null && latest.skill() != null && latest.skill().isAttackSkill()
                    ? SE_MOVER_E_ATACAR_MOVEMENT : 0;
        }
    },

    /**
     * "Você adquire 1 ponto permanente de Iniciativa e então, se sua Iniciativa se tornar 4 ou mais, você adquire uma
     * Vantagem de Iniciativa."
     *
     * <p><b>Fully real.</b> The permanent point is {@link Feat#resolveEgoBonus} ({@code
     * Character#getEffectiveEgoTotal}). The Vantagem (core 0.1.7.21) is picked at acquisition (table ruling
     * 2026-10-17) and lives on {@link VantagemDeIniciativaAprimoradaFeat}, which grants it through {@link
     * Feat#getGrantedEgoAdvantages} while the Iniciativa total is 4 or more; so the bare constant declares the
     * choice and is refused by {@code FeatService#grantFeat}.
     */
    INICIATIVA_APRIMORADA(
            "Você adquire 1 ponto permanente de Iniciativa e então, se sua Iniciativa se tornar 4 "
                    + "ou mais, você adquire uma Vantagem de Iniciativa.",
            () -> FeatRequirements.builder().build()) {
        @Override
        public int resolveEgoBonus(final EgoDomain domain, final Character character) {
            return domain == EgoDomain.INICIATIVA ? 1 : 0;
        }

        @Override
        public List<FeatChoice<?>> resolveRequiredChoices(final Character holder) {
            return List.of(FeatChoice.ofOne(org.aventyrs.core.ego.EgoAdvantage.class,
                    VantagemDeIniciativaAprimoradaFeat.optionsFor(holder)));
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
    // Fully real. The Iniciativa half is Feat#resolveInitiativeBonus — ⚠️ "a sua escolha" is read as the higher of half
    // Destreza and half Carisma: the choice is made per roll, and nothing would pick the lower one. The PA half (core
    // 0.1.7.21) is Feat#resolveFirstToActBlessings, granted by Scene#applyFirstToActBlessings while the holder holds
    // the first turn-order slot (table ruling 2026-10-17): +1PA for 2 Rodadas and +1PA for 1, so "+2PA na primeira
    // Rodada e +1PA na segunda", to the holder and their allies. The Movimento half is the two approach hooks, read
    // by Scene#getHeadingMovementBonus off a caller's MovementHeading ("em direção a" = ends nearer), in core Rodadas
    // 0 and 1 — the table's first two, the same count MOVIMENTO_RAPIDO's Rodadas Pares reads.
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

        @Override
        public List<Blessing> resolveFirstToActBlessings(final CombatantSheet holder) {
            return List.of(
                    new Blessing(ModifierType.ACTION_POINTS, 1, PORTA_ESTANDARTE_ROUNDS, TargetScope.SELF_AND_ALLIES,
                            name()),
                    new Blessing(ModifierType.ACTION_POINTS, 1, 1, TargetScope.SELF_AND_ALLIES,
                            name() + PORTA_ESTANDARTE_FIRST_ROUND_SOURCE));
        }

        @Override
        public int resolveApproachMovementIncrease(final CombatantSheet holder, final int currentRound,
                                                   final boolean firstToAct) {
            return firstToAct && currentRound < PORTA_ESTANDARTE_ROUNDS ? PORTA_ESTANDARTE_MOVEMENT : 0;
        }

        @Override
        public int resolveAllyApproachMovementIncrease(final CombatantSheet holder, final int currentRound,
                                                       final boolean firstToAct) {
            return resolveApproachMovementIncrease(holder, currentRound, firstToAct);
        }
    },

    /**
     * "Você pode se mover enquanto furtivo, mas seu Movimento Base é reduzido à metade", the halving falling away at 7
     * Graduações em Furtividade.
     *
     * <p><b>Fully real.</b> The permission is {@link Feat#movesWhileHidden}, which {@code HidingService#reveals} asks
     * before letting {@code RevealTrigger#MOVEMENT} end a concealment: its holder moves and stays Escondido. The
     * price (core 0.1.7.20) is {@link Feat#halvesCurrentMovementBase}: while the holder is Escondido ({@code
     * CombatantSheet#getHidden()}) and has fewer than {@value #FURTIVO_FREE_GRADUATION} Graduações em Furtividade,
     * {@code MovementService}'s sheet path halves the Movimento Base in its last stage.
     */
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

        @Override
        public boolean halvesCurrentMovementBase(final Character character, final CombatantSheet holder) {
            return holder != null && holder.getHidden().isPresent()
                    && character.getEffectiveGraduation(SkillType.FURTIVIDADE) < FURTIVO_FREE_GRADUATION;
        }
    },

    /**
     * "Em combate, rolagens de Perícia de Ataque realizadas imediatamente após ser bem-sucedido em rolagens de
     * Atletismo para Natação tem o Tempo de Ação reduzido em -1PA. Personagens que possuam Movimento Base de Natação
     * tem o Tempo de Ação de investidas sempre reduzidos em -1PA."
     *
     * <p><b>Both clauses are real.</b> The first (core 0.1.7.19) is {@link #resolveAttackActionPointAdjustment}: -1PA
     * when the Turn's latest recorded action is a successful Atletismo roll for {@code Manoeuvre#NADAR} ({@code
     * CombatantAction#succeededAt}) — the Nadar manoeuvre is the "rolagem de Atletismo para Natação", and the caller
     * records it with its manoeuvre. {@code ActionPointsService#getAttackCost} keeps the 1PA floor.
     *
     * <p>The second — "o Tempo de Ação de investidas sempre reduzidos em -1PA" — is {@link
     * Feat#resolveChargeActionPointReduction}, which {@code ChargeService#getActionPointCost} subtracts from its 3PA
     * baseline, only for a holder who possesses a Movimento Base de Natação ({@code
     * MovementService#hasMovementMode(Character, MovementMode)}, Forma-blind since this hook has no sheet).
     *
     * <p>The Especialização Triatleta Pré-requisito is real — {@code FeatRequirements#requiredSkillTraits}.
     */
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
            return new MovementServiceImpl().hasMovementMode(character, MovementMode.SWIM)
                    ? CHARGE_ACTION_POINT_REDUCTION : 0;
        }

        @Override
        public int resolveAttackActionPointAdjustment(final SkillType skillType, final AttackSource attackSource,
                                                      final CombatantSheet attacker, final Set<Feat> activatedFeats) {
            List<CombatantAction> turn = attacker.getActionsThisTurn();
            return !turn.isEmpty() && turn.get(turn.size() - 1).succeededAt(Manoeuvre.NADAR)
                    ? -SWIM_ATTACK_ACTION_POINT_REDUCTION : 0;
        }
    },

    /**
     * "Você pode fazer uma acrobacia para se mover de forma segura pelo cenário, sem provocar Reações", up to half
     * Destreza in UD plus 2UD per Título.
     *
     * <p>The source prints "3 Talentos de Mobilidade" under {@code Descrição} with the Pré-requisito line left empty;
     * read as the Pré-requisito.
     *
     * <p><b>Real</b> (core 0.1.7.20) as its own manoeuvre, {@code Manoeuvre#MOVIMENTO_ACROBATICO}, declared through
     * {@code RepositionService#beginAcrobatic}. Table ruling 2026-10-17: it is taken <i>in place of</i> a Reposicionar
     * — the same Ação Livre, refused whenever a Reposicionar would be, and excluding Pontos de Ação movement the same
     * way — once per Rodada, reaching {@code RepositionService#getAcrobaticDistance} (half the Destreza total plus
     * 2UD per Título held), and exempt from movement Reações in {@code MovementReactionService}.
     */
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
     * "Suas Investidas recebem Área de Efeito – Explosão. Durante o movimento da investida você recebe Redução de
     * Danos Sofridos igual ao número de Títulos Aventyrs que você possuir."
     *
     * <p><b>Fully real.</b> The Área de Efeito clause: an attack whose SkillRoll carries {@code Manoeuvre.INVESTIDA} is
     * an {@code AreaOfEffect#ATTACK_EXPLOSION} ({@link Feat#resolveAttackArea}), which {@code AttackDelivery} accepts
     * on a {@code DeliveredAttack} naming the footprint's occupants. The RD clause is {@link
     * Feat#resolveChargeMovementDamageReduction}, one RDS instance per Título Aventyr held (a Título is "Desperto"
     * simply by being held, per {@code InstinctAbility#SENTIR_A_INTENCAO}'s reading). Since core 0.1.7.22 it is
     * <b>applied</b>: {@code ChargeService#openMovementWard} grants it, open-ended, as the charge's movement begins and
     * {@code #closeMovementWard} ends it at the attack, so a Defender o Perímetro struck along the route meets it.
     */
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
        public AreaOfEffect resolveAttackArea(final Character attacker, final SkillType attackSkill,
                                              final AttackSource attackSource, final SkillRoll skillRoll) {
            return skillRoll != null && skillRoll.getManoeuvre() == Manoeuvre.INVESTIDA
                    ? AreaOfEffect.ATTACK_EXPLOSION : null;
        }

        @Override
        public int resolveChargeMovementDamageReduction(final Character character) {
            return character.getAllTitles().size();
        }
    },

    /**
     * "Se em sua Investida Selvagem você causar danos à 3 ou mais inimigos você recebe Bônus em Defesas igual à 1 +
     * quantidade de Títulos Aventyr Bruto Despertos, Duração em Rodadas igual ao número de inimigos atingidos pela
     * explosão da investida. Os Bônus em Defesas aumentam em +1 por alvo se utilizar um item do tipo Escudo de peso
     * Médio ou Superior."
     *
     * <p><b>Real</b> (core 0.1.7.22) through {@link Feat#resolveChargeAftermathEffects}, applied by {@code
     * ChargeService#applySavageAftermath} once the caller has counted the explosion's enemy targets and those who
     * actually lost PV. A sourced Defesas bonus, so a second charge renews it. "Títulos Bruto Despertos" are held
     * Títulos of {@code TitleArchetype#BRUTO}; the Escudo is an equipped {@code ItemCategory#SHIELD} of authored
     * weight {@code MEDIUM}/{@code HEAVY}.
     */
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
                    .build()) {
        @Override
        public List<org.aventyrs.core.sheet.TemporaryEffect> resolveChargeAftermathEffects(
                final CombatantSheet holder, final org.aventyrs.core.item.Weapon chargeWeapon, final int enemiesHit,
                final int enemiesDamaged) {
            if (enemiesDamaged < SAVAGE_AFTERMATH_MINIMUM_DAMAGED || enemiesHit <= 0) {
                return List.of();
            }
            int bonus = 1 + bruteTitles(holder.getCharacter()) + (holdsMediumShield(holder.getCharacter()) ? enemiesHit : 0);
            return List.of(new org.aventyrs.core.sheet.TemporaryBonus(ModifierType.DEFESAS, bonus, enemiesHit, name()));
        }
    },

    /**
     * "Se em sua Investida Selvagem você causar danos à 3 ou mais inimigos, você recebe Roubo de Vida igual à 1 +
     * quantidade de Títulos Aventyr Bruto Despertos, Duração em Rodadas igual ao número de inimigos atingidos pela
     * explosão da investida. O valor do Roubo de Vida aumenta em +1 se utilizar uma arma de peso Médio ou Superior."
     *
     * <p><b>Real</b> (core 0.1.7.22), the Solar twin: a sourced {@code sheet.LifeSteal} lasting that many Rodadas,
     * through {@link Feat#resolveChargeAftermathEffects} and {@code ChargeService#applySavageAftermath}. "Uma arma de
     * peso Médio ou Superior" is the weapon the Investida was made with.
     */
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
                    .build()) {
        @Override
        public List<org.aventyrs.core.sheet.TemporaryEffect> resolveChargeAftermathEffects(
                final CombatantSheet holder, final org.aventyrs.core.item.Weapon chargeWeapon, final int enemiesHit,
                final int enemiesDamaged) {
            if (enemiesDamaged < SAVAGE_AFTERMATH_MINIMUM_DAMAGED || enemiesHit <= 0) {
                return List.of();
            }
            int steal = 1 + bruteTitles(holder.getCharacter()) + (isMediumOrHeavier(chargeWeapon) ? 1 : 0);
            return List.of(new org.aventyrs.core.sheet.LifeSteal(steal, java.util.Optional.of(enemiesHit), name()));
        }
    };

    /** Solar/Lunar's "se … você causar danos à 3 ou mais inimigos". */
    private static final int SAVAGE_AFTERMATH_MINIMUM_DAMAGED = 3;

    /** "Títulos Aventyr Bruto Despertos": held Títulos of the Bruto archetype. */
    private static int bruteTitles(final Character character) {
        return (int) character.getAllTitles().stream()
                .filter(title -> title.getArchetype() == org.aventyrs.core.title.TitleArchetype.BRUTO)
                .count();
    }

    /** "Se utilizar um item do tipo Escudo de peso Médio ou Superior" — an equipped Escudo, by authored weight. */
    private static boolean holdsMediumShield(final Character character) {
        return character.getEquipment().stream()
                .filter(item -> item.getCategory() == org.aventyrs.core.item.ItemCategory.SHIELD)
                .anyMatch(MobilidadeFeat::isMediumOrHeavier);
    }

    private static boolean isMediumOrHeavier(final org.aventyrs.core.item.Item item) {
        return item != null && item.getWeightClass() != null
                && item.getWeightClass() != org.aventyrs.core.item.ItemWeightClass.LIGHT;
    }

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

    /** ESQUIVA's "+2 em suas Defesas para resistir à Reações". */
    private static final int ESQUIVA_REACTION_DEFENSE_BONUS = 2;

    /** ESQUIVA's "+1 em suas Defesas" after moving. */
    private static final int ESQUIVA_MOVED_DEFENSE_BONUS = 1;

    /** SE_MOVER_E_ATACAR's "Distância Curta ou superior": the band starts past Muito Curta's 2UD. */
    private static final int SE_MOVER_E_ATACAR_DISTANCE = 3;

    /** SE_MOVER_E_ATACAR's "+2UD". */
    private static final int SE_MOVER_E_ATACAR_MOVEMENT = 2;

    /** MOVIMENTO_FURTIVO: "Se você possuir 7 ou mais Graduações em Furtividade este Talento não mais reduz". */
    private static final int FURTIVO_FREE_GRADUATION = 7;

    /** INVESTIDA_AQUATICA's "-1PA" on an attack right after a successful Natação roll. */
    private static final int SWIM_ATTACK_ACTION_POINT_REDUCTION = 1;

    /** INVESTIDA_AQUATICA's own stated "-1PA" on the Tempo de Ação of an Investida. */
    private static final int CHARGE_ACTION_POINT_REDUCTION = 1;

    /** PORTA_ESTANDARTE's "Nas 2 primeiras Rodadas do combate". */
    private static final int PORTA_ESTANDARTE_ROUNDS = 2;

    /** PORTA_ESTANDARTE's "+2UD". */
    private static final int PORTA_ESTANDARTE_MOVEMENT = 2;

    /** Keeps the first Rodada's extra +1PA apart from the two-Rodada one — a sourced Blessing replaces its twin. */
    private static final String PORTA_ESTANDARTE_FIRST_ROUND_SOURCE = ":PRIMEIRA_RODADA";

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
