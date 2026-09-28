package org.aventyrs.core.feat;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.magic.SpellDurationService;
import org.aventyrs.core.magic.SpellDurationServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.sheet.TemporaryEffect;

/**
 * Barreira Mágica — the {@link ActiveAbility} {@link MetamagicoFeat#ARCANISTA_EXPERIENTE} grants:
 * "esta ação consome 1PA e 3PM, você recebe Bônus de +2 em suas Defesas. Barreiras criadas desta
 * forma tem Duração de 2 Rodadas … e Resfriamento 1."
 *
 * <p><b>The Defesas figure is a replacement ladder, not a sum</b> — the two rungs above restate it
 * rather than adding to it ("Barreiras Mágicas criadas por você <i>agora concedem</i> Bônus de +3",
 * then +5). So the figure is resolved from the holder's own held Talentos at activation time
 * ({@link #resolveDefesasBonus}) rather than being fixed on the ability, and only {@code
 * ARCANISTA_EXPERIENTE} grants the ability at all: were the upgrades to grant it too, {@code
 * Character#getActiveAbilities()} would list three Barreiras instead of one better one.
 *
 * <p>A single instance held on that constant, as {@code Feat#resolveActiveAbility()} requires —
 * {@code ActiveAbilityService#activate} matches a held ability by {@code ==}, and the Resfriamento
 * ledger is keyed by identity for the same reason.
 *
 * <p><b>What the holder's other Talentos change</b> (0.0.70) is read here too, off the holder, the
 * same way the ladder is: {@code ARTESAO_DE_BARREIRAS} makes it an Ação Livre, {@code
 * ENGENHEIRO_DO_MANA} takes 1PM off (never below 1), and "podem ter a Duração estendida por quaisquer
 * efeitos que aumente a Duração de Magias" adds {@link SpellDurationService#resolveNonSpellDurationIncrease}
 * — only an extension scoped to no kind of Magia reaches it, since a Barreira is none. The bonus is
 * granted under {@link #SOURCE}, so a refresh replaces rather than stacks and {@link #isActiveOn}
 * can tell one is running.
 */
public final class BarreiraMagicaActiveAbility implements ActiveAbility {

    /** The source every Barreira's bonus is granted under — what "com uma Barreira Mágica ativa" reads. */
    public static final String SOURCE = "BARREIRA_MAGICA";

    /** "+1 às Defesas de seus aliados adjacentes" — Mestre Arcanista. */
    private static final int MESTRE_ALLY_DEFESAS_BONUS = 1;

    /** "+3 às Defesas de seus aliados adjacentes" — Desafiador da Realidade. */
    private static final int DESAFIADOR_ALLY_DEFESAS_BONUS = 3;

    private static final SpellDurationService SPELL_DURATION_SERVICE = new SpellDurationServiceImpl();

    /** "Bônus de +2 em suas Defesas" — Arcanista Experiente's own figure. */
    private static final int BASE_DEFESAS_BONUS = 2;

    /** "agora concedem Bônus de +3 em suas Defesas" — Mestre Arcanista replaces the figure above. */
    private static final int MESTRE_DEFESAS_BONUS = 3;

    /** "+5" — Desafiador da Realidade, replacing it again. */
    private static final int DESAFIADOR_DEFESAS_BONUS = 5;

    private static final ActionCost ACTION_POINT_COST = ActionCost.ofActionPoints(1);
    private static final int MAGIC_POINT_COST = 3;
    private static final int DURATION_IN_ROUNDS = 2;
    private static final int COOLDOWN_IN_ROUNDS = 1;

    @Override
    public String getDescription() {
        return MetamagicoFeat.ARCANISTA_EXPERIENTE.getDescription();
    }

    @Override
    public ActionCost getActionPointCost() {
        return ACTION_POINT_COST;
    }

    @Override
    public ActionCost getActionPointCost(final Character holder) {
        return holds(holder, MetamagicoFeat.ARTESAO_DE_BARREIRAS) ? ActionCost.FREE_ACTION : ACTION_POINT_COST;
    }

    @Override
    public int getMagicPointCost() {
        return MAGIC_POINT_COST;
    }

    /** "O Custo de Mana … para ativar efeitos de Talentos Metamágicos é reduzido em -1PM (mínimo 1PM)". */
    @Override
    public int getMagicPointCost(final Character holder) {
        return holds(holder, MetamagicoFeat.ENGENHEIRO_DO_MANA)
                ? Math.max(MINIMUM_MAGIC_POINT_COST, MAGIC_POINT_COST - 1)
                : MAGIC_POINT_COST;
    }

    /** Engenheiro do Mana's "(mínimo 1PM)". */
    private static final int MINIMUM_MAGIC_POINT_COST = 1;

    @Override
    public int getDurationInRounds() {
        return DURATION_IN_ROUNDS;
    }

    /** "Resfriamento 1" — the catalog's first, and so far only, stated Resfriamento. */
    @Override
    public int getCooldownRounds() {
        return COOLDOWN_IN_ROUNDS;
    }

    /**
     * The Barreira itself: a {@link ModifierType#DEFESAS} {@link TemporaryBonus} — both Defesas,
     * since the clause says "suas Defesas" without narrowing to DF or DM — for {@link
     * #DURATION_IN_ROUNDS} Rodadas, at whatever figure the holder's highest rung states.
     */
    @Override
    public TemporaryEffect resolveEffect(final Character character) {
        return new TemporaryBonus(ModifierType.DEFESAS, resolveDefesasBonus(character),
                resolveDurationInRounds(character), SOURCE);
    }

    /** The 2 Rodadas, extended by every effect that lengthens any Magia's Duração. */
    public int resolveDurationInRounds(final Character character) {
        return DURATION_IN_ROUNDS + SPELL_DURATION_SERVICE.resolveNonSpellDurationIncrease(character);
    }

    /** Whether sheet has a Barreira Mágica running right now. */
    public static boolean isActiveOn(final CombatantSheet sheet) {
        return sheet != null && sheet.hasEffectFrom(SOURCE);
    }

    /**
     * What an active Barreira of holder grants each adjacent ally's Defesas: +1 with Mestre
     * Arcanista, +3 with Desafiador da Realidade (a replacement, like the holder's own figure), 0
     * below them or with no Barreira running.
     */
    public static int resolveAllyDefesasBonus(final CombatantSheet holder) {
        if (!isActiveOn(holder)) {
            return 0;
        }
        Character character = holder.getCharacter();
        if (holds(character, MetamagicoFeat.DESAFIADOR_DA_REALIDADE)) {
            return DESAFIADOR_ALLY_DEFESAS_BONUS;
        }
        return holds(character, MetamagicoFeat.MESTRE_ARCANISTA) ? MESTRE_ALLY_DEFESAS_BONUS : 0;
    }

    /** The highest rung the holder has reached — each restates the figure rather than adding to it. */
    private int resolveDefesasBonus(final Character character) {
        if (holds(character, MetamagicoFeat.DESAFIADOR_DA_REALIDADE)) {
            return DESAFIADOR_DEFESAS_BONUS;
        }
        return holds(character, MetamagicoFeat.MESTRE_ARCANISTA) ? MESTRE_DEFESAS_BONUS : BASE_DEFESAS_BONUS;
    }

    private static boolean holds(final Character character, final MetamagicoFeat rung) {
        return character.getFeats().stream().anyMatch(feat -> feat.catalogEntry() == rung);
    }
}
