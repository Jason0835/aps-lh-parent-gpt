## ZLT框架Cloud模块项目指引

### 项目搭建
1. maven引用zlt-module-starter启动包
2. maven添加父POM包

        <groupId>com.ruoyi</groupId>
        <artifactId>ruoyi</artifactId>
        <version>...</version>
3. 添加maven打包配置

        <build>
            <plugins>
                <plugin>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-maven-plugin</artifactId>
                    <executions>
                        <execution>
                            <goals>
                                <goal>repackage</goal>
                            </goals>
                        </execution>
                    </executions>
                </plugin>
            </plugins>
        </build>
4. 加入yml配置\
注意引入nacos配置项和修改项目名称、端口

        server:
          port: 9500        
        nacos:
          server-addr: 192.168.2.93:8848        
        # Spring
        spring:
          application:
            # 应用名称
            name: zlt-kettle
5. 调整共享配置项目\
需要使用的nacos配置加入到yml

        #共享配置
        spring:
            cloud:
                nacos:
                    config:
                        shared-configs[0]:
                          data-id: application-${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}
                        shared-configs[1]:
                          data-id: druid_${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}
                        shared-configs[2]:
                          data-id: system-frame-${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}
                        shared-configs[3]:
                          data-id: system-api-prefix-${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}
                        shared-configs[4]:
                          data-id: zipkin-${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}

6. nacos修改增加配置

        system-frame-${spring.profiles.active}      当前系统代号，跟系统授权一致
        system-api-prefix-${spring.profiles.active} 当前系统的服务前缀，跟前端路由配置要一致
        
7. gateway的nacos路由增加配置

        routes:
          # 代码生成
          - id: ruoyi-gen               跟spring.application.name一致
            uri: lb://ruoyi-gen         跟spring.application.name一致
            predicates:
              - Path=/code/**           跟system-api-prefix-* 配置一致
            filters:
              - StripPrefix=1  
              
8. 添加启动类注解

        @EnableCustomConfig
        @SpringCloudApplication
        @EnableCustomSwagger2
        
9. 添加项目的nacos配置\
添加一个yml,命名为spring.application.name前缀的yml\
在yml,添加需要的配置swagger

        # swagger 配置
        swagger:
          title: Kettle模块接口文档
          license: Powered By telecom
          licenseUrl: https://
          authorization:
            name: telecom
            auth-regex: ^.*$
            authorization-scope-list:
              - scope: server
                description: 客户端授权范围
            token-url-list:
              - http://192.168.100.126:8080/auth/oauth/token

10. 调用外部接口的增加client配置

        webclient:
          url: http://localhost:7080/
          retryTimes: 1
          
11. 需要使用数据库增加mapper别名配置

        mybatis:
          # 搜索指定包别名
          typeAliasesPackage: com.zlt.kettle
          # 配置mapper的扫描，找到所有的mapper.xml映射文件
          mapperLocations: classpath:mapper/**/*.xml

###接口实现
1. 项目内创建com.zlt包下的业务包，编写controller/service/mapper
2. 创建一个子POM项目,定义api,编写com.zlt.*.api下的domain/service\
这个service是FeignClient的定义接口，用来给前端或后端互调使用。
3. 给前端的接口FeignClient定义

        @FeignClient(contextId = "iKettleProxyService", 
            value = ServiceNameConstants.GATEWAY_SERVICE,       前端使用的必须是GATEWAY_SERVICE
            path="${api.path.kettle:kettle}")                   path对应网关的路由配置
        public interface IKettleProxyService {
        
            @PostMapping("/kettle/transLog")                    跟controller一致，完整上下文
            @ApiOperation("转换日志")
            public TableDataInfo getTransLogs(@RequestBody TransRecord transRecord);
        
4. 给后端的接口FeignClient定义

        @FeignClient(contextId = "remoteAuthService",           
            name = "${remoteApi.value.auth}")                   跟spring.application.name一致
        public interface RemoteAuthService {
        
            @PostMapping("appendUserAuth")
            public R<LoginUser> appendUserAuths(@RequestBody AjaxResult auths);
        }

###数据库mybatis脚本
1. resources文件夹下创建文件夹，放mapper.xml文件

        mapper.*.**Mapper.xml
2. 映射关系在typeAliasesPackage有定义，mapper文件可以直接用类名，如

        <resultMap type="JobRecord"     实体类名
            id="JobRecordResult">
###国际化语言
1. 代码用I18n,在resoures下创建i18n文件夹，放入语言文件
2. 在nacos的application配置，增加指定文件

        i18n_msg:
          baseName: i18n/messages,i18n/ValidationMessages
3. 系统字典使用国际化，在 系统配置 里面定义,前端调用时考虑显示，后端都用code\
   如果后端要取出code的label,不能走数据库方法，否则不支持语言包显示。
4. 部门、菜单等使用动态语言包的，使用内部转换方法，转换语言json
        
        String newName = StringUtils.getLocaleName(sysDept.getLangJson(),
            SecurityUtils.getUserLang(), 
            sysDept.getDeptId().toString());
            sysDept.setDeptName(newName);
5. 一般语言包代码调用示例：

        I18nUtil.getMessage("auth.error.login.notexist.token.info")



