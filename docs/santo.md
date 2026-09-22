# Santo — how each Interaction works

A caller's guide to activating Santo's traits: what to put on the request, what comes back, and
**what you still have to do afterwards**. Authored against V19 (`docs/rules/titulos.txt`, lines
242–362) as of 0.0.46.

> The authoritative API contract is the javadoc on each class and on
> `org.aventyrs.core.title.package-info.java` — this file is a map, not a second source of truth.
> If they disagree, the javadoc wins and this file is stale.

---

## The one thing to internalise

**Most of these report; they do not apply.** This core computes; a caller decides who and when.
Three traits act directly on the activator, three hand you a value object you must do something
with, and the rest fall in between. The "Caller must" column below is the part that bites.

An activation that is *refused* costs nothing — every gate runs before anything is spent.

---

## Activating anything

```java
InteractionResult result = title.activateAbility(ability, TitleAbilityActivationRequest.builder()
        .activator(sheet)          // always required
        .determinationPoints(2)    // required for a Variável cost, ignored for a Fixed one
        .target(allySheet)         // only where the table says so
        .sceneContext(context)     // only where the table says so
        .scene(scene)              // only where the table says so
        .choice(SomeEnum.VALUE)    // only where the table says so
        .build());
```

`AventyrTitle#activateAbility` checks the trait is actually held, then finds the Interaction class
through the catalogue constant's own `interactionClass` column and instantiates it reflectively —
which is why every Interaction has a public no-argument constructor. Building the Interaction
yourself and calling `applyTo`/`activate` skips only the held-check.

`Santo` offers three thin delegates as sugar — `activateAbencoadoPelaLuz`,
`activateGritoDeGuerraVulcano`, `activateOrgulhoElduriano`. Everything else goes through
`activateAbility` directly.

### What `activate` does, in order

1. **Silêncio** — refuses if the activator can't activate abilities at all
2. **PD amount** — checked against the trait's `PDCost` (`INVALID_PD_AMOUNT`)
3. **PD affordability** (`NOT_ENOUGH_DETERMINATION_POINTS`)
4. **PV cost**, where the trait has one — refused if paying would drop the activator to 0 or below
   (`NOT_ENOUGH_HIT_POINTS`); paying to exactly 1 PV is allowed
5. the trait's own **`validate`** — scene, target, choice, equipment
6. **spend** PD, then PV (PV comes off as plain damage: no RD/RA/Meio-Dano applies to a
   self-inflicted cost)
7. **record** the activation
8. **resolve** the effect

**Pontos de Ação are reported, never deducted.** Nothing in this core keeps a spent-this-Turn PA
pool, so the Tempo de Ativação column is information for your UI, not a charge.

---

## The twelve at a glance

| Trait | PD | PA | Request needs | Caller must |
| --- | --- | --- | --- | --- |
| **Abençoado pela Luz** *(Esp.)* | — (3PV) | 2 | `choice(Branch)`, optional `target` | nothing — the heal is applied |
| **Fúria dos Deuses** *(Esp.)* | — (1/3/5PV) | — | `choice(Tier)` | fold `empoweredAttack` into the attack |
| Proteção Ungida | **2 or 3** | 2 | `determinationPoints` | nothing |
| Bastião dos Necessitados | passive | — | — | — *(not activatable)* |
| Guarda Vidas | 3 | Reação | `target`, `sceneContext` | build the attack against `redirectedAttackTarget` |
| Protetor da Vida e da Morte | passive | — | — | — *(not activatable)* |
| Orgulho Elduriano | **2+** | 3 | `scene`, optional `sceneContext` | `refreshAura` after movement/teleport; `recordAttack` |
| Grito de Guerra Vulcano | 2 | 1 | — | grant the 3 `blessings` to self + adjacent allies |
| Corpo Indestrutível de Epona | 4 | 1 | — | nothing |
| Glória Relampejante de Tesla | 3 | Ação Livre | — | grant the 1 `blessing` to self + allies at Curta |
| Sacrifício Ymiriano | — (Vigor PV) | 1 | — | nothing |
| Espinhos Venenos de Gaea | — (Vigor PV) | 2 | — | deal the reported `retaliation` to attackers |
| Placidez de Undine, Rancor de Haloi | 2 | 3 | — | fold `empoweredAttack` into the attack |
| Furor de Sylph | 2 (+Vigor PV) | Ação Livre | — | `consumeEnhancedAttack` per attack |

---

## The four carriers

Everything reported comes back in one of these shapes.

### `Blessing` → you choose the recipients

A `TargetScope.SELF_AND_ALLIES` Blessing means *this core cannot resolve who your allies are*.
Read `result.getBlessings()`, work out the recipients yourself, and call `grantBlessing` on each.
The Interaction publishes the radius as a constant (`GloriaRelampejanteDeTeslaInteraction.ALLY_RANGE`).

```java
List<CombatantSheet> recipients = new ArrayList<>(context.getAlliesWithin(ALLY_RANGE));
recipients.add(actor);
result.getBlessings().forEach(b -> recipients.forEach(r -> r.grantBlessing(b)));
```

A Blessing from the same source **replaces** its predecessor and renews the Duração — it does not
stack. That is deliberate, and is how re-activating works throughout.

### `EmpoweredAttack` → you fold it into the attack you build next

`title.EmpoweredAttack` is what *one* attack gains from a trait activated just before it. The
activation always resolves **before** the attack exists, so nothing has to reach into a resolution
already in flight — that ordering is the whole trick, and it is why `AttackDelivery` needed no
change to support any of this.

| Field | What you do with it |
| --- | --- |
| `attackRollBonus()` | add to the attack roll's total |
| `difficultyReduction()` | **níveis**, not a number — `DifficultyLevel#easier(n)` |
| `extraDamageDice()` | roll that many further d6 yourself — this core never rolls |
| `extraDamageFlat()` | add to the dano roll |
| `rangeIncrease()` | widen the reach by that many `Range` bands |
| `criticalMarginIncrease()` | **lower** the Margem Crítica Menor by it — lower is wider |
| `lifeSteal()` / `determinationSteal()` | apply on a hit; Determinação has no mechanism yet |
| `additionalCriticalEffect()` | add to the attack's Efeitos Críticos |

Every field is inert at `0`/`null`, so a producer sets only its own. **Nothing inside this core
consumes an `EmpoweredAttack`.**

### `Enchantment` → the recipient decides, not you

`sheet.Enchantment` marks a `TemporaryEffect` as an **Efeito de Encantamento**. A caster builds one
and offers it through `CombatantSheet#applyEnchantment`, which returns whether it took hold:

```java
boolean landed = target.applyEnchantment(new ForcedTargeting(santo, 2));
```

That one door is where the tag does all its work, and **every decision in it is a fact about the
recipient**:

| Decided by | Reads | Effect |
|---|---|---|
| Immunity | `Character#isImmuneToEnchantments()` | refused outright, returns `false` |
| Duração | the recipient's own `Ungido#blessesBoth()` | a **harmful** one lands at half Duração, rounded up |

Immunity is Fada/Fúria/Górgona's "imunes aos efeitos **diretos**"; this door *is* "direto", so the
indirect half of that clause needs nothing — an enchanted weapon's damage never arrives here.
`GorgonaFeat#MARCA_DA_MALDICAO` strips the immunity, which is why you read
`Character#isImmuneToEnchantments()` and never `Race` directly.

### `Retaliation` → you deal it back yourself

`combat.Retaliation` appears on `DeliveredAttackResult`/`IncomingAttackResult`, not on an
activation result: it is what the *defender's* thorns owe whoever just attacked them.

```java
Retaliation r = attackResult.getRetaliation();
if (r != null) {
    damageService.applyDamage(attacker, ctx, r.descriptor(), defender, r.damage(), false);
    if (attackDealtDamage) {                       // only this half is conditional
        attacker.applyCondition(new Condition(r.conditionOnDamage(), r.conditionRounds(), null));
    }
}
```

Applying it as ordinary damage is correct, not a workaround: the attacker's own RD/RA *should*
judge thorns like any other incoming hit.

---

## Each Interaction

### Abençoado pela Luz *(Especialização)* — `AbencoadoPelaLuzInteraction`

> Seu toque tem capacidades curativas… recupere 3+ Quantidade de Habilidades de Abençoado pela Luz
> PV, ou Remover um Malefício.

**3PV, 2PA.** Priced in PV, not PD — its `PDCost` is genuinely 0.

Requires `choice(Branch.HEAL)` or `choice(Branch.REMOVE_MALEFICIO)`; there is no safe default
between a heal and a cure, so an activation naming neither is refused (`TITLE_ABILITY_CHOICE_REQUIRED`).
`target` is the touched character, defaulting to the activator.

`HEAL` **applies** the heal and reports it as `resourceGainValue`/`resourceGainType`. The amount
counts the *toucher's* own Abençoado pela Luz Habilidades, not the target's — it is the Santo's
mastery that makes the touch stronger — so the activator must really hold the Título, not merely
have a `Santo` object nearby.

> **TODO** `REMOVE_MALEFICIO` is inert. V19 widened it to four kinds (Doença, Encantamento,
> Maldição, Veneno), but "Encantamento" is not a `ConditionType` at all — it is
> `MagicType.ENCANTAMENTO`, and nothing tracks which Magias affect a combatant. There is also no
> request field for *which* Malefício to remove.

### Fúria dos Deuses *(Especialização)* — `FuriaDosDeusesInteraction`

> Sempre que realizar um ataque você pode pagar o Custo de 1PV, 3PV ou 5PV.

**No PD, no PA** — it rides on an attack you were making anyway. Requires `choice(Tier)`.

| Tier | PV | Buys |
| --- | --- | --- |
| `VANTAGEM` | 1 | Vantagem on the attack roll (+2) |
| `REDUCAO_DE_GD` | 3 | GD −1 nível |
| `FURIA_MAIOR` | 5 | GD −1 nível **and** +1d6+Vigor of dano |

The top tier is cumulative with the middle one ("adicionalmente à redução de GD"). The 1PV tier is
a *different, cheaper* effect, not an addition — the text never combines it with the others.

Returns an `EmpoweredAttack`. Note a nearly-dead Santo may afford `VANTAGEM` but not `FURIA_MAIOR`.

### Proteção Ungida — `ProtecaoUngidaInteraction`

> Abençoa um item do tipo Armadura ou Escudo… todo o dano que seria causado a você é reduzido à
> metade… ao custo de 3PA e 3PD, ungir uma Armadura e Escudo simultaneamente.

**2PD (one kind) or 3PD (both), 2PA/3PA.** The cost is `variable(2)`, so **you must name the
amount** — `applyTo(sheet)` alone will not work. That is the deliberate guard for a two-mode
ability: there is no safe default between blessing one item and two.

Gates: you must be using an Armadura or Escudo (`TITLE_ABILITY_REQUIRES_ARMOR_OR_SHIELD`); 3PD
additionally requires holding **any** Santo Suprema (`INVALID_PD_AMOUNT`) and actually wearing
both; more than 3PD is refused.

Applies a `HALF_DAMAGE` Blessing for 3 Rodadas, plus a `sheet.Ungido` recording which
`ItemCategory`s are blessed — read `getUngido().blessesBoth()`. **Kinds, not copies**: which
particular Armadura was blessed is not recorded and does not matter.

> **TODO** the Duração-halving of Encantamentos/Maldições that `blessesBoth()` unlocks needs an
> Encantamento/Maldição classification and a public duration mutator; neither exists.

### Guarda Vidas — `GuardaVidasInteraction`

> Você pode se teletransportar para a frente de um aliado em Distância Curta, se tornando o alvo
> do ataque em seu lugar.

**3PD, Reação.** Requires `sceneContext` and a `target` that is a real ally, not the activator
(`getEffectiveTarget()` is deliberately *not* used — "um aliado" is somebody else), within the
teleport's reach (`TELEPORT_TARGET_OUT_OF_RANGE`).

The interception works purely by **ordering**: list the Reação with `ReactionOptionsService`,
activate it, then build the attack naming `result.getRedirectedAttackTarget()` as defender. The
orchestrators resolve an ordinary attack and never learn a Reação happened. "O ataque ainda deve
superar as suas Defesas" holds by construction — they are the Santo's.

> **TODO** nothing counts a Reação as *spent*, so a Santo who already reacted this Rodada is still
> offered this one.

### Orgulho Elduriano — `OrgulhoEldurianoInteraction`

> Todos os inimigos em Distância Curta são obrigados a desferir o primeiro ataque… em você…
> os danos causados serão reduzidos à metade. Esta Habilidade é um Efeito de Encantamento.

**2PD for the first foe, +1PD each additional; 3PA.** Requires `scene`
(`TITLE_ABILITY_REQUIRES_SCENE`). **The PD buy foes, not Rodadas** — the Duração is a flat 2.

**The Aura is only an emitter.** It casts, per foe it catches, a `sheet.ForcedTargeting` — an
`sheet.Enchantment` that lives on **that enemy's own sheet**. So the Interaction builds the effect
and nothing else; what actually lands is the recipient's business:

- a **Fada, Fúria or Górgona takes nothing** (`Race#isImmuneToEnchantments`), and costs the Aura
  none of its budget — it never caught them
- an enemy warded by an **Armadura and an Escudo Ungido** takes it at **half Duração**
- each caught foe counts down **their own** Duração from when they were caught, so a foe entering
  on the last Rodada still gets a full one

Two caller duties:

- **`scene.refreshAura(holder, holderContext)` after any movement or teleport** — the holder's or
  a foe's, since distance is mutual. This core does no geometry, so this is the moment foes
  entering the radius are caught. Passing `sceneContext` on the request catches those already in
  range immediately.
- **`scene.recordAttack(attacker, defender)`** beside `recordAction` — a `CombatantAction` names
  no target, and it is what discharges the compulsion for the Rodada.

Thereafter `AttackDelivery`/`AttackReceiver` refuse a bound attacker's first attack of the Rodada
against anyone else (`FORCED_ATTACK_TARGET_REQUIRED`) — pass `forcedTargetUnavailable(true)` when
the Santo genuinely isn't a valid target, which this core cannot judge — and report
`auraHalvesDamage` on its later attacks, applying the Meio-Dano for you. Both now read the
attacker's own `getForcedTargeting()`, not the Scene's Aura list.

A bound foe **keeps the compulsion if they leave the radius** — it is theirs now. The one thing
that ends it early is the Santo leaving the Scene: `removeParticipant` lifts every compulsion they
cast, since there is nobody left to be provoked by.

> **TODO** "sempre afeta os alvos em sua Área de Efeito" — the Aura is still not classified as an
> Área de Efeito for `EVASAO`-style clauses.

### Grito de Guerra Vulcano — `GritoDeGuerraVulcanoInteraction`

**2PD, 1PA.** Reports **three** Blessings at `TargetScope.SELF_AND_ALLIES` for 2 Rodadas:
Defesas +3, and Vantagem on both attack Perícias. Recipients are self + `Range.ADJACENTE`.

### Corpo Indestrutível de Epona — `CorpoIndestrutivelDeEponaInteraction`

> O primeiro ataque que lhe causaria Danos é reduzido à zero, após isso você recebe RDS 5. A
> Redução de Danos Sofridos é reduzida em -2 para cada dano sofrido.

**4PD, 1PA, 1 Rodada.** Applies a `sheet.PeleDePedra`; nothing for the caller to do.

`DamageServiceImpl` consults it **by type** — the one place it reads a concrete effect rather than
summing a `ModifierType`, because neither half is a stat. It is read **last**, after RD/RA/Meio-Dano,
so a blow those already turned aside never spends the negation. The sequence for 10 raw damage is
`0 → 5 → 7 → 9 → 10 → 10`.

> ⚠️ `calculateFinalDamage` **spends** the stone, so it is not a pure query while one is held.
> Deliberate: `DamageInteraction` splits mitigation from application, and a hook in the latter
> would miss every real attack.

### Glória Relampejante de Tesla — `GloriaRelampejanteDeTeslaInteraction`

**3PD, Ação Livre.** Reports **one** Blessing: `ACTION_POINTS` +2 for 1 Rodada at
`SELF_AND_ALLIES`, recipients being self + `ALLY_RANGE` (*Curta*, not adjacency).

The PA lands only through `ActionPointsService`'s `CombatantSheet` overloads — a caller reading PA
through the `Character`-only overload will not see it.

> V19 dropped this clause's RA half; it grants no Redução Absoluta.

### Sacrifício Ymiriano — `SacrificioYmirianoInteraction`

**Vigor in PV, 1PA, 1 Rodada.** Applies two SELF Blessings: Força **= Vigor**, and Categoria de
Tamanho **+2**. The same figure is both price and benefit.

**Reach:** an `<ATTR>_BONUS` bonus is read on the Perícia-roll path only, so the Força reaches a
Força-governed roll and nothing else — not PV/PM/PD, and not the melee ½-Força dano term.

> **TODO** "Margem Crítica Menor +2 contra inimigos que tenham infligido danos aos seus aliados" —
> nothing tracks who has harmed your group.

### Espinhos Venenos de Gaea — `EspinhosVenenosDeGaeaInteraction`

**Vigor in PV, 2PA, 2 Rodadas.** Applies a `RETALIATION_DAMAGE` Blessing worth the holder's Vigor.

From then on, any **Ataque Corpo-a-Corpo** resolved against them reports a `Retaliation` — see the
carrier section above for how to deal it. Melee only, judged by the Perícia; an Ataque à Distância
provokes nothing.

**Reported whether or not the attack landed** — "atacarem", not "acertarem". Only the Veneno half
is conditional on damage actually being dealt.

`ConditionType.ENVENENADO` is real: `LIFE_MULTIPLIER` −1, so a poisoned character's **maximum PV
falls**. Current PV does not re-scale, so they can drop a `CharacterStatus` tier and climb back
when it lapses.

> ⚠️ The condition catalogue says "Multiplicador de **Bônus Base**"; this clause says
> "Multiplicador de **Pontos de Vida**". The concrete clause was taken as authoritative — worth
> confirming against the core rulebook.

### Placidez de Undine, Rancor de Haloi — `PlacidezDeUndineRancorDeHaloiInteraction`

**2PD, 3PA.** No PV cost, unlike its three siblings. Returns an `EmpoweredAttack` carrying Margem
Crítica +2, Roubo de Vida 3, Roubo de Determinação 2, and `OFERENDA_MALDITA` as an additional
Efeito Crítico.

Reported rather than applied on purpose: `sheet.LifeSteal` counts down in *Rodadas* and would keep
stealing all Rodada, and Margem Crítica Menor has no `ModifierType` at all.

> **TODO** Roubo de Determinação has no mechanism. The per-target "imunes a ela por 2 Rodadas"
> window has no Rodada-scoped ledger (`markAffectedUntilRest` is until-Rest).

### Furor de Sylph — `FurorDeSylphInteraction`

> Você recebe Bônus de +1PA… O Furor de Sylph aprimora uma quantidade de ataques igual à 1+ metade
> dos PV gastos.

**2PD + Vigor in PV, Ação Livre.** Grants a **budget of attacks**, not a timed effect — the clause
names no Duração at all.

```java
if (sheet.consumeEnhancedAttack(AbracadoPelaEscuridaoAbility.FUROR_DE_SYLPH)) {
    // this attack is enhanced
}
```

The **+1PA is scanned off that budget**, not granted: it lasts exactly while attacks remain and
needs no revoke. Nothing consumes a charge automatically, and nothing clears the budget at a
Rodada or Turn boundary. Re-activating **replaces** the remaining budget rather than banking onto it.

> **TODO** none of the per-attack half is real: *Alcance Estendido* is not in the Obra-Prima
> catalogue; the 1UD push and Reposicionar need geometry; and the +1d6 belongs on an attack this
> activation — funding several later ones — is not adjacent to.

---

## The two that are not activatable

Both are `Custo de Ativação: Nenhum, habilidade passiva` and correctly report no Interaction.

**Bastião dos Necessitados** is resolved by scanning, never granted. Adjacent allies on lower PV
receive RDS `1 + ½(Habilidades de Santo)`; the Santo receives half that while protecting at least
one. **RDS is RD**, not RA. Both halves need a `SceneContext`, so they are visible only through
`getTotalDamageReduction(target, damageType, source, sceneContext)` or `calculateFinalDamage` —
the sheet-and-type overloads pass `null` and never see them.

**Protetor da Vida e da Morte** is entirely TODO: no floor-at-1PV concept, no "redirect an ally's
damage to yourself" mechanism, and no locked-PV subtype.

---

## Pitfalls

- **A Variável cost makes the PD amount mandatory.** Affects Proteção Ungida and Orgulho Elduriano;
  `applyTo(sheet)` throws `INVALID_PD_AMOUNT` for both.
- **A PV cost is refused at exactly its own value.** Paying may never bring the activator to 0, so
  a Santo on 3 PV cannot pay 3.
- **PA are never deducted.** Don't expect a pool.
- **`SELF_AND_ALLIES` means you resolve the recipients.** A reported Blessing that nobody grants
  does nothing.
- **An Encantamento can simply not land.** `applyEnchantment` returns `false` for an immune
  recipient — check it rather than assuming the effect is there.
- **Several traits need the Título really granted to the Character**, not just a `Santo` object in
  a local — anything counting the holder's own Habilidades (Abençoado pela Luz's heal, Bastião's
  RDS) reads `getAllTitles()`.
- **"Descansos Verdadeiros ou Roubo de Vida" is unenforced everywhere.** No locked-PV subtype
  exists, so a plain heal restores PV that the rules say it should not.
