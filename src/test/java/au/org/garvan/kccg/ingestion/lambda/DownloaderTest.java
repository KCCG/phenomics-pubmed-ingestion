package au.org.garvan.kccg.ingestion.lambda;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.Before;


/**
 * Created by ahmed on 18/7/17.
 */
public class DownloaderTest {

    Downloader testD;


    @Before
    public void init() throws Exception {
        testD = new Downloader();


    }

//
//    @Test
//    public void handleRequest() {
//        HashMap<String, Object> input = new HashMap<>();
//        input.put(Constants.WORKER_ID, "0");
//        input.put(Constants.IGNITION_CALL, false);
//        String result = testD.handleRequest(input, buildContext());
//        Assert.assertEquals(result, "OK");
//    }

//    @Test
//    public void callWithID() throws Exception {
//        Downloader.callWithID(Arrays.asList("29216712"));
//    }


    private Context buildContext() {
        return
                new Context() {
                    public String getAwsRequestId() {
                        return null;
                    }

                    public String getLogGroupName() {
                        return null;
                    }

                    public String getLogStreamName() {
                        return null;
                    }

                    public String getFunctionName() {
                        return null;
                    }

                    public String getFunctionVersion() {
                        return null;
                    }

                    public String getInvokedFunctionArn() {
                        return null;
                    }

                    @Override
                    public CognitoIdentity getIdentity() {
                        return null;
                    }

                    @Override
                    public ClientContext getClientContext() {
                        return null;
                    }


                    public int getRemainingTimeInMillis() {
                        return 0;
                    }

                    public int getMemoryLimitInMB() {
                        return 0;
                    }

                    @Override
                    public LambdaLogger getLogger() {
                        return null;
                    }


                };

    }


}