package org.aventyrs.core.item;

import java.util.List;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.modifier.ModifierType;

/**
 * A piece of Equipamento — the catalog entry, carrying every column an item's own rules-text
 * block lists: its heading ({@link #getWeightClass()}/{@link #getRarity()}), Descrição, Favor
 * (plus the Requisitos gating it, and any Efeitos Adicionais — all three on {@link
 * ItemFavor}), Preço, DF, DM, Dureza and Conjuração.
 *
 * <p><b>Catalog, not owned copy.</b> An implementation describes what "an Armadura Completa"
 * *is*, the same way {@code org.aventyrs.core.feat.Feat} describes a Talento — not one
 * character's own dented, upgraded copy of it. Per-copy state (Dureza actually remaining after
 * damage, Obra-Prima tier, Aprimoramentos — all cited by {@code
 * org.aventyrs.core.ego.ResourcesAdvantage#BARGANHISTA}/{@code #HERANCA_FAMILIAR} and {@code
 * org.aventyrs.core.skill.profissao.ProfissaoCompetencyAbility}) is deliberately not modeled
 * yet, and would be a separate held-instance type wrapping a catalog entry — the same
 * catalog-versus-instance split {@code org.aventyrs.core.title.AventyrTitle}'s own javadoc
 * documents. Nothing on {@code Character}/{@code CharacterSheet} holds an {@code Item} yet
 * either; that inventory belongs with the per-copy type, not this one.
 *
 * <p><b>None of the numeric columns has a consumer yet</b>, and each is blocked on its own
 * separate missing system, not one shared gap:
 * <ul>
 *   <li>{@link #getPrice()} is in Pontos de Equipamento (PE). No PE budget/economy exists —
 *   the gap {@code ResourcesAdvantage#BARGANHISTA}'s own "-2PE, mínimo 1PE" already cites.</li>
 *   <li>{@link #getPhysicalDefenseBonus()}/{@link #getMagicDefenseBonus()} (DF/DM) have no
 *   stat to add to: Defesas isn't a concept this core computes at all (see {@code
 *   org.aventyrs.core.race.Gigantes}/{@code org.aventyrs.core.race.Elfo}'s own citations, and
 *   {@code org.aventyrs.core.modifier.ModifierType#DEFESAS}, a real registry entry nothing
 *   reads yet).</li>
 *   <li>{@link #getHardness()} (Dureza) has no damage-to-equipment or repair mechanic —
 *   exactly what {@code ProfissaoCompetencyAbility#REPARO_MELHORADO}/{@code
 *   #AUMENTAR_A_DUREZA} are still TODO'd on.</li>
 *   <li>{@link #getCastingBonus()} (Conjuração) has no conjuração-roll hook: casting is two
 *   rolls (see {@code org.aventyrs.core.magic.SpellCastingService}), but neither takes an
 *   item-granted modifier, and no {@code Magia} entity exists to carry one against.</li>
 * </ul>
 * The values themselves are real, exact data all the same — the same "can't apply it yet
 * doesn't mean can't compute it yet" discipline this codebase applies to {@code Santo
 * #getDefesasBonus} and every {@code ModifierType#DEFESAS}-typed {@code Blessing}.
 *
 * <p>The {@link ItemFavor} is the exception that *does* land on already-real machinery: its
 * bonuses are {@link ModifierType}-typed {@link ItemBonus}es, so a Favor granting RD (e.g.
 * {@link ArmorItem#ARMADURA_COMPLETA}) names the same {@code DAMAGE_REDUCTION} that {@code
 * DamageService#getTotalDamageReduction} already sums. What's missing is only the *inventory*
 * — no {@code Character}/{@code CharacterSheet} holds an item, so no scanning service reaches
 * one on its own yet; a caller with the item in hand calls {@link
 * #resolveFavorBonus(ModifierType, Character)} directly.
 */
public interface Item {

    /** This item's own name, e.g. "Armadura Completa". */
    String getName();

    /** The item's Descrição — flavour/usage text, never mechanics (those are the columns). */
    String getDescription();

    ItemCategory getCategory();

    ItemRarity getRarity();

    ItemWeightClass getWeightClass();

    /** Preço, in Pontos de Equipamento (PE). */
    int getPrice();

    /** Bônus na DF (Defesa Física). */
    int getPhysicalDefenseBonus();

    /** Bônus na DM (Defesa Mágica). */
    int getMagicDefenseBonus();

    /** Dureza — how much punishment the item itself absorbs before breaking. */
    int getHardness();

    /**
     * Bônus na Conjuração. A plain number, so an item whose Conjuração column reads
     * "Desvantagem" (e.g. {@link ArmorItem#ARMADURA_COMPLETA}) carries {@code
     * org.aventyrs.core.skill.Skill#DISADVANTAGE_MALUS} — Desvantagem is a flat -2, the exact
     * mirror of this codebase's "Vantagem is a flat +2" convention — and one reading
     * "Vantagem" would carry {@code Skill#ADVANTAGE_BONUS}. 0 means the item neither helps nor
     * hinders conjuração.
     */
    int getCastingBonus();

    /**
     * This item's Favor — its real, {@link ModifierType}-typed bonuses plus the Requisitos
     * gating them — or {@code null} for an item that grants none. Prefer {@link
     * #resolveFavorBonus(ModifierType, Character)}/{@link #grantsFavorTo(Character)} over
     * null-checking this at a call site that only cares what the Favor currently grants.
     */
    ItemFavor getFavor();

    /** Whether this item is Ofensivo/Defensivo/Utilitário/Consumível, per its category. */
    default ItemType getType() {
        return getCategory().getType();
    }

    /**
     * Whether character currently meets this item's Requisitos, so its Favor (and any Efeitos
     * Adicionais) apply to them. Always {@code false} for an item with no Favor at all — there
     * is nothing to grant.
     *
     * <p>This checks *possession of the requirement*, not possession of the item: this core
     * has no inventory (see this interface's own javadoc), so a caller answers "is it actually
     * equipped" itself.
     */
    default boolean grantsFavorTo(final Character character) {
        return getFavor() != null && getFavor().isGrantedTo(character);
    }

    /**
     * How much of modifierType this item's Favor currently grants character — 0 for an item
     * with no Favor, one whose Requisitos character doesn't meet, or one that grants nothing
     * of that type. The entry point a scanning service would call once an inventory exists;
     * until then, a caller holding the item asks directly.
     */
    default int resolveFavorBonus(final ModifierType modifierType, final Character character) {
        return getFavor() == null ? 0 : getFavor().resolveBonus(modifierType, character);
    }

    /**
     * Every bonus this item's Favor grants character right now — empty when it grants none, or
     * when its Requisitos aren't met.
     */
    default List<ItemBonus> resolveFavorBonuses(final Character character) {
        return getFavor() == null ? List.of() : getFavor().resolveBonuses(character);
    }
}
