package com.zlt.aps.common.engine.service.impl;

import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.engine.constants.EngineConstants;
import com.zlt.aps.common.engine.domain.LhEngineTireConstructionInfo;
import com.zlt.aps.common.engine.mapper.CommonMapper;
import com.zlt.aps.common.engine.mapper.LhEngineTireConstructionInfoMapper;
import com.zlt.aps.common.engine.service.LhEngineTireConstructionInfoService;
import com.zlt.aps.common.engine.utils.GenerageMapKeyUtils;
import com.zlt.aps.lh.api.domain.dto.LhParamsDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 硫化工序参数逻辑层实现类
 */
@Service("lhEngineTireConstructionInfoService")
public class LhEngineTireConstructionInfoServiceImpl implements LhEngineTireConstructionInfoService {

    @Autowired
    private LhEngineTireConstructionInfoMapper lhEngineTireConstructionInfoMapper;
    @Autowired
    private CommonMapper commonMapper;
    /**
     * 根据条件获取硫化外胎施工信息
     * @param lhEngineTireConstructionInfo 硫化外胎施工信息
     * @return
     */
    @Override
    public List<LhEngineTireConstructionInfo> selectLhTireConstructionInfoList(LhEngineTireConstructionInfo lhEngineTireConstructionInfo) {
        return lhEngineTireConstructionInfoMapper.selectLhTireConstructionInfoList(lhEngineTireConstructionInfo);
    }

    /**
     * 根据sap品号和胎胚代码进行硫化时长获取
     * 2021-09-04 徐工确认做算法的时候相同SAP品号可以按一样的方式来获取硫化时间
     * @param sapCode sap品号
     * @param embryoCode  胎胚代码
     * @return
     */
    @Override
    public Double getLhTireTimeBySapCode(String sapCode, String embryoCode) {
        Double lhTime=null;
        //1.先进行硫化施工信息查询
        LhEngineTireConstructionInfo condition=new LhEngineTireConstructionInfo();
        condition.setSapCode(sapCode);
        List<LhEngineTireConstructionInfo> constructionInfoList=this.selectLhTireConstructionInfoList(condition);
        if(StringUtils.isNotEmpty(constructionInfoList)){
            LhEngineTireConstructionInfo info=constructionInfoList.get(0);
            if(info.getCuringTime()!=null){
                lhTime=info.getCuringTime();
            }
        }
        if(lhTime==null){
            //硫化施工拿不到硫化时间，从硫化工序读取默认参数，如果没有则抛异常
            LhParamsDto lhParamsCondtion=new LhParamsDto();
            lhParamsCondtion.setParamCode(EngineConstants.DEFAULT_LH_TIRE_TIME);
            List<LhParamsDto> lhParamsDtoList =this.commonMapper.selectLhParams(lhParamsCondtion);
            if(StringUtils.isEmpty(lhParamsDtoList)){
                throw new IllegalArgumentException(I18nUtil.getMessage("engine.common.lh.time.error"));
            }else{
                LhParamsDto lhParamsDto=lhParamsDtoList.get(0);
                if(StringUtils.isEmpty(lhParamsDto.getParamValue())){
                    throw new IllegalArgumentException(I18nUtil.getMessage("engine.common.lh.time.error"));
                }
                lhTime=Double.valueOf(lhParamsDto.getParamValue());
            }
        }
        return lhTime;
    }

    /**
     * 获取硫化施工信息
     * @param sapCode
     * @param embryoCode
     * @return
     */
    @Override
    public LhEngineTireConstructionInfo getLhConstructionInfoByCondition(String sapCode, String embryoCode) {
        LhEngineTireConstructionInfo lhEngineTireConstructionInfo =null;
        LhEngineTireConstructionInfo condition=new LhEngineTireConstructionInfo();
        condition.setSapCode(sapCode);
        if(StringUtils.isNotEmpty(embryoCode)){
            condition.setEmbryoCode(embryoCode);
        }
        List<LhEngineTireConstructionInfo> constructionInfoList=this.selectLhTireConstructionInfoList(condition);
        if(StringUtils.isNotEmpty(constructionInfoList)){
            lhEngineTireConstructionInfo=constructionInfoList.get(0);
        }
        return lhEngineTireConstructionInfo;
    }

    /**
     * 获取硫化时长
     * @param sapCode
     * @param embryoCode
     * @param sapTireConstructionListMap
     * @return
     */
    @Override
    public Double getSingleTireTimeBySap(String sapCode, String embryoCode, Map<String, List<LhEngineTireConstructionInfo>> sapTireConstructionListMap) {
        Double lhTime=null;
        String key= GenerageMapKeyUtils.createMapKey(sapCode,embryoCode);
        if(StringUtils.isNotEmpty(sapTireConstructionListMap)&&sapTireConstructionListMap.containsKey(sapCode)){
            List<LhEngineTireConstructionInfo> constructionInfoList=sapTireConstructionListMap.get(sapCode);
            if(StringUtils.isNotEmpty(constructionInfoList)){
                LhEngineTireConstructionInfo defaultInfo=constructionInfoList.get(0);
                for(LhEngineTireConstructionInfo lhEngineTireConstructionInfo:constructionInfoList){
                    String machKey=GenerageMapKeyUtils.createMapKey(lhEngineTireConstructionInfo.getSapCode(),lhEngineTireConstructionInfo.getEmbryoCode());
                    if(key.equals(machKey)){
                        defaultInfo=lhEngineTireConstructionInfo;
                        break;
                    }
                }
              /*  LhEngineTireConstructionInfo info=constructionInfoList.get(0);
                if(info.getCuringTime()!=null){
                    lhTime=info.getCuringTime();
                }*/
              if(defaultInfo!=null&&defaultInfo.getCuringTime()!=null){
                  lhTime=defaultInfo.getCuringTime();
              }
            }
        }
        if(lhTime==null){
            //硫化施工拿不到硫化时间，从硫化工序读取默认参数，如果没有则抛异常
            LhParamsDto lhParamsCondtion=new LhParamsDto();
            lhParamsCondtion.setParamCode(EngineConstants.DEFAULT_LH_TIRE_TIME);
            List<LhParamsDto> lhParamsDtoList =this.commonMapper.selectLhParams(lhParamsCondtion);
            if(StringUtils.isEmpty(lhParamsDtoList)){
                throw new IllegalArgumentException(I18nUtil.getMessage("engine.common.lh.time.error"));
            }else{
                LhParamsDto lhParamsDto=lhParamsDtoList.get(0);
                if(StringUtils.isEmpty(lhParamsDto.getParamValue())){
                    throw new IllegalArgumentException(I18nUtil.getMessage("engine.common.lh.time.error"));
                }
                lhTime=Double.valueOf(lhParamsDto.getParamValue());
            }
        }
        return lhTime;
    }
}
