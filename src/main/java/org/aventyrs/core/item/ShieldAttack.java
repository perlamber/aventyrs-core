package org.aventyrs.core.item;

import org.aventyrs.core.ability.ItemActiveAbility;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Objects;

/**
 * A Escudo swung as a weapon — {@code EscudeiroFeat#ATACAR_COM_ESCUDOS}' "Você pode usar seu escudo
 * para atacar". The {@link AttackSource} an Ataque com Escudo names, so every attack path this core
 * has (the roll, {@code AttackDelivery}, the dano roll, the action log) reaches it unchanged: "Ataques
 * com escudos podem receber benefícios de ataque e dano de Talentos e Habilidades Aventyrs".
 *
 * <p>A <b>view over an equipped Escudo</b>, not an item of its own: it wraps the copy on the
 * wielder's equipment (so damaging "a arma utilizada" damages the shield itself) and states only the
 * weapon columns the Talento authors — Ataque Corpo-a-Corpo, "Escudos Leves 1d6+1, Médios 1d6+2,
 * Pesados 1d6+3", "Margem Crítica Menor 17 e Sucesso Crítico: Atordoante". Two views of one shield are
 * {@link #equals equal}, which is how "atacar duas ou mais vezes com um escudo" is counted off the
 * action log. {@link #wings()} is {@code EscudeiroFeat#ASAS_ADAMANTINAS}' "suas Asas são consideradas
 * itens do tipo Escudo" — a Escudo Médio with nothing to wrap (table ruling).
 *
 * <p>It also carries <b>which Defesa the attack is rolled against</b> ({@link #getTargetDefense()}),
 * because the roll adds "os Bônus de Defesa Física quando rolada contra a DF do alvo ou com os Bônus
 * de Defesa Mágica quando rolada contra a DM do alvo"; {@code AttackDelivery} re-aims it at its own
 * {@code DefenseType} ({@link #against}).
 *
 * <p>Nothing here checks the wielder may swing it: {@code AbstractSkillInteraction} refuses an Ataque
 * com Escudo from someone without Atacar com Escudos, or with a shield not equipped, or with wings
 * while flying ({@code SHIELD_ATTACK_NOT_PERMITTED}).
 */
public final class ShieldAttack implements Weapon {

    /** Wings count as a Escudo Médio (table ruling). */
    public static final ItemWeightClass WINGS_WEIGHT = ItemWeightClass.MEDIUM;

    private static final ShieldAttack WINGS = new ShieldAttack(null, DefenseType.PHYSICAL);

    private final Item shield;
    private final DefenseType targetDefense;

    private ShieldAttack(final Item shield, final DefenseType targetDefense) {
        this.shield = shield;
        this.targetDefense = targetDefense == null ? DefenseType.PHYSICAL : targetDefense;
    }

    /**
     * An Ataque com Escudo with shield, rolled against the Defesa Física.
     *
     * @throws IllegalArgumentException when shield is not an {@link ItemCategory#SHIELD}
     */
    public static ShieldAttack of(final Item shield) {
        if (shield == null || shield.getCategory() != ItemCategory.SHIELD) {
            throw new IllegalArgumentException("Not a Escudo: " + shield);
        }
        return new ShieldAttack(shield, DefenseType.PHYSICAL);
    }

    /** An attack with Asas Adamantinas' wings, rolled against the Defesa Física. */
    public static ShieldAttack wings() {
        return WINGS;
    }

    /** This same attack, rolled against defenseType instead. */
    public ShieldAttack against(final DefenseType defenseType) {
        return defenseType == targetDefense ? this : new ShieldAttack(shield, defenseType);
    }

    /** The equipped Escudo swung, or {@code null} for {@link #wings()}. */
    public Item getShield() {
        return shield;
    }

    public boolean isWings() {
        return shield == null;
    }

    /** The Defesa this attack is rolled against — which of the shield's Defesa bonuses the roll adds. */
    public DefenseType getTargetDefense() {
        return targetDefense;
    }

    /** "Escudos Leves 1d6+1, Médios 1d6+2, Pesados 1d6+3". */
    @Override
    public DamageBase getDamageBase() {
        return switch (getWeightClass()) {
            case LIGHT -> DamageBase.of(1, 1);
            case MEDIUM -> DamageBase.of(1, 2);
            default -> DamageBase.of(1, 3);
        };
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_CORPO_A_CORPO;
    }

    /** "Escudos possuem Margem Crítica Menor 17". */
    @Override
    public int getLesserCriticalMargin() {
        return Weapon.DEFAULT_LESSER_CRITICAL_MARGIN;
    }

    /** "… e Sucesso Crítico: Atordoante". */
    @Override
    public CriticalEffectType getCriticalEffect() {
        return CriticalEffectType.ATORDOANTE;
    }

    @Override
    public String getName() {
        return shield == null ? "Asas" : shield.getName();
    }

    @Override
    public String getDescription() {
        return shield == null ? "" : shield.getDescription();
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.SHIELD;
    }

    @Override
    public ItemRarity getRarity() {
        return shield == null ? ItemRarity.COMMON : shield.getRarity();
    }

    /** The authored weight — the column the dano table keys on. */
    @Override
    public ItemWeightClass getWeightClass() {
        return shield == null ? WINGS_WEIGHT : shield.getWeightClass();
    }

    @Override
    public int getPrice() {
        return shield == null ? 0 : shield.getPrice();
    }

    @Override
    public int getPhysicalDefenseBonus() {
        return shield == null ? 0 : shield.getPhysicalDefenseBonus();
    }

    @Override
    public int getMagicDefenseBonus() {
        return shield == null ? 0 : shield.getMagicDefenseBonus();
    }

    @Override
    public int getHardness() {
        return shield == null ? 0 : shield.getHardness();
    }

    @Override
    public int getDamageTaken() {
        return shield == null ? 0 : shield.getDamageTaken();
    }

    @Override
    public boolean isDestroyed() {
        return shield != null && shield.isDestroyed();
    }

    /** Damage to "a arma utilizada" lands on the shield itself. */
    @Override
    public int applyDamage(final int rawDamage) {
        return shield == null ? 0 : shield.applyDamage(rawDamage);
    }

    @Override
    public int repair(final int rawRecovery) {
        return shield == null ? 0 : shield.repair(rawRecovery);
    }

    @Override
    public int getCastingBonus() {
        return shield == null ? 0 : shield.getCastingBonus();
    }

    @Override
    public ItemFavor getFavor() {
        return shield == null ? null : shield.getFavor();
    }

    @Override
    public Masterpiece getMasterpiece() {
        return shield == null ? null : shield.getMasterpiece();
    }

    @Override
    public List<Improvement> getImprovements() {
        return shield == null ? List.of() : shield.getImprovements();
    }

    @Override
    public ItemActiveAbility getActiveAbility() {
        return shield == null ? null : shield.getActiveAbility();
    }

    @Override
    public RegaliaGrade getRegaliaGrade() {
        return shield == null ? null : shield.getRegaliaGrade();
    }

    /** Two views of the same shield (or both of the wings) are one weapon, whatever Defesa they aim at. */
    @Override
    public boolean equals(final Object other) {
        return other instanceof ShieldAttack that && this.shield == that.shield;
    }

    @Override
    public int hashCode() {
        return shield == null ? 0 : System.identityHashCode(shield);
    }

    @Override
    public String toString() {
        return "ShieldAttack[" + getName() + "]";
    }

    /** Whether item is the equipped Escudo this attack swings. */
    public boolean swings(final Item item) {
        return shield != null && shield == item;
    }

    /** Whether source is an Ataque com Escudo swinging item. */
    public static boolean swings(final Object source, final Item item) {
        return source instanceof ShieldAttack attack && attack.swings(item);
    }

    /** Whether source is an attack with the wings. */
    public static boolean isWingsAttack(final Object source) {
        return source instanceof ShieldAttack attack && attack.isWings();
    }

    /**
     * What bonus — a shield's (or the wings') Defesa — becomes once it has attacked since its holder's
     * latest own Turn began: the actions of this Rodada and of that Turn whose source swung matches,
     * counted once each. No attack keeps everything. Otherwise the most generous {@code
     * Feat#resolveShieldDefenseRetention} among the held Talentos applies, and with none speaking
     * Atacar com Escudos' own rule does: one attack keeps half, two or more keep nothing.
     */
    public static int retained(final org.aventyrs.core.sheet.CombatantSheet holder,
                               final java.util.function.Predicate<Object> swung, final int bonus) {
        if (holder == null || bonus <= 0) {
            return bonus;
        }
        java.util.Set<org.aventyrs.core.sheet.CombatantAction> attacks =
                java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        holder.getActionsThisRound().stream().filter(action -> swung.test(action.attackSource())).forEach(attacks::add);
        holder.getActionsOfLatestOwnTurn().stream().filter(action -> swung.test(action.attackSource()))
                .forEach(attacks::add);
        int count = attacks.size();
        if (count == 0) {
            return bonus;
        }
        ShieldDefenseRetention retention = null;
        for (org.aventyrs.core.feat.Feat feat : holder.getCharacter().getFeats()) {
            ShieldDefenseRetention stated = feat.resolveShieldDefenseRetention(count);
            retention = stated == null ? retention : stated.atLeast(retention);
        }
        if (retention == null) {
            retention = count == 1 ? ShieldDefenseRetention.HALF : ShieldDefenseRetention.NONE;
        }
        return retention.apply(bonus);
    }

    /** {@link Objects#requireNonNull} guard kept for callers wanting the wrapped copy. */
    public Item requireShield() {
        return Objects.requireNonNull(shield, "wings wrap no item");
    }
}
