package org.aventyrs.core.feat;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.race.AbstractMesticoRace;

/**
 * Talentos Elementais — the tree of the six Mestiços Elementais (Agástias, Aquan, Colosso,
 * Dólos, Flaminídeo, Invernal), built around resisting, wielding and finally becoming one's own
 * element.
 *
 * <p><b>"Apenas personagens de Raças Elementais" is enforced through {@link
 * AbstractMesticoRace}</b>, which is exactly those six and nothing else — {@code MeioElfo} and
 * {@code NascidoDoDragao} are Mestiços that implement {@code Race} directly because they share
 * none of that base's structure. {@code requiredRace} tests with {@code isInstance}, so naming
 * the base gates the whole family in one clause.
 *
 * <p><b>The tree's own tables name an element per race, and no race records one.</b> Both Gana
 * Elemental and Resistência Elemental print a RAÇA→ELEMENTO table (Colosso→Terra, Invernal→Gelo,
 * Flaminídeo→Fogo, and so on), and three Talentos here key off "seu elemento". None of the six
 * race classes carries an {@code ElementalType} — only {@code NascidoDoDragao} does, for its own
 * Escamas Cromática. That is a missing <b>column on the races</b>, not on these Talentos, and it
 * would be the first thing to add before any of this tree can work. The tables also list an
 * "Elemental da Madeira" (Natural) that has no race class at all.
 *
 * <p>No constant carries a mechanical effect. Every one of them is blocked on the same missing
 * system, which is the single highest-value thing outstanding for the racial catalog:
 * <b>Resistência and Vulnerabilidade Elemental do not exist</b>. {@code DamageType} has no
 * elemental breakdown feeding RD/RA, nothing nullifies a damage type outright, and nothing
 * amplifies one either — the same gap {@code NascidoDoDragao}'s Escamas Cromática, {@code
 * Guampo}'s Benção Divina and {@code Troll}'s Anatomia Vegetal all cite.
 */
public enum ElementalFeat implements Feat {

    /**
     * "Ao tempo de 1PA e ao custo de 2PD você pode encantar uma de suas Armas Naturais ou uma
     * outra arma ao toque, o Dano Base da Arma escolhida aumenta em +2 e o tipo de dano causado
     * muda para Físico Elemental por 2 Rodadas."
     */
    // TODO: an activated ability spending PA and PD, which nothing converts into an effect.
    // TODO: the Dano Base uplift is scoped to one *chosen weapon* for 2 Rodadas.
    //  Feat#resolveDamageBaseIncrease is unconditional and sees neither the weapon nor a
    //  duration, so granting it there would raise every attack the holder ever makes, forever.
    // TODO: re-typing dano is not expressible — same gap OrquicoFeat#PALADINO_DE_EPONA and
    //  FeralFeat#DESPREZO_NATURAL cite — and "seu elemento" has no column to read (class javadoc).
    GANA_ELEMENTAL(
            "Ao tempo de 1PA e ao custo de 2PD você pode encantar uma de suas Armas Naturais ou "
                    + "uma outra arma ao toque, o Dano Base da Arma escolhida aumenta em +2 e o "
                    + "tipo de dano causado muda para Físico Elemental por 2 Rodadas.",
            FeatRequirements.builder()
                    .requiredRace(AbstractMesticoRace.class)
                    .build()),

    /**
     * "Aprender magias Elementais de seu elemento custa 0.5EXP a menos. Suas Magias Elementais
     * de dano e cura tem seus efeitos numéricos aumentados em +2, suas Magias de Encantamento
     * com este elemento tem a Duração aumentada em +1 Rodada."
     */
    // TODO: the EXP discount now has both a figure and a hook — SpellService#grantSpell spends
    //  SpellService.ACQUISITION_EXPERIENCE_COST (BigDecimal, so 0.5 fits) and getAcquisitionCost
    //  sums Feat#resolveSpellAcquisitionCostReduction. What still blocks it is the scope: "magias
    //  Elementais de seu elemento" needs both a per-Magia Elemental type (Spell has getPrimaryType/
    //  getSecondaryType, so "Elemental" is checkable) AND "seu elemento", which has no column to
    //  read (class javadoc) — so this constant can't yet tell which Elemental Magias qualify.
    // TODO: "efeitos numéricos aumentados em +2" — a few Magias now carry a structured
    //  Spell#getPrimaryDamage() (SpellDamage), resolved by SpellCastingService#resolvePrimaryDamage,
    //  so a flat +2 to the deterministic amount would have somewhere to land. Still blocked: no
    //  hook on that resolution for a Feat to contribute to, healing has no column at all, and the
    //  "de seu elemento" scope is the same missing element column the EXP half cites.
    // TODO: the Duração uplift would modify SpellDuration, which is authored per Magia and read
    //  as a constant; nothing resolves a Magia's Duração against its caster.
    ARCANISMO_ELEMENTAL(
            "Aprender magias Elementais de seu elemento custa 0.5EXP a menos. Suas Magias "
                    + "Elementais de dano e cura tem seus efeitos numéricos aumentados em +2, "
                    + "suas Magias de Encantamento com este elemento tem a Duração aumentada em "
                    + "+1 Rodada.",
            FeatRequirements.builder()
                    .requiredFeat(GANA_ELEMENTAL)
                    .build()),

    /**
     * "Conforme seu Elemento, você reduz o primeiro dano Elemental de cada Cena à metade (efeito
     * de Meio-Dano), então recebe Resistência Elemental (RE) para resistir aos efeitos Elementais
     * posteriores."
     */
    // TODO: RE does not exist, and damage-type-scoped mitigation does not either — see the class
    //  javadoc. Note the Meio-Dano half has a real mechanism (DamageService's halfDamage flag),
    //  but it cannot be reached: the flag is per-calculation, and scoping it to "the first
    //  Elemental damage of each Cena" needs both a damage type on the incoming hit and a
    //  per-Cena counter, neither of which exists.
    RESISTENCIA_ELEMENTAL(
            "Conforme seu Elemento, você reduz o primeiro dano Elemental de cada Cena à metade "
                    + "(efeito de Meio-Dano), então recebe Resistência Elemental (RE) para "
                    + "resistir aos efeitos Elementais posteriores.",
            FeatRequirements.builder()
                    .requiredRace(AbstractMesticoRace.class)
                    .build()),

    /**
     * "A cada Cena você pode reduzir a zero o primeiro dano elemental do elemento qual você é
     * resistente, danos posteriores são reduzidos à metade."
     */
    // TODO: same missing RE and damage-type scoping as its prerequisite, plus the same per-Cena
    //  counter. Reducing damage *to zero* is additionally a nullification stage beyond RD/RA —
    //  CLAUDE.md's "damage-type immunity" gap — which EscudeiroFeat already cites.
    RESISTENCIA_ELEMENTAL_SUPERIOR(
            "A cada Cena você pode reduzir a zero o primeiro dano elemental do elemento qual você "
                    + "é resistente, danos posteriores são reduzidos à metade.",
            FeatRequirements.builder()
                    .requiredFeat(RESISTENCIA_ELEMENTAL)
                    .build()),

    /**
     * "Como uma Reação você pode fazer com que seu corpo seja coberto pelo seu elemento…
     * Personagens adjacentes que te causarem danos enquanto seu corpo estiver coberto sofrem 2
     * pontos de Dano Físico Elemental."
     */
    // TODO: this is retaliation damage — CLAUDE.md's "Reactive/retaliation damage" row.
    //  DamageService only ever computes damage *to* a target *from* an attacker, never the
    //  reverse, and nothing lets a victim respond to having been hit.
    // TODO: spending a Reação to enter a Turn-scoped state has no mechanism either; Reações are
    //  a counter (ReactionsService), with nothing that spends one.
    REPARACAO_ELEMENTAL(
            "Como uma Reação você pode fazer com que seu corpo seja coberto pelo seu elemento ou "
                    + "de espinhos elementais, este efeito tem por Duração apenas o Turno em que "
                    + "foi ativado. Personagens adjacentes que te causarem danos enquanto seu "
                    + "corpo estiver coberto de seu elemento ou espinhos sofrem 2 pontos de Dano "
                    + "Físico Elemental, este Dano é aumentado em +1 para cada Título Aventyr que "
                    + "você tiver Desperto.",
            FeatRequirements.builder()
                    .requiredFeat(RESISTENCIA_ELEMENTAL)
                    .build()),

    /**
     * "No final de cada um de seus Turnos, enquanto estiver com Gana Cataclísmica ativa, você
     * causa 1d6 pontos de Dano Mágico Elemental a todos os personagens em Distância Curta."
     */
    // TODO: gated on an active Gana, which cannot be activated.
    // TODO: outward area damage at Turn end — the same shape DraconicoFeat#AURA_DRACONICA and
    //  TrollFeat#REGENERACAO_REATIVA_ESPINHOSA are blocked on: nothing turns a Range into a set
    //  of targets to damage, and CharacterSheet#finishTurn has no hook to fire from.
    // TODO: "reduzido em 1 para cada UD percorrido" is distance-falloff geometry this core never
    //  does — Range is a band, not a measured distance.
    AURA_CATACLISMICA(
            "No final de cada um de seus Turnos, enquanto estiver com Gana Cataclísmica ativa, "
                    + "você causa 1d6 pontos de Dano Mágico Elemental a todos os personagens em "
                    + "Distância Curta, este dano é reduzido em 1 para cada UD percorrido.",
            FeatRequirements.builder()
                    .requiredFeat(GANA_ELEMENTAL)
                    .requiredAwakenedTitles(2)
                    .build()),

    /**
     * "Seu primeiro ataque em cada Rodada, enquanto estiver com Gana Elemental ativo, tem a
     * Margem Crítica Menor aumentada em +1, tem sua Rolagem efetuada contra a DM de seu alvo e
     * causa danos mágicos."
     */
    // TODO: gated on an active Gana, which cannot be activated.
    // TODO: two further blockers — redirecting an Ataque roll to
    //  compare against DM instead of DF is not expressible, since a foe's Defesa is an authored
    //  number nothing compares a roll against yet; and "seu primeiro ataque em cada Rodada" is
    //  the "this one delivered attack" scoping gap. The Margem Crítica itself is no longer a
    //  blocker — Feat#resolveCriticalMarginIncrease is real (see PeritoFeat#CONTROLE_DA_SITUACAO)
    //  — but it cannot be scoped to one attack of the Rodada.
    // TODO: Corrente de Efeitos – Explosão Cataclísmica is not among the 13 EffectChainService
    //  resolves.
    GOLPE_CATACLISMICO(
            "Seu primeiro ataque em cada Rodada, enquanto estiver com Gana Elemental ativo, tem a "
                    + "Margem Crítica Menor aumentada em +1, tem sua Rolagem efetuada contra a DM "
                    + "de seu alvo e causa danos mágicos. Se este ataque for um Acerto Crítico ele "
                    + "receberá a Corrente de Efeitos – Explosão Cataclísmica.",
            FeatRequirements.builder()
                    .requiredFeat(GANA_ELEMENTAL)
                    .requiredAwakenedTitles(1)
                    .build()),

    /** "Você se torna imune ao elemento que você adquiriu resistência." */
    // TODO: damage-type immunity is a further stage beyond RD/RA and does not exist in any form —
    //  CLAUDE.md's "Damage-type-scoped mitigation, and damage-type immunity" row. The one
    //  immunity mechanism this core has is CriticalEffectType, which names Efeitos Críticos, not
    //  damage types.
    IMUNIDADE_ELEMENTAL(
            "Você se torna imune ao elemento que você adquiriu resistência.",
            FeatRequirements.builder()
                    .requiredFeat(RESISTENCIA_ELEMENTAL_SUPERIOR)
                    .requiredAwakenedTitles(1)
                    .build()),

    /**
     * "Você se transforma em um ser Elemental completo. Você recebe RDS e Resistência a Críticos.
     * O dano causado por sua Reparação Elemental muda para 1d6."
     *
     * <p>Unlike every other transformation in the racial catalog this one is <b>permanent</b> —
     * it is not a Forma with a Duração and a Custo, so its RDS would be an unconditional grant if
     * there were a hook for it.
     */
    // TODO: its Pré-requisito names *two* Talentos — Reparação Elemental and Resistência
    //  Elemental Superior — and FeatRequirements#requiredFeat is singular. Only the second is
    //  recorded, so the gate is looser than the text; this is one of the four constants
    //  docs/rules/talentos-index.md lists under "Two required Talentos".
    // The RDS half is real: RDS is ordinary RD (see ArtesCompetencyAbility's own "+1 RDS"), and
    // this transformation is permanent rather than a Forma with a Duração, so the grant is
    // unconditional. The clause states no number, so it uses DamageService's own default — the
    // convention CLAUDE.md sets for an RD clause with no figure in its rules text.
    // TODO: Resistência a Críticos is not a stat this core computes — distinct from
    //  Race#getCriticalEffectImmunities(), which is an all-or-nothing filter keyed on an identity.
    TRANSFORMACAO_ELEMENTAL(
            "Você se transforma em um ser Elemental completo. Você recebe RDS e Resistência a "
                    + "Críticos. O dano causado por sua Reparação Elemental muda para 1d6, este "
                    + "dano aumenta em +1 para cada Título Aventyr Desperto.",
            FeatRequirements.builder()
                    .requiredFeat(RESISTENCIA_ELEMENTAL_SUPERIOR)
                    .requiredAwakenedTitles(2)
                    .build()) {
        @Override
        public int resolveDamageReduction(final Character character) {
            return DamageService.DEFAULT_DAMAGE_REDUCTION;
        }
    };

    private final String description;
    private final FeatRequirements featRequirements;

    ElementalFeat(final String description, final FeatRequirements featRequirements) {
        this.description = description;
        this.featRequirements = featRequirements;
    }

    @Override
    public FeatCategory getFeatCategory() {
        return FeatCategory.ELEMENTAL;
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
