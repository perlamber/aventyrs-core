package org.aventyrs.core.background;

import lombok.Getter;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.AntecedenteFeat;
import org.aventyrs.core.skill.SkillTraitKind;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.atletismo.AtletismoCompetencyAbility;
import org.aventyrs.core.skill.attention.AttentionSpecialization;
import org.aventyrs.core.skill.artes.ArtesSpecialization;
import org.aventyrs.core.skill.conhecimentos.ConhecimentosSpecialization;
import org.aventyrs.core.skill.dirigirecavalgar.DirigirECavalgarSpecialization;
import org.aventyrs.core.skill.empatiaselvagem.EmpatiaSelvagemSpecialization;
import org.aventyrs.core.skill.furtividade.FurtividadeSpecialization;
import org.aventyrs.core.skill.medicinaecura.MedicinaECuraSpecialization;
import org.aventyrs.core.skill.profissao.ProfissaoSpecialization;

import java.util.List;
import java.util.Set;

import static org.aventyrs.core.background.GraduationGrant.choose;
import static org.aventyrs.core.background.GraduationGrant.fixed;
import static org.aventyrs.core.background.TraitGrants.anyOf;
import static org.aventyrs.core.background.TraitGrants.anyOfIfTrained;
import static org.aventyrs.core.background.TraitGrants.attackSkills;
import static org.aventyrs.core.background.TraitGrants.ifTrained;
import static org.aventyrs.core.background.TraitGrants.of;
import static org.aventyrs.core.skill.SkillType.ATAQUE_A_DISTANCIA;
import static org.aventyrs.core.skill.SkillType.ATAQUE_CORPO_A_CORPO;
import static org.aventyrs.core.skill.SkillType.ARTES;
import static org.aventyrs.core.skill.SkillType.ATLETISMO;
import static org.aventyrs.core.skill.SkillType.ATTENTION;
import static org.aventyrs.core.skill.SkillType.CONHECIMENTOS;
import static org.aventyrs.core.skill.SkillType.DIRIGIR_E_CAVALGAR;
import static org.aventyrs.core.skill.SkillType.DOMINIO_DO_MANA;
import static org.aventyrs.core.skill.SkillType.EMPATIA_SELVAGEM;
import static org.aventyrs.core.skill.SkillType.FURTIVIDADE;
import static org.aventyrs.core.skill.SkillType.MEDICINA_E_CURA;
import static org.aventyrs.core.skill.SkillType.PROFISSAO;

/**
 * The Antecedentes de Naturalidade — {@code antecedentes.txt} "ANTECEDENTES DE NATURALIDADE".
 *
 * <p>Only the authored ones: the Estados Federados (Byakko-Uuguul, Genbu-Chulshin, Seiryuu-Iái'zi,
 * Suzaku-Shusshin), Grurton (Lunari, Solari) and Anima are marked "(não disponível)" in the rules
 * text, with XX/YY/ZZ placeholders for every clause, so none is listed.
 *
 * <p>"Se treinado em X recebe a Especialização Y" is trained in X at all at the moment the
 * Antecedente is picked, this Antecedente's own +1 included — see {@link Background#resolveTraitGrants}.
 */
@Getter
public enum OriginBackground implements Background {

    DECIEMBRANO("Deciembrano", "Harenai",
            "De natureza ilhéu e cultura naval, os Deciembranos vivem das águas por gerações e consideram "
                    + "partes de si mesmos, além de possuírem forte conexão com animais marinhos.",
            AntecedenteFeat.DECIEMBRANO,
            List.of(fixed(ATLETISMO), choose(1, DIRIGIR_E_CAVALGAR, EMPATIA_SELVAGEM))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(TraitGrants.fixed(trained, AtletismoCompetencyAbility.ANFIBIO),
                    ifTrained(trained, DIRIGIR_E_CAVALGAR, DirigirECavalgarSpecialization.AQUATICOS),
                    ifTrained(trained, EMPATIA_SELVAGEM, EmpatiaSelvagemSpecialization.ANIMAIS_MUNDANOS));
        }
    },

    ELDURIANO("Elduriano", "Elduriano",
            "Considerado Terra Santa e o país do progresso, seus habitantes estão acostumados com a presença "
                    + "de itens e veículos tecnológicos, além de estudarem muito sobre as culturas divinas.",
            AntecedenteFeat.ELDURIANO,
            List.of(choose(2, CONHECIMENTOS, PROFISSAO, DIRIGIR_E_CAVALGAR))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(ifTrained(trained, CONHECIMENTOS, ConhecimentosSpecialization.COSMOLOGIA),
                    ifTrained(trained, DIRIGIR_E_CAVALGAR, DirigirECavalgarSpecialization.VEICULOS_TECNOLOGICOS),
                    ifTrained(trained, PROFISSAO, ProfissaoSpecialization.MECANICA));
        }
    },

    HARENAI("Harenai", "Harenai",
            "O Povo do Deserto. Embora tenham cidades estacionárias próximas ao mar, aos oásis e aos bolsões de "
                    + "Areia Negra, sua cultura nômade é forte e presente. Alegres e conectados as artes, são "
                    + "conhecidos por sua alquimia e o domínio do Xajah.",
            AntecedenteFeat.HARENAI,
            List.of(choose(2, ARTES, CONHECIMENTOS, MEDICINA_E_CURA))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(ifTrained(trained, ARTES, ArtesSpecialization.MUSICA),
                    ifTrained(trained, CONHECIMENTOS, ConhecimentosSpecialization.NATUREZA),
                    ifTrained(trained, MEDICINA_E_CURA, MedicinaECuraSpecialization.ALQUIMIA));
        }
    },

    JULLYANO("Jullyano", "Harenai",
            "Um local de difícil sobrevivência, onde quem domina os Oasis, Labirintos e Tecnologias se "
                    + "sobressaem sobre os menos favorecidos, criando então um estilo de vida dependente e "
                    + "especializado em buscar por estes locais e recursos, ou morrer tentando.",
            AntecedenteFeat.JULLYANO,
            List.of(choose(2, ATTENTION, FURTIVIDADE, PROFISSAO))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(ifTrained(trained, ATTENTION, AttentionSpecialization.INVESTIGAR),
                    ifTrained(trained, FURTIVIDADE, FurtividadeSpecialization.MAESTRIA_DA_OCULTACAO),
                    ifTrained(trained, PROFISSAO, ProfissaoSpecialization.MECANICA));
        }
    },

    NORTENHO("Nortenho", "Nortês",
            "Os domínios do Senhor das Guerras do Norte, Ymir, e seus filhos Anões e Gigantes. Um local repleto "
                    + "de criaturas enormes e vorazes, de clima gélido intenso e lar de povos tribais e clãs que "
                    + "vivem em constante conflitos por rixas culturais, territórios ou recursos.",
            AntecedenteFeat.NORTENHO,
            List.of(choose(1, ATAQUE_A_DISTANCIA, ATAQUE_CORPO_A_CORPO), choose(1, ATTENTION, FURTIVIDADE))) {
        /** "Recebem uma Especialização e uma Habilidade de Competência da Perícia de Ataque escolhida." */
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            List<SkillType> attack = attackSkills(graduationSkills);
            return of(anyOf(trained, SkillTraitKind.SPECIALIZATION, 1, attack),
                    anyOf(trained, SkillTraitKind.COMPETENCY_ABILITY, 1, attack));
        }
    },

    OFI("Ofí", "Continental, dialeto Ofí",
            "O reino dos aventureiros e exploradores. A vida dos Ofís gira em torno das aventuras e "
                    + "explorações, seja em suas guildas ou naqueles que vivem de suas demandas.",
            AntecedenteFeat.OFI,
            List.of(choose(2, ATTENTION, DIRIGIR_E_CAVALGAR, PROFISSAO))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(ifTrained(trained, ATTENTION, AttentionSpecialization.SENTIDOS_APURADOS),
                    anyOfIfTrained(trained, SkillTraitKind.SPECIALIZATION, DIRIGIR_E_CAVALGAR),
                    TraitGrants.trained(trained, PROFISSAO)
                            ? TraitGrants.choose(trained, 1, List.of(ProfissaoSpecialization.ALFAIATARIA_E_CURTUME,
                                    ProfissaoSpecialization.METALURGIA, ProfissaoSpecialization.JOALHERIA))
                            : null);
        }
    },

    SUDITO_DO_DRAGAO("Súdito do Dragão", "Dracônico Veldoran",
            "Nascidos nas florestas labirínticas e mágicas Feéricas, abençoados no nascimento por Gaea e seus "
                    + "filhos, os súditos do Dragão são um povo fortemente ligado as culturas mágicas e naturais.",
            AntecedenteFeat.SUDITO_DO_DRAGAO,
            List.of(fixed(CONHECIMENTOS), fixed(EMPATIA_SELVAGEM))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(TraitGrants.fixed(trained, ConhecimentosSpecialization.NATUREZA),
                    TraitGrants.fixed(trained, ConhecimentosSpecialization.METAMAGICO),
                    anyOf(trained, SkillTraitKind.SPECIALIZATION, 1, List.of(EMPATIA_SELVAGEM)),
                    anyOf(trained, SkillTraitKind.COMPETENCY_ABILITY, 1, List.of(CONHECIMENTOS, EMPATIA_SELVAGEM)));
        }
    },

    VASTARE("Vastare", "Vastare",
            "Herdeiros das culturas feéricas e criadores das principais magias modernas, os Vastare são um povo "
                    + "conectado as artes Arcanas e a Natureza, aprendendo a se conectar ao ambiente e ao Mana desde "
                    + "o nascimento. Também são fortemente ligados a cultura e as artes e ao Arqueirismo.",
            AntecedenteFeat.VASTARE,
            List.of(choose(2, ATAQUE_A_DISTANCIA, CONHECIMENTOS, DOMINIO_DO_MANA))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(TraitGrants.trained(trained, CONHECIMENTOS)
                            ? TraitGrants.choose(trained, 1, List.of(ConhecimentosSpecialization.NATUREZA,
                                    ConhecimentosSpecialization.METAMAGICO))
                            : null,
                    anyOfIfTrained(trained, SkillTraitKind.COMPETENCY_ABILITY, DOMINIO_DO_MANA));
        }
    },

    NATUREZA_LONGINQUA("Natureza Longínqua", null,
            "Você é nativo de outro plano e está viajando por outras realidades por motivos particulares, mas "
                    + "por algum motivo particular Tellus parece ser favorável para o seu desenvolvimento.",
            AntecedenteFeat.NATUREZA_LONGINQUA,
            List.of(new GraduationGrant(List.of(SkillType.values()), 1))) {
        /** "Você recebe duas Especializações e uma Habilidade de Competência da Perícia Escolhida." */
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(anyOf(trained, SkillTraitKind.SPECIALIZATION, 2, graduationSkills),
                    anyOf(trained, SkillTraitKind.COMPETENCY_ABILITY, 1, graduationSkills));
        }
    },

    SEM_PATRIA_RECONHECIDA("Sem Pátria Reconhecida", null,
            "Você é bárbaro, indígena ou selvagem, e cresceu em uma região distante do progresso, apenas "
                    + "cercados dos hábitos de seu povo e de uma natureza selvagem.",
            AntecedenteFeat.SEM_PATRIA_RECONHECIDA,
            List.of(choose(2, DOMINIO_DO_MANA, FURTIVIDADE, ATAQUE_A_DISTANCIA, ATAQUE_CORPO_A_CORPO))) {
        /** "Você recebe uma Habilidade de Competência de uma das Perícias escolhidas." */
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(anyOf(trained, SkillTraitKind.COMPETENCY_ABILITY, 1, graduationSkills));
        }
    };

    private final String displayName;
    private final String additionalLanguage;
    private final String description;
    private final AntecedenteFeat benefit;
    private final List<GraduationGrant> graduationGrants;

    OriginBackground(final String displayName, final String additionalLanguage, final String description,
                     final AntecedenteFeat benefit, final List<GraduationGrant> graduationGrants) {
        this.displayName = displayName;
        this.additionalLanguage = additionalLanguage;
        this.description = description;
        this.benefit = benefit;
        this.graduationGrants = graduationGrants;
    }

    @Override
    public BackgroundKind getKind() {
        return BackgroundKind.ORIGIN;
    }
}
