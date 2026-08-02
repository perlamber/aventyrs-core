package org.aventyrs.core.skill;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The Habilidades de Competência available to characters trained in Artes.
 */
@Getter
@AllArgsConstructor
public enum ArtesCompetencyAbility implements SkillCompetencyAbility {

    // TODO: motivates allies, granting them (not the user) a Perícia-roll bonus for 1
    // Rodada, extending to 2 Rodadas at 5 Graduações and 3 Rodadas at 10 — but the bonus
    // itself is a lookup by which GD tier the Artes roll hit (Fácil +1, Médio +2, Muito
    // Difícil +3, Improvável +4, Milagre +5), not a flat value. Needs a
    // roll-resolution-vs-DifficultyLevel engine (to know which GD tier was reached, then
    // look up its bonus), ally-targeting, and Rodada-scoped duration tracking, none of
    // which exist yet.
    DOM_BARDICO("Você pode utilizar sua arte para motivar seus aliados, concedendo Bônus " +
            "em rolagens de Perícias a eles (mas não a você) por 1 Rodada, este benefício " +
            "aumenta 2 Rodadas ao alcançar 5 Graduações, então para 3 Rodadas com 10 " +
            "Graduações. O Bônus concedido varia conforme o GD alcançado na rolagem: GD " +
            "Fácil - Bônus de +1; GD Médio - Bônus de +2; GD Muito Difícil - Bônus de +3; " +
            "GD Improvável - Bônus de +4; GD Milagre - Bônus de +5."),

    // TODO: lets Artes substitute for the now-real Conhecimentos Perícia, with shallower
    // results — no Perícia-substitution mechanism exists yet (same gap as
    // GnoseAbility.PERITO_TEORICO).
    DOMINIO_CULTURAL("É possível utilizar rolagens de Artes ao invés de Conhecimentos, mas " +
            "informações obtidas destas formas são superficiais."),

    // TODO: choose a Perícia once (permanent choice) and gain a bonus depending on that
    // Perícia's category: Perícias de Ataque get +1 Dano Base, Esquiva e Aparar gets +1 RDS
    // (RD is now mechanically real — see DamageService.getTotalDamageReduction — so this
    // specific branch's numeric effect isn't the blocker anymore), any other Perícia gets +1
    // Margem Crítica Menor (same gap as DominioDoManaCompetencyAbility.LETALIDADE_ARCANA).
    // Persisting *which* Perícia was chosen is no longer the blocker either — see
    // org.aventyrs.core.ability.AcquiredChoice / Character#getAbilityChoices /
    // org.aventyrs.core.character.services.AbilityChoiceService#getChoiceFor. What's still
    // missing is wiring: no <Skill>Interaction/DamageService call site yet checks "does this
    // character have APRIMORAR_COM_ARTE, and does its recorded choice match the skill being
    // rolled right now" to actually apply the right one of the three branches.
    // GnoseAbility.PERITO_TEORICO is in the same spot — choice now persistable, substitution
    // mechanism itself still missing.
    APRIMORAR_COM_ARTE("Escolha uma Perícia, você criou uma forma única, exótica e ou " +
            "artística, de utilizar a Perícia escolhida, o que lhe concede um dos " +
            "benefícios a seguir: Perícias de Ataque - Dano Base +1. Esquiva e Aparar - " +
            "Redução de Danos Sofridos (RDS) +1. Outras Perícias – Margem Crítica Menor +1."),

    // TODO: halves the time to craft artistic works while Fora de Combate — needs an
    // artistic-creation-time system (same gap as ProfissaoCompetencyAbility
    // .CONSTRUTOR_EFICIENTE's production-time reduction) and a Fora-de-Combate/Cena-state
    // condition, neither of which exist yet.
    DESEMPENHO_EXTRAORDINARIO("Fora de Combate, o tempo necessário para criar suas obras " +
            "artísticas é reduzido à metade."),

    // TODO: a successful Artes roll performing for a crowd turns listeners more favorable
    // or neutral, with the roll's own GD varying by local receptivity (minimum Médio) — no
    // roll-resolution-vs-DifficultyLevel engine, NPC-disposition/reputation system, or
    // local-receptivity-to-GD mapping exists yet.
    ESPALHAR_REPUTACAO("Você pode fazer uma rolagem de Artes enquanto se apresenta para uma " +
            "multidão contando histórias suas ou de seu grupo, se for bem-sucedido os " +
            "ouvintes se tornam mais favoráveis ou neutros, a GD da rolagem pode variar " +
            "conforme a receptividade local (mínimo Médio).");

    private final String description;

    @Override
    public SkillType getSkillType() {
        return SkillType.ARTES;
    }
}
