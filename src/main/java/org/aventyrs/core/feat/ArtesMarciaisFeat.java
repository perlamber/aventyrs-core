package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * Talentos de Arte Marcial — what a character does fighting unarmed, or with Armas Naturais.
 *
 * <p>Each of the five <i>Dominar Arte Marcial</i> constants is a mutually exclusive style
 * ("nenhum outro Talento Dominar Arte Marcial"). {@link FeatRequirements} has no "and none of
 * these" clause, so this tree overrides {@link #isEligible} directly: a character may hold at
 * most one Dominar style, or two once {@link #ARTE_MARCIAL_MISTA} is held. The check is local to
 * this enum because no other tree needs the identical shape — {@code ElficoFeat}'s Guardião cap
 * is race-parameterised, a different question.
 *
 * <p>The old blanket gap — "nothing classifies an attack as Desarmado/Arma Natural" — is now
 * <b>closed on the Dano Base and Defesas paths</b>: {@code DamageBaseService}'s two overloads
 * make {@code weapon == null} unambiguously an Ataque Desarmado, {@code
 * ItemCategory.NATURAL_WEAPON} marks a weapon as an Arma Natural, and {@link
 * Character#treatsAsNaturalWeapon(Weapon)} is the per-character view every Arma-Natural clause
 * consults — which is what lets {@link #DOMINAR_ARTE_MARCIAL_FERROADA_ESMAGADORA} reclassify a
 * light blade and have {@link #ARTISTA_MARCIAL}/{@link #DEFESA_DE_MAOS_LIMPAS} see it.
 *
 * <p>Two things remain unmodelled and block a conditional half here and there: an unarmed attack
 * has no {@code AttackSource} on the Perícia-roll path ({@code AbstractSkillInteraction#applyTo}'s
 * 5th parameter is {@code null} for it, indistinguishable from "caller didn't say"), and there
 * is no "which action was this spent as" marker — the latter is what still caps {@code
 * IMPACTO_ROCHOSO}'s dano Vantagem at +2 (no "+1d6 se Reação") and {@code TIGRE_E_SERPENTE}'s
 * Margem Crítica at +1 (no "+3 se Ação Livre ou Reação").
 */
public enum ArtesMarciaisFeat implements Feat {

    /**
     * "O Dano Base de seus Ataques Desarmados e Armas Naturais aumenta em +1, o Dano Base
     * cumulativamente aumenta em +1 para cada Título Aventyr Desperto que você possui." A
     * Título Aventyr is "Desperto" simply by being held — see {@code
     * InstinctAbility#SENTIR_A_INTENCAO}'s own confirmed reading of this exact phrase, {@link
     * Character#getAllTitles()} non-empty/its size is the count.
     *
     * <p><b>Fully real.</b> The Título-count half is computable arithmetic ({@link
     * Character#getAllTitles()}), and the gate is now expressible too: {@code DamageBaseService}
     * passes the wielded {@link Weapon} ({@code null} on its Ataque Desarmado overload), so
     * {@link #resolveDamageBaseIncrease(Character, Weapon)} applies the scale-up only when the
     * attack is unarmed ({@code weapon == null}) or made with an Arma Natural ({@code
     * weapon.getCategory() == }{@link ItemCategory#NATURAL_WEAPON}) — a wielded blade, bow or
     * club gets nothing.
     */
    ARTISTA_MARCIAL(
            "O Dano Base de seus Ataques Desarmados e Armas Naturais aumenta em +1, o Dano Base "
                    + "cumulativamente aumenta em +1 para cada Título Aventyr Desperto que você possui.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.STRENGTH)
                    .requiredAttributeValue(2)
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(2)
                    .build()) {
        @Override
        public int resolveDamageBaseIncrease(final Character character, final Weapon weapon) {
            boolean unarmedOrNatural = weapon == null || character.treatsAsNaturalWeapon(weapon);
            return unarmedOrNatural ? BASE_DAMAGE_BASE_INCREASE + character.getAllTitles().size() : 0;
        }
    },

    /**
     * "Enquanto não estiver utilizando nenhuma arma, exceto Armas Naturais, você recebe Bônus de
     * +2 em suas Defesas, este Bônus aumenta cumulativamente em +1 para cada Título Aventyr que
     * você tenha Desperto."
     *
     * <p><b>Fully real</b>, and the only Arte Marcial constant that is. Both halves are
     * computable: "não estiver utilizando nenhuma arma" reads {@code Character#getEquipment()}
     * for any {@link Weapon}, and the Título count is {@code Character#getAllTitles()}. It grants
     * to both {@link DefenseType}s, since "suas Defesas" unqualified means DF and DM alike.
     *
     * <p>The "exceto Armas Naturais" exception is honoured: a {@link Weapon} whose {@code
     * getCategory()} is {@link ItemCategory#NATURAL_WEAPON} does not count as "utilizando uma
     * arma", so wielding only Armas Naturais keeps the bonus.
     */
    DEFESA_DE_MAOS_LIMPAS(
            "Enquanto não estiver utilizando nenhuma arma, exceto Armas Naturais, você recebe "
                    + "Bônus de +2 em suas Defesas, este Bônus aumenta cumulativamente em +1 para "
                    + "cada Título Aventyr que você tenha Desperto.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ESQUIVA_E_APARAR)
                    .requiredSkillGraduation(4)
                    .requiredFeat(ARTISTA_MARCIAL)
                    .build()) {
        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character) {
            return wieldsANonNaturalWeapon(character) ? 0 : BASE_DEFENSE_BONUS + character.getAllTitles().size();
        }
    },

    /**
     * "Enquanto não estiver utilizando nenhuma arma - exceto Armas Naturais - e nenhum item do
     * tipo Escudo seus ataques afetam um alvo adicional."
     *
     * <p><b>Fully real</b>, and the Talento that made multi-target attack resolution exist. Three
     * halves, all wired:
     *
     * <ul>
     *   <li>The <b>extra target</b> is {@link #resolveAdditionalTargets}, summed by {@code
     *   AttackTargetingService#getMaximumTargets} and enforced by {@code AttackDelivery#resolve},
     *   which compares the one attack total against each target's own Defesa.</li>
     *   <li>The <b>dano Desvantagem</b> is {@link #resolveDamageBonus(SkillType, SceneContext,
     *   CombatantSheet, Character, AttackSource, int)} — a flat {@code Skill#DISADVANTAGE_MALUS},
     *   conditioned on the attack's target count rather than on any one target, because a
     *   multi-target attack still makes a single dano roll.</li>
     *   <li>The <b>halved damage</b> on the additional target is {@code
     *   DamageInteraction#halvingDamage()}, which {@code AttackDelivery} sets on the chain head it
     *   builds for every target but the primary. It is real Meio-Dano — applied after RD and RA,
     *   the same last stage every other half-damage source lands on.</li>
     * </ul>
     *
     * <p>Both gates read the same two facts, and both hooks check them, so the Talento can never
     * grant the extra target without also charging the Desvantagem: "não estiver utilizando
     * nenhuma arma, exceto Armas Naturais" is {@link #wieldsANonNaturalWeapon} (drawn weapons, not
     * merely carried ones — see that method), and "nenhum item do tipo Escudo" is {@link
     * #wieldsAShield}. The mutual-exclusion Pré-requisito is enforced by this enum's {@code
     * isEligible} override.
     *
     * <p>"Alvos adicionais precisam estar adjacentes ao alvo primário" is deliberately <b>not</b>
     * checked: it is pairwise geometry between two combatants who are both not the roller, which a
     * {@code SceneContext} cannot answer and this core never computes. Choosing the targets is the
     * caller's step; this core enforces only how many there may be.
     */
    DOMINAR_ARTE_MARCIAL_ARTE_FLUIDA(
            "Enquanto não estiver utilizando nenhuma arma - exceto Armas Naturais - e nenhum item "
                    + "do tipo Escudo seus ataques afetam um alvo adicional. Alvos adicionais "
                    + "precisam estar adjacentes ao alvo primário. Enquanto houver mais de um alvo "
                    + "você sofre Desvantagem em rolagens de Danos, os danos no alvo adicional são "
                    + "reduzidos à metade.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(5)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveAdditionalTargets(final SkillType attackingSkillType, final Character character) {
            return arteFluidaApplies(character) ? ARTE_FLUIDA_ADDITIONAL_TARGETS : 0;
        }

        /**
         * "Enquanto houver mais de um alvo" — {@code targetCount} of 0 is the bonuses-only preview
         * path and 1 an ordinary attack, so neither is charged the Desvantagem. Overriding the
         * longest overload with no unconditional value of its own to add, per {@code Feat}'s
         * defaulting convention.
         */
        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType, final SceneContext sceneContext,
                                                         final CombatantSheet attackTarget, final Character actor,
                                                         final AttackSource attackSource, final int targetCount) {
            if (targetCount <= 1 || !arteFluidaApplies(actor)) {
                return Optional.empty();
            }
            return Optional.of(new DamageBonus(Skill.DISADVANTAGE_MALUS, DamageType.FISICO));
        }
    },

    /**
     * "Armas leves de combate corpo-a-corpo que você utilizar são consideradas Armas Naturais
     * para você" — the style that turns a real weapon into an Arma Natural so every other
     * Arma-Natural Talento reaches it.
     *
     * <p><b>Real.</b> It overrides {@link Feat#reclassifiesAsNaturalWeapon}, and because every
     * Arma-Natural clause in the codebase asks {@link Character#treatsAsNaturalWeapon(Weapon)}
     * rather than testing {@link ItemCategory#NATURAL_WEAPON} itself, the reclassification is
     * visible exactly where the rules text promises: {@link #ARTISTA_MARCIAL}'s Dano Base grant,
     * {@link #DEFESA_DE_MAOS_LIMPAS}'s Defesas, {@code MonstruosoFeat#GARRAS_MONSTRUOSAS} and
     * {@code DefensiveImprovement#BENCAO_SELVAGEM} all pick it up with no change of their own.
     *
     * <p>"Armas leves de combate corpo-a-corpo" reads as three columns the weapon already
     * carries — {@link ItemWeightClass#LIGHT} (via {@code getEffectiveWeightClass()}, so a ruined
     * weapon is weighed as it actually is) and {@link SkillType#ATAQUE_CORPO_A_CORPO}. The
     * "enquanto não estiver utilizando nenhum item do tipo Escudo" gate reads {@code
     * Character#getEquipment()} for a {@link ItemCategory#SHIELD}.
     */
    DOMINAR_ARTE_MARCIAL_FERROADA_ESMAGADORA(
            "Enquanto não estiver utilizando nenhum item do tipo Escudo, armas leves de combate "
                    + "corpo-a-corpo que você utilizar são consideradas Armas Naturais para você. "
                    + "Esta Arte Marcial permite que você utilize quaisquer outros Talentos ou "
                    + "Habilidades que afetem Armas Naturais para afetar sua arma, exceto para "
                    + "substituir explicitamente Ataques Desarmados.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(5)
                    .requiredAwakenedTitles(1)
                    .build()) {
        /**
         * Deliberately does <b>not</b> re-ask {@code Character#treatsAsNaturalWeapon} — that is
         * this hook's own caller, and consulting it here would recurse. A weapon already of
         * {@link ItemCategory#NATURAL_WEAPON} needs no reclassifying anyway.
         */
        @Override
        public boolean reclassifiesAsNaturalWeapon(final Weapon weapon, final Character character) {
            return !wieldsAShield(character)
                    && weapon.getEffectiveWeightClass() == ItemWeightClass.LIGHT
                    && weapon.getSkillType() == SkillType.ATAQUE_CORPO_A_CORPO;
        }
    },

    /**
     * "Atacando apenas com Ataques Desarmados, você recebe Vantagem em suas Rolagens de Dano,
     * este benefício muda para +1d6 se o seu ataque for uma Reação."
     *
     * <p><b>The dano Vantagem half is real</b>: "não estiver utilizando nenhuma arma" reads
     * {@code Character#getEquipment()} for any non-natural {@link Weapon} (the same gate {@link
     * #DEFESA_DE_MAOS_LIMPAS} uses), and while that holds every Ataque Corpo-a-Corpo <em>is</em>
     * an Ataque Desarmado — so {@link #resolveDamageBonus} grants a flat {@code
     * Skill#ADVANTAGE_BONUS}, summed by {@code AbstractSkillInteraction} like any other dano
     * source. Mutual exclusion is enforced by this enum's {@code isEligible} override.
     */
    // TODO: "muda para +1d6 se o seu ataque for uma Reação" needs the attack to know it was made
    //  as a Reação; neither AttackDelivery nor SkillRoll carries the action type it was spent as.
    //  So only the flat +2 is granted.
    // TODO: the Escudo half — "+2 Defesas para resistir à Reações enquanto utilizando um Escudo"
    //  — can gate on ItemCategory.SHIELD now, but there is no way to scope a Defesa bonus to
    //  "resistir a uma Reação" specifically.
    DOMINAR_ARTE_MARCIAL_IMPACTO_ROCHOSO(
            "Enquanto não estiver utilizando nenhuma arma, atacando apenas com Ataques "
                    + "Desarmados, você recebe Vantagem em suas Rolagens de Dano, este benefício "
                    + "muda para +1d6 se o seu ataque for uma Reação. Enquanto estiver utilizando "
                    + "um item do tipo Escudo você recebe Bônus de +2 em suas Defesas para "
                    + "resistir à Reações de outros personagens.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ESQUIVA_E_APARAR)
                    .requiredSkillGraduation(5)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType, final SceneContext sceneContext,
                                                         final CombatantSheet attackTarget, final Character actor) {
            if (attackingSkillType != SkillType.ATAQUE_CORPO_A_CORPO || wieldsANonNaturalWeapon(actor)) {
                return Optional.empty();
            }
            return Optional.of(new DamageBonus(Skill.ADVANTAGE_BONUS, DamageType.FISICO));
        }
    },

    /**
     * "Você não é considerado Desprevenido enquanto estiver Caído" — the grappling style.
     */
    // TODO: ConditionType.CAIDO and DESPREVENIDO both exist now, and CAIDO confers DESPREVENIDO
    //  for real — what this clause needs is the opposite: a way for a held trait to *suppress* an
    //  implied condition. Nothing can veto an implication (ConditionType#getImplied is read live,
    //  the same shape Race#getRacialAbilities has and the same gap GorgonaFeat#MARCA_DA_MALDICAO
    //  cites for losing Imunidade a Encantamentos).
    // TODO: Agarrar/Empurrar/Derrubar are manoeuvres with no representation, so a Vantagem
    //  scoped to them cannot be expressed (this core does not track what a roll is *for*).
    // TODO: "+2 em sua DF", "pode realizar uma Reação adicional" and "pode se levantar como Ação
    //  Livre" are all conditional on being Caído — a held Talento cannot see its holder's
    //  Condições (Feat resolve hooks take a Character, a Condition lives on the CombatantSheet).
    DOMINAR_ARTE_MARCIAL_SUBMISSAO(
            "Você não é considerado Desprevenido enquanto estiver Caído e não sofre Desvantagens "
                    + "em rolagens de Perícias de Ataque com Armas Naturais ou Desarmado nesta "
                    + "condição, você também recebe Vantagem em suas rolagens de Ataque "
                    + "Corpo-a-Corpo para Agarrar, Empurrar ou Derrubar. Enquanto estiver caído "
                    + "você recebe Bônus de +2 em sua DF, pode realizar uma Reação adicional e "
                    + "pode se levantar como Ação Livre.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(5)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "A Margem Crítica Menor de seus Ataques Corpo-a-Corpo aumenta em +1 e seus Acertos Críticos
     * ganham a Corrente de Efeitos – Rugido."
     *
     * <p>The flat "+1 Margem Crítica de Ataques Corpo-a-Corpo" half is <b>real</b> —
     * {@link #resolveCriticalMarginIncrease(SkillType, org.aventyrs.core.scene.SceneContext,
     * Character)} is on {@code Feat} now and {@code AbstractSkillInteraction#sumCriticalMarginIncrease}
     * scans {@code Character#getFeats()} for it. Like {@code AnaoFeat#GLORIA_YMIRIANA} it uses the
     * hook for a Menor-tier clause, which is all it widens: {@code SkillRoll#getCriticalResult(int)}
     * applies its margin to the Menor tier only, Acerto Crítico Maior staying a literal triple-6 —
     * so this matches the clause exactly.
     */
    // TODO: the "muda para +3 se o ataque for uma Ação Livre ou Reação" upgrade needs the attack
    //  to know which action it was spent as — see IMPACTO_ROCHOSO's own note. So only the flat +1
    //  is granted.
    // TODO: granting a Corrente de Efeitos – Rugido to a critical needs a hook on the attack path;
    //  EffectChain lists on DeliveredAttack are supplied by the caller, not assembled from the
    //  attacker's Talentos.
    DOMINAR_ARTE_MARCIAL_TIGRE_E_SERPENTE(
            "A Margem Crítica Menor de seus Ataques Corpo-a-Corpo aumenta em +1 e seus Acertos "
                    + "Críticos ganham a Corrente de Efeitos – Rugido. O aumento na Margem Crítica "
                    + "Menor muda para +3 (ao invés de +1) se o seu ataque for uma Ação Livre ou "
                    + "Reação.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(5)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                                  final Character character) {
            return skillType == SkillType.ATAQUE_CORPO_A_CORPO ? TIGRE_E_SERPENTE_MARGIN_INCREASE : 0;
        }
    },

    /**
     * "Você recebe Bônus de +1 em rolagens de Danos Críticos de Armas Naturais e Defesas para
     * cada Talento de Arte Marcial que possuir. Você pode possuir um segundo Talento de Dominar
     * Arte Marcial."
     *
     * <p><b>The Defesas half is real</b> — {@code +1} to both DF and DM per {@code
     * FeatCategory#ARTE_MARCIAL} Talento held (this one included), through {@link
     * #resolveDefenseBonus}. The clause grants that figure to Danos Críticos <em>and</em> Defesas
     * as two independent bonuses, so wiring the Defesas half alone is exact, not a partial total.
     * The "segundo Talento de Dominar Arte Marcial" cap-raise is applied by this enum's {@code
     * isEligible} override.
     */
    // TODO: the "Danos Críticos de Armas Naturais" half has no hook — DamageBonus applies to any
    //  dano roll, with no notion of the critical half.
    ARTE_MARCIAL_MISTA(
            "Você recebe Bônus de +1 em rolagens de Danos Críticos de Armas Naturais e Defesas "
                    + "para cada Talento de Arte Marcial que possuir. Você pode possuir um segundo "
                    + "Talento de Dominar Arte Marcial.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                    .requiredSkillGraduation(7)
                    .requiredAwakenedTitles(2)
                    .requiredFeatCategory(FeatCategory.ARTE_MARCIAL)
                    .requiredFeatCategoryCount(1)
                    .build()) {
        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character) {
            long arteMarcialFeats = character.getFeats().stream()
                    .map(Feat::catalogEntry)
                    .filter(feat -> feat.getFeatCategory() == FeatCategory.ARTE_MARCIAL)
                    .count();
            return (int) (ARTE_MARCIAL_MISTA_DEFENSE_BONUS_PER_FEAT * arteMarcialFeats);
        }
    };

    /** ARTISTA_MARCIAL's own flat "+1" before any Título Aventyr Desperto is counted. */
    private static final int BASE_DAMAGE_BASE_INCREASE = 1;

    /** DEFESA_DE_MAOS_LIMPAS's own flat "+2" before any Título Aventyr Desperto is counted. */
    private static final int BASE_DEFENSE_BONUS = 2;

    /** TIGRE_E_SERPENTE's flat "+1 número" to the Margem Crítica of an Ataque Corpo-a-Corpo. */
    private static final int TIGRE_E_SERPENTE_MARGIN_INCREASE = 1;

    /** ARTE_MARCIAL_MISTA's own flat "+1" to Defesas per Talento de Arte Marcial held. */
    private static final int ARTE_MARCIAL_MISTA_DEFENSE_BONUS_PER_FEAT = 1;

    /** ARTE_FLUIDA's "um alvo adicional" — one target beyond the primary. */
    private static final int ARTE_FLUIDA_ADDITIONAL_TARGETS = 1;

    /**
     * ARTE_FLUIDA's own gate, shared by both of its halves so the extra target and the dano
     * Desvantagem can never disagree about whether the Talento is currently active.
     */
    private static boolean arteFluidaApplies(final Character character) {
        return !wieldsANonNaturalWeapon(character) && !wieldsAShield(character);
    }

    /**
     * Whether character has an item of {@link ItemCategory#SHIELD} equipped — the "nenhum item do
     * tipo Escudo" gate {@link #DOMINAR_ARTE_MARCIAL_ARTE_FLUIDA} and {@link
     * #DOMINAR_ARTE_MARCIAL_FERROADA_ESMAGADORA} share.
     *
     * <p>Reads {@code Character#getEquipment()}, not {@code getDrawnWeapons()}, unlike {@link
     * #wieldsANonNaturalWeapon}: a Escudo is not a {@link Weapon}, so it has no drawn/sheathed
     * state to consult — being equipped <em>is</em> carrying it on the arm.
     */
    private static boolean wieldsAShield(final Character character) {
        return character.getEquipment().stream()
                .anyMatch(item -> item.getCategory() == ItemCategory.SHIELD);
    }

    /**
     * Whether character is wielding any {@link Weapon} that is <b>not</b> an Arma Natural for
     * them — the "não estiver utilizando nenhuma arma, exceto Armas Naturais" gate {@link
     * #DEFESA_DE_MAOS_LIMPAS} and {@link #DOMINAR_ARTE_MARCIAL_IMPACTO_ROCHOSO} share.
     *
     * <p>Reads {@code Character#getDrawnWeapons()}, not {@code getEquipment()}: "utilizando" is
     * <b>in hand</b>, so a blade sheathed on the belt costs a martial artist nothing. A weapon
     * must be drawn to be used, and drawing one is itself an action.
     */
    private static boolean wieldsANonNaturalWeapon(final Character character) {
        return character.getDrawnWeapons().stream()
                .anyMatch(weapon -> !character.treatsAsNaturalWeapon(weapon));
    }

    private final String description;
    private final FeatRequirements featRequirements;

    ArtesMarciaisFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.ARTE_MARCIAL;
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
     * Adds the mutual-exclusion cap on top of the base {@link Feat#isEligible} check: a character
     * may hold at most one <i>Dominar Arte Marcial</i> style, raised to two while {@link
     * #ARTE_MARCIAL_MISTA} is held. Every other {@code FeatRequirements} clause is checked the
     * normal way — see this enum's own javadoc for why the cap lives here rather than on {@code
     * FeatRequirements}.
     */
    @Override
    public boolean isEligible(final Character character) {
        if (!Feat.super.isEligible(character)) {
            return false;
        }
        if (!isDominarStyle()) {
            return true;
        }
        long heldStyles = character.getFeats().stream()
                .map(Feat::catalogEntry)
                .filter(ArtesMarciaisFeat.class::isInstance)
                .map(ArtesMarciaisFeat.class::cast)
                .filter(ArtesMarciaisFeat::isDominarStyle)
                .count();
        int allowed = character.getFeats().stream()
                .map(Feat::catalogEntry)
                .anyMatch(ARTE_MARCIAL_MISTA::equals) ? 2 : 1;
        return heldStyles < allowed;
    }

    /** Whether this constant is one of the five mutually exclusive <i>Dominar Arte Marcial</i> styles. */
    private boolean isDominarStyle() {
        return switch (this) {
            case DOMINAR_ARTE_MARCIAL_ARTE_FLUIDA, DOMINAR_ARTE_MARCIAL_FERROADA_ESMAGADORA,
                 DOMINAR_ARTE_MARCIAL_IMPACTO_ROCHOSO, DOMINAR_ARTE_MARCIAL_SUBMISSAO,
                 DOMINAR_ARTE_MARCIAL_TIGRE_E_SERPENTE -> true;
            default -> false;
        };
    }
}
