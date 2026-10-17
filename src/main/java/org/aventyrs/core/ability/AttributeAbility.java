package org.aventyrs.core.ability;

import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillTraitKind;
import org.aventyrs.core.skill.SkillType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

public interface AttributeAbility {
    AttributeDomain getAttributeDomain();
    String getDescription();

    /**
     * Which Ego domains this ability grants a non-cumulative temporary point in when the
     * holder's own Perícia roll resolves to criticalResult — e.g. {@code
     * CharismaAbility#DESTINO_FAVORAVEL} granting Sorte and Autocontrole on {@link
     * CriticalResult#ACERTO_CRITICO_MAIOR}. Empty by default; only override on a constant
     * whose rules text reacts to the roller's own critical result this way. "Non-cumulative"
     * per that kind of ability's own rules text: applied via {@code CombatantSheet
     * #receiveNonCumulativeTemporaryEgoPoints} with this ability as its source — a received point
     * (refill first, else an extra), and a repeat trigger adds nothing while this ability still
     * holds an unspent extra; an unrelated source's own points still add on top.
     */
    default List<EgoDomain> resolveCriticalSuccessEgoGain(CriticalResult criticalResult) {
        return List.of();
    }

    /**
     * Which single Ego domain this ability permanently grants +1 to (in {@code
     * EgoValue#variable}, never {@code #base} — see {@code EgoValue}'s own javadoc) the
     * moment it's acquired — e.g. {@code CharismaAbility#DESTINO_FAVORAVEL}'s "Você adquire
     * um ponto de Sorte permanentemente." Empty by default; only override on a constant
     * whose rules text grants a permanent Ego point this way. Applied by {@code
     * org.aventyrs.core.character.services.AttributeAbilityService#grantAttributeAbility},
     * not by this method itself — same "resolve, don't mutate" shape as {@link
     * #resolveCriticalSuccessEgoGain}.
     */
    default Optional<EgoDomain> resolvePermanentEgoGain() {
        return Optional.empty();
    }

    /**
     * How many temporary Ego points this ability owes its holder <em>on the following
     * Rodada</em> the first time in a game session that domain's spendable points are reduced
     * to zero — {@link GnoseAbility#ESTABILIDADE_EMOCIONAL}'s "a primeira vez em cada sessão de
     * jogo que seu Autocontrole for reduzido a zero você receberá 1 ponto temporário neste Ego
     * na Rodada seguinte". 0 by default, and 0 for any domain other than the one the ability's
     * own rules text names.
     *
     * <p>Resolved by {@code AbstractCombatantSheet#spendEgoPoints} — the single funnel both a
     * deliberate use and an enemy's drain pass through, since "for reduzido a zero" does not
     * care <em>how</em> the pool emptied (deliberately unlike {@link
     * org.aventyrs.core.ego.EgoAdvantage#resolveEgoSpendRecovery}, which only a holder's own
     * chosen expenditure triggers). The once-per-session guard is {@code
     * CombatantSheet#consumeOncePerSession}, keyed on the ability constant itself, and the
     * delivery is {@code CombatantSheet#scheduleTemporaryEgoPointGrant} — see both for what a
     * "session" and a "next Rodada" mean here.
     *
     * <p>Same "resolve, don't mutate" shape as {@link #resolvePermanentEgoGain}: this only
     * says how much is owed.
     */
    default int resolveEgoDepletionGrant(EgoDomain domain) {
        return 0;
    }

    /**
     * The {@link ActiveAbility} this ability grants the character the moment it's acquired —
     * e.g. {@link FocusAbility#CONCENTRACAO_PROFUNDA}'s own activatable state. Empty by
     * default; only override on a constant whose rules text describes something the holder
     * must actively spend Pontos de Ação/Magia to trigger, rather than an always-on passive
     * effect. Applied by {@code
     * org.aventyrs.core.character.services.AttributeAbilityService#grantAttributeAbility},
     * not by this method itself — same "resolve, don't mutate" shape as {@link
     * #resolvePermanentEgoGain}.
     */
    default Optional<ActiveAbility> resolveActiveAbility() {
        return Optional.empty();
    }

    /**
     * Every {@link SkillType} this ability grants outright training in the moment it's
     * acquired — e.g. {@link GnoseAbility#RATO_DE_BIBLIOTECA}'s own "treinamento em todas as
     * Perícias que não for treinado." Applied by {@code
     * org.aventyrs.core.character.services.AttributeAbilityService#grantAttributeAbility},
     * which adds a fresh, untrained-graduation {@code CharacterSkill} for each entry (no
     * {@code specializations} — {@code CharacterSkill#getSpecializations()} already defaults
     * to an empty list, so RATO_DE_BIBLIOTECA's own "não recebem Especializações iniciais"
     * needs nothing further) — same "resolve, don't mutate" shape as {@link
     * #resolvePermanentEgoGain}/{@link #resolveActiveAbility}. Takes the whole {@link
     * Character} (not just its trained {@link SkillType}s, unlike {@link
     * #resolvePendingSkillTraitChoices(Collection)}) because this needs the *untrained*
     * complement of that set, not the trained set itself. Empty by default; only override on
     * a constant whose rules text grants outright Perícia training like this.
     */
    default List<SkillType> resolveGrantedSkillTraining(Character character) {
        return List.of();
    }

    /**
     * {@link #resolvePendingSkillTraitChoices(Collection)} against character's own currently
     * trained Perícias — the overload to call when a full {@link Character} is in hand. Never
     * override this one: the rule lives on the {@link Collection} overload, which is all any
     * of these abilities actually needs (see its javadoc for why that matters).
     */
    default List<SkillType> resolvePendingSkillTraitChoices(Character character) {
        return resolvePendingSkillTraitChoices(character.getSkills().keySet());
    }

    /**
     * Every {@link SkillType} this ability still owes the holder a trait choice for, given
     * trainedSkills — e.g. {@link CharismaAbility#CHARME}'s own "pick a {@code
     * SkillCompetencyAbility} <b>and</b> a {@code SkillSpecialization} for each trained Carisma
     * Perícia," or {@link GnoseAbility#DOMINIO_DO_CONHECIMENTO}'s own specialization-<b>only</b>
     * "pick a {@code SkillSpecialization} for each known Perícia." These are <i>candidates</i>,
     * not a quota: how many of them the player may actually resolve is {@link
     * #resolvePendingSkillTraitChoiceLimit()}, and which trait(s) each one owes is {@link
     * #resolvePendingSkillTraitKinds()} — neither is carried by this list itself. Empty by
     * default; only override on a constant whose rules text requires the player to pick one or
     * both of these per Perícia at acquisition time.
     *
     * <p>Takes the trained {@link SkillType}s rather than a {@link Character} — unlike {@link
     * #resolvePermanentEgoGain}/{@link #resolveActiveAbility}, which Perícias are owed a choice
     * depends on which ones are currently trained, but on <i>nothing else</i> about the
     * character. Keeping it to that lets a caller that has no {@code Character} yet — a
     * character-creation wizard, where the abilities and the Perícias are both still being
     * picked — ask the same question the acquisition path asks, instead of duplicating the rule
     * or fabricating a throwaway {@code Character} to hold it.
     *
     * <p>Reported by {@code
     * org.aventyrs.core.character.services.AttributeAbilityService#grantAttributeAbility} via
     * {@code AttributeAbilityGrantResult#getPendingSkillTraitChoices()} — a caller (an API/UI
     * layer) is expected to resolve each one, once the player picks, via {@code
     * AttributeAbilityService#grantCompetencyAbilityChoice} and/or {@code
     * AttributeAbilityService#grantSpecializationChoice}; this method itself never mutates
     * anything.
     */
    default List<SkillType> resolvePendingSkillTraitChoices(Collection<SkillType> trainedSkills) {
        return List.of();
    }

    /**
     * How many of {@link #resolvePendingSkillTraitChoices}' candidates the player may actually
     * resolve — the "até 3" in {@link GnoseAbility#DOMINIO_DO_CONHECIMENTO}'s own rules text,
     * against a candidate list of every Perícia the character knows. Empty means "one per
     * candidate, no cap of its own," which is what an ability like {@link
     * CharismaAbility#CHARME} owes ("de cada Perícia baseada em Carisma em que for treinado" —
     * every candidate it reports, by construction); only override on a constant whose rules
     * text names a number smaller than the candidate list it offers.
     *
     * <p>Deliberately <b>not</b> a function of the candidate list: a caller wanting the number
     * of choices actually owed takes {@code min} of this and that list's own size, which is
     * exactly what {@code AttributeAbilityGrantResult#getPendingSkillTraitChoiceLimit()}
     * already reports. Nothing enforces the cap at resolution time — {@code
     * AttributeAbilityService#grantSpecializationChoice} records whatever it's handed, same
     * unenforced-prerequisite restraint already applied to "Requer N Graduações"-style clauses
     * elsewhere in this core.
     */
    default OptionalInt resolvePendingSkillTraitChoiceLimit() {
        return OptionalInt.empty();
    }

    /**
     * Which {@link SkillTraitKind}s each of {@link #resolvePendingSkillTraitChoices}' entries
     * owes — {@link SkillTraitKind#SPECIALIZATION} alone for {@link
     * GnoseAbility#DOMINIO_DO_CONHECIMENTO}, both for {@link CharismaAbility#CHARME}, which
     * owes an Especialização <i>and</i> an Habilidade de Competência per Perícia. This is what
     * tells a caller which of {@code AttributeAbilityService#grantCompetencyAbilityChoice}/
     * {@code #grantSpecializationChoice} to call for an entry, without a per-ability switch of
     * its own. Empty by default, and expected to stay in lockstep with {@link
     * #resolvePendingSkillTraitChoices}: a constant that overrides one overrides the other.
     */
    default Set<SkillTraitKind> resolvePendingSkillTraitKinds() {
        return Set.of();
    }

    /**
     * Extra PM a Rest of restType recovers on top of {@code RestService}'s own Foco-times-
     * multiplier calculation — e.g. {@link FocusAbility#CANALIZADOR_DE_MANA}'s "Descansos
     * Verdadeiros Longos ou superiores permitem que você recupere +2PM adicionais." Zero by
     * default; only override on a constant whose rules text grants a Rest-tier-conditioned PM
     * bonus like this. Takes restType explicitly (rather than being a reflection-invoked
     * {@code @Modifier} method) because the bonus is conditioned on *which* Rest tier is being
     * applied, which a no-arg method has no way to see — same reasoning as {@code
     * SkillCompetencyAbility#resolveDamageBonus}/{@code #resolveAttackRollBonus} needing their
     * own explicit parameters instead of {@code @Modifier}. Summed by {@code
     * org.aventyrs.core.rest.RestService#getRecoveredMagicPoints} across {@code
     * Character#getAttributeAbilities()}. Paid only by a real Descanso ({@code
     * RecoveryOrigin#DESCANSO}); see {@link #resolveRestEquivalentMagicPointsBonus}.
     */
    default int resolveRestMagicPointsBonus(RestType restType) {
        return 0;
    }

    /**
     * Extra PV a Rest of restType recovers on top of {@code RestService}'s own Vigor-times-
     * multiplier calculation — e.g. {@link VigorAbility#METABOLISMO_RAPIDO}'s "Descansos
     * Verdadeiros Longos ou superiores permitem que você recupere +3PV adicionais." Mirrors
     * {@link #resolveRestMagicPointsBonus}'s identical shape/reasoning exactly, on the PV side
     * instead of PM. Zero by default; only override on a constant whose rules text grants a
     * Rest-tier-conditioned PV bonus like this. Summed by {@code
     * org.aventyrs.core.rest.RestService#getRecoveredHitPoints} across {@code
     * Character#getAttributeAbilities()}. Paid only by a real Descanso ({@code
     * RecoveryOrigin#DESCANSO}); see {@link #resolveRestEquivalentHitPointsBonus}.
     */
    default int resolveRestHitPointsBonus(RestType restType) {
        return 0;
    }

    /**
     * Extra PD a Rest of restType recovers on top of {@code RestService}'s own Instinto-times-
     * multiplier calculation — e.g. {@link InstinctAbility#SUPERMOTIVADO}'s "recupera 1PD
     * adicional... Descansos longos ou superiores adicionalmente... 2PD adicionais." Mirrors
     * {@link #resolveRestMagicPointsBonus}/{@link #resolveRestHitPointsBonus}'s identical
     * shape/reasoning, on the PD side. Zero by default; only override on a constant whose
     * rules text grants a Rest-tier-conditioned PD bonus like this. Summed by {@code
     * org.aventyrs.core.rest.RestService#getRecoveredDeterminationPoints} across {@code
     * Character#getAttributeAbilities()}. Paid only by a real Descanso ({@code
     * RecoveryOrigin#DESCANSO}); see {@link #resolveRestEquivalentDeterminationPointsBonus}.
     */
    default int resolveRestDeterminationPointsBonus(RestType restType) {
        return 0;
    }

    /**
     * Extra PV an effect recovering "como se passasse por um Descanso" of restType gives — {@code
     * RestService#getRecoveredHitPoints} with {@code RecoveryOrigin#EQUIVALENTE}. Unlike {@link
     * #resolveRestHitPointsBonus}, which pays only on a real Descanso (table ruling, 2026-10-16:
     * Metabolismo Rápido's +3PV is a Descanso's, and a Magia's heal gets its +2PV through {@link
     * #resolveHealingReceivedBonus} instead). Zero by default; override only for a clause written
     * for <em>any</em> recovery effect.
     */
    default int resolveRestEquivalentHitPointsBonus(RestType restType) {
        return 0;
    }

    /** {@link #resolveRestEquivalentHitPointsBonus}'s PM twin. Zero by default. */
    default int resolveRestEquivalentMagicPointsBonus(RestType restType) {
        return 0;
    }

    /**
     * {@link #resolveRestEquivalentHitPointsBonus}'s PD twin — {@link InstinctAbility#SUPERMOTIVADO}'s
     * "Efeitos que te permitam recuperar PD o fazem recuperar 1PD adicional", which names every
     * recovery effect, not only a Descanso. Zero by default.
     */
    default int resolveRestEquivalentDeterminationPointsBonus(RestType restType) {
        return 0;
    }

    /**
     * A flat bonus on the holder's dano roll against attackTarget — {@link StrengthAbility#SUBJUGAR}'s
     * "Vantagem em rolagens de Dano para atacar personagens caídos ou desprevenidos". Summed by {@code
     * AbstractSkillInteraction}'s dano-bonus sum as {@code DamageContributionSource#ATTRIBUTE_ABILITY};
     * attackTarget is {@code null} on the main pass, so a target-conditioned constant returns 0 until
     * the attack-target pass. Zero by default.
     */
    default int resolveDamageRollBonus(final SkillType skillType, final CombatantSheet holder,
                                       final CombatantSheet attackTarget, final SceneContext sceneContext) {
        return 0;
    }

    /**
     * Raw damage the holder's landed attack made with skillType deals to the primary target's worn Escudo, else
     * Armadura — {@link StrengthAbility#ESTILHACADOR}'s 2 on an Ataque Corpo-a-Corpo. Reported by {@code
     * AttackDelivery} on {@code DeliveredAttackResult#getDefensiveEquipmentDamage()} and resolved against the real
     * sheet by {@code combat.ShieldBreak}. Zero by default.
     */
    default int resolveDefensiveEquipmentDamageOnHit(final SkillType skillType) {
        return 0;
    }

    /**
     * The most Rodadas a harmful effect with a counted Duração may last on the holder — {@link
     * VigorAbility#RECUPERACAO_ASSOMBROSA}'s 3. Applied as the effect lands by {@code AbstractCombatantSheet} (both
     * {@code applyEffect} and {@code applyCondition}), judged by {@code TemporaryEffect#isHarmful()}; an open-ended
     * effect, or one scoped to the combat or a Descanso, counts as permanent and is never cut. Empty by default.
     */
    default java.util.OptionalInt resolveHarmfulDurationCap() {
        return java.util.OptionalInt.empty();
    }

    /**
     * How many Categorias de Peso lighter the holder's equipment counts for their benefit — {@link
     * StrengthAbility#BESTA_DE_CARGA}'s 1. Summed by {@code Character#carriedWeightClassOf}. Zero by default.
     */
    default int resolveCarriedWeightClassShift() {
        return 0;
    }

    /**
     * PA the holder may <em>buy</em> off a Perícia roll governed by governingDomain — {@link
     * CharismaAbility#VOZ_DE_OURO}'s "-1PA" on a Carisma-based action, paid with {@link
     * #resolveBoughtSkillRollDeterminationCost}. Offered by {@code ActionPointsService#getBoughtReductionCost}, taken
     * by its {@code getSkillRollCost(…, buyReduction)}. Zero by default.
     */
    default int resolveBoughtSkillRollActionPointReduction(final AttributeDomain governingDomain) {
        return 0;
    }

    /** The PD {@link #resolveBoughtSkillRollActionPointReduction} costs — Voz de Ouro's 2. Zero by default. */
    default int resolveBoughtSkillRollDeterminationCost() {
        return 0;
    }

    /**
     * The PD a Rodada 0.0 action costs the holder — {@link InstinctAbility#PARANOIA_SAUDAVEL}'s "Ao custo de 2PD e
     * apenas uma vez a cada Cena" — or empty when this Habilidade offers none. Read by {@code
     * character.services.PreInitiativeService}. Empty by default.
     */
    default OptionalInt resolvePreInitiativeActionCost() {
        return OptionalInt.empty();
    }

    /**
     * Extra PV a heal its holder <em>receives</em> recovers — {@link VigorAbility#METABOLISMO_RAPIDO}'s
     * "+2PV adicionais" on a heal from a Magia or a Habilidade de Título ({@code
     * HealingSource#isMagiaOrTituloAbility}). Added by {@code AbstractCombatantSheet#heal} beside the
     * Talentos' recipient adjustment, so it is halved and Coma-capped like the rest of the heal, and
     * never asked for Roubo de Vida. source may be {@code null} (an unsourced heal). Zero by default.
     */
    default int resolveHealingReceivedBonus(final org.aventyrs.core.sheet.HealingSource source) {
        return 0;
    }

    /**
     * PM a heal its holder receives also restores — {@link FocusAbility#CANALIZADOR_DE_MANA}'s "+1PM
     * adicional" on a Magia's or Título's PV heal. Paid by {@code AbstractCombatantSheet#heal} only
     * when PV actually came back, through {@code recoverMagicPoints} (so Feridas Dolorosas refuses it).
     * Zero by default.
     */
    default int resolveHealingReceivedManaBonus(final org.aventyrs.core.sheet.HealingSource source) {
        return 0;
    }

    /**
     * The flat bonus this Habilidade adds to an already-present Roubo de Mana — {@link
     * FocusAbility#CANALIZADOR_DE_MANA}'s "seu Roubo de Mana total aumenta em +1 (não cumulativo)".
     * Read by {@code AttackDelivery} only when the attack already steals Mana, the same "amplify,
     * never grant" rule as {@link #resolveLifeStealBonus}. Zero by default.
     */
    default int resolveManaStealBonus() {
        return 0;
    }

    /**
     * The flat bonus this Habilidade adds to a character's total Roubo de Vida, summed by
     * {@code org.aventyrs.core.character.services.LifeStealService#getTotalLifeSteal} —
     * exactly once, never per active {@link org.aventyrs.core.sheet.LifeSteal} effect, and
     * only once that total is already positive (a character with no active {@code LifeSteal}
     * effect of their own gets none of this either — it only ever amplifies an already-active
     * one, never grants one from nothing) — e.g. {@link VigorAbility#METABOLISMO_RAPIDO}'s
     * "Personagens sob efeitos de Roubo de Vida têm seu efeito de Roubo de Vida aumentado em
     * +1 (não cumulativo)." Zero by default; only override on a constant whose rules text
     * grants a flat, non-stacking Roubo de Vida bonus like this.
     */
    default int resolveLifeStealBonus() {
        return 0;
    }

    /**
     * Every {@link Blessing} this Habilidade grants the moment its holder wins
     * initiative for their group — mirrors {@code org.aventyrs.core.ego.EgoAdvantage
     * #resolveInitiativeBlessings}'s own shape (see that method's javadoc for the full
     * mechanism, and {@code org.aventyrs.core.character.services.InitiativeBlessingService}
     * for how this is scanned alongside {@code EgoAdvantage}/{@code
     * org.aventyrs.core.skill.SkillCompetencyAbility}). Empty by default; only override on a
     * constant whose rules text grants a bonus specifically for winning initiative.
     */
    default List<Blessing> resolveInitiativeBlessings() {
        return List.of();
    }

    /**
     * How many extra "números" this Habilidade widens skillType's Margem Crítica Menor by
     * right now, conditioned on {@link SceneContext} — e.g. {@link
     * org.aventyrs.core.ability.DexterityAbility#LETALIDADE_PROGRESSIVA}'s Round-scaling bonus
     * to Ataque à Distância during a Cena de Combate. Mirrors {@code
     * org.aventyrs.core.ego.EgoAdvantage#resolveCriticalMarginIncrease}/{@code
     * org.aventyrs.core.skill.SkillCompetencyAbility#resolveCriticalMarginIncrease}'s identical
     * shape — see {@link org.aventyrs.core.skill.SkillRoll#getCriticalResult(int)} for how the
     * sum across all three is actually consumed. Zero by default; only override on a constant
     * whose rules text widens Margem Crítica Menor like this.
     */
    default int resolveCriticalMarginIncrease(SkillType skillType, SceneContext sceneContext) {
        return 0;
    }

    /**
     * Extra Pontos de Ação this Habilidade grants on the given Turn — e.g. {@link
     * DexterityAbility#APRESSADO}'s "+1PA em turnos de Rodadas pares". turnNumber is 0-based,
     * the same convention {@code org.aventyrs.core.action.ActionProfile} and every {@code
     * ActionPointsService} overload already use: Turn 0 <em>is</em> the first Rodada, so an
     * even Rodada is an odd turnNumber. Takes turnNumber explicitly (rather than being a
     * reflection-invoked {@code @Modifier(ModifierType.ACTION_POINTS)} method) because the
     * grant is conditioned on <i>which</i> Rodada this is, which a no-arg method has no way to
     * see — same reasoning as {@link #resolveRestMagicPointsBonus} needing its own explicit
     * {@code RestType}. Summed by {@code
     * org.aventyrs.core.action.ActionPointsService#getMaxActionPoints} across {@code
     * Character#getAttributeAbilities()}, into the baseline the {@code ActionProfile}
     * adjustment is then applied on top of — so a profile that zeroes PA outright still zeroes
     * this too. Zero by default; only override on a constant whose rules text grants
     * Turn-conditioned PA like this.
     */
    default int resolveActionPointsBonus(int turnNumber) {
        return 0;
    }

    /**
     * A bonus toward this Perícia's own roll ({@code skillRollBonus}) granted only on the
     * first roll governed by rolledDomain each of the holder's own Turns — e.g. {@link
     * org.aventyrs.core.ability.DexterityAbility#PRECISAO}'s Vantagem on the first
     * Destreza-based Perícia roll each Turn. Only ever called by {@code
     * AbstractSkillInteraction} once {@link org.aventyrs.core.sheet.CombatantSheet
     * #isFirstRollOfTurnFor} confirms this roll is that first one (it reads the per-Turn slice of
     * the sheet's action log) — so, unlike this interface's other {@code resolve*} hooks, an
     * override doesn't need to check that condition itself, only gate on rolledDomain. Empty by
     * default; only override on a constant whose rules text grants a bonus scoped to the first
     * roll of a Turn like this.
     */
    default Optional<Integer> resolveFirstRollOfTurnBonus(AttributeDomain rolledDomain) {
        return Optional.empty();
    }

    /**
     * Whether this ability makes the holder's <b>first Magia of each Rodada</b> add its full Foco
     * where the effect's rules text says "Metade do Foco" — {@code FocusAbility#MAGIA_PODEROSA}.
     * Read by {@code org.aventyrs.core.magic.SpellCastingService#resolvePrimaryDamage} when it
     * resolves a Magia's {@link org.aventyrs.core.magic.SpellDamage}, gated there on the caster
     * having recorded no Magia in {@code CombatantSheet#getActionsThisRound()} yet this Rodada.
     * False by default; only {@code MAGIA_PODEROSA} overrides it.
     */
    default boolean upgradesFirstSpellOfRoundFocusScaling() {
        return false;
    }

    /**
     * Whether this ability makes the holder's <b>first attack of each Rodada</b> add their full
     * Força to its dano roll where the base rule adds half — {@code
     * StrengthAbility#DESTRUIDOR_DE_MUROS}. Read by {@code AbstractSkillInteraction}'s dano-bonus
     * sum, gated there on the attacker having recorded no Perícia de Ataque roll in {@code
     * CombatantSheet#getActionsThisRound()} yet this Rodada.
     *
     * <p>The exact mirror of {@link #upgradesFirstSpellOfRoundFocusScaling}, deliberately: both
     * ask "does this holder's first X of the Rodada use a whole Atributo instead of half of it",
     * and keeping the two shapes identical is what makes the pair legible. They stay separate
     * hooks rather than one parameterized by {@link AttributeDomain} because the two clauses gate
     * on different actions (a Magia versus an attack) and nothing needs to ask both at once.
     *
     * <p><b>"O primeiro ataque" means any attack</b>, not the first <i>melee</i> one — a holder
     * who opens the Rodada with a bow shot has spent it, and the swing that follows adds only
     * half. False by default; only {@code DESTRUIDOR_DE_MUROS} overrides it.
     */
    default boolean upgradesFirstMeleeAttackOfRoundStrengthScaling() {
        return false;
    }

    /**
     * Extra Movimento Base, in UD, this trait grants on one specific movement of the Rodada —
     * for a clause scoped to <i>which</i> movement it is, e.g. {@code
     * org.aventyrs.core.ability.DexterityAbility#PASSOS_LONGOS}'s "seu primeiro movimento em
     * cada Rodada tem a distância aumentada em +2UD". movementIndex is 0-based, so the first
     * movement of the Rodada is 0; it comes from {@code CombatantSheet
     * #getMovementsTakenThisRound()} / {@code CombatantSheet#consumeMovementThisRound()}.
     *
     * <p><b>Per Ponto de Ação, like every other movement figure in this core</b> — see {@code
     * MovementService#getMovementBase}. A clause naming a UD amount is always widening what one
     * Ponto de Ação buys, never handing out a one-off distance, so this is summed into the same
     * per-Ponto-de-Ação total the permanent {@code ModifierType#MOVEMENT} scan produces rather
     * than added once afterwards.
     *
     * <p>Zero by default; only override on a constant whose rules text scopes a movement bonus
     * to a particular movement of the Rodada. An unconditional "+NUD ao Movimento Base" is a
     * plain {@code @Modifier(ModifierType.MOVEMENT)} method instead, and a Round-<i>window</i>
     * clause ("nas duas primeiras Rodadas") is a {@code TemporaryBonus} — both are different
     * axes from this one.
     */
    default int resolveRoundMovementIncrease(int movementIndex) {
        return 0;
    }

    /**
     * Whether the movement at movementIndex (0-based, the Rodada's first is 0) ignores Terreno
     * Difícil — pays 1UD per hex where it would pay {@code
     * MovementTerrainService#DIFFICULT_TERRAIN_COST}. Read by {@code
     * MovementTerrainService#ignoresDifficultTerrain}; {@code false} by default. {@link
     * StrengthAbility#MOVIMENTO_LIVRE} is the consumer.
     */
    default boolean ignoresDifficultTerrain(int movementIndex) {
        return false;
    }

    /**
     * A bonus toward this Perícia's own roll ({@code skillRollBonus}), granted on <b>every</b>
     * roll currently governed by rolledDomain — unlike {@link #resolveFirstRollOfTurnBonus},
     * not limited to the first such roll each Turn — e.g. {@link
     * InstinctAbility#SENTIR_A_INTENCAO}'s Vantagem on Instinto-governed rolls while the
     * holder has no Título Aventyr yet. Takes character explicitly (rather than being a
     * reflection-invoked {@code @Modifier} method, or scoped only by rolledDomain like {@link
     * #resolveFirstRollOfTurnBonus}) because the condition may depend on Character-level state
     * — here, held Títulos — a no-arg method or the domain alone can't see. Summed by {@code
     * org.aventyrs.core.skill.AbstractSkillInteraction} across {@code
     * Character#getAttributeAbilities()}. Zero by default; only override on a constant whose
     * rules text grants a bonus scoped to every roll governed by one Attribute like this.
     */
    default int resolveAttributeDomainRollBonus(AttributeDomain rolledDomain, Character character) {
        return 0;
    }

    /**
     * {@link #resolveAttributeDomainRollBonus(AttributeDomain, Character)} seeing the roller's sheet — its per-Turn
     * action log, where "imediatamente após" lives: {@link CharismaAbility#AGRESSAO_ANUNCIADA} (a Força/Destreza roll
     * right after a Carisma one) and {@link CharismaAbility#FLOREIO_ARCANO} (a Carisma roll right after a Magia). The
     * roll being resolved is not yet logged, so the log's last entry is the action before it. holder is {@code null}
     * on a sheet-less path, which reads as "condition not met". Defaults to the shorter form.
     */
    default int resolveAttributeDomainRollBonus(final AttributeDomain rolledDomain, final Character character,
                                                final CombatantSheet holder) {
        return resolveAttributeDomainRollBonus(rolledDomain, character);
    }

    /**
     * Same shape as {@link #resolveAttributeDomainRollBonus}, but for a GD reduction (a
     * difficulty-reduction step count, like {@code SkillCompetencyAbility#getDifficultyReduction}) —
     * e.g. {@link InstinctAbility#SENTIR_A_INTENCAO}'s own "reduz a GD da rolagem em -1 nível"
     * once the holder has acquired a Título Aventyr, replacing that same constant's Vantagem
     * branch above rather than adding to it (the two are mutually exclusive by character
     * state, never both nonzero for the same character). Zero by default; only override on a
     * constant whose rules text grants a GD reduction scoped to one Attribute's rolls like
     * this.
     */
    default int resolveAttributeDomainDifficultyReduction(AttributeDomain rolledDomain, Character character) {
        return 0;
    }

    /**
     * The flat Redução de Dano this Habilidade grants against an incoming hit, conditioned on
     * damageType and source (the attacker's own {@link CombatantSheet}) — e.g. {@link
     * VigorAbility#RIGIDEZ_DA_MONTANHA}'s Dano-Físico-only reduction, -1 normally or -2 once
     * source's own {@code SizeCategory} is smaller than target's. target is this Habilidade's
     * own holder — its own {@link CombatantSheet} — mirroring {@code
     * org.aventyrs.core.ego.EgoAdvantage#resolveSkillSpecificRollBonus}'s identical "target =
     * the holder's own sheet, passed explicitly because the bonus may depend on the holder's
     * own state" shape, here extended with a second {@link CombatantSheet} (source) for an
     * ability that also needs to know who dealt the hit — the reverse of {@code
     * SkillCompetencyAbility#resolveAttackRollBonus(CombatantSheet actor, CombatantSheet
     * attackTarget)}'s outgoing-side pair, since this is the defender's own ability reacting to
     * an incoming hit rather than the attacker's. damageType/source may be {@code null}
     * (unclassified damage / no attacker identity known) — every override is expected to treat
     * either as "condition not met" wherever its own rules text depends on it, the same
     * restraint every other {@code resolve*} hook here already applies; a flat, type-scoped-only
     * reduction can still apply with a {@code null} source, since it doesn't need one. Summed by
     * {@code org.aventyrs.core.character.services.DamageService#getTotalDamageReduction} across
     * {@code Character#getAttributeAbilities()}. Zero by default; only override on a constant
     * whose rules text grants a reduction scoped to one {@link DamageType} like this.
     */
    default int resolveDamageReduction(DamageType damageType, CombatantSheet source, CombatantSheet target) {
        return 0;
    }

    /**
     * A bonus to the Perícia roll of an attack made as part of a named {@link Manoeuvre} — e.g.
     * {@link DexterityAbility#IMPLACAVEL}'s "você recebe Vantagem em suas jogadas de Ataque
     * Corpo-a-Corpo", whose surrounding clauses scope it to the holder's Investidas. Summed by
     * {@code org.aventyrs.core.skill.AbstractSkillInteraction} across {@code
     * Character#getAttributeAbilities()}, off {@code SkillRoll#getManoeuvre()}.
     *
     * <p>manoeuvre is {@code null} for an ordinary attack, which every override must read as
     * "condition not met" — a manoeuvre-scoped bonus that leaked onto plain attacks would be the
     * opposite of what its rules text says. skillType is the Perícia actually being rolled, so a
     * clause naming one ({@code IMPLACAVEL}'s Corpo-a-Corpo) narrows on it rather than trusting
     * the manoeuvre to imply it.
     *
     * <p>Vantagem is a flat {@code Skill#ADVANTAGE_BONUS} (+2) here as everywhere else, never a
     * reroll. Zero by default. There is deliberately <b>no {@code Feat} twin</b> — no Talento
     * states a manoeuvre-scoped roll bonus; add one with its first consumer.
     */
    default int resolveManoeuvreRollBonus(Manoeuvre manoeuvre, SkillType skillType) {
        return 0;
    }

    /**
     * {@link #resolveManoeuvreRollBonus(Manoeuvre, SkillType)} knowing the Atributo that governs the roll
     * once every substitution has run — {@link StrengthAbility#ESTILHACADOR}'s "Perícias de Ataque
     * (baseadas em Força)" against objects. Defaults to the shorter form, so every existing override
     * keeps working; override this one only when the governing Atributo matters.
     */
    default int resolveManoeuvreRollBonus(final Manoeuvre manoeuvre, final SkillType skillType,
                                          final AttributeDomain governingDomain) {
        return resolveManoeuvreRollBonus(manoeuvre, skillType);
    }

    /**
     * A flat bonus on the holder's dano against an object it strikes ({@code ObjectAttackService#strike},
     * {@code Manoeuvre#ATACAR_OBJETOS}) when the roll was governed by governingDomain — {@link
     * StrengthAbility#ESTILHACADOR}'s Vantagem em Danos contra objetos. Added to the raw damage before
     * the manoeuvre's Meio-Dano. Zero by default.
     */
    default int resolveObjectDamageBonus(final AttributeDomain governingDomain) {
        return 0;
    }

    /**
     * How much further than the baseline an Investida of this ability's holder may travel, as a
     * <b>multiplier increase</b> on Movimento Base — {@link DexterityAbility#IMPLACAVEL}'s "você
     * pode percorrer até o triplo do seu Movimento Base, ao invés do dobro" is exactly 1, lifting
     * {@code ChargeService#BASE_MOVEMENT_MULTIPLIER}'s 2 to 3.
     *
     * <p>A multiplier rather than a UD figure because that is the shape the rules state it in, and
     * because the two are not interchangeable: the charge allowance is Movimento Base times this,
     * so the same clause is worth more to a larger or faster charger. It is <b>not</b> a {@code
     * ModifierType#MOVEMENT} bonus — that widens what one Ponto de Ação buys on every movement,
     * which this does not touch. Summed by {@code ChargeService#getMovementAllowance}; zero by
     * default.
     */
    default int resolveChargeMovementMultiplierIncrease() {
        return 0;
    }

    /**
     * Whether this ability exempts its holder's movement from provoking Reações — {@link
     * DexterityAbility#IMPLACAVEL}'s "suas investidas não provocam Reações de outros personagens
     * na cena", which is scoped to the Investida and to nothing else.
     *
     * <p>manoeuvre is the manoeuvre being performed, {@code null} for an ordinary movement. An
     * override that exempts only one manoeuvre must therefore test it: returning {@code true}
     * unconditionally would exempt every walk its holder ever takes. Read by {@code
     * MovementReactionService#getProvokedReactors}; {@code false} by default.
     */
    default boolean exemptsFromMovementReactions(Manoeuvre manoeuvre) {
        return false;
    }

    /**
     * Whether this ability spares its holder the Redutor an Investida normally inflicts on its
     * own charger when the attack misses — {@link DexterityAbility#IMPLACAVEL}'s "quando
     * malsucedido, você não recebe o redutor padrão de -2 em suas Defesas".
     *
     * <p>It waives the penalty outright rather than reducing it, so it is a boolean and not a
     * figure; the word "padrão" in that clause is also what identifies the -2 as the manoeuvre's
     * own baseline rather than something this Habilidade invented. Read by {@code
     * ChargeService#applyOutcome}; {@code false} by default.
     */
    default boolean waivesChargeMissDefensePenalty() {
        return false;
    }

    // ---- Anatomy and resistance hooks --------------------------------------------------------
    //
    // Added for the Habilidades Monstruosas (see org.aventyrs.core.monster.MonstrousAbilityGrant),
    // and read by AbstractCombatantSheet / DamageServiceImpl for every combatant alike — a foe the
    // client plays through a CharacterSheet is covered as much as a MonsterSheet. Each defaults to
    // "nothing", so no existing Habilidade de Atributo changes.

    /** Instances of Resistência Elemental (RE) against element — each worth {@link org.aventyrs.core.character.Resistance#RE}. */
    default int resolveElementalResistanceInstances(final org.aventyrs.core.magic.ElementalType element) {
        return 0;
    }

    /** Whether a hit of this kind is reduced to half — a Meio-Dano limited to a {@code DamageScope}. */
    default boolean halvesDamage(final org.aventyrs.core.character.DamageType damageType,
                                 final org.aventyrs.core.character.DamageDescriptor descriptor) {
        return false;
    }

    /** Whether a hit of this kind deals nothing at all — "Imunidade ao Elemento escolhido". */
    default boolean isImmuneToDamage(final org.aventyrs.core.character.DamageType damageType,
                                     final org.aventyrs.core.character.DamageDescriptor descriptor) {
        return false;
    }

    /**
     * Whether this holder is <i>vulnerável</i> to a hit of this kind. Reported only: no rules text
     * gives a Vulnerabilidade a number (racas.txt and criacao-de-monstros.txt name it, never size
     * it), so {@code DamageServiceImpl} does not change the damage — see {@code
     * CombatantSheet#isVulnerableToDamage}.
     */
    default boolean isVulnerableToDamage(final org.aventyrs.core.character.DamageType damageType,
                                         final org.aventyrs.core.character.DamageDescriptor descriptor) {
        return false;
    }

    /** Efeitos Críticos this Habilidade makes its holder shrug off. */
    default Set<org.aventyrs.core.effect.CriticalEffectType> resolveCriticalEffectImmunities() {
        return Set.of();
    }

    /** Standing Resistência a Críticos — the value {@code CombatantSheet#getTotalCriticalResistance} sums (2 per instance). */
    default int resolveCriticalResistance() {
        return 0;
    }

    /** "Imunidade à Críticos Menores". */
    default boolean ignoresMinorCriticalEffects() {
        return false;
    }

    /** Whether a {@code Condição} of this type never takes hold on the holder. */
    default boolean isImmuneToCondition(final org.aventyrs.core.sheet.ConditionType conditionType) {
        return false;
    }

    /** Armas Naturais this Habilidade grants its holder — read by {@code Character#getNaturalWeapons()}. */
    default List<org.aventyrs.core.item.NaturalWeapon> getGrantedNaturalWeapons() {
        return List.of();
    }

    /** Whether the holder is always in flight — "O Movimento Base de Voo está sempre ativo". */
    default boolean keepsFlying() {
        return false;
    }

    /**
     * What the holder gains while it is in flight ("Bônus Racial de +5 em Defesas enquanto voando")
     * — applied by {@code CombatantSheet#setFlying(true)} and lifted when it lands. Fresh instances
     * each call: a {@code TemporaryEffect} is mutable.
     */
    default List<org.aventyrs.core.sheet.TemporaryEffect> resolveWhileFlyingEffects() {
        return List.of();
    }

    /**
     * Effects the holder gains after taking finalDamage (&gt; 0) — "Após sofrer danos recupera 1d6PV
     * durante 2 Rodadas". Applied to the holder by {@code DamageService#notifyDamageTaken}.
     */
    default List<org.aventyrs.core.sheet.TemporaryEffect> resolveDamageTakenEffects(
            final org.aventyrs.core.sheet.CombatantSheet holder, final int finalDamage) {
        return List.of();
    }
}
