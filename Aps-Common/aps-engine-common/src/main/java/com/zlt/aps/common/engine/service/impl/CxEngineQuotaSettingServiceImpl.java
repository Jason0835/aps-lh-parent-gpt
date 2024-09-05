package com.zlt.aps.common.engine.service.impl;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.domain.CxEngineQuotaSetting;
import com.zlt.aps.common.engine.enums.TireTypeEnums;
import com.zlt.aps.common.engine.mapper.CxEngineQuotaSettingMapper;
import com.zlt.aps.common.engine.service.CxEngineQuotaSettingService;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
  * 成型定额数据获取
  * @ClassName CxEngineQuotaSettingServiceImpl
  * @Description TODO
  * @Author Joran.Zhang
  * @Date 2021/6/29 19:53
  * @Version 1.0
**/
@Service("commonCxEngineQuotaSettingService")
public class CxEngineQuotaSettingServiceImpl implements CxEngineQuotaSettingService {

    @Autowired
    private CxEngineQuotaSettingMapper cxEngineQuotaSettingMapper;

    /**
     * 加载所有成型机台对应的定额列表组装成map
     * @return
     */
    @Override
    public Map<String, List<CxEngineQuotaSetting>> listCxMachineQuotaSettingMap() {
        Map<String,List<CxEngineQuotaSetting>> quotaSettingMap=null;
        List<CxEngineQuotaSetting> cxEngineQuotaSettingList=this.cxEngineQuotaSettingMapper.selectCxEngineQuotaSettingList(new CxEngineQuotaSetting());
        if(StringUtils.isNotEmpty(cxEngineQuotaSettingList)){
            quotaSettingMap=new HashMap<>();
            List<CxEngineQuotaSetting> matchList=null;
            for (CxEngineQuotaSetting cxEngineQuotaSetting:cxEngineQuotaSettingList){
                matchList=new ArrayList<>();
                String cxMachineCode=cxEngineQuotaSetting.getCxMachineCode();//成型机台编号
                String specDimension=cxEngineQuotaSetting.getSpecDimension()==null?"":""+cxEngineQuotaSetting.getSpecDimension();//外胎规格尺寸信息
                String carcassBothLayer=cxEngineQuotaSetting.getCarcassBothLayer()==null?"":""+cxEngineQuotaSetting.getCarcassBothLayer();//胎体布层数
                String reinforce=cxEngineQuotaSetting.getReinforce();//是否补强
                String tireType=cxEngineQuotaSetting.getTireType();//轮胎类型
                String mapKey= GenerageMapKeyUtils.createMapKey(cxMachineCode,specDimension,carcassBothLayer,reinforce,tireType);
                if(quotaSettingMap.containsKey(mapKey)){
                    matchList=quotaSettingMap.get(mapKey);
                }
                matchList.add(cxEngineQuotaSetting);
                quotaSettingMap.put(mapKey,matchList);

            }
        }
        return quotaSettingMap;
    }

    /**
     * 根据轮胎规格描述解析对应的外胎类型
     * @param specDesc
     * @return
     */
    @Override
    public String getTireTypeCode(String specDesc) {
        return TireTypeEnums.getTireTypeCode(specDesc);
    }
}
