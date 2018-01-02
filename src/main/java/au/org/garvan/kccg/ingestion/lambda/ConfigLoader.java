package au.org.garvan.kccg.ingestion.lambda;

import com.google.common.base.Strings;

/**
 * Created by ahmed on 20/7/17.
 */
public class ConfigLoader {


    private static String ENV;
    private static String S3_BUCKET;
    private static String REGION;
    private static String DAYS;
    private static Boolean PERSIST_IN_SOLR = null;
    private static Boolean PERSIST_IN_S3 = null;
    private static Boolean SEND_TO_PIPELINE = null;
    private static String PIPELINE_ENDPOINT;



    public static String getENV() {
        if (Strings.isNullOrEmpty(ENV)) {
            ENV = System.getenv("ENV");
        }

        return Strings.isNullOrEmpty(ENV) ? "DEV" : ENV;

    }

    public static String getS3Bucket() {
        if (Strings.isNullOrEmpty(S3_BUCKET)) {
            S3_BUCKET = System.getenv("S3_BUCKET");
        }
        return Strings.isNullOrEmpty(S3_BUCKET) ? "phenomics-pubmed-articles" : S3_BUCKET;
    }

    public static String getREGION() {
        if (Strings.isNullOrEmpty(REGION)) {
            REGION = System.getenv("REGION");
        }

        return Strings.isNullOrEmpty(REGION) ? "ap-southeast-2" : REGION;
    }

    public static String getDAYS() {
        if (Strings.isNullOrEmpty(DAYS)) {
            DAYS = System.getenv("DAYS");
        }

        return Strings.isNullOrEmpty(DAYS) ? "2" : DAYS;
    }

    public static String getPipelineEndpoint() {
        if (Strings.isNullOrEmpty(PIPELINE_ENDPOINT)) {
            PIPELINE_ENDPOINT = System.getenv("PIPELINE_ENDPOINT");
        }
        return PIPELINE_ENDPOINT;
    }

    public static boolean shouldPersistInSolr() {

        if (PERSIST_IN_SOLR == null) {
            PERSIST_IN_SOLR = System.getenv("PERSIST_IN_SOLR").equals("true")  ? Boolean.TRUE : Boolean.FALSE;
        }
        return PERSIST_IN_SOLR;
    }

    public static boolean shouldPersistInS3() {

        if (PERSIST_IN_S3 == null) {
            PERSIST_IN_S3 = System.getenv("PERSIST_IN_S3").equals("true") ? Boolean.TRUE : Boolean.FALSE;
        }
        return PERSIST_IN_S3;
    }
    public static boolean shouldSendToPipeline () {

        if (SEND_TO_PIPELINE == null) {
            SEND_TO_PIPELINE = System.getenv("SEND_TO_PIPELINE").equals("true")  ? Boolean.TRUE : Boolean.FALSE;
        }
        return SEND_TO_PIPELINE;
    }


}
