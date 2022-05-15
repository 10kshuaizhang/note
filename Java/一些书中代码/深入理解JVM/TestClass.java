/**
 * 被动使用类字段演示一：
 * 通过子类引用父类的静态字段，不会导致子类初始化
 */
public class TestClass {
//    public class Parent {
//        public static int A = 1;
//        static {
//            A = 2;
//        }
//    }
//
//
//    static class Sub extends Parent {
//        public static int B = A;
//    }
//
//    public static void main(String[] args) {
//        System.out.println(Sub.);
//    }

    static {
        i = 0; // 给变量赋值可以正常编译通过
//        System.out.print(i); // 这句话编译器会提示"非法向前引用"
    }
    static int i = 1;

    public static class SuperClass {
        static {
            System.out.println("Superclass init");
        }

        public static int value = 123;
    }

    public static class SubClass extends SuperClass{
        static {
            System.out.println("Subclass init");
        }
    }



    public static void main(String[] args) {
//        System.out.println(SubClass.value);
        SuperClass[] sca = new SuperClass[10];
    }
}
