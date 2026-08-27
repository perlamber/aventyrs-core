package org.aventyrs.core.ability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillTraitKind;
import org.aventyrs.core.skill.SkillType;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Getter
@AllArgsConstructor
public enum CharismaAbility implements AttributeAbility {

    AGRESSAO_ANUNCIADA("Você adquire Vantagem em rolagens de Perícias baseadas em Força ou Destreza sempre que " +
            "as fizer imediatamente após realizar uma rolagem de Perícia baseada em Carisma."),

    CHARME("Você adquire uma Especialização e uma Habilidade de Competência de cada Perícia baseada em Carisma " +
            "em que for treinado no momento em que adquirir esta Habilidade.") {
        // "de cada Perícia baseada em Carisma em que for treinado" — one per candidate, so no
        // resolvePendingSkillTraitChoiceLimit override; the default (empty, meaning "the whole
        // candidate list") is already this ability's own quota.
        @Override
        public List<SkillType> resolvePendingSkillTraitChoices(final Collection<SkillType> trainedSkills) {
            return trainedSkills.stream()
                    .filter(skillType -> skillType.newSkillInstance().getAttributeDomain() == AttributeDomain.CHARISMA)
                    .toList();
        }

        @Override
        public Set<SkillTraitKind> resolvePendingSkillTraitKinds() {
            return Set.of(SkillTraitKind.SPECIALIZATION, SkillTraitKind.COMPETENCY_ABILITY);
        }
    },

    DESTINO_FAVORAVEL("Você adquire um ponto de Sorte permanentemente; sempre que tiver um Sucesso Crítico Maior " +
            "em uma rolagem de Perícia você adquire um ponto temporário, não cumulativo, em Sorte e em " +
            "Autocontrole.") {
        @Override
        public Optional<EgoDomain> resolvePermanentEgoGain() {
            return Optional.of(EgoDomain.SORTE);
        }

        @Override
        public List<EgoDomain> resolveCriticalSuccessEgoGain(final CriticalResult criticalResult) {
            return criticalResult == CriticalResult.ACERTO_CRITICO_MAIOR
                    ? List.of(EgoDomain.SORTE, EgoDomain.AUTOCONTROLE)
                    : List.of();
        }
    },

    VOZ_DE_OURO("Você pode gastar 2PD para reduzir em -1PA o Tempo de qualquer Ação que exija rolagens de " +
            "Perícias baseadas em Carisma."),

    FLOREIO_ARCANO("Você adquire Vantagem em rolagens de Perícias baseadas em Carisma sempre que as fizer " +
            "imediatamente após Conjurar uma Magia.");

    private final String description;

    @Override
    public AttributeDomain getAttributeDomain() {
        return AttributeDomain.CHARISMA;
    }
}
