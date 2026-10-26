package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.AppointmentRate;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
//import itsdc.dolphin.utils.CacheManager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.util.Arrays; 
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

 

@RestController
public class Macps extends PromotBase implements Do2AuditCageProcessor, java.io.Serializable {
	Employee employee;
	Employee previousEmployeeStatus = null;
	Rank rank;
	Rank empRank = null;
	List<String> alwToStop = null;
	List<PayScale> currentPayList = null;
	List<PayScale> previousPayList = null;
	List<EmployeeServiceDetail> esdPromotList = null;
	List<Rank> rankList = null;
	Date chkDate1 = null;
	Date chkDate2 = null;
	Date sepDate = null;
	Date tsnkDate = null;
	Date acpnkDate = null;
	Date nkDate = null;
	Date acphavDate = null;
	Date havDate = null;
	Date acpnbsubDate = null;
	Date nbsubDate = null;	
	Date acpsubDate = null;
	Date subDate = null;
	Date acpsubmajDate = null;
	Date submajDate = null;
	Date unwillingDate = null;
	Date willingDate = null;
	String unwillingRank = null;
	boolean unwillingFound=false, willingAfterUnwillingFound=false;

	char empGroup = ' ';
	StringBuffer buffer = new StringBuffer();

	public static final String INVALID_PROMOT_RANK = "AU:Invalid Rank for promotion to MACP Rank.\n";
	public static final String DUPLICATE_IN_ESD = "DU:MACP Promotion Already Adjusted. \n";
	public static final String INVALID_PAY_STAGE = "AU:Pay Stage Wrong For Fixation. \n";
	public static final String LESS_QS = "AU:Qualifying Service is less for ACP. \n";
	public static final String INVALID_DATE = "AU:From date is prior to 01-01-2006. \n";
	public static final String INVALID_DATE_ESTT22 = "AU:From date is prior to 01-01-2009. \n";
	public static final String NOT_YET_ATTESTED = "AU:PBOR has not yet been attested. \n";
	public static final String NO_ENROLMENT_DATE = "AU:Enrolment Date field is blank. \n";
	public static final String ATTESTED_AFTER_MACPS = "AU:Attested  after from date of MACP \n";
	//public static final String HONY_GRADE_PENDING = "AU:Direct NBSUB is entitled upto 2nd MACP only - SAI 1/S/2008 \n";
	static public final String INCR_NOT_DUE_ON_OPTION_DATE  = "AU:Increment Not Due In Pre-Promoted/Pre-Upgraded Grade On Opted Date \n";
	public static final String INVALID_COMBINATION_COLA_COLB = "VL:Invalid Column7A and Column7B Combination. \n";
	public static final String BEFORE_MACPS_DATE_DSC = "VL:FromDate Prior to 01012016 For DSC. \n";
	public static final String BEFORE_MACPS_APPROVAL_DATE_DSC = "VL:DO2 Date Prior to 01042022 For DSC. \n";
	public static final String NO_BATCH_PROCESSING_DSC = "VL:Batch Processing Not For DSC.  Process Individually After Verification of Eligibility. \n";
	public static final String UNWILLING_PROMOTION_EXISTS = "VL:OPTPRO For Unwilling For The Promotion Exists. \n";
	
/*	// MACPS code was amended vide GOI, MOD letter No. 14(1)/99-D(AG) dated 25-07-2018
		// as MACP implementation date changed from 01-09-2018 to 01-01-2006.
	*/
	
	@Override
	public boolean isValid(Do2 do2) {

		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		//rejects the do2 if the pbor is ta 
		if (employee.getRegularTaFlag()!=null && employee.getRegularTaFlag().equals("T") && !employee.getTaAppointmentCategory().equals("R")) {
			do2.setReason("No MACPs for TA  Personnel appointed in OTHER and DEPARTMENTAL CATEGORY");
			do2.setStatus(Do2Manager.INVALID);
			do2Repo.save(do2); 
		    return false;				
		}
		sepDate = null;
		if (employee.getAttestationDate() != null && employee.getInitialMusteringDate() != null){
			if (employee.getAttestationDate().before(employee.getInitialMusteringDate()))
				sepDate = employee.getAttestationDate();
			else 
				sepDate = employee.getInitialMusteringDate();
		} else if (employee.getAttestationDate() == null && employee.getInitialMusteringDate() != null){
				sepDate = employee.getInitialMusteringDate();
		} else if (employee.getAttestationDate() != null && employee.getInitialMusteringDate() == null){
			sepDate = employee.getAttestationDate();	
		}
		tsnkDate = employee.getTsnkDate();
		acpnkDate = employee.getAcpnaikDate();
		nkDate = employee.getNaikDate();
		acphavDate = employee.getAcphavDate();
		havDate = employee.getHavDate();
		acpnbsubDate = employee.getAcpnbsubDate();
		nbsubDate = employee.getNbsubDate();	
		acpsubDate = employee.getAcpsubDate();
		subDate = employee.getSubDate();
		acpsubmajDate = employee.getAcpsubmajDate();
		submajDate = employee.getSubmajDate();
		searchForReversion(do2);
	
		empRank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(), "PAYFIX");
		if (empRank == null) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(Do2Manager.SERVICE_DETAIL_MISSING);
			validity = false;
		}
	
		if (!(isValidStartDate(do2))) {
			if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
				buffer.append(INVALID_DATE_ESTT22);
				validity = false;
			} else {
				buffer.append(INVALID_DATE);
				validity = false;
			}			
		}
	
		if (do2.getId() == newDo2.getId()) {
			if ((isDo2InESD(do2))) {
				buffer.append(DUPLICATE_IN_ESD);
				validity = false;
				do2.setStatus(Do2Manager.INVALID);
			}
		}
	
		if (employee.getEnrolmentDate() == null){
			buffer.append(NO_ENROLMENT_DATE);
		    validity = false;
		}
	    
		if (employee.getAttestationDate() == null) {
			if (employee.getNbsubDate() !=null && employee.getEnrolmentDate().equals(employee.getNbsubDate())){
			    ;
			} else if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
				;
			} else {
				buffer.append(NOT_YET_ATTESTED);
				validity = false;
			}			
		}
		
		if (employee.getAttestationDate() != null && (employee.getAttestationDate().after(do2.getFromDate()))) {
			buffer.append(ATTESTED_AFTER_MACPS);
			validity = false;
		}
		
		// Added on 06.10.2022 - DSC-MACPS
		if (!Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			if (!isValidColumnABCombination(do2)) {
				buffer.append(INVALID_COMBINATION_COLA_COLB);
				validity = false;	
			}			
		} else {
			if (do2.getAo()==null || do2.getAoDate()==null) {
				buffer.append(NO_BATCH_PROCESSING_DSC);
				validity = false;
			}
			if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
				buffer.append(BEFORE_MACPS_DATE_DSC);
				validity = false;
			}
			try {
				if (do2.getDo2Date().before(DateManager.getDate("2022-04-01"))) {
					buffer.append(BEFORE_MACPS_APPROVAL_DATE_DSC);
					validity = false;
				}
			} catch (Exception e) {
				Utils.handleException(e);
			}			
		}
		
		if (do2.getFromDate().after(DateManager.getSeventhCpcDate()) && do2.getToDate()!=null)			
			if (!doesIncrExistsOnOptionDate(do2, employee)) {
				buffer.append(INCR_NOT_DUE_ON_OPTION_DATE);
				validity=false;
				do2.setStatus(Do2Manager.INVALID);
			}
		
	//	Auth : Min. Of Def. Finance F.No.1/13/2011/D (Pay/Services) dated 30.06.2014 - Amendment to SAI 1/S/2008
		/*if (do2.getColumnA().equals("HONYGRADE")){
			buffer.append(HONY_GRADE_PENDING);
			do2.setStatus(Do2Manager.PENDING);
			validity = false;
		}*/
		List<Do2> optproDo2List=do2Repo.findAllByEmployeeIdAndDescriptionAndStatusAndFromDateLessThanEqualOrderByFromDate(employee.getId(), "OPTPRO", Do2Manager.VALID, do2.getFromDate());
		if (optproDo2List!=null && optproDo2List.size()>0) {
			if (!isWilling(do2, optproDo2List)) {
				validity=false;
				buffer.append(UNWILLING_PROMOTION_EXISTS);
			}
		}	
		
		if (!isValidRank(do2)) {
			validity = false;
		}		
		
		if (!validity) {
			if ((do2.getStatus() == null)
					|| (do2.getStatus() != null && !do2.getStatus().equals(Do2Manager.PENDING))) {
				do2.setStatus(Do2Manager.INVALID);
			}
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		
		do2.setReason(buffer.toString());
		 do2Repo.save(do2);
		 
	 
		if (validity && do2.getId() == newDo2.getId()) {	 
			if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())
					|| (do2.getFromDate().after(DateManager.getSeventhCpcDate()) && do2.getToDate()!=null)) {		 
				if (employee.getRegularTaFlag() != null && employee.getRegularTaFlag().equals("R")) {			 
					if (isProfixReq(do2)) {					 
						createProfixDo2(employee,do2);
					}
				}
			}
		}

		return validity;
	}

/*	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		
		if (newAuditCage.getDo2().getColumnA().equals("NKGRADE") && (newAuditCage.getGradePay() == 2400)) {
			return newAuditCage;
		}
		if (newAuditCage.getDo2().getColumnA().equals("HAVGRADE") && (newAuditCage.getGradePay() == 2800)) {
			return newAuditCage;
		}
		if (newAuditCage.getDo2().getColumnA().equals("NSUBGRADE") && (newAuditCage.getGradePay() == 4200)) {
			return newAuditCage;
		}
		if (newAuditCage.getDo2().getColumnA().equals("SUBGRADE") && (newAuditCage.getGradePay() == 4600)) {
			return newAuditCage;
		}
		if (newAuditCage.getDo2().getColumnA().equals("SMAJGRADE") && (newAuditCage.getGradePay() == 4800)) {
			return newAuditCage;
		}
		if (newAuditCage.getDo2().getColumnA().equals("HONYGRADE") && (newAuditCage.getGradePay() == 5400)) {
			return newAuditCage;
		}
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		previousEmployeeStatus = employee;		
		
		payFixMacps(newAuditCage, baseAuditCage);
		if (newAuditCage.getDo2().getColumnA().equals("HONYGRADE")) {
			newAuditCage.setOldBasicPay(10500);
		} else {				
			if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
				getOldBasicPay(newAuditCage, baseAuditCage,	previousEmployeeStatus);
			}
		}

		return newAuditCage;
	}*/

	// above method commented and amended as stated below due to sevencpc
	
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {		
	 
		if (newAuditCage.getDo2().getFromDate().before(DateManager.getSeventhCpcDate()) 
				|| (newAuditCage.getPayLevel()!=null && newAuditCage.getPayLevel().equals("0") 
				&& !newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate()))) {
				
			if (newAuditCage.getDo2().getColumnA().equals("NKGRADE") && (newAuditCage.getGradePay() == 2400)) {
				return newAuditCage;
			}
			if (newAuditCage.getDo2().getColumnA().equals("HAVGRADE") && (newAuditCage.getGradePay() == 2800)) {
				return newAuditCage;
			}
			if (newAuditCage.getDo2().getColumnA().equals("NSUBGRADE") && (newAuditCage.getGradePay() == 4200)) {
				return newAuditCage;
			}
			if (newAuditCage.getDo2().getColumnA().equals("SUBGRADE") && (newAuditCage.getGradePay() == 4600)) {
				return newAuditCage;
			}
			if (newAuditCage.getDo2().getColumnA().equals("SMAJGRADE") && (newAuditCage.getGradePay() == 4800)) {
				return newAuditCage;
			}
			if (newAuditCage.getDo2().getColumnA().equals("HONYGRADE") && (newAuditCage.getGradePay() == 5400)) {
				return newAuditCage;
			}
			employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
			previousEmployeeStatus = employee;		
			
			if (employee.getRegularTaFlag() != null && employee.getRegularTaFlag().equals("T")) {
				get6cpcPayForTa(newAuditCage, baseAuditCage);
			} else {
				payFixMacps(newAuditCage, baseAuditCage);
			}
			
			if (newAuditCage.getPayLevel()!=null && newAuditCage.getPayLevel().equals("0") 
					&& !newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
				newAuditCage.setOldBasicPay(newAuditCage.getBasicPay());
				newAuditCage.setOldGradePay(newAuditCage.getGradePay());						
			} else if (newAuditCage.getDo2().getColumnA().equals("HONYGRADE")) {
				newAuditCage.setOldBasicPay(10500);
			} else {				
				if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
					getOldBasicPay(newAuditCage, baseAuditCage,	previousEmployeeStatus);
				}
			}
		} else {
			 	payFix7Cpc(newAuditCage, baseAuditCage);
				getOld6pcBasicPay(newAuditCage, baseAuditCage);
		}
		
		updateBdtInfo(newAuditCage.getDo2());
		return newAuditCage;
	}
	
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {

		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PROMOT+"");
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		
		// The following lines commented since APPT PAY is admissible on grant of MACPS also
		// Authority : AT/I/3500/Dolphin/Vol.VII dated 14-12-2016.
		
		/*final String[] alwNotAdmissible = { "APTPAY" };
		alwToStop = Arrays.asList(alwNotAdmissible);
		AuditCageHelper.removeAllowances(alwToStop, newAuditCage);*/
		
		
		
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2,
				Do2Manager.ESD_PROMOT);
		reverseUpdateEmployee(refDo2);
		return Do2Manager.SUCCESS;
	}

	// *********************************************************************************
	public boolean isValidStartDate(Do2 do2) {
	//	Date macp_impl_date = DateManager.getSixthCpcAllowanceDate();
		Date macp_impl_date = DateManager.getSixthCpcDate();
		if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
			try {
				macp_impl_date=DateManager.getDate("2009-01-01");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		} 
		return (!do2.getFromDate().before(macp_impl_date));
	}

	public boolean isProfixReq(Do2 do2) {
		Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
		Date yDate = Utils.getLastDayOfCurrentQuarter();
		Calendar bb = new GregorianCalendar();
		bb.setTimeInMillis(yDate.getTime());
		int cc = bb.get(Calendar.YEAR);
		Calendar qtrDate1 = new GregorianCalendar(cc, Calendar.JANUARY, 01);
		Calendar qtrDate2 = new GregorianCalendar(cc, Calendar.JULY, 01);
		Date jancuryr = qtrDate1.getTime();
		Date julcuryr = qtrDate2.getTime();
		Date acpDate = do2.getFromDate();
		Calendar cal = new GregorianCalendar();
		cal.setTimeInMillis(acpDate.getTime());
		int acpYear = cal.get(Calendar.YEAR);
  
		Calendar chkDate1 = new GregorianCalendar(acpYear, Calendar.JANUARY, 01);
		Calendar chkDate2 = new GregorianCalendar(acpYear, Calendar.JULY, 01);
		
		Date jan01 = chkDate1.getTime();
		Date jul01 = chkDate2.getTime();	
		
		if (do2.getFromDate().after(DateManager.getSeventhCpcDate()) && do2.getToDate()!=null) {
			return true;
		}
		
		if ((acpDate.after(jan01)) && (acpDate.before(jul01))) {
			if ((emp.getFsDueDate() != null && emp.getFsDueDate().after(jan01) && emp.getFsDueDate().before(jul01)) ||
				(Utils.isEmpThisMonthFs(emp) && (cc == acpYear) && Utils.getLastDayOfCurrentQuarter().after(jancuryr) &&
				 Utils.getLastDayOfCurrentQuarter().before(julcuryr))) {
				;
			} else {
				return true;
			}			
		}
		return false;
	}

	public boolean isDo2InESD(Do2 do2) {
	 	List<EmployeeServiceDetail> esdList = esdRepo.findDupMacpsInEsd(do2.getEmployee().getId(), do2.getId(), Do2Manager.ESD_PROMOT+"".trim(), do2.getColumnA().trim().toLowerCase());
					
		if (esdList.size() > 0) {
			for (EmployeeServiceDetail esdp : esdList) {
				if (esdp.getDo2().getColumnA() != null && esdp.getDo2().getColumnA().equals(do2.getColumnA())
						&& esdp.getDo2().getId()!=do2.getId())
					return true;
			}
		}
		return false;
	}

	public boolean isValidRank(Do2 do2) {
		
		/*Rank rank = Utils.getRankForPromot(do2.getArmyNo(), do2.getFromDate());*/
		String rankCode = empRank.getRankCode();
		
		if (employee.getEnrolmentDate() != null && employee.getNbsubDate() != null){
			if (employee.getEnrolmentDate().equals(employee.getNbsubDate())){
				if (Integer.parseInt(rankCode) < 50){
					rankCode="50";
				}
			}
		}
		if (employee.getEnrolmentDate() != null && employee.getHavDate() != null){
			if (employee.getEnrolmentDate().equals(employee.getHavDate())){
				if (Integer.parseInt(rankCode) < 40){
					rankCode="40";
				}
			}
		}		
		
		if (!Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) && do2.getFromDate().equals(DateManager.getMacpsStartDate())) {
			if (do2.getColumnA().equals("HAVGRADE")) {
                if (employee.getAcpnaikDate() != null && employee.getAcpnaikDate().equals(DateManager.getMacpsStartDate())) {
                	rankCode="29";
                } else if (employee.getNaikDate() != null && employee.getNaikDate().equals(DateManager.getMacpsStartDate())) {
                	rankCode="30";
                }
			} else if (do2.getColumnA().equals("NSUBGRADE")) { 
				if (employee.getAcphavDate() != null && employee.getAcphavDate().equals(DateManager.getMacpsStartDate())){
						rankCode="39";
				} else if (employee.getHavDate() != null && employee.getHavDate().equals(DateManager.getMacpsStartDate())){
					rankCode="40";
				}
			} else if (do2.getColumnA().equals("SUBGRADE")) {
				if (employee.getAcpnbsubDate() != null && employee.getAcpnbsubDate().equals(DateManager.getMacpsStartDate())){
					rankCode="49";
				} else if (employee.getNbsubDate() != null && employee.getNbsubDate().equals(DateManager.getMacpsStartDate())){
					rankCode="50";
				} 
			} else if (do2.getColumnA().equals("SMAJGRADE")) { 
				if (employee.getAcpsubDate() != null && employee.getAcpsubDate().equals(DateManager.getMacpsStartDate())){
						rankCode="59";
				} else if (employee.getSubDate() != null && employee.getSubDate().equals(DateManager.getMacpsStartDate())){
						rankCode="60";
				} 
			} else if (do2.getColumnA().equals("HONYGRADE")) { 
				if (employee.getAcpsubmajDate() != null && employee.getAcpsubmajDate().equals(DateManager.getMacpsStartDate())){
						rankCode="69";
				} else if (employee.getSubmajDate() != null && employee.getSubmajDate().equals(DateManager.getMacpsStartDate())){
						rankCode="70";
				}
			}	
		} else if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC) && do2.getFromDate().equals(DateManager.getMacpsStartDateForDSC())) {
			if (do2.getColumnA().equals("HAVGRADE")) {
                if (employee.getAcpnaikDate() != null && employee.getAcpnaikDate().equals(DateManager.getMacpsStartDateForDSC())) {
                	rankCode="29";
                } else if (employee.getNaikDate() != null && employee.getNaikDate().equals(DateManager.getMacpsStartDateForDSC())) {
                	rankCode="30";
                }
			} else if (do2.getColumnA().equals("NSUBGRADE")) { 
				if (employee.getAcphavDate() != null && employee.getAcphavDate().equals(DateManager.getMacpsStartDateForDSC())){
						rankCode="39";
				} else if (employee.getHavDate() != null && employee.getHavDate().equals(DateManager.getMacpsStartDateForDSC())){
					rankCode="40";
				}
			} else if (do2.getColumnA().equals("SUBGRADE")) {
				if (employee.getAcpnbsubDate() != null && employee.getAcpnbsubDate().equals(DateManager.getMacpsStartDateForDSC())){
					rankCode="49";
				} else if (employee.getNbsubDate() != null && employee.getNbsubDate().equals(DateManager.getMacpsStartDateForDSC())){
					rankCode="50";
				} 
			} else if (do2.getColumnA().equals("SMAJGRADE")) { 
				if (employee.getAcpsubDate() != null && employee.getAcpsubDate().equals(DateManager.getMacpsStartDateForDSC())){
						rankCode="59";
				} else if (employee.getSubDate() != null && employee.getSubDate().equals(DateManager.getMacpsStartDateForDSC())){
						rankCode="60";
				} 
			} else if (do2.getColumnA().equals("HONYGRADE")) { 
				if (employee.getAcpsubmajDate() != null && employee.getAcpsubmajDate().equals(DateManager.getMacpsStartDateForDSC())){
						rankCode="69";
				} else if (employee.getSubmajDate() != null && employee.getSubmajDate().equals(DateManager.getMacpsStartDateForDSC())){
						rankCode="70";
				}
			} 		
		}
	
		Date rankDate = new Date();
		/*if (Utils.getPao().getPaoCode().equals("94")) {
			buffer.append("Not yet implemented \n") ;
			return false;
		}*/
		if (employee.getEnrolmentDate().equals(employee.getNbsubDate())){
			if (do2.getColumnA().equals("SUBGRADE")|| do2.getColumnA().equals("SMAJGRADE") || do2.getColumnA().equals("HONYGRADE")){
					;
			} else {
				buffer.append("Direct NBSUB entitled to only ACPSUB/ACPSUBMAJ/ACPHONLT. \n") ;
				return false;	
			}
				
		} else if (employee.getEnrolmentDate().equals(employee.getHavDate())){
			if (do2.getColumnA().equals("NSUBGRADE")|| do2.getColumnA().equals("SUBGRADE") || do2.getColumnA().equals("SMAJGRADE")){
					;
			} else {
				buffer.append("Direct Hav entitled to only ACPNBSUB/ACPSUB/ACPSUBMAJ. \n") ;
				return false;	
			}
		} else {
			if (do2.getColumnA().equals("SMAJGRADE")) {
				if ((employee.getMusterJcoFlag()!=null && employee.getMusterJcoFlag() == true && employee.getMusterJcoDate().before(do2.getFromDate())) && 
					(employee.getAcpnaikDate() == null && employee.getNaikDate() == null &&
					 employee.getAcphavDate() == null && employee.getHavDate() == null)) {
					;					
				} else {
					buffer.append("Not entitled to SMAJGRADE. \n") ;
					return false;	
				}
			} else if (do2.getColumnA().equals("SUBGRADE")){
				if ((employee.getMusterJcoFlag()!=null && employee.getMusterJcoFlag() == true && employee.getMusterJcoDate().before(do2.getFromDate()))){					
					if ((employee.getAcpnaikDate() != null || employee.getNaikDate() != null) &&
					     employee.getAcphavDate() == null && employee.getHavDate() == null) {
						;			
					} else if ((employee.getAcpnaikDate() == null && employee.getNaikDate() == null) &&
								employee.getAcphavDate() == null && employee.getHavDate() == null) {
						;	
					}
				} else if (((employee.getMusterHavildarFlag()!=null && employee.getMusterHavildarFlag() == true && employee.getMusterHavildarDate().before(do2.getFromDate())) ||
					    (employee.getSportHavildarFlag()!=null && employee.getSportHavildarFlag() == true && employee.getSportHavildarDate().before(do2.getFromDate())) ||
					    (employee.getGraduateHavildarFlag()!=null && employee.getGraduateHavildarFlag() == true && employee.getGraduateHavildarDate().before(do2.getFromDate()))) && 
					    (employee.getAcpnaikDate() == null && employee.getNaikDate() == null))  {
					;	
				}  else {
					buffer.append("Not entitled to SUBGRADE. \n") ;
					return false;	
				}
			} else if (do2.getColumnA().equals("NKGRADE")|| do2.getColumnA().equals("HAVGRADE") || do2.getColumnA().equals("NSUBGRADE")){
					;
			} else {
				buffer.append("Enrolled as Sepoys are entitled to only ACPNK/ACPHAV/ACPNBSUB. \n") ;
				return false;	
			}
		}
		if (do2.getColumnA().equals("NKGRADE")) {
			if (!rankCode.equals("20")) {
				buffer.append("Not a Sepoy. \n") ;
				return false;
			} else {
				if (!(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceFirstMacps())) {
					buffer.append("Not completed " + getMinService() + " yrs service. \n") ;
					return false;
				} 
			}
		    return true;
		}
 
		if (do2.getColumnA().equals("HAVGRADE")) {
			if (rankCode.equals("20") || rankCode.equals("25")){
				buffer.append("Rank is not ACPNK/NK \n") ;
				return false;
			}
			/*if (rankCode.equals("20")) {
				if (do2.getFromDate().equals(DateManager.getMacpsStartDate()) && 
				    DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), do2.getFromDate()) >= minServiceSecondMacps()) {
					return true;
				} else if (do2.getFromDate().after(DateManager.getMacpsStartDate()) && 
					 DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), do2.getFromDate()) >= minServiceSecondMacps()) {
					 buffer.append("Sepoy - ACP/MACPS to previous grade to be adjusted first \n") ;
					 return false;
				} else {
				   buffer.append("Sepoy - Not completed 16 yrs service. \n") ;
				   return false;
				}
			} */
			if (rankCode.equals("29")) {
				if (acpnkDate == null) {
					buffer.append("AcpNk date not available \n") ;
					return false;
				}
				rankDate = acpnkDate;
				if (DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceSecondMacps()) {					
				    return true;
				}
				if (DateManager.monthsBetweenTwoDates(rankDate, new java.sql.Date(do2.getFromDate().getTime())) >= minService()){
					return true;
				} 
				buffer.append("Not completed " + getMinService() + " yrs service from ACPNK date. \n") ;
				return false;
			}
			if (rankCode.equals("30")) {	
				if (acpnkDate == null && nkDate == null) {
					buffer.append("Neither AcpNk nor Nk date available \n") ;
					return false;
				}
				if (acpnkDate != null) {
					rankDate = acpnkDate;
				} else if (nkDate != null) {
					rankDate = nkDate;
				} 
				if (DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceSecondMacps()){
					return true;
				}
				if ((rankDate.before(do2.getFromDate()) && (DateManager.monthsBetweenTwoDates(rankDate, new java.sql.Date(do2.getFromDate().getTime())) >= minService()))){
					return true;
				} else {
				  buffer.append("Not completed " + getMinService() + " yrs service from ACPNK/NK date. \n") ;
				  return false;
				}
			}
		}
		System.out.println("............. rankcode " +rankCode+"  cola " +do2.getColumnA());
		if (do2.getColumnA().equals("NSUBGRADE")) {
			
			if (rankCode.equals("20") || rankCode.equals("25") || rankCode.equals("29") || rankCode.equals("30")) {
				buffer.append("Rank is not ACPHAV/HAV \n") ;
				return false;
			}
			if (rankCode.equals("39")) {
				if (acphavDate == null) {
					buffer.append("AcpHav date not available \n") ;
					return false;
				}
				rankDate = acphavDate;
				if (DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceThirdMacps()){
					return true;
				}
				if (DateManager.monthsBetweenTwoDates(rankDate, new java.sql.Date(do2.getFromDate().getTime())) >= minService()){
					return true;
				} 
				buffer.append("Not completed " + getMinService() + " yrs service from ACPHAV date. \n") ;
				return false;
			}
			if (rankCode.equals("40")) {
				if (acphavDate == null && havDate == null) {
					buffer.append("Neither AcpHav nor Hav date available \n") ;
					return false;
				}
				if (acphavDate != null) {
					rankDate = acphavDate;
				} else if (havDate != null) {
					rankDate = havDate;
				} 
				if (DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceThirdMacps()){
			        return true;
				}
				if ((employee.getMusterHavildarFlag()!=null && employee.getMusterHavildarFlag() == true 
						&& employee.getMusterHavildarDate()!=null && employee.getMusterHavildarDate().before(do2.getFromDate())) && 
					(employee.getAcpnaikDate() == null && employee.getNaikDate() == null) && rankDate.before(do2.getFromDate()) &&
					(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceSecondMacps()))  {
					 return true;	
				}		
				if ((employee.getSportHavildarFlag()!=null && employee.getSportHavildarFlag() == true 
						&& employee.getSportHavildarDate()!=null && employee.getSportHavildarDate().before(do2.getFromDate())) && 
						(employee.getAcpnaikDate() == null && employee.getNaikDate() == null) && rankDate.before(do2.getFromDate()) &&
						(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceSecondMacps()))  {
						 return true;	
				}
				if ((employee.getGraduateHavildarFlag()!=null && employee.getGraduateHavildarFlag() == true 
						&& employee.getGraduateHavildarDate()!=null && employee.getGraduateHavildarDate().before(do2.getFromDate())) && 
						(employee.getAcpnaikDate() == null && employee.getNaikDate() == null) && rankDate.before(do2.getFromDate()) &&
						(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceSecondMacps()))  {
						 return true;	
				}
				if ((rankDate.before(do2.getFromDate()) && (DateManager.monthsBetweenTwoDates(rankDate, new java.sql.Date(do2.getFromDate().getTime())) >= minService()))){
					return true;
				} else {
				  buffer.append("Not completed " + getMinService() + " yrs service from ACPHAV/HAV date. \n") ;
				  return false;
				}
			}
				
		}
		if (do2.getColumnA().equals("SUBGRADE")) {
			if (rankCode.equals("20") || rankCode.equals("25") || rankCode.equals("29") || rankCode.equals("30") ||
				rankCode.equals("39") || rankCode.equals("40")) {
				buffer.append("Rank is not Nbsub/AcpNbsub.ACP/MACPS to previous grade to be adjusted first \n") ;
				return false;
			}			
			if (rankCode.equals("49")) {
				if (acpnbsubDate == null) {
					buffer.append("Acpnbsub date not available \n") ;
					return false;
				}
				rankDate = acpnbsubDate;				
				if ((employee.getMusterHavildarFlag() == true && employee.getMusterHavildarDate().before(do2.getFromDate())) && 
					(employee.getAcpnaikDate() == null && employee.getNaikDate() == null) && rankDate.before(do2.getFromDate()) &&
					(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceThirdMacps()))  {
					 return true;	
				}
				if ((employee.getMusterJcoFlag() == true && employee.getMusterJcoDate().before(do2.getFromDate()))){
					if (((employee.getAcpnaikDate() != null || employee.getNaikDate() != null) &&
						 employee.getAcphavDate() == null && employee.getHavDate() == null) && rankDate.before(do2.getFromDate()) &&
						(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceThirdMacps())) {
							return true;				
					} else if (((employee.getAcpnaikDate() == null && employee.getNaikDate() == null) &&
								employee.getAcphavDate() == null && employee.getHavDate() == null) && rankDate.before(do2.getFromDate()) &&
								(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceSecondMacps())) {
									return true;			
					}
				}				
				/*if (acpnbsubDate.equals(DateManager.getSixthCpcAllowanceDate()) && 
					do2.getFromDate().equals(DateManager.getSixthCpcAllowanceDate()) && 
					(havDate !=null && havDate.equals(employee.getEnrolmentDate()) && 
					(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), do2.getFromDate()) >= minServiceSecondMacps()))){
					return true;
				}	*/
				
				if (acpnbsubDate.equals(DateManager.getSixthCpcDate()) && 
						do2.getFromDate().equals(DateManager.getSixthCpcDate()) && 
						(havDate !=null && havDate.equals(employee.getEnrolmentDate()) && 
						(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceSecondMacps()))){
						return true;
				}	
				
				if (DateManager.monthsBetweenTwoDates(rankDate, new java.sql.Date(do2.getFromDate().getTime())) >= minService()){
					return true;
				} 
				buffer.append("Not completed required length of service for ACPSUB.  \n") ;
				return false;
			}
			if (rankCode.equals("50")) {
				if (acpnbsubDate == null && nbsubDate == null) {
					buffer.append("Neither AcpNbsub nor Nbsub date available \n") ;
					return false;
				}
				if (acpnbsubDate != null) {
					rankDate = acpnbsubDate;
				} else if (nbsubDate != null) {
					rankDate = nbsubDate;
				} 
				if (((employee.getMusterHavildarFlag() == true && employee.getMusterHavildarDate().before(do2.getFromDate())) ||
						 (employee.getSportHavildarFlag() == true && employee.getSportHavildarDate().before(do2.getFromDate()) || 
						 (employee.getGraduateHavildarFlag() == true && employee.getGraduateHavildarDate().before(do2.getFromDate()))) && 
						(employee.getAcpnaikDate() == null && employee.getNaikDate() == null) && rankDate.before(do2.getFromDate())) &&
						(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceThirdMacps()))  {
						 return true;	
				}			
				if ((employee.getMusterJcoFlag() == true && employee.getMusterJcoDate().before(do2.getFromDate()))){
					if (((employee.getAcpnaikDate() != null || employee.getNaikDate() != null) &&
						 employee.getAcphavDate() == null && employee.getHavDate() == null) && rankDate.before(do2.getFromDate()) &&
						(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceThirdMacps())) {
							return true;				
					} else if (((employee.getAcpnaikDate() == null && employee.getNaikDate() == null) &&
								employee.getAcphavDate() == null && employee.getHavDate() == null) && rankDate.before(do2.getFromDate()) &&
								(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceSecondMacps())) {
									return true;			
					}
				}	
				if ((havDate !=null && havDate.equals(employee.getEnrolmentDate())) &&
					(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceSecondMacps())) {
				    return true;
				}
				if ((nbsubDate !=null && nbsubDate.equals(employee.getEnrolmentDate())) &&
					(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceFirstMacps())) {
					return true;
				}
				if ((rankDate.before(do2.getFromDate()) && (DateManager.monthsBetweenTwoDates(rankDate, new java.sql.Date(do2.getFromDate().getTime())) >= minService()))){
					return true;
				} else {
				  buffer.append("Not completed required length of service for ACPSUB. \n") ;
				  return false;
				}
			}
		}
		if (do2.getColumnA().equals("SMAJGRADE")) {
			if (rankCode.equals("20") || rankCode.equals("25") || rankCode.equals("29") || rankCode.equals("30") || 
				rankCode.equals("39") || rankCode.equals("40") || rankCode.equals("49") || rankCode.equals("50")) {
				buffer.append("Rank is not Acpsub/Sub \n") ;
				return false;
			}			
			if (rankCode.equals("59")) {
				if (acpsubDate == null) {
					buffer.append("Acpsub date not available \n") ;
					return false;
				}
				rankDate = acpsubDate;	
				if ((employee.getMusterJcoFlag() == true && employee.getMusterJcoDate().before(do2.getFromDate())) && 
				    (employee.getAcpnaikDate() == null && employee.getNaikDate() == null &&
					 employee.getAcphavDate() == null && employee.getHavDate() == null) && rankDate.before(do2.getFromDate()) &&
					(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceThirdMacps()))  {
					 return true;	
				}
				
				/*if (acpsubDate.equals(DateManager.getSixthCpcAllowanceDate()) && 
					do2.getFromDate().equals(DateManager.getSixthCpcAllowanceDate()) && 
				    (nbsubDate !=null && nbsubDate.equals(employee.getEnrolmentDate())) &&
					(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), do2.getFromDate()) >= minServiceSecondMacps())) {
						return true;
				}*/
				
				if (acpsubDate.equals(DateManager.getSixthCpcDate()) && 
						do2.getFromDate().equals(DateManager.getSixthCpcDate()) && 
					    (nbsubDate !=null && nbsubDate.equals(employee.getEnrolmentDate())) &&
						(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceSecondMacps())) {
							return true;
				}
				
				if (DateManager.monthsBetweenTwoDates(rankDate, new java.sql.Date(do2.getFromDate().getTime())) >= minService()){
					    return true;
				} 
				buffer.append("Not completed required length of service for AcpSubMaj. \n") ;
				return false;
			}
			if (rankCode.equals("60")) {
				if (acpsubDate == null && subDate == null) {
					buffer.append("Neither Acpsub nor Sub date available \n") ;
					return false;
				}
				if (acpsubDate != null) {
					rankDate = acpsubDate;
				} else if (subDate != null) {
					rankDate = subDate;
				} 
				if ((employee.getMusterJcoFlag() == true && employee.getMusterJcoDate().before(do2.getFromDate())) && 
				    (employee.getAcpnaikDate() == null && employee.getNaikDate() == null &&
					 employee.getAcphavDate() == null && employee.getHavDate() == null) && rankDate.before(do2.getFromDate()) &&
					(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceThirdMacps()))  {
					 return true;	
				}
				if ((havDate !=null && havDate.equals(employee.getEnrolmentDate())) &&
					(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceThirdMacps())) {
				    return true;
				}
				if ((nbsubDate !=null && nbsubDate.equals(employee.getEnrolmentDate())) &&
					(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceSecondMacps())) {
					return true;
				}				
				if ((rankDate.before(do2.getFromDate()) && (DateManager.monthsBetweenTwoDates(rankDate, new java.sql.Date(do2.getFromDate().getTime())) >= minService()))){
					return true;
				} else {
				  buffer.append("Not completed required length of service for AcpSubMaj. \n") ;
				  return false;
				}
			}
				
		}
		if (do2.getColumnA().equals("HONYGRADE")) {
			if (rankCode.equals("20") || rankCode.equals("25") || rankCode.equals("29") || rankCode.equals("30") || 
				rankCode.equals("39") || rankCode.equals("40") || rankCode.equals("49") || rankCode.equals("50") ||
				rankCode.equals("59") || rankCode.equals("60")) {
				buffer.append("Rank is not Acpsubmaj/Submaj \n") ;
				return false;
			}			
			if (rankCode.equals("69")) {
				if (acpsubmajDate == null) {
					buffer.append("Acpsubmaj date not available \n") ;
					return false;
				}
				rankDate = acpsubmajDate;	
				/*if (acpsubmajDate.equals(DateManager.getSixthCpcAllowanceDate()) && 
					do2.getFromDate().equals(DateManager.getSixthCpcAllowanceDate()) && 
					(nbsubDate !=null && nbsubDate.equals(employee.getEnrolmentDate()) && 
					(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), do2.getFromDate()) >= minServiceThirdMacps()))){
					return true;
				}			*/
				
				if (acpsubmajDate.equals(DateManager.getSixthCpcDate()) && 
						do2.getFromDate().equals(DateManager.getSixthCpcDate()) && 
						(nbsubDate !=null && nbsubDate.equals(employee.getEnrolmentDate()) && 
						(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(do2.getFromDate().getTime())) >= minServiceThirdMacps()))){
						return true;
				}				
				
				
				if (DateManager.monthsBetweenTwoDates(rankDate, new java.sql.Date(do2.getFromDate().getTime())) >= minService()){
					    return true;
				} 
				buffer.append("Not completed required length of service for HonyGrade. \n") ;
				return false;
			}
			if (rankCode.equals("70")) {
				if (acpsubmajDate == null && submajDate == null) {
					buffer.append("Neither Acpsubmaj nor Submaj date available \n") ;
					return false;
				}
				if (acpsubmajDate != null) {
					rankDate = acpsubmajDate;
				} else if (submajDate != null) {
					rankDate = submajDate;
				} 
				if ((nbsubDate !=null && nbsubDate.equals(employee.getEnrolmentDate())) &&
					(DateManager.monthsBetweenTwoDates(employee.getEnrolmentDate(), new java.sql.Date(new java.sql.Date(do2.getFromDate().getTime()).getTime())) >= minServiceThirdMacps())) {
				    return true;
				}
				if ((rankDate.before(do2.getFromDate()) && (DateManager.monthsBetweenTwoDates(rankDate, new java.sql.Date(do2.getFromDate().getTime())) >= minService()))){
					return true;
				} else {
				  buffer.append("Not completed required length of service for HonyGrade. \n") ;
				  return false;
				}
			}
				
		}
		String msg = "Rank Code : " + rankCode + " Not a valid rank/not completed required length of service. \n"; 
		buffer.append(msg) ;
		return false;
	}
	

	public void updateEmployee(Do2 do2) {
		String strRankCode = " ";
		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
		if (do2.getColumnA().trim().equals("NKGRADE")) {
			strRankCode = "29";
			employee.setAcpnaikDate(do2.getFromDate());
		}
		else if (do2.getColumnA().trim().equals("HAVGRADE")) {
			strRankCode = "39";
			employee.setAcphavDate(do2.getFromDate());
		}
		else if (do2.getColumnA().trim().equals("NSUBGRADE")) {
			strRankCode = "49";
			employee.setAcpnbsubDate(do2.getFromDate());
		}
		else if (do2.getColumnA().trim().equals("SUBGRADE")) { 
			strRankCode = "59";
			employee.setAcpsubDate(do2.getFromDate());
		}
		else if (do2.getColumnA().trim().equals("SMAJGRADE")) { 
			strRankCode = "69";
			employee.setAcpsubmajDate(do2.getFromDate());
		}
		else if (do2.getColumnA().trim().equals("HONYGRADE")) { 
			strRankCode = "79";
	//		employee.setAcphonyDate(do2.getFromDate());
		}
		
		Rank rank= rankRepo.findByRankCode(strRankCode);
				
		 

		employee.setActingRank(rank);
		employee.setActingRankDate(do2.getFromDate());		
	}

	public void reverseUpdateEmployee(Do2 do2) {
		if (employee == null)
			employee = Utils.getCurrentEmployee(do2.getArmyNo());
		employee.setActingRank(null);
		employee.setActingRankDate(null);
		esdPromotList = esdRepo.findAllByEmployeeIdAndStatusOrderByFromDateDesc(employee.getId(),Do2Manager.ESD_PROMOT+"");
		 
		String pRankCode = null;
		Date pRankDate = new Date();
		if (esdPromotList==null || (esdPromotList!=null && esdPromotList.size()==0)) {
			return;	
		}
         
		pRankDate = esdPromotList.get(0).getDo2().getFromDate();
		if (esdPromotList.get(0).getDo2().getDescription().equals("MSTHAV")){
			pRankCode = "40";	
		} else if (esdPromotList.get(0).getDo2().getDescription().equals("ACP")) {
				if (do2.getColumnA().trim().equals("NKSCALE"))
			        pRankCode = "30";
		        else 
		        	pRankCode = "40";
		} else if (esdPromotList.get(0).getDo2().getColumnA().trim().substring(2).equals("NK")) {
			pRankCode = "30";
		} else if (esdPromotList.get(0).getDo2().getColumnA().trim().substring(2).equals("HAV")) {
			pRankCode = "40";
		} else if (esdPromotList.get(0).getDo2().getColumnA().trim().substring(2).equals("SUB")) {
			pRankCode = "60";
		} else if (esdPromotList.get(0).getDo2().getColumnA().trim().substring(2).equals("SUBMAJ")) {
			pRankCode = "70";
		}
        if (pRankCode != null) {
        	Rank rank = rankRepo.findByRankCode(pRankCode);
        			
        	 

        	employee.setActingRank(rank);
        	employee.setActingRankDate(pRankDate);
        }
		if (do2.getColumnA().equals("NKGRADE"))			
			employee.setAcpnaikDate(null);
		else if (do2.getColumnA().equals("HAVGRADE"))
			employee.setAcphavDate(null);
		else if (do2.getColumnA().equals("NSUBGRADE"))
			employee.setAcpnbsubDate(null);
		else if (do2.getColumnA().equals("SUBGRADE"))
			employee.setAcpsubDate(null);
		else if (do2.getColumnA().equals("SMAJGRADE"))
			employee.setAcpsubmajDate(null);
		/*else if (do2.getColumnA().equals("HONYGRADE"))
			employee.setAcphonyDate(null);*/
	
	}
	@Override 
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
	}
	public void payFixMacps(AuditCage nAuditCage, AuditCage bAuditCage) {
		employee = null;
		employee = Utils.getCurrentEmployee(nAuditCage.getDo2().getArmyNo());
		Rank qRank = null;
		
		if (nAuditCage.getDo2().getColumnA().equals("NKGRADE"))
			qRank=rankRepo.findByRankCode("29");
		else if (nAuditCage.getDo2().getColumnA().equals("HAVGRADE"))
			qRank=rankRepo.findByRankCode("39");
		else if (nAuditCage.getDo2().getColumnA().equals("NSUBGRADE"))
			qRank=rankRepo.findByRankCode("49");
		else if (nAuditCage.getDo2().getColumnA().equals("SUBGRADE"))
			qRank=rankRepo.findByRankCode("59");
		else if (nAuditCage.getDo2().getColumnA().equals("SMAJGRADE"))
			qRank=rankRepo.findByRankCode("69");
		else if (nAuditCage.getDo2().getColumnA().equals("HONYGRADE"))
			qRank=rankRepo.findByRankCode("79");
	 
		String soldierGroup = "";
		GroupCategory groupCategory = Utils.getGroupCategory(nAuditCage.getDo2(),"PREV");
		
		if (groupCategory!=null) {
			soldierGroup = groupCategory.getSoldierGroup();
		}
		
		if (soldierGroup.equals("Z") && !nAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			soldierGroup = "Y";
		}
		if (nAuditCage.getDo2().getColumnA().equals("HONYGRADE")) {
				currentPayList = psRepo.findByRankIdFromDateBetweenOrFromDateGreaterThanEqual(qRank.getId(), nAuditCage.getFromDate());
						
						
	 
		} else {
			currentPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqual(qRank.getId(), soldierGroup,
					nAuditCage.getFromDate());
	 
		}
		//basic pay
		Date yDate = Utils.getLastDayOfCurrentQuarter();
		Calendar bb = new GregorianCalendar();
		bb.setTimeInMillis(yDate.getTime());
		int cc = bb.get(Calendar.YEAR);
		Calendar qtrDate1 = new GregorianCalendar(cc, Calendar.JANUARY, 01);
		Calendar qtrDate2 = new GregorianCalendar(cc, Calendar.JULY, 01);
		Date jancuryr = qtrDate1.getTime();
		Date julcuryr = qtrDate2.getTime();
		Date xDate = nAuditCage.getDo2().getFromDate();
		Calendar yy = new GregorianCalendar();
		yy.setTimeInMillis(xDate.getTime());
		int zz = yy.get(Calendar.YEAR);
		Calendar chkDate1 = new GregorianCalendar(zz, Calendar.JANUARY, 01);
		Calendar chkDate2 = new GregorianCalendar(zz, Calendar.JULY, 01);
		Date jan01 = chkDate1.getTime();
		Date jul01 = chkDate2.getTime();
		
	
	//	if (!(xDate.after(jan01) && xDate.before(jul01))) {
		if ((!(xDate.after(jan01) && xDate.before(jul01))) 
				|| (nAuditCage.getPayLevel()!=null && nAuditCage.getPayLevel().equals("0") 
				&& !nAuditCage.getFromDate().before(DateManager.getSeventhCpcDate()))) {

		 	nAuditCage.setBasicPay(bAuditCage.getBasicPay()	+ (Utils.getIncrmentRoundedToTen(bAuditCage.getBasicPay()
							+ bAuditCage.getGradePay() + (bAuditCage.getXGroupPay() == null ? 0: bAuditCage.getXGroupPay()), nAuditCage.getFromDate())));
		 	
		 	if(currentPayList!=null && currentPayList.size() > 0 ) {
		 		if (nAuditCage.getBasicPay() < currentPayList.get(0).getStartScale()) {
					nAuditCage.setBasicPay(currentPayList.get(0).getStartScale());
				}
		 	}
		 	
		} else {
		   if ((employee.getFsDueDate() != null && employee.getFsDueDate().after(jan01) && employee.getFsDueDate().before(jul01)) ||
			   (Utils.isEmpThisMonthFs(employee) && (cc == zz) && Utils.getLastDayOfCurrentQuarter().after(jancuryr) &&
				Utils.getLastDayOfCurrentQuarter().before(julcuryr))) {
			   nAuditCage.setBasicPay(bAuditCage.getBasicPay()
						+ (Utils.getIncrmentRoundedToTen(bAuditCage.getBasicPay()
								+ bAuditCage.getGradePay()
								+ (bAuditCage.getXGroupPay() == null ? 0: bAuditCage.getXGroupPay()), 
								bAuditCage.getFromDate())));
				if(currentPayList!=null && currentPayList.size() > 0 ) {
					if (nAuditCage.getBasicPay() < currentPayList.get(0).getStartScale()) {
						nAuditCage.setBasicPay(currentPayList.get(0).getStartScale());
					}
				}
			 	
		   } else {
			   if (nAuditCage.getDo2().getColumnA().equals("NSUBGRADE")) {		
				   if ((nAuditCage.getBasicPay() < 8600 && nAuditCage.getXGroupPay()==0) || 
					   (nAuditCage.getBasicPay() < 8580 && nAuditCage.getXGroupPay()==1400)) {
					   if(currentPayList!=null && currentPayList.size() > 0 )
					   nAuditCage.setBasicPay(currentPayList.get(0).getStartScale());
		 		   }
			   }
		   }
		}	 
		// grade pay		
			if(currentPayList!=null && currentPayList.size() > 0 )
			nAuditCage.setGradePay(currentPayList.get(0).getGradePay());
		// ms pay
			if (!(nAuditCage.getDo2().getFromDate().before(DateManager
					.getSixthCpcAllowanceDate()))) {
				if (nAuditCage.getDo2().getColumnA().equals("HONYGRADE")) 
	//			    nAuditCage.setMsPay(6000); -- as orders are not clear
					 nAuditCage.setMsPay(getMsPay(nAuditCage.getDo2()));
				}
		// Transport Allowance
			if (nAuditCage.getTransport() != null && nAuditCage.getTransport() !=0) {
				nAuditCage.setTransport(AuditCageHelper.getTransportRate(nAuditCage,bAuditCage));
			}
	//	System.out.println("aaaaaaaaaaaaaaaaaaaaabbbbbbbbbbbbbbbbbbbbbb  increment date macps  " +employee.getIncrementDate());	
		if (!nAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
			if (nAuditCage.getBasicPay()>bAuditCage.getBasicPay()) {
				employee.setIncrementDate(Utils.getSevenPcNextIncrementDate(nAuditCage.getDo2().getFromDate()));
			}
		}
	//	System.out.println("jjjjjjjjjj nbp bbp fd " +nAuditCage.getBasicPay()+" "+bAuditCage.getBasicPay()+" "+nAuditCage.getFromDate());
	//	System.out.println("bbbbbbbbbbbbbbbbbbbbbbccccccccccccccccccccc increment date macps " + employee.getIncrementDate());
	
	}
	
		
	public void searchForReversion(Do2 do2) {
		 
	Employee emp = do2.getEmployee();
	List<String> descList=Arrays.asList(new String[] {"REVERT","LORANK","RANKS"});
	
	List<EmployeeServiceDetail> esdList = esdRepo.findByStatusDescListFromDateLessThan(emp.getId(),Do2Manager.ESD_PROMOT+"", do2.getFromDate(),descList);
	 
		if (esdList.size()>0) {
			EmployeeServiceDetail esd = null;
			for (int i = 0; i < esdList.size(); i++) {
				esd = esdList.get(i);
				if (esd.getDo2().getDescription().equals("RANKS")) {
					if (sepDate != null && sepDate.before(esd.getDo2().getFromDate())) {
						sepDate = esd.getDo2().getFromDate();	
					}
					if (tsnkDate != null && tsnkDate.before(esd.getDo2().getFromDate())) {
						tsnkDate = null;	
					}
					if (acpnkDate != null && acpnkDate.before(esd.getDo2().getFromDate())) {
						acpnkDate = null;	
					}
					if (nkDate != null && nkDate.before(esd.getDo2().getFromDate())) {
						nkDate = null;	
					}
					if (acphavDate != null && acphavDate.before(esd.getDo2().getFromDate())) {
						acphavDate = null;	
					}
					if (havDate != null && havDate.before(esd.getDo2().getFromDate())) {
						havDate = null;	
					}
				}
				if (esd.getDo2().getDescription().equals("LORANK") || (esd.getDo2().getDescription().equals("REVERT"))) {
					if (esd.getDo2().getColumnA().equals("SEP")) {
						if (sepDate != null && sepDate.before(esd.getDo2().getFromDate())) 
							sepDate = esd.getDo2().getFromDate();
						if (acpnkDate != null && acpnkDate.before(esd.getDo2().getFromDate()))
							acpnkDate = null;
						if (nkDate != null && nkDate.before(esd.getDo2().getFromDate()))
							nkDate = null;
					}
					if (esd.getDo2().getColumnA().equals("NK") || esd.getDo2().getColumnA().equals("PANK")){ 
						if (nkDate != null && nkDate.before(esd.getDo2().getFromDate()))
							nkDate = esd.getDo2().getFromDate();
						if (acphavDate != null && acphavDate.before(esd.getDo2().getFromDate()))
							acphavDate = null;
						if (havDate != null && havDate.before(esd.getDo2().getFromDate()))
							havDate = null;
					}
					if (esd.getDo2().getColumnA().equals("NBSUB")) {
						if (nbsubDate != null && nbsubDate.before(esd.getDo2().getFromDate()))
						    nbsubDate = esd.getDo2().getFromDate();	
						if (acpsubDate != null && acpsubDate.before(esd.getDo2().getFromDate()))
							acpsubDate = null;
						if (subDate != null && subDate.before(esd.getDo2().getFromDate()))
							subDate = null;
					}
					if (esd.getDo2().getColumnA().equals("SUB") || esd.getDo2().getColumnA().equals("PASUB")){
						if (subDate != null && subDate.before(esd.getDo2().getFromDate()))
							subDate = esd.getDo2().getFromDate();
						if (acpsubmajDate != null && acpsubmajDate.before(esd.getDo2().getFromDate()))
							acpsubmajDate = null;
						if (submajDate != null && submajDate.before(esd.getDo2().getFromDate()))
							submajDate = null;
					}
				}
			}
		}
	}
	public double minService(){
		double noOfMonths = 96.0;
		if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
			noOfMonths=120.0;
		}
		if (unwillingFound && willingAfterUnwillingFound) {
			double unwillingMonths=0;
			if (unwillingDate!=null && willingDate!=null && willingDate.after(unwillingDate)) {
				unwillingMonths=DateManager.monthsBetweenTwoDates(unwillingDate, willingDate);
				if (unwillingMonths>0) {
					noOfMonths+=unwillingMonths;
				}
			}			
		}		
		return noOfMonths;
	}
	public double minServiceFirstMacps(){
		double noOfMonths = 96.0;
		if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
			noOfMonths=120.0;
		}
		if (unwillingFound && willingAfterUnwillingFound) {
			double unwillingMonths=0;
			if (unwillingDate!=null && willingDate!=null && willingDate.after(unwillingDate)) {
				unwillingMonths=DateManager.monthsBetweenTwoDates(unwillingDate, willingDate);
				if (unwillingMonths>0) {
					noOfMonths+=unwillingMonths;
				}
			}			
		}		
		return noOfMonths;
	}
	public double minServiceSecondMacps(){
		double noOfMonths = 192.0;
		if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
			noOfMonths=240.0;
		}
		if (unwillingFound && willingAfterUnwillingFound) {
			double unwillingMonths=0;
			if (unwillingDate!=null && willingDate!=null && willingDate.after(unwillingDate)) {
				unwillingMonths=DateManager.monthsBetweenTwoDates(unwillingDate, willingDate);
				if (unwillingMonths>0) {
					noOfMonths+=unwillingMonths;
				}
			}			
		}		
		return noOfMonths;
	}
	public double minServiceThirdMacps(){
		double noOfMonths = 288.0;
		if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
			noOfMonths=360.0;
		}
		if (unwillingFound && willingAfterUnwillingFound) {
			double unwillingMonths=0;
			if (unwillingDate!=null && willingDate!=null && willingDate.after(unwillingDate)) {
				unwillingMonths=DateManager.monthsBetweenTwoDates(unwillingDate, willingDate);
				if (unwillingMonths>0) {
					noOfMonths+=unwillingMonths;
				}
			}			
		}		
		return noOfMonths;
	}
	/*public int getMsPay(){
		int mspay = 2000;
		if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
			mspay=0;
		}
		return mspay;
	}*/
	
	
	public int getMsPay(Do2 do2){
 		int mspay=0;
 		if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
 			mspay=2000;
 		} else {
 			mspay=5200;
 		}
		if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
			mspay=0;
		}
		return mspay;
	}
	
	/*public int getXGroupPay(){
		int xpay = 1400;		
		return xpay;
	}*/
	
	public int getXGroupPay(Do2 do2){
 		int xpay=0;
 		if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
 			xpay=1400;
 		} else {
 			xpay=3600;
 		}
		
		return xpay;
	}
	public int getMinService(){
		int yrs = 8;	
		if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
			yrs = 10;
		}
		return yrs;
	}
	
	public void payFix7Cpc(AuditCage nAuditCage, AuditCage bAuditCage) {
		employee = null;
		employee = Utils.getCurrentEmployee(nAuditCage.getDo2().getArmyNo());
		Rank qRank = null;
		String promotPayLevel = "";
		//Rank currentRank = Utils.getPborRank(nAuditCage.getDo2().getArmyNo(), nAuditCage.getDo2().getFromDate(),"PROMOT");   	
		Rank currentRank = Utils.getPborRank(nAuditCage.getDo2().getArmyNo(), nAuditCage.getDo2().getFromDate(),"PAYFIX"); 
		
		if (nAuditCage.getDo2().getColumnA().equals("NKGRADE"))
			qRank=rankRepo.findByRankCode("29");
		else if (nAuditCage.getDo2().getColumnA().equals("HAVGRADE"))
			qRank=rankRepo.findByRankCode("39");
		else if (nAuditCage.getDo2().getColumnA().equals("NSUBGRADE"))
			qRank=rankRepo.findByRankCode("49");
		else if (nAuditCage.getDo2().getColumnA().equals("SUBGRADE"))
			qRank=rankRepo.findByRankCode("59");
		else if (nAuditCage.getDo2().getColumnA().equals("SMAJGRADE"))
			qRank=rankRepo.findByRankCode("69");
		else if (nAuditCage.getDo2().getColumnA().equals("HONYGRADE"))
			qRank=rankRepo.findByRankCode("79");
		
	 
		
		Integer newBasic = nAuditCage.getBasicPay();
		Integer basicpay = nAuditCage.getBasicPay();
		
		String currentPayLevel = Do2Manager.getPayLevelPromot(currentRank.getRankCode());
		promotPayLevel = Do2Manager.getPayLevelPromot(qRank.getRankCode());
		
		if (nAuditCage.getDo2().getColumnA().equals("HONYGRADE")
				&& employee.getNbsubDate()!=null && employee.getEnrolmentDate().equals(employee.getNbsubDate())) {
			promotPayLevel = "9";
		}
		
	/*	
		System.out.println ( " ARMY NO " +nAuditCage.getDo2().getArmyNo());
		System.out.println( "current rank " +currentRank.getRankCode() );
		System.out.println( " Q rank " +qRank.getRankCode());
		System.out.println (" current pay level "  +currentPayLevel);
		System.out.println( " promot pay level" +promotPayLevel);
	*/	
		if (currentPayLevel!=null && promotPayLevel!=null && currentPayLevel.equals(promotPayLevel) 
				|| promotPayLevel.equals(nAuditCage.getPayLevel())) {
	 		return;
		}
		// Added code for OPTFIX Line 1239 to 1262 
		
		List<Do2> optfixDo2List = do2Repo.findByEmployeeIdAndFromDateAndStatusAndDescription(nAuditCage.getEmployee().getId(),
				nAuditCage.getFromDate(),"V", "OPTFIX");
				
				
	/*			Utils.getEntityManager().createQuery("select d from Do2 d where d.description='OPTFIX' and d.status='V' "
				+ " and d.fromDate=:macpsDate and d.employee=:emp ").setParameter("emp", nAuditCage.getEmployee())
				.setParameter("macpsDate", nAuditCage.getFromDate()).getResultList();*/
		
		if ((optfixDo2List!=null && optfixDo2List.size()>0) 
				|| (nAuditCage.getDo2().getToDate()!=null)) {

		//	List<PayMatrix>paymatrixList1 = matrixRepo.findByPayLevelAndBasicPayGreaterThanEqualOrderByBasicPayAsc(promotPayLevel, newBasic); // commented on 16.05.2025 - Guards RTI
			List<PayMatrix>paymatrixList1 = matrixRepo.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(promotPayLevel, newBasic);
					
		/*			Utils.getEntityManager().createQuery( 
					 " select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND "
					+ " pm.basicPay>=:sevencpcpay order by basicPay asc ")
					.setParameter("paylevel", promotPayLevel)
					.setParameter("sevencpcpay", newBasic)
					.getResultList();*/
			
			if (paymatrixList1.size() > 0) {
				newBasic = paymatrixList1.get(0).getBasicPay();
			}
		
			nAuditCage.setBasicPay(newBasic);
			nAuditCage.setOldGradePay(Do2Manager.getSevenCpcGradepay(promotPayLevel));
			nAuditCage.setPayLevel(promotPayLevel);
		//	Utils.getEntityManager().flush();
			return;
		} 
				
		List<PayMatrix>paymatrixList = matrixRepo.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(currentPayLevel,
				basicpay);
				
		/*		Utils.getEntityManager().createQuery( 
				 " select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND "
				+ "  pm.basicPay>:sevencpcpay order by basicPay asc ")
				.setParameter("paylevel", currentPayLevel)
				.setParameter("sevencpcpay", basicpay)
				.getResultList();*/
		
		if (paymatrixList.size() > 0) {
			newBasic = paymatrixList.get(0).getBasicPay();
		}
		
		List<PayMatrix>paymatrixList1 =  matrixRepo.findByPayLevelAndBasicPayGreaterThanEqualOrderByBasicPayAsc(promotPayLevel,
				newBasic);
				
				
		/*		
				Utils.getEntityManager().createQuery( 
				 " select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND "
				+ " pm.basicPay>=:sevencpcpay order by basicPay asc ")
				.setParameter("paylevel", promotPayLevel)
				.setParameter("sevencpcpay", newBasic)
				.getResultList();*/
		
		if (paymatrixList1.size() > 0) {
			newBasic = paymatrixList1.get(0).getBasicPay();
		}
		
		
//		System.out.println (" basic pay " +newBasic  +" rank " +currentRank.getRankCode() +"EMPLOYEE " +employee.getArmyNo());
		nAuditCage.setBasicPay(newBasic);
		nAuditCage.setOldGradePay(Do2Manager.getSevenCpcGradepay(promotPayLevel));
		nAuditCage.setPayLevel(promotPayLevel);	
		if (nAuditCage.getDo2().getToDate()==null) {
			employee.setIncrementDate(Utils.getSevenPcNextIncrementDate(nAuditCage.getDo2().getFromDate()));	
		}		
//	 	System.out.println("aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa 111111111111111 increment date from Macps  " +employee.getIncrementDate());
	//	Utils.getEntityManager().flush();
	}
	
	public void getOld6pcBasicPay(AuditCage nAuditCage, AuditCage bAuditCage) {
		employee = null;
		employee = Utils.getCurrentEmployee(nAuditCage.getDo2().getArmyNo());
		Rank qRank = null;
		
		if (nAuditCage.getDo2().getColumnA().equals("NKGRADE"))
			qRank=rankRepo.findByRankCode("29");
		else if (nAuditCage.getDo2().getColumnA().equals("HAVGRADE"))
			qRank=rankRepo.findByRankCode("39");
		else if (nAuditCage.getDo2().getColumnA().equals("NSUBGRADE"))
			qRank=rankRepo.findByRankCode("49");
		else if (nAuditCage.getDo2().getColumnA().equals("SUBGRADE"))
			qRank=rankRepo.findByRankCode("59");
		else if (nAuditCage.getDo2().getColumnA().equals("SMAJGRADE"))
			qRank=rankRepo.findByRankCode("69");
		else if (nAuditCage.getDo2().getColumnA().equals("HONYGRADE"))
			qRank=rankRepo.findByRankCode("79");
		
		String soldierGroup = "";
		GroupCategory groupCategory = Utils.getGroupCategory(nAuditCage.getDo2(),"PREV");
		
		if (groupCategory!=null) {
			soldierGroup = groupCategory.getSoldierGroup();
		}
		
		if (soldierGroup.equals("Z") && !nAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			soldierGroup = "Y";
		}
		if (nAuditCage.getDo2().getColumnA().equals("HONYGRADE")) {
				currentPayList = psRepo.findByRankIdFromDateBetweenOrFromDateGreaterThanEqual(qRank.getId(),nAuditCage.getFromDate());
						
						
		/*				Utils.getEntityManager().createQuery(
						"SELECT ps FROM PayScale ps WHERE ps.rank=:rnk "
								+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
								+ " or (:fromDate >= ps.fromDate and ps.toDate is null ))")
				.setParameter("rnk",qRank)
				.setParameter("fromDate", nAuditCage.getFromDate())
				.getResultList();*/
		} else {
			currentPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqual(qRank.getId(),soldierGroup,
													nAuditCage.getFromDate());
					
		/*			Utils.getEntityManager().createQuery(
					"SELECT ps FROM PayScale ps WHERE ps.rank=:rnk "
							+ " AND ps.soldierGroup.soldierGroup=:grp "
							+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
							+ " or (:fromDate >= ps.fromDate and ps.toDate is null ))")
			.setParameter("rnk",qRank)
			.setParameter("grp", soldierGroup)
			.setParameter("fromDate", nAuditCage.getFromDate())
			.getResultList();*/
		}
		//basic pay
		Date yDate = Utils.getLastDayOfCurrentQuarter();
		Calendar bb = new GregorianCalendar();
		bb.setTimeInMillis(yDate.getTime());
		int cc = bb.get(Calendar.YEAR);
		Calendar qtrDate1 = new GregorianCalendar(cc, Calendar.JANUARY, 01);
		Calendar qtrDate2 = new GregorianCalendar(cc, Calendar.JULY, 01);
		Date jancuryr = qtrDate1.getTime();
		Date julcuryr = qtrDate2.getTime();
		Date xDate = nAuditCage.getDo2().getFromDate();
		Calendar yy = new GregorianCalendar();
		yy.setTimeInMillis(xDate.getTime());
		int zz = yy.get(Calendar.YEAR);
		Calendar chkDate1 = new GregorianCalendar(zz, Calendar.JANUARY, 01);
		Calendar chkDate2 = new GregorianCalendar(zz, Calendar.JULY, 01);
		Date jan01 = chkDate1.getTime();
		Date jul01 = chkDate2.getTime();
	
		int xGroupPay = 1400;
		
		if (!(xDate.after(jan01) && xDate.before(jul01))) {
			nAuditCage.setOldBasicPay(bAuditCage.getOldBasicPay()
					+ (Utils.getIncrmentRoundedToTen(bAuditCage.getOldBasicPay()
							+ bAuditCage.getOldGradePay()
							+ xGroupPay, 
							bAuditCage.getFromDate())));
			
			/*if (nAuditCage.getBasicPay() < currentPayList.get(0).getStartScale()) {
				nAuditCage.setBasicPay(currentPayList.get(0).getStartScale());
			}*/
			if (nAuditCage.getOldBasicPay() < currentPayList.get(0).getStartScale()) {
					nAuditCage.setOldBasicPay(currentPayList.get(0).getStartScale());
			}
			
		} else {
		   if ((employee.getFsDueDate() != null && employee.getFsDueDate().after(jan01) && employee.getFsDueDate().before(jul01)) ||
			   (Utils.isEmpThisMonthFs(employee) && (cc == zz) && Utils.getLastDayOfCurrentQuarter().after(jancuryr) &&
				Utils.getLastDayOfCurrentQuarter().before(julcuryr))) {
			   nAuditCage.setOldBasicPay(bAuditCage.getOldBasicPay()
						+ (Utils.getIncrmentRoundedToTen(bAuditCage.getOldBasicPay()
								+ bAuditCage.getOldGradePay()
								+ xGroupPay,  
								bAuditCage.getFromDate())));
				
				if (nAuditCage.getOldBasicPay() < currentPayList.get(0).getStartScale()) {
					nAuditCage.setOldBasicPay(currentPayList.get(0).getStartScale());
				}
		   } else {
			   if (nAuditCage.getDo2().getColumnA().equals("NSUBGRADE")) {		
				   if ((nAuditCage.getOldBasicPay() < 8600 && nAuditCage.getXGroupPay()==0) || 
					   (nAuditCage.getOldBasicPay() < 8580 && nAuditCage.getXGroupPay()==xGroupPay)) {
					   nAuditCage.setOldBasicPay(currentPayList.get(0).getStartScale());
				   }
			   }
		   }
		}	
		
/*		System.out.println("ttttttttttttttttttttttttt  current pay scale  " + currentPayList.get(0).getStartScale()+"  "+currentPayList.get(0).getGradePay()
				+ "  "+nAuditCage.getBasicPay()+"  "+nAuditCage.getOldBasicPay()+"  "+nAuditCage.getGradePay()+"  "+nAuditCage.getOldGradePay());
*/		// grade pay
		if(currentPayList!=null && currentPayList.size() >0)
		nAuditCage.setOldGradePay(currentPayList.get(0).getGradePay());
		// ms pay
			if (!(nAuditCage.getDo2().getFromDate().before(DateManager.getSixthCpcAllowanceDate()))) {
				if (nAuditCage.getDo2().getColumnA().equals("HONYGRADE")) 
	//			    nAuditCage.setMsPay(6000); -- as orders are not clear
					 nAuditCage.setMsPay(getMsPay(nAuditCage.getDo2()));
				else
					nAuditCage.setMsPay(getMsPay(nAuditCage.getDo2()));
			}
			//System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx  nauditcage  " + nAuditCage.getOldBasicPay() +"  "+nAuditCage.getOldGradePay()+"  "+nAuditCage.getBasicPay());		
	}
	
	
	public void get6cpcPayForTa(AuditCage nAuditCage, AuditCage bAuditCage) {
		employee = null;
		employee = Utils.getCurrentEmployee(nAuditCage.getDo2().getArmyNo());
		Rank qRank = null;
				
		if (nAuditCage.getDo2().getColumnA().equals("NKGRADE"))
			qRank=rankRepo.findByRankCode("29");
		else if (nAuditCage.getDo2().getColumnA().equals("HAVGRADE"))
			qRank=rankRepo.findByRankCode("39");
		else if (nAuditCage.getDo2().getColumnA().equals("NSUBGRADE"))
			qRank=rankRepo.findByRankCode("49");
		else if (nAuditCage.getDo2().getColumnA().equals("SUBGRADE"))
			qRank=rankRepo.findByRankCode("59");
		else if (nAuditCage.getDo2().getColumnA().equals("SMAJGRADE"))
			qRank=rankRepo.findByRankCode("69");
		else if (nAuditCage.getDo2().getColumnA().equals("HONYGRADE"))
			qRank=rankRepo.findByRankCode("79");
						
		String soldierGroup = "";
		GroupCategory groupCategory = Utils.getGroupCategory(nAuditCage.getDo2(),"PREV");
		
		if (groupCategory!=null) {
			soldierGroup = groupCategory.getSoldierGroup();
		}
		
		if (soldierGroup.equals("Z") && !nAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			soldierGroup = "Y";
		}
		if (nAuditCage.getDo2().getColumnA().equals("HONYGRADE")) {
				currentPayList =  psRepo.findByRankIdFromDateBetweenOrFromDateGreaterThanEqual(qRank.getId(),
						nAuditCage.getFromDate());
						
						
		/*				Utils.getEntityManager().createQuery(
						"SELECT ps FROM PayScale ps WHERE ps.rank=:rnk "
								+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
								+ " or (:fromDate >= ps.fromDate and ps.toDate is null ))")
				.setParameter("rnk",qRank)
				.setParameter("fromDate", nAuditCage.getFromDate())
				.getResultList();*/
		} else {
			currentPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqual(qRank.getId(),soldierGroup,
					nAuditCage.getFromDate());
					
					
		/*			Utils.getEntityManager().createQuery(
					"SELECT ps FROM PayScale ps WHERE ps.rank=:rnk "
							+ " AND ps.soldierGroup.soldierGroup=:grp "
							+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
							+ " or (:fromDate >= ps.fromDate and ps.toDate is null ))")
			.setParameter("rnk",qRank)
			.setParameter("grp", soldierGroup)
			.setParameter("fromDate", nAuditCage.getFromDate())
			.getResultList();*/
		}
	
		nAuditCage.setBasicPay(bAuditCage.getBasicPay()
			+ (Utils.getIncrmentRoundedToTen(bAuditCage.getBasicPay()
					+ bAuditCage.getGradePay()
					+ (bAuditCage.getXGroupPay() == null ? 0: bAuditCage.getXGroupPay()), 
					bAuditCage.getFromDate())));
	
		if (nAuditCage.getBasicPay() < currentPayList.get(0).getStartScale()) {
			nAuditCage.setBasicPay(currentPayList.get(0).getStartScale());
		}
		
		
		// grade pay		
		nAuditCage.setGradePay(currentPayList.get(0).getGradePay());
		// ms pay
		if (!(nAuditCage.getDo2().getFromDate().before(DateManager.getSixthCpcAllowanceDate()))) {
			if (nAuditCage.getDo2().getColumnA().equals("HONYGRADE")) 
		//			    nAuditCage.setMsPay(6000); -- as orders are not clear
				 nAuditCage.setMsPay(getMsPay(nAuditCage.getDo2()));
		}
		// Transport Allowance
		if (nAuditCage.getTransport() != null && nAuditCage.getTransport() !=0) {
			nAuditCage.setTransport(AuditCageHelper.getTransportRate(nAuditCage,bAuditCage));
		}
	}
	
	
	public boolean doesIncrExistsOnOptionDate(Do2 do2, Employee employee) {
		String incrdesc="INCR";
		if (employee.getRegularTaFlag().equals("T")) {
			incrdesc="TAINCR";
		}
		
		List<Do2> do2List = do2Repo.findByEmployeeIdAndFromDateAndStatusAndDescription(employee.getId(),do2.getToDate(),"V",incrdesc);
				
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.description='INCR' "
		 		+ " and d.employee=:emp and d.status='V' and d.fromDate=:toDate ")
				.setParameter("emp", employee).setParameter("toDate", do2.getToDate())
				.getResultList();*/
		
		if (do2List==null || (do2List!=null && do2List.size()==0)) {
			if (Utils.getPao().getPaoCode().equals(Do2Manager.APS_NAGPUR) && do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
				return true;
			}
		}

		if (do2List==null || (do2List!=null && do2List.size()==0)) {
		   	if (Utils.getPao().getPaoCode().equals(Do2Manager.APS_NAGPUR) && do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
		   		return true;
		   	}
		 }
		if (do2List!=null && do2List.size()>0) {
			return true;
		} else {
			do2List=null;
			do2List = do2Repo.findByDescStatusFromDateLessThanOrderByFromDateDesc(employee.getId(),"V",incrdesc,do2.getToDate());
			 
					
		/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.description='INCR' "
			 		+ " and d.employee=:emp and d.status='V' and d.fromDate<:toDate order by d.fromDate desc ")
					.setParameter("emp", employee).setParameter("toDate", do2.getToDate())
					.getResultList();*/
			if (do2List!=null && do2List.size()>0) {
				Calendar nextIncrDate = new GregorianCalendar();
				nextIncrDate.setTime(do2List.get(0).getFromDate());
				int year = nextIncrDate.get(Calendar.YEAR);
				year++;
				try {
					nextIncrDate.setTime(DateManager.getDate(year, nextIncrDate.get(Calendar.MONTH)+1, nextIncrDate.get(Calendar.DATE)));
				} catch (Exception e) {
					Utils.handleException(e);
				}
 				
				if (do2.getToDate().equals(nextIncrDate.getTime())) {
					return true;
				} else {
					return false;
				}				
			} else {
				if (employee!=null && employee.getIncrementDate()!=null) {
					return (employee.getIncrementDate().equals(do2.getToDate()));
				} else {
					return false;
				}
			}		 
		}	 
	}

	public boolean isValidColumnABCombination(Do2 do2) {
		
		if (do2.getColumnA()==null || (do2.getColumnA()!=null && do2.getColumnA().length()==0)) {
			return false;
		}
		
		if (do2.getColumnB()==null || (do2.getColumnB()!=null && do2.getColumnB().length()==0)) {
			return false;
		}
		
		if (do2.getEmployee().getEnrolmentDate()!=null && do2.getEmployee().getHavDate()!=null 
				&& do2.getEmployee().getEnrolmentDate().equals(do2.getEmployee().getHavDate())) {
			
			if (do2.getColumnB().equals("THIRD")  && do2.getColumnA().equals("HAVGRADE")) {
				return false;
			}
			
			if (do2.getColumnB().equals("SECOND")  && do2.getColumnA().equals("HAVGRADE")) {
				return false;
			}
			
			if (do2.getColumnB().equals("FIRST")  && do2.getColumnA().equals("HAVGRADE")) {
				return false;
			}
			
			if (do2.getColumnB().equals("THIRD")  && do2.getColumnA().equals("NSUBGRADE")) {
				return false;
			}
			
			if (do2.getColumnB().equals("SECOND")  && do2.getColumnA().equals("NSUBGRADE")) {
				return false;
			}
			
			if (do2.getColumnB().equals("THIRD")  && do2.getColumnA().equals("SUBGRADE")) {
				return false;
			}
			
			if (do2.getColumnB().equals("FIRST")  && do2.getColumnA().equals("SUBGRADE")) {
				return false;
			}
			
			if (do2.getColumnB().equals("SECOND")  && do2.getColumnA().equals("SMAJGRADE")) {
				return false;
			}
			
			if (do2.getColumnB().equals("FIRST")  && do2.getColumnA().equals("SMAJGRADE")) {
				return false;
			}
			
			if (do2.getColumnA().equals("HONYGRADE")) {
				return false;
			}			
		}
		
		if (do2.getEmployee().getEnrolmentDate()!=null && do2.getEmployee().getNbsubDate()!=null 
				&& do2.getEmployee().getEnrolmentDate().equals(do2.getEmployee().getNbsubDate())) {
			
			if (do2.getColumnB().equals("THIRD")  && do2.getColumnA().equals("NSUBGRADE")) {
				return false;
			}
			
			if (do2.getColumnB().equals("SECOND")  && do2.getColumnA().equals("NSUBGRADE")) {
				return false;
			}
			
			if (do2.getColumnB().equals("FIRST")  && do2.getColumnA().equals("NSUBGRADE")) {
				return false;
			}
			
			if (do2.getColumnB().equals("THIRD")  && do2.getColumnA().equals("SUBGRADE")) {
				return false;
			}
			
			if (do2.getColumnB().equals("SECOND")  && do2.getColumnA().equals("SUBGRADE")) {
				return false;
			}
			
			if (do2.getColumnB().equals("THIRD")  && do2.getColumnA().equals("SMAJGRADE")) {
				return false;
			}
			
			if (do2.getColumnB().equals("FIRST")  && do2.getColumnA().equals("SMAJGRADE")) {
				return false;
			}			 			
		}
		
		if (do2.getColumnB().equals("FIRST")  && do2.getColumnA().equals("HAVGRADE")) {
			return false;
		}
		
		if (do2.getColumnB().equals("THIRD")  && do2.getColumnA().equals("HAVGRADE")) {
			return false;
		}
		
		if (do2.getColumnB().equals("SECOND")  && do2.getColumnA().equals("NKGRADE")) {
			return false;
		}
		
		if (do2.getColumnB().equals("THIRD")  && do2.getColumnA().equals("NKGRADE")) {
			return false;
		}	
		
		return true;
	}

	public boolean isWilling(Do2 do2, List<Do2> optproDo2List) {
		unwillingFound=false; willingAfterUnwillingFound=false;
		unwillingDate=null; willingDate=null; unwillingRank=null;
		for (Do2 d : optproDo2List) {
			if (d.getColumnA().trim().toUpperCase().equals("UNWILLING")) {
				if (do2.getColumnA().trim().toUpperCase().startsWith(d.getColumnC().trim().toUpperCase())) {
					unwillingFound=true;
					unwillingDate=d.getFromDate();
					unwillingRank=d.getColumnC().trim().toUpperCase();
				} else if (do2.getColumnA().trim().toUpperCase().startsWith("NSUB") 
						&& d.getColumnC().trim().toUpperCase().equals("NBSUB")) {
					unwillingFound=true;
					unwillingDate=d.getFromDate();
					unwillingRank=d.getColumnC().trim().toUpperCase();
				} else if (do2.getColumnA().trim().toUpperCase().startsWith("SMAJ") 
						&& d.getColumnC().trim().toUpperCase().equals("SUBMAJ")) {
					unwillingFound=true;
					unwillingDate=d.getFromDate();
					unwillingRank=d.getColumnC().trim().toUpperCase();
				} else if (do2.getColumnA().trim().toUpperCase().startsWith("NRIS") 
						&& d.getColumnC().trim().toUpperCase().equals("NBRIS")) {
					unwillingFound=true;
					unwillingDate=d.getFromDate();
					unwillingRank=d.getColumnC().trim().toUpperCase();
				} else if (do2.getColumnA().trim().toUpperCase().startsWith("RMAJ") 
						&& d.getColumnC().trim().toUpperCase().equals("RISMAJ")) {
					unwillingFound=true;
					unwillingDate=d.getFromDate();
					unwillingRank=d.getColumnC().trim().toUpperCase();
				}					
			}
			if (d.getColumnA().trim().toUpperCase().equals("WILLING")) {
				if (unwillingFound && d.getFromDate().after(unwillingDate)
						&& unwillingRank!=null && d.getColumnC().equals(unwillingRank)) {
			 		willingDate=d.getFromDate();
					willingAfterUnwillingFound=true;
		 		}
			}
		}		
		if (unwillingFound && !willingAfterUnwillingFound) {
			return false;
		}		
		return true;
	}	 
}
