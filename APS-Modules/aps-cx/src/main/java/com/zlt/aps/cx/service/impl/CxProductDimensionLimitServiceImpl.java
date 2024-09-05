package com.zlt.aps.cx.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.cx.mapper.CxProductDimensionLimitMapper;
import com.zlt.aps.cx.api.domain.entity.CxProductDimensionLimit;
import com.zlt.aps.cx.service.CxProductDimensionLimitService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import java.util.ArrayList;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 成型投产班次同寸口硫化班次限定设置Service业务层处理
 * 
 * @author zlt
 * @date 2022-01-08
 */
@Service
public class CxProductDimensionLimitServiceImpl implements CxProductDimensionLimitService
{
    @Autowired
    private CxProductDimensionLimitMapper cxProductDimensionLimitMapper;

    /**
     * 查询成型投产班次同寸口硫化班次限定设置
     * 
     * @param id 成型投产班次同寸口硫化班次限定设置ID
     * @return 成型投产班次同寸口硫化班次限定设置
     */
    @Override
    public CxProductDimensionLimit selectCxProductDimensionLimitById(Long id)
    {
        return cxProductDimensionLimitMapper.selectCxProductDimensionLimitById(id);
    }

    /**
     * 查询成型投产班次同寸口硫化班次限定设置列表
     * 
     * @param cxProductDimensionLimit 成型投产班次同寸口硫化班次限定设置
     * @return 成型投产班次同寸口硫化班次限定设置
     */
    @Override
    public List<CxProductDimensionLimit> selectCxProductDimensionLimitList(CxProductDimensionLimit cxProductDimensionLimit)
    {
        return cxProductDimensionLimitMapper.selectCxProductDimensionLimitList(cxProductDimensionLimit);
    }

    /**
     * 新增成型投产班次同寸口硫化班次限定设置
     * 
     * @param cxProductDimensionLimit 成型投产班次同寸口硫化班次限定设置
     * @return 结果
     */
    @Override
    public int insertCxProductDimensionLimit(CxProductDimensionLimit cxProductDimensionLimit)
    {
        cxProductDimensionLimit.setBaseVale(null);
        return cxProductDimensionLimitMapper.insertCxProductDimensionLimit(cxProductDimensionLimit);
    }

    /**
     * 修改成型投产班次同寸口硫化班次限定设置
     * 
     * @param cxProductDimensionLimit 成型投产班次同寸口硫化班次限定设置
     * @return 结果
     */
    @Override
    public int updateCxProductDimensionLimit(CxProductDimensionLimit cxProductDimensionLimit)
    {
        cxProductDimensionLimit.setBaseVale(cxProductDimensionLimit.getId());
        return cxProductDimensionLimitMapper.updateCxProductDimensionLimit(cxProductDimensionLimit);
    }

    /**
     * 批量删除成型投产班次同寸口硫化班次限定设置
     * 
     * @param ids 需要删除的成型投产班次同寸口硫化班次限定设置ID
     * @return 结果
     */
    @Override
    public int deleteCxProductDimensionLimitByIds(Long[] ids)
    {
        return cxProductDimensionLimitMapper.deleteCxProductDimensionLimitByIds(ids);
    }

    /**
     * 删除成型投产班次同寸口硫化班次限定设置信息
     * 
     * @param id 成型投产班次同寸口硫化班次限定设置ID
     * @return 结果
     */
    @Override
    public int deleteCxProductDimensionLimitById(Long id)
    {
        return cxProductDimensionLimitMapper.deleteCxProductDimensionLimitById(id);
    }

    /**
     * 校验成型投产班次同寸口硫化班次限定设置唯一性
     */
    @Override
    public String checkCxProductDimensionLimitUnique(CxProductDimensionLimit cxProductDimensionLimit) {
        List<CxProductDimensionLimit> list = cxProductDimensionLimitMapper.checkCxProductDimensionLimitUnique(cxProductDimensionLimit);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入成型投产班次同寸口硫化班次限定设置数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<CxProductDimensionLimit> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxProductDimensionLimit> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxProductDimensionLimit cxProductDimensionLimit = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, cxProductDimensionLimit);
            if (CollectionUtils.isNotEmpty(validated)) {
                cxProductDimensionLimit.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                cxProductDimensionLimit.setBaseVale(null);
                importList.add(cxProductDimensionLimit);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    cxProductDimensionLimitMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    CxProductDimensionLimit cxProductDimensionLimit = list.get(i);
                    // 错误记录跳过
                    if (cxProductDimensionLimit.getId() != null && cxProductDimensionLimit.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkCxProductDimensionLimitUnique(cxProductDimensionLimit);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertCxProductDimensionLimit(cxProductDimensionLimit);
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
