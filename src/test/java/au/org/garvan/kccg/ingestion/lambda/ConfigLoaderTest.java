package au.org.garvan.kccg.ingestion.lambda;

import com.google.common.base.Strings;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by ahmed on 20/7/17.
 */
public class ConfigLoaderTest {
    @Test
    public void getS3Bucket() throws Exception {
        String var = ConfigLoader.getS3Bucket();
        Assert.assertFalse(Strings.isNullOrEmpty(var));
    }

    @Test
    public void getREGION() throws Exception {
        String var = ConfigLoader.getREGION();
        Assert.assertFalse(Strings.isNullOrEmpty(var));
    }

    @Test
    public void getDAYS() throws Exception {
        String var = ConfigLoader.getDAYS();
        Assert.assertFalse(Strings.isNullOrEmpty(var));
    }

    @Test
    public void shouldPersistInSolr() throws Exception {
        Boolean var = ConfigLoader.shouldPersistInSolr();
        Assert.assertFalse(var == null);
    }

    @Test
    public void shouldPersistInS3() throws Exception {
        Boolean var = ConfigLoader.shouldPersistInS3();
        Assert.assertFalse(var ==null);
    }

    @Test
    public void getENV() throws Exception {
        String var = ConfigLoader.getENV();
        Assert.assertFalse(Strings.isNullOrEmpty(var));
    }



}