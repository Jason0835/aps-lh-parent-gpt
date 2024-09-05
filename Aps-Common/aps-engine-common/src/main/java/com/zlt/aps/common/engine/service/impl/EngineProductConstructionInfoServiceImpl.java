package com.zlt.aps.common.engine.service.impl;

import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.common.engine.domain.BaseCxConstructionInfo;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;
import com.zlt.aps.common.engine.enums.TireTypeEnums;
import com.zlt.aps.common.engine.mapper.EngineConstructionInfoMapper;
import com.zlt.aps.common.engine.mapper.EngineProductConstructionInfoMapper;
import com.zlt.aps.common.engine.service.EngineConstructionInfoService;
import com.zlt.aps.common.engine.service.EngineProductConstructionInfoService;
import com.zlt.aps.common.engine.utils.CollectionUtil;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * 施工信息Service业务层处理
 * 
 * @author Joran.zhang
 * @date 2021-06-30
 */
@Service("commonEngineProductConstructionInfoService")
@Slf4j
public class EngineProductConstructionInfoServiceImpl implements EngineProductConstructionInfoService
{
    @Autowired
    private EngineProductConstructionInfoMapper engineProductConstructionInfoMapper;

    /**
     * 加载所有胎胚代码对应的成型定额获取的数据
     * @Author Joran.Zhang
     * @Description
     * @Date 2021/12/02 19:02
     * @param
     * @return
     */
    @Override
    public Map<String, BaseCxConstructionInfo> loadConstructionInfo() {
        Map<String, BaseCxConstructionInfo> embryoCodeConstructionMap=null;
        List<EngineProductConstructionInfo> engineConstructionInfoList =this.engineProductConstructionInfoMapper.selectEngineProductConstructionInfoList(new EngineProductConstructionInfo());
        if(StringUtils.isNotEmpty(engineConstructionInfoList)){
            embryoCodeConstructionMap=new HashMap<>();
            for(EngineProductConstructionInfo engineConstructionInfo:engineConstructionInfoList){
                String embryoCode=engineConstructionInfo.getEmbryoCode();//胎胚代码
                String bomDataVersion=engineConstructionInfo.getEmbryoVersion();//施工版本
                String key =GenerageMapKeyUtils.createMapKey(embryoCode,bomDataVersion);
                fillingMap(key,engineConstructionInfo,embryoCodeConstructionMap);
            }
        }else{
            embryoCodeConstructionMap=new HashMap<>();
        }
        return embryoCodeConstructionMap;
    }

    /**
     * 初始化所有规格
     * @return
     */
    @Override
    public Map<String, EngineProductConstructionInfo> loadEngineConstructionMap() {
        Map<String, EngineProductConstructionInfo> embryoCodeConstructionMap=new HashMap<>();
        List<EngineProductConstructionInfo> engineConstructionInfoList =this.engineProductConstructionInfoMapper.selectEngineProductConstructionInfoList(new EngineProductConstructionInfo());
        if(StringUtils.isNotEmpty(engineConstructionInfoList)){
            for(EngineProductConstructionInfo engineConstructionInfo:engineConstructionInfoList){
                String embryoCode=engineConstructionInfo.getEmbryoCode();//胎胚代码
                String bomDataVersion=engineConstructionInfo.getEmbryoVersion();//施工版本
                if(StringUtils.isEmpty(embryoCode)||StringUtils.isEmpty(bomDataVersion)){
                    log.error("【Redis缓存施工】缓存施工信息异常，胎胚代码或施工版本为空，不进行施工缓存。");
                    continue;
                }
                String key= GenerageMapKeyUtils.createMapKey(embryoCode,bomDataVersion);
                embryoCodeConstructionMap.put(key,engineConstructionInfo);
            }
        }
        return embryoCodeConstructionMap;
    }

    /**
     * 重新组装数据
     * @param engineConstructionInfoMap
     * @return
     */
    @Override
    public Map<String, BaseCxConstructionInfo> changeConstructionInfo(Map<String, EngineProductConstructionInfo> engineConstructionInfoMap) {
        Map<String, BaseCxConstructionInfo> embryoCodeConstructionMap=null;
        if(StringUtils.isEmpty(engineConstructionInfoMap)){
            embryoCodeConstructionMap=loadConstructionInfo();
        }else{
            embryoCodeConstructionMap=new HashMap<>();
            for (Map.Entry<String, EngineProductConstructionInfo> entry: engineConstructionInfoMap.entrySet()) {
                String key=entry.getKey();//胎胚代码
                EngineProductConstructionInfo engineConstructionInfo=entry.getValue();
                fillingMap(key,engineConstructionInfo,embryoCodeConstructionMap);
            }
        }
        return embryoCodeConstructionMap;
    }

    /**
     * 集合数据填充
     * @param engineConstructionInfo
     * @param embryoCodeConstructionMap
     */
    private void fillingMap(String key ,EngineProductConstructionInfo engineConstructionInfo, Map<String, BaseCxConstructionInfo> embryoCodeConstructionMap) {
        BaseCxConstructionInfo baseCxConstructionInfo=new BaseCxConstructionInfo();
        baseCxConstructionInfo.setEmbryoCode(engineConstructionInfo.getEmbryoCode());//胎胚代码
        baseCxConstructionInfo.setBomDataVersion(engineConstructionInfo.getEmbryoVersion());//施工版本
        baseCxConstructionInfo.setSpecDimension(engineConstructionInfo.getDimension());//寸口
        baseCxConstructionInfo.setSpecDesc(engineConstructionInfo.getSpecDesc());//规格描述
        baseCxConstructionInfo.setTireType(TireTypeEnums.getTireTypeCode(engineConstructionInfo.getSpecDesc()));//获取轮胎类型
        String reinforce=StringUtils.isNotEmpty(engineConstructionInfo.getReinforceSealGlue())?"0":"1";//是否补强
        baseCxConstructionInfo.setReinforce(reinforce);
        int tireFabricCount=0;
        tireFabricCount+=StringUtils.isEmpty(engineConstructionInfo.getTireFabricCode1())?0:1;
        tireFabricCount+=StringUtils.isEmpty(engineConstructionInfo.getTireFabricCode2())?0:1;
        tireFabricCount+=StringUtils.isEmpty(engineConstructionInfo.getTireFabricCode3())?0:1;
        baseCxConstructionInfo.setCarcassBothLayer(tireFabricCount);//胎体布层数
        baseCxConstructionInfo.setSectionWidth(engineConstructionInfo.getSectionWidth());//断面宽
        embryoCodeConstructionMap.put(key,baseCxConstructionInfo);
    }


}
