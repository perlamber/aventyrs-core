package org.aventyrs.core.magic;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.grid.GridPosition;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * Everything one Magia invocation needs. A Combatant or position target is supplied only when
 * {@link SpellTargeting} for the version being cast permits it.
 *
 * <p><b>One thing a cast chooses that the Magia does not fix</b>: which <em>version</em> is being
 * cast, {@link #useAlternateVersion}. That single flag also settles which effect the cast uses,
 * because a Magia and its {@code Efeito Alternativo} are two separate {@link Spell}s each carrying
 * its own effect columns — so there is no second "which effect" question to ask.
 */
@Getter
@Builder
public class SpellCastRequest {
    @NonNull
    private final CombatantSheet caster;
    @NonNull
    private final Spell spell;
    @NonNull
    private final SceneContext sceneContext;
    @NonNull
    private final Scene scene;
    private final CombatantSheet combatantTarget;
    private final GridPosition positionTarget;

    /**
     * Cast {@link #spell}'s {@code Efeito Alternativo} rather than its base version — "um
     * personagem que aprenda a versão base automaticamente aprende sua segunda versão", so both
     * are always available to whoever knows the Magia.
     *
     * <p>The service resolves {@code Spell#getAlternateVersion()} itself and casts <em>that</em>,
     * so the second version's own Tempo de Ativação, Alcance and GD are what get validated and
     * reported. Refused with {@code NO_ALTERNATE_SPELL_VERSION} when the Magia has none.
     *
     * <p>Passing the alternate as {@link #spell} directly still works — it is a {@link Spell} like
     * any other — but this spares the caller the lookup.
     *
     * <p><b>It is also what says which effect the cast uses.</b> The two versions author separate
     * effect columns, so choosing the version chooses the effect; {@code
     * org.aventyrs.core.effect.SpellEffectFactory} then simply builds whatever the chosen version
     * authors, with no selector of its own.
     */
    private final boolean useAlternateVersion;
}
