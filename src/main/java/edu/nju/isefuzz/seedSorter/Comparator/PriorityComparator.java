package edu.nju.isefuzz.seedSorter.Comparator;

import edu.nju.isefuzz.model.Seed;

import java.util.Comparator;

public class PriorityComparator implements Comparator<Seed> {
    @Override
    public int compare(Seed s1, Seed s2) {
        return Double.compare(s2.getPriorityScore(), s1.getPriorityScore()); // 优先级高的优先
    }
}