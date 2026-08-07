package org.aventyrs.core.sheet;

import java.util.Base64;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Builder(toBuilder = true) @Getter
@AllArgsConstructor
@NoArgsConstructor
public class Player {
    protected Long id;
    protected String name;
    protected String login;
    protected Base64 password;
}
