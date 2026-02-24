package com.zlt.aps.mdm.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.vo.ImportContext;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.log.annotation.Log;
import com.ruoyi.common.log.enums.BusinessType;
import com.tlt.aps.constant.StringConstant;
import com.zlt.aps.mdm.mapper.MdmProductConstructionEntityMapper;
import com.zlt.aps.mdm.service.IMdmProductConstructionService;
import com.zlt.aps.mdm.api.domain.entity.MdmProductConstruction;
import com.zlt.aps.mdm.api.domain.vo.MdmProductConstructionImportVo;
import com.zlt.bill.common.controller.AbstractDocBizController;
import com.zlt.bill.common.service.IDocService;
import com.zlt.common.utils.ImportExcelUtils;
import com.zlt.common.utils.PubUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Copyright (c) 2022, All rights reserved。
 * 文件名称：MdmProductConstructionController.java
 * 描    述：SAP与施工对照 控制层类：....
 *
 * @author zlt
 * @version 1.0
 * <p>
 * 修改记录：
 * 修改时间：...
 * 修 改 人：zlt
 * 修改内容：...
 * @date 2025-02-25
 */
@Slf4j
@Api(tags = "SAP与施工对照")
@RestController
@RequestMapping("/mdmProductConstruction")
public class MdmProductConstructionController extends AbstractDocBizController<MdmProductConstruction> {

    @Autowired
    private IMdmProductConstructionService mdmProductConstructionService;

    @Autowired
    private MdmProductConstructionEntityMapper mdmProductConstructionEntityMapper;

    /**
     * 查询SAP与施工对照列表
     */
    @ApiOperation("查询列表")
    @PostMapping("/list")
    @Override
    public TableDataInfo list(@RequestBody MdmProductConstruction queryVO) {
        this.startPage(this.getOrderBy());
        QueryWrapper<MdmProductConstruction> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, queryVO);
        List<MdmProductConstruction> list;
        String isCheckAbnormal = queryVO.getIsCheckAbnormal();
        if (PubUtil.isNotEmpty(isCheckAbnormal) && StringConstant.ONE.equals(isCheckAbnormal)) {
            list = mdmProductConstructionEntityMapper.selectMdmProductConstructionList(wrapper);
        } else {
            list = mdmProductConstructionEntityMapper.selectList(wrapper);
        }
        return getDataTable(list);
    }

    @Override
    protected String getOrderBy() {
        return "create_time desc";
    }


    /**
     * 保存
     */
    @Log(title = "ui.data.column.mdmProductConstruction.modelName", businessType = BusinessType.INSERT_OR_UPDATE)
    @ApiOperation("保存")
    @PostMapping("/save")
    @Override
    public AjaxResult save(@RequestBody MdmProductConstruction billVO) {
        return super.save(billVO);
    }

    /**
     * 删除
     */
    @Log(title = "ui.data.column.mdmProductConstruction.modelName", businessType = BusinessType.DELETE)
    @ApiOperation("删除")
    @DeleteMapping("/remove")
    @Override
    public AjaxResult removeByIds(@RequestBody List<Long> ids) {
        return super.removeByIds(ids);
    }


    /**
     * 获取SAP与施工对照详细信息
     */
    @ApiOperation("获取详细信息")
    @GetMapping(value = "/{billId}")
    @Override
    public MdmProductConstruction getInfo(@PathVariable("billId") Long billId) {
        return super.getInfo(billId);
    }


    /**
     * 根据集合导入SAP与施工对照数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmProductConstruction.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入数据")
    @PostMapping("/importData")
    @Override
    public AjaxResult importData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        return super.importData(importContext, updateSupport);
    }

    /**
     * 导出列表
     */
    @Log(title = "SAP与施工对照", businessType = BusinessType.EXPORT)
    @ApiOperation("导入数据")
    @PostMapping("/exportData/{fileName}")
    @Override
    public byte[] exportData(@RequestBody MdmProductConstruction queryVO, @PathVariable("fileName") String fileName,
                             HttpServletResponse response) throws IOException {
        return super.exportData(queryVO, fileName, response);
    }

    @Override
    protected List<MdmProductConstruction> listExportData(MdmProductConstruction obj) {
        QueryWrapper<MdmProductConstruction> wrapper = new QueryWrapper<>();
        this.builderCondition(wrapper, obj);
        String isCheckAbnormal = obj.getIsCheckAbnormal();
        if (PubUtil.isNotEmpty(isCheckAbnormal) && StringConstant.ONE.equals(isCheckAbnormal)) {
            return mdmProductConstructionEntityMapper.selectMdmProductConstructionList(wrapper);
        } else {
            return mdmProductConstructionEntityMapper.selectList(wrapper);
        }
    }

    @Override
    protected IDocService getDocService() {
        return mdmProductConstructionService;
    }

    /**
     * 条件拼接
     *
     * @param queryWrapper
     * @param queryVO
     */
    @Override
    protected void builderCondition(QueryWrapper<MdmProductConstruction> queryWrapper, MdmProductConstruction queryVO) {
        String tableAlias = "";
        String isCheckAbnormal = queryVO.getIsCheckAbnormal();
        if (PubUtil.isNotEmpty(isCheckAbnormal) && StringConstant.ONE.equals(isCheckAbnormal)) {
            tableAlias = "t.";
            queryWrapper.eq(tableAlias + "IS_DELETE", StringConstant.ZERO);
        }
        queryWrapper.eq(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("factoryCode")), tableAlias + "FACTORY_CODE", queryVO.getFieldValueByFieldName("factoryCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("productCode")), tableAlias + "PRODUCT_CODE", queryVO.getFieldValueByFieldName("productCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("specCode")), tableAlias + "SPEC_CODE", queryVO.getFieldValueByFieldName("specCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("constructionCode")), tableAlias + "CONSTRUCTION_CODE", queryVO.getFieldValueByFieldName("constructionCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("embryoCode")), tableAlias + "EMBRYO_CODE", queryVO.getFieldValueByFieldName("embryoCode"));
        queryWrapper.like(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("bomVersion")), tableAlias + "BOM_VERSION", queryVO.getFieldValueByFieldName("bomVersion"));
        queryWrapper.gt(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("createStartTime")), tableAlias + "CREATE_TIME", queryVO.getFieldValueByFieldName("createStartTime"));
        queryWrapper.lt(PubUtil.isNotEmpty(queryVO.getFieldValueByFieldName("createEndTime")), tableAlias + "CREATE_TIME", queryVO.getFieldValueByFieldName("createEndTime"));
    }

    @Override
    protected String getTypeCode() {
        return "0108";
    }

    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;

    /**
     * 导入客户格式数据
     *
     * @param importContext 导入上下文
     * @param updateSupport 已存在记录是否更新
     * @return 结果
     */
    @Log(title = "ui.data.column.mdmProductConstruction.modelName", businessType = BusinessType.IMPORT)
    @ApiOperation("导入客户格式数据")
    @PostMapping("/importOfflineData")
    public AjaxResult importOfflineData(@RequestBody ImportContext importContext, @RequestParam("updateSupport") boolean updateSupport) throws Exception {
        Date beginTime = DateUtils.getNowDate();
        ImportLog importLog = ImportExcelUtils.getImportLogAndUploadFile(importContext.getFileBytes(), importContext.getImportFilePath(), importContext.getProcedureCode(), importContext.getFunctionName(), importContext.getOriFileName(), 1);
        importLog = this.iImportLogService.add(importLog);
        ExcelUtil<MdmProductConstructionImportVo> util = new ExcelUtil<>(MdmProductConstructionImportVo.class);
        InputStream is = new ByteArrayInputStream(importContext.getFileBytes());
        List<MdmProductConstructionImportVo> list = util.importExcel(is);
        AjaxResult ajaxResult = mdmProductConstructionService.importOfflineData(list, updateSupport, importLog.getId());
        Date endTime = DateUtils.getNowDate();
        importLog.setRowCount(list.size());
        importLog.setBeginTime(beginTime);
        importLog.setEndTime(endTime);
        importLog.setSpendTime(DateUtils.getDiffTime(endTime, beginTime));
        ImportExcelUtils.updateImportLogAndFormatMsg(importLog, ajaxResult, this.iImportLogService);
        ImportExcelUtils.saveImportErrorLogs(ajaxResult, this.iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 接口同步SAP与施工关系
     */
    @ApiOperation("接口同步SAP与施工关系")
    @PostMapping("/syncProductConstructionInfo")
    public AjaxResult syncProductConstructionInfo() {
        // 获取最大版本
        String dataVersion = getSyncDataVersion();
        // 获取最大时间
        String maxTime = mdmProductConstructionEntityMapper.selectMaxTime();
        // 获取数据，分页，1000条生成
        int pageNum = 1;
        int pageSize = 1000;
        List<MdmProductConstruction> syncData = null;
        while (syncData == null || syncData.size() >= pageSize) {
            syncData = getSyncData(maxTime, pageNum++);
            mdmProductConstructionService.syncProductConstructionInfo(syncData, dataVersion);
        }
        return AjaxResult.success();
    }

    @Value("${itf.mid.url}")
    private String midUrl;
    @Value("${itf.mid.username}")
    private String midUsername;
    @Value("${itf.mid.password}")
    private String midPassword;

    @Value("${itf.mesMid.url}")
    private String mesMidUrl;
    @Value("${itf.mesMid.username}")
    private String mesUsername;
    @Value("${itf.mesMid.password}")
    private String mesPassword;

    /**
     * 获取同步版本号
     *
     * @return 结果
     */
    public String getSyncDataVersion() {
        String dataVersion = "";
        //该写法会自动关闭资源
        try (Connection con = DriverManager.getConnection(midUrl, midUsername, midPassword);
             Statement stmt = con.createStatement()) {

            String sql = "select case when f.DATA_VERSION is null then '-' else f.DATA_VERSION end DATA_VERSION , " +
                    " f.factory_code, " +
                    " f.company_code" +
                    " from (select 1 as dummy) d" +
                    " left join (" +
                    " SELECT DATA_VERSION , factory_code, company_code" +
                    " FROM(" +
                    "  SELECT I.DATA_VERSION, factory_code, company_code" +
                    "  FROM AUX_REQ_SYNC_DATA_LOGS I" +
                    "  WHERE I.STATUS=1" +
                    "  AND I.SYNC_KEY='MES_TIRE_BOM'" +
                    "  ORDER BY I.CREATE_DATE ASC, i.msg_id ASC" +
                    " ) t" +
                    " limit 1" +
                    " ) f on 1 = 1";
            ResultSet resultSet = stmt.executeQuery(sql);

            while (resultSet.next()) {
                // 处理结果集
                dataVersion = resultSet.getString("DATA_VERSION");
                log.info("SAP与施工关系同步版本号：{}", dataVersion);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }

        return dataVersion;
    }

    /**
     * 获取同步数据
     * @param maxTime 最大时间
     * @param pageNum 页码
     * @return 结果
     */
    private List<MdmProductConstruction> getSyncData(String maxTime, Integer pageNum) {
        List<MdmProductConstruction> result = new ArrayList<>();
        //该写法会自动关闭资源
        try (Connection con = DriverManager.getConnection(mesMidUrl, mesUsername, mesPassword);
             Statement stmt = con.createStatement()) {
            String sql = "SELECT *   " +
                    "FROM (  " +
                    "    SELECT   " +
                    "        t.*,  " +
                    "        ROW_NUMBER() OVER (ORDER BY t.create_time DESC) AS page_rn    " +
                    "    FROM (  " +
                    "        SELECT  " +
                    "            '116' factory_code,  " +
                    "            SUBSTRING(tb.sap_code, 11, LEN(tb.sap_code)) product_code,  " +
                    "            SUBSTRING(tb.tyrematerialid, 2, LEN(tb.tyrematerialid)) spec_code,  " +
                    "            SUBSTRING(m.PLM_CODE, 2, LEN(m.PLM_CODE)) construction_code,  " +
                    "            tb.GreenTyreMaterialID embryo_code,  " +
                    "            SUBSTRING(m.PLM_CODE, 8, LEN(m.PLM_CODE)) production_version,  " +
                    "            CASE WHEN ClassificationAttrs LIKE '%半鼓式%' THEN 2 ELSE 1 END MOULD_METHOD,  " +
                    "            tb.SubMatEditionNo BOM_VERSION,  " +
                    "            tb.crite create_time,  " +
                    "            'MES' create_by,  " +
                    "            ROW_NUMBER() OVER(  " +
                    "                PARTITION BY   " +
                    "                    SUBSTRING(tb.sap_code, 11, LEN(tb.sap_code)),  " +
                    "                    CASE WHEN ClassificationAttrs LIKE '%半鼓式%' THEN 2 ELSE 1 END  " +
                    "                ORDER BY tb.crite DESC  " +
                    "            ) AS group_rn    " +
                    "        FROM PLM_TIRE_BOM tb  " +
                    "        INNER JOIN PLM_MATERIAL m   " +
                    "            ON m.MatSpec = tb.GreenTyreMaterialID   " +
                    "            AND m.EditionNo = tb.SubMatEditionNo  " +
                    "        WHERE tb.crite >= '" + maxTime + "'" +
                    "    ) t  " +
                    "    WHERE t.group_rn = 1  " +
                    ") tt  " +
                    "WHERE tt.page_rn BETWEEN ((" + pageNum + "-1) * 1000 + 1) AND ( " + pageNum + "  * 1000);  ";
            ResultSet resultSet = stmt.executeQuery(sql);
            while (resultSet.next()) {
                // 处理结果集
                MdmProductConstruction mdmProductConstruction = new MdmProductConstruction();
                mdmProductConstruction.setFactoryCode(resultSet.getString("factory_code"));
                mdmProductConstruction.setProductCode(resultSet.getString("product_code"));
                mdmProductConstruction.setSpecCode(resultSet.getString("spec_code"));
                mdmProductConstruction.setConstructionCode(resultSet.getString("construction_code"));
                mdmProductConstruction.setEmbryoCode(resultSet.getString("embryo_code"));
                mdmProductConstruction.setProductionVersion(resultSet.getString("production_version"));
                mdmProductConstruction.setMouldMethod(resultSet.getString("MOULD_METHOD"));
                mdmProductConstruction.setBomVersion(resultSet.getString("BOM_VERSION"));
                mdmProductConstruction.setCreateTime(resultSet.getDate("create_time"));
                mdmProductConstruction.setCreateBy(resultSet.getString("create_by"));
                mdmProductConstruction.setUpdateBy(resultSet.getString("create_by"));
                mdmProductConstruction.setUpdateTime(new Date());
                result.add(mdmProductConstruction);
            }
        } catch (SQLException e) {
            e.printStackTrace();
            log.error(e.getMessage());
            throw new RuntimeException(e.getMessage());
        }
        return result;
    }
}
