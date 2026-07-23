package org.aventyrs.core.feat;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FeatCategory {

    ARTILHARIA(Type.GERAL), ASSASSINO(Type.GERAL), DESTINO(Type.GERAL), DUELISTA(Type.GERAL), ESCUDEIRO(Type.GERAL),
    METAMAGICO(Type.GERAL), MOBILIDADE(Type.GERAL), PERITO(Type.GERAL), SOBREVIVENCIA(Type.GERAL),
    MONSTRUOSO(Type.RACIAL), FERRICO(Type.RACIAL), ELFICO(Type.RACIAL), BESTIAL(Type.RACIAL);

    private final Type type;

    public enum Type{
        GERAL,RACIAL;
    }
}