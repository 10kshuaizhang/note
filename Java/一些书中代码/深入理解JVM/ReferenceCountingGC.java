public class ReferenceCountingGC {
    /**
     * objA  和 objB会不会被GC呢？
     */

    public Object instance = null;

    private static final int _1MB = 1024 * 1024;

    // 这个成员属性的唯一意义就是占点内存，以便能在GC日志中看清楚是否回收过
    private byte[] bigSize = new byte[2 * _1MB];

    public static void main(String[] args) {
        ReferenceCountingGC objA = new ReferenceCountingGC();
        ReferenceCountingGC objB = new ReferenceCountingGC();
        objA.instance = objB;
        objB.instance = objA;

        objA = null;
        objB = null;

        // 加上在这行发生GC， objA和objB会被回收吗？
        System.gc();

    }

}
