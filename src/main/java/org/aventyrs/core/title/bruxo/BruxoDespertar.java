package org.aventyrs.core.title.bruxo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.EgoCost;
import org.aventyrs.core.title.PDCost;

import java.util.Optional;

import static org.aventyrs.core.title.PDCost.fixed;

/**
 * Bruxo's Despertar, "Contrato com Além" — "Os poderes de um Bruxo". Passive and always held
 * (through {@link Bruxo#getAllAbilities()}) but never among {@link Bruxo#getAbilities()}, so it
 * counts toward no "Habilidades de Bruxo" figure. It exists as a constant because it carries the
 * first Misticismo pick: {@code Bruxo} stores every pick under its teacher's name.
 */
@Getter
@AllArgsConstructor
public enum BruxoDespertar implements MisticismoTeacher {

    // Passive. Real through Bruxo#getGrantedMimetizedSpells: the picked tree's Magias are mimetized
    // at 0/1/2/3 PD per Semente/Broto/Muda/Emergente, each rung gated on the Habilidades de Bruxo held.
    CONTRATO_COM_ALEM(
            "Você aprende um Misticismo à sua escolha. Misticismos são Árvores de Magia aprendidas com o Título " +
            "Bruxo a partir de suas Habilidades e Supremas, cuja progressão é baseada no número de Habilidades de " +
            "Bruxo um personagem possui, ao invés de se basear na Perícia 'Conhecimentos' ou Talentos. " +
            "Misticismos usam PD para a Conjuração, ao invés de PM. Quando aprende um Misticismo o personagem é " +
            "capaz de conjurar a Magia Semente da Árvore escolhida ao custo de 0 PD. Personagens que possuam pelo " +
            "menos 2 Habilidades quaisquer de Bruxo são capazes de conjurar as magias Broto de todos seus " +
            "Misticismos, ao custo de 1PD cada. Com 5 Habilidades de Bruxo são capazes de conjurar as Magias do " +
            "tipo Muda, ao custo de 2PD cada, então com 7 Habilidades de Bruxo, ao custo de 3PD, são capazes de " +
            "conjurar Magias Emergentes. Bruxos normalmente são incapazes de conjurar Misticismos Florescentes.",
            fixed(0), EgoCost.NONE, ActionCost.NONE, Optional.empty(), Optional.of(MisticismoFilter.ANY));

    private final String description;
    private final PDCost PDCost;
    private final EgoCost egoCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
    private final Optional<MisticismoFilter> misticismoFilter;
}
