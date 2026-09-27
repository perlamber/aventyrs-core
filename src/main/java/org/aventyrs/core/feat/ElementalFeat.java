package org.aventyrs.core.feat;

import java.util.Optional;
import java.math.BigDecimal;
import org.aventyrs.core.magic.Spell;
import org.aventyrs.core.magic.MagicType;
import org.aventyrs.core.magic.ElementalType;
import org.aventyrs.core.character.DamageType;
import org.aventyrs.core.character.DamageScope;
import org.aventyrs.core.character.DamageDescriptor;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.character.services.DamageService;
import org.aventyrs.core.race.AbstractMesticoRace;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.sheet.CombatantSheet;

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
 * <p><b>"Seu elemento" is readable now</b> — {@code AbstractMesticoRace#getElement()}, the column
 * both tables assign — so the resistance ladder is real: {@link #RESISTENCIA_ELEMENTAL}'s RE,
 * {@link #RESISTENCIA_ELEMENTAL_SUPERIOR}'s Meio-Dano, {@link #IMUNIDADE_ELEMENTAL}'s immunity, and
 * {@link #ARCANISMO_ELEMENTAL}'s EXP discount, plus {@link #TRANSFORMACAO_ELEMENTAL}'s RDS and
 * Resistência a Críticos. The paragraph below predates that and is kept for its history:
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
    // The activation itself would fit ActiveAbility as it stands — 1PA, a 2PD cost
    // (ActiveAbility#getDeterminationPointCost, added for the Formas) and a 2-Rodada Duração. It
    // is not authored because every effect it would apply is blocked below.
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
    // The EXP discount is real: 0.5 off a Magia whose Árvore is Elemental of the holder's element
    // (AbstractMesticoRace#getElement; an Árvore of every element, ElementalType.TODOS, counts).
    // "Suas Magias Elementais de dano … tem seus efeitos numéricos aumentados em +2" is real
    // (Feat#resolveSpellDamageBonus), for an Elemental Magia of the holder's element.
    // TODO: "e cura" — a Magia's healing has no resolved figure on SpellCastingResult.
    // "Suas Magias de Encantamento com este elemento tem a Duração aumentada em +1 Rodada" is real
    // (Feat#resolveSpellDurationIncrease, summed by SpellDurationService on an extendable Duração).
    ARCANISMO_ELEMENTAL(
            "Aprender magias Elementais de seu elemento custa 0.5EXP a menos. Suas Magias "
                    + "Elementais de dano e cura tem seus efeitos numéricos aumentados em +2, "
                    + "suas Magias de Encantamento com este elemento tem a Duração aumentada em "
                    + "+1 Rodada.",
            FeatRequirements.builder()
                    .requiredFeat(GANA_ELEMENTAL)
                    .build()) {
        @Override
        public int resolveSpellDamageBonus(final Spell spell, final Character character,
                                           final java.util.Set<Feat> activatedFeats) {
            return ofOwnElement(spell, character, MagicType.ELEMENTAL) ? ARCANISMO_NUMERIC_BONUS : 0;
        }

        @Override
        public int resolveSpellDurationIncrease(final Spell spell, final Character character) {
            return ofOwnElement(spell, character, MagicType.ENCANTAMENTO) ? ARCANISMO_DURATION_BONUS : 0;
        }

        @Override
        public BigDecimal resolveSpellAcquisitionCostReduction(final Character character, final Spell spell) {
            Optional<ElementalType> own = elementOf(character);
            Optional<ElementalType> spellElement = spell.getTree().getElementalType();
            boolean ownElement = own.isPresent() && spell.getTree().hasMagicType(MagicType.ELEMENTAL)
                    && spellElement.filter(el -> el == own.get() || el == ElementalType.TODOS).isPresent();
            return ownElement ? ARCANISMO_EXPERIENCE_DISCOUNT : BigDecimal.ZERO;
        }
    },

    /**
     * "Conforme seu Elemento, você reduz o primeiro dano Elemental de cada Cena à metade (efeito
     * de Meio-Dano), então recebe Resistência Elemental (RE) para resistir aos efeitos Elementais
     * posteriores."
     */
    // The RE is real: one instance against the holder's own element (AbstractMesticoRace
    // #getElement), summed by CombatantSheet#getElementalResistanceInstances. ⚠️ It applies from the
    // first hit, where the text halves the first hit of each Cena and grants RE only "para resistir
    // aos efeitos Elementais posteriores".
    // TODO: the first-hit-of-each-Cena Meio-Dano — nothing counts elemental hits suffered per Cena
    //  (getAttacksSufferedThisRound is per Rodada and blind to type).
    RESISTENCIA_ELEMENTAL(
            "Conforme seu Elemento, você reduz o primeiro dano Elemental de cada Cena à metade "
                    + "(efeito de Meio-Dano), então recebe Resistência Elemental (RE) para "
                    + "resistir aos efeitos Elementais posteriores.",
            FeatRequirements.builder()
                    .requiredRace(AbstractMesticoRace.class)
                    .build()) {
        @Override
        public int resolveElementalResistanceInstances(final ElementalType element, final Character character,
                                                        final CombatantSheet holder) {
            return elementOf(character).filter(own -> own == element).isPresent() ? 1 : 0;
        }
    },

    /**
     * "A cada Cena você pode reduzir a zero o primeiro dano elemental do elemento qual você é
     * resistente, danos posteriores são reduzidos à metade."
     */
    // "Danos posteriores são reduzidos à metade" is real: a Meio-Dano scoped to the holder's own
    // element (Feat#halvesDamage, DamageScope#element).
    // TODO: "reduzir a zero o primeiro dano elemental" of each Cena — the zeroing stage exists
    //  (Feat#isImmuneToDamage), but nothing counts elemental hits per Cena to pick out the first.
    //  So the first hit is halved like the rest.
    RESISTENCIA_ELEMENTAL_SUPERIOR(
            "A cada Cena você pode reduzir a zero o primeiro dano elemental do elemento qual você "
                    + "é resistente, danos posteriores são reduzidos à metade.",
            FeatRequirements.builder()
                    .requiredFeat(RESISTENCIA_ELEMENTAL)
                    .build()) {
        @Override
        public boolean halvesDamage(final DamageType damageType, final DamageDescriptor descriptor,
                                    final Character character, final CombatantSheet holder) {
            return elementOf(character).map(own -> DamageScope.element(own).matches(damageType, descriptor))
                    .orElse(false);
        }
    },

    /**
     * "Como uma Reação você pode fazer com que seu corpo seja coberto pelo seu elemento…
     * Personagens adjacentes que te causarem danos enquanto seu corpo estiver coberto sofrem 2
     * pontos de Dano Físico Elemental."
     */
    // TODO: the retaliation itself would fit Feat#resolveRetaliation (reported on a landed melee
    //  hit), and a Reação can be spent now (CombatantSheet#spendReaction); what is missing is the
    //  state it gates on — "coberto pelo seu elemento" for one Turn, entered as a Reação. An
    //  ActiveAbility with a REACTION cost could enter it, but no Turn-scoped state carries the
    //  Talento's own retaliation while it lasts.
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
    // TODO: redirecting the Ataque roll to the target's DM instead of DF — the defender's Defesa
    //  type is the caller's pick (DeliveredAttack#defenseType), and no Talento hook can change it.
    //  "Seu primeiro ataque em cada Rodada" is not a blocker: the sheet-aware
    //  resolveCriticalMarginIncrease plus isFirstAttackRollOfTurn/the action log already express
    //  it (AssassinoFeat#ACERTO_CRITICO_RELAMPAGO). "Causa danos mágicos" is the damage-retyping
    //  gap (plan Phase D).
    // TODO: Corrente de Efeitos – Explosão Cataclísmica is not an authored EffectChain.
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
    // Real: immunity to hits of the holder's own element (Feat#isImmuneToDamage), judged before
    // every reduction.
    IMUNIDADE_ELEMENTAL(
            "Você se torna imune ao elemento que você adquiriu resistência.",
            FeatRequirements.builder()
                    .requiredFeat(RESISTENCIA_ELEMENTAL_SUPERIOR)
                    .requiredAwakenedTitles(1)
                    .build()) {
        @Override
        public boolean isImmuneToDamage(final DamageType damageType, final DamageDescriptor descriptor,
                                        final Character character, final CombatantSheet holder) {
            return elementOf(character).map(own -> DamageScope.element(own).matches(damageType, descriptor))
                    .orElse(false);
        }
    },

    /**
     * "Você se transforma em um ser Elemental completo. Você recebe RDS e Resistência a Críticos.
     * O dano causado por sua Reparação Elemental muda para 1d6."
     *
     * <p>Unlike every other transformation in the racial catalog this one is <b>permanent</b> —
     * it is not a Forma with a Duração and a Custo, so its RDS would be an unconditional grant if
     * there were a hook for it.
     */
    // Both required Talentos — Reparação Elemental and Resistência Elemental Superior — are
    // enforced now: FeatRequirements#requiredFeats is a set.
    // The RDS half is real: RDS is ordinary RD (see ArtesCompetencyAbility's own "+1 RDS"), and
    // this transformation is permanent rather than a Forma with a Duração, so the grant is
    // unconditional. The clause states no number, so it uses DamageService's own default — the
    // convention CLAUDE.md sets for an RD clause with no figure in its rules text.
    // The Resistência a Críticos is real too, and unconditional for the same reason the RDS is:
    // one instance (the clause states no figure), summed by
    // CombatantSheet#getTotalCriticalResistance off the attack target's own sheet. Still distinct
    // from Race#getCriticalEffectImmunities(), an all-or-nothing filter keyed on an identity.
    TRANSFORMACAO_ELEMENTAL(
            "Você se transforma em um ser Elemental completo. Você recebe RDS e Resistência a "
                    + "Críticos. O dano causado por sua Reparação Elemental muda para 1d6, este "
                    + "dano aumenta em +1 para cada Título Aventyr Desperto.",
            FeatRequirements.builder()
                    .requiredFeat(RESISTENCIA_ELEMENTAL_SUPERIOR)
                    .requiredFeat(REPARACAO_ELEMENTAL)
                    .requiredAwakenedTitles(2)
                    .build()) {
        /** "Você recebe RDS" with no figure — one instance. */
        @Override
        public int resolveDamageTakenReduction(final Character character) {
            return DamageService.DAMAGE_TAKEN_REDUCTION_INSTANCE;
        }

        @Override
        public int resolveCriticalResistance(final Character character, final SceneContext sceneContext) {
            return CombatantSheet.CRITICAL_RESISTANCE_INSTANCE;
        }
    };

    /** ARCANISMO_ELEMENTAL's "custa 0.5EXP a menos". */
    private static final BigDecimal ARCANISMO_EXPERIENCE_DISCOUNT = new BigDecimal("0.5");

    /** ARCANISMO_ELEMENTAL's "efeitos numéricos aumentados em +2". */
    private static final int ARCANISMO_NUMERIC_BONUS = 2;

    /** ARCANISMO_ELEMENTAL's "Duração aumentada em +1 Rodada". */
    private static final int ARCANISMO_DURATION_BONUS = 1;

    /** Whether spell's Árvore carries type and the holder's own element (an all-element Árvore counts). */
    private static boolean ofOwnElement(final Spell spell, final Character character, final MagicType type) {
        Optional<ElementalType> own = elementOf(character);
        return own.isPresent() && spell.getTree().hasMagicType(type)
                && spell.getTree().getElementalType().filter(el -> el == own.get() || el == ElementalType.TODOS).isPresent();
    }

    /** "Seu elemento" — the holder's race's own, when it is one of the Raças Elementais. */
    private static Optional<ElementalType> elementOf(final Character character) {
        return character.getRace() instanceof AbstractMesticoRace elemental
                ? Optional.ofNullable(elemental.getElement()) : Optional.empty();
    }

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
