# Defeitos e Qualidades — implementation plan

Source: `docs/rules/defeitos-e-qualidades.txt` (imported 2026-09-28 from *v19 - Defeitos e Qualidades
20260715*). 12 Defeitos × 3 gravidades (Leve / Moderado / Grave) and 12 Qualidades × 2 classes
(Menor / Maior). Target: core 0.0.72, then aventyrs-api, then the client — the same path the
Antecedentes took (0.0.71, `docs/wiring-antecedentes.md` in the client).

## Table rulings (2026-09-28)

- **No campaign switch.** Always available, entirely optional per character.
- **Sense-scoped clauses** (Deficiência Sensorial, Sentido Superior) apply to **every Atenção roll**
  and to no other Perícia.
- **"Regalia Verdadeira"** (Sonho de Gilgamesh) = wielding any Regalia, any `RegaliaGrade`.
- **Scope of this pass**: creation (Defeitos + Benefícios de Superação + Qualidades), the GM granting a
  Defeito mid-campaign (no Superação benefit), and Superar a Defeito (3 / 5 / 7 EXP).

## The rules in one screen

**Defeitos.** At creation, at most 3, **one per gravidade**; only those give a *Benefício de
Superação*. Mid-campaign Defeitos are unlimited and give none (the Narrador may grant one as a
reward — a GM action, not a rule). Superar costs Leve 3 / Moderado 5 / Grave 7 EXP, plus narrative
context and opportunity (the EXP is the only half core can check).

| Gravidade | Benefício de Superação — pick one |
| --- | --- |
| Leve | 1 Qualidade Menor · training in 1 more Perícia · +1 Graduação in a trained Perícia |
| Moderado | 1 Qualidade Maior · 2 Qualidades Menores · 1 Talento Geral (prerequisites apply) |
| Grave | 1 Maior + 1 Menor · 1 Talento *qualquer* + 1 Graduação or training · 1 more Habilidade de Atributo, de Ego or de Competência (prerequisites apply) |

**Qualidades.** Menor / Maior; a **Maior includes its Menor's effect** unless stated. At most **3
Qualidades** in total. Acquired only at creation: as a Benefício de Superação, or by trading
Talentos Gerais (1 → a Menor, 2 → a Maior). A Qualidade **opposing a held Defeito** can't be taken
(each Qualidade names its opposite Defeito). Qualidades can be switched off by temporary Malefícios
(curses).

**Sobreposição.** A Defeito's effect **overrides** racial traits, Talentos, Título and Competência
effects: "Multiplicador de PV sempre 1, não é possível aumentar" beats every +N; an immunity doesn't
beat a Vulnerabilidade; a Título can't awaken against Herança de Gilgamesh.

## Model (core)

Mirrors `org.aventyrs.core.background`, with the same "the effect is a Talento-shaped trait" trick
that made every Antecedente Benefício free to wire:

- `org.aventyrs.core.defect`: `Defect` enum (12), `DefectSeverity` (LEVE/MODERADO/GRAVE,
  with its Superar EXP), `Quality` enum (12), `QualityClass` (MENOR/MAIOR), each Defeito/Qualidade
  naming its opposite.
- `HeldDefect(defect, severity, acquiredAtCreation, choice)` and `HeldQuality(quality, class, choice)` —
  the choice for the ones that ask (Deficiência Física: braços/pernas; Deficiência Sensorial /
  Sentido Superior: sentido; Fobia: its object, free text; Vulnerabilidade / Resistência Atípica:
  energy; Tendência Atlética Maior: Força/Destreza).
- `Character#defects` / `#qualities`; each held level folds into `Character#getFeats()` as a
  `DefeitoFeat` / `QualidadeFeat` constant (non-catalog, like `AntecedenteFeat`), so the existing
  hooks reach it. A Maior Qualidade folds in **both** its Maior and Menor constants.
- **Sobreposição as a stage, not a sum.** The three multipliers get a final Defeito stage:
  `Feat#resolveLifeMultiplierCeiling` (and PM/PD twins) applied *after* every bonus — the "sempre
  igual à 1" clauses. Elemental/typed immunity is checked against a held Vulnerabilidade first.
- **Creation**: `CharacterCreationService` gains `getSuperacaoOptions(severity)`,
  `applyDefectsAndQualities(...)` — validates one per severity, ≤ 3 Qualidades, no opposing pair,
  the chosen Superação per Defeito, and the Talentos-Gerais trade (reduces
  `getStartingFeatSlots`). Order in the creation flow: **before the Talentos step** (a Superação can
  grant a Talento or a Habilidade, and the trade removes General slots), after Egos.
- **Mid-campaign**: `DefectService#grantDefect(character, defect, severity)` (GM, no benefit) and
  `#overcome(character, sheet, heldDefect)` spending 3/5/7 EXP (progression-locked like the rest).

## Clause triage

✅ existing hook · 🔧 small new hook/stage · 📖 narrative only (modelled as data, no effect)

### Defeitos

| Defeito | Leve | Moderado | Grave |
| --- | --- | --- | --- |
| Comportamento Excêntrico | Desvantagem, Carisma-based rolls ✅ (governing-Atributo roll bonus) | GD +1 nível, Carisma ✅ (negative `resolveDifficultyReduction`) | Automatic failure, Carisma 🔧 (`resolveAutomaticFailure`, twin of `resolveAutomaticSuccess`) |
| Corpo Frágil | Mult. PV −1 ✅ | Mult. PV −2; Doença/Veneno duration ×2 🔧 (Condição duration multiplier) | Mult. PV = 1, can't rise 🔧 (ceiling stage); Doença/Veneno maximized ×2 🔧 |
| Desapego Material | 📖 (carried only) | 📖 (Voto de Pobreza) | 📖 + optional check: ≤ 1 Ofensivo + 1 Defensivo non-tech equipped |
| Deficiência Física (braços/pernas) | Desvantagem on rolls using the limb 🔧 (which Perícias "use" arms/legs — see Q2); braços: Desvantagem on Dano Físico ✅; pernas: Movimento −2UD ✅ | Can't roll those Perícias 🔧; pernas: Movimento ½ 🔧 (halving stage, gap catalog "Multiplicative stages"); prosthesis → Leve effects (a toggle) | + no Habilidade de Atributo of Força **or** Destreza ("aleatoriamente") 🔧 acquisition refusal |
| Deficiência Sensorial | Desvantagem, **every Atenção roll** ✅ | GD +1 nível, Atenção ✅ | Automatic failure, Atenção 🔧 |
| Desconexão com o AEther | Mult. PM −2 ✅ | Mult. PM = 1, can't rise 🔧 | + never recovers PM from Descanso 🔧 (rest hook) |
| Distúrbio de Atenção | Desvantagem on Iniciativa and Atenção 🔧 (Iniciativa is a total, not a roll — see Q3) | + −1PA in odd Rodadas of combat 🔧 (Rodada-parity PA stage) | + no Ações Livres / Reações in odd Rodadas 🔧 |
| Fobia (object chosen) | Abalado while in its presence — 📖 trigger, ✅ Condição (GM applies) | Assustado | Apavorado |
| Herança de Gilgamesh | Mult. PD −1; can't awaken Título Secundário ✅ (`resolveTitleAcquisitionPermission`) | Mult. PD −2; no Secundário; Primário only from 25 EXP total ✅ | Mult. PD = 1 🔧; can't awaken any Título ✅ |
| Memória Fraca | 📖 | 📖 | 📖 |
| Vulnerabilidade (Físico não-Elemental / Mágico / 2 random Elementos) | Desvantagem on Esquiva e Aparar vs that type 🔧 (incoming attack's type at defence time); +2 dano on a hit ✅ (damage-taken bonus) | GD +1 nível to defend; +3 dano | Can't defend 🔧; +1d6 dano 🔧 (extra die taken) |
| Restrição Moral | 📖 | 📖 (could forbid attacking Caído/Desprevenido targets — see Q5) | 📖 |

### Qualidades (Maior includes Menor)

| Qualidade | Menor | Maior | Opposes |
| --- | --- | --- | --- |
| Centelha Maior | First PD-costing Título/Talento effect per combat costs 0; first Ego-costing one costs 1 🔧 (per-Cena cost override) | Mult. PD +1, +2 while wielding any Regalia ✅ | Herança de Gilgamesh |
| Destinado a Fortuna | Recursos points worth +2PE 🔧; +1 temp Recurso on arc completion 📖 (no arc concept) | +1 permanent Recursos ✅ (Ego base, like Nobre) | Desapego Material |
| Escolhido da Magia | RM + Vantagem on Domínio do Mana to cast ✅ | Mult. PM +1 ✅; negate first Magia each combat, recover its PM 🔧 | Desconexão com o AEther |
| Intelecto Superior | New Especialização after the 1st session ✅ (session-end acquisitions) | Graduações to the 3rd cost −0.5 EXP ✅ (0.0.71 hook); a Competência after the 3rd session 🔧 (session count) | Memória Fraca |
| Oportunista Nato | Vantagem on the first roll vs each Desprevenido target per Cena 🔧 (per-target log) | Vantagem on Atenção/Persuasão ✅; on any roll vs Desprevenido ✅ | Restrição Moral |
| Radiante | Vantagem, Carisma-based ✅ | GD −1 nível once per Cena, Carisma ✅ (roll activation + marker) | Comportamento Excêntrico |
| Resiliência Heroica | Can't be Abalado/Assustado/Apavorado while an ally nearby is 🔧 | Never Assustado/Apavorado (capped at Abalado) ✅ (Condição suppression) | Fobia |
| Resistência Atípica (1 Elemento / Profana / Divina) | Resistance: Meio-Dano + reduced duration ✅ (element); Profana/Divina = Magias of that `MagicType` 🔧 | Immunity ✅ | Vulnerabilidade |
| Saúde de Ferro | +1PV per Descanso Longo+ (+2 with a Título) ✅ | Mult. PV +1; +2PV per Descanso Longo+ ✅ | Corpo Frágil |
| Sentido Superior | Vantagem, every Atenção roll ✅ | Once per session: choose success on Atenção ✅ (Artista-style) | Deficiência Sensorial |
| Sexto Sentido | Vantagem on Atenção ✅ and Iniciativa (see Q3) | +1 permanent Iniciativa ✅; an Atenção Especialização or Competência ✅ | Distúrbio de Atenção |
| Tendência Atlética | Vantagem on the first Força/Destreza roll each even Rodada 🔧 (Rodada parity) | A Habilidade de Atributo of Força or Destreza ✅ (`getGrantedAttributeAbilities`) | Deficiência Física |

## Phases

1. **Model + creation (core).** — **Done (0.0.72)**, see `0.072.CHANGELOG.md`. Enums, held records, Superação options and validation (one per
   gravidade, ≤ 3 Qualidades, opposing pairs, Talentos-Gerais trade), materializing a Superação's
   Perícia/Graduação/Talento/Habilidade, folding the traits into `getFeats()`. Package-info creation
   step, CLAUDE.md row, tests.
2. **Everything ✅.** All clauses on existing hooks (≈ 60% of the table).
3. **The 🔧 stages.** Multiplier ceiling stage (Sobreposição), automatic failure, Rodada-parity
   (PA / Ações Livres / Reações / Vantagem), incoming-attack type at defence, Condição duration
   multiplier, per-Cena first-cost override, "negate the first Magia", Movimento halving.
4. **Mid-campaign.** `DefectService#grantDefect` / `#overcome` (3/5/7 EXP, progression lock).
5. **API.** `defects` / `qualities` on the Character entry/DTO/response (with each choice), validated
   like the Antecedentes.
6. **Client.** Wizard step "Defeitos e Qualidades" before Talentos (optional; Superação picks; the
   trade shrinking the Talentos step), hub display + GM "grant Defeito" / player "Superar", scene
   wiring for the activated ones (Aura de Confiança, Sentir o Todo), Fobia's Condição applied by the GM.

## Answered (2026-09-29)

1. **Talentos-Gerais trade kept, but gated**: a character may trade 1 Talento Geral for a Menor (2 for
   a Maior) only while holding at least one creation Defeito — **no Qualidade without a Defeito**. A
   Defeito needn't yield a Qualidade: its Superação may be any listed option (e.g. the Moderado's
   Talento Geral).
2. **Which Perícias depend on a limb (Deficiência Física):**
   - none: Artes, Atenção, Conhecimentos, Domínio do Mana, Empatia Selvagem, Medicina e Cura,
     Persuasão, Profissão;
   - braços: Ataque à Distância, Ataque Corpo-a-Corpo;
   - pernas: Esquiva e Aparar, Furtividade;
   - both: Atletismo, Dirigir e Cavalgar.
3. **Iniciativa is rolled** when a character is added to the Scene; Vantagem/Desvantagem there are
   ±2 on that roll (a new Iniciativa-roll bonus, beside the Iniciativa total).
4. **"Aleatoriamente"** is rolled at creation (client) and stored like any other choice.
5. **Restrição Moral** stays narrative — with a TODO to enforce Moderado/Grave against Caído and
   Desprevenido targets once that's wanted.
6. **Qualidades switched off by curses**: out of scope — a TODO where Maldições are wired.
7. **Grave's "Habilidade de Ego"** = a second Vantagem de Ego.
