package org.aventyrs.core.title;

import static org.aventyrs.core.util.TranslatableMessages.TITLE_AWAKENING_CHOICE_NOT_ELIGIBLE;
import static org.aventyrs.core.util.TranslatableMessages.TITLE_AWAKENING_NOT_DUE;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.feat.DestinoFeat;
import org.aventyrs.core.feat.Feat;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;

public class TitleAwakeningServiceImpl implements TitleAwakeningService {

    /** The slots with an EXP mark, in the order they fill. */
    private static final List<TitleSlot> PICKED_SLOTS = List.of(TitleSlot.PRIMARY, TitleSlot.SECONDARY);

    private final TitleAcquisitionService titleAcquisitionService;

    public TitleAwakeningServiceImpl() {
        this(new TitleAcquisitionServiceImpl());
    }

    public TitleAwakeningServiceImpl(final TitleAcquisitionService titleAcquisitionService) {
        this.titleAcquisitionService = titleAcquisitionService;
    }

    @Override
    public Optional<TitleSlot> dueSlot(final CharacterSheet sheet) {
        Character character = sheet.getCharacter();
        for (TitleSlot slot : PICKED_SLOTS) {
            if (titleIn(character, slot) != null) {
                continue;
            }
            return isDue(sheet, character, slot) ? Optional.of(slot) : Optional.empty();
        }
        return Optional.empty();
    }

    private boolean isDue(final CharacterSheet sheet, final Character character, final TitleSlot slot) {
        BigDecimal mark = TitleAwakeningService.awakeningExperience(slot).orElse(null);
        if (mark == null || sheet.getTotalExperience().compareTo(mark) < 0) {
            return false;
        }
        // Each Título awakens from a Centelha; one sacrificed to a Regalia forge leaves a slot unfillable.
        if (character.getAllTitles().size() >= character.getCentelhas()) {
            return false;
        }
        List<Feat> feats = character.getFeats();
        if (feats.stream().anyMatch(feat -> feat.presetsTitleAwakening(slot))) {
            return false;
        }
        return !optionsFor(sheet, slot).isEmpty();
    }

    @Override
    public List<AventyrTitle> optionsFor(final CharacterSheet sheet, final TitleSlot slot) {
        Character character = sheet.getCharacter();
        return TitleCatalog.all().stream()
                .filter(option -> character.getAllTitles().stream()
                        .noneMatch(held -> TitleCatalog.isSameFamily(held, option)))
                .filter(option -> titleAcquisitionService.isPermitted(character, sheet, option, slot))
                .toList();
    }

    @Override
    public boolean mayPostpone(final Character character) {
        return character.getFeats().stream()
                .anyMatch(feat -> feat.catalogEntry() == DestinoFeat.ATRASAR_DESPERTAR);
    }

    @Override
    public TitleSlot awaken(final CharacterSheet sheet, final AventyrTitle title) throws IllegalOperationException {
        TitleSlot slot = dueSlot(sheet).orElseThrow(() -> new IllegalOperationException(TITLE_AWAKENING_NOT_DUE));
        if (optionsFor(sheet, slot).stream().noneMatch(option -> TitleCatalog.isSameFamily(option, title))) {
            throw new IllegalOperationException(TITLE_AWAKENING_CHOICE_NOT_ELIGIBLE);
        }
        titleAcquisitionService.grantTitle(sheet.getCharacter(), sheet, title, slot);
        return slot;
    }

    private static AventyrTitle titleIn(final Character character, final TitleSlot slot) {
        return switch (slot) {
            case PRIMARY -> character.getPrimaryTitle();
            case SECONDARY -> character.getSecondaryTitle();
            case TERTIARY -> character.getTertiaryTitle();
        };
    }
}
