package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.entity.InProductionSpec;
import com.zlt.aps.cx.mapper.InProductionSpecMapper;
import com.zlt.aps.cx.service.InProductionSpecService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 成型机台当前生产规格Service业务层处理
 *
 * @author chen
 * @date 2022-02-25
 */
@Service
public class InProductionSpecServiceImpl implements InProductionSpecService {
    @Autowired
    private InProductionSpecMapper inProductionSpecMapper;

    /**
     * 查询成型机台当前生产规格
     *
     * @param id 成型机台当前生产规格ID
     * @return 成型机台当前生产规格
     */
    @Override
    public InProductionSpec selectInProductionSpecById(Long id) {
        return inProductionSpecMapper.selectInProductionSpecById(id);
    }

    /**
     * 查询成型机台当前生产规格列表
     *
     * @param inProductionSpec 成型机台当前生产规格
     * @return 成型机台当前生产规格
     */
    @Override
    public List<InProductionSpec> selectInProductionSpecList(InProductionSpec inProductionSpec) {
        return inProductionSpecMapper.selectInProductionSpecList(inProductionSpec);
    }

    /**
     * 新增成型机台当前生产规格
     *
     * @param inProductionSpec 成型机台当前生产规格
     * @return 结果
     */
    @Override
    public int insertInProductionSpec(InProductionSpec inProductionSpec) {
        inProductionSpec.setBaseVale(null);
        return inProductionSpecMapper.insertInProductionSpec(inProductionSpec);
    }

    /**
     * 修改成型机台当前生产规格
     *
     * @param inProductionSpec 成型机台当前生产规格
     * @return 结果
     */
    @Override
    public int updateInProductionSpec(InProductionSpec inProductionSpec) {
        inProductionSpec.setBaseVale(inProductionSpec.getId());
        return inProductionSpecMapper.updateInProductionSpec(inProductionSpec);
    }

    /**
     * 批量删除成型机台当前生产规格
     *
     * @param ids 需要删除的成型机台当前生产规格ID
     * @return 结果
     */
    @Override
    public int deleteInProductionSpecByIds(Long[] ids) {
        return inProductionSpecMapper.deleteInProductionSpecByIds(ids);
    }

    /**
     * 删除成型机台当前生产规格信息
     *
     * @param id 成型机台当前生产规格ID
     * @return 结果
     */
    @Override
    public int deleteInProductionSpecById(Long id) {
        return inProductionSpecMapper.deleteInProductionSpecById(id);
    }

    /**
     * 校验成型机台当前生产规格唯一性
     */
    @Override
    public String checkInProductionSpecUnique(InProductionSpec inProductionSpec) {
        if (inProductionSpec == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<InProductionSpec> list = inProductionSpecMapper.selectInProductionSpecList(inProductionSpec);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入成型机台当前生产规格数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<InProductionSpec> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<InProductionSpec> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            InProductionSpec inProductionSpec = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, inProductionSpec);
            if (CollectionUtils.isNotEmpty(validated)) {
                inProductionSpec.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                inProductionSpec.setBaseVale(null);
                importList.add(inProductionSpec);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                inProductionSpecMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    InProductionSpec inProductionSpec = list.get(i);
                    // 错误记录跳过
                    if (inProductionSpec.getId() != null && inProductionSpec.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkInProductionSpecUnique(inProductionSpec);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertInProductionSpec(inProductionSpec);
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
