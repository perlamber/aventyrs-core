package org.aventyrs.core.feat;

import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.FormType;
import org.aventyrs.core.sheet.TemporaryEffect;

import java.util.List;

/**
 * Turning into an animal — {@code BestialFeat#METAMORFOSE_SELVAGEM}: "Você pode gastar 2PD para se
 * transformar em um animal que pertence a uma categoria de Talento Bestial que você possua."
 *
 * <p><b>Open-ended, and that is a reading rather than a transcription.</b> The clause states no
 * Duração at all — unlike every other transformation in the catalog — and lists what it grants
 * "enquanto este Talento estiver ativo". Read as a toggle: {@link #resolveDurationInRounds()}
 * returns {@code null}, so the {@code FormEffect} never lapses and {@code
 * CombatantSheet#enterForm(null)} is the way back. Two {@code FeralFeat} Talentos that
 * <em>remove</em> the Duração from a transformation confirm a toggle is a shape this ruleset uses.
 *
 * <p><b>It grants no {@link TemporaryEffect} of its own</b>, deliberately. The Talento's three
 * bonuses (+2UD Movimento, +2 Defesas, Vantagem com Armas Naturais) are resolved by the Talento
 * itself, gated on the Forma, rather than scheduled here as countdowns — because a countdown
 * beside an open-ended shape would expire while its holder was still an animal. A form-gated
 * bonus and the form it depends on can never drift apart.
 */
final class MetamorfoseSelvagemActiveAbility implements ActiveAbility {

    /** "Gastar 2PD" — the clause's whole cost; it names no Tempo de Ação. */
    private static final int DETERMINATION_POINT_COST = 2;

    @Override
    public String getDescription() {
        return BestialFeat.METAMORFOSE_SELVAGEM.getDescription();
    }

    /** No Tempo de Ação is stated, so none is charged. */
    @Override
    public int getActionPointCost() {
        return 0;
    }

    @Override
    public int getMagicPointCost() {
        return 0;
    }

    @Override
    public int getDeterminationPointCost() {
        return DETERMINATION_POINT_COST;
    }

    /** Meaningless here — {@link #resolveDurationInRounds()} is what the service reads. */
    @Override
    public int getDurationInRounds() {
        return 0;
    }

    /** {@code null} — the transformation lasts until its holder ends it. See this class's javadoc. */
    @Override
    public Integer resolveDurationInRounds() {
        return null;
    }

    @Override
    public FormType resolveGrantedForm(final Character character) {
        return FormType.ANIMAL;
    }

    @Override
    public List<TemporaryEffect> resolveEffects(final Character character) {
        return List.of();
    }

    @Override
    public TemporaryEffect resolveEffect(final Character character) {
        return null;
    }
}
