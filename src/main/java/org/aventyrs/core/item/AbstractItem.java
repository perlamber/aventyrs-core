package org.aventyrs.core.item;

import org.aventyrs.core.effect.DefensiveCriticalEffectType;
import org.aventyrs.core.ability.ItemActiveAbility;
import org.aventyrs.core.skill.SkillType;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * A plain builder-built {@link Item}, for a one-off or caller-supplied piece of Equipamento
 * that doesn't belong in one of this package's own catalog enums (e.g. {@link ArmorItem}) —
 * mirroring {@code org.aventyrs.core.feat.AbstractFeat}'s identical role alongside {@code
 * org.aventyrs.core.feat.ArtesMarciaisFeat}.
 *
 * <p>An item built through {@code builder()} directly has no Dano Base and cannot be swung — see
 * {@link AbstractWeapon} for the {@link Weapon} counterpart, which extends this class and adds that
 * one column. {@link #builderFromTemplate} is the exception, and builds one of those whenever the
 * template it is handed is itself a {@code Weapon}.
 *
 * <p>Nothing here validates the values it's handed: like every other builder in this codebase
 * (see CLAUDE.md's note on {@code AttributeValue.builder()}), it's a data holder, not a
 * gatekeeper.
 */
@Getter
@SuperBuilder
public class AbstractItem implements Item {

    private ItemTemplate template;
    private String name;
    private String description;
    private ItemCategory category;
    private ItemRarity rarity;
    private ItemWeightClass weightClass;
    private int price;
    private int physicalDefenseBonus;
    private int magicDefenseBonus;
    private int hardness;
    private int damageTaken;
    private int castingBonus;
    private ItemFavor favor;
    private Masterpiece masterpiece;
    @Builder.Default
    private List<Improvement> improvements = new ArrayList<>();
    private PowerStone powerStone;
    private RegaliaGrade regaliaGrade;
    private ItemActiveAbility activeAbility;

    /**
     * The {@code Character#getId()} of whoever forged this copy, or {@code null} for one whose
     * maker is unknown (a template-forged bridge copy, a GM drop, loot). Stamped by {@code
     * org.aventyrs.core.character.services.EquipmentCraftingService#forge}; the "quem produziu"
     * half of {@code ProfissaoCompetencyAbility#FORJA_VULCANA} and {@code
     * ResourcesAdvantage#BARGANHISTA} reads it.
     */
    private UUID producedByCharacterId;

    /**
     * Whether this copy was handed over by Aventyr itself rather than made by anyone — the GM
     * path, {@link ItemForgery#donatedByAventyr(ItemSpecification)}. Distinct from a {@code null}
     * {@link #producedByCharacterId}, which only says the maker is unknown (loot, a bridge copy):
     * this says there was no making and no cost, and it is what tells a Regalia a party was given
     * apart from one a crafter earned.
     */
    private boolean donatedByAventyr;

    private UUID improvementEffectSceneId;
    private int improvementEffectLastActiveRound;

    /**
     * How many units this copy is — 1 for ordinary Equipamento, a lot of {@link ItemTemplate#getLotSize()} for a
     * {@link ProjectileItem}. Only a {@linkplain #isStackable() stackable} copy ever moves it, through {@link
     * #addUnits(int)} and {@link #takeUnits(int)}.
     */
    @Builder.Default
    private int quantity = 1;

    /**
     * A builder pre-filled with template's catalog columns — the shared half of {@link
     * #fromTemplate} and {@code
     * org.aventyrs.core.character.services.EquipmentCraftingService#forge} (which overrides
     * {@code hardness} and sets {@code producedByCharacterId} before building). Per-copy state
     * (damage, Obra-Prima, Aprimoramentos, Pedra do Poder) is deliberately not carried over.
     */
    public static AbstractItemBuilder<?, ?> builderFromTemplate(final ItemTemplate template) {
        return baseBuilderFor(template)
                .template(template)
                .name(template.getName())
                .description(template.getDescription())
                .category(template.getCategory())
                .rarity(template.getRarity())
                .weightClass(template.getWeightClass())
                .price(template.getPrice())
                .physicalDefenseBonus(template.getPhysicalDefenseBonus())
                .magicDefenseBonus(template.getMagicDefenseBonus())
                .hardness(template.getHardness())
                .castingBonus(template.getCastingBonus())
                .favor(template.getFavor())
                .quantity(template.getLotSize());
    }

    /**
     * The right builder for template's own kind — an {@link AbstractWeapon} one when the template
     * <em>is</em> a {@link Weapon}, and a plain {@link AbstractItem} one otherwise.
     *
     * <p><b>Why this exists.</b> A forged copy of a catalog Arma has to keep being an Arma. Built
     * through {@code AbstractItem.builder()} it would come back as a bare {@code AbstractItem}:
     * {@code instanceof Weapon} false, so nothing could draw it ({@code Character#drawWeapon} takes
     * a {@code Weapon}), no Perícia de Ataque could name it as its {@code AttackSource}, and {@code
     * DamageBaseService} could not be handed it at all — a bought sword that could not be swung.
     * The three weapon columns are copied off the template here for the same reason every other
     * column is: the copy is the template's copy.
     *
     * <p>Its Efeito Crítico and Margem Crítica Menor columns are carried too: both are read now
     * ({@code AttackDelivery} applies the weapon's own Efeito Crítico, {@code CriticalServiceImpl}
     * its margin), so a copy reporting the interface defaults would crit like a club.
     */
    private static AbstractItemBuilder<?, ?> baseBuilderFor(final ItemTemplate template) {
        if (!(template instanceof Weapon weapon)) {
            return AbstractItem.builder();
        }
        return AbstractWeapon.builder()
                .damageBase(weapon.getDamageBase())
                .skillType(weapon.getSkillType())
                .range(weapon.getRange())
                .criticalEffect(weapon.getCriticalEffect())
                .lesserCriticalMargin(weapon.getLesserCriticalMargin())
                .ammunitionKind(weapon.getAmmunitionKind())
                .magazineCapacity(weapon.getMagazineCapacity())
                .reloadCost(weapon.getReloadCost())
                .favoredReloadCost(weapon.getFavoredReloadCost());
    }

    /**
     * The Efeito Crítico Defensivo this copy grants its wearer — its catalog entry's, when that is an
     * Armadura or Escudo ({@link CriticallyDefensiveItem}); {@code null} for anything else.
     */
    @Override
    public DefensiveCriticalEffectType getDefensiveCriticalEffect() {
        return template instanceof CriticallyDefensiveItem defensive ? defensive.getDefensiveCriticalEffect() : null;
    }

    /**
     * Whether this copy is one stack of interchangeable units: its catalog entry sells in lots ({@link
     * ItemTemplate#getLotSize()} above 1) and nothing has made it unique — no Obra-Prima, Aprimoramento, Pedra,
     * Regalia mark or damage. Two stackable copies of the same template are the same thing in different amounts,
     * which is what lets {@code AbstractCombatantSheet#addToInventory} merge them.
     */
    @Override
    public boolean isStackable() {
        return template != null && template.getLotSize() > 1 && masterpiece == null && improvements.isEmpty()
                && powerStone == null && regaliaGrade == null && damageTaken == 0;
    }

    /** Adds units to this stack — a plain mutator; a non-positive count changes nothing. */
    public void addUnits(final int units) {
        if (units > 0) {
            quantity += units;
        }
    }

    /**
     * Removes up to units from this stack and returns how many were actually taken — never more than it holds,
     * never negative. A stack taken down to 0 stays an object; dropping it from the inventory is the caller's.
     */
    public int takeUnits(final int units) {
        int taken = Math.max(0, Math.min(units, quantity));
        quantity -= taken;
        return taken;
    }

    public static AbstractItem fromTemplate(final ItemTemplate template) {
        return builderFromTemplate(template).build();
    }

    /**
     * Sets the damage this copy carries outright. A plain mutator — prefer {@link
     * Item#applyDamage(int)}, which is what actually resolves the item's own mitigation; this is
     * for a fixture, a test, or a DTO restoring a previously-saved copy.
     */
    public void setDamageTaken(final int damageTaken) {
        this.damageTaken = damageTaken;
    }

    /**
     * Fits this copy's single Obra-Prima.
     *
     * <p>A {@link DefensiveMasterpiece} is refused bare because its entries carry creation-time
     * choices a copy must remember ({@code MAGISTRAL}'s Defesa, {@code SOB_MEDIDA}'s Reação/Ação
     * Livre) — {@link ItemMasterpiece} is the wrapper that holds them. {@link OffensiveMasterpiece}
     * needs no such wrapper and is fitted directly: no offensive entry has a choice this core can
     * read (Ossos de Monstro's Habilidade and Espírito Umbral's Subordinado are both blocked on
     * systems that don't exist), so a wrapper would carry nothing but a second name for the enum.
     */
    public void setMasterpiece(final Masterpiece masterpiece) {
        if (masterpiece instanceof DefensiveMasterpiece) {
            throw new IllegalArgumentException("Use ItemMasterpiece to fit a defensive masterpiece.");
        }
        if (masterpiece instanceof ItemMasterpiece && category != null && category.getType() != ItemType.DEFENSIVE) {
            throw new IllegalArgumentException("A defensive masterpiece requires a defensive item.");
        }
        // A Projétil is ammunition, not Equipamento to be improved: no list offers it an Obra-Prima.
        if (masterpiece != null && category == ItemCategory.PROJECTILE) {
            throw new IllegalArgumentException("A projectile cannot be a masterpiece.");
        }
        if (masterpiece instanceof OffensiveMasterpiece && category != null
                && category.getType() != ItemType.OFFENSIVE) {
            throw new IllegalArgumentException("An offensive masterpiece requires an offensive item.");
        }
        if (masterpiece instanceof ItemMasterpiece itemMasterpiece
                && itemMasterpiece.getDefinition() == DefensiveMasterpiece.DYOSPIROS
                && (category != ItemCategory.SHIELD
                || (weightClass != ItemWeightClass.MEDIUM && weightClass != ItemWeightClass.HEAVY))) {
            throw new IllegalArgumentException("Dyospiros requires a medium or heavy shield.");
        }
        this.masterpiece = masterpiece;
    }

    /**
     * Fits one more Aprimoramento to this copy. Keeps only the <b>type-compatibility</b> guards
     * the old single-value {@code setImprovement} carried (right wrapper class, defensive item,
     * CAMUFLADA/CAMADA_DE_REFORCO shape) — the <b>rules gates</b> (item must be an
     * Obra-Prima, the {@code getWeightClass().getMaximumImprovements()} slot cap, no duplicate
     * definition) live on {@code
     * org.aventyrs.core.character.services.EquipmentCraftingService#installImprovement}, the
     * validated player entry point, per CLAUDE.md's "Caps and prerequisites are enforced only on
     * the service entry point". The {@code @SuperBuilder}'s {@code improvements(...)} bypasses
     * even these.
     */
    public void addImprovement(final Improvement improvement) {
        if (improvement instanceof DefensiveImprovement) {
            throw new IllegalArgumentException("Use ItemImprovement to fit a defensive improvement.");
        }
        if (improvement instanceof ItemImprovement itemImprovement) {
            if (category != null && category.getType() != ItemType.DEFENSIVE) {
                throw new IllegalArgumentException("A defensive improvement requires a defensive item.");
            }
            if (itemImprovement.getDefinition() == DefensiveImprovement.CAMUFLADA
                    && category != ItemCategory.ARMOR) {
                throw new IllegalArgumentException("Camuflada requires an armor.");
            }
            if (itemImprovement.getDefinition() == DefensiveImprovement.CAMADA_DE_REFORCO
                    && ((category == ItemCategory.SHIELD) != (itemImprovement.getSelectedDefense() == null))) {
                throw new IllegalArgumentException("Camada de Reforço uses the shield variant only on shields.");
            }
        }
        if (improvement instanceof OffensiveImprovement offensive) {
            if (category != null && category.getType() != ItemType.OFFENSIVE) {
                throw new IllegalArgumentException("An offensive improvement requires an offensive item.");
            }
            // The two constants whose Características Adicionais say "Apenas …" outright. Machado
            // Anexo and Baioneta only *make sense* on a ranged weapon but never say "Apenas", so
            // they get no gate — catalogs and builders here are data holders, not gatekeepers.
            if (offensive == OffensiveImprovement.CORRENTE_COM_PESO && weightClass != ItemWeightClass.LIGHT) {
                throw new IllegalArgumentException("Corrente com Peso requires a light weapon.");
            }
            if (offensive == OffensiveImprovement.PENTE_ALONGADO
                    && (!(this instanceof Weapon weapon) || weapon.getSkillType() != SkillType.ATAQUE_A_DISTANCIA)) {
                throw new IllegalArgumentException("Pente Alongado requires a ranged weapon.");
            }
        }
        if (improvements == null) {
            improvements = new ArrayList<>();
        }
        improvements.add(improvement);
    }

    /**
     * Binds a Pedra do Poder to this item for the Duração of a Vinculação, or unbinds it with {@code null}. Refuses a
     * stone this copy cannot host ({@link Item#acceptsPowerStone(PowerStone)}). The Encaixe Aprimoramento is gone
     * (table ruling, 2026-10-12): any Obra-Prima Arma, Armadura or Escudo takes one.
     *
     * <p>A bare mutator: the Carga, the Danos, the PA and both Resfriamentos are {@code PowerStoneService#bind}'s,
     * which is the entry point a player goes through. The {@code @SuperBuilder}'s {@code powerStone(...)} bypasses even
     * the host check, per CLAUDE.md's "Builder-bypassable invariants".
     */
    public void setPowerStone(final PowerStone powerStone) {
        if (powerStone != null && !acceptsPowerStone(powerStone)) {
            throw new IllegalArgumentException("Only an Obra-Prima Arma, Armadura or Escudo can host this Pedra do Poder.");
        }
        this.powerStone = powerStone;
    }

    /**
     * Sets (or clears, with {@code null}) this copy's Regalia grade. A copy that carries an
     * {@link ItemActiveAbility} cannot be demoted to a non-Regalia — the same guard the old
     * boolean marker enforced. The {@code @SuperBuilder}'s {@code regaliaGrade(...)} bypasses it.
     */
    public void setRegaliaGrade(final RegaliaGrade regaliaGrade) {
        if (regaliaGrade == null && activeAbility != null) {
            throw new IllegalStateException("An item with an active ability must be a Regalia.");
        }
        this.regaliaGrade = regaliaGrade;
    }

    public void setActiveAbility(final ItemActiveAbility activeAbility) {
        if (activeAbility != null && regaliaGrade == null) {
            throw new IllegalStateException("Only a Regalia may have an active ability.");
        }
        this.activeAbility = activeAbility;
    }

    @Override
    public void activateImprovementEffect(final org.aventyrs.core.scene.SceneContext sceneContext,
                                          final int lastActiveRound) {
        if (sceneContext.getSceneId() == null || isDestroyed()) {
            return;
        }
        improvementEffectSceneId = sceneContext.getSceneId();
        improvementEffectLastActiveRound = lastActiveRound;
    }

    @Override
    public boolean hasActiveImprovementEffect(final org.aventyrs.core.scene.SceneContext sceneContext) {
        return !isDestroyed() && sceneContext != null && sceneContext.getSceneId() != null
                && sceneContext.getSceneId().equals(improvementEffectSceneId)
                && sceneContext.getCurrentRound() <= improvementEffectLastActiveRound;
    }
}
