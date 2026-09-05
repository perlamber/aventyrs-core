package org.aventyrs.core.skill.ataqueadistancia;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageBonus;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.Weapon;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.Skill;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

import java.util.Optional;

/**
 * The Habilidades de Competência available to characters trained in Ataque à Distância. Most
 * of these need a system this core doesn't have yet (damage/critical-damage rolls,
 * range/targeting, or dice rolling this core deliberately never does — see the {@code skill}
 * package-info's "What this library computes" section) so they aren't expressible for real
 * today; see each constant's TODO. Three are exceptions: {@link #DISPARO_ARCANO}'s
 * unconditional Attribute substitution (see {@link SkillCompetencyAbility
 * #getSubstituteAttributeDomain()}), {@link #ARREMESSO_PODEROSO}'s substitution scoped to
 * thrown weapons and Magias (see {@link SkillCompetencyAbility
 * #resolveSubstituteAttributeDomain(AttackSource)}), and {@link #FRIEZA}'s Vantagem on
 * nearby-target damage rolls (see {@link SkillCompetencyAbility#resolveDamageBonus}).
 */
@Getter
@AllArgsConstructor
public enum AtaqueADistanciaCompetencyAbility implements SkillCompetencyAbility {

    /**
     * Real, and the reference for a <b>delivery-scoped</b> substitution — the counterpart to
     * {@link #DISPARO_ARCANO}'s unconditional one. Force applies only when the attack is
     * actually thrown or cast, so this overrides {@link SkillCompetencyAbility
     * #resolveSubstituteAttributeDomain(AttackSource)} and leaves {@link SkillCompetencyAbility
     * #getSubstituteAttributeDomain()} empty; overriding the unconditional hook would hand
     * Força to the bow shot this clause deliberately excludes.
     *
     * <p>"Armas de arremessos" is read as {@link ItemCategory#THROWABLE} alone. {@code BOW}/
     * {@code CROSSBOW} are fired rather than thrown, {@code PROJECTILE} is their ammunition, and
     * {@code SPEAR} is a thrust polearm — a lança meant to be hurled would be authored as a
     * {@code THROWABLE} weapon, since {@code Weapon#getSkillType()} is one column and a weapon
     * usable either way can't be expressed today anyway.
     *
     * <p>"Magias" needs no further check: {@link SkillCompetencyAbility#resolveAttributeDomain}
     * only consults abilities whose own {@link #getSkillType()} matches the Perícia being
     * rolled, so any Magia reaching this constant is already one delivered by an Ataque à
     * Distância roll.
     *
     * <p>The two halves of the clause are told apart by <em>type</em>: a {@link Spell} is the
     * Magia half outright, and a {@link Weapon} qualifies only on its {@link ItemCategory}. This
     * is where such a test belongs — {@link AttackSource} carries no {@code isThrown()}, because
     * the next clause scoped to weapon categories will draw the line somewhere else.
     *
     * <p>A {@code null} attackSource — a caller who didn't say what the attack was made with —
     * fails both {@code instanceof} checks and so reads as "no scope matched", leaving the roll
     * on Destreza with no null branch needed. That is also why this does <em>not</em> widen the
     * Ataque à Distância Graduação cap: {@code SkillGraduationService} asks which Attribute
     * currently <em>governs</em> the Perícia, and this one governs only some of its rolls.
     */
    ARREMESSO_PODEROSO("Você pode substituir o Atributo Base desta perícia por Força, mas " +
            "apenas para rolagens de ataques com armas de arremessos e magias.") {
        @Override
        public Optional<AttributeDomain> resolveSubstituteAttributeDomain(final AttackSource attackSource) {
            boolean thrownOrSpell = attackSource instanceof Spell
                    || (attackSource instanceof Weapon weapon && weapon.getCategory() == ItemCategory.THROWABLE);
            return thrownOrSpell ? Optional.of(AttributeDomain.STRENGTH) : Optional.empty();
        }
    },

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
        // Overrides the 4-arg overload, per the cascading convention, even though neither of
        // its two extra parameters is read here: only the target's distance matters.
        @Override
        public Optional<DamageBonus> resolveDamageBonus(final SkillType attackingSkillType, final SceneContext sceneContext, final CombatantSheet attackTarget, final Character actor) {
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
