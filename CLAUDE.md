# Aventyrs Core — Project Conventions

Rules-engine core for the Aventyrs tabletop game. Pure Java library (Lombok + JUnit 5 + Gradle),
no framework dependencies — see `org.aventyrs.core.skill.attention.Attention`/
`org.aventyrs.core.skill.artes.Artes` and their `Interaction`s for the reference
implementation of the skill machinery, and the subsystem-skill index below for everything
else. Every Perícia's classes live together under their own
subpackage of `org.aventyrs.core.skill` (e.g. `org.aventyrs.core.skill.artes` holds every
`Artes*` class) — only the shared, cross-skill machinery (`AbstractSkillInteraction`,
`Skill`, `SkillType`, `SkillCompetencyAbility`, `SkillExcellency`, `SkillRoll`,
`DifficultyLevel`, etc.) stays directly in `org.aventyrs.core.skill` itself.

## Recurring conventions

These hold across every subsystem skill and section; they aren't repeated per-feature.

- **Build for the second real consumer, not the first hypothetical one.** Don't generalize a
  shape, widen a shared interface, or add a mechanism speculatively — wait until a second real
  case needs the identical shape. A method stays on its own concrete class until then (see
  `ArtesAprimorarComArteAbility#getCriticalMarginReduction`, still parameterized by a
  dynamically-chosen Perícia and consumed by nothing). Promotion is earned, not assumed:
  `resolveCriticalMarginIncrease` did earn it, and now sits on all three of `EgoAdvantage`/
  `AttributeAbility`/`SkillCompetencyAbility`. Conversely, several pieces here *were* built
  ahead of a consumer on purpose (`ReactionsService`, `InitiativeService`, `AcquiredChoice`,
  `CharacterSheet#startTurn`); that's noted where it applies.
- **Cascading overloads.** When a computation grows a new optional input, add a longer overload
  and have every shorter one delegate down with `null`; the longest holds all the real logic. A
  subclass overrides the **longest** overload it needs, never a shorter one — virtual dispatch
  still routes the short forms to it. Used by `AbstractSkillInteraction#applyTo`
  (`CombatantSheet` → `+SceneContext` → `+SkillRoll` → `+attackTarget` → `+AttackSource`, the
  last holding all the logic), `DamageService`, `DamageInteraction`, `SceneContext`, and `Scene
  #addParticipant`. Two deliberate non-cascades to know: `ActionPointsService`'s `Character`/
  `CombatantSheet` pair and `DamageBaseService`'s `Weapon`/`SkillType` pair are *different
  questions*, not optional inputs, and don't delegate.
- **"Can't apply it yet" doesn't mean "can't compute it yet."** A formula is real, tested data
  even when the stat it feeds is entirely missing — TODO the *application*, not the
  *arithmetic*, and say which is which. This extends to *granting*: a `Blessing` typed to a
  `ModifierType` no one reads yet is still granted for real. It does **not** extend to a
  mechanism with no entry point at all (RA has no `TemporaryBonus` grant path, so an
  RA-granting clause genuinely can't be expressed).
- **The three-source scan.** Character-level stats aggregated from abilities sum
  `@Modifier`/`ModifierType` bonuses across `attributeAbilities`, `skillCompetencyAbilities`,
  and — per trained Perícia — that Perícia's unlocked `SkillExcellency` tiers. Prefer
  `SkillCompetencyAbility.allFor` so racial abilities are included (`ReactionsService`/
  `InitiativeService` predate that fix and still scan only the acquired list).
  `resolveInitiativeBlessings` is the one deliberate three-source-minus-excellencies exception.
- **A no-arg `@Modifier` method can't see context.** `ModifierResolver.invoke` always calls
  with zero arguments and `@Modifier`'s `ModifierType` is a compile-time-fixed annotation
  value. Anything conditioned on a `SceneContext`, an attack target, the rolled `SkillType`, or
  the holder's own live state must be a `default resolve*` method on the ability interface
  instead. Items are data (`ItemBonus`) for the same reason — don't route them through
  `ModifierResolver`.
- **Possession is validated; eligibility mostly isn't.** This core refuses a roll naming a
  trait the character never acquired, but doesn't check that a trait's own mechanic is
  implemented or that an acquisition was legal. "Requer N Graduações"-style clauses stay
  unenforced comments — the only two exceptions are `AventyrTitleAbility` and `Feat`
  prerequisites.
- **Builder-bypassable invariants.** Caps and prerequisites are enforced only on the service
  entry point that applies them; `Character.builder()`, Fixture Factory templates, and plain
  mutators bypass them by design, and tests routinely rely on that.
- **TODO discipline.** State what the trait is supposed to do and which *specific* missing
  system blocks it — cite from the catalog below rather than a blanket "unimplemented," and
  one line per genuinely separate missing piece. When a mechanism gets built, don't assume it
  finished every trait citing it; check each constant's own TODO. When rules text for a skill
  is revised, fix anything elsewhere citing it as a precedent in the same change.
- **A `Feat` can see its holder's sheet.** `resolveSkillRollBonus`/`resolveDefenseBonus`/
  `resolveDamageReduction` each have a `CombatantSheet`-taking overload, which is what reaches
  held `Condição`s, the per-Rodada action log, temporary bonuses and current PV — none of which a
  `Character` can. **On `Feat` the longer form *defaults to* the shorter**, the opposite of the
  cascading convention elsewhere, so every existing override keeps working untouched; a
  sheet-conditioned constant overrides the longest and returns its own unconditional value too.
  `null` (a `Character`-only entry point, or a bonuses-only preview) reads as "condition not met".
- **Drawing a weapon is an action, and goes through `WeaponDrawService`.** It prices the draw
  (`DEFAULT_DRAW_COST` = 1PA, **an inference** — no rules text states it; `SAQUE_RAPIDO` makes it
  an Ação Livre via `Feat#drawsWeaponAsFreeAction`) and gates it (carried, not already drawn, and
  no Condição with `preventsArming()` — Devorado). `Character#drawWeapon` stays the unvalidating
  mutator beneath it, the same split `ActiveAbilityService#activate` draws. **The PA are reported,
  not deducted** — this core keeps no spent-this-Turn pool.
- **"Utilizando uma arma" means *drawn*, not equipped.** `Character#drawnWeapons` is the in-hand
  subset of `equipment` (`drawWeapon`/`sheatheWeapon`/`isDrawn`/`isWieldingAWeapon`), and drawing
  is itself an action. A sheathed blade costs a martial artist nothing —
  `ArtesMarciaisFeat#DEFESA_DE_MAOS_LIMPAS` and `CombatantSheet#disarm` both read the drawn list,
  never `getEquipment()`. **Deliberately no hand slots**: no entry in `equipment.txt` states a
  weapon's handedness, so a two-handed weapon has nothing to declare itself with, and every
  Talento reading this asks only "is anything drawn", never *which* hand. `CharacterFixture`
  defaults it to an immutable `List.of()`, same caveat as `feats`/`equipment`.
- **A critical hit rolls one extra die, plus a flat +2** — 2d6+1 crits for 3d6+1, then +2, then
  the attack's own bonuses. **Any "+1d6" effect respects `DamageBase.MAX_DICE`**: at 3 dice the
  extra die becomes +2 instead, so 3d6+1 becomes 3d6+3. That rule is general, not
  critical-specific, and applies to every clause granting an extra die.
- **Every movement figure is per Ponto de Ação.** A UD amount named by any movement clause —
  a permanent `ModifierType.MOVEMENT` bonus, a Round-scoped `TemporaryBonus`, or one scoped to a
  particular movement of the Rodada (`resolveRoundMovementIncrease`) — always widens what *one*
  Ponto de Ação buys; it is never a one-off distance added once to a movement's total. Movimento
  Base 4 with +2UD over 3 Pontos de Ação is (4+2)x3 = 18UD, never 4x3+2 = 14UD. `MovementService`
  therefore returns the per-point figure and never multiplies — how many points go to moving is
  the player's call. See the `aggregated-character-stats` skill for the three axes a movement
  clause can land on.
- **Vantagem is a flat +2 on a *dano* roll too, and dano bonuses sum.** `Skill#ADVANTAGE_BONUS`/
  `DISADVANTAGE_MALUS` apply unchanged to a dano roll. Every source contributing to one —
  `SkillCompetencyAbility`/`EgoAdvantage`/`Feat#resolveDamageBonus`, a `TemporaryBonus` or a
  `Condition` carrying `ModifierType.DAMAGE_ROLL_BONUS`, and the *target's* outward-facing
  `ConditionType#getAttackerDamageBonus` (Flanqueado) — is **summed** by `DamageBonus#total`, not
  first-wins. Mixed `DamageType`s flatten to the first typed contributor's; an untyped-only total
  is `FISICO`. A net of zero reports no bonus at all.
- **This core never rolls dice, never does geometry, and never tracks what a roll is *for*.**
  Dice results, distances, and initiative values all arrive already resolved from a caller. A
  bonus scoped to a narrative *purpose* ("only for animal-related rolls") can't be modeled —
  document the simplification on the constant rather than silently narrowing or over-granting.
  A scope of specific *named* skills is trackable, and does have a hook. So is a scope of *what
  the attack was made with* — `Weapon` and `Spell` are both `AttackSource`s and reach a roll;
  don't file a new clause under this restriction without checking which of the three it is.

## Missing systems — the gap catalog

Check here before assuming a TODO needs a new gap named. Nothing below exists in this core.

| Missing system | Notes / where cited |
| --- | --- |
| **Defesas — *mostly built*** | `DefenseService` + `DefenseType` are real, and `DEFESAS`/`PHYSICAL_DEFENSE`/`MAGIC_DEFENSE` all have readers. What's still missing is narrower: `Santo#getDefesasBonus` has no granting trigger (*when* each adjacent ally receives it), and a foe's Defesa is an authored flat number with no defined conversion from a GD reduction's *níveis* — so an attacker-side `difficultyReduction` (now including `AssassinoFeat#SAQUE_RELAMPAGO`'s "-1 nível") is computed but reported unapplied on the `AttackDelivery` path (`DeliveredAttackResult#getUnappliedDifficultyReduction()`), while it applies for real on the direct skill-roll path and via `AttackReceiver`. Don't cite this as "no Defesas stat exists".
| **Owned/produced item copy** | The `Item` *catalog* is real, and so is inventory now — `Character#equipment` (worn/wielded, scanned by `DefenseService`/`DamageService`) and `AbstractCombatantSheet#inventory` (carried, including a foe's loot). Per-copy state built: **Dureza remaining** (`Item#applyDamage`/`getCurrentHardness`/`isDestroyed`, mitigated by the item's own enhancements only — see "Damage to an item…"), a fitted **Obra-Prima/Aprimoramento** (`DefensiveMasterpiece`/`DefensiveImprovement` + `ItemMasterpiece`/`ItemImprovement`), and a socketed **Pedra do Poder** (`PowerStone` = `PowerStoneType` + `PowerStoneQuality` + optional masterpiece/improvement, gated on `DefensiveImprovement.ENCAIXE` via `AbstractItem#setPowerStone`). Still missing: who produced it, a PE economy, production/**repair** (damage is one-way), the offensive Obra-Prima/Aprimoramento catalogs, and the Pedra do Poder **charge/Resfriamento/Vinculação economy** (authored data, no activation or bind step). Cite the specific piece, not a blanket "no Item entity", "no inventory" or "no Dureza". |
| **Classifying an attack as Desarmado/Arma Natural — *closed off the roll path*** | The Arma Natural marker is real: `ItemCategory.NATURAL_WEAPON` is the column, and **`Character#treatsAsNaturalWeapon(Weapon)` is the single view every clause consults** — never the raw category, because `Feat#reclassifiesAsNaturalWeapon` lets a Talento make an ordinary weapon count as natural *for its holder* (`ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_FERROADA_ESMAGADORA`), and its rules text requires that to be visible to every other Arma-Natural clause (`ARTISTA_MARCIAL`, `DEFESA_DE_MAOS_LIMPAS`, `MonstruosoFeat`, `DefensiveImprovement#BENCAO_SELVAGEM` all route through it). Reclassification is additive and one-directional — nothing can make a real Arma Natural stop counting. Desarmado is unambiguous **on the Dano Base path only**, where `DamageBaseService`'s two overloads make `weapon == null` mean it. **Still missing:** (a) on the **Perícia-roll path** an Ataque Desarmado has no `AttackSource`, so `AbstractSkillInteraction#applyTo`'s 5th parameter is `null` for both "unarmed" and "caller didn't say" — every consumer of that distinction is blocked on something else too (a dano-roll malus, condition suppression, retaliation damage), so don't add the marker without a real consumer; (b) **no Arma Natural is authored** — `docs/rules/equipamentos.txt` lists 12 (Garras Afiadas, Presas Longas, Chifres Poderosos, Cauda Chicote, Arma de Sopro…), but no weapon catalogue exists at all yet, and a Talento *granting* one (`BestialFeat`, `DraconicoFeat`, `FeralFeat`) has no mechanism either. |
| **Damage-type-scoped mitigation, and damage-type immunity** | `DamageType` has no Corte/Perfuração/Impacto breakdown (nor Profano/Natural/Esmagamento), and RD/RA are resolved with no notion of damage type — the one exception is `AttributeAbility#resolveDamageReduction`, unreachable from a `SkillCompetencyAbility`. *Nullifying* a damage type outright is a further missing stage: there is no immunity mechanism of any kind. Cited by `Zumbi` (imune a Profanos/Naturais, -3 vs Esmagamento). |
| **Multiplicative stages** | `MovementService` sums `MOVEMENT` additively with no halving stage (unlike `DamageService`'s real `HALF_DAMAGE`). Don't add a `MOVEMENT_HALVED` constant — the mechanism is missing, not just a reader. |
| **Movimento Base — *the per-movement axis is built*** | `MovementService#getMovementBase(CombatantSheet[, int movementIndex])` resolves `getTemporaryBonus(MOVEMENT)` **and** `resolveRoundMovementIncrease` across `AttributeAbility`/`SkillCompetencyAbility`/`Feat`/equipped `Item`, indexed by `CombatantSheet#consumeMovementThisRound()`. That closes "which movement of the Rodada is this" (`DexterityAbility#PASSOS_LONGOS`, `MobilidadeFeat#VELOCISTA`). Still missing, and what a movement TODO should cite instead: a movement's **distance** and **direction** are not recorded, and the counter resets at `startTurn`, so nothing knows a character moved in a *previous* Turn (unlike the roll-action log, which resets at `startNewRound` and `startTurn` only slices — don't conflate the two). Investida/Reposicionar are still unmodelled manoeuvres. |
| **Temporary PA/Reação/Ação Livre grants — *built*** | Closed. The `CombatantSheet`-taking overloads of `ActionPointsService#getMaxActionPoints`/`ReactionsService#getTotalReactions`/`FreeActionsService#getTotalFreeActions` read `getTemporaryBonus(ACTION_POINTS/REACTIONS/FREE_ACTIONS)` for real. The `Character`-only overloads still can't — no sheet to ask — so cite *that* if a caller only holds a `Character`, not "the mechanism is missing". |
| **Temporary RA grants** | `getTotalAbsoluteDamageReduction` never reads `getTemporaryBonus(ABSOLUTE_DAMAGE_REDUCTION)` — RA comes only from continuously-scanned passive hooks. |
| **Round-scoped Attribute bonuses** | `AttributeValue` has only `base`/`racialBonus`/`variable`, all permanent — never summed via `ModifierType`. |
| **Roll-resolution engine — *built*** | A roll now knows the GD it was made against and reports the verdict. `SkillRoll` carries an optional `targetValue` (a plain int — `DifficultyLevel#getBaseValue()` is the usual source, but `ConditionType#DEVORADO`'s "GD 10+Vigor" has no tier, and `SkillRoll.against(dice, tier)` is the convenience for when it does) and an optional `ActionCost` (roll-metadata — PA / Ação Livre / Reação — *not* a resolution input; read only by `Feat#resolveAttackCostDifficultyReduction`); `InteractionResult` reports `succeeded`, a signed `margin`, and the resolved `governingAttributeDomain` (for building a `CombatantAction`). **Three states, not two** — both are `null` when no target was stated, which an ability gated on success must read as "cannot tell", never "failed". A tie succeeds, and a held `difficultyReduction` eases the target by whole *níveis* before comparing, mirroring `AttackReceiver#resolve`. Two hooks hang off it: `SkillCompetencyAbility#resolveAutomaticSuccess(SkillType, int targetValue, SceneContext)` ("sempre bem-sucedido, dispensando rolagens" — takes the target because auto-success is routinely GD-capped) and `#resolveSuccessBlessings(...)`, resolved **only when `succeeded` is true** and reported on `InteractionResult#blessings` for the caller to grant. Still missing, and what a TODO should cite instead: `SkillExcellency` has no `resolve*` hook at all, so an Excelência cannot see a target GD or a `SceneContext` (`MedicinaECuraExcellency#FOCADO`); a `TemporaryBonus` is not consumed on first use, so "sua *próxima* rolagem" over-grants for the rest of its duration; and nothing rerolls — repeating a roll is the caller's step. |
| **Area de Efeito — *described, not resolved*** | The footprint is real data: `scene.AreaOfEffect` (an `AreaShape` — CIRCULO/LINHA/CONE/PENETRANTE/EXPLOSAO — plus one length in UD), reachable from `Spell#getTargeting()`, and ~35 authored Magias now supply one. Four things are still missing, so cite the specific one: (a) **footprint resolution** — nothing turns an area into a set of hexes or targets; a LINHA/CONE additionally needs a *facing*, which is chosen per cast, not authored on the Magia, so this belongs in `scene.grid` taking the aim as arguments; (b) **no classification of an incoming attack as an area one** — `AttackDelivery`/`AttackReceiver` carry no such flag, which is what still blocks `EsquivaEApararCompetencyAbility.EVASAO` and `AbencoadoPelaLuzAbility`; (c) **caster exclusion** — "a Conjurador is never damaged by their own Magia" is a universal rule, so it is deliberately *not* a column anywhere (no `excludesCaster` flag); it belongs to the missing targeting resolution, and `Spell` has no damage column to test against anyway; (d) **a Foco-scaled footprint** — four Magias grow their area at Foco 5 or above ("aumenta para Média se tiver Foco 5 ou superior"), and `AreaOfEffect` holds one fixed length, so each is authored at its base size with the growth clause in its prose. That needs a footprint resolvable against a sheet, which is (a). |
| **Malefício classification — *built*** | The Condições catalogue is real: `sheet.ConditionType` (17 entries, authored from `docs/rules/condicoes-e-maleficios-.txt`) + `sheet.Condition` (a `TemporaryEffect`, so Rodada countdown and expiry come free). Effects reach the engine three ways — typed `ConditionEffect` maluses summed by `CombatantSheet#getConditionBonus` into `DefenseService`/`AbstractSkillInteraction`; `getImplied()` conditions resolved transitively and deduplicated by type (Caído confers Desprevenido, and two sources conferring it still cost -2 once); and the outright prohibitions (`isMovementPrevented`/`isHealingPrevented`/`isAbilityActivationPrevented`/`isSpellCastingPrevented`), gating `MovementService`, `heal`, `ActiveAbilityService` and `SpellCastingService`. The fear ladder decays on expiry (`getDecaysTo`). **What's still missing, and what a TODO should cite instead:** (a) nothing *applies* a condition automatically — no critical-hit, attack or Talento hook affects a target, so every `applyCondition` is a caller's call (`ABRIR_DEFESAS`); (b) **a held trait can see its holder's Condições only where a hook has a `CombatantSheet`-taking overload** — `Feat#resolveSkillRollBonus`/`resolveDefenseBonus`/`resolveDamageReduction` now do (their longest form takes a `holder` and falls through to the sheet-less form; `AssassinoFeat#SAQUE_RAPIDO`'s drawn-weapon Desvantagem and `AssassinoFeat#ESCUDO_DE_SOMBRAS`'s Escondido +3 are real through them). `Feat#resolveCriticalMarginIncrease` has a `holder` overload too now (`AssassinoFeat#ACERTO_CRITICO_RELAMPAGO`). Still sheet-less, and so still blocking "enquanto estiver X": every `SkillCompetencyAbility` resolve hook (`ACAO_SURPRESA`, `MORTE_OCULTA`) and `Feat#resolveMovementIncrease` (`MOVIMENTO_FURTIVO`); (c) no per-condition **immunity or suppression** — a trait cannot be exempt from a Malefício, nor veto an implication (`ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_SUBMISSAO`'s "não Desprevenido enquanto Caído"); (d) three authored entries are inert pending systems of their own — Cego (needs a d6 sub-roll), Devorado, Envenenado ("Multiplicador de Bônus Base" exists nowhere), Doente. **`DESARMADO` means *disarmed*, never *unarmed*** — the effect that inflicts it is `CombatantSheet#disarm(Weapon)` (unequips, hands the weapon back since this core models no ground, and applies the condition only once no wielded `Weapon` remains); `rearm` lifts it, which is why it is open-ended rather than counting down. A weapon can refuse via `Weapon#isDisarmable()` ("Não pode ser desarmado", the Manopla de Segurança Aprimoramento). An **Ataque Desarmado** is a different thing entirely: a punch, authored as its own Arma Natural — it starts at `DamageBase.UNARMED` (the bottom rung) and still takes every Talento/Habilidade scale-up, while correctly taking none of the enhancement scale-ups bound to a weapon there isn't one of. `DEVORADO` leans on exactly that: it leaves you unarmed and additionally blocks re-arming (`ConditionType#preventsRearming()` → `rearm` refuses), since nothing you dropped is reachable from inside a creature — but it does **not** confer `DESARMADO`, whose Desvantagem the rules never charge a swallowed character. **Two things the rules text calls Malefícios are not `ConditionType` constants and must not become them**: *Coma* is `CharacterStatus#COMMA` (a PV tier), and *Encantamento* is `MagicType#ENCANTAMENTO` (a Magia — nothing tracks which Magias currently affect a combatant). `ESCONDIDO` is in the enum but not in the source file, being a Condição rather than a Malefício. Don't cite this as "no Malefício classification exists". |
| **Living/undead classification** | No vitality tag on `Character`. `CreatureType` has only HUMANOIDE/FEERICO/MONSTRUOSO, none of which is about being alive. `MonsterTemplate#isUndead()` is a deliberately narrow stand-in — exact for every combatant this core can build, wrong the day a player character can be undead or a construct must count as non-living. |
| **A summon acting on its summoner's roll** | `SummonedMonsterTemplate` builds a creature a Conjurador raised, but nothing models the player then *rolling for it*. `AttackDelivery` assumes the roller is the attacker and `AttackReceiver` that they're the defender; neither has a notion of rolling on a third combatant's behalf. This is why `CriticalEffect#applicableTo` is shared between them. |
| **Fadiga/asfixia, and healing inversion** | Nothing tracks sleep or breathing, so "não precisam dormir ou respirar" has no effect to be exempt from; and `CombatantSheet#heal` has no hook to redirect a recovery into damage (`Zumbi`'s Divine-magic clause — note `Zumbi` **is** `ReanimarSpell.REANIMAR`'s stat block, and what's missing between them is only a `Spell`-to-`MonsterTemplate` link). |
| **A foe's own dano roll — *half-closed*** | `DamageBase` now models exactly a "1d6+3"-shaped figure, so a stat block's "Danos de Ataques" finally has a type to live in — but `MonsterTemplate` has no column for it and `AttackDelivery`/`AttackReceiver` still assemble a `DamageInteraction` with the caller supplying the number. This core still never rolls the dice. Cite the missing *column*, not a missing concept. |
| **Attack maximum range — *partly built*** | An attack's max distance is a property of the Weapon or Spell, never a character stat. Built: `Weapon#getRange()` (authored Alcance, `@Builder.Default` `Range.ADJACENTE` — melee weapons carry one too) / `getEffectiveRange()` (→ `ADJACENTE` once destroyed, mirroring `getEffectiveDamageBase`), a Magia's `getTargeting().range()`, `Range#increasedBy(int)` (band-ladder shift, clamped both ends), `AttackRangeService#getEffectiveRange(Character, Weapon\|Spell)` (weapon returns a `Range`, spell an `Optional<Range>` — empty for Pessoal/Toque/Planar/caster-centred), and `Feat#resolveAttackRangeIncrease(Character, AttackSource)` — the **first `Feat` hook to take an `AttackSource`** — feeding `AttackRangeService` a step count. `ArtilhariaFeat#TIRO_LONGO`'s flat "+1 nível" half is real through it, scoped to `ATAQUE_A_DISTANCIA` delivery. Still missing: no `SkillCompetencyAbility`/`AttributeAbility` range hook (added with its first consumer); no equipment scan (the offensive Obra-Prima/Aprimoramento catalog — "Alcance Estendido" — doesn't exist, and Arco Longo's "Alcance Base muda para" Favor is a *replacement* with no `ModifierType`); `TIRO_LONGO`'s "+2 níveis com Mira Impecável" half needs "this one delivered attack" scoping; and nothing gates an attack on being *in* range — `AttackDelivery`/`SpellCastingService#validateRequest` still never compare `range()` to the target's distance. |
| **Forced attack targeting / interception** | No "another Character becomes the target instead" mid-resolution — see `SantoAbility.GUARDA_VIDAS`. |
| **Reactive/retaliation damage** | `DamageService` only computes damage *to* a target *from* an attacker, never the reverse. |
| **Forced movement / positioning** | Knockback, "empurrado 1UD", Reposicionar — this core never does geometry. |
| **Continuous cross-character passive grants** | Partly built: `AventyrTitleAbility#resolveAllyAbsoluteDamageReduction` scans a target's adjacent allies for outward RA grants (Santo's Bastião dos Necessitados). Still missing for Defesas (`Santo` Despertar — its bonus is on the concrete class, unreachable by a scan) and for `SkillCompetencyAbility` (`INSTINTO_DE_LUTHER`). See the `damage-and-combat` skill, "Ally-facing passive grants are scanned, not granted". |
| **Movement-triggered Reações** | No movement-triggers-Reação mechanism, and no suppression of one. Cited by `POSICIONAMENTO_ESTRATEGICO` and `AS_NA_MANGA` — but note both of those grant their *movement* half for real. A clause exempting movement from Reações is currently **exempt from nothing**, so it costs nothing to omit; it becomes real the day this lands, and both constants need revisiting then. |
| **Resource-spend triggers — *built for Ego points*** | Closed for Ego: `EgoPointsService#useEgoPointsForEffect` spends and resolves the holder's `EgoAdvantage` against the completed `EgoPointSpend` in one call, which is how `DETERMINACAO_HEROICA` works for real. **A deliberate *use* and an enemy's *drain* are different call sites, not a flag** — `Primor` calls `CombatantSheet#spendEgoPoints` directly and triggers nothing, which is what stops a critical hit healing its victim. `AS_NA_MANGA` is real through the same hook (`resolveEgoSpendBlessings`, granting +2UD). Still missing: PV/PM/PD spends have no report or reaction path at all. **A *defeat* trigger is built** — `DefeatBlessingService#applyDefeatBlessings(attacker, defeated, viaCriticalHit)`, caller-driven (this core still has no true observer): after an attack the caller determined was fatal, it scans the attacker's `Feat#resolveDefeatBlessings` and applies each `Blessing` — `AssassinoFeat#SANGUE_QUENTE`/`VIOLENCIA_DESCOMUNAL`/`ARCANISMO_AVASSALADOR`. |
| **One-time roll effects bought with a resource** | Spending PV/PM to modify a single roll's outcome (e.g. a GD reduction) has no transaction — see `Orc`'s Agnação Ancestral. |
| **"This one delivered attack" scoping** | A bonus scoped to the single attack delivered by activating another ability fits no per-roll `resolve*` hook, which are all generic per skill type. |
| **Within-Turn activation counter — *a roll-action log is built, per Rodada and per Cena*** | `CombatantSheet#recordAction(CombatantAction)` feeds two logs: `getActionsThisRound()` (skill, resolved governing `AttributeDomain`, `AttackSource`, `ActionCost`, `turnNumber`, `ActionOutcome`), cleared by `startNewRound()` (`Scene#next()` at the Rodada wrap), with `startTurn` marking a per-Turn slice read by `isFirstRollOfTurnFor`/`isFirstAttackRollOfTurn` (this *replaced* `consumeFirstRollThisTurn`); and `getActionsThisCena()`, cleared only by `startNewScene()` (`Scene#addParticipant`), for a "primeira ... na Cena" clause. `hasDrawnWeaponThisScene()` mirrors `hasDrawnWeaponThisTurn()`. `Feat#resolveCriticalMarginIncrease` and `resolveSkillRollBonus` have `CombatantSheet`-taking overloads that read these — `AssassinoFeat#SAQUE_RELAMPAGO`'s rider and `ACERTO_CRITICO_RELAMPAGO` are real through them. **The API records** — `applyTo` only reads, never writes. Still missing: nothing auto-records; no *count of activations of one specific ability*; roll-actions only (movement has `consumeMovementThisRound`); no scene-*end* trigger (so "até o fim da Cena" is approximated with a long Rodada count — `AssassinoFeat#ARCANISMO_AVASSALADOR`). |
| **Game-session tracking — *the boundary is the consumer's, the state is missing*** | The end-of-session *trigger* is deliberately outside this core: a Narrador presses a button, and the consumer calls `EgoPointsService#applySessionRecovery(Map<CombatantSheet, EgoDomain>)` — one call carrying the table's per-player choices. `MOTIVACAO_DE_MOSES`/`DILETO_DE_TYKHE` are fully real through it. What's still absent is any per-session **state**: no session identity, no counter, nothing recording that a session happened. Hence recovery is deliberately **not idempotent** (double-application is the consumer's to prevent), and a clause that must *count* within a session — `ESTABILIDADE_EMOCIONAL`'s "a primeira vez em cada sessão", `MeioElfo`'s "1x por sessão" — stays unbuildable, because a manual button marks a boundary without telling this core it was crossed. |
| **Roubo de Mana / de Determinação** | Only Roubo de Vida exists (`LifeStealService`). |
| **Terreno difícil** | `TerrainType` describes a whole Scene, not a per-movement cost to ignore. |
| **Item numeric columns** | PE has no economy and Conjuração no item-granted hook on either `SpellCastingService` roll. Dureza is off this list: it is a real, consumed pool now — only *repair* is still absent. So is a Pedra do Poder's Cargas/Resfriamento/Danos de Vinculação/Duração do Efeito (`PowerStoneQuality`): exact authored data, but no activation service and no forge/bind step consume it. |
| **Acquisition-time choice on a Talento — *built*** | A Talento whose rules say "escolha uma Perícia / um tipo de terreno / um tipo de arma" records the pick as a hand-written `AbstractFeat` subclass carrying a `@NonNull` choice field (`FocoEmPericiaFeat`, `TerrenoPrediletoFeat`, `EspecialistaEmArmaFeat`, `AtiradorPerfeitoFeat`, `AdotadoPorSylphFeat`, `AcertoCriticoAprimoradoFeat`) — the same catalog-vs-acquired split `ArtesAprimorarComArteAbility` keeps, overriding `Feat#catalogEntry()` so `isEligible`/`FeatCatalog#availableFor` still see it as the enum constant, and a static `chosenBy(Character)` for dependents. Weapon-type choices are the `AttackMethod` enum, matched against the delivered `AttackSource` (`Feat#resolveSkillRollBonus`/`resolveCriticalMarginIncrease` grew a trailing-`AttackSource` cascading overload for it). **Still not covered**: a choice of Atributo/Ego/Árvore de Magia (blocked on *granting* an ability slot, below, or Talento-can't-grant-Atributo), a "pick N Formas/Armas Naturais" (no form state, no weapon catalog), and `FeatRequirements` has no hook for "a held Talento's own choice must be/not be X". See the `adding-a-feat` skill. |
| **Acquisition-slot grants** | "Grants an extra acquisition slot" traits (`Elfo`' Origem Mística, `Anao`' Pequenos Gigantes) have no shape. This is *granting* a free ability/Especialização/Talento slot — distinct from *recording an acquisition-time pick*, above. |
| **Fractional Talento costs** | `getNewFeatCost` returns `int`, so a 2.5-XP discount can't be represented (`Gigantes`' whole-number 2 can). |

## Where the detail lives — subsystem skills

CLAUDE.md keeps only what every session needs: the orientation above, **Recurring conventions**,
the **Missing systems gap catalog**, the two sections below, and this index. Every subsystem
deep-dive is now a Claude Code skill — invoke the one that matches what you're touching; each
carries the architectural rationale that used to be inline here plus a reference-file list.

| Skill | Covers |
| --- | --- |
| `adding-a-pericia` | A whole new Perícia — `Skill`/`SkillType`/`<Skill>Specialization`/`<Skill>CompetencyAbility`/`<Skill>Interaction`/`<Skill>Excellency`, fixtures, tests. Reference: `attention.Attention` / `artes.Artes`. |
| `skill-roll-mechanics` | The shared roll machinery — `AbstractSkillInteraction#applyTo` cascading overloads, `SkillRoll`/`DifficultyLevel`, `SkillTrait`/`requestedAbility` validation, `SkillInteractionFactory`/`SkillRollRequest`, the per-skill `ModifierType`, and "Vantagem is a flat +2". |
| `aggregated-character-stats` | Fixed-base-plus-ability-sum counters — Reações, Ações Livres, Pontos de Ação, Iniciativa, Movimento Base; the per-Round `CombatantSheet` overloads and `ActionProfile`. |
| `attribute-graduation-progression` | Raising an Attribute `base` / Perícia Graduação and spending XP — `CharacterAttributeService#upgradeBase`, `SkillGraduationService#upgradeGraduation`, the caps and cost formulas. |
| `scene-context-and-positioning` | `org.aventyrs.core.scene` — `Scene` sub-groups / turn order, `SceneContext` snapshot, `Range`, mid-Scene initiative changes, `Character`/`CharacterSheet` identity. |
| `magic-system` | Magias, Árvores de Magia, the three acquisition gates, the 145-Magia catalog, a Magia's reach, and the two-roll casting flow. |
| `ego-point-pools` | The two spendable Ego point pools per `EgoDomain` — `EgoPointPool`, `spendEgoPoints`, `useEgoPointsForEffect`, session recovery, `TemporaryEgoPenalty`. |
| `adding-an-ego-advantage` | A Vantagem de Ego — the `EgoAdvantage` hook selection, creation gating, acquisition-time-choice pattern. |
| `damage-and-combat` | `AttackDelivery`/`AttackReceiver`, `DamageBase`/`DamageBaseService` (the odometer scale), `AttackRangeService` (an attack's max distance from the Weapon/Spell), `DamageService` mitigation, `HitPointsService#getStatus`/`CharacterStatus`, ally-facing RA scans, `CombatantSheet` vs `CharacterSheet` vs `MonsterSheet`. |
| `ability-acquisition-and-substitution` | Acquisition-time ability choices (the three patterns) and Perícia base-Attribute substitution (`getSubstituteAttributeDomain`, `AttackSource`, the 5-arg `applyTo`). |
| `adding-a-title` / `adding-a-title-specialization` | A Título Aventyr and its Especializações — `org.aventyrs.core.title`. |
| `adding-a-feat` / `testing-a-feat` | A Talento — `org.aventyrs.core.feat`, the sealed catalog, the four `resolve*` hooks. |
| `adding-a-race` | A Raça — `org.aventyrs.core.race`, the three Race shapes, the clause-triage table, `*RacialAbility` selection. |
| `building-a-foe` | A monster — `AbstractMonsterTemplate` / `GenericMonster` / `SummonedMonsterTemplate`, the four authored numbers, `CriticalEffect` immunity. |
| `adding-an-item` | A piece of Equipamento — `org.aventyrs.core.item` (see also the section below, still held inline). |
| `granting-a-blessing` | Any temporary, trigger-based bonus to the holder and/or allies — `Blessing`/`TemporaryBonus`/`TargetScope`, `CharacterSheet#grantTemporaryBonus`, initiative-win grants. |

**When a subsystem's architecture changes, update its skill — not this table** (unless the
skill's trigger surface changed). Cross-skill references use the skill name in backticks.

## Itens/Equipamento — `org.aventyrs.core.item`

An `Item` is the **catalog entry** for a piece of Equipamento — what "an Armadura Completa" is,
the same way `Feat` describes a Talento — carrying every column an item's rules-text block
lists (`ItemWeightClass`/`ItemRarity`, `description`, `price` in PE, `physicalDefenseBonus`/
`magicDefenseBonus`, `hardness`, `castingBonus`) plus an `ItemFavor` for its conditional half.
**Use the `adding-an-item` skill** to add one — it carries the column-to-field mapping, the
`ItemFavor`/`ItemBonus`/`ItemRequirements` shapes, the one-enum-per-`ItemCategory` layout, the
`Skill.DISADVANTAGE_MALUS` convention, and the test checklist. The architecture:

- **Catalog vs owned copy** — the split `AventyrTitle`'s javadoc documents. The **catalog** side
  is `ItemTemplate` (the enum constants: `ArmorItem` etc.); the **owned copy** side is `Item`
  proper, built via `AbstractItem`/`AbstractWeapon`, carrying its own per-copy state: Dureza
  remaining (`applyDamage`), a fitted Obra-Prima/Aprimoramento (`DefensiveMasterpiece`/
  `DefensiveImprovement` wrapped by `ItemMasterpiece`/`ItemImprovement`), a socketed **Pedra do
  Poder** (`PowerStone`), and a Regalia's `ItemActiveAbility`. Still unmodeled: who produced it,
  the offensive Obra-Prima/Aprimoramento catalogs, and any Pedra do Poder charge/bind economy —
  several TODOs cite these (`ProfissaoCompetencyAbility`, `ResourcesAdvantage#HERANCA_FAMILIAR`).
  **Inventory is real** — `Character#equipment` (worn/wielded, scanned by every
  `resolveEnhancement*` consumer) and `AbstractCombatantSheet#inventory` (carried), both mutable
  `List<Item>`, the same shape as `Character#feats`.
- **Pedra do Poder** (`PowerStoneType` × `PowerStoneQuality`, + optional `PowerStoneMasterpiece`/
  `PowerStoneImprovement`) — a per-copy socketed buff with a **tri-modal** effect: an always-on
  Efeito Base plus one of an Efeito Defensivo/Ofensivo, selected by the host's `Item#getType()`.
  Its passive `ItemBonus`-typed effects fold into the Masterpiece/Improvement aggregation via
  `Item#resolvePowerStoneBonus`; `AbstractItem#setPowerStone` gates the socket on
  `DefensiveImprovement.ENCAIXE` (armor/shield only until an offensive Encaixe exists). Most mode
  effects are catalog-only, TODO'd on the same gaps the racial-feat catalog cites (no elemental
  resistance, no first-instance damage tracking, no attribute-from-equipment hook, …).
- **`ItemFavor` is the conditional half, and its bonuses are real data, not prose**: it carries
  a list of `ItemBonus` (a `ModifierType` + value pair), resolved via `ItemFavor#resolveBonus
  (ModifierType, Character)` / `Item#resolveFavorBonus(...)` — 0 unless the `ItemRequirements`
  (an `AttributeDomain` + value) are met. It's **data, not `@Modifier` methods**, unlike every
  ability enum, and that's forced: `@Modifier`'s `ModifierType` is a compile-time-fixed
  annotation value, so one shared `ItemFavor` class can't vary which type a given item grants —
  the same limitation "A ModifierType per skill" documents. **Don't route items through
  `ModifierResolver`.** `ItemBonus` is deliberately not `TemporaryBonus`/`Blessing` either: an
  item's Favor lasts as long as the item is carried and never reaches anyone but its wielder, so
  a countdown, a `TargetScope` and a granting `source` would all be dead weight.
- **`ItemRequirements` checks `getTotal()`, not `getBase()`** — deliberately unlike
  `FeatRequirements`, which uses `base`: acquiring a Talento is gated on what the character
  personally invested in, but whether an item's Favor applies is a "can I meet this right now"
  question, so a Bônus Racial or a variable bonus counts. It's a narrower record than
  `FeatRequirements` (no `requiredSkillType`/`requiredFeat`) rather than a reuse of it — widen
  it only if a real item ever names a Perícia/Talento/Título.
- **Two columns reach a real consumer, three don't.** The Favor's `DAMAGE_REDUCTION` is scanned
  by `DamageServiceImpl` over `character.getEquipment()`, and DF/DM by
  `DefenseServiceImpl.sumEquipment` — a new item's values flow into both with no wiring. Dureza is a real pool too, spent by
  `Item#applyDamage` (below). **Preço and Conjuração still have no consumer**, each blocked on a
  different missing system (no PE economy, no item-granted hook on either `SpellCastingService`
  roll). Their values are real, exact data all the same, per the "can't apply it yet doesn't mean
  can't compute it yet" discipline.
- **Dano Base is on `Weapon`, not on `Item`** — `Weapon extends Item` adds exactly two abstract
  columns, `getDamageBase()` and `getSkillType()` (the Perícia it's swung with, which is what
  `DamageBaseService` scans by), and `AbstractWeapon extends AbstractItem` is its builder-built form
  (both use `@SuperBuilder` so the subclass inherits the ten `AbstractItem` columns rather than
  restating them; `AbstractItem.builder()` is unaffected). Every other weapon property — Preço,
  Dureza, Raridade, `ItemFavor`, even DF/DM — is an ordinary `Item` one and needs no override.
  **Don't put a weapon-only column on `Item` with a harmless-looking default**: that's what this
  interface replaced, and a defaulted `UNARMED` made "a helmet" and "a real dagger" answer
  identically. `DamageBaseService` takes a `Weapon`, so the compiler refuses a pauldron — no
  `isWeapon()` flag and no runtime guard, the same enforcement-by-type as `CharacterSheet` vs
  `MonsterSheet`. Nothing checks that a `Weapon`'s `ItemCategory` is actually `OFFENSIVE`, per the
  usual builders-aren't-gatekeepers restraint.
- A Favor clause with no `ModifierType` to express it contributes no `ItemBonus` and lives on in
  `getDescription()` until its mechanism exists — either because no reader for the concept
  exists (`ARMADURA_COMPLETA`'s "de Corte" scoping, modeled as plain RD) or because `ItemBonus`
  can't hold the *shape* even though the stat has a `ModifierType` (`ARMADURA_DE_JUSTA`'s
  halving; **don't add a `MOVEMENT_HALVED` constant** — the missing piece is the multiplicative
  mechanism, not a reader). But check the *net effect* before assuming a split is needed:
  `ROUPA_PESADA`'s two clauses read like they need DF and DM separately, yet always net out to
  an unconditional +1 to both, so it's granted for real as one combined `DEFESAS` bonus of 2.
- `ItemInteraction` is untouched by this — still the bare pre-existing "TODO implement" stub,
  since nothing yet *uses* an item as an `Interaction`.

### Damage to an item, and what a destroyed one stops granting

An `Item` copy has PV of its own. `getEffectiveHardness()` is its maximum (authored Dureza plus
its Obra-Prima/Aprimoramento adjustments), `getDamageTaken()` the per-copy damage it carries,
`getCurrentHardness()` what's left, and `applyDamage(int)` the entry point — returning what
actually landed.

- **An item is mitigated by its own enhancements only.** `getItemDamageReduction()` sums
  `Improvement#getItemDamageReduction`/`Masterpiece#getItemDamageReduction`
  (`DefensiveImprovement.RESISTENTE`'s -1 is the reference) and nothing else: its wielder's
  RD/RA belong to a different victim. That's why the whole calculation lives on `Item` rather
  than in `DamageService` — no three-source scan is involved, so it needs no `ModifierResolver`
  and no `Character`, the same reasoning that keeps a permanent Ego max on the sheet. Don't
  confuse it with `Improvement#resolveDamageReduction(DamageDescriptor, Character)`, which is
  the RD the *wearer* gets.
- **`isDestroyed()` is `getDamageTaken() > 0 && getCurrentHardness() == 0`** — an undamaged item
  is never destroyed, including one whose Dureza is 0. **Every bonus-granting default on `Item`
  is gated on it in one place** (Defesas, Favor, enhancement bonuses, enhancement RD, Dano Base
  scale-ups, Duração extension, the improvement-effect window), so no consuming service carries a
  check of its own — and a new one is wired correctly by going through those defaults. That is
  what promoted `resolveEnhancementDurationIncreaseInRounds` onto `Item`: `SpellDurationService`
  used to reach `getImprovement()` directly and would have kept extending a Duração off a wreck.
  `Weapon#getEffectiveDamageBase()` is the same idea for a swing — `UNARMED` once destroyed,
  while `getDamageBase()` stays the authored column.
- **Three things are deliberately not gated**: `getEffectiveHardness()` (the ceiling destruction
  is derived from), `getEffectiveWeightClass()` (a ruined breastplate is exactly as heavy — the
  Destreza penalty `EsquivaEApararInteraction` reads is a burden, not a benefit), and the
  identity columns, which is what lets a player find the wreck to drop it. **A destroyed item
  stays in `Character#equipment`/`CombatantSheet#inventory` as garbage** until its owner removes
  it; destruction removes effects, not the object.
- **An `ItemTemplate` takes no damage and returns 0** — damaging a shared catalog entry would
  break every copy of that Equipamento at once. Same silent no-op templates already give
  `activateImprovementEffect`. Damage on a real copy accumulates *past* 0 rather than clamping,
  so fitting an enhancement that raises the maximum can't undestroy a wreck.
- **Nothing calls `applyDamage` automatically yet, and there is no repair.** Every clause that
  deals item damage — `StrengthAbility#ESTILHACADOR`, `DuelistaFeat#DEFENDER_SE_ATACANDO`, the
  Estilhaçador/Sabotar/Repelir e Suprimir/Retorno de Danos Efeitos Críticos, Bola de Fogo
  Elduriana's "Objetos em posse dos personagens afetados sofrem metade deste dano" — is blocked
  on a further system of its own, and `ProfissaoCompetencyAbility#REPARO_MELHORADO`/the Artesão
  tree need a repair pipeline (`FORTALECER` additionally needs Scene-scoped damage tracking).
  Real and tested with no automatic caller, like `MoralHerdadaAbility#applyStartingFama`.

## Consumer-facing documentation lives in `package-info.java`, and must stay current

This is a library other Java code (an API layer, or a game UI) imports directly — so
"how do I use this" docs go in `package-info.java` files next to the relevant package, not a
separate markdown guide that can silently drift out of sync. Two exist so far:

- `org.aventyrs.core.skill` — how to perform a Skill Roll via `Interaction`/`CharacterSheet`.
- `org.aventyrs.core.character.services` — the full ordered list of character-creation
  choices (Race → Attributes → Egos → conditional Vantagem de Autocontrole → ActionProfile →
  assemble via `Character.builder()`).

**Whenever a change adds a new creation-time choice** (a new Ego/Attribute-like allocation,
another permanent "pick one" enum like `ActionProfile`, a new conditional Vantagem like
Autocontrole's) **update the `character.services` package-info's numbered list and code
example in the same change** — don't leave it for later. The same applies to the `skill`
package-info if the Skill Roll protocol itself changes (new `InteractionResult` fields, a
different dispatch shape, etc.). A consumer coding against a stale list will silently miss
required or newly-available choices.

Before publishing a version other people can `javadoc`, note that `./gradlew javadoc`
currently fails on ~5 pre-existing errors — Lombok's generated `Builder` inner classes aren't
visible to the standalone javadoc tool without a delombok step. This is unrelated to the
package-info content itself (verified by running `javadoc` directly against just the relevant
sources); fixing it would need a delombok task (e.g. the `io.freefair.lombok` plugin).
