package org.aventyrs.core.character.services;

import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;

/**
 * Agnação Ancestral — the Orc Característica "em um ritual fora de cenas de combate podem gastar 3PM
 * para se conectar aos seus ancestrais Elementais da Terra para conseguirem conselhos, reduzindo em
 * -1 nível o GD de uma rolagem de perícia (são considerados treinados e especialistas nesta
 * rolagem, mesmo que não sejam)".
 *
 * <p><b>Three caller steps</b>, the shape Curar os Mortos' banked permission already has: {@link
 * #perform} pays the PM and banks one counsel on the sheet ({@link #COUNSEL}); the roll it is spent on
 * is built with {@code SkillRoll#counselled()}, which {@code AbstractSkillInteraction} honours only
 * while a counsel is banked; and {@link #spend} uses it up once that roll resolves — a roll only
 * reads. A banked counsel lapses at the next Cena ({@code CombatantSheet#startNewScene}).
 */
public interface AncestralCounselService {

    /** The charge {@link #perform} banks — see {@code CombatantSheet#grantCharge}. */
    Object COUNSEL = "AGNACAO_ANCESTRAL";

    /** "Podem gastar 3PM." */
    int MAGIC_POINT_COST = 3;

    /** "Reduzindo em -1 nível o GD." */
    int DIFFICULTY_REDUCTION = 1;

    /**
     * What a ritual produced: the counsel is banked either way, and {@code
     * OrquicoFeat#AGNACAO_ANCESTRAL_SUPERIOR}'s holder also receives a Subordinado Peão "até seu
     * próximo Descanso" — commanded for real since core 0.0.98; false when no Subordinado slot was free.
     */
    record AncestralCounsel(boolean pawnSubordinateGranted) {
    }

    /**
     * Performs the ritual: refuses a non-Orc (or one whose innate racial traits a Forma
     * suppresses), a Cena de Combate, or fewer than 3PM; otherwise spends them and banks a counsel.
     * A {@code null} context is no active Scene, and so not a combate.
     *
     * @throws org.aventyrs.core.sheet.IllegalOperationException {@code ANCESTRAL_COUNSEL_NOT_HELD},
     *         {@code ANCESTRAL_COUNSEL_IN_COMBAT} or {@code NOT_ENOUGH_MAGIC_POINTS}
     */
    AncestralCounsel perform(CombatantSheet orc, SceneContext sceneContext);

    /**
     * {@link #perform}, the Superior's Peão commanded with pawnBenefit (Vantagem em Perícias or the Margem Crítica) until
     * the next Descanso (core 0.0.98). With no free Subordinado slot the Peão is not granted, and the counsel still is.
     */
    AncestralCounsel perform(CombatantSheet orc, SceneContext sceneContext,
                             org.aventyrs.core.subordinate.SubordinateBenefit pawnBenefit);

    /** Uses up one banked counsel after the roll it was spent on; {@code true} if there was one. */
    boolean spend(CombatantSheet orc);
}
