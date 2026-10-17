package org.aventyrs.core.feat;

import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.character.MovementMode;
import lombok.Getter;
import lombok.NonNull;
import org.aventyrs.core.character.AttributeDomain;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.item.NaturalWeapon;
import org.aventyrs.core.scene.SceneContext;
import org.aventyrs.core.skill.AttackSource;
import org.aventyrs.core.skill.SkillRoll;
import org.aventyrs.core.skill.SkillTrait;
import org.aventyrs.core.skill.SkillType;

import java.util.List;
import java.util.Optional;

/**
 * The acquired, per-character form of a {@link BestialFeat} Herança, carrying the {@link
 * SkillTrait} the player chose — "Bestiais Bovídeos recebem uma Habilidade de Competência de
 * Atletismo", "Bestiais Reptilianos recebem uma Especialização adicional de 'Atletismo'". Grant
 * <em>this</em> in {@code Character#feats} in place of the bare enum constant, the same
 * catalog-vs-acquired split {@link ConselheiroDeGuerraYmirianoFeat} keeps for its Habilidade de
 * Força.
 *
 * <p><b>One class for the whole tree</b>, parameterised by which Herança it is, rather than six
 * near-identical ones: every Herança is built from the same four clauses (see {@link
 * BestialFeat}'s javadoc) and differs only in <i>which</i> Atributo, Arma Natural and Perícia it
 * names — all of which the constant itself already answers. This is the first choice-carrying
 * form to serve more than one constant; the earlier ones ({@link FocoEmPericiaFeat}, {@link
 * TerrenoPrediletoFeat}, …) each wrap exactly one because each is the only constant of its shape.
 *
 * <p><b>The delegation is explicit.</b> Replacing the constant in {@code getFeats()} would otherwise drop the
 * effects the constant itself overrides, so every hook a Herança overrides is forwarded by hand: the Atributo
 * ({@link #resolveAttributeBonus}), the Arma Natural ({@link #getGrantedNaturalWeapons}), the movement mode and Voo
 * ({@link #grantsMovementMode}, {@link #resolveFlightActivation}), the roll bonuses (Anfíbia's grapple Vantagem,
 * Canina's Faro Apurado — both {@code resolveSkillRollBonus} ends), Ecolocalização's GD ({@link
 * #resolveDifficultyReduction}) and the breathing pair ({@link #breathesUnderwater}, {@link
 * #resolveSubmergedBreathMultiplier}). {@code AbstractFeat} deliberately does <em>not</em> forward everything to
 * {@link #catalogEntry()} — a choice-carrying form replaces its constant rather than decorating it, and a blanket
 * forward would make it impossible to <i>drop</i> a clause. <b>Add a line here whenever a Herança overrides a new
 * hook</b> — {@code HerancaBestialFeatTest} fails on one that is missing. Ecolocalização's activation hooks
 * ({@code permitsActivation}, its PD cost) are asked of the activated <em>constant</em> a roll names, so they need
 * no forward.
 *
 * <p>Each Herança declares its picks through {@code Feat#resolveRequiredChoices} — one {@link SkillTrait} per kind
 * (core 0.1.7.23) — so the bare constant is refused by {@code FeatService#grantFeat} and this is the form every
 * holder carries. Canina owes two, an Especialização then a Habilidade de Competência.
 */
@Getter
public final class HerancaBestialFeat extends AbstractFeat {

    private final BestialFeat heranca;
    private final List<SkillTrait> chosenTraits;

    public HerancaBestialFeat(@NonNull final BestialFeat heranca, @NonNull final List<SkillTrait> chosenTraits) {
        super(heranca.getFeatCategory(), heranca.getDescription(), heranca.getFeatRequirements());
        if (chosenTraits.isEmpty()) {
            throw new IllegalArgumentException("A Herança grants at least one trait");
        }
        this.heranca = heranca;
        this.chosenTraits = List.copyOf(chosenTraits);
    }

    public HerancaBestialFeat(@NonNull final BestialFeat heranca, @NonNull final SkillTrait chosenTrait) {
        this(heranca, List.of(chosenTrait));
    }

    public static HerancaBestialFeat of(@NonNull final BestialFeat heranca,
                                        @NonNull final SkillTrait chosenTrait) {
        return new HerancaBestialFeat(heranca, chosenTrait);
    }

    /** The picks in {@code BestialFeat#resolveRequiredChoices} order — Canina's Especialização, then its Competência. */
    public static HerancaBestialFeat of(@NonNull final BestialFeat heranca,
                                        @NonNull final List<SkillTrait> chosenTraits) {
        return new HerancaBestialFeat(heranca, chosenTraits);
    }

    /** The first trait picked — the only one, for every Herança but Canina. */
    public SkillTrait getChosenTrait() {
        return chosenTraits.get(0);
    }

    /**
     * The first trait a character chose for heranca, if they hold that Herança in its acquired form —
     * mirrors {@link FocoEmPericiaFeat#chosenBy}. Takes the Herança because a Bestial routinely
     * holds several at once, each with its own pick.
     */
    public static Optional<SkillTrait> chosenBy(final Character character, final BestialFeat heranca) {
        return character.getFeats().stream()
                .filter(HerancaBestialFeat.class::isInstance)
                .map(HerancaBestialFeat.class::cast)
                .filter(held -> held.getHeranca() == heranca)
                .map(HerancaBestialFeat::getChosenTrait)
                .findFirst();
    }

    @Override
    public Feat catalogEntry() {
        return heranca;
    }

    /** "Recebem uma Habilidade de Competência / Especialização de &lt;Perícia&gt;." */
    @Override
    public List<SkillTrait> getGrantedSkillTraits(final Character character) {
        return chosenTraits;
    }

    /** Forwarded — "+1 de bônus racial em &lt;Atributo&gt;", fixed per Herança. */
    @Override
    public int resolveAttributeBonus(final AttributeDomain domain, final Character character) {
        return heranca.resolveAttributeBonus(domain, character);
    }

    /** Forwarded — the Arma Natural the Bovídea/Canina/Felina Heranças hand over. */
    @Override
    public List<NaturalWeapon> getGrantedNaturalWeapons(final Character character) {
        return heranca.getGrantedNaturalWeapons(character);
    }

    /** Forwarded — Anfíbia's Natação, Aviana's Voo, Reptiliana's Vertical. */
    @Override
    public boolean grantsMovementMode(final MovementMode mode, final Character character,
                                      final CombatantSheet holder) {
        return heranca.grantsMovementMode(mode, character, holder);
    }

    /** Forwarded — Aviana's Efeito de Voo. */
    @Override
    public java.util.Optional<org.aventyrs.core.character.FlightActivation> resolveFlightActivation(
            final Character character, final CombatantSheet holder) {
        return heranca.resolveFlightActivation(character, holder);
    }

    /** Forwarded — the shortest roll-bonus form, Canina's Faro Apurado (a preview path asks this one). */
    @Override
    public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                     final SkillTrait requestedAbility, final Character character) {
        return heranca.resolveSkillRollBonus(skillType, sceneContext, requestedAbility, character);
    }

    /** Forwarded — the longest roll-bonus form, Anfíbia's grapple Vantagem (it reads the roll's Manobra). */
    @Override
    public int resolveSkillRollBonus(final SkillType skillType, final SceneContext sceneContext,
                                     final SkillTrait requestedAbility, final Character character,
                                     final AttackSource attackSource, final CombatantSheet holder,
                                     final SkillRoll skillRoll) {
        return heranca.resolveSkillRollBonus(skillType, sceneContext, requestedAbility, character, attackSource,
                holder, skillRoll);
    }

    /** Forwarded — Cetácea's Ecolocalização, on the roll it was activated on. */
    @Override
    public int resolveDifficultyReduction(final SkillType skillType, final Character character,
                                          final SceneContext sceneContext, final SkillRoll skillRoll) {
        return heranca.resolveDifficultyReduction(skillType, character, sceneContext, skillRoll);
    }

    /** Forwarded — Anfíbia's breathing under water. */
    @Override
    public boolean breathesUnderwater(final Character character) {
        return heranca.breathesUnderwater(character);
    }

    /** Forwarded — Cetácea's doubled breath under water. */
    @Override
    public int resolveSubmergedBreathMultiplier(final Character character) {
        return heranca.resolveSubmergedBreathMultiplier(character);
    }
}
