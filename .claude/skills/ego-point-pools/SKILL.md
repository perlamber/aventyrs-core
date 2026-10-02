---
name: ego-point-pools
description: This skill should be used for any work on the two spendable Ego point pools per `EgoDomain` on a `CombatantSheet` — `EgoPointPool` (the four equations: permanentMax/permanentRemaining/temporaryCeiling/temporaryRemaining), `CombatantSheet#spendEgoPoints`/`getAvailableEgoPoints`, `EgoPointsService#useEgoPointsForEffect`/`applySessionRecovery`, `EgoPointSpend`/`EgoPointType`, `PendingEgoRecovery`, `TemporaryEgoPenalty`, `receiveTemporaryEgoPoints`/`receiveNonCumulativeTemporaryEgoPoints` (received points and consumed extras), the cap at 5 (`Character#MAX_EGO`, `getEgoOverflow`, `restoreExtraTemporaryEgoPoints`), `EgoPointsService#grantTemporaryByNarrator`/`grantPermanentByNarrator`, Recursos → PE (`SocialClass`, `spendResourcesForEquipmentPoints`, starting PE and store), `scheduleTemporaryEgoPointGrant`/`DelayedEgoGrant` (points owed on the next Rodada), `CombatantSheet#consumeOncePerSession`/`startNewSession` (the once-per-game-session guard, a transient per-sheet marker set), `AttributeAbility#resolveEgoDepletionGrant` (a clause reacting to an Ego being reduced to zero), or the `EgoAdvantage` hooks `resolveEgoSpendRecovery`/`resolveEgoSpendBlessings`/`resolveExtraSessionEgoRecovery`/`resolvePermanentEgoGain`. Also use it when asked why spending a permanent point "hurts twice", why there's a `spent` counter instead of a held balance, why `AUTOCONTROLE`/`RECURSOS`/`SORTE` aren't `ModifierType`s, or how `Primor`'s drain differs from a deliberate use, why a received point refills before it becomes an extra, why no Ego is above 5, or what "a primeira vez em cada sessão de jogo" means here.
---

# Ego points are two pools per domain — `EgoPointPool`, `spendEgoPoints`

An `EgoDomain` isn't only a rating: it's **two spendable point pools** on a `CombatantSheet`,
permanent and temporary, both real currency. The whole model is four equations, all in
`org.aventyrs.core.sheet.EgoPointPool`:

```
permanentMax        = character.getEffectiveEgoTotal(domain)   // base + variable + Talentos, ≤ 5
permanentRemaining  = max(0, permanentMax - permanentSpent)
temporaryCeiling    = max(0, permanentRemaining - Σ activeEgoPenalty)
temporaryRemaining  = max(0, temporaryCeiling - temporarySpent) + extras
availableEgoPoints  = permanentRemaining + temporaryRemaining
```

**Both pools start full**, and the temporary ceiling tracks permanent points *remaining* rather
than the permanent maximum — so **spending a permanent point hurts twice**. Sorte 3 with nothing
spent is 3 + 3 = 6 spendable; spend the 3 temporary then the 3 permanent and you get all 6, but
spend the 3 permanent *first* and the ceiling collapses to 0, so you only ever get 3.
`EgoPointFeatureTest` pins both directions.

**A permanent spend keeps the temporary points still held** (table ruling, 2026-09-30): held =
min(held, new ceiling), never ceiling − spent. The book's Recursos example is the proof — Recursos 5
(3 disponíveis) spends 2 temporary then 1 permanent and "fica com Recursos total 4 (1 disponível)";
subtracting the spent count would give 0. `EgoPointPool#spendPermanent` does it by taking the same
amount back off `temporarySpent` (floored at 0); the normalization clips what no longer fits.

- **Permanent points never recover.** Not on Rest, not per session, not anywhere. They're only
  *earned*, via `AttributeAbility#resolvePermanentEgoGain` → `CharacterEgos#withVariableBonus`.
  Nothing reduces them automatically either — only their holder spending one deliberately.
- **Temporary points recover one per game session** — one *total* across all four domains, not
  one apiece (table ruling: the GM picks one Ego in the end-of-session modal and every character gets
  1 point in it — the map form below with one domain throughout; it only *refills*, never an extra).
  That's
  `EgoPointsService#applySessionRecovery(sheet, chosenDomain)`, plus whatever extra a held
  Vantagem grants in its own domain (`EgoAdvantage#resolveExtraSessionEgoRecovery`;
  `MOTIVACAO_DE_MOSES`/`DILETO_DE_TYKHE` are the two). **No automatic caller, by design** — a
  session ends when the table says so, so the trigger is a GM action in the consuming app. The
  bulk overload `applySessionRecovery(Map<CombatantSheet, EgoDomain>)` is what that button calls:
  the map carries each player's own choice, and being *in* the map is how a consumer excludes a
  foe or an absent player — no `instanceof CharacterSheet` filter, no "everyone in the Scene"
  default. Pair it with `Scene#getAllParticipants()` (active ∪ pending, disjoint by construction)
  to build the roster. It is **deliberately not idempotent**: a second press recovers again,
  bounded only by each ceiling, and guarding that belongs to the consumer: the only session state
  this core has is the per-sheet `consumeOncePerSession` set below, which is a clause-level guard,
  not a record that a session ended.
- **Rest does not refill the temporary pool.** `RestService#applyRest` resolves only
  `PendingEgoRecovery` — points some effect *specifically promised back* (today: `Primor`'s).
  Don't wire session recovery into it.

**Why a `spent` counter and not a held balance.** Both halves are `ResourcePool`s. With a stored
balance every ceiling change (a permanent spend, a penalty landing or expiring, a point earned)
needs a destructive clamp pushed from four call sites, and such a clamp is irreversible — an
expiring penalty could never give back the point it truncated. With a spent counter the ceiling is
only ever *read*, so what remains is recomputed fresh and un-clamps by itself. Same
recompute-on-demand discipline as `HitPointsService#getStatus` and
`InitiativeEntry#getEffectiveInitiativeValue`. `temporarySpent` is additionally **normalized down
to the current ceiling** on every temporary-facing call, so a fallen ceiling can't leave a hidden
debt that swallows the next recovery — deliberately unlike `ResourcePool` for PV, where overspend
past the max is *kept*, because that negative range is what distinguishes FALLEN/COMMA/DEAD.

**The permanent max is read straight off `Character`, and PV's isn't.** `AbstractCombatantSheet`
calls `getCharacter().getEgos().getEgo(domain).getTotal()` directly. That looks like the
`HitPointsService#getStatus` case but isn't: `getStatus` had to leave `sheet` because max PV needs
a `ModifierResolver` three-source `@Modifier` scan and `org.aventyrs.core.sheet` must not depend
on `org.aventyrs.core.character.services`. A permanent Ego max needs **no scan at all**.

**Don't confuse the pool with `InitiativeService`.** It reads the same `EgoValue.getTotal()`, then
adds a `ModifierType.INITIATIVE` three-source sum, because Iniciativa doubles as a turn-order
stat. A `TemporaryBonus(INITIATIVE, +2, 2)` must never widen how many Iniciativa *points* can be
spent. This asymmetry is the likeliest future misreading of the two.

**Spending names its pool, and reports back.** `spendEgoPoints(EgoDomain, EgoPointType, int)`
returns an `EgoPointSpend` (domain, type, and the amount *actually* spent after clamping). There
is deliberately **no** temporary→permanent fallback (it would silently spend a point that costs
twice over) and **no** shorter overload defaulting the type — the cascading-overload convention is
for genuinely *optional* inputs, and the pool is the whole question. It floors at 0 rather than
throwing; affordability is the caller's own `getAvailableEgoPoints(domain) >= n`. The reported
`type` is what `DETERMINACAO_HEROICA`'s "se o ponto for permanente" needs, and it's why callers
promising points back (`Primor` → `PendingEgoRecovery`) must register `spend.getValue()`, never
what they asked for.

**Two hooks fire off a deliberate spend**, both resolved by `useEgoPointsForEffect` and both
scoped to the Vantagem held in the spend's *own* domain: `resolveEgoSpendRecovery` returns a flat
figure applied to PV/PM/PD (`DETERMINACAO_HEROICA`), and `resolveEgoSpendBlessings` returns
`Blessing`s granted straight to the spender (`AS_NA_MANGA`'s +2UD Movimento). Blessings are
applied directly rather than handed back, unlike `resolveInitiativeBlessings`, because who spent
the points is unambiguous — so `TargetScope` other than `SELF` has no meaning there yet.

**Using points is a different call site from being drained of them.**
`EgoPointsService#useEgoPointsForEffect(sheet, domain, type, amount, rolledValue)` is the holder
deliberately spending — it spends *and* applies whatever `EgoAdvantage#resolveEgoSpendRecovery`
that use earns (`DETERMINACAO_HEROICA`'s "+1d6PV, PM e PD", doubled on a permanent point), in one
call so the recovery can't be forgotten. `CombatantSheet#spendEgoPoints` is the raw drain, and is
what `Primor` calls — reacting there would mean a critical hit *healing* the character it just
hit. There is no flag on the spend distinguishing the two, and no observer: the choice of entry
point is the distinction. The hook is consulted only for the Vantagem held in the spend's **own**
domain. The 1d6 arrives already rolled (one roll covers all three pools) and is validated as a
legal d6 face — a negative would otherwise reach `heal` and silently *damage* the character.

**A temporary Ego reduction is `TemporaryEgoPenalty`, a dedicated `TemporaryEffect`** — not a
negative `TemporaryBonus`. It lowers the temporary *ceiling* and is never a spend, which is the
point: a spend is consumption that would outlive the penalty's own Duração, while a ceiling term
reverses the moment the effect expires. Adding `AUTOCONTROLE`/`RECURSOS`/`SORTE` to `ModifierType`
would expose them to `ModifierResolver`'s `@Modifier` scan (implying abilities could grant
spendable Ego points), and `INITIATIVE` already means turn order, so one of the four domains would
collide semantically with no way out. Same restraint that keeps `LifeSteal` off `ModifierType`. It
rides the ordinary `applyEffect`/`tickTemporaryEffects`/`finishTurn` lifecycle with no new
machinery, and is read by a private `sumEgoPenalty` mirroring `getTotalLifeSteal`.

**Received points and extras (table ruling, 0.0.76).** A temporary point arrives one of two ways:

- **Recovered** — `recoverTemporaryEgoPoints`: "get back what you spent". Refills under the ceiling,
  never past it. The session point, Primor's Rest promise, Uno com a Ira's hours.
- **Received** — `receiveTemporaryEgoPoints(domain, source, amount)`: "você receberá N pontos
  temporários". Refills first, exactly like a recovery; the remainder is held as an **extra** above the
  ceiling. A Narrador's grant, `DelayedEgoGrant` delivery, Estabilidade Emocional, the past-5 overflow.

An extra is **consumed, not a ceiling**: a temporary spend takes extras first (oldest source first),
and a spent extra never comes back — no recovery reaches it. The table's example, pinned in
`EgoPointFeatureTest`: Ego 3 full receives 1 → 4 temporary; spends 2 → 2; receives 1 → back to 3 / 3.
`getMaxTemporaryEgoPoints` is the ceiling only, so `getTemporaryEgoPoints` may exceed it; the spent count
under the ceiling is `max − (temporary − getExtraTemporaryEgoPoints)`.

Extras are held per source only so a source can ask what it still holds.
`receiveNonCumulativeTemporaryEgoPoints` is `CharismaAbility.DESTINO_FAVORAVEL`'s "um ponto temporário,
não cumulativo": nothing while that source still holds its unspent extra; an unrelated source's points
still add on top. An **Ego loan** (`receiveEgoLoan`, Transferência) is held as an extra outright
(`EgoPointPool#addExtra`, no refill) so it stays identifiable: at the end of the Cena a still-held one is
withdrawn and goes back to its lender, a spent one is simply gone.

**No Ego is ever above 5.** `Character#getEffectiveEgoTotal` is capped at `Character.MAX_EGO`, so the
pool, Iniciativa and Moral Herdada's Fama all see 5. What runs past it is `Character#getEgoOverflow`,
received **once** as extras: every pool read goes through the sheet's private `egoPool(domain)`, which
calls `EgoPointPool#syncOverflow` — it remembers how much overflow was already received, so Recursos 7
starts at 5 + 2 extras, and a point earned past 5 lands the first time it's seen, but neither twice.
A consumer that rebuilds sheets must persist `getExtraTemporaryEgoPoints` and `getEgoOverflowReceived`
and hand both back through `restoreExtraTemporaryEgoPoints` **before any other Ego read** — otherwise a
rebuilt sheet receives its overflow afresh. Rehydrated extras are held under one source, so a
non-cumulative source's identity doesn't survive a rebuild.

**The Narrador's grants** are `EgoPointsService#grantTemporaryByNarrator(sheet, domain, amount)` — a
received point — and `grantPermanentByNarrator(character, domain, amount)`, which returns a rebuilt
`Character` with `+amount` variable (a sheet's Character is final; build a new sheet from it).

**A grant owed on the *next* Rodada is `scheduleTemporaryEgoPointGrant(domain, source, amount)`**,
carried as a `DelayedEgoGrant` and delivered by `startNewRound()` — the real Rodada boundary
(`Scene#next()` calls it). Deliberately *not* a `TemporaryEffect`: those tick at Turn end via
`finishTurn`, so one registered mid-Rodada would fire inside the very Rodada it must skip. With
nothing ever calling `startNewRound`, the grant simply waits — the same fallback
`consumeMovementThisRound` documents.

**"A primeira vez em cada sessão de jogo" is `CombatantSheet#consumeOncePerSession(marker)`** —
`true` only the first claim, backed by a `transient` per-sheet set. **A session is the sheet
object's lifetime in the running client**: the client keeps the sheet open for as long as the
table plays, and the state is deliberately excluded from anything persisted, so a reloaded sheet
starts a fresh session. `startNewSession()` resets it for a process that outlives one sitting;
`hasConsumedOncePerSession` is the non-consuming reader. This is **independent of**
`applySessionRecovery`'s GM button above — that call may never be made, and this guard must hold
regardless.

**Ego depletion is a trigger, and it fires on a drain too.** `AttributeAbility
#resolveEgoDepletionGrant(domain)` says how many temporary points a held ability owes on the
following Rodada the first time in a session that domain is reduced to zero
(`GnoseAbility.ESTABILIDADE_EMOCIONAL`, +1 Autocontrole). It resolves inside
`AbstractCombatantSheet#spendEgoPoints`, the one funnel both a deliberate use and `Primor`'s drain
pass through — because "for reduzido a zero" doesn't care which emptied the pool, deliberately
unlike `useEgoPointsForEffect`, which exists precisely to keep a drain from paying a Vantagem. A
pool emptied some other way (a `TemporaryEgoPenalty` landing) does **not** trigger it: nothing
does that today, and `applyEffect` has no equivalent hook.

**Recursos buys PE (0.0.77).** `ego.SocialClass` is the Recursos table, one row per value 0–5, picked by
the Recursos still held **permanently** (`SocialClass.current(sheet)`) — the book's "valor total". A temporary
spend never moves the row; a permanent one lowers it for every later point.
`EgoPointsService#spendResourcesForEquipmentPoints` spends through `useEgoPointsForEffect` and prices each
point at the row it left from (plus `Feat#resolveResourcesPointValueBonus`, Saber Investir's +2), paying into
the sheet's PE wallet. Starting PE are `CharacterCreationService#grantStartingEquipmentPoints` (once, never on a
rebuilt sheet); the starting store is `#getStartingStore`. **Recursos is never a session pick** —
`applySessionRecovery` throws `RESOURCES_NOT_RECOVERED_BY_SESSION`; wages, loot and rewards are
`grantTemporaryByNarrator`. The Utilidades e Serviços columns are reference data, never enforced.

**Sorte on a roll (0.0.78).** A Ponto de Sorte's roll effects are `ego.SorteEffect` marks on the `SkillRoll`
(`withSorte`, `rerolledWithSorte`), each costing the point type it names — a temporary point can never buy a
permanent effect. `EgoPointsService#applySorte`/`#rerollWithSorte` pay the point (through `useEgoPointsForEffect`)
and return the marked roll; the caller resolves it again, since every one is decided after seeing the dice.
`AbstractSkillInteraction` applies the Vantagem, the eased GD and the chosen success (a Crítico Menor);
`AttackDelivery`/`AttackReceiver` apply the chosen hit/defence and the unleashed Corrente + Maior-severity Efeitos
Críticos. A rule that isn't dice (a Defeito's automatic failure, an immunity, Trava Mental, Frenesi's suppression)
still wins.

**Iniciativa (0.0.79).** `InitiativeEgoService` pays and applies every Iniciativa spend. The order changes are
an **override** on the sheet (`CombatantSheet#overrideInitiative(value, rodadas)`, `null` = the Cena), which
`InitiativeEntry#getEffectiveInitiativeValue` returns instead of the rolled value + `INITIATIVE` bonuses — not a
`TemporaryBonus`, whose Turn-end countdown can lapse before the Rodada boundary where the Scene re-sorts. `Scene`
advances each override right before that re-sort, so a 1-Rodada override governs exactly the next Rodada; a new
Cena drops it. The "até duas rolagens na mesma Cena" effects are sheet charges (`ego.InitiativeRollCharge`, 2 per
point, dropped with the Cena), spent one per roll by `useRollCharge`, which marks the `SkillRoll`.

**Autocontrole (0.0.81).** `AutocontroleEgoService` pays each spend. What reacts to an incoming attack is a mark
on the **defence roll** (`ego.AutocontroleDefence`), read by `AttackReceiver`: the avoided Corrente/critical doesn't
land and isn't reported as triggered, and the permanent marks grant **Cena immunity** — a per-sheet set keyed by
`sheet.CenaImmunity#kindOf` (Condição type / Efeito Crítico type / effect class), refused by `applyEffect` and
`applyCondition` and dropped by `startNewScene()`. "Zero this Rodada's damage" is `negateDamageThisRound`: the sheet
counts PV lost through `applyDamage` per Rodada, gives them back, and takes none until the boundary.

**Ego at zero (0.0.82).** When an Ego's **permanent** points reach 0 (having had some), it owes a 1d6 on its
table (`ego.EgoSetback`; Recursos has none) — `CombatantSheet#getOwedEgoSetbacks()`, recorded by
`EgoSetbackService#rollSetback` with the caller's die. A setback holds only while the permanent points are 0
(`getEgoSetback` drops it lazily once one comes back), and each entry is read where it applies: the three action
economy services, `MovementServiceImpl`, `EffectChainService#getRequiredMargin`, `InitiativeEntry`, the Título
activation gate, `heldConditions()` (the derived Apavorado/Desprevenido/Imobilizado), `getBlindCheckThreshold`,
`AbstractSkillInteraction`/`AttackDelivery`/`AttackReceiver`.

**A PdN's Efeitos de Ego (0.0.83).** Every Ego service pays through `EgoPointsService#payForEffect`. A player spends;
a PdN (`CombatantSheet#isPdn` — every `MonsterSheet`, or a sheet `markAsExemplarPdn`) spends nothing and records the
point owed to every PJ (`recordPdnEgoUse`): drained at once for a temporary effect, at the end of the Cena for a
permanent one, and handed over with `grantPdnCompensation`. A Regular `MonsterSheet` is refused — only Exemplares
use Efeitos de Ego (table ruling) — and so is any PdN not `isIntelligent()` ("quando inteligentes", core 0.0.84).

## Reference files to read first

- `src/main/java/org/aventyrs/core/sheet/EgoPointPool.java` — the four equations.
- `src/main/java/org/aventyrs/core/sheet/AbstractCombatantSheet.java`
  (`spendEgoPoints`, `getAvailableEgoPoints`, `receiveTemporaryEgoPoints`, `egoPool`, `sumEgoPenalty`).
- `src/main/java/org/aventyrs/core/character/services/EgoPointsService.java` /
  `EgoPointsServiceImpl.java` — `useEgoPointsForEffect`, `applySessionRecovery`.
- `src/main/java/org/aventyrs/core/sheet/EgoPointSpend.java` / `EgoPointType.java` /
  `TemporaryEgoPenalty.java` / `PendingEgoRecovery.java` / `DelayedEgoGrant.java`.
- `src/test/java/org/aventyrs/core/**/EgoPointFeatureTest.java` — pins both spend orderings.
- `src/test/java/org/aventyrs/core/sheet/EstabilidadeEmocionalFeatureTest.java` — the
  depletion trigger, the once-per-session guard and the next-Rodada delivery, end to end.
