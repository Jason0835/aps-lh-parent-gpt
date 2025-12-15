package com.zlt.aps.cx.handle;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.cx.service.CxScheduleResultService;
import com.zlt.aps.cxlh.cx.api.domain.dto.CxTransferDeskDTO;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxScheduleResult;
import com.zlt.aps.lh.api.domain.bo.ValidateResult;
import com.zlt.aps.lh.api.domain.dto.LhTransferDeskDTO;
import com.zlt.aps.lh.api.domain.entity.LhScheduleResult;
import com.zlt.aps.maindata.service.IMdmMoldingMachineService;
import com.zlt.aps.monthplan.api.domain.entity.MdmMoldingMachine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @author xh
 * @version 1.0
 * @Description 成型排程 相关校验
 * @date 2025/3/25
 */
@Service
public class CxScheduleResultCheckHandle {

    @Autowired
    private CxScheduleResultService cxScheduleResultService;
    @Autowired
    private IMdmMoldingMachineService mdmMoldingMachineService;

    /**
     * 成型转机台校验
     * @param dto
     * @return
     */
    public ValidateResult changeMachinePreCheck(CxTransferDeskDTO dto){
        //发布中 发布超时的 不能转机台
        int releasingOrTimeoutByIds = cxScheduleResultService.isReleasingOrTimeoutByIds(new long[]{dto.getId()});
        if (releasingOrTimeoutByIds > 0) {
            return ValidateResult.error(I18nUtil.getMessage("ui.data.column.scheduleResult.release.isReleasingOrTimeoutById"));
        }
        //查询排程是否存在
        CxScheduleResult cxscheduleResult = cxScheduleResultService.selectById(dto.getId());
        if (cxscheduleResult == null) {
            return ValidateResult.error(I18nUtil.getMessage("ui.data.column.lhScheduleResult.changeMachine.notExist"));
        }
        //查询机台是否存在
//        MdmMoldingMachine cxMachineInfo = mdmMoldingMachineService.getMoldingMachineByMachineCode(cxscheduleResult.getFactoryCode(),dto.getCxMachineCode());
//        if (cxMachineInfo == null) {
//            return ValidateResult.error(I18nUtil.getMessage("ui.data.column.lhScheduleResult.machineInfo.notExist"));
//        }
        //判断是否空机台
//        CxScheduleResult cxScheduleResultMachine = cxScheduleResultService.getScheduleResultByMachineCodeAndScheduleDate(cxscheduleResult.getFactoryCode(),dto.getCxMachineCode(),cxscheduleResult.getScheduleDate());
//        if(cxScheduleResultMachine != null){
//            return ValidateResult.error(I18nUtil.getMessage("ui.data.column.lhScheduleResult.machine.schedule.exist"));
//        }
        return ValidateResult.success();
    }



}
