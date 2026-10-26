package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.ClassPayRate;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeDeduction;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.LpcDeputation;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.Usr;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.RestController;

 

@RestController
public class Revdep extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {

	Employee employee;
	private StringBuffer buffer = new StringBuffer();
	List<EmployeeServiceDetail> esdList;
	List<EmployeeDeduction>empDeductList;
	private String message;
	static final public String INVALID_COLUMN7A = "AU:Deputation agency in col.7A not matching with TFRDEP DOII.\n";
	static final public String INVALID_DATE = "AU:REVDEP from date is prior to TFRDEP from date.\n";
	static final public String NO_CORR_TFRDEP = "AU:No Corresponding Tfrdep Part II Order available.\n";
	
	public Revdep() {
		HttpSession sess = Utils.currentSession();
		if(sess != null) {
			usr = (Usr)sess.getAttribute("loggedInUsr");
		}
	}

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;

		if (!isValid7AColumn(do2)) {
			buffer.append(INVALID_COLUMN7A);
			validity = false;
		}

		if (!isValidDate(do2)) {
			buffer.append(INVALID_DATE);
			validity = false;
		}
		
		if (newDo2.getId()==do2.getId()) {
			if(!isTfrDepExists(do2)){
				buffer.append(NO_CORR_TFRDEP);
				validity =false;
			}
		}
		
		if (!validity) {
			do2.setStatus(Do2Manager.PENDING);
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		newAuditCage.setTransport(0);
		newAuditCage.setDeputationStatus(null);
		newAuditCage.setDeputationFlag(false);
		newAuditCage.setAbsentFlag(false);
		Date tfrdepDate=null;
		boolean alwrevDo2Found=false;
		
	    List<Do2> tfrdepDo2List = Utils.getReferenceDo2ForDeputation(newAuditCage.getDo2(),Do2Manager.TFRDEP);
	    if (tfrdepDo2List.size() >0){	    	   
	    	  tfrdepDate = tfrdepDo2List.get(0).getFromDate();   
	    }
	    List<Do2> alwrevDo2List = do2Repo.findByEmployeeIdAndDescriptionAndColumnA(newAuditCage.getEmployee().getId(), "ALWREV", "6CPC");
	    		
	    /*		Utils.getEntityManager().createQuery("select d from Do2 d where d.employee=:employee and "
	    		+ " d.description='ALWREV' and d.columnA='6CPC' ").setParameter("employee", employee).getResultList();*/
	    
	    if (alwrevDo2List.size()> 0) {
	    	alwrevDo2Found=true;
	    } 

	    if (tfrdepDate !=null && tfrdepDate.before(DateManager.getSixthCpcAllowanceDate())
	    		&& !newAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
	    	if (baseAuditCage.getClassPay() != null	&& baseAuditCage.getClassPay() != 0) {
	    		if (alwrevDo2Found) {
					newAuditCage.setClassPay((int) getRevisedClasspayRate(newAuditCage,tfrdepDate));
	    		}
			}
	    }
		newAuditCage.setPmha(AuditCageHelper.getPmhaRate(newAuditCage.getDo2(), newAuditCage));
		newAuditCage.setLra((int)AuditCageHelper.getLraRate(newAuditCage));
	//	Utils.getEntityManager().flush();
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		if (do2.getId() == newDo2.getId()) {
			updateBdtInfo(do2);
		}
	
		
		if (do2.getFromDate().after(DateManager.getCgguardDeputationDate())) {
			if (Do2Manager.getValidStopPayDeputationFromAug2016().contains(do2.getColumnA())){
				updateLpcDeputation(do2, employee);
			}
		} else {
			if (Do2Manager.getValidStopPayDeputation().contains(do2.getColumnA())){
				updateLpcDeputation(do2, employee);
			}
		}
	
		AuditCageHelper.stopAllAllowances(newAuditCage);
		
		if (employee.getStrincRank() != null && !do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
			reviseStrIncRate();
		}	
		
		return Do2Manager.SUCCESS;
	}

	public void updateEmployee(Do2 do2, Employee employee) {
	 	employee.setDeputationFlag(false);
		employee.setRecordStatus(Do2Manager.VALID_CHAR+"");
		if (!employee.getFsDueDate().after(do2.getFromDate()) && employee.getStatusCode().getId()<5) {
			employee.setFsDueDate(null);
			employee.setPresentUnitDate(do2.getFromDate());
			employee.setUnit(do2.getUnit());
		}
	}

	public void updateEmployeeServiceDetails(Do2 do2, Employee employee) {
		esdList = esdRepo.findByEmpIdStatusToDateNullOrderByFromdateDesc(employee.getId(), Do2Manager.getDeputationStatusCodeMap().get(do2.getColumnA()).charAt(0)+"");
				
				
		/*		Utils.getEntityManager().createQuery("select esd from EmployeeServiceDetail esd where esd.employee=:employee AND esd.status=:status " +
				"  AND esd.toDate=null order by fromDate DESC")
				.setParameter("employee", employee).setParameter("status",Do2Manager.getDeputationStatusCodeMap().get(do2.getColumnA()).charAt(0))
				.getResultList();*/

		if (esdList.size() > 0) {
			esdList.get(0).setToDate(DateManager.getPreviousDate(do2.getFromDate()));
		}
	}
	//@In
	Usr usr;
	
	public void updateLpcDeputation(Do2 do2, Employee employee) {
		LpcDeputation lpcDeputation = new LpcDeputation();
		lpcDeputation.setEmployee(employee);
		lpcDeputation.setDo2(do2);
		lpcDeputation.setLpcType("I");
		lpcDeputation.setRecordStatus("V");
		/*if(do2.getUsr() != null){
			lpcDeputation.setUsr(do2.getUsr());
			Utils.setActionDate(do2.getUsr(), lpcDeputation);
		}*/
		lpcDeputation.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		lpcDeputation.setArmyNo(do2.getArmyNo());
		lpcDeputation.setCheckDigit(do2.getCheckDigit());
		lpcDepRepo.save(lpcDeputation);
	//	Utils.getEntityManager().persist(lpcDeputation);
	}
	
	/*public void updateEmployeeDeduction(Do2 do2, Employee employee) {
		empDeductList = Utils.getEntityManager().createQuery("select empdeduct from EmployeeDeduction empdeduct  where " +
				" empdeduct.employee=:employee And empdeduct.qe=:qe")
				.setParameter("employee", employee)
				.setParameter("qe", Utils.getCurrentQuarter())
				.getResultList();
	//	System.out.println("empDeductList" +empDeductList.size());
		if(empDeductList.size()==0) {
		EmployeeDeduction empDeduction = new EmployeeDeduction();
		empDeduction.setEmployee(employee);
		empDeduction.setQe(Utils.getCurrentQuarter());
		empDeduction.setOpeningBalance(0.0);
		empDeduction.setAcrTotalAmount(0.0);
		empDeduction.setAdjustmentTotalAmountCr(0.0);
		empDeduction.setAdjustmentTotalAmountDb(0.0);
		empDeduction.setAgisAmount(0.0);
		empDeduction.setBhutanTotalAmount(0.0);
		empDeduction.setCbTotalAmountCr(0.0);
		empDeduction.setCbTotalAmountDb(0.0);
		empDeduction.setFamoTotalAmount(0.0);
		empDeduction.setFundSubscriptionRefund(0.0);
		empDeduction.setIncomeTaxEc(0.0);
		empDeduction.setLoanRecoveryAmount(0.0);
		empDeduction.setPliAmount(0.0);
		empDeduction.setTransferInAmount(0.0);
		empDeduction.setEmployeeType('D');
		empDeduction.setCreatedAt(new Date());
		Utils.getEntityManager().persist(empDeduction);
	}
	}*/
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		List<Do2> do2List = Utils.getReferenceDo2ForDeputation(refDo2, Do2Manager.TFRDEP);
		if (do2List.size() > 0) {
			Do2 tfrDepDo2 = do2List.get(0);
			AuditCage tfrDepAuditCage = Utils.getAuditCage(tfrDepDo2);
			if (tfrDepAuditCage !=null){
				
				if (tfrDepDo2.getFromDate().after(DateManager.getCgguardDeputationDate())) {
					if (Do2Manager.getValidStopPayDeputationFromAug2016().contains(tfrDepDo2.getColumnA())){
						tfrDepAuditCage.setToDate(null);
						tfrDepAuditCage.setAbsentFlag(true);
					}
				} else {
					if (Do2Manager.getValidStopPayDeputation().contains(tfrDepDo2.getColumnA())){
						tfrDepAuditCage.setToDate(null);
						tfrDepAuditCage.setAbsentFlag(true);
					}
				}
					
				
				tfrDepAuditCage.setDeputationFlag(true);
			}
			
		//	Utils.getEntityManager().persist(tfrDepAuditCage);
			
			employee.setDeputationFlag(true);
			employee.setRecordStatus(Do2Manager.getDeputationStatusCodeMap().get(refDo2.getColumnA()).charAt(0)+"");
			empRepo.save(employee);
		//	Utils.getEntityManager().persist(employee);
		}
		
		esdList = esdRepo.findAllByEmployeeIdAndStatusOrderByFromDateDesc(employee.getId(),Do2Manager.getDeputationStatusCodeMap().get(refDo2.getColumnA()).charAt(0)+"");
				
		/*		Utils.getEntityManager().createQuery("select esd from EmployeeServiceDetail esd where esd.do2.employee = :employee AND esd.status=:status order by fromDate DESC")
				.setParameter("employee", employee).setParameter("status", Do2Manager.getDeputationStatusCodeMap().get(refDo2.getColumnA()).charAt(0))
				.getResultList();*/

		if (esdList.size() > 0) {
			esdList.get(0).setToDate(null);
 		}
		
		lpcDepRepo.deleteFromLpcDepForDo2(refDo2.getId());
	//	Utils.getEntityManager().createNativeQuery("DELETE FROM lpc_deputation where fk_do2=:do2")
	//	.setParameter("do2", refDo2).executeUpdate();		
	
		return Do2Manager.SUCCESS;
	}

	public boolean isValid7AColumn(Do2 do2) {
		List<Do2> do2List = Utils.getReferenceDo2ForDeputation(do2,
				Do2Manager.TFRDEP);
		return do2List.size() > 0
				&& do2List.get(0).getColumnA().equals(do2.getColumnA());
	}

	public boolean isValidDate(Do2 do2) {
		List<Do2> do2List = Utils.getReferenceDo2ForDeputation(do2,	Do2Manager.TFRDEP);
		return do2List.size() > 0
				&& do2List.get(0).getFromDate().before(do2.getFromDate());
			
	}
	public boolean isTfrDepExists(Do2 do2){
		
		Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
		       List<Do2> do2List = do2Repo.findByFromDateLessThanEqualAndDo2idNotEqualAndDescOrderByFromDateDesc(emp.getId(),"REVDEP",do2.getFromDate(),
		    		   																			Do2Manager.VALID, do2.getId());
		    		   
		    		   
		    	/*	   Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:emp and d.description=:description and d.fromDate<=:fromDate and d.id !=:id and d.status =:status order by d.fromDate desc")
		       											   .setParameter("emp", emp)
		       											   .setParameter("description","REVDEP")
		       											   .setParameter("fromDate",do2.getFromDate())
		       											   .setParameter("id",do2.getId())
		       											   .setParameter("status",Do2Manager.VALID)
		       											   .getResultList();*/
		       if(do2List.size() ==0){
		    	   return true;
		       }else{
		    	    Date revDepDate = do2List.get(0).getFromDate();
		    	    List<Do2> tfrDepList = do2Repo.findByFromDateGreaterThanEqualToAndFromDateLessThanEqualToAndDesc(emp.getId(),"TFRDEP",
		    	    																						revDepDate,do2.getFromDate());
		    	    		
		    	/*    		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:emp and d.description=:description and d.fromDate>=:fromDate and d.fromDate<=:toDate")
					   .setParameter("emp", emp)
						   .setParameter("description","TFRDEP")
						   .setParameter("fromDate",revDepDate)
						   .setParameter("toDate",do2.getFromDate())
						   .getResultList();*/
		    	    if(tfrDepList.size() >0)
		    	    	return true;
		    	    

		       }
		      return false;
		       											  
	}
	
	public double getRevisedClasspayRate(AuditCage auditCage,Date tfrdepDate) {
        
		double classPay=auditCage.getClassPay();
		GroupCategory catg = Utils.getGroupCategory(auditCage.getDo2(),"CUR");
		String soldierClass="";
		
		List<ClassPayRate> classPayRateList1 = classPayRepo.findByGroupCatRateFromDateBetweenOrFromDateGreaterThan(catg.getId(),
																										classPay, tfrdepDate);
				
			/*	Utils.getEntityManager().createQuery(
				"SELECT cpr from ClassPayRate cpr where cpr.groupCategory= :groupCategory " +
				" and cpr.rate = :rate and " +
				" ((:fromDate between cpr.fromDate and cpr.toDate) or " +
				" (:fromDate >= cpr.fromDate and cpr.toDate is null))")
		.setParameter("groupCategory", catg )
		.setParameter("rate", classPay)
		.setParameter("fromDate", tfrdepDate)
		.getResultList();*/
         
	
		if(classPayRateList1.size() >0){
			soldierClass = classPayRateList1.get(0).getClassPayCode()+"";
		}
		
		List<ClassPayRate> classPayRateList = classPayRepo.findByGroupCatClassPayCodeFromDateBetweenOrFromDateGreaterThan(catg.getId(),
				soldierClass, auditCage.getDo2().getFromDate());
				
				
		/*		Utils.getEntityManager().createQuery(
				"SELECT cpr from ClassPayRate cpr where cpr.groupCategory= :groupCategory " +
				" and cpr.classPayCode = :currentClassPayCode and " +
				" ((:fromDate between cpr.fromDate and cpr.toDate) or " +
				" (:fromDate >= cpr.fromDate and cpr.toDate is null))")
				.setParameter("groupCategory", catg)
				.setParameter("currentClassPayCode", soldierClass)
				.setParameter("fromDate", auditCage.getDo2().getFromDate())
				.getResultList();*/
	
		
		if (classPayRateList.size() > 0) {
	 	//	classPay =  classPayRateList.get(0).getRate().intValue();	 
	 		classPay =  classPayRateList.get(0).getRate();
		}
		
		return classPay;

	}


	public void reviseStrIncRate() {
		List<AuditCageAllowance> newAuditCageAllowanceList = acaRepo.findAllByAuditCageId(newAuditCage.getId());
				
	/*			Utils
				.getEntityManager()
				.createQuery(
						"SELECT aca FROM AuditCageAllowance aca WHERE aca.auditCage=:auditCage ")
				.setParameter("auditCage", newAuditCage).getResultList();*/
		for (AuditCageAllowance aca : newAuditCageAllowanceList) {
			if (aca.getPayCode().getPayCode().equals("STRINC")) {
				Rank rank = employee.getStrincRank();

				List<Allowance> alwList = Utils.getApplicableAllowanceRates(
						"STRINC", newAuditCage.getDo2().getFromDate(),
						newAuditCage.getDo2().getFromDate(), rank);
				if (alwList.size() > 0) {
					acaRepo.delete(aca);
				//	Utils.getEntityManager().remove(aca);
					newAuditCage.getAuditCageAllowances().remove(aca);	//	line added on 09.10.2020
				 	AuditCageAllowance allowanceNewRecord = new AuditCageAllowance();
					allowanceNewRecord.setAuditCage(newAuditCage);
					allowanceNewRecord.setPayCode(aca.getPayCode());
					allowanceNewRecord.setDo2(aca.getDo2());
					allowanceNewRecord.setAmount((int) alwList.get(0).getRateAmount());
					allowanceNewRecord.setCreatedAt(new Timestamp(System.currentTimeMillis()));
					acaRepo.save(allowanceNewRecord);
			//		Utils.getEntityManager().persist(allowanceNewRecord);
				//	Utils.getEntityManager().flush();
					//aca.setAmount((int) alwList.get(0).getRateAmount());
				}
			}
		}
	}
	@Override
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2, employee);
		updateEmployeeServiceDetails(do2, employee);
	}

}
