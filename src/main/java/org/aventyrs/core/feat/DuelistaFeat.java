package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CriticalDamage;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.effect.GolpeTrovejante;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.skill.CriticalResult;
import java.util.List;
import org.aventyrs.core.combat.Retaliation;
import org.aventyrs.core.item.AttackMethod;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Talentos de Duelista — weapon mastery, two-weapon fighting, and trading accuracy for damage.
 *
 * <p>Most of this tree is <b>opted into on one attack</b> rather than held as a standing bonus, and
 * three mechanisms carry that (core 0.0.70):
 *
 * <ul>
 *   <li><b>Activation.</b> The attacker names what it spends on the roll ({@code
 *       SkillRoll#getActivatedFeats()}); {@link Feat#permitsActivation} is each Talento's per-use
 *       gate — its Perícia, its "uma vez por Rodada/Turno" read off the holder's log ({@code
 *       CombatantAction#activatedFeats()}), or the attack it must follow — and the roll- and
 *       dano-bonus hooks take the roll, so one choice reaches both rolls it trades between.</li>
 *   <li><b>Rerolling the lowest die.</b> {@code SkillRoll#rerollingLowestDie} records the reroll the
 *       caller threw; {@link Feat#grantsLowestDieReroll} is what makes it legal. The dano roll's twin
 *       is reported on {@code InteractionResult#getDamageLowestDieRerolls()}.</li>
 *   <li><b>What an attack costs.</b> {@code ActionPointsService#getAttackCost} starts from the Perícia
 *       roll's price (2PA — the table's ruling) and applies {@link
 *       Feat#resolveAttackActionPointOverride}/{@link Feat#resolveAttackActionPointAdjustment}.</li>
 * </ul>
 *
 * <p>An extra attack is still the caller's to make — this core resolves one attack per call — but
 * whether the second swing of a pair or an Um-Dois is <em>allowed</em>, and what it costs, is read
 * off the Turn's log.
 */
public enum DuelistaFeat implements Feat {

    /**
     * "Escolha entre um tipo de arma, armas naturais ou magias ofensivas. Receba vantagem nas
     * rolagens de ataque com a arma escolhida."
     *
     * <p><b>Real</b>, through {@link EspecialistaEmArmaFeat} — the acquired, choice-carrying form
     * granted in place of this constant. {@code Feat#resolveSkillRollBonus}'s {@link AttackSource}
     * overload is what lets the Vantagem scope to "com a arma escolhida" rather than any weapon.
     */
    ESPECIALISTA_EM_ARMA(
            "Escolha entre um tipo de arma, armas naturais ou magias ofensivas. Receba vantagem "
                    + "nas rolagens de ataque com a arma escolhida, ou com suas magias ofensivas "
                    + "(que exijam uma rolagem de perícia de Ataque), conforme escolhido.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(2)
                    .build()) {
        /** "Escolha entre um tipo de arma, armas naturais ou magias ofensivas." */
        @Override
        public List<FeatChoice<?>> resolveRequiredChoices(final Character holder) {
            return List.of(FeatChoice.ofOne(AttackMethod.class, List.of(AttackMethod.values())));
        }
    },

    /**
     * "Você pode aumentar o Tempo de Ação de um Ataque Corpo-a-Corpo em +1PA, se o fizer poderá
     * rolar novamente o dado de menor valor em sua rolagem."
     *
     * <p><b>Real.</b> Activated on an Ataque Corpo-a-Corpo, it makes the roll's {@code
     * SkillRoll#rerollingLowestDie} legal and adds 1PA to the attack's price.
     */
    LUTADOR_NATO(
            "Você pode aumentar o Tempo de Ação de um Ataque Corpo-a-Corpo em +1PA, se o fizer "
                    + "poderá rolar novamente o dado de menor valor em sua rolagem. O novo "
                    + "resultado será utilizado, mesmo que seja inferior ao anterior.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(1)
                    .build()) {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return skillType == SkillType.ATAQUE_CORPO_A_CORPO;
        }

        @Override
        public boolean grantsLowestDieReroll(final SkillType skillType) {
            return skillType == SkillType.ATAQUE_CORPO_A_CORPO;
        }

        @Override
        public int resolveAttackActionPointAdjustment(final SkillType skillType, final AttackSource attackSource,
                                                      final CombatantSheet attacker, final Set<Feat> activatedFeats) {
            return skillType == SkillType.ATAQUE_CORPO_A_CORPO && activatedFeats.contains(this)
                    ? REROLL_SURCHARGE : 0;
        }
    },

    /**
     * "Você pode optar por receber Desvantagem em rolagens de 'Ataque Corpo-a-Corpo' para receber
     * vantagem em sua rolagem de dano."
     *
     * <p><b>Real.</b> Activated on an Ataque Corpo-a-Corpo: Desvantagem on the roll, Vantagem on its
     * dano — or, when Lutador Nato is activated on the same roll, "+1d6" on the dano instead of the
     * Vantagem ({@code InteractionResult#getExtraDamageDice()}).
     */
    ATAQUE_CONCENTRADO(
            "Você pode optar por receber Desvantagem em rolagens de ‘Ataque Corpo-a-Corpo’ para "
                    + "receber vantagem em sua rolagem de dano. Se utilizar este Talento ao mesmo "
                    + "tempo que Lutador Nato, ao invés da Vantagem na rolagem de Dano, seu dano "
                    + "aumenta em +1d6.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.STRENGTH)
                    .requiredAttributeValue(4)
                    .requiredFeat(LUTADOR_NATO)
                    .build()) {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return skillType == SkillType.ATAQUE_CORPO_A_CORPO;
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character,
                                          final AttackSource attackSource, final CombatantSheet holder,
                                          final SkillRoll skillRoll) {
            return activatedOn(this, skillType, skillRoll) ? Skill.DISADVANTAGE_MALUS : 0;
        }

        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType,
                                                         final SceneContext sceneContext,
                                                         final CombatantSheet attackTarget, final Character actor,
                                                         final AttackSource attackSource, final int targetCount,
                                                         final CombatantSheet holder, final SkillRoll skillRoll) {
            return activatedOn(this, attackingSkillType, skillRoll) && !skillRoll.activated(LUTADOR_NATO)
                    ? Optional.of(new DamageBonus(Skill.ADVANTAGE_BONUS, DamageType.FISICO))
                    : Optional.empty();
        }

        @Override
        public int resolveExtraDamageDice(final SkillType attackSkill, final AttackSource attackSource,
                                          final CombatantSheet holder, final SkillRoll skillRoll) {
            return activatedOn(this, attackSkill, skillRoll) && skillRoll.activated(LUTADOR_NATO) ? 1 : 0;
        }
    },

    /**
     * "Você recebe Vantagem em suas rolagens de Ataque Corpo-a-Corpo contra alvos adjacentes."
     * <b>Both halves are real.</b>
     *
     * <p>On a Perícia de Ataque roll {@code SceneContext#getOpposedCharacter()} is the target, so
     * "alvos adjacentes" is {@code getDistanceTo(target).isWithin(Range.ADJACENTE)} — the same
     * reading {@code AnaoFeat#VANTAGEM_DE_TAMANHO} takes of its own opponent clause. The dano
     * reroll ("Sempre que usar o talento Lutador Nato contra um alvo adjacente…") is reported on
     * {@code InteractionResult#getDamageLowestDieRerolls()} when the roll activated Lutador Nato
     * against an adjacent attack target.
     */
    LUTAR_ENGAJADO(
            "Você recebe Vantagem em suas rolagens de Ataque Corpo-a-Corpo contra alvos "
                    + "adjacentes. Sempre que usar o talento Lutador Nato contra um alvo adjacente "
                    + "poderá também rolar novamente o dado de menor valor em suas rolagens de "
                    + "Dano.",
            FeatRequirements.builder()
                    .alternative(FeatRequirements.builder()
                            .requiredFeat(LUTADOR_NATO)
                            .build())
                    .alternative(FeatRequirements.builder()
                            .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                            .requiredSkillGraduation(4)
                            .build())
                    .build()) {
        /**
         * Returns 0 with no Scene, or with no target to measure against: the clause is scoped to
         * adjacency, so granting it unconditionally would hand the Vantagem to every melee swing
         * including the ones made at reach.
         */
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            if (skillType != SkillType.ATAQUE_CORPO_A_CORPO || sceneContext == null) {
                return 0;
            }
            return isAdjacent(sceneContext, sceneContext.getOpposedCharacter()) ? Skill.ADVANTAGE_BONUS : 0;
        }

        @Override
        public int resolveDamageLowestDieRerolls(final SkillType attackSkill, final SceneContext sceneContext,
                                                 final CombatantSheet holder, final CombatantSheet attackTarget,
                                                 final SkillRoll skillRoll) {
            return activatedOn(LUTADOR_NATO, attackSkill, skillRoll) && isAdjacent(sceneContext, attackTarget) ? 1 : 0;
        }
    },

    /**
     * "Enquanto empunhar 2 armas simultaneamente você poderá realizar dois ataques, um com cada
     * uma de suas armas, ao custo de 3PA."
     *
     * <p><b>Real.</b> Activated on each of the two attacks: the first of a pair costs 3PA and the
     * second nothing more ({@code ActionPointsService#getAttackCost}); the second is allowed only with
     * the <em>other</em> drawn weapon, read off the Turn's log. Both rolls take Desvantagem, and both
     * dano rolls another one unless a drawn weapon is {@link ItemWeightClass#LIGHT} — ⚠️ table ruling:
     * with no per-hand tracking, "a arma na mão inábil" is read as "the lighter one", so the penalty
     * applies only when neither weapon is light.
     */
    COMBATER_COM_2_ARMAS(
            "Enquanto empunhar 2 armas simultaneamente você poderá realizar dois ataques, um com "
                    + "cada uma de suas armas, ao custo de 3PA. Rolagens de Perícias de Ataque "
                    + "feitas desta forma sofrem Desvantagem. Se a arma na mão inábil não for uma "
                    + "arma leve adicionalmente você sofre Desvantagem nas rolagens de Danos.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(3)
                    .build()) {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            if (!skillType.isAttackSkill() || holder == null) {
                return false;
            }
            List<Weapon> drawn = holder.getCharacter().getDrawnWeapons();
            if (drawn.size() < 2 || !(attackSource instanceof Weapon weapon) || !drawn.contains(weapon)) {
                return false;
            }
            AttackSource opener = pairOpener(holder);
            return opener == null || !opener.equals(weapon);
        }

        @Override
        public Integer resolveAttackActionPointOverride(final SkillType skillType, final AttackSource attackSource,
                                                        final CombatantSheet attacker, final Set<Feat> activatedFeats) {
            if (!activatedFeats.contains(this)) {
                return null;
            }
            return pairOpener(attacker) == null ? TWO_WEAPON_PAIR_COST : 0;
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character,
                                          final AttackSource attackSource, final CombatantSheet holder,
                                          final SkillRoll skillRoll) {
            return skillType.isAttackSkill() && skillRoll != null && skillRoll.activated(this)
                    ? Skill.DISADVANTAGE_MALUS : 0;
        }

        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType,
                                                         final SceneContext sceneContext,
                                                         final CombatantSheet attackTarget, final Character actor,
                                                         final AttackSource attackSource, final int targetCount,
                                                         final CombatantSheet holder, final SkillRoll skillRoll) {
            if (skillRoll == null || !skillRoll.activated(this)) {
                return Optional.empty();
            }
            boolean anyLight = actor.getDrawnWeapons().stream()
                    .anyMatch(weapon -> weapon.getEffectiveWeightClass() == ItemWeightClass.LIGHT);
            return anyLight ? Optional.empty()
                    : Optional.of(new DamageBonus(Skill.DISADVANTAGE_MALUS, DamageType.FISICO));
        }

        /**
         * The weapon that opened a pair still owed its second attack this Turn, or {@code null} —
         * an odd count of this Talento's activations this Turn means the last one is unanswered.
         */
        private AttackSource pairOpener(final CombatantSheet holder) {
            if (holder == null) {
                return null;
            }
            List<CombatantAction> pair = holder.getActionsThisTurn().stream()
                    .filter(action -> action.activated(this))
                    .toList();
            return pair.size() % 2 == 1 ? pair.get(pair.size() - 1).attackSource() : null;
        }
    },

    /**
     * "Você recebe um bônus de +1 em DF enquanto estiver empunhando 2 armas", scaling to +3/+5 by
     * Graduação em Ataque Corpo-a-Corpo.
     *
     * <p><b>Real.</b> "Empunhando 2 armas" means two drawn weapons ({@code
     * Character#getDrawnWeapons()}). Whether either one attacked is read from the Rodada's action
     * log, matching each action's {@code AttackSource} against the drawn weapons. "Reduzidos à zero
     * por 1 Rodada se ambas as armas foram utilizadas para atacar em seu Turno" reads the holder's
     * own most recent Turn ({@code CombatantSheet#getActionsOfLatestOwnTurn()}), which survives the
     * Rodada wrap until the holder's next Turn begins — the table's reading of "por 1 Rodada". It
     * needs a sheet, so the {@code Character}-only overloads grant nothing.
     */
    DEFESA_COM_2_ARMAS(
            "Você recebe um bônus de +1 em DF enquanto estiver empunhando 2 armas, e bônus "
                    + "cumulativo de +2 em DF (para um total de +3) se não utilizar nenhuma de "
                    + "suas armas para atacar. Este Bônus aumenta para +2/+4 se tiver 4 ou mais "
                    + "Graduações em Ataque Corpo-a-Corpo, e para +3/+5 se tiver 7 ou mais "
                    + "Graduações. Estes bônus em DF são reduzidos à zero por 1 Rodada se ambas as "
                    + "armas foram utilizadas para atacar em seu Turno, a menos que você tenha 10 "
                    + "Graduações.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ESQUIVA_E_APARAR)
                    .requiredSkillGraduation(3)
                    .requiredFeat(COMBATER_COM_2_ARMAS)
                    .build()) {
        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character,
                                        final SceneContext sceneContext, final CombatantSheet holder) {
            if (defenseType != DefenseType.PHYSICAL || holder == null
                    || character.getDrawnWeapons().size() < 2) {
                return 0;
            }
            int graduation = character.getEffectiveGraduation(SkillType.ATAQUE_CORPO_A_CORPO);
            int wielding = graduation >= 7 ? 3 : graduation >= 4 ? 2 : 1;
            long attackedThisRound = drawnWeaponsThatAttacked(character, holder.getActionsThisRound());
            long attackedLastOwnTurn = drawnWeaponsThatAttacked(character, holder.getActionsOfLatestOwnTurn());
            if ((attackedThisRound >= 2 || attackedLastOwnTurn >= 2)
                    && graduation < TWO_WEAPON_DEFENSE_KEEPS_BONUS_GRADUATION) {
                return 0;
            }
            if (attackedThisRound == 0) {
                return wielding + TWO_WEAPON_DEFENSE_IDLE_BONUS;
            }
            return wielding;
        }

        private long drawnWeaponsThatAttacked(final Character character, final List<CombatantAction> actions) {
            return actions.stream()
                    .map(CombatantAction::attackSource)
                    .filter(Objects::nonNull)
                    .filter(source -> character.getDrawnWeapons().contains(source))
                    .distinct()
                    .count();
        }
    },

    /**
     * "Você recebe Vantagem em todas as suas rolagens de ataque feitas com qualquer arma ou se
     * estiver desarmado." <b>Every clause is real.</b>
     *
     * <p>"Com qualquer arma ou se estiver desarmado" enumerates every way an attack can be made,
     * so it needs no condition at all — contrast {@code ArtesMarciaisFeat#ARTISTA_MARCIAL}, whose
     * clause covers only *some* of those cases. The "-1PA (mínimo 1PA)" is {@link
     * #resolveAttackActionPointAdjustment} on the first attack each Rodada made with the method
     * chosen in Especialista em Arma — ⚠️ table ruling: the first <em>with that method</em>, even if
     * another attack came earlier. "Apenas personagens que não escolheram Magias Ofensivas" is an
     * {@link #isEligible(Character, CharacterSheet)} override reading that same choice.
     */
    DOMINAR_ARMAS(
            "Você recebe Vantagem em todas as suas rolagens de ataque feitas com qualquer arma ou "
                    + "se estiver desarmado. Adicionalmente o primeiro ataque que fizer a cada "
                    + "Rodada, com a arma escolhida no talento ‘Especialista em Arma’, tem seu "
                    + "Tempo de Ação reduzido em -1PA (mínimo 1PA). Apenas personagens que não "
                    + "escolheram Magias Ofensivas podem adquirir este Talento.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(6)
                    .requiredFeat(ESPECIALISTA_EM_ARMA)
                    .build()) {
        /** Unconditional across both Perícias de Ataque — no Scene or target is consulted. */
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character) {
            return skillType.isAttackSkill() ? Skill.ADVANTAGE_BONUS : 0;
        }

        @Override
        public boolean isEligible(final Character character, final CharacterSheet sheet) {
            return Feat.meetsRequirements(getFeatRequirements(), character, sheet)
                    && EspecialistaEmArmaFeat.chosenBy(character)
                            .filter(method -> method == AttackMethod.OFFENSIVE_MAGIC)
                            .isEmpty();
        }

        @Override
        public int resolveAttackActionPointAdjustment(final SkillType skillType, final AttackSource attackSource,
                                                      final CombatantSheet attacker, final Set<Feat> activatedFeats) {
            if (!skillType.isAttackSkill() || attacker == null) {
                return 0;
            }
            Character character = attacker.getCharacter();
            Optional<AttackMethod> chosen = EspecialistaEmArmaFeat.chosenBy(character);
            if (chosen.isEmpty() || !chosen.get().matches(attackSource, character)) {
                return 0;
            }
            boolean earlier = attacker.getActionsThisRound().stream()
                    .anyMatch(action -> action.isAttack() && chosen.get().matches(action.attackSource(), character));
            return earlier ? 0 : -DOMINAR_ARMAS_REDUCTION;
        }
    },

    /**
     * "Seus ataques possuem a Margem Crítica Menor aumentada em +1 e recebem a Corrente de
     * Efeitos – Golpe Trovejante." <b>Both halves are real.</b>
     *
     * <p>{@link EspecialistaEmArmaFeat#chosenBy} makes "o método escolhido" readable, and both
     * {@code Feat#resolveCriticalMarginIncrease(SkillType, SceneContext, Character, AttackSource)}
     * and {@link #resolveEffectChains} take the {@link AttackSource} the match needs. Golpe
     * Trovejante is {@link GolpeTrovejante}: when the Corrente threshold is cleared on a critical,
     * {@code AttackDelivery} applies the weapon's natural Efeito Crítico once more.
     */
    MAESTRIA_EM_ARMA(
            "Enquanto estiver utilizando o método escolhido para atacar no Talento ‘Especialista "
                    + "em Arma’, seus ataques possuem a Margem Crítica Menor aumentada em +1 e "
                    + "recebem a Corrente de Efeitos – Golpe Trovejante.",
            FeatRequirements.builder()
                    .requiredFeat(DOMINAR_ARMAS)
                    .build()) {
        @Override
        public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                                   final Character character, final AttackSource attackSource) {
            return skillType.isAttackSkill() && usingChosenMethod(character, attackSource)
                    ? MAESTRIA_EM_ARMA_MARGIN_INCREASE : 0;
        }

        @Override
        public List<EffectChain> resolveEffectChains(final Character attacker, final SkillType attackSkill,
                                                     final AttackSource attackSource) {
            return attackSkill.isAttackSkill() && usingChosenMethod(attacker, attackSource)
                    ? List.of(new GolpeTrovejante()) : List.of();
        }

        private boolean usingChosenMethod(final Character character, final AttackSource attackSource) {
            Optional<AttackMethod> chosen = EspecialistaEmArmaFeat.chosenBy(character);
            return chosen.isPresent() && chosen.get().matches(attackSource, character);
        }
    },

    /**
     * "Você pode escolher receber Desvantagem em sua Rolagem de Danos para adquirir Vantagem em sua
     * Rolagem de Perícia de Ataque." <b>Real</b> — {@link #ATAQUE_CONCENTRADO} inverted, activated on
     * any Perícia de Ataque ("ao desferir um ataque").
     */
    ATAQUE_RAPIDO(
            "Ao desferir um ataque você pode escolher receber Desvantagem em sua Rolagem de Danos "
                    + "para adquirir Vantagem em sua Rolagem de Perícia de Ataque.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(2)
                    .build()) {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return skillType.isAttackSkill();
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character,
                                          final AttackSource attackSource, final CombatantSheet holder,
                                          final SkillRoll skillRoll) {
            return skillType.isAttackSkill() && skillRoll != null && skillRoll.activated(this)
                    ? Skill.ADVANTAGE_BONUS : 0;
        }

        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType,
                                                         final SceneContext sceneContext,
                                                         final CombatantSheet attackTarget, final Character actor,
                                                         final AttackSource attackSource, final int targetCount,
                                                         final CombatantSheet holder, final SkillRoll skillRoll) {
            return attackingSkillType.isAttackSkill() && skillRoll != null && skillRoll.activated(this)
                    ? Optional.of(new DamageBonus(Skill.DISADVANTAGE_MALUS, DamageType.FISICO))
                    : Optional.empty();
        }
    },

    /**
     * "Uma vez por Turno você pode fazer uma rolagem de Perícia de Ataque ao tempo de 1PA." <b>Real.</b>
     * Activated: once per Turn ({@code countFeatActivationsThisTurn}), priced at 1PA ({@link
     * #resolveAttackActionPointOverride}), and a Meio-Dano on every target ({@link #halvesAttackDamage}).
     */
    ATAQUE_REPENTINO(
            "Você é capaz de fazer um ataque de grande velocidade, mas que causa poucos danos. Uma "
                    + "vez por Turno você pode fazer uma rolagem de Perícia de Ataque ao tempo de "
                    + "1PA. Os danos causados por este ataque são reduzidos à metade, este é um "
                    + "efeito de Meio-Dano.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(4)
                    .requiredFeat(ATAQUE_RAPIDO)
                    .build()) {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return skillType.isAttackSkill() && holder != null && holder.countFeatActivationsThisTurn(this) == 0;
        }

        @Override
        public Integer resolveAttackActionPointOverride(final SkillType skillType, final AttackSource attackSource,
                                                        final CombatantSheet attacker, final Set<Feat> activatedFeats) {
            return activatedFeats.contains(this) ? SUDDEN_ATTACK_COST : null;
        }

        @Override
        public boolean halvesAttackDamage(final SkillType attackSkill, final AttackSource attackSource,
                                          final CombatantSheet attacker, final SkillRoll skillRoll) {
            return skillRoll != null && skillRoll.activated(this);
        }
    },

    /**
     * "Enquanto estiver cego ou privado de seus sentidos visuais você não fica Desprevenido em função
     * destas condições." <b>Every clause is real.</b>
     *
     * <p>"Não fica Desprevenido em função destas condições" is {@link #suppressesImpliedCondition}
     * vetoing Cego → Desprevenido. "Não precisa efetuar rolagens de 1d6 para utilizar efeitos pessoais
     * e Ataques Corpo-a-Corpo" is {@link #exemptsFromBlindCheck} — every Perícia but Ataque à
     * Distância, whose 1d6 still stands. Its price, "rolagens de Perícias feitas às cegas recebem
     * Desvantagem", is a Desvantagem on every Perícia roll made while Cego (sheet-aware
     * resolveSkillRollBonus).
     */
    COMBATER_AS_CEGAS(
            "Enquanto estiver cego ou privado de seus sentidos visuais você não fica Desprevenido "
                    + "em função destas condições e não precisa efetuar rolagens de 1d6 para "
                    + "utilizar efeitos pessoais e Ataques Corpo-a-Corpo. Para receber os "
                    + "benefícios deste Talento, rolagens de Perícias feitas às cegas recebem "
                    + "Desvantagem.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.INSTINCT)
                    .requiredAttributeValue(3)
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(4)
                    .build()) {
        @Override
        public boolean suppressesImpliedCondition(final ConditionType implier, final ConditionType implied) {
            return implier == ConditionType.CEGO && implied == ConditionType.DESPREVENIDO;
        }

        @Override
        public boolean exemptsFromBlindCheck(final SkillType skillType) {
            return skillType != SkillType.ATAQUE_A_DISTANCIA;
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character,
                                          final AttackSource attackSource, final CombatantSheet holder) {
            return holder != null && holder.hasCondition(ConditionType.CEGO, sceneContext)
                    ? Skill.DISADVANTAGE_MALUS : 0;
        }
    },

    /**
     * "Você pode adicionar metade do seu valor de Vigor à sua rolagem de danos físicos
     * corpo-a-corpo, se o fizer e for bem-sucedido você sofre 2 pontos de Dano Físico Primordial."
     *
     * <p><b>Real</b>, as an activation — which is what keeps the price from being skipped: half Vigor
     * reaches the dano of an Ataque Corpo-a-Corpo that is not a Magia only when the roll names it,
     * and a hit then reports {@link #resolveLockedSelfDamageOnHit} (2) on {@code
     * DeliveredAttackResult#getLockedSelfDamage()}, which the caller pays with {@code
     * CombatantSheet#payWithVitality} — "não podem ser reduzidos e são recuperados apenas com
     * Descansos Verdadeiros" is exactly that method.
     */
    FORCA_EXCESSIVA(
            "Você pode adicionar metade do seu valor de Vigor à sua rolagem de danos físicos "
                    + "corpo-a-corpo, se o fizer e for bem-sucedido você sofre 2 pontos de Dano "
                    + "Físico Primordial. Danos sofridos desta forma não podem ser reduzidos e são "
                    + "recuperados apenas com Descansos Verdadeiros.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.STRENGTH)
                    .requiredAttributeValue(3)
                    .build()) {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return skillType == SkillType.ATAQUE_CORPO_A_CORPO && !(attackSource instanceof Spell);
        }

        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType,
                                                         final SceneContext sceneContext,
                                                         final CombatantSheet attackTarget, final Character actor,
                                                         final AttackSource attackSource, final int targetCount,
                                                         final CombatantSheet holder, final SkillRoll skillRoll) {
            if (!activatedOn(this, attackingSkillType, skillRoll) || attackSource instanceof Spell) {
                return Optional.empty();
            }
            int halfVigor = actor.getEffectiveAttributeTotal(AttributeDomain.VIGOR) / 2;
            return halfVigor == 0 ? Optional.empty() : Optional.of(new DamageBonus(halfVigor, DamageType.FISICO));
        }

        @Override
        public int resolveLockedSelfDamageOnHit(final SkillType attackSkill, final AttackSource attackSource,
                                                final CombatantSheet attacker, final SkillRoll skillRoll) {
            return activatedOn(this, attackSkill, skillRoll) ? FORCA_EXCESSIVA_SELF_DAMAGE : 0;
        }
    },

    /**
     * "Ao realizar um ataque com uma Arma ou Ataque Desarmado seu tipo de ataque muda para Área de
     * Efeito – Explosão." <b>Real</b> when the roll names it: an attack with a Weapon or an Ataque
     * Desarmado — anything but a Magia — becomes an AreaOfEffect#ATTACK_EXPLOSION
     * (Feat#resolveAttackArea). "Apenas uma vez por Rodada" is its {@link #permitsActivation},
     * counted off the Rodada's log.
     */
    ATAQUE_GIRATORIO(
            "Apenas uma vez por Rodada, ao realizar um ataque com uma Arma ou Ataque Desarmado seu "
                    + "tipo de ataque muda para Área de Efeito – Explosão.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .requiredFeatCategory(FeatCategory.DUELISTA)
                    .requiredFeatCategoryCount(4)
                    .build()) {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return skillType.isAttackSkill() && !(attackSource instanceof Spell)
                    && holder != null && holder.countFeatActivationsThisRound(this) == 0;
        }

        @Override
        public AreaOfEffect resolveAttackArea(final Character attacker, final SkillType attackSkill,
                                              final AttackSource attackSource, final SkillRoll skillRoll) {
            return skillRoll != null && skillRoll.activated(ATAQUE_GIRATORIO) && !(attackSource instanceof Spell)
                    ? AreaOfEffect.ATTACK_EXPLOSION : null;
        }
    },

    /**
     * "Após ser bem-sucedido em um Ataque Rápido você pode fazer imediatamente um Ataque Concentrado
     * ao custo de 1PA." <b>Real.</b> Activated together with Ataque Concentrado, and only when the
     * Turn's last action was a landed attack that activated Ataque Rápido ("imediatamente"); priced at
     * 1PA. The attack itself is the caller's to make, like every attack.
     */
    UM_DOIS(
            "Após ser bem-sucedido em um Ataque Rápido você pode fazer imediatamente um Ataque "
                    + "Concentrado ao custo de 1PA.",
            FeatRequirements.builder()
                    .requiredFeat(ATAQUE_CONCENTRADO)
                    .requiredFeat(ATAQUE_RAPIDO)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            if (!skillType.isAttackSkill() || skillRoll == null || !skillRoll.activated(ATAQUE_CONCENTRADO)
                    || holder == null) {
                return false;
            }
            List<CombatantAction> turn = holder.getActionsThisTurn();
            if (turn.isEmpty()) {
                return false;
            }
            CombatantAction previous = turn.get(turn.size() - 1);
            return previous.isAttack() && previous.activated(ATAQUE_RAPIDO)
                    && previous.outcome() != null && Boolean.TRUE.equals(previous.outcome().succeeded());
        }

        @Override
        public Integer resolveAttackActionPointOverride(final SkillType skillType, final AttackSource attackSource,
                                                        final CombatantSheet attacker, final Set<Feat> activatedFeats) {
            return activatedFeats.contains(this) ? UM_DOIS_COST : null;
        }
    },

    /**
     * "Seu segundo ataque contra um mesmo alvo na mesma Rodada tem a Margem Crítica Menor aumentada
     * em +2 números." <b>Both halves are real.</b> The critical dano half is Metade da Gnose as a flat
     * CriticalDamage, which AbstractSkillInteraction adds only on an Acerto Crítico. The Margem half
     * counts the holder's attacks this Rodada whose primary target was this one ({@code
     * CombatantSheet#countAttacksAgainstThisRound}) — ⚠️ table ruling: exactly the second, not the
     * third or later.
     */
    EXPLORAR_PONTOS_FRACOS(
            "Seu segundo ataque contra um mesmo alvo na mesma Rodada tem a Margem Crítica Menor "
                    + "aumentada em +2 números. Seus Acertos Críticos causam Metade da Gnose como "
                    + "dano adicional.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(4)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                                   final Character character, final AttackSource attackSource,
                                                   final CombatantSheet holder, final CombatantSheet attackTarget) {
            CombatantSheet target = attackTarget != null ? attackTarget
                    : sceneContext == null ? null : sceneContext.getOpposedCharacter();
            if (!skillType.isAttackSkill() || holder == null || target == null) {
                return 0;
            }
            return holder.countAttacksAgainstThisRound(target.getId()) == 1 ? EXPLORAR_PONTOS_FRACOS_MARGIN_INCREASE : 0;
        }

        @Override
        public CriticalDamage resolveCriticalDamage(final SkillType attackSkill, final SceneContext sceneContext,
                                                    final Character character, final AttackSource attackSource,
                                                    final CriticalResult criticalResult, final SkillRoll skillRoll) {
            return CriticalDamage.ofFlat(halfGnose(character));
        }
    },

    /**
     * "O dano adicional de Metade da Gnose não pode ser curado, exceto por Descansos Verdadeiros e
     * Roubo de Vida." <b>Real.</b> On a critical, {@code AttackDelivery} marks each target's chain head
     * with Explorar Pontos Fracos' Metade da Gnose ({@link #resolveUnhealableCriticalDamage}), and the
     * victim locks that much of what landed ({@code CombatantSheet#lockDamage(int, true)}): ordinary
     * healing stops short of it, Roubo de Vida and a Descanso Verdadeiro do not.
     */
    FERIDAS_ARDENTES(
            "O dano adicional de Metade da Gnose, causado em seus Danos Críticos, não pode ser "
                    + "curado, exceto por Descansos Verdadeiros e efeitos de Roubo de Vida.",
            FeatRequirements.builder()
                    .requiredFeat(EXPLORAR_PONTOS_FRACOS)
                    .build()) {
        @Override
        public int resolveUnhealableCriticalDamage(final SkillType attackSkill, final AttackSource attackSource,
                                                   final Character attacker, final CriticalResult criticalResult) {
            return attackSkill.isAttackSkill() && attacker.getFeats().contains(EXPLORAR_PONTOS_FRACOS)
                    ? halfGnose(attacker) : 0;
        }
    },

    /**
     * "Você pode substituir sua rolagem de Defesa Física por uma Rolagem de Perícia de Ataque."
     * <b>Real</b>, on {@code AttackReceiver}: an {@code IncomingAttack} naming a {@code defenseSkill}
     * rolls that Perícia de Ataque in place of the whole defence, provided the roll activates this
     * Talento — "apenas uma vez por Rodada" is its {@link #permitsActivation}. A held defence reports
     * the defender's Força as {@code IncomingAttackResult#getCounterWeaponDamage()} for the caller to
     * deal to the weapon struck with ({@code Item#applyDamage}); a failed one reports {@code
     * attackerDamageAdvantage}, the attacker's Vantagem na rolagem de danos.
     */
    DEFENDER_SE_ATACANDO(
            "Apenas uma vez por Rodada, você pode substituir sua rolagem de Defesa Física por uma "
                    + "Rolagem de Perícia de Ataque. Se for bem-sucedido você evita o ataque que "
                    + "lhe seria causado e inflige Força pontos de danos à arma ou projétil "
                    + "utilizado, se for malsucedido o atacante recebe Vantagem na rolagem de "
                    + "danos deste ataque.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .requiredFeatCategory(FeatCategory.DUELISTA)
                    .requiredFeatCategoryCount(3)
                    .build()) {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return skillType.isAttackSkill() && holder != null && holder.countFeatActivationsThisRound(this) == 0;
        }

        @Override
        public boolean permitsDefenseSubstitution(final DefenseType defenseType, final boolean enchantmentOrCurse) {
            return defenseType == DefenseType.PHYSICAL;
        }
    },

    /**
     * "Você pode usar o Talento Defender-se Atacando também em substituição à Defesa Mágica." <b>Real.</b>
     * Widens {@link #DEFENDER_SE_ATACANDO}'s substitution to {@code DefenseType#MAGIC}, "exceto para
     * evitar Encantamentos e Maldições" — {@code IncomingAttack#isEnchantmentOrCurseAttack()}, stated by
     * the caller or read off a Magia source of either {@code MagicType}. The activation and its
     * once-per-Rodada limit stay Defender-se Atacando's.
     */
    DEFENDER_SE_ATACANDO_SUPERIOR(
            "Você pode usar o Talento Defender-se Atacando também em substituição à Defesa Mágica, "
                    + "exceto para evitar Encantamentos e Maldições.",
            FeatRequirements.builder()
                    .requiredFeat(DEFENDER_SE_ATACANDO)
                    .build()) {
        @Override
        public boolean permitsDefenseSubstitution(final DefenseType defenseType, final boolean enchantmentOrCurse) {
            return defenseType == DefenseType.MAGIC && !enchantmentOrCurse;
        }
    },

    /**
     * "Você recebe Resistência à Críticos. Sempre que um atacante Corpo-a-Corpo lhe infligir danos
     * físicos ele também sofre 1 ponto de Dano Físico."
     *
     * <p>One of the five Talentos with no Pré-requisito line; "Vigor 5" is printed as its
     * Pré-requisito and is modelled as such.
     */
    // Both halves are real. Resistência à Críticos: one instance, unconditional (see
    // CombatantSheet#CRITICAL_RESISTANCE_INSTANCE — the clause states no figure). The
    // retaliation: Feat#resolveRetaliation, reported on a landed melee hit and dealt by the caller.
    // ⚠️ "danos físicos" is read as any landed melee hit — nothing types a melee attack as
    // non-physical today.
    // Keeps the unsuffixed name of the two Talentos the rules both call "Coração de Ferro";
    // DestinoFeat's is CORACAO_DE_FERRO_DO_DESTINO. See that constant for why they cannot share
    // one name: a Feat's name() is its persisted identity.
    CORACAO_DE_FERRO(
            "Você recebe Resistência à Críticos. Sempre que um atacante Corpo-a-Corpo lhe infligir "
                    + "danos físicos ele também sofre 1 ponto de Dano Físico, se o ataque for um "
                    + "Acerto Crítico ao invés disso ele sofre 3 pontos de Danos Físicos.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.VIGOR)
                    .requiredAttributeValue(5)
                    .build()) {
        @Override
        public int resolveCriticalResistance(final Character character, final SceneContext sceneContext) {
            return CombatantSheet.CRITICAL_RESISTANCE_INSTANCE;
        }

        @Override
        public Retaliation resolveRetaliation(final Character holder, final AttackSource attackSource,
                                              final Character attacker, final boolean criticalHit) {
            return new Retaliation(criticalHit ? CORACAO_DE_FERRO_CRITICAL_RETALIATION : CORACAO_DE_FERRO_RETALIATION,
                    new DamageDescriptor(DamageType.FISICO), null, 0);
        }
    };

    /** MAESTRIA_EM_ARMA's own stated "+1" to the Margem Crítica Menor. */
    private static final int MAESTRIA_EM_ARMA_MARGIN_INCREASE = 1;

    /** EXPLORAR_PONTOS_FRACOS' "+2 números" on the second attack against one target. */
    private static final int EXPLORAR_PONTOS_FRACOS_MARGIN_INCREASE = 2;

    /** CORACAO_DE_FERRO's "1 ponto de Dano Físico" on an ordinary hit. */
    private static final int CORACAO_DE_FERRO_RETALIATION = 1;

    /** CORACAO_DE_FERRO's "3 pontos de Danos Físicos" when the hit was an Acerto Crítico. */
    private static final int CORACAO_DE_FERRO_CRITICAL_RETALIATION = 3;

    /** DEFESA_COM_2_ARMAS' cumulative "+2" while neither weapon has attacked. */
    private static final int TWO_WEAPON_DEFENSE_IDLE_BONUS = 2;

    /** DEFESA_COM_2_ARMAS' "a menos que você tenha 10 Graduações". */
    private static final int TWO_WEAPON_DEFENSE_KEEPS_BONUS_GRADUATION = 10;

    /** LUTADOR_NATO's "+1PA". */
    private static final int REROLL_SURCHARGE = 1;

    /** COMBATER_COM_2_ARMAS' "dois ataques … ao custo de 3PA". */
    private static final int TWO_WEAPON_PAIR_COST = 3;

    /** ATAQUE_REPENTINO's "ao tempo de 1PA". */
    private static final int SUDDEN_ATTACK_COST = 1;

    /** UM_DOIS' "ao custo de 1PA". */
    private static final int UM_DOIS_COST = 1;

    /** DOMINAR_ARMAS' "-1PA". */
    private static final int DOMINAR_ARMAS_REDUCTION = 1;

    /** FORCA_EXCESSIVA's "2 pontos de Dano Físico Primordial". */
    private static final int FORCA_EXCESSIVA_SELF_DAMAGE = 2;

    private final String description;
    private final FeatRequirements featRequirements;

    DuelistaFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    /** Whether feat was activated on a melee roll — the scope of Lutador Nato and its two riders. */
    private static boolean activatedOn(final Feat feat, final SkillType skillType, final SkillRoll skillRoll) {
        return skillType == SkillType.ATAQUE_CORPO_A_CORPO && skillRoll != null && skillRoll.activated(feat);
    }

    /** Whether target stands adjacent to the roller, by the roller's own SceneContext. */
    private static boolean isAdjacent(final SceneContext sceneContext, final CombatantSheet target) {
        if (sceneContext == null || target == null) {
            return false;
        }
        Range distance = sceneContext.getDistanceTo(target);
        return distance != null && distance.isWithin(Range.ADJACENTE);
    }

    /** "Metade da Gnose" — Explorar Pontos Fracos' critical dano and Feridas Ardentes' locked share. */
    private static int halfGnose(final Character character) {
        return character.getEffectiveAttributeTotal(AttributeDomain.GNOSE) / 2;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.DUELISTA;
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
