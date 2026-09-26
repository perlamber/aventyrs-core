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
    public static final String SCENE_ALREADY_IN_COMBAT = "SCENE_ALREADY_IN_COMBAT";
    /** {@code Scene#endCombat()} was asked of a Scene that is not a Cena de Combate. */
    public static final String SCENE_NOT_IN_COMBAT = "SCENE_NOT_IN_COMBAT";
    public static final String INVALID_SKILL_ROLL = "INVALID_SKILL_ROLL";
    public static final String INVALID_ACTION_COST = "INVALID_ACTION_COST";

    /**
     * A {@code ActionCost.Kind#DYNAMIC} cost — a price whose amount the activating player still
     * chooses — was asked what it spent, or was filed on a {@code CombatantAction}. Resolve it
     * against the chosen amount first ({@code ActionCost#resolve}).
     */
    public static final String UNRESOLVED_ACTION_COST = "UNRESOLVED_ACTION_COST";
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

    /**
     * A manoeuvre needs the weapon already in hand and this one is carried but sheathed. An
     * Investida bundles a movement with an attack and prices neither a draw nor a moment to make
     * one — see {@code ChargeService}.
     */
    public static final String WEAPON_NOT_DRAWN = "WEAPON_NOT_DRAWN";

    /** An Investida was declared with something that is not swung as an Ataque Corpo-a-Corpo. */
    public static final String CHARGE_REQUIRES_MELEE_WEAPON = "CHARGE_REQUIRES_MELEE_WEAPON";

    /** An Investida was declared by a combatant a held Condição forbids moving — Agarrado/Imobilizado. */
    public static final String CHARGE_MOVEMENT_PREVENTED = "CHARGE_MOVEMENT_PREVENTED";

    /** An Investida was declared from Terreno Difícil, or along a path entering it. */
    public static final String CHARGE_IN_DIFFICULT_TERRAIN = "CHARGE_IN_DIFFICULT_TERRAIN";

    /**
     * An Investida was declared by a combatant who already Reposicionou this Turn — a Reposicionar
     * and spending Pontos de Ação moving exclude each other (table ruling).
     */
    public static final String CHARGE_AFTER_REPOSITION = "CHARGE_AFTER_REPOSITION";

    /** A Reposicionar was declared by a combatant a held Condição forbids moving. */
    public static final String REPOSITION_MOVEMENT_PREVENTED = "REPOSITION_MOVEMENT_PREVENTED";

    /** A Reposicionar was declared with no Ação Livre to spend on it. */
    public static final String REPOSITION_REQUIRES_FREE_ACTION = "REPOSITION_REQUIRES_FREE_ACTION";

    /** A Reposicionar was declared from Terreno Difícil, or toward a hex that is. */
    public static final String REPOSITION_IN_DIFFICULT_TERRAIN = "REPOSITION_IN_DIFFICULT_TERRAIN";

    /**
     * A Reposicionar was declared by a combatant who already spent Pontos de Ação moving this Turn
     * (an Investida included) — the two exclude each other (table ruling).
     */
    public static final String REPOSITION_AFTER_MOVEMENT = "REPOSITION_AFTER_MOVEMENT";

    /** The attacker cannot presently attack with this weapon at all — a Forma suppressing it. */
    public static final String CANNOT_ATTACK_WITH_WEAPON = "CANNOT_ATTACK_WITH_WEAPON";
    public static final String NOT_ENOUGH_ACTION_POINTS = "NOT_ENOUGH_ACTION_POINTS";

    /**
     * An ability taken as a Reação was activated by a holder entitled to none — see {@code
     * ReactionsService}. Nothing here counts a Reação as <em>spent</em>, so this is "entitled to
     * any at all", not a live pool.
     */
    public static final String NOT_ENOUGH_REACTIONS = "NOT_ENOUGH_REACTIONS";

    /** An ability taken as an Ação Livre was activated by a holder entitled to none — see {@code FreeActionsService}. */
    public static final String NOT_ENOUGH_FREE_ACTIONS = "NOT_ENOUGH_FREE_ACTIONS";
    public static final String NOT_ENOUGH_MAGIC_POINTS = "NOT_ENOUGH_MAGIC_POINTS";

    /** An activated ability's Pontos de Vida cost exceeds what the holder can safely spend. */
    public static final String NOT_ENOUGH_HIT_POINTS = "NOT_ENOUGH_HIT_POINTS";
    public static final String SKILL_NOT_TRAINED = "SKILL_NOT_TRAINED";
    public static final String SKILL_TRAIT_SKILL_TYPE_MISMATCH = "SKILL_TRAIT_SKILL_TYPE_MISMATCH";
    public static final String INITIATIVE_NOT_WON = "INITIATIVE_NOT_WON";
    public static final String INVALID_DAMAGE_TYPE_ELEMENT_PAIRING = "INVALID_DAMAGE_TYPE_ELEMENT_PAIRING";
    public static final String INVALID_DAMAGE_BASE = "INVALID_DAMAGE_BASE";
    public static final String REQUIRED_TITLE_TRAIT_NOT_HELD = "REQUIRED_TITLE_TRAIT_NOT_HELD";
    /** The Título trait names no {@code AbstractTitleAbilityInteraction} — it is passive, or its activation isn't built. */
    public static final String TITLE_ABILITY_NOT_ACTIVATABLE = "TITLE_ABILITY_NOT_ACTIVATABLE";
    /** The Título activation acts on the live Scene, and the request named none. */
    public static final String TITLE_ABILITY_REQUIRES_SCENE = "TITLE_ABILITY_REQUIRES_SCENE";
    /** The Título activation needs the player's choice between its branches, and the request made none. */
    public static final String TITLE_ABILITY_CHOICE_REQUIRED = "TITLE_ABILITY_CHOICE_REQUIRED";
    /** The Título activation acts on somebody else, and the request named no target (or named the activator). */
    public static final String TITLE_ABILITY_REQUIRES_TARGET = "TITLE_ABILITY_REQUIRES_TARGET";
    /** A teleportation was declared to somewhere further than the ability's own reach — see {@code Teleportation}. */
    public static final String TELEPORT_TARGET_OUT_OF_RANGE = "TELEPORT_TARGET_OUT_OF_RANGE";
    /** The Título activation blesses an Armadura or Escudo, and the activator is using neither. */
    public static final String TITLE_ABILITY_REQUIRES_ARMOR_OR_SHIELD = "TITLE_ABILITY_REQUIRES_ARMOR_OR_SHIELD";
    /** The Título ability has already been activated as many times as it may be this Turno. */
    public static final String TITLE_ABILITY_ACTIVATION_LIMIT_REACHED = "TITLE_ABILITY_ACTIVATION_LIMIT_REACHED";
    /** The Título choice was already made and "depois de escolhido não é possível mudar" — or the pick is not one of the offered options. */
    public static final String TITLE_ABILITY_CHOICE_LOCKED = "TITLE_ABILITY_CHOICE_LOCKED";
    /** The Título activation needs its activator armed with nothing but Armas Naturais, and a weapon is drawn. */
    public static final String TITLE_ABILITY_REQUIRES_NATURAL_WEAPONS_ONLY = "TITLE_ABILITY_REQUIRES_NATURAL_WEAPONS_ONLY";
    /** The Título activation's target is not an enemy within the distance the ability names. */
    public static final String TITLE_ABILITY_TARGET_OUT_OF_RANGE = "TITLE_ABILITY_TARGET_OUT_OF_RANGE";
    public static final String REQUIRED_ATTRIBUTE_ABILITY_NOT_HELD = "REQUIRED_ATTRIBUTE_ABILITY_NOT_HELD";
    public static final String TITLE_NOT_HELD = "TITLE_NOT_HELD";
    public static final String EXTRA_SUPREMA_ALREADY_GRANTED = "EXTRA_SUPREMA_ALREADY_GRANTED";
    public static final String TITLE_ABILITY_PREREQUISITE_NOT_MET = "TITLE_ABILITY_PREREQUISITE_NOT_MET";
    /** An activation or cast asked to pay in PV, and no held Título lets it (Transferir Vitalidade). */
    public static final String HIT_POINT_PAYMENT_NOT_PERMITTED = "HIT_POINT_PAYMENT_NOT_PERMITTED";
    /** A Título trait whose clause limits it to once per Rodada was activated again this Rodada. */
    public static final String TITLE_ABILITY_ALREADY_USED_THIS_ROUND = "TITLE_ABILITY_ALREADY_USED_THIS_ROUND";
    /** A Título activation needs a target other than its activator and was given none. */
    public static final String TITLE_ABILITY_TARGET_REQUIRED = "TITLE_ABILITY_TARGET_REQUIRED";
    /** A Curandeiro heal was aimed at a target it cannot affect again until the Curandeiro's Descanso Longo. */
    public static final String TARGET_ALREADY_AFFECTED_UNTIL_REST = "TARGET_ALREADY_AFFECTED_UNTIL_REST";
    /** A Título trait that needs a wounded target was aimed at one with no damage. */
    public static final String TARGET_NOT_WOUNDED = "TARGET_NOT_WOUNDED";
    /** A Título activation's choice combination is not allowed in the current situation. */
    public static final String TITLE_ABILITY_CHOICE_NOT_PERMITTED = "TITLE_ABILITY_CHOICE_NOT_PERMITTED";
    public static final String TITLE_ACQUISITION_PREVENTED = "TITLE_ACQUISITION_PREVENTED";
    public static final String FEAT_PREREQUISITE_NOT_MET = "FEAT_PREREQUISITE_NOT_MET";

    /**
     * The bare catalog constant of a Talento whose acquisition requires the player to choose
     * something was passed to {@code FeatService#grantFeat} — see {@code
     * Feat#resolveRequiredChoices}. Grant the acquired, choice-carrying form instead.
     */
    public static final String FEAT_REQUIRES_CHOICE = "FEAT_REQUIRES_CHOICE";

    /**
     * A creation-time Talento selection doesn't fit the character's starting slots — the wrong
     * number of picks, or a pick its slot doesn't offer. See {@code
     * CharacterCreationService#grantStartingFeats}.
     */
    public static final String INVALID_STARTING_FEAT_SELECTION = "INVALID_STARTING_FEAT_SELECTION";

    /**
     * A Talento only a newly created character may take ("Apenas personagens recém-criados") was
     * passed to {@code FeatService#grantFeat} — see {@code Feat#isAcquirableOnlyAtCreation}. Take
     * it in a starting slot instead.
     */
    public static final String FEAT_ONLY_AT_CREATION = "FEAT_ONLY_AT_CREATION";

    /**
     * The Título picked for {@code DestinoFeat#DESPERTAR_ANTECIPADO} is not one the holder may
     * take — see {@code DespertarAntecipadoFeat#optionsFor}.
     */
    public static final String DESPERTAR_ANTECIPADO_CHOICE_NOT_ELIGIBLE = "DESPERTAR_ANTECIPADO_CHOICE_NOT_ELIGIBLE";

    /**
     * A held Talento forbids that whole kind of Equipamento — {@code
     * Feat#getForbiddenEquipmentCategories}, as {@code DraconicoFeat#ASAS_DE_DRAGAO} does for a
     * Capa. A permanent restriction on the equipment list, distinct from a Forma's temporary
     * {@code FormEquipmentPolicy}.
     */
    public static final String EQUIPMENT_CATEGORY_FORBIDDEN = "EQUIPMENT_CATEGORY_FORBIDDEN";
    /**
     * The Habilidade de Força chosen for {@code AnaoFeat#CONSELHEIRO_DE_GUERRA_YMIRIANO} —
     * "1 Habilidade de Força (que você cumpra os requisitos)" — was picked by a character with
     * no Força ability slot (Força base below {@code
     * AttributeAbilityService#FIRST_ABILITY_ATTRIBUTE_BASE}).
     */
    public static final String CONSELHEIRO_STRENGTH_ABILITY_REQUIREMENT_NOT_MET = "CONSELHEIRO_STRENGTH_ABILITY_REQUIREMENT_NOT_MET";

    /**
     * A Talento granting a free Habilidade de Atributo of a <em>chosen</em> Atributo — {@code
     * DestinoFeat#PRODIGIO}/{@code #GENIALIDADE}/{@code #GENIALIDADE_DESPERTA}, via {@code
     * HabilidadeDeAtributoEscolhidaFeat} — was picked with an ability whose Atributo the holder
     * has no slot in (that Atributo's base below {@code
     * AttributeAbilityService#FIRST_ABILITY_ATTRIBUTE_BASE}). The chosen-Atributo twin of {@link
     * #CONSELHEIRO_STRENGTH_ABILITY_REQUIREMENT_NOT_MET}, which names Força outright.
     */
    public static final String CHOSEN_ATTRIBUTE_ABILITY_REQUIREMENT_NOT_MET = "CHOSEN_ATTRIBUTE_ABILITY_REQUIREMENT_NOT_MET";

    /**
     * The Talento picked for {@code DestinoFeat#EXCEPCIONALIDADE} is not one it offers the holder:
     * not a Talento Racial, already held, or its requirements other than Raça are unmet — see
     * {@code ExcepcionalidadeFeat#optionsFor}.
     */
    public static final String EXCEPCIONALIDADE_CHOICE_NOT_ELIGIBLE = "EXCEPCIONALIDADE_CHOICE_NOT_ELIGIBLE";
    public static final String NOT_AN_ATTACK_SKILL = "NOT_AN_ATTACK_SKILL";

    /**
     * An attack named more targets than the attacker's Talentos entitle them to — see {@code
     * AttackTargetingService#getMaximumTargets}.
     */
    public static final String TOO_MANY_ATTACK_TARGETS = "TOO_MANY_ATTACK_TARGETS";
    /** A provoking Aura binds the attacker, and its first attack this Rodada must target the Aura's holder. */
    public static final String FORCED_ATTACK_TARGET_REQUIRED = "FORCED_ATTACK_TARGET_REQUIRED";
    public static final String INVALID_AREA_OF_EFFECT = "INVALID_AREA_OF_EFFECT";
    public static final String INVALID_SPELL_TARGETING = "INVALID_SPELL_TARGETING";
    public static final String INVALID_SPELL_TREE = "INVALID_SPELL_TREE";
    public static final String INVALID_SPELL_DURATION = "INVALID_SPELL_DURATION";
    public static final String INVALID_SPELL_ACTIVATION = "INVALID_SPELL_ACTIVATION";
    public static final String SPELL_PREREQUISITE_NOT_MET = "SPELL_PREREQUISITE_NOT_MET";

    /**
     * A Magia would open an Árvore de Magia the Conjurador does not know yet, and every Árvore
     * their Talentos allow ({@code SpellService#getKnownTreeCapacity}) is already known.
     */
    public static final String SPELL_TREE_CAPACITY_REACHED = "SPELL_TREE_CAPACITY_REACHED";
    public static final String INVALID_SPELL_CAST_TARGET = "INVALID_SPELL_CAST_TARGET";
    public static final String INVALID_SPELL_DAMAGE = "INVALID_SPELL_DAMAGE";
    public static final String INVALID_SPELL_HEALING = "INVALID_SPELL_HEALING";
    public static final String INVALID_SPELL_ALTERNATE_EFFECT = "INVALID_SPELL_ALTERNATE_EFFECT";

    /**
     * An {@code Efeito Alternativo} was passed to {@code SpellService#grantSpell}. It is learned
     * free with its parent — "um personagem que aprenda a versão base automaticamente aprende sua
     * segunda versão" — so granting one would charge experience for a Magia already known.
     */
    public static final String SPELL_ALTERNATE_VERSION_NOT_GRANTABLE = "SPELL_ALTERNATE_VERSION_NOT_GRANTABLE";

    /** A cast asked for a Magia's Efeito Alternativo, and that Magia has none. */
    public static final String NO_ALTERNATE_SPELL_VERSION = "NO_ALTERNATE_SPELL_VERSION";
    public static final String INVALID_MIMETIZED_SPELL = "INVALID_MIMETIZED_SPELL";
    public static final String MIMETIZED_SPELL_NOT_HELD = "MIMETIZED_SPELL_NOT_HELD";

    /** The caster is under a Condição that forbids Conjurar Magias — Silêncio. */
    public static final String SPELL_CASTING_PREVENTED = "SPELL_CASTING_PREVENTED";

    /** The actor is under a Condição that forbids activating Habilidades — Silêncio. */
    public static final String ABILITY_ACTIVATION_PREVENTED = "ABILITY_ACTIVATION_PREVENTED";

    /**
     * The ability's Resfriamento has not elapsed — {@code
     * CombatantSheet#getRemainingCooldown(ActiveAbility)} is still above zero. Distinct from
     * {@link #ABILITY_ACTIVATION_PREVENTED}, which is a Condição forbidding activation outright.
     */
    public static final String ABILITY_ON_COOLDOWN = "ABILITY_ON_COOLDOWN";
    /** A Regular monster has used its two Efeitos de Ego this Cena — see {@code MonsterSheet#checkEgoEffectAvailable}. */
    public static final String MONSTER_EGO_EFFECTS_EXHAUSTED = "MONSTER_EGO_EFFECTS_EXHAUSTED";

    /** An activated ability's Pontos de Determinação cost exceeds what the holder has left. */
    public static final String NOT_ENOUGH_DETERMINATION_POINTS = "NOT_ENOUGH_DETERMINATION_POINTS";
    /** The PD offered for an activation isn't an amount its {@code PDCost} accepts. */
    public static final String INVALID_PD_AMOUNT = "INVALID_PD_AMOUNT";

    /**
     * The Forma an ability would put its holder into is one their own Talentos refuse — see
     * {@code CombatantSheet#canTakeForm} and {@code Feat#resolveFormAccess}.
     */
    public static final String FORM_NOT_AVAILABLE = "FORM_NOT_AVAILABLE";

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

    /** The combatant does not have enough Pontos de Equipamento (PE) for this purchase. */
    public static final String NOT_ENOUGH_EQUIPMENT_POINTS = "NOT_ENOUGH_EQUIPMENT_POINTS";

    /** This item store does not sell the requested Equipamento — above its Raridade ceiling, a
     * Regalia, or an Arma/Defesa Natural. */
    public static final String ITEM_NOT_OFFERED = "ITEM_NOT_OFFERED";

    /** An item store's Raridade ceiling must be a purchasable tier — {@code NATURAL} is not one. */
    public static final String STORE_RARITY_NOT_PURCHASABLE = "STORE_RARITY_NOT_PURCHASABLE";

    /** A Regalia is never stocked by an item store — it is forged for a specific Centelha, not sold. */
    public static final String STORE_DOES_NOT_SELL_REGALIA = "STORE_DOES_NOT_SELL_REGALIA";

    /**
     * Two Itens contend for the same single Equipamento slot — a character wears at most one
     * Armadura, one Elmo, one par de Botas, one Capa and one par de Manoplas.
     */
    public static final String EQUIPMENT_SLOT_ALREADY_OCCUPIED = "EQUIPMENT_SLOT_ALREADY_OCCUPIED";

    /** A character may wield at most one Escudo at a time. */
    public static final String TOO_MANY_SHIELDS = "TOO_MANY_SHIELDS";

    /**
     * The equipped Escudos and armas need more hands than the character has — two hands. A shield
     * or a one-handed weapon takes one; a Lâmina Pesada, a two-handed weapon, or any Arco/Besta
     * takes both.
     */
    public static final String NOT_ENOUGH_HANDS = "NOT_ENOUGH_HANDS";

    /**
     * A participant tried to progress (Talento, Graduação, Título, Especialização, Habilidade,
     * Atributo) while their Campanha has a Sessão in progress.
     */
    public static final String PROGRESSION_LOCKED_DURING_SESSION = "PROGRESSION_LOCKED_DURING_SESSION";

    /** A Sessão was asked to move to a status its lifecycle doesn't allow (CREATED → ONGOING → ENDED only). */
    public static final String INVALID_SESSION_TRANSITION = "INVALID_SESSION_TRANSITION";

    /** A Sessão cannot start while another Sessão of the same Campanha is ONGOING. */
    public static final String SESSION_ALREADY_ONGOING = "SESSION_ALREADY_ONGOING";

    /** A new Sessão cannot be created while another is still CREATED or ONGOING. */
    public static final String SESSION_ALREADY_OPEN = "SESSION_ALREADY_OPEN";

    /** No Sessão with the requested number exists in the Campanha. */
    public static final String SESSION_NOT_FOUND = "SESSION_NOT_FOUND";

    /** An activated ability's temporary Ego cost (e.g. "1 Ponto Temporário de Autocontrole") exceeds what the holder has. */
    public static final String NOT_ENOUGH_EGO_POINTS = "NOT_ENOUGH_EGO_POINTS";

    /** The actor is in a state that forbids this Perícia — Frenesi's "Perícias que exijam concentração ou raciocínio". */
    public static final String SKILL_USE_PREVENTED = "SKILL_USE_PREVENTED";

    /** The trait "só pode ser ativada durante o efeito de Frenesi", and its activator is not in Frenesi. */
    public static final String FRENZY_REQUIRED = "FRENZY_REQUIRED";

    /** Frenesi was started while one is already running. */
    public static final String FRENZY_ALREADY_ACTIVE = "FRENZY_ALREADY_ACTIVE";

    /** Frenesi "não pode ser interrompida voluntariamente" without Uno com a Ira. */
    public static final String FRENZY_CANNOT_END_VOLUNTARILY = "FRENZY_CANNOT_END_VOLUNTARILY";

    /** The trait needs Titã Enlouquecido active in the current Frenesi. */
    public static final String TITA_ENLOUQUECIDO_REQUIRED = "TITA_ENLOUQUECIDO_REQUIRED";

    /** A beneficial Magia from someone else failed to overcome a target treating every Magia as hostile. */
    public static final String SPELL_RESISTED_AS_HOSTILE = "SPELL_RESISTED_AS_HOSTILE";

    /** The trait's own trigger has not happened — Frenesi Assustador's "ao derrotar um inimigo … ou após desferir um Acerto Crítico". */
    public static final String TITLE_ABILITY_TRIGGER_NOT_MET = "TITLE_ABILITY_TRIGGER_NOT_MET";

    /** A Saquear named a combatant who is not among the looter's enemies. */
    public static final String LOOT_TARGET_NOT_AN_ENEMY = "LOOT_TARGET_NOT_AN_ENEMY";

    /** A Saquear named a foe whose PV are still above zero. */
    public static final String LOOT_TARGET_NOT_DEFEATED = "LOOT_TARGET_NOT_DEFEATED";
}
