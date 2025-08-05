package com.rich.richcodeweaver.utiles.generator;

import cn.hutool.core.lang.Dict;
import cn.hutool.setting.yaml.YamlUtil;
import com.mybatisflex.codegen.Generator;
import com.mybatisflex.codegen.config.GlobalConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.Map;

/**
 * MyBatisFlex 官方指定代码生成器
 * 源：https://mybatis-flex.com/zh/others/codegen.html
 *
 * @author DuRuiChi
 * @create 2025/8/5
 **/
public class MyBatisFlexCodeGenerator {

    // 定义需要生成代码的数据库表名（可配置多表）
    private static final String[] TABLE_NAMES = {"user"};

    public static void main(String[] args) {
        // 1. 读取YAML配置文件获取数据库配置
        Dict dict = YamlUtil.loadByPath("application.yml");
        // 解析spring.datasource路径下的配置项
        Map<String, Object> dataSourceConfig = dict.getByPath("spring.datasource");

        // 提取数据库连接参数
        String url = String.valueOf(dataSourceConfig.get("url"));
        String username = String.valueOf(dataSourceConfig.get("username"));
        String password = String.valueOf(dataSourceConfig.get("password"));

        // 2. 创建HikariCP数据源（高性能连接池）
        HikariDataSource dataSource = new HikariDataSource();
        dataSource.setJdbcUrl(url);       // 设置JDBC连接地址
        dataSource.setUsername(username); // 设置数据库用户名
        dataSource.setPassword(password); // 设置数据库密码

        // 3. 创建全局代码生成配置
        GlobalConfig globalConfig = createGlobalConfig();

        // 4. 实例化代码生成器（传入数据源和配置）
        Generator generator = new Generator(dataSource, globalConfig);

        // 5. 执行代码生成（生成实体类/Mapper/Service/Controller等）
        generator.generate();
    }

    // 全局配置构建方法（配置参考：https://mybatis-flex.com/zh/others/codegen.html）
    public static GlobalConfig createGlobalConfig() {
        // 初始化全局配置对象
        GlobalConfig globalConfig = new GlobalConfig();

        // 配置基础包路径（生成代码的根包名）
        globalConfig.getPackageConfig()
                .setBasePackage("com.rich.richcodeweaver.generatorTest");

        // 配置表策略
        globalConfig.getStrategyConfig()
                .setGenerateTable(TABLE_NAMES) // 指定生成表（空则生成所有表）
                .setLogicDeleteColumn("isDelete"); // 设置逻辑删除字段名（非必选）

        // 实体类配置
        globalConfig.enableEntity()
                .setWithLombok(true)    // 启用Lombok注解（自动生成getter/setter）
                .setJdkVersion(21);     // 指定JDK版本（影响生成的语法特性）

        // Mapper接口及XML配置
        globalConfig.enableMapper();    // 生成Mapper接口
        globalConfig.enableMapperXml(); // 生成MyBatis映射文件

        // Service层配置
        globalConfig.enableService();    // 生成Service接口
        globalConfig.enableServiceImpl();// 生成ServiceImpl实现类

        // 控制层配置
        globalConfig.enableController(); // 生成Controller类

        // 文档注释配置
        globalConfig.getJavadocConfig()
                .setAuthor("@author DuRuiChi")
                .setSince("");  // 清空@since标签（避免固定版本号影响后续生成）

        return globalConfig;
    }
}