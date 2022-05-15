# 第一章 MySQL体系结构和存储引擎

## 1.1 定义数据库和实例

1. **数据库**：物理操作系统文件和其他形式<u>文件</u>类型的<u>集合</u>
2. **实例**：MySQL数据由后台<u>线程</u>以及一个共享内存区组成。
    * 共享内存可以背运行的后台线程所共享
    * 数据库实例才是真正用于操作数据库文件的。
3. MySQL数据库实例在系统上的表现就是一个进程。

## 1.2 MySQL体系结构

1. MySQL由以下部分组成：
    1. 连接池组件
    2. 管理服务和工具组件
    3. SQL接口组件
    4. 查询分析器组件
    5. 优化器组件
    6. 缓冲组件
    7. 插件式存储引擎
    8. 物理文件
2. **插件式表存储引擎**：区别于其他数据库最重要的一个特点。
3. 存储引擎是基于表的。

## 1.3 MySQL存储引擎

### 1.3.1 InnoDB存储引擎

1. 特点：行锁支持，支持外键，支持非锁定读。
2. 高并发性通过多版本并发控制MVCC来获得，并实现了SQL 标准的4种隔离级别，<u>默认为REPEATABLE级别</u>。同时使用next-key locking的侧罗来避免幻读（phantom）的产生。
    1. 另外还提供了插入缓冲（insert buffer）、二次写（double write）、自适应哈希索引（adaptive hash index）、预读（read ahead）等高性能高可用的功能
3. 对于数据的存储，innoDB采用聚集的方式，因此每张表的存储都是按主键的顺序进行存放。
    * 如果没有显示定义主键，存储引擎会为每一行商城一个6字节的ROWID并以此作为主键。

### 1.3.2 MyISAM存储引擎

1. 特点：不支持事务、表锁设计，支持全文索引；缓冲池只缓存索引文件，而不缓冲数据文件。
2. myISAM存储引擎表由MYD和MYI组成，MYD用来存放数据文件，MYI用来存放索引文件。

### 1.3.3 NDB存储引擎

1. 集群存储引擎，share nothing的集群架构。特点是数据全部放在内存，因此主键查找的速度极快。

### 1.3.4 Memory存储引擎

1. 将数据都放在内存中，适合存储临时数据表，以及数据仓库的维度表。默认使用哈希索引而非B+树索引。
2. 只支持表锁，并发性能差，不支持TEXT和BLOB类型；存储变长字段（varchar）是按照定长字段（char）进行的，因此会浪费内存。
3. 如果用它存放查询的中间结果，且结果大于表容量，则数据库会把它转换到MyISAM存储引擎表而存放到磁盘中，又myISAM不缓存数据文件，这时产生的临时表的性能对查询会有损失。

### 1.3.5 Archive存储引擎

1. 只支持INSERT和SELECT操作
2. 提供告诉的插入和压缩功能，适合存储归档数据。

### 1.3.6 Federated存储引擎

1. 不存放数据，指向一台远程MySQL数据库服务器上的表，类似透明网关。

### 1.3.7 Maria存储引擎

1. MyISAM后续版本替代，支持缓存数据和索引文件，应用了行锁设计，提供了MVCC，支持事务和非事务的选项，以及更好处理BLOB的性能。

### 1.3.8 其他存储引擎

## 1.4 存储引擎的比较（略）

## 1.5 连接MySQL

### 1.5.1 TCP/IP

### 1.5.2 命名管道和共享内存

### 1.5.3 UNIX域套接字

## 1.6 小结

本章介绍了数据库和数据库实例的定义，分析了MySQL数据库体系结构，进一步突出强调实例和数据库的区别。介绍了各种常见表存储引擎的特性、使用情况以及它们之间的区别。

# 第二章 InnoDB存储引擎

本章介绍InnoDB体系架构以及其特性。

## 2.1 InnoDB存储引擎概述

## 2.2 InnoDB存储引擎的版本

## 2.3 InnoDB体系架构

<img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202112271745677.png" alt="image-20211227174547457" style="zoom:30%;" />

### 2.3.1 后台线程

1. **Master Thread**： 负责将缓冲池中的数据异步刷新到磁盘，保证数据的一致性，包括脏页的刷新、合并插入缓冲、UNDO页的回收等。
2. **IO Thread**： 处理异步IO请求的回调。
    * write，read，log， insert buffer IO Thread
3. **Purge Thread**：将提交的事务的undolog回收。
4. **Purge Cleaner Thread**：将之前版本中的脏页的刷新操作都放到单独的线程来完成。为了减轻原Master Thread的工作以及对于用户查询线程的阻塞。

### 2.3.3 内存

1. **缓冲池**

    1. 因为InnoDB是基于磁盘系统的，并将其中的记录按照页的方式进行管理，可将其视为基于磁盘的数据库系统。由于CPU速度和磁盘速度的鸿沟，通常使用缓冲池技术。
    2. 一块内存区域
        1. 读：先将磁盘读取到的页存放在缓冲池中，下次再读相同页时，先判断是否在缓冲池中，若在，则称命中，直接读取，否则读取磁盘中的页。
        2. 修改：首先修改缓冲池中的页，再以一定的频率刷新到磁盘上。通过一种叫**<u>checkpoint</u>**的机制刷新回磁盘。
    3. 缓冲池的大小影响着数据库的整体性能。
    4. InnoDB存储引擎内存结构
        1. <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202112271817544.png" alt="image-20211227181733510" style="zoom:30%;" />

    5. 从InnoDB 1.0.x开始，允许有多个缓冲池实例，每个页根据哈希平均分配到不同的缓冲池实例中，减少数据库内部的资源竞争。

2. **LRU list、Free List和Flush List**

    1. 缓冲池是通过LRU算法管理的。
        1. InnoDB还有midpoint优化。新读取的页不放在首部而是放入到LRU列表的midpoint位置。默认位置为5/8处。
        2. 相比于朴素LRU算法：某些操作可能将缓冲池的页刷出，影响效率，例如索引或数据的扫描操作，需要访问表中的很多页，这些数据并非热点数据。
        3. 为了解决这个问题，还有一个参数innodb_old_blocks_time，用于表示读取到的mid位置后需要等待多久才会被加入到LRU的热端，如果在执行操作前用户觉得自己的热点数据不止某个值，可以在执行前通过设定此参数使数据尽量不被刷出。
        4. page made young：从old部分加入到new部分的操作。显示了LRU列表中页移动到前端的次数。
        5. LRU列表中的页被修改后成为脏页。
    2. FLUSH列表即为脏列表，脏页存在于FLUSH列表和LUR列表，但是互不影响。LRU列表管理缓冲池中页的可用性，Flush列表用来管理将页刷新回磁盘。

3. 重做日志缓冲：存储引擎现将redo log放在这个缓冲区，然后按照一定频率刷新到redo log 文件。

    1. 刷新时机
        1. Master Thread每一秒
        2. 每个事务提交时
        3. 当redo log缓冲池剩余空间小于一半

4. 额外的内存池

    1. 对内存的管理是通过内存堆。
    2. e.g.缓冲池的缓冲控制对象就是从额外的内存池申请。

## 2.4 checkpoint 技术

1. 为了避免发生数据丢失，当前事务数据库系统普遍采用Arite Ahead Log策略，即当事务提交时，先写重做日志，再修改页。当发生数据丢失时，通过redo log完成数据的恢复。这也是ACID中的D持久性的要求。

2. 如果

    1. 缓冲池可以缓存数据库中所有数据；
    2. 重做日志可以无限增大

    可以通过重做日志来恢复整个数据库中的数据到宕机发生的时刻。

3. **检查点Checkpoint**技术是解决：

    1. 缩短数据库恢复时间
        * 宕机发生时，数据库不需要重做所有的日志，因为checkpoint之前的页都已经刷新回磁盘。数据库只需要对checkpoint之后的重做日志进行恢复。
    2. 缓冲池不够用时，将脏页刷新到磁盘
        * 根据LRU算法算出的最近最少使用的页，如果是脏页，则需要强制执行checkpoint，将脏页刷回磁盘。
    3. 重做日志不可用时，刷新脏页。
        * 出现不可用是因为redo log被设计成重复使用，如果可能发生宕机系统恢复还需要这部分日志，则强制产生checkpoint，将缓冲池中的页至少刷新到当前重做日志的位置。

4. InnoDB是通过LSN标记版本的

5. checkpoint分类

    1. Sharp Checkpoint：数据库关闭时所有的脏页都刷新回磁盘。
    2. Fuzzy Checkpoint：只刷新一部分脏页。
        1. Master Thread Checkpoint：异步操作。
        2. FLUSH_LRU_LIST Checkpoint：存储引擎需要保证有100个空闲页可用，会将LRU列表尾部的页移除，如果存在脏页则需要进行checkpoint。
        3. Async/Sync Flush Checkpoint： 指的是重做日志不可用的情况。脏页是从脏页列表选取的。
            1. 在InnoDB 1.2.x版本开始，这部分操作放在了单独的page cleaner Thread中，不会阻塞用户查询线程。
        4. Dirty Page too much Checkpoint：保证缓冲池有足够可用的页。

## 2.5 Master Thread工作方式

### 2.5.1 InnoDB 1.0.x版本之前的Master Thread

1. Master Thread具有最高的线程优先级别
2. 内部由多个循环组成：主循环、后台循环、刷新循环、暂停循环。主线程会跟进数据库运行状态在这几个循环之间切换。
3. Loop被称为主循环，其中有两大部分的操作：每秒钟和每10s。通过thread sleep实现，所以操作是不精确的。
    1. 每秒钟的操作：
        * 日志缓冲刷新到磁盘，即使这个事务还没有提交（总是）；
            * 这个可以解释很大的事务commit时间很短
        * 合并插入缓存（可能）；
            * 当前1s的IO小于5次才会
        * 至多刷新100个InnoDB的缓冲池中的脏页到磁盘（可能）；
            * 缓冲池中的脏页的比例超过了阈值
        * 如果当前没有用户活动，则切换到background loop（可能）；
4. 后台循环：若当前没有用户活动或者数据库关闭，就会切换到这个循环

### 2.5.2  InnoDB 1.2.x版本之前的Master Thread

1. 从之前的版本看，在写密集的应用程序中，美妙可能会产生大于100个脏页，大于20个插入缓冲。所以InnoDB Plugin提供了innodb_io_capacity来进行控制。
2. 脏页站缓冲池90%太大了。经过测试，默认值变为75%。

### 2.5.3  InnoDB 1.2.x版本的Master Thread

1. 刷新脏页的操作分离到purge cleaner线程

## 2.6 InnoDB 关键特性

* 插入缓冲
* 两次血
* 自适应哈希索引
* 异步IO
* 刷新临接页

### 2.6.1 插入缓冲

1. Insert Buffer：对于非聚集索引的插入或更新操作，不是每一次直接插入到索引页中，而是先判断插入的非聚集索引是否在缓冲池中，若在直接插入，若不在则先放入到一个Insert Buffer对象中。好像这个非聚集索引已经插入到叶子结点，实际没有，只是放在另一个位置。然后再以一定的频率和情况进行Insert Buffer和辅助索引页子节点的合并操作。
    1. 将多个插入合并到一个操作（因为在一个索引页），提高了非聚集索引插入的性能
2. 需要满足的条件
    1. 索引是辅助索引
    2. 索引不是唯一的
3. 问题：写密集情况下插入缓冲会占用过多的缓冲池内存。

4. change Buffer：insert buffer升级版本，对INSERT，DELETE，UPDATE都缓冲
    1. 对一条记录的UPDATE分为两个过程
        1. 将记录标为已删除
        2. 真正将记录删除
5. insert buffer内部实现
    1. b+树
6. Merge Insert buffer

### 2.6.2 两次写

1. 在应用重做日志前，用户需要一个页的副本，当写入失效发生时，先通过页的副本来还原该页，再进行重做。

### 2.6.3 自适应哈希索引

1. InnoDB会监控对表上各索引的查询，如果观察到建立哈希索引可以带来速度提升，则建立哈希索引，称之为自适应哈希索引。
    1. 要求：对页的连续访问模式是一致的。
    2. 只能进行等值的查询。

### 2.6.4 异步IO

1. 用户在发出一个IO请求后立即再发出另一个IO请求，当全部IO请求发送完毕后等待所有的IO操作完成。
2. 可以进行IO merge，将多个IO合并为1个。e.g. 用户需要访问页（space，page_np)为：（8，6）、（8，7）、（8，8），每个页的大小为16KB，同步IO需要进行3次IO操作，AIO会判断到这三个页是连续的，底层会发送一个IO请求从（8，6）开始读取48KB的页。
3. 应用：read ahead，脏页刷新，即磁盘的写入都是由AIO完成。

### 2.6.5 刷新邻接页

1. 刷新脏页时，检测他所在的extent区的所有页，如果是脏页，一起刷新，

## 2.7 启动、关闭与恢复

## 2.8 小结

本章对InnoDB存储引擎及其体系结构（后台线程、内存结构）进行概述，又详细介绍其关键特性，最后介绍启动关闭时一些参数配置。

# 第三章 文件

* 参数文件
* 日志文件
* socket文件
* pid文件
* MySQL表结构文件
* 存储引擎文件

## 3.1 参数文件

### 3.1.1 什么是参数

1. 键值对

### 3.1.2 参数类型

1. 动态参数：实例运行中更改
2. 静态参数：运行生命周期内不可更改

## 3.2 日志文件

* 错误日志
* 二进制日志
* 慢查询日志
* 查询日志

### 3.2.1 错误日志

1. 对MySQL的启动、运行、关闭过程进行了记录。

### 3.2.2 慢查询日志

1. 帮助DBA定位可能存在问题的SQL语句。e.g.设定一个阈值，超过这个阈值的语句都被记录到慢查询日志。
2. 默认是关闭的。

### 3.2.3 查询日志

1. 查询日志记录了所有对MySQL数据库请求的信息，无论是否正确执行。

### 3.2.4 二进制日志

1. 二进制日志记录了对MySQL数据库执行更改的所有操作，但是不包括SELECT和SHOW这类操作。
2. 作用：
    1. 恢复：某些数据的恢复需要二进制文件。例如，在一个数据库全被文件恢复后，用户可以通过二进制日志进行point-in-time恢复
    2. 复制：通过复制和执行二进制日志使一台远程的MYSQL数据库(一般是slave或者standy)与一台MYSQL数据库(master或primary)进行实时同步。
    3. 审计：用户可以通过二进制日志中的信息来进行审计，判断是否有对数据库进行注入的攻击。

## 3.3 套接字文件

* 在UNIX系统下连接MySQL采用UNIX域套接字方式，需要套接字文件。

## 3.4 pid文件

* 当MySQL实例启动时，会将自己的进程ID写入一个文件中。

## 3.5 表结构定义文件

* 文本格式，视图的定义也在其中。

## 3.6 InnoDB存储引擎文件

### 3.6.1 表空间文件

1. InnoDB采用将存储的数据按<u>**表空间**</u>进行存放的设计。

### 3.6.2 重做日志文件

1. 记录了存储引擎的事务日志。

## 3.7 小结

* 本章介绍MySQL数据库相关的一些文件，可以分为MySQL数据库文件以及各存储引擎相关的文件。与MySQL有关的文件中，**<u>错误文件</u>**和**<u>二进制文件</u>**非常重要。DBA可以使用错误文件的记录来分析错误，也可以通过其警告优化数据库或存储引擎。
* 二进制文件非常关键，可以用来point in time的恢复和复制环境的搭建。
* 最后介绍InnoDB存储引擎相关文件，包括表空间文件和重做日志文件，表空间文件时用来管理存储引擎的存储，分为共享表空间和独立表空间。重做日志非常重要，是存储引擎的事务日志。

# 第四章 表

## 4.1 索引组织表

1. InnoDB中，表都是根据主键顺序组织存放的，这种方式成为索引组织表。
2. InnoDB每个表都有主键，如果创建时没有显式定义
    1. 如果表中存在非空唯一索引，该索引即为主键；如果有多个，则选择第一个定义的；
    2. 否则创建6字节大小的指针

## 4.2 InnoDB逻辑存储结构

* 所有数据都被逻辑的存放在一个空间中，称之为表空间。表空间又由段、区、页组成。页在一些文档也称为块。

    <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202112311511179.png" alt="image-20211231151101069" style="zoom:30%;" />

### 4.2.1 表空间

1. 默认情况下InnoDB有一个共享表空间```ibddata1```，所有的数据都放在这里；如果用户启用参数innodb_file_per_table，则每张表内的数据可以单独放到一个表空间
    1. 注意：启用参数后，每张表空间只存放数据、索引、插入缓冲Bitmap页，其他数据如回滚undo信息，插入缓冲索引页、系统事务信息、二次写缓冲还是放在原来的共享表空间。

### 4.2.2 段

1. 常见的段有：数据段、索引段、回滚段等。

2. 数据段是B+树的叶子节点，索引段是B+树的非叶子节点。

### 4.2.3 区

1. 区是由连续页组成的空间，在任何情况下的大小都为1MB。但是页的大小可能不同。

### 4.2.4 页（块）

1. 是InnoDB磁盘管理的最小单位，可以通过`innodb_page_size`将页的大小设置为4K、8K、16K。若设置完成，则所有表中页的大小都为`innodb_page_size`，不可以对其再次进行修改。除非通过`mysqldump`导入和导出操作来产生新的库。

     常见页的类型：

    - 数据页`B-tree Node`
    - undo页`undo Log Page`
    - 系统页`System Page`
    - 事务数据页`Transaction system Page`
    - 插入缓冲位图页`Insert Buffer Bitmap`
    - 插入缓冲空闲列表页`Insert Buffer Free List`
    - 未压缩的二进制大对象页`Uncompressed BLOB Page`
    - 压缩的二进制大对象页`compressed BLOB Page`

### 4.2.5 行

InnoDB存储引擎是面向列的，也就是说数据是按行进行存放的。

## 4.3 InnoDB行记录格式

### 4.3.1 Compact行记录格式

1. 目的是为了高效的存储数据。

### 4.3.2 Redundant行记录格式

1. 是MySQL5.0之前的行记录方式，而了兼容之前版本的页格式。

### 4.3.3 行溢出数据

1. 一般情况下，InnoDB存储引擎的数据都是存放在页类型为B-tree node中，当发生行溢出时，数据存放在页类型为Uncompress BLOB页中。

<img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202112311545777.png" alt="image-20211231154548715" style="zoom:33%;" />

### 4.3.4 Compressed和Dynamic行记录格式

### 4.3.5 CHAR的行存储结构

1. 在多字节字符集情况下，CHAR和VARCHAR时机行存储基本是没有区别的。

## 4.4 InnoDB数据页结构

<img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202112311555725.png" alt="image-20211231155514679" style="zoom:33%;" />

## 4.5 Named File Formats机制

1. 处理不同版本下页结构兼容问题。

## 4.6 约束

### 4.6.1 数据完整性

1. 实体完整性保证表中有一个主键；
2. 域完整性保证每列数据的值满足特定的条件。
    1. 合适的数据类型
    2. 外键约束
    3. 触发器
    4. DEFAULT约束
3. 参照完整性保证两张表之间关系：外键或触发器。
4.  对于INNODB存储引擎本身而言，提供了以下几种约束：
    - `Primary Key`
    - `Unique Key`
    - `Foreign Key`
    - `Default`
    - `NOT NULL`

### 4.6.2 约束的创建和查找

1. 创建方式
    1. 表建立时进行定义
    2. ALERT TABLE命令创建约束

### 4.6.3 约束和索引的区别

约束更是一个逻辑的概念，保证数据的完整性；而索引是一个数据结构，既有逻辑上的概念，在数据库中还代表着物理存储方式。

### 4.6.4 对错误数据的约束

在某些默认设置下，MYSQL数据库允许非法的或者不正确的数据的插入或更新，又或者可以在数据库内部转换为一个合法的值，例如像对NOT NULL的字段插入一个NULL值，MYSQL数据库会将其改为0再进行插入，因此数据库本身没有对数据的正确性进行约束。

但是上述情况MySQL会发出警告，但是想设置为报错，必须设置参数`sql_mode`来严格审核输入的参数。

```sql
SET sql_mode = 'STRICT_TRANS_TABLES';
```

### 4.6.5 ENUM和SET约束

### 4.6.6 触发器与约束

1. 触发器的作用时在INSERT、DELETE、UPDATE命令之前或之后自动调用SQL命令或存储过程。

### 4.6.7 外键约束

```sql
CREATE TABLE parent(
	id INT NOT NULL
    PRIMARY KEY(id)
)ENGINE = INNODB;

CREATE TABLE child(
	id INT,
    parent_id INT,
    FOREIGN KEY(parent_id) REFERENCES parent(id) ON DELETE RESTRICT
)ENGINE = INNODB
```

被引用的表为父表，引用的表称为子表。外键定义时的ON DELETE 和 ON UPDATE表示在对父表进行DELETE 和 UPDATE操作时，对子表做的操作，可以定义的子表操作有：

- CASCADE：父表进行UPDATE或者DELETE操作时，子表也进行这种操作
- SET NULL：父表发生UPDATE或者DELETE操作时，子表对应的数据设置为NULL
- NO ACTION：父表发生DELETE或者UPDATE操作时，抛出错误，不允许这类操作发生。
- RESTRICT：父表发生DELETE或者UPDATE操作时，抛出错误，不允许这类操作发生。

## 4.7 视图

1. 是一个命名的虚表，由一个SQL查询来定义，可以当做表使用。与持久表不同，视图中的数据没有实际的物理存储。

    ```sql
    CREATE VIEW v_t
    AS
    SELECT * FROM t WHERE id < 0;
    ```

### 4.7.1 视图的作用

1. 被用作一个**抽象装置**，构成需不需要关系基表的结构，只需要按照视图定义来获取或者更新数据。在一定程度上是一个**安全层的**作用。
2. 也可以通过视图的定义更新基本表。

### 4.7.2 物化视图

1. 用于预先计算并保存多表链接或聚集等耗时较多的SQL操作结果在存储设备上。

## 4.8 分区表（Skip）

### 4.8.1 概述

 分区的过程是将一个表或索引分解为多个更小、更可管理的部分。就访问数据库的应用而言，从逻辑上只有一个表或者索引，但是在物理上这个表或者索引可能由数十个物理分区组成。没个分区都是独立的对象，可以独自处理，也可以作为一个更大对象的一部分进行处理。

 MYSQL数据库支持的分区类型为水平分区，即将同一张表中不同行的记录分配到不同的物理文件中。MYSQL的分区都是局部分区索引，一个分区中既存放了数据又存放了索引。而全局分区是指，数据存放在各个分区中，但是所有数据的索引放在一个对象中。

 查看数据库是否启动了分区功能

```sql
SHOW VARIABLES LIKE '%partition%'\G;
SHOW PLUGINS\G;
```

 使用分区，并不一定会是使得数据运行的更快。分区可能会给某些SQL语句性能提高，但是主要用于数据库高可用性的管理。

 MYSQL支持的分区

- RANGE分区：行数据基于属于一个给定连续区间的列值被放入分区。

- LIST分区：LIST分区面向的是离散的值

- HASH分区：根据用户自定义表达式返回值来进行分区

- KEY分区：根据MYSQL提供的哈希函数来进行分区。

    不论使用何种分区，表中存在在主键或者唯一索引时，分区列必须是唯一索引的一个组成部分。

### 4.8.2 分区类型

1. RANGE分区

    ```sql
    # id小于10 数据插入到p0分区， id大于等于10小于20，输入插入到p1分区
    CREATE TABLE t (
    	id INT
    )ENGINE=INNODB
    PARTITION BY RANGE(id)(
    	PARTITION p0 VALUES LESS THAN(10),
    	PARTITION p1 VALUES LESS THAN(20)
    );
    ```

     分区之后，表不再由一个ibd文件组成了，而是由多个分区Ibd文件组成。

     可以通过查询`infomation_scheme`架构下的`PARTITIONS`表来查看每个分区的具体信息

    ```sql
    SELECT * FROM information_scheme.PARTITIONS
    WHERE table_schema=database() AND table_name='t'\G;
    ```

     如果我们插入id为30的数值，会抛出异常，不让添加。所以我们需要再添加一个MAXVALUE的值的分区。

    ```sql
    ALTER TABLE t
    ADD PARTITION(
    	partition p2 values less than maxvalue);
    )
    ```

     RANGE主要用于日期列的分区，一个demo

    ```sql
    CREATE TABLE sales(
    	money INT UNSIGNED NOT NULL
        date DATETIME
    )ENGINE = INNODB
    PARTITION by RANGE(YEAR(date)) (
    	PARTITION p2008 VALUES LESS THAN (2009),
        PARTITION p2009 VALUES LESS THAN (2010),
        PARTITION p2010 VALUES LESS THAN (2011)
    );
    ```

     这样创建的好处是，管理sales这样表，如果要删除2008年的数据，不用执行SQL，只需删除2008年数据所在的分区即可。查询2008年的数据也会变快。

    

     对于sales这张分区表，设计按照每年每月进行分区

    ```sql
    CREATE TABLE sales(
    	money INT UNSIGNED NOT NULL,
        date DATETIME
    )ENGINE = INNODB
    PARTITION by RANGE(YEAR(date)*100+MONTH(date)) (
    	PARTITION p201001 VALUES LESS THAN (201001),
        PARTITION p201002 VALUES LESS THAN (201002),
        PARTITION p201003 VALUES LESS THAN (201003)
    );
    ```

     但是执行先SQL的时候还是去查找了3个分区

    ```sql
    EXPLAIN PARTITIONS
    SELECT * FROM sales
    WHERE date>='2010-01-01' AND date<='2010-01-31'\G;
    ```

     这个是由于对RANGE分区的查询，优化器只能对`YEAR()`，`TO_DAYS()`，`TO_SECONDS()`，`UNIX_TIMESTAMP()`这类函数进行优化选择。因此对于上述需求，应该更改为：

    ```sql
    CREATE TABLE sales(
    	money INT UNSIGNED NOT NULL,
        date DATETIME
    )ENGINE = INNODB
    PARTITION by RANGE(TO_DAYS(date)) (
    	PARTITION p201001 VALUES LESS THAN (TO_DAYS('2010-02-01')),
        PARTITION p201002 VALUES LESS THAN (TO_DAYS('2010-03-01')),
        PARTITION p201003 VALUES LESS THAN (TO_DAYS('2010-04-01'))
    );
    ```

2. LIST分区

     与RANGE分区很相似，只是分区列的值是离散的，而非连续的。

    ```sql
    CREATE TABLE t (
    	a INT,
        b INT
    )ENGINE = INNODB
    PARTITION BY LIST(b) (
    	PARTITION P0 VALUES IN （1,3,5，7,9），
        PARTITION P1 VALUES IN (0，2，4，6，8)
    );
    ```

3. HASH分区

     目的是将数据均匀地分布到预先定义的各个分区中，保证各分区的数据数量大致一样的。

     用户不需要指定列值，只要基于将要进行哈希分区的列值指定一个列值或者表达式，以及指定分区的表要被分割成的分区数量。

    ```sql
    CREATE TABLE t_hash(
    	a INT,
        b DATETIME
    )ENGING=INNODB
    # expr返回一个整数的表达式。仅仅是字段类型为MYSQL整形的列名
    PARTITION BY HASH (YEAR(b))
    # 划分分区的数量
    PARTITIONS 4
    ```

     其实就是使用取余的方式分配到不同的分区中。`mod(YEAR(b), 4)`

    

     还支持一种LINEAR HASH的分区，语法与HASH一致。但是进行分区的判断算法不同

    - 取大于分区数量的下一个2的幂值V，`V=POWER(2, CEILING(LOG(2, num)))`

    - 所在分区`N=YEAR('2010-04-01')&(V-1)`

        优势在于增加、删除、合并和拆分分区变得更加快捷，这有利于处理含有大量数据的表。缺点是，数据分布可能不是太均衡。

4. KEY分区

     和HASH分区相似，不同之处HASH使用用户定义的函数进行分区，KEY分区使用MYSQL数据库提供的函数进行分区，INNODB使用哈希函数。

    ```msyql
    CREATE TABLE t_hash(
    	a INT,
        b DATETIME
    )ENGING=INNODB
    PARTITION BY KEY (b)
    PARTITIONS 4
    ```

5. COLUMNS分区

     可以对多个列的值进行分区，可以支持的数据类型为：

    - 所有整型类型，浮点型不支持

    - 日期类型：DATE和DATETIME

    - 字符串类型：CHAR 、VARCHAR、BINARY、VARBINARY，不支持BLOB和TEXT

        可以用来替代RANGE 和LIST分区

    ```sql
    CREATE TABLE t_hash(
    	a INT,
        b DATETIME
    )ENGING=INNODB
    PARTITION BY RANGE COLUMNS(b)(
    	partition p0 values less than ('2009-01-01'),
        partition p1 values less than ('201--01-01'), 
    )
    PARTITIONS 4
    ```

    

### 4.8.3 子分区

 子分区是在分区的基础上再进行分区，有时也称这种分区为复合分区。MYSQL允许数据库在RANGE和LIST的分区上再进行HASH或KEY的子分区。

```sql
CREATE TABLE ts(a INT, b DATE)
PARTITION BY RANGE(YEAR(b))
SUBPARTITION BY HASH(TO_DAYS(b)) (
    PARTITION p0 VALUES LESS THAN (1990) (
    	SUBPARTITION S0,
        SUBPARTITION s1
    )
    PARTITION p1 VALUES LESS THAN (2000) (
    	SUBPARTITION S2,
        SUBPARTITION s3
    )
    PARTITION p2 VALUES LESS THAN MAXVALUE (
    	SUBPARTITION S4,
        SUBPARTITION s5
    )
);
```

 注意点：

- 每个子分区的数量必须相同
- 要在一个分区表的任何分区上使用`SUBPARTITION`来明确定义任何子分区，就必须定义所有的子分区。
- 每个`SUBPARTITION`子句必须包括子分区的一个名字。
- 子分区的名必须是唯一的。

### 4.8.4 分区中的NULL值

 MYSQL允许对NULL值做分区，在MYSQL中，NULL值视为小于任何一个非NULL值。

- 对于RANGE分区，如果插入了NULL值，会放在最左边的分区。

- 对于LIST分区，如果要是用NULL值，必须显式地指出哪个分区中放入NULL值，否则会报错。、

    ```sql
    CREATE TABLE t_list(
    	a INT,
        b INT
    )ENGINE=INNODB
    PARTITION BY LIST(b)(
    	PARTITION p0 VALUES IN (1,3,5,7,9, NULL),
        PARTITION p1 VALUES IN (0,2,4,6,8)
    )
    ```

- 对于HASH和KEY分区，任何分区函数都会将含有NULL值的记录返回为0.

### 4.8.5 分区和性能

 分区不一定会提升查询速度。

 数据库的应用分为两类：

- 在线事务处理`OLTP`
- 在线分析处理`OLAP`

 对于OLAP应用，分区可以提升查询性能。因为OLAP大多数查询需要频繁扫描一张很大的表。假设有一个一亿行的表，其中有时间戳属性，用户的查询需要从这张表中获取一年的数据。如果按时间戳进行分区。则只需要扫响应分区即可。

 对于OLTP应用，通常不会获取一张大表中10%的数据，大部分是通过索引返回几条记录即可。而根据B+树索引的原理可知，对于一张大表，一般的B+树需要2~3次的磁盘IO。因此B+树可以很好的完成操作，不需要分区的帮助。

### 4.8.6 在表和分区间交换数据

 `ALTER TABLE ... EXCHANGE PARTITION`可以让分区或子分区中的数据与另外一个非分区的表中的数据进行交换。

- 要交换的表需和分区表有着相同的表结构，但是表不能含有分区
- 在非分区表中的数据必须和在交换的分区定义内
- 被交换的表中不能含有外键，或者其他的表含有对该表的外键引用。
- 用户除了需要ALTER INSERT和CREATE权限外，还需要DROP权限
- 使用该语句时，不会触发交换表和被交换表上的触发器
- AUTO_INCREMENT列会被重置。

```sql
ALTER TABLE e EXCHANGE PARTITION p0 WITH TABLE e2;
```

## 4.9 小结

* 首先介绍了InnoDB存储引擎表总是按照主键索引顺序存放， 然后深入介绍表的物理实现。
* 接着介绍了表有关的约束问题，MySQL通过约束保证数据的各种完整性，也提到了外键特性。
* 之后介绍了视图，MySQL视图是虚拟表，本身不支持物化视图，但是通过一些技巧（触发器）可以实现一些简单的物化视图功能。
* 最后介绍分区。

# 第五章 索引与算法

## 5.1 InnoDB存储引擎索引概述

1. InnoDB支持常见的索引：
    1. B+树索引：只能查到具体的页，不能到行。把页读入到内存中进行查找。
    2. 全文索引
    3. 哈希索引：自适应

## 5.2 数据结构与算法

### 5.2.1 二分查找法

略

### 5.2.2 二叉查找树与平衡二叉树

1. B+树是通过二叉查找树，再由平衡二叉树，B树演化而来。

2. 在二叉查找树总左子树的键值总是小于根的键值，右子树的键值总是大于根的键值。所以中序遍历可以得到键值的排序输出。通过二叉查找树进行查找，性能还是可以的。但是二叉查找树的构造方式有很多，如果是下图，效率就很低:

    ![img](https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202202201427375.png)

3. 所以如果要最大性能构造一颗二叉查找树，需要这颗二叉查找树是平衡的，从而引出新定义——平衡二叉树，又称为AVL树。但是维护一颗AVL树代价很大，每次插入都需要通过左旋右旋来保证平衡。因此AVL多用于**内存结构**对象中，维护的开销相对较小。

## 5.3 B+树

1. 定义：是为**磁盘或其他直接存取**辅助设备设计的一种平衡查找树。所有记录节点都是按照键值的大小顺序存放在同一层叶子结点上，由各叶子结点指针进行连接。

    ### 5.3.1 B+树的插入操作

    1. 插入必须保证插入后叶子结点中的记录依然排序，同时要考虑到B+树的三种情况。

        <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201041447898.png" alt="image-20220104144700825" style="zoom:33%;" />

    2. e.g. 

        1. 插入28，满足第一种情况，直接插入即可
            
            <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201041505008.png" alt="image-20220104150526968" style="zoom:33%;" />
            
        2. 插入70，满足第二种情况，Leaf Page已满，Index Page没有满。插入Leaf Page后情况为50、55、60、65、70，并根据中间值60拆分叶子结点。
            
            <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201041508069.png" alt="image-20220104150828030" style="zoom:33%;" />
            
        3. 插入95，Leaf Page 和Index Page都满了，符合第三种情况，需要做两次拆分，如下图：
            
            <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201041509605.png" alt="image-20220104150934568" style="zoom:33%;" />
    
    3. 为了保持平衡会对于新插入的键值做很多的**<u>拆分页</u>**工作，而B+树主要用于磁盘，所以拆分页都是磁盘操作，**应该减少**。所以，B+树还提供了类似平衡二叉树的**<u>旋转</u>**操作。
        1. 旋转发生在Leaf Page已经满，但是其左右兄弟结点没有满的情况下。B+树会将记录转移到兄弟结点上。通常首先是左兄弟。图5-7插入70的情况：
            
            <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201041505008.png" alt="image-20220104150526968" style="zoom:33%;" />
            
            <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201041515647.png" alt="image-20220104151527593" style="zoom:33%;" />

### 5.3.2 B+树的删除操作

1. B+树使用**填充因子**控制树的产出变化，50%是可设的最小值。删除操作需要保证删除后叶子结点有序，也需要考虑三种情况：
    
    <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201041519899.png" alt="image-20220104151907858" style="zoom:33%;" />
    
2. e.g. 

    1. 根据图5-9的B+树进行操作。首先删除70这个记录，符合第一种情况：

    <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201041523932.png" alt="image-20220104152313894" style="zoom:33%;" />
    
    2. 接着删除25的记录，符合第一种情况，但是25还是Index Page的值，所以删除25后会将28更新到Page Index中：
    
    <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201041525581.png" alt="image-20220104152550545" style="zoom:33%;" />
    
    3. 最后删除60，删除后填充因子小于50%，需要合并操作。
    
    <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201041530212.png" alt="image-20220104153018176" style="zoom:33%;" />

## 5.4 B+树索引

1. B+树的高度一般在2-4层。
2. 分为聚集索引和辅助索引。区别在于叶子结点是否存放一整行信息。

### 5.4.1 聚集索引

1. InnoDB是索引组织表，表中数据按照主键顺序存放。而聚集索引就是按照每张表主键构造一棵B+树，同时叶子结点存放整张表的行记录数据。所以数据也是索引的一部分。
    1. 索引上直接找到数据，速度快
    2. 因为采用**<u>逻辑</u>**顺序（页和页的记录都是通过**<u>双向链表</u>**连接），适合范围查找。

### 5.4.2 辅助索引（非聚集索引）

1. 叶子结点不包含记录的全部数据，包含键值和书签，书签用来告诉InnoDB哪里可以找到对应的数据。由于InnoDB是索引组织表，所以InnoDB的书签就是相应数据的聚集索引键。
2. 当通过辅助索引来寻找数据时，InnoDB会遍历辅助索引并通过叶级别的指针获得指向主键索引的主键，然后再通过主键索引来找到一个完整的行记录。

### 5.4.3 B+树索引的分裂

1. InnoDB可以通过插入的顺序信息决定后续插入的分裂方向以及分裂点。

### 5.4.4 B+树索引的管理

#### 1. 索引管理

1. ```sql
    ALTER TABLE tb1_name
    | ADD {INDEX|KEY} [index_name]
    [index_type] (index_col_name, ...) [index_option] ...
    
    ALTER TABLE tb1_name
    DROP PRIMARY KEY
    | DROP {INDEX|KEY} index_name
    ```

2. 通过`CREATE/DROP INDEX`的语法同样很简单

    ```sql
    CREATE [UNIQUE] INDEX index_name
    [index_type]
    ON tab_name (index_col_name, ...)
    
    DROP INDEX index_name ON tb1_name
    ```

3. 创建前100个字段的索引

    ```sql
    ALTER TABLE t
    ADD KEY idx_b(b(100));
    ```

4. 创建联合索引

    ```sql
    ALTER TABLE t
    ADD KEY idx_a_c (a,c);
    ```

5. 查看表中索引信息

    ```sql
    SHOW INDEX FROM t;
    ```

    SHOW INDEX展现结果中每列的含义

    | name           | value                                                        |
    | -------------- | ------------------------------------------------------------ |
    | `table`        | 索引所在的表名                                               |
    | `non_unique`   | 非唯一的索引，可以看到primary key是0                         |
    | `key_name`     | 索引的名字，可以根据这个名字来drop index                     |
    | `sql_in_index` | 索引中该列的位置                                             |
    | `column_name`  | 索引列的名字                                                 |
    | `collation`    | 列以什么方式存储在索引中。B+树索引总是A，使用HEAP引擎，且使用HASH索引，这里会显示NULL。 |
    | `cardinality`  | 索引中唯一值的数目的估计值。cardinality表行数应尽可能接近1，如果非常小，用户需要考虑是否可以删除此索引。 |
    | `sub_part`     | 是否是列的部分索引                                           |
    | `packed`       | 关键字如何被压缩                                             |
    | `null`         | 索引列是否含有NULL                                           |
    | `index_type`   | 索引的类型。                                                 |
    | `comment`      | 注释                                                         |

    **Cardinality值非常关键**，优化器会根据这个值来判断是否使用这个索引。但是这个值不是实时更新的，代价太大，所以只是一个大概值，正确是和行数一致，需要立即更新的话使用命令：```analyze table t\G;```

    如果Cardinality为NULL，

    - 在某些情况下可能会发生索引建立了却没有用到的情况。

    - 对两条基本一样的语句执行EXPLAIN，但是最终出来的结果不一样，一个使用索引，另一个使用全表扫描。

        解决的最好方法，就是做一次ANALYZE TABLE的操作，建议是在一个非高峰时间。

#### 2. Fast Index Creation（FIC）

Mysql 5.5版本之前存在的一个普遍被人诟病的问题是MYSQL数据库对于索引的添加或者删除的这类操作，MYSQL数据库的操作过程为：

- 首先创建一张新的临时表，表结构为通过命令ALTER TABLE新定义的结构。
- 然后把原表中数据导入到临时表
- 接着删除原表
- 最后把临时表重名为原来的表名。

如果用户对于一张大表进行索引的添加和删除操作，会需要很长的时间，更关键的是，若有大量事务需要访问正在被修改的表，这意味着数据库服务不可用。

从InnoDB 1.0.X版本开始支持一种称为`fast index creation`快速索引创建的索引创建方式——简称FIC。

1. 限于辅助索引，对创建索引的表加S锁，创建过程不需要新建表，所以很快
2. 删除过程只需要更新内部视图，将辅助索引空间标为可用，并删除内部视图的对该表的索引定义即可。
2. 这里需要注意的是：临时表的创建路径是通过参数tmpdir进行设置的，用户必须保证tmpdir有足够的空间可以存放临时表，否则会导致创建索引失败。由于FIC在索引创建的过程中对表机上了S锁，因此在创建的过程中只能对该表进行读操作，若有大量的事务需要对目标库进行写操作，那么数据库的服务同样不可用。

#### 3. Online Schema Change

1. 在事务创建过程中可以对表进行读写。提高了原有MYSQL数据库在DDL操作时的并发性。是通过PHP脚本开发的。
2. 实现OSC步骤如下：
    - `init`：初始化阶段，对创建的表做一些验证工作，如检查表是否有主键，是否存在触发器或者外键等。
    - `createCopyTable`：创建和原始表结构一样的新表
    - `alterCopyTable`：对创建的新表进行ALTER TABLE操作，如添加索引或列等。
    - `createDeltasTable`：创建`deltas`表，该表的作用是为下一步创建的触发器所使用。之后对元彪的所有DML操作会被记录到`createDeltasTable`中。
    - `createTriggers`：对原表创建INSERT、UPDATE、DELETE操作的触发器。触发操作产生的记录被记录到的`deltas`表中。
    - `startSnpshotXact`：开始OSC操作的事务。
    - `selectTableIntoOutfile`：将原表的数据写入到新表。为了减少对原表的锁定时间，这里通过分片将数据输出到多个外部文件，然后将外部文件的数据导入到copy表中。分片的大小可以指定。
    - `dropNCIndexs`：在导入到新表前，删除新表中所有的辅助索引。
    - `loadCopyTable`：将导出的分片文件导入到新表。
    - `replayChanges`：将OSC过程中原表DML操作的记录应用到新表中，这些记录被保存在`deltas`表中。
    - `recreateNCIndexes`：重新创建辅助索引。
    - `replayChanges`：再次进行DML日志的回放操作，这些日志是在上述创建辅助索引中过程新产生的日志。
    - `swapTables`：将原表和新表交换名字，整个操作需要锁定2张表，不允许新的数据产生。由于改名是一个很快的操作，因此堵塞的时间非常短。
1. 有一定局限性，要求进行修改的表一定要有主键，且表本身不能存在外键和触发器。此外，在进行OSC过程中，允许`sql_bin_log=0`，因此所做的操作不会同步到slave服务器，可能导致主从不一致的情况。

#### 4. Online DDL

1. 创建索引时运行DML操作（INSERT、UPDATE、DELETE）

## 5.5 Cardinality 值

### 5.5.1 什么是Cardinality

1. 在访问表中很少一部分时使用B+树索引才有意义。e.g. 在性别字段，可取范围很小，称为低选择性。添加索引没有必要。如果某个<u>**字段取值范围很广**</u>，几乎没有重复，称为高选择性，此时使用b+树索引比较合适。
2. 如何确定高选择性——cardinality
    1. Cardinality：表示索引中不重复记录数量的<u>预估值</u>。

### 5.5.2 InnoDB存储引擎的Cardinality统计

1. MySQL有不同的存储引擎，每种存储引擎对于B+树索引的实现不同，所以对Cardinality的统计是放在存储引擎层进行的。
2. 由于有可能数据量很大，所以通过**<u>采样</u>**的方式完成。随机取8个叶子结点不同值的平均数。（因为随机，所以每次Cardinality值可能不一样）
3. 发生时机：INSERT和UPDATE，更新策略为如下的某一种：
    1. 表中1/16的数据已发生过变化
    2. stat_modified_counter > 2000000000

## 5.6 B+树索引的使用

### 5.6.1 不同应用中B+树索引的使用

* OLTP：通过索引取得少量数据
* OLAP：复杂查询，需要多表联接操作

### 5.6.2 联合索引

1. 联合索引：对多个列进行索引。

2. 好处：

    1. 对于查询SELECT * FROM TABLE WHERE a=XXX and b=XXX可以用（a,b)这个联合索引，也可以对SELECT * FROM TABLE WHERE a=XXX使用，但不能对SELECT * FROM TABLE WHERE b=XXX，因为b是无序的：
        
        <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201041939410.png" alt="image-20220104193957363" style="zoom:33%;" />
        
    2. 对第二个键值进行了排序处理，有时不需要额外的排序操作。
    
        1. e.g.  
    
            ```sql
            CREATE TABLE buy_log (
              	user_id INT UNSIGNED NOT NULL,
                buy_date DATE
            ) ENGINE=InnoDB
            
            INSERT INTO buy_log VALUES (1, '2009-01-01');
            INSERT INTO buy_log VALUES (2, '2009-01-01');
            INSERT INTO buy_log VALUES (3, '2009-01-01');
            INSERT INTO buy_log VALUES (1, '2009-02-01');
            INSERT INTO buy_log VALUES (3, '2009-02-01');
            INSERT INTO buy_log VALUES (1, '2009-03-01');
            INSERT INTO buy_log VALUES (1, '2009-04-01');
            
            ALTER TABLE buy_log ADD KEY (userid);
            ALTER TABLE buy_log ADD KEY(userid, buy_date);
            ```
    
            如果只对于userid进行查询，则优化器的选择有两个，单个的userid索引和（userid, buy_date) 联合索引，最终选择userid因为该索引的叶子结点<u>包含单个键值，所以一个页能存放更多的记录</u>。
    
        2. 接着如果要去除userid为1的近三次购买记录
    
            ```sql
            SELECT * FROM buy_log
            		WHERE userid=1 ORDER BY buy_date DESC LIMIT 3
            ```
    
            优化器也有两个可选，但这次选择了（userid, buy_date) 联合索引，因为这个联合索引中bu y_date已经排好序，无需再对buy_date进行额外排序操作。
    
        3. 如果强制使用userid，那么会使用一个排序才能完成查询。using filesort。

### 5.6.3 覆盖索引

1. 即从辅助索引中既可以得到查询记录，而不需要查询聚集索引中的记录。
2. 好处是不包含整行记录信息，其大小远小于聚集索引，减少IO操作。

```sql
# 对于InnodbDB存储引擎的辅助索引而言，由于包含了主键信息，因此其叶子节点存放的数据为(primary key1，primary key2，... ， key1， key2，...)，则下列语句可以使用一次辅助联合索引来完成。
SELECT KEY2 FROM table WHERE KEY1=xxx;
SELECT primary key2, KEY2 FROM table WHERE KEY1=xxx;
SELECT primary key1, KEY2 FROM table WHERE KEY1=xxx;
SELECT primary key1, primary key2, KEY2 FROM table WHERE KEY1=xxx;
```

### 5.6.4 优化器选择不使用索引的情况

1. 多发生于范围查找或JOIN连接情况下。（离散读取比较多的情况）

### 5.6.5 索引提示

1. 错误选择索引（很少发生）
2. 可选索引多，选择的时间开销大。

### 5.6.6 Multi-Range Read优化

1. 目的：减少磁盘随机访问，将随机方位转化为较为顺序的访问。

2. 工作方式：
    1. 将查询得到的辅助索引键值放入一个缓存（这时数据是根据辅助索引键值排序）
    2. 将缓存中的键值按照RowID排序
    3. 根据RowID顺序访问实际的数据文件
    
3. 场景一：如果缓冲池不够大，即不能放进去一张表的数据，频繁的操作会导致缓存中的页频繁被替换出，又被读进去。按照主键顺序可以减少此行为。

    ```select * from salaries where salary>10000 AND salary<40000;```

4. 场景二：将范围查询拆分为键值对，在拆分过程中过滤掉不符合查询条件的数据。

    ```select * from t  where key_part1 >= 1000 and key_part1 < 2000 and key_part2 = 10000;```

    这个表上有(key_part1， key_part2)的辅助联合索引，在不采用MRR的时候，会先按照`key_part1`在1000和2000的数据全部查出来，然后再按照`key_part2`进行过滤。所以这个时候启用MRR，他会先把过滤条件拆解为(1000,1000)，(1001,1000)……然后再进行查询。

### 5.6.7 Index Condition Pushdown（ICP）优化

1. 在支持ICP优化后，MYSQL数据库会在取出索引的同时，判断是否可以进行where条件的过滤，也就是将WHERE的部分过滤操作放在了存储引擎层。

## 5.7 哈希算法

### 5.7.1 哈希表

1. 直接寻址

    1. 问题：计算机容量限制

2. 哈希：根据函数算出槽位置

    1. 有可能碰撞
    2. 拉链法：放入链表

    <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201042012307.png" alt="image-20220104201208263" style="zoom:33%;" />

### 5.7.2 InnoDB中的哈希算法

1. 冲突机制：采用链表方式， 哈希函数采用除法散列（h(k) = h mod m）

### 5.7.3 自适应哈希索引

1. 数据库自己创建，DBA不干预

## 5.8 全文检索

### 5.8.1 概述

1. B+树可以通过索引字段的前缀查找，B+树索引是支持的，利用B+树索引就可以进行快速查询。但是不能查找包含某一单词的查询。所以需要进入全文检索技术`Full-Test Search`

    ```sql
    SELECT * FROM blog WHERE content like '%xxx%'
    ```

### 5.8.2 倒排索引

1. 倒排索引是全文检索的实现方式

2. 通过在辅助表```auxiliary table```存储单词和单词自身在一个或多个文档所在位置的映射。这通常利用关联数组实现，其拥有两种表现形式：

    - `invert file index`：{单词，单词所在文档的ID}

        <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201042022145.png" alt="image-20220104202230080" style="zoom:33%;" />

    - `full inverted index`：{单词，（单词所在文档的ID，在具体文档中的位置）} 不仅存储了ID，还存储了出现的位置。空间占用更多，但是功能更强。

        <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201042022396.png" alt="image-20220104202243354" style="zoom:33%;" />

### 5.8.3 InnoDB全文检索

1. innoDB采用full inverted index。
2. 信息放在辅助表（持久），存放在磁盘。为了提高全文检索的并行性能，共有六张`Auxiliary Table`辅助表，目前每张表根据word的`Latin`编码进行分区。另外还使用FTS index Cache（全文检索索引缓存（红黑树结构））提高性能。
    1. 表数据更新后，先导入到`FTS Index Cache`中，但是还没有更新到`Auxiliary Table`中。`InnoDB`存储引擎会批量对`Auxiliary Table`进行更新，而不是每次插入后更新一次`Auxiliary Table`。
    1. 当对全文检索进行查询时，`Auxiliary Table`首先将会在`FTS Index Cache`中对应的word字段合并到`Auxiliary Table`中，然后再进行查询。
3. FTS Document ID：**<u>与word进行映射</u>**。
4. 限制：
    1. 每张表只能有一个全文索引
    2. 多列组成的全文索引需要使用相同字符集和排序规则
    3. 不支持无单词界定符的语言

### 5.8.4 全文检索

语法为：

```sql
MATCH(col1, col2, ...) AGAINST (expr (serch_modifier))
search_modifier:
{
	IN NATURAL LANGUAGE MODE
	| IN NATURAL LANGUAGE MODE WITH QUERY EXPANSION
	| IN BOOLEAN MODE
	| WITH QUERY EXPANSIONS
}
```

#### 1. Natrual Language

 通过MACTH函数进行查询，默认采用的模式，表示查询带有指定word的文档。

```sql
select * from fts_a where match(body) against('Porridge' IN NATURAL LANGUAGE MODE)
select * from fts_a where match(body) against('Porridge')；
```

在where条件中使用MATCH函数，其返回结果是根据相关性进行降序排序的，即相关性最高的结果放在第一位。相关性的值是一个非负的浮点数字。0表示咩有任何相关行。

 相关性的计算依据的条件：

- word是否在文档中出现
- word在文档中出现的次数
- word在索引列中的数量
- 多少个文档包含该word

通过SQL语句查看相关性：

```sql
select fts_doc_id, body, 
Match(body) against('Porridge' IN NATURAL LANGUAGE MODE) as Relevance
from fts_a;
```

#### 2. Boolean

略

#### 3. Query Expansion

 支持全文索引的扩展查询。通过在查询短语中添加`WITH QUERY EXPANSION`或`IN NATURAL LANGUAGE MODE WITH QUERY EXPANSION`可以开启`bind query expansion`，查询分为两个阶段：

- 第一阶段：根据搜索的单词进行全文索引查询。
- 第二阶段：根据第一阶段产生的分词再进行一次全文检索的查询

## 5.9 小结

本章介绍了一些数据结构，全文索引。B+树索引和哈希索引，以及这些索引的使用和优化。

# 第六章 锁

## 6.1 什么是锁

1. 锁机制用于管理对**共享资源**的**并发访问**， 提供数据的完整性和一致性。

## 6.2 lock与latch

1. **latch**一般称为闩锁（轻量级锁），因为其要求锁定的时间必须非常短。又分为mutex互斥量和rwlock读写锁，保证并发**线程**操作临界资源的正确性，通常没有死锁检测的机制。
2. **lock**的对象是事务，用来锁定的是数据库中的对象（表、行、页）。被lock的对象仅在事务commit或rollback后进行释放。具有死锁机制。

## 6.3 InnoDB存储引擎中的锁

### 6.3.1 锁的类型

1. InnoDB实现了两种标准的行级锁：共享锁（S Lock）允许事务读一行数据，和排他锁（X Lock）允许事务删除或更新一行数据。

2. **锁兼容**：如果事务T1已经获得了行r的共享锁，那么另外的事务T2可以立即获得行的共享锁，因为读取没有改变r的数据。

3. 锁不兼容：如果事务T3想获得r的排他锁，则必须等待T1 T2释放r的共享锁。

    <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201051557498.png" alt="image-20220105155708355" style="zoom:33%;" />

4. **多粒度**锁定：支持表锁和行锁同时存在。

    1. 意向锁：将锁定的对象分为多个层次，意向锁意味着事务希望更细粒度上进行加锁。

        1. 意向共享锁（IS Lock）：事务想要获得一张表中某几行的共享锁

        2. 意向排他锁（IX Lock）：事务想要获得一张表中某几行的排他锁

            <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201051601896.png" alt="image-20220105160144859" style="zoom:33%;" />

### 6.3.2 一致性非锁定读

1. 指的是InnoDB通过行多版本控制，读取当前执行时间数据看中行的数据。如歌读取的行正在执行DELETE或UPDATE操作，这是读取操作不会因此去等待行上锁的释放，而去读取行的一个*快照*。

    <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201051606224.png" alt="image-20220105160647187" style="zoom:25%;" />

2. 该操作是通过undo段来完成。
3. 在READ COMMITTED和REPEATABLE READ（InnoDB默认事务隔离级别）下，存储引擎采用非锁定一致性读。
    1. 对快照的定义不同：
        1. READ COMMITED下， 总是读取最新的一份快照；
        2. REPEATABLE READ下，读取事务开始时的行数据版本。

### 6.3.3 一致性锁定读

1. 用户需要显式的对读取加锁：

    ```sql
    SELECT ... FOR UPDATE # X锁
    SELECT ... LOCK IN SHARE MODE # S锁
    ```

### 6.3.4 自增长与锁

1. AUTO-INC Locking：插入操作会依据自增长计数器值加1赋予自增长列。在完成对自增长值插入的SQL语句后立即释放。

2. 是一种特殊的表锁机制，为了提高**插入的性能**，锁不是在一个事务完成后才释放，而是在完成对自增长值插入的SQL语句后立即释放。虽然一定程度上提升了并发效率，但是还是有问题。

    - 事务依旧还是要等待前一个插入的完成（虽然不用等待事务的完成）。
    - 对于INSERT...SELECT的大数据量的插入会影响插入的性能，因为另一个事务的插入会被堵塞。

     5.1.22版本之后，提供了一种轻量级互斥量额自增长实现机制，大大提高了自增长插入的性能。提供参数`innodb_autoinc_lock_mode`来控制自增长的模式。

     先对自增长的插入进行分类：

    | 插入类型             | 说明                                                         |
    | -------------------- | ------------------------------------------------------------ |
    | `insert-like`        | 所有的插入语句                                               |
    | `simple inserts`     | 插入前就能确定插入行数的语句。不包括`INSERT ... ON DUPLICATE KEY UPDATE`这类SQL |
    | `bulk inserts`       | 指在插入前不能确定得到插入行数的语句，如`INSERT...SELECT`，`LOAD DATA` |
    | `mixed-mode inserts` | 指插入中有一部分的值是自增长的，有一部分是确定的。如         |

     分析参数`innodb_autoinc_lock_mode`以及各个设置下自增的影响，总共有三个有效值可以设定，即0、1、2。

    | `innodb_autoinc_lock_mode` | 说明                                                         |
    | -------------------------- | ------------------------------------------------------------ |
    | 0                          | 5.1.22版本之前自增长的实现方式                               |
    | 1                          | 参数默认值。对于`simple inserts`，会用互斥量去对内存中的计数器进行累加操作。对于`bulk inserts`使用传统的`AUTO-INC Locking`方式。这种配置下，如果不考虑回滚操作的话，自增列键值增长还是连续的。并且在这种方式下，`statement-based`方式的`replication`还是能很好的工作。 |
    | 2                          | 对于所有`INSERT-like`自增长值的产生都是通过互斥量而不是`AUTO-INC Locking`方式。显然，这是性能最高的方式，但是因为并发插入的存在，在每次插入时，自增长的值可能不是连续的。最终昂要的是，基于`Statement-Base Replication`会出现问题。因此，使用这个模式，任何时候都应该使用`row-base replication`。这样才能保证最大的并发性能以及`replication`主从数据的一致性 |

### 6.3.5 外键和锁

1. 外键的插入和更新，**首先查询父表的记录**，即SELECT父表，对于父表的SELECT操作，不是使用一致性非锁定读的方式，因为这样会发生数据不一致的问题，使用SELECT ... LOCK IN SHARE MODE，主动对父表加一个S锁。如果父表已经有X锁，子表的操作会被阻塞。

## 6.4 锁的算法

### 6.4.1 行锁的3种算法

1. Record Lock：单个记录上的锁。
2. Gap Lock：锁定一个范围不包括记录本身。
3. Next-key Lock：锁定一个范围+记录本身。为了解决幻读。如果索引具有唯一性，会优化降级为Record Lock。

### 6.4.2 解决幻读

`Phantom Problem`（幻读问题）：是指在同一事务下，连续执行两次同样的SQL语句可能导致不同的结果，第二次的SQL语句可能会返回之前不存在的行。

 在默认事务隔离级别`REPEATABLE READ`下，`INNODB`存储引擎采用`Next-Key Locking`来避免。例如：

```sql
CREATE TABLE t (a INT PRIMARY KEY)
BEGIN
SELECT * FROM T
WHRER a > 2 FOR UPDATE;
```

 这个事务中。对于a>2的范围加上了X锁，因此任何对和这个返回的插入都是不被允许的，所以避免的幻读。

## 6.5 锁问题

### 6.5.1 脏读

1. **脏数据**是指未提交的数据。
2. **脏读**指的是在不同的事务下，当前事务可以读到另外事务未提交的数据，简单来说就是可以读到脏数据。
3. 在事务隔离级别`READ UNCOMMITTED`下才会发生，但是实际生产并不会使用这个级别，但是可以将replication环境中的slave节点设置为这个级别，以提升性能。

### 6.5.2 不可重复读

1. 不可重复读是指在一个事务内多次读取同一数据集合。在这个事务还没有结束时，**另外一个事务**也访问了该同一数据集合，并做了一些DML操作。因此，在第一个事务中的两次读数据之间，由于第二个事务的修改，那么第一个事务两次读到的数据可能是不一样的。这样就发生了在一个事务内两次读到的数据是不一样的情况，这种情况称为不可重复读。

### 6.5.3 丢失更新

1. 就是一个事务的更新操作会被另一个事务的更新操作所覆盖，从而导致数据的不一致。

2. 在数据库层面，任何事务隔离界别都会通过加锁，来避免出现丢失更新的情况。一般出现在用户编程时发生。

- 事务T1查询一行的数据，放入本地内存，并显示给一个终端用户USER1
- 事务T2也查询一行的数据，放入本地内存，并显示给一个终端用户USER2
- USER1修改这行数据，更新数据库并提交
- USER2修改这行数据，更新数据库并提交

3. 解决方式，是将事务并行操作变成串行的操作。所以要对用户读取的记录上加一个排他X锁即可。

```sql
select cash into @cash
form account
where user = pUser FOR UPDATE;
```

## 6.6 阻塞

 因为不同锁之间的兼容性关系，在有些时刻一个事务中的锁需要等待另外一个事务中的锁释放它所占用的资源，这就是堵塞。

1. innoddb_lock_wait_timeout控制等待时间
2. innodb_rollback_on_timeout设定是否对等待超时的任务回滚，默认不回滚一般

## 6.7 死锁

### 6.7.1 死锁的概念

1. 死锁是指两个或两个以上的事务在执行过程中，因争夺锁资源而造成的一种互相等待现象。

2. 解决死锁问题最简单方式是不要有等待，将任何的等待都转化为回滚，并且事务重新开始。毫无疑问，这的确可以避免死锁问题的产生。但是性能很差。

3. 解决死锁问题最简单方法是超时，即当两个事务互相等待时，当一个等待时间超过阈值时，其中一个事务进行回滚，另外一个事务就能继续进行。虽然简单，但是仅仅根据FIFO进行回滚，若超时的事务占据权重比较大，会浪费较多时间。

4. 所以除了超时机制外，还普遍采用`wait-for graph`等待图的方式来进行死锁检测。这要求数据库保存以下两种信息：

    - 锁的信息链表

    - 事务等待链表

    <img src="https://images2018.cnblogs.com/blog/657755/201808/657755-20180830110854256-2110464959.png" alt="img" style="zoom:33%;" />

    通过上述链表可以构造出一张图，如果存在回路，那就代表存在死锁，因此资源间互相发生等待。在图中，事务T1执向T2边的定义为：

    - 事务T1等待事务T2所占用的资源

    - 事务T1最终等待T2所占用的资源，也就是事务之间在等待相同的资源，而事务T1发生在事务T2的后面。
        <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202202201422544.png" alt="img" style="zoom:50%;" />

### 6.7.2 死锁的概率

<img src="https://images2018.cnblogs.com/blog/657755/201808/657755-20180830110918207-1026598151.png" alt="img" style="zoom:50%;" />

事务发生死锁的概率与以下几点因素有关：

- 系统中事务的数量n，数量越多发生死锁的概率越大。
- 每个事务操作的数量r，每个事务操作的数量越多，发生死锁的概率越大
- 操作数据的集合R，越小则发生死锁的概率越大。

### 6.7.3 死锁的示例

## 6.8 锁升级

1. 将当前的锁的粒度降低，一般是为了避免锁的开销
2. InnoDB不存在这个问题，因为他是根据事务而不是记录产生锁的，不管是一个事务锁住一个记录还是多个记录开销是一致的。

## 6.9 小结

介绍了锁以及它带来的问题。

# 第七章 事务

事务将数据库从一种一致状态转为另一种一致状态。

## 7.1 认识事务

### 7.1.1 概述

1. 要么都做，要么不做
2. 满足ACID特性
3. InnoDB默认隔离级别为READ REPEATABLE
4. ACID
    1. A：原子性：整个数据库事务是不可分割的工作单位。
    2. C：一致性：事务将数据库从一种状态转变为下一种一致的状态。
    3. I：隔离性：要求每个读写事务的对象对其他事物的操作对象能互相分离。
    4. D：持久性：事务一旦提交，结果是永久性的。

### 7.1.2 分类

1. **扁平事务**：所有的操作处于同一层次，其间的操作是原子的。主要限制是不能提交或者回滚事务的某一部分，或分几个步骤提交。

<img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201111957971.png" alt="image-20220111195713706" style="zoom:25%;" />

2. **带有保存点的扁平事务**：除了支持扁平事务支持的操作，运行在事务执行过程中回到同一事务中较早的一个状态。

<img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201112001855.png" alt="image-20220111200150817" style="zoom:15%;" />

3. **链事务**：当系统崩溃时，保存点是易失的而非持久的。链事务的思想是：在提交一个事务时，释放不需要的数据对象，将必要的处理上下文隐式地传给下一个要开始的事务。
    1. 下一个事务的开始和当前事务的提交合并为一个操作，下一个事务将看到上一个事务的结果。
    2. 回滚仅限当前事务，COMMIT后释放当前事务的锁。
4. **嵌套事务**：层次框架结构。由顶层事务控制着各个层次的事务，子事务控制着局部变化。
    1. 子树可以是嵌套事务也可以是扁平事务。
    2. 也结点事务时扁平事务。跟到叶子距离可以不同。
    3. 子事务可以提交也可以回滚，但不会马上生效。子事务在顶层事务提交后才真正提交。
    4. 任意一个事务的回滚会引起子事务回滚，子事务仅保留ACI特性，没有D。

<img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201112008425.png" alt="image-20220111200831383" style="zoom:15%;" />

5. **分布式事务**：分布式环境下的扁平事务。

## 7.2 事务的实现

1. 隔离性由锁实现，原子性、一致性、持久性通过redo log和undo log实现。

### 7.2.1 redo

#### 1.基本概念

1. 重做日志实现事务的持久性。由重做日志缓冲（易失的）和重做日志文件（持久的）组成。
2. Force Log at Commit：当事务提交时，必须先将该事务的所有日志写入到重做日志文件进行持久化，待事务的COMMIT操作完成才算完成。
3. 为了保证每次日志都写入redo log，每次重做日志写入重做日志文件后，InnoDB都需要调用一次fsync操作。
    1. 重做日志缓冲先写入文件系统缓存，为了确保写入磁盘，必须进行次fsync，所以磁盘的性能决定了数据库性能。
4. 可以手动设置非持久性情况，提高性能。周期性的fsync而非每次提交，但也存在部分事务可能会被丢失的风险。
5. *binlog：MySQL层面产生的。

#### 2. log block

1. 重做日志文件、重做日志缓存都是以块的方式保存。称为重做日志块，大小为512字节。

    <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201112053269.png" alt="image-20220111205328198" style="zoom:15%;" />

#### 3. log group

1. 逻辑上的概念，由多个redo log file组成。
2. log buffer刷新内存中log block到磁盘的规则：
    1. 事务提交
    2. log buffer一半空间被使用
    3. log checkpoint时候

<img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201112059137.png" alt="image-20220111205908096" style="zoom:25%;" />

#### 4. 重做日志格式

<img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201112101797.png" alt="image-20220111210139754" style="zoom:35%;" />

#### 5. LSN

1. 日志序列号。占用8字节，单调递增。
2. 含义：重做日志写入总量checkpoint位置；页的版本。

#### 6. 恢复

### 7.2.2 undo

#### 1. 基本概念

1. undo存放在数据库内部的一个特殊段（segment）中，称为undo段，位于共享表空间内。

2. 将数据库逻辑的恢复到原来的样子
3. 用户执行<u>ROLLBACK</u>时候，会将事务回滚，但是内存表的大小不会收缩。实际上他做的事与原来相反的操作。
4. undo的另一个作用是<u>MVCC</u>。当用户读取一行时，如该记录一件被另一事务占用，当前事务可以通过undo读取之前的行版本信息，以此实现一致性非锁定读。
5. undo log会产生redo log，因为undo log也需要持久性的保护。

#### 2. undo存储管理

1. InnoDB有rollback segment，每个回滚段记录了1024个undo log segment，在每个undo log segment段中进行undo页的申请。
2. 事务提交后不能马上删除undo log及他所在的页。因为其他事务可能通过undo log来得到行记录之前的版本。是否可以删除由purge线程判断。

#### 3. undo log格式

1. undo log : 分为 insert undo log和update undo log。因为insert操作的记录只对事务本身可见，对其他事务不可见（隔离性）。该undo log可以在事务提交后直接删除，不需要进行purge操作。
2. update undo log: 对delete和update操作产生的`undo log`。该`undo log`可能需要提供MVCC机制，因此不能在事务提交时就进行删除。

#### 4. 查看undo信息

```sql
# 查看rollback segment
DESC INNODB_TRX_ROLLBACK_SEGMENT;

# 查看rollback segment所在的页
SELECT segment_id, space, page_no
from INNODB_TRX_ROLLBACK_SEGMENT;

# 记录事务对应的undo log
SELECT * FROM information_schema.INNODB_TRX_UNDO\G;
```



### 7.2.3 purge

1. delete操作只是把记录的delete flag设置为1，记录没有被删除。真正的删除是在purge操作。
2. 因为Innodb支持MVCC，记录不能在事务提交时立即进行处理。

### 7.2.4 group commit

1. 事务提交时会进行两个阶段的操作：

    1. 修改内存中事务对应的信息，并且将日志写入重做日志缓冲；
    2. 调用fsync将确保日志都从重做日志缓冲写入磁盘。

    步骤2相对步骤1慢，当有事务进行这个过程时，其他事务可以进行步骤1，完成步骤2后，大家使用1次fsync刷新到磁盘，减少磁盘压力。

## 7.3 事务控制语句

- `START TRANSACTION | BEGIN`：显式地开启一个事务
- `COMMIT`：提交事务
- `ROLLBACK`：回滚会结束用户的事务，并撤销正在进行的所有未提交的修改
- `SAVEPOINT identifier`：允许在事务中创建一个保存点，一个事务中可以有多个`SAVEPOINT`。
- `RELEASE SAVEPOINT identifier`：删除一个事务的保存点，当没有一个保存点执行这句语句时，会抛出一个异常。
- `ROLLBACK TO [SAVEPOINT] identifier`：与`SAVEPOINT`命令一起使用，可以把事务回滚到标记点，而不会管在此标记点之前的任何工作。
- ```SET TRANSACTION``` : 用来设置事务的隔离级别
    - `READ UNCOMMITTED`
    - `READ COMMITTED`
    - `REPEATABLE READ`
    - `SERIALIZABLE`

 `COMMIT WORK`: 用来控制事务结束后的行为是`CHAIN`还是`RELEASE`。如果是`CHAIN`方式，那么事务就变成了链事务。

 通过参数`completion_type`来控制

- 该参数默认为0，表示没有任何操作，这时和`COMMIT`是完全等价的
- 设置为1，等同于`COMMIT AND CHAIN`，表示马上自动开启一个相同隔离级别的事务。
- 设置为2 ，等同于`COMMIT AND RELEASE`，表示事务提交之后会自动断开与服务器的连接。

## 7.4 隐式提交的SQL语句

1. DDL（Data Definition Languages）语句
2. 用来隐式修改MySQL架构的操作
3. 管理语句

## 7.5 对于事务操作的统计

1. QPS `Question Per Second`：每秒请求数
2. 事务处理能力衡量 TPS：（rollback + commit）/ t

## 7.6 事务的隔离级别

1. SQL标准定义的四个隔离级别：READ UNCOMMITTED，READ COMMITTED，REPEATABLE READ，SERIALIZABLE
2. InnoDB 默认REPEATABLE READ，并且使用next-key lock避免幻读，已经达到SQL标准的串行化。

## 7.7 分布式事务

### 7.7.1 MySQL数据库的分布式事务

分布式事务使用两段式提交:

1. 第一阶段所有参与全局事务的阶段开始准备，告诉事务管理器他们准备好了。
1. 第二阶段事务管理器高速资源管理器执行rollback还是commit。如果任何一个节点不能提交，全部节点被告知回滚。

<img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202201162005841.png" alt="image-20220116200539759" style="zoom:23%;" />

### 7.7.2 内部XA事务

1. 存储引擎和插件之间或者存储引擎之间的分布式事务称为内部XA事务。e.g.binlog和InnoDB存储引擎之间。

## 7.8 不好的事务习惯

### 7.8.1 在循环中提交

```sql
CREATE PROCEDURE load(count INT UNSIGNED) 
BEGIN
DECLARE s INT UNSIGNED DEFAULT 1;
DECLARE c CHAR(80) DEFAULT REPEAT('a', 80);
WHILE S <= count DO
INSERT INTO t1 SELECT NULL, c;
SET S = S + 1;
END WHILE;
END;
```

每次的insert都会发生自动提交，如果用户插入10000条数据，在5000条是发生了错误，那这5000已存在的数据如何处理? 如果每次都提交，每次都需要写重做日志，影响效率。所以建议使用同一个事务:

```sql
CREATE PROCEDURE load(count INT UNSIGNED) 
BEGIN
DECLARE s INT UNSIGNED DEFAULT 1;
DECLARE c CHAR(80) DEFAULT REPEAT('a', 80);
START TRANSACTION;
WHILE S <= count DO
INSERT INTO t1 SELECT NULL, c;
SET S = S + 1;
END WHILE;
COMMIT;
END;
```

### 7.8.2 使用自动提交

编写应用程序开发时，最好把事务的控制权限交给开发人员，在程序端进行事务的开始和结束。

### 7.8.3 使用自动回滚

使用自动回滚之后，MYSQL在程序段是不会抛出异常信息的，不便于调试，所以一般存储过程中值存放逻辑操作即可。管理操作全部放在java中进行。

## 7.9 长事务

执行时间长的事务。

这边一般会将长事务拆解为多个小事务进行操作。这样子，如果发生是失败了，可以继续在失败的小事务上继续进行重试，而不用全部重试。节省时间。

## 7.10 小结

本章了解了什么事事务以及如何使用。事务遵循的ACID特性，进一步了解了实现原子性、隔离性的redo log和undo log。还了解了事务隔离级别。最后讨论如何正确的使用事务，最好的做法是把事务的START TRANSACTION、COMMIT、ROLLBACK交给程序完成，而非在存储过程内完成。

# 第八章 备份与恢复（Skip）

# 第九章 性能调优（Skip）

# 第十章 InnoDB存储引擎源代码编译和调试（Skip）

