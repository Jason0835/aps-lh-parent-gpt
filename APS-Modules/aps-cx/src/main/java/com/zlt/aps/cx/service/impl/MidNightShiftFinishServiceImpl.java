package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.entity.MidNightShiftFinish;
import com.zlt.aps.cx.mapper.MidNightShiftFinishMapper;
import com.zlt.aps.cx.service.MidNightShiftFinishService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 成型排程中夜班完成量Service业务层处理
 *
 * @author chen
 * @date 2022-02-25
 */
@Service
public class MidNightShiftFinishServiceImpl implements MidNightShiftFinishService {
    @Autowired
    private MidNightShiftFinishMapper midNightShiftFinishMapper;

    /**
     * 查询成型排程中夜班完成量
     *
     * @param id 成型排程中夜班完成量ID
     * @return 成型排程中夜班完成量
     */
    @Override
    public MidNightShiftFinish selectMidNightShiftFinishById(Long id) {
        return midNightShiftFinishMapper.selectMidNightShiftFinishById(id);
    }

    /**
     * 查询成型排程中夜班完成量列表
     *
     * @param midNightShiftFinish 成型排程中夜班完成量
     * @return 成型排程中夜班完成量
     */
    @Override
    public List<MidNightShiftFinish> selectMidNightShiftFinishList(MidNightShiftFinish midNightShiftFinish) {
        return midNightShiftFinishMapper.selectMidNightShiftFinishList(midNightShiftFinish);
    }

    /**
     * 新增成型排程中夜班完成量
     *
     * @param midNightShiftFinish 成型排程中夜班完成量
     * @return 结果
     */
    @Override
    public int insertMidNightShiftFinish(MidNightShiftFinish midNightShiftFinish) {
        midNightShiftFinish.setBaseVale(null);
        return midNightShiftFinishMapper.insertMidNightShiftFinish(midNightShiftFinish);
    }

    /**
     * 修改成型排程中夜班完成量
     *
     * @param midNightShiftFinish 成型排程中夜班完成量
     * @return 结果
     */
    @Override
    public int updateMidNightShiftFinish(MidNightShiftFinish midNightShiftFinish) {
        midNightShiftFinish.setBaseVale(midNightShiftFinish.getId());
        return midNightShiftFinishMapper.updateMidNightShiftFinish(midNightShiftFinish);
    }

    /**
     * 批量删除成型排程中夜班完成量
     *
     * @param ids 需要删除的成型排程中夜班完成量ID
     * @return 结果
     */
    @Override
    public int deleteMidNightShiftFinishByIds(Long[] ids) {
        return midNightShiftFinishMapper.deleteMidNightShiftFinishByIds(ids);
    }

    /**
     * 删除成型排程中夜班完成量信息
     *
     * @param id 成型排程中夜班完成量ID
     * @return 结果
     */
    @Override
    public int deleteMidNightShiftFinishById(Long id) {
        return midNightShiftFinishMapper.deleteMidNightShiftFinishById(id);
    }

    /**
     * 校验成型排程中夜班完成量唯一性
     */
    @Override
    public String checkMidNightShiftFinishUnique(MidNightShiftFinish midNightShiftFinish) {
        if (midNightShiftFinish == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<MidNightShiftFinish> list = midNightShiftFinishMapper.selectMidNightShiftFinishList(midNightShiftFinish);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入成型排程中夜班完成量数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<MidNightShiftFinish> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<MidNightShiftFinish> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            MidNightShiftFinish midNightShiftFinish = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, midNightShiftFinish);
            if (CollectionUtils.isNotEmpty(validated)) {
                midNightShiftFinish.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else {
                midNightShiftFinish.setBaseVale(null);
                importList.add(midNightShiftFinish);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                midNightShiftFinishMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    MidNightShiftFinish midNightShiftFinish = list.get(i);
                    // 错误记录跳过
                    if (midNightShiftFinish.getId() != null && midNightShiftFinish.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkMidNightShiftFinishUnique(midNightShiftFinish);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertMidNightShiftFinish(midNightShiftFinish);
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
