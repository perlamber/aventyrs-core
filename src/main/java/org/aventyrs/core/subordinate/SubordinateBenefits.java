package org.aventyrs.core.subordinate;

import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;

import java.util.List;

/**
 * How many of a benefit reach a combatant right now (core 0.0.92) — its own Subordinados', plus every Prodigioso one an
 * ally commands ("Subordinados Prodigiosos concedem seus benefícios a todos os Personagens do Grupo de Jogadores"; the
 * allies are {@code SceneContext#getAllies()}, characters only). "Os benefícios de Subordinados comuns e prodigiosos se
 * acumulam", so the count is a sum. A combat-only benefit needs a Cena de Combate; a {@code null} context withholds it.
 *
 * <p>⚠️ Since core 0.1.5.6 a monster can command Subordinados too (the GM grants them), and a Prodigioso one reaches
 * allies of its commander's own kind — a character's reaches the group's characters, a monster's the group's monsters
 * ({@link #sameKind}).
 */
public final class SubordinateBenefits {

    private SubordinateBenefits() {
    }

    /** Every Subordinado sheet commands. */
    public static List<Subordinate> of(final CombatantSheet sheet) {
        return sheet == null ? List.of() : sheet.getRunningEffects().stream()
                .filter(Subordinate.class::isInstance)
                .map(Subordinate.class::cast)
                .toList();
    }

    /** How many benefit reach sheet. */
    public static int count(final CombatantSheet sheet, final SceneContext sceneContext, final SubordinateBenefit benefit) {
        if (sheet == null || benefit.isCombatOnly() && (sceneContext == null || !sceneContext.isCombatScene())) {
            return 0;
        }
        long own = of(sheet).stream().filter(held -> held.getBenefit() == benefit).count();
        long allied = sceneContext == null ? 0 : sceneContext.getAllies().stream()
                .filter(ally -> ally != sheet && sameKind(sheet, ally))
                .flatMap(ally -> of(ally).stream())
                .filter(held -> held.isProdigious() && held.getBenefit() == benefit)
                .count();
        return (int) (own + allied);
    }

    /**
     * Whether a Prodigioso Subordinado held by ally reaches sheet (core 0.1.5.6): both characters, or both not — "todos
     * os Personagens do Grupo" read for a monster commander as its group's monsters.
     */
    public static boolean sameKind(final CombatantSheet sheet, final CombatantSheet ally) {
        return (sheet instanceof CharacterSheet) == (ally instanceof CharacterSheet);
    }
}
