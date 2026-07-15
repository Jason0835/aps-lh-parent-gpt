package com.zlt.aps.cd15.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zlt.aps.cd15.service.Cd15RollingStabilityService;
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

/** 使用Redis保存CD15定时滚动输入版本首次观察时间。 */
@Service
@RequiredArgsConstructor
public class Cd15RollingStabilityServiceImpl implements Cd15RollingStabilityService {

    private static final int MIN_TTL_MINUTES = 180;

    private final RedissonClient redissonClient;
    private final ObjectMapper objectMapper;

    @Override
    public boolean observe(String stateKey, String inputVersion, Instant observedAt,
                           int stableMinutes) {
        if (stateKey == null || inputVersion == null || observedAt == null) {
            return false;
        }
        String bucketKey = "aps:cd15:rolling-input:" + stateKey;
        RLock lock = redissonClient.getLock(bucketKey + ":lock");
        lock.lock();
        try {
            RBucket<String> bucket = redissonClient.getBucket(bucketKey);
            VersionState current = this.read(bucket.get());
            if (current == null || !Objects.equals(inputVersion, current.getInputVersion())) {
                this.write(bucket, new VersionState(inputVersion, observedAt.toString()), stableMinutes);
                return stableMinutes <= 0;
            }
            Instant firstSeenTime = Instant.parse(current.getFirstSeenTime());
            return Duration.between(firstSeenTime, observedAt).toMinutes() >= Math.max(0, stableMinutes);
        } finally {
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

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

    private void write(RBucket<String> bucket, VersionState state, int stableMinutes) {
        try {
            int ttlMinutes = Math.max(MIN_TTL_MINUTES, stableMinutes + 120);
            bucket.set(objectMapper.writeValueAsString(state), ttlMinutes, TimeUnit.MINUTES);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("CD15滚动输入稳定状态序列化失败", exception);
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