package org.aventyrs.core.sheet;

import lombok.Getter;
import org.aventyrs.core.item.AbstractItem;
import org.aventyrs.core.item.PowerStone;

/**
 * A Pedra do Poder vinculada: stone bound to host for the Duração of one Vinculação ({@code
 * docs/rules/pedras-do-poder.txt}, "Vinculando Pedras do Poder"). Held on the bearer's sheet, where it counts down
 * like any {@link TemporaryEffect}. While it runs, the host's {@code Item#getPowerStone()} is the stone, which is the
 * one thing every stone effect reads.
 *
 * <p><b>Ending it is not silent.</b> "Finalizada a Duração dos Efeitos, a Pedra do Poder e o Equipamento – ambos –
 * entram em estado de Resfriamento." So when it lapses, {@code AbstractCombatantSheet#tickTemporaryEffects} hands it
 * to {@link CombatantSheet#endPowerStoneBinding}, which unbinds the host and starts both cooldowns. That is the same
 * "an expiring effect that must <em>do</em> something" case a lapsing Forma is. Created only by {@code
 * PowerStoneService#bind}.
 */
@Getter
public class PowerStoneBinding extends TemporaryEffect {

    private final AbstractItem host;
    private final PowerStone stone;
    /** The Duração this binding was given — Mecanismo de Sobrecarga doubled, Última Carga fixed at 2. */
    private final int durationRounds;
    /** The Resfriamento both stone and host enter when it ends — Mecanismo de Sobrecarga doubled. */
    private final int cooldownRounds;

    public PowerStoneBinding(final AbstractItem host, final PowerStone stone, final int durationRounds,
                             final int cooldownRounds) {
        super(durationRounds);
        this.host = host;
        this.stone = stone;
        this.durationRounds = durationRounds;
        this.cooldownRounds = cooldownRounds;
    }
}
