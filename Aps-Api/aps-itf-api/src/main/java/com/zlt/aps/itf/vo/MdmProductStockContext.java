package com.zlt.aps.itf.vo;

import com.ruoyi.common.core.utils.DateUtils;
import com.zlt.aps.monthplan.api.domain.entity.MdmProductStock;
import lombok.Data;

import java.io.Serializable;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Chen
 * @since 2026/2/9
 */
@Data
public class MdmProductStockContext implements Serializable {

    int subMonthParam1 = 3, subMonthParam2 = 6, subMonthParam3 = 9, subMonthParam4 = 12;

    /**
     * 分厂
     */
    private String factoryCode;

    /**
     * 产品品类，TBR 全钢 PCR 半钢
     */
    private String productTypeCode;

    /**
     * 库存日期
     */
    private Date stockDate;

    /**
     * 库存日期日历对象
     */
    private Calendar stockDateCalendar;

    /**
     * 库存年份
     */
    private int stockYear;

    /**
     * 库存月份
     */
    private int stockMonth;

    /**
     * 库存日期-subMonthParam1
     */
    private Date subTime1;

    /**
     * 库存日期-subMonthParam2
     */
    private Date subTime2;

    /**
     * 库存日期-subMonthParam3
     */
    private Date subTime3;

    /**
     * 库存日期-subMonthParam4
     */
    private Date subTime4;

    /**
     * 库存日期-系统参数：SYS0101001
     */
    private Date overdueRegularTime;

    /**
     * 库存日期-系统参数：SYS0101002
     */
    private Date overdueCycleTime;

    /**
     * 库存日期-系统参数：SYS0101003
     */
    private Date overdueTireWaringTime;

    /**
     * 是否保存库存列表
     */
    private Boolean isSaveStock;

    public void initContext(MdmProductStock mdmProductStock) {
        Date stockDate = mdmProductStock.getStockDate();
        if (stockDate == null) {
            try {
                stockDate = DateUtils.getNowDate("yyyy-MM-dd");
            } catch (ParseException e) {
                throw new RuntimeException(e);
            }
        }

        this.factoryCode = mdmProductStock.getFactoryCode();
        this.productTypeCode = mdmProductStock.getProductTypeCode();
        this.stockDate = stockDate;

        Calendar stockDateCalendar = Calendar.getInstance();
        stockDateCalendar.setTime(stockDate);
        this.stockYear = stockDateCalendar.get(Calendar.YEAR);
        this.stockMonth = stockDateCalendar.get(Calendar.MONTH) + 1;

        stockDateCalendar.add(Calendar.MONTH, -subMonthParam1);
        this.subTime1 = stockDateCalendar.getTime();

        stockDateCalendar.setTime(stockDate);
        stockDateCalendar.add(Calendar.MONTH, -subMonthParam2);
        this.subTime2 = stockDateCalendar.getTime();

        stockDateCalendar.setTime(stockDate);
        stockDateCalendar.add(Calendar.MONTH, -subMonthParam3);
        this.subTime3 = stockDateCalendar.getTime();

        stockDateCalendar.setTime(stockDate);
        stockDateCalendar.add(Calendar.MONTH, -subMonthParam4);
        this.subTime4 = stockDateCalendar.getTime();

        this.stockDateCalendar = stockDateCalendar;
    }
}
