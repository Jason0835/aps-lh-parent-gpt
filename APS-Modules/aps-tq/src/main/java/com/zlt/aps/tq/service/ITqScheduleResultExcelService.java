package com.zlt.aps.tq.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.tq.api.domain.dto.TqScheduleResultImportDTO;
import com.zlt.aps.tq.api.domain.entity.TqScheduleResult;

/**
 * 胎圈排程结果专用模板导入导出服务。
 *
 * @author APS
 */
public interface ITqScheduleResultExcelService {

    /**
     * 按专用模板导出胎圈排程结果。
     *
     * @param queryVO  查询条件，必须包含工厂和排程日期
     * @param fileName 导出文件名称
     * @return Excel 文件字节
     */
    byte[] exportDataScheduleResult(TqScheduleResult queryVO, String fileName);

    /**
     * 按专用模板导入胎圈排程结果。
     *
     * @param importDTO     导入文件和业务条件
     * @param updateSupport 已存在记录是否更新
     * @return 导入结果和错误明细
     * @throws Exception 导入日志或文件读取异常时抛出
     */
    AjaxResult importDataScheduleResult(TqScheduleResultImportDTO importDTO, boolean updateSupport) throws Exception;

    /**
     * 下载胎圈排程结果空白导入模板。
     *
     * <p>基于 {@code excelModel/tqScheduleResult.xlsx} 生成：填充标题日期与多语言表头，
     * 不含任何数据行，用户填写后通过导入接口上传。</p>
     *
     * @param queryVO 查询条件（用于填充模板标题日期）
     * @return 模板文件字节
     */
    byte[] downloadTemplate(TqScheduleResult queryVO);
}
