1.0Beta
项目构成
推荐使用jdk24

所有模块均依赖于common
common包含了
redis
es
pgsql
lombok
mybatis-plus
springboot-web
actuator
根模块继承于springboot-stater，版本为3.2.6
特殊说明：子模块导入common，必须要单独声明es和redis才会导入，不会强行依赖

本项目依赖于redis，pgsql，es实现
该项目可以独立运行为单体架构，也可以通过部署在k8s集群里直接拆分成微服务

踩坑记录
1未设置lombok编译器，导致setter等失效，并且因为还写业务，只有config用上了setter导致配置注入失效，害我以为是配置文件的问题
2common模块的application文件名冲突，打包后会被覆盖，导致common模块的配置全部失效，解决方法，我是直接改的后缀，我感觉这种方法是真优雅
3logstash日志异常，始终无法把日志记录在es里，通过各种链路分析，最后发现，原来是端口映射错了，helm安装的端口是5044默认端口，配置文件里是却是其他的，导致端口没有被监听