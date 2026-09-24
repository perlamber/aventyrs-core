package org.aventyrs.core.sheet;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.skill.DifficultyLevel;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Being Escondido — {@link ConditionType#ESCONDIDO} held, <b>plus the Grau de Dificuldade it is
 * worth</b>.
 *
 * <p>A plain {@link Condition} says only <i>that</i> a combatant is hidden. Hiding needs a GD too:
 * the Furtividade roll that put them there is what every would-be observer's Atenção roll has to
 * reach, and it has to outlive the roll that produced it. That is what this subclass carries, the
 * same catalogue-entry-plus-held-instance split {@link Condition} already draws against {@link
 * ConditionType} — {@code ESCONDIDO} describes what being hidden <i>is</i>, an instance of this is
 * one character hidden at Difícil +1.
 *
 * <h2>A tier and a flat bonus, not a bare number</h2>
 *
 * <p>A GD in this game is a {@link DifficultyLevel} with a modifier on it, and that is how one is
 * held here: {@link #getDifficultyLevel()} plus {@link #getBonus()}, exactly the pair a {@code
 * org.aventyrs.core.monster.SkillDifficulty} holds for each Perícia a foe presents a GD on.
 * {@link #getConcealmentValue()} flattens it the way this core flattens every other GD — {@link
 * DifficultyLevel#getBaseValue()} plus the bonus, the same arithmetic {@code
 * AttackReceiver#resolve} and {@code SkillRoll#against} do — and {@link
 * #getConcealmentValue(boolean)} is the easier threshold an observer rolling with a matching
 * Especialização gets instead.
 *
 * <p>{@link #fromRoll} is what decomposes a Furtividade total into the pair, thresholding against
 * {@link DifficultyLevel#reachedByAsExpert} when the roll named a held {@code
 * FurtividadeSpecialization#MAESTRIA_DA_OCULTACAO} and {@link DifficultyLevel#reachedBy} when it
 * did not — the same choice {@code AbstractSkillInteraction} already makes for {@code
 * InteractionResult#getReachedDifficultyLevel()}, so the tier here is the tier that roll reported.
 *
 * <h2>Why a tier and not just the total</h2>
 *
 * <p>Because it makes the Especialização worth the same thing on both sides of the contest, which
 * a bare number cannot. <b>Maestria da Ocultação is opposed by Sentidos Apurados</b> ({@code
 * FurtividadeSpecialization#MAESTRIA_DA_OCULTACAO} against {@code
 * AttentionSpecialization#SENTIDOS_APURADOS}), and each buys the same thing it buys everywhere
 * else in this ruleset: the easier threshold of whatever tier is in play.
 *
 * <p>Worked through, a Furtividade total of 21:
 *
 * <ul>
 *   <li>rolled <b>with</b> Maestria da Ocultação it is Difícil +1 (Difícil's expert threshold is
 *   20). An ordinary watcher has to reach Difícil's base 23, so <b>24</b>; one with Sentidos
 *   Apurados reaches its expert 20, so <b>21</b> — the raw total, as if neither had specialised.</li>
 *   <li>rolled <b>raw</b> it is Médio +3. An ordinary watcher needs Médio's base 18, so
 *   <b>21</b>; one with Sentidos Apurados needs its expert 16, so <b>19</b>.</li>
 * </ul>
 *
 * <p>So the specialised hider is harder to find than the dice alone say, and the specialised
 * watcher takes exactly that advantage back. Flattening to a number at the moment of hiding would
 * have thrown away the tier the second half of the contest needs.
 *
 * <p><b>A subclass rather than a value field on {@link Condition}</b>, because exactly one
 * condition in the catalogue has a magnitude and widening the shared shape for it would hand a
 * meaningless GD to the other sixteen. It is the shape {@link Regeneration} already takes beside
 * {@link TemporaryBonus}.
 *
 * <p><b>Open-ended.</b> {@code remainingRounds} is {@code null}: hiding does not lapse on a
 * Rodada count, it ends when something reveals it — the same reasoning that makes {@link
 * ConditionType#DESARMADO} open-ended, since {@code CombatantSheet#rearm} is what lifts that one.
 * {@code HidingService#reveal} is what lifts this one.
 *
 * <p><b>Who has already spotted them lives here too.</b> Detection is per observer — a Goblin
 * failing its Atenção does not stop the Elfo beside it succeeding — so the sheet cannot hold a
 * single "visible" flag. {@link #getDetectedBy()} accumulates whoever has resolved a successful
 * look, and dies with the condition: revealing and re-hiding builds a fresh instance with an
 * empty set, which is exactly right, since a new Furtividade roll is a new place to hide.
 * Observers are held as {@link CombatantSheet#getId()} rather than as sheet references, matching
 * how {@code org.aventyrs.core.scene.Scene} identifies its own participants.
 */
@Getter
public class Hidden extends Condition {

    /** The tier this concealment presents. Fixed at the moment of hiding; nothing degrades it. */
    @NonNull
    private final DifficultyLevel difficultyLevel;

    /**
     * The flat modifier on top of {@link #getDifficultyLevel()}'s threshold — by how much the
     * Furtividade roll exceeded the tier it reached. Negative only in the one case {@link
     * #fromRoll} documents: a total too low to reach even {@link DifficultyLevel#VERY_EASY}.
     */
    private final int bonus;

    private final Set<UUID> detectedBy = new LinkedHashSet<>();

    public Hidden(@NonNull final DifficultyLevel difficultyLevel, final int bonus) {
        super(ConditionType.ESCONDIDO, null);
        this.difficultyLevel = difficultyLevel;
        this.bonus = bonus;
    }

    /**
     * Decomposes an already-resolved Furtividade total into the tier it reached and the excess
     * over that tier's threshold — a total of 21 rolled as an expert is {@link
     * DifficultyLevel#HARD} +1, the same 21 rolled plainly is {@link DifficultyLevel#MEDIUM} +3.
     *
     * <p>rolledAsExpert is {@code true} when the roll named a held {@code
     * FurtividadeSpecialization#MAESTRIA_DA_OCULTACAO} (or any other matching Especialização) —
     * the same condition under which {@code AbstractSkillInteraction} thresholds against {@link
     * DifficultyLevel#getExpertValue()}. See "Why a tier and not just the total" above for what
     * that is worth on each side of the contest.
     *
     * <p>A total below even {@link DifficultyLevel#VERY_EASY}'s threshold clamps to that tier and
     * carries a <b>negative</b> bonus, so {@link #getConcealmentValue()} still reproduces the
     * total. Hiding badly is a bad hiding place, not a failure to hide: nothing in the rules text
     * makes a low roll refuse to conceal, and inventing a floor would be inventing a rule.
     */
    public static Hidden fromRoll(final int furtividadeTotal, final boolean rolledAsExpert) {
        DifficultyLevel reached = (rolledAsExpert
                ? DifficultyLevel.reachedByAsExpert(furtividadeTotal)
                : DifficultyLevel.reachedBy(furtividadeTotal))
                .orElse(DifficultyLevel.VERY_EASY);
        int threshold = rolledAsExpert ? reached.getExpertValue() : reached.getBaseValue();
        return new Hidden(reached, furtividadeTotal - threshold);
    }

    /**
     * The number an ordinary observer's Atenção total must reach — {@link
     * DifficultyLevel#getBaseValue()} plus {@link #getBonus()}, the way this core flattens every
     * other GD ({@code AttackReceiver#resolve}, {@code SkillRoll#against}). Hand it to {@code
     * SkillRoll}'s {@code targetValue} so the Atenção roll resolves through the ordinary machinery.
     */
    public int getConcealmentValue() {
        return getConcealmentValue(false);
    }

    /**
     * The number this observer has to reach, given whether their Atenção roll named a held {@code
     * AttentionSpecialization#SENTIDOS_APURADOS} — the Especialização that opposes Maestria da
     * Ocultação. An expert watcher thresholds the tier at {@link DifficultyLevel#getExpertValue()},
     * exactly as {@code AbstractSkillInteraction} thresholds every other expert roll; see the
     * worked example in this class's javadoc for what the pair is worth on each side.
     */
    public int getConcealmentValue(final boolean observerIsExpert) {
        return (observerIsExpert ? difficultyLevel.getExpertValue() : difficultyLevel.getBaseValue()) + bonus;
    }

    /** Whether observer has already seen through this concealment. */
    public boolean isDetectedBy(final CombatantSheet observer) {
        return observer != null && detectedBy.contains(observer.getId());
    }

    /**
     * Records that observer sees through this concealment from now on. The unvalidating mutator
     * beneath {@code HidingService#resolveDetection} — it compares nothing and asks nothing about
     * groups, the same split {@code Character#drawWeapon} keeps beneath {@code WeaponDrawService}.
     */
    public void markDetectedBy(final CombatantSheet observer) {
        if (observer != null) {
            detectedBy.add(observer.getId());
        }
    }

    /** The ids of every observer that has seen through this concealment, unmodifiable. */
    public Set<UUID> getDetectedBy() {
        return Collections.unmodifiableSet(detectedBy);
    }
}
