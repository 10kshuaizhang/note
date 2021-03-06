# 1. 什么是对象

## 1.1 抽象的历程

1. 程序员需要在机器模型（计算机）和问题空间（问题实际存在之处）建立联系。
2. 通过建立“对象”，<u>程序将自己改变为描述问题的语言</u>。
3. SmallTalk的5个基本特征：
    1. <u>万物皆对象</u>。可以存储数据，执行操作。
    2. <u>一段程序实际上就是多个对象通过发送消息通知彼此要干什么</u>。“发送消息”其实是请求对象的某个方法。
    3. <u>内存角度而言，每一个对象是由其他更为基础的对象组成的。</u>
    4. <u>每一个对象都有类型。</u>对象的类型就是他所依据生成实例的**类**。
    5. <u>同一类型的对象可以接受相同的消息。</u>
4. 对象有**状态、行为及标识。**

## 1.2 对象具有接口

1. 所有的对象都能够归为某一类，并且一类对象拥有一些共同的行为和特征。
2. 对象能够接收什么请求是由“接口（interface）”决定的。
3. 统一建模语言。（UML）
    1. 每一个类都表示为一个方块，方块头不是类名，中部是你想要描述的数据成员，而方法则位于方块的底部（通常只有公有方法）。

## 1.3 对象可以提供服务

1. 将对象视为服务提供者。

## 1.4 隐藏的实现

1. 类的创建者和客户程序员。
2. 设置访问控制
    1. 是防止客户程序员接触到他们不该触碰的内容。
    2. 库的设计者在改变类内部机制时，不担心影响到使用该类的客户程序员。

## 1.5 复用实现

1. 组合：<u>利用已有的类组合成新类。</u>

## 1.6 继承

1. 可以通过基类呈现核心思想，从基类派生出的众多子类则为其核心思想提供不同的实现方式。

### is-a关系与is-like-a关系

2. is-a里氏替换原则：只重写基类中定义的方法，可以直接用子类的对象代替基类的对象。
3. is-like-a：在子类新增了接口

## 1.7 多态

1. 非面向对象：前期绑定：会生成对具体方法名的调用，决定了被执行代码的绝对地址。
2. 对于继承而言，程序直到运行时才能明确代码地址
3. 面向对象：后期绑定。编译器会确保被调用的代码存在，并且对该方法的参数和返回值进行类型检查，但是她并不知道执行的是哪段代码。
4. 向上转型：将子类视为基类的过程。

## 1.8 单根层次结构

1. 有利于实现垃圾收集器。

## 1.9 集合

1. 集合的选择：
    1. 不同的集合提供不同类型的接口和行为。
    2. 不同集合在特定操作的执行效率也会有差异。

### 参数化类型（泛型）

2. 可以让编译器自动适配特定的类型。

## 1.10 对象的创建和生命周期

1. 垃圾收集器：处理内存释放的相关问题。

## 1.11 异常处理

## 1.12 总结 

# 2. Java安装

# 3. 对象无处不在

## 3.1 通过引用操作对象

我们日常实际操作的实际是对象的引用。

## 3.2 必须创建所有对象

### 3.2.1 数据保存在哪

1. 寄存器
2. 栈
3. 堆
4. 常量存储
5. 非RAM存储
    1. 序列化对象
    2. 持久化对象

### 3.2.2 特殊情况：基本类型

1. 自动装箱和拆箱
2. 高精度数字

### 3.2.3 Java中的数组

## 3.3 注释

## 3.4 无须销毁对象

### 3.4.1 作用域

### 3.4.2 对象的作用域

## 3.5 使用class关键字创建新类型

1. 字段：数据变量和方法
2. 基本类型的默认值：作为类成员时候

## 3.6 方法、参数和返回值

1. 方法决定了对象可以接收哪些信息
    1. 函数签名：方法名+参数类型，方法唯一标识符

2. 参数列表

## 3.7 编写Java程序 

### 3.7.1 名称可见性

1. 命名空间

    域名反转使用，全小写

### 3.7.2 使用其他组件

import

### 3.7.3 static关键字

1. 情况：
    1. 需要一小块共享空间保存特定字段，需要很少或者不需要对象的创建。
    2. 需要使用一个类的方法，而此方法和具体的类无关
2. static变量或方法可以通过对象或者是直接调用。

## 3.8 第一个java程序

## 3.9 编程风格

驼峰命名法

## 3.10 总结



