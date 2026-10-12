package org.aventyrs.core.item;

import lombok.Getter;

/**
 * The Projéteis catalog — {@link ItemCategory#PROJECTILE}, one constant per {@link ProjectileHead} per {@link
 * AmmunitionKind} it is made in: a Flecha for an Arco, a Virote for a Besta, identical in everything else (table
 * ruling, 2026-10-12). Dardos for a Zarabatana are not authored yet (see {@link AmmunitionKind#DART}).
 *
 * <p>A projectile is <b>ammunition, not a weapon</b>: it implements {@link ItemTemplate} only, so nothing can draw it
 * or name it as an {@code AttackSource}. It is <b>stackable</b> — {@link #getLotSize()} is the "Preço para N
 * unidades" lot, and a bought or looted copy carries that many units in {@link Item#getQuantity()} — and it is
 * <b>consumed</b> by firing ({@code combat.AmmunitionService}). Its Efeito Crítico, Margem and Corrente live on
 * {@link #getHead()}.
 */
@Getter
public enum ProjectileItem implements ItemTemplate {

    FLECHA_BODKIN(ProjectileHead.BODKIN, AmmunitionKind.ARROW),
    VIROTE_BODKIN(ProjectileHead.BODKIN, AmmunitionKind.BOLT),
    FLECHA_ESFERICA(ProjectileHead.ESFERICA, AmmunitionKind.ARROW),
    VIROTE_ESFERICO(ProjectileHead.ESFERICA, AmmunitionKind.BOLT),
    FLECHA_FOLHA_CORTANTE(ProjectileHead.FOLHA_CORTANTE, AmmunitionKind.ARROW),
    VIROTE_FOLHA_CORTANTE(ProjectileHead.FOLHA_CORTANTE, AmmunitionKind.BOLT),
    FLECHA_PONTA_SIMPLES(ProjectileHead.PONTA_SIMPLES, AmmunitionKind.ARROW),
    VIROTE_PONTA_SIMPLES(ProjectileHead.PONTA_SIMPLES, AmmunitionKind.BOLT),
    FLECHA_PONTA_DUPLA(ProjectileHead.PONTA_DUPLA, AmmunitionKind.ARROW),
    VIROTE_PONTA_DUPLA(ProjectileHead.PONTA_DUPLA, AmmunitionKind.BOLT),
    FLECHA_PORTA_FOGO(ProjectileHead.PORTA_FOGO, AmmunitionKind.ARROW),
    VIROTE_PORTA_FOGO(ProjectileHead.PORTA_FOGO, AmmunitionKind.BOLT);

    private final ProjectileHead head;
    private final AmmunitionKind ammunitionKind;

    ProjectileItem(final ProjectileHead head, final AmmunitionKind ammunitionKind) {
        this.head = head;
        this.ammunitionKind = ammunitionKind;
    }

    /** The projectile a copy is made from, or {@code null} for any other item — a forged copy keeps its template. */
    public static ProjectileItem of(final Item item) {
        if (item instanceof ProjectileItem template) {
            return template;
        }
        return item instanceof AbstractItem copy && copy.getTemplate() instanceof ProjectileItem template
                ? template : null;
    }

    /**
     * A copy of one whole lot that remembers its template — what makes it {@linkplain Item#isStackable() stackable}
     * and recognisable to {@link #of(Item)}. The interface's bare bridge copy keeps no template.
     */
    @Override
    public Item forge() {
        return AbstractItem.fromTemplate(this);
    }

    @Override
    public String getName() {
        return (ammunitionKind == AmmunitionKind.ARROW ? "Flecha " : "Virote ") + head.getName();
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.PROJECTILE;
    }

    @Override
    public ItemRarity getRarity() {
        return head.getRarity();
    }

    /** No weight class is printed for a Projétil; a single unit is read as Leve. */
    @Override
    public ItemWeightClass getWeightClass() {
        return ItemWeightClass.LIGHT;
    }

    /** The Preço of one lot of {@link #getLotSize()} units. */
    @Override
    public int getPrice() {
        return head.getPrice();
    }

    @Override
    public int getLotSize() {
        return head.getLotSize();
    }

    @Override
    public int getPhysicalDefenseBonus() {
        return 0;
    }

    @Override
    public int getMagicDefenseBonus() {
        return 0;
    }

    @Override
    public int getHardness() {
        return 0;
    }

    @Override
    public int getCastingBonus() {
        return 0;
    }

    /** Every row's Favor is "Nenhum"; its Efeitos Adicionais live on {@link #getHead()}. */
    @Override
    public ItemFavor getFavor() {
        return null;
    }
}
