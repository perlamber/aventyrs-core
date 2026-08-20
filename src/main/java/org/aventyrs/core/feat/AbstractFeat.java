package org.aventyrs.core.feat;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AbstractFeat implements Feat {

    private FeatCategory featCategory;
    private String description;
    private FeatRequirements featRequirements;

}
