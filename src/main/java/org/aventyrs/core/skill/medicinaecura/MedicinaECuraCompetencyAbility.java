package org.aventyrs.core.skill.medicinaecura;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.UtilityItem;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.CompetencyUses;
import org.aventyrs.core.skill.UseWindow;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

/**
 * The Habilidades de Competência available to characters trained in Medicina e Cura.
 */
@Getter
@AllArgsConstructor
public enum MedicinaECuraCompetencyAbility implements SkillCompetencyAbility {

    /**
     * Real (core 0.0.102): the first Medicina e Cura roll of each Rodada costs -1PA while its holder carries or
     * wears a {@link UtilityItem#KIT_DE_PRIMEIROS_SOCORROS} (table ruling: a real item, in the store's
     * Utilidades). "Uma vez por Rodada" is read off the Rodada's action log, so it needs no ledger of its own.
     */
    BOM_DOUTOR("Se tiver um kit de primeiros socorros em mãos, uma vez por Rodada os usos " +
            "desta Perícia têm o Tempo de Ação reduzido em -1PA.") {
        @Override
        public int resolveSkillRollActionPointAdjustment(final SkillType skillType, final CombatantSheet holder) {
            if (skillType != SkillType.MEDICINA_E_CURA || !UtilityItem.KIT_DE_PRIMEIROS_SOCORROS.isHeldBy(holder)) {
                return 0;
            }
            boolean usedThisRodada = holder.getActionsThisRound().stream()
                    .anyMatch(action -> action.skill() == SkillType.MEDICINA_E_CURA);
            return usedThisRodada ? 0 : -1;
        }
    },

    // Real (core 0.0.103): skill.CompetencyActions#rollMedicinaAlternativa (GD Médio); on a success the
    // caller rolls one 1d6 for PV, PM and PD alike (table ruling) and owes it on the patient's sheet
    // (CombatantSheet#owePendingRestBonus), paid by RestService#applyRest at the Descanso's end.
    MEDICINA_ALTERNATIVA("Você pode, antes ou durante o descanso de outra criatura, fazer " +
            "massagens, usar de acupuntura ou outros recursos prazerosos e relaxantes, para " +
            "melhorar a qualidade do descanso do alvo. Faça uma rolagem contra GD média, se " +
            "for bem-sucedido o alvo recuperará 1d6PV, PM e PD adicionais ao fim do " +
            "Descanso."),

    // Real (core 0.0.103): skill.CompetencyActions#rollMilagreiro (GD Difícil) and #applyMilagreiro on
    // the patient — a Descanso Curto's PV, plus its PM/PD with Milagre Maior (#milagreMaior, the base
    // Corrente margin); once per patient until a Descanso Longo, failed attempt included, kept as a
    // rest-scoped use on the patient's sheet so it persists.
    MILAGREIRO("Quando fizer rolagens de Medicina e Cura para tratar e estancar ferimentos " +
            "você pode aumentar o Grau de Dificuldade para Difícil, se o fizer e for " +
            "bem-sucedido os personagens afetados recuperam PV como se passassem por um " +
            "Descanso Curto. Um mesmo personagem não pode ser afetado por Milagreiro uma " +
            "segunda vez até passar por um Descanso Longo, mesmo que a primeira tentativa " +
            "tenha falhado. Esta Habilidade possui a Corrente de Efeitos – Milagre Maior: O " +
            "personagem alvo também recupera PD e PM."),

    /**
     * Real (core 0.0.102) as a limited use: 1/2/3 per Descanso Longo ({@link CompetencyUses}). A use makes one
     * Medicina e Cura roll an Ação Livre — priced by the caller, which spends the use as it rolls.
     */
    SOCORRO_IMEDIATO("A cada dia você efetuar rolagens desta Perícia como Ação Livre, este " +
            "benefício é renovado após passar por um Descanso Longo. Você ganhar usos " +
            "adicionais deste benefício na 5ª e 10ª Graduação.") {
        @Override
        public int resolveUseLimit(final Character holder) {
            int graduation = holder == null ? 0 : holder.getEffectiveGraduation(SkillType.MEDICINA_E_CURA);
            return 1 + (graduation >= 5 ? 1 : 0) + (graduation >= 10 ? 1 : 0);
        }

        @Override
        public UseWindow getUseWindow() {
            return UseWindow.LONG_REST;
        }
    },

    // TODO: +1 Rodada to potions'/antidotes' Duração, then +1 more at the 5th and 10th
    // graduation — no potion/antidote entity or duration-tracking system exists yet (same
    // gap as DominioDoManaCompetencyAbility.CONJURACAO_DURADOURA).
    ALQUIMIA_MAIOR("Suas poções e antídotos tem a Duração de efeito aumentada em +1 " +
            "Rodada, então em +1 Rodada na 5ª e 10ª Graduação.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.MEDICINA_E_CURA;
    }
}
