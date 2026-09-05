package org.aventyrs.core.effect;

/**
 * Which Efeito Crítico an {@link CriticalEffect} <i>is</i> — the identity a rules text names when
 * it refers to one without applying it.
 *
 * <h2>Why an enum rather than the implementing class</h2>
 *
 * Everywhere else in this core an effect is identified by its own class: {@code
 * AbstractCombatantSheet#applyEffect} de-duplicates non-cumulative effects with {@code
 * existing.getClass() == effect.getClass()}, and {@code heal} clears {@code Bleeding} with an
 * {@code instanceof}. That works because those call sites always hold a real instance.
 *
 * <p>A creature's immunity list doesn't. A stat block names the Efeitos Críticos its anatomy
 * shrugs off, and it names <b>all</b> of them — including ones this core has not built. Keyed on
 * {@code Class<? extends CriticalEffect>}, an immunity to Dilacerar would be inexpressible until
 * the day a {@code Dilacerar} class exists, and the stat block would have to lie by omission in
 * the meantime. Keyed on this enum, the immunity is real, exact, authored data from the start,
 * and the day the effect is implemented it is <i>already</i> being resisted correctly — the same
 * "can't apply it yet doesn't mean can't compute it yet" discipline an unread {@code ItemBonus}
 * column follows.
 *
 * <h2>This is the complete Efeitos Críticos Ofensivos list; most have no class behind them</h2>
 *
 * All 23 constants are the authored catalog in {@code docs/rules/efeitos-criticos.txt}
 * ("Lista de Efeitos Críticos Ofensivos", L120–214), transcribed in full rather than as-needed.
 * Doing it piecemeal is what left a Magia catalog naming 14 of these able to reference only 3.
 *
 * <ul>
 *   <li><b>Implemented</b> — {@link #SANGRAMENTO} ({@link Sangramento}), {@link #PURGA_DE_MANA}
 *   ({@link ManaPurge}), {@link #PRIMOR} ({@link Primor}), {@link #SABOTAGEM} ({@link Sabotage}),
 *   {@link #EXECUCAO_REAL} ({@link RealExecution}).</li>
 *   <li><b>Named only</b> — the other 18. Nothing produces these yet; they exist so an immunity
 *   to them can be authored, so a Magia can name the one it applies, and so that whoever builds
 *   one has the constant waiting. A caller cannot currently construct an effect reporting one of
 *   these types, which is why filtering on them is a no-op today rather than an error.</li>
 * </ul>
 *
 * <p>Each constant carries its own Maior/Menor rules text verbatim, since that text is what a
 * future implementation must satisfy and it lives in a document this core does not ship.
 *
 * <h2>Two names differ from the catalog's</h2>
 *
 * {@link #PURGA_DE_MANA} is the catalog's <i>Purga-Mana</i> and {@link #SABOTAGEM} its
 * <i>Sabotar</i>. Both predate the catalog import; same effect, no ambiguity — worth knowing
 * before a rename looks like a new entry.
 *
 * <h2>Efeitos Críticos <i>Defensivos</i> are not in here</h2>
 *
 * The catalog's other nine are a separate mechanism — triggered by an <i>Acerto</i> Crítico on
 * the defender's own Defesa roll, granted by the armour or shield worn, and substituting for the
 * attacker's Falha Crítica. Nothing is ever immune to one, which is this enum's whole reason to
 * exist, so they do not belong here. See {@code docs/rules/efeitos-criticos-index.md}.
 *
 * <p>The source list is marked {@code <remake pendente>} by its own author, so treat these 23 as
 * current but unstable.
 */
public enum CriticalEffectType {

    /** Bleeding — immediate PV loss plus a per-Rodada drain. See {@link Sangramento}. */
    SANGRAMENTO,

    /** Mana burn. See {@link ManaPurge}. */
    PURGA_DE_MANA,

    /** The attacker's flourish, spending a temporary Ego point. See {@link Primor}. */
    PRIMOR,

    /** Targets the victim's equipment. See {@link Sabotage}. */
    SABOTAGEM,

    /** An outright kill. See {@link RealExecution}. */
    EXECUCAO_REAL,

    /**
     * Maior: alvo recebe a condição Amaldiçoado por 1d6 Rodadas, com Redutor de -5 em suas
     * Defesas. Menor: o mesmo, com Redutor de -2.
     */
    AMALDICOAR,

    /**
     * Maior: o alvo recebe RA para resistir a todos os ataques sofridos nas próximas 2 Rodadas.
     * Menor: o mesmo, apenas nesta Rodada.
     */
    AMENIZAR,

    /**
     * Maior: o alvo não pode realizar nenhuma ação por 1 Rodada. Menor: não pode realizar Ações
     * Livres nem Reações, e suas ações custam 1PA a mais, por 1 Rodada.
     */
    ATORDOANTE,

    /**
     * Maior: na Rodada seguinte o alvo sofre dano igual ao dano Elemental causado por este
     * ataque. Menor: metade desse dano.
     */
    CATACLISMO,

    /**
     * Maior: o alvo tem um de seus membros amputados. Menor: um membro inutilizado por 2
     * Rodadas — amputado se o alvo já perdeu metade dos PV nesta Cena.
     */
    DESMEMBRAR,

    /**
     * Maior: o alvo perde 1 ponto temporário de Força <i>e</i> Destreza. Menor: de Força
     * <i>ou</i> Destreza, definido aleatoriamente.
     */
    DILACERAR,

    /**
     * Maior: a arma fica presa no alvo; removê-la exige 1 ação livre ou 2PA do alvo e causa 3d6
     * pontos de dano irredutível. Menor: removê-la exige 1PA e causa 2d6.
     */
    EMPALAR,

    /**
     * Maior: todos os itens do alvo sofrem metade do dano sofrido por seu dono. Menor: apenas um
     * item, escolhido aleatoriamente (1 Arma, 2 Armadura, 3 Escudo, 4 Capa, 5 Elmo, 6 Núcleo
     * Tecnológico; rola de novo se o alvo não usar um item do tipo sorteado).
     */
    ESTILHACADOR,

    /**
     * Maior: o alvo perde 2PD e 1PD por Rodada até o fim da Cena ou 1 minuto, o que for maior.
     * Menor: o mesmo, por até um número de Rodadas igual ao Instinto dele. Cura interrompe a
     * perda por Rodada. Same shape as {@link #SANGRAMENTO}/{@link #PURGA_DE_MANA}, bounded by a
     * third Attribute.
     */
    EXCRUCIANTE,

    /**
     * Maior: o alvo perde 3 Multiplicadores de PV (não cumulativo) e não pode receber cura até o
     * fim da Cena. Menor: perde 2 Multiplicadores; efeitos de cura reduzidos à metade por 2
     * Rodadas (cumulativo).
     */
    FERIDA_PROFUNDA,

    /**
     * Maior: o item afetado recupera todos os PV perdidos nesta Cena e concede RD e RM ao seu
     * usuário por 2 Rodadas. Menor: apenas concede RD e RM por 2 Rodadas.
     *
     * <p>"RM" appears nowhere else in this ruleset — this core models RD and RA. Likely RA, but
     * the source does not say so; left as written.
     */
    FORTALECER,

    /**
     * Maior: Vantagem em rolagens de Ataque e Dano por 2 Rodadas, e a Margem Crítica Menor dos
     * seus ataques aumenta em +2 números (cumulativo). Menor: por 1 Rodada, +1 número.
     */
    GUILHOTINA,

    /**
     * Maior: o alvo torna-se imune a encantamentos nocivos e maldições por 5 Rodadas. Menor: por
     * 2 Rodadas. Reads the Malefício classification (Maldição/Encantamento) that this core does
     * not yet carry.
     */
    IMUNIZAR,

    /**
     * Maior: o alvo sofre 2 pontos de dano de fogo por Rodada até apagá-lo com 3PA. Menor: o
     * mesmo, apagável com 2PA.
     *
     * <p>The two tiers deal identical damage in the source; only the PA cost to extinguish
     * differs.
     */
    INFLAMAR,

    /** Maior: Roubo de Vida 2d6. Menor: Roubo de Vida 1d6. */
    OFERENDA_MALDITA,

    /**
     * Maior: a Duração da Magia aumenta em +2d6 unidades. Menor: +1d6 unidades.
     *
     * <p>"Unidades", not Rodadas — the increase is denominated in whatever unit the Magia's own
     * Duração was authored in, so a Magia storing only a converted Rodada count (1 minuto = 12
     * Rodadas) would apply this at a twelfth of its intended size. The most common Efeito
     * Crítico in the Magia catalog, at 57 of 145.
     */
    POTENCIALIZAR,

    /**
     * Maior: Bônus de +5 em suas Defesas para evitar ataques do alvo por 2 Rodadas. Menor: +2.
     * Se o alvo for um objeto ou magia, o bônus aplica-se apenas contra os efeitos e ataques
     * daquele objeto ou magia.
     */
    PREVENIR,

    /**
     * Maior: o alvo não pode Conjurar Magias nem ativar Habilidades de Título ou Monstruosas por
     * 2 Rodadas, e as que estiverem em curso ou encantando-o são interrompidas. Menor: por 1
     * Rodada, interrompendo apenas as conjuradas ou ativadas na última Rodada.
     */
    TOQUE_DO_AETHER
}
