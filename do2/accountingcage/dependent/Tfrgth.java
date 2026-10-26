package itsdc.dolphin.do2.accountingcage.dependent;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.LpcRegister;
import itsdc.dolphin.models.Unit;
import itsdc.dolphin.do2.AccountingCageBase;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.do2.auditcage.AuditCageHelper;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 

/**
 * 
 * @author ITSDC, Sec'bad.
 *
 */

@RestController
public class Tfrgth extends AccountingCageBase implements java.io.Serializable {
	
	public StringBuffer buffer = new StringBuffer(); 
	Employee employee;
	public List<Allowance> alwList;
	Date cbillbegDate=null;
	Date cbillendDate=null;
	final static public String DUPLICATE_UNITMOVE = "AU:Transfer Grant not admissible for Second UNITMOVE within 6 months.\n"; 
//	final static public String DUPLICATE_INDMOVE = "AU:Transfer Grant not admissible for Second INDMOVE within 6 months.\n";
	final static public String GRANT_DATE_INVALID = "AU:Transfer Grant From Date is not tallying with POSIN/TFRIN Date.\n"; 
	final static public String REF_DO2_INVALID = "AU:Invalid Reference Do2.\n"; 
 	final static public String INVALID_UNIT = "VL:To be published by POSIN unit only.\n"; 
	final static public String INVALID_FROM_DATE = "AU:TFRGT already granted for same From Date.\n";
	final static public String ALLOWANCE_RATE_NOT_AVAILABLE = "AU:Allowance Rate is not available.\n";
	final static public String REVDEP_AVAILABLE = "AU:TFRGT Not admissible on reversion from deputation \n";
	static public final String NOT_IN_FS_LIST = "AU:This Army No is Not a FS Case/Fs Case Entry Not approved. \n.";
	final static public String DUPLICATE_DO2 = "AU:Transfer Grant Already Adjusted With Different Date In This Month.\n";
	static public Date revised_appxj_impl_date ;
	
	
	@Override
	public boolean isValid(Do2 do2) {
		
		boolean validity = true;
		revised_appxj_impl_date = DateManager.getRevisedAppendixJImplementationDate();
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		try {
		   cbillbegDate=DateManager.getDate("2013-04-01");
		   cbillendDate=DateManager.getDate("2014-07-31");
		} catch (Exception e) {
			Utils.handleException(e);
		}
		
		if (employee.getStatusCode().getId()>6) {
			if (do2.getColumnB()!=null && do2.getColumnB().equals("INDMOVE")
					&& (do2.getRefDo2No()==null || (do2.getRefDo2No()!=null && do2.getRefDo2No().length()==0))) {
				do2.setStatus(Do2Manager.INVALID);
				do2.setReason(" NO REF DO2 FOUND AND THE PBOR NON EFFECTIVE - PLEASE CHECK");
				return false;
			}
			if (isNe(employee, do2)) {
				do2.setStatus(Do2Manager.INVALID);
				do2.setReason("TFRGTH ALREADY EXISTS OR ON OR AFTER DISCHARGE DATE - PLEASE CHECk");
				return false;
			}
		}
		
		if (!do2.getFromDate().before(cbillbegDate) && !do2.getFromDate().after(cbillendDate)){
			validity = false;
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason("To be claimed as advance through IAFA 194 between 1.4.2013 and 31.7.2014");	
			do2Repo.save(do2);
			return false;
		}		
		if (do2.getColumnB().equals("INDMOVE")) {
			if(!isValidRefDo2(do2)) {
				validity = false;
				buffer.append(REF_DO2_INVALID) ; 
				do2.setStatus(Do2Manager.INVALID);
			}
		}
		
		if(do2.getColumnB().equals("INDMOVE")) {
			if(!isValidGrantDate(do2)) {
				validity = false;
				buffer.append(GRANT_DATE_INVALID);
				do2.setStatus(Do2Manager.INVALID);
			}
		}
 		if (do2.getColumnB().equals("UNITMOVE")) {// commented on 24.01.2023 to allow 2 unitmoves within 6 months and to restrict two INDmoves within 6 months.
			if (!isValidMove(do2)) {
				validity = false;
				buffer.append(DUPLICATE_UNITMOVE);
				do2.setStatus(Do2Manager.INVALID);
			}
		}  
/*		if (do2.getColumnB().equals("INDMOVE")) { // commented on 20.02.2025 to allow 2 indmoves within 6 months and to restrict two unitmoves within 6 months.
			if (!isValidMove(do2)) {
				validity = false;
				buffer.append(DUPLICATE_INDMOVE);
				do2.setStatus(Do2Manager.INVALID);
			}
		} */
		if (do2.getColumnB().equals("INDMOVE")) {
			if (!isValidUnit(do2)) {
				validity = false;
				buffer.append(INVALID_UNIT);
				do2.setStatus(Do2Manager.INVALID);
			}
		}
		if (!isValidFromDate(do2)) {
			validity = false;
			buffer.append(INVALID_FROM_DATE);
			do2.setStatus(Do2Manager.INVALID);
		}

		if (!isValidAllowanceRate(do2)) {
			validity = false;
			buffer.append(ALLOWANCE_RATE_NOT_AVAILABLE);
			do2.setStatus(Do2Manager.PENDING);
		}
		if (!isRevdepDo2Available(do2, employee)) {
			validity = false;
			buffer.append(REVDEP_AVAILABLE);
		}

		if (isTfrgthAlreadyAdjustedInCurrentMonth(do2, employee)) {
			validity = false;
			buffer.append(DUPLICATE_DO2);
			do2.setStatus(Do2Manager.INVALID);
		}
		
		if (!validity) {
			if (do2.getStatus() != null && !do2.getStatus().equals(Do2Manager.PENDING))
			     do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
			if (do2.getAcceptedQe()==null){
				do2.setAcceptedQe(Utils.getCurrentQuarter());	
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())){
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		}
	 		
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		
		return validity;
	}
	
	public boolean isValidAllowanceRate(Do2 do2) {
		alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnA(), do2.getFromDate(), do2.getFromDate());
		return (alwList != null);
	}
	
	@Override
	public String process(Do2 do2) {
		double basicPay = 0;
		double gradePay = 0;
		double msPay =0;
		double xPay = 0;
		double classPay = 0;
		double dp = 0;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> ac = acRepo.findByFromDateLessThanEqualValidOrderByFromDateDescToDateDesc(employee.getId(), do2.getFromDate());
				
				
	/*			Utils.getEntityManager().createQuery("SELECT ac FROM AuditCage ac " + 
		" WHERE ac.employee=:employee AND ac.effective=true AND ac.fromDate<=:frDt " +
		" AND ac.absentFlag=false AND ac.cancelled=false AND ac.recordStatus!='P' " +
										" AND ac.deputationFlag=false ORDER BY ac.fromDate DESC, ac.toDate DESC")
		.setParameter("employee", employee)
		.setParameter("frDt", do2.getFromDate()).getResultList();*/
/*        if (ac.size() > 0) {
        	if (!ac.get(0).getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
        		if(ac.get(0).getBasicPay() != null) {
        			basicPay = ac.get(0).getBasicPay();
        		}
        		if(ac.get(0).getGradePay() != null) {
        			gradePay = ac.get(0).getGradePay();
        		}
        		if(ac.get(0).getMsPay() != null) {
        			msPay = ac.get(0).getMsPay();
        		}
        		if(ac.get(0).getXGroupPay() != null) {
        			xPay = ac.get(0).getXGroupPay();
        		}
        	}
        	else if (ac.get(0).getFromDate().after(DateManager.getSixthCpcDate())|| 
        			 ac.get(0).getFromDate().equals(DateManager.getSixthCpcDate())){
        		if(ac.get(0).getOldBasicPay() != null) {
        			basicPay = ac.get(0).getOldBasicPay();
        		}
        		dp=Math.round(basicPay * 0.5);
        	}
        	else {
        		if(ac.get(0).getBasicPay() != null) {
        			basicPay = ac.get(0).getBasicPay();
        		}
        		if(ac.get(0).getDp() != null) {
        			dp = ac.get(0).getDp();
        		}
        		if(ac.get(0).getClassPay() != null) {
        			classPay = ac.get(0).getClassPay();
        		}
        	}
		}*/
		
		if (ac.size() > 0) {
			if (ac.get(0).getFromDate().before(DateManager.getSixthCpcDate())) {
				if(ac.get(0).getBasicPay() != null) {
        			basicPay = ac.get(0).getBasicPay();
        		}
        		if(ac.get(0).getDp() != null) {
        			dp = ac.get(0).getDp();
        		}
        		if(ac.get(0).getClassPay() != null) {
        			classPay = ac.get(0).getClassPay();
        		}
			} else if (ac.get(0).getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				if(ac.get(0).getOldBasicPay() != null) {
        			basicPay = ac.get(0).getOldBasicPay();
        		}
        		dp=Math.round(basicPay * 0.5);
			} else if (ac.get(0).getFromDate().before(DateManager.getSeventhCpcDate())) {
				if(ac.get(0).getBasicPay() != null) {
        			basicPay = ac.get(0).getBasicPay();
        		}
        		if(ac.get(0).getGradePay() != null) {
        			gradePay = ac.get(0).getGradePay();
        		}
        		if(ac.get(0).getMsPay() != null) {
        			msPay = ac.get(0).getMsPay();
        		}
        		if(ac.get(0).getXGroupPay() != null) {
        			xPay = ac.get(0).getXGroupPay();
        		}
			} else if (ac.get(0).getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
				
				if (ac.get(0).getPayLevel().equals("0")) {
					if(ac.get(0).getBasicPay() != null) {
	        			basicPay = ac.get(0).getBasicPay();
	        		}
	        		if(ac.get(0).getGradePay() != null) {
	        			gradePay = ac.get(0).getGradePay();
	        		}
	        		if(ac.get(0).getMsPay() != null) {
	        			msPay = ac.get(0).getMsPay();
	        		}
	        		if(ac.get(0).getXGroupPay() != null) {
	        			xPay = ac.get(0).getXGroupPay();
	        		}
				} else {
					if(ac.get(0).getOldBasicPay() != null) {
	        			basicPay = ac.get(0).getOldBasicPay();
	        		}
	        		if(ac.get(0).getOldGradePay() != null) {
	        			gradePay = ac.get(0).getOldGradePay();
	        		}
	        			msPay = Utils.get6cpcMsPay(ac.get(0));
	        	       		
	        			xPay = Utils.get6cpcXGroupPay(ac.get(0));
				}
				
				
			} else {
				if(ac.get(0).getBasicPay() != null) {
        			basicPay = ac.get(0).getBasicPay();
        		}
        		/*if(ac.get(0).getGradePay() != null) {
        			gradePay = ac.get(0).getGradePay();
        		}*/
				// mspay not included with effect from 01-07-2017 for TFRGTH
        		/*if(ac.get(0).getMsPay() != null) {
        			msPay = ac.get(0).getMsPay();
        		}
        		if(ac.get(0).getXGroupPay() != null) {
        			xPay = ac.get(0).getXGroupPay();
        		}*/
			}
		}
				
		
		
		
		alwList = new ArrayList<Allowance>();
		
	
		alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnA(), do2.getFromDate(), do2.getFromDate());
		
		if(alwList.size() >0){
			// class pay has been removed wef 01092008 from TFRGTh vide HQ letter no AT/I/3510/6thCPC/VI dated 01.08.2012	
			// mspay has been removed wef 01072017 (80% of basicpay,xpay only admissible)
			if ( do2.getColumnC() != null && do2.getColumnC().equals("ISLANDS") && !do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())){
				Utils.addArrears(do2, ((basicPay + dp + gradePay + msPay + xPay + classPay) *1));
			}else {
				Utils.addArrears(do2, ((basicPay + dp + gradePay + msPay + xPay + classPay) * alwList.get(0).getRatePercentage()));
			}
			
		}
		
		return Do2Manager.SUCCESS;
	}	 

	public boolean isValidRefDo2(Do2 do2) {
		
		/*if  (Do2Manager.HRMS_IMPLEMENTED_PAO_LIST.contains(Utils.getPao().getPaoCode()) && do2.getFromDate().after(cbillendDate)){
			return true;
		}*/
	/*System.out.println ( " do2 from date " +do2.getFromDate() );
	System.out.println ( " revised_appx_iple_date " +revised_appxj_impl_date);
	System.out.println ( " cbillbegDate "+cbillbegDate  +" cbillendDate " +cbillendDate);
	System.out.println ( " do2 date " +do2.getDo2Date());*/

		
			if (Do2Manager.HRMS_IMPLEMENTED_PAO_LIST.contains(Utils.getPao().getPaoCode())  || Utils.getPao().getPaoCode().equals(Do2Manager.APS_NAGPUR)) {
				
						
						//if (!do2.getDo2Date().before(revised_appxj_impl_date) && do2.getFromDate().after(cbillendDate)) {
						if (!do2.getDo2Date().before(revised_appxj_impl_date) && (do2.getFromDate().before(cbillbegDate) || do2.getFromDate().after(cbillendDate)))  {
							return true;
						} //else if(!do2.getDo2Date().after(revised_appxj_impl_date) && do2.getFromDate().before(cbillbegDate) && do2.getFromDate().after(cbillendDate)) {
					else if(!do2.getDo2Date().after(revised_appxj_impl_date) ){
				System.out.println ( " AAAAAAAAAAAAAAA ");
						List<Do2> do2List = Utils.getReferenceDo2List(do2);
						for(Do2 d : do2List){
							if (do2.getDescription().equals(Do2Manager.TRANSFER_GRANT_HIGH)){
								if((d.getDescription().equals(Do2Manager.POSIN) || d.getDescription().equals("TFRIN"))){
									return true;
								}
							}
							if (do2.getDescription().equals(Do2Manager.TRANSFER_GRANT_LOW)){
								if(d.getDescription().equals(Do2Manager.POSIN)){
									return true;
								}
							}
						}
					}
						
				}  else {
					List<Do2> do2List = Utils.getReferenceDo2List(do2);
					for(Do2 d : do2List){
						if (do2.getDescription().equals(Do2Manager.TRANSFER_GRANT_HIGH)){
							if((d.getDescription().equals(Do2Manager.POSIN) || d.getDescription().equals("TFRIN"))){
								return true;
							}
						}
						if (do2.getDescription().equals(Do2Manager.TRANSFER_GRANT_LOW)){
							if(d.getDescription().equals(Do2Manager.POSIN)){
								return true;
							}
						}
					}
				}
				
		
	
		
		return false;
	}
	
	public boolean isValidGrantDate(Do2 do2) {  
		
		/*if (Do2Manager.HRMS_IMPLEMENTED_PAO_LIST.contains(Utils.getPao().getPaoCode()) && do2.getFromDate().after(cbillendDate)){
			return true;
		}*/
		
	
			if (Do2Manager.HRMS_IMPLEMENTED_PAO_LIST.contains(Utils.getPao().getPaoCode())) {
				//if (!do2.getDo2Date().before(revised_appxj_impl_date) && do2.getFromDate().after(cbillendDate))  {
				if (!do2.getDo2Date().before(revised_appxj_impl_date) && (do2.getFromDate().before(cbillbegDate) || do2.getFromDate().after(cbillendDate)))  {
					return true;
				} //else if(!do2.getDo2Date().after(revised_appxj_impl_date) && do2.getFromDate().before(cbillbegDate) && do2.getFromDate().after(cbillendDate)) {
				else if(!do2.getDo2Date().after(revised_appxj_impl_date) ) {
					List<Do2> do2List = Utils.getReferenceDo2List(do2);
					for (Do2 d : do2List){
						if (do2.getDescription().equals(Do2Manager.TRANSFER_GRANT_HIGH)){
							   if ((d.getDescription().equals(Do2Manager.POSIN) || d.getDescription().equals("TFRIN")) && (d.getFromDate().equals(do2.getFromDate()))){
								   return true;
							   }
						}
						if (do2.getDescription().equals(Do2Manager.TRANSFER_GRANT_LOW)){
							if (((d.getDescription().equals(Do2Manager.POSIN)) && (d.getFromDate().equals(do2.getFromDate())))){
									return true;
							}
						}			 	
					}
			    }
				
			} else {
				List<Do2> do2List = Utils.getReferenceDo2List(do2);
				for (Do2 d : do2List){
					if (do2.getDescription().equals(Do2Manager.TRANSFER_GRANT_HIGH)){
						   if ((d.getDescription().equals(Do2Manager.POSIN) || d.getDescription().equals("TFRIN")) && (d.getFromDate().equals(do2.getFromDate()))){
							   return true;
						   }
					}
					if (do2.getDescription().equals(Do2Manager.TRANSFER_GRANT_LOW)){
						if (((d.getDescription().equals(Do2Manager.POSIN)) && (d.getFromDate().equals(do2.getFromDate())))){
								return true;
						}
					}			 	
				}
			}
		
		
		
				
		return false;
	}
	//method replace on 23042012(to admit transfer grant on revdep from imtrat
	public boolean isRevdepDo2Available(Do2 do2, Employee employee) {
		List<Do2> do2List =do2Repo.findByEmployeeIdAndFromDateAndStatusAndDescription(employee.getId(), do2.getFromDate(), Do2Manager.VALID, Do2Manager.REVDEP);
				
		/*		Utils.getEntityManager().createQuery("select do from Do2 do " + 
				"where do.employee =:employee and do.description=:revdep and " +
				"do.fromDate =:fromdt  and do.status =:vld")
				.setParameter("employee", employee)
				.setParameter("revdep", Do2Manager.REVDEP)
				.setParameter("vld",Do2Manager.VALID)
				.setParameter("fromdt", do2.getFromDate()).getResultList();*/
		
		if(do2List.size()>0){
			
			if(do2List.get(0).getColumnA()!=null && (do2List.get(0).getColumnA().equals("IMTRAT") || do2List.get(0).getColumnA().equals("EMBASSY"))){
				return true;
			} else if(do2List.get(0).getColumnA()!=null && (do2List.get(0).getColumnA().equals("CGUARD") && !do2.getFromDate().before(DateManager.getCgguardDeputationDate()))){
				return true;
			}else{
				return false;
			}
		}
		return(do2List.size() == 0);
	}
	
	public boolean isValidMove(Do2 do2) {

		List<Do2> refDo2List = do2Repo.findByColumnBMultipleDescDo2idNotEqualOrderByFromdate(employee.getId(), Do2Manager.TRANSFER_GRANT_HIGH,
															Do2Manager.TRANSFER_GRANT_LOW, do2.getId(), do2.getColumnB(), Do2Manager.VALID);
				
		/*		Utils.getEntityManager().createQuery("select refdo from Do2 refdo " + 
				"where refdo.employee=:employee and (refdo.description=:tfrgth or refdo.description=:tfrgtl) " +
				"and refdo.id !=:id and refdo.columnB=:columnB and refdo.status=:vld order by refdo.fromDate ")
				.setParameter("employee", employee)
				.setParameter("tfrgth", Do2Manager.TRANSFER_GRANT_HIGH)
				.setParameter("tfrgtl", Do2Manager.TRANSFER_GRANT_LOW)
				.setParameter("id", do2.getId()).setParameter("vld", Do2Manager.VALID)
				.setParameter("columnB", do2.getColumnB()).getResultList();*/

		                                 
		if (refDo2List.size()==0)
			return true;
		
		for (Do2 oldDo2 : refDo2List) {
			
			if (!do2.getFromDate().before(oldDo2.getFromDate())) {
				if (DateManager.getNoOfDays(oldDo2.getFromDate(), do2.getFromDate()) <= 180) {
					return false;
				}
			} else {
				if (DateManager.getNoOfDays(do2.getFromDate(), oldDo2.getFromDate()) <= 180) {
					return false;
				}
			}
		}
		
		return true;
		
	}

	public boolean isValidUnit(Do2 do2) {
		/*if (Do2Manager.HRMS_IMPLEMENTED_PAO_LIST.contains(Utils.getPao().getPaoCode()) && do2.getFromDate().after(cbillendDate)){
			return true;
		}*/
		
		
		
			
			if (Do2Manager.HRMS_IMPLEMENTED_PAO_LIST.contains(Utils.getPao().getPaoCode())) {
			//	if (!do2.getDo2Date().before(revised_appxj_impl_date) && do2.getFromDate().after(cbillendDate)) {
				if (!do2.getDo2Date().before(revised_appxj_impl_date) && (do2.getFromDate().before(cbillbegDate) || do2.getFromDate().after(cbillendDate)))  {
					return true;
				} //else if(!do2.getDo2Date().after(revised_appxj_impl_date) && do2.getFromDate().before(cbillbegDate) && do2.getFromDate().after(cbillendDate)) {
				else if(!do2.getDo2Date().after(revised_appxj_impl_date) ) {
						List<Do2> do2List = Utils.getReferenceDo2List(do2);
			        for (Do2 d : do2List){
			    	   if ((d.getDescription().equals(Do2Manager.POSIN)) && (d.getUnitCode().equals(do2.getUnitCode())))
			    			   return true;
			        }
				}  
			}  else {
				List<Do2> do2List = Utils.getReferenceDo2List(do2);
		        for (Do2 d : do2List){
		    	   if ((d.getDescription().equals(Do2Manager.POSIN)) && (d.getUnitCode().equals(do2.getUnitCode())))
		    			   return true;
		        }
			}
		
		
		
		
        
	    return false;
	}
	
	public boolean isValidFromDate(Do2 do2) {
		List<Do2> refdo2 = do2Repo.findByFromdateEqualMultipleDescDo2IdNotEqualOrderByFromDateDesc(employee.getId(), do2.getId(), 
																							"TFRGTL", "TFRGTH", do2.getFromDate());
				
			/*	Utils.getEntityManager().createQuery("select refdo from Do2 refdo " + 
							"where refdo.employee=:employee and refdo.id !=:id and (refdo.description='TFRGTL' or " +
							"refdo.description='TFRGTH') and refdo.status='V' and " +
							"refdo.fromDate =:reffromdt order by refdo.fromDate desc")
							.setParameter("employee", employee).setParameter("id", do2.getId())
							.setParameter("reffromdt", do2.getFromDate())
							.getResultList();*/
		
			return (refdo2.size() == 0); 
				
	}
	
/*	public boolean isNe(Employee employee, Do2 do2){
		List<LpcRegister> lpcList= Utils.getEntityManager().createQuery(
			"select l FROM LpcRegister l where l.employee=:emp and l.recordStatus='F' and l.entryApproved=true and l.ecsGenerated=true ")
			.setParameter("emp",employee).getResultList();
		 if (lpcList.size()>0) {
			 List<Do2>do2List=Utils.getEntityManager().createQuery("select d FROM Do2 d where d.employee=:emp and d.description='POSIN' " +
			 		" and d.status='V' and d.columnA='FS' ")
				.setParameter("emp", employee).getResultList();
			 if (do2List.size()>0) {
				 Date dischargePosinDate=do2List.get(0).getFromDate();
				 if (!do2.getFromDate().before(dischargePosinDate)) {
					return true;
				 }
			 }
		 }
		 return false;
	}
	
*/

	public boolean isNe(Employee employee, Do2 do2){
		List<LpcRegister> lpcList= lpcRepo.findByRecordStatusEntryApprovedAndEcsGenerated(employee.getId());
				
		//		Utils.getEntityManager().createQuery(
	//		"select l FROM LpcRegister l where l.employee=:emp and l.recordStatus='F' and l.entryApproved=true and l.ecsGenerated=true ")
	//		.setParameter("emp",employee).getResultList();
		 if (lpcList.size()>0) {
			 List<Do2>posindo2List=do2Repo.findByEmployeeIdAndDescriptionAndColumnA(employee.getId(),"POSIN","FS");
					 
			//		 Utils.getEntityManager().createQuery("select d FROM Do2 d where d.employee=:emp and d.description='POSIN' " +
			// 		" and d.status='V' and d.columnA='FS' ")
		//		.setParameter("emp", employee).getResultList();
			 List<Do2>tfrgthList=do2Repo.findByEmployeeIdAndDescriptionAndStatusAndColumnB(employee.getId(), "TFRGTH", "FS","V");
					 
			//		 Utils.getEntityManager().createQuery("select d FROM Do2 d where d.employee=:emp and d.description='TFRGTH' " +
			//	 		" and d.status='V' and d.columnB='FS' ")
			//		.setParameter("emp", employee).getResultList();
			 if (posindo2List.size()>0  && tfrgthList.size()>0) {
				 Date dischargePosinDate=posindo2List.get(0).getFromDate();
				 if (!do2.getFromDate().before(dischargePosinDate)) {
					return true;
				 }
			 } else if (posindo2List.size()>0 && tfrgthList.size()==0) {
				 return false;
			 } 
		 }
		 return false;
	}

	
	public boolean isTfrgthAlreadyAdjustedInCurrentMonth(Do2 do2, Employee employee) {
		Calendar cal = new GregorianCalendar();
		cal.setTime(do2.getFromDate());
		
		List<Do2> do2List  =do2Repo.findByYearMonthDo2IdNotEqualColBMultipleDescOrderByFromdate(employee.getId(), do2.getId(),
				"TFRGTH", "TFRGTL", cal.get(Calendar.YEAR), ((cal.get(Calendar.MONTH))+1), do2.getColumnB());

				
		/*		Utils.getEntityManager().createQuery("select d from Do2 d where (d.description='TFRGTH' OR d.description='TFRGTL') "
				+ " and d.status='V' and extract(year from d.fromDate)=:year and extract(month from d.fromDate)=:month and "
				+ " d.employee=:emp and d.id!=:do2Id and d.columnB=:colB order by d.fromDate ")
				.setParameter("emp", employee).setParameter("year", cal.get(Calendar.YEAR)).setParameter("colB", do2.getColumnB())
				.setParameter("month", ((cal.get(Calendar.MONTH))+1)).setParameter("do2Id", do2.getId()).getResultList();*/
			
		if (do2List.size() > 0 ) {
			 for (Do2 d : do2List){
				 
				 List<Do2> do2List1  = do2Repo.findByEmployeeIdAndFromDateAndStatusAndDescription(employee.getId(), d.getFromDate(), "V", "POSIN");
						 
			/*			 Utils.getEntityManager().createQuery("select d from Do2 d where "
				 		+ " d.description='POSIN'  and d.employee=:emp and d.status='V' and d.fromDate=:fromDate  ")
							.setParameter("emp", employee)
							.setParameter("fromDate", d.getFromDate()).getResultList();*/
				 
				 if (do2List1.size()==0) {
					 return true;
				 }
			 }
			
		} /*else {
			 List<Do2> do2List2  = Utils.getEntityManager().createQuery("select d from Do2 d where "
				 		+ " d.description='POSIN'  and d.employee=:emp and d.status='V' and d.fromDate=:fromDate  ")
							.setParameter("emp", employee)
							.setParameter("fromDate", do2.getFromDate()).getResultList();
			 
			 if (do2List2.size()==0) {
				 return true;
			 }
		}*/
		return false;
	}	
}