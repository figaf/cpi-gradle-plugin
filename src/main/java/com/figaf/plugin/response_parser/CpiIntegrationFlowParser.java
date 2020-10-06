package com.figaf.plugin.response_parser;

import com.figaf.plugin.entities.CpiIntegrationObjectData;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Arsenii Istlentev
 */
public class CpiIntegrationFlowParser {

    public static List<CpiIntegrationObjectData> buildCpiIntegrationObjectDataList(String responseBody) {
        JSONObject response = new JSONObject(responseBody);
        JSONArray iFlowsJsonArray = response.getJSONObject("d").getJSONArray("results");

        List<CpiIntegrationObjectData> integrationFlows = new ArrayList<>();

        for (int ind = 0; ind < iFlowsJsonArray.length(); ind++) {
            JSONObject iFlowElement = iFlowsJsonArray.getJSONObject(ind);

            if (!StringUtils.equals(CpiCommonParser.optString(iFlowElement, "Type"), "IFlow")) {
                continue;
            }

            CpiIntegrationObjectData integrationFlow = new CpiIntegrationObjectData();
            integrationFlow.setExternalId(iFlowElement.getString("reg_id"));
            integrationFlow.setTechnicalName(iFlowElement.getString("Name"));
            integrationFlow.setDisplayedName(iFlowElement.getString("DisplayName"));
            integrationFlow.setVersion(CpiCommonParser.optString(iFlowElement, "Version"));
            integrationFlow.setCreationDate(
                new Timestamp(Long.parseLong(iFlowElement.getString("CreatedAt").replaceAll("[^0-9]", "")))
            );
            integrationFlow.setCreatedBy(CpiCommonParser.optString(iFlowElement, "CreatedBy"));
            String modifiedAt = CpiCommonParser.optString(iFlowElement, "ModifiedAt");
            integrationFlow.setModificationDate(modifiedAt != null
                ? new Timestamp(Long.parseLong(modifiedAt.replaceAll("[^0-9]", "")))
                : null
            );
            integrationFlow.setModifiedBy(iFlowElement.getString("ModifiedBy"));
            integrationFlow.setDescription(CpiCommonParser.optString(iFlowElement, "Description"));

            integrationFlows.add(integrationFlow);
        }
        return integrationFlows;
    }

    public static String retrieveDeployStatus(String body) {
        JSONObject response = new JSONObject(body);
        return (String) response.get("status");
    }

}
