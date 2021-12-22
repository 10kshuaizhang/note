import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class IOTest {
    public static void main(String[] args) {
//        String name = IOTest.class.getName();
//        String canonical_name = IOTest.class.getCanonicalName();
//        String simple_name = IOTest.class.getSimpleName();
//        String funcName = Thread.currentThread().getStackTrace()[1].getMethodName();
//        System.out.println("name: " + name);
//        System.out.println("canonical_name: " + canonical_name);
//        System.out.println("simple_name: " + simple_name);
//        System.out.println(funcName);
//        String s = "com.bytedance.phoebusdemo.cases.UiOperateTest";

        int[] nums = new int[]{1, 2, 3};
        int[] empty_array = new int[3];
        argumentTest();
        argumentTest(nums);
        argumentTest(empty_array);

//        File parentDir = new File("/Users/bytedance/IdeaProjects/testProj/src/fileDir");
////        File file = new File(parentDir, "");
//        parentDir.mkdirs();
//
//        File saveFile = new File(parentDir, "file.txt");
//        try {
//            saveFile.createNewFile();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        file.createNewFile();
    }
    public static void argumentTest(int... num) {
        if (num.length != 0) {
            System.out.println(num[0]);
        } else {
            System.out.println("Nothing to print.");
        }
//        System.out.println("ABC");
    }
}
