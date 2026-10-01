package org.aventyrs.core.feat;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.scene.InitiativePosition;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillTraitKind;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.atletismo.AtletismoCompetencyAbility;
import org.aventyrs.core.skill.atletismo.AtletismoSpecialization;
import org.aventyrs.core.skill.attention.AttentionSpecialization;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Talentos de Perito — depth in a chosen Perícia rather than breadth.
 *
 * <p>Almost the whole tree hangs off {@link #FOCO_EM_PERICIA}'s acquisition-time choice of a
 * Perícia. {@link FocoEmPericiaFeat} is the acquired, choice-carrying form (granted in place of the
 * bare constant), and every "a Perícia escolhida" constant below reads it through {@link
 * FocoEmPericiaFeat#chosenBy}.
 *
 * <p><b>"Vantagem adicional" is another {@link Skill#ADVANTAGE_BONUS}</b> — table ruling
 * (2026-09-28). Vantagem is a flat +2 in this core, so a second one on a roll that already has one
 * is simply +2 more; {@link #DISCRETO}, {@link #EXIBICIONISTA} and {@link #TRABALHO_EM_EQUIPE} each
 * add theirs on top of Foco em Perícia's own.
 *
 * <p><b>Every constant is wired</b>, and the tree is what a handful of engine pieces were built for:
 * the neutral allegiance ({@code Scene#setAggressive}, {@code SceneContext#getNeutrals}), a Perícia
 * roll's price ({@code ActionPointsService#getSkillRollCost}), the roll-aware GD and reroll hooks,
 * {@code Scene#deferToLast}, the Reação/Ação Livre and Corrente-resistance hooks, the creation-time
 * branch of a Pré-requisito ({@link Feat#isEligibleAtCreation}) and the Rodada-bought states of
 * {@link PeritoActiveAbility}. Each constant says which, and what it still reads approximately.
 */
public enum PeritoFeat implements Feat {

    /**
     * "Escolha uma perícia, adquira vantagem nas rolagens da Perícia escolhida."
     *
     * <p><b>Real</b>, through {@link FocoEmPericiaFeat} — the acquired form carrying the choice, whose
     * Vantagem is a flat {@link Skill#ADVANTAGE_BONUS} on that Perícia. The Pré-requisito ("Treinamento
     * na Perícia escolhida, que não seja de ataque, ou 4 graduações se for uma Perícia de ataque") is
     * a fact about the choice, so it lives in three places that agree: the options offered are the
     * Perícias that qualify, this constant is eligible only while at least one does, and the acquired
     * form re-checks its own pick ({@link FocoEmPericiaFeat#qualifies}).
     */
    FOCO_EM_PERICIA(
            "Escolha uma perícia, adquira vantagem nas rolagens da Perícia escolhida.",
            FeatRequirements.builder().build()) {
        @Override
        public List<FeatChoice<?>> resolveRequiredChoices(final Character holder) {
            return List.of(FeatChoice.ofOne(SkillType.class, Arrays.stream(SkillType.values())
                    .filter(skill -> FocoEmPericiaFeat.qualifies(holder, skill))
                    .toList()));
        }

        @Override
        public boolean isEligible(final Character character, final CharacterSheet sheet) {
            return Feat.meetsRequirements(getFeatRequirements(), character, sheet)
                    && Arrays.stream(SkillType.values()).anyMatch(skill -> FocoEmPericiaFeat.qualifies(character, skill));
        }
    },

    /**
     * "Sempre que efetuar rolagens de uma Perícia que você tenha Foco você adquire Vantagem
     * adicional", provided no ally or neutral is within Distância Curta; "Se adicionalmente nenhum
     * inimigo puder vê-lo você também recebe Vantagem em rolagens de Dano".
     *
     * <p><b>Real.</b> Both conditions read the roller's {@link SceneContext} — its allies and its
     * neutrals ({@code Scene}'s aggression map) within {@link Range#DISTANCIA_CURTA}; with no Scene
     * nobody can tell who is near, so it grants nothing. "Nenhum inimigo puder vê-lo" is the holder
     * being {@code Escondido} from every enemy in the context: hidden, and detected by none of them.
     * The dano Vantagem reaches any dano roll while both hold — the clause names no Perícia for it.
     */
    DISCRETO(
            "Sempre que efetuar rolagens de uma Perícia que você tenha Foco você adquire Vantagem "
                    + "adicional. Este talento só pode ser usado se você não tiver aliados ou "
                    + "personagens neutros em Distâncias Curtas. Se adicionalmente nenhum inimigo "
                    + "puder vê-lo você também recebe Vantagem em rolagens de Dano.",
            FeatRequirements.builder()
                    .requiredFeat(FOCO_EM_PERICIA)
                    .build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return hasFocusIn(character, skillType) && isAlone(sceneContext) ? Skill.ADVANTAGE_BONUS : 0;
        }

        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType,
                                                         final SceneContext sceneContext,
                                                         final CombatantSheet attackTarget, final Character actor,
                                                         final AttackSource attackSource, final int targetCount,
                                                         final CombatantSheet holder) {
            return isAlone(sceneContext) && isUnseen(holder, sceneContext)
                    ? Optional.of(new DamageBonus(Skill.ADVANTAGE_BONUS, DamageType.FISICO))
                    : Optional.empty();
        }
    },

    /**
     * "Sempre que efetuar rolagens de uma Perícia que você tenha foco em frente a uma plateia você
     * recebe vantagem adicional. Considere plateia um grupo de 4 ou mais personagens inteligentes
     * neutros a cena."
     *
     * <p><b>Real</b>, off {@code SceneContext#getNeutrals()} — the Scene's participants neutral
     * towards the roller, at any distance ("a cena"), counting those that {@code
     * CombatantSheet#isIntelligent()} — every character, and a foe the Narrador marked (core 0.0.84).
     */
    EXIBICIONISTA(
            "Sempre que efetuar rolagens de uma Perícia que você tenha foco em frente a uma "
                    + "plateia você recebe vantagem adicional. Considere plateia um grupo de 4 ou "
                    + "mais personagens inteligentes neutros a cena.",
            FeatRequirements.builder()
                    .requiredFeat(FOCO_EM_PERICIA)
                    .build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return hasFocusIn(character, skillType) && hasAudience(sceneContext) ? Skill.ADVANTAGE_BONUS : 0;
        }
    },

    /**
     * "A primeira rolagem da Perícia escolhida que fizer em cada um de seus Turnos tem o Tempo de
     * Ação reduzido em -1PA."
     *
     * <p><b>Real</b>, through {@link Feat#resolveSkillRollActionPointAdjustment}: priced by {@code
     * ActionPointsService#getSkillRollCost} — and by {@code #getAttackCost} when the Foco is a Perícia
     * de Ataque — never below 1PA. "A primeira … em cada um de seus Turnos" is the holder's Turn log
     * ({@code CombatantSheet#getActionsThisTurn()}) holding no roll of that Perícia yet.
     */
    PERITO_VELOZ(
            "A primeira rolagem da Perícia escolhida que fizer em cada um de seus Turnos tem o "
                    + "Tempo de Ação reduzido em -1PA.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.GNOSE)
                    .requiredAttributeValue(3)
                    .requiredFeat(FOCO_EM_PERICIA)
                    .build()) {
        @Override
        public int resolveSkillRollActionPointAdjustment(final SkillType skillType, final CombatantSheet roller,
                                                         final Set<Feat> activatedFeats,
                                                         final SceneContext sceneContext) {
            boolean firstOfTurn = roller.getActionsThisTurn().stream()
                    .noneMatch(action -> action.skill() == skillType);
            return hasFocusIn(roller.getCharacter(), skillType) && firstOfTurn ? -PERITO_VELOZ_REDUCTION : 0;
        }
    },

    /**
     * "Na primeira Rodada de cada Cena de Combate você pode optar por utilizar a Perícia escolhida
     * como uma Ação Livre ou como uma Reação. Nas segundas Rodadas você poderá utilizar a Perícia
     * escolhida como uma Ação Livre. Estes benefícios são restritos a uma utilização por Rodada."
     *
     * <p><b>Real</b>, as an <b>activated</b> Talento: the roll names it in {@code
     * SkillRoll#getActivatedFeats()} and states its own price on {@code SkillRoll#getActionCost()} —
     * an {@link ActionCost#FREE_ACTION} in the first two Rodadas, or a {@link ActionCost#REACTION} in
     * the first. {@link #permitsActivation} refuses anything else, and a second use in the Rodada. The
     * Reação, when that is the price, is the caller's to spend ({@code CombatantSheet#spendReaction}),
     * as every Reação is. "Primeira"/"segunda" Rodada are {@code SceneContext#getCurrentRound()} 1 and
     * 2 of a Cena de Combate, the numbering {@code SceneContext#isWithinFirstCombatRounds} uses.
     *
     * <p>Its "5 Graduações na Perícia escolhida" is real too — an {@code isEligible} override reading
     * Foco em Perícia's pick.
     */
    MESTRE_PERITO(
            "Na primeira Rodada de cada Cena de Combate você pode optar por utilizar a Perícia "
                    + "escolhida como uma Ação Livre ou como uma Reação. Nas segundas Rodadas você "
                    + "poderá utilizar a Perícia escolhida como uma Ação Livre. Estes benefícios "
                    + "são restritos a uma utilização por Rodada.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.GNOSE)
                    .requiredAttributeValue(5)
                    .requiredFeat(PERITO_VELOZ)
                    .build()) {
        @Override
        public boolean isEligible(final Character character, final CharacterSheet sheet) {
            return Feat.meetsRequirements(getFeatRequirements(), character, sheet)
                    && FocoEmPericiaFeat.chosenBy(character)
                            .filter(skill -> graduationOf(character, skill) >= MESTRE_PERITO_GRADUATION)
                            .isPresent();
        }

        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder,
                                         final SceneContext sceneContext) {
            if (holder == null || sceneContext == null || !sceneContext.isCombatScene()
                    || !hasFocusIn(holder.getCharacter(), skillType)
                    || holder.countFeatActivationsThisRound(this) > 0) {
                return false;
            }
            ActionCost cost = skillRoll.getActionCost();
            if (cost == null) {
                return false;
            }
            int round = sceneContext.getCurrentRound();
            return switch (cost.kind()) {
                case FREE_ACTION -> round == 1 || round == 2;
                case REACTION -> round == 1;
                default -> false;
            };
        }
    },

    /**
     * "Se não estiver em combate ou qualquer outra situação de estresse, você pode optar por reduzir
     * o GD de suas rolagens em -2 Níveis ao custo de triplicar seu Tempo de Ação. Sob situações de
     * estresse ou combates, ao invés dos efeitos anteriores, você poderá rolar novamente o dado de
     * menor valor de suas rolagens."
     *
     * <p><b>Real</b>, as an <b>activated</b> Talento on a roll of the Foco Perícia, whose effect is
     * decided by where the roll is made: outside a Cena de Combate (or with no Scene at all) it eases
     * the GD two níveis ({@link Feat#resolveDifficultyReduction(SkillType, Character, SceneContext,
     * SkillRoll)}) and triples the price ({@link Feat#resolveSkillRollActionPointMultiplier}); inside
     * one it pays for {@code SkillRoll#rerollingLowestDie} instead. ⚠️ "Qualquer outra situação de
     * estresse" is read as the Cena de Combate alone — nothing else in this core marks a Scene as
     * stressful; a GM calling one so simply has the player not opt in.
     *
     * <p>"Não pode ser usado em conjunto com a perícia Persuasão e Atenção" is a condition of use
     * ({@link #permitsActivation}), not of acquisition. The Pré-requisito is real in full: Foco on a
     * Perícia that is not de Ataque, and 2 Habilidades de Competência <em>or</em> 4 Especializações of
     * it — racial and Talento-granted ones count, the same aggregate every trait check reads.
     */
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
                    .build()) {
        @Override
        public boolean isEligible(final Character character, final CharacterSheet sheet) {
            return Feat.meetsRequirements(getFeatRequirements(), character, sheet)
                    && FocoEmPericiaFeat.chosenBy(character)
                            .filter(skill -> !skill.isAttackSkill())
                            .filter(skill -> competencyAbilitiesOf(character, skill) >= MAESTRIA_COMPETENCY_ABILITIES
                                    || character.getSpecializations(skill).size() >= MAESTRIA_SPECIALIZATIONS)
                            .isPresent();
        }

        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return holder != null && hasFocusIn(holder.getCharacter(), skillType)
                    && skillType != SkillType.PERSUASAO && skillType != SkillType.ATTENTION;
        }

        @Override
        public int resolveDifficultyReduction(final SkillType skillType, final Character character,
                                              final SceneContext sceneContext, final SkillRoll skillRoll) {
            return skillRoll != null && skillRoll.activated(this) && !isInCombat(sceneContext)
                    ? MAESTRIA_DIFFICULTY_REDUCTION : 0;
        }

        @Override
        public int resolveSkillRollActionPointMultiplier(final SkillType skillType, final CombatantSheet roller,
                                                         final Set<Feat> activatedFeats,
                                                         final SceneContext sceneContext) {
            return activatedFeats.contains(this) && !isInCombat(sceneContext) ? MAESTRIA_TIME_MULTIPLIER : 1;
        }

        @Override
        public boolean grantsLowestDieReroll(final SkillType skillType, final CombatantSheet roller,
                                             final SceneContext sceneContext) {
            return roller != null && hasFocusIn(roller.getCharacter(), skillType) && isInCombat(sceneContext);
        }
    },

    /**
     * "A Margem Crítica de todas as suas rolagens de Perícias é aumentada em +2", excluding
     * Perícias de Ataque and Esquiva e Aparar.
     *
     * <p><b>Real.</b> It names the Margem Crítica without narrowing to the Menor tier, which is what
     * {@code SkillRoll#getCriticalResult(int)} widens, and the exclusion is {@code
     * SkillType#isAttackSkill()}. The Pré-requisito — "4 ou mais Graduações em pelo menos 3 diferentes
     * Perícias" — is an {@code isEligible} override, since {@code FeatRequirements} names one Perícia.
     */
    CONTROLE_DA_SITUACAO(
            "A Margem Crítica de todas as suas rolagens de Perícias é aumentada em +2. Este "
                    + "Talento não afeta rolagens de Perícias de Ataque e Esquivar e Aparar.",
            FeatRequirements.builder().build()) {
        @Override
        public boolean isEligible(final Character character, final CharacterSheet sheet) {
            return Feat.meetsRequirements(getFeatRequirements(), character, sheet)
                    && hasBroadTraining(character);
        }

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
     * "Após falhar em uma rolagem de Perícia você pode optar por tentar novamente a mesma ação, se o
     * fizer a nova tentativa utilizará apenas 1PA e poderá ser rolada com bônus de Vantagem."
     *
     * <p><b>Real</b>, as an <b>activated</b> Talento on the retry: {@link #permitsActivation} requires
     * the holder's most recent roll ({@code CombatantSheet#getActionsThisCena()}) to be a failure of
     * the same Perícia, and priced at 1PA ({@link Feat#resolveSkillRollActionPointOverride}) it rolls
     * with Vantagem. The caller throws the new dice, as it throws every die. ⚠️ Two readings: "a mesma
     * ação" is the same Perícia (the log records no Especialização or purpose), and a retry that fails
     * cannot itself be retried — "após falhar em uma rolagem" is read as the original attempt, or the
     * Talento would buy unlimited 1PA attempts. A roll with no stated GD never failed, so it cannot be
     * retried.
     */
    // The disjunctive Pré-requisito is real — "Gnose 3 *ou* Foco 3", two FeatRequirements#anyOf branches.
    LEMBRAR_COMO_SE_FAZ(
            "Após falhar em uma rolagem de Perícia você pode optar por tentar novamente a mesma "
                    + "ação, se o fizer a nova tentativa utilizará apenas 1PA e poderá ser rolada "
                    + "com bônus de Vantagem.",
            FeatRequirements.builder()
                    .alternative(FeatRequirements.builder()
                            .attributeDomain(AttributeDomain.GNOSE)
                            .requiredAttributeValue(3)
                            .build())
                    .alternative(FeatRequirements.builder()
                            .attributeDomain(AttributeDomain.FOCUS)
                            .requiredAttributeValue(3)
                            .build())
                    .build()) {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            if (holder == null) {
                return false;
            }
            List<CombatantAction> log = holder.getActionsThisCena();
            if (log.isEmpty()) {
                return false;
            }
            CombatantAction previous = log.get(log.size() - 1);
            return previous.skill() == skillType
                    && previous.outcome() != null
                    && Boolean.FALSE.equals(previous.outcome().succeeded())
                    && !previous.activated(this);
        }

        @Override
        public Integer resolveSkillRollActionPointOverride(final SkillType skillType, final CombatantSheet roller,
                                                           final Set<Feat> activatedFeats,
                                                           final SceneContext sceneContext) {
            return activatedFeats.contains(this) ? LEMBRAR_RETRY_COST : null;
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character,
                                          final AttackSource attackSource, final CombatantSheet holder,
                                          final SkillRoll skillRoll) {
            return skillRoll != null && skillRoll.activated(this) ? Skill.ADVANTAGE_BONUS : 0;
        }
    },

    /**
     * "Sempre que for beneficiado pelos efeitos de 'Lembrar Como se Faz', a segunda rolagem tem o GD
     * reduzido em -1 nível."
     *
     * <p><b>Real</b>: a roll that activates {@link #LEMBRAR_COMO_SE_FAZ} <em>is</em> the second
     * rolagem, so its GD eases one nível whenever this Talento is held beside it.
     */
    // Its own disjunction — "Gnose 5 ou Foco 5" — is real; the required Talento is common to both
    // branches, so it stays on the outer group rather than being repeated in each.
    LEMBRAR_REVISAR_E_APRIMORAR(
            "Sempre que for beneficiado pelos efeitos de ‘Lembrar Como se Faz’, a segunda rolagem "
                    + "tem o GD reduzido em -1 nível.",
            FeatRequirements.builder()
                    .requiredFeat(LEMBRAR_COMO_SE_FAZ)
                    .alternative(FeatRequirements.builder()
                            .attributeDomain(AttributeDomain.GNOSE)
                            .requiredAttributeValue(5)
                            .build())
                    .alternative(FeatRequirements.builder()
                            .attributeDomain(AttributeDomain.FOCUS)
                            .requiredAttributeValue(5)
                            .build())
                    .build()) {
        @Override
        public int resolveDifficultyReduction(final SkillType skillType, final Character character,
                                              final SceneContext sceneContext, final SkillRoll skillRoll) {
            return skillRoll != null && skillRoll.activated(LEMBRAR_COMO_SE_FAZ) ? 1 : 0;
        }
    },

    /**
     * "Adquira vantagem em suas rolagens de 'Persuasão'. A GD de suas rolagens de Atenção: Discernir
     * Motivação é reduzida em -1 nível."
     *
     * <p><b>Real</b>, both halves: a flat {@link Skill#ADVANTAGE_BONUS} on Persuasão, and one nível off
     * an Atenção roll made <em>as</em> Discernir Motivação — the Especialização the roll requests
     * ({@code SkillRoll#getRequestedAbility()}), which the roll-taking GD hook sees.
     */
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

        @Override
        public int resolveDifficultyReduction(final SkillType skillType, final Character character,
                                              final SceneContext sceneContext, final SkillRoll skillRoll) {
            return skillType == SkillType.ATTENTION && skillRoll != null
                    && skillRoll.getRequestedAbility() == AttentionSpecialization.DISCERNIR_MOTIVACAO ? 1 : 0;
        }
    },

    /**
     * "Você recebe Vantagem em suas rolagens de 'Artes' e 'Persuasão', mas apenas quando tiver a
     * intenção de blefar, realizar ações teatrais ou disfarces."
     *
     * <p><b>Real</b>, as an <b>activated</b> Talento — table ruling (2026-09-28). The intent is the
     * player's declaration: a roll made to bluff, act or disguise names this Talento, and only such a
     * roll of Artes or Persuasão gets the Vantagem. The core does not judge what a roll is for; the GM
     * does, as with every opt-in. "2 graduações em Atuação e em Persuasão" reads Atuação as the Artes
     * Perícia (whose Especialização it is): Persuasão is the data clause, Artes an {@code isEligible}
     * override.
     */
    MESTRE_EM_ATUACAO(
            "Você recebe Vantagem em suas rolagens de ‘Artes’ e ‘Persuasão’, mas apenas quando "
                    + "tiver a intenção de blefar, realizar ações teatrais ou disfarces.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.PERSUASAO)
                    .requiredSkillGraduation(2)
                    .build()) {
        @Override
        public boolean isEligible(final Character character, final CharacterSheet sheet) {
            return Feat.meetsRequirements(getFeatRequirements(), character, sheet)
                    && graduationOf(character, SkillType.ARTES) >= MESTRE_EM_ATUACAO_ARTES_GRADUATION;
        }

        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return isPerformance(skillType);
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character,
                                          final AttackSource attackSource, final CombatantSheet holder,
                                          final SkillRoll skillRoll) {
            return skillRoll != null && skillRoll.activated(this) && isPerformance(skillType)
                    ? Skill.ADVANTAGE_BONUS : 0;
        }
    },

    /**
     * "Escolha 3 Perícias que você possua Treinamento. Você pode escolher uma Especialização ou
     * Habilidade de Competência de cada uma destas Perícias."
     *
     * <p><b>Real</b>, through {@link ChosenSkillTraitsFeat}, and now discoverable the way {@code
     * GnoseAbility#DOMINIO_DO_CONHECIMENTO} is: the choice names three trained Perícias, and {@link
     * #resolveRequiredSkillTraitKinds()} tells the client each owes an Especialização <em>or</em> a
     * Habilidade de Competência. The acquired form carries the traits picked from them.
     *
     * <p>"Personagens recém-criados ou Graduação 4 em 3 diferentes Perícias" is real in both branches:
     * {@link #isEligibleAtCreation} opens it to any new character (who still needs three trained
     * Perícias to choose from), and {@link #isEligible} asks the Graduações afterwards.
     */
    TREINADO_EM_PERICIAS(
            "Escolha 3 Perícias que você possua Treinamento. Você pode escolher uma Especialização "
                    + "ou Habilidade de Competência de cada uma destas Perícias.",
            FeatRequirements.builder().build()) {
        @Override
        public boolean isEligible(final Character character, final CharacterSheet sheet) {
            return Feat.meetsRequirements(getFeatRequirements(), character, sheet)
                    && hasBroadTraining(character);
        }

        @Override
        public boolean isEligibleAtCreation(final Character character, final CharacterSheet sheet) {
            return Feat.meetsRequirements(getFeatRequirements(), character, sheet)
                    && character.getSkills().size() >= TREINADO_PICKS;
        }

        @Override
        public List<FeatChoice<?>> resolveRequiredChoices(final Character holder) {
            return List.of(new FeatChoice<>(SkillType.class, TREINADO_PICKS, Arrays.stream(SkillType.values())
                    .filter(holder.getSkills()::containsKey)
                    .toList()));
        }

        @Override
        public Set<SkillTraitKind> resolveRequiredSkillTraitKinds() {
            return Set.of(SkillTraitKind.SPECIALIZATION, SkillTraitKind.COMPETENCY_ABILITY);
        }
    },

    /**
     * "No início de cada Cena de Combate, antes de qualquer ação sua, você pode escolher reduzir seu
     * valor de Iniciativa de modo a agir por último. Enquanto você for o último a agir você recebe
     * uma Ação Livre e Reação adicional."
     *
     * <p><b>Real.</b> Acting last is {@code Scene#deferToLast}, which this Talento permits ({@link
     * #permitsDeferringToLast}) and which enforces "antes de qualquer ação sua". The Ação Livre and
     * Reação are read off {@code SceneContext#getInitiativePosition()} being {@code LAST} in a Cena de
     * Combate, by {@code FreeActionsService}/{@code ReactionsService}'s context-taking overloads — so
     * being last by one's own roll counts too, and someone slower joining ends it.
     */
    // "Iniciativa 2 ou inferior" is enforced through FeatRequirements#maximumEgoDomain — an EgoDomain
    // rather than an AttributeDomain, since Iniciativa is an Ego in this ruleset.
    ANALISTA_TATICO(
            "No início de cada Cena de Combate, antes de qualquer ação sua, você pode escolher "
                    + "reduzir seu valor de Iniciativa de modo a agir por último. Enquanto você "
                    + "for o último a agir você recebe uma Ação Livre e Reação adicional.",
            FeatRequirements.builder()
                    .maximumEgoDomain(EgoDomain.INICIATIVA)
                    .maximumEgoValue(2)
                    .build()) {
        @Override
        public boolean permitsDeferringToLast(final Character character) {
            return true;
        }

        @Override
        public int resolveReactionsIncrease(final Character character, final SceneContext sceneContext) {
            return actsLastInCombat(sceneContext) ? 1 : 0;
        }

        @Override
        public int resolveFreeActionsIncrease(final Character character, final SceneContext sceneContext) {
            return actsLastInCombat(sceneContext) ? 1 : 0;
        }
    },

    /**
     * "Enquanto você for o último a agir, sua Margem Crítica Menor aumenta em +1 para cada Título
     * Aventyr Desperto. Enquanto você for o último a agir, a sua resistência a Corrente de Efeitos
     * aumenta em +1 e a resistência à Correntes de Efeitos de seus inimigos alvos (apenas para
     * resistir aos seus efeitos) é reduzida em -1."
     *
     * <p><b>Real</b>, every clause off where the holder stands in the order of play. The Margem is
     * read off the roller's own {@code SceneContext}; the two Corrente figures by {@code
     * EffectChainService#getRequiredMargin(CombatantSheet, InitiativePosition, CombatantSheet,
     * InitiativePosition)}, which {@code AttackDelivery}/{@code AttackReceiver} resolve each side's
     * position for — off the live Scene when the attack carries one. Unlike its sibling, not gated on
     * a Cena de Combate: the position is only ever known inside a Scene's order anyway.
     */
    // Its own "Iniciativa 2 ou inferior" is enforced too, the same way.
    GRANDE_ANALISTA_TATICO(
            "Enquanto você for o último a agir, sua Margem Crítica Menor aumenta em +1 para cada "
                    + "Título Aventyr Desperto. Enquanto você for o último a agir, a sua "
                    + "resistência a Corrente de Efeitos aumenta em +1 e a resistência à Correntes "
                    + "de Efeitos de seus inimigos alvos é reduzida em -1.",
            FeatRequirements.builder()
                    .maximumEgoDomain(EgoDomain.INICIATIVA)
                    .maximumEgoValue(2)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                                  final Character character) {
            return sceneContext != null && sceneContext.getInitiativePosition() == InitiativePosition.LAST
                    ? character.getAllTitles().size() : 0;
        }

        @Override
        public int resolveEffectChainResistanceIncrease(final Character holder,
                                                        final InitiativePosition holderPosition) {
            return holderPosition == InitiativePosition.LAST ? 1 : 0;
        }

        @Override
        public int resolveTargetEffectChainResistanceReduction(final Character attacker,
                                                               final InitiativePosition attackerPosition) {
            return attackerPosition == InitiativePosition.LAST ? 1 : 0;
        }
    },

    /**
     * "Você recebe Vantagem adicional em suas Rolagens da Perícia escolhida enquanto houver pelo
     * menos dois aliados em Distância Curta treinados nesta mesma Perícia."
     *
     * <p><b>Real</b>, off {@code SceneContext#getAlliesWithin(DISTANCIA_CURTA)} and each ally's own
     * training in the Foco Perícia. A holder of {@link #TRABALHO_EM_EQUIPE_APRIMORADO} takes that
     * Talento's GD reduction "ao invés de" this Vantagem, so this grants nothing to them.
     */
    TRABALHO_EM_EQUIPE(
            "Você recebe Vantagem adicional em suas Rolagens da Perícia escolhida no Talento Foco "
                    + "em Perícia enquanto houver pelo menos dois aliados em Distância Curta "
                    + "treinados nesta mesma Perícia.",
            FeatRequirements.builder()
                    .requiredFeat(FOCO_EM_PERICIA)
                    .build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            boolean improved = character.getFeats().stream()
                    .anyMatch(feat -> feat.catalogEntry() == TRABALHO_EM_EQUIPE_APRIMORADO);
            return !improved && isTeamworkBacked(character, skillType, sceneContext) ? Skill.ADVANTAGE_BONUS : 0;
        }
    },

    /**
     * "Sempre que você puder ser beneficiado pelo Talento Trabalho em Equipe, ao invés de receber
     * Vantagem adicional a GD de sua rolagem será reduzida em -1 Nível."
     *
     * <p><b>Real</b>: the same condition as {@link #TRABALHO_EM_EQUIPE}, answered on the GD instead —
     * and that Talento stands down for a holder of this one.
     */
    TRABALHO_EM_EQUIPE_APRIMORADO(
            "Sempre que você puder ser beneficiado pelo Talento Trabalho em Equipe, ao invés de "
                    + "receber Vantagem adicional a GD de sua rolagem será reduzida em -1 Nível.",
            FeatRequirements.builder()
                    .requiredFeat(TRABALHO_EM_EQUIPE)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveDifficultyReduction(final SkillType skillType, final Character character,
                                              final SceneContext sceneContext) {
            return isTeamworkBacked(character, skillType, sceneContext) ? 1 : 0;
        }
    },

    /**
     * "Você pode respirar na água por um curto período, ao custo de 1PD por Rodada."
     *
     * <p><b>Real</b>, as {@link PeritoActiveAbility#GUELRAS}: each activation spends 1PD for a Rodada
     * of it, and {@code CombatantSheet#canBreatheUnderwater()} answers true while it runs. That answer
     * is the whole effect — nothing in this core tracks breathing, so it is the caller that decides
     * who is drowning.
     */
    // The Pré-requisito is real in full: Treinamento em Atletismo, the Especialização Pulmão de Aço
    // and the Habilidade de Competência Anfíbio (FeatRequirements#requiredSkillTraits takes both).
    CRIANCA_DO_MAR(
            "Você pode respirar na água por um curto período, ao custo de 1PD por Rodada.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATLETISMO)
                    .requiredSkillGraduation(1)
                    .requiredSkillTrait(AtletismoSpecialization.PULMAO_DE_ACO)
                    .requiredSkillTrait(AtletismoCompetencyAbility.ANFIBIO)
                    .build()) {
        @Override
        public Optional<ActiveAbility> resolveActiveAbility() {
            return Optional.of(PeritoActiveAbility.GUELRAS);
        }

        @Override
        public boolean allowsUnderwaterBreathing(final Character character, final CombatantSheet holder) {
            return PeritoActiveAbility.GUELRAS.isActiveOn(holder);
        }
    },

    /**
     * "Como insetos, você é capaz de se mover e grudar em paredes e tetos, incluindo superfícies
     * lisas e movimentos de cabeça para baixo, ao custo de 1PD por Rodada."
     *
     * <p><b>Real</b>, as {@link PeritoActiveAbility#ADERENCIA}: each activation spends 1PD for a
     * Rodada, and {@code CombatantSheet#canClingToSurfaces()} answers true while it runs. The
     * Movimento Base Vertical it moves by is the one its own Pré-requisito Alpinista Veloz already
     * grants ({@code MovementMode.CLIMB}); which surface is where is geometry, the caller's.
     */
    // The Pré-requisito is real in full: Treinamento em Atletismo, the Especialização Levantamento de
    // Peso and the Habilidade de Competência Alpinista Veloz.
    REI_DA_MONTANHA(
            "Como insetos, você é capaz de se mover e grudar em paredes e tetos, incluindo "
                    + "superfícies lisas e movimentos de cabeça para baixo, ao custo de 1PD por "
                    + "Rodada.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATLETISMO)
                    .requiredSkillGraduation(1)
                    .requiredSkillTrait(AtletismoSpecialization.LEVANTAMENTO_DE_PESO)
                    .requiredSkillTrait(AtletismoCompetencyAbility.ALPINISTA_VELOZ)
                    .build()) {
        @Override
        public Optional<ActiveAbility> resolveActiveAbility() {
            return Optional.of(PeritoActiveAbility.ADERENCIA);
        }

        @Override
        public boolean clingsToSurfaces(final Character character, final CombatantSheet holder) {
            return PeritoActiveAbility.ADERENCIA.isActiveOn(holder);
        }
    };

    /** CONTROLE_DA_SITUACAO's own stated "+2" to the Margem Crítica. */
    private static final int CONTROLE_DA_SITUACAO_MARGIN_INCREASE = 2;

    /** "4 ou mais Graduações em pelo menos 3 diferentes Perícias" — Controle da Situação and Treinado em Perícias. */
    private static final int BROAD_TRAINING_GRADUATION = 4;
    private static final int BROAD_TRAINING_SKILLS = 3;

    /** Perito Veloz's "-1PA". */
    private static final int PERITO_VELOZ_REDUCTION = 1;

    /** Mestre Perito's "5 Graduações na Perícia escolhida". */
    private static final int MESTRE_PERITO_GRADUATION = 5;

    /** Maestria em Perícia's "-2 Níveis", "triplicar seu Tempo de Ação" and its "2 … ou 4" Pré-requisito. */
    private static final int MAESTRIA_DIFFICULTY_REDUCTION = 2;
    private static final int MAESTRIA_TIME_MULTIPLIER = 3;
    private static final int MAESTRIA_COMPETENCY_ABILITIES = 2;
    private static final int MAESTRIA_SPECIALIZATIONS = 4;

    /** Lembrar Como se Faz's "a nova tentativa utilizará apenas 1PA". */
    private static final int LEMBRAR_RETRY_COST = 1;

    /** Mestre em Atuação's "2 graduações em Atuação". */
    private static final int MESTRE_EM_ATUACAO_ARTES_GRADUATION = 2;

    /** Treinado em Perícias' "Escolha 3 Perícias". */
    private static final int TREINADO_PICKS = 3;

    /** Exibicionista's "um grupo de 4 ou mais personagens". */
    private static final int AUDIENCE_SIZE = 4;

    /** Trabalho em Equipe's "pelo menos dois aliados". */
    private static final int TEAMWORK_ALLIES = 2;

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

    /** Whether skill is the Perícia character chose for Foco em Perícia. */
    private static boolean hasFocusIn(final Character character, final SkillType skill) {
        return FocoEmPericiaFeat.chosenBy(character).filter(skill::equals).isPresent();
    }

    /** Discreto's "não tiver aliados ou personagens neutros em Distâncias Curtas" — false with no Scene. */
    private static boolean isAlone(final SceneContext sceneContext) {
        return sceneContext != null
                && !sceneContext.hasAllyWithin(Range.DISTANCIA_CURTA)
                && !sceneContext.hasNeutralWithin(Range.DISTANCIA_CURTA);
    }

    /** Discreto's "nenhum inimigo puder vê-lo" — Escondido, and seen through by no enemy in the context. */
    private static boolean isUnseen(final CombatantSheet holder, final SceneContext sceneContext) {
        return holder != null && sceneContext != null && holder.getHidden()
                .filter(hidden -> sceneContext.getEnemies().stream().noneMatch(hidden::isDetectedBy))
                .isPresent();
    }

    /** Exibicionista's plateia — four or more neutral characters in the Scene. */
    private static boolean hasAudience(final SceneContext sceneContext) {
        return sceneContext != null && sceneContext.getNeutrals().stream()
                .filter(CombatantSheet::isIntelligent)
                .count() >= AUDIENCE_SIZE;
    }

    /** Mestre em Atuação's two Perícias. */
    private static boolean isPerformance(final SkillType skillType) {
        return skillType == SkillType.ARTES || skillType == SkillType.PERSUASAO;
    }

    /** "Se não estiver em combate" — no Scene at all counts as out of combat. */
    private static boolean isInCombat(final SceneContext sceneContext) {
        return sceneContext != null && sceneContext.isCombatScene();
    }

    /** Analista Tático's "Enquanto você for o último a agir", inside a Cena de Combate. */
    private static boolean actsLastInCombat(final SceneContext sceneContext) {
        return sceneContext != null && sceneContext.isCombatScene()
                && sceneContext.getInitiativePosition() == InitiativePosition.LAST;
    }

    /** Trabalho em Equipe's condition: the Foco Perícia, with two allies in Curta trained in it. */
    private static boolean isTeamworkBacked(final Character character, final SkillType skillType,
                                            final SceneContext sceneContext) {
        return hasFocusIn(character, skillType) && sceneContext != null
                && sceneContext.getAlliesWithin(Range.DISTANCIA_CURTA).stream()
                        .filter(ally -> ally.getCharacter().getSkills().containsKey(skillType))
                        .count() >= TEAMWORK_ALLIES;
    }

    /** "4 ou mais Graduações em pelo menos 3 diferentes Perícias". */
    private static boolean hasBroadTraining(final Character character) {
        return Arrays.stream(SkillType.values())
                .filter(skill -> graduationOf(character, skill) >= BROAD_TRAINING_GRADUATION)
                .count() >= BROAD_TRAINING_SKILLS;
    }

    /** How many Habilidades de Competência of skill character holds — racial and granted ones included. */
    private static long competencyAbilitiesOf(final Character character, final SkillType skill) {
        return SkillCompetencyAbility.allFor(character).stream()
                .filter(ability -> ability.getSkillType() == skill)
                .count();
    }

    private static int graduationOf(final Character character, final SkillType skill) {
        CharacterSkill trained = character.getSkills().get(skill);
        return trained == null ? 0 : trained.getGraduation().getGraduationValue();
    }
}
