package com.beijixing.bxuser.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码哈希生成工具
 *
 * 使用方法:
 *   java -cp ... com.beijixing.bxuser.util.GenerateHash <password>
 *
 * 安全提示:
 *   - 禁止在代码中硬编码密码
 *   - 生产环境密码必须通过命令行参数或环境变量传入
 *   - 标准测试密码: Admin@123
 *
 * @author 北极星AI团队
 */
public class GenerateHash {
    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("错误: 必须提供密码参数");
            System.err.println("用法: java GenerateHash <password>");
            System.err.println("示例: java GenerateHash Admin@123");
            System.exit(1);
        }

        String password = args[0];
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String hash = encoder.encode(password);

        System.out.println("原始密码: " + password);
        System.out.println("BCrypt哈希: " + hash);
        System.out.println("");
        System.out.println("请将此哈希值存储到数据库，不要存储原始密码");
    }
}
