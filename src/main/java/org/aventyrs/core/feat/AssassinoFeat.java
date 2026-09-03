package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.effect.CriticalEffect;
import org.aventyrs.core.effect.Sangramento;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.profissao.ProfissaoSpecialization;
import org.aventyrs.core.title.TitleArchetype;
import java.util.List;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.CharacterSkill;
import java.util.Optional;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.item.AttackMethod;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;

/**
 * Talentos de Assassino — critical hits, opening strikes, and finishing a wounded target.
 *
 * <p>The Margem Crítica hook ({@code Feat#resolveCriticalMarginIncrease}, with a {@code
 * CombatantSheet} overload for a clause that reads the action log) widens the Margem Crítica
 * <b>Menor</b> only — {@code SkillRoll#getCriticalResult(int)} applies its margin to that tier,
 * while Acerto Crítico Maior needs a literal triple-6, so a clause that also names the Maior tier
 * gets its Menor half and nothing for the Maior one; a Maior widening would need its own explicit
 * mechanism.
 *
 * <p>Each constant's javadoc states exactly what is modelled and what is a deliberate
 * simplification. The gaps still behind an un-granted half — per CLAUDE.md's gap catalog — are
 * narrower now: the PM-spend-modifies-one-roll transaction and attack-from-a-resolution ({@link
 * #GOLPE_SOBRENATURAL}, {@link #GOLPE_SOMBRA_SOBRENATURAL}); {@code SpellCastingService} not
 * resolving a delivery dice-roll ({@link #ACERTO_CRITICO_ARCANO}); an Arma Tecnológica
 * classification / weapon catalogue ({@link #ESPECIALISTA_TECNOLOGICO}); a Roubo-de-Vida trigger
 * and the "Roubo de Bônus Base" steal type ({@link #BANQUETEAR_SE}); and a movement's direction
 * ({@link #VIOLENCIA_DESCOMUNAL}). The defeat trigger is now real — {@code
 * DefeatBlessingService#applyDefeatBlessings}, caller-driven like {@code recordAction}.
 */
public enum AssassinoFeat implements Feat {

    /**
     * "Escolha entre um Tipo de Arma ou Conjuração de Magias. Sua Margem Crítica Menor com o tipo
     * de arma escolhida, ou das magias que você conjurar, é aumentada em +1."
     *
     * <p>The root of the tree, and the one Talento in this catalog with <b>no Pré-requisito at
     * all</b> — a genuinely unrestricted acquisition, not an omission in the source.
     *
     * <p><b>Real</b>, through {@link AcertoCriticoAprimoradoFeat} — the acquired, choice-carrying
     * form granted in place of this constant. "Tipo de Arma ou Conjuração de Magias" is an {@code
     * AttackMethod}, matched against the delivered {@code AttackSource}; the margin widens the
     * Margem Crítica Menor exactly (see the enum javadoc).
     */
    ACERTO_CRITICO_APRIMORADO(
            "Escolha entre um Tipo de Arma ou Conjuração de Magias. Sua Margem Crítica Menor com o "
                    + "tipo de arma escolhida, ou das magias que você conjurar, é aumentada em +1.",
            FeatRequirements.builder().build()),

    /**
     * "Você pode sacar uma arma como Ação Livre, a primeira rolagem de Perícia de Ataque que
     * realizar neste mesmo turno será feita em Desvantagem."
     *
     * <p><b>The Desvantagem is real.</b> Both halves of its condition are live sheet state:
     * {@code CombatantSheet#hasDrawnWeaponThisTurn()} (set by {@code
     * CombatantSheet#drawWeapon(Weapon)}, the timed form of {@code Character#drawWeapon}) and
     * {@code CombatantSheet#isFirstAttackRollOfTurn()}. Reached through the {@link
     * CombatantSheet}-taking {@link Feat#resolveSkillRollBonus} overload — a {@link Character}
     * alone cannot see either.
     *
     * <p>The malus applies <b>only when a weapon was actually drawn this Turn</b>: the clause is
     * the price of the quick draw, not a standing penalty on a character who began the Turn
     * already armed.
     *
     * <p><b>The "como Ação Livre" half is real too</b>, through {@link
     * Feat#drawsWeaponAsFreeAction} — {@code WeaponDrawService#getDrawCost} returns {@code
     * ActionCost#FREE_ACTION} for a holder instead of the default 1PA.
     */
    SAQUE_RAPIDO(
            "Você pode sacar uma arma como Ação Livre, a primeira rolagem de Perícia de Ataque que "
                    + "realizar neste mesmo turno será feita em Desvantagem.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.DEXTERITY)
                    .requiredAttributeValue(2)
                    .build()) {
        /**
         * Returns 0 with no sheet — a bonuses-only preview or a {@code Character}-only caller
         * cannot tell whether a weapon was drawn this Turn, and charging the Desvantagem anyway
         * would penalise a character who never drew.
         */
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character,
                                          final AttackSource attackSource, final CombatantSheet holder) {
            if (!skillType.isAttackSkill() || holder == null) {
                return 0;
            }
            return holder.hasDrawnWeaponThisTurn() && holder.isFirstAttackRollOfTurn()
                    ? Skill.DISADVANTAGE_MALUS : 0;
        }

        @Override
        public boolean drawsWeaponAsFreeAction(final Character character) {
            return true;
        }
    },

    /**
     * "A cada Rodada, a primeira vez que fizer um ataque utilizando apenas 1PA ou Ação Livre a GD
     * da Perícia de Ataque é reduzida em -1 nível."
     *
     * <p><b>The "-1 nível" half is real</b>, through {@link SaqueRelampagoFeat} — the acquired,
     * choice-carrying form ("Escolha entre Armas ou Magias", a {@link WeaponOrSpellChoice}),
     * granted in place of this constant. It overrides {@code Feat#resolveAttackCostDifficultyReduction},
     * which reads the roll's {@code ActionCost} ("apenas 1PA ou Ação Livre" — a Reação or Ação
     * Livre spends 0) and {@code CombatantSheet#getActionsThisRound()} ("a primeira vez... a cada
     * Rodada"), and folds a -1 into {@code difficultyReduction}. It is applied through {@code
     * DifficultyLevel#easier} on the direct skill-roll path and by {@code AttackReceiver}; on the
     * {@code AttackDelivery} flat-Defesa path it is reported on {@code
     * DeliveredAttackResult#getUnappliedDifficultyReduction()} without being applied — a foe's
     * authored Defesa has no tier to ease (the {@code AttackDelivery} "Open question").
     *
     * <p><b>The Vantagem rider is a deliberate simplification: not granted.</b> "Imediatamente
     * após sacar sua primeira arma, ou a primeira magia conjurada, <i>na Cena de Combate</i>" is
     * scoped to the first draw / first cast of the whole Cena, and this core tracks weapon draws
     * and casts only per <i>Turn</i> / per <i>Rodada</i> ({@code hasDrawnWeaponThisTurn},
     * {@code getActionsThisRound}), never per Cena. Granting it off the Turn-scoped state would
     * pay out on every Rodada's first attack after a re-draw, which is wider than the clause.
     *
     * <p>The Pré-requisito is a disjunction — "Destreza 3 e Saque Rápido, <i>ou</i> Foco 5". The
     * {@code FeatRequirements} carries the Destreza+Saque Rápido branch; {@link #isEligible} is
     * overridden to also accept a Foco-5 build.
     */
    SAQUE_RELAMPAGO(
            "Escolha entre Armas ou Magias. A cada Rodada, a primeira vez que fizer um ataque "
                    + "utilizando apenas 1PA ou Ação Livre a GD da Perícia de Ataque é reduzida em "
                    + "-1 nível. Se este ataque for realizado imediatamente após sacar sua primeira "
                    + "arma, ou for a primeira magia conjurada, na Cena de Combate, você também "
                    + "recebe Vantagem nesta Rolagem.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.DEXTERITY)
                    .requiredAttributeValue(3)
                    .requiredFeat(SAQUE_RAPIDO)
                    .build()) {
        /** "Destreza 3 e Saque Rápido" (the {@code FeatRequirements}) <b>or</b> "Foco 5". */
        @Override
        public boolean isEligible(final Character character) {
            return super.isEligible(character)
                    || character.getAttributes().getAttribute(AttributeDomain.FOCUS).getBase() >= FOCO_5;
        }
    },

    /**
     * "Você pode guardar sua arma atual como uma Reação. Este Talento pode ser usado apenas uma
     * vez a cada Rodada."
     *
     * <p><b>Sheathing itself is real</b> — {@code Character#sheatheWeapon(Weapon)} puts a weapon
     * away while leaving it equipped. What this Talento adds is purely an <b>action-economy
     * permission</b> ("como uma Reação", "apenas uma vez a cada Rodada") and a
     * source-of-sheathing restriction ("não é possível usar Saque Rápido para sacar uma arma
     * guardada usando este Talento"). This core models neither: it has no per-Talento
     * activation-cost / activation-count mechanism, and {@code drawnWeapons} records only which
     * weapons are in hand, not how each got there. The permission is therefore left to the table
     * to adjudicate — the Talento is held and its sheathe/draw primitives exist, only the
     * "as a Reação, once per Rodada" accounting is out of scope.
     */
    TROCA_DE_ARMA_VELOZ(
            "Você pode guardar sua arma atual como uma Reação. Este Talento pode ser usado apenas "
                    + "uma vez a cada Rodada. Não é possível usar o Talento Saque Rápido para "
                    + "sacar uma arma guardada usando este Talento.",
            FeatRequirements.builder()
                    .requiredFeat(SAQUE_RAPIDO)
                    .build()),

    /**
     * "Seus ataques recebem 'Sangramento' como Efeito Crítico adicional."
     *
     * <p><b>Real</b>, through {@link Feat#resolveExtraCriticalEffects} — when the holder lands a
     * Perícia de Ataque as an Acerto Crítico, {@code AttackDelivery} merges a {@link Sangramento}
     * (severity taken from the triggering {@code CriticalResult}) into the attack's Efeito
     * Crítico chain, then filters it by the victim's immunities like any other.
     */
    ABRIR_FERIDAS(
            "Seus ataques recebem ‘Sangramento’ como Efeito Crítico adicional.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.STRENGTH)
                    .requiredAttributeValue(3)
                    .requiredFeat(ACERTO_CRITICO_APRIMORADO)
                    .build()) {
        @Override
        public List<CriticalEffect> resolveExtraCriticalEffects(final Character attacker, final SkillType attackSkill,
                                                                final AttackSource attackSource,
                                                                final CriticalResult criticalResult) {
            return attackSkill.isAttackSkill() && criticalResult != null && criticalResult.isCriticalSuccess()
                    ? List.of(new Sangramento(criticalResult)) : List.of();
        }
    },

    /**
     * "A primeira magia conjurada a cada Rodada utilizando apenas 1PA ou Ação Livre tem a Margem
     * Crítica Menor aumentada em +1."
     *
     * <p><b>Not granted.</b> Two things it needs are absent from the casting path, not from this
     * constant: {@code SpellCastingService} threads no {@code ActionCost} through its delivery
     * roll, and nothing records a cast in the per-Rodada action log — so "a primeira magia
     * conjurada a cada Rodada utilizando apenas 1PA ou Ação Livre" has no history to test
     * against, even though {@code resolveCriticalMarginIncrease} could carry the +1 once it did.
     *
     * <p>Its Pré-requisito is both Talentos "'Acerto Crítico Aprimorado' (Magias) e 'Saque
     * Relâmpago'". {@code FeatRequirements} carries the Saque Relâmpago gate; {@link #isEligible}
     * additionally checks that the held Acerto Crítico Aprimorado chose {@code
     * AttackMethod#OFFENSIVE_MAGIC} (via {@code AcertoCriticoAprimoradoFeat#chosenBy}).
     */
    ACERTO_CRITICO_ARCANO(
            "A primeira magia conjurada a cada Rodada utilizando apenas 1PA ou Ação Livre tem a "
                    + "Margem Crítica Menor aumentada em +1.",
            FeatRequirements.builder()
                    .requiredFeat(SAQUE_RELAMPAGO)
                    .build()) {
        @Override
        public boolean isEligible(final Character character) {
            return super.isEligible(character)
                    && AcertoCriticoAprimoradoFeat.chosenBy(character)
                            .filter(method -> method == AttackMethod.OFFENSIVE_MAGIC).isPresent();
        }
    },

    /**
     * "A Margem Crítica Menor aumenta em +2 em seu primeiro ataque de cada cena", dropping to +1
     * on each Rodada's first attack from the second Rodada on.
     *
     * <p><b>Real</b>, through {@link Feat#resolveCriticalMarginIncrease}'s {@code CombatantSheet}
     * overload — the Talento reads the per-Cena and per-Rodada action logs off {@code holder} and
     * the chosen weapon type off {@code AcertoCriticoAprimoradoFeat#chosenBy}. On the Cena's first
     * attack, made with that weapon type and immediately after drawing it this Turn, the Margem
     * Crítica Menor widens by <b>+2</b>; from the second Rodada on, the Rodada's first attack with
     * that weapon type widens by <b>+1</b>. Pré-requisito: both 'Acerto Crítico Aprimorado'
     * (a weapon choice, not Magias) and 'Saque Relâmpago', enforced by {@link #isEligible}.
     */
    ACERTO_CRITICO_RELAMPAGO(
            "A Margem Crítica Menor aumenta em +2 em seu primeiro ataque de cada cena. Este "
                    + "benefício é válido apenas se o ataque for realizado imediatamente após "
                    + "sacar Armas do tipo escolhida no talento ‘Acerto Crítico Aprimorado’. A "
                    + "partir da segunda Rodada do combate, seu primeiro ataque de cada Rodada com "
                    + "esta mesma arma tem a Margem Crítica Menor aumentada em +1.",
            FeatRequirements.builder()
                    .requiredFeat(SAQUE_RELAMPAGO)
                    .build()) {
        @Override
        public boolean isEligible(final Character character) {
            return super.isEligible(character)
                    && AcertoCriticoAprimoradoFeat.chosenBy(character)
                            .filter(method -> method != AttackMethod.OFFENSIVE_MAGIC).isPresent();
        }

        @Override
        public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                                   final Character character, final AttackSource attackSource,
                                                   final CombatantSheet holder) {
            if (!skillType.isAttackSkill() || holder == null) {
                return 0;
            }
            AttackMethod chosen = AcertoCriticoAprimoradoFeat.chosenBy(character).orElse(null);
            if (chosen == null || !chosen.matches(attackSource, character)) {
                return 0;
            }
            boolean firstOfCena = holder.getActionsThisCena().stream().noneMatch(a -> isAttackWith(a, chosen, character));
            boolean firstOfRound = holder.getActionsThisRound().stream().noneMatch(a -> isAttackWith(a, chosen, character));
            int round = sceneContext == null ? 0 : sceneContext.getCurrentRound();
            if (firstOfCena && holder.hasDrawnWeaponThisTurn() && holder.isFirstAttackRollOfTurn()) {
                return LIGHTNING_CRITICAL_CENA_MARGIN;
            }
            return round >= LIGHTNING_CRITICAL_SECOND_ROUND && firstOfRound
                    ? LIGHTNING_CRITICAL_ROUND_MARGIN : 0;
        }
    },

    /**
     * "Você recebe Vantagem em rolagens de Danos em alvos que já tenham perdido pelo menos a
     * metade de seus PV."
     *
     * <p><b>Both halves are real.</b> The Vantagem is a flat {@code Skill#ADVANTAGE_BONUS} through
     * {@link Feat#resolveDamageBonus}, whose {@code attackTarget} parameter makes the condition
     * testable. The "7 ou mais graduações" half widens the Margem Crítica Menor by 1 through
     * {@link Feat#resolveCriticalMarginIncrease}, reading the target off {@code
     * SceneContext#getOpposedCharacter()} the same way {@code AnaoFeat#GLORIA_YMIRIANA} does — a
     * Golpe de Finalização is an attack whose target has already lost half its PV, so the same
     * condition gates both halves. "Perdido pelo menos a metade de seus PV" is current PV at or
     * below half the maximum, both from {@code HitPointsService}.
     *
     * <p>The rules text widens the Menor <em>and Maior</em> tiers; only the Menor half is
     * expressible — {@code SkillRoll#getCriticalResult(int)} applies its margin to the Menor tier
     * alone and Acerto Crítico Maior needs a literal triple-6 (see the enum javadoc).
     */
    GOLPE_DE_FINALIZACAO(
            "Você recebe Vantagem em rolagens de Danos em alvos que já tenham perdido pelo menos a "
                    + "metade de seus PV. Se possuir 7 ou mais graduações na Perícia de Ataque "
                    + "utilizada, as Margens Críticas Menor e Maior de seus Golpes de Finalização "
                    + "aumentam em +1.",
            FeatRequirements.builder()
                    .requiredFeatCategory(FeatCategory.ASSASSINO)
                    .requiredFeatCategoryCount(3)
                    .build()) {
        /**
         * Returns empty with no attackTarget — on the bonuses-only preview path there is nobody
         * whose PV to measure, and granting it unconditionally would hand the Vantagem against a
         * fresh target too.
         */
        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType, final SceneContext sceneContext,
                                                         final CombatantSheet attackTarget, final Character actor) {
            if (!attackingSkillType.isAttackSkill() || attackTarget == null) {
                return Optional.empty();
            }
            return isFinishingBlowAgainst(attackTarget)
                    ? Optional.of(new DamageBonus(Skill.ADVANTAGE_BONUS, DamageType.FISICO))
                    : Optional.empty();
        }

        /**
         * The "7 ou mais graduações na Perícia de Ataque utilizada" half — the Margem Crítica
         * Menor widens by 1 on a Golpe de Finalização made with a Perícia de Ataque the holder has
         * Graduação 7+ in. The target is {@code SceneContext#getOpposedCharacter()}, so a
         * bonuses-only preview (no opposed combatant) widens nothing, and a fresh target fails the
         * half-PV check just as it does for the Vantagem half.
         */
        @Override
        public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                                   final Character character) {
            if (!skillType.isAttackSkill() || sceneContext == null || sceneContext.getOpposedCharacter() == null) {
                return 0;
            }
            if (graduationOf(character, skillType) < FINISHING_CRITICAL_MIN_GRADUATION) {
                return 0;
            }
            return isFinishingBlowAgainst(sceneContext.getOpposedCharacter())
                    ? FINISHING_CRITICAL_MARGIN_INCREASE : 0;
        }
    },

    /**
     * "Sempre que realizar Golpes de Finalização você pode gastar 1PM, se o fizer sua rolagem de
     * ataque será efetuada contra a DM do alvo, ao invés da DF; este ataque causa Danos Mágicos."
     *
     * <p><b>Not granted — three separate missing systems, none of them this constant's.</b> A
     * one-time PM spend that modifies a single roll has no transaction path (PV/PM/PD spends
     * have no reaction hook); redirecting an attack from DF to DM is not expressible ({@code
     * AttackDelivery} takes the {@code DefenseType} from the attack, with no override); and
     * granting a Corrente de Efeitos to a critical has no hook (see {@link #ABRIR_FERIDAS}).
     * Held; resolved at the table.
     */
    GOLPE_SOBRENATURAL(
            "Sempre que realizar Golpes de Finalização você pode gastar 1PM, se o fizer sua "
                    + "rolagem de ataque será efetuada contra a DM do alvo, ao invés da DF, este "
                    + "ataque causa Danos Mágicos. Acertos Críticos de Golpes Sobrenaturais "
                    + "recebem a Corrente de Efeitos – Escancarar Defesas. Este Talento pode ser "
                    + "utilizado apenas 1 vez a cada Rodada.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.FOCUS)
                    .requiredAttributeValue(2)
                    .requiredFeat(GOLPE_DE_FINALIZACAO)
                    .build()),

    /**
     * "Após ser bem-sucedido em realizar um Golpe Sobrenatural você pode gastar +2PM para
     * realizar um novo Golpe Sobrenatural contra o mesmo alvo como uma Ação Livre; um segundo
     * Golpe Sobrenatural no mesmo alvo na mesma Rodada tem a Margem Crítica Menor e Maior
     * aumentadas em +1."
     *
     * <p><b>Not granted.</b> It builds on {@link #GOLPE_SOBRENATURAL}, which is itself unmodelled
     * (PM spend / DF→DM redirect); adds an <i>extra attack</i>, which nothing in this core can
     * initiate from a resolution; and its Margem Crítica half needs "second Golpe this Rodada"
     * scoping plus a Maior-tier widening the margin does not offer (see the enum javadoc). Held;
     * resolved at the table.
     */
    GOLPE_SOMBRA_SOBRENATURAL(
            "Após ser bem-sucedido em realizar um Golpe Sobrenatural você pode gastar +2PM, se o "
                    + "fizer poderá realizar um novo Golpe Sobrenatural contra o mesmo alvo como "
                    + "uma Ação Livre. Sempre que efetuar um segundo Golpe Sobrenatural contra um "
                    + "mesmo alvo na mesma Rodada, este segundo ataque tem a Margem Crítica Menor "
                    + "e Maior aumentadas em +1 número. Golpe Sombra Sobrenatural pode ser "
                    + "utilizado apenas em seu Turno e apenas uma vez por Rodada.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.FOCUS)
                    .requiredAttributeValue(3)
                    .requiredFeat(GOLPE_SOBRENATURAL)
                    .build()),

    /**
     * "Você recebe Bônus de +3 em suas Defesas e RDS enquanto estiver escondido ou invisível."
     *
     * <p><b>Both halves are real for the "escondido" case</b>, through the {@link CombatantSheet}
     * -taking overloads of {@link Feat#resolveDefenseBonus} and {@link Feat#resolveDamageReduction}
     * — a Condição lives on the sheet, which is exactly what those overloads exist to reach.
     * "Suas Defesas" unqualified means DF and DM alike.
     *
     * <p>"Ou invisível" adds nothing — no invisibility Condição is authored (see {@code
     * docs/rules/condicoes-e-maleficios-.txt}), so the bonus is gated on Escondido alone: narrower
     * than the disjunction, never wider. The Pré-requisito "Corrida Furtiva" is {@code
     * MobilidadeFeat#MOVIMENTO_FURTIVO} (the two names are the same Talento), and is enforced.
     */
    ESCUDO_DE_SOMBRAS(
            "Você recebe Bônus de +3 em suas Defesas e RDS enquanto estiver escondido ou "
                    + "invisível.",
            FeatRequirements.builder()
                    .requiredFeat(MobilidadeFeat.MOVIMENTO_FURTIVO)
                    .build()) {
        /**
         * Returns 0 with no sheet — the {@code Character}-only entry points cannot tell whether
         * their holder is hidden, and granting +3 unconditionally would pay out in the open.
         */
        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character,
                                        final SceneContext sceneContext, final CombatantSheet holder) {
            return isHidden(holder) ? SHADOW_SHIELD_BONUS : 0;
        }

        @Override
        public int resolveDamageReduction(final Character character, final CombatantSheet holder) {
            return isHidden(holder) ? SHADOW_SHIELD_BONUS : 0;
        }
    },

    /**
     * "Sempre que derrotar um inimigo, reduzindo seus PV à 0 ou menos, para este Turno você
     * recebe +1PA e então +1PA para cada Título Aventyr que possuir."
     *
     * <p><b>Real</b>, through {@link Feat#resolveDefeatBlessings} — after an attack the caller has
     * determined dropped a target to 0 PV, {@code DefeatBlessingService#applyDefeatBlessings}
     * grants the attacker a Turn-scoped {@code ModifierType.ACTION_POINTS} {@link Blessing} worth
     * {@code 1 + Character#getAllTitles().size()}. Any defeat triggers it, critical or not.
     */
    SANGUE_QUENTE(
            "Sempre que derrotar um inimigo, reduzindo seus PV à 0 ou menos, para este Turno você "
                    + "recebe +1PA e então +1PA para cada Título Aventyr que possuir.",
            FeatRequirements.builder()
                    .requiredFeat(GOLPE_DE_FINALIZACAO)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public List<Blessing> resolveDefeatBlessings(final Character attacker, final CombatantSheet defeated,
                                                     final boolean viaCriticalHit) {
            int actionPoints = 1 + attacker.getAllTitles().size();
            return List.of(new Blessing(ModifierType.ACTION_POINTS, actionPoints, TURN_SCOPED_ROUNDS,
                    TargetScope.SELF, name()));
        }
    },

    /**
     * "Você não recebe Vantagem em Danos em seus Acertos Críticos, ao invés disso recebe Bônus de
     * +1d6", plus a Movimento Base increase when an Acerto Crítico eliminates a target.
     *
     * <p><b>The Movimento half is real</b>, through {@link Feat#resolveDefeatBlessings} — after a
     * critical hit the caller determined eliminated a target, the attacker gets a {@code
     * ModifierType.MOVEMENT} {@link Blessing} worth {@code 2 ×} the number of Bruto Títulos
     * Despertos. The "apenas para se aproximar de personagens inimigos" scoping is <b>dropped</b>
     * — this core never tracks a movement's direction — so the grant is unconditional movement,
     * documented as wider than the clause.
     *
     * <p>The "+1d6 ao invés de Vantagem em Danos Críticos" half is not modelled: there is no
     * "Vantagem em Danos on an Acerto Crítico" baseline for it to replace.
     */
    VIOLENCIA_DESCOMUNAL(
            "Você não recebe Vantagem em Danos em seus Acertos Críticos, ao invés disso recebe "
                    + "Bônus de +1d6. Sempre que eliminar um alvo com um Acerto Crítico, seu "
                    + "Movimento Base aumenta em +dobro do Número de Títulos Brutos Despertos, mas "
                    + "apenas para se aproximar de personagens inimigos.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.STRENGTH)
                    .requiredAttributeValue(5)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public List<Blessing> resolveDefeatBlessings(final Character attacker, final CombatantSheet defeated,
                                                     final boolean viaCriticalHit) {
            if (!viaCriticalHit) {
                return List.of();
            }
            long brutoTitles = attacker.getAllTitles().stream()
                    .filter(title -> title.getArchetype() == TitleArchetype.BRUTO).count();
            return List.of(new Blessing(ModifierType.MOVEMENT, (int) (2 * brutoTitles), TURN_SCOPED_ROUNDS,
                    TargetScope.SELF, name()));
        }
    },

    /**
     * "O Dano Base de suas Armas Tecnológicas aumenta em +1", plus a Danos Críticos bonus per
     * Título Especialista Desperto.
     *
     * <p><b>The Pré-requisito is fully enforced</b> — {@link #isEligible} adds the second
     * Graduação floor ("4 Graduações em Profissão") and the required Especialização Mecânica on
     * top of the {@code FeatRequirements} Ataque Corpo-a-Corpo gate.
     *
     * <p><b>The two effect halves are not granted:</b> no Arma Tecnológica classification exists
     * ({@code ItemCategory} carries no such value, and no weapon catalogue is authored), so a
     * Dano Base bonus would hit every weapon; and a bonus to <i>critical</i> damage specifically
     * has no hook.
     */
    ESPECIALISTA_TECNOLOGICO(
            "O Dano Base de suas Armas Tecnológicas aumenta em +1. Você recebe Bônus de +3 em "
                    + "rolagens de Danos Críticos de Armas Tecnológicas para cada Título Aventyr "
                    + "Especialista Desperto.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(4)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public boolean isEligible(final Character character) {
            return super.isEligible(character)
                    && graduationOf(character, SkillType.PROFISSAO) >= TECH_PROFISSAO_GRADUATION
                    && holdsSpecialization(character, ProfissaoSpecialization.MECANICA);
        }
    },

    /**
     * "Suas Magias ofensivas têm a Margem Crítica Menor aumentada em 1 + o Número de Títulos
     * Arcanos Despertos."
     *
     * <p><b>The Margem Crítica half is real</b> — {@code Feat#resolveCriticalMarginIncrease}'s
     * {@link AttackSource} overload scopes it to a {@code Spell} ({@code
     * AttackMethod#OFFENSIVE_MAGIC}), and the Título count is {@code Character#getAllTitles()}
     * narrowed to {@code TitleArchetype#ARCANO}. Widens the Margem Crítica Menor (see the enum
     * javadoc).
     *
     * <p><b>The Conjuração half is real</b>, through {@link Feat#resolveDefeatBlessings} — a
     * critical-hit elimination grants a cumulative {@code ModifierType.DOMINIO_DO_MANA_ROLL_BONUS}
     * {@link Blessing} of +1, which the Domínio do Mana (Conjuração) roll already reads. "Até o
     * fim da Cena" has no scene-end trigger, so the Blessing is granted for {@link
     * #REST_OF_CENA_ROUNDS} Rodadas — long enough to outlast any Cena, an acknowledged
     * approximation of the duration, not the count.
     */
    ARCANISMO_AVASSALADOR(
            "Suas Magias ofensivas têm a Margem Crítica Menor aumentada em 1 + Número de Títulos "
                    + "Arcanos Despertos. Sempre que eliminar um alvo com um Acerto Crítico você "
                    + "recebe Bônus cumulativo de +1 em Conjuração até o fim da Cena.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.DOMINIO_DO_MANA)
                    .requiredSkillGraduation(7)
                    .requiredFeat(ACERTO_CRITICO_APRIMORADO)
                    .requiredAwakenedTitles(1)
                    .requiredTitleArchetype(TitleArchetype.ARCANO)
                    .build()) {
        @Override
        public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                                   final Character character, final AttackSource attackSource) {
            if (!skillType.isAttackSkill() || !AttackMethod.OFFENSIVE_MAGIC.matches(attackSource, character)) {
                return 0;
            }
            long arcanosDespertos = character.getAllTitles().stream()
                    .filter(title -> title.getArchetype() == TitleArchetype.ARCANO)
                    .count();
            return (int) (1 + arcanosDespertos);
        }

        @Override
        public List<Blessing> resolveDefeatBlessings(final Character attacker, final CombatantSheet defeated,
                                                     final boolean viaCriticalHit) {
            return viaCriticalHit
                    ? List.of(new Blessing(ModifierType.DOMINIO_DO_MANA_ROLL_BONUS, 1, REST_OF_CENA_ROUNDS,
                            TargetScope.SELF, name()))
                    : List.of();
        }
    },

    /**
     * "Seu primeiro Acerto Crítico em rolagem de Perícia de Ataque de cada Rodada recebe Roubo de
     * Vida 1" — or Roubo de Bônus Base 1 with 2 Títulos Aventyrs Despertos.
     *
     * <p><b>Not granted.</b> {@code LifeStealService} exists, but no {@code Feat} hook grants
     * Roubo de Vida and nothing scopes it to one roll per Rodada; "Roubo de Bônus Base" is a
     * third steal type this core does not model (only Roubo de Vida exists); and "Raça Renascida"
     * has no {@code Race} class, so {@code requiredRace} is left unset and the Talento is open to
     * every race — narrower enforcement than the rules, flagged here rather than guessed at.
     */
    BANQUETEAR_SE(
            "Seu primeiro Acerto Crítico em rolagem de Perícia de Ataque de cada Rodada recebe "
                    + "Roubo de Vida 1. Caso possua 2 Títulos Aventyrs Despertos ao invés de Roubo "
                    + "de Vida você recebe Roubo de Bônus Base 1.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .build());

    /** ESCUDO_DE_SOMBRAS' own stated "+3", granted to both Defesas and to RDS. */
    private static final int SHADOW_SHIELD_BONUS = 3;

    /** SAQUE_RELAMPAGO's "ou Foco 5" disjunctive branch. */
    private static final int FOCO_5 = 5;

    /** ESPECIALISTA_TECNOLOGICO's "4 Graduações em Profissão" second skill floor. */
    private static final int TECH_PROFISSAO_GRADUATION = 4;

    /** ACERTO_CRITICO_RELAMPAGO's "+2 no primeiro ataque de cada cena" / "+1 a partir da segunda Rodada". */
    private static final int LIGHTNING_CRITICAL_CENA_MARGIN = 2;
    private static final int LIGHTNING_CRITICAL_ROUND_MARGIN = 1;
    private static final int LIGHTNING_CRITICAL_SECOND_ROUND = 2;

    /** A Blessing scoped "para este Turno" — expires at the end of the current Rodada. */
    private static final int TURN_SCOPED_ROUNDS = 1;

    /**
     * ARCANISMO_AVASSALADOR's "até o fim da Cena" Conjuração bonus, granted for this many Rodadas
     * — an acknowledged over-long approximation, since this core has no scene-end trigger.
     */
    private static final int REST_OF_CENA_ROUNDS = 100;

    /** Whether character holds specialization on any of their trained Perícias. */
    private static boolean holdsSpecialization(final Character character, final SkillSpecialization specialization) {
        return character.getSkills().values().stream()
                .anyMatch(skill -> skill.getSpecializations().contains(specialization));
    }

    /** Whether action is a Perícia de Ataque made with an {@code AttackSource} matching method. */
    private static boolean isAttackWith(final CombatantAction action, final AttackMethod method, final Character character) {
        return action.skill() != null && action.skill().isAttackSkill()
                && method.matches(action.attackSource(), character);
    }

    /**
     * Whether holder is currently Escondido. {@code null} — a caller holding only a {@link
     * Character} — reads as "not hidden", the same way a {@code null} {@code SceneContext} reads
     * as "condition not met" everywhere else.
     */
    private static boolean isHidden(final CombatantSheet holder) {
        return holder != null && holder.hasCondition(ConditionType.ESCONDIDO, null);
    }

    /** GOLPE_DE_FINALIZACAO's "7 ou mais graduações na Perícia de Ataque utilizada". */
    private static final int FINISHING_CRITICAL_MIN_GRADUATION = 7;

    /** …raises "as Margens Críticas Menor e Maior ... em +1" — the Menor half, all the hook reaches. */
    private static final int FINISHING_CRITICAL_MARGIN_INCREASE = 1;

    private final String description;
    private final FeatRequirements featRequirements;

    AssassinoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.ASSASSINO;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public FeatRequirements getFeatRequirements() {
        return featRequirements;
    }

    /**
     * Whether an attack against target counts as a Golpe de Finalização — the target has already
     * lost at least half its PV, i.e. current PV is at or below half the maximum, both from
     * {@code HitPointsService}. The shared condition behind both halves of {@link
     * #GOLPE_DE_FINALIZACAO}.
     */
    private static boolean isFinishingBlowAgainst(final CombatantSheet target) {
        HitPointsService hitPointsService = new HitPointsServiceImpl();
        Character victim = target.getCharacter();
        return hitPointsService.getCurrentHitPoints(victim, target) * 2
                <= hitPointsService.getMaxHitPoints(victim);
    }

    /** The holder's current Graduação in skillType, 0 when the Perícia is untrained. */
    private static int graduationOf(final Character character, final SkillType skillType) {
        CharacterSkill characterSkill = character.getSkills().get(skillType);
        return characterSkill == null ? 0 : characterSkill.getGraduation().getGraduationValue();
    }
}
