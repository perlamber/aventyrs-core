package org.aventyrs.core.item;

import lombok.Getter;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.SkillType;

/**
 * The Armas Naturais catalog — Chifres Poderosos, Garras Afiadas, a Cauda Chicote, an Arma de
 * Sopro: the weapons that are part of a creature's body rather than something forged or bought.
 * One constant per entry of {@code docs/rules/equipamentos.txt}'s "Armas Naturais" table,
 * mirroring {@link ArmorItem}'s one-enum-per-{@link ItemCategory} shape (and, like it, holding
 * the rules text as the single source of truth rather than duplicating it).
 *
 * <p>Implements both {@link ItemTemplate} (a catalog blueprint, never a forged per-copy object —
 * a body part takes no Dureza damage and carries no Obra-Prima) and {@link Weapon} (it has a
 * Dano Base and a Perícia, and is an {@link org.aventyrs.core.skill.AttackSource}). Every entry
 * reports {@link ItemCategory#NATURAL_WEAPON}, so {@link
 * org.aventyrs.core.character.Character#treatsAsNaturalWeapon(Weapon)} already recognises one
 * with no change, and {@code DamageBaseService#getDamageBase(Character, Weapon)} scales it up
 * from its authored row exactly as it does a machado.
 *
 * <h2>What is <em>not</em> modeled, and why</h2>
 *
 * <ul>
 *   <li><b>The Favor is {@code null} on every constant.</b> None of the authored natural-weapon
 *   Favors has a {@link org.aventyrs.core.modifier.ModifierType} to carry it: "Roubo de Vida 1"
 *   (Presas Longas), "Margem Crítica aumentada em +1 número" (Garras Afiadas), "Adiciona metade
 *   do Vigor ao Dano" (Arma de Sopro) and the Correntes de Efeitos (Cauda Chicote/Constritora)
 *   all name mechanics with no {@code ItemBonus}-expressible shape. Per the {@code
 *   adding-an-item} skill, such a clause contributes no {@code ItemBonus} and stays in prose —
 *   here, in each constant's javadoc.</li>
 *   <li><b>No Efeito Crítico column.</b> Each entry authors one ("Empalar (17)", "Dilacerar
 *   (16)", …), but nothing scans a {@link Weapon} for a crit effect — {@code AttackDelivery}
 *   only reads the caller-supplied list plus {@code Feat#resolveExtraCriticalEffects}. Recorded
 *   in javadoc rather than added as an unread column (contrast {@link
 *   ArmorItem}'s {@code defensiveCriticalEffect}, which had the same "nothing reads it yet"
 *   status but a clear second consumer on the way). Add the column with its first real reader.</li>
 *   <li><b>Preço / DF / DM / Dureza / Conjuração are all 0.</b> A natural weapon has no Preço
 *   line at all, and is not an object that breaks or that a PE economy prices — {@link
 *   ItemRarity#NATURAL} is the marker for exactly that.</li>
 * </ul>
 *
 * <p>Granted to a character by {@code Feat#getGrantedNaturalWeapons} and surfaced through {@code
 * Character#getNaturalWeapons()} — see those. {@code DraconicoFeat#ARMAMENTO_DRACONICO} (via
 * {@link org.aventyrs.core.feat.ArmamentoDraconicoFeat}), {@code DraconicoFeat#SOPRO_DE_DRAGAO}
 * and {@code BestialFeat}'s Bovídea/Canina/Felina Heranças are the first grantors.
 */
@Getter
public enum NaturalWeapon implements ItemTemplate, Weapon {

    /**
     * Arma de Sopro (Leve/Natural) — "permitem que certos personagens criem e emitam energia
     * física elemental". Dano 1d6+2, Elemental, Efeito Crítico Cataclismo (17), Alcance
     * Cone (Médio) — modeled as {@link Range#DISTANCIA_MEDIA} (this core has no cone footprint;
     * see CLAUDE.md's "Area de Efeito" row) — Requisito Vigor 3.
     *
     * <p><b>Favor</b> "Adiciona metade do Vigor ao Dano" and the <b>Efeitos Adicionais</b>
     * "Refrigeração 1, dano reduzido em -1 à cada UD percorrido" are unmodeled: a Vigor-scaled
     * dano bonus is not a fixed {@code ItemBonus}, and neither Refrigeração nor per-UD falloff
     * has any reader. The Elemental damage type is likewise not carried — {@code DamageType} has
     * no elemental breakdown.
     */
    ARMA_DE_SOPRO(
            "Arma de Sopro",
            "Armas de Sopro permitem que certos personagens criem e emitam energia física "
                    + "elemental.",
            ItemWeightClass.LIGHT,
            DamageBase.of(1, 2),
            SkillType.ATAQUE_A_DISTANCIA,
            Range.DISTANCIA_MEDIA),

    /**
     * Cauda Chicote (Média/Natural) — "uma Cauda fina e leve, mas muito rápida … ideal para
     * ataques velozes". Dano 1d6+1, Esmagamento, Efeito Crítico Estilhaçador (17), Alcance
     * Adjacente, Requisito Destreza 3.
     *
     * <p><b>Favor</b> (Corrente de Efeitos – Chicotear: empurra o alvo 1UD e causa 2 de dano aos
     * equipamentos defensivos) and the <b>Efeito Adicional</b> (empurra o alvo 1UD em Acertos
     * Críticos) are unmodeled: this core does no forced movement (CLAUDE.md's "Forced movement /
     * positioning" row) and has no automatic item-damage caller ("Damage to an item…" section).
     */
    CAUDA_CHICOTE(
            "Cauda Chicote",
            "Uma Cauda fina e leve, mas muito rápida, algumas vezes com ossos sobressalentes e "
                    + "pesados na ponta, sendo ideal para ataques velozes.",
            ItemWeightClass.MEDIUM,
            DamageBase.of(1, 1),
            SkillType.ATAQUE_CORPO_A_CORPO,
            Range.ADJACENTE),

    /**
     * Cauda Constritora (Pesada/Natural) — "uma Cauda longa e pesada, que permite se enrolar nas
     * mais diversas superfícies e criaturas". Dano 1d6+1, Esmagamento, Efeito Crítico
     * Estilhaçador (17), Alcance Adjacente, Requisito Vigor 3.
     *
     * <p><b>Favor</b> (Corrente de Efeitos – Agarrar e Constringir: prende o alvo, causando dano
     * igual ao Vigor por Rodada) and its <b>Efeito Adicional</b> are unmodeled — agarrar is not
     * a manoeuvre this core represents (CLAUDE.md's Herança Anfíbia TODO makes the same point).
     */
    CAUDA_CONSTRITORA(
            "Cauda Constritora",
            "Uma Cauda longa e pesada, que permite se enrolar nas mais diversas superfícies e "
                    + "criaturas.",
            ItemWeightClass.HEAVY,
            DamageBase.of(1, 1),
            SkillType.ATAQUE_CORPO_A_CORPO,
            Range.ADJACENTE),

    /**
     * Chifres Poderosos (Média/Natural) — "chifres, galhadas ou cornos poderosos, capazes de
     * desferir ataques poderosos". Dano 1d6+1, Perfuração, Efeito Crítico Empalar (17), Alcance
     * Adjacente, Requisito Força 3.
     *
     * <p><b>Favor</b> ("Dano em investidas aumenta +1d6") and the <b>Efeito Adicional</b>
     * ("Atacar após se mover em direção ao alvo aumenta o dano em +1") are unmodeled: Investida
     * is an unmodelled manoeuvre (CLAUDE.md's Movimento Base row) and this core rolls no dice.
     */
    CHIFRES_PODEROSOS(
            "Chifres Poderosos",
            "Chifres, galhadas ou cornos poderosos, capazes de desferir ataques poderosos.",
            ItemWeightClass.MEDIUM,
            DamageBase.of(1, 1),
            SkillType.ATAQUE_CORPO_A_CORPO,
            Range.ADJACENTE),

    /**
     * Garras Afiadas (Leve/Natural) — "garras longas e naturalmente afiadas, capaz de retalhar a
     * carne de seus inimigos". Dano 1d6+1, Corte, Efeito Crítico Dilacerar (16), Alcance
     * Adjacente, Requisito Destreza 3.
     *
     * <p><b>Favor</b> ("Margem Crítica aumentada em +1 número") and the <b>Efeito Adicional</b>
     * ("Não é possível segurar objetos enquanto ataca com as Garras Afiadas") are unmodeled: no
     * {@code ModifierType} carries a Margem Crítica widening from an item (contrast {@code
     * Feat#resolveCriticalMarginIncrease}, which is a Talento hook), and this core models no hand
     * slots (CLAUDE.md's "no hand slots" note).
     */
    GARRAS_AFIADAS(
            "Garras Afiadas",
            "Garras longas e naturalmente afiadas, capaz de retalhar a carne de seus inimigos.",
            ItemWeightClass.LIGHT,
            DamageBase.of(1, 1),
            SkillType.ATAQUE_CORPO_A_CORPO,
            Range.ADJACENTE),

    /**
     * Presas Longas (Leve/Natural). Dano 1d6+1, Perfuração, Efeito Crítico Sangramento (16),
     * Alcance Adjacente, Requisito Vigor 5.
     *
     * <p><b>Favor</b> ("Ataques com as Presas Longas recebem Roubo de Vida 1") and the <b>Efeito
     * Adicional</b> (Corrente de Efeitos – Saliva Anticoagulante) are unmodeled: {@code
     * LifeStealService} exists but is driven per attack by a caller, with no item-granted hook,
     * and a per-weapon Margem Crítica / Sangramento-duration rider has no reader.
     */
    PRESAS_LONGAS(
            "Presas Longas",
            "",
            ItemWeightClass.LIGHT,
            DamageBase.of(1, 1),
            SkillType.ATAQUE_CORPO_A_CORPO,
            Range.ADJACENTE),

    /**
     * Ataque Desarmado (Leve/Natural) — "ataques feitos com punhos, pernas e outras partes do
     * corpo". Dano 1d6 ({@link DamageBase#UNARMED}), Esmagamento, Efeito Crítico Atordoante (17),
     * Alcance Adjacente.
     *
     * <p>The authored catalog row for a bare punch. It is <em>not</em> a new mechanic: {@code
     * DamageBaseService}'s unarmed overload already starts every Ataque Desarmado at {@link
     * DamageBase#UNARMED} without naming a weapon, and CLAUDE.md treats a punch as exactly that.
     * This constant exists so a caller that <em>does</em> want to name it (a UI listing what a
     * character can strike with) has something to point at, and so the table is complete.
     *
     * <p>Its <b>Efeito Adicional</b> "Desvantagem nas rolagens de Ataque Corpo-a-Corpo" is
     * unmodeled — the dano-roll / roll malus for an unnamed Ataque Desarmado is one of the
     * consumers CLAUDE.md's "Classifying an attack as Desarmado/Arma Natural" row lists as still
     * blocked.
     */
    ATAQUE_DESARMADO(
            "Ataque Desarmado",
            "Ataques feitos com punhos, pernas e outras partes do corpo.",
            ItemWeightClass.LIGHT,
            DamageBase.UNARMED,
            SkillType.ATAQUE_CORPO_A_CORPO,
            Range.ADJACENTE);

    private final String name;
    private final String description;
    private final ItemWeightClass weightClass;
    private final DamageBase damageBase;
    private final SkillType skillType;
    private final Range range;

    NaturalWeapon(final String name, final String description, final ItemWeightClass weightClass,
                  final DamageBase damageBase, final SkillType skillType, final Range range) {
        this.name = name;
        this.description = description;
        this.weightClass = weightClass;
        this.damageBase = damageBase;
        this.skillType = skillType;
        this.range = range;
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.NATURAL_WEAPON;
    }

    @Override
    public ItemRarity getRarity() {
        return ItemRarity.NATURAL;
    }

    /** Always 0 — a natural weapon has no Preço line and no PE economy would ever price one. */
    @Override
    public int getPrice() {
        return 0;
    }

    /** Always 0 — an Arma Natural grants no Defesa Física (a Defesa Natural is a separate table). */
    @Override
    public int getPhysicalDefenseBonus() {
        return 0;
    }

    /** Always 0 — see {@link #getPhysicalDefenseBonus()}. */
    @Override
    public int getMagicDefenseBonus() {
        return 0;
    }

    /** Always 0 — a body part is not an object that takes Dureza damage; {@link #isDestroyed()} is never true. */
    @Override
    public int getHardness() {
        return 0;
    }

    /** Always 0 — no Arma Natural carries a Conjuração column. */
    @Override
    public int getCastingBonus() {
        return 0;
    }

    /** Always {@code null} — no authored natural-weapon Favor has a {@code ModifierType} to carry it. */
    @Override
    public ItemFavor getFavor() {
        return null;
    }
}
