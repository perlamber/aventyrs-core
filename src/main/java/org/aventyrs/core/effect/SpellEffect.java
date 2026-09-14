package org.aventyrs.core.effect;

import org.aventyrs.core.magic.Spell;

/**
 * A Magia's own {@code Efeito:} line, made executable — the third category of {@link Effect},
 * beside {@link CriticalEffect} (an Efeito Crítico) and {@link EffectChain} (a Corrente de
 * Efeitos). Where those two are named by what <i>triggered</i> them, a Spell Effect is named by
 * what produced it: the Magia itself.
 *
 * <p>Its four subcategories divide by what the effect <i>does</i> — {@link OffensiveEffect},
 * {@link DefensiveEffect}, {@link HealingEffect}, {@link InvocationEffect}. Only Healing and
 * Defensive have concrete implementations today; see each marker's own javadoc for what blocks
 * the other two.
 *
 * <p><b>A Spell Effect applies for real, but a caller drives it.</b> Like every {@link Effect} it
 * is an {@code Interaction<CombatantSheet>}, so {@code applyTo} mutates the target exactly as
 * {@link Definhar}/{@link Sangramento} do. {@code SpellCastingService} only <i>hands one back</i>
 * on {@code SpellCastingResult#getSpellEffect()} — it never runs it, keeping the same
 * "this core computes, the caller decides when" boundary {@code
 * SpellCastingResult#getPrimaryDamage()} already documents:
 *
 * <pre>{@code
 * SpellCastingResult result = spellCastingService.castSpell(request);
 * // nothing has happened to the target yet
 * target.receiveInteraction(result.getSpellEffect());
 * }</pre>
 *
 * <p><b>One shape per branch, parameterized by rung — not one class per Magia.</b> The Magias of
 * a ramificação share an effect that deepens as the tree climbs, so a concrete Spell Effect is
 * built from authored data ({@code Spell#getHealing()}, {@code Spell#getCleansedConditions()})
 * rather than subclassed per constant. {@code SpellHealingEffect} covers the whole of Vida's
 * principal branch, from Descanso Mínimo at Semente to a full recovery at Florescente.
 */
public interface SpellEffect extends Effect {

    /** The Magia that produced this effect — its rung, branch and prose all reachable from here. */
    Spell getSpell();
}
