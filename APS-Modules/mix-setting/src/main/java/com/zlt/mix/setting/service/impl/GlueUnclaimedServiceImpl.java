package com.zlt.mix.setting.service.impl;

import java.util.*;
import java.util.stream.Collectors;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import javax.annotation.Resource;

import com.ruoyi.common.utils.StringUtils;
import org.springframework.stereotype.Service;
import com.alibaba.nacos.common.utils.CollectionUtils;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.setting.mapper.GlueUnclaimedMapper;
import com.zlt.mix.setting.api.domain.entity.GlueUnclaimed;
import com.zlt.mix.setting.service.GlueUnclaimedService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.ruoyi.common.i18n.utils.I18nUtil;
import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 胶料白班待支领Service业务层处理
 *
 * @author zlt
 * @date 2022-09-05
 */
@Service
public class GlueUnclaimedServiceImpl extends ServiceImpl<GlueUnclaimedMapper, GlueUnclaimed> implements GlueUnclaimedService {
    @Resource
    private GlueUnclaimedMapper glueUnclaimedMapper;

    /**
     * 查询胶料白班待支领列表
     *
     * @param glueUnclaimed 胶料白班待支领
     * @return 胶料白班待支领
     */
    @Override
    public List<GlueUnclaimed> selectGlueUnclaimedList(GlueUnclaimed glueUnclaimed) {
        return glueUnclaimedMapper.selectGlueUnclaimedList(glueUnclaimed);
    }

    /**
     * 保存胶料白班待支领信息（id为空则新增，id不为空则修改）
     *
     * @param glueUnclaimed
     */
    @Override
    public void saveGlueUnclaimed(GlueUnclaimed glueUnclaimed) {
        if (ZltConstant.NOT_UNIQUE.equals(checkGlueUnclaimedUnique(glueUnclaimed))) {
            throw new RuntimeException(I18nUtil.getMessage("setting.unclaimed.database.unique" ));
        }
        glueUnclaimed.setBaseValue(glueUnclaimed.getId());
        this.saveOrUpdate(glueUnclaimed);
    }

    /**
     * 批量删除胶料白班待支领
     *
     * @param ids 需要删除的胶料白班待支领ID
     * @return 结果
     */
    @Override
    public int deleteGlueUnclaimedByIds(Long[] ids)
    {
        return glueUnclaimedMapper.deleteGlueUnclaimedByIds(ids);
    }


    /**
     * 校验胶料白班待支领唯一性
     */
    @Override
    public String checkGlueUnclaimedUnique(GlueUnclaimed glueUnclaimed) {
        if (glueUnclaimed == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        QueryWrapper<GlueUnclaimed> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DEL_FLAG", ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq("SCHEDULE_DATE", glueUnclaimed.getScheduleDate());
        queryWrapper.eq("MIX_AREA", glueUnclaimed.getMixArea());
        queryWrapper.eq("GLUE", glueUnclaimed.getGlue());
        if (glueUnclaimed.getId() != null) {
            queryWrapper.ne("ID", glueUnclaimed.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<GlueUnclaimed> list = glueUnclaimedMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 导入胶料白班待支领数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<GlueUnclaimed> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<GlueUnclaimed> importList = new ArrayList<>();   //各种校验通过后的导入数据列表（最终可以导入数据库的计划）
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();  //导入错误明显列表

        try {

            //公共校验（非空校验、长度校验等）
            for (int i = 0; i < list.size(); i++) {
                GlueUnclaimed glueUnclaimed = list.get(i);
                //exce中重复记录校验
//                Long hasValue = groupMap.get(glueUnclaimed.getScheduleDate()  + glueUnclaimed.getMixArea() + glueUnclaimed.getGlue());
//
//                if (hasValue > 1) {
//                    //导入的excel中的数据违反了唯一键约束
//                    glueUnclaimed.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
//                    String message = I18nUtil.getMessage("setting.unclaimed.excel.unique");
//                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
//                }

                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, glueUnclaimed); //校验excel每个单元格长度、类型等

                if (CollectionUtils.isEmpty(validated) && glueUnclaimed.getId() == null) {
                    glueUnclaimed.setBaseValue(null);
                    importList.add(glueUnclaimed);
                } else {
                    glueUnclaimed.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    importErrorLogs.addAll(validated);
                }
            }

            if(!importList.isEmpty()) {
                String mixArea = importList.get(0).getMixArea();
                Date scheduleDate = importList.get(0).getScheduleDate();
                Map<String, List<GlueUnclaimed>> summaryMap = importList.stream().collect(Collectors.groupingBy(GlueUnclaimed::getGlue));
                List<GlueUnclaimed> resultList = new ArrayList<>();
                summaryMap.forEach((k,v)->{
                    Integer shelfNumSum = v.stream().mapToInt(GlueUnclaimed::getShelfNum2).sum();
                    String remarks = v.stream().map(GlueUnclaimed::getRemark).collect(Collectors.joining(","));
                    GlueUnclaimed glueUnclaimed = new GlueUnclaimed(mixArea, scheduleDate, k, shelfNumSum, remarks);
                    resultList.add(glueUnclaimed);
                });
                glueUnclaimedMapper.mergeSql(resultList);  //根据唯一键批量新增或修改

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
        failureNum = list.size() - successNum; //失败记录数
        if (failureNum > 0) {
            return AjaxResult.error(I18nUtil.getMessage("ui.message.import.fail") + "," + successNum + "," + failureNum, importErrorLogs);
        } else {
            return AjaxResult.success(I18nUtil.getMessage("ui.message.import.success") + "," + successNum);
        }
    }
}
