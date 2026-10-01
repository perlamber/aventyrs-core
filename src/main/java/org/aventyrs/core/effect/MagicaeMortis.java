package org.aventyrs.core.effect;

import lombok.NonNull;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.DamageReceipt;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.ResourceType;
import org.aventyrs.core.title.TitleArchetype;

/**
 * Magicae Mortis — "Esta Magia recebe Roubo de Vida igual à 1+ seu número de Títulos Arcanos ou Abençoados
 * Despertos." A Corrente de Efeitos (the Magicae Mortis Talento of Morte e Renascimento, not yet in this core).
 *
 * <p>A Roubo de Vida read the way {@link OferendaMaldita} reads one: the caster recovers {@link #lifeSteal} PV, never
 * more than this hit actually dealt ({@code CombatantSheet#getLastDamageReceived()}), through {@code
 * CombatantSheet#healFromLifeSteal}. A Título counts when it is held — every held Título is Desperto — and its
 * {@link TitleArchetype} is {@code ARCANO} or {@code ABENCOADO}.
 */
public class MagicaeMortis extends AbstractEffect implements EffectChain {

    private final CombatantSheet caster;

    /** @param caster the Conjurador, whose Títulos size the Roubo de Vida and who recovers it */
    public MagicaeMortis(@NonNull final CombatantSheet caster) {
        this.caster = caster;
    }

    /** "1+ seu número de Títulos Arcanos ou Abençoados Despertos". */
    public int lifeSteal() {
        return 1 + (int) caster.getCharacter().getAllTitles().stream()
                .filter(title -> title.getArchetype() == TitleArchetype.ARCANO
                        || title.getArchetype() == TitleArchetype.ABENCOADO)
                .count();
    }

    @Override
    public String getDescription() {
        return "Esta Magia recebe Roubo de Vida igual à 1+ seu número de Títulos Arcanos ou Abençoados Despertos.";
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        int dealt = target.getLastDamageReceived().map(DamageReceipt::damage).orElse(0);
        int before = caster.getDamageTaken();
        if (dealt > 0) {
            caster.healFromLifeSteal(Math.min(lifeSteal(), dealt));
        }
        return reportChain(InteractionResult.builder()
                .resourceGainValue(before - caster.getDamageTaken())
                .resourceGainType(ResourceType.HIT_POINTS)
                .resultStatus(resolveStatus(target)))
                .build();
    }
}
