package com.zlt.aps.common.engine.common;

import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.constants.RedisCacheKeyPrefixConstants;
import com.zlt.aps.common.engine.domain.BaseCxConstructionInfo;
import com.zlt.aps.common.engine.domain.CxEngineQuotaSetting;
import com.zlt.aps.common.engine.domain.EngineProductConstructionInfo;
import com.zlt.aps.common.engine.mapper.CommonMapper;
import com.zlt.aps.common.engine.service.CxEngineQuotaSettingService;
import com.zlt.aps.common.engine.service.EngineProductConstructionInfoService;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.cx.api.domain.entity.CxMachineInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成型定额对外暴露公共服务
 */
@Component("cxEngineQuotaCommonService")
@Slf4j
public class CxEngineQuotaCommonService {

    @Autowired
    private CxEngineQuotaSettingService cxEngineQuotaSettingService;

    @Autowired
    private EngineProductConstructionInfoService engineProductConstructionInfoService;

    @Autowired
    private CommonMapper commonMapper;

    @Autowired
    private RedisTemplate redisTemplate;

    /**
     * 根据成型机台编号获取成型机定额量
     * @param cxMachineCode
     * @param embryoCode
     * @return
     */
    public Integer getCxMachineQuota(String cxMachineCode,String embryoCode,String bomDataVersion){
        String key=GenerageMapKeyUtils.createMapKey(embryoCode,bomDataVersion);
        Integer finalQuota=null;
        Map<String,CxMachineInfo> machineQuotaMap =getCxMachineInfoFromRedis();
        Map<String, BaseCxConstructionInfo> embryoCodeConstructionMap=initConstructionInfoMap();
        Map<String, List<CxEngineQuotaSetting>> quotaSettingMap=initCxQuotaSetting();
        BaseCxConstructionInfo cxInfo=embryoCodeConstructionMap.get(key);
        if(cxInfo!=null){
            cxInfo.setCxMachineCode(cxMachineCode);
            String specDimension=cxInfo.getSpecDimension()==null?"":""+cxInfo.getSpecDimension();//外胎规格尺寸信息
            String carcassBothLayer=cxInfo.getCarcassBothLayer()==null?"":""+cxInfo.getCarcassBothLayer();//胎体布层数
            String reinforce=cxInfo.getReinforce();//是否补强
            String tireType=cxInfo.getTireType();//轮胎类型
            String mapKey= GenerageMapKeyUtils.createMapKey(cxMachineCode,specDimension,carcassBothLayer,reinforce,tireType);
            if(quotaSettingMap.containsKey(mapKey)){//定额表中存在
                Double sectionWidth=cxInfo.getSectionWidth();//断面宽
                if(sectionWidth==null){ //如果断面宽为空时则默认取机台定额
                    if(machineQuotaMap.containsKey(cxMachineCode)&&machineQuotaMap.get(cxMachineCode)!=null){
                        CxMachineInfo cxMachineInfo=machineQuotaMap.get(cxMachineCode);
                        Long machineQuota=machineQuotaMap.get(cxMachineCode).getQuata();
                        finalQuota= BigDecimal.valueOf(machineQuota * cxMachineInfo.getQuotaRatio()).setScale(1, RoundingMode.UP).intValue();
                    }
                    log.debug("【获取定额】，断面宽为空，默认取机台定额，"+finalQuota);
                }else{
                    List<CxEngineQuotaSetting> quotaSettingList=quotaSettingMap.get(mapKey);
                    for (CxEngineQuotaSetting cxEngineQuotaSetting:quotaSettingList){
                        if(sectionWidth>=cxEngineQuotaSetting.getSectionWidthMinimum()&&sectionWidth<=cxEngineQuotaSetting.getSectionWidthMaximum()){
                            Integer machineQuota=cxEngineQuotaSetting.getFinalQuota();
                            finalQuota=BigDecimal.valueOf(machineQuota * cxEngineQuotaSetting.getQuotaRatio()).setScale(1, RoundingMode.UP).intValue();
                            break;
                        }
                    }
                }

            }
        }
        if(finalQuota==null){
            if(machineQuotaMap.containsKey(cxMachineCode)&&machineQuotaMap.get(cxMachineCode)!=null){//Joran 2021-11-19 如果定额配置里面断面宽不在范围内就取机台定额
                CxMachineInfo cxMachineInfo=machineQuotaMap.get(cxMachineCode);
                Long machineQuota=machineQuotaMap.get(cxMachineCode).getQuata();
                finalQuota= BigDecimal.valueOf(machineQuota * cxMachineInfo.getQuotaRatio()).setScale(1, RoundingMode.UP).intValue();
                log.debug("【获取定额】，断面宽不在定额设定范围内，取机台默认定额，"+finalQuota);
            }else{
                throw new IllegalArgumentException(StringUtils.format(I18nUtil.getMessage("cx.engine.common.quota.exception"), cxMachineCode));
            }
        }
        return  finalQuota;
    }

    /**
     * 根据成型机台编号和胎胚代码，计算定额平均值
     *
     * @param quotaKeys 机台编号获取成型机code数组，格式：机台code$胎面胎面
     * @return
     */
    public Integer getCxMachineQuota(String[] quotaKeys) {
        try {
            Integer totalQuota = 0;
            for (int i = 0; i < quotaKeys.length; i++) {
                String[] quotaKeysArry = quotaKeys[i].split("\\$");
                if(quotaKeysArry.length != 3) {
                    log.error("根据成型机台编号和胎胚代码计算定额平均值错误，原因：数据缺失（quotaKeys：quotaKeys[i]）");
                    continue;
                }
                int quota = getCxMachineQuota(quotaKeysArry[0], quotaKeysArry[1],quotaKeysArry[2]);
                totalQuota += quota;
            }
            return (int)Math.ceil(totalQuota.doubleValue() / quotaKeys.length);
        } catch (Exception e) {
            log.error("根据成型机台编号和胎胚代码计算定额平均值异常", e);
            return 0;
        }
    }

    /**
     *  获取成型机台信息缓存
     * @Author Joran.Zhang
     * @Description
     * @Date 2021/7/1 14:41
     * @param
     * @return
     */
    public Map<String,CxMachineInfo> getCxMachineInfoFromRedis(){
        Map<String,CxMachineInfo> machineQuotaMap=null;
        if(!redisTemplate.hasKey(RedisCacheKeyPrefixConstants.CX_MACHINE_INFO_MAP)){
            machineQuotaMap =getMachineQuotaMap();
            if(StringUtils.isNotEmpty(machineQuotaMap)){
                redisTemplate.opsForHash().putAll(RedisCacheKeyPrefixConstants.CX_MACHINE_INFO_MAP,machineQuotaMap);
            }

        }else{
            machineQuotaMap= redisTemplate.opsForHash().entries(RedisCacheKeyPrefixConstants.CX_MACHINE_INFO_MAP);
        }
        return  machineQuotaMap;
    }

    /**
     * 刷新成型机台信息缓存
     * @Author Joran.Zhang
     * @Description
     * @Date 2021/7/1 14:14
     * @param
     * @return
     */
    public void delCacheCxMachineInfoMap(){
        //删除缓存
        redisTemplate.delete(RedisCacheKeyPrefixConstants.CX_MACHINE_INFO_MAP);
        //删除缓存
        redisTemplate.delete(RedisCacheKeyPrefixConstants.CX_QUOTA_SETTING_MAP);
    }

    /**
     *  初始化施工信息表中的获取定额关键内容
     * @Author Joran.Zhang
     * @Description
     * @Date 2021/7/1 14:07
     * @param
     * @return
     */
    private Map<String, BaseCxConstructionInfo> initConstructionInfoMap() {
        Map<String, BaseCxConstructionInfo> embryoCodeConstructionMap =null;
        if(!redisTemplate.hasKey(RedisCacheKeyPrefixConstants.CONSTRUCTION_INFO_MAP)){
            log.debug("开始初始化施工信息表中的获取定额关键内容缓存..."+ DateUtils.getTime());
            //加载全部规格的施工信息
            Map<String, EngineProductConstructionInfo> engineConstructionInfoMap=loadEngineConstructionMapFromRedis();
            //重新组装施工信息为定额相关
            embryoCodeConstructionMap =this.engineProductConstructionInfoService.changeConstructionInfo(engineConstructionInfoMap);
            if(StringUtils.isNotEmpty(engineConstructionInfoMap)){
                redisTemplate.opsForHash().putAll(RedisCacheKeyPrefixConstants.CONSTRUCTION_INFO_MAP,embryoCodeConstructionMap);
            }
            log.debug("初始化施工信息表中的获取定额关键内容缓存结束..."+ DateUtils.getTime());
        }else{
            embryoCodeConstructionMap= redisTemplate.opsForHash().entries(RedisCacheKeyPrefixConstants.CONSTRUCTION_INFO_MAP);
        }
        return embryoCodeConstructionMap;

    }

    public void delCacheConstructionInfoMap(){
        //删除缓存
        redisTemplate.delete(RedisCacheKeyPrefixConstants.CONSTRUCTION_INFO_MAP);
        //删除胎胚对应的缓存信息
        redisTemplate.delete(RedisCacheKeyPrefixConstants.ENGINE_CONSTRUCTION_MAP);
    }

    /**
     * 初始化定额数据组装
     * @Author Joran.Zhang
     * @Description
     * @Date 2021/7/1 14:05
     * @param
     * @return
     */
    private Map<String, List<CxEngineQuotaSetting>> initCxQuotaSetting(){
        Map<String, List<CxEngineQuotaSetting>> quotaSettingMap = null;
        if(!redisTemplate.hasKey(RedisCacheKeyPrefixConstants.CX_QUOTA_SETTING_MAP)){
            log.debug("开始定额数据初始化依赖数据缓存..."+ DateUtils.getTime());
            quotaSettingMap=cxEngineQuotaSettingService.listCxMachineQuotaSettingMap();
            if(StringUtils.isNotEmpty(quotaSettingMap)){
                redisTemplate.opsForHash().putAll(RedisCacheKeyPrefixConstants.CX_QUOTA_SETTING_MAP,quotaSettingMap);
            }
            log.debug("结束定额数据初始化依赖数据缓存..."+ DateUtils.getTime());
        }else{
            quotaSettingMap= redisTemplate.opsForHash().entries(RedisCacheKeyPrefixConstants.CX_QUOTA_SETTING_MAP);
        }
        return quotaSettingMap;
    }

    public void delCacheCxQuotaSetting(){
        //删除缓存
        redisTemplate.delete(RedisCacheKeyPrefixConstants.CX_QUOTA_SETTING_MAP);
    }

    /**
     * 初始化成型机台默认定额数据
     * @return
     */
    public Map<String,CxMachineInfo> getMachineQuotaMap(){
        Map<String,CxMachineInfo> machineQuotaMap=new HashMap<>();
        List<CxMachineInfo> cxMachineInfoList=this.commonMapper.selectCxMachineInfoList(new CxMachineInfo());
        if(StringUtils.isNotEmpty(cxMachineInfoList)){
            machineQuotaMap=new HashMap<>();
            for(CxMachineInfo cxMachineInfo:cxMachineInfoList){
                if(StringUtils.isNotEmpty(cxMachineInfo.getMachineCode())){
                    machineQuotaMap.put(cxMachineInfo.getMachineCode(),cxMachineInfo);
                }else{
                    log.error("【Redis缓存成型机台信息】初始化机台数据，机台编号为空，不进行缓存");
                }

            }
        }
        return machineQuotaMap;
    }

    /**
     *  获取成型施工信息缓存
     * @Author Joran.Zhang
     * @Description
     * @Date 2021/7/1 14:41
     * @param
     * @return
     */
    public Map<String, EngineProductConstructionInfo> loadEngineConstructionMapFromRedis(){
        Map<String, EngineProductConstructionInfo> embryoCodeConstructionMap=null;
        if(!redisTemplate.hasKey(RedisCacheKeyPrefixConstants.ENGINE_CONSTRUCTION_MAP)){
            embryoCodeConstructionMap=engineProductConstructionInfoService.loadEngineConstructionMap();
            if(StringUtils.isNotEmpty(embryoCodeConstructionMap)){
                redisTemplate.opsForHash().putAll(RedisCacheKeyPrefixConstants.ENGINE_CONSTRUCTION_MAP,embryoCodeConstructionMap);
            }
        }else{
            embryoCodeConstructionMap= redisTemplate.opsForHash().entries(RedisCacheKeyPrefixConstants.ENGINE_CONSTRUCTION_MAP);
        }
        return  embryoCodeConstructionMap;
    }
}