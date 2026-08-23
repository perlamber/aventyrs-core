/**
 * Resolving an attack against a character — the target side of a combat exchange.
 *
 * <h2>Two directions, one rule</h2>
 *
 * <b>This game's dice are always rolled by the player.</b> A foe never touches them, so it
 * contributes a fixed number in whichever direction an exchange runs — and that gives this
 * package two mirrored entry points, not one:
 *
 * <ul>
 *   <li>{@link org.aventyrs.core.combat.AttackReceiver} — <b>a foe attacks the player.</b> The
 *   foe presents a Grau de Dificuldade and a flat bonus; the player rolls Esquiva e Aparar
 *   against it. A <i>Falha</i> Crítica on that roll is the foe landing a critical hit.</li>
 *   <li>{@link org.aventyrs.core.combat.AttackDelivery} — <b>the player attacks a foe.</b> The
 *   foe presents a Defesa (its DF or DM); the player rolls an Ataque Perícia against it. An
 *   <i>Acerto</i> Crítico is the ordinary direction, and the one {@code
 *   CriticalEffect#validateCriticalHit} was written for.</li>
 * </ul>
 *
 * <p>They are mirrors, not halves: neither ever calls the other, and an exchange runs one of them
 * per attack. Both fill a hole {@code AtaqueADistanciaInteraction} and {@code
 * AtaqueCorpoACorpoInteraction} named in their own javadoc — <i>"the rules text compares this
 * roll against a target's DF or DM rather than a fixed GD, but that target-side lookup/conversion
 * is left to a layer above this core."</i>
 *
 * <h2>Where a foe's numbers come from</h2>
 *
 * A foe's Defesa and attack GD are authored on its stat block rather than derived from its
 * Perícias — see {@code org.aventyrs.core.monster.MonsterTemplate}. Both request types take them
 * already resolved rather than reaching into the other combatant, with {@code
 * DeliveredAttack#from(MonsterSheet, DefenseType)} filling them off a foe's sheet when there is
 * one. A Defesa is therefore a <i>target number</i> when a foe holds it, and a <i>pool of roll
 * bonuses</i> when a player does (see {@code org.aventyrs.core.character.DefenseType}) — the same
 * stat, on whichever side of the dice it lands.
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
 * Interaction<CombatantSheet> stage = attack.getDefenseResult().getNextInteraction();
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
 * CombatantSheet#consumeFirstRollThisTurn}, and a temporary Ego point on a critical success),
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
