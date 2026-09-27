package org.aventyrs.core.character.services;

import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.feat.OrquicoFeat;
import org.aventyrs.core.race.Orc;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.ANCESTRAL_COUNSEL_IN_COMBAT;
import static org.aventyrs.core.util.TranslatableMessages.ANCESTRAL_COUNSEL_NOT_HELD;
import static org.aventyrs.core.util.TranslatableMessages.NOT_ENOUGH_MAGIC_POINTS;

public class AncestralCounselServiceImpl implements AncestralCounselService {

    private final MagicPointsService magicPointsService;

    public AncestralCounselServiceImpl() {
        this(new MagicPointsServiceImpl());
    }

    public AncestralCounselServiceImpl(@NonNull final MagicPointsService magicPointsService) {
        this.magicPointsService = magicPointsService;
    }

    @Override
    public AncestralCounsel perform(@NonNull final CombatantSheet orc, final SceneContext sceneContext) {
        Character character = orc.getCharacter();
        if (character == null || !(character.getRace() instanceof Orc)
                || orc.getRacialTraitSuppression().suppressesInnateTraits()) {
            throw new IllegalOperationException(ANCESTRAL_COUNSEL_NOT_HELD);
        }
        if (sceneContext != null && sceneContext.isCombatScene()) {
            throw new IllegalOperationException(ANCESTRAL_COUNSEL_IN_COMBAT);
        }
        if (magicPointsService.getCurrentMagicPoints(character, orc) < MAGIC_POINT_COST) {
            throw new IllegalOperationException(NOT_ENOUGH_MAGIC_POINTS);
        }
        orc.spendMagicPoints(MAGIC_POINT_COST);
        orc.grantCharge(COUNSEL);
        boolean superior = character.getFeats().stream()
                .anyMatch(feat -> feat.catalogEntry() == OrquicoFeat.AGNACAO_ANCESTRAL_SUPERIOR);
        return new AncestralCounsel(superior);
    }

    @Override
    public boolean spend(@NonNull final CombatantSheet orc) {
        return orc.consumeCharge(COUNSEL);
    }
}
