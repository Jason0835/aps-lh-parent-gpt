package com.zlt.mix.setting.service.impl;

import com.alibaba.nacos.common.utils.CollectionUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruoyi.api.gateway.system.domain.ImportErrorLog;
import com.ruoyi.common.core.utils.SecurityUtils;
import com.ruoyi.common.core.utils.reflect.ReflectUtils;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.ruoyi.common.i18n.utils.I18nUtil;
import com.zlt.mix.common.core.constant.ZltConstant;
import com.zlt.mix.common.core.utils.ImportUtil;
import com.zlt.mix.setting.api.domain.entity.GlueDecompose;
import com.zlt.mix.setting.mapper.GlueDecomposeMapper;
import com.zlt.mix.setting.service.GlueDecomposeService;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zlt.mix.common.core.utils.ImportUtil.addImportErrorLog;

/**
 * 终炼母炼分解Service业务层处理
 *
 * @author Liam
 * @date 2022-03-28
 */
@Service
public class GlueDecomposeServiceImpl extends ServiceImpl<GlueDecomposeMapper, GlueDecompose> implements GlueDecomposeService {
    @Resource
    private GlueDecomposeMapper glueDecomposeMapper;

    /**
     * 查询终炼母炼分解列表
     *
     * @param glueDecompose 终炼母炼分解
     * @return 终炼母炼分解
     */
    @Override
    public List<GlueDecompose> selectGlueDecomposeList(GlueDecompose glueDecompose) {
        return glueDecomposeMapper.selectGlueDecomposeList(glueDecompose);
    }

    /**
     * 保存终炼母炼分解信息（id为空则新增，id不为空则修改）
     *
     * @param glueDecompose
     */
    @Override
    public void saveGlueDecompose(GlueDecompose glueDecompose) {
        if (ObjectUtils.allNotNull(glueDecompose.getGlue()) && ZltConstant.NOT_UNIQUE.equals(checkGlueDecomposeUnique(glueDecompose))) {
            throw new RuntimeException(I18nUtil.getMessage("setting.decompose.database.unique"));
        }

        //填写母胶
        monthName(glueDecompose);

        glueDecompose.setBaseValue(glueDecompose.getId());
        this.saveOrUpdate(glueDecompose);
    }


    /**
     * 批量删除终炼母炼分解
     *
     * @param ids 需要删除的终炼母炼分解ID
     * @return 结果
     */
    @Override
    public int deleteGlueDecomposeByIds(Long[] ids) {
        return glueDecomposeMapper.deleteGlueDecomposeByIds(ids);
    }


    /**
     * 校验终炼母炼分解唯一性
     */
    @Override
    public String checkGlueDecomposeUnique(GlueDecompose glueDecompose) {
        if (glueDecompose == null) {
            return ZltConstant.NOT_UNIQUE;
        }

        QueryWrapper<GlueDecompose> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("DEL_FLAG", ZltConstant.DEL_FLAG_NORMAL);
        queryWrapper.eq("GLUE", glueDecompose.getGlue());
        if (glueDecompose.getId() != null) {
            queryWrapper.ne("ID", glueDecompose.getId());  //编辑的时候校验，要过滤掉自身的id
        }

        List<GlueDecompose> list = glueDecomposeMapper.selectList(queryWrapper);
        if (list.size() > 0) {
            return ZltConstant.NOT_UNIQUE;
        }
        return ZltConstant.UNIQUE;
    }

    /**
     * 导入终炼母炼分解数据
     *
     * @param list          要导入的数据集合
     * @param updateSupport 已存在记录是否更新
     * @param importLogId   导入日志id
     */
    @Override
    public AjaxResult importData(List<GlueDecompose> list, boolean updateSupport, Long importLogId) {
        //初始化
        int successNum = 0;
        int failureNum = 0;
        List<GlueDecompose> importList = new ArrayList<>();   //各种校验通过后的导入数据列表（最终可以导入数据库的计划）
        List<ImportErrorLog> importErrorLogs = new ArrayList<>();  //导入错误明显列表
        List<ImportErrorLog> codeUniqueErrorLogs = new ArrayList<>();  //违反数据库唯一键的错误列表
        Map<Integer, Long> codeUniqueErrorMap = new HashMap<>();  //用来存储哪一行数据违反了数据库唯一键

        try {
            if (!updateSupport && CollectionUtils.isNotEmpty(list)) {
                //没有勾选更新记录，需要唯一键校验导入的数据在系统中是否已经存在
                codeUniqueErrorLogs = this.glueDecomposeMapper.listGlueDecomposeNotUnique(list, importLogId, I18nUtil.getMessage("setting.decompose.database.unique"), SecurityUtils.getUsername());
                importErrorLogs.addAll(codeUniqueErrorLogs);
                codeUniqueErrorMap = codeUniqueErrorLogs.stream().collect(Collectors.groupingBy(a -> a.getErrorRow(), Collectors.counting()));
            }

            //按业务主键分组（用来排除导入的excel中哪些数据违反了唯一键约束）
            Map<String, Long> groupMap = list.stream().collect(Collectors.groupingBy(a -> a.getGlue(), Collectors.counting()));

            //公共校验（非空校验、长度校验等）
            for (int i = 0; i < list.size(); i++) {
                GlueDecompose glueDecompose = list.get(i);
                //exce中重复记录校验
                Long hasValue = groupMap.get(glueDecompose.getGlue());
                if (hasValue > 1) {
                    //导入的excel中的数据违反了唯一键约束
                    glueDecompose.setId(-999L);   //校验没通过的记录，设置id为-999作为标记
                    String message = I18nUtil.getMessage("setting.decompose.excel.unique");
                    addImportErrorLog(importLogId, i + 2, message, importErrorLogs);
                }

                //违反数据库唯一键的记录
                if (codeUniqueErrorMap.containsKey(i + 2)) {
                    //数据已经系统中存在
                    glueDecompose.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                }

                List<ImportErrorLog> validated = ImportUtil.validated(importLogId, i + 2, glueDecompose); //校验excel每个单元格长度、类型等

                if (CollectionUtils.isEmpty(validated) && glueDecompose.getId() == null) {

                    //填写母胶
                    monthName(glueDecompose);

                    glueDecompose.setBaseValue(null);
                    importList.add(glueDecompose);

                } else {
                    glueDecompose.setId(-999L);  //校验没通过的记录，设置id为-999作为标记
                    importErrorLogs.addAll(validated);
                }
            }

            //勾选更新记录，调用merge即可
            if (updateSupport && CollectionUtils.isNotEmpty(importList)) {
                glueDecomposeMapper.mergeSql(importList);  //根据唯一键批量新增或修改
            } else if (!updateSupport && CollectionUtils.isNotEmpty(importList)) {
                glueDecomposeMapper.batchInsertGlueDecomposeInfo(importList);  //批量插入
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


    /**
     * 自动拼写未填写的母炼胶名
     * 需要保证胶料名称和段数是存在的
     *
     * @param glueDecompose 终炼母炼分解信息
     */
    public void monthName(GlueDecompose glueDecompose) {
        int segment = glueDecompose.getSegment();
        for (int i = 1; i < segment; i++) {

            //获取母炼胶名
            String name = "motherGlue" + i;
            String motherGlue = ReflectUtils.invokeGetter(glueDecompose, name);

            if (StringUtils.isEmpty(motherGlue)) {
                StringBuilder str = new StringBuilder(glueDecompose.getGlue());
                str.append("/").append(segment).append(i);

                //设置母炼胶名
                ReflectUtils.invokeSetter(glueDecompose, name, str.toString());
            }
        }
    }
}
