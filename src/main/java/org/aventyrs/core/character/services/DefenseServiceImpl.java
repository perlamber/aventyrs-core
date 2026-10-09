package org.aventyrs.core.character.services;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.CharacterSkill;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.item.Item;
import org.aventyrs.core.item.ItemCategory;
import org.aventyrs.core.item.ShieldAttack;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.modifier.ModifierResolver;
import org.aventyrs.core.modifier.ModifierResolverImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.AttackerGuard;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillExcellency;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Map;

public class DefenseServiceImpl implements DefenseService {

    private final ModifierResolver modifierResolver;

    /** Bastião de Vidro reads what mitigation the holder forgoes to turn it into Defesa. */
    private final DamageService damageService = new DamageServiceImpl();

    public DefenseServiceImpl() {
        this(new ModifierResolverImpl());
    }

    public DefenseServiceImpl(final ModifierResolver modifierResolver) {
        this.modifierResolver = modifierResolver;
    }

    @Override
    public int getTotalDefense(final Character character, final DefenseType defenseType) {
        return getTotalDefense(character, defenseType, null);
    }

    @Override
    public int getTotalDefense(final Character character, final DefenseType defenseType, final SceneContext sceneContext) {
        return sumAbilityModifiers(character, defenseType, null) + sumEquipment(character, defenseType, sceneContext)
                + sumFeats(character, defenseType, sceneContext)
                + sumTitleBaseDefesas(character, sceneContext, null);
    }

    @Override
    public int getTotalDefense(final CombatantSheet target, final DefenseType defenseType) {
        return getTotalDefense(target, defenseType, null);
    }

    @Override
    public int getTotalDefense(final CombatantSheet target, final DefenseType defenseType,
                               final SceneContext sceneContext) {
        return getTotalDefense(target, defenseType, sceneContext, null);
    }

    @Override
    public int getTotalDefense(final CombatantSheet target, final DefenseType defenseType,
                               final SceneContext sceneContext, final DamageDescriptor damageDescriptor) {
        // Início Defensivo: "você ignora efeitos que reduzem Defesas" — timed maluses subtracted back
        // out, and a Condição's net malus floored at nothing.
        boolean ignoreMaluses = target.getCharacter().getFeats().stream()
                .anyMatch(feat -> feat.ignoresDefenseMaluses(sceneContext, target));
        int timed = target.getTemporaryBonus(ModifierType.DEFESAS)
                + target.getTemporaryBonus(defenseType.getModifierType());
        // Desprevenido's -4 Defesas, and anything conferring it (Caído, Flanqueado,
        // Cego, or the fear ladder while close enough to its origin).
        int conditions = target.getConditionBonus(ModifierType.DEFESAS, sceneContext)
                + target.getConditionBonus(defenseType.getModifierType(), sceneContext);
        if (ignoreMaluses) {
            timed -= target.getTemporaryMalus(ModifierType.DEFESAS) + target.getTemporaryMalus(defenseType.getModifierType());
            conditions = Math.max(0, conditions);
        }
        return sumAbilityModifiers(target.getCharacter(), defenseType, target)
                + sumEquipment(target, defenseType, sceneContext, damageDescriptor)
                + sumFeats(target.getCharacter(), defenseType, sceneContext, target)
                + sumTitleBaseDefesas(target.getCharacter(), sceneContext, target)
                + timed
                + conditions
                + sumGuardsAgainstOpponent(target, sceneContext)
                + mitigationForgoneAsDefense(target, sceneContext)
                + adjacentBarreiraBonus(sceneContext)
                + sumDefenseBonusesAgainst(target.getCharacter(), damageDescriptor)
                // A Torre's "+2 nas Defesas" (core 0.0.92), own or a Prodigioso ally's.
                + org.aventyrs.core.subordinate.SubordinateBenefit.DEFESAS * org.aventyrs.core.subordinate.SubordinateBenefits.count(target, sceneContext, org.aventyrs.core.subordinate.SubordinateBenefit.TORRE_DEFESAS);
    }

    /** Vulnerabilidade's Desvantagem to defend against the attack's own kind — only when the kind is known. */
    private static int sumDefenseBonusesAgainst(final Character character, final DamageDescriptor damageDescriptor) {
        return damageDescriptor == null ? 0 : character.getFeats().stream()
                .mapToInt(feat -> feat.resolveDefenseBonusAgainst(damageDescriptor, character))
                .sum();
    }

    /**
     * Mestre Arcanista's and Desafiador da Realidade's "+1/+3 às Defesas de seus aliados adjacentes"
     * while their Barreira Mágica runs — <b>scanned</b> off the recipient's own snapshot, not granted,
     * since the figure reads nothing of the recipient and adjacency is mutual. The best adjacent ally
     * counts, not the sum: two Barreiras beside you are the same protection twice (a reading — the
     * text names one Barreira's allies). A {@code null} context has nobody adjacent.
     */
    private static int adjacentBarreiraBonus(final SceneContext sceneContext) {
        if (sceneContext == null) {
            return 0;
        }
        return sceneContext.getAlliesWithin(org.aventyrs.core.scene.Range.ADJACENTE).stream()
                .mapToInt(org.aventyrs.core.feat.BarreiraMagicaActiveAbility::resolveAllyDefesasBonus)
                .max().orElse(0);
    }

    /**
     * Bastião de Vidro: "Você não é beneficiado por efeitos de Redução de Danos Sofridos, ao invés disso
     * você recebe Bônus em Defesa igual ao valor que você receberia de Redução de Danos Sofridos. Você
     * não é beneficiado por RA, RD e RM, ao invés disso você recebe Bônus de +1 em Defesas para cada um
     * destes efeitos." The RDS the holder would have, plus 1 for each of RA, RD and RM they would have
     * any of — read off {@link DamageService}, whose own figures still report what is forgone.
     */
    private int mitigationForgoneAsDefense(final CombatantSheet target, final SceneContext sceneContext) {
        if (target.getCharacter().getFeats().stream().noneMatch(Feat::forgoesDamageMitigation)) {
            return 0;
        }
        int bonus = damageService.getTotalDamageTakenReduction(target, sceneContext);
        bonus += damageService.getTotalAbsoluteDamageReduction(target, sceneContext) > 0 ? 1 : 0;
        bonus += damageService.getTotalDamageReduction(target, DamageType.FISICO, null) > 0 ? 1 : 0;
        bonus += damageService.getTotalMagicReduction(target) > 0 ? 1 : 0;
        return bonus;
    }

    /**
     * Defesas this target holds against <b>one attacker</b> — {@code sheet.AttackerGuard}s
     * (Prevenir, Ímpeto Defensivo Menor, Rolamento Ofensivo) matched against the defence roll's
     * {@code SceneContext#getOpposedCharacter()}, which is the attacker on a defence roll. No
     * context, or none naming an opponent, sees none — "cannot tell" withholds.
     */
    private int sumGuardsAgainstOpponent(final CombatantSheet target, final SceneContext sceneContext) {
        if (sceneContext == null) {
            return 0;
        }
        return target.getGuardsAgainst(sceneContext.getOpposedCharacter()).stream()
                .mapToInt(AttackerGuard::getDefesasBonus)
                .sum();
    }

    /**
     * Defesas every held Título's own Efeito Base grants its holder — Santo's Despertar "+2 em
     * suas Defesas, esse Bônus aumenta em +1 para cada aliado adjacente, Especializações e
     * Supremas que você possua" ({@link org.aventyrs.core.title.AventyrTitle
     * #resolveBaseDefesasBonus}).
     *
     * <p>Broad, never scoped: a base effect's "suas Defesas" is plural, so the figure lands on DF
     * and DM alike and is added once whichever {@code defenseType} was asked for — which is why
     * this takes none.
     *
     * <p>Called from both the {@link Character} and {@link CombatantSheet} paths: those two
     * deliberately do not cascade (they answer different questions, per CLAUDE.md), so a source
     * added to one only would silently appear on half the callers.
     *
     * <p>Scanned rather than granted, so a bonus that changes as allies move is right by
     * construction. The Título-<i>Primário</i> half is the deliberate exception — it derives from
     * the holder's adjacency rather than the recipient's, so it cannot be scanned from here and is
     * granted as a real Aura by {@code Scene#refreshProjectedAuras} instead, arriving in the
     * {@code getTemporaryBonus(DEFESAS)} term below.
     */
    private int sumTitleBaseDefesas(final Character character, final SceneContext sceneContext,
                                    final CombatantSheet sheet) {
        return character.getAllTitles().stream()
                .mapToInt(title -> title.resolveBaseDefesasBonus(sceneContext, character, sheet,
                        character.getPrimaryTitle() == title))
                .sum();
    }

    /**
     * The standard three-source {@code @Modifier} scan (see this service's interface javadoc),
     * each source summed for both {@link ModifierType#DEFESAS} and defenseType's own scoped
     * type. Uses {@link SkillCompetencyAbility#allFor} so a racial ability granting a Defesa
     * counts identically to an acquired one.
     *
     * <p>sheet is nullable, and is the only way a Forma suppressing its holder's race can drop
     * the racial abilities from this scan — the {@link Character}-taking public overloads pass
     * {@code null} and so never suppress.
     */
    private int sumAbilityModifiers(final Character character, final DefenseType defenseType,
                                    final CombatantSheet sheet) {
        int total = sumBothTypes(character.getAttributeAbilities(), defenseType);
        total += sumBothTypes(SkillCompetencyAbility.allFor(character, sheet), defenseType);
        for (Map.Entry<SkillType, CharacterSkill> entry : character.getSkills().entrySet()) {
            int graduationValue = entry.getValue().getGraduation().getGraduationValue();
            List<SkillExcellency> unlockedExcellencies = SkillExcellency.unlockedBy(
                    entry.getKey().getExcellencyClass(), graduationValue);
            total += sumBothTypes(unlockedExcellencies, defenseType);
        }
        return total;
    }

    /**
     * Every equipped {@link Item}'s contribution: its flat DF or DM column, the item's
     * Masterpiece contribution (which can replace its own base column for a "muda para" Favor),
     * plus whichever Defesa-typed {@code ItemBonus}es its {@code ItemFavor} currently grants.
     */
    private int sumEquipment(final Character character, final DefenseType defenseType,
                             final SceneContext sceneContext) {
        return sumEquipment(character, defenseType, sceneContext, null);
    }

    private int sumEquipment(final Character character, final DefenseType defenseType,
                             final SceneContext sceneContext, final DamageDescriptor damageDescriptor) {
        int total = 0;
        for (Item item : character.getEquipment()) {
            total += item.getEffectiveDefenseBonus(defenseType, character, sceneContext, damageDescriptor);
        }
        return total;
    }

    /**
     * The {@link CombatantSheet}-aware equipment pass — an Escudo's Favor bonus gated on live
     * state (Escudo Médio's "se não realizou ação ofensiva nesta Rodada") resolves only here.
     */
    private int sumEquipment(final CombatantSheet target, final DefenseType defenseType,
                             final SceneContext sceneContext, final DamageDescriptor damageDescriptor) {
        int total = 0;
        for (Item item : target.getCharacter().getEquipment()) {
            int bonus = item.getEffectiveDefenseBonus(defenseType, target, sceneContext, damageDescriptor);
            // Atacar com Escudos: a shield that attacked keeps only part of what it grants.
            total += item.getCategory() == ItemCategory.SHIELD ? retainedShieldDefense(target, item, bonus) : bonus;
        }
        return total;
    }

    /**
     * What bonus becomes once shield has attacked since its wielder's latest Turn began — "perde metade
     * dos bônus em Defesas concedidos por ele até o início de seu próximo turno … atacar duas ou mais
     * vezes com um escudo faz com que você não receba seus bônus" — eased by the Talentos that ease it.
     * The attacks are read off the Rodada's log and the holder's latest own Turn, so the loss holds from
     * the swing until the holder's next Turn begins. See {@link ShieldAttack#retained}.
     */
    private static int retainedShieldDefense(final CombatantSheet target, final Item shield, final int bonus) {
        return ShieldAttack.retained(target, source -> ShieldAttack.swings(source, shield), bonus);
    }

    /**
     * Every held Talento's unconditional contribution to this Defesa — {@code
     * Feat#resolveDefenseBonus}. Feats are deliberately not part of the {@code @Modifier} scan
     * above (nothing else in this codebase scans them reflectively), so they get their own
     * explicit pass, the same way equipment does.
     */
    private int sumFeats(final Character character, final DefenseType defenseType, final SceneContext sceneContext) {
        return sumFeats(character, defenseType, sceneContext, null);
    }

    /**
     * holder is the combatant's own sheet, or {@code null} on the {@code Character}-only entry
     * point — a Talento conditioned on live combat state reads that as "condition not met".
     */
    private int sumFeats(final Character character, final DefenseType defenseType,
                          final SceneContext sceneContext, final CombatantSheet holder) {
        return character.getFeats().stream()
                .mapToInt(feat -> feat.resolveDefenseBonus(defenseType, character, sceneContext, holder))
                .sum();
    }

    private int sumBothTypes(final java.util.Collection<?> sources, final DefenseType defenseType) {
        return modifierResolver.sumModifiers(sources, ModifierType.DEFESAS)
                + modifierResolver.sumModifiers(sources, defenseType.getModifierType());
    }
}
