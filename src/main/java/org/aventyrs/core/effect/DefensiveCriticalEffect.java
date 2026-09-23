package org.aventyrs.core.effect;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.rest.RestType;
import org.aventyrs.core.sheet.AttackerGuard;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Condition;
import org.aventyrs.core.sheet.ConditionType;
import org.aventyrs.core.sheet.ForcedTargeting;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.CriticalResult;
import org.aventyrs.core.skill.SkillType;
import org.aventyrs.core.util.DiceRoller;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

/**
 * One Efeito Crítico <b>Defensivo</b>, ready to apply — triggered by an Acerto Crítico on the
 * defender's own Defesa roll, granted by the Armadura/Escudo worn (see {@code
 * DefensiveCriticalEffects#grantedTo}), and acting on the defender and the attacker, both in hand.
 * {@code AttackReceiver} reports these; the caller calls {@link #apply()} on each, since {@code
 * AttackReceiver} itself stays report-only.
 *
 * <p>What each does, per {@code docs/rules/efeitos-criticos.txt}:
 * <ul>
 *   <li><b>Choque de AEther / Faísca de Determinação</b> — Menor: the defender recovers 2PM/2PD and the
 *   attacker loses 2. Maior: the defender's Multiplicador +1 for 2 Rodadas, the attacker's −1 until
 *   their next Descanso.</li>
 *   <li><b>Contra-Atacante</b> — reported: the counter-attack, with Vantagem on its dano (and, Maior,
 *   the defender's Efeito Crítico Menor).</li>
 *   <li><b>Ímpeto Defensivo</b> — the push reported (1/2UD); Menor +2 Defesas against that attacker for
 *   1 Rodada, Maior immunity to their attacks for 1 Rodada ({@code AttackerGuard}).</li>
 *   <li><b>Liberdade de Ação</b> — reported: a free 2UD move.</li>
 *   <li><b>Provocar</b> — the attacker takes 3/1 Dano Físico Primordial and is challenged (a 1-Rodada
 *   {@code ForcedTargeting} on them); the defender's Margem Crítica Menor to resist that attack +3/+1.</li>
 *   <li><b>Repelir e Suprimir</b> — the attacker's weapon takes 2d6/1d6 through its own Dureza; a
 *   repelled Magia's caster takes it instead and is suppressed.</li>
 *   <li><b>Retorno de Danos</b> — a melee attacker and their weapon take 3d6/1d6 + the defender's
 *   Vigor of Dano Físico Primordial.</li>
 *   <li><b>Surto Arcano</b> — reported: a quick cast.</li>
 * </ul>
 * Dano Físico <b>Primordial</b> is applied as bare PV loss: this core has no Primordial type, and
 * the resistances all exempt it ("não-PRIMORDIAIS").
 *
 * <p>TODO, each blocked on its own: Liberdade de Ação Maior's "Movimentos do atacante são
 * considerados em Terreno Difícil" (no per-movement Terreno); Repelir e Suprimir Maior's "a arma se
 * torna inutilizável por 1 Rodada" and Menor's "+1PA to attacks of the same type" (no per-weapon
 * usability flag, no per-attack-type PA price); Retorno de Danos' "projéteis são destruídos e não
 * aplicam áreas de efeitos" (no projectile or Área de Efeito resolution).
 */
@Getter
public final class DefensiveCriticalEffect {

    /** The types whose effect throws dice. */
    private static final Set<DefensiveCriticalEffectType> DICE_BEARING = EnumSet.of(
            DefensiveCriticalEffectType.REPELIR_E_SUPRIMIR, DefensiveCriticalEffectType.RETORNO_DE_DANOS);

    private final DefensiveCriticalEffectType type;
    private final CombatantSheet defender;
    private final CombatantSheet attacker;
    private final CriticalResult criticalResult;
    private final SkillType attackSkill;
    private final AttackSource attackSource;
    private final DiceRoller dice;

    private DefensiveCriticalEffect(final DefensiveCriticalEffectType type, final CombatantSheet defender,
                                    final CombatantSheet attacker, final CriticalResult criticalResult,
                                    final SkillType attackSkill, final AttackSource attackSource,
                                    final DiceRoller dice) {
        this.type = type;
        this.defender = defender;
        this.attacker = attacker;
        this.criticalResult = criticalResult;
        this.attackSkill = attackSkill;
        this.attackSource = attackSource;
        this.dice = dice;
    }

    /**
     * type, for defender's Acerto Crítico against attacker — empty when it throws dice and none
     * were supplied, or when there is no attacker for it to act on.
     *
     * @param criticalResult the defender's own roll — {@code ACERTO_CRITICO_MAIOR} or {@code _MENOR}
     */
    public static Optional<DefensiveCriticalEffect> of(@NonNull final DefensiveCriticalEffectType type,
                                                       @NonNull final CombatantSheet defender,
                                                       final CombatantSheet attacker,
                                                       @NonNull final CriticalResult criticalResult,
                                                       final SkillType attackSkill, final AttackSource attackSource,
                                                       final DiceRoller dice) {
        CriticalEffect.validateCriticalHit(criticalResult);
        if (attacker == null || (DICE_BEARING.contains(type) && dice == null)) {
            return Optional.empty();
        }
        return Optional.of(new DefensiveCriticalEffect(type, defender, attacker, criticalResult, attackSkill,
                attackSource, dice));
    }

    /** Whether this is a Maior critical. */
    public boolean isMajor() {
        return criticalResult == CriticalResult.ACERTO_CRITICO_MAIOR;
    }

    private int pick(final int major, final int minor) {
        return isMajor() ? major : minor;
    }

    /** Applies what can be applied to the two sheets, and reports the rest. */
    public DefensiveCriticalOutcome apply() {
        return switch (type) {
            case CHOQUE_DE_AETHER -> drain(ModifierType.MANA_MULTIPLIER, true);
            case FAISCA_DE_DETERMINACAO -> drain(ModifierType.DETERMINATION_MULTIPLIER, false);
            case CONTRA_ATACANTE -> new DefensiveCriticalOutcome(type, 0, 0, 0, true, isMajor(),
                    DefensiveCriticalOutcome.QuickCast.NONE, false);
            case IMPETO_DEFENSIVO -> impeto();
            case LIBERDADE_DE_ACAO -> new DefensiveCriticalOutcome(type, 0, 0, FREE_MOVE_UD, false, false,
                    DefensiveCriticalOutcome.QuickCast.NONE, false);
            case PROVOCAR -> provocar();
            case REPELIR_E_SUPRIMIR -> repelir();
            case RETORNO_DE_DANOS -> retorno();
            case SURTO_ARCANO -> new DefensiveCriticalOutcome(type, 0, 0, 0, false, false,
                    isMajor() ? DefensiveCriticalOutcome.QuickCast.ANY : DefensiveCriticalOutcome.QuickCast.SEED_OR_BUD,
                    false);
        };
    }

    /** Liberdade de Ação's "mover até 2UD". */
    static final int FREE_MOVE_UD = 2;
    /** Choque de AEther / Faísca de Determinação Menor: "recupera 2 … o atacante perde 2". */
    static final int POOL_SWING = 2;
    static final int MULTIPLIER_ROUNDS = 2;

    private DefensiveCriticalOutcome drain(final ModifierType multiplier, final boolean mana) {
        if (isMajor()) {
            defender.grantBlessing(new Blessing(multiplier, 1, MULTIPLIER_ROUNDS, TargetScope.SELF, type.name()));
            // "até que ele passe por um Descanso" — any tier.
            attacker.applyEffectUntilRest(new TemporaryBonus(multiplier, -1, null, type.name(), 1), RestType.MINIMO);
        } else if (mana) {
            defender.recoverMagicPoints(POOL_SWING);
            attacker.spendMagicPoints(POOL_SWING);
        } else {
            defender.recoverDeterminationPoints(POOL_SWING);
            attacker.spendDeterminationPoints(POOL_SWING);
        }
        return DefensiveCriticalOutcome.of(type);
    }

    private DefensiveCriticalOutcome impeto() {
        defender.applyEffect(isMajor()
                ? new AttackerGuard(attacker.getId(), 0, 0, true, 1)
                : AttackerGuard.defesas(attacker, 2, 1));
        return new DefensiveCriticalOutcome(type, 0, pick(2, 1), 0, false, false,
                DefensiveCriticalOutcome.QuickCast.NONE, false);
    }

    private DefensiveCriticalOutcome provocar() {
        int damage = pick(3, 1);
        attacker.applyDamage(damage);
        // "será desafiado e o próximo ataque dele deverá ter você novamente como alvo primário" — a
        // challenge, not stated to be an Encantamento, so it is applied directly rather than through
        // applyEnchantment's immunity door.
        attacker.applyEffect(new ForcedTargeting(defender, 1));
        defender.applyEffect(new AttackerGuard(attacker.getId(), 0, pick(3, 1), false, 1));
        return new DefensiveCriticalOutcome(type, damage, 0, 0, false, false,
                DefensiveCriticalOutcome.QuickCast.NONE, false);
    }

    private DefensiveCriticalOutcome repelir() {
        int damage = dice.rollD6(pick(2, 1));
        if (attackSource instanceof Spell) {
            // "Se o ataque repelido for mágico os danos são causados ao conjurador".
            attacker.applyDamage(damage);
            if (isMajor()) {
                attacker.applyCondition(new Condition(ConditionType.SILENCIO, 1, defender));
            }
            return new DefensiveCriticalOutcome(type, damage, 0, 0, false, false,
                    DefensiveCriticalOutcome.QuickCast.NONE, true);
        }
        if (attackSource instanceof Item weapon) {
            weapon.applyDamage(damage);
        }
        return DefensiveCriticalOutcome.of(type);
    }

    private DefensiveCriticalOutcome retorno() {
        if (attackSkill != SkillType.ATAQUE_CORPO_A_CORPO) {
            // A ranged attacker is out of reach; only their projectile is affected, which is a TODO.
            return DefensiveCriticalOutcome.of(type);
        }
        int damage = dice.rollD6(pick(3, 1))
                + defender.getCharacter().getEffectiveAttributeTotal(AttributeDomain.VIGOR);
        attacker.applyDamage(damage);
        if (attackSource instanceof Item weapon) {
            weapon.applyDamage(damage);
        }
        return new DefensiveCriticalOutcome(type, damage, 0, 0, false, false,
                DefensiveCriticalOutcome.QuickCast.NONE, false);
    }
}
