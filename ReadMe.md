# 1.说明
&emsp;&emsp;hive-quota-plugin-1.0.0.jar插件通过Hive MetaStore的DDL语句的事件监听器实现了对数据库配额Quota的控制。
# 2.编译打包
```bash
mvn clean package
```
插件jar包路径
```log
target/hive-quota-plugin-1.0.0.jar
```
# 3. 插件上传
&emsp;&emsp;将hive-quota-plugin插件上传至Hive MetaStore的lib路径下
```bash
scp -r target/hive-quota-plugin-1.0.0.jar root@<ip address>:${HIVE_HOME}/lib/
```

# 4. 配置监听器
&emsp;&emsp;通过ambari-manager-ui进行配置如下参数
```properties
hive.metastore.pre.event.listeners=org.apache.hadoop.hive.ql.security.authorization.AuthorizationPreEventListener,org.apache.hadoop.hive.plugin.quota.QuotaMetaStorePreEventListener
hive.metastore.event.listeners=org.apache.hadoop.hive.plugin.quota.QuotaMetaStoreEventListener
```

# 5. 重启Hive MetaStore服务

# 6. DDL使用方法：
```sql
create database financial_02 with dbproperties('table.quota'='50', 'name.quota'='1000', 'space.quota'='10240');
alter database financial_02 set dbproperties('space.quota'='10240');

use financial_02;
create table employees(
  name string comment '员工名字',
  salary float comment '薪资',
  sub_ordinates array<string> comment '组员名字',
  deductions map<string, float> comment '保险、考勤等扣款',
  address struct<street:string, city:string, state:string, zip:int> comment '员工住址'
)comment '员工薪资表'
location '/user/hive/warehouse/finanical.db/employees'
tblproperties ('space.quota'='10240', 'name.quota'='1000');

``` 
* **table.quota:** 是指当前数据库中的表的数量，表包括默认表(MANAGED_TABLE)，外部表(EXTERNAL_TABLE),视图(VIRTUAL_VIEW)，物化视图(MATERIALIZED_VIEW)。  
* **name.quota:** 与HDFS Path的nameQuota定义一致。  
* **space.quota:** 与HDFS Path的spaceQuota定义一致。  