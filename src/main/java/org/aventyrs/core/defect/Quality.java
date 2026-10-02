package org.aventyrs.core.defect;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.FeatChoice;
import org.aventyrs.core.feat.QualidadeFeat;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillTraitCatalog;
import org.aventyrs.core.skill.SkillTraitKind;
import org.aventyrs.core.skill.SkillType;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * A Qualidade — {@code docs/rules/defeitos-e-qualidades.txt} "Lista de Qualidades". Each opposes one
 * {@link Defect} ({@link #getOpposes()}): "Não é possível adquirir uma Qualidade que seja opositora em
 * conceito ou efeito a um Defeito que você possua". Its effects are {@link QualidadeFeat} constants —
 * a Maior holds its Menor's too ({@link #effectsAt}).
 */
public enum Quality {
    CENTELHA_MAIOR("Centelha Maior", Defect.HERANCA_DE_GILGAMESH),
    DESTINADO_A_FORTUNA("Destinado a Fortuna", Defect.DESAPEGO_MATERIAL),
    ESCOLHIDO_DA_MAGIA("Escolhido da Magia", Defect.DESCONEXAO_COM_O_AETHER),
    INTELECTO_SUPERIOR("Intelecto Superior", Defect.MEMORIA_FRACA),
    OPORTUNISTA_NATO("Oportunista Nato", Defect.RESTRICAO_MORAL),
    RADIANTE("Radiante", Defect.COMPORTAMENTO_EXCENTRICO),
    RESILIENCIA_HEROICA("Resiliência Heroica", Defect.FOBIA),
    RESISTENCIA_ATIPICA("Resistência Atípica", Defect.VULNERABILIDADE) {
        /** "Energia Elemental (1 único elemento à sua escolha), Energia Profana ou Energia Divina". */
        @Override
        public List<FeatChoice<?>> resolveChoices(final QualityClass qualityClass, final Character character,
                                                  final List<Object> made) {
            FeatChoice<?> kind = FeatChoice.ofOne(EnergyKind.class, Arrays.asList(EnergyKind.values()));
            return !made.isEmpty() && made.get(0) == EnergyKind.ELEMENTAL
                    ? List.of(kind, FeatChoice.ofOne(ElementalType.class, Arrays.asList(ElementalType.values())))
                    : List.of(kind);
        }
    },
    SAUDE_DE_FERRO("Saúde de Ferro", Defect.CORPO_FRAGIL),
    SENTIDO_SUPERIOR("Sentido Superior", Defect.DEFICIENCIA_SENSORIAL) {
        @Override
        public List<FeatChoice<?>> resolveChoices(final QualityClass qualityClass, final Character character,
                                                  final List<Object> made) {
            return List.of(FeatChoice.ofOne(Sense.class, Arrays.asList(Sense.values())));
        }
    },
    SEXTO_SENTIDO("Sexto Sentido", Defect.DISTURBIO_DE_ATENCAO) {
        /** Precognição's "uma Especialização ou Habilidade de Competência de Atenção" — one not yet held. */
        @Override
        public List<FeatChoice<?>> resolveChoices(final QualityClass qualityClass, final Character character,
                                                  final List<Object> made) {
            if (qualityClass != QualityClass.MAIOR) {
                return List.of();
            }
            List<SkillTrait> options = Stream.concat(
                            SkillTraitCatalog.traitsOf(SkillType.ATTENTION, SkillTraitKind.SPECIALIZATION).stream(),
                            SkillTraitCatalog.traitsOf(SkillType.ATTENTION, SkillTraitKind.COMPETENCY_ABILITY).stream())
                    .filter(trait -> character == null || !Superacao.holds(character, trait))
                    .toList();
            return List.of(FeatChoice.ofOne(SkillTrait.class, options));
        }
    },
    TENDENCIA_ATLETICA("Tendência Atlética", Defect.DEFICIENCIA_FISICA) {
        /** Divinal's "Escolha entre Força ou Destreza" — the Habilidade itself fills a bonus slot later. */
        @Override
        public List<FeatChoice<?>> resolveChoices(final QualityClass qualityClass, final Character character,
                                                  final List<Object> made) {
            return qualityClass == QualityClass.MAIOR
                    ? List.of(FeatChoice.ofOne(AttributeDomain.class, List.of(AttributeDomain.STRENGTH, AttributeDomain.DEXTERITY)))
                    : List.of();
        }
    };

    private final String displayName;
    private final Defect opposes;

    Quality(final String displayName, final Defect opposes) {
        this.displayName = displayName;
        this.opposes = opposes;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** The Defeito this Qualidade may not be held beside. */
    public Defect getOpposes() {
        return opposes;
    }

    /** Every effect of this Qualidade at qualityClass — a Maior's own and, cumulatively, its Menor's. */
    public List<QualidadeFeat> effectsAt(final QualityClass qualityClass) {
        QualidadeFeat menor = QualidadeFeat.valueOf(name() + "_MENOR");
        return qualityClass == QualityClass.MAIOR
                ? List.of(menor, QualidadeFeat.valueOf(name() + "_MAIOR"))
                : List.of(menor);
    }

    /** The picks this Qualidade asks for at qualityClass, given the ones made so far. None by default. */
    public List<FeatChoice<?>> resolveChoices(final QualityClass qualityClass, final Character character,
                                              final List<Object> made) {
        return List.of();
    }
}
