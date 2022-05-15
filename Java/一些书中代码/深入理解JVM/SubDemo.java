public class SubDemo {
    /**
     * 请实现一个函数，将一个字符串中的每个空格替换成“%20”。
     * 例如，当字符串为We Are Happy.则经过替换之后的字符串为We%20Are%20Happy。
     */
    public static final String SUB_STRING = "%20";

    public static String subBlankSpace(String str) {
        if (str == null || str.length() == 0) return str;

        StringBuilder sb = new StringBuilder();
        for (Character c : str.toCharArray()) {
            if (c == ' ') {
                sb.append(SUB_STRING);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static void main(String[] args) {
        String test_string = "We Are Happy";
        String test_string2 = "";
        System.out.println(subBlankSpace(test_string));
        System.out.println(subBlankSpace(test_string2));

    }
}
