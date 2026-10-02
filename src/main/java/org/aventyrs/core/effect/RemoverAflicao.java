package org.aventyrs.core.effect;

import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.HealingSource;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;

/**
 * Remover Aflição — "Os Alvos recuperam +2PV." A Corrente de Efeitos (Acólito da Luz Primordial's Fiel rung, on
 * "Magias e Habilidades que tenham como alvos um ou mais aliados" — a Devoto Talento not yet in this core).
 *
 * <p>A heal of {@link #RECOVERY}, chained onto whatever it rides. Like {@link Sobrecura} it is one heal effect with
 * its Magia ({@link HealingSource#spellChain}), so in Coma it adds nothing beyond the Magia's own 1PV.
 */
public class RemoverAflicao extends AbstractEffect implements EffectChain {

    /** "+2PV". */
    public static final int RECOVERY = 2;

    private final CombatantSheet healer;
    private final Spell parentSpell;

    /** With no healer or Magia known — keyed by this class alone. */
    public RemoverAflicao() {
        this(null, null);
    }

    /**
     * @param healer      who cast or activated what it rides, or {@code null}
     * @param parentSpell the Magia it rides, or {@code null} for a Habilidade
     */
    public RemoverAflicao(final CombatantSheet healer, final Spell parentSpell) {
        this.healer = healer;
        this.parentSpell = parentSpell;
    }

    @Override
    public String getDescription() {
        return "Os Alvos recuperam +2PV.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        int before = target.getDamageTaken();
        target.heal(RECOVERY, HealingSource.spellChain(RemoverAflicao.class, parentSpell, healer));
        return reportChain(InteractionResult.builder()
                .resourceGainValue(before - target.getDamageTaken())
                .resourceGainType(ResourceType.HIT_POINTS)
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
