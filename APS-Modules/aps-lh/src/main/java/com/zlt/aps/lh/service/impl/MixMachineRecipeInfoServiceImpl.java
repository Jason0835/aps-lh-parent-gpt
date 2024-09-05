package com.zlt.aps.lh.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.lh.mapper.MixMachineRecipeInfoMapper;
import com.zlt.aps.lh.api.domain.entity.MixMachineRecipeInfo;
import com.zlt.aps.lh.service.MixMachineRecipeInfoService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import java.util.ArrayList;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 机台和配方对应及下车重量Service业务层处理
 * 
 * @author zlt
 * @date 2021-11-09
 */
@Service
public class MixMachineRecipeInfoServiceImpl implements MixMachineRecipeInfoService
{
    @Autowired
    private MixMachineRecipeInfoMapper mixMachineRecipeInfoMapper;

    /**
     * 查询机台和配方对应及下车重量
     * 
     * @param id 机台和配方对应及下车重量ID
     * @return 机台和配方对应及下车重量
     */
    @Override
    public MixMachineRecipeInfo selectMixMachineRecipeInfoById(Long id)
    {
        return mixMachineRecipeInfoMapper.selectMixMachineRecipeInfoById(id);
    }

    /**
     * 查询机台和配方对应及下车重量列表
     * 
     * @param mixMachineRecipeInfo 机台和配方对应及下车重量
     * @return 机台和配方对应及下车重量
     */
    @Override
    public List<MixMachineRecipeInfo> selectMixMachineRecipeInfoList(MixMachineRecipeInfo mixMachineRecipeInfo)
    {
        return mixMachineRecipeInfoMapper.selectMixMachineRecipeInfoList(mixMachineRecipeInfo);
    }

    /**
     * 新增机台和配方对应及下车重量
     * 
     * @param mixMachineRecipeInfo 机台和配方对应及下车重量
     * @return 结果
     */
    @Override
    public int insertMixMachineRecipeInfo(MixMachineRecipeInfo mixMachineRecipeInfo)
    {
        mixMachineRecipeInfo.setBaseVale(null);
        return mixMachineRecipeInfoMapper.insertMixMachineRecipeInfo(mixMachineRecipeInfo);
    }

    /**
     * 修改机台和配方对应及下车重量
     * 
     * @param mixMachineRecipeInfo 机台和配方对应及下车重量
     * @return 结果
     */
    @Override
    public int updateMixMachineRecipeInfo(MixMachineRecipeInfo mixMachineRecipeInfo)
    {
        mixMachineRecipeInfo.setBaseVale(mixMachineRecipeInfo.getId());
        return mixMachineRecipeInfoMapper.updateMixMachineRecipeInfo(mixMachineRecipeInfo);
    }

    /**
     * 批量删除机台和配方对应及下车重量
     * 
     * @param ids 需要删除的机台和配方对应及下车重量ID
     * @return 结果
     */
    @Override
    public int deleteMixMachineRecipeInfoByIds(Long[] ids)
    {
        return mixMachineRecipeInfoMapper.deleteMixMachineRecipeInfoByIds(ids);
    }

    /**
     * 删除机台和配方对应及下车重量信息
     * 
     * @param id 机台和配方对应及下车重量ID
     * @return 结果
     */
    @Override
    public int deleteMixMachineRecipeInfoById(Long id)
    {
        return mixMachineRecipeInfoMapper.deleteMixMachineRecipeInfoById(id);
    }

    /**
     * 校验机台和配方对应及下车重量唯一性
     */
    @Override
    public String checkMixMachineRecipeInfoUnique(MixMachineRecipeInfo mixMachineRecipeInfo) {
        if (mixMachineRecipeInfo == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MixMachineRecipeInfo> list = mixMachineRecipeInfoMapper.selectMixMachineRecipeInfoList(mixMachineRecipeInfo);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入机台和配方对应及下车重量数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MixMachineRecipeInfo> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MixMachineRecipeInfo> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MixMachineRecipeInfo mixMachineRecipeInfo = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, mixMachineRecipeInfo);
            if (CollectionUtils.isNotEmpty(validated)) {
                mixMachineRecipeInfo.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mixMachineRecipeInfo.setBaseVale(null);
                importList.add(mixMachineRecipeInfo);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    mixMachineRecipeInfoMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MixMachineRecipeInfo mixMachineRecipeInfo = list.get(i);
                    // 错误记录跳过
                    if (mixMachineRecipeInfo.getId() != null && mixMachineRecipeInfo.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMixMachineRecipeInfoUnique(mixMachineRecipeInfo);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMixMachineRecipeInfo(mixMachineRecipeInfo);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("此处需手动填写唯一校验失败国际化信息"), importErrorLogs);
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
}
