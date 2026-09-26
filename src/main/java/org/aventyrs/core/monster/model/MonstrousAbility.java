package org.aventyrs.core.monster.model;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.MonsterCategory;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Map;

/**
 * A Habilidade Monstruosa — one entry under a {@link MonsterModel} in {@code
 * criacao-de-monstros.txt}, e.g. "Habilidade – Celeridade [Deviante]".
 *
 * <p>Each has a tier (the bracketed Categoria: the least a monster must be to take it), a passive
 * and/or active effect, and a ladder of "Aprimoramentos dos Deviantes/Predadores/Apex/Abominações"
 * that <b>apply on their own</b> once the monster's Categoria reaches them. So every effect hook
 * here takes the holder's current {@link MonsterCategory}: the same held Habilidade answers
 * differently at GP 20 and at GP 50, with nothing re-acquired.
 *
 * <p>Implemented by one enum per Modelo. <b>Only Abençoado de Cireneia's are implemented</b>
 * ({@link #isImplemented()}); the other five Modelos' constants are catalog entries — name, tier
 * and the rules text — whose effects are applied at the table by hand until each Modelo is built.
 *
 * <p>The hooks mirror how every other trait reaches this core, so nothing downstream needed to
 * learn about monsters:
 *
 * <ul>
 *   <li>{@link #resolveModifier} — a permanent number on a {@link ModifierType} (PA, Iniciativa,
 *   Movimento, Reações, Defesas …). {@code MonstrousAbilityGrant} surfaces it as an ordinary
 *   {@code @Modifier}, so {@code ActionPointsService}, {@code InitiativeService} etc. pick it up
 *   through their existing {@code attributeAbilities} scan.</li>
 *   <li>{@link #resolveRacialAttributeBonuses} — "Bônus Racial de Destreza +2", folded into the
 *   Atributo's racial half at spawn.</li>
 *   <li>{@link #resolveSkillLevelShift} — "Perícias Baseadas em Destreza GD +1": steps on the
 *   ladder, applied after the Categoria's GD ceiling ("Aprimoramentos Monstruosos podem fazer com
 *   que a GD máxima em perícia seja superada").</li>
 *   <li>{@link #resolveActiveAbilities} — the "Efeito Ativo", as an ordinary {@link ActiveAbility}
 *   activated through {@code ActiveAbilityService}, costs and Resfriamento included.</li>
 * </ul>
 *
 * <p>A Habilidade that asks the monster to pick something ("+1 Reação ou +1 Ação Livre", "Escolha
 * um Elemento") lists the options in {@link #getChoiceOptions()}; the pick travels as a plain
 * string beside the Habilidade and comes back into every hook.
 */
public interface MonstrousAbility {

    /** The constant's name — the stable wire key. Supplied by the implementing enum. */
    String name();

    /** The Modelo this Habilidade belongs to. */
    MonsterModel getModel();

    /** The bracketed Categoria — the least a monster must be to take it. */
    MonsterCategory getTier();

    /** Its name as the rules print it, e.g. "Celeridade". */
    String getDisplayName();

    /** The rules text — effects and every Aprimoramento. */
    String getDescription();

    /**
     * Whether this core applies its effects. {@code false} is a catalog entry: it can be held,
     * counted and shown, but its hooks all answer "nothing".
     */
    default boolean isImplemented() {
        return false;
    }

    /** What it asks the monster to choose, or empty when it asks nothing. */
    default List<String> getChoiceOptions() {
        return List.of();
    }

    /**
     * Its permanent contribution to {@code type} at {@code category} — the passive plus every
     * Aprimoramento that category has reached.
     *
     * @param choice the monster's pick from {@link #getChoiceOptions()}, or {@code null}
     */
    default int resolveModifier(final ModifierType type, final MonsterCategory category, final String choice) {
        return 0;
    }

    /** Bônus Racial it grants per Atributo at {@code category}. */
    default Map<AttributeDomain, Integer> resolveRacialAttributeBonuses(final MonsterCategory category,
                                                                         final String choice) {
        return Map.of();
    }

    /** Steps up the GD ladder it grants {@code skill} at {@code category}. */
    default int resolveSkillLevelShift(final SkillType skill, final AttributeDomain governingAttribute,
                                       final MonsterCategory category) {
        return 0;
    }

    /**
     * Its Efeitos Ativos at {@code category}. Called once per spawn; the instances are held by
     * the monster's {@code Character}, and {@code ActiveAbilityService} identifies them by
     * reference, so each call returns fresh ones.
     */
    default List<ActiveAbility> resolveActiveAbilities(final MonsterCategory category, final String choice) {
        return List.of();
    }

    /**
     * Bonus on the Ataque GD it presents during an Investida. A monster never rolls, so "Vantagem
     * nas Rolagens de Ataque … durante Investidas" becomes a Vantagem's +2 on the threshold the
     * defender must beat.
     *
     * <p>TODO: applied by the caller — {@code IncomingAttack} carries no "this was an Investida" flag.
     */
    default int resolveChargeAttackBonus(final MonsterCategory category) {
        return 0;
    }

    /** Flat dano bonus during an Investida — a Vantagem em Danos is +2. Same TODO as {@link #resolveChargeAttackBonus}. */
    default int resolveChargeDamageBonus(final MonsterCategory category) {
        return 0;
    }

    /**
     * "Investidas sempre acertam o alvo". Reported, not enforced — same TODO as {@link
     * #resolveChargeAttackBonus}.
     */
    default boolean chargesAlwaysHit(final MonsterCategory category) {
        return false;
    }
}
