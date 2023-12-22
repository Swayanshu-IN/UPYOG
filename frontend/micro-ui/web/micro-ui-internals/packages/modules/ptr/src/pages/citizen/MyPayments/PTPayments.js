import { Card, KeyNote, SubmitBar } from "@egovernments/digit-ui-react-components";
import React from "react";
import { useTranslation } from "react-i18next";
import { Link } from "react-router-dom";
import { convertEpochToDate, DownloadReceipt } from "../../../utils/index";

const PTPayments = ({ application }) => {
  const { t } = useTranslation();
  return (
    <Card>
      <KeyNote
        noteStyle={{ fontSize: "24px", fontWeight: "700" }}
        keyValue={t("CS_PAYMENT_AMOUNT_PAID_WITHOUT_SYMBOL")}
        note={`₹ ${application?.paymentDetails?.[0]?.totalAmountPaid}`}
      />
      <KeyNote keyValue={t("PTR_SEARCHPROPERTY_TABEL_PID")} note={t(application?.paymentDetails?.[0]?.bill?.consumerCode)} />
      <KeyNote keyValue={t("PTR_OWNERS_NAME")} note={application?.owners?.map((owner) => owner?.name).join(",")} />
      <KeyNote keyValue={t("PTR_RECEIPT_DATE_LABEL")} note={convertEpochToDate(application?.paymentDetails?.[0]?.receiptDate) || t("CS_NA")} />
      <KeyNote keyValue={t("PTR_RECEIPT_NO_LABEL")} note={t(application?.paymentDetails?.[0]?.receiptNumber)} />
      <SubmitBar
        label={t("PTR_DOWNLOAD_RECEIPT")}
        onSubmit={(e) => DownloadReceipt(application?.paymentDetails?.[0]?.bill?.consumerCode, application?.tenantId, "PT")}
      />
    </Card>
  );
};

export default PTPayments;
