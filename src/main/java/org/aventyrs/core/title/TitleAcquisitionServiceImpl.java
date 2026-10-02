package org.aventyrs.core.title;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.feat.TitleAcquisitionPermission;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_ACQUISITION_PREVENTED;

public class TitleAcquisitionServiceImpl implements TitleAcquisitionService {

    @Override
    public AventyrTitle grantTitle(final Character character, final AventyrTitle title, final TitleSlot slot)
            throws IllegalOperationException {
        return grantTitle(character, null, title, slot);
    }

    @Override
    public AventyrTitle grantTitle(final Character character, final CharacterSheet sheet, final AventyrTitle title,
                                   final TitleSlot slot) throws IllegalOperationException {
        if (!isPermitted(character, sheet, title, slot)) {
            throw new IllegalOperationException(TITLE_ACQUISITION_PREVENTED);
        }
        character.grantTitle(title, slot);
        return title;
    }

    @Override
    public boolean isPermitted(final Character character, final AventyrTitle title) {
        TitleAcquisitionPermission permission = character.getFeats().stream()
                .map(feat -> feat.resolveTitleAcquisitionPermission(title.getIdentity(), character))
                .reduce(TitleAcquisitionPermission.NO_OPINION, TitleAcquisitionPermission::merge);
        return permission != TitleAcquisitionPermission.PROHIBIT;
    }

    @Override
    public boolean isPermitted(final Character character, final AventyrTitle title, final TitleSlot slot) {
        return isPermitted(character, null, title, slot);
    }

    @Override
    public boolean isPermitted(final Character character, final CharacterSheet sheet, final AventyrTitle title,
                               final TitleSlot slot) {
        return isPermitted(character, title)
                && character.getFeats().stream().allMatch(feat -> feat.permitsTitleSlot(slot, character, sheet));
    }
}
