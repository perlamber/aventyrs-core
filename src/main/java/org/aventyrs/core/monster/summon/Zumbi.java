package org.aventyrs.core.monster.summon;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.item.Item;
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

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <b>Zumbi</b> — a corpse animated by a Conjurador, and the most basic invocação in the game.
 *
 * <pre>{@code
 * MonsterSheet raised  = Zumbi.builder().build().spawn(necromancer, gm);              // by a Conjurador
 * MonsterSheet fromGm  = Zumbi.builder().sizeCategory(SizeCategory.PLUS_ONE)
 *                             .build().spawn(7, gm);                                 // a number off a form
 * MonsterSheet shambler = Zumbi.builder().build().spawn(gm);                          // purely narrative
 * }</pre>
 *
 * <h2>A fixed stat block, not a {@code MonsterBlueprint}</h2>
 *
 * Its numbers come from the Magia that raises it and the Conjurador's Graduação, not from {@code
 * criacao-de-monstros.txt}'s Grau de Poder — so it states them outright rather than deriving them.
 * And it carries <b>two</b> per-instance choices a catalog constant cannot hold: the Conjurador's
 * Graduação, and the Categoria de Tamanho of the corpse being animated.
 *
 * <h2>What varies, and what doesn't</h2>
 *
 * The body's Categoria de Tamanho is the animator's choice, clamped to {@link #MIN_SIZE_CATEGORY}
 * …{@link #MAX_SIZE_CATEGORY} ("igual à do corpo usado (mínimo -2, máximo +2)"). Everything the
 * Conjurador's Graduação changes is folded in here at build time rather than patched onto the
 * sheet afterwards — Força by {@link #getAttributeBases()}, the attack GD by {@link
 * #getSkillDifficulties()}, and the roll-facing clauses by the single {@link ZumbiAbility} instance.
 * The one exception is Roubo de Vida, which is a {@code TemporaryEffect} on the sheet rather than
 * anything on the {@code Character}, so {@link #spawn()} applies it after the sheet exists.
 *
 * <h2>Anatomia de Morto-Vivo — the manual's, not the printed "Menor"</h2>
 *
 * The Magia prints an "Anatomia de Morto-Vivo Menor". By table ruling (2026-10-14) every spell-raised undead takes the
 * full Anatomia de Morto-Vivo of the Monster manual's Escolhido de Macária instead — {@link #hasUndeadAnatomy()}, see
 * {@link org.aventyrs.core.monster.UndeadAnatomy} for every clause it carries (its Efeito Crítico immunities, Profano
 * healing, curative effects dealing Dano Sagrado, Resistência a Correntes +2, no asfixia). The Menor's own immunities
 * (Amaldiçoar, Excruciante, Profano and Natural damage) are therefore gone, by that ruling. TODO: "Danos Físicos de
 * Esmagamento sofridos reduzidos em -3" — Corte/Perfuração/Impacto are still not modelled.
 *
 * <p>"Danos de Ataques 1d6+3 (Base 1 + Metade da Força)" is {@link #naturalAttack()} (core 0.1.6.25).
 */
@Getter
@Builder(toBuilder = true)
public class Zumbi implements SummonedMonsterTemplate {

    /** "mínimo -2" — the smallest corpse worth animating. */
    public static final SizeCategory MIN_SIZE_CATEGORY = SizeCategory.MINUS_TWO;

    /** "máximo +2" — past this the corpse is too large for the Magia. */
    public static final SizeCategory MAX_SIZE_CATEGORY = SizeCategory.PLUS_TWO;

    /** Its Força before any Bônus Mágico — {@link ZumbiAbility#STRENGTH_AND_LIFE_STEAL_TIER} raises it. */
    public static final int BASE_STRENGTH = 4;

    /** "20PV (Multiplicador de PV x5)" — 10 base + Vigor 2 × 5. */
    public static final int LIFE_MULTIPLIER = 5;

    /** "Bônus Racial de +5 em Defesas" plus the stat block's own "Defesas +6". */
    public static final int DEFENSES = 11;

    /** "Possuem 2 Pontos de Ação (PA)." */
    public static final int ACTION_POINTS = 2;

    /** The Graduação in Domínio do Mana of whoever raised it; 0 when nobody did. */
    @Builder.Default
    private final int conjuradorManaGraduation = 0;

    /** "Categoria de Tamanho igual à do corpo usado" — clamped by {@link #getSizeCategory()}. */
    @Builder.Default
    @NonNull
    private final SizeCategory bodySizeCategory = SizeCategory.ZERO;

    /** A Zumbi raised by a Conjurador of the given Graduação; 0 for one nobody raised. */
    public static Zumbi summonedBy(final int manaGraduation) {
        return Zumbi.builder().conjuradorManaGraduation(manaGraduation).build();
    }

    @Override
    public Zumbi withConjurador(final int manaGraduation) {
        return toBuilder().conjuradorManaGraduation(manaGraduation).build();
    }

    @Override
    public String getName() {
        return "Zumbi";
    }

    /**
     * Every Attribute the stat block states, listed in full — including the five that happen to
     * equal {@code AttributeValue}'s own default of 1. An authored stat block saying "Destreza 1"
     * is not the same fact as an omission, and writing them out keeps the block readable against
     * the source text.
     *
     * <p>Força carries the {@link ZumbiAbility#STRENGTH_AND_LIFE_STEAL_TIER} Bônus Mágico, folded
     * in here because {@code AttributeValue} has no round-scoped or modifier-sourced component —
     * only {@code base}/{@code racialBonus}/{@code variable}, none summed via {@link
     * org.aventyrs.core.modifier.ModifierType}.
     */
    @Override
    public Map<AttributeDomain, Integer> getAttributeBases() {
        Map<AttributeDomain, Integer> bases = new EnumMap<>(AttributeDomain.class);
        bases.put(AttributeDomain.STRENGTH, BASE_STRENGTH + encantamentoStrength());
        bases.put(AttributeDomain.DEXTERITY, 1);
        bases.put(AttributeDomain.VIGOR, 2);
        bases.put(AttributeDomain.GNOSE, 1);
        bases.put(AttributeDomain.INSTINCT, 1);
        bases.put(AttributeDomain.FOCUS, 1);
        bases.put(AttributeDomain.CHARISMA, 1);
        return bases;
    }

    private int encantamentoStrength() {
        return conjuradorManaGraduation >= ZumbiAbility.STRENGTH_AND_LIFE_STEAL_TIER
                ? ZumbiAbility.ENCANTAMENTO_STRENGTH
                : 0;
    }

    private boolean hasLifeSteal() {
        return conjuradorManaGraduation >= ZumbiAbility.STRENGTH_AND_LIFE_STEAL_TIER;
    }

    /** "Ataque Corpo-a-Corpo [Primal] +2". */
    @Override
    public Map<SkillType, Integer> getSkillGraduations() {
        return Map.of(SkillType.ATAQUE_CORPO_A_CORPO, 2);
    }

    @Override
    public Map<SkillType, List<SkillSpecialization>> getSkillSpecializations() {
        return Map.of(SkillType.ATAQUE_CORPO_A_CORPO, List.of(AtaqueCorpoACorpoSpecialization.PRIMAL));
    }

    @Override
    public List<SkillCompetencyAbility> getSkillCompetencyAbilities() {
        return List.of(new ZumbiAbility(conjuradorManaGraduation));
    }

    @Override
    public List<AttributeAbility> getAttributeAbilities() {
        return List.of();
    }

    @Override
    public List<Item> getEquipment() {
        return List.of(naturalAttack());
    }

    /**
     * "Danos de Ataques 1d6+3 (Base 1+ Metade da Força)" (core 0.1.6.25), authored as an Arma Natural less the ½ Força
     * the engine adds itself — the {@code NatureSummon#naturalAttack()} shape.
     */
    public org.aventyrs.core.item.AbstractWeapon naturalAttack() {
        return org.aventyrs.core.item.AbstractWeapon.builder()
                .name("Ataque Natural")
                .category(org.aventyrs.core.item.ItemCategory.NATURAL_WEAPON)
                .rarity(org.aventyrs.core.item.ItemRarity.NATURAL)
                .weightClass(org.aventyrs.core.item.ItemWeightClass.MEDIUM)
                .damageBase(org.aventyrs.core.character.DamageBase.of(1, 1))
                .skillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .build();
    }

    /**
     * The corpse's own Categoria de Tamanho, clamped into the range the Magia can animate. Clamped
     * rather than rejected: the size comes from whatever body was to hand, so a caller naming one
     * outside the range is describing a corpse, not making an illegal request.
     */
    @Override
    public SizeCategory getSizeCategory() {
        return clamp(bodySizeCategory, MIN_SIZE_CATEGORY, MAX_SIZE_CATEGORY);
    }

    /** body clamped into min…max — the corpse-size range every raised undead's Magia states. */
    public static SizeCategory clamp(final SizeCategory body, final SizeCategory min, final SizeCategory max) {
        if (body.getCategory() < min.getCategory()) {
            return min;
        }
        if (body.getCategory() > max.getCategory()) {
            return max;
        }
        return body;
    }

    @Override
    public int getPhysicalDefense() {
        return DEFENSES;
    }

    @Override
    public int getMagicDefense() {
        return DEFENSES;
    }

    /**
     * The GD a Zumbi presents on anything but an attack. Authored, like every foe's — see {@code
     * MonsterTemplate}. Its stat block states no GD, so this is the catalogue default for a
     * creature this weak rather than a number read off the text.
     */
    @Override
    public SkillDifficulty getGeneralDifficulty() {
        return SkillDifficulty.of(DifficultyLevel.EASY, 0);
    }

    /**
     * "Recebem Bônus em Perícia de Ataque igual à quantidade de Graduações em Domínio do Mana de
     * seu Conjurador" — applied here to the GD of each Perícia de Ataque as well as on {@link
     * ZumbiAbility}, and deliberately so. The two feed opposite directions of an exchange: the
     * ability's {@code @Modifier} raises the Zumbi's own Ataque roll, while this raises the
     * threshold its attack presents when the <i>defender</i> rolls (see {@code AttackReceiver}).
     * One clause, two consumers. Every other Perícia keeps {@link #getGeneralDifficulty()}.
     */
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
        return ACTION_POINTS;
    }

    @Override
    public int getLifeMultiplier() {
        return LIFE_MULTIPLIER;
    }

    @Override
    public boolean isUndead() {
        return true;
    }

    /** The Anatomia de Morto-Vivo (table ruling, 2026-10-14) — its immunities come with it. */
    @Override
    public boolean hasUndeadAnatomy() {
        return true;
    }

    /**
     * The inherited spawn, plus the one tier bonus that isn't a {@code Character} fact: "Roubo de
     * Vida 1" at {@link ZumbiAbility#STRENGTH_AND_LIFE_STEAL_TIER}.
     *
     * <p>Roubo de Vida is a {@code LifeSteal} {@code TemporaryEffect} held on the sheet, not a
     * {@link org.aventyrs.core.modifier.ModifierType} — {@code AbstractCombatantSheet#getTotalLifeSteal}
     * sums the active ones — so it can only be applied once the sheet exists. Open-ended
     * ({@code Optional.empty()}): it lasts as long as the Zumbi does, and nothing about the
     * encantamento counts down in Rodadas.
     *
     * <p>Everything {@code spawn()}'s own contract promises still holds: each call builds its own
     * {@code Character}, its own mutable {@code SkillGraduation}s and now its own {@code
     * LifeSteal}, so two Zumbis raised from one template share nothing.
     */
    @Override
    public MonsterSheet spawn(final Player gm) {
        MonsterSheet sheet = SummonedMonsterTemplate.super.spawn(gm);
        if (hasLifeSteal()) {
            sheet.applyEffect(new LifeSteal(ZumbiAbility.LIFE_STEAL, Optional.empty()));
        }
        return sheet;
    }
}
