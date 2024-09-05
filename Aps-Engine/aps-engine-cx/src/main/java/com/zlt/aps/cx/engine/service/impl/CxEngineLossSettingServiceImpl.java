package com.zlt.aps.cx.engine.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.cx.engine.domain.CxEngineLossSetting;
import com.zlt.aps.cx.engine.mapper.CxEngineLossSettingMapper;
import com.zlt.aps.cx.engine.service.CxEngineLossSettingService;
import com.zlt.aps.cx.engine.utils.CxScheduleUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.constant.UserConstants;


/**
 * 成型损耗率设定Service业务层处理
 * 
 * @author Joran.zhang
 * @date 2021-06-29
 */
@Service
public class CxEngineLossSettingServiceImpl implements CxEngineLossSettingService
{
    @Autowired
    private CxEngineLossSettingMapper cxEngineLossSettingMapper;

    /**
     * 查询成型损耗率设定
     * 
     * @param id 成型损耗率设定ID
     * @return 成型损耗率设定
     */
    @Override
    public CxEngineLossSetting selectCxEngineLossSettingById(Long id)
    {
        return cxEngineLossSettingMapper.selectCxEngineLossSettingById(id);
    }

    /**
     * 查询成型损耗率设定列表
     * 
     * @param cxEngineLossSetting 成型损耗率设定
     * @return 成型损耗率设定
     */
    @Override
    public List<CxEngineLossSetting> selectCxEngineLossSettingList(CxEngineLossSetting cxEngineLossSetting)
    {
        return cxEngineLossSettingMapper.selectCxEngineLossSettingList(cxEngineLossSetting);
    }

    /**
     * 新增成型损耗率设定
     * 
     * @param cxEngineLossSetting 成型损耗率设定
     * @return 结果
     */
    @Override
    public int insertCxEngineLossSetting(CxEngineLossSetting cxEngineLossSetting)
    {
        cxEngineLossSetting.setBaseVale(null);
        return cxEngineLossSettingMapper.insertCxEngineLossSetting(cxEngineLossSetting);
    }

    /**
     * 修改成型损耗率设定
     * 
     * @param cxEngineLossSetting 成型损耗率设定
     * @return 结果
     */
    @Override
    public int updateCxEngineLossSetting(CxEngineLossSetting cxEngineLossSetting)
    {
        cxEngineLossSetting.setBaseVale(cxEngineLossSetting.getId());
        return cxEngineLossSettingMapper.updateCxEngineLossSetting(cxEngineLossSetting);
    }

    /**
     * 批量删除成型损耗率设定
     * 
     * @param ids 需要删除的成型损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteCxEngineLossSettingByIds(Long[] ids)
    {
        return cxEngineLossSettingMapper.deleteCxEngineLossSettingByIds(ids);
    }

    /**
     * 删除成型损耗率设定信息
     * 
     * @param id 成型损耗率设定ID
     * @return 结果
     */
    @Override
    public int deleteCxEngineLossSettingById(Long id)
    {
        return cxEngineLossSettingMapper.deleteCxEngineLossSettingById(id);
    }

    /**
     * 校验${subTable.functionName}唯一性
     */
    @Override
    public String checkCxEngineLossSettingUnique(CxEngineLossSetting cxEngineLossSetting) {
        if (cxEngineLossSetting == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<CxEngineLossSetting> list = cxEngineLossSettingMapper.selectCxEngineLossSettingList(cxEngineLossSetting);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 加载全部耗损率信息
     * @return
     */
    @Override
    public Map<String, Double> loadCxMachineLossRateMap() {
        Map<String,Double> lossRateMap=new HashMap<>();
        List<CxEngineLossSetting> list = cxEngineLossSettingMapper.selectCxEngineLossSettingList(new CxEngineLossSetting());
        if(StringUtils.isNotEmpty(list)){
            lossRateMap=new HashMap<>();
            for(CxEngineLossSetting cxEngineLossSetting:list){
                String key= CxScheduleUtils.getMapKeyByInputString(cxEngineLossSetting.getMachineCode(),cxEngineLossSetting.getEmbryoCode());
                lossRateMap.put(key,cxEngineLossSetting.getLossRate());
            }
        }
        return lossRateMap;
    }

}
