package com.zlt.aps.monthplan.common.utils;

import com.tlt.aps.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.concurrent.locks.ReentrantLock;

/**
 *  需求版本号服务实现
 * @author Yelq
 */
@Service
@Transactional
@RequiredArgsConstructor
public class RequirementVersionServiceImpl implements RequirementVersionService {

  private static final DateTimeFormatter DATE_FORMATTER =
      DateTimeFormatter.ofPattern("yyyyMMdd");
  private static final Pattern VERSION_PATTERN =
      Pattern.compile("^REQ\\d{8}\\d{3}$");

  private final ReentrantLock lock = new ReentrantLock();

  private final RedisSequenceStorageService sequenceService;

  @Override
  public String generateVersion(String prefix) {
    lock.lock();
    try {
      String dateStr = getTodayDate();
      long sequence = sequenceService.incrementAndGet(dateStr);

      validateSequence(sequence);

      // 记录每日生成计数
      sequenceService.incrementDailyCounter();

      return formatVersion(prefix,dateStr, sequence);
    } finally {
      lock.unlock();
    }
  }

  @Override
  public List<String> generateBatchVersions(String prefix,int count) {
    if (count <= 0 || count > 1000) {
      throw new BusinessException("生成数量必须在1到1000之间");
    }

    List<String> versions = new ArrayList<>();
    lock.lock();
    try {
      String dateStr = getTodayDate();

      for (int i = 0; i < count; i++) {
        long sequence = sequenceService.incrementAndGet(dateStr);
        validateSequence(sequence);
        versions.add(formatVersion(prefix,dateStr, sequence));
      }

      // 批量更新每日计数
      for (int i = 0; i < count; i++) {
        sequenceService.incrementDailyCounter();
      }

    } finally {
      lock.unlock();
    }

    return versions;
  }

  @Override
  public int getCurrentSequence() {
    String dateStr = getTodayDate();
    long sequence = sequenceService.getCurrentSequence(dateStr);
    return (int) sequence;
  }

  @Override
  public boolean validateVersionFormat(String version) {
    if (version == null) {
      return false;
    }
    return VERSION_PATTERN.matcher(version).matches();
  }

  @Override
  public VersionInfo parseVersion(String version) {
    if (!validateVersionFormat(version)) {
      throw new BusinessException("无效的版本号格式: " + version);
    }

    String prefix = version.substring(0, 3);
    String dateStr = version.substring(3, 11);
    String seqStr = version.substring(11);

    LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyyMMdd"));
    int sequence = Integer.parseInt(seqStr);

    return new VersionInfo(prefix, date, sequence, version);
  }

  private String formatVersion(String prefix,String dateStr, long sequence) {
    return String.format("%s%s%03d", prefix, dateStr, sequence);
  }

  private String getTodayDate() {
    return LocalDate.now().format(DATE_FORMATTER);
  }

  private void validateSequence(long sequence) {
    if (sequence > 999) {
      throw new BusinessException(
          String.format("当日序列号已超过最大值999，当前序列号: %d", sequence)
      );
    }
  }
}
