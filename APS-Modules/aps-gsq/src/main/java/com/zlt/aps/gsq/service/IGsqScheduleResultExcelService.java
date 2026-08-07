package com.zlt.aps.gsq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.gsq.api.domain.dto.GsqScheduleResultImportDTO;
import com.zlt.aps.gsq.api.domain.entity.GsqScheduleResult;

/**
 * 钢丝圈排程结果模板导入导出服务。
 */
public interface IGsqScheduleResultExcelService {

    /**
     * 按专用模板导出钢丝圈排程结果。
     *
     * @param queryVO 查询条件，必须包含工厂和排程日期
     * @param fileName 导出文件名称
     * @return Excel 文件字节
     * @throws com.ruoyi.common.exception.ServiceException 模板缺失或查询条件不完整时抛出
     */
    byte[] exportDataScheduleResult(GsqScheduleResult queryVO, String fileName);

    /**
     * 按专用模板导入钢丝圈排程结果。
     *
     * @param importDTO 导入文件和业务条件
     * @param updateSupport 已存在记录是否更新
     * @return 导入结果和错误明细
     * @throws Exception 文件读取或导入日志处理失败时抛出
     */
    AjaxResult importDataScheduleResult(GsqScheduleResultImportDTO importDTO, boolean updateSupport) throws Exception;
}