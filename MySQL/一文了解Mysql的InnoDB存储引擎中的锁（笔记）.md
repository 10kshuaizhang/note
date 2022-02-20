

原文：https://zhuanlan.zhihu.com/p/47648412

本文主要内容：

* 介绍InnoDB中的锁的类型（X、S、IX、IS）。

* 解释为什么引入意向锁

* 行锁的三种算法：Record Lock，Gap Lock，Next-key Lock

# 1. InnoDB存储引擎中的锁

1. MySQL中的锁大概可以分为：

    1. ![img](https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202112132010549.jpg)

    2. 锁的对象是**事务**， 用来锁定数据库中的对象例如表页行。一般锁的对象在**commit或rollback**后释放，且有**死锁**机制。

2. InnoDB中的行锁：共享锁（S Lock）允许事务读一行数据；排他锁（X Lock）：允许事务删除或更新一行数据。

    1. **共享锁**：若事务T对数据对象A上S锁，则事务T可以读A但是不能修改A，其他事物只能再对A加S锁，不能加X锁，直到T释放A上的S锁。
    2. **排他锁**：若事务T对数据对象A加上X锁，事务T可以读A也可以修改A，其他事务不能再对A加任何锁，直到T释放A上的锁。
        1. 以上情况称为**锁不兼容**

3. **意向锁**：多粒度锁定。允许行级锁和表级锁同时存在。

    1. 将锁定的对象分成多个层次。可以看成是树形结构，对最下层的对象上锁必须先对他的上层节点上锁。
    2. 为什么加意向锁
        1. 如果事务T要对表T1加X锁，那么他必须判断T1表下每一行是否加了S锁或者X锁（因为锁的不兼容性），这样会导致效率比较低。
        2. 如果事务T要对表T1加X锁，在这之前已经有事务对表T1中的行记录R加了S锁，那么此时在表T1上有IS锁，当前事务T对T1准备加X锁时候，由于X锁与IS锁不兼容，T要等到锁操作完成，节省了遍历操作，提升锁定父节点的效率。
        3. <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202112132023529.jpg" alt="img" style="zoom:50%;" />

# 2. 锁的算法

1. 3个：

    1. Record Lock：单个行记录上的锁
    2. Gap Lock：间隙锁，锁定一个范围，但不包含记录本身
    3. Next-Key Lock：Gap Lock + Record Lock，锁定一个范围，并包含记录本身。为了解决**幻读**问题。

2. 对于next-key lock，当锁定一个行，且**索引唯一**，InnoDB会把他**优化成Record Lock**；如果不是唯一索引，则按照本来的规则锁定一个范围和记录。

    1. note：当唯一索引是多个列组成，这时候不会优化降级。

3. [MySQL的一致性非锁定读和一致性锁定读](https://www.jianshu.com/p/45900fe75e51)

4. InnoDB默认事务隔离级别是RR级别，**<u>可重复读</u>**。该级别下采用next key lock加锁，可以防止幻读现象。

    1. > 幻读：在同一事务下，连续执行两次**同样的SQL语句**，可能导致不同的结果，第二次的SQL语句可能会返回之前不存在的行。

    2. e.g.

        1. ```sql
            create table t (a int primary key);
            insert into t select 1;
            insert into t select 2;
            insert into t select 5;
            ```

            假设有如下执行序列：

            ![preview](https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202112141948766.jpg)

​					会话A在时间2查询的结果为5， 由于使用了select ... for update语句，这就为（2， +∞）范围加了X锁。因此对于这个范围内的插入是不允许的，由于4在这个范围，不允许被插入，也就避免了幻读。



































