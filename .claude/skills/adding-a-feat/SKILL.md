---
name: adding-a-feat
description: This skill should be used when the user asks to "add a new Talento", "add a new Feat", "add [Feat name] to [tree]Feat", or references adding a new entry to org.aventyrs.core.feat. Walks through producing a catalog entry like `ArtesMarciaisFeat.ARTISTA_MARCIAL` — its requirements, its bonus, and the enum wrapping class — mirroring CLAUDE.md's "Adding a new Talento (Feat)" section.
---

# Adding a new Talento (Feat)

A Talento sits alongside skills/Títulos as an acquirable character trait, but it's a flat
catalog — one enum per Talento *tree*, named by `FeatCategory` (e.g. `ArtesMarciaisFeat` for
`FeatCategory.ARTE_MARCIAL`), mirroring `<Skill>CompetencyAbility`'s one-enum-per-domain shape.
Read `CLAUDE.md`'s "Adding a new Talento (Feat)" section first — it carries the full
architectural rationale; this skill is the operational checklist on top of it.

## 1. Read the rules text first

Get the Talento's name, its full Pré-requisito line, and its full Descrição verbatim. Parse
the Pré-requisito into whichever of these it actually names (a Talento rarely names all
three):
- An Attribute floor (e.g. "Força 2") → `attributeDomain`/`requiredAttributeValue`.
- A Perícia Graduação floor (e.g. "2 Graduações em Ataque Corpo-a-Corpo") →
  `requiredSkillType`/`requiredSkillGraduation`.
- Another specific Talento already held (e.g. "Requer Artista Marcial") → `requiredFeat`,
  pointing at that other Talento's own enum constant.

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

## 3. Classify the Descrição's mechanic

Same discipline as every other ability catalog in this codebase (see CLAUDE.md's "Adding a new
Perícia" TODO-writing convention, and the Título skill's own step 3):

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
  passive grants — see CLAUDE.md's "Adding a new Título Aventyr" section for the full list and
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

## 4. Wire `FeatService`

Nothing to build here — `FeatService#grantFeat(Character, CharacterSheet, Feat)` already
validates `Feat#isEligible` (which reads straight off `FeatRequirements`, no per-Talento code
needed) and spends `character.getRace().getNewFeatCost(feat.getFeatCategory())` XP generically.
A new Talento constant works through this service automatically the moment its
`FeatRequirements` are set correctly in step 2 — don't hand-write a new validation path.

## 5. Write tests

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

- CLAUDE.md's "Adding a new Talento (Feat)" section if the checklist itself changed.

## Reference files to read first

- `src/main/java/org/aventyrs/core/feat/ArtesMarciaisFeat.java` — the worked example this
  skill follows (a tree with one constant so far, `ARTISTA_MARCIAL`, with a real Dano Base
  formula method and no wired caller yet).
- `src/main/java/org/aventyrs/core/feat/Feat.java` — `isEligible(Character)`, the
  requirement-check mechanism every new Talento's Pré-requisito plugs into via
  `FeatRequirements`.
- `src/main/java/org/aventyrs/core/feat/FeatRequirements.java` — the four prerequisite fields
  (`attributeDomain`/`requiredAttributeValue`, `requiredSkillType`/`requiredSkillGraduation`,
  `requiredFeat`); leave any subset unset when the rules text doesn't name it.
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
  "compute the real formula even with no caller yet" precedent step 3 follows.
