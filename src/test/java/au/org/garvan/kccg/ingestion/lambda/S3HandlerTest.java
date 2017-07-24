package au.org.garvan.kccg.ingestion.lambda;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ahmed on 18/7/17.
 */
public class S3HandlerTest {


    List<Article> testArticles;

    @Before
    public void init() {
        testArticles = new ArrayList<>();
        testArticles.add(new Article(1, LocalDate.now(), "Test Title 1", "Test Doc 1", "en", Boolean.TRUE));
        testArticles.add(new Article(2, LocalDate.now().withDayOfMonth(19), "Test Title 2", "Test Doc 2", "en", Boolean.TRUE));


    }


    @Test
    public void archiveArticles() throws Exception {
        S3Handler.archiveArticles(testArticles);
    }


}