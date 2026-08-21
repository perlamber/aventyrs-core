/**
 * Resolving an attack against a character — the target side of a combat exchange.
 *
 * <h2>Why this package exists</h2>
 *
 * Every other piece of an attack already had a home: {@code AtaqueADistanciaInteraction}/{@code
 * AtaqueCorpoACorpoInteraction} compute an attacker's roll, {@code EsquivaEApararInteraction}
 * computes a defense roll, {@code DefenseService} computes DF/DM, and {@code DamageService}
 * mitigates damage. What was missing was the step that connects them, which both Ataque
 * Interactions' javadocs named explicitly: <i>"the rules text compares this roll against a
 * target's DF or DM rather than a fixed GD, but that target-side lookup/conversion is left to a
 * layer above this core."</i> {@link org.aventyrs.core.combat.AttackReceiver} is that layer.
 *
 * <h2>The resolution model</h2>
 *
 * <b>This game's dice are always rolled by the player.</b> An attack against a character is
 * therefore not resolved as an attacker's roll at all — it's resolved as <i>the defender's own
 * Esquiva e Aparar roll</i>, against the Grau de Dificuldade the attack presents, resisting with
 * either DF or DM ({@code org.aventyrs.core.character.DefenseType}). A Defesa is consequently a
 * pool of bonuses feeding that roll, not a passive score, which is how this codebase already
 * modeled it before the type existed: {@code EsquivaEApararExcellency#FOCADO}'s "Defesas +1" is a
 * plain {@code SKILL_ROLL_BONUS}.
 *
 * <h2>Using it</h2>
 *
 * <pre>{@code
 * IncomingAttackResult attack = new AttackReceiver().resolve(IncomingAttack.builder()
 *         .defender(defenderSheet)
 *         .attacker(attackerSheet)                      // optional
 *         .difficultyLevel(DifficultyLevel.HARD)
 *         .defenseType(DefenseType.PHYSICAL)
 *         .attackBonus(2)
 *         .defenseRoll(new SkillRoll(List.of(4, 5, 3)))
 *         .criticalEffect(new Sangramento(CriticalResult.ACERTO_CRITICO_MAIOR))
 *         .effectChain(new Definhar())
 *         .build());
 *
 * // On a hit, the defence result carries a pre-wired chain: Damage -> Correntes -> Críticos.
 * Interaction<CharacterSheet> stage = attack.getDefenseResult().getNextInteraction();
 * if (stage instanceof DamageInteraction damage) {
 *     InteractionResult result = damage.applyTo(defenderSheet, rawDamage, false);
 *     while (result.getNextInteraction() != null) {
 *         result = defenderSheet.receiveInteraction(result.getNextInteraction());
 *     }
 * }
 * }</pre>
 *
 * <h2>Three independent outcomes</h2>
 *
 * {@code resolve} answers three separate questions, and they don't imply one another:
 * {@code getDefended()} (did it land), {@code getCriticalEffectTriggered()} (was the defence roll
 * a Falha Crítica — inverted, since it's the defender rolling), and {@code
 * getEffectChainTriggered()} (did the attack beat the defence by {@code
 * EffectChainService}'s required margin, 5 normally and 7 against {@code
 * AutocontroleAdvantage#RESOLUTO}). An attack can trigger a Corrente de Efeitos without
 * critting, and crit without triggering one.
 *
 * <h2>Damage is not computed here</h2>
 *
 * Deliberately: turning a roll into a raw damage figure needs a weapon/dano-roll concept this
 * core doesn't have — the still-manual "Skill -&gt; Damage handoff" {@code
 * org.aventyrs.core.effect}'s package-info names. What this package does instead is decide
 * <i>which stages fire</i> and assemble them into one chain, with a {@link
 * org.aventyrs.core.effect.DamageInteraction} at the head for the caller to feed its own damage
 * figure into.
 *
 * <h2>Report-only</h2>
 *
 * {@code resolve} assembles the chain but applies none of it, and touches no resource on the
 * defender — the same restraint {@code GritoDeGuerraVulcanoInteraction} applies to the Blessings
 * it reports. The one thing that does change is the defence roll itself happening ({@code
 * CharacterSheet#consumeFirstRollThisTurn}, and a temporary Ego point on a critical success),
 * which is why {@code resolve} rolls exactly once per attack.
 *
 * <h2>What this package deliberately doesn't do</h2>
 *
 * <ul>
 *   <li><b>It doesn't roll dice.</b> The defender's {@code SkillRoll} arrives already rolled, the
 *   same as everywhere else in this core. Omitting it is legal and yields a partial report —
 *   thresholds and bonuses, with every outcome field left {@code null} and no chain built.</li>
 *   <li><b>It doesn't choose which Efeitos an attack inflicts.</b> A caller supplies the
 *   concrete {@code CriticalEffect}s/{@code EffectChain}s on the {@code IncomingAttack}; this
 *   package decides only whether each group fires, and in what order they chain.</li>
 *   <li><b>It doesn't spend a Reação</b>, or check that the defender has one to spend. This core
 *   tracks counters, not when they may be spent.</li>
 *   <li><b>It doesn't know about Área de Efeito</b>, forced targeting/interception, or reactive
 *   damage — all still missing systems, cited by {@code EsquivaEApararCompetencyAbility#EVASAO}
 *   and {@code SantoAbility#GUARDA_VIDAS} among others.</li>
 * </ul>
 */
package org.aventyrs.core.combat;
