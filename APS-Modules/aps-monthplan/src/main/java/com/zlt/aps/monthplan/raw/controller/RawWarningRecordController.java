package com.zlt.aps.monthplan.raw.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.maindata.enums.MsgTemplateEnums;
import com.zlt.aps.maindata.utils.MessageServiceUtils;
import com.zlt.aps.maindata.mapper.RawWarningRecordEntityMapper;
import com.zlt.aps.maindata.service.IRawWarningRecordService;
import com.zlt.aps.monthplan.api.domain.entity.RawWarningRecord;
import com.zlt.aps.monthplan.raw.service.IRawWarningService;
import com.zlt.common.utils.PubUtil;
import com.zlt.msg.message.domain.vo.MessageContext;
import com.zlt.msg.message.enums.MsgChannelEnums;
import com.zlt.msg.message.enums.MsgTypeEnums;
import lombok.extern.slf4j.Slf4j;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

import com.ruoyi.common.core.web.page.TableDataInfo;

import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService ;

/**
* Copyright (c) 2022, All rights reserved。
* 文件名称：RawWarningRecordController.java
* 描    述：原材料预警记录 控制层类：....
*@author zlt
*@date 2025-12-17
*@version 1.0
*
 *  修改记录：
*     修改时间：...
*     修 改 人：zlt
*     修改内容：...
*/
@Slf4j
@Api(tags = "原材料预警记录")
@RestController
@RequestMapping("/rawWarningRecord")
public class RawWarningRecordController extends AbstractDocBizController<RawWarningRecord> {

    @Autowired
    private IRawWarningRecordService rawWarningRecordService;

    @Autowired
    private RawWarningRecordEntityMapper entityMapper;

    @Autowired
    private IRawWarningService rawWarningService;

    @Autowired
    private MessageServiceUtils messageServiceAdapter;

    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;


    /**
     * 查询原材料预警记录列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody RawWarningRecord queryVO) {
        return super.list(queryVO);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }

    /**
     * 获取原材料预警记录详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public RawWarningRecord getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 导出列表
     */
    @Log(title = "原材料预警记录", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody RawWarningRecord queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<RawWarningRecord> listExportData(RawWarningRecord obj) {
        QueryWrapper<RawWarningRecord> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        return entityMapper.selectList(wrapper);
    }

    @Override
    protected IDocService getDocService(){
        return rawWarningRecordService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<RawWarningRecord> queryWrapper, RawWarningRecord queryVO) {
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("warningType")), "WARNING_TYPE", queryVO.getFieldValueByFieldName("warningType"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialCode")), "MATERIAL_CODE", queryVO.getFieldValueByFieldName("materialCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("materialDesc")), "MATERIAL_DESC", queryVO.getFieldValueByFieldName("materialDesc"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("warningLevel")), "WARNING_LEVEL", queryVO.getFieldValueByFieldName("warningLevel"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("warningTitle")), "WARNING_TITLE", queryVO.getFieldValueByFieldName("warningTitle"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("warningContent")), "WARNING_CONTENT", queryVO.getFieldValueByFieldName("warningContent"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("relatedMonth")), "RELATED_MONTH", queryVO.getFieldValueByFieldName("relatedMonth"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("relatedWeek")), "RELATED_WEEK", queryVO.getFieldValueByFieldName("relatedWeek"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("warningData")), "WARNING_DATA", queryVO.getFieldValueByFieldName("warningData"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("status")), "STATUS", queryVO.getFieldValueByFieldName("status"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("handler")), "HANDLER", queryVO.getFieldValueByFieldName("handler"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("handleTime")), "HANDLE_TIME", queryVO.getFieldValueByFieldName("handleTime"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("handleOpinion")), "HANDLE_OPINION", queryVO.getFieldValueByFieldName("handleOpinion"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("notified")), "NOTIFIED", queryVO.getFieldValueByFieldName("notified"));
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("notifyTime")), "NOTIFY_TIME", queryVO.getFieldValueByFieldName("notifyTime"));
    }


    @Override
    protected String getTypeCode(){
        return "S3521";
    }

    @PostMapping("/execute-usage-warning")
    @ApiOperation("执行用量偏差预警")
    public AjaxResult executeUsageWarning(@RequestParam("factoryCode") String factoryCode,
                                          @RequestParam("year") Integer year,
                                          @RequestParam("week") Integer week,
                                          @RequestParam("month") Integer month){
        try {
            int warningCount = rawWarningService.executeUsageDeviationWarning(factoryCode, year, week, month);
            String resultMessage = StringUtils.format(
                    I18nUtil.getMessage("raw.warning.usage.deviation.result"),
                    warningCount
            );
            if (warningCount > 0) {
                // 2. 发送预警通知
                sendUsageWarningNotification(factoryCode, year, week, month, warningCount, resultMessage);
            }
            return AjaxResult.success(resultMessage);
        }catch (Exception e) {
            String errorMessage = StringUtils.format(
                    I18nUtil.getMessage("raw.warning.usage.deviation.exception"),
                    e.getMessage()
            );
            return AjaxResult.error(errorMessage);
        }
    }

    /**
     * 发送用量偏差预警通知
     * @param factoryCode 工厂
     * @param year 年份
     * @param week 周次
     * @param month 月份
     * @param warningCount 预警数量
     * @param resultMessage 结果消息
     */
    private void sendUsageWarningNotification(String factoryCode, Integer year, Integer week, Integer month, int warningCount, String resultMessage) {
        // 构建跳转路径
        String billUrl = messageServiceAdapter.buildFrontendUrl("/rawMaterial/rawWarningRecord",
                "factoryCode", factoryCode,
                "year", year.toString(),
                "week", week.toString(),
                "month", month.toString());

        // 构建完整上下文
        MessageContext context = messageServiceAdapter.buildMessageContext(
                null,
                 this.getTypeCode(),
                 I18nUtil.getMessage("ui.data.column.rawWarningRecord.modelName"),
                null,
                 billUrl,
                 resultMessage,
                 SecurityUtils.getUsername(),
                 null
        );

        List<SysDictData> dictDataList = iSysDictDataCacheService.getType("biz_factory_name");
        String factoryName = dictDataList.stream().filter(dictData -> dictData.getDictValue().equals(factoryCode)).findFirst().get().getDictLabel();

        // 发送消息
        messageServiceAdapter.sendMessage(
                 MsgTemplateEnums.RAW_WARNING_RECORD.getCode(),
                 MsgTypeEnums.NOTICE.getCode(),
                 MsgChannelEnums.SYSTEM.getCode(),
                 null,
                 context,
                 factoryName,
                 year,
                 month,
                 week,
                 warningCount
        );
    }

    @PostMapping("/execute-new-material-warning")
    @ApiOperation("执行新材料预警")
    public AjaxResult executeNewMaterialWarning(@RequestParam("factoryCode") String factoryCode,
                                                @RequestParam("year") Integer year,
                                                @RequestParam("month") Integer month) {
        try{
            int warningCount = rawWarningService.executeNewMaterialWarning(factoryCode, year, month);
            String resultMessage = StringUtils.format(
                    I18nUtil.getMessage("raw.warning.new.material.result"),
                    warningCount
            );
            if (warningCount > 0) {
                // 2. 发送预警通知
                sendNewMaterialWarningNotification(factoryCode, year, month, warningCount, resultMessage);
            }
            return AjaxResult.success(resultMessage);
        }catch (Exception e) {
            String errorMessage = StringUtils.format(
                    I18nUtil.getMessage("raw.warning.new.material.exception"),
                    e.getMessage()
            );
            return AjaxResult.error(errorMessage);
        }
    }


    /**
     * 发送新材料预警通知
     * @param factoryCode 工厂
     * @param year 年份
     * @param month 月份
     * @param warningCount  预警数量
     * @param resultMessage 结果消息
     */
    private void sendNewMaterialWarningNotification(String factoryCode, Integer year, Integer month, int warningCount, String resultMessage) {
        //1.获取工厂国际化
        List<SysDictData> dictDataList = iSysDictDataCacheService.getType("biz_factory_name");
        String factoryName = dictDataList.stream().filter(dictData -> dictData.getDictValue().equals(factoryCode)).findFirst().get().getDictLabel();

        // 2.发送消息
        messageServiceAdapter.sendNotice(MsgTemplateEnums.RAW_NEW_WARNING.getCode() ,"" , factoryName,
                year,
                month,
                warningCount);
    }


    @PostMapping("/sync-actual-usage")
    @ApiOperation("同步周维度原材料实际用量数据")
    public AjaxResult syncActualUsage(@RequestParam("factoryCode") String factoryCode,
                                      @RequestParam("year") Integer year,
                                      @RequestParam("week") Integer week,
                                      @RequestParam("month") Integer month){
        return rawWarningService.syncWeekActualUsage(factoryCode, year, week, month);
    }
}
