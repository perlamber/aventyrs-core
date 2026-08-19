package org.aventyrs.core.title.santo;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.rest.RestService;
import org.aventyrs.core.rest.RestServiceImpl;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;

/**
 * Abençoado pela Luz's own touch-heal-or-cure effect — mirrors an ordinary Perícia's
 * {@code <Skill>Interaction} shape (an {@link Interaction}&lt;{@link CharacterSheet}&gt;
 * computing an {@link InteractionResult}) minus anything Perícia-roll-specific: no {@code
 * skillRollBonus}, no dice, no {@code SkillRoll} parameter at all — this is a direct effect on
 * {@code target} (the touched character), not a Perícia test. Activated via {@link
 * Santo#activateAbencoadoPelaLuz}, which validates the actor actually holds this
 * Especialização before delegating here — this class itself doesn't check that, the same way
 * a {@code <Skill>Interaction} doesn't re-check the trained-Skill lookup its caller already
 * resolved.
 *
 * <p>{@code sceneContext} isn't consulted by this ability's own rules text (it names no
 * Scene-conditioned fact) but is still accepted, for the same reason every Interaction in this
 * core does: consistency with the established cascading shape, and so a future Título ability
 * that *does* need one doesn't need a differently-shaped entry point.
 *
 * <p>This is a single, hand-written class, not yet a generalized "Título ability activation"
 * base class — mirrors {@code AbstractSkillInteraction}'s own history (built only once
 * multiple Perícias needed the identical shape). Only one Título ability effect is real enough
 * to activate today; extract a shared base once a second one needs the same cascade.
 */
public class AbencoadoPelaLuzInteraction implements Interaction<CharacterSheet> {

    private final RestService restService;

    public AbencoadoPelaLuzInteraction() {
        this(new RestServiceImpl());
    }

    public AbencoadoPelaLuzInteraction(final RestService restService) {
        this.restService = restService;
    }

    /**
     * Safe no-op default (mirrors {@code DamageInteraction#applyTo(CharacterSheet)}'s own
     * "insufficient information given" shape) — {@code chooseHeal} genuinely needs a real
     * choice to mean anything, so the bare 1-arg form (the only one {@code
     * CharacterSheet#receiveInteraction} can call) picks neither branch rather than guessing.
     */
    @Override
    public InteractionResult applyTo(final CharacterSheet target) {
        return applyTo(target, null, false);
    }

    /**
     * chooseHeal picks between Abençoado pela Luz's two branches: {@code true} heals target as
     * if they'd taken a Descanso Curto — real, via {@link SantoSpecialization
     * #resolveShortRestHealAmount} — reported as a {@link InteractionResult#getResourceGainValue()}/
     * {@link InteractionResult#getResourceGainType()} pair. {@code false} attempts to remove a
     * Malefício — still TODO'd (see that same method's own javadoc for the missing
     * classification) — so this branch applies nothing and reports an inert result.
     */
    public InteractionResult applyTo(final CharacterSheet target, final SceneContext sceneContext, final boolean chooseHeal) {
        Character character = target.getCharacter();
        if (!chooseHeal) {
            // TODO: Malefício removal — see SantoSpecialization#resolveShortRestHealAmount's
            // own citation for the missing Malefício/Encantamento/Maldição/Doença
            // classification this branch needs before it can do anything real.
            return InteractionResult.builder()
                    .resultStatus(character.getStatus())
                    .build();
        }
        int healed = SantoSpecialization.ABENCOADO_PELA_LUZ.resolveShortRestHealAmount(character, restService);
        target.heal(healed);
        return InteractionResult.builder()
                .resultStatus(character.getStatus())
                .resourceGainValue(healed)
                .resourceGainType(ResourceType.HIT_POINTS)
                .build();
    }
}
