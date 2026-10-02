package org.aventyrs.core.effect;

import java.util.List;

import lombok.Getter;

import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.InteractionResult;
import org.aventyrs.core.sheet.TargetScope;

/**
 * Dracônecer's Corrente (core 0.1.0): "o alvo desta magia recebe asas e capacidade de voar com Movimento Base de Voo
 * igual à sua velocidade em terra. O corpo dele é coberto por escamas de Dragão, que lhe fornecem RD e RM."
 *
 * <p>For the Magia's Duração: the Voo mode ({@link ModifierType#GRANTS_FLIGHT} — its figure is the land Movimento
 * Base, what {@code MovementService} already falls back to), and one instance each of RD and RM ({@value
 * DamageService#DEFAULT_DAMAGE_REDUCTION} apiece — the clause names no figure, read as one instance, table ruling
 * 2026-10-02). Granted under the Magia's name, like its {@code Efeito:}.
 */
@Getter
public class Draconato extends AbstractEffect implements EffectChain {

    private final Spell spell;
    private final int rounds;

    public Draconato(final Spell spell, final int rounds) {
        this.spell = spell;
        this.rounds = rounds;
    }

    @Override
    public String getDescription() {
        return spell.getEffectChainDescription();
    }

    @Override
    public InteractionResult applyTo(final CombatantSheet target) {
        if (rounds <= 0) {
            return reportChain(InteractionResult.builder().resultStatus(resolveStatus(target))).build();
        }
        String source = BodyChangeGrant.sourceOf(spell);
        List<Blessing> granted = List.of(
                new Blessing(ModifierType.GRANTS_FLIGHT, 1, rounds, TargetScope.SINGLE_TARGET, source),
                new Blessing(ModifierType.DAMAGE_REDUCTION, DamageService.DEFAULT_DAMAGE_REDUCTION, rounds,
                        TargetScope.SINGLE_TARGET, source),
                new Blessing(ModifierType.MAGIC_REDUCTION, DamageService.DEFAULT_DAMAGE_REDUCTION, rounds,
                        TargetScope.SINGLE_TARGET, source));
        granted.forEach(target::grantBlessing);
        return reportChain(InteractionResult.builder()
                .resultStatus(resolveStatus(target))
                .blessings(granted))
                .build();
    }
}
