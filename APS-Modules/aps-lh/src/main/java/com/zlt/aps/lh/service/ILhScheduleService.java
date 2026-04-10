package com.zlt.aps.lh.service;

import com.zlt.aps.lh.api.domain.dto.LhScheduleRequestDTO;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResponseDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.api.domain.vo.LhScheduleShiftDateVO;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 硫化排程主服务接口
 * <p>排程入口，负责编排排程全流程</p>
 *
 * @author APS
 */
public interface ILhScheduleService extends IDocService<LhScheduleResult> {

    /**
     * 执行自动排程
     *
     * @param request 排程请求参数
     * @return 排程响应结果
     */
    LhScheduleResponseDTO executeSchedule(LhScheduleRequestDTO request);

    /**
     * 发布排程结果到MES
     *
     * @param batchNo 批次号
     * @return 发布响应结果
     */
    LhScheduleResponseDTO publishSchedule(String batchNo);

    /**
     * 根据排程结束日（yyyy-MM-dd）构建窗口内 8 个班次的日期展示列表
     *
     * @param scheduleDate 排程日期字符串，格式 yyyy-MM-dd；空或非法时返回空列表
     * @return 班次 1～8 与对应 MM/dd，顺序与默认 8 班模板日历日一致
     */
    List<LhScheduleShiftDateVO> listScheduleShiftDates(String scheduleDate);
}
