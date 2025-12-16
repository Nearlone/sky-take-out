# 苍穹外卖

from: itheima

author: Nearlone

version: 1.0

## 个人修改

### 员工管理

1. 密码存储：改进过时MD5算法，采用BCryptPasswordEncoder算法
2. 异常处理：改进SQL异常：数据库字段唯一性约束，不同字段会有相应的提醒
3. token校验：获取LocalThread存储的员工ID将在后置拦截器释放



