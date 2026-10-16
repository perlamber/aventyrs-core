package org.aventyrs.core.scene;

import lombok.Builder;
import lombok.NonNull;
import lombok.Singular;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;

import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * An aura a monster emits (core 0.1.7.4) — the generic shape behind the 20261015 manual's Aura do Paradoxo, Aura
 * Elemental, Aura dos Ventos Caóticos, Dádiva de Gaea, Presença do Ceifeiro, Mestre das Marionetes and Flagelo de
 * Lacerto's fear. Produced by a Habilidade Monstruosa ({@code MonstrousAbility#resolveAuras}) while it holds — a
 * passive one always, an emitted one while its Efeito Ativo's state runs.
 *
 * <p>Two halves, both caller-driven because this core does no geometry: <b>coverage</b> ({@code
 * Scene#refreshMonsterAuras}, after any movement) grants {@link #whileInside} and {@link #wardedConditions} to whoever
 * stands inside and takes them back from whoever left — the {@link ProjectedAura} ledger, aimed at any reach; and the
 * <b>pulse</b> ({@code Scene#pulseMonsterAuras}, at the holder's Turn start) turns {@link #pulse} into {@link AuraHit}s.
 *
 * @param source                         what grants it — the key a renewal replaces by
 * @param radius                         how far it reaches
 * @param reach                          whom it reaches
 * @param whileInside                    open-ended figures granted while inside ("-2PA")
 * @param wardedConditions               Condições those inside are immune to (Dádiva de Gaea's Medo and Possessão)
 * @param holderActionPointsWhileOccupied PA the holder gains while at least one enemy is inside (Paradoxo's +3)
 * @param exitLingerRounds               Rodadas {@link #whileInside} keeps biting after leaving ("recuperados 1 Rodada
 *                                       após os personagens deixarem a Área")
 * @param emptyRoundsToEnd               pulses with nobody else inside after which it ends (0 = never)
 * @param difficultTerrain               its area is Terreno Difícil (Ventos Caóticos) — reported for the caller's map
 * @param exempt                         who it never affects ("Monstros que possuam Aura do Paradoxo são imunes")
 * @param pulse                          what it does each Rodada, or {@code null}
 */
@Builder
public record MonsterAura(@NonNull String source, @NonNull Range radius, @NonNull AuraReach reach,
                          @Singular("whileInside") List<AuraBonus> whileInside,
                          @Singular Set<ConditionType> wardedConditions,
                          int holderActionPointsWhileOccupied, int exitLingerRounds, int emptyRoundsToEnd,
                          boolean difficultTerrain, Predicate<CombatantSheet> exempt, AuraPulse pulse) {

    public MonsterAura {
        whileInside = whileInside == null ? List.of() : List.copyOf(whileInside);
        wardedConditions = wardedConditions == null ? Set.of() : Set.copyOf(wardedConditions);
        exempt = exempt == null ? sheet -> false : exempt;
    }

    /** Whether target is one this aura can reach at all, distance aside. */
    public boolean reaches(final CombatantSheet holder, final CombatantSheet target, final SceneContext holderContext) {
        if (exempt.test(target)) {
            return false;
        }
        if (target == holder) {
            return reach == AuraReach.SELF_AND_MONSTER_ALLIES;
        }
        return switch (reach) {
            case ENEMIES -> holderContext.getEnemies().contains(target);
            case LIVING_ENEMIES -> holderContext.getEnemies().contains(target) && !target.hasUndeadAnatomy();
            case ALL_OTHERS -> true;
            case LIVING_OTHERS -> !target.hasUndeadAnatomy();
            case SELF_AND_MONSTER_ALLIES -> holderContext.getAllies().contains(target) && target.isMonster();
        };
    }
}
