package org.aventyrs.core.character;

import java.lang.reflect.Method;

public class KeyAttributeMethods
{
    public static Method getVigor;
    public static Method getStrength;
    public static Method getDexterity;
    public static Method getFocus;
    public static Method getInstinct;
    public static Method getGnose;
    public static Method getCharisma;
    static
    {
        try {
            getVigor = CharacterAttributes.class.getMethod("getVigor");
            getStrength = CharacterAttributes.class.getMethod("getStrength");
            getDexterity = CharacterAttributes.class.getMethod("getDexterity");
            getFocus = CharacterAttributes.class.getMethod("getFocus");
            getInstinct = CharacterAttributes.class.getMethod("getInstinct");
            getGnose = CharacterAttributes.class.getMethod("getGnose");
            getCharisma = CharacterAttributes.class.getMethod("getCharisma");
        }catch (NoSuchMethodException e)
        {
            e.printStackTrace();
        }
    }
}
