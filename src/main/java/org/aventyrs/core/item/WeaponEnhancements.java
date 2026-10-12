package org.aventyrs.core.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.effect.EffectChain;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.util.DiceRoller;

/**
 * The one scan over <b>the weapon an attack is made with</b> — its fitted Obra-Prima's and Aprimoramentos'
 * Ataque/Danos columns and every {@link OffensiveImprovement} clause that names "this weapon's" hits (core
 * 0.1.6.9). This is the weapon-scoped pass those columns waited on: it reads the attack's own {@link
 * AttackSource}, never the loadout, so a sheathed Precisa adaga cannot sharpen a sword swing.
 *
 * <p>A <b>host</b> is the item the enhancements are fitted to: the attack source itself, or — for a view such as
 * {@link AttachedMeleeAttack} — the copy behind it ({@link Weapon#host()}). Only an intact offensive host counts;
 * a wreck grants nothing, the gate {@link Item}'s own enhancement hooks apply.
 *
 * <p>The Magia half ({@link #castingRollBonus}, {@link #spellDamageBonus}, {@link #spellHealingBonus}) reads every
 * <em>drawn</em> weapon instead, since a Magia is not made with one: "utilizando" means drawn here as everywhere.
 */
public final class WeaponEnhancements {

    private WeaponEnhancements() {
    }

    /** The intact offensive item source's enhancements are fitted to, or {@code null}. */
    public static Item hostOf(final AttackSource source) {
        if (!(source instanceof Weapon weapon)) {
            return null;
        }
        Item host = weapon.host();
        return host == null || host.isDestroyed() || host.getType() != ItemType.OFFENSIVE ? null : host;
    }

    /** The {@link OffensiveImprovement}s fitted to item, unwrapped — empty for {@code null}. */
    public static List<OffensiveImprovement> offensiveImprovements(final Item item) {
        return item == null ? List.of() : item.getImprovements().stream()
                .map(Improvement::offensiveDefinition)
                .filter(Objects::nonNull)
                .toList();
    }

    /** The Ataque columns of source's Obra-Prima and Aprimoramentos. */
    public static int attackRollBonus(final AttackSource source) {
        Item host = hostOf(source);
        if (host == null) {
            return 0;
        }
        return (host.getMasterpiece() == null ? 0 : host.getMasterpiece().getAttackBonus())
                + host.getImprovements().stream().mapToInt(Improvement::getAttackBonus).sum();
    }

    /** The Danos columns of source's Obra-Prima and Aprimoramentos — a flat dano-roll bonus. */
    public static int damageRollBonus(final AttackSource source) {
        Item host = hostOf(source);
        if (host == null) {
            return 0;
        }
        return (host.getMasterpiece() == null ? 0 : host.getMasterpiece().getDamageBonus())
                + host.getImprovements().stream().mapToInt(Improvement::getDamageBonus).sum();
    }

    /**
     * Alcance Estendido's "+1 em rolagens de Ataque e Dano [contra] alvos não adjacentes" — the same figure for both
     * rolls. 0 unless the Scene states attackTarget's distance and it is beyond Adjacente.
     */
    public static int nonAdjacentBonus(final AttackSource source, final CombatantSheet attackTarget,
                                       final SceneContext sceneContext) {
        if (attackTarget == null || sceneContext == null) {
            return 0;
        }
        Range distance = sceneContext.getDistanceTo(attackTarget);
        if (distance == null || distance.isWithin(Range.ADJACENTE)) {
            return 0;
        }
        return offensiveImprovements(hostOf(source)).stream().mapToInt(OffensiveImprovement::getNonAdjacentBonus).sum();
    }

    /** Roubo de Vida every hit with source carries. */
    public static int lifeSteal(final AttackSource source) {
        return offensiveImprovements(hostOf(source)).stream().mapToInt(OffensiveImprovement::getLifeSteal).sum();
    }

    /** Roubo de Mana every hit with source carries. */
    public static int manaSteal(final AttackSource source) {
        return offensiveImprovements(hostOf(source)).stream().mapToInt(OffensiveImprovement::getManaSteal).sum();
    }

    /** Roubo de Determinação every hit with source carries. */
    public static int determinationSteal(final AttackSource source) {
        return offensiveImprovements(hostOf(source)).stream()
                .mapToInt(OffensiveImprovement::getDeterminationSteal).sum();
    }

    /** The Correntes a hit with source gains once it clears the Corrente threshold. */
    public static List<EffectChain> effectChains(final AttackSource source, final CombatantSheet attacker,
                                                 final DiceRoller roller) {
        List<EffectChain> chains = new ArrayList<>();
        for (OffensiveImprovement improvement : offensiveImprovements(hostOf(source))) {
            EffectChain chain = improvement.createEffectChain(attacker, roller);
            if (chain != null) {
                chains.add(chain);
            }
        }
        return chains;
    }

    /** Whether every hit with source ignores the target's RD — Benção Vulcana. */
    public static boolean ignoresDamageReduction(final AttackSource source) {
        return offensiveImprovements(hostOf(source)).stream().anyMatch(OffensiveImprovement::ignoresDamageReduction);
    }

    /** The element every hit with source deals in addition to its own types — Benção Elemental — or {@code null}. */
    public static ElementalType additionalElement(final AttackSource source) {
        Item host = hostOf(source);
        return host == null ? null : host.getImprovements().stream()
                .filter(OffensiveItemImprovement.class::isInstance)
                .map(improvement -> ((OffensiveItemImprovement) improvement).getSelectedElementalType())
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    /** UD weapon's own Aprimoramentos add to its Distância de Ataque — Alcance Estendido. */
    public static int reachIncreaseUd(final Weapon weapon) {
        return offensiveImprovements(hostOf(weapon)).stream()
                .mapToInt(OffensiveImprovement::getAttackReachIncreaseUd).sum();
    }

    /** What the Conjuração roll of spell gains from every drawn weapon's Conjuração columns and Magia clauses. */
    public static int castingRollBonus(final Character character, final Spell spell) {
        int total = 0;
        for (Item host : drawnHosts(character)) {
            total += host.getMasterpiece() == null ? 0 : host.getMasterpiece().getCastingBonus();
            total += host.getImprovements().stream().mapToInt(Improvement::getCastingBonus).sum();
            total += offensiveImprovements(host).stream()
                    .mapToInt(improvement -> improvement.resolveCastingRollBonus(spell)).sum();
        }
        return total;
    }

    /** What spell's damage gains from every drawn weapon's Aprimoramentos — Arcanista, Benção Elemental. */
    public static int spellDamageBonus(final Character character, final Spell spell) {
        return drawnHosts(character).stream()
                .flatMap(host -> offensiveImprovements(host).stream())
                .mapToInt(improvement -> improvement.resolveSpellDamageBonus(spell))
                .sum();
    }

    /** What spell's healing gains from every drawn weapon's Aprimoramentos — Arcanista. */
    public static int spellHealingBonus(final Character character, final Spell spell) {
        return drawnHosts(character).stream()
                .flatMap(host -> offensiveImprovements(host).stream())
                .mapToInt(improvement -> improvement.resolveSpellHealingBonus(spell))
                .sum();
    }

    private static List<Item> drawnHosts(final Character character) {
        if (character == null || character.getDrawnWeapons() == null) {
            return List.of();
        }
        return character.getDrawnWeapons().stream()
                .map(WeaponEnhancements::hostOf)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
    }
}
