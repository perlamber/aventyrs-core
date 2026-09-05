---
name: adding-a-feat
description: This skill should be used when the user asks to "add a new Talento", "add a new Feat", "add [Feat name] to [tree]Feat", or references adding a new entry to org.aventyrs.core.feat. Walks through producing a catalog entry like `ArtesMarciaisFeat.ARTISTA_MARCIAL` — its requirements, its bonus, and the enum wrapping class — plus the sealed-catalog / `resolve*`-hook rationale, mirroring `ArtesMarciaisFeat`/`MetamagicoFeat` as the reference.
---

# Adding a new Talento (Feat)

A Talento sits alongside skills/Títulos as an acquirable character trait, but it's a flat
catalog — one enum per Talento *tree*, named by `FeatCategory` (e.g. `ArtesMarciaisFeat` for
`FeatCategory.ARTE_MARCIAL`), mirroring `<Skill>CompetencyAbility`'s one-enum-per-domain shape.
This skill is the full checklist; the architectural rationale is in section 0 (it used to live
in CLAUDE.md's "Adding a new Talento (Feat)" section). Use the **`testing-a-feat`** skill to
test one.

## 0. Architectural rationale

`Feat`/`FeatRequirements`/`AbstractFeat`/`FeatCategory` plus `FeatService#grantFeat(Character,
CharacterSheet, Feat)` (validate `Feat#isEligible`, spend `Race#getNewFeatCost` XP —
`Race.BASE_NEW_FEAT_COST` is 3 — then mutate, the same shape as
`CharacterAttributeService#upgradeBase`/`TitleAbilityService#grantTitleAbility`) are the whole
mechanism.

- **`Feat` is `sealed`, and that is what makes the catalog enumerable.** `permits
  ArtesMarciaisFeat, MetamagicoFeat, AbstractFeat`, so `FeatCatalog` discovers every authored
  Talento via `Feat.class.getPermittedSubclasses()` — reflection with no classpath scan, no I/O,
  and no silent partial results. A classpath walk was rejected deliberately: it has to
  special-case exploded dirs vs JAR entries vs the module path, and when it guesses wrong a whole
  Talento tree just quietly stops being offered. The permits clause is **compiler-enforced**.
- **`AbstractFeat` is `non-sealed` with a *public* all-args constructor** — the extension point
  that keeps sealing from closing this library off. A consumer's homebrew Talento is
  `new AbstractFeat(category, description, requirements) { @Override ... }`, a first-class `Feat`
  everywhere, that simply never appears in `FeatCatalog`. **Don't implement `Feat` directly**
  (the compiler now refuses it).
- **Discovery lives in `FeatCatalog`, never as a `static` field on `Feat`.** `Feat` declares
  `default` methods, so initialising `MetamagicoFeat` initialises `Feat` first — a static
  initialiser on `Feat` calling `getEnumConstants()` back on that enum would observe it
  mid-initialisation and could read its constants as `null`. A separate class breaks the cycle.
- `FeatService#getAvailableFeats(Character)` (prerequisites met, not already held) and
  `#getAffordableFeats(Character, CharacterSheet)` (that list, narrowed by the XP wallet) are
  **deliberately separate methods, not a flag**: "am I allowed this?" and "can I afford it?"
  differ over time and a UI wants both. Cost is per-Race, so two characters with identical
  Talentos and identical XP can get different answers.
- **`Feat` carries a family of `default resolve*` hooks**, not one — among them
  `resolveDamageBaseIncrease(Character, Weapon)`, `resolveAttackRangeIncrease(Character,
  AttackSource)` and `resolveAttackCostDifficultyReduction(SkillType, SceneContext, Character,
  AttackSource, ActionCost, List<CombatantAction>)` (both take an `AttackSource` — a range clause
  scoped to delivery, and a GD clause scoped to *what the attack cost* plus the Rodada's prior
  actions, `AssassinoFeat#SAQUE_RELAMPAGO`; the latter is first-consumer-shaped, see
  `damage-and-combat`), `resolveBranchLevelIncrease` (the Árvore de Magia cap — see the
  `magic-system` skill), `grantsFreeSpellAcquisition(Character, Spell)` /
  `resolveSpellAcquisitionCostReduction(Character, Spell)` → `BigDecimal` (a Talento that hands a
  Magia to its holder free, or discounts the XP price — both fed to
  `SpellService#getAcquisitionCost`, which also sums the `Race` twin; the waiver is wired ahead
  of `MetamagicoFeat#ARCANISTA`'s choice class, the discount alongside
  `ElementalFeat#ARCANISMO_ELEMENTAL`), `resolveDefenseBonus(DefenseType, Character)`,
  `resolveManaMultiplierIncrease`/`resolveRestMagicPointsBonus(RestType, Character)`, the
  movement/action-point ones, `resolveExtraCriticalEffects(...)` (Efeitos Críticos a Talento adds
  to a critical hit — `AttackDelivery` scans it, `AssassinoFeat#ABRIR_FERIDAS`), and
  `resolveDefeatBlessings(attacker, defeated, viaCriticalHit)` (`Blessing`s the moment one of the
  holder's attacks drops a foe — `DefeatBlessingService`, caller-driven),
  `getGrantedNaturalWeapons(Character)` (`NaturalWeapon`s — `Character#getNaturalWeapons()`),
  `resolveLifeStealBonus(Character)` (Roubo de Vida amplification — `LifeStealService`,
  `VampiricoFeat#SEDE_DE_SANGUE`), `resolveAdditionalTargets(SkillType, Character)` (how many
  targets beyond the primary one attack may name — `AttackTargetingService`,
  `ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_ARTE_FLUIDA`; the hook answers *how many*, never *which*
  — adjacency is the caller's, see `damage-and-combat`), `resolveAttributeBonus(AttributeDomain, Character)` (a flat
  Atributo grant, the first Talento hook to do so — read *only* by `AbstractSkillInteraction`
  into a governed Perícia roll, `VampiricoFeat#MESTRE_VAMPIRO`), and `resolveActiveAbility()` →
  `Optional<ActiveAbility>` (a Poder Vampírico — an activatable timed state triggered through
  `ActiveAbilityService#activate`; must return a **stable singleton**, since
  `Character#getActiveAbilities()` aggregates `getFeats()` live and `activate` matches by `==` —
  see `PoderVampiricoActiveAbility` / `VampiricoFeat`). Several hooks now have a
  trailing `CombatantSheet holder` overload that falls through to the sheet-less form
  (`resolveSkillRollBonus`, `resolveDefenseBonus`, `resolveDamageReduction`,
  `resolveCriticalMarginIncrease`) — override it for a clause reading held `Condição`s or the
  per-Rodada/per-Cena action log. `resolveDamageBonus` has a trailing `int targetCount` overload
  on the same defaulting terms, for a clause conditioned on how many targets the one dano roll
  covers (`ARTE_FLUIDA`'s "enquanto houver mais de um alvo … Desvantagem em rolagens de Danos");
  `0` there means no target was named, never "one". The "keep it on the tree enum until a
  second consumer earns the interface" rule still holds for a formula only that tree reads — but
  **a hook a *service* must scan for has to be on `Feat` itself**, since `character.getFeats()`
  is a `List<Feat>`. Feats are deliberately **not** part of any `ModifierResolver` `@Modifier`
  scan, so each consuming service gives them an explicit pass, the way `DefenseServiceImpl`
  already does for equipment.
- **A Talento's prerequisites are real, enforced data** — the second exception (alongside
  `AventyrTitleAbility`'s "Requer N Especializações/Habilidades") to this codebase's usual
  "leave 'Requer N Graduações' as an unenforced comment" restraint, because a Talento's own
  Pré-requisito is always a simple numeric/identity threshold `FeatRequirements` can model
  directly.
- `MetamagicoFeat` is the second tree, and worth reading before adding a third — it's the first
  to need a **sibling constant as a prerequisite**, which Java's forward-reference rule forbids
  in constructor arguments. Two things fall out: `featRequirements` is held as a
  `Supplier<FeatRequirements>` rather than a plain field, and **the constants are ordered so
  every prerequisite is declared before its dependents** (`MetamagicoFeatTest` pins that
  ordering).

## 1. Read the rules text first

Get the Talento's name, its full Pré-requisito line, and its full Descrição verbatim.

`Feat` (`org.aventyrs.core.feat`) is the shared interface — `getFeatCategory()`/
`getDescription()`/`getFeatRequirements()`, plus a default `isEligible(Character)` that combines
*every* set prerequisite in `getFeatRequirements()` at once (mirrors `AventyrTitleAbility
#isEligible`'s identical shape, with Attribute/Perícia/Feat prerequisites instead of
Especialização/other-Habilidade ones). `FeatRequirements` (a `@Builder record`) carries the raw
data. Unlike most "Requer N..." prerequisites elsewhere in this core (left as an unenforced
comment), a Talento's prerequisites are always simple numeric/identity thresholds, so they're
modeled as real, structured, *enforced* data.

Parse the Pré-requisito into whichever of these it actually names (a Talento rarely names all
three):
- An Attribute floor (e.g. "Força 2") → `attributeDomain`/`requiredAttributeValue`.
- A Perícia Graduação floor (e.g. "2 Graduações em Ataque Corpo-a-Corpo") →
  `requiredSkillType`/`requiredSkillGraduation`.
- Another specific Talento already held (e.g. "Requer Artista Marcial") → `requiredFeat`,
  pointing at that other Talento's own enum constant.
- A held Habilidade de Competência → `requiredSkillCompetencyAbility`; N Títulos Aventyr Despertos
  (optionally of one `TitleArchetype`) → `requiredAwakenedTitles`/`requiredTitleArchetype`; a Race
  / `CreatureType` / `Deity` → `requiredRace`/`requiredCreatureType`/`requiredDeity`; N other
  Talentos of a tree → `requiredFeatCategory`/`requiredFeatCategoryCount`.
- **A count of Regalias the holder has forged** ("criação de 3 ou mais Regalias Menores") →
  `craftedRegaliaGrade`/`craftedRegaliaCount` (read off `Character#getRegaliasCrafted`) — the
  `ArtificeFeat` ladder's own acquisition gate.

**Before filing a clause here, ask whether it gates *acquiring* the Talento or *using* it.**
`FeatRequirements` answers "may this character learn it", checked once by `FeatService#grantFeat`;
a condition that must hold *every time the Talento is exercised* belongs in a `resolve*`-style
hook instead, because a requirement that stops being true would otherwise read as un-learning the
Talento. `ArtificeFeat` is the worked example and carries both: Profissão 7 / the rung below / the
forged-Regalia history are requirements, while "a Regalia em sua posse" is a **permission
condition** — `Feat#itsAllowedToCraftRegalia(Character holder)` returns the `RegaliaGrade` the
holder may forge *right now*, `null` when they hold no Regalia, and `org.aventyrs.core.item
.ItemForgery` scans every held Talento rather than looking up a specific constant. A crafter who
sells their Regalia keeps the Talento and simply cannot forge until they own one again.

Then classify the Descrição's mechanic the same way every other ability in this codebase is
classified — see step 3.

## 2. Find (or create) the tree enum

Check whether an enum for this Talento's `FeatCategory` already exists under
`org.aventyrs.core.feat` (e.g. `ArtesMarciaisFeat` for `ARTE_MARCIAL`). If it does, add a new
constant to it. If not, create `<Tree>Feat implements Feat` — package-level, alongside `Feat`/
`AbstractFeat`/`FeatCategory`/`FeatRequirements` (this codebase doesn't give Feats their own
per-tree subpackage the way Perícias/Títulos get one, since a Talento tree has no sibling
files of its own beyond the enum itself). Override `getFeatCategory()` **once, at the enum
level**, returning the fixed `FeatCategory` constant for the whole tree — every constant in
that enum belongs to the same category, so this isn't per-constant.

Each constant's constructor takes `description` and `featRequirements`:

```java
ARTISTA_MARCIAL(
        "O Dano Base de seus Ataques Desarmados e Armas Naturais aumenta em +1, ...",
        FeatRequirements.builder()
                .attributeDomain(AttributeDomain.STRENGTH)
                .requiredAttributeValue(2)
                .requiredSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
                .requiredSkillGraduation(2)
                .build());
```

Leave any prerequisite field unset (not zero-as-a-real-value) when the rules text doesn't name
it — `Feat#isEligible`'s default method already treats an unset `attributeDomain`/
`requiredSkillType`/`requiredFeat` as "not a blocking prerequisite," never as "requires zero."
An untrained Perícia reads as Graduação 0 for the skill floor, same as everywhere else in this
core.

If a bare `Feat` value is needed somewhere without hand-writing a whole enum body, use
`AbstractFeat` (`@Builder`, implements `Feat`) — the plain generic implementation for a Talento
that needs no constant-specific override.

## 3. Classify the Descrição's mechanic

Same discipline as every other ability catalog in this codebase (see CLAUDE.md's TODO-writing
convention in "Recurring conventions", and the `adding-a-title` skill's own step 3):

- **Real now**: pure arithmetic over already-real data — e.g. `ArtesMarciaisFeat
  #getDanoBaseBonus(Character)` sums a flat base plus `character.getAllTitles().size()`
  (a Título Aventyr is "Desperto" simply by being held — see `InstinctAbility
  #SENTIR_A_INTENCAO`'s own confirmed reading of that exact phrase). **"Can't apply it yet"
  (no consumer exists to call the method automatically) doesn't mean "can't compute it yet"**
  — implement the formula for real even with zero callers, same as
  `ArtesAprimorarComArteAbility#getBaseDamageBonus` was before any combat/roll-resolution
  engine existed to call it.
- **TODO'd**: cite the *specific* missing system, checking CLAUDE.md's existing gap catalog
  first (Defesas, Item/Equipamento, Encantamento/Maldição classification, Área de Efeito, a
  flat Desvantagem constant, forced-attack-targeting, cross-character continuously-recomputed
  passive grants — see CLAUDE.md's "Missing systems — the gap catalog" for the full list and
  exact citations to reuse verbatim rather than re-deriving).

Only add the method to the enum type itself (a concrete, non-abstract instance method,
overridden per constant that needs it, defaulting to zero/no-op on every other constant) —
**not** to the shared `Feat` interface — unless a second Talento tree genuinely needs the
identical hook. This mirrors `SkillCompetencyAbility`'s own default-method-plus-override shape,
just scoped one level narrower (per-tree-enum instead of interface-wide) until a second real
consumer earns the wider scope.

If the bonus is a flat Vantagem-style +2, or a roll-scoped bonus rather than a Dano Base one,
check whether it fits an existing hook first (`Feat` currently has none of
`SkillCompetencyAbility`'s roll-scoped `resolve*` hooks — if a Talento needs one, that's a
second real consumer; add it to `Feat` itself at that point, not before).

## 3b. Acquisition-time choice ("escolha uma Perícia / um tipo de terreno / um tipo de arma")

When the Descrição opens with a player pick, the enum constant **cannot** hold it — one
constant is shared by every character. Model it exactly like `ArtesAprimorarComArteAbility`:

- A hand-written `final class <Name>Feat extends AbstractFeat` in `org.aventyrs.core.feat`,
  with a `@NonNull` final choice field, a `static of(...)` factory, and `super(...)` delegating
  `getFeatCategory()/getDescription()/getFeatRequirements()` to the enum constant.
- Override `Feat#catalogEntry()` to return the enum constant. This is what keeps
  `Feat#isEligible`'s `requiredFeat` check and `FeatCatalog#availableFor`'s "already held"
  filter working — both compare against `catalogEntry()`, not object identity. **Do not** add a
  custom `equals`.
- Override the `resolve*` hook(s) the clause reaches, branching on the choice.
- Add a `static Optional<C> chosenBy(Character)` (mirrors
  `PeritoTeoricoAbility.resolveAttributeDomain`) so dependent Talentos in the same tree — which
  stay plain enum constants — can read the pick.
- Leave the enum constant in place as the catalog/rules entry, with its TODO block replaced by
  a javadoc `<p><b>Real</b>, through {@link <Name>Feat}` note. `FeatCatalog` still lists the
  constant (the choice instance isn't an enum, so it's never listed); the caller builds the
  instance at grant time and passes it to the existing `grantFeat`.
- **Choice types**: a `SkillType`, `TerrainType` (its six constants *are* the six terrenos),
  `AttributeDomain`, `EgoDomain`, a `Set<SkillType>`, a `Set<NaturalWeapon>` (`ArmamentoDraconicoFeat`
  — "escolha duas armas entre …", validated to exactly N of a fixed allow-list, granted via
  `Feat#getGrantedNaturalWeapons`). A "tipo de arma / armas naturais / magias
  ofensivas" choice is `org.aventyrs.core.item.AttackMethod` — matched against the delivered
  `AttackSource` via `AttackMethod#matches`, using the trailing-`AttackSource` cascading
  overload of `resolveSkillRollBonus`/`resolveCriticalMarginIncrease` (the longer form defaults
  to the shorter — override the longer one). A **coarser** "Armas ou Magias" (whole-category)
  choice is a local 2-value enum instead — `WeaponOrSpellChoice` (`SaqueRelampagoFeat`) — kept
  local because only one Talento names it; `matches(AttackSource)` is a bare `instanceof`.

References: `FocoEmPericiaFeat` (SkillType), `TerrenoPrediletoFeat` (TerrainType + a
Scene-conditioned hook), `EspecialistaEmArmaFeat`/`AtiradorPerfeitoFeat`/
`AcertoCriticoAprimoradoFeat` (AttackMethod), `AdotadoPorSylphFeat` /
`ArmamentoDraconicoFeat` (a set). Still unbuildable:
a chosen-Atributo/Ego grant (blocked on the acquisition-slot / Talento-can't-grant-Atributo
gaps), and a `FeatRequirements` clause about *another* held Talento's own choice.

## 4. Wire `FeatService`

Nothing to build here — `FeatService#grantFeat(Character, CharacterSheet, Feat)` already
validates `Feat#isEligible` (which reads straight off `FeatRequirements`, no per-Talento code
needed) and spends `character.getRace().getNewFeatCost(feat.getFeatCategory())` XP generically.
It throws `IllegalOperationException` (`FEAT_PREREQUISITE_NOT_MET`) when `isEligible` fails.
A new Talento constant works through this service automatically the moment its
`FeatRequirements` are set correctly in step 2 — don't hand-write a new validation path.

`grantFeat` then calls `Character#grantFeat(Feat)`, a plain mutator (like `#grantTitle`) that
validates nothing itself, trusting the caller already did — `Character#feats` is a real
*mutable* `List<Feat>`, unlike `skillCompetencyAbilities`/`attributeAbilities`'s `@Singular`,
fixed-at-creation-only shape, because a Talento is acquired well after a character exists. It
defaults to a fresh `new ArrayList<>()` per `Character.builder().build()` call, so there's no
aliasing across separate Characters.

## 5. Write tests

**Use the `testing-a-feat` skill** — it carries the full test checklist, and the rule that
matters most: a Talento is tested by the effect it causes on a character who *legally acquired
it* (through `FeatService#grantFeat`, meeting its own `FeatRequirements`), read from the service
that consumes its hook — never by calling the hook and asserting its return value. The outline
below is the shape that skill expands on.

One file per tree enum (create if this is the tree's first constant, extend if not):
- Every constant has a non-blank description.
- Every constant reports the tree's fixed `FeatCategory`.
- Expected constant count.
- `isEligible(Character)`: each prerequisite independently unmet rejects, all met accepts, an
  unset prerequisite never blocks (build a `Character` via `CharacterFixture` that satisfies
  every *other* set prerequisite to isolate the one under test — see
  `FeatServiceImplTest#grantFeatRejectsWhenAttributeRequirementIsntMetYet`/
  `#grantFeatRejectsWhenSkillGraduationRequirementIsntMetYet` for the shape).
- Any real formula method (step 3): the branch that grants a real value, and every other
  constant returning zero/no-op.

Add `FeatServiceImplTest` coverage too if this is genuinely new ground for that service (a new
kind of prerequisite it hasn't been exercised against yet) — most new Talentos need no service
test changes at all, since `FeatService` itself is generic over `FeatRequirements`.

**Remember `CharacterFixture`'s `feats` trap**: it defaults to immutable `List.of()`, matching
every other trait list there — a test granting a Feat onto a fixture-built `Character` must
first swap in a mutable list: `CharacterFixture.blank(CharacterFixture.BLANK).feats(new
ArrayList<>()).build()`.

## 6. Update docs

- This skill — section 0 if an *architectural* fact changed (the catalog shape, prerequisite
  enforcement, the `resolve*` hooks), the checklist steps otherwise. Update CLAUDE.md's skill
  index only if the trigger surface changed.

## Reference files to read first

- `src/main/java/org/aventyrs/core/feat/ArtesMarciaisFeat.java` — the worked example this
  skill follows (a tree with one constant so far, `ARTISTA_MARCIAL`, with a real Dano Base
  formula method and no wired caller yet).
- `src/main/java/org/aventyrs/core/feat/Feat.java` — `isEligible(Character)`, the
  requirement-check mechanism every new Talento's Pré-requisito plugs into via
  `FeatRequirements`.
- `src/main/java/org/aventyrs/core/feat/FeatRequirements.java` — every prerequisite field
  (`attributeDomain`/`requiredAttributeValue`, `requiredSkillType`/`requiredSkillGraduation`,
  `requiredFeat`, `requiredSkillCompetencyAbility`, `requiredAwakenedTitles`/`requiredTitleArchetype`,
  `requiredRace`/`requiredCreatureType`/`requiredDeity`, `requiredFeatCategory`/`…Count`,
  `craftedRegaliaGrade`/`craftedRegaliaCount`); leave any subset
  unset when the rules text doesn't name it. Note what is *not* here: a condition of use — see
  `Feat#itsAllowedToCraftRegalia` and `org.aventyrs.core.item.ItemForgery`.
- `src/main/java/org/aventyrs/core/character/services/FeatService.java`/
  `FeatServiceImpl.java` — the single entry point (`grantFeat`) that validates `isEligible`
  then spends XP via `Race#getNewFeatCost`, mirroring `TitleAbilityServiceImpl
  #grantTitleAbility`'s identical shape.
- `src/main/java/org/aventyrs/core/character/Character.java` — `feats`/`grantFeat(Feat)`, and
  why this one trait list is a real mutable field unlike `skillCompetencyAbilities`/
  `attributeAbilities`.
- `src/test/java/org/aventyrs/core/character/services/FeatServiceImplTest.java` — the
  requirement-isolation test shape (one test per independently-unmet prerequisite), and the
  `CharacterFixture` mutable-`feats`-swap pattern.
- `src/main/java/org/aventyrs/core/skill/artes/ArtesAprimorarComArteAbility.java` — the
  "compute the real formula even with no caller yet" precedent step 3 follows, and the
  catalog-constant-vs-acquired-instance split step 3b mirrors.
- `src/main/java/org/aventyrs/core/feat/FocoEmPericiaFeat.java` /
  `src/main/java/org/aventyrs/core/feat/EspecialistaEmArmaFeat.java` — the step-3b
  choice-carrying `AbstractFeat` subclass, and `AttackMethod` as a choice type.
- `src/main/java/org/aventyrs/core/feat/ArmamentoDraconicoFeat.java` — a step-3b subclass whose
  choice is a validated `Set` (exactly N of a fixed allow-list) granting via a non-`resolve*`
  hook (`Feat#getGrantedNaturalWeapons`).
