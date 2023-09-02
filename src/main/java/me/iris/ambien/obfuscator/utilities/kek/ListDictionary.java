package me.iris.ambien.obfuscator.utilities.kek;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public abstract class ListDictionary implements IDictionary {
    private final int baseRepeatTime;
    private final List<String> used = new ArrayList<>();
    private final Random random = new Random();

    public ListDictionary(int baseRepeatTime) {
        this.baseRepeatTime = baseRepeatTime;
    }

    @Override
    public String get() {
        List<String> list = getList();
        String s;
        int ticks = 0;
        int repeatTime = baseRepeatTime;

        do {
            StringBuilder b = new StringBuilder();

            for (int i = 0; i < repeatTime; i++) {
                b.append(list.get(random.nextInt(list.size())));
            }

            s = b.toString();

            if (ticks == list.size()) {
                repeatTime++;
                ticks = 0;
            }

            ticks++;
        } while (used.contains(s));

        used.add(s);

        return s;
    }

    @Override
    public void reset() {
        used.clear();
    }

    public void addUsed(String s) {
        used.add(s);
    }

    protected abstract List<String> getList();
}

