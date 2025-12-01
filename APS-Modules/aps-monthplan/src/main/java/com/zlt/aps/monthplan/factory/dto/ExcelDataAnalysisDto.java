package com.zlt.aps.monthplan.factory.dto;

import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * excel导入数据解析辅助类
 *
 * @author ZLT
 * @date 20250926
 */
@Data
public class ExcelDataAnalysisDto {
    /**
     * excel导入的日志id
     */
    private Long importLogId;
    /**
     * 校验错误日志信息
     */
    private List<ImportErrorLog> importErrorLogs;
    /**
     * 数据处理失败计数器
     */
    private Integer failureNumber;
    /**
     * 配置缺失计数器
     */
    private Integer configureMissNumber;

    public ExcelDataAnalysisDto(Long importLogId) {
        this.importLogId = importLogId;
        this.importErrorLogs = new ArrayList<>();
        this.failureNumber = BigDecimal.ZERO.intValue();
        this.configureMissNumber = BigDecimal.ZERO.intValue();
    }
}
