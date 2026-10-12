package org.aventyrs.core.item;

import java.util.List;
import org.aventyrs.core.ability.ActiveAbility;
import org.aventyrs.core.character.Character;
import org.aventyrs.core.modifier.ModifierType;
import org.aventyrs.core.sheet.ActionCost;
import org.aventyrs.core.sheet.CombatantSheet;
import org.aventyrs.core.sheet.TemporaryBonus;
import org.aventyrs.core.sheet.TemporaryEffect;
import org.aventyrs.core.skill.Skill;

/**
 * The Efeito Ativo three Artefatos de Conjuração share the shape of: "Com o Tempo de Ação de 1PA, o usuário pode
 * utilizar 2PM para … por 3 Rodadas (efeito não cumulativo). Apenas personagens capazes de Conjurar ou Mimetizar
 * magias podem ativar este efeito." Offered by {@code Character#getActiveAbilities()} while the artefact is drawn
 * ({@link CastingArtifacts#activeAbilities}) and activated through the ordinary {@code ActiveAbilityService}. The
 * grant is a sourced {@link TemporaryBonus}, so activating again renews rather than stacks; the artefact's Favor,
 * when its Requisitos are met, raises the figure.
 */
public enum ArtifactActiveAbility implements ActiveAbility {

    /** Adaga de Cerimônias: Dano Base of Ataques Físicos, Danos e Curas Mágicas +1 — +2 under its Favor. */
    ADAGA_DE_CERIMONIAS(CastingArtifactItem.ADAGA_DE_CERIMONIAS) {
        @Override
        public List<TemporaryEffect> resolveEffects(final Character character) {
            int bonus = CastingArtifacts.isEmpowered(character, getArtifact()) ? 2 : 1;
            return List.of(new TemporaryBonus(ModifierType.DAMAGE_BASE_INCREASE, bonus, DURATION, name()));
        }
    },

    /**
     * Bisarma Canalizadora: Vantagem on attacks and casting. TODO: its Favor's Alcance Estendido while active has no
     * equipment range pass.
     */
    BISARMA_CANALIZADORA(CastingArtifactItem.BISARMA_CANALIZADORA) {
        @Override
        public List<TemporaryEffect> resolveEffects(final Character character) {
            return List.of(
                    new TemporaryBonus(ModifierType.ATAQUE_CORPO_A_CORPO_ROLL_BONUS, Skill.ADVANTAGE_BONUS, DURATION, name()),
                    new TemporaryBonus(ModifierType.ATAQUE_A_DISTANCIA_ROLL_BONUS, Skill.ADVANTAGE_BONUS, DURATION, name()),
                    new TemporaryBonus(ModifierType.DOMINIO_DO_MANA_ROLL_BONUS, Skill.ADVANTAGE_BONUS, DURATION, name()));
        }
    },

    /** Gládio Canalizador: Margem Crítica Menor of attacks and Magias +2 — +3 under its Favor. */
    GLADIO_CANALIZADOR(CastingArtifactItem.GLADIO_CANALIZADOR) {
        @Override
        public List<TemporaryEffect> resolveEffects(final Character character) {
            int bonus = CastingArtifacts.isEmpowered(character, getArtifact()) ? 3 : 2;
            return List.of(new TemporaryBonus(ModifierType.LESSER_CRITICAL_MARGIN, bonus, DURATION, name()));
        }
    };

    /** "por 3 Rodadas". */
    public static final int DURATION = 3;

    /** "utilizar 2PM". */
    public static final int MAGIC_POINT_COST = 2;

    private static final ActionCost ACTION_POINT_COST = ActionCost.ofActionPoints(1);

    private final CastingArtifactItem artifact;

    ArtifactActiveAbility(final CastingArtifactItem artifact) {
        this.artifact = artifact;
    }

    public CastingArtifactItem getArtifact() {
        return artifact;
    }

    @Override
    public String getDescription() {
        return artifact.getFavor().getAdditionalEffects();
    }

    @Override
    public ActionCost getActionPointCost() {
        return ACTION_POINT_COST;
    }

    @Override
    public int getMagicPointCost() {
        return MAGIC_POINT_COST;
    }

    @Override
    public int getDurationInRounds() {
        return DURATION;
    }

    @Override
    public TemporaryEffect resolveEffect(final Character character) {
        return resolveEffects(character).get(0);
    }

    /** "Apenas personagens capazes de Conjurar ou Mimetizar magias", with the artefact still drawn. */
    @Override
    public boolean isUsableBy(final CombatantSheet activator) {
        Character character = activator.getCharacter();
        return CastingArtifacts.isDrawn(character, artifact)
                && (!character.getCastableSpells().isEmpty() || !character.getMimetizedSpells().isEmpty());
    }
}
