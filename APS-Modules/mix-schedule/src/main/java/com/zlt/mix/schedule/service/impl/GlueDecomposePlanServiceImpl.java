package com.zlt.mix.schedule.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.service.ISysDictDataCacheService;
import com.ruoyi.common.core.domain.SysDictData;
import com.ruoyi.common.core.utils.DateUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.ruoyi.common.utils.StringUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.BigDecimalUtil;
import com.zlt.mix.common.core.utils.CollectionUtil;
import com.zlt.mix.common.core.utils.ExcelUtils;
import com.zlt.mix.common.core.utils.MixCommonUtil;
import com.zlt.mix.schedule.api.domain.dto.GlueDecomposePlanExportDictDto;
import com.zlt.mix.schedule.api.domain.dto.GlueSpanReceiveDto;
import com.zlt.mix.schedule.api.domain.dto.GlueSpanSendDto;
import com.zlt.mix.schedule.api.domain.entity.GlueDecomposePlan;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanSend;
import com.zlt.mix.schedule.engine.service.basicdata.MachineEngineService;
import com.zlt.mix.schedule.engine.service.basicdata.MixingMinProductEngineService;
import com.zlt.mix.schedule.engine.service.basicdata.RecipeEngineService;
import com.zlt.mix.schedule.engine.service.decompose.DecomposeEngineService;
import com.zlt.mix.schedule.engine.vo.GlueAreaMachineVo;
import com.zlt.mix.schedule.engine.vo.GlueSendReceiveVo;
import com.zlt.mix.schedule.mapper.GlueDecomposePlanMapper;
import com.zlt.mix.schedule.service.GlueDecomposePlanService;
import com.zlt.mix.schedule.service.GlueScheduleResultService;
import com.zlt.mix.schedule.service.GlueSpanReceiveService;
import com.zlt.mix.schedule.service.GlueSpanSendService;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分解胶料需求量Service业务层处理
 *
 * @author chen
 * @date 2022-05-04
 */
@Service
public class GlueDecomposePlanServiceImpl extends ServiceImpl<GlueDecomposePlanMapper, GlueDecomposePlan> implements GlueDecomposePlanService {
    @Resource
    private GlueDecomposePlanMapper glueDecomposePlanMapper;
    @Resource
    private GlueScheduleResultService glueScheduleResultService;
    @Resource
    private DecomposeEngineService decomposeEngineService;
    @Resource
    private GlueSpanSendService glueSpanSendService;
    @Resource
    private GlueSpanReceiveService glueSpanReceiveService;
    @Resource
    private RecipeEngineService recipeEngineService;
    @Resource
    private MachineEngineService machineEngineService;
    @Resource
    private ISysDictDataCacheService iSysDictDataCacheService;
    @Resource
    private MixingMinProductEngineService mixingMinProductEngineService;

    @Value("${excelModelPath}")
    public String excelModelPath;

    /**
     * 查询分解胶料需求量列表
     *
     * @param glueDecomposePlan 分解胶料需求量
     * @return 分解胶料需求量
     */
    @Override
    public List<GlueDecomposePlan> selectGlueDecomposePlanList(GlueDecomposePlan glueDecomposePlan) {
        List<GlueDecomposePlan> list = glueDecomposePlanMapper.selectGlueDecomposePlanList(glueDecomposePlan);
        getMachineName(list, glueDecomposePlan.getMixArea());
        return list;
    }

    /**
     * 保存分解胶料需求量信息（id为空则新增，id不为空则修改）
     */
    @Override
    public List<GlueDecomposePlan> saveGlueDecomposePlan(GlueDecomposePlan glueDecomposePlan) {
        if (ZltConstant.NOT_UNIQUE.equals(checkGlueDecomposePlanUnique(glueDecomposePlan))) {
            throw new RuntimeException(I18nUtil.getMessage("schedule.glueDecomposePlan.database.unique"));
        }
        boolean isAdd = (glueDecomposePlan.getId() == null);  //true：新增，false：修改
        glueDecomposePlan.setBaseValue(glueDecomposePlan.getId());
        // 新增时回填库存、安全库存、生产量
        if (isAdd) {
            glueDecomposePlan.setIsFinishing(ZltConstant.IS_FINISHING_NO);
            decomposeEngineService.addDecomposePlan(glueDecomposePlan);  //分解胶料需求--新增(可以新增终炼胶，也可以新增母炼胶。新增后要把现新增的胶料的子胶 也一起计算新增进去)
            return new ArrayList<>();
        } else {
            //需要判断下是否 用户有修改机台、计划量、备注，只有值有变化，才做以下操作
            GlueDecomposePlan plan = baseMapper.selectById(glueDecomposePlan.getId());
            //标记是否修改生产量
            boolean changeProduceQty = !MixCommonUtil.compare(glueDecomposePlan.getProduceQty(), plan.getProduceQty());
            //标记是否修改机台编号
            boolean changeMachineCode = !MixCommonUtil.compare(glueDecomposePlan.getMachineCode(), plan.getMachineCode());
            //标记是否修改备注
            boolean changeRemark = !MixCommonUtil.compare(glueDecomposePlan.getRemark(), plan.getRemark());
            if (changeProduceQty || changeMachineCode || changeRemark) {
                glueDecomposePlan.setUpGlue(plan.getUpGlue());
                if(changeMachineCode) {
                    //如果是转机台
                    Double produceQty = glueDecomposePlan.getProduceQty();
                    String newMachineCode = glueDecomposePlan.getMachineCode();
                    String newRemark = glueDecomposePlan.getRemark();
                    BeanUtils.copyProperties(plan, glueDecomposePlan);  //复制对象
                    glueDecomposePlan.setProduceQty(produceQty);
                    glueDecomposePlan.setMachineCode(newMachineCode);
                    glueDecomposePlan.setRemark(newRemark);
                }
                //优先级（是否修改生产量方式）：修改生产量（isModifyProduceQty=true） > 修改机台编号（isModifyProduceQty=false） >  修改备注（isModifyProduceQty=true）
                List<GlueDecomposePlan> updateList = decomposeEngineService.recalculateDecomposePlan(glueDecomposePlan,
                        changeProduceQty || (!changeMachineCode && changeRemark));  //修改了安全库存、生产量、机台后，当前记录以及它的子胶的计划量、生产量都需要重新计算
                if (updateList.size() > 0) {
                    glueDecomposePlanMapper.mergeById(updateList);
                    // 加载炼胶单规格最小排产数
                    Map<String, BigDecimal> mixingMinProductMap = mixingMinProductEngineService.mapMixingMinProduct(glueDecomposePlan.getMixArea());
                    // 如果新增的分解计划，涉及到塑炼胶，需要重新计算塑炼的胶的计划量
                    decomposeEngineService.updateSLDecomposePlan(glueDecomposePlan.getMixArea(), glueDecomposePlan.getPlanDate(), updateList, mixingMinProductMap);
                    getMachineName(updateList, glueDecomposePlan.getMixArea());
                    return updateList;
                }
            }
            return new ArrayList<>();
        }
    }

    /**
     * 批量删除分解胶料需求量
     *
     * @param ids 需要删除的分解胶料需求量ID
     * @return 结果
     */
    @Override
    public int deleteGlueDecomposePlanByIds(Long[] ids) {
        return glueDecomposePlanMapper.deleteGlueDecomposePlanByIds(ids);
    }


    /**
     * 校验分解胶料需求量唯一性
     */
    @Override
    public String checkGlueDecomposePlanUnique(GlueDecomposePlan glueDecomposePlan) {
        if (glueDecomposePlan == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        QueryWrapper<GlueDecomposePlan> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DEL_FLAG", ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq("PLAN_DATE", glueDecomposePlan.getPlanDate());
        queryWrapper.eq("MIX_AREA", glueDecomposePlan.getMixArea());
        queryWrapper.eq("GLUE", glueDecomposePlan.getGlue());
        if (glueDecomposePlan.getId() != null) {
            queryWrapper.ne("ID", glueDecomposePlan.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<GlueDecomposePlan> list = glueDecomposePlanMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 更新安全库存
     *
     * @param glueDecomposePlan 要更新的数据
     */
    @Override
    public List<GlueDecomposePlan> updateSafeStock(GlueDecomposePlan glueDecomposePlan) {
        if (ZltConstant.NOT_UNIQUE.equals(checkGlueDecomposePlanUnique(glueDecomposePlan))) {
            throw new RuntimeException(I18nUtil.getMessage("schedule.glueDecomposePlan.database.unique"));
        }

        GlueDecomposePlan plan = baseMapper.selectById(glueDecomposePlan.getId());
        //需要判断下是否 用户有修改 安全库存，只有值有变化，才做以下操作
        if (!MixCommonUtil.compare(glueDecomposePlan.getSafeStockQty(), plan.getSafeStockQty())) {
            glueDecomposePlan.setBaseValue(glueDecomposePlan.getId());
            glueDecomposePlanMapper.mergeSafeStock(glueDecomposePlan);   //更新安全库存

            glueDecomposePlan.setUpGlue(plan.getUpGlue());
            List<GlueDecomposePlan> updateList = decomposeEngineService.recalculateDecomposePlan(glueDecomposePlan, false);   //修改了安全库存、生产量、机台后，当前记录以及它的子胶的计划量、生产量都需要重新计算
            glueDecomposePlanMapper.mergeById(updateList);
            getMachineName(updateList, glueDecomposePlan.getMixArea());
            return updateList;
        }

        return new ArrayList<>();
    }

    /**
     * 分解计划前进行密炼机台校验
     *
     * @param glueDecomposePlan
     * @return
     */
    @Override
    public String validateMachineData(GlueDecomposePlan glueDecomposePlan) {
        StringBuilder msg = new StringBuilder();
        Date planDate = glueDecomposePlan.getPlanDate();
        if (planDate == null) {
            return msg.toString();
        }
        String mixArea = glueDecomposePlan.getMixArea();
        if (StringUtils.isEmpty(mixArea)) {
            return msg.toString();
        }
        String planDateStr = DateUtils.parseDateToStr("yyyy-MM-dd", planDate);
        int exceptionCount = glueDecomposePlanMapper.countOfMachineCodeException(planDateStr, mixArea);
        if (BigDecimal.ZERO.intValue() < exceptionCount) {
            List<SysDictData> mixAreaList = iSysDictDataCacheService.getType("MIX_AREA");
            Map<String, String> mixAreaMap = mixAreaList.stream().collect(Collectors.toMap(SysDictData::getDictValue, SysDictData::getDictLabel, (v1, v2) -> v1));
            msg.append(StringUtils.format(I18nUtil.getMessage("schedule.glueDecomposePlan.machine.error"), planDateStr, mixAreaMap.getOrDefault(mixArea, mixArea)));
        }
        return msg.toString();
    }

    /**
     * 根据模板文件导出到Excel
     *
     * @param dto 参数
     * @return Excel字节数组
     */
    @Override
    public byte[] exportData(GlueDecomposePlanExportDictDto dto) {
        List<GlueDecomposePlan> list = this.selectGlueDecomposePlanList(dto);
        //按用户语言读取模板
        InputStream in = this.getClass().getClassLoader().getResourceAsStream(excelModelPath + I18nUtil.getMessage("schedule.glueDecomposePlan.modelName") + ".xlsx");
        Workbook webBook = ExcelUtils.readExcel(in);
        //填充数据
        if (CollectionUtils.isNotEmpty(list)) {
            Map<String, String> mixAreaDictMap = dto.getMixAreaDictMap();
            Map<String, String> isFinishingDictMap = dto.getIsFinishingDictMap();
            Sheet sheet = webBook.getSheetAt(0);
            CellStyle cellStyle = ExcelUtils.createCellStyle(webBook);
            for (int i = 0; i < list.size(); i++) {
                GlueDecomposePlan glueDecomposePlan = list.get(i);
                Row row = sheet.createRow(i + 2);
                int cellNum = 0;
                row.createCell(cellNum++).setCellValue(glueDecomposePlan.getPlanDate() == null ? "" : DateFormatUtils.format(glueDecomposePlan.getPlanDate(), "yyyy-MM-dd"));
                row.createCell(cellNum++).setCellValue(glueDecomposePlan.getGlue() == null ? "" : glueDecomposePlan.getGlue());
                row.createCell(cellNum++).setCellValue(glueDecomposePlan.getIsFinishing() == null ? "" : isFinishingDictMap.getOrDefault(glueDecomposePlan.getIsFinishing(), ""));
                row.createCell(cellNum++).setCellValue(glueDecomposePlan.getPlanQty() == null ? BigDecimal.ZERO.doubleValue() : glueDecomposePlan.getPlanQty());
                row.createCell(cellNum++).setCellValue(glueDecomposePlan.getStockQty() == null ? BigDecimal.ZERO.doubleValue() : glueDecomposePlan.getStockQty());
                row.createCell(cellNum++).setCellValue(glueDecomposePlan.getSafeStockQty() == null ? BigDecimal.ZERO.doubleValue() : glueDecomposePlan.getSafeStockQty());
                row.createCell(cellNum++).setCellValue(glueDecomposePlan.getProduceQty() == null ? BigDecimal.ZERO.doubleValue() : glueDecomposePlan.getProduceQty());
                row.createCell(cellNum).setCellValue(glueDecomposePlan.getMachineName() == null ? "" : glueDecomposePlan.getMachineName());
                for (int j = 0; j <= cellNum; j++) {
                    row.getCell(j).setCellStyle(cellStyle);
                }
            }
            Cell cell = sheet.getRow(0).getCell(3);
            String title = cell.getStringCellValue();
            title = title + mixAreaDictMap.getOrDefault(dto.getMixArea(), "");
            cell.setCellValue(title);
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
     * 检测对应日期和密炼区的数据是否存在
     *
     * @param glueDecomposePlan 时间和密炼区
     * @return 是否唯一的常量值
     */
    @Override
    public String checkPlanDateAndMixAreaExist(GlueDecomposePlan glueDecomposePlan) {
        if (glueDecomposePlan == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        LambdaQueryWrapper<GlueDecomposePlan> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(GlueDecomposePlan::getPlanDate, glueDecomposePlan.getPlanDate());
        queryWrapper.eq(GlueDecomposePlan::getMixArea, glueDecomposePlan.getMixArea());
        queryWrapper.eq(GlueDecomposePlan::getDelFlag, ZltConstant.DEL_FLAG_NORMAL);

        Long integer = glueDecomposePlanMapper.selectCount(queryWrapper);
        if (integer != null && integer > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 根据机台编号查询机台名称并设值
     *
     * @param updateList 要查询的集合
     * @param mixArea    密炼区
     */
    private void getMachineName(List<GlueDecomposePlan> updateList, String mixArea) {
        MixMachine param = new MixMachine();
        param.setMixArea(mixArea);
        List<MixMachine> mixMachineList = glueScheduleResultService.getMachineInfo(param);
        if (CollectionUtil.isEmpty(mixMachineList)) {
            return;
        }
        Map<String, String> machineMap = mixMachineList.stream().collect(Collectors.toMap(MixMachine::getMachineCode, MixMachine::getMachineName));
        for (GlueDecomposePlan decomposePlan : updateList) {
            String planMachineCode = decomposePlan.getMachineCode();
            if (planMachineCode == null) {
                continue;
            }
            String[] machineCodeArr = planMachineCode.split(",");
            StringBuilder machineName = new StringBuilder();
            for (String machineCode : machineCodeArr) {
                machineName.append(machineMap.getOrDefault(machineCode, "")).append(",");
            }
            decomposePlan.setMachineName(machineName.substring(0, machineName.length() - 1));
        }
    }

    /**
     * 检测对应日期和密炼区的数据是否存在没有选择机台的记录
     *
     * @param glueDecomposePlan 时间和密炼区
     * @return 是否唯一的常量值
     */
	public String checkMachineError(GlueDecomposePlan glueDecomposePlan) {
		if (glueDecomposePlan == null) {
			return ZltConstant.NOT_UNIQUE;
		}

		LambdaQueryWrapper<GlueDecomposePlan> queryWrapper = new LambdaQueryWrapper<>();
		queryWrapper.eq(GlueDecomposePlan::getPlanDate, glueDecomposePlan.getPlanDate());
		queryWrapper.eq(GlueDecomposePlan::getMixArea, glueDecomposePlan.getMixArea());
		queryWrapper.eq(GlueDecomposePlan::getDelFlag, ZltConstant.DEL_FLAG_NORMAL);
		queryWrapper.and(wrapper -> wrapper.or()
				// 机台编号为空
				.or(wrapper_or1 -> wrapper_or1.isNull(GlueDecomposePlan::getMachineCode)).or()
				// 或者机台编号不唯一
				.or(wrapper_or2 -> wrapper_or2.like(GlueDecomposePlan::getMachineCode, ",")));
		Long integer = glueDecomposePlanMapper.selectCount(queryWrapper);
		if (integer != null && integer > 0) {
			return ZltConstant.NOT_UNIQUE;
		}
		return ZltConstant.UNIQUE;
	}

    /**
     * 根据条件查询分解胶料需求量跨区发送列表
     *
     * @param entity 查询条件
     * @return 结果
     */
    @Override
    public List<GlueSpanSend> listGlueSpanSend(GlueSpanSend entity) {
        entity.setSource(ZltConstant.SOURCE_GLUE_DECOMPOSE_PLAN);
        return glueSpanSendService.listGlueSpanSend(entity);
    }

    /**
     * 发送跨区请求
     *
     * @param dto 跨区请求集合
     * @return 结果
     */
    @Override
    public AjaxResult sendGlueSpan(GlueSpanSendDto dto) throws ParseException {
        List<GlueSpanSend> list = dto.getGlueSpanSendList();
        if (CollectionUtils.isNotEmpty(list)) {
            List<GlueSpanReceive> glueSpanReceiveList = new ArrayList<>();
            for (GlueSpanSend glueSpanSend : list) {
                glueSpanSend.setBaseValue(null);
//                glueSpanSend.setScheduleDate(DateUtils.addDays(DateUtils.getNowDate("yyyy-MM-dd"), 1));
                glueSpanSend.setSendTime(new Date());
                glueSpanSend.setReceiveStatus(ZltConstant.RECEIVE_STATUS_NO);
                glueSpanSend.setSource(ZltConstant.SOURCE_GLUE_DECOMPOSE_PLAN);
                glueSpanSend.setIsAuto(ZltConstant.IS_AUTO_NO);
                GlueSpanReceive glueSpanReceive = new GlueSpanReceive();
                BeanUtils.copyProperties(glueSpanSend, glueSpanReceive);
                glueSpanSendService.insertGlueSpanSend(glueSpanSend);
                glueSpanReceive.setSendId(glueSpanSend.getId());
                glueSpanReceiveList.add(glueSpanReceive);
            }
            glueSpanReceiveService.batchInsertGlueSpanReceive(glueSpanReceiveList);
        }
        return AjaxResult.success();
    }

    /**
     * 根据条件查询分解胶料需求量跨区接收列表
     *
     * @param entity 查询条件
     * @return 结果
     */
    @Override
    public List<GlueSpanReceive> listGlueSpanReceive(GlueSpanReceive entity) {
        entity.setSource(ZltConstant.SOURCE_GLUE_DECOMPOSE_PLAN);
        return glueSpanReceiveService.listGlueSpanReceive(entity);
    }

    /**
     * 根据id查询跨区接收信息
     * @param entity id
     * @return 查询到的记录
     */
    @Override
    public GlueSpanReceive getGlueSpanReceiveInfo(GlueSpanReceive entity) {
        return glueSpanReceiveService.getGlueSpanReceiveInfo(entity);
    }

    /**
     * 接收跨区请求
     *
     * @param dto 要接收的跨区请求
     * @return 结果
     */
    @Override
    public AjaxResult receiveGlueSpanReceive(GlueSpanReceiveDto dto) {
        List<GlueSpanReceive> receiveList = dto.getGlueSpanReceiveList();
        Long[] ids = new Long[receiveList.size()];
        for (int i = 0; i < receiveList.size(); i++) {
            GlueSpanReceive glueSpanReceive = receiveList.get(i);
            glueSpanReceive.setBaseValue(glueSpanReceive.getId());
            glueSpanReceive.setReceiveStatus(ZltConstant.RECEIVE_STATUS_YES);
            glueSpanReceive.setReceiveTime(new Date());
            ids[i] = glueSpanReceive.getId();
        }
        if (glueSpanReceiveService.getAlreadyReceivedCountByIds(ids) > 0) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.message.receive.alreadyReceive"));
        }
        glueSpanReceiveService.mergeGlueSpanReceive(receiveList);
        glueSpanSendService.mergeGlueSpanSend(receiveList);
        return AjaxResult.success();
    }

    /**
     * 根据id删除跨区发送记录
     * @param ids id
     * @return 结果
     */
    @Override
    public AjaxResult deleteGlueSpanSend(Long[] ids) {
        // 校验是否已接收
        Integer alreadyReceivedCount = glueSpanSendService.getAlreadyReceivedCount(ids);
        if (alreadyReceivedCount > 0) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.message.send.alreadyReceive"));
        }
        if (glueSpanReceiveService.getAlreadyReceivedCount(ids) > 0) {
            throw new RuntimeException(I18nUtil.getMessage("ui.data.message.send.alreadyReceive"));
        }
        glueSpanSendService.deleteByIds(ids);
        glueSpanReceiveService.deleteBySendIds(ids);
        return AjaxResult.success();
    }

    /**
     * 分解胶料计划后，在根据胶料跨区设置表，自动胶料发送跨区记录
     * @param planDate
     * @param mixArea
     */
    public void autoCreateSpanSend(Date planDate, String mixArea) {
        glueDecomposePlanMapper.deleteAutoNotReceive(planDate, mixArea);   //删除未被接收的接收记录
        glueDecomposePlanMapper.deleteAutoNotReceiveSend(planDate, mixArea);  //删除未被接收的发送记录

        List<GlueSpanSend> list = glueDecomposePlanMapper.listGlueSpanSetting(planDate, mixArea);  //获取胶料跨区发送设置表中的记录
        if(list.isEmpty()) {
            return;
        }
        List<Long> retryReceiveIdList = new ArrayList<>(); //跨区接收对应的分解计划中的机台为空的接收表的id
        List<Long> sendIdList = new ArrayList<>();
        for(GlueSpanSend spanSend : list) {
            //保存发送记录
            spanSend.setBaseValue(null);
            spanSend.setExpectDemandTime(planDate);
            spanSend.setSendPerson(spanSend.getCreateBy()); //设置发送人
            glueSpanSendService.save(spanSend);
            sendIdList.add(spanSend.getId());

            //保存接受记录
            GlueSpanReceive spanReceive = new GlueSpanReceive();
            BeanUtils.copyProperties(spanSend, spanReceive);
            spanReceive.setSendId(spanReceive.getId());
            spanReceive.setId(null);
            glueSpanReceiveService.save(spanReceive);

            if(StringUtils.isBlank(spanSend.getDecomposeMachineCode())) {
                retryReceiveIdList.add(spanReceive.getId());
            }
        }
        glueDecomposePlanMapper.matchGlueReceiveMachine(planDate, sendIdList);  //批量设置分解后跨区接收表的默认机台

        if(!retryReceiveIdList.isEmpty()) {
            //如果委托方分解胶料计划的机台为空，则说明他没办法计算出生产量。此时需要通过被委托方机台信息，来重新计算跨区发送和接收的“生产量”
            GlueSendReceiveVo glueSendReceiveVo =decomposeEngineService.retrySpanProductQty(planDate, mixArea, retryReceiveIdList);
            if(glueSendReceiveVo != null) {
                glueSpanSendService.updateBatchById(glueSendReceiveVo.getSendList());
                glueSpanReceiveService.updateBatchById(glueSendReceiveVo.getReceiveList());
                this.updateBatchById(glueSendReceiveVo.getGlueDecomposePlanList());
            }
        }
    }

    /**
     * 根据选中的ids查询跨区发送时要携带的字段
     * @param ids 选中的id
     * @return 查询结果
     */
    @Override
    public List<GlueDecomposePlan> selectSpanSendNeedFieldByIds(Long[] ids) {
        return glueDecomposePlanMapper.selectSpanSendNeedFieldByIds(ids);
    }
    

    /**
     * 计算跨区请求发送量
     * @param dto 要计算的跨区请求
     * @return 查询结果
     */
    @Override
	public List<GlueSpanReceive> caculateGlueSpanSendQty(GlueSpanReceiveDto dto) {
		List<GlueSpanReceive> receiveList = new ArrayList<>(); // 跨区请求数列表
		String entrustMixArea = dto.getEntrustMixArea(); // 委托密炼区
		String entrustedMixArea = dto.getEntrustedMixArea(); // 被委托密炼区

		// 取出当天该密炼区的所有分解记录
		GlueDecomposePlan queryParam = new GlueDecomposePlan();
		queryParam.setMixArea(entrustMixArea);
		queryParam.setPlanDate(dto.getScheduleDate());
		List<GlueDecomposePlan> allPlanList = glueDecomposePlanMapper.selectGlueDecomposePlanList(queryParam);
		if (CollectionUtil.isEmpty(allPlanList)) {
			return receiveList;
		}

		Map<String, GlueDecomposePlan> allPlanMap = allPlanList.stream()
				.filter(p -> StringUtils.isNotEmpty(p.getGlue()))
				.collect(Collectors.toMap(GlueDecomposePlan::getGlue, Function.identity(), (v1, v2) -> v1)); // 分解记录
		List<Long> scheduleIdList = dto.getScheduleIdList();
		List<GlueDecomposePlan> planList = allPlanMap.values().stream() // 获取委托胶料的分解计划
				.filter(p -> StringUtils.isNotEmpty(p.getUpGlue()) && StringUtils.isEmpty(p.getMachineCode())) // 只保留胶料信息不为空，但是机台信息为空的记录
				.filter(p -> scheduleIdList.contains(p.getId())).collect(Collectors.toList());

		List<GlueAreaMachineVo> areaGlueList = allPlanMap.values().stream()
				.filter(p -> StringUtils.isNotEmpty(p.getMachineCode()) && !p.getMachineCode().contains(",")).map(p -> {
					GlueAreaMachineVo areaGlue = new GlueAreaMachineVo();
					areaGlue.setMixArea(entrustMixArea);
					areaGlue.setGlue(p.getGlue());
					areaGlue.setMachineCode(p.getMachineCode());
					return areaGlue;
				}).collect(Collectors.toList()); // 构建委托密炼区 + 胶料 + 机台绑定关系列表

		Map<String, String> glueMachineMap = machineEngineService.mapGlueMachine(entrustedMixArea); // 被委托区的机台Map
		List<GlueAreaMachineVo> entrustedAreaGlueList = planList.stream()
				.filter(p -> glueMachineMap.get(entrustedMixArea + p.getGlue()) != null).map(p -> {
					String glue = p.getGlue();
					String machineCode = glueMachineMap.get(entrustedMixArea + glue);
					GlueAreaMachineVo areaGlue = new GlueAreaMachineVo();
					areaGlue.setMixArea(entrustedMixArea);
					areaGlue.setGlue(glue);
					areaGlue.setMachineCode(machineCode);
					p.setMachineCode(machineCode); // 同时把被委托区的机台设定到分解记录中
					p.setPlanQty(0D);
					p.setProduceQty(0D);
					return areaGlue;
				}).collect(Collectors.toList()); // 构建被委托密炼区 + 胶料 + 机台绑定关系列表
		areaGlueList.addAll(entrustedAreaGlueList);
		Map<String, Double> glueWeightMap = recipeEngineService.mapGlueWeight(areaGlueList, null); /* 胶料单车总重Map */

		int successCount;
		do {
			successCount = 0;
			// 通过上级胶料计划取出
			for (GlueDecomposePlan plan : planList) {
				BigDecimal produceQty = BigDecimalUtil.valueOf(plan.getProduceQty()); // 生产数
				if (produceQty.compareTo(BigDecimal.ZERO) > 0) { // 已经有生产数的不需要处理
					continue;
				}
				// 如果没有生产数，则需要计算
				String upGlue = plan.getUpGlue();
				String glue = plan.getGlue();
				GlueDecomposePlan upGluePlan = allPlanMap.get(upGlue); // 取出上级胶料分解计划
				if (upGluePlan == null) {
					continue;
				}
				BigDecimal upWeight = BigDecimalUtil.valueOf(glueWeightMap.get(upGlue + upGluePlan.getMachineCode())); // 上级胶单车总重
				BigDecimal weight = BigDecimalUtil.valueOfZero(glueWeightMap.get(glue + plan.getMachineCode())); // 本段胶单车总重
				if (upWeight == null || weight.compareTo(BigDecimal.ZERO) == 0) { // 数据不完整，忽略
					continue;
				}
				BigDecimal upProduceQty = BigDecimalUtil.valueOfZero(upGluePlan.getProduceQty()); // 上级胶料生产数
				if (upProduceQty.compareTo(BigDecimal.ZERO) <= 0) { // 没有上级胶料，忽略
					continue;
				}

				// 需求数 = 上级胶料生产数 * 上级胶料单车总重 / 本段胶单车总重，结果向上取整
				BigDecimal planQty = upProduceQty.multiply(upWeight).divide(weight, 0, RoundingMode.UP);
				BigDecimal stockQty = BigDecimalUtil.valueOfZero(plan.getStockQty()); // 库存数
				BigDecimal safeStockQty = BigDecimalUtil.valueOfZero(plan.getSafeStockQty()); // 安全库存数
				produceQty = planQty.add(safeStockQty).subtract(stockQty); // 生产数 =（需求数 + 安全库存数） - 库存数
				produceQty = BigDecimalUtil.greatest(produceQty, BigDecimal.ZERO);
				plan.setProduceQty(produceQty.doubleValue());

				if (produceQty.compareTo(BigDecimal.ZERO) > 0) {
					GlueSpanReceive receive = new GlueSpanReceive();
					receive.setSendQty(produceQty.longValue());
					receive.setScheduleId(plan.getId());
					receiveList.add(receive);
					successCount++; // 只有生产数大于0的情况下成功计数 + 1
				}
			}
		} while (successCount > 0); // 当有计算的情况下，需要往回重新遍历

		// 遍历分解列表，无法计算出生产量的胶料统一设置为0
		for (GlueDecomposePlan plan : planList) {
			if (receiveList.stream().noneMatch(r -> plan.getId().compareTo(r.getScheduleId()) == 0)) {
				GlueSpanReceive receive = new GlueSpanReceive();
				receive.setSendQty(0L);
				receive.setScheduleId(plan.getId());
				receiveList.add(receive);
			}
		}
		return receiveList;
	}
}
