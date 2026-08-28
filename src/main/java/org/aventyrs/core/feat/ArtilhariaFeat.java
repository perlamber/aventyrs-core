package org.aventyrs.core.feat;

import org.aventyrs.core.skill.SkillType;

/**
 * Talentos de Artilharia — ranged attacks, aiming, and firing more than once.
 *
 * <p>Two limits of {@link FeatRequirements} show up repeatedly here and are noted per constant:
 * {@code requiredFeat} is <b>singular</b>, so a Talento demanding two named Talentos can only
 * record one; and a Pré-requisito naming a Talento from another tree is wired only once that
 * tree exists.
 */
public enum ArtilhariaFeat implements Feat {

    /**
     * "Você pode aumentar seu tempo de disparo em +1PA quando atacar utilizando a perícia 'ataque
     * à distância', se o fizer poderá rolar novamente o dado de menor valor em sua rolagem."
     */
    // TODO: rerolling one die of a SkillRoll has no representation — this core never rolls dice,
    //  and SkillRoll arrives already resolved, so a reroll must be the caller's own step.
    // TODO: "Treinamento em Ataque-à-distância", with no number, is read as Graduação 1.
    MIRA_IMPECAVEL(
            "Você pode aumentar seu tempo de disparo em +1PA quando atacar utilizando a perícia "
                    + "‘ataque à distância’, se o fizer poderá rolar novamente o dado de menor "
                    + "valor em sua rolagem. O novo resultado será utilizado, mesmo que seja "
                    + "inferior ao anterior.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_A_DISTANCIA)
                    .requiredSkillGraduation(1)
                    .build()),

    /**
     * "Escolha um tipo de arma de Ataque a Distância ou de Arremesso, você recebe Vantagem nas
     * rolagens de ataque sempre que atacar inimigos à Distâncias Médias ou superiores."
     *
     * <p>The Vantagem itself is an ordinary {@code Skill#ADVANTAGE_BONUS}, and the range condition
     * is expressible ({@code Range#DISTANCIA_MEDIA} and beyond, via {@code SceneContext}).
     */
    // TODO: no Feat hook receives a SceneContext or an AttackSource, so neither the range
    //  condition nor the chosen weapon type can be tested — a flat grant would apply at every
    //  range, with every weapon.
    // TODO: carries an acquisition-time choice (a tipo de arma) — CLAUDE.md's AcquiredChoice
    //  pattern, still without a consuming mechanism.
    ATIRADOR_PERFEITO(
            "Escolha um tipo de arma de Ataque a Distância ou de Arremesso, você recebe Vantagem "
                    + "nas rolagens de ataque sempre que atacar inimigos à Distâncias Médias ou "
                    + "superiores enquanto utilizando armas do tipo escolhido.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_A_DISTANCIA)
                    .requiredSkillGraduation(2)
                    .build()),

    /** "Sempre que utilizar o talento 'Atirador Perfeito' você recebe também vantagem nas rolagens de dano." */
    // TODO: a Vantagem on a dano roll has no hook, and "sempre que utilizar outro Talento" is the
    //  gap catalog's "This one delivered attack" scoping — no per-roll hook is scoped that way.
    ABATER_A_CACA(
            "Sempre que utilizar o talento ‘Atirador Perfeito’ você recebe também vantagem nas "
                    + "rolagens de dano.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_A_DISTANCIA)
                    .requiredSkillGraduation(3)
                    .requiredFeat(ATIRADOR_PERFEITO)
                    .build()),

    /** "Você recebe Vantagem em suas rolagens de Dano sempre que utilizar o Talento 'Atirador Perfeito'." */
    // TODO: identical blockers to ABATER_A_CACA. Note the two Talentos state the same effect in
    //  the same words; the source authors both, so both are catalogued rather than merged.
    UM_TIRO_UMA_MORTE(
            "Você recebe Vantagem em suas rolagens de Dano sempre que utilizar o Talento "
                    + "‘Atirador Perfeito’.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_A_DISTANCIA)
                    .requiredSkillGraduation(4)
                    .requiredFeat(ATIRADOR_PERFEITO)
                    .build()),

    /** "A distância máxima de seus ataques à Distância, físicos e Mágicos, aumentam em +1 nível." */
    // TODO: nothing reads a character's maximum attack range — Range bands describe a resolved
    //  distance handed in by a caller, and no stat expresses how far this character can reach.
    TIRO_LONGO(
            "A distância máxima de seus ataques à Distância, físicos e Mágicos, aumentam em +1 "
                    + "nível. Sempre que efetuar ataques utilizando dos benefícios do Talento "
                    + "‘Mira Impecável’, a distância dos seus ataques aumenta em +1 passo (para o "
                    + "total de +2 níveis).",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_A_DISTANCIA)
                    .requiredSkillGraduation(3)
                    .build()),

    /**
     * "Após fazer um ataque com uma arma à distância você pode fazer um ataque adicional com a
     * mesma arma em desvantagem ao custo de 1PA."
     */
    // TODO: granting an extra attack is not expressible — an attack is initiated by a caller,
    //  never by a resolution.
    // TODO: "apenas uma vez por Rodada" needs a per-Rodada activation counter.
    TIRO_RAPIDO(
            "Após fazer um ataque com uma arma à distância você pode fazer um ataque adicional com "
                    + "a mesma arma em desvantagem ao custo de 1PA. Este talento pode ser "
                    + "utilizado apenas uma vez por Rodada.",
            FeatRequirements.builder()
                    .requiredFeat(MIRA_IMPECAVEL)
                    .build()),

    /** "Sempre que tiver um Acerto Crítico usando o talento 'Mira Impecável' você causa +1d6 de dano adicional." */
    // TODO: scoped to "this one delivered attack" made with another Talento — see ABATER_A_CACA.
    // TODO: its Pré-requisito names *two* Talentos (Mira Impecável and Acerto Crítico
    //  Aprimorado); requiredFeat is singular, so only the intra-tree one is recorded and
    //  AssassinoFeat#ACERTO_CRITICO_APRIMORADO goes unenforced.
    MIRA_MORTAL(
            "Sempre que tiver um Acerto Crítico usando o talento ‘Mira Impecável’ você causa +1d6 "
                    + "de dano adicional.",
            FeatRequirements.builder()
                    .requiredFeat(MIRA_IMPECAVEL)
                    .build()),

    /**
     * "Uma vez por Rodada, em seu Turno, você pode disparar um projétil adicional em seus ataques,
     * se o fizer os danos causados pelo ataque aumentam em +1d6."
     */
    // TODO: granting an extra projectile, and applying Correntes de Efeito twice, both need
    //  multi-hit attack resolution — AttackDelivery resolves exactly one.
    TIRO_DUPLO(
            "Uma vez por Rodada, em seu Turno, você pode disparar um projétil adicional em seus "
                    + "ataques, se o fizer os danos causados pelo ataque aumentam em +1d6 "
                    + "independentemente do tipo de projétil utilizado, você recebe Desvantagem "
                    + "nesta rolagem de Ataque à Distância. Correntes de Efeito e Efeitos Críticos "
                    + "aplicam seus efeitos duas vezes.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_A_DISTANCIA)
                    .requiredSkillGraduation(5)
                    .requiredFeatCategory(FeatCategory.ARTILHARIA)
                    .requiredFeatCategoryCount(2)
                    .build()),

    /**
     * "Você pode estender os benefícios de Combater as Cegas aos seus Ataques-a-Distância
     * efetuados contra alvos em até Distância Média."
     */
    // TODO: extends another Talento's benefit, which itself is not yet authored — its
    //  Pré-requisito (the Talento 'Combater as Cegas') is left unset until that tree lands, so
    //  this is currently open to characters who lack it.
    DISPARO_AS_CEGAS(
            "Você pode estender os benefícios de Combater as Cegas aos seus Ataques-a-Distância "
                    + "efetuados contra alvos em até Distância Média.",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_A_DISTANCIA)
                    .requiredSkillGraduation(4)
                    .build()),

    /**
     * "Como Tiro Duplo, mas você pode disparar uma quantidade de projéteis adicionais igual a sua
     * quantidade de Títulos Aventyr Despertos (mínimo 2 projéteis, máximo 4 projéteis)."
     */
    // TODO: same multi-hit blocker as TIRO_DUPLO. The projectile count itself is computable —
    //  clamp(getAllTitles().size(), 2, 4) — but there is nothing to spend it on.
    TIRO_MULTIPLO(
            "Como Tiro Duplo, mas você pode disparar uma quantidade de projéteis adicionais igual "
                    + "a sua quantidade de Títulos Aventyr Despertos (mínimo 2 projéteis, máximo 4 "
                    + "projéteis).",
            FeatRequirements.builder()
                    .requiredSkillType(SkillType.ATAQUE_A_DISTANCIA)
                    .requiredSkillGraduation(7)
                    .requiredFeat(TIRO_DUPLO)
                    .requiredAwakenedTitles(1)
                    .build());

    private final String description;
    private final FeatRequirements featRequirements;

    ArtilhariaFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.ARTILHARIA;
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
