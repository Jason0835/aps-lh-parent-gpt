package com.zlt.aps.controller.cx;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.SysDictData;
import com.ruoyi.api.gateway.system.service.*;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.zlt.aps.common.constant.ApsBootConstant;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.cx.api.domain.dto.ConstructionInfoDto;
import com.zlt.aps.cx.api.domain.entity.*;
import com.zlt.aps.cx.api.service.ICxConstructionInfoService;
import com.zlt.aps.cx.api.service.ICxMachineInfoService;
import com.zlt.aps.cx.api.service.ICxProductConstructionInfoService;
import com.zlt.aps.cx.api.service.IMdmMonthProdPlanService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import com.zlt.framework.utils.AuthorizationUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 主计划月度生产计划Controller
 *
 * @author zlt
 * @date 2021-09-15
 */
@Api(tags = "主计划月度生产计划")
@Controller
@RequestMapping("/cx/mdmMonthProdPlan")
public class MdmMonthProdPlanController extends BaseController {

    private final String prefix = "cx/mdmMonthProdPlan";
    @Autowired
    ISysDictTypeService iSysDictTypeService;
    @Autowired
    private IMdmMonthProdPlanService iMdmMonthProdPlanService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;
    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;
    @Autowired
    private ICxConstructionInfoService iCxConstructionInfoService;
    @Autowired
    private ICxMachineInfoService cxMachineInfoService;

    @Autowired
    private ICxProductConstructionInfoService iCxProductConstructionInfoService;

    @Value("${excelTemplateModel}")
    private String excelTemplateModel;

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("cx:mdmMonthProdPlan:view")
    @GetMapping()
    public String toIndex(ModelMap mop) {
        mop.put("initDate", DateUtils.parseDateToStr("yyyy-MM", new Date()));
        return prefix + "/mdmMonthProdPlan";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    public String add(ModelMap mmap) {
        MdmMonthProdPlan mdmMonthProdPlan = new MdmMonthProdPlan();
        mdmMonthProdPlan.setMainPlanMonth(new Date());
        mmap.put("mdmMonthProdPlan", mdmMonthProdPlan);
        return prefix + "/add";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("mdmMonthProdPlan", iMdmMonthProdPlanService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 根据条件查询主计划月度生产计划列表
     */
    @ApiOperation("根据条件查询主计划月度生产计划列表")
    @RequiresPermissions("cx:mdmMonthProdPlan:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(MdmMonthProdPlan entity) {
        Date queryDate = entity.getMainPlanMonth();
        if (queryDate == null) {
            queryDate = new Date();
        }
        String mainPlanMonth = DateFormatUtils.format(queryDate, "yyyyMM");
        String year = mainPlanMonth.substring(0, 4);
        String month = mainPlanMonth.substring(4);
        entity.setYear(year);
        entity.setMonth(month);
        if (StringUtils.isBlank(entity.getIsFinamized())) {
            entity.setIsFinamized("0");
        }
        return iMdmMonthProdPlanService.list(entity);
    }

    /**
     * 修改施工版本
     */
    @GetMapping("/changeBomDataVersion/{id}")
    public String changeBomDataVersion(@PathVariable("id") Long id, ModelMap mmap) {

        MdmMonthProdPlan mmp = iMdmMonthProdPlanService.getInfo(id);
        CxProductConstructionInfo pc = new CxProductConstructionInfo();
        pc.setDelFlag("0");
        pc.setEmbryoCode(mmp.getEmbryoCode());
        List<CxProductConstructionInfo> pcList = iCxProductConstructionInfoService.getList(pc);
        if (CollectionUtils.isEmpty(pcList)) {
            pcList = new ArrayList<CxProductConstructionInfo>();
        }
        mmap.put("entity", mmp);
        mmap.put("embryoVersions", pcList);
        return prefix + "/changeBomDataVersion";
    }

    /**
     * 修改施工版本
     */
    @ApiOperation("修改施工版本")
    @RequiresPermissions("cx:cxScheduleResult:edit")
    @PostMapping("/changeBomDataVersion")
    @ResponseBody
    public AjaxResult changeBomDataVersion(MdmMonthProdPlan mdmMonthProdPlan) {
        return iMdmMonthProdPlanService.edit(mdmMonthProdPlan);
    }


    /**
     * 修改或新增主计划月度生产计划
     */
    @ApiOperation("修改或新增主计划月度生产计划")
    @RequiresPermissions("cx:mdmMonthProdPlan:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(MdmMonthProdPlan mdmMonthProdPlan) {
        return iMdmMonthProdPlanService.edit(mdmMonthProdPlan);
    }

    /**
     * 修改或新增主计划月度生产计划
     */
    @ApiOperation("修改或新增主计划月度生产计划")
    @RequiresPermissions("cx:mdmMonthProdPlan:edit")
    @PostMapping("/updateExpectedExcessArrears")
    @ResponseBody
    public AjaxResult updateExpectedExcessArrears(MdmMonthProdPlan mdmMonthProdPlan) {
        return iMdmMonthProdPlanService.updateExpectedExcessArrears(mdmMonthProdPlan);
    }
    /**
     * 新增主计划月度生产计划
     */
    @ApiOperation("新增主计划月度生产计划")
    @RequiresPermissions("cx:mdmMonthProdPlan:add")
    @PostMapping("/add")
    @ResponseBody
    public AjaxResult add(MdmMonthProdPlan mdmMonthProdPlan) {
        mdmMonthProdPlan.setDataSource("1");
        AjaxResult ajaxResult = iMdmMonthProdPlanService.add(mdmMonthProdPlan);
        return ajaxResult;
    }

    /**
     * 删除主计划月度生产计划
     */
    @ApiOperation("删除主计划月度生产计划（id不为空）")
    @RequiresPermissions("cx:mdmMonthProdPlan:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iMdmMonthProdPlanService.remove(arr);
    }

    /**
     * 校验主计划月度生产计划唯一性
     */
    @ApiOperation("校验主计划月度生产计划唯一性")
    @PostMapping("/checkMdmMonthProdPlanUnique")
    @ResponseBody
    public String checkMdmMonthProdPlanUnique(MdmMonthProdPlan mdmMonthProdPlan) {
        return iMdmMonthProdPlanService.checkMdmMonthProdPlanUnique(mdmMonthProdPlan);
    }

    /**
     * 导出主计划月度生产计划
     */
    @ApiOperation("导出主计划月度生产计划")
    @RequiresPermissions("cx:mdmMonthProdPlan:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response, MdmMonthProdPlan entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.mdmMonthProdPlan.modelName");
        Date queryDate = entity.getMainPlanMonth();
        if (queryDate == null) {
            queryDate = new Date();
        }
        String mainPlanMonth = DateFormatUtils.format(queryDate, "yyyyMM");
        String year = mainPlanMonth.substring(0, 4);
        String month = mainPlanMonth.substring(4);
        entity.setYear(year);
        entity.setMonth(month);
        if (StringUtils.isBlank(entity.getIsFinamized())) {
            entity.setIsFinamized("0");
        }
        List<MdmMonthProdPlan> list = iMdmMonthProdPlanService.getList(entity);
        String lang = AuthorizationUtils.getLang();
        String tempName = (ApsBootConstant.EN_US.equals(lang) ? ApsBootConstant.CX_EN_PLAN : ApsBootConstant.CX_ZH_PLAN);
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelTemplateModel + "cx/" + tempName + ".xlsx");
        if (in == null) {
            return;
        }
        ExcelUtil.setResponseHeader(response, fileName);
        Workbook workbook = ExcelUtils.readExcel(in);
        fillWorkBook(list, workbook, queryDate, month);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, entity.toString(), ApsConstant.PROCEDURE_CODE_CX);
        iExportLogService.add(exportLog);
    }

    /**
     * 填充workbook
     *
     * @param workbook
     * @return
     */
    public void fillWorkBook(List<MdmMonthProdPlan> list, Workbook workbook, Date queryDate, String month) {
        Sheet sheet = workbook.getSheetAt(0);
        CellStyle cellStyle = ExcelUtils.createCellStyle(workbook);
        Calendar calendar = Calendar.getInstance();
        List<SysDictData> STORAGE_LOCATION = iSysDictDataCacheService.getType("STORAGE_LOCATION");
        Map<String, String> STORAGE_LOCATION_Map = STORAGE_LOCATION.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        Cell cell0 = sheet.getRow(0).getCell(1);
        Cell cell1 = sheet.getRow(1).getCell(6);
        Cell cell2 = sheet.getRow(1).getCell(7);
        String title0 = cell0.getStringCellValue();
        String title1 = cell1.getStringCellValue();
        String title2 = cell2.getStringCellValue();
        title0 = title0.replace("${date0}", DateFormatUtils.format(queryDate, "yyyy年MM月"));
        title1 = title1.replace("${date1}", DateFormatUtils.format(new Date(), "yyyy年MM月dd日"));
        title2 = title2.replace("${month}", month);
        cell0.setCellValue(title0);
        cell1.setCellValue(title1);
        cell2.setCellValue(title2);

        for (int i = 0; i < list.size(); i++) {
            Row row = sheet.createRow(i + 3);
            MdmMonthProdPlan mdmMonthProdPlan = list.get(i);

            String bdStr = "";
            String edStr = "";
            Date bd = mdmMonthProdPlan.getBeginDate();
            Date ed = mdmMonthProdPlan.getEndDate();
            if (bd != null) {
                calendar.setTime(bd);
                bdStr = calendar.get(Calendar.DATE) + "";
            }
            if (ed != null) {
                calendar.setTime(ed);
                edStr = calendar.get(Calendar.DATE) + "";
            }
            row.createCell(0).setCellValue(mdmMonthProdPlan.getMaterialCode() == null ? "" : mdmMonthProdPlan.getMaterialCode());
            row.createCell(1).setCellValue(mdmMonthProdPlan.getSpecDesc() == null ? "" : mdmMonthProdPlan.getSpecDesc());
            row.createCell(2).setCellValue(mdmMonthProdPlan.getEmbryoCode() == null ? "" : mdmMonthProdPlan.getEmbryoCode());
            row.createCell(3).setCellValue(mdmMonthProdPlan.getSpecDimension() == null ? "" : mdmMonthProdPlan.getSpecDimension().doubleValue() + "");
            row.createCell(4).setCellValue(mdmMonthProdPlan.getQualityGrade() == null ? "" : mdmMonthProdPlan.getQualityGrade());
            row.createCell(5).setCellValue(mdmMonthProdPlan.getStorageLocation() == null ? "" : STORAGE_LOCATION_Map.get(mdmMonthProdPlan.getStorageLocation()));
            row.createCell(6).setCellValue(mdmMonthProdPlan.getSpecialRequirements() == null ? "" : mdmMonthProdPlan.getSpecialRequirements());
            row.createCell(7).setCellValue(mdmMonthProdPlan.getTheoryProductionPlan() == null ? "" : mdmMonthProdPlan.getTheoryProductionPlan() + "");
            row.createCell(8).setCellValue(mdmMonthProdPlan.getExpectedExcessArrears() == null ? "" : mdmMonthProdPlan.getExpectedExcessArrears() + "");
            row.createCell(10).setCellValue(mdmMonthProdPlan.getPlanModifyQty() == null ? "" : mdmMonthProdPlan.getPlanModifyQty() + "");
            row.createCell(11).setCellValue(mdmMonthProdPlan.getActualArrangement() == null ? "" : mdmMonthProdPlan.getActualArrangement() + "");
            row.createCell(15).setCellValue(mdmMonthProdPlan.getRemark() == null ? "" : mdmMonthProdPlan.getRemark());
            row.createCell(16).setCellValue(bdStr);
            row.createCell(17).setCellValue(edStr);
            row.createCell(18).setCellValue(mdmMonthProdPlan.getProductQty1() == null ? "" : mdmMonthProdPlan.getProductQty1() + "");
            row.createCell(19).setCellValue(mdmMonthProdPlan.getProductQty2() == null ? "" : mdmMonthProdPlan.getProductQty2() + "");
            row.createCell(20).setCellValue(mdmMonthProdPlan.getProductQty3() == null ? "" : mdmMonthProdPlan.getProductQty3() + "");
            row.createCell(21).setCellValue(mdmMonthProdPlan.getProductQty4() == null ? "" : mdmMonthProdPlan.getProductQty4() + "");
            row.createCell(22).setCellValue(mdmMonthProdPlan.getProductQty5() == null ? "" : mdmMonthProdPlan.getProductQty5() + "");
            row.createCell(23).setCellValue(mdmMonthProdPlan.getProductQty6() == null ? "" : mdmMonthProdPlan.getProductQty6() + "");
            row.createCell(24).setCellValue(mdmMonthProdPlan.getProductQty7() == null ? "" : mdmMonthProdPlan.getProductQty7() + "");
            row.createCell(25).setCellValue(mdmMonthProdPlan.getProductQty8() == null ? "" : mdmMonthProdPlan.getProductQty8() + "");
            row.createCell(26).setCellValue(mdmMonthProdPlan.getProductQty9() == null ? "" : mdmMonthProdPlan.getProductQty9() + "");
            row.createCell(27).setCellValue(mdmMonthProdPlan.getProductQty10() == null ? "" : mdmMonthProdPlan.getProductQty10() + "");
            row.createCell(28).setCellValue(mdmMonthProdPlan.getProductQty11() == null ? "" : mdmMonthProdPlan.getProductQty11() + "");
            row.createCell(29).setCellValue(mdmMonthProdPlan.getProductQty12() == null ? "" : mdmMonthProdPlan.getProductQty12() + "");
            row.createCell(30).setCellValue(mdmMonthProdPlan.getProductQty13() == null ? "" : mdmMonthProdPlan.getProductQty13() + "");
            row.createCell(31).setCellValue(mdmMonthProdPlan.getProductQty14() == null ? "" : mdmMonthProdPlan.getProductQty14() + "");
            row.createCell(32).setCellValue(mdmMonthProdPlan.getProductQty15() == null ? "" : mdmMonthProdPlan.getProductQty15() + "");
            row.createCell(33).setCellValue(mdmMonthProdPlan.getProductQty16() == null ? "" : mdmMonthProdPlan.getProductQty16() + "");
            row.createCell(34).setCellValue(mdmMonthProdPlan.getProductQty17() == null ? "" : mdmMonthProdPlan.getProductQty17() + "");
            row.createCell(35).setCellValue(mdmMonthProdPlan.getProductQty18() == null ? "" : mdmMonthProdPlan.getProductQty18() + "");
            row.createCell(36).setCellValue(mdmMonthProdPlan.getProductQty19() == null ? "" : mdmMonthProdPlan.getProductQty19() + "");
            row.createCell(37).setCellValue(mdmMonthProdPlan.getProductQty20() == null ? "" : mdmMonthProdPlan.getProductQty20() + "");
            row.createCell(38).setCellValue(mdmMonthProdPlan.getProductQty21() == null ? "" : mdmMonthProdPlan.getProductQty21() + "");
            row.createCell(39).setCellValue(mdmMonthProdPlan.getProductQty22() == null ? "" : mdmMonthProdPlan.getProductQty22() + "");
            row.createCell(40).setCellValue(mdmMonthProdPlan.getProductQty23() == null ? "" : mdmMonthProdPlan.getProductQty23() + "");
            row.createCell(41).setCellValue(mdmMonthProdPlan.getProductQty24() == null ? "" : mdmMonthProdPlan.getProductQty24() + "");
            row.createCell(42).setCellValue(mdmMonthProdPlan.getProductQty25() == null ? "" : mdmMonthProdPlan.getProductQty25() + "");
            row.createCell(43).setCellValue(mdmMonthProdPlan.getProductQty26() == null ? "" : mdmMonthProdPlan.getProductQty26() + "");
            row.createCell(44).setCellValue(mdmMonthProdPlan.getProductQty27() == null ? "" : mdmMonthProdPlan.getProductQty27() + "");
            row.createCell(45).setCellValue(mdmMonthProdPlan.getProductQty28() == null ? "" : mdmMonthProdPlan.getProductQty28() + "");
            row.createCell(46).setCellValue(mdmMonthProdPlan.getProductQty29() == null ? "" : mdmMonthProdPlan.getProductQty29() + "");
            row.createCell(47).setCellValue(mdmMonthProdPlan.getProductQty30() == null ? "" : mdmMonthProdPlan.getProductQty30() + "");
            for (int j = 0; j < 48; j++) {
                Cell cell = row.getCell(j);
                if (cell == null) {
                    cell = row.createCell(j);
                }
                cell.setCellStyle(cellStyle);
            }
        }
    }

    /**
     * 预计超欠产导出
     */
    @ApiOperation("预计超欠产导出")
    @RequiresPermissions("cx:mdmMonthProdPlan:expectedExport")
    @GetMapping("/expectedExport")
    @ResponseBody
    public void expectedExport(HttpServletResponse response, MdmMonthProdPlan entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.mdmMonthProdPlan.modelName");
        fileName = fileName + "-" + I18nUtil.getMessage("ui.data.column.mdmMonthProdPlan.expectedExcessArrears");
        Date queryDate = entity.getMainPlanMonth();
        if (queryDate == null) {
            queryDate = new Date();
        }
        String mainPlanMonth = DateFormatUtils.format(queryDate, "yyyyMM");
        String year = mainPlanMonth.substring(0, 4);
        String month = mainPlanMonth.substring(4);
        entity.setYear(year);
        entity.setMonth(month);
        if (StringUtils.isBlank(entity.getIsFinamized())) {
            entity.setIsFinamized("0");
        }
        List<CxMdmMonthProdPlan1> list = iMdmMonthProdPlanService.expectedExport(entity);
        ExcelUtil<CxMdmMonthProdPlan1> util = new ExcelUtil<>(CxMdmMonthProdPlan1.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, entity.toString(), ApsConstant.PROCEDURE_CODE_CX);
        iExportLogService.add(exportLog);
    }

    /**
     * 超欠产导出
     */
    @ApiOperation("超欠产导出")
    @RequiresPermissions("cx:mdmMonthProdPlan:overProdExport")
    @GetMapping("/overProdExport")
    @ResponseBody
    public void overProdExport(HttpServletResponse response, MdmMonthProdPlan entity) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.mdmMonthProdPlan.modelName");
        fileName = fileName + "-" + I18nUtil.getMessage("ui.data.column.mdmMonthProdPlan.overProduction");
        Date queryDate = entity.getMainPlanMonth();
        if (queryDate == null) {
            queryDate = new Date();
        }
        String mainPlanMonth = DateFormatUtils.format(queryDate, "yyyyMM");
        String year = mainPlanMonth.substring(0, 4);
        String month = mainPlanMonth.substring(4);
        entity.setYear(year);
        entity.setMonth(month);
        if (StringUtils.isBlank(entity.getIsFinamized())) {
            entity.setIsFinamized("0");
        }
        List<CxMdmMonthProdPlan2> list = iMdmMonthProdPlanService.overProdExport(entity);
        ExcelUtil<CxMdmMonthProdPlan2> util = new ExcelUtil<>(CxMdmMonthProdPlan2.class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, entity.toString(), ApsConstant.PROCEDURE_CODE_CX);
        iExportLogService.add(exportLog);
    }

    /**
     * 下载导入模板
     *
     * @param response 下载的模板文件
     * @throws IOException 异常
     */
    @ApiOperation("下载导入模板")
    @GetMapping("/importTemplate/{mainPlanMonth}")
    @ResponseBody
    public void importTemplate(HttpServletResponse response, @PathVariable("mainPlanMonth") Date mainPlanMonth) throws IOException {

        String lang = AuthorizationUtils.getLang();
        String tempName = (ApsBootConstant.EN_US.equals(lang) ? ApsBootConstant.CX_EN_PLAN : ApsBootConstant.CX_ZH_PLAN);
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelTemplateModel + "cx/" + tempName + ".xlsx");
        if (in == null) {
            return ;
        }
        String fileName = I18nUtil.getMessage("ui.data.column.mdmMonthProdPlan.modelName");
        ExcelUtil.setResponseHeader(response, fileName);

        Workbook webBook = null;
        try {
            if (mainPlanMonth == null) {
                mainPlanMonth = new Date();
            }
            String month = DateFormatUtils.format(mainPlanMonth, "yyyyMM").substring(4);

            webBook = ExcelUtils.readExcel(in);
            Sheet sheet = webBook.getSheetAt(0);
            Cell cell0 = sheet.getRow(0).getCell(1);
            Cell cell1 = sheet.getRow(1).getCell(6);
            Cell cell2 = sheet.getRow(1).getCell(7);
            String title0 = cell0.getStringCellValue();
            String title1 = cell1.getStringCellValue();
            String title2 = cell2.getStringCellValue();
            title0 = title0.replace("${date0}", DateFormatUtils.format(mainPlanMonth, "yyyy年MM月"));
            title1 = title1.replace("${date1}", DateFormatUtils.format(new Date(), "yyyy年MM月dd日"));
            title2 = title2.replace("${month}", month);
            cell0.setCellValue(title0);
            cell1.setCellValue(title1);
            cell2.setCellValue(title2);
            webBook.write(response.getOutputStream());
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(webBook, in);
        }
    }

    /**
     * 跳转至导入页面
     *
     * @param mop
     * @return
     */
    @GetMapping("/importData")
    public String importDate(ModelMap mop) {
        return prefix + "/import";
    }

    /**
     * excel数据导入
     *
     * @param file          要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("cx:mdmMonthProdPlan:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, Date mainPlanMonth, String updateSupport, String isFinamized) throws Exception {

        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        InputStream in = new ByteArrayInputStream(data);
        // 上传文件到服务器，并获取导入记录对象进行保存
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_CX,
                I18nUtil.getMessage("ui.data.column.mdmMonthProdPlan.modelName"), file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);

        Boolean update = "0".equals(updateSupport) ? true : false;
        Boolean finamized = "0".equals(isFinamized) ? true : false;
        String month = DateUtils.parseDateToStr("yyyy-MM", mainPlanMonth);

        List<SysDictData> STORAGE_LOCATION = iSysDictDataCacheService.getType("STORAGE_LOCATION");
        Map<String, String> STORAGE_LOCATION_Map = STORAGE_LOCATION.stream().collect(Collectors.toMap(SysDictData::getDictLabel, SysDictData::getDictValue));
        AjaxResult ajaxResult = iMdmMonthProdPlanService.importData(data, month, update, importLog.getId(), finamized, STORAGE_LOCATION_Map);
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }


    /**
     * 下发主计划
     */
    @ApiOperation("下发主计划")
    @RequiresPermissions("cx:mdmMonthProdPlan:issuePlan")
    @PostMapping("/issuePlan")
    @ResponseBody
    public AjaxResult issuePlan(MdmMonthProdPlan mdmMonthProdPlan) {
        Date queryDate = mdmMonthProdPlan.getMainPlanMonth();
        if (queryDate == null) {
            queryDate = new Date();
        }
        String mainPlanMonth = DateFormatUtils.format(queryDate, "yyyyMM");
        String year = mainPlanMonth.substring(0, 4);
        String month = mainPlanMonth.substring(4);
        mdmMonthProdPlan.setYear(year);
        mdmMonthProdPlan.setMonth(month);
        mdmMonthProdPlan.setIsFinamized("0");
        List<SysDictData> dicts = iSysDictDataCacheService.getType("quality_level");
        Map<String, String> sysDictDataeMap = dicts.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        List<SysDictData> STORAGE_LOCATION = iSysDictDataCacheService.getType("STORAGE_LOCATION");
        Map<String, String> STORAGE_LOCATION_Map = STORAGE_LOCATION.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
        sysDictDataeMap.putAll(STORAGE_LOCATION_Map);
        return iMdmMonthProdPlanService.issuePlan(mdmMonthProdPlan, sysDictDataeMap);
    }

    /**
     * 根据胎胚代码获取规格尺寸
     */
    @ApiOperation("根据胎胚代码获取规格尺寸")
    @PostMapping("/getSpecDesc")
    @ResponseBody
    public AjaxResult getSpecDesc(MdmMonthProdPlan mdmMonthProdPlan) {
        if (StringUtils.isBlank(mdmMonthProdPlan.getEmbryoCode())) {
            return AjaxResult.error();
        }
        ConstructionInfoDto constructionInfoDto = new ConstructionInfoDto();
        constructionInfoDto.setEmbryoCode(mdmMonthProdPlan.getEmbryoCode());
        List<ConstructionInfoDto> list = iCxConstructionInfoService.exportData(constructionInfoDto);
        if (CollectionUtils.isNotEmpty(list)) {
            return AjaxResult.success(list.get(0));
        } else {
            return AjaxResult.error(I18nUtil.getMessage("ui.data.column.mdmMonthProdPlan.embryoCodeNotExist"));
        }
    }


    /**
     * 跳转至甘特图
     */
    @GetMapping("/gantt/{flag}")
    public String gantt(ModelMap mmap,@PathVariable("flag") int flag) {
        mmap.put("scheduleDate", DateUtils.parseDateToStr("yyyy-MM", new Date()));
        return prefix + "/monthPlanGant";
    }


    /**
     * 规格甘特图
     */
    @PostMapping("/getGantData")
    @ResponseBody
    public AjaxResult getGantData(Gante gante) {
        Date scheduleDate = gante.getScheduleDate()==null?new Date():gante.getScheduleDate();
        gante.setYear(DateUtils.getYear(scheduleDate));
        gante.setMonth(DateUtils.getMonth(scheduleDate));
        List<Gante> cxGanteDataList = iMdmMonthProdPlanService.getMonthPlanGanteData(gante);
        List<SysDictData> STORAGE_LOCATION = iSysDictDataCacheService.getType("STORAGE_LOCATION");
        if (CollectionUtils.isNotEmpty(cxGanteDataList) && CollectionUtils.isNotEmpty(STORAGE_LOCATION)){
            Map<String, String> dictMap = STORAGE_LOCATION.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel));
            cxGanteDataList.forEach(a->a.setStorageLocation(dictMap.get(a.getStorageLocation())));
        }
        return AjaxResult.success(cxGanteDataList);
    }


    /**
     * 日产量曲线图
     */
    @GetMapping("/dailyChart/{scheduleDate}")
    public String gantt(ModelMap mmap,@PathVariable("scheduleDate") String scheduleDate) {
        Map<String,List<Integer>> map = iMdmMonthProdPlanService.dailyChart(scheduleDate);
        mmap.put("date",map.get("date"));
        mmap.put("production",map.get("production"));
        mmap.put("molds",map.get("molds"));
        return prefix + "/dailyProductionLineChart";
    }

}
