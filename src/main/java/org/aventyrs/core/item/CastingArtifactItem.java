package org.aventyrs.core.item;

import lombok.Getter;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.effect.FuturoPrevisto;
import org.aventyrs.core.effect.MarcasDaGuerra;
import org.aventyrs.core.effect.MarcasDaProtecao;
import org.aventyrs.core.effect.ToqueDivino;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillType;

/**
 * The Artefatos de Conjuração catalog — {@link ItemCategory#CASTING_ARTIFACT}, one constant per entry of {@code
 * docs/rules/equipamentos.txt}'s "Artefatos de Conjuração" (filed under Armas). Every one is a {@link Weapon} so it
 * can be <b>drawn</b>: an artefact helps only while it is in hand ("utilizando" = drawn, the core's convention), and
 * every clause it carries — Favor <em>and</em> Efeitos Adicionais, per the item convention — holds only while its
 * wielder meets its Requisitos ({@link Item#grantsFavorTo(Character)}). Three are pure foci with no attack (Bola de
 * Crista, Grimório, Varinha): {@link #canAttack()} is false.
 *
 * <p>What each one does is read by {@link CastingArtifacts} (the scan over the drawn ones) from the hooks below, and
 * reaches {@code SpellCastingServiceImpl} (Vantagem, PM, Tempo de Conjuração, damage/heal, post-cast Defesas, the
 * Correntes), {@code CriticalServiceImpl} (Margem Crítica das Magias) and {@code AttackRangeServiceImpl} (Bola de
 * Crista's reach). The three "1PA, 2PM, 3 Rodadas" buffs are {@link ArtifactActiveAbility}, offered while the
 * artefact is drawn.
 */
@Getter
public enum CastingArtifactItem implements ItemTemplate, Weapon {

    /**
     * Adaga de Cerimônias (Leve/Incomum) — Preço 12. 1d6+1 Perfurante, Primor (17), Adjacente, Des 3/Foc 3. Its Efeito
     * Ativo (+1 Dano Base, +2 under its Favor) is {@link ArtifactActiveAbility#ADAGA_DE_CERIMONIAS}.
     */
    ADAGA_DE_CERIMONIAS("Adaga de Cerimônias", ItemWeightClass.LIGHT, ItemRarity.UNCOMMON, 12,
            DamageBase.of(1, 1), CriticalEffectType.PRIMOR, 17, Range.ADJACENTE,
            new ItemRequirements(AttributeDomain.DEXTERITY, 3, AttributeDomain.FOCUS),
            "Bônus do efeito ativo muda para +2.",
            "Com o Tempo de Ação de 1PA, o usuário pode utilizar 2PM para aumentar o Dano Base dos Ataques Físicos, "
                    + "Danos Mágicos e Curas Mágicas em +1 por 3 Rodadas (efeito não cumulativo).") {
        @Override
        public ActiveAbility getArtifactAbility() {
            return ArtifactActiveAbility.ADAGA_DE_CERIMONIAS;
        }
    },

    /**
     * Báculo (Leve/Incomum) — Preço 12. 1d6+1 Esmagamento, Atordoante (17), Adjacente, Foco 3. Vantagem on casting,
     * the Toque Divino Corrente, and Margem Crítica das Magias +1.
     */
    BACULO("Báculo", ItemWeightClass.LIGHT, ItemRarity.UNCOMMON, 12,
            DamageBase.of(1, 1), CriticalEffectType.ATORDOANTE, 17, Range.ADJACENTE,
            new ItemRequirements(AttributeDomain.FOCUS, 3),
            "Vantagem nas Rolagens de Perícias para Conjuração de Magias. Magias conjuradas recebem a Corrente de "
                    + "Efeitos – Toque Divino: Efeitos de danos e curas desta sua magia aumentam em +2.",
            "Margem Crítica Menor das Magias que conjurar aumentam em +1.") {
        @Override
        public boolean grantsCastingAdvantage() {
            return true;
        }

        @Override
        public int getSpellCriticalMarginIncrease() {
            return 1;
        }

        @Override
        public EffectChain createSpellEffectChain(final CombatantSheet caster, final Spell spell,
                                                  final boolean hostile) {
            return new ToqueDivino(hostile);
        }
    },

    /**
     * Bisarma Canalizadora (Pesado/Épico) — Preço 20. 2d6+1 Perfurante, Primor (16), Muito Curto, For 3/Foc 3, both
     * hands. Its Efeito Ativo (Vantagem on attacks and casting) is {@link ArtifactActiveAbility#BISARMA_CANALIZADORA}.
     * TODO: its Favor "recebe Alcance Estendido como Aprimoramento de Obra-Prima adicional" while active — no
     * equipment range pass exists (the Alcance Estendido Aprimoramento's own TODO).
     */
    BISARMA_CANALIZADORA("Bisarma Canalizadora", ItemWeightClass.HEAVY, ItemRarity.EPIC, 20,
            DamageBase.of(2, 1), CriticalEffectType.PRIMOR, 16, Range.DISTANCIA_MUITO_CURTA,
            new ItemRequirements(AttributeDomain.STRENGTH, 3, AttributeDomain.FOCUS),
            "Enquanto efeito estiver ativo recebe Alcance Estendido como Aprimoramento de Obra-Prima adicional.",
            "Exige uso de ambas as mãos para atacar. Com o Tempo de Ação de 1PA, o usuário pode utilizar 2PM para "
                    + "receber Vantagem em suas rolagens de Perícias para ataques e conjurações por 3 Rodadas (efeito "
                    + "não cumulativo).") {
        @Override
        public ActiveAbility getArtifactAbility() {
            return ArtifactActiveAbility.BISARMA_CANALIZADORA;
        }
    },

    /**
     * Bola de Cristal ("Bola de Crista" in the source) (Leve/Épico) — Preço 20, Foco 4, no attack. Vantagem on
     * casting, the Futuro Previsto Corrente, +1UD to Toque reach and one step to Distância reach, and no sum-triggered
     * Efeito Crítico Menor on its holder's casting rolls.
     */
    BOLA_DE_CRISTA("Bola de Crista", ItemWeightClass.LIGHT, ItemRarity.EPIC, 20,
            null, null, Weapon.DEFAULT_LESSER_CRITICAL_MARGIN, Range.ADJACENTE,
            new ItemRequirements(AttributeDomain.FOCUS, 4),
            "Vantagem nas Rolagens de Perícias para Conjuração de Magias. Magias conjuradas recebem a Corrente de "
                    + "Efeitos – Futuro Previsto: Esta Magia aplica Efeitos Críticos Menores aos alvos.",
            "Alcance de suas Magias de Toque aumentam em +1UD, Alcance de suas Magias à Distância aumentam em 1 passo. "
                    + "Magias conjuradas não mais desencadeiam Efeitos Críticos Menores em função da soma dos dados em "
                    + "suas rolagens de Perícias.") {
        @Override
        public boolean grantsCastingAdvantage() {
            return true;
        }

        @Override
        public int getTouchReachIncreaseUd() {
            return 1;
        }

        @Override
        public int getSpellRangeSteps() {
            return 1;
        }

        @Override
        public boolean suppressesSumMinorCriticals() {
            return true;
        }

        @Override
        public EffectChain createSpellEffectChain(final CombatantSheet caster, final Spell spell,
                                                  final boolean hostile) {
            return new FuturoPrevisto(caster, spell);
        }
    },

    /**
     * Cajado do Amparo (Médio/Comum) — Preço 14. 1d6+1 Esmagamento, Atordoante (17), Adjacente, Foco 3. Defesas +1
     * for 1 Rodada after casting, raised to +3 by its Marcas da Proteção Corrente (never on a Semente).
     */
    CAJADO_DO_AMPARO("Cajado do Amparo", ItemWeightClass.MEDIUM, ItemRarity.COMMON, 14,
            DamageBase.of(1, 1), CriticalEffectType.ATORDOANTE, 17, Range.ADJACENTE,
            new ItemRequirements(AttributeDomain.FOCUS, 3),
            "Magias conjuradas recebem a Corrente de Efeitos – Marcas da Proteção: Bônus nas Defesas concedido muda "
                    + "para +3. Esta Corrente de Efeitos não desencadeia durante Conjuração de Magias do tipo Semente.",
            "Fornece Bônus de +1 em suas Defesas por 1 Rodada após conjurar uma Magia.") {
        @Override
        public int getCastDefesasBonus() {
            return 1;
        }

        @Override
        public EffectChain createSpellEffectChain(final CombatantSheet caster, final Spell spell,
                                                  final boolean hostile) {
            return spell.getBranchLevel() == org.aventyrs.core.magic.BranchLevel.SEMENTE
                    ? null : new MarcasDaProtecao(caster);
        }
    },

    /**
     * Cajado de Guerra (Médio/Raro) — Preço 26. 1d6+3 Esmagamento, Atordoante (17), Adjacente, For 3/Foc 3. Margem
     * Crítica das Magias +2, damage and healing +1, Defesas +1 for 1 Rodada after casting; its Marcas da Guerra
     * Corrente raises both to +2.
     */
    CAJADO_DE_GUERRA("Cajado de Guerra", ItemWeightClass.MEDIUM, ItemRarity.RARE, 26,
            DamageBase.of(1, 3), CriticalEffectType.ATORDOANTE, 17, Range.ADJACENTE,
            new ItemRequirements(AttributeDomain.STRENGTH, 3, AttributeDomain.FOCUS),
            "Margem Crítica Menor das Magias que conjurar aumentam em +2. Magias conjuradas recebem a Corrente de "
                    + "Efeitos – Marcas da Guerra: Os Bônus em Efeitos de danos e curas de suas Magias e em suas Defesas "
                    + "mudam para +2.",
            "Efeitos de danos e curas magicas aumentam em +1. Concede Bônus de +1 em suas Defesas por 1 Rodada após "
                    + "conjurar uma Magia.") {
        @Override
        public int getSpellCriticalMarginIncrease() {
            return 2;
        }

        @Override
        public int getSpellEffectBonus() {
            return 1;
        }

        @Override
        public int getCastDefesasBonus() {
            return 1;
        }

        @Override
        public EffectChain createSpellEffectChain(final CombatantSheet caster, final Spell spell,
                                                  final boolean hostile) {
            return new MarcasDaGuerra(caster, hostile);
        }
    },

    /**
     * Gládio Canalizador (Médio/Raro) — Preço 16. 1d6+3 Cortante, Primor (17), Adjacente, For 3/Foc 3. Its Efeito Ativo
     * (Margem Crítica +2 on attacks and Magias, +3 under its Favor) is {@link ArtifactActiveAbility#GLADIO_CANALIZADOR}.
     */
    GLADIO_CANALIZADOR("Gládio Canalizador", ItemWeightClass.MEDIUM, ItemRarity.RARE, 16,
            DamageBase.of(1, 3), CriticalEffectType.PRIMOR, 17, Range.ADJACENTE,
            new ItemRequirements(AttributeDomain.STRENGTH, 3, AttributeDomain.FOCUS),
            "Bônus na Margem Crítica Menor do efeito ativo muda para +3.",
            "Com o Tempo de Ação de 1PA, o usuário pode utilizar 2PM para aumentar a Margem Crítica Menor de Ataques e "
                    + "Magias em +2 por 3 Rodadas (efeito não cumulativo).") {
        @Override
        public ActiveAbility getArtifactAbility() {
            return ArtifactActiveAbility.GLADIO_CANALIZADOR;
        }
    },

    /**
     * Grimório (Leve/Comum) — Preço 6, Foco 4, no attack (its stray "Atordoante (17)" is a source defect). Margem
     * Crítica das Magias +2, Otimização Metamágica (a critical Magia's Efeitos Críticos trigger twice), and the first
     * Magia of each Rodada costs 1PA less.
     */
    GRIMORIO("Grimório", ItemWeightClass.LIGHT, ItemRarity.COMMON, 6,
            null, null, Weapon.DEFAULT_LESSER_CRITICAL_MARGIN, Range.ADJACENTE,
            new ItemRequirements(AttributeDomain.FOCUS, 4),
            "Margem Crítica Menor das Magias que conjurar aumentam em +2. Rolagens Críticas de Magias recebem a "
                    + "Corrente de Efeitos – Otimização Metamágica: Os Efeitos Críticos desta magia desencadeiam duas vezes.",
            "Tempo de Conjuração da primeira Magia que conjurar a cada Rodada é reduzido em -1PA.") {
        @Override
        public int getSpellCriticalMarginIncrease() {
            return 2;
        }

        @Override
        public boolean doublesSpellCriticalEffects() {
            return true;
        }

        @Override
        public int getFirstSpellActivationReduction() {
            return 1;
        }
    },

    /**
     * Varinha (Leve/Comum) — Preço 10, Foco 3, no attack. Margem Crítica das Magias +1 (Favor) +1 (Efeito Adicional),
     * Vantagem on casting, and the first Magia of each Rodada costs 1PM less ("Custos de Bônus Bases", minimum 1).
     */
    VARINHA("Varinha", ItemWeightClass.LIGHT, ItemRarity.COMMON, 10,
            null, null, Weapon.DEFAULT_LESSER_CRITICAL_MARGIN, Range.ADJACENTE,
            new ItemRequirements(AttributeDomain.FOCUS, 3),
            "Margem Crítica Menor das Magias que conjurar aumentam em +1. Custos de Bônus Bases usados para conjurar "
                    + "a primeira magia de cada Rodada são reduzidos em -1 (até o mínimo de 1 Bônus Base).",
            "Margem Crítica Menor das Magias que conjurar aumentam em +1. Vantagem nas rolagens de Perícias para "
                    + "conjurar Magias.") {
        @Override
        public boolean grantsCastingAdvantage() {
            return true;
        }

        @Override
        public int getSpellCriticalMarginIncrease() {
            return 2;
        }

        @Override
        public int getFirstSpellManaReduction() {
            return 1;
        }
    };

    private final String name;
    private final ItemWeightClass weightClass;
    private final ItemRarity rarity;
    private final int price;
    private final DamageBase authoredDamageBase;
    private final CriticalEffectType criticalEffect;
    private final int lesserCriticalMargin;
    private final Range range;
    private final ItemFavor favor;

    CastingArtifactItem(final String name, final ItemWeightClass weightClass, final ItemRarity rarity,
                        final int price, final DamageBase damageBase, final CriticalEffectType criticalEffect,
                        final int lesserCriticalMargin, final Range range, final ItemRequirements requirements,
                        final String favorText, final String additionalEffects) {
        this.name = name;
        this.weightClass = weightClass;
        this.rarity = rarity;
        this.price = price;
        this.authoredDamageBase = damageBase;
        this.criticalEffect = criticalEffect;
        this.lesserCriticalMargin = lesserCriticalMargin;
        this.range = range;
        this.favor = ItemFavor.builder()
                .description(favorText)
                .requirements(requirements)
                .additionalEffects(additionalEffects)
                .build();
    }

    /** The artefact a drawn copy was made from, or {@code null} for any other item. */
    public static CastingArtifactItem of(final Item item) {
        if (item instanceof CastingArtifactItem template) {
            return template;
        }
        return item instanceof AbstractItem copy && copy.getTemplate() instanceof CastingArtifactItem template
                ? template : null;
    }

    // --- what an artefact does while drawn (read by CastingArtifacts) ---------------------------

    /** "Vantagem nas Rolagens de Perícias para Conjuração de Magias". */
    public boolean grantsCastingAdvantage() {
        return false;
    }

    /** "Margem Crítica Menor das Magias que conjurar aumentam em +N" — summed on the Domínio do Mana roll. */
    public int getSpellCriticalMarginIncrease() {
        return 0;
    }

    /** "Efeitos de danos e curas mágicas aumentam em +N" — Cajado de Guerra's +1. */
    public int getSpellEffectBonus() {
        return 0;
    }

    /** "Bônus de +N em suas Defesas por 1 Rodada após conjurar uma Magia" — both Cajados. */
    public int getCastDefesasBonus() {
        return 0;
    }

    /** "Tempo de Conjuração da primeira Magia … a cada Rodada é reduzido em -NPA" — Grimório. */
    public int getFirstSpellActivationReduction() {
        return 0;
    }

    /** "Custos de Bônus Bases … da primeira magia de cada Rodada … reduzidos em -N" — Varinha. */
    public int getFirstSpellManaReduction() {
        return 0;
    }

    /** "Os Efeitos Críticos desta magia desencadeiam duas vezes" on a critical casting roll — Grimório. */
    public boolean doublesSpellCriticalEffects() {
        return false;
    }

    /** Bola de Crista: "Alcance de suas Magias de Toque aumentam em +1UD" — reported, a Toque has no band. */
    public int getTouchReachIncreaseUd() {
        return 0;
    }

    /** Bola de Crista: "Alcance de suas Magias à Distância aumentam em 1 passo". */
    public int getSpellRangeSteps() {
        return 0;
    }

    /** Bola de Crista: no Efeito Crítico Menor "em função da soma dos dados" on its holder's casting rolls. */
    public boolean suppressesSumMinorCriticals() {
        return false;
    }

    /**
     * The Corrente de Efeitos a Magia cast with this artefact drawn receives — Toque Divino, Futuro Previsto, Marcas
     * da Proteção, Marcas da Guerra — built for that one cast, or {@code null}. hostile is whether the Magia lands on
     * an enemy (its damage half) rather than an ally (its healing half).
     */
    public EffectChain createSpellEffectChain(final CombatantSheet caster, final Spell spell, final boolean hostile) {
        return null;
    }

    /** The "1PA, 2PM, 3 Rodadas" Efeito Ativo this artefact offers while drawn, or {@code null}. */
    public ActiveAbility getArtifactAbility() {
        return null;
    }

    // --- Item / Weapon columns ------------------------------------------------------------------

    /** Whether this artefact can be swung at all — false for the three pure foci (Bola, Grimório, Varinha). */
    @Override
    public boolean canAttack() {
        return authoredDamageBase != null;
    }

    /** The printed Dano; {@link DamageBase#UNARMED} for a pure focus, which never attacks anyway. */
    @Override
    public DamageBase getDamageBase() {
        return authoredDamageBase == null ? DamageBase.UNARMED : authoredDamageBase;
    }

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_CORPO_A_CORPO;
    }

    @Override
    public String getDescription() {
        return "";
    }

    @Override
    public ItemCategory getCategory() {
        return ItemCategory.CASTING_ARTIFACT;
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

    /** Whether character currently wields this artefact's power — drawn, intact, and meeting its Requisitos. */
    public boolean empowers(final Item copy, final Character character) {
        return copy != null && copy.grantsFavorTo(character);
    }
}
