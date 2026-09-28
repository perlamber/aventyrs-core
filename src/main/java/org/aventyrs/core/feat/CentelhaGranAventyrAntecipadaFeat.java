package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.sheet.CharacterSheet;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.PendingAcquisition;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.TitleAwakening;
import org.aventyrs.core.title.TitleCatalog;

import java.math.BigDecimal;
import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.DESPERTAR_ANTECIPADO_CHOICE_NOT_ELIGIBLE;

/**
 * The acquired form of {@link DestinoFeat#CENTELHA_GRAN_AVENTYR_ANTECIPADA} — "Você desperta seu
 * Título Secundário ao atingir a marca de 23EXP." Grant <em>this</em> in place of the bare constant.
 * The same pick-now, awaken-later shape as {@link DespertarAntecipadoFeat}, whose options it reuses.
 *
 * <p>Owed as a {@link TitleAwakening} into {@link TitleSlot#SECONDARY} while that slot is empty, the
 * Primário is filled, and the holder's total EXP has reached {@value #EXPERIENCE_MARK}. ⚠️ It is
 * performed at the next reported session end ({@code CharacterSheet#applySessionEndAcquisitions}),
 * the one moment this core is told acquisitions are due — not the instant the EXP lands.
 */
@Getter
public final class CentelhaGranAventyrAntecipadaFeat extends AbstractFeat {

    /** "Ao atingir a marca de 23EXP." */
    public static final int EXPERIENCE_MARK = 23;

    private final AventyrTitle chosenTitle;

    public CentelhaGranAventyrAntecipadaFeat(@NonNull final AventyrTitle chosenTitle) {
        super(DestinoFeat.CENTELHA_GRAN_AVENTYR_ANTECIPADA.getFeatCategory(),
                DestinoFeat.CENTELHA_GRAN_AVENTYR_ANTECIPADA.getDescription(),
                DestinoFeat.CENTELHA_GRAN_AVENTYR_ANTECIPADA.getFeatRequirements());
        this.chosenTitle = chosenTitle;
    }

    /** The validated factory: chosenTitle must be one {@link #optionsFor} offers holder. */
    public static CentelhaGranAventyrAntecipadaFeat of(@NonNull final Character holder, @NonNull final AventyrTitle chosenTitle) {
        if (optionsFor(holder).stream().noneMatch(option -> TitleCatalog.isSameFamily(option, chosenTitle))) {
            throw new IllegalOperationException(DESPERTAR_ANTECIPADO_CHOICE_NOT_ELIGIBLE);
        }
        return new CentelhaGranAventyrAntecipadaFeat(chosenTitle);
    }

    /** Every Título holder may pick, less the families they already hold. */
    public static List<AventyrTitle> optionsFor(final Character holder) {
        return DespertarAntecipadoFeat.optionsFor(holder).stream()
                .filter(option -> holder.getAllTitles().stream().noneMatch(held -> TitleCatalog.isSameFamily(held, option)))
                .toList();
    }

    @Override
    public Feat catalogEntry() {
        return DestinoFeat.CENTELHA_GRAN_AVENTYR_ANTECIPADA;
    }

    @Override
    public List<PendingAcquisition> resolveSessionEndAcquisitions(final Character character, final CharacterSheet sheet) {
        if (sheet == null || character.getPrimaryTitle() == null || character.getSecondaryTitle() != null
                || sheet.getTotalExperience().compareTo(BigDecimal.valueOf(EXPERIENCE_MARK)) < 0) {
            return List.of();
        }
        return List.of(new TitleAwakening(chosenTitle, TitleSlot.SECONDARY));
    }
}
