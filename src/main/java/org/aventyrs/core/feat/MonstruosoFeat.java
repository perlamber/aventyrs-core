package org.aventyrs.core.feat;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.DefenseType;
import org.aventyrs.core.race.CreatureType;
import org.aventyrs.core.title.TitleArchetype;

/**
 * Talentos Monstruosos — the open tree any Monstruoso race can draw on, from unusual anatomy to
 * extra heads to acid blood.
 *
 * <p>Two constants carry real effects, and they are the batch's most interesting pair: {@link
 * #PELE_RIJA} grants DF and RD together, and {@link #OSSOS_OCOS} is the catalog's <b>first
 * Talento to apply a real malus</b> — a −1 Multiplicador de PV paid for by a +1UD Movimento Base.
 *
 * <p>Gated through {@code FeatRequirements#requiredCreatureType}, since "Raça Monstruosa" spans
 * Aviano, Goblin, Ogro, Guampo, Indômito, Troll and Bestial with no common supertype.
 */
public enum MonstruosoFeat implements Feat {

    /**
     * "Receba +1 de bônus Racial em um dos atributos cedidos por sua Raça ou no atributo 'Força'…
     * Adicionalmente você recebe vantagem em rolagens de Persuasão e Atenção: Discernir Motivação
     * efetuadas contra outros indivíduos de sua raça que não possuam este Talento."
     */
    // TODO: a Talento cannot grant an Atributo bonus — see BestialFeat's class javadoc — and this
    //  one is additionally a choice between a dynamically-determined set (whichever Atributos the
    //  holder's Race grants) plus Força.
    // TODO: the Vantagem is scoped to the *target* — same race, and lacking this same Talento —
    //  and resolveSkillRollBonus carries no opponent. Nothing anywhere lets a roll bonus inspect
    //  who is being rolled against except the attack-specific resolveAttackRollBonus.
    // TODO: its Pré-requisito is "qualquer atributo que receba bônus Racial com valor Base igual
    //  à 5", which names no particular AttributeDomain — one of the three constants
    //  docs/rules/talentos-index.md lists under "Any Attribute at N". Only the race clause is
    //  enforced, so the gate is looser than written.
    ALFA(
            "Receba +1 de bônus Racial em um dos atributos cedidos por sua Raça ou no atributo "
                    + "'Força', a sua escolha. Adicionalmente você recebe vantagem em rolagens de "
                    + "Persuasão e Atenção: Discernir Motivação efetuadas contra outros indivíduos "
                    + "de sua raça que não possuam este Talento.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.MONSTRUOSO)
                    .build()),

    /**
     * "Você recebe Resistência a Críticos. Você ignora o primeiro Efeito Crítico Menor que sofrer
     * em cada Cena de Combate."
     */
    // TODO: Resistência a Críticos is not a stat this core computes.
    // TODO: ignoring an Efeito Crítico is close to expressible but not quite — CriticalEffect
    //  #applicableTo already filters a victim's immunities, but it keys on CriticalEffectType
    //  (which effect) and this clause keys on *severity* (Menor vs Maior), which the filter does
    //  not carry. It also needs a per-Cena counter, which nothing tracks.
    // TODO: "apenas personagens não-humanos" is a negated race clause, and FeatRequirements
    //  carries only thresholds that must be met — so this is left ungated on race entirely.
    ANATOMIA_INCOMUM(
            "Você recebe Resistência a Críticos. Você ignora o primeiro Efeito Crítico Menor que "
                    + "sofrer em cada Cena de Combate. Você ignora um Efeito Crítico Menor "
                    + "adicional para cada Título Aventyr Desperto.",
            FeatRequirements.builder().build()),

    /** "Você é imune a Efeitos Críticos Menores. Sua resistência às Correntes de Efeitos aumenta em +2." */
    // TODO: severity-keyed immunity — see ANATOMIA_INCOMUM. Note this is the one clause in the
    //  catalog that would be *fully* served by widening CriticalEffect#applicableTo to consider
    //  severity, since unlike its prerequisite it needs no per-Cena counter.
    // TODO: "resistência às Correntes de Efeitos" is a stat this core does not compute —
    //  EffectChainService resolves whether a Corrente triggers, with nothing to resist it by.
    ANATOMIA_UNICA(
            "Você é imune a Efeitos Críticos Menores. Sua resistência às Correntes de Efeitos "
                    + "aumenta em +2.",
            FeatRequirements.builder()
                    .requiredFeat(ANATOMIA_INCOMUM)
                    .build()),

    /**
     * "Você adquire Bônus Racial de +1 em Gnose ou Instinto, a sua escolha, mas sofre desvantagem
     * em rolagens de perícia baseadas 'Carisma' quando roladas contra criaturas não monstruosas."
     */
    // TODO: a Talento cannot grant an Atributo bonus, and this one is a choice besides.
    // TODO: the Desvantagem is scoped two ways this core cannot express at once — by
    //  AttributeDomain ("baseadas em Carisma") with a named Especialização carve-out
    //  (Persuasão: Intimidação), and by the target's CreatureType.
    // TODO: its Pré-requisito is a disjunction — Bestiais or Ogros, or any other Monstruosa with
    //  a Título — and every clause combines with and. The general branch is recorded, so a
    //  Bestial or Ogro without a Título is wrongly refused.
    DUAS_CABECAS(
            "Você possui 2 cabeças, duas pensam melhor do que uma. Você adquire Bônus Racial de "
                    + "+1 em Gnose ou Instinto, a sua escolha, mas sofre desvantagem em rolagens "
                    + "de perícia baseadas 'Carisma' (exceto Persuasão: Intimidação) quando "
                    + "roladas contra criaturas não monstruosas, devido temor ou repulsa que sua "
                    + "aparência causa.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.MONSTRUOSO)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Seus ossos são mais leves que o normal, porém mais frágeis. Você adquire vantagem em suas
     * rolagens de Perícia baseadas em Destreza e seu Movimento Base aumenta em +1UD, mas seu
     * multiplicador de PV é reduzido em -1." Both the Movimento and the PV malus are real.
     *
     * <p><b>The first Talento in the catalog to apply a malus for real</b>, and it does so because
     * the trade the rules text frames — lighter, therefore faster, therefore frailer — is
     * expressible on both sides. That is what distinguishes it from {@code GiganteFeat}'s Clã
     * Talentos and {@code GorgonaFeat#CABELO_SERPENTINO}, which are withheld whole precisely
     * because only their malus could be expressed.
     */
    // TODO: the Destreza Vantagem is scoped by AttributeDomain rather than naming a Perícia, and
    //  resolveSkillRollBonus takes a SkillType — "whichever Perícias Destreza currently governs"
    //  is itself live data once a substitution ability is held. Same gap CavalariaFeat
    //  #GRANDE_GINETE cites. Note this leaves the holder with one unpaid bonus, which is the
    //  reason the pair above is granted and the third clause is not: the movement/PV trade stands
    //  on its own.
    OSSOS_OCOS(
            "Seus ossos são mais leves que o normal, porém mais frágeis. Você adquire vantagem em "
                    + "suas rolagens de Perícia baseadas em Destreza e seu Movimento Base aumenta "
                    + "em +1UD, mas seu multiplicador de PV é reduzido em -1.",
            FeatRequirements.builder().build()) {
        @Override
        public int resolveMovementIncrease(final Character character) {
            return 1;
        }

        @Override
        public int resolveLifeMultiplierIncrease(final Character character) {
            return -1;
        }
    },

    /** "Você recebe Bônus Racial de +2 em DF e RDS." Both halves real. */
    // TODO: its Pré-requisito is a disjunction — "Raças Monstruosa ou Vigor 4" — and every clause
    //  combines with and. The Vigor branch is recorded rather than the race one, because it is
    //  the branch a non-Monstruoso can reach; a Monstruoso with Vigor 3 is wrongly refused.
    PELE_RIJA(
            "Você recebe Bônus Racial de +2 em DF e RDS.",
            FeatRequirements.builder()
                    .attributeDomain(AttributeDomain.VIGOR)
                    .requiredAttributeValue(4)
                    .build()) {
        @Override
        public int resolveDefenseBonus(final DefenseType defenseType, final Character character) {
            return defenseType == DefenseType.PHYSICAL ? PELE_RIJA_BONUS : 0;
        }

        @Override
        public int resolveDamageReduction(final Character character) {
            return PELE_RIJA_BONUS;
        }
    },

    /**
     * "Sempre que for atingido por um ataque Corpo-a-Corpo, o atacante sofre 1 ponto de Dano
     * Físico Elemental: Natural."
     */
    // TODO: retaliation damage — CLAUDE.md's "Reactive/retaliation damage" row. DamageService
    //  only ever computes damage *to* a target *from* an attacker, never the reverse, and nothing
    //  lets a victim respond to having been hit. Same blocker as ElementalFeat#REPARACAO_ELEMENTAL
    //  and TrollFeat#REGENERACAO_REATIVA_ESPINHOSA.
    // TODO: the +2 for an Arma Natural or Desarmado attacker needs the two markers CLAUDE.md's
    //  "Classifying an attack as Desarmado/Arma Natural" row names — and AttackSource reaches the
    //  attacker's own roll, not the victim's sheet.
    SANGUE_ACIDO(
            "Sempre que for atingido por um ataque Corpo-a-Corpo, o atacante sofre 1 ponto de "
                    + "Dano Físico Elemental: Natural. Este dano aumenta em +2 se o atacante tiver "
                    + "utilizado de Armas Naturais ou Desarmado.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.MONSTRUOSO)
                    .attributeDomain(AttributeDomain.VIGOR)
                    .requiredAttributeValue(3)
                    .build()),

    /**
     * "Uma terceira cabeça nasce em ti… Alvos de seus ataques e personagens intimidados por você
     * perdem temporariamente 1 ponto de Autocontrole."
     */
    // TODO: a Talento cannot grant an Atributo bonus, and "o Atributo faltante" additionally
    //  depends on which one DUAS_CABECAS chose — a per-acquisition choice a flat enum cannot hold.
    // TODO: draining a target's temporary Autocontrole is close: CombatantSheet#spendEgoPoints is
    //  exactly the raw-drain entry point (the one Primor uses, deliberately distinct from a
    //  holder's own deliberate use). What is missing is the trigger — nothing fires off "this
    //  character was the target of your attack", which is the "this one delivered attack" scoping
    //  gap. The recovery clause needs a per-Descanso hook RestService does not expose either.
    CERBERO(
            "Uma terceira cabeça nasce em ti. Você adquire bônus racial de +1 no Atributo "
                    + "faltante, aquele que não foi escolhido no talento 'Duas Cabeças', entre "
                    + "Instinto e Gnose. Alvos de seus ataques (físicos e mágicos) e personagens "
                    + "intimidados por você perdem temporariamente 1 ponto de Autocontrole. "
                    + "Pontos de Autocontrole perdidos desta forma são recuperados 1 a cada "
                    + "descanso completo, efeito não cumulativo.",
            FeatRequirements.builder()
                    .requiredFeat(DUAS_CABECAS)
                    .requiredAwakenedTitles(2)
                    .build()),

    /**
     * "Você pode mudar sua aparência, assumindo uma forma humana comum."
     *
     * <p>Tagged {@code Aventyr/Feérico/Monstruoso}; filed here rather than in {@code FeericoFeat}
     * because the source document prints it under the Monstruoso heading. Its Pré-requisito
     * covers both populations, and only the Monstruoso branch is enforced (next note).
     */
    // TODO: needs an appearance/form state — the same missing piece Gorgona's own forms,
    //  DraconicoFeat#DRACONATO and HomemFera's Forma Híbrida are all blocked on, plus a way to
    //  suppress physical racial traits while it holds.
    // TODO: its Pré-requisito is a disjunction — Monstruoso with a Título, or Feérico with none —
    //  and every clause combines with and, so a Feérico is wrongly refused.
    MIMETIZAR_FORMA_HUMANA(
            "Você pode mudar sua aparência, assumindo uma forma humana comum. Você perde "
                    + "características raciais físicas, como escamas, chifres, garras etc. Sua "
                    + "aparência base é similar, o máximo possível, de sua forma natural e é "
                    + "sempre igual, você não consegue se passar por outras pessoas com esta "
                    + "habilidade. Personagens Monstruosos precisam utilizar 2PD para ativar este "
                    + "efeito, personagens Feéricos utilizam 2PM.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.MONSTRUOSO)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Você recebe Bônus de +1 em rolagens de danos de suas Armas Naturais, este Bônus aumenta
     * cumulativamente em +1 para cada Título Aventyr que você tenha Desperto."
     */
    // TODO: a flat dano bonus from a Talento has no hook — resolveDamageBonus lives on
    //  SkillCompetencyAbility and EgoAdvantage, both reached through a skill Interaction rather
    //  than through character.getFeats(). And the scope is Armas Naturais, which do not exist.
    FEROCIDADE(
            "Você recebe Bônus de +1 em rolagens de danos de suas Armas Naturais, este Bônus "
                    + "aumenta cumulativamente em +1 para cada Título Aventyr que você tenha "
                    + "Desperto.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.MONSTRUOSO)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Você sente a presença de personagens que não sejam Monstros ou Monstruosos em Distância
     * Curta, identificando suas posições automaticamente."
     */
    // TODO: automatic detection with no roll — nothing models "knowing where someone is", and
    //  this core has no visibility or detection state for the clause to bypass.
    // TODO: "não recebe Desvantagens para atacar às cegas" suppresses a Desvantagem that is
    //  itself unmodelled — Skill#DISADVANTAGE_MALUS is applied by a caller, and nothing can
    //  cancel one. Same shape as CavalariaFeat#GRANDE_GINETE's own suppression clause.
    FAREJAR_O_MEDO(
            "Você sente a presença de personagens que não sejam Monstros ou Monstruosos em "
                    + "Distância Curta, identificando suas posições automaticamente, dispensando "
                    + "quaisquer testes de Perícia para encontrá-los. Você não recebe Desvantagens "
                    + "em Perícias para atacar às cegas os personagens identificados desta forma.",
            FeatRequirements.builder()
                    .requiredCreatureType(CreatureType.MONSTRUOSO)
                    .requiredFeat(DuelistaFeat.COMBATER_AS_CEGAS)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "O Dano Base de todas as suas Armas Naturais aumenta em +1. Os Bônus em danos concedidos
     * por Ferocidade são convertidos em Aumento de Dano Base."
     */
    // TODO: withheld even though Feat#resolveDamageBaseIncrease exists, because that hook is
    //  unconditional and sees neither the weapon nor the SkillType — granting it here would raise
    //  the Dano Base of *every* attack, where the clause covers Armas Naturais alone. Contrast
    //  AnaoFeat#FILHO_DE_YMIR, whose "de armas" scope is granted with the over-grant documented:
    //  there the excluded case is only an Ataque Desarmado, here it would be every weapon the
    //  character ever wields. The direction of the error is what differs, not the hook.
    // TODO: "os Bônus de Ferocidade são convertidos em Dano Base" is the exclusive-conversion
    //  shape AtaqueCorpoACorpoCompetencyAbility#BRUTALIDADE already implements for real — worth
    //  reading as the model, once Armas Naturais exist.
    SELVAGERIA(
            "O Dano Base de todas as suas Armas Naturais aumenta em +1. Os Bônus em danos "
                    + "concedidos por Ferocidade são convertidos em Aumento de Dano Base.",
            FeatRequirements.builder()
                    .requiredFeat(FEROCIDADE)
                    .requiredAwakenedTitles(1)
                    .requiredTitleArchetype(TitleArchetype.BRUTO)
                    .build());

    private static final int PELE_RIJA_BONUS = 2;

    private final String description;
    private final FeatRequirements featRequirements;

    MonstruosoFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.MONSTRUOSO;
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
