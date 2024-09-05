# ZLT前端框架使用指引

##构建项目：
MAVEN的POM通过nexus引用zlt-frame-starter包。\
在SRC目录创建工程包，默认com.zlt会被扫描、注入。\
其它包要注入需要在启动类额外增加注解。 \
启动类增加注解:

    @EnableZLTFrame
    @SpringCloudApplication。

##配置yml:
默认情况下，加载ruoyi-admin包里面的配置文件。\
resources文件夹加入application-Special.yml，修改实际的IP地址等用于部署。\
新建application.yml加入额外配置文件引用。 

已经配置读取nacos的共享配置

    nacos-addr: 192.168.2.93:8848

共享配置包括：

    shared-configs[0]:
      data-id: application-${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}
    shared-configs[1]:
      data-id: system-api-prefix-${spring.profiles.active}.${spring.cloud.nacos.config.file-extension}

system-api-prefix的配置内容：\
api.path.**用来跟后端的服务对应，这个值跟gateway的path对应起来。

如：两个业务系统都要用system模块，但是分别连自己的数据库，这时候代码一样，就配置2套system-api-prefix。\
两套的前端用system.name来区分，一个BootUI前端对一个name对应一个system-api-prefix配置。\
${system.name}auth 对应gateway中的path。\
_remoteApi不是给前端使用的，可以忽略。_

    #系统接口前缀，属于一套系统的前缀统一修改。
    
    system:
      #名称默认为空，ruoyi框架
      name: ''
    #API path 要跟gateway的配置相同
    #使用FeignClient的代码引用。Cloud端根据路由的配置转发
    api:
      path:
        auth: ${system.name}auth
        job: ${system.name}schedule/job
        gen: ${system.name}code/gen 
        system: ${system.name}system
        file: ${system.name}file
        kettle: ${system.name}kettle
    
    #这里是内部互相调用的Feign设定，以下前端不考虑可以删除
    remoteApi:
      value:
        auth: ruoyi-auth
        system: ruoyi-system
        gateway: ruoyi-gateway
        file: ruoyi-file
        kettle: zlt-kettle


切换单点或本机登录的开关：
    
    shiro-sso-enable: true
单机版验证码，会话的网关配置：

    gateway-addr: http://192.168.2.93:8080/
##日志输出：
application.yml增加日志等级

    logging:
      level:
        com.ruoyi: debug
        org.springframework: warn
        com.zlt: debug

##前端静态文件：
在resources文件夹增加META-INF/resoureces文件夹存放静态文件。

    static      js/css/png/...
    templates   html

##语言包：
######后端代码使用的语言包
resources下创建static\i18n文件夹存放新的语言文件。\
在yml增加语言文件路径：

    messages:
        # 国际化资源文件路径
        # 包括common包里面的语言，后端同步，20201202 linbn
        basename: static/i18n/messages,i18n/commonMessages
######前端代码使用的语言包

    resources\META-INF\resources\static\locales
包含2种js文件和引用I18n
1. 框架单独编写的JS代码引用的文件是locale_**.js。\
这个文件需要增加的内容，从ruoyi-admin把locale文件拷出，增加内容后，放在新项目的对应文件夹内。
覆盖掉替换包的文件。这种情况下页面不需要再额外增加前端引用。\
如果是加新的locale-xx_.js文件，要在include.html中增加引用。\
${#locale}是当前系统的语言标识。

        <script th:src="@{'/locales/locale_'+${#locale}+'.js'}"></script>
2. 第三方组件编写的JS代码引用的语言文件。\
文件直接放到文件夹，在include.html中增加引用。\
${#locale}是当前系统的语言标识。\
例如：

	    <!-- jquery-validate 表单树插件 -->
	    <script th:src="@{/ajax/libs/bootstrap-treetable/bootstrap-treetable.js}"></script>
	    <script th:src="@{'/locales/bootstrap-treetable-'+${#locale}+'.js'}"></script>
3. Thymeleaf使用I18n,跟后端代码使用同一个文件，存放在static\i18n

####语言包使用：
######所有js语言包要求压缩成min使用。

使用I18n给前端html,js使用示例：

    [[#{ui.frame.select.select.all}]]       用在标签上引号之外，js内也用这个
    #{ui.frame.btn.name.change}             用在th:修饰的标签上
使用I18n在后端代码使用示例：

    I18nUtil.getMessage("gateway.get.fail") 返回redis字符串，如果没有则是key,如gateway.get.fail
使用locales_*.js的前端代码使用示例：

    "open": {
        name: frame.contextMenu.open,       跟locales文件的json对应
组件多国语言包示例：

    validate_messages_en_US.min.js
    validate_messages_zh_CN.min.js