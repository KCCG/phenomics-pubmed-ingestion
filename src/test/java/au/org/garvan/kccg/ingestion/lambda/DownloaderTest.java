package au.org.garvan.kccg.ingestion.lambda;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;

import com.amazonaws.services.lambda.runtime.LambdaLogger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;


/**
 * Created by ahmed on 18/7/17.
 */
public class DownloaderTest {

    Downloader testD;


    @Before
    public void init() throws Exception {
        testD = new Downloader();


    }

    @Test
    public void handleRequest() {
        String result = testD.handleRequest(new HashMap<>(), buildContext());
        Assert.assertEquals(result, "OK");
    }

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