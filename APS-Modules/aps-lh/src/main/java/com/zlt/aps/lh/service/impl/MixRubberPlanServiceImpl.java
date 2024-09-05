package com.zlt.aps.lh.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.lh.mapper.MixRubberPlanMapper;
import com.zlt.aps.lh.api.domain.entity.MixRubberPlan;
import com.zlt.aps.lh.service.MixRubberPlanService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import java.util.ArrayList;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 胶料日计划计划Service业务层处理
 * 
 * @author zlt
 * @date 2021-11-10
 */
@Service
public class MixRubberPlanServiceImpl implements MixRubberPlanService
{
    @Autowired
    private MixRubberPlanMapper mixRubberPlanMapper;

    /**
     * 查询胶料日计划计划
     * 
     * @param id 胶料日计划计划ID
     * @return 胶料日计划计划
     */
    @Override
    public MixRubberPlan selectMixRubberPlanById(Long id)
    {
        return mixRubberPlanMapper.selectMixRubberPlanById(id);
    }

    /**
     * 查询胶料日计划计划列表
     * 
     * @param mixRubberPlan 胶料日计划计划
     * @return 胶料日计划计划
     */
    @Override
    public List<MixRubberPlan> selectMixRubberPlanList(MixRubberPlan mixRubberPlan)
    {
        return mixRubberPlanMapper.selectMixRubberPlanList(mixRubberPlan);
    }

    /**
     * 新增胶料日计划计划
     * 
     * @param mixRubberPlan 胶料日计划计划
     * @return 结果
     */
    @Override
    public int insertMixRubberPlan(MixRubberPlan mixRubberPlan)
    {
        mixRubberPlan.setBaseVale(null);
        return mixRubberPlanMapper.insertMixRubberPlan(mixRubberPlan);
    }

    /**
     * 修改胶料日计划计划
     * 
     * @param mixRubberPlan 胶料日计划计划
     * @return 结果
     */
    @Override
    public int updateMixRubberPlan(MixRubberPlan mixRubberPlan)
    {
        mixRubberPlan.setBaseVale(mixRubberPlan.getId());
        return mixRubberPlanMapper.updateMixRubberPlan(mixRubberPlan);
    }

    /**
     * 批量删除胶料日计划计划
     * 
     * @param ids 需要删除的胶料日计划计划ID
     * @return 结果
     */
    @Override
    public int deleteMixRubberPlanByIds(Long[] ids)
    {
        return mixRubberPlanMapper.deleteMixRubberPlanByIds(ids);
    }

    /**
     * 删除胶料日计划计划信息
     * 
     * @param id 胶料日计划计划ID
     * @return 结果
     */
    @Override
    public int deleteMixRubberPlanById(Long id)
    {
        return mixRubberPlanMapper.deleteMixRubberPlanById(id);
    }

    /**
     * 校验胶料日计划计划唯一性
     */
    @Override
    public String checkMixRubberPlanUnique(MixRubberPlan mixRubberPlan) {
        if (mixRubberPlan == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MixRubberPlan> list = mixRubberPlanMapper.selectMixRubberPlanList(mixRubberPlan);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入胶料日计划计划数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MixRubberPlan> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MixRubberPlan> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MixRubberPlan mixRubberPlan = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, mixRubberPlan);
            if (CollectionUtils.isNotEmpty(validated)) {
                mixRubberPlan.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mixRubberPlan.setBaseVale(null);
                importList.add(mixRubberPlan);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    mixRubberPlanMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MixRubberPlan mixRubberPlan = list.get(i);
                    // 错误记录跳过
                    if (mixRubberPlan.getId() != null && mixRubberPlan.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMixRubberPlanUnique(mixRubberPlan);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMixRubberPlan(mixRubberPlan);
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
