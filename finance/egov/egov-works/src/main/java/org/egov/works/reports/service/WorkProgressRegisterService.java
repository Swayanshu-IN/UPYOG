/*
 * eGov suite of products aim to improve the internal efficiency,transparency,
 *    accountability and the service delivery of the government  organizations.
 *
 *     Copyright (C) <2015>  eGovernments Foundation
 *
 *     The updated version of eGov suite of products as by eGovernments Foundation
 *     is available at http://www.egovernments.org
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see http://www.gnu.org/licenses/ or
 *     http://www.gnu.org/licenses/gpl.html .
 *
 *     In addition to the terms of the GPL license to be adhered to in using this
 *     program, the following additional terms are to be complied with:
 *
 *         1) All versions of this program, verbatim or modified must carry this
 *            Legal Notice.
 *
 *         2) Any misrepresentation of the origin of the material is prohibited. It
 *            is required that all modified versions of this material be marked in
 *            reasonable ways as different from the original version.
 *
 *         3) This license does not grant any rights to any user of the program
 *            with regards to rights under trademark law for use of the trade names
 *            or trademarks of eGovernments Foundation.
 *
 *   In case of any queries, you can reach eGovernments Foundation at contact@egovernments.org.
 */
package org.egov.works.reports.service;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.egov.utils.Constants;
import org.egov.works.lineestimate.entity.enums.LineEstimateStatus;
import org.egov.works.lineestimate.repository.LineEstimateDetailsRepository;
import org.egov.works.reports.entity.EstimateAbstractReport;
import org.egov.works.reports.entity.WorkProgressRegister;
import org.egov.works.reports.entity.WorkProgressRegisterSearchRequest;
import org.egov.works.utils.WorksConstants;
import org.hibernate.Criteria;
import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.criterion.CriteriaSpecification;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Restrictions;
import org.hibernate.transform.Transformers;
import org.hibernate.type.LongType;
import org.hibernate.type.StringType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WorkProgressRegisterService {

    private static final SimpleDateFormat FORMATDDMMYYYY = new SimpleDateFormat("dd/MM/yyyy", Constants.LOCALE);

    @Autowired
    private LineEstimateDetailsRepository lineEstimateDetailsRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public List<String> findWorkIdentificationNumbersToSearchLineEstimatesForLoa(final String code) {
        final List<String> workIdNumbers = lineEstimateDetailsRepository
                .findWorkIdentificationNumbersToSearchWorkProgressRegister("%" + code + "%",
                        LineEstimateStatus.ADMINISTRATIVE_SANCTIONED.toString(),
                        LineEstimateStatus.TECHNICAL_SANCTIONED.toString());
        return workIdNumbers;
    }

    @Transactional
    public List<WorkProgressRegister> searchWorkProgressRegister(
            final WorkProgressRegisterSearchRequest workProgressRegisterSearchRequest) {
        if (workProgressRegisterSearchRequest != null) {
            final Criteria criteria = entityManager.unwrap(Session.class).createCriteria(WorkProgressRegister.class);
            if (workProgressRegisterSearchRequest.getDepartment() != null)
                criteria.add(Restrictions.eq("department.id",
                        workProgressRegisterSearchRequest.getDepartment()));
            if (workProgressRegisterSearchRequest.getWorkIdentificationNumber() != null)
                criteria.add(Restrictions.eq("winCode",
                        workProgressRegisterSearchRequest.getWorkIdentificationNumber()).ignoreCase());
            if (workProgressRegisterSearchRequest.getContractor() != null) {
                criteria.createAlias("contractor", "contractor ");
                criteria.add(Restrictions.or(Restrictions.ilike("contractor.code",
                        workProgressRegisterSearchRequest.getContractor(), MatchMode.ANYWHERE),
                        Restrictions.ilike("contractor.name",
                                workProgressRegisterSearchRequest.getContractor(), MatchMode.ANYWHERE)));
            }
            if (workProgressRegisterSearchRequest.getAdminSanctionFromDate() != null)
                criteria.add(Restrictions.ge("adminSanctionDate",
                        workProgressRegisterSearchRequest.getAdminSanctionFromDate()));
            if (workProgressRegisterSearchRequest.getAdminSanctionToDate() != null)
                criteria.add(Restrictions.le("adminSanctionDate",
                        workProgressRegisterSearchRequest.getAdminSanctionToDate()));

            if (workProgressRegisterSearchRequest.isSpillOverFlag())
                criteria.add(Restrictions.eq("spillOverFlag", workProgressRegisterSearchRequest.isSpillOverFlag()));

            criteria.setResultTransformer(CriteriaSpecification.DISTINCT_ROOT_ENTITY);
            return criteria.list();
        } else
            return new ArrayList<WorkProgressRegister>();
    }

    public Date getReportSchedulerRunDate() {
        Query query = null;
        query = entityManager.unwrap(Session.class).createQuery(
                "from WorkProgressRegister ");
        List<WorkProgressRegister> obj = query.setMaxResults(1).list();
        Date runDate = null;
        if (obj != null) {
            runDate = obj.get(0).getCreatedDate();
        }
        return runDate;
    }

    @Transactional
    public List<EstimateAbstractReport> searchEstimateAbstractReportByDepartmentWise(
            final EstimateAbstractReport estimateAbstractReport) {

        Query query = null;
        query = entityManager.unwrap(Session.class).createSQLQuery(getQueryForDepartmentWiseReport(estimateAbstractReport))
                .addScalar("departmentName", StringType.INSTANCE)
                .addScalar("lineEstimates", LongType.INSTANCE)
                .addScalar("adminSanctionedEstimates", LongType.INSTANCE)
                .addScalar("adminSanctionedAmountInCrores", StringType.INSTANCE)
                .addScalar("technicalSanctionedEstimates", LongType.INSTANCE)
                .addScalar("loaCreated", LongType.INSTANCE)
                .addScalar("agreementValueInCrores", StringType.INSTANCE)
                .addScalar("workInProgress", LongType.INSTANCE)
                .addScalar("WorkCompleted", LongType.INSTANCE)
                .addScalar("billsCreated", LongType.INSTANCE)
                .addScalar("BillValueInCrores", StringType.INSTANCE)
                .setResultTransformer(Transformers.aliasToBean(EstimateAbstractReport.class));
        return query.list();

    }

    @Transactional
    public List<EstimateAbstractReport> searchEstimateAbstractReportByTypeOfWorkWise(
            final EstimateAbstractReport estimateAbstractReport) {

        Query query = null;
        if (estimateAbstractReport.getDepartment() != null)
            query = entityManager.unwrap(Session.class).createSQLQuery(getQueryForTypeOfWorkWiseReport(estimateAbstractReport))
                    .addScalar("typeOfWorkName", StringType.INSTANCE)
                    .addScalar("departmentName", StringType.INSTANCE)
                    .addScalar("lineEstimates", LongType.INSTANCE)
                    .addScalar("adminSanctionedEstimates", LongType.INSTANCE)
                    .addScalar("adminSanctionedAmountInCrores", StringType.INSTANCE)
                    .addScalar("technicalSanctionedEstimates", LongType.INSTANCE)
                    .addScalar("loaCreated", LongType.INSTANCE)
                    .addScalar("agreementValueInCrores", StringType.INSTANCE)
                    .addScalar("workInProgress", LongType.INSTANCE)
                    .addScalar("WorkCompleted", LongType.INSTANCE)
                    .addScalar("billsCreated", LongType.INSTANCE)
                    .addScalar("BillValueInCrores", StringType.INSTANCE)
                    .setResultTransformer(Transformers.aliasToBean(EstimateAbstractReport.class));
        else
            query = entityManager.unwrap(Session.class).createSQLQuery(getQueryForTypeOfWorkWiseReport(estimateAbstractReport))
                    .addScalar("typeOfWorkName", StringType.INSTANCE)
                    .addScalar("lineEstimates", LongType.INSTANCE)
                    .addScalar("adminSanctionedEstimates", LongType.INSTANCE)
                    .addScalar("adminSanctionedAmountInCrores", StringType.INSTANCE)
                    .addScalar("technicalSanctionedEstimates", LongType.INSTANCE)
                    .addScalar("loaCreated", LongType.INSTANCE)
                    .addScalar("agreementValueInCrores", StringType.INSTANCE)
                    .addScalar("workInProgress", LongType.INSTANCE)
                    .addScalar("WorkCompleted", LongType.INSTANCE)
                    .addScalar("billsCreated", LongType.INSTANCE)
                    .addScalar("BillValueInCrores", StringType.INSTANCE)
                    .setResultTransformer(Transformers.aliasToBean(EstimateAbstractReport.class));
        return query.list();

    }

    private String getQueryForDepartmentWiseReport(EstimateAbstractReport estimateAbstractReport) {
        StringBuilder workInProgessCondition = new StringBuilder();
        StringBuilder filterConditions = new StringBuilder();

        if (estimateAbstractReport != null) {
            if (estimateAbstractReport.isSpillOverFlag()) {

                workInProgessCondition.append(" details.workordercreated  = true ");
                workInProgessCondition.append(" AND details.workcompleted  = false ");

                filterConditions.append(" AND details.spilloverflag = true ");

            } else {

                workInProgessCondition.append(" details.spilloverflag = false ");
                workInProgessCondition.append(" AND details.wostatuscode = 'APPROVED' ");
                workInProgessCondition.append(" AND details.workcompleted  = false ");

            }

            if (estimateAbstractReport.getDepartment() != null) {
                filterConditions.append(" AND details.department = " + estimateAbstractReport.getDepartment());
            }

            if (estimateAbstractReport.getAdminSanctionFromDate() != null) {
                filterConditions.append(" AND details.adminsanctiondate >= to_date('"
                        + FORMATDDMMYYYY.format(estimateAbstractReport.getAdminSanctionFromDate()) + "','dd/mm/yyyy')");
            }

            if (estimateAbstractReport.getAdminSanctionToDate() != null) {
                filterConditions.append(" AND details.adminsanctiondate <= to_date('"
                        + FORMATDDMMYYYY.format(estimateAbstractReport.getAdminSanctionToDate()) + "','dd/mm/yyyy')");
            }

            if (estimateAbstractReport.getScheme() != null) {
                filterConditions.append(" AND details.scheme = " + estimateAbstractReport.getScheme());
            }

            if (estimateAbstractReport.getSubScheme() != null) {
                filterConditions.append(" AND details.subScheme = " + estimateAbstractReport.getSubScheme());
            }

            if (estimateAbstractReport.getWorkCategory() != null
                    && !estimateAbstractReport.getWorkCategory().equalsIgnoreCase("undefined")) {
                if (estimateAbstractReport.getWorkCategory().equalsIgnoreCase(WorksConstants.SLUM_WORK)) {

                    filterConditions.append(" AND details.workcategory = '"
                            + estimateAbstractReport.getWorkCategory().replace("_", " ") + "'");
                    if (estimateAbstractReport.getTypeOfSlum() != null) {
                        filterConditions.append(" AND details.typeofslum = '" + estimateAbstractReport.getTypeOfSlum() + "'");
                    }

                    if (estimateAbstractReport.getBeneficiary() != null) {
                        filterConditions.append(" AND details.beneficiary = '" + estimateAbstractReport.getBeneficiary() + "'");
                    }

                } else {

                    filterConditions.append(" AND details.workcategory = '"
                            + estimateAbstractReport.getWorkCategory().replace("_", " ") + "'");

                }
            }

            if (estimateAbstractReport.getNatureOfWork() != null) {
                filterConditions.append(" AND details.natureofwork = " + estimateAbstractReport.getNatureOfWork());
            }

        }
        StringBuilder query = new StringBuilder();
        query.append("SELECT departmentName AS departmentName, ");
        query.append(" SUM(lineEstimates)                 AS lineEstimates ,  ");
        query.append(" SUM(lineEstimateDetails)           AS lineEstimateDetails ,  ");
        query.append(" SUM(adminSanctionedAmountInCrores) AS adminSanctionedAmountInCrores,  ");
        query.append(" SUM(adminSanctionedEstimates)      AS adminSanctionedEstimates,  ");
        query.append(" SUM(technicalSanctionedEstimates)  AS technicalSanctionedEstimates,  ");
        query.append(" SUM(loaCreated)                    AS loaCreated,  ");
        query.append(" SUM(agreementValueInCrores)        AS agreementValueInCrores,  ");
        query.append(" SUM(workInProgress)                AS workInProgress,  ");
        query.append(" SUM(WorkCompleted)                 AS WorkCompleted ,  ");
        query.append(" SUM(billsCreated)                  AS billsCreated,  ");
        query.append(" SUM(BillValueInCrores)             AS BillValueInCrores  ");
        query.append(" FROM  ");
        query.append(" (  ");
        query.append(" SELECT details.departmentName       AS departmentName,  ");
        query.append(" COUNT(DISTINCT details.leid)         AS lineEstimates,  ");
        query.append(" COUNT(details.ledid)                 AS lineEstimateDetails,  ");
        query.append(" SUM(details.estimateamount)/10000000 AS adminSanctionedAmountInCrores,  ");
        query.append(" COUNT(details.lestatus)              AS adminSanctionedEstimates,  ");
        query.append(" 0                                    AS technicalSanctionedEstimates,  ");
        query.append(" 0                                    AS loaCreated,  ");
        query.append(" 0                                    AS agreementValueInCrores, ");
        query.append(" 0                                    AS workInProgress, ");
        query.append(" 0                                    AS WorkCompleted , ");
        query.append(" 0                                    AS billsCreated, ");
        query.append(" 0                                    AS BillValueInCrores ");
        query.append(" FROM egw_mv_work_progress_register details, ");
        query.append("   egw_status status ");
        query.append(" WHERE details.lestatus = status.code ");
        query.append(" AND status.code       IN ('TECHNICAL_SANCTIONED','ADMINISTRATIVE_SANCTIONED') ");
        query.append(filterConditions.toString());
        query.append(" GROUP BY details.departmentName ");
        query.append(" UNION ");
        query.append(" SELECT details.departmentName AS departmentName, ");
        query.append(" 0                           AS lineEstimates, ");
        query.append(" 0                           AS lineEstimateDetails, ");
        query.append(" 0                           AS adminSanctionedAmountInCrores, ");
        query.append(" 0                           AS adminSanctionedEstimates, ");
        query.append(" COUNT(details.lestatus)     AS technicalSanctionedEstimates, ");
        query.append(" 0                           AS loaCreated, ");
        query.append(" 0                           AS agreementValueInCrores, ");
        query.append(" 0                           AS workInProgress, ");
        query.append(" 0                           AS WorkCompleted , ");
        query.append(" 0                           AS billsCreated, ");
        query.append(" 0                           AS BillValueInCrores ");
        query.append(" FROM egw_mv_work_progress_register details, ");
        query.append(" egw_status status ");
        query.append(" WHERE details.lestatus = status.code ");
        query.append(" AND status.code       IN ('TECHNICAL_SANCTIONED') ");
        query.append(filterConditions.toString());
        query.append(" GROUP BY details.departmentName ");
        query.append(" UNION ");
        query.append(" SELECT details.departmentName         AS departmentName, ");
        query.append(" 0                                     AS lineEstimates, ");
        query.append(" 0                                     AS lineEstimateDetails, ");
        query.append(" 0                                     AS adminSanctionedAmountInCrores, ");
        query.append(" 0                                     AS adminSanctionedEstimates, ");
        query.append(" 0                                     AS technicalSanctionedEstimates, ");
        query.append(" COUNT(details.ledid)                  AS loaCreated, ");
        query.append(" SUM(details.agreementamount)/10000000 AS agreementValueInCrores, ");
        query.append(" 0                                     AS workInProgress, ");
        query.append(" 0                                     AS WorkCompleted, ");
        query.append(" 0                                     AS billsCreated, ");
        query.append(" 0                                     AS BillValueInCrores ");
        query.append(" FROM egw_mv_work_progress_register details ");
        query.append(" WHERE details.agreementnumber IS NOT NULL ");
        query.append(" AND details.wostatuscode       = 'APPROVED' ");
        query.append(filterConditions.toString());
        query.append(" GROUP BY details.departmentName ");
        query.append(" UNION ");
        query.append(" SELECT details.departmentName AS departmentName, ");
        query.append(" 0                             AS lineEstimates, ");
        query.append(" 0                             AS lineEstimateDetails, ");
        query.append(" 0                             AS adminSanctionedAmountInCrores, ");
        query.append(" 0                             AS adminSanctionedEstimates, ");
        query.append(" 0                             AS technicalSanctionedEstimates, ");
        query.append(" 0                             AS loaCreated, ");
        query.append(" 0                             AS agreementValueInCrores, ");
        query.append(" COUNT(DISTINCT details.ledid) AS workInProgress, ");
        query.append(" 0                             AS WorkCompleted, ");
        query.append(" 0                             AS billsCreated, ");
        query.append(" 0                             AS BillValueInCrores ");
        query.append(" FROM egw_mv_work_progress_register details ");
        query.append(" WHERE ");
        query.append(workInProgessCondition.toString());
        query.append(filterConditions.toString());
        query.append(" GROUP BY details.departmentName ");
        query.append(" UNION ");
        query.append(" SELECT details.departmentName AS departmentName, ");
        query.append(" 0                             AS lineEstimates, ");
        query.append(" 0                             AS lineEstimateDetails, ");
        query.append(" 0                             AS adminSanctionedAmountInCrores, ");
        query.append(" 0                             AS adminSanctionedEstimates, ");
        query.append(" 0                             AS technicalSanctionedEstimates, ");
        query.append(" 0                             AS loaCreated, ");
        query.append(" 0                             AS agreementValueInCrores, ");
        query.append(" 0                             AS workInProgress, ");
        query.append(" COUNT(DISTINCT details.ledid) AS WorkCompleted, ");
        query.append(" 0                             AS billsCreated, ");
        query.append(" 0                             AS BillValueInCrores ");
        query.append(" FROM egw_mv_work_progress_register details ");
        query.append(" WHERE details.workcompleted = true ");
        query.append(filterConditions.toString());
        query.append(" GROUP BY details.departmentName ");
        query.append(" UNION ");
        query.append(" SELECT details.departmentName       AS departmentName, ");
        query.append(" 0                                   AS lineEstimates, ");
        query.append(" 0                                   AS lineEstimateDetails, ");
        query.append(" 0                                   AS adminSanctionedAmountInCrores, ");
        query.append(" 0                                   AS adminSanctionedEstimates, ");
        query.append(" 0                                   AS technicalSanctionedEstimates, ");
        query.append(" 0                                   AS loaCreated, ");
        query.append(" 0                                   AS agreementValueInCrores, ");
        query.append(" 0                                   AS workInProgress, ");
        query.append(" 0                                   AS WorkCompleted , ");
        query.append(" COUNT(DISTINCT billdetail.billid)   AS billsCreated, ");
        query.append(" SUM(billdetail.billamount)/10000000 AS BillValueInCrores ");
        query.append(" FROM egw_mv_work_progress_register details , ");
        query.append(" egw_mv_estimate_abstract_by_department_billdetail billdetail ");
        query.append(" WHERE billdetail.ledid = details.ledid ");
        query.append(filterConditions.toString());
        query.append(" GROUP BY details.departmentName ");
        query.append(" ) final ");
        query.append(" GROUP BY departmentname  ");
        return query.toString();
    }

    private String getQueryForTypeOfWorkWiseReport(EstimateAbstractReport estimateAbstractReport) {
        StringBuilder workInProgessCondition = new StringBuilder();
        StringBuilder filterConditions = new StringBuilder();
        StringBuilder selectQuery = new StringBuilder();
        StringBuilder groupByQuery = new StringBuilder();
        StringBuilder mainSelectQuery = new StringBuilder();
        StringBuilder mainGroupByQuery = new StringBuilder();
        
        if (estimateAbstractReport != null) {
            if (estimateAbstractReport.isSpillOverFlag()) {

                workInProgessCondition.append(" details.workordercreated  = true ");
                workInProgessCondition.append(" AND details.workcompleted  = false ");

                filterConditions.append(" AND details.spilloverflag = true ");

            } else {

                workInProgessCondition.append(" details.spilloverflag = false ");
                workInProgessCondition.append(" AND details.wostatuscode = 'APPROVED' ");
                workInProgessCondition.append(" AND details.workcompleted  = false ");

            }
            
            if (estimateAbstractReport.getTypeOfWork() != null) {
                filterConditions.append(" AND details.typeofwork = " + estimateAbstractReport.getTypeOfWork());
            }

            if (estimateAbstractReport.getSubTypeOfWork() != null) {
                filterConditions.append(" AND details.subtypeofwork = " + estimateAbstractReport.getSubTypeOfWork());
            }
            
            if (estimateAbstractReport.getDepartment() != null) {
                
                filterConditions.append(" AND details.department = " + estimateAbstractReport.getDepartment());
                
                selectQuery.append(" SELECT details.typeOfWorkName       AS typeOfWorkName,  ");
                selectQuery.append(" details.departmentName         AS departmentName,  ");
                
                mainSelectQuery.append(" SELECT typeOfWorkName       AS typeOfWorkName,  ");
                mainSelectQuery.append(" departmentName         AS departmentName,  ");
                
                groupByQuery.append(" GROUP BY details.typeOfWorkName,details.departmentName ");
                mainGroupByQuery.append(" GROUP BY typeofworkname,departmentname ");
            }else{
                selectQuery.append(" SELECT details.typeOfWorkName       AS typeOfWorkName,  ");
                
                mainSelectQuery.append(" SELECT typeOfWorkName       AS typeOfWorkName,  ");
                
                groupByQuery.append(" GROUP BY details.typeOfWorkName ");
                
                mainGroupByQuery.append(" GROUP BY typeofworkname ");
            }

            if (estimateAbstractReport.getAdminSanctionFromDate() != null) {
                filterConditions.append(" AND details.adminsanctiondate >= to_date('"
                        + FORMATDDMMYYYY.format(estimateAbstractReport.getAdminSanctionFromDate()) + "','dd/mm/yyyy')");
            }

            if (estimateAbstractReport.getAdminSanctionToDate() != null) {
                filterConditions.append(" AND details.adminsanctiondate <= to_date('"
                        + FORMATDDMMYYYY.format(estimateAbstractReport.getAdminSanctionToDate()) + "','dd/mm/yyyy')");
            }

            if (estimateAbstractReport.getScheme() != null) {
                filterConditions.append(" AND details.scheme = " + estimateAbstractReport.getScheme());
            }

            if (estimateAbstractReport.getSubScheme() != null) {
                filterConditions.append(" AND details.subScheme = " + estimateAbstractReport.getSubScheme());
            }

            if (estimateAbstractReport.getWorkCategory() != null
                    && !estimateAbstractReport.getWorkCategory().equalsIgnoreCase("undefined")) {
                if (estimateAbstractReport.getWorkCategory().equalsIgnoreCase(WorksConstants.SLUM_WORK)) {

                    filterConditions.append(" AND details.workcategory = '"
                            + estimateAbstractReport.getWorkCategory().replace("_", " ") + "'");
                    if (estimateAbstractReport.getTypeOfSlum() != null) {
                        filterConditions.append(" AND details.typeofslum = '" + estimateAbstractReport.getTypeOfSlum() + "'");
                    }

                    if (estimateAbstractReport.getBeneficiary() != null) {
                        filterConditions.append(" AND details.beneficiary = '" + estimateAbstractReport.getBeneficiary() + "'");
                    }

                } else {

                    filterConditions.append(" AND details.workcategory = '"
                            + estimateAbstractReport.getWorkCategory().replace("_", " ") + "'");

                }
            }

            if (estimateAbstractReport.getNatureOfWork() != null) {
                filterConditions.append(" AND details.natureofwork = " + estimateAbstractReport.getNatureOfWork());
            }

        }
        StringBuilder query = new StringBuilder();
        query.append(mainSelectQuery.toString());
        query.append(" SUM(lineEstimates)                 AS lineEstimates ,  ");
        query.append(" SUM(lineEstimateDetails)           AS lineEstimateDetails ,  ");
        query.append(" SUM(adminSanctionedAmountInCrores) AS adminSanctionedAmountInCrores,  ");
        query.append(" SUM(adminSanctionedEstimates)      AS adminSanctionedEstimates,  ");
        query.append(" SUM(technicalSanctionedEstimates)  AS technicalSanctionedEstimates,  ");
        query.append(" SUM(loaCreated)                    AS loaCreated,  ");
        query.append(" SUM(agreementValueInCrores)        AS agreementValueInCrores,  ");
        query.append(" SUM(workInProgress)                AS workInProgress,  ");
        query.append(" SUM(WorkCompleted)                 AS WorkCompleted ,  ");
        query.append(" SUM(billsCreated)                  AS billsCreated,  ");
        query.append(" SUM(BillValueInCrores)             AS BillValueInCrores  ");
        query.append(" FROM  ");
        query.append(" (  ");
        query.append(selectQuery.toString());
        query.append(" COUNT(DISTINCT details.leid)         AS lineEstimates,  ");
        query.append(" COUNT(details.ledid)                 AS lineEstimateDetails,  ");
        query.append(" SUM(details.estimateamount)/10000000 AS adminSanctionedAmountInCrores,  ");
        query.append(" COUNT(details.lestatus)              AS adminSanctionedEstimates,  ");
        query.append(" 0                                    AS technicalSanctionedEstimates,  ");
        query.append(" 0                                    AS loaCreated,  ");
        query.append(" 0                                    AS agreementValueInCrores, ");
        query.append(" 0                                    AS workInProgress, ");
        query.append(" 0                                    AS WorkCompleted , ");
        query.append(" 0                                    AS billsCreated, ");
        query.append(" 0                                    AS BillValueInCrores ");
        query.append(" FROM egw_mv_work_progress_register details, ");
        query.append("   egw_status status ");
        query.append(" WHERE details.lestatus = status.code ");
        query.append(" AND status.code       IN ('TECHNICAL_SANCTIONED','ADMINISTRATIVE_SANCTIONED') ");
        query.append(filterConditions.toString());
        query.append(groupByQuery.toString());
        query.append(" UNION ");
        query.append(selectQuery.toString());
        query.append(" 0                           AS lineEstimates, ");
        query.append(" 0                           AS lineEstimateDetails, ");
        query.append(" 0                           AS adminSanctionedAmountInCrores, ");
        query.append(" 0                           AS adminSanctionedEstimates, ");
        query.append(" COUNT(details.lestatus)     AS technicalSanctionedEstimates, ");
        query.append(" 0                           AS loaCreated, ");
        query.append(" 0                           AS agreementValueInCrores, ");
        query.append(" 0                           AS workInProgress, ");
        query.append(" 0                           AS WorkCompleted , ");
        query.append(" 0                           AS billsCreated, ");
        query.append(" 0                           AS BillValueInCrores ");
        query.append(" FROM egw_mv_work_progress_register details, ");
        query.append(" egw_status status ");
        query.append(" WHERE details.lestatus = status.code ");
        query.append(" AND status.code       IN ('TECHNICAL_SANCTIONED') ");
        query.append(filterConditions.toString());
        query.append(groupByQuery.toString());
        query.append(" UNION ");
        query.append(selectQuery.toString());
        query.append(" 0                                     AS lineEstimates, ");
        query.append(" 0                                     AS lineEstimateDetails, ");
        query.append(" 0                                     AS adminSanctionedAmountInCrores, ");
        query.append(" 0                                     AS adminSanctionedEstimates, ");
        query.append(" 0                                     AS technicalSanctionedEstimates, ");
        query.append(" COUNT(details.ledid)                  AS loaCreated, ");
        query.append(" SUM(details.agreementamount)/10000000 AS agreementValueInCrores, ");
        query.append(" 0                                     AS workInProgress, ");
        query.append(" 0                                     AS WorkCompleted, ");
        query.append(" 0                                     AS billsCreated, ");
        query.append(" 0                                     AS BillValueInCrores ");
        query.append(" FROM egw_mv_work_progress_register details ");
        query.append(" WHERE details.agreementnumber IS NOT NULL ");
        query.append(" AND details.wostatuscode       = 'APPROVED' ");
        query.append(filterConditions.toString());
        query.append(groupByQuery.toString());
        query.append(" UNION ");
        query.append(selectQuery.toString());
        query.append(" 0                             AS lineEstimates, ");
        query.append(" 0                             AS lineEstimateDetails, ");
        query.append(" 0                             AS adminSanctionedAmountInCrores, ");
        query.append(" 0                             AS adminSanctionedEstimates, ");
        query.append(" 0                             AS technicalSanctionedEstimates, ");
        query.append(" 0                             AS loaCreated, ");
        query.append(" 0                             AS agreementValueInCrores, ");
        query.append(" COUNT(DISTINCT details.ledid) AS workInProgress, ");
        query.append(" 0                             AS WorkCompleted, ");
        query.append(" 0                             AS billsCreated, ");
        query.append(" 0                             AS BillValueInCrores ");
        query.append(" FROM egw_mv_work_progress_register details ");
        query.append(" WHERE ");
        query.append(workInProgessCondition.toString());
        query.append(filterConditions.toString());
        query.append(groupByQuery.toString());
        query.append(" UNION ");
        query.append(selectQuery.toString());
        query.append(" 0                             AS lineEstimates, ");
        query.append(" 0                             AS lineEstimateDetails, ");
        query.append(" 0                             AS adminSanctionedAmountInCrores, ");
        query.append(" 0                             AS adminSanctionedEstimates, ");
        query.append(" 0                             AS technicalSanctionedEstimates, ");
        query.append(" 0                             AS loaCreated, ");
        query.append(" 0                             AS agreementValueInCrores, ");
        query.append(" 0                             AS workInProgress, ");
        query.append(" COUNT(DISTINCT details.ledid) AS WorkCompleted, ");
        query.append(" 0                             AS billsCreated, ");
        query.append(" 0                             AS BillValueInCrores ");
        query.append(" FROM egw_mv_work_progress_register details ");
        query.append(" WHERE details.workcompleted = true ");
        query.append(filterConditions.toString());
        query.append(groupByQuery.toString());
        query.append(" UNION ");
        query.append(selectQuery.toString());
        query.append(" 0                                   AS lineEstimates, ");
        query.append(" 0                                   AS lineEstimateDetails, ");
        query.append(" 0                                   AS adminSanctionedAmountInCrores, ");
        query.append(" 0                                   AS adminSanctionedEstimates, ");
        query.append(" 0                                   AS technicalSanctionedEstimates, ");
        query.append(" 0                                   AS loaCreated, ");
        query.append(" 0                                   AS agreementValueInCrores, ");
        query.append(" 0                                   AS workInProgress, ");
        query.append(" 0                                   AS WorkCompleted , ");
        query.append(" COUNT(DISTINCT billdetail.billid)   AS billsCreated, ");
        query.append(" SUM(billdetail.billamount)/10000000 AS BillValueInCrores ");
        query.append(" FROM egw_mv_work_progress_register details , ");
        query.append(" egw_mv_estimate_abstract_by_department_billdetail billdetail ");
        query.append(" WHERE billdetail.ledid = details.ledid ");
        query.append(filterConditions.toString());
        query.append(groupByQuery.toString());
        query.append(" ) final ");
        query.append(mainGroupByQuery.toString());
        return query.toString();
    }

}
