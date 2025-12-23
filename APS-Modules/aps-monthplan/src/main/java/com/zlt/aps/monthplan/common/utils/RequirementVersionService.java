package com.zlt.aps.monthplan.common.utils;

import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

/**
 * 需求版本号服务接口
 * @author Yelq
 */
public interface RequirementVersionService {

  /**
   * 生成单个需求版本号
   * @return REQ+yyyymmdd+3位流水号
   */
  String generateVersion(String prefix);

  /**
   * 批量生成需求版本号
   * @param count 生成数量
   * @return 版本号列表
   */
  List<String> generateBatchVersions(String prefix,int count);

  /**
   * 获取当前日期已生成的版本号数量
   * @return 当前序列号
   */
  int getCurrentSequence();

  /**
   * 验证版本号格式是否有效
   * @param version 版本号
   * @return 是否有效
   */
  boolean validateVersionFormat(String version);

  /**
   * 解析版本号信息
   * @param version 版本号
   * @return 版本号详情
   */
  VersionInfo parseVersion(String version);

  /**
   * 版本号信息类
   */
  @Getter
  class VersionInfo {
    // Getters
    private final String prefix;
    private final LocalDate date;
    private final int sequence;
    private final String fullVersion;

    public VersionInfo(String prefix, LocalDate date, int sequence, String fullVersion) {
      this.prefix = prefix;
      this.date = date;
      this.sequence = sequence;
      this.fullVersion = fullVersion;
    }

  }
}
