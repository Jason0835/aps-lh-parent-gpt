package com.zlt.aps.lh.engine.task;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.result.ValidateResult;
import com.zlt.aps.common.engine.service.AutoScheduleLogService;
import com.zlt.aps.common.engine.utils.BeanConverUtil;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResultDto;
import com.zlt.aps.lh.api.domain.entity.LhMachineInfo;
import com.zlt.aps.lh.engine.constants.LhEngineConstants;
import com.zlt.aps.lh.engine.domain.LhEngineScheduleResult;
import com.zlt.aps.lh.engine.exception.LhEngineException;
import com.zlt.aps.lh.engine.mapper.CommonLhEngineMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

import static com.zlt.aps.common.core.utils.ApsCommonUtil.logSplit;

/**
 * 硫化工序转机台引擎算法任务
 */
@Component("lhEngineChangeMachineScheduleTask")
@Slf4j
public class LhEngineChangeMachineScheduleTask {

    @Autowired
    private LhScheduleTaskCheck lhScheduleTaskCheck;

    @Autowired
    private CommonLhEngineMapper commonLhEngineMapper;

    @Resource
    private AutoScheduleLogService autoScheduleLogService;
    /**
     * 转机台前置验证
     * @param lhScheduleResultDto
     * @param changeMachineCode 要转入的机台编号
     * @return
     */
    public ValidateResult changeMachinePreCheck(LhScheduleResultDto lhScheduleResultDto,String changeMachineCode){
        StringBuilder errorMsg=new StringBuilder();
        //参数验证
        lhScheduleTaskCheck.validateChangeMachineParams(lhScheduleResultDto,errorMsg);
        if(StringUtils.isNotEmpty(errorMsg)){
            lhScheduleResultDto.setIsSuccess(false);
            return ValidateResult.error(errorMsg.toString());
        }
        //验证转入机台是否存在
        lhScheduleTaskCheck.changeMachineCodeValidate(changeMachineCode,errorMsg);
        if(StringUtils.isNotEmpty(errorMsg)){
            lhScheduleResultDto.setIsSuccess(false);
            return ValidateResult.error(errorMsg.toString());
        }
        //验证机台是否相同
        String beforeMachineCode=lhScheduleResultDto.getLhMachineCode();
        if(changeMachineCode.equals(beforeMachineCode)){
            errorMsg.append(I18nUtil.getMessage("lh.engine.changeMachine.machine.same.error"));
        }
        if(StringUtils.isNotEmpty(errorMsg)){
            lhScheduleResultDto.setIsSuccess(false);
            return ValidateResult.error(errorMsg.toString());
        }
        lhScheduleResultDto.setIsSuccess(true);//验证通过标识
        return ValidateResult.success();
    }

    /**
     * 转机台进行机台更新为新机台
     * @param lhScheduleResultDto
     * @param changeMachineCode
     */
    public void changeMachine(LhScheduleResultDto lhScheduleResultDto,String changeMachineCode) throws LhEngineException {
        //基础检查入参
        if(lhScheduleResultDto==null){
            throw new LhEngineException(I18nUtil.getMessage("lh.engine.input.params.empty.error"));
        }
        //检查验证是否通过
        if(!lhScheduleResultDto.getIsSuccess()){
            throw new LhEngineException(I18nUtil.getMessage("lh.engine.changeMachine.no.check.error"));
        }
        Map<String , LhMachineInfo> machineInfoMap =lhScheduleTaskCheck.getLhMachineInfoMap("");
        String sourceMachineName="已停用或禁用";
        if(StringUtils.isNotEmpty(machineInfoMap)&&machineInfoMap.containsKey(lhScheduleResultDto.getLhMachineCode())){
            LhMachineInfo  sourceLhMachine=machineInfoMap.get(lhScheduleResultDto.getLhMachineCode());
            sourceMachineName=sourceLhMachine.getMachineName();
        }

        String changeMachineName="已停用或禁用";
        if(StringUtils.isNotEmpty(machineInfoMap)&&machineInfoMap.containsKey(changeMachineCode)){
            LhMachineInfo  changeMachine=machineInfoMap.get(changeMachineCode);
            changeMachineName=changeMachine.getMachineName();
        }

        LhEngineScheduleResult lhEngineScheduleResult= BeanConverUtil.conver(lhScheduleResultDto,LhEngineScheduleResult.class);
        lhEngineScheduleResult.setLhMachineCode(changeMachineCode);
        lhEngineScheduleResult.setLhMachineName(changeMachineName);
        lhEngineScheduleResult.setUpdateTime(DateUtils.getNowDate());
        lhEngineScheduleResult.setUpdateBy(SecurityUtils.getUsername());
        lhEngineScheduleResult.setRemark("原始机台：" + sourceMachineName+",转入机台：" + changeMachineName);
        //更新硫化排程机台
        commonLhEngineMapper.updateLhScheduleLhMachine(lhEngineScheduleResult);
        //插入转机台日志
        autoScheduleLogService.insertLhScheduleLog(lhEngineScheduleResult.getBatchNo(), lhEngineScheduleResult.getOrderNo(), "转机台日志",
                logSplit("原始机台：" + sourceMachineName, ",转入机台：" + changeMachineName,"操作人员："+SecurityUtils.getUsername(),"操作时间："+DateUtils.getTime())); //添加日志
    }

}
