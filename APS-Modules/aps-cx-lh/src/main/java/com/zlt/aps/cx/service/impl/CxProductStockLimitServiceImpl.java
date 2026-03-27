package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.cx.mapper.entity.CxProductStockLimitMapper;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxProductStockLimit;

import com.zlt.aps.cx.service.CxProductStockLimitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 成型投产班次库存限定设置Service业务层处理
 * 
 * @author zlt
 * @date 2022-01-07
 */
@Service
public class CxProductStockLimitServiceImpl implements CxProductStockLimitService
{
    @Autowired
    private CxProductStockLimitMapper cxProductStockLimitMapper;

    /**
     * 查询成型投产班次库存限定设置
     * 
     * @param id 成型投产班次库存限定设置ID
     * @return 成型投产班次库存限定设置
     */
    @Override
    public CxProductStockLimit selectCxProductStockLimitById(Long id)
    {
        return cxProductStockLimitMapper.selectCxProductStockLimitById(id);
    }

    /**
     * 查询成型投产班次库存限定设置列表
     * 
     * @param cxProductStockLimit 成型投产班次库存限定设置
     * @return 成型投产班次库存限定设置
     */
    @Override
    public List<CxProductStockLimit> selectCxProductStockLimitList(CxProductStockLimit cxProductStockLimit)
    {
        return cxProductStockLimitMapper.selectCxProductStockLimitList(cxProductStockLimit);
    }

    /**
     * 新增成型投产班次库存限定设置
     * 
     * @param cxProductStockLimit 成型投产班次库存限定设置
     * @return 结果
     */
    @Override
    public int insertCxProductStockLimit(CxProductStockLimit cxProductStockLimit)
    {
        cxProductStockLimit.setBaseVale(null);
        return cxProductStockLimitMapper.insertCxProductStockLimit(cxProductStockLimit);
    }

    /**
     * 修改成型投产班次库存限定设置
     * 
     * @param cxProductStockLimit 成型投产班次库存限定设置
     * @return 结果
     */
    @Override
    public int updateCxProductStockLimit(CxProductStockLimit cxProductStockLimit)
    {
        cxProductStockLimit.setBaseVale(cxProductStockLimit.getId());
        return cxProductStockLimitMapper.updateCxProductStockLimit(cxProductStockLimit);
    }

    /**
     * 批量删除成型投产班次库存限定设置
     * 
     * @param ids 需要删除的成型投产班次库存限定设置ID
     * @return 结果
     */
    @Override
    public int deleteCxProductStockLimitByIds(Long[] ids)
    {
        return cxProductStockLimitMapper.deleteCxProductStockLimitByIds(ids);
    }

    /**
     * 删除成型投产班次库存限定设置信息
     * 
     * @param id 成型投产班次库存限定设置ID
     * @return 结果
     */
    @Override
    public int deleteCxProductStockLimitById(Long id)
    {
        return cxProductStockLimitMapper.deleteCxProductStockLimitById(id);
    }

    /**
     * 校验成型投产班次库存限定设置唯一性
     */
    @Override
    public List<CxProductStockLimit> checkCxProductStockLimitUnique(CxProductStockLimit cxProductStockLimit) {
        return cxProductStockLimitMapper.checkCxProductStockLimitUnique(cxProductStockLimit);
    }

    /**
     * 导入成型投产班次库存限定设置数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<CxProductStockLimit> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxProductStockLimit> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxProductStockLimit cxProductStockLimit = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, cxProductStockLimit);
            if (CollectionUtils.isNotEmpty(validated)) {
                cxProductStockLimit.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                cxProductStockLimit.setBaseVale(null);
                importList.add(cxProductStockLimit);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    cxProductStockLimitMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    CxProductStockLimit cxProductStockLimit = list.get(i);
                    // 错误记录跳过
                    if (cxProductStockLimit.getId() != null && cxProductStockLimit.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = "";
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertCxProductStockLimit(cxProductStockLimit);
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
