package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.race.NascidoDoDragao;

/**
 * Talentos Dracônicos — the Nascido do Dragão's own tree, and the ruleset's route to the
 * physical inheritance of a true Dragon: natural weapons, wings, a breath weapon, and finally a
 * full draconic form.
 *
 * <p>Only one clause is real — {@link #ASAS_DE_DRAGAO}'s +2 Defesas, which is unconditional
 * because the wings are always there. Everything else in the tree hangs off one of two missing
 * systems, both already recorded on {@code NascidoDoDragao} itself: <b>no Arma Natural</b> (no
 * weapon catalog is authored, and nothing marks a weapon as natural) and <b>no flight or form
 * state</b>.
 *
 * <p><b>"Recém-criados" is not modelled.</b> Two constants restrict themselves to a Nascido do
 * Dragão "recém-criado", i.e. acquirable only at character creation. Nothing anywhere tracks
 * when a Talento was acquired, so that half of their Pré-requisito is dropped and only the race
 * clause is enforced — the gate is looser than the text, never stricter, the same direction
 * every other unexpressible clause in this catalog errs in.
 */
public enum DraconicoFeat implements Feat {

    /**
     * "Assim como os Dragões você possui um repertório de Armas Naturais, escolha duas armas
     * entre: Chifres Poderosos, Cauda Chicote, Garras Afiadas e Presas Longas."
     */
    // TODO: no weapon catalog is authored (only ArmorItem), and nothing marks a weapon as an
    //  Arma Natural — the two-markers-missing gap CLAUDE.md's "Classifying an attack as
    //  Desarmado/Arma Natural" row names. The pick-two-of-four choice would additionally need
    //  the AcquiredChoice mechanism, which no Talento uses today: a Feat is a flat catalog
    //  constant carrying no per-acquisition data (see CLAUDE.md's one-enum-per-tree rationale).
    ARMAMENTO_DRACONICO(
            "Assim como os Dragões você possui um repertório de Armas Naturais, escolha duas "
                    + "armas entre: Chifres Poderosos, Cauda Chicote, Garras Afiadas e Presas "
                    + "Longas. Você possui as Armas Naturais escolhidas.",
            FeatRequirements.builder()
                    .requiredRace(NascidoDoDragao.class)
                    .build()),

    /**
     * "Você tem asas e possui Movimento Base de Voo… Asas de Dragão são extremamente grandes,
     * rígidas e resistentes, por isso concedem Bônus de +2 as suas Defesas, mas o impede de usar
     * Equipamentos do tipo Capa." The Defesas half is real.
     *
     * <p>Unconditional, and that is what makes it expressible where every other clause in this
     * tree is not: the wings are a permanent feature of the body, not something spent into or
     * transformed into, so the bonus applies whether or not the holder is flying. It covers
     * <b>both</b> DF and DM — the text says "suas Defesas", the broad form.
     */
    // TODO: the flight half needs a flight state and a Movimento Base de Voo, neither of which
    //  exists — see Aviano's own Braços Alados. Note the PD cost, its per-Título reduction and
    //  the 1d6 + metade do Vigor Duração are all exact figures with nothing to apply them to.
    // TODO: "impede de usar Equipamentos do tipo Capa" needs an equipment *restriction*
    //  mechanism. Character#equip validates nothing (ItemCategory.CLOAK exists, but no rule
    //  anywhere refuses an item), so the malus that pays for this bonus is currently free.
    ASAS_DE_DRAGAO(
            "Você tem asas e possui Movimento Base de Voo. Iniciar uma ação de voo em situações "
                    + "estressantes, como as Cenas de Combate, exige o uso de 4PD. Este Custo é "
                    + "reduzido em -2 para cada Título Aventyr que o personagem possua. A Duração "
                    + "do Efeito de Voo é igual à 1d6+Metade do Vigor Rodadas. Asas de Dragão são "
                    + "extremamente grandes, rígidas e resistentes, por isso concedem Bônus de +2 "
                    + "as suas Defesas, mas o impede de usar Equipamentos do tipo Capa.",
            FeatRequirements.builder()
                    .requiredRace(NascidoDoDragao.class)
                    .build()) {
        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character) {
            return ASAS_DEFENSE_BONUS;
        }
    },

    /**
     * "Você tem a Arma Natural: Arma de Sopro e é capaz de soprar energia Elemental como um
     * Dragão Verdadeiro. A Margem Crítica Menor aumenta em +1 e o dano do Sopro aumenta em +1d6
     * para cada Título Aventyr Desperto."
     */
    // TODO: no Arma Natural concept, so there is no Sopro to roll — same gap as
    //  ARMAMENTO_DRACONICO. Its element would be NascidoDoDragao#getElementalLineage(), which is
    //  real data, but nothing consumes an elemental damage type either.
    // TODO: the Margem Crítica half needs resolveCriticalMarginIncrease, which lives on
    //  EgoAdvantage/AttributeAbility/SkillCompetencyAbility but not on Feat — and it would have
    //  to be scoped to this one weapon's rolls, which is the "this one delivered attack"
    //  scoping gap.
    SOPRO_DE_DRAGAO(
            "Você tem a Arma Natural: Arma de Sopro e é capaz de soprar energia Elemental como um "
                    + "Dragão Verdadeiro. A Margem Crítica Menor aumenta em +1 e o dano do Sopro "
                    + "aumenta em +1d6 para cada Título Aventyr Desperto.",
            FeatRequirements.builder()
                    .requiredRace(NascidoDoDragao.class)
                    .build()),

    /**
     * "Após usar seu Sopro de Dragão você emana uma aura de energia que te acompanha por 2
     * Rodadas… personagens adjacentes sofrem 2 pontos de Dano Físico Elemental."
     */
    // TODO: triggered by SOPRO_DE_DRAGAO, which does not exist yet.
    // TODO: recurring damage to everyone adjacent at the start of each of the holder's Turns is
    //  an outward, area-shaped effect nothing models: DamageService only ever computes damage
    //  *to* one target *from* an attacker, CharacterSheet#startTurn is still a no-op with no
    //  hook to fire from, and Área de Efeito has no footprint resolution (CLAUDE.md's "Area de
    //  Efeito" row, part (a)).
    AURA_DRACONICA(
            "Após usar seu Sopro de Dragão você emana uma aura de energia que te acompanhada por "
                    + "2 Rodadas. Durante a ativação da aura e no início de cada um dos seus "
                    + "Turnos subsequentes, enquanto a aura estiver ativa, personagens adjacentes "
                    + "sofrem 2 pontos de Dano Físico Elemental. O dano da aura aumenta em +1 e o "
                    + "alcance em +1UD para cada Título Aventyr Desperto.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.INSTINCT)
                    .requiredAttributeValue(3)
                    .requiredFeat(SOPRO_DE_DRAGAO)
                    .build()),

    /**
     * "Temporariamente você pode mudar sua forma física, se transformando em um dragão bípede,
     * abandonando quaisquer traços raciais existente."
     */
    // TODO: needs a form state — the same missing piece HomemFera's own Forma Híbrida is blocked
    //  on, here with the extra requirement that the form *suppresses* the holder's racial traits,
    //  which nothing can do (Race#getRacialAbilities() is read live on every roll with no way to
    //  suspend it).
    // TODO: "não poderá ser reativado até que passe por um Descanso Longo" needs a
    //  once-per-Descanso activation counter; CharacterSheet tracks Round-scoped TemporaryEffects,
    //  not activations, and RestService clears nothing of the kind.
    DRACONATO(
            "Temporariamente você pode mudar sua forma física, se transformando em um dragão "
                    + "bípede, abandonando quaisquer traços raciais existente. Transformar-se em "
                    + "um Draconato requer 3PA + 3PD, ao fazê-lo sua Categoria de Tamanho, Força "
                    + "e Foco aumentam em +2 para cada Título Aventyr que você possuir. A Duração "
                    + "na forma de Draconato é de 3 Rodadas, este Efeito não poderá ser reativado "
                    + "até que passe por um Descanso Longo.",
            FeatRequirements.builder()
                    .requiredRace(NascidoDoDragao.class)
                    .requiredAwakenedTitles(1)
                    .build());

    private static final int ASAS_DEFENSE_BONUS = 2;

    private final String description;
    private final FeatRequirements featRequirements;

    DraconicoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.DRACONICO;
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
