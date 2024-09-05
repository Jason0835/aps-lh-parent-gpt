package com.zlt.aps.lh.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.lh.api.domain.entity.LhMoldAdjustPlan;
import com.zlt.aps.lh.mapper.LhMoldAdjustPlanMapper;
import com.zlt.aps.lh.service.LhMoldAdjustPlanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 硫化模具调整计划Service业务层处理
 *
 * @author chen
 * @date 2022-03-23
 */
@Service
public class LhMoldAdjustPlanServiceImpl implements LhMoldAdjustPlanService {
    @Autowired
    private LhMoldAdjustPlanMapper lhMoldAdjustPlanMapper;

    /**
     * 查询硫化模具调整计划
     *
     * @param id 硫化模具调整计划ID
     * @return 硫化模具调整计划
     */
    @Override
    public LhMoldAdjustPlan selectLhMoldAdjustPlanById(Long id) {
        return lhMoldAdjustPlanMapper.selectLhMoldAdjustPlanById(id);
    }

    /**
     * 查询硫化模具调整计划列表
     *
     * @param lhMoldAdjustPlan 硫化模具调整计划
     * @return 硫化模具调整计划
     */
    @Override
    public List<LhMoldAdjustPlan> selectLhMoldAdjustPlanList(LhMoldAdjustPlan lhMoldAdjustPlan) {
        return lhMoldAdjustPlanMapper.selectLhMoldAdjustPlanList(lhMoldAdjustPlan);
    }

    /**
     * 新增硫化模具调整计划
     *
     * @param lhMoldAdjustPlan 硫化模具调整计划
     * @return 结果
     */
    @Override
    public int insertLhMoldAdjustPlan(LhMoldAdjustPlan lhMoldAdjustPlan) {
        lhMoldAdjustPlan.setBaseVale(null);
        return lhMoldAdjustPlanMapper.insertLhMoldAdjustPlan(lhMoldAdjustPlan);
    }

    /**
     * 修改硫化模具调整计划
     *
     * @param lhMoldAdjustPlan 硫化模具调整计划
     * @return 结果
     */
    @Override
    public int updateLhMoldAdjustPlan(LhMoldAdjustPlan lhMoldAdjustPlan) {
        lhMoldAdjustPlan.setBaseVale(lhMoldAdjustPlan.getId());
        return lhMoldAdjustPlanMapper.updateLhMoldAdjustPlan(lhMoldAdjustPlan);
    }

    /**
     * 批量删除硫化模具调整计划
     *
     * @param ids 需要删除的硫化模具调整计划ID
     * @return 结果
     */
    @Override
    public int deleteLhMoldAdjustPlanByIds(Long[] ids) {
        return lhMoldAdjustPlanMapper.deleteLhMoldAdjustPlanByIds(ids);
    }

    /**
     * 删除硫化模具调整计划信息
     *
     * @param id 硫化模具调整计划ID
     * @return 结果
     */
    @Override
    public int deleteLhMoldAdjustPlanById(Long id) {
        return lhMoldAdjustPlanMapper.deleteLhMoldAdjustPlanById(id);
    }

    /**
     * 校验硫化模具调整计划唯一性
     */
    @Override
    public String checkLhMoldAdjustPlanUnique(LhMoldAdjustPlan lhMoldAdjustPlan) {
        if (lhMoldAdjustPlan == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<LhMoldAdjustPlan> list = lhMoldAdjustPlanMapper.selectLhMoldAdjustPlanList(lhMoldAdjustPlan);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入硫化模具调整计划数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<LhMoldAdjustPlan> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<LhMoldAdjustPlan> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhMoldAdjustPlan lhMoldAdjustPlan = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, lhMoldAdjustPlan);
            if (CollectionUtils.isNotEmpty(validated)) {
                lhMoldAdjustPlan.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                lhMoldAdjustPlan.setBaseVale(null);
                importList.add(lhMoldAdjustPlan);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                lhMoldAdjustPlanMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    LhMoldAdjustPlan lhMoldAdjustPlan = list.get(i);
                    // 错误记录跳过
                    if (lhMoldAdjustPlan.getId() != null && lhMoldAdjustPlan.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkLhMoldAdjustPlanUnique(lhMoldAdjustPlan);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertLhMoldAdjustPlan(lhMoldAdjustPlan);
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
