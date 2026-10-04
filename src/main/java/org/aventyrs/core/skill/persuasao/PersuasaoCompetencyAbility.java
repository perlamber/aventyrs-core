package org.aventyrs.core.skill.persuasao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantAction;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillTrait;
import java.util.List;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * The Habilidades de Competência available to characters trained in Persuasão.
 */
@Getter
@AllArgsConstructor
public enum PersuasaoCompetencyAbility implements SkillCompetencyAbility {

    // Substitutes Força for Carisma — see SkillCompetencyAbility.getSubstituteAttributeDomain().
    FORCA_OPRESSORA("Você pode substituir o Atributo Base desta Perícia por Força.") {
        @Override
        public Optional<AttributeDomain> getSubstituteAttributeDomain() {
            return Optional.of(AttributeDomain.STRENGTH);
        }
    },

    /**
     * Real (core 0.0.103). A success under Comunicação, Mentir ou Omitir or Intimidação grants a marker Blessing
     * named for that Especialização, lasting the rest of the Cena (table ruling; kept per combat); while it runs,
     * every Persuasão roll under the same Especialização gets Vantagem. ⚠️ "contra outros personagens próximos
     * (Distância Curta)" is the roller's declaration: a Perícia roll names no target, so neither the "other" nor
     * the distance is checked.
     */
    ESPALHAR_EMOCOES("Após ser bem-sucedido em rolagens de Comunicação, Mentir ou Omitir " +
            "ou Intimidação, você recebe Vantagem em rolagens semelhantes contra outros " +
            "personagens próximos (Distância Curta).") {
        @Override
        public List<Blessing> resolveSuccessBlessings(final SkillType skillType, final SkillTrait requestedAbility,
                                                       final SceneContext sceneContext) {
            if (skillType != SkillType.PERSUASAO || requestedAbility == null || !EMOTION_SPECIALIZATIONS.contains(requestedAbility)) {
                return List.of();
            }
            // A marker: worth nothing itself, read back by resolveConditionalRollBonus below.
            return List.of(new Blessing(ModifierType.PERSUASAO_ROLL_BONUS, 0, 1, TargetScope.SELF,
                    emotionSource(requestedAbility)).untilCombatEnds());
        }

        @Override
        public Optional<Integer> resolveConditionalRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                                             final SkillTrait requestedAbility, final CombatantSheet holder) {
            boolean spread = skillType == SkillType.PERSUASAO && requestedAbility != null
                    && EMOTION_SPECIALIZATIONS.contains(requestedAbility)
                    && holder != null && holder.hasEffectFrom(emotionSource(requestedAbility));
            return spread ? Optional.of(Skill.ADVANTAGE_BONUS) : Optional.empty();
        }
    },

    // TODO: Vantagem scoped to the *target's* gender/attraction toward the character — this
    // depends on a property of the specific other character being rolled against, not
    // something the acting character's own abilities can resolve in isolation (same kind of
    // gap as the NPC-disposition systems ArtesCompetencyAbility.ESPALHAR_REPUTACAO and
    // EmpatiaSelvagemExcellency.LENDA need); no such cross-character-disposition system
    // exists yet.
    SEDUTOR("Vantagens em rolagens desta Perícia roladas contra personagens do sexo " +
            "oposto, ou contra quaisquer personagens que possam se sentir atraídos por " +
            "você."),

    // TODO: -1PE to Equipamento purchase/production costs. Item#getPrice() is a real PE figure
    // per catalog entry, so the gap is the economy around it rather than the entity: there is no
    // PE budget to spend from and no purchase or production entry point to discount (same gap as
    // ProfissaoExcellency.FOCADO/LENDA).
    CAMBALACHO("O custo de compra e produção de Equipamentos é reduzido em -1PE."),

    /**
     * "Se for bem-sucedido sua próxima rolagem de Perícia de Ataque nesta Rodada recebe
     * Vantagem." <b>The Vantagem half is real</b>, through {@link
     * SkillCompetencyAbility#resolveSuccessBlessings} — the roll now knows the GD it was made
     * against, so "bem-sucedido" is answerable, and a Rodada-scoped {@code TemporaryBonus} on
     * both Perícias de Ataque is what carries it.
     */
    // Both halves real (core 0.0.100): the Vantagem and the -1PA hold while the Finta's Blessings
    // stand, and onActionRecorded lifts them after the first Perícia de Ataque roll — "sua próxima
    // rolagem", within "nesta Rodada" (the Blessings' own 1 Rodada).
    FINTAR_APRIMORADO("Você pode fazer rolagens para enganar seus oponentes, se for " +
            "bem-sucedido sua próxima rolagens de Perícia de Ataque nesta Rodada recebe " +
            "Vantagem e tem seu Tempo de Ação reduzido em -1PA.") {
        @Override
        public List<Blessing> resolveSuccessBlessings(final SkillType skillType, final SkillTrait requestedAbility,
                                                       final SceneContext sceneContext) {
            if (skillType != SkillType.PERSUASAO) {
                return List.of();
            }
            return List.of(
                    new Blessing(ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS, Skill.ADVANTAGE_BONUS,
                            FINTA_DURATION_IN_ROUNDS, TargetScope.SELF, FINTAR_APRIMORADO.name()),
                    new Blessing(ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, Skill.ADVANTAGE_BONUS,
                            FINTA_DURATION_IN_ROUNDS, TargetScope.SELF, FINTAR_APRIMORADO.name()));
        }

        @Override
        public int resolveAttackActionPointAdjustment(final SkillType attackSkill, final CombatantSheet holder) {
            return holder != null && holder.hasEffectFrom(FINTAR_APRIMORADO.name()) ? -FINTA_ACTION_POINT_REDUCTION : 0;
        }

        @Override
        public void onActionRecorded(final CombatantSheet holder, final CombatantAction action) {
            if (action.skill() != null && action.skill().isAttackSkill()) {
                holder.removeEffectsFrom(FINTAR_APRIMORADO.name());
            }
        }
    };

    /** "tem seu Tempo de Ação reduzido em -1PA". */
    private static final int FINTA_ACTION_POINT_REDUCTION = 1;

    /** Espalhar Emoções' three Especializações. */
    private static final java.util.Set<SkillTrait> EMOTION_SPECIALIZATIONS = java.util.Set.of(
            PersuasaoSpecialization.COMUNICACAO, PersuasaoSpecialization.MENTIR_OU_OMITIR,
            PersuasaoSpecialization.INTIMIDACAO);

    /** The marker source naming which Especialização spread its emotions. */
    private static String emotionSource(final SkillTrait specialization) {
        return "ESPALHAR_EMOCOES:" + ((Enum<?>) specialization).name();
    }

    /** "Nesta Rodada" — the Finta's Vantagem lasts the Rodada it was won in. */
    private static final int FINTA_DURATION_IN_ROUNDS = 1;

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.PERSUASAO;
    }
}
