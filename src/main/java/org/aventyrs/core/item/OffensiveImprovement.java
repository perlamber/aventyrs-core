package org.aventyrs.core.item;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CriticalDamage;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;

import lombok.Getter;

/**
 * The catalog of Aprimoramentos that can be fitted to an offensive item — the "Aprimoramentos de
 * Obras-Primas Ofensivas" list of {@code docs/rules/equipamentos.txt}, the mirror of {@link
 * DefensiveImprovement}.
 *
 * <p><b>The name alone is not a key.</b> Oculta, Encaixe, Benção de Proteção/Elemental/Elduriana/
 * Vulcana/Ymiriana and both Maldições are printed in <em>both</em> Aprimoramento lists with
 * different Favor text and columns; the list a constant appears under is what identifies it. This
 * enum and {@link DefensiveImprovement} therefore share nine names and agree on almost nothing.
 *
 * <p><b>No Requisitos column.</b> The Aprimoramento tables print {@code DF | DM | Ataque | Danos |
 * Conjuração} and stop — unlike an Obra-Prima, an Aprimoramento's Favor is never gated on an
 * Atributo, so nothing here takes a {@link MasterpieceRequirements} and every effect below is
 * unconditional.
 *
 * <p><b>What is live.</b> Guarda Mãos' and Benção de Proteção's DF/DM reach {@code DefenseService};
 * Oculta's Vantagem reaches the Furtividade roll; Benção Ymiriana's "Dano Base da Arma aumenta em
 * +1" reaches {@code DamageBaseService}; Cruel's "Danos Críticos" reaches a crit's dano roll
 * ({@link Improvement#resolveCriticalDamage}); Manopla de Segurança's "Não pode ser desarmado" is what
 * {@code Weapon#isDisarmable()} has been waiting for; Encaixe is what lets {@code
 * AbstractItem#setPowerStone} socket a Pedra do Poder into a <em>weapon</em>; and every constant
 * prices off the Armas column ({@link EnhancementPriceCategory#WEAPON}).
 *
 * <p>The Ataque and Danos columns are authored and read by nothing — see {@link
 * OffensiveMasterpiece}'s javadoc for why, and for the weapon-scoped roll pass they wait on.
 */
@Getter
public enum OffensiveImprovement implements Improvement {
    OCULTA("Oculta", ItemRarity.UNCOMMON, 0, 0, 0, 0, 0,
            "Vantagem em rolagens de Furtividade.", "Item tem aparência de item comum.") {
        @Override
        public int resolveBonus(final ModifierType modifierType, final SkillType skillType,
                                final Character character) {
            return skillType == SkillType.FURTIVIDADE && modifierType == skillType.getRollBonusType()
                    ? Skill.ADVANTAGE_BONUS : 0;
        }
    },
    // TODO: a Magia carries no numeric damage/healing effect for an item to raise — the same column
    // DefensiveMasterpiece#BANHADA_EM_OURO and OffensiveMasterpiece#PODEROSA are blocked on.
    ARCANISTA("Arcanista", ItemRarity.RARE, 0, 0, 0, 0, 1,
            "Cura e Danos Mágicos +1.", null),
    CRUEL("Cruel", ItemRarity.COMMON, 0, 0, 0, 0, 0,
            "Bônus em Danos Críticos muda para +3.", "Danos Críticos +2.") {
        /**
         * <b>+3, not +2+3.</b> The Favor "muda para" its own Característica Adicional rather than
         * stacking with it — the same net-effect reading {@code OffensiveMasterpiece#PRECISA}'s two
         * clauses got — and an Aprimoramento's Favor has no Requisitos column to gate it (see
         * {@link #OCULTA}, granted just as unconditionally), so the higher figure is simply what
         * this one is worth.
         *
         * <p>Only ever asked about the weapon it is fitted to: {@code
         * Item#resolveEnhancementCriticalDamage} enforces that, as it does for Dano Base.
         */
        @Override
        public CriticalDamage resolveCriticalDamage(final Weapon weapon, final Character character) {
            return CriticalDamage.ofFlat(3);
        }
    },
    // The enabler for a Pedra do Poder on a weapon — the offensive twin of
    // DefensiveImprovement#ENCAIXE, and what AbstractItem#setPowerStone checks for an offensive
    // host. A stone in a weapon selects its Efeito Ofensivo (PowerStone#resolveBonus reads
    // Item#getType()), which until now nothing could reach.
    ENCAIXE("Encaixe", ItemRarity.EPIC, 0, 0, 0, 0, 0,
            null, "Permite encaixe de Pedra do Poder."),
    GUARDA_MAOS("Guarda Mãos", ItemRarity.COMMON, 1, 0, 0, 0, 0, null, null),
    // Unlike its defensive namesake this one is the bare column: the offensive list gives it no
    // Favor at all, so there is no first-Rodadas window and no damage-taken escalation here.
    BENCAO_DE_PROTECAO("Benção de Proteção", ItemRarity.UNCOMMON, 1, 1, 0, 0, 0, null, null),
    // TODO: the Adicional's "+1UD" cannot be expressed — AttackRangeService has no equipment pass,
    // and Feat#resolveAttackRangeIncrease counts whole Range *bands* while this names a UD amount,
    // so the existing hook is the wrong unit rather than merely unscanned. The Favor additionally
    // needs the distance to the target, which no enhancement hook is handed.
    ALCANCE_ESTENDIDO("Alcance Estendido", ItemRarity.RARE, 0, 0, 0, 0, 0,
            "Bônus de +1 em rolagens de Ataque e Dano alvos não adjacentes.",
            "Distância de Ataque aumenta +1UD."),
    // TODO: the damage an attack deals carries no DamageDescriptor an item can add an element to,
    // and a Magia-scoped Conjuração/Dano bonus has no hook. The element choice is therefore not
    // recorded either — a creation-time choice with nothing to consume it would be dead state (see
    // ItemImprovement, which exists only because the defensive catalog's choices *are* read).
    BENCAO_ELEMENTAL("Benção Elemental", ItemRarity.RARE, 0, 0, 0, 1, 0,
            "Bônus de +1 em Conjuração e Danos de Magia Elemental.",
            "Danos causados são Elemental (tipo escolhido na fabricação) em adicional aos seus tipos."),
    // TODO: nothing lets one weapon count as a second ItemCategory — Weapon#getSkillType() and
    // getCategory() are each a single authored column, and Character#treatsAsNaturalWeapon is the
    // only reclassification this core has (and it only reaches Armas Naturais).
    MACHADO_ANEXO("Machado Anexo", ItemRarity.UNCOMMON, 0, 0, 0, 0, 0,
            null, "Armas de Ataque à Distância são Machados."),
    // TODO: no EffectChain exists; raising a *target's* GD de Magias has no hook (a Magia's GD is
    // resolved from the caster, see SpellCastingService); and Roubo de Mana does not exist at all
    // (only Roubo de Vida is modelled — LifeStealService).
    BENCAO_ELDURIANA("Benção Elduriana", ItemRarity.EPIC, 0, 0, 0, 0, 0,
            "Corrente de Efeitos – Inquisição: GD de Magias do Alvo +2 níveis.", "Roubo de Mana 1."),
    // TODO: no mechanism bypasses a mitigation stage — DamageService always applies the target's
    // RD/RM, and RE has no constant at all (see CLAUDE.md's damage-type-scoped mitigation row).
    BENCAO_VULCANA("Benção Vulcana", ItemRarity.EPIC, 0, 0, 0, 0, 0,
            "Corrente de Efeitos – Ignora Resistências: Ignora RE e RM.", "Ignora RD do alvo."),
    BENCAO_YMIRIANA("Benção Ymiriana", ItemRarity.UNCOMMON, 0, 0, 0, 1, 0,
            "Dano Base da Arma aumenta em +1.", null) {
        /**
         * Only ever asked about the weapon this Aprimoramento is fitted to — {@code
         * Item#resolveEnhancementDamageBaseIncrease} refuses to let an offensive enhancement raise
         * a <em>different</em> weapon's Dano Base, which is what "da Arma" means. Ungated: an
         * Aprimoramento has no Requisitos column.
         */
        @Override
        public int resolveDamageBaseIncrease(final Weapon weapon, final Character character) {
            return 1;
        }
    },
    MANOPLA_DE_SEGURANCA("Manopla de Segurança", ItemRarity.COMMON, 0, 0, 1, 0, 0,
            null, "Não pode ser desarmado.") {
        @Override
        public boolean preventsDisarming() {
            return true;
        }
    },
    // TODO: no EffectChain exists, and this core grants no dice. The "exige uso de ambas as mãos"
    // half cannot be expressed either — CharacterSheet's two-hand budget *infers* handedness from
    // ItemWeightClass plus category rather than reading an authored column, so an Aprimoramento
    // cannot override it. "Apenas Armas Leves" is enforced by AbstractItem#addImprovement.
    CORRENTE_COM_PESO("Corrente com Peso", ItemRarity.UNCOMMON, 0, 0, 0, 0, 0,
            "Corrente de Efeitos – Corrente Atacante: Dano +1d6.",
            "Apenas Armas Leves e passa a exigir uso de ambas as mãos."),
    // TODO: munição is not tracked — no Weapon column counts shots, and nothing spends one.
    // "Apenas armas à Distância" is enforced by AbstractItem#addImprovement.
    PENTE_ALONGADO("Pente Alongado", ItemRarity.RARE, 0, 0, 0, 0, 0,
            "Quantidade de munições muda pra +3.", "Apenas armas à Distância, +2 munições."),
    // TODO: the same single-category limit MACHADO_ANEXO cites.
    BAIONETA("Baioneta", ItemRarity.COMMON, 0, 0, 0, 0, 0,
            null, "Armas de Ataque à Distância são Lanças."),
    // TODO: an item cannot *grant* Roubo de Vida. LifeStealService sums a sheet's active LifeSteal
    // effects plus Feat#resolveGrantedLifeSteal and has no equipment pass — and could not be given
    // a correct one here, since getTotalLifeSteal(character, sheet) is handed no weapon and would
    // grant this while its wielder swung something else.
    SOLVE_VIDAS("Maldição: Solve-Vidas", ItemRarity.MYTHIC, 0, 0, 1, 0, 0,
            "Roubo de Vida +1.", "Roubo de Vida 2."),
    // TODO: Roubo de Mana and Roubo de Determinação do not exist in this core at all — only Roubo
    // de Vida is modelled.
    LADRA_DO_AETHER("Maldição: Ladra do AEther", ItemRarity.MYTHIC, 0, 0, 1, 0, 0,
            "Roubo de Mana e Determinação +1.", "Roubo de Mana e Determinação 1.");

    private final String name;
    private final ItemRarity rarity;
    private final int physicalDefenseBonus;
    private final int magicDefenseBonus;
    private final int attackBonus;
    private final int damageBonus;
    private final int castingBonus;
    private final String favorDescription;
    private final String additionalEffects;

    OffensiveImprovement(final String name, final ItemRarity rarity, final int physicalDefenseBonus,
                         final int magicDefenseBonus, final int attackBonus, final int damageBonus,
                         final int castingBonus, final String favorDescription, final String additionalEffects) {
        this.name = name;
        this.rarity = rarity;
        this.physicalDefenseBonus = physicalDefenseBonus;
        this.magicDefenseBonus = magicDefenseBonus;
        this.attackBonus = attackBonus;
        this.damageBonus = damageBonus;
        this.castingBonus = castingBonus;
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
}
