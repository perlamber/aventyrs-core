package org.aventyrs.core.sheet;

import lombok.NonNull;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * One hit an area effect deals to one character — reported, never applied: this core holds no
 * positions and owns nobody's sheet, so the caller runs each through {@code DamageService#applyDamage}
 * (or hands it to the client that owns the target). A Grito Espinhoso yields one per enemy in
 * Distância Curta; Cataclismo Elemental one per chosen element per character.
 *
 * @param target     who takes it
 * @param amount     raw damage, before the target's own mitigation — already doubled where the clause
 *                   says "este dano é dobrado em … adjacentes"
 * @param descriptor its type and element, for the mitigation stages
 * @param source     who deals it
 */
public record AreaDamage(@NonNull CombatantSheet target, int amount, @NonNull DamageDescriptor descriptor,
                         @NonNull CombatantSheet source) {

    /** Cataclismo Elemental: "dano … igual ao número de Elementos escolhido" — 1 per element. */
    static final int CATACLYSM_DAMAGE_PER_ELEMENT = 1;

    /**
     * Cataclismo Elemental's additional damage for one Magia de Duração instantânea or one Grito de
     * Guerra of caster's: "dano Mágico Elemental igual ao número de Elementos escolhido a todos os
     * personagens em Distância Curta, este dano é dobrado em personagens adjacentes". One hit of 1 per
     * chosen element — each resisted on its own — to <b>every</b> character in Distância Curta, allies
     * included, doubled on those adjacent.
     *
     * @return {@code null} when caster has no Cataclismo running or no sceneContext is in hand — the
     *         shape the result fields use for "none"
     */
    public static List<AreaDamage> cataclysm(@NonNull final CombatantSheet caster, final SceneContext sceneContext) {
        Optional<Frenzy> frenzy = caster.getOwnFrenzy()
                .filter(held -> held.hasMode(FrenzyMode.CATACLISMO_ELEMENTAL))
                .filter(held -> !held.getCataclysmElements().isEmpty());
        if (frenzy.isEmpty() || sceneContext == null) {
            return null;
        }
        List<AreaDamage> hits = new ArrayList<>();
        Stream.concat(sceneContext.getAlliesWithin(Range.DISTANCIA_CURTA).stream(),
                        sceneContext.getEnemiesWithin(Range.DISTANCIA_CURTA).stream())
                .distinct()
                .filter(target -> target != caster)
                .forEach(target -> {
                    int amount = CATACLYSM_DAMAGE_PER_ELEMENT
                            * (sceneContext.getDistanceTo(target) == Range.ADJACENTE ? 2 : 1);
                    for (ElementalType element : frenzy.get().getCataclysmElements()) {
                        hits.add(new AreaDamage(target, amount, new DamageDescriptor(DamageType.ELEMENTAL, element),
                                caster));
                    }
                });
        return hits.isEmpty() ? null : List.copyOf(hits);
    }
}
