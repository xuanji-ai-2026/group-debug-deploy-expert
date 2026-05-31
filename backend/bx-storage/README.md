# bx-storage 存储服务

## 模块说明
文件存储服务，提供腾讯云COS集成、分片上传、CDN加速等功能。

## 目录结构
```
bx-storage/
├── src/main/java/com/beijixing/storage/
│   ├── BxStorageApplication.java
│   ├── config/
│   ├── controller/
│   ├── service/
│   ├── cos/              # COS操作
│   └── util/
├── src/main/resources/
│   └── application.yml
└── pom.xml
```

## 核心功能
- ST-001: 腾讯云COS集成
- ST-002: 分片上传
- ST-003: CDN加速
- ST-004: 文件访问权限控制

## 端口
- 服务端口: 8090
