package org.aventyrs.core.util;

import java.security.SecureRandom;

public class Dice {

    /**
     * @param numberOfDices
     * @return Integer - total value of the dices
     */
    public static Integer rollDices(int numberOfDices)
    {
        SecureRandom rand = new SecureRandom();
        // Generate random integers in range 0 to 999
        Integer randInt = 0;
        for (int i=0;i < numberOfDices; i++)
        {
            randInt += rand.nextInt(6)+1;
        }
        return randInt;
    }

    /**
     *  Rolls the default ammount of diceswhich is 3
     * @return teh total value of rolls
     */
    public static Integer rollThreeDices()
    {
        return rollDices(3);
    }
}
