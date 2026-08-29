package org.aventyrs.core.magic.catalog;

import org.aventyrs.core.effect.CriticalEffectType;
import org.aventyrs.core.magic.ActivationTime;
import org.aventyrs.core.magic.AuthoredSpell;
import org.aventyrs.core.magic.BranchLevel;
import org.aventyrs.core.magic.SpellData;
import org.aventyrs.core.magic.SpellDuration;
import org.aventyrs.core.magic.SpellTargeting;
import org.aventyrs.core.magic.SpellTree;
import org.aventyrs.core.scene.AreaOfEffect;
import org.aventyrs.core.scene.Range;
import org.aventyrs.core.skill.DifficultyLevel;
import org.aventyrs.core.skill.SkillType;

/**
 * PROFANAR (Profana) — eight Magias, diverging at Broto into a life-drain line and an
 * area-corruption line, converging at Florescente.
 *
 * <p><b>Roubo de Vida is the one mechanic this tree leans on that is genuinely real</b>: {@code
 * LifeStealService} exists, and five of these eight grant it. Its siblings do not — Roubo de Mana
 * and Roubo de Determinação are both named here (Toque do Ceifeiro's Corrente grants all three at
 * once) and neither has a service behind it.
 *
 * <p>Three Magias take their Duração <i>and</i> their Alcance from Solo Profano. The Duração is a
 * live {@code SpellDuration.sameAs} reference; the Alcance is not, because an enum constant may
 * not read a sibling constant from its own arguments and {@code SpellTargeting} has no deferred
 * form — those two restate the footprint literally, so keep them in step by hand if Solo Profano's
 * ever changes.
 */
public enum ProfanarSpell implements AuthoredSpell {

    /**
     * The third and last entry whose GD is bare "{@code DM do Alvo}" with no tier.
     *
     * <p>Its Efeito Alternativo is the catalog's cleanest use of {@code CharacterStatus}' negative
     * range: <i>Arrancar a Alma</i> applies only to a living character at "0 ou menos PV", which is
     * exactly the unclamped subtraction {@code HitPointsService#getStatus} preserves and {@code
     * getCurrentHitPoints} floors away. TODO nothing acts on it — there is no instant-death path,
     * and "personagens vivos" is the living/undead classification this core lacks.
     */
    LACERAR_A_ALMA(SpellData.builder()
            .name("Lacerar a Alma")
            .branchLevel(BranchLevel.SEMENTE)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Um ataque que rouba a vitalidade do alvo.")
            .primaryEffectDescription("Como parte da conjuração desta magia você deve tocar um inimigo, seu toque "
                    + "recebe Roubo de Vida 2. O Alvo deste ataque sofre uma quantidade de pontos de dano Profano "
                    + "igual à metade de seu Foco. "
                    + "Apenas personagens vivos e que possuam 1 ou mais PV podem ser alvo deste efeito.")
            .secondaryEffectDescription("Arrancar a Alma: O personagem tocado por esta magia morre instantaneamente "
                    + "e você recupera 3PV. "
                    + "Apenas personagens vivos e que possuam 0 ou menos PV podem ser alvo deste efeito.")
            .criticalEffectType(CriticalEffectType.AMALDICOAR)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /** "é dissipada caso o alvo se distancie do conjurador além de seu alcance" is a range-maintained effect; nothing re-checks a distance after a cast. */
    DRENAR_VIDA(SpellData.builder()
            .name("Drenar Vida")
            .branchLevel(BranchLevel.BROTO)
            .branch(MagicBranch.PROFANAR_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Um ataque feito com uma corrente mágica que conecta o conjurador e seu alvo, transferindo "
                    + "a vitalidade do alvo para si.")
            .primaryEffectDescription("Você cria uma corrente com grilhões mágicos, um deles conectados a você e "
                    + "outro ao seu alvo, se for bem-sucedido. O alvo desta magia sofre 1d6+Metade do Foco Pontos de "
                    + "Dano Profano, +2 Pontos de Dano em cada Rodada seguinte. "
                    + "Esta magia possui Roubo de Vida 2 e é dissipada caso o alvo se distancie do conjurador além "
                    + "de seu alcance.")
            .effectChainDescription("Enraizar: O alvo não pode se mover na primeira Rodada de efeito desta magia, "
                    + "mas ainda pode fazer outras ações normalmente.")
            .criticalEffectType(CriticalEffectType.OFERENDA_MALDITA)
            .duration(SpellDuration.concentracaoMais(1))
            .targeting(SpellTargeting.distancia(Range.DISTANCIA_MEDIA))
            .build()),

    /**
     * One of the four Magias whose Concentração carries an extra clause of its own: "Enquanto
     * estiver se concentrando para manter o Solo Profano você não pode se mover" — a restriction
     * on the caster <em>while</em> focused, which is phase one of a Concentração duration and the
     * phase that has no length to count down.
     *
     * <p>Its {@code Alcance:} also states that the area stays where it was cast even as the caster
     * moves, which is why it is centred on the caster only at the moment of casting.
     */
    SOLO_PROFANO(SpellData.builder()
            .name("Solo Profano")
            .branchLevel(BranchLevel.BROTO)
            .branch(MagicBranch.PROFANAR_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.MEDIUM)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Uma magia capaz de corromper uma área e afetar os vivos que andarem sobre ela.")
            .primaryEffectDescription("Mortos-Vivos e Abissais nesta área recuperam 2PV a cada Rodada e recebem "
                    + "Vantagem em suas rolagens de Perícias de Ataque e Dano. "
                    + "Personagens vivos dentro da área sofrem 1+Metade do Foco pontos de Dano a cada Rodada. "
                    + "Enquanto estiver se concentrando para manter o Solo Profano você não pode se mover. "
                    + "Após a conjuração o solo profano permanece ativo no mesmo local, mesmo que você se mova.")
            .effectChainDescription("Terreno Sanguessugas: Esta magia recebe Roubo de Vida 1.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.concentracaoMais(2))
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.circle(Range.DISTANCIA_MEDIA)))
            .build()),

    /** Its Efeito spells Roubo de Vida as "Roubou de Vida"; transcribed as written. */
    TOQUE_DO_CEIFEIRO(SpellData.builder()
            .name("Toque do Ceifeiro")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.PROFANAR_PRINCIPAL)
            .activationTime(ActivationTime.pa(3))
            .attackSkillType(SkillType.ATAQUE_A_DISTANCIA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Invoca uma criatura feita de sombras e energia mortis, cujo toque absorve as forças vitais "
                    + "do alvo e as transfere para o conjurador.")
            .primaryEffectDescription("O conjurador invoca um Comensal da Morte, uma das entidades que recolhe as "
                    + "forças dos vivos e as entregam para a própria Escuridão. "
                    + "A gigantesca criatura ataca o alvo com sua foice, capaz de afetar criaturas distantes, "
                    + "causando danos ao corpo, a mente e essência do alvo. "
                    + "Essa magia confere o Malefício Amaldiçoado ao alvo por 2 Rodadas, ele também perde 1d6PD, "
                    + "1d6PM e 1d6+Metade do Foco PV. "
                    + "Esta magia recebe Roubou de Vida 2, apenas personagens vivos podem ser alvo deste ataque.")
            .effectChainDescription("Estigma Profano: Se o alvo for um personagem previamente Amaldiçoado ele perde "
                    + "adicionalmente +1d6PV, +1d6PM e +1d6PD, esta magia recebe Roubo de Determinação 1, Roubo de "
                    + "Mana 1, Roubo de Vida 3 (em substituição ao Roubo de Vida 2).")
            .criticalEffectType(CriticalEffectType.OFERENDA_MALDITA)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.distancia(Range.DISTANCIA_CURTA))
            .build()),

    /**
     * Takes both its Duração and its Alcance from Solo Profano. The Duração is the live reference;
     * the footprint is restated — see the class javadoc.
     *
     * <p>Its {@code Efeito Crítico:} line is annotated "(aplicado a todos os Personagens na área)",
     * scoping who the Amaldiçoar lands on. The type is authored; the scoping has nowhere to live,
     * and this core does not resolve a Magia's target set anyway.
     */
    FACE_DO_ABISMO(SpellData.builder()
            .name("Face do Abismo")
            .branchLevel(BranchLevel.MUDA)
            .branch(MagicBranch.PROFANAR_ALTERNATIVO)
            .activationTime(ActivationTime.pa(1))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.HARD)
            .description("Aprimora um Solo Profano, fortalecendo seus efeitos.")
            .primaryEffectDescription("Seres vivos dentro da Área de Efeito do ‘Solo Profano’, tem seus efeitos de "
                    + "cura e recuperação de vida reduzidos à metade, exceto por efeitos de Roubo de Vida. "
                    + "Rolagens de Perícias de Ataque e Dano efetuados por todos os personagens dentro do Solo "
                    + "Profano recebem Bônus de Vantagem. "
                    + "Os seus ataques, físicos e mágicos, e os efetuados por Mortos-Vivos e Abissais dentro desta "
                    + "área recebem Roubo de Vida 1.")
            .effectChainDescription("Traumatizar: Outros personagens, exceto Mortos-Vivos e Abissais, sofrem 1d6 "
                    + "Pontos de Dano Profano e não podem ativar efeitos de Ego dentro do Solo Profano.")
            .criticalEffectType(CriticalEffectType.AMALDICOAR)
            .duration(SpellDuration.sameAs(() -> SOLO_PROFANO.getDuration()))
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.circle(Range.DISTANCIA_MEDIA)))
            .build()),

    /** TODO its recovery is keyed to each felled target's Vigor, Instinto and Foco — an amount read off the <em>victim's</em> Attributes, which no hook in this core is shaped to ask for. */
    SACRIFICO_PROFANO(SpellData.builder()
            .name("Sacrifico Profano")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.PROFANAR_PRINCIPAL)
            .activationTime(ActivationTime.pa(4))
            .attackSkillType(SkillType.ATAQUE_CORPO_A_CORPO)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .castingDifficultyFlooredByTargetMagicDefense(true)
            .description("Sacrifica um Aliado ou Subordinado para causar grande dano e recuperar as próprias "
                    + "forças.")
            .primaryEffectDescription("O conjurador toca simultaneamente um aliado e um inimigo, e os preenche com "
                    + "uma grande quantidade de energia Profana. O aliado e o inimigo tocado sofrem 3d6+Metade do "
                    + "Foco pontos de dano. "
                    + "Você recupera uma quantidade de PV igual ao Vigor de cada personagem que tenha seus PV "
                    + "reduzidos à zero ou menos com essa magia. Você recupera o dobro de PV de alvos amaldiçoados.")
            .effectChainDescription("Funeral Herege: Você recupera uma quantidade de PD igual ao Instinto de cada "
                    + "personagem que tenha seus PV reduzidos à zero ou menos com essa magia, e de PM igual ao Foco "
                    + "deles. "
                    + "Você recupera o dobro de PD, PM e PV de alvos amaldiçoados.")
            .criticalEffectType(CriticalEffectType.OFERENDA_MALDITA)
            .duration(SpellDuration.INSTANTANEA)
            .targeting(SpellTargeting.TOQUE)
            .build()),

    /** Takes both its Duração and its Alcance from Solo Profano, as Face do Abismo does. */
    CORACAO_DO_ABISMO(SpellData.builder()
            .name("Coração do Abismo")
            .branchLevel(BranchLevel.EMERGENTE)
            .branch(MagicBranch.PROFANAR_ALTERNATIVO)
            .activationTime(ActivationTime.pa(2))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.VERY_HARD)
            .description("Aprimora a Face do Abismo, tornando-a ainda mais letal.")
            .primaryEffectDescription("Cada personagem dentro do Solo Profano tem seu Multiplicador de PV reduzidos "
                    + "em 1 a cada Rodada. "
                    + "Sempre que você for bem-sucedido em reduzir os PV de outro personagem à zero ou menos em "
                    + "decorrência de seus ataques e magias (incluindo o Solo Profano) você recupera 1 de seus "
                    + "próprios Multiplicadores de PV.")
            .effectChainDescription("Favor da Escuridão: Sempre que um personagem tiver seus PV reduzidos à zero ou "
                    + "menos você recupera 2 Multiplicadores de PV (ao invés de apenas 1), este personagem retornará "
                    + "para lhe auxiliar no combate enquanto o Solo Profano permanecer ativo. Considere estes "
                    + "personagens como Subordinado de um tipo à sua escolha. "
                    + "Estes personagens são considerados Personagens Possuídos, efeitos que encerram possessões os "
                    + "liberta, encerrando o efeito sobre eles.")
            .criticalEffectType(CriticalEffectType.AMALDICOAR)
            .duration(SpellDuration.sameAs(() -> SOLO_PROFANO.getDuration()))
            .targeting(SpellTargeting.areaDeEfeito(AreaOfEffect.circle(Range.DISTANCIA_MEDIA)))
            .build()),

    /**
     * The convergence rung, and <b>the only Magia in the catalog with two duration fields</b>: its
     * own {@code Duração: 3 Rodadas} plus a separate {@code Duração do Portal: 1 minuto}. {@code
     * SpellData} holds one, so the Magia's own is authored and the portal's is transcribed into the
     * prose — a second column would exist for exactly one consumer.
     *
     * <p>TODO the portal leads to the Plano Primordial da Escuridão. No planar concept exists.
     */
    PORTAL_PARA_A_ESCURIDAO(SpellData.builder()
            .name("Portal para a Escuridão")
            .branchLevel(BranchLevel.FLORESCENTE)
            .activationTime(ActivationTime.pa(5))
            .attackSkillType(SkillType.DOMINIO_DO_MANA)
            .castingDifficultyLevel(DifficultyLevel.UNLIKELY)
            .description("Sacrifica uma criatura para abrir um portal para o Abismo, o Plano Primordial da "
                    + "Escuridão.")
            .primaryEffectDescription("Um breve ritual que envolve o sacrifício de um alvo. "
                    + "Você cria com sua magia uma pequena lâmina feita de sombras, esta quando usada para atacar "
                    + "causa 3d6+Metade do Foco pontos de dano e Efeito Crítico Oferenda Maldita. Alvos voluntários "
                    + "não sofrem danos, mas são eliminados imediatamente. "
                    + "Ataques rolados com esta lâmina são feitos contra DM dos alvos, caso um personagem tenha seus "
                    + "PV reduzidos à zero ou menos ele será destruído e não poderá ser ressuscitado, seus restos "
                    + "mortais se levantarão formando um portal de passagem disforme. Qualquer personagem que passe "
                    + "por baixo dele é levado ao Plano Primordial da Escuridão. "
                    + "Duração do Portal: 1 minuto.")
            .secondaryEffectDescription("Regresso ao Equilíbrio: Uma vez no Abismo o conjurador pode abrir um portal "
                    + "para retornar ao Plano Material invocando uma nova lâmina e se flagelando com ela, "
                    + "sacrificando metade dos seus PV atuais.")
            .criticalEffectType(CriticalEffectType.POTENCIALIZAR)
            .duration(SpellDuration.rodadas(3))
            .targeting(SpellTargeting.PESSOAL)
            .build());

    private final SpellData data;

    ProfanarSpell(final SpellData data) {
        this.data = data;
    }

    @Override
    public SpellData getData() {
        return data;
    }

    @Override
    public SpellTree getTree() {
        return MagicTree.PROFANAR;
    }
}
