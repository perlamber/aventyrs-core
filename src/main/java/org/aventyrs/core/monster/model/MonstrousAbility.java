package org.aventyrs.core.monster.model;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.feat.FeatCategory;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.MonsterCategory;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A Habilidade Monstruosa — one entry under a {@link MonsterModel} in {@code
 * criacao-de-monstros.txt}, e.g. "Habilidade – Celeridade [Deviante]".
 *
 * <p>Each has a tier (the bracketed Categoria: the least a monster must be to take it), a passive
 * and/or active effect, and a ladder of "Aprimoramentos dos Deviantes/Predadores/Apex/Abominações"
 * that <b>apply on their own</b> once the monster's Categoria reaches them. So every hook takes an
 * {@link AbilityContext} — the holder's Categoria and the picks it made — and the same held
 * Habilidade answers differently at GP 20 and GP 50 with nothing re-acquired.
 *
 * <p>Implemented by one enum per Modelo. {@link #getImplementationStatus()} says how much of each
 * this core applies; a PARTIAL one names what's missing in {@link #getUnappliedNote()}.
 *
 * <p>The hooks mirror how every other trait reaches this core, so nothing downstream needed to
 * learn about monsters. {@code MonstrousAbilityGrant} dresses a held Habilidade as an {@code
 * AttributeAbility}, which is how the ones marked <i>(ability scan)</i> below reach the ordinary
 * services; the rest are read by {@code MonsterBlueprint} when it builds the {@code Character} or
 * spawns the sheet.
 *
 * <ul>
 *   <li>{@link #resolveModifier} <i>(ability scan)</i> — a permanent number on a {@link ModifierType}.</li>
 *   <li>{@link #resolveRacialAttributeBonuses} — "Bônus Racial de X +N", folded into the Atributo.</li>
 *   <li>{@link #resolveSkillLevelShift} — "Perícias baseadas em Destreza GD +1".</li>
 *   <li>{@link #resolveActiveAbilities} — the Efeitos Ativos.</li>
 *   <li>Anatomy <i>(ability scan)</i>: RE, scoped Meio-Dano, immunities and vulnerabilities to damage,
 *   Efeitos Críticos, Resistência a Críticos, Condições, Armas Naturais, flight.</li>
 *   <li>Build-time grants: standing effects, Magias, Talentos and Talento slots.</li>
 * </ul>
 */
public interface MonstrousAbility {

    /** The constant's name — the stable wire key, unique within its Modelo. Supplied by the enum. */
    String name();

    /** The Modelo this Habilidade belongs to. */
    MonsterModel getModel();

    /** The bracketed Categoria — the least a monster must be to take it. */
    MonsterCategory getTier();

    /** Its name as the rules print it, e.g. "Celeridade". */
    String getDisplayName();

    /** The rules text — effects and every Aprimoramento. */
    String getDescription();

    // ---- Status ----------------------------------------------------------------------------------

    /** How much of this Habilidade this core applies. */
    default ImplementationStatus getImplementationStatus() {
        return ImplementationStatus.TABLE_ONLY;
    }

    /** What isn't applied, and which missing system blocks it — {@code null} when all of it is. */
    default String getUnappliedNote() {
        return null;
    }

    /** Whether every clause is applied — {@link #getImplementationStatus()} is {@code APPLIED}. */
    default boolean isImplemented() {
        return getImplementationStatus() == ImplementationStatus.APPLIED;
    }

    /**
     * The key of a trait this Habilidade shares with a sibling in its Modelo — Anatomia Vegetal,
     * granted by both Nascido Habilidades — or {@code null}. Of several held Habilidades naming one
     * key, only the first gets {@link AbilityContext#ownsSharedTraits()}, and grants it.
     */
    default String sharedTraitKey() {
        return null;
    }

    // ---- Picks -----------------------------------------------------------------------------------

    /** What it asks the monster to pick at category — empty for a Habilidade that asks nothing. */
    default List<ChoiceSpec> getChoiceSpecs(final MonsterCategory category) {
        return List.of();
    }

    // ---- Numbers ---------------------------------------------------------------------------------

    /** Its permanent contribution to type — the passive plus every Aprimoramento reached. */
    default int resolveModifier(final ModifierType type, final AbilityContext context) {
        return 0;
    }

    /** Bônus Racial it grants per Atributo. */
    default Map<AttributeDomain, Integer> resolveRacialAttributeBonuses(final AbilityContext context) {
        return Map.of();
    }

    /** Steps up the GD ladder it grants skill (governed by governingAttribute). */
    default int resolveSkillLevelShift(final SkillType skill, final AttributeDomain governingAttribute,
                                       final AbilityContext context) {
        return 0;
    }

    /** "+N números" on the Margem Crítica Menor of the holder's attacks with skill. */
    default int resolveCriticalMarginIncrease(final SkillType skill, final AbilityContext context) {
        return 0;
    }

    // ---- Efeitos Ativos --------------------------------------------------------------------------

    /**
     * Its Efeitos Ativos. Called once per spawn; {@code ActiveAbilityService} identifies them by
     * reference, so each call returns fresh instances.
     */
    default List<ActiveAbility> resolveActiveAbilities(final AbilityContext context) {
        return List.of();
    }

    /**
     * Bonus on the Ataque GD it presents during an Investida — a Vantagem's +2, since a monster
     * never rolls. TODO: applied by the caller; {@code IncomingAttack} has no Investida flag.
     */
    default int resolveChargeAttackBonus(final AbilityContext context) {
        return 0;
    }

    /** Flat dano bonus during an Investida. Same TODO as {@link #resolveChargeAttackBonus}. */
    default int resolveChargeDamageBonus(final AbilityContext context) {
        return 0;
    }

    /** "Investidas sempre acertam o alvo". Reported, not enforced. */
    default boolean chargesAlwaysHit(final AbilityContext context) {
        return false;
    }

    // ---- Anatomy -----------------------------------------------------------------------------------

    /** Instances of RE against element. */
    default int resolveElementalResistanceInstances(final ElementalType element, final AbilityContext context) {
        return 0;
    }

    /** Whether a hit of this kind is halved. */
    default boolean halvesDamage(final DamageType type, final DamageDescriptor descriptor, final AbilityContext context) {
        return false;
    }

    /** Whether a hit of this kind deals nothing. */
    default boolean isImmuneToDamage(final DamageType type, final DamageDescriptor descriptor, final AbilityContext context) {
        return false;
    }

    /** Whether the holder is vulnerável to a hit of this kind (reported only — see {@code CombatantSheet#isVulnerableToDamage}). */
    default boolean isVulnerableToDamage(final DamageType type, final DamageDescriptor descriptor, final AbilityContext context) {
        return false;
    }

    default Set<CriticalEffectType> resolveCriticalEffectImmunities(final AbilityContext context) {
        return Set.of();
    }

    /** Standing Resistência a Críticos, 2 per instance. */
    default int resolveCriticalResistance(final AbilityContext context) {
        return 0;
    }

    default boolean ignoresMinorCriticalEffects(final AbilityContext context) {
        return false;
    }

    default boolean isImmuneToCondition(final ConditionType conditionType, final AbilityContext context) {
        return false;
    }

    default List<NaturalWeapon> resolveNaturalWeapons(final AbilityContext context) {
        return List.of();
    }

    /** Whether the holder never lands. */
    default boolean keepsFlying(final AbilityContext context) {
        return false;
    }

    /** What the holder gains while in flight — fresh instances each call. */
    default List<TemporaryEffect> resolveWhileFlyingEffects(final AbilityContext context) {
        return List.of();
    }

    /** What the holder gains after taking finalDamage — fresh instances each call. */
    default List<TemporaryEffect> resolveDamageTakenEffects(final int finalDamage, final AbilityContext context) {
        return List.of();
    }

    // ---- Build time ------------------------------------------------------------------------------

    /**
     * Open-ended effects the holder carries from the moment it spawns ("Recupera Vigor PV por
     * Rodada", "recebe Roubo de Vida 2"). {@code character} is the holder, for a figure that scales
     * with an Atributo. Fresh instances each call.
     */
    default List<TemporaryEffect> resolveStandingEffects(final AbilityContext context, final Character character) {
        return List.of();
    }

    /** Magias it teaches ("Aprende 2 Árvores de Magia Elemental"). */
    default List<Spell> resolveGrantedSpells(final AbilityContext context) {
        return List.of();
    }

    /** Talentos it grants outright ("Mimetizando os efeitos do Talento Aparência Inofensiva"). */
    default List<Feat> resolveGrantedFeats(final AbilityContext context) {
        return List.of();
    }

    /** Talento slots it adds to the monster's budget ("Recebe um Talento Geral ou Monstruoso adicional"). */
    default int resolveBonusFeatSlots(final AbilityContext context) {
        return 0;
    }

    /** Talento categories it lets the monster take beyond Geral and Monstruoso ("um Talento Racial da raça escolhida"). */
    default Set<FeatCategory> resolveAllowedFeatCategories(final AbilityContext context) {
        return Set.of();
    }
}
