package com.zlt.aps.lh.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.lh.api.domain.entity.LhInProductionSpec;
import com.zlt.aps.lh.mapper.LhInProductionSpecMapper;
import com.zlt.aps.lh.service.LhInProductionSpecService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 硫化机台当前生产规格Service业务层处理
 *
 * @author chen
 * @date 2022-03-23
 */
@Service
public class LhInProductionSpecServiceImpl implements LhInProductionSpecService {
    @Autowired
    private LhInProductionSpecMapper lhInProductionSpecMapper;

    /**
     * 查询硫化机台当前生产规格
     *
     * @param id 硫化机台当前生产规格ID
     * @return 硫化机台当前生产规格
     */
    @Override
    public LhInProductionSpec selectLhInProductionSpecById(Long id) {
        return lhInProductionSpecMapper.selectLhInProductionSpecById(id);
    }

    /**
     * 查询硫化机台当前生产规格列表
     *
     * @param lhInProductionSpec 硫化机台当前生产规格
     * @return 硫化机台当前生产规格
     */
    @Override
    public List<LhInProductionSpec> selectLhInProductionSpecList(LhInProductionSpec lhInProductionSpec) {
        return lhInProductionSpecMapper.selectLhInProductionSpecList(lhInProductionSpec);
    }

    /**
     * 新增硫化机台当前生产规格
     *
     * @param lhInProductionSpec 硫化机台当前生产规格
     * @return 结果
     */
    @Override
    public int insertLhInProductionSpec(LhInProductionSpec lhInProductionSpec) {
        lhInProductionSpec.setBaseVale(null);
        return lhInProductionSpecMapper.insertLhInProductionSpec(lhInProductionSpec);
    }

    /**
     * 修改硫化机台当前生产规格
     *
     * @param lhInProductionSpec 硫化机台当前生产规格
     * @return 结果
     */
    @Override
    public int updateLhInProductionSpec(LhInProductionSpec lhInProductionSpec) {
        lhInProductionSpec.setBaseVale(lhInProductionSpec.getId());
        return lhInProductionSpecMapper.updateLhInProductionSpec(lhInProductionSpec);
    }

    /**
     * 批量删除硫化机台当前生产规格
     *
     * @param ids 需要删除的硫化机台当前生产规格ID
     * @return 结果
     */
    @Override
    public int deleteLhInProductionSpecByIds(Long[] ids) {
        return lhInProductionSpecMapper.deleteLhInProductionSpecByIds(ids);
    }

    /**
     * 删除硫化机台当前生产规格信息
     *
     * @param id 硫化机台当前生产规格ID
     * @return 结果
     */
    @Override
    public int deleteLhInProductionSpecById(Long id) {
        return lhInProductionSpecMapper.deleteLhInProductionSpecById(id);
    }

    /**
     * 校验硫化机台当前生产规格唯一性
     */
    @Override
    public String checkLhInProductionSpecUnique(LhInProductionSpec lhInProductionSpec) {
        if (lhInProductionSpec == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<LhInProductionSpec> list = lhInProductionSpecMapper.selectLhInProductionSpecList(lhInProductionSpec);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入硫化机台当前生产规格数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<LhInProductionSpec> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<LhInProductionSpec> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            LhInProductionSpec lhInProductionSpec = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, lhInProductionSpec);
            if (CollectionUtils.isNotEmpty(validated)) {
                lhInProductionSpec.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                lhInProductionSpec.setBaseVale(null);
                importList.add(lhInProductionSpec);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                lhInProductionSpecMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    LhInProductionSpec lhInProductionSpec = list.get(i);
                    // 错误记录跳过
                    if (lhInProductionSpec.getId() != null && lhInProductionSpec.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkLhInProductionSpecUnique(lhInProductionSpec);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertLhInProductionSpec(lhInProductionSpec);
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
