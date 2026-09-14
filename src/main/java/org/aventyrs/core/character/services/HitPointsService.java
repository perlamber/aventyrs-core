package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterStatus;
import org.aventyrs.core.sheet.CombatantSheet;

public interface HitPointsService {
    int DEFAULT_LIFE_MULTIPLIER = 4;
    int BASE_HIT_POINTS = 10;

    /**
     * The Life Multiplier, base {@value #DEFAULT_LIFE_MULTIPLIER}, increased by sources such
     * as the Sobre-humano Vigor ability.
     */
    int getLifeMultiplier(Character character);

    /**
     * The same multiplier, plus whatever a Forma adds to it for as long as it is worn — the
     * "Multiplicador de PV aumenta em +2 para cada Título Aventyr Desperto" half of {@code
     * FeericoFeat#ANCIENTEFORME}, and {@code FormaMetamorfica#CAVALO_DE_CHIFRES}'s flat +1.
     *
     * <p>Both clauses arrive the same way: every held Talento's {@code
     * Feat#resolveLifeMultiplierIncrease(Character, CombatantSheet)}, resolved <em>from</em> the
     * worn Forma. Deliberately <b>not</b> a round-scoped {@code TemporaryBonus}, even though
     * {@code FormaActiveAbility} grants the Defesas and Categoria halves of Ancienteforme's very
     * sentence that way: a countdown is tied to itself rather than to the shape, so a holder
     * leaving early through {@code enterForm(null)} would keep the uplift and one still
     * transformed when it lapsed would lose it. Max PV is too visible a number to let drift apart
     * from the shape that granted it.
     *
     * <p><b>Read this wherever a sheet is in hand.</b> {@link #getLifeMultiplier(Character)} is
     * the Forma-blind figure — what the character is worth out of any shape — and stays that way.
     */
    int getLifeMultiplier(Character character, CombatantSheet characterSheet);

    /**
     * Every flat {@link org.aventyrs.core.modifier.ModifierType#HIT_POINTS} bonus the character
     * holds — the "recebe Bônus Mágico de +NPV" shape, whose amount its rules text states
     * outright rather than deriving from Vigor.
     *
     * <p>Scanned over both {@code getAttributeAbilities()} and {@link
     * org.aventyrs.core.skill.SkillCompetencyAbility#allFor} (so a racial or monster-authored
     * trait counts), which is deliberately <b>wider</b> than {@link #getLifeMultiplier}'s
     * Attribute-ability-only scan: that narrower scan is what its one existing consumer needs
     * and widening it would change a computed total nothing asked to change. Neither scans
     * {@code SkillExcellency} tiers — no Excelência grants either.
     *
     * <p>Not to be confused with the Life Multiplier. A multiplier scales with Vigor, so it
     * makes an already-tough creature proportionally tougher; this is flat, and must stay flat —
     * expressing a stated "+10PV" as a multiplier uplift only lands on the right number at one
     * specific Vigor.
     */
    int getHitPointsBonus(Character character);

    /**
     * Total (maximum) Hit Points: {@value #BASE_HIT_POINTS} plus Vigor's total value times the
     * Life Multiplier, plus {@link #getHitPointsBonus}.
     */
    int getMaxHitPoints(Character character);

    /**
     * The maximum a combatant currently has, Forma included — {@link
     * #getLifeMultiplier(Character, CombatantSheet)} in place of the Forma-blind multiplier, and
     * the sheet-aware Vigor total, so a shape that suppresses its holder's race takes the racial
     * Vigor off their PV too.
     *
     * <p><b>Max PV moves while transformed, and current PV follows it.</b> Damage is stored on
     * the sheet and is not re-scaled, so current PV is still {@code max - damageTaken}: entering
     * a Forma that raises the multiplier widens the headroom, and leaving it narrows it again. A
     * character who took damage while transformed can therefore drop a {@code CharacterStatus}
     * tier the moment the Duração lapses.
     *
     * <p>⚠️ That consequence is a <b>derivation, not a transcription</b> — the rules text states
     * what the multiplier becomes and says nothing about what happens to accumulated damage when
     * it falls back. The alternatives (re-scaling damage proportionally, or clamping the drop so
     * a Forma can never kill its holder) are equally inventable, so none is invented here: the
     * arithmetic is the plain one and the question is left visible. Nothing is written on
     * transforming, which is the same reason the Arma Natural swap and racial-trait suppression
     * are derived rather than mutated.
     */
    int getMaxHitPoints(Character character, CombatantSheet characterSheet);

    /**
     * Current Hit Points: the maximum minus the damage accumulated on the character's sheet,
     * never below zero.
     */
    int getCurrentHitPoints(Character character, CombatantSheet characterSheet);

    /**
     * Resolves which {@link CharacterStatus} tier a current/max Hit Points pair falls into.
     * Unlike {@link #getCurrentHitPoints}, {@code currentHitPoints} here is the raw,
     * unclamped value (maximum minus damage taken) — it can go negative, since damage can
     * exceed max Hit Points (see {@code ResourcePool}'s own javadoc) and that negative range
     * is exactly what distinguishes {@code FALLEN}/{@code COMMA}/{@code DEAD}.
     *
     * <ul>
     *   <li>{@code CLEAN}: at full Hit Points ({@code currentHitPoints >= maxHitPoints}).</li>
     *   <li>{@code HIGH_LIFE}: above two thirds of max.</li>
     *   <li>{@code MEDIUM_LIFE}: above one third of max.</li>
     *   <li>{@code LOW_LIFE}: above zero.</li>
     *   <li>{@code FALLEN}: zero or below, but above negative half of max.</li>
     *   <li>{@code COMMA}: negative half of max or below, but above negative max.</li>
     *   <li>{@code DEAD}: negative max or below.</li>
     * </ul>
     */
    CharacterStatus getStatus(int currentHitPoints, int maxHitPoints);

    /**
     * The {@link CharacterStatus} tier characterSheet's owner is in <i>right now</i>, derived
     * from the damage currently on the sheet — {@code getMaxHitPoints(sheet.getCharacter()) -
     * sheet.getDamageTaken()}, handed to {@link #getStatus(int, int)}.
     *
     * <p>That subtraction is deliberately <b>unclamped</b>, unlike {@link #getCurrentHitPoints},
     * which floors at zero: the negative range is exactly what distinguishes {@code FALLEN} from
     * {@code COMMA} from {@code DEAD}, so clamping here would make those three tiers
     * unreachable.
     *
     * <p>Resolved fresh on every call, never stored. This is the same "recompute on demand from
     * data already in hand" discipline {@code InitiativeEntry#getEffectiveInitiativeValue} uses
     * for a participant's live Iniciativa standing, and it exists for the same reason: a stored
     * copy needs every path that changes Hit Points to remember to refresh it, and the
     * per-Rodada ones ({@code Bleeding}, {@code Withering}), {@code CombatantSheet#heal} and
     * {@code RestService#applyRest} have no service in scope to refresh through.
     *
     * <p>It lives on this service rather than on {@code CombatantSheet} itself — which holds
     * both halves of the input and would read more naturally — because {@code
     * org.aventyrs.core.sheet} does not depend on {@code org.aventyrs.core.character.services},
     * and resolving a maximum needs {@link #getMaxHitPoints}'s Vigor/Life-Multiplier scan.
     * Putting it on the sheet would invert that dependency. The shape to compare it against is
     * {@link #getCurrentHitPoints} above, its sheet-taking sibling — not the "cascading
     * overload" convention, which delegates from a longer signature down to a shorter one; this
     * delegates the other way, up to the pure tier function.
     */
    CharacterStatus getStatus(CombatantSheet characterSheet);
}
