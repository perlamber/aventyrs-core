package org.aventyrs.core.item;

import org.aventyrs.core.ability.ItemActiveAbility;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

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
    private Improvement improvement;
    private PowerStone powerStone;
    private boolean regalia;
    private ItemActiveAbility activeAbility;
    private UUID improvementEffectSceneId;
    private int improvementEffectLastActiveRound;

    public static AbstractItem fromTemplate(final ItemTemplate template) {
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
                .favor(template.getFavor())
                .build();
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

    public void setImprovement(final Improvement improvement) {
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
        this.improvement = improvement;
    }

    /**
     * Sockets a Pedra do Poder into this item. Requires the Encaixe Aprimoramento
     * ({@link DefensiveImprovement#ENCAIXE}) already fitted — "permite encaixe de Pedra do
     * Poder" — so an armor or shield only, until an offensive Encaixe exists. Same
     * guard-on-the-setter style as {@link #setImprovement}/{@link #setMasterpiece}; the
     * {@code @SuperBuilder}'s {@code powerStone(...)} bypasses it, per CLAUDE.md's
     * "Builder-bypassable invariants".
     */
    public void setPowerStone(final PowerStone powerStone) {
        if (powerStone != null && !(improvement instanceof ItemImprovement fitted
                && fitted.getDefinition() == DefensiveImprovement.ENCAIXE)) {
            throw new IllegalArgumentException(
                    "A Pedra do Poder requires the Encaixe Aprimoramento fitted to its host item.");
        }
        this.powerStone = powerStone;
    }

    public void setRegalia(final boolean regalia) {
        if (!regalia && activeAbility != null) {
            throw new IllegalStateException("An item with an active ability must be a Regalia.");
        }
        this.regalia = regalia;
    }

    public void setActiveAbility(final ItemActiveAbility activeAbility) {
        if (activeAbility != null && !regalia) {
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
