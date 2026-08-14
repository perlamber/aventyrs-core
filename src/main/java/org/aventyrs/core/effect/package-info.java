/**
 * The Skill -&gt; Damage -&gt; EffectChain -&gt; CriticalEffect pipeline: what happens after
 * a Skill roll lands, in order.
 *
 * <h2>The pipeline, and how it's chained</h2>
 *
 * Each stage is an ordinary {@link org.aventyrs.core.sheet.Interaction}&lt;{@link
 * org.aventyrs.core.sheet.CharacterSheet}&gt;, applied via {@link
 * org.aventyrs.core.sheet.CharacterSheet#receiveInteraction} exactly like a Perícia roll.
 * A stage that wants another stage to follow sets {@link
 * org.aventyrs.core.sheet.InteractionResult#getNextInteraction()} on its own result — a
 * caller drives the whole pipeline with one generic loop, never hardcoding which stages
 * apply to a given roll:
 *
 * <pre>{@code
 * InteractionResult result = sheet.receiveInteraction(new DamageInteraction());
 * while (result.getNextInteraction() != null) {
 *     result = sheet.receiveInteraction(result.getNextInteraction());
 * }
 * }</pre>
 *
 * <p>{@code nextInteraction} is typed as the shared {@code Interaction<CharacterSheet>}
 * interface, not any one concrete stage — {@link org.aventyrs.core.effect.EffectChain},
 * {@link org.aventyrs.core.effect.CriticalEffect}, {@link
 * org.aventyrs.core.effect.DamageInteraction} itself, or any future stage are all
 * interchangeable in that slot, and the loop above never inspects which one it's
 * holding. Whether to populate it is entirely each stage's own decision — a stage that
 * doesn't apply, or applies but has nothing worth chaining into, simply leaves it
 * {@code null} and the loop ends there. No central orchestrator decides this on a
 * stage's behalf; see {@link org.aventyrs.core.effect.DamageInteraction}'s own javadoc
 * for the one real gating rule this package ships (only forward when the hit actually
 * dealt damage).
 *
 * <h2>What's real today, and what isn't</h2>
 *
 * {@link org.aventyrs.core.effect.DamageInteraction} is a complete, concrete stage —
 * wraps {@link org.aventyrs.core.character.services.DamageService} to compute and apply
 * mitigated damage. {@link org.aventyrs.core.effect.Effect} (the parent of every Efeito),
 * {@link org.aventyrs.core.effect.EffectChain} (Corrente de Efeitos), and {@link
 * org.aventyrs.core.effect.CriticalEffect} (Efeito Crítico) are real, isolated
 * interfaces — each extends {@code Interaction<CharacterSheet>} so a concrete
 * implementation plugs into {@code receiveInteraction} with zero other code touched,
 * the same zero-touch guarantee every concrete {@code <Skill>Interaction} already
 * relies on — but have no concrete implementation yet. Several abilities/races are
 * already blocked on this becoming real: {@code
 * org.aventyrs.core.ego.AutocontroleAdvantage#RESOLUTO} (a Defesas-comparison threshold
 * on a Corrente de Efeitos — the Defesas system itself is also still missing), {@code
 * org.aventyrs.core.skill.artes.ArtesCompetencyAbility#DISPARO_RICOCHETE}, {@code
 * org.aventyrs.core.skill.medicinaecura.MedicinaECuraCompetencyAbility#MILAGREIRO},
 * {@code org.aventyrs.core.ability.VigorAbility#RECUPERACAO_ASSOMBROSA}, and the racial
 * traits documented on {@code org.aventyrs.core.race.Gorgona}/{@code Flaminideo}/
 * {@code Invernal}. Building the interfaces doesn't retroactively finish any of them —
 * each is still exactly as TODO'd as before, just against a real (if empty) type now
 * instead of a wholly nonexistent one.
 *
 * <p>One piece of a Corrente de Efeitos' own mechanic is real and tested, though: {@link
 * org.aventyrs.core.effect.EffectChainService} computes whether a Corrente de Efeitos
 * hits a target — the triggering roll's total must clear a required challenge number
 * (derived from an optional {@code DifficultyLevel}, a reduction step count, and a
 * caller-supplied variable bonus) by a margin, 5 normally or 7 when the target holds
 * {@code AutocontroleAdvantage#RESOLUTO}. This is exactly RESOLUTO's own rules text, now
 * computable — but nothing calls {@code EffectChainService#hits(...)} yet, since no
 * concrete {@code EffectChain} implementation exists to call it from.
 *
 * <p>The Skill -&gt; Damage handoff itself is still manual: there is no
 * {@code SkillRoll}-to-{@code rawDamage} conversion (that needs a weapon/dano-roll
 * concept this core doesn't have yet), so a caller computes the Skill roll, then
 * separately constructs and calls {@code DamageInteraction} with whatever raw damage it
 * rolled — the same honestly-deferred gap {@link
 * org.aventyrs.core.magic.SpellCastingService} documents for its own two-roll
 * orchestration ("Casting a Magia is two separate rolls"). From {@code
 * DamageInteraction} onward, though, {@code nextInteraction} is a real, working wire-up
 * mechanism, ready the moment a concrete {@code EffectChain}/{@code CriticalEffect}
 * exists to hand to it.
 *
 * <p>This core still never rolls dice, never decides whether an attack landed, and
 * never detects a critical on its own — same boundary as {@link org.aventyrs.core.skill}
 * (see that package's own "What this library computes" section). A caller supplies
 * {@code rawDamage} already resolved, and would likewise supply whichever concrete
 * {@code EffectChain}/{@code CriticalEffect} instance applies once those exist.
 */
package org.aventyrs.core.effect;
