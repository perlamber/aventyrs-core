package org.aventyrs.core.feat;

import org.aventyrs.core.race.Pequenino;

/**
 * Talentos Pequeninos — one about acting twice in a Turn, two about changing what kind of
 * creature the holder is, and one about striking from hiding.
 *
 * <p>No constant carries a mechanical effect. The two Linhagem Talentos are the most interesting
 * failure of the batch: each changes the holder's <b>Tipo de Personagem</b> outright ("seu tipo
 * de Personagem muda para Feérico"), which is the same shape {@code Indomito}'s Monstros em
 * Potencial is blocked on — {@code Race#getCreatureType()} takes no {@code Character}, so a
 * creature type that varies with what its holder acquired cannot be expressed. Their EXP
 * discount is blocked twice over: {@code Race#getNewFeatCost} returns an {@code int} that cannot
 * hold 0.5, <i>and</i> it belongs to the Race, so a per-character discount has nowhere to live
 * even at a whole number.
 *
 * <p><b>Both Linhagem Talentos are gated only on the race.</b> Each one's entire Pré-requisito is
 * an exclusion of the other — "Apenas Pequeninos que não possuam o Talento 'Linhagem de
 * Lacerto'" — and {@code FeatRequirements} carries only thresholds that must be met, never one
 * that must not. So a character can legally hold both, which the text forbids; the gate is
 * looser than written, the direction every unexpressible clause in this catalog errs in.
 */
public enum PequeninoFeat implements Feat {

    /**
     * "Sempre que efetuar uma segunda rolagem de Perícia num mesmo Turno você recebe +1PA. Apenas
     * rolagens de Perícias diferentes desencadeiam este efeito."
     */
    // TODO: needs a within-Turn roll counter, and a discriminating one — it must know that a
    //  second roll happened and that it was a *different* Perícia. CharacterSheet tracks
    //  Round-scoped TemporaryEffects, not activation or roll counts, and the one thing close to
    //  it (consumeFirstRollThisTurn) is keyed by AttributeDomain and answers only "was this the
    //  first", never "which Perícias have been rolled".
    // TODO: the PA it grants is Rodada-scoped, so it is a Blessing of ModifierType.ACTION_POINTS
    //  rather than Feat#resolveActionPointsIncrease (which is for a permanent grant) — but
    //  nothing fires a Blessing off a roll being made.
    HIPERATIVIDADE(
            "Sempre que efetuar uma segunda rolagem de Perícia num mesmo Turno você recebe +1PA. "
                    + "Apenas rolagens de Perícias diferentes desencadeiam este efeito e os "
                    + "Pontos de Ação ganhos desta forma duram apenas por esta Rodada.",
            FeatRequirements.builder()
                    .requiredRace(Pequenino.class)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Seu tipo de Personagem muda para Feérico e você pode adquirir Talentos deste tipo. Para
     * você os Talentos Feéricos custam -0.5EXP."
     */
    // TODO: a per-character CreatureType — see the class javadoc. Note the second half of that
    //  clause ("você pode adquirir Talentos deste tipo") would follow for free once the type were
    //  live, since FeatRequirements#requiredRace is what gates a racial tree, not the type.
    // TODO: the -0.5EXP discount is blocked twice — a fractional cost getNewFeatCost's int cannot
    //  hold, and a per-character discount on a per-Race method. Same int-vs-fractional mismatch
    //  Pequenino's own Adaptação already cites.
    LINHAGEM_DE_FLORA(
            "Seu tipo de Personagem muda para Feérico e você pode adquirir Talentos deste tipo. "
                    + "Para você os Talentos Feéricos custam -0.5EXP. Apenas Pequeninos que não "
                    + "possuam o Talento 'Linhagem de Lacerto'.",
            FeatRequirements.builder()
                    .requiredRace(Pequenino.class)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Seu tipo de Personagem muda para Monstruoso e você pode adquirir Talentos deste tipo. Para
     * você os Talentos Monstruosos custam -0.5EXP."
     */
    // TODO: identical shape and identical blockers to LINHAGEM_DE_FLORA above, toward
    //  CreatureType.MONSTRUOSO instead.
    LINHAGEM_DE_LACERTO(
            "Seu tipo de Personagem muda para Monstruoso e você pode adquirir Talentos deste "
                    + "tipo. Para você os Talentos Monstruosos custam -0.5EXP. Apenas Pequeninos "
                    + "que não possuam o Talento 'Linhagem de Flora'.",
            FeatRequirements.builder()
                    .requiredRace(Pequenino.class)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Você recebe Vantagem em sua primeira Rolagem de Furtividade efetuada em cada Cena.
     * Enquanto nenhum outro personagem puder te ver, suas Rolagens de Perícia tem o GD reduzido
     * em -1 nível."
     */
    // TODO: the Vantagem needs a per-Cena counter (nothing tracks a Cena boundary) and a Feat
    //  roll-bonus hook, which does not exist — every flat roll bonus in this core is a @Modifier
    //  method on an ability, and Talentos are outside every ModifierResolver scan.
    // TODO: the GD half is *not* the unconditional shape Feat#resolveDifficultyReduction takes:
    //  it is gated on being unseen (no visibility/detection state exists), carved out for two
    //  named Perícias, and additionally limited to once per Rodada. Contrast
    //  GnomoFeat#FAVORITOS_DE_TESLA, which is unconditional and therefore real.
    SILENCIO_PRE_SURPRESA(
            "Você recebe Vantagem em sua primeira Rolagem de Furtividade efetuada em cada Cena. "
                    + "Enquanto nenhum outro personagem puder te ver, suas Rolagens de Perícia tem "
                    + "o GD reduzido em -1 nível, este efeito não reduz o GD de Conhecimentos e "
                    + "Profissão. Este efeito pode ser ativado apenas uma vez a cada Rodada.",
            FeatRequirements.builder()
                    .requiredRace(Pequenino.class)
                    .requiredAwakenedTitles(1)
                    .build());

    private final String description;
    private final FeatRequirements featRequirements;

    PequeninoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.PEQUENINO;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public FeatRequirements getFeatRequirements() {
        return featRequirements;
    }
}
