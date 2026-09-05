---
name: adding-a-race
description: This skill should be used when the user asks to "add a new Raça", "add a new Race", "add [race name] to org.aventyrs.core.race", "add a Mestiço/mixed-blood race", "add a racial ability/Habilidade Racial/Característica Racial", or gives the rules-text block for a playable race (its Atributos Raciais, Categoria de Tamanho, Idiomas, Longevidade, Talentos gratuitos and Características Raciais). Walks through picking the right Race shape (stateless, Mestiço, or Elemental Mestiço), overriding only the hooks the rules text actually reaches, deciding whether the race needs a `*RacialAbility` enum at all, and the javadoc gap-list discipline — mirroring `Anao`/`Elfo`/`MeioElfo`/`Colosso` as the reference implementations.
---

# Adding a Raça (Race)

A `Race` lives in `org.aventyrs.core.race` — a **sibling** of `org.aventyrs.core.character`,
not a subpackage of it, mirroring how `org.aventyrs.core.ability`/`org.aventyrs.core.feat`
already sit alongside `character`. `Character` holds a `Race race` field and `Race
#generateEmptyCharacter` returns a `Character.CharacterBuilder`; the two packages reference
each other, which is an ordinary mutual class dependency, not a layering violation.

There are ~22 races today. Nearly all of the rules text a race presents lands on a
**deliberately unimplemented** gap — so the single most important habit in this skill is
knowing which two or three clauses are mechanically real and recording the rest honestly.

## 0. Pick the shape first

Three shapes exist, and picking wrong costs a rewrite:

| Shape | When | Reference |
| --- | --- | --- |
| **Stateless** `implements Race` | The race carries no player choice at creation | `Human`, `Anao`, `Elfo`, `Monstruoso` |
| **Instance-based** `implements Race` | Mestiço with a bespoke rule set of its own | `MeioElfo` |
| `extends AbstractMesticoRace` | One of the 6 Mestiços Elementais | `Colosso`, `Agastias` |

Most races are stateless — a plain class with no fields, whose overrides return constants.
Only reach for an instance-based shape when the rules text makes the *player* pick something
at creation ("escolha uma raça...", "seus parentes mortais").

**A Mestiço carrying a choice is the `ArtesAprimorarComArteAbility` pattern, not
`AcquiredChoice`** — the choice feeds this object's *own* `getFixedAttributeBonuses()`/
`getCreatureType()`/`generateEmptyCharacter`, so it's a constructor field on the class.
`AcquiredChoice` is for a choice consumed by some *other* mechanism.

This needs **zero** changes to `CharacterCreationServiceImpl` or the creation-flow steps in
`character.services`' `package-info.java` — Step 1 ("Pick a Race") just becomes
`new MeioElfo(parentRace)` instead of `new Human()`.

## 1. Read the rules text, then triage every clause

Get the actual block before modeling anything. Race rules text is mostly narrative, and the
same handful of clauses recur across every race with the same verdict. Triage against this
table before writing a line:

| Clause | Verdict |
| --- | --- |
| Atributos Raciais (fixed) | **Real** — `getFixedAttributeBonuses()` |
| Atributos Raciais (player picks N points among a set) | **Real** — `getChoosableAttributeBonusPoints()` + `getChoosableAttributes()` |
| Categoria de Tamanho | **Real** — `getBaseSizeCategory()` + `generateEmptyCharacter` |
| Feérico / Humanoide / Monstruoso | **Real** — `getCreatureType()`, no default, always override |
| Renascido / Morto-Vivo, "para pré-requisitos conta como sua raça em vida" | **Real** — `getCreatureType()` returns `CreatureType.RENASCIDO`, and override `getPrerequisiteCreatureType()` to the life-race's type (default is `getCreatureType()`). `Vampiro` is the reference. The Morto-Vivo *behaviours* (healing inversion, no sleep/breath, damage-type immunity) are all still gaps. |
| Arma Natural every member is born with | **Real** — `getGrantedNaturalWeapons()` → `List<NaturalWeapon>`, folded into `Character#getNaturalWeapons()` with the `Feat` twin (`Vampiro`'s per-lineage Presas Longas/Garras Afiadas). A per-*form* natural weapon (`HomemFera`) still needs the form state. |
| "Talentos [de tipo X] custam N EXP" | **Real only if N is a whole number** — `getNewFeatCost(FeatCategory)` |
| "Aprender magias custa N EXP a menos" | **Real** — `resolveSpellAcquisitionCostReduction(Character, Spell)` → `BigDecimal` (fractional is fine, unlike feat cost); `SpellService#getAcquisitionCost` sums it with the `Feat` twin and floors. `Agastias`' "Magia é Ciência" is the reference. A scope of "de seu elemento" / "Naturais" is still a gap (no per-Magia element column; type is checkable via `Spell#getPrimaryType`). |
| A conditional roll bonus / Vantagem | **Real** — a `*RacialAbility` constant, see step 3 |
| **Idiomas** | Gap — no Language/Idioma concept exists anywhere in this core |
| **Longevidade / idade** | Gap — no age/lifespan concept on `Character` or `Race` |
| **Talento gratuito** | Gap — `Race` has no hook to grant a `Feat` at creation |
| **Treinamento/Especialização em Perícia inicial** | Gap — `Race` has no hook for granting starting Perícia training; `CharacterCreationServiceImpl` allocates only Attributes/Egos |
| **Uma Habilidade de Competência/Atributo extra** | Gap — "grant an extra acquisition slot" has no shape (`Elfo`'s Origem Mística, `Anao`' Pequenos Gigantes) |
| **Visão no Escuro / sentidos** | Gap — no vision/senses concept |
| **"1x por sessão"** | Gap — no game-session concept |
| **Talentos custam 2.5 EXP** | Gap — `getNewFeatCost` returns `int`; a fractional cost can't be represented |

`Gigantes`' Talentos de Sobrevivência at 2 XP is the one feat-cost override that *is* real,
precisely because 2 is a whole number. `Elfo`'s Conexão com o Mana, `Human`'s and
`Pequenino`'s Adaptação all want 2.5 and are therefore all still TODO'd — don't round one to
2 or 3 to make it fit.

**Tendência is deliberately left unconstrained on every race.** Rules text saying a race
"raramente"/"normalmente" holds some tendência is advisory, not a hard rule;
`Character#getTendencia()` stays a plain unvalidated 1-10 value. Don't add validation.

## 2. Implement the `Race` hooks

`Race` has exactly **one** method with no default — everything else is opt-in:

```java
public class Anao implements Race {

    @Override
    public CreatureType getCreatureType() {          // no default: always override
        return CreatureType.HUMANOIDE;
    }

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        return Map.of(AttributeDomain.VIGOR, 1, AttributeDomain.GNOSE, 1);
    }

    @Override
    public SizeCategory getBaseSizeCategory() {
        return SizeCategory.MINUS_ONE;
    }

    @Override
    public Character.CharacterBuilder generateEmptyCharacter(final List<DlcRuleset> dlcRulesetList) {
        return Character.builder().sizeCategory(getBaseSizeCategory());
    }

    @Override
    public List<SkillCompetencyAbility> getRacialAbilities() {
        return List.of(AnoesRacialAbility.ABATEDORES_DE_GIGANTES, AnoesRacialAbility.FILHOS_DA_MONTANHA);
    }
}
```

Two shape rules that are easy to get wrong:

- **`getBaseSizeCategory()` and `generateEmptyCharacter` are not redundant.** Override
  `getBaseSizeCategory()` rather than hardcoding the literal a second time inside
  `generateEmptyCharacter` — then have `generateEmptyCharacter` call it, exactly as above. A
  Mestiço race reads its parent's `getBaseSizeCategory()` directly, without assembling a whole
  `Character` just to read one field back, and that only works if every race reports it
  honestly. A `SizeCategory.ZERO` race needs neither override (`Human`/`Elfo` return a bare
  `Character.builder()`).
- **Overriding nothing is a valid, deliberate answer.** `Human` overrides only
  `getCreatureType()`/`generateEmptyCharacter`, because Humanos *are* the size/attribute
  baseline — "não recebem pontos de atributos adicionais", "Categoria de Tamanho 0". That's a
  match to the rules text, not an oversight, and `Human`'s javadoc says so explicitly. Say so
  in yours too when it applies.

For a choosable-attribute race, `getChoosableAttributeBonusPoints()` and
`getChoosableAttributes()` only mean anything together — `CharacterCreationServiceImpl
.allocateAttributes` validates the player's chosen map against both (every key must be in
`getChoosableAttributes()`, no negative values, and the total must equal the points exactly).
See `Elfo` (+1 Destreza fixed, plus 1 free point between Foco and Gnose).

## 3. Racial abilities — and when *not* to build the enum

A trait every member of a race automatically gets contributes to a roll the **exact** same way
a player-acquired `SkillCompetencyAbility` does, so it's modeled as one rather than a parallel
duplicate type. `getRacialAbilities()` returns `List<SkillCompetencyAbility>`, differing from
`Character#getSkillCompetencyAbilities()` only in *where* it's sourced from — fixed per race,
not a per-Character acquisition — never in how a consumer treats its entries.

**This is already wired generically.** `AbstractSkillInteraction.allSkillCompetencyAbilities`
concatenates `character.getRace().getRacialAbilities()` into the acquired list for the
`@Modifier`-based `skillRollBonus` sum, the `getDifficultyReduction()` sum,
`resolveAttributeDomain`, and `validateRequestedTrait`. A new constant works for every
character of that race with **no Interaction-specific code at all**.

### Don't force an enum into existence for symmetry

**Having no racial abilities is by far the common case.** Of the 29 races in this package,
**seven** override `getRacialAbilities()` with real content — `Anao`, `Elfo`, `Aviano`,
`Goblin`, `Guampo`, `HomemFera`, and the Mestiços (which inherit rather than author). Every
other race leaves it at `Race`'s empty default, because none of their traits fit the shape: some
aren't roll-conditioned at all (a Defesa modifier, a conditional RD), one needs a target
classification this core can't make, and several are really "grant an acquisition slot" traits.
`Ogro`, `Indomito` and `Troll` are the three most recent races to add none at all, and each says
why in its javadoc.

**Only build a `<Race>RacialAbility` enum once a trait's shape genuinely fits** — there are six
today (`AnoesRacialAbility`, `ElfosRacialAbility`, `AvianosRacialAbility`,
`GoblinsRacialAbility`, `GuamposRacialAbility`, `HomensFeraRacialAbility`). If the race has none,
the javadoc says why (see `Human`'s closing paragraph) and you are done. Don't create one for
symmetry.

**A racial ability is not limited to roll bonuses.** `GuamposRacialAbility#VIGOR_DE_EPONA` grants
flat RD and `HomensFeraRacialAbility#FORTALECIMENTO_FERAL` flat Movimento Base — both ordinary
`@Modifier` methods, both reaching their service because `DamageServiceImpl` and
`MovementServiceImpl` scan `SkillCompetencyAbility.allFor`. When a clause grants a stat rather
than a roll bonus, check that the consuming service uses `allFor` and not
`character.getSkillCompetencyAbilities()` alone before assuming it is a gap.

**A Característica that is neither a roll bonus nor a stat may still have its own `Race` hook.**
`Race#getCriticalEffectImmunities()` is the one that exists (`Troll`'s Anatomia Vegetal); an
immunity is a filter, not a modifier, so forcing it into a `*RacialAbility` enum would have been
the wrong shape.

### Writing the constants

The enum is `@Getter @AllArgsConstructor`, implements `SkillCompetencyAbility`, holds a
`description`, and declares one enum-level `getSkillType()` that constant bodies override where
they differ. Pick the hook by what the clause is conditioned on:

| Conditioned on | Hook |
| --- | --- |
| Nothing (unconditional Vantagem on one Perícia) | plain `@Modifier(ModifierType.<SKILL>_ROLL_BONUS)` returning `Skill.ADVANTAGE_BONUS` |
| The attack target (its size, etc.) | `resolveAttackRollBonus(CombatantSheet actor, CombatantSheet attackTarget)` |
| The Scene (terrain, proximity) and/or the requested trait | `resolveConditionalRollBonus(SceneContext, SkillTrait requestedAbility)` |
| Substituting the Perícia's base Attribute | `getSubstituteAttributeDomain()` |

`ElfosRacialAbility.SENTIDOS_ABSOLUTOS` is the unconditional case — an ordinary
`@Modifier(ModifierType.ATTENTION_ROLL_BONUS)` method, granted the same flat-Vantagem way as
any acquired ability. **Vantagem is a flat +2 (`Skill.ADVANTAGE_BONUS`), never a reroll.**

`AnoesRacialAbility` carries both harder cases and is worth reading in full:

- `ABATEDORES_DE_GIGANTES` — Vantagem on Ataque rolls against a target 2+ Categorias de Tamanho
  larger. Its rules text covers **every** Perícia de Ataque, so `getSkillType()` stays a single
  representative value (`ATAQUE_A_DISTANCIA`) while **`SkillTrait#matchesSkillType` is
  overridden** to `requestedSkillType.isAttackSkill()` (that default lives on `SkillTrait`, and
  `validateRequestedTrait` calls it instead of raw equality), so a `SkillRoll#requestedAbility`
  naming it
  validates against either attack Perícia. `resolveAttackRollBonus` is scanned unconditionally
  with no per-`skillType` filter, so the representative value doesn't otherwise matter. It
  reaches both attack Perícias because the `attackTarget`-aware 4-arg `applyTo` now lives on
  `AbstractSkillInteraction` gated on `isAttackSkill()`, not on `AtaqueADistanciaInteraction`.
- `FILHOS_DA_MONTANHA` — Vantagem on Conhecimentos: Natureza while in mountain/cave terrain.
  It overrides `getSkillType()` to its real Perícia and gates on **both**
  `sceneContext.isTerrain(MOUNTAIN, CAVE)` **and** `requestedAbility ==
  ConhecimentosSpecialization.NATUREZA`. This ability is never itself the `requestedAbility` —
  the Especialização is — so it only fires once the roll requests one the character genuinely
  holds.

Both return `Optional.empty()` on a null argument or an unmet condition; `resolveAttackRollBonus`
results are **summed** across every ability that grants one, unlike `resolveDamageBonus` where
callers take the first non-empty.

If a clause's Vantagem is scoped to a narrative *purpose* rather than a named skill or a
trackable condition, that can't be modeled — this core doesn't track what a roll is *for*.
Document the simplification on the constant rather than silently narrowing or over-granting.

## 4. Mestiço races

A Mestiço picks a parent `Race` at creation. Two invariants, both constructor-enforced with
`IllegalOperationException`:

- **`INVALID_PARENT_RACE`** — `parentRace.isMestico()` is rejected ("que não seja mestiça"), so
  Mestiços never chain. `MeioElfo` additionally rejects a non-`HUMANOIDE` parent; `Vampiro`
  rejects a parent whose `CreatureType` is outside the chosen sub-raça's allowed set (its
  `VampiroLineage` carries a `Set<CreatureType>`), and an Atributo-Herdado pick the parent
  doesn't grant.
- **`INVALID_INHERITED_RACIAL_ABILITIES`** — at most 2, and every one must actually belong to
  `parentRace.getRacialAbilities()`.

Override `isMestico()` to `true` — that flag is what the *next* Mestiço's validation reads.

**A Mestiço with a second in-race choice** (`Vampiro`'s sub-raça, `Agastias`' Linhagem) declares
that as a nested enum on the race class, carrying whatever authored data the choice fixes — for
`Vampiro.VampiroLineage`, the +1 Atributo, the feeding Armas Naturais, and the allowed parent
types. The constructor takes the enum value alongside the parent `Race`.

**"2 Características Raciais aleatórias" does not mean this core rolls anything.** The same
discipline as `SkillRoll` applies: the constructor accepts up to 2 *already-externally-resolved*
abilities and returns them from `getRacialAbilities()`, where the generic scan picks them up
with no extra wiring. Most races have 0-1 real entries to draw from, so the pool is usually
short — that's a **data-catalog** gap, not a randomness gap. Say which in the javadoc.

`getInheritedAttributeAbilities()` (Físico Mortal) is the player's own choice, one per Atributo
the parent actually grants, validated against `parentRace.getFixedAttributeBonuses().keySet()`
with duplicates per domain rejected. **It has no automatic consumer** —
`Character.attributeAbilities` is a plain builder field, not derived from `Race` — so a caller
must read it and pass it into `Character.builder().attributeAbilities(...)` themselves.

### The 6 Mestiços Elementais

`AbstractMesticoRace` exists because these 6 genuinely share identical structure — a
`parentRace`, the same "+2, ou +3 se o parente conceder este Atributo" conditional, and the
identical Mestiço Mortal/Físico Mortal traits verbatim. **This shared base is the exception,
not the rule**: every other race is bespoke enough that a shared base would mostly hold empty
defaults. Don't extend it for a non-Elemental race.

A subclass supplies only its attribute pair and size offset:

```java
public class Colosso extends AbstractMesticoRace {
    private static final AttributeDomain PRIMARY_ATTRIBUTE = AttributeDomain.VIGOR;
    private static final AttributeDomain REDUCED_ATTRIBUTE = AttributeDomain.DEXTERITY;
    private static final int PRIMARY_BONUS = 2;
    private static final int PRIMARY_BONUS_WHEN_PARENT_GRANTS_IT = 3;
    private static final int REDUCED_BONUS = -1;
    private static final int SIZE_CATEGORY_OFFSET = 1;

    public Colosso(@NonNull final Race parentRace) {
        this(parentRace, List.of(), List.of());
    }

    public Colosso(@NonNull final Race parentRace,
                   @NonNull final List<SkillCompetencyAbility> inheritedRacialAbilities,
                   @NonNull final List<AttributeAbility> inheritedAttributeAbilities) {
        super(parentRace, inheritedRacialAbilities, inheritedAttributeAbilities);
    }

    @Override
    public Map<AttributeDomain, Integer> getFixedAttributeBonuses() {
        int primaryBonus = parentGrants(PRIMARY_ATTRIBUTE) ? PRIMARY_BONUS_WHEN_PARENT_GRANTS_IT : PRIMARY_BONUS;
        return Map.of(PRIMARY_ATTRIBUTE, primaryBonus, REDUCED_ATTRIBUTE, REDUCED_BONUS);
    }

    @Override
    protected int getSizeCategoryOffset() { return SIZE_CATEGORY_OFFSET; }
}
```

Always give the convenience 1-arg constructor delegating with two `List.of()`s, and name every
number as a `private static final`. Current values: primary +2/+3 and reduced -1 across all six;
offsets are 0 (Agástias, Aquan, Flaminídeo), +1 (Colosso, Invernal), -1 (Dólos).

`getCreatureType()` is **already delegated** to the parent on the base class — the rules text's
own words, "conforme sua metade não-elemental" — and `generateEmptyCharacter` is `final` there,
applying `parentRace.getBaseSizeCategory().shift(getSizeCategoryOffset())`. Don't re-override
either.

`Agastias` is the one carrying a *second* acquisition-time choice — a `Linhagem`
(`VULCANO`/`TROVEJANTE`) nested enum selecting which Attribute takes the -1. Follow it if a race
needs a similar in-race variant, declaring the enum nested in the race class.

## 5. Javadoc is where the gaps get recorded

Every race class carries a class-level javadoc naming, in order: which traits are mechanically
real (linking each to its method), then a `<ul>` with one `<li>` per unimplemented trait, each
stating **what the trait does** and **which specific missing system blocks it** — cite the gap
by name from the table in step 1, and cross-reference the other race that has the identical gap
("same shape and same gap as `Elfo`'s Origem Mística"). Read `Anao`'s or `MeioElfo`'s in full
for the reference tone before writing yours.

This isn't decoration: it's how the next person knows a missing override is deliberate.

## 6. Write tests

`src/test/java/org/aventyrs/core/race/` has one `<Race>Test` per race. Follow `AnaoTest`, which
sweeps every hook whether overridden or not:

- `generateEmptyCharacterSeeds<X>SizeCategory` — build a full `Character` through the returned
  builder (`.player(...).name(...).race(...).attributes(...).egos(...).actionProfile(...)`) and
  assert the seeded size.
- `hasFixed<X>And<Y>RacialBonuses` — the exact `Map`.
- `has<X>CreatureTypeAnd<Y>BaseSizeCategory`.
- `hasNoChoosableRacialBonuses` (or the positive form asserting points + eligible set).
- `usesTheBaseCostsForFeatsAndSkills` — assert against `Race.BASE_NEW_FEAT_COST`/
  `BASE_NEW_SKILL_COST` **by constant**, never the literals 3/2. Invert it for a race that
  really does override a cost.
- `isNotMestico` / the Mestiço equivalents.

For a Mestiço, additionally pin **both** rejection paths (`isMestico()` parent, and an
inherited ability the parent doesn't have), the `parentGrants` +2-vs-+3 branch in both
directions, and the size offset against a parent whose own base size isn't `ZERO`.

For a `*RacialAbility` enum, add `<Race>RacialAbilityTest` covering each constant's condition in
**both** directions — met and unmet — plus the null-argument guards, following
`AnoesRacialAbilityTest`.

## Reference files to read first

- `org.aventyrs.core.race.Race` — the interface; note `getCreatureType()` is the only
  non-default method.
- `org.aventyrs.core.race.Human` — the baseline, and the reference for "overriding nothing,
  deliberately".
- `org.aventyrs.core.race.Anao` — the fullest stateless race (fixed bonuses, size, two abilities).
- `org.aventyrs.core.race.AnoesRacialAbility` — both hard ability shapes.
- `org.aventyrs.core.race.AbstractMesticoRace` + `Colosso` + `Agastias` — the Elemental shape.
- `org.aventyrs.core.race.MeioElfo` — the bespoke Mestiço.
- `org.aventyrs.core.race.Vampiro` — the `RENASCIDO` split (`getPrerequisiteCreatureType()`), a
  race that grants Armas Naturais (`getGrantedNaturalWeapons()`), a nested sub-raça choice enum,
  and a sub-raça-constrained parent validation.
- `src/test/java/org/aventyrs/core/race/AnaoTest` — the test shape to copy.
