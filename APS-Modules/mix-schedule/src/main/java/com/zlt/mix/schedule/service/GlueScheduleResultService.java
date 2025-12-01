package com.zlt.mix.schedule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.schedule.api.domain.dto.*;
import com.zlt.mix.schedule.api.domain.entity.GlueScheduleResult;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanSend;
import com.zlt.mix.setting.api.domain.entity.MixMachine;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * 终炼/母炼日计划排程Service接口
 *
 * @author chen
 * @date 2022-05-16
 */
public interface GlueScheduleResultService extends IService<GlueScheduleResult> {
    /**
     * 查询终炼/母炼日计划排程列表
     *
     * @param glueScheduleResult 终炼/母炼日计划排程
     * @return 终炼/母炼日计划排程集合
     */
    List<GlueScheduleResult> selectGlueScheduleResultList(GlueScheduleResult glueScheduleResult);

    /**
     * 保存终炼/母炼日计划排程信息（id为空则新增，id不为空则修改）
     *
     * @param glueScheduleResult
     */
    @Transactional(rollbackFor = Exception.class)
    List<GlueScheduleResult> saveGlueScheduleResult(GlueScheduleResult glueScheduleResult);

    /**
     * 批量删除终炼/母炼日计划排程
     *
     * @param ids 需要删除的终炼/母炼日计划排程ID
     * @param isChangeMasterbatch 是否联级修改母炼胶标识
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    int deleteGlueScheduleResultByIds(Long[] ids, Boolean isChangeMasterbatch);

    /**
     * 校验终炼/母炼日计划排程唯一性
     */
    String checkGlueScheduleResultUnique(GlueScheduleResult glueScheduleResult);

    /**
     * 导入终炼/母炼日计划排程数据
     */
    AjaxResult importData(List<GlueScheduleResult> list, Date scheduleDate, String mixArea, Long importLogId);

    /**
     * 发布终炼母炼日计划
     */
    @Transactional(rollbackFor = Exception.class)
    AjaxResult publish(GlueScheduleResult glueScheduleResult);
    
    /**
     * 更新下发状态
     * 
     * @param resultIdList 待下发的排程ID列表
     */
    @Transactional(rollbackFor = Exception.class)
    AjaxResult updateRelaseStatus(Long[] resultIdList, String relaseStatus);

    /**
     * 批量转机台
     */
    @Transactional(rollbackFor = Exception.class)
    AjaxResult batchChangeMachine(String machineCode, Long[] ids);

    /**
     * 转机台
     */
    @Transactional(rollbackFor = Exception.class)
    AjaxResult changeMachine(GlueScheduleResult glueScheduleResult);

    /**
     * 根据参数查询机台信息
     */
    List<MixMachine> getMachineInfo(MixMachine param);

    /**
     * 根据模板文件导出到Excel
     *
     * @param dto 参数
     * @return Excel字节数组
     */
    byte[] exportData(GlueScheduleResultExportDictDto dto);

    /**
     * 检测对应日期和密炼区的数据是否存在
     *
     * @param glueScheduleResult 日期和密炼区
     * @return 是否唯一的常量值
     */
    String checkScheduleDateAndMixAreaExist(GlueScheduleResult glueScheduleResult);

    /**
     * 更改配方信息
     * @param glueScheduleResult id、配方阶段、配方版本号、配方类型
     * @return 结果
     */
    AjaxResult changeRecipe(GlueScheduleResult glueScheduleResult);

    /**
     * 根据id查询排程结果信息
     * @param id id
     * @return 查询到的记录
     */
    public GlueScheduleResult getById(Long id);

    /**
     * 根据ids查询发布状态是否有不是【未发布】的记录
     * @param ids ids
     * @return 不是未发布的记录数
     */
    int isNoReleaseByIds(Long[] ids);

    /**
     * 获取统计信息
     * @param glueScheduleResult 日期、密炼区、机台编号
     * @return 统计好的信息列表
     */
    List<GlueScheduleResultStatisticsDto> statistics(GlueScheduleResult glueScheduleResult);

    /**
     * 根据条件查询终炼母炼日计划跨区发送列表
     * @param entity 查询条件
     * @return 结果
     */
    List<GlueSpanSend> listGlueSpanSend(GlueSpanSend entity);

    /**
     * 发送跨区请求
     * @param dto 跨区请求集合
     * @return 结果
     */
    @Transactional
    AjaxResult sendGlueSpan(GlueSpanSendDto dto) throws ParseException;

    /**
     * 根据条件查询终炼母炼日计划跨区接收列表
     * @param entity 查询条件
     * @return 结果
     */
    List<GlueSpanReceive> listGlueSpanReceive(GlueSpanReceive entity);

    /**
     * 接收跨区请求
     * @param dto 要接收的跨区请求
     * @return 结果
     */
    @Transactional
    AjaxResult receiveGlueSpanReceive(GlueSpanReceiveDto dto);

    /**
     * 根据排程日期、密炼区、机台，查询机台的各班次总计划量
     * @param glueScheduleResult 参数
     * @return 结果
     */
    GlueSpanReceiveQtyDto getSumQtyByMachineCode(GlueScheduleResult glueScheduleResult);

    /**
     * 删除发送的跨区请求
     * @return 结果
     */
    @Transactional
    AjaxResult deleteGlueSpanSend(Long[] ids);

    /**
     * 根据选中的ids查询跨区发送时要携带的字段
     * @param ids 选中的id
     * @return 查询结果
     */
    List<GlueScheduleResult> selectSpanSendNeedFieldByIds(Long[] ids);
    
	/**
	 * 终炼胶母炼胶日计划自动排程
	 * 
	 */
	void autoGlueSchedule(GlueScheduleResult glueScheduleResult);

    /**
     * 获取排程日期的昨日早班合计，夜班合计，早班合计，库存合计，理论交班库存合计
     *
     * @param scheduleResult 排程日期
     * @return 结果
     */
    AjaxResult getSummaryVo(GlueScheduleResult scheduleResult);
}
