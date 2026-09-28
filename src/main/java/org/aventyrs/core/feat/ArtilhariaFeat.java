package org.aventyrs.core.feat;

import java.util.Optional;

import org.aventyrs.core.character.Character;
import java.util.List;
import org.aventyrs.core.character.CriticalDamage;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.item.AttackMethod;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.skill.SkillTrait;
import java.util.Set;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;

/**
 * Talentos de Artilharia — ranged attacks, aiming, and firing more than once.
 *
 * <p>Like {@link DuelistaFeat}, most of the tree is <b>spent on one attack</b> — named in {@code
 * SkillRoll#getActivatedFeats()}, gated per use by {@link Feat#permitsActivation} and priced by
 * {@code ActionPointsService#getAttackCost} (core 0.0.70). An extra attack (Tiro Rápido) stays the
 * caller's to make; an extra projectile (Tiro Duplo, Tiro Múltiplo) is the same one roll with more
 * dano dice and its Correntes and Efeitos Críticos applied again ({@link
 * Feat#resolveAttackEffectRepetitions}).
 */
public enum ArtilhariaFeat implements Feat {

    /**
     * "Você pode aumentar seu tempo de disparo em +1PA quando atacar utilizando a perícia 'ataque
     * à distância', se o fizer poderá rolar novamente o dado de menor valor em sua rolagem."
     */
    // Real (core 0.0.70): the ranged twin of DuelistaFeat#LUTADOR_NATO — activated on an Ataque à
    // Distância, it makes SkillRoll#rerollingLowestDie legal and adds 1PA to the attack's price.
    // ⚠️ "Treinamento em Ataque-à-distância", with no number, is read as Graduação 1 — trained at all.
    MIRA_IMPECAVEL(
            "Você pode aumentar seu tempo de disparo em +1PA quando atacar utilizando a perícia "
                    + "‘ataque à distância’, se o fizer poderá rolar novamente o dado de menor "
                    + "valor em sua rolagem. O novo resultado será utilizado, mesmo que seja "
                    + "inferior ao anterior.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_A_DISTANCIA)
                    .requiredSkillGraduation(1)
                    .build()) {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return skillType == SkillType.ATAQUE_A_DISTANCIA;
        }

        @Override
        public boolean grantsLowestDieReroll(final SkillType skillType) {
            return skillType == SkillType.ATAQUE_A_DISTANCIA;
        }

        @Override
        public int resolveAttackActionPointAdjustment(final SkillType skillType, final AttackSource attackSource,
                                                      final CombatantSheet attacker, final Set<Feat> activatedFeats) {
            return skillType == SkillType.ATAQUE_A_DISTANCIA && activatedFeats.contains(this) ? 1 : 0;
        }
    },

    /**
     * "Escolha um tipo de arma de Ataque a Distância ou de Arremesso, você recebe Vantagem nas
     * rolagens de ataque sempre que atacar inimigos à Distâncias Médias ou superiores."
     *
     * <p><b>Real</b>, through {@link AtiradorPerfeitoFeat} — the acquired, choice-carrying form
     * granted in place of this constant. {@code Feat#resolveSkillRollBonus}'s {@link AttackSource}
     * overload supplies the chosen weapon type; the range condition reads {@code
     * SceneContext#getOpposedCharacter()}/{@code getDistanceTo}, the same way {@code
     * Feat#resolveCriticalMarginIncrease}'s own javadoc points an opponent-conditioned clause to.
     */
    ATIRADOR_PERFEITO(
            "Escolha um tipo de arma de Ataque a Distância ou de Arremesso, você recebe Vantagem "
                    + "nas rolagens de ataque sempre que atacar inimigos à Distâncias Médias ou "
                    + "superiores enquanto utilizando armas do tipo escolhido.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_A_DISTANCIA)
                    .requiredSkillGraduation(2)
                    .build()) {
        /** The same "tipo de arma, armas naturais ou magias ofensivas" pick its Duelista twin makes. */
        @Override
        public List<FeatChoice<?>> resolveRequiredChoices(final Character holder) {
            return List.of(FeatChoice.ofOne(AttackMethod.class, List.of(AttackMethod.values())));
        }
    },

    /**
     * "Sempre que utilizar o talento 'Atirador Perfeito' você recebe também vantagem nas rolagens
     * de dano."
     *
     * <p><b>Real.</b> Atirador Perfeito's benefit is not an opt-in — its Vantagem em Ataque
     * triggers whenever the holder attacks with the chosen weapon type at Distância Média or
     * beyond ({@link AtiradorPerfeitoFeat#matchesConditions}) — so "sempre que utilizar" is that
     * same condition, and this grants a flat {@code Skill#ADVANTAGE_BONUS} to the dano roll under
     * it, untyped so it flattens to {@code FISICO} in {@code DamageBonus#total}.
     */
    ABATER_A_CACA(
            "Sempre que utilizar o talento ‘Atirador Perfeito’ você recebe também vantagem nas "
                    + "rolagens de dano.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_A_DISTANCIA)
                    .requiredSkillGraduation(3)
                    .requiredFeat(ATIRADOR_PERFEITO)
                    .build()) {
        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType,
                                                         final SceneContext sceneContext,
                                                         final CombatantSheet attackTarget, final Character actor,
                                                         final AttackSource attackSource) {
            return atiradorPerfeitoDanoVantagem(actor, sceneContext, attackSource);
        }
    },

    /** "Você recebe Vantagem em suas rolagens de Dano sempre que utilizar o Talento 'Atirador Perfeito'." */
    // Identical effect to ABATER_A_CACA — the source authors both in the same words, so both are
    // catalogued rather than merged, and both resolve through the same helper.
    UM_TIRO_UMA_MORTE(
            "Você recebe Vantagem em suas rolagens de Dano sempre que utilizar o Talento "
                    + "‘Atirador Perfeito’.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_A_DISTANCIA)
                    .requiredSkillGraduation(4)
                    .requiredFeat(ATIRADOR_PERFEITO)
                    .build()) {
        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType,
                                                         final SceneContext sceneContext,
                                                         final CombatantSheet attackTarget, final Character actor,
                                                         final AttackSource attackSource) {
            return atiradorPerfeitoDanoVantagem(actor, sceneContext, attackSource);
        }
    },

    /**
     * "A distância máxima de seus ataques à Distância, físicos e Mágicos, aumentam em +1 nível."
     *
     * <p>The flat "+1 nível" half is real: {@link #resolveAttackRangeIncrease} returns one band
     * for any attack delivered by {@link SkillType#ATAQUE_A_DISTANCIA} — a weapon de Ataque à
     * Distância/Arremesso or a ranged Magia alike, which is the "físicos e Mágicos" scope —
     * and {@code org.aventyrs.core.character.services.AttackRangeService} advances the source's
     * own Alcance by it.
     *
     * <p>The "+1 passo (para o total de +2 níveis)" with Mira Impecável is real too: the attack names
     * Mira Impecável among what it spends, and the activation-aware {@link
     * #resolveAttackRangeIncrease(Character, AttackSource, Set)} adds the second step.
     */
    TIRO_LONGO(
            "A distância máxima de seus ataques à Distância, físicos e Mágicos, aumentam em +1 "
                    + "nível. Sempre que efetuar ataques utilizando dos benefícios do Talento "
                    + "‘Mira Impecável’, a distância dos seus ataques aumenta em +1 passo (para o "
                    + "total de +2 níveis).",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_A_DISTANCIA)
                    .requiredSkillGraduation(3)
                    .build()) {
        @Override
        public int resolveAttackRangeIncrease(final Character character, final AttackSource attackSource) {
            return attackSource != null
                    && attackSource.getAttackSkillType() == SkillType.ATAQUE_A_DISTANCIA ? 1 : 0;
        }

        @Override
        public int resolveAttackRangeIncrease(final Character character, final AttackSource attackSource,
                                              final Set<Feat> activatedFeats) {
            int base = resolveAttackRangeIncrease(character, attackSource);
            return base > 0 && activatedFeats.contains(MIRA_IMPECAVEL) ? base + 1 : base;
        }
    },

    /**
     * "Após fazer um ataque com uma arma à distância você pode fazer um ataque adicional com a
     * mesma arma em desvantagem ao custo de 1PA." <b>Real.</b> The extra attack is the caller's, like
     * every attack; activating this Talento on it is allowed only right after an attack with that same
     * ranged weapon this Turn, once per Rodada ({@link #permitsActivation}), and makes it cost 1PA with
     * Desvantagem.
     */
    TIRO_RAPIDO(
            "Após fazer um ataque com uma arma à distância você pode fazer um ataque adicional com "
                    + "a mesma arma em desvantagem ao custo de 1PA. Este talento pode ser "
                    + "utilizado apenas uma vez por Rodada.",
            FeatRequirements.builder()
                    .requiredFeat(MIRA_IMPECAVEL)
                    .build()) {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            if (skillType != SkillType.ATAQUE_A_DISTANCIA || !(attackSource instanceof Weapon weapon)
                    || holder == null || holder.countFeatActivationsThisRound(this) > 0) {
                return false;
            }
            List<CombatantAction> turn = holder.getActionsThisTurn();
            if (turn.isEmpty()) {
                return false;
            }
            CombatantAction previous = turn.get(turn.size() - 1);
            return previous.isAttack() && weapon.equals(previous.attackSource());
        }

        @Override
        public Integer resolveAttackActionPointOverride(final SkillType skillType, final AttackSource attackSource,
                                                        final CombatantSheet attacker, final Set<Feat> activatedFeats) {
            return activatedFeats.contains(this) ? TIRO_RAPIDO_COST : null;
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character,
                                          final AttackSource attackSource, final CombatantSheet holder,
                                          final SkillRoll skillRoll) {
            return activated(this, skillType, skillRoll) ? Skill.DISADVANTAGE_MALUS : 0;
        }
    },

    /** "Sempre que tiver um Acerto Crítico usando o talento 'Mira Impecável' você causa +1d6 de dano adicional." */
    // Real: the "usando o talento 'Mira Impecável'" scoping that ABATER_A_CACA still waits on is
    // expressible now — SkillRoll#getActivatedFeats() is the roller's statement of which Talentos
    // they spent on this one roll (see that class's own javadoc).
    // Both required Talentos are enforced — FeatRequirements#requiredFeats is a set — and so is
    // "com arma escolhida de ataque à distância ou arremesso", an isEligible override reading
    // Acerto Crítico Aprimorado's recorded choice (AcertoCriticoAprimoradoFeat#chosenBy).
    MIRA_MORTAL(
            "Sempre que tiver um Acerto Crítico usando o talento ‘Mira Impecável’ você causa +1d6 "
                    + "de dano adicional.",
            FeatRequirements.builder()
                    .requiredFeat(MIRA_IMPECAVEL)
                    .requiredFeat(AssassinoFeat.ACERTO_CRITICO_APRIMORADO)
                    .build()) {
        /**
         * The die <b>adds</b> to the crit's baseline Vantagem em Danos rather than replacing it
         * (unlike {@code AssassinoFeat#VIOLENCIA_DESCOMUNAL}): the clause reads "causa +1d6 de dano
         * adicional", with no "ao invés disso". A 1d6 weapon therefore rolls 2d6, +2, on such a crit.
         *
         * <p>Granted only when this attack actually <em>used</em> {@link #MIRA_IMPECAVEL} — holding
         * it and paying its +1PA to reroll the lowest die are different facts, and the clause names
         * the second. The caller states that on the roll; a roll that states nothing gets nothing,
         * rather than the benefit by default.
         */
        @Override
        public CriticalDamage resolveCriticalDamage(final SkillType attackSkill, final SceneContext sceneContext,
                                                    final Character character, final AttackSource attackSource,
                                                    final CriticalResult criticalResult, final SkillRoll skillRoll) {
            return skillRoll != null && skillRoll.activated(MIRA_IMPECAVEL)
                    ? CriticalDamage.ofDice(1) : CriticalDamage.NONE;
        }

        @Override
        public boolean isEligible(final Character character, final CharacterSheet sheet) {
            return Feat.meetsRequirements(getFeatRequirements(), character, sheet)
                    && AcertoCriticoAprimoradoFeat.chosenBy(character).filter(RANGED_METHODS::contains).isPresent();
        }
    },

    /**
     * "Uma vez por Rodada, em seu Turno, você pode disparar um projétil adicional em seus ataques,
     * se o fizer os danos causados pelo ataque aumentam em +1d6."
     */
    // Real: activated on an Ataque à Distância made on the holder's own Turn (not as a Reação), once
    // per Rodada — shared with TIRO_MULTIPLO, which is the same shot with more projectiles. One roll,
    // with Desvantagem; +1d6 on its dano (InteractionResult#getExtraDamageDice); and every triggered
    // Corrente and Efeito Crítico applied once more (Feat#resolveAttackEffectRepetitions).
    TIRO_DUPLO(
            "Uma vez por Rodada, em seu Turno, você pode disparar um projétil adicional em seus "
                    + "ataques, se o fizer os danos causados pelo ataque aumentam em +1d6 "
                    + "independentemente do tipo de projétil utilizado, você recebe Desvantagem "
                    + "nesta rolagem de Ataque à Distância. Correntes de Efeito e Efeitos Críticos "
                    + "aplicam seus efeitos duas vezes.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_A_DISTANCIA)
                    .requiredSkillGraduation(5)
                    .requiredFeatCategory(FeatCategory.ARTILHARIA)
                    .requiredFeatCategoryCount(2)
                    .build()) {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return extraProjectilePermitted(skillType, skillRoll, holder);
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character,
                                          final AttackSource attackSource, final CombatantSheet holder,
                                          final SkillRoll skillRoll) {
            return activated(this, skillType, skillRoll) ? Skill.DISADVANTAGE_MALUS : 0;
        }

        @Override
        public int resolveExtraDamageDice(final SkillType attackSkill, final AttackSource attackSource,
                                          final CombatantSheet holder, final SkillRoll skillRoll) {
            return activated(this, attackSkill, skillRoll) ? 1 : 0;
        }

        @Override
        public int resolveAttackEffectRepetitions(final SkillType attackSkill, final AttackSource attackSource,
                                                  final CombatantSheet attacker, final SkillRoll skillRoll) {
            return activated(this, attackSkill, skillRoll) ? 1 : 0;
        }
    },

    /**
     * "Você pode estender os benefícios de Combater as Cegas aos seus Ataques-a-Distância
     * efetuados contra alvos em até Distância Média."
     */
    // Real: the Pré-requisito names DuelistaFeat#COMBATER_AS_CEGAS, and the benefit it extends — no
    // Cego 1d6 — reaches an Ataque à Distância aimed at a target up to Distância Média, read off the
    // roll's opposed character (Feat#exemptsFromBlindCheck(SkillType, SceneContext)). Não ficar
    // Desprevenido is already Combater às Cegas' own, whatever the attack; its price (Desvantagem
    // às cegas) is not a benefit and is not extended by anything here.
    DISPARO_AS_CEGAS(
            "Você pode estender os benefícios de Combater as Cegas aos seus Ataques-a-Distância "
                    + "efetuados contra alvos em até Distância Média.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_A_DISTANCIA)
                    .requiredSkillGraduation(4)
                    .requiredFeat(DuelistaFeat.COMBATER_AS_CEGAS)
                    .build()) {
        @Override
        public boolean exemptsFromBlindCheck(final SkillType skillType, final SceneContext sceneContext) {
            if (skillType != SkillType.ATAQUE_A_DISTANCIA || sceneContext == null
                    || sceneContext.getOpposedCharacter() == null) {
                return false;
            }
            Range distance = sceneContext.getDistanceTo(sceneContext.getOpposedCharacter());
            return distance != null && distance.isWithin(Range.DISTANCIA_MEDIA);
        }
    },

    /**
     * "Como Tiro Duplo, mas você pode disparar uma quantidade de projéteis adicionais igual a sua
     * quantidade de Títulos Aventyr Despertos (mínimo 2 projéteis, máximo 4 projéteis)."
     */
    // Real — ⚠️ table ruling: it scales per projectile. N = clamp(Títulos Despertos, 2, 4) extra
    // projectiles give +Nd6 dano and every Corrente and Efeito Crítico applied N more times, with
    // one Desvantagem; once per Rodada, sharing Tiro Duplo's use.
    TIRO_MULTIPLO(
            "Como Tiro Duplo, mas você pode disparar uma quantidade de projéteis adicionais igual "
                    + "a sua quantidade de Títulos Aventyr Despertos (mínimo 2 projéteis, máximo 4 "
                    + "projéteis).",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_A_DISTANCIA)
                    .requiredSkillGraduation(7)
                    .requiredFeat(TIRO_DUPLO)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public boolean permitsActivation(final SkillType skillType, final SkillRoll skillRoll,
                                         final AttackSource attackSource, final CombatantSheet holder) {
            return extraProjectilePermitted(skillType, skillRoll, holder);
        }

        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                          final SkillTrait requestedAbility, final Character character,
                                          final AttackSource attackSource, final CombatantSheet holder,
                                          final SkillRoll skillRoll) {
            return activated(this, skillType, skillRoll) ? Skill.DISADVANTAGE_MALUS : 0;
        }

        @Override
        public int resolveExtraDamageDice(final SkillType attackSkill, final AttackSource attackSource,
                                          final CombatantSheet holder, final SkillRoll skillRoll) {
            return activated(this, attackSkill, skillRoll) ? extraProjectiles(holder.getCharacter()) : 0;
        }

        @Override
        public int resolveAttackEffectRepetitions(final SkillType attackSkill, final AttackSource attackSource,
                                                  final CombatantSheet attacker, final SkillRoll skillRoll) {
            return activated(this, attackSkill, skillRoll) ? extraProjectiles(attacker.getCharacter()) : 0;
        }
    };

    /** TIRO_RAPIDO's "ao custo de 1PA". */
    private static final int TIRO_RAPIDO_COST = 1;

    /** TIRO_MULTIPLO's "(mínimo 2 projéteis, máximo 4 projéteis)". */
    private static final int MIN_EXTRA_PROJECTILES = 2;
    private static final int MAX_EXTRA_PROJECTILES = 4;

    /** Acerto Crítico Aprimorado choices that are "arma … de ataque à distância ou arremesso" (Mira Mortal). */
    private static final Set<AttackMethod> RANGED_METHODS = Set.of(AttackMethod.BOW, AttackMethod.CROSSBOW,
            AttackMethod.THROWABLE, AttackMethod.PROJECTILE);

    /** Whether feat was activated on an Ataque à Distância roll. */
    private static boolean activated(final Feat feat, final SkillType skillType, final SkillRoll skillRoll) {
        return skillType == SkillType.ATAQUE_A_DISTANCIA && skillRoll != null && skillRoll.activated(feat);
    }

    /**
     * Tiro Duplo's and Tiro Múltiplo's shared gate: an Ataque à Distância "em seu Turno" — read as not
     * a Reação — and "uma vez por Rodada" for either of the two.
     */
    private static boolean extraProjectilePermitted(final SkillType skillType, final SkillRoll skillRoll,
                                                    final CombatantSheet holder) {
        boolean reaction = skillRoll != null && skillRoll.getActionCost() != null
                && skillRoll.getActionCost().kind() == ActionCost.Kind.REACTION;
        // One shot is either the double or the multiple one, never both.
        boolean both = skillRoll != null && skillRoll.activated(TIRO_DUPLO) && skillRoll.activated(TIRO_MULTIPLO);
        return skillType == SkillType.ATAQUE_A_DISTANCIA && !reaction && !both && holder != null
                && holder.countFeatActivationsThisRound(TIRO_DUPLO) + holder.countFeatActivationsThisRound(TIRO_MULTIPLO) == 0;
    }

    /** "Uma quantidade de projéteis adicionais igual a sua quantidade de Títulos Aventyr Despertos (mínimo 2, máximo 4)". */
    private static int extraProjectiles(final Character character) {
        return Math.max(MIN_EXTRA_PROJECTILES, Math.min(MAX_EXTRA_PROJECTILES, character.getAllTitles().size()));
    }

    /**
     * The dano Vantagem {@link #ABATER_A_CACA} and {@link #UM_TIRO_UMA_MORTE} both grant — a flat
     * {@code Skill#ADVANTAGE_BONUS}, present exactly when Atirador Perfeito's own condition holds
     * ({@link AtiradorPerfeitoFeat#matchesConditions}), read via {@link
     * AtiradorPerfeitoFeat#chosenBy}.
     */
    private static Optional<DamageBonus> atiradorPerfeitoDanoVantagem(final Character actor,
                                                                      final SceneContext sceneContext,
                                                                      final AttackSource attackSource) {
        AttackMethod chosen = AtiradorPerfeitoFeat.chosenBy(actor).orElse(null);
        return AtiradorPerfeitoFeat.matchesConditions(chosen, actor, sceneContext, attackSource)
                ? Optional.of(new DamageBonus(Skill.ADVANTAGE_BONUS, DamageType.FISICO))
                : Optional.empty();
    }

    private final String description;
    private final FeatRequirements featRequirements;

    ArtilhariaFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.ARTILHARIA;
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
