package org.aventyrs.core.ability;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.skill.CriticalResult;

import java.util.List;
import java.util.Optional;

@Getter
@AllArgsConstructor
public enum CharismaAbility implements AttributeAbility {

    AGRESSAO_ANUNCIADA("Você adquire Vantagem em rolagens de Perícias baseadas em Força ou Destreza sempre que " +
            "as fizer imediatamente após realizar uma rolagem de Perícia baseada em Carisma."),

    // TODO: grants an Especialização and a Habilidade de Competência for every trained Carisma-based Perícia at
    // the moment of acquisition — typed, held SkillSpecializations/SkillCompetencyAbilities
    // both exist now, but there's still no hook for an ability's own acquisition to grant
    // either onto a character's already-trained Perícias; same "no hook for granting at
    // acquisition time" gap ConhecimentosCompetencyAbility.GENERALISTA's own TODO cites.
    CHARME("Você adquire uma Especialização e uma Habilidade de Competência de cada Perícia baseada em Carisma " +
            "em que for treinado no momento em que adquirir esta Habilidade."),

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
