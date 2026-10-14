package org.aventyrs.core.monster.summon;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageBase;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.item.AbstractWeapon;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ItemRarity;
import org.aventyrs.core.item.ItemWeightClass;
import org.aventyrs.core.monster.MonsterSheet;
import org.aventyrs.core.monster.SkillDifficulty;
import org.aventyrs.core.monster.SummonedMonsterTemplate;
import org.aventyrs.core.sheet.LifeSteal;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillSpecialization;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.ataquecorpoacorpo.AtaqueCorpoACorpoSpecialization;
import org.aventyrs.core.skill.attention.AttentionSpecialization;
import org.aventyrs.core.skill.esquivaeaparar.EsquivaEApararSpecialization;
import org.aventyrs.core.skill.furtividade.FurtividadeSpecialization;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <b>Carniçal</b> — the creature of {@code ReanimarSpell#CRIAR_CARNICAL} (core 0.1.6.25), raised from a corpse and
 * built the {@link Zumbi} way: a fixed stat block from {@code docs/rules/magias.txt}, its Conjurador's Graduação in
 * Domínio do Mana folded in at build time, and the manual's Anatomia de Morto-Vivo in place of the printed "Menor"
 * (table ruling, 2026-10-14).
 *
 * <p><b>Açougueiro</b> ({@link #isButcher()}): "Recebem +1PA, Vantagem nas rolagens de Perícias de Ataque e Danos,
 * possuem Roubo de Vida 1" — all applied. "Recuperam 1d6PV sempre que reduzem os PV de um personagem vivo à zero ou
 * menos" is the caller's: {@link #BUTCHER_KILL_HEAL_DICE} dice, healed after the killing blow. Its missing Duração and
 * the +5PM are the invocation's ({@code UndeadInvocationService}).
 *
 * <p>Defesas: "Bônus Racial de +3 em Defesas" plus the stat block's "Defesas +11", summed as the Zumbi's are (⚠️ the
 * same reading). Its attack is an authored Arma Natural ({@link #naturalAttack()}): "2d6+3 (Base 0+ Metade da Força)"
 * less the ½ Força the engine adds itself.
 */
@Getter
@Builder(toBuilder = true)
public class Carnical implements SummonedMonsterTemplate {

    /** "mínimo -1". */
    public static final SizeCategory MIN_SIZE_CATEGORY = SizeCategory.MINUS_ONE;

    /** "máximo +2". */
    public static final SizeCategory MAX_SIZE_CATEGORY = SizeCategory.PLUS_TWO;

    /** "26PV (Multiplicador de PV x4)". */
    public static final int LIFE_MULTIPLIER = 4;

    /** "Bônus Racial de +3 em Defesas" plus "Defesas +11". */
    public static final int DEFENSES = 14;

    /** "Possuem 3 Pontos de Ação (PA)." */
    public static final int ACTION_POINTS = 3;

    /** The Bônus Mágico (encantamento) to Força, Destreza and Vigor at 7 Graduações. */
    public static final int ENCANTAMENTO_ATTRIBUTES = 2;

    /** Açougueiro's "+1PA". */
    public static final int BUTCHER_ACTION_POINTS = 1;

    /** Açougueiro's "Roubo de Vida 1". */
    public static final int BUTCHER_LIFE_STEAL = 1;

    /** Açougueiro's "recuperam 1d6PV" on a kill. */
    public static final int BUTCHER_KILL_HEAL_DICE = 1;

    @Builder.Default
    private final int conjuradorManaGraduation = 0;

    /** "Categoria de Tamanho igual à do corpo usado" — clamped by {@link #getSizeCategory()}. */
    @Builder.Default
    @NonNull
    private final SizeCategory bodySizeCategory = SizeCategory.ZERO;

    /** Raised with the Efeito Alternativo Açougueiro. */
    @Builder.Default
    private final boolean butcher = false;

    public static Carnical summonedBy(final int manaGraduation) {
        return Carnical.builder().conjuradorManaGraduation(manaGraduation).build();
    }

    @Override
    public Carnical withConjurador(final int manaGraduation) {
        return toBuilder().conjuradorManaGraduation(manaGraduation).build();
    }

    @Override
    public String getName() {
        return "Carniçal";
    }

    private int encantamento() {
        return conjuradorManaGraduation >= ZumbiAbility.STRENGTH_AND_LIFE_STEAL_TIER ? ENCANTAMENTO_ATTRIBUTES : 0;
    }

    /** "Força 6, Destreza 4, Vigor 4, Gnose 1, Instinto 4, Foco 1, Carisma 1", plus the encantamento at 7. */
    @Override
    public Map<AttributeDomain, Integer> getAttributeBases() {
        Map<AttributeDomain, Integer> bases = new EnumMap<>(AttributeDomain.class);
        bases.put(AttributeDomain.STRENGTH, 6 + encantamento());
        bases.put(AttributeDomain.DEXTERITY, 4 + encantamento());
        bases.put(AttributeDomain.VIGOR, 4 + encantamento());
        bases.put(AttributeDomain.GNOSE, 1);
        bases.put(AttributeDomain.INSTINCT, 4);
        bases.put(AttributeDomain.FOCUS, 1);
        bases.put(AttributeDomain.CHARISMA, 1);
        return bases;
    }

    @Override
    public Map<SkillType, Integer> getSkillGraduations() {
        return Map.of(SkillType.ATAQUE_CORPO_A_CORPO, 6, SkillType.ATTENTION, 4,
                SkillType.ESQUIVA_E_APARAR, 4, SkillType.FURTIVIDADE, 4);
    }

    @Override
    public Map<SkillType, List<SkillSpecialization>> getSkillSpecializations() {
        return Map.of(
                SkillType.ATAQUE_CORPO_A_CORPO, List.of(AtaqueCorpoACorpoSpecialization.PRIMAL,
                        AtaqueCorpoACorpoSpecialization.INFANTARIA_LEVE, AtaqueCorpoACorpoSpecialization.INFANTARIA_PESADA),
                SkillType.ATTENTION, List.of(AttentionSpecialization.SENTIDOS_APURADOS),
                SkillType.ESQUIVA_E_APARAR, List.of(EsquivaEApararSpecialization.LUTADOR_LEVE,
                        EsquivaEApararSpecialization.SOLDADO_DE_INFANTARIA, EsquivaEApararSpecialization.PESO_PESADO),
                SkillType.FURTIVIDADE, List.of(FurtividadeSpecialization.MAESTRIA_DA_OCULTACAO));
    }

    @Override
    public List<SkillCompetencyAbility> getSkillCompetencyAbilities() {
        return List.of(new CarnicalAbility(conjuradorManaGraduation, butcher));
    }

    @Override
    public List<AttributeAbility> getAttributeAbilities() {
        return List.of();
    }

    @Override
    public List<Item> getEquipment() {
        return List.of(naturalAttack());
    }

    /** "Danos de Ataques 2d6+3 (Base 0+ Metade da Força)", less the ½ Força the engine adds. */
    public AbstractWeapon naturalAttack() {
        return AbstractWeapon.builder()
                .name("Garras e Dentes")
                .category(ItemCategory.NATURAL_WEAPON)
                .rarity(ItemRarity.NATURAL)
                .weightClass(ItemWeightClass.MEDIUM)
                .damageBase(DamageBase.of(2, 0))
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .build();
    }

    @Override
    public SizeCategory getSizeCategory() {
        return Zumbi.clamp(bodySizeCategory, MIN_SIZE_CATEGORY, MAX_SIZE_CATEGORY);
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

    /** The trained Perícias at Médio, its attack also carrying the Conjurador's bonus (the {@link Zumbi} reading). */
    @Override
    public Map<SkillType, SkillDifficulty> getSkillDifficulties() {
        Map<SkillType, SkillDifficulty> difficulties = new EnumMap<>(SkillType.class);
        difficulties.put(SkillType.ATTENTION, SkillDifficulty.of(DifficultyLevel.MEDIUM, 0));
        difficulties.put(SkillType.ESQUIVA_E_APARAR, SkillDifficulty.of(DifficultyLevel.MEDIUM, 0));
        difficulties.put(SkillType.FURTIVIDADE, SkillDifficulty.of(DifficultyLevel.MEDIUM, 0));
        SkillDifficulty attack = SkillDifficulty.of(DifficultyLevel.MEDIUM, Math.max(0, conjuradorManaGraduation));
        for (SkillType skillType : SkillType.values()) {
            if (skillType.isAttackSkill()) {
                difficulties.put(skillType, attack);
            }
        }
        return difficulties;
    }

    @Override
    public int getActionPoints() {
        return ACTION_POINTS + (butcher ? BUTCHER_ACTION_POINTS : 0);
    }

    @Override
    public int getLifeMultiplier() {
        return LIFE_MULTIPLIER;
    }

    /** "Ágeis e inteligentes" — the one spell undead that is. */
    @Override
    public boolean isIntelligent() {
        return true;
    }

    @Override
    public boolean hasUndeadAnatomy() {
        return true;
    }

    /** Açougueiro's Roubo de Vida 1, applied once the sheet exists (the {@link Zumbi#spawn} shape). */
    @Override
    public MonsterSheet spawn(final Player gm) {
        MonsterSheet sheet = SummonedMonsterTemplate.super.spawn(gm);
        if (butcher) {
            sheet.applyEffect(new LifeSteal(BUTCHER_LIFE_STEAL, Optional.empty()));
        }
        return sheet;
    }
}
