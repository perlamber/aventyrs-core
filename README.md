# Aventyrs Core

The rules engine for the **Aventyrs** tabletop RPG — a pure Java library that answers
"given this character's current state, what are the numbers?" for skill rolls, combat,
damage, movement and character progression.

No framework, no persistence, no I/O. Just the rules.

---

## 1. What this is

A dependency-light calculator that a game UI, an API layer, or a VTT integration imports
directly. It models characters, foes, Perícias (skills), combat exchanges, temporary effects
and scenes — and computes the inputs to every roll.

Rules terminology stays in **Portuguese** (Perícia, Vantagem, Rodada, Dano Base); all code
identifiers are in **English**.

## 2. The one design principle

**This core never rolls dice, never does geometry, and never decides success or failure.**

It hands you a bonus; you add it to your own dice result. It tells you how many steps to shift
a difficulty; you compare against your own target. Distances, initiative values and dice
results all arrive *already resolved* from the caller.

That boundary is what keeps the library a pure function of character state — and it is
deliberate, not an unfinished feature.

## 3. Requirements

- **Java 21** (toolchain-enforced; consumers must also be on 21+)
- Gradle 9.x via the bundled wrapper — no local Gradle install needed

## 4. Installing

Published as `org.aventyrs.core:aventyrs-core`. Publish locally with:

```bash
./gradlew publishToMavenLocal
```

Then consume it:

```groovy
repositories { mavenLocal() }
dependencies { implementation 'org.aventyrs.core:aventyrs-core:0.0.22' }
```

## 5. Building and testing

```bash
./gradlew build              # compile, test, and enforce the coverage gate
./gradlew test               # tests only
./gradlew jacocoTestReport   # coverage report -> build/reports/jacoco
```

The build enforces a **85% JaCoCo coverage floor** (`check` depends on
`jacocoTestCoverageVerification`). Tests are JUnit 5 with Fixture Factory for test data.

> `./gradlew javadoc` currently fails on pre-existing Lombok/delombok errors. Unrelated to the
> library itself; it needs a delombok step to fix.

## 6. The model at a glance

| Concept | Values |
| --- | --- |
| **Attributes** | Vigor, Strength, Dexterity, Focus, Instinct, Gnose, Charisma |
| **Egos** | Autocontrole, Recursos, Sorte, Iniciativa |
| **Perícias** | 14, incl. Atenção, Artes, Atletismo, Ataque Corpo a Corpo/à Distância, Esquiva e Aparar, Furtividade, Domínio do Mana |
| **Status** | CLEAN → HIGH_LIFE → MEDIUM_LIFE → LOW_LIFE → FALLEN → COMMA → DEAD |
| **Races** | 22, including Mestiço (mixed-blood) variants |

A character also carries Talentos (feats), Títulos Aventyr, Vantagens de Ego, equipment, and
per-Perícia Especializações and Habilidades de Competência.

## 7. Creating a character

There's no single `createCharacter(...)` call — creation is an ordered sequence of validated
choices assembled through the builder:

```java
CharacterCreationService creation = new CharacterCreationServiceImpl();
Race race = new Human();

CharacterAttributes attributes = creation.allocateAttributes(race, basePoints, Map.of());
CharacterEgos egos = creation.allocateEgos(Map.of(EgoDomain.AUTOCONTROLE, 1));

Character.CharacterBuilder builder = Character.builder()
        .name("Aria").race(race)
        .attributes(attributes).egos(egos)
        .actionProfile(ActionProfile.ESTRATEGISTA);

if (creation.isEgoAdvantageAvailable(EgoDomain.AUTOCONTROLE, egos)) {
    builder.egoAdvantage(EgoDomain.AUTOCONTROLE, AutocontroleAdvantage.RESOLUTO);
}

CharacterSheet sheet = CharacterSheet.of(builder.build(), player);
```

The authoritative, always-current step list lives in
`org.aventyrs.core.character.services/package-info.java`.

## 8. Rolling a Perícia

Either construct the concrete `<Skill>Interaction`, or dispatch by `SkillType` when you only
have one in hand (e.g. deserializing a request):

```java
SkillRollRequest request = SkillRollRequest.builder()
        .skillType(SkillType.ARTES)
        .target(sheet)
        .skillRoll(new SkillRoll(List.of(4, 5, 6)))  // your dice, already rolled
        .build();

InteractionResult result = SkillInteractionFactory.resolve(request);

int bonus     = result.getSkillRollBonus();       // add to your roll
int reduction = result.getDifficultyReduction();  // GD steps to shift easier
```

`skillRollBonus` already folds in the governing Attribute, Graduação (or the untrained
penalty), and every applicable ability, Excelência tier and temporary bonus.

## 9. Sheets: players and foes

Combat-facing code should type against **`CombatantSheet`**, the shared interface:

- **`CharacterSheet`** — a player character; adds the XP wallet and Fama.
- **`MonsterSheet`** — a foe; adds the four authored stat-block numbers (DF, DM, attack GD,
  attack bonus) it presents *because it never rolls*.

Progression services take `CharacterSheet` specifically, so **a monster cannot be levelled up —
enforced by the compiler**, not a runtime flag. Build foes via `MonsterTemplate` /
`GenericMonster`.

## 10. Combat

The player always rolls, so `org.aventyrs.core.combat` has **two mirrored entry points**:

| | Foe attacks player | Player attacks foe |
| --- | --- | --- |
| Entry point | `AttackReceiver.resolve` | `AttackDelivery.resolve` |
| Player rolls | Esquiva e Aparar | a Perícia de Ataque |
| Foe contributes | a GD + flat bonus | a flat Defesa |
| Critical trigger | Falha Crítica | Acerto Crítico |

Both are report-only and assemble the same `Damage → Correntes → Críticos` chain.

## 11. Damage and mitigation

Three layers in fixed order: **RD** and **RA** (two independent flat reductions) → **half
damage** → **shield points**. Dano Base is a position on a scale (`1d6+0` up through `3d6+3`,
then a flat overflow), and is **never** merged with a flat damage *bonus* — the two are
separate mechanics.

`CharacterStatus` is always **derived fresh** from current damage, never stored.

## 12. Scenes, initiative and range

A `Scene` tracks participants, sub-groups (allies/enemies), rounds and turn order.
`SceneContext` is a resolved per-roll snapshot — allies, enemies and already-computed `Range`
bands — with no live `Scene` reference. Initiative can shift mid-scene via temporary bonuses;
turn *order* only re-sorts at a round boundary.

## 13. Ego points

Each Ego domain is **two spendable pools**, permanent and temporary. Both start full, and the
temporary ceiling tracks permanent points *remaining* — so spending a permanent point costs you
twice. Permanent points never recover; temporary points recover one per game session, via
`EgoPointsService#applySessionRecovery`, triggered by the consuming app.

## 14. Progression

Attribute bases (capped at 5) and Perícia Graduações (capped at 2× the governing Attribute's
base) are each raised one point at a time, spending XP, through
`CharacterAttributeService#upgradeBase` and `SkillGraduationService#upgradeGraduation`.
Talentos and Título abilities go through `FeatService` / `TitleAbilityService`.

Caps are enforced on those service entry points only — builders and fixtures bypass them by
design, and tests rely on that.

## 15. Extending it

`CLAUDE.md` is the full conventions guide: architecture rationale, the catalog of deliberately
missing systems, and the restraint rules (notably: *build for the second real consumer, not the
first hypothetical one*).

Guided walkthroughs exist as Claude Code skills for the common additions — `adding-a-pericia`,
`adding-a-race`, `adding-a-feat`, `adding-a-title`, `adding-an-item`,
`adding-an-ego-advantage`, `building-a-foe`, `granting-a-blessing`.

Consumer-facing API docs live in `package-info.java` files (`skill`, `character.services`,
`combat`, `title`, `effect`, `monster`) — **keep them current in the same change**, never in a
separate markdown file that can drift.
