package org.aventyrs.core.feat;

import org.aventyrs.core.ability.ActiveAbility;
import java.math.BigDecimal;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.race.Vampiro;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * Talentos Vampíricos — six <b>Poderes Vampíricos</b>, one that extends them, and three that
 * climb the Vampiro's own hierarchy.
 *
 * <p><b>The tree is gated on {@code requiredRace(Vampiro.class)}.</b> {@code
 * org.aventyrs.core.race.Vampiro} is a {@link org.aventyrs.core.race.CreatureType#RENASCIDO}
 * Mestiço whose seven {@code VampiroLineage} sub-raças fix the +1 Atributo and the feeding Armas
 * Naturais. {@code requiredCreatureType} is deliberately not used as a stand-in, since a
 * Vampiro's {@code getPrerequisiteCreatureType()} is its <em>life-race's</em> type.
 *
 * <p><b>A "Poder Vampírico" is an activated power with a Duração</b> — "Ativar Poderes Vampíricos
 * requer uma Ação Livre, duram por 2 Rodadas e consomem 3PV cada" ({@code Vampiro}'s Sangue,
 * Poder e Dependência). This is now <b>built</b>: {@link #OSTEOMANCIA}, {@link #CELERIDADE_VAMPIRICA},
 * {@link #ARMAMENTO_DE_ORLOK} and {@link #DOM_DE_MIRCALLA} each grant a {@link
 * PoderVampiricoActiveAbility} (via {@link Feat#resolveActiveAbility()}), triggered through
 * {@code org.aventyrs.core.character.services.ActiveAbilityService#activate} — which spends the
 * 3PV and applies the buff as {@code TemporaryBonus}es/a {@code LifeSteal} for the Duração.
 * {@link #PODER_VAMPIRICO_DURADOURO} extends that Duração by one Rodada per Título Aventyr.
 *
 * <p>{@link #METAMORFOSE_DRACULEA} is real too, and the tree's most elaborate constant — see
 * {@link MetamorfoseDraculeaFeat} and {@link FormaMetamorfica}. Still blocked: {@link #PRESENCA_DE_CARMILLA}
 * (a per-Rodada effect needs the Scene's neighbours and their PV, which {@code
 * TemporaryEffect#applyRoundEffect(CombatantSheet)} cannot see), {@link #LACOS_ROMPIDOS}/{@link
 * #MESTRE_VAMPIRO}'s target-scoped Vantagem / Laços-de-Sangue relation / gerar Prole, and {@link
 * #ABOMINACAO} (its entire source description is the character "V").
 */
public enum VampiricoFeat implements Feat {

    /**
     * <b>Source-document defect.</b> This Talento's entire {@code Descrição:} line is the single
     * character "V" — the text is missing from the source, not omitted here. Transcribed as a
     * named placeholder so the constant exists and the catalog count is honest; there is nothing
     * to implement and nothing to TODO beyond obtaining the real text.
     *
     * <p>The same family of defect as {@code Se Mover e Atacar}'s missing description and {@code
     * Escudo Que Anda}'s misplaced prerequisite, both recorded in {@code
     * docs/rules/talentos-index.md}.
     */
    ABOMINACAO(
            "<descrição ausente no documento de origem: a linha Descrição: contém apenas 'V'>",
            FeatRequirements.builder().requiredRace(Vampiro.class).build()),

    /**
     * "Você recebe Vantagem em rolagens de Ataque e Dano e estende seu Roubo de Vida Racial e de
     * Poderes Vampíricos a todos os seus ataques Corpo-a-Corpo e Magias."
     *
     * <p><b>Real, as a Poder Vampírico.</b> Activating it grants, for the Duração: {@code
     * ATAQUE_CORPO_A_CORPO_ROLL_BONUS}/{@code ATAQUE_A_DISTANCIA_ROLL_BONUS}/{@code
     * DAMAGE_ROLL_BONUS} +2 (Vantagem on Ataque and Dano rolls) and a {@code LifeSteal(1)} — the
     * "estende seu Roubo de Vida a todos os seus ataques" clause, modeled as a blanket Roubo de
     * Vida 1 while active. {@link #SEDE_DE_SANGUE} then amplifies that active {@code LifeSteal}
     * for real.
     */
    ARMAMENTO_DE_ORLOK(
            "Você recebe Vantagem em rolagens de Ataque e Dano e estende seu Roubo de Vida Racial "
                    + "e de Poderes Vampíricos a todos os seus ataques Corpo-a-Corpo e Magias "
                    + "capazes de causar danos diretamente.",
            FeatRequirements.builder().requiredRace(Vampiro.class).build()) {
        private final ActiveAbility ability = new PoderVampiricoActiveAbility(this);

        @Override
        public Optional<ActiveAbility> resolveActiveAbility() {
            return Optional.of(ability);
        }
    },

    /**
     * "Você adquire a capacidade de se transformar em animais ou névoa… Escolha 2 Formas
     * Metamórficas."
     */
    // Real, through MetamorfoseDraculeaFeat — the acquired form recording which Formas were
    // chosen, with both lineage rules validated (Dampiro 1 / Rakshasa 4 / otherwise 2, and no
    // Névoa for a Rakshasa). Each chosen Forma is separately activatable as a Poder Vampírico
    // (Ação Livre, 3PV, 2 Rodadas) through Feat#resolveActiveAbilities — the plural hook this
    // Talento is the reason for — and entering one suppresses weapons while leaving defensive
    // items working, which is this clause's own equipment rule (FormType#getEquipmentPolicy).
    // Each row's ARMA NATURAL column is granted for real now, through
    // MetamorfoseDraculeaFeat#getGrantedNaturalWeapons(Character, CombatantSheet) and read off
    // CombatantSheet#getNaturalWeapons() — and it *replaces* the holder's own while the shape is
    // worn. That replacement is a reading rather than a transcription; FormaMetamorfica's javadoc
    // carries the two counts on which the source argues the other way.
    // Three of the six HABILIDADE entries are live too: Aranha's Furtividade Vantagem, Lobo's
    // Perícias de Ataque Vantagem (plus its one-handed-weapon clawback, FormType
    // #permitsOneHandedWeapons) and Morcego's Roubo de Vida +2.
    // TODO: the other HABILIDADE entries are authored text granted by nothing, each blocked on its
    //  own missing system rather than one shared gap — see the per-constant TODOs on
    //  FormaMetamorfica, which name them individually (a Movimento Base sub-stat for Vertical/Voo,
    //  the Multiplicador de PV's sheet reach, damage-type
    //  immunity, and a concrete Corrente de Efeitos over an inert ConditionType#ENVENENADO).
    // TODO: Névoa's "é incapaz de causar danos" is only half closed. Emptying its Armas Naturais
    //  stops the weapon path, but nothing stops a damaging Magia, and canAttackWith(null) — an
    //  Ataque Desarmado — stays unconditionally true. A blanket damage prohibition exists nowhere.
    METAMORFOSE_DRACULEA(
            // NOTE: the source reads "armas não podem seu [sic] utilizadas, são substituídas por
            // armas naturais" — two comma-joined clauses. The "seu"→"ser" typo is repaired and an
            // "e" inserted for readability; the grammar matters, since whether "são substituídas"
            // takes *armas* as its subject is exactly what FormaMetamorfica's reading turns on.
            "Você adquire a capacidade de se transformar em animais ou névoa; enquanto usando "
                    + "metamorfose seus Equipamentos se adaptam ao seu corpo, itens defensivos "
                    + "continuam concedendo seus benefícios, armas não podem ser utilizadas e são "
                    + "substituídas por armas naturais. Escolha 2 Formas Metamórficas entre Aranha "
                    + "Gigante, Cavalo de Chifres, Lobo Dentes-de-Sabre, Morcego Atroz, Névoa e "
                    + "Serpente Espinhosa (Dampiros podem escolher apenas 1, Rakshasa podem "
                    + "escolher 4, mas não podem se transformar em Névoa).",
            FeatRequirements.builder().requiredRace(Vampiro.class).build()) {
        /**
         * "Escolha 2 Formas Metamórficas … (Dampiros podem escolher apenas 1, Rakshasa podem
         * escolher 4, mas não podem se transformar em Névoa)" — advertised so a client can
         * discover the choice from the catalog constant itself, and so {@code
         * FeatService#grantFeat} refuses this constant taken plain.
         */
        @Override
        public java.util.List<FeatChoice<?>> resolveRequiredChoices(final Character holder) {
            return java.util.List.of(new FeatChoice<>(ActiveAbility.class,
                    MetamorfoseDraculeaFeat.choicesFor(holder),
                    FormaMetamorfica.availableTo(holder).stream()
                            .map(FormaMetamorfica::getTransformation)
                            .toList()));
        }
    },

    /**
     * "Você recebe Bônus Racial de +1 em Carisma e Instinto. Estes Bônus aumentam em +1 para cada
     * Título Aventyr que você possuir."
     *
     * <p><b>Real, as a Poder Vampírico.</b> Activating it grants {@code CHARISMA_BONUS} and
     * {@code INSTINCT_BONUS} {@code TemporaryBonus}es of {@code 1 + }Títulos for the Duração.
     * <b>Partial reach:</b> those round-scoped Atributo bonuses are read only by {@code
     * AbstractSkillInteraction} — a Perícia roll governed by Carisma or Instinto — and by nothing
     * else (HP/PM/Iniciativa still read {@code AttributeValue#getTotal()}). The clause calls it a
     * "Bônus Racial", which reads permanent; modeled as the Duração-scoped Poder it is.
     */
    DOM_DE_MIRCALLA(
            "Você recebe Bônus Racial de +1 em Carisma e Instinto. Estes Bônus aumentam em +1 "
                    + "para cada Título Aventyr que você possuir.",
            FeatRequirements.builder().requiredRace(Vampiro.class).build()) {
        private final ActiveAbility ability = new PoderVampiricoActiveAbility(this);

        @Override
        public Optional<ActiveAbility> resolveActiveAbility() {
            return Optional.of(ability);
        }
    },

    /**
     * "Você pode moldar seus ossos para melhor proteger seu corpo, recebendo Bônus de +2 em suas
     * Defesas. Os Bônus aumentam em +1 para cada Título Aventyr que você possuir."
     *
     * <p><b>Real, as a Poder Vampírico.</b> Activating it grants a {@code TemporaryBonus(DEFESAS,
     * 2 + }Títulos{@code )} for the Duração — summed for real by {@code DefenseService}. Modeled
     * as the timed buff it is, not the permanent {@code Feat#resolveDefenseBonus} its figure
     * would otherwise fit.
     */
    OSTEOMANCIA(
            "Você pode moldar seus ossos para melhor proteger seu corpo, recebendo Bônus de +2 em "
                    + "suas Defesas. Os Bônus em Defesas aumentam em +1 para cada Título Aventyr "
                    + "que você possuir.",
            FeatRequirements.builder().requiredRace(Vampiro.class).build()) {
        private final ActiveAbility ability = new PoderVampiricoActiveAbility(this);

        @Override
        public Optional<ActiveAbility> resolveActiveAbility() {
            return Optional.of(ability);
        }
    },

    /**
     * "A cada Rodada você recupera 1PV para cada personagem vivo em Distância Muito Curta que
     * esteja ferido."
     */
    // Real. Activating it (3PV, Ação Livre, the Poderes' Duração) holds a sheet.CarmillaPresence, and
    // VampiricPresenceService#drain — called at the holder's Turn start — recovers 1PV per wounded,
    // living combatant within Muito Curta, one band wider per Título. ⚠️ "Vivo" is anyone whose Raça is
    // not RENASCIDO; a foe without a Raça counts as living.
    PRESENCA_DE_CARMILLA(
            "Você é capaz de roubar sangue dos vivos próximos a você. A cada Rodada você recupera "
                    + "1PV para cada personagem vivo em Distância Muito Curta que esteja ferido "
                    + "(que tenha perdido 1 ou mais PV), este é um efeito de Roubo de Vida. A "
                    + "Distância aumenta em +1 nível para cada Título Aventyr que você possuir.",
            FeatRequirements.builder().requiredRace(Vampiro.class).build()) {
        private final ActiveAbility ability = new PoderVampiricoActiveAbility(this);

        @Override
        public Optional<ActiveAbility> resolveActiveAbility() {
            return Optional.of(ability);
        }
    },

    /**
     * "A Duração de seus Poderes Vampíricos aumenta para 2 Rodadas."
     *
     * <p>The constant that establishes the whole tree's reading. Its Pré-requisito ("2 outros
     * Talentos de Poderes Vampíricos") is real and enforced.
     *
     * <p><b>Real.</b> While this Talento is held, {@link PoderVampiricoActiveAbility} adds one
     * Rodada per Título Aventyr to every Poder Vampírico's Duração. Its first clause, "aumenta
     * para 2 Rodadas", is redundant with the race Característica (which already sets the base to
     * 2) — a source inconsistency, resolved toward the more specific text.
     */
    // "2 outros Talentos de Poderes Vampíricos" is exact: isEligible counts the held Poderes
    // (isPoderVampirico — the eight whose heading reads "Poder Vampírico –"), not the whole tree.
    PODER_VAMPIRICO_DURADOURO(
            "A Duração de seus Poderes Vampíricos aumenta para 2 Rodadas. Adicionalmente a "
                    + "Duração de seus Poderes aumentam em +1 Rodada para cada Título Aventyr que "
                    + "você possuir.",
            FeatRequirements.builder()
                    .requiredRace(Vampiro.class)
                    .build()) {
        @Override
        public boolean isEligible(final Character character, final CharacterSheet sheet) {
            long otherPoderes = character.getFeats().stream()
                    .map(Feat::catalogEntry)
                    .filter(held -> held instanceof VampiricoFeat vampirico && vampirico.isPoderVampirico())
                    .distinct()
                    .count();
            return Feat.meetsRequirements(getFeatRequirements(), character, sheet)
                    && otherPoderes >= DURADOURO_REQUIRED_PODERES;
        }
    },

    /**
     * "Desfaz os Laços-de-Sangue… Vantagem em rolagens de Perícias baseadas em Carisma e Atenção
     * efetuadas contra outros Vampiros."
     */
    // The Vantagem is real: a roll opposed by another Vampiro (SceneContext#getOpposedCharacter) on a
    // Perícia whose own Atributo is Carisma, or on Atenção — ⚠️ "baseadas em Carisma e Atenção" names
    // a Perícia beside an Atributo, read as either. MESTRE_VAMPIRO widens it to any opponent.
    // The Pré-requisito is fully enforced now — "EXP total ≥ 15 ou 1 Título Aventyr Desperto",
    // two FeatRequirements#anyOf branches, with the Raça common to both on the outer group. The
    // EXP branch reads CharacterSheet#getTotalExperience through Feat#isEligible's sheet-taking
    // overload; a sheet-less preview skips it and falls back to the Título branch alone.
    // The Laços-de-Sangue are real: Vampiro#getSireId records the master, and BloodBondService#getMaster
    // answers "no master" once this Talento is held.
    LACOS_ROMPIDOS(
            "Desfaz os Laços-de-Sangue, não precisando mais obedecer ao seu mestre. Vantagem em "
                    + "rolagens de Perícias baseadas em Carisma e Atenção efetuadas contra outros "
                    + "Vampiros.",
            FeatRequirements.builder()
                    .requiredRace(Vampiro.class)
                    .alternative(FeatRequirements.builder()
                            .requiredTotalExperience(BigDecimal.valueOf(15))
                            .build())
                    .alternative(FeatRequirements.builder()
                            .requiredAwakenedTitles(1)
                            .build())
                    .build()) {
        @Override
        public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                         final SkillTrait requestedAbility, final Character character) {
            CombatantSheet opponent = sceneContext == null ? null : sceneContext.getOpposedCharacter();
            if (opponent == null || !isCharismaOrAttention(skillType)) {
                return 0;
            }
            boolean mestre = character.getFeats().stream().anyMatch(feat -> feat.catalogEntry() == MESTRE_VAMPIRO);
            return mestre || isVampiro(opponent) ? Skill.ADVANTAGE_BONUS : 0;
        }
    },

    /**
     * "Você chega no ponto evolutivo mais alto da sua raça… Você agora pode gerar Prole, criando
     * outros Vampiros."
     */
    /**
     * <p>The "+1 ao seu Bônus Racial em Atributo ganho por ser um Vampiro" half is <b>real</b>:
     * {@link Feat#resolveAttributeBonus} returns +1 for the holder's own {@code VampiroLineage}
     * Atributo (Força for a Nosferatu, Carisma for a Baobhan, …). <b>Partial reach</b>, per that
     * hook's javadoc — the +1 lands on a Perícia roll governed by that Atributo, not on HP/PM/etc.
     */
    // Real: LACOS_ROMPIDOS' Vantagem reaches any opponent while this is held, and the GD eases one
    // nível on those rolls against another Vampiro (the SceneContext-taking resolveDifficultyReduction).
    // "Gerar Prole" is BloodBondService#sire, which builds the progeny's Vampiro Raça naming this
    // master; creating the Character around it stays the caller's.
    // Its own disjunction is enforced too — "2 Títulos Aventyr Despertos ou EXP total ≥ 30" —
    // with the Raça and the required Talento common to both branches.
    MESTRE_VAMPIRO(
            "Você chega no ponto evolutivo mais alto da sua raça. Seu Bônus Racial em Atributo "
                    + "ganho por ser um Vampiro aumenta em +1 e sua Vantagem em rolagens de "
                    + "Perícias baseadas em Carisma e Atenção é aplicada contra quaisquer outros "
                    + "personagens; quando efetuadas contra outros Vampiros a GD é reduzida em -1 "
                    + "Nível. Você agora pode gerar Prole, criando outros Vampiros – Dampiros não "
                    + "recebem este benefício.",
            FeatRequirements.builder()
                    .requiredRace(Vampiro.class)
                    .requiredFeat(LACOS_ROMPIDOS)
                    .alternative(FeatRequirements.builder()
                            .requiredAwakenedTitles(2)
                            .build())
                    .alternative(FeatRequirements.builder()
                            .requiredTotalExperience(BigDecimal.valueOf(30))
                            .build())
                    .build()) {
        @Override
        public int resolveAttributeBonus(final AttributeDomain domain, final Character character) {
            return character.getRace() instanceof Vampiro vampiro
                    && vampiro.getLineage().getBonusAttribute() == domain
                    ? MESTRE_VAMPIRO_ATTRIBUTE_BONUS : 0;
        }

        @Override
        public int resolveDifficultyReduction(final SkillType skillType, final Character character,
                                              final SceneContext sceneContext) {
            CombatantSheet opponent = sceneContext == null ? null : sceneContext.getOpposedCharacter();
            return opponent != null && isVampiro(opponent) && isCharismaOrAttention(skillType) ? 1 : 0;
        }
    },

    /**
     * "Você recebe temporariamente +1 PA. Enquanto afetado por Celeridade seu Movimento Base
     * aumenta em +1 para cada Título Aventyr que você possuir."
     *
     * <p><b>Real, as a Poder Vampírico.</b> Activating it grants {@code TemporaryBonus(ACTION_POINTS,
     * 1)} and {@code TemporaryBonus(MOVEMENT, }Títulos{@code )} for the Duração — both read by the
     * {@code CombatantSheet} overloads of {@code ActionPointsService}/{@code MovementService}.
     */
    CELERIDADE_VAMPIRICA(
            "Você recebe temporariamente +1 PA. Enquanto afetado por Celeridade seu Movimento "
                    + "Base aumenta em +1 para cada Título Aventyr que você possuir.",
            FeatRequirements.builder()
                    .requiredRace(Vampiro.class)
                    .requiredAwakenedTitles(1)
                    .build()) {
        private final ActiveAbility ability = new PoderVampiricoActiveAbility(this);

        @Override
        public Optional<ActiveAbility> resolveActiveAbility() {
            return Optional.of(ability);
        }
    },

    /**
     * "Seu Roubo de Vida Racial aumenta em +1 para cada Título Desperto que você possuir."
     *
     * <p><b>Real-but-inert.</b> {@link Feat#resolveLifeStealBonus} returns the holder's Títulos
     * Despertos count, summed by {@code LifeStealService#getTotalLifeSteal} — but, like every
     * Roubo de Vida bonus, only while a {@code LifeSteal} effect is already active (e.g. from
     * {@link #ARMAMENTO_DE_ORLOK}). And nothing in this core resolves a dealt hit to read {@code
     * getTotalLifeSteal} yet, so the amplification is computed, not applied — the same status as
     * the rest of the Roubo de Vida infrastructure.
     */
    SEDE_DE_SANGUE(
            "Seu Roubo de Vida Racial aumenta em +1 para cada Título Desperto que você possuir.",
            FeatRequirements.builder()
                    .requiredRace(Vampiro.class)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveLifeStealBonus(final Character character) {
            return character.getAllTitles().size();
        }
    };

    private static final int MESTRE_VAMPIRO_ATTRIBUTE_BONUS = 1;

    /** Poder Vampírico Duradouro: "2 outros Talentos de Poderes Vampíricos". */
    private static final int DURADOURO_REQUIRED_PODERES = 2;

    /**
     * Whether this is a Poder Vampírico — a Talento whose heading reads "Poder Vampírico –", the ones
     * {@link #PODER_VAMPIRICO_DURADOURO} counts and lengthens. {@link #LACOS_ROMPIDOS}, {@link
     * #MESTRE_VAMPIRO} and Duradouro itself are not.
     */
    public boolean isPoderVampirico() {
        return switch (this) {
            case ABOMINACAO, ARMAMENTO_DE_ORLOK, METAMORFOSE_DRACULEA, DOM_DE_MIRCALLA, OSTEOMANCIA,
                 PRESENCA_DE_CARMILLA, CELERIDADE_VAMPIRICA, SEDE_DE_SANGUE -> true;
            default -> false;
        };
    }

    /** "Perícias baseadas em Carisma e Atenção": a Perícia whose own Atributo is Carisma, or Atenção. */
    private static boolean isCharismaOrAttention(final SkillType skillType) {
        return skillType == SkillType.ATTENTION
                || skillType.newSkillInstance().getAttributeDomain() == AttributeDomain.CHARISMA;
    }

    private static boolean isVampiro(final CombatantSheet sheet) {
        return sheet.getCharacter() != null && sheet.getCharacter().getRace() instanceof Vampiro;
    }

    private final String description;
    private final FeatRequirements featRequirements;

    VampiricoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.VAMPIRICO;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public FeatRequirements getFeatRequirements() {
        return featRequirements;
    }
}
