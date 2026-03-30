package com.zlt.aps.lh.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.zlt.aps.lh.api.domain.entity.MesLhScheduleResult;
import com.zlt.aps.lh.mapper.MesLhScheduleResultEntityMapper;
import com.zlt.aps.lh.service.IMesLhScheduleResultService;
import com.zlt.common.utils.PubUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MesLhScheduleResultServiceImpl.java
 * 描    述：MesLhScheduleResultServiceImpl硫化排程下发接口业务层处理
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-03-18
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MesLhScheduleResultServiceImpl implements IMesLhScheduleResultService {

    private final MesLhScheduleResultEntityMapper entityMapper;

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    protected void builderCondition(QueryWrapper<MesLhScheduleResult> queryWrapper, MesLhScheduleResult queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("batchNo")), "BATCH_NO", queryVO.getFieldValueByFieldName("batchNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("orderNo")), "ORDER_NO", queryVO.getFieldValueByFieldName("orderNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("monthPlanNo")), "MONTH_PLAN_NO", queryVO.getFieldValueByFieldName("monthPlanNo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lhMachineCode")), "LH_MACHINE_CODE", queryVO.getFieldValueByFieldName("lhMachineCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("leftRightMold")), "LEFT_RIGHT_MOLD", queryVO.getFieldValueByFieldName("leftRightMold"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lhMachineName")), "LH_MACHINE_NAME", queryVO.getFieldValueByFieldName("lhMachineName"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCode")), "PRODUCT_CODE", queryVO.getFieldValueByFieldName("productCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specCode")), "SPEC_CODE", queryVO.getFieldValueByFieldName("specCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoCode")), "EMBRYO_CODE", queryVO.getFieldValueByFieldName("embryoCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoStock")), "EMBRYO_STOCK", queryVO.getFieldValueByFieldName("embryoStock"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specDesc")), "SPEC_DESC", queryVO.getFieldValueByFieldName("specDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("lhTime")), "LH_TIME", queryVO.getFieldValueByFieldName("lhTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dailyPlanQty")), "DAILY_PLAN_QTY", queryVO.getFieldValueByFieldName("dailyPlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mpMoldQty")), "MP_MOLD_QTY", queryVO.getFieldValueByFieldName("mpMoldQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("moldQty")), "MOLD_QTY", queryVO.getFieldValueByFieldName("moldQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("singleMoldShiftQty")), "SINGLE_MOLD_SHIFT_QTY", queryVO.getFieldValueByFieldName("singleMoldShiftQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("moldInfo")), "MOLD_INFO", queryVO.getFieldValueByFieldName("moldInfo"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("mouldMethod")), "MOULD_METHOD", queryVO.getFieldValueByFieldName("mouldMethod"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("bomVersion")), "BOM_VERSION", queryVO.getFieldValueByFieldName("bomVersion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("scheduleDate")), "SCHEDULE_DATE", queryVO.getFieldValueByFieldName("scheduleDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productionStatus")), "PRODUCTION_STATUS", queryVO.getFieldValueByFieldName("productionStatus"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class1PlanQty")), "CLASS1_PLAN_QTY", queryVO.getFieldValueByFieldName("class1PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class1StartTime")), "CLASS1_START_TIME", queryVO.getFieldValueByFieldName("class1StartTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class1EndTime")), "CLASS1_END_TIME", queryVO.getFieldValueByFieldName("class1EndTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class1Analysis")), "CLASS1_ANALYSIS", queryVO.getFieldValueByFieldName("class1Analysis"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class1FinishQty")), "CLASS1_FINISH_QTY", queryVO.getFieldValueByFieldName("class1FinishQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class2PlanQty")), "CLASS2_PLAN_QTY", queryVO.getFieldValueByFieldName("class2PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class2StartTime")), "CLASS2_START_TIME", queryVO.getFieldValueByFieldName("class2StartTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class2EndTime")), "CLASS2_END_TIME", queryVO.getFieldValueByFieldName("class2EndTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class2Analysis")), "CLASS2_ANALYSIS", queryVO.getFieldValueByFieldName("class2Analysis"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class2FinishQty")), "CLASS2_FINISH_QTY", queryVO.getFieldValueByFieldName("class2FinishQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class3PlanQty")), "CLASS3_PLAN_QTY", queryVO.getFieldValueByFieldName("class3PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class3StartTime")), "CLASS3_START_TIME", queryVO.getFieldValueByFieldName("class3StartTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class3EndTime")), "CLASS3_END_TIME", queryVO.getFieldValueByFieldName("class3EndTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class3Analysis")), "CLASS3_ANALYSIS", queryVO.getFieldValueByFieldName("class3Analysis"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class3FinishQty")), "CLASS3_FINISH_QTY", queryVO.getFieldValueByFieldName("class3FinishQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class4PlanQty")), "CLASS4_PLAN_QTY", queryVO.getFieldValueByFieldName("class4PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class4StartTime")), "CLASS4_START_TIME", queryVO.getFieldValueByFieldName("class4StartTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class4EndTime")), "CLASS4_END_TIME", queryVO.getFieldValueByFieldName("class4EndTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class4Analysis")), "CLASS4_ANALYSIS", queryVO.getFieldValueByFieldName("class4Analysis"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class4FinishQty")), "CLASS4_FINISH_QTY", queryVO.getFieldValueByFieldName("class4FinishQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class5PlanQty")), "CLASS5_PLAN_QTY", queryVO.getFieldValueByFieldName("class5PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class5StartTime")), "CLASS5_START_TIME", queryVO.getFieldValueByFieldName("class5StartTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class5EndTime")), "CLASS5_END_TIME", queryVO.getFieldValueByFieldName("class5EndTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class5Analysis")), "CLASS5_ANALYSIS", queryVO.getFieldValueByFieldName("class5Analysis"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class5FinishQty")), "CLASS5_FINISH_QTY", queryVO.getFieldValueByFieldName("class5FinishQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class6PlanQty")), "CLASS6_PLAN_QTY", queryVO.getFieldValueByFieldName("class6PlanQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class6StartTime")), "CLASS6_START_TIME", queryVO.getFieldValueByFieldName("class6StartTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class6EndTime")), "CLASS6_END_TIME", queryVO.getFieldValueByFieldName("class6EndTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class6Analysis")), "CLASS6_ANALYSIS", queryVO.getFieldValueByFieldName("class6Analysis"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("class6FinishQty")), "CLASS6_FINISH_QTY", queryVO.getFieldValueByFieldName("class6FinishQty"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("isDelivery")), "IS_DELIVERY", queryVO.getFieldValueByFieldName("isDelivery"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("issuedDate")), "ISSUED_DATE", queryVO.getFieldValueByFieldName("issuedDate"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("issuedStatus")), "ISSUED_STATUS", queryVO.getFieldValueByFieldName("issuedStatus"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("targetSystemIdentification")), "TARGET_SYSTEM_IDENTIFICATION", queryVO.getFieldValueByFieldName("targetSystemIdentification"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dataVersion")), "DATA_VERSION", queryVO.getFieldValueByFieldName("dataVersion"));
        // queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("createDate")), "CREATE_DATE", queryVO.getFieldValueByFieldName("createDate"));
        // queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("updateDate")), "UPDATE_DATE", queryVO.getFieldValueByFieldName("updateDate"));
    }

    /**
     * 列表查询
     */
    @Override
    public List<MesLhScheduleResult> selectList(MesLhScheduleResult queryVO) {
        QueryWrapper<MesLhScheduleResult> wrapper = new QueryWrapper<>();
        builderCondition(wrapper, queryVO);
        return entityMapper.selectList(wrapper);
    }
}
