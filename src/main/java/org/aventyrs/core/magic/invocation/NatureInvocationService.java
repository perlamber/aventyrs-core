package org.aventyrs.core.magic.invocation;

import org.aventyrs.core.scene.Scene;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.scene.SummonSpawner;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Player;
import org.aventyrs.core.subordinate.Subordinate;
import org.aventyrs.core.subordinate.SubordinateBenefit;
import org.aventyrs.core.util.DiceRoller;

import java.util.List;

/**
 * What each ALIADOS DA NATUREZA Magia does once it is cast (core 0.0.92) — the creature it invokes into the {@link
 * Scene}, for how long, and what it may not coexist with. Called after {@code SpellCastingService} has resolved the cast
 * and the caller has paid it: this places the summons ({@link Scene#addSummons}) and reports any extra PM, it rolls
 * nothing but a Lacerto creature's powers.
 *
 * <p>Every creature is a {@code monster.summon.NatureSummon} spawned at the caster's Graduação in Domínio do Mana and
 * run by gm's {@link Player} record, with its caster's player controlling it (table ruling, 2026-10-01).
 *
 * <p>Exclusivity, each a {@link Scene#addSummons} group:
 * <ul>
 *   <li>{@link #FAUNA_GROUP} — "Apenas 1 Aliado da Natureza … por vez" and "Não é possível manter invocações de Canção
 *       de Flora e Aliados da Natureza simultaneamente": a new cast of either sends the previous one's animals away at
 *       the end of the caster's Turn (⚠️ Canção's "dissipa a conjuração anterior" read the same way);</li>
 *   <li>{@link #EXPERIMENTO_GROUP} — "Não é possível manter mais de uma criatura invocada por Experimento";</li>
 *   <li>{@link #ORGULHO_GROUP} — the Orgulho's, which Laboratório de Lacerto shares ("não é cumulativo com a magia
 *       Orgulho de Lacerto").</li>
 * </ul>
 * Totem de Gaea's Predadores and the Anciente join no group: "É possível combinar animais invocados com Totem de Gaea
 * com os invocados por Canção de Flora ou Aliados da Natureza", and nothing limits the Anciente.
 */
public interface NatureInvocationService {

    String FAUNA_GROUP = "ALIADOS_DA_NATUREZA";
    String EXPERIMENTO_GROUP = "EXPERIMENTO_DE_LACERTO";
    String ORGULHO_GROUP = "ORGULHO_DE_LACERTO";

    /** Aliados da Natureza, Experimento de Lacerto and Totem de Gaea: "Duração: 3 Rodadas". */
    int ROUNDS = 3;

    /** "Concentração + 2 Rodadas" — Canção de Flora, Orgulho de Lacerto, Despertar Anciente. */
    int TRAILING_ROUNDS = 2;

    /** Predador Regional: "aumentar o Custo de Conjuração em +1PM". */
    int PREDATOR_MANA_COST = 1;

    /** Canção de Flora: "+2PM para cada animal adicional". */
    int ADDITIONAL_ANIMAL_MANA_COST = 2;

    /** Canção de Flora: "+1PM para cada animal fortalecido". */
    int STRENGTHENED_ANIMAL_MANA_COST = 1;

    /** Fauna Flora: "1 Aliado da Natureza para cada 3 Graduações em Conhecimento: Natureza". */
    int FAUNA_FLORA_GRADUATIONS_PER_ANIMAL = 3;

    /** Laboratório de Lacerto: "invoca simultaneamente 2 Experimentos de Lacerto". */
    int LABORATORY_EXPERIMENTS = 2;

    /** Totem de Gaea's grants to animal allies: "+1PA, Força +2, Vigor +2 … GD … -1 nível". */
    int TOTEM_ACTION_POINTS = 1;
    int TOTEM_ATTRIBUTE_BONUS = 2;
    int TOTEM_DIFFICULTY_REDUCTION = 1;

    /** {@link #invokeAliado}'s creatures, unplaced — caster's Graduação in Domínio do Mana sets their tiers. */
    InvocationPlan planAliado(CombatantSheet caster, boolean predator);

    /**
     * plan as caster's Títulos change it, given the per-cast options (core 0.1.2) — every creature carries the {@link
     * SummonEnhancement} the Títulos resolve ({@code AventyrTitle#resolveSummonEnhancement}); Invocação Dupla doubles
     * the creatures, halves the Duração (⚠️ floored, never below 1 Rodada) and doubles the Concentração upkeep; and
     * the options' price is reported: +1PA (Multiplicador de PV), +2PD (Atributo), +2PA (Dupla). Nothing is spent.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code REQUIRED_TITLE_TRAIT_NOT_HELD} for an option no
     *         held trait permits, or anything a Título refuses
     */
    InvocationPlan enhance(InvocationPlan plan, CombatantSheet caster, InvocationOptions options);

    /** {@link #singCancaoDeFlora}'s pack, unplaced. */
    InvocationPlan planCancaoDeFlora(CombatantSheet caster, int additionalAnimals, int strengthened, boolean faunaFlora);

    /** {@link #invokeExperimento}'s creature, unplaced, its power rolled on roller. */
    InvocationPlan planExperimento(CombatantSheet caster, DiceRoller roller);

    /** {@link #invokeOrgulho}'s creature, unplaced, its powers rolled on roller. */
    InvocationPlan planOrgulho(CombatantSheet caster, DiceRoller roller);

    /** {@link #invokeLaboratorio}'s two Experimentos, unplaced. */
    InvocationPlan planLaboratorio(CombatantSheet caster, DiceRoller roller);

    /** {@link #awakenAnciente}'s Anciente, unplaced. */
    InvocationPlan planAnciente(CombatantSheet caster);

    /** The Predador Totem de Gaea raises each Rodada, unplaced; {@link InvocationPlan#rounds()} is each one's Duração. */
    InvocationPlan planTotemPredator(CombatantSheet caster);

    /** Aliados da Natureza — an Aliado, or with predator (its Efeito Alternativo, +1PM) a Predador Regional. */
    Invocation invokeAliado(Scene scene, CombatantSheet caster, boolean predator, Player gm);

    /**
     * Canção de Flora — "1 Predador Regional e 1 Aliado da Natureza", plus additionalAnimals more Aliados (+2PM each),
     * strengthened of the Aliados made Predadores (+1PM each), and, when its Corrente Fauna Flora fired, one more Aliado
     * per {@link #FAUNA_FLORA_GRADUATIONS_PER_ANIMAL} Graduações in Conhecimentos (⚠️ counted only with the Natureza
     * Especialização). Held by Concentração.
     */
    Invocation singCancaoDeFlora(Scene scene, CombatantSheet caster, int additionalAnimals, int strengthened,
                                 boolean faunaFlora, Player gm);

    /** Experimento de Larcerto — its one power rolled on roller. */
    Invocation invokeExperimento(Scene scene, CombatantSheet caster, DiceRoller roller, Player gm);

    /** Orgulho de Lacerto — its two powers rolled on roller. Held by Concentração. */
    Invocation invokeOrgulho(Scene scene, CombatantSheet caster, DiceRoller roller, Player gm);

    /** Laboratório de Lacerto (Orgulho's Efeito Alternativo) — two Experimentos, each rolling its power. */
    Invocation invokeLaboratorio(Scene scene, CombatantSheet caster, DiceRoller roller, Player gm);

    /** Despertar Anciente de Gaea. Held by Concentração. */
    Invocation awakenAnciente(Scene scene, CombatantSheet caster, Player gm);

    /**
     * Totem de Gaea — a Predador Regional now and one more at each Rodada boundary for its {@link #ROUNDS} Rodadas, each
     * lasting ⚠️ an Aliados da Natureza's {@link #ROUNDS} (they "são Predadores Regionais criados pela magia Aliados da
     * Natureza").
     */
    SummonSpawner raiseTotem(Scene scene, CombatantSheet caster, Player gm);

    /**
     * Totem de Gaea's blessing for one Rodada on each of animals — the caller names the animal allies within
     * Distância Longa of the Totem, since this core holds no positions. Grants +1PA, Força +2, Vigor +2 and -1 nível on
     * every Perícia. ⚠️ Força and Vigor reach only Perícia rolls governed by them: PV and the ½ Força dano term read the
     * permanent Atributo.
     */
    void blessFromTotem(List<? extends CombatantSheet> animals);

    /**
     * Cativar Animal — the touched animal becomes caster's Subordinado of benefit's grade (Cavaleiro or Torre) for the
     * Duração; with Falsa Matilha it is Prodigioso and the Duração is halved (3 → ⚠️ 2, rounded up as a halved Duração
     * is elsewhere). Refused for a target that is not an animal, or one already cativado since its last Descanso.
     *
     * <p>TODO the animal leaving its side of the Cena while it serves is the caller's — a Subordinado takes no actions,
     * so the client stops giving it Turns. TODO Domar's "realizando inclusive ações contrárias aos seus instintos" is
     * narrative.
     */
    Subordinate captivate(CombatantSheet caster, CombatantSheet animal, SubordinateBenefit benefit,
                          boolean falsaMatilha, SceneContext sceneContext);

    /**
     * {@link #captivate}, judged on animalType rather than animal's own — for a caller holding only a stand-in for a
     * foe another client runs, whose real kind it read off the foe's stat block (core 0.0.96). The "until a Descanso"
     * mark lands on the sheet passed.
     */
    Subordinate captivate(CombatantSheet caster, CombatantSheet animal, org.aventyrs.core.race.CreatureType animalType,
                          SubordinateBenefit benefit, boolean falsaMatilha, SceneContext sceneContext);
}
