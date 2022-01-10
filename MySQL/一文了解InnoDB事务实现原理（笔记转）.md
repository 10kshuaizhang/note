# InnoDB事务实现原理

原文：https://zhuanlan.zhihu.com/p/48327345

ACD三个特性是通过Redo log和Undo log实现的，隔离性是通过锁实现的。

## 1. Redo Log

1. 重做日志用来实现事务的持久性。包括两部分：
    1. <u>**内存中的重做日志缓冲**</u>
    2. <u>**重做日志文件**</u>
    
2. InnoDB在事务提交时，将该事务的所有日志写入到redo日志文件中，待事务<u>commit</u>完成才算事务操作完成。

3. 每次将redo log buffer写入redo log file后，都需要一次<u>fsync</u>操作，因为重做日志缓冲只是先把内容先写入到操作系统的缓冲系统中，并没有确保写入磁盘，必须进行一次fsync操作，所以磁盘的性能在一定程度上决定了事务提交的性能。
    1. redo log buffer刷入磁盘是有一定规则的：事务提交时，log buffer一半空间被使用时， log checkpoint时。
    
4. <u>redo log file</u>
    
    1. 以块block保存（512字节），和磁盘扇区大小一样，因此redo log写入可以保证**原子性**，不需要保证double write。
    
    2. 内容上除了日志记录本身，还有日志块头（12字节）和日志块尾（8字节）两部分
    
        <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202112191130098.png" alt="image-20211219113024994" style="zoom:15%;" />
    
    4. 由于innodb存储引擎的存储管理是基于页的，所以重做日志格式也是基于页的。他们还有通用的头部格式：
        
        <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202112191132815.jpg" alt="img" style="zoom:33%;" />
        
        1. 之后是redo log body，根据重做日志类型不同，会有不同的存储内容。
    
5. <u>LSN： Log Sequence Number</u>日志序列号，8个字节，单调递增
    
    1. 表示事务写入重做日志字节的总量。
    2. 页中的LSN可以判断页是否需要进行恢复操作。
    
6. 恢复操作
    1. 不管是否正常关闭， InnoDB都会尝试恢复操作。
    
    2. e.g. checkpoint表示已经刷新到磁盘页上的LSN， 因此在恢复过程中仅需恢复checkpoint开始的日志部分：点那个数据库在checkpoint的LSN 为 10000时发生宕机， 恢复操作进恢复LSN10000到13000范围内的日志：
        
        <img src="https://pic4.zhimg.com/80/v2-2ada545e4563563a1dfc096c5a402e0b_1440w.jpg" alt="img" style="zoom:25%;" />

## 2. Undo Log

1. 实现两个功能：

    1. <u>**实现事务回滚**</u>
    2. <u>**实现MVCC**</u>

2. 他是逻辑日志，而redo log 是物理日志。

3. 当执行**<u>回滚</u>**时，就可以从undo log中的逻辑记录读取到相应操作并回滚。当应用到**<u>版本控制</u>**的时候：当某一行被其他事务锁定，它可以从undo log中分析出该行记录以前的数据是什么，从而提供该行的版本信息，帮助用户实现**一致性非锁定读取**。

4. e.g.

    1. InnoDB为每行记录都实现了三个隐藏字段来实现MVCC
        1. 6字节的事务ID；
        2. 7字节的回滚指针，指向写到rollback segment的一条undo 记录；
        3. 隐藏的id
        
    2. 初始的数据行：
        1. F1～F6是某行列的名字，1～6是其对应的数据。后面三个隐含字段分别对应该行的事务号和回滚指针，假如这条数据是刚INSERT的，可以认为ID为1，其他两个字段为空：
        
            <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202112191145705.jpg" alt="img" style="zoom:50%;" />
        
    3. 事务1更改各行的各字段的值
    
        1. 当事务1更改该行的值时，会进行如下操作：
            - 用排他锁锁定该行
            - 记录redo log
            - 把该行修改前的值Copy（可以理解成Copy，不要纠结前面说反向更新这里说复制，原理是一样的）到undo log，即上图中下面的行
            - 修改当前行的值，填写事务编号，使回滚指针指向undo log中的修改前的行。
            
            <img src="https://pic4.zhimg.com/80/v2-f52db177c809db0772a6a4e3b53c1b57_1440w.jpg" alt="img" style="zoom:50%;" />
    
    4. 事务2修改该行的值
    
        1. 与事务1相同，此时undo log，中有有两行记录，并且通过回滚指针连在一起。
    
            这些通过回滚指针联系起来的行相当于是数据的多个快照，从而实现MVCC的一致性非锁定读了。
    
            <img src="https://pic2.zhimg.com/80/v2-8f54037947575cfbd8cf02725cdee19d_1440w.jpg" alt="img" style="zoom: 20%;" />
    
    5. 隐藏列保存了行的船舰时间和过期时间（非实际时间，数据库版本号， 每开始一个事务，版本号自增）。
    
    6. 在RR隔离级别下， MVCC操作如下
    
        1. **SELECT**：Innodb会根据以下两个条件检查每行记录：
            1. 只差找版本早于当前事务版本的数据行，可以确保读取到的是在事务前就存在的；
            2. 行删除版本，要么为定义要么大于当前事务版本，可以确保读取到的是未删除的数据。
        2. **INSERT**：为插入的数据保存当前版本号作为行版本号；
        3. **DELETE**：为删除的的数据保存当前版本号作为行删除标识；
        4. **UPDATE**：为插入的新纪录的数据保存当前版本号作为行版本号，同时保存当前版本号作为行删除标识；
    
    7. 答疑解惑：

        1. 按照之前的Select规则，会话B 的事务是在 会话A的后面开启的，那么B的事务版本号大于A的事务版本号。这样在A中插入的数据在未提交的情况下，B可以读到A修改的数据，这不就自相矛盾了么？
        
            <img src="https://pic3.zhimg.com/80/v2-81b3f05ad1dd6c031206189404ba5b7e_1440w.jpg" alt="img" style="zoom:25%;" />
        
        3. read view确认一个记录是否看到有两个法则：
            1. 办不到read view创建时刻以后启动的事务；
            2. 看不到read view创建时活跃的事务
            
        4. 对于Session A，start transaction时并没有创建read view，而是在update语句才创建。所以Session A 的read view创建时间要比Session B的晚。所以B是不会看到A的操作的。因此防止了不可重复读。
