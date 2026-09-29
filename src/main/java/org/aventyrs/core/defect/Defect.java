package org.aventyrs.core.defect;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.DefeitoFeat;
import org.aventyrs.core.feat.FeatChoice;
import org.aventyrs.core.magic.ElementalType;

import java.util.Arrays;
import java.util.List;

/**
 * A Defeito — {@code docs/rules/defeitos-e-qualidades.txt} "Lista de Defeitos". Its effect at each
 * gravidade is a {@link DefeitoFeat} constant ({@link #effectAt}); the picks it asks for are {@link
 * #resolveChoices}. Held as a {@link HeldDefect}.
 */
public enum Defect {
    COMPORTAMENTO_EXCENTRICO("Comportamento Excêntrico",
            "Você possui hábitos incomuns, ou estranhos, e as coisas que você fala não faz sentido para a maioria das pessoas."),
    CORPO_FRAGIL("Corpo Frágil", ""),
    DESAPEGO_MATERIAL("Desapego Material",
            "Você não se importa com bens além do essencial, para você basta o mínimo para viver."),
    DEFICIENCIA_FISICA("Deficiência Física",
            "Um de seus membros é atrofiado, pouco desenvolvido ou ausente, por isso você tem dificuldades em realizar "
                    + "ações que o envolva.") {
        /** The limb; at Grave also Dependência's Atributo — "Força ou de Destreza (escolhido aleatoriamente)". */
        @Override
        public List<FeatChoice<?>> resolveChoices(final DefectSeverity severity, final Character character,
                                                  final List<Object> made) {
            FeatChoice<?> limb = FeatChoice.ofOne(Limb.class, Arrays.asList(Limb.values()));
            return severity == DefectSeverity.GRAVE
                    ? List.of(limb, FeatChoice.ofOne(AttributeDomain.class, List.of(AttributeDomain.STRENGTH, AttributeDomain.DEXTERITY)))
                    : List.of(limb);
        }
    },
    DEFICIENCIA_SENSORIAL("Deficiência Sensorial",
            "Você não tem uma audição, olfato ou visão bem desenvolvida, ou sofre de alguma deficiência em um destes sentidos.") {
        @Override
        public List<FeatChoice<?>> resolveChoices(final DefectSeverity severity, final Character character,
                                                  final List<Object> made) {
            return List.of(FeatChoice.ofOne(Sense.class, Arrays.asList(Sense.values())));
        }
    },
    DESCONEXAO_COM_O_AETHER("Desconexão com o AEther",
            "Você tem dificuldades para se conectar com o AEther, por isso possui dificuldades em manipular ou reter o "
                    + "Mana, a energia mágica."),
    DISTURBIO_DE_ATENCAO("Distúrbio de Atenção", ""),
    FOBIA("Fobia",
            "Você tem medo de algo relativamente comum e não consegue lidar bem com aquilo. Escolha entre um tipo de "
                    + "animal, raça ou situação.") {
        /** The object of the fobia — free text, see {@link #FREE_TEXT}. */
        @Override
        public List<FeatChoice<?>> resolveChoices(final DefectSeverity severity, final Character character,
                                                  final List<Object> made) {
            return List.of(FREE_TEXT);
        }
    },
    HERANCA_DE_GILGAMESH("Herança de Gilgamesh",
            "Gilgamesh sacrificou sua Centelha Aventyr, a dos seus homens e de seus descendentes para criar as Regalias, "
                    + "por isso aqueles de seu sangue tem dificuldades em Despertar."),
    MEMORIA_FRACA("Memória Fraca", "Você tem péssima memória e esquece das coisas que vivencia com certa facilidade."),
    VULNERABILIDADE("Vulnerabilidade",
            "Você tem uma sensibilidade maior, ou uma resistência menor, à certos tipos de feridas.") {
        /** The damage kind, then — for Danos Elementais — "dois elementos aleatórios". */
        @Override
        public List<FeatChoice<?>> resolveChoices(final DefectSeverity severity, final Character character,
                                                  final List<Object> made) {
            FeatChoice<?> kind = FeatChoice.ofOne(VulnerabilityKind.class, Arrays.asList(VulnerabilityKind.values()));
            return !made.isEmpty() && made.get(0) == VulnerabilityKind.ELEMENTAL
                    ? List.of(kind, new FeatChoice<>(ElementalType.class, 2, Arrays.asList(ElementalType.values())))
                    : List.of(kind);
        }
    },
    RESTRICAO_MORAL("Restrição Moral",
            "Você acredita nas regras e na lei, ou tem um código de honra rigoroso, por isso sempre segue algumas regras.");

    /**
     * A free-text pick — Fobia's object. Its empty option list means "any non-blank text", the one
     * {@link FeatChoice} whose options are not enumerable.
     */
    public static final FeatChoice<String> FREE_TEXT = new FeatChoice<>(String.class, 1, List.of());

    private final String displayName;
    private final String description;

    Defect(final String displayName, final String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /** This Defeito's effect at severity. */
    public DefeitoFeat effectAt(final DefectSeverity severity) {
        return DefeitoFeat.valueOf(name() + "_" + severity.name());
    }

    /**
     * The picks this Defeito asks for at severity, given the ones made so far (a later pick may depend
     * on an earlier one — Vulnerabilidade's Elementos). Answered in order into {@link HeldDefect#choices()}.
     * None by default.
     */
    public List<FeatChoice<?>> resolveChoices(final DefectSeverity severity, final Character character,
                                              final List<Object> made) {
        return List.of();
    }
}
