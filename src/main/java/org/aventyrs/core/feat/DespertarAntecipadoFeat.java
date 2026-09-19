package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.TitleSlot;
import org.aventyrs.core.sheet.IllegalOperationException;
import org.aventyrs.core.sheet.PendingAcquisition;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.TitleAcquisitionService;
import org.aventyrs.core.title.TitleAcquisitionServiceImpl;
import org.aventyrs.core.title.TitleAwakening;
import org.aventyrs.core.title.TitleCatalog;

import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.DESPERTAR_ANTECIPADO_CHOICE_NOT_ELIGIBLE;

/**
 * The acquired, per-character form of {@link DestinoFeat#DESPERTAR_ANTECIPADO} — "Seu personagem
 * desperta seu Título Primário ao fim da primeira sessão de Jogo." Grant <em>this</em> at
 * character creation in place of the bare constant.
 *
 * <p><b>The Título is picked now and awakened later.</b> The player chooses which Título will be
 * their Primário when taking the Talento; this carries it. Nothing is awakened at acquisition —
 * {@link #resolveSessionEndAcquisitions} owes it as a {@link TitleAwakening} into {@link
 * TitleSlot#PRIMARY}, and {@code CharacterSheet#applySessionEndAcquisitions} performs it when the
 * session ends.
 *
 * <p>It is owed only while the Título Primário slot is empty. Once the Título is awakened the
 * debt is settled, so a second session-end call awakens nothing. A session end that nobody
 * reported leaves the debt standing, and the next reported one settles it: this core cannot tell
 * which session was the first (see the {@code campaign} package-info).
 */
@Getter
public final class DespertarAntecipadoFeat extends AbstractFeat {

    private final AventyrTitle chosenTitle;

    public DespertarAntecipadoFeat(@NonNull final AventyrTitle chosenTitle) {
        super(DestinoFeat.DESPERTAR_ANTECIPADO.getFeatCategory(), DestinoFeat.DESPERTAR_ANTECIPADO.getDescription(),
                DestinoFeat.DESPERTAR_ANTECIPADO.getFeatRequirements());
        this.chosenTitle = chosenTitle;
    }

    /**
     * The validated factory — use this at grant time. chosenTitle must belong to a Título family
     * {@link #optionsFor(Character)} offers holder. It may already carry the Especializações and
     * Habilidades the player picked; it is awakened exactly as given.
     */
    public static DespertarAntecipadoFeat of(@NonNull final Character holder, @NonNull final AventyrTitle chosenTitle) {
        if (optionsFor(holder).stream().noneMatch(option -> TitleCatalog.isSameFamily(option, chosenTitle))) {
            throw new IllegalOperationException(DESPERTAR_ANTECIPADO_CHOICE_NOT_ELIGIBLE);
        }
        return new DespertarAntecipadoFeat(chosenTitle);
    }

    /**
     * Every Título holder may pick — each {@link TitleCatalog} family that holder's Talentos do not
     * prohibit ({@link TitleAcquisitionService#isPermitted}). Checked again at awakening, since a
     * Talento taken in between could prohibit it.
     */
    public static List<AventyrTitle> optionsFor(final Character holder) {
        TitleAcquisitionService titleAcquisitionService = new TitleAcquisitionServiceImpl();
        return TitleCatalog.all().stream()
                .filter(title -> titleAcquisitionService.isPermitted(holder, title))
                .toList();
    }

    @Override
    public Feat catalogEntry() {
        return DestinoFeat.DESPERTAR_ANTECIPADO;
    }

    /** "desperta seu Título Primário ao fim da primeira sessão" — owed until that slot is filled. */
    @Override
    public List<PendingAcquisition> resolveSessionEndAcquisitions(final Character character) {
        if (character.getPrimaryTitle() != null) {
            return List.of();
        }
        return List.of(new TitleAwakening(chosenTitle, TitleSlot.PRIMARY));
    }
}
