package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.dto.CxStockLocationSortDto;
import com.zlt.aps.cx.entity.CxStockLocationSort;
import com.zlt.aps.cx.mapper.CxStockLocationSortMapper;
import com.zlt.aps.cx.service.CxStockLocationSortService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;


/**
 * 库存地点生产顺序Service业务层处理
 *
 * @author chen
 * @date 2021-07-22
 */
@Service
public class CxStockLocationSortServiceImpl extends ServiceImpl<CxStockLocationSortMapper, CxStockLocationSort> implements CxStockLocationSortService {
    @Autowired
    private CxStockLocationSortMapper cxStockLocationSortMapper;

    /**
     * 查询库存地点生产顺序
     *
     * @param id 库存地点生产顺序ID
     * @return 库存地点生产顺序
     */
    @Override
    public CxStockLocationSortDto selectCxStockLocationSortById(Long id) {
        return cxStockLocationSortMapper.selectCxStockLocationSortById(id);
    }

    /**
     * 查询库存地点生产顺序列表
     *
     * @param cxStockLocationSort 库存地点生产顺序
     * @return 库存地点生产顺序
     */
    @Override
    public List<CxStockLocationSortDto> selectCxStockLocationSortList(CxStockLocationSort cxStockLocationSort) {
        return cxStockLocationSortMapper.selectCxStockLocationSortList(cxStockLocationSort);
    }

    /**
     * 新增库存地点生产顺序
     *
     * @param cxStockLocationSort 库存地点生产顺序
     * @return 结果
     */
    @Override
    public int insertCxStockLocationSort(CxStockLocationSort cxStockLocationSort) {
        cxStockLocationSort.setBaseVale(null);
        return cxStockLocationSortMapper.insertCxStockLocationSort(cxStockLocationSort);
    }

    /**
     * 修改库存地点生产顺序
     *
     * @param cxStockLocationSort 库存地点生产顺序
     * @return 结果
     */
    @Override
    public int updateCxStockLocationSort(CxStockLocationSort cxStockLocationSort) {
        cxStockLocationSort.setBaseVale(cxStockLocationSort.getId());
        return cxStockLocationSortMapper.updateCxStockLocationSort(cxStockLocationSort);
    }

    /**
     * 批量删除库存地点生产顺序
     *
     * @param ids 需要删除的库存地点生产顺序ID
     * @return 结果
     */
    @Override
    public int deleteCxStockLocationSortByIds(Long[] ids) {
        return cxStockLocationSortMapper.deleteCxStockLocationSortByIds(ids);
    }

    /**
     * 删除库存地点生产顺序信息
     *
     * @param id 库存地点生产顺序ID
     * @return 结果
     */
    @Override
    public int deleteCxStockLocationSortById(Long id) {
        return cxStockLocationSortMapper.deleteCxStockLocationSortById(id);
    }

    /**
     * 校验记录唯一性
     */
    @Override
    public String checkCxStockLocationSortUnique(CxStockLocationSort cxStockLocationSort) {
        if (cxStockLocationSort == null) {
            return UserConstants.NOT_UNIQUE;
        }
        int num = cxStockLocationSortMapper.checkUnique(cxStockLocationSort);
        if (num > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<CxStockLocationSortDto> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxStockLocationSort> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getStockLocation()==null?"":a.getStockLocation()), Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            CxStockLocationSortDto dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getStockLocation()==null?"":dto.getStockLocation());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.stockLocationSort.stockLocation");
                message=String.format(message,columnName);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);
            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                CxStockLocationSort newEntity = new CxStockLocationSort();
                BeanUtils.copyProperties(dto, newEntity);
                newEntity.setBaseVale(null);
                newList.add(newEntity);
            }
        }

        //新集合操作（更新或插入操作）
        if (CollectionUtils.isNotEmpty(list)) {
            try {
                //勾选更新记录，调用mergeOrInsert
                if (updateSupport && CollectionUtils.isNotEmpty(newList)) {
                    successNum = newList.size();
                    cxStockLocationSortMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        CxStockLocationSortDto dto = list.get(i);
                        //过滤错误的记录
                        if (dto.getId() != null && dto.getId() == -999L) {
                            continue;
                        }
                        CxStockLocationSort newItem = new CxStockLocationSort();
                        BeanUtils.copyProperties(dto, newItem);
                        newItem.setBaseVale(null);

                        int num = cxStockLocationSortMapper.checkUnique(newItem);
                        if (num <= 0) {
                            successNum++;
                            saveOrUpdate(newItem);
                        } else {
                            failureNum++;
                            addImportErrorLog(importLogId, i + 2, I18nUtil.getMessage("ui.error.message.quota.unique"), importErrorLogs);
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
        }
        //返回提示信息及错误集合
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

}
