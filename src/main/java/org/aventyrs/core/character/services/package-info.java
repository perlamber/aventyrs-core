/**
 * Character-level services: creation, and the derived-stat calculators
 * (Hit/Magic/Determination Points, Reações, Iniciativa, etc.) built on top of a finished
 * {@link org.aventyrs.core.character.Character}.
 *
 * <h2>Creating a Character</h2>
 *
 * There's no single {@code createCharacter(...)} entry point — creation is a sequence of
 * independent choices, each validated by {@link org.aventyrs.core.character.services.CharacterCreationService},
 * that get assembled into a {@link org.aventyrs.core.character.Character} at the end via its
 * builder. As of this writing, the steps are:
 *
 * <ol>
 *   <li><b>Pick a {@link org.aventyrs.core.race.Race}</b> (e.g.
 *       {@link org.aventyrs.core.race.Human}) — it drives step 2's racial bonuses and
 *       the XP costs {@code Race} exposes for later advancement.</li>
 *   <li><b>Pick an {@link org.aventyrs.core.character.Alignment}</b> — it defaults to
 *       {@code NEUTRAL}, but some Talentos have a moral-alignment prerequisite (e.g. Corruptor
 *       Sombrio accepts {@code NEUTRAL} or {@code EVIL}).</li>
 *   <li><b>Allocate Attributes</b> — {@link org.aventyrs.core.character.services.CharacterCreationService#allocateAttributes}
 *       spends the {@value org.aventyrs.core.character.services.CharacterCreationService#STARTING_ATTRIBUTE_POINTS}
 *       starting points (no base above
 *       {@value org.aventyrs.core.character.services.CharacterCreationService#MAX_STARTING_ATTRIBUTE_BASE})
 *       plus the race's fixed/choosable racial bonuses, returning a
 *       {@link org.aventyrs.core.character.CharacterAttributes}.</li>
 *   <li><b>Allocate Egos</b> — {@link org.aventyrs.core.character.services.CharacterCreationService#allocateEgos}
 *       places the single extra point among the four
 *       {@link org.aventyrs.core.character.EgoDomain}s (every Ego starts at
 *       {@value org.aventyrs.core.character.services.CharacterCreationService#STARTING_EGO_POINTS}),
 *       returning a {@link org.aventyrs.core.character.CharacterEgos}. This allocation decides
 *       more than Vantagem eligibility (step 4) and the Iniciativa base {@link
 *       org.aventyrs.core.character.services.InitiativeService} reads: each Ego's total is also
 *       that domain's <b>permanent spendable point pool</b>, and — since the temporary ceiling
 *       tracks permanent points remaining — the size of its temporary pool too. See {@code
 *       org.aventyrs.core.sheet.EgoPointPool}, and {@link
 *       org.aventyrs.core.character.services.EgoPointsService} for the per-session recovery of
 *       the temporary half.</li>
 *   <li><b>Vantagens de Ego (conditional, one check per domain with a catalog)</b> — for each
 *       {@link org.aventyrs.core.character.EgoDomain} that has one (today: {@code AUTOCONTROLE}'s
 *       {@link org.aventyrs.core.ego.AutocontroleAdvantage}, {@code INICIATIVA}'s {@link
 *       org.aventyrs.core.ego.InitiativeAdvantage}), if {@link
 *       org.aventyrs.core.character.services.CharacterCreationService#isEgoAdvantageAvailable}
 *       is {@code true} for that domain against the {@code CharacterEgos} from step 3, the
 *       player may choose one constant from that domain's catalog; a domain the player didn't
 *       reach the threshold for (or that has no catalog yet) simply stays absent from {@link
 *       org.aventyrs.core.character.Character#getEgoAdvantage}. This eligibility can never be
 *       reached later — see that method's javadoc.</li>
 *   <li><b>Pick a {@link org.aventyrs.core.action.ActionProfile}</b> — one of the six, chosen
 *       once and permanent. It's a real mechanical choice, not flavour: the profile is the
 *       last stage of every per-Round PA/Reação/Ação Livre total, applied on top of every
 *       ability, excellency and granted bonus. Read those totals through the {@code
 *       CombatantSheet}-taking overloads of {@code
 *       org.aventyrs.core.action.ActionPointsService#getMaxActionPoints}, {@link
 *       org.aventyrs.core.character.services.ReactionsService#getTotalReactions} and {@link
 *       org.aventyrs.core.character.services.FreeActionsService#getTotalFreeActions} — the
 *       {@code Character}-taking ones report the permanent total, with no profile and no
 *       Round-scoped bonus applied.</li>
 *   <li><b>Assemble the {@code Character}</b> via {@link org.aventyrs.core.character.Character#builder()},
 *       passing the results of steps 1-6. Everything else (starting {@code skills},
 *       {@code attributeAbilities}, {@code activeAbilities}, {@code skillCompetencyAbilities},
 *       {@code abilityChoices}, {@code actionPoints}, {@code temporaryActionPointsBonus}, {@code sizeCategory},
 *       {@code status}, {@code reactions}, {@code freeActions}, {@code alignment}) has a
 *       sensible {@code @Builder.Default} and rarely needs overriding at creation. {@code
 *       sexo} ({@link org.aventyrs.core.character.Character.Sexo}) is the one exception with
 *       no default at all — {@code null} unless set, since no eligibility/validation logic
 *       for it exists here (unlike, say, step 4's Vantagens de Ego). {@code
 *       quickLearningSkills} is the one creation-time pick a race asks for here: an Aprendizado
 *       Rápido race ({@code Race#hasQuickLearning()} — Humano, Pequenino, Gnomo, Goblin) names its
 *       two Perícias Treinadas, whose 2nd and 3rd Graduação then cost 0.5 EXP less
 *       ({@code SkillGraduationService#getUpgradeCost(Character, SkillType)}). Empty by default.</li>
 *   <li><b>Defeitos e Qualidades (optional) — after the Perfil de Ação, before the Habilidades de
 *       Atributo and the Talentos</b>, through {@link
 *       org.aventyrs.core.character.services.CharacterCreationService#applyDefectsAndQualities}, which
 *       returns a new {@code Character}. It changes what the later steps offer: the Talentos step reads
 *       {@code getStartingFeatSlots(Character)} (the {@code Race} form no longer tells the whole story —
 *       a Qualidade traded for Talentos Gerais removes slots, a Superação adds one), and the Habilidades
 *       step gains {@code Character#getBonusAttributeAbilitySlots()}. See {@code org.aventyrs.core.defect}.</li>
 *   <li><b>Pick the two Antecedentes — last</b>, on the character step 7 built (its Talentos and
 *       Árvores below too): one Naturalidade and one Carreira, through {@link
 *       org.aventyrs.core.character.services.CharacterCreationService#applyBackground}, which
 *       <em>returns a new</em> {@code Character} with the Antecedente's Graduações, Especializações,
 *       Habilidades de Competência and Ego written in. "Se treinado em X" is judged against the
 *       character at this moment, so everything else must be chosen first. An Ego point it adds can
 *       make a Vantagem de Ego (step 5) newly available — re-check it afterwards. See {@code
 *       org.aventyrs.core.background} for the full protocol.</li>
 * </ol>
 *
 * <pre>{@code
 * CharacterCreationService creation = new CharacterCreationServiceImpl();
 * Race race = new Human();
 *
 * // 7 points total across the 7 Attributes, e.g. 2 in Vigor and Instinto, 1 in the rest
 * Map<AttributeDomain, Integer> basePoints = Map.of(
 *         AttributeDomain.VIGOR, 2, AttributeDomain.STRENGTH, 1, AttributeDomain.DEXTERITY, 1,
 *         AttributeDomain.FOCUS, 1, AttributeDomain.INSTINCT, 2, AttributeDomain.GNOSE, 0,
 *         AttributeDomain.CHARISMA, 0);
 * CharacterAttributes attributes = creation.allocateAttributes(race, basePoints, Map.of());
 * CharacterEgos egos = creation.allocateEgos(Map.of(EgoDomain.AUTOCONTROLE, 1));
 *
 * Character.CharacterBuilder builder = Character.builder()
 *         .player(player)
 *         .name("Aria")
 *         .race(race)
 *         .alignment(Alignment.NEUTRAL)
 *         .attributes(attributes)
 *         .egos(egos)
 *         .actionProfile(ActionProfile.REFLEXOS_RAPIDOS)
 *         .quickLearningSkills(Set.of(SkillType.ATTENTION, SkillType.ATLETISMO)); // Aprendizado Rápido
 *
 * if (creation.isEgoAdvantageAvailable(EgoDomain.AUTOCONTROLE, egos)) {
 *     builder.egoAdvantage(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.RESOLUTO); // player's choice
 * }
 * if (creation.isEgoAdvantageAvailable(EgoDomain.INICIATIVA, egos)) {
 *     builder.egoAdvantage(EgoDomain.INICIATIVA, InitiativeAdvantage.IMPETO); // player's choice
 * }
 *
 * // ... starting Talentos and Árvores (below), then the Antecedentes, last:
 * Character character = creation.applyBackgrounds(builder.build(),
 *         AcquiredBackground.of(OriginBackground.OFI, List.of(SkillType.ATTENTION, SkillType.PROFISSAO),
 *                 List.of(ProfissaoSpecialization.METALURGIA)),
 *         AcquiredBackground.of(CareerBackground.BATEDOR, List.of(), List.of()));
 * CombatantSheet sheet = CombatantSheet.of(character, player);
 * }</pre>
 *
 * <h2>Árvores de Magia, after the starting Talentos</h2>
 *
 * A character whose starting Talentos include {@code MetamagicoFeat#ARCANISTA} owes two more
 * picks, and they come <b>last</b>, because both depend on everything before them: the number
 * of Árvores is the Conhecimentos roll value (Graduação + Gnose), and the Brotos depend on the
 * Árvores. Nothing records them apart from the Magias themselves — replay them through {@link
 * org.aventyrs.core.character.services.SpellService} on the assembled {@code Character}:
 *
 * <pre>{@code
 * SpellService spells = new SpellServiceImpl();
 * CharacterSheet sheet = CharacterSheet.of(character, player);  // 0 XP is enough: all free
 * for (SpellTree tree : chosenTrees) {                          // up to getOpenTreeSlots
 *     spells.learnTree(character, sheet, tree);                 // grants its Semente
 * }
 * for (Spell broto : chosenBrotos) {                            // from getFreeSpellOptions(BROTO)
 *     spells.grantSpell(character, sheet, broto);               // the two free picks
 * }
 * }</pre>
 *
 * Picking one of a diverging Árvore's two Brotos <em>is</em> choosing its ramificação. The same
 * two questions ({@code getOpenTreeSlots}, {@code getOwedFreeSpells}) stay live after creation: a
 * raised Conhecimentos Graduação opens another Árvore, and each later ladder rung owes two more
 * picks at the rung it unlocks.
 *
 * <h2>Creating a foe instead</h2>
 *
 * None of the above applies to a monster. It has no player, allocates no points, picks no
 * Vantagens de Ego and no {@code ActionProfile} of its own (it carries {@code
 * MonsterTemplate.DEFAULT_ACTION_PROFILE}, the one profile that adjusts none of the three
 * counters, unless its stat block says otherwise) — its stats come from {@code
 * criacao-de-monstros.txt}'s own budgets and ceilings, not the ones step 2 enforces. Build one
 * through {@code org.aventyrs.core.monster.MonsterBlueprint} instead:
 *
 * <pre>{@code
 * MonsterSheet goblin = SampleMonster.GOBLIN_SELVAGEM.get().spawn(gm);   // built by the rules
 * MonsterSheet boss = MonsterBlueprint.builder()...                   // a designed one
 *         .build().spawn(gm);
 * }</pre>
 *
 * <p>A monster also cannot be advanced afterwards: {@link
 * org.aventyrs.core.character.services.CharacterAttributeService#upgradeBase}, {@link
 * org.aventyrs.core.character.services.SkillGraduationService#upgradeGraduation}, {@code
 * FeatService#grantFeat}, {@code TitleAbilityService#grantTitleAbility} and {@code
 * SpellService#grantSpell} all take a {@code CharacterSheet} because that is where experience
 * lives, and a {@code MonsterSheet} isn't one. ({@code grantSpell} spends {@code
 * SpellService#getAcquisitionCost} — the Magia's rung cost, waived by a Talento's free pick —
 * so a foe can't climb an Árvore de Magia either.)
 * That's a compile-time guarantee, not a runtime check — see {@code
 * org.aventyrs.core.sheet.CombatantSheet}.
 *
 * <h2>Keeping this current</h2>
 *
 * Both lists above are living inventories — the player one of every creation-time choice in
 * order, the foe one of how a monster is built instead. <b>Whenever a
 * new mechanic adds a creation-time choice</b> (a new Ego/Attribute-like allocation, another
 * "pick one permanently" enum like {@code ActionProfile}, a new conditional Vantagem like
 * Autocontrole's), add a numbered step here describing it and update the code example. Don't
 * let this drift: an API/UI built against a stale version of this list will silently miss
 * new required (or newly-available) choices.
 */
package org.aventyrs.core.character.services;
