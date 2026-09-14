package org.aventyrs.core.character.services;

import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.Hidden;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

/**
 * Esconder-se — going hidden on a Furtividade roll, being looked for, and being given away.
 *
 * <p>The rule: a character rolls Furtividade and the total <b>becomes a Grau de Dificuldade</b>.
 * Anyone outside their group who wants to see them makes an Atenção roll against it; whoever falls
 * short simply does not see them. The hiding ends the moment its holder attacks, makes a Perícia
 * roll against somebody else, or moves — that last one unless {@code
 * MobilidadeFeat#MOVIMENTO_FURTIVO} says otherwise.
 *
 * <p><b>The GD is a tier plus a flat bonus</b>, not a bare number — {@link Hidden} holds the
 * {@link DifficultyLevel} the Furtividade roll reached and the excess over it, the same pair a foe
 * already presents with {@code MonsterTemplate#getAttackDifficulty()}/{@code getAttackBonus()}.
 * That is what lets the two Especializações of this contest — {@code
 * FurtividadeSpecialization#MAESTRIA_DA_OCULTACAO} hiding, {@code
 * AttentionSpecialization#SENTIDOS_APURADOS} watching — each be worth what an Especialização is
 * worth everywhere else: the easier threshold of the tier in play. See {@link Hidden} for the
 * worked example.
 *
 * <p><b>This core never rolls.</b> Both rolls arrive already totalled: the Furtividade one as
 * {@link #hide}'s argument, an observer's Atenção one as {@link #resolveDetection}'s. A {@code
 * MonsterSheet} is the exception that proves it — a foe never rolls at all, so its own Atenção is
 * an authored flat number on the stat block ({@code MonsterTemplate#getPerception()}), read here
 * instead of a roll, exactly like the Defesas and the GD de ataque it already presents.
 *
 * <h2>Where the state lives</h2>
 *
 * <p>On the hider's own sheet, as a {@link Hidden} — the {@link ConditionType#ESCONDIDO} Condição
 * plus the number it is worth plus whoever has already seen through it. Nothing is stored on the
 * observers: being spotted is a fact about one concealment, and it dies when that concealment
 * does. See {@link Hidden} for why the value needed a Condição subclass.
 *
 * <h2>What it deliberately does not do</h2>
 *
 * <ul>
 *   <li><b>Nothing hides, looks or reveals on its own.</b> Every method here is called by whoever
 *   is driving the action — the same caller-driven discipline {@link DefeatBlessingService} and
 *   {@code ChargeService#applyOutcome} keep. In particular {@code
 *   org.aventyrs.core.combat.AttackDelivery} does not call {@link #reveal}: it stays report-only,
 *   so the caller that resolved the attack reports the give-away.</li>
 *   <li><b>It does not gate hiding.</b> Furtividade's own rules text forbids hiding while
 *   observed ({@code FurtividadeCompetencyAbility#AGORA_ESTOU_AGORA_NAO_ESTOU} lifts the
 *   restriction at +1 nível de GD), but "being observed" is not a state this core holds — {@link
 *   #isHiddenFrom} answers the narrower question of who is fooled by an <i>existing</i>
 *   concealment, which is not the same as knowing whether anyone currently has eyes on you.
 *   {@link #hide} therefore accepts any roll, and the Narrador adjudicates.</li>
 *   <li><b>It measures nothing.</b> No distance, cover or lighting term reaches the GD — this
 *   core does no geometry, and the Furtividade total is taken as the whole of the concealment.</li>
 *   <li><b>It grants no Vantagem and no bonus.</b> What being Escondido is <i>worth</i> belongs to
 *   whichever trait says so ({@code AssassinoFeat#ESCUDO_DE_SOMBRAS}'s +3, which reads the
 *   Condição off the sheet for itself), not to this service.</li>
 * </ul>
 */
public interface HidingService {

    /**
     * What a foe with nothing authored spots at — {@link DifficultyLevel#EASY}'s threshold.
     *
     * <p><b>Inferred, not authored.</b> No stat block in {@code docs/rules/} carries an Atenção
     * column. 14 is where an unremarkable creature's own Atenção roll would land on average (3d6
     * averages 10.5, plus a modest Atributo and Graduação), so a foe nobody thought about is
     * neither blind nor uncannily sharp. A creature meant to be either authors its own figure.
     */
    int DEFAULT_MONSTER_PERCEPTION = 14;

    /**
     * Hides hider on a Furtividade roll made <b>without</b> a matching Especialização — {@link
     * #hide(CombatantSheet, int, boolean)} with {@code false}.
     */
    Hidden hide(CombatantSheet hider, int furtividadeTotal);

    /**
     * Hides hider at furtividadeTotal — the total of an already-resolved {@link
     * SkillType#FURTIVIDADE} roll (its {@code InteractionResult#getSkillRollBonus()} plus the
     * dice), decomposed into a tier and a bonus by {@link Hidden#fromRoll}.
     *
     * <p>rolledWithSpecialization is {@code true} when the roll named a held {@code
     * FurtividadeSpecialization#MAESTRIA_DA_OCULTACAO} — the same condition under which {@code
     * AbstractSkillInteraction} thresholded it as an expert roll, so the tier this stores is the
     * one {@code InteractionResult#getReachedDifficultyLevel()} already reported for it.
     *
     * <p>Applies {@link ConditionType#ESCONDIDO} through {@code CombatantSheet#applyCondition},
     * which replaces any concealment already held: hiding again is a new place to hide, so
     * whoever had already spotted the old one starts over.
     *
     * @return the {@link Hidden} now held, for a caller that wants to read the GD back
     */
    Hidden hide(CombatantSheet hider, int furtividadeTotal, boolean rolledWithSpecialization);

    /**
     * Hides hider at a GD stated outright, for a caller that already has the tier and modifier
     * rather than a roll to decompose — a Narrador setting a hiding place by fiat, or a
     * concealment restored from persisted state.
     *
     * <p>Not part of the cascade the other two form: "here is the GD" and "here is a roll" are
     * different questions, the same reason {@code ActionPointsService}'s {@code Character}/{@code
     * CombatantSheet} pair does not delegate either way.
     */
    Hidden hide(CombatantSheet hider, DifficultyLevel difficultyLevel, int bonus);

    /**
     * The GD an ordinary observer's Atenção roll must reach to see hider — {@link
     * #getConcealmentValue(CombatantSheet, boolean)} with {@code false}.
     */
    Integer getConcealmentValue(CombatantSheet hider);

    /**
     * The GD this observer's Atenção roll must reach, given whether it names a held {@code
     * AttentionSpecialization#SENTIDOS_APURADOS}; {@code null} when hider is not hidden at all.
     * Handed to {@code SkillRoll#getTargetValue()} so the Atenção roll resolves through the
     * ordinary roll machinery, which is why it is flattened to a number here rather than handed
     * back as the tier — {@link CombatantSheet#getHidden()} is where a caller reads the pair.
     */
    Integer getConcealmentValue(CombatantSheet hider, boolean observerIsExpert);

    /**
     * Whether hider is currently unseen by observer: hidden, observer outside their group, and
     * observer not already among those who have seen through it.
     *
     * <p>hiderContext is the <b>hider's own</b> snapshot — the group test reads {@code
     * SceneContext#getAllies()}, which is exactly "everyone in my sub-group but me". A {@code
     * null} context means group membership cannot be established, and is read as observer being
     * outside the group: unlike every proximity-gated clause in this core, defaulting the other
     * way would let a caller with no Scene lose a concealment it had just been granted, and being
     * hidden from an ally costs that ally nothing this core models.
     */
    boolean isHiddenFrom(CombatantSheet hider, CombatantSheet observer, SceneContext hiderContext);

    /**
     * Resolves one observer's attempt to see hider, and remembers the answer.
     *
     * <p>The opposing value is attentionTotal — an already-resolved {@link SkillType#ATTENTION}
     * roll — <b>except for a {@code MonsterSheet}</b>, which contributes its authored {@code
     * MonsterTemplate#getPerception()} instead and ignores anything passed: a foe never rolls.
     * A {@code null} total from a rolling observer is "cannot tell", and sees nothing rather than
     * seeing everything.
     *
     * <p>observerIsExpert says whether that roll named a held {@code
     * AttentionSpecialization#SENTIDOS_APURADOS}, and eases the GD to the tier's expert threshold
     * if so — see {@link #getConcealmentValue(CombatantSheet, boolean)}. It is ignored for a foe,
     * which holds no Especialização on a roll it never makes. The four-argument form below is the
     * ordinary watcher.
     *
     * <p>Reaching the GD exactly succeeds, the same reading of a tie {@code
     * InteractionResult#getSucceeded()} and {@code org.aventyrs.core.combat.AttackReceiver} take.
     * A success is recorded on the concealment, so this observer keeps seeing hider without
     * re-rolling every Rodada; a failure records nothing, leaving the caller free to offer another
     * look later.
     *
     * @return whether observer sees hider now — including {@code true} when there was nothing to
     * see through in the first place (not hidden, an ally, or already spotted)
     */
    boolean resolveDetection(CombatantSheet hider, CombatantSheet observer, Integer attentionTotal,
                             boolean observerIsExpert, SceneContext hiderContext);

    /**
     * {@link #resolveDetection(CombatantSheet, CombatantSheet, Integer, boolean, SceneContext)} for
     * an observer with no matching Especialização.
     */
    boolean resolveDetection(CombatantSheet hider, CombatantSheet observer, Integer attentionTotal,
                             SceneContext hiderContext);

    /**
     * Whether trigger would give hider away — {@code false} when they are not hidden at all, and
     * for a {@link RevealTrigger#MOVEMENT} excused by a held {@code Feat#movesWhileHidden}.
     *
     * <p>The asking half of {@link #reveal}, for a caller pricing an action before committing to
     * it, the same split {@code WeaponDrawService#canDraw} keeps beside {@code draw}.
     */
    boolean reveals(CombatantSheet hider, RevealTrigger trigger);

    /**
     * Gives hider away if trigger does, lifting {@link ConditionType#ESCONDIDO} outright — the
     * whole concealment, for every observer at once, since what was given away was the hiding
     * place and not one creature's line of sight.
     *
     * @return whether this call actually ended a concealment — {@code false} when {@link #reveals}
     * says no, in which case nothing is touched
     */
    boolean reveal(CombatantSheet hider, RevealTrigger trigger);
}
