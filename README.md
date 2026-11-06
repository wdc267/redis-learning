# hm-dianping（黑马点评）

基于 **Spring Boot + MyBatis-Plus + Redis** 的本地生活点评类后端服务，覆盖商铺查询、优惠券秒杀、探店博客、关注、签到、附近商铺等典型业务场景，重点实践 Redis 在实际业务中的落地用法（分布式锁、缓存穿透/雪崩、Lua 脚本、GEO、Set 去重、Stream 异步下单等）。

> 说明：本项目为跟随课程推进的学习型项目，代码按章节逐步实现，当前进度见文末「实现进度」。

## 技术栈

| 分类 | 技术 | 版本 |
| --- | --- | --- |
| 语言 | Java | 1.8 |
| 框架 | Spring Boot | 2.3.12.RELEASE |
| ORM | MyBatis-Plus | 3.4.3 |
| 数据库 | MySQL | 5.1.47 驱动（兼容 5.7 / 8.0） |
| 缓存 | Redis + Lettuce 连接池 | spring-boot-starter-data-redis |
| 工具库 | Hutool | 5.7.17 |
| 简化代码 | Lombok | 随 Boot 版本管理 |
| 构建 | Maven | - |

## 环境要求

- JDK 1.8+
- Maven 3.6+
- MySQL 5.7+（本地 3306 端口）
- Redis 5.0+（本地或虚拟机，默认 6379）
- Nginx（可选，用于托管前端静态页面与上传的图片）

## 快速开始

### 1. 初始化数据库

```bash
# 创建库并导入建表/初始数据脚本
mysql -uroot -p -e "CREATE DATABASE IF NOT EXISTS hmdp DEFAULT CHARSET utf8mb4;"
mysql -uroot -p hmdp < src/main/resources/db/hmdp.sql
```

### 2. 配置本机连接信息

配置分成两个文件，**口令只落在被忽略的那个里**：

| 文件 | 是否入库 | 作用 |
| --- | --- | --- |
| `application.yaml` | ✅ 提交 | 通用配置 + 占位符，不含任何口令 |
| `application-local.yaml` | ❌ 已忽略 | 你本机的真实库/Redis 地址与口令 |
| `application-local.example.yaml` | ✅ 提交 | 上面的模板，供新环境复制 |

首次拉下代码后：

```bash
# Windows
copy src\main\resources\application-local.example.yaml src\main\resources\application-local.yaml
# macOS / Linux
cp src/main/resources/application-local.example.yaml src/main/resources/application-local.yaml
```

然后填入本机值。`application.yaml` 中已设置 `spring.profiles.active: local`，启动时会自动加载
`application-local.yaml` 覆盖同名配置项；**该文件缺失也不会启动失败**，此时使用 `application.yaml`
里的默认值（127.0.0.1、root、空口令）。

如果不想建文件，也可以用环境变量覆盖：

```bash
MYSQL_HOST / MYSQL_PORT / MYSQL_DB / MYSQL_USERNAME / MYSQL_PASSWORD
REDIS_HOST / REDIS_PORT / REDIS_PASSWORD
```

端口默认 `8081`，临时改端口用启动参数 `--server.port=8082`。

### 3. 配置图片上传目录

`com.hmdp.utils.SystemConstants.IMAGE_UPLOAD_DIR` 默认为课程环境路径：

```java
public static final String IMAGE_UPLOAD_DIR = "D:\\lesson\\nginx-1.18.0\\html\\hmdp\\imgs\\";
```

请改成你本机 Nginx 静态资源目录下实际存在的图片目录，否则 `POST /upload/blog` 会失败。

### 4. 启动

```bash
mvn spring-boot:run
# 或运行 com.hmdp.HmDianPingApplication
```

服务默认监听 `http://localhost:8081`。

## 目录结构

```
hm-dianping
├── pom.xml
├── src
│   ├── main
│   │   ├── java/com/hmdp
│   │   │   ├── HmDianPingApplication.java   # 启动类
│   │   │   ├── config/                      # MyBatis-Plus 分页、全局异常处理
│   │   │   ├── controller/                  # REST 接口层
│   │   │   ├── service/                     # 业务接口 + impl 实现
│   │   │   ├── mapper/                      # MyBatis-Plus Mapper
│   │   │   ├── entity/                      # 数据库实体
│   │   │   ├── dto/                         # 传输对象（Result、UserDTO 等）
│   │   │   └── utils/                       # 正则、Redis Key 常量、登录拦截等工具
│   │   └── resources
│   │       ├── application.yaml             # 通用配置（占位符，无口令）
│   │       ├── application-local.yaml       # 本机真实口令（已忽略，不入库）
│   │       ├── application-local.example.yaml # 本机配置模板
│   │       ├── db/hmdp.sql                  # 建表 + 初始数据
│   │       └── mapper/VoucherMapper.xml     # 自定义 SQL
│   └── test/java/com/hmdp                   # 测试
└── target/                                  # 构建产物（不纳入版本控制）
```

## 接口一览

| 模块 | 方法 | 路径 | 说明 |
| --- | --- | --- | --- |
| 用户 | POST | `/user/code` | 发送手机验证码 |
| 用户 | POST | `/user/login` | 验证码登录 / 自动注册 |
| 用户 | POST | `/user/logout` | 退出登录 |
| 用户 | GET | `/user/me` | 获取当前登录用户 |
| 用户 | GET | `/user/info/{id}` | 查看用户详情 |
| 商铺 | GET | `/shop/{id}` | 商铺详情 |
| 商铺 | POST | `/shop` | 新增商铺 |
| 商铺 | PUT | `/shop` | 更新商铺 |
| 商铺 | GET | `/shop/of/type` | 按类型分页查询商铺 |
| 商铺 | GET | `/shop/of/name` | 按名称搜索商铺 |
| 商铺类型 | GET | `/shop-type/list` | 商铺类型列表 |
| 优惠券 | POST | `/voucher` | 新增普通券 |
| 优惠券 | POST | `/voucher/seckill` | 新增秒杀券 |
| 优惠券 | GET | `/voucher/list/{shopId}` | 店铺优惠券列表 |
| 秒杀下单 | POST | `/voucher-order/seckill/{id}` | 抢购秒杀券 |
| 博客 | POST | `/blog` | 发布探店笔记 |
| 博客 | PUT | `/blog/like/{id}` | 点赞 / 取消点赞 |
| 博客 | GET | `/blog/of/me` | 我的笔记 |
| 博客 | GET | `/blog/hot` | 热门笔记 |
| 关注 | - | `/follow/**` | 关注 / 取关 / 共同关注 |
| 图片 | POST | `/upload/blog` | 上传笔记图片 |
| 图片 | GET | `/upload/blog/delete` | 删除笔记图片 |

统一响应体为 `com.hmdp.dto.Result`，包含 `success`、`errorMsg`、`data`、`total` 字段。

## 实现进度

| 模块 | 状态 |
| --- | --- |
| 工程骨架、实体 / Mapper / Controller 分层 | 已完成 |
| 短信验证码发送 + 验证码登录（Session 存验证码与用户） | 已完成 |
| 登录校验拦截器（Redis Token）、商铺缓存、缓存穿透 / 击穿处理 | 待实现 |
| 优惠券秒杀（Lua 校验库存、一人一单、异步下单） | 待实现 |
| 探店博客、点赞排行、关注 Feed 流 | 待实现 |
| 用户签到、附近商铺（GEO） | 待实现 |

## 配置与安全提示

- 仓库中**不含任何真实口令**：`application.yaml` 只有占位符与默认值，真实值放在被忽略的
  `application-local.yaml` 中。
- 请勿为了省事把口令写回 `application.yaml`，也勿提交自己本地的 `application-local.yaml`
  （`.gitignore` 已拦截，如发现 `git status` 里出现它请检查忽略规则）。
- 若不小心已经提交过真实口令，改文件并不能消除历史记录，需要重写提交历史或作废对应口令。

## 声明

本项目源自课程实战案例，仅用于学习与技术验证，不含任何生产环境数据。
