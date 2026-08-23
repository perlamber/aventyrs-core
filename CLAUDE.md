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
  `SorteAdvantage#resolveCriticalMarginIncrease`). Conversely, several pieces here *were* built
  ahead of a consumer on purpose (`ReactionsService`, `InitiativeService`, `AcquiredChoice`,
  `CharacterSheet#startTurn`); that's noted where it applies.
- **Cascading overloads.** When a computation grows a new optional input, add a longer overload
  and have every shorter one delegate down with `null`; the longest holds all the real logic. A
  subclass overrides the **longest** overload it needs, never a shorter one — virtual dispatch
  still routes the short forms to it. Used by `AbstractSkillInteraction#applyTo`
  (`CharacterSheet` → `+SceneContext` → `+SkillRoll`, plus `AtaqueADistanciaInteraction`'s 4-arg
  `+attackTarget`), `DamageService`, `DamageInteraction`, `SceneContext`, and `Scene
  #addParticipant`.
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
  A scope of specific *named* skills is trackable, and does have a hook.

## Missing systems — the gap catalog

Check here before assuming a TODO needs a new gap named. Nothing below exists in this core.

| Missing system | Notes / where cited |
| --- | --- |
| **Defesas — *mostly built*** | `DefenseService` + `DefenseType` are real, and `DEFESAS`/`PHYSICAL_DEFENSE`/`MAGIC_DEFENSE` all have readers. What's still missing is narrower: `Santo#getDefesasBonus` has no granting trigger (*when* each adjacent ally receives it), and a foe's Defesa is an authored flat number with no defined conversion from a GD reduction's *níveis*. Don't cite this as "no Defesas stat exists".
| **Owned/produced item copy** | The `Item` *catalog* is real, and so is inventory now — `Character#equipment` (worn/wielded, scanned by `DefenseService`/`DamageService`) and `AbstractCombatantSheet#inventory` (carried, including a foe's loot). Still missing: **per-copy state** (Dureza remaining, Obra-Prima tier, Aprimoramentos, who produced it), a PE economy, and production/repair. Cite the specific piece, not a blanket "no Item entity" or "no inventory". |
| **Damage-type-scoped mitigation** | `DamageType` has no Corte/Perfuração/Impacto breakdown; RD/RA are resolved with no notion of damage type. |
| **Multiplicative stages** | `MovementService` sums `MOVEMENT` additively with no halving stage (unlike `DamageService`'s real `HALF_DAMAGE`). Don't add a `MOVEMENT_HALVED` constant — the mechanism is missing, not just a reader. |
| **Temporary PA grants** | `ActionPointsServiceImpl` reads only `Character#getTemporaryActionPointsBonus()`, never `getTemporaryBonus(ModifierType.ACTION_POINTS)`, so such a `Blessing` is inert. |
| **Temporary RA grants** | `getTotalAbsoluteDamageReduction` never reads `getTemporaryBonus(ABSOLUTE_DAMAGE_REDUCTION)` — RA comes only from continuously-scanned passive hooks. |
| **Round-scoped Attribute bonuses** | `AttributeValue` has only `base`/`racialBonus`/`variable`, all permanent — never summed via `ModifierType`. |
| **Roll-resolution engine** | `SkillRoll#getCriticalResult()` is a fixed dice-matching check, not a threshold/margin comparison, so nothing consults `resolveCriticalMarginIncrease`/`getCriticalMarginReduction`. Auto-success effects have no hook either. |
| **Area de Efeito** | Cited but unbuilt — see `EsquivaEApararCompetencyAbility.EVASAO`. |
| **Malefício classification** | No Encantamento/Maldição/Doença tag exists — see `Withering`, `ABRIR_DEFESAS`. |
| **Forced attack targeting / interception** | No "another Character becomes the target instead" mid-resolution — see `SantoAbility.GUARDA_VIDAS`. |
| **Reactive/retaliation damage** | `DamageService` only computes damage *to* a target *from* an attacker, never the reverse. |
| **Forced movement / positioning** | Knockback, "empurrado 1UD", Reposicionar — this core never does geometry. |
| **Continuous cross-character passive grants** | Partly built: `AventyrTitleAbility#resolveAllyAbsoluteDamageReduction` scans a target's adjacent allies for outward RA grants (Santo's Bastião dos Necessitados). Still missing for Defesas (`Santo` Despertar — its bonus is on the concrete class, unreachable by a scan) and for `SkillCompetencyAbility` (`INSTINTO_DE_LUTHER`). See "Ally-facing passive grants are scanned, not granted". |
| **Movement-triggered Reações** | No movement-triggers-Reação mechanism, and no suppression of one. Cited by `POSICIONAMENTO_ESTRATEGICO`, `AS_NA_MANGA`. |
| **Resource-spend triggers** | Nothing observes `spendTemporaryEgoPoints` etc. An observer/event mechanism was deliberately rejected — this codebase has none anywhere. |
| **One-time roll effects bought with a resource** | Spending PV/PM to modify a single roll's outcome (e.g. a GD reduction) has no transaction — see `Orc`'s Agnação Ancestral. |
| **"This one delivered attack" scoping** | A bonus scoped to the single attack delivered by activating another ability fits no per-roll `resolve*` hook, which are all generic per skill type. |
| **Within-Turn activation counter** | `CharacterSheet` tracks Round-scoped `TemporaryEffect`s, not same-Turn activation counts. |
| **Game-session tracking** | No "1x por sessão" concept — cited by `MOTIVACAO_DE_MOSES`, `DILETO_DE_TYKHE`, `MeioElfo`. |
| **Roubo de Mana / de Determinação** | Only Roubo de Vida exists (`LifeStealService`). |
| **Terreno difícil** | `TerrainType` describes a whole Scene, not a per-movement cost to ignore. |
| **Item numeric columns** | PE has no economy, Dureza no damage/repair mechanic, Conjuração no item-granted hook on either `SpellCastingService` roll. |
| **Acquisition-slot grants** | "Grants an extra acquisition slot" traits (`Elfos`' Origem Mística, `Anao`' Pequenos Gigantes) have no shape. |
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
Especializações and their gated abilities — both package the full file/test checklist. The
invariants worth knowing without doing Título work:

- **Catalog vs. instance**: `AventyrTitle` is the per-character *held instance* (same shape as
  `MoralHerdadaAbility`/`ArtesAprimorarComArteAbility`), **not** a stateless-per-family class
  like `Race` — held specializations/abilities are genuinely per-acquisition data. "Which
  Título family this is" is answered by which concrete class implements `AventyrTitle`,
  deliberately not a separate identity enum.
- **Three slots, not a list**: `Character` holds plain nullable `primaryTitle`/`secondaryTitle`/
  `tertiaryTitle` fields, keyed by `TitleSlot` (`org.aventyrs.core.character`, alongside
  `Character` — mirroring `EgoDomain`'s placement). **Whether an instance is the holder's
  Título Primário is not a method on `AventyrTitle`** — it's a fact about *which slot* holds it,
  so at most one can ever be primary by construction. A caller resolves
  `Character#getPrimaryTitle() == title` externally and passes it in where needed.
  `Character#grantTitle(AventyrTitle, TitleSlot)` sets the field directly, overwriting that
  slot — acquiring a Título costs no XP and needs no `CharacterSheet`, so unlike
  `upgradeBase`/`upgradeGraduation` there's no service to route through.
  `Character#getAllTitles()` is the derived list (Primário first, empty slots omitted) a
  scanning service uses. `CharacterFixture` sets all three to `null`.
- **Subpackage per Título**: `org.aventyrs.core.title.<titlename>` (lowercase, no separators —
  e.g. `org.aventyrs.core.title.santo`), mirroring `org.aventyrs.core.skill.<skillname>`. Only
  the shared framework interfaces stay directly in `org.aventyrs.core.title`.
- **Exactly two Especializações per Título** — no more, no less; a character may hold both,
  one, or neither. The enum may start with fewer than two constants if the rules text for one
  wasn't supplied yet; don't invent content.
- **A Título trait with a real activation cost is an Active Ability**, whether cataloged as an
  Especialização or a Habilidade/Suprema: `AventyrTitleSpecialization extends
  AventyrTitleAbility`, so every specialization constant satisfies the ability interface too.
  `AventyrTitle#getAllAbilities()` combines both catalogs into one `List<AventyrTitleAbility>`
  for a scanning service (`DamageServiceImpl`'s RA scan uses it).
  - `isPassive()` is **derived**, not stored: `getActionPointCost() == 0 &&
    !isReactionActivation() && !isFreeActionActivation()`. A Reação or Ação Livre still counts
    as active. The formula can't distinguish "no cost" from "a real cost expressed some other
    way" — `SantoSpecialization.ABRACADO_PELA_ESCURIDAO`'s cost is entirely PV-based, so it
    overrides `isPassive()` to `false` explicitly. Follow that pattern for any similar trait.
  - **One `<X>Interaction` per Active Ability**, built the moment — and only the moment — at
    least one clause is mechanically real. Passive abilities never get one. The entry point is
    always a validating method on the concrete class (`Santo#activate<X>`), never a call
    against the bare catalog constant: the *instance* is what knows which traits it holds, so
    it mirrors `validateRequestedTrait`'s "must actually be held" check
    (`IllegalOperationException`/`REQUIRED_TITLE_TRAIT_NOT_HELD`) before delegating.
  - Two real shapes exist and they're genuinely different — don't reuse one without checking
    the target shape matches: `AbencoadoPelaLuzInteraction` (one touched target, direct
    mutation) vs. `GritoDeGuerraVulcanoInteraction` (report-only `Blessing` list the caller
    applies). Extract a shared base class only once a *third* needs an identical cascade.
- **`getInteractionClass()`** (`Optional<Class<? extends Interaction>>`, no default — every
  constant answers explicitly) declares the bond between a constant and the Interaction that
  activates it, so the two can't silently drift. This is a **declared** bond only — nothing
  dispatches reflectively off it; don't build that speculatively.
- **"Requer N Especializações/Habilidades" prerequisites are real, enforced data** — one of
  only two exceptions in this codebase to the usual "leave prerequisites as an unenforced
  comment" restraint (the other is `Feat`). `getRequiredSpecializations()`/
  `getRequiredSpecialization()`/`getRequiredOtherAbilities()` carry the numbers, `isEligible
  (AventyrTitle)` combines them, and `TitleAbilityService#grantTitleAbility` checks it,
  throwing `TITLE_ABILITY_PREREQUISITE_NOT_MET`. "N outras Habilidades" is scoped to sibling
  constants of the same concrete catalog via `Enum#getDeclaringClass()` — **not** plain
  `getClass()`, which a constant-specific body would report differently.
  The Suprema-per-combination cap is softer: `getAvailableSupremaSlots` reports how many more
  a Título may receive (the normal 1, plus one while `InstinctAbility#CENTELHA_SUPERIOR`'s
  one-time grant is unspent) and `grantTitleAbility` enforces it on that one entry point, but
  constructing an `AventyrTitle` directly with more is still unchecked.
- A Habilidade/Suprema gated on one *specific* Especialização lives in its own
  `<Specialization>Ability` enum, not the flat `<Title>Ability` one. Because both are held by
  the same character, the concrete class's abilities field must be typed
  `List<AventyrTitleAbility>`, never `List<<Title>Ability>` — a `<Specialization>Ability`
  constant is a different Java enum and could never fit the narrower type.

**Keep `org.aventyrs.core.title/package-info.java` current** whenever the granting API changes
shape — same discipline as `character.services`' own package-info.
## Adding a new Talento (Feat) — `org.aventyrs.core.feat`

A Talento (e.g. `ArtesMarciaisFeat.ARTISTA_MARCIAL`) is a flat catalog entry, one enum per
Talento *tree* named by `FeatCategory` (e.g. `ArtesMarciaisFeat` for `FeatCategory
.ARTE_MARCIAL`) — mirrors `<Skill>CompetencyAbility`'s one-enum-per-domain shape, not
`AventyrTitle`'s per-instance-class one, since a Talento (unlike a Título) never carries
per-acquisition player choices of its own today. `Feat`/`FeatRequirements`/`AbstractFeat`/
`FeatCategory` and `FeatService#grantFeat(Character, CharacterSheet, Feat)` (validate
`Feat#isEligible`, spend `Race#getNewFeatCost` XP, then mutate — the same shape as
`CharacterAttributeService#upgradeBase`/`TitleAbilityService#grantTitleAbility`) are the whole
mechanism; the `adding-a-feat` Claude Code skill packages the full checklist as an invokable
walkthrough.

Two facts worth knowing outside that checklist:

- **A Talento's prerequisites are real, enforced data** — the second exception (alongside
  `AventyrTitleAbility`'s "Requer N Especializações/Habilidades") to this codebase's usual
  "leave 'Requer N Graduações' as an unenforced comment" restraint, because a Talento's own
  Pré-requisito is always a simple numeric/identity threshold `FeatRequirements` can model
  directly.
- **`Character#feats` is a real *mutable* `List<Feat>`** — unlike `skillCompetencyAbilities`/
  `attributeAbilities`'s `@Singular`, fixed-at-creation-only shape, since a Talento is acquired
  well after a character exists; `Character#grantFeat(Feat)` is a plain mutator (like
  `#grantTitle`) that `FeatService` calls after spending XP. It defaults to a fresh `new
  ArrayList<>()` per `Character.builder().build()` call, but `CharacterFixture` defaults it to
  the same immutable `List.of()` every other trait list there uses — a test granting a Feat
  onto a fixture-built Character must first swap in a mutable list via `.toBuilder().feats(new
  ArrayList<>()).build()`.

## Itens/Equipamento — `org.aventyrs.core.item`

An `Item` is the **catalog entry** for a piece of Equipamento — what "an Armadura Completa" is,
the same way `Feat` describes a Talento — carrying every column an item's rules-text block
lists: its heading (`ItemWeightClass`/`ItemRarity`, the "(Pesado/Raro)" pair), `description`,
`price` (in **Pontos de Equipamento**, the PE `ResourcesAdvantage#BARGANHISTA` already cites),
`physicalDefenseBonus`/`magicDefenseBonus` (DF/DM), `hardness` (Dureza), `castingBonus`
(Conjuração), and an `ItemFavor`.

- **Catalog, not owned copy** — the same split `AventyrTitle`'s javadoc documents, resolved the
  *other* way than a Título's: an item's stats are identical for every copy, so the enum
  constant *is* the item. Per-copy state (Dureza actually remaining, Obra-Prima tier,
  Aprimoramentos, who produced it) is deliberately unmodeled, and would be a separate
  held-instance type wrapping a catalog entry. Nothing on `Character`/`CharacterSheet` holds an
  `Item` yet either — that inventory belongs with the per-copy type, not this one. Don't build
  it speculatively; several TODOs cite it (`ProfissaoCompetencyAbility`,
  `ResourcesAdvantage#HERANCA_FAMILIAR`), but none is unblocked by the catalog alone.
- **One enum per `ItemCategory`** (e.g. `ArmorItem` for `ItemCategory.ARMOR`), mirroring
  `<Skill>CompetencyAbility`/`ArtesMarciaisFeat`'s one-enum-per-domain shape;
  `AbstractItem` (`@Builder`) is the `AbstractFeat` equivalent for a one-off or
  caller-supplied item that doesn't belong in a catalog enum.
- **`ItemFavor` is the conditional half, and its bonuses are real data, not prose**: a Favor
  grants a bonus of any sort, exactly like a `SkillCompetencyAbility` does, so it carries a
  list of `ItemBonus` (a `ModifierType` + value pair) resolved via `ItemFavor#resolveBonus
  (ModifierType, Character)` / `Item#resolveFavorBonus(...)` — 0 unless the `ItemRequirements`
  (an `AttributeDomain` + value, e.g. "Força 3") are met. `ARMADURA_COMPLETA`'s "Dano de Corte
  sofrido é reduzido em -2" is `DAMAGE_REDUCTION` 2, landing on the RD `DamageService
  #getTotalDamageReduction` already sums for real. The rules text stays on `getDescription()`
  alongside it, same single-source-of-truth convention every ability enum follows.
  - It's **data, not `@Modifier` methods**, unlike every ability enum, and that's forced:
    `@Modifier`'s `ModifierType` is a compile-time-fixed annotation value, so one shared
    `ItemFavor` class can't vary which type a given item grants — the same limitation "A
    ModifierType per skill" documents. Don't try to route items through `ModifierResolver`.
  - `ItemBonus` is deliberately **not** `TemporaryBonus` or `Blessing`: an item's Favor lasts
    as long as the item is carried (no Rodada countdown) and never reaches anyone but its
    wielder (no `TargetScope`, no granting `source`), so all three of those fields would be
    dead weight.
  - A Favor clause with no `ModifierType` to express it yet contributes no `ItemBonus` and
    lives on in `getDescription()` until its mechanism exists. `ARMADURA_COMPLETA`'s own
    "de Corte" scoping is exactly that kind of simplification — no damage-type-scoped
    mitigation exists (`DamageType` has no Corte/Perfuração/Impacto breakdown, and RD/RA are
    resolved with no notion of damage type), so it's modeled as plain RD, documented on the
    constant rather than silently narrowed or over-granted. So is a clause whose *shape*
    `ItemBonus` can't hold even though a `ModifierType` for the stat exists —
    `ARMADURA_DE_JUSTA`'s "Movimento Base reduzido à metade" is a halving, and
    `MovementService` sums `MOVEMENT` additively with no multiplicative stage for one to feed
    (unlike `DamageService`'s own real `HALF_DAMAGE` stage); don't add a `MOVEMENT_HALVED`
    constant for it, since the missing piece is the mechanism, not just a reader. `ROUPA_PESADA`
    is the reverse case worth knowing, though: its rules text reads at first glance like it
    needs DF and DM as two separate comparable stats ("+1 na Defesa faltante entre DF ou DM",
    plus an Efeito Adicional filling in whichever of the two the base clause didn't), but the
    two clauses combined always net out to an unconditional +1 DF *and* +1 DM regardless of the
    per-copy production choice — so despite `ModifierType.DEFESAS` being a single
    undifferentiated constant, it's granted for real, as one combined `DEFESAS` bonus of 2, with
    no "Efeitos Adicionais" line needed at all. Don't assume a Favor naming two stats always
    needs them split — check whether the rules text's *net effect* is actually unconditional
    on both first.
  - The optional "Efeitos Adicionais" line is granted by the *same* requirement, not
    independently, so it lives here rather than on `Item`; `null` for an item with none, checked
    via `hasAdditionalEffects()`. It stays free text — what one does varies too widely per item
    to have a shared shape yet. No cataloged item currently has one.
  - Everything else on `Item` applies to anyone carrying it; everything on `ItemFavor` needs
    `Item#grantsFavorTo(Character)` to hold first.
  - A requirement isn't always Força — `ROBE_CERIMONIAL`/`ROBE_DE_GUERRA` are gated on Gnose
    and `ROUPA_PESADA` on Destreza; any `AttributeDomain` is fair game.
- **`ItemRequirements` checks `getTotal()`, not `getBase()`** — deliberately unlike
  `FeatRequirements`, which uses `base`: acquiring a Talento is gated on what the character
  personally invested in, but whether an item's Favor applies is a "can I meet this right now"
  question, so a Bônus Racial or a variable bonus counts. It's a narrower record than
  `FeatRequirements` (no `requiredSkillType`/`requiredFeat`) rather than a reuse of it — widen
  it only if a real item ever names a Perícia/Talento/Título.
- **`castingBonus` is a plain number, and "Desvantagem" is `Skill.DISADVANTAGE_MALUS` (-2)** —
  the symmetric counterpart to `Skill.ADVANTAGE_BONUS` this codebase's gap catalog used to
  list as missing. `ArmorItem.ARMADURA_COMPLETA`'s Conjuração column is the first
  real, unconditional Desvantagem in the ruleset and what motivated adding it. The *scoped*
  Desvantagem clauses elsewhere (`race/Bestial.java`'s Inocência Selvagem,
  `AbencoadoPelaLuzAbility`) are still blocked on their own separate gaps — this core doesn't
  track what a roll is *for* — not on the constant.
- **What's missing for a Favor is only the inventory**: no `Character`/`CharacterSheet` holds
  an `Item`, so no scanning service reaches one on its own — `DamageServiceImpl`'s RD scan
  covers `attributeAbilities`/`skillCompetencyAbilities`/excellencies and would need equipped
  items added as a fourth source. Until then a caller holding the item asks it directly. The
  *bonus* itself is already real; don't re-cite it as unexpressible.
- **None of the numeric *columns* has a consumer yet** (DF/DM/Dureza/Conjuração/Preço, as
  opposed to the Favor above), and each is blocked on a *different*
  missing system — PE has no budget/economy, DF/DM have no Defesas stat (`ModifierType.DEFESAS`
  is a real registry entry nothing reads), Dureza has no damage/repair mechanic, Conjuração has
  no item-granted hook on either of `SpellCastingService`'s two rolls. The values are real,
  exact data all the same, per the "can't apply it yet doesn't mean can't compute it yet"
  discipline.
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

## Casting a Magia is two separate rolls — `org.aventyrs.core.magic.SpellCastingService`

Casting a Magia with a rolled effect always involves **two** rolls, not one: whichever
Perícia actually delivers the spell (e.g. `AtaqueADistanciaInteraction` for a ranged spell,
`AtaqueCorpoACorpoInteraction` for a Toque spell) rolled against the **target's** GD, followed
by a `DominioDoManaInteraction` roll against the **Magia's own** GD. `SpellCastingService
.castSpell(CharacterSheet, Interaction<CharacterSheet> deliveryInteraction)` orchestrates
this: it rolls the given delivery Interaction, then rolls Domínio do Mana, and returns both
`InteractionResult`s in a `SpellCastingResult` — it never picks the delivery Interaction
itself (the caller does, since only the caller knows which Magia/weapon is being used).

No `Magia` entity/list exists yet, so `SpellCastingService` only computes both rolls' bonuses
— it doesn't know either roll's target GD, so it can't resolve success/failure for either
roll yet. This is deliberately left as a TODO on the service itself rather than guessed at.

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

**Use the `granting-a-blessing` skill** for the full walkthrough of adding one — including how
to tell a genuine `Blessing` case apart from the narrower bonus mechanisms elsewhere in this
file (a flat `@Modifier`, `resolveConditionalRollBonus`, `resolveDamageBonus`,
`resolveAbsoluteDamageReduction`/`resolveHalfDamage`). The architecture:

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
  automatically via `CharacterSheet#finishTurn()`, which `Scene#next()` now calls on whoever's
  turn is ending (see "Iniciativa can change mid-Scene" below).
- `Blessing` (`org.aventyrs.core.sheet`) is what an Interaction *reports* granting: a
  `ModifierType`, `int value`, `int rounds`, a `TargetScope scope`, and a `String source`.
  `modifierType` deliberately matches `TemporaryBonus`'s own field, so a caller passes it
  straight into `grantTemporaryBonus` with no mapping step.
  - `scope` is a `TargetScope` — `SINGLE_TARGET`/`ALLIES`/`ENEMIES`/`SELF`/`SELF_AND_ALLIES`.
    `ALLIES` *excludes* the holder ("a eles, mas não a você"); `SELF_AND_ALLIES` includes them.
    Who actually *receives* a blessing is not this core's concern — a caller resolves
    recipients via `Scene.getAllies`/`getEnemies`, `SceneContext.getAlliesWithin`, or its own
    target lookup, and calls `grantTemporaryBonus` on each.
  - `source` identifies the granting trait. **Prefer the granting enum's own `.name()`** over a
    string literal wherever the granting site has the constant in hand, so the two can't drift.
    A literal is right only where no such enum exists (e.g. a test double). This still tracks
    *what trait* granted a bonus, never *which Character*.
  - `InteractionResult.blessings` is a `List` — one Interaction can report several at once
    (`GritoDeGuerraVulcanoInteraction` reports three). It stays `null` when nothing is granted
    (same not-applicable convention as every other field); a caller gates on `!= null`.
    `InteractionResult` is `@Builder(toBuilder = true)` so an overriding subclass extends the
    base result rather than reassembling every field.
- `ArtesInteraction` overrides `applyTo` to report DOM_BARDICO's `Blessing` once a `SkillRoll`
  reached at least GD Médio: `SKILL_ROLL_BONUS` (its rules text is unrestricted), `ALLIES`,
  `rounds` 1/2/3 at Graduação 1/5/10, and `value` via `domBardicoBonusValue` — Médio +1,
  Difícil +2, Muito Difícil +3, Improvável +4, Milagre +5. `UNIMAGINABLE` isn't named in the
  text; it inherits Improvável's +4 — **an inference, not confirmed text**. Below Médio, or
  with no roll, `blessings` stays `null` rather than reporting a partially-filled `Blessing`.
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
## Vantagens de Ego — `Character#egoAdvantages`

A Vantagem de Ego (`AutocontroleAdvantage`, `InitiativeAdvantage`, `ResourcesAdvantage`,
`SorteAdvantage`) is chosen once at character creation, gated on that `EgoDomain`'s
creation-time `base` reaching `CharacterCreationService.EGO_ADVANTAGE_MIN_BASE` (3) — the same
threshold for every domain, checked via the single generic
`isEgoAdvantageAvailable(EgoDomain, CharacterEgos)`. Don't reintroduce a per-domain
`isXAdvantageAvailable` method/constant pair unless a domain's threshold is ever confirmed to
differ from 3.

`Character` stores every domain's choice in one `@Singular Map<EgoDomain, EgoAdvantage>
egoAdvantages` — never one nullable field per domain. A domain with no chosen Vantagem is
simply absent, never a `null` value inside. Read via `Character#getEgoAdvantage(EgoDomain)`,
set via the `@Singular`-generated `.egoAdvantage(EgoDomain, EgoAdvantage)` — never index the
map directly outside those two spots.

`EgoAdvantage` carries `default` hooks alongside `getEgoDomain()`/`getDescription()`, all
defaulting to empty, mirroring `SkillCompetencyAbility`'s `resolve*` shape (same reason: not
reflection-discoverable via a no-arg `@Modifier`), but summed across **every** skill, since a
Vantagem de Ego was never tied to one Perícia:

- `resolveConditionalRollBonus(SceneContext)` — a bonus toward *any* Perícia roll, summed by
  `AbstractSkillInteraction#sumEgoAdvantageRollBonuses` into `skillRollBonus`.
- `resolveDamageBonus(SceneContext)` — resolved by `AbstractSkillInteraction` itself whenever
  `skillType.isAttackSkill()`, first non-empty wins. Needing no `attackTarget`, it works for
  both Ataque à Distância *and* Corpo a Corpo off the plain 2-arg `applyTo` — unlike
  `SkillCompetencyAbility#resolveDamageBonus`, which is only reachable through
  `AtaqueADistanciaInteraction`'s 4-arg overload.
- `resolveSkillSpecificRollBonus(SkillType, SceneContext, CharacterSheet target)` — for a
  Vantagem scoped to specific *named* skills (e.g. `MORAL_HERDADA`'s "+1 em Artes e
  Persuasão"), which `resolveConditionalRollBonus` would over-grant into every other Perícia.
  A scope of named skills *is* trackable, unlike a narrative *purpose*. `target` is the
  roller's own sheet (not an attack target), passed because the bonus may depend on the
  roller's own live state. Summed by `sumEgoAdvantageSkillSpecificRollBonuses`.
- `resolveAbsoluteDamageReduction(SceneContext)`/`resolveHalfDamage(SceneContext)` — see
  "Damage mitigation" below.

`InitiativeAdvantage.IMPETO` and `TORRE_EM_MOVIMENTO` are the `SceneContext`-conditioned
overriders; `MoralHerdadaAbility` is the skill-scoped one.

### Cena de Combate, Rounds, and "ganhou a iniciativa"

Three facts `Scene` resolves once and carries into the `SceneContext` snapshot, the same
already-resolved way as allies/enemies/distances/terrain — `SceneContext` never queries a live
`Scene`:

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

`POSICIONAMENTO_ESTRATEGICO`'s Reação-suppression half is the only piece of this family still
TODO'd, on the movement-triggers-Reação gap. Check each constant's own TODO rather than
assuming one fix unblocks every citation of it.

### Acquisition-time choice — `MoralHerdadaAbility`

`MORAL_HERDADA` needs a Fama Positiva/Negativa choice feeding its own math, so — same pattern
as `APRIMORAR_COM_ARTE` — the enum constant stays the catalog entry and a character who picks
it is granted a `MoralHerdadaAbility(FamaChoice)` instance in `egoAdvantages` instead. It
delegates `getEgoDomain()`/`getDescription()` to the constant, and:

- overrides `resolveSkillSpecificRollBonus`: `+1` (`BASE_ROLL_BONUS`) toward Artes and
  Persuasão only, `+1` more per 10 points (`FAMA_POINTS_PER_BONUS_STEP`, floor division) of the
  chosen Fama — read *live* off `target` every call, not frozen at acquisition, since Fama keeps
  growing and the rules track "a Fama escolhida," not a snapshot.
- exposes `applyStartingFama(Character, CharacterSheet)` for the "recebe Fama igual ao seu
  valor de Recursos" half — real and tested, **not** TODO'd, but with no automatic caller:
  `CharacterCreationServiceImpl` only assembles a `Character`, and Fama lives on
  `CharacterSheet`. Same ordering gap `upgradeBase`/`upgradeGraduation` work around by taking
  both explicitly.

### `SorteAdvantage#resolveCriticalMarginIncrease` — a per-enum method, not on the interface

`ACE`'s `resolveCriticalMarginIncrease(SkillType, SceneContext)` is real, tested data — `+1`
for a Perícia de Ataque during a Cena de Combate, `+3` for a non-Ataque Perícia outside one,
`0` otherwise — the same shape as `ArtesAprimorarComArteAbility#getCriticalMarginReduction`,
and carrying that method's identical gap: no roll-resolution engine consults either
automatically.

It's declared on `SorteAdvantage` itself, **not** the shared `EgoAdvantage` interface —
unlike `resolveSkillSpecificRollBonus`, which had a second real consumer already. Promote it
only if a second `EgoDomain`'s Vantagem ever needs the same shape.
## Movimento Base, and blessings granted on winning initiative

**`MovementService#getTotalMovement(Character, turnNumber)`** follows the `InitiativeService`
variant of the aggregated-stat shape (no new `Character` field, no `CharacterFixture` change).
Base is derived — `SizeCategory.getMovementPerActionPoint()` (via
`CharacterSizeService#getEffectiveSizeCategory`, so size-shifting is reflected) times
`ActionPointsService.getMaxActionPoints` for that Turn — plus the usual `ModifierType.MOVEMENT`
three-source sum, using `SkillCompetencyAbility.allFor` so racial abilities count (unlike
`ReactionsService`/`InitiativeService`, which predate that fix). Floored at 0. Returns the
**permanent** total only; a caller wanting this Round's actual figure adds
`CharacterSheet#getTemporaryBonus(ModifierType.MOVEMENT)`.

Vertical/swim movement (`AtletismoCompetencyAbility.ALPINISTA_VELOZ`/`ANFIBIO`) and a
mount's own movement (`DirigirECavalgarExcellency`) are a **different** sub-stat — don't wire
them into `ModifierType.MOVEMENT`.

**`Blessing`** is also what a trait grants the moment its holder wins initiative. A
`default List<Blessing> resolveInitiativeBlessings()` (empty by default) exists on
`EgoAdvantage`, `AttributeAbility`, and `SkillCompetencyAbility` — those exact three sources,
deliberately **not** `SkillExcellency`, unlike the four-source flat-`@Modifier` convention.
No-arg, since it resolves once at grant-time, not per-roll; Round-scoping lives in the granted
blessing's own `rounds`. `InitiativeAdvantage.POSICIONAMENTO_ESTRATEGICO` is the only current
override, granting `(MOVEMENT, +2, 2 rounds, SELF_AND_ALLIES)`.

**`InitiativeBlessingService#resolveBlessings(Character)`** is the pure-function scan across
those three sources. It grants and mutates nothing.

**`Scene#applyInitiativeBlessings(CharacterSheet winner, List<Blessing> blessings)`** is called
the moment winner's group wins, with the caller passing already-resolved blessings — `Scene`
never reaches into a Service to compute what abilities grant (the same restraint
`buildContext` follows). Throws `IllegalOperationException`/`INITIATIVE_NOT_WON` if
`wonInitiative(winner)` isn't already true. It revokes every blessing an earlier call granted
(via `CharacterSheet#removeEffect` — reference-based, since neither `TemporaryEffect` nor
`TemporaryBonus` overrides `equals()`, so an unrelated bonus of the same `ModifierType`
survives), then grants all of `blessings` to winner plus every `SELF_AND_ALLIES`-scoped one to
each `getAllies(winner)`, tracking each fresh `TemporaryBonus` in a `Scene`-owned
`grantedBlessings` map so the next call revokes precisely these and no more.

`addParticipant(CharacterSheet, int, UUID group)` extends currently-active **ally-scoped**
blessings to someone joining an already-blessed group. `isGroupBlessed(UUID)` derives that
from `grantedBlessings` directly rather than caching it — a group is blessed exactly when
someone already tracked belongs to it. The blessing *values* (`activeBlessings`) do need their
own field, since `grantedBlessings`' raw `TemporaryBonus`es don't carry the originating
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
  javadoc says "a creature".
- **`Character` is shared too** — a foe's Attributes, Perícias, abilities and equipment are an
  ordinary `Character`, with `race` set to the single catch-all `Monstruoso` and `player` left
  `null` (that field is nullable for exactly this reason, and nothing in main source reads it).
  Don't build a parallel `Monster extends Character`; the stat-carrying half was never
  player-specific.
- **`lifeMultiplier`/`determinationMultiplier` are now `Character` fields**, mirroring the
  `manaMultiplier` that already was one. This is what lets a foe's PV budget be tuned apart from
  its Vigor — previously the only way to make something tanky also inflated every Vigor-governed
  roll. They're not monster-only; a GM house rule uses them the same way.

## Building a foe — `org.aventyrs.core.monster.MonsterTemplate`

Two paths, the same `Item`/`AbstractItem`/`ArmorItem` split this codebase already uses:
`AbstractMonsterTemplate` (`@Builder`) is *fill in the form* for a designed foe, `GenericMonster`
is a catalog of stand-ins for *a generic monster on-scene*. `spawn()` returns a fully independent
`MonsterSheet` each call — and must, since `SkillGraduation` is mutable and
`CharacterSkill#increaseGraduation` mutates in place, so sharing one across spawns would let one
foe's growth raise another's.

A foe's four numbers are **authored, not derived** — a stat block says what a Goblin's DF is; it
isn't recomputed from its Destreza and Graduação the way a player's defence roll is. Nothing
checks them against the Attributes behind them, deliberately.

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

## Races live in `org.aventyrs.core.race`, not `org.aventyrs.core.character`

`Race` and every implementation (`Human`, `Anao`, `Elfos`, `Gigantes`, `Pequenino`, `Gnomos`,
`Orcs`, and any `*RacialAbility` enums) live in their own top-level package, `org.aventyrs.core
.race` — a sibling of `org.aventyrs.core.character`, not a subpackage of it, mirroring how
`org.aventyrs.core.ability`/`org.aventyrs.core.feat` already sit alongside `character` rather
than inside it. `Character` still holds a `Race race` field (and imports `org.aventyrs.core
.race.Race` for it) — the two packages reference each other (`Race#generateEmptyCharacter`
returns a `Character.CharacterBuilder`), which is an ordinary mutual class dependency Java has
no trouble with, not a layering violation. If a future race needs its own subpackage
(mirroring `org.aventyrs.core.skill.<skillname>`'s one-subpackage-per-Perícia convention),
nothing here rules that out — there just wasn't a need for it yet.

Not every race needs a `*RacialAbility` catalog: `Gigantes`/`Pequenino` (and the newly
fleshed-out `Human` javadoc) leave `getRacialAbilities()` at `Race`'s own empty default,
because none of their racial traits actually fit `SkillCompetencyAbility`'s shape — some
aren't roll-conditioned at all (a Defesa modifier, a conditional RD), one needs a target
classification this core can't make, one spans multiple `SkillType`s by `AttributeDomain`
rather than naming one, and several are really "grant an acquisition slot" traits (matching
`Elfos`' Origem Mística / `Anao`' Pequenos Gigantes' already-documented gap), not roll
bonuses. Don't force a `*RacialAbility` enum into existence just for symmetry with `Anao`/
`Elfos` — only build one once a trait's shape genuinely fits.

`Gigantes` is also the first race to override `getNewFeatCost(FeatCategory)` for real
(Talentos de Sobrevivência cost 2 XP instead of `Race.BASE_NEW_FEAT_COST`'s 3) — unlike the
2.5-XP-style discounts every other race's own "Talentos custam menos" trait wants (`Elfos`'
Conexão com o Mana, `Human`'s/`Pequenino`'s own Adaptação), 2 is a whole number `int` can
represent exactly, so this one didn't hit the int-vs-fractional mismatch those did.

## Racial Abilities reuse `SkillCompetencyAbility` — `Race#getRacialAbilities()`

A trait every member of a Race is automatically granted (e.g. Anões' Abatedores de Gigantes,
Vantagem on Ataque rolls against a target 2+ Categorias de Tamanho larger) contributes to a
roll the *exact* same way a player-acquired `SkillCompetencyAbility` does — so it's modeled as
one, rather than a parallel duplicate type: `Race.getRacialAbilities()` (default `List.of()`,
overridden e.g. by `Anao` returning `List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES)`) is
just another `List<SkillCompetencyAbility>`, differing from `Character
.getSkillCompetencyAbilities()` only in *where* it's sourced from — fixed per Race, not a
player's per-Character acquisition choice — never in how a consumer treats its entries. A
consumer that wants "every ability of this kind, acquired or racial" concatenates both lists
(see `AtaqueADistanciaInteraction`, below) rather than adding a second parallel scan.

This is also what motivated `SkillCompetencyAbility#resolveAttackRollBonus(CharacterSheet
actor, CharacterSheet attackTarget)`, a fourth `resolve*`/`get*` default method alongside
`getDifficultyReduction()`/`getSubstituteAttributeDomain()`/`resolveDamageBonus()` — Abatedores
de Gigantes' bonus targets `skillRollBonus` itself (the Perícia roll, not a dano roll), and,
like `FRIEZA`'s `resolveDamageBonus`, is conditioned on the real attack target (its
`SizeCategory` here, not its distance), which a reflection-invoked no-arg `@Modifier` method
still can't see. Unlike `resolveDamageBonus` (only one bonus expected to apply per roll, so
callers take the first non-empty result), `resolveAttackRollBonus` returns a plain `Optional
<Integer>` meant to be *summed* across every ability that grants one — the same additive
convention every other `skillRollBonus` source already follows.

Two consumers wire this in, at two different levels:

- **Generically, for every skill**: `AbstractSkillInteraction` itself now has a private
  `allSkillCompetencyAbilities(Character)` helper — `character.getSkillCompetencyAbilities()`
  concatenated with `character.getRace().getRacialAbilities()` — and every place its `applyTo`
  used to scan only the acquired list (the `@Modifier`-based `skillRollBonus` sum, the
  `getDifficultyReduction()` sum, and `SkillCompetencyAbility.resolveAttributeDomain`) now
  scans this combined one instead, with zero special-casing. `validateRequestedAbility` checks
  both lists too, so a `SkillRoll` can name a racial ability as its `requestedAbility` the same
  as an acquired one. This is what makes `ElfosRacialAbility.SENTIDOS_ABSOLUTOS` (unconditional
  Vantagem on every Atenção roll — an ordinary `@Modifier(ModifierType.ATTENTION_ROLL_BONUS)`
  method, granted the same flat-Vantagem way as any acquired ability, see "Vantagem is a flat
  +2 bonus" above) work automatically for every `Elfos` character, with no
  `AttentionInteraction`-specific code at all.
- **Explicitly, where an ability needs the real attack target**: `resolveDamageBonus`/
  `resolveAttackRollBonus` aren't part of that generic scan (they need `attackTarget`, which
  `AbstractSkillInteraction`'s shared `applyTo` has no parameter for) — `AtaqueADistanciaInteraction
  #applyTo(CharacterSheet, SceneContext, SkillRoll, CharacterSheet)` (the same overload FRIEZA
  needed) does its own concatenation and resolves both against it identically, never checking
  *which* constant or *which* source answered. `AnoesRacialAbility.ABATEDORES_DE_GIGANTES` only
  overrides `getSkillType()` to `ATAQUE_A_DISTANCIA` and is only wired into this one Interaction
  so far — the rules text actually covers every "Perícia de Ataque" (`SkillType#isAttackSkill()`),
  but `AtaqueCorpoACorpoInteraction` doesn't yet take an `attackTarget` parameter, so the melee
  side isn't wired; a future change extending it should follow this exact same shape rather
  than inventing a new one.

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
`FurtividadeCompetencyAbility.LADINO_TEORICO`, `PersuasaoCompetencyAbility.FORCA_OPRESSORA`),
and — for the *unconditional* case (always substitutes, no scoping to a specific
attack/delivery method) — it's now mechanically real, following `ACUIDADE` as the reference:

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
  .resolveAttributeDomain(skillCompetencyAbilities, skillType, defaultDomain)`'s job — a
  static method on the interface, mirroring `SkillExcellency.unlockedBy`'s existing
  static-method-on-interface shape. It filters for entries whose `getSkillType()` matches
  `skillType` and whose `getSubstituteAttributeDomain()` is present, returning the first
  match's Attribute or `defaultDomain` if none — byte-for-byte identical at every call site
  (only `skillType`/`defaultDomain` vary), so it's a single shared method rather than
  duplicated. It's called unconditionally by `AbstractSkillInteraction.applyTo` for every
  skill now (see the next section) — safe even for skills with no substituting ability, since
  it just falls through to `defaultDomain` — and by `SkillGraduationService`'s max-Graduação
  cap for the same reason.
- This only covers the *unconditional* case. A substitution scoped to a specific circumstance
  (e.g. `AtaqueADistanciaCompetencyAbility.ARREMESSO_PODEROSO`, only for thrown-weapon/spell
  attacks) can't be modeled this way — same "this codebase doesn't track what a roll is *for*"
  simplification already documented for scoped Vantagem bonuses below; document that gap in a
  TODO on the constant instead. `GnoseAbility.PERITO_TEORICO` is also a different shape
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
- Building this mechanism doesn't retroactively finish every ability that cites it — check
  each constant's own TODO. `ACUIDADE`, `ACROBATA`, `DISPARO_ARCANO`, and `MAGIA_SELVAGEM` are
  fully wired (enum override + `Interaction` filter + service overload) — see
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
