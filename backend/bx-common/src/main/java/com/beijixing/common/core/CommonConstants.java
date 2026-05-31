/**
 * 北极星AI商机获客系统 - 开发规范通知
 * 
 * ⚠️ 重要提醒：所有开发必须严格按照设计文档执行！
 * 
 * 【强制要求】
 * 1. 代码目录结构必须符合 docs/inbox/AI-shangji/文件目录结构.md.docx 定义
 * 2. API设计必须符合 docs/inbox/AI-shangji/API接口设计.md.docx
 * 3. 数据库表结构必须符合 docs/inbox/AI-shangji/数据库设计.md.docx
 * 4. 后端服务命名必须符合规范：bx-{模块名}
 * 5. Java包名必须是：com.beijixing.{模块名}
 * 
 * 【目录结构规范】
 * src/main/java/com/beijixing/{模块名}/
 *   ├── config/          # 配置类
 *   ├── controller/      # 控制器
 *   ├── service/         # 服务层
 *   │   └── impl/        # 实现类
 *   ├── repository/      # 数据访问层
 *   ├── entity/          # 实体类
 *   ├── vo/              # 响应VO
 *   ├── dto/             # 请求DTO
 *   ├── enums/           # 枚举类
 *   ├── exception/       # 异常类
 *   ├── util/            # 工具类
 *   ├── aspect/          # 切面
 *   └── interceptor/     # 拦截器
 * 
 * 【如无法按文档执行】
 * 必须立即请示并说明原因，获得批准后方可调整！
 * 
 * 【项目根目录】
 * /workspace/projects/beijixing-ai/
 * 
 * 【文档位置】
 * /workspace/projects/workspace/skills/digital-employee-manager/docs/inbox/AI-shangji/
 */

package com.beijixing.common.core;

/**
 * 系统常量
 */
public class CommonConstants {
    
    /** 系统名称 */
    public static final String SYSTEM_NAME = "北极星AI商机获客系统";
    
    /** 系统代号 */
    public static final String SYSTEM_CODE = "BEIJIXING";
    
    /** 版本号 */
    public static final String VERSION = "1.0.0";
    
    /** 租户ID请求头 */
    public static final String TENANT_ID_HEADER = "X-Tenant-Id";
    
    /** 请求ID */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";
    
    /** Token请求头 */
    public static final String AUTHORIZATION_HEADER = "Authorization";
    
}
