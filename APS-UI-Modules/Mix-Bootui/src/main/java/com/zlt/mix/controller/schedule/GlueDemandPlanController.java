package com.zlt.mix.controller.schedule;

import com.alibaba.fastjson.JSON;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common4ui.utils.file.FileUtils4UI;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ExcelUtil;
import com.zlt.mix.common.utils.ExportUtil;
import com.zlt.mix.common.utils.ImportUtil;
import com.zlt.mix.schedule.api.domain.dto.GlueDemandPlanExportDictDto;
import com.zlt.mix.schedule.api.domain.entity.GlueDemandPlan;
import com.zlt.mix.schedule.api.domain.entity.GlueDemandPlanInit;
import com.zlt.mix.schedule.api.service.IGlueDemandPlanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.poi.hssf.usermodel.HSSFDateUtil;
import org.apache.poi.ss.usermodel.*;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 分厂胶料需求计划Controller
 *
 * @author chen
 * @date 2022-04-18
 */
@Api(tags = "分厂胶料需求计划")
@Controller
@RequestMapping("/schedule/glueDemandPlan")
public class GlueDemandPlanController extends BaseController {

    @Resource
    private IGlueDemandPlanService iGlueDemandPlanService;
    @Resource
    private IExportLogService iExportLogService;
    @Resource
    private IImportErrorLogService iImportErrorLogService;
    @Resource
    private IImportLogService iImportLogService;
    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;

    @Value("${excelTemplateModel}")
    private String excelTemplateModel;

    private final String prefix = "schedule/glueDemandPlan";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("schedule:glueDemandPlan:view")
    @GetMapping()
    public String toIndex(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));
        return prefix + "/glueDemandPlan";
    }

    @ApiOperation("根据条件查询分厂胶料需求计划列表")
    @RequiresPermissions("schedule:glueDemandPlan:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo listGlueDemandPlan(GlueDemandPlan entity) {
        return iGlueDemandPlanService.listGlueDemandPlan(entity);
    }

    /**
     * 跳转至新增页面
     */
    @ApiOperation("跳转至新增页面")
    @GetMapping("/add")
    public String toAdd(ModelMap mmap) {
        GlueDemandPlan glueDemandPlan = new GlueDemandPlan();
        glueDemandPlan.setPlanDate(DateUtils.addDays(new Date(), 1));
        mmap.put("glueDemandPlan", glueDemandPlan);
        return prefix + "/add";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/edit/{id}")
    public String toEdit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("glueDemandPlan", iGlueDemandPlanService.getGlueDemandPlanInfo(id));
        return prefix + "/edit";
    }

    @ApiOperation("跳转至修改页面")
    @GetMapping("/split/{id}")
    public String toSplit(@ApiParam("id") @PathVariable("id") Long id, ModelMap mmap){
        mmap.put("glueDemandPlan", iGlueDemandPlanService.getGlueDemandPlanInfo(id));
        return prefix + "/split";
    }

    @ApiOperation("跳转至导入页面")
    @GetMapping("/importData")
    public String importDate(ModelMap mmp) {
        mmp.put("prefix", prefix);
        mmp.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));
        return prefix + "/importData";
    }

    @ApiOperation("修改或新增分厂胶料需求计划")
    @PostMapping("/save")
    @ResponseBody
    public AjaxResult saveGlueDemandPlan(GlueDemandPlan glueDemandPlan) {
        return iGlueDemandPlanService.saveGlueDemandPlan(glueDemandPlan);
    }

    @ApiOperation("删除分厂胶料需求计划（id不为空）")
    @RequiresPermissions("schedule:glueDemandPlan:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult removeGlueDemandPlan(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iGlueDemandPlanService.deleteGlueDemandPlan(arr);
    }

    @ApiOperation("校验分厂胶料需求计划唯一性")
    @PostMapping("/checkGlueDemandPlanUnique")
    @ResponseBody
    public String checkGlueDemandPlanUnique(GlueDemandPlan glueDemandPlan) {
        return iGlueDemandPlanService.checkGlueDemandPlanUnique(glueDemandPlan);
    }

    /**
     * 导出分厂胶料需求计划
     */
    @ApiOperation("导出分厂胶料需求计划")
    @RequiresPermissions("schedule:glueDemandPlan:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, GlueDemandPlan glueDemandPlan) throws IOException {
        String fileName = I18nUtil.getMessage("schedule.glueDemandPlan.modelName");
        GlueDemandPlanExportDictDto dictDto = new GlueDemandPlanExportDictDto();
        HashMap<String, String> factoryDictMap = iSysDictDataCacheService.getType("FACTORY").stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (s, s2) -> s, HashMap::new));
        HashMap<String, String> mixAreaDictMap = iSysDictDataCacheService.getType("MIX_AREA").stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (s, s2) -> s, HashMap::new));
        dictDto.setFactoryDictMap(factoryDictMap);
        dictDto.setMixAreaDictMap(mixAreaDictMap);
        BeanUtils.copyProperties(glueDemandPlan,dictDto);
        byte[] data = iGlueDemandPlanService.exportData(dictDto);
        ExportLog exportLog = ExportUtil.uploadAndExportExcelByByte(response, data, fileName, dictDto.toString(), ZltConstant.PROCEDURE_CODE_MIX);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载导入模板
     *
     * @param response 下载的模板文件
     * @throws IOException 异常
     */
    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate")
    @ResponseBody
    public AjaxResult importTemplate(HttpServletResponse response) throws IOException {
        String fileName = I18nUtil.getMessage("schedule.glueDemandPlan.modelName");
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelTemplateModel + "schedule/" + fileName + ".xlsx");
        if (in == null) {
            return AjaxResult.error(I18nUtil.getMessage("ui.common.message.fileNotFound"));
        }
        ExcelUtil.setResponseHeader(response, fileName);
        FileUtils4UI.writeInputStream(in, response.getOutputStream());
        return AjaxResult.success();
    }

    /**
     * excel数据导入
     *
     * @param file     要导入的文件
     * @param planDate 计划日期
     * @param factory  分厂
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("schedule:glueDemandPlan:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, Date planDate, String factory, boolean isSkip) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ZltConstant.PROCEDURE_CODE_MIX,
                I18nUtil.getMessage("schedule.glueDemandPlan.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        //文件解析
        InputStream in = new ByteArrayInputStream(data);
        List<GlueDemandPlanInit> list = parseObject(in, planDate, factory, isSkip);
        //导入数据
        AjaxResult ajaxResult = iGlueDemandPlanService.importData(list, importLog.getId(), isSkip);
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 拆分需求计划
     *
     * @param listStr 拆分后的数据json字符串
     * @param id   要拆分的数据id
     */
    @RequiresPermissions("schedule:glueDemandPlan:split")
    @ApiOperation("拆分需求计划")
    @PostMapping("/splitPlan")
    @ResponseBody
    public AjaxResult splitPlan(@ApiParam("拆分后的数据json字符串") String listStr, @ApiParam("要拆分的数据id") Long id){
        List<GlueDemandPlan> list = JSON.parseArray(listStr, GlueDemandPlan.class);
        return iGlueDemandPlanService.splitPlan(list, id);
    }

    /**
     * 重新匹配密炼区
     *
     * @param glueDemandPlan 需要重新匹配的计划日期
     */
    @RequiresPermissions("schedule:glueDemandPlan:rematch")
    @ApiOperation("重新匹配密炼区")
    @PostMapping("/rematch")
    @ResponseBody
    public AjaxResult rematch(GlueDemandPlan glueDemandPlan) {
        return iGlueDemandPlanService.rematch(glueDemandPlan);
    }

    @ApiOperation("检测对应日期和分厂的数据是否存在")
    @PostMapping("/checkPlanDateAndFactoryExist")
    @ResponseBody
    public AjaxResult checkPlanDateAndFactoryExist(GlueDemandPlan glueDemandPlan) {
        String unique = iGlueDemandPlanService.checkPlanDateAndFactoryExist(glueDemandPlan);
        //避免ZltConstant是否唯一的常量值修改，在此处定义0为唯一，1为不唯一
        if (ZltConstant.UNIQUE.equals(unique)) {
            return AjaxResult.success("0");
        }
        if (ZltConstant.NOT_UNIQUE.equals(unique)) {
            return AjaxResult.success("1");
        }
        return AjaxResult.error();
    }

    /**
     * 根据流解析excel数据成对应集合
     *
     * @param in 流数据
     * @return 解析后集合
     * @throws IOException 异常
     */
    private List<GlueDemandPlanInit> parseObject(InputStream in, Date planDate, String factory, boolean isSkip) throws IOException {
        List<SysDictData> mixArea = iSysDictDataCacheService.getType("MIX_AREA");
        Map<String, String> mixAreaDictMap = mixArea.stream().collect(Collectors.toMap(SysDictData::getDictLabel, SysDictData::getDictValue));

        List<GlueDemandPlanInit> list = new ArrayList<>();
        if (in == null) {
            return list;
        }
        Workbook workbook = WorkbookFactory.create(in);
        Sheet sheet = workbook.getSheetAt(0);
        if (sheet == null) {
            throw new IOException(I18nUtil.getMessage("common.error.util.file.sheet.noexist"));
        }
        int rows = sheet.getPhysicalNumberOfRows();
        if (rows > 0) {
            for (int i = 1; i < rows; i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                String importDate=getCellValue(row.getCell(1));    //1 导入日期
                String importFactory=getCellValue(row.getCell(2));    //2 导入分厂
                String glueName = getCellValue(row.getCell(3));    //3 胶料名称
                String glue = getCellValue(row.getCell(4));    //4 胶料号
                String sapCode = getCellValue(row.getCell(5));    //5 品号
                String stockQty = getCellValue(row.getCell(6));    //6 库存数量
                String dayPlanSurplus=getCellValue(row.getCell(7));    //7 白班剩余计划
                String actualFinish=getCellValue(row.getCell(8));    //8 实际完成
                String totalPlanQty = getCellValue(row.getCell(9));    //9 日计划量(车)
                String mixAreaValue = getCellValue(row.getCell(10));    //10 密炼区
                String vehicleDesk=getCellValue(row.getCell(11));    //11 车/桌
                String vehicleWeight=getCellValue(row.getCell(12));    //12 单车重量
                String midPlanQty = getCellValue(row.getCell(13));   //13 夜班计划量
                String midCollar = getCellValue(row.getCell(14));   //14 夜班支领
                String midRemark = getCellValue(row.getCell(15));   //15 夜班备注
                String nightPlanQty = getCellValue(row.getCell(16));   //16 白班计划量
                String nightCollar = getCellValue(row.getCell(17));   //17 白班支领
                String nightRemark = getCellValue(row.getCell(18));   //18 白班备注
                String nextMidPlanQty = getCellValue(row.getCell(19));   //19 次日夜班
                String notCollar = getCellValue(row.getCell(20));   //20 未支领

                String dayPlanQty = "";   //19 白班计划量
                String dayCollar = "";   //20 白班支领
                String dayRemark = "";   //21 白班备注

                // 如果勾选计划量为0时跳过，则空或者为0.0时跳过
//                if (isSkip && (StringUtils.isBlank(totalPlanQty) || new BigDecimal(BigInteger.ZERO, 1).toString().equals(totalPlanQty))) {
//                    continue;
//                }

                // 当前行所有数据为空，跳过当前记录
                if (StringUtils.isAllBlank(importDate, importFactory, glueName, glue, sapCode, stockQty, dayPlanSurplus,
                        actualFinish, totalPlanQty, mixAreaValue, vehicleDesk, vehicleWeight, midPlanQty, midCollar, midRemark,
                        nightPlanQty, nightCollar, nightRemark, dayPlanQty, dayCollar, dayRemark, nextMidPlanQty, notCollar)) {
                    continue;
                }

                GlueDemandPlanInit entity = new GlueDemandPlanInit();
                entity.setImportDate(importDate);//导入日期
                entity.setImportFactory(importFactory);//导入分厂
                entity.setPlanDate(planDate);
                entity.setFactory(factory);
                entity.setGlueName(getStringValue(glueName));//胶料名称
                entity.setGlue(glue);//胶料号
                entity.setSapCode(StringUtils.isNotEmpty(sapCode)?sapCode.trim():sapCode);//品号
                entity.setStockQty(stockQty);//库存数量
                entity.setDayPlanSurplus(dayPlanSurplus);//白班计划剩余量
                entity.setActualFinish(actualFinish);//实际完成
                entity.setVehicleDesk(vehicleDesk);//车/桌
                entity.setVehicleWeight(vehicleWeight);//单车重量
                entity.setMixArea(getDictValues(mixAreaDictMap, mixAreaValue));
                entity.setTotalPlanQty(StringUtils.isNotEmpty(totalPlanQty) ? BigDecimal.valueOf(Double.parseDouble(totalPlanQty)) : null);
                entity.setMidPlanQty(StringUtils.isNotEmpty(midPlanQty) ? BigDecimal.valueOf(Double.parseDouble(midPlanQty)) : null);
                entity.setMidCollar(midCollar);//中班支领
                entity.setMidRemark(getStringValue(midRemark));
                entity.setNightPlanQty(StringUtils.isNotEmpty(nightPlanQty) ? BigDecimal.valueOf(Double.parseDouble(nightPlanQty)) : null);
                entity.setNightCollar(nightCollar);//夜班支领
                entity.setNightRemark(getStringValue(nightRemark));
                entity.setDayPlanQty(StringUtils.isNotEmpty(dayPlanQty) ? BigDecimal.valueOf(Double.parseDouble(dayPlanQty)) : null);
                entity.setDayRemark(getStringValue(dayRemark));
                entity.setDayCollar(dayCollar);//白班支领
                entity.setNextMidPlanQty(nextMidPlanQty);//次日中班
                entity.setNotCollar(notCollar);//未支领
                list.add(entity);
            }
        }
        return list;
    }

    /**
     * 获取单元格值
     * @param cell 单元格
     * @return 解析后的值
     */
    private String getCellValue(Cell cell) {
        Object val = null;
        if (cell == null) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA) {
            val = cell.getNumericCellValue();
            //当val值为大值时，很可能解析为科学计数法显示
            String valStr = val + "";
            if (HSSFDateUtil.isCellDateFormatted(cell)) {
                Date date = cell.getDateCellValue();
                val = DateUtils.parseDateToStr("yyyy-MM-dd",date);
            }else if (valStr.contains("E")) {
                BigDecimal realValue = new BigDecimal(valStr);
                val = realValue.toPlainString();
            } else {
                if ((Double) val % 1 != 0) {
                    val = new BigDecimal(val.toString());
                } else {
                    val = new DecimalFormat("0").format(val);
                }
            }
        } else if (cell.getCellType() == CellType.STRING) {
            val = cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            val = cell.getBooleanCellValue();
        }
        if (val == null) {
            return null;
        }
        return val + "";
    }

    /**
     * 获取字符串值
     * @param val 源字符串
     * @return 字符串值
     */
    private String getStringValue(String val) {
        String stringVal;
        if (val == null) {
            return null;
        }
        try {
            stringVal = val + "";
        } catch (Exception e) {
            return null;
        }
        return stringVal;
    }

    /**
     * 获取解析后字典value值
     * @param dictMap 字典map
     * @param dictLabel 字典label
     * @return 解析后字典值，如果有一个未解析到则返回null
     */
    private String getDictValues(Map<String, String> dictMap, String dictLabel){
        if (StringUtils.isEmpty(dictLabel)) {
            return "";
        }
        String[] dictLabels = dictLabel.split(",");
        StringBuilder dictValue = new StringBuilder();
        for (String label : dictLabels) {
            String value = dictMap.get(label);
            if (value == null) {
                return "";
            }
            dictValue.append(value).append(",");
        }
        return dictValue.substring(0, dictValue.length() - 1);
    }
}
