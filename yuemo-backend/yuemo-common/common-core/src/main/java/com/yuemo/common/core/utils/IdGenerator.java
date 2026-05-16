package com.yuemo.common.core.utils;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 分布式 ID 生成工具（简化版雪花算法 + 随机数降级）
 */
public final class IdGenerator {

    private IdGenerator() {}

    public static long nextId() {
        long timestamp = System.currentTimeMillis();
        long random = ThreadLocalRandom.current().nextLong(0, 1 << 12);
        return (timestamp << 12) | random;
    }
}
