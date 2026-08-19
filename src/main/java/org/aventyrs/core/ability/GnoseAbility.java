package org.aventyrs.core.ability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.skill.SkillTraitKind;
import org.aventyrs.core.skill.SkillType;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;

@Getter
@AllArgsConstructor
public enum GnoseAbility implements AttributeAbility {

    // Mirrors CharismaAbility#CHARME's own resolvePendingSkillTraitChoices shape, minus the
    // Attribute-domain filter — "Perícias conhecidas" here means every currently trained
    // Perícia, not just Gnose-based ones — and capped at DOMINIO_DO_CONHECIMENTO_CHOICE_LIMIT
    // rather than one per candidate, per this ability's own "até 3" text. Each resolved entry
    // goes through org.aventyrs.core.character.services.AttributeAbilityService
    // #grantSpecializationChoice (specialization only — unlike CHARME, this ability owes no
    // SkillCompetencyAbility alongside it, which is what resolvePendingSkillTraitKinds reports).
    // The cap is reported, not enforced at resolution time: grantSpecializationChoice records
    // whatever it's handed, same unenforced-prerequisite restraint already applied to
    // "Requer N Graduações"-style clauses elsewhere.
    // ConhecimentosCompetencyAbility.GENERALISTA is the same shape one level down — a
    // SkillCompetencyAbility rather than an AttributeAbility — and is now wired to the same
    // grantSpecializationChoice mechanism via its own resolvePendingSpecializationChoices;
    DOMINIO_DO_CONHECIMENTO("Você recebe uma nova Especialização de até 3 Perícias conhecidas.") {
        @Override
        public List<SkillType> resolvePendingSkillTraitChoices(final Collection<SkillType> trainedSkills) {
            return List.copyOf(trainedSkills);
        }

        @Override
        public OptionalInt resolvePendingSkillTraitChoiceLimit() {
            return OptionalInt.of(DOMINIO_DO_CONHECIMENTO_CHOICE_LIMIT);
        }

        @Override
        public Set<SkillTraitKind> resolvePendingSkillTraitKinds() {
            return Set.of(SkillTraitKind.SPECIALIZATION);
        }
    },

    // Mirrors CharismaAbility#CHARME's own resolvePendingSkillTraitChoices/Kinds shape, minus
    // the Attribute-domain filter — "até 2 Perícias conhecidas" here means any known Perícia,
    // not just Gnose-based ones — and capped at GENIALIDADE_E_ESFORCO_CHOICE_LIMIT, mirroring
    // DOMINIO_DO_CONHECIMENTO's own "até 3" shape. This only resolves *which* Perícias owe the
    // choice; the granted SkillCompetencyAbility's own prerequisites ("você ainda precisa
    // cumprir com seus pré-requisitos") are, as always, unenforced — same restraint already
    // applied to every other unenforced acquisition prerequisite in this codebase.
    GENIALIDADE_E_ESFORCO("Você adquire uma nova Habilidade de Competência de até 2 Perícias conhecidas, você " +
            "ainda precisa cumprir com seus pré-requisitos.") {
        @Override
        public List<SkillType> resolvePendingSkillTraitChoices(final Collection<SkillType> trainedSkills) {
            return List.copyOf(trainedSkills);
        }

        @Override
        public OptionalInt resolvePendingSkillTraitChoiceLimit() {
            return OptionalInt.of(GENIALIDADE_E_ESFORCO_CHOICE_LIMIT);
        }

        @Override
        public Set<SkillTraitKind> resolvePendingSkillTraitKinds() {
            return Set.of(SkillTraitKind.COMPETENCY_ABILITY);
        }
    },

    // Only the "+1 permanente em Autocontrole" half is real: resolvePermanentEgoGain below,
    // applied by AttributeAbilityServiceImpl#grantAttributeAbility the exact same way
    // CharismaAbility#DESTINO_FAVORAVEL's own permanent Sorte point already is. The "primeira
    // vez em cada sessão de jogo... 1 ponto temporário na Rodada seguinte" recovery half stays
    // TODO'd — it needs the same "no game-session tracking system exists yet" gap
    // AutocontroleAdvantage#MOTIVACAO_DE_MOSES's own "por sessão de jogo" clause already cites,
    // plus a "next Rodada" delayed-grant mechanism nothing in this core has either.
    ESTABILIDADE_EMOCIONAL("Você adquire permanentemente 1 ponto de Autocontrole, a primeira vez em cada sessão " +
            "de jogo que seu Autocontrole for reduzido a zero você receberá 1 ponto temporário neste Ego na " +
            "Rodada seguinte.") {
        @Override
        public Optional<EgoDomain> resolvePermanentEgoGain() {
            return Optional.of(EgoDomain.AUTOCONTROLE);
        }
    },

    // The concrete, grantable form of this ability is org.aventyrs.core.ability
    // .PeritoTeoricoAbility — one constant per SkillType, since which Perícia was chosen is
    // which constant a character holds, not a separately-recorded value. This constant stays
    // the catalog/rules-text entry (mirrors ArtesCompetencyAbility#APRIMORAR_COM_ARTE's own
    // redirect-to-instance-class comment).
    PERITO_TEORICO("Escolha uma Perícia conhecida, você pode substituir o Atributo Base da Perícia escolhida por " +
            "Gnose, esta escolha não pode ser revertida."),

    // "Todas as Perícias que não for treinado" is real: resolveGrantedSkillTraining below
    // returns every currently-untrained SkillType, and AttributeAbilityServiceImpl
    // #grantAttributeAbility grants a fresh CharacterSkill for each. The "não recebem
    // Especializações iniciais" half needs nothing further — CharacterSkill.specializations
    // already defaults to an empty list.
    RATO_DE_BIBLIOTECA("Você recebe treinamento em todas as Perícias que não for treinado, mas as Perícias " +
            "treinadas desta forma não recebem Especializações iniciais.") {
        @Override
        public List<SkillType> resolveGrantedSkillTraining(final Character character) {
            return Arrays.stream(SkillType.values())
                    .filter(skillType -> !character.getSkills().containsKey(skillType))
                    .toList();
        }
    };

    /** The "até 3" in {@link #DOMINIO_DO_CONHECIMENTO}'s rules text: how many of the Perícias it
     * offers as candidates the player may actually take the new Especialização on. */
    public static final int DOMINIO_DO_CONHECIMENTO_CHOICE_LIMIT = 3;

    /** The "até 2" in {@link #GENIALIDADE_E_ESFORCO}'s rules text: how many of the Perícias it
     * offers as candidates the player may actually take the new Habilidade de Competência on. */
    public static final int GENIALIDADE_E_ESFORCO_CHOICE_LIMIT = 2;

    private final String description;

    @Override
    public AttributeDomain getAttributeDomain() {
        return AttributeDomain.GNOSE;
    }
}
