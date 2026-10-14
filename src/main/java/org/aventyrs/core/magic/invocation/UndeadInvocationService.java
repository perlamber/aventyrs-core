package org.aventyrs.core.magic.invocation;

import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.SceneSummon;
import org.aventyrs.core.scene.SummonSpawner;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.subordinate.Subordinate;
import org.aventyrs.core.subordinate.SubordinateBenefit;
import org.aventyrs.core.util.DiceRoller;

/**
 * The REANIMAR tree's invocations (core 0.1.6.25; rulings in {@code docs/plans/undead-invocation-plan.md}) — the
 * {@link NatureInvocationService} twin. Every creature takes the Anatomia de Morto-Vivo; one raised from a corpse
 * replaces the corpse's token and leaves it behind again when it goes ({@code Scene#raise}, {@code
 * Scene#dismissSummon}); every invocation is refused past the caster's Foco ({@link SummonLimit}) but Festim dos Mortos,
 * whose excess rises uncontrolled.
 *
 * <p>As with the nature tree, a plan is what a client sends a server; the {@code Scene}-taking methods place it in a
 * core {@code Scene}.
 */
public interface UndeadInvocationService {

    /** Reanimar's and Criar Carniçal's "Concentração + 2 Rodadas". */
    int TRAILING_ROUNDS = 2;

    /** Açougueiro: "aumentar o custo de Conjuração desta magia em +5PM". */
    int BUTCHER_EXTRA_MANA = 5;

    /** Servo Cadavérico: the Escudeiro Cadavérico's "nas 2 primeiras Rodadas". */
    int ESCUDEIRO_CADAVERICO_ROUNDS = 2;

    /** The source every Servo Cadavérico Subordinado carries. */
    String SERVO_CADAVERICO = "Servo Cadavérico";

    /** The exclusivity group of an Açougueiro Carniçal — "não poderá controlar mais nenhum outro Carniçal". */
    String BUTCHER_GROUP = "ACOUGUEIRO";

    /** Reanimar: a Zumbi from corpse, held by Concentração + 2. */
    UndeadPlan planReanimar(CombatantSheet caster, CombatantSheet corpse);

    /** Criar Carniçal: a Carniçal from corpse, Concentração + 2 — or, as Açougueiro, with no Duração and +5PM. */
    UndeadPlan planCarnical(CombatantSheet caster, CombatantSheet corpse, boolean butcher);

    /**
     * Places plan in scene, raised from corpse: the corpse must be raisable ({@link RaiseDeadService}) and the caster
     * have room ({@link SummonLimit}); an Açougueiro Carniçal and any other Carniçal of the caster's exclude each other.
     */
    SceneSummon raise(Scene scene, CombatantSheet caster, CombatantSheet corpse, UndeadPlan plan, Player gm);

    /**
     * Reanimar's Servo Cadavérico: corpse becomes caster's Subordinado — a Cavaleiro or a Torre (benefit's grade),
     * held by Concentração + 2, counting as Prodigioso for the first 2 Rodadas when escudeiroCadaverico (its
     * Corrente). The corpse stays on the board as the servant (⚠️ reading), and its raise count goes up by one.
     */
    Subordinate servoCadaverico(Scene scene, CombatantSheet caster, CombatantSheet corpse, SubordinateBenefit benefit,
                                boolean escudeiroCadaverico, SceneContext sceneContext);

    /**
     * Festim dos Mortos: opens the Sarcófago Negro (or, hordaFaminta, the Jazigo Profano) — one creature now and one
     * each Rodada for 3 Rodadas from roller's 1d6 ({@link FestimDosMortos}).
     */
    SummonSpawner openSarcophagus(Scene scene, CombatantSheet caster, boolean hordaFaminta, DiceRoller roller,
                                  Player gm);

    /**
     * A Habilidade Monstruosa's invocation (core 0.1.6.26) — Senhor dos Mortos' Procissão dos Desencarnados, the option
     * of {@code MonstrousActiveAbility#getSummonOptions()} its monster picked: stock Carrascos Moribundos ({@code
     * CarrascoMorimbundo#stock}) placed as monster's invocações. "A quantidade de mortos-vivos controlados ou invocados
     * pelo Senhor dos Mortos não pode superar seu Valor de Carisma" — refused past its Carisma.
     */
    java.util.List<SceneSummon> invokeCarrascos(Scene scene, CombatantSheet monster,
                                                org.aventyrs.core.monster.model.MonsterSummoning summoning, Player gm);

    /**
     * Cuspidor de Pragas: "Enquanto em combate, a Cada Rodada Ímpar dois Carrascos Moribundos do tipo Deviante são
     * invocados … Quando a Cena de Combate termina ou o Cuspidor de Pragas é destruído os mortos invocados são
     * destruídos." The caller starts it when combat begins (or when the Cuspidor joins one) and stops it with {@code
     * Scene#endSpawnersOf} when the Cuspidor is destroyed; the end of combat stops it on its own. ⚠️ "Ímpar" is the
     * table's count from 1 — core Rodadas 0, 2, 4…
     */
    SummonSpawner startPlagueSpitting(Scene scene, CombatantSheet spitter, Player gm);
}
