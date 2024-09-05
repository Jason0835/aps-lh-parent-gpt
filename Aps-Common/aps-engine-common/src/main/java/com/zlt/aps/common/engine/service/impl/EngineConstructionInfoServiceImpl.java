package com.zlt.aps.common.engine.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.common.CxEngineQuotaCommonService;
import com.zlt.aps.common.engine.domain.BaseCxConstructionInfo;
import com.zlt.aps.common.engine.domain.EngineConstructionInfo;
import com.zlt.aps.common.engine.enums.TireTypeEnums;
import com.zlt.aps.common.engine.mapper.EngineConstructionInfoMapper;
import com.zlt.aps.common.engine.service.EngineConstructionInfoService;
import com.zlt.aps.common.engine.utils.CollectionUtil;

import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.constant.UserConstants;


/**
 * 施工信息Service业务层处理
 * 
 * @author Joran.zhang
 * @date 2021-06-30
 */
@Service("commonEngineConstructionInfoService")
@Slf4j
public class EngineConstructionInfoServiceImpl implements EngineConstructionInfoService
{
    @Autowired
    private EngineConstructionInfoMapper engineConstructionInfoMapper;
    
	@Autowired
	private CxEngineQuotaCommonService cxEngineQuotaCommonService;

    /**
     * 查询施工信息
     * 
     * @param id 施工信息ID
     * @return 施工信息
     */
    @Override
    public EngineConstructionInfo selectEngineConstructionInfoById(Long id)
    {
        return engineConstructionInfoMapper.selectEngineConstructionInfoById(id);
    }

    /**
     * 查询施工信息列表
     * 
     * @param engineConstructionInfo 施工信息
     * @return 施工信息
     */
    @Override
    public List<EngineConstructionInfo> selectEngineConstructionInfoList(EngineConstructionInfo engineConstructionInfo)
    {
        return engineConstructionInfoMapper.selectEngineConstructionInfoList(engineConstructionInfo);
    }

    /**
     * 批量查询施工信息列表
     * @param embryoCodeList 胚胎代码List
     * @return 施工信息
     */
    @Override
	public List<EngineConstructionInfo> selectEngineConstructionInfoListBatch(List<String> embryoCodeList) {
		// 从缓存中加载出施工信息
		//Map<String, EngineConstructionInfo> constructionMap = cxEngineQuotaCommonService
		//		.loadEngineConstructionMapFromRedis();
        //TODO Joran 2021-12-02 施工代码调整，后续补充调整回来
        Map<String, EngineConstructionInfo> constructionMap = new HashMap<>();
		if (CollectionUtil.isEmpty(constructionMap) || CollectionUtil.isEmpty(embryoCodeList)) {
			// 参数或结果为空则直接返回空集合
			return new ArrayList<>(0);
		}
		return embryoCodeList.stream().map(c -> constructionMap.get(c)).collect(Collectors.toList());
	}

    /**
     * 新增施工信息
     * 
     * @param engineConstructionInfo 施工信息
     * @return 结果
     */
    @Override
    public int insertEngineConstructionInfo(EngineConstructionInfo engineConstructionInfo)
    {
        engineConstructionInfo.setBaseVale(null);
        return engineConstructionInfoMapper.insertEngineConstructionInfo(engineConstructionInfo);
    }

    /**
     * 修改施工信息
     * 
     * @param engineConstructionInfo 施工信息
     * @return 结果
     */
    @Override
    public int updateEngineConstructionInfo(EngineConstructionInfo engineConstructionInfo)
    {
        engineConstructionInfo.setBaseVale(engineConstructionInfo.getId());
        return engineConstructionInfoMapper.updateEngineConstructionInfo(engineConstructionInfo);
    }

    /**
     * 批量删除施工信息
     * 
     * @param ids 需要删除的施工信息ID
     * @return 结果
     */
    @Override
    public int deleteEngineConstructionInfoByIds(Long[] ids)
    {
        return engineConstructionInfoMapper.deleteEngineConstructionInfoByIds(ids);
    }

    /**
     * 删除施工信息信息
     * 
     * @param id 施工信息ID
     * @return 结果
     */
    @Override
    public int deleteEngineConstructionInfoById(Long id)
    {
        return engineConstructionInfoMapper.deleteEngineConstructionInfoById(id);
    }

    /**
     * 校验${subTable.functionName}唯一性
     */
    @Override
    public String checkEngineConstructionInfoUnique(EngineConstructionInfo engineConstructionInfo) {
        if (engineConstructionInfo == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<EngineConstructionInfo> list = engineConstructionInfoMapper.selectEngineConstructionInfoList(engineConstructionInfo);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 加载所有胎胚代码对应的成型定额获取的数据
     * @Author Joran.Zhang
     * @Description
     * @Date 2021/6/30 19:02
     * @param
     * @return
     */
    @Override
    public Map<String, BaseCxConstructionInfo> loadConstructionInfo() {
        Map<String, BaseCxConstructionInfo> embryoCodeConstructionMap=null;
        List<EngineConstructionInfo> engineConstructionInfoList =this.engineConstructionInfoMapper.selectEngineConstructionInfoList(new EngineConstructionInfo());
        if(StringUtils.isNotEmpty(engineConstructionInfoList)){
            embryoCodeConstructionMap=new HashMap<>();
            BaseCxConstructionInfo baseCxConstructionInfo=null;
            for(EngineConstructionInfo engineConstructionInfo:engineConstructionInfoList){
                String embryoCode=engineConstructionInfo.getEmbryoCode();
                String bomDataVersion=engineConstructionInfo.getBomDataVersion();
                String key=GenerageMapKeyUtils.createMapKey(embryoCode,bomDataVersion);
                fillingMap(key,engineConstructionInfo,embryoCodeConstructionMap);
               /* baseCxConstructionInfo=new BaseCxConstructionInfo();
                baseCxConstructionInfo.setEmbryoCode(embryoCode);//胎胚代码
                baseCxConstructionInfo.setBomDataVersion(bomDataVersion);
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
                embryoCodeConstructionMap.put(embryoCode,baseCxConstructionInfo);*/
            }
        }
        return embryoCodeConstructionMap;
    }

    /**
     * 初始化所有规格
     * @return
     */
    @Override
    public Map<String, EngineConstructionInfo> loadEngineConstructionMap() {
        Map<String, EngineConstructionInfo> embryoCodeConstructionMap=null;
        List<EngineConstructionInfo> engineConstructionInfoList =this.engineConstructionInfoMapper.selectEngineConstructionInfoList(new EngineConstructionInfo());
        if(StringUtils.isNotEmpty(engineConstructionInfoList)){
            embryoCodeConstructionMap=new HashMap<>();
            for(EngineConstructionInfo engineConstructionInfo:engineConstructionInfoList){
                String embryoCode=engineConstructionInfo.getEmbryoCode();//胎胚代码
                String bomDataVersion=engineConstructionInfo.getBomDataVersion();//施工版本
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
    public Map<String, BaseCxConstructionInfo> changeConstructionInfo(Map<String, EngineConstructionInfo> engineConstructionInfoMap) {
        Map<String, BaseCxConstructionInfo> embryoCodeConstructionMap=null;
        if(StringUtils.isEmpty(engineConstructionInfoMap)){
            embryoCodeConstructionMap=loadConstructionInfo();
        }else{
            embryoCodeConstructionMap=new HashMap<>();
            BaseCxConstructionInfo baseCxConstructionInfo=null;
            for (Map.Entry<String, EngineConstructionInfo> entry: engineConstructionInfoMap.entrySet()) {
                String embryoCode=entry.getKey();//胎胚代码
                EngineConstructionInfo engineConstructionInfo=entry.getValue();
                String bomDataVersion=engineConstructionInfo.getBomDataVersion();
                String key=GenerageMapKeyUtils.createMapKey(embryoCode,bomDataVersion);
    /*            baseCxConstructionInfo=new BaseCxConstructionInfo();
                baseCxConstructionInfo.setEmbryoCode(engineConstructionInfo.getEmbryoCode());//胎胚代码
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
                baseCxConstructionInfo.setSectionWidth(engineConstructionInfo.getSectionWidth());//断面宽*/
                fillingMap(key,engineConstructionInfo,embryoCodeConstructionMap);
            }
        }
        return embryoCodeConstructionMap;
    }

    @Override
    public AjaxResult mergeSql(List<EngineConstructionInfo> list) {
        if (CollectionUtils.isEmpty(list)) {
            return AjaxResult.error("list无内容");
        }
        engineConstructionInfoMapper.mergeSql(list);
        return AjaxResult.success();
    }

    /**
     * 集合数据填充
     * @param engineConstructionInfo
     * @param embryoCodeConstructionMap
     */
    private void fillingMap(String key ,EngineConstructionInfo engineConstructionInfo, Map<String, BaseCxConstructionInfo> embryoCodeConstructionMap) {
        BaseCxConstructionInfo baseCxConstructionInfo=new BaseCxConstructionInfo();
        baseCxConstructionInfo.setEmbryoCode(engineConstructionInfo.getEmbryoCode());//胎胚代码
        baseCxConstructionInfo.setBomDataVersion(engineConstructionInfo.getBomDataVersion());//施工版本
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
