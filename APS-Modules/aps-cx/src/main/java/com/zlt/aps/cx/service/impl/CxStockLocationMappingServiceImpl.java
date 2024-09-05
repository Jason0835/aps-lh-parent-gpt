package com.zlt.aps.cx.service.impl;

import java.util.List;
import com.ruoyi.common.core.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.zlt.aps.cx.mapper.CxStockLocationMappingMapper;
import com.zlt.aps.cx.api.domain.entity.CxStockLocationMapping;
import com.zlt.aps.cx.service.CxStockLocationMappingService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import java.util.ArrayList;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 库存地点映射Service业务层处理
 * 
 * @author zlt
 * @date 2021-11-15
 */
@Service
public class CxStockLocationMappingServiceImpl implements CxStockLocationMappingService
{
    @Autowired
    private CxStockLocationMappingMapper cxStockLocationMappingMapper;

    /**
     * 查询库存地点映射
     * 
     * @param id 库存地点映射ID
     * @return 库存地点映射
     */
    @Override
    public CxStockLocationMapping selectCxStockLocationMappingById(Long id)
    {
        return cxStockLocationMappingMapper.selectCxStockLocationMappingById(id);
    }

    /**
     * 查询库存地点映射列表
     * 
     * @param cxStockLocationMapping 库存地点映射
     * @return 库存地点映射
     */
    @Override
    public List<CxStockLocationMapping> selectCxStockLocationMappingList(CxStockLocationMapping cxStockLocationMapping)
    {
        return cxStockLocationMappingMapper.selectCxStockLocationMappingList(cxStockLocationMapping);
    }

    /**
     * 新增库存地点映射
     * 
     * @param cxStockLocationMapping 库存地点映射
     * @return 结果
     */
    @Override
    public int insertCxStockLocationMapping(CxStockLocationMapping cxStockLocationMapping)
    {
        cxStockLocationMapping.setBaseVale(null);
        return cxStockLocationMappingMapper.insertCxStockLocationMapping(cxStockLocationMapping);
    }

    /**
     * 修改库存地点映射
     * 
     * @param cxStockLocationMapping 库存地点映射
     * @return 结果
     */
    @Override
    public int updateCxStockLocationMapping(CxStockLocationMapping cxStockLocationMapping)
    {
        cxStockLocationMapping.setBaseVale(cxStockLocationMapping.getId());
        return cxStockLocationMappingMapper.updateCxStockLocationMapping(cxStockLocationMapping);
    }

    /**
     * 批量删除库存地点映射
     * 
     * @param ids 需要删除的库存地点映射ID
     * @return 结果
     */
    @Override
    public int deleteCxStockLocationMappingByIds(Long[] ids)
    {
        return cxStockLocationMappingMapper.deleteCxStockLocationMappingByIds(ids);
    }

    /**
     * 删除库存地点映射信息
     * 
     * @param id 库存地点映射ID
     * @return 结果
     */
    @Override
    public int deleteCxStockLocationMappingById(Long id)
    {
        return cxStockLocationMappingMapper.deleteCxStockLocationMappingById(id);
    }

    /**
     * 校验库存地点映射唯一性
     */
    @Override
    public String checkCxStockLocationMappingUnique(CxStockLocationMapping cxStockLocationMapping) {
        if (cxStockLocationMapping == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<CxStockLocationMapping> list = cxStockLocationMappingMapper.ckeckCxStockLocationMappingUnique(cxStockLocationMapping);
        if (CollectionUtils.isNotEmpty(list)) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入库存地点映射数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<CxStockLocationMapping> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxStockLocationMapping> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxStockLocationMapping cxStockLocationMapping = list.get(i);
            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, errorNum, cxStockLocationMapping);
            if (CollectionUtils.isNotEmpty(validated)) {
                cxStockLocationMapping.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                cxStockLocationMapping.setBaseVale(null);
                importList.add(cxStockLocationMapping);
            }
        }

        try {
            //勾选更新记录，调用mergeOrInsert
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                successNum = importList.size();
                    cxStockLocationMappingMapper.mergeSql(importList);
            } else {
                //唯一则新增
                for (int i = 0; i < list.size(); i++) {
                    CxStockLocationMapping cxStockLocationMapping = list.get(i);
                    // 错误记录跳过
                    if (cxStockLocationMapping.getId() != null && cxStockLocationMapping.getId().equals(-999L)) {
                        continue;
                    }
                    String unique = this.checkCxStockLocationMappingUnique(cxStockLocationMapping);
                    if (UserConstants.UNIQUE.equals(unique)) {
                        successNum++;
                        this.insertCxStockLocationMapping(cxStockLocationMapping);
                    } else {
                        failureNum++;
                        addImportErrorLog(importLogId, i + 2,
                                I18nUtil.getMessage("ui.data.column.mouthPlate.alreadyExists"), importErrorLogs);
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
