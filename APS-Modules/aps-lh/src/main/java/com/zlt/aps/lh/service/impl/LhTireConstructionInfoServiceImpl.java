package com.zlt.aps.lh.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.lh.api.domain.entity.LhTireConstructionInfo;
import com.zlt.aps.lh.mapper.LhTireConstructionInfoMapper;
import com.zlt.aps.lh.service.LhTireConstructionInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 硫化外胎施工信息Service业务层处理
 * 
 * @author zlt
 * @date 2021-11-15
 */
@Service
public class LhTireConstructionInfoServiceImpl implements LhTireConstructionInfoService
{
    @Autowired
    private LhTireConstructionInfoMapper lhTireConstructionInfoMapper;

    /**
     * 查询硫化外胎施工信息
     * 
     * @param id 硫化外胎施工信息ID
     * @return 硫化外胎施工信息
     */
    @Override
    public LhTireConstructionInfo selectLhTireConstructionInfoById(Long id)
    {
        return lhTireConstructionInfoMapper.selectLhTireConstructionInfoById(id);
    }

    /**
     * 查询硫化外胎施工信息列表
     * 
     * @param lhTireConstructionInfo 硫化外胎施工信息
     * @return 硫化外胎施工信息
     */
    @Override
    public List<LhTireConstructionInfo> selectLhTireConstructionInfoList(LhTireConstructionInfo lhTireConstructionInfo)
    {
        return lhTireConstructionInfoMapper.selectLhTireConstructionInfoList(lhTireConstructionInfo);
    }

    /**
     * 新增硫化外胎施工信息
     * 
     * @param lhTireConstructionInfo 硫化外胎施工信息
     * @return 结果
     */
    @Override
    public int insertLhTireConstructionInfo(LhTireConstructionInfo lhTireConstructionInfo)
    {
        lhTireConstructionInfo.setBaseVale(null);
        return lhTireConstructionInfoMapper.insertLhTireConstructionInfo(lhTireConstructionInfo);
    }

    /**
     * 修改硫化外胎施工信息
     * 
     * @param lhTireConstructionInfo 硫化外胎施工信息
     * @return 结果
     */
    @Override
    public int updateLhTireConstructionInfo(LhTireConstructionInfo lhTireConstructionInfo)
    {
        lhTireConstructionInfo.setBaseVale(lhTireConstructionInfo.getId());
        return lhTireConstructionInfoMapper.updateLhTireConstructionInfo(lhTireConstructionInfo);
    }

    /**
     * 批量删除硫化外胎施工信息
     * 
     * @param ids 需要删除的硫化外胎施工信息ID
     * @return 结果
     */
    @Override
    public int deleteLhTireConstructionInfoByIds(Long[] ids)
    {
        return lhTireConstructionInfoMapper.deleteLhTireConstructionInfoByIds(ids);
    }

    /**
     * 删除硫化外胎施工信息信息
     * 
     * @param id 硫化外胎施工信息ID
     * @return 结果
     */
    @Override
    public int deleteLhTireConstructionInfoById(Long id)
    {
        return lhTireConstructionInfoMapper.deleteLhTireConstructionInfoById(id);
    }

    /**
     * 校验硫化外胎施工信息唯一性
     */
    @Override
    public String checkLhTireConstructionInfoUnique(LhTireConstructionInfo lhTireConstructionInfo) {
        if (lhTireConstructionInfo == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<LhTireConstructionInfo> list = lhTireConstructionInfoMapper.checkLhTireConstructionInfoUnique(lhTireConstructionInfo);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入硫化外胎施工信息数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<LhTireConstructionInfo> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<LhTireConstructionInfo> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhTireConstructionInfo lhTireConstructionInfo = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, lhTireConstructionInfo);
            if (CollectionUtils.isNotEmpty(validated)) {
                lhTireConstructionInfo.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                lhTireConstructionInfo.setBaseVale(null);
                importList.add(lhTireConstructionInfo);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    lhTireConstructionInfoMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    LhTireConstructionInfo lhTireConstructionInfo = list.get(i);
                    // 错误记录跳过
                    if (lhTireConstructionInfo.getId() != null && lhTireConstructionInfo.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkLhTireConstructionInfoUnique(lhTireConstructionInfo);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertLhTireConstructionInfo(lhTireConstructionInfo);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.data.column.mouthPlate.alreadyExists"), importErrorLogs);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            successNum = 0;
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 根据sap查询对应的胎胚代码
     * @param lhTireConstructionInfo sap品号
     * @return 查询到的胎胚代码
     */
    @Override
    public List<LhTireConstructionInfo> getEmbryoCodeListBySapCode(LhTireConstructionInfo lhTireConstructionInfo) {
        return lhTireConstructionInfoMapper.getEmbryoCodeListBySapCode(lhTireConstructionInfo);
    }

}
