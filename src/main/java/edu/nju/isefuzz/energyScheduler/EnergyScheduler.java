package edu.nju.isefuzz.energyScheduler;

import edu.nju.isefuzz.model.ExecutionResult;
import edu.nju.isefuzz.model.Seed;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * EnergyScheduler 类用于管理种子的能量。
 * 参考 AFL++ 的能量调度机制，确保初始种子具有非零能量，并合理分配和消耗能量。
 *
 * 作者：邱凯旋
 */
public class EnergyScheduler {
    private static final Logger logger = Logger.getLogger(EnergyScheduler.class.getName());

    // Map 用于存储每个种子的当前能量
    private Map<Seed, Integer> seedEnergyMap;

    // 种子的初始能量
    private final int INITIAL_ENERGY = 100;

    // 每次变异的能量消耗
    private final int ENERGY_PER_MUTATION = 10;

    // 每个种子能拥有的最大能量
    private final int MAX_ENERGY = 1000;

    // 能量恢复参数
    private final int ENERGY_RECOVERY_AMOUNT = 10; // 每次恢复的能量
    private final int RECOVERY_INTERVAL_MS = 60000; // 恢复间隔（毫秒）

    public EnergyScheduler() {
        seedEnergyMap = new ConcurrentHashMap<>();
    }

    /**
     * 添加种子到能量调度器，并分配初始能量。
     *
     * @param seed 要添加的种子
     */
    public void addSeed(Seed seed) {
        if (!seedEnergyMap.containsKey(seed)) {
            seedEnergyMap.put(seed, INITIAL_ENERGY);
            logger.info("Added seed with initial energy: " + seed);
        }
    }

    /**
     * 获取种子的当前能量。
     *
     * @param seed 要查询的种子
     * @return 当前能量，若种子不存在则返回0
     */
    public int getEnergy(Seed seed) {
        return seedEnergyMap.getOrDefault(seed, 0);
    }

    /**
     * 消耗种子的能量。
     *
     * @param seed   要消耗能量的种子
     * @param amount 消耗的能量量
     */
    public void consumeEnergy(Seed seed, int amount) {
        seedEnergyMap.computeIfPresent(seed, (s, energy) -> {
            int newEnergy = Math.max(energy - amount, 0);
            logger.fine("Consumed " + amount + " energy from seed: " + seed + ". Remaining energy: " + newEnergy);
            return newEnergy;
        });
    }

    /**
     * 恢复所有种子的能量，确保不超过最大能量限制。
     */
    public void recoverEnergy() {
        seedEnergyMap.forEach((seed, energy) -> {
            if (energy < MAX_ENERGY) {
                int recoveredEnergy = Math.min(energy + ENERGY_RECOVERY_AMOUNT, MAX_ENERGY);
                seedEnergyMap.put(seed, recoveredEnergy);
                logger.fine("Recovered energy for seed: " + seed + ". New energy: " + recoveredEnergy);
            }
        });
        logger.info("Energy recovery completed.");
    }

    /**
     * 根据种子的当前能量决定生成的变异种子数量。
     *
     * @param seed 要生成变异的种子
     * @return 可生成的变异种子数量
     */
    public int determineMutationCount(Seed seed) {
        int energy = getEnergy(seed);
        if (energy < ENERGY_PER_MUTATION) {
            return 0;
        }
        return energy / ENERGY_PER_MUTATION;
    }

    /**
     * 根据执行结果更新种子的能量。
     * 奖励发现新覆盖块，惩罚导致崩溃。
     *
     * @param seed       被执行的种子
     * @param execResult 执行结果
     */
    public void updateEnergy(Seed seed, ExecutionResult execResult) {
        if (execResult.isReachNewBlock()) {
            // 奖励发现新覆盖块
            int reward = 50; // 奖励能量量，可根据需要调整
            seedEnergyMap.merge(seed, reward, (oldEnergy, r) -> Math.min(oldEnergy + r, MAX_ENERGY));
            logger.info("Seed " + seed + " found new coverage. Rewarded " + reward + " energy. New energy: " + getEnergy(seed));
        }

        if (execResult.isHasFatal()) {
            // 惩罚导致崩溃
            int penalty = 20; // 惩罚能量量，可根据需要调整
            seedEnergyMap.computeIfPresent(seed, (s, energy) -> {
                int newEnergy = Math.max(energy - penalty, 0);
                logger.info("Seed " + seed + " caused a crash. Penalized " + penalty + " energy. New energy: " + newEnergy);
                return newEnergy;
            });
        }
    }

    /**
     * 定期恢复种子的能量，需在主循环或定时任务中调用。
     */
    public void startEnergyRecovery() {
        Thread recoveryThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(RECOVERY_INTERVAL_MS);
                    recoverEnergy();
                } catch (InterruptedException e) {
                    logger.severe("Energy recovery thread interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        recoveryThread.setDaemon(true);
        recoveryThread.start();
        logger.info("Started energy recovery thread.");
    }

    /**
     * 移除种子从能量调度器。
     *
     * @param seed 要移除的种子
     */
    public void removeSeed(Seed seed) {
        seedEnergyMap.remove(seed);
        logger.info("Removed seed from EnergyScheduler: " + seed);
    }
}