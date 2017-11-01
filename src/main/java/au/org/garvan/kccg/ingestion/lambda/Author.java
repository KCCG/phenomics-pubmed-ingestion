package au.org.garvan.kccg.ingestion.lambda;

import jdk.nashorn.internal.objects.annotations.Property;
import lombok.Getter;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Created by ahmed on 3/7/17.
 */
public class Author {

    @Getter
    @Property
    private String foreName;
    @Getter
    @Property
    private String lastName;
    @Getter
    @Property
    private String initials;
    @Getter
    @Property
    private List<String> affiliationInfo;

    public Author(JSONObject jsonAuthor){

            initials = jsonAuthor.has("Initials")?(String) jsonAuthor.get("Initials"):"";
            foreName = jsonAuthor.has("ForeName")?(String) jsonAuthor.get("ForeName"):"";
            lastName = jsonAuthor.has("LastName")?(String) jsonAuthor.get("LastName"):"";


        affiliationInfo = new ArrayList<>();

        if (jsonAuthor.has("AffiliationInfo")) {
            Object obj = jsonAuthor.get("AffiliationInfo");

            if (obj instanceof JSONArray) {
                affiliationInfo = StreamSupport.stream(jsonAuthor.getJSONArray("AffiliationInfo").spliterator(), false)
                        .map(JSONObject.class::cast)
                        .map(x -> (String) x.get("Affiliation"))
                        .collect(Collectors.toList());
            } else if (obj instanceof JSONObject) {
                affiliationInfo.add((String) ((JSONObject) obj).get("Affiliation"));
            }
        }
    }

  public JSONObject constructJson()
  {
      JSONObject author = new JSONObject();
      author.put("initials", initials);
      author.put("foreName", foreName);
      author.put("lastName", lastName);
      return author;
  }

}
