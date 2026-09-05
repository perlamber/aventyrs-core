package org.aventyrs.core.feat;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.race.Vampiro;

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
 * <p>Still blocked: {@link #METAMORFOSE_DRACULEA} (form state), {@link #PRESENCA_DE_CARMILLA}
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
    // TODO: needs a form state — the same missing piece DraconicoFeat#DRACONATO, HomemFera's
    //  Forma Híbrida and Gorgona's own forms are blocked on. The Poder Vampírico activation
    //  mechanism (now built) does NOT cover this: a Forma is a persistent alternate shape that
    //  swaps weapons and racial traits, not a timed TemporaryBonus.
    // TODO: the choice of two Formas from a table of six could be recorded now (a choice-carrying
    //  AbstractFeat subclass — see ArmamentoDraconicoFeat for the Set shape), and each Forma's
    //  Arma Natural has a catalog entry (NaturalWeapon). But each Forma also grants its own
    //  ability — a Movimento Base Vertical/de Voo, a Multiplicador de PV, a Corrente de Efeitos,
    //  physical-damage immunity — each separately blocked, and all gated on the form state.
    // TODO: "Dampiros escolhem 1, Rakshasa escolhem 4" — Vampiro.VampiroLineage has DAMPIRO/RAKSHASA,
    //  so the per-lineage count is expressible; the form state each Forma needs is the blocker.
    METAMORFOSE_DRACULEA(
            "Você adquire a capacidade de se transformar em animais ou névoa; enquanto usando "
                    + "metamorfose seus Equipamentos se adaptam ao seu corpo, itens defensivos "
                    + "continuam concedendo seus benefícios, armas não podem ser utilizadas e são "
                    + "substituídas por armas naturais. Escolha 2 Formas Metamórficas entre Aranha "
                    + "Gigante, Cavalo de Chifres, Lobo Dentes-de-Sabre, Morcego Atroz, Névoa e "
                    + "Serpente Espinhosa (Dampiros podem escolher apenas 1, Rakshasa podem "
                    + "escolher 4, mas não podem se transformar em Névoa).",
            FeatRequirements.builder().requiredRace(Vampiro.class).build()),

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
    // TODO: the Poder Vampírico activation mechanism is built, but this one still cannot be
    //  expressed: its per-Rodada recovery counts qualifying nearby combatants, and the only
    //  per-Rodada hook — TemporaryEffect#applyRoundEffect(CombatantSheet) — gets the holder's
    //  sheet alone, never the Scene's neighbours or their PV. "vivo" also needs the living/undead
    //  classification this core still lacks behaviourally (CreatureType.RENASCIDO exists but
    //  nothing keys "is this alive" off it).
    PRESENCA_DE_CARMILLA(
            "Você é capaz de roubar sangue dos vivos próximos a você. A cada Rodada você recupera "
                    + "1PV para cada personagem vivo em Distância Muito Curta que esteja ferido "
                    + "(que tenha perdido 1 ou mais PV), este é um efeito de Roubo de Vida. A "
                    + "Distância aumenta em +1 nível para cada Título Aventyr que você possuir.",
            FeatRequirements.builder().requiredRace(Vampiro.class).build()),

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
    // TODO: its requiredFeatCategory counts every Talento of this tree, where the text says "de
    //  Poderes Vampíricos" — the six constants whose name carries that prefix. Laços Rompidos,
    //  Mestre Vampiro and this constant itself are not Poderes, so the gate is looser than
    //  written; FeatCategory is one level coarser than the clause needs, and there is no finer
    //  "Poder Vampírico" sub-tag mechanism.
    PODER_VAMPIRICO_DURADOURO(
            "A Duração de seus Poderes Vampíricos aumenta para 2 Rodadas. Adicionalmente a "
                    + "Duração de seus Poderes aumentam em +1 Rodada para cada Título Aventyr que "
                    + "você possuir.",
            FeatRequirements.builder()
                    .requiredRace(Vampiro.class)
                    .requiredFeatCategory(FeatCategory.VAMPIRICO)
                    .requiredFeatCategoryCount(2)
                    .build()),

    /**
     * "Desfaz os Laços-de-Sangue… Vantagem em rolagens de Perícias baseadas em Carisma e Atenção
     * efetuadas contra outros Vampiros."
     */
    // TODO: this is not a Poder Vampírico (no Duração), so the activation mechanism does not
    //  apply. The Vantagem is scoped two ways this core cannot express at once — by AttributeDomain
    //  ("baseadas em Carisma e Atenção", where Atenção is a Perícia rather than an Atributo, an
    //  inconsistency in the source) and by the *target* being a Vampiro, which
    //  resolveSkillRollBonus carries no opponent for.
    // TODO: its Pré-requisito is a disjunction — "EXP total ≥ 15 ou 1 Título Aventyr Desperto" —
    //  and every clause combines with and. The Título branch is recorded because the other is
    //  unreachable anyway: EXP total lives on CharacterSheet and Feat#isEligible takes only a
    //  Character, one of the CharacterSheet-side clauses docs/rules/talentos-index.md records.
    // TODO: Laços-de-Sangue — a master-and-progeny relation between Vampiros — has no
    //  representation, so there is nothing to undo.
    LACOS_ROMPIDOS(
            "Desfaz os Laços-de-Sangue, não precisando mais obedecer ao seu mestre. Vantagem em "
                    + "rolagens de Perícias baseadas em Carisma e Atenção efetuadas contra outros "
                    + "Vampiros.",
            FeatRequirements.builder()
                    .requiredRace(Vampiro.class)
                    .requiredAwakenedTitles(1)
                    .build()),

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
    // TODO: widening Laços Rompidos' Vantagem to every target, and adding a GD reduction against
    //  Vampiros specifically, both need the target-scoping that clause already lacks — and
    //  resolveDifficultyReduction carries no opponent either.
    // TODO: gerar Prole needs the Laços-de-Sangue relation, and creating another Character is
    //  outside anything this core models.
    // TODO: same EXP-total disjunction as its prerequisite; only the 2-Título branch is recorded.
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
                    .requiredAwakenedTitles(2)
                    .build()) {
        @Override
        public int resolveAttributeBonus(final AttributeDomain domain, final Character character) {
            return character.getRace() instanceof Vampiro vampiro
                    && vampiro.getLineage().getBonusAttribute() == domain
                    ? MESTRE_VAMPIRO_ATTRIBUTE_BONUS : 0;
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
