package com.zlt.aps.cx.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.vo.CxScheduleResultTemplateImportVO;
import com.zlt.bill.common.service.IDocService;

import java.time.LocalDate;
import java.util.Date;
import java.util.List;

/**
 * 成型排程结果服务接口
 *
 * @author APS Team
 */
public interface CxScheduleResultService extends IDocService<CxScheduleResult> {

    /**
     * 根据排程日期查询排程结果
     *
     * @param scheduleDate 排程日期
     * @return 排程结果列表
     */
    List<CxScheduleResult> listByScheduleDate(LocalDate scheduleDate);

    /**
     * 基于模板导出数据
     *
     * @param list         排程结果列表
     * @param scheduleDate 排程日期
     * @return 模板 Excel 字节数据
     */
    byte[] exportData(List<CxScheduleResult> list, Date scheduleDate);

    /**
     * 导出成型余量数据。
     *
     * @param queryVO 查询条件，按成型排程结果列表查询口径筛选数据
     * @param fileName 导出文件名
     * @return 成型余量Excel文件字节数组
     */
    byte[] exportCxRemainQty(CxScheduleResult queryVO, String fileName);

    /**
     * 导出成型结构切换数据。
     *
     * @param queryVO 查询条件，按成型排程结果列表查询口径筛选数据
     * @param fileName 导出文件名，保留用于对齐远程调用契约
     * @return 成型结构切换Excel文件字节数组
     */
    byte[] exportStructureChange(CxScheduleResult queryVO, String fileName);

    /**
     * 基于模板导入数据
     *
     * @param list          模板导入VO列表
     * @param result        查询参数（含工厂/日期）
     * @param updateSupport 已存在记录是否更新
     * @param logId         导入日志ID
     * @return 导入结果
     */
    AjaxResult importScheduleTemplate(List<CxScheduleResultTemplateImportVO> list,
                                      CxScheduleResult result, boolean updateSupport, Long logId);
}
