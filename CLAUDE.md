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
  (`CombatantSheet` → `+SceneContext` → `+SkillRoll` → `+attackTarget` → `+AttackSource` →
  `+additionalTargets`, the last holding all the logic — and note `attackTarget` stays the
  *primary* target rather than being widened into a list, since widening it would have made
  `applyTo(t, ctx, roll, null, null)` ambiguous), `DamageService`, `DamageInteraction`,
  `SceneContext`, and `Scene#addParticipant`. Two deliberate non-cascades to know: `ActionPointsService`'s `Character`/
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
  prerequisites. **And a prerequisite is not a condition of use**: `FeatRequirements`/`isEligible`
  answer "may they learn it", checked once at `grantFeat`; something that must hold *each time the
  trait is exercised* goes in a hook returning "not permitted right now" instead
  (`Feat#itsAllowedToCraftRegalia` is the reference — see the Itens section; `FeralFeat`'s "não
  pode ser **usado** em conjunto" is deliberately *not* a `forbiddenFeat` for the same reason).
  **A `Feat` prerequisite is a full boolean now, not just a set of floors**: `FeatRequirements`
  carries ceilings (`maximumAttributeValue`/`maximumEgoValue`), negations (`forbiddenRace`/
  `forbiddenFeats`), domain-less thresholds (`requiredAnyAttributeValue`) and a real disjunction
  (`anyOf`, nested groups of which one must hold *on top of* the outer clauses). Two clauses need
  the `CharacterSheet` (`requiredFame`/`requiredTotalExperience`), so `isEligible(Character,
  CharacterSheet)` is the authoritative form and the one to override — the `Character`-only
  overload delegates to it with `null` and **skips** those two rather than failing them, keeping
  `FeatCatalog#availableFor(character)` a superset of `availableFor(character, sheet)`: looser,
  never stricter. A mutually-exclusive pair makes each half name its twin, which forces that enum
  onto `Supplier<FeatRequirements>` *and* a `private static` accessor for the forward half — a
  `Supplier` defers evaluation, but only a method body lifts Java's forward-reference rule. See
  the `adding-a-feat` skill and `docs/rules/talentos-index.md` for what a Pré-requisito still
  cannot say.
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
  never `getEquipment()`. **No per-hand tracking of drawn weapons**: `drawnWeapons` is a flat
  list, and every Talento reading it asks only "is anything drawn", never *which* hand.
  `CharacterFixture` defaults it to an immutable `List.of()`, same caveat as `feats`/`equipment`.
  **A two-hand *budget* does exist for the equipped loadout**, though — `CharacterSheet#equip`/
  `canEquip`/`validateEquipmentLoadout` enforce it (1 Armadura/Elmo/Botas/Capa/Manoplas each, ≤1
  Escudo, and Escudos + armas within 2 hands, where a `MEDIUM`/`HEAVY` weapon or any `BOW`/
  `CROSSBOW` takes both). Handedness is **inferred** from `ItemWeightClass` + category, not
  authored — `equipamentos.txt` still gives no weapon a hands column. The plain `Character#equip`
  mutator stays unvalidating and builder-bypassable; `CombatantSheet#rearm` and a foe's loadout
  don't go through the check.
- **A critical hit grants Vantagem em Danos — a flat +2, and *no* extra die.** It is an ordinary
  `Skill#ADVANTAGE_BONUS` on the dano roll, the same +2 Vantagem is worth everywhere else, so a
  crit is a `DamageBonus` contributor and not a stage of its own. **The "+1d6" belongs to specific
  traits, never to the baseline**, and they divide into two kinds: one *replaces* the Vantagem
  (`AssassinoFeat#VIOLENCIA_DESCOMUNAL` — "Você não recebe Vantagem em Danos em seus Acertos
  Críticos, ao invés disso recebe Bônus de +1d6", which is the clause that proves what the
  baseline is), and the rest *add* to it (Mira Mortal, the two Bruto Título clauses at
  `talentos.txt:295`/`299`, Rancor Ymiriano). Nothing here is modelled — neither the baseline
  Vantagem nor any grant — so a crit currently changes the dano roll not at all.
  **Do not describe crit damage as "an extra die plus +2"**; that conflated the Talento's
  replacement die with the base rule, and this bullet said so until it was checked against
  `talentos.txt`.
  ⚠️ **Unverified:** whether a granted "+1d6" is subject to `DamageBase.MAX_DICE` (at 3 dice
  becoming +2 instead). That claim arrived attached to the wrong crit rule above and may be
  downstream of the same error — a bonus die is not a Dano Base die, and the cap governs the
  Dano Base scale. Confirm against the core rulebook before relying on it.
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
- **An Ataque Corpo-a-Corpo adds half its attacker's Força to the dano roll**, floored, untyped —
  `AbstractSkillInteraction#resolveMeleeStrengthDamage`, summed into `DamageBonus#total`'s
  flatModifier alongside the four trait scans. It is a property of the *Perícia*, not of anything
  held, so it hangs off no `resolve*` hook; `StrengthAbility#DESTRUIDOR_DE_MUROS` upgrades the
  Rodada's **first attack** (any attack, not the first melee one) to the full value via
  `AttributeAbility#upgradesFirstMeleeAttackOfRoundStrengthScaling`, the exact mirror of
  `upgradesFirstSpellOfRoundFocusScaling`. **Melee only** — Ataque à Distância adds nothing, which
  is why the Arco Composto has a Favor granting the term (unauthored: `ItemBonus` can't express a
  derived half-Atributo, and the offensive weapon catalog doesn't exist). **A dano bonus, never a
  Dano Base scale-up** — the rules' own `1d6+4 (Base 2 + Metade da Força)` notation keeps the two
  apart. It stays **Força** under an `ACUIDADE`/`SAGACIDADE_ARCANA` substitution: that hook
  replaces the Atributo governing the *roll*, while this term names Força specifically. It reads
  `Character#getEffectiveAttributeTotal(STRENGTH)` — so a *permanent* `Feat#resolveAttributeBonus`
  Força grant reaches it, but a *round-scoped* `STRENGTH_BONUS` `TemporaryBonus` still does not
  (that is read only on a Força-governed Perícia roll, and a dano roll is not one).
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
| **Owned/produced item copy** | The `Item` *catalog* is real, and so is inventory now — `Character#equipment` (worn/wielded, scanned by `DefenseService`/`DamageService`) and `AbstractCombatantSheet#inventory` (carried, including a foe's loot). Per-copy state built: **Dureza remaining** (`Item#applyDamage`/`getCurrentHardness`/`isDestroyed`, mitigated by the item's own enhancements only — see "Damage to an item…"), a fitted **Obra-Prima** and up to 1/2/3 **Aprimoramentos** by `ItemWeightClass` (`AbstractItem#improvements` is a `List<Improvement>`; `getImprovement()` is a deprecated first-or-null shim), a socketed **Pedra do Poder** (gated on `DefensiveImprovement.ENCAIXE` via `AbstractItem#setPowerStone`), and **who produced it** (`AbstractItem#producedByCharacterId`, stamped by `EquipmentCraftingService#forge`). **Fabricação/reparo is built** — `EquipmentCraftingService` (+`Impl`; the forge itself is `item.ItemForgery`, working from an `ItemSpecification` and consuming `Feat#itsAllowedToCraftRegalia` — plus `ItemForgery#donatedByAventyr`, the gate-free GM path marking `Item#isDonatedByAventyr()`) prices the work in PE, times it in days/hours, resolves the GD from `ItemRarity` (`getFabricationDifficulty`/`getRepairDifficulty`/`getMasterpieceRepairDifficulty`/`getImprovementInstallDifficulty`/`getMinimumMasterpieceGraduation`), gates it (trade Especialização, Obra-Prima Graduação floor, weight-class Aprimoramento cap), and — caller having already resolved the Perícia roll — `forge`/`repair`/`installImprovement` mutate; `Item#repair(int)` is the raw mutator beneath, the mirror of `applyDamage`. `ProfissaoCompetencyAbility#CONSTRUTOR_EFICIENTE`/`AUMENTAR_A_DUREZA`/`REPARO_MELHORADO` are real through it (`resolveProductionTimeMultiplier`/`resolveProducedHardnessMultiplier`/`resolveRepairHardnessBonus` on `SkillCompetencyAbility`). **Regalia crafting is built** — `EquipmentCraftingService#forgeRegalia(crafter, trade, base, RegaliaGrade, RegaliaDonation)` is what makes the whole `ArtificeFeat` tree real: `RegaliaGrade` (MENOR/SUPERIOR/DIVINA — **a per-copy `Item#getRegaliaGrade()` property, never an `ItemRarity`**) carries GD (Inimaginável/Milagre) and days (90/145/180); the forge gates on the trade Especialização, the matching `ArtificeFeat` (`requiredToForge`), a willing Centelha `RegaliaDonation` and — for Divina — a `CreatureType.DRAGAO`/`ELEMENTAL`/`ABISSAL`/`CELESTIAL` donor; on success it marks the copy, stamps `producedByCharacterId`, and advances `Character#recordRegaliaCrafted`/`getRegaliasCrafted` (the "criação de 3 ou mais Regalias" history `FeatRequirements#craftedRegaliaGrade`/`requiredRegaliaInPossession` gate on). See `docs/rules/fabricacao-e-reparo.txt`. **Buying from a shop is built** — `ItemForgery#purchased(ItemSpecification)` (the third factory beside `by`/`donatedByAventyr`: no crafter, no crafter-gates, refuses a Regalia, copy has no maker and is *not* an Aventyr donation) + `ItemPurchaseService`, spending `AbstractCombatantSheet#equipmentPoints`; `ItemStore` (held by a `Scene`, a `maxRarity` ceiling over `ItemCatalog`) is the offering — `getOfferedItems()` plus `getOfferedMasterpieces()`/`getOfferedImprovements()` (the authored `DefensiveMasterpiece`/`DefensiveImprovement` catalogs, same ceiling), and `offers(ItemSpecification)` applies that ceiling to a copy's Obra-Prima and every Aprimoramento, not just its base. Still missing: a **PE economy for *production*** (a forge's cost is reported, not spent), an Aprimoramento's install **time** (the days/hours figures read a bare `ItemTemplate#getPrice()`, never a specification's total, so a fitted Aprimoramento's Preço reaches the cost but not the schedule), the offensive Obra-Prima/Aprimoramento catalogs, `FORJA_VULCANA`'s benefits (Resistência a Críticos / Margem Crítica Maior / item-scoped choices — none exist), `REPARO_MELHORADO`'s "estende a Magias e Habilidades" clause (no Dureza on either), the Pedra do Poder **charge/Resfriamento/Vinculação economy**, and — for Regalias — the Centelha *loss* (nothing tracks a character's Centelhas), the *Forja do Olho de Deus* location (no places), and the Divina mandatory-Acerto-Crítico (reported, not enforced — this core never rolls). Cite the specific piece. |
| **Classifying an attack as Desarmado/Arma Natural — *closed off the roll path*** | The Arma Natural marker is real: `ItemCategory.NATURAL_WEAPON` is the column, and **`Character#treatsAsNaturalWeapon(Weapon)` is the single view every clause consults** — never the raw category, because `Feat#reclassifiesAsNaturalWeapon` lets a Talento make an ordinary weapon count as natural *for its holder* (`ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_FERROADA_ESMAGADORA`), and its rules text requires that to be visible to every other Arma-Natural clause (`ARTISTA_MARCIAL`, `DEFESA_DE_MAOS_LIMPAS`, `MonstruosoFeat`, `DefensiveImprovement#BENCAO_SELVAGEM` all route through it). Reclassification is additive and one-directional — nothing can make a real Arma Natural stop counting. Desarmado is unambiguous **on the Dano Base path only**, where `DamageBaseService`'s two overloads make `weapon == null` mean it. **Still missing:** (a) on the **Perícia-roll path** an Ataque Desarmado has no `AttackSource`, so `AbstractSkillInteraction#applyTo`'s 5th parameter is `null` for both "unarmed" and "caller didn't say" — every consumer of that distinction is blocked on something else too (a dano-roll malus, condition suppression, retaliation damage), so don't add the marker without a real consumer; (b) **the Arma Natural catalog is authored** — `NaturalWeapon` (implements `ItemTemplate`+`Weapon`, one constant per `docs/rules/equipamentos.txt` "Armas Naturais" row: Arma de Sopro, Cauda Chicote/Constritora, Chifres Poderosos, Garras Afiadas, Presas Longas, Ataque Desarmado), and a Talento *grants* one through `Feat#getGrantedNaturalWeapons` → `Character#getNaturalWeapons()` (`ArmamentoDraconicoFeat` for the pick-two `DraconicoFeat#ARMAMENTO_DRACONICO`, `DraconicoFeat#SOPRO_DE_DRAGAO`, `BestialFeat`'s Bovídea/Canina/Felina). A **race** grants them too, through `Race#getGrantedNaturalWeapons()` — `Vampiro`'s per-`VampiroLineage` Presas Longas/Garras Afiadas — folded into the same `Character#getNaturalWeapons()` view. Still missing here: `NascidoDoDragao`/`Feral`/`Monstruoso`/`HomemFera` name Armas Naturais too but are blocked on a form state or per-sub-race authoring; an **offensive Efeito Crítico column** on `Weapon` (no weapon→crit scan exists — the "Empalar (17)" etc. are javadoc-only); each weapon's own **Favor** (none has a `ModifierType` — Roubo de Vida, Margem Crítica, metade do Vigor ao Dano); and the **elemental damage type** the Sopro deals. |
| **Damage-type-scoped mitigation, and damage-type immunity — *RM is built*** | `DamageType` has no Corte/Perfuração/Impacto breakdown (nor Profano/Natural/Esmagamento). **RM (Resistência à Magias) is real** — `ModifierType.MAGIC_REDUCTION` + `DamageService#getTotalMagicReduction` (same five sources as RD), added by `calculateFinalDamage` **only** when the caller typed the hit `DamageType.MAGICO`; `GorgonaFeat#PROTECAO_DA_RAINHA_DAS_FADAS` and `DefensiveMasterpiece#DYOSPIROS`/`MITRAL` are the consumers. What is still missing: **RD itself is type-blind**, applying whatever the type although the rules scope it to Dano Físico não-PRIMORDIAL e não-ELEMENTAL — so a MAGICO hit currently takes RD *and* RM. RE (Resistência Elemental) has no constant (every authored RE clause is scoped to one `ElementalType`, which is the `DamageDescriptor` path equipment already uses). `AttributeAbility#resolveDamageReduction` remains the one type-aware RD hook, unreachable from a `SkillCompetencyAbility`. *Nullifying* a damage type outright is a further missing stage: there is no immunity mechanism of any kind. Cited by `Zumbi` (imune a Profanos/Naturais, -3 vs Esmagamento). |
| **Resistência a Críticos (RC) — *built*** | Closed for a character's own RC. `CombatantSheet#getTotalCriticalResistance(SceneContext)` sums every source — the holder's Raça (`Race#getCriticalResistance()`), each held Talento (`Feat#resolveCriticalResistance`) and any round-scoped `ModifierType.CRITICAL_RESISTANCE` `TemporaryBonus` — and `AbstractSkillInteraction` subtracts the *attack target's* figure from the attacker's summed Margem Crítica Menor widening (so it also covers `AttackDelivery`, not the `AttackReceiver` mirror). One instance = `CombatantSheet.CRITICAL_RESISTANCE_INSTANCE` (2), per `docs/rules/defesas-e-resistencias.txt`, and a numberless clause grants exactly one. Consumers: `AnaoFeat#VIGOR_DO_INVERNO` (timed), `MonstruosoFeat#ANATOMIA_INCOMUM`, `ElementalFeat#TRANSFORMACAO_ELEMENTAL`, `DuelistaFeat#CORACAO_DE_FERRO`, `SobrevivenciaFeat#PROTETOR_TERRITORIALISTA` (terrain-gated), `Troll`. **Never read `getTemporaryBonus(CRITICAL_RESISTANCE)` alone** — that sees only the timed half. The `SceneContext` the scan receives is the **attacker's** snapshot (the only one in reach — `SceneContext` holds no `Scene`), so an override may read Scene-wide facts (terrain, round, isCombatScene) and must never read proximity. **Still missing:** the "-1 à Margem Crítica Maior" clause (this ruleset models no Acerto Crítico Maior margin at all); "não-PRIMORDIAL" scoping; RC only cancels attacker widening, never pushing a crit below baseline (the "até o mínimo de 17" floor is approximated); an **item-scoped** RC a produced/worn Equipamento passes to its wearer (`ProfissaoCompetencyAbility#FORJA_VULCANA`, `DefensiveMasterpiece#MITRAL`); and the two `GorgonaFeat` grants, still form-gated. No `SkillCompetencyAbility`/`AttributeAbility` hook — no constant asks for one. Not the same as `Race#getCriticalEffectImmunities()` (an all-or-nothing `CriticalEffectType` filter). |
| **Multiplicative stages** | `MovementService` sums `MOVEMENT` additively with no halving stage (unlike `DamageService`'s real `HALF_DAMAGE`). Don't add a `MOVEMENT_HALVED` constant — the mechanism is missing, not just a reader. |
| **Movimento Base — *the per-movement axis is built*** | `MovementService#getMovementBase(CombatantSheet[, int movementIndex])` resolves `getTemporaryBonus(MOVEMENT)` **and** `resolveRoundMovementIncrease` across `AttributeAbility`/`SkillCompetencyAbility`/`Feat`/equipped `Item`, indexed by `CombatantSheet#consumeMovementThisRound()`. That closes "which movement of the Rodada is this" (`DexterityAbility#PASSOS_LONGOS`, `MobilidadeFeat#VELOCISTA`). Still missing, and what a movement TODO should cite instead: a movement's **distance** and **direction** are not recorded, and the counter resets at `startTurn`, so nothing knows a character moved in a *previous* Turn (unlike the roll-action log, which resets at `startNewRound` and `startTurn` only slices — don't conflate the two). Investida/Reposicionar are still unmodelled manoeuvres. |
| **Temporary PA/Reação/Ação Livre grants — *built*** | Closed. The `CombatantSheet`-taking overloads of `ActionPointsService#getMaxActionPoints`/`ReactionsService#getTotalReactions`/`FreeActionsService#getTotalFreeActions` read `getTemporaryBonus(ACTION_POINTS/REACTIONS/FREE_ACTIONS)` for real. The `Character`-only overloads still can't — no sheet to ask — so cite *that* if a caller only holds a `Character`, not "the mechanism is missing". |
| **"Spend a resource to enter a timed state" — *built, Resfriamento included*** | `ActiveAbility` + `ActiveAbilityService#activate` do the whole cycle: validate the ability is held (by reference), check **Resfriamento** and PA/PM/**PV** affordability (`getHitPointCost()`, refused if self-fatal — "Ação Livre" = `getActionPointCost()` 0), spend, apply every `TemporaryEffect` from `resolveEffects(Character)`, and *then* start the Resfriamento — so a refused activation costs nothing and locks nothing out. **Resfriamento** is `ActiveAbility#getCooldownRounds()` (0 = "as often as you can pay") plus a per-sheet ledger (`CombatantSheet#startCooldown`/`getRemainingCooldown`), keyed by *identity* like the held-ability check, and burned down at the **Rodada** boundary (`startNewRound()`) — deliberately not the `TemporaryEffect` countdown, which ticks at Turn *end* and would return a Rodada-measured Resfriamento early for whoever acts late in the order, the same reasoning `scheduleTemporaryEgoPointGrant` follows. A **Talento** contributes an ability via `Feat#resolveActiveAbility()` (must return a stable singleton — `Character#getActiveAbilities()` aggregates `getFeats()` live and `activate` matches by `==`); an `AttributeAbility` still copies its own onto `Character.activeAbilities` at acquisition. Consumers: `VampiricoFeat`'s four Poderes Vampíricos (`PoderVampiricoActiveAbility`), `FocusAbility#CONCENTRACAO_PROFUNDA`, and `MetamagicoFeat#ARCANISTA_EXPERIENTE`'s Barreira Mágica (`BarreiraMagicaActiveAbility` — the catalog's only stated Resfriamento; its two upgrade rungs *replace* the Defesas figure rather than adding to it, so the ability reads the holder's rungs and only the first rung grants it). **A Pedra do Poder's Resfriamento is a different, still-inert thing** (`PowerStoneQuality`, item-side, no activation service) — don't conflate them. **Entering a Forma is one of these transactions** — `ActiveAbility#resolveGrantedForm` names the shape, and `activate` additionally refuses a shape the holder's Talentos forbid, enters it, and applies a `FormEffect` that ends it when the Duração lapses. Two cost/gate pieces were added with it: `getDeterminationPointCost()` (Formas are priced in **PD**) and `getReactivationRest()` → a `RestType`, a Resfriamento measured in **Descansos** rather than Rodadas ("não poderá ser reativado até que passe por um Descanso Longo"), held in its own ledger and cleared by `RestService#applyRest` at that tier *or stronger*. `activate` refuses while **either** cooldown is owing (`CombatantSheet#isOnCooldown`). Still not covered: a per-Rodada effect needing the Scene's neighbours (`TemporaryEffect#applyRoundEffect(CombatantSheet)` gets the holder's sheet alone — `PRESENCA_DE_CARMILLA`); an **ally-facing** activated grant, since `activate` applies every effect to the activator's own sheet and sees no Scene (`MESTRE_ARCANISTA`/`DESAFIADOR_DA_REALIDADE`'s "+1/+3 às Defesas de seus aliados adjacentes"); an activation that **delivers one attack** rather than entering a state (`OrquicoFeat#TREMOR`); a **PD** cost field (`ElementalFeat#GANA_ELEMENTAL`'s "2PD" — not added, since that clause is blocked on two other things anyway); and a **temporary ability** grant, which `TemporaryBonus` cannot carry (`GnomoFeat#MIMETIZAR_COMPETENCIA`'s active half). The flight TODOs in `Aviano`/`DraconicoFeat`/`FeericoFeat` cite the flight *sub-stat* as their blocker, not the activation. |
| **Forma (alternate shape) — *the state, the gate and most deltas are built; two remain*** | `sheet.FormType` is the authored catalog of alternate shapes — ANIMAL/HIBRIDA/MONSTRUOSA/FEERICA/HUMANA/DRACONATO/ANCIENTE plus the six Formas Metamórficas (NEVOA/ARANHA_GIGANTE/CAVALO_DE_CHIFRES/LOBO_DENTES_DE_SABRE/MORCEGO_ATROZ/SERPENTE_ESPINHOSA) — and `CombatantSheet#getCurrentForm()`/`enterForm(FormType)`/`isInForm` hold it. **Of the four deltas this row used to list, two are now built** (equipment restrictions, the Arma Natural swap); the Multiplicador de PV uplift and racial-trait suppression are what is left. **There is no constant for "their own shape"** — that is `null`, so a state with two spellings of "normal" cannot arise; `HomemFera` switching back to Humanoide is leaving the Forma. Named by **shape, not by source**: `MONSTRUOSA` is one constant though three unrelated traits reach it, because "enquanto em sua Forma Monstruosa" asks about the shape. `enterForm` is an **unvalidating mutator** and nothing transforms anybody automatically — a caller's call, exactly like `applyCondition`; `CombatantSheet#canTakeForm` is the separate question, combining every held Talento's `Feat#resolveFormAccess` (`FormAccess.FORBIDDEN` refuses that shape; `REQUIRED` locks the holder in and so refuses every other shape *and* their own — `GorgonaFeat#ACOLHIDA_POR_FLORA` and `#MARCA_DA_MALDICAO` are the two consumers). A clause gated on the Forma reads it through a `CombatantSheet`-taking hook (`Feat#resolveCriticalResistance`'s longest overload — `GorgonaFeat`'s two Proteções). **What a Forma is worth is partly built.** `FormaActiveAbility` grants its uplift as round-scoped `TemporaryBonus`es lasting exactly the Duração — a `SIZE_CATEGORY` shift (real, via the new `CharacterSizeService#getEffectiveSizeCategory(CombatantSheet)` overload; the `Character`-only one structurally cannot see a sheet-held bonus), a `DEFESAS` bonus, and one `<ATTR>_BONUS` per Atributo the text names, each figure multiplied by the holder's Títulos Despertos (`FormaActiveAbility.Uplift`, because Draconato and Ancienteforme disagree on more than one number). **The Atributo half has the documented partial reach**: it lands on a Perícia roll governed by that Atributo and nowhere else, since PV/PM/PD/Conjuração read `Character#getEffectiveAttributeTotal`, which has no sheet — a three-Rodada Forma raising max PV would need those totals recomputed per Rodada, which this core does not do. **Still missing, and what a Forma TODO should cite:** a **Multiplicador de PV** uplift that is in force only while transformed — the figure itself scales per Título Aventyr Desperto, exactly like the Defesas and Categoria halves of the same sentence, and is unbuilt for the sheet-reach reason above rather than any per-Rodada one (`HitPointsService#getLifeMultiplier` takes a `Character` and cannot see the Forma's `TemporaryBonus`; a *permanent* per-Título multiplier is real and ordinary — `OrquicoFeat#TERRA_NAS_VEIAS`), **equipment restrictions** *partly* — `FormType#getEquipmentPolicy()` is a per-shape `FormEquipmentPolicy`, because the rules give two different answers (`WEAPONS_SUPPRESSED` for the Metamorfose Dracúlea shapes, "itens defensivos continuam concedendo seus benefícios"; `ALL_SUPPRESSED` for Metamorfose Selvagem's "é impossível usar equipamentos"). Only the weapons half has a consumer: `CombatantSheet#canAttackWith` refuses a wielded `Weapon` while the Forma suppresses them, exempting an Arma Natural — and **two per-shape exceptions live inside `WEAPONS_SUPPRESSED`**, which is why they are columns on `FormType` rather than new policy constants: `FormType#permitsOneHandedWeapons()` (Lobo Dentes-de-Sabre's "pode empunhar armas de uma mão com as presas", the table's one clawback, using the same `ItemWeightClass`-plus-category handedness inference as `CharacterSheet`'s two-hand budget — read the **authored** `getWeightClass()`, not `getEffectiveWeightClass()`, which throws on an unauthored column and would let the two disagree), and Névoa going the other way. `ANIMAL` is `ALL_SUPPRESSED` (Metamorfose Selvagem's "é impossível usar equipamentos") — ⚠️ which `HomemFera`'s own Animal rung therefore **inherits**, its clause not being in this repo to check against. `DraconicoFeat#ASAS_DE_DRAGAO`'s "impede Equipamentos do tipo Capa" is a *permanent* Talento-level restriction and lives elsewhere — `Feat#getForbiddenEquipmentCategories`, enforced on the equipment list by `CharacterSheet#equip`/`canEquip`/`validateEquipmentLoadout`. **The two are deliberately not merged**: a Forma's policy is temporary, keyed on the shape its holder is in, and answers "can I use this right now"; a forbidden category answers "may this ever be on my body". A Vampiro in wolf shape still *owns* the sword they cannot swing. Still missing: **suppression of racial traits** ("abandonando seus traços raciais" — `Race#getRacialAbilities()` is read live with no way to suspend it; the phrase occurs in exactly two Talentos, `DraconicoFeat#DRACONATO` and `FeericoFeat#ANCIENTEFORME`, and in neither is it built). **The per-Forma Arma Natural swap is built** — `Feat#getGrantedNaturalWeapons(Character, CombatantSheet)` lets a shape contribute its own (`FormaMetamorfica`'s ARMA NATURAL column) and `Feat#replacesNaturalWeaponsWhileInForm` lets it **replace** the holder's, both aggregated by **`CombatantSheet#getNaturalWeapons()`** — the sheet-aware twin of `Character#getNaturalWeapons()`, which stays deliberately Forma-blind (it is what a Character *is*, not what a combatant currently looks like) and is the one to read when no sheet is in hand. Two hooks rather than a sentinel empty list, because "grants none" and "grants none **and** cancels everyone else's" are different answers and Névoa needs the second; its empty list is what makes `canAttackWith` refuse every Arma Natural, closing half of "é incapaz de causar danos" (the Magia half and `canAttackWith(null)` — a punch is not a weapon — stay open). **Read the replacement as this Talento's own clause, not as racial-trait suppression arriving early**: it cancels Armas Naturais only, only while the shape is worn, and it is a *reading* — the source's "armas não podem ser utilizadas, são substituídas por armas naturais" takes **armas** as the subject of "são substituídas", so the text arguably only says weapons are replaced; `FormaMetamorfica`'s javadoc carries that argument so a later reader sees a decision rather than a slip. **Nothing is written on transforming**, so all three exits (a `FormEffect` lapsing, `enterForm(null)`, a second Forma displacing the first) need no undo — which is the whole reason it is derived rather than swapped. **Entering one is built** — see the timed-state row: `FormaActiveAbility` is the shared form for the Talentos written to the "3PA + 3PD, 3 Rodadas, not again until a Descanso Longo" template (`DraconicoFeat#DRACONATO`, `FeericoFeat#ANCIENTEFORME`), and a `FormEffect` returns its holder to their own shape as the Duração lapses. **That expiry is the one place leaving a Forma is not a caller's call**; one entered through the bare `enterForm` mutator is still the caller's to undo. **A Forma may also be open-ended** — `ActiveAbility#resolveDurationInRounds()` returns `null` for a shape its holder stays in until they leave it, carried by `FormEffect`'s own nullable `remainingRounds`; `BestialFeat#METAMORFOSE_SELVAGEM` is the case, its clause stating no Duração at all unlike every other transformation. **A bonus lasting "while transformed" should be resolved *from* the Forma, never scheduled as a `TemporaryBonus` beside it** — a countdown would expire while its holder was still in the shape. That is why `Feat#resolveMovementIncrease` and `resolveDamageBonus` grew `CombatantSheet holder` overloads: the roll, dano, Defesa and Movimento halves of one clause must all read the same Forma. |
| **Temporary RA grants** | `getTotalAbsoluteDamageReduction` never reads `getTemporaryBonus(ABSOLUTE_DAMAGE_REDUCTION)` — RA comes only from continuously-scanned passive hooks. |
| **Permanent Attribute bonuses — *built*; round-scoped ones — *partly built*** | `AttributeValue` has only `base`/`racialBonus`/`variable`, all permanent. A **permanent** `Feat#resolveAttributeBonus(AttributeDomain, Character)` grant (`VampiricoFeat#MESTRE_VAMPIRO`, the seven `BestialFeat` Heranças, `ConselheiroDeGuerraYmirianoFeat`'s +1 Gnose, `FeralFeat`'s three physical-trait Talentos, `FeericoFeat#NINFA`/`SIRENIDEO`, `TrollFeat#VIGOR_TROLLICO` — every one a *fixed* Atributo) is now summed by **`Character#getEffectiveAttributeTotal(domain)`**, and every Atributo-*total* reader calls it — PV/PM/PD, Conjuração, Rest, `EsquivaEApararInteraction`, `ItemRequirements`, `SpellDurationService`, the half-Atributo effect maths, and the melee ½-Força dano term. `AttributeValue#getBase()` readers deliberately do **not** (the Graduação cap, the Habilidade slot count, `FeatRequirements`, `CharacterAttributeService#upgradeBase`) — those gate on invested base. `AbstractSkillInteraction` keeps its own inline `resolveAttributeBonus` sum on top of `getValueForRoll` (equivalent; don't route `getValueForRoll` through the helper — it'd double-count). **Round-scoped** bonuses are still narrower: `ModifierType` has one `<ATTR>_BONUS` per `AttributeDomain` (`AttributeDomain#getBonusModifierType()`), a `TemporaryBonus` of one (`DOM_DE_MIRCALLA`) is read only on the Perícia-roll path (`getEffectiveAttributeTotal` has no `CombatantSheet`), and HP/PM/PD are not recomputed per Rodada anyway. |
| **Roll-resolution engine — *built*** | A roll now knows the GD it was made against and reports the verdict. `SkillRoll` carries an optional `targetValue` (a plain int — `DifficultyLevel#getBaseValue()` is the usual source, but `ConditionType#DEVORADO`'s "GD 10+Vigor" has no tier, and `SkillRoll.against(dice, tier)` is the convenience for when it does) and an optional `ActionCost` (roll-metadata — PA / Ação Livre / Reação — *not* a resolution input; read only by `Feat#resolveAttackCostDifficultyReduction`); `InteractionResult` reports `succeeded`, a signed `margin`, and the resolved `governingAttributeDomain` (for building a `CombatantAction`). **Three states, not two** — both are `null` when no target was stated, which an ability gated on success must read as "cannot tell", never "failed". A tie succeeds, and a held `difficultyReduction` eases the target by whole *níveis* before comparing, mirroring `AttackReceiver#resolve`. Two hooks hang off it: `SkillCompetencyAbility#resolveAutomaticSuccess(SkillType, int targetValue, SceneContext)` ("sempre bem-sucedido, dispensando rolagens" — takes the target because auto-success is routinely GD-capped) and `#resolveSuccessBlessings(...)`, resolved **only when `succeeded` is true** and reported on `InteractionResult#blessings` for the caller to grant. Still missing, and what a TODO should cite instead: `SkillExcellency` has no `resolve*` hook at all, so an Excelência cannot see a target GD or a `SceneContext` (`MedicinaECuraExcellency#FOCADO`); a `TemporaryBonus` is not consumed on first use, so "sua *próxima* rolagem" over-grants for the rest of its duration; and nothing rerolls — repeating a roll is the caller's step. |
| **Area de Efeito — *described, not resolved*** | The footprint is real data: `scene.AreaOfEffect` (an `AreaShape` — CIRCULO/LINHA/CONE/PENETRANTE/EXPLOSAO — plus one length in UD), reachable from `Spell#getTargeting()`, and ~35 authored Magias now supply one. Four things are still missing, so cite the specific one: (a) **footprint resolution** — nothing turns an area into a set of hexes or targets; a LINHA/CONE additionally needs a *facing*, which is chosen per cast, not authored on the Magia, so this belongs in `scene.grid` taking the aim as arguments; (b) **no classification of an incoming attack as an area one** — `AttackDelivery`/`AttackReceiver` carry no such flag, which is what still blocks `EsquivaEApararCompetencyAbility.EVASAO` and `AbencoadoPelaLuzAbility`; (c) **caster exclusion** — "a Conjurador is never damaged by their own Magia" is a universal rule, so it is deliberately *not* a column anywhere (no `excludesCaster` flag); it belongs to the missing targeting resolution, and `Spell` has no damage column to test against anyway; (d) **a Foco-scaled footprint** — four Magias grow their area at Foco 5 or above ("aumenta para Média se tiver Foco 5 ou superior"), and `AreaOfEffect` holds one fixed length, so each is authored at its base size with the growth clause in its prose. That needs a footprint resolvable against a sheet, which is (a). |
| **Malefício classification — *built*** | The Condições catalogue is real: `sheet.ConditionType` (17 entries, authored from `docs/rules/condicoes-e-maleficios-.txt`) + `sheet.Condition` (a `TemporaryEffect`, so Rodada countdown and expiry come free). Effects reach the engine three ways — typed `ConditionEffect` maluses summed by `CombatantSheet#getConditionBonus` into `DefenseService`/`AbstractSkillInteraction`; `getImplied()` conditions resolved transitively and deduplicated by type (Caído confers Desprevenido, and two sources conferring it still cost -2 once); and the outright prohibitions (`isMovementPrevented`/`isHealingPrevented`/`isAbilityActivationPrevented`/`isSpellCastingPrevented`), gating `MovementService`, `heal`, `ActiveAbilityService` and `SpellCastingService`. The fear ladder decays on expiry (`getDecaysTo`). **What's still missing, and what a TODO should cite instead:** (a) nothing *applies* a condition automatically — no critical-hit, attack or Talento hook affects a target, so every `applyCondition` is a caller's call (`ABRIR_DEFESAS`); (b) **a held trait can see its holder's Condições only where a hook has a `CombatantSheet`-taking overload** — `Feat#resolveSkillRollBonus`/`resolveDefenseBonus`/`resolveDamageReduction` now do (their longest form takes a `holder` and falls through to the sheet-less form; `AssassinoFeat#SAQUE_RAPIDO`'s drawn-weapon Desvantagem and `AssassinoFeat#ESCUDO_DE_SOMBRAS`'s Escondido +3 are real through them). `Feat#resolveCriticalMarginIncrease` has a `holder` overload too now (`AssassinoFeat#ACERTO_CRITICO_RELAMPAGO`). Still sheet-less, and so still blocking "enquanto estiver X": every `SkillCompetencyAbility` resolve hook (`ACAO_SURPRESA`, `MORTE_OCULTA`) and `Feat#resolveMovementIncrease` (`MOVIMENTO_FURTIVO`); (c) no per-condition **immunity or suppression** — a trait cannot be exempt from a Malefício, nor veto an implication (`ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_SUBMISSAO`'s "não Desprevenido enquanto Caído"); (d) three authored entries are inert pending systems of their own — Cego (needs a d6 sub-roll), Devorado, Envenenado ("Multiplicador de Bônus Base" exists nowhere), Doente. **`DESARMADO` means *disarmed*, never *unarmed*** — the effect that inflicts it is `CombatantSheet#disarm(Weapon)` (unequips, hands the weapon back since this core models no ground, and applies the condition only once no wielded `Weapon` remains); `rearm` lifts it, which is why it is open-ended rather than counting down. A weapon can refuse via `Weapon#isDisarmable()` ("Não pode ser desarmado", the Manopla de Segurança Aprimoramento). An **Ataque Desarmado** is a different thing entirely: a punch, authored as its own Arma Natural — it starts at `DamageBase.UNARMED` (the bottom rung) and still takes every Talento/Habilidade scale-up, while correctly taking none of the enhancement scale-ups bound to a weapon there isn't one of. `DEVORADO` leans on exactly that: it leaves you unarmed and additionally blocks re-arming (`ConditionType#preventsRearming()` → `rearm` refuses), since nothing you dropped is reachable from inside a creature — but it does **not** confer `DESARMADO`, whose Desvantagem the rules never charge a swallowed character. **Two things the rules text calls Malefícios are not `ConditionType` constants and must not become them**: *Coma* is `CharacterStatus#COMMA` (a PV tier), and *Encantamento* is `MagicType#ENCANTAMENTO` (a Magia — nothing tracks which Magias currently affect a combatant). `ESCONDIDO` is in the enum but not in the source file, being a Condição rather than a Malefício. Don't cite this as "no Malefício classification exists". |
| **Living/undead classification — *the tag exists, the behaviours don't*** | `CreatureType.RENASCIDO` is now a real value and `Vampiro` reports it from `getCreatureType()`. But a Renascido's *prerequisites* still count its life-race's type ("conforme sua raça em vida"), which is why `Race#getPrerequisiteCreatureType()` is a separate hook — `getCreatureType()` is the true class, `getPrerequisiteCreatureType()` (default → `getCreatureType()`, Vampiro → parent's) is what `Feat#isEligible`'s `requiredCreatureType` compares. What `RENASCIDO` still drives *nothing*: no healing inversion (`CombatantSheet#heal` has no redirect hook — Vampiro's "curado com Magia Profana, 1d6+Vigor" is inert), no Divine-vs-Profana magic-source distinction, no no-sleep/no-breath exemption (the "Fadiga/asfixia" row), no "Natural" damage immunity. `MonsterTemplate#isUndead()` stays the narrow stand-in for a foe. |
| **A summon acting on its summoner's roll** | `SummonedMonsterTemplate` builds a creature a Conjurador raised, but nothing models the player then *rolling for it*. `AttackDelivery` assumes the roller is the attacker and `AttackReceiver` that they're the defender; neither has a notion of rolling on a third combatant's behalf. This is why `CriticalEffect#applicableTo` is shared between them. |
| **Fadiga/asfixia, and healing inversion** | Nothing tracks sleep or breathing, so "não precisam dormir ou respirar" has no effect to be exempt from; and `CombatantSheet#heal` has no hook to redirect a recovery into damage (`Zumbi`'s Divine-magic clause — note `Zumbi` **is** `ReanimarSpell.REANIMAR`'s stat block, and what's missing between them is only a `Spell`-to-`MonsterTemplate` link). |
| **A foe's own dano roll — *half-closed*** | `DamageBase` now models exactly a "1d6+3"-shaped figure, so a stat block's "Danos de Ataques" finally has a type to live in — but `MonsterTemplate` has no column for it and `AttackDelivery`/`AttackReceiver` still assemble a `DamageInteraction` with the caller supplying the number. This core still never rolls the dice. Cite the missing *column*, not a missing concept. |
| **Attack maximum range — *partly built*** | An attack's max distance starts with the Weapon or Spell and is widened by the attacker's Categoria de Tamanho — see `docs/rules/categorias-de-tamanho.txt`, Alcance column. `AttackRangeService#getEffectiveRangeInUnidadesDeDistancia` reports the exact reach in UD; `getEffectiveRange` rounds it up into a `Range` band, which is right for `isWithin` comparisons and wrong for measurement (an adaga at Categoria +4 reaches 3UD but bands only reach 4). Built: `Weapon#getRange()` (authored Alcance, `@Builder.Default` `Range.ADJACENTE` — melee weapons carry one too) / `getEffectiveRange()` (→ `ADJACENTE` once destroyed, mirroring `getEffectiveDamageBase`), a Magia's `getTargeting().range()`, `Range#increasedBy(int)` (band-ladder shift, clamped both ends), `AttackRangeService#getEffectiveRange(Character, Weapon\|Spell)` (weapon returns a `Range`, spell an `Optional<Range>` — empty for Pessoal/Toque/Planar/caster-centred), and `Feat#resolveAttackRangeIncrease(Character, AttackSource)` — the **first `Feat` hook to take an `AttackSource`** — feeding `AttackRangeService` a step count. `ArtilhariaFeat#TIRO_LONGO`'s flat "+1 nível" half is real through it, scoped to `ATAQUE_A_DISTANCIA` delivery. `SizeCategory#getBodyRadius()` is the defender-side twin — how far a target's body extends from its centre, subtracted from the measured distance by whoever holds the positions (this core holds none). A reach rule, not occupancy: a creature still stands in one position. Still missing: no `SkillCompetencyAbility`/`AttributeAbility` range hook (added with its first consumer); no equipment scan (the offensive Obra-Prima/Aprimoramento catalog — "Alcance Estendido" — doesn't exist, and Arco Longo's "Alcance Base muda para" Favor is a *replacement* with no `ModifierType`); `TIRO_LONGO`'s "+2 níveis com Mira Impecável" half needs "this one delivered attack" scoping; and nothing gates an attack on being *in* range — `AttackDelivery`/`SpellCastingService#validateRequest` still never compare `range()` to the target's distance. |
| **Multi-target attacks — *built*** | One attack reaching several combatants is real: `Feat#resolveAdditionalTargets(SkillType, Character)` states how many targets beyond the primary a Talento grants, `AttackTargetingService#getMaximumTargets` sums it onto `BASE_TARGETS` = 1, and `DeliveredAttack#additionalTargets` (a `List<AttackTarget>` — sheet + that defender's own Defesa) carries them into `AttackDelivery#resolve`, which **refuses more than the attacker is entitled to** (`TOO_MANY_ATTACK_TARGETS`). The roll happens **once** and is compared against each Defesa in turn; every target but the primary gets its own `DeliveredAttackTargetResult` (margin/hit/Corrente threshold/chain, all judged per target) and a chain head marked `DamageInteraction#halvingDamage()` — a real attacker-side Meio-Dano, OR'd into the target's own sources by `DamageService#calculateFinalDamage(…, boolean halfDamage)` so it still lands last and never quarters. The single dano roll reaches a count-conditioned clause through `Feat#resolveDamageBonus(…, int targetCount)`. `ArtesMarciaisFeat#DOMINAR_ARTE_MARCIAL_ARTE_FLUIDA` is fully real through all of it. **Still missing, and what a TODO should cite instead:** the rules require an additional target to be *adjacent to the primary*, which is pairwise geometry between two combatants who are both not the roller — a `SceneContext` only holds distances measured from its own holder, so **picking the targets is the caller's/UI's job and this core enforces only the count**; every target-conditioned hook (`resolveDamageBonus`'s `attackTarget`, `resolveAttackRollBonus`) still resolves against the **primary** target alone; and Meio-Dano applied to *every* target including the primary has no expression (`CavalariaFeat#ATAQUE_EM_ARCO`'s "em cada alvo", which is additionally blocked on a montaria concept). |
| **Forced attack targeting / interception** | No "another Character becomes the target instead" mid-resolution — see `SantoAbility.GUARDA_VIDAS`. |
| **Reactive/retaliation damage** | `DamageService` only computes damage *to* a target *from* an attacker, never the reverse. |
| **Forced movement / positioning** | Knockback, "empurrado 1UD", Reposicionar — this core never does geometry. |
| **Continuous cross-character passive grants** | Partly built: `AventyrTitleAbility#resolveAllyAbsoluteDamageReduction` scans a target's adjacent allies for outward RA grants (Santo's Bastião dos Necessitados). Still missing for Defesas (`Santo` Despertar — its bonus is on the concrete class, unreachable by a scan) and for `SkillCompetencyAbility` (`INSTINTO_DE_LUTHER`). See the `damage-and-combat` skill, "Ally-facing passive grants are scanned, not granted". |
| **Movement-triggered Reações** | No movement-triggers-Reação mechanism, and no suppression of one. Cited by `POSICIONAMENTO_ESTRATEGICO` and `AS_NA_MANGA` — but note both of those grant their *movement* half for real. A clause exempting movement from Reações is currently **exempt from nothing**, so it costs nothing to omit; it becomes real the day this lands, and both constants need revisiting then. |
| **Resource-spend triggers — *built for Ego points*** | Closed for Ego: `EgoPointsService#useEgoPointsForEffect` spends and resolves the holder's `EgoAdvantage` against the completed `EgoPointSpend` in one call, which is how `DETERMINACAO_HEROICA` works for real. **A deliberate *use* and an enemy's *drain* are different call sites, not a flag** — `Primor` calls `CombatantSheet#spendEgoPoints` directly and triggers nothing, which is what stops a critical hit healing its victim. `AS_NA_MANGA` is real through the same hook (`resolveEgoSpendBlessings`, granting +2UD). PV/PM/PD spends still have no *reaction/report* path — the one PV-spend that is modeled is `ActiveAbility#getHitPointCost` paying into a timed state (a Poder Vampírico), which triggers nothing. **A *defeat* trigger is built** — `DefeatBlessingService#applyDefeatBlessings(attacker, defeated, viaCriticalHit)`, caller-driven (this core still has no true observer): after an attack the caller determined was fatal, it scans the attacker's `Feat#resolveDefeatBlessings` and applies each `Blessing` — `AssassinoFeat#SANGUE_QUENTE`/`VIOLENCIA_DESCOMUNAL`/`ARCANISMO_AVASSALADOR`. **A *combat-start* trigger is built** too — `CombatantSheet#startCombat()` scans the sheet's own `Character`'s `Feat#resolveCombatStartBlessings` and grants each `Blessing` as a `TemporaryBonus` (idempotent within a Cena, re-armed by `startNewScene()`); `Scene#startCombat()` fires it for every participant, flips `isCombatScene()`, and — the other half of this reshape — is the point from which `Scene#getCurrentRound()` begins to advance (`next()` bumps the counter and runs the Rodada boundary only while `combatScene`; before it, `next()` just cycles the cursor). `AnaoFeat#VIGOR_DO_INVERNO` is the consumer (self-scoped RD at `DamageService.DEFAULT_DAMAGE_REDUCTION` + `ModifierType.CRITICAL_RESISTANCE`, both lasting `HitPointsService#getLifeMultiplier / 2` Rodadas). No auto-fire, no scene-*end* trigger. |
| **One-time roll effects bought with a resource** | Spending PV/PM to modify a single roll's outcome (e.g. a GD reduction) has no transaction — see `Orc`'s Agnação Ancestral. |
| **"This one delivered attack" scoping** | A bonus scoped to the single attack delivered by activating another ability fits no per-roll `resolve*` hook, which are all generic per skill type. |
| **Within-Turn activation counter — *a roll-action log is built, per Rodada and per Cena*** | `CombatantSheet#recordAction(CombatantAction)` feeds two logs: `getActionsThisRound()` (skill, resolved governing `AttributeDomain`, `AttackSource`, `ActionCost`, `turnNumber`, `ActionOutcome`), cleared by `startNewRound()` (`Scene#next()` at the Rodada wrap), with `startTurn` marking a per-Turn slice read by `isFirstRollOfTurnFor`/`isFirstAttackRollOfTurn` (this *replaced* `consumeFirstRollThisTurn`); and `getActionsThisCena()`, cleared only by `startNewScene()` (`Scene#addParticipant`), for a "primeira ... na Cena" clause. `hasDrawnWeaponThisScene()` mirrors `hasDrawnWeaponThisTurn()`. `Feat#resolveCriticalMarginIncrease` and `resolveSkillRollBonus` have `CombatantSheet`-taking overloads that read these — `AssassinoFeat#SAQUE_RELAMPAGO`'s rider and `ACERTO_CRITICO_RELAMPAGO` are real through them, and so is `PequeninoFeat#SILENCIO_PRE_SURPRESA`'s "primeira Rolagem de Furtividade … em cada Cena" Vantagem (`getActionsThisCena()` holds no earlier Furtividade action). **The API records** — `applyTo` only reads, never writes. **A `Scene` keeps a permanent history too**: `Scene#recordAction(CombatantSheet, CombatantAction)` appends a `scene.SceneAction` (the acting sheet + the action) to `Scene#getActionHistory()` — never cleared, the client's combat log — *and* downstreams to `CombatantSheet#recordAction`, so a Scene-driven caller records once. The sheet method stays the no-Scene entry point, the `next()`/`startNewRound()` split. **The orchestrators build the `CombatantAction` for you**: `DeliveredAttack`/`IncomingAttack` carry an optional `Scene` (the `SpellCastRequest` "live Scene beside the snapshot" shape), and `AttackDelivery`/`AttackReceiver`/`SpellCastingService` bundle the roll as `DeliveredAttackResult`/`IncomingAttackResult`/`SpellCastingResult#getRecordedAction()` — still **not** recorded (all three stay report-only), so the caller files it with one `scene.recordAction(actor, result.getRecordedAction())`. Still missing: nothing auto-records; no *count of activations of one specific ability*; roll-actions only (movement has `consumeMovementThisRound`); no scene-*end* trigger (so "até o fim da Cena" is approximated with a long Rodada count — `AssassinoFeat#ARCANISMO_AVASSALADOR`). |
| **Game-session tracking — *a session is the sheet's lifetime; the recovery button still isn't*** | Two independent notions of "session", and they don't meet. (a) The end-of-session **recovery trigger** is deliberately outside this core: a Narrador presses a button, and the consumer calls `EgoPointsService#applySessionRecovery(Map<CombatantSheet, EgoDomain>)` — one call carrying the table's per-player choices. `MOTIVACAO_DE_MOSES`/`DILETO_DE_TYKHE` are real through it, and it stays **not idempotent** (double-application is the consumer's to prevent), because no session identity or counter exists. (b) A **once-per-session guard** *is* built: `CombatantSheet#consumeOncePerSession(marker)` claims a marker exactly once against a `transient` per-sheet set, where a session is **the sheet object's own lifetime in the running client** — the client holds the sheet open for as long as the table plays, and the state never reaches persistence, so a reloaded sheet starts fresh; `startNewSession()` is the explicit reset for a process outliving one sitting. `GnoseAbility#ESTABILIDADE_EMOCIONAL` is fully real through it (`AttributeAbility#resolveEgoDepletionGrant`, triggered inside `AbstractCombatantSheet#spendEgoPoints` — the one funnel a deliberate use *and* a Primor drain both pass, since "reduzido a zero" doesn't care which emptied it). Don't cite this row as "no session concept exists"; cite the specific half. Still missing: session identity/counting *within* a session (a "3x por sessão" clause has nothing to count with), a **Race** hook for a per-session grant (`Orc`'s Placitude Térrea — `EgoAdvantage#resolveExtraSessionEgoRecovery` has no `Race` counterpart), and the trigger/Cena-duration halves of `MeioElfo`'s "1x por sessão". |
| **"Next Rodada" delayed grants — *built for Ego points only*** | `CombatantSheet#scheduleTemporaryEgoPointGrant(domain, source, amount)` registers a `DelayedEgoGrant`, delivered by `startNewRound()` — the real Rodada boundary (`Scene#next()` calls it), deliberately **not** a `TemporaryEffect`: those tick at *Turn end* via `finishTurn`, so one registered mid-Rodada would fire inside the very Rodada it must skip. Same "register now, resolve at the boundary" shape `owePendingEgoRecovery`/`applyPendingEgoRecoveries` has for a Rest. The grant itself is `grantTemporaryEgoPoints(domain, source, amount)` — **two steps on purpose**: widen the ceiling (`grantTemporaryEgoPointBonus`, capped per source) *then* `recoverTemporaryEgoPoints` under it, because on an emptied pool a bare recovery restores nothing and a bare widening grants nothing the second time the same source fires. Nothing else is schedulable into a future Rodada — a delayed *action*, damage, or `Blessing` still has no carrier. |
| **Roubo de Mana / de Determinação** | Only Roubo de Vida exists (`LifeStealService`). Its total now sums a `Feat#resolveLifeStealBonus(Character)` scan (`VampiricoFeat#SEDE_DE_SANGUE`) alongside the `AttributeAbility` one — still gated on an active `LifeSteal` effect being present ("amplify, never grant from nothing"), and still with **no automatic combat caller** (nothing in `src/main` resolves a dealt hit and reads `getTotalLifeSteal`). |
| **Terreno difícil** | `TerrainType` describes a whole Scene, not a per-movement cost to ignore. |
| **Item numeric columns** | PE is a **budget now, but only a store purchase spends it**: `AbstractCombatantSheet#equipmentPoints` (+`grant`/`spendEquipmentPoints`, throwing `NOT_ENOUGH_EQUIPMENT_POINTS`) is the wallet, and `ItemPurchaseService#purchase(ItemStore, ItemSpecification\|ItemTemplate, CombatantSheet)` debits it — forging the copy via `ItemForgery#purchased`, applying `EgoAdvantage#resolveEquipmentPurchaseDiscount` (`ResourcesAdvantage#BARGANHISTA`'s −2PE, floored at 1), adding it to the buyer's inventory. **Production** still doesn't spend — `ItemForgery#getForgingCost`/`EquipmentCraftingService#getFabricationCost` report, nothing deducts (BARGANHISTA's "produzir" half stays TODO). **Obra-Prima and Aprimoramento Preços are authored now** — `EnhancementPricing` is the two rarity-by-`EnhancementPriceCategory` grids (Armas/Armaduras/Pedras do Poder) from `equipamentos.txt`, and `Masterpiece`/`Improvement#getPriceModifier()` default to a lookup down the constant's own `getPriceCategory()` column (both hooks abstract on purpose, so the unauthored offensive catalog can't be silently priced as armour). Only `ItemActiveAbility#getPriceModifier()` is still 0. Conjuração has no item-granted hook on either `SpellCastingService` roll. Dureza is off this list: a real, consumed pool, and now with a *repair* path too (`Item#repair`/`EquipmentCraftingService`). A Pedra do Poder's Cargas/Resfriamento/Danos de Vinculação/Duração do Efeito (`PowerStoneQuality`) stay authored-inert: no activation service and no forge/bind step consume them. An Aprimoramento's own fabrication *time* still can't be computed — `EquipmentCraftingService`'s days-and-hours figures read `ItemTemplate#getPrice()`, the bare base, not a specification's total. |
| **Acquisition-time choice on a Talento — *built, but only one kind is discoverable*** | A Talento whose rules say "escolha uma Perícia / um tipo de terreno / um tipo de arma" records the pick as a hand-written `AbstractFeat` subclass carrying a `@NonNull` choice field (`FocoEmPericiaFeat`, `TerrenoPrediletoFeat`, `EspecialistaEmArmaFeat`, `AtiradorPerfeitoFeat`, `AdotadoPorSylphFeat`, `AcertoCriticoAprimoradoFeat`, `ArmamentoDraconicoFeat`) — the same catalog-vs-acquired split `ArtesAprimorarComArteAbility` keeps, overriding `Feat#catalogEntry()` so `isEligible`/`FeatCatalog#availableFor` still see it as the enum constant, and a static `chosenBy(Character)` for dependents. Weapon-type choices are the `AttackMethod` enum, matched against the delivered `AttackSource` (`Feat#resolveSkillRollBonus`/`resolveCriticalMarginIncrease` grew a trailing-`AttackSource` cascading overload for it); `ArmamentoDraconicoFeat` instead carries a `Set<NaturalWeapon>` (exactly 2 of a fixed 4) and grants them through `Feat#getGrantedNaturalWeapons`. **Still not covered**: a choice of Ego/Árvore de Magia (a chosen *Atributo* is now within reach — `ConselheiroDeGuerraYmirianoFeat` grants a *fixed*-Atributo bonus + a *fixed* Habilidade via `Feat#resolveAttributeBonus` (full effective total) and `Feat#getGrantedAttributeAbilities`; a *chosen*-Atributo one just needs a subclass whose two hooks branch on the picked `AttributeDomain` — `HumanoFeat#LIMIAR_DA_EVOLUCAO`'s own work), a "pick N Formas" (no form state), and `FeatRequirements` has no hook for "a held Talento's own choice must be/not be X". **Discoverability is `Feat#resolveRequiredChoices(Character)`** → `List<FeatChoice<?>>`, where `FeatChoice<T>(Class<T> type, int picks, List<T> options)` carries a type token to route on, how many must be picked, and the options **already filtered for that holder** (a Rakshasa is not offered Névoa). A client walks the catalog asking this one question instead of knowing which constants are special. **A non-empty answer makes the bare constant ungrantable** — `FeatService#grantFeat` throws `FEAT_REQUIRES_CHOICE`, detected as `feat == feat.catalogEntry()` — which matters because a choice-carrying Talento's effect usually lives *entirely* on its acquired form, so granting the constant plain cost XP and did nothing. **9 constants declare their choice**: `FOCO_EM_PERICIA`, `TERRENO_PREDILETO`, `ESPECIALISTA_EM_ARMA`, `ATIRADOR_PERFEITO`, `ACERTO_CRITICO_APRIMORADO`, `SAQUE_RELAMPAGO`, `ARMAMENTO_DRACONICO`, `CONSELHEIRO_DE_GUERRA_YMIRIANO`, `METAMORFOSE_DRACULEA`. **The rest cannot yet**, and it is one missing piece rather than four: `AdotadoPorSylphFeat`, `HerancaBestialFeat`, `ChosenSkillTraitsFeat` and `HabilidadeDeAtributoEscolhidaFeat` all need to enumerate *options* nothing indexes — there is no registry of a Perícia's own `SkillCompetencyAbility`/`SkillSpecialization` constants, and none of every `AttributeAbility`. `SkillType` already carries an `excellencyClass`, and `AttributeDomain` could carry an ability class the same way, so both are mirror-additions rather than new mechanisms. Two invariants are pinned catalog-wide in `FeatServiceImplTest`: every declaring constant refuses its bare form, and no declared choice offers fewer options than it demands picks. See the `adding-a-feat` skill. |
| **Acquisition-slot grants — *built for Talentos; no `Race` path*** | A **Talento** hands its holder free traits through two hooks, both folded live into the views every consumer already reads, so no service changes: `Feat#getGrantedAttributeAbilities(Character)` → `Character#getAttributeAbilities()` (a Habilidade de Atributo — `ConselheiroDeGuerraYmirianoFeat`'s fixed Força one, and `HabilidadeDeAtributoEscolhidaFeat` for the three `DestinoFeat` constants whose Atributo is the player's pick — no field for the Atributo, since an `AttributeAbility` reports its own), and `Feat#getGrantedSkillTraits(Character)` → `SkillCompetencyAbility#allFor` (now a **three**-source scan, deduplicated) and `Character#getSpecializations(SkillType)` (**read this, not `CharacterSkill#getSpecializations()`**, which is the acquired list alone). One hook carries both trait kinds because the rules text routinely offers a choice between them in one clause. Neither grant consumes a paid slot, and only the granted trait's passive/`resolve*` hooks apply — not the one-time acquisition side-effects a service would run. **Which trait was picked lives on an acquired `AbstractFeat` subclass**: `HerancaBestialFeat` (six `BestialFeat` Heranças, forwarding each constant's own Atributo/Arma Natural clauses by hand — `AbstractFeat` deliberately does *not* blanket-forward to `catalogEntry()`, since a choice-carrying form replaces its constant rather than decorating it), `AdotadoPorSylphFeat`, and `ChosenSkillTraitsFeat` for the cross-tree constants that grant traits and *nothing else* (`PeritoFeat#TREINADO_EM_PERICIAS`, the two `GnomoFeat` ones) — don't widen that last one with delegation. Still no shape: a free **Talento** slot ("escolha um Talento Racial"), and a **`Race`** hook for any of these (`Anao`'s Pequenos Gigantes, `Elfo`'s Origem Mística — `Feat` is the only granting path). |
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
`Skill.DISADVANTAGE_MALUS` convention, and the test checklist. **`ItemCatalog`** is the
`FeatCatalog` equivalent — every authored `ItemTemplate`, discovered from an explicit
`CATALOG_ENUMS` registry (not a sealed `permits`, because tests use anonymous `ItemTemplate`s);
a new category enum must be added there. `ItemCatalog.availableUpTo(ItemRarity)` /
`ItemRarity#isPurchasable`/`isAtMost` are what `ItemStore` filters by. The architecture:

- **Catalog vs owned copy** — the split `AventyrTitle`'s javadoc documents. The **catalog** side
  is `ItemTemplate` (the enum constants: `ArmorItem` etc.); the **owned copy** side is `Item`
  proper, built via `AbstractItem`/`AbstractWeapon`, carrying its own per-copy state: Dureza
  remaining (`applyDamage`/`repair`), a fitted Obra-Prima (`ItemMasterpiece`) and a
  weight-class-capped `List<Improvement>` of Aprimoramentos (`addImprovement`; `getImprovement()`
  is a deprecated first-or-null shim), a socketed **Pedra do Poder** (`PowerStone`), a **Regalia**
  marker (`Item#getRegaliaGrade()` → `RegaliaGrade` MENOR/SUPERIOR/DIVINA, `isRegalia()` iff set —
  a per-copy property, **never an `ItemRarity`**) carrying an `ItemActiveAbility`, and **who
  produced it** (`producedByCharacterId`). Fabricação/reparo runs through
  `EquipmentCraftingService` (see the gap catalog's "Owned/produced item copy" row), but the forge
  itself is its own type: **`ItemForgery`**, working from an **`ItemSpecification`** — the "what to
  make" value (base `ItemTemplate` + `ItemMasterpiece` + `List<Improvement>` + optional
  `RegaliaGrade`), since a copy is more than its catalog entry. Every `forge`/`forgeRegalia` on the
  service is a delegate. **Three paths, and the difference is the whole point**: `ItemForgery.by(
  crafter, trade, spec[, donation])` checks the trade, the Regalia **permission**, the willing
  Centelha donor (`RegaliaDonation`) and the Divina essence, the Obra-Prima Graduação floor and the
  Aprimoramento rules (Obra-Prima to host, weight-class cap, no duplicates — the same three
  `installImprovement` applies), stamps `producedByCharacterId` and advances
  `Character#recordRegaliaCrafted`; `ItemForgery.donatedByAventyr(spec)` is the **GM path** — no
  crafter, no gate, no cost, no history, no Dureza scaling — marking the copy
  `Item#isDonatedByAventyr()`; `ItemForgery.purchased(spec)` is the **shop path** — no crafter and
  no crafter-gates like the donation, but it **refuses a Regalia** (`STORE_DOES_NOT_SELL_REGALIA`),
  the copy is *not* marked a donation (someone made it, just not the buyer) and has no maker
  (`producedByCharacterId` null, "maker unknown" — the same as loot), and `getPurchasePrice()` is
  the **full** `getTotalValue()`, not the self-forge half. The donation marker is deliberately not
  a `null producedByCharacterId`, which only means "maker unknown" (loot, a bridge copy, a shop
  copy); a donation says there was no making. Neither `donatedByAventyr` nor `purchased` is a
  method on `EquipmentCraftingService` — a donation skips every number that service computes, and
  a purchase has its own service (`ItemPurchaseService`, which spends PE).
- **A forge is assembled a decision at a time, and each one is priced.** `ItemForgery#setMasterpiece`
  /`addImprovement`/`setActiveAbility` each replace the held `ItemSpecification` (via `toBuilder`)
  and **re-total** `getTotalValue()` — base Preço plus every part's own `getPriceModifier()`
  (`Masterpiece`, `Improvement`, and now `ItemActiveAbility`). Recomputed, never accumulated, so
  clearing a part gives its price back. `getForgingCost()` is **half that total, halved once at the
  end**, floored, never below 1 — the same "metade do Preço" as
  `EquipmentCraftingService#getFabricationCost` for a bare template, and **0 for a donation**
  (worth is not cost). **An Obra-Prima and an Aprimoramento each carry a real Preço** via
  `EnhancementPricing`; only an `ItemActiveAbility` still contributes 0. A
  forge's cost is **reported, not spent** (only a *store purchase* spends PE — `ItemPurchaseService`
  against `AbstractCombatantSheet#equipmentPoints`). The mutators validate nothing; `validate()`
  judges once (and it is what refuses an `ItemActiveAbility` on a non-Regalia,
  `ACTIVE_ABILITY_REQUIRES_REGALIA`, rather than letting `AbstractItem#setActiveAbility` throw).
  **Acquiring a Talento and using it are different questions.** `FeatRequirements`/`isEligible`
  gate acquisition (checked once, by `FeatService#grantFeat`); a condition that must hold at every
  use is a hook. `ArtificeFeat` carries both — Profissão 7 / the rung below / the forged-Regalia
  history are requirements, "a Regalia em sua posse" is `Feat#itsAllowedToCraftRegalia(holder)`,
  returning the `RegaliaGrade` permitted *right now* (`null` = not permitted, never "no such
  Talento"; `Character#possessesRegalia` is the condition). `ItemForgery` scans held Talentos for a
  permission rather than looking one up by grade, so an outside `Feat` can permit a grade with no
  change to the forge.
  Still unmodeled: a PE economy for *production* (a store purchase spends PE, a self-forge only
  reports its cost), the offensive Obra-Prima/Aprimoramento catalogs, any Pedra
  do Poder charge/bind economy, and — for Regalias — the Centelha *loss* itself, the *Forja do
  Olho de Deus* location, and the Divina mandatory-Crítico (all caller/GM adjudicated). TODOs
  cite these (`ResourcesAdvantage#HERANCA_FAMILIAR`).
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
  a list of `ItemBonus` (a `ModifierType` + value + `FavorCondition`), resolved via
  `ItemFavor#resolveBonus(ModifierType, Character|CombatantSheet)` / `Item#resolveFavorBonus(...)`
  — 0 unless the `ItemRequirements` are met. It's **data, not `@Modifier` methods**, unlike every
  ability enum, and that's forced: `@Modifier`'s `ModifierType` is a compile-time-fixed
  annotation value, so one shared `ItemFavor` class can't vary which type a given item grants —
  the same limitation "A ModifierType per skill" documents. **Don't route items through
  `ModifierResolver`.** `ItemBonus` is deliberately not `TemporaryBonus`/`Blessing` either: an
  item's Favor lasts as long as the item is carried and never reaches anyone but its wielder, so
  a countdown, a `TargetScope` and a granting `source` would all be dead weight. The **one**
  live-state gate is `FavorCondition` (default `NONE`): `NO_OFFENSIVE_ACTION_THIS_ROUND` is the
  Escudos' "se não realizou nenhuma ação ofensiva nesta Rodada" — read against
  `CombatantSheet#hasActedOffensivelyThisRound()` (derived from the per-Rodada action log, no new
  state), and resolves to 0 on any `Character`-only path.
- **An "Efeitos Adicionais" line is a real effect, not flavour.** Fold it into `favor.bonuses`
  wherever it maps to a consumed `ModifierType`; the `additionalEffects` string is only for one
  this core still can't express. It is gated by the Favor's `ItemRequirements` when there is a
  Favor, and applies unconditionally (`requirements = null`) when there isn't.
- **`ItemRequirements` checks `getTotal()`, not `getBase()`** — deliberately unlike
  `FeatRequirements`, which uses `base`: acquiring a Talento is gated on what the character
  personally invested in, but whether an item's Favor applies is a "can I meet this right now"
  question, so a Bônus Racial or a variable bonus counts. It's a narrower record than
  `FeatRequirements` (no `requiredSkillType`/`requiredFeat`), plus an optional `alternativeDomain`
  for a "Car 3/Gno 3"-style two-Atributo column (`isMetBy` = either).
- **The Favor reaches a real consumer for RD/RM, DF/DM, `MOVEMENT`, and a named Perícia's roll
  bonus.** `DamageServiceImpl` scans `DAMAGE_REDUCTION`/`MAGIC_REDUCTION` (sheet-aware on the RD
  path, so a `FavorCondition` gate lands), `DefenseServiceImpl.sumEquipment` DF/DM (sheet-aware
  too), `MovementServiceImpl` a flat `MOVEMENT` bump, and `AbstractSkillInteraction` a
  whole-Perícia `<SKILL>_ROLL_BONUS` — a new item's values flow into all of them with no wiring.
  Dureza is a real pool spent by `Item#applyDamage`. **Preço** is spent only on a *store
  purchase* (`ItemPurchaseService`); a self-forge only reports it. **Conjuração still has no
  consumer** — no item-granted hook on either `SpellCastingService` roll. Their values are real,
  exact data all the same, per the "can't apply it yet doesn't mean can't compute it yet"
  discipline.
- **Efeito Crítico Defensivo — Armaduras e Escudos only.** `getDefensiveCriticalEffect()` lives
  on the `CriticallyDefensiveItem` interface (`ArmorItem`/`ShieldItem`), never on `Item` — a
  Capa/Bota/Elmo grants none. Authored from `docs/rules/efeitos-criticos.txt`; no reader yet.
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
- **`NaturalWeapon` is the Armas Naturais catalog** — one constant per `equipamentos.txt` "Armas
  Naturais" row, `implements ItemTemplate, Weapon`, `getCategory()` fixed to `NATURAL_WEAPON`
  (so `Character#treatsAsNaturalWeapon` recognises one with no change) and `ItemRarity.NATURAL`
  the marker for "part of a body, not bought or forged". Preço/DF/DM/Dureza/Conjuração are all 0
  and the Favor is `null` on every constant (no authored natural-weapon Favor has a
  `ModifierType`). Granted by `Feat#getGrantedNaturalWeapons` **and** `Race#getGrantedNaturalWeapons()`
  (`Vampiro`'s per-`VampiroLineage` weapons), both folded into `Character#getNaturalWeapons()`.
  No offensive Efeito Crítico column (no weapon→crit scan exists — the "Empalar (17)" numerals
  are javadoc-only).
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
- **Nothing calls `applyDamage` automatically yet.** Every clause that deals item damage —
  `StrengthAbility#ESTILHACADOR`, `DuelistaFeat#DEFENDER_SE_ATACANDO`, the
  Estilhaçador/Sabotar/Repelir e Suprimir/Retorno de Danos Efeitos Críticos, Bola de Fogo
  Elduriana's "Objetos em posse dos personagens afetados sofrem metade deste dano" — is blocked
  on a further system of its own. Real and tested with no automatic caller, like
  `MoralHerdadaAbility#applyStartingFama`.
- **Repair is built** — `Item#repair(int)` is the raw mutator (mirror of `applyDamage`, a no-op
  on a template, and it *can* bring a wreck back), and `EquipmentCraftingService#repair` is the
  validated player entry point that also folds in `ProfissaoCompetencyAbility#REPARO_MELHORADO`.
  The Artesão tree (`ArtesaoSpell#RESTAURAR_OBJETOS`) can now call `repair` too; `FORTALECER`
  still needs Scene-scoped damage tracking on top.

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
