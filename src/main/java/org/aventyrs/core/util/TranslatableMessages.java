package org.aventyrs.core.util;

public class TranslatableMessages {
    public static final String NOT_ENOUGH_EXPERIENCE = "NOT_ENOUGH_EXPERIENCE";
    public static final String INVALID_ATTRIBUTE_POINT_ALLOCATION = "INVALID_ATTRIBUTE_POINT_ALLOCATION";
    public static final String INVALID_RACIAL_BONUS_ALLOCATION = "INVALID_RACIAL_BONUS_ALLOCATION";
    public static final String ATTRIBUTE_BASE_AT_MAXIMUM = "ATTRIBUTE_BASE_AT_MAXIMUM";
    public static final String SKILL_GRADUATION_AT_MAXIMUM = "SKILL_GRADUATION_AT_MAXIMUM";
    public static final String ATTRIBUTE_ABILITY_ALREADY_CHOSEN = "ATTRIBUTE_ABILITY_ALREADY_CHOSEN";
    public static final String NO_ATTRIBUTE_ABILITY_SLOT_AVAILABLE = "NO_ATTRIBUTE_ABILITY_SLOT_AVAILABLE";
    public static final String INVALID_EGO_POINT_ALLOCATION = "INVALID_EGO_POINT_ALLOCATION";
    public static final String NO_PARTICIPANTS_IN_SCENE = "NO_PARTICIPANTS_IN_SCENE";
    public static final String CHARACTER_SHEET_NOT_IN_SCENE = "CHARACTER_SHEET_NOT_IN_SCENE";
    public static final String INVALID_TURN_CURSOR = "INVALID_TURN_CURSOR";
    public static final String INVALID_SKILL_ROLL = "INVALID_SKILL_ROLL";
    public static final String INVALID_ACTION_COST = "INVALID_ACTION_COST";
    public static final String INVALID_DIE_ROLL = "INVALID_DIE_ROLL";
    public static final String REQUIRED_SKILL_TRAIT_NOT_HELD = "REQUIRED_SKILL_TRAIT_NOT_HELD";
    public static final String UNKNOWN_SKILL_TYPE = "UNKNOWN_SKILL_TYPE";
    public static final String INVALID_PARENT_RACE = "INVALID_PARENT_RACE";
    public static final String INVALID_INHERITED_RACIAL_ABILITIES = "INVALID_INHERITED_RACIAL_ABILITIES";
    public static final String INVALID_INHERITED_ATTRIBUTE_ABILITIES = "INVALID_INHERITED_ATTRIBUTE_ABILITIES";
    public static final String INVALID_ELEMENTAL_LINEAGE = "INVALID_ELEMENTAL_LINEAGE";
    public static final String CRITICAL_EFFECT_REQUIRES_A_CRITICAL_HIT = "CRITICAL_EFFECT_REQUIRES_A_CRITICAL_HIT";
    public static final String INVALID_PRIMOR_EGO_DOMAIN = "INVALID_PRIMOR_EGO_DOMAIN";
    public static final String ACTIVE_ABILITY_NOT_HELD = "ACTIVE_ABILITY_NOT_HELD";

    /** A weapon cannot be drawn because the character is not carrying it. */
    public static final String WEAPON_NOT_CARRIED = "WEAPON_NOT_CARRIED";

    /** A weapon cannot be drawn because it is already in hand. */
    public static final String WEAPON_ALREADY_DRAWN = "WEAPON_ALREADY_DRAWN";

    /** A held Condição forbids getting a weapon into your hands at all — Devorado. */
    public static final String WEAPON_DRAW_PREVENTED = "WEAPON_DRAW_PREVENTED";
    public static final String NOT_ENOUGH_ACTION_POINTS = "NOT_ENOUGH_ACTION_POINTS";
    public static final String NOT_ENOUGH_MAGIC_POINTS = "NOT_ENOUGH_MAGIC_POINTS";

    /** An activated ability's Pontos de Vida cost exceeds what the holder can safely spend. */
    public static final String NOT_ENOUGH_HIT_POINTS = "NOT_ENOUGH_HIT_POINTS";
    public static final String SKILL_NOT_TRAINED = "SKILL_NOT_TRAINED";
    public static final String SKILL_TRAIT_SKILL_TYPE_MISMATCH = "SKILL_TRAIT_SKILL_TYPE_MISMATCH";
    public static final String INITIATIVE_NOT_WON = "INITIATIVE_NOT_WON";
    public static final String INVALID_DAMAGE_TYPE_ELEMENT_PAIRING = "INVALID_DAMAGE_TYPE_ELEMENT_PAIRING";
    public static final String INVALID_DAMAGE_BASE = "INVALID_DAMAGE_BASE";
    public static final String REQUIRED_TITLE_TRAIT_NOT_HELD = "REQUIRED_TITLE_TRAIT_NOT_HELD";
    public static final String REQUIRED_ATTRIBUTE_ABILITY_NOT_HELD = "REQUIRED_ATTRIBUTE_ABILITY_NOT_HELD";
    public static final String TITLE_NOT_HELD = "TITLE_NOT_HELD";
    public static final String EXTRA_SUPREMA_ALREADY_GRANTED = "EXTRA_SUPREMA_ALREADY_GRANTED";
    public static final String TITLE_ABILITY_PREREQUISITE_NOT_MET = "TITLE_ABILITY_PREREQUISITE_NOT_MET";
    public static final String FEAT_PREREQUISITE_NOT_MET = "FEAT_PREREQUISITE_NOT_MET";
    public static final String NOT_AN_ATTACK_SKILL = "NOT_AN_ATTACK_SKILL";

    /**
     * An attack named more targets than the attacker's Talentos entitle them to — see {@code
     * AttackTargetingService#getMaximumTargets}.
     */
    public static final String TOO_MANY_ATTACK_TARGETS = "TOO_MANY_ATTACK_TARGETS";
    public static final String INVALID_AREA_OF_EFFECT = "INVALID_AREA_OF_EFFECT";
    public static final String INVALID_SPELL_TARGETING = "INVALID_SPELL_TARGETING";
    public static final String INVALID_SPELL_TREE = "INVALID_SPELL_TREE";
    public static final String INVALID_SPELL_DURATION = "INVALID_SPELL_DURATION";
    public static final String INVALID_SPELL_ACTIVATION = "INVALID_SPELL_ACTIVATION";
    public static final String SPELL_PREREQUISITE_NOT_MET = "SPELL_PREREQUISITE_NOT_MET";
    public static final String INVALID_SPELL_CAST_TARGET = "INVALID_SPELL_CAST_TARGET";
    public static final String INVALID_SPELL_DAMAGE = "INVALID_SPELL_DAMAGE";

    /** The caster is under a Condição that forbids Conjurar Magias — Silêncio. */
    public static final String SPELL_CASTING_PREVENTED = "SPELL_CASTING_PREVENTED";

    /** The actor is under a Condição that forbids activating Habilidades — Silêncio. */
    public static final String ABILITY_ACTIVATION_PREVENTED = "ABILITY_ACTIVATION_PREVENTED";

    /** The crafter doesn't hold the Especialização de Profissão a given kind of item needs. */
    public static final String CRAFTING_TRADE_NOT_HELD = "CRAFTING_TRADE_NOT_HELD";

    /** Fabricating an Obra-Prima of this Raridade needs a higher Profissão Graduação than the crafter has. */
    public static final String MASTERPIECE_GRADUATION_TOO_LOW = "MASTERPIECE_GRADUATION_TOO_LOW";

    /** An Aprimoramento can only be fitted to an Obra-Prima. */
    public static final String ITEM_NOT_A_MASTERPIECE = "ITEM_NOT_A_MASTERPIECE";

    /** The item already carries as many Aprimoramentos as its Categoria de Peso allows. */
    public static final String IMPROVEMENT_SLOTS_FULL = "IMPROVEMENT_SLOTS_FULL";

    /** The item already carries this exact Aprimoramento — "Aprimoramentos diferentes". */
    public static final String DUPLICATE_IMPROVEMENT = "DUPLICATE_IMPROVEMENT";

    /** A shared catalog {@code ItemTemplate} cannot be forged, repaired or modified in place. */
    public static final String CANNOT_MODIFY_TEMPLATE = "CANNOT_MODIFY_TEMPLATE";

    /**
     * An {@code ItemActiveAbility} was asked for on a forge that is not making a Regalia — only a
     * Regalia carries one ({@code AbstractItem#setActiveAbility}'s own guard, reported here as a
     * refusal a caller can show rather than an {@code IllegalStateException}).
     */
    public static final String ACTIVE_ABILITY_REQUIRES_REGALIA = "ACTIVE_ABILITY_REQUIRES_REGALIA";

    /**
     * No Talento the crafter holds permits forging a Regalia of this grade <em>right now</em> —
     * either they never acquired the Talento de Artífice for it, or its use-condition isn't met
     * (no Regalia of that grade in their possession). One message for both, because {@code
     * Feat#itsAllowedToCraftRegalia} answers the two as one question.
     */
    public static final String REGALIA_CRAFTING_NOT_PERMITTED = "REGALIA_CRAFTING_NOT_PERMITTED";

    /**
     * The Centelha donor is not willing — "Se o personagem doador não for voluntário ou se
     * arrepender em meio ao processo a criação da Regalia irá falhar".
     */
    public static final String REGALIA_DONOR_NOT_WILLING = "REGALIA_DONOR_NOT_WILLING";

    /** A Regalia Divina's Centelhas must come from a Dragão, Elemental, Abissal ou Celestial. */
    public static final String REGALIA_DIVINE_DONOR_REQUIRED = "REGALIA_DIVINE_DONOR_REQUIRED";
}
