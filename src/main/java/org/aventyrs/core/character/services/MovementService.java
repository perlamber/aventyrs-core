package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.SizeCategory;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillExcellency;

/**
 * Movimento Base — how far a combatant travels per Ponto de Ação spent moving.
 *
 * <p><b>Every movement figure in this ruleset is per Ponto de Ação.</b> That is the single rule
 * to carry into any new movement clause: a UD amount named anywhere — a permanent {@code
 * ModifierType#MOVEMENT} bonus, a Round-scoped {@code TemporaryBonus}, or a bonus scoped to one
 * particular movement of the Rodada ({@code resolveRoundMovementIncrease}) — always widens what
 * <em>one</em> Ponto de Ação buys. None of them is ever a one-off distance added once to a
 * movement's total. So a character with Movimento Base 4 who gains +2UD and spends 3 Pontos de
 * Ação moving covers (4+2)×3 = 18UD, never 4×3+2 = 14UD.
 *
 * <p>This is why every method here returns the per-Ponto-de-Ação figure and none of them
 * multiplies: how many Pontos de Ação go to moving rather than to everything else is the
 * player's choice at the table, and a caller that wants a distance multiplies by the number it
 * chose. See {@link #getMovementBase(Character)} for the rest of that reasoning.
 */
public interface MovementService {

    /**
     * Movimento Base in UD (Unidades de Distância) — how far this character travels for
     * <b>each</b> Ponto de Ação spent moving, <b>not</b> a whole-Turn allowance. It is {@link
     * SizeCategory#getMovementPerActionPoint()} (via {@link CharacterSizeService
     * #getEffectiveSizeCategory}, so a size-shifting ability like Sangue de Gigante is already
     * reflected) plus any {@link org.aventyrs.core.modifier.ModifierType#MOVEMENT} bonus found
     * on attributeAbilities, skillCompetencyAbilities (acquired <b>and</b> racial — see {@link
     * org.aventyrs.core.skill.SkillCompetencyAbility#allFor}; unlike {@link ReactionsService}/
     * {@link InitiativeService}, which predate that fix and still only scan the acquired list,
     * this newer service starts from the corrected combined one), or the unlocked {@link
     * SkillExcellency} tiers of every trained Perícia. Never negative — like Reações/Ações
     * Livres/RD/RA, this is a spendable-resource-like budget, not a signed comparative value
     * like Iniciativa.
     *
     * <p><b>This service deliberately does not multiply by {@link ActionPointsService
     * #getMaxActionPoints}.</b> A character's Pontos de Ação are spent across moving
     * <em>and</em> everything else they do that Turn, and how many go to each is the player's
     * choice at the table — a decision this core has no way to make and no reason to
     * pre-empt. A caller that wants the distance covered by a specific number of Pontos de
     * Ação multiplies this figure by that number itself; one that wants the theoretical
     * maximum multiplies by {@code getMaxActionPoints} for the Turn in question. Neither
     * belongs here, which is also why this takes no {@code turnNumber}: nothing about
     * Movimento Base varies by Turn.
     *
     * <p>This is the <em>permanent</em> total only — the {@link CombatantSheet} overloads below
     * are what add everything scoped to the here and now.
     */
    int getMovementBase(Character character);

    /**
     * Movimento Base for whichever movement this combatant makes <b>next</b> this Rodada — the
     * overload almost every in-Scene caller wants. Resolves {@link
     * #getMovementBase(CombatantSheet, int)} against {@link
     * CombatantSheet#getMovementsTakenThisRound()}, so it reads the position without claiming
     * it; a caller actually moving calls {@link CombatantSheet#consumeMovementThisRound()} to
     * take that position.
     *
     * <p>Deliberately <b>not</b> a cascade from {@link #getMovementBase(Character)}: that one
     * answers a different question (what this Character permanently has, with no Scene in
     * sight), the same non-cascading {@code Character}/{@code CombatantSheet} pair {@code
     * ActionPointsService} already draws.
     */
    int getMovementBase(CombatantSheet sheet);

    /**
     * Movimento Base for the movementIndex-th movement of this Rodada, 0-based — the longest
     * form, holding all the logic. On top of {@link #getMovementBase(Character)}'s permanent
     * total it adds two things scoped to now:
     *
     * <ul>
     *   <li>{@link CombatantSheet#getTemporaryBonus}({@code ModifierType.MOVEMENT}) — a
     *   Round-scoped grant from someone else's action (e.g. {@code
     *   InitiativeAdvantage#POSICIONAMENTO_ESTRATEGICO}'s +2UD while its holder's group holds
     *   initiative), which lives on the sheet rather than the {@code Character}, the same
     *   "permanent ability vs. granted-by-someone-else's-action" split every other stat here
     *   draws.</li>
     *   <li>{@code resolveRoundMovementIncrease(movementIndex, …)} across the same four sources
     *   the permanent {@code MOVEMENT} scan already covers — {@code AttributeAbility}, {@code
     *   SkillCompetencyAbility}, {@code Feat} and equipped {@code Item}s — for a clause scoped
     *   to <i>which</i> movement of the Rodada this is, e.g. {@code
     *   DexterityAbility#PASSOS_LONGOS}'s "seu primeiro movimento em cada Rodada" or {@code
     *   MobilidadeFeat#VELOCISTA}'s "+1UD para cada outro movimento feito no mesmo Turno".</li>
     * </ul>
     *
     * <p>Both are per Ponto de Ação, like everything else this service returns — see the class
     * javadoc's rule. Never negative, for the same reason the permanent total isn't.
     */
    int getMovementBase(CombatantSheet sheet, int movementIndex);
}
