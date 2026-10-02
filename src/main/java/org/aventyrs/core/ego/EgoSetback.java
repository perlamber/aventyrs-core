package org.aventyrs.core.ego;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.EgoDomain;

import java.util.Arrays;
import java.util.Optional;

/**
 * The 1d6 tables an Ego rolls on when it reaches zero (2.5 Ego; "Ego chegando a ZERO: rolar 1d6 na tabela de reveses
 * do Ego"; core 0.0.82). Table rulings (2026-09-30): an Ego is at zero when its <b>permanent</b> points are, and the
 * setback holds until it has a permanent point again. Recursos has no table ("extrema pobreza" —
 * {@code SocialClass#isExtremePoverty}).
 *
 * <p>Recorded on the sheet ({@code CombatantSheet#recordEgoSetback}) by {@code EgoSetbackService#rollSetback} with a
 * face the caller threw; each constant's effect is read where it applies — see its own javadoc.
 */
@Getter
@AllArgsConstructor
public enum EgoSetback {

    // ---- Sorte a Zero ("perde as bênçãos de Tykhé") ----

    /** "Incapaz de realizar Acertos Críticos e desencadear Correntes de Efeitos" — its critical successes read as none, and its attacks fire no Corrente. */
    SUPERFICIALIDADE(EgoDomain.SORTE, 1, "Superficialidade"),
    /** "Sofre efeitos de Erros Críticos sempre que falhar numa rolagem de Perícia por diferença ≥ 5" — ⚠️ read as a Falha Crítica Menor. */
    AZARAO(EgoDomain.SORTE, 2, "Azarão"),
    /** "Ao ser bem-sucedido numa Rolagem de Perícia, rola 1d6: 1 ou 2 indica falha" — the roll's 1d6 check, the one a Cego throws, failing on 2 or less. */
    INSUCESSO(EgoDomain.SORTE, 3, "Insucesso"),
    /** "Correntes de Efeitos o afetam quando superam suas Defesas (sem a margem de 5)" — the required margin against it is 0. */
    ATRAIR_A_MORTE(EgoDomain.SORTE, 4, "Atrair a Morte"),
    /**
     * "A cada 1d6 Rodadas, no início do seu Turno, sofre os efeitos de uma Falha Crítica em Perícia" — recorded and
     * reported only: this core has no table of a Perícia's Falha Crítica effects to apply.
     */
    ESCARNIO_DE_TYKHE(EgoDomain.SORTE, 5, "Escárnio de Tykhé"),
    /** "Nada de mal acontece, por enquanto." */
    SORTE_NO_AZAR(EgoDomain.SORTE, 6, "Sorte no Azar"),

    // ---- Iniciativa a Zero ----

    /** "Perde a capacidade de agir na primeira Rodada de cada Cena" — no Pontos de Ação in Rodada 1. */
    DISTRACAO(EgoDomain.INICIATIVA, 1, "Distração"),
    /** "Redutor de -1PA em Rodadas ímpares." */
    FRAQUEZA(EgoDomain.INICIATIVA, 2, "Fraqueza"),
    /** "Não pode mais usar Ações Livres." */
    HESITACAO(EgoDomain.INICIATIVA, 3, "Hesitação"),
    /**
     * "Torna-se o último a agir em todas as Cenas, exceto quando o último ponto perdido for de uso permanente, cujas
     * rolagens serão feitas na ordem escolhida até o fim do período determinado" — last in the order, unless an
     * Iniciativa override it bought still holds.
     */
    LENTIDAO(EgoDomain.INICIATIVA, 4, "Lentidão"),
    /** "Movimento Base reduzido à metade." */
    PASSOS_REDUZIDOS(EgoDomain.INICIATIVA, 5, "Passos Reduzidos"),
    /** "Incapaz de fazer Reações." */
    REFLEXO_LENTO(EgoDomain.INICIATIVA, 6, "Reflexo Lento"),

    // ---- Autocontrole a Zero ("insano; incapaz de ações que exijam concentração") — every face also blocks concentration ----

    /** "Não diferencia inimigos de aliados e ataca alvos próximos" — compelled to attack the nearest creature. */
    ENFURECIDOS(EgoDomain.AUTOCONTROLE, 1, "Enfurecidos"),
    /** "Incapaz de ativar Habilidades de Títulos Aventyrs; qualquer Habilidade ativa é encerrada." */
    CENTELHA_MORTA(EgoDomain.AUTOCONTROLE, 2, "Centelha Morta"),
    /** "Acometido pelo medo, foge de tudo e todos" — Apavorado while it holds. */
    PANICO(EgoDomain.AUTOCONTROLE, 3, "Pânico"),
    /** "Age como criança, inocente e indefeso" — ⚠️ read as Desprevenido while it holds. */
    REGRESSAO_MENTAL(EgoDomain.AUTOCONTROLE, 4, "Regressão Mental"),
    /** "Paralisado ou aprisionado à própria imaginação" — ⚠️ read as Imobilizado while it holds. */
    TORPOR(EgoDomain.AUTOCONTROLE, 5, "Torpor"),
    /** "Todos os efeitos com Correntes de Efeito o afetam, mesmo sem superar em 5 suas Defesas" — margin 0, as Atrair a Morte. */
    VONTADE_FRACA(EgoDomain.AUTOCONTROLE, 6, "Vontade Fraca");

    private final EgoDomain domain;
    private final int face;
    private final String displayName;

    /** The Egos that roll on a table — every one but Recursos. */
    public static boolean hasTable(final EgoDomain domain) {
        return domain != EgoDomain.RECURSOS;
    }

    /** domain's entry for face (1–6), or empty for Recursos or a face off the die. */
    public static Optional<EgoSetback> of(final EgoDomain domain, final int face) {
        return Arrays.stream(values()).filter(setback -> setback.domain == domain && setback.face == face).findFirst();
    }
}
