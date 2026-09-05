/**
 * Perícias (Skills) and how to roll them.
 *
 * <h2>Performing a Skill Roll</h2>
 *
 * A Skill Roll is requested by constructing the concrete
 * {@link org.aventyrs.core.sheet.Interaction} for that Perícia — one class per Perícia, named
 * {@code <Skill>Interaction} (e.g. {@link org.aventyrs.core.skill.attention.AttentionInteraction},
 * {@link org.aventyrs.core.skill.artes.ArtesInteraction}) — and handing it to the target's
 * {@link org.aventyrs.core.sheet.CombatantSheet#receiveInteraction}:
 *
 * <pre>{@code
 * CombatantSheet sheet = CombatantSheet.of(character, player);
 *
 * InteractionResult result = sheet.receiveInteraction(new AttentionInteraction());
 *
 * int rollBonus            = result.getSkillRollBonus();      // add to the caller's own dice roll
 * int difficultyReduction  = result.getDifficultyReduction(); // steps to shift the target GD easier
 * CharacterStatus status   = result.getResultStatus();        // the character's current status
 * }</pre>
 *
 * <h2>Telling the roll what it is up against, and reading the verdict</h2>
 *
 * A {@link org.aventyrs.core.skill.SkillRoll} may state the <b>Grau de Dificuldade it was made
 * against</b>. Supply it and the result reports whether the roll beat it; leave it out and the
 * verdict is {@code null}, which means <b>"nobody said"</b> — not "failed":
 *
 * <pre>{@code
 * SkillRoll roll = SkillRoll.against(List.of(4, 5, 6), DifficultyLevel.MEDIUM);
 *
 * // receiveInteraction takes the Interaction alone, so a roll goes through applyTo…
 * InteractionResult result = new AtletismoInteraction().applyTo(sheet, sceneContext, roll);
 *
 * // …or through the factory, which carries the same SkillRoll:
 * InteractionResult result = SkillInteractionFactory.resolve(SkillRollRequest.builder()
 *         .skillType(SkillType.ATLETISMO).target(sheet).skillRoll(roll).build());
 *
 * Boolean succeeded = result.getSucceeded(); // TRUE / FALSE / null == "no target was stated"
 * Integer margin    = result.getMargin();    // signed distance from the target; null likewise
 * }</pre>
 *
 * <p><b>A caller must not read {@code null} as a failure.</b> The three states are distinct, and
 * every ability gated on success treats {@code null} as "cannot tell".
 *
 * <p>The target is a plain number, not a
 * {@link org.aventyrs.core.skill.DifficultyLevel} — most GDs are a tier, and
 * {@code SkillRoll.against(dice, tier)} resolves one for you, but some are computed
 * ("GD 10 + Vigor") and land between tiers. Use
 * {@code new SkillRoll(dice, requestedAbility, targetValue)} for those.
 *
 * <p>A {@link org.aventyrs.core.skill.SkillRoll} may also carry an
 * {@link org.aventyrs.core.sheet.ActionCost} — what the action this roll represents cost (a
 * Pontos de Ação amount, an Ação Livre, or a Reação) — via
 * {@code new SkillRoll(dice, requestedAbility, targetValue, actionCost)}. It is metadata: only a
 * cost-gated Talento reads it ({@code AssassinoFeat#SAQUE_RELAMPAGO}). {@code null} means "not
 * stated".
 *
 * <p>Two things happen automatically once a target is stated, and a caller needs to know about
 * both because neither is visible in the dice:
 *
 * <ul>
 *   <li><b>The roller's own {@code difficultyReduction} is already applied</b> — it eases the
 *   target by whole <i>níveis</i> before comparing, so do <b>not</b> subtract it again yourself.
 *   {@code getDifficultyReduction()} is still reported for display.</li>
 *   <li><b>An ability may make the roll succeed outright</b>, regardless of the dice
 *   ("sempre bem-sucedido, dispensando rolagens"). Such a roll reports {@code succeeded == true}
 *   with a {@code margin} of {@code 0}.</li>
 * </ul>
 *
 * <p>A roll that succeeds may also earn {@link org.aventyrs.core.sheet.Blessing}s — reported on
 * {@code result.getBlessings()} for the caller to grant, exactly as the initiative-time ones
 * are. They are never applied by the Interaction itself, since a {@code Blessing}'s
 * {@link org.aventyrs.core.sheet.TargetScope} may name someone other than the roller.
 *
 * <p>A caller that only has a {@link org.aventyrs.core.skill.SkillType} in hand (e.g. an API
 * layer deserializing an incoming roll request) doesn't need to know which concrete
 * {@code <Skill>Interaction} class that maps to — {@link org.aventyrs.core.skill.SkillRollRequest}
 * bundles skillType/target (and optionally an already-rolled
 * {@link org.aventyrs.core.skill.SkillRoll}/{@link org.aventyrs.core.scene.SceneContext}), and
 * {@link org.aventyrs.core.skill.SkillInteractionFactory#resolve} dispatches it:
 *
 * <pre>{@code
 * SkillRollRequest request = SkillRollRequest.builder()
 *         .skillType(SkillType.ARTES)
 *         .target(sheet)
 *         .skillRoll(SkillRoll.against(List.of(4, 5, 6), DifficultyLevel.MEDIUM))
 *         .build();
 *
 * InteractionResult result = SkillInteractionFactory.resolve(request);
 * }</pre>
 *
 * <p>A {@link org.aventyrs.core.skill.SkillRoll} may also name a {@code requestedAbility} — a
 * {@link org.aventyrs.core.skill.SkillTrait} the character is specifically invoking with this
 * roll, as opposed to a plain Perícia test. This can be either one of the character's
 * {@link org.aventyrs.core.skill.SkillCompetencyAbility} maneuvers, or one of their held
 * {@link org.aventyrs.core.skill.SkillSpecialization}s (see
 * {@link org.aventyrs.core.character.CharacterSkill#getSpecializations()}). If the character
 * doesn't actually hold that trait, the roll is rejected with an
 * {@link org.aventyrs.core.sheet.IllegalOperationException} rather than silently computing a
 * result for a maneuver/Especialização they never acquired. When the requested trait is a
 * {@code SkillSpecialization}, the roll's reached {@link org.aventyrs.core.skill.DifficultyLevel}
 * is resolved against that tier's easier {@link org.aventyrs.core.skill.DifficultyLevel#getExpertValue()}
 * threshold instead of its {@link org.aventyrs.core.skill.DifficultyLevel#getBaseValue()}.
 *
 * <h2>What this library computes — and what it leaves to the caller</h2>
 *
 * {@code skillRollBonus} already folds in everything this core knows about: the trained
 * {@code AttributeDomain} total plus {@code SkillGraduation} (or
 * {@link org.aventyrs.core.skill.Skill#UNTRAINED_PENALTY} if never trained), any Vantagem or
 * other flat bonus from {@code AttributeAbility}/{@code SkillCompetencyAbility}/unlocked
 * {@link org.aventyrs.core.skill.SkillExcellency} tiers (Vantagem is a flat
 * {@link org.aventyrs.core.skill.Skill#ADVANTAGE_BONUS}, not a reroll — see
 * {@code Skill.ADVANTAGE_BONUS}'s javadoc). {@code difficultyReduction} is the number of GD
 * steps the target {@link org.aventyrs.core.skill.DifficultyLevel} should be shifted easier by
 * (via {@link org.aventyrs.core.skill.DifficultyLevel#easier}).
 *
 * <p>The result also reports {@code governingAttributeDomain} — the {@code AttributeDomain} that
 * actually governed the roll after every substitution — {@code null} unless a {@code SkillRoll}
 * was supplied.
 *
 * <p>This core deliberately never rolls dice, never knows what {@code DifficultyLevel} a
 * specific check is being made against, and never decides success or failure — a UI or API
 * layer sitting on top of this library owns the actual roll (add {@code rollBonus} to its own
 * dice result, shift its own target GD by {@code difficultyReduction}, then compare). This
 * keeps the core a pure rules calculator: given a Character's current state, what are the
 * inputs to this roll — nothing about how those inputs get turned into a die result.
 *
 * <h2>Recording an action</h2>
 *
 * After resolving a roll, the caller may log it on the combatant's per-Rodada action log:
 *
 * <pre>{@code
 * InteractionResult result = new AtaqueADistanciaInteraction().applyTo(sheet, ctx, roll, target, weapon);
 * sheet.recordAction(new CombatantAction(
 *         SkillType.ATAQUE_A_DISTANCIA, result.getGoverningAttributeDomain(), weapon,
 *         roll.getActionCost(), scene.getCurrentRound(), ActionOutcome.from(result)));
 * }</pre>
 *
 * {@code applyTo} never records for you — it only <em>reads</em> the log (for
 * {@code DexterityAbility#PRECISAO}'s "first roll of your Turn" and
 * {@code AssassinoFeat#SAQUE_RELAMPAGO}'s "first cheap attack of the Rodada"). Not recording
 * means those never advance. Clear the log each Rodada with {@code sheet.startNewRound()}, or
 * drive the Scene with {@code Scene#next()}, which does it for every participant at the wrap.
 *
 * <h2>Adding a new Perícia</h2>
 *
 * See the project's {@code CLAUDE.md} for the full checklist — a new Perícia needs, at
 * minimum, its own {@link org.aventyrs.core.skill.Skill}, {@link org.aventyrs.core.skill.SkillType}
 * constant, and {@code <Skill>Interaction}. Every concrete
 * {@link org.aventyrs.core.sheet.Interaction} implementation binds its generic parameter to
 * {@link org.aventyrs.core.sheet.CombatantSheet} (the only {@link org.aventyrs.core.sheet.Interactable}
 * today), and {@link org.aventyrs.core.sheet.CombatantSheet#receiveInteraction} always just
 * delegates to it — {@code return interaction.applyTo(this);} — so a new Perícia is added
 * without ever touching {@code CombatantSheet} itself.
 */
package org.aventyrs.core.skill;