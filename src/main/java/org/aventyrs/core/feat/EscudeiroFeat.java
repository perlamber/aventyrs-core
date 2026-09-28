package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.MovementMode;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.effect.Rugido;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.ShieldAttack;
import org.aventyrs.core.item.ShieldDefenseRetention;
import org.aventyrs.core.item.ShieldItem;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.race.Anao;
import org.aventyrs.core.race.Gigantes;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.sheet.ActionCost;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Talentos de Escudeiro — fighting with, and behind, a shield.
 *
 * <p><b>A Escudo can be swung now</b> (core 0.0.70): {@link ShieldAttack} is the {@code Weapon} view
 * of an equipped Escudo — Ataque Corpo-a-Corpo, 1d6+1/+2/+3 by weight, Margem Crítica Menor 17,
 * Atordoante — so an Ataque com Escudo goes through every attack path this core has, and the six
 * constants built on it are real. The Defesa a shield loses by attacking is {@code DefenseService}'s,
 * read off the action log ({@link ShieldAttack#retained}).
 *
 * <p>Table rulings (2026-09-27): the shield's roll bonus is <em>added</em> to an ordinary Ataque
 * Corpo-a-Corpo roll; Criar Refúgio's uses refill on a Descanso Longo Verdadeiro and its condition is
 * "hasn't attacked this Rodada"; Asas Adamantinas' wings are a Escudo Médio that may be Especialista em
 * Escudo's choice but is never a Escudo de Corpo; Domínio's "Empurrão Violento" is the Corrente Rugido.
 */
public enum EscudeiroFeat implements Feat {

    /**
     * "Sempre que iniciar um combate com um Escudo de Categoria Média ou Pesada em mãos, sua
     * iniciativa aumenta em +2."
     *
     * <p><b>Real</b>, through {@link Feat#resolveInitiativeBonus}: an equipped {@code
     * ItemCategory#SHIELD} whose authored weight is {@code ItemWeightClass#MEDIUM} or {@code
     * HEAVY}, or Asas Adamantinas' wings, a Escudo Médio. Iniciativa is read when it is rolled, so "ao
     * iniciar um combate" is judged then — ⚠️ before anyone has taken flight, so the wings count.
     */
    ESCUDO_VELOZ(
            "Sempre que iniciar um combate com um Escudo de Categoria Média ou Pesada em mãos, sua "
                    + "iniciativa aumenta em +2.",
            FeatRequirements.builder().build()) {
        @Override
        public int resolveInitiativeBonus(final Character character) {
            boolean heavyShield = character.getEquipment().stream()
                    .anyMatch(item -> item.getCategory() == ItemCategory.SHIELD
                            && (item.getWeightClass() == ItemWeightClass.MEDIUM
                            || item.getWeightClass() == ItemWeightClass.HEAVY));
            return heavyShield || holdsWings(character) ? ESCUDO_VELOZ_INITIATIVE_BONUS : 0;
        }
    },

    /**
     * "Você recebe +1 em suas Defesas enquanto utilizar um item escolhido do tipo 'Escudo'",
     * rising to +2 at 4 Graduações em Esquiva e Aparar and +3 at 7.
     *
     * <p><b>Real</b>, through {@link EspecialistaEmEscudoFeat} — the acquired form recording the
     * chosen {@link ShieldSpecialty}, granted in place of this constant. The bonus holds while a copy
     * of that Escudo is equipped, or — for the wings — while its holder is not flying.
     */
    ESPECIALISTA_EM_ESCUDO(
            "Você recebe +1 em suas Defesas enquanto utilizar um item escolhido do tipo ‘Escudo’. "
                    + "Escolha um item do tipo ‘Escudo’, se tiver 4 ou mais graduações em ‘Esquiva "
                    + "e Aparar’ enquanto estiver utilizando o item escolhido este Bônus aumenta "
                    + "para +2, se tiver 7 ou mais Graduações este Bônus aumenta para +3.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ESQUIVA_E_APARAR)
                    .requiredSkillGraduation(1)
                    .build()) {
        /** Every catalog Escudo, and the wings for a holder of Asas Adamantinas (table ruling). */
        @Override
        public List<FeatChoice<?>> resolveRequiredChoices(final Character holder) {
            List<ShieldSpecialty> options = new ArrayList<>();
            for (ShieldSpecialty specialty : ShieldSpecialty.values()) {
                if (!specialty.isWings() || holder != null && holdsWings(holder)) {
                    options.add(specialty);
                }
            }
            return List.of(FeatChoice.ofOne(ShieldSpecialty.class, options));
        }
    },

    /**
     * "Você pode usar seu escudo para atacar, se o fizer você perde metade dos bônus em Defesas
     * concedidos por ele." <b>Every clause is real.</b>
     *
     * <ul>
     *   <li>"Usar seu escudo para atacar" is an attack whose source is a {@link ShieldAttack} — refused
     *   without this Talento or without the shield in hand.</li>
     *   <li>"Recebem Metade das suas Graduações em Esquiva e Aparar + Bônus Defensivos do Escudo (DF
     *   contra DF, DM contra DM)" is {@link #resolveSkillRollBonus} on top of the ordinary Ataque
     *   Corpo-a-Corpo roll (table ruling).</li>
     *   <li>The dano, "Margem Crítica Menor 17 e Sucesso Crítico: Atordoante" are {@link ShieldAttack}'s
     *   weapon columns.</li>
     *   <li>"Perde metade … até o início de seu próximo turno", and nothing at all after two or more, is
     *   {@link #resolveShieldDefenseRetention}, applied by {@code DefenseService}.</li>
     * </ul>
     */
    ATACAR_COM_ESCUDOS(
            "Você pode usar seu escudo para atacar, se o fizer você perde metade dos bônus em "
                    + "Defesas concedidos por ele até o início de seu próximo turno. Rolagens de "
                    + "Ataque Corpo-a-Corpo efetuadas com escudos recebem Metade das suas Graduações "
                    + "em Esquiva e Aparar + Bônus Defensivos do Escudo, o dano causado varia "
                    + "conforme o tipo de escudo: Escudos Leves 1d6+1, Médios 1d6+2, Pesados 1d6+3. "
                    + "Escudos possuem Margem Crítica Menor 17 e Sucesso Crítico: Atordoante.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(2)
                    .build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character,
                                          final AttackSource attackSource, final CombatantSheet holder,
                                          final SkillRoll skillRoll) {
            if (!(attackSource instanceof ShieldAttack shield) || !skillType.isAttackSkill()) {
                return 0;
            }
            int halfGraduation = character.getEffectiveGraduation(SkillType.ESQUIVA_E_APARAR) / 2;
            return halfGraduation + shieldDefenseBonus(shield, character, holder, sceneContext);
        }

        @Override
        public ShieldDefenseRetention resolveShieldDefenseRetention(final int shieldAttacks) {
            return shieldAttacks <= 1 ? ShieldDefenseRetention.HALF : ShieldDefenseRetention.NONE;
        }
    },

    /**
     * "Se você não se mover em seu Turno e estiver empunhando um escudo que você seja especialista,
     * você recebe um Bônus de +2 em suas Defesas por 1 Rodada." <b>Real.</b> Granted when the holder's
     * Turn ends ({@link #resolveTurnEndBlessings}) with no movement bought that Turn and no Reposicionar
     * — which a Gigante or an Anão is allowed ("não perdem os bônus … se o único movimento feito por
     * eles na Rodada for ‘Reposicionar’") — while the Escudo they are Especialista in is in hand. "Por 1
     * Rodada" is the table's reading: until the holder's next Turn begins.
     */
    DEFESA_TARTARUGA(
            "Se você não se mover em seu Turno e estiver empunhando um escudo que você seja "
                    + "especialista, você recebe um Bônus de +2 em suas Defesas por 1 Rodada. "
                    + "Personagens das raças Gigante e Anão não perdem os bônus concedidos por este "
                    + "Talento se o único movimento feito por eles na Rodada for ‘Reposicionar’.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ESQUIVA_E_APARAR)
                    .requiredSkillGraduation(4)
                    .requiredFeat(ESPECIALISTA_EM_ESCUDO)
                    .build()) {
        @Override
        public List<Blessing> resolveTurnEndBlessings(final CombatantSheet holder) {
            Character character = holder.getCharacter();
            boolean repositionExempt = character.getRace() instanceof Gigantes || character.getRace() instanceof Anao;
            boolean stood = holder.getMovementsTakenThisRound() == 0
                    && (holder.getRepositionsTakenThisRound() == 0 || repositionExempt);
            if (!stood || !EspecialistaEmEscudoFeat.isUsingChosenShield(holder)) {
                return List.of();
            }
            return List.of(new Blessing(ModifierType.DEFESAS, DEFESA_TARTARUGA_BONUS, 1, TargetScope.SELF, name())
                    .countingDownAtTurnStart());
        }
    },

    /**
     * "Após uma rolagem de Iniciativa, se você não for o primeiro a agir, nas duas primeiras Rodadas
     * do combate você recebe bônus de +3 em suas Defesas", or +5 if last to act. <b>Real.</b> Where the
     * holder acts is {@code SceneContext#getInitiativePosition()}, the window {@code
     * isWithinFirstCombatRounds(2)}; "você ignora efeitos que reduzem Defesas" is {@link
     * #ignoresDefenseMaluses} over the same window.
     */
    INICIO_DEFENSIVO(
            "Após uma rolagem de Iniciativa, se você não for o primeiro a agir, nas duas primeiras "
                    + "Rodadas do combate você recebe bônus de +3 em suas Defesas. Se você for o "
                    + "último a agir este bônus aumenta para +5. Durante estas Rodadas iniciais "
                    + "você ignora efeitos que reduzem Defesas.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ESQUIVA_E_APARAR)
                    .requiredSkillGraduation(2)
                    .build()) {
        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character,
                                        final SceneContext sceneContext) {
            if (!inOpeningRounds(sceneContext)) {
                return 0;
            }
            return sceneContext.getInitiativePosition() == org.aventyrs.core.scene.InitiativePosition.LAST
                    ? INICIO_DEFENSIVO_LAST_BONUS : INICIO_DEFENSIVO_BONUS;
        }

        @Override
        public boolean ignoresDefenseMaluses(final SceneContext sceneContext, final CombatantSheet holder) {
            return inOpeningRounds(sceneContext);
        }

        private boolean inOpeningRounds(final SceneContext sceneContext) {
            return sceneContext != null && sceneContext.isWithinFirstCombatRounds(INICIO_DEFENSIVO_ROUNDS)
                    && sceneContext.getInitiativePosition().isAfterFirst();
        }
    },

    /**
     * "Após realizar uma rolagem de Perícia de Ataque, você pode realizar um Ataque com Escudo em
     * Desvantagem com Tempo de Ação reduzido em -1PA." <b>Real.</b> The follow-up is the caller's
     * attack; activating this Talento on it is allowed only when it is an Ataque com Escudo right after
     * another attack this Turn, and makes it -2 and 1PA cheaper (never below 1PA).
     */
    ESPARTANO(
            "Após realizar uma rolagem de Perícia de Ataque, você pode realizar um Ataque com "
                    + "Escudo em Desvantagem com Tempo de Ação reduzido em -1PA.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(3)
                    .requiredFeat(ATACAR_COM_ESCUDOS)
                    .build()) {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            if (!(attackSource instanceof ShieldAttack) || holder == null) {
                return false;
            }
            List<CombatantAction> turn = holder.getActionsThisTurn();
            return !turn.isEmpty() && turn.get(turn.size() - 1).isAttack();
        }

        @Override
        public int resolveAttackActionPointAdjustment(final SkillType skillType, final AttackSource attackSource,
                                                      final CombatantSheet attacker, final Set<Feat> activatedFeats) {
            return activatedFeats.contains(this) ? -1 : 0;
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character,
                                          final AttackSource attackSource, final CombatantSheet holder,
                                          final SkillRoll skillRoll) {
            return skillRoll != null && skillRoll.activated(this) ? Skill.DISADVANTAGE_MALUS : 0;
        }
    },

    /**
     * "Você mantém metade dos Bônus Defensivos de seu Escudo ao realizar mais de um Ataque com
     * Escudos na mesma Rodada." <b>Real</b>: {@link #resolveShieldDefenseRetention} keeps half however
     * many times the shield attacked.
     */
    ATAQUE_MULTIPLO_COM_ESCUDOS(
            "Você mantém metade dos Bônus Defensivos de seu Escudo ao realizar mais de um Ataque "
                    + "com Escudos na mesma Rodada.",
            FeatRequirements.builder()
                    .requiredFeat(ATACAR_COM_ESCUDOS)
                    .build()) {
        @Override
        public ShieldDefenseRetention resolveShieldDefenseRetention(final int shieldAttacks) {
            return ShieldDefenseRetention.HALF;
        }
    },

    /**
     * "Você não perde Bônus Defensivo ao atacar com Escudos", plus +2 Margem Crítica Menor and a
     * Corrente de Efeitos on criticals. <b>Real.</b> "Feitos em seus Turnos" is {@code
     * CombatantSheet#isInOwnTurn()}; the Corrente is {@link Rugido}, added to a critical hit whether or
     * not the Corrente threshold was cleared (⚠️ "seus Acertos Críticos recebem" read as the critical
     * being the trigger).
     */
    ARTE_DO_ESCUDO_ATACANTE(
            "Você não perde Bônus Defensivo ao atacar com Escudos. Seus ataques com Escudos feitos "
                    + "em seus Turnos tem a Margem Crítica Menor aumentada em +2 números e seus "
                    + "Acertos Críticos recebem a Corrente de Efeitos – Rugido.",
            FeatRequirements.builder()
                    .requiredFeat(ATAQUE_MULTIPLO_COM_ESCUDOS)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public ShieldDefenseRetention resolveShieldDefenseRetention(final int shieldAttacks) {
            return ShieldDefenseRetention.FULL;
        }

        @Override
        public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                                   final Character character, final AttackSource attackSource,
                                                   final CombatantSheet holder, final CombatantSheet attackTarget) {
            return attackSource instanceof ShieldAttack && holder != null && holder.isInOwnTurn()
                    ? ARTE_MARGIN_INCREASE : 0;
        }

        @Override
        public List<EffectChain> resolveCriticalHitEffectChains(final Character attacker, final SkillType attackSkill,
                                                                final AttackSource attackSource,
                                                                final CombatantSheet holder) {
            return attackSource instanceof ShieldAttack && holder != null && holder.isInOwnTurn()
                    ? List.of(new Rugido(true)) : List.of();
        }
    },

    /**
     * "A GD de seu primeiro Ataque com Escudo de cada um de seus Turnos é reduzida em -1 nível."
     * <b>Real</b> as a GD reduction — which {@code AttackDelivery} reports as {@code
     * unappliedDifficultyReduction} against a flat Defesa, as it does every nível.
     *
     * <p>"Após ser bem-sucedido em realizar um Empurrão Violento" is a critical Ataque com Escudo that
     * landed this Turn — Arte do Escudo Atacante's Rugido (table ruling). Until the Turn ends the
     * shield then has Alcance Estendido: "Distância de Ataque aumenta +1UD" ({@link
     * #resolveAttackReachIncrease}) and "Bônus de +1 em rolagens de Ataque e Dano [contra] alvos não
     * adjacentes".
     */
    DOMINIO_DA_ARTE_DO_ESCUDO_ATACANTE(
            "A GD de seu primeiro Ataque com Escudo de cada um de seus Turnos é reduzida em -1 "
                    + "nível. Após ser bem-sucedido em realizar um Empurrão Violento seu Escudo "
                    + "recebe Alcance Estendido como Aprimoramento até o final do Turno.",
            FeatRequirements.builder()
                    .requiredFeat(ARTE_DO_ESCUDO_ATACANTE)
                    .build()) {
        @Override
        public int resolveAttackCostDifficultyReduction(final SkillType skillType, final SceneContext sceneContext,
                final Character character, final AttackSource attackSource, final ActionCost actionCost,
                final List<CombatantAction> actionsThisRound, final CombatantSheet holder) {
            if (!(attackSource instanceof ShieldAttack) || holder == null || !holder.isInOwnTurn()) {
                return 0;
            }
            boolean earlier = holder.getActionsThisTurn().stream()
                    .anyMatch(action -> action.attackSource() instanceof ShieldAttack);
            return earlier ? 0 : 1;
        }

        @Override
        public int resolveAttackReachIncrease(final CombatantSheet attacker, final AttackSource attackSource) {
            return attackSource instanceof ShieldAttack && pushedThisTurn(attacker) ? ALCANCE_ESTENDIDO_UD : 0;
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character,
                                          final AttackSource attackSource, final CombatantSheet holder,
                                          final SkillRoll skillRoll) {
            CombatantSheet target = sceneContext == null ? null : sceneContext.getOpposedCharacter();
            return attackSource instanceof ShieldAttack && pushedThisTurn(holder)
                    && beyondAdjacent(sceneContext, target) ? ALCANCE_ESTENDIDO_BONUS : 0;
        }

        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType,
                                                         final SceneContext sceneContext,
                                                         final CombatantSheet attackTarget, final Character actor,
                                                         final AttackSource attackSource, final int targetCount,
                                                         final CombatantSheet holder, final SkillRoll skillRoll) {
            return attackSource instanceof ShieldAttack && pushedThisTurn(holder)
                    && beyondAdjacent(sceneContext, attackTarget)
                    ? Optional.of(new DamageBonus(ALCANCE_ESTENDIDO_BONUS, DamageType.FISICO)) : Optional.empty();
        }

        /** Whether a critical Ataque com Escudo landed this Turn — the Rugido "Empurrão Violento". */
        private boolean pushedThisTurn(final CombatantSheet holder) {
            return holder != null && holder.isInOwnTurn()
                    && holder.getCharacter().getFeats().contains(ARTE_DO_ESCUDO_ATACANTE)
                    && holder.getActionsThisTurn().stream().anyMatch(action ->
                    action.attackSource() instanceof ShieldAttack && action.outcome() != null
                            && Boolean.TRUE.equals(action.outcome().succeeded())
                            && action.outcome().criticalResult() != null
                            && action.outcome().criticalResult().isCriticalSuccess());
        }

        private boolean beyondAdjacent(final SceneContext sceneContext, final CombatantSheet target) {
            if (sceneContext == null || target == null) {
                return false;
            }
            Range distance = sceneContext.getDistanceTo(target);
            return distance != null && !distance.isWithin(Range.ADJACENTE);
        }
    },

    /** "Você pode fazer Reações mesmo quando o efeito impedir Reações." */
    // Real, both clauses. ignoresReactionPrevention makes ReactionsService drop negative timed
    // REACTIONS bonuses (Atordoante's), which is exactly what prevents Reações in this core. And
    // permitsPerimeterDefenceAsChargeTarget keeps the holder among MovementReactionService's reactors
    // to an Investida aimed at them, while using a Escudo (the wings count, unless flying). Table
    // ruling (2026-09-28): an Investida's target cannot Defender o Perímetro against the charger.
    // Only a Mestre Escudeiro can.
    MESTRE_ESCUDEIRO(
            "Você pode fazer Reações mesmo quando o efeito impedir Reações. Enquanto estiver "
                    + "utilizando um item do tipo Escudo você pode fazer Reações do tipo Defender "
                    + "o Perímetro mesmo quando for alvo de investidas.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .requiredFeatCategory(FeatCategory.ESCUDEIRO)
                    .requiredFeatCategoryCount(2)
                    .build()) {
        @Override
        public boolean ignoresReactionPrevention(final Character character) {
            return true;
        }

        @Override
        public boolean permitsPerimeterDefenceAsChargeTarget(final CombatantSheet holder) {
            boolean shield = holder.getCharacter().getEquipment().stream()
                    .anyMatch(item -> item.getCategory() == ItemCategory.SHIELD && !item.isDestroyed());
            return shield || holdsWings(holder.getCharacter()) && !holder.isFlying();
        }
    },

    /**
     * "Enquanto portar um Escudo de Corpo e não efetuar ataques você reduz danos sofridos à zero",
     * for 1 + the holder's Título count many attacks. <b>Real</b>, through {@link
     * #resolveDamageNegationBudget}: while a Escudo de Corpo is equipped and the holder has not acted
     * offensively this Rodada, {@code DamageService} reduces a landed hit that would still hurt to zero,
     * spending one use; the uses refill on a Descanso Longo Verdadeiro (table ruling). A hit turned
     * aside entirely spends nothing.
     */
    CRIAR_REFUGIO(
            "Enquanto portar um Escudo de Corpo e não efetuar ataques você reduz danos sofridos à "
                    + "zero. O número de ataques que podem ser reduzidos à zero desta forma é "
                    + "igual à 1 + número de Títulos que você possuir.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .requiredFeatCategory(FeatCategory.ESCUDEIRO)
                    .requiredFeatCategoryCount(2)
                    .build()) {
        @Override
        public int resolveDamageNegationBudget(final CombatantSheet target) {
            Character character = target.getCharacter();
            boolean bodyShield = character.getEquipment().stream()
                    .anyMatch(item -> EspecialistaEmEscudoFeat.isChosenShield(item, ShieldItem.ESCUDO_DE_CORPO));
            return bodyShield && !target.hasActedOffensivelyThisRound() ? 1 + character.getAllTitles().size() : 0;
        }
    },

    /**
     * "Você recebe Bônus de +2 em suas Defesas enquanto não estiver utilizando suas asas para
     * voar", and those Asas count as a Escudo meanwhile. <b>Every clause is real.</b>
     *
     * <ul>
     *   <li>"Possuir Asas" — the Pré-requisito — is holding the Voo movement mode from the Raça or a
     *   Talento ({@link #isEligible}).</li>
     *   <li>The +2 holds while the holder is not flying ({@code CombatantSheet#isFlying()}, or the
     *   context's {@code EnvironmentalState#flying()} with no sheet), and is the wings' own Bônus
     *   Defensivo: attacking with them costs it as a shield's would.</li>
     *   <li>"Suas Asas são consideradas itens do tipo Escudo" — a Escudo Médio (table ruling) for every
     *   "wielding a Escudo" clause: {@link ShieldAttack#wings()}, Escudo Veloz, and Especialista em
     *   Escudo's {@link ShieldSpecialty#ASAS_ADAMANTINAS}. Never a Escudo de Corpo.</li>
     * </ul>
     */
    ASAS_ADAMANTINAS(
            "Você recebe Bônus de +2 em suas Defesas enquanto não estiver utilizando suas asas "
                    + "para voar. Para efeitos diversos, como Talentos e Habilidades, suas Asas "
                    + "são consideradas itens do tipo Escudo enquanto você não estiver voando.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public boolean isEligible(final Character character, final CharacterSheet sheet) {
            return Feat.meetsRequirements(getFeatRequirements(), character, sheet) && hasWings(character);
        }

        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character,
                                        final SceneContext sceneContext, final CombatantSheet holder) {
            // The sheet's own flight, or the context's; with neither to ask, "cannot tell" grants nothing.
            boolean contextFlying = sceneContext != null && sceneContext.getEnvironmentalState().flying();
            boolean flying = holder != null ? holder.isFlying() || contextFlying
                    : sceneContext == null || contextFlying;
            if (flying) {
                return 0;
            }
            return ShieldAttack.retained(holder, ShieldAttack::isWingsAttack, ASAS_ADAMANTINAS_DEFENSE_BONUS);
        }
    },

    /**
     * "Você não é beneficiado por RA, RD e RM, ao invés disso você recebe Bônus de +1 em Defesas
     * para cada um destes efeitos." <b>Real.</b> {@link #forgoesDamageMitigation}: {@code
     * DamageService} applies none of RDS, RA, RD or RM to the holder (nor Pele de Pedra's RDS, nor RA's
     * crit clause), and {@code DefenseService} adds the RDS they would have had plus 1 for each of RA,
     * RD and RM they would have any of.
     */
    BASTIAO_DE_VIDRO(
            "Você não é beneficiado por efeitos de Redução de Danos Sofridos, ao invés disso você "
                    + "recebe Bônus em Defesa igual ao valor que você receberia de Redução de "
                    + "Danos Sofridos. Você não é beneficiado por RA, RD e RM, ao invés disso você "
                    + "recebe Bônus de +1 em Defesas para cada um destes efeitos.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .requiredFeatCategory(FeatCategory.ESCUDEIRO)
                    .requiredFeatCategoryCount(3)
                    .build()) {
        @Override
        public boolean forgoesDamageMitigation() {
            return true;
        }
    };

    /** ESCUDO_VELOZ's "sua iniciativa aumenta em +2". */
    private static final int ESCUDO_VELOZ_INITIATIVE_BONUS = 2;

    /** ASAS_ADAMANTINAS' "Bônus de +2 em suas Defesas" while not flying. */
    private static final int ASAS_ADAMANTINAS_DEFENSE_BONUS = 2;

    /** DEFESA_TARTARUGA's "+2 em suas Defesas por 1 Rodada". */
    private static final int DEFESA_TARTARUGA_BONUS = 2;

    /** INICIO_DEFENSIVO's "+3", "+5" when last, and "nas duas primeiras Rodadas". */
    private static final int INICIO_DEFENSIVO_BONUS = 3;
    private static final int INICIO_DEFENSIVO_LAST_BONUS = 5;
    private static final int INICIO_DEFENSIVO_ROUNDS = 2;

    /** ARTE_DO_ESCUDO_ATACANTE's "+2 números". */
    private static final int ARTE_MARGIN_INCREASE = 2;

    /** Alcance Estendido: "Distância de Ataque aumenta +1UD" and "+1 em rolagens de Ataque e Dano". */
    private static final int ALCANCE_ESTENDIDO_UD = 1;
    private static final int ALCANCE_ESTENDIDO_BONUS = 1;

    private final String description;
    private final FeatRequirements featRequirements;

    EscudeiroFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    /** Whether character holds Asas Adamantinas — whose wings count as a Escudo while not flying. */
    public static boolean holdsWings(final Character character) {
        return character.getFeats().contains(ASAS_ADAMANTINAS);
    }

    /**
     * "Possuir Asas": the Voo movement mode from the Raça or a held Talento — the same sources {@code
     * MovementService#hasMovementMode} reads, minus the sheet-only ones (a Forma's), since a
     * Pré-requisito is judged on the character.
     */
    public static boolean hasWings(final Character character) {
        boolean racial = character.getRace() != null && character.getRace().grantsMovementMode(MovementMode.FLIGHT);
        return racial || character.getFeats().stream().anyMatch(feat -> feat != ASAS_ADAMANTINAS
                && (feat.grantsMovementMode(MovementMode.FLIGHT, character, null)
                || feat.resolveMovementBaseOverride(MovementMode.FLIGHT, character) != null));
    }

    /**
     * An Ataque com Escudo's "Bônus Defensivos do Escudo" for the Defesa it is rolled against — the
     * shield's own DF or DM contribution (Favor included, with the sheet), or the wings' +2.
     */
    private static int shieldDefenseBonus(final ShieldAttack shield, final Character character,
                                          final CombatantSheet holder, final SceneContext sceneContext) {
        if (shield.isWings()) {
            return ASAS_ADAMANTINAS_DEFENSE_BONUS;
        }
        Item item = shield.getShield();
        return holder != null
                ? item.getEffectiveDefenseBonus(shield.getTargetDefense(), holder, sceneContext, null)
                : item.getEffectiveDefenseBonus(shield.getTargetDefense(), character, sceneContext);
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.ESCUDEIRO;
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
