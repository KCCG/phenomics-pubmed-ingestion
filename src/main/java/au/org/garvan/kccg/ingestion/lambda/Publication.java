package au.org.garvan.kccg.ingestion.lambda;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

/**
 * Created by ahmed on 1/11/17.
 */
@AllArgsConstructor
public class Publication {

    @Getter
    @Setter
    private String title;
    @Getter
    @Setter
    private String ISOAbbreviation;
    @Getter
    @Setter
    private String ISSNType;
    @Getter
    @Setter
    private String ISSNNumber;


    public Publication(JSONObject jsonPublication) {
        title = jsonPublication.get("Title").toString();

        if (jsonPublication.has("ISOAbbreviation")){
            ISOAbbreviation = jsonPublication.get("ISOAbbreviation").toString();

        }
        else
            ISOAbbreviation = "N/A";

        if (jsonPublication.has("ISSN"))
        {
            ISSNType= ((JSONObject)jsonPublication.get("ISSN")).get("IssnType").toString();
            ISSNNumber = ((JSONObject)jsonPublication.get("ISSN")).get("content").toString();
        }
    }

    public JSONObject constructJson()
    {
        JSONObject publication = new JSONObject();
        publication.put("title", title);
        publication.put("isoAbbreviation", ISOAbbreviation);
        publication.put("issnType", ISSNType);
        publication.put("issnNumber", ISSNNumber);
        return publication;
    }


}
