package com.zlt.aps.lh.handle;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.lh.api.domain.bo.ValidateResult;
import com.zlt.aps.lh.api.domain.dto.LhOrderInsertDTO;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResultUpdateDTO;
import com.zlt.aps.lh.api.domain.dto.LhTransferDeskDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.lh.service.ILhMachineInfoService;
import com.zlt.aps.lh.service.LhScheduleResultService;
import com.zlt.aps.maindata.service.IMdmMaterialInfoService;
import com.zlt.aps.mp.api.domain.entity.LhMachineInfo;
import com.zlt.aps.mp.api.domain.entity.MdmMaterialInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * @author xh
 * @version 1.0
 * @Description 硫化排程 相关校验
 * @date 2025/3/21
 */
@Service
public class LhScheduleResultCheckHandle {

    @Autowired
    private LhScheduleResultService lhScheduleResultService;

    @Autowired
    private ILhMachineInfoService lhMachineInfoService;

    @Autowired
    private IMdmMaterialInfoService mdmMaterialInfoService;

    /**
     * 硫化转机台验证
     * @param dto 转机台DTO
     * @return
     */
    public ValidateResult changeMachinePreCheck(LhTransferDeskDTO dto){
        //发布中 发布超时的 不能转机台
        int releasingOrTimeoutByIds = lhScheduleResultService.isReleasingOrTimeoutByIds(new long[]{dto.getId()});
        if (releasingOrTimeoutByIds > 0) {
            return ValidateResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        //查询排程是否存在
        LhScheduleResult lhscheduleResult = lhScheduleResultService.selectById(dto.getId());
        if (lhscheduleResult == null) {
            return ValidateResult.error(I18nUtil.getMessage("ui.data.column.lhScheduleResult.changeMachine.notExist"));
        }
        //查询机台编号是否存在
        LhMachineInfo lhMachineInfo = lhMachineInfoService.selectOneByMachineCode(dto.getFactoryCode(),dto.getLhMachineCode());
        if (lhMachineInfo == null) {
            return ValidateResult.error(I18nUtil.getMessage("ui.data.column.lhScheduleResult.machineInfo.notExist"));
        }
        //判断机台是否可用
        if (lhMachineInfo.getStatus().equals(ApsConstant.FALSE)) {
            return ValidateResult.error(I18nUtil.getMessage("ui.data.column.lhScheduleResult.machine.status.error"));
        }
        //验证机台是否相同
        String beforeMachineCode=lhscheduleResult.getLhMachineCode();
        if(dto.getLhMachineCode().equals(beforeMachineCode)){
            return ValidateResult.error(I18nUtil.getMessage("ui.data.column.lhScheduleResult.machine.same.error"));
        }
        //查询规格和机台是否匹配  通过物料信息表
        MdmMaterialInfo mdmMaterialInfo = mdmMaterialInfoService.selectOneByProductCodeAndSpecCode(lhscheduleResult.getProductCode(), lhscheduleResult.getFactoryCode());
        if (mdmMaterialInfo == null) {
            return ValidateResult.error(I18nUtil.getMessage("ui.data.column.lhScheduleResult.productInfo.notExist"));
        }
        // 判断物料的寸口是否为空
        // BigDecimal proSize = mdmMaterialInfo.getProSize();
        // 获取机台生产寸口范围
        BigDecimal dimensionMinimum = lhMachineInfo.getDimensionMinimum();
        BigDecimal dimensionMaximum = lhMachineInfo.getDimensionMaximum();
       /* // 校验物料寸口是否低于机台生产下限
        if (dimensionMinimum != null && proSize.compareTo(dimensionMinimum) < 0) {
            return ValidateResult.error(I18nUtil.getMessage("ui.data.column.lhScheduleResult.machine.dimension.not.in.range"));
        }
        // 校验物料寸口是否超过机台生产上限
        if (dimensionMaximum != null && proSize.compareTo(dimensionMaximum) > 0) {
            return ValidateResult.error(I18nUtil.getMessage("ui.data.column.lhScheduleResult.machine.dimension.not.in.range"));
        }*/
        //转入机台当日存在排程，不可转入
        LhScheduleResult lhScheduleResultMachine = lhScheduleResultService.getScheduleResultByMachineCodeAndScheduleDate(lhscheduleResult.getFactoryCode(),dto.getLhMachineCode(),lhscheduleResult.getScheduleDate());
        if(lhScheduleResultMachine != null){
            return ValidateResult.error(I18nUtil.getMessage("ui.data.column.lhScheduleResult.machine.schedule.exist"));
        }
        return ValidateResult.success();
    }


    /**
     * 更新排程校验(调量)
     * @param dto
     * @return
     */
    public ValidateResult updateLhScheduleResultCheck(LhScheduleResultUpdateDTO dto){
        //发布中 发布超时的 不能转机台
        int releasingOrTimeoutByIds = lhScheduleResultService.isReleasingOrTimeoutByIds(new long[]{dto.getId()});
        if (releasingOrTimeoutByIds > 0) {
            return ValidateResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        //查询排程是否存在
        LhScheduleResult lhscheduleResult = lhScheduleResultService.selectById(dto.getId());
        if (lhscheduleResult == null) {
            return ValidateResult.error(I18nUtil.getMessage("ui.data.column.lhScheduleResult.changeMachine.notExist"));
        }

        return ValidateResult.success();
    }

    /**
     * 插单校验
     * @param dto
     * @return
     */
    public ValidateResult insertLhScheduleResultCheck(LhOrderInsertDTO dto){
        //查询机台编号是否存在
        LhMachineInfo lhMachineInfo = lhMachineInfoService.selectOneByMachineCode(dto.getFactoryCode(),dto.getLhMachineCode());
        if (lhMachineInfo == null) {
            return ValidateResult.error(I18nUtil.getMessage("ui.data.column.lhScheduleResult.machineInfo.notExist"));
        }
        //判断机台是否可用
        if (lhMachineInfo.getStatus().equals(ApsConstant.FALSE)) {
            return ValidateResult.error(I18nUtil.getMessage("ui.data.column.lhScheduleResult.machine.status.error"));
        }
        //查询规格和机台是否匹配  通过物料信息表
        MdmMaterialInfo mdmMaterialInfo = mdmMaterialInfoService.selectOneByProductCodeAndSpecCode(dto.getProductCode(), dto.getFactoryCode());
        if (mdmMaterialInfo == null) {
            return ValidateResult.error(I18nUtil.getMessage("ui.data.column.lhScheduleResult.productInfo.notExist"));
        }
        // 判断物料的寸口是否为空
        // BigDecimal proSize = mdmMaterialInfo.getProSize();
        // 获取机台生产寸口范围
        BigDecimal dimensionMinimum = lhMachineInfo.getDimensionMinimum();
        BigDecimal dimensionMaximum = lhMachineInfo.getDimensionMaximum();
       /* // 校验物料寸口是否低于机台生产下限
        if (dimensionMinimum != null && proSize.compareTo(dimensionMinimum) < 0) {
            return ValidateResult.error(I18nUtil.getMessage("ui.data.column.lhScheduleResult.machine.dimension.not.in.range"));
        }
        // 校验物料寸口是否超过机台生产上限
        if (dimensionMaximum != null && proSize.compareTo(dimensionMaximum) > 0) {
            return ValidateResult.error(I18nUtil.getMessage("ui.data.column.lhScheduleResult.machine.dimension.not.in.range"));
        }*/
        return ValidateResult.success();
    }
}
