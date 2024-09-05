package com.zlt.aps.cx.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.cx.mapper.CxCloseOutRangeMapper;
import com.zlt.aps.cx.api.domain.entity.CxCloseOutRange;
import com.zlt.aps.cx.service.CxCloseOutRangeService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import java.util.ArrayList;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 成型收尾范围系数Service业务层处理
 * 
 * @author zlt
 * @date 2021-12-28
 */
@Service
public class CxCloseOutRangeServiceImpl implements CxCloseOutRangeService
{
    @Autowired
    private CxCloseOutRangeMapper cxCloseOutRangeMapper;

    /**
     * 查询成型收尾范围系数
     * 
     * @param id 成型收尾范围系数ID
     * @return 成型收尾范围系数
     */
    @Override
    public CxCloseOutRange selectCxCloseOutRangeById(Long id)
    {
        return cxCloseOutRangeMapper.selectCxCloseOutRangeById(id);
    }

    /**
     * 查询成型收尾范围系数列表
     * 
     * @param cxCloseOutRange 成型收尾范围系数
     * @return 成型收尾范围系数
     */
    @Override
    public List<CxCloseOutRange> selectCxCloseOutRangeList(CxCloseOutRange cxCloseOutRange)
    {
        return cxCloseOutRangeMapper.selectCxCloseOutRangeList(cxCloseOutRange);
    }

    /**
     * 新增成型收尾范围系数
     * 
     * @param cxCloseOutRange 成型收尾范围系数
     * @return 结果
     */
    @Override
    public int insertCxCloseOutRange(CxCloseOutRange cxCloseOutRange)
    {
        cxCloseOutRange.setBaseVale(null);
        return cxCloseOutRangeMapper.insertCxCloseOutRange(cxCloseOutRange);
    }

    /**
     * 修改成型收尾范围系数
     * 
     * @param cxCloseOutRange 成型收尾范围系数
     * @return 结果
     */
    @Override
    public int updateCxCloseOutRange(CxCloseOutRange cxCloseOutRange)
    {
        cxCloseOutRange.setBaseVale(cxCloseOutRange.getId());
        return cxCloseOutRangeMapper.updateCxCloseOutRange(cxCloseOutRange);
    }

    /**
     * 批量删除成型收尾范围系数
     * 
     * @param ids 需要删除的成型收尾范围系数ID
     * @return 结果
     */
    @Override
    public int deleteCxCloseOutRangeByIds(Long[] ids)
    {
        return cxCloseOutRangeMapper.deleteCxCloseOutRangeByIds(ids);
    }

    /**
     * 删除成型收尾范围系数信息
     * 
     * @param id 成型收尾范围系数ID
     * @return 结果
     */
    @Override
    public int deleteCxCloseOutRangeById(Long id)
    {
        return cxCloseOutRangeMapper.deleteCxCloseOutRangeById(id);
    }

    /**
     * 校验成型收尾范围系数唯一性
     */
    @Override
    public String checkCxCloseOutRangeUnique(CxCloseOutRange cxCloseOutRange) {
        List<CxCloseOutRange> list = cxCloseOutRangeMapper.checkCxCloseOutRangeUnique(cxCloseOutRange);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入成型收尾范围系数数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<CxCloseOutRange> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxCloseOutRange> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxCloseOutRange cxCloseOutRange = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, cxCloseOutRange);
            if (CollectionUtils.isNotEmpty(validated)) {
                cxCloseOutRange.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                cxCloseOutRange.setBaseVale(null);
                importList.add(cxCloseOutRange);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    cxCloseOutRangeMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    CxCloseOutRange cxCloseOutRange = list.get(i);
                    // 错误记录跳过
                    if (cxCloseOutRange.getId() != null && cxCloseOutRange.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkCxCloseOutRangeUnique(cxCloseOutRange);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertCxCloseOutRange(cxCloseOutRange);
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
