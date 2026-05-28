package com.company.roro.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebMvc;

/**
 * Swagger / Knife4j 接口文档配置类
 *
 * 作用：
 * 1. 自动生成接口文档，方便前后端协作
 * 2. 提供在线调试接口的功能（Knife4j 增强）
 * 3. 导出 OpenAPI 规范的 JSON，供 Apifox/Postman 等工具导入
 *
 * 访问地址：
 * - Knife4j 可视化页面：http://localhost:8080/doc.html
 * - Swagger JSON 数据：http://localhost:8080/v2/api-docs
 *
 * 常用注解（在 Controller 中使用）：
 * - @Api(tags = "品牌管理")：标注在 Controller 类上，用于分组
 * - @ApiOperation("查询所有品牌")：标注在方法上，描述接口作用
 * - @ApiParam("品牌ID")：标注在参数上，描述参数含义
 * - @ApiModel("品牌实体")：标注在实体类上
 * - @ApiModelProperty("品牌名称")：标注在实体字段上
 */
@Configuration
@EnableSwagger2WebMvc  // 启用 Swagger2，兼容 Spring Boot 2.x
public class SwaggerConfig {

    /**
     * 创建 Swagger 的 Docket Bean
     *
     * Docket 是 Swagger 的核心配置对象，用于：
     * - 指定文档类型（SWAGGER_2）
     * - 配置 API 基本信息（标题、描述、版本等）
     * - 指定扫描哪些接口生成文档
     *
     * @return Docket 实例
     */
    @Bean
    public Docket createRestApi() {
        return new Docket(DocumentationType.SWAGGER_2)
                // 设置 API 文档的基本信息
                .apiInfo(apiInfo())
                // 选择哪些接口生成文档
                .select()
                // 指定扫描的包路径：只扫描 controller 包下的接口
                // 这样可以避免把 Spring Boot 内置的接口也生成进去
                .apis(RequestHandlerSelectors.basePackage("com.company.roro.controller"))
                // 扫描所有路径（any()），也可以指定特定路径（如 ant("/api/**")）
                .paths(PathSelectors.any())
                .build();
    }

    /**
     * 配置 API 文档的基本信息
     *
     * 这些信息会显示在文档页面的顶部，包括：
     * - 标题
     * - 描述
     * - 版本
     * - 联系人信息
     * - 服务条款 URL
     * - 许可证信息
     *
     * @return ApiInfo 对象
     */
    private ApiInfo apiInfo() {
        return new ApiInfoBuilder()
                // 文档标题（显示在页面顶部）
                .title("滚装船在途监控系统 API")

                // 文档描述（说明这个 API 的用途）
                .description("提供在途车辆监控、Excel 批量上传、图表数据查询等接口")

                // 联系人信息（谁负责这个 API）
                .contact(new Contact(
                        "冯伟",                      // 联系人姓名
                        "安吉远海",     // 团队/公司官网
                        "fengwei@anji-coscoshipping.com"          // 联系邮箱
                ))

                // API 版本号（建议与项目版本保持一致）
                .version("1.0.0")

                // 服务条款 URL（可选）
                .termsOfServiceUrl("https://your-company.com/terms")

                // 许可证信息（可选）
                .license("Apache 2.0")
                .licenseUrl("https://www.apache.org/licenses/LICENSE-2.0.html")

                .build();
    }
}