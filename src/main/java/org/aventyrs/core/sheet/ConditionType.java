package org.aventyrs.core.sheet;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.Skill;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * The Condições de Personagem a combatant can be under. The catalogue entry for a condition,
 * the same catalogue-not-instance split {@code org.aventyrs.core.item.Item} draws: a constant
 * here describes what Desprevenido <i>is</i>, while {@link Condition} is one combatant actually
 * being under it, with its own countdown and its own origin.
 *
 * <p><b>Malefícios are the harmful majority, not the whole enum.</b> A Condição is just a state
 * a combatant is in; most of the authored ones make things harder, but {@link #ESCONDIDO} is a
 * state a character puts <i>themselves</i> in and benefits from. Nothing here is typed by which
 * it is — no {@code isHarmful()} flag — because nothing asks: a clause names the condition it
 * cares about.
 *
 * <p>Authored from {@code docs/rules/condicoes-e-maleficios-.txt}, whose title covers only the
 * Malefícios. Three notes on that source: Caído is listed twice, and the fuller of the two
 * entries is the one modelled (it adds a Dano Corpo-a-Corpo malus the shorter one omits);
 * "Envenado" is a typo for {@link #ENVENENADO}; and {@link #ESCONDIDO} is not in it at all,
 * being a Condição rather than a Malefício.
 *
 * <p>Two things the rules text calls Malefícios are deliberately <b>not</b> constants here,
 * because they are other kinds of thing this core already models: <b>Coma</b> is {@code
 * CharacterStatus#COMMA}, a tier of the PV ladder (see {@code
 * SobrevivenciaFeat#PERMANECER_CONSCIENTE}), and <b>Encantamento</b> is {@code
 * MagicType#ENCANTAMENTO}, a kind of Magia (see {@code SantoSpecialization}).
 *
 * <p><b>Three ways a condition reaches the rules engine</b>, and a constant may use any mix:
 *
 * <ul>
 *   <li><b>{@link #getEffects()}</b> — typed numeric maluses ({@link ConditionEffect}), summed by
 *   {@code CombatantSheet#getConditionBonus} into whichever service already reads that {@link
 *   ModifierType}. Desprevenido's -2 Defesas needs no wiring in {@code DefenseService} beyond
 *   that call, exactly as an {@code ItemBonus} needs none.</li>
 *   <li><b>{@link #getImplied()}</b> — conditions this one confers ("também considerado
 *   Desprevenido"). Resolved transitively and range-scoped, so removing Caído removes the
 *   Desprevenido it brought without any separate bookkeeping.</li>
 *   <li><b>The boolean gates</b> ({@link #preventsMovement()} and friends) — for a clause that
 *   forbids something outright rather than taxing it. A malus and a prohibition are different
 *   shapes; don't model "não pode se mover" as a large negative {@link ModifierType#MOVEMENT}.</li>
 * </ul>
 *
 * <p><b>Desvantagem is a flat {@link Skill#DISADVANTAGE_MALUS}</b> (-2), the mirror of Vantagem —
 * see CLAUDE.md's "Vantagem is a flat +2" section. Desprevenido's "-2 em suas Defesas" is its own
 * separately-stated figure and gets its own constant, even though the two happen to coincide.
 */
@Getter
@AllArgsConstructor
public enum ConditionType {

    /**
     * "Alvo sofre Desvantagem em suas rolagens de perícia enquanto estiver a até 4UD da origem de
     * seu medo." The mildest rung of the fear ladder, and the one the other two decay into.
     */
    ABALADO("Alvo sofre Desvantagem em suas rolagens de perícia enquanto estiver a até 4UD da "
            + "origem de seu medo. Condição permanece ativa por 2 Rodadas, a menos que a origem "
            + "do efeito diga o contrário.") {
        @Override
        public List<ConditionEffect> getEffects() {
            return List.of(new ConditionEffect(ModifierType.SKILL_ROLL_BONUS, Skill.DISADVANTAGE_MALUS, Range.DISTANCIA_CURTA));
        }
    },

    /**
     * "Desvantagem em suas rolagens de perícia e Dano … Desprevenido enquanto adjacente … Ao fim
     * da duração alvo se torna Abalado."
     */
    // Fully real: the Perícia and Dano maluses (both proximity-scoped), the adjacency-scoped
    // Desprevenido, and the decay into ABALADO.
    ASSUSTADO("Alvo sofre Desvantagem em suas rolagens de perícia e Dano enquanto estiver a até "
            + "4UD da origem de seu medo. Recebe a Condição desprevenido enquanto adjacente a "
            + "origem de seu medo. Condição permanece ativa por 2 Rodadas, a menos que a origem "
            + "do efeito diga o contrário. Ao fim da duração alvo se torna Abalado.") {
        @Override
        public List<ConditionEffect> getEffects() {
            return List.of(
                    new ConditionEffect(ModifierType.SKILL_ROLL_BONUS, Skill.DISADVANTAGE_MALUS, Range.DISTANCIA_CURTA),
                    new ConditionEffect(ModifierType.DAMAGE_ROLL_BONUS, Skill.DISADVANTAGE_MALUS, Range.DISTANCIA_CURTA));
        }

        @Override
        public Map<ConditionType, Range> getImplied() {
            return Map.of(DESPREVENIDO, Range.ADJACENTE);
        }

        @Override
        public ConditionType getDecaysTo() {
            return ABALADO;
        }
    },

    /**
     * "Desvantagem … a até 8UD … Desprevenido … a menos de 4UD … Sempre deve tentar se manter
     * afastado … Ao fim da duração alvo se torna Assustado."
     */
    // TODO: "Sempre deve tentar se manter afastado, ao menos 4UD" is compelled movement — this
    //  core never does geometry and has no notion of constraining where a combatant may go
    //  (gap catalog, "Forced movement / positioning").
    APAVORADO("Alvo sofre Desvantagem em suas rolagens de perícia e Dano enquanto estiver a até "
            + "8UD da origem de seu medo. Recebe a Condição desprevenido enquanto estiver a menos "
            + "de 4UD da origem de seu medo. Sempre deve tentar se manter afastado, ao menos 4UD, "
            + "da origem de seu medo. Condição permanece ativa por 2 Rodadas, a menos que a "
            + "origem do efeito diga o contrário. Ao fim da duração alvo se torna Assustado.") {
        @Override
        public List<ConditionEffect> getEffects() {
            return List.of(
                    new ConditionEffect(ModifierType.SKILL_ROLL_BONUS, Skill.DISADVANTAGE_MALUS, Range.DISTANCIA_MEDIA),
                    new ConditionEffect(ModifierType.DAMAGE_ROLL_BONUS, Skill.DISADVANTAGE_MALUS, Range.DISTANCIA_MEDIA));
        }

        @Override
        public Map<ConditionType, Range> getImplied() {
            return Map.of(DESPREVENIDO, Range.DISTANCIA_CURTA);
        }

        @Override
        public ConditionType getDecaysTo() {
            return ASSUSTADO;
        }
    },

    /**
     * "Efeitos conforme descrição da maldição." Deliberately carries no effects of its own: the
     * condition <i>is</i> the marker, and whatever Maldição inflicted it supplies the mechanics.
     * A held {@link Condition} of this type is what a clause like {@code GorgonaFeat}'s "sempre
     * considerado Amaldiçoado" needs to test, and what {@code VidaSpell}'s Malefício-removal
     * clauses need to strip.
     */
    AMALDICOADO("Efeitos conforme descrição da maldição."),

    /**
     * "Personagem sofre Redutor de -2 em suas Defesas." The most-implied condition in the
     * catalogue — Assustado, Apavorado, Flanqueado, Caído and Cego all confer it.
     */
    DESPREVENIDO("Personagem sofre Redutor de -2 em suas Defesas.") {
        @Override
        public List<ConditionEffect> getEffects() {
            return List.of(new ConditionEffect(ModifierType.DEFESAS, DESPREVENIDO_DEFENSE_MALUS, null));
        }
    },

    /**
     * "Personagens flanqueados, cercados, ficam Desprevenidos. Atacar um personagem Flanqueado
     * garante Vantagem na rolagem de Dano." <b>Both halves are real.</b>
     *
     * <p>The second is the catalogue's only <i>outward-facing</i> effect — the Vantagem lands on
     * whoever attacks the holder, not on the holder — so it is {@link #getAttackerDamageBonus()}
     * rather than a {@link ConditionEffect}, and {@code AbstractSkillInteraction} reads it off the
     * <em>attackTarget</em>'s sheet.
     */
    // TODO: nothing detects flanking — whether a combatant is surrounded is geometry this core
    //  does not do, so this condition is always applied by a caller, never derived.
    FLANQUEADO("Personagens flanqueados, cercados, ficam Desprevenidos. Atacar um personagem "
            + "Flanqueado garante Vantagem na rolagem de Dano.") {
        @Override
        public Map<ConditionType, Range> getImplied() {
            return alwaysImplies(DESPREVENIDO);
        }

        @Override
        public int getAttackerDamageBonus() {
            return Skill.ADVANTAGE_BONUS;
        }
    },

    /**
     * "Desvantagem em rolagens de Perícias e Dano Corpo-a-Corpo. Também considerado
     * Desprevenido." Modelled from the fuller of the source's two Caído entries.
     */
    // TODO: the Dano malus is stated as "Dano Corpo-a-Corpo" specifically, but a
    //  ConditionEffect carries a ModifierType and a Range, not a Perícia — DAMAGE_ROLL_BONUS is
    //  summed for whichever Perícia de Ataque is rolled. Granted unscoped, which is wider than
    //  the text on an Ataque à Distância made while prone; the alternative is granting nothing.
    CAIDO("Desvantagem em rolagens de Perícias e Dano Corpo-a-Corpo. Também considerado "
            + "Desprevenido.") {
        @Override
        public List<ConditionEffect> getEffects() {
            return List.of(
                    new ConditionEffect(ModifierType.SKILL_ROLL_BONUS, Skill.DISADVANTAGE_MALUS, null),
                    new ConditionEffect(ModifierType.DAMAGE_ROLL_BONUS, Skill.DISADVANTAGE_MALUS, null));
        }

        @Override
        public Map<ConditionType, Range> getImplied() {
            return alwaysImplies(DESPREVENIDO);
        }
    },

    /** "Não pode realizar movimentos. Sofre Desvantagem em rolagens de Perícia." */
    // TODO: the two escape routes (1PA for a fresh Agarrar attack roll, 3PA to break free at the
    //  cost of exposure to the Reação "Defender o Perímetro") need an activated-action entry
    //  point, a GD-vs-roll resolution, and that Reação — none of which exist. See
    //  EscudeiroFeat, which records Defender o Perímetro as an unmodelled Reação type too.
    AGARRADO("Não pode realizar movimentos. Sofre Desvantagem em rolagens de Perícia. Pode usar "
            + "1PA para tentar se livrar, efetuando uma nova rolagem de ataque para Agarrar. Pode "
            + "usar 3PA para se libertar imediatamente, mas se torna vulnerável a reação Defender "
            + "o Perímetro.") {
        @Override
        public List<ConditionEffect> getEffects() {
            return List.of(new ConditionEffect(ModifierType.SKILL_ROLL_BONUS, Skill.DISADVANTAGE_MALUS, null));
        }

        @Override
        public boolean preventsMovement() {
            return true;
        }
    },

    /** "Não pode realizar movimentos ou rolagens de perícias baseadas em Força ou Destreza." */
    // TODO: the Força/Destreza roll prohibition is a *hard block* on a roll, not a malus, and
    //  nothing lets a held condition refuse a roll — AbstractSkillInteraction resolves the
    //  governing AttributeDomain but has no veto point. "Exceto para se soltar" additionally
    //  needs the escape action AGARRADO's own TODO describes.
    // TODO: "Após se libertarem mudam para a Condição Agarrado" is a transition on *escape*, not
    //  on expiry, so it is not getDecaysTo() — it needs the same missing escape action.
    IMOBILIZADO("Não pode realizar movimentos ou rolagens de perícias baseadas em Força ou "
            + "Destreza, exceto para se soltar. Pode usar 1PA para tentar se livrar, efetuando "
            + "uma nova rolagem de ataque para Agarrar. Pode usar 3PA para se libertar "
            + "imediatamente, mas se torna vulnerável a reação Defender o Perímetro. Após se "
            + "libertarem mudam para a Condição Agarrado.") {
        @Override
        public boolean preventsMovement() {
            return true;
        }
    },

    /**
     * "Podem efetuar rolagens de Ataque Corpo-a-Corpo desarmado ou com Armas Leves, GD 10+Vigor."
     *
     * <p><b>Being swallowed leaves you unarmed and unable to re-arm</b> — you fight with what is
     * already in your hands, and nothing you dropped can be picked back up from inside a
     * creature. That half is real: {@link #preventsArming()} refuses {@code
     * CombatantSheet#rearm(Weapon)}. "Desarmado" in this clause is the <i>Ataque Desarmado</i>
     * sense (a punch), not {@link #DESARMADO} the Malefício — this condition does not confer
     * that one, whose Desvantagem the rules text never charges a swallowed character.
     *
     * <p>An Ataque Desarmado made from inside already resolves correctly with no work here:
     * {@code DamageBaseService#getDamageBase(Character, SkillType)} starts at {@link
     * org.aventyrs.core.character.DamageBase#UNARMED}, the bottom rung, and still applies every
     * Talento/Habilidade scale-up on top — while deliberately skipping the enhancement scale-ups
     * that are bound to a weapon there isn't one of. The same split holds on the Perícia roll:
     * armour and other non-weapon Equipamento keep contributing, a weapon cannot.
     */
    // TODO: the GD "10+Vigor" of the devourer needs a GD-vs-roll comparison; "causar o dobro do
    //  Vigor de quem o devorou para se libertarem" needs damage accumulated against a specific
    //  captor; the per-Categoria-de-Tamanho Dano Desvantagem needs a size comparison against that
    //  captor; and "ignoram RD" needs a per-attack RD bypass (DamageService's
    //  ignoreDamageReduction flag is set per call, with nothing to key it off a condition).
    // TODO: the captor is the Condition's own source by convention, but nothing enforces that a
    //  DEVORADO Condition is constructed with one — the three clauses above all need it.
    DEVORADO("Podem efetuar rolagens de Ataque Corpo-a-Corpo desarmado ou com Armas Leves, GD "
            + "10+Vigor. Precisam causar uma quantidade de dano igual ao dobro do Vigor de quem o "
            + "devorou para se libertarem, sendo regurgitados. Sofrem Desvantagem na rolagem de "
            + "Dano para cada Categoria de Tamanho inferior ao personagem que o devorou. Ataques "
            + "realizados no interior de outras criaturas ignoram RD que elas possuam.") {
        @Override
        public boolean preventsArming() {
            return true;
        }

        @Override
        public boolean restrictsAttacksToLightWeapons() {
            return true;
        }
    },

    /**
     * "Desvantagem nas rolagens de Perícia de Ataque e Dano." <b>Fully real</b>, including the
     * effect that inflicts it — {@code CombatantSheet#disarm(Weapon)}.
     *
     * <p><b>Desarmado here means <i>disarmed</i> — something knocked your weapon out of your
     * hands — not <i>unarmed</i>.</b> The two are easy to confuse and are different concepts:
     * an <b>Ataque Desarmado</b> is a punch, an authored Arma Natural in its own right (1d6,
     * Esmagamento) that {@code ArtesMarciaisFeat#ARTISTA_MARCIAL} and friends scope to, and a
     * character choosing to fight bare-handed is <i>not</i> under this condition. This one is a
     * Malefício inflicted on you; that one is a way of attacking. A weapon can be immune to
     * being knocked away — {@code Weapon#isDisarmable()}, the "Não pode ser desarmado" Manopla
     * de Segurança.
     */
    DESARMADO("Desvantagem nas rolagens de Perícia de Ataque e Dano.") {
        /**
         * Scoped to the two Perícias de Ataque by naming their own {@link ModifierType}s rather
         * than {@code SKILL_ROLL_BONUS}, which would tax every Perícia — the per-Perícia
         * constants exist for exactly this (see {@code SkillType#getRollBonusType()}).
         */
        @Override
        public List<ConditionEffect> getEffects() {
            return List.of(
                    new ConditionEffect(ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS, Skill.DISADVANTAGE_MALUS, null),
                    new ConditionEffect(ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, Skill.DISADVANTAGE_MALUS, null),
                    new ConditionEffect(ModifierType.DAMAGE_ROLL_BONUS, Skill.DISADVANTAGE_MALUS, null));
        }
    },

    /** "O tempo de todas as ações aumentam em 1PA." */
    // TODO: {@code ModifierType#SKILL_ROLL_COST} exists and ActionPointsService reads it, but it
    //  scopes to a Perícia *roll*'s cost — this clause raises the cost of **every** action, and
    //  this core has no general action-cost concept to raise (what an action costs is the
    //  caller's, see MovementService's own note on Pontos de Ação).
    CONFUSO("O tempo de todas as ações aumentam em 1PA."),

    /** "Deve rolar 1d6 sempre que efetuar uma rolagem de perícia … Adicionalmente são considerados desprevenidos." */
    // TODO: the 1d6 miss-chance is not expressible — this core never rolls dice (CLAUDE.md), and
    //  the clause needs a *second* roll resolved per Perícia roll with a per-roll-type threshold
    //  (2 for personal effects, 3 for Corpo-a-Corpo, 5 for à Distância). A caller supplying that
    //  d6 would need a hook on the roll path that does not exist. The Desprevenido half is real.
    CEGO("Deve rolar 1d6 sempre que efetuar uma rolagem de perícia. Perícias de efeitos pessoal, "
            + "falham com resultados 2 ou menos. Rolagens de Ataque corpo a Corpo falham com "
            + "resultados 3 ou menos. Rolagens de Ataque à Distância falham com resultados "
            + "menores que 5. Adicionalmente são considerados desprevenidos.") {
        @Override
        public Map<ConditionType, Range> getImplied() {
            return alwaysImplies(DESPREVENIDO);
        }
    },

    /** "O personagem não pode ser curado e nem regenerar pontos de vida." */
    FERIDAS_DOLOROSAS("O personagem não pode ser curado e nem regenerar pontos de vida.") {
        @Override
        public boolean preventsHealing() {
            return true;
        }
    },

    /** "Personagem não podem ativar Habilidades de Aventyrs ou de Monstros e não podem Conjurar Magias." */
    SILENCIO("Personagem não podem ativar Habilidades de Aventyrs ou de Monstros e não podem "
            + "Conjurar Magias.") {
        @Override
        public boolean preventsAbilityActivation() {
            return true;
        }

        @Override
        public boolean preventsSpellCasting() {
            return true;
        }
    },

    /** "Alvo perde Multiplicador de Bônus Base, conforme especificado no efeito." */
    // TODO: "Multiplicador de Bônus Base" is a stat this core does not have — the phrase appears
    //  nowhere else except AssassinoFeat's own TODO'd "Roubo de Bônus Base". Until it exists
    //  there is nothing to reduce. "Conforme especificado no efeito" additionally means the
    //  amount lives on whatever inflicted this, not on the condition.
    // TODO: "Personagens imunes a efeitos Selvagens também são imunes" needs an effect-source
    //  classification (Selvagem) and a per-condition immunity check, neither of which exists.
    ENVENENADO("Alvo perde Multiplicador de Bônus Base, conforme especificado no efeito. "
            + "Personagens imunes a efeitos Selvagens também são imunes a este efeito."),

    /**
     * The hidden state a character enters by using Furtividade — a Condição rather than a
     * Malefício, and the one entry here that helps its holder.
     *
     * <p>A marker, like {@link #AMALDICOADO}: every clause in the catalogue <i>reads</i> it
     * ({@code FurtividadeCompetencyAbility#ACAO_SURPRESA}/{@code #MORTE_OCULTA}, {@code
     * MobilidadeFeat#MOVIMENTO_FURTIVO}, {@code AssassinoFeat#ESCUDO_DE_SOMBRAS}) rather than
     * describing effects of its own, so it carries none.
     */
    // TODO: nothing applies this automatically. Entering it is "upon using Furtividade", but a
    //  Furtividade roll can fail and this core has no roll-resolution-vs-GD engine to know
    //  whether it succeeded, so a caller applies it. Nothing lifts it either — being seen,
    //  attacking, or moving into the open would all end it, none of which is modelled.
    ESCONDIDO("Estado de ocultação obtido ao utilizar a Perícia Furtividade."),

    /** "Alvo sofre redutores conforme especificado no efeito." */
    // TODO: entirely open-ended — "conforme especificado no efeito" means the maluses live on
    //  whatever inflicted this, so there is nothing fixed to author here. Same missing Selvagem
    //  immunity classification as ENVENENADO.
    DOENTE("Alvo sofre redutores conforme especificado no efeito. Personagens imunes a efeitos "
            + "Selvagens também são imunes a este efeito.");

    /**
     * An implication that always holds, with no proximity scope — {@link Map#of} rejects a null
     * value, and {@code null} is exactly how {@link #getImplied()} spells "always".
     */
    private static Map<ConditionType, Range> alwaysImplies(final ConditionType implied) {
        return Collections.singletonMap(implied, null);
    }

    /** Desprevenido's own stated "-2 em suas Defesas". */
    private static final int DESPREVENIDO_DEFENSE_MALUS = -2;

    /** "Condição permanece ativa por 2 Rodadas" — the fear ladder's stated default. */
    public static final int DEFAULT_FEAR_DURATION_IN_ROUNDS = 2;

    private final String description;

    /**
     * One typed numeric malus this condition imposes, optionally scoped to a proximity band
     * around the condition's origin. {@code within} is {@code null} for an effect that always
     * applies; otherwise the effect counts only while the holder is at that {@link Range} or
     * closer to {@link Condition#getSource()} — which is how "enquanto estiver a até 4UD da
     * origem de seu medo" is expressed (4UD is {@link Range#DISTANCIA_CURTA}, 8UD is {@link
     * Range#DISTANCIA_MEDIA}).
     *
     * <p>Data, not a {@code @Modifier} method, for the same reason {@code ItemBonus} is: a
     * shared class cannot vary the compile-time-fixed {@code ModifierType} of an annotation.
     */
    public record ConditionEffect(ModifierType type, int value, Range within) {
    }

    /** Typed numeric maluses this condition imposes. Empty unless a constant overrides it. */
    public List<ConditionEffect> getEffects() {
        return List.of();
    }

    /**
     * Conditions this one confers ("também considerado Desprevenido"), each mapped to the {@link
     * Range} within which it applies — {@code null} meaning always. Resolved transitively by
     * {@code CombatantSheet#getActiveConditions}, so a condition implied at two different ranges
     * by two held conditions counts at whichever is satisfied.
     */
    public Map<ConditionType, Range> getImplied() {
        return Map.of();
    }

    /**
     * The condition this one turns into when its duration runs out — the fear ladder's "Ao fim da
     * duração alvo se torna Abalado". {@code null} for a condition that simply ends. Applied by
     * {@code CombatantSheet#tickTemporaryEffects()} at the moment of expiry.
     */
    public ConditionType getDecaysTo() {
        return null;
    }

    /**
     * A flat dano-roll bonus this condition grants to <b>whoever attacks its holder</b> —
     * {@link #FLANQUEADO}'s "Atacar um personagem Flanqueado garante Vantagem na rolagem de
     * Dano". Zero unless a constant overrides it.
     *
     * <p>The mirror image of {@link #getEffects()}, which is what the holder suffers. Kept a
     * separate hook rather than a {@link ConditionEffect} with an "applies to the attacker" flag
     * because the two are read at different times off different sheets: {@code
     * AbstractSkillInteraction} sums this from the attackTarget's conditions, and everything in
     * {@code getEffects()} from the roller's own.
     *
     * <p>Not proximity-scoped: an attacker is by definition attacking, and no authored condition
     * scopes an outward effect by distance. Add a {@code Range} when one does.
     */
    public int getAttackerDamageBonus() {
        return 0;
    }

    /** Whether this condition forbids movement outright — "não pode realizar movimentos". */
    public boolean preventsMovement() {
        return false;
    }

    /**
     * Whether this condition allows only an Ataque Desarmado or an {@link
     * org.aventyrs.core.item.ItemWeightClass#LIGHT} weapon — {@link #DEVORADO}'s "podem efetuar
     * rolagens de Ataque Corpo-a-Corpo desarmado ou com Armas Leves". False by default.
     *
     * <p>Read through {@code CombatantSheet#canAttackWith(Weapon)}. <b>Nothing enforces it at
     * attack time</b>: {@code DamageBaseService} swings whichever weapon its caller names, and
     * there is no validation point between choosing an attack and resolving it. So this is a
     * predicate a caller consults when deciding what to offer — real, exact data with no
     * automatic consumer, the same standing {@code Item#applyDamage} has.
     */
    public boolean restrictsAttacksToLightWeapons() {
        return false;
    }

    /**
     * Whether this condition stops its holder getting a weapon into their hands at all —
     * {@link #DEVORADO}'s "you are inside something": you can neither reach what you dropped nor
     * draw what is sheathed on your belt. False by default.
     *
     * <p>Covers <b>both</b> routes, which is why it is not named for either: {@code
     * CombatantSheet#rearm(Weapon)} (recovering a dropped weapon) and {@code
     * WeaponDrawService#draw} (taking a carried one in hand) each refuse on it.
     *
     * <p>Deliberately not true of {@link #DESARMADO}: being disarmed is precisely the state you
     * leave by arming yourself again, so a condition that blocked that would never end.
     */
    public boolean preventsArming() {
        return false;
    }

    /** Whether this condition forbids recovering Pontos de Vida by any means. */
    public boolean preventsHealing() {
        return false;
    }

    /** Whether this condition forbids activating Habilidades de Aventyr or de Monstro. */
    public boolean preventsAbilityActivation() {
        return false;
    }

    /** Whether this condition forbids Conjurar Magias. */
    public boolean preventsSpellCasting() {
        return false;
    }
}
