package com.zlt.aps.controller.lh;

import com.ruoyi.api.gateway.system.domain.ExportLog;
import com.ruoyi.api.gateway.system.domain.ImportLog;
import com.ruoyi.api.gateway.system.domain.SysDictData;
import com.ruoyi.api.gateway.system.service.IExportLogService;
import com.ruoyi.api.gateway.system.service.IImportErrorLogService;
import com.ruoyi.api.gateway.system.service.IImportLogService;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.poi.ExcelUtil;
import com.ruoyi.common.core.web.controller.BaseController;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.text.Convert;
import com.ruoyi.common4ui.utils.StringUtils;
import com.zlt.aps.common.core.constant.ApsConstant;
import com.zlt.aps.common.core.enums.MoldChangeTypeEnums;
import com.zlt.aps.common.utils.ExportUtil;
import com.zlt.aps.common.utils.ImportUtil;
import com.zlt.aps.lh.api.domain.dto.LhApsMoldAdjustPlanDto;
import com.zlt.aps.lh.api.domain.entity.LhApsMoldAdjustPlan;
import com.zlt.aps.lh.api.service.ILhApsMoldAdjustPlanService;
import com.zlt.aps.lh.api.service.ILhMachineInfoService;
import com.zlt.file.encryptbyll.FileEncryptUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.ParseException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 硫化工序模具变动单APSController
 * @author Joran.zhang
 * @date 2022-06-07
 */
@Api(tags = "硫化工序模具变动单APS")
@Controller
@RequestMapping("/lh/lhApsMoldAdjustPlan")
public class LhApsMoldAdjustPlanController extends BaseController {

    @Autowired
    private ILhApsMoldAdjustPlanService iLhApsMoldAdjustPlanService;
    @Autowired
    private IExportLogService iExportLogService;
    @Autowired
    private IImportErrorLogService iImportErrorLogService;
    @Autowired
    private IImportLogService iImportLogService;

    @Autowired
    private ISysDictDataCacheService iSysDictDataCacheService;

    @Autowired
    private ILhMachineInfoService lhMachineInfoService;

    private final String prefix = "lh/lhApsMoldAdjustPlan";

    /**
     * 跳转至主页面
     */
    @RequiresPermissions("lh:lhApsMoldAdjustPlan:view")
    @GetMapping()
    public String toIndex(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return prefix + "/lhApsMoldAdjustPlan";
    }

    /**
     * 跳转至新增页面
     */
    @GetMapping("/add")
    @RequiresPermissions("lh:lhApsMoldAdjustPlan:add")
    public String add(ModelMap mmap) {
        mmap.put("lhApsMoldAdjustPlan", new LhApsMoldAdjustPlan());
        return prefix + "/edit";
    }

    /**
     * 跳转至修改页面
     */
    @GetMapping("/edit/{id}")
    @RequiresPermissions("lh:lhApsMoldAdjustPlan:edit")
    public String edit(@PathVariable("id") Long id, ModelMap mmap) {
        mmap.put("lhApsMoldAdjustPlan", iLhApsMoldAdjustPlanService.getInfo(id));
        return prefix + "/edit";
    }

    /**
     * 跳转至修改执行状态页面
     */
    @GetMapping("/changeExecute/{ids}")
    @RequiresPermissions("lh:lhApsMoldAdjustPlan:changeExecute")
    public String changeExecute(@PathVariable("ids") String ids, ModelMap mmap) {
        mmap.put("ids", ids);
        return prefix + "/changeExecute";
    }

    /**
     * 跳转到导入页面
     *
     */
    @GetMapping("/toImport")
    public String toImport(ModelMap mmap) {
        mmap.put("initDate", DateUtils.parseDateToStr("yyyy-MM-dd", DateUtils.addDays(new Date(), 1)));  //当前日期+1天
        return prefix + "/importData";
    }

    /**
     * 跳转到主子表编辑页面
     */
    @GetMapping("/toSubDataEdit")
    public String toSubDataEdit(ModelMap mmap) {
        mmap.put("lhApsMoldAdjustPlan", new LhApsMoldAdjustPlanDto());
        return prefix + "/subDataEdit";
    }

    /**
     * 根据条件查询硫化工序模具变动单APS列表
     */
    @ApiOperation("根据条件查询硫化工序模具变动单APS列表")
    @RequiresPermissions("lh:lhApsMoldAdjustPlan:list")
    @PostMapping("/list")
    @ResponseBody
    public TableDataInfo list(LhApsMoldAdjustPlan entity) {
        return iLhApsMoldAdjustPlanService.list(entity);
    }

    /**
     * 修改或新增硫化工序模具变动单APS
     */
    @ApiOperation("修改或新增硫化工序模具变动单APS")
    @RequiresPermissions("lh:lhApsMoldAdjustPlan:edit")
    @PostMapping("/edit")
    @ResponseBody
    public AjaxResult editSave(LhApsMoldAdjustPlan lhApsMoldAdjustPlan) {
        AjaxResult ajaxResult = null;
        if (lhApsMoldAdjustPlan.getId() != null){
            ajaxResult = iLhApsMoldAdjustPlanService.edit(lhApsMoldAdjustPlan);
        } else{
            ajaxResult = iLhApsMoldAdjustPlanService.add(lhApsMoldAdjustPlan);
        }
        return ajaxResult;
    }

    /**
     * 新增硫化工序模具变动单APS
     */
    @ApiOperation("新增硫化工序模具变动单APS")
    @RequiresPermissions("lh:lhApsMoldAdjustPlan:add")
    @PostMapping("/addSubData")
    @ResponseBody
    public AjaxResult addSubData(LhApsMoldAdjustPlanDto dto) {
        return iLhApsMoldAdjustPlanService.addSubData(dto);
    }

    /**
     * 删除硫化工序模具变动单APS
     */
    @ApiOperation("删除硫化工序模具变动单APS（id不为空）")
    @RequiresPermissions("lh:lhApsMoldAdjustPlan:remove")
    @PostMapping("/remove")
    @ResponseBody
    public AjaxResult remove(String ids) {
        Long[] arr = Convert.toLongArray(ids);
        return iLhApsMoldAdjustPlanService.remove(arr);
    }

    /**
     * 校验硫化工序模具变动单APS唯一性
     */
    @ApiOperation("校验硫化工序模具变动单APS唯一性")
    @PostMapping("/checkLhApsMoldAdjustPlanUnique")
    @ResponseBody
    public String checkLhApsMoldAdjustPlanUnique(LhApsMoldAdjustPlan lhApsMoldAdjustPlan) {
        return iLhApsMoldAdjustPlanService.checkLhApsMoldAdjustPlanUnique(lhApsMoldAdjustPlan);
    }

    /**
     * 发布排程
     */
    @ApiOperation("发布排程")
    @RequiresPermissions("lh:lhApsMoldAdjustPlan:publish")
    @PostMapping("/publish")
    @ResponseBody
    public AjaxResult publish(LhApsMoldAdjustPlan lhApsMoldAdjustPlan) {
        // 默认发布当天排程结果
        if (lhApsMoldAdjustPlan.getPlanDate() == null) {
            lhApsMoldAdjustPlan.setPlanDate(DateUtils.addDays(new Date(), 1));
        }
        return iLhApsMoldAdjustPlanService.publish(lhApsMoldAdjustPlan);
    }

    /**
     * 根据ids更改执行状态
     * @param lhApsMoldAdjustPlan ids、要更改的状态
     * @return 结果
     */
    @ApiOperation("更改执行状态")
    @RequiresPermissions("lh:lhApsMoldAdjustPlan:changeExecute")
    @PostMapping("/changeExecute")
    @ResponseBody
    public AjaxResult changeExecute(LhApsMoldAdjustPlan lhApsMoldAdjustPlan){
        return iLhApsMoldAdjustPlanService.changeExecute(lhApsMoldAdjustPlan);
    }

    /**
     * 导出硫化工序模具变动单APS
     */
    @ApiOperation("导出硫化工序模具变动单APS")
    @RequiresPermissions("lh:lhApsMoldAdjustPlan:export")
    @GetMapping("/export")
    @ResponseBody
    public void export(HttpServletResponse response,LhApsMoldAdjustPlan lhApsMoldAdjustPlan) throws IOException {
        String fileName = I18nUtil.getMessage("ui.data.column.lhApsMoldAdjustPlan.modelName");
        List<LhApsMoldAdjustPlan> list = iLhApsMoldAdjustPlanService.getList(lhApsMoldAdjustPlan);
        ExcelUtil<LhApsMoldAdjustPlan> util = new ExcelUtil<>(LhApsMoldAdjustPlan. class);
        Workbook workbook = util.exportExcel2(response, list, fileName);
        ExportLog exportLog = ExportUtil.uploadAndExportExcel(response, workbook, fileName, lhApsMoldAdjustPlan.toString(),"ApsConstant.PROCEDURE_CODE_XXX");
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
        String fileName = I18nUtil.getMessage("ui.data.column.lhApsMoldAdjustPlan.modelName");
        ExcelUtil<LhApsMoldAdjustPlan> util = new ExcelUtil<>(LhApsMoldAdjustPlan.class);
        util.exportExcel(response, null, fileName, fileName);
        return AjaxResult.success();
    }

    /**
     * excel数据导入
     *
     * @param file 要导入的文件
     * @param updateSupport 已存在的记录是否更新
     * @return 结果
     * @throws Exception 异常
     */
    @RequiresPermissions("lh:lhApsMoldAdjustPlan:import")
    @ApiOperation("excel数据导入")
    @PostMapping("/importData")
    @ResponseBody
    public AjaxResult importData(MultipartFile file, Date planDate, boolean updateSupport) throws Exception {
        //文件解密
        byte[] data = this.useFileEncrypt ? FileEncryptUtils.DecodeFile(file) : file.getBytes();
        ImportLog importLog = ImportUtil.getImportLogAndUploadFile(data, ApsConstant.PROCEDURE_CODE_LH,
                I18nUtil.getMessage("ui.data.column.lhApsMoldAdjustPlan.modelName"),file.getOriginalFilename());
        importLog = iImportLogService.add(importLog);
        //ExcelUtil<LhApsMoldAdjustPlan> util = new ExcelUtil<>(LhApsMoldAdjustPlan.class);
        InputStream in = new ByteArrayInputStream(data);
        List<SysDictData> changeTypeList = iSysDictDataCacheService.getType("MOLD_CHANGE_TYPE");
        Map<String, String> changeTypeMap = changeTypeList.stream().collect(Collectors.toMap(SysDictData::getDictLabel, SysDictData::getDictValue));
        /*List<LhMachineInfo> machineList=lhMachineInfoService.exportList(new LhMachineInfo());
        List<LhApsMoldAdjustPlan> list = parseObject(in,changeTypeMap,machineList);*/
        List<LhApsMoldAdjustPlan> list = parseObject(in, planDate,changeTypeMap);
       /* for(LhApsMoldAdjustPlan lhApsMoldAdjustPlan:list){
            System.out.println(lhApsMoldAdjustPlan.toString());
        }*/
        AjaxResult ajaxResult = iLhApsMoldAdjustPlanService.importData(list, updateSupport, importLog.getId());
        // 更新日志记录成功数，失败数
        ImportUtil.updateImportLogAndFormatMsg(importLog, ajaxResult, iImportLogService);
        // 保存失败记录
        ImportUtil.saveImportErrorLogs(ajaxResult, iImportErrorLogService);
        return ajaxResult;
    }

    /**
     * 根据excel的io流解析成对应集合对象
     * @param in io
     * @return 解析后集合对象
     * @throws Exception 异常
     */
    private List<LhApsMoldAdjustPlan> parseObject(InputStream in, Date planDate, Map<String, String> changeTypeMap) throws Exception {

        List<LhApsMoldAdjustPlan> list = new ArrayList<>();
        if (in == null) {
            return list;
        }
        /*Map<String,String> machineMap=new HashMap<>();
        if(StringUtils.isNotEmpty(machineList)){
            machineMap = machineList.stream().collect(Collectors.toMap(LhMachineInfo::getMachineName, LhMachineInfo::getMachineCode));
        }*/

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
               LhApsMoldAdjustPlan apsMoldAdjustPlan=getRowData(sheet,row,changeTypeMap);
                // 所有数据都为空的情况，视为该行数据为无效行，不做导入
                if (apsMoldAdjustPlan == null) {
                    continue;
                }
               apsMoldAdjustPlan.setPlanDate(planDate);
               String changeTypeVal=apsMoldAdjustPlan.getChangeType();
               if(StringUtils.isNotEmpty(changeTypeVal)&&StringUtils.isNotEmpty(changeTypeMap)&&changeTypeMap.containsKey(changeTypeVal)){
                    String changTypeValue=changeTypeMap.get(changeTypeVal);
                    apsMoldAdjustPlan.setChangeType(changTypeValue); //类型
                }

              /*  //验证是否读取合并行
                if(apsMoldAdjustPlan.isMergeRow()){
                    list.add(apsMoldAdjustPlan);
                    i+=1; //读取下一行
                    Row mergeRow = sheet.getRow(i);
                    //获取合并行数据
                    getMergeRowData(sheet,apsMoldAdjustPlan,list,mergeRow,machineMap,changeTypeMap);
                    continue;
                }*/
                list.add(apsMoldAdjustPlan);
            }
        }
        return list;
    }

    /**
     * 获取合并行数据并添加到集合中
     * @param list
     * @param mergeRow
     */
    private void getMergeRowData(Sheet sheet,LhApsMoldAdjustPlan lastRowData,List<LhApsMoldAdjustPlan> list, Row mergeRow, Map<String, String> changeTypeMap) throws ParseException {
        if (mergeRow == null) {
            return;
        }
        LhApsMoldAdjustPlan nextRowData=getRowData(sheet,mergeRow,changeTypeMap);
        nextRowData.setPlanDate(lastRowData.getPlanDate());//设置同日期
        nextRowData.setLhMachineCode(lastRowData.getLhMachineCode()); //设置为同机台号
        nextRowData.setLhMachineName(lastRowData.getLhMachineName()); //设置为同机台
        String changeTypeVal=nextRowData.getChangeType();
        if(StringUtils.isNotEmpty(changeTypeVal)&&StringUtils.isNotEmpty(changeTypeMap)&&changeTypeMap.containsKey(changeTypeVal)){
            String changTypeValue=changeTypeMap.get(changeTypeVal);
            nextRowData.setChangeType(changTypeValue); //类型
        }
        list.add(nextRowData);

    }

    /**
     * 读取单行数据并进行数据组装
     * @param row
     * @param machineMap
     * @param changeTypeMap
     * @return
     */
    private LhApsMoldAdjustPlan getRowData(Sheet sheet,Row row, Map<String, String> changeTypeMap) throws ParseException {
        int index=0;
//        Date planDate=row.getCell(index).getDateCellValue(); //下达日期
        /*Date planDateVal = null;
        boolean isMergeRow = isMergedRegion(sheet,row.getRowNum(),index);
        if(isMergeRow){
            planDateVal = getMergedRegionDateValue(sheet,row.getRowNum(),index);
        }else{
            planDateVal = getCellDateValue(row.getCell(index)); //下达日期
        }
        index+=1;*/
//        String lhMachineNameVal=getCellValue(row.getCell(index)); //机台名称
        String lhMachineNameVal = null;
        boolean isMergeRow = isMergedRegion(sheet,row.getRowNum(),index);
        if(isMergeRow){
            lhMachineNameVal = getMergedRegionValue(sheet,row.getRowNum(),index);
        }else{
            lhMachineNameVal = getCellValue(row.getCell(index)); //机台名称
        }

        index+=1;
        String beforeSapCode=getCellValue(row.getCell(index)); //前SAP品号

        index+=1;
        String beforeSpecDesc=getCellValue(row.getCell(index)); //前规格

        index+=1;
        String beforeEmbryoCode=getCellValue(row.getCell(index)); //前胎胚代码

        index+=1;
        String tireRoughStockVal=getCellValue(row.getCell(index)); //库存数

        index+=1;
        String useMoldNumberVal=getCellValue(row.getCell(index)); //使用模数

        index+=1;
        String leftRightMold=getCellValue(row.getCell(index)); //左右模

      /*  index+=1;
        String leftMoldCode=getCellValue(row.getCell(index)); //左模具信息

        index+=1;
        String rightMoldCode=getCellValue(row.getCell(index)); //右模具信息*/

        index+=1;

        String changeTypeVal="";
//        boolean isMergeRow=isMergedRegion(sheet,row.getRowNum(),index);
//        if(isMergeRow){
//            changeTypeVal=getMergedRegionValue(sheet,row.getRowNum(),index);
//        }else{
//            changeTypeVal=getCellValue(row.getCell(index)); //换模类型
//        }
        changeTypeVal=getCellValue(row.getCell(index)); //换模类型

        index+=1;
        String afterSapCode=getCellValue(row.getCell(index)); //后SAP品号

        index+=1;
        String afterSpecDesc=getCellValue(row.getCell(index)); //后规格

        index+=1;
        String afterEmbryoCode=getCellValue(row.getCell(index)); //后胎胚代码

        index+=1;
//        Cell changeMoldTimeCell = row.getCell(index);
//        Date changeMoldTime= changeMoldTimeCell == null ? null : changeMoldTimeCell.getDateCellValue(); //更换时间
        Date changeMoldTimeVal = null;
        isMergeRow = isMergedRegion(sheet,row.getRowNum(),index);
        if(isMergeRow){
            changeMoldTimeVal = getMergedRegionDateValue(sheet,row.getRowNum(),index);
        }else{
            changeMoldTimeVal = getCellDateValue(row.getCell(index)); //更换时间
        }

        index+=1;
        String remark=getCellValue(row.getCell(index)); //备注

        LhApsMoldAdjustPlan apsMoldAdjustPlan = new LhApsMoldAdjustPlan();
//        apsMoldAdjustPlan.setPlanDate(planDate);
//        apsMoldAdjustPlan.setPlanDate(planDateVal);
        /*if(StringUtils.isNotEmpty(machineMap)&&machineMap.containsKey(lhMachineNameVal)){
            apsMoldAdjustPlan.setLhMachineCode(machineMap.get(lhMachineNameVal)); //解析名称转机台编号
        }*/
        Calendar calendar = Calendar.getInstance();
        // 如果日期为1970-01-01时，表示不符合日期表达式或为空
        calendar.setTimeInMillis(-28800000);
        if (StringUtils.isAllBlank(lhMachineNameVal, beforeSapCode, beforeEmbryoCode, beforeSpecDesc, tireRoughStockVal,
                useMoldNumberVal, leftRightMold, changeTypeVal, afterSapCode, afterEmbryoCode, afterSpecDesc, remark) && (changeMoldTimeVal == null || calendar.getTime().equals(changeMoldTimeVal))) {
            return null;
        }
        apsMoldAdjustPlan.setLhMachineName(lhMachineNameVal);
        apsMoldAdjustPlan.setBeforeSapCode(beforeSapCode);
        apsMoldAdjustPlan.setBeforeEmbryoCode(beforeEmbryoCode);
        apsMoldAdjustPlan.setBeforeSpecDesc(beforeSpecDesc);
        if(StringUtils.isNotEmpty(tireRoughStockVal)){
            // 去除 .0后缀
            tireRoughStockVal = tireRoughStockVal.replace(".0","");
            Integer tireRoughStock = Convert.toIntExcelUtil(tireRoughStockVal, Integer.MAX_VALUE);
            apsMoldAdjustPlan.setTireRoughStock(tireRoughStock);//设置胎胚库存
        }
        if(StringUtils.isNotEmpty(useMoldNumberVal)){
            BigDecimal useMoldNumberBig=new BigDecimal(useMoldNumberVal);
            apsMoldAdjustPlan.setUseMoldNumber(useMoldNumberBig.intValue());//使用模数
        }
        apsMoldAdjustPlan.setLeftRightMold(leftRightMold);//左右模
        //apsMoldAdjustPlan.setLeftMoldCode(leftMoldCode);//左模具信息
        apsMoldAdjustPlan.setChangeType(changeTypeVal); //原始类型
        apsMoldAdjustPlan.setMergeRow(isMergeRow);//设置是否为合并单元格
        //apsMoldAdjustPlan.setRightMoldCode(rightMoldCode);//右模具信息
        apsMoldAdjustPlan.setAfterSapCode(afterSapCode);
        apsMoldAdjustPlan.setAfterEmbryoCode(afterEmbryoCode);
        apsMoldAdjustPlan.setAfterSpecDesc(afterSpecDesc);
//        apsMoldAdjustPlan.setChangeMoldTime(changeMoldTime);
        apsMoldAdjustPlan.setChangeMoldTime(changeMoldTimeVal);
        apsMoldAdjustPlan.setRemark(remark);
        return apsMoldAdjustPlan;
    }

    /**
     * 判断指定的单元格是否是合并单元格
     *
     * @param sheet
     * @param row    行下标
     * @param column 列下标
     * @return
     */
    public boolean isMergedRegion(Sheet sheet, int row, int column) {
        int sheetMergeCount = sheet.getNumMergedRegions();
        for (int i = 0; i < sheetMergeCount; i++) {
            CellRangeAddress range = sheet.getMergedRegion(i);
            int firstColumn = range.getFirstColumn();
            int lastColumn = range.getLastColumn();
            int firstRow = range.getFirstRow();
            int lastRow = range.getLastRow();
            if (row >= firstRow && row <= lastRow) {
                if (column >= firstColumn && column <= lastColumn) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getMergedRegionValue(Sheet sheet, int row, int column) {
        int sheetMergeCount = sheet.getNumMergedRegions();

        for (int i = 0; i < sheetMergeCount; i++) {
            CellRangeAddress ca = sheet.getMergedRegion(i);
            int firstColumn = ca.getFirstColumn();
            int lastColumn = ca.getLastColumn();
            int firstRow = ca.getFirstRow();
            int lastRow = ca.getLastRow();

            if (row >= firstRow && row <= lastRow) {

                if (column >= firstColumn && column <= lastColumn) {
                    Row fRow = sheet.getRow(firstRow);
                    Cell fCell = fRow.getCell(firstColumn);
                    return getCellValue(fCell);
                }
            }
        }
        return "";
    }

    public String getCellValue(Cell cell){
        Object val=null;
        if(cell==null){
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC || cell.getCellType() == CellType.FORMULA) {
            val = cell.getNumericCellValue();
            //当val值为大值时，很可能解析为科学计数法显示
            String valStr=val+"";
            if(valStr.indexOf("E")>=0){
                BigDecimal realValue=  new BigDecimal(valStr);
                val=realValue.toPlainString();
            }
        } else if (cell.getCellType() == CellType.STRING) {
            val = cell.getStringCellValue();
        } else if (cell.getCellType() == CellType.BOOLEAN) {
            val = cell.getBooleanCellValue();
        }
        if(val==null){
            return null;
        }
        return val+"";
    }

    private Date getMergedRegionDateValue(Sheet sheet, int row, int column) throws ParseException {
        int sheetMergeCount = sheet.getNumMergedRegions();

        for (int i = 0; i < sheetMergeCount; i++) {
            CellRangeAddress ca = sheet.getMergedRegion(i);
            int firstColumn = ca.getFirstColumn();
            int lastColumn = ca.getLastColumn();
            int firstRow = ca.getFirstRow();
            int lastRow = ca.getLastRow();

            if (row >= firstRow && row <= lastRow) {

                if (column >= firstColumn && column <= lastColumn) {
                    Row fRow = sheet.getRow(firstRow);
                    Cell fCell = fRow.getCell(firstColumn);
                    return getCellDateValue(fCell);
                }
            }
        }
        return null;
    }

    private Date getCellDateValue(Cell cell) throws ParseException {
        Date val = null;
        if(cell == null){
            return null;
        }
        // excel内格式不是日期类型转换报错时，转换后如果为null，则代表不符合日期表达式，返回1970-01-01，后端校验时会给予提示
        try {
            val = cell.getDateCellValue();
        } catch (Exception e) {
            val = DateUtils.parseDate(cell.getStringCellValue());
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(-28800000);
        return val == null && StringUtils.isNotBlank(cell.getStringCellValue()) ? calendar.getTime() : val;
    }

    /**
     * 获取前规格信息
     * @param sapCode sap品号
     * @param embryoCode 胎胚代码
     * @return 规格信息
     */
    @ApiOperation("获取前规格信息")
    @PostMapping("/getBeforeSpecDesc")
    @ResponseBody
    public AjaxResult getBeforeSpecDesc(LhApsMoldAdjustPlan lhApsMoldAdjustPlan) {
        return AjaxResult.success(iLhApsMoldAdjustPlanService.getBeforeSpecDesc(lhApsMoldAdjustPlan));
    }

    /**
     * 获取后规格信息
     * @param sapCode sap品号
     * @param embryoCode 胎胚代码
     * @return 规格信息
     */
    @ApiOperation("获取后规格信息")
    @PostMapping("/getAfterSpecDesc")
    @ResponseBody
    public AjaxResult getAfterSpecDesc(LhApsMoldAdjustPlan lhApsMoldAdjustPlan) {
        return AjaxResult.success(iLhApsMoldAdjustPlanService.getAfterSpecDesc(lhApsMoldAdjustPlan));
    }

    /**
     * 校验是否必填
     * @return 结果
     */
    @ApiOperation("校验是否必填")
    @PostMapping("/checkIsRequired")
    @ResponseBody
    public AjaxResult checkIsRequired(LhApsMoldAdjustPlanDto dto) {
        List<LhApsMoldAdjustPlan> apsMoldAdjustPlanList = dto.getApsMoldAdjustPlanList();
        String message = "";
        for (LhApsMoldAdjustPlan lhApsMoldAdjustPlan : apsMoldAdjustPlanList) {
            String changeType = lhApsMoldAdjustPlan.getChangeType();
            if(StringUtils.isNotEmpty(changeType)){
                MoldChangeTypeEnums moldChangeTypeEnums=MoldChangeTypeEnums.getMoldChangeTypeByValue(changeType);
                if(moldChangeTypeEnums == null){
                    return AjaxResult.error(I18nUtil.getMessage(""));
                }
                // 前sap和胎胚不能同时为空校验，后sap和胎胚不能同时为空校验
                if (StringUtils.isAllBlank(lhApsMoldAdjustPlan.getBeforeSapCode(), lhApsMoldAdjustPlan.getBeforeEmbryoCode(),
                        lhApsMoldAdjustPlan.getAfterSapCode(), lhApsMoldAdjustPlan.getAfterEmbryoCode())) {
                    message = StringUtils.format(I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.sapAndEmbryoCodeCannotEmptyAtTheSameTime"),
                            lhApsMoldAdjustPlan.getLhMachineName(), moldChangeTypeEnums.getZhName());
                    return AjaxResult.error(message);
                }
                // 未填写使用模数默认为 2
                if (lhApsMoldAdjustPlan.getUseMoldNumber() == null) {
                    lhApsMoldAdjustPlan.setUseMoldNumber(2);
                }
                switch (moldChangeTypeEnums){
                    case LEFT_CLOSE_MERGE:
                    case RIGHT_CLOSE_MERGE:
                    case LEFT_POINT_MERGE:
                    case RIGHT_POINT_MERGE:
                    case CLOSE_OUT_MEGER:
                        //确认左模收尾合并、右模收尾合并、左模点数合并、右模点数合并、收尾合并类型库存数必填
                        if (lhApsMoldAdjustPlan.getUseMoldNumber() == null) {
                            message= StringUtils.format(I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.changeType.stockEmpty"),lhApsMoldAdjustPlan.getLhMachineName(),moldChangeTypeEnums.getZhName());
                            return AjaxResult.error(message);
                        }
                        break;
                    case CLOSE_OUT_CHANGE:
                    case POINT_OUT_CHANGE:
                        // 收尾换、点数换，库存数、使用模数必填
                        if (lhApsMoldAdjustPlan.getTireRoughStock() == null || lhApsMoldAdjustPlan.getUseMoldNumber() == null) {
                            message = StringUtils.format(I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.changeType.useMoldAndChangeTimeEmpty"),
                                    lhApsMoldAdjustPlan.getLhMachineName(), moldChangeTypeEnums.getZhName());
                            return AjaxResult.error(message);
                        }
                        break;
                    case LEFT_MOLD_MERGE:
                    case RIGHT_MOLD_MERGE:
                    case SPLIT_MOLD_MEGER:
                        // 左模合并、右模合并、拆模合并，更换时间必填
                        if (lhApsMoldAdjustPlan.getChangeMoldTime() == null) {
                            message = StringUtils.format(I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.changeType.changeTimeEmpty"),
                                    lhApsMoldAdjustPlan.getLhMachineName(), moldChangeTypeEnums.getZhName());
                            return AjaxResult.error(message);
                        }
                        break;
                    case SPLIT_OUT_CHANGE:
                        //拆模换，换模时间、使用模数必填
                        if (lhApsMoldAdjustPlan.getChangeMoldTime() == null || lhApsMoldAdjustPlan.getUseMoldNumber() == null) {
                            message = StringUtils.format(I18nUtil.getMessage("ui.data.message.lhApsMoldAdjustPlan.changeType.useMoldAndChangeTimeEmpty"),
                                    lhApsMoldAdjustPlan.getLhMachineName(), moldChangeTypeEnums.getZhName());
                            return AjaxResult.error(message);
                        }
                        break;
                    default:
                        break;
                }
            }
        }
        return AjaxResult.success();
    }
}
