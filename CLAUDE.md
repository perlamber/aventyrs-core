# Aventyrs Core — Project Conventions

Rules-engine core for the Aventyrs tabletop game. Pure Java library (Lombok + JUnit 5 + Gradle),
no framework dependencies — see `org.aventyrs.core.skill.attention.Attention`/
`org.aventyrs.core.skill.artes.Artes` and their `Interaction`s for the reference
implementation of everything below. Every Perícia's classes live together under their own
subpackage of `org.aventyrs.core.skill` (e.g. `org.aventyrs.core.skill.artes` holds every
`Artes*` class) — only the shared, cross-skill machinery (`AbstractSkillInteraction`,
`Skill`, `SkillType`, `SkillCompetencyAbility`, `SkillExcellency`, `SkillRoll`,
`DifficultyLevel`, etc.) stays directly in `org.aventyrs.core.skill` itself.

## Recurring conventions

These hold across every section below; they aren't repeated per-feature.

- **Build for the second real consumer, not the first hypothetical one.** Don't generalize a
  shape, widen a shared interface, or add a mechanism speculatively — wait until a second real
  case needs the identical shape. A method stays on its own concrete class until then (see
  `ArtesAprimorarComArteAbility#getCriticalMarginReduction`, still parameterized by a
  dynamically-chosen Perícia and consumed by nothing). Promotion is earned, not assumed:
  `resolveCriticalMarginIncrease` did earn it, and now sits on all three of `EgoAdvantage`/
  `AttributeAbility`/`SkillCompetencyAbility`. Conversely, several pieces here *were* built
  ahead of a consumer on purpose (`ReactionsService`, `InitiativeService`, `AcquiredChoice`,
  `CharacterSheet#startTurn`); that's noted where it applies.
- **Cascading overloads.** When a computation grows a new optional input, add a longer overload
  and have every shorter one delegate down with `null`; the longest holds all the real logic. A
  subclass overrides the **longest** overload it needs, never a shorter one — virtual dispatch
  still routes the short forms to it. Used by `AbstractSkillInteraction#applyTo`
  (`CombatantSheet` → `+SceneContext` → `+SkillRoll` → `+attackTarget` → `+AttackSource`, the
  last holding all the logic), `DamageService`, `DamageInteraction`, `SceneContext`, and `Scene
  #addParticipant`. Two deliberate non-cascades to know: `ActionPointsService`'s `Character`/
  `CombatantSheet` pair and `DamageBaseService`'s `Weapon`/`SkillType` pair are *different
  questions*, not optional inputs, and don't delegate.
- **"Can't apply it yet" doesn't mean "can't compute it yet."** A formula is real, tested data
  even when the stat it feeds is entirely missing — TODO the *application*, not the
  *arithmetic*, and say which is which. This extends to *granting*: a `Blessing` typed to a
  `ModifierType` no one reads yet is still granted for real. It does **not** extend to a
  mechanism with no entry point at all (RA has no `TemporaryBonus` grant path, so an
  RA-granting clause genuinely can't be expressed).
- **The three-source scan.** Character-level stats aggregated from abilities sum
  `@Modifier`/`ModifierType` bonuses across `attributeAbilities`, `skillCompetencyAbilities`,
  and — per trained Perícia — that Perícia's unlocked `SkillExcellency` tiers. Prefer
  `SkillCompetencyAbility.allFor` so racial abilities are included (`ReactionsService`/
  `InitiativeService` predate that fix and still scan only the acquired list).
  `resolveInitiativeBlessings` is the one deliberate three-source-minus-excellencies exception.
- **A no-arg `@Modifier` method can't see context.** `ModifierResolver.invoke` always calls
  with zero arguments and `@Modifier`'s `ModifierType` is a compile-time-fixed annotation
  value. Anything conditioned on a `SceneContext`, an attack target, the rolled `SkillType`, or
  the holder's own live state must be a `default resolve*` method on the ability interface
  instead. Items are data (`ItemBonus`) for the same reason — don't route them through
  `ModifierResolver`.
- **Possession is validated; eligibility mostly isn't.** This core refuses a roll naming a
  trait the character never acquired, but doesn't check that a trait's own mechanic is
  implemented or that an acquisition was legal. "Requer N Graduações"-style clauses stay
  unenforced comments — the only two exceptions are `AventyrTitleAbility` and `Feat`
  prerequisites.
- **Builder-bypassable invariants.** Caps and prerequisites are enforced only on the service
  entry point that applies them; `Character.builder()`, Fixture Factory templates, and plain
  mutators bypass them by design, and tests routinely rely on that.
- **TODO discipline.** State what the trait is supposed to do and which *specific* missing
  system blocks it — cite from the catalog below rather than a blanket "unimplemented," and
  one line per genuinely separate missing piece. When a mechanism gets built, don't assume it
  finished every trait citing it; check each constant's own TODO. When rules text for a skill
  is revised, fix anything elsewhere citing it as a precedent in the same change.
- **This core never rolls dice, never does geometry, and never tracks what a roll is *for*.**
  Dice results, distances, and initiative values all arrive already resolved from a caller. A
  bonus scoped to a narrative *purpose* ("only for animal-related rolls") can't be modeled —
  document the simplification on the constant rather than silently narrowing or over-granting.
  A scope of specific *named* skills is trackable, and does have a hook. So is a scope of *what
  the attack was made with* — `Weapon` and `Spell` are both `AttackSource`s and reach a roll;
  don't file a new clause under this restriction without checking which of the three it is.

## Missing systems — the gap catalog

Check here before assuming a TODO needs a new gap named. Nothing below exists in this core.

| Missing system | Notes / where cited |
| --- | --- |
| **Defesas — *mostly built*** | `DefenseService` + `DefenseType` are real, and `DEFESAS`/`PHYSICAL_DEFENSE`/`MAGIC_DEFENSE` all have readers. What's still missing is narrower: `Santo#getDefesasBonus` has no granting trigger (*when* each adjacent ally receives it), and a foe's Defesa is an authored flat number with no defined conversion from a GD reduction's *níveis*. Don't cite this as "no Defesas stat exists".
| **Owned/produced item copy** | The `Item` *catalog* is real, and so is inventory now — `Character#equipment` (worn/wielded, scanned by `DefenseService`/`DamageService`) and `AbstractCombatantSheet#inventory` (carried, including a foe's loot). Still missing: **per-copy state** (Dureza remaining, Obra-Prima tier, Aprimoramentos, who produced it), a PE economy, and production/repair. Cite the specific piece, not a blanket "no Item entity" or "no inventory". |
| **Classifying an attack as Desarmado/Arma Natural** | A roll can now say *what* it was made with — `AttackSource` reaches `applyTo` as its 5th parameter — but that does **not** close this. `AttackSource` is implemented only by `Weapon`/`Spell`, so an Ataque Desarmado is just a `null`, indistinguishable from a caller who didn't say; and nothing marks a weapon as an *Arma Natural* either. So `ArtesMarciaisFeat#ARTISTA_MARCIAL`'s Dano Base grant still applies to every attack its holder makes, and gating it on `attackSource == null` would both over-apply (an unspecified attack) and silently drop the Armas Naturais half. Two markers missing, not one. |
| **Damage-type-scoped mitigation, and damage-type immunity** | `DamageType` has no Corte/Perfuração/Impacto breakdown (nor Profano/Natural/Esmagamento), and RD/RA are resolved with no notion of damage type — the one exception is `AttributeAbility#resolveDamageReduction`, unreachable from a `SkillCompetencyAbility`. *Nullifying* a damage type outright is a further missing stage: there is no immunity mechanism of any kind. Cited by `Zumbi` (imune a Profanos/Naturais, -3 vs Esmagamento). |
| **Multiplicative stages** | `MovementService` sums `MOVEMENT` additively with no halving stage (unlike `DamageService`'s real `HALF_DAMAGE`). Don't add a `MOVEMENT_HALVED` constant — the mechanism is missing, not just a reader. |
| **Temporary PA/Reação/Ação Livre grants — *built*** | Closed. The `CombatantSheet`-taking overloads of `ActionPointsService#getMaxActionPoints`/`ReactionsService#getTotalReactions`/`FreeActionsService#getTotalFreeActions` read `getTemporaryBonus(ACTION_POINTS/REACTIONS/FREE_ACTIONS)` for real. The `Character`-only overloads still can't — no sheet to ask — so cite *that* if a caller only holds a `Character`, not "the mechanism is missing". |
| **Temporary RA grants** | `getTotalAbsoluteDamageReduction` never reads `getTemporaryBonus(ABSOLUTE_DAMAGE_REDUCTION)` — RA comes only from continuously-scanned passive hooks. |
| **Round-scoped Attribute bonuses** | `AttributeValue` has only `base`/`racialBonus`/`variable`, all permanent — never summed via `ModifierType`. |
| **Roll-resolution engine — *partly built*** | The Margem Crítica half is real: `SkillRoll#getCriticalResult(int)` takes a widening margin, and `AbstractSkillInteraction#sumCriticalMarginIncrease` feeds it the sum of `resolveCriticalMarginIncrease` across `EgoAdvantage`/`AttributeAbility`/`SkillCompetencyAbility`. Still missing: a GD threshold/margin comparison, a hook for auto-success effects, and any consumer for the differently-shaped `ArtesAprimorarComArteAbility#getCriticalMarginReduction`. |
| **Area de Efeito — *described, not resolved*** | The footprint is real data: `scene.AreaOfEffect` (an `AreaShape` — CIRCULO/LINHA/CONE — plus one length in UD), reachable from `Spell#getTargeting()`. Three things are still missing, so cite the specific one: (a) **footprint resolution** — nothing turns an area into a set of hexes or targets; a LINHA/CONE additionally needs a *facing*, which is chosen per cast, not authored on the Magia, so this belongs in `scene.grid` taking the aim as arguments; (b) **no classification of an incoming attack as an area one** — `AttackDelivery`/`AttackReceiver` carry no such flag, which is what still blocks `EsquivaEApararCompetencyAbility.EVASAO` and `AbencoadoPelaLuzAbility`; (c) **caster exclusion** — "a Conjurador is never damaged by their own Magia" is a universal rule, so it is deliberately *not* a column anywhere (no `excludesCaster` flag); it belongs to the missing targeting resolution, and `Spell` has no damage column to test against anyway. |
| **Malefício classification** | No Encantamento/Maldição/Doença tag exists — see `Withering`, `ABRIR_DEFESAS`. |
| **Living/undead classification** | No vitality tag on `Character`. `CreatureType` has only HUMANOIDE/FEERICO/MONSTRUOSO, none of which is about being alive. `MonsterTemplate#isUndead()` is a deliberately narrow stand-in — exact for every combatant this core can build, wrong the day a player character can be undead or a construct must count as non-living. |
| **A summon acting on its summoner's roll** | `SummonedMonsterTemplate` builds a creature a Conjurador raised, but nothing models the player then *rolling for it*. `AttackDelivery` assumes the roller is the attacker and `AttackReceiver` that they're the defender; neither has a notion of rolling on a third combatant's behalf. This is why `CriticalEffect#applicableTo` is shared between them. |
| **Fadiga/asfixia, and healing inversion** | Nothing tracks sleep or breathing, so "não precisam dormir ou respirar" has no effect to be exempt from; and `CombatantSheet#heal` has no hook to redirect a recovery into damage (`Zumbi`'s Divine-magic clause, which also needs the missing `Magia` entity). |
| **A foe's own dano roll — *half-closed*** | `DamageBase` now models exactly a "1d6+3"-shaped figure, so a stat block's "Danos de Ataques" finally has a type to live in — but `MonsterTemplate` has no column for it and `AttackDelivery`/`AttackReceiver` still assemble a `DamageInteraction` with the caller supplying the number. This core still never rolls the dice. Cite the missing *column*, not a missing concept. |
| **Forced attack targeting / interception** | No "another Character becomes the target instead" mid-resolution — see `SantoAbility.GUARDA_VIDAS`. |
| **Reactive/retaliation damage** | `DamageService` only computes damage *to* a target *from* an attacker, never the reverse. |
| **Forced movement / positioning** | Knockback, "empurrado 1UD", Reposicionar — this core never does geometry. |
| **Continuous cross-character passive grants** | Partly built: `AventyrTitleAbility#resolveAllyAbsoluteDamageReduction` scans a target's adjacent allies for outward RA grants (Santo's Bastião dos Necessitados). Still missing for Defesas (`Santo` Despertar — its bonus is on the concrete class, unreachable by a scan) and for `SkillCompetencyAbility` (`INSTINTO_DE_LUTHER`). See "Ally-facing passive grants are scanned, not granted". |
| **Movement-triggered Reações** | No movement-triggers-Reação mechanism, and no suppression of one. Cited by `POSICIONAMENTO_ESTRATEGICO` and `AS_NA_MANGA` — but note both of those grant their *movement* half for real. A clause exempting movement from Reações is currently **exempt from nothing**, so it costs nothing to omit; it becomes real the day this lands, and both constants need revisiting then. |
| **Resource-spend triggers — *built for Ego points*** | Closed for Ego: `EgoPointsService#useEgoPointsForEffect` spends and resolves the holder's `EgoAdvantage` against the completed `EgoPointSpend` in one call, which is how `DETERMINACAO_HEROICA` works for real. **A deliberate *use* and an enemy's *drain* are different call sites, not a flag** — `Primor` calls `CombatantSheet#spendEgoPoints` directly and triggers nothing, which is what stops a critical hit healing its victim. No observer mechanism was added; this codebase still has none anywhere. `AS_NA_MANGA` is real through the same hook (`resolveEgoSpendBlessings`, granting +2UD). Still missing: PV/PM/PD spends have no report or reaction path at all. |
| **One-time roll effects bought with a resource** | Spending PV/PM to modify a single roll's outcome (e.g. a GD reduction) has no transaction — see `Orc`'s Agnação Ancestral. |
| **"This one delivered attack" scoping** | A bonus scoped to the single attack delivered by activating another ability fits no per-roll `resolve*` hook, which are all generic per skill type. |
| **Within-Turn activation counter** | `CharacterSheet` tracks Round-scoped `TemporaryEffect`s, not same-Turn activation counts. |
| **Game-session tracking — *the boundary is the consumer's, the state is missing*** | The end-of-session *trigger* is deliberately outside this core: a Narrador presses a button, and the consumer calls `EgoPointsService#applySessionRecovery(Map<CombatantSheet, EgoDomain>)` — one call carrying the table's per-player choices. `MOTIVACAO_DE_MOSES`/`DILETO_DE_TYKHE` are fully real through it. What's still absent is any per-session **state**: no session identity, no counter, nothing recording that a session happened. Hence recovery is deliberately **not idempotent** (double-application is the consumer's to prevent), and a clause that must *count* within a session — `ESTABILIDADE_EMOCIONAL`'s "a primeira vez em cada sessão", `MeioElfo`'s "1x por sessão" — stays unbuildable, because a manual button marks a boundary without telling this core it was crossed. |
| **Roubo de Mana / de Determinação** | Only Roubo de Vida exists (`LifeStealService`). |
| **Terreno difícil** | `TerrainType` describes a whole Scene, not a per-movement cost to ignore. |
| **Item numeric columns** | PE has no economy, Dureza no damage/repair mechanic, Conjuração no item-granted hook on either `SpellCastingService` roll. |
| **Acquisition-slot grants** | "Grants an extra acquisition slot" traits (`Elfo`' Origem Mística, `Anao`' Pequenos Gigantes) have no shape. |
| **Fractional Talento costs** | `getNewFeatCost` returns `int`, so a 2.5-XP discount can't be represented (`Gigantes`' whole-number 2 can). |

## Attribute `base`/Perícia Graduação: hard caps, and both upgrades cost XP

An Attribute's `base` is capped at 5; a Perícia's Graduação is capped at twice the `base` of
whichever Attribute currently governs it. Raising either one point costs XP, at a different
formula per case, and can only happen with a `CharacterSheet` in hand (that's where
`unUsedExperience` lives) — this is a Character-*progression* action, not a pure computation
on a bare value.

- `AttributeValue.getTotal()` sums three independent components (`base`, `racialBonus`,
  `variable`), but only `base` is what a character invests in directly, and it's capped at
  `CharacterAttributeService.MAX_ATTRIBUTE_BASE` (5). `CharacterAttributeServiceImpl
  .upgradeBase(AttributeValue currentValue, CharacterSheet characterSheet)` raises it by
  exactly one point: checks the cap, spends `getUpgradeCost(currentValue)` XP from
  `characterSheet` (throwing `IllegalOperationException` on either failure), then returns the
  upgraded `AttributeValue`. Cost is the target base + 1 (e.g. 4→5 costs 6).
- A Perícia's Graduação is capped at `SkillGraduationService
  .GRADUATION_TO_ATTRIBUTE_BASE_MULTIPLIER` (2) times the `base` — not the full total,
  `racialBonus`/`variable` don't widen this — of whichever Attribute currently *governs* that
  Perícia for this character. "Governs" isn't always the Perícia's own fixed
  `AttributeDomain`: if the character holds a `SkillCompetencyAbility` for that same skill
  granting an unconditional substitution (see the "Unconditional Perícia base-Attribute
  substitution" section below), the substituted Attribute's `base` is what the cap uses
  instead — `SkillGraduationServiceImpl.getMaxGraduation` resolves this via the same
  `SkillCompetencyAbility.resolveAttributeDomain` every substitution-aware
  `<Skill>Interaction` already calls for its roll, so the two never disagree on which
  Attribute is "in charge" of a given Perícia right now.
  `SkillGraduationServiceImpl.upgradeGraduation(Character character, CharacterSheet
  characterSheet, SkillType skillType)` raises it by exactly one point, same
  check-cap-then-spend-XP-then-mutate order as `upgradeBase`. Cost is **half** the target
  Graduação (e.g. 6→7 costs 3.5 — a `BigDecimal`, genuinely fractional, not rounded) — a
  different formula from Attributes, so don't share `getUpgradeCost` logic between the two
  services or assume one mirrors the other's numbers.
- Both `upgradeBase`/`upgradeGraduation` take the Character's data (`AttributeValue`/
  `CharacterSkill` in one, `Character` in the other) *and* a separate `CharacterSheet`
  parameter, mirroring `RestService.applyRest(Character, CharacterSheet, ...)`'s existing
  split: compute from the Character-side data, but only the `CharacterSheet` carries
  `unUsedExperience` to spend from (and, for Graduação, the `CharacterSkill` instance being
  mutated is looked up from `character.getSkills()`, not `characterSheet` — `Character`
  remains the single source of truth for skills/attributes/abilities). This genuinely needs
  both parameters, unlike `DamageService`'s own methods — see "Damage mitigation" below for
  why those instead take a `CharacterSheet` alone wherever one is available at all, deriving
  `Character` from it via `getCharacter()` rather than asking the caller to pass both.
- `CharacterSheet.useExperience` had a latent bug fixed alongside this: it used to subtract
  `expToUse` from `unUsedExperience` *before* checking whether the result was negative, so a
  rejected spend still silently corrupted the balance. Nothing called it before these two
  upgrade paths existed, so the bug was inert; it isn't anymore now that real callers exist —
  a rejected `upgradeBase`/`upgradeGraduation` call must leave `unUsedExperience` untouched
  (see `CharacterSheetTest#useExperienceLeavesUnusedExperienceUntouchedWhenRejected`).

**Foes are exempt from both caps by construction, not by an exception.** A `MonsterSheet` isn't
a `CharacterSheet`, and both `upgradeBase`/`upgradeGraduation` take the latter (that's where
`unUsedExperience` lives) — so a monster can't reach either entry point, and its Attribute bases
and Graduações are simply whatever its stat block authored. Nothing was added to allow that; the
builder-bypassability below is what makes it work. See "Two kinds of sheet" and
`org.aventyrs.core.monster`.

Nothing stops `AttributeValue.builder().base(...)` or `CharacterSkill#increaseGraduation`
from being called directly with a value past either cap (or with no XP spent at all) though
(Lombok's builder has no such validation, `increaseGraduation` is a plain mutator, and
Fixture Factory/test code routinely builds `CharacterAttributes`/`CharacterSkill` straight
from the builder or fixture templates, bypassing both services entirely) — so when writing a
fixture or test:
- that needs an Attribute total above 5, put the excess in `variable` (representing
  spells/feats/equipment; `racialBonus` should stay small, matching
  `CharacterCreationServiceImpl`'s actual fixed-plus-chosen racial allocations), not `base`.
  `CharacterFixture.ATTRIBUTE_SUBSTITUTIONS` follows this (e.g. `base(5).variable(3)` for a
  total of 8) — don't regress it back to a raw `base(8)`.
- is specifically exercising `CharacterAttributeService`/`SkillGraduationService` (like
  `CharacterAttributeServiceTest`/`SkillGraduationServiceImplTest`), keep the Attribute
  `base`/Graduação and the `CharacterSheet`'s `unUsedExperience` consistent with both the cap
  and the real cost formula, the same way those test files do.

The many pre-existing `<Skill>InteractionTest` files that jump a skill straight to Graduação
7 or 10 (to unlock `PRODIGIO`/`LENDA`) alongside a low Attribute `base`, with no XP spent at
all, are **not** violating either the cap or the cost rule — they call `CharacterSkill
#increaseGraduation` directly, the same service-bypassing test convenience
`AttributeValue.builder().base(...)` already is for Attributes, and `SkillGraduationService`
doesn't gate that method. Both the cap and the cost only apply going forward through
`CharacterAttributeService.upgradeBase`/`SkillGraduationService.upgradeGraduation`
themselves; don't retrofit those existing Excelência-tier tests to comply with either.

## Adding a new Perícia (Skill)

See `org.aventyrs.core.skill.attention.Attention`/`org.aventyrs.core.skill.artes.Artes` and
their `Interaction`s for the reference implementation — every Perícia's classes live together
under their own subpackage of `org.aventyrs.core.skill` (e.g. `org.aventyrs.core.skill.artes`
holds every `Artes*` class), and a new one needs a `Skill` class, a `SkillType` constant (plus
matching `ModifierType`/`PeritoTeoricoAbility` entries), a `<Skill>Specialization` enum, a
`<Skill>CompetencyAbility` enum, a `<Skill>Interaction` extending `AbstractSkillInteraction`, a
`<Skill>Excellency` enum, fixture support, and tests. The `adding-a-pericia` Claude Code skill
packages the full checklist as an invokable walkthrough.

## Títulos Aventyr — `org.aventyrs.core.title`

A Título Aventyr (e.g. `Santo`) sits alongside `Race`/skills/ego advantages as a top-level
character concept, each with its own catalog of Especializações and Habilidades/Supremas.

**Use the `adding-a-title` skill** to add one, and `adding-a-title-specialization` for its
Especializações and their gated abilities. Between them those two carry the full file/test
checklist, the subpackage and `List<AventyrTitleAbility>` typing rules, the
active-vs-passive classification (`isPassive()` is derived, not stored), `getInteractionClass()`,
and both Interaction shapes. Only the rationale they defer back to lives here:

- **Catalog vs. instance**: `AventyrTitle` is the per-character *held instance* (same shape as
  `MoralHerdadaAbility`/`ArtesAprimorarComArteAbility`), **not** a stateless-per-family class
  like `Race` — held specializations/abilities are genuinely per-acquisition data. "Which
  Título family this is" is answered by which concrete class implements `AventyrTitle`,
  deliberately not a separate identity enum.
- **Three slots, not a list**: `Character` holds plain nullable `primaryTitle`/`secondaryTitle`/
  `tertiaryTitle` fields, keyed by `TitleSlot` (`org.aventyrs.core.character`, mirroring
  `EgoDomain`'s placement). **Whether an instance is the holder's Título Primário is not a
  method on `AventyrTitle`** — it's a fact about *which slot* holds it, so at most one can ever
  be primary by construction; a caller resolves `Character#getPrimaryTitle() == title`
  externally and passes it in where needed. `Character#grantTitle(AventyrTitle, TitleSlot)` sets
  the field directly, overwriting that slot — acquiring a Título costs no XP and needs no
  `CharacterSheet`, so unlike `upgradeBase`/`upgradeGraduation` there's no service to route
  through. `Character#getAllTitles()` is the derived list (Primário first, empty slots omitted)
  a scanning service uses. `CharacterFixture` sets all three to `null`.
- **"Requer N Especializações/Habilidades" prerequisites are real, enforced data** — one of
  only two exceptions in this codebase to the usual "leave prerequisites as an unenforced
  comment" restraint (the other is `Feat`), enforced by `TitleAbilityService#grantTitleAbility`
  (`TITLE_ABILITY_PREREQUISITE_NOT_MET`). The Suprema-per-combination cap is softer:
  `getAvailableSupremaSlots` reports how many more a Título may receive, and `grantTitleAbility`
  enforces it on that one entry point, but constructing an `AventyrTitle` directly with more is
  still unchecked.

**Keep `org.aventyrs.core.title/package-info.java` current** whenever the granting API changes
shape — same discipline as `character.services`' own package-info.

## Adding a new Talento (Feat) — `org.aventyrs.core.feat`

A Talento (e.g. `ArtesMarciaisFeat.ARTISTA_MARCIAL`) is a flat catalog entry, one enum per
Talento *tree* named by `FeatCategory` (e.g. `ArtesMarciaisFeat` for `FeatCategory
.ARTE_MARCIAL`) — mirroring `<Skill>CompetencyAbility`'s one-enum-per-domain shape, not
`AventyrTitle`'s per-instance-class one, since a Talento (unlike a Título) never carries
per-acquisition player choices of its own today. `Feat`/`FeatRequirements`/`AbstractFeat`/
`FeatCategory` plus `FeatService#grantFeat(Character, CharacterSheet, Feat)` (validate
`Feat#isEligible`, spend `Race#getNewFeatCost` XP — `Race.BASE_NEW_FEAT_COST` is 3 — then
mutate, the same shape as `CharacterAttributeService#upgradeBase`/
`TitleAbilityService#grantTitleAbility`) are the whole mechanism. **Use the `adding-a-feat`
skill**, which carries the checklist, the enum-wrapping shape, and the mutable-`feats` fixture
trap, and the **`testing-a-feat` skill** to test one.

**A Talento is tested by what it does to a character who legally acquired it**, never by calling
its hook and asserting the return value. The character must satisfy that Talento's own
`FeatRequirements` and acquire it through `FeatService#grantFeat` — `Character#grantFeat`
validates nothing, so a test using it would still pass for a Talento no character could ever
reach — and the assertion reads the consuming service before and after (`DamageBaseService`,
`SpellService`, `DefenseService`, `MagicPointsService`, `RestService`), not the hook.
`ArtesMarciaisFeatTest`/`MetamagicoFeatIntegrationTest` are the reference; `MetamagicoFeatTest`
is the second layer, where a formula's own edge cases (rounding, zero floors) still belong. A
Race that overrides `Race#getNewFeatCost` gets its own catalog-driven `<Race>FeatCostTest` —
`GigantesFeatCostTest` today, and `Gigantes` is still the only Race with a discount.

**`Feat` is `sealed`, and that is what makes the catalog enumerable.** `permits
ArtesMarciaisFeat, MetamagicoFeat, AbstractFeat`, so `FeatCatalog` discovers every authored
Talento via `Feat.class.getPermittedSubclasses()` — reflection with no classpath scan, no I/O,
and no silent partial results. A classpath walk was rejected deliberately: it has to special-case
exploded dirs vs JAR entries vs the module path, and when it guesses wrong a whole Talento tree
just quietly stops being offered. The permits clause is **compiler-enforced**, so the same
mistake is a build failure instead.

- **`AbstractFeat` is `non-sealed` with a *public* all-args constructor** — the extension point
  that keeps sealing from closing this library off. A consumer's homebrew Talento is
  `new AbstractFeat(category, description, requirements) { @Override ... }`, a first-class
  `Feat` everywhere, that simply never appears in `FeatCatalog` — correct, since the catalog is
  the authored ruleset, not every `Feat` constructible. **Don't implement `Feat` directly** (the
  compiler now refuses it); two tests were converted to `AbstractFeat` when this landed.
- **Discovery lives in `FeatCatalog`, never as a `static` field on `Feat`.** `Feat` declares
  `default` methods, so initialising `MetamagicoFeat` initialises `Feat` first — a static
  initialiser on `Feat` calling `getEnumConstants()` back on that enum would observe it
  mid-initialisation and could read its constants as `null`. A separate class breaks the cycle.
- `FeatService#getAvailableFeats(Character)` (prerequisites met, not already held) and
  `#getAffordableFeats(Character, CharacterSheet)` (that list, narrowed by the XP wallet) are
  **deliberately separate methods, not a flag**: "am I allowed this?" and "can I afford it?"
  differ over time and a UI wants both. Cost is per-Race, so two characters with identical
  Talentos and identical XP can get different answers.

**`Feat` also carries four `default resolve*` hooks**, not one — `resolveDamageBaseIncrease`,
plus `resolveBranchLevelIncrease` (the Árvore de Magia cap), `resolveDefenseBonus(DefenseType,
Character)` and `resolveManaMultiplierIncrease`/`resolveRestMagicPointsBonus(RestType,
Character)`. The skill's "keep it on the tree enum until a second consumer earns the interface"
rule still holds for a formula only that tree reads — but **a hook a *service* must scan for has
to be on `Feat` itself**, since `character.getFeats()` is a `List<Feat>` and a service can see
nothing narrower. That is what promoted these four. Feats are deliberately **not** part of any
`ModifierResolver` `@Modifier` scan (nothing scans them reflectively), so each consuming service
gives them an explicit pass, the way `DefenseServiceImpl` already does for equipment.

`MetamagicoFeat` is the second tree, and worth reading before adding a third — it's the first to
need a **sibling constant as a prerequisite**, which Java's forward-reference rule forbids in
constructor arguments even when qualified. Two things fall out: `featRequirements` is held as a
`Supplier<FeatRequirements>` rather than a plain field, and **the constants are ordered so every
prerequisite is declared before its dependents** (`MetamagicoFeatTest` pins that ordering, so a
future insertion can't silently break it).

One fact worth knowing outside that checklist: **a Talento's prerequisites are real, enforced
data** — the second exception (alongside `AventyrTitleAbility`'s "Requer N Especializações/
Habilidades") to this codebase's usual "leave 'Requer N Graduações' as an unenforced comment"
restraint, because a Talento's own Pré-requisito is always a simple numeric/identity threshold
`FeatRequirements` can model directly.

## Itens/Equipamento — `org.aventyrs.core.item`

An `Item` is the **catalog entry** for a piece of Equipamento — what "an Armadura Completa" is,
the same way `Feat` describes a Talento — carrying every column an item's rules-text block
lists (`ItemWeightClass`/`ItemRarity`, `description`, `price` in PE, `physicalDefenseBonus`/
`magicDefenseBonus`, `hardness`, `castingBonus`) plus an `ItemFavor` for its conditional half.
**Use the `adding-an-item` skill** to add one — it carries the column-to-field mapping, the
`ItemFavor`/`ItemBonus`/`ItemRequirements` shapes, the one-enum-per-`ItemCategory` layout, the
`Skill.DISADVANTAGE_MALUS` convention, and the test checklist. The architecture:

- **Catalog, not owned copy** — the same split `AventyrTitle`'s javadoc documents, resolved the
  *other* way than a Título's: an item's stats are identical for every copy, so the enum
  constant *is* the item. Per-copy state (Dureza actually remaining, Obra-Prima tier,
  Aprimoramentos, who produced it) is deliberately unmodeled, and would be a separate
  held-instance type wrapping a catalog entry. Don't build it speculatively; several TODOs cite
  it (`ProfissaoCompetencyAbility`, `ResourcesAdvantage#HERANCA_FAMILIAR`), but none is
  unblocked by the catalog alone. **Inventory itself is real**, though — `Character#equipment`
  (worn/wielded) and `AbstractCombatantSheet#inventory` (carried, including a foe's loot), both
  mutable `List<Item>` with plain mutators, the same shape as `Character#feats`.
- **`ItemFavor` is the conditional half, and its bonuses are real data, not prose**: it carries
  a list of `ItemBonus` (a `ModifierType` + value pair), resolved via `ItemFavor#resolveBonus
  (ModifierType, Character)` / `Item#resolveFavorBonus(...)` — 0 unless the `ItemRequirements`
  (an `AttributeDomain` + value) are met. It's **data, not `@Modifier` methods**, unlike every
  ability enum, and that's forced: `@Modifier`'s `ModifierType` is a compile-time-fixed
  annotation value, so one shared `ItemFavor` class can't vary which type a given item grants —
  the same limitation "A ModifierType per skill" documents. **Don't route items through
  `ModifierResolver`.** `ItemBonus` is deliberately not `TemporaryBonus`/`Blessing` either: an
  item's Favor lasts as long as the item is carried and never reaches anyone but its wielder, so
  a countdown, a `TargetScope` and a granting `source` would all be dead weight.
- **`ItemRequirements` checks `getTotal()`, not `getBase()`** — deliberately unlike
  `FeatRequirements`, which uses `base`: acquiring a Talento is gated on what the character
  personally invested in, but whether an item's Favor applies is a "can I meet this right now"
  question, so a Bônus Racial or a variable bonus counts. It's a narrower record than
  `FeatRequirements` (no `requiredSkillType`/`requiredFeat`) rather than a reuse of it — widen
  it only if a real item ever names a Perícia/Talento/Título.
- **Two columns reach a real consumer, three don't.** The Favor's `DAMAGE_REDUCTION` is scanned
  by `DamageServiceImpl` over `character.getEquipment()`, and DF/DM by
  `DefenseServiceImpl.sumEquipment` — a new item's values flow into both with no wiring. **Preço,
  Dureza and Conjuração still have no consumer**, each blocked on a different missing system (no
  PE economy, no damage/repair mechanic, no item-granted hook on either `SpellCastingService`
  roll). Their values are real, exact data all the same, per the "can't apply it yet doesn't mean
  can't compute it yet" discipline.
- **Dano Base is on `Weapon`, not on `Item`** — `Weapon extends Item` adds exactly two abstract
  columns, `getDamageBase()` and `getSkillType()` (the Perícia it's swung with, which is what
  `DamageBaseService` scans by), and `AbstractWeapon extends AbstractItem` is its builder-built form
  (both use `@SuperBuilder` so the subclass inherits the ten `AbstractItem` columns rather than
  restating them; `AbstractItem.builder()` is unaffected). Every other weapon property — Preço,
  Dureza, Raridade, `ItemFavor`, even DF/DM — is an ordinary `Item` one and needs no override.
  **Don't put a weapon-only column on `Item` with a harmless-looking default**: that's what this
  interface replaced, and a defaulted `UNARMED` made "a helmet" and "a real dagger" answer
  identically. `DamageBaseService` takes a `Weapon`, so the compiler refuses a pauldron — no
  `isWeapon()` flag and no runtime guard, the same enforcement-by-type as `CharacterSheet` vs
  `MonsterSheet`. Nothing checks that a `Weapon`'s `ItemCategory` is actually `OFFENSIVE`, per the
  usual builders-aren't-gatekeepers restraint.
- A Favor clause with no `ModifierType` to express it contributes no `ItemBonus` and lives on in
  `getDescription()` until its mechanism exists — either because no reader for the concept
  exists (`ARMADURA_COMPLETA`'s "de Corte" scoping, modeled as plain RD) or because `ItemBonus`
  can't hold the *shape* even though the stat has a `ModifierType` (`ARMADURA_DE_JUSTA`'s
  halving; **don't add a `MOVEMENT_HALVED` constant** — the missing piece is the multiplicative
  mechanism, not a reader). But check the *net effect* before assuming a split is needed:
  `ROUPA_PESADA`'s two clauses read like they need DF and DM separately, yet always net out to
  an unconditional +1 to both, so it's granted for real as one combined `DEFESAS` bonus of 2.
- `ItemInteraction` is untouched by this — still the bare pre-existing "TODO implement" stub,
  since nothing yet *uses* an item as an `Interaction`.

## Requesting a specific Habilidade de Competência or Especialização on a roll — `SkillRoll#getRequestedAbility`

Some rolls aren't a plain Perícia test — the caller is specifically invoking one of the
character's `SkillCompetencyAbility` maneuvers (e.g. "roll Furtividade using Disparo
Ricochete") or a held Especialização (e.g. "roll Atenção as an Investigar check"), and this
core should refuse the roll outright if the character never actually acquired that trait,
rather than silently computing a result for one they can't use.

- `SkillTrait` (`org.aventyrs.core.skill`) is the shared interface behind both:
  `SkillType getSkillType(); String getDescription();`. `SkillCompetencyAbility extends
  SkillTrait` (plus its own ability-specific default methods —
  `getDifficultyReduction()`/`getSubstituteAttributeDomain()`/etc., unaffected by this).
  `SkillSpecialization extends SkillTrait` too, with nothing added on top — every
  `<Skill>Specialization` enum constant implements it. Neither interface is sealed; only these
  two implementations exist today, but nothing requires that going forward.
- `SkillRoll` carries an optional `requestedAbility` (a `SkillTrait` — either a
  `SkillCompetencyAbility` or a `SkillSpecialization`), set via its second constructor overload
  — `SkillRoll(dice)` still delegates to `SkillRoll(dice, null)`, so every existing call site (a
  plain roll) is unaffected. `null` means "just a plain roll," and skips validation entirely —
  this is the common case.
- `AbstractSkillInteraction`'s 3-arg `applyTo` validates a non-null `requestedAbility` via
  `validateRequestedTrait` before doing anything else: it must belong to this same `skillType`
  (`requestedTrait.getSkillType() == skillType`) *and* actually be held — for a
  `SkillCompetencyAbility` that means present in `character.getSkillCompetencyAbilities()` or
  `character.getRace().getRacialAbilities()` (unchanged from before); for a
  `SkillSpecialization` that means present in `characterSkill.getSpecializations()` instead
  (`characterSkill` is resolved via `findCharacterSkill` before this check runs, so an untrained
  skill's fallback `CharacterSkill` — with no specializations — correctly fails this check).
  Either failure throws `IllegalOperationException` (`REQUIRED_SKILL_TRAIT_NOT_HELD`). This
  applies generically to every skill, in one place, the same way every other `applyTo`
  computation already does.
- When the validated `requestedAbility` is a `SkillSpecialization`, `applyTo` resolves
  `InteractionResult#reachedDifficultyLevel` via `DifficultyLevel#reachedByAsExpert` instead of
  `DifficultyLevel#reachedBy` — thresholding against each tier's easier `expertValue` instead of
  its `baseValue`. This is the first real consumer of `expertValue`, which used to be dead data
  (see "Rolling the dice" below).
- This only validates *possession* of the trait, the same restraint CLAUDE.md already
  documents elsewhere (no "Requer N Graduações" enforcement, no acquisition-legality checks) —
  it doesn't validate that a `SkillCompetencyAbility`'s own mechanic is actually implemented, or
  gate on anything about the roll's circumstances (range, targets, etc.), and it doesn't
  validate that a requested `SkillSpecialization` is actually the right fit for whatever the
  roll is being used for narratively — this core still doesn't track what a roll is *for*. A
  caller can still request a trait whose effect is entirely TODO'd (an ability) or narratively
  mismatched (a specialization); the roll proceeds, it's just no longer possible to request a
  trait the character was never granted in the first place.

## Dispatching a roll by SkillType — `SkillInteractionFactory`, `SkillRollRequest`

A caller that only has a `SkillType` in hand (e.g. an API layer deserializing an incoming roll
request) previously had no way to reach the right concrete `<Skill>Interaction` without its
own hand-written switch over all of them. `SkillType` already carries every other per-skill
mapping this core needs (see the "Adding a new Perícia" section/`adding-a-pericia` skill), so this reuses it rather than
introducing a second, parallel "which skill is this" enum that could drift out of sync with
`SkillType` itself:

- `SkillType.newInteraction()` returns a fresh `<Skill>Interaction` via a stored
  `Supplier<AbstractSkillInteraction>` per constant, built through that Interaction's default
  (no-arg) constructor — the same one every concrete subclass already exposes.
- `SkillInteractionFactory` (`org.aventyrs.core.skill`) is the single call site for this:
  `create(SkillType)` just delegates to `newInteraction()`, and `resolve(SkillRollRequest)`
  looks up the right Interaction *and* calls its 3-arg `applyTo` in one step.
- `SkillRollRequest` (`@Builder`) is the wrapper bundling everything one roll needs:
  `skillType`/`target` (`@NonNull`, required) plus `sceneContext`/`skillRoll` (both optional,
  `null` same as every `applyTo` overload already accepts). A caller builds one of these once
  instead of separately resolving which `<Skill>Interaction` class to instantiate and calling
  its `applyTo` overload directly.
- This is purely a dispatch/ergonomics convenience — it doesn't change what any `applyTo`
  computes, and a caller that already knows which concrete `<Skill>Interaction` to use (most
  of this core's own tests) can keep constructing and calling it directly; nothing requires
  going through `SkillRollRequest`.

## Character-level stats aggregated from abilities (e.g. Reações, Ações Livres, Pontos de Ação, Iniciativa)

Some Character-level counters need a fixed base value *and* a fully-modified total summed
from abilities — this is what `ReactionsService`, `FreeActionsService`,
`ActionPointsServiceImpl.getMaxActionPoints` (the latter scans the same three sources to
support things like `AtletismoExcellency.LENDA`'s +1PA), and `InitiativeService` all do. Don't
compute the modified total inside `Character` itself (it would need to instantiate
`ModifierResolverImpl` directly, which doesn't belong on a data class):

- `Character` holds only the plain fixed counter (e.g. `reactions`/`freeActions`, a normal
  `@Builder.Default` field with Lombok's regular getter — no suppression, no manual method),
  defaulting to a constant declared on the **service** interface
  (`ReactionsService.DEFAULT_REACTIONS`, mirroring `ActionPointsService.DEFAULT_ACTION_POINTS`
  and `FreeActionsService.DEFAULT_FREE_ACTIONS`).
- A dedicated `<Stat>Service`/`<Stat>ServiceImpl` in `org.aventyrs.core.character.services`
  (e.g. `ReactionsService.getTotalReactions(Character)`) takes a constructor-injected
  `ModifierResolver` (default `new ModifierResolverImpl()`, same DI convention as every other
  service) and sums `@Modifier`/`ModifierType` bonuses across **three** sources:
  `character.getAttributeAbilities()`, `character.getSkillCompetencyAbilities()`, and — per
  trained Perícia in `character.getSkills()` — whichever `SkillExcellency` tiers that Perícia's
  graduation has unlocked (resolved generically via `SkillType.getExcellencyClass()` +
  `SkillExcellency.unlockedBy`, since the concrete `<Skill>Excellency` enum type isn't known at
  compile time from a bare `SkillType`). Clamp the total at 0.
- Ações Livres are mechanically identical to Reações (same 3-source aggregation, same
  `DEFAULT_*` shape) — the only difference between the two is *when* they may be spent (a
  Reação only in response to someone else's action; an Ação Livre also on the character's own
  Turn), which is a game-flow/Scene-timing concern this library doesn't enforce, not a
  computation difference. Don't build a distinct aggregation shape for a new counter just
  because it's spent under different narrative conditions — reuse this same pattern.
- Whenever a new fixed counter field is added to `Character` here, it must also be added to
  `CharacterFixture`'s `BLANK` template `Rule` (see the comment on `loadCharacterTemplates` —
  Fixture Factory never goes through the Lombok builder, so `@Builder.Default` values don't
  apply automatically).
- `InitiativeService.getTotalInitiative` is a variant of this same shape, not a byte-for-byte
  copy: its base isn't a plain `Character` counter defaulting to a `<Stat>Service` constant —
  Iniciativa already existed as one of the four `EgoDomain`s (`character.getEgos()
  .getIniciativa().getTotal()`, itself `base + variable`, see `EgoValue`), so that's the base
  the `ModifierType.INITIATIVE` sum from the usual three sources is added on top of, with no
  new `Character` field and nothing to add to `CharacterFixture`'s `BLANK` template. It also
  deliberately does **not** clamp at 0 like Reações/Ações Livres/RD/RA do — those are spendable
  resources where negative is meaningless, but Iniciativa is a turn-order value, so a large
  enough malus leaving it negative is still a valid (if late) position, not an error state.
  `InitiativeEntry`'s own javadoc documents how this fits into a `Scene`'s actual turn order:
  this service only computes the fixed Ego-plus-modifiers component, a caller still adds
  whichever dice roll they applied on top before handing the total to
  `Scene#addParticipant`, since this library never rolls dice itself.

Mirror this shape for any new stat abilities/competencies/excellencies can modify, and
remember to give the new `ModifierType` constant a `@Modifier`-annotated method on whichever
concrete ability/excellency should affect it (e.g. `AttentionExcellency.FOCADO` for
`REACTIONS`). No concrete ability grants `ModifierType.INITIATIVE` yet — `InitiativeService`
was built ahead of a first consumer, same as `ReactionsService` once was, not because building
it just unblocked one. The two existing rules-text mentions of a flat +2 Iniciativa
(`AttentionExcellency.LENDA`, `MeioElfo`'s Provar Seu Valor) are each still TODO'd on their
*own* missing system first — a graduation-threshold-crossing trigger for the former, a "game
session" concept for the latter's "1x por sessão" — so a plain `@Modifier(ModifierType
.INITIATIVE)` method on either wouldn't actually match its rules text yet; don't wire one in
just because the scanning now exists.

### What a combatant has *this Round* — the `CombatantSheet` overloads, and `ActionProfile`

The three aggregations above answer "what does this character permanently have". PA, Reações
and Ações Livres also have a *per-Round* answer, which is a different number, and each of the
three services carries a second and third overload for it:

| | permanent | per-Round |
| --- | --- | --- |
| PA | `getMaxActionPoints(Character, turn)` | `getMaxActionPoints(CombatantSheet, turn[, SceneContext])` |
| Reações | `getTotalReactions(Character)` | `getTotalReactions(CombatantSheet, turn[, SceneContext])` |
| Ações Livres | `getTotalFreeActions(Character)` | `getTotalFreeActions(CombatantSheet, turn[, SceneContext])` |

- **The `Character` overload is not a shorter form of the sheet one.** It's a genuinely
  different question with a different input, so it does *not* cascade-delegate the way
  `applyTo`'s overloads do — it has no sheet to read a `TemporaryBonus` from and (for
  Reações/Ações Livres) no turnNumber to apply a profile with, and that's deliberate, not a
  gap. Only the two *sheet* overloads cascade into each other, the 2-arg passing `null` for
  `SceneContext`. Prefer the sheet form for anything combat-facing.
- **Two things only the sheet overloads see**: `getTemporaryBonus(ACTION_POINTS/REACTIONS/
  FREE_ACTIONS)` — so a `Blessing` typed to any of the three is live, where it used to be
  inert — and, via `SceneContext#isCombatScene`, whether this is a Cena de Combate. A `null`
  `SceneContext` reads as "not a Cena de Combate", never as an error.
- **The `ActionProfile` adjustment is always applied last, and always exactly once.** Each
  `<Stat>ServiceImpl` computes an *unclamped* permanent baseline in a private helper, adds the
  sheet's `TemporaryBonus`, hands the result to the profile, and clamps at 0 once at the very
  end. That ordering is what makes a profile's *denial* clauses real rather than
  out-summable: `MOVIMENTO_PLANEJADO` returns exactly 0 Ações Livres on Turn 0 and
  `CALCULISTA` exactly 0 PA on Round 0, however many an ability or a `Blessing` granted.
  Don't clamp the baseline separately, and don't apply the profile inside a helper the other
  overload also calls.

`ActionProfile` now carries **three** parallel adjustment hooks — `adjustActionPoints`,
`adjustReactions`, `adjustFreeActions` — each in the usual cascading pair, the 2-arg form
delegating down with a `null` `SceneContext` and the 3-arg holding the logic, so a constant
body always overrides the **3-arg** one even when it ignores the context (`MOVIMENTO_PLANEJADO`
does). `adjustSkillRollCost` deliberately has **no** `SceneContext` overload — no profile
conditions a roll's cost on the Scene, so widening it would be building for a hypothetical
consumer.

Four of the six profiles are now fully real: `IMPULSIVO`/`CALCULISTA` (PA by Round),
`MOVIMENTO_PLANEJADO` (0 Ações Livres on Turn 0, +1 from Turn 1 on), `REFLEXOS_RAPIDOS` (+1
Reação every Round, *not* gated on combat) and `ESTRATEGISTA` (-1 PA / +1 Reação / +1 Ação
Livre, all three gated on `isCombatScene`). `CONSCIENCIA_DEFENSIVA` is the one still TODO'd,
and `adjustReactions` is the **wrong hook** for it, not a missing one — its clause exempts
specific movements from *provoking* Reações, which is the movement-triggers-Reação gap, and
never changes how many a character *has*.

Because it adjusts none of the three, `CONSCIENCIA_DEFENSIVA` is also what
`MonsterTemplate.DEFAULT_ACTION_PROFILE` and `CharacterFixture`'s templates now use.
**Both used to be `REFLEXOS_RAPIDOS`, chosen back when that constant was inert** — once it
started granting a Reação for real, every fixture-built Character and every foe whose stat
block is silent would have silently carried an extra one. Worth remembering when picking a
placeholder constant: "does nothing today" is not a stable property.

## Árvores de Magia — `SpellTree`, `SpellBranch`, and the three acquisition gates

A Magia's place in its Árvore is three columns on `Spell`: `getTree()`, `getBranchLevel()` (how
deep) and `getBranch()` (which ramificação, `Optional.empty()` on the **trunk**).

```
SEMENTE ── BROTO ─┬─ MUDA(A) ── EMERGENTE(A) ─┬─ FLORESCENTE
                  └─ MUDA(B) ── EMERGENTE(B) ─┘
\_____ trunk ____/ \____ ramificações _____/  \___ trunk ___/
```

- **Convergence has no mechanism — being branchless *is* it.** A Magia after the branches rejoin
  reports no `SpellBranch`, so it sits on every path and the branch gate can never refuse it.
  Don't add a "these branches converge here" structure; there's nothing for it to do.
- **Zero or two ramificações, never one.** `SpellTree.validateBranches` throws
  `INVALID_SPELL_TREE` on 1 or 3+ — a divergence into a single path is meaningless, and
  `MAGIA_ALTERNATIVA`'s "ambas as ramificações" only reads for two. It's called from the branch
  gate, so no tree reaches an acquisition decision unvalidated.
- **`SpellTree`/`SpellBranch` are interfaces**, like `AventyrTitle`/`Feat` — trees are authored
  per family, and a central catalog enum would sit empty until the first one lands. **No tree is
  authored yet**; `TestSpellTree`/`TestSpellBranch` are the test stubs, shaped exactly like
  Aliados da Natureza (diverge at MUDA, converge at FLORESCENTE).

### The three gates — `Spell#isEligible(Character, BranchLevel maxBranchLevel)`

All three must hold, the same combine-every-prerequisite shape as `Feat#isEligible` and
`AventyrTitleAbility#isEligible`. Enforced by `SpellService#grantSpell`.

| gate | rule |
| --- | --- |
| **Cap** | `maxBranchLevel` must reach the Magia's own `BranchLevel` |
| **Climb** | unless SEMENTE, a Magia of **this same tree** must be held at the *immediately* shallower rung |
| **Branch** | no Magia of this tree may be held on a *different* ramificação |

- **All three are derived, never stored.** There is no "chosen branch" field and no
  unlocked-levels counter: a Conjurador's branch in a tree simply *is* whichever ramificação
  their acquired Magias sit on. Same recompute-on-demand discipline as `HitPointsService
  #getStatus`/`InitiativeEntry#getEffectiveInitiativeValue`. Don't add a
  `Map<SpellTree, SpellBranch>` — it could only ever disagree with `getSpells()`.
- **The cap arrives already resolved**, not scanned inside `isEligible` — the same
  resolve-then-pass-in shape `DamageServiceImpl` uses for `hasLowerPvAdjacentAlly`. That keeps
  the gates a pure function and the feat scan in the service layer.
- **Only the branch gate has an exemption**: `MagiaAlternativaAbility` held for this tree's
  `MagicType`. It loosens neither the cap nor the climb — `SpellEligibilityTest` pins that.
- **A foothold in a *different* tree never counts** for the climb. That's what makes a capped
  Conjurador spend sideways — more Magias at their current depth, from other Árvores — until a
  Talento raises the cap.
- `BranchLevel` carries the ladder operations the gates ask for: `isAtLeast` (ordinal
  comparison, same as `Range#isWithin`), `previous()` (empty at SEMENTE) and `advancedBy` (the
  clamping the cap uses).

### The cap, and `MAGIA_ALTERNATIVA`

- `SpellService#getMaxBranchLevel` is `SEMENTE` advanced by the summed
  `Feat#resolveBranchLevelIncrease` across `character.getFeats()`, clamped at FLORESCENTE.
  **Talentos are the only source** — there is deliberately no `ModifierType` for this and no
  three-source `ModifierResolver` scan, so a Habilidade or an Excelência can't grant spell
  depth. `MetamagicoFeat` authors the complete ladder: `ARCANISTA` → BROTO,
  `ARCANISTA_EXPERIENTE` → MUDA, `MESTRE_ARCANISTA` → EMERGENTE, `DESAFIADOR_DA_REALIDADE` →
  FLORESCENTE, each granting **exactly one rung**. Summing one rung apiece is only correct
  *because they chain* — each names the previous as its `requiredFeat`, so they can't be acquired
  out of order or in isolation, and holding all four lands exactly on FLORESCENTE. A fifth rung
  would grant +1 like the rest; never compensate for a missing rung by granting +2 somewhere.
- `SpellService#grantSpell` **spends no XP**, deliberately — no acquisition cost has been
  specified, and inventing one would bake in a number rules text then has to override. It still
  takes a `CharacterSheet`, so adding a cost stays a one-line change. This is the service's one
  open question, flagged in its javadoc.
- `MagiaAlternativaAbility` (`org.aventyrs.core.ability`) is one `AttributeAbility` constant per
  `MagicType` — CLAUDE.md's pattern 3, mirroring `PeritoTeoricoAbility` exactly. Grant the
  constant, not `FocusAbility.MAGIA_ALTERNATIVA`, which stays the catalog/rules-text entry.
- ⚠️ **`MagicType` and the rules text disagree.** `MAGIA_ALTERNATIVA` names eight types —
  including **Temporal** and **Umbral**, which the enum lacks — and omits the `NATURAL` it has.
  `MagiaAlternativaAbility` follows the enum (one constant per existing value). Settle this
  before authoring a tree typed `NATURAL`, which could never be exempted if NATURAL isn't a real
  Tipo de Magia. Adding a `MagicType` constant means adding a matching one there.

## A Magia's reach — `SpellReach`, `SpellTargeting`, `AreaOfEffect`

`SpellReach` is only the *discriminator* (PESSOAL/TOQUE/AREA_DE_EFEITO/DISTANCIA). It can never
describe a Magia on its own — three of its four constants need a parameter an enum constant has
nowhere to put — so `Spell#getTargeting()` returns a **`SpellTargeting`** record pairing it with
exactly the parameters that reach takes, validated in the canonical constructor:

| reach | `range` | `area` |
| --- | --- | --- |
| `PESSOAL` | absent | absent |
| `TOQUE` | absent (Adjacente implied) | absent |
| `DISTANCIA` | required | absent |
| `AREA_DE_EFEITO` | *optional* — absent means centred on the Conjurador | required |

Anything else throws `IllegalOperationException`/`INVALID_SPELL_TARGETING` — the same
cross-field pairing check `DamageBonus` applies to its own type/element pair, and for the same
reason: the combination is authored data, so a meaningless one is a mistake, not a value.

- **`PESSOAL` and `AREA_DE_EFEITO` are the pair most easily confused.** `PESSOAL` is strictly
  single-target-on-self — a Magia castable nowhere else. A burst *centred* on the Conjurador is
  an `AREA_DE_EFEITO` with no `range`; it affects an area, and only its origin differs from the
  ranged case. For that constant alone, `range` answers "how far away may the centre be placed",
  not "how far can this reach a target".
- **`LINHA` and `CONE` are emanations** (`AreaShape#isEmanation`) — they radiate from the
  Conjurador, so they have no centre to place and may never carry a `range`. Only a `CIRCULO`
  can. Relaxing that is deleting one clause of `isLegalCombination`.
- **`AreaOfEffect` is one shape plus one length in UD**, and lives in `org.aventyrs.core.scene`
  beside `Range`, not in `magic` — `EsquivaEApararCompetencyAbility.EVASAO` and
  `AbencoadoPelaLuzAbility` both describe Área de Efeito effects that aren't Magias. A LINHA is
  1 UD wide and a CONE's spread derives from its length; neither is stored, since no rules text
  names a width or an angle yet.
- **`TOQUE` stores no `Range.ADJACENTE`** — the constant implies it, and storing it too would be
  authored redundancy that could disagree with itself.

What this does *not* do is in the gap catalog's "Area de Efeito" row: no footprint resolution,
no classification of an incoming attack as area-based, and no caster-exclusion flag.

## Casting a Magia is two separate rolls — `org.aventyrs.core.magic.SpellCastingService`

Casting a Magia with a rolled effect always involves **two** rolls, not one: whichever
Perícia actually delivers the spell (e.g. `AtaqueADistanciaInteraction` for a ranged spell,
`AtaqueCorpoACorpoInteraction` for a Toque spell) rolled against the **target's** GD, followed
by a `DominioDoManaInteraction` roll against the **Magia's own** GD. `SpellCastingService
.castSpell(CharacterSheet, Interaction<CharacterSheet> deliveryInteraction)` orchestrates
this: it rolls the given delivery Interaction, then rolls Domínio do Mana, and returns both
`InteractionResult`s in a `SpellCastingResult` — it never picks the delivery Interaction
itself (the caller does, since only the caller knows which Magia/weapon is being used).

`Spell` is a real entity now, and a character carries the Magias they know (`Character#spells`,
via `SpellService#grantSpell` — see "Árvores de Magia" above). What's still missing is narrower:
**no Magia is authored anywhere** (no Árvore de Magia exists yet, so there is no catalog to cast
*from*), and `SpellCastingService` still doesn't know either roll's target GD, so it computes
both rolls' bonuses without resolving success/failure for either. Cite the missing *catalog* or
the missing *GD*, not a missing `Magia` entity. Left as a TODO on the service rather than
guessed at.

This is also where an ability whose effect targets the *delivery* roll, not Domínio do Mana's
own, would eventually plug in — don't try to force it onto
`SkillCompetencyAbility.getDifficultyReduction()`/`SkillExcellency`, since that hook only ever
feeds back into that *same* skill's own Interaction, never another skill's. No current ability
needs this: `DominioDoManaCompetencyAbility.FEITICEIRO` used to be the reference example (a -1
GD on the delivery roll), but a rules revision replaced it with `Magia Selvagem` (an attribute
substitution) — and `AtaqueADistanciaExcellency.LENDA`'s "bônus de conjuração" clause, the
other ability this service was built to eventually host, was *also* dropped in its own
revision. `SpellCastingService` itself is still correct (Domínio do Mana's own rules text still
describes casting as two separate rolls), it just currently has no concrete ability wired
toward this specific extension point — a reminder that these cross-references need
re-checking whenever a cited skill gets revised, and that a piece of infrastructure can outlive
the example that originally justified building it.

## Identity, and Scene sub-groups — `org.aventyrs.core.scene.Scene#getAllies`

Both `Character` and `CharacterSheet` carry their own `UUID id`, auto-generated by default but
deliberately **not** `final` — two *separate* identities, since the same `Character` could in
principle back more than one `CharacterSheet`. This project has no persistence layer of its
own, but is meant to be loaded from one (a DTO reconstructing a previously-saved Character/
CharacterSheet needs to set the *existing* id, not mint a new one), so both need a real,
settable path for that:
- `Character.id` is `@Builder.Default protected UUID id = UUID.randomUUID();` — already
  settable normally, via `Character.builder().id(existingId)...build()` (or
  `.toBuilder().id(existingId).build()`), no extra work needed there.
- `CharacterSheet` has no builder (`@RequiredArgsConstructor(staticName="of")`, not
  `@Builder`), so `id` gets its own second factory overload instead:
  `CharacterSheet.of(Character, Player)` mints a fresh one (the common case, a genuinely new
  entity); `CharacterSheet.of(Character, Player, @NonNull UUID id)` takes an existing one
  (the reconstruction-from-a-DTO case) — it just delegates to the first overload (reusing its
  `@NonNull` validation on `character`/`player` for free) and overwrites `id` before
  returning. There's still no public setter, so `id` can only be set at construction, through
  one of those two factories — don't add a general `setId(...)` just because the field is no
  longer `final`.

Neither class overrides `equals()`/`hashCode()`; every existing
`assertEquals(List.of(sheet, ...), ...)` test relies on reference equality and still does —
`id` is meant for callers (like `Scene`) that need to recognize "this is the same
participant" without holding the exact same object reference, not for changing what
`.equals()` means on these classes. Don't add `equals()`/`hashCode()` overrides keyed on `id`
without checking every existing reference-equality-based usage first.

`Scene` groups its participants into sub-groups via `InitiativeEntry.group` (a `UUID`, no
dedicated `Group` class — nothing about a group needs data beyond an identifier). Two
`addParticipant` overloads exist: the original 2-arg one now delegates to a 3-arg one, passing
`UUID.randomUUID()` — so a participant added without an explicit group starts in a group of
one, with no default allies, and every one of the 12 pre-existing `SceneTest` methods keeps
passing unchanged. Pass the *same* `UUID` to every `CharacterSheet` that should consider each
other allies (e.g. a party, or a pack of enemies) via the 3-arg overload.

`Scene.getAllies(CharacterSheet)` returns every other participant sharing that Perícia's
group — excluding the caller itself, matching how `DOM_BARDICO`'s targeting works ("a eles,
mas não a você"), which is the ability this method exists to eventually support (still not
wired: `DOM_BARDICO` itself remains TODO'd on the GD-tier-to-bonus lookup and Rodada-scoped
duration, neither of which this method solves). It searches both `activeEntries` and
`pendingEntries` — sub-group membership isn't a turn-order concern, so a participant added
mid-Round is already an ally before joining the rotation — and throws
`IllegalOperationException` (`CHARACTER_SHEET_NOT_IN_SCENE`) if asked about a `CharacterSheet`
that was never added to this `Scene`, rather than silently returning an empty list (which
would mask a caller bug the same way a typo'd wrong `CharacterSheet` would).

`CharacterFixture`'s `id` field is a trap worth knowing about: its `Rule`'s
`UUID.randomUUID()` call runs once, when `loadTemplates()` executes — not once per
`blank`/`gimme` call — so every `Character` built from the same template *within one test*
shares that one `id`. This doesn't affect `Scene`'s correctness (it keys off
`CharacterSheet.id`, and `CharacterSheet.of(...)` is a real constructor call that mints a
fresh `id` every time, never templated), but a test that specifically needs several distinct
`Character`s with distinct `id`s must override `.id(UUID.randomUUID())` on each one via
`CharacterFixture.blank(...)`'s returned builder.

## A ModifierType per skill — fixing cross-skill bonus leakage

`ModifierType.SKILL_ROLL_BONUS` applies broadly, to *every* Perícia's roll — that's correct
for abilities like `DirigirECavalgarCompetencyAbility.CONTROLAR_ANIMAIS` (see the Vantagem
section below), but it was, until this was fixed, the *only* option `AbstractSkillInteraction`
summed — meaning a bonus meant for one specific Perícia (e.g. a temporary buff an ally
received "for Atletismo") had no way to avoid leaking into every other Perícia's roll too,
since `sumSkillRollBonusModifiers`/`getTemporaryBonus` had no per-skill filter to apply.

- Every `SkillType` now also carries its own `ModifierType` — `rollBonusType` — e.g.
  `ATLETISMO`'s is `ModifierType.ATLETISMO_ROLL_BONUS`. This can't be a runtime-computed
  mapping, since `@Modifier(ModifierType.X)` is a compile-time-fixed annotation value;
  `ModifierResolver` has no way to scan "give me whatever modifiers apply to skillType Y" — it
  can only scan for one fixed `ModifierType` at a time. One real enum constant per skill is
  the only way an ability's `@Modifier` method (or a granted `TemporaryBonus`) can target one
  specific Perícia through this reflection-based mechanism.
- `AbstractSkillInteraction.sumSkillRollBonusModifiers` sums **both** `SKILL_ROLL_BONUS` and
  `skillType.getRollBonusType()` for every source (`attributeAbilities`,
  `skillCompetencyAbilities`, unlocked `SkillExcellency` tiers), and `applyTo` does the same
  for `target.getTemporaryBonus(...)` — additively, not either/or, so an ability or a granted
  `TemporaryBonus` can choose either the broad generic type or one specific skill's, and both
  still combine correctly on a roll that has some of each (see `ArtesInteractionTest
  #applyToCombinesTheGenericAndArtesSpecificTemporaryBonusesAdditively`).
- **Whenever a new `SkillType` constant is added, add a matching `<SKILL>_ROLL_BONUS`
  constant to `ModifierType` too** (see the "Adding a new Perícia" section/`adding-a-pericia`
  skill) — there's no default;
  omitting it would make `SkillType.rollBonusType` uninitializable.

## Temporary bonuses from other Characters — `CharacterSheet#grantTemporaryBonus`

A `CharacterSheet` can hold bonuses/maluses granted by *another* Character's action, lasting a
few Rodadas. The motivating example is `ArtesCompetencyAbility.DOM_BARDICO`.

**Use the `granting-a-blessing` skill** to add one — it carries the full walkthrough, the
`TargetScope` choice and its recipient-resolution, the `DOM_BARDICO`/
`GRITO_DE_GUERRA_VULCANO` worked examples, and how to tell a genuine `Blessing` case apart from
the narrower bonus mechanisms elsewhere in this file (a flat `@Modifier`,
`resolveConditionalRollBonus`, `resolveDamageBonus`, `resolveAbsoluteDamageReduction`/
`resolveHalfDamage`). The data model a *consumer* needs:

- `TemporaryBonus` (`org.aventyrs.core.sheet`) pairs a `ModifierType`, an `int value`, and a
  `remainingRounds` countdown — reusing the `ModifierType` taxonomy rather than a parallel one.
  Pass the broad `SKILL_ROLL_BONUS` or one skill's own type depending on whether the granting
  rules text names a Perícia. It counts down in *rounds remaining*, not an absolute expiry
  Round, so `CharacterSheet` never couples to `Scene`'s round-numbering.
- `grantTemporaryBonus(ModifierType, int value, int rounds)` adds one;
  `getTemporaryBonus(ModifierType)` sums every currently-active one of that type.
  `CharacterSheet` doesn't track *who* granted a bonus — find targets via `Scene.getAllies` at
  grant time instead.
- `tickTemporaryBonuses()` counts every held bonus down and discards expired ones. Reached
  automatically via `CharacterSheet#finishTurn()`, which `Scene#next()` calls on whoever's turn
  is ending (see "Iniciativa can change mid-Scene" below).
- `Blessing` (`org.aventyrs.core.sheet`) is what an Interaction *reports* granting: a
  `ModifierType`, `int value`, `int rounds`, a `TargetScope scope`, and a `String source`.
  `modifierType` deliberately matches `TemporaryBonus`'s own field, so a caller passes it
  straight into `grantTemporaryBonus` with no mapping step. Who actually *receives* a blessing
  is not this core's concern — a caller resolves recipients via `Scene.getAllies`/`getEnemies`,
  `SceneContext.getAlliesWithin`, or its own target lookup, and calls `grantTemporaryBonus` on
  each. `source` identifies the granting *trait*, never which Character: **prefer the granting
  enum's own `.name()`** over a string literal wherever the granting site has the constant in
  hand, so the two can't drift.
- `InteractionResult.blessings` is a `List` — one Interaction can report several at once. It
  stays `null` when nothing is granted (same not-applicable convention as every other field);
  a caller gates on `!= null`. `InteractionResult` is `@Builder(toBuilder = true)` so an
  overriding subclass extends the base result rather than reassembling every field.

## Rolling the dice — `SkillRoll`, `DifficultyLevel#reachedBy`

This core deliberately never rolls dice itself (see the `skill` package-info's "What this
library computes" section) — but plenty of abilities' effects depend on which GD tier a roll
*reached*, or whether it was a critical, so an API layer needs a way to hand an
already-physically-rolled result in, and this core needs a way to turn that into something
useful. Every Perícia roll in this ruleset is 3d6 — always exactly 3 six-sided dice.

- `SkillRoll` (`org.aventyrs.core.skill`) wraps the 3 individual die faces as a
  `List<Integer>`, not just their sum — a compact "111-666, sum the digits" packed-int
  encoding was considered and rejected: it buys "one int instead of a 3-element list" at the
  cost of no compile-time safety (an invalid value like `289` or `700` only fails at
  runtime, deep in parsing) and an unusual decode step every time someone reads a value like
  `435`. The individual faces matter, not just the total, because `getCriticalResult()`
  depends on *matching* dice at the extremes: three 1s is `FALHA_CRITICA_MAIOR`, two 1s is
  `FALHA_CRITICA_MENOR`, three/two 6s are the `ACERTO_CRITICO_MAIOR`/`_MENOR` equivalents (see
  `CriticalResult`) — the sum alone can't distinguish "rolled 1-1-4" from "rolled 2-2-2", but
  the first is a critical failure and the second isn't. The two/six-case "Acerto Crítico"
  pairing is inferred by symmetry with the confirmed "Falha Crítica" one, not itself
  independently confirmed against rules text — flagged in `CriticalResult`'s own javadoc.
  `SkillRoll`'s constructor validates exactly 3 dice, each 1-6 — a genuine system boundary
  (input from outside this core), unlike internal invariants this codebase otherwise trusts.
- `DifficultyLevel.reachedBy(int total)` resolves the highest tier a total clears, judged
  against each tier's `baseValue` — mirroring `SkillExcellency.unlockedBy`'s
  threshold-lookup shape, returning `Optional.empty()` if total falls short of even
  `VERY_EASY`. Its sibling `DifficultyLevel.reachedByAsExpert(int total)` does the identical
  lookup against `expertValue` instead — see "Requesting a specific Habilidade de Competência
  or Especialização on a roll" above for how `AbstractSkillInteraction` picks between the two
  (a held, matching `SkillSpecialization` named as the roll's `requestedAbility` switches to
  `reachedByAsExpert`). Neither method validates *why* a caller picked one over the other —
  resolving "does this roll's Especialização match what it's being used for" beyond mere
  possession is still a separate, unbuilt concern (this core doesn't track what a roll is *for*
  — same gap as scoped Vantagem/substitution elsewhere).
- `AbstractSkillInteraction` gained a third `applyTo` overload —
  `applyTo(CharacterSheet, SceneContext, SkillRoll)` — following the exact same
  cascading-delegation shape as the `SceneContext` overload before it: each shorter overload
  just delegates down with `null` for the new parameter, and the longest one holds all the
  real logic. When `skillRoll` is non-`null`, it sets `InteractionResult.reachedDifficultyLevel`
  (from `DifficultyLevel.reachedBy`/`reachedByAsExpert(skillRollBonus + skillRoll.getTotal())`,
  picked per the previous bullet) and `.criticalResult` (from `skillRoll.getCriticalResult()`);
  both stay `null` otherwise, same as every other not-applicable `InteractionResult` field.
  `ArtesInteraction` now overrides this 3-arg overload (it used to override the 2-arg one) —
  a subclass always overrides the *longest* overload whose real logic it needs to build on
  top of, not a shorter one, even if it doesn't touch that overload's own newest parameter
  itself; every shorter overload still reaches its override correctly through virtual
  dispatch, so this changes nothing for callers using `applyTo(target)`.
- The engine to know *which* tier a roll reached now exists; each ability's own consumption
  of that is still separate, per-ability wiring — `ArtesCompetencyAbility.DOM_BARDICO` is done
  (its 5-tier bonus table now maps onto 5 consecutive `DifficultyLevel` constants, with only
  `UNIMAGINABLE` handled by inference rather than named text — see `ArtesInteraction`'s own
  javadoc), but e.g. `MedicinaECuraExcellency.FOCADO`'s auto-success is still unbuilt.
  Deliberately don't guess at a mapping where the rules text leaves genuine ambiguity — DOM_
  BARDICO's own `UNIMAGINABLE` inference is flagged as exactly that, an inference, not text.

## Range and SceneContext — `org.aventyrs.core.scene.SceneContext`

Some bonuses are conditioned on how close an ally or enemy is. This core has no
grid/positioning system, so distances always arrive already-resolved from a caller.

- `Range` (`org.aventyrs.core.scene`) is the seven UD bands — `ADJACENTE` (1),
  `DISTANCIA_MUITO_CURTA` (2), `DISTANCIA_CURTA` (4), `DISTANCIA_MEDIA` (8),
  `DISTANCIA_LONGA` (16), `DISTANCIA_MUITO_LONGA` (24), and `AO_ALCANCE_DOS_OLHOS` (limited by
  eyesight, so its `maxUnidadesDeDistancia` is `null` — the one constant where that field
  doesn't apply). Ordered nearest-to-farthest so `isWithin(Range)` expresses "Distância Curta
  ou inferior" via ordinal comparison. `fromUnidadesDeDistancia(int)` resolves a raw UD count
  to its band (past 24 → `AO_ALCANCE_DOS_OLHOS`).
  > This enum first shipped missing `DISTANCIA_MUITO_CURTA` because it was built from memory.
  > Get the source rules text before modeling a new domain enum, not just the usages already
  > visible in this codebase's TODOs.
- `Scene.getEnemies(CharacterSheet)` is the complement of `getAllies` — every participant not
  sharing that sub-group. With more than two sub-groups, "not my group" and "hostile to me"
  aren't the same thing; this core has no faction/allegiance concept beyond that binary.
- `SceneContext` is a **plain, resolved snapshot** — `allies`, `enemies`, and a
  `Map<CharacterSheet, Range>` of known distances (anyone absent reads as out of range) — with
  **no `Scene` reference**. It never holds or queries a live `Scene`: that would couple a
  lightweight context to the whole class, make it awkward to construct in isolation, and give
  it implicit "live" semantics a per-roll snapshot doesn't want.
  `Scene.buildContext(CharacterSheet, Map<CharacterSheet, Range>)` resolves the lists **once**
  for a caller that has a `Scene`; anyone else constructs one directly.
- Exposes `getDistanceTo`, `hasAllyWithin`/`hasEnemyWithin`, `countAlliesWithin`/
  `countEnemiesWithin` (for a bonus scaling with *how many* are nearby), and
  `getAlliesWithin`/`getEnemiesWithin` (the sheets themselves, for a caller inspecting each
  qualifying one — e.g. `DamageServiceImpl`'s `BASTIAO_DOS_NECESSITADOS` scan). All four
  `*Within` shapes share one private `isWithin` filter.
- `FRIEZA` is the reference for a fully-wired proximity bonus: `SkillCompetencyAbility
  #resolveDamageBonus(SceneContext, CharacterSheet attackTarget)` (default `Optional.empty()`)
  returns a `DamageBonus` (`value`/`DamageType`, `org.aventyrs.core.character`), carried on
  `InteractionResult#damageBonus`. Both the amount *and* the condition live on the constant.
  `AtaqueADistanciaInteraction`'s 4-arg `applyTo(..., CharacterSheet attackTarget)` overload
  supplies the target and takes the first non-empty result, knowing nothing about which
  constant answered. Melee has no such overload yet.
- `AbstractSkillInteractionTest.GangUpBonusInteraction` demonstrates the count-scaled shape —
  `min(countAlliesWithin(range) * perAllyBonus, max)` inside a subclass's `applyTo` override —
  as a test-only subclass, since no real ability needs it yet. Follow that rather than the
  boolean `hasAllyWithin` shape when one turns up.

Most proximity-gated abilities still need at least one *other* missing system too — `FOCADO`,
`PERCEPCAO_DE_FOXM`, `ESPALHAR_EMOCOES`, `ESCONDER_OUTROS`, and `DISPARO_RICOCHETE` are all
still blocked. Each one's TODO says which piece `SceneContext` closed and which remain.

### Cena de Combate, Rounds, and "ganhou a iniciativa"

Three more facts `Scene` resolves once and carries into the `SceneContext` snapshot, the same
already-resolved way as allies/enemies/distances/terrain:

- `Scene.combatScene` (`isCombatScene()`/`setCombatScene(boolean)`) — `false` until a caller
  flips it once combat breaks out, same as `terrainType` starts unset.
- `Scene.wonInitiative(CharacterSheet)` resolves "ganhou a iniciativa" at the **sub-group**
  level: a group's value is the highest individual `getEffectiveInitiativeValue()` among its
  members, compared against every other group's highest. A tie for the overall highest is a win
  for every group sharing it — the rules name no tie-breaker, so don't invent one.
- `SceneContext.isWithinFirstCombatRounds(int roundCount)` is the shared round-window
  primitive for every "duas primeiras Rodadas de cada Cena de Combate" clause. **Round 0 never
  counts** — it's `Scene`'s "before anyone acted" starting value, so eligibility starts at
  Round 1 and `roundCount=2` covers Rounds 1 and 2. A deliberate reading, not explicit text.

A caller building a `SceneContext` directly gets non-combat defaults (`false`/`0`/`false`).

## Vantagens de Ego — `Character#egoAdvantages`

A Vantagem de Ego (`AutocontroleAdvantage`, `InitiativeAdvantage`, `ResourcesAdvantage`,
`SorteAdvantage`, in `org.aventyrs.core.ego`) is chosen once at character creation, gated on that
`EgoDomain`'s creation-time `base` reaching `CharacterCreationService.EGO_ADVANTAGE_MIN_BASE` (3)
— the same threshold for every domain, checked via the single generic
`isEgoAdvantageAvailable(EgoDomain, CharacterEgos)`. Don't reintroduce a per-domain
`isXAdvantageAvailable` method/constant pair unless a domain's threshold is ever confirmed to
differ from 3. **Use the `adding-an-ego-advantage` skill** to add one — it carries the
clause-to-hook mapping, the acquisition-time-choice pattern, and the test checklist.

`Character` stores every domain's choice in one `@Singular Map<EgoDomain, EgoAdvantage>
egoAdvantages` — never one nullable field per domain. A domain with no chosen Vantagem is
simply absent, never a `null` value inside. Read via `Character#getEgoAdvantage(EgoDomain)`,
set via the `@Singular`-generated `.egoAdvantage(EgoDomain, EgoAdvantage)` — never index the
map directly outside those two spots.

`EgoAdvantage` carries seven `default` hooks alongside `getEgoDomain()`/`getDescription()`, all
defaulting to empty, mirroring `SkillCompetencyAbility`'s `resolve*` shape (same reason: not
reflection-discoverable via a no-arg `@Modifier`), but summed across **every** skill, since a
Vantagem de Ego was never tied to one Perícia: `resolveConditionalRollBonus`,
`resolveSkillSpecificRollBonus`, `resolveDamageBonus`, `resolveAbsoluteDamageReduction`,
`resolveHalfDamage`, `resolveCriticalMarginIncrease`, and `resolveInitiativeBlessings`. Three
distinctions worth knowing without doing Vantagem work:

- **`resolveSkillSpecificRollBonus(SkillType, SceneContext, CombatantSheet target)`** exists so a
  Vantagem scoped to specific *named* skills (e.g. `MORAL_HERDADA`'s "+1 em Artes e Persuasão")
  doesn't over-grant into every other Perícia the way `resolveConditionalRollBonus` would. A
  scope of named skills *is* trackable, unlike a narrative *purpose*. Its `target` is the
  roller's own sheet (not an attack target), passed because the bonus may depend on the roller's
  own live state.
- **`EgoAdvantage#resolveDamageBonus(SceneContext)`** needs no `attackTarget`, so it works for
  both Ataque à Distância *and* Corpo a Corpo off the plain 2-arg `applyTo` — unlike
  `SkillCompetencyAbility#resolveDamageBonus`, which is only reachable through the 4-arg
  `attackTarget`-aware overload. First non-empty wins.
- **`resolveCriticalMarginIncrease(SkillType, SceneContext)` is fully consumed**, and lives on
  all three of `EgoAdvantage`/`AttributeAbility`/`SkillCompetencyAbility` with an identical
  signature — `AbstractSkillInteraction#sumCriticalMarginIncrease` sums it across all three
  sources and feeds the total to `SkillRoll#getCriticalResult(int)`, which widens which faces
  count toward a critical. `SorteAdvantage.ACE` is the reference override (+1 for a Perícia de
  Ataque during a Cena de Combate, +3 for a non-Ataque Perícia outside one, 0 otherwise). Its
  still-unconsumed neighbour is the differently-shaped
  `ArtesAprimorarComArteAbility#getCriticalMarginReduction`.

`InitiativeAdvantage.IMPETO` and `TORRE_EM_MOVIMENTO` are the `SceneContext`-conditioned
overriders; `MoralHerdadaAbility` is the skill-scoped one, and also the reference for a Vantagem
carrying an acquisition-time choice (the enum constant stays the catalog entry; the character is
granted a `MoralHerdadaAbility(FamaChoice)` *instance* in `egoAdvantages`, reading the
choice-dependent value live off `target` rather than freezing it at acquisition).
`MoralHerdadaAbility#applyStartingFama(Character, CharacterSheet)` is real and tested but has
**no automatic caller** — `CharacterCreationServiceImpl` only assembles a `Character`, and Fama
lives on `CharacterSheet`; the same ordering gap `upgradeBase`/`upgradeGraduation` work around by
taking both explicitly.

`POSICIONAMENTO_ESTRATEGICO`'s Reação-suppression half is the only piece of this family still
TODO'd, on the movement-triggers-Reação gap. Check each constant's own TODO rather than
assuming one fix unblocks every citation of it.

## Ego points are two pools per domain — `EgoPointPool`, `spendEgoPoints`

An `EgoDomain` isn't only a rating: it's **two spendable point pools** on a `CombatantSheet`,
permanent and temporary, both real currency. The whole model is four equations, all in
`org.aventyrs.core.sheet.EgoPointPool`:

```
permanentMax        = character.getEgos().getEgo(domain).getTotal()   // base + variable
permanentRemaining  = max(0, permanentMax - permanentSpent)
temporaryCeiling    = max(0, permanentRemaining + Σ bonus(source) - Σ activeEgoPenalty)
temporaryRemaining  = max(0, temporaryCeiling - temporarySpent)
availableEgoPoints  = permanentRemaining + temporaryRemaining
```

**Both pools start full**, and the temporary ceiling tracks permanent points *remaining* rather
than the permanent maximum — so **spending a permanent point hurts twice**. Sorte 3 with nothing
spent is 3 + 3 = 6 spendable; spend the 3 temporary then the 3 permanent and you get all 6, but
spend the 3 permanent *first* and the ceiling collapses to 0, so you only ever get 3. Nothing
special-cases that — it falls out of the arithmetic. `EgoPointFeatureTest` pins both directions.

- **Permanent points never recover.** Not on Rest, not per session, not anywhere. They're only
  *earned*, via `AttributeAbility#resolvePermanentEgoGain` → `CharacterEgos#withVariableBonus`.
  Nothing reduces them automatically either — only their holder spending one deliberately.
- **Temporary points recover one per game session** — one *total* across all four domains, not
  one apiece, with the player choosing which domain receives it. That's
  `EgoPointsService#applySessionRecovery(sheet, chosenDomain)`, plus whatever extra a held
  Vantagem grants in its own domain (`EgoAdvantage#resolveExtraSessionEgoRecovery`;
  `MOTIVACAO_DE_MOSES`/`DILETO_DE_TYKHE` are the two). **No automatic caller, by design** — a
  session ends when the table says so, so the trigger is a GM action in the consuming app. The
  bulk overload `applySessionRecovery(Map<CombatantSheet, EgoDomain>)` is what that button calls:
  the map carries each player's own choice, and being *in* the map is how a consumer excludes a
  foe or an absent player — no `instanceof CharacterSheet` filter, no "everyone in the Scene"
  default. Pair it with `Scene#getAllParticipants()` (active ∪ pending, disjoint by construction)
  to build the roster. It is **deliberately not idempotent**: a second press recovers again,
  bounded only by each ceiling, and guarding that belongs to the consumer, since this core has no
  session state to hang a guard on.
- **Rest does not refill the temporary pool.** `RestService#applyRest` resolves only
  `PendingEgoRecovery` — points some effect *specifically promised back* (today: `Primor`'s).
  Don't wire session recovery into it.

**Why a `spent` counter and not a held balance.** Both halves are `ResourcePool`s. With a stored
balance every ceiling change (a permanent spend, a penalty landing or expiring, a point earned)
needs a destructive clamp pushed from four call sites, and such a clamp is irreversible — an
expiring penalty could never give back the point it truncated. With a spent counter the ceiling is
only ever *read*, so what remains is recomputed fresh and un-clamps by itself. Same
recompute-on-demand discipline as `HitPointsService#getStatus` and
`InitiativeEntry#getEffectiveInitiativeValue`. `temporarySpent` is additionally **normalized down
to the current ceiling** on every temporary-facing call, so a fallen ceiling can't leave a hidden
debt that swallows the next recovery — deliberately unlike `ResourcePool` for PV, where overspend
past the max is *kept*, because that negative range is what distinguishes FALLEN/COMMA/DEAD.

**The permanent max is read straight off `Character`, and PV's isn't.** `AbstractCombatantSheet`
calls `getCharacter().getEgos().getEgo(domain).getTotal()` directly. That looks like the
`HitPointsService#getStatus` case but isn't: `getStatus` had to leave `sheet` because max PV needs
a `ModifierResolver` three-source `@Modifier` scan and `org.aventyrs.core.sheet` must not depend
on `org.aventyrs.core.character.services`. A permanent Ego max needs **no scan at all**.

**Don't confuse the pool with `InitiativeService`.** It reads the same `EgoValue.getTotal()`, then
adds a `ModifierType.INITIATIVE` three-source sum, because Iniciativa doubles as a turn-order
stat. A `TemporaryBonus(INITIATIVE, +2, 2)` must never widen how many Iniciativa *points* can be
spent. This asymmetry is the likeliest future misreading of the two.

**Spending names its pool, and reports back.** `spendEgoPoints(EgoDomain, EgoPointType, int)`
returns an `EgoPointSpend` (domain, type, and the amount *actually* spent after clamping). There
is deliberately **no** temporary→permanent fallback (it would silently spend a point that costs
twice over) and **no** shorter overload defaulting the type — the cascading-overload convention is
for genuinely *optional* inputs, and the pool is the whole question. It floors at 0 rather than
throwing; affordability is the caller's own `getAvailableEgoPoints(domain) >= n`. The reported
`type` is what `DETERMINACAO_HEROICA`'s "se o ponto for permanente" needs, and it's why callers
promising points back (`Primor` → `PendingEgoRecovery`) must register `spend.getValue()`, never
what they asked for.

**Two hooks fire off a deliberate spend**, both resolved by `useEgoPointsForEffect` and both
scoped to the Vantagem held in the spend's *own* domain: `resolveEgoSpendRecovery` returns a flat
figure applied to PV/PM/PD (`DETERMINACAO_HEROICA`), and `resolveEgoSpendBlessings` returns
`Blessing`s granted straight to the spender (`AS_NA_MANGA`'s +2UD Movimento). Blessings are
applied directly rather than handed back, unlike `resolveInitiativeBlessings`, because who spent
the points is unambiguous — so `TargetScope` other than `SELF` has no meaning there yet.

**Using points is a different call site from being drained of them.**
`EgoPointsService#useEgoPointsForEffect(sheet, domain, type, amount, rolledValue)` is the holder
deliberately spending — it spends *and* applies whatever `EgoAdvantage#resolveEgoSpendRecovery`
that use earns (`DETERMINACAO_HEROICA`'s "+1d6PV, PM e PD", doubled on a permanent point), in one
call so the recovery can't be forgotten. `CombatantSheet#spendEgoPoints` is the raw drain, and is
what `Primor` calls — reacting there would mean a critical hit *healing* the character it just
hit. There is no flag on the spend distinguishing the two, and no observer: the choice of entry
point is the distinction. The hook is consulted only for the Vantagem held in the spend's **own**
domain, the same scoping `resolveExtraSessionEgoRecovery` uses. The 1d6 arrives already rolled
(one roll covers all three pools) and is validated as a legal d6 face — a negative would otherwise
reach `heal` and silently *damage* the character.

**A temporary Ego reduction is `TemporaryEgoPenalty`, a dedicated `TemporaryEffect`** — not a
negative `TemporaryBonus`. It lowers the temporary *ceiling* and is never a spend, which is the
point: a spend is consumption that would outlive the penalty's own Duração, while a ceiling term
reverses the moment the effect expires. Adding `AUTOCONTROLE`/`RECURSOS`/`SORTE` to `ModifierType`
would expose them to `ModifierResolver`'s `@Modifier` scan (implying abilities could grant
spendable Ego points), and `INITIATIVE` already means turn order, so one of the four domains would
collide semantically with no way out. Same restraint that keeps `LifeSteal` off `ModifierType`. It
rides the ordinary `applyEffect`/`tickTemporaryEffects`/`finishTurn` lifecycle with no new
machinery, and is read by a private `sumEgoPenalty` mirroring `getTotalLifeSteal`.

**`grantTemporaryEgoPointBonus(domain, source, amount)` raises the ceiling, non-cumulatively per
source** — this is what `CharismaAbility.DESTINO_FAVORAVEL`'s "um ponto temporário, não
cumulativo" actually is. Repeat triggers from that same source don't widen it twice; an unrelated
source's grant still adds on top.

## Movimento Base, and blessings granted on winning initiative

**`MovementService#getMovementBase(Character)`** follows the `InitiativeService` variant of the
aggregated-stat shape (no new `Character` field, no `CharacterFixture` change). Base is derived
— `SizeCategory.getMovementPerActionPoint()` (via
`CharacterSizeService#getEffectiveSizeCategory`, so size-shifting is reflected) — plus the usual
`ModifierType.MOVEMENT` three-source sum, using `SkillCompetencyAbility.allFor` so racial
abilities count (unlike `ReactionsService`/`InitiativeService`, which predate that fix). Floored
at 0. Returns the **permanent** figure only; a caller wanting this Round's actual one adds
`CharacterSheet#getTemporaryBonus(ModifierType.MOVEMENT)`.

**It is a figure *per Ponto de Ação*, not a whole-Turn allowance**, which is why it takes no
`turnNumber` and never touches `ActionPointsService`. A character spends their Pontos de Ação
across moving *and* everything else they do that Turn, and the split is the player's choice at
the table — so multiplying by `getMaxActionPoints` here would bake in an assumption this core
has no business making. A caller that wants distance covered multiplies this by however many
points were actually spent moving. `MovementServiceImpl` used to multiply, and its constructor
took an `ActionPointsService` for it; both are gone.

That per-point reading is also what a `MOVEMENT` bonus means, and it lands differently
depending on the rules text granting it. "Seu Movimento Base aumenta em +2UD"
(`AtaqueCorpoACorpoExcellency.FOCADO`, `AtletismoCompetencyAbility.PASSO_LARGO`,
`DexterityAbility.PASSOS_LONGOS`, `InitiativeAdvantage.POSICIONAMENTO_ESTRATEGICO`) lands
exactly — every point spent moving is worth that much more. `SorteAdvantage.AS_NA_MANGA`'s
"você pode se mover até 2UD" is a **one-shot step**, and as a `MOVEMENT` `TemporaryBonus` it now
over-grants once its holder spends more than one Ponto de Ação moving; that's flagged on the
constant and deliberately kept, since a one-shot movement allowance is a mechanism this core
doesn't have and granting nothing would be further from the clause. Check which of the two
shapes a new grant is before reaching for `ModifierType.MOVEMENT`.

Vertical/swim movement (`AtletismoCompetencyAbility.ALPINISTA_VELOZ`/`ANFIBIO`) and a
mount's own movement (`DirigirECavalgarExcellency`) are a **different** sub-stat — don't wire
them into `ModifierType.MOVEMENT`.

**`Blessing`** is also what a trait grants the moment its holder wins initiative:
`default List<Blessing> resolveInitiativeBlessings()` (empty by default) on `EgoAdvantage`,
`AttributeAbility`, and `SkillCompetencyAbility` — those exact three sources, deliberately
**not** `SkillExcellency`, unlike the four-source flat-`@Modifier` convention — scanned by the
pure-function `InitiativeBlessingService#resolveBlessings(Character)` and applied by
`Scene#applyInitiativeBlessings(CharacterSheet winner, List<Blessing> blessings)`, which the
caller hands already-resolved blessings (`Scene` never reaches into a Service to compute what
abilities grant, the same restraint `buildContext` follows). **The `granting-a-blessing` skill
covers that path**; two `Scene`-internal facts it doesn't:

- `applyInitiativeBlessings` **revokes every blessing an earlier call granted** — via
  `CharacterSheet#removeEffect`, reference-based, since neither `TemporaryEffect` nor
  `TemporaryBonus` overrides `equals()`, so an unrelated bonus of the same `ModifierType`
  survives — before granting the new set, tracking each fresh `TemporaryBonus` in a
  `Scene`-owned `grantedBlessings` map so the next call revokes precisely these and no more. It
  throws `IllegalOperationException`/`INITIATIVE_NOT_WON` if `wonInitiative(winner)` isn't
  already true.
- `addParticipant(CharacterSheet, int, UUID group)` extends currently-active **ally-scoped**
  blessings to someone joining an already-blessed group. `isGroupBlessed(UUID)` derives that
  from `grantedBlessings` directly rather than caching it — a group is blessed exactly when
  someone already tracked belongs to it. The blessing *values* (`activeBlessings`) do need
  their own field, since `grantedBlessings`' raw `TemporaryBonus`es don't carry the originating
  `scope`. A `TargetScope.SELF` blessing never propagates to a newcomer or to allies.

## Iniciativa can change mid-Scene — `InitiativeEntry#getEffectiveInitiativeValue`

`getInitiativeValue()` is fixed once rolled, but a participant's Iniciativa *standing* isn't:
a granted bonus can change it mid-Scene. All such bonuses route through the pre-existing
`ModifierType.INITIATIVE`/`TemporaryBonus` machinery.

The key constraint: **`CharacterSheet` has no reference back to its `Scene`**, so a granting
Ability can never call into `Scene` — it grants a plain
`grantTemporaryBonus(ModifierType.INITIATIVE, value, rounds)` exactly like every other ability
effect, and `Scene` picks it up itself, on its own schedule. No push method
(`Scene#updateInitiative`) and no observer mechanism was added — this codebase has no
event/observer pattern anywhere, and `Scene` already references every `CharacterSheet` it
tracks, so it can just ask.

- `getEffectiveInitiativeValue()` is `getInitiativeValue()` plus whatever
  `ModifierType.INITIATIVE` currently sums to on `characterSheet`, resolved fresh every call.
- `Scene#wonInitiative`/`#buildContext` (via `bestInitiativeValue`) and `insertSorted` all read
  the effective value, so a granted bonus flips "who's winning" the instant it's granted.
- Turn *order* is deliberately **not** live: `activeEntries` keeps its last sorted sequence
  until the next Round boundary, so a granted bonus never reshuffles turns already in progress
  (the same guarantee `addParticipant` gives a mid-Round newcomer via `pendingEntries`).
  `Scene#next()`'s round-wrap point is `startNewRound()` — merges `pendingEntries` *and*
  re-sorts `activeEntries` by effective value (`List#sort`, stable, so ties keep their order).
- `Scene#next()` calls `CharacterSheet#finishTurn()` on whoever's turn is ending (skipped on
  the first call), which is what actually ticks `TemporaryBonus`es toward expiry. Consequence:
  a 1-Rodada bonus on the participant last in the order expires in the very `next()` call that
  also triggers the re-sort, so it never appears in the resorted order — existing Rodada
  semantics, not something this introduces.
- `CharacterSheet#startTurn(int turnNumber)` is the mirror of `finishTurn()`, called as a Turn
  begins (passing `getCurrentRound()`), and fires even on the first `next()` call. `turnNumber`
  is 0-based, the same convention `ActionPointsService`/`ActionProfile`/`MovementService` use.
  Currently a **no-op** — nothing triggers "no início do seu turno" yet (`Bleeding`/`ManaDrain`/
  `Withering` all apply at Turn-*end*) — so its wiring has no test yet; add one alongside
  whatever first overrides a start-of-Turn hook.

## Two kinds of sheet — `CombatantSheet`, and why monsters can't level up

A combat participant is a `CombatantSheet` (`org.aventyrs.core.sheet`), not a `CharacterSheet`.
Two implementations exist, both extending `AbstractCombatantSheet`, which holds every shared
behaviour exactly once:

- **`CharacterSheet`** — a player character. Adds `player`, the experience wallet
  (`totalExperience`/`unUsedExperience`/`useExperience`/`accumulateExperience`) and Fama.
- **`MonsterSheet`** (`org.aventyrs.core.monster`) — a foe. Adds the four authored stat-block
  numbers a foe presents *because it never rolls*: `physicalDefense`/`magicDefense` (what a
  player's Ataque roll must beat) and `attackDifficulty`/`attackBonus` (what its own attacks
  present to a player's Esquiva e Aparar roll).

Everything else — damage, shields, Mana/Determinação, temporary Ego points, `TemporaryEffect`s,
inventory, the Turn lifecycle, `receiveInteraction` — is on the shared half and behaves
identically for both. **Type combat-facing signatures as `CombatantSheet`**; the only things that
should still name `CharacterSheet` are the four XP-spending services plus
`MoralHerdadaAbility#applyStartingFama` and `RestService#applyRest`.

- **That split is the enforcement mechanism, and it's the whole reason to prefer an interface
  here.** Experience lives on `CharacterSheet`, so `CharacterAttributeService#upgradeBase`,
  `SkillGraduationService#upgradeGraduation`, `FeatService#grantFeat` and
  `TitleAbilityService#grantTitleAbility` keep taking that concrete type and a monster cannot be
  passed to one. There is deliberately **no `isMonster()` flag and no runtime guard** — the
  compiler refuses it. `MonsterSheetTest` asserts this reflectively, because writing it as
  `monster instanceof CharacterSheet` doesn't compile, which is the point.
- **Ego points are on the shared half**, despite reading as player-facing: `Primor` applies to a
  *target*, so leaving them off `CombatantSheet` would break it against a foe. `EgoDomain`'s own
  javadoc says "a creature". Both pools live there — see "Ego points are two pools per domain".
- **`Character` is shared too** — a foe's Attributes, Perícias, abilities and equipment are an
  ordinary `Character`, with `race` set to the single catch-all `Monstruoso` and `player` left
  `null` (that field is nullable for exactly this reason, and nothing in main source reads it).
  Don't build a parallel `Monster extends Character`; the stat-carrying half was never
  player-specific.
- **`lifeMultiplier`/`determinationMultiplier` are now `Character` fields**, mirroring the
  `manaMultiplier` that already was one. This is what lets a foe's PV budget be tuned apart from
  its Vigor — previously the only way to make something tanky also inflated every Vigor-governed
  roll. They're not monster-only; a GM house rule uses them the same way.
- **`ModifierType.HIT_POINTS` is the *flat* PV grant, and is not interchangeable with
  `LIFE_MULTIPLIER`.** `HitPointsService#getMaxHitPoints` is `BASE_HIT_POINTS + Vigor.getTotal() *
  lifeMultiplier + getHitPointsBonus(character)`. Use the multiplier when bulk should scale with
  Vigor; use `HIT_POINTS` when rules text states an amount ("recebe Bônus Mágico de +10PV") — a
  stated number expressed as a multiplier uplift only lands correctly at one specific Vigor.
  `getHitPointsBonus` scans `attributeAbilities` **and** `SkillCompetencyAbility.allFor`;
  `getLifeMultiplier` deliberately still scans only the former, since widening it would change a
  total nothing asked to change. Neither scans `SkillExcellency` tiers.

## Building a foe — `org.aventyrs.core.monster.MonsterTemplate`

Two paths, the same `Item`/`AbstractItem`/`ArmorItem` split this codebase already uses:
`AbstractMonsterTemplate` (`@Builder`) is *fill in the form* for a designed foe, `GenericMonster`
is a catalog of stand-ins for *a generic monster on-scene*. **Use the `building-a-foe` skill** to
add either — it carries the path choice, the builder's `@Singular` names and defaults, the
`lifeMultiplier`-not-Vigor tuning rule, and the test checklist. Two facts worth knowing without
doing monster work:

- A foe's four numbers are **authored, not derived** — a stat block says what a Goblin's DF is; it
  isn't recomputed from its Destreza and Graduação the way a player's defence roll is. Nothing
  checks them against the Attributes behind them, deliberately.
- `spawn()` returns a fully independent `MonsterSheet` each call — and must, since
  `SkillGraduation` is mutable and `CharacterSkill#increaseGraduation` mutates in place, so
  sharing one across spawns would let one foe's growth raise another's.
- Beyond those four numbers, a stat block may also author `getActionPoints()`,
  `getSkillSpecializations()` (the `[Primal]`-style bracketed tag), `isUndead()` and
  `getCriticalEffectImmunities()`. All four are **defaulted**, so every pre-existing foe is
  unchanged; only add one when the rules text states it.

### Invocações — a third path, `SummonedMonsterTemplate`

A creature whose stat block is parameterized by its Conjurador (`Zumbi`, in
`org.aventyrs.core.monster.summon`) implements `SummonedMonsterTemplate` and lives in **its own
class**, not a catalog enum — it carries per-instance choices a constant can't hold.

- **The parameter is a plain `int`**, the Conjurador's Graduação in Domínio do Mana, because every
  clause of this shape keys off exactly that one number. `0` means no Conjurador — a Narrador
  placing one for narrative reasons, or a number typed into a form — and is a real, meaningful
  value, not a null stand-in. `spawn(Character)` reads it off a real caster; `spawn(int)` takes it
  raw; the inherited no-arg `spawn()` is the narrative case. Don't invent a Conjurador↔invocação
  entity: nothing else in this core would use it, and it would make the commonest case impossible.
- **`withConjurador` returns a copy.** The tier bonuses reach into Attributes, abilities and the
  authored attack bonus, all of which `spawn()` reads off the template *before* building the
  `Character` — so the Graduação can't be applied to the sheet afterwards, and a shared catalog
  entry must stay immutable.
- **An instance-based ability is what makes tiered numbers possible.** `ZumbiAbility` follows
  `ArtesAprimorarComArteAbility`'s pattern: `ModifierResolver` invokes `@Modifier` methods *on the
  source instance*, so a no-arg annotated method can return an instance field. The annotation's
  `ModifierType` is still compile-time-fixed; only the returned value varies.
- **One clause can need two consumers.** The Zumbi's "Bônus em Perícia de Ataque igual às
  Graduações do Conjurador" is both a `@Modifier` on the ability (raising the Zumbi's own Ataque
  roll) *and* the template's `getAttackBonus()` (raising the threshold its attack presents when the
  defender rolls) — opposite directions of the same exchange, per "Both directions of an attack".

### Efeito Crítico immunity — `CriticalEffect#applicableTo`

An Anatomia clause naming Efeitos Críticos a creature shrugs off is real, enforced data.
`CriticalEffectType` is the identity an immunity names, and `CombatantSheet
#getCriticalEffectImmunities()` (default empty on `AbstractCombatantSheet`, overridden by
`MonsterSheet`) is the set.

- **Keyed on an enum, not on `Class<? extends CriticalEffect>`.** Four of the five effects a Zumbi
  resists — Amaldiçoar, Dilacerar, Excruciante, Ferida Profunda — have no implementation at all.
  Keyed on a class, those immunities would be inexpressible until someone built the effect; keyed
  on the enum they're authored, exact data now, and correct the day the effect lands. Same "can't
  apply it yet doesn't mean can't compute it yet" discipline as an unread `ItemBonus` column.
  Add a constant when something names it, not for completeness.
- **The filter is on `CriticalEffect`, not in either attack entry point.** An immunity is a fact
  about the *victim*, identical whichever direction is running, and `AttackDelivery`/
  `AttackReceiver` both route their `criticalEffects` through the one static method. Writing it
  into one would leave the other silently wrong — the same mistake `resolveAttackRollBonus` once
  made by living on `AtaqueADistanciaInteraction` alone.
- **It filters, it doesn't throw.** A caller assembling an attack has no obligation to know what
  its target resists, and an attack that crits against an immune target is still a critical hit —
  it just produces a shorter chain.

## Both directions of an attack — `AttackReceiver` and `AttackDelivery`

The player always rolls, so a foe contributes a fixed number whichever way an exchange runs, and
`org.aventyrs.core.combat` has **two mirrored entry points** rather than one:

| | Foe attacks the player | Player attacks a foe |
| --- | --- | --- |
| Entry point | `AttackReceiver.resolve` | `AttackDelivery.resolve` |
| Player rolls | Esquiva e Aparar | a Perícia de Ataque |
| Foe contributes | a GD + flat bonus | a flat Defesa (DF or DM) |
| Critical trigger | the roll's **Falha** Crítica | the roll's **Acerto** Crítico |

Neither ever calls the other. Both are report-only, both roll exactly once (the roll consumes
`consumeFirstRollThisTurn` and can grant an Ego point), and both assemble the same pre-wired
`Damage → Correntes → Críticos` chain onto the result's `nextInteraction`.

- `CriticalEffect#validateCriticalHit` demands an *Acerto* Crítico, which is correct for
  `AttackDelivery` and **still awkward for `AttackReceiver`**, where the trigger is a Falha
  Crítica — a caller there has to construct the Efeito with a value describing something that
  didn't happen, and the Maior/Menor severity it should inherit from the defence roll is picked by
  hand instead. That translation is still missing; don't assume the offensive direction fixed it.
- The Corrente margin is inverted for `AttackReceiver` (attack beats defence by 5, or 7 vs
  `RESOLUTO`) — an inference from `EffectChainService`'s own text, flagged on the class.
- `AttackDelivery` **reports but does not apply** the attacker's `difficultyReduction`: it's
  denominated in níveis and a foe's Defesa is a flat integer, with no defined conversion. TODO'd
  on the class rather than guessed at.
- **The `attackTarget`-aware 4-arg `applyTo` now lives on `AbstractSkillInteraction`**, gated on
  `isAttackSkill()`, not on `AtaqueADistanciaInteraction`. That was the fix
  `AnoesRacialAbility.ABATEDORES_DE_GIGANTES` needed — its rules text always covered every Perícia
  de Ataque, but melee had no way to see the target.
- **`DeliveredAttack#attackSource` carries what the attack is made with** (a `Weapon` or a
  `Spell`), and `AttackDelivery` hands it to the longest `applyTo` so a delivery-scoped ability
  like `ARREMESSO_PODEROSO` resolves against the real attack — see "Unconditional Perícia
  base-Attribute substitution". It's optional, and applies on the `attackRoll == null`
  bonuses-only preview path too. `AttackReceiver` has no equivalent: the roll there is the
  defender's Esquiva e Aparar, and this core models nothing about what the *foe* swung.

## Raças — `org.aventyrs.core.race`, not `org.aventyrs.core.character`

`Race` and every implementation (~22 today, plus `CreatureType`, `AbstractMesticoRace` and any
`*RacialAbility` enums) live in their own top-level package, `org.aventyrs.core.race` — a
sibling of `org.aventyrs.core.character`, not a subpackage of it, mirroring how
`org.aventyrs.core.ability`/`org.aventyrs.core.feat` already sit alongside `character` rather
than inside it. `Character` still holds a `Race race` field — the two packages reference each
other (`Race#generateEmptyCharacter` returns a `Character.CharacterBuilder`), an ordinary mutual
class dependency, not a layering violation. If a future race needs its own subpackage
(mirroring `org.aventyrs.core.skill.<skillname>`), nothing rules that out; there just wasn't a
need yet.

**Use the `adding-a-race` skill** to add one — it carries the three Race shapes (stateless,
bespoke Mestiço, `AbstractMesticoRace`), the clause-triage table separating the handful of
mechanically-real traits from the many gap-blocked ones, the `*RacialAbility` hook selection,
the Mestiço constructor invariants, and the test checklist. The rationale it defers back here:

- **Racial abilities reuse `SkillCompetencyAbility`, deliberately.** A trait every member of a
  Race gets contributes to a roll the *exact* same way a player-acquired ability does, so
  `Race#getRacialAbilities()` is just another `List<SkillCompetencyAbility>` rather than a
  parallel duplicate type — differing only in *where* it's sourced from (fixed per Race, not a
  per-Character acquisition), never in how a consumer treats its entries.
  `AbstractSkillInteraction`'s private `allSkillCompetencyAbilities(Character)` concatenates
  both lists, so every `applyTo` scan (the `@Modifier` `skillRollBonus` sum, the
  `getDifficultyReduction()` sum, `resolveAttributeDomain`, and `validateRequestedTrait`) covers
  racial abilities with zero special-casing. That's what makes
  `ElfosRacialAbility.SENTIDOS_ABSOLUTOS` work for every `Elfo` with no
  `AttentionInteraction`-specific code at all.
- **Not every race needs a `*RacialAbility` catalog — most don't.** Only `Anao`/`Elfo` author one
  (the Mestiços inherit rather than author); the other ~18 races leave `getRacialAbilities()` at
  the empty default, because their traits genuinely don't fit the shape — some aren't
  roll-conditioned at all (a Defesa modifier, a conditional RD), one needs a target
  classification this core can't make, and several are "grant an acquisition slot" traits (the
  `Elfo` Origem Mística / `Anao` Pequenos Gigantes gap). Don't force an enum into existence for
  symmetry.
- **`resolveAttackRollBonus(CombatantSheet actor, CombatantSheet attackTarget)` is summed, not
  first-non-empty** — unlike `resolveDamageBonus`, where only one bonus is expected to apply per
  roll. It exists because Abatedores de Gigantes targets `skillRollBonus` itself and is
  conditioned on the real attack target's `SizeCategory`, which a reflection-invoked no-arg
  `@Modifier` can't see. It now reaches **both** attack Perícias: the `attackTarget`-aware 4-arg
  `applyTo` was lifted from `AtaqueADistanciaInteraction` to `AbstractSkillInteraction`, gated on
  `SkillType#isAttackSkill()` (see "Both directions of an attack").
- **A whole-number feat-cost override is real; a fractional one isn't.** `Gigantes` overrides
  `getNewFeatCost(FeatCategory)` for real (Talentos de Sobrevivência at 2 XP instead of
  `Race.BASE_NEW_FEAT_COST`'s 3), because 2 is a whole number `int` represents exactly. Every
  other race's "Talentos custam menos" trait wants 2.5 (`Elfo`'s Conexão com o Mana, `Human`'s
  and `Pequenino`'s Adaptação) and stays TODO'd on the int-vs-fractional mismatch.
- **Mestiços never chain, and this core still doesn't roll dice.** `Race#isMestico()` is the flag
  a Mestiço constructor validates its chosen `parentRace` against ("que não seja mestiça"), and
  `CreatureType` (HUMANOIDE/FEERICO/MONSTRUOSO) is what a parent is checked against — a Mestiço
  reports its parent's own type, "conforme sua metade não-elemental", so "Mestiço" is a separate
  flag, not a fourth `CreatureType`. "2 Características Raciais **aleatórias**" is honoured the
  same way `SkillRoll` is: the constructor accepts up to 2 already-externally-resolved abilities
  (validated against the parent's own list) rather than rolling them here.

## Dano Base is a position on a scale — `DamageBase`, `DamageBaseService`

**Dano Base and a dano *bonus* are different mechanics and never merge.** A `DamageBase`
(`org.aventyrs.core.character`) is the raw `<dice>d6+<value>` an attack starts from; a
`DamageBonus` is a flat number added to an already-rolled total. That's why a character can
attack at a Dano Base of 1d6+0 and still land for 1d6+20 — the twenty is bonuses, not twenty
scale-ups. Never sum the two, and never model a "+N Dano Base" clause as a `DamageBonus` (or
vice versa): a scale-up may be worth a whole extra die.

**The scale is an odometer with an open-ended overflow.** `value` climbs to `MAX_VALUE` (3),
then rolls over — reset to 0, add a die — until `diceCount` reaches `MAX_DICE` (3). Once *both*
are capped, every further scale-up adds a flat +2, forever:

```
0 → 1d6+0   4 → 2d6+0   8 → 3d6+0   12 → 3d6+5
1 → 1d6+1   5 → 2d6+1   9 → 3d6+1   13 → 3d6+7
2 → 1d6+2   6 → 2d6+2  10 → 3d6+2   14 → 3d6+9
3 → 1d6+3   7 → 2d6+3  11 → 3d6+3   (both capped)
```

**`scale` is the only stored component** — `diceCount()`/`value()` are derived, so an
unreachable pairing (2d6+7) simply cannot be constructed, and a negative scale clamps to
`UNARMED` (1d6+0), the bottom rung and literally what an Ataque Desarmado deals. `DamageBase.of
(dice, value)` is the *authoring* factory and validates the pre-overflow region only (1..3 dice,
0..3 value, `INVALID_DAMAGE_BASE` otherwise) — rows past 3d6+3 are only ever reached by scaling
up, never authored. Same "genuine system boundary" validation as `SkillRoll`'s dice.

**`DamageBaseService#getDamageBase`** is the whole calculation, and comes in **two overloads
taking two different inputs, not a cascading pair**: `getDamageBase(Character, Weapon)` for a
swing (starting row *and* attacking Perícia both read off the weapon, via
`Weapon#getSkillType()`) and `getDamageBase(Character, SkillType)` for an Ataque Desarmado
(starts at `UNARMED`, and needs the Perícia named because there's no weapon to name it).
Neither delegates to the other — same "two different questions" split as
`ActionPointsService`'s `Character`/`CombatantSheet` pair, not the optional-input cascade. The
starting row is then advanced by three summed sources —

| source | hook |
| --- | --- |
| `character.getFeats()` | `Feat#resolveDamageBaseIncrease(Character)` |
| `SkillCompetencyAbility.allFor(character)` | `resolveDamageBaseIncrease(SkillType, Character)` |
| **only the attacking Perícia's** unlocked tiers | `SkillExcellency#resolveDamageBaseIncrease()` |

Three things about that scan differ from this codebase's usual three-source shape, each
deliberately:

- **The Excelência source is scoped to the attacking Perícia alone**, not every trained one —
  `AtaqueADistanciaExcellency.FOCADO` must not raise a Corpo-a-Corpo swing's Dano Base. That
  scoping is also why its hook needs no `SkillType` parameter, where the competency one does.
- **The competency source is *not* pre-filtered by the ability's own `getSkillType()`.** An
  ability may raise a Perícia other than its own — `ArtesAprimorarComArteAbility` is an *Artes*
  ability raising the Dano Base of whichever Perícia de Ataque its holder chose. Each override
  checks `attackingSkillType` itself, same shape as `resolveCriticalMarginIncrease`.
- **`AttributeAbility`/`EgoAdvantage` are not scanned and carry no hook** — no constant on
  either grants Dano Base today.

**The weapon is passed, never looked up.** A character may carry several; only the caller knows
which one is swinging. Nothing checks it's equipped — but its *type* is checked, for free: the
parameter is a `Weapon`, so "what does this shield hit for" is unaskable rather than
answered with a stand-in. Both `AbstractWeapon#damageBase` and `#skillType` are `@NonNull` for
the same reason — a weapon that deals bare-fist dano says so with `DamageBase.UNARMED`, it
doesn't leave the field empty.

**The attacking Perícia is a column of the weapon, not an argument beside it.** There used to be
a third parameter, and it let a caller pair a machado with Ataque à Distância — expressible and
meaningless, and it made every call site restate something the catalog entry already knows. A
weapon usable with *either* Perícia (a lança thrown rather than thrust) can't be expressed by one
column and would need a per-swing choice; no catalog entry needs one yet, so don't build it. The
one caller that still names a `SkillType` is the unarmed overload, which genuinely has nothing to
read it off — an Ataque Desarmado isn't assumed to be Corpo a Corpo, since `ARTISTA_MARCIAL`-style
grants and Armas Naturais both reach it.

### `BRUTALIDADE` is the reference, and needs no threshold trigger

`AtaqueCorpoACorpoCompetencyAbility.BRUTALIDADE` has all three of its tiers real: a flat +1
dano bonus below 5 Graduações, converting to +1 Dano Base at 5, becoming +2 at 10. **"Convertido"
is exclusive** — `resolveDamageBonus` returns empty from 5 on, so the two halves are never held
at once.

There is deliberately **no** "graduation crossed a threshold" trigger and nothing to migrate,
because neither half is ever *stored*: the bonus is resolved per dano roll and the increase per
attack, both reading the holder's Graduação live, so raising it changes the answer on the next
call by itself. Same recompute-on-demand discipline as `HitPointsService#getStatus` and
`InitiativeEntry#getEffectiveInitiativeValue`. Don't reach for a trigger mechanism when the
value can just be asked for.

Its flat-bonus half is what widened **`SkillCompetencyAbility#resolveDamageBonus`** to a
4-arg overload — `(SkillType attackingSkillType, SceneContext, CombatantSheet attackTarget,
Character actor)` — with the original 2-arg form delegating down with `null`s, per the usual
cascading convention. `actor` is the *roller*, passed because the bonus depends on the holder's
own live state (the same reason `EgoAdvantage#resolveSkillSpecificRollBonus` takes one).
`FRIEZA` moved its override onto the 4-arg form and reads neither new parameter — note it also
still doesn't check `attackingSkillType`, so an Ataque à Distância ability's dano bonus applies
to a Corpo-a-Corpo swing; that's pre-existing, not something the widening introduced.

## Damage mitigation — `org.aventyrs.core.character.services.DamageService`

Three layers of mitigation, in a fixed order:

1. **RD (Redução de Dano)** and **RA (Redução Absoluta)** — two independent flat reductions,
   each summed via the standard three-source scan and floored at 0. The only difference:
   `calculateFinalDamage`'s `ignoreDamageReduction` flag skips RD, never RA.
2. **Half damage** — applied *last*, after RD/RA, via the `halfDamage` flag. Rounds down.
3. **Shield points** — absorbed inside `CharacterSheet#applyDamage` itself, after
   `DamageService` computed the post-mitigation amount.

`applyDamage(CharacterSheet, int rawDamage, boolean ignoreDamageReduction)` bridges the two.
It takes **no** separate `Character` — applying damage always needs a concrete sheet to
mutate, so `getCharacter()` always suffices (unlike `RestService.applyRest`, which genuinely
needs both — see the Attribute/Graduação section).

An ability granting RD *or* RA without a number in its rules text uses
`DamageService.DEFAULT_DAMAGE_REDUCTION` (+2); only deviate when the text states one (e.g.
`APRIMORAR_COM_ARTE`'s "+1 RDS").

RD being real doesn't make every RD-granting ability real — `APRIMORAR_COM_ARTE` grants it as
one branch of a choice, `ProfissaoCompetencyAbility.FORJA_VULCANA` as a per-item choice still
blocked on the missing owned-item copy. Check what's *actually* blocking an ability.

### `CharacterStatus` is derived, never stored

There is **no `status` field on `Character`** and no `updateStatus` mutator. A character's tier
is `HitPointsService#getStatus(CombatantSheet)`, resolved fresh on every call from the damage
currently on the sheet: `getMaxHitPoints(sheet.getCharacter()) - sheet.getDamageTaken()`, handed
to the pure `getStatus(int, int)`.

That subtraction is **unclamped**, deliberately not `getCurrentHitPoints` (which floors at 0) —
the negative range is exactly what distinguishes `FALLEN`/`COMMA`/`DEAD`, so clamping would make
those three tiers unreachable. `HitPointsServiceTest` guards this directly.

It lives on `HitPointsService` rather than on `CombatantSheet`, which holds both halves of the
input and would read more naturally, because **`org.aventyrs.core.sheet` must not depend on
`org.aventyrs.core.character.services`** and resolving a maximum needs `getMaxHitPoints`'s
Vigor/Life-Multiplier scan. Its shape sibling is `getCurrentHitPoints(Character, CombatantSheet)`
— not the cascading-overload convention, which delegates the other way.

**Why it isn't stored.** A stored copy needs every path that changes Hit Points to remember to
refresh it, and most of them have no service in scope to refresh through: `Bleeding`/`Withering`
tick inside `tickTemporaryEffects`, `Sangramento` damages the sheet directly, `RealExecution`
applies curse damage, and `CombatantSheet#heal`/`RestService#applyRest` recover it. Before 0.0.18
the field was stale on six of the eight Hit-Point-mutation paths in `src/main` — a bleeding
character could cross every tier down to `DEAD` while it sat at whatever the last mitigated hit
wrote. `applyDamage` no longer mutates the `Character` as a side effect at all.

This is the same recompute-on-demand discipline as "Iniciativa can change mid-Scene" below: ask
the data you already hold, rather than pushing a value at whoever might care.

A consumer may still **persist** a tier of its own alongside its stored damage (aventyrs-api
does, so a client can badge a token whose full sheet it hasn't hydrated). That is a boundary
denormalization of a value the consumer chose to store, with no core field behind it.

### Ally-facing passive grants are scanned, not granted

An ability whose rules text buffs *someone else* continuously — Santo's Bastião dos Necessitados,
"Aliados adjacentes, apenas aqueles com menos PV que você, recebem RA" — is resolved by
**scanning**, never by handing the recipient a `TemporaryBonus`.

`AventyrTitleAbility#resolveAllyAbsoluteDamageReduction(SceneContext, boolean allyHasLowerPv)` is
the hook; `DamageServiceImpl#sumAllyGrantedAbsoluteDamageReduction` is the scan. It runs in the
opposite direction from every other source in `computeTotalAbsoluteDamageReduction`: those all
start from the target's own traits, this one walks `sceneContext.getAlliesWithin(ADJACENTE)` and
asks each neighbour what it grants outward. The `boolean` is the same PV comparison
`resolveAbsoluteDamageReduction`'s `hasLowerPvAdjacentAlly` carries for the self-facing half,
in the opposite direction; `DamageServiceImpl` is the only caller with a `HitPointsService` in
hand, so it resolves both and passes them in.

**Why not a grant.** `TemporaryBonus` is a snapshot. Granting one on "an ally came adjacent"
creates a revocation obligation — moved away, died, left the Scene, grantor died — and something
would have to notice each of those to revoke it. It also creates a persistence obligation for a
value that is pure derivation. Scanned at the moment the recipient's damage is actually
calculated, the answer is correct by construction as characters move in and out of range:
nothing to grant, nothing to revoke, nothing to persist.
`BastiaoDosNecessitadosTest#theGrantIsWithheldOnceTheAllyIsNoLongerAdjacent` pins this — no
revocation runs anywhere in it; only the `SceneContext` differs between the two assertions.

The second consumer that would justify generalizing this shape is Santo's Despertar (the same
thing for Defesas), which additionally needs `Santo#getDefesasBonus(SceneContext)` promoted from
the concrete class to the interface before any scan can reach it. Build it when that lands, not
before.

### RA/half-damage conditioned on `SceneContext`

`getTotalAbsoluteDamageReduction`/`calculateFinalDamage`/`applyDamage` each have a
`SceneContext`-accepting overload (`Character`-only ones delegate down with `null`), and
`EgoAdvantage` has two matching default-empty hooks —
`resolveAbsoluteDamageReduction(SceneContext)` (summed alongside the reflection-based
`ABSOLUTE_DAMAGE_REDUCTION` scan) and `resolveHalfDamage(SceneContext)` (a boolean, ORed with
the `HALF_DAMAGE` scan being `> 0`). `InitiativeAdvantage#TORRE_EM_MOVIMENTO` overrides both.
Reached end-to-end via `DamageInteraction`'s own matching overloads.

`getTotalAbsoluteDamageReduction(CharacterSheet target, SceneContext)` sums a **third** source:
every held `AventyrTitleAbility`'s
`resolveAbsoluteDamageReduction(SceneContext, boolean hasLowerPvAdjacentAlly)` (see
`SantoAbility#BASTIAO_DOS_NECESSITADOS`). That boolean is a PV comparison neither
`SceneContext` nor a no-arg `@Modifier` can resolve, so `DamageServiceImpl` — the only caller
holding a `HitPointsService` — resolves it once via `getAlliesWithin(Range.ADJACENTE)` and
passes it in. It takes a `CharacterSheet` directly, never a redundant `Character` alongside it.

There is deliberately **no** sheet-less `(Character, SceneContext)` public overload — every
real caller has a sheet by then (an attack always has a target). The sheet-less
`calculateFinalDamage` overload uses the private
`computeTotalAbsoluteDamageReduction(Character, CharacterSheet target, SceneContext)` helper
instead (`target` nullable only internally, so `EgoAdvantage`'s contribution still applies
sheet-less, the Título-ability one never does).

## Acquisition-time ability choices

Some abilities make the player pick a value when acquired. **Three** patterns, chosen by what
consumes the choice and whether the choice space is open-ended:

**1. The choice feeds the ability's own `@Modifier` methods** → an *instance-based class*, not
`AcquiredChoice`. `ArtesCompetencyAbility.APRIMORAR_COM_ARTE` is the reference: the enum
constant stays the catalog entry, and characters are granted an
`ArtesAprimorarComArteAbility(chosenSkill)` in `skillCompetencyAbilities`. `ModifierResolver`
invokes `@Modifier` methods *on the source instance* (caching reflection per class, not per
instance), so a modifier method can branch on the instance's own choice field and every
existing three-source scan picks it up with **zero** service changes.

Mirror that class's shape: name it `<Skill><AbilityName>Ability` in the same package as the
catalog enum; mark the choice field `@NonNull`; delegate `getSkillType()`/`getDescription()` to
the constant; return 0 from each `@Modifier` method when the choice doesn't select its branch;
keep the enum constant with a comment redirecting to the class; put TODOs on the *class*, not
the constant. Use `SkillType.isAttackSkill()` for a branch keyed on "Perícias de Ataque".

**Not every branch fits `@Modifier`** — this ability's three split exactly on that line.
`damageReduction()` is unconditionally active once chosen, so a no-arg `@Modifier` works.
`getBaseDamageBonus(SkillType)` and `getCriticalMarginReduction(SkillType)` are each scoped to
one *dynamically-chosen* Perícia, which a no-arg method can't see — model those as plain public
instance methods taking the `SkillType` explicitly, returning 0 on no match. Real, tested, with
no automatic caller yet. Don't force them into `@Modifier` for consistency: an unconditional
version would grant the bonus to every Perícia's roll.

**2. Consumed elsewhere, and the choice space is genuinely open-ended** → `AcquiredChoice<C>`,
pairing the ability instance with the chosen value. `Character.abilityChoices` holds these
*alongside* the normal ability lists — the ability is still granted the usual way; this is only
the "what did they pick" data. Read back via `AbilityChoiceService.getChoiceFor`. This solves
*persisting* the choice only; the consuming mechanism is separate work. **No ability uses this
for real yet** — built ahead of a first consumer.

**3. Consumed elsewhere, but the choice space is small and fixed at compile time** → **one enum
constant per legal option**, each implementing the ability interface. `GnoseAbility
.PERITO_TEORICO`'s reference is `org.aventyrs.core.ability.PeritoTeoricoAbility`, one
`AttributeAbility` constant per `SkillType`; granting the matching constant records both the
ability and the choice in one object, with no persistence step and no lookup at the consuming
end. Costs more code up front (a constant per option, and a new one whenever `SkillType`
grows), buys compile-time enumerability and zero runtime bookkeeping. Prefer this over
`AcquiredChoice` whenever the choice is "pick one of a small, already-fixed set."

Don't build a service to validate that a choice is *legal* — just record what was picked.

## Unconditional Perícia base-Attribute substitution — `SkillCompetencyAbility.getSubstituteAttributeDomain()`

"Lets this Perícia use Attribute X instead of its normal base Attribute" is another common
TODO reason across ability enums (`AtaqueCorpoACorpoCompetencyAbility.ACUIDADE`,
`AtaqueADistanciaCompetencyAbility.DISPARO_ARCANO`, `AtletismoCompetencyAbility.ACROBATA`,
`AttentionCompetencyAbility.ALMA_DE_SHERLOCK`'s substitution half, `DominioDoManaCompetencyAbility
.MAGIA_SELVAGEM`, `EmpatiaSelvagemCompetencyAbility.ACADEMICO_SELVAGEM`/`INSTINTO_ANIMAL`,
`FurtividadeCompetencyAbility.LADINO_TEORICO`, `PersuasaoCompetencyAbility.FORCA_OPRESSORA`).
It's mechanically real in **two** shapes, and which one a constant uses is decided by whether
its rules text attaches a circumstance:

| | hook | reference |
| --- | --- | --- |
| unconditional | `getSubstituteAttributeDomain()` (no args) | `ACUIDADE` |
| scoped to how the attack is delivered | `resolveSubstituteAttributeDomain(AttackSource)` | `ARREMESSO_PODEROSO` |

A constant picks exactly one. Overriding the unconditional hook for a scoped clause substitutes
on every roll, including the ones the clause excludes; the scoped hook **defaults to the
unconditional one**, so every unconditional overrider is untouched by its existence and
`resolveAttributeDomain` still has a single call site. Note that inverts the usual
cascading-overload direction (short delegates *down* to long) — it's a defaulting relationship,
not a cascade, and is worth remembering before "fixing" it.

- `SkillCompetencyAbility` carries a `default Optional<AttributeDomain>
  getSubstituteAttributeDomain()` returning `Optional.empty()`, mirroring
  `getDifficultyReduction()`'s existing default-method-plus-override shape. Only override it
  on a constant whose rules text grants the substitution unconditionally, per-constant (an
  enum-constant body, exactly like `AtaqueCorpoACorpoExcellency.PRODIGIO`'s
  `getDifficultyReduction()` override) — never at the enum type level, which would force
  every other constant in that same enum to implement it too.
- `CharacterSkillService.getValueForRoll` has a second overload taking an extra
  `AttributeDomain substituteAttributeDomain` parameter — `null` means "use the Skill's own
  `getAttributeDomain()`, as before" (the original 3-arg overload just delegates to this one
  with `null`), non-null overrides it. The service itself never scans abilities — it only
  ever receives a resolved value.
- Resolving *which* Attribute (if any) applies is `SkillCompetencyAbility
  .resolveAttributeDomain(skillCompetencyAbilities, skillType, defaultDomain[, AttackSource])`'s
  job — a static method on the interface, mirroring `SkillExcellency.unlockedBy`'s existing
  static-method-on-interface shape. It filters for entries whose `getSkillType()` matches
  `skillType` and whose `resolveSubstituteAttributeDomain(attackSource)` is present, returning
  the first match's Attribute or `defaultDomain` if none — byte-for-byte identical at every call
  site (only `skillType`/`defaultDomain` vary), so it's a single shared method rather than
  duplicated. **First match wins and the rules name no precedence** when a character holds two
  substituting abilities for one Perícia (Ataque Corpo-a-Corpo's `ACUIDADE`/`SAGACIDADE_ARCANA`
  are that pair today), so none is invented. It's called unconditionally by
  `AbstractSkillInteraction.applyTo` for every skill — safe even for skills with no substituting
  ability, since it just falls through to `defaultDomain`.
- **The Graduação cap deliberately calls the 3-arg form**, passing no `AttackSource`, so only
  *unconditional* substitutions widen it. `SkillGraduationService` asks which Attribute
  currently **governs** the Perícia; a delivery-scoped substitution governs only some of its
  rolls. `ACUIDADE` widens the cap, `ARREMESSO_PODEROSO` doesn't — that asymmetry is the point,
  not an oversight, and `SkillGraduationServiceImplTest` pins both directions.
- `GnoseAbility.PERITO_TEORICO` is a different shape
  entirely: which Attribute to substitute is fixed (Gnose), but *which Perícia* it applies to
  is a per-character choice, not a fixed one enum-constant-to-enum-constant mapping like
  `ACUIDADE`'s — but unlike a truly open-ended choice, every legal option is already known at
  compile time (every `SkillType`), so it's wired via `org.aventyrs.core.ability
  .PeritoTeoricoAbility` — one `AttributeAbility` constant *per* `SkillType`, rather than the
  third pattern the previous section describes for an open-ended choice (`AcquiredChoice`).
  Granting a character `PeritoTeoricoAbility.FURTIVIDADE` both records that they acquired
  Perito Teórico *and* which Perícia they chose, in one object. Its own static
  `resolveAttributeDomain(Collection<AttributeAbility>, SkillType, AttributeDomain)` mirrors
  this section's own `SkillCompetencyAbility.resolveAttributeDomain` shape, and both
  `AbstractSkillInteraction.applyTo` and `SkillGraduationServiceImpl.getMaxGraduation` consult
  it first, feeding its result in as this section's own `resolveAttributeDomain`'s
  `defaultDomain` parameter — so a `SkillCompetencyAbility` substitution still wins if one
  somehow also targets the same Perícia, PERITO_TEORICO's Gnose applies otherwise, and the
  Perícia's own natural Attribute applies if neither does. Both call sites needed no
  constructor/DI changes for this — it's a pure function over `character.getAttributeAbilities()`,
  already in hand at both.
- **`AttackSource` (`org.aventyrs.core.skill`) is the delivery channel, and it's an interface
  `Weapon` and `Spell` implement directly** — there is no wrapper type standing between them and
  a roll, and don't reintroduce one. Its single member is `getAttackSkillType()`, which is not a
  new column: `Weapon#getSkillType()` (delegated to by a `default`, so the two can't drift) and
  `Spell#getAttackSkillType()` both already carried it, and naming the concept they share is what
  keeps this from being a marker interface. Only `Weapon` extends it, never `Item` — same
  enforcement-by-type as keeping `getDamageBase()` off a pauldron.
- **A hook narrows by `instanceof`, not by asking the interface.** There is no `isThrown()`/
  `isWeapon()`: which `ItemCategory` values count as "arremesso" is one clause's reading and the
  next clause's would differ, so the test lives on the constant — `ARREMESSO_PODEROSO` is
  `instanceof Spell || (instanceof Weapon w && w.getCategory() == THROWABLE)`, the same way
  `FRIEZA` holds both the amount and the `Range` condition of its own bonus rather than pushing
  either onto `SceneContext`.
- **`null` means the caller didn't say**, and an `instanceof` chain reads that as "no scope
  matched" for free — no null branch needed. It does *not* mean "unarmed": an Ataque Desarmado
  has no representation, deliberately, because nothing consumes one yet and a constant for it
  would have to pick an `getAttackSkillType()` that an Ataque Desarmado has no fixed answer for.
- It reaches the roll as the **fifth and last** `applyTo` parameter, and that placement is
  forced, not stylistic: the resolved `AttributeDomain` feeds `getValueForRoll`, both
  `sumAttributeDomain*` scans, and — decisively — `consumeFirstRollThisTurn(domain)`, which is
  *stateful*. A domain resolved after the fact can't un-consume a Turn's first roll, so the
  usual "layer it onto the result" trick (which is exactly how the `attackTarget` half still
  works, see `applyAttackTargetBonuses`) is unavailable here. Being a parameter rather than a
  field on `SkillRoll` also keeps the substitution visible on `AttackDelivery`'s bonuses-only
  preview path, where no dice have been rolled yet. Callers pass the `Weapon`/`Spell` itself —
  `.attackSource(ADAGA_DE_ARREMESSO)`, not a wrapper around it.
- **Because the logic moved to the 5-arg overload, a subclass must override *that* one.**
  `ArtesInteraction` was moved from the 3-arg accordingly, even though Artes is not a Perícia de
  Ataque and reads neither new parameter — an override left on a shorter overload is silently
  skipped by any caller using a longer one. `EsquivaEApararInteraction`'s own 4-arg
  `(..., DefenseType)` is a separate signature rather than an override and needed no change.
- Reachable end-to-end from `DeliveredAttack#attackSource` (via `AttackDelivery`) and
  `SkillRollRequest#attackSource` (via `SkillInteractionFactory`), both optional. **`SpellCasting
  Service` is the one path it does *not* reach**: `castSpell` takes an already-built delivery
  `Interaction` and runs it through `receiveInteraction` (the 1-arg `applyTo`), so there's no
  seam — closing that needs `castSpell` to take the `Spell` itself, which the missing target-GD
  resolution will force anyway. TODO'd on the service.
- Building this mechanism doesn't retroactively finish every ability that cites it — check
  each constant's own TODO. `ACUIDADE`, `SAGACIDADE_ARCANA`, `ACROBATA`, `DISPARO_ARCANO`,
  `MAGIA_SELVAGEM` and `ARREMESSO_PODEROSO` are fully wired (enum override + `Interaction`
  filter + service overload;
  `SAGACIDADE_ARCANA` needed only the enum override, the other two pieces being generic since
  `ACUIDADE` landed — Ataque Corpo-a-Corpo is now the one Perícia with *two* substituting
  abilities, and `resolveAttributeDomain`'s first-match wins between them, since the rules name
  no precedence) — see
  `AttributeSubstitutionFeatureTest` for an end-to-end test exercising all four on one
  Character at once, including a control Perícia (Persuasão) proving none of them leaks into
  an unrelated roll. `AttentionCompetencyAbility.ALMA_DE_SHERLOCK`'s substitution half,
  `EmpatiaSelvagemCompetencyAbility.ACADEMICO_SELVAGEM`/`INSTINTO_ANIMAL`,
  `FurtividadeCompetencyAbility.LADINO_TEORICO`, and `PersuasaoCompetencyAbility
  .FORCA_OPRESSORA` still need the same three-piece wiring applied to their own constant and
  `<Skill>Interaction` before they're real, even though the mechanism they were blocked on no
  longer needs to be invented.

## Vantagem is a flat +2 bonus, not a reroll mechanic

"Grants Vantagem on X rolls" is one of the most common TODO reasons across every ability
enum, but it isn't a d20-style "roll twice, take the higher" mechanic — in this game
**Vantagem is just a flat +2 bonus to that specific roll** (`Skill.ADVANTAGE_BONUS`). So an
ability that grants Vantagem on a Perícia roll is implemented exactly like any other roll
bonus: a `@Modifier(ModifierType.SKILL_ROLL_BONUS)` method on the concrete ability/excellency
returning `Skill.ADVANTAGE_BONUS`, summed into `skillRollBonus` inside the skill's
`<Skill>Interaction.applyTo` — see `DirigirECavalgarCompetencyAbility.CONTROLAR_ANIMAIS` /
`DirigirECavalgarInteraction`. No separate flag or dice-rolling engine needed.

`AbstractSkillInteraction.applyTo` (see the "Adding a new Perícia" section/`adding-a-pericia`
skill — every `<Skill>Interaction` extends it, so this is no longer duplicated per skill) sums
`ModifierType.SKILL_ROLL_BONUS`
across the same three sources `ReactionsService` uses for Reações — `attributeAbilities`,
`skillCompetencyAbilities`, and the trained skill's own unlocked `SkillExcellency` tiers — plus
a fourth, `CharacterSheet`-level one: `target.getTemporaryBonus(ModifierType.SKILL_ROLL_BONUS)`
(see "Temporary bonuses from other Characters" below) — even before any ability actually
grants it for that specific skill, so future abilities work without touching any Interaction
at all.

If the ability's Vantagem is scoped to a specific *purpose* within the skill (e.g.
`CONTROLAR_ANIMAIS`'s is only for animal/animal-drawn-vehicle rolls, not every Dirigir e
Cavalgar roll), that narrowing can't be modeled yet — this codebase doesn't track what a roll
is *for*. Document that simplification in a comment on the enum constant rather than silently
narrowing or silently over-granting.

Don't treat a citation of one of these examples as permanent, either — `AttentionCompetencyAbility
.PERCEPCAO_DE_FOXM` used to be this section's reference example (a scoped Vantagem bonus), but
a rules revision turned it into an auto-success effect instead, which no longer fits this
pattern at all. When revising a skill, check whether anything elsewhere cites it as a
precedent and fix those citations in the same change (see the `ATAQUE_A_DISTANCIA` and
`ATENÇÃO` revisions for two examples of this).

## Consumer-facing documentation lives in `package-info.java`, and must stay current

This is a library other Java code (an API layer, or a game UI) imports directly — so
"how do I use this" docs go in `package-info.java` files next to the relevant package, not a
separate markdown guide that can silently drift out of sync. Two exist so far:

- `org.aventyrs.core.skill` — how to perform a Skill Roll via `Interaction`/`CharacterSheet`.
- `org.aventyrs.core.character.services` — the full ordered list of character-creation
  choices (Race → Attributes → Egos → conditional Vantagem de Autocontrole → ActionProfile →
  assemble via `Character.builder()`).

**Whenever a change adds a new creation-time choice** (a new Ego/Attribute-like allocation,
another permanent "pick one" enum like `ActionProfile`, a new conditional Vantagem like
Autocontrole's) **update the `character.services` package-info's numbered list and code
example in the same change** — don't leave it for later. The same applies to the `skill`
package-info if the Skill Roll protocol itself changes (new `InteractionResult` fields, a
different dispatch shape, etc.). A consumer coding against a stale list will silently miss
required or newly-available choices.

Before publishing a version other people can `javadoc`, note that `./gradlew javadoc`
currently fails on ~5 pre-existing errors — Lombok's generated `Builder` inner classes aren't
visible to the standalone javadoc tool without a delombok step. This is unrelated to the
package-info content itself (verified by running `javadoc` directly against just the relevant
sources); fixing it would need a delombok task (e.g. the `io.freefair.lombok` plugin).
