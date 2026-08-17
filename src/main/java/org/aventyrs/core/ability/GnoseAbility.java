package org.aventyrs.core.ability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.skill.SkillTraitKind;
import org.aventyrs.core.skill.SkillType;

import java.util.Collection;
import java.util.List;
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
    // TODO: it carries no limit of its own yet, so its "até 2" is still uncommunicated.
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

    // TODO: grants a new Habilidade de Competência for up to 2 known Perícias, still subject to its
    // prerequisites — no Perícia/Habilidade de Competência system exists yet.
    GENIALIDADE_E_ESFORCO("Você adquire uma nova Habilidade de Competência de até 2 Perícias conhecidas, você " +
            "ainda precisa cumprir com seus pré-requisitos."),

    // TODO: grants +1 permanent Autocontrole and a recovery effect the first time Autocontrole hits zero each
    // session — no Autocontrole/Ego system exists yet.
    ESTABILIDADE_EMOCIONAL("Você adquire permanentemente 1 ponto de Autocontrole, a primeira vez em cada sessão " +
            "de jogo que seu Autocontrole for reduzido a zero você receberá 1 ponto temporário neste Ego na " +
            "Rodada seguinte."),

    // The concrete, grantable form of this ability is org.aventyrs.core.ability
    // .PeritoTeoricoAbility — one constant per SkillType, since which Perícia was chosen is
    // which constant a character holds, not a separately-recorded value. This constant stays
    // the catalog/rules-text entry (mirrors ArtesCompetencyAbility#APRIMORAR_COM_ARTE's own
    // redirect-to-instance-class comment).
    PERITO_TEORICO("Escolha uma Perícia conhecida, você pode substituir o Atributo Base da Perícia escolhida por " +
            "Gnose, esta escolha não pode ser revertida."),

    // TODO: grants training in all untrained Perícias without initial Especializações — no
    // hook for granting Perícia training itself at acquisition time exists yet (same "Race has
    // no hook for granting starting Perícia training" gap CLAUDE.md documents for Elfo's Origem
    // Mística); the "without initial Especializações" half is trivially satisfiable once that
    // exists (CharacterSkill.specializations already defaults to an empty list).
    RATO_DE_BIBLIOTECA("Você recebe treinamento em todas as Perícias que não for treinado, mas as Perícias " +
            "treinadas desta forma não recebem Especializações iniciais.");

    /** The "até 3" in {@link #DOMINIO_DO_CONHECIMENTO}'s rules text: how many of the Perícias it
     * offers as candidates the player may actually take the new Especialização on. */
    public static final int DOMINIO_DO_CONHECIMENTO_CHOICE_LIMIT = 3;

    private final String description;

    @Override
    public AttributeDomain getAttributeDomain() {
        return AttributeDomain.GNOSE;
    }
}
