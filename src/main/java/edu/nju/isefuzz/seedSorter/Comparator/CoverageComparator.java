package edu.nju.isefuzz.seedSorter.Comparator;

import edu.nju.isefuzz.model.Seed;

import java.util.Comparator;

/**
 * 覆盖块数比较器
 * @author 邱凯旋
 */
public class CoverageComparator implements Comparator<Seed> {
    @Override
    public int compare(Seed s1, Seed s2) {
        return Integer.compare(s2.getBlockCount(), s1.getBlockCount()); // 覆盖块数多的优先
    }
}
