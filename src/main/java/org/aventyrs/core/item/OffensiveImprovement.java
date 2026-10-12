package org.aventyrs.core.item;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CriticalDamage;
import org.aventyrs.core.effect.CorrenteAtacante;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.effect.IgnoraResistencias;
import org.aventyrs.core.effect.Inquisicao;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.util.DiceRoller;

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
 * <p><b>Every constant is live (core 0.1.6.9).</b> The DF/DM columns reach {@code DefenseService}; the
 * Ataque and Danos columns, and every clause naming "this weapon's" attacks, reach the attack through
 * {@link WeaponEnhancements} — always the host weapon the attack is made <em>with</em>, never a sheathed
 * one; the Conjuração column and the Magia clauses reach {@code SpellCastingService} while the host is
 * drawn. The per-constant hooks below are offensive-only on purpose: each answers for its host weapon,
 * which {@link Improvement}'s shared hooks are not handed. Every constant prices off the Armas column
 * ({@link EnhancementPriceCategory#WEAPON}).
 *
 * <p><b>"muda para" replaces, "+N" adds.</b> A Favor that says its figure "muda para" N supersedes its
 * own Característica Adicional (Cruel, Pente Alongado); one that says "+N" stacks on it (Solve-Vidas,
 * Ladra do AEther). Table rulings and readings of 2026-10-12 are noted on each constant.
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
    /** "Cura e Danos Mágicos +1" — every Magia cast while the host is drawn, read like an artefact's. */
    ARCANISTA("Arcanista", ItemRarity.RARE, 0, 0, 0, 0, 1,
            "Cura e Danos Mágicos +1.", null) {
        @Override
        public int resolveSpellDamageBonus(final Spell spell) {
            return 1;
        }

        @Override
        public int resolveSpellHealingBonus(final Spell spell) {
            return 1;
        }
    },
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
    // Encaixe (the Pedra do Poder socket) was removed by table ruling, 2026-10-12: any Obra-Prima
    // weapon now takes a stone — see Item#acceptsPowerStone.
    GUARDA_MAOS("Guarda Mãos", ItemRarity.COMMON, 1, 0, 0, 0, 0, null, null),
    // Unlike its defensive namesake this one is the bare column: the offensive list gives it no
    // Favor at all, so there is no first-Rodadas window and no damage-taken escalation here.
    BENCAO_DE_PROTECAO("Benção de Proteção", ItemRarity.UNCOMMON, 1, 1, 0, 0, 0, null, null),
    /**
     * "Distância de Ataque aumenta +1UD" rides the UD axis {@code AttackRangeService} already sums for
     * Talentos; the Favor's "+1 em rolagens de Ataque e Dano" needs a target that is not adjacent, judged
     * through the Scene's positions ({@code WeaponEnhancements#nonAdjacentBonus}).
     */
    ALCANCE_ESTENDIDO("Alcance Estendido", ItemRarity.RARE, 0, 0, 0, 0, 0,
            "Bônus de +1 em rolagens de Ataque e Dano alvos não adjacentes.",
            "Distância de Ataque aumenta +1UD.") {
        @Override
        public int getAttackReachIncreaseUd() {
            return 1;
        }

        @Override
        public int getNonAdjacentBonus() {
            return 1;
        }
    },
    /**
     * Fitted through {@link OffensiveItemImprovement}, which records the element chosen at fabrication.
     * By table ruling (2026-10-12) a hit with the host is <b>fully both types</b>: the target's immunity
     * to the element stops it, that element's RE is subtracted on top of RD, and a Fraqueza to it applies
     * ({@code DamageInteraction#withAdditionalElement}). A copy fitted bare (no element recorded) keeps the
     * columns and the Favor, and its Adicional does nothing. The Favor reaches any Magia whose damage is
     * elemental, ⚠️ whichever element — "Magia Elemental" names none.
     */
    BENCAO_ELEMENTAL("Benção Elemental", ItemRarity.RARE, 0, 0, 0, 1, 0,
            "Bônus de +1 em Conjuração e Danos de Magia Elemental.",
            "Danos causados são Elemental (tipo escolhido na fabricação) em adicional aos seus tipos.") {
        @Override
        public int resolveCastingRollBonus(final Spell spell) {
            return isElemental(spell) ? 1 : 0;
        }

        @Override
        public int resolveSpellDamageBonus(final Spell spell) {
            return isElemental(spell) ? 1 : 0;
        }

        @Override
        public boolean requiresElementalTypeChoice() {
            return true;
        }
    },
    /**
     * "Armas de Ataque à Distância são Machados" (table ruling, 2026-10-12): the host counts as a Machado
     * for every clause scoped to the kind of weapon, and can strike corpo-a-corpo through {@link
     * AttachedMeleeAttack}. ⚠️ This core has no Machado category — a Machado de Mão is a Lâmina Leve, a
     * Machado de Guerra a Lâmina Pesada — so an attached axe is read as the hand axe: {@link
     * ItemCategory#LIGHT_BLADE}.
     */
    MACHADO_ANEXO("Machado Anexo", ItemRarity.UNCOMMON, 0, 0, 0, 0, 0,
            null, "Armas de Ataque à Distância são Machados.") {
        @Override
        public ItemCategory getAdditionalCategory() {
            return ItemCategory.LIGHT_BLADE;
        }
    },
    /**
     * The Corrente leaves the target's Magias "+2 níveis" harder until the attacker's next Turn starts
     * (table ruling, 2026-10-12) — {@link Inquisicao}. "Roubo de Mana 1" mirrors the Roubo de
     * Determinação ruling: the target loses the PM and the attacker recovers what was taken.
     */
    BENCAO_ELDURIANA("Benção Elduriana", ItemRarity.EPIC, 0, 0, 0, 0, 0,
            "Corrente de Efeitos – Inquisição: GD de Magias do Alvo +2 níveis.", "Roubo de Mana 1.") {
        @Override
        public EffectChain createEffectChain(final CombatantSheet attacker, final DiceRoller roller) {
            return new Inquisicao(attacker);
        }

        @Override
        public int getManaSteal() {
            return 1;
        }
    },
    /**
     * "Ignora RD do alvo" on every hit with the host; the Corrente adds RE and RM to what that same hit
     * ignores ({@link IgnoraResistencias}) — ⚠️ a reading: a Corrente normally lands <em>after</em> the
     * damage, but one that names the hit's own resistances can only mean that hit. RA is never ignored.
     */
    BENCAO_VULCANA("Benção Vulcana", ItemRarity.EPIC, 0, 0, 0, 0, 0,
            "Corrente de Efeitos – Ignora Resistências: Ignora RE e RM.", "Ignora RD do alvo.") {
        @Override
        public EffectChain createEffectChain(final CombatantSheet attacker, final DiceRoller roller) {
            return new IgnoraResistencias();
        }

        @Override
        public boolean ignoresDamageReduction() {
            return true;
        }
    },
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
    /**
     * The Corrente's "+1d6" is thrown with the attack's own dice and lands after the hit's mitigation
     * ({@link CorrenteAtacante}, the Rugido precedent). "Apenas Armas Leves" is enforced by {@code
     * AbstractItem#addImprovement}; "passa a exigir uso de ambas as mãos" by {@link HandBudget}.
     */
    CORRENTE_COM_PESO("Corrente com Peso", ItemRarity.UNCOMMON, 0, 0, 0, 0, 0,
            "Corrente de Efeitos – Corrente Atacante: Dano +1d6.",
            "Apenas Armas Leves e passa a exigir uso de ambas as mãos.") {
        @Override
        public EffectChain createEffectChain(final CombatantSheet attacker, final DiceRoller roller) {
            return new CorrenteAtacante(roller);
        }

        @Override
        public boolean requiresBothHands() {
            return true;
        }
    },
    /**
     * "muda pra +3" replaces the Adicional's +2, read as the Besta's magazine ({@code
     * Weapon#getEffectiveMagazineCapacity}) — an Arco draws from the bag and has no magazine to widen.
     * "Apenas armas à Distância" is enforced by {@code AbstractItem#addImprovement}.
     */
    PENTE_ALONGADO("Pente Alongado", ItemRarity.RARE, 0, 0, 0, 0, 0,
            "Quantidade de munições muda pra +3.", "Apenas armas à Distância, +2 munições.") {
        @Override
        public int getMagazineCapacityIncrease() {
            return 3;
        }
    },
    /** As {@link #MACHADO_ANEXO}, with a Lança — {@link ItemCategory#SPEAR}. */
    BAIONETA("Baioneta", ItemRarity.COMMON, 0, 0, 0, 0, 0,
            null, "Armas de Ataque à Distância são Lanças.") {
        @Override
        public ItemCategory getAdditionalCategory() {
            return ItemCategory.SPEAR;
        }
    },
    /** "Roubo de Vida 2" and the Favor's "+1" stack: Roubo de Vida 3 on every hit with the host. */
    SOLVE_VIDAS("Maldição: Solve-Vidas", ItemRarity.MYTHIC, 0, 0, 1, 0, 0,
            "Roubo de Vida +1.", "Roubo de Vida 2.") {
        @Override
        public int getLifeSteal() {
            return 3;
        }
    },
    /** "Roubo de Mana e Determinação 1" plus the Favor's "+1": 2 of each on every hit with the host. */
    LADRA_DO_AETHER("Maldição: Ladra do AEther", ItemRarity.MYTHIC, 0, 0, 1, 0, 0,
            "Roubo de Mana e Determinação +1.", "Roubo de Mana e Determinação 1.") {
        @Override
        public int getManaSteal() {
            return 2;
        }

        @Override
        public int getDeterminationSteal() {
            return 2;
        }
    };

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

    @Override
    public OffensiveImprovement offensiveDefinition() {
        return this;
    }

    /** Whether fitting this one needs a creation-time element — {@link #BENCAO_ELEMENTAL} alone. */
    public boolean requiresElementalTypeChoice() {
        return false;
    }

    /** Roubo de Vida every hit with the host carries. */
    public int getLifeSteal() {
        return 0;
    }

    /** Roubo de Mana every hit with the host carries. */
    public int getManaSteal() {
        return 0;
    }

    /** Roubo de Determinação every hit with the host carries. */
    public int getDeterminationSteal() {
        return 0;
    }

    /** UD added to the host's Distância de Ataque. */
    public int getAttackReachIncreaseUd() {
        return 0;
    }

    /** Ataque and Dano bonus against a target the attacker is not adjacent to. */
    public int getNonAdjacentBonus() {
        return 0;
    }

    /** Whether every hit with the host ignores the target's RD. */
    public boolean ignoresDamageReduction() {
        return false;
    }

    /** The second category the host counts as — and strikes corpo-a-corpo as — or {@code null}. */
    public ItemCategory getAdditionalCategory() {
        return null;
    }

    /** Whether the host needs both hands once this is fitted. */
    public boolean requiresBothHands() {
        return false;
    }

    /** Extra rounds a host magazine holds; nothing for a weapon without one. */
    public int getMagazineCapacityIncrease() {
        return 0;
    }

    /**
     * The Corrente de Efeitos a hit with the host gains once it clears the Corrente threshold, or {@code null}.
     *
     * @param attacker who made the attack
     * @param roller   the attack's own dice, or {@code null} to report a die rather than throw it
     */
    public EffectChain createEffectChain(final CombatantSheet attacker, final DiceRoller roller) {
        return null;
    }

    /** What this adds to the Conjuração roll of spell while the host is drawn, beyond the Conjuração column. */
    public int resolveCastingRollBonus(final Spell spell) {
        return 0;
    }

    /** What this adds to spell's damage while the host is drawn. */
    public int resolveSpellDamageBonus(final Spell spell) {
        return 0;
    }

    /** What this adds to spell's healing while the host is drawn. */
    public int resolveSpellHealingBonus(final Spell spell) {
        return 0;
    }

    private static boolean isElemental(final Spell spell) {
        return spell != null && spell.getPrimaryDamage().map(damage -> damage.elementalType() != null).orElse(false);
    }
}
