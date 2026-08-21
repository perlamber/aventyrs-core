package org.aventyrs.core.combat;

import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.Singular;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.effect.CriticalEffect;
import org.aventyrs.core.effect.DamageInteraction;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillRoll;

import java.util.List;

/**
 * Everything one incoming attack needs, bundled — the request half of {@link
 * AttackReceiver#resolve}, modeled on {@code org.aventyrs.core.skill.SkillRollRequest}'s own
 * "required fields plus optional ones defaulting to {@code null}" shape.
 *
 * <p>Note what <i>isn't</i> here: the attacker's roll. This game's dice are always rolled by the
 * player, so an attack against a character presents a {@link #difficultyLevel} and a flat {@link
 * #attackBonus}, and it's the <b>defender</b> who rolls (via {@link #defenseRoll}).
 *
 * <p>Damage isn't here either, and that's deliberate rather than an omission: converting a roll
 * into a raw damage figure needs a weapon/dano-roll concept this core doesn't have (the gap
 * {@code org.aventyrs.core.effect}'s package-info calls the still-manual "Skill -&gt; Damage
 * handoff"). {@link AttackReceiver} therefore reports <i>which stages an attack triggers</i>,
 * pre-wired as a chain, and the caller supplies the damage figure when it applies the {@link
 * DamageInteraction} at the head of it.
 */
@Getter
@Builder
public class IncomingAttack {

    /** The character being attacked — the one who rolls Esquiva e Aparar. */
    @NonNull
    private final CharacterSheet defender;

    /** The Grau de Dificuldade this attack presents to the defender's roll. */
    @NonNull
    private final DifficultyLevel difficultyLevel;

    /** Whether the defender resists with DF or DM. */
    @NonNull
    private final DefenseType defenseType;

    /**
     * A flat modifier on top of {@link #difficultyLevel}'s own threshold — how much harder (or,
     * negative, easier) than the bare tier this particular attack is to avoid.
     */
    private final int attackBonus;

    /**
     * The defender's already-rolled 3d6, or {@code null} when the caller only wants the
     * bonuses/threshold reported and hasn't rolled yet (this core never rolls dice itself). With
     * {@code null}, every outcome on {@link IncomingAttackResult} stays {@code null}/empty — the
     * result is genuinely undetermined, not a miss.
     */
    private final SkillRoll defenseRoll;

    /** Nearby allies/enemies and their ranges, or {@code null} outside an encounter. */
    private final SceneContext sceneContext;

    /**
     * Who is attacking, or {@code null} when nothing/nobody in particular is (a trap, an
     * environmental effect).
     */
    private final CharacterSheet attacker;

    /**
     * The Efeitos Críticos this attack inflicts <i>if</i> the defense roll comes up a critical
     * failure — caller-constructed, because this core has no way to know which one a given
     * weapon or Magia carries (Sangramento? ManaPurge? RealExecution?), the same restraint
     * {@code org.aventyrs.core.effect}'s package-info already states. An attack may carry
     * several; all of them fire together when the critical lands, in the order given.
     *
     * <p>Each is gated at <i>its own</i> construction on being handed an Acerto Crítico (see
     * {@code CriticalEffect#validateCriticalHit}), so a caller builds these only for an attack
     * that can actually crit.
     */
    @Singular
    private final List<CriticalEffect> criticalEffects;

    /**
     * The Correntes de Efeitos this attack inflicts <i>if</i> it clears the defense by the
     * required margin — see {@link AttackReceiver} for that threshold. Independent of {@link
     * #criticalEffects}: an attack can trigger a Corrente without a critical, and a critical
     * without a Corrente.
     */
    @Singular
    private final List<EffectChain> effectChains;
}
