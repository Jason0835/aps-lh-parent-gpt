package com.zlt.aps.monthplan.common.utils;

import com.ruoyi.common.core.web.domain.BaseEntity;
import com.zlt.core.dao.basedao.BaseDao;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * 大数据分批次批量插入处理器
 * 解决MySQL死锁问题
 * @author Administrator
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class BatchInsertProcessor<T extends BaseEntity> {


  // 默认配置
  private static final int DEFAULT_BATCH_SIZE = 1000;
  private static final int DEFAULT_MAX_RETRY_TIMES = 3;
  private static final long DEFAULT_RETRY_INTERVAL_MS = 1000;

  // 配置参数
  private int batchSize = DEFAULT_BATCH_SIZE;
  private int maxRetryTimes = DEFAULT_MAX_RETRY_TIMES;
  private long retryIntervalMs = DEFAULT_RETRY_INTERVAL_MS;
  private boolean enableParallel = false;
  private boolean useTransactional = true;

  private final BaseDao baseDao;
  // 自定义线程池，避免使用默认的ForkJoinPool
  private final Executor batchInsertExecutor;


  /**
   * 批量插入主方法
   */
  public BatchInsertResult batchInsert(List<T> dataList) {
    return batchInsert(dataList, null);
  }

  /**
   * 批量插入主方法（带进度回调）
   */
  public BatchInsertResult batchInsert(List<T> dataList,
                                       Consumer<BatchInsertProgress> progressCallback) {

    if (CollectionUtils.isEmpty(dataList)) {
      log.warn("批量插入数据为空");
      return new BatchInsertResult(0, 0, 0);
    }

    // 数据分片
    List<List<T>> batches = partitionList(dataList, batchSize);
    int totalBatches = batches.size();

    log.info("开始批量插入，总数据量: {}, 批次大小: {}, 总批次数: {}",
        dataList.size(), batchSize, totalBatches);

    BatchInsertResult result;

    if (enableParallel && totalBatches > 1) {
      result = batchInsertParallel(batches, progressCallback);
    } else {
      result = batchInsertSequential(batches, progressCallback);
    }

    log.info("批量插入完成，成功: {}, 失败: {}, 重试: {}",
        result.getSuccessCount(),
        result.getFailureCount(),
        result.getRetryCount());

    return result;
  }

  /**
   * 顺序批量插入
   */
  private BatchInsertResult batchInsertSequential(List<List<T>> batches,
                                                  Consumer<BatchInsertProgress> progressCallback) {

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);
    AtomicInteger retryCount = new AtomicInteger(0);
    int totalBatches = batches.size();

    for (int i = 0; i < totalBatches; i++) {
      List<T> batch = batches.get(i);

      try {
        // 执行插入
        executeBatchInsert(batch, successCount, failureCount, retryCount);

        // 进度回调
        if (progressCallback != null) {
          BatchInsertProgress progress = new BatchInsertProgress(
              i + 1, totalBatches,
              successCount.get(), failureCount.get()
          );
          progressCallback.accept(progress);
        }

        // 批次间短暂休眠，避免死锁
        if (i < totalBatches - 1) {
          Thread.sleep(10);
        }

      } catch (Exception e) {
        log.error("第 {} 批次插入异常", i + 1, e);
        failureCount.addAndGet(batch.size());
      }
    }

    return new BatchInsertResult(
        successCount.get(),
        failureCount.get(),
        retryCount.get()
    );
  }

  /**
   * 并行批量插入
   */
  private BatchInsertResult batchInsertParallel(List<List<T>> batches,
                                                Consumer<BatchInsertProgress> progressCallback) {

    AtomicInteger successCount = new AtomicInteger(0);
    AtomicInteger failureCount = new AtomicInteger(0);
    AtomicInteger retryCount = new AtomicInteger(0);
    AtomicInteger completedBatches = new AtomicInteger(0);
    int totalBatches = batches.size();

    List<CompletableFuture<Void>> futures = new ArrayList<>();

    for (int i = 0; i < totalBatches; i++) {
      List<T> batch = batches.get(i);
      final int batchIndex = i;

      CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
        try {
          executeBatchInsert(batch, successCount, failureCount, retryCount);

          int completed = completedBatches.incrementAndGet();
          if (progressCallback != null) {
            BatchInsertProgress progress = new BatchInsertProgress(
                completed, totalBatches,
                successCount.get(), failureCount.get()
            );
            progressCallback.accept(progress);
          }

        } catch (Exception e) {
          log.error("并行批次 {} 插入异常", batchIndex, e);
          failureCount.addAndGet(batch.size());
        }
      }, batchInsertExecutor);

      futures.add(future);
    }

    // 等待所有任务完成
    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

    return new BatchInsertResult(
        successCount.get(),
        failureCount.get(),
        retryCount.get()
    );
  }

  /**
   * 执行批次插入（带重试机制）
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW,
      rollbackFor = Exception.class)
  public void executeBatchInsert(List<T> batch,
                                 AtomicInteger successCount,
                                 AtomicInteger failureCount,
                                 AtomicInteger retryCount) {

    if (!useTransactional) {
      insertWithRetry(batch, successCount, failureCount, retryCount);
      return;
    }

    // 事务方式插入
    try {
      baseDao.insertBatch(batch);
      successCount.addAndGet(batch.size());
      log.debug("批次插入成功，数据量: {}", batch.size());
    } catch (DataAccessException e) {
      // 死锁等异常，触发重试
      if (isDeadlockException(e)) {
        log.warn("检测到死锁异常，触发重试机制");
        insertWithRetry(batch, successCount, failureCount, retryCount);
      } else {
        log.error("批次插入失败", e);
        failureCount.addAndGet(batch.size());
        throw e;
      }
    }
  }

  /**
   * 带重试机制的插入
   */
  private void insertWithRetry(List<T> batch,
                               AtomicInteger successCount,
                               AtomicInteger failureCount,
                               AtomicInteger retryCount) {

    int retry = 0;
    boolean success = false;

    while (retry <= maxRetryTimes && !success) {
      try {
        if (retry > 0) {
          log.info("第 {} 次重试插入，批次大小: {}", retry, batch.size());
          retryCount.incrementAndGet();
          // 指数退避
          Thread.sleep(retryIntervalMs * retry);
        }

        baseDao.insertBatch(batch);
        successCount.addAndGet(batch.size());
        success = true;

      } catch (Exception e) {
        retry++;
        if (retry > maxRetryTimes) {
          log.error("批次插入重试 {} 次后失败", maxRetryTimes, e);
          failureCount.addAndGet(batch.size());

          // 可选的降级策略：单条插入
          if (batch.size() > 1) {
            log.info("尝试降级为单条插入");
            insertOneByOne(batch, successCount, failureCount);
          }
        }
      }
    }
  }

  /**
   * 降级策略：单条插入
   */
  private void insertOneByOne(List<T> batch,
                              AtomicInteger successCount,
                              AtomicInteger failureCount) {
    for (BaseEntity item : batch) {
      try {
        baseDao.insert(item);
        successCount.incrementAndGet();
      } catch (Exception e) {
        log.error("单条插入失败", e);
        failureCount.incrementAndGet();
      }
    }
  }

  /**
   * 判断是否为死锁异常
   */
  private boolean isDeadlockException(DataAccessException e) {
    String message = e.getMessage();
    return message != null && (
        message.contains("deadlock") ||
            message.contains("Deadlock") ||
            message.contains("Lock wait timeout") ||
            // MySQL死锁错误码
            message.contains("1213") ||
            // MySQL锁等待超时错误码
            message.contains("1205")
    );
  }

  /**
   * 列表分片
   */
  private <E> List<List<E>> partitionList(List<E> list, int batchSize) {
    List<List<E>> batches = new ArrayList<>();
    int total = list.size();

    for (int i = 0; i < total; i += batchSize) {
      int end = Math.min(total, i + batchSize);
      batches.add(new ArrayList<>(list.subList(i, end)));
    }

    return batches;
  }


  // Builder模式配置参数
  public BatchInsertProcessor<T> batchSize(int batchSize) {
    this.batchSize = batchSize;
    return this;
  }

  public BatchInsertProcessor<T> maxRetryTimes(int maxRetryTimes) {
    this.maxRetryTimes = maxRetryTimes;
    return this;
  }

  public BatchInsertProcessor<T> retryIntervalMs(long retryIntervalMs) {
    this.retryIntervalMs = retryIntervalMs;
    return this;
  }

  public BatchInsertProcessor<T>  enableParallel(boolean enableParallel) {
    this.enableParallel = enableParallel;
    return this;
  }

  public BatchInsertProcessor<T>  maxConcurrentThreads(int maxConcurrentThreads) {
    return this;
  }

  public BatchInsertProcessor<T>  useTransactional(boolean useTransactional) {
    this.useTransactional = useTransactional;
    return this;
  }

  /**
   * 批量插入结果
   */
  @Getter
  public static class BatchInsertResult {
    private final int successCount;
    private final int failureCount;
    private final int retryCount;

    public BatchInsertResult(int successCount, int failureCount, int retryCount) {
      this.successCount = successCount;
      this.failureCount = failureCount;
      this.retryCount = retryCount;
    }
  }

  /**
   * 批量插入进度
   */
  @Getter
  public static class BatchInsertProgress {
    private final int currentBatch;
    private final int totalBatches;
    private final int successCount;
    private final int failureCount;

    public BatchInsertProgress(int currentBatch, int totalBatches,
                               int successCount, int failureCount) {
      this.currentBatch = currentBatch;
      this.totalBatches = totalBatches;
      this.successCount = successCount;
      this.failureCount = failureCount;
    }

    public double getProgress() {
      return (double) currentBatch / totalBatches * 100;
    }

    // getters...
  }
}
