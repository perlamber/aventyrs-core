package org.aventyrs.core.scene;

import org.aventyrs.core.modifier.ModifierType;

/**
 * A passive Aura one combatant projects onto the allies around them — a continuous bonus with no
 * activation, no cost and no Duração, which lasts exactly as long as the recipient stays in range.
 * {@code AbstractCombatantSheet#resolveProjectedAuras} produces these; {@link
 * Scene#refreshProjectedAuras} grants and revokes them.
 *
 * <p>The live one is Santo's Título-Primário clause: "aliados adjacentes a você recebem Bônus em
 * Defesas iguais à metade dos Bônus que você receber em função do Efeito Base deste Título."
 *
 * <p><b>Why this is granted when every other ally-facing passive in this core is scanned.</b> The
 * usual rule ({@code AventyrTitleAbility#resolveAllyAbsoluteDamageReduction}, Bastião dos
 * Necessitados) is to recompute at the moment the recipient needs the number, so nothing has to be
 * revoked when someone moves. That works because those figures derive from the <i>recipient's</i>
 * own situation. This one does not: the amount is half of what the <b>holder</b> gets, and the
 * holder's own bonus counts the holder's adjacent allies. A scan running from the recipient can
 * only see the recipient's neighbours, so it would quietly undercount. Resolving holder-side is the
 * only way to get the figure right, and a holder-side resolution has nowhere to put its answer
 * except onto the recipient — hence a grant, with {@link Scene} owning the revocation the way it
 * already does for initiative Blessings.
 *
 * <p>Deliberately <b>not</b> an {@link ActiveAura}. That type is the provocation Aura: a binding
 * ledger with a forced attack target, a per-Rodada Desvantagem, a Descanso-scoped memory and a
 * Rodada countdown, whose bindings persist once made even after the foe leaves range. This needs
 * none of it and wants the opposite range behaviour — a recipient who walks away stops benefiting
 * immediately. Two things called "Aura" in the fiction that share no mechanism.
 *
 * @param modifierType what the bonus modifies — {@code DEFESAS} for Santo's
 * @param value        the per-ally amount, already resolved from the holder's own context
 * @param radius       how far it reaches; {@code ADJACENTE} for Santo's "aliados adjacentes"
 * @param source       which trait projects it, so a re-grant replaces rather than stacks
 */
public record ProjectedAura(ModifierType modifierType, int value, Range radius, String source) {

    public ProjectedAura {
        if (modifierType == null || radius == null || source == null) {
            throw new IllegalArgumentException("A ProjectedAura needs a modifierType, a radius and a source");
        }
    }
}
