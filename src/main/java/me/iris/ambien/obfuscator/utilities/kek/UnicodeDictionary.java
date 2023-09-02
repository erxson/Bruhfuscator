package me.iris.ambien.obfuscator.utilities.kek;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class UnicodeDictionary extends ListDictionary {
    private static final List<String> arabic = new ArrayList<>();
    private static final List<String> unicode = new ArrayList<>();

    static {
        for (int i = 0x060C; i <= 0x06FE; i++) { // Arabic
            arabic.add(Character.toString((char) i));
        }

        unicode.addAll(Arrays.asList(
                "\u034C",
                "\u035C",
                "\u034E",
                "\u0344",
                "\u0306",
                "\u0307",
                "\u0321",
                "\u0331"
        ));
    }

    public UnicodeDictionary(int repeatTime) {
        super(repeatTime);
    }

    @Override
    public List<String> getList() {
        return unicode;
    }
}

