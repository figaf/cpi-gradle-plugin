package com.figaf.plugin.response_parser;

import com.figaf.plugin.entities.IntegrationPackage;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Arsenii Istlentev
 */
public class IntegrationPackageParser {

    public static List<IntegrationPackage> buildIntegrationPackages(String body) {
        JSONObject response = new JSONObject(body);
        JSONArray packagesJsonArray = response.getJSONObject("d").getJSONArray("results");

        List<IntegrationPackage> packages = new ArrayList<>();
        for (int ind = 0; ind < packagesJsonArray.length(); ind++) {
            JSONObject packageElement = packagesJsonArray.getJSONObject(ind);

            IntegrationPackage integrationPackage = new IntegrationPackage();
            integrationPackage.setExternalId(packageElement.getString("reg_id"));
            integrationPackage.setTechnicalName(packageElement.getString("TechnicalName"));
            integrationPackage.setDisplayedName(packageElement.getString("DisplayName"));
            integrationPackage.setVersion(CpiCommonParser.optString(packageElement, "Version"));
            integrationPackage.setCreationDate(
                new Timestamp(Long.parseLong(packageElement.getString("CreatedAt").replaceAll("[^0-9]", "")))
            );
            integrationPackage.setCreatedBy(CpiCommonParser.optString(packageElement, "CreatedBy"));
            String modifiedAt = CpiCommonParser.optString(packageElement, "ModifiedAt");
            integrationPackage.setModificationDate(modifiedAt != null
                ? new Timestamp(Long.parseLong(modifiedAt.replaceAll("[^0-9]", "")))
                : null
            );
            integrationPackage.setModifiedBy(packageElement.getString("ModifiedBy"));
//            integrationPackage.setVendor(optString(packageElement, "Vendor"));
//            integrationPackage.setShortDescription(optString(packageElement, "ShortText"));

            packages.add(integrationPackage);
        }

        return packages;
    }

}
