package com.zlt.aps.mp.setting.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.zlt.aps.enums.YesOrNoEnum;
import com.zlt.aps.maindata.mapper.MdmHolidayEntityMapper;
import com.zlt.aps.maindata.mapper.MdmWorkCalendarEntityMapper;
import com.zlt.aps.maindata.service.IMdmWorkCalendarService;
import com.zlt.aps.mp.api.domain.entity.MdmHoliday;
import com.zlt.aps.mp.api.domain.entity.MdmWorkCalendar;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmWorkCalendarController.java
 * 描    述：工作日历 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-12-03
 */
@Slf4j
@Api(tags = "工作日历")
@RestController
@RequestMapping("/mdmWorkCalendar")
public class MdmWorkCalendarController extends AbstractDocBizController<MdmWorkCalendar> {

    @Autowired
    private IMdmWorkCalendarService mdmWorkCalendarService;

    @Autowired
    private MdmWorkCalendarEntityMapper entityMapper;

    @Autowired
    private MdmHolidayEntityMapper holidayEntityMapper;

    /**
     * 查询工作日历列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmWorkCalendar queryVO) {
        TableDataInfo tableDataInfo = super.list(queryVO);
        List<MdmWorkCalendar> list = (List<MdmWorkCalendar>) tableDataInfo.getRows();
        Integer queryYear = queryVO.getYear();
        if (queryYear == null) {
            return tableDataInfo;
        }
        // 查询节假日配置
        List<MdmHoliday> mdmHolidayList = holidayEntityMapper.selectByYear(queryYear);
        Map<Date, String> holidayMap = new HashMap<>();
        if (CollectionUtils.isNotEmpty(mdmHolidayList)) {
            holidayMap = mdmHolidayList.stream().collect(Collectors.toMap(MdmHoliday::getHolidayDate, MdmHoliday::getHolidayNames));
        }
        for (MdmWorkCalendar mdmWorkCalendar : list) {
            Integer year = mdmWorkCalendar.getYear();
            Integer month = mdmWorkCalendar.getMonth();
            Integer day = mdmWorkCalendar.getDay();
            Date date = null;
            try {
                date = DateUtils.parseDate(year + "-" + String.format("%02d", month) + "-" + String.format("%02d", day), "yyyy-MM-dd");
            } catch (ParseException e) {
                log.error("日期转换异常", e);
            }
            mdmWorkCalendar.setCalendarTime(date);

            mdmWorkCalendar.setHolidayNames(holidayMap.getOrDefault(date, ""));
        }
        return tableDataInfo;
    }

    @Override
    protected String getOrderBy() {
        return "PROC_CODE, YEAR, MONTH, DAY ASC";
    }

    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmWorkCalendar.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmWorkCalendar billVO) {
        Date calendarTime = billVO.getProductionDate();
        if (calendarTime != null) {
            Calendar instance = Calendar.getInstance();
            instance.setTime(calendarTime);
            billVO.setYear(instance.get(Calendar.YEAR));
            billVO.setMonth(instance.get(Calendar.MONTH) + 1);
            billVO.setDay(instance.get(Calendar.DAY_OF_MONTH));
        }
        // 停产比例改成0
        if (YesOrNoEnum.NO.getCode().equals(billVO.getDayFlag())) {
            billVO.setRate(0);
        }
        // 比例如果是0，赋值成停产
        Integer rate = billVO.getRate();
        if (rate == 0) {
            billVO.setDayFlag(YesOrNoEnum.NO.getCode());
        }
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmWorkCalendar.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取工作日历详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmWorkCalendar getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入工作日历数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmWorkCalendar.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "工作日历", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmWorkCalendar queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmWorkCalendar> listExportData(MdmWorkCalendar obj) {
        QueryWrapper<MdmWorkCalendar> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService() {
        return mdmWorkCalendarService;
    }

    /**
     * 条件拼接
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmWorkCalendar> queryWrapper, MdmWorkCalendar queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("procCode")), "PROC_CODE", queryVO.getFieldValueByFieldName("procCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("year")), "YEAR", queryVO.getFieldValueByFieldName("year"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("month")), "MONTH", queryVO.getFieldValueByFieldName("month"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("day")), "DAY", queryVO.getFieldValueByFieldName("day"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("oneShiftFlag")), "ONE_SHIFT_FLAG", queryVO.getFieldValueByFieldName("oneShiftFlag"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("twoShiftFlag")), "TWO_SHIFT_FLAG", queryVO.getFieldValueByFieldName("twoShiftFlag"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("threeShiftFlag")), "THREE_SHIFT_FLAG", queryVO.getFieldValueByFieldName("threeShiftFlag"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("dayFlag")), "DAY_FLAG", queryVO.getFieldValueByFieldName("dayFlag"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("rate")), "RATE", queryVO.getFieldValueByFieldName("rate"));
    }

    @Override
    protected String getTypeCode() {
        return "MDM0104";
    }

    /**
     * 根据用户名称过滤出可查看的工序列表
     *
     * @param userName 用户名称
     * @return 结果
     */
    @ApiOperation("根据用户名称过滤出可查看的工序列表")
    @PostMapping("/selectProcCodeList")
    public AjaxResult selectProcCodeList(@RequestParam("userName") String userName) {
        if (StringUtils.isBlank(userName)) {
            userName = SecurityUtils.getUsername();
        }
        return AjaxResult.success(mdmWorkCalendarService.selectProcCodeList(userName));
    }

    /**
     * 生成全年工作日历
     *
     * @param entity 条件
     * @return 结果
     */
    @ApiOperation("生成全年工作日历")
    @PostMapping("/genAnnualPlan")
    public AjaxResult genAnnualPlan(@RequestBody MdmWorkCalendar entity) {
        return mdmWorkCalendarService.genAnnualPlan(entity);
    }
}
