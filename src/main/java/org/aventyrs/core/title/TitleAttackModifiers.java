package org.aventyrs.core.title;

import lombok.NonNull;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.AttackSource;

import java.util.ArrayList;
import java.util.List;

/**
 * What a holder's Títulos change about the <b>one attack they are about to build</b>, where this
 * core cannot apply the change itself — the passive, per-attack twin of {@link EmpoweredAttack},
 * which reports what an <i>activation</i> gives the next attack.
 *
 * <p>Everything a Título can apply inside a roll it does, through the scans on {@link
 * AventyrTitle} (Margem Crítica, Dano Crítico, Dano Base, Vantagem on the roll and the dano). What
 * is left lands here because each piece belongs to the caller by this core's own conventions:
 * <ul>
 *   <li>{@link #extraDamageDice} — this core never rolls dice;</li>
 *   <li>{@link #actionPointReduction} — the price of an attack is caller-supplied on its {@code
 *       SkillRoll}, and no PA pool exists to charge;</li>
 *   <li>{@link #defenseType} / {@link #damageDescriptor} — the caller names the Defesa it rolls
 *       against on {@code DeliveredAttack} and types the damage it deals;</li>
 *   <li>{@link #effectChains} — a {@code DeliveredAttack}'s Correntes are caller-supplied;</li>
 *   <li>{@link #criticalEffectOverride} — an Efeito Crítico the attack's own weapon effect is
 *       replaced with. Informational: {@code AttackDelivery} applies it itself.</li>
 * </ul>
 *
 * <p>Every field is inert at 0/{@code null}/empty. {@link #resolve} is the one query a caller
 * makes, before building the attack; {@link #consumeCharges} is the one it makes after.
 *
 * @param extraDamageDice      further d6 the dano roll gains
 * @param actionPointReduction PA this attack's price is reduced by, floored by the caller at the
 *                             cheapest legal price
 * @param defenseType          the Defesa this attack is rolled against instead of the usual one, or
 *                             {@code null} to leave it
 * @param damageDescriptor     what this attack's damage is typed as instead, or {@code null}
 * @param criticalEffectOverride the Efeito Crítico this attack carries instead of its own, or
 *                             {@code null}
 * @param effectChains         Correntes de Efeito this attack gains
 */
public record TitleAttackModifiers(int extraDamageDice,
                                   int actionPointReduction,
                                   DefenseType defenseType,
                                   DamageDescriptor damageDescriptor,
                                   CriticalEffectType criticalEffectOverride,
                                   List<EffectChain> effectChains) {

    /** Nothing changed. */
    public static final TitleAttackModifiers NONE = new TitleAttackModifiers(0, 0, null, null, null, List.of());

    public TitleAttackModifiers {
        effectChains = effectChains == null ? List.of() : List.copyOf(effectChains);
    }

    /** Whether this changes nothing at all. */
    public boolean isNone() {
        return extraDamageDice == 0 && actionPointReduction == 0 && defenseType == null
                && damageDescriptor == null && criticalEffectOverride == null && effectChains.isEmpty();
    }

    /**
     * Folds other onto this one: dice, PA reductions and Correntes add up; for the three
     * replacements the first one stated wins, since two Títulos retyping one attack is a
     * conflict no rules text resolves, and a stable answer is better than a silent last-wins.
     */
    public TitleAttackModifiers plus(@NonNull final TitleAttackModifiers other) {
        List<EffectChain> chains = new ArrayList<>(effectChains);
        chains.addAll(other.effectChains);
        return new TitleAttackModifiers(
                extraDamageDice + other.extraDamageDice,
                actionPointReduction + other.actionPointReduction,
                defenseType != null ? defenseType : other.defenseType,
                damageDescriptor != null ? damageDescriptor : other.damageDescriptor,
                criticalEffectOverride != null ? criticalEffectOverride : other.criticalEffectOverride,
                chains);
    }

    /**
     * Every held Título's {@link AventyrTitle#resolveAttackModifiers}, folded — what holder's
     * attack with attackSource against attackTarget gains. Ask <b>before</b> building the attack.
     */
    public static TitleAttackModifiers resolve(@NonNull final CombatantSheet holder, final AttackSource attackSource,
                                               final CombatantSheet attackTarget, final SceneContext sceneContext) {
        TitleAttackModifiers total = NONE;
        for (AventyrTitle title : holder.getCharacter().getAllTitles()) {
            total = total.plus(title.resolveAttackModifiers(holder, attackSource, attackTarget, sceneContext));
        }
        return total;
    }

    /**
     * Spends every budget of attacks an attack with attackSource uses up, across holder's
     * Títulos — call it <b>after</b> the attack resolves, since the roll itself reads the budget
     * ({@link AventyrTitle#consumeAttackCharges}).
     */
    public static void consumeCharges(@NonNull final CombatantSheet holder, final AttackSource attackSource) {
        holder.getCharacter().getAllTitles().forEach(title -> title.consumeAttackCharges(holder, attackSource));
    }
}
