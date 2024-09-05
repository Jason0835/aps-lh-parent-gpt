package com.zlt.aps.lh.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.lh.mapper.MixMachineInfoMapper;
import com.zlt.aps.lh.api.domain.entity.MixMachineInfo;
import com.zlt.aps.lh.service.MixMachineInfoService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import java.util.ArrayList;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 密炼机台信息Service业务层处理
 * 
 * @author zlt
 * @date 2021-11-09
 */
@Service
public class MixMachineInfoServiceImpl implements MixMachineInfoService
{
    @Autowired
    private MixMachineInfoMapper mixMachineInfoMapper;

    /**
     * 查询密炼机台信息
     * 
     * @param id 密炼机台信息ID
     * @return 密炼机台信息
     */
    @Override
    public MixMachineInfo selectMixMachineInfoById(Long id)
    {
        return mixMachineInfoMapper.selectMixMachineInfoById(id);
    }

    /**
     * 查询密炼机台信息列表
     * 
     * @param mixMachineInfo 密炼机台信息
     * @return 密炼机台信息
     */
    @Override
    public List<MixMachineInfo> selectMixMachineInfoList(MixMachineInfo mixMachineInfo)
    {
        return mixMachineInfoMapper.selectMixMachineInfoList(mixMachineInfo);
    }

    /**
     * 新增密炼机台信息
     * 
     * @param mixMachineInfo 密炼机台信息
     * @return 结果
     */
    @Override
    public int insertMixMachineInfo(MixMachineInfo mixMachineInfo)
    {
        mixMachineInfo.setBaseVale(null);
        return mixMachineInfoMapper.insertMixMachineInfo(mixMachineInfo);
    }

    /**
     * 修改密炼机台信息
     * 
     * @param mixMachineInfo 密炼机台信息
     * @return 结果
     */
    @Override
    public int updateMixMachineInfo(MixMachineInfo mixMachineInfo)
    {
        mixMachineInfo.setBaseVale(mixMachineInfo.getId());
        return mixMachineInfoMapper.updateMixMachineInfo(mixMachineInfo);
    }

    /**
     * 批量删除密炼机台信息
     * 
     * @param ids 需要删除的密炼机台信息ID
     * @return 结果
     */
    @Override
    public int deleteMixMachineInfoByIds(Long[] ids)
    {
        return mixMachineInfoMapper.deleteMixMachineInfoByIds(ids);
    }

    /**
     * 删除密炼机台信息信息
     * 
     * @param id 密炼机台信息ID
     * @return 结果
     */
    @Override
    public int deleteMixMachineInfoById(Long id)
    {
        return mixMachineInfoMapper.deleteMixMachineInfoById(id);
    }

    /**
     * 校验密炼机台信息唯一性
     */
    @Override
    public String checkMixMachineInfoUnique(MixMachineInfo mixMachineInfo) {
        if (mixMachineInfo == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MixMachineInfo> list = mixMachineInfoMapper.selectMixMachineInfoList(mixMachineInfo);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入密炼机台信息数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MixMachineInfo> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MixMachineInfo> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MixMachineInfo mixMachineInfo = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, mixMachineInfo);
            if (CollectionUtils.isNotEmpty(validated)) {
                mixMachineInfo.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mixMachineInfo.setBaseVale(null);
                importList.add(mixMachineInfo);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    mixMachineInfoMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MixMachineInfo mixMachineInfo = list.get(i);
                    // 错误记录跳过
                    if (mixMachineInfo.getId() != null && mixMachineInfo.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMixMachineInfoUnique(mixMachineInfo);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMixMachineInfo(mixMachineInfo);
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
