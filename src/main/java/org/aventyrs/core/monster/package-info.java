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
 * Two paths, mirroring the {@code Item}/{@code AbstractItem}/{@code ArmorItem} split:
 *
 * <pre>{@code
 * // Fill in the form — a unique foe worth designing.
 * MonsterSheet troll = AbstractMonsterTemplate.builder()
 *         .name("Troll da Ponte Velha")
 *         .attributeBase(AttributeDomain.VIGOR, 9)      // past MAX_ATTRIBUTE_BASE, deliberately
 *         .skillGraduation(SkillType.ATAQUE_CORPO_A_CORPO, 12)
 *         .sizeCategory(SizeCategory.PLUS_TWO)
 *         .physicalDefense(19).magicDefense(13)
 *         .attackDifficulty(DifficultyLevel.HARD).attackBonus(3)
 *         .lifeMultiplier(7)                            // bulk without inflating Vigor
 *         .build()
 *         .spawn();
 *
 * // Or grab a generic one on-scene.
 * MonsterSheet thug = GenericMonster.CAPANGA.spawn();
 * }</pre>
 *
 * <p>{@code spawn()} produces a fully independent foe every call — its own identity, its own
 * resource pools, its own {@code SkillGraduation} instances (which are mutable, so sharing them
 * would let one monster's growth raise another's).
 *
 * <h2>The four authored numbers</h2>
 *
 * A foe never rolls, so it contributes fixed values in both directions of an exchange:
 * {@code physicalDefense}/{@code magicDefense} are what a player's Ataque roll must beat (see
 * {@code org.aventyrs.core.combat.AttackDelivery}), and {@code attackDifficulty}/{@code
 * attackBonus} are what its own attacks present to a player's Esquiva e Aparar roll (see
 * {@code org.aventyrs.core.combat.AttackReceiver}).
 *
 * <p>They're <b>authored on the stat block, not derived</b> from the foe's Perícias. That keeps a
 * stat block readable and tunable by hand; the cost is that nothing checks the numbers against
 * the Attributes behind them, deliberately.
 *
 * <h2>Race</h2>
 *
 * Every foe carries the single {@code org.aventyrs.core.race.Monstruoso} race, which is
 * deliberately empty. A player race is heritage and carries innate traits; a monster's traits are
 * authored per stat block, so there's no per-family race to invent. Supply them through the
 * template's own ability lists — they land on {@code Character} and are scanned identically.
 */
package org.aventyrs.core.monster;
