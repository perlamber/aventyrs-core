package org.aventyrs.core.magic.invocation;

import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.subordinate.Subordinate;
import org.aventyrs.core.subordinate.SubordinateBenefit;

/**
 * Turns a cast of any invocation Magia into what it asks the Cena for (core 0.1.7.6), by the {@link SpellInvocation}
 * the Magia names — the one place that knows which {@link NatureInvocationService} or {@link UndeadInvocationService}
 * method each one is, so a caller never recognises a Magia by its constant.
 */
public interface SpellInvocationService {

    /**
     * The PM invocation adds to its Magia's own — Predador Regional's +1, Canção de Flora's per animal, Açougueiro's
     * +5; 0 for the rest. {@code SpellCastingService#resolveManaCost} already includes it.
     */
    int extraManaCost(SpellInvocation invocation, CombatantSheet caster, InvocationChoices choices);

    /**
     * What a landed cast of invocation asks for — creatures, a spawner, or a Subordinado to pick a benefit for. Rolls
     * what the Magia rolls at the cast with {@link InvocationCast#roller()}, and folds in the caster's Bruxo choices.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException when a body or an animal is needed and none was named
     */
    InvocationOrder plan(SpellInvocation invocation, InvocationCast cast);

    /**
     * Makes body caster's Subordinado for a {@link SpellInvocation.Shape#SUBORDINATE} invocation — Cativar Animal or
     * Falsa Matilha on an animal judged by animalType (its own when {@code null}), Servo Cadavérico on a corpse, its
     * Escudeiro Cadavérico when effectChainTriggered. scene is only read by Servo
     * Cadavérico.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException for any other invocation, or when the rules refuse it
     */
    Subordinate subordinate(SpellInvocation invocation, Scene scene, CombatantSheet caster, CombatantSheet body,
                            org.aventyrs.core.race.CreatureType animalType, SubordinateBenefit benefit,
                            boolean effectChainTriggered, SceneContext sceneContext);
}
