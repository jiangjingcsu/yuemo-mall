package com.yuemo.server.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Slf4j
@Component
@RequiredArgsConstructor
public class DatabaseWarmupConfig {

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void warmUpDatabase() {
        log.info("========== 数据库连接池预热开始 ==========");
        long startTime = System.currentTimeMillis();

        try {
            int poolSize = 0;
            for (int i = 0; i < 5; i++) {
                try (Connection conn = dataSource.getConnection()) {
                    poolSize++;
                    log.debug("预热连接 {}: {}", i + 1, conn.getCatalog());
                }
            }

            String version = jdbcTemplate.queryForObject("SELECT VERSION()", String.class);

            long elapsed = System.currentTimeMillis() - startTime;
            log.info("========== 数据库连接池预热完成 ==========");
            log.info("MySQL 版本: {}", version);
            log.info("预热连接数: {}", poolSize);
            log.info("预热耗时: {} ms", elapsed);

        } catch (Exception e) {
            log.error("数据库预热失败", e);
        }
    }
}
