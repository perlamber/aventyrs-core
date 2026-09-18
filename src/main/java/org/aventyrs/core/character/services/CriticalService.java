package org.aventyrs.core.character.services;

import org.aventyrs.core.character.CriticalDamage;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillType;

/**
 * The two figures an Acerto Crítico turns on: the <b>Margem Crítica Menor</b> a roll has to reach,
 * and the <b>Dano Crítico</b> the hit then adds to its own dano roll.
 *
 * <p>This is the same arithmetic {@code AbstractSkillInteraction} applies while resolving a roll —
 * it delegates here rather than keeping a second copy — exposed so a caller can ask
 * <em>beforehand</em>. A sheet screen showing "Margem Crítica Menor 16" for the equipped weapon, or
 * "Dano Crítico +2" beside it, needs exactly these answers and has no roll to resolve yet.
 *
 * <p>Both take the attack as it would actually be made: the {@link AttackSource} decides the weapon
 * column and every weapon-scoped clause, and the {@link SceneContext} decides the conditional ones
 * (a Cena de Combate's Rodada, the Terreno, the opposed character). {@code null} for either is
 * "not stated", and every clause conditioned on what wasn't stated simply doesn't apply.
 */
public interface CriticalService {

    /**
     * The 3d6 total an Acerto Crítico Menor has to reach for this holder attacking with this
     * source right now — the weapon's own authored margin, lowered by every "+1 número" the holder
     * currently has (Habilidades, Excelências, Vantagens, Talentos and the weapon's own fitted
     * enhancements) and raised back by the target's Resistência a Críticos. <b>Lower is wider.</b>
     *
     * <p>Never above the weapon's own authored margin: RC cancels widening rather than making a
     * crit harder than the baseline, the same "até o mínimo de 17" clamp the roll itself applies.
     *
     * @param target the attack's victim, whose Resistência a Críticos narrows the margin back, or
     * {@code null} when there is nobody in particular to attack yet.
     */
    int getLesserCriticalMargin(CombatantSheet holder, SkillType skillType, AttackSource attackSource,
                                SceneContext sceneContext, CombatantSheet target);

    /**
     * The weapon's own authored Margem Crítica Menor, before any widening — {@code
     * Weapon#DEFAULT_LESSER_CRITICAL_MARGIN} for an attack made with anything that is not a weapon
     * (a Magia, an unarmed strike) and for a plain Perícia test. Carried alongside {@link
     * #getLesserCriticalMargin} so a caller can show the widening as the modifier it is — "16
     * (base 17)" — rather than one number whose provenance nothing can recover.
     */
    int getBaseLesserCriticalMargin(AttackSource attackSource);

    /**
     * Every "+1 número" of widening this holder currently has for this attack, summed — the
     * Habilidade, Excelência, Vantagem and Talento scans plus the wielded weapon's fitted
     * enhancements. <b>Not</b> narrowed by any target's Resistência a Críticos; {@link
     * #getLesserCriticalMargin} applies that, and clamps.
     */
    int sumCriticalMarginIncrease(CombatantSheet holder, SkillType skillType, AttackSource attackSource,
                                  SceneContext sceneContext);

    /**
     * What a critical hit would add to its own dano roll — the baseline Vantagem em Danos, plus (or
     * replaced by) whatever the holder's Talentos and the wielded weapon's enhancements grant. See
     * {@link CriticalDamage}.
     *
     * <p>{@link CriticalDamage#NONE} for a Perícia that is not an attack one: nothing else has a
     * dano roll to add to.
     *
     * @param criticalResult which critical this is being asked about — a clause may distinguish
     * Menor from Maior. {@code CriticalResult#ACERTO_CRITICO_MENOR} is the honest default for a
     * caller previewing "what a crit is worth".
     * @param skillRoll the roll being made, for a clause scoped to how the attack was made
     * ({@code ArtilhariaFeat#MIRA_MORTAL} reads {@code SkillRoll#activated}); {@code null} when
     * nothing has been rolled yet, which such a clause reads as "not activated".
     */
    CriticalDamage getCriticalDamage(CombatantSheet holder, SkillType skillType, AttackSource attackSource,
                                     CriticalResult criticalResult, SkillRoll skillRoll, SceneContext sceneContext);
}
