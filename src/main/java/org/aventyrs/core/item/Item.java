package org.aventyrs.core.item;

import java.util.List;

import org.aventyrs.core.ability.ItemActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.modifier.ModifierType;

/**
 * A unique piece of Equipamento in a character's possession — the genuine owned item, not the
 * static catalog entry. This is the runtime shape for an actual forged, worn, or carried object,
 * with its own state and upgrades. The catalog blueprint lives in {@link ItemTemplate}.
 *
 * <p>The old model conflated these two concepts: a catalog entry such as {@link ArmorItem}
 * also implemented this interface. This redesign keeps the compatibility bridge in place by
 * making template-backed enums implement {@link ItemTemplate}; the unique instance contract stays
 * here. Per-copy state is where damage, Obra-Prima and Aprimoramento already live — see {@link
 * #applyDamage(int)} and {@link #isDestroyed()} for the first of those. <b>Who forged it</b> is
 * still not modeled (cited by {@code org.aventyrs.core.ego.ResourcesAdvantage#BARGANHISTA}/{@code
 * #HERANCA_FAMILIAR} and {@code org.aventyrs.core.skill.profissao.ProfissaoCompetencyAbility}),
 * and belongs on the held instance rather than in the static catalog. {@link Masterpiece} is now modeled through
 * {@link DefensiveMasterpiece}/{@link ItemMasterpiece}; the item copy holds one, including any
 * creation-time choice it needs.
 *
 * <p><b>Most of the numeric columns still have no consumer</b>, each blocked on its own separate
 * missing system rather than one shared gap — DF/DM being the exception that now has one:
 * <ul>
 *   <li>{@link #getPrice()} is in Pontos de Equipamento (PE). No PE budget/economy exists —
 *   the gap {@code ResourcesAdvantage#BARGANHISTA}'s own "-2PE, mínimo 1PE" already cites.</li>
 *   <li><b>{@link #getPhysicalDefenseBonus()}/{@link #getMagicDefenseBonus()} (DF/DM) are no
 *   longer inert</b> — {@code org.aventyrs.core.character.services.DefenseService} sums them for
 *   every equipped item, and {@code
 *   org.aventyrs.core.skill.esquivaeaparar.EsquivaEApararInteraction} feeds that into the
 *   defender's roll. See {@code org.aventyrs.core.character.DefenseType} for why a Defesa is a
 *   pool of roll bonuses rather than a passive stat, and {@code
 *   org.aventyrs.core.combat.AttackReceiver} for the entry point that drives it.</li>
 * <li><b>{@link #getHardness()} (Dureza) is no longer inert</b> — it is this copy's maximum PV,
 *   spent by {@link #applyDamage(int)} and read back through {@link #getCurrentHardness()}/{@link
 *   #isDestroyed()}. What's still missing is <i>repair</i> and <i>production</i>, which is what
 *   {@code ProfissaoCompetencyAbility#REPARO_MELHORADO}/{@code #AUMENTAR_A_DUREZA} are now
 *   TODO'd on.</li>
 *   <li>{@link #getCastingBonus()} (Conjuração) has no conjuração-roll hook: casting is two
 *   rolls (see {@code org.aventyrs.core.magic.SpellCastingService}), but neither takes an
 *   item-granted modifier, and no {@code Magia} entity exists to carry one against.</li>
 * </ul>
 * The values themselves are real, exact data all the same — the same "can't apply it yet
 * doesn't mean can't compute it yet" discipline this codebase applies to {@code Santo
 * #getDefesasBonus}. DF/DM are what that discipline eventually pays off: they were exact data
 * with nothing to read them for several revisions before {@code DefenseService} existed.
 *
 * <p>The {@link ItemFavor} is the exception that *does* land on already-real machinery: its
 * bonuses are {@link ModifierType}-typed {@link ItemBonus}es, so a Favor granting RD (e.g.
 * {@link ArmorItem#ARMADURA_COMPLETA}) names the same {@code DAMAGE_REDUCTION} that {@code
 * DamageService#getTotalDamageReduction} already sums — and now genuinely does, scanning {@code
 * Character#getEquipment()} as its fourth source. A caller holding an item that isn't equipped
 * can still ask it directly via {@link #resolveFavorBonus(ModifierType, Character)}.
 *
 * <p><b>Dano Base is not here</b>, deliberately — it lives on {@link Weapon}, the sub-interface
 * for items that can actually be swung, so that nothing can ask a helmet what it hits for. See
 * that interface's own javadoc.
 *
 * <p>A copy can also carry a <b>Pedra do Poder</b> ({@link #getPowerStone()}), socketed once the
 * Encaixe Aprimoramento is fitted. Its tri-modal buff — an always-on Efeito Base plus one of an
 * Efeito Defensivo/Ofensivo chosen from {@link #getType()} — folds into the same enhancement
 * aggregation the Masterpiece and Improvement use ({@link #resolvePowerStoneBonus},
 * {@link #resolveEnhancementBonus}, {@link #resolveEnhancementDamageBaseIncrease}). Its
 * charge/Resfriamento/Vinculação economy is authored data with no consumer yet, like Preço.
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
     * The effective weight category after the fitted Masterpiece and Improvement adjustments.
     * Signed adjustments stack and are bounded to the light-to-heavy authored range.
     */
    default ItemWeightClass getEffectiveWeightClass() {
        int masterpieceBonus = getMasterpiece() == null ? 0 : getMasterpiece().getWeightClassBonus();
        int improvementBonus = getImprovement() == null ? 0 : getImprovement().getWeightClassBonus();
        return getWeightClass().adjustedBy(masterpieceBonus + improvementBonus);
    }

    /**
     * This item's <em>maximum</em> PV — its authored Dureza, adjusted by its Masterpiece and
     * Improvement, never below zero. Deliberately unaffected by damage already taken (that's
     * {@link #getCurrentHardness()}) and by destruction: it is the ceiling destruction is judged
     * against, so gating it on {@link #isDestroyed()} would make the state uncomputable.
     */
    default int getEffectiveHardness() {
        int masterpieceBonus = getMasterpiece() == null ? 0 : getMasterpiece().getHardnessBonus();
        int improvementBonus = getImprovement() == null ? 0 : getImprovement().getHardnessBonus();
        return Math.max(0, getHardness() + masterpieceBonus + improvementBonus);
    }

    /**
     * Damage this copy has actually taken, via {@link #applyDamage(int)}. Always 0 for an {@link
     * ItemTemplate}: a catalog entry is shared by every copy of that Equipamento, so it is never
     * damaged — per-copy state lives on the forged {@link AbstractItem} instance alone.
     */
    default int getDamageTaken() {
        return this instanceof AbstractItem item ? item.getDamageTaken() : 0;
    }

    /** The PV this copy has left — {@link #getEffectiveHardness()} less its damage, floored at 0. */
    default int getCurrentHardness() {
        return Math.max(0, getEffectiveHardness() - getDamageTaken());
    }

    /**
     * Whether this copy has been reduced to 0 PV. <b>A destroyed item grants nothing</b> — every
     * Defesa column, Favor, Aprimoramento/Obra-Prima bonus and Dano Base contribution below reads
     * as absent, so no consuming service needs a check of its own.
     *
     * <p>It is still a real item: destruction removes its <em>effects</em>, not the object, which
     * stays in {@code Character#getEquipment()}/{@code CombatantSheet#getInventory()} as garbage
     * until its owner discards it. Three things are deliberately <em>not</em> gated on this:
     * {@link #getEffectiveHardness()} (the ceiling this state is derived from), {@link
     * #getEffectiveWeightClass()} (a ruined breastplate is exactly as heavy to wear — the Destreza
     * penalty {@code EsquivaEApararInteraction} reads is a burden, not a benefit), and {@link
     * #getName()}/the other identity columns, which is what lets a player find the wreck to drop.
     *
     * <p>An item with no damage at all is never destroyed, including one whose Dureza is 0 —
     * that reads as "nothing has happened to it yet", not as pre-broken.
     */
    default boolean isDestroyed() {
        return getDamageTaken() > 0 && getCurrentHardness() == 0;
    }

    /**
     * How much of the damage aimed at <em>this item</em> its own fitted Obra-Prima and
     * Aprimoramento shrug off (e.g. {@link DefensiveImprovement#RESISTENTE}'s "danos causados a
     * este equipamento são reduzidos em -1").
     *
     * <p><b>An item is mitigated by its own enhancements only</b> — never by its wielder's RD/RA,
     * which is a separate pool belonging to a different victim. That's why this is resolved here
     * rather than through {@code org.aventyrs.core.character.services.DamageService}: no
     * three-source scan is involved, so nothing about it needs a {@code ModifierResolver} or a
     * {@code Character} at all, the same reasoning that keeps a permanent Ego maximum on the
     * sheet rather than in a service.
     */
    default int getItemDamageReduction() {
        int masterpieceReduction = getMasterpiece() == null ? 0 : getMasterpiece().getItemDamageReduction();
        int improvementReduction = getImprovement() == null ? 0 : getImprovement().getItemDamageReduction();
        return Math.max(0, masterpieceReduction + improvementReduction);
    }

    /**
     * Deals rawDamage to this item itself, returning how much actually landed after {@link
     * #getItemDamageReduction()}. Damage accumulates past the item's Dureza rather than clamping,
     * so a fitted enhancement later raising {@link #getEffectiveHardness()} can't quietly undestroy
     * a wreck.
     *
     * <p>An {@link ItemTemplate} takes no damage and returns 0 — damaging a shared catalog entry
     * would break every copy of that Equipamento at once. Same silent no-op an {@link
     * ItemTemplate} already gives {@link #activateImprovementEffect}.
     *
     * <p>There is no repair counterpart yet: {@code ProfissaoCompetencyAbility#REPARO_MELHORADO}
     * and the Artesão tree both need a production/repair pipeline this core still lacks, and
     * {@code CriticalEffectType#FORTALECER}'s "recupera todos os PV perdidos nesta Cena"
     * additionally needs Scene-scoped damage tracking.
     */
    default int applyDamage(final int rawDamage) {
        if (!(this instanceof AbstractItem item)) {
            return 0;
        }
        int finalDamage = Math.max(0, rawDamage - getItemDamageReduction());
        item.setDamageTaken(item.getDamageTaken() + finalDamage);
        return finalDamage;
    }

    /** This item's complete current DF or DM contribution, including fitted enhancements and Favor. */
    default int getEffectiveDefenseBonus(final org.aventyrs.core.character.DefenseType defenseType,
                                         final Character character,
                                         final org.aventyrs.core.scene.SceneContext sceneContext) {
        return getEffectiveDefenseBonus(defenseType, character, sceneContext, null);
    }

    /**
     * This item's complete current DF or DM contribution against the supplied incoming damage.
     * The caller still chooses the explicit Defesa pool used by the attack.
     */
    default int getEffectiveDefenseBonus(final org.aventyrs.core.character.DefenseType defenseType,
                                         final Character character,
                                         final org.aventyrs.core.scene.SceneContext sceneContext,
                                         final DamageDescriptor damageDescriptor) {
        if (isDestroyed()) {
            return 0;
        }
        int masterpieceBonus = getMasterpiece() == null
                ? 0 : getMasterpiece().getEffectiveDefenseBonus(defenseType, character);
        int improvementBonus = getImprovement() == null
                ? 0 : getImprovement().getEffectiveDefenseBonus(
                        defenseType, character, sceneContext, this, damageDescriptor);
        return defenseType.columnOf(this) + masterpieceBonus + improvementBonus
                + resolvePowerStoneBonus(ModifierType.DEFESAS)
                + resolvePowerStoneBonus(defenseType.getModifierType())
                + resolveFavorBonus(ModifierType.DEFESAS, character)
                + resolveFavorBonus(defenseType.getModifierType(), character);
    }

    /**
     * How much of modifierType this item's socketed Pedra do Poder currently grants — 0 for an
     * item with none, or a destroyed one. The stone selects its Efeito Defensivo or Efeito
     * Ofensivo from {@link #getType()}; its Efeito Base always applies.
     */
    default int resolvePowerStoneBonus(final ModifierType modifierType) {
        return getPowerStone() == null || isDestroyed() ? 0
                : getPowerStone().resolveBonus(modifierType, getType());
    }

    /** Every non-Favor numeric bonus granted by this item's fitted enhancements. */
    default int resolveEnhancementBonus(final ModifierType modifierType,
                                        final org.aventyrs.core.skill.SkillType skillType,
                                        final Character character) {
        if (isDestroyed()) {
            return 0;
        }
        int masterpieceBonus = getMasterpiece() == null ? 0
                : getMasterpiece().resolveBonus(modifierType, skillType, character);
        int improvementBonus = getImprovement() == null ? 0
                : getImprovement().resolveBonus(modifierType, skillType, character);
        return masterpieceBonus + improvementBonus + resolvePowerStoneBonus(modifierType);
    }

    /**
     * Extra Movimento Base, in UD, this item grants on one specific movement of the Rodada —
     * the per-movement counterpart of the {@code ModifierType#MOVEMENT} bonus {@link
     * #resolveEnhancementBonus} already carries, for an Equipamento whose rules text scopes its
     * bonus to <i>which</i> movement of the Rodada it is. movementIndex is 0-based.
     *
     * <p>Zero by default and, like every other bonus-granting default here, gated on {@link
     * #isDestroyed()} in this one place so no consuming service needs a check of its own. No
     * catalogued Equipamento names such a clause yet — the hook exists so {@code
     * MovementServiceImpl} resolves all four of its permanent-{@code MOVEMENT} sources on the
     * per-movement axis too, rather than silently covering three of them.
     */
    default int resolveRoundMovementIncrease(final int movementIndex, final Character character) {
        return 0;
    }

    /** Dano Base scale-ups this item grants when weapon is the attack source. */
    default int resolveEnhancementDamageBaseIncrease(final Weapon weapon, final Character character) {
        if (isDestroyed()) {
            return 0;
        }
        int masterpieceBonus = getMasterpiece() == null ? 0
                : getMasterpiece().resolveDamageBaseIncrease(weapon, character);
        int improvementBonus = getImprovement() == null ? 0
                : getImprovement().resolveDamageBaseIncrease(weapon, character);
        int powerStoneBonus = getPowerStone() == null ? 0
                : getPowerStone().resolveDamageBaseIncrease(weapon, getType());
        return masterpieceBonus + improvementBonus + powerStoneBonus;
    }

    /** This item's fitted enhancement reduction for one fully-classified incoming damage instance. */
    default int resolveEnhancementDamageReduction(final DamageDescriptor damageDescriptor,
                                                  final Character character) {
        return getImprovement() == null || isDestroyed() ? 0
                : getImprovement().resolveDamageReduction(damageDescriptor, character);
    }

    /**
     * The Rodadas this item's fitted Aprimoramento adds to spell's resolved Duração — the entry
     * point {@code org.aventyrs.core.magic.SpellDurationService} scans equipment through, so a
     * destroyed item stops extending a Duração without that service knowing about destruction.
     */
    default int resolveEnhancementDurationIncreaseInRounds(final Spell spell, final Character character) {
        return getImprovement() == null || isDestroyed() ? 0
                : getImprovement().resolveDurationIncreaseInRounds(spell, character);
    }

    /**
     * Notifies this item's fitted Improvement after its wearer takes final damage. Catalog
     * templates and items without an Improvement deliberately do nothing.
     */
    default void notifyFinalDamageTaken(final int finalDamage,
                                        final org.aventyrs.core.scene.SceneContext sceneContext) {
        if (getImprovement() != null && !isDestroyed()) {
            getImprovement().onFinalDamageTaken(this, finalDamage, sceneContext);
        }
    }

    /** Opens this item's current improvement-effect window through the stated Scene Round. */
    default void activateImprovementEffect(final org.aventyrs.core.scene.SceneContext sceneContext,
                                           final int lastActiveRound) {
        if (this instanceof AbstractItem item && sceneContext != null) {
            item.activateImprovementEffect(sceneContext, lastActiveRound);
        }
    }

    /** Whether this item's current improvement-effect window is active in sceneContext. */
    default boolean hasActiveImprovementEffect(final org.aventyrs.core.scene.SceneContext sceneContext) {
        return !isDestroyed() && this instanceof AbstractItem item
                && item.hasActiveImprovementEffect(sceneContext);
    }

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
        return getFavor() != null && !isDestroyed() && getFavor().isGrantedTo(character);
    }

    /**
     * How much of modifierType this item's Favor currently grants character — 0 for an item
     * with no Favor, one whose Requisitos character doesn't meet, or one that grants nothing
     * of that type. The entry point a scanning service would call once an inventory exists;
     * until then, a caller holding the item asks directly.
     */
    default int resolveFavorBonus(final ModifierType modifierType, final Character character) {
        return getFavor() == null || isDestroyed() ? 0 : getFavor().resolveBonus(modifierType, character);
    }

    /**
     * Every bonus this item's Favor grants character right now — empty when it grants none, or
     * when its Requisitos aren't met.
     */
    default List<ItemBonus> resolveFavorBonuses(final Character character) {
        return getFavor() == null || isDestroyed() ? List.of() : getFavor().resolveBonuses(character);
    }

    /** A unique item may carry a single masterpiece of craft; most items do not. */
    default Masterpiece getMasterpiece() {
        return this instanceof AbstractItem item ? item.getMasterpiece() : null;
    }

    /** A unique item may carry a single improvement; most items do not. */
    default Improvement getImprovement() {
        return this instanceof AbstractItem item ? item.getImprovement() : null;
    }

    /**
     * The Pedra do Poder socketed into this item, or {@code null} while it has none. Requires the
     * Encaixe Aprimoramento to fit (see {@link AbstractItem#setPowerStone}); a catalog {@link
     * ItemTemplate} never has one.
     */
    default PowerStone getPowerStone() {
        return this instanceof AbstractItem item ? item.getPowerStone() : null;
    }

    /**
     * The active ability carried by this Regalia, or {@code null} while it has none. Non-Regalias
     * always return {@code null}.
     */
    ItemActiveAbility getActiveAbility();

    /** Whether this owned item is a Regalia. */
    boolean isRegalia();
}
