package edu.nju.isefuzz.mutator;

import edu.nju.isefuzz.model.Seed;

public interface Mutator {
    Seed mutate(Seed seed);
}
