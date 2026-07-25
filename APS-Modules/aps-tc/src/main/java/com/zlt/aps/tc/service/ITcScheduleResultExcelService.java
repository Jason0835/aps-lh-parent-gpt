package com.zlt.aps.tc.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tc.api.domain.dto.TcScheduleResultImportDTO;
import com.zlt.aps.tc.api.domain.entity.TcScheduleResult;

/**
 * 胎侧排程结果专用模板导入导出服务。
 */
public interface ITcScheduleResultExcelService {

    /**
     * 按专用模板导出胎侧排程结果。
     *
     * @param queryVO 查询条件，必须包含工厂和单个排程日期
     * @param fileName 导出文件名称
     * @return Excel 文件字节
     * @throws com.ruoyi.common.exception.ServiceException 条件或模板无效时抛出
     */
    byte[] exportDataScheduleResult(TcScheduleResult queryVO, String fileName);

    /**
     * 按专用模板导入胎侧排程结果。
     *
     * @param importDTO 导入文件和业务条件
     * @param updateSupport 是否允许覆盖更新
     * @return 导入结果及行级错误
     * @throws Exception 文件上传日志或解析失败时抛出
     */
    AjaxResult importDataScheduleResult(TcScheduleResultImportDTO importDTO, boolean updateSupport) throws Exception;
}
