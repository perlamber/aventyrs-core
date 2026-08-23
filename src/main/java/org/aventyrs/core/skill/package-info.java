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
 *         .skillRoll(new SkillRoll(List.of(4, 5, 6))) // the caller's own already-rolled dice
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
 * <p>This core deliberately never rolls dice, never knows what {@code DifficultyLevel} a
 * specific check is being made against, and never decides success or failure — a UI or API
 * layer sitting on top of this library owns the actual roll (add {@code rollBonus} to its own
 * dice result, shift its own target GD by {@code difficultyReduction}, then compare). This
 * keeps the core a pure rules calculator: given a Character's current state, what are the
 * inputs to this roll — nothing about how those inputs get turned into a die result.
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