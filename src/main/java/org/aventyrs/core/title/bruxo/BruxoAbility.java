package org.aventyrs.core.title.bruxo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitle;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.EgoCost;
import org.aventyrs.core.title.PDCost;

import java.util.Optional;

import static org.aventyrs.core.title.PDCost.fixed;

/**
 * Bruxo's own Habilidades/Supremas — those gated on no one Especialização. Every "Requer" clause is
 * enforced data, checked by {@link AventyrTitleAbility#isEligible} through {@code
 * TitleAbilityService#grantTitleAbility}. "Habilidades de Bruxo" counts {@link
 * AventyrTitle#getAbilities()} — Habilidades and Supremas, never Especializações (table ruling,
 * 2026-10-05) — which is exactly what the default count reads.
 */
@Getter
@AllArgsConstructor
public enum BruxoAbility implements MisticismoTeacher {

    // Requer 1 Especialização de 'Bruxo'. "Custo: 1 ponto permanente de Ego, a escolha do jogador",
    // "Tempo: Ritual, 1 dia". The familiar is a permanent Subordinado plus a non-combat token
    // (Categoria -4, FamiliarTemplate, refused every attack) leashed to Bruxo#getFamiliarLeash UD (table
    // ruling, 2026-10-05). Real through Bruxo#performFamiliarRitual (the permanent Ego, binding the Familiar)
    // and Bruxo#summonFamiliar (the token and its Subordinado in a Cena) — not an Interaction, since the
    // permanent Ego rebuilds the Character.
    FAMILIAR_MAIOR(
            "Ativar esta habilidade exige um Ritual de 1 dia, envolvendo sacrifícios materiais ou pessoais, " +
            "fortalecendo o vínculo do Bruxo com a Entidade ou Divindade que ele tem por Patrono. Ao fim dos ritos " +
            "uma criatura sobrenatural, normalmente um pequeno Animal Elemental, Abissal ou Celestial, passará a " +
            "acompanhar o personagem pelo resto de sua vida, o auxiliando nas mais diversas atividades, e o " +
            "aconselhando. Um Familiar Maior possui Categoria de Tamanho -4, é considerada uma Invocação, não é " +
            "capaz de lutar e nunca se afasta mais do que 'Quantidade de Habilidades de Bruxo' UD de você. " +
            "Familiares Maiores são Subordinado e podem ter quaisquer tipos, definido na invocação (a escolha não " +
            "pode ser alterada).",
            false, fixed(0), EgoCost.NONE, ActionCost.NONE, Optional.empty(), Optional.empty(), 1, 0),

    // Requer 1 Especialização de 'Bruxo' e outras 2 Habilidades de Bruxo. "Custo/Tempo: Variável" —
    // per-cast opt-ins (+1PA, +2PD) on an invocation Magia, so the constant itself costs nothing.
    // Read off the summoner by Bruxo#resolveSummonEnhancement.
    INVOCACAO_MAIOR(
            "Você pode aumentar o Tempo de Conjuração de uma magia de invocação em +1PA, se o fizer a invocação " +
            "terá seu Multiplicador de PV aumentados em +3. Você pode aumentar o Custo de Conjuração em 2PD, se o " +
            "fizer a invocação receberá Bônus de +4 em Força ou Destreza, à sua escolha. Ambos os Efeitos são " +
            "cumulativos, Invocações Maiores beneficiadas por ambos os efeitos adicionalmente são imunes a efeitos " +
            "de Encantamentos Nocivos e Maldições. Você ou seus aliados, enquanto adjacentes à uma Invocação " +
            "Maior, recebem RM.",
            false, fixed(0), EgoCost.NONE, ActionCost.NONE, Optional.empty(), Optional.empty(), 1, 2),

    // Requer 'Invocação Maior' — #isEligible — and "requer 6 Habilidades de Bruxo". Passive. The
    // Misticismo de Invocação is real (Bruxo#getGrantedMimetizedSpells); the +2PA doubling is a
    // per-cast opt-in read by Bruxo#resolveSummonEnhancement.
    INVOCACAO_DUPLA(
            "Você aprende um novo Misticismo de Invocação, à sua escolha. Você pode aumentar o custo de " +
            "conjuração de uma magia de invocação em +2PA, se o fizer a magia invocará uma criatura adicional do " +
            "mesmo tipo e com as mesmas habilidades e bônus, mas a Duração da magia é reduzida à metade. Esta " +
            "Suprema permite invocar múltiplas criaturas mesmo quando a descrição da Magia disser que apenas uma " +
            "pode ser invocada, custos de Concentração – se houver – também são duplicados.",
            true, fixed(0), EgoCost.NONE, ActionCost.NONE, Optional.empty(),
            Optional.of(MisticismoFilter.INVOCATION), 0, 6),

    // "requer 8 Habilidades de Bruxo". Passive. Real through Bruxo#getGrantedMimetizedSpells: the
    // non-Invocação Misticismo, and Florescente at 5PD on the two Misticismos chosen by
    // Bruxo#choosePactoTrees.
    PACTO_DE_CONJURACAO(
            "Você aprende um novo Misticismo que não seja de Invocação, à sua escolha. Escolha 2 de seus " +
            "Misticismos, você é capaz de conjurar magias Florescentes dos Misticismos escolhidos ao Custo de 5PD.",
            true, fixed(0), EgoCost.NONE, ActionCost.NONE, Optional.empty(),
            Optional.of(MisticismoFilter.NON_INVOCATION), 0, 8);

    private final String description;
    private final boolean supreme;
    private final PDCost PDCost;
    private final EgoCost egoCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
    private final Optional<MisticismoFilter> misticismoFilter;
    private final int requiredSpecializations;
    private final int requiredOtherAbilities;

    /** The count prerequisite, plus the Invocação Maior that Invocação Dupla names by name. */
    @Override
    public boolean isEligible(final AventyrTitle title) {
        boolean counted = MisticismoTeacher.super.isEligible(title);
        return this == INVOCACAO_DUPLA ? counted && title.getAbilities().contains(INVOCACAO_MAIOR) : counted;
    }
}
