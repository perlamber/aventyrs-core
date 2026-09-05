package org.aventyrs.core.skill.persuasao;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.Blessing;
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

    // TODO: after succeeding at a Comunicação/Mentir ou Omitir/Intimidação roll, grants Vantagem
    // on similar rolls against other nearby characters. Two of the three pieces are real now:
    // "nearby" is SceneContext/Range, and granting on success is
    // SkillCompetencyAbility#resolveSuccessBlessings (see FINTAR_APRIMORADO). What blocks it is
    // "rolagens semelhantes contra *outros personagens*" — the Vantagem is scoped both to the
    // three named Especializações and to a set of targets other than the one just rolled
    // against, and a Blessing names a ModifierType and a TargetScope, neither of which can
    // express "the same Especialização, against everyone except that one".
    ESPALHAR_EMOCOES("Após ser bem-sucedido em rolagens de Comunicação, Mentir ou Omitir " +
            "ou Intimidação, você recebe Vantagem em rolagens semelhantes contra outros " +
            "personagens próximos (Distância Curta)."),

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
    // TODO: "sua *próxima* rolagem" is one roll, not every Ataque roll of the Rodada — nothing
    //  consumes a TemporaryBonus on first use, so this over-grants for the rest of the Rodada.
    //  Granting nothing would be further from the clause; the narrower shape needs a
    //  consumed-on-use bonus, which no mechanism provides.
    // TODO: the "-1PA" half — ModifierType.SKILL_ROLL_COST exists but ActionPointsService reads
    //  it only from attributeAbilities, and it scopes to a Perícia roll's cost generally rather
    //  than to one upcoming roll.
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
    };

    /** "Nesta Rodada" — the Finta's Vantagem lasts the Rodada it was won in. */
    private static final int FINTA_DURATION_IN_ROUNDS = 1;

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.PERSUASAO;
    }
}
