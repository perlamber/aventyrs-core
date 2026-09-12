# Talentos TODO — phased implementation plan

~381 `TODO:` lines live across the 32 `*Feat` files in `org.aventyrs.core.feat`. Almost none
is a *feat-specific* problem: each is blocked on one of a small set of **missing engine
mechanics** catalogued in `CLAUDE.md` ("Missing systems — the gap catalog"). This plan groups
the work by that mechanic, orders the groups by dependency and return-on-effort, and says which
constants each group lands.

## How to read this

- A **phase** builds one or a few related mechanics, then wires *every* constant that was only
  waiting on it, with tests, and updates the mechanic's subsystem skill + the `CLAUDE.md` gap
  catalog row + any `package-info.java`.
- Constant counts are approximate — the exact set is re-derived at phase start by re-reading
  each cited constant's own TODO (per `CLAUDE.md`: *"When a mechanism gets built, don't assume
  it finished every trait citing it; check each constant's own TODO"*).
- "Effort" is rough sessions of focused work, design doc included.
- Phases 0–4 are **harvest** — mechanics already built or nearly so; highest ROI, do first.
  Phases 5–7 are **core state systems**. Phases 8–11 are **large combat/magic systems**.
  Phase 12 is **narrow deep systems**. The "Won't schedule" section stays as sharpened
  comments.

---

## Phase 0 — Stale-TODO reconciliation sweep

**No new mechanics.** Several hooks named as "missing" in TODO prose already exist; the memory
note *"Stale TODOs"* and `CLAUDE.md`'s TODO-discipline bullet both call this out. Walk every
feat file once, and for each TODO either turn it into working code+test or rewrite it to cite
the *real* current blocker.

Known-stale patterns to reconcile (verify each against the interface before acting):

| Prose in TODO | Reality | Action |
| --- | --- | --- |
| "a Talento cannot grant an Atributo bonus" | `Feat#resolveAttributeBonus(AttributeDomain, Character)` exists, summed by `Character#getEffectiveAttributeTotal`. `TrollFeat#VIGOR_TROLLICO`'s own TODO already says so. | Wire the unconditional `+N` half for `FeralFeat` (×3), `MonstruosoFeat` (×2–3), `FeericoFeat` (×3), `BestialFeat` Heranças, `TrollFeat#VIGOR_TROLLICO`/`BENCAO_DE_MAPINGUARI`, `DestinoFeat`. Leave a TODO only for the still-blocked other half. |
| "Feat has no dano-bonus hook" | `Feat#resolveDamageBonus` (4-arg: `SceneContext`, target, actor, count) exists. | Wire the computable flat bonus for `DuelistaFeat#FORCA_EXCESSIVA` (half Vigor), `DuelistaFeat` crit-dano halves, `OrquicoFeat` (half Vigor to physical dano). Self-damage / typed-damage riders stay TODO. |
| "the EXP discount has no hook" | `Feat#resolveSpellAcquisitionCostReduction` + `SpellService#grantSpell` spend it. | Wire `ElementalFeat` and any sibling citing it. |
| "Feat#resolveCriticalMarginIncrease is real, but…" | The `holder`-taking overload exists (`AssassinoFeat#ACERTO_CRITICO_RELAMPAGO`). | Wire the unconditional margin bump for `DuelistaFeat`/`EscudeiroFeat`/`SobrevivenciaFeat` constants whose *only* block was the hook. |
| "needs a per-Rodada / per-Turn / per-Cena activation counter" | The roll-action log is built (`getActionsThisRound/Turn/Cena`, `isFirstAttackRollOfTurn`). | Reconcile `CavalariaFeat`, `ArtilhariaFeat`, `DuelistaFeat#GOLPE_OPORTUNO`, `PequeninoFeat#SILENCIO_PRE_SURPRESA` — some are now expressible; the rest need a *count of activations of one specific ability*, which is still missing. |
| "resolveDifficultyReduction is real, but summed by…" | Confirm the summation concern per constant; several are wireable. | `PeritoFeat`, `DestinoFeat`, `EscudeiroFeat`. |

**Deliverable:** a diff that closes an estimated 30–50 TODOs outright and sharpens the rest.
**Effort:** 1–2 sessions. **Do first — nothing depends on it and it shrinks every later phase.**

---

## Phase 1 — Small additive stats & caps ✅ **DONE**

Self-contained numeric mechanics with no cross-system reach.

### 1a. Permanent Resistência a Críticos scan ✅
Built as `CombatantSheet#getTotalCriticalResistance(SceneContext)` — the sheet totals its Raça's
`Race#getCriticalResistance()`, each held Talento's `Feat#resolveCriticalResistance`, and the
round-scoped `ModifierType.CRITICAL_RESISTANCE` `TemporaryBonus`; `AbstractSkillInteraction` now
subtracts that total instead of reading `getTemporaryBonus` directly. One instance is
`CombatantSheet.CRITICAL_RESISTANCE_INSTANCE`, promoted out of `AnaoFeat`'s private constant.
- **Landed:** `MonstruosoFeat#ANATOMIA_INCOMUM`, `ElementalFeat#TRANSFORMACAO_ELEMENTAL`,
  `DuelistaFeat#CORACAO_DE_FERRO`, `SobrevivenciaFeat#PROTETOR_TERRITORIALISTA` (terrain-gated),
  plus the racial `Troll`.
- **Not landed, second blocker found:** `GorgonaFeat#PROTECAO_DO_DEUS_DOS_MONSTROS` /
  `#PROTECAO_DA_RAINHA_DAS_FADAS` are form-gated (Phase 5). `TrollFeat`/`GiganteFeat` turned out
  to have no RC clause at all — the plan's list was approximate.
- **Deliberately not built:** a `SkillCompetencyAbility`/`AttributeAbility` hook. No constant
  wants one — `ProfissaoCompetencyAbility#FORJA_VULCANA`'s RC is *item*-scoped, a shape neither
  `Item` nor the sheet total has, and that is now what its TODO cites.
- Still deferred: the "−1 à Margem Crítica Maior" clause, non-PRIMORDIAL scoping, item-scoped RC.
- ⚠️ Design note carried in the javadoc: the `SceneContext` the scan receives is the **attacker's**
  snapshot (the only one in reach — `SceneContext` holds no `Scene`), so an override may read
  Scene-wide facts and must never read proximity.

### 1b. RM — Redução Mágica ✅
A magic-damage path did exist (`DamageType.MAGICO` already reaches `calculateFinalDamage`), so
this did not pull Phase 8 forward. `ModifierType.MAGIC_REDUCTION` +
`DamageService#getTotalMagicReduction(CombatantSheet)` (same five sources as RD), added to the
flat stage **only** when the caller typed the hit `MAGICO`; skipped by `ignoreDamageReduction`
alongside RD (an inference, documented).
- **Landed:** `GorgonaFeat#PROTECAO_DA_RAINHA_DAS_FADAS` (its RM half was unconditional all
  along — only its RC is form-gated), `DefensiveMasterpiece#DYOSPIROS` and `#MITRAL`.
- **Not landed, second blocker found:** `MetamagicoFeat#ARCANISTA` (scoped to "Magias que você
  conheça" — nothing classifies an incoming effect as a specific Magia),
  `TrollFeat#VIGOR_TROLLICO` (unmodelled sub-lineage), `GorgonaFeat#MONSTROS_EM_PELE_DE_FADA`
  (form toggle), `EscudeiroFeat#BASTIAO_DE_VIDRO` (needs mitigation to be *suppressible*).
- **Known incorrectness, deliberately left:** RD is still type-blind, so a MAGICO hit takes RD
  *and* RM. Narrowing RD is Phase 8's job; the over-mitigation is documented on
  `ModifierType#MAGIC_REDUCTION`, `DamageType` and the gap-catalog row.
- RE (Resistência Elemental) got no constant: every authored RE clause is scoped to one
  `ElementalType`, which is the `DamageDescriptor` path equipment already uses.

### 1c. Ceiling / absolute-set stage ✅ (one half; the other has no consumer)
- **Categoria de Tamanho** — built as *two* hooks, because the catalog authors both shapes:
  `Feat#resolveSizeCategoryOverride` (an absolute set) and `Feat#resolveSizeCategoryIncrease`
  (a shift). `CharacterSizeService#getEffectiveSizeCategory` applies the override first, then
  every shift — the `@Modifier` scan and the Talento one — on top of it.
  **Landed:** `GnomoFeat#DUENDE` (→ -2), `FeericoFeat#PIXIE` (→ -3), `FeericoFeat#LUPERCAL` (→ 0),
  and `GiganteFeat#GIGANTE_DO_CLA_EMPUSA`, which is now granted **whole** (size shift + Força per
  Título + the -2 Defesas) rather than withheld. Its Jotun twin stays withheld: its malus alone
  is still inexpressible.
- **PA ceiling — not built, and should not be.** The catalog's only PA-ceiling clause is
  `SobrevivenciaFeat#PERMANECER_CONSCIENTE` (the plan named a constant that does not exist), and
  it is gated on "enquanto Permanecer Consciente estiver ativo" — an active state that does not
  exist either. A ceiling stage built today would have nothing that could switch it on. Build it
  with that state; its TODO now says so.

**Depends on:** Phase 0 (so counts are clean).

---

## Phase 2 — FeatRequirements expansion (acquisition-gating correctness) ✅ **DONE**

**Built:** `anyOf` (nested disjunction groups, checked recursively — outer clauses AND at least
one branch, so common ground is written once); `maximumAttributeDomain`/`Value` and
`maximumEgoDomain`/`Value`; `requiredAnyAttributeValue` + the narrower
`requiredAnyRacialAttributeValue`; `forbiddenRace`; `forbiddenFeats`; `requiredFame` /
`requiredTotalExperience`. `requiredFeat` became a `@Singular` set (every existing `.requiredFeat(X)`
call site still compiles) and `requiredSkillCompetencyAbility` became `requiredSkillTraits`, typed
as `SkillTrait`, so an Especialização prerequisite fits the same clause.

`Feat#isEligible(Character, CharacterSheet)` is the authoritative form; `isEligible(Character)`
delegates with `null` and **skips** the two sheet-side clauses rather than failing them.
`FeatService#grantFeat` and the new `getAvailableFeats(Character, CharacterSheet)` /
`FeatCatalog#availableFor(Character, CharacterSheet)` pass the sheet, so the sheet-less listing is
a documented *superset* of the real gate — pinned by a test.

**Two things found on the way:**
- A latent bug: six constants override `isEligible(Character)` (`ArtesMarciaisFeat`, `ElficoFeat`,
  four in `AssassinoFeat`). Adding the 2-arg form would have silently bypassed every one at
  `grantFeat`; they now override the 2-arg form.
- A `Supplier<FeatRequirements>` does **not** make a forward reference to a later enum constant
  legal — Java's rule is about the reference, not when it is evaluated. Mutually-exclusive pairs
  therefore need the `Supplier` *and* a `private static` accessor for the forward half (a method
  body is not an initializer). Five enums converted.

**Not built, deliberately** — each recorded in `docs/rules/talentos-index.md` under "What a
Pré-requisito still cannot say": "personagens recém-criados" (nothing records creation time); a
*second* Perícia Graduação; a constraint on another held Talento's recorded *choice* (the three
cases want equals/differs/is-a-weapon, which is not data); a cap on how many of a family may be
held. The last three stay `isEligible` overrides. `FeralFeat`'s "não pode ser **usado** em
conjunto" is a use restriction and deliberately does not become a `forbiddenFeat`.

**Original plan text follows.**

### Phase 2 (as planned)

`FeatRequirements`/`isEligible` currently expresses only "attribute/skill/feat threshold that
must be met". Add:

- **Disjunctive groups** — "Gnose 3 *ou* Foco 3", "Lutador Nato *ou* 4 Graduações",
  "EXP total ≥ 15 *ou* 1 Título Desperto". (~15 constants model only one branch today.)
- **Maximum-value prerequisites** — "Força ≤ 2", "Iniciativa 2 ou inferior".
- **Any-attribute-at-value** — "qualquer Atributo com Valor Base 5", "qualquer Atributo que
  receba bônus Racial com Base ≥ 3".
- **Negated race** — "apenas personagens não-humanos".
- **Mutual exclusion** — "não pode possuir mais de um Talento de Clã"; two feats that exclude
  each other (`MobilidadeFeat` Investida Solar/Lunar).
- **Held-feat's-own-choice** — "a held Talento's chosen Perícia/Atributo must (not) be X".
- **CharacterSheet-side values** — `Fama 15`, `EXP total ≥ N`. Requires `FeatRequirements` to
  see sheet data at `grantFeat` (today it reads `Character` only) — decide: pass the sheet, or
  a small projection.

Split out if it needs its own concept: **"recém-criados"** (creation-time-only) — nothing
records when a Talento was acquired or that a character is freshly created. Defer to a
"character provenance" mini-phase or leave as documented gate.

- Lands (gating correctness only — effects may still be blocked): ~20+ across `DestinoFeat`,
  `PeritoFeat`, `DuelistaFeat`, `MonstruosoFeat`, `VampiricoFeat`, `MobilidadeFeat`,
  `SobrevivenciaFeat`, `GiganteFeat`, `IndomitoFeat`.
- **Effort:** 1–2 sessions (record/enum shape + tests via `FeatRequirementsGateTest`).
- **Value:** `FeatCatalog#availableFor` / `isEligible` stop returning wrong answers; independent
  of every effect phase.

---

## Phase 3 — Acquisition-slot grants ✅ **DONE**

**Built:** `Feat#getGrantedSkillTraits(Character)` — one hook for both trait kinds, because the
rules text routinely offers a choice between them in one clause. A granted `SkillCompetencyAbility`
is folded into `SkillCompetencyAbility#allFor` (now a three-source scan, deduplicated); a granted
`SkillSpecialization` into the new `Character#getSpecializations(SkillType)`. Both roll-path
readers (`AbstractSkillInteraction#validateRequestedTrait`, `Feat`'s own trait check) now go
through the aggregating views rather than the raw lists, so a granted trait can be *named* on a
roll like any acquired one.

The existing `getGrantedAttributeAbilities` needed no widening: the open choice was already
expressible, only a class to record it was missing.

**Three acquired forms**, each serving several constants rather than one:
- `HerancaBestialFeat` — the six `BestialFeat` Heranças, forwarding each constant's own Atributo
  and Arma Natural clauses by hand. This closed what `BestialFeat`'s javadoc called *the single
  most-cited blocker of the racial catalog*.
- `HabilidadeDeAtributoEscolhidaFeat` — `DestinoFeat#PRODIGIO`/`GENIALIDADE`/`GENIALIDADE_DESPERTA`.
  No field for the chosen Atributo: an `AttributeAbility` reports its own.
- `ChosenSkillTraitsFeat` — any constant across any tree whose *whole* payload is chosen traits
  (`PeritoFeat#TREINADO_EM_PERICIAS`, `GnomoFeat#SABICHAO`/`MIMETIZAR_COMPETENCIA`). No delegation,
  which is its entry condition.

`AdotadoPorSylphFeat` gained the grant half it was missing, with a second factory: the Perícia pick
and the Habilidade pick are separate acts, so `of(SkillType...)` records the first alone and
`of(SkillCompetencyAbility...)` records the finished state, deriving the Perícias from it.

**Deliberately not built:** a blanket forward from `AbstractFeat` to `catalogEntry()` — a
choice-carrying form replaces its constant rather than decorating it, and a blanket forward would
make it impossible to drop a clause on purpose. **Still missing:** a free **Talento** slot
("escolha um Talento Racial"), and a **`Race`** hook for any of these — `Anao`'s Pequenos Gigantes
and `Elfo`'s Origem Mística are the same shape but `Feat` is the only granting path.
`HumanoFeat#APRENDIZADO_RAPIDO_E_CONTINUO` stays blocked on a different gap: which Perícias
Aprendizado Rápido benefits is a creation-time choice nothing records.

**Original plan text follows.**

### Phase 3 (as planned)

Generalise the narrowly-built `Feat#getGrantedAttributeAbilities` (currently: one *named*
ability, `ConselheiroDeGuerraYmirianoFeat`) to the shapes the gap catalog lists as missing:

- An **open-choice slot** — "uma Habilidade de Competência de Atletismo à sua escolha", "uma
  Especialização de Atenção". Model with the existing `AcquiredChoice<C>` / `AbilityChoiceService`.
- A **free Especialização / Habilidade de Competência slot** not tied to one Perícia.
- A **`Race` hook** counterpart (today only `Feat` can grant) — for the racial constants, out of
  plan scope but the hook is shared.

- Lands: `DestinoFeat#PRODIGIO`/`APADRINHADO`/`FILHO_DA_FORTUNA`, `PeritoFeat#POLIMATA`,
  `GnomoFeat` (×2), `BestialFeat` free-Competência halves (×4), `FeericoFeat#DADIVA_FEERICA`,
  `HumanoFeat`, `GorgonaFeat` — ~12.
- **Effort:** 1–2 sessions. **Depends on:** Phase 2 for the ones whose slot is gated.

---

## Phase 4 — Active-ability-backed timed effects + Resfriamento ✅ **DONE**

**Built:** `ActiveAbility#getCooldownRounds()` (0 by default) plus a per-sheet ledger —
`CombatantSheet#startCooldown`/`getRemainingCooldown`, an `IdentityHashMap` keyed the same way
`activate` recognises a held ability. `activate` checks it alongside the other gates *before* any
cost is paid, and starts it only *after* the activation has fully succeeded, so a refused one
neither costs nor locks out. It burns down at the **Rodada** boundary (`startNewRound()`),
deliberately not on the `TemporaryEffect` countdown, which ticks at Turn *end* and would return a
Rodada-measured Resfriamento early for whoever acts late in the order — the same reasoning
`scheduleTemporaryEgoPointGrant` already follows.

**Barreira Mágica** — `BarreiraMagicaActiveAbility`, granted by `MetamagicoFeat#ARCANISTA_EXPERIENTE`:
1PA + 3PM for a `DEFESAS` `TemporaryBonus` over 2 Rodadas, Resfriamento 1. The two upgrade rungs
*replace* the figure (+2 → +3 → +5) rather than adding to it, so **only the first rung grants the
ability** and the ability reads the holder's held rungs — three rungs held means one better
Barreira, not three stacking ones. Lands the self half of all three constants.

**Audited and deliberately not landed** — each was on the plan's list, and each turned out to need
something other than the activation transaction:
- `OrquicoFeat#TREMOR` — its Efeito Ativo fires a **single attack**, not a timed state. `activate`
  spends a cost and applies effects lasting N Rodadas; there is no Duração here to hold. The plan's
  "partial" was wrong, and its TODO now says why.
- `ElementalFeat#GANA_ELEMENTAL` — costs **2PD**, and `ActiveAbility` has no Determinação cost
  field. Not added: the clause is blocked on a weapon-scoped Dano Base uplift and dano retyping
  regardless, so a PD cost would have had no reachable consumer.
- `GnomoFeat#MIMETIZAR_COMPETENCIA`'s active half — a **temporary ability** grant, which
  `TemporaryBonus` (a `ModifierType` + value) cannot carry. Its passive half went real in Phase 3.
- The `MESTRE_ARCANISTA`/`DESAFIADOR_DA_REALIDADE` **ally** halves — `activate` applies every
  effect to the activator's own sheet and sees no `Scene`, so there is nobody adjacent to grant to.

**Note:** a Pedra do Poder carries a Resfriamento of its own (`PowerStoneQuality`), item-side and
still inert. Different mechanism; don't conflate them.

**Original plan text follows.**

### Phase 4 (as planned)

The activation transaction (`ActiveAbility` + `ActiveAbilityService#activate`: validate held,
check PA/PM/PV, spend, apply `TemporaryEffect`s) and the `Blessing`/`TargetScope` machinery both
exist. What's missing is **Resfriamento (cooldown)** and the authoring of specific abilities.

- **Resfriamento** — `ActiveAbility#getCooldownRounds()` + a per-sheet cooldown ledger checked by
  `activate`, decremented at the Rodada boundary (`startNewRound`).
- **Barreira Mágica** — `ArcanistaActiveAbility` (1PA + 3PM → +2 Defesas / 2 Rodadas, Resfriamento
  1) and its two upgrades (+3 self & +1 adjacent allies; +5 & +3) via `Blessing` with an
  ally `TargetScope`.
- **Other ready timed states** — audit `OrquicoFeat#TREMOR` (3PA+3PM), `ElementalFeat` Gana (if
  merely a timed bonus and not a form), `GnomoFeat` active half. Author the ones that are pure
  timed bonuses; route the rest to Phase 5.

- Lands: `MetamagicoFeat#ARCANISTA`/`ARCANISTA_EXPERIENTE`/`ARTESAO_DE_BARREIRAS` + 2 upgrade
  constants (~5); `OrquicoFeat#TREMOR` (partial — aftershock needs Phase 9); `ElementalFeat`
  Gana-gated constants become activatable (~4, effects still need Phase 8/9).
- **Effort:** 2 sessions.

---

## Phase 5 — Form state system  ⭐ largest single unlock — **slices 1–2 DONE, 3 partial**

The plan estimated 3–5 sessions, and that was right: the Forma is not one mechanism but a state
plus five independent deltas. Split into slices so each lands whole rather than half-building all
of it. **Slices 1 (the state and the gate) and 2 (entering one) are done**; slice 3 is scoped below.

### Slice 1 — the state and the gate ✅
`sheet.FormType` (8 authored shapes) + `CombatantSheet#getCurrentForm()`/`enterForm`/`isInForm`.
Two decisions worth keeping:
- **No constant for "their own shape"** — that is `null`. `HomemFera`'s rules text lists Humanoide
  alongside the three alternates, but modelling it would give "normal" two spellings; switching
  back to Humanoide is *leaving* the Forma.
- **Named by shape, not by source.** `MONSTRUOSA` is one constant though `Gorgona`, `HomemFera` and
  `MonstruosoFeat` all reach it — "enquanto em sua Forma Monstruosa" asks about the shape.

`enterForm` is an **unvalidating mutator** and nothing transforms anybody automatically, exactly
like `applyCondition`. `CombatantSheet#canTakeForm` is the separate question, combining every held
Talento's `Feat#resolveFormAccess` → `FormAccess` (`FORBIDDEN` refuses that shape; `REQUIRED` locks
the holder in, refusing every other shape *and* their own). Three-valued for the reason
`resolveTitleAcquisitionPermission` is: with booleans, "says nothing" and "says no" collapse.

**Landed:** `GorgonaFeat#MARCA_DA_MALDICAO` (lock), `#ACOLHIDA_POR_FLORA` (forbid), and the
form-gated Resistência a Críticos on both `#PROTECAO_DO_DEUS_DOS_MONSTROS` and
`#PROTECAO_DA_RAINHA_DAS_FADAS` — which needed a new `CombatantSheet`-taking overload of
`Feat#resolveCriticalResistance`, since the Forma lives on the sheet.

### Slice 2 — entering a Forma as a transaction ✅
Reused Phase 4's `ActiveAbility` cycle rather than building a parallel one. Three pieces added to
it: `getDeterminationPointCost()` (Formas are priced in **PD**, which `ActiveAbility` had no field
for), `getReactivationRest()` → a `RestType` (a Resfriamento measured in **Descansos**, its own
ledger, cleared by `RestService#applyRest` at that tier *or stronger*), and `resolveGrantedForm()`.
`activate` now refuses while **either** cooldown is owing, refuses a shape the holder's Talentos
forbid, enters it, and applies a `FormEffect` that returns them to their own shape as the Duração
lapses — the one place leaving a Forma is not a caller's call.

`FormaActiveAbility` is one class for both Talentos written to the same template (3PA + 3PD, 3
Rodadas, not again until a Descanso Longo): **landed `DraconicoFeat#DRACONATO` and
`FeericoFeat#ANCIENTEFORME`** — the transformation, its cost, its Duração and its gate, all real.
Their stat deltas are slice 3, and each constant now says so.

`BestialFeat#METAMORFOSE_SELVAGEM` (2PD) was left: its text states no Duração and its whole payload
is deltas plus an equipment restriction, so entering the shape alone would land nothing.

### Slice 3 — what a Forma actually *does* — **two of five done**
`FormaActiveAbility` grants its uplift as round-scoped `TemporaryBonus`es lasting the Duração,
through `FormaActiveAbility.Uplift` — a per-Título figure per kind, because Draconato and
Ancienteforme disagree on more than one number (+2 to everything, versus +2 size/Defesas but +1
Atributo).

- **`SizeCategory` shift — done.** `CharacterSizeService` gained a `CombatantSheet` overload
  reading `getTemporaryBonus(SIZE_CATEGORY)` on top of the two existing shift sources; the roll
  path routes through it. The `Character`-only overload structurally cannot see it, the same split
  the aggregate PA/Reações/RD reads already carry.
- **Round-scoped Atributo bonus — done, with the mechanism's own documented limit.** An
  `<ATTR>_BONUS` `TemporaryBonus` reaches a Perícia roll governed by that Atributo and nothing
  else. Not a shortcut: PV/PM/PD/Conjuração read `Character#getEffectiveAttributeTotal`, which has
  no sheet, and a three-Rodada Forma raising max PV would need those totals recomputed per Rodada,
  which this core deliberately does not do. Same limit `VampiricoFeat#DOM_DE_MIRCALLA` has always
  had, and the test pins both halves of it.
- **Still open, and now the only one:** a **Multiplicador de PV** uplift in force only while
  transformed (its figure scales per Título Desperto like the rest of that sentence — a
  *permanent* per-Título multiplier is already ordinary, `OrquicoFeat#TERRA_NAS_VEIAS`; what is
  missing is sheet reach, since `getLifeMultiplier` takes a `Character`).
- **Suppression of racial traits — done, as a four-rung ladder.** `RacialTraitSuppression`
  (`NONE` → `NATURAL_WEAPONS_ONLY` → `PHYSICAL` → `ALL`), declared by
  `Feat#resolveRacialTraitSuppression` and folded by `CombatantSheet#getRacialTraitSuppression()`,
  the one question every racial aggregation asks. `DRACONATO`/`ANCIENTEFORME` take `ALL`;
  `MonstruosoFeat#MIMETIZAR_FORMA_HUMANA` takes `PHYSICAL` (its first real clause);
  `METAMORFOSE_DRACULEA`'s weapon swap **was folded in** as `NATURAL_WEAPONS_ONLY` — which is why
  there are four rungs and not three. **Mapping Dracúlea to `PHYSICAL` was this plan's first
  sketch and was wrong**: its text never says "abandonando traços raciais", so that would have
  newly suppressed its holder's RC, anatomy immunities and racial size on the strength of a
  reading already flagged as debatable. Only the `Race` term of a trait is silenced — a
  Talento-granted RC or Arma Natural survives even `ALL`, which is the mechanism's likeliest bug
  and has its own test. Creature type is never suppressed and must not be: Mimetizar's own
  Pré-requisito is `requiredCreatureType`, so silencing it would make the Talento retroactively
  ineligible for its own holder.
  ⚠️ Base Categoria de Tamanho under `PHYSICAL` is an **inference** — the clause enumerates
  appendages ("escamas, chifres, garras") and never stature.
  Partial reach on the two `ALL`-only traits (racial Habilidades, racial Atributo bonuses): both
  need a sheet, so they land on the roll path plus the Defesa/Movimento/Dano sheet overloads and
  not on PV/PM/Conjuração — the documented limit, to be cited per caller rather than per
  mechanism.
- **The audit found the Atributo half was blocked by information loss, not sheet reach** — the
  opposite of what the docs claimed. `CharacterCreationServiceImpl` merged the race's *fixed*
  bonus with the player's *chosen* allocation into one `AttributeValue#racialBonus` int, so
  nothing downstream could tell which part was racial. Now `fixedRacialBonus` +
  `chosenRacialBonus`, with `getRacialBonus()` reporting the sum so every reader (notably
  `FeatRequirements`' "recebe Bônus Racial") is untouched. Worth doing on its own terms: that
  provenance was simply being discarded.
- **`MIMETIZAR_FORMA_HUMANA` still has no activation transaction**, and it is not a small gap:
  "Monstruosos precisam utilizar 2PD … Feéricos utilizam 2PM" prices one ability differently per
  `CreatureType`, and `ActiveAbility`'s cost methods take no arguments. `FormaActiveAbility` does
  not fit either (3PA + 3PD, a 3-Rodada Duração, a Descanso Longo gate — none stated here). The
  shape is entered through the bare `enterForm` mutator meanwhile, as every Forma that no
  `ActiveAbility` grants already is.
- **Arma Natural swap — done, by deriving rather than swapping.** `Feat#getGrantedNaturalWeapons(
  Character, CombatantSheet)` lets a worn shape contribute its own and
  `Feat#replacesNaturalWeaponsWhileInForm` lets it replace the holder's, both aggregated by the new
  **`CombatantSheet#getNaturalWeapons()`**; `Character#getNaturalWeapons()` stays Forma-blind.
  **The rejected alternative was mutating the equipment/weapon list on transforming** — it needs a
  matching restore on all three ways out of a Forma (a `FormEffect` lapsing, `enterForm(null)`, a
  second Forma displacing the first), and any path that forgets silently eats the character's
  gear. Deriving needs no undo, which is the same reasoning `FormEquipmentPolicy` already followed.
  ⚠️ **The *replacement* is a reading, not a transcription**: "armas não podem ser utilizadas, são
  substituídas por armas naturais" takes *armas* as the subject of "são substituídas", so the text
  arguably replaces only weapons; it is read as also cancelling the holder's own Armas Naturais
  because the shapes are whole animals. `FormaMetamorfica`'s javadoc carries the counter-argument.
  Névoa's empty list closes half of "é incapaz de causar danos" for free.
- **Three of the six HABILIDADE rows landed with it** — Aranha's Furtividade Vantagem, Lobo's
  Perícias de Ataque Vantagem, Morcego's Roubo de Vida +2 (which needed a
  `resolveLifeStealBonus(Character, CombatantSheet)` overload). The other three are blocked on
  *different* systems and each constant now says which, rather than sharing one blanket TODO.
- **Fixed a test that asserted the opposite of the rules.** Lobo Dentes-de-Sabre's row ends "pode
  empunhar armas de uma mão com as presas", but `MetamorfoseDraculeaTest` transformed into Lobo
  specifically and asserted a one-handed sword was refused. The clawback is now
  `FormType#permitsOneHandedWeapons()`, the general-suppression test was repointed at Morcego, and
  Lobo has its own test covering both halves.
- **Equipment restrictions — done for the weapons half.** `FormType#getEquipmentPolicy()` returns
  a `FormEquipmentPolicy`, since the rules give two different answers: `WEAPONS_SUPPRESSED` (the
  Metamorfose Dracúlea shapes — defensive items keep working) and `ALL_SUPPRESSED` (Metamorfose
  Selvagem). `CombatantSheet#canAttackWith` is the consumer, exempting Armas Naturais.
  `ANIMAL` is `ALL_SUPPRESSED`, which `HomemFera`'s Animal rung inherits (flagged — its text is
  not in this repo). `ASAS_DE_DRAGAO`'s permanent Capa ban is a different shape and stays out.
- **`BestialFeat#METAMORFOSE_SELVAGEM` landed whole** — 2PD into `FormType.ANIMAL`, with every
  clause resolved *from the Forma* rather than scheduled as a countdown (+2UD Movimento, +2
  Defesas, Vantagem em Ataque e Dano com Armas Naturais, and all equipment suppressed). That
  needed `CombatantSheet holder` overloads on `Feat#resolveMovementIncrease` and
  `resolveDamageBonus`, so all four halves of one clause read the same Forma.
  ⚠️ **Its Duração is a reading, not a transcription**: the clause states none, so the shape is a
  toggle (`ActiveAbility#resolveDurationInRounds()` → `null`). Every other transformation in the
  catalog states a figure; `MetamorfoseSelvagemTest#theShapeNeverLapsesOnItsOwn` is where a later
  source correction would land.
- **`VampiricoFeat#METAMORFOSE_DRACULEA` landed** — the tree's most elaborate constant.
  `FormaMetamorfica` is the six-row table (Arma Natural + Habilidade, feat-scoped since the shapes
  belong to this Talento); `MetamorfoseDraculeaFeat` records the acquisition-time choice and
  validates both lineage rules (Dampiro 1 / Rakshasa 4 / otherwise 2, no Névoa for a Rakshasa);
  each chosen Forma is separately activatable as a Poder Vampírico. That needed a **plural
  `Feat#resolveActiveAbilities`** hook — a single `Optional` cannot carry four, and `activate`
  matches by `==`, so "which shape" has to be part of each ability's identity. The plural hook is
  the reusable piece for the next Talento wanting choosable Formas.

**Landed on top of slice 2:** `DRACONATO`'s size/Força/Foco uplift and `ANCIENTEFORME`'s
Defesas/size/Carisma/Foco one — every half of both Talentos except Ancienteforme's PV multiplier
and both texts' "abandonando seus traços raciais".

**The deltas shared one root cause**, and naming it is what closed three of them: each needed
*sheet*-scoped state to reach a *`Character`*-level aggregate, and the answer every time was a
**sheet-aware twin beside the Forma-blind original** rather than a mutation —
`CombatantSheet#getNaturalWeapons()`, `getRacialTraitSuppression()`,
`SkillCompetencyAbility.allFor(Character, CombatantSheet)` and
`Character#getEffectiveAttributeTotal(domain, CombatantSheet)`. The same move is available to the
one left (`HitPointsService#getLifeMultiplier`, for the PV multiplier). One architectural problem
wearing four hats, not four separate features — and the hat is now a known shape with a house
pattern to match.

**Original plan text follows.**

### Phase 5 (as planned)

A **persistent alternate shape** (entered/exited, not timed) on `CombatantSheet`:
`CharacterForm` carrying — stat deltas, ability grants, `SizeCategory` override, equipment
restrictions, and a set of effects/hooks "active only while in this form". Plus a small
**equipment-restriction** sub-concept ("é impossível usar equipamentos enquanto ativo",
"impede Equipamentos do tipo Capa").

- Design task: write `docs/rules/`-style notes + a `scene`/`character` `package-info` update +
  a new `form-state` subsystem skill.
- Lands: `DraconicoFeat#DRACONATO`/`METAMORFOSE_DRACULEA` (+ unblocks the `AURA_DRACONICA` /
  `SOPRO` chain pending Phase 9), `FeralFeat` Forma Híbrida (×3), `BestialFeat#FORMA_BESTIAL`,
  `GorgonaFeat` forms (×4), `FeericoFeat` forms (×3), `MonstruosoFeat#APARENCIA_MONSTRUOSA`,
  `VampiricoFeat` (×2), plus `HomemFera`/`NascidoDoDragao` racial forms (out of plan scope but
  same mechanism) — **~20 feat constants**.
- **Effort:** 3–5 sessions. **Depends on:** nothing hard; do *after* the harvest phases so the
  design absorbs fewer incidental fixes.

---

## Phase 6 — Flight & vertical / aquatic movement sub-stats

- Parallel movement axes in `MovementService`: **Movimento Base de Voo / de Natação / de
  Escalada**, each resolved the same three-source way as land Movimento.
- A **flight state** — often gated behind a Phase 5 form or a Phase 4 activation; the flag
  itself is small.
- Lands: `AvianoFeat` (whole tree ~5), `DraconicoFeat` flight halves, `FeericoFeat#ASAS_FEERICAS`,
  `BestialFeat` swim/flight, `PeritoFeat#CRIANCA_DO_MAR` / climbing, `MobilidadeFeat#NADADOR`,
  `FeericoFeat#NADADEIRAS` — **~15**.
- **Effort:** 2 sessions. **Depends on:** Phase 5 for form-gated flight; the sub-stat is
  independent.

---

## Phase 7 — Positioning & manoeuvre system

`CLAUDE.md`: *"this core never does geometry"* — so model these **caller-adjudicated**: the
engine reports amounts and legality, the caller/UI applies position, mirroring the
`difficultyReduction` "computed, reported unapplied" precedent.

- **Forced movement** — knockback / "empurrado NUD" / Reposicionar (amount + direction reported).
- **Investida** (charge) as a distinct action with its own movement allowance.
- **Movement-provokes-Reação** + suppression of it ("seu movimento não provoca Reações").
- **Per-manoeuvre movement allowance** (distinct from the per-PA figure).
- **Distance moved this Turn / last Turn** — extend `consumeMovementThisRound` to record a
  distance and survive a Turn boundary (today it resets at `startTurn`).
- Lands: `MobilidadeFeat` (Investida ×3, Reposicionar, exempt-from-Reação ×2, "moved this/last
  Turn" halves), `EscudeiroFeat` (×3), `CavalariaFeat` (partial), `ArtesMarciaisFeat` manoeuvre
  Vantagens, `ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_*` Agarrar/Empurrar/Derrubar — **~12**.
- **Effort:** 2–3 sessions. Consider a **partial** delivery (hooks + reported amounts only).

---

## Phase 8 — Damage-type system

- `DamageType` breakdown — Corte / Perfuração / Impacto, Profano / Natural / Esmagamento.
- **Damage-type-scoped RD/RA** — resolve mitigation with a notion of incoming type
  (`AttributeAbility#resolveDamageReduction` already takes a `DamageDescriptor`; extend to the
  `SkillCompetencyAbility` / `Feat` / equipment paths).
- **Damage retyping** — a `Feat`/ability hook that changes an attack's `DamageType`
  (`OrquicoFeat#PALADINO_DE_EPONA`, `FeralFeat`, `ElementalFeat`).
- **Damage-type immunity** — a nullification stage beyond RD/RA (`MetamagicoFeat` immunity half,
  `ElementalFeat`, `Zumbi` racial).
- Touches `DamageService` core, every `Weapon`'s authored type, and monster stat blocks.
- Lands: `OrquicoFeat` (×3), `FeralFeat` (×2), `ElementalFeat` (×4), `MonstruosoFeat`,
  `MetamagicoFeat`, `GorgonaFeat`, `SobrevivenciaFeat` — **~15**.
- **Effort:** 3–4 sessions. **Depends on:** partly pulled forward by Phase 1b (RM).

---

## Phase 9 — Área de Efeito resolution, area damage, retaliation

- **Footprint resolution** — `scene.grid` turning an `AreaOfEffect` + a per-cast aim into a set
  of targets; LINHA/CONE facing as arguments; caster exclusion; Foco-scaled footprint growth.
- **Incoming-attack "is area" flag** on `AttackDelivery`/`AttackReceiver` (unblocks
  `EsquivaEApararCompetencyAbility#EVASAO`, `AbencoadoPelaLuzAbility` too).
- **Outward area damage at Turn end** — the Aura shape (`DraconicoFeat#AURA_DRACONICA`,
  `ElementalFeat`, `OrquicoFeat` aftershock, `TrollFeat`).
- **Reactive / retaliation damage** — `DamageService` reverse path + a trigger fired on taking
  damage (also serves `FeralFeat`/`TrollFeat` Regeneração Reativa — the healing counterpart).
- Lands: `DuelistaFeat#ATAQUE_GIRATORIO`/`GOLPE_TEMPESTADE`, `MobilidadeFeat#INVESTIDA_*`,
  `OrquicoFeat#TREMOR`/aftershock, `ElementalFeat` (×3), `DraconicoFeat#AURA_DRACONICA`,
  `MonstruosoFeat` retaliation (×2), `FeralFeat`/`TrollFeat` Regeneração Reativa,
  `SobrevivenciaFeat` — **~18**.
- **Effort:** 4+ sessions — the largest. **Depends on:** Phase 4 (activation), Phase 7 (grid).

---

## Phase 10 — Spellcasting extensions

- **Mimetizar** — an acquisition-less casting path: cast a Magia the caster never learned
  (`SpellCastingService` gains a "mimicked spell" entry point that skips `Spell#isEligible`).
- **Magia storage** — cast now, release later as an Ação Livre (`MetamagicoFeat#ARMAZENAR_MAGIA`
  ×2). A per-sheet "held cast" slot.
- **Spell numeric-effect uplift** (+2 to structured numeric effects), **Duração uplift**,
  **casting-time reduction** (`Spell#getActivationTime()` column exists), **delayed effect**
  ("inicia 1 Rodada após a conjuração").
- **Mana cost resolved through a service** — `Spell#getManaCost()` exists but nothing spends it.
- Lands: `MetamagicoFeat` Aptidão ladder (~6), `ARMAZENAR_MAGIA` (×2), `ARTESAO` ladder,
  `GnomoFeat`, `GorgonaFeat`, `ElficoFeat` (×2), `FeericoFeat`, `FadasFeat`, `FuriasFeat`,
  `ElementalFeat` — **~18**.
- **Effort:** 3 sessions. **Depends on:** Phase 8 for spell damage type; `magic-system` skill.

---

## Phase 11 — Montaria / veículo

A mount/vehicle the holder is "on": its own Pontos de Ação and movement, and an "enquanto
montado / dirigindo" state.
- Lands: `CavalariaFeat` (whole tree ~6), parts of `MobilidadeFeat`.
- **Effort:** 2–3 sessions. Self-contained — schedulable any time after Phase 1. Low priority
  unless Cavalaria matters to a campaign.

---

## Phase 12 — Título / Destino / Vampiro relational systems

Deep, narrow-consumer systems — do last.
- **Despertar timeline** — title-awakening schedule, PV/PM-multiplier uplift on awakening,
  ordering constraints ("adquirido antes de Despertar"), EXP thresholds.
- **Título Habilidade activation cost** — `AventyrTitleAbility` has no cost column.
- **Centelha** — a possess/lack resource (also a Regalia-crafting gate today adjudicated by GM).
- **Laços-de-Sangue** — master/progeny relation between Vampiros; **gerar Prole** (creating a
  `Character` at runtime).
- **PD-equivalent rest recovery** — a Determinação twin of `resolveRestMagicPointsBonus`.
- **Favoritismo** — unmodelled social mechanic.
- Lands: `DestinoFeat` (whole tree ~10), `VampiricoFeat` (×5), parts of `DraconicoFeat` /
  `AventyrTitle`.
- **Effort:** 3–4 sessions.

---

## Won't schedule — keep as sharpened comments

These are blocked by design decisions, not missing work. Leave a one-line TODO citing the
reason; revisit only if a scheduled phase incidentally enables one.

| Blocker | Constants |
| --- | --- |
| **Core never rolls dice** — reroll a die, reroll lowest die | `ArtilhariaFeat`, `DuelistaFeat` (×3), `PeritoFeat` (×3) |
| **An attack is caller-initiated** — "grants an extra attack / projectile" | `EscudeiroFeat`, `ArtilhariaFeat` (×2), `DuelistaFeat` (×2) |
| **Narrative-purpose scoping** — "rolagens relacionadas a animais", "para criar equipamento", "para se aproximar de aliados" | `GoblinFeat`, `BestialFeat#FARO_APURADO`, `PeritoFeat`, `GiganteFeat` |
| **Pure geometry / distance falloff** — "−1 para cada UD percorrido", terrain mapping in Distância Média | `ElementalFeat`, `BestialFeat#ECOLOCALIZACAO` |
| **State that exempts from nothing** — breathing/sleep exemptions where the state isn't tracked and nothing charges for it | `BestialFeat`, `AvianoFeat`, `PeritoFeat`, `ElficoFeat`, `TrollFeat#SONO_DE_PEDRA` |
| **"count of activations of one specific ability"** — distinct from the roll-action log | `ArtilhariaFeat`, `CavalariaFeat`, `OrquicoFeat` "uma vez a cada Rodada" halves |

---

## Dependency summary

```
Phase 0 (stale sweep) ──► everything (shrinks each later phase)
Phase 1 (small stats) ──► independent
Phase 2 (FeatRequirements) ──► Phase 3 (gated slots)
Phase 4 (activation + Resfriamento) ──► Phase 9 (activated auras)
Phase 5 (form state) ──► Phase 6 (form-gated flight), parts of 9, 12
Phase 7 (positioning/grid) ──► Phase 9 (area footprint)
Phase 1b (RM) ──► pulled from Phase 8 (damage-type)
Phase 8 (damage-type) ──► Phase 10 (spell damage type)
Phase 11 (montaria) ──► independent, any time after Phase 1
```

## Per-phase checklist (`CLAUDE.md` conventions)

1. Build the mechanic for its **real** consumers only — no speculative widening.
2. Wire every waiting constant; write effect-level tests (`testing-a-feat` skill).
3. Re-scan constants that cite the mechanic — some have a *second* blocker.
4. Update the subsystem skill, the `CLAUDE.md` gap-catalog row, and any `package-info.java`.
5. If rules text for a skill was leaned on as precedent elsewhere, fix those citations too.
