package com.zlt.aps.cd90.common.config;

import com.baomidou.mybatisplus.extension.incrementer.OracleKeyGenerator;

//@Configuration
//@MapperScan("com.zlt.**.mapper")
public class MybatisPlusConfigurer {

    /**
     * Sequence主键自增
     *
     * @return 返回oracle自增类
     * @author zhenggc
     * @date 2019/1/2
     */
//    @Bean
    public OracleKeyGenerator oracleKeyGenerator() {
        return new OracleKeyGenerator();
    }
}


