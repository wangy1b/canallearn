# mysql canal kafka

## docker: mysql + windows: canal + idea:canal client

### **docker: mysql**

1. 配置docker中的mysql

   ```shell
   ## 开启端口3306 -p 3306:3306
   ## 挂载目录 -v
   ## 设置root密码 -e MYSQL_ROOT_PASSWORD=root
   ## 选择mysql版本 mysql:5.7
   ## 设置container名字 --name mysql_for_canal
   ## 后台运行 -d
   docker run -p 3306:3306 --name mysql_for_canal -v C:\Program\MyDocker\canal_mysql\mysql\data:/var/lib/mysql -v C:\Program\MyDocker\canal_mysql\mysql\conf:/etc/mysql/conf.d -e MYSQL_ROOT_PASSWORD=root -d mysql:5.7
   ```

2. docker container中mysql_for_canal 安装vim，方便后续查看更改配置文件

   ```shell
   cd /etc/apt/
   
   echo "
   # 默认注释了源码镜像以提高 apt update 速度，如有需要可自行取消注释
   deb http://mirrors.aliyun.com/debian/ buster main contrib non-free
   # deb-src http://mirrors.aliyun.com/debian/ buster main contrib non-free
   deb http://mirrors.aliyun.com/debian/ buster-updates main contrib non-free
   # deb-src http://mirrors.aliyun.com/debian/ buster-updates main contrib non-free
   deb http://mirrors.aliyun.com/debian/ buster-backports main contrib non-free
   # deb-src http://mirrors.aliyun.com/debian/ buster-backports main contrib non-free
   deb http://mirrors.aliyun.com/debian-security buster/updates main contrib non-free
   # deb-src http://mirrors.aliyun.com/debian-security buster/updates main contrib non-free
   " > sources.list
   
   apt-get update
   apt-get install vim
   ```

3. 进入mysql 创建canal用户及用来测试的数据库test

   ```sql
   --授权 canal 链接 MySQL 账号具有作为 MySQL slave 的权限, 如果已有账户可直接 grant
   CREATE USER canal IDENTIFIED BY 'canal'； 
   GRANT SELECT, REPLICATION SLAVE, REPLICATION CLIENT ON *.* TO 'canal'@'%'；
   -- GRANT ALL PRIVILEGES ON *.* TO 'canal'@'%' ；
   FLUSH PRIVILEGES；
   
   create database test；
   GRANT ALL PRIVILEGES ON *.* TO 'canal'@'%' ;
   FLUSH PRIVILEGES;
   ```

4. 配置mysql文件，开启binlog

   测试根据挂在的目录下面` C:\Program\MyDocker\canal_mysql\mysql\conf`对应 `/etc/mysql/conf.d`下面my.cnf没有生效，docker中又无法查看当前的配置文件是哪个，经过测试

   ```shell
   root@f858100aa945:/etc/mysql# ls -lth
   total 12K
   -rwxr-xr-x 1 root root 1.2K Apr 23 07:56 mysql.cnf
   drwxr-xr-x 1 root root 4.0K Apr 23 07:52 mysql.conf.d
   drwxrwxrwx 1 root root 4.0K Apr 23 07:09 conf.d
   lrwxrwxrwx 1 root root   24 Apr 19 18:57 my.cnf -> /etc/alternatives/my.cnf
   -rw-r--r-- 1 root root  839 Aug  3  2016 my.cnf.fallback
   
   root@f858100aa945:/etc/mysql# ls -lth /etc/alternatives/my.cnf
   lrwxrwxrwx 1 root root 20 Apr 19 18:57 /etc/alternatives/my.cnf -> /etc/mysql/mysql.cnf
   
   root@f858100aa945:/etc/mysql# cat my.cnf
   # permission to link the program and your derivative works with the
   # separately licensed software that they have included with MySQL.
   #
   # This program is distributed in the hope that it will be useful,
   # but WITHOUT ANY WARRANTY; without even the implied warranty of
   # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   # GNU General Public License, version 2.0, for more details.
   #
   # You should have received a copy of the GNU General Public License
   # along with this program; if not, write to the Free Software
   # Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301 USA
   
   !includedir /etc/mysql/conf.d/
   !includedir /etc/mysql/mysql.conf.d/
   ```

   既然排除了 `/etc/mysql/conf.d/` 和 `/etc/mysql/mysql.conf.d/`目录，`/etc/mysql/my.cnf` -> `/etc/alternatives/my.cnf` -> `/etc/mysql/mysql.cnf`，所以猜测生效的就是`/etc/mysql/mysql.cnf`。

   vim直接复制有些问题，直接将windows本地的配置文件编辑好拷贝过去

   mysql.cnf文件内容：

   ```properties
   # Copyright (c) 2016, 2021, Oracle and/or its affiliates.
   #
   # This program is free software; you can redistribute it and/or modify
   # it under the terms of the GNU General Public License, version 2.0,
   # as published by the Free Software Foundation.
   #
   # This program is also distributed with certain software (including
   # but not limited to OpenSSL) that is licensed under separate terms,
   # as designated in a particular file or component or in included license
   # documentation.  The authors of MySQL hereby grant you an additional
   # permission to link the program and your derivative works with the
   # separately licensed software that they have included with MySQL.
   #
   # This program is distributed in the hope that it will be useful,
   # but WITHOUT ANY WARRANTY; without even the implied warranty of
   # MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   # GNU General Public License, version 2.0, for more details.
   #
   # You should have received a copy of the GNU General Public License
   # along with this program; if not, write to the Free Software
   # Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301 USA
   
   !includedir /etc/mysql/conf.d/
   !includedir /etc/mysql/mysql.conf.d/
   [mysqld]
   log_timestamps=SYSTEM
   default-time-zone='+8:00'
   log_bin=mysql-bin
   binlog_format=Row
   server-id=1
   sql_mode=STRICT_TRANS_TABLES,NO_ZERO_IN_DATE,NO_ZERO_DATE,ERROR_FOR_DIVISION_BY_ZERO,NO_AUTO_CREATE_USER,NO_ENGINE_SUBSTITUTION
   ```

   windows本地拷贝文件到docker中

   ```shell
   C:\Users\32006\Desktop>docker cp  mysql.cnf mysql_for_canal:/etc/mysql/mysql.cnf
   ```

   测试mysql是否开启binlog

   ```sql
   show VARIABLES like 'log_bin';
   --log_bin	ON
   show master status；
   --File	Position	Binlog_Do_DB	Binlog_Ignore_DB	Executed_Gtid_Set
   --mysql-bin.000001	1241			
   ```

### **window:canal**

1. 下载canal [canal.deployer-1.1.5.tar.gz](https://github.com/alibaba/canal/releases/download/canal-1.1.5/canal.deployer-1.1.5.tar.gz) 并解压安装

2. 配置修改 `C:\Program\canal.deployer-1.1.5\conf\example\instance.properties`

   ~~~properties
   #################################################
   ## mysql serverId , v1.0.26+ will autoGen
   # canal.instance.mysql.slaveId=0
   
   # enable gtid use true/false
   canal.instance.gtidon=false
   
   # position info
   canal.instance.master.address=127.0.0.1:3306
   canal.instance.master.journal.name=
   canal.instance.master.position=
   canal.instance.master.timestamp=
   canal.instance.master.gtid=
   
   # rds oss binlog
   canal.instance.rds.accesskey=
   canal.instance.rds.secretkey=
   canal.instance.rds.instanceId=
   
   # table meta tsdb info
   canal.instance.tsdb.enable=true
   #canal.instance.tsdb.url=jdbc:mysql://127.0.0.1:3306/canal_tsdb
   #canal.instance.tsdb.dbUsername=canal
   #canal.instance.tsdb.dbPassword=canal
   
   #canal.instance.standby.address =
   #canal.instance.standby.journal.name =
   #canal.instance.standby.position =
   #canal.instance.standby.timestamp =
   #canal.instance.standby.gtid=
   
   # username/password
   canal.instance.dbUsername=canal
   canal.instance.dbPassword=canal
   canal.instance.connectionCharset = UTF-8
   ## 手动设置了默认的数据库
   canal.instance.defaultDatabaseName = test
   # enable druid Decrypt database password
   canal.instance.enableDruid=false
   #canal.instance.pwdPublicKey=MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBALK4BUxdDltRRE5/zXpVEVPUgunvscYFtEip3pmLlhrWpacX7y7GCMo2/JM6LeHmiiNdH1FWgGCpUfircSwlWKUCAwEAAQ==
   
   # table regex
   canal.instance.filter.regex=.*\\..*
   # table black regex
   canal.instance.filter.black.regex=mysql\\.slave_.*
   # table field filter(format: schema1.tableName1:field1/field2,schema2.tableName2:field1/field2)
   #canal.instance.filter.field=test1.t_product:id/subject/keywords,test2.t_company:id/name/contact/ch
   # table field black filter(format: schema1.tableName1:field1/field2,schema2.tableName2:field1/field2)
   #canal.instance.filter.black.field=test1.t_product:subject/product_image,test2.t_company:id/name/contact/ch
   
   # mq config
   canal.mq.topic=example
   # dynamic topic route by schema or table regex
   #canal.mq.dynamicTopic=mytest1.user,mytest2\\..*,.*\\..*
   canal.mq.partition=0
   # hash partition config
   #canal.mq.partitionsNum=3
   #canal.mq.partitionHash=test.table:id^name,.*\\..*
   #canal.mq.dynamicTopicPartitionNum=test.*:4,mycanal:6
   #################################################
   ~~~

3. 启动 `C:\Program\canal.deployer-1.1.5\bin\startup.bat`

4. 查看 server 日志 `C:\Program\canal.deployer-1.1.5\logs\canal\canal.log`

   ~~~ log
   2021-04-23 15:16:06.600 [canal-instance-scan-0] INFO  com.alibaba.otter.canal.deployer.CanalController - auto notify stop example successful.
   2021-04-23 15:16:06.737 [canal-instance-scan-0] INFO  com.alibaba.otter.canal.deployer.CanalController - auto notify start example successful.
   2021-04-23 15:16:06.737 [canal-instance-scan-0] INFO  com.alibaba.otter.canal.deployer.CanalController - auto notify reload example successful.
   2021-04-23 15:16:18.349 [main] INFO  com.alibaba.otter.canal.deployer.CanalLauncher - ## set default uncaught exception handler
   2021-04-23 15:16:18.392 [main] INFO  com.alibaba.otter.canal.deployer.CanalLauncher - ## load canal configurations
   2021-04-23 15:16:18.419 [main] INFO  com.alibaba.otter.canal.deployer.CanalStarter - ## start the canal server.
   2021-04-23 15:16:18.729 [main] INFO  com.alibaba.otter.canal.deployer.CanalController - ## start the canal server[192.168.10.42(192.168.10.42):11111]
   2021-04-23 15:16:20.412 [main] INFO  com.alibaba.otter.canal.deployer.CanalStarter - ## the canal server is running now ......
   2021-04-23 16:07:29.250 [main] INFO  com.alibaba.otter.canal.deployer.CanalLauncher - ## set default uncaught exception handler
   2021-04-23 16:07:29.290 [main] INFO  com.alibaba.otter.canal.deployer.CanalLauncher - ## load canal configurations
   2021-04-23 16:07:29.317 [main] INFO  com.alibaba.otter.canal.deployer.CanalStarter - ## start the canal server.
   2021-04-23 16:07:29.647 [main] INFO  com.alibaba.otter.canal.deployer.CanalController - ## start the canal server[192.168.10.42(192.168.10.42):11111]
   2021-04-23 16:07:31.562 [main] INFO  com.alibaba.otter.canal.deployer.CanalStarter - ## the canal server is running now ......
   ~~~

5. 查看 instance 的日志 `C:\Program\canal.deployer-1.1.5\logs\example\example.log`

   ~~~log
   2021-04-23 16:07:31.392 [main] INFO  c.a.otter.canal.instance.spring.CanalInstanceWithSpring - start CannalInstance for 1-example 
   2021-04-23 16:07:31.416 [main] WARN  c.a.o.canal.parse.inbound.mysql.dbsync.LogEventConvert - --> init table filter : ^.*\..*$
   2021-04-23 16:07:31.417 [main] WARN  c.a.o.canal.parse.inbound.mysql.dbsync.LogEventConvert - --> init table black filter : ^mysql\.slave_.*$
   2021-04-23 16:07:31.526 [main] INFO  c.a.otter.canal.instance.core.AbstractCanalInstance - subscribe filter change to .*\..*
   2021-04-23 16:07:31.526 [main] WARN  c.a.o.canal.parse.inbound.mysql.dbsync.LogEventConvert - --> init table filter : ^.*\..*$
   2021-04-23 16:07:31.526 [main] INFO  c.a.otter.canal.instance.core.AbstractCanalInstance - start successful....
   2021-04-23 16:07:32.568 [destination = example , address = /127.0.0.1:3306 , EventParser] WARN  c.a.o.c.p.inbound.mysql.rds.RdsBinlogEventParserProxy - ---> begin to find start position, it will be long time for reset or first position
   2021-04-23 16:07:32.569 [destination = example , address = /127.0.0.1:3306 , EventParser] WARN  c.a.o.c.p.inbound.mysql.rds.RdsBinlogEventParserProxy - prepare to find start position just show master status
   2021-04-23 16:07:35.031 [New I/O server worker #1-1] INFO  c.a.otter.canal.instance.core.AbstractCanalInstance - subscribe filter change to .*\..*
   2021-04-23 16:07:35.031 [New I/O server worker #1-1] WARN  c.a.o.canal.parse.inbound.mysql.dbsync.LogEventConvert - --> init table filter : ^.*\..*$
   2021-04-23 16:07:35.953 [destination = example , address = /127.0.0.1:3306 , EventParser] WARN  c.a.o.c.p.inbound.mysql.rds.RdsBinlogEventParserProxy - ---> find start position successfully, EntryPosition[included=false,journalName=mysql-bin.000001,position=4,serverId=1,gtid=<null>,timestamp=1619165195000] cost : 3373ms , the next step is binlog dump
   
   ~~~


### **idea:canal client**

idea新建项目

依赖配置添加到pom文件：

```
<dependency>
    <groupId>com.alibaba.otter</groupId>
    <artifactId>canal.client</artifactId>
    <version>1.1.0</version>
</dependency>
```

复制代码 ClientExample

~~~ java

import java.net.InetSocketAddress;
import java.util.List;

// import javax.validation.constraints.NotNull;
import lombok.NonNull;

import com.alibaba.otter.canal.client.CanalConnector;
import com.alibaba.otter.canal.client.CanalConnectors;
import com.alibaba.otter.canal.common.utils.AddressUtils;
import com.alibaba.otter.canal.protocol.CanalEntry.Column;
import com.alibaba.otter.canal.protocol.CanalEntry.Entry;
import com.alibaba.otter.canal.protocol.CanalEntry.EntryType;
import com.alibaba.otter.canal.protocol.CanalEntry.EventType;
import com.alibaba.otter.canal.protocol.CanalEntry.RowChange;
import com.alibaba.otter.canal.protocol.CanalEntry.RowData;
import com.alibaba.otter.canal.protocol.Message;

    public class ClientSample {

        public static void main(String args[]) {
            // 创建链接
            CanalConnector connector = CanalConnectors.newSingleConnector(
                    new InetSocketAddress(AddressUtils.getHostIp(), 11111),
                    "example",
                    "",
                    "");
            int batchSize = 1000;
            int emptyCount = 0;
            try {
                connector.connect();
                connector.subscribe(".*\\..*");
                connector.rollback();
                int totalEmtryCount = 1200;
                while (emptyCount < totalEmtryCount) {
                    Message message = connector.getWithoutAck(batchSize); // 获取指定数量的数据
                    long batchId = message.getId();
                    int size = message.getEntries().size();
                    if (batchId == -1 || size == 0) {
                        emptyCount++;
                        System.out.println("empty count : " + emptyCount);
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        emptyCount = 0;
                        // System.out.printf("message[batchId=%s,size=%s] \n", batchId, size);
                        printEntry(message.getEntries());
                    }

                    connector.ack(batchId); // 提交确认
                    // connector.rollback(batchId); // 处理失败, 回滚数据
                }

                System.out.println("empty too many times, exit");
            } finally {
                connector.disconnect();
            }
        }

        private static void printEntry(@NonNull List<Entry> entrys) {
            for (Entry entry : entrys) {
                if (entry.getEntryType() == EntryType.TRANSACTIONBEGIN || entry.getEntryType() == EntryType.TRANSACTIONEND) {
                    continue;
                }

                RowChange rowChage = null;
                try {
                    rowChage = RowChange.parseFrom(entry.getStoreValue());
                } catch (Exception e) {
                    throw new RuntimeException("ERROR ## parser of eromanga-event has an error , data:" + entry.toString(),
                            e);
                }

                EventType eventType = rowChage.getEventType();
                System.out.println(String.format("================> binlog[%s:%s] , name[%s,%s] , eventType : %s",
                        entry.getHeader().getLogfileName(), entry.getHeader().getLogfileOffset(),
                        entry.getHeader().getSchemaName(), entry.getHeader().getTableName(),
                        eventType));

                for (RowData rowData : rowChage.getRowDatasList()) {
                    if (eventType == EventType.DELETE) {
                        printColumn(rowData.getBeforeColumnsList());
                    } else if (eventType == EventType.INSERT) {
                        printColumn(rowData.getAfterColumnsList());
                    } else {
                        System.out.println("-------> before");
                        printColumn(rowData.getBeforeColumnsList());
                        System.out.println("-------> after");
                        printColumn(rowData.getAfterColumnsList());
                    }
                }
            }
        }

        private static void printColumn(@NonNull List<Column> columns) {
            for (Column column : columns) {
                System.out.println(column.getName() + " : " + column.getValue() + "    update=" + column.getUpdated());
            }
        }

}

~~~

运行之后，打开mysql ，运行语句测试

~~~ sql
create table test_canal(id int,name VARCHAR(20));
insert into test_canal values(1,'a');
insert into test_canal values(2,'b');
UPDATE test_canal set name = 'bbbb' where id = 2;
UPDATE test_canal set name = 'cc' where id = 2;
~~~

~~~
================> binlog[mysql-bin.000001:613] , name[test,test_canal] , eventType : INSERT
id : 2    update=true
name : b    update=true

...

================> binlog[mysql-bin.000001:1156] , name[test,test_canal] , eventType : UPDATE
-------> before
id : 2    update=false
name : bbbb    update=false
-------> after
id : 2    update=false
name : cc    update=true

~~~

测试完成

## docker: mysql + window: canal + docker: kafka

### docker: canal

新建network

```shell
docker network create -d bridge --subnet 172.10.0.0/16 --gateway 172.10.0.1 mynet
```

拉取镜像

~~~ 
docker pull canal/canal-server:v1.1.1
~~~

指定name和挂在目录

~~~ shell
## 
docker run -d -p 11111:11111 -v C:\Program\MyDocker\canal_mysql\canal\conf:/home/admin/canal-server/conf/ -v C:\Program\MyDocker\canal_mysql\canal\logs:/home/admin/canal-server/logs --name canal_for_mysql canal/canal-server:v1.1.1
~~~

利用之前的container生成一个镜像docker_mysql

~~~shell
### 利用之前的container生成一个镜像docker_mysql
docker commit mysql_for_canal docker_mysql
~~~

**发现一个问题：**

windows 10 docker desktop 新版本测试有问题，无法启动canal-server，只能在windows下启动canal

[win10 dockerdesktop如果使用WSL 2 based engine，canal-server死活起不来，什么报错信息都看不到，
换回hyperv就好了](https://github.com/alibaba/canal/issues/3079#issue-718564335)

所以不用想了，win10 wsl2 使用不了canal，貌似官方也没啥好办法，直接把这个issue关了

```
win10 dockerdesktop如果使用WSL 2 based engine，canal-server死活起不来，什么报错信息都看不到，
换回hyperv就好了
```

故放弃docker中运行canal，就在windows本地启动canal

### docker: kafka

1. 安装zk

   ~~~ shell
   ## windows dos cmd
   docker run -d -p 2181:2181 --network mynet --name test_canal_zk wurstmeister/zookeeper:3.4.6 
   ~~~

2. 安装kafka

   ~~~ shell
   ## windows dos cmd
   docker run -d -p 9092:9092 -p 9094:9094 --network mynet --name test_canal_kfk ^
   -e KAFKA_ADVERTISED_LISTENERS=INSIDE://:9094,OUTSIDE://localhost:9092 ^
   -e KAFKA_LISTENERS=INSIDE://:9094,OUTSIDE://:9092 ^
   -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP=INSIDE:PLAINTEXT,OUTSIDE:PLAINTEXT ^
   -e KAFKA_INTER_BROKER_LISTENER_NAME=INSIDE ^
   -e KAFKA_ZOOKEEPER_CONNECT=test_canal_zk:2181 ^
   -e KAFKA_CREATE_TOPICS="test_canal:1:1" ^
   -v C:\Program\MyDocker\kfk\run\docker.sock:/var/run/docker.sock ^
   wurstmeister/kafka:2.12-2.2.1 
   ~~~

   > 测试kafka命令：
   >
   > * 进入docker
   >
   >   docker exec -it test_canal_kfk /bin/sh
   >
   > * 查看topics
   >
   >   kafka-topics.sh --bootstrap-server test_canal_kfk:9092 --list
   >
   > * 打开producer
   >
   >   kafka-console-producer.sh --topic test_canal --broker-list test_canal_kfk:9092
   >
   > * 打开consumer
   >
   >   kafka-console-consumer.sh --topic test_canal --bootstrap-server test_canal_kfk:9092
   >
   > * 手动删除不需要的topic
   >
   >   kafka-topics.sh  --delete --zookeeper test_canal_zk:2181 --topic exapmle
   >
   >   需要提前 设置 delete.topic.enable=true
   >
   >   或者 在程序里删除`client.deleteTopics()`

3. canal 指定到kafka

   * 修改 `conf\example\instance.properties`

     ~~~ properties
     #################################################
     ## mysql serverId , v1.0.26+ will autoGen
     # canal.instance.mysql.slaveId=0
     
     # enable gtid use true/false
     canal.instance.gtidon=false
     
     # position info
     canal.instance.master.address=127.0.0.1:3306
     canal.instance.master.journal.name=
     canal.instance.master.position=
     canal.instance.master.timestamp=
     canal.instance.master.gtid=
     
     # rds oss binlog
     canal.instance.rds.accesskey=
     canal.instance.rds.secretkey=
     canal.instance.rds.instanceId=
     
     # table meta tsdb info
     canal.instance.tsdb.enable=true
     #canal.instance.tsdb.url=jdbc:mysql://127.0.0.1:3306/canal_tsdb
     #canal.instance.tsdb.dbUsername=canal
     #canal.instance.tsdb.dbPassword=canal
     
     #canal.instance.standby.address =
     #canal.instance.standby.journal.name =
     #canal.instance.standby.position =
     #canal.instance.standby.timestamp =
     #canal.instance.standby.gtid=
     
     # username/password
     canal.instance.dbUsername=canal
     canal.instance.dbPassword=canal
     canal.instance.connectionCharset = UTF-8
     canal.instance.defaultDatabaseName = test
     # enable druid Decrypt database password
     canal.instance.enableDruid=false
     #canal.instance.pwdPublicKey=MFwwDQYJKoZIhvcNAQEBBQADSwAwSAJBALK4BUxdDltRRE5/zXpVEVPUgunvscYFtEip3pmLlhrWpacX7y7GCMo2/JM6LeHmiiNdH1FWgGCpUfircSwlWKUCAwEAAQ==
     
     # table regex
     # canal.instance.filter.regex=.*\\..*
     canal.instance.filter.regex=.*\\..*
     # table black regex
     canal.instance.filter.black.regex=mysql\\.slave_.*
     # table field filter(format: schema1.tableName1:field1/field2,schema2.tableName2:field1/field2)
     #canal.instance.filter.field=test1.t_product:id/subject/keywords,test2.t_company:id/name/contact/ch
     # table field black filter(format: schema1.tableName1:field1/field2,schema2.tableName2:field1/field2)
     #canal.instance.filter.black.field=test1.t_product:subject/product_image,test2.t_company:id/name/contact/ch
     
     # mq config
     canal.mq.topic=example
     # dynamic topic route by schema or table regex
     #canal.mq.dynamicTopic=mytest1.user,mytest2\\..*,.*\\..*
     canal.mq.partition=0
     # hash partition config
     #canal.mq.partitionsNum=3
     #canal.mq.partitionHash=test.table:id^name,.*\\..*
     #canal.mq.dynamicTopicPartitionNum=test.*:4,mycanal:6
     #################################################
     ~~~

     > canal instance.properties 配置kafka细节：
     >
     > * 数据库链接：
     >
     >   canal.instance.master.address=127.0.0.1:3306
     >
     > * 数据库用户信息
     >
     >   canal.instance.dbUsername=canal
     >   canal.instance.dbPassword=canal
     >   canal.instance.connectionCharset = UTF-8
     >   canal.instance.defaultDatabaseName = test
     >
     > * table regex
     >   `canal.instance.filter.regex=.*\\..*`
     >
     > * mq config
     >   canal.mq.topic=example
     >
     > * dynamic topic route by schema or table regex
     >
     >    针对库名或者表名发送动态topic
     >
     >   `# canal.mq.dynamicTopic=mytest1.user,mytest2\\..*,.*\\..*`
     >
     >   库名.表名: 唯一主键，多个表之间用逗号分隔
     >
     >   ```
     >   #canal.mq.partitionHash=mytest.person:id,mytest.role:id
     >   ```

   * 修改`conf\canal.properties`

     ~~~ properties
     #################################################
     ######### 		common argument		#############
     #################################################
     # tcp bind ip
     canal.ip =
     # register ip to zookeeper
     canal.register.ip =
     canal.port = 11111
     canal.metrics.pull.port = 11112
     # canal instance user/passwd
     # canal.user = canal
     # canal.passwd = E3619321C1A937C46A0D8BD1DAC39F93B27D4458
     
     # canal admin config
     #canal.admin.manager = 127.0.0.1:8089
     canal.admin.port = 11110
     canal.admin.user = admin
     canal.admin.passwd = 4ACFE3202A5FF5CF467898FC58AAB1D615029441
     # admin auto register
     #canal.admin.register.auto = true
     #canal.admin.register.cluster =
     #canal.admin.register.name =
     
     canal.zkServers =
     # flush data to zk
     canal.zookeeper.flush.period = 1000
     canal.withoutNetty = false
     # tcp, kafka, rocketMQ, rabbitMQ
     # canal.serverMode = tcp
     canal.serverMode = kafka
     # flush meta cursor/parse position to file
     canal.file.data.dir = ${canal.conf.dir}
     canal.file.flush.period = 1000
     ## memory store RingBuffer size, should be Math.pow(2,n)
     canal.instance.memory.buffer.size = 16384
     ## memory store RingBuffer used memory unit size , default 1kb
     canal.instance.memory.buffer.memunit = 1024 
     ## meory store gets mode used MEMSIZE or ITEMSIZE
     canal.instance.memory.batch.mode = MEMSIZE
     canal.instance.memory.rawEntry = true
     
     ## detecing config
     canal.instance.detecting.enable = false
     #canal.instance.detecting.sql = insert into retl.xdual values(1,now()) on duplicate key update x=now()
     canal.instance.detecting.sql = select 1
     canal.instance.detecting.interval.time = 3
     canal.instance.detecting.retry.threshold = 3
     canal.instance.detecting.heartbeatHaEnable = false
     
     # support maximum transaction size, more than the size of the transaction will be cut into multiple transactions delivery
     canal.instance.transaction.size =  1024
     # mysql fallback connected to new master should fallback times
     canal.instance.fallbackIntervalInSeconds = 60
     
     # network config
     canal.instance.network.receiveBufferSize = 16384
     canal.instance.network.sendBufferSize = 16384
     canal.instance.network.soTimeout = 30
     
     # binlog filter config
     canal.instance.filter.druid.ddl = true
     canal.instance.filter.query.dcl = false
     canal.instance.filter.query.dml = false
     canal.instance.filter.query.ddl = false
     canal.instance.filter.table.error = false
     canal.instance.filter.rows = false
     canal.instance.filter.transaction.entry = false
     canal.instance.filter.dml.insert = false
     canal.instance.filter.dml.update = false
     canal.instance.filter.dml.delete = false
     
     # binlog format/image check
     canal.instance.binlog.format = ROW,STATEMENT,MIXED 
     canal.instance.binlog.image = FULL,MINIMAL,NOBLOB
     
     # binlog ddl isolation
     canal.instance.get.ddl.isolation = false
     
     # parallel parser config
     canal.instance.parser.parallel = true
     ## concurrent thread number, default 60% available processors, suggest not to exceed Runtime.getRuntime().availableProcessors()
     #canal.instance.parser.parallelThreadSize = 16
     ## disruptor ringbuffer size, must be power of 2
     canal.instance.parser.parallelBufferSize = 256
     
     # table meta tsdb info
     canal.instance.tsdb.enable = true
     canal.instance.tsdb.dir = ${canal.file.data.dir:../conf}/${canal.instance.destination:}
     canal.instance.tsdb.url = jdbc:h2:${canal.instance.tsdb.dir}/h2;CACHE_SIZE=1000;MODE=MYSQL;
     canal.instance.tsdb.dbUsername = canal
     canal.instance.tsdb.dbPassword = canal
     # dump snapshot interval, default 24 hour
     canal.instance.tsdb.snapshot.interval = 24
     # purge snapshot expire , default 360 hour(15 days)
     canal.instance.tsdb.snapshot.expire = 360
     
     #################################################
     ######### 		destinations		#############
     #################################################
     canal.destinations = example
     # conf root dir
     canal.conf.dir = ../conf
     # auto scan instance dir add/remove and start/stop instance
     canal.auto.scan = true
     canal.auto.scan.interval = 5
     # set this value to 'true' means that when binlog pos not found, skip to latest.
     # WARN: pls keep 'false' in production env, or if you know what you want.
     canal.auto.reset.latest.pos.mode = false
     
     canal.instance.tsdb.spring.xml = classpath:spring/tsdb/h2-tsdb.xml
     #canal.instance.tsdb.spring.xml = classpath:spring/tsdb/mysql-tsdb.xml
     
     canal.instance.global.mode = spring
     canal.instance.global.lazy = false
     canal.instance.global.manager.address = ${canal.admin.manager}
     #canal.instance.global.spring.xml = classpath:spring/memory-instance.xml
     canal.instance.global.spring.xml = classpath:spring/file-instance.xml
     #canal.instance.global.spring.xml = classpath:spring/default-instance.xml
     
     ##################################################
     ######### 	      MQ Properties      #############
     ##################################################
     # aliyun ak/sk , support rds/mq
     canal.aliyun.accessKey =
     canal.aliyun.secretKey =
     canal.aliyun.uid=
     
     canal.mq.flatMessage = true
     canal.mq.canalBatchSize = 50
     canal.mq.canalGetTimeout = 100
     # Set this value to "cloud", if you want open message trace feature in aliyun.
     canal.mq.accessChannel = local
     
     canal.mq.database.hash = true
     canal.mq.send.thread.size = 30
     canal.mq.build.thread.size = 8
     
     ##################################################
     ######### 		     Kafka 		     #############
     ##################################################
     kafka.bootstrap.servers = 127.0.0.1:9092
     kafka.acks = all
     kafka.compression.type = none
     kafka.batch.size = 16384
     kafka.linger.ms = 1
     kafka.max.request.size = 1048576
     kafka.buffer.memory = 33554432
     kafka.max.in.flight.requests.per.connection = 1
     kafka.retries = 0
     
     kafka.kerberos.enable = false
     kafka.kerberos.krb5.file = "../conf/kerberos/krb5.conf"
     kafka.kerberos.jaas.file = "../conf/kerberos/jaas.conf"
     
     ##################################################
     ######### 		    RocketMQ	     #############
     ##################################################
     rocketmq.producer.group = test
     rocketmq.enable.message.trace = false
     rocketmq.customized.trace.topic =
     rocketmq.namespace =
     rocketmq.namesrv.addr = 127.0.0.1:9876
     rocketmq.retry.times.when.send.failed = 0
     rocketmq.vip.channel.enabled = false
     rocketmq.tag = 
     
     ##################################################
     ######### 		    RabbitMQ	     #############
     ##################################################
     rabbitmq.host =
     rabbitmq.virtual.host =
     rabbitmq.exchange =
     rabbitmq.username =
     rabbitmq.password =
     rabbitmq.deliveryMode =
     ~~~

     > canal canal.properties 配置kafka细节：
     >
     > * tcp, kafka, rocketMQ, rabbitMQ
     >
     >   canal.serverMode = kafka 
     >
     > * destinations 
     >
     >   canal.destinations = example
     >
     > * kafka
     >
     >   kafka.bootstrap.servers = 127.0.0.1:9092
     >   kafka.acks = all
     >   kafka.compression.type = none
     >   kafka.batch.size = 16384
     >   kafka.linger.ms = 1
     >   kafka.max.request.size = 1048576
     >   kafka.buffer.memory = 33554432
     >   kafka.max.in.flight.requests.per.connection = 1
     >   kafka.retries = 0
     >
     >   kafka.kerberos.enable = false
     >   kafka.kerberos.krb5.file = "../conf/kerberos/krb5.conf"
     >   kafka.kerberos.jaas.file = "../conf/kerberos/jaas.conf"
     >

   * 测试canal输出到kafka的topic是否有数据

     ~~~ shell
     $ kafka-console-consumer.sh --topic example --bootstrap-server test_canal_kfk:9092
     {"data":[{"id":"3","name":"c"}],"database":"test","es":1619331876000,"id":5,"isDdl":false,"mysqlType":{"id":"int(11)","name":"varchar(20)"},"old":null,"pkNames":null,"sql":"","sqlType":{"id":4,"name":12},"table":"test_canal","ts":1619331877986,"type":"INSERT"}
     {"data":[{"id":"3","name":"c"}],"database":"test","es":1619331884000,"id":6,"isDdl":false,"mysqlType":{"id":"int(11)","name":"varchar(20)"},"old":null,"pkNames":null,"sql":"","sqlType":{"id":4,"name":12},"table":"test_canal","ts":1619331885401,"type":"DELETE"}
     {"data":[{"id":"1","name":"a"},{"id":"1","name":"a"}],"database":"test","es":1619332401000,"id":7,"isDdl":false,"mysqlType":{"id":"int(11)","name":"varchar(20)"},"old":null,"pkNames":null,"sql":"","sqlType":{"id":4,"name":12},"table":"test_canal","ts":1619332402880,"type":"DELETE"}
     ~~~

4. idea 连接到kafka

   测试consumer client 是否能收到kafka数据就行 


## 测试：mysql + canal + kafka + spark streaming + redis

修改mysql中数据，实现canal 将binlog转到kafka ，通过spark streaming 消费计算PV/UV，将最终结果写到Redis

### 模拟mysql 数据变化

~~~ sql
CREATE TABLE test.user_click_log (
user_id varchar(20)
,ip  varchar(20)
,ad_id varchar(20)
,province varchar(20)
,city varchar(20)
,c_time timestamp
)

~~~

操作mysql

~~~ java
/*
userID + ip + adID + province + city  + c_time
*/

String tableName = "test.user_click_log";

String ddl = "drop TABLE if exists " + tableName + ";" +
    "CREATE TABLE "+ tableName+" (\n" +
    "        user_id varchar(20)\n" +
    "        ,ip  varchar(20)\n" +
    "        ,ad_id varchar(20)\n" +
    "        ,province varchar(20)\n" +
    "        ,city varchar(20)\n" +
    "        ,c_time timestamp\n" +
    "        )\n" +
    "        ;";
// DBUtil.execute(ddl);

final Random random = new Random();
final String[] provinces = new String[]{"Guangdong", "Zhejiang", "Jiangsu", "Fujian"};
//城市
final Map<String, String[]> cities = new HashMap<String, String[]>();
cities.put("Guangdong", new String[]{"Guangzhou", "Shenzhen", "DongGuan"});
cities.put("Zhejiang", new String[]{"Hangzhou", "Wenzhou", "Ningbo"});
cities.put("Jiangsu", new String[]{"Nanjing", "Suzhou", "Wuxi"});
cities.put("Fujian", new String[]{"Fuzhou", "xiamen", "Sanming"});


new Thread(new Runnable() {

    Long c_time = new Date().getTime();

    @SneakyThrows
    @Override
    public void run() {
        while (true) {
            //在线处理广告点击流 广告点击的基本数据格式：timestamp、ip、userID、adID、province、city
            // Long c_time = new Date().getTime();
            c_time += 1800000;// 每次加30分钟
            String strDateFormat = "yyyy-MM-dd HH:mm:ss";
            SimpleDateFormat sdf = new SimpleDateFormat(strDateFormat);
            String c_time_format = sdf.format(c_time);

            String ip = String.format("192.168.%s.%s",random.nextInt(255),random.nextInt(255));
            int userID = random.nextInt(1000);
            int adID = random.nextInt(100);
            String province = provinces[random.nextInt(4)];
            String city = cities.get(province)[random.nextInt(3)];
            String sql_text = String.format("insert into %s " +
                                            "values('user-%s','%s','ad-%s','%s','%s','%s')"
                                            , tableName,userID, ip, adID, province, city, c_time_format);

            // String clickedAd = userID + ip + adID + province + city  + c_time ;
            System.out.println(sql_text);
            DBUtil.execute(sql_text);

            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}).start();
~~~



### docker 安装Redis 

~~~ shell
docker run -d -p 6379:6379 --network mynet --rm --name redis-test redis:latest
docker exec -it redis-test /bin/bash
~~~



### spark streaming 计算

> 启动顺序：
>
> 1. mysql (docker)
> 2. canal (windows)
> 3. zookeeper (docker)
> 4. kafka (docker)
> 5. 模拟写数据到mysql  (idea)
> 6. 启动pv/uv计算的spark streaming  (idea)
> 7. 启动查看redis里UV的变化值  (idea)

#### pv的计算

拉取数据有两种方式，基于received和direct方式，这里用direct直拉的方式，用的mapWithState算子保存状态，这个算子与updateStateByKey一样，并且性能更好。当然了实际中数据过来需要经过清洗，过滤，才能使用。

updateStateByKey:

~~~ java
JavaPairDStream<Tuple2<String, String>, Integer> pv1 = pv.updateStateByKey(
    new Function2<List<Integer>, Optional<Integer>, Optional<Integer>>() {
        @Override
        public Optional<Integer> call(List<Integer> v1, Optional<Integer> state) throws Exception {
            Integer newValues = 0;

            if (state.isPresent()) {
                newValues = state.get();
            }

            for (Integer value : v1) {
                newValues += value;
            }
            return Optional.of(newValues);
        }
    });
~~~

mapWithState:

~~~ java
JavaMapWithStateDStream<Tuple2<String, String>, Integer, Integer, Tuple2<Tuple2<String, String>, Integer>> stateWordCounts = pv.mapWithState
                (StateSpec.function(
                    new org.apache.spark.api.java.function.Function3<Tuple2<String, String>, Optional<Integer>, State<Integer>,
                    Tuple2<Tuple2<String, String>, Integer>>() {
                        @Override
                        public Tuple2<Tuple2<String, String>, Integer> call(Tuple2<String, String> word, Optional<Integer> one, State<Integer> state) throws Exception {
                            Option<Integer> stateCount = state.getOption();
                            Integer sum = one.orElse(0);
                            if (stateCount.isDefined()) {
                                sum += stateCount.get();
                            }
                            state.update(sum);
                            return new Tuple2<Tuple2<String, String>, Integer>(word, sum);
                        }
                    }));
~~~

比较：updateStateByKey是输出增量数据,随着时间的增加, 输出的数据越来越多,这样会影响计算的效率, 对CPU和内存压力较大，而mapWithState则输出本批次数据,但是也含有状态更新.

~~~ scala
// 实时流量状态更新函数
  val mapFunction = (datehour:String, pv:Option[Long], state:State[Long]) => {
    val accuSum = pv.getOrElse(0L) + state.getOption().getOrElse(0L)
    val output = (datehour,accuSum)
    state.update(accuSum)
    output
  }

// 计算pv
 val stateSpec = StateSpec.function(mapFunction)
 val helper_count_all = helper_data.map(x => (x._1,1L)).mapWithState(stateSpec).stateSnapshots().repartition(2)
~~~

#### uv的计算

uv是要全天去重的，每次进来一个batch的数据，如果用原生的reduceByKey或者groupByKey对配置要求太高，在配置较低情况下，我们申请了一个93G的redis用来去重，原理是每进来一条数据，将date作为key，guid加入set集合，20秒刷新一次，也就是将set集合的尺寸取出来，更新一下数据库即可。

~~~ scala
helper_data.foreachRDD(rdd => {
        rdd.foreachPartition(eachPartition => {
        // 获取redis连接
          val jedis = getJedis
          eachPartition.foreach(x => {
            val date:String = x._1.split(":")(0)
            val key = date
            // 将date作为key，guid(x._2)加入set集合
            jedis.sadd(key,x._2)
            // 设置存储每天的数据的set过期时间，防止超过redis容量，这样每天的set集合，定期会被自动删除
            jedis.expire(key,ConfigFactory.rediskeyexists)
          })
          // 关闭连接
          closeJedis(jedis)
        })
      })
~~~

