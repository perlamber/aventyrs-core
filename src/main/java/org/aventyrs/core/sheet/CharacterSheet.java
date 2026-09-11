package org.aventyrs.core.sheet;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.item.Weapon;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.aventyrs.core.util.TranslatableMessages.EQUIPMENT_CATEGORY_FORBIDDEN;
import static org.aventyrs.core.util.TranslatableMessages.EQUIPMENT_SLOT_ALREADY_OCCUPIED;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_EXPERIENCE;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_HANDS;
import static org.aventyrs.core.util.TranslatableMessages.TOO_MANY_SHIELDS;

/**
 * A player character's sheet — a {@link CombatantSheet} plus the three things only a player has:
 * the {@link Player} behind it, an experience wallet, and Fama.
 *
 * <p>Everything a foe also does — damage, shields, Mana/Determinação, temporary Ego points,
 * Efeitos, the Turn lifecycle — lives on {@link AbstractCombatantSheet} and is shared verbatim
 * with {@code org.aventyrs.core.monster.MonsterSheet}. Only what's genuinely player-shaped is
 * here.
 *
 * <p><b>That's what keeps monsters out of the progression system.</b> Experience is spent from
 * this class, so the four services that spend it — {@code CharacterAttributeService#upgradeBase},
 * {@code SkillGraduationService#upgradeGraduation}, {@code FeatService#grantFeat}, {@code
 * TitleAbilityService#grantTitleAbility} — take a {@code CharacterSheet} rather than a {@code
 * CombatantSheet}, and a monster therefore cannot reach them at all. A monster's Attributes and
 * Graduações are deliberately uncapped (see CLAUDE.md's caps section — the builders never
 * validated anything), so the thing actually worth preventing was never "exceeding the cap", it
 * was "levelling up like a character". No {@code isMonster()} flag exists because none is needed:
 * it doesn't compile.
 */
@Getter
public class CharacterSheet extends AbstractCombatantSheet {

    @NonNull
    private final Player player;

    private BigDecimal totalExperience = BigDecimal.ZERO;

    private BigDecimal unUsedExperience = BigDecimal.ZERO;

    private int famaPositiva = 0;

    private int famaNegativa = 0;

    private CharacterSheet(final Character character, final Player player) {
        super(character);
        this.player = player;
    }

    public static CharacterSheet of(@NonNull final Character character, @NonNull final Player player) {
        return new CharacterSheet(character, player);
    }

    /**
     * Same as {@link #of(Character, Player)}, but with a known id instead of a freshly minted one
     * — for reconstructing a sheet from persisted state (e.g. a DTO) whose identity already
     * exists.
     */
    public static CharacterSheet of(final Character character, final Player player, @NonNull final UUID id) {
        CharacterSheet sheet = of(character, player);
        sheet.restoreId(id);
        return sheet;
    }

    /**
     * Consumes the available experience.
     *
     * <p>The subtraction happens only once the result is known to be non-negative — an earlier
     * version subtracted first and checked afterwards, so a rejected spend still silently
     * corrupted the balance.
     *
     * @param expToUse experience to be used
     * @return BigDecimal remaining experience
     * @throws IllegalOperationException in case unUsed experience is lower than consumed
     */
    public BigDecimal useExperience(BigDecimal expToUse) throws IllegalOperationException {
        BigDecimal remainingExperience = unUsedExperience.subtract(expToUse);
        if (remainingExperience.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalOperationException(NOT_ENOUGH_EXPERIENCE);
        }
        return unUsedExperience = remainingExperience;
    }

    public BigDecimal accumulateExperience(BigDecimal experience) {
        unUsedExperience = unUsedExperience.add(experience);
        return totalExperience = totalExperience.add(experience);
    }

    /**
     * Increases Fama Positiva — e.g. an Excelência bonus or a Narrador reward.
     * @return int total Fama Positiva after the increase
     */
    public int increaseFamaPositiva(int amount) {
        return famaPositiva += amount;
    }

    /**
     * Increases Fama Negativa — e.g. an Excelência bonus or a Narrador reward.
     * @return int total Fama Negativa after the increase
     */
    public int increaseFamaNegativa(int amount) {
        return famaNegativa += amount;
    }

    // --- Equipamento: quantos Itens de cada tipo um personagem pode usar de uma vez -----------

    /**
     * The {@link ItemCategory}s a character wears exactly one of — Armadura, Elmo, Botas, Capa,
     * Manoplas. An Escudo is <em>also</em> limited to one, but it is a hand-held item rather than a
     * body slot, so it is checked separately (with its own {@code TOO_MANY_SHIELDS} message) and
     * additionally spends a hand.
     */
    private static final Set<ItemCategory> SINGLE_BODY_SLOTS = Set.of(
            ItemCategory.ARMOR, ItemCategory.HELMET, ItemCategory.BOOTS,
            ItemCategory.CLOAK, ItemCategory.GLOVES);

    /** Hands a character has to hold Escudos and armas with. */
    private static final int AVAILABLE_HANDS = 2;

    /**
     * Puts item into this character's equipped {@link Character#getEquipment()} — but only if the
     * resulting loadout is legal ({@link #validateEquipmentLoadout()}'s rules). The validated
     * counterpart of the plain {@code Character#equip(Item)} mutator, the same check-then-mutate
     * split {@code WeaponDrawService#draw} keeps over {@code Character#drawWeapon}.
     *
     * <p>Only on {@link CharacterSheet}, not {@code AbstractCombatantSheet}: a foe's loadout is
     * authored wholesale and never assembled a piece at a time, the same reason the XP-spending
     * services take a {@code CharacterSheet}. {@code CombatantSheet#rearm} still reaches {@code
     * Character#equip} directly — picking a weapon back up after a disarm cannot break a loadout
     * that was already legal.
     *
     * @throws IllegalOperationException leaving the character untouched, if adding item would
     *         break a slot limit or the two-hand budget
     */
    public void equip(@NonNull final Item item) {
        String violation = findLoadoutViolation(withAdded(getCharacter().getEquipment(), item));
        if (violation != null) {
            throw new IllegalOperationException(violation);
        }
        getCharacter().equip(item);
    }

    /**
     * Whether {@link #equip(Item)} would accept item right now — the non-throwing form, for a
     * caller deciding what to offer rather than performing it.
     */
    public boolean canEquip(@NonNull final Item item) {
        return findLoadoutViolation(withAdded(getCharacter().getEquipment(), item)) == null;
    }

    /**
     * Checks the character's <em>current</em> {@link Character#getEquipment()} against the
     * equip-slot rules, throwing {@link IllegalOperationException} on the first breach:
     *
     * <ul>
     *   <li>at most one Armadura, Elmo, Botas, Capa and Manoplas each
     *       ({@code EQUIPMENT_SLOT_ALREADY_OCCUPIED});</li>
     *   <li>at most one Escudo ({@code TOO_MANY_SHIELDS});</li>
     *   <li>no item of a category a held Talento forbids outright
     *       ({@code EQUIPMENT_CATEGORY_FORBIDDEN}) — {@code DraconicoFeat#ASAS_DE_DRAGAO}'s Capa;</li>
     *   <li>the equipped Escudos and armas fit in two hands ({@code NOT_ENOUGH_HANDS}) — an
     *       Escudo or a one-handed weapon takes one hand, a two-handed weapon (a Lâmina Pesada or
     *       any non-Leve weapon) and every Arco/Besta take both. So a shield pairs with one light
     *       weapon or a second light weapon, but never with a heavy weapon or a bow.</li>
     * </ul>
     *
     * <p>The builder, Fixture Factory templates and {@code Character#equip} all bypass this by
     * design (the usual "builder-bypassable invariants" restraint); this is the method to call
     * after assembling a loadout that way.
     *
     * <p><b>Handedness is inferred, not authored</b> — {@code docs/rules/equipamentos.txt} gives
     * no weapon a hands column, so a weapon counts as two-handed when its {@link
     * ItemWeightClass} is {@code MEDIUM}/{@code HEAVY} or its category is {@link ItemCategory#BOW}/
     * {@link ItemCategory#CROSSBOW} (a Besta de Mão is Leve yet still needs both hands). Weapons
     * that occupy a different body slot ("ocupa o espaço de um item do tipo Bota") and a
     * Força-conditioned two-handed grip are not modeled.
     */
    public void validateEquipmentLoadout() {
        String violation = findLoadoutViolation(getCharacter().getEquipment());
        if (violation != null) {
            throw new IllegalOperationException(violation);
        }
    }

    private static List<Item> withAdded(final List<Item> current, final Item item) {
        List<Item> candidate = new ArrayList<>(current);
        candidate.add(Objects.requireNonNull(item));
        return candidate;
    }

    /**
     * The offending message key, or {@code null} when every item fits. An instance method rather
     * than a static one because the last rule is the holder's own: a Talento may forbid a whole
     * {@link ItemCategory} outright ({@code Feat#getForbiddenEquipmentCategories}).
     */
    private String findLoadoutViolation(final List<Item> items) {
        Set<ItemCategory> forbidden = forbiddenCategories();
        if (items.stream().anyMatch(item -> forbidden.contains(item.getCategory()))) {
            return EQUIPMENT_CATEGORY_FORBIDDEN;
        }
        for (ItemCategory slot : SINGLE_BODY_SLOTS) {
            if (countOfCategory(items, slot) > 1) {
                return EQUIPMENT_SLOT_ALREADY_OCCUPIED;
            }
        }
        if (countOfCategory(items, ItemCategory.SHIELD) > 1) {
            return TOO_MANY_SHIELDS;
        }
        if (items.stream().mapToInt(CharacterSheet::handCost).sum() > AVAILABLE_HANDS) {
            return NOT_ENOUGH_HANDS;
        }
        return null;
    }

    /** Every category this character's held Talentos refuse outright — usually none. */
    private Set<ItemCategory> forbiddenCategories() {
        return getCharacter().getFeats().stream()
                .flatMap(feat -> feat.getForbiddenEquipmentCategories(getCharacter()).stream())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static long countOfCategory(final List<Item> items, final ItemCategory category) {
        return items.stream().filter(item -> item.getCategory() == category).count();
    }

    /** How many hands item takes up while equipped — 0 for anything not held to fight with. */
    private static int handCost(final Item item) {
        if (item.getCategory() == ItemCategory.SHIELD) {
            return 1;
        }
        if (item.getCategory() == ItemCategory.PROJECTILE || !(item instanceof Weapon weapon)) {
            return 0;
        }
        return isTwoHanded(weapon) ? 2 : 1;
    }

    private static boolean isTwoHanded(final Weapon weapon) {
        return weapon.getCategory() == ItemCategory.BOW
                || weapon.getCategory() == ItemCategory.CROSSBOW
                || weapon.getWeightClass() == ItemWeightClass.MEDIUM
                || weapon.getWeightClass() == ItemWeightClass.HEAVY;
    }
}
