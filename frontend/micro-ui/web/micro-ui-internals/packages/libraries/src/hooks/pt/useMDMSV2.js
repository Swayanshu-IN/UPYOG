import { MdmsServiceV2, getGeneralCriteria } from "../../services/elements/MDMSV2";
import { useQuery } from "react-query";

const useMDMSV2 = (tenantId, moduleCode, type, config = { }, payload = []) => {
  const useFinancialYears = () => {
    return useQuery("PT_FINANCIAL_YEARLS", () => MdmsServiceV2.getDataByCriteria(tenantId, payload, moduleCode));
  };
  const useCommonFieldsConfig = () => {
    return useQuery("COMMON_FIELDS", () => MdmsServiceV2.getCommonFieldsConfig(tenantId, moduleCode, type, payload));
  };

  const usePropertyTaxDocuments = () => {
    return useQuery("PT_PROPERTY_TAX_DOCUMENTS", () => MdmsServiceV2.getDataByCriteria(tenantId, payload, moduleCode));
  };

  /*const useGenderDetails = () => {
    return useQuery("PT_GENDER_DETAILS", () => MdmsServiceV2.getGenderTypeDetails(tenantId, type, filter), config);
  };*/

  switch (type) {
    case "FINANCIAL_YEARLS":
      return useFinancialYears();
    case "PROPERTY_TAX_DOCUMENTS":
      return usePropertyTaxDocuments();
    case "CommonFieldsConfig":
      return useCommonFieldsConfig();
    /*case "GenderType":
      return useGenderDetails();*/

    default:
      return useQuery(type, () => MdmsServiceV2.getDataByCriteria(tenantId, getGeneralCriteria(tenantId, moduleCode, type), moduleCode), config);
  }
};

export default useMDMSV2;
