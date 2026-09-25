/**
 * The Skill -&gt; Damage -&gt; EffectChain -&gt; CriticalEffect pipeline: what happens after
 * a Skill roll lands, in order.
 *
 * <h2>The pipeline, and how it's chained</h2>
 *
 * Each stage is an ordinary {@link org.aventyrs.core.sheet.Interaction}&lt;{@link
 * org.aventyrs.core.sheet.CombatantSheet}&gt;, applied via {@link
 * org.aventyrs.core.sheet.CombatantSheet#receiveInteraction} exactly like a Perícia roll.
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
 * <p>{@code nextInteraction} is typed as the shared {@code Interaction<CombatantSheet>}
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
 * interfaces — each extends {@code Interaction<CombatantSheet>} so a concrete
 * implementation plugs into {@code receiveInteraction} with zero other code touched,
 * the same zero-touch guarantee every concrete {@code <Skill>Interaction} already
 * relies on. Three concrete {@code CriticalEffect}s exist so far. Two drain a resource
 * immediately plus per-Rodada: {@link org.aventyrs.core.effect.Sangramento} (PV, via
 * {@link org.aventyrs.core.sheet.Bleeding}) and {@link org.aventyrs.core.effect.ManaPurge}
 * (PM, via {@link org.aventyrs.core.sheet.ManaDrain}) — each interrupted by the matching
 * recovery ({@code CombatantSheet#heal}/{@code #recoverMagicPoints}). The third, {@link
 * org.aventyrs.core.effect.Primor}, is shaped differently: a one-time spend from the
 * target's <em>temporary</em> Ego pool (Sorte or Autocontrole), via {@code
 * CombatantSheet#spendEgoPoints} — never the permanent pool, which nothing in this ruleset
 * drains automatically — with no ongoing per-Rodada loss, instead owed back at the target's
 * next qualifying Rest via {@code org.aventyrs.core.sheet.PendingEgoRecovery} for what was
 * <em>actually</em> spent rather than what was asked for — resolved for real by {@code
 * RestServiceImpl#applyRest}, since {@code RestService} (unlike Scene's still-nonexistent
 * turn shifter) already is a complete "a Rest happened" trigger. A fourth, {@link
 * org.aventyrs.core.effect.Sabotage}, is a deliberate placeholder — it targets equipment
 * rather than a CombatantSheet resource pool, and although {@code Item}, {@code
 * CombatantSheet#getInventory} and {@code Item#applyDamage} are all real now, nothing
 * classifies an item as "tecnológico" and no Efeito Crítico damages one automatically, so its
 * {@code applyTo} computes nothing beyond {@code resultStatus}; see its own class javadoc
 * for the full breakdown. A fifth, {@link org.aventyrs.core.effect.RealExecution}, is
 * fully real — it guarantees {@code CharacterStatus#DEAD} (the closest thing this core
 * computes to "destroyed") unconditionally on Acerto Crítico Maior, or conditionally on
 * Menor (only when the target's current Hit Points are already at or below double their
 * Vigor); its own rules text's "não poderá ser ressuscitado" clause is enforced too — the
 * destroyed target is marked {@code CombatantSheet#markBeyondRevival()}, and no heal,
 * however permitted, reaches it again. All
 * five share the "reject anything that isn't an Acerto Crítico" constructor validation,
 * living once on {@link org.aventyrs.core.effect.CriticalEffect#validateCriticalHit}
 * rather than duplicated per implementation. {@link org.aventyrs.core.effect.EffectChain}
 * now has its first concrete implementation, {@link org.aventyrs.core.effect.Definhar}:
 * an ongoing per-Rodada curse-damage drain (via {@link
 * org.aventyrs.core.sheet.Withering}, applied through {@link
 * org.aventyrs.core.sheet.CombatantSheet#applyCurseDamage}) lasting Rodadas equal to the
 * target's own Vigor, non-cumulative per its own rules text — reapplying it replaces the
 * existing drain rather than stacking a second one. Unlike every {@code CriticalEffect}
 * above, it isn't gated on a {@code CriticalResult} at construction: its own rules text
 * has no Maior/Menor split, and it's triggered as a Corrente de Efeitos rather than a
 * critical-hit-only consequence.
 * A second concrete {@code EffectChain}, {@link org.aventyrs.core.effect.Sobrecura}, is the
 * bonus recovery three Magias of Vida's healing branch name ("+1d6+Metade do Foco PV"); like
 * {@code Definhar} it isn't gated on a {@code CriticalResult}, and like every dice figure in
 * this core its d6 arrives already rolled from the caller.
 *
 * <h2>Spell Effects — the third category</h2>
 *
 * {@link org.aventyrs.core.effect.SpellEffect} is a Magia's own {@code Efeito:} line made
 * executable, sitting beside {@code CriticalEffect} and {@code EffectChain} under {@code Effect}.
 * Where those two are named by what <em>triggered</em> them, a Spell Effect is named by what
 * produced it, and its four subcategories divide by what it <em>does</em>: {@link
 * org.aventyrs.core.effect.OffensiveEffect} (damage, debuffs), {@link
 * org.aventyrs.core.effect.DefensiveEffect} (buffs, cleansing), {@link
 * org.aventyrs.core.effect.HealingEffect} and {@link
 * org.aventyrs.core.effect.InvocationEffect}.
 *
 * <p>Two are concrete, and both are <b>parameterized by authored data rather than subclassed per
 * Magia</b> — the Magias of a ramificação share one effect that deepens as the tree climbs, so
 * one class serves a whole branch:
 *
 * <ul>
 *   <li>{@link org.aventyrs.core.effect.SpellHealingEffect} — built from a {@code
 *   org.aventyrs.core.magic.SpellHealing}, it restores as much as a Descanso of the named tier
 *   would ({@code RestService#getRecoveredHitPoints}, read against the <em>target's</em> Vigor)
 *   without being one, or every lost PV outright. Vida's principal branch is its whole catalog,
 *   from Descanso Mínimo at Semente to a full recovery at Florescente.</li>
 *   <li>{@link org.aventyrs.core.effect.ConditionCleansingEffect} — lifts the {@code
 *   ConditionType}s a Magia names. Vida's alternativo branch is its whole catalog.</li>
 * </ul>
 *
 * <p>{@code OffensiveEffect} and {@code InvocationEffect} have none: the first because no branch
 * needing one is wired yet (both its halves — {@code SpellDamage} and {@code applyCondition} —
 * already exist), the second because {@code Spell} has no column pointing at a {@code
 * MonsterTemplate} for a conjured creature to be built from.
 *
 * <p><b>How one gets built, and how a category is added.</b> {@link
 * org.aventyrs.core.effect.SpellEffectFactory} is the entry point, and it holds no mapping of its
 * own: {@link org.aventyrs.core.effect.SpellEffectKind} carries a supplier of each category's
 * {@link org.aventyrs.core.effect.SpellEffectBuilder}, and each builder reads <em>one authored
 * column</em> of the Magia, answering empty when that column is absent. So <b>adding a category
 * is a builder class plus a supplier on its constant — nothing else changes</b>, and in particular
 * {@code SpellCastingServiceImpl} needs no edit. That is the same
 * one-constant-carries-its-own-lookup shape {@code org.aventyrs.core.skill.SkillType} uses for
 * {@code interactionFactory}, with {@code SpellEffectFactory} as the thin facade {@code
 * SkillInteractionFactory} is over it.
 *
 * <p>Keyed on the Magia's <em>columns</em> rather than on a registry of effect classes, which is
 * what keeps it consistent with {@link org.aventyrs.core.effect.Effect}'s promise above: nothing
 * enumerates effect types, and no new {@code Effect} has to enlist anywhere to be applicable.
 *
 * <p><b>There is no "which effect" selector, because the version is one.</b> A Magia and its
 * {@code Efeito Alternativo} are two separate {@code Spell}s, each carrying its own effect
 * columns, so {@code SpellCastRequest#isUseAlternateVersion()} already answers it; each version
 * then authors at most one effect, which {@code SpellEffectFactoryTest} pins across the whole
 * catalog. An authoring mistake there stops the build rather than a cast.
 *
 * <p><b>A Spell Effect applies for real, but nothing fires one.</b> {@code
 * SpellCastingService#resolveEffect} delegates to that factory and {@code
 * SpellCastingResult#getSpellEffect()} hands the result back; the service never runs it, because
 * this core resolves no target GD and so cannot tell whether the cast landed. The caller decides,
 * then drives it through the same drain loop above — chaining a Corrente on first with {@code
 * AbstractEffect#chainInto} if it judges one triggered. Same boundary as every other stage here.
 *
 * <p>Several abilities/races are still blocked on a Corrente de Efeitos of their own — {@code
 * org.aventyrs.core.ego.AutocontroleAdvantage#RESOLUTO} (a Defesas-comparison threshold
 * on a Corrente de Efeitos — the Defesas system exists now, so what RESOLUTO still lacks is a
 * concrete Corrente whose text needs that comparison), {@code
 * org.aventyrs.core.skill.artes.ArtesCompetencyAbility#DISPARO_RICOCHETE}, {@code
 * org.aventyrs.core.skill.medicinaecura.MedicinaECuraCompetencyAbility#MILAGREIRO},
 * {@code org.aventyrs.core.ability.VigorAbility#RECUPERACAO_ASSOMBROSA}, and the racial
 * traits documented on {@code org.aventyrs.core.race.Gorgona}/{@code Flaminideo}/
 * {@code Invernal}. {@link org.aventyrs.core.effect.Definhar} existing doesn't
 * retroactively finish any of them — none of their own rules text describes Definhar's
 * effect, each still needs its own distinct Corrente de Efeitos built for it — the same
 * "a real (if empty) type now instead of a wholly nonexistent one" gap the interfaces
 * themselves closed, just one level further along.
 *
 * <p>One piece of a Corrente de Efeitos' own mechanic is real and tested, though: {@link
 * org.aventyrs.core.effect.EffectChainService} computes whether a Corrente de Efeitos
 * hits a target — the triggering roll's total must clear a required challenge number
 * (derived from an optional {@code DifficultyLevel}, a reduction step count, and a
 * caller-supplied variable bonus) by a margin, 5 normally or 7 when the target holds
 * {@code AutocontroleAdvantage#RESOLUTO}. This is exactly RESOLUTO's own rules text, now
 * computable — but nothing calls {@code EffectChainService#hits(...)} yet: {@link
 * org.aventyrs.core.effect.Definhar}'s own rules text has no challenge-number/margin
 * clause of its own to check, so even with a first concrete {@code EffectChain} now real,
 * this is still waiting on one whose own text actually needs this margin math.
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
 * {@code rawDamage} already resolved, and likewise decides when a critical actually
 * landed and constructs the matching {@code Sangramento}/{@code ManaPurge}/{@code
 * Primor}/{@code Sabotage}/{@code RealExecution} (or a future {@code EffectChain}
 * instance) itself — this core never triggers one on its own, and never rolls Primor's
 * own "definido aleatoriamente" domain choice or Sabotage's eventual 3d6 damage roll
 * either.
 */
package org.aventyrs.core.effect;
