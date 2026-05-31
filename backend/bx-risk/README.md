# bx-risk 风控服务

## 模块说明
风控管理服务，提供规则引擎、评分引擎、策略引擎、反爬虫、反AI检测等功能。

## 目录结构
```
bx-risk/
├── src/main/java/com/beijixing/risk/
│   ├── BxRiskApplication.java
│   ├── config/
│   ├── controller/
│   ├── engine/           # 风控引擎
│   │   ├── RuleEngine.java
│   │   ├── ScoreEngine.java
│   │   ├── StrategyEngine.java
│   │   └── DecisionEngine.java
│   ├── service/
│   ├── repository/
│   ├── entity/
│   └── enums/
├── src/main/resources/
│   └── application.yml
└── pom.xml
```

## 核心功能
- RC-001: 规则引擎
- RC-002: 评分引擎
- RC-003: 策略引擎
- RC-004: 决策引擎
- RC-005~013: 各类风控策略

## 端口
- 服务端口: 8086
