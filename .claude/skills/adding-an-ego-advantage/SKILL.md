---
name: adding-an-ego-advantage
description: This skill should be used when the user asks to "add a new Vantagem de Ego", "add an Ego Advantage", "add [name] to SorteAdvantage/InitiativeAdvantage/ResourcesAdvantage/AutocontroleAdvantage", or gives the rules text for a Vantagem belonging to one of the four EgoDomains (Autocontrole, Iniciativa, Recursos, Sorte). Walks through picking the right EgoAdvantage hook for the clause, the acquisition-time-choice pattern, and wiring it — mirroring org.aventyrs.core.ego as the reference implementation.
---

# Adding a Vantagem de Ego

A Vantagem de Ego lives in `org.aventyrs.core.ego`, one enum per `EgoDomain`:
`AutocontroleAdvantage`, `InitiativeAdvantage`, `ResourcesAdvantage`, `SorteAdvantage`. Each
implements `EgoAdvantage`.

## 0. How it's chosen and stored

Chosen **once at character creation**, gated on that `EgoDomain`'s creation-time `base` reaching
`CharacterCreationService.EGO_ADVANTAGE_MIN_BASE` (**3**) — the same threshold for every domain,
checked through the single generic `isEgoAdvantageAvailable(EgoDomain, CharacterEgos)`. **Don't
reintroduce a per-domain `isXAdvantageAvailable` method/constant pair** unless a domain's
threshold is ever confirmed to differ from 3.

`Character` stores every domain's choice in one `@Singular Map<EgoDomain, EgoAdvantage>
egoAdvantages` — never one nullable field per domain. A domain with no chosen Vantagem is simply
**absent**, never a `null` value inside. Read via `Character#getEgoAdvantage(EgoDomain)`, set via
the `@Singular`-generated `.egoAdvantage(EgoDomain, EgoAdvantage)` — **never index the map
directly** outside those two spots.

Adding a constant to an existing enum needs no `Character`/fixture change. A genuinely new
`EgoDomain` would — but there are exactly four, and that isn't a thing you add.

## 1. Pick the hook that matches the clause

`EgoAdvantage` declares `getEgoDomain()`/`getDescription()` plus **seven `default` hooks**, all
defaulting to empty/zero/false. They mirror `SkillCompetencyAbility`'s `resolve*` shape for the
same reason — a no-arg `@Modifier` method can't see a `SceneContext` — but are summed across
**every** skill, since a Vantagem de Ego was never tied to one Perícia.

Match the rules text to exactly one:

| The clause says | Hook | Notes |
| --- | --- | --- |
| a bonus toward *any* Perícia roll | `resolveConditionalRollBonus(SceneContext)` | summed by `AbstractSkillInteraction#sumEgoAdvantageRollBonuses` into `skillRollBonus` |
| a bonus toward specific **named** skills | `resolveSkillSpecificRollBonus(SkillType, SceneContext, CombatantSheet target)` | see below |
| extra dano on an attack | `resolveDamageBonus(SceneContext)` | resolved whenever `skillType.isAttackSkill()`, **first non-empty wins** |
| flat RA (Redução Absoluta) | `resolveAbsoluteDamageReduction(SceneContext)` | summed alongside the reflection-based `ABSOLUTE_DAMAGE_REDUCTION` scan |
| damage taken is halved | `resolveHalfDamage(SceneContext)` | boolean, ORed with the `HALF_DAMAGE` scan being `> 0` |
| widens Margem Crítica Menor | `resolveCriticalMarginIncrease(SkillType, SceneContext)` | see step 2 |
| a bonus the moment its holder **wins initiative** | `resolveInitiativeBlessings()` | **stop — use the `granting-a-blessing` skill instead** |

Two shape notes that decide which of the roll-bonus pair you want:

- **`resolveSkillSpecificRollBonus` exists so a named-skill scope doesn't over-grant.**
  `MORAL_HERDADA`'s "+1 em Artes e Persuasão" through `resolveConditionalRollBonus` would leak
  into every other Perícia. A scope of specific *named* skills **is** trackable; a narrative
  *purpose* ("only for animal-related rolls") is not — this core doesn't track what a roll is
  *for*, so document that simplification on the constant rather than narrowing or over-granting.
  Its `target` is **the roller's own sheet**, not an attack target — passed because the bonus may
  depend on the roller's own live state (Fama, PV, …). Note the type is `CombatantSheet`, so it
  works against a foe too.
- **`EgoAdvantage#resolveDamageBonus` needs no `attackTarget`**, so it works for both Ataque à
  Distância *and* Corpo a Corpo off the plain 2-arg `applyTo` — unlike
  `SkillCompetencyAbility#resolveDamageBonus`, which is only reachable through the 4-arg
  `attackTarget`-aware overload. If the clause is conditioned on the *target* rather than the
  scene, it does not belong on an Ego advantage.

**Only override a hook whose rules text genuinely calls for it.** Every one is `default` so a
constant declares nothing it doesn't need.

## 2. `resolveCriticalMarginIncrease` is fully wired — it is *not* a dead hook

This hook lives on **all three** ability interfaces — `EgoAdvantage`, `AttributeAbility`, and
`SkillCompetencyAbility` — with an identical signature.
`AbstractSkillInteraction#sumCriticalMarginIncrease` sums it across all three sources and feeds
the total into `SkillRoll#getCriticalResult(int)`, which widens which faces count toward a
critical (e.g. 5s counting alongside 6s). It is consulted for real on every roll that carries a
`SkillRoll`.

`SorteAdvantage.ACE` is the reference override: **+1** for a Perícia de Ataque during a Cena de
Combate, **+3** for a non-Ataque Perícia outside one, **0** otherwise.

Don't describe this as blocked on a missing roll-resolution engine — that was true before the
margin-aware overload landed and is no longer. The still-unconsumed neighbour is
`ArtesAprimorarComArteAbility#getCriticalMarginReduction`, a *different* method parameterized by
a dynamically-chosen Perícia.

## 3. If the Vantagem needs an acquisition-time choice

Some Vantagens make the player pick a value when they take it, and that choice feeds the
Vantagem's own math. Follow `MoralHerdadaAbility` (the same pattern as
`ArtesCompetencyAbility.APRIMORAR_COM_ARTE`):

- The **enum constant stays the catalog entry**. A character who picks it is granted a
  `MoralHerdadaAbility(FamaChoice)` *instance* in `egoAdvantages` instead.
- The instance delegates `getEgoDomain()`/`getDescription()` to the constant and overrides
  whichever hook the choice feeds.
- **Read the choice-dependent value live off `target` on every call, never frozen at
  acquisition.** `MORAL_HERDADA` grants `+1` (`BASE_ROLL_BONUS`) toward Artes and Persuasão, plus
  `+1` per 10 points (`FAMA_POINTS_PER_BONUS_STEP`, floor division) of the chosen Fama — read
  live, because Fama keeps growing and the rules track "a Fama escolhida", not a snapshot.

Leave a comment on the enum constant redirecting to the class, and put TODOs on the **class**,
not the constant.

### The creation-ordering gap

`MoralHerdadaAbility#applyStartingFama(Character, CharacterSheet)` covers the "recebe Fama igual
ao seu valor de Recursos" half. It is **real and tested, not TODO'd** — but has **no automatic
caller**: `CharacterCreationServiceImpl` only assembles a `Character`, and Fama lives on
`CharacterSheet`. That's the same ordering gap `upgradeBase`/`upgradeGraduation` work around by
taking both parameters explicitly. If your new Vantagem has a "starts with N of some
sheet-resident resource" clause, expect the same shape and the same lack of a caller.

## 4. Don't promote a hook to the interface speculatively

`EgoAdvantage`'s hook set grew one method at a time, each on a real second consumer. If your
Vantagem needs a shape no existing hook has, put the method on **its own enum** first — a plain
public method on that constant — and promote it to `EgoAdvantage` only once a second
`EgoDomain`'s Vantagem needs the identical shape. This is CLAUDE.md's "build for the second real
consumer" rule; `resolveCriticalMarginIncrease` is the worked example of a method that *earned*
promotion (it now sits on three interfaces), not a counterexample.

## 5. Write tests

Follow the existing per-enum test files:

- Catalog sweep — one constant per cataloged Vantagem, each with a non-blank description and the
  right `getEgoDomain()`.
- One test per overridden hook, exercising **both** branches of its condition (in and out of a
  Cena de Combate, in and out of range, above and below a threshold) — `ACE`'s +1/+3/0 needs all
  three.
- For a choice-carrying instance class, a test per choice value, plus one proving the value is
  read live rather than frozen (mutate `target` between two calls and assert the result moved).
- An end-to-end test through the real `<Skill>Interaction` proving the bonus lands in
  `skillRollBonus`, and a control Perícia proving a skill-scoped bonus does **not** leak.

## Reference files to read first

- `org.aventyrs.core.ego.EgoAdvantage` — the interface and all seven hooks, each with javadoc
  saying exactly when to override it.
- `org.aventyrs.core.ego.SorteAdvantage` — `ACE`'s `resolveCriticalMarginIncrease` override.
- `org.aventyrs.core.ego.InitiativeAdvantage` — `IMPETO`/`TORRE_EM_MOVIMENTO`, the
  `SceneContext`-conditioned overriders.
- `org.aventyrs.core.ego.MoralHerdadaAbility` — the acquisition-time-choice instance class.
- `org.aventyrs.core.skill.AbstractSkillInteraction` — `sumEgoAdvantageRollBonuses`,
  `sumEgoAdvantageSkillSpecificRollBonuses`, `sumCriticalMarginIncrease` (the three call sites).
