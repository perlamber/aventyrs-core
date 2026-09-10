package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.race.Aviano;
import org.aventyrs.core.skill.SkillType;

/**
 * Talentos Avianos — all four about <b>Voo</b>, which is the tree's defining feature and also
 * its defining gap.
 *
 * <p>Three of the four are catalog entries with enforced Pré-requisitos and no effect, blocked
 * on one shared missing system: <b>this core models no flight</b>. `Aviano`'s own Braços Alados
 * racial trait already records it — there is no flight state and no Movimento Base de Voo
 * distinct from the ordinary one. (The generic "spend a resource to enter a timed state"
 * transaction now exists — {@code ActiveAbilityService#activate} + {@code ActiveAbility}, with a
 * PA/PM/PV cost — but flight is a distance sub-stat plus a facing/altitude notion, not a
 * `TemporaryBonus`, and the PD cost these Talentos name is not among the supported ones.) Every
 * one of them adjusts a figure that does not exist, so none grants a partial effect.
 *
 * <p>{@link #VISAO_DA_VERDADE} is the exception, and the reason {@code
 * Feat#resolveDifficultyReduction} exists: its GD clause is the plain unconditional form, on a
 * named Perícia, with nothing about flight in it.
 */
public enum AvianoFeat implements Feat {

    /**
     * "A Duração de seu Efeito de Voo aumenta em +1d6 Rodadas. Seu Movimento Base de Voo aumenta
     * em +2UD para cada Título Aventyr Desperto que possuir."
     */
    // TODO: needs a flight state and a Movimento Base de Voo — see Aviano's own Braços Alados.
    //  Note the +2UD half must NOT be routed through resolveMovementIncrease: that hook feeds
    //  MovementService#getMovementBase, the character's ordinary ground movement, and this
    //  clause names Voo specifically. Vertical/swim/flight movement is a different sub-stat,
    //  the same distinction AtletismoCompetencyAbility#ALPINISTA_VELOZ/ANFIBIO already draw.
    // TODO: the +1d6 Duração is a die, and this core never rolls dice.
    CORACAO_ALADO(
            "A Duração de seu Efeito de Voo aumenta em +1d6 Rodadas. Seu Movimento Base de Voo "
                    + "aumenta em +2UD para cada Título Aventyr Desperto que possuir.",
            FeatRequirements.builder()
                    .requiredRace(Aviano.class)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Seus braços e asas se separam, agora você pode usar suas mãos livremente enquanto voando,
     * elas não são mais consideradas membros inábeis, mas seu Movimento Base de Voo é reduzido
     * em -3UD."
     */
    // TODO: needs a flight state, and needs the limb/anatomy concept Aviano's Braços Alados is
    //  itself blocked on — "membros inábeis" is not a condition anything models, so a Talento
    //  lifting it has nothing to lift. Both halves of this Talento are inert together, which is
    //  at least self-consistent: neither the penalty nor its price applies.
    BRACOS_LIVRES(
            "Seu corpo muda e seus braços e asas se separam, agora você pode usar suas mãos "
                    + "livremente enquanto voando, elas não são mais consideradas membros "
                    + "inábeis, mas seu Movimento Base de Voo é reduzido em -3UD.",
            FeatRequirements.builder()
                    .requiredRace(Aviano.class)
                    .attributeDomain(AttributeDomain.DEXTERITY)
                    .requiredAttributeValue(4)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Enquanto estiver consciente você pode voar sem limites de Duração, mesmo em Cenas
     * estressantes. Você pode desligar parte do seu cérebro e dormir voando em linha reta por
     * até Descanso Mínimo."
     */
    // TODO: needs a flight state. Removing a Duração limit presupposes the Duração.
    // TODO: the sleep-flying half additionally needs a sleep state, which nothing tracks — the
    //  same "no Fadiga/asfixia" gap Troll's own Sono de Pedra cites.
    ETERNO_VIAJANTE(
            "Enquanto estiver consciente você pode voar sem limites de Duração, mesmo em Cenas "
                    + "estressantes. Você pode desligar parte do seu cérebro e dormir voando em "
                    + "linha reta por até Descanso Mínimo, mas apenas se aparentar estar em "
                    + "segurança nos momentos iniciais.",
            FeatRequirements.builder()
                    .requiredFeat(CORACAO_ALADO)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "A GD de suas rolagens de Atenção é reduzida em -1 Nível, adicionalmente você pode enxergar
     * auras mágicas e personagens invisíveis." The GD half is real.
     *
     * <p>The first Talento in the catalog to reduce a roll's GD for real — the clause is
     * unconditional, names one Perícia, and depends on nothing this core lacks, which is exactly
     * the shape {@link Feat#resolveDifficultyReduction} was added for.
     */
    // TODO: seeing auras mágicas and invisible characters needs a vision/senses concept and an
    //  invisibility state, neither of which exists.
    VISAO_DA_VERDADE(
            "A GD de suas rolagens de Atenção é reduzida em -1 Nível, adicionalmente você pode "
                    + "enxergar auras mágicas e personagens invisíveis.",
            FeatRequirements.builder()
                    .requiredRace(Aviano.class)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveDifficultyReduction(final SkillType skillType, final Character character) {
            return skillType == SkillType.ATTENTION ? ATENCAO_DIFFICULTY_REDUCTION : 0;
        }
    };

    private static final int ATENCAO_DIFFICULTY_REDUCTION = 1;

    private final String description;
    private final FeatRequirements featRequirements;

    AvianoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.AVIANO;
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
