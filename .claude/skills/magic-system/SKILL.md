---
name: magic-system
description: This skill should be used for any work touching Magias, Árvores de Magia, or spellcasting in org.aventyrs.core.magic / org.aventyrs.core.magic.catalog — adding or revising a Magia in a `<Tree>Spell` enum, wiring a new Árvore into `MagicTree`, the three acquisition gates (`Spell#isEligible` — cap/climb/branch), `SpellService#grantSpell`/`getMaxBranchLevel`, `MagiaAlternativaAbility`, `SpellTree`/`SpellBranch`/`BranchLevel`/`MagicBranch`/`BranchRole`, a Magia's reach (`SpellReach`/`SpellTargeting`/`AreaOfEffect`), `SpellDuration`/`ActivationTime`/`ElementalType`, `SpellData`/`AuthoredSpell`, `SpellCatalog`, or the two-roll casting flow (`SpellCastingService#castSpell`, delivery roll + `DominioDoManaInteraction` roll). Also use it when asked about which GD a Magia is cast against, why the Magia catalog is inert, or what "Força Umbral" blocks. Reference the `adding-a-feat` skill for the `MetamagicoFeat` branch-cap ladder and `magias-index.md` for the source-of-truth survey.
---

# The magic system

`org.aventyrs.core.magic` holds the spellcasting engine; `org.aventyrs.core.magic.catalog`
holds the authored ruleset (145 Magias across 20 Árvores). Everything below was previously the
"Árvores de Magia", "The Magia catalog", "A Magia's reach", and "Casting a Magia is two
separate rolls" sections of CLAUDE.md.

All of CLAUDE.md's "Recurring conventions" still apply here — in particular **"can't apply it
yet doesn't mean can't compute it yet"** (the catalog is almost entirely inert on purpose) and
the **TODO discipline** (name the specific blocking gap from CLAUDE.md's gap catalog, one line
per genuinely separate missing piece).

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
- **`SpellTree`/`SpellBranch` are interfaces**, like `AventyrTitle`/`Feat`, so a consumer can
  author their own. **All 20 Árvores of the ruleset are authored** in
  `org.aventyrs.core.magic.catalog` — see "The Magia catalog" below.
  `TestSpellTree`/`TestSpellBranch`/`TestSpell` still exist and should stay: the acquisition-gate
  tests need Magias placed at arbitrary spots in a tree of a known shape, and pinning the engine's
  own tests to real catalog entries would make a rules revision break them.

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
  resolve-then-pass-in shape `DamageServiceImpl` uses for `hasLowerPvAdjacentAlly` (see the
  `damage-and-combat` skill). That keeps the gates a pure function and the feat scan in the
  service layer.
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
  FLORESCENTE, each granting **exactly one rung** (see the `adding-a-feat` skill). Summing one
  rung apiece is only correct *because they chain* — each names the previous as its
  `requiredFeat`, so they can't be acquired out of order or in isolation, and holding all four
  lands exactly on FLORESCENTE. A fifth rung would grant +1 like the rest; never compensate for
  a missing rung by granting +2 somewhere.
- `SpellService#grantSpell` **spends `getAcquisitionCost(character, spell)` XP**, then mutates —
  the same validate→spend→grant order as `FeatService#grantFeat`. The base price is
  `SpellService.ACQUISITION_EXPERIENCE_COST`, keyed on `BranchLevel`: `0 / 1 / 2 / 3 / 5` for
  SEMENTE / BROTO / MUDA / EMERGENTE / FLORESCENTE. Only the four deeper figures are authored (the
  `MetamagicoFeat` ladder's "ao custo de N exp" clauses, the ruleset's only stated spell cost);
  SEMENTE-is-free is an inference. It's a `BigDecimal` (like `SkillGraduationService`'s cost), so
  a fractional discount is representable. `getAcquisitionCost(character, spell)` is where every
  adjustment lands: **`Feat#grantsFreeSpellAcquisition`** waives it outright (a Magia a held
  Talento names — no catalog consumer), **a free ladder pick** waives it too (see "Árvores
  conhecidas and the free picks" below), and otherwise **`Feat#resolveSpellAcquisitionCostReduction` +
  `Race#resolveSpellAcquisitionCostReduction`** are summed off the base and floored at zero —
  `Agastias`'s "Magia é Ciência" (−0.5 EXP, unscoped) is the live consumer; `ElementalFeat
  #ARCANISMO_ELEMENTAL` and the `Furia`/`NascidoDaFloresta` "Natural" clauses are the pending
  ones. A UI must call `getAcquisitionCost` (not the map) so all of that shows.
- `SpellService#getKnownTrees(Character)` — the distinct `SpellTree`s over `character.getSpells()`,
  i.e. the Árvores the Conjurador *conhece* (holding any one Magia of a tree is knowing it, since
  the climb gate makes the first one a SEMENTE). Derived, never stored — same discipline as the
  branch resolution. What `MetamagicoFeat#ARCANISTA`'s "conhece N Árvores de Magia" reads.
- **Árvores conhecidas and the free picks — ARCANISTA, wired (0.0.58).** Two `Feat` hooks, and
  **no choice class and no stored picks**, for the same reason there is no "chosen branch" field:
  - `Feat#resolveKnownTreeCapacity` → `SpellService#getKnownTreeCapacity`/`getOpenTreeSlots`.
    `ARCANISTA` answers the **Conhecimentos roll value** (Graduação + Gnose via
    `CharacterSkillService#getValueForRoll`; table ruling 2026-09-25), resolved live, so a raised
    Graduação opens a slot. `grantSpell` refuses a Magia of an unknown Árvore with no slot open
    (`SPELL_TREE_CAPACITY_REACHED`) — **nothing but ARCANISTA opens Árvores**, so a character
    without it learns no Magia. Mimetized Magias (`APTIDAO_MAGICA_AMPLA`) are a separate list and
    ungated. `learnTree(character, sheet, tree)` opens one and grants its Semente.
  - `Feat#resolveFreeSpellPicks` → `List<FreeSpellPick(rung, picks)>` — 2 at the rung each ladder
    Talento unlocks. `getOwedFreeSpells` is picks minus **distinct Árvores already holding that
    rung**, so the first two such Magias from different Árvores are free
    (`getAcquisitionCost` waives exactly those), and `getFreeSpellOptions(character, rung)` lists
    what may fill one. In a tree diverging at that rung both ramificações are offered, so **the
    pick is the branch choice**; deeper rungs follow the branch by the ordinary gates ("sempre
    do mesmo ramo da magia de nível anterior").
  - A UI prompts on both whenever either is non-zero — after a Talento grant, and after a raised
    Graduação is saved.
- `MagiaAlternativaAbility` (`org.aventyrs.core.ability`) is one `AttributeAbility` constant per
  `MagicType` — pattern 3 in the `ability-acquisition-and-substitution` skill, mirroring
  `PeritoTeoricoAbility` exactly. Grant the constant, not `FocusAbility.MAGIA_ALTERNATIVA`,
  which stays the catalog/rules-text entry.
- ⚠️ **`MagicType` and the rules text mostly agree now.** `TEMPORAL` and `UMBRAL` were added
  when the catalog landed (Tempo and Transporte are two fully-specified Temporal trees), so every
  type `MAGIA_ALTERNATIVA` names has a constant. The one remaining disagreement runs the other
  way: `NATURAL` is a `MagicType` the ability text omits, and three trees are typed with it, so a
  constant exists for it too rather than leaving them unexemptable. **Adding a `MagicType`
  constant still means adding a matching `MagiaAlternativaAbility` one.**
- ⚠️ **`NATURAL` is both a `MagicType` and an `ElementalType`, deliberately.** The source document
  lists it as an Elemental subdivision (L15) yet tags three trees with it standalone, and does not
  reconcile the two. Both constants exist so either reading has somewhere to go; that's a refusal
  to pick a side, not a resolution.
- **The exemption matches *either* of a tree's two Tipos de Magia**, via `SpellTree#hasMagicType`.
  The catalog's two-part tag is not a precedence statement — `ALIADOS DA NATUREZA
  (Natural/Invocação)` is as much an Invocação tree as a Natural one.

## The Magia catalog — `org.aventyrs.core.magic.catalog`

All **145 Magias across 20 Árvores** of the ruleset's complete section are authored, transcribed
from `docs/rules/magias.txt`. Read `docs/rules/magias-index.md` alongside them: it is the
source-of-truth survey, and it records every count, every source-document defect, and every
judgement call made while transcribing.

- **One `MagicTree` constant per Árvore, one `<Tree>Spell` enum per Árvore** (`PiromanciaSpell`,
  `VidaSpell`, …), mirroring `Feat`'s one-enum-per-tree shape — and enums specifically because a
  consumer persists a Conjurador's known Magias, and `name()` is the stable key that survives a
  round trip.
- **`MagicBranch` holds all 36 ramificações in one enum**, two per diverging tree. That is not the
  shared `PRIMEIRA`/`SEGUNDA` enum `SpellBranch`'s javadoc rejects: every constant belongs to
  exactly one tree, so no two trees share a branch object and `isEligible`'s identity comparison
  stays correct.
- **`SpellCatalog` is the entry point** — `all()`, `in(tree)`, `at(rung)`, `ofType(magicType)`.
  Unlike `FeatCatalog` it uses **no reflection at all**: the trees are an enum and each knows its
  own Magias (`SpellTree#getSpells()`), so it is a flat-map over `MagicTree.values()`. That is why
  `Spell` is deliberately **not** sealed — a consumer's homebrew Magia is a first-class `Spell`
  that simply never appears in the catalog.
- **`SpellCatalogTest` stands in for the missing `permits` clause.** Wiring a tree's enum into its
  `MagicTree` constant is a separate step from writing the enum, and forgetting it fails
  *silently* — `getSpells()` just answers empty and the whole Árvore stops being offered. The test
  pins per-tree counts, branch invariants, and every blank descriptor against the source document.

### Authoring one — `SpellData` + `AuthoredSpell`

A Magia is a `SpellData` (a `@Builder` whose every field is a descriptor line of the rules text)
plus two methods. `AuthoredSpell` is the whole of the per-tree delegation written once — twenty
trees times fifteen columns is three hundred methods it replaces — so a tree enum is constants, a
`getData()` and a `getTree()`.

**Prose is transcribed verbatim, including the document's own typos and inconsistencies**, with a
note on the constant saying so. An invocation's stat block is the one exception: it is summarised
with a TODO, because it belongs in a `MonsterTemplate` and `Spell` has no column pointing at one.

**Every "can't apply it yet" gets its gap named on the constant**, per the usual TODO discipline.
The catalog is complete, exact and mostly *inert* — the rungs, PM costs, acquisition gates,
category tags and (for a handful of Magias) primary damage are live, but most *effects* are still
blocked. The index's authoring-status section carries the blocker-to-Magia table; check it before
assuming a new mechanism unblocks a tree.

### A Magia's primary damage — `SpellDamage` / `resolvePrimaryDamage`

`SpellData#primaryDamage` (nullable; `Spell#getPrimaryDamage()` → `Optional`) is the structured
form of an `Efeito:` line like *"Causa 2d6+Metade do Foco pontos de Dano Mágico Elemental: Magma"*
— a `SpellDamage` record of `(diceCount, flatBonus, FocusScaling, DamageType, ElementalType)`.

- **Split along the "this core never rolls dice" line.** `diceCount` d6 are the *caller's* to roll
  and add (same boundary as `DamageBase`); `flatBonus` + the Foco term are deterministic.
  `SpellCastingService#resolvePrimaryDamage(spell, caster)` computes the deterministic part into a
  `ResolvedSpellDamage` (`deterministicAmount`, `diceCount`, type, element, `focusFullyApplied`),
  and `castSpell` puts it on `SpellCastingResult#getPrimaryDamage()`. **Report-only** — nothing
  applies it; the caller rolls the dice, adds, and runs its own `DamageInteraction`.
- **`FocusScaling.HALF` is the only authored value.** `FocusAbility#MAGIA_PODEROSA` upgrades it to
  `FULL` for the caster's first Magia of a Rodada (no `Spell` in `caster.getActionsThisRound()`)
  via `AttributeAbility#upgradesFirstSpellOfRoundFocusScaling()`. `NONE` exists for a dice-only
  figure but no authored Magia needs it yet.
- **"Dano Mágico Elemental: X" and bare "Dano Elemental: X" both map to `DamageType.ELEMENTAL` +
  the element** — there is no `MAGICO_ELEMENTAL` constant, and `ELEMENTAL` already reads as the
  magical case (`FISICO_ELEMENTAL` being the physical one). Pairing validated like `DamageBonus`.
- **Authored so far:** the four straightforward `IraDeVulcanoSpell` damage rungs (Sopro de Magma
  Menor/Maior, Torrente Vulcânica, Ira de Vulcano's primary wave) and three `PiromanciaSpell`
  entries (Hálito de Eldur, Bola de Fogo Elduriana, Olhar de Eldur). What `SpellDamage` does
  **not** hold, and stays prose on the constant: distance falloff ("-2 por UD"), a recurring "a
  cada Rodada" cadence, a delayed second wave, and damage to someone other than the cast target.
  Extend it Magia-by-Magia as a real consumer needs each. **Healing is no longer on that list** —
  it has a column of its own, `SpellHealing`; see the next section.

### A Magia's second version — `Efeito Alternativo`

**An Efeito Alternativo is a second version of the same Magia, never a second Magia.** The ruleset
is explicit — *"um personagem que aprenda a versão base automaticamente aprende sua segunda
versão"* (`magias.txt:29`) — and the same preamble makes it a branch-long concern: one ramificação
*"foca na evolução dos Efeitos Alternativos"* (L30), which is what `BranchRole.ALTERNATIVO` names.

- **Never author one as its own catalog constant.** It would charge XP in `SpellService#grantSpell`,
  count as a climb-gate foothold, and change `SpellCatalog.all().size()`. `grantSpell` refuses one
  outright (`SPELL_ALTERNATE_VERSION_NOT_GRANTABLE`, tested against `Spell#isAlternateVersion()`).
- **`SpellAlternateEffect` is a delta; `AlternateSpellVersion` applies it.** Every field null means
  "inherited". The decorator `implements Spell`, which is the whole payoff — `castSpell`,
  `resolveEffect`, `SpellHealingEffect`, `SpellDurationService` all work on a second version with
  no change, because none of them can tell it is one.
- **64 of 145 have one, and 40 override at least one parent column** — Alcance, Duração, GD,
  Tempo de Ativação, Perícia Chave, Efeito Crítico, plus PM cost. Eight override three or more.
  A prose string cannot represent that; this is why the column exists.
- **Overrides run both directions.** Two alternates convert a Reação *back* to Pontos de Ação, so
  `activationTime` is fully replaceable, not a one-way widening.
- **`suppressesCriticalEffect` is separate from `criticalEffectType`** — a null type already means
  "inherited" and cannot also mean "this version receives none" (Transporte's *Portal de Fuga*).
  `castingDifficultyFlooredByTargetMagicDefense` is a boxed `Boolean` for the same tri-state reason.
- **`getAlternateTargeting()` empties once the delta overrides `getTargeting()`.** ⚠️ That column is
  the parent's dual `Alcance: Pessoal ou Toque` line and has **nothing** to do with an Efeito
  Alternativo despite the name — don't conflate them.
- **The prose is not duplicated.** `SpellAlternateEffect` carries the *name* only; the alternate's
  own `Efeito:` is the parent's `secondaryEffectDescription`, read through the decorator.
- **Author an override only where it maps to a real type.** Reaches stated in metres or dice
  formulas ("até 4m", "cone de até 6 metros", "2d6*100 UD") have no `Range` band; a Duração stated
  *relative* to the parent ("reduzida à metade") has no absolute figure. Both stay prose + TODO.
- **Two source defects to know**: the document heads one alternate `Efeito Alternativa` (Hálito de
  Eldur's), which a naive grep misses — the real count is 64, not 63; and Ocultação's *Ataque
  Sombrio* names an Efeito Crítico ("Oferenda Sombria") that the Efeitos Críticos section does not
  define, left unauthored rather than substituted.
- `SpellCatalogTest` pins that every transcribed Efeito Alternativo is wired and no other is.

### A Magia's effect — `SpellEffect` and its four categories

A Magia's `Efeito:` line becomes executable through **`org.aventyrs.core.effect.SpellEffect`**, a
third category under the pre-existing `Effect` root beside `CriticalEffect` and `EffectChain` —
**not a hierarchy of its own**. `Effect extends Interaction<CombatantSheet>`, so a concrete Spell
Effect plugs into `receiveInteraction` with zero other code touched, and `AbstractEffect` supplies
`chainInto`/`reportChain` free. Its four subcategories divide by what the effect *does*:
`OffensiveEffect` (damage, debuffs), `DefensiveEffect` (buffs, cleansing), `HealingEffect`,
`InvocationEffect`.

- **One shape per ramificação, parameterized by rung — never a class per Magia.** The Magias of a
  branch share an effect that deepens as the tree climbs, so a concrete Spell Effect is built from
  authored columns rather than subclassed per constant. `SpellHealingEffect` covers the whole of
  Vida's principal branch (Descanso Mínimo at Semente → full recovery at Florescente);
  `ConditionCleansingEffect` covers its alternativo one. **Adding a rung is authoring data, not
  writing a class** — reach for a new class only when a branch's *shape* differs, not its depth.
- **Two authored columns feed them**, both on `SpellData` with `Spell`/`AuthoredSpell` delegates,
  wired exactly like `primaryDamage`: **`healing`** (a `SpellHealing`) and **`cleansedConditions`**
  (a `Set<ConditionType>`, `@Builder.Default` empty because callers iterate it).
- **`SpellHealing` states a Descanso tier, not dice.** Every healing Magia reads "recupera PV como
  se passasse por um Descanso Mínimo/Longo/Total", and `RestType` carries those four tiers. Either
  a `restEquivalent` tier **or** `fullRecovery` — both or neither throws `INVALID_SPELL_HEALING`,
  the same cross-field validation `SpellDamage` and `SpellTargeting` apply. Plus
  `halvedForHostiles` (Nova Rejuvenescedora) and a `HealingCondition` gate
  (`ONLY_IF_NOT_BLEEDING`, Aliviar a Dor's "ao invés disso").
- ⚠️ **The amount reads the *target's* Vigor, not the caster's** — "faça com que *um alvo* recupere
  PV como se *passasse* por um Descanso" names the recipient, the same reading
  `SpellDuration.targetAttribute` takes. This is the one place a Magia's magnitude is not the
  Conjurador's own; a Foco term (`Sobrecura`) still is.
- **It heals as much as a Descanso, and is not one.** The effect calls `RestService
  #getRecoveredHitPoints` then a bare `CombatantSheet#heal` — **never `RestService#applyRest`**,
  because "Este é um efeito similar a Descanso e não substitui Descansos reais", and a real Rest
  also restores PM/PD, settles every `PendingEgoRecovery` and clears rest cooldowns. Going through
  `heal` also gets Feridas Dolorosas' refusal and Sangramento's interruption for free.
- **`SpellEffectFactory` builds them, and a new category touches nothing else.**
  `SpellEffectKind` carries a `Supplier<SpellEffectBuilder>` per category — the same
  one-constant-carries-its-own-lookup idiom `SkillType` uses for `interactionFactory` — and the
  factory is the thin facade over it that `SkillInteractionFactory` is over `newInteraction()`.
  **Adding `OFFENSIVE`/`INVOCATION` is a builder class plus a supplier; `SpellCastingServiceImpl`
  needs no edit.** Each builder reads **one authored column** and answers empty otherwise, which is
  what keeps this keyed on data rather than on a registry of effect types.
- ⚠️ **There is no "which effect" selector, and there must not be one.** A Magia and its Efeito
  Alternativo are two separate `Spell`s carrying separate effect columns, so choosing the version
  *is* choosing the effect — `useAlternateVersion` answers it, and each version authors at most
  one effect. `SpellEffectFactoryTest#noVersionAuthorsMoreThanOneEffect` pins that across every
  Magia **and** every second version, so an authoring mistake stops the build rather than a cast.
  Don't reintroduce a kind parameter or a runtime ambiguity throw; the invariant is the design.
- **The effect applies; the caller drives it.** `SpellCastingService#resolveEffect(spell,
  context)` delegates to the factory and `SpellCastingResult#getSpellEffect()` reports it —
  `castSpell` never runs it, since this core resolves no target GD and so cannot tell the cast
  landed. Same report-only restraint as `getPrimaryDamage()`/`getRecordedAction()`. A Corrente the
  caller judged triggered is chained on first (`Sobrecura`), which is also why the factory returns
  a single effect rather than a pre-composed chain — `chainInto` *replaces* a successor.
- **A `SpellCastRequest` chooses one thing the Magia does not fix**: `useAlternateVersion` — cast
  the Efeito Alternativo, with the service resolving `getAlternateVersion()` itself. ⚠️ **The
  version is resolved *before* validation**, so a second version's own Alcance is what the target
  shape is judged against — Procrastinar Ferimento is Distância Curta where its parent is Pessoal,
  and validating the parent's reach would refuse a legal cast. It is also what the delivery roll,
  Duração, damage, effect and `recordedAction` all read.
- **Which targets are hostile, and who is in an area, stay the caller's.** No footprint resolution
  exists, so `castSpell` builds the effect for the primary target alone (deriving hostility from
  the caster's own `SceneContext#getEnemies` into a `SpellEffectContext`); a caller sweeping an
  area calls `SpellEffectFactory.create` per combatant.
- **`ConditionType.POSSESSAO` was added for `EXORCIZAR`** — not in
  `docs/rules/condicoes-e-maleficios-.txt`, authored from the Fantasma's *Possessão Furiosa*
  Corrente in `magias.txt`, modelled on `SILENCIO`. And **`ConditionType#isMaleficio()`/
  `maleficios()`** were added for `CORPO_FECHADO`'s "todos os Malefícios" — the first clause to ask
  about the category instead of naming its members. Everything but `ESCONDIDO`.
- **Still inert:** `OffensiveEffect` (no wired branch needs one; both halves already exist) and
  `InvocationEffect` (`Spell` has no `MonsterTemplate` column). `CORPO_FECHADO`'s Malefício
  *immunity* half needs per-condition immunity, which does not exist. The Regeneração tree is
  unblocked but unwired — `SpellHealing` would need a recurring figure beside the instant one.

### Four columns the catalog forced into existence

Each closed a gap CLAUDE.md used to name. Don't re-derive them:

- **`SpellDuration`** replaces the old `int getDuration()`, and the reason is `POTENCIALIZAR` —
  57 of 145 Magias carry it, and it adds "+2d6 **unidades**" in the Magia's *own* unit. Storing
  `1 minuto` as a bare `12` lands that at 1/12th its magnitude. So it holds a count, a
  `DurationUnit` (1 min = 12 Rodadas, carried-in knowledge stated in no source document), a
  `concentration` flag and a `DurationKind`. `inRodadas()` is the canonical form.
  - **Concentração is a flag, not a kind**, and its count is the **trailing** one: the effect runs
    uncounted while focus holds, and the count starts when focus breaks (on the caster's own cast
    or attack — *not* on being attacked). 20 Magias. Two narrow things are missing: moving a
    `TemporaryEffect`'s `remainingRounds` from `null` to a count, and a caster-to-sustained-effects
    link for the 17 that land on someone else's sheet (`Scene.grantedBlessings` is the precedent).
  - **`SAME_AS_REFERENCED` holds a `Supplier`**, for the same forward-reference reason
    `MetamagicoFeat`'s `FeatRequirements` is one. It is not cosmetic: Corpo Rochoso's *Dádiva de
    Epona* raises Rigidez Térrea's Duração, and five Magias must follow it.
- **`ActivationTime`/`ActivationType`** — `Tempo de Ativação` is not a PA count: five Magias cost a
  Reação and four an Ação Livre.
- **`ElementalType`** — the catalog never writes a bare "Elemental", always `Elemental: Fogo`. It's
  a column on `SpellTree` rather than constants on `MagicType`, because the two are asked about
  independently and folding them would narrow a `MagiaAlternativaAbility.ELEMENTAL` exemption.
- **`Spell#getEffectChainDescription()`** — the `Corrente de Efeitos` field, and it is **prose, not
  an enum reference**. The 145 Magias name 69 distinct Correntes inline and *none* appears in the
  shared 13-entry Corrente catalog; the two populations are completely disjoint.

### Two columns are derivable, and one of them is redundant

- **`GD da Conjuração` is a function of the rung, not an independent column.** Semente→Fácil,
  Broto→Médio, Muda→Difícil, Emergente→Muito Difícil, Florescente→Improvável, with **zero
  deviations** across the 134 of 145 Magias that state a plain tier (the other 11 are 4 blanks, 3
  bare-`DM do Alvo`, 3 per-rung tables and 1 allegiance-conditioned entry). The `ou DM do Alvo
  (maior)` floor is orthogonal — all 46 of those still state their rung's tier.
  `Spell.castingDifficultyAgainst(BranchLevel)` *is* this ladder; it was written for the three
  dispelling Magias, which apply it to the rung of the effect they target rather than their own.
  **So a blank GD is derivable, not unknowable** — the four blank ones are still authored as
  `null`, because filling them by rule is a decision nobody has taken, not because the value is
  unknown.
- **An unstated `Tempo de Ativação` is 2PA**, which is also the catalog's modal value (58 of 145).
  Don't apply it over an authored value: the one Umbral Magia that states a time states a Reação.

Both matter mainly for the **44 unauthored Umbral Magias** — `GD da Conjuração` is the only column
blank on all 44, and it is exactly the one these rules recover. What actually keeps that section
out of `MagicTree` is that acquisition is gated on a *Força Umbral* Talento that does not exist.
`docs/rules/magias-index.md` carries the full listing.

### Branch roles are the one place this catalog contains judgement

The document names no ramificação, but it does state what the two always are (L30: "Um deles
aprofunda o efeito principal da magia, enquanto outro foca na evolução dos Efeitos Alternativos").
So `MagicBranch` names them by `BranchRole` — `PRINCIPAL`/`ALTERNATIVO` — rather than inventing 36
names. **Which entry holds which role is a reading**, and every constant records its own trace.

- **The document's printed order carries no meaning.** In 14 of 18 diverging trees the first-listed
  entry deepens the principal effect; in Morte, Piromancia, Reanimar and Voo it is the second. That
  split is the evidence.
- **Six trees carry no `Efeito Alternativo` before their divergence**, so L30's second half has
  nothing to point at and the trace runs on the principal effect alone.
- **Ocultação breaks the rule outright** and says so on the constant; **Polimorfismo is close to a
  coin flip** and says so too. Don't "fix" either without new rules text.
- **Piromancia's two are the only branches with names of their own** (Eldur / Boros), carried by
  every Magia on each path, so those two override their role label via `MagicBranch`'s optional
  `authoredName`.

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

What this does *not* do is in CLAUDE.md's gap catalog, "Area de Efeito" row: no footprint
resolution, no classification of an incoming attack as area-based, and no caster-exclusion flag.

## Casting a Magia is two separate rolls — `SpellCastingService`

Casting a Magia with a rolled effect always involves **two** rolls, not one: whichever
Perícia actually delivers the spell (e.g. `AtaqueADistanciaInteraction` for a ranged spell,
`AtaqueCorpoACorpoInteraction` for a Toque spell) rolled against the **target's** GD, followed
by a `DominioDoManaInteraction` roll against the **Magia's own** GD. `SpellCastingService
.castSpell(CharacterSheet, Interaction<CharacterSheet> deliveryInteraction)` orchestrates
this: it rolls the given delivery Interaction, then rolls Domínio do Mana, and returns both
`InteractionResult`s in a `SpellCastingResult` — it never picks the delivery Interaction
itself (the caller does, since only the caller knows which Magia/weapon is being used).

`Spell` is a real entity, the full 145-Magia catalog is authored, and a character carries the
Magias they know (`Character#spells`, via `SpellService#grantSpell`). What's still missing is
narrower than it was: `SpellCastingService` doesn't know either roll's target GD, so it
computes both rolls' bonuses without resolving success/failure for either. It *does* now report
`SpellCastingResult#getPrimaryDamage()` for a Magia that authors a `SpellDamage` (see "A Magia's
primary damage" above) — deterministic part resolved, dice left to the caller, nothing applied.

It also reports **two Talento-adjusted cast figures**, both report-only:
`getCastingDifficultyLevel()` — the Magia's authored GD da Conjuração eased by the summed
`Feat#resolveCastingDifficultyReduction(Spell, Character)` (with `getCastingDifficultyReduction()`
beside it, since a Magia with no fixed tier still has a reduction to report) — and
`getActivationTime()`, the Tempo de Ativação less the summed
`Feat#resolveCastingActionPointReduction(Spell, Character, currentRound, actionsThisRound)`,
applied only to a PA activation and floored at 1PA. Both hooks are Magia-scoped rather than
Perícia-scoped, which is why neither goes through `Feat#resolveDifficultyReduction`.
`DestinoFeat#ARCANISMO_DRUIDICO` is the consumer. `currentRound` is the Scene's **0-based**
counter, so a clause about "Rodadas Ímpares" (1st, 3rd…) tests `currentRound % 2 == 0`.

`castSpell(SpellCastRequest)` also bundles the cast as a ready `CombatantAction` on
`SpellCastingResult#getRecordedAction()` — the delivering Perícia, the `Spell` as `attackSource`,
and `turnNumber` from `request.getScene().getCurrentRound()`. Partial: no roll is handed to
`castSpell`, so `governingAttributeDomain` and the `ActionOutcome` verdict stay unset (unlike
`DeliveredAttackResult`'s, where the roll is supplied). Not recorded — the caller files it with
`scene.recordAction(caster, action)`, and once filed the `Spell` `attackSource` is what
`isFirstSpellCastOfRound` / `upgradesFirstSpellOfRoundFocusScaling` read back. The legacy
`castSpell(CombatantSheet, Interaction)` overload leaves it `null` (no Scene).

**Cite the missing *GD*, not a missing catalog or a missing `Magia` entity** — both of those are
closed. And be precise about which GD: the Domínio do Mana roll's own is real authored data
(`Spell#getCastingDifficultyLevel()`), so what's actually absent is the *delivery* roll's, which
is the target's Defesa Mágica — an authored flat number on a `MonsterSheet` that nothing compares
a roll against. Two authored GD shapes are blocked on that same comparison:
`isCastingDifficultyFlooredByTargetMagicDefense()` (49 Magias) and `getCastingDifficultyAgainst`
(3). Left as a TODO on the service rather than guessed at.

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

## Reference files to read first

- `src/main/java/org/aventyrs/core/magic/Spell.java` — the interface every Magia implements;
  `isEligible`, `getTargeting`, `castingDifficultyAgainst`, all fifteen columns.
- `src/main/java/org/aventyrs/core/magic/SpellTree.java` / `SpellBranch.java` / `BranchLevel.java`
  — the tree structure and the three gates' ladder operations.
- `src/main/java/org/aventyrs/core/character/services/SpellService.java` /
  `SpellServiceImpl.java` (`SpellServiceImplTest.java`, `SpellEligibilityTest.java`) —
  `grantSpell`, `getMaxBranchLevel`, the feat scan.
- `src/main/java/org/aventyrs/core/magic/catalog/MagicTree.java` / `SpellCatalog.java` /
  `MagicBranch.java` / `BranchRole.java` (`SpellCatalogTest.java`) — the catalog wiring.
- `src/main/java/org/aventyrs/core/magic/catalog/PiromanciaSpell.java` — a diverging tree with
  named branches; `VidaSpell.java` — a straightforward one. Read one before authoring.
- `src/main/java/org/aventyrs/core/magic/SpellData.java` / `AuthoredSpell.java` — the authoring
  shape.
- `src/main/java/org/aventyrs/core/magic/SpellTargeting.java` / `SpellReach.java`
  (`SpellTargetingTest.java`) and `src/main/java/org/aventyrs/core/scene/AreaOfEffect.java`.
- `src/main/java/org/aventyrs/core/magic/SpellCastingService.java` /
  `SpellCastingServiceImpl.java` (`SpellCastingServiceImplTest.java`).
- `src/main/java/org/aventyrs/core/magic/SpellDuration.java` / `DurationUnit.java` /
  `DurationKind.java` / `ActivationTime.java` / `ElementalType.java`.
- `src/main/java/org/aventyrs/core/magic/SpellDamage.java` / `FocusScaling.java` /
  `ResolvedSpellDamage.java` (`SpellDamageTest.java`) — the primary-damage model;
  `AttributeAbility#upgradesFirstSpellOfRoundFocusScaling` / `FocusAbility#MAGIA_PODEROSA`.
- `src/main/java/org/aventyrs/core/magic/SpellHealing.java` / `HealingCondition.java`
  (`SpellHealingTest.java`) — the healing column; `org.aventyrs.core.effect/SpellEffect.java` +
  `HealingEffect`/`DefensiveEffect`/`OffensiveEffect`/`InvocationEffect`, and the concrete
  `SpellHealingEffect.java` / `ConditionCleansingEffect.java` / `Sobrecura.java` with their tests.
  Read `org.aventyrs.core.effect/package-info.java` first — it is the pipeline's narrative.
- `org.aventyrs.core.effect/SpellEffectFactory.java` / `SpellEffectKind.java` /
  `SpellEffectBuilder.java` / `SpellEffectContext.java` and the two `*EffectBuilder`s
  (`SpellEffectFactoryTest`, `SpellEffectKindTest`, `SpellEffectBuilderTest`) — how a Magia's
  columns become an effect, and the one place to touch when adding a category.
- `docs/rules/magias-index.md` — the source-of-truth survey; `docs/rules/magias.txt` — the raw
  rules text.
