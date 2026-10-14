package org.aventyrs.core.item;

import org.aventyrs.core.character.Resistance;
import java.util.List;
import java.util.Objects;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.combat.EquipmentRetaliation;
import org.aventyrs.core.combat.Retaliation;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillType;

/**
 * The one scan over <b>every intact defensive item its wearer has equipped</b> — an Armadura's or Escudo's Obra-Prima
 * and {@link DefensiveImprovement}s (core 0.1.6.11), the twin of {@link WeaponEnhancements}. Where that pass reads the
 * weapon an attack is made with, this one reads the wearer: a defensive host's Ataque, Danos and Conjuração columns
 * reach <b>every</b> attack and every Magia its wearer makes (table ruling, 2026-10-12), and its reactive clauses
 * answer for the wearer being attacked or hit.
 *
 * <p>A <b>host</b> is an equipped, intact, non-offensive item. A wreck grants nothing, the same gate {@link Item}'s
 * own enhancement hooks apply; a carried-but-unworn copy is not in {@code Character#getEquipment()} and so grants
 * nothing either.
 */
public final class DefensiveEnhancements {

    /** Benção Ymiriana's Danos column: +1, becoming +2 — so the window adds 1 per fitted copy. */
    static final int YMIRIAN_WINDOW_DAMAGE_BONUS = 1;
    /** Espinhosa: "Dano dos espinhos aumentado para 2" — its Favor's figure, to the attacker and their weapon. */
    static final int THORNS_DAMAGE = 2;
    /** Rigidez Refletora: "Armas de atacantes corpo-a-corpo sofrem 2 pontos de danos". */
    static final int REFLECTIVE_WEAPON_DAMAGE = 2;
    /** Benção Vulcana's and Benção Elduriana's "Danos … sofridos reduzidos em -1". */
    static final int TYPED_DAMAGE_REDUCTION = Resistance.RDS.instances(1);
    /** Benção Elduriana: "Duração de Encantamentos e Maldições hostis reduzidas em -1 Rodada". */
    static final int HOSTILE_DURATION_REDUCTION = 1;
    /** Ladra do AEther: "você recupera 1PM e 1PD". */
    static final int AETHER_THIEF_RECOVERY = 1;

    /** The source Benção Ymiriana's window is granted under — a second attack renews it rather than stacking. */
    public static final String YMIRIAN_WINDOW_SOURCE = DefensiveImprovement.BENCAO_YMIRIANA.getName();

    private DefensiveEnhancements() {
    }

    /** The equipped, intact, non-offensive items of character. */
    public static List<Item> wornHosts(final Character character) {
        if (character == null || character.getEquipment() == null) {
            return List.of();
        }
        return character.getEquipment().stream()
                .filter(Objects::nonNull)
                .filter(item -> !item.isDestroyed() && item.getType() != ItemType.OFFENSIVE)
                .toList();
    }

    /** The {@link DefensiveImprovement}s fitted to item, unwrapped. */
    public static List<DefensiveImprovement> defensiveImprovements(final Item item) {
        return item == null ? List.of() : item.getImprovements().stream()
                .map(improvement -> improvement instanceof ItemImprovement fitted ? fitted.getDefinition()
                        : improvement instanceof DefensiveImprovement bare ? bare : null)
                .filter(Objects::nonNull)
                .toList();
    }

    /** How many copies of improvement character's worn hosts carry. */
    public static int count(final Character character, final DefensiveImprovement improvement) {
        return (int) wornHosts(character).stream()
                .flatMap(host -> defensiveImprovements(host).stream())
                .filter(fitted -> fitted == improvement)
                .count();
    }

    /** The Ataque columns of every worn host — added to every attack roll its wearer makes. */
    public static int attackRollBonus(final Character character) {
        return wornHosts(character).stream()
                .mapToInt(host -> (host.getMasterpiece() == null ? 0 : host.getMasterpiece().getAttackBonus())
                        + defensiveImprovements(host).stream().mapToInt(DefensiveImprovement::getAttackBonus).sum())
                .sum();
    }

    /** The Danos columns of every worn host — a flat bonus on every dano roll its wearer makes. */
    public static int damageRollBonus(final Character character) {
        return wornHosts(character).stream()
                .mapToInt(host -> (host.getMasterpiece() == null ? 0 : host.getMasterpiece().getDamageBonus())
                        + defensiveImprovements(host).stream().mapToInt(DefensiveImprovement::getDamageBonus).sum())
                .sum();
    }

    /** The Conjuração columns of every worn host — added to every Conjuração roll its wearer makes. */
    public static int castingRollBonus(final Character character) {
        return wornHosts(character).stream()
                .mapToInt(host -> (host.getMasterpiece() == null ? 0 : host.getMasterpiece().getCastingBonus())
                        + defensiveImprovements(host).stream().mapToInt(DefensiveImprovement::getCastingBonus).sum())
                .sum();
    }

    /**
     * Benção Ymiriana: "O Bônus em Danos aumenta para +2 por 1 Rodada após sofrer um ataque". Fires on being the
     * target of <b>any</b> attack, hit or miss, and lasts until the end of the wearer's next Turn (table ruling,
     * 2026-10-12) — so one granted outside the wearer's Turn counts down at the end of their next one, and one granted
     * inside it outlasts this one. Renewed, never stacked: a sourced {@code DAMAGE_ROLL_BONUS}, read by every dano roll.
     *
     * @return whether a window was opened (the wearer carries at least one intact Ymiriana host)
     */
    public static boolean onAttacked(final CombatantSheet defender) {
        if (defender == null) {
            return false;
        }
        int copies = count(defender.getCharacter(), DefensiveImprovement.BENCAO_YMIRIANA);
        if (copies == 0) {
            return false;
        }
        int rounds = defender.isInOwnTurn() ? 2 : 1;
        defender.grantBlessing(new Blessing(ModifierType.DAMAGE_ROLL_BONUS, copies * YMIRIAN_WINDOW_DAMAGE_BONUS,
                rounds, TargetScope.SELF, YMIRIAN_WINDOW_SOURCE));
        return true;
    }

    /**
     * Espinhosa and Rigidez Refletora against an attack on defender — fired hit or miss, against the attack's
     * <b>primary</b> target only, never an additional one (table ruling, 2026-10-12). Espinhosa's 2 Dano Físico reaches
     * the melee attacker and their weapon; Rigidez Refletora's 2 the melee attacker's weapon, and a projectile aimed at
     * its wearer is destroyed. {@code null} when nothing answers.
     *
     * @param attackSkill the attack's Perícia — melee is judged by it, never by the weapon's category
     * @param projectile  the projectile the attack fired, or {@code null}
     */
    public static EquipmentRetaliation retaliationAgainst(final CombatantSheet defender, final SkillType attackSkill,
                                                         final ProjectileItem projectile) {
        if (defender == null) {
            return null;
        }
        Character wearer = defender.getCharacter();
        int thorns = count(wearer, DefensiveImprovement.ESPINHOSA);
        int reflective = count(wearer, DefensiveImprovement.RIGIDEZ_REFLETORA);
        boolean melee = attackSkill == SkillType.ATAQUE_CORPO_A_CORPO;
        Retaliation attackerDamage = melee && thorns > 0
                ? new Retaliation(thorns * THORNS_DAMAGE, new DamageDescriptor(DamageType.FISICO), null, 0)
                : null;
        int weaponDamage = melee ? thorns * THORNS_DAMAGE + reflective * REFLECTIVE_WEAPON_DAMAGE : 0;
        boolean projectileDestroyed = projectile != null && reflective > 0;
        if (attackerDamage == null && weaponDamage == 0 && !projectileDestroyed) {
            return null;
        }
        return new EquipmentRetaliation(attackerDamage, weaponDamage, projectileDestroyed);
    }

    /**
     * Benção Vulcana's "Danos Elementais sofridos reduzidos em -1" and "Danos Físicos sofridos reduzidos em -1", and
     * Benção Elduriana's "Danos Mágicos sofridos reduzidos em -1" — summed per fitted copy, against one hit. A hit
     * naming an element — an Elemental, a Mágico Elemental, or a weapon's Benção Elemental on top — is Elemental; a
     * Físico or untyped one is Físico; any Mágico hit is Mágico, whatever its qualifier (table ruling, 2026-10-12).
     */
    public static int damageTakenReduction(final Character character, final DamageType effectiveType,
                                           final DamageDescriptor descriptor, final boolean additionalElement) {
        boolean elemental = effectiveType == DamageType.ELEMENTAL || additionalElement
                || descriptor != null && descriptor.elementalType() != null;
        boolean physical = effectiveType == null || effectiveType == DamageType.FISICO;
        boolean magical = effectiveType == DamageType.MAGICO;
        int vulcana = count(character, DefensiveImprovement.BENCAO_VULCANA);
        int elduriana = count(character, DefensiveImprovement.BENCAO_ELDURIANA);
        return TYPED_DAMAGE_REDUCTION * ((elemental ? vulcana : 0) + (physical ? vulcana : 0) + (magical ? elduriana : 0));
    }

    /**
     * Benção Elduriana: how many Rodadas a hostile Encantamento or Maldição landing on character loses — 1 per fitted
     * copy. 0 for any other Magia; whether it is hostile is the caster's question ({@code SpellCastingService}).
     */
    public static int hostileDurationReduction(final Character character, final Spell spell) {
        if (spell == null || !isEnchantmentOrCurse(spell)) {
            return 0;
        }
        return HOSTILE_DURATION_REDUCTION * count(character, DefensiveImprovement.BENCAO_ELDURIANA);
    }

    /**
     * Maldição: Ladra do AEther — "Sempre que você for afetado por efeitos de Magias que não sejam sementes … você
     * recupera 1PM e 1PD", for any Magia not cast by the wearer (table ruling, 2026-10-12), once per cast. Applied
     * here; the caller calls it once per target a Magia lands on. The Monstros' magical abilities half stays TODO: no
     * monster ability is classified as magical.
     *
     * @return the PM recovered (the PD equal it, unless something blocks recovery)
     */
    public static int onAffectedBySpell(final CombatantSheet target, final Spell spell, final CombatantSheet caster) {
        if (target == null || spell == null || caster == target || spell.getBranchLevel() == BranchLevel.SEMENTE) {
            return 0;
        }
        int copies = count(target.getCharacter(), DefensiveImprovement.LADRA_DO_AETHER);
        if (copies == 0) {
            return 0;
        }
        // recoverMagicPoints answers what is still spent, so what came back is the difference.
        int spentBefore = target.recoverMagicPoints(0);
        int spentAfter = target.recoverMagicPoints(copies * AETHER_THIEF_RECOVERY);
        target.recoverDeterminationPoints(copies * AETHER_THIEF_RECOVERY);
        return spentBefore - spentAfter;
    }

    /**
     * Maldição: Solve-Vidas — "Sempre que um Personagem inimigo vivo tiver seus PV reduzidos à 0 ou menos em até
     * Distância Curta ele será imediatamente morto e você recupera uma quantidade de PV igual ao Vigor dele". A sweep,
     * like {@code Scene#settleSummons}: the caller runs it for each wearer after applying damage to anyone, with the
     * wearer's own context (whose distances it reads). Whatever dropped the enemy counts, and the death is an ordinary
     * one — not beyond revival (table ruling, 2026-10-12). An enemy already dead is not killed again.
     *
     * <p>Every sheet in reach must be real for this all-in-one form. A client holding only a stand-in for a victim
     * splits it: {@link #soulReapingReach} on the wearer's side, {@link #reap} on the victim's owner, {@link
     * #recoverReapedVigor} back on the wearer's.
     *
     * @return the enemies killed — the wearer has recovered each one's Vigor total in PV
     */
    public static List<CombatantSheet> reapSouls(final CombatantSheet wearer,
                                                 final org.aventyrs.core.scene.SceneContext wearerContext) {
        List<CombatantSheet> reaped = new java.util.ArrayList<>();
        for (CombatantSheet victim : soulReapingReach(wearer, wearerContext)) {
            int vigor = reap(victim);
            if (vigor >= 0) {
                reaped.add(victim);
                recoverReapedVigor(wearer, vigor);
            }
        }
        return reaped;
    }

    /** The living enemies within Distância Curta of a Solve-Vidas wearer — empty when wearer wears none. */
    public static List<CombatantSheet> soulReapingReach(final CombatantSheet wearer,
                                                        final org.aventyrs.core.scene.SceneContext wearerContext) {
        if (wearer == null || wearerContext == null
                || count(wearer.getCharacter(), DefensiveImprovement.SOLVE_VIDAS) == 0) {
            return List.of();
        }
        return wearerContext.getEnemiesWithin(org.aventyrs.core.scene.Range.DISTANCIA_CURTA).stream()
                .filter(victim -> victim != wearer && isLiving(victim))
                .toList();
    }

    /**
     * Kills victim if a Solve-Vidas may — living, at 0 PV or less, not already dead — with enough curse damage to cross
     * the DEAD threshold, as Execução Real deals it (but not beyond revival).
     *
     * @return the victim's Vigor total — what the wearer recovers — or -1 when it was not killed
     */
    public static int reap(final CombatantSheet victim) {
        if (victim == null || !isLiving(victim)) {
            return -1;
        }
        org.aventyrs.core.character.services.HitPointsService hitPoints =
                new org.aventyrs.core.character.services.HitPointsServiceImpl();
        if (hitPoints.getCurrentHitPoints(victim.getCharacter(), victim) > 0
                || hitPoints.getStatus(victim) == org.aventyrs.core.character.CharacterStatus.DEAD) {
            return -1;
        }
        victim.applyCurseDamage(2 * hitPoints.getMaxHitPoints(victim.getCharacter()));
        return Math.max(0, victim.getAttributeTotal(org.aventyrs.core.character.AttributeDomain.VIGOR));
    }

    /** The wearer's half of a reaping: vigor PV back, as a Solve-Vidas heal. */
    public static void recoverReapedVigor(final CombatantSheet wearer, final int vigor) {
        if (wearer != null && vigor > 0) {
            wearer.heal(vigor, new org.aventyrs.core.sheet.HealingSource(DefensiveImprovement.SOLVE_VIDAS, true,
                    wearer, null, null));
        }
    }

    /** "Vivo" — not a Renascido, nor a foe marked undead. ⚠️ A foe with no Raça counts as living. */
    public static boolean isLiving(final CombatantSheet sheet) {
        if (sheet.hasUndeadAnatomy()
                || sheet instanceof org.aventyrs.core.monster.MonsterSheet monster && monster.isUndead()) {
            return false;
        }
        Character character = sheet.getCharacter();
        return character == null || character.getRace() == null
                || character.getRace().getCreatureType() != org.aventyrs.core.race.CreatureType.RENASCIDO;
    }

    private static boolean isEnchantmentOrCurse(final Spell spell) {
        return spell.getPrimaryType() == MagicType.ENCANTAMENTO || spell.getSecondaryType() == MagicType.ENCANTAMENTO
                || spell.getPrimaryType() == MagicType.MALDICAO || spell.getSecondaryType() == MagicType.MALDICAO;
    }
}
