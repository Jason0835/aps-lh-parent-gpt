package com.zlt.aps.cx.engine.task;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cx.engine.domain.CxInProductionSpec;
import com.zlt.aps.cx.engine.mapper.CxLhEngineCommonMapper;
import com.zlt.aps.cx.engine.mapper.CxScheduleEngineMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 独立逻辑用来处理一些非功能性数据或者公用接口
 */
@Service("cxEngineCommonService")
@Slf4j
public class CxEngineCommonService {

    @Autowired
    private CxLhEngineCommonMapper cxLhEngineCommonMapper;

    @Autowired
    private CxScheduleEngineMapper cxScheduleEngineMapper;

    /**
     * 根据排程日期获取所有成型机台当前在产规格信息
     * @param productDate 生产日期
     * @return <机台编号,投产对象>
     */
    public Map<String,String> cxMachineInProductSpecMap(Date productDate){
        Map<String,String> machineProductMap=new HashMap<>();
        if(productDate==null){
            productDate= DateUtils.getNowDate();
        }
        String productDateStr=DateUtils.parseDateToStr("yyyyMMdd",productDate);
        List<CxInProductionSpec> cxInProductionSpecList=cxLhEngineCommonMapper.selectInProductionSpecByDate(productDateStr);
        if(StringUtils.isNotEmpty(cxInProductionSpecList)){
            machineProductMap= cxInProductionSpecList.stream()
                    .collect(Collectors.toMap(CxInProductionSpec::getCxMachineCode, CxInProductionSpec::getEmbryoCode, (v1, v2) -> v2));
        }
        return machineProductMap;
    }

    /**
     * 把排程数据同步到log表
     *
     * @param scheduleDate 排程日期，格式：yyyyMMdd
     */
    public void syncCxScheduleToLog(String scheduleDate,String cxMachineCode,String sourceCxOrder) {
        cxScheduleEngineMapper.syncCxScheduleToLog(scheduleDate,cxMachineCode);
        cxScheduleEngineMapper.deleteCxSchedule(scheduleDate,cxMachineCode);
        //Joran 2021-09-07 删除模具变动单临时表数据
        cxLhEngineCommonMapper.syncMoldChagePlanToLog(scheduleDate,sourceCxOrder);
        cxLhEngineCommonMapper.deleteLhEngineMoldChangePlanByScheduleDate(scheduleDate,sourceCxOrder);
    }


}
