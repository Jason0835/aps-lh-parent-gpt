package com.zlt.aps.lh.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ruoyi.common.core.web.domain.AjaxResult;
import com.zlt.aps.lh.api.domain.entity.Gante;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResultDto;
import com.zlt.aps.lh.entity.LhScheduleResult;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;


/**
 * 硫化排程结果Service接口
 *
 * @author chen
 * @date 2021-07-19
 */
public interface LhScheduleResultService extends IService<LhScheduleResult> {
    /**
     * 查询硫化排程结果
     *
     * @param id 硫化排程结果ID
     * @return 硫化排程结果
     */
    public LhScheduleResultDto selectLhScheduleResultById(Long id);

    /**
     * 查询硫化排程结果列表
     *
     * @param lhScheduleResult 硫化排程结果
     * @return 硫化排程结果集合
     */
    public List<LhScheduleResultDto> selectLhScheduleResultList(LhScheduleResult lhScheduleResult);

    /**
     * 新增硫化排程结果
     *
     * @param lhScheduleResult 硫化排程结果
     * @return 结果
     */
    public int insertLhScheduleResult(LhScheduleResult lhScheduleResult);

    /**
     * 修改硫化排程结果
     *
     * @param lhScheduleResult 硫化排程结果
     * @return 结果
     */
    public int updateLhScheduleResult(LhScheduleResult lhScheduleResult);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量
     * @param newSchedule
     */
    void insetDispatcherLog(String operType, LhScheduleResultDto oldSchedule, LhScheduleResultDto newSchedule);

    /**
     * 判断是否是“调度员”，如果调度员，则需要需要记录操作日志
     * @param operType 操作类型：0--转机台、1--调量、2--插单
     */
    void insetDispatcherLogInsertOrder(String operType, List<LhScheduleResultDto> scheduleResults, LhScheduleResultDto newSchedule);

    /**
     * 根据排程日期和代码查询排程结果
     * @param scheduleResult 排程日期、代码
     * @return 查询到的数据
     */
    List<LhScheduleResultDto> selectByScheduleDateAndCode(LhScheduleResultDto scheduleResult);

    /**
     * 批量删除硫化排程结果
     *
     * @param ids 需要删除的硫化排程结果ID
     */
    public void deleteLhScheduleResultByIds(long[] ids);

    /**
     * 删除硫化排程结果信息
     *
     * @param id 硫化排程结果ID
     * @return 结果
     */
    public int deleteLhScheduleResultById(Long id);

    /**
     * 校验硫化排程结果唯一性
     */
    public String checkLhScheduleResultUnique(LhScheduleResult lhScheduleResult);

    /**
     * 导出excel表格
     *
     * @param list 要导出的数据集合
     * @return 字节数组
     */
    public byte[] export(List<LhScheduleResultDto> list);

    /**
     * 排程发布
     * @param scheduleDate  排程日期
     * @param dataVersion 接口数据版本
     * @param factoryCode 分厂代号
     * @param companyCode  分公司代号
     */
    public AjaxResult publish(long[] ids,Date scheduleDate,String dataVersion,String factoryCode,String companyCode);
    
	/**
	 * 更新指定相关数据记录的发布状态
	 * 
	 * @param dataVersion 数据版本
	 * @param ids         排程ID列表
	 * @param status      更新的状态
	 */
	void updateRelaseStatus(String dataVersion, long[] ids, String status);

    /**
     * 导入数据，并保存记录
     *
     * @param list          要导入数据
     * @param updateSupport 已存在是否更新
     * @param importLogId   导入日志id
     * @return 导入后提示信息
     */
    AjaxResult importData(List<LhScheduleResultDto> list, Long importLogId, Date scheduleDate);

    /**
     * 查询排程日期是否已发布
     * @param scheduleDate 排程日期
     * @return 是否已经发布
     */
    Boolean isPublish(Date scheduleDate);

    /**
     * 根据排程日期查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param scheduleDate 排程日期
     * @return 查询到的记录数
     */
    public int isReleasingOrTimeoutByDate(Date scheduleDate);

    /**
     * 根据id查询当前日期发布状态为"发布中"或"超时失败"的记录
     * @param ids id
     * @return 查询到的记录数
     */
    public int isReleasingOrTimeoutByIds(long[] ids);

    /**
     * 更改发布状态
     * @param scheduleDate 排程日期
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    public int changeReleaseStatus(LhScheduleResult entity);

    int isPublishByIds(long[] ids);

    /**
     * 查询排程机台甘特图数据
     */
    public List<Gante> getLhGanteData(Gante gante);

}
