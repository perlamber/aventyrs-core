/**
 * Foes — the other kind of combatant, and the stat blocks that produce them.
 *
 * <h2>Why a foe needs its own type</h2>
 *
 * Before this package, "the enemy" was a {@code CharacterSheet} standing in for one: every
 * {@code enemies} list in {@code Scene}/{@code SceneContext} was typed that way. That works right
 * up until you need a foe to be what a foe actually is — no {@code Player}, no experience, no
 * Fama, Attributes and Graduações past any player ceiling, and fixed numbers where a player has
 * dice.
 *
 * <p>So {@link org.aventyrs.core.sheet.CombatantSheet} was extracted from
 * {@code CharacterSheet}, and {@link org.aventyrs.core.monster.MonsterSheet} is the other
 * implementation. Everything combat touches — damage, shields, Mana, Efeitos, inventory,
 * initiative, the Turn lifecycle — is shared verbatim through
 * {@code AbstractCombatantSheet}; only the player-only half stayed behind.
 *
 * <h2>Monsters can't level up, and nothing checks for it</h2>
 *
 * A monster's Attributes and Graduações are uncapped. That needed no mechanism: this core's caps
 * have only ever been enforced on the XP-spending services, never on construction —
 * {@code AttributeValue.builder().base(9)} has always been legal.
 *
 * <p>What needed solving was the reverse, and the type split solved it for free. Experience lives
 * on {@code CharacterSheet}, so {@code CharacterAttributeService#upgradeBase}, {@code
 * SkillGraduationService#upgradeGraduation}, {@code FeatService#grantFeat} and {@code
 * TitleAbilityService#grantTitleAbility} all take that concrete type. A {@code MonsterSheet}
 * cannot be passed to any of them. No {@code isMonster()} flag, no runtime guard — it doesn't
 * compile.
 *
 * <h2>Building one</h2>
 *
 * <b>By the rules</b> — {@code docs/rules/criacao-de-monstros.txt} — is the path for any monster a
 * Mestre authors. A {@link org.aventyrs.core.monster.MonsterBlueprint} holds the <i>choices</i> (Grau
 * de Poder, Regular or Exemplar, Atributos, trained Perícias and their upgrades, Modelos and
 * Habilidades Monstruosas, Talentos, Egos) and {@link org.aventyrs.core.monster.MonsterRules} derives
 * every number from them — the Categoria and its ceilings, each Perícia's GD, the Defesas, PA, the
 * PV/PD/PM formula — and validates the budgets:
 *
 * <pre>{@code
 * MonsterBlueprint pantera = MonsterBlueprint.builder()
 *         .name("Pantera de Cireneia")
 *         .powerDegree(30)                                   // → Predador
 *         .attributeBase(AttributeDomain.DEXTERITY, 5)        // 10 points above base 1, max 5
 *         .trainedSkill(SkillType.ESQUIVA_E_APARAR)
 *         .gnoseUpgrade(SkillType.ESQUIVA_E_APARAR)
 *         .progressionUpgrade(SkillType.ESQUIVA_E_APARAR, 2)
 *         .model(MonsterModel.ABENCOADO_DE_CIRENEIA)
 *         .ability(MonstrousAbilitySelection.of(CireneiaAbility.ATRIBUTOS_APRIMORADOS))
 *         .build();
 * List<MonsterViolation> problems = MonsterRules.validate(pantera);   // empty when legal
 * MonsterSheet sheet = pantera.spawn(gm);
 * }</pre>
 *
 * <p>{@link org.aventyrs.core.monster.SampleMonster} holds worked, legal examples.
 *
 * <p><b>A fixed stat block</b> — {@link org.aventyrs.core.monster.AbstractMonsterTemplate} — states
 * its Defesas and GDs outright instead. It is kept for summons (whose numbers come from their
 * Conjurador, not from these rules) and for hand-built foes in tests:
 *
 * <pre>{@code
 * MonsterSheet troll = AbstractMonsterTemplate.builder()
 *         .name("Troll da Ponte Velha")
 *         .physicalDefense(19).magicDefense(13)
 *         .skillDifficulty(SkillType.ATAQUE_CORPO_A_CORPO, SkillDifficulty.of(DifficultyLevel.HARD, 3))
 *         .build()
 *         .spawn(gm);
 * }</pre>
 *
 * <p>A creature whose numbers depend on <b>who summoned it</b> takes a third path:
 *
 * <pre>{@code
 * // An invocação — half its stat block reads "se você possuir N Graduações em Domínio do Mana".
 * MonsterSheet raised = Zumbi.builder().build().spawn(necromancer, gm);   // a real Conjurador
 * MonsterSheet fromGm = Zumbi.builder().build().spawn(7, gm);             // a number off a form
 * MonsterSheet loose  = Zumbi.builder().build().spawn(gm);              // nobody summoned it
 * }</pre>
 *
 * <p>See {@link org.aventyrs.core.monster.SummonedMonsterTemplate} for why the parameter is a
 * plain {@code int} rather than a summoner entity, and {@link org.aventyrs.core.monster.summon}
 * for the catalog.
 *
 * <p>{@code spawn()} produces a fully independent foe every call — its own identity, its own
 * resource pools, its own {@code SkillGraduation} instances (which are mutable, so sharing them
 * would let one monster's growth raise another's).
 *
 * <h2>The authored numbers: two Defesas and a GD per Perícia</h2>
 *
 * A foe never rolls, so it contributes fixed values in both directions of an exchange:
 * {@code physicalDefense}/{@code magicDefense} are what a player's Ataque roll must beat (see
 * {@code org.aventyrs.core.combat.AttackDelivery}), and a {@link
 * org.aventyrs.core.monster.SkillDifficulty} — a tier plus a flat bonus — is what it presents on
 * every Perícia it uses in place of a roll. {@code getSkillDifficulties()} holds one per Perícia
 * it knows, and {@code getGeneralDifficulty()} answers for everything else, so {@code
 * getSkillDifficulty(SkillType)} never comes back empty. Its attacks present {@code
 * getSkillDifficulty(ATAQUE_CORPO_A_CORPO)} (or {@code ATAQUE_A_DISTANCIA}) to a player's Esquiva
 * e Aparar roll (see {@code org.aventyrs.core.combat.AttackReceiver}); its Atenção GD, flattened,
 * is {@code getPerception()}, what a hider's concealment is compared against (see {@code
 * org.aventyrs.core.character.services.HidingService}); and its Furtividade GD is how well it
 * hides. <i>(Before 0.0.54 a foe had a single attack GD and a separate flat perception; the
 * former is now the general GD.)</i>
 *
 * <p>On a rules-built foe they're <b>derived, live</b>, by {@link
 * org.aventyrs.core.monster.MonsterRules}; on a fixed stat block they're authored and never
 * recomputed. {@code MonsterSheet} answers both through the same getters.
 *
 * <h2>Beyond those numbers: what else a fixed stat block may author</h2>
 *
 * Four further hooks on {@link org.aventyrs.core.monster.MonsterTemplate}, all defaulted, so a
 * foe that says nothing about them behaves exactly as before:
 *
 * <ul>
 *   <li>{@code getActionPoints()} — "Possuem 2 Pontos de Ação (PA)". Authored for the same reason
 *   the Defesas are; {@code ActionPointsService} still layers its modifier scan and the {@code
 *   ActionProfile} adjustment on top.</li>
 *   <li>{@code getSkillSpecializations()} — the bracketed tag beside a Perícia, e.g. {@code
 *   Ataque Corpo-a-Corpo [Primal]}.</li>
 *   <li>{@code isUndead()} — deliberately narrow, and the only vitality classification this core
 *   has. See its own javadoc for what it does and does not claim.</li>
 *   <li>{@code getCriticalEffectImmunities()} — an Anatomia clause naming Efeitos Críticos the
 *   creature shrugs off, enforced by {@code CriticalEffect#applicableTo} in <i>both</i>
 *   directions of an exchange.</li>
 * </ul>
 *
 * <h2>Race</h2>
 *
 * Every foe carries the single {@code org.aventyrs.core.race.Monstruoso} race, which is
 * deliberately empty. A player race is heritage and carries innate traits; a monster's traits come
 * from its Modelos and Habilidades Monstruosas (or, on a fixed stat block, its own ability lists),
 * so there's no per-family race to invent. They land on {@code Character} and are scanned
 * identically.
 */
package org.aventyrs.core.monster;
