---
name: attribute-graduation-progression
description: This skill should be used for any work on raising an Attribute's `base` or a Perícia's Graduação and spending XP for it — `CharacterAttributeService#upgradeBase` (cap 5, cost target+1), `SkillGraduationService#upgradeGraduation` (cap = 2×governing-Attribute base, cost = half target Graduação, a `BigDecimal`), `getMaxGraduation`, `CharacterSheet#useExperience`/`unUsedExperience`, or `CharacterAttributeService.MAX_ATTRIBUTE_BASE` / `SkillGraduationService.GRADUATION_TO_ATTRIBUTE_BASE_MULTIPLIER`. Also use it when asked why foes bypass the caps, why fixtures/tests put an Attribute total above 5 in `variable` not `base`, why existing `<Skill>InteractionTest` files call `increaseGraduation` directly, or how an unconditional Perícia base-Attribute substitution widens the Graduação cap.
---

# Attribute `base` / Perícia Graduação: hard caps, and both upgrades cost XP

An Attribute's `base` is capped at 5; a Perícia's Graduação is capped at twice the `base` of
whichever Attribute currently governs it. Raising either one point costs XP, at a different
formula per case, and can only happen with a `CharacterSheet` in hand (that's where
`unUsedExperience` lives) — this is a Character-*progression* action, not a pure computation
on a bare value. CLAUDE.md's "Recurring conventions" — especially **builder-bypassable
invariants** — apply.

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
  granting an unconditional substitution (see the `ability-acquisition-and-substitution`
  skill), the substituted Attribute's `base` is what the cap uses instead —
  `SkillGraduationServiceImpl.getMaxGraduation` resolves this via the same
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
  both parameters, unlike `DamageService`'s own methods (see the `damage-and-combat` skill for
  why those take a `CharacterSheet` alone and derive `Character` via `getCharacter()`).
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
builder-bypassability below is what makes it work. See the `damage-and-combat` skill's "Two
kinds of sheet" section and `org.aventyrs.core.monster`.

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

## Reference files to read first

- `src/main/java/org/aventyrs/core/character/services/CharacterAttributeService.java` /
  `CharacterAttributeServiceImpl.java` (`CharacterAttributeServiceTest.java`).
- `src/main/java/org/aventyrs/core/character/services/SkillGraduationService.java` /
  `SkillGraduationServiceImpl.java` (`SkillGraduationServiceImplTest.java`) — `getMaxGraduation`,
  `upgradeGraduation`.
- `src/main/java/org/aventyrs/core/character/AttributeValue.java` — the three components.
- `src/main/java/org/aventyrs/core/sheet/CharacterSheet.java` (`useExperience`,
  `unUsedExperience`; `CharacterSheetTest.java`).
- `src/main/java/org/aventyrs/core/rest/RestService.java` — the `(Character, CharacterSheet)`
  split this mirrors.
