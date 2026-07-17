package com.zlt.aps.tm.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tm.api.domain.dto.TmScheduleResultImportDTO;
import com.zlt.aps.tm.api.domain.entity.TmScheduleResult;

/**
 * 胎面排程结果模板导入导出服务。
 */
public interface ITmScheduleResultExcelService {

    /**
     * 按专用模板导出胎面排程结果。
     *
     * @param queryVO 查询条件，必须包含工厂和排程日期
     * @param fileName 导出文件名称
     * @return Excel 文件字节
     * @throws com.ruoyi.common.exception.ServiceException 模板缺失或查询条件不完整时抛出
     */
    byte[] exportDataScheduleResult(TmScheduleResult queryVO, String fileName);

    /**
     * 按专用模板导入胎面排程结果。
     *
     * @param importDTO 导入文件和业务条件
     * @param updateSupport 已存在记录是否更新
     * @return 导入结果和错误明细
     * @throws Exception 文件读取或导入日志处理失败时抛出
     */
    AjaxResult importDataScheduleResult(TmScheduleResultImportDTO importDTO, boolean updateSupport) throws Exception;
}
