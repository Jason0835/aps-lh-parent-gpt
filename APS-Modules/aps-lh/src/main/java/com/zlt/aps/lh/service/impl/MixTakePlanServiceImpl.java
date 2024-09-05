package com.zlt.aps.lh.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.lh.mapper.MixTakePlanMapper;
import com.zlt.aps.lh.api.domain.entity.MixTakePlan;
import com.zlt.aps.lh.service.MixTakePlanService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import java.util.ArrayList;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 支领计划Service业务层处理
 * 
 * @author zlt
 * @date 2021-11-09
 */
@Service
public class MixTakePlanServiceImpl implements MixTakePlanService
{
    @Autowired
    private MixTakePlanMapper mixTakePlanMapper;

    /**
     * 查询支领计划
     * 
     * @param id 支领计划ID
     * @return 支领计划
     */
    @Override
    public MixTakePlan selectMixTakePlanById(Long id)
    {
        return mixTakePlanMapper.selectMixTakePlanById(id);
    }

    /**
     * 查询支领计划列表
     * 
     * @param mixTakePlan 支领计划
     * @return 支领计划
     */
    @Override
    public List<MixTakePlan> selectMixTakePlanList(MixTakePlan mixTakePlan)
    {
        return mixTakePlanMapper.selectMixTakePlanList(mixTakePlan);
    }

    /**
     * 新增支领计划
     * 
     * @param mixTakePlan 支领计划
     * @return 结果
     */
    @Override
    public int insertMixTakePlan(MixTakePlan mixTakePlan)
    {
        mixTakePlan.setBaseVale(null);
        return mixTakePlanMapper.insertMixTakePlan(mixTakePlan);
    }

    /**
     * 修改支领计划
     * 
     * @param mixTakePlan 支领计划
     * @return 结果
     */
    @Override
    public int updateMixTakePlan(MixTakePlan mixTakePlan)
    {
        mixTakePlan.setBaseVale(mixTakePlan.getId());
        return mixTakePlanMapper.updateMixTakePlan(mixTakePlan);
    }

    /**
     * 批量删除支领计划
     * 
     * @param ids 需要删除的支领计划ID
     * @return 结果
     */
    @Override
    public int deleteMixTakePlanByIds(Long[] ids)
    {
        return mixTakePlanMapper.deleteMixTakePlanByIds(ids);
    }

    /**
     * 删除支领计划信息
     * 
     * @param id 支领计划ID
     * @return 结果
     */
    @Override
    public int deleteMixTakePlanById(Long id)
    {
        return mixTakePlanMapper.deleteMixTakePlanById(id);
    }

    /**
     * 校验支领计划唯一性
     */
    @Override
    public String checkMixTakePlanUnique(MixTakePlan mixTakePlan) {
        if (mixTakePlan == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MixTakePlan> list = mixTakePlanMapper.selectMixTakePlanList(mixTakePlan);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入支领计划数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MixTakePlan> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MixTakePlan> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MixTakePlan mixTakePlan = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, mixTakePlan);
            if (CollectionUtils.isNotEmpty(validated)) {
                mixTakePlan.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                mixTakePlan.setBaseVale(null);
                importList.add(mixTakePlan);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    mixTakePlanMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MixTakePlan mixTakePlan = list.get(i);
                    // 错误记录跳过
                    if (mixTakePlan.getId() != null && mixTakePlan.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMixTakePlanUnique(mixTakePlan);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMixTakePlan(mixTakePlan);
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
