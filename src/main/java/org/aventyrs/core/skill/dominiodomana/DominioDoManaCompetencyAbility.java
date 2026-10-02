package org.aventyrs.core.skill.dominiodomana;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * The Habilidades de Competência available to characters trained in Domínio do Mana. Most of
 * these modify some aspect of Magias (critical margin, duration, damage/healing,
 * concentration) that no {@code Magia}/spellcasting entity or resolution engine can express
 * yet — none of them are expressible for real today; see each constant's TODO. MAGIA_SELVAGEM's
 * unconditional Attribute substitution is the exception — see {@link SkillCompetencyAbility
 * #getSubstituteAttributeDomain()}.
 */
@Getter
@AllArgsConstructor
public enum DominioDoManaCompetencyAbility implements SkillCompetencyAbility {

    /**
     * Real (core 0.0.101): +1/+2/+3 "números" (5ª/10ª Graduação em Domínio do Mana) on the Margem Crítica
     * Menor of a Magia's roll — the Conjuração itself (a Domínio do Mana roll) and an attack a {@link
     * Spell} delivers.
     */
    LETALIDADE_ARCANA("A Margem Crítica Menor de suas Magias é aumentada em +1 número, " +
            "então em +1 ao alcançar a 5ª e 10ª graduação.") {
        @Override
        public int resolveCriticalMarginIncrease(final SkillType skillType, final SceneContext sceneContext,
                                                 final AttackSource attackSource, final CombatantSheet holder) {
            boolean spellRoll = skillType == SkillType.DOMINIO_DO_MANA || attackSource instanceof Spell;
            return spellRoll ? tiered(1, holder == null ? null : holder.getCharacter()) : 0;
        }
    },

    // Substitutes Foco for Instinto — see SkillCompetencyAbility.getSubstituteAttributeDomain().
    MAGIA_SELVAGEM("Você pode alterar o Atributo Base desta Perícia para Instinto.") {
        @Override
        public Optional<AttributeDomain> getSubstituteAttributeDomain() {
            return Optional.of(AttributeDomain.INSTINCT);
        }
    },

    /** Real (core 0.0.101): +1/+2/+3 Rodadas on a Magia whose Duração is counted in Rodadas. */
    CONJURACAO_DURADOURA("A Duração de suas Magias é aumentada em +1 Rodada, então em +1 " +
            "ao alcançar a 5ª e 10ª graduação.") {
        @Override
        public int resolveSpellDurationIncrease(final Spell spell, final Character caster) {
            return tiered(1, caster);
        }
    },

    /** Real (core 0.0.101): +2/+3/+4 on a Magia's dano and on its cura. */
    ARCANISMO_EXPLOSIVA("Efeitos de Danos e Curas de suas Magias são aumentados em +2, " +
            "então em +1 ao alcançar a 5ª e 10ª graduação.") {
        @Override
        public int resolveSpellDamageBonus(final Spell spell, final Character caster) {
            return tiered(2, caster);
        }

        @Override
        public int resolveSpellHealingBonus(final Spell spell, final Character caster) {
            return tiered(2, caster);
        }
    },

    // Nothing to protect yet: this core breaks Concentração only on the caster's own cast or attack
    // (Scene#breakConcentration), never on damage, so a holder already keeps it after sofrer Danos.
    // The day damage can break Concentração, that path must consult this constant.
    CONCENTRACAO_INABALAVEL("Você não perde a Concentração para manter ativa suas magias " +
            "após sofrer Danos.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.DOMINIO_DO_MANA;
    }

    /** base, then +1 at the 5ª and +1 at the 10ª Graduação em Domínio do Mana — base alone with no holder. */
    private static int tiered(final int base, final Character holder) {
        int graduation = holder == null ? 0 : holder.getEffectiveGraduation(SkillType.DOMINIO_DO_MANA);
        return base + (graduation >= 5 ? 1 : 0) + (graduation >= 10 ? 1 : 0);
    }
}
