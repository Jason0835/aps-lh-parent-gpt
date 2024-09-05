package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.dto.CxScheduleLimitDto;
import com.zlt.aps.cx.entity.CxScheduleLimit;
import com.zlt.aps.cx.mapper.CxScheduleLimitMapper;
import com.zlt.aps.cx.service.CxScheduleLimitService;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * <p>
 * 成型排产限制信息维护 服务实现类
 * </p>
 *
 * @author chenxueyuan
 * @since 2021-06-16
 */
@Service
public class CxScheduleLimitServiceImpl extends ServiceImpl<CxScheduleLimitMapper, CxScheduleLimit> implements CxScheduleLimitService {

    @Autowired
    private CxScheduleLimitMapper cxScheduleLimitMapper;

    /**
     * 查询成型排产限制信息维护列表
     *
     * @param limit 成型排产限制信息维护
     * @return 成型排产限制信息维护集合
     */
    @Override
    public List<CxScheduleLimitDto> selectLimitList(CxScheduleLimit limit) {
        return cxScheduleLimitMapper.selectLimitList(limit);
    }

    /**
     * 查询成型排产限制信息维护列表
     *
     * @param id 要查询的id
     * @return 成型排产限制信息维护集合
     */
    @Override
    public CxScheduleLimit selectLimitById(Long id) {
        LambdaQueryWrapper<CxScheduleLimit> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxScheduleLimit::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(CxScheduleLimit::getId, id);
        return cxScheduleLimitMapper.selectOne(wrapper);
    }

    /**
     * 新增或更新成型排产限制信息维护
     *
     * @param limit 成型排产限制信息维护
     */
    @Override
    public void saveLimit(CxScheduleLimit limit) {
        BigDecimal minimun = limit.getTireAvgLhStockMinimun();
        BigDecimal maximun = limit.getTireAveLhStockMaximun();
        if (ObjectUtils.allNotNull(minimun, maximun) && minimun.longValue() > maximun.longValue()) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.minMoreThanMax"));
        }
        // 校验唯一性
        List<CxScheduleLimitDto> dtos = cxScheduleLimitMapper.checkUnique(limit);
        if (dtos.size() > 0) {
            throw new RuntimeException(I18nUtil.getMessage("ui.error.message.limit.unique"));
        }
        limit.setBaseVale(limit.getId());
        saveOrUpdate(limit);
    }

    /**
     * 批量删除成型排产限制信息维护
     *
     * @param ids 需要删除的成型排产限制信息维护ID
     */
    @Override
    public void deleteLimitByIds(Long[] ids) {
        if (ids == null) {
            return;
        }
        List<CxScheduleLimit> list = new ArrayList<>();
        for (Long id : ids) {
            CxScheduleLimit limit = new CxScheduleLimit();
            limit.setId(id);
            limit.setDelFlag(ApsConstant.DEL_FLAG_DEL);
            limit.setBaseVale(limit.getId());
            list.add(limit);
        }
        updateBatchById(list);
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<CxScheduleLimitDto> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxScheduleLimit> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> (a.getMachineType()+a.getSpecDimension()), Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            CxScheduleLimitDto dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getMachineType()+dto.getSpecDimension());
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.cx.machine.type");
                String columnName2 = I18nUtil.getMessage("ui.data.column.cx.limit.specDimension");
                message=String.format(message,columnName+"+"+columnName2);
                addImportErrorLog(importLogId, i + 2,message, importErrorLogs);
                continue;
            }

            List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, dto);
            if (CollectionUtils.isNotEmpty(validated)) {
                dto.setId(-999L);
                failureNum++;
                importErrorLogs.addAll(validated);
            } else{

                if(dto.getTireAvgLhStockMinimun()!=null && dto.getTireAveLhStockMaximun()!=null && dto.getTireAvgLhStockMinimun().compareTo( dto.getTireAveLhStockMaximun())>0){
                    failureNum++;
                    dto.setId(-999L);
                    addImportErrorLog(importLogId, i + 2,
                            I18nUtil.getMessage("ui.error.message.minMoreThanMax"), importErrorLogs);
                    continue;
                }

                CxScheduleLimit newEntity = new CxScheduleLimit();
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
                    cxScheduleLimitMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        CxScheduleLimitDto dto = list.get(i);
                        //过滤错误的记录
                        if (dto.getId() != null && dto.getId() == -999L) {
                            continue;
                        }
                        CxScheduleLimit newItem = new CxScheduleLimit();
                        BeanUtils.copyProperties(dto, newItem);
                        newItem.setBaseVale(null);

                        List<CxScheduleLimitDto> exist = cxScheduleLimitMapper.checkUnique(newItem);
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
