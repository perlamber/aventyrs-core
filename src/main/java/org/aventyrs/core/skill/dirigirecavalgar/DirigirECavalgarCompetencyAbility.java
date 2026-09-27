package org.aventyrs.core.skill.dirigirecavalgar;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.feat.CavalariaFeat;
import org.aventyrs.core.modifier.Modifier;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

/**
 * The Habilidades de Competência available to characters trained in Dirigir e Cavalgar.
 */
@Getter
@AllArgsConstructor
public enum DirigirECavalgarCompetencyAbility implements SkillCompetencyAbility {

    // Note: Vantagem here is only meant for animal/animal-drawn-vehicle rolls, not every
    // Dirigir e Cavalgar roll (e.g. it shouldn't apply to Veículos Tecnológicos) — this
    // codebase doesn't track what a roll is *for*, so it's implemented as an unconditional
    // flat bonus rather than silently narrowed (see CLAUDE.md's Vantagem section for this
    // pattern in general).
    CONTROLAR_ANIMAIS("Você se especializou em controlar animais e veículos de tração " +
            "animal e recebe Vantagem em suas rolagens.") {
        @Modifier(ModifierType.SKILL_ROLL_BONUS)
        public int advantageBonus() {
            return Skill.ADVANTAGE_BONUS;
        }
    },

    // TODO: lets this Perícia substitute for Profissão: Mecânica for simple vehicle
    // maintenance — no Perícia-to-Perícia substitution mechanism exists yet (same gap as
    // ArtesCompetencyAbility.DOMINIO_CULTURAL, which substitutes Artes for Conhecimentos).
    MANUTENCAO_VEICULAR("Em substituição à Profissão: Mecânica, você pode usar esta " +
            "perícia para fazer manutenções simples em veículos."),

    // Real. The base restriction is CombatantSheet#isSkillUsePrevented (every Perícia but Dirigir e
    // Cavalgar is refused while riding), which this ability lifts; in exchange its holder takes a
    // Desvantagem on every roll governed by Força or Destreza while riding — turned into a Vantagem
    // by CavalariaFeat#GRANDE_GINETE ("ao invés disso recebe Vantagem").
    GINETE("A restrição de não usar outras Perícias enquanto dirigindo um veículo ou " +
            "cavalgando um animal é retirada, porém você sofre Desvantagem em todas as " +
            "rolagens de Perícias baseadas em Força e Destreza feitas nestas condições.") {
        @Override
        public int resolveGoverningAttributeRollBonus(final AttributeDomain domain, final CombatantSheet holder) {
            if (holder == null || !holder.isRiding()
                    || (domain != AttributeDomain.STRENGTH && domain != AttributeDomain.DEXTERITY)) {
                return 0;
            }
            boolean grandeGinete = holder.getCharacter().getFeats().stream()
                    .anyMatch(feat -> feat.catalogEntry() == CavalariaFeat.GRANDE_GINETE);
            return grandeGinete ? Skill.ADVANTAGE_BONUS : Skill.DISADVANTAGE_MALUS;
        }
    },

    // TODO: automatic success is expressible now (SkillCompetencyAbility#resolveAutomaticSuccess,
    // see AttentionCompetencyAbility.PERCEPCAO_DE_FOXM), but "enquanto não estiver sob grande
    // estresse" has nothing to read: no stress concept exists, and SceneContext#isCombatScene()
    // is a narrower thing than stress. Granting it unconditionally would make every Dirigir e
    // Cavalgar roll automatic.
    DIRECAO_SEGURA("Você é bem-sucedido em quaisquer rolagens de Dirigir e Cavalgar " +
            "enquanto não estiver sob grande estresse."),

    // TODO: unlocks maneuvers (jumping, wall-running, etc.) while riding/driving, vetoable
    // by the Narrador if the maneuver is impossible — no maneuver/stunt system or
    // GM-adjudication hook exists yet.
    DIRECAO_ACROBATA("Você pode para fazer manobras como saltar, correr pelas paredes, " +
            "dentre outras ações, com sua montaria ou veículo. O uso deste recurso pode ser " +
            "vetado pela Narrador caso a manobra escolhida seja algo impossível de ser " +
            "realizada.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.DIRIGIR_E_CAVALGAR;
    }
}
