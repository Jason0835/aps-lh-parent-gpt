package com.zlt.aps.cx.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ImportUtil;

import com.zlt.aps.cx.mapper.entity.CxHolidaySettingMapper;
import com.zlt.aps.cx.service.CxHolidaySettingService;
import com.zlt.aps.cxlh.cx.api.domain.dto.CxHolidaySettingDto;
import com.zlt.aps.cxlh.cx.api.domain.entity.CxHolidaySetting;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.aps.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 假日设定Service业务层处理
 *
 * @author chen
 * @date 2021-06-30
 */
@Service
public class CxHolidaySettingServiceImpl extends ServiceImpl<CxHolidaySettingMapper, CxHolidaySetting> implements CxHolidaySettingService {
    @Autowired
    private CxHolidaySettingMapper cxHolidaySettingMapper;

    /**
     * 查询成型假日设定列表
     *
     * @param setting 成型假日设定
     * @return 成型假日设定集合
     */
    @Override
    public List<CxHolidaySettingDto> selectCxHolidaySettingList(CxHolidaySetting setting) {
        return cxHolidaySettingMapper.selectCxHolidaySettingList(setting);
    }

    /**
     * 查询成型假日设定
     *
     * @param id 成型假日设定ID
     * @return 成型假日设定
     */
    @Override
    public CxHolidaySetting selectCxHolidaySettingById(Long id) {
        LambdaQueryWrapper<CxHolidaySetting> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(CxHolidaySetting::getDelFlag, ApsConstant.DEL_FLAG_NORMAL);
        wrapper.eq(CxHolidaySetting::getId, id);
        return cxHolidaySettingMapper.selectOne(wrapper);
    }

    /**
     * 保存成型假日设定 不提供修改功能
     *
     * @param dto 成型假日设定
     */
    @Override
    public void saveCxHolidaySetting(CxHolidaySettingDto dto) {
        // 通过日历类获取假日开始时间和结束时间之间的所有日期，批量新增到数据库中
        Calendar startCalendar = Calendar.getInstance();
        Calendar endCalendar = Calendar.getInstance();
        try {
            startCalendar.setTime(DateUtils.parseDate(dto.getStartTime(), "yyyy-MM-dd"));
            endCalendar.setTime(DateUtils.parseDate(dto.getEndTime(), "yyyy-MM-dd"));
        } catch (ParseException e) {
            e.printStackTrace();
        }
        int num = endCalendar.get(Calendar.DAY_OF_YEAR) - startCalendar.get(Calendar.DAY_OF_YEAR);

        ArrayList<CxHolidaySetting> list = new ArrayList<>();
        for (int i = 0; i <= num; i++) {
            CxHolidaySetting cxHolidaySetting = new CxHolidaySetting();
            cxHolidaySetting.setHolidayDay(startCalendar.getTime());
//            cxHolidaySetting.setHolidayName(dto.getHolidayName());
            cxHolidaySetting.setRemark(dto.getRemark());
            cxHolidaySetting.setBaseVale(cxHolidaySetting.getId());
            list.add(cxHolidaySetting);
            startCalendar.add(Calendar.DAY_OF_YEAR, 1);
        }
        saveBatch(list);
    }

    /**
     * 批量删除成型假日设定
     *
     * @param ids 需要删除的成型假日设定ID
     */
    @Override
    public void deleteCxHolidaySettingByIds(Long[] ids) {
        if (ids == null) {
            return;
        }
        List<CxHolidaySetting> list = new ArrayList<>();
        for (Long id : ids) {
            CxHolidaySetting setting = new CxHolidaySetting();
            setting.setId(id);
            setting.setDelFlag(ApsConstant.DEL_FLAG_DEL);
            setting.setBaseVale(setting.getId());
            list.add(setting);
        }
        updateBatchById(list);
    }

    /**
     * 校验记录唯一性
     *
     * @param setting 要校验记录
     * @return 查询到的结果
     */
    @Override
    public List<CxHolidaySettingDto> checkUnique(CxHolidaySetting setting) {
        return cxHolidaySettingMapper.checkUnique(setting);
    }

    /**
     * 导入数据
     */
    @Override
    public AjaxResult importData(List<CxHolidaySettingDto> list, boolean updateSupport, Long importLogId) {

        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<CxHolidaySetting> newList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();

        //按业务主键分组
        Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getHolidayDay()+"", Collectors.counting()));

        //公共校验（非空校验、长度校验等）
        for (int i = 0; i < list.size(); i++) {
            int j = i + 2;
            CxHolidaySettingDto dto = list.get(i);

            //重复记录校验
            Long hasValue = groupMap.get(dto.getHolidayDay()+"");
            if (hasValue > 1) {
                failureNum++;
                dto.setId(-999L);
                String message = I18nUtil.getMessage("ui.data.column.all.conflictRecord");
                String columnName = I18nUtil.getMessage("ui.data.column.holiday.holidayDay");
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
                CxHolidaySetting newEntity = new CxHolidaySetting();
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
                    cxHolidaySettingMapper.mergeSql(newList);
                } else {
                    //唯一则新增
                    for (int i = 0; i < list.size(); i++) {
                        CxHolidaySettingDto dto = list.get(i);
                        //过滤错误的记录
                        if (dto.getId() != null && dto.getId() == -999L) {
                            continue;
                        }
                        CxHolidaySetting newItem = new CxHolidaySetting();
                        BeanUtils.copyProperties(dto, newItem);
                        newItem.setBaseVale(null);

                        List<CxHolidaySettingDto> exist = cxHolidaySettingMapper.checkUnique2(newItem);
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
