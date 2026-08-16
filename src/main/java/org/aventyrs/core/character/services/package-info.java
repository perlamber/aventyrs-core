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
 *       returning a {@link org.aventyrs.core.character.CharacterEgos}.</li>
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
 *       once and permanent.</li>
 *   <li><b>Assemble the {@code Character}</b> via {@link org.aventyrs.core.character.Character#builder()},
 *       passing the results of steps 1-5. Everything else (starting {@code skills},
 *       {@code attributeAbilities}, {@code activeAbilities}, {@code skillCompetencyAbilities},
 *       {@code abilityChoices}, {@code actionPoints}, {@code temporaryActionPointsBonus}, {@code sizeCategory},
 *       {@code status}, {@code reactions}, {@code freeActions}, {@code tendencia}) has a
 *       sensible {@code @Builder.Default} and rarely needs overriding at creation. {@code
 *       sexo} ({@link org.aventyrs.core.character.Character.Sexo}) is the one exception with
 *       no default at all — {@code null} unless set, since no eligibility/validation logic
 *       for it exists here (unlike, say, step 4's Vantagens de Ego).</li>
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
 *         .attributes(attributes)
 *         .egos(egos)
 *         .actionProfile(ActionProfile.REFLEXOS_RAPIDOS);
 *
 * if (creation.isEgoAdvantageAvailable(EgoDomain.AUTOCONTROLE, egos)) {
 *     builder.egoAdvantage(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.RESOLUTO); // player's choice
 * }
 * if (creation.isEgoAdvantageAvailable(EgoDomain.INICIATIVA, egos)) {
 *     builder.egoAdvantage(EgoDomain.INICIATIVA, InitiativeAdvantage.IMPETO); // player's choice
 * }
 *
 * Character character = builder.build();
 * CharacterSheet sheet = CharacterSheet.of(character, player);
 * }</pre>
 *
 * <h2>Keeping this current</h2>
 *
 * This list is a living inventory of every creation-time choice, in order — <b>whenever a
 * new mechanic adds a creation-time choice</b> (a new Ego/Attribute-like allocation, another
 * "pick one permanently" enum like {@code ActionProfile}, a new conditional Vantagem like
 * Autocontrole's), add a numbered step here describing it and update the code example. Don't
 * let this drift: an API/UI built against a stale version of this list will silently miss
 * new required (or newly-available) choices.
 */
package org.aventyrs.core.character.services;
