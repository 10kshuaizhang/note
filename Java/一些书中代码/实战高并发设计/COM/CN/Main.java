package COM.CN;

public class Main {
    public static void main(String[] args) {
//        SmsService smsService = new SmsServiceImpl();
//        SmsProxy smsProxy = new SmsProxy(smsService);
//        smsProxy.send("Java");
        SmsService smsService = (SmsService) JdkProxyFactory.getProxy(new SmsServiceImpl());
        smsService.send("Java");
    }
}
