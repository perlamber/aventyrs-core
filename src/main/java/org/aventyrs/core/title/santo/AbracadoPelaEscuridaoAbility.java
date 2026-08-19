package org.aventyrs.core.title.santo;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Optional;

import org.aventyrs.core.character.Character;
import org.aventyrs.core.sheet.Interaction;
import org.aventyrs.core.title.AventyrTitleAbility;

/**
 * The Habilidades/Suprema gated on holding the {@link SantoSpecialization#ABRACADO_PELA_ESCURIDAO}
 * Especialização specifically — see {@link AbencoadoPelaLuzAbility}'s own javadoc for why
 * these live in their own enum rather than folded into {@link SantoAbility}.
 */
@Getter
@AllArgsConstructor
public enum AbracadoPelaEscuridaoAbility implements AventyrTitleAbility {

    // Requer Especialização 'Abraçado pela Escuridão'. "Custo de Ativação: Variável" is the
    // PV cost below (equal to Vigor), not a PD cost — PDCost is genuinely 0, not merely
    // unmodeled. The "gastar PV igual ao seu Vigor" half is real — see
    // #resolveVigorPvCost below, since Vigor's total is already a plain, real value
    // (character.getAttributes().getVigor().getTotal()). Everything the spent PV buys is
    // fully TODO'd, three separate gaps: (1) "+2 em Força" (then +3 if activated twice in the
    // same Turn) is a *temporary*, Round-scoped bonus to an Attribute's own total — unlike
    // every other stat this core tracks (Reações, PA, RD/RA, skill rolls, ...), Attributes are
    // never summed via ModifierType/CharacterSheet#getTemporaryBonus at all
    // (AttributeValue only has base/racialBonus/variable, all permanent) — no mechanism for a
    // temporary Attribute bonus exists anywhere in this core; (2) "ativada duas vezes no mesmo
    // Turno" needs a per-ability, within-Turn activation-count tracker, which doesn't exist
    // (CharacterSheet tracks Round-scoped TemporaryEffects, not a same-Turn activation
    // counter); (3) the Categoria de Tamanho +1 half could in principle reuse
    // ModifierType.SIZE_CATEGORY, but still needs (1) and (2) solved first to know *when* to
    // grant it. The "recuperados com Descansos Verdadeiros ou Roubo de Vida" clause needs the
    // same "locked, Rest/Roubo-de-Vida-only" HP-loss subtype SantoAbility
    // #PROTETOR_DA_VIDA_E_DA_MORTE's own TODO cites — "Descanso Verdadeiro" itself maps onto
    // this codebase's own RestType.LONGO-or-higher tiers, the same inference
    // VigorAbility#METABOLISMO_RAPIDO/FocusAbility#CANALIZADOR_DE_MANA already establish, so
    // that half of the phrase isn't itself a new gap.
    SACRIFICIO_YMIRIANO(
            "Para ativar esta Habilidade você deve gastar uma quantidade de pontos de vida " +
            "igual ao seu Vigor, se o fizer você recebe Bônus Variável de +2 em Força por 2 " +
            "Rodadas. Esta Habilidade pode ser ativada até duas vezes, seu efeito é " +
            "cumulativo. Se Sacrifício Ymiriano for ativada duas vezes no mesmo Turno o " +
            "Bônus em Força muda para +3, sua Categoria de Tamanho é aumentada em +1 e a " +
            "Duração do efeito aumenta para 3 Rodadas. Pontos de Vida perdidos desta forma " +
            "só podem ser recuperados com Descansos Verdadeiros ou Roubo de Vida.",
            false, 0, 1, false, Optional.empty()),

    // Requer Especialização 'Abraçado pela Escuridão'. "Custo de Ativação: Variável" is the
    // "qualquer quantidade de PV (mínimo 1)" spent below, not a PD cost — PDCost is genuinely
    // 0. The Duração formula ("1 + metade dos PV gastos, mínimo 1 Rodada") is real — see
    // #resolveDurationFromPvSpent below, pure arithmetic over the player's own chosen PV
    // amount, no missing system needed. The protective effect itself is fully TODO'd: this
    // core has no "attacker takes reactive/retaliation damage for attacking a protected
    // target" mechanism at all (DamageService only ever computes damage *to* a target *from*
    // an attacker, never the reverse), and "+1d6 pontos de Dano" on a successful attack needs
    // this core's "never rolls dice" boundary crossed, which it deliberately never does.
    ESPINHOS_DE_GAEA(
            "Para ativar esta Habilidade você deve gastar qualquer quantidade de PV (mínimo " +
            "1), a Duração desta Habilidade é igual a 1+ metade dos PV gastos (mínimo 1 " +
            "Rodada). Enquanto protegido pelos Espinhos de Gaea, personagens que te atacarem " +
            "corpo-a-corpo sofrem 2 pontos de Dano Mágico Elemental: Natural, se o ataque " +
            "for bem-sucedido o atacante sofrerá +1d6 pontos de Dano.",
            false, 0, 2, false, Optional.empty()),

    // Requer Especialização 'Abraçado pela Escuridão'. Fixed cost (2PD/3PA — not "Variável"
    // like its siblings), so that data is real; the effect is fully TODO'd. "Sua Margem
    // Crítica Menor... aumenta em +2" and the Roubo de Vida/Mana/Determinação amounts are all
    // scoped to "este ataque" specifically — the one attack delivered *as part of* this
    // Habilidade's own activation — not a standing bonus to every future roll, so this doesn't
    // fit AventyrTitleAbility#resolveAbsoluteDamageReduction's shape (built for continuously
    // scanned passives) or any existing per-roll resolve* hook; this core's roll-resolution
    // machinery (AbstractSkillInteraction/SkillRoll) computes bonuses for *any* roll of a given
    // skill type, with no concept of "the one attack being performed right now as part of
    // activating a different ability." Roubo de Mana/Roubo de Determinação don't exist at all
    // (only Roubo de Vida does, via LifeStealService — see FocusAbility's own "needs a Roubo
    // de Mana effect to exist in the first place" citation for the identical gap). Corrente de
    // Efeitos (Oferenda Maldita as an additional Efeito Crítico) is the same unbuilt system
    // AutocontroleAdvantage#RESOLUTO already cites. "Imunes a ela por 2 Rodadas" needs a
    // per-ability, per-target, Round-scoped immunity tracker, which doesn't exist anywhere.
    PLACIDEZ_DE_UNDINE_RANCOR_DE_HALOI(
            "Como parte da ativação desta Habilidade você deve desferir um ataque com sua " +
            "Arma, sua Margem Crítica Menor para este ataque aumenta em +2 números. Para " +
            "este ataque você recebe Roubo de Vida 3, Roubo de Mana 2, Roubo de Determinação " +
            "1 e a Corrente de Efeitos – Rancor de Haloi: Este ataque recebe Oferenda " +
            "Maldita como um Efeito Crítico adicional. Inimigos que tenham sofrido danos " +
            "desta Habilidade se tornam imunes a ela por 2 Rodadas.",
            false, 2, 3, false, Optional.empty()),

    // Requer 2 Habilidades de 'Abraçado pela Escuridão' (unenforced). Same "Custo de Ativação:
    // Variável" nuance as SACRIFICIO_YMIRIANO — the PV cost equals Vigor's total, real via
    // #resolveVigorPvCost (shared with that constant); PDCost(2) is the separate, fixed PD
    // cost the rules text does state. The "aprimora uma quantidade de ataques igual à 1+
    // metade dos PV gastos" count is likewise real — see #resolveEnhancedAttackCountFromPvSpent
    // below, sharing its "1 + metade" formula shape with ESPINHOS_DE_GAEA's own Duração
    // (same arithmetic, different meaning — attack count here, not Rodadas). Everything the
    // spent PV/PD actually buys is fully TODO'd: the +1PA half hits the exact same
    // ActionPointsServiceImpl gap SantoAbility's own catalog TODO already cites
    // (ActionPointsServiceImpl#getMaxActionPoints never reads
    // CharacterSheet#getTemporaryBonus(ModifierType.ACTION_POINTS), only the plain
    // Character#getTemporaryActionPointsBonus() field, so a Round-scoped grant would be
    // silently inert); "Aprimoramento de Obra-Prima Alcance Estendido" needs the still-missing
    // Item/Equipamento entity (Raridade, Obra-Prima tiers, Aprimoramentos — the identical gap
    // ResourcesAdvantage#BARGANHISTA/HERANCA_FAMILIAR already cite); "empurrado 1UD... pode se
    // Reposicionar" needs a forced-movement/positioning mechanism this core has never modeled
    // (this core "never does geometry" — see Range's own javadoc); and the Corrente de
    // Efeitos + "+1d6" dano clause needs both the unbuilt Corrente de Efeitos system and this
    // core's "never rolls dice" boundary, the identical pair SantoSpecialization
    // #ABRACADO_PELA_ESCURIDAO's own Fúria dos Deuses Maior TODO cites. The locked-HP-pool
    // clause is the same gap as every other "Descansos ou Roubo de Vida" citation above.
    FUROR_DE_SYLPH(
            "Para ativar esta Habilidade você deve gastar uma quantidade de pontos de vida " +
            "igual ao seu Vigor. Você recebe Bônus de +1PA e seus ataques recebem o " +
            "Aprimoramento de Obra-Prima Alcance Estendido e a Corrente de Efeitos – Furor " +
            "de Sylph: O dano de seu ataque aumenta em +1d6 e seu alvo é empurrado 1UD para " +
            "trás e você pode se Reposicionar. O Furor de Sylph aprimora uma quantidade de " +
            "ataques igual à 1+ metade dos PV gastos com esta Habilidade, PV perdidos desta " +
            "forma só podem ser recuperados com Descansos ou Roubo de Vida.",
            true, 2, 0, false, Optional.empty()) {
        @Override
        public boolean isFreeActionActivation() {
            return true;
        }
    };

    private final String description;
    private final boolean supreme;
    private final int PDCost;
    private final int actionPointCost;
    private final boolean reactionActivation;
    private final Optional<Class<? extends Interaction>> interactionClass;

    /**
     * The "gastar uma quantidade de pontos de vida igual ao seu Vigor" cost shared by
     * {@code SACRIFICIO_YMIRIANO} and {@code FUROR_DE_SYLPH} — real, tested: Vigor's total is
     * already a plain value this core computes everywhere else
     * ({@code character.getAttributes().getVigor().getTotal()}), so this needs no missing
     * system of its own, even though what the spent PV actually buys is TODO'd on both
     * constants. Returns 0 for the other two constants, rather than throwing, so a caller
     * never needs to guard on which constant it's holding.
     */
    public int resolveVigorPvCost(final Character character) {
        if (this != SACRIFICIO_YMIRIANO && this != FUROR_DE_SYLPH) {
            return 0;
        }
        return character.getAttributes().getVigor().getTotal();
    }

    /**
     * Espinhos de Gaea's own "a Duração desta Habilidade é igual a 1+ metade dos PV gastos
     * (mínimo 1 Rodada)" — pure arithmetic over the player's own chosen PV amount, real and
     * tested. Returns 0 for every other constant.
     */
    public int resolveDurationFromPvSpent(final int pvSpent) {
        if (this != ESPINHOS_DE_GAEA) {
            return 0;
        }
        return onePlusHalfPvSpent(pvSpent);
    }

    /**
     * Furor de Sylph's own "aprimora uma quantidade de ataques igual à 1+ metade dos PV
     * gastos com esta Habilidade" — the identical "1 + metade" arithmetic shape as
     * {@link #resolveDurationFromPvSpent}, but a distinct method since it means something
     * different here (a count of enhanced attacks, not a Duração in Rodadas). Returns 0 for
     * every other constant.
     */
    public int resolveEnhancedAttackCountFromPvSpent(final int pvSpent) {
        if (this != FUROR_DE_SYLPH) {
            return 0;
        }
        return onePlusHalfPvSpent(pvSpent);
    }

    private static int onePlusHalfPvSpent(final int pvSpent) {
        return Math.max(1, 1 + pvSpent / 2);
    }
}
