package me.iris.ambien.obfuscator.utilities.kek;

public interface IDictionary {
    String get();

    void reset();

    static IDictionary newDictionary() {
        return new UnicodeDictionary(10);
    }
}
