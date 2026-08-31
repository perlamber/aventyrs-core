---
name: ability-acquisition-and-substitution
description: This skill should be used for any work on an ability that makes the player pick a value when acquired (`ArtesAprimorarComArteAbility`-style instance classes, `AcquiredChoice<C>`/`AbilityChoiceService`, or one-enum-constant-per-option like `PeritoTeoricoAbility`), or on letting a Perícia use a different base Attribute — `SkillCompetencyAbility#getSubstituteAttributeDomain()` (unconditional) / `resolveSubstituteAttributeDomain(AttackSource)` (delivery-scoped), `SkillCompetencyAbility.resolveAttributeDomain(...)`, `CharacterSkillService.getValueForRoll`'s substitute overload, `AttackSource` (the `Weapon`/`Spell` delivery channel, `getAttackSkillType`), or the 5-arg `applyTo` overload. Also use it when asked why `ACUIDADE` widens the Graduação cap but `ARREMESSO_PODEROSO` doesn't, why `AttackSource` is the 5th `applyTo` parameter, or which of the three acquisition-choice patterns to use.
---

# Acquisition-time ability choices, and Perícia base-Attribute substitution

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
not a cascade.

- `SkillCompetencyAbility` carries a `default Optional<AttributeDomain>
  getSubstituteAttributeDomain()` returning `Optional.empty()`. Only override it on a constant
  whose rules text grants the substitution unconditionally, per-constant (an enum-constant body,
  exactly like `AtaqueCorpoACorpoExcellency.PRODIGIO`'s `getDifficultyReduction()` override) —
  never at the enum type level.
- `CharacterSkillService.getValueForRoll` has a second overload taking an extra
  `AttributeDomain substituteAttributeDomain` parameter — `null` means "use the Skill's own
  `getAttributeDomain()`, as before", non-null overrides it. The service itself never scans
  abilities — it only ever receives a resolved value.
- Resolving *which* Attribute (if any) applies is `SkillCompetencyAbility
  .resolveAttributeDomain(skillCompetencyAbilities, skillType, defaultDomain[, AttackSource])`'s
  job — a static method on the interface, mirroring `SkillExcellency.unlockedBy`'s shape. It
  filters for entries whose `getSkillType()` matches `skillType` and whose
  `resolveSubstituteAttributeDomain(attackSource)` is present, returning the first match's
  Attribute or `defaultDomain` if none. **First match wins and the rules name no precedence**
  when a character holds two substituting abilities for one Perícia (Ataque Corpo-a-Corpo's
  `ACUIDADE`/`SAGACIDADE_ARCANA` are that pair today). It's called unconditionally by
  `AbstractSkillInteraction.applyTo` for every skill — safe even for skills with no substituting
  ability, since it falls through to `defaultDomain`.
- **The Graduação cap deliberately calls the 3-arg form**, passing no `AttackSource`, so only
  *unconditional* substitutions widen it (see the `attribute-graduation-progression` skill).
  `SkillGraduationService` asks which Attribute currently **governs** the Perícia; a
  delivery-scoped substitution governs only some of its rolls. `ACUIDADE` widens the cap,
  `ARREMESSO_PODEROSO` doesn't — that asymmetry is the point, and
  `SkillGraduationServiceImplTest` pins both directions.
- `GnoseAbility.PERITO_TEORICO` is a different shape: which Attribute to substitute is fixed
  (Gnose), but *which Perícia* it applies to is a per-character choice — but every legal option
  is already known at compile time (every `SkillType`), so it's wired via
  `org.aventyrs.core.ability.PeritoTeoricoAbility` — one `AttributeAbility` constant *per*
  `SkillType` (pattern 3 above). Its own static
  `resolveAttributeDomain(Collection<AttributeAbility>, SkillType, AttributeDomain)` mirrors the
  `SkillCompetencyAbility.resolveAttributeDomain` shape, and both `AbstractSkillInteraction
  .applyTo` and `SkillGraduationServiceImpl.getMaxGraduation` consult it first, feeding its
  result in as `resolveAttributeDomain`'s `defaultDomain` parameter — so a `SkillCompetencyAbility`
  substitution still wins if one somehow also targets the same Perícia, PERITO_TEORICO's Gnose
  applies otherwise, and the Perícia's own natural Attribute applies if neither does.
- **`AttackSource` (`org.aventyrs.core.skill`) is the delivery channel, and it's an interface
  `Weapon` and `Spell` implement directly** — there is no wrapper type between them and a roll,
  and don't reintroduce one. Its single member is `getAttackSkillType()`, which is not a new
  column: `Weapon#getSkillType()` (delegated to by a `default`, so the two can't drift) and
  `Spell#getAttackSkillType()` both already carried it. Only `Weapon` extends it, never `Item`.
- **A hook narrows by `instanceof`, not by asking the interface.** There is no `isThrown()`/
  `isWeapon()`: which `ItemCategory` values count as "arremesso" is one clause's reading and the
  next clause's would differ, so the test lives on the constant — `ARREMESSO_PODEROSO` is
  `instanceof Spell || (instanceof Weapon w && w.getCategory() == THROWABLE)`, the same way
  `FRIEZA` holds both the amount and the `Range` condition of its own bonus.
- **`null` means the caller didn't say**, and an `instanceof` chain reads that as "no scope
  matched" for free — no null branch needed. It does *not* mean "unarmed": an Ataque Desarmado
  has no representation, deliberately, because nothing consumes one yet.
- It reaches the roll as the **fifth and last** `applyTo` parameter, and that placement is
  forced: the resolved `AttributeDomain` feeds `getValueForRoll`, both `sumAttributeDomain*`
  scans, and — decisively — `consumeFirstRollThisTurn(domain)`, which is *stateful*. A domain
  resolved after the fact can't un-consume a Turn's first roll, so the usual "layer it onto the
  result" trick (how the `attackTarget` half still works, see `applyAttackTargetBonuses`) is
  unavailable here. Being a parameter rather than a field on `SkillRoll` also keeps the
  substitution visible on `AttackDelivery`'s bonuses-only preview path. Callers pass the
  `Weapon`/`Spell` itself — `.attackSource(ADAGA_DE_ARREMESSO)`, not a wrapper around it.
- **Because the logic moved to the 5-arg overload, a subclass must override *that* one.**
  `ArtesInteraction` was moved from the 3-arg accordingly, even though Artes reads neither new
  parameter — an override left on a shorter overload is silently skipped by any caller using a
  longer one. `EsquivaEApararInteraction`'s own 4-arg `(..., DefenseType)` is a separate
  signature rather than an override and needed no change.
- Reachable end-to-end from `DeliveredAttack#attackSource` (via `AttackDelivery`) and
  `SkillRollRequest#attackSource` (via `SkillInteractionFactory`), both optional.
  **`SpellCastingService` is the one path it does *not* reach**: `castSpell` takes an
  already-built delivery `Interaction` and runs it through `receiveInteraction` (the 1-arg
  `applyTo`), so there's no seam — closing that needs `castSpell` to take the `Spell` itself,
  which the missing target-GD resolution will force anyway (see the `magic-system` skill).
- Building this mechanism doesn't retroactively finish every ability that cites it — check
  each constant's own TODO. `ACUIDADE`, `SAGACIDADE_ARCANA`, `ACROBATA`, `DISPARO_ARCANO`,
  `MAGIA_SELVAGEM` and `ARREMESSO_PODEROSO` are fully wired (enum override + `Interaction`
  filter + service overload) — see `AttributeSubstitutionFeatureTest` for an end-to-end test on
  one Character, including a control Perícia (Persuasão) proving none leaks into an unrelated
  roll. `AttentionCompetencyAbility.ALMA_DE_SHERLOCK`'s substitution half,
  `EmpatiaSelvagemCompetencyAbility.ACADEMICO_SELVAGEM`/`INSTINTO_ANIMAL`,
  `FurtividadeCompetencyAbility.LADINO_TEORICO`, and `PersuasaoCompetencyAbility.FORCA_OPRESSORA`
  still need the same three-piece wiring applied to their own constant and `<Skill>Interaction`.

## Reference files to read first

- `src/main/java/org/aventyrs/core/skill/artes/ArtesAprimorarComArteAbility.java` — pattern 1.
- `src/main/java/org/aventyrs/core/ability/AcquiredChoice.java` /
  `src/main/java/org/aventyrs/core/character/services/AbilityChoiceService.java` — pattern 2.
- `src/main/java/org/aventyrs/core/ability/PeritoTeoricoAbility.java` — pattern 3, and the
  Gnose-substitution shape.
- `src/main/java/org/aventyrs/core/skill/SkillCompetencyAbility.java`
  (`getSubstituteAttributeDomain`, `resolveSubstituteAttributeDomain`, `resolveAttributeDomain`).
- `src/main/java/org/aventyrs/core/skill/AttackSource.java`.
- `src/main/java/org/aventyrs/core/skill/AbstractSkillInteraction.java` — the 5-arg `applyTo`.
- `src/test/java/org/aventyrs/core/**/AttributeSubstitutionFeatureTest.java`.
