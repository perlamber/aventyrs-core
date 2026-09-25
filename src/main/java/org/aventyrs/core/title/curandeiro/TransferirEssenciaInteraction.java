package org.aventyrs.core.title.curandeiro;

import org.aventyrs.core.character.EgoDomain;
import org.aventyrs.core.character.services.DeterminationPointsServiceImpl;
import org.aventyrs.core.character.services.MagicPointsService;
import org.aventyrs.core.character.services.MagicPointsServiceImpl;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.ResourceType;

/** Transferir Essência — PM, or 1 temporary Sorte. See {@link TransferenciaInteraction}. */
public class TransferirEssenciaInteraction extends TransferenciaInteraction {

    private final MagicPointsService magicPointsService = new MagicPointsServiceImpl();

    public TransferirEssenciaInteraction() {
        super(MartirAltruistaAbility.TRANSFERIR_ESSENCIA, new DeterminationPointsServiceImpl(), EgoDomain.SORTE,
                ResourceType.MAGIC_POINTS);
    }

    @Override
    protected int currentPoints(final CombatantSheet sheet) {
        return magicPointsService.getCurrentMagicPoints(sheet.getCharacter(), sheet);
    }

    @Override
    protected int missingPoints(final CombatantSheet sheet) {
        return sheet.getManaSpent();
    }

    @Override
    protected void spend(final CombatantSheet sheet, final int amount) {
        sheet.spendMagicPoints(amount);
    }

    @Override
    protected void recover(final CombatantSheet sheet, final int amount) {
        sheet.recoverMagicPoints(amount);
    }
}
