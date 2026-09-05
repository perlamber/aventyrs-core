---
name: testing-a-feat
description: This skill should be used when writing or reviewing tests for a Talento (Feat) — after `adding-a-feat` authors a constant, when asked to "test a Feat/Talento", "add tests for [Feat name]", "check a Talento's effect", or when a Race's reduced Talento cost needs covering. Walks through testing a Talento by the effect it causes *while held by a character who legally acquired it* — the objective, not the hook — plus the per-Race feat-cost test class.
---

# Testing a Talento (Feat)

A Talento is never tested as a function. It is tested as **something a character holds**: a
player meets its Pré-requisito, pays its XP, and from that moment a number on their sheet is
different. That sentence is the test.

Calling `ARTISTA_MARCIAL.resolveDamageBaseIncrease(character)` and asserting `1` proves the
arithmetic and nothing else — it would still pass if `DamageBaseService` never scanned
`getFeats()`, if the Pré-requisito were unsatisfiable, and if the constant cost 40 XP. Those are
the three ways a Talento actually breaks.

## The three questions every Talento test file answers

1. **Can a character reach it?** — the Pré-requisito is satisfiable, and each half independently
   blocks. Through `FeatService#grantFeat`, not `Feat#isEligible` alone.
2. **What changes once they hold it?** — the *objective*: an observable number, read from the
   service that consumes the hook, before versus after acquisition.
3. **What must not change?** — the control. Every neighbouring stat, and every other constant in
   the tree.

Hook-level unit tests (`resolveX(...)` returning a number) are still worth writing for the
formula's own edge cases — rounding, a zero floor, scaling terms — but they are the *second*
layer. `MetamagicoFeatTest` (formulas) and `MetamagicoFeatIntegrationTest` (objectives) are that
split. Never let the file contain only the first layer.

## 1. Name the objective before writing anything

Read the Descrição and finish this sentence for each real clause:

> A character who holds this Talento has a different **&lt;number&gt;**, read from
> **&lt;service&gt;**, by **&lt;amount&gt;**.

| clause shape | the number | the service to read it from |
| --- | --- | --- |
| "+N Dano Base" | `DamageBase` of a swing | `DamageBaseService#getDamageBase` |
| "conjura magias do tipo X" | the Árvore cap | `SpellService#getMaxBranchLevel` |
| "Bônus em sua DM/DF" | the Defesa total | `DefenseService#getTotalDefense` |
| "multiplicador de PM aumentado" | max PM | `MagicPointsService#getManaMultiplier`, and `#getMaxMagicPoints` |
| "a cada Descanso recupera +NPM" | PM recovered | `RestService#getRecoveredMagicPoints` |

If the sentence can't be finished, the clause is gap-blocked — go to step 5, don't invent a
number.

## 2. Build a character who genuinely qualifies

The character under test must satisfy the Talento's **own** `FeatRequirements`, built from real
data, not waved through. This is what catches an unsatisfiable Pré-requisito — a
`requiredAttributeValue` past the cap of 5, a `requiredSkillType` naming a Perícia the character
can't train, a `requiredFeat` chain that can't be walked in declaration order.

```java
private static Character.CharacterBuilder character() {
    return CharacterFixture.blank(CharacterFixture.BLANK).feats(new ArrayList<>());
}

/** Satisfies ARTISTA_MARCIAL's own "Força 2 e 2 Graduações em Ataque Corpo-a-Corpo". */
private static Character martialArtist() {
    return character()
            .attributes(CharacterAttributes.builder()
                    .strength(AttributeValue.builder().domain(AttributeDomain.STRENGTH).base(2).build())
                    .build())
            .skill(SkillType.ATAQUE_CORPO_A_CORPO, trained(new AtaqueCorpoACorpo(), 2))
            .build();
}
```

Three traps:

- **`CharacterFixture`'s `feats` is an immutable `List.of()`** — every such helper must swap in
  `new ArrayList<>()` or the first grant throws `UnsupportedOperationException`.
- **Meet the requirement exactly, not generously.** A character built with Força 5 when the
  Talento asks for 2 hides a typo in the constant's own `requiredAttributeValue`. Give the
  requirement's exact value; a separate test drops it one below to prove it blocks.
- **Attribute floors read `base`, not `getTotal()`** (unlike `ItemRequirements`) — putting the
  value in `variable` does not satisfy a Pré-requisito. Remember the `base` cap of 5 while you're
  there: if the constant asks for more, that is a finding, not a fixture problem.

For a Talento whose `requiredFeat` names a sibling, walk the whole chain through the service —
each rung acquired legally, in order. That is the only test that proves the ladder is walkable.

## 3. Acquire it the way a player does

```java
CharacterSheet sheet = CharacterSheet.of(character, new Player());
sheet.accumulateExperience(BigDecimal.TEN);

featService.grantFeat(character, sheet, ArtesMarciaisFeat.ARTISTA_MARCIAL);
```

**Use `FeatService#grantFeat` in the objective test, not `Character#grantFeat`.** The plain
mutator validates nothing — it is the right tool in an unrelated test that just needs the
Talento present (`DamageBaseServiceTest` uses it correctly, since the subject there is the
service's scan), but in a *Talento's own* test it silently skips the two things that make the
Talento reachable at all: the prerequisite check, and the XP spend.

Assert the wallet too, at least once per tree: `getUnUsedExperience()` fell by exactly
`character.getRace().getNewFeatCost(feat.getFeatCategory())`. And assert the rejection path
leaves the wallet untouched — `CharacterSheet#useExperience` had a real bug here once.

## 4. Assert the objective as a before/after delta

Read the number *before* the grant, grant, read it again. A delta survives an unrelated change
to the fixture's baseline; a hardcoded total does not, and a test that pins the total is the one
that gets "fixed" by pasting in whatever the code now prints.

```java
@Test
void acquiringArtistaMarcialRaisesTheDanoBaseTheCharacterSwingsWith() throws IllegalOperationException {
    Character character = martialArtist();
    DamageBase before = damageBaseService.getDamageBase(character, ESPADA);

    featService.grantFeat(character, sheetWith(character, BigDecimal.TEN), ArtesMarciaisFeat.ARTISTA_MARCIAL);

    assertEquals(before.scaledUp(1), damageBaseService.getDamageBase(character, ESPADA));
}
```

Use the domain type's own advance operation (`DamageBase#scaledUp`, `BranchLevel#advancedBy`)
rather than arithmetic on a raw int, so the assertion still reads correctly across an overflow
row or a clamp.

Where a clause **scales off the holder's own state** ("+1 para cada Título Aventyr Desperto",
"metade de suas Graduações em Domínio do Mana"), test at least two points on that scale from
within the character — one with the term at zero, one with it non-zero. One point can't tell a
scaling term from a constant.

## 5. The controls

- **The neighbouring stat.** A DM clause must leave DF alone; an Ataque Corpo-a-Corpo grant must
  leave the Ataque à Distância swing alone. Read both, assert one moved and one didn't.
- **Every other constant in the tree returns zero.** Loop `values()`, skip the one under test,
  assert `0` from each hook. This is what stops a copy-pasted override landing on the wrong
  constant.
- **A gap-blocked Talento changes nothing at all.** Grant it and assert every service still
  reports its earlier number — see `MetamagicoFeatIntegrationTest
  #aTalentoWithNoRealClauseChangesNothing`. That is the correct, honest test for a TODO'd clause:
  it pins today's behaviour and fails the day someone wires it up half-way.

Never assert on a TODO'd clause's intended-but-unbuilt effect, and never weaken an assertion to
accommodate one.

## 6. Race-scoped cost: `<Race>FeatCostTest`

A `Race` that overrides `Race#getNewFeatCost(FeatCategory)` changes what every Talento of a
category costs its members — a character-level effect like any other, and it needs its own test
class named for the race, in `org.aventyrs.core.feat`. `Gigantes` (Sobrevivência at 2 instead of
`Race.BASE_NEW_FEAT_COST` 3) is the only one today; `GigantesFeatCostTest` is the worked example.

The race's own `<Race>Test` already asserts the bare lookup. This class covers the three things
it cannot:

1. **Every relevant authored Talento**, driven off the catalog rather than a hand-listed set —
   iterate `FeatCatalog.in(<discounted category>)` asserting the reduced cost, and
   `FeatCatalog.all()` asserting every *other* category still pays the base. Written that way it
   covers a Talento added years later automatically, and passes vacuously while a category has no
   constants yet.
2. **The discount reaching the wallet**, end to end through `FeatService#grantFeat` — the lookup
   being right proves nothing if `grantFeat` charges a different number.
3. **The discount not leaking**, by granting the same Talento to a character of the same build
   whose only difference is `race`, and asserting the two wallets differ by exactly the discount.

Cover `getAffordableFeats` too where the discount changes the answer: an XP balance that affords
a discounted Talento for this race and nothing for another is the sharpest statement of what the
override is for.

When the discounted category has no authored Talento yet, prove the end-to-end path with a
homebrew `AbstractFeat` in that category (`AbstractFeat` is `non-sealed` with a public
constructor for exactly this). Say so in a comment, so the day a real one lands the reader knows
to point the test at it.

## Checklist

- [ ] Catalog shape: every constant has a non-blank description, non-null requirements, the
      tree's own `FeatCategory`; the constant count is pinned.
- [ ] Prerequisites: each half independently unmet rejects through `grantFeat`; all met accepts;
      an unset half never blocks; a `requiredFeat` chain is walked in order.
- [ ] Objective: a before/after delta per real clause, read from the consuming service, on a
      character who acquired the Talento through `FeatService`.
- [ ] Scaling terms tested at two points.
- [ ] Controls: the neighbouring stat, every other constant at zero, gap-blocked constants
      changing nothing.
- [ ] XP: the exact cost is spent; a rejected grant leaves the wallet untouched.
- [ ] `<Race>FeatCostTest` if a Race discounts this Talento's category.

## Reference files to read first

- `src/test/java/org/aventyrs/core/feat/ArtesMarciaisFeatTest.java` — the worked example of the
  whole shape: eligibility through the service, the objective as a `DamageBase` delta, the
  Título-count scaling term at two points, and the Ataque à Distância control.
- `src/test/java/org/aventyrs/core/feat/GigantesFeatCostTest.java` — the race-cost shape of
  step 6, catalog-driven.
- `src/test/java/org/aventyrs/core/feat/MetamagicoFeatIntegrationTest.java` — one objective test
  per real clause across four different consuming services, plus the
  "a gap-blocked Talento changes nothing" control.
- `src/test/java/org/aventyrs/core/feat/MetamagicoFeatTest.java` — the second layer: formula
  edge cases (rounding down, untrained reads as 0) and the every-other-constant-is-zero loops.
- `src/test/java/org/aventyrs/core/character/services/FeatServiceImplTest.java` — the
  requirement-isolation shape, and `getAvailableFeats`/`getAffordableFeats`.
- `src/main/java/org/aventyrs/core/feat/Feat.java` — the five hooks a clause can land on, and
  `isEligible`'s combine-every-set-prerequisite rule.
- `.claude/skills/adding-a-feat/SKILL.md` — authoring the constant this skill then tests.
