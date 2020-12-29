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
&emsp;&emsp;hive-site.xml添加配置如下参数
```properties
hive.metastore.pre.event.listeners=org.apache.hadoop.hive.ql.security.authorization.AuthorizationPreEventListener,org.apache.hadoop.hive.plugin.quota.QuotaMetaStorePreEventListener
hive.metastore.event.listeners=org.apache.hadoop.hive.plugin.quota.QuotaMetaStoreEventListener
```
# 5. 将hive加入到hdfs的superusergroup

对Hive Database的空间配额(spaceQuota)的设置，实质上是利用HDFS将其在HDFS上的存储路径设置配额限制。设置HDFS的路径的Quota需要当前用户(hive)在超级用户组(supergroup)中，超级用户组默认为supergroup
其配置项在hdfs-site.xml中:
```xml
<property>
    <name>dfs.permissions.superusergroup</name>
    <value>hdfs</value>
</property>
```
因此需要将hive用户加到hdfs用户组中,
```bash
#如将用户hive增加到supergroup中，再执行：
usermod -a -G hdfs hive
#同步系统的权限信息到HDFS：
su - hdfs -s /bin/bash -c "hdfs dfsadmin -refreshUserToGroupsMappings"

```
详细操作步骤如下：

1. 在Hive Metastore安装节点上执行如下命令：

		[root@master keytabs]# hdfs dfsadmin -report
		Configured Capacity: 92359540224 (86.02 GB)
		Present Capacity: 70267350594 (65.44 GB)
		DFS Remaining: 66347736705 (61.79 GB)
		DFS Used: 3919613889 (3.65 GB)
		DFS Used%: 5.58%
		Replicated Blocks:
		Under replicated blocks: 972
		Blocks with corrupt replicas: 0
		Missing blocks: 0
		Missing blocks (with replication factor 1): 0
		Low redundancy blocks with highest priority to recover: 972
		Pending deletion blocks: 0
		Erasure Coded Block Groups: 
		Low redundancy block groups: 0
		Block groups with corrupt internal blocks: 0
		Missing block groups: 0
		Low redundancy blocks with highest priority to recover: 0
		Pending deletion blocks: 0

		-------------------------------------------------
		report: Access denied for user hive. Superuser privilege is required
    并会出现上面的提示信息。


2. 将hive添加到hdfs超级用户组中，并确认增加增加成功（各个节点都执行）：

		[root@master var]# usermod -a -G hdfs hive
		[root@master var]# id hive
		uid=1187(hive) gid=989(hive) groups=989(hive),1001(hdfs),1199(supergroup)
		[root@master var]# grep hdfs /etc/group
		hdfs:x:1001:hdfs,hive
		hadoop:x:1002:yarn-ats,infra-solr,streamline,logsearch,registry,ambari-qa,hdfs,zookeeper,hbase,yarn,mapred,ranger
		supergroup:x:1199:hive,hdfs


3. 增加的信息同步到HDFS，注意这里需要使用hdfs的票据在管理节点（NameNode所在节点）来操作：

		[root@manager keytabs]# kinit -kt hdfs.headless.keytab hdfs-dcenter@BIGDATA
		[root@manager keytabs]# klist
		Ticket cache: FILE:/tmp/krb5cc_0
		Default principal: hdfs-dcenter@BIGDATA
	
		Valid starting       Expires              Service principal
		11/05/2019 09:01:34  11/06/2019 09:01:34  krbtgt/BIGDATA@BIGDATA
		renew until 11/12/2019 09:01:34
		[root@manager keytabs]# hdfs dfsadmin -refreshUserToGroupsMappings
		Refresh user to groups mapping successful
4. 切换成hive用户进行验证：

		[root@manager keytabs]# kinit -kt hive.service.keytab  hive/manager.bigdata.com@BIGDATA
		[root@manager keytabs]# hdfs dfsadmin -report
		Configured Capacity: 92359540224 (86.02 GB)
		Present Capacity: 70272813153 (65.45 GB)
		DFS Remaining: 66339854409 (61.78 GB)
		DFS Used: 3932958744 (3.66 GB)
		DFS Used%: 5.60%
		Replicated Blocks:
		Under replicated blocks: 983
		Blocks with corrupt replicas: 0
		Missing blocks: 0
		Missing blocks (with replication factor 1): 0
		Low redundancy blocks with highest priority to recover: 983
		Pending deletion blocks: 0
		Erasure Coded Block Groups: 
		Low redundancy block groups: 0
		Block groups with corrupt internal blocks: 0
		Missing block groups: 0
		Low redundancy blocks with highest priority to recover: 0
		Pending deletion blocks: 0
	
		-------------------------------------------------
		Live datanodes (1):
	
		Name: 10.221.129.12:1019 (worker.bigdata.com)
		Hostname: worker.bigdata.com
		Decommission Status : Normal
		Configured Capacity: 92359540224 (86.02 GB)
		DFS Used: 3932958744 (3.66 GB)
		Non DFS Used: 16943099368 (15.78 GB)
		DFS Remaining: 66339854409 (61.78 GB)
		DFS Used%: 4.26%
		DFS Remaining%: 71.83%
		Configured Cache Capacity: 0 (0 B)
		Cache Used: 0 (0 B)
		Cache Remaining: 0 (0 B)
		Cache Used%: 100.00%
		Cache Remaining%: 0.00%
		Xceivers: 8
		Last contact: Tue Nov 05 09:04:07 CST 2019
		Last Block Report: Tue Nov 05 08:57:55 CST 2019
		Num of Blocks: 983
    至此没有报错，说明hive成功加入hdfs的超级用户组。
    
# 6. 重启Hive MetaStore服务

# 7. DDL使用方法：
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
tblproperties ('space.quota'='10240', 'name.quota'='1000');

``` 
* **table.quota:** 是指当前数据库中的表的数量，表包括默认表(MANAGED_TABLE)，外部表(EXTERNAL_TABLE),视图(VIRTUAL_VIEW)，物化视图(MATERIALIZED_VIEW)。  
* **name.quota:** 与HDFS Path的nameQuota定义一致。  
* **space.quota:** 与HDFS Path的spaceQuota定义一致。  
