package org.aventyrs.core.monster.summon;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.character.services.DevourServiceImpl;
import org.aventyrs.core.effect.DevorarInteiro;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.effect.InocularVeneno;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemRarity;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.SummonedMonsterTemplate;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.EnchantmentWard;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.monster.SkillDifficulty;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoSpecialization;
import org.aventyrs.core.skill.attention.AttentionSpecialization;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEApararSpecialization;
import org.aventyrs.core.skill.furtividade.FurtividadeSpecialization;
import org.aventyrs.core.util.DiceRoller;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * A creature of the ALIADOS DA NATUREZA tree (core 0.0.92) — {@link NatureSummonKind} names which, and this carries
 * what varies per invocation: the Conjurador's Graduação in Domínio do Mana, and for a Lacerto creature the {@link
 * LacertoPower}s rolled when the Magia was cast.
 *
 * <pre>{@code
 * MonsterSheet aliado = NatureSummon.of(NatureSummonKind.ALIADO_DA_NATUREZA).spawn(druid, gm);
 * MonsterSheet beast  = NatureSummon.invoked(NatureSummonKind.EXPERIMENTO_DE_LACERTO, roller).spawn(druid, gm);
 * }</pre>
 *
 * <p>The Zumbi shape ({@link Zumbi}): every tier is folded in at build time — Atributos by {@link
 * #getAttributeBases()}, Defesas by {@link #getPhysicalDefense()}, the attack GD by {@link #getSkillDifficulties()},
 * and the roll-facing clauses by one {@link NatureSummonAbility} and one {@link NatureSummonTrait}.
 *
 * <p><b>Its attack is an authored Arma Natural</b> ({@link #naturalAttack()}): the stat block's "Danos de Ataques" minus
 * the ½ Força and size terms the engine adds itself, so a summon's dano is rolled through the ordinary path. Bruto's
 * "+1d6" raises it by {@link #BRUTO_DAMAGE_SCALE_UPS} rows, exactly one die below the cap.
 *
 * <p>The Anciente's Bênçãos (a rolled per-Rodada heal, and the same amount to adjacent allies in even Rodadas) are
 * {@link #regenerationFlat} plus the caller's d6; the Lacerto creatures' Sopro and Aura Elemental are {@link #breath}
 * and {@link #aura} (core 0.0.97).
 */
@Getter
@Builder(toBuilder = true)
public class NatureSummon implements SummonedMonsterTemplate {

    /** "Se você possuir 4 ou mais Graduações em Domínio do Mana" — the Aliado's +5PV and Defesas +2. */
    public static final int HIT_POINTS_TIER = 4;

    /** "Se você possuir 7 ou mais Graduações em Domínio do Mana" — +2 Força and Destreza (and Vigor, for Lacerto's). */
    public static final int ENCANTAMENTO_TIER = 7;

    /** "Se você possuir 10 Graduações em Domínio do Mana" — the GD reduction. */
    public static final int DIFFICULTY_REDUCTION_TIER = 10;

    /** The Bônus Mágico (encantamento) to each Atributo at {@link #ENCANTAMENTO_TIER}. */
    public static final int ENCANTAMENTO_ATTRIBUTE = 2;

    /** The Aliado's "Defesas +2" at {@link #HIT_POINTS_TIER}. */
    public static final int ENCANTAMENTO_DEFESAS = 2;

    /** The Orgulho's Bruto: "Vigor +3". */
    public static final int BRUTO_VIGOR = 3;

    /** Bruto's "Dano aumenta +1d6" — one die is four rows of the Dano Base scale. */
    public static final int BRUTO_DAMAGE_SCALE_UPS = 4;

    @NonNull
    private final NatureSummonKind kind;

    /** The Graduação in Domínio do Mana of whoever invoked it; 0 when nobody did. */
    @Builder.Default
    private final int conjuradorManaGraduation = 0;

    /** Its rolled {@link LacertoPower}s — empty for a kind that has none. */
    @Builder.Default
    @NonNull
    private final List<LacertoPower> powers = List.of();

    /** kind with no powers — the Aliado, Predador and Anciente, or a Lacerto creature whose powers the caller sets. */
    public static NatureSummon of(@NonNull final NatureSummonKind kind) {
        return NatureSummon.builder().kind(kind).build();
    }

    /** kind as a Magia invokes it: a Lacerto creature's powers rolled now, on roller (table ruling, 2026-10-01). */
    public static NatureSummon invoked(@NonNull final NatureSummonKind kind, @NonNull final DiceRoller roller) {
        return NatureSummon.builder().kind(kind).powers(LacertoPower.roll(kind.getPowerCount(), roller)).build();
    }

    /**
     * The creature a persisted summon describes (core 0.0.95) — its kind, its Conjurador's Graduação and its rolled
     * powers by name, as an API stores them. A power this build does not know is skipped.
     */
    public static NatureSummon restore(@NonNull final String kind, final int conjuradorManaGraduation,
                                       final List<String> powers) {
        List<LacertoPower> known = new ArrayList<>();
        if (powers != null) {
            for (String power : powers) {
                for (LacertoPower candidate : LacertoPower.values()) {
                    if (candidate.name().equals(power)) {
                        known.add(candidate);
                    }
                }
            }
        }
        return NatureSummon.builder().kind(NatureSummonKind.valueOf(kind))
                .conjuradorManaGraduation(conjuradorManaGraduation).powers(known).build();
    }

    @Override
    public NatureSummon withConjurador(final int manaGraduation) {
        return toBuilder().conjuradorManaGraduation(manaGraduation).build();
    }

    @Override
    public String getName() {
        return kind.getDisplayName();
    }

    public boolean hasPower(final LacertoPower power) {
        return powers.contains(power);
    }

    @Override
    public Map<AttributeDomain, Integer> getAttributeBases() {
        Map<AttributeDomain, Integer> bases = kind.getAttributeBases();
        if (conjuradorManaGraduation >= ENCANTAMENTO_TIER) {
            switch (kind) {
                case ALIADO_DA_NATUREZA, PREDADOR_REGIONAL -> raise(bases, ENCANTAMENTO_ATTRIBUTE,
                        AttributeDomain.STRENGTH, AttributeDomain.DEXTERITY);
                case EXPERIMENTO_DE_LACERTO, ORGULHO_DE_LACERTO -> raise(bases, ENCANTAMENTO_ATTRIBUTE,
                        AttributeDomain.STRENGTH, AttributeDomain.DEXTERITY, AttributeDomain.VIGOR);
                default -> { }
            }
        }
        if (kind == NatureSummonKind.ORGULHO_DE_LACERTO && hasPower(LacertoPower.BRUTO)) {
            raise(bases, BRUTO_VIGOR, AttributeDomain.VIGOR);
        }
        return bases;
    }

    private static void raise(final Map<AttributeDomain, Integer> bases, final int amount,
                              final AttributeDomain... domains) {
        for (AttributeDomain domain : domains) {
            bases.merge(domain, amount, Integer::sum);
        }
    }

    @Override
    public Map<SkillType, Integer> getSkillGraduations() {
        return kind.getSkillGraduations();
    }

    @Override
    public Map<SkillType, List<SkillSpecialization>> getSkillSpecializations() {
        return Map.of(
                SkillType.ATAQUE_CORPO_A_CORPO, List.of(AtaqueCorpoACorpoSpecialization.PRIMAL),
                SkillType.ATTENTION, List.of(AttentionSpecialization.SENTIDOS_APURADOS),
                SkillType.ESQUIVA_E_APARAR, List.of(EsquivaEApararSpecialization.GUERREIRO_NATURAL),
                SkillType.FURTIVIDADE, List.of(FurtividadeSpecialization.MAESTRIA_DA_OCULTACAO));
    }

    @Override
    public List<SkillCompetencyAbility> getSkillCompetencyAbilities() {
        return List.of(new NatureSummonAbility(kind, powers, conjuradorManaGraduation));
    }

    @Override
    public List<AttributeAbility> getAttributeAbilities() {
        return List.of(new NatureSummonTrait(kind, powers));
    }

    @Override
    public List<Item> getEquipment() {
        return List.of(naturalAttack());
    }

    /** "Danos de Ataques", authored as an Arma Natural — see the class javadoc. */
    public AbstractWeapon naturalAttack() {
        int scaleUps = hasPower(LacertoPower.BRUTO) ? BRUTO_DAMAGE_SCALE_UPS : 0;
        return AbstractWeapon.builder()
                .name("Ataque Natural")
                .category(ItemCategory.NATURAL_WEAPON)
                .rarity(ItemRarity.NATURAL)
                .weightClass(ItemWeightClass.MEDIUM)
                .damageBase(kind.getDamageBase().scaledUp(scaleUps))
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .build();
    }

    @Override
    public SizeCategory getSizeCategory() {
        return kind.getSizeCategory();
    }

    @Override
    public int getPhysicalDefense() {
        return kind.getPhysicalDefense() + encantamentoDefesas();
    }

    @Override
    public int getMagicDefense() {
        return kind.getMagicDefense() + encantamentoDefesas();
    }

    private int encantamentoDefesas() {
        boolean aliado = kind == NatureSummonKind.ALIADO_DA_NATUREZA || kind == NatureSummonKind.PREDADOR_REGIONAL;
        return aliado && conjuradorManaGraduation >= HIT_POINTS_TIER ? ENCANTAMENTO_DEFESAS : 0;
    }

    /** No stat block states a GD; the catalogue default, as {@link Zumbi#getGeneralDifficulty()}. */
    @Override
    public SkillDifficulty getGeneralDifficulty() {
        return SkillDifficulty.of(DifficultyLevel.EASY, 0);
    }

    /** The Conjurador's attack bonus also raises the GD its attacks present to a defender — {@link Zumbi}'s reading. */
    @Override
    public Map<SkillType, SkillDifficulty> getSkillDifficulties() {
        SkillDifficulty attack = SkillDifficulty.of(DifficultyLevel.EASY, Math.max(0, conjuradorManaGraduation));
        Map<SkillType, SkillDifficulty> difficulties = new EnumMap<>(SkillType.class);
        for (SkillType skillType : SkillType.values()) {
            if (skillType.isAttackSkill()) {
                difficulties.put(skillType, attack);
            }
        }
        return difficulties;
    }

    @Override
    public int getActionPoints() {
        return kind.getActionPoints();
    }

    @Override
    public int getLifeMultiplier() {
        return kind.getLifeMultiplier();
    }

    /** An animal for the Aliado and Predador; an "animal monstruoso" or a "Monstro" for Lacerto's. */
    @Override
    public CreatureType getCreatureType() {
        return kind == NatureSummonKind.ALIADO_DA_NATUREZA || kind == NatureSummonKind.PREDADOR_REGIONAL
                ? CreatureType.ANIMAL
                : SummonedMonsterTemplate.super.getCreatureType();
    }

    /** Sopro Elemental: "3PA, Área de Efeito – Cone", Refrigeração 1 (core 0.0.97). */
    public static final int BREATH_ACTION_POINTS = 3;
    public static final int BREATH_COOLDOWN = 1;

    /**
     * Its Sopro Elemental, if it rolled one (core 0.0.97) — Experimento: "Cone de 4UD. 2d6+2 (1+ Metade do Vigor) de Dano
     * Mágico Elemental, reduzido em 2 a cada UD"; Orgulho: "Cone de 6UD. 3d6 de Dano, reduzido em 1 a cada UD" (⚠️ read
     * as the same Dano Mágico Elemental). self is the spawned creature, whose Vigor the Experimento's figure reads.
     */
    public java.util.Optional<ElementalDischarge> breath(final org.aventyrs.core.character.Character self) {
        if (!hasPower(LacertoPower.SOPRO_ELEMENTAL)) {
            return java.util.Optional.empty();
        }
        org.aventyrs.core.character.DamageDescriptor natural = new org.aventyrs.core.character.DamageDescriptor(
                org.aventyrs.core.character.DamageType.ELEMENTAL, org.aventyrs.core.magic.ElementalType.NATURAL);
        return java.util.Optional.of(kind == NatureSummonKind.ORGULHO_DE_LACERTO
                ? new ElementalDischarge(3, 0, 1, natural, 6, BREATH_ACTION_POINTS, BREATH_COOLDOWN)
                : new ElementalDischarge(2, 1 + vigorOf(self) / 2, 2, natural, 4, BREATH_ACTION_POINTS, BREATH_COOLDOWN));
    }

    /**
     * Its Aura Elemental, if it rolled one (core 0.0.97) — Experimento: "2 de Dano Físico Elemental a cada Rodada em alvos
     * adjacentes"; Orgulho: "1d6 de Dano Mágico Elemental".
     */
    public java.util.Optional<ElementalDischarge> aura() {
        if (!hasPower(LacertoPower.AURA_ELEMENTAL)) {
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(kind == NatureSummonKind.ORGULHO_DE_LACERTO
                ? new ElementalDischarge(1, 0, 0, new org.aventyrs.core.character.DamageDescriptor(
                        org.aventyrs.core.character.DamageType.ELEMENTAL, org.aventyrs.core.magic.ElementalType.NATURAL),
                        0, 0, 0)
                : new ElementalDischarge(0, 2, 0, new org.aventyrs.core.character.DamageDescriptor(
                        org.aventyrs.core.character.DamageType.FISICO_ELEMENTAL,
                        org.aventyrs.core.magic.ElementalType.NATURAL), 0, 0, 0));
    }

    /**
     * The Anciente's Benção da Regeneração (core 0.0.97): "Recuperam 1d6+5 (Metade do Vigor) PV no início de cada
     * Rodada" — the flat half, ½ of self's Vigor; the caller adds the d6. Its Benção Compartilhada heals adjacent allies
     * the same amount in even Rodadas. 0 for every other kind.
     */
    public int regenerationFlat(final org.aventyrs.core.character.Character self) {
        return kind == NatureSummonKind.ANCIENTE ? vigorOf(self) / 2 : 0;
    }

    private static int vigorOf(final org.aventyrs.core.character.Character self) {
        return self == null ? 0 : self.getEffectiveAttributeTotal(AttributeDomain.VIGOR);
    }

    /** Membros Múltiplos' paired attack — the Aliado's own, or a Lacerto creature's rolled power. */
    @Override
    public List<org.aventyrs.core.feat.Feat> getFeats() {
        return new NatureSummonTrait(kind, powers).hasMultipleLimbs()
                ? List.of(org.aventyrs.core.feat.CriaturaFeat.MEMBROS_MULTIPLOS)
                : List.of();
    }

    /** Inocular Veneno and Devorar Inteiro ride every attack it makes. */
    @Override
    public List<EffectChain> resolveAttackEffectChains(final CombatantSheet self) {
        List<EffectChain> chains = new ArrayList<>();
        if (hasPower(LacertoPower.INOCULAR_VENENO)) {
            chains.add(new InocularVeneno(kind == NatureSummonKind.ORGULHO_DE_LACERTO));
        }
        if (hasPower(LacertoPower.DEVORAR_INTEIRO)) {
            // ⚠️ "até 1 personagem para cada ponto de Vigor" read through the Ogro's own stomach, which is Vigor-sized.
            chains.add(new DevorarInteiro(self, new DevourServiceImpl()));
        }
        return chains;
    }

    /** The Anciente's "Imunidade a … Encantamentos nocivos" is a ward held for as long as it stands. */
    @Override
    public MonsterSheet spawn(final Player gm) {
        MonsterSheet sheet = SummonedMonsterTemplate.super.spawn(gm);
        if (kind == NatureSummonKind.ANCIENTE) {
            sheet.applyEffect(EnchantmentWard.openEnded());
        }
        return sheet;
    }
}
