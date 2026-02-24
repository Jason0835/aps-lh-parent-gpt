package com.zlt.aps.maindata.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zlt.aps.maindata.domain.dto.MouldMonthUseDto;
import com.zlt.aps.monthplan.api.domain.entity.MdmMouldUseStatus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 模具可用状态Mapper接口
 *
 * @author leo
 * @date 2021-08-27
 */
public interface MdmMouldUseStatusEntityMapper extends BaseMapper<MdmMouldUseStatus> {

    /**
     * 查询模具可用状态列表
     *
     * @param mdmMouldUseStatus 模具可用状态
     * @return 模具可用状态集合
     */
    List<MdmMouldUseStatus> selectMouldUseStatusList(MdmMouldUseStatus mdmMouldUseStatus);

    /**
     * 查询模具可用状态列表
     *
     * @param mdmMouldUseStatus 模具可用状态
     * @return 模具可用状态集合
     */
    List<MdmMouldUseStatus> selectMouldUseStatusListForProductCode(MdmMouldUseStatus mdmMouldUseStatus);

    List<MdmMouldUseStatus> checkMouldUseStatusUnique(MdmMouldUseStatus mdmMouldUseStatus);

    /**
     * 删除年月底下的模具可用状态数据
     *
     * @param year  年
     * @param month 月
     * @return
     */
    int deleteMouldUseStatusByTime(@Param("companyCode") String companyCode, @Param("factoryCode") String factoryCode, @Param("year") int year, @Param("month") int month);

    /**
     * 根据年，月，分厂编码，模具编码删除模具信息
     *
     * @param mdmMouldUseStatus
     * @return
     */
    int deleteModelUseStatusByMouldCode(MdmMouldUseStatus mdmMouldUseStatus);

    /**
     * 根据物料号查询条数
     */
    int selectCountMouldUseStatusListForProductCode(MdmMouldUseStatus mdmMouldUseStatus);

    /**
     * 查询条数
     */
    int selectCountMouldUseStatusList(MdmMouldUseStatus mdmMouldUseStatus);

    /**
     * 根据分厂，年，月查询模具可用状态
     *
     * @param factoryCode
     * @param year
     * @param month
     * @return
     */
    List<MdmMouldUseStatus> queryByFactoryCodeYearMonth(@Param("factoryCode") String factoryCode,
                                                        @Param("year") int year,
                                                        @Param("month") int month,
                                                        @Param("mouldCodes") List<String> mouldCodes);

    /**
     * 根据分厂，年月，查询特定模具的可用信息
     *
     * @param factoryCode   分厂
     * @param year          年
     * @param month         月
     * @param mouldCodeList 模具编码集合
     * @return
     */
    List<MouldMonthUseDto> getMonthUsedMould(@Param("factoryCode") String factoryCode,
                                             @Param("year") int year,
                                             @Param("month") int month,
                                             @Param("mouldCodeList") List<String> mouldCodeList);
}
