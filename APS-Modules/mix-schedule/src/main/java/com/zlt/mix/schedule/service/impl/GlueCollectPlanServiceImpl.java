package com.zlt.mix.schedule.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.enums.IsFinishEnum;
import com.zlt.mix.common.core.utils.CollectionUtil;
import com.zlt.mix.common.core.utils.DateUtil;
import com.zlt.mix.common.core.utils.ExcelUtils;
import com.zlt.mix.common.engine.constants.EngineConstants;
import com.zlt.mix.common.engine.service.impl.IncrementService;
import com.zlt.mix.schedule.api.domain.dto.GlueCollectPlanExportDictDto;
import com.zlt.mix.schedule.api.domain.entity.GlueCollectPlan;
import com.zlt.mix.schedule.engine.service.basicdata.ParamsEngineService;
import com.zlt.mix.schedule.mapper.GlueCollectPlanMapper;
import com.zlt.mix.schedule.service.GlueCollectPlanService;
import com.zlt.mix.schedule.service.GlueScheduleResultService;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.mix.common.core.constant.ZltConstant.DEFAULT_PARAMS_MIX_AREA;
import static com.zlt.mix.schedule.engine.constants.GlueEngineConstants.IS_ADD_LAST_SURPLUS;

/**
 * 汇总胶料需求计划Service业务层处理
 *
 * @author chen
 * @date 2022-04-25
 */
@Service
public class GlueCollectPlanServiceImpl extends ServiceImpl<GlueCollectPlanMapper, GlueCollectPlan> implements GlueCollectPlanService {
    @Resource
    private GlueCollectPlanMapper glueCollectPlanMapper;

    @Resource
    private IncrementService incrementService;

    @Autowired
    private GlueScheduleResultService glueScheduleResultService;

    @Resource
    private ParamsEngineService paramsEngineService;

    @Value("${excelModelPath}")
    public String excelModelPath;

    /**
     * 查询汇总胶料需求计划列表
     *
     * @param glueCollectPlan 汇总胶料需求计划
     * @return 汇总胶料需求计划
     */
    @Override
    public List<GlueCollectPlan> selectGlueCollectPlanList(GlueCollectPlan glueCollectPlan) {
        List<GlueCollectPlan> list = glueCollectPlanMapper.selectGlueCollectPlanList(glueCollectPlan);
        MixMachine param = new MixMachine();
        param.setMixArea(glueCollectPlan.getMixArea());
        List<MixMachine> mixMachineList = glueScheduleResultService.getMachineInfo(param);
        if (CollectionUtil.isEmpty(mixMachineList)) {
            return list;
        }
        Map<String, String> machineMap = mixMachineList.stream().collect(Collectors.toMap(item -> item.getMixArea() + item.getMachineCode(), MixMachine::getMachineName));
        for (GlueCollectPlan collectPlan : list) {
            String planMachineCode = collectPlan.getMachineCode();
            if (planMachineCode == null) {
                continue;
            }
            String mixArea = collectPlan.getMixArea();
            String[] machineCodeArr = planMachineCode.split(",");
            StringBuilder machineName = new StringBuilder();
            for (String machineCode : machineCodeArr) {
                machineName.append(machineMap.getOrDefault(mixArea + machineCode, "")).append(",");
            }
            collectPlan.setMachineName(machineName.substring(0, machineName.length() - 1));
        }
        return list;
    }

    /**
     * 保存汇总胶料需求计划信息（id为空则新增，id不为空则修改）
     *
     * @param glueCollectPlan
     */
    @Override
    public void saveGlueCollectPlan(GlueCollectPlan glueCollectPlan) {
        glueCollectPlan.setBaseValue(glueCollectPlan.getId());
        this.saveOrUpdate(glueCollectPlan);
    }

    /**
     * 批量删除汇总胶料需求计划
     *
     * @param ids 需要删除的汇总胶料需求计划ID
     * @return 结果
     */
    @Override
    public int deleteGlueCollectPlanByIds(Long[] ids) {
        return glueCollectPlanMapper.deleteGlueCollectPlanByIds(ids);
    }

    /**
     * 汇总计划(先备份数据，在删掉当前数据，最后在重新汇总最新的数据到表中)
     * @param glueCollectPlan
     */
    @Transactional(rollbackFor = Exception.class)
    public void summaryPlan(GlueCollectPlan glueCollectPlan) {
        Date planDate = glueCollectPlan.getPlanDate();
        if(planDate == null) {
            return;
        }
        String batchNo = incrementService.getSequence3(EngineConstants.COLLECT_PREFIX + DateUtil.formatDateYmd(planDate));  //创建批次号
        glueCollectPlanMapper.syncCollectPlanToLog(planDate);   //把数据备份到日志表中
        glueCollectPlanMapper.deleteCollectPlan(planDate);   //删除旧数据
        glueCollectPlanMapper.summaryBasePlan(batchNo, planDate, SecurityUtils.getUsername());
        glueCollectPlanMapper.matchMachine(planDate);   //匹配密炼机台
        glueCollectPlanMapper.matchSpecialMachine(planDate);   //匹配终炼胶的特殊一次法机台

        Map<String, String> params = paramsEngineService.mapGlueParams(DEFAULT_PARAMS_MIX_AREA);   //胶料参数设置map
        glueCollectPlanMapper.lastSurplusPlan(planDate, params.getOrDefault(IS_ADD_LAST_SURPLUS, ""));//更新昨日剩余和生产量

        // 汇总后重算白班待支领量
        glueCollectPlanMapper.recaculateGlueUnclaimed(planDate);
    }

    /**
     * 汇总计划前进行密炼区域验证数据格式
     * @param glueCollectPlan
     * @return
     */
    @Override
    public String validateMixAreaData(GlueCollectPlan glueCollectPlan) {
        String msg="";
        Date planDate = glueCollectPlan.getPlanDate();
        if(planDate == null) {
            return msg;
        }
        String planDateStr= DateUtils.parseDateToStr("yyyy-MM-dd",planDate);

        int exceptionCount=glueCollectPlanMapper.countOfMixAreaException(planDateStr);
        if(BigDecimal.ZERO.intValue()<exceptionCount ){
            msg= StringUtils.format(I18nUtil.getMessage("schedule.glueCollectPlan.mixArea.error"),planDateStr);
        }
        return msg;
    }

    /**
     * 根据模板文件导出到Excel
     *
     * @param dto 参数
     * @return Excel字节数组
     */
    @Override
    public byte[] exportData(GlueCollectPlanExportDictDto dto) {
        List<GlueCollectPlan> list = this.selectGlueCollectPlanList(dto);
        //按用户语言读取模板
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + I18nUtil.getMessage("schedule.glueCollectPlan.modelName") + ".xlsx");
        Workbook webBook = ExcelUtils.readExcel(in);
        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            Map<String, String> factoryDictMap = dto.getFactoryDictMap();
            Map<String, String> mixAreaDictMap = dto.getMixAreaDictMap();
            Sheet sheet = webBook.getSheetAt(0);
            CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
            for (int i = 0; i < list.size(); i++) {
                GlueCollectPlan glueCollectPlan = list.get(i);
                Row row = sheet.createRow(i + 2);
                int cellNum = 0;
                row.createCell(cellNum++).setCellValue(glueCollectPlan.getPlanDate() == null ? "" : DateFormatUtils.format(glueCollectPlan.getPlanDate(), "yyyy-MM-dd"));
                String factoryStr = StringUtils.isNotBlank(glueCollectPlan.getFactory()) ? glueCollectPlan.getFactory() : "";
                StringBuilder factorySb = new StringBuilder();
                if (factoryStr.contains(",")) {
                    String[] factoryArr = factoryStr.split(",");
                    for (String factory : factoryArr) {
                        factorySb.append(factoryDictMap.getOrDefault(factory, "")).append(",");
                    }
                }else {
                    factorySb.append(factoryDictMap.getOrDefault(factoryStr, "")).append(",");
                }
                row.createCell(cellNum++).setCellValue(factorySb.substring(0 , factorySb.length() - 1));
                row.createCell(cellNum++).setCellValue(glueCollectPlan.getGlue() == null ? "" : glueCollectPlan.getGlue());
                String mixAreaStr = glueCollectPlan.getMixArea();
                StringBuilder mixAreaDictLabel = new StringBuilder();
                if (StringUtils.isNotBlank(mixAreaStr)) {
                    String[] mixAreaArr = mixAreaStr.split(",");
                    for (String mixArea : mixAreaArr) {
                        mixAreaDictLabel.append(mixAreaDictMap.getOrDefault(mixArea, "")).append(",");
                    }
                }
                row.createCell(cellNum++).setCellValue(mixAreaStr == null ? "" : mixAreaDictLabel.substring(0, mixAreaDictLabel.length() - 1));
                row.createCell(cellNum++).setCellValue(glueCollectPlan.getMachineName() == null ? "" : glueCollectPlan.getMachineName());
                String isFinishing = glueCollectPlan.getIsFinishing() == null ? "" : glueCollectPlan.getIsFinishing();
                row.createCell(cellNum++).setCellValue(isFinishing.equals(IsFinishEnum.IS_FINISH_NOT.getDictValue()) ? IsFinishEnum.IS_FINISH_NOT.getDictLabel() : IsFinishEnum.IS_FINISH_YES.getDictLabel());
                row.createCell(cellNum++).setCellValue(glueCollectPlan.getTotalPlanQty() == null ? BigDecimal.ZERO.doubleValue() : glueCollectPlan.getTotalPlanQty());
                row.createCell(cellNum++).setCellValue(glueCollectPlan.getLastSurplus() == null ? BigDecimal.ZERO.doubleValue() : glueCollectPlan.getLastSurplus());
                row.createCell(cellNum++).setCellValue(glueCollectPlan.getProduceQty() == null ? BigDecimal.ZERO.doubleValue() : glueCollectPlan.getProduceQty());
                row.createCell(cellNum++).setCellValue(glueCollectPlan.getMidPlanQty() == null ? BigDecimal.ZERO.doubleValue() : glueCollectPlan.getMidPlanQty());
                row.createCell(cellNum++).setCellValue(glueCollectPlan.getMidRemark() == null ? "" : glueCollectPlan.getMidRemark());
                row.createCell(cellNum++).setCellValue(glueCollectPlan.getNightPlanQty() == null ? BigDecimal.ZERO.doubleValue() : glueCollectPlan.getNightPlanQty());
                row.createCell(cellNum).setCellValue(glueCollectPlan.getNightRemark() == null ? "" : glueCollectPlan.getNightRemark());
                // row.createCell(cellNum++).setCellValue(glueCollectPlan.getDayPlanQty() == null ? BigDecimal.ZERO.doubleValue() : glueCollectPlan.getDayPlanQty());
                // row.createCell(cellNum).setCellValue(glueCollectPlan.getDayRemark() == null ? "" : glueCollectPlan.getDayRemark());
                for (int j = 0; j <= cellNum; j++) {
                    row.getCell(j).setCellStyle(cellStyle);
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
     * 检测对应日期的数据是否存在
     *
     * @param glueCollectPlan 日期
     * @return 是否唯一的常量值
     */
    @Override
    public String checkPlanDateExist(GlueCollectPlan glueCollectPlan) {
        if (glueCollectPlan == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        LambdaQueryWrapper<GlueCollectPlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GlueCollectPlan::getPlanDate, glueCollectPlan.getPlanDate());
        queryWrapper.eq(GlueCollectPlan::getDelFlag, ZltConstant.DEL_FLAG_NORMAL);

        Long integer = glueCollectPlanMapper.selectCount(queryWrapper);
        if (integer != null && integer > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }
}
