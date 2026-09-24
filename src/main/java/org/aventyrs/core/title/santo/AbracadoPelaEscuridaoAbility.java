package org.aventyrs.core.title.santo;

import java.util.Optional;

import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitleAbility;
import org.aventyrs.core.title.AventyrTitleSpecialization;
import org.aventyrs.core.title.PDCost;
import static org.aventyrs.core.title.PDCost.fixed;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * The Habilidades/Suprema gated on holding the {@link SantoSpecialization#ABRACADO_PELA_ESCURIDAO}
 * Especialização specifically — see {@link AbencoadoPelaLuzAbility}'s own javadoc for why
 * these live in their own enum rather than folded into {@link SantoAbility}, and for why every
 * constant here overrides {@code AventyrTitleAbility#getRequiredSpecialization()} with {@code
 * SantoSpecialization#ABRACADO_PELA_ESCURIDAO} (real, enforced data now — see {@code
 * AventyrTitleAbility#isEligible}). {@code FUROR_DE_SYLPH} additionally overrides {@code
 * getRequiredOtherAbilities()} for its own "2 Habilidades de 'Abraçado pela Escuridão'"
 * clause, counted only against sibling constants of this same enum.
 */
@Getter
@AllArgsConstructor
public enum AbracadoPelaEscuridaoAbility implements AventyrTitleAbility {

    // Requer Especialização 'Abraçado pela Escuridão' — enforced (see class javadoc).
    // "Custo de Ativação: Variável" is the PV cost (equal to Vigor), not a PD cost — PDCost is
    // genuinely 0, not merely unmodeled. Real through SacrificioYmirianoInteraction: the PV cost is
    // paid by AbstractTitleAbilityInteraction#resolveHitPointCost off #resolveVigorPvCost below;
    // the Força bonus is a Round-scoped AttributeDomain#getBonusModifierType() TemporaryBonus,
    // which reaches a Força-governed roll and, since 0.0.51, every sheet-holding
    // Character#getEffectiveAttributeTotal reader too (the melee ½-Força dano term among them); and
    // the Categoria de Tamanho +2 is a
    // ModifierType.SIZE_CATEGORY bonus read by CharacterSizeService#getEffectiveSizeCategory(
    // CombatantSheet). Both figures are now Vigor-scaled rather than flat, and the Duração is a
    // flat 1 Rodada — V19 dropped the whole "ativada até duas vezes / no mesmo Turno / seu efeito é
    // cumulativo" clause the previous revision had, so nothing here counts activations any more.
    // TODO: "a Margem Crítica Menor de seus ataques direcionados à inimigos que tenham infligido
    //  danos aos seus aliados aumenta em +2" — nothing tracks *who damaged an ally*. Scene
    //  #recordAttack(attacker, defender) records that an attack was declared, not that it landed,
    //  and there is no ally-scoped variant of it; a per-attacker "has harmed my group" ledger would
    //  be the missing piece. Not the same gap as the Margem Crítica arithmetic itself, which is
    //  real (AbstractSkillInteraction#sumCriticalMarginIncrease).
    // The "recuperados com Descansos Verdadeiros ou Roubo de Vida" clause needs the
    // same "locked, Rest/Roubo-de-Vida-only" HP-loss subtype SantoAbility
    // #PROTETOR_DA_VIDA_E_DA_MORTE's own TODO cites — "Descanso Verdadeiro" itself maps onto
    // this codebase's own RestType.LONGO-or-higher tiers, the same inference
    // VigorAbility#METABOLISMO_RAPIDO/FocusAbility#CANALIZADOR_DE_MANA already establish, so
    // that half of the phrase isn't itself a new gap.
    SACRIFICIO_YMIRIANO(
            "O Custo de Ativação desta Habilidade é igual ao seu próprio Vigor em PV. Você " +
            "recebe Bônus Variável de Força igual ao seu Vigor e sua Categoria de Tamanho " +
            "aumenta em +2 por 1 Rodada. Enquanto Sacrifício Ymiriano estiver ativo a Margem " +
            "Crítica Menor de seus ataques direcionados à inimigos que tenham infligido danos " +
            "aos seus aliados aumenta em +2. Pontos de Vida perdidos desta forma só podem ser " +
            "recuperados com Descansos Verdadeiros ou Roubo de Vida.",
            false, fixed(0), ActionCost.ofActionPoints(1), Optional.of(SacrificioYmirianoInteraction.class),
            Optional.of(SantoSpecialization.ABRACADO_PELA_ESCURIDAO), 0),

    // Requer Especialização 'Abraçado pela Escuridão' — enforced (see class javadoc).
    // "Custo de Ativação: Variável" is the PV cost (equal to Vigor, shared with
    // SACRIFICIO_YMIRIANO and FUROR_DE_SYLPH via #resolveVigorPvCost), not a PD cost — PDCost is
    // genuinely 0. V19 replaced the previous revision's player-chosen PV amount and its
    // "1 + metade dos PV gastos" Duração with a Vigor cost and a flat 2 Rodadas, so no PV-scaled
    // arithmetic remains on this constant.
    // Real now through EspinhosVenenosDeGaeaInteraction. The thorns are modelled as a *state*
    // rather than a listener: the activation grants a round-scoped ModifierType.RETALIATION_DAMAGE
    // Blessing worth the holder's Vigor, and AttackDelivery/AttackReceiver read it off the
    // defender on any Ataque Corpo-a-Corpo and report a combat.Retaliation. That fits existing
    // machinery exactly, the Duração being 2 Rodadas — which is what a TemporaryBonus counts.
    // The damage is *reported, never dealt*: this core computes damage only to a target from an
    // attacker, so the caller applies it against the attacker's own sheet — where the attacker's
    // RD/RA can judge it like any other incoming hit, which is where it belongs anyway.
    // The Malefício Veneno half is real too: ConditionType.ENVENENADO now carries a
    // ModifierType.LIFE_MULTIPLIER -1, read by HitPointsService#getLifeMultiplier(Character,
    // CombatantSheet), so an attacker who takes it really does lose maximum PV. ⚠️ That entry's
    // own catalogue wording says "Multiplicador de Bônus Base" and this clause says
    // "Multiplicador de Pontos de Vida" — the concrete clause is taken as authoritative; see
    // ConditionType#ENVENENADO's own note.
    // "Atacarem", not "acertarem": the thorns are reported whether or not the attack landed, and
    // only the Veneno half is conditional on damage actually being dealt.
    ESPINHOS_VENENOS_DE_GAEA(
            "O Custo de Ativação desta Habilidade é igual ao seu próprio Vigor em PV. " +
            "Enquanto ativo os Espinhos Venenos de Gaea causam danos aos personagens que lhe " +
            "atacarem Corpo-a-Corpo, Vigor pontos de Dano Físico Elemental: Natural. " +
            "Personagem que lhe infligirem danos adicionalmente perdem -1 Multiplicador de " +
            "Pontos de Vida (Malefício Veneno) por 2 Rodadas. Duração do Efeito: 2 Rodadas.",
            false, fixed(0), ActionCost.ofActionPoints(2), Optional.of(EspinhosVenenosDeGaeaInteraction.class),
            Optional.of(SantoSpecialization.ABRACADO_PELA_ESCURIDAO), 0),

    // Requer Especialização 'Abraçado pela Escuridão' — enforced (see class javadoc). Fixed
    // cost (2PD/3PA — not "Variável" like its siblings). Real through
    // PlacidezDeUndineRancorDeHaloiInteraction, which *reports* what the one attack gets rather
    // than applying anything: the activation happens before that attack exists, so the caller
    // builds it with the report in hand — the same ordering that makes SantoAbility#GUARDA_VIDAS
    // work without touching AttackDelivery. See that Interaction's own javadoc for why the Margem
    // Crítica and the Roubo de Vida are reported rather than granted, and for what is still
    // missing (Roubo de Determinação, and the per-target 2-Rodada immunity).
    // V19 removed the previous revision's "Roubo de Mana 2" clause entirely and raised Roubo de
    // Determinação from 1 to 2, so the old comment's Roubo-de-Mana citation no longer applies.
    PLACIDEZ_DE_UNDINE_RANCOR_DE_HALOI(
            "Como parte da ativação desta Habilidade você deve desferir um ataque físico, sua " +
            "Margem Crítica Menor para este ataque aumenta em +2 números. Este ataque você " +
            "recebe Roubo de Vida 3, Roubo de Determinação 2 e a Corrente de Efeitos – Rancor " +
            "de Haloi: Este ataque recebe Oferenda Maldita como um Efeito Crítico adicional. " +
            "Inimigos que tenham sofrido danos desta Habilidade se tornam imunes a ela por 2 " +
            "Rodadas.",
            false, fixed(2), ActionCost.ofActionPoints(3),
            Optional.of(PlacidezDeUndineRancorDeHaloiInteraction.class),
            Optional.of(SantoSpecialization.ABRACADO_PELA_ESCURIDAO), 0),

    // Requer 2 Habilidades de 'Abraçado pela Escuridão' — enforced (see class javadoc; same
    // "its own comment never repeats the base Especialização requirement, but the class-level
    // javadoc already states every constant here needs it, and 2 sibling Habilidades implies
    // it anyway" inference AbencoadoPelaLuzAbility#GLORIA_RELAMPEJANTE_DE_TESLA's own comment
    // documents). Same "Custo de Ativação: Variável" nuance as SACRIFICIO_YMIRIANO — the PV cost
    // equals Vigor's total, real via #resolveVigorPvCost; PDCost(2) is the separate, fixed PD cost
    // the rules text does state.
    // Real now through FurorDeSylphInteraction. What used to block it was that everything here is
    // scoped to a *count of attacks* (#resolveEnhancedAttackCountFromPvSpent) and the clause names
    // no Duração in Rodadas, while a TemporaryBonus only ever counts down in Rodadas. That is what
    // CombatantSheet#grantEnhancedAttacks now carries: a budget, spent per attack, cleared by
    // nothing. The +1PA rides on the same budget via AventyrTitleAbility#resolveActionPointBonus,
    // scanned by ActionPointsServiceImpl rather than granted — so it lasts exactly as long as the
    // attacks do.
    // Still TODO, and none of these is this clause's own gap:
    // - "o Aprimoramento de Obra-Prima Alcance Estendido" — no such entry exists in the
    //   Obra-Prima/Aprimoramento catalogue (OffensiveMasterpiece/OffensiveImprovement);
    // - "seu alvo é empurrado 1UD para trás e você pode se Reposicionar" — forced movement (the
    //   Reposicionar itself is RepositionService now);
    // - "+1d6" of dano — reportable in principle (FuriaDosDeusesInteraction does exactly that),
    //   but this is an Ação Livre funding *several* later attacks rather than empowering one named
    //   attack, so there is no single attack to report it onto.
    // The locked-HP-pool clause is the same gap as every other "Descansos ou Roubo de Vida"
    // citation above.
    FUROR_DE_SYLPH(
            "Para ativar esta Habilidade você deve gastar uma quantidade de pontos de vida " +
            "igual ao seu Vigor. Você recebe Bônus de +1PA e seus ataques recebem o " +
            "Aprimoramento de Obra-Prima Alcance Estendido e a Corrente de Efeitos – Furor " +
            "de Sylph: O dano de seu ataque aumenta em +1d6 e seu alvo é empurrado 1UD para " +
            "trás e você pode se Reposicionar. O Furor de Sylph aprimora uma quantidade de " +
            "ataques igual à 1+ metade dos PV gastos com esta Habilidade, PV perdidos desta " +
            "forma só podem ser recuperados com Descansos ou Roubo de Vida.",
            true, fixed(2), ActionCost.FREE_ACTION, Optional.of(FurorDeSylphInteraction.class),
            Optional.of(SantoSpecialization.ABRACADO_PELA_ESCURIDAO), 2) {
        /** "Você recebe Bônus de +1PA", for as long as enhanced attacks remain unspent. */
        @Override
        public int resolveActionPointBonus(final CombatantSheet holder) {
            return holder != null && holder.getRemainingEnhancedAttacks(this) > 0
                    ? FurorDeSylphInteraction.ACTION_POINT_BONUS
                    : 0;
        }
    };

    private final String description;
    private final boolean supreme;
    private final PDCost PDCost;
    private final ActionCost actionPointCost;
    private final Optional<Class<? extends Interaction>> interactionClass;
    private final Optional<AventyrTitleSpecialization> requiredSpecialization;
    private final int requiredOtherAbilities;

    /**
     * The "O Custo de Ativação desta Habilidade é igual ao seu próprio Vigor em PV" cost shared by
     * {@code SACRIFICIO_YMIRIANO}, {@code ESPINHOS_VENENOS_DE_GAEA} and {@code FUROR_DE_SYLPH} —
     * real, tested: Vigor's total is already a plain value this core computes everywhere else
     * ({@code character.getEffectiveAttributeTotal(AttributeDomain.VIGOR)}), so this needs no
     * missing system of its own, even though what the spent PV buys is still TODO'd on two of the
     * three. Under V19 this is the cost of every constant here <em>except</em> {@code
     * PLACIDEZ_DE_UNDINE_RANCOR_DE_HALOI}, which is PD-priced; it returns 0 for that one rather
     * than throwing, so a caller never needs to guard on which constant it's holding.
     */
    public int resolveVigorPvCost(final Character character) {
        if (this == PLACIDEZ_DE_UNDINE_RANCOR_DE_HALOI) {
            return 0;
        }
        return character.getEffectiveAttributeTotal(AttributeDomain.VIGOR);
    }

    /**
     * Furor de Sylph's own "aprimora uma quantidade de ataques igual à 1+ metade dos PV
     * gastos com esta Habilidade" — a <em>count of enhanced attacks</em>, never a Duração in
     * Rodadas, which is exactly why that constant stays unwired: a {@code TemporaryBonus} only
     * ever counts down in Rodadas, and this clause names none. Real, tested arithmetic all the
     * same. Returns 0 for every other constant.
     *
     * <p>Under V19 this is the only PV-scaled formula left here — {@code
     * ESPINHOS_VENENOS_DE_GAEA}'s Duração is now a flat 2 Rodadas rather than the previous
     * revision's "1 + metade dos PV gastos", so the twin this method used to share its shape with
     * is gone.
     */
    public int resolveEnhancedAttackCountFromPvSpent(final int pvSpent) {
        if (this != FUROR_DE_SYLPH) {
            return 0;
        }
        return Math.max(1, 1 + pvSpent / 2);
    }
}
