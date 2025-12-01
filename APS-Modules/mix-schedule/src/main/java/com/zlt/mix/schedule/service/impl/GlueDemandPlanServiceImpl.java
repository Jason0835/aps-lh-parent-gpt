package com.zlt.mix.schedule.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.DateUtil;
import com.zlt.mix.common.core.utils.ExcelUtils;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.zlt.mix.common.engine.constants.EngineConstants;
import com.zlt.mix.common.engine.service.impl.IncrementService;
import com.zlt.mix.common.engine.utils.BeanConverUtil;
import com.zlt.mix.schedule.api.domain.dto.GlueDemandPlanExportDictDto;
import com.zlt.mix.schedule.api.domain.entity.GlueDemandPlan;
import com.zlt.mix.schedule.api.domain.entity.GlueDemandPlanInit;
import com.zlt.mix.schedule.mapper.GlueDemandPlanInitMapper;
import com.zlt.mix.schedule.mapper.GlueDemandPlanMapper;
import com.zlt.mix.schedule.service.GlueDemandPlanService;
import com.zlt.mix.setting.api.domain.entity.FactoryGlueAreaRelation;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 分厂胶料需求计划Service业务层处理
 *
 * @author chen
 * @date 2022-04-18
 */
@Service
public class GlueDemandPlanServiceImpl extends ServiceImpl<GlueDemandPlanMapper, GlueDemandPlan> implements GlueDemandPlanService {
    @Resource
    private GlueDemandPlanMapper glueDemandPlanMapper;

    @Resource
    private GlueDemandPlanInitMapper glueDemandPlanInitMapper;

    @Resource
    private IncrementService incrementService;

    @Value("${excelModelPath}")
    public String excelModelPath;

    /**
     * 查询分厂胶料需求计划列表
     *
     * @param glueDemandPlan 分厂胶料需求计划
     * @return 分厂胶料需求计划
     */
    @Override
    public List<GlueDemandPlan> selectGlueDemandPlanList(GlueDemandPlan glueDemandPlan) {
        return glueDemandPlanMapper.selectGlueDemandPlanList(glueDemandPlan);
    }

    /**
     * 保存分厂胶料需求计划信息（id为空则新增，id不为空则修改）
     *
     * @param glueDemandPlan
     */
    @Override
    public void saveGlueDemandPlan(GlueDemandPlan glueDemandPlan) {
        if (ZltConstant.NOT_UNIQUE.equals(checkGlueDemandPlanUnique(glueDemandPlan))) {
            throw new RuntimeException(I18nUtil.getMessage("schedule.glueDemandPlan.database.unique"));
        }
        glueDemandPlan.setBaseValue(glueDemandPlan.getId());
        /*// 数据源不为拆分，才汇总计算各班计划量
        if (!ZltConstant.DEMAND_SOURCE_SPLIT.equals(glueDemandPlan.getDataSource())) {
            setTotalPlanQty(glueDemandPlan);
        }*/
        if (StringUtils.isBlank(glueDemandPlan.getDataSource())) {
            glueDemandPlan.setDataSource(ZltConstant.DEMAND_SOURCE_ADD);
        }
        this.saveOrUpdate(glueDemandPlan);
    }

    /**
     * 批量删除分厂胶料需求计划
     *
     * @param ids 需要删除的分厂胶料需求计划ID
     * @return 结果
     */
    @Override
    public int deleteGlueDemandPlanByIds(Long[] ids) {
        return glueDemandPlanMapper.deleteGlueDemandPlanByIds(ids);
    }


    /**
     * 校验分厂胶料需求计划唯一性
     */
    @Override
    public String checkGlueDemandPlanUnique(GlueDemandPlan glueDemandPlan) {
        if (glueDemandPlan == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        LambdaQueryWrapper<GlueDemandPlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GlueDemandPlan::getDelFlag, ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq(GlueDemandPlan::getPlanDate, glueDemandPlan.getPlanDate());
        queryWrapper.eq(GlueDemandPlan::getFactory, glueDemandPlan.getFactory());
        queryWrapper.eq(GlueDemandPlan::getGlue, glueDemandPlan.getGlue());
        queryWrapper.eq(GlueDemandPlan::getMixArea, glueDemandPlan.getMixArea());
        if (glueDemandPlan.getId() != null) {
            queryWrapper.ne(GlueDemandPlan::getId, glueDemandPlan.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<GlueDemandPlan> list = glueDemandPlanMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 导入分厂胶料需求计划数据
     *
     * @param list        要导入的数据集合
     * @param importLogId 导入日志id
     * @param isSkip      是否跳过计划量为0的
     */
    @Override
    public AjaxResult importData(List<GlueDemandPlanInit> list, Long importLogId, Boolean isSkip) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        int skipNum = 0;
        List<GlueDemandPlanInit> importList = new ArrayList<>();   //各种校验通过后的导入数据列表（最终可以导入数据库的计划）
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();  //导入错误明显列表
        //获取终炼胶名的Set集合
        Set<String> materialNameSet = glueDemandPlanMapper.listMaterialNameSet();
        Date planDate = list.get(0).getPlanDate();
        String factory = list.get(0).getFactory();
        String batchNo = incrementService.getSequence3(EngineConstants.DEMAND_PREFIX + factory + DateUtil.formatDateYmd(planDate));  //创建批次号

        // 取出取出分厂对应的胶料密炼区对应关系
		Map<String, String> glueMixAreaMap = glueDemandPlanMapper.listFactoryGlueAreaRelation(factory).stream()
				.collect(Collectors.toMap(FactoryGlueAreaRelation::getGlue, FactoryGlueAreaRelation::getMixArea,
						(v1, v2) -> v1));
        
        try {
            //按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getPlanDate() + a.getFactory() + a.getGlue(), Collectors.counting()));

            List<GlueDemandPlan> convertList = BeanConverUtil.converList(list, GlueDemandPlan.class);
            //公共校验（非空校验、长度校验等）
            for (int i = 0; i < convertList.size(); i++) {
                GlueDemandPlan glueDemandPlan = convertList.get(i);
                GlueDemandPlanInit glueDemandPlanInit = list.get(i);
                
                // 如果勾选了跳过，且计划为0的记录，直接跳过不处理
                if (isSkip && Optional.ofNullable(glueDemandPlanInit.getTotalPlanQty()).orElse(BigDecimal.ZERO).compareTo(BigDecimal.ZERO) == 0) {
                	skipNum ++;
                	continue;
                }
                
                //excel中重复记录校验
                Long hasValue = groupMap.get(glueDemandPlan.getPlanDate() + glueDemandPlan.getFactory() + glueDemandPlan.getGlue());
                if (hasValue > 1) {
                    //导入的excel中的数据违反了唯一键约束
                    glueDemandPlan.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    glueDemandPlanInit.setId(-999L);
                    String message = I18nUtil.getMessage("schedule.glueDemandPlan.excel.unique");
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                if (StringUtils.isEmpty(glueMixAreaMap.get(glueDemandPlan.getGlue()))) {
                    // 判断分厂 + 胶料是否有配置对应密炼区
                    glueDemandPlan.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    glueDemandPlanInit.setId(-999L);
                    String message = I18nUtil.getMessage("schedule.glueDemandPlan.notExistsMixArea");
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                //不存在对应物料名称
                if(!materialNameSet.contains(glueDemandPlan.getGlue())){
                    glueDemandPlan.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    glueDemandPlanInit.setId(-999L);
                    String message = I18nUtil.getMessage("schedule.glueDemandPlan.glueExists");
                    addImportErrorLog(importLogId, i + 2, String.format(message,glueDemandPlan.getGlue()), importErrorLogs);
                }

                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, glueDemandPlan); //校验excel每个单元格长度、类型等

                glueDemandPlan.setBatchNo(batchNo);
                glueDemandPlanInit.setBatchNo(batchNo);
                if (CollectionUtils.isEmpty(validated) && glueDemandPlan.getId() == null) {
                    glueDemandPlan.setBaseValue(null);
                    glueDemandPlan.setDataSource(ZltConstant.DEMAND_SOURCE_IMPORT);
                    glueDemandPlanInit.setBaseValue(null);
                    glueDemandPlanInit.setDataSource(ZltConstant.DEMAND_SOURCE_IMPORT);
                    importList.add(glueDemandPlanInit);
                } else {
                    glueDemandPlan.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    glueDemandPlanInit.setId(-999L);
                    importErrorLogs.addAll(validated);
                }
            }

            if (CollectionUtils.isNotEmpty(importList)) {
                // 先将数据原有表数据备份到日志表中，再物理删除原有表数据，最后批量新增
                GlueDemandPlan glueDemandPlan = new GlueDemandPlan();
                glueDemandPlan.setPlanDate(planDate);
                glueDemandPlan.setFactory(factory);
                glueDemandPlanMapper.backupToGlueDemandPlanLog(glueDemandPlan);
                glueDemandPlanMapper.deleteByPlanDateAndFactory(glueDemandPlan);
                GlueDemandPlanInit glueDemandPlanInit = new GlueDemandPlanInit();
                glueDemandPlanInit.setPlanDate(planDate);
                glueDemandPlanInit.setFactory(factory);
                glueDemandPlanInitMapper.backupToGlueDemandPlanInitLog(glueDemandPlanInit);
                glueDemandPlanInitMapper.deleteByPlanDateAndFactory(glueDemandPlanInit);
                // 将t_glue_demand_plan新导入的数据，新增到t_glue_demand_plan_init表中作为初始数据
                glueDemandPlanInitMapper.batchInsertGlueDemandPlanInitInfo(importList);
                glueDemandPlanMapper.batchInsertFromInit(glueDemandPlan);  //批量插入
                glueDemandPlanMapper.rematch(planDate, factory);
            }
        } catch (Exception e) {
            log.error("导入出错", e);
            // 执行sql失败，插入导入失败记录
            failureNum = list.size();
            importErrorLogs.clear();
            addImportErrorLog(importLogId, null, e.getMessage(), importErrorLogs);
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        }

        successNum = importList.size();  //成功记录数
        failureNum = list.size() - skipNum - successNum; //失败记录数
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }

    /**
     * 根据系统中设置好的 胶料号与密炼区的匹配关系，重新匹配密炼区为空的 分厂胶料需求计划
     *
     * @param planDate 计划日期
     */
    @Override
    public void rematch(Date planDate) {
    	if (glueDemandPlanMapper.checkRematchNotExistsMixArea(planDate) > 0) { // 如果存在没有配置对应关系的直接报错
    		throw new RuntimeException(I18nUtil.getMessage("schedule.glueDemandPlan.Rematch.notExistsMixArea"));
    	}
        this.glueDemandPlanMapper.rematch(planDate, null);
    }

    /**
     * 分厂计划根据密炼区拆分
     *
     * @param list 拆分后的记录
     * @param id   拆分前的记录id
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void splitPlan(List<GlueDemandPlan> list, Long id) {
        this.deleteGlueDemandPlanByIds(new Long[]{id});  //拆分前删除原来的数据
        //保存拆分后的数据
        for (GlueDemandPlan plan : list) {
            if (plan.getTotalPlanQty() == null || BigDecimal.ZERO.equals(plan.getTotalPlanQty())) {
                continue;
            }
            plan.setTotalPlanQty(plan.getTotalPlanQty() == null ? BigDecimal.ZERO : plan.getTotalPlanQty());
            plan.setMidPlanQty(plan.getMidPlanQty() == null ? BigDecimal.ZERO : plan.getMidPlanQty());
            plan.setNightPlanQty(plan.getNightPlanQty() == null ? BigDecimal.ZERO : plan.getNightPlanQty());
            plan.setDayPlanQty(plan.getDayPlanQty() == null ? BigDecimal.ZERO : plan.getDayPlanQty());
            plan.setRemark("拆分源数据id：" + id);
            plan.setDataSource(ZltConstant.DEMAND_SOURCE_SPLIT);
            this.saveGlueDemandPlan(plan);
        }
    }

    /**
     * 根据模板导出分厂胶料需求计划
     *
     * @param glueDemandPlan 查询参数
     * @return 文件字节
     */
    @Override
    public byte[] export(GlueDemandPlanExportDictDto glueDemandPlan) {
        List<GlueDemandPlan> list = glueDemandPlanMapper.selectGlueDemandPlanList(glueDemandPlan);
        //按用户语言读取模板
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + I18nUtil.getMessage("schedule.glueDemandPlan.modelName") + ".xlsx");
        Workbook webBook = ExcelUtils.readExcel(in);
        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            Map<String, String> factoryDictMap = glueDemandPlan.getFactoryDictMap();
            Map<String, String> mixAreaDictMap = glueDemandPlan.getMixAreaDictMap();
            Sheet sheet = webBook.getSheetAt(0);
            CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
            int[] cellNums = {1, 2, 4, 9, 10, 13, 15, 16, 18};
            for (int i = 0; i < list.size(); i++) {
                GlueDemandPlan demandPlan = list.get(i);
                Row row = sheet.createRow(i + 1);
                int cellNumIndex = 0;
                row.createCell(cellNums[cellNumIndex++]).setCellValue(demandPlan.getPlanDate() == null ? "" : DateFormatUtils.format(demandPlan.getPlanDate(), "yyyy-MM-dd"));
                row.createCell(cellNums[cellNumIndex++]).setCellValue(demandPlan.getFactory() == null ? "" : factoryDictMap.getOrDefault(demandPlan.getFactory(), ""));
                row.createCell(cellNums[cellNumIndex++]).setCellValue(demandPlan.getGlue() == null ? "" : demandPlan.getGlue());
                row.createCell(cellNums[cellNumIndex++]).setCellValue(demandPlan.getTotalPlanQty() == null ? BigDecimal.ZERO.doubleValue() : demandPlan.getTotalPlanQty().doubleValue());
                String mixAreaStr = demandPlan.getMixArea();
                StringBuilder mixAreaDictLabel = new StringBuilder();
                if (StringUtils.isNotBlank(mixAreaStr)) {
                    String[] mixAreaArr = mixAreaStr.split(",");
                    for (String mixArea : mixAreaArr) {
                        mixAreaDictLabel.append(mixAreaDictMap.getOrDefault(mixArea, "")).append(",");
                    }
                }
                row.createCell(cellNums[cellNumIndex++]).setCellValue(mixAreaStr == null ? "" : mixAreaDictLabel.substring(0, mixAreaDictLabel.length() - 1));
                row.createCell(cellNums[cellNumIndex++]).setCellValue(demandPlan.getMidPlanQty() == null ? BigDecimal.ZERO.doubleValue() : demandPlan.getMidPlanQty().doubleValue());
                row.createCell(cellNums[cellNumIndex++]).setCellValue(demandPlan.getMidRemark() == null ? "" : demandPlan.getMidRemark());
                row.createCell(cellNums[cellNumIndex++]).setCellValue(demandPlan.getNightPlanQty() == null ? BigDecimal.ZERO.doubleValue() : demandPlan.getNightPlanQty().doubleValue());
                row.createCell(cellNums[cellNumIndex++]).setCellValue(demandPlan.getNightRemark() == null ? "" : demandPlan.getNightRemark());
                // row.createCell(cellNums[cellNumIndex++]).setCellValue(demandPlan.getDayPlanQty() == null ? BigDecimal.ZERO.doubleValue() : demandPlan.getDayPlanQty().doubleValue());
                // row.createCell(cellNums[cellNumIndex]).setCellValue(demandPlan.getDayRemark() == null ? "" : demandPlan.getDayRemark());
                for (int cellNum : cellNums) {
                    row.getCell(cellNum).setCellStyle(cellStyle);
                }
            }
        }
        //写出字节流
        ByteArrayOutputStream out = null;
        byte[] data = null;
        try {
            out = new ByteArrayOutputStream();
            webBook.write(out);
            data = out.toByteArray();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(out);
        }
        return data;
    }

    /**
     * 检测对应日期和分厂的数据是否存在
     *
     * @param glueDemandPlan 日期和分厂
     * @return 是否唯一的常量值
     */
    @Override
    public String checkPlanDateAndFactoryExist(GlueDemandPlan glueDemandPlan) {
        if (glueDemandPlan == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        LambdaQueryWrapper<GlueDemandPlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GlueDemandPlan::getPlanDate, glueDemandPlan.getPlanDate());
        queryWrapper.eq(GlueDemandPlan::getFactory, glueDemandPlan.getFactory());
        queryWrapper.eq(GlueDemandPlan::getDelFlag, ZltConstant.DEL_FLAG_NORMAL);

        Long integer = glueDemandPlanMapper.selectCount(queryWrapper);
        if (integer != null && integer > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 计算总计划量
     * @param glueDemandPlan 中班、夜班、白班计划量总和计算
     */
    private void setTotalPlanQty(GlueDemandPlan glueDemandPlan) {
        BigDecimal midPlanQty = glueDemandPlan.getMidPlanQty() == null ? BigDecimal.ZERO : glueDemandPlan.getMidPlanQty();
        BigDecimal nightPlanQty = glueDemandPlan.getNightPlanQty() == null ? BigDecimal.ZERO : glueDemandPlan.getNightPlanQty();
        BigDecimal dayPlanQty = glueDemandPlan.getDayPlanQty() == null ? BigDecimal.ZERO : glueDemandPlan.getDayPlanQty();
        BigDecimal totalPlanQty = midPlanQty.add(nightPlanQty).add(dayPlanQty);
        glueDemandPlan.setTotalPlanQty(totalPlanQty);
    }
}
