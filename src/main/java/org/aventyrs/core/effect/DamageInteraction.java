package org.aventyrs.core.effect;

import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;

/**
 * The Damage stage of the Skill -&gt; Damage -&gt; EffectChain -&gt; CriticalEffect
 * pipeline (see {@code org.aventyrs.core.effect} package-info) — computes and applies
 * mitigated damage via {@link DamageService}, then reports the outcome.
 *
 * <p>Mirrors {@code AbstractSkillInteraction}'s cascading-overload shape: the 1-arg
 * {@link #applyTo(CharacterSheet)} required by {@link Interaction} (and the only one
 * {@link CharacterSheet#receiveInteraction} can call) delegates down with defaults — no
 * damage, no {@code SceneContext}, no {@code DamageType}/source, no next stage — so it's a
 * safe no-op when invoked generically. A caller that actually has damage to deal, optionally a
 * {@code SceneContext} (e.g. for {@code InitiativeAdvantage#TORRE_EM_MOVIMENTO}'s
 * Scene-conditioned RA/half-damage — {@code null} the same as every other
 * not-currently-in-a-Scene case elsewhere), optionally this hit's {@link DamageType} and its
 * source (the attacker's own {@link CharacterSheet} — e.g. for {@code
 * VigorAbility#RIGIDEZ_DA_MONTANHA}'s Dano-Físico-only, attacker-size-conditioned RD), and
 * optionally a next stage to hand off to once it exists, calls the 7-arg
 * {@link #applyTo(CharacterSheet, SceneContext, DamageType, CharacterSheet, int, boolean,
 * Interaction)} directly.
 */
public class DamageInteraction implements Interaction<CharacterSheet> {

    private final DamageService damageService;

    public DamageInteraction() {
        this(new DamageServiceImpl());
    }

    public DamageInteraction(final DamageService damageService) {
        this.damageService = damageService;
    }

    @Override
    public InteractionResult applyTo(final CharacterSheet target) {
        return applyTo(target, null, 0, false, null);
    }

    public InteractionResult applyTo(final CharacterSheet target, final int rawDamage, final boolean ignoreDamageReduction) {
        return applyTo(target, null, rawDamage, ignoreDamageReduction, null);
    }

    /** Same as {@link #applyTo(CharacterSheet, int, boolean)}, but also given sceneContext — see this class's own javadoc. */
    public InteractionResult applyTo(final CharacterSheet target, final SceneContext sceneContext, final int rawDamage, final boolean ignoreDamageReduction) {
        return applyTo(target, sceneContext, rawDamage, ignoreDamageReduction, null);
    }

    public InteractionResult applyTo(final CharacterSheet target, final int rawDamage, final boolean ignoreDamageReduction,
                                      final Interaction<CharacterSheet> nextInteraction) {
        return applyTo(target, null, rawDamage, ignoreDamageReduction, nextInteraction);
    }

    public InteractionResult applyTo(final CharacterSheet target, final SceneContext sceneContext, final int rawDamage, final boolean ignoreDamageReduction,
                                      final Interaction<CharacterSheet> nextInteraction) {
        return applyTo(target, sceneContext, null, null, rawDamage, ignoreDamageReduction, nextInteraction);
    }

    /**
     * Same as {@link #applyTo(CharacterSheet, SceneContext, int, boolean, Interaction)}, but
     * also given this hit's damageType and source, with no next stage — see this class's own
     * javadoc.
     */
    public InteractionResult applyTo(final CharacterSheet target, final SceneContext sceneContext,
                                      final DamageType damageType, final CharacterSheet source,
                                      final int rawDamage, final boolean ignoreDamageReduction) {
        return applyTo(target, sceneContext, damageType, source, rawDamage, ignoreDamageReduction, null);
    }

    /**
     * Applies {@code rawDamage} (mitigated per {@link DamageService#calculateFinalDamage(CharacterSheet,
     * SceneContext, DamageType, CharacterSheet, int, boolean)}) to {@code
     * target}. {@code nextInteraction} is only carried onto the returned {@link
     * InteractionResult#getNextInteraction()} when this hit actually dealt damage (final damage
     * &gt; 0) — a hit fully absorbed by RD/RA has nothing for a downstream {@link EffectChain}/
     * {@link CriticalEffect} to react to, so the chain ends here instead of forwarding into a
     * stage with nothing to work from.
     */
    public InteractionResult applyTo(final CharacterSheet target, final SceneContext sceneContext,
                                      final DamageType damageType, final CharacterSheet source,
                                      final int rawDamage, final boolean ignoreDamageReduction,
                                      final Interaction<CharacterSheet> nextInteraction) {
        int finalDamage = damageService.calculateFinalDamage(target, sceneContext, damageType, source, rawDamage, ignoreDamageReduction);
        target.applyDamage(finalDamage);

        InteractionResult.InteractionResultBuilder result = InteractionResult.builder()
                .resultStatus(target.getCharacter().getStatus())
                .resourceLossValue(finalDamage)
                .resourceLossType(ResourceType.HIT_POINTS);
        if (finalDamage > 0) {
            result.nextInteraction(nextInteraction);
        }
        return result.build();
    }
}
