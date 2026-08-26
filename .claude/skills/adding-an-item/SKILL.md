---
name: adding-an-item
description: This skill should be used when the user asks to "add a new Item", "add a new Equipamento", "add [item name] to ArmorItem", "add a new item category/enum", "model an item's Favor", or gives the rules-text block for a piece of Equipamento (its Peso/Raridade heading, Preço, DF/DM, Dureza, Conjuração, Favor and Requisitos columns). Walks through turning that block into a catalog constant with real, typed bonuses — mirroring org.aventyrs.core.item.ArmorItem as the reference implementation.
---

# Adding an Item (Equipamento)

An `Item` is the **catalog entry** for a piece of Equipamento — what "an Armadura Completa"
*is*, the same way `Feat` describes a Talento. It carries every column the item's rules-text
block lists, and its `ItemFavor` carries that block's conditional half as real
`ModifierType`-typed data rather than prose.

## 0. The one architectural fact to hold onto

**Catalog, not owned copy.** An item's stats are identical for every copy, so the enum constant
*is* the item. This is the same catalog-vs-instance split `AventyrTitle`'s javadoc documents,
resolved the *opposite* way from a Título's — a Título is a per-character held instance because
its specializations are per-acquisition data; an item has no such per-acquisition data.

Per-copy state is deliberately unmodeled: **Dureza actually remaining, Obra-Prima tier,
Aprimoramentos, and who produced it** would need a separate held-instance type wrapping a
catalog entry. Don't build it speculatively — several TODOs cite it
(`ProfissaoCompetencyAbility`, `ResourcesAdvantage#HERANCA_FAMILIAR`), but none is unblocked by
the catalog alone.

**Inventory, however, is real** — `Character#equipment` (worn/wielded) and
`AbstractCombatantSheet#inventory` (carried, including a foe's loot). Both are mutable
`List<Item>` with plain mutators, the same shape as `Character#feats`. This matters in step 4:
two scanning services already reach equipped items on their own.

## 1. Read the rules text first

Get the actual block before modeling anything. Every item presents the same columns, and each
maps to exactly one field:

| Rules-text column | Field | Notes |
| --- | --- | --- |
| Heading "(Pesado/Raro)" | `ItemWeightClass` + `ItemRarity` | the pair in the item's title |
| Descrição | `description` | flavour/usage text, **never** mechanics |
| Preço | `price` | in Pontos de Equipamento (PE) |
| DF / DM | `physicalDefenseBonus` / `magicDefenseBonus` | |
| Dureza | `hardness` | the *pristine* value, not a per-copy remaining one |
| Conjuração | `castingBonus` | see step 3 |
| Favor + Requisitos | `ItemFavor` | see step 2 — `null` when "Favor: Nenhum" |

Don't invent a column the block doesn't state. "Favor: Nenhum" is exactly a `null` `ItemFavor`
— the item grants nothing conditional, so there is no Requisito either (the two always travel
together: Requisitos exist *only* to gate a Favor). `Item#grantsFavorTo` is then permanently
`false` and `resolveFavorBonus` permanently 0, with no null-checking needed at any call site.

## 2. Model the Favor as data, not prose

`ItemFavor` (`@Builder`) carries a `description`, an `ItemRequirements`, a list of `ItemBonus`
(a `ModifierType` + `int value` record), and an optional free-text `additionalEffects`.

```java
ItemFavor.builder()
        .description("Dano de Corte sofrido é reduzido em -2.")
        .requirements(new ItemRequirements(AttributeDomain.STRENGTH, 3))
        .bonus(new ItemBonus(ModifierType.DAMAGE_REDUCTION, 2))
        .build()
```

The rules text stays on `description` *alongside* the typed bonus — the same
single-source-of-truth convention every ability enum follows.

**It's data, not `@Modifier` methods, and that's forced.** `@Modifier`'s `ModifierType` is a
compile-time-fixed annotation value, so one shared `ItemFavor` class could never vary which type
a given item grants — the identical limitation CLAUDE.md's "A ModifierType per skill" documents.
**Don't try to route items through `ModifierResolver`.**

**`ItemBonus` is deliberately not `TemporaryBonus` or `Blessing`.** An item's Favor lasts as
long as the item is carried (no Rodada countdown) and never reaches anyone but its wielder (no
`TargetScope`, no granting `source`) — all three of those fields would be dead weight. If you
came here from `granting-a-blessing`, this is the branch where that skill does *not* apply.

### Requirements check `getTotal()`, not `getBase()`

`ItemRequirements(AttributeDomain, int)` is deliberately unlike `FeatRequirements`, which uses
`base`: acquiring a Talento is gated on what the character personally invested in, but whether
an item's Favor applies is a **"can I meet this right now"** question, so a Bônus Racial or a
variable bonus counts.

It's a narrower record than `FeatRequirements` (no `requiredSkillType`/`requiredFeat`) rather
than a reuse of it — widen it only if a real item ever names a Perícia/Talento/Título.

**A requirement isn't always Força** — `ROBE_CERIMONIAL`/`ROBE_DE_GUERRA` are gated on Gnose and
`ROUPA_PESADA` on Destreza. Any `AttributeDomain` is fair game.

### When a Favor clause has no `ModifierType` to express it

Contribute **no** `ItemBonus` and leave the clause living in `getDescription()` until its
mechanism exists. Two distinct reasons this happens, and they are not the same:

- **No reader for the concept.** `ARMADURA_COMPLETA`'s "de Corte" scoping — `DamageType` has no
  Corte/Perfuração/Impacto breakdown and RD/RA resolve with no notion of damage type. Modeled as
  plain RD, with the simplification documented on the constant rather than silently narrowed or
  over-granted.
- **A shape `ItemBonus` can't hold, even though the stat has a `ModifierType`.**
  `ARMADURA_DE_JUSTA`'s "Movimento Base reduzido à metade" is a *halving*, and `MovementService`
  sums `MOVEMENT` additively with no multiplicative stage to feed (unlike `DamageService`'s real
  `HALF_DAMAGE`). **Don't add a `MOVEMENT_HALVED` constant** — the missing piece is the
  mechanism, not a reader.

**Check the net effect before assuming a split is needed.** `ROUPA_PESADA` is the trap: its text
reads at first glance like it needs DF and DM as two separate comparable stats ("+1 na Defesa
faltante entre DF ou DM", plus an Efeito Adicional filling in whichever the base clause didn't).
But the two clauses combined always net out to an unconditional +1 DF *and* +1 DM regardless of
the per-copy production choice — so despite `ModifierType.DEFESAS` being a single
undifferentiated constant, it's granted for real, as one combined `DEFESAS` bonus of **2**, with
no "Efeitos Adicionais" line at all.

### Efeitos Adicionais

Granted by the *same* requirement, not independently — which is why it lives on `ItemFavor`
rather than on `Item`. `null` for an item with none; check via `hasAdditionalEffects()`. It
stays free text: what one does varies too widely per item to have a shared shape yet. **No
cataloged item currently has one.**

Rule of thumb: everything on `Item` applies to anyone carrying it; everything on `ItemFavor`
needs `Item#grantsFavorTo(Character)` to hold first.

## 3. Conjuração, and "Desvantagem" as a number

`castingBonus` is a plain number. An item whose Conjuração column reads **"Desvantagem"** carries
`Skill.DISADVANTAGE_MALUS` (**-2**) — the exact mirror of this codebase's "Vantagem is a flat +2"
convention. One reading "Vantagem" would carry `Skill.ADVANTAGE_BONUS`. A column reading **"-"**
is **0**: the item neither helps nor hinders conjuração.

`ArmorItem.ARMADURA_COMPLETA` is the first real, unconditional Desvantagem in the ruleset and
what motivated adding the constant. Note that the *scoped* Desvantagem clauses elsewhere
(`race/Bestial.java`'s Inocência Selvagem, `AbencoadoPelaLuzAbility`) are still blocked on their
own separate gaps — this core doesn't track what a roll is *for* — not on this constant.

## 4. Know which columns actually reach a consumer

Two do, and a new item's values flow into them automatically with no wiring:

- **The Favor's `DAMAGE_REDUCTION`** — `DamageServiceImpl` iterates `character.getEquipment()`
  and calls `item.resolveFavorBonus(ModifierType.DAMAGE_REDUCTION, character)`, landing on the
  RD that `getTotalDamageReduction` already sums for real.
- **DF/DM** — `DefenseServiceImpl.sumEquipment` walks the same list per `DefenseType`.

Three still have **no consumer**, each blocked on a *different* missing system: **Preço** (PE has
no budget/economy), **Dureza** (no damage/repair mechanic), **Conjuração** (no item-granted hook
on either of `SpellCastingService`'s two rolls). Their values are still real, exact data — per
this codebase's "can't apply it yet doesn't mean can't compute it yet" discipline. Author them
correctly; just don't claim they do something.

`ItemInteraction` remains a bare "TODO implement" stub — nothing yet *uses* an item as an
`Interaction`. Adding an item does not change that.

## 5. Pick the right home for the constant

**One enum per `ItemCategory`** (e.g. `ArmorItem` for `ItemCategory.ARMOR`), mirroring
`<Skill>CompetencyAbility`/`ArtesMarciaisFeat`'s one-enum-per-domain shape. The enum implements
`Item`, holds a `private final` field per column, takes them all through its constructor, and
overrides `getCategory()` to return its one fixed category. Lombok's `@Getter` covers the rest.

If the category has no enum yet, create `<Category>Item` alongside `ArmorItem` and follow its
layout exactly. `ItemCategory` already enumerates the full set (ARMOR, BOOTS, CLOAK, GLOVES,
HELMET, RING, SHIELD, BOW, THROWABLE, CROSSBOW, WHIP, CLUB, LIGHT_BLADE, HEAVY_BLADE, SPEAR,
PROJECTILE, POTION, SCROLL), each carrying its `ItemType`
(Ofensivo/Defensivo/Utilitário/Consumível) — so `getType()` is derived, never authored.

**`AbstractItem` (`@Builder`) is the `AbstractFeat` equivalent** — use it for a one-off or
caller-supplied item that doesn't belong in a catalog enum, not for a cataloged one.

Put a javadoc block on each constant carrying its rules text, and use it to record any
simplification you made in step 2 (see `ARMADURA_DE_GLADIADOR`'s for the reference tone).

## 6. Write tests

Follow `ArmorItemTest`/`ItemFavorTest`'s existing shape — per-constant tests plus catalog-wide
sweeps:

- `<item>CarriesEveryColumnOfItsRulesText` — one per constant, asserting all eight columns.
- `<item>FavorGrants<Bonus>OnlyAt<Attribute><N>` — the requirement gate in both directions:
  below the threshold grants 0, at/above grants the real value.
- `<item>FavorGrantsNothingItDoesNotName` — a `ModifierType` the Favor doesn't carry reads 0.
- `<item>GrantsNo<X>ForIts<Clause>Clause` — pin each deliberate simplification from step 2, so a
  later reader can't mistake it for an oversight (see
  `armaduraDeJustaGrantsNoMovementBonusForItsHalvingClause`).
- `<item>HasNoAdditionalEffects` / `hasOneConstantPerCatalogedArmor` /
  `everyCatalogedFavorIsDescribedAndCarriesItsRequirements` /
  `everyCatalogedFavorGrantsAtLeastOneRealBonus` — catalog-wide sweeps; extend the count in the
  first when you add a constant.

## Reference files to read first

- `org.aventyrs.core.item.ArmorItem` — the reference catalog. `ARMADURA_COMPLETA` (full Favor +
  Desvantagem), `ARMADURA_DE_GLADIADOR` (`null` Favor, "-" Conjuração), `ARMADURA_DE_JUSTA`
  (unexpressible halving), `ROUPA_PESADA` (the net-effect trap).
- `org.aventyrs.core.item.Item` — the interface and its three `default` Favor helpers.
- `org.aventyrs.core.item.ItemFavor` / `ItemBonus` / `ItemRequirements`.
- `src/test/java/org/aventyrs/core/item/ArmorItemTest` — the test shape to copy.
