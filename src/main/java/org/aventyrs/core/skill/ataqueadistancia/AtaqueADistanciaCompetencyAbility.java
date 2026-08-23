package org.aventyrs.core.skill.ataqueadistancia;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * The Habilidades de Competência available to characters trained in Ataque à Distância. Most
 * of these need a system this core doesn't have yet (damage/critical-damage rolls,
 * range/targeting, or dice rolling this core deliberately never does — see the {@code skill}
 * package-info's "What this library computes" section) so they aren't expressible for real
 * today; see each constant's TODO. DISPARO_ARCANO's unconditional Attribute substitution
 * (see {@link SkillCompetencyAbility#getSubstituteAttributeDomain()}) and FRIEZA's Vantagem
 * on nearby-target damage rolls (see {@link SkillCompetencyAbility#resolveDamageBonus}) are
 * the exceptions.
 */
@Getter
@AllArgsConstructor
public enum AtaqueADistanciaCompetencyAbility implements SkillCompetencyAbility {

    // TODO: lets this Perícia use Força instead of its normal base Attribute (Destreza), but
    // only for attacks made with arremesso (throwing) weapons or Magias. Unlike
    // AtaqueCorpoACorpoCompetencyAbility.ACUIDADE/this same enum's DISPARO_ARCANO, this
    // substitution is *scoped* to a specific delivery method, not unconditional — the generic
    // mechanism (SkillCompetencyAbility.getSubstituteAttributeDomain()) doesn't cover that
    // narrowing, same simplification already documented for scoped Vantagem bonuses (see
    // CLAUDE.md's "Vantagem is a flat +2 bonus" section) — this codebase doesn't track what
    // weapon/delivery-method a specific roll is being made with.
    ARREMESSO_PODEROSO("Você pode substituir o Atributo Base desta perícia por Força, mas " +
            "apenas para rolagens de ataques com armas de arremessos e magias."),

    // Substitutes Destreza for Foco — see SkillCompetencyAbility.getSubstituteAttributeDomain().
    DISPARO_ARCANO("Você pode substituir o Atributo Base desta perícia por Foco.") {
        @Override
        public Optional<AttributeDomain> getSubstituteAttributeDomain() {
            return Optional.of(AttributeDomain.FOCUS);
        }
    },

    // Vantagem on damage rolls, against a target at Distância Curta or closer. Both the
    // amount and the condition live here, via SkillCompetencyAbility#resolveDamageBonus —
    // unlike a plain @Modifier(ModifierType) method (reflection-invoked with zero args, see
    // ModifierResolver), this ability's bonus depends on per-roll data (the real attack
    // target's distance), so it needs sceneContext/attackTarget handed in explicitly instead.
    FRIEZA("Vantagem nas rolagens de dano de Ataques à Distância realizados contra alvos " +
            "em Distância Curta ou inferior.") {
        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SceneContext sceneContext, final CombatantSheet attackTarget) {
            if (sceneContext == null || attackTarget == null) {
                return Optional.empty();
            }
            Range distanceToTarget = sceneContext.getDistanceTo(attackTarget);
            if (distanceToTarget == null || !distanceToTarget.isWithin(Range.DISTANCIA_CURTA)) {
                return Optional.empty();
            }
            return Optional.of(new DamageBonus(Skill.ADVANTAGE_BONUS, DamageType.FISICO));
        }
    },

    // TODO: a successful hit on a damaging attack lets the character pick a new target at
    // Distância Muito Curta from the original and roll this Perícia again, dealing 1d6
    // damage (or the Magia/Efeito's own described damage if lower) on success — the Distância
    // vocabulary exists now (org.aventyrs.core.scene.Range), but SceneContext only answers
    // "how far is CombatantSheet X", not "which targets are within Y of this other target",
    // so picking a *new* target this way still isn't expressible; also no "Corrente de
    // Efeitos" (chain-effect) or Magia/Efeito entity exists yet, and this core deliberately
    // never rolls dice (1d6) — see the skill package-info.
    DISPARO_RICOCHETE("Seus Ataques à Distância capazes de infligir danos recebem a " +
            "Corrente de Efeitos – Ricochete - Você pode escolher um novo alvo em " +
            "Distância Muito Curta de seu alvo inicial e efetuar uma nova rolagem nesta " +
            "Perícia, se for bem-sucedido o alvo adicional sofre 1d6 pontos de dano (ou o " +
            "dano descrito no Efeito, o que for menor)."),

    // TODO: Vantagem on Critical Damage rolls — no critical-damage-roll concept exists yet.
    MIRAR_NA_CABECA("Vantagem nas rolagens de Danos Críticos.");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ATAQUE_A_DISTANCIA;
    }
}
