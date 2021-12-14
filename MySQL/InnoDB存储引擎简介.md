# InnoDB体系架构

[一文了解InnoDB存储引擎](https://zhuanlan.zhihu.com/p/47581960) 读后笔记 

主要内容：

- InnoDB体系架构
- CheckPoint技术
- InnoDB关键特性

1. 从MySQL5.5开始，InnoDB是默认的表存储引擎，特点是**行锁设计、支持MVCC、支持外键、提供一致性非锁定读、同时被设计用来有效利用以及使用内存和CPU**
2. 存储引擎内部有多个内存块， 组成一个内存池， 后台线程负责刷新内存池中的数据、将已经修改的数据刷新到磁盘等等。
    1. <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202112121826152.jpeg" style="zoom:33%;" />
    2. 后台线程：有多个不同的线程，负责不同的任务。
        1. Master Thread
            1. 核心线程，**负责将缓冲池中的数据异步刷新到磁盘**，保证数据一致性，包括脏页的刷新、合并插入缓冲、Undo页的回收等。
        2. IO Thread
            1. InnoDB大量使用异步IO请求， 这个线程主要**处理这些请求的回调**。
        3. Purge Thread
            1. 回收已经使用并分配的undo页。
            2. 支持多个Purge Thread，加快undo页的回收。
        4. Page Cleaner Thread
            1. 将之前版本中**脏页的刷新操作**都放入单独的线程中来完成，减轻主线程的负担--对于用户查询线程的阻塞。
    3. 内存
        1. InnoDB是基于磁盘存储的，但是由于CPU和磁盘速度之间的差异，它使用**缓冲池**（Buffer Pool）提升性能。
            1. 缓冲池：一块内存区域
            2. 过程：读取操作中，将从磁盘读取到的页放在缓冲池，下次再读时
                * 在缓冲池中，直接读取
                * 不在，读取磁盘上的页。
            3. 缓存池中的数据页类型：**索引页、数据页**、undo页、插入缓冲、自适应哈希索引、InnodDB的锁信息、数据字典等。
            4. 页的大小默认16KB。
        2. 如何存储到缓存池？
            1. InnoDB为每个页创建控制信息（表空间编号、页号、页的buffer pool的地址、*一些锁信息和LSN信息*）
            2. 控制块和缓存页是一一对应的，在缓存池中，控制块在前。
                1. <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202112131946903.jpeg" alt="v2-6f6a5ebdfc6f356392d20d2f639415be_1440w" style="zoom:50%;" />
        3. 缓冲池的管理
            * **free list**
                * 最初启动mySQL服务器的时候，还没有磁盘页放到缓存池
                * 随着程序运行，磁盘上的页存到buffer pool
                * **Free链表**记录可用的页。当刚初始化，所有的页都是空闲的。
                * <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202112131948531.jpeg" alt="v2-65102c79f334d6b9c490ebf94542833b_1440w" style="zoom:25%;" />
                    * 链表的节点记录控制块的地址，控制块记录缓存页地址，相当于链表对应一个空闲的缓存页
                * 当从磁盘加载一个页时， 从空闲链表取出一个节点。
            * 当free list用完，即分配的缓存池大小用完，需要移除一些缓存页，为了提高后续读写的**缓存命中率**， InnoDB Buffer Pool采用**LRU算法**进行页面淘汰。
                * 如何实现--引入一个LRU链表，当我们访问一个页时：
                    * 如果存在缓存池中，直接把该页对应的LRU 链表节点移动到链表头部；
                    * 如果不在，将该页从磁盘加载到缓存池，并把该缓存页包装成节点放到链表头部。
                * 上述方式存在**性能**问题，例如一次全表扫描就把数据页大换血
                * InnoDB做了优化，加入了midpoint。新读取到的页不插入在头部，而是LRU列表的midpoint，这个算法称之为**midpoint insertion stategy**。默认配置插入到列表长度的5/8处。midpoint由参数innodb_old_blocks_pct控制。
                * midpoint之前的是new列表，之后的是old，new中的可理解为热点数据。
                * 同时InnoDB存储引擎还引入了innodb_old_blocks_time来表示页读取到mid位置之后需要等待多久才会被加入到LRU列表的热端。可以通过设置该参数保证热点数据不轻易被刷出。
        4. **脏页**
            1. 缓存池中被更新的页。
            2. 如何同步到磁盘
                1. 更新就同步：性能极差。
                2. 未来的某个时间点，后台线程依次刷新，修改到磁盘。
            3. 如何区分脏页？
                1. 脏页链表，也叫FLUSH链表，在LRU中修改过的都需要加入，不需要重复放入。（指向的是LRU链表的页）
                2. 在FLUSH链表中的脏页是根据oldest_lsn（这个值表示这个页第一次被更改时的lsn号，对应值oldest_modification，每个页头部记录）进行排序刷新到磁盘的，值越小表示要最先被刷新，避免数据不一致。

# CheckPoint技术

1. 解决的问题
    1. 缩短数据库恢复时间
    2. 缓冲池不够用的时候，将脏页刷新到磁盘
    3. 重做日志不可用时，刷新脏页
2. 如何缩短数据库恢复时间？
    1. redo log中记录checkpoint的位置，这个点之前的页已经刷新回磁盘，只需对它之后的日志进行恢复。这样缩短了恢复时间。
3. buffer pool不够用的时候，根据LRU算法溢出最近最少使用的页，如果页为脏页，强制执行checkpoint将脏页刷回磁盘。
4. 重做日志不可用，是指重做日志的这部分不可以被覆盖，为什么？因为：由于重做日志的设计是循环使用的。这部分对应的数据还未刷新到磁盘上。数据库恢复时，如果不需要这部分日志，即可被覆盖；如果需要，必须强制执行checkpoint，将缓冲池中的页至少刷新到当前重做日志的位置。
5. checkpoint每次刷新多少页到磁盘？每次从哪里取脏页？什么时间触发checkpoint？
    1. **Sharp Checkpoint**：发生在数据库关闭时关闭时，将**所有**的脏页刷回磁盘。（默认）
    2. **Fuzzy Checkpoint**：数据库运行时使用，刷新部分脏页。
        1. 主线程checkpoint：异步刷新（查询线程不受阻），每秒或每10s从缓冲池脏页刷新一定比例的页回磁盘。
        2. FLUSH_LRU_LIST Checkpoint：InnoDB存储引擎需要保证LRU列表中差不多有100个空闲页可供使用。
            1. 在InnoDB 1.1.x版本之前，用户查询线程会检查LRU列表是否有足够的空间操作。如果没有，根据LRU算法，溢出LRU列表尾端的页，如果这些页有脏页，需要进行checkpoint。因此叫：flush_lru_list checkpoint。
            2. InnoDB 1.2.x开始，这个检查放在了单独的进程（Page Cleaner）中进行。
                * 好处：1.减少master Thread的压力 2.减轻用户线程阻塞。
        3. Async/Sync Flush Checkpoint：指**重做日志不可用**的情况，需要强制刷新页回磁盘，此时的页是脏页列表选取的，保证redo log的可用性。
            * InnoDB存储引擎，通过LSN（Log Sequence Number）来标记版本，LSN是8字节的数字。每个页有LSN，重做日志有LSN，checkpoint有LSN。
            * **写入日志的LSN:redo_lsn**，**刷新回磁盘的最新页LSN:checkpoint_lsn**， 有如下定义:
                * checkpoint_age = redo_lsn - checkpoint_lsn
                * async_water_mark = 75% * total_redo_file_size
                * sync_water_mark = 90% * total_redo_file_size
                * 刷新过程如下图所示：

<img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202112121826322.png" style="zoom:50%;" />

4. Dirty Page too much Checkpoint: 脏页太多，强制checkpoint，保证缓冲池有足够可用的页。
    * 参数设置：innodb_max_dirty_pages_pct = 75 表示：当缓冲池中脏页的数量占75%时，强制checkpoint。

## InnoDB关键特性

1. **插入缓冲**

    1. 一般情况下插入是按照主键递增的顺序进行，插入聚集索引也是顺序的，不需要随机读取，因此速度比较快的。

    2. 如果索引非聚集且不唯一，需要离散访问（**B+树**的特性导致），导致插入操作性能下降。

    3. insert buffer

        1. 查询buffer pool，**非聚集索引页**存在，直接插入

        2. 不存在，放入缓冲池，然后以一定频率进行插入缓冲和辅助索引叶子结点的合并操作，这样可以将多个插入合并到一个操作中（因为在一个索引页中），提升课非聚集索引的插入性能。

        3. 需要满足的条件：

            * 索引是辅助索引；

            * 索引不是唯一的：在插入缓冲时，数据库并不去查找索引页来判断插入的记录的唯一性。如果去查找肯定又会有离散读取的情况发生，从而导致Insert Buffer失去了意义。

2. **两次写**-提高可靠性

    <img src="https://raw.githubusercontent.com/10kshuaizhang/note-images/main/202112121826859.png" style="zoom:50%;" />

    * 两次写需要额外添加两个部分：
        1）内存中的**两次写缓冲**（doublewrite buffer），大小为2MB
        2）磁盘上**共享表空间中连续的128页**，大小也为2MB

    * **原理**

        1. 当刷新缓冲池脏页时，并不直接写到数据文件中，而是先拷贝至内存中的两次写缓冲区;
        2. 接着从两次写缓冲区分两次写入磁盘共享表空间中，每次写入1MB;(Q:如果此时发生断电呢，副本没有写完成)
        3. 待第2步完成后，再将两次写缓冲区写入数据文件

        这样就可以解决上文提到的部分写失效的问题，因为在磁盘共享表空间中已有数据页副本拷贝，如果数据库在页写入数据文件的过程中宕机，在实例恢复时，可以从共享表空间中找到该页副本，将其拷贝覆盖原有的数据页，再应用重做日志即可。

3. **自适应哈希索引**

    1. InnoDB存储引擎会监控对表上各索引页的查询。如果观察到建立哈希索引可以提升速度，这简历哈希索引，称之为自适应哈希索引(Adaptive Hash Index, AHI)。AHI是通过缓冲池的B+树页构造而来的，因此建立的速度非常快，且不需要对整张表构建哈希索引。
    2. InnoDB存储引擎会自动根据访问的频率和模式来自动的为某些热点页建立哈希索引。
    3. AHI有一个要求，对这个页的连续访问模式(查询条件)必须一样的。例如联合索引(a,b)其访问模式可以有以下情况:
        - WHERE a=XXX;
        - WHERE a=xxx AND b=xxx。
            若交替进行上述两张查询，InnoDB存储引擎不会对该页构造AHI。此外AHI还有如下要求：
        - 以该模式访问了100次；

4. **异步IO（AIO）**

    1. 不需要等一个IO完成再进行下一个，而是全部发送IO请求，然后等待所有完成。
    2. IO merge操作：多个IO合并为一个IO操作，这样可以提高IOPS的性能。
    3. 应用：read ahead；脏页的刷新

5. **刷新邻接页**

    * InnoDB存储引擎在刷新一个脏页时，会检测该页所在区(extent)的所有页，如果是脏页，那么一起刷新。这样做的好处是通过AIO可以将多个IO写操作合并为一个IO操作。

