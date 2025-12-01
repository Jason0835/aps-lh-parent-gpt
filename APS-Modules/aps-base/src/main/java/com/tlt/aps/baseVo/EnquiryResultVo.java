package com.tlt.aps.baseVo;


import lombok.Data;

import java.math.BigDecimal;

@Data
public class EnquiryResultVo {
    String supportCode;
    BigDecimal price;

    String billNo;
    String rowNo;
}
