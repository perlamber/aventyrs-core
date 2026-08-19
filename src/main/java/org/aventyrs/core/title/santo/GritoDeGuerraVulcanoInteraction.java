package org.aventyrs.core.title.santo;

import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.Skill;

import java.util.List;

/**
 * Grito de Guerra Vulcano's own activation — mirrors {@link AbencoadoPelaLuzInteraction}'s
 * cascading-overload shape (an {@link Interaction}&lt;{@link CharacterSheet}&gt; computing an
 * {@link InteractionResult}), but reports its grants via {@link
 * InteractionResult#getBlessings()} instead of mutating a resolved recipient list directly —
 * this ability's own rules text grants "a você e seus aliados adjacentes," the same
 * self-plus-allies shape {@code ArtesCompetencyAbility#DOM_BARDICO} already reports this way,
 * just with more than one {@link Blessing} at once (DOM_BARDICO only ever grants one). A caller
 * resolves the concrete recipients (the actor itself, plus {@code
 * SceneContext#getAlliesWithin(Range.ADJACENTE)} for each {@link TargetScope#SELF_AND_ALLIES}
 * entry) and calls {@code CharacterSheet#grantTemporaryBonus} on each — this class doesn't
 * mutate anything itself, the same "compute what, caller applies who" restraint {@code
 * InteractionResult}'s own {@code blessings} javadoc documents. Activated via {@link
 * Santo#activateGritoDeGuerraVulcano}, which validates the actor actually holds this Habilidade
 * before delegating here.
 *
 * <p>Every clause of this ability's own rules text is now reported as a real {@link Blessing},
 * including the "+2 em Defesas" one ({@link ModifierType#DEFESAS}) — even though nothing
 * consumes that {@code ModifierType} yet, since no Defesas stat/service exists in this core.
 * "Can't apply it yet doesn't mean can't compute/grant it yet" (see CLAUDE.md's own discipline
 * on this, and {@code ModifierType#ACTION_POINTS}'s identical already-grantable-but-inert
 * precedent) — reporting the Defesas blessing now means nothing has to change here once a
 * Defesas stat/service eventually reads {@code ModifierType.DEFESAS}-typed {@code
 * TemporaryBonus}es; the gap is entirely on the consuming side, not this class.
 */
public class GritoDeGuerraVulcanoInteraction implements Interaction<CharacterSheet> {

    private static final int VANTAGEM_ROUNDS = 2;
    private static final int DEFESAS_BONUS = 2;

    @Override
    public InteractionResult applyTo(final CharacterSheet actor) {
        return applyTo(actor, null);
    }

    /**
     * {@code sceneContext} isn't consulted by this ability's own rules text — every {@link
     * Blessing} reported here is {@link TargetScope#SELF_AND_ALLIES} regardless of context, the
     * concrete recipient resolution being entirely the caller's job — but it's still accepted,
     * for the same reason every Interaction in this core does: consistency with the established
     * cascading shape, and so a future Título ability that *does* need one doesn't need a
     * differently-shaped entry point.
     */
    public InteractionResult applyTo(final CharacterSheet actor, final SceneContext sceneContext) {
        String source = AbencoadoPelaLuzAbility.GRITO_DE_GUERRA_VULCANO.name();
        return InteractionResult.builder()
                .resultStatus(actor.getCharacter().getStatus())
                .blessings(List.of(
                        new Blessing(ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, Skill.ADVANTAGE_BONUS, VANTAGEM_ROUNDS, TargetScope.SELF_AND_ALLIES, source),
                        new Blessing(ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS, Skill.ADVANTAGE_BONUS, VANTAGEM_ROUNDS, TargetScope.SELF_AND_ALLIES, source),
                        new Blessing(ModifierType.DEFESAS, DEFESAS_BONUS, VANTAGEM_ROUNDS, TargetScope.SELF_AND_ALLIES, source)
                ))
                .build();
    }
}
