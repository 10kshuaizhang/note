package COM.CN;

public class SmsProxy implements SmsService{

    private final SmsService smsService;

    public SmsProxy(SmsService smsService) {
        this.smsService = smsService;
    }
    @Override
    public String send(String message) {
        System.out.println("before send");
        smsService.send(message);
        System.out.println("after send");
        return null;
    }

}
