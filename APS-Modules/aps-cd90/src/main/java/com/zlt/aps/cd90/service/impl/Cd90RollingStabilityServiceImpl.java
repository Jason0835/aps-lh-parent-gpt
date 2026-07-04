package com.zlt.aps.cd90.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zlt.aps.cd90.service.Cd90RollingStabilityService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RBucket;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/** 使用Redis保存跨节点输入版本首次观察时间。 */
@Service
@RequiredArgsConstructor
public class Cd90RollingStabilityServiceImpl implements Cd90RollingStabilityService {

    private static final int MIN_TTL_MINUTES = 180;

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    /** 在短锁内读取或更新观察状态，防止多个Job节点互相覆盖。 */
    @Override
    public boolean observe(String stateKey, String inputVersion, Instant observedAt,
                           int stableMinutes) {
        if (stateKey == null || inputVersion == null || observedAt == null) {
            return false;
        }
        String bucketKey = "aps:cd90:rolling-input:" + stateKey;
        RLock lock = redissonClient.getLock(bucketKey + ":lock");
        lock.lock();
        try {
            RBucket<String> bucket = redissonClient.getBucket(bucketKey);
            VersionState current = this.read(bucket.get());
            if (current == null || !Objects.equals(inputVersion, current.getInputVersion())) {
                this.write(bucket, new VersionState(inputVersion, observedAt.toString()),
                        stableMinutes);
                return stableMinutes <= 0;
            }
            Instant firstSeenTime = Instant.parse(current.getFirstSeenTime());
            return Duration.between(firstSeenTime, observedAt).toMinutes()
                    >= Math.max(0, stableMinutes);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /** 反序列化Redis状态，空值或旧格式按首次观察处理。 */
    private VersionState read(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, VersionState.class);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    /** 写入版本和首次观察时间，并确保状态覆盖完整滚动窗口。 */
    private void write(RBucket<String> bucket, VersionState state, int stableMinutes) {
        try {
            int ttlMinutes = Math.max(MIN_TTL_MINUTES, stableMinutes + 120);
            bucket.set(objectMapper.writeValueAsString(state), ttlMinutes, TimeUnit.MINUTES);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("滚动输入稳定状态序列化失败", exception);
        }
    }

    /** Redis中保存的输入版本观察状态。 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class VersionState {
        /** 输入版本。 */
        private String inputVersion;
        /** 首次观察时间。 */
        private String firstSeenTime;
    }
}
