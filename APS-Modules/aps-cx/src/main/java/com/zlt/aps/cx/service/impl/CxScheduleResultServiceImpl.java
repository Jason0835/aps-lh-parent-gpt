package com.zlt.aps.cx.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.core.web.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.aps.common.core.domain.CellStyle;
import com.zlt.aps.common.core.utils.ExcelUtils;
import com.zlt.aps.constant.FactoryConstant;
import com.zlt.aps.cx.entity.schedule.CxScheduleResult;
import com.zlt.aps.cx.mapper.CxScheduleResultMapper;
import com.zlt.aps.cx.service.CxScheduleDetailService;
import com.zlt.aps.cx.service.CxScheduleResultService;
import com.zlt.aps.cx.vo.CxScheduleResultTemplateImportVO;
import com.zlt.aps.mp.api.domain.entity.MpStructureAllocation;
import com.zlt.aps.mp.api.enums.AlternativeTypeEnum;
import com.zlt.aps.mp.api.service.IMpStructureAllocationRemoteService;
import com.zlt.bill.common.service.AbstractDocService;
import com.zlt.common.enums.ImportErrorTypeEnums;
import com.zlt.common.utils.ImportExcelValidatedUtils;
import com.zlt.common.utils.PubUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 成型排程结果服务实现类
 *
 * @author APS Team
 */
@Slf4j
@Service
public class CxScheduleResultServiceImpl extends AbstractDocService<CxScheduleResult>
        implements CxScheduleResultService {

    @Autowired
    private CxScheduleResultMapper cxScheduleResultMapper;

    @Autowired
    private CxScheduleDetailService cxScheduleDetailService;

    @Autowired
    private IMpStructureAllocationRemoteService mpStructureAllocationRemoteService;

    @Override
    public List<CxScheduleResult> listByScheduleDate(LocalDate scheduleDate) {
        return cxScheduleResultMapper.selectList(new LambdaQueryWrapper<CxScheduleResult>()
                .eq(CxScheduleResult::getScheduleDate, scheduleDate.atStartOfDay())
                .orderByAsc(CxScheduleResult::getCxMachineCode));
    }

    @Override
    public List<CxScheduleResult> listByLhScheduleIds(List<Long> lhScheduleIds) {
        if (CollectionUtils.isEmpty(lhScheduleIds)) {
            return Collections.emptyList();
        }
        List<Long> queryIds = lhScheduleIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (CollectionUtils.isEmpty(queryIds)) {
            return Collections.emptyList();
        }
        QueryWrapper<CxScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("IS_DELETE", "0");
        queryWrapper.and(wrapper -> {
            boolean first = true;
            for (Long lhScheduleId : queryIds) {
                // LH_SCHEDULE_IDS 主要保存为逗号分隔字符串，同时兼容历史数据中的中文逗号、斜杠和分号。
                // 统一转成英文逗号后再使用 FIND_IN_SET，避免 ID=1 误匹配 10、11。
                String findInSetSql = "FIND_IN_SET({0}, REPLACE(REPLACE(REPLACE(REPLACE(REPLACE(LH_SCHEDULE_IDS, '，', ','), '/', ','), '；', ','), ';', ','), ' ', ''))";
                if (first) {
                    wrapper.apply(findInSetSql, String.valueOf(lhScheduleId));
                    first = false;
                } else {
                    wrapper.or().apply(findInSetSql, String.valueOf(lhScheduleId));
                }
            }
        });
        return cxScheduleResultMapper.selectList(queryWrapper);
    }

    @Override
    public AjaxResult importData(List<CxScheduleResult> list, boolean updateSupport, Long importLogId) {
        int successNum = 0;
        int failureNum = 0;
        List<CxScheduleResult> importList = new ArrayList<>();
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        String uniqueMsg = I18nUtil.getMessage("import.validated.unique");

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxScheduleResult docEntity = list.get(i);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(importLogId, errorNum, docEntity);
            ImportExcelValidatedUtils.validatedRepeat(list, docEntity, i, 2, importLogId, validated);
            if (CollectionUtils.isNotEmpty(validated)) {
                failureNum++;
                docEntity.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        for (int i = 0; i < list.size(); i++) {
            int errorNum = i + 2;
            CxScheduleResult docEntity = list.get(i);
            if (docEntity.getId() != null && docEntity.getId() == -999L) {
                continue;
            }

            if (StringUtils.isBlank(docEntity.getCxMachineCode())) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.cxScheduleResult.machineCodeRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            if (docEntity.getScheduleDate() == null) {
                failureNum++;
                String message = I18nUtil.getMessage("ui.data.alert.cxScheduleResult.scheduleDateRequired");
                ImportExcelValidatedUtils.addImportErrorLog(importLogId, ImportErrorTypeEnums.OTHERS.getCode(),
                        errorNum, String.format(message, errorNum), importErrorLogs);
                continue;
            }

            if (checkUnique(docEntity).equals(UserConstants.UNIQUE)) {
                importList.add(docEntity);
                successNum++;
            } else {
                if (updateSupport) {
                    QueryWrapper<CxScheduleResult> queryWrapper = new QueryWrapper<>();
                    queryWrapper.eq("CX_MACHINE_CODE", docEntity.getCxMachineCode());
                    queryWrapper.eq("SCHEDULE_DATE", docEntity.getScheduleDate());
                    queryWrapper.eq("ORDER_NO", docEntity.getOrderNo());
                    CxScheduleResult existEntity = cxScheduleResultMapper.selectOne(queryWrapper);
                    if (existEntity != null) {
                        docEntity.setId(existEntity.getId());
                        importList.add(docEntity);
                        successNum++;
                    }
                } else {
                    failureNum++;
                    ImportExcelValidatedUtils.addImportErrorLog(importLogId, errorNum,
                            String.format(uniqueMsg, errorNum), importErrorLogs);
                }
            }
        }

        if (CollectionUtils.isEmpty(importList)) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        for (CxScheduleResult entity : importList) {
            if (entity.getId() != null) {
                cxScheduleResultMapper.updateById(entity);
            } else {
                cxScheduleResultMapper.insert(entity);
            }
        }

        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    @Override
    public String checkUnique(CxScheduleResult entity) {
        QueryWrapper<CxScheduleResult> queryWrapper = new QueryWrapper<>();
        queryWrapper.ne(PubUtil.isNotEmpty(entity.getFieldValueByFieldName("id")), "ID", entity.getFieldValueByFieldName("id"));
        queryWrapper.eq("CX_MACHINE_CODE", entity.getCxMachineCode());
        queryWrapper.eq("SCHEDULE_DATE", entity.getScheduleDate());
        queryWrapper.eq("ORDER_NO", entity.getOrderNo());

        if (cxScheduleResultMapper.selectCount(queryWrapper) > 0) {
            return UserConstants.NOT_UNIQUE;
        } else {
            return UserConstants.UNIQUE;
        }
    }

    @Override
    protected List<String> getCheckUniqueFields() {
        return Arrays.asList("cxMachineCode", "scheduleDate", "orderNo");
    }

    @Override
    protected String getDocTypeCode() {
        return "CX_SCHEDULE_RESULT";
    }

    @Override
    public byte[] exportData(List<CxScheduleResult> list, Date scheduleDate) {
        java.io.InputStream inputStream = this.getClass().getClassLoader()
                .getResourceAsStream("excelModel/cxjhtemplate.xls");
        if (Objects.isNull(inputStream)) {
            throw new ServiceException("成型计划导入模板不存在");
        }
        List<CxScheduleResult> exportList = Objects.isNull(list) ? Collections.emptyList() : list;
        Map<String, Object> tableMap = buildExportTableMap(exportList, scheduleDate);
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        excelDataList.add(buildExportDataList(exportList));
        return ExcelUtils.writeMultiList(inputStream, 1, tableMap, excelDataList);
    }

    /**
     * 导出成型余量数据（含两个Sheet页：成型余量 + 成型计划明细）。
     *
     * @param queryVO 查询条件，按成型排程结果列表查询口径筛选数据
     * @param fileName 导出文件名，保留用于对齐远程调用契约
     * @return 成型余量Excel文件字节数组
     */
    @Override
    public byte[] exportCxRemainQty(CxScheduleResult queryVO, String fileName) {
        // 按成型排程结果列表的查询口径查询明细数据
        List<CxScheduleResult> list = cxScheduleResultMapper.selectList(buildCxRemainQtyQueryWrapper(queryVO));

        // 构建第一页（成型余量）数据
        byte[] firstSheetBytes = buildFirstSheetBytes(list);

        // 加载第一页结果工作簿
        XSSFWorkbook finalWorkbook;
        try {
            finalWorkbook = new XSSFWorkbook(new ByteArrayInputStream(firstSheetBytes));
        } catch (Exception e) {
            throw new ServiceException("读取成型余量导出结果失败", e);
        }

        // 构建第二页（成型计划明细），写入最终工作簿
        buildSecondSheet(finalWorkbook, list);

        // 输出最终工作簿字节数组
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            finalWorkbook.write(out);
            finalWorkbook.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new ServiceException("导出Excel失败", e);
        }
    }

    /**
     * 构建第一页（成型余量）数据。
     *
     * @param list 成型排程结果明细列表
     * @return 成型余量Sheet的字节数组
     */
    private byte[] buildFirstSheetBytes(List<CxScheduleResult> list) {
        InputStream inputStream = this.getClass().getClassLoader()
                .getResourceAsStream("excelModel/cxyl.xlsx");
        if (Objects.isNull(inputStream)) {
            throw new ServiceException("成型余量导出模板不存在");
        }

        // 按机台+物料合并余量后填充模板
        Map<String, Object> tableMap = new HashMap<>(16);
        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        excelDataList.add(buildCxRemainQtyExportDataList(list));
        return ExcelUtils.writeMultiList(inputStream, 0, tableMap, excelDataList);
    }

    /**
     * 构建第二页（成型计划明细），直接操作备份模板的单元格填入数据。
     *
     * @param finalWorkbook 最终输出工作簿，第二页将追加到此工作簿
     * @param list 成型排程结果明细列表
     */
    private void buildSecondSheet(XSSFWorkbook finalWorkbook, List<CxScheduleResult> list) {
        InputStream templateInput = this.getClass().getClassLoader()
                .getResourceAsStream("excelModel/cxjhtemplate_backup.xlsx");
        if (Objects.isNull(templateInput)) {
            throw new ServiceException("成型计划模板不存在");
        }

        try {
            XSSFWorkbook templateWorkbook = new XSSFWorkbook(templateInput);
            Sheet sheet = templateWorkbook.getSheetAt(0);

            List<CxScheduleResult> exportList = Objects.isNull(list) ? Collections.emptyList() : list;

            // Step 1: 修改标题日期
            applyTitleDate(sheet, exportList);

            // Step 2: 修改班次日期
            applyShiftDates(sheet, exportList);

            // Step 3: 清除 Row 7+ 示例数据（不删行，保留图片锚点）
            clearSampleData(sheet);

            // Step 4: 填入数据行（从 Row 7 开始）
            fillDataRows(sheet, exportList);

            // 复制到最终工作簿
            ExcelUtils.copySheet(templateWorkbook, 0, finalWorkbook);

            // copySheet 对隐藏列和图片支持不完善，显式补上
            int newSheetIdx = finalWorkbook.getNumberOfSheets() - 1;
            Sheet targetSheet = finalWorkbook.getSheetAt(newSheetIdx);
            targetSheet.setColumnHidden(0, true); // A
            targetSheet.setColumnHidden(1, true); // B
            targetSheet.setColumnHidden(2, true); // C

            // 复制 logo 图片：从模板中取出图片数据，写入最终工作簿
            copyLogoPicture(templateWorkbook, finalWorkbook, targetSheet);

            templateWorkbook.close();
        } catch (ServiceException e) {
            throw e;
        } catch (Exception e) {
            throw new ServiceException("生成成型计划Sheet失败", e);
        }
    }

    /**
     * 从模板工作簿自动发现图片，复制到最终工作簿的目标Sheet中。
     * 使用 POI 底层 OPCPackage 读取图片数据和锚点信息。
     */
    private void copyLogoPicture(XSSFWorkbook sourceWorkbook, XSSFWorkbook targetWorkbook, Sheet targetSheet) {
        try {
            // 尝试从源 Sheet 获取绘图，若 shapes 为空则图片引用可能丢失
            XSSFSheet sourceSheet = sourceWorkbook.getSheetAt(0);
            XSSFDrawing sourceDrawing = sourceSheet.getDrawingPatriarch();
            if (sourceDrawing != null && !sourceDrawing.getShapes().isEmpty()) {
                // 第1种方式：通过 POI 绘图对象复制
                XSSFDrawing targetDrawing = ((XSSFSheet) targetSheet).createDrawingPatriarch();
                for (XSSFShape shape : sourceDrawing.getShapes()) {
                    if (shape instanceof XSSFPicture) {
                        XSSFPicture picture = (XSSFPicture) shape;
                        XSSFPictureData pictureData = picture.getPictureData();
                        int pictureIdx = targetWorkbook.addPicture(
                                pictureData.getData(), pictureData.getPictureType());
                        XSSFClientAnchor anchor = (XSSFClientAnchor) picture.getClientAnchor();
                        XSSFClientAnchor newAnchor = new XSSFClientAnchor(
                                anchor.getDx1(), anchor.getDy1(),
                                anchor.getDx2(), anchor.getDy2(),
                                anchor.getCol1(), anchor.getRow1(),
                                anchor.getCol2(), anchor.getRow2());
                        newAnchor.setAnchorType(anchor.getAnchorType());
                        targetDrawing.createPicture(newAnchor, pictureIdx);
                    }
                }
                return;
            }

            // 第2种方式：直接读 zip 中的图片文件，按模板固定锚点写入
            byte[] imageBytes = readFileFromWorkbookZip(sourceWorkbook, "xl/media/image1.png");
            if (imageBytes == null || imageBytes.length == 0) return;

            int pictureIdx = targetWorkbook.addPicture(imageBytes, Workbook.PICTURE_TYPE_PNG);
            XSSFDrawing targetDrawing = ((XSSFSheet) targetSheet).createDrawingPatriarch();
            // 锚点：C0 row0 到 E2（与原始模板一致：col3~5, row0~2）
            XSSFClientAnchor anchor = new XSSFClientAnchor(28575, 52070, 647700, 152400, 3, 0, 5, 2);
            targetDrawing.createPicture(anchor, pictureIdx);
        } catch (Exception e) {
            log.warn("复制logo图片失败: {}", e.getMessage());
        }
    }

    /**
     * 从 XSSFWorkbook 底层 zip 包中读取指定路径的原始字节。
     */
    private byte[] readFileFromWorkbookZip(XSSFWorkbook workbook, String entryPath) {
        try {
            java.lang.reflect.Field pkgField = XSSFWorkbook.class.getDeclaredField("pkg");
            pkgField.setAccessible(true);
            org.apache.poi.openxml4j.opc.OPCPackage pkg =
                    (org.apache.poi.openxml4j.opc.OPCPackage) pkgField.get(workbook);
            org.apache.poi.openxml4j.opc.PackagePart part = pkg.getPart(
                    org.apache.poi.openxml4j.opc.PackagingURIHelper.createPartName(
                            new java.net.URI(null, null, "/" + entryPath, null)));
            try (InputStream is = part.getInputStream()) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) > -1) bos.write(buf, 0, n);
                return bos.toByteArray();
            }
        } catch (Exception e) {
            log.warn("读取工作簿内文件失败: {} - {}", entryPath, e.getMessage());
            return null;
        }
    }

    /**
     * 修改标题 H1：将固定日期前缀替换为排程日期。
     */
    private void applyTitleDate(Sheet sheet, List<CxScheduleResult> list) {
        Row row1 = sheet.getRow(0);
        if (row1 == null) return;
        Cell h1 = row1.getCell(7); // H1
        if (h1 == null) return;
        String title = h1.getStringCellValue();
        if (title == null) return;

        Date scheduleDate = list.stream()
                .map(CxScheduleResult::getScheduleDate)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (scheduleDate == null) return;

        String datePrefix = cn.hutool.core.date.DateUtil.format(scheduleDate, "yyyy年MM月dd日");
        // 原始标题格式: "2026年5月3日全钢成型工程..."，替换日期前缀
        String prefix = "全钢成型工程生产";
        int idx = title.indexOf(prefix);
        if (idx > 0) {
            title = datePrefix + title.substring(idx);
        }
        h1.setCellValue(title);
    }

    /**
     * 修改 Row 4 各班次日期，替换原固定日期为排程日期。
     */
    private void applyShiftDates(Sheet sheet, List<CxScheduleResult> list) {
        Date scheduleDate = list.stream()
                .map(CxScheduleResult::getScheduleDate)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
        if (scheduleDate == null) return;

        java.time.LocalDate baseDate = cn.hutool.core.date.DateUtil.toLocalDateTime(scheduleDate).toLocalDate();
        java.time.LocalDate d1 = baseDate.minusDays(2);
        java.time.LocalDate d2 = baseDate.minusDays(1);
        java.time.LocalDate d3 = baseDate;
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MM/dd");

        String[] shiftFormats = {
                "早班 Ca sáng %s", // D1
                "中班 Ca chiều %s", // D1
                "夜班 Ca đêm %s", // D2
                "早班 Ca sáng %s", // D2
                "中班 Ca chiều %s", // D2
                "夜班 Ca đêm %s", // D3
                "早班 Ca sáng %s", // D3
                "中班 Ca chiều %s", // D3
        };
        String[] dates = {
                d1.format(fmt), d1.format(fmt),
                d2.format(fmt), d2.format(fmt), d2.format(fmt),
                d3.format(fmt), d3.format(fmt), d3.format(fmt)
        };
        int[] cols = {15, 20, 25, 30, 35, 40, 45, 50}; // P,U,Z,AE,AJ,AO,AT,AY (0-based)

        Row row4 = sheet.getRow(3);
        if (row4 == null) return;
        for (int i = 0; i < cols.length; i++) {
            Cell cell = row4.getCell(cols[i]);
            if (cell != null) {
                cell.setCellValue(String.format(shiftFormats[i], dates[i]));
            }
        }
    }

    /**
     * 清除 Row 7+ 的示例数据（保留行结构，不删行以保护图片锚点）。
     */
    private void clearSampleData(Sheet sheet) {
        int lastRow = sheet.getLastRowNum();
        for (int r = 6; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row != null) {
                for (int c = 0; c < row.getLastCellNum(); c++) {
                    Cell cell = row.getCell(c);
                    if (cell != null) {
                        cell.setCellValue((String) null);
                    }
                }
            }
        }
    }

    /**
     * 填入数据行：按机台分组，每组末尾插入小计行。
     * 数据从 Row 7 (0-based row 6) 开始填入。
     */
    private void fillDataRows(Sheet sheet, List<CxScheduleResult> exportList) {
        if (CollectionUtils.isEmpty(exportList)) return;

        // 按机台编码分组
        Map<String, List<CxScheduleResult>> groupMap = exportList.stream()
                .collect(Collectors.groupingBy(
                        item -> PubUtil.isNotEmpty(item.getCxMachineCode()) ? item.getCxMachineCode() : "",
                        LinkedHashMap::new,
                        Collectors.toList()));

        int rowIdx = 6; // 0-based, Row 7
        for (Map.Entry<String, List<CxScheduleResult>> entry : groupMap.entrySet()) {
            List<CxScheduleResult> groupList = entry.getValue();
            groupList.sort(Comparator.comparing(
                    item -> PubUtil.isNotEmpty(item.getMaterialCode()) ? item.getMaterialCode() : "",
                    String::compareTo));

            for (CxScheduleResult item : groupList) {
                writeDataRow(sheet, rowIdx++, item);
            }
            // 小计行
            writeSubtotalRow(sheet, rowIdx++, groupList);
        }
    }

    /**
     * 写入一行明细数据到模板。
     * 列映射: C4=机台, C5=结构, C6=胎胚编码, C7=胎胚描述, C8=物料描述,
     * C9=物料编码, C12=合计成型余量, C13=合计硫化余量, C14=胎胚库存, C15=硫化班产,
     * C16-C55=8班次x5列, C56=合计计划, C57=合计实际, C58=总计, C59=备注, C60=硫化机台数
     */
    private void writeDataRow(Sheet sheet, int rowIdx, CxScheduleResult item) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
            row = sheet.createRow(rowIdx);
        }
        int c = 0;
        // C1-C3: skip
        c = 3;
        setCellVal(row.createCell(c++), item.getCxMachineCode());       // C4
        setCellVal(row.createCell(c++), item.getStructureName());       // C5
        setCellVal(row.createCell(c++), item.getEmbryoCode());          // C6
        setCellVal(row.createCell(c++), item.getMaterialDesc());        // C7
        setCellVal(row.createCell(c++), item.getMainMaterialDesc());    // C8
        setCellVal(row.createCell(c++), item.getMaterialCode());        // C9
        setCellVal(row.createCell(c++), null);                          // C10 TD胶种
        setCellVal(row.createCell(c++), null);                          // C11 TD整车条数
        setCellVal(row.createCell(c++), item.getCxRemainQty());         // C12
        setCellVal(row.createCell(c++), item.getLhRemainQty());         // C13
        setCellVal(row.createCell(c++), item.getTotalStock());          // C14
        setCellVal(row.createCell(c++), item.getLhClassQty());          // C15

        // C16-C55: 8班次 x 5列
        setCellVal(row.createCell(c++), item.getClass1PlanQty());
        setCellVal(row.createCell(c++), item.getClass1FinishQty());
        setCellVal(row.createCell(c++), item.getClass1Analysis());
        setCellVal(row.createCell(c++), item.getClass1RecipeType());
        setCellVal(row.createCell(c++), item.getClass1RecipeNo());

        setCellVal(row.createCell(c++), item.getClass2PlanQty());
        setCellVal(row.createCell(c++), item.getClass2FinishQty());
        setCellVal(row.createCell(c++), item.getClass2Analysis());
        setCellVal(row.createCell(c++), item.getClass2RecipeType());
        setCellVal(row.createCell(c++), item.getClass2RecipeNo());

        setCellVal(row.createCell(c++), item.getClass3PlanQty());
        setCellVal(row.createCell(c++), item.getClass3FinishQty());
        setCellVal(row.createCell(c++), item.getClass3Analysis());
        setCellVal(row.createCell(c++), item.getClass3RecipeType());
        setCellVal(row.createCell(c++), item.getClass3RecipeNo());

        setCellVal(row.createCell(c++), item.getClass4PlanQty());
        setCellVal(row.createCell(c++), item.getClass4FinishQty());
        setCellVal(row.createCell(c++), item.getClass4Analysis());
        setCellVal(row.createCell(c++), item.getClass4RecipeType());
        setCellVal(row.createCell(c++), item.getClass4RecipeNo());

        setCellVal(row.createCell(c++), item.getClass5PlanQty());
        setCellVal(row.createCell(c++), item.getClass5FinishQty());
        setCellVal(row.createCell(c++), item.getClass5Analysis());
        setCellVal(row.createCell(c++), item.getClass5RecipeType());
        setCellVal(row.createCell(c++), item.getClass5RecipeNo());

        setCellVal(row.createCell(c++), item.getClass6PlanQty());
        setCellVal(row.createCell(c++), item.getClass6FinishQty());
        setCellVal(row.createCell(c++), item.getClass6Analysis());
        setCellVal(row.createCell(c++), item.getClass6RecipeType());
        setCellVal(row.createCell(c++), item.getClass6RecipeNo());

        setCellVal(row.createCell(c++), item.getClass7PlanQty());
        setCellVal(row.createCell(c++), item.getClass7FinishQty());
        setCellVal(row.createCell(c++), item.getClass7Analysis());
        setCellVal(row.createCell(c++), item.getClass7RecipeType());
        setCellVal(row.createCell(c++), item.getClass7RecipeNo());

        setCellVal(row.createCell(c++), item.getClass8PlanQty());
        setCellVal(row.createCell(c++), item.getClass8FinishQty());
        setCellVal(row.createCell(c++), item.getClass8Analysis());
        setCellVal(row.createCell(c++), item.getClass8RecipeType());
        setCellVal(row.createCell(c++), item.getClass8RecipeNo());

        // C56-C60: 合计
        BigDecimal totalPlan = sumPlan(item);
        BigDecimal totalFinish = sumFinish(item);
        setCellVal(row.createCell(c++), totalPlan);      // C56 合计计划
        setCellVal(row.createCell(c++), totalFinish);     // C57 合计实际
        setCellVal(row.createCell(c++), totalPlan);       // C58 总计
        setCellVal(row.createCell(c++), item.getRemark());// C59 备注
        setCellVal(row.createCell(c++), item.getLhMachineQty()); // C60
    }

    /**
     * 写入小计行。
     */
    private void writeSubtotalRow(Sheet sheet, int rowIdx, List<CxScheduleResult> groupList) {
        Row row = sheet.getRow(rowIdx);
        if (row == null) {
            row = sheet.createRow(rowIdx);
        }
        int c = 3;
        setCellVal(row.createCell(c++), "小计"); // C4
        c = 4; // skip C5-C15
        c = 15;

        BigDecimal[] planSums = new BigDecimal[9];
        BigDecimal[] finishSums = new BigDecimal[9];
        for (int i = 1; i <= 8; i++) {
            planSums[i] = BigDecimal.ZERO;
            finishSums[i] = BigDecimal.ZERO;
        }
        for (CxScheduleResult item : groupList) {
            planSums[1] = safeAdd(planSums[1], item.getClass1PlanQty());
            planSums[2] = safeAdd(planSums[2], item.getClass2PlanQty());
            planSums[3] = safeAdd(planSums[3], item.getClass3PlanQty());
            planSums[4] = safeAdd(planSums[4], item.getClass4PlanQty());
            planSums[5] = safeAdd(planSums[5], item.getClass5PlanQty());
            planSums[6] = safeAdd(planSums[6], item.getClass6PlanQty());
            planSums[7] = safeAdd(planSums[7], item.getClass7PlanQty());
            planSums[8] = safeAdd(planSums[8], item.getClass8PlanQty());

            finishSums[1] = safeAdd(finishSums[1], item.getClass1FinishQty());
            finishSums[2] = safeAdd(finishSums[2], item.getClass2FinishQty());
            finishSums[3] = safeAdd(finishSums[3], item.getClass3FinishQty());
            finishSums[4] = safeAdd(finishSums[4], item.getClass4FinishQty());
            finishSums[5] = safeAdd(finishSums[5], item.getClass5FinishQty());
            finishSums[6] = safeAdd(finishSums[6], item.getClass6FinishQty());
            finishSums[7] = safeAdd(finishSums[7], item.getClass7FinishQty());
            finishSums[8] = safeAdd(finishSums[8], item.getClass8FinishQty());
        }

        for (int i = 1; i <= 8; i++) {
            setCellVal(row.createCell(c++), planSums[i]);
            setCellVal(row.createCell(c++), finishSums[i]);
            c += 3; // skip analysis, recipeType, recipeNo
        }

        BigDecimal totalPlan = BigDecimal.ZERO;
        BigDecimal totalFinish = BigDecimal.ZERO;
        for (int i = 1; i <= 8; i++) {
            totalPlan = safeAdd(totalPlan, planSums[i]);
            totalFinish = safeAdd(totalFinish, finishSums[i]);
        }
        setCellVal(row.createCell(c++), totalPlan);   // C56
        setCellVal(row.createCell(c++), totalFinish);  // C57
        setCellVal(row.createCell(c++), totalPlan);    // C58
    }

    /**
     * 安全设置单元格值。
     */
    private void setCellVal(Cell cell, Object value) {
        if (value == null) return;
        if (value instanceof BigDecimal) {
            cell.setCellValue(((BigDecimal) value).doubleValue());
        } else if (value instanceof Number) {
            cell.setCellValue(((Number) value).doubleValue());
        } else if (value instanceof Date) {
            cell.setCellValue((Date) value);
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private BigDecimal sumPlan(CxScheduleResult item) {
        return safeAdd(safeAdd(safeAdd(safeAdd(safeAdd(safeAdd(safeAdd(
                item.getClass1PlanQty(), item.getClass2PlanQty()),
                item.getClass3PlanQty()), item.getClass4PlanQty()),
                item.getClass5PlanQty()), item.getClass6PlanQty()),
                item.getClass7PlanQty()), item.getClass8PlanQty());
    }

    private BigDecimal sumFinish(CxScheduleResult item) {
        return safeAdd(safeAdd(safeAdd(safeAdd(safeAdd(safeAdd(safeAdd(
                item.getClass1FinishQty(), item.getClass2FinishQty()),
                item.getClass3FinishQty()), item.getClass4FinishQty()),
                item.getClass5FinishQty()), item.getClass6FinishQty()),
                item.getClass7FinishQty()), item.getClass8FinishQty());
    }

    /**
     * 安全加法，null视为0。
     */
    private BigDecimal safeAdd(BigDecimal a, BigDecimal b) {
        if (a == null && b == null) return BigDecimal.ZERO;
        if (a == null) return b;
        if (b == null) return a;
        return a.add(b);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AjaxResult importScheduleTemplate(List<CxScheduleResultTemplateImportVO> list,
                                              CxScheduleResult result, boolean updateSupport, Long logId) {
        if (Objects.isNull(result) || Objects.isNull(result.getScheduleDate())) {
            return AjaxResult.error("导入条件中的排程日期不能为空");
        }
        if (Objects.isNull(list) || list.isEmpty()) {
            return AjaxResult.error("导入文件未读取到有效明细行");
        }

        Date scheduleDate = cn.hutool.core.date.DateUtil.beginOfDay(result.getScheduleDate());
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();
        int successNum = 0;
        int failureNum = 0;

        for (int i = 0; i < list.size(); i++) {
            int rowNum = i + 2;
            CxScheduleResultTemplateImportVO row = list.get(i);
            if (Objects.isNull(row)) {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(logId, rowNum,
                        "第" + rowNum + "行数据为空", importErrorLogs);
                continue;
            }
            row.setScheduleDate(scheduleDate);
            List<ImportErrorLog> validated = ImportExcelValidatedUtils.validated(logId, rowNum, row);
            ImportExcelValidatedUtils.validatedRepeat(list, row, i, 2, logId, validated,
                    "cxMachineCode", "materialCode");
            if (PubUtil.isNotEmpty(validated)) {
                failureNum++;
                row.setId(-999L);
                importErrorLogs.addAll(validated);
            }
        }

        List<String> machineCodes = list.stream()
                .filter(Objects::nonNull)
                .filter(item -> !Objects.equals(item.getId(), -999L))
                .map(CxScheduleResultTemplateImportVO::getCxMachineCode)
                .filter(StringUtils::isNotBlank).map(String::trim).distinct()
                .collect(Collectors.toList());
        List<String> materialCodes = list.stream()
                .filter(Objects::nonNull)
                .filter(item -> !Objects.equals(item.getId(), -999L))
                .map(CxScheduleResultTemplateImportVO::getMaterialCode)
                .filter(StringUtils::isNotBlank).map(String::trim).distinct()
                .collect(Collectors.toList());

        Map<String, CxScheduleResult> existMap = new LinkedHashMap<>();
        if (!machineCodes.isEmpty() && !materialCodes.isEmpty()) {
            List<CxScheduleResult> exists = cxScheduleResultMapper.selectList(
                    new LambdaQueryWrapper<CxScheduleResult>()
                            .eq(CxScheduleResult::getScheduleDate, scheduleDate)
                            .in(CxScheduleResult::getCxMachineCode, machineCodes)
                            .in(CxScheduleResult::getMaterialCode, materialCodes));
            existMap = exists.stream().collect(Collectors.toMap(
                    this::buildImportUniqueKey,
                    item -> item,
                    (oldValue, newValue) -> oldValue,
                    LinkedHashMap::new));
        }

        Set<String> importUniqueKeys = new HashSet<>();

        for (int i = 0; i < list.size(); i++) {
            CxScheduleResultTemplateImportVO row = list.get(i);
            int rowNum = i + 2;
            if (Objects.isNull(row) || Objects.equals(row.getId(), -999L)) {
                continue;
            }

            if (StringUtils.isBlank(row.getCxMachineCode())) {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(logId, ImportErrorTypeEnums.OTHERS.getCode(),
                        rowNum, "第" + rowNum + "行 成型机台编号不能为空", importErrorLogs);
                continue;
            }
            if (StringUtils.isBlank(row.getMaterialCode())) {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(logId, ImportErrorTypeEnums.OTHERS.getCode(),
                        rowNum, "第" + rowNum + "行 物料编号不能为空", importErrorLogs);
                continue;
            }

            String uniqueKey = scheduleDate.getTime() + "|"
                    + row.getCxMachineCode().trim() + "|"
                    + row.getMaterialCode().trim();
            if (!importUniqueKeys.add(uniqueKey)) {
                failureNum++;
                ImportExcelValidatedUtils.addImportErrorLog(logId, ImportErrorTypeEnums.OTHERS.getCode(),
                        rowNum, "第" + rowNum + "行 机台+物料在导入文件中重复", importErrorLogs);
                continue;
            }

            String dbUniqueKey = buildImportUniqueKey(row.getCxMachineCode(), scheduleDate, row.getMaterialCode());
            CxScheduleResult target = existMap.get(dbUniqueKey);
            boolean isInsert = Objects.isNull(target);

            if (isInsert) {
                target = new CxScheduleResult();
                target.setDataSource("2");
                target.setIsRelease("0");
                target.setProductionStatus("0");
            }
            target.setScheduleDate(scheduleDate);
            target.setCxMachineCode(row.getCxMachineCode().trim());
            target.setCxMachineName(row.getCxMachineName());
            target.setEmbryoCode(row.getEmbryoCode());
            target.setMaterialCode(row.getMaterialCode().trim());
            target.setMaterialDesc(row.getMaterialDesc());
            target.setMainMaterialDesc(row.getMainMaterialDesc());
            target.setStructureName(row.getStructureName());
            target.setBomDataVersion(row.getBomDataVersion());
            target.setOrderNo(row.getOrderNo());
            target.setCxBatchNo(row.getCxBatchNo());
            target.setIsRelease(row.getIsRelease());
            target.setDataSource(row.getDataSource());

            target.setClass1PlanQty(row.getClass1PlanQty());
            target.setClass1FinishQty(row.getClass1FinishQty());
            target.setClass1Analysis(row.getClass1Analysis());
            target.setClass1RecipeType(row.getClass1RecipeType());
            target.setClass1RecipeNo(row.getClass1RecipeNo());

            target.setClass2PlanQty(row.getClass2PlanQty());
            target.setClass2FinishQty(row.getClass2FinishQty());
            target.setClass2Analysis(row.getClass2Analysis());
            target.setClass2RecipeType(row.getClass2RecipeType());
            target.setClass2RecipeNo(row.getClass2RecipeNo());

            target.setClass3PlanQty(row.getClass3PlanQty());
            target.setClass3FinishQty(row.getClass3FinishQty());
            target.setClass3Analysis(row.getClass3Analysis());
            target.setClass3RecipeType(row.getClass3RecipeType());
            target.setClass3RecipeNo(row.getClass3RecipeNo());

            target.setClass4PlanQty(row.getClass4PlanQty());
            target.setClass4FinishQty(row.getClass4FinishQty());
            target.setClass4Analysis(row.getClass4Analysis());
            target.setClass4RecipeType(row.getClass4RecipeType());

            target.setClass5PlanQty(row.getClass5PlanQty());
            target.setClass5FinishQty(row.getClass5FinishQty());
            target.setClass5Analysis(row.getClass5Analysis());
            target.setClass5RecipeType(row.getClass5RecipeType());

            target.setClass6PlanQty(row.getClass6PlanQty());
            target.setClass6FinishQty(row.getClass6FinishQty());
            target.setClass6Analysis(row.getClass6Analysis());
            target.setClass6RecipeType(row.getClass6RecipeType());

            target.setClass7PlanQty(row.getClass7PlanQty());
            target.setClass7FinishQty(row.getClass7FinishQty());
            target.setClass7Analysis(row.getClass7Analysis());
            target.setClass7RecipeType(row.getClass7RecipeType());

            target.setClass8PlanQty(row.getClass8PlanQty());
            target.setClass8FinishQty(row.getClass8FinishQty());
            target.setClass8Analysis(row.getClass8Analysis());
            target.setClass8RecipeType(row.getClass8RecipeType());

            target.setTotalStock(row.getTotalStock());
            target.setLhMachineCode(row.getLhMachineCode());
            target.setCxRemainQty(row.getCxRemainQty());
            target.setLhRemainQty(row.getLhRemainQty());
            target.setLhClassQty(row.getLhClassQty());

            if (isInsert) {
                cxScheduleResultMapper.insert(target);
                existMap.put(dbUniqueKey, target);
            } else {
                cxScheduleResultMapper.updateById(target);
            }
            successNum++;
        }

        if (failureNum > 0) {
            return AjaxResult.error(
                    I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum,
                    importErrorLogs);
        }
        return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
    }

    private String buildImportUniqueKey(CxScheduleResult entity) {
        return buildImportUniqueKey(entity.getCxMachineCode(), entity.getScheduleDate(), entity.getMaterialCode());
    }

    private String buildImportUniqueKey(String cxMachineCode, Date scheduleDate, String materialCode) {
        return StringUtils.defaultString(cxMachineCode).trim() + "|"
                + cn.hutool.core.date.DateUtil.format(cn.hutool.core.date.DateUtil.beginOfDay(scheduleDate), "yyyy-MM-dd") + "|"
                + StringUtils.defaultString(materialCode).trim();
    }

    /**
     * 构建成型余量导出查询条件。
     *
     * @param queryVO 查询条件，来源于UI导出请求
     * @return 成型排程结果Lambda查询条件
     */
    private LambdaQueryWrapper<CxScheduleResult> buildCxRemainQtyQueryWrapper(CxScheduleResult queryVO) {
        CxScheduleResult query = Objects.isNull(queryVO) ? new CxScheduleResult() : queryVO;
        return new LambdaQueryWrapper<CxScheduleResult>()
                .eq(PubUtil.isNotEmpty(query.getScheduleDate()), CxScheduleResult::getScheduleDate, query.getScheduleDate())
                .like(PubUtil.isNotEmpty(query.getCxMachineCode()), CxScheduleResult::getCxMachineCode, query.getCxMachineCode())
                .like(PubUtil.isNotEmpty(query.getMaterialCode()), CxScheduleResult::getMaterialCode, query.getMaterialCode())
                .like(PubUtil.isNotEmpty(query.getMaterialDesc()), CxScheduleResult::getMaterialDesc, query.getMaterialDesc())
                .like(PubUtil.isNotEmpty(query.getMainMaterialDesc()), CxScheduleResult::getMainMaterialDesc, query.getMainMaterialDesc())
                .eq(PubUtil.isNotEmpty(query.getOrderNo()), CxScheduleResult::getOrderNo, query.getOrderNo())
                .eq(PubUtil.isNotEmpty(query.getProductionStatus()), CxScheduleResult::getProductionStatus, query.getProductionStatus())
                .eq(PubUtil.isNotEmpty(query.getIsRelease()), CxScheduleResult::getIsRelease, query.getIsRelease())
                .orderByAsc(CxScheduleResult::getCxMachineCode)
                .orderByAsc(CxScheduleResult::getMaterialCode);
    }

    /**
     * 构建成型余量模板列表数据。
     *
     * @param list 成型排程结果明细列表
     * @return 模板列表行数据，字段名与cxyl.xlsx中的列表占位符保持一致
     */
    private List<Map<String, Object>> buildCxRemainQtyExportDataList(List<CxScheduleResult> list) {
        List<CxScheduleResult> exportList = Objects.isNull(list) ? Collections.emptyList() : list;
        Map<String, List<CxScheduleResult>> groupMap = exportList.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.groupingBy(this::buildCxRemainQtyGroupKey, LinkedHashMap::new, Collectors.toList()));

        List<Map<String, Object>> dataList = new ArrayList<>();
        for (List<CxScheduleResult> groupList : groupMap.values()) {
            if (CollectionUtils.isEmpty(groupList)) {
                continue;
            }
            CxScheduleResult first = groupList.get(0);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("cxMachineCode", first.getCxMachineCode());
            row.put("materialCode", first.getMaterialCode());
            row.put("mainMaterialDesc", firstNonBlank(groupList, "mainMaterialDesc"));
            // 小胶种暂未明确来源，按需求先导出空值，避免误用其他业务字段。
            row.put("smallGlue", "");
            row.put("cxRemainQty", sumCxRemainQty(groupList));
            row.put("remark", buildCxRemainQtyRemark(groupList));
            dataList.add(row);
        }
        return dataList;
    }

    /**
     * 构建成型余量导出分组键。
     *
     * @param item 成型排程结果明细
     * @return 机台编号和物料编码组成的唯一分组键
     */
    private String buildCxRemainQtyGroupKey(CxScheduleResult item) {
        return StringUtils.defaultString(item.getCxMachineCode()).trim() + "|"
                + StringUtils.defaultString(item.getMaterialCode()).trim();
    }

    /**
     * 获取分组内第一个非空文本字段。
     *
     * @param list 分组明细列表
     * @param fieldName 字段名称，目前用于胎胚描述取值
     * @return 第一个非空字段值，没有则返回空字符串
     */
    private String firstNonBlank(List<CxScheduleResult> list, String fieldName) {
        for (CxScheduleResult item : list) {
            if ("mainMaterialDesc".equals(fieldName) && StringUtils.isNotBlank(item.getMainMaterialDesc())) {
                return item.getMainMaterialDesc();
            }
        }
        return "";
    }

    /**
     * 合计分组内成型余量。
     *
     * @param list 分组明细列表
     * @return 成型余量合计，空值按0处理
     */
    private BigDecimal sumCxRemainQty(List<CxScheduleResult> list) {
        return list.stream()
                .map(CxScheduleResult::getCxRemainQty)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 构建分组备注。
     *
     * @param list 分组明细列表
     * @return 去重后的备注文本，多个备注使用中文分号拼接
     */
    private String buildCxRemainQtyRemark(List<CxScheduleResult> list) {
        return list.stream()
                .map(CxScheduleResult::getRemark)
                .filter(StringUtils::isNotBlank)
                .map(String::trim)
                .distinct()
                .collect(Collectors.joining("；"));
    }

    /**
     * 导出成型结构切换数据。
     * 主数据源改为T_MP_STRUCTURE_ALLOCATION（通过Feign获取），
     * 只展示有2条以上结构记录的机台（说明有结构切换），
     * 计算收尾预计时间和开产预计时间。
     *
     * @param queryVO 查询条件，按成型排程结果列表查询口径筛选数据
     * @param fileName 导出文件名，保留用于对齐远程调用契约
     * @return 成型结构切换Excel文件字节数组
     */
    @Override
    public byte[] exportStructureChange(CxScheduleResult queryVO, String fileName) {
        InputStream inputStream = this.getClass().getClassLoader()
                .getResourceAsStream("excelModel/cxStructureChangeExportTemp.xlsx");
        if (Objects.isNull(inputStream)) {
            throw new ServiceException("成型结构切换导出模板不存在");
        }

        MpStructureAllocation structureQuery = buildStructureAllocationQuery(queryVO);
        TableDataInfo structureDataInfo = mpStructureAllocationRemoteService.list(structureQuery);
        List<MpStructureAllocation> structureList = structureDataInfo != null
                ? convertToMpStructureAllocationList(structureDataInfo.getRows())
                : Collections.emptyList();

        if (CollectionUtils.isEmpty(structureList)) {
            List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
            excelDataList.add(new ArrayList<>());
            return ExcelUtils.writeMultiList(inputStream, 0, new HashMap<>(), excelDataList);
        }

        Map<String, List<MpStructureAllocation>> machineGroupMap = structureList.stream()
                .filter(Objects::nonNull)
                .filter(s -> StringUtils.isNotBlank(s.getCxMachineCode()))
                .collect(Collectors.groupingBy(
                        s -> s.getCxMachineCode().trim(),
                        LinkedHashMap::new,
                        Collectors.toList()));
        machineGroupMap.entrySet().removeIf(entry -> entry.getValue().size() < 2);

        List<CxScheduleResult> scheduleResults = cxScheduleResultMapper.selectList(
                buildStructureChangeQueryWrapper(queryVO));
        // 按物料编码+胎胚代码分组，取成型余量最大值（同结构下不同机台可能存在共用数据）
        Map<String, BigDecimal> remainQtyMap = scheduleResults.stream()
                .filter(r -> StringUtils.isNotBlank(r.getMaterialCode()) && StringUtils.isNotBlank(r.getEmbryoCode()))
                .filter(r -> r.getCxRemainQty() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getMaterialCode().trim() + "|" + r.getEmbryoCode().trim(),
                        Collectors.reducing(BigDecimal.ZERO, CxScheduleResult::getCxRemainQty, BigDecimal::max)));

        // 构建结构名到物料编码+胎胚代码的映射，用于通过结构名查找成型余量
        Map<String, String> structureToRemainKeyMap = scheduleResults.stream()
                .filter(r -> StringUtils.isNotBlank(r.getStructureName()))
                .filter(r -> StringUtils.isNotBlank(r.getMaterialCode()) && StringUtils.isNotBlank(r.getEmbryoCode()))
                .collect(Collectors.toMap(
                        CxScheduleResult::getStructureName,
                        r -> r.getMaterialCode().trim() + "|" + r.getEmbryoCode().trim(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new));

        LocalDate scheduleDate = queryVO != null && queryVO.getScheduleDate() != null
                ? cn.hutool.core.date.DateUtil.toLocalDateTime(queryVO.getScheduleDate()).toLocalDate()
                : LocalDate.now();

        List<Map<String, Object>> dataList = buildStructureChangeDataListV2(
                machineGroupMap, remainQtyMap, structureToRemainKeyMap, scheduleDate);

        Map<String, Object> tableMap = new HashMap<>();
        List<CellStyle> cellStyleList = buildCellStyleListForStructureChange(dataList);
        if (PubUtil.isNotEmpty(cellStyleList)) {
            tableMap.put("CELL_STYLE", cellStyleList);
        }

        List<List<Map<String, Object>>> excelDataList = new ArrayList<>();
        excelDataList.add(dataList);
        return ExcelUtils.writeMultiList(inputStream, 0, tableMap, excelDataList);
    }

    /**
     * 将Feign远程调用返回的LinkedHashMap列表转换为MpStructureAllocation实体列表。
     * Feign反序列化泛型丢失，TableDataInfo.getRows()中的元素实际类型为LinkedHashMap，
     * 直接强转会导致ClassCastException，需使用ObjectMapper.convertValue进行类型转换。
     *
     * @param rows Feign远程调用返回的行数据列表
     * @return MpStructureAllocation实体列表
     */
    private List<MpStructureAllocation> convertToMpStructureAllocationList(List<?> rows) {
        List<MpStructureAllocation> entityList = new ArrayList<>();
        if (PubUtil.isEmpty(rows)) {
            return entityList;
        }
        com.fasterxml.jackson.databind.ObjectMapper objectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
        for (Object obj : rows) {
            if (obj instanceof MpStructureAllocation) {
                entityList.add((MpStructureAllocation) obj);
            } else if (obj instanceof Map) {
                MpStructureAllocation entity = objectMapper.convertValue(obj, MpStructureAllocation.class);
                entityList.add(entity);
            }
        }
        return entityList;
    }

    /**
     * 构建结构排产查询条件，从排程结果查询VO转换为结构排产查询对象。
     *
     * @param queryVO 排程结果查询条件
     * @return 结构排产查询对象
     */
    private MpStructureAllocation buildStructureAllocationQuery(CxScheduleResult queryVO) {
        MpStructureAllocation structureQuery = new MpStructureAllocation();
        // 分厂为空时赋默认工厂编码
        String factoryCode = (queryVO != null && StringUtils.isNotBlank(queryVO.getFactoryCode()))
                ? queryVO.getFactoryCode() : FactoryConstant.DEFAULT_FACTORY_CODE;
        structureQuery.setFactoryCode(factoryCode);
        if (queryVO != null) {
            structureQuery.setCxMachineCode(queryVO.getCxMachineCode());
        }
        // 年月参数从排程日期的年月拆出，排程日期为空时默认当前日期
        LocalDate ld = (queryVO != null && queryVO.getScheduleDate() != null)
                ? DateUtil.toLocalDateTime(queryVO.getScheduleDate()).toLocalDate()
                : LocalDate.now();
        structureQuery.setYear(ld.getYear());
        structureQuery.setMonth(ld.getMonthValue());
        return structureQuery;
    }

    /**
     * 构建成型结构切换导出查询条件（用于查询排程结果余量数据）。
     *
     * @param queryVO 查询条件，来源于UI导出请求
     * @return 成型排程结果Lambda查询条件
     */
    private LambdaQueryWrapper<CxScheduleResult> buildStructureChangeQueryWrapper(CxScheduleResult queryVO) {
        CxScheduleResult query = Objects.isNull(queryVO) ? new CxScheduleResult() : queryVO;
        return new LambdaQueryWrapper<CxScheduleResult>()
                .eq(PubUtil.isNotEmpty(query.getScheduleDate()), CxScheduleResult::getScheduleDate, query.getScheduleDate())
                .like(PubUtil.isNotEmpty(query.getCxMachineCode()), CxScheduleResult::getCxMachineCode, query.getCxMachineCode())
                .like(PubUtil.isNotEmpty(query.getMaterialCode()), CxScheduleResult::getMaterialCode, query.getMaterialCode())
                .like(PubUtil.isNotEmpty(query.getMaterialDesc()), CxScheduleResult::getMaterialDesc, query.getMaterialDesc())
                .like(PubUtil.isNotEmpty(query.getMainMaterialDesc()), CxScheduleResult::getMainMaterialDesc, query.getMainMaterialDesc())
                .eq(PubUtil.isNotEmpty(query.getOrderNo()), CxScheduleResult::getOrderNo, query.getOrderNo())
                .eq(PubUtil.isNotEmpty(query.getProductionStatus()), CxScheduleResult::getProductionStatus, query.getProductionStatus())
                .eq(PubUtil.isNotEmpty(query.getIsRelease()), CxScheduleResult::getIsRelease, query.getIsRelease())
                .orderByAsc(CxScheduleResult::getCxMachineCode)
                .orderByAsc(CxScheduleResult::getScheduleDate)
                .orderByAsc(CxScheduleResult::getMaterialCode);
    }

    /**
     * 构建成型结构切换模板列表数据（V2版本，基于T_MP_STRUCTURE_ALLOCATION）。
     * 按成型机台分组，每个机台按beginDay排序，
     * 相邻结构之间生成一条切换记录。
     *
     * 班产和收尾预计时间由成型排程同事提供接口获取（TODO），
     * 当前仅填充结构和成型余量，收尾/开产预计时间字段暂输出空串。
     *
     * @param machineGroupMap 按机台分组的结构排产数据
     * @param remainQtyMap 余量映射（key: materialCode|embryoCode）
     * @param structureToRemainKeyMap 结构名到余量映射key的映射（key: structureName, value: materialCode|embryoCode）
     * @param scheduleDate 排程日期
     * @return 模板列表行数据
     */
    private List<Map<String, Object>> buildStructureChangeDataListV2(
            Map<String, List<MpStructureAllocation>> machineGroupMap,
            Map<String, BigDecimal> remainQtyMap,
            Map<String, String> structureToRemainKeyMap,
            LocalDate scheduleDate) {

        List<Map<String, Object>> dataList = new ArrayList<>();

        for (Map.Entry<String, List<MpStructureAllocation>> entry : machineGroupMap.entrySet()) {
            String machineCode = entry.getKey();
            List<MpStructureAllocation> structures = entry.getValue().stream()
                    .sorted(Comparator.comparing(MpStructureAllocation::getBeginDay, Comparator.nullsLast(Comparator.naturalOrder())))
                    .collect(Collectors.toList());

            for (int i = 0; i < structures.size() - 1; i++) {
                MpStructureAllocation prevStructure = structures.get(i);
                MpStructureAllocation nextStructure = structures.get(i + 1);

                Map<String, Object> row = buildStructureChangeRow(
                        machineCode, prevStructure, nextStructure,
                        remainQtyMap, structureToRemainKeyMap, scheduleDate);
                dataList.add(row);
            }
        }

        dataList.sort(Comparator.comparing(
                row -> (String) row.get("_sortKey"),
                Comparator.nullsLast(Comparator.naturalOrder())));

        int rowIndex = 1;
        for (Map<String, Object> row : dataList) {
            row.put("stt", rowIndex++);
        }

        return dataList;
    }

    /**
     * 构建单条结构切换导出行数据。
     *
     * 成型余量通过结构名关联到物料编码+胎胚代码，再从remainQtyMap取最大值。
     * 班产和收尾/开产预计时间由成型排程同事提供接口获取（TODO），
     * 当前receiveChangeDate和vulcanizeChangeDate暂输出空串。
     *
     * @param machineCode 成型机台编码
     * @param prevStructure 前结构（当前正在执行的结构）
     * @param nextStructure 后结构（即将切换到的结构）
     * @param remainQtyMap 余量映射（key: materialCode|embryoCode）
     * @param structureToRemainKeyMap 结构名到余量映射key的映射
     * @param scheduleDate 排程日期
     * @return 单行导出数据
     */
    private Map<String, Object> buildStructureChangeRow(
            String machineCode,
            MpStructureAllocation prevStructure,
            MpStructureAllocation nextStructure,
            Map<String, BigDecimal> remainQtyMap,
            Map<String, String> structureToRemainKeyMap,
            LocalDate scheduleDate) {

        String prevStructureName = StringUtils.defaultString(prevStructure.getStructureName()).trim();
        String nextStructureName = StringUtils.defaultString(nextStructure.getStructureName()).trim();
        String alternatingType = StringUtils.defaultString(nextStructure.getAlternatingType()).trim();
        boolean isInchChange = AlternativeTypeEnum.PRO_SIZE_ALTERNATIVE.getCode().equals(alternatingType);

        // 通过结构名查找对应的物料编码+胎胚代码key，再从remainQtyMap取成型余量
        String remainKey = structureToRemainKeyMap.getOrDefault(prevStructureName, "");
        BigDecimal remainQty = StringUtils.isNotBlank(remainKey)
                ? remainQtyMap.getOrDefault(remainKey, BigDecimal.ZERO)
                : BigDecimal.ZERO;
        if (remainQty.compareTo(BigDecimal.ZERO) == 0 && prevStructure.getNetQty() != null) {
            remainQty = new BigDecimal(prevStructure.getNetQty());
        }

        int year = prevStructure.getYear() != null ? prevStructure.getYear() : scheduleDate.getYear();
        int month = prevStructure.getMonth() != null ? prevStructure.getMonth() : scheduleDate.getMonthValue();

        LocalDate nextBeginDate = nextStructure.getBeginDay() != null
                ? LocalDate.of(year, month, Math.min(nextStructure.getBeginDay(), LocalDate.of(year, month, 1).lengthOfMonth()))
                : scheduleDate;

        // TODO: 班产和收尾预计时间由成型排程提供接口获取，传入结构和成型余量，
        //  当前收尾预计时间和开产预计时间暂输出空串，待接口对接后替换
        String estimatedEndTime = "";
        String estimatedStartTime = "";

        String remark = isInchChange ? "换英寸" : "换结构";

        Map<String, Object> row = new LinkedHashMap<>();
        row.put("stt", 0);
        row.put("cxMachineCode", machineCode);
        row.put("materialSpec", prevStructureName + "→" + nextStructureName);
        row.put("qty", remainQty.intValue());
        row.put("receivePlanDate", formatDateFromDay(year, month, prevStructure.getEndDay()));
        row.put("receiveChangeDate", estimatedEndTime);
        row.put("remark", remark);
        row.put("orderNo", "");
        row.put("vulcanizePlanDate", formatDateFromDay(year, month, nextStructure.getBeginDay()));
        row.put("vulcanizeChangeDate", estimatedStartTime);
        row.put("remark2", nextStructure.getRemark() != null ? nextStructure.getRemark() : "");
        row.put("traceTd", "");
        row.put("traceSw", "");
        row.put("traceIl", "");
        row.put("traceUb", "");
        row.put("traceBd", "");
        row.put("traceCa", "");
        row.put("traceBe", "");
        row.put("traceCh", "");

        String sortKey = String.format("%04d-%02d-%02d", year, month,
                nextStructure.getBeginDay() != null ? nextStructure.getBeginDay() : 99);
        row.put("_sortKey", sortKey);
        row.put("_nextBeginDate", nextBeginDate);

        return row;
    }

    /**
     * 根据年月和日数字格式化日期为"MM.DD"格式。
     *
     * @param year 年份
     * @param month 月份
     * @param day 日（1-31）
     * @return 格式化字符串
     */
    private String formatDateFromDay(int year, int month, Integer day) {
        if (day == null) {
            return "";
        }
        return String.format("%02d.%02d", month, day);
    }

    /**
     * 构建成型结构切换导出的单元格样式列表（底色间隔区分）。
     * 规则：根据开产时间(月计划)分组，同一天有2条以上记录的标红，
     * 连续多日都有2条以上时按白、红、白、红交替，
     * 一天只有1条的默认白色。
     *
     * @param dataList 导出数据列表
     * @return 单元格样式列表
     */
    private List<CellStyle> buildCellStyleListForStructureChange(List<Map<String, Object>> dataList) {
        List<CellStyle> cellStyleList = new ArrayList<>();
        if (CollectionUtils.isEmpty(dataList)) {
            return cellStyleList;
        }

        Map<String, List<Integer>> dateGroupMap = new LinkedHashMap<>();
        for (int i = 0; i < dataList.size(); i++) {
            Object nextBeginDate = dataList.get(i).get("_nextBeginDate");
            String dateKey;
            if (nextBeginDate instanceof LocalDate) {
                dateKey = ((LocalDate) nextBeginDate).toString();
            } else {
                dateKey = String.valueOf(dataList.get(i).get("vulcanizePlanDate"));
            }
            dateGroupMap.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(i);
        }

        boolean colorToggle = false;
        String redColor = "#FFC7CE";
        for (Map.Entry<String, List<Integer>> dateEntry : dateGroupMap.entrySet()) {
            List<Integer> rowIndexes = dateEntry.getValue();
            if (rowIndexes.size() >= 2) {
                colorToggle = !colorToggle;
                if (colorToggle) {
                    for (Integer rowIdx : rowIndexes) {
                        cellStyleList.add(new CellStyle(
                                rowIdx + 1, rowIdx + 1, 0, 11,
                                redColor, true));
                    }
                }
            } else {
                colorToggle = false;
            }
        }

        return cellStyleList;
    }

    /**
     * 格式化班次开始时间，用于导出显示。
     *
     * @param date 班次开始时间
     * @return 格式化后的时间字符串，空值返回空串
     */
    private String formatStartTime(Date date) {
        return date != null ? cn.hutool.core.date.DateUtil.format(date, "yyyy-MM-dd HH:mm") : "";
    }

    private Map<String, Object> buildExportTableMap(List<CxScheduleResult> list, Date scheduleDate) {
        Map<String, Object> tableMap = new LinkedHashMap<>();
        tableMap.put("scheduleDate", scheduleDate != null
                ? cn.hutool.core.date.DateUtil.format(scheduleDate, "yyyy-MM-dd") : "");
        tableMap.put("totalCount", list != null ? list.size() : 0);
        return tableMap;
    }

    private List<Map<String, Object>> buildExportDataList(List<CxScheduleResult> list) {
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            CxScheduleResult item = list.get(i);
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("index", i + 1);
            row.put("cxMachineCode", item.getCxMachineCode());
            row.put("cxMachineName", item.getCxMachineName());
            row.put("embryoCode", item.getEmbryoCode());
            row.put("materialCode", item.getMaterialCode());
            row.put("materialDesc", item.getMaterialDesc());
            row.put("mainMaterialDesc", item.getMainMaterialDesc());
            row.put("structureName", item.getStructureName());
            row.put("bomDataVersion", item.getBomDataVersion());
            row.put("orderNo", item.getOrderNo());
            row.put("cxBatchNo", item.getCxBatchNo());
            row.put("scheduleDate", item.getScheduleDate());

            row.put("class1PlanQty", item.getClass1PlanQty());
            row.put("class1FinishQty", item.getClass1FinishQty());
            row.put("class1Analysis", item.getClass1Analysis());
            row.put("class2PlanQty", item.getClass2PlanQty());
            row.put("class2FinishQty", item.getClass2FinishQty());
            row.put("class2Analysis", item.getClass2Analysis());
            row.put("class3PlanQty", item.getClass3PlanQty());
            row.put("class3FinishQty", item.getClass3FinishQty());
            row.put("class3Analysis", item.getClass3Analysis());
            row.put("class4PlanQty", item.getClass4PlanQty());
            row.put("class5PlanQty", item.getClass5PlanQty());
            row.put("class6PlanQty", item.getClass6PlanQty());
            row.put("class7PlanQty", item.getClass7PlanQty());
            row.put("class8PlanQty", item.getClass8PlanQty());

            row.put("totalStock", item.getTotalStock());
            row.put("lhMachineCode", item.getLhMachineCode());
            row.put("cxRemainQty", item.getCxRemainQty());
            row.put("lhRemainQty", item.getLhRemainQty());
            row.put("lhClassQty", item.getLhClassQty());

            dataList.add(row);
        }
        return dataList;
    }
}
