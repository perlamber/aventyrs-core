package org.aventyrs.core.feat;

/**
 * Talentos Vampíricos — six <b>Poderes Vampíricos</b>, one that extends them, and three that
 * climb the Vampiro's own hierarchy.
 *
 * <p><b>No constant carries a mechanical effect, and two structural facts explain all eleven.</b>
 *
 * <p><b>1. There is no Vampiro race in this core.</b> Every constant here reads "Apenas
 * personagens da Raça Vampiro", and {@code org.aventyrs.core.race} has no such class — the race
 * is described in a section of the ruleset this core has not transcribed. So {@code
 * FeatRequirements#requiredRace} is left unset throughout and <b>the whole tree is currently
 * ungated</b>, which is much looser than any other racial tree. {@code requiredCreatureType} is
 * deliberately not used as a stand-in: a Vampiro would presumably be {@code MONSTRUOSO}, but
 * gating on that would open these Talentos to every Troll, Goblin and Ogro, which is further
 * from the text than leaving the clause unexpressed. Add {@code requiredRace(Vampiro.class)} to
 * all eleven the day the race exists.
 *
 * <p><b>2. A "Poder Vampírico" is an activated power with a Duração, not a permanent trait.</b>
 * {@link #PODER_VAMPIRICO_DURADOURO} is the evidence — it exists solely to raise "a Duração de
 * seus Poderes Vampíricos" to 2 Rodadas — and {@link #CELERIDADE_VAMPIRICA} confirms it by
 * granting its PA "temporariamente". So the six {@code Poder Vampírico –} constants are all
 * temporary buffs, and none may be granted as a standing bonus however expressible its figure
 * looks. {@link #OSTEOMANCIA}'s "+2 em suas Defesas" would otherwise map straight onto {@code
 * Feat#resolveDefenseBonus}; granting it there would hand a Vampiro a permanent bonus the rules
 * text lends for a Rodada.
 *
 * <p>That reading is an inference, not stated outright — the source never prints an activation
 * cost or a base Duração for these powers. It is recorded here because it is what keeps six
 * otherwise-implementable clauses from being over-granted, and it is the first thing to re-check
 * if this tree is ever revisited.
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
            FeatRequirements.builder().build()),

    /**
     * "Você recebe Vantagem em rolagens de Ataque e Dano e estende seu Roubo de Vida Racial e de
     * Poderes Vampíricos a todos os seus ataques Corpo-a-Corpo e Magias."
     */
    // TODO: a Poder Vampírico is temporary (class javadoc), so even the Ataque half — which would
    //  otherwise fit Feat#resolveSkillRollBonus on isAttackSkill() — cannot be granted standing.
    // TODO: a Vantagem on a *dano* roll has no Feat hook; resolveDamageBonus lives on
    //  SkillCompetencyAbility and EgoAdvantage, reached through a skill Interaction.
    // TODO: extending Roubo de Vida needs a Feat life-steal hook, which does not exist —
    //  LifeStealService reads an active LifeSteal effect on the sheet plus
    //  AttributeAbility#resolveLifeStealBonus, and LifeSteal is deliberately kept off ModifierType.
    ARMAMENTO_DE_ORLOK(
            "Você recebe Vantagem em rolagens de Ataque e Dano e estende seu Roubo de Vida Racial "
                    + "e de Poderes Vampíricos a todos os seus ataques Corpo-a-Corpo e Magias "
                    + "capazes de causar danos diretamente.",
            FeatRequirements.builder().build()),

    /**
     * "Você adquire a capacidade de se transformar em animais ou névoa… Escolha 2 Formas
     * Metamórficas."
     */
    // TODO: needs a form state — the same missing piece DraconicoFeat#DRACONATO, HomemFera's
    //  Forma Híbrida and Gorgona's own forms are blocked on.
    // TODO: the choice of two Formas from a table of six could be recorded now (a choice-carrying
    //  AbstractFeat subclass — see FocoEmPericiaFeat), but each Forma grants its own Arma Natural
    //  (no weapon catalog) plus its own ability — among them a Movimento Base Vertical, a
    //  Movimento Base de Voo, a Multiplicador de PV, a Corrente de Efeitos and an outright
    //  immunity to physical damage. Every one of those is separately blocked.
    // TODO: "Dampiros escolhem 1, Rakshasa escolhem 4" names two Vampiro sub-races that, like the
    //  Vampiro race itself, have no representation here.
    METAMORFOSE_DRACULEA(
            "Você adquire a capacidade de se transformar em animais ou névoa; enquanto usando "
                    + "metamorfose seus Equipamentos se adaptam ao seu corpo, itens defensivos "
                    + "continuam concedendo seus benefícios, armas não podem ser utilizadas e são "
                    + "substituídas por armas naturais. Escolha 2 Formas Metamórficas entre Aranha "
                    + "Gigante, Cavalo de Chifres, Lobo Dentes-de-Sabre, Morcego Atroz, Névoa e "
                    + "Serpente Espinhosa (Dampiros podem escolher apenas 1, Rakshasa podem "
                    + "escolher 4, mas não podem se transformar em Névoa).",
            FeatRequirements.builder().build()),

    /**
     * "Você recebe Bônus Racial de +1 em Carisma e Instinto. Estes Bônus aumentam em +1 para cada
     * Título Aventyr que você possuir."
     */
    // TODO: a Talento cannot grant an Atributo bonus — see BestialFeat's class javadoc — and this
    //  is a Poder Vampírico besides, so the grant is temporary rather than standing. Note the
    //  clause calls it a "Bônus Racial", which reads permanent and sits awkwardly with the
    //  Duração reading; flagged rather than resolved.
    DOM_DE_MIRCALLA(
            "Você recebe Bônus Racial de +1 em Carisma e Instinto. Estes Bônus aumentam em +1 "
                    + "para cada Título Aventyr que você possuir.",
            FeatRequirements.builder().build()),

    /**
     * "Você pode moldar seus ossos para melhor proteger seu corpo, recebendo Bônus de +2 em suas
     * Defesas. Os Bônus aumentam em +1 para cada Título Aventyr que você possuir."
     */
    // TODO: the figure maps straight onto Feat#resolveDefenseBonus and scales off
    //  Character#getAllTitles(), so this is the tree's most nearly-implementable clause — and it
    //  is withheld only because a Poder Vampírico is temporary (class javadoc). Granting it here
    //  would turn a Rodada-long buff into a permanent +2 to both Defesas. The day an activation
    //  mechanism exists, this is a Blessing of ModifierType.DEFESAS rather than a Feat hook.
    OSTEOMANCIA(
            "Você pode moldar seus ossos para melhor proteger seu corpo, recebendo Bônus de +2 em "
                    + "suas Defesas. Os Bônus em Defesas aumentam em +1 para cada Título Aventyr "
                    + "que você possuir.",
            FeatRequirements.builder().build()),

    /**
     * "A cada Rodada você recupera 1PV para cada personagem vivo em Distância Muito Curta que
     * esteja ferido."
     */
    // TODO: a Poder Vampírico, so temporary. Beyond that it needs a per-Rodada recovery that
    //  counts qualifying nearby combatants: SceneContext#getEnemiesWithin/getAlliesWithin gives
    //  the neighbours, but "vivo" needs the living/undead classification this core lacks
    //  (CreatureType has no vitality tag) and "ferido" needs each neighbour's current PV, which
    //  only HitPointsService can answer — a service an enum constant cannot reach.
    // TODO: CharacterSheet#startTurn is still a no-op with no hook to fire a per-Rodada effect
    //  from.
    PRESENCA_DE_CARMILLA(
            "Você é capaz de roubar sangue dos vivos próximos a você. A cada Rodada você recupera "
                    + "1PV para cada personagem vivo em Distância Muito Curta que esteja ferido "
                    + "(que tenha perdido 1 ou mais PV), este é um efeito de Roubo de Vida. A "
                    + "Distância aumenta em +1 nível para cada Título Aventyr que você possuir.",
            FeatRequirements.builder().build()),

    /**
     * "A Duração de seus Poderes Vampíricos aumenta para 2 Rodadas."
     *
     * <p>The constant that establishes the whole tree's reading — see the class javadoc. Its
     * Pré-requisito ("2 outros Talentos de Poderes Vampíricos") is real and enforced.
     */
    // TODO: extends a Duração that does not exist, because no Poder Vampírico can be activated.
    // TODO: its requiredFeatCategory counts every Talento of this tree, where the text says "de
    //  Poderes Vampíricos" — the six constants whose name carries that prefix. Laços Rompidos,
    //  Mestre Vampiro and this constant itself are not Poderes, so the gate is looser than
    //  written; FeatCategory is one level coarser than the clause needs.
    PODER_VAMPIRICO_DURADOURO(
            "A Duração de seus Poderes Vampíricos aumenta para 2 Rodadas. Adicionalmente a "
                    + "Duração de seus Poderes aumentam em +1 Rodada para cada Título Aventyr que "
                    + "você possuir.",
            FeatRequirements.builder()
                    .requiredFeatCategory(FeatCategory.VAMPIRICO)
                    .requiredFeatCategoryCount(2)
                    .build()),

    /**
     * "Desfaz os Laços-de-Sangue… Vantagem em rolagens de Perícias baseadas em Carisma e Atenção
     * efetuadas contra outros Vampiros."
     */
    // TODO: the Vantagem is scoped two ways this core cannot express at once — by AttributeDomain
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
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Você chega no ponto evolutivo mais alto da sua raça… Você agora pode gerar Prole, criando
     * outros Vampiros."
     */
    // TODO: raising "seu Bônus Racial em Atributo ganho por ser um Vampiro" needs both a Vampiro
    //  race to have granted one and a Talento-side Atributo hook, neither of which exists.
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
                    .requiredFeat(LACOS_ROMPIDOS)
                    .requiredAwakenedTitles(2)
                    .build()),

    /**
     * "Você recebe temporariamente +1 PA. Enquanto afetado por Celeridade seu Movimento Base
     * aumenta em +1 para cada Título Aventyr que você possuir."
     */
    // TODO: the word "temporariamente" is what confirms the whole tree's Duração reading — so
    //  neither half may use Feat#resolveActionPointsIncrease (explicitly for a permanent grant)
    //  nor resolveMovementIncrease. Both are Blessings of ModifierType.ACTION_POINTS/MOVEMENT
    //  once an activation mechanism exists, which is exactly the split those hooks' javadocs draw.
    CELERIDADE_VAMPIRICA(
            "Você recebe temporariamente +1 PA. Enquanto afetado por Celeridade seu Movimento "
                    + "Base aumenta em +1 para cada Título Aventyr que você possuir.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .build()),

    /** "Seu Roubo de Vida Racial aumenta em +1 para cada Título Desperto que você possuir." */
    // TODO: needs a Vampiro race to have granted a Roubo de Vida Racial in the first place, and a
    //  Feat life-steal hook to raise it — LifeSteal is deliberately kept off ModifierType, so a
    //  Talento has no way to contribute to LifeStealService's total.
    SEDE_DE_SANGUE(
            "Seu Roubo de Vida Racial aumenta em +1 para cada Título Desperto que você possuir.",
            FeatRequirements.builder()
                    .requiredAwakenedTitles(1)
                    .build());

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
