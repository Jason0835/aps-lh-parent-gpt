package com.zlt.aps.lh.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.common.core.domain.SchedulePublishRecord;
import com.zlt.aps.lh.api.domain.entity.Gante;
import com.zlt.aps.lh.api.domain.dto.LhScheduleResultDto;
import com.zlt.aps.lh.entity.LhScheduleResult;
import org.apache.ibatis.annotations.Param;

import java.util.Date;
import java.util.List;

/**
 * 硫化排程结果Mapper接口
 *
 * @author chen
 * @date 2021-07-19
 */
public interface LhScheduleResultMapper extends BaseMapper<LhScheduleResult> {
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
     * 删除硫化排程结果
     *
     * @param id 硫化排程结果ID
     * @return 结果
     */
    public int deleteLhScheduleResultById(Long id);

    /**
     * 批量删除硫化排程结果
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteLhScheduleResultByIds(long[] ids);

    /**
     * 发布指定日期所有排程结果
     *
     * @param scheduleResult 日期条件
     */
    public void publishAll(LhScheduleResult scheduleResult);

    /**
     * 保存发布日志
     *
     * @param schedulePublishRecord 要保存的发布日志
     * @return 结果
     */
    public int insertPublishRecord(SchedulePublishRecord schedulePublishRecord);

	/**
	 * 根据数据版本更新发布日志状态
	 *
	 * @param dataVersion 数据版本
	 * @param status      状态
	 */
	public int updatePublishRecordVersion(@Param("dataVersion") String dataVersion, @Param("status") String status);

    /**
     * 根据排程日期、物料编号、机台id校验唯一性
     * @param scheduleResult 要校验记录
     * @return 查询到的记录数
     */
    public int checkUnique(LhScheduleResult scheduleResult);

    /**
     * 把排程数据发布到中间库
     * @param dataVersion 接口发布版本号
     * @param ids  排程发布的ids
	 * @param factoryCode 厂别
	 * @param companyCode 分公司编号
     */
    public void deployScheduleToMes(@Param("dataVersion") String dataVersion, @Param("ids") long[] ids,
			@Param("factoryCode") String factoryCode, @Param("companyCode") String companyCode);

    /**
     * 查询指定日期的排程结果是否已经发布
     * @param schedulePublishRecord 要查询的日期及工序参数
     * @return 查询到的记录条数
     */
    public int isPublish(SchedulePublishRecord schedulePublishRecord);

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
    public int changeReleaseStatus(LhScheduleResult entity);

    /**
     * 更新发布记录发布状态
     * @param schedulePublishRecord 发布记录
     * @return 影响行数
     */
    public int updatePublishRecord(SchedulePublishRecord schedulePublishRecord);

    public int batchUpdate(@Param("array") long[] ids, @Param("status") String status);

    /**
     * 根据id查询未发布记录的条数
     * @param ids id
     * @return 未发布的记录条数
     */
    public int isPublishByIds(long[] ids);

    /**
     * 根据排程日期、胎胚代码、SAP、施工版本查询记录
     * @return 查询到的记录
     */
    List<LhScheduleResultDto> selectByScheduleDateAndCode(LhScheduleResultDto scheduleResult);

    /**
     * 查询排程机台甘特图数据
     */
    public List<Gante> getLhGanteData(Gante gante);

    /**
     * 查询排程规格甘特图数据
     */
    public List<Gante> getLhSpecGanteData(Gante gante);
}
