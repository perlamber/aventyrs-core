package org.aventyrs.core.feat;

import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.IllegalOperationException;

import java.util.List;

import static org.aventyrs.core.util.TranslatableMessages.EXCEPCIONALIDADE_CHOICE_NOT_ELIGIBLE;
import static org.aventyrs.core.util.TranslatableMessages.FEAT_REQUIRES_CHOICE;

/**
 * The acquired, per-character form of {@link DestinoFeat#EXCEPCIONALIDADE} — "Escolha um Talento
 * Racial que você cumpra todos os demais requisitos, além da Raça. Você recebe os benefícios do
 * Talento escolhido." Grant <em>this</em> in {@code Character#feats} in place of the bare
 * constant.
 *
 * <p><b>The chosen Talento is granted whole, not forwarded.</b> {@link #getGrantedFeats} folds it
 * into {@code Character#getFeats()}, so it is held in every sense: all of its effect hooks apply,
 * and it counts for other Talentos' prerequisites. A character who takes Aviano's {@code
 * CORACAO_ALADO} this way can therefore go on to buy {@code ETERNO_VIAJANTE}, whose only gates are
 * that Talento and a Título. Forwarding hook by hand, as {@code HerancaBestialFeat} does for six
 * constants, would not scale to a choice spanning every racial tree.
 *
 * <p>Only Excepcionalidade's own XP is paid; the granted Talento costs nothing.
 */
@Getter
public final class ExcepcionalidadeFeat extends AbstractFeat {

    private final Feat chosenFeat;

    public ExcepcionalidadeFeat(@NonNull final Feat chosenFeat) {
        super(DestinoFeat.EXCEPCIONALIDADE.getFeatCategory(), DestinoFeat.EXCEPCIONALIDADE.getDescription(),
                DestinoFeat.EXCEPCIONALIDADE.getFeatRequirements());
        this.chosenFeat = chosenFeat;
    }

    /**
     * The validated factory — use this at grant time. chosenFeat must be one {@link
     * #optionsFor(Character)} offers holder, compared through {@code catalogEntry()} so a Talento
     * Racial that carries a choice of its own ({@code ConselheiroDeGuerraYmirianoFeat}) is passed
     * in its acquired form. Its bare constant is refused, exactly as {@code FeatService#grantFeat}
     * would refuse it.
     */
    public static ExcepcionalidadeFeat of(@NonNull final Character holder, @NonNull final Feat chosenFeat) {
        if (!optionsFor(holder).contains(chosenFeat.catalogEntry())) {
            throw new IllegalOperationException(EXCEPCIONALIDADE_CHOICE_NOT_ELIGIBLE);
        }
        if (chosenFeat == chosenFeat.catalogEntry() && !chosenFeat.resolveRequiredChoices(holder).isEmpty()) {
            throw new IllegalOperationException(FEAT_REQUIRES_CHOICE);
        }
        return new ExcepcionalidadeFeat(chosenFeat);
    }

    /**
     * Every Talento Racial holder may pick: authored in a {@link FeatCategory.Type#RACIAL} tree,
     * not already held, and eligible once its Raça clauses are dropped ({@link
     * Feat#isEligibleRegardlessOfRace}).
     *
     * <p>"Talento Racial" is read as the tree, so a general-tree Talento that merely names a race
     * ({@code Anão/Sobrevivência}-style) is not offered. The {@code CharacterSheet}-side
     * prerequisites (Fama, EXP total) are skipped rather than failed, as in every sheet-less
     * eligibility preview; no authored Talento Racial names either.
     */
    public static List<Feat> optionsFor(final Character holder) {
        return FeatCatalog.all().stream()
                .filter(feat -> feat.getFeatCategory().getType() == FeatCategory.Type.RACIAL)
                .filter(feat -> holder.getFeats().stream().noneMatch(held -> held.catalogEntry() == feat))
                .filter(feat -> feat.isEligibleRegardlessOfRace(holder, null))
                .toList();
    }

    @Override
    public Feat catalogEntry() {
        return DestinoFeat.EXCEPCIONALIDADE;
    }

    /** "Você recebe os benefícios do Talento escolhido" — granted whole; see the class javadoc. */
    @Override
    public List<Feat> getGrantedFeats(final Character character) {
        return List.of(chosenFeat);
    }
}
