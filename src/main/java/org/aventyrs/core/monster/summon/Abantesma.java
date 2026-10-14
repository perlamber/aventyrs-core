package org.aventyrs.core.monster.summon;

import lombok.Builder;
import lombok.Getter;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.DamageScope;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.effect.PossessaoFuriosa;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemRarity;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.monster.SkillDifficulty;
import org.aventyrs.core.monster.SummonedMonsterTemplate;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataqueadistancia.AtaqueADistanciaSpecialization;
import org.aventyrs.core.skill.attention.AttentionSpecialization;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEApararSpecialization;
import org.aventyrs.core.skill.furtividade.FurtividadeSpecialization;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * <b>Abantesma</b> — the ghost of {@code ReanimarSpell#INVOCAR_ABANTESMA} and the 6 on Festim dos Mortos' die (core
 * 0.1.6.25). A fixed stat block from {@code docs/rules/magias.txt}, with the manual's Anatomia de Morto-Vivo (ruling,
 * 2026-10-14).
 *
 * <ul>
 *   <li>"Fantasmas não possuem PV, quando sofrem danos a Duração da Magia é reduzida em -1 Rodada" — {@link
 *   #damageShortensDuration()}: {@code Scene#settleSummons} takes a Rodada off its record and wipes the damage.</li>
 *   <li>"Incorpóreos e imunes à danos Físicos (exceto danos Elementais)" — {@link #getDamageImmunities()}: plain
 *   Físico; an Elemental hit is its own {@code DamageType} since 0.1.6.10, so it is not caught.</li>
 *   <li>"Possessão Furiosa" rides every attack ({@link #resolveAttackEffectChains}).</li>
 *   <li>"Força Nula … Vigor Nulo" ⚠️ read as 0. "Efetuam ataques contra a DM dos alvos" is the caller's: {@link
 *   #ATTACKS_MAGIC_DEFENSE}. "Não podem se afastar além de Distância Longa de seu Conjurador" is positional, the
 *   caller's too.</li>
 *   <li>"Danos de Ataques 1d6+6 (Base 2+ Metade da Destreza)" — {@link #naturalAttack()} carries the Base; TODO the ½
 *   Destreza, which no engine term adds to a ranged attack.</li>
 * </ul>
 */
@Getter
@Builder(toBuilder = true)
public class Abantesma implements SummonedMonsterTemplate {

    /** "Defesas +18". */
    public static final int DEFENSES = 18;

    /** "Possuem 4 Pontos de Ação (PA)." */
    public static final int ACTION_POINTS = 4;

    /** "Efetuam ataques contra a DM dos alvos." */
    public static final boolean ATTACKS_MAGIC_DEFENSE = true;

    /** "Duração: 5 Rodadas" — Invocar Abantesma's own. */
    public static final int ROUNDS = 5;

    @Builder.Default
    private final int conjuradorManaGraduation = 0;

    public static Abantesma summonedBy(final int manaGraduation) {
        return Abantesma.builder().conjuradorManaGraduation(manaGraduation).build();
    }

    @Override
    public Abantesma withConjurador(final int manaGraduation) {
        return toBuilder().conjuradorManaGraduation(manaGraduation).build();
    }

    @Override
    public String getName() {
        return "Abantesma";
    }

    private int encantamento() {
        return conjuradorManaGraduation >= ZumbiAbility.STRENGTH_AND_LIFE_STEAL_TIER ? 2 : 0;
    }

    /** "Força Nula, Destreza 8, Vigor Nulo, Gnose 2, Instinto 5, Foco 2, Carisma 2", the encantamento at 7. */
    @Override
    public Map<AttributeDomain, Integer> getAttributeBases() {
        Map<AttributeDomain, Integer> bases = new EnumMap<>(AttributeDomain.class);
        bases.put(AttributeDomain.STRENGTH, encantamento());
        bases.put(AttributeDomain.DEXTERITY, 8 + encantamento());
        bases.put(AttributeDomain.VIGOR, encantamento());
        bases.put(AttributeDomain.GNOSE, 2);
        bases.put(AttributeDomain.INSTINCT, 5);
        bases.put(AttributeDomain.FOCUS, 2);
        bases.put(AttributeDomain.CHARISMA, 2);
        return bases;
    }

    @Override
    public Map<SkillType, Integer> getSkillGraduations() {
        return Map.of(SkillType.ATAQUE_A_DISTANCIA, 10, SkillType.ATTENTION, 10,
                SkillType.ESQUIVA_E_APARAR, 10, SkillType.FURTIVIDADE, 4);
    }

    @Override
    public Map<SkillType, List<SkillSpecialization>> getSkillSpecializations() {
        return Map.of(
                SkillType.ATAQUE_A_DISTANCIA, List.of(AtaqueADistanciaSpecialization.ARTILHARIA_LEVE),
                SkillType.ATTENTION, List.of(AttentionSpecialization.SENTIDOS_APURADOS),
                SkillType.ESQUIVA_E_APARAR, List.of(EsquivaEApararSpecialization.LUTADOR_LEVE,
                        EsquivaEApararSpecialization.SOLDADO_DE_INFANTARIA, EsquivaEApararSpecialization.PESO_PESADO),
                SkillType.FURTIVIDADE, List.of(FurtividadeSpecialization.MAESTRIA_DA_OCULTACAO));
    }

    @Override
    public List<SkillCompetencyAbility> getSkillCompetencyAbilities() {
        return List.of(new AbantesmaAbility(conjuradorManaGraduation));
    }

    @Override
    public List<AttributeAbility> getAttributeAbilities() {
        return List.of();
    }

    @Override
    public List<Item> getEquipment() {
        return List.of(naturalAttack());
    }

    /** "Atacam à Distância" — Base 2 of "1d6+6 (Base 2+ Metade da Destreza)". */
    public AbstractWeapon naturalAttack() {
        return AbstractWeapon.builder()
                .name("Toque Espectral")
                .category(ItemCategory.NATURAL_WEAPON)
                .rarity(ItemRarity.NATURAL)
                .weightClass(ItemWeightClass.LIGHT)
                .damageBase(DamageBase.of(1, 2))
                .skillType(SkillType.ATAQUE_A_DISTANCIA)
                .build();
    }

    /** "Categoria de Tamanho 0." */
    @Override
    public SizeCategory getSizeCategory() {
        return SizeCategory.ZERO;
    }

    @Override
    public int getPhysicalDefense() {
        return DEFENSES;
    }

    @Override
    public int getMagicDefense() {
        return DEFENSES;
    }

    @Override
    public SkillDifficulty getGeneralDifficulty() {
        return SkillDifficulty.of(DifficultyLevel.EASY, 0);
    }

    /** ⚠️ The +10 Perícias presented at Difícil, its attack also carrying the Conjurador's bonus. */
    @Override
    public Map<SkillType, SkillDifficulty> getSkillDifficulties() {
        Map<SkillType, SkillDifficulty> difficulties = new EnumMap<>(SkillType.class);
        difficulties.put(SkillType.ATTENTION, SkillDifficulty.of(DifficultyLevel.HARD, 0));
        difficulties.put(SkillType.ESQUIVA_E_APARAR, SkillDifficulty.of(DifficultyLevel.HARD, 0));
        difficulties.put(SkillType.FURTIVIDADE, SkillDifficulty.of(DifficultyLevel.MEDIUM, 0));
        SkillDifficulty attack = SkillDifficulty.of(DifficultyLevel.HARD, Math.max(0, conjuradorManaGraduation));
        for (SkillType skillType : SkillType.values()) {
            if (skillType.isAttackSkill()) {
                difficulties.put(skillType, attack);
            }
        }
        return difficulties;
    }

    @Override
    public int getActionPoints() {
        return ACTION_POINTS;
    }

    /** Incorpóreo: plain Físico damage reaches nothing. */
    @Override
    public Set<DamageScope> getDamageImmunities() {
        return Set.of(DamageScope.PHYSICAL);
    }

    @Override
    public List<EffectChain> resolveAttackEffectChains(final CombatantSheet self) {
        return List.of(new PossessaoFuriosa(self));
    }

    @Override
    public boolean hasUndeadAnatomy() {
        return true;
    }

    /** "Fantasmas não possuem PV" — damage costs it Duração instead. */
    @Override
    public boolean damageShortensDuration() {
        return true;
    }
}
