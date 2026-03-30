package com.zlt.aps.lh.service;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResultUpdateDTO;
import com.zlt.aps.lh.api.domain.entity.LhCxLinkageConfirm;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.bill.common.service.IDocService;

import java.util.List;

/**
 * 硫化排程调整服务
 * @author pancd
 * @version 1.0
 * @Description
 * @date 2025/2/18
 */
public interface LhScheduleAdjustService extends IDocService<LhCxLinkageConfirm> {

    /**
     * 生成硫化排程调整信息
     * @param lhScheduleResult
     * @return
     */
    public AjaxResult generateLhScheduleAdjust(LhScheduleResult lhScheduleResult);


    /**
     * 发布后的硫化计划调量，生成联动成型记录
     */
    void preAdjustment(LhScheduleResultUpdateDTO updateResultDto);

    /**
     * 确认联动调整记录
     */
    AjaxResult confirmAdjust(LhCxLinkageConfirm lhCxLinkageConfirm);

    /**
     * 列表查询
     */
    List<LhCxLinkageConfirm> selectList(LhCxLinkageConfirm entity);
}
