package org.aventyrs.core.feat;

import org.aventyrs.core.character.MovementMode;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.race.Aviano;
import org.aventyrs.core.skill.SkillType;

/**
 * Talentos Avianos — all four about <b>Voo</b>, which is the tree's defining feature and also
 * its defining gap.
 *
 * <p><b>The Movimento Base de Voo is real</b> ({@code MovementMode#FLIGHT}, granted by {@code
 * Aviano}'s Braços Alados), so {@link #CORACAO_ALADO}'s +2UD per Título and {@link
 * #BRACOS_LIVRES}' -3UD land on it. What stays missing is <b>flying as a timed state</b> — the PD
 * cost, the dice-rolled Duração, the sleep-flying — since whether a character is flying is the
 * caller's {@code EnvironmentalState#flying}, not something this core enters or ends.
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
    // The +2UD per Título is real, on the flight axis (Feat#resolveModeMovementIncrease), which
    // Aviano's own Braços Alados grants.
    // TODO: the +1d6 Duração is a die, and this core never rolls dice.
    CORACAO_ALADO(
            "A Duração de seu Efeito de Voo aumenta em +1d6 Rodadas. Seu Movimento Base de Voo "
                    + "aumenta em +2UD para cada Título Aventyr Desperto que possuir.",
            FeatRequirements.builder()
                    .requiredRace(Aviano.class)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveModeMovementIncrease(final MovementMode mode, final Character character) {
            return mode == MovementMode.FLIGHT ? CORACAO_ALADO_FLIGHT_PER_TITLE * character.getAllTitles().size() : 0;
        }
    },

    /**
     * "Seus braços e asas se separam, agora você pode usar suas mãos livremente enquanto voando,
     * elas não são mais consideradas membros inábeis, mas seu Movimento Base de Voo é reduzido
     * em -3UD."
     */
    // The -3UD on the flight axis is real (Feat#resolveModeMovementIncrease).
    // TODO: the hands half needs the limb/anatomy concept Aviano's Braços Alados is blocked on —
    //  "membros inábeis" is not a condition anything models, so there is nothing to lift. ⚠️ So
    //  the Talento's price currently applies without its benefit.
    BRACOS_LIVRES(
            "Seu corpo muda e seus braços e asas se separam, agora você pode usar suas mãos "
                    + "livremente enquanto voando, elas não são mais consideradas membros "
                    + "inábeis, mas seu Movimento Base de Voo é reduzido em -3UD.",
            FeatRequirements.builder()
                    .requiredRace(Aviano.class)
                    .attributeDomain(AttributeDomain.DEXTERITY)
                    .requiredAttributeValue(4)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public int resolveModeMovementIncrease(final MovementMode mode, final Character character) {
            return mode == MovementMode.FLIGHT ? BRACOS_LIVRES_FLIGHT_MALUS : 0;
        }
    },

    /**
     * "Enquanto estiver consciente você pode voar sem limites de Duração, mesmo em Cenas
     * estressantes. Você pode desligar parte do seu cérebro e dormir voando em linha reta por
     * até Descanso Mínimo."
     */
    // TODO: removing the flight Duração limit — the Movimento Base de Voo exists now, but flying
    //  as a timed state with a PD cost and a Duração is not modelled (whether one is flying is
    //  the caller's EnvironmentalState#flying), so there is no limit to remove.
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

    /** CORACAO_ALADO's "+2UD para cada Título Aventyr Desperto". */
    private static final int CORACAO_ALADO_FLIGHT_PER_TITLE = 2;

    /** BRACOS_LIVRES' "seu Movimento Base de Voo é reduzido em -3UD". */
    private static final int BRACOS_LIVRES_FLIGHT_MALUS = -3;

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
