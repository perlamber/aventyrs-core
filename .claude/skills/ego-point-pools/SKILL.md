---
name: ego-point-pools
description: This skill should be used for any work on the two spendable Ego point pools per `EgoDomain` on a `CombatantSheet` — `EgoPointPool` (the four equations: permanentMax/permanentRemaining/temporaryCeiling/temporaryRemaining), `CombatantSheet#spendEgoPoints`/`getAvailableEgoPoints`, `EgoPointsService#useEgoPointsForEffect`/`applySessionRecovery`, `EgoPointSpend`/`EgoPointType`, `PendingEgoRecovery`, `TemporaryEgoPenalty`, `grantTemporaryEgoPointBonus`/`grantTemporaryEgoPoints`, `scheduleTemporaryEgoPointGrant`/`DelayedEgoGrant` (points owed on the next Rodada), `CombatantSheet#consumeOncePerSession`/`startNewSession` (the once-per-game-session guard, a transient per-sheet marker set), `AttributeAbility#resolveEgoDepletionGrant` (a clause reacting to an Ego being reduced to zero), or the `EgoAdvantage` hooks `resolveEgoSpendRecovery`/`resolveEgoSpendBlessings`/`resolveExtraSessionEgoRecovery`/`resolvePermanentEgoGain`. Also use it when asked why spending a permanent point "hurts twice", why there's a `spent` counter instead of a held balance, why `AUTOCONTROLE`/`RECURSOS`/`SORTE` aren't `ModifierType`s, or how `Primor`'s drain differs from a deliberate use, why a granted temporary point takes two steps, or what "a primeira vez em cada sessão de jogo" means here.
---

# Ego points are two pools per domain — `EgoPointPool`, `spendEgoPoints`

An `EgoDomain` isn't only a rating: it's **two spendable point pools** on a `CombatantSheet`,
permanent and temporary, both real currency. The whole model is four equations, all in
`org.aventyrs.core.sheet.EgoPointPool`:

```
permanentMax        = character.getEgos().getEgo(domain).getTotal()   // base + variable
permanentRemaining  = max(0, permanentMax - permanentSpent)
temporaryCeiling    = max(0, permanentRemaining + Σ bonus(source) - Σ activeEgoPenalty)
temporaryRemaining  = max(0, temporaryCeiling - temporarySpent)
availableEgoPoints  = permanentRemaining + temporaryRemaining
```

**Both pools start full**, and the temporary ceiling tracks permanent points *remaining* rather
than the permanent maximum — so **spending a permanent point hurts twice**. Sorte 3 with nothing
spent is 3 + 3 = 6 spendable; spend the 3 temporary then the 3 permanent and you get all 6, but
spend the 3 permanent *first* and the ceiling collapses to 0, so you only ever get 3. Nothing
special-cases that — it falls out of the arithmetic. `EgoPointFeatureTest` pins both directions.

- **Permanent points never recover.** Not on Rest, not per session, not anywhere. They're only
  *earned*, via `AttributeAbility#resolvePermanentEgoGain` → `CharacterEgos#withVariableBonus`.
  Nothing reduces them automatically either — only their holder spending one deliberately.
- **Temporary points recover one per game session** — one *total* across all four domains, not
  one apiece, with the player choosing which domain receives it. That's
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

**`grantTemporaryEgoPointBonus(domain, source, amount)` raises the ceiling, non-cumulatively per
source** — this is what `CharismaAbility.DESTINO_FAVORAVEL`'s "um ponto temporário, não
cumulativo" actually is. Repeat triggers from that same source don't widen it twice; an unrelated
source's grant still adds on top.

**Handing over a genuinely spendable point is `grantTemporaryEgoPoints(domain, source, amount)`,
and it is two steps.** It widens the ceiling *and then* recovers under it. Neither half works
alone on an emptied pool: with everything spent the ceiling is 0, so a bare
`recoverTemporaryEgoPoints` restores nothing, while a bare `grantTemporaryEgoPointBonus` is capped
per source and so grants nothing the second time that same source fires. Use it whenever rules
text says "você receberá N pontos temporários neste Ego"; use `recoverTemporaryEgoPoints` only for
"get back what you spent".

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

## Reference files to read first

- `src/main/java/org/aventyrs/core/sheet/EgoPointPool.java` — the four equations.
- `src/main/java/org/aventyrs/core/sheet/AbstractCombatantSheet.java`
  (`spendEgoPoints`, `getAvailableEgoPoints`, `grantTemporaryEgoPointBonus`, `sumEgoPenalty`).
- `src/main/java/org/aventyrs/core/character/services/EgoPointsService.java` /
  `EgoPointsServiceImpl.java` — `useEgoPointsForEffect`, `applySessionRecovery`.
- `src/main/java/org/aventyrs/core/sheet/EgoPointSpend.java` / `EgoPointType.java` /
  `TemporaryEgoPenalty.java` / `PendingEgoRecovery.java` / `DelayedEgoGrant.java`.
- `src/test/java/org/aventyrs/core/**/EgoPointFeatureTest.java` — pins both spend orderings.
- `src/test/java/org/aventyrs/core/sheet/EstabilidadeEmocionalFeatureTest.java` — the
  depletion trigger, the once-per-session guard and the next-Rodada delivery, end to end.
