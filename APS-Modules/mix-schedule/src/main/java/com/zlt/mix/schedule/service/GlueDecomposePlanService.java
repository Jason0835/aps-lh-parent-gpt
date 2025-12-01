package com.zlt.mix.schedule.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.mix.schedule.api.domain.dto.GlueDecomposePlanExportDictDto;
import com.zlt.mix.schedule.api.domain.dto.GlueSpanReceiveDto;
import com.zlt.mix.schedule.api.domain.dto.GlueSpanSendDto;
import com.zlt.mix.schedule.api.domain.entity.GlueDecomposePlan;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanReceive;
import com.zlt.mix.schedule.api.domain.entity.GlueSpanSend;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.util.Date;
import java.util.List;

/**
 * 分解胶料需求量Service接口
 *
 * @author chen
 * @date 2022-05-04
 */
public interface GlueDecomposePlanService extends IService<GlueDecomposePlan> {
    /**
     * 查询分解胶料需求量列表
     *
     * @param glueDecomposePlan 分解胶料需求量
     * @return 分解胶料需求量集合
     */
    List<GlueDecomposePlan> selectGlueDecomposePlanList(GlueDecomposePlan glueDecomposePlan);

    /**
     * 保存分解胶料需求量信息（id为空则新增，id不为空则修改）
     *
     * @param glueDecomposePlan
     */
    List<GlueDecomposePlan> saveGlueDecomposePlan(GlueDecomposePlan glueDecomposePlan);

    /**
     * 批量删除分解胶料需求量
     *
     * @param ids 需要删除的分解胶料需求量ID
     * @return 结果
     */
    int deleteGlueDecomposePlanByIds(Long[] ids);

    /**
     * 校验分解胶料需求量唯一性
     */
    String checkGlueDecomposePlanUnique(GlueDecomposePlan glueDecomposePlan);

    /**
     * 更新安全库存
     * @param glueDecomposePlan 要更新的数据
     * @return 结果
     */
    List<GlueDecomposePlan> updateSafeStock(GlueDecomposePlan glueDecomposePlan);

    /**
     * 分解计划进行验证机台信息
     * @param glueDecomposePlan
     * @return
     */
    String validateMachineData(GlueDecomposePlan glueDecomposePlan);

    /**
     * 根据模板文件导出到Excel
     *
     * @param dto 参数
     * @return Excel字节数组
     */
    byte[] exportData(GlueDecomposePlanExportDictDto dto);

    /**
     * 检测对应日期和密炼区的数据是否存在
     *
     * @param glueDecomposePlan 时间和密炼区
     * @return 是否唯一的常量值
     */
    String checkPlanDateAndMixAreaExist(GlueDecomposePlan glueDecomposePlan);

    /**
     * 检测对应日期和密炼区的数据是否存在没有选择机台的记录
     *
     * @param glueDecomposePlan 时间和密炼区
     * @return 是否唯一的常量值
     */
    String checkMachineError(GlueDecomposePlan glueDecomposePlan);

    /**
     * 根据条件查询分解胶料需求量跨区发送列表
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
     * 根据条件查询分解胶料需求量跨区接收列表
     * @param entity 查询条件
     * @return 结果
     */
    List<GlueSpanReceive> listGlueSpanReceive(GlueSpanReceive entity);

    /**
     * 根据id查询跨区接收信息
     * @param entity id
     * @return 查询到的记录
     */
    public GlueSpanReceive getGlueSpanReceiveInfo(GlueSpanReceive entity);

    /**
     * 接收跨区请求
     * @param dto 要接收的跨区请求
     * @return 结果
     */
    @Transactional
    AjaxResult receiveGlueSpanReceive(GlueSpanReceiveDto dto);

    /**
     * 删除发送的跨区请求
     * @return 结果
     */
    @Transactional
    AjaxResult deleteGlueSpanSend(Long[] ids);

    /**
     * 分解胶料计划后，在根据胶料跨区设置表，自动胶料发送跨区记录
     * @param planDate
     * @param mixArea
     */
    void autoCreateSpanSend(Date planDate, String mixArea);

    /**
     * 根据选中的ids查询跨区发送时要携带的字段
     * @param ids 选中的id
     * @return 查询结果
     */
    List<GlueDecomposePlan> selectSpanSendNeedFieldByIds(Long[] ids);
    
    /**
     * 
     * @param dto
     * @return
     */
    List<GlueSpanReceive> caculateGlueSpanSendQty(GlueSpanReceiveDto dto);
}
