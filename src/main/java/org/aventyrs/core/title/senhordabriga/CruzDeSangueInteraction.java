package org.aventyrs.core.title.senhordabriga;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.services.DeterminationPointsService;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.HitPointsService;
import org.aventyrs.core.character.services.HitPointsServiceImpl;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.title.AbstractTitleAbilityInteraction;
import org.aventyrs.core.title.TitleAbilityActivationRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Cruz de Sangue's activation (2PD, Reação on {@code SELF_TARGETED_BY_MELEE_ATTACK}) — everything
 * for 1 Rodada, and all of it <b>applied</b> to the activator, the only one it touches:
 * <ul>
 *   <li>"redutor em suas Defesas igual à metade de sua Destreza" — a negative {@code DEFESAS}
 *       Blessing, floored halving as every half-Atributo term here is;</li>
 *   <li>"aumenta sua Margem Crítica Menor de Defesas e Ataque Corpo-a-Corpo com Armas naturais em
 *       +2" — an activation window {@link SenhorDaBriga#resolveCriticalMarginIncrease} reads, since
 *       a Margem Crítica has no {@code ModifierType} a Blessing could carry;</li>
 *   <li>the counter-attack's "+1d6" — a one-attack budget whose die {@link
 *       SenhorDaBriga#resolveAttackModifiers} reports on the next attack; the caller offers that
 *       counter-attack when the Defesa fails;</li>
 *   <li>Malícia de Valentão's "RDS2" — a {@code DAMAGE_REDUCTION} Blessing, RDS being RD.</li>
 * </ul>
 * Applied rather than reported, like Sacrifício Ymiriano's: the activator is the only recipient,
 * and already in hand, so there is nothing for a caller to resolve.
 */
public class CruzDeSangueInteraction extends AbstractTitleAbilityInteraction {

    /** "Por 1 Rodada". */
    static final int ROUNDS = 1;
    /** "os danos deste ataque aumentam em +1d6" — one counter-attack. */
    static final int COUNTER_ATTACKS = 1;
    /** Malícia de Valentão, "RDS2". */
    static final int MALICIA_DAMAGE_REDUCTION = 2;

    private final HitPointsService hitPointsService = new HitPointsServiceImpl();

    public CruzDeSangueInteraction() {
        this(new DeterminationPointsServiceImpl());
    }

    public CruzDeSangueInteraction(final DeterminationPointsService determinationPointsService) {
        super(FantasmaDoRingueAbility.CRUZ_DE_SANGUE, determinationPointsService);
    }

    /** "redutor em suas Defesas igual à metade de sua Destreza" — as a positive figure. */
    static int resolveDefesasPenalty(final CombatantSheet activator) {
        return activator.getCharacter().getEffectiveAttributeTotal(AttributeDomain.DEXTERITY) / 2;
    }

    @Override
    protected InteractionResult resolve(final TitleAbilityActivationRequest request, final int determinationPoints) {
        CombatantSheet activator = request.getActivator();
        String source = FantasmaDoRingueAbility.CRUZ_DE_SANGUE.name();
        List<Blessing> blessings = new ArrayList<>();
        int penalty = resolveDefesasPenalty(activator);
        if (penalty > 0) {
            blessings.add(new Blessing(ModifierType.DEFESAS, -penalty, ROUNDS, TargetScope.SELF, source));
        }
        if (SenhorDaBriga.requireHeldBy(activator).holdsMaliciaDeValentao()) {
            blessings.add(new Blessing(ModifierType.DAMAGE_REDUCTION, MALICIA_DAMAGE_REDUCTION, ROUNDS,
                    TargetScope.SELF, source));
        }
        blessings.forEach(activator::grantBlessing);
        activator.openActivationWindow(FantasmaDoRingueAbility.CRUZ_DE_SANGUE, ROUNDS);
        activator.grantEnhancedAttacks(FantasmaDoRingueAbility.CRUZ_DE_SANGUE, COUNTER_ATTACKS);

        return InteractionResult.builder()
                .resultStatus(hitPointsService.getStatus(activator))
                .build();
    }
}
