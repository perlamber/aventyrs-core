package org.aventyrs.core.background;

import lombok.Getter;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.feat.AntecedenteFeat;
import org.aventyrs.core.feat.FeatChoice;
import org.aventyrs.core.magic.catalog.MagicTree;
import org.aventyrs.core.skill.SkillTraitKind;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.skill.attention.AttentionSpecialization;
import org.aventyrs.core.skill.conhecimentos.ConhecimentosSpecialization;
import org.aventyrs.core.skill.dirigirecavalgar.DirigirECavalgarSpecialization;
import org.aventyrs.core.skill.persuasao.PersuasaoSpecialization;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.aventyrs.core.background.GraduationGrant.choose;
import static org.aventyrs.core.background.GraduationGrant.fixed;
import static org.aventyrs.core.background.TraitGrants.anyOf;
import static org.aventyrs.core.background.TraitGrants.of;
import static org.aventyrs.core.skill.SkillType.ARTES;
import static org.aventyrs.core.skill.SkillType.ATAQUE_A_DISTANCIA;
import static org.aventyrs.core.skill.SkillType.ATAQUE_CORPO_A_CORPO;
import static org.aventyrs.core.skill.SkillType.ATLETISMO;
import static org.aventyrs.core.skill.SkillType.ATTENTION;
import static org.aventyrs.core.skill.SkillType.CONHECIMENTOS;
import static org.aventyrs.core.skill.SkillType.DIRIGIR_E_CAVALGAR;
import static org.aventyrs.core.skill.SkillType.DOMINIO_DO_MANA;
import static org.aventyrs.core.skill.SkillType.EMPATIA_SELVAGEM;
import static org.aventyrs.core.skill.SkillType.ESQUIVA_E_APARAR;
import static org.aventyrs.core.skill.SkillType.FURTIVIDADE;
import static org.aventyrs.core.skill.SkillType.MEDICINA_E_CURA;
import static org.aventyrs.core.skill.SkillType.PERSUASAO;
import static org.aventyrs.core.skill.SkillType.PROFISSAO;

/**
 * The Antecedentes de Carreira — {@code antecedentes.txt} "ANTECEDENTES DE CARREIRA": "relacionado ao
 * estilo de vida anterior do personagem, refere-se aos estudos, treinamentos, ou rotina prévia".
 *
 * <p>Readings, not certainties: Eremita's "Empatia Monstruosa" is the Perícia Empatia Selvagem (a
 * table ruling — Empatia Monstruosa is one of its Especializações, not a Perícia), and
 * Trombadinha's twice-stated Especialização is one.
 */
@Getter
public enum CareerBackground implements Background {

    ACADEMICO_AVENTYR("Acadêmico Aventyr",
            "Você foi treinado para Despertar como Aventyr e se tornar um aventureiro de elite.",
            AntecedenteFeat.ACADEMICO_AVENTYR, List.of()),

    ALDEAO("Aldeão",
            "Você veio de família comum e, antes de iniciar nas aventuras, seguia os ofícios da família.",
            AntecedenteFeat.ALDEAO, List.of(choose(1, PROFISSAO, EMPATIA_SELVAGEM))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(anyOf(trained, SkillTraitKind.COMPETENCY_ABILITY, 1, graduationSkills));
        }
    },

    APOSTADOR("Apostador",
            "Você vive e sempre viveu intensamente, acreditando que tudo é sorte e destino, se arriscando em "
                    + "jogos de azar, apostas ou quaisquer situações que fujam ao controle dos homens.",
            AntecedenteFeat.APOSTADOR, List.of()) {
        @Override
        public Map<EgoDomain, Integer> getEgoBonuses() {
            return Map.of(EgoDomain.SORTE, 1);
        }
    },

    APRENDIZ("Aprendiz",
            "Você era um aprendiz ou estudante e passou a primeira etapa da sua vida estudando e se especializando.",
            AntecedenteFeat.APRENDIZ, List.of(new GraduationGrant(List.of(SkillType.values()), 1))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(anyOf(trained, SkillTraitKind.SPECIALIZATION, 1, graduationSkills));
        }
    },

    ARTISTA("Artista",
            "Além de aventureiro você era um artista e perito em algumas das muitas formas de artes.",
            AntecedenteFeat.ARTISTA, List.of(fixed(ARTES))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(anyOf(trained, SkillTraitKind.COMPETENCY_ABILITY, 1, List.of(ARTES)));
        }
    },

    ATLETA("Atleta", "Você é ou era um atleta profissional.",
            AntecedenteFeat.ATLETA, List.of(fixed(ATLETISMO))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(anyOf(trained, SkillTraitKind.COMPETENCY_ABILITY, 1, List.of(ATLETISMO)));
        }
    },

    BATEDOR("Batedor",
            "Você foi treinado e atuava em um grupo, bando ou força militar como batedor.",
            AntecedenteFeat.BATEDOR, List.of(fixed(FURTIVIDADE), fixed(ATTENTION))),

    CACADOR("Caçador",
            "Você era um caçador e tirava seu sustento do abate de animais e pequenos monstros.",
            AntecedenteFeat.CACADOR, List.of(fixed(FURTIVIDADE), fixed(CONHECIMENTOS))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(TraitGrants.fixed(trained, ConhecimentosSpecialization.NATUREZA));
        }
    },

    COMERCIANTE("Comerciante",
            "Você descende de uma família de comerciantes e aprendeu o ofício da administração e negociação.",
            AntecedenteFeat.COMERCIANTE, List.of(fixed(PERSUASAO))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(TraitGrants.fixed(trained, PersuasaoSpecialization.NEGOCIACAO),
                    TraitGrants.fixed(trained, PersuasaoSpecialization.COMUNICACAO));
        }
    },

    CURANDEIRO("Curandeiro",
            "Você foi aprendiz de médicos ou curandeiros, por isso sabe as melhores formas de curar e cuidar de ferimentos.",
            AntecedenteFeat.CURANDEIRO, List.of(fixed(MEDICINA_E_CURA))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(anyOf(trained, SkillTraitKind.SPECIALIZATION, 1, List.of(MEDICINA_E_CURA)),
                    anyOf(trained, SkillTraitKind.COMPETENCY_ABILITY, 1, List.of(MEDICINA_E_CURA)));
        }
    },

    EREMITA("Eremita",
            "Antes de se aventurar você era um nômade que vivia isolado da civilização.",
            AntecedenteFeat.EREMITA, List.of(fixed(CONHECIMENTOS), fixed(EMPATIA_SELVAGEM))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(TraitGrants.fixed(trained, ConhecimentosSpecialization.NATUREZA));
        }
    },

    ESCUDEIRO("Escudeiro",
            "Você foi escudeiro ou pajem de um cavaleiro ou herói e agora segue os seus passos.",
            AntecedenteFeat.ESCUDEIRO, List.of(fixed(CONHECIMENTOS), fixed(DIRIGIR_E_CAVALGAR))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(TraitGrants.fixed(trained, ConhecimentosSpecialization.GEO_HISTORIA));
        }

        /** Aprendiz de Cavaleiro: "escolha entre Esquiva e Aparar ou uma Perícia de Ataque". */
        @Override
        public List<FeatChoice<?>> resolveBenefitChoices(final Character character) {
            return List.of(FeatChoice.ofOne(SkillType.class,
                    List.of(ESQUIVA_E_APARAR, ATAQUE_A_DISTANCIA, ATAQUE_CORPO_A_CORPO)));
        }
    },

    ESPIAO("Espião",
            "Você é ou foi treinado por um clã, família ou unidade militar para ser um espião.",
            AntecedenteFeat.ESPIAO, List.of(fixed(PERSUASAO))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(TraitGrants.fixed(trained, PersuasaoSpecialization.MENTIR_OU_OMITIR),
                    TraitGrants.fixed(trained, PersuasaoSpecialization.OBTER_INFORMACOES),
                    TraitGrants.fixed(trained, PersuasaoSpecialization.NEGOCIACAO));
        }
    },

    ESTUDIOSO_ARCANO("Estudioso Arcano",
            "Você estudou as artes Arcanas em uma Academia de Magias ou teve um tutor que lhe iniciou no Arcanismo.",
            AntecedenteFeat.ESTUDIOSO_ARCANO, List.of(choose(1, CONHECIMENTOS, DOMINIO_DO_MANA))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(anyOf(trained, SkillTraitKind.COMPETENCY_ABILITY, 1, graduationSkills));
        }

        /** Aprendizado Arcano: "Escolha uma Árvore de Magias". */
        @Override
        public List<FeatChoice<?>> resolveBenefitChoices(final Character character) {
            return List.of(FeatChoice.ofOne(MagicTree.class, Arrays.asList(MagicTree.values())));
        }
    },

    MARINHEIRO("Marinheiro",
            "Você era um pescador, pirata ou marinheiro antes de iniciar sua vida de aventuras.",
            AntecedenteFeat.MARINHEIRO, List.of(fixed(ATLETISMO), fixed(DIRIGIR_E_CAVALGAR))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(TraitGrants.fixed(trained, DirigirECavalgarSpecialization.AQUATICOS));
        }
    },

    NEGOCIADOR("Negociador",
            "Você é de uma família de negociadores ou diplomatas e foi ensinado na arte das negociações.",
            AntecedenteFeat.NEGOCIADOR, List.of(choose(1, ATTENTION, PERSUASAO))) {
        /** "Baseado na Perícia escolhida você recebe … Discernir Motivação ou Negociação." */
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(graduationSkills.contains(ATTENTION)
                            ? TraitGrants.fixed(trained, AttentionSpecialization.DISCERNIR_MOTIVACAO) : null,
                    graduationSkills.contains(PERSUASAO)
                            ? TraitGrants.fixed(trained, PersuasaoSpecialization.NEGOCIACAO) : null);
        }
    },

    NOVICO("Noviço",
            "Você era um monge ou clérigo antes de se tornar aventureiro e é fortalecido por sua fé e convicções.",
            AntecedenteFeat.NOVICO, List.of()) {
        @Override
        public Map<EgoDomain, Integer> getEgoBonuses() {
            return Map.of(EgoDomain.AUTOCONTROLE, 1);
        }
    },

    NOBRE("Nobre",
            "Nascido em berço de ouro, você é um filho de nobre fora da linhagem sucessória que decidiu criar seu "
                    + "próprio nome como aventureiro.",
            AntecedenteFeat.NOBRE, List.of()) {
        @Override
        public Map<EgoDomain, Integer> getEgoBonuses() {
            return Map.of(EgoDomain.RECURSOS, 1);
        }
    },

    RITUALISTA("Ritualista",
            "Você cresceu entre bruxos, xamãs ou druidas, então também se tornou ritualista de um pequeno culto.",
            AntecedenteFeat.RITUALISTA, List.of(fixed(CONHECIMENTOS))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(TraitGrants.fixed(trained, ConhecimentosSpecialization.COSMOLOGIA));
        }
    },

    SELVAGEM("Selvagem", "", AntecedenteFeat.SELVAGEM, List.of()),

    SOLDADO("Soldado",
            "Você passou por treinamento militar, mas pediu sua dispensa para se aventurar ou se aventura em nome "
                    + "de algum nobre.",
            AntecedenteFeat.SOLDADO,
            List.of(choose(1, ATAQUE_A_DISTANCIA, ATAQUE_CORPO_A_CORPO, DIRIGIR_E_CAVALGAR, ESQUIVA_E_APARAR))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(anyOf(trained, SkillTraitKind.SPECIALIZATION, 1, graduationSkills));
        }
    },

    TROMBADINHA("Trombadinha",
            "Você vivia de pequenos furtos ou faz parte de uma guilda de ladrões.",
            AntecedenteFeat.TROMBADINHA, List.of(choose(1, ATLETISMO, FURTIVIDADE))) {
        @Override
        public List<TraitGrant> resolveTraitGrants(final Character trained, final Set<SkillType> graduationSkills) {
            return of(anyOf(trained, SkillTraitKind.SPECIALIZATION, 1, graduationSkills));
        }
    };

    private final String displayName;
    private final String description;
    private final AntecedenteFeat benefit;
    private final List<GraduationGrant> graduationGrants;

    CareerBackground(final String displayName, final String description, final AntecedenteFeat benefit,
                     final List<GraduationGrant> graduationGrants) {
        this.displayName = displayName;
        this.description = description;
        this.benefit = benefit;
        this.graduationGrants = graduationGrants;
    }

    @Override
    public BackgroundKind getKind() {
        return BackgroundKind.CAREER;
    }
}
