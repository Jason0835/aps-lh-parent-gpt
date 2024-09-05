package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.dto.CxMonthStockDto;
import com.zlt.aps.cx.api.domain.entity.CxProductConstructionInfo;
import com.zlt.aps.cx.entity.CxMonthStock;
import com.zlt.aps.cx.mapper.CxMonthStockMapper;
import com.zlt.aps.cx.mapper.CxProductConstructionInfoMapper;
import com.zlt.aps.cx.service.CxMonthStockService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * monthStockService业务层处理
 *
 * @author chen
 * @date 2021-06-17
 */
@Service
public class CxMonthStockServiceImpl extends ServiceImpl<CxMonthStockMapper, CxMonthStock> implements CxMonthStockService {
    @Autowired
    private CxMonthStockMapper cxMonthStockMapper;

    @Autowired
    private CxProductConstructionInfoMapper cxProductConstructionInfoMapper;

    /**
     * 查询成型月结库存列表
     *
     * @param dto 成型月结库存
     * @return 成型月结库存集合
     */
    @Override
    public List<CxMonthStockDto> selectCxMonthStockList(CxMonthStockDto dto) {
        if(StringUtils.isNotEmpty(dto.getEndStockMonth())){
            String endStockMonth=  dto.getEndStockMonth();
            Calendar cal = Calendar.getInstance();
            cal.setTime(DateUtils.parseDate(endStockMonth));
            cal.add(Calendar.MONTH, 1);
            endStockMonth= DateUtils.parseDateToStr("yyyy-MM",cal.getTime());
            dto.setEndStockMonth(endStockMonth);
        }
        return cxMonthStockMapper.selectCxMonthStockList(dto);
    }

    /**
     * 查询成型月结库存
     *
     * @param id 成型月结库存ID
     * @return 成型月结库存
     */
    @Override
    public CxMonthStock selectCxMonthStockById(Long id) {
        LambdaQueryWrapper<CxMonthStock> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxMonthStock::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(CxMonthStock::getId, id);
        return cxMonthStockMapper.selectOne(wrapper);
    }

    /**
     * 修改/新增成型月结库存（id为空 新增，否则修改）
     *
     * @param monthStock 成型月结库存
     */
    @Override
    public void saveCxMonthStock(CxMonthStock monthStock) {
        monthStock.setBaseVale(monthStock.getId());
        saveOrUpdate(monthStock);
    }

    /**
     * 批量删除成型月结库存
     *
     * @param ids 需要删除的成型月结库存ID
     */
    @Override
    public void deleteCxMonthStockByIds(Long[] ids) {
        if (ids == null) {
            return;
        }
        List<CxMonthStock> list = new ArrayList<>();
        for (Long id : ids) {
            CxMonthStock monthStock = new CxMonthStock();
            monthStock.setId(id);
            monthStock.setDelFlag(ApsConstant.DEL_FLAG_DEL);
            monthStock.setBaseVale(monthStock.getId());
            list.add(monthStock);
        }
        updateBatchById(list);
    }

    /**
     * 校验唯一性
     *
     * @param stock 要校验的记录
     * @return 是否唯一
     */
    @Override
    public String checkUnique(CxMonthStock stock) {
        if (stock == null) {
            return UserConstants.NOT_UNIQUE;
        }
        List<CxMonthStock> unique = cxMonthStockMapper.checkCxMonthStockUnique(stock);
        if (unique.size() > 0) {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<CxMonthStockDto> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxMonthStock> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getStockMonth()+a.getEmbryoCode()+a.getBomDataVersion(), Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            CxMonthStockDto dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getStockMonth()+dto.getEmbryoCode()+dto.getBomDataVersion());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.cx.monthStock.stockMonth");
                String columnName2 = I18nUtil.getMessage("ui.data.column.cx.monthStock.embryoCode");
                String columnName3 = I18nUtil.getMessage("ui.data.column.productStatus.bomDataVersion");
                message=String.format(message,columnName+"+"+columnName2+"+"+columnName3);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);
            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{
                CxMonthStock newEntity = new CxMonthStock();
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
                    cxMonthStockMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        CxMonthStockDto dto = list.get(i);
                        //过滤错误的记录
                        if (dto.getId() != null && dto.getId() == -999L) {
                            continue;
                        }
                        CxMonthStock newItem = new CxMonthStock();
                        BeanUtils.copyProperties(dto, newItem);
                        newItem.setBaseVale(null);

                        List<CxMonthStock> exist = cxMonthStockMapper.checkCxMonthStockUnique(newItem);
                        if (CollectionUtils.isEmpty(exist)) {
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
