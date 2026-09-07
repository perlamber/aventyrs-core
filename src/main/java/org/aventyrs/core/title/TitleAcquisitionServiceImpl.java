package org.aventyrs.core.title;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.feat.TitleAcquisitionPermission;
import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ACQUISITION_PREVENTED;

public class TitleAcquisitionServiceImpl implements TitleAcquisitionService {

    @Override
    public AventyrTitle grantTitle(final Character character, final AventyrTitle title, final TitleSlot slot)
            throws IllegalOperationException {
        TitleAcquisitionPermission permission = character.getFeats().stream()
                .map(feat -> feat.resolveTitleAcquisitionPermission(title.getIdentity(), character))
                .reduce(TitleAcquisitionPermission.NO_OPINION, TitleAcquisitionPermission::merge);
        if (permission == TitleAcquisitionPermission.PROHIBIT) {
            throw new IllegalOperationException(TITLE_ACQUISITION_PREVENTED);
        }
        character.grantTitle(title, slot);
        return title;
    }
}
