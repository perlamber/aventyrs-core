package org.aventyrs.core.item;

import java.util.List;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CriticalDamage;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.modifier.ModifierType;

import lombok.Getter;

/**
 * The catalog of Obra-Prima options that can be fitted to an offensive item — the "Obras-Primas
 * Ofensivas" list of {@code docs/rules/equipamentos.txt}, the mirror of {@link
 * DefensiveMasterpiece}.
 *
 * <p><b>The name alone is not a key.</b> Seven "Material Especial – …" entries and Conjuradora,
 * Equilibrada, Magistral and Banhada em Prata/Ouro all recur in the Defensiva list with
 * <em>different</em> Favor text and columns — the list a constant is printed under is what
 * identifies it, so this enum and {@link DefensiveMasterpiece} deliberately share several names
 * and agree on nothing else.
 *
 * <p><b>Two extra columns.</b> The Ofensiva table prints {@code DF | DM | Ataque | Danos |
 * Conjuração | Requisitos} where the Defensiva one prints only {@code DF | DM | Conjuração |
 * Requisitos}, which is why {@link Masterpiece#getAttackBonus()}/{@link
 * Masterpiece#getDamageBonus()} exist at all. Both are exact, authored, and <b>read by nothing</b>
 * — the same state {@link Improvement}'s pair has carried since the defensive catalog was written
 * (four {@code DefensiveImprovement} constants author them too). An "Ataque" bonus would need a
 * roll pass scoped to <i>the weapon the attack was delivered with</i>: {@code
 * AbstractSkillInteraction#sumEquipmentRollBonuses} scans the whole loadout, so routing them
 * through {@code ModifierType.ATAQUE_*_ROLL_BONUS} would let a sheathed Precisa dagger sharpen a
 * sword swing. Add that pass with its first real consumer, not here.
 *
 * <p><b>What is live.</b> The DF/DM columns reach {@code DefenseService}; a "Dano Base da Arma
 * aumenta em +1" Favor reaches {@code DamageBaseService} through {@link
 * #resolveDamageBaseIncrease}; a "Dano sofrido reduzido" Favor reaches {@code DamageService}
 * through {@link #resolveBonus}; and every constant prices off the Armas column of "Preços de
 * Obras-Primas" ({@link EnhancementPriceCategory#WEAPON}) — the reason {@link
 * Masterpiece#getPriceCategory()} was declared abstract rather than defaulted to armour.
 *
 * <p><b>Requisitos gate the Favor, never the columns</b>, exactly as {@link DefensiveMasterpiece}
 * has it: the DF/DM/Ataque/Danos/Conjuração row is what the piece <em>is</em>, and the Favor line
 * is what its wielder earns by being strong/deft/learned enough to use it.
 *
 * <p>A "/" in a Requisitos column is read as <b>both</b> Atributos here, matching {@link
 * DefensiveMasterpiece}'s reading of the same notation in the same table. ({@link ItemRequirements}
 * reads the same "/" on a base item's own Requisitos column as <i>either</i> — a pre-existing
 * disagreement between the two catalogs, left as found rather than silently reconciled.)
 */
@Getter
public enum OffensiveMasterpiece implements Masterpiece {
    // The Favor restates the Ataque column rather than adding to it ("Bônus em rolagens de Ataque
    // +1" is that +1), so there is one grant here, not two — the same net-effect check
    // ArmorItem#ROUPA_PESADA's two clauses needed. TODO: nothing reads the Ataque column; see this
    // enum's javadoc for the weapon-scoped roll pass it waits on.
    PRECISA("Precisa", ItemRarity.COMMON, 0, 0, 1, 0, 0, requirements(AttributeDomain.DEXTERITY, 3),
            "Bônus em rolagens de Ataque +1.", null),
    BRUTAL("Brutal", ItemRarity.UNCOMMON, 0, 0, 0, 1, 0, requirements(AttributeDomain.STRENGTH, 3),
            "Dano Base da Arma aumenta em +1.", null),
    // TODO: the Favor's Ataque half waits on the weapon-scoped roll pass (see this enum's javadoc);
    // its Conjuração half has nowhere to land at all — neither SpellCastingService roll takes an
    // item-granted modifier, which is why Item#getCastingBonus() is inert too.
    EQUILIBRADA("Equilibrada", ItemRarity.UNCOMMON, 0, 0, 0, 1, 0,
            requirements(AttributeDomain.STRENGTH, 3, AttributeDomain.GNOSE, 3),
            "Bônus em rolagens de Ataque e Conjuração +1.", null),
    DEFENSORA("Defensora", ItemRarity.COMMON, 1, 1, 0, 0, 0,
            requirements(AttributeDomain.DEXTERITY, 3, AttributeDomain.GNOSE, 3),
            "Dano sofrido reduzido em -1.", null),
    // TODO: a Conjuração bonus has no consumer — see EQUILIBRADA.
    CONJURADORA("Conjuradora", ItemRarity.UNCOMMON, 0, 0, 0, 0, 1, requirements(AttributeDomain.FOCUS, 3),
            "Bônus em rolagens de Conjuração +1.", null),
    // The Característica Adicional's "Duração +1" is real (resolveDurationIncreaseInRounds, read by
    // SpellDurationService) and unconditional, being an Adicional rather than a Favor.
    // TODO: the Favor's Corrente de Efeitos and the Adicional's "Dano e Cura +1" are not — a Magia
    // carries no numeric damage/healing effect for an item to raise (see DefensiveMasterpiece
    // #BANHADA_EM_OURO, blocked on the same column), and no EffectChain exists.
    PODEROSA("Poderosa", ItemRarity.RARE, 0, 0, 0, 0, 1, requirements(AttributeDomain.FOCUS, 3),
            "Magias de Dano ou Cura, Corrente de Efeitos – Foco: Efeito +2.",
            "Magias tem Dano e Cura +1, Duração +1."),
    // TODO: the Margem Crítica hook now exists (resolveCriticalMarginIncrease), but this clause also
    // needs a Monstro classification of the attack's *target*, which no enhancement hook is handed.
    BANHADA_EM_PRATA("Banhada em Prata", ItemRarity.UNCOMMON, 0, 0, 1, 1, 1,
            requirements(AttributeDomain.GNOSE, 3),
            "Margem Crítica Menor aumentada em +2 contra Monstros.",
            "Benefícios desta Obra-Prima são aplicados apenas em ataques, Conjurações de Magias e efeitos rolados contra Monstros."),
    // TODO: needs which Defesa pool the attack was rolled against (an enhancement is never told),
    // and "ignora RM" — no mechanism bypasses a mitigation stage (see DamageService).
    BANHADA_EM_OURO("Banhada em Ouro", ItemRarity.RARE, 0, 0, 1, 0, 1, requirements(AttributeDomain.FOCUS, 3),
            "Margem Crítica Menor de Ataques rolados contra a DM aumentada em +1.",
            "Primeiro Ataque da Rodada ignora RM."),
    MAGISTRAL("Magistral", ItemRarity.RARE, 0, 0, 1, 0, 0,
            requirements(AttributeDomain.STRENGTH, 3, AttributeDomain.DEXTERITY, 3),
            "Dano Base da Arma aumenta em +1.", null),
    // The Característica Adicional's "Margem Crítica Menor +1" is real — resolveCriticalMarginIncrease.
    // The Favor's "Margem Crítica Maior +1" is real too — resolveMajorCriticalMarginIncrease.
    DECISIVA("Decisiva", ItemRarity.UNCOMMON, 0, 0, 0, 0, 0, requirements(AttributeDomain.DEXTERITY, 5),
            "Margem Crítica Maior +1.", "Margem Crítica Menor +1."),
    // TODO: an attack cannot be redirected to roll against the target's DM instead of their DF
    // (DefenseType is the caller's choice at the attack site, unreachable from an enhancement), the
    // damage an attack deals has no type an item can set, and Corte/Perfuração/Esmagamento is not a
    // breakdown DamageType has.
    DYOSPIROS("Material Especial - Dyospiros", ItemRarity.UNCOMMON, 0, 0, 0, 1, 0,
            requirements(AttributeDomain.FOCUS, 3),
            "Ataque feito contra a DM do alvo e causa danos mágicos.",
            "Esta arma é feita de madeira, danos Cortantes são substituídos por Esmagamento."),
    // TODO: needs a Monster Ability model and an activated masterpiece-Favor mechanism — the same
    // pair DefensiveMasterpiece#COURO_DE_MONSTRO waits on.
    OSSOS_DE_MONSTRO("Material Especial - Ossos de Monstro", ItemRarity.RARE, 0, 0, 0, 1, 0,
            requirements(AttributeDomain.GNOSE, 3),
            "Fornece uma Habilidade Monstruosa Deviante ou Predador, definida aleatoriamente na construção do item.",
            "Ativar o Favor 1PA e 3PM, Duração 2 Rodadas."),
    // TODO: no EffectChain exists, and the damage an attack deals carries no DamageDescriptor an
    // item can set (see DYOSPIROS).
    GELO_VERDADEIRO("Material Especial - Gelo Verdadeiro", ItemRarity.RARE, 0, 0, 1, 0, 0,
            requirements(AttributeDomain.STRENGTH, 3),
            "Corrente de Efeitos – Gelo Verdadeiro: Dano +3.",
            "Dano causado é Físico Elemental: Gelo."),
    // Both halves are real now: the Favor's "Danos Críticos +3" through resolveCriticalDamage, the
    // Característica Adicional's "Margem Crítica Menor +1" through resolveCriticalMarginIncrease.
    MITRAL("Material Especial - Mitral", ItemRarity.EPIC, 0, 0, 1, 0, 0,
            requirements(AttributeDomain.DEXTERITY, 3),
            "Danos Críticos aumentam em +3.", "Margem Crítica Menor +1."),
    // TODO: no EffectChain exists; turning one delivered attack into an Área de Efeito needs the
    // footprint resolution CLAUDE.md's "Área de Efeito" row describes; and the damage type is
    // DYOSPIROS' gap again.
    DENTE_DE_DRAGAO("Material Especial - Dente de Dragão", ItemRarity.MYTHIC, 0, 0, 1, 1, 1,
            requirements(AttributeDomain.DEXTERITY, 3),
            "Corrente de Efeitos – Fúria Dracônica: Área de Efeito – Explosão.",
            "Dano causado é Físico Elemental."),
    // The Característica Adicional's "Dano Base +1" is real and unconditional (an Adicional, not a
    // Favor, so no Requisitos gate it) — the one constant here whose scale-up isn't Favor-gated.
    // TODO: the Favor's Corrente de Efeitos is not — no EffectChain exists.
    ADAMANTINA("Material Especial - Adamantina", ItemRarity.EPIC, 0, 0, 0, 1, 0,
            requirements(AttributeDomain.DEXTERITY, 3),
            "Corrente de Efeitos - Ataque Meteórico: Dano Físico +3.", "Dano Base +1."),
    // TODO: needs item agreement state and a Subordinado model — the same pair
    // DefensiveMasterpiece#ESPIRITO_UMBRAL waits on, plus the granted/withheld "+1d6" this core
    // cannot express (it grants no dice).
    ESPIRITO_UMBRAL("Material Especial - Espírito Umbral", ItemRarity.MYTHIC, 0, 0, 1, 0, 1,
            requirements(AttributeDomain.CHARISMA, 5),
            "Apenas quando em acordo, a arma conta como um Subordinado de tipo determinado em sua confecção.",
            "Este item está vivo e é senciente, pode se comunicar telepaticamente com seu usuário e costuma ajudar seu portador quando seus objetivos estão alinhados.");

    /** Mitral's Favor: "Danos Críticos aumentam em +3". */
    private static final int MITRAL_CRITICAL_DAMAGE_BONUS = 3;

    private final String name;
    private final ItemRarity rarity;
    private final int physicalDefenseBonus;
    private final int magicDefenseBonus;
    private final int attackBonus;
    private final int damageBonus;
    private final int castingBonus;
    private final MasterpieceRequirements requirements;
    private final String favorDescription;
    private final String additionalEffects;

    OffensiveMasterpiece(final String name, final ItemRarity rarity, final int physicalDefenseBonus,
                         final int magicDefenseBonus, final int attackBonus, final int damageBonus,
                         final int castingBonus, final MasterpieceRequirements requirements,
                         final String favorDescription, final String additionalEffects) {
        this.name = name;
        this.rarity = rarity;
        this.physicalDefenseBonus = physicalDefenseBonus;
        this.magicDefenseBonus = magicDefenseBonus;
        this.attackBonus = attackBonus;
        this.damageBonus = damageBonus;
        this.castingBonus = castingBonus;
        this.requirements = requirements;
        this.favorDescription = favorDescription;
        this.additionalEffects = additionalEffects;
    }

    @Override
    public String getDescription() {
        return favorDescription;
    }

    /** Every entry here is fitted to a weapon, so all of them price off the Armas column. */
    @Override
    public EnhancementPriceCategory getPriceCategory() {
        return EnhancementPriceCategory.WEAPON;
    }

    /**
     * "Dano Base da Arma aumenta em +1", on the three constants that say it. Favor-gated on
     * {@link #getRequirements()} for Brutal and Magistral; ungated for Adamantina, whose scale-up
     * is a Característica Adicional rather than a Favor.
     *
     * <p>Only ever asked about the weapon this Obra-Prima is fitted to — {@code
     * Item#resolveEnhancementDamageBaseIncrease} refuses to let an offensive enhancement raise the
     * Dano Base of a <em>different</em> weapon in the same loadout, which is what "da Arma" means.
     */
    @Override
    public int resolveDamageBaseIncrease(final Weapon weapon, final Character character) {
        if (this == ADAMANTINA) {
            return 1;
        }
        return (this == BRUTAL || this == MAGISTRAL) && requirements.isMetBy(character) ? 1 : 0;
    }

    /**
     * "Margem Crítica Menor +1", on the two constants that say it — each lowering by one the 3d6
     * total an Acerto Crítico Menor with this weapon has to reach ({@code SkillRoll
     * #getCriticalResult(int, int)}).
     *
     * <p><b>Ungated.</b> On both Decisiva and Mitral the clause is the Característica Adicional,
     * not the Favor, so it applies to whoever swings the weapon — the same split {@link
     * #resolveDamageBaseIncrease} makes for Adamantina. What each one's <em>Favor</em> says is a
     * different figure and is not this: Decisiva's is a Margem Crítica <b>Maior</b> and Mitral's is
     * its crit dano (see {@link #resolveCriticalDamage}).
     *
     * <p>Only ever asked about the weapon this Obra-Prima is fitted to, enforced by {@code
     * Item#resolveEnhancementCriticalMarginIncrease}.
     */
    @Override
    public int resolveCriticalMarginIncrease(final Weapon weapon, final Character character) {
        return this == DECISIVA || this == MITRAL ? 1 : 0;
    }

    /**
     * Decisiva's Favor, "Margem Crítica Maior +1" — a Favor, so gated on {@link #getRequirements()}
     * (Destreza 5), unlike its ungated Menor Característica Adicional above.
     */
    @Override
    public int resolveMajorCriticalMarginIncrease(final Weapon weapon, final Character character) {
        return this == DECISIVA && requirements.isMetBy(character) ? 1 : 0;
    }

    /**
     * Mitral's "Danos Críticos aumentam em +3" — a Favor, so it is gated on {@link
     * #getRequirements()}, unlike the Margem Crítica Menor clause above.
     */
    // TODO: Banhada em Prata's and Banhada em Ouro's Margem Crítica Menor clauses are still
    //  unexpressible: one needs the attack target's Monstro classification and the other which
    //  Defesa the attack was rolled against, and an enhancement hook is handed neither.
    @Override
    public CriticalDamage resolveCriticalDamage(final Weapon weapon, final Character character) {
        return this == MITRAL && requirements.isMetBy(character)
                ? CriticalDamage.ofFlat(MITRAL_CRITICAL_DAMAGE_BONUS)
                : CriticalDamage.NONE;
    }

    @Override
    public int resolveBonus(final ModifierType modifierType, final org.aventyrs.core.skill.SkillType skillType,
                            final Character character) {
        if (!requirements.isMetBy(character)) {
            return 0;
        }
        if (this == DEFENSORA && modifierType == ModifierType.DAMAGE_REDUCTION) {
            return 1;
        }
        return 0;
    }

    /**
     * Poderosa's "Magias tem … Duração +1" — a Característica Adicional, so it applies to every
     * Magia its wielder casts and is not gated on the Requisitos the Favor is.
     */
    @Override
    public int resolveDurationIncreaseInRounds(final Spell spell, final Character character) {
        return this == PODEROSA ? 1 : 0;
    }

    private static MasterpieceRequirements requirements(final AttributeDomain domain, final int value) {
        return requirements(domain, value, null, 0);
    }

    private static MasterpieceRequirements requirements(final AttributeDomain firstDomain, final int firstValue,
                                                        final AttributeDomain secondDomain, final int secondValue) {
        List<ItemRequirements> requirements = secondDomain == null
                ? List.of(new ItemRequirements(firstDomain, firstValue))
                : List.of(new ItemRequirements(firstDomain, firstValue),
                        new ItemRequirements(secondDomain, secondValue));
        return new MasterpieceRequirements(requirements);
    }
}
