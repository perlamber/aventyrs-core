package org.aventyrs.core.race;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.Blessing;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.TargetScope;
import org.aventyrs.core.skill.SkillCompetencyAbility;
import org.aventyrs.core.skill.SkillType;

import java.util.List;

/**
 * Habilidades Raciais granted to every Troll — see {@link Race#getRacialAbilities()} for why
 * these are modeled as ordinary {@link SkillCompetencyAbility} instances rather than a separate
 * type. Authored from {@code docs/rules/racas.txt}'s "Características Raciais" block.
 *
 * <p><b>Only Regeneração Reativa lives here.</b> The other four Características in that block are
 * not absent by oversight: Anatomia Vegetal's two implemented halves already have hooks of their
 * own on {@link Race} ({@link Race#getCriticalResistance()} and {@link
 * Race#getCriticalEffectImmunities()}) because neither is a per-roll contribution — an immunity
 * set is not a number to sum — while Sono de Pedra, Crescimento Constante and Visão no Escuro are
 * each blocked on a system this core does not have (sleep, age, senses). A constant carrying
 * nothing would be a catalog entry pretending to be a mechanism.
 */
@Getter
@AllArgsConstructor
public enum TrollsRacialAbility implements SkillCompetencyAbility {

    /**
     * "Após sofrer danos de fontes inimigas recuperam 2PV por Rodada (em seus Turnos) por uma
     * quantidade de Rodadas igual ao próprio Vigor. A quantidade de PV recuperados desta forma não
     * pode superar os danos sofridos (Efeito não cumulativo)."
     *
     * <p><b>The trigger is stated here, on the ability.</b> {@link
     * SkillCompetencyAbility#resolveDamageTakenBlessings} is what "após sofrer danos de fontes
     * inimigas" means, and this constant answers it with a whole {@link Blessing} — the figure, the
     * Rodadas (the holder's own Vigor), the cap ("não pode superar os danos sofridos", the damage
     * that triggered it) and the source. {@code DamageService#notifyDamageTaken} only applies what
     * it is given, so a second damage-triggered clause needs no change there.
     *
     * <p>"Efeito não cumulativo" needs no expression of its own: a {@link Blessing} with a source
     * already refuses to stack with another from the same source, replacing it and renewing the
     * duration. {@code TrollFeat#REGENERACAO_REATIVA_SUPERIOR} lifts that ceiling, and <b>this
     * constant resolves it</b> — see {@link #simultaneousCeilingFor} for why the buff is read here
     * rather than by whatever applies the Blessing.
     *
     * <p><b>2PV, and this constant is now the single source of that figure.</b> {@code
     * docs/rules/talentos.txt} prints 1PV where {@code FeralFeat#BENCAO_DE_MAPINGUARI} quotes this
     * Característica, but that Talento's own instruction is "adquire a Habilidade Racial
     * Regeneração Reativa (Trolls)" — it hands over <em>this</em> ability, through {@code
     * Feat#getGrantedSkillTraits}, so its holder regenerates at the figure the ability states. The
     * quoted 1PV is treated as a transcription divergence in the Talentos document rather than a
     * second, weaker version of the same trait.
     */
    REGENERACAO_REATIVA("Após sofrer danos de fontes inimigas recuperam 2PV por Rodada (em seus "
            + "Turnos) por uma quantidade de Rodadas igual ao próprio Vigor. A quantidade de PV "
            + "recuperados desta forma não pode superar os danos sofridos (Efeito não cumulativo).") {
        @Override
        public List<Blessing> resolveDamageTakenBlessings(final CombatantSheet holder, final int finalDamage) {
            Character character = holder.getCharacter();
            return List.of(new Blessing(ModifierType.REGENERATION,
                    REGENERATION_PER_ROUND,
                    character.getEffectiveAttributeTotal(AttributeDomain.VIGOR, holder),
                    TargetScope.SELF,
                    name(),
                    finalDamage,
                    simultaneousCeilingFor(character)));
        }
    };

    /** Regeneração Reativa's own stated "recuperam 2PV por Rodada". */
    private static final int REGENERATION_PER_ROUND = 2;

    /**
     * How many Regeneração Reativa effects character may carry at once — the Característica's own
     * "Efeito não cumulativo" floor of one, raised by whichever held Talento claims the highest
     * ({@code TrollFeat#REGENERACAO_REATIVA_SUPERIOR}'s "se tornam cumulativos, podendo somar uma
     * quantidade de efeitos simultâneos igual 1+ número de Títulos Aventyr Despertos", the only
     * claimant today).
     *
     * <p><b>The ability asks, not whoever consumes the trigger.</b> {@code
     * Feat#resolveSimultaneousRegenerationLimit} names one Característica, so only the trait it
     * buffs may read it — a generic damage-taken consumer scanning it would hand a
     * regeneration-specific figure to every unrelated Blessing it happened to grant in the same
     * pass. Resolving it here also makes the {@link Blessing} truthful to anyone inspecting it,
     * rather than merely correct once consumed. Same shape as {@code
     * PoderVampiricoActiveAbility#durationFor}, where a separate Talento lengthens an effect and
     * the granting ability is what asks for it; this is the first {@link SkillCompetencyAbility}
     * to do it.
     *
     * <p><b>The highest claim wins, not the sum</b> — two such clauses would each state a whole
     * ceiling rather than a step.
     *
     * <p><b>Resolved at grant time</b>, and baked into each {@code
     * org.aventyrs.core.sheet.Regeneration}: {@code CombatantSheet#applyEffect} trims using the
     * incoming effect's own ceiling, so acquiring a Título widens it from the next hit onward and
     * never restores an instance already displaced.
     */
    private static int simultaneousCeilingFor(final Character character) {
        return Math.max(Blessing.DEFAULT_MAXIMUM_SIMULTANEOUS, character.getFeats().stream()
                .mapToInt(feat -> feat.resolveSimultaneousRegenerationLimit(character))
                .max().orElse(0));
    }

    private final String description;

    /**
     * A representative value only — Regeneração Reativa is scoped to no Perícia, and {@code
     * DamageService#notifyDamageTaken} applies no per-{@code SkillType} filter when it scans
     * {@link SkillCompetencyAbility#allFor}. Same "the constant has to report something"
     * situation as {@code GuamposRacialAbility#VIGOR_DE_EPONA}'s and {@code
     * HomensFeraRacialAbility}'s own enum-level defaults; Medicina e Cura is picked because
     * recovering PV is the one thing this ruleset's own healing clauses cluster around.
     */
    @Override
    public SkillType getSkillType() {
        return SkillType.MEDICINA_E_CURA;
    }
}
