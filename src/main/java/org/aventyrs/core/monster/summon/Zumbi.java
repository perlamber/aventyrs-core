package org.aventyrs.core.monster.summon;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.monster.MonsterSheet;
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
import java.util.Set;

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
 * <h2>A class, not a {@code GenericMonster} constant</h2>
 *
 * Two reasons, either sufficient. It has a name and a story, which {@code GenericMonster}'s own
 * javadoc excludes from that enum ("a foe with a name and a story belongs in {@code
 * AbstractMonsterTemplate} or a catalog of its own"). And it carries <b>two</b> per-instance
 * choices an enum constant cannot hold: the Conjurador's Graduação, and the Categoria de Tamanho
 * of the corpse being animated.
 *
 * <h2>What varies, and what doesn't</h2>
 *
 * The body's Categoria de Tamanho is the animator's choice, clamped to {@link #MIN_SIZE_CATEGORY}
 * …{@link #MAX_SIZE_CATEGORY} ("igual à do corpo usado (mínimo -2, máximo +2)"). Everything the
 * Conjurador's Graduação changes is folded in here at build time rather than patched onto the
 * sheet afterwards — Força by {@link #getAttributeBases()}, the attack bonus by {@link
 * #getAttackBonus()}, and the roll-facing clauses by the single {@link ZumbiAbility} instance.
 * The one exception is Roubo de Vida, which is a {@code TemporaryEffect} on the sheet rather than
 * anything on the {@code Character}, so {@link #spawn()} applies it after the sheet exists.
 *
 * <h2>Anatomia de Morto-Vivo Menor</h2>
 *
 * Only two of its four clauses reach a mechanism. The immunity to Amaldiçoar, Dilacerar,
 * Excruciante, Ferida Profunda and Sangramento is real and enforced (see {@link
 * CriticalEffectType}), and {@link #isUndead()} is what makes the Vantagem-against-the-living
 * clause resolvable. The rest are TODOs below, each on its own missing system.
 *
 * <p>TODO: "são imunes a danos Profanos e Naturais" and "Danos Físicos de Esmagamento sofridos
 * reduzidos em -3" both need damage-type-scoped mitigation. {@code DamageType} has no Profano,
 * Natural or Esmagamento constant, and {@code DamageService} resolves RD/RA with no notion of the
 * incoming damage's type at all — the sole exception, {@code AttributeAbility#resolveDamageReduction},
 * is not reachable from a {@code SkillCompetencyAbility}. The -3 is a reduction, not an immunity,
 * so it needs the type-scoped half; the two immunities need a nullification stage that does not
 * exist in any form.
 *
 * <p>TODO: "Não precisam dormir ou respirar" needs a fadiga/asfixia system. Nothing in this core
 * tracks either, so there is no effect to be exempt from.
 *
 * <p>TODO: "sofrem Danos de Magias Divinas que recuperam PV ao invés de se curarem" needs both a
 * Magia entity carrying a Tipo (no {@code Magia} exists — see {@code SpellCastingService}, which
 * cannot resolve either roll's GD for the same reason) and an inversion stage on healing;
 * {@code CombatantSheet#heal} has no hook to redirect a recovery into damage.
 *
 * <p>TODO: "Danos de Ataques 1d6+3 (Base 1 + Metade da Força)" has nowhere to live. A foe's damage
 * is entirely caller-supplied today — {@code AttackReceiver}/{@code AttackDelivery} assemble a
 * {@code DamageInteraction} and the caller feeds it the figure, because this core has no
 * weapon/dano-roll concept and never rolls dice. The formula is recorded here rather than dropped.
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

    /**
     * "Imunes aos Efeitos Críticos Amaldiçoar, Dilacerar, Excruciante, Ferida Profunda e
     * Sangramento." Four of the five have no implementation yet; naming them is the point — see
     * {@link CriticalEffectType}.
     */
    public static final Set<CriticalEffectType> ANATOMIA_DE_MORTO_VIVO_MENOR = Set.of(
            CriticalEffectType.AMALDICOAR,
            CriticalEffectType.DILACERAR,
            CriticalEffectType.EXCRUCIANTE,
            CriticalEffectType.FERIDA_PROFUNDA,
            CriticalEffectType.SANGRAMENTO);

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
        return List.of();
    }

    /**
     * The corpse's own Categoria de Tamanho, clamped into the range the Magia can animate. Clamped
     * rather than rejected: the size comes from whatever body was to hand, so a caller naming one
     * outside the range is describing a corpse, not making an illegal request.
     */
    @Override
    public SizeCategory getSizeCategory() {
        if (bodySizeCategory.getCategory() < MIN_SIZE_CATEGORY.getCategory()) {
            return MIN_SIZE_CATEGORY;
        }
        if (bodySizeCategory.getCategory() > MAX_SIZE_CATEGORY.getCategory()) {
            return MAX_SIZE_CATEGORY;
        }
        return bodySizeCategory;
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
     * The GD a Zumbi's own attacks present to a defender's Esquiva e Aparar roll. Authored, like
     * every foe's — see {@code MonsterTemplate}. Its stat block states no GD, so this is the
     * catalogue default for a creature this weak rather than a number read off the text.
     */
    @Override
    public DifficultyLevel getAttackDifficulty() {
        return DifficultyLevel.EASY;
    }

    /**
     * "Recebem Bônus em Perícia de Ataque igual à quantidade de Graduações em Domínio do Mana de
     * seu Conjurador" — applied here as well as on {@link ZumbiAbility}, and deliberately so.
     * The two feed opposite directions of an exchange: the ability's {@code @Modifier} raises the
     * Zumbi's own Ataque roll, while this raises the threshold its attack presents when the
     * <i>defender</i> rolls (see {@code AttackReceiver}). One clause, two consumers.
     */
    @Override
    public int getAttackBonus() {
        return Math.max(0, conjuradorManaGraduation);
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

    @Override
    public Set<CriticalEffectType> getCriticalEffectImmunities() {
        return ANATOMIA_DE_MORTO_VIVO_MENOR;
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
