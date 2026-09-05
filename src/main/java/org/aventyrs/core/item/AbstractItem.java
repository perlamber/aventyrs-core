package org.aventyrs.core.item;

import org.aventyrs.core.ability.ItemActiveAbility;

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
 * <p>An item built through this has no Dano Base and cannot be swung — see {@link
 * AbstractWeapon} for the {@link Weapon} counterpart, which extends this class and adds that
 * one column.
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
     * A builder pre-filled with template's catalog columns — the shared half of {@link
     * #fromTemplate} and {@code
     * org.aventyrs.core.character.services.EquipmentCraftingService#forge} (which overrides
     * {@code hardness} and sets {@code producedByCharacterId} before building). Per-copy state
     * (damage, Obra-Prima, Aprimoramentos, Pedra do Poder) is deliberately not carried over.
     */
    public static AbstractItemBuilder<?, ?> builderFromTemplate(final ItemTemplate template) {
        return AbstractItem.builder()
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
                .favor(template.getFavor());
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

    public void setMasterpiece(final Masterpiece masterpiece) {
        if (masterpiece instanceof DefensiveMasterpiece) {
            throw new IllegalArgumentException("Use ItemMasterpiece to fit a defensive masterpiece.");
        }
        if (masterpiece instanceof ItemMasterpiece && category != null && category.getType() != ItemType.DEFENSIVE) {
            throw new IllegalArgumentException("A defensive masterpiece requires a defensive item.");
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
     * ENCAIXE/CAMUFLADA/CAMADA_DE_REFORCO shape) — the <b>rules gates</b> (item must be an
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
            if (itemImprovement.getDefinition() == DefensiveImprovement.ENCAIXE
                    && category != ItemCategory.ARMOR && category != ItemCategory.SHIELD) {
                throw new IllegalArgumentException("Encaixe requires an armor or shield.");
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
        if (improvements == null) {
            improvements = new ArrayList<>();
        }
        improvements.add(improvement);
    }

    /**
     * Sockets a Pedra do Poder into this item. Requires the Encaixe Aprimoramento
     * ({@link DefensiveImprovement#ENCAIXE}) already fitted — "permite encaixe de Pedra do
     * Poder" — so an armor or shield only, until an offensive Encaixe exists. Same
     * guard-on-the-setter style as {@link #addImprovement}/{@link #setMasterpiece}; the
     * {@code @SuperBuilder}'s {@code powerStone(...)} bypasses it, per CLAUDE.md's
     * "Builder-bypassable invariants".
     */
    public void setPowerStone(final PowerStone powerStone) {
        boolean encaixeFitted = improvements != null && improvements.stream()
                .anyMatch(fitted -> fitted instanceof ItemImprovement itemImprovement
                        && itemImprovement.getDefinition() == DefensiveImprovement.ENCAIXE);
        if (powerStone != null && !encaixeFitted) {
            throw new IllegalArgumentException(
                    "A Pedra do Poder requires the Encaixe Aprimoramento fitted to its host item.");
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
