package org.aventyrs.core.monster;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.ability.AttributeAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.monster.model.AbilityContext;
import org.aventyrs.core.monster.model.MonstrousAbility;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A held {@link MonstrousAbility}, resolved against its holder's {@link AbilityContext} and dressed
 * as an {@link AttributeAbility} — the adapter that lets a Habilidade Monstruosa reach every
 * aggregated stat with no service knowing monsters exist.
 *
 * <p>The stat services sum {@code @Modifier}s across {@code Character#getAttributeAbilities()}; a
 * {@code @Modifier} method takes no arguments, so the context is captured here per instance — the
 * pattern {@code ZumbiAbility} uses for its tiers — and each method delegates to {@link
 * MonstrousAbility#resolveModifier}. {@code ModifierResolverImpl} indexes declared methods only,
 * which is why they live on this concrete class. The anatomy hooks ({@code AttributeAbility}'s RE,
 * Meio-Dano, immunities, Armas Naturais, flight …) are forwarded the same way, and read by {@code
 * AbstractCombatantSheet}/{@code DamageServiceImpl} for any combatant.
 *
 * <p>{@link #hitPoints()} also carries the flat "+2PV" every Habilidade Monstruosa grants its holder
 * ("Para cada Habilidade Monstruosa que possuir ele recebe Bônus de +2PV") — including the
 * table-only ones, whose effects are otherwise not applied.
 */
@Getter
@EqualsAndHashCode
public class MonstrousAbilityGrant implements AttributeAbility {

    /** "Para cada Habilidade Monstruosa que possuir ele recebe Bônus de +2PV". */
    public static final int HIT_POINTS_PER_ABILITY = 2;

    @NonNull
    private final MonstrousAbility ability;

    @NonNull
    private final AbilityContext context;

    public MonstrousAbilityGrant(@NonNull final MonstrousAbility ability, @NonNull final AbilityContext context) {
        this.ability = ability;
        this.context = context;
    }

    public MonsterCategory getCategory() {
        return context.category();
    }

    private int resolve(final ModifierType type) {
        return ability.resolveModifier(type, context);
    }

    @Modifier(ModifierType.HIT_POINTS)
    int hitPoints() {
        return HIT_POINTS_PER_ABILITY + resolve(ModifierType.HIT_POINTS);
    }

    @Modifier(ModifierType.LIFE_MULTIPLIER)
    int lifeMultiplier() {
        return resolve(ModifierType.LIFE_MULTIPLIER);
    }

    @Modifier(ModifierType.MANA_MULTIPLIER)
    int manaMultiplier() {
        return resolve(ModifierType.MANA_MULTIPLIER);
    }

    @Modifier(ModifierType.DETERMINATION_MULTIPLIER)
    int determinationMultiplier() {
        return resolve(ModifierType.DETERMINATION_MULTIPLIER);
    }

    @Modifier(ModifierType.ACTION_POINTS)
    int actionPoints() {
        return resolve(ModifierType.ACTION_POINTS);
    }

    @Modifier(ModifierType.REACTIONS)
    int reactions() {
        return resolve(ModifierType.REACTIONS);
    }

    @Modifier(ModifierType.FREE_ACTIONS)
    int freeActions() {
        return resolve(ModifierType.FREE_ACTIONS);
    }

    @Modifier(ModifierType.INITIATIVE)
    int initiative() {
        return resolve(ModifierType.INITIATIVE);
    }

    @Modifier(ModifierType.MOVEMENT)
    int movement() {
        return resolve(ModifierType.MOVEMENT);
    }

    @Modifier(ModifierType.DEFESAS)
    int defenses() {
        return resolve(ModifierType.DEFESAS);
    }

    @Modifier(ModifierType.PHYSICAL_DEFENSE)
    int physicalDefense() {
        return resolve(ModifierType.PHYSICAL_DEFENSE);
    }

    @Modifier(ModifierType.MAGIC_DEFENSE)
    int magicDefense() {
        return resolve(ModifierType.MAGIC_DEFENSE);
    }

    @Modifier(ModifierType.DAMAGE_REDUCTION)
    int damageReduction() {
        return resolve(ModifierType.DAMAGE_REDUCTION);
    }

    @Modifier(ModifierType.DAMAGE_TAKEN_REDUCTION)
    int damageTakenReduction() {
        return resolve(ModifierType.DAMAGE_TAKEN_REDUCTION);
    }

    @Modifier(ModifierType.MAGIC_REDUCTION)
    int magicReduction() {
        return resolve(ModifierType.MAGIC_REDUCTION);
    }

    @Modifier(ModifierType.ABSOLUTE_DAMAGE_REDUCTION)
    int absoluteDamageReduction() {
        return resolve(ModifierType.ABSOLUTE_DAMAGE_REDUCTION);
    }

    @Modifier(ModifierType.HALF_DAMAGE)
    int halfDamage() {
        return resolve(ModifierType.HALF_DAMAGE);
    }

    @Modifier(ModifierType.SIZE_CATEGORY)
    int sizeCategory() {
        return resolve(ModifierType.SIZE_CATEGORY);
    }

    // ---- Forwarded AttributeAbility hooks --------------------------------------------------------

    @Override
    public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext) {
        return ability.resolveCriticalMarginIncrease(skillType, context);
    }

    @Override
    public int resolveElementalResistanceInstances(final ElementalType element) {
        return ability.resolveElementalResistanceInstances(element, context);
    }

    @Override
    public boolean halvesDamage(final DamageType damageType, final DamageDescriptor descriptor) {
        return ability.halvesDamage(damageType, descriptor, context);
    }

    @Override
    public boolean isImmuneToDamage(final DamageType damageType, final DamageDescriptor descriptor) {
        return ability.isImmuneToDamage(damageType, descriptor, context);
    }

    @Override
    public boolean isVulnerableToDamage(final DamageType damageType, final DamageDescriptor descriptor) {
        return ability.isVulnerableToDamage(damageType, descriptor, context);
    }

    @Override
    public Set<CriticalEffectType> resolveCriticalEffectImmunities() {
        return ability.resolveCriticalEffectImmunities(context);
    }

    @Override
    public int resolveCriticalResistance() {
        return ability.resolveCriticalResistance(context);
    }

    @Override
    public boolean ignoresMinorCriticalEffects() {
        return ability.ignoresMinorCriticalEffects(context);
    }

    @Override
    public boolean isImmuneToCondition(final ConditionType conditionType) {
        return ability.isImmuneToCondition(conditionType, context);
    }

    @Override
    public List<NaturalWeapon> getGrantedNaturalWeapons() {
        return ability.resolveNaturalWeapons(context);
    }

    @Override
    public List<TemporaryEffect> resolveDamageTakenEffects(final org.aventyrs.core.sheet.CombatantSheet holder,
                                                           final int finalDamage) {
        return ability.resolveDamageTakenEffects(finalDamage, context);
    }

    @Override
    public boolean keepsFlying() {
        return ability.keepsFlying(context);
    }

    @Override
    public List<TemporaryEffect> resolveWhileFlyingEffects() {
        return ability.resolveWhileFlyingEffects(context);
    }

    // ---- The holder's attacks (core 0.1.7.2) ---------------------------------------------------------

    /**
     * Every Habilidade Monstruosa character holds. A live scene foe is a plain {@code CharacterSheet} built from its
     * blueprint, never a {@code MonsterSheet}, so the attack hooks are read here, off the Character, for any attacker.
     */
    public static Stream<MonstrousAbilityGrant> heldBy(final Character character) {
        return character == null ? Stream.empty() : character.getAttributeAbilities().stream()
                .filter(MonstrousAbilityGrant.class::isInstance)
                .map(MonstrousAbilityGrant.class::cast);
    }

    /**
     * holder has just activated activated — applies what each held Habilidade grants for it ({@link
     * MonstrousAbility#resolveActivationTriggeredEffects}). An Efeito Ativo is traced to its Modelo by name among the
     * holder's own Habilidades.
     */
    public static void onActivation(final CombatantSheet holder, final org.aventyrs.core.ability.ActiveAbility activated) {
        if (holder == null || !(activated instanceof org.aventyrs.core.monster.model.MonstrousActiveAbility active)) {
            return;
        }
        heldBy(holder.getCharacter())
                .filter(grant -> grant.ability.resolveActiveAbilities(grant.context).stream()
                        .anyMatch(own -> own instanceof org.aventyrs.core.monster.model.MonstrousActiveAbility mine
                                && mine.getName().equals(active.getName())))
                .map(grant -> grant.ability.getModel())
                .findFirst()
                .ifPresent(model -> trigger(holder,
                        org.aventyrs.core.monster.model.ActivationTrigger.monstrous(model)));
    }

    /** holder has just activated a Título Aventyr Habilidade — see {@link #onActivation}. */
    public static void onTitleActivation(final CombatantSheet holder) {
        if (holder != null) {
            trigger(holder, org.aventyrs.core.monster.model.ActivationTrigger.title());
        }
    }

    private static void trigger(final CombatantSheet holder,
                                final org.aventyrs.core.monster.model.ActivationTrigger trigger) {
        heldBy(holder.getCharacter())
                .flatMap(grant -> grant.ability.resolveActivationTriggeredEffects(trigger, grant.context).stream())
                .toList()
                .forEach(holder::applyEffect);
    }

    /** Every aura holder's Habilidades have it emit right now — {@link MonstrousAbility#resolveAuras}. */
    public static List<org.aventyrs.core.scene.MonsterAura> auras(final CombatantSheet holder) {
        return holder == null ? List.of() : heldBy(holder.getCharacter())
                .flatMap(grant -> grant.ability.resolveAuras(holder, grant.context).stream())
                .toList();
    }

    /** The Correntes every held Habilidade puts on holder's attack — {@link MonstrousAbility#resolveAttackEffectChains}. */
    public static List<EffectChain> attackEffectChains(final CombatantSheet holder, final SkillType attackSkill,
                                                       final AttackSource attackSource) {
        return holder == null ? List.of() : heldBy(holder.getCharacter())
                .flatMap(grant -> grant.ability.resolveAttackEffectChains(attackSkill, attackSource, holder,
                        grant.context).stream())
                .toList();
    }

    /** The holder's own Resistência à Correntes de Efeitos raise. */
    public static int effectChainResistanceIncrease(final CombatantSheet holder) {
        return holder == null ? 0 : heldBy(holder.getCharacter())
                .mapToInt(grant -> grant.ability.resolveEffectChainResistanceIncrease(holder, grant.context))
                .sum();
    }

    /** How far the holder lowers its targets' Resistência à Correntes de Efeitos. */
    public static int targetEffectChainResistanceReduction(final CombatantSheet holder) {
        return holder == null ? 0 : heldBy(holder.getCharacter())
                .mapToInt(grant -> grant.ability.resolveTargetEffectChainResistanceReduction(holder, grant.context))
                .sum();
    }

    /**
     * The first held Habilidade that retypes holder's attack, or {@code null} — with Ascenção's Profano/Sagrado laid
     * over it when it names an element ({@link MonstrousAbility#resolveElementalDamageSanctity}).
     */
    public static DamageDescriptor attackDamageRetype(final CombatantSheet holder, final SkillType attackSkill,
                                                      final AttackSource attackSource) {
        if (holder == null) {
            return null;
        }
        DamageDescriptor retype = heldBy(holder.getCharacter())
                .map(grant -> grant.ability.resolveAttackDamageRetype(attackSkill, attackSource, holder,
                        grant.context))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (retype == null || retype.elementalType() == null) {
            return retype;
        }
        return heldBy(holder.getCharacter())
                .map(grant -> grant.ability.resolveElementalDamageSanctity(grant.context))
                .filter(Objects::nonNull)
                .findFirst()
                .map(sanctity -> new DamageDescriptor(retype.damageType(), retype.elementalType(), sanctity))
                .orElse(retype);
    }

    /**
     * The first Área de Efeito a held Habilidade grants character's attack, or {@code null}. holder is its sheet, or
     * {@code null} on a Character-only path — an area bought by an Efeito Ativo then stays unseen.
     */
    public static org.aventyrs.core.scene.AreaOfEffect attackArea(final Character character,
                                                                  final SkillType attackSkill,
                                                                  final AttackSource attackSource,
                                                                  final CombatantSheet holder) {
        return heldBy(character)
                .map(grant -> grant.ability.resolveAttackArea(attackSkill, attackSource, holder, grant.context))
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /** The Efeitos Críticos every held Habilidade adds to holder's critical hit. */
    public static List<CriticalEffectType> additionalCriticalEffects(final CombatantSheet holder,
                                                                     final SkillType attackSkill,
                                                                     final AttackSource attackSource) {
        return holder == null ? List.of() : heldBy(holder.getCharacter())
                .flatMap(grant -> grant.ability.resolveAdditionalCriticalEffects(attackSkill, attackSource, holder,
                        grant.context).stream())
                .toList();
    }

    /**
     * {@code null}: a Habilidade Monstruosa belongs to no Atributo. The domain is read only where a
     * character <i>acquires</i> an Habilidade de Atributo (slot counting, {@code
     * AttributeAbilityService}), which a monster never goes through.
     */
    @Override
    public AttributeDomain getAttributeDomain() {
        return null;
    }

    @Override
    public String getDescription() {
        return ability.getDescription();
    }
}
