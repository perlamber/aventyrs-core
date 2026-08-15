package org.aventyrs.core.effect;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.character.services.DamageServiceImpl;
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
 * damage, no next stage — so it's a safe no-op when invoked generically. A caller that
 * actually has damage to deal, and optionally a next stage to hand off to once it
 * exists, calls the 4-arg {@link #applyTo(CharacterSheet, int, boolean, Interaction)}
 * directly.
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
        return applyTo(target, 0, false, null);
    }

    public InteractionResult applyTo(final CharacterSheet target, final int rawDamage, final boolean ignoreDamageReduction) {
        return applyTo(target, rawDamage, ignoreDamageReduction, null);
    }

    /**
     * Applies {@code rawDamage} (mitigated per {@link DamageService#calculateFinalDamage})
     * to {@code target}. {@code nextInteraction} is only carried onto the returned
     * {@link InteractionResult#getNextInteraction()} when this hit actually dealt damage
     * (final damage &gt; 0) — a hit fully absorbed by RD/RA has nothing for a downstream
     * {@link EffectChain}/{@link CriticalEffect} to react to, so the chain ends here
     * instead of forwarding into a stage with nothing to work from.
     */
    public InteractionResult applyTo(final CharacterSheet target, final int rawDamage, final boolean ignoreDamageReduction,
                                      final Interaction<CharacterSheet> nextInteraction) {
        Character character = target.getCharacter();
        int finalDamage = damageService.calculateFinalDamage(character, rawDamage, ignoreDamageReduction);
        target.applyDamage(finalDamage);

        InteractionResult.InteractionResultBuilder result = InteractionResult.builder()
                .resultStatus(character.getStatus())
                .resourceLossValue(finalDamage)
                .resourceLossType(ResourceType.HIT_POINTS);
        if (finalDamage > 0) {
            result.nextInteraction(nextInteraction);
        }
        return result.build();
    }
}
