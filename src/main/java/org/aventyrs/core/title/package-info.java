/**
 * Títulos Aventyrs and how a Character holds them.
 *
 * <h2>Granting and reading a Título</h2>
 *
 * An {@link org.aventyrs.core.title.AventyrTitle} is a held instance — it carries the chosen
 * Especializações/Habilidades, the same "instance carries the acquisition-time choice" shape
 * as {@code org.aventyrs.core.ego.MoralHerdadaAbility}. A character holds **exactly three**
 * Título slots — Primário/Secundário/Terciário, {@link org.aventyrs.core.character.TitleSlot}
 * — unlike {@link org.aventyrs.core.race.Race} (exactly one field per Character); a slot may
 * be left empty:
 *
 * <pre>{@code
 * Santo santo = new Santo(List.of(), List.of(SantoAbility.BASTIAO_DOS_NECESSITADOS));
 * character.grantTitle(santo, TitleSlot.PRIMARY);
 *
 * AventyrTitle primary       = character.getPrimaryTitle();  // null if that slot is empty
 * List<AventyrTitle> all     = character.getAllTitles();     // only the filled slots
 * }</pre>
 *
 * <p>{@link org.aventyrs.core.character.Character#getPrimaryTitle()}/{@code #getSecondaryTitle()}/
 * {@code #getTertiaryTitle()} are plain nullable fields, not the {@code @Singular} immutable-list
 * shape {@code skillCompetencyAbilities}/{@code attributeAbilities} use — see
 * {@code Character#grantTitle(AventyrTitle, TitleSlot)}'s own javadoc for why. Which slot a
 * Título occupies is a fact about the *Character*, not something the held {@code AventyrTitle}
 * instance reports about itself — there is no {@code isPrimaryTitle()} method on {@code
 * AventyrTitle}; "is this the character's Título Primário" is answered by
 * {@code character.getPrimaryTitle() == title}, not by asking the instance.
 *
 * <p>{@link org.aventyrs.core.title.TitleAcquisitionService#grantTitle} is the validated grant
 * (a held Talento may prohibit a Título), and {@code #isPermitted} is the same check without the
 * grant. {@link org.aventyrs.core.title.TitleCatalog#all()} lists one fresh instance of every
 * Título family. A Título owed for later is a {@link org.aventyrs.core.title.TitleAwakening},
 * granted by {@code CharacterSheet#applySessionEndAcquisitions()}.
 *
 * <h2>Activating a Título trait</h2>
 *
 * Every activated Habilidade, Suprema or Especialização goes through one entry point, {@link
 * org.aventyrs.core.title.AventyrTitle#activateAbility}: it checks the trait is held, builds the
 * {@link org.aventyrs.core.title.AbstractTitleAbilityInteraction} the trait's catalog constant
 * names in {@code interactionClass}, and runs it. That base refuses under Silêncio, checks the
 * PD amount against the trait's {@link org.aventyrs.core.title.PDCost} and the activator's
 * current PD, runs the trait's own checks, and only then spends the PD and resolves the effect —
 * so a refused activation costs nothing. The PD paid comes back on {@code
 * InteractionResult#getDeterminationPointsSpent()}; the PA/Reação/Ação Livre cost is reported by
 * the trait and never deducted.
 *
 * <pre>{@code
 * InteractionResult result = santo.activateAbility(AbencoadoPelaLuzAbility.ORGULHO_ELDURIANO,
 *         TitleAbilityActivationRequest.builder()
 *                 .activator(holderSheet)
 *                 .scene(scene)                        // this trait registers an Aura
 *                 .sceneContext(holderContext)         // binds foes already in range
 *                 .determinationPoints(3)              // Variável: the player's pick; 3 Rodadas
 *                 .build());
 *
 * santo.activateAbility(SantoSpecialization.ABENCOADO_PELA_LUZ,
 *         TitleAbilityActivationRequest.builder()
 *                 .activator(holderSheet)
 *                 .target(allySheet)                   // omitted = the activator
 *                 .choice(AbencoadoPelaLuzInteraction.Branch.HEAL)
 *                 .build());                           // Fixed 1PD: no amount needed
 * }</pre>
 *
 * <p>What each trait needs from the request is its own call — a missing Scene or choice is
 * refused with {@code TITLE_ABILITY_REQUIRES_SCENE}/{@code TITLE_ABILITY_CHOICE_REQUIRED}. A
 * trait whose effect is reported rather than applied (Grito de Guerra Vulcano's {@code
 * Blessing}s) still leaves granting them to the caller.
 *
 * <h2>Around every attack: {@link org.aventyrs.core.title.TitleAttackModifiers}</h2>
 *
 * A held Título can also shape its holder's attacks without being activated — {@code
 * SenhorDaBriga}'s natural-weapon clauses. Everything that lands inside a roll is applied by this
 * core's own scans ({@code AventyrTitle#resolveCriticalMarginIncrease}/{@code
 * #resolveCriticalDamage}/{@code #resolveDamageBaseIncrease}/{@code #resolveAttackRollBonus}/
 * {@code #resolveDamageRollBonus}/the holder-aware {@code #resolveBaseDefesasBonus}). What a caller
 * must apply comes back from one query before the attack is built, and one call after it:
 *
 * <pre>{@code
 * TitleAttackModifiers mods = TitleAttackModifiers.resolve(attackerSheet, weapon, defenderSheet, context);
 * // roll mods.extraDamageDice() more d6, lower the PA price by mods.actionPointReduction(),
 * // roll against mods.defenseType() and type the damage mods.damageDescriptor() when non-null,
 * // and add mods.effectChains() to the DeliveredAttack
 * DeliveredAttackResult result = attackDelivery.resolve(request.build());
 * TitleAttackModifiers.consumeCharges(attackerSheet, weapon);   // after — the roll read the budget
 * }</pre>
 *
 * <p>Combat-scoped grants ("até o final da Cena") end at {@code Scene#endCombat()}, which the GM
 * calls; a caller rebuilding a Scene from persistence sets the flag with {@code setCombatScene}.
 *
 * <h2>Adding a new Título</h2>
 *
 * A concrete Título's classes live together in their own subpackage,
 * {@code org.aventyrs.core.title.<titlename>} (e.g. {@link org.aventyrs.core.title.santo}) —
 * mirroring {@code org.aventyrs.core.skill.<skillname>}'s one-subpackage-per-catalog
 * convention. Add it to {@link org.aventyrs.core.title.TitleCatalog} too, or no Talento will
 * offer it. See the project's {@code CLAUDE.md} "Adding a new Título" section (and the
 * {@code adding-a-title} Claude Code skill) for the full checklist; {@code Santo} is the
 * worked reference example for activations, {@code SenhorDaBriga} for passives reaching the attack,
 * {@code GiganteEnfurecido} for a timed self-state ({@code sheet.Frenzy}) paid in Autocontrole
 * ({@link org.aventyrs.core.title.EgoCost}) whose Especializações are modes of one activation
 * ({@code TitleAbilityActivationRequest#getChoices}), and {@code Curandeiro} for a Título that bends the
 * limits on healing the fallen ({@link org.aventyrs.core.title.AventyrTitle#bypassesComaHealingCap},
 * {@link org.aventyrs.core.title.AventyrTitle#claimRevival}) — asked of the <b>healer's</b> Títulos by
 * {@code CombatantSheet#heal(int, HealingSource)}; pass the caster wherever a heal is built, or the
 * hooks never see it (see {@code docs/curandeiro.md}).
 */
package org.aventyrs.core.title;
