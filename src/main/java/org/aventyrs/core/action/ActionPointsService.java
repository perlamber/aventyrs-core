package org.aventyrs.core.action;

import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillType;
import java.util.Set;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.atletismo.AtletismoExcellency;

public interface ActionPointsService {
    /** Every character starts with 3 Pontos de Ação (PA) before any bonus or profile applies. */
    int DEFAULT_ACTION_POINTS = 3;

    /** A Perícia roll costs 2PA before any bonus, malus or profile applies. */
    int DEFAULT_SKILL_ROLL_COST = 2;

    /**
     * Total PA available on the given Turn: the character's own fixed
     * {@link Character#getActionPoints()} counter, plus any
     * {@link org.aventyrs.core.modifier.ModifierType#ACTION_POINTS} bonus found on
     * attributeAbilities, skillCompetencyAbilities, or the unlocked
     * {@link org.aventyrs.core.skill.SkillExcellency} tiers of every trained Perícia (e.g.
     * {@link org.aventyrs.core.skill.atletismo.AtletismoExcellency#LENDA}), plus the character's
     * {@link Character#getTemporaryActionPointsBonus()}, plus every {@link
     * org.aventyrs.core.ability.AttributeAbility}'s Turn-conditioned {@link
     * org.aventyrs.core.ability.AttributeAbility#resolveActionPointsBonus(int)} (e.g. {@link
     * org.aventyrs.core.ability.DexterityAbility#APRESSADO}), adjusted by the character's
     * {@link ActionProfile} for that Turn. turnNumber is 0-based (0 is the character's first
     * Turn/Round), so an even Rodada is an odd turnNumber. Never negative.
     *
     * <p>This overload has no sheet, so it can't see a Round-scoped {@code TemporaryBonus} and
     * can't tell a Cena de Combate from any other — see
     * {@link #getMaxActionPoints(CombatantSheet, int, SceneContext)} for the combat-facing form.
     */
    int getMaxActionPoints(Character character, int turnNumber);

    /**
     * {@link #getMaxActionPoints(CombatantSheet, int, SceneContext)} with no Scene in hand —
     * delegates down with a {@code null} {@link SceneContext}.
     */
    int getMaxActionPoints(CombatantSheet sheet, int turnNumber);

    /**
     * Total PA this combatant has on the given Round. The sheet-taking form of
     * {@link #getMaxActionPoints(Character, int)}, and the one a Scene should use: it adds two
     * things the Character-only overload structurally cannot see.
     *
     * <ul>
     *   <li>The sheet's own {@link CombatantSheet#getTemporaryBonus(ModifierType)} for
     *       {@link ModifierType#ACTION_POINTS} — a Round-scoped {@code TemporaryBonus}, i.e.
     *       whatever a {@code Blessing} typed to that {@code ModifierType} granted. This is
     *       distinct from {@link Character#getTemporaryActionPointsBonus()}, a plain permanent
     *       int mutated directly, which both overloads read.</li>
     *   <li>Whether this is a Cena de Combate, which {@link ActionProfile#ESTRATEGISTA} needs.
     *       A {@code null} sceneContext reads as "not a Cena de Combate".</li>
     * </ul>
     *
     * <p>The {@link ActionProfile} adjustment is applied <em>last</em>, on top of both — so
     * {@link ActionProfile#CALCULISTA}'s first-Round 0 stays 0 however much was granted.
     * turnNumber is 0-based. Never negative.
     */
    int getMaxActionPoints(CombatantSheet sheet, int turnNumber, SceneContext sceneContext);

    /**
     * PA cost of a Perícia roll on the given Turn: {@value #DEFAULT_SKILL_ROLL_COST} plus
     * any {@link org.aventyrs.core.modifier.ModifierType#SKILL_ROLL_COST} adjustment —
     * abilities, feats or any other bonus-granting source — then the character's
     * {@link ActionProfile} for that Turn. turnNumber is 0-based. Never negative.
     *
     * <p>Takes a {@link Character} in both the sheet-less and sheet-having cases: no profile
     * varies a roll's cost by Scene, and no {@code ModifierType.SKILL_ROLL_COST}
     * {@code TemporaryBonus} has a granting path, so a sheet would add nothing here.
     */
    int getSkillRollCost(Character character, int turnNumber);

    /**
     * What one attack costs attacker on the given Turn, with activatedFeats spent on it — the Tempo
     * de Ação of an attack, which is a Perícia roll's ({@link #getSkillRollCost}) unless a Talento
     * says otherwise:
     *
     * <ol>
     *   <li>the lowest {@code Feat#resolveAttackActionPointOverride} among the held Talentos replaces
     *   the Perícia roll's price (Ataque Repentino's and Um-Dois' 1PA, Combater com 2 Armas' 3PA for
     *   the pair — 0 on its second attack);</li>
     *   <li>every {@code Feat#resolveAttackActionPointAdjustment} is added — Lutador Nato's +1PA —
     *   and every reduction taken off, never below 1PA (Dominar Armas' "(mínimo 1PA)").</li>
     * </ol>
     *
     * <p>Asked <b>before</b> the roll, since the price decides whether it can be made: activatedFeats
     * is what the attacker is about to name on the {@code SkillRoll}. A price of 0 from a Perícia
     * roll made free is an {@link ActionCost#FREE_ACTION}; one from an override (an attack already
     * paid for by its pair) is {@link ActionCost#NONE}. Reported, never deducted — this core keeps no
     * spent-PA ledger.
     */
    ActionCost getAttackCost(CombatantSheet attacker, SkillType attackSkill, AttackSource attackSource,
                             Set<Feat> activatedFeats, int turnNumber);

    /**
     * Whether the character has enough PA on the given Turn to pay that Turn's
     * {@link #getSkillRollCost(Character, int)}.
     */
    boolean canAffordSkillRoll(Character character, int turnNumber);

    /**
     * {@link #canAffordSkillRoll(CombatantSheet, int, SceneContext)} with no Scene in hand.
     */
    boolean canAffordSkillRoll(CombatantSheet sheet, int turnNumber);

    /**
     * Whether this combatant has enough PA on the given Round to pay that Round's
     * {@link #getSkillRollCost(Character, int)}, measured against the sheet-aware
     * {@link #getMaxActionPoints(CombatantSheet, int, SceneContext)} rather than the
     * Character-only total — so a granted PA {@code TemporaryBonus} (or Estrategista's combat
     * malus) is reflected in the answer.
     */
    boolean canAffordSkillRoll(CombatantSheet sheet, int turnNumber, SceneContext sceneContext);
}
