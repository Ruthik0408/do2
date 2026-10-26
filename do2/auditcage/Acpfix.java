package itsdc.dolphin.do2.auditcage;

 

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 

@RestController
public class Acpfix extends PromotBase implements  Do2AuditCageProcessor, java.io.Serializable {

	Employee employee     = null;
	Rank   rank           = null;
	List<Rank> rankList   = null;
	public Date chkDate1  = null;
	public Date chkDate2  = null;
	Do2 prevPromotDo2     = null;
	StringBuffer buffer   = new StringBuffer();
	
	public static final String ACP_NOT_FOUND    = "AU:ACP/MACPS DO2 For Pay fixation not found.\n";
	public static final String DUPLICATE_ACPFIX = "Du:Duplicate Acpfix Do2. \n";
	public static final String INCORRECT_PROFIX_7CPC = "AU:Option Not Found Exercised For Pay Fixation On Upgradation In 7cpc.\n";
//	public static final String ACP_NOT_REQUIRED = " AU:ACP DO2 erroneously generated earlier rejected now for TA personnel.  Pay fixation done on MACPS date itself.\n";	
	public static final String TAINCR_REQUIRED = "AU:Valid TA Increment DOII With Same FromDate Not Found. \n";	

	@Override
	public boolean isValid(Do2 do2) {
		boolean acpfixValidity = true;
		
		if (doesDuplicateAcpDo2Exist(do2)) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(DUPLICATE_ACPFIX);
	
			do2Repo.save(do2);				
	 		return false;
		}
		
	/*	
		if (do2.getEmployee().getRegularTaFlag()!=null && do2.getEmployee().getRegularTaFlag().equals("T")) {
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(ACP_NOT_REQUIRED);
			do2Repo.save(do2);			
			return false;
		}*/
		
		if (do2.getEmployee().getRegularTaFlag()!=null && do2.getEmployee().getRegularTaFlag().equals("T")) {
			List<Do2> taincrDo2List=do2Repo.findByDescFromDateEqualDesc(do2.getEmployee().getId(), "V", "TAINCR", do2.getFromDate());
			if (taincrDo2List!=null && taincrDo2List.size()>0) {
				;
			} else {
				taincrDo2List=null;
				Calendar oneYearPriorDateCal = new GregorianCalendar();
				oneYearPriorDateCal.setTime(do2.getFromDate());
				Date oneYearPriorDate = do2.getFromDate();
				try {
					oneYearPriorDate = DateManager.getDate((oneYearPriorDateCal.get(Calendar.YEAR)-1), (oneYearPriorDateCal.get(Calendar.MONTH)+1), oneYearPriorDateCal.get(Calendar.DATE));
				} catch (Exception e) {
					Utils.handleException(e);
				}				
				taincrDo2List=do2Repo.findByDescFromDateEqualDesc(do2.getEmployee().getId(), "V", "TAINCR", oneYearPriorDate);
				if (taincrDo2List!=null && taincrDo2List.size()>0) {
					;
				} else {
					do2.setStatus(Do2Manager.PENDING);
					do2.setReason(TAINCR_REQUIRED);
					do2Repo.save(do2);			
					return false;
				}
			}	
		}
		
		if (!isAcpAvailable(do2)) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(ACP_NOT_FOUND);
			acpfixValidity = false;
		}
		
		if (isAcpFixNotOnNextIncrDate(do2)){
			do2.setStatus(Do2Manager.INVALID);
			buffer.append("ACPFIX date incorrect");
			acpfixValidity = false;
		}
		
		if (do2.getFromDate().after(DateManager.getSeventhCpcDate()) && !isValidAcpfixIn7cpc(do2)) {
			acpfixValidity=false;
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(INCORRECT_PROFIX_7CPC); 
		}
		
		if (acpfixValidity) {
			do2.setStatus(Do2Manager.VALID);
		}
		
 		do2.setReason(buffer.toString());
		
 		do2Repo.save(do2);
		return acpfixValidity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		if (newAuditCage.getDo2().getFromDate().before(DateManager.getSixthCpcDate())) {
			get5cpcPay(newAuditCage, baseAuditCage, baseAuditCage.getDo2().getEmployee());
			return newAuditCage;
		}
		
		if (newAuditCage.getDo2().getFromDate().before(DateManager.getSeventhCpcDate())) {
			List<String> descList=Arrays.asList(new String [] {"ACP","MACPS"});
			List<Do2> acpDo2List =do2Repo.findByDescListFromDateLessThanOrderByFromDateDesc(newAuditCage.getEmployee().getId(),Do2Manager.VALID,descList,newAuditCage.getFromDate());
			 			
			AuditCage acpPreviousAuditCage = null;
			AuditCage acpAuditCage =null;
			Do2 acpDo2 = null;
			int gpay = 0;
		 	
			if (acpDo2List.size()>0) {
				acpDo2 = acpDo2List.get(0);
				acpAuditCage = Utils.getAuditCage(acpDo2);
			}
			
			if (acpDo2 != null) {
				if (acpDo2.getColumnA().equals("NKSCALE") || acpDo2.getColumnA().equals("NKGRADE")){
					gpay=2000;
				} else if (acpDo2.getColumnA().equals("HAVSCALE") || acpDo2.getColumnA().equals("HAVGRADE")){
					gpay=2400;
				} else if (acpDo2.getColumnA().equals("NSUBGRADE")){
					gpay=2800;
				} else if (acpDo2.getColumnA().equals("SUBGRADE")){
					gpay=4200;
				} else if (acpDo2.getColumnA().equals("SMAJGRADE")){
					gpay=4600;;
				} else if (acpDo2.getColumnA().equals("HONYGRADE")){
					gpay=4800;;
				} 
				Date fromDate = acpDo2.getFromDate();
				acpPreviousAuditCage = AuditCageHelper.getPreviousDateAuditCage(fromDate, newAuditCage.getDo2());
		 	}
			if (acpAuditCage !=null && acpPreviousAuditCage !=null && acpAuditCage.getBasicPay()==9300){
			
				if ((acpPreviousAuditCage.getXGroupPay() != null &&
					 acpPreviousAuditCage.getXGroupPay() != 0 && 
					 acpPreviousAuditCage.getBasicPay() < 8580)) {
					return newAuditCage;
				}
				if (acpPreviousAuditCage.getBasicPay()<8600 && 
				    acpPreviousAuditCage.getXGroupPay()==0){
					return newAuditCage;
				}
			}
			
			if (acpPreviousAuditCage!=null) {				
				newAuditCage.setBasicPay(baseAuditCage.getBasicPay()
						+ Utils.getIncrmentRoundedToTen(baseAuditCage.getBasicPay() + gpay 
						+ (acpPreviousAuditCage.getXGroupPay() == null ? 0: acpPreviousAuditCage.getXGroupPay()),
										newAuditCage.getFromDate()));
			}	 
		} else {
			int newBasic = newAuditCage.getBasicPay();
 			newAuditCage.setBasicPay(baseAuditCage.getBasicPay());

            List<Do2> acpDo2List = do2Repo.findByDescFromDateLessThanEqualDesc(newAuditCage.getEmployee().getId(), Do2Manager.VALID, Arrays.asList("MACPS"), newAuditCage.getFromDate());
			 
			
			AuditCage acpPreviousAuditCage = null;
		//	AuditCage acpAuditCage =null;
			Do2 acpDo2 = null;
			int gpay = 0;
		 	
			if (acpDo2List.size()>0) {
				acpDo2 = acpDo2List.get(0);
		//		acpAuditCage = Utils.getAuditCage(acpDo2);
			}
			
			if (acpDo2 != null) {
				if (acpDo2.getColumnA().equals("NKSCALE") || acpDo2.getColumnA().equals("NKGRADE")){
					gpay=2000;
				} else if (acpDo2.getColumnA().equals("HAVSCALE") || acpDo2.getColumnA().equals("HAVGRADE")){
					gpay=2400;
				} else if (acpDo2.getColumnA().equals("NSUBGRADE")){
					gpay=2800;
				} else if (acpDo2.getColumnA().equals("SUBGRADE")){
					gpay=4200;
				} else if (acpDo2.getColumnA().equals("SMAJGRADE")){
					gpay=4600;
				} else if (acpDo2.getColumnA().equals("HONYGRADE")){
					gpay=4800;
				}				 
				acpPreviousAuditCage =	AuditCageHelper.getPreviousDateAuditCageIn7cpc(acpDo2.getFromDate(), newAuditCage.getDo2());			
		 	}
			
			if (acpPreviousAuditCage!=null) {
				newAuditCage.setOldBasicPay(acpPreviousAuditCage.getOldBasicPay()
					+ Utils.getIncrmentRoundedToTen(acpPreviousAuditCage.getOldBasicPay() + gpay 
					+ (acpPreviousAuditCage.getXGroupPay() == null ? 0: acpPreviousAuditCage.getXGroupPay()),
									newAuditCage.getFromDate()));
			}
			// Added code for OPTFIX Line 190 to 201 
			
			if (acpDo2!=null && acpDo2.getToDate()!=null && acpDo2.getToDate().equals(newAuditCage.getFromDate())) {
				
				String promotPayLevel = Do2Manager.getPayLevel(gpay);
				newBasic = newAuditCage.getBasicPay();
	 		//	System.out.println("^^^^^^^^^^^^^^^ acpfix ....  "  +gpay +"  "+newBasic);

				List<PayMatrix>paymatrixList = matrixRepo.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(promotPayLevel, newBasic);
				 
				
				if (paymatrixList!=null && paymatrixList.size() > 0) {
					newBasic = paymatrixList.get(0).getBasicPay();
				}
				
				paymatrixList = null;
			
				promotPayLevel = newAuditCage.getPayLevel(); 
				
				paymatrixList = matrixRepo.findByPayLevelAndBasicPayGreaterThanEqualOrderByBasicPayAsc(promotPayLevel,newBasic);
				 
				if (paymatrixList!=null && paymatrixList.size() > 0) { 
					newBasic = paymatrixList.get(0).getBasicPay(); 
				}
				 		
				newAuditCage.setBasicPay(newBasic); 
				 
			} 
		}
	 	return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		if (!do2.getFromDate().before(DateManager.getSeventhCpcDate()) && newAuditCage.getGradePay()==0
				&& newAuditCage.getEmployee().getRegularTaFlag().trim().equals("R".trim())) {
			newAuditCage.getEmployee().setIncrementDate(Utils.getSevenPcNextIncrementDate(do2.getFromDate()));
			empRepo.save(newAuditCage.getEmployee());
		}
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		if (!currentDo2.getFromDate().before(DateManager.getSeventhCpcDate()) 
				&& currentDo2.getEmployee().getRegularTaFlag().trim().equals("R".trim())) {
			currentDo2.getEmployee().setIncrementDate(currentDo2.getFromDate());
			empRepo.save(currentDo2.getEmployee());
		}
		return Do2Manager.SUCCESS;
	}

	public boolean isAcpAvailable(Do2 do2) {
		Employee emp = do2.getEmployee();
		List<String> descList=Arrays.asList(new String[] {"ACP","MACPS"});
		
		List<Do2> acpDo2List = do2Repo.findByDescListFromDateLessThanEqualOrderByFromDateDesc(emp.getId(),Do2Manager.VALID, descList,do2.getFromDate());
			 
		if (acpDo2List == null || acpDo2List.size()==0)
			return false;

		// In view of Estt No.22, the following changes are made -- 28.08.2017
		String colA = do2.getColumnA();
		if (do2.getColumnB()!=null && do2.getColumnB().equals("OPTFIX")) {
			if (do2.getRefDo2No()!=null && (Utils.getReferenceDo2No(acpDo2List.get(0)).equals(do2.getRefDo2No()))) {
				return true;
			} else {
				return false;
			}
		}
		
        if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
        	if (colA.equals("SLGRADE")){
        		colA = "NKGRADE";
        	} else if (colA.equals("ALGRADE")){
        		colA = "HAVGRADE";
        	} else if (colA.equals("DLGRADE")){
        		colA = "NSUBGRADE";
        	}  else if (colA.equals("CLGRADE")){
        		colA = "SUBGRADE";
        	} else if (colA.equals("POLGRADE")){
        		colA = "SMAJGRADE";
        	} 
        }
        //*********************************//
		for (Do2 acpDo2 : acpDo2List) {
			if (((acpDo2.getDescription().equals("ACP")) || (acpDo2.getDescription().equals("MACPS")))
					&& (acpDo2.getColumnA() != null && acpDo2.getColumnA().equals(colA))) {
			 	return true;
			}

		}
		return false;
	}      
	
	public boolean isAcpFixNotOnNextIncrDate(Do2 do2) {
		Employee emp = do2.getEmployee();
		Date acpdate = null;
		Date acpfixdate = null;
		if (do2.getFromDate().before(DateManager.getSixthCpcDate()) || do2.getFromDate().after(DateManager.getSeventhCpcDate())){
			return false;			
		}
		List<String> descList=Arrays.asList(new String[] {"ACP","MACPS"});
		
		List<Do2> acpDo2List = do2Repo.findByDescListFromDateLessThanOrderByFromDateDesc(emp.getId(),Do2Manager.VALID, descList,do2.getFromDate());
		 
		
		if (acpDo2List==null || (acpDo2List!=null && acpDo2List.size()==0))
			return false;

		for (Do2 acpDo2 : acpDo2List) {
			if  (acpDo2.getColumnA() != null && acpDo2.getColumnA().equals(do2.getColumnA())) {
			 	acpdate = acpDo2.getFromDate();
			}
		}
		if (acpdate == null)
			return false;		
				
		Calendar yy = new GregorianCalendar();
		yy.setTimeInMillis(acpdate.getTime());
		int zz = yy.get(Calendar.YEAR);
		Calendar chkDate1 = new GregorianCalendar(zz, Calendar.JANUARY, 01);
		Calendar chkDate2 = new GregorianCalendar(zz, Calendar.JULY, 01);
		Date jan01 = chkDate1.getTime();
		Date jul01 = chkDate2.getTime();
		if (acpdate.after(jan01) && acpdate.before(jul01)){
			acpfixdate = jul01;
		}
		List<Do2> payfixList = do2Repo.findByDescStatusFromDateLessThan(emp.getId(),Do2Manager.VALID,"PAYFIX",acpdate);
				
			 
		if (payfixList != null){
			if (!do2.getFromDate().equals(acpfixdate))
			   return true;
		}
		return false;
	}   
	
	public boolean doesDuplicateAcpDo2Exist(Do2 do2) {
		Employee emp = do2.getEmployee();
		boolean validity = false;
		List<Do2> duplicateDo2List = null;
	 	 	
		duplicateDo2List =do2Repo.findByDescFromDateEqualIdNotEqual(emp.getId(),Do2Manager.VALID,"ACPFIX",do2.getFromDate(),do2.getId());
				
		 
		if (duplicateDo2List!=null && duplicateDo2List.size()>0) {
			validity = true;
		} 
		
		return validity;
	}
	
	public boolean isValidAcpfixIn7cpc(Do2 do2) {
		List<Do2> do2List = do2Repo.findValidAcpFix(do2.getEmployee().getId(),do2.getFromDate(),do2.getColumnA());
				
		 
		if (do2List!=null && do2List.size()>0) {
			return true;
		}
		return false;			
	}
}

