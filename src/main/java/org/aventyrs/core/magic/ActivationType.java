package org.aventyrs.core.magic;

/** Which resource casting a Magia spends — see {@link ActivationTime}. */
public enum ActivationType {

    /** Pontos de Ação, 1 through 5. The ordinary case. */
    PONTOS_DE_ACAO,

    /** A Reação — spendable only in response to someone else's action. */
    REACAO,

    /** An Ação Livre. */
    ACAO_LIVRE
}
