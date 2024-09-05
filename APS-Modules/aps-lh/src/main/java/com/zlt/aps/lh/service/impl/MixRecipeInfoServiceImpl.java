package com.zlt.aps.lh.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.lh.mapper.MixRecipeInfoMapper;
import com.zlt.aps.lh.api.domain.entity.MixRecipeInfo;
import com.zlt.aps.lh.service.MixRecipeInfoService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import java.util.ArrayList;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 密炼配方信息Service业务层处理
 * 
 * @author zlt
 * @date 2021-11-09
 */
@Service
public class MixRecipeInfoServiceImpl implements MixRecipeInfoService
{
    @Autowired
    private MixRecipeInfoMapper mixRecipeInfoMapper;

    /**
     * 查询密炼配方信息
     * 
     * @param id 密炼配方信息ID
     * @return 密炼配方信息
     */
    @Override
    public MixRecipeInfo selectMixRecipeInfoById(Long id)
    {
        return mixRecipeInfoMapper.selectMixRecipeInfoById(id);
    }

    /**
     * 查询密炼配方信息列表
     * 
     * @param mixRecipeInfo 密炼配方信息
     * @return 密炼配方信息
     */
    @Override
    public List<MixRecipeInfo> selectMixRecipeInfoList(MixRecipeInfo mixRecipeInfo)
    {
        return mixRecipeInfoMapper.selectMixRecipeInfoList(mixRecipeInfo);
    }

    /**
     * 新增密炼配方信息
     * 
     * @param mixRecipeInfo 密炼配方信息
     * @return 结果
     */
    @Override
    public int insertMixRecipeInfo(MixRecipeInfo mixRecipeInfo)
    {
        mixRecipeInfo.setBaseVale(null);
        return mixRecipeInfoMapper.insertMixRecipeInfo(mixRecipeInfo);
    }

    /**
     * 修改密炼配方信息
     * 
     * @param mixRecipeInfo 密炼配方信息
     * @return 结果
     */
    @Override
    public int updateMixRecipeInfo(MixRecipeInfo mixRecipeInfo)
    {
        mixRecipeInfo.setBaseVale(mixRecipeInfo.getId());
        return mixRecipeInfoMapper.updateMixRecipeInfo(mixRecipeInfo);
    }

    /**
     * 批量删除密炼配方信息
     * 
     * @param ids 需要删除的密炼配方信息ID
     * @return 结果
     */
    @Override
    public int deleteMixRecipeInfoByIds(Long[] ids)
    {
        return mixRecipeInfoMapper.deleteMixRecipeInfoByIds(ids);
    }

    /**
     * 删除密炼配方信息信息
     * 
     * @param id 密炼配方信息ID
     * @return 结果
     */
    @Override
    public int deleteMixRecipeInfoById(Long id)
    {
        return mixRecipeInfoMapper.deleteMixRecipeInfoById(id);
    }

    /**
     * 校验密炼配方信息唯一性
     */
    @Override
    public String checkMixRecipeInfoUnique(MixRecipeInfo mixRecipeInfo) {
        if (mixRecipeInfo == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MixRecipeInfo> list = mixRecipeInfoMapper.selectMixRecipeInfoList(mixRecipeInfo);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入密炼配方信息数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MixRecipeInfo> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MixRecipeInfo> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MixRecipeInfo mixRecipeInfo = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, mixRecipeInfo);
            if (CollectionUtils.isNotEmpty(validated)) {
                mixRecipeInfo.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mixRecipeInfo.setBaseVale(null);
                importList.add(mixRecipeInfo);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    mixRecipeInfoMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MixRecipeInfo mixRecipeInfo = list.get(i);
                    // 错误记录跳过
                    if (mixRecipeInfo.getId() != null && mixRecipeInfo.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMixRecipeInfoUnique(mixRecipeInfo);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMixRecipeInfo(mixRecipeInfo);
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
