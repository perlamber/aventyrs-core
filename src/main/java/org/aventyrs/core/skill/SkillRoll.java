package org.aventyrs.core.skill;

import org.aventyrs.core.action.Manoeuvre;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.List;
import java.util.Set;

import static org.aventyrs.core.util.TranslatableMessages.INVALID_SKILL_ROLL;

/**
 * The already-rolled dice behind a Perícia test — always exactly 3 six-sided dice in this
 * ruleset. This core deliberately never rolls dice itself (see the {@code skill}
 * package-info's "What this library computes" section), so the caller (an API layer) rolls
 * physically or via an RNG and hands the individual face values in here — not just their sum,
 * since {@link #getCriticalResult(int, int)} reads the faces themselves for an Acerto Crítico
 * Maior (a literal three 6s), not only their total.
 *
 * <p>{@code dice} is validated at construction — exactly 3 values, each 1-6 — since this is a
 * true system boundary (input from outside this core), unlike internal invariants this
 * codebase otherwise trusts without checking.
 *
 * <p>{@code requestedAbility} is optional: a caller performing this roll specifically *as* one
 * of the character's held {@link SkillTrait}s — either a {@link SkillCompetencyAbility}
 * maneuver or a {@link SkillSpecialization} — (as opposed to a plain Perícia test) names which
 * one here. {@link AbstractSkillInteraction} then validates the character actually holds it
 * before proceeding — see its own javadoc; when it's a held {@link SkillSpecialization}, the
 * roll's reached {@link DifficultyLevel} is resolved via {@link
 * DifficultyLevel#reachedByAsExpert} instead of {@link DifficultyLevel#reachedBy}. {@code null}
 * means "just a plain roll, no specific trait being invoked," and skips that check entirely.
 *
 * <p>{@code targetValue} is the number this roll has to <b>beat</b> — the Grau de Dificuldade it
 * was made against, already reduced to a plain int. Optional, and {@code null} for a roll made
 * against nothing in particular, in which case {@code InteractionResult#getSucceeded()} stays
 * {@code null} too: "nobody said what this was against" is a different answer from "failed".
 *
 * <p><b>A number, not a {@link DifficultyLevel}.</b> The usual source is {@code
 * DifficultyLevel#getBaseValue()}, but not every GD in the rules is a tier — {@code
 * ConditionType#DEVORADO}'s "GD 10+Vigor" is computed from a creature and lands between them —
 * so the comparison is arithmetic, exactly as {@code
 * org.aventyrs.core.combat.AttackReceiver#resolve} already does for the combat side. Use {@link
 * #against(List, DifficultyLevel)} when the target genuinely is a tier.
 *
 * <p>{@code actionCost} is optional roll <b>metadata</b> — the Pontos de Ação / Ação Livre /
 * Reação this roll's action cost (see {@link org.aventyrs.core.sheet.ActionCost}). Deliberately
 * <em>not</em> a domain-resolution input the way {@code attackSource} is: nothing in {@link
 * AbstractSkillInteraction}'s bonus or GD math reads it except the one cost-conditioned {@code
 * Feat} hook ({@code Feat#resolveAttackCostDifficultyReduction}, for {@code
 * AssassinoFeat#SAQUE_RELAMPAGO}). It is carried so the caller can build a {@code
 * org.aventyrs.core.sheet.CombatantAction} for the per-Rodada log and so a cost-gated Talento
 * can see it. {@code null} means "caller didn't say".
 *
 * <p>{@code manoeuvre} is optional roll-metadata on the same terms — <b>which named manoeuvre
 * this roll is the attack half of</b> (see {@link Manoeuvre}), {@code null} for an ordinary
 * attack. It rides here rather than on {@code org.aventyrs.core.combat.DeliveredAttack} for one
 * concrete reason: {@code AttackDelivery} hands this object to the longest {@code applyTo}, so a
 * marker here reaches the bonus maths on <em>both</em> the delivery path and the direct
 * skill-roll path, where a field on the request would reach only the first. Unlike {@code
 * actionCost} it <em>is</em> read by that maths — {@code AbstractSkillInteraction} adds an
 * Investida's flat dano bonus and scans {@code AttributeAbility#resolveManoeuvreRollBonus} off it
 * — because a manoeuvre changes what the action <em>is</em>, not merely what it cost.
 *
 * <p>{@code activatedFeats} is roll-metadata on those same terms — <b>which Talentos the roller
 * spent to make this roll</b>, rather than merely held while making it. Empty for a roll nobody
 * activated anything for, which is most of them. This is the distinction a per-attack clause
 * scoped to another Talento's use needs and could not previously express: {@code
 * ArtilhariaFeat#MIRA_MORTAL}'s "sempre que tiver um Acerto Crítico usando o talento ‘Mira
 * Impecável’" is granted off {@code SkillRoll#activated(Feat)}, since holding Mira Impecável and
 * paying its +1PA to reroll a die are different facts and only the second one earns the die.
 * Which Talentos those are is the caller's statement, the same way the dice themselves are: this
 * core neither rolls nor spends anything.
 */
public class SkillRoll {
    private static final int EXPECTED_DICE_COUNT = 3;
    private static final int MIN_FACE_VALUE = 1;
    private static final int MAX_FACE_VALUE = 6;

    /** 1+1+1 — the only combination of 3 dice (each 1-6) that sums to 3. */
    private static final int MAJOR_CRITICAL_FAILURE_TOTAL = 3;

    /** 1+1+2 — the only combination of 3 dice (each 1-6) that sums to 4. */
    private static final int MINOR_CRITICAL_FAILURE_TOTAL = 4;

    /**
     * The floor {@link #getCriticalResult(int, int)} clamps a widened Margem Crítica Menor to — one
     * above {@link #MINOR_CRITICAL_FAILURE_TOTAL}, the lowest total this ruleset leaves free for a
     * critical <em>success</em>. Absurd widening therefore makes every roll that isn't a Falha
     * Crítica a critical, and never overturns the two Falhas themselves.
     */
    private static final int MIN_TOTAL = MINOR_CRITICAL_FAILURE_TOTAL + 1;

    private final List<Integer> dice;
    private final SkillTrait requestedAbility;
    private final Integer targetValue;
    private final ActionCost actionCost;
    private final Manoeuvre manoeuvre;
    private final Set<Feat> activatedFeats;

    public SkillRoll(final List<Integer> dice) {
        this(dice, null, null, null);
    }

    public SkillRoll(final List<Integer> dice, final SkillTrait requestedAbility) {
        this(dice, requestedAbility, null, null);
    }

    /**
     * A roll made against a {@link DifficultyLevel} tier — the common case, resolving the tier to
     * its {@link DifficultyLevel#getBaseValue()} so the comparison stays arithmetic.
     */
    public static SkillRoll against(final List<Integer> dice, final DifficultyLevel target) {
        return new SkillRoll(dice, null, target == null ? null : target.getBaseValue());
    }

    public SkillRoll(final List<Integer> dice, final SkillTrait requestedAbility, final Integer targetValue) {
        this(dice, requestedAbility, targetValue, null);
    }

    public SkillRoll(final List<Integer> dice, final SkillTrait requestedAbility, final Integer targetValue,
                     final ActionCost actionCost) {
        this(dice, requestedAbility, targetValue, actionCost, null);
    }

    /**
     * Names the {@link Manoeuvre} this roll is the attack half of. Every shorter constructor
     * delegates down with a {@code null} manoeuvre — an ordinary attack.
     */
    public SkillRoll(final List<Integer> dice, final SkillTrait requestedAbility, final Integer targetValue,
                     final ActionCost actionCost, final Manoeuvre manoeuvre) {
        this(dice, requestedAbility, targetValue, actionCost, manoeuvre, null);
    }

    /**
     * The canonical form, adding the {@link Feat}s the roller <b>activated</b> to make this roll.
     * Every shorter constructor delegates down to it with none.
     */
    public SkillRoll(final List<Integer> dice, final SkillTrait requestedAbility, final Integer targetValue,
                     final ActionCost actionCost, final Manoeuvre manoeuvre, final Set<Feat> activatedFeats) {
        if (dice.size() != EXPECTED_DICE_COUNT) {
            throw new IllegalOperationException(INVALID_SKILL_ROLL);
        }
        for (int face : dice) {
            if (face < MIN_FACE_VALUE || face > MAX_FACE_VALUE) {
                throw new IllegalOperationException(INVALID_SKILL_ROLL);
            }
        }
        this.dice = dice;
        this.requestedAbility = requestedAbility;
        this.targetValue = targetValue;
        this.actionCost = actionCost;
        this.manoeuvre = manoeuvre;
        this.activatedFeats = activatedFeats == null ? Set.of() : Set.copyOf(activatedFeats);
    }

    /**
     * The number this roll has to beat, or {@code null} when it was made against nothing stated.
     * See this class's own javadoc for why it is an int rather than a {@link DifficultyLevel}.
     */
    public Integer getTargetValue() {
        return targetValue;
    }

    /**
     * The {@link SkillTrait} (a {@link SkillCompetencyAbility} or a {@link SkillSpecialization})
     * this roll is being made to invoke, or {@code null} for a plain roll.
     */
    public SkillTrait getRequestedAbility() {
        return requestedAbility;
    }

    /**
     * What this roll's action cost — a Pontos de Ação amount, an Ação Livre, or a Reação — or
     * {@code null} when the caller didn't say. See this class's own javadoc for why it is
     * metadata rather than a resolution input.
     */
    public ActionCost getActionCost() {
        return actionCost;
    }

    /**
     * The named manoeuvre this roll is the attack half of, or {@code null} for an ordinary
     * attack — never "not an Investida". See this class's own javadoc for why it rides the roll.
     */
    public Manoeuvre getManoeuvre() {
        return manoeuvre;
    }

    /**
     * The Talentos the roller activated to make this roll — never {@code null}, empty for a roll
     * nobody spent a Talento on. See this class's own javadoc for why it is roll metadata.
     */
    public Set<Feat> getActivatedFeats() {
        return activatedFeats;
    }

    /** Whether feat is one of the {@link #getActivatedFeats()} — the read an override wants. */
    public boolean activated(final Feat feat) {
        return activatedFeats.contains(feat);
    }

    /** The sum of all 3 dice — what gets added to the Perícia's own bonus and compared against a GD. */
    public int getTotal() {
        return dice.stream().mapToInt(Integer::intValue).sum();
    }

    /** Same as {@link #getCriticalResult(int)} with no Margem Crítica widening applied. */
    public CriticalResult getCriticalResult() {
        return getCriticalResult(0);
    }

    /**
     * Same as {@link #getCriticalResult(int, int)} against the {@link
     * Weapon#DEFAULT_LESSER_CRITICAL_MARGIN} — the margin every Arma whose "Efeito Crítico" column
     * prints no number after it is on, and the only honest answer for a roll made with no weapon in
     * hand at all (a Perícia test, a Magia).
     */
    public CriticalResult getCriticalResult(final int criticalMarginIncrease) {
        return getCriticalResult(criticalMarginIncrease, Weapon.DEFAULT_LESSER_CRITICAL_MARGIN);
    }

    /**
     * See {@link CriticalResult} for what each outcome means.
     *
     * <p>lesserCriticalMargin is the <b>3d6 total an Acerto Crítico Menor has to reach</b> — the
     * number the Armas table prints in parentheses beside a weapon's Efeito Crítico ({@code
     * Sangramento (17)}, a Florete's {@code (16)}), read off {@link
     * Weapon#getLesserCriticalMargin()} by {@link AbstractSkillInteraction}. <b>Lower is wider.</b>
     * This is the authored representation of a Margem Crítica in this ruleset, and the one thing
     * that decides a Menor critical; the "two dice showing 6" rule this method applied before
     * 0.0.41 was an approximation of it invented here, and a weapon's own column outvotes it.
     *
     * <p>criticalMarginIncrease is the combined "número" widening from every source that grants one
     * right now — see {@link org.aventyrs.core.ability.AttributeAbility#resolveCriticalMarginIncrease}/{@link
     * org.aventyrs.core.ego.EgoAdvantage#resolveCriticalMarginIncrease}/{@link
     * SkillCompetencyAbility#resolveCriticalMarginIncrease} (e.g. {@link
     * org.aventyrs.core.ability.DexterityAbility#LETALIDADE_PROGRESSIVA}), plus {@code
     * Item#resolveEnhancementCriticalMarginIncrease}, summed by {@link AbstractSkillInteraction}
     * across all of them before calling this — a caller with no such source in hand can pass 0.
     * Each {@code +1} número lowers the total to reach by one (17 → 16, exactly as {@code
     * AssassinoFeat#ACERTO_CRITICO_APRIMORADO} reads), negative increases are treated as 0, and
     * the threshold is floored at {@code MIN_TOTAL} so no amount of widening can make every roll
     * a critical.
     *
     * <p>Only widens Acerto Crítico Menor: unlike it, Falha Crítica Menor/Maior and Acerto Crítico
     * Maior are each fixed at one exact dice combination in this ruleset's own text, and all three
     * are checked first — so three 6s stays Acerto Crítico <b>Maior</b> however wide the Menor
     * margin is, and a Falha Crítica is never overturned by one. A clause that widens Acerto
     * Crítico <em>Maior</em> is a separate, explicit mechanism — it does not ride this margin, and
     * none is authored yet.
     */
    public CriticalResult getCriticalResult(final int criticalMarginIncrease, final int lesserCriticalMargin) {
        int total = getTotal();
        if (total == MAJOR_CRITICAL_FAILURE_TOTAL) {
            return CriticalResult.FALHA_CRITICA_MAIOR;
        }
        if (countFace(MAX_FACE_VALUE) == EXPECTED_DICE_COUNT) {
            return CriticalResult.ACERTO_CRITICO_MAIOR;
        }
        if (total == MINOR_CRITICAL_FAILURE_TOTAL) {
            return CriticalResult.FALHA_CRITICA_MENOR;
        }
        int widenedThreshold = Math.max(MIN_TOTAL, lesserCriticalMargin - Math.max(0, criticalMarginIncrease));
        if (total >= widenedThreshold) {
            return CriticalResult.ACERTO_CRITICO_MENOR;
        }
        return CriticalResult.NONE;
    }

    private int countFace(final int face) {
        return (int) dice.stream().filter(rolled -> rolled == face).count();
    }
}
