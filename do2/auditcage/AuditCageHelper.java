package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AllowanceRate;
import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.BhutanSlab;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.LpcRegister;
import itsdc.dolphin.models.MutualInclusive;
import itsdc.dolphin.models.Pao;
import itsdc.dolphin.models.PayCode;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.RhLocation;
import itsdc.dolphin.models.Unit;
import itsdc.dolphin.models.Usr;
import itsdc.dolphin.repositories.AllowanceRateRepository;
import itsdc.dolphin.repositories.ArrearsRepository;
import itsdc.dolphin.repositories.AuditCageAllowanceRepository;
import itsdc.dolphin.repositories.AuditCageRepository;
import itsdc.dolphin.repositories.BhutanSlabRepository;
import itsdc.dolphin.repositories.Do2Repository;
import itsdc.dolphin.repositories.EmployeeServiceDetailRepository;
import itsdc.dolphin.repositories.PayCodeRepository;
import itsdc.dolphin.repositories.PayMatrixRepository;
import itsdc.dolphin.repositories.PayScaleRepository;
import itsdc.dolphin.repositories.RhLocationRepository;
import itsdc.dolphin.repositories.UnitRepository;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;
 

@RestController
@DependsOn("utils")
public class AuditCageHelper {
	  
	@Autowired
	AuditCageAllowanceRepository acaRepo1;
	@Autowired
	Do2Repository do2Repo1;
	@Autowired
	AuditCageRepository acRepo1;
	@Autowired
	EmployeeServiceDetailRepository esdRepo1;
	@Autowired
	UnitRepository unitRepo1;
	@Autowired
	PayCodeRepository pcRepo1;
	@Autowired
	PayMatrixRepository matrixRepo1;
	@Autowired
	ArrearsRepository arrRepo1;
	@Autowired
	PayScaleRepository psRepo1;
	@Autowired
	AllowanceRateRepository alwRateRepo1;
	@Autowired
	BhutanSlabRepository bsRepo1;	
	@Autowired
	RhLocationRepository rhlRepo1;	
	
	public static AuditCageAllowanceRepository acaRepo;
	public static Do2Repository do2Repo;
	public static AuditCageRepository acRepo;
	public static EmployeeServiceDetailRepository esdRepo;
	public static UnitRepository unitRepo;
	public static PayCodeRepository pcRepo;
	public static PayMatrixRepository matrixRepo;
	public static ArrearsRepository arrRepo;
	public static PayScaleRepository psRepo;
	public static AllowanceRateRepository alwRateRepo;
	public static BhutanSlabRepository bsRepo;
	public static RhLocationRepository rhlRepo;	
	
	@PostConstruct
	public void init() {
		acaRepo=acaRepo1;
		do2Repo=do2Repo1;
		acRepo=acRepo1;
		esdRepo=esdRepo1;
		unitRepo=unitRepo1;
		pcRepo=pcRepo1;
		matrixRepo=matrixRepo1;
		arrRepo=arrRepo1;
		psRepo=psRepo1;
		alwRateRepo=alwRateRepo1;
		bsRepo=bsRepo1;
		rhlRepo=rhlRepo1;		
	}
/*
	static public void updateAuditCageAllowance(AuditCage auditCage,
			String payCode, double amount) {
		List<AuditCageAllowance> auditCageAllowanceList = acaRepo.findByAuditCageIdAndPayCodeId(auditCage.getId(),payCode);
		 		 
		if (auditCageAllowanceList.size() > 0) {
			auditCageAllowanceList.get(0).setAmount((int) Math.round(amount));
			auditCageAllowanceList.get(0).setDo2(auditCage.getDo2());
		} else {
			addAuditCageAllowance(auditCage, payCode, amount);
		}

	}
*/
	//03.07.2026
	static public void updateAuditCageAllowance(AuditCage auditCage, String payCode, double amount) {
		List<AuditCageAllowance> auditCageAllowanceList = acaRepo.findByAuditCageIdAndPayCodeId(auditCage.getId(),payCode);
		 		 
		if (auditCageAllowanceList!=null && auditCageAllowanceList.size() > 0) {
			auditCageAllowanceList.get(0).setAmount((int) Math.round(amount));
			auditCageAllowanceList.get(0).setDo2(auditCage.getDo2());
			acaRepo.save(auditCageAllowanceList.get(0));
		} else {
			List<AuditCageAllowance> acaList=null;
			acaList=acaRepo.findByAuditCageIdAndPayCodeAndAmount(auditCage.getId(), payCode, (int)amount);
			if (acaList==null || (acaList!=null && acaList.size()==0)) {
				addAuditCageAllowance(auditCage, payCode, amount);
			}
		}
	}
	
	static private void addAuditCageAllowance(AuditCage auditCage,
			String payCode, double amount) {
		AuditCageAllowance allowanceNewRecord = new AuditCageAllowance();
		allowanceNewRecord.setAuditCage(auditCage);
		allowanceNewRecord.setPayCode(Utils.getPayCode(payCode));
		allowanceNewRecord.setDo2(auditCage.getDo2());
		allowanceNewRecord.setAmount((int) Math.round(amount));
		allowanceNewRecord.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		acaRepo.save(allowanceNewRecord);
		 
	}

	// Used only for Posin,Posout,Pctrg
	static public void stopAllowances(List<String> allowanceList,
			AuditCage newAuditCage, AuditCage baseAuditCage) {
		 
		List<AuditCageAllowance> newAuditCageAllowanceList = acaRepo.findAllByAuditCageId(newAuditCage.getId());
			 
		List<AuditCageAllowance> baseAuditCageAllowanceList = acaRepo.findAllByAuditCageId(baseAuditCage.getId());
				 

		HashMap<String, String> newAllowanceMap = new HashMap<String, String>();

		for (AuditCageAllowance aca : newAuditCageAllowanceList) {
			newAllowanceMap.put(
					aca.getPayCode().getPayCode() + aca.getAmount(), aca
							.getPayCode().getPayCode()
							+ aca.getAmount());
		}

		for (AuditCageAllowance aca : baseAuditCageAllowanceList) {
			if (!allowanceList.contains(aca.getPayCode().getPayCode())
					|| (aca.getPayCode().getPayCode().equals(Do2Manager.APTPAY) && Do2Manager.APPOINTMENT_EXCEPTION_PAOS
							.contains(Utils.getPao().getPaoCode()))) {
				if (newAllowanceMap.get(aca.getPayCode().getPayCode()
						+ aca.getAmount()) != null)

					continue;

				AuditCageAllowance allowanceNewRecord = new AuditCageAllowance();
				allowanceNewRecord.setAuditCage(newAuditCage);
				allowanceNewRecord.setPayCode(aca.getPayCode());
				allowanceNewRecord.setDo2(aca.getDo2());
				allowanceNewRecord.setAmount(aca.getAmount());
				allowanceNewRecord.setCreatedAt(new Timestamp(System.currentTimeMillis()));
				acaRepo.save(allowanceNewRecord);
			}
		}
		 
	}

	/**
	 * 
	 * @param do2FromDate
	 * @param employee
	 * @return Maximum date which is less than or equal to do2FromDate
	 */
	static public Date getAuditCageMaxDate(Employee employee, Do2 do2) {
		Date pendingDate = do2.getFromDate();
		Date maxDate = null;
		List<java.util.Date> maxDateList=null;
		if (Do2Manager.getMusterAttesttDescription().contains(do2.getDescription()) || 
		   (do2.getDescription().equals(Do2Manager.CANCEL) && Do2Manager.getMusterAttesttDescription().contains(do2.getColumnA()))) {
			List<Do2> enmrstDo2List = do2Repo.findByDescFromDateLessThanEqual(employee.getId(),"ENRMST", do2.getFromDate());
				 	 
	
			if (enmrstDo2List.size()>0) {
				pendingDate = enmrstDo2List.get(0).getFromDate();
				maxDateList = acRepo.findMaxAcFromDateEqual(employee.getId(),pendingDate); 
					 
			} else {
				maxDateList = acRepo.findMaxAcFromDateLessThan(employee.getId(),do2.getFromDate());
					 
			} 
			if (maxDateList.size() > 0 && maxDateList.get(0) != null) {
				maxDate = maxDateList.get(0);
			}
			return maxDate;
		}

		if (do2.getDescription().equals(Do2Manager.HOSPITAL)	&& do2.getColumnA()!=null && do2.getColumnA().equals(Do2Manager.DISCH)) {
			List<Do2> refDo2List = Utils.getReferenceDo2List(do2);

			for (Do2 admitDo2 : refDo2List) {
				if (admitDo2.getDescription().equals(Do2Manager.HOSPITAL)
						&& admitDo2.getColumnA()!=null && admitDo2.getColumnA().equals(Do2Manager.ADMITTED)) {
					pendingDate = admitDo2.getFromDate();
					break;
				}
			}

		} else if (do2.getDescription().equals(Do2Manager.ACQUIT)) {
			List<Do2> refDo2List = Utils.getReferenceDo2List(do2);
			for (Do2 refDo2 : refDo2List) {
				if (refDo2.getDescription().equals(Do2Manager.ARREST)) {
					Do2 arrestDo2 = refDo2;
					pendingDate = arrestDo2.getFromDate();
				}
			}
			
		} else if (Utils.getPao().getPaoCode().equals(Do2Manager.APS_NAGPUR) && 
				  (do2.getDescription().equals("REFIXP") && (do2.getColumnC() != null && 
				   (do2.getColumnC().equals("MACPS") || do2.getColumnC().equals("INCR") || do2.getColumnC().equals("PROMOT"))))) {
			Date apsCutOffDate = null;
			try {
				apsCutOffDate = DateManager.getDate("2016-10-01");// cut off date for aps nagpur
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			pendingDate = apsCutOffDate;	
			
		} 	else if (do2.getDescription().equals("SIXCPC") || do2.getDescription().equals("OPTCPC")) {
			if (do2.getDescription().equals("OPTCPC") && !do2.getColumnA().equals("INCR") && do2.getColumnB().equals("SEVENPC")) {
				Rank sevenCpcEffectiveDateRank = Utils.getPborRank(do2.getArmyNo(), DateManager.getPreviousDate(DateManager.getSeventhCpcDate()), "PAYFIX");
				Rank sevenCpcOptionDateRank = Utils.getPborRank(do2.getArmyNo(), DateManager.getNextDate(do2.getFromDate()), "PAYFIX");
				if (sevenCpcEffectiveDateRank!=null && sevenCpcOptionDateRank!=null 
						&& (Integer.parseInt(sevenCpcEffectiveDateRank.getRankCode())>=Integer.parseInt(sevenCpcOptionDateRank.getRankCode()))) {
					;
				} else {
					Do2 payfixDo2 = Utils.getpayfixDo2(do2);
					pendingDate = payfixDo2.getFromDate();
				}
			} else {
				Do2 payfixDo2 = Utils.getpayfixDo2(do2); 
				pendingDate = payfixDo2.getFromDate();	
			}			
		} else if (do2.getDescription().equals(Do2Manager.CANCEL) && (do2.getColumnA().equals("SIXCPC")|| do2.getColumnA().equals("OPTCPC"))) {
			Do2 payfixDo2 = Utils.getpayfixCancelledDo2(do2);
			pendingDate = payfixDo2.getFromDate();
		} else if (Do2Manager.getRegularisationDescriptions().contains(do2.getDescription()) ||
				 do2.getDescription().equals(Do2Manager.CANCEL) && 
				(Do2Manager.getRegularisationDescriptions().contains(do2.getColumnA()))) {
			pendingDate= do2.getFromDate();
			List<Do2> do2List = null;
			if (Do2Manager.getRegularisationDescriptions().contains(do2.getDescription())) {
				do2List = Utils.getReferenceDo2List(do2);
			} else {
				Do2 refDo2 = Utils.getReferenceDo2(do2);
				do2List = Utils.getReferenceDo2List(refDo2);
			}

			for (Do2 refDo2 : do2List) {
				if (Do2Manager.STOP_ALL.contains(refDo2.getDescription())) {
					Do2 absentDo2 = refDo2;
					List<Do2> leaveDo2List = Utils
							.getReferenceDo2List(absentDo2);
					for (Do2 leaveRefDo2 : leaveDo2List) {
						if (Do2Manager.getAllLeaveTypes().contains(
								leaveRefDo2.getDescription())) {
							Do2 leaveDo2 = leaveRefDo2;
							pendingDate = leaveDo2.getFromDate();
						}
					}
				}
			}

		} else if (do2.getDescription().equals("PROMOT") && Do2Manager.getPaidActingDescription().contains(do2.getColumnA())) {
			List<Do2> do2List = Utils.getSubstantiveDo2BeforePaidActing(do2);
			if (do2List.size() > 0) {
				pendingDate = do2List.get(0).getFromDate();
			} else {
				pendingDate = do2.getFromDate();
			}

		} else if (do2.getDescription().equals("TYRJ")) {
			pendingDate = getFieldAllowanceCeaseDateOnTD(do2, employee);
		} else {
			pendingDate = do2.getFromDate();
		}
		if (pendingDate != null) {
			if (pendingDate.equals(employee.getEnrolmentDate())){
				maxDateList =acRepo.findMaxAcDate(employee.getId(), pendingDate);
				 
			} else {
				maxDateList = acRepo.findMaxAcFromDateLessThan(employee.getId(),pendingDate);
						 
			}
			if (maxDateList.size() > 0 && maxDateList.get(0) != null) {
				maxDate = maxDateList.get(0);
				if (maxDate.before(employee.getEnrolmentDate())){
					maxDate=employee.getEnrolmentDate();
				}
			}
		}
		return maxDate;
	}
   
	
	/**
	 * Helps in determining what allowances are admissible with the Do2 and adds
	 * new records in AuditCageAllowance table for all admissible ones including
	 * personal allowances.
	 * 
	 * @param do2
	 * @param newAuditCage
	 *            The reference AuditCage for the new list of allowances (Based
	 *            on the Do2 being processed).
	 * @param baseAuditCage
	 *            Tells you what allowances are currently being drawn.
	 */
	

	static public void allowInclusiveAllowances(Do2 do2, AuditCage newAuditCage, AuditCage baseAuditCage) {
		String payCode= do2.getDescription();
		 
		if (do2.getDescription().equals("RLCA1") || do2.getDescription().equals("RLCA2") || 
			do2.getDescription().equals("RLCA3") || do2.getDescription().equals("RLCA4") || 
			do2.getDescription().equals("TLA1") || do2.getDescription().equals("TLA2") || 
			do2.getDescription().equals("TLA3")){
		    payCode = do2.getColumnB();
		}
	 
		if (do2.getDescription().equals("RISK") && do2.getColumnC()!=null 
			&& (do2.getColumnC().equals("CFAA") || do2.getColumnC().equals("CMFAA") || do2.getColumnC().equals("CHAFAA"))) {
			payCode = do2.getColumnC();
		}
		
		if (!Do2Manager.mutualExclusiveList.contains(payCode)){
			  return;  
		}
		 
		final String [][] mutualInclusiveList = {
				{"CFAA","HAUCH"},    {"CFAA","HAUCL"},    {"CFAA","HAUCS"},    {"CFAA","SIACHN"},
				{"CFAA","SFA"},      {"CHAFAA","HAUCH"},  {"CHAFAA","HAUCL"},  {"CHAFAA","HAUCS"},
				{"CHAFAA","SIACHN"}, {"CHAFAA","SFA"},    {"HAUCH","BHUTAN"},  {"HAUCH","CFAA"},
				{"HAUCH","CHAFAA"},  {"HAUCH","SCCIA"},   {"HAUCL","BHUTAN"},  {"HAUCL","CFAA"},
				{"HAUCL","CHAFAA"},  {"HAUCL","SCCIA"},   {"HAUCS","BHUTAN"},  {"HAUCS","CFAA"},
				{"HAUCS","CHAFAA"},  {"HAUCS","SCCIA"},   {"ISLNDA","ANAL1"},  {"ISLNDA","LAKDWP"},
				{"ISLNDA","ANAL2"},  {"ISLNDA","HAA"},    {"SCCIA","HAUCH"},   {"SCCIA","HAUCL"},
				{"SCCIA","HAUCS"},   {"SCCIA","SFA"},     {"SIACHN","CFAA"},   {"SIACHN","CHAFAA"},
				{"SFA","SCCIA"},     {"SFA","CFAA"},      {"SFA","CHAFAA"},    {"SFA","CMFAA"},
				{"DAA","BHUTAN"},    {"BHUTAN","DAA"},    {"BHUTAN","HAUCL"},  {"BHUTAN","HAUCH"},
				{"BHUTAN","HAUCS"},  {"ANAL1","ISLNDA"},  {"LAKDWP","ISLNDA"}, {"ANAL2","ISLNDA"},
				{"HAA","ISLNDA"},    {"CMFAA","SFA"},     {"ISLNDA","ANAL"},   {"ANAL","ISLNDA"},
		//Following added on 28.11.2022
		{"CFAA","HAUCA"}, {"CMFAA","HAUCA"}, {"CHAFAA","HAUCA"},
		{"HAUCA","CFAA",}, {"HAUCA","CMFAA"}, {"HAUCA","CHAFAA"},
		{"RISK","HAUCL"}, {"RISK","HAUCH"}, {"RISK","HAUCS"}, {"RISK","SIACHN"}, {"RISK","HAUCA"},
		{"HAUCL","RISK"}, {"HAUCH","RISK"}, {"HAUCS","RISK"}, {"SIACHN","RISK"}, {"HAUCA","RISK"},
		{"SFA","RISK"}, {"RISK","SFA"},};						// added on 01.03.2023.
		/*List<MutualInclusive> inclusiveList = CacheManager.getInclusiveAllowances(do2);
		
		HashSet<String> inclusiveSet = new HashSet<String>();
		for (MutualInclusive mi : inclusiveList) {
			System.out.println("Allowance Name : " + mi.getAllowanceRate1().getId());
			if (mi.getAllowanceRate1().getAllowanceName().equals("RLCA1")
					|| mi.getAllowanceRate1().getAllowanceName().equals("RLCA2")){
				inclusiveSet.add(mi.getAllowanceRate1().getSubAllowanceName());
				System.out.println("Allowance Name : " + mi.getAllowanceRate1().getSubAllowanceName());
			} else {
				inclusiveSet.add(mi.getAllowanceRate1().getAllowanceName());
				System.out.println("Allowance Name : " + mi.getAllowanceRate1().getAllowanceName());		
			}			
		}*/
		HashSet<String> inclusiveSet = new HashSet<String>();
		for (int i = 0; i < mutualInclusiveList.length; i++) {
			String allcename = do2.getDescription();
			if (do2.getDescription().equals(Do2Manager.RLCA1)) {
				if (do2.getColumnB().equals(Do2Manager.ANDAMAN_ALLOWANCE1) || 
					do2.getColumnB().equals(Do2Manager.LAKSHADWEEP_ALLOWANCE)) {
					allcename = do2.getColumnB();
				}
			} else if (do2.getDescription().equals(Do2Manager.RLCA2)) {
				if (do2.getColumnB().equals(Do2Manager.ANDAMAN_ALLOWANCE2)) {
					allcename = do2.getColumnB();
				}
			} else if (do2.getDescription().equals(Do2Manager.TLA1)) {
				if (do2.getColumnB().equals(Do2Manager.ANDAMAN_ALLOWANCE)) {
					allcename = do2.getColumnB();
				}
			} 
			if (mutualInclusiveList[i][0].equals(allcename)){
	//			System.out.println("Allowance Name : " + mutualInclusiveList[i][1]); 
				inclusiveSet.add(mutualInclusiveList[i][1]);
			}
		}
		List<AuditCageAllowance> newAuditCageAllowanceList =acaRepo.findAllByAuditCageId(newAuditCage.getId());
				  
		for (AuditCageAllowance aca : newAuditCageAllowanceList) {
			
			if (aca.getPayCode().getPayCode().equals(payCode)
					|| inclusiveSet.contains(aca.getPayCode().getPayCode())
					|| aca.getPayCode().getPersonal() == true
					|| !Do2Manager.mutualExclusiveList.contains(aca.getPayCode().getPayCode()))
				continue; 
					
					acaRepo.delete(aca);			 
		//	newAuditCage.getAuditCageAllowances().remove(aca);
		} 
	}

	
	/**
	 * Method for Do2 Affecting AuditCageAllowances only. Copies All Fields
	 * except fromDate,ToDate,Do2.
	 * 
	 * @param newAuditCage
	 *            AuditCage to which fields are to be copied.
	 * @param baseAuditCage
	 *            AuditCage from which fields are copied.
	 */
	static public void copyAuditCagePayElements(AuditCage newAuditCage, AuditCage baseAuditCage) {
		newAuditCage.setBasicPay(baseAuditCage.getBasicPay());
		newAuditCage.setDp(baseAuditCage.getDp());
		newAuditCage.setGradePay(baseAuditCage.getGradePay());
		newAuditCage.setDa(baseAuditCage.getDa());
		newAuditCage.setClassPay(baseAuditCage.getClassPay());
		newAuditCage.setMsPay(baseAuditCage.getMsPay());
		newAuditCage.setXGroupPay(baseAuditCage.getXGroupPay());
		newAuditCage.setPmha(baseAuditCage.getPmha());
		newAuditCage.setLra(baseAuditCage.getLra());
		newAuditCage.setTransport(baseAuditCage.getTransport());
		newAuditCage.setOldBasicPay(baseAuditCage.getOldBasicPay());
		newAuditCage.setOldClassPay(baseAuditCage.getOldClassPay());
		newAuditCage.setOldGradePay(baseAuditCage.getOldGradePay());
		newAuditCage.setPayLevel(baseAuditCage.getPayLevel());
		
	}

	static public void removeAllowances(List<String> allowanceList,
			AuditCage newAuditCage) {
		 
		List<AuditCageAllowance> auditCageAllowanceList =acaRepo.findAllByAuditCageId(newAuditCage.getId());
		 
		for (AuditCageAllowance aca : auditCageAllowanceList) {
		
			String name = aca.getPayCode().getPayCode();
			if (allowanceList.contains(aca.getPayCode().getPayCode())) {
				acaRepo.delete(aca);				 
				newAuditCage.getAuditCageAllowances().remove(aca);
			}
		}
		 
		
	}
	static public void removeAllowances(AuditCage newAuditCage) {
 
		List<AuditCageAllowance> auditCageAllowanceList = acaRepo.findAllByAuditCageId(newAuditCage.getId());
			 
		for (AuditCageAllowance aca : auditCageAllowanceList) {
			if (aca.getPayCode().getPersonal()==false) {
				acaRepo.delete(aca);
				newAuditCage.getAuditCageAllowances().remove(aca);
			}
		}
		 
		}
	
	static public void removeAllowancesForPosin(AuditCage newAuditCage) {

		
		List<AuditCageAllowance> auditCageAllowanceList =acaRepo.findAllByAuditCageId(newAuditCage.getId());
				 
	
		for (AuditCageAllowance aca : auditCageAllowanceList) {
			if (aca.getPayCode().getPersonal()==false) {
				
				if (aca.getPayCode().getPayCode().equals("AVPAY") 
						|| aca.getPayCode().getPayCode().equals("AETECH")
						|| aca.getPayCode().getPayCode().equals("AIRWCA")
						|| aca.getPayCode().getPayCode().equals("APTPAY")
						|| aca.getPayCode().getPayCode().equals("FLCHCA")) {
					continue;
				}
				acaRepo.delete(aca);				 
				newAuditCage.getAuditCageAllowances().remove(aca);
			}
		}
		 
	}
	static public void updateEmployeeServiceDetail(Do2 do2, String status) {
		List<EmployeeServiceDetail> employeeServiceDetailList = Utils.getEmployeeServiceDetail(do2, status);
	 
		if (employeeServiceDetailList.size()==0) {
			addEmployeeServiceDetail(do2, status);
			return;
		}
		
		if (employeeServiceDetailList.size()==1) {
			
			if (employeeServiceDetailList.get(0).getDo2().getId()==do2.getId()) {
				return;
			}
			
			if (employeeServiceDetailList.get(0).getFromDate().equals(do2.getFromDate())) {
 	  			employeeServiceDetailList.get(0).setToDate(do2.getFromDate());
 	  		 	addEmployeeServiceDetail(do2, status);
 	  		 	return;
			}
			
			if (employeeServiceDetailList.get(0).getFromDate().after(do2.getFromDate())) {
 	 			addEmployeeServiceDetail(do2, status).setToDate(DateManager.getPreviousDate(employeeServiceDetailList.get(0).getFromDate()));
  			} else {
 	  			employeeServiceDetailList.get(0).setToDate(DateManager.getPreviousDate(do2.getFromDate()));
				addEmployeeServiceDetail(do2, status);
			}
			return;
		}

		for (int i=0; i<employeeServiceDetailList.size(); i++) {
 			
 			if (employeeServiceDetailList.get(i).getDo2().getId()==do2.getId()) {
				return;
			}
 			if (employeeServiceDetailList.get(i).getFromDate().after(do2.getFromDate())) {
 				if (i !=0){
 					
 					//amended on 23042012
 					if (employeeServiceDetailList.get(i-1).getFromDate().equals(employeeServiceDetailList.get(i).getFromDate())){
 						employeeServiceDetailList.get(i-1).setToDate(employeeServiceDetailList.get(i).getFromDate());
 					} else {
 						employeeServiceDetailList.get(i-1).setToDate(DateManager.getPreviousDate(do2.getFromDate()));
 					}

 				}
 	  			
 	 			addEmployeeServiceDetail(do2, status).setToDate(DateManager.getPreviousDate(employeeServiceDetailList.get(i).getFromDate()));
 	 			return;
 			}
 			if (i==employeeServiceDetailList.size()-1 && employeeServiceDetailList.get(i).getToDate()==null) {
 				//amended on 23042012
 				if (do2.getFromDate().equals(employeeServiceDetailList.get(i).getFromDate())){
 					employeeServiceDetailList.get(i).setToDate(do2.getFromDate());	
 				} else {
 					employeeServiceDetailList.get(i).setToDate(DateManager.getPreviousDate(do2.getFromDate()));
 				}
 				addEmployeeServiceDetail(do2, status);
 				return;
 			}   
 		}
	}

	static public EmployeeServiceDetail addEmployeeServiceDetail(Do2 do2,
			String status) {
		
		EmployeeServiceDetail empServiceDetail = new EmployeeServiceDetail();
		do2Repo.save(do2);
		 
		empServiceDetail.setDo2(do2);
		if (Do2Manager.getValidNextDayDo2s().contains(do2.getDescription())) {
			empServiceDetail.setFromDate(DateManager.getNextDate(do2
					.getFromDate()));
		} else {
			empServiceDetail.setFromDate(do2.getFromDate());
		}

		empServiceDetail.setStatus(status);
		empServiceDetail.setEmployee(Utils.getCurrentEmployee(do2.getArmyNo()));
		empServiceDetail.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		empServiceDetail.setUsr(Utils.getLoggedInUsr());
		esdRepo.save(empServiceDetail); 

		return empServiceDetail;
	}

	static public void reverseUpdateEmployeeServiceDetail(Do2 refDo2,
			char status) {
		List<EmployeeServiceDetail> employeeServiceDetailList =esdRepo.findAllByEmployeeIdAndStatusOrderByFromDateAscToDateAsc(refDo2.getEmployee().getId(),status+"");
				 
		EmployeeServiceDetail base = null;
		EmployeeServiceDetail follow = null;
		EmployeeServiceDetail current = null;
		int listSize = employeeServiceDetailList.size();
		if(listSize == 1){
			current=employeeServiceDetailList.get(0);
			esdRepo.delete(current);
		 	return;
		}
		for (int i = 0; i < listSize; i++) {
			current = employeeServiceDetailList.get(i);
			if (current.getDo2().getId() == refDo2.getId()) {
				esdRepo.delete(current);
				 if (i == 0){
					 esdRepo.delete(current);
					 
					return;
				}					
				base = employeeServiceDetailList.get(i - 1);
				if (i == (listSize - 1)) {
					base.setToDate(null);
					return;
				} else {
					if (i < listSize-1) {
						follow = employeeServiceDetailList.get(i + 1);
						if(base.getFromDate().equals(follow.getFromDate())){
							base.setToDate(follow.getFromDate());
						}else{
						base.setToDate(DateManager.getPreviousDate(follow
								.getFromDate()));
						}
						return;
					}
					
				}
			}
		}
		 
	}

	public static void insertDaDo2s(Employee employee, Do2 do2) {
		//condition added for TA personnel -to generate da do2 on muster
				//from the cut off date instead of enrolement date
		List<Allowance> allowanceList=null;
		if(employee.getRegularTaFlag() !=null && employee.getRegularTaFlag().equals("T")){
			Date fromDate= employee.getEnrolmentDate();
			if(isBaseAfterEnrollDate(do2)){
				fromDate= DateManager.getNextDate(DateManager.getTaCutOffDate());
			}else{
				fromDate = employee.getEnrolmentDate();
			}
			allowanceList = Utils.getApplicableAllowanceRates(
					Do2Manager.DA, fromDate, Utils
							.getLastDayOfCurrentQuarter());
		}else{
			 allowanceList = Utils.getApplicableAllowanceRates(
					Do2Manager.DA, employee.getEnrolmentDate(), Utils
							.getLastDayOfCurrentQuarter());	
		}

		String qe = Utils.getCurrentQuarter();
		 

		for (Allowance allowance : allowanceList) {
			List<Do2> duplicateDo2List =do2Repo.findByDescFromDateEqualValidAndPending(employee.getId(), Do2Manager.VALID,Do2Manager.PENDING,Do2Manager.DA, allowance.getFromDate());
					 

			if (duplicateDo2List == null || duplicateDo2List.size() == 0){
				Do2 daDo2 = new Do2();
	
				daDo2.setEmployee(employee);
				daDo2.setArmyNo(employee.getArmyNo());
				daDo2.setCheckDigit(employee.getCheckDigit());
				daDo2.setUnitCode(employee.getUnit().getUnitCode());
				daDo2.setUnit(employee.getUnit());
				daDo2.setDo2Group("IA");
				daDo2.setDo2Type("5");
				daDo2.setDo2No("5555");
				daDo2.setDo2ItemNo("555");
				daDo2.setDo2Date(DateManager.getToday());
				daDo2.setDescription("DA");
				daDo2.setFromDate(allowance.getFromDate());
				daDo2.setBatchNo(do2.getBatchNo());
				daDo2.setColumnC(do2.getDescription());
				daDo2.setStatus(Do2Manager.PENDING);
				daDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
				daDo2.setAuditCage(true);
				daDo2.setQe(qe);
				daDo2.setAcceptedQe(qe);
				daDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
				do2Repo.save(daDo2);
				 
			}
		}
		
	}

	public static void insertLraDo2s(Employee employee, Do2 do2) {
		if(employee.getRegularTaFlag().equals("T")){
			if(employee.getTaAppointmentCategory().equals("D") 
		       || employee.getTaAppointmentCategory().equals("O")){
				return;
			}
		}

		List<Allowance> allowanceList = Utils.getApplicableAllowanceRates(
				Do2Manager.NRA, do2.getFromDate(), Utils.getLastDayOfCurrentQuarter());
		String qe = Utils.getCurrentQuarter();
		 
		for (Allowance allowance : allowanceList) {
			List<Do2> duplicateDo2List =do2Repo.findByDescFromDateEqualValidAndPending(employee.getId(), Do2Manager.VALID,Do2Manager.PENDING,Do2Manager.LRA, allowance.getFromDate());
				 
			if (duplicateDo2List == null || duplicateDo2List.size() == 0){
				Do2 lraDo2 = new Do2();
	
				lraDo2.setEmployee(employee);
				lraDo2.setArmyNo(employee.getArmyNo());
				lraDo2.setCheckDigit(employee.getCheckDigit());
				lraDo2.setUnitCode(employee.getUnit().getUnitCode());
				lraDo2.setUnit(employee.getUnit());
				lraDo2.setDo2Group("IA");
				lraDo2.setDo2Type("4");
				lraDo2.setDo2No("4444");
				lraDo2.setDo2ItemNo("444");
				lraDo2.setDo2Date(DateManager.getToday());
				lraDo2.setDescription("LRA");
				lraDo2.setFromDate(allowance.getFromDate());
				lraDo2.setBatchNo(do2.getBatchNo());
				lraDo2.setColumnC(do2.getDescription());
				lraDo2.setColumnA(Do2Manager.GRANTED);
				lraDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
				lraDo2.setAuditCage(true);
				lraDo2.setStatus(Do2Manager.PENDING);
				lraDo2.setQe(qe);
				lraDo2.setAcceptedQe(qe);
				lraDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
				do2Repo.save(lraDo2);
			}
		}
	}

	public static void insertAllowanceRevisionDo2(Employee employee,
			Date fromDate, Do2 do2) {

		Do2 alwrevDo2 = new Do2();
		String qe = Utils.getCurrentQuarter();
		alwrevDo2.setEmployee(employee);
		alwrevDo2.setArmyNo(employee.getArmyNo());
		alwrevDo2.setCheckDigit(employee.getCheckDigit());
		alwrevDo2.setUnitCode(employee.getUnit().getUnitCode());
		alwrevDo2.setUnit(employee.getUnit());
		alwrevDo2.setDo2Group("IA");
		alwrevDo2.setDo2Type("7");
		alwrevDo2.setDo2No("7777");
		alwrevDo2.setDo2ItemNo("777");
		alwrevDo2.setDo2Date(DateManager.getToday());
		alwrevDo2.setDescription("ALWREV");
		alwrevDo2.setFromDate(fromDate);
		alwrevDo2.setBatchNo(do2.getBatchNo());
		alwrevDo2.setColumnC(do2.getDescription());
		alwrevDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		alwrevDo2.setStatus(Do2Manager.PENDING);
		alwrevDo2.setAuditCage(true);
		alwrevDo2.setQe(qe);
		alwrevDo2.setAcceptedQe(qe);
		alwrevDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		do2Repo.save(alwrevDo2);
	}

	public static void stopAllAllowances(AuditCage newAuditCage) {
		 
		List<AuditCageAllowance> newAuditCageAllowanceList = acaRepo.findAllByAuditCageId(newAuditCage.getId());
			 	 
		for (AuditCageAllowance aca : newAuditCageAllowanceList) {		
			if (aca.getPayCode().getPayCode().equals(Do2Manager.APTPAY)
					|| aca.getPayCode().getPersonal() == false) {
				acaRepo.delete(aca);				 	
				newAuditCage.getAuditCageAllowances().remove(aca);
			}

		}
		 
	}

	public static void insertPayfixDo2(Employee employee, Date fromDate) {
		Do2 payfixDo2 = new Do2();
		payfixDo2.setEmployee(employee);
		payfixDo2.setArmyNo(employee.getArmyNo());
		payfixDo2.setCheckDigit(employee.getCheckDigit());
		payfixDo2.setUnitCode(employee.getUnit().getUnitCode());
		payfixDo2.setUnit(employee.getUnit());
		payfixDo2.setDo2Group("IA");
		payfixDo2.setDo2Type("7");
		payfixDo2.setDo2No("1234");
		payfixDo2.setDo2ItemNo("123");
		payfixDo2.setDo2Date(DateManager.getToday());
		payfixDo2.setDescription("PAYFIX");
		payfixDo2.setFromDate(fromDate);
		payfixDo2.setColumnA(null);
		payfixDo2.setColumnB(null);
		payfixDo2.setColumnC(null);
		payfixDo2.setRefDo2No(null);
		payfixDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		payfixDo2.setStatus(Do2Manager.PENDING);
		payfixDo2.setAuditCage(true);
		payfixDo2.setQe(Utils.getCurrentQuarter());
		payfixDo2.setAcceptedQe(Utils.getCurrentQuarter());
		payfixDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		do2Repo.save(payfixDo2);
	}

	
	public static void insertFix7pcDo2(Employee employee, Date fromDate) {
		Do2 payfixDo2 = new Do2();
		payfixDo2.setEmployee(employee);
		payfixDo2.setArmyNo(employee.getArmyNo());
		payfixDo2.setCheckDigit(employee.getCheckDigit());
		payfixDo2.setUnitCode(employee.getUnit().getUnitCode());
		payfixDo2.setUnit(employee.getUnit());
		payfixDo2.setDo2Group("IA");
		payfixDo2.setDo2Type("7");
		payfixDo2.setDo2No("1234");
		payfixDo2.setDo2ItemNo("123");
		payfixDo2.setDo2Date(DateManager.getToday());
		payfixDo2.setDescription("FIX7PC");
		payfixDo2.setFromDate(fromDate);
		payfixDo2.setColumnA(null);
		payfixDo2.setColumnB(null);
		payfixDo2.setColumnC(null);
		payfixDo2.setRefDo2No(null);
		payfixDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		payfixDo2.setStatus(Do2Manager.PENDING);
		payfixDo2.setAuditCage(true);
		payfixDo2.setQe(Utils.getCurrentQuarter());
		payfixDo2.setAcceptedQe(Utils.getCurrentQuarter());
		payfixDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		do2Repo.save(payfixDo2);
	}

	static List<Integer> rankBasedAllowance = Utils.getPayCodeIds(Arrays
			.asList(new String[] { "FORLAG", "FLCHCA", "FLYPAY", "HAUCL",
					"HAUCH", "AVPAY", "CHAFAA", "COMNDO", "DEPSAG", "DEPSRG",
					"SFASFF", "SFA", "SFACOM" }));
	static List<Integer> basicRateBasedAllowance = Utils.getPayCodeIds(Arrays
			.asList(new String[] { "CCA", "CCB1", "CCB2", "HCA", "CCA1" }));

	// pay_code FAA to be confirmed - add to the paycode table this paycode


	/*
	 * static List<Integer> basicPcBasedAllowance = Utils.getPayCodeIds(Arrays
	 * .asList(new String[] { "DEPA", "NSGA", "HAA", "SSA", "ADPOST", "EXTWK"}));
	 */	
	static List<Integer> basicPcBasedAllowance = Utils.getPayCodeIds(Arrays
			.asList(new String[] { "DEPA", "NSGA", "HAA", "SSA", "ADPOST", "EXTWK", "TRGALL"})); 

	static List<Integer> basicAreaBasedAllowance = Utils
			.getPayCodeIds(Arrays.asList(new String[] { 
					"HIMA1", "ARUNA1","UP","LAKDWP","MIZO1","JK1","SIKKIM", 
					"ANAL1", "NAGLND",	"HIMA2","ARUNA2","MIZO2","JK2", 
					"TRIPU1", "ANAL2","MANI","MIZO3","JK3","TRIPU2",
					"HIMA3", "ASMEGH","HIMA4", "UK", "ARUNA", "ANAL" }));

	static List<Integer> rankAreaBasedAllowance = Utils.getPayCodeIds(Arrays
			.asList(new String[] { "SCCIA" , "SPIC"}));


	static List<Integer> dateBasedAllowance = Utils.getPayCodeIds(Arrays
			.asList(new String[] { "AETECH", "PARA", "PARAR", "ADPAY", "HAUCS", "SIACHN"}));

	static List<Integer> rankCityBasedAllowance = Utils.getPayCodeIds(Arrays
			.asList(new String[] { "CILQ" }));

	static List<Integer> rankPaoBasedAllowance = Utils.getPayCodeIds(Arrays
			.asList(new String[] { "CFAA", "CMFAA" }));
	
	//added on 21062012 for revision islnda
	static List<Integer> payElementsRelatedRevision = Utils.getPayCodeIds(Arrays
			.asList(new String[] { "ISLNDA" }));
	
	static List<Integer> hrdallRevision = Utils.getPayCodeIds(Arrays
			.asList(new String[] { "HRDALL" }));
	
//	static List<Integer> cityBasedAllowance = Utils.getPayCodeIds(Arrays.asList(new String[] { "HRA", "HRAFAM", "FAA" }));
	
	static List<Integer> cityBasedAllowance = Utils.getPayCodeIds(Arrays.asList(new String[] { "HRA", "HRAFAM", "FAA" , "HRALF", "FAMLF"}));
	static List<Integer> bhutanRateRevision = Utils.getPayCodeIds(Arrays.asList(new String[] { "BHUTAN" }));

	static List<Integer> rumcigRevision = Utils.getPayCodeIds(Arrays.asList(new String[] { "RUMCIG" }));

	static List<Integer> spincRevision = Utils.getPayCodeIds(Arrays.asList(new String[] { "SPINC" }));
	
	static List<Integer> gradePayBasedAllowance = Utils.getPayCodeIds(Arrays
			.asList(new String[] { "PROALL" }));
	
	static List<Integer> fieldAreaBasedAllowance = Utils.getPayCodeIds(Arrays.asList(new String[] { "RISK", "HAUCA" }));
	
	static public int getRevisedAllowanceRate(AuditCageAllowance atcAlw, AuditCage newAuditCage) {
 		//System.out.println(" new auditcage111 " +newAuditCage.getEmployee().getArmyNo());
		Rank rank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(),newAuditCage.getFromDate(),"ALLCE");
		Employee employee = newAuditCage.getEmployee();

		int chkAmt = atcAlw.getAmount();
		
		PayCode pc = pcRepo.findById(atcAlw.getPayCode().getId()).get();
				
				 
		List<Allowance> rateList = null;
		
 		if (!newAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
				 if (Do2Manager.getAbolishedDescriptionsSevencpc().contains(atcAlw.getPayCode().getPayCode())) {
					   final String[] alwUnderProcess = {atcAlw.getPayCode().getPayCode() };
						final List<String> alwToStop = Arrays.asList(alwUnderProcess);
						removeAllowances(alwToStop, newAuditCage);
				 }
			}	 
 		
		/*if ((Utils.isEmpThisMonthFs(newAuditCage.getEmployee()) || (newAuditCage.getDo2().getEmployee().getStatusCode().getId() > 4))) {
			//System.out.println(" new auditcage 222 " +newAuditCage.getEmployee().getArmyNo());
			if (!newAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			//	System.out.println(" new auditcage 333 " +newAuditCage.getEmployee().getArmyNo());
				if (atcAlw.getPayCode().getPayCode().equals("STRINC") ||
						atcAlw.getPayCode().getPayCode().equals("CILQ") ||
								atcAlw.getPayCode().getPayCode().equals("FAA")) {
					chkAmt=0;
				}
			}
		}*/
		
		if (rankBasedAllowance.contains(atcAlw.getPayCode().getId())) {
			
			if (atcAlw.getPayCode().getPayCode().equals("CHAFAA") 
					&& !newAuditCage.getFromDate().before(DateManager.getChafaaStopDate())) {
				chkAmt = 0;
				
			} else if (atcAlw.getPayCode().getPayCode().equals("SFA")) {
			     chkAmt=getSfaRate(chkAmt, newAuditCage.getFromDate(), rank);
			} else if (atcAlw.getPayCode().getPayCode().equals("FLCHCA")) {
				final String[] alwUnderProcess = {"FLCHCA" };
				final List<String> alwToStop = Arrays.asList(alwUnderProcess);
				 if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())){
					 if(Integer.parseInt(rank.getRankCode()) >= 60) {
							removeAllowances(alwToStop, newAuditCage);
					 } else {
						 rateList = Utils.getApplicableAllowanceRates(pc.getPayCode(), 
								 newAuditCage.getFromDate(),newAuditCage.getFromDate(), rank);
					 }
				 } else {
					removeAllowances(alwToStop, newAuditCage);
				 }
			} else {
				rateList = Utils.getApplicableAllowanceRates(
							pc.getPayCode(), newAuditCage.getFromDate(),
							newAuditCage.getFromDate(), rank);
			
			
				chkAmt = (int) (rateList.size() > 0 ? rateList.get(0)
							.getRateAmount() : chkAmt);
			}
		}
		// to be clarified - basicpay - HCA is still continuing
		if (basicRateBasedAllowance.contains(atcAlw.getPayCode().getId())) {
			Date sevencpcDate = DateManager.getSeventhCpcDate();
			int basicPay = newAuditCage.getBasicPay() == null ? 0 : newAuditCage.getBasicPay();
			int gradePay = newAuditCage.getGradePay() == null ? 0 : newAuditCage.getGradePay();
			int oldGradePay = newAuditCage.getOldGradePay() == null ? 0 : newAuditCage.getOldGradePay();
			int dearnessPay = newAuditCage.getDp() == null ? 0 : newAuditCage.getDp();
			
			if (newAuditCage.getFromDate()!=null && !newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate()) 
					&& atcAlw.getPayCode()!=null && atcAlw.getPayCode().getPayCode().equals("HCA")) {
			
				List<Do2> do2List =do2Repo.findByDescColB(newAuditCage.getEmployee().getId(),Do2Manager.VALID,"OPTCPC","SEVENPC");
					 
				if (do2List.size()>0){
				   	sevencpcDate = do2List.get(0).getFromDate();
				   	oldGradePay = gradePay;
				}
			}
						
			if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())){
				if (newAuditCage.getFromDate().before(DateManager.getDPDate())){
					;
				} else {
					basicPay= basicPay+dearnessPay;
				}
			}
						
			/*if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
				rateList = Utils.getApplicableAllowanceRates(pc.get(0)
						.getPayCode(), newAuditCage.getFromDate(), newAuditCage
						.getFromDate(), basicPay);
			} else if (newAuditCage.getDo2().getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				rateList = Utils.getApplicableAllowanceRates(pc.get(0)
						.getPayCode(), newAuditCage.getFromDate(), newAuditCage
						.getFromDate(), (newAuditCage.getOldBasicPay()+Math.round(newAuditCage.getOldBasicPay()/2)+newAuditCage.getOldClassPay()));
				} else {
					rateList = Utils.getApplicableAllowanceRates(pc.get(0)
							.getPayCode(), newAuditCage.getGradePay(), newAuditCage
							.getFromDate(), newAuditCage.getFromDate());
					}*/

			// commented above lines and amended as given below due to implementation of sevencpc
			
			if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
				rateList = Utils.getApplicableAllowanceRates(pc.getPayCode(), newAuditCage.getFromDate(), newAuditCage
						.getFromDate(), basicPay);
			} else if (newAuditCage.getDo2().getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				rateList = Utils.getApplicableAllowanceRates(pc.getPayCode(), newAuditCage.getFromDate(), newAuditCage
						.getFromDate(), (newAuditCage.getOldBasicPay()+Math.round(newAuditCage.getOldBasicPay()/2)+newAuditCage.getOldClassPay()));
				} else if (newAuditCage.getDo2().getFromDate().before(sevencpcDate)) {
					rateList = Utils.getApplicableAllowanceRates(pc.getPayCode(), gradePay, 
							newAuditCage.getFromDate(), newAuditCage.getFromDate());
				} else if (!newAuditCage.getDo2().getFromDate().after(DateManager.getSeventhCpcAllowanceDate())){
					rateList = Utils.getApplicableAllowanceRates(pc.getPayCode(), 
							oldGradePay, newAuditCage.getFromDate(), newAuditCage.getFromDate());	
				} else { // HCA abolished with effect from 01-07-2017. 
					final String[] alwUnderProcess = {atcAlw.getPayCode().getPayCode() };
					final List<String> alwToStop = Arrays.asList(alwUnderProcess);
					removeAllowances(alwToStop, newAuditCage);
				}

			chkAmt = (int) (rateList!=null && rateList.size() > 0 ? rateList.get(0).getRateAmount() : chkAmt);
		}
		// whether the rate includes dp before 1.1.2006 and is basicpay as per
		// the auditcage can be a parameter
		
	/*	if (basicPcBasedAllowance.contains(atcAlw.getPayCode().getId())) {
			double daRate = 0.0;
			int dearnessAmount = 0;
			int basicPay = newAuditCage.getBasicPay() == null ? 0 : newAuditCage.getBasicPay();
			int dp = newAuditCage.getDp() == null ? 0 : newAuditCage.getDp();
			int gradePay = newAuditCage.getGradePay() == null ? 0 : newAuditCage.getGradePay();
			int msPay = newAuditCage.getMsPay() == null ? 0 : newAuditCage.getMsPay();
			int xGroupPay = newAuditCage.getXGroupPay() == null ? 0 : newAuditCage.getXGroupPay();
			int classPay = newAuditCage.getClassPay() == null ? 0 : newAuditCage.getClassPay();
			int dearnessAllowance = getFullMonthDaWithoutTransport(newAuditCage.getDo2(), newAuditCage);
			int oldBasicPay = newAuditCage.getOldBasicPay() == null ? 0 : newAuditCage.getOldBasicPay();
			int oldClassPay = newAuditCage.getOldClassPay() == null ? 0 : newAuditCage.getOldClassPay();
			int oldDp = (int)(Math.round(oldBasicPay * 0.5));
			List<Allowance> daRates = Utils.getApplicableAllowanceRates("DA",newAuditCage.getFromDate(),newAuditCage.getFromDate());
			if(daRates.size() >0){
				daRate = daRates.get(0).getRateAmount();
				dearnessAmount = (int) Math.round((oldBasicPay+oldDp) * daRate);
			}
			rateList = Utils.getApplicableAllowanceRates(pc.get(0).getPayCode(), newAuditCage.getFromDate(), newAuditCage.getFromDate());
			if (rateList.size() > 0){
				 if (atcAlw.getPayCode().getPayCode().equals("NSGA")) {
					if(newAuditCage.getFromDate().before((DateManager.getSixthCpcDate()))){
						chkAmt = (int) Math.round((basicPay + dp + classPay + dearnessAllowance) * rateList.get(0).getRatePercentage());
			 		}else if (newAuditCage.getFromDate().before((DateManager.getSixthCpcAllowanceDate()))){
			 			chkAmt = (int) Math.round((oldBasicPay + oldDp + oldClassPay + dearnessAmount) * rateList.get(0).getRatePercentage());
			 		}else {
			 			chkAmt = (int) Math.round((basicPay + gradePay + msPay + xGroupPay + classPay + dearnessAllowance)*rateList.get(0).getRatePercentage());
			 		}
				} else if (atcAlw.getPayCode().getPayCode().equals("HAA")) {
					if (newAuditCage.getFromDate().before((DateManager.getSixthCpcDate()))){
						chkAmt = (int) Math.round((basicPay + dp) * rateList.get(0).getRatePercentage());
			 		} else if (newAuditCage.getFromDate().before((DateManager.getSixthCpcAllowanceDate()))){
			 			chkAmt = (int) Math.round((oldBasicPay + oldDp) * rateList.get(0).getRatePercentage());
			 		} else {
			 			chkAmt = (int) Math.round((basicPay + gradePay + msPay + xGroupPay) * rateList.get(0).getRatePercentage());
			 		}
				} else if (atcAlw.getPayCode().getPayCode().equals("SSA")) {
					if (newAuditCage.getFromDate().before((DateManager.getSixthCpcAllowanceDate()))){
			 			chkAmt = (int) Math.round((oldBasicPay + oldDp) * rateList.get(0).getRatePercentage());
			 		} else{
			 			chkAmt = (int) Math.round((basicPay + gradePay + msPay + xGroupPay) * rateList.get(0).getRatePercentage());
			 		} 
				} else if (atcAlw.getPayCode().getPayCode().equals("DEPA")) {
					if (newAuditCage.getFromDate().before((DateManager.getSixthCpcDate()))){
						chkAmt = (int) Math.round((basicPay + classPay) * rateList.get(0).getRatePercentage());
			 		} else if (!newAuditCage.getFromDate().before((DateManager.getSixthCpcAllowanceDate()))){
			 			if (atcAlw.getDo2() != null && atcAlw.getDo2().getColumnB() != null){
			 				if (atcAlw.getDo2().getColumnB().equals("50%")) {
			 					chkAmt = (int) Math.round((basicPay + gradePay) * 0.05);
							    if (chkAmt > 2000){
							    	chkAmt = 2000;
							    }
							} else {
								chkAmt = (int)Math.round((basicPay + gradePay) * 0.1);
								 if (chkAmt > 4000){
									 chkAmt = 4000;
								 }
							}
			 			}
			 		}
			 	} 
		    }
			return chkAmt;
		}*/
		
		
		if (basicPcBasedAllowance.contains(atcAlw.getPayCode().getId())) {
			double daRate = 0.0;
			int dearnessAmount = 0;
			int basicPay = newAuditCage.getBasicPay() == null ? 0 : newAuditCage.getBasicPay();
			int dp = newAuditCage.getDp() == null ? 0 : newAuditCage.getDp();
			int gradePay = newAuditCage.getGradePay() == null ? 0 : newAuditCage.getGradePay();
			int msPay = newAuditCage.getMsPay() == null ? 0 : newAuditCage.getMsPay();
			int xGroupPay = newAuditCage.getXGroupPay() == null ? 0 : newAuditCage.getXGroupPay();
			int classPay = newAuditCage.getClassPay() == null ? 0 : newAuditCage.getClassPay();
			int dearnessAllowance = getFullMonthDaWithoutTransport(newAuditCage.getDo2(), newAuditCage);
			int oldBasicPay = newAuditCage.getOldBasicPay() == null ? 0 : newAuditCage.getOldBasicPay();
			int oldClassPay = newAuditCage.getOldClassPay() == null ? 0 : newAuditCage.getOldClassPay();
			int oldDp = (int)(Math.round(oldBasicPay * 0.5));
			int oldGradePay = newAuditCage.getOldGradePay() == null ? 0 : newAuditCage.getOldGradePay();
			
			List<Allowance> daRates = Utils.getApplicableAllowanceRates("DA",newAuditCage.getFromDate(),newAuditCage.getFromDate());
			if(daRates.size() >0){
				daRate = daRates.get(0).getRateAmount();
				dearnessAmount = (int) Math.round((oldBasicPay+oldDp) * daRate);
			}
			rateList = Utils.getApplicableAllowanceRates(pc.getPayCode(), newAuditCage.getFromDate(), newAuditCage.getFromDate());
			if (rateList.size() > 0){
				 if (atcAlw.getPayCode().getPayCode().equals("NSGA")) {
					 Date sevencpcDate = DateManager.getSeventhCpcDate();
					 List<Do2> do2List = do2Repo.findByDescColB(atcAlw.getDo2().getEmployee().getId(),Do2Manager.VALID,"OPTCPC","SEVENPC");
					 
					 if (do2List.size()>0){
						sevencpcDate = do2List.get(0).getFromDate();							   
					  }
					if(newAuditCage.getFromDate().before((DateManager.getSixthCpcDate()))){
						chkAmt = (int) Math.round((basicPay + dp + classPay + dearnessAllowance) * rateList.get(0).getRatePercentage());
			 		}else if (newAuditCage.getFromDate().before((DateManager.getSixthCpcAllowanceDate()))){
			 			chkAmt = (int) Math.round((oldBasicPay + oldDp + oldClassPay + dearnessAmount) * rateList.get(0).getRatePercentage());
			 		}else if (newAuditCage.getFromDate().before(sevencpcDate)){
			 			chkAmt = (int) Math.round((basicPay + gradePay + msPay + xGroupPay + classPay + dearnessAllowance)*rateList.get(0).getRatePercentage());
			 		} else if (newAuditCage.getFromDate().before((DateManager.getSeventhCpcAllowanceDate()))){
			 			msPay = Utils.get6cpcMsPay(newAuditCage);
			 			xGroupPay = Utils.get6cpcXGroupPay(newAuditCage);
			 	//		chkAmt = (int) Math.round((oldBasicPay + oldGradePay + msPay + xGroupPay + classPay + dearnessAllowance)*rateList.get(0).getRatePercentage());  Below two lines added on 20.10.2020
			 			int da = (int) Math.round((oldBasicPay + oldGradePay + msPay + xGroupPay)*daRate);
			 			chkAmt = (int) Math.round((oldBasicPay + oldGradePay + msPay + xGroupPay + classPay + da)*rateList.get(0).getRatePercentage());
			 			/*System.out.println ( " FROM DATE " +newAuditCage.getFromDate()  +" oldbasic " +oldBasicPay  +"  "+oldGradePay  +" " +msPay  +" " +xGroupPay  + " "
			 					+dearnessAllowance  +" " +classPay);*/
			 		} else {
			 			//chkAmt = (int) Math.round((basicPay  + msPay + xGroupPay + classPay + dearnessAllowance)*rateList.get(0).getRatePercentage());
			 			chkAmt = (int) Math.round((basicPay)*rateList.get(0).getRatePercentage());
			 		}
				} else if (atcAlw.getPayCode().getPayCode().equals("HAA")) {
					if (newAuditCage.getFromDate().before((DateManager.getSixthCpcDate()))){
						chkAmt = (int) Math.round((basicPay + dp) * rateList.get(0).getRatePercentage());
			 		} else if (newAuditCage.getFromDate().before((DateManager.getSixthCpcAllowanceDate()))){
			 			chkAmt = (int) Math.round((oldBasicPay + oldDp) * rateList.get(0).getRatePercentage());
			 		} else if (newAuditCage.getFromDate().before((DateManager.getSeventhCpcDate()))){
			 			chkAmt = (int) Math.round((basicPay + gradePay + msPay + xGroupPay) * rateList.get(0).getRatePercentage());
			 		} else if (newAuditCage.getFromDate().before((DateManager.getSeventhCpcAllowanceDate()))){
			 			msPay = Utils.get6cpcMsPay(newAuditCage);
			 			xGroupPay = Utils.get6cpcXGroupPay(newAuditCage);
			 			chkAmt = (int) Math.round((oldBasicPay + oldGradePay + msPay + xGroupPay) * rateList.get(0).getRatePercentage());
			 		} else {
			 			//chkAmt = (int) Math.round((basicPay + gradePay + msPay + xGroupPay) * rateList.get(0).getRatePercentage());
			 			chkAmt = (int) Math.round((basicPay) * rateList.get(0).getRatePercentage());
			 		}
				} else if (atcAlw.getPayCode().getPayCode().equals("SSA")) {
					if (newAuditCage.getFromDate().before((DateManager.getSixthCpcAllowanceDate()))){
			 			chkAmt = (int) Math.round((oldBasicPay + oldDp) * rateList.get(0).getRatePercentage());
			 		} else if (newAuditCage.getFromDate().before((DateManager.getSeventhCpcDate()))){
			 			chkAmt = (int) Math.round((basicPay + gradePay + msPay + xGroupPay) * rateList.get(0).getRatePercentage());
			 		} else if (newAuditCage.getFromDate().before((DateManager.getSeventhCpcAllowanceDate()))){
			 			msPay = Utils.get6cpcMsPay(newAuditCage);
			 			xGroupPay = Utils.get6cpcXGroupPay(newAuditCage);
			 			chkAmt = (int) Math.round((oldBasicPay + oldGradePay + msPay + xGroupPay) * rateList.get(0).getRatePercentage());
			 		} else {
			 			if (!Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)) {
			 				chkAmt = (int) Math.round((basicPay) * rateList.get(0).getRatePercentage());
						} else {
							chkAmt = (int) Math.round((basicPay) * 0.20);
						}
			 		}
				} else if (atcAlw.getPayCode().getPayCode().equals("DEPA")) {
					if (newAuditCage.getFromDate().before((DateManager.getSixthCpcDate()))){
						chkAmt = (int) Math.round((basicPay + classPay) * rateList.get(0).getRatePercentage());
					} else if (newAuditCage.getFromDate().before((DateManager.getSixthCpcAllowanceDate()))){
						chkAmt = (int) Math.round((oldBasicPay + oldClassPay) * rateList.get(0).getRatePercentage());
					} else if (newAuditCage.getFromDate().before((DateManager.getSeventhCpcDate()))){
						if (atcAlw.getDo2() != null && atcAlw.getDo2().getColumnB() != null){
			 				if (atcAlw.getDo2().getColumnB().equals("50%")) {
			 					chkAmt = (int) Math.round((basicPay + gradePay) * 0.05);
							    if (chkAmt > 2000){
							    	chkAmt = 2000;
							    }
							} else {
								chkAmt = (int)Math.round((basicPay + gradePay) * 0.1);
								 if (chkAmt > 4000){
									 chkAmt = 4000;
								 }
							}
			 			}
					} else  if (newAuditCage.getFromDate().before((DateManager.getSeventhCpcAllowanceDate()))){
						if (atcAlw.getDo2() != null && atcAlw.getDo2().getColumnB() != null){
							if (atcAlw.getDo2().getColumnB().equals("50%")) {
			 					chkAmt = (int) Math.round((oldBasicPay + oldGradePay) * 0.05);
							    if (chkAmt > 2000){
							    	chkAmt = 2000;
							    }
							} else {
								chkAmt = (int)Math.round((oldBasicPay + oldGradePay) * 0.1);
								 if (chkAmt > 4000){
									 chkAmt = 4000;
								 }
							}
						}
		 			} else {
		 				if (atcAlw.getDo2() != null && atcAlw.getDo2().getColumnB() != null){
		 					if (atcAlw.getDo2().getColumnB().equals("50%")) {
			 					chkAmt = (int) Math.round((basicPay) * 0.05);
							    if (chkAmt > 4500){
							    	chkAmt = 4500;
							    }
							} else {
								chkAmt = (int)Math.round((basicPay) * 0.1);
								 if (chkAmt > 9000){
									 chkAmt = 9000;
								 }
							}
		 				}		 				
					}
					
				}
				return chkAmt;				
			}
			 if (atcAlw.getPayCode().getPayCode().equals("ADPOST")) {
					if (!newAuditCage.getFromDate().before((DateManager.getSeventhCpcAllowanceDate()))) {
						if (newAuditCage.getMsPay()==15500) {
							return 0;
						}
						if (atcAlw.getDo2() != null && atcAlw.getDo2().getColumnB() != null) {
							int allowanceAmount = atcAlw.getAmount();
							int basicPayTenPercent = (int) (Math.round(newAuditCage.getBasicPay()*0.1));
		 			 		List<PayMatrix> payMatrixList = null;
					 		int increasedBasicPay = newAuditCage.getBasicPay();
					 		payMatrixList = matrixRepo.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(newAuditCage.getPayLevel(),newAuditCage.getBasicPay());
					 			 
					 		if (payMatrixList!=null && payMatrixList.size()>0) {
					 			increasedBasicPay=payMatrixList.get(0).getBasicPay();
					 			payMatrixList=null;
					 		}
							
					 		if (atcAlw.getDo2().getColumnB().trim().equals("CAPT")) {
					 			payMatrixList = matrixRepo.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc("10B",increasedBasicPay);
					 				 
					 		}
					 		
					 		if (atcAlw.getDo2().getColumnB().trim().equals("MAJ")) {
					 			payMatrixList = matrixRepo.findByPayLevelAndBasicPayGreaterThanEqualOrderByBasicPayAsc("11",increasedBasicPay);
					 				 
					 		}
					 		
					 		if (payMatrixList!=null && payMatrixList.size()>0) {
					 			increasedBasicPay=payMatrixList.get(0).getBasicPay();
					 			payMatrixList=null;
					 		}	 		
					 		
					 		if ((increasedBasicPay-newAuditCage.getBasicPay())>basicPayTenPercent) {
					 			allowanceAmount=(increasedBasicPay-newAuditCage.getBasicPay());
					 		} else {
					 			allowanceAmount=basicPayTenPercent;
					 		}
					 		
					 		if ((allowanceAmount+newAuditCage.getBasicPay())>225000) {
					 			allowanceAmount=(225000-(newAuditCage.getBasicPay()+allowanceAmount));
	 				 		}
	 				 		chkAmt = allowanceAmount;
						}
					}
				}
		
			 if (atcAlw.getPayCode().getPayCode().equals("EXTWK")) {
					basicPay = newAuditCage.getBasicPay() == null ? 0 : newAuditCage.getBasicPay();
					chkAmt = (int) Math.round(basicPay*0.02);
				
			 }


			 if (atcAlw.getPayCode().getPayCode().equals("TRGALL")) {
				 if(atcAlw.getDo2().getColumnB()!=null){
					 String columnB = atcAlw.getDo2().getColumnB();
					 basicPay = newAuditCage.getBasicPay() == null ? 0 : newAuditCage.getBasicPay();
					
						rateList = Utils.getApplicableAllowanceRates(atcAlw.getDo2().getDescription(),
									columnB, newAuditCage.getFromDate(), newAuditCage.getFromDate());
					
					  if (rateList.size() > 0) {
						  chkAmt = (int)Math.round((basicPay) * rateList.get(0).getRatePercentage());
					  }
					
				 }
			} 
		 
			 return chkAmt;
		}

		
		
		// is basicpay as per the auditcage can be a parameter
		
		/*if (basicAreaBasedAllowance.contains(atcAlw.getPayCode().getId())) {
			if (atcAlw.getDo2() != null) {
				if (newAuditCage.getFromDate().before(
						DateManager.getSixthCpcAllowanceDate())) {
					rateList = Utils.getApplicableAllowanceRates(atcAlw.getDo2().getDescription(),
							atcAlw.getDo2().getColumnB(),
							newAuditCage.getFromDate(), newAuditCage
									.getFromDate(), newAuditCage.getBasicPay());
					chkAmt = ((int) (rateList.size() > 0 ? rateList.get(0)
							.getRateAmount() : chkAmt));
				} else {
														
					rateList = Utils.getApplicableAllowanceRates(atcAlw.getDo2().getDescription(),
							atcAlw.getDo2().getColumnB(),newAuditCage.getGradePay(), newAuditCage.getFromDate(), newAuditCage
									.getFromDate() );
					chkAmt = ((int) (rateList.size() > 0 ? rateList.get(0)
							.getRateAmount() : chkAmt));
				}
			}
			
			
			return chkAmt;
		}*/
		
		
		// commented above lines and amended as given below due to implementation of sevencpc
		
		
		if (basicAreaBasedAllowance.contains(atcAlw.getPayCode().getId())) {
			if (atcAlw.getDo2()!=null) {
				int basicPay = newAuditCage.getBasicPay();
				int gradePay = newAuditCage.getGradePay();
				int oldGradePay = newAuditCage.getOldGradePay();
				Date sevencpcDate = DateManager.getSeventhCpcDate();
				Date fromDate = newAuditCage.getFromDate();
				String description = atcAlw.getDo2().getDescription();
				String columnB = atcAlw.getDo2().getColumnB();
				
				List<Do2> do2List = do2Repo.findByDescColB(atcAlw.getDo2().getEmployee().getId(), Do2Manager.VALID, "OPTCPC", "SEVENPC");
					  
					if (do2List.size()>0){
					   	sevencpcDate = do2List.get(0).getFromDate();
					   	oldGradePay = gradePay;
					}
					if (newAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
						rateList = Utils.getApplicableAllowanceRates(description, columnB,  fromDate, fromDate, basicPay);
						chkAmt = ((int) (rateList.size() > 0 ? rateList.get(0).getRateAmount() : chkAmt));
					} else if (newAuditCage.getFromDate().before(sevencpcDate)){
						rateList = Utils.getApplicableAllowanceRates(description, columnB,  gradePay, fromDate, fromDate);
						chkAmt = ((int) (rateList.size() > 0 ? rateList.get(0).getRateAmount() : chkAmt));
					} else if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())){
						rateList = Utils.getApplicableAllowanceRates(description, columnB,  fromDate, fromDate, oldGradePay);
						chkAmt = ((int) (rateList.size() > 0 ? rateList.get(0).getRateAmount() : chkAmt));
					}  else {
						int gradePayForPayLevel = Do2Manager.getSevenCpcGradepay(newAuditCage.getPayLevel());
						rateList = Utils.getApplicableAllowanceRates(description,columnB, gradePayForPayLevel, fromDate,fromDate );
						chkAmt = ((int) (rateList.size() > 0 ? rateList.get(0)
							.getRateAmount() : chkAmt));
				}
			}
			return chkAmt;
		}
			
		
		/*if (basicAreaBasedAllowance.contains(atcAlw.getPayCode().getId())) {
			if (atcAlw.getDo2() != null) {
				if (newAuditCage.getFromDate().before(
						DateManager.getSixthCpcAllowanceDate())) {
					rateList = Utils.getApplicableAllowanceRates(atcAlw.getDo2().getDescription(),
							atcAlw.getDo2().getColumnB(),
							newAuditCage.getFromDate(), newAuditCage
									.getFromDate(), newAuditCage.getBasicPay());
					chkAmt = ((int) (rateList.size() > 0 ? rateList.get(0)
							.getRateAmount() : chkAmt));
				} else if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())){
														
					rateList = Utils.getApplicableAllowanceRates(atcAlw.getDo2().getDescription(),
							atcAlw.getDo2().getColumnB(),newAuditCage.getGradePay(), newAuditCage.getFromDate(), newAuditCage
									.getFromDate() );
					chkAmt = ((int) (rateList.size() > 0 ? rateList.get(0)
							.getRateAmount() : chkAmt));
				} else if (newAuditCage.getFromDate().before((DateManager.getSeventhCpcAllowanceDate()))){
					rateList = Utils.getApplicableAllowanceRates(atcAlw.getDo2().getDescription(),
							atcAlw.getDo2().getColumnB(),newAuditCage.getOldGradePay(), newAuditCage.getFromDate(), newAuditCage
									.getFromDate() );
					chkAmt = ((int) (rateList.size() > 0 ? rateList.get(0)
							.getRateAmount() : chkAmt));
				} else {
					int gradePayForPayLevel = Do2Manager.getSevenCpcGradepay(newAuditCage.getPayLevel());
						rateList = Utils.getApplicableAllowanceRates(atcAlw.getDo2().getDescription(),
								atcAlw.getDo2().getColumnB(),newAuditCage.getGradePay(), newAuditCage.getFromDate(), newAuditCage
										.getFromDate() );
						rateList = Utils.getApplicableAllowanceRates(atcAlw.getDo2().getDescription(),
								atcAlw.getDo2().getColumnB(),gradePayForPayLevel, newAuditCage.getFromDate(), newAuditCage
										.getFromDate() );
					chkAmt = ((int) (rateList.size() > 0 ? rateList.get(0)
							.getRateAmount() : chkAmt));
				}
			}
			return chkAmt;
		}
*/
		if (rankAreaBasedAllowance.contains(atcAlw.getPayCode().getId())) {

			 if (atcAlw.getPayCode().getPayCode().equals("SPIC")) {
			//	 System.out.println ( " im here xxx ");
				 if(atcAlw.getDo2().getColumnB()!=null){
				//	 System.out.println ( " im here yyy ");
					 rank = Utils.getPborRank(atcAlw.getDo2().getArmyNo(), atcAlw.getDo2().getFromDate(),"ALLCE");
					 int gradePay = Do2Manager.getSevenCpcGradepay(newAuditCage.getPayLevel());
				//	 System.out.println (  " from date " +atcAlw.getDo2().getFromDate()  + " gp " +gradePay   + " columnB " +atcAlw.getDo2().getColumnB()  +atcAlw.getDo2().getDescription()  +" fdnew" +newAuditCage.getDo2().getFromDate());
					 rateList = Utils.getApplicableAllowanceRates(atcAlw.getDo2().getDescription(), gradePay, atcAlw.getDo2().getColumnB(), 
							 newAuditCage.getDo2().getFromDate(), newAuditCage.getDo2().getFromDate());
						if (rateList.size()>0) {
				//			System.out.println ( " im hee ");
							chkAmt = (int) rateList.get(0).getRateAmount();	
				 		}
				 }
				 
			 } else {
				 if(atcAlw.getDo2()!=null){
						
						if(atcAlw.getDo2().getColumnB() ==null){
						  rateList = Utils.getApplicableAllowanceRates(pc.getPayCode(), 
									 DateManager.getScciaOldDate(),DateManager.getScciaOldDate(), rank);
						  chkAmt = ((int) (rateList.size() > 0 ? rateList.get(0)
									.getRateAmount() : chkAmt));
					  }else{
						  rateList = Utils.getApplicableAllowanceRates(pc.getPayCode(), 
									atcAlw.getDo2().getColumnB(), newAuditCage.getFromDate(), newAuditCage.getFromDate(), rank);
						  chkAmt = ((int) (rateList.size() > 0 ? rateList.get(0)
									.getRateAmount() : chkAmt));
					  }
					}
			 }
			
			
			
			
			return chkAmt;

		}

		if (dateBasedAllowance.contains(atcAlw.getPayCode().getId())) {

			rateList = Utils.getApplicableAllowanceRates(
					pc.getPayCode(), newAuditCage.getFromDate(),
					newAuditCage.getFromDate());
			chkAmt = ((int) (rateList.size() > 0 ? rateList.get(0)
					.getRateAmount() : chkAmt));
			return chkAmt;
		}

		if (rankCityBasedAllowance.contains(atcAlw.getPayCode().getId())) {
			if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
				
				// Added on 18.04.2022 to get CILQ adjusted as per MACPS rank wef 01092008 
				if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
					rank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), DateManager.getNextDate(newAuditCage.getFromDate()),"PAYFIX");	
				}
				
			 			if (atcAlw.getDo2() != null) {
							String columnC = atcAlw.getDo2().getColumnC();
							if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
								if (columnC.equals("CCOTH") || columnC.equals("CCC")) {
									columnC="CCZ";
								}
								if (columnC.equals("CCA") || columnC.equals("CCB1") || columnC.equals("CCB2")) {
									columnC="CCY";
								}
								if (columnC.equals("CCA1")) {
									columnC="CCX";
								}
							}
					 		rateList = Utils.getApplicableAllowanceRates(pc.getPayCode(), newAuditCage.getFromDate(),
									newAuditCage.getFromDate(), rank, columnC);
							chkAmt = ((int) (rateList.size() > 0 ? rateList.get(0).getRateAmount() : chkAmt));
							
							
							
					 		return chkAmt;
						}
			} else {
					final String[] alwUnderProcess = {"CILQ"};
					final List<String> alwToStop = Arrays.asList(alwUnderProcess);
					removeAllowances(alwToStop, newAuditCage);
			}
	
		}


		if (rankPaoBasedAllowance.contains(atcAlw.getPayCode().getId())) {
			Pao pao = Utils.getPao();
			
			if (!Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
				pao = null; 
			}
			rateList = Utils.getApplicableAllowanceRates(pc.getPayCode(), newAuditCage.getFromDate(),newAuditCage.getFromDate(),
					rank, pao);
			chkAmt = ((int) (rateList.size() > 0 ? rateList.get(0).getRateAmount() : chkAmt));
			return chkAmt;
		}
		
		
		/*if (cityBasedAllowance.contains(atcAlw.getPayCode().getId())) {
			if (atcAlw.getPayCode().getPayCode().equals("FAA")) {
				rateList = Utils.getApplicableAllowanceRates("FAA", atcAlw.getAuditCage().getFromDate(), atcAlw.getAuditCage().getFromDate());
			} else if (atcAlw.getDo2() != null) {
					rateList = Utils.getApplicableAllowanceRates(atcAlw.getDo2()
									.getDescription(), atcAlw.getDo2()
									.getFromDate(), atcAlw.getDo2()
									.getFromDate(), atcAlw.getDo2().getColumnC());
			}
			if (rateList !=null){
				int gradePay = newAuditCage.getGradePay() == null ? 0 : newAuditCage.getGradePay();
				int msPay = newAuditCage.getMsPay() == null ? 0 : newAuditCage.getMsPay();
				chkAmt = (rateList.size() > 0 ? (int) Math.round((newAuditCage.getBasicPay() + gradePay + msPay)
						* rateList.get(0).getRatePercentage()) : chkAmt);
			}
		}*/
		
		// commented above lines and amended as given below due to implementation of sevencpc
		
		if (cityBasedAllowance.contains(atcAlw.getPayCode().getId())) {
			
			
			if (atcAlw.getPayCode().getPayCode().equals("FAA")) {
				
				final String[] alwUnderProcess = {"FAA" };
				final List<String> alwToStop = Arrays.asList(alwUnderProcess);
				 if (!newAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())){
					 removeAllowances(alwToStop, newAuditCage);
				} else {
					if (Integer.parseInt(rank.getRankCode()) > 49) {
						removeAllowances(alwToStop, newAuditCage);
					} else {
						rateList = Utils.getApplicableAllowanceRates("FAA", atcAlw.getAuditCage().getFromDate(), atcAlw.getAuditCage().getFromDate());
					}
				}
			} 	else if (atcAlw.getPayCode().getPayCode().equals("HRA")  || 
									 atcAlw.getPayCode().getPayCode().equals("FAMLF")){


					if (atcAlw.getDo2().getColumnC() != null) {
						String columnC =atcAlw.getDo2().getColumnC();
						if (atcAlw.getDo2().getColumnC()!=null && atcAlw.getDo2().getColumnC().length() >= 3){
							if (atcAlw.getDo2().getColumnC().equals("CCZ")){
								columnC = "CCZ-1";				
							}
							if (atcAlw.getDo2().getColumnC().equals("CCY")){
								columnC = "CCY-1";				
							}
							if ( atcAlw.getDo2().getColumnC().equals("CCX")){
								columnC = "CCX-1";				
							}
							
					/*		rateList = Utils.getApplicableAllowanceRates(atcAlw.getDo2()
									.getDescription(), atcAlw.getDo2()
									.getFromDate(), atcAlw.getDo2()
									.getFromDate(), columnC); */
							
							rateList = Utils.getApplicableAllowanceRates(atcAlw.getDo2()
									.getDescription(), atcAlw.getAuditCage()
									.getFromDate(), atcAlw.getAuditCage()
									.getFromDate(), columnC);
							
						}
					}
					
			} else  if (atcAlw.getPayCode().getPayCode().equals("HRAFAM")){
				
				if (atcAlw.getDo2().getColumnC() != null) {

					String columnC=atcAlw.getDo2().getColumnC();
					if (atcAlw.getDo2().getColumnC()!=null && atcAlw.getDo2().getColumnC().length() >= 3){
						if (atcAlw.getDo2().getColumnC().startsWith("CCZ")){
							columnC = "CCZ";				
						}
						if (atcAlw.getDo2().getColumnC().startsWith("CCY")){
							columnC = "CCY";				
						}
						if ( atcAlw.getDo2().getColumnC().startsWith("CCX")){
							columnC = "CCX";				
						}
					rateList = Utils.getApplicableAllowanceRates(atcAlw.getDo2()
							.getDescription(), atcAlw.getDo2()
							.getFromDate(), atcAlw.getDo2()
							.getFromDate(), atcAlw.getDo2().getColumnC());
				}
			 }
		} else if (atcAlw.getPayCode().getPayCode().equals("HRALF")) {
			String columnC=atcAlw.getDo2().getColumnC();
			if (atcAlw.getDo2().getColumnC()!=null && atcAlw.getDo2().getColumnC().length() >= 3){
				
				if (Integer.parseInt(rank.getRankCode()) > 49) {
					if (atcAlw.getDo2().getColumnC().startsWith("CCZ")){
						columnC = "CCZ-4";				
					}
					if (atcAlw.getDo2().getColumnC().startsWith("CCY")){
						columnC = "CCY-4";				
					}
					if ( atcAlw.getDo2().getColumnC().startsWith("CCX")){
						columnC = "CCX-4";				
					}
				} else  {
					if (atcAlw.getDo2().getColumnC().startsWith("CCZ")){
						columnC = "CCZ-3";				
					}
					if (atcAlw.getDo2().getColumnC().startsWith("CCY")){
						columnC = "CCY-3";				
					}
					if ( atcAlw.getDo2().getColumnC().startsWith("CCX")){
						columnC = "CCX-3";				
					}
				}
				
			rateList = Utils.getApplicableAllowanceRates(atcAlw.getDo2()
					.getDescription(), atcAlw.getDo2()
					.getFromDate(), atcAlw.getDo2()
					.getFromDate(), atcAlw.getDo2().getColumnC());
		 }
		}
			if (rateList !=null){
				 
				int gradePay = newAuditCage.getGradePay() == null ? 0 : newAuditCage.getGradePay();
				int msPay = newAuditCage.getMsPay() == null ? 0 : newAuditCage.getMsPay();
				int oldGradePay = newAuditCage.getOldGradePay() == null ? 0 : newAuditCage.getOldGradePay();
				 Date sevencpcDate = DateManager.getSeventhCpcDate();
				  List<Do2> do2List = do2Repo.findByDescColB(atcAlw.getDo2().getEmployee().getId(),"V", "OPTCPC","SEVENPC");
				  				 if (do2List.size()>0){
				  					sevencpcDate = do2List.get(0).getFromDate();							   
				  				  }
				
				if (atcAlw.getPayCode().getPayCode().equals("HRAFAM")) {
					if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())){
						  chkAmt = (rateList.size() > 0 ? (int) Math.round((newAuditCage.getBasicPay() + gradePay + msPay)
									* rateList.get(0).getRatePercentage()) : chkAmt);
					 } else /*if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate()))*/{
						 msPay = Utils.get6cpcMsPay(newAuditCage);
				 		 chkAmt = (rateList.size() > 0 ? (int) Math.round((newAuditCage.getOldBasicPay() + oldGradePay + msPay)
									* rateList.get(0).getRatePercentage()) : chkAmt);
					}
				} else 	if (atcAlw.getPayCode().getPayCode().equals("HRA")) {
					if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())){
						  chkAmt = (rateList.size() > 0 ? (int) Math.round((newAuditCage.getBasicPay() + gradePay + msPay)
									* rateList.get(0).getRatePercentage()) : chkAmt);
					 } else if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())){
						 msPay = Utils.get6cpcMsPay(newAuditCage);
				 		 chkAmt = (rateList.size() > 0 ? (int) Math.round((newAuditCage.getOldBasicPay() + oldGradePay + msPay)
									* rateList.get(0).getRatePercentage()) : chkAmt);
					} else if (atcAlw.getPayCode().getPayCode().equals("HRALF")) {  // Modified on 21.09.2020 as new LF rates updated
						 chkAmt = (rateList.size() > 0 ? (int) Math.round(rateList.get(0).getRateAmount()) : chkAmt);
					} else if (atcAlw.getPayCode().getPayCode().equals("FAMLF")) {
							chkAmt=150;
					} else {
						/* chkAmt = (rateList.size() > 0 ? (int) Math.round((newAuditCage.getBasicPay())
									* rateList.get(0).getRatePercentage()) : chkAmt);*/
						if ((atcAlw.getDo2().getLra() != null && atcAlw.getDo2().getLra() == 5) 
								&& (atcAlw.getDo2().getColumnC()!=null && 
								!atcAlw.getDo2().getColumnC().substring(0,3).equals("CCZ"))){
							chkAmt = (rateList.size() > 0 ? (int) Math.round((newAuditCage.getBasicPay())
									* (rateList.get(0).getRatePercentage() - 0.05)) : chkAmt);
						} else {
							chkAmt = (rateList.size() > 0 ? (int) Math.round((newAuditCage.getBasicPay())
													* rateList.get(0).getRatePercentage()) : chkAmt);
						}
				   }
				}else if (atcAlw.getPayCode().getPayCode().equals("FAA")) {
					 					if (newAuditCage.getFromDate().before(sevencpcDate)){
						 						 chkAmt = (rateList.size() > 0 ? (int) Math.round((newAuditCage.getBasicPay()+gradePay+msPay)
						 								 * rateList.get(0).getRatePercentage()) : chkAmt);
						 					} else if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
						 						
						 							msPay = Utils.get6cpcMsPay(newAuditCage);
						 							 chkAmt = (rateList.size() > 0 ? (int) Math.round((newAuditCage.getOldBasicPay()+oldGradePay+msPay)
						 									 * rateList.get(0).getRatePercentage()) : chkAmt);
						 					}
			}
			}
			
		}
		if (bhutanRateRevision.contains(atcAlw.getPayCode().getId())){
			   chkAmt = getBhutanRate(newAuditCage.getDo2());
		}
		
		//lines added for revision of islnda allowance
		if (payElementsRelatedRevision.contains(atcAlw.getPayCode().getId())){
		   if (atcAlw.getDo2() !=null){
			   double amount = getIslndaRate(atcAlw.getDo2(),newAuditCage);
			   chkAmt = (int) amount;
		   }	   
		}
		if (hrdallRevision.contains(atcAlw.getPayCode().getId())){
		 /*  if (atcAlw.getDo2() !=null){
			   double amount = getHrdallRate(atcAlw.getDo2(),newAuditCage);
			   chkAmt = (int) amount;
		   }*/
		   
		   double amount = getHrdallRate(atcAlw.getDo2(),newAuditCage.getDo2(),newAuditCage);
		   chkAmt = (int) amount;
		}
		
		if (rumcigRevision.contains(atcAlw.getPayCode().getId())){
			
			rateList = Utils.getApplicableAllowanceRates(atcAlw.getDo2()
					.getDescription(), atcAlw.getDo2().getColumnB(), newAuditCage.getFromDate(), newAuditCage.getFromDate());
			
			if (rateList.size() > 0) {
				chkAmt = (rateList.size() > 0 ? (int) Math.round(rateList.get(0).getRateAmount()) : chkAmt);
			}
			
			return chkAmt;
		}

	/*	if (spincRevision.contains(atcAlw.getPayCode().getId())){
			
			chkAmt = reviseSpincRate(chkAmt, atcAlw.getDo2(), newAuditCage);
			
			return chkAmt;
		} */	
		
		if (gradePayBasedAllowance.contains(atcAlw.getPayCode().getId())) {
			if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {														
			 			if (atcAlw.getDo2() != null) { 							 
			 				int gradePay = 0;
			 				gradePay = Do2Manager.getSevenCpcGradepay(newAuditCage.getPayLevel());			
			 				rateList = Utils.getApplicableAllowanceRates(atcAlw.getDo2().getDescription(), gradePay, atcAlw.getDo2().getFromDate(), atcAlw.getDo2().getFromDate());
							chkAmt = ((int) (rateList.size() > 0 ? rateList.get(0).getRateAmount() : chkAmt));														
					 		return chkAmt;
						}
			} else {
					final String[] alwUnderProcess = {"PROALL"};
					final List<String> alwToStop = Arrays.asList(alwUnderProcess);
					removeAllowances(alwToStop, newAuditCage);
			}	
		}
		if (fieldAreaBasedAllowance.contains(atcAlw.getPayCode().getId())) {			
			rateList = Utils.getApplicableAllowanceRates(atcAlw.getDo2().getDescription(), atcAlw.getDo2().getColumnB(), newAuditCage.getFromDate(), newAuditCage.getFromDate());
			
			// Hqrs. Office letter No.AT-I/Army/ALLO-3500/5/2020-PAYC/E-869 dated 09.04.2025 addressed to ITSDC and copy to all PCsDA/CsDA, received through email.
			if (newAuditCage.getPayLevel()!=null && ((newAuditCage.getPayLevel().length()<=2 && Integer.parseInt(newAuditCage.getPayLevel())>=9) 
						|| (newAuditCage.getPayLevel().length()==3 && newAuditCage.getPayLevel().equals("10B")))) {
				int paylevel=9; // to fetch appropriate record from allowance_rate table
				rateList = null;
				rateList = Utils.getApplicableAllowanceRates(atcAlw.getDo2().getDescription(), atcAlw.getDo2().getColumnB(), paylevel, newAuditCage.getFromDate(), newAuditCage.getFromDate());
			}
	 		
			if (atcAlw.getDo2().getDescription().equals("RISK")) {
	 			for (Allowance allowance : rateList) {
	 				if (allowance.getRatePercentage()==atcAlw.getDo2().getRate()) {
	 					chkAmt=(int)allowance.getRateAmount();
	 					break;
	 				}
	 			}
	 		} else {
	 			chkAmt = ((int) (rateList.size() > 0 ? rateList.get(0).getRateAmount() : chkAmt));	
	 		}			
			return chkAmt;
		}

		return chkAmt;
		 
 	}

 	static public void generateCancelDo2(Do2 do2, Do2 stopAllRelatedDo2) {
		Do2 cancelDo2 = new Do2();
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		cancelDo2.setEmployee(employee);
		cancelDo2.setArmyNo(employee.getArmyNo());
		cancelDo2.setCheckDigit(employee.getCheckDigit());
		cancelDo2.setUnitCode(employee.getUnit().getUnitCode());
		cancelDo2.setUnit(employee.getUnit());
		cancelDo2.setDo2Group("IA");
		cancelDo2.setDo2Type(do2.getDo2Type());
		cancelDo2.setDo2No(do2.getDo2No());
		cancelDo2.setDo2ItemNo(do2.getDo2ItemNo());
		cancelDo2.setDo2Date(DateManager.getToday());
		cancelDo2.setDescription("CANCEL");
		cancelDo2.setFromDate(stopAllRelatedDo2.getFromDate());
		cancelDo2.setColumnA(stopAllRelatedDo2.getDescription());
		cancelDo2.setRefDo2No(Utils.getReferenceDo2No(stopAllRelatedDo2));
		cancelDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		cancelDo2.setAuditCage(true);
		cancelDo2.setStatus(Do2Manager.PENDING);
		cancelDo2.setQe(Utils.getCurrentQuarter());
		cancelDo2.setAcceptedQe(Utils.getCurrentQuarter());
		cancelDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		do2Repo.save(cancelDo2);
		 
	}

	// Method below updatearrestArrears is used for recovery of amount
	// credited at the time of arrest to be recovered at the time of cancel of
	// arrest/bail/acquit do2s.
	static public void recoverSpecifiedPercentageArrest(Do2 arrestDo2,
			Do2 newDo2) {

		List<Arrears> arrestArrearsList = arrRepo.findByDo2IdAndArrestPayment(arrestDo2.getId(), true);
		 
		if (arrestArrearsList.size() > 0) {
			updateArrestArrears(arrestArrearsList, arrestDo2, newDo2);
		}

	}

	static public void recoverSpecifiedPercentageBail(Do2 bailDo2, Do2 newDo2) {

		List<Arrears> bailArrearsList =arrRepo.findByDo2IdAndArrestPayment(bailDo2.getId(), true);
				 
		if (bailArrearsList.size() > 0) {
			updateArrestArrears(bailArrearsList, bailDo2, newDo2);
		}

	}

	private static void updateArrestArrears(List<Arrears> recoveryList,
			Do2 do2, Do2 newDo2) {

		for (Arrears arr : recoveryList) {
			if (arr.getAmount() == 0)
				continue;
			Arrears arrears = new Arrears();
			arrears.setPayCode(arr.getPayCode());
			arrears.setFromDate(arr.getFromDate());
			arrears.setToDate(arr.getToDate());
			if (newDo2.getDescription().equals(Do2Manager.CANCEL)
					&& newDo2.getColumnA().equals(Do2Manager.ACQUIT)) {
				arrears.setAmount(arr.getAmount());
			} else {
				arrears.setAmount(arr.getAmount() * -1);
			}
			arrears.setDo2(newDo2);
			arrears.setPaid(false);
			arrears.setArrestPayment(false);
			arrears.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			arrears.setQe(Utils.getCurrentQuarter());

			arrears.setRoundOffAmount(false);
			arrears.setArrestPayment(false);
			arrRepo.save(arrears);
			 

		}
	}

	public static void setSpecifiedValues(AuditCage auditCage,
			double paymentMultiplier) {
		auditCage.setBasicPay(auditCage.getBasicPay() == null ? 0 : (int) Math
				.round(auditCage.getBasicPay() * paymentMultiplier));
		auditCage.setDp(auditCage.getDp() == null ? 0 : (int) Math
				.round(auditCage.getDp() * paymentMultiplier));
		auditCage.setGradePay(auditCage.getGradePay() == null ? 0 : (int) Math
				.round(auditCage.getGradePay() * paymentMultiplier));
		auditCage.setMsPay(auditCage.getMsPay() == null ? 0 : (int) Math
				.round(auditCage.getMsPay() * paymentMultiplier));
		auditCage.setXGroupPay(auditCage.getXGroupPay() == null ? 0
				: (int) Math
						.round(auditCage.getXGroupPay() * paymentMultiplier));
		auditCage.setClassPay(auditCage.getClassPay() == null ? 0 : (int) Math
				.round(auditCage.getClassPay() * paymentMultiplier));
		auditCage.setDa(auditCage.getDa() == null ? 0 : (int) Math
				.round(auditCage.getDa() * paymentMultiplier));
		auditCage.setTransport(auditCage.getTransport() == null ? 0
				: (int) Math
						.round(auditCage.getTransport() * paymentMultiplier));
		auditCage.setPmha(auditCage.getPmha() == null ? 0 : (int) Math
				.round(auditCage.getPmha() * paymentMultiplier));
		auditCage.setLra(auditCage.getLra() == null ? 0 : (int) Math
				.round(auditCage.getLra() * paymentMultiplier));
	}

	static public int getMspRate(AuditCage dueAuditCage) {
		int mspay = 0;
		Employee employee = Utils.getCurrentEmployee(dueAuditCage.getDo2()
				.getArmyNo());

		List<PayScale> payList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(employee.getSubstantiveRank().getId(),employee.getGroupCategory().getSoldierGroup(),dueAuditCage.getDo2().getFromDate());
		 
		if (payList.size() > 0) {
			mspay = payList.get(0).getMsp();
		}
		return mspay;
	}
	static public int getXGroupRate(AuditCage dueAuditCage) {
		int xGroupPay = 0;
		Employee employee = Utils.getCurrentEmployee(dueAuditCage.getDo2()
				.getArmyNo());
		List<PayScale> payList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(employee.getSubstantiveRank().getId(),employee.getGroupCategory().getSoldierGroup(),dueAuditCage.getDo2().getFromDate());
			 
		if (payList.size() > 0) {
			xGroupPay = payList.get(0).getXGroupPay();
		}
		return xGroupPay;
	}
	static public int getPmhaRate(Do2 do2, AuditCage auditCage) {
		
		double pmhaAmount = auditCage.getPmha();
		String allowanceName = "PMHAL";
		Pao pao = null;
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			pao = Utils.getPao();
		}
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		int gsoCode = 1;
		if (employee.getGsoCode() != null){
			gsoCode = employee.getGsoCode();
			if (gsoCode == 3){
				gsoCode = 1;
			}
		}
		
		if (employee.getSubstantiveRank() != null && employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)) {
			return 0;
		}
		if (employee.getSubstantiveRank() != null && 
		   (employee.getSubstantiveRank().getRankCode().equals(Do2Manager.HONYCP) && !employee.getHonyCaptainDate().after(do2.getFromDate())) ||
		   (employee.getSubstantiveRank().getRankCode().equals(Do2Manager.HONYLT) && !employee.getHonyLieuDate().after(do2.getFromDate()))) {
			return 0;
		}
		List<Allowance> pmhaAlwList = null;
		List<AllowanceRate >allowanceRateList =alwRateRepo.findByAlwNameLikeAndRateFromdateLessThanEqual(pmhaAmount,auditCage.getFromDate(),"PMHA");
		 
		if (allowanceRateList.size()>0) {
			allowanceName = allowanceRateList.get(0).getAllowanceName();
		}
		
		if (pao !=null && pao.getPaoCode().equals(Do2Manager.PAO_DSC)) {
			pmhaAlwList = Utils.getApplicableAllowanceRates(allowanceName, do2.getFromDate(), do2.getFromDate(), gsoCode, pao);
			if (pmhaAlwList !=null && pmhaAlwList.size()>0) {
				return (int) pmhaAlwList.get(0).getRateAmount();
			} 
		} else {
			pmhaAlwList = Utils.getApplicableAllowanceRates(allowanceName, do2.getFromDate(), do2.getFromDate(), gsoCode, pao);
			if (pmhaAlwList !=null && pmhaAlwList.size()>0) {
				if (employee.getEnrolmentDate() == employee.getHavDate() || employee.getEnrolmentDate() == employee.getNbsubDate()) 
				    return (int) pmhaAlwList.get(0).getRateAmount();
				Date sepDate = null;
				if (employee.getAttestationDate() != null){
					sepDate = employee.getAttestationDate();
				}
				if (sepDate != null && employee.getInitialMusteringDate() != null && sepDate.after(employee.getInitialMusteringDate())){
					sepDate = employee.getInitialMusteringDate();
				}
				if (sepDate != null && do2.getFromDate().before(sepDate) && !do2.getFromDate().before(employee.getEnrolmentDate())){
					return (int) Math.round(pmhaAlwList.get(0).getRateAmount() * 3/4);
				}
				return (int) pmhaAlwList.get(0).getRateAmount();
			} 
		}
		
		return (int) pmhaAmount;
	
	}
	
/*	static public int getTransportRate(AuditCage newAuditCage,	AuditCage baseAuditCage) {
		Rank rank =Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getFromDate(),"ALLCE");
		int transportAmount = newAuditCage.getTransport();
		Employee employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		if (employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)) {
			return 0; 
		}
		List<Allowance> tptAlwList = null;
		String descnForTpt = null;
		String cityForTpt = null;
			
		List<Do2> tptlDo2List = Utils.getEntityManager().createQuery(
				"SELECT tl FROM Do2 AS tl WHERE "
						+ " tl.employee =:emp AND (tl.description='TPTL' OR tl.description='TPTH') AND "
						+ " tl.fromDate <=:frdt AND tl.columnA='GRANTED' AND tl.status='V' ORDER BY tl.fromDate DESC")
		.setParameter("emp", employee)
		.setParameter("frdt", newAuditCage.getFromDate())
		.getResultList();

		if (tptlDo2List.size() == 0)
			return transportAmount;

		cityForTpt = tptlDo2List.get(0).getColumnC();
		descnForTpt = tptlDo2List.get(0).getDescription();

		int gradePay = newAuditCage.getGradePay();
		int basicPay = newAuditCage.getBasicPay();

		if ((gradePay > 1999 && gradePay < 4200) && (basicPay >= 7440))
			gradePay = 4200;

		if (!(newAuditCage.getDo2().getFromDate().before(DateManager
				.getSixthCpcAllowanceDate()))) {
			   if(gradePay==0){
               	
               	tptAlwList = Utils.getApplicableAllowanceRates(descnForTpt, DateManager.getPreviousDate(DateManager.getSixthCpcAllowanceDate()),
   						DateManager.getPreviousDate(DateManager.getSixthCpcAllowanceDate()), rank,cityForTpt);
               }else{
			
			tptAlwList = Utils.getApplicableAllowanceRates(descnForTpt,
					newAuditCage.getFromDate(), newAuditCage.getFromDate(),
					gradePay, cityForTpt);
               }
		} else {
			tptAlwList = Utils.getApplicableAllowanceRates(descnForTpt,
					newAuditCage.getFromDate(), newAuditCage.getFromDate(),
					rank, cityForTpt);
		}
		if(tptAlwList.size()> 0 )
			return ((int) tptAlwList.get(0).getRateAmount());
		else 
			return transportAmount;
	}*/
	
	
	// commented above lines and amended as given below due to implementation of sevencpc
	
/*	static public int getTransportRate(AuditCage newAuditCage,	AuditCage baseAuditCage) {
		Rank rank =Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getFromDate(),"ALLCE");
		int transportAmount = newAuditCage.getTransport();
		Employee employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		if (employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)) {
			return 0; 
		}
		List<Allowance> tptAlwList = null;
		String descnForTpt = null;
		String cityForTpt = null;
			
		List<Do2> tptlDo2List = Utils.getEntityManager().createQuery(
				"SELECT tl FROM Do2 AS tl WHERE "
						+ " tl.employee =:emp AND (tl.description='TPTL' OR tl.description='TPTH') AND "
						+ " tl.fromDate <=:frdt AND tl.columnA='GRANTED' AND tl.status='V' ORDER BY tl.fromDate DESC")
		.setParameter("emp", employee)
		.setParameter("frdt", newAuditCage.getFromDate())
		.getResultList();

		if (tptlDo2List.size() == 0)
			return transportAmount;

		cityForTpt = tptlDo2List.get(0).getColumnC();
		descnForTpt = tptlDo2List.get(0).getDescription();

	//following lines added in view of seventh cpc.	
		int basicPay = 0;
		int gradePay = 0;
		if (!newAuditCage.getDo2().getFromDate().before(DateManager.getSeventhCpcDate())){
			// the below code amended in view of OPTCPC do2
			if (newAuditCage.getPayLevel().equals("0") && newAuditCage.getOldGradePay()==0) {
				gradePay = newAuditCage.getGradePay();
				basicPay = newAuditCage.getBasicPay();
			} else {
				gradePay = newAuditCage.getOldGradePay();
				basicPay = newAuditCage.getOldBasicPay();
			}
		} else {
			 basicPay = newAuditCage.getBasicPay();
			 gradePay = newAuditCage.getGradePay();
		}
		
	//	int gradePay = newAuditCage.getGradePay();
		
		if ((gradePay > 1999 && gradePay < 4200) && (basicPay >= 7440))
			gradePay = 4200;

		if (!(newAuditCage.getDo2().getFromDate().before(DateManager
				.getSixthCpcAllowanceDate()))) {
			   if(gradePay==0){
               	
               	tptAlwList = Utils.getApplicableAllowanceRates(descnForTpt, DateManager.getPreviousDate(DateManager.getSixthCpcAllowanceDate()),
   						DateManager.getPreviousDate(DateManager.getSixthCpcAllowanceDate()), rank,cityForTpt);
               }else{
			
			tptAlwList = Utils.getApplicableAllowanceRates(descnForTpt,
					newAuditCage.getFromDate(), newAuditCage.getFromDate(),
					gradePay, cityForTpt);
               }
		} else {
			tptAlwList = Utils.getApplicableAllowanceRates(descnForTpt,
					newAuditCage.getFromDate(), newAuditCage.getFromDate(),
					rank, cityForTpt);
		}
		if(tptAlwList.size()> 0 )
			return ((int) tptAlwList.get(0).getRateAmount());
		else 
			return transportAmount;
	}
*/
	
	static public int getTransportRate(AuditCage newAuditCage,	AuditCage baseAuditCage) {
		Rank rank =Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), newAuditCage.getFromDate(),"ALLCE");
		int transportAmount = newAuditCage.getTransport();
		Employee employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		if (employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)) {
			return 0; 
		}
		
		List<Allowance> tptAlwList = null;
		String descnForTpt = null;
		String cityForTpt = null;
		int MINIMUM_6CPC_TPTH_RATE = 1000;
			
		List<Do2> tptlDo2List = do2Repo.findBy2DescColAFromDateLessThanEqual(employee.getId(), Do2Manager.VALID, "TPTL", "TPTH", "GRANTED",newAuditCage.getFromDate());
		 
		if (tptlDo2List.size() == 0)
			return transportAmount;

		cityForTpt = tptlDo2List.get(0).getColumnC();
		descnForTpt = tptlDo2List.get(0).getDescription();

	//following lines added in view of seventh cpc.	
		int basicPay = 0;
		int gradePay = 0;
		
		// Below lines Commented for revision of 7cpc TPTL rate wef 01.07.2017
		
	/*	if (!newAuditCage.getDo2().getFromDate().before(DateManager.getSeventhCpcDate())){
			// the below code amended in view of OPTCPC do2
			if (newAuditCage.getPayLevel().equals("0") && newAuditCage.getOldGradePay()==0) {
				gradePay = newAuditCage.getGradePay();
				basicPay = newAuditCage.getBasicPay();
			} else {
				gradePay = newAuditCage.getOldGradePay();
				basicPay = newAuditCage.getOldBasicPay();
			}
		} else {
			 basicPay = newAuditCage.getBasicPay();
			 gradePay = newAuditCage.getGradePay();
		}
		
	//	int gradePay = newAuditCage.getGradePay();
		
		if ((gradePay > 1999 && gradePay < 4200) && (basicPay >= 7440))
			gradePay = 4200;

		if (!(newAuditCage.getDo2().getFromDate().before(DateManager.getSixthCpcAllowanceDate()))) {
			   if(gradePay==0){               	
               	tptAlwList = Utils.getApplicableAllowanceRates(descnForTpt, DateManager.getPreviousDate(DateManager.getSixthCpcAllowanceDate()),
   						DateManager.getPreviousDate(DateManager.getSixthCpcAllowanceDate()), rank,cityForTpt);
               }else{
			
			tptAlwList = Utils.getApplicableAllowanceRates(descnForTpt,
					newAuditCage.getFromDate(), newAuditCage.getFromDate(),
					gradePay, cityForTpt);
               }
		} else {
			tptAlwList = Utils.getApplicableAllowanceRates(descnForTpt,	newAuditCage.getFromDate(), newAuditCage.getFromDate(),
					rank, cityForTpt);
		}*/
		 
		List<Allowance> allowances = new ArrayList<Allowance>();
		
		if (newAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
			
				allowances = Utils.getApplicableAllowanceRates(descnForTpt, newAuditCage.getFromDate(),
															   newAuditCage.getFromDate(), rank, cityForTpt);
				if (allowances.size()>0) {
					transportAmount = (int) allowances.get(0).getRateAmount();
				}

		} else {
		 
			if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
				gradePay = newAuditCage.getGradePay();
				basicPay = newAuditCage.getBasicPay();
			} else  {
			 	if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
					if (newAuditCage.getPayLevel().equals("0") && newAuditCage.getGradePay()>0) {
						gradePay = newAuditCage.getGradePay();
						basicPay = newAuditCage.getBasicPay();
					} else {
						gradePay = newAuditCage.getOldGradePay();
						basicPay = newAuditCage.getOldBasicPay();
					}
				} else {
					gradePay = newAuditCage.getOldGradePay();
					basicPay = newAuditCage.getOldBasicPay();
				}
			}			

			if ((gradePay > 1999 && gradePay < 4200) && basicPay >= 7440) {
				if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate()) 
						&& newAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
					gradePay = 4200;	
				}
			}
							
			
            if (newAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate()) && gradePay==0) {
            	allowances = Utils.getApplicableAllowanceRates(descnForTpt, 
            			     DateManager.getPreviousDate(DateManager.getSixthCpcAllowanceDate()),
						     DateManager.getPreviousDate(DateManager.getSixthCpcAllowanceDate()), rank, cityForTpt);
            } else if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate()) && gradePay==0) {                	
            	allowances = Utils.getApplicableAllowanceRates(descnForTpt, 
       			     DateManager.getPreviousDate(DateManager.getSixthCpcAllowanceDate()),
					     DateManager.getPreviousDate(DateManager.getSixthCpcAllowanceDate()), rank, cityForTpt);
            } else if (gradePay>0 && !newAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate()) 
            			&& newAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
            	allowances = Utils.getApplicableAllowanceRates(descnForTpt, newAuditCage.getFromDate(),
                        newAuditCage.getFromDate(), gradePay, cityForTpt);
            } else if (!newAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
            	// grade pay is arrived based on pay_level in base auditcage after 01.07.2017 and sent as parameter
      /*         	allowances = Utils.getApplicableAllowanceRates(descnForTpt, baseAuditCage.getPayLevel(), newAuditCage.getFromDate(),
                        newAuditCage.getFromDate(), Do2Manager.getSevenCpcGradepay(baseAuditCage.getPayLevel()), cityForTpt);
                                               
  */     
              	String payLevel = null;
            	if (newAuditCage.getPayLevel()!=null) {
            		if (newAuditCage.getPayLevel().length()<3 && Integer.parseInt(newAuditCage.getPayLevel())<3) {
            			if (newAuditCage.getBasicPay()>14600) {
            				payLevel = "3";	
            			} else {
            				 return 0;
            			}                			
            		} else {
            			payLevel = newAuditCage.getPayLevel();
            		}                		 
            	} else if (newAuditCage.getBasicPay()>14600) {
            		payLevel = "3";
            	} else {
            		 return 0;
            	}
            	
            	if (payLevel.equals("9")) {
            		payLevel="10";
            	}
               	allowances = Utils.getApplicableAllowanceRates(descnForTpt, payLevel, newAuditCage.getFromDate(),
                        newAuditCage.getFromDate(), Do2Manager.getSevenCpcGradepay(payLevel), cityForTpt);
            	
         //   	System.out.println("yyyyyyyyyyyyyyyyyy  pay level ac helper  " +newAuditCage.getPayLevel() + "  "+allowances.size()+"  "+Do2Manager.getSevenCpcGradepay(payLevel));
            }
		
            if (allowances.size()>0) {
            	transportAmount = (int) allowances.get(0).getRateAmount();
			}

			if (gradePay>0 && !newAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate()) 
					&& newAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate()) 
					&& descnForTpt.equalsIgnoreCase(Do2Manager.TPTH) && (transportAmount < MINIMUM_6CPC_TPTH_RATE)) {
				transportAmount = MINIMUM_6CPC_TPTH_RATE;
			}			
		}		 
		return transportAmount;
	}
	static public double getLraRate(AuditCage dueAuditCage) {
		double lraRate = 0.0;
		Employee employee = Utils.getCurrentEmployee(dueAuditCage.getDo2().getArmyNo());
		if (employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)) {
			return lraRate;
		}
		Rank rank = Utils.getPborRank(employee.getArmyNo(),dueAuditCage.getFromDate(),"ALLCE");
		if (rank !=null &&(rank.getRankCode().equals(Do2Manager.HONYLT) || rank.getRankCode().equals(Do2Manager.HONYCP))){
			return lraRate;
		}
		List<Allowance> alwList = Utils.getApplicableAllowanceRates(
				Do2Manager.NRA, dueAuditCage.getFromDate(), dueAuditCage.getFromDate());
		employee = Utils.getCurrentEmployee(dueAuditCage.getDo2().getArmyNo());

		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			lraRate = (alwList.get(0).getRateAmount() * ((float) Do2Manager.LRA_DSC_LEAVE_DAYS / Do2Manager.NO_OF_MONTHS));
			return lraRate;
		} else {
			lraRate = (alwList.get(0).getRateAmount() * ((float) Do2Manager.LRA_ORS_LEAVE_DAYS / Do2Manager.NO_OF_MONTHS));	
		}
		if (employee.getEnrolmentDate() == employee.getHavDate() || employee.getEnrolmentDate() == employee.getNbsubDate()) 
		    return lraRate;
		Date sepDate = null;
		if (employee.getAttestationDate() != null){
			sepDate = employee.getAttestationDate();
		}
		if (sepDate != null && employee.getInitialMusteringDate() != null && sepDate.after(employee.getInitialMusteringDate())){
			sepDate = employee.getInitialMusteringDate();
		}
		if (sepDate != null && dueAuditCage.getDo2().getFromDate().before(sepDate) && !dueAuditCage.getDo2().getFromDate().before(employee.getEnrolmentDate())){
			lraRate=0;
			return lraRate;
		}
		return lraRate;
	}
	static public void regenerateCancelledIncrmentDo2s(Do2 do2) {
		Do2 incrDo2 = new Do2();
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		incrDo2.setEmployee(employee);
		incrDo2.setArmyNo(employee.getArmyNo());
		incrDo2.setCheckDigit(employee.getCheckDigit());
		incrDo2.setUnitCode(employee.getUnit().getUnitCode());
		incrDo2.setUnit(employee.getUnit());
		incrDo2.setDo2Group(do2.getDo2Group());
		incrDo2.setDo2Type(do2.getDo2Type());
		incrDo2.setDo2No(do2.getDo2No());
		incrDo2.setDo2ItemNo(do2.getDo2ItemNo());
		incrDo2.setDo2Date(DateManager.getToday());
		incrDo2.setDescription(do2.getDescription());
		incrDo2.setFromDate(do2.getFromDate());
		incrDo2.setColumnC(do2.getColumnC());
		incrDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		incrDo2.setAuditCage(true);
		incrDo2.setStatus(Do2Manager.PENDING);
		incrDo2.setQe(Utils.getCurrentQuarter());
		incrDo2.setAcceptedQe(Utils.getCurrentQuarter());
		incrDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		do2Repo.save(incrDo2);
	}

	static public void insertIncrmentDo2s(Do2 do2, Date fromDate, String columnA) {
		
		/*List<Do2> duplicateDo2List = Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE   " +
				" d.employee=:emp AND d.description='INCR' AND (d.status='V' or d.status='P') AND d.fromDate = :fromDate")
				.setParameter("emp", do2.getEmployee()).setParameter("fromDate", fromDate).getResultList();

		if (duplicateDo2List.size() > 0){
			return;
		}*/

		
		Do2 incrDo2 = new Do2();
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		incrDo2.setEmployee(employee);
		incrDo2.setArmyNo(employee.getArmyNo());
		incrDo2.setCheckDigit(employee.getCheckDigit());
		incrDo2.setUnitCode(employee.getUnit().getUnitCode());
		incrDo2.setUnit(employee.getUnit());
		incrDo2.setDo2Group(do2.getDo2Group());
		incrDo2.setDo2Type(do2.getDo2Type());
		incrDo2.setDo2No(do2.getDo2No());
		incrDo2.setDo2ItemNo(do2.getDo2ItemNo());
		incrDo2.setDo2Date(DateManager.getToday());
		incrDo2.setDescription("INCR");
		incrDo2.setFromDate(fromDate);
		
		if (do2.getDescription().equals("SIXCPC"))
			incrDo2.setColumnC("SIXCPC");
		else
			incrDo2.setColumnC(columnA);
		
		incrDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		incrDo2.setAuditCage(true);
		incrDo2.setStatus(Do2Manager.PENDING);
		incrDo2.setQe(Utils.getCurrentQuarter());
		incrDo2.setAcceptedQe(Utils.getCurrentQuarter());
		incrDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		do2Repo.save(incrDo2);
	}

	static public void generateIncrCancelDo2s(Do2 do2) {
		Do2 incrDo2 = new Do2();
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		incrDo2.setEmployee(employee);
		incrDo2.setArmyNo(employee.getArmyNo());
		incrDo2.setCheckDigit(employee.getCheckDigit());
		incrDo2.setUnitCode(employee.getUnit().getUnitCode());
		incrDo2.setUnit(employee.getUnit());
		incrDo2.setDo2Group(do2.getDo2Group());
		incrDo2.setDo2Type("0");
		incrDo2.setDo2No("6666");
		incrDo2.setDo2ItemNo("666");
		incrDo2.setDo2Date(DateManager.getToday());
		incrDo2.setDescription(Do2Manager.CANCEL);
		incrDo2.setFromDate(do2.getFromDate());
		incrDo2.setColumnA(do2.getDescription());
		incrDo2.setColumnC(do2.getColumnC());
		incrDo2.setRefDo2No(Utils.getReferenceDo2No(do2));
		incrDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		incrDo2.setAuditCage(true);
		incrDo2.setStatus(Do2Manager.PENDING);
		incrDo2.setQe(Utils.getCurrentQuarter());
		incrDo2.setAcceptedQe(Utils.getCurrentQuarter());
		incrDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		do2Repo.save(incrDo2);
	}

	static public void generateForGspayDo2(Do2 do2, String columnA) {
		Do2 genDo2 = new Do2();
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		genDo2.setEmployee(employee);
		genDo2.setArmyNo(employee.getArmyNo());
		genDo2.setCheckDigit(employee.getCheckDigit());
		genDo2.setUnitCode(employee.getUnit().getUnitCode());
		genDo2.setUnit(employee.getUnit());
		genDo2.setDo2Group(do2.getDo2Group());
		genDo2.setDo2Type(do2.getDo2Type());
		genDo2.setDo2No(do2.getDo2No());
		genDo2.setDo2ItemNo(do2.getDo2ItemNo());
		genDo2.setDo2Date(do2.getDo2Date());
		genDo2.setDescription("FORGSP");
		genDo2.setColumnA(columnA);
		genDo2.setPunishment(do2.getPunishment());
		genDo2.setFromDate(do2.getFromDate());
		genDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		genDo2.setRefDo2No(Utils.getReferenceDo2No(do2));
		genDo2.setAuditCage(true);
		genDo2.setStatus(Do2Manager.PENDING);
		genDo2.setQe(Utils.getCurrentQuarter());
		genDo2.setAcceptedQe(Utils.getCurrentQuarter());
		genDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		do2Repo.save(genDo2);
		 
	}

	static public void generateReduceDo2(Do2 do2, Date fromDate) {
		Do2 rmilriDo2 = new Do2();
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		rmilriDo2.setEmployee(employee);
		rmilriDo2.setArmyNo(employee.getArmyNo());
		rmilriDo2.setCheckDigit(employee.getCheckDigit());
		rmilriDo2.setUnitCode(employee.getUnit().getUnitCode());
		rmilriDo2.setUnit(employee.getUnit());
		rmilriDo2.setDo2Group(do2.getDo2Group());
		rmilriDo2.setDo2Type(do2.getDo2Type());
		rmilriDo2.setDo2No(do2.getDo2No());
		rmilriDo2.setDo2ItemNo(do2.getDo2ItemNo());
		rmilriDo2.setDo2Date(do2.getDo2Date());
		rmilriDo2.setDescription("REDUCE");
		rmilriDo2.setPunishment(do2.getPunishment());
		rmilriDo2.setFromDate(fromDate);
		rmilriDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		rmilriDo2.setRefDo2No(Utils.getReferenceDo2No(do2));
		rmilriDo2.setAuditCage(true);
		rmilriDo2.setStatus(Do2Manager.PENDING);
		rmilriDo2.setQe(Utils.getCurrentQuarter());
		rmilriDo2.setAcceptedQe(Utils.getCurrentQuarter());
		rmilriDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		do2Repo.save(rmilriDo2);
	}

	static public AuditCage getPreviousDateAuditCage(Date fromDate, Do2 do2) {
		 
		List<AuditCage> atCageList = acRepo.findByFromDateLessThanAndDpZeroAndGpNotZero(do2.getEmployee().getId(), fromDate);
			 
		if (atCageList.size()>0) {
			return atCageList.get(0);	
		} else {
			return null;
		}
	}
	
	
	/*static public AuditCage getPreviousDateAuditCage5cpc(Date fromDate, Do2 do2) {
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> atCageList = Utils.getEntityManager().createQuery("SELECT ac FROM AuditCage ac where ac.employee=:employee AND ac.cancelled=false AND  " +
						" ac.effective=true AND ac.recordStatus!='P' AND ac.fromDate <:fromDate AND ac.dp=0  " +
						" order by ac.fromDate desc, ac.toDate desc").setParameter("employee", employee).setParameter("fromDate", fromDate).getResultList();

		if (atCageList.size()>0) {
			return atCageList.get(0);	
		} else {
			return null;
		}
	}*/
	// Above method commented and modified on 14.02.2019 for CMP fs case
	static public AuditCage getPreviousDateAuditCage5cpc(Date fromDate, Do2 do2) {
		 
		List<AuditCage> atCageList =acRepo.findByFromDateLessThan(do2.getEmployee().getId(), fromDate);
					 
		if (atCageList.size()>0) {
			return atCageList.get(0);	
		} else {
			return null;
		}
	}
	
	static public AuditCage getPreviousDateAuditCageFor6PcOldPay(Date fromDate, Do2 do2) {
	 
		List<AuditCage> atCageList =acRepo.findByFromDateLessThan(do2.getEmployee().getId(), fromDate);
			 
		if (atCageList.size()>0) {
			return atCageList.get(0);	
		} else {
			return null;
		}
	}
	static public AuditCage getPreviousDateAuditCageIn7cpc(Date fromDate, Do2 do2) {
		 
		List<AuditCage> atCageList = acRepo.findByFromDateLessThanAndDpZeroAndGpZeroAndPlNotZero(do2.getEmployee().getId(), fromDate);
				 
		if (atCageList!=null && atCageList.size()>0) {
			return atCageList.get(0);	
		} else {
			return null;
		}
	}
/*	
	static public AuditCage getRemustAuditCage(Date fromDate, Do2 do2) {
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> atCageList = Utils
				.getEntityManager()
				.createQuery(
						"SELECT ac FROM AuditCage ac where ac.employee=:employee AND ac.cancelled=false AND  " +
						" ac.effective=true AND ac.recordStatus!='P' AND ac.fromDate <=:fromDate  AND ac.dp=0 AND ac.gradePay!=0 " +
						" order by ac.fromDate desc, ac.toDate desc ")
				.setParameter("employee", employee).setParameter("fromDate", fromDate)
				.getResultList();

		if (atCageList.size()>0) {
			return atCageList.get(0);	
		} else {
			return null;
		}
	}
*/	
	
	
	static public AuditCage getRemustAuditCage(Date fromDate, Do2 do2) {
		 
		Date sixCpcFromDate =  sixcpcDo2Exists(do2);
		List<AuditCage> atCageList =acRepo.findByFromDateLessThanEqualAndDpZeroAndGpNotZeroMoreBasic(do2.getEmployee().getId(), fromDate);
	
		if (atCageList.size()>0  ) {
			for (AuditCage remustac: atCageList) {
				if (remustac.getDo2().getDescription().equals("REMUST") ||remustac.getDo2().getDescription().equals("MSTJCO")) {
					return remustac;
				}
			}
	
		} else {
			if (fromDate.before(sixCpcFromDate)) {
				List<AuditCage> atCageList1 = acRepo.findByFromDateLessThanEqualAndDpZeroAndGpNotZero(do2.getEmployee().getId(), sixCpcFromDate);
						 
				if (atCageList1.size()>0) {
					return atCageList1.get(0);	
				}
			}
			
		}
				
		return null;
	}
	
	

public static Date sixcpcDo2Exists(Do2 do2){
		List<Do2> do2List =do2Repo.findByDescStatus(do2.getEmployee().getId(), Do2Manager.VALID,"SIXCPC");
			 
	    if (do2List.size()>0){
	        	return do2List.get(0).getFromDate();
	    }
	    return DateManager.getSixthCpcDate();
	} 
	
	
	static public AuditCage getBaseAuditCage(Date fromDate, Do2 do2) {
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		List<AuditCage> atCageList = acRepo.findByFromDateLessThanEqualOrderByFromDateDescToDateDescBpDesc(employee.getId(), fromDate);
			 
        if (atCageList.size() > 0)
        	return atCageList.get(0);
        return null;
	}


	public static void generateTptlGrantDo2(Do2 do2, Date grantDate) {
		Do2 tptlGrantDo2 = new Do2();
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		tptlGrantDo2.setEmployee(employee);
		tptlGrantDo2.setArmyNo(employee.getArmyNo());
		tptlGrantDo2.setCheckDigit(employee.getCheckDigit());
		tptlGrantDo2.setUnitCode(do2.getUnitCode());
		tptlGrantDo2.setUnit(do2.getUnit());
		tptlGrantDo2.setDo2Group("IA");
		tptlGrantDo2.setDo2Type(do2.getDo2Type());
		tptlGrantDo2.setDo2No(do2.getDo2No());
		tptlGrantDo2.setDo2ItemNo(do2.getDo2ItemNo());
		tptlGrantDo2.setDo2Date(do2.getDo2Date());
		tptlGrantDo2.setDescription("TPTL");
		tptlGrantDo2.setFromDate(grantDate);
		tptlGrantDo2.setColumnA("GRANTED");
		tptlGrantDo2.setColumnC(do2.getColumnC());
		tptlGrantDo2.setRate(do2.getRate());
		tptlGrantDo2.setStatus(Do2Manager.PENDING);
		tptlGrantDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		tptlGrantDo2.setAuditCage(true);
		tptlGrantDo2.setQe(Utils.getCurrentQuarter());
		tptlGrantDo2.setAcceptedQe(Utils.getCurrentQuarter());
		tptlGrantDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		do2Repo.save(tptlGrantDo2);
	}

	/**
	 * if Do2 involves only AuditCage Elements and allowances which do not have
	 * link to mutual inclusive list
	 * 
	 * @param newAuditCage
	 * @param baseAuditCage
	 */
    
	static public void copyCurrentAllowances(AuditCage newAuditCage, AuditCage baseAuditCage) { 
 
		List<AuditCageAllowance> newAuditCageAllowanceList = acaRepo.findAllByAuditCageId(newAuditCage.getId());         
		List<AuditCageAllowance> currentAuditCageAllowanceList = acaRepo.findAllByAuditCageId(baseAuditCage.getId());		
			 
		HashMap<String, AuditCageAllowance> newAllowanceMap = new HashMap<String, AuditCageAllowance>();
		HashMap<String, AuditCageAllowance> currentAllowanceMap = new HashMap<String, AuditCageAllowance>();
		
		for (AuditCageAllowance aca : newAuditCageAllowanceList) {
			aca.setAuditCage(newAuditCage);
			newAllowanceMap.put(aca.getPayCode().getPayCode() + aca.getAmount(), aca);
		}

		for (AuditCageAllowance aca : currentAuditCageAllowanceList) {
			aca.setAuditCage(baseAuditCage);
			currentAllowanceMap.put(aca.getPayCode().getPayCode() + aca.getAmount(), aca);
		}
		
		for (AuditCageAllowance aca : currentAuditCageAllowanceList) {
			AuditCageAllowance newAca = newAllowanceMap.get(aca.getPayCode().getPayCode() + aca.getAmount());	
			if (newAca != null) {
				newAca.setDo2(aca.getDo2());				
				int alwAmount = getRevisedAllowanceRate(aca, newAuditCage);
				if (alwAmount != aca.getAmount()) {
					acaRepo.delete(newAca);
					
					newAuditCage.getAuditCageAllowances().remove(newAca); 
					newAllowanceMap.remove(aca.getPayCode().getPayCode() + aca.getAmount());
				} else {
					continue;
				}
			}			
			
			int alwAmount = getRevisedAllowanceRate(aca, newAuditCage);
			AuditCageAllowance allowanceNewRecord = new AuditCageAllowance();
			allowanceNewRecord.setAuditCage(newAuditCage);
			allowanceNewRecord.setPayCode(aca.getPayCode());
			allowanceNewRecord.setDo2(aca.getDo2());
			allowanceNewRecord.setCreatedAt(new Timestamp(System.currentTimeMillis()));

			allowanceNewRecord.setAmount(alwAmount);
			acaRepo.save(allowanceNewRecord);
			 
			newAllowanceMap.put(aca.getPayCode().getPayCode() + alwAmount, allowanceNewRecord);
		}

		for (AuditCageAllowance aca : newAuditCageAllowanceList) {
	//		System.out.println("...............aca.id " +aca.getId()+"  amount "+aca.getAmount());
			if (currentAllowanceMap.get(aca.getPayCode().getPayCode() + aca.getAmount()) != null) {
				if (aca.getPayCode().getPayCode().equals("RISK")) {				
					RhLocation rhl = null;
					rhl=rhlRepo.getRhLocationList(aca.getDo2().getColumnC(), newAuditCage.getFromDate()); 
	//				System.out.println("......rhl.........."+rhl.getLocationCode()+" frdt "+rhl.getFromDate());
					if (rhl==null || (rhl!=null && rhl.getRhCode()!=null && rhl.getRhCode().equals("NA"))) { 
	//					System.out.println(".......risk .......removing aca id " +aca.getId());
						newAuditCage.getAuditCageAllowances().remove(aca);
						acaRepo.delete(aca);	
					} else if (rhl!=null && !rhl.getRhCode().trim().toUpperCase().equals(aca.getDo2().getColumnB().trim().toUpperCase())) {
	//					System.out.println(".......risk 111.......removing aca id " +aca.getId());
						newAuditCage.getAuditCageAllowances().remove(aca);
						acaRepo.delete(aca);
					} else if (rhl!=null && aca.getDo2().getRate()>rhl.getAdmissiblePercentage()) {
	//					System.out.println(".......risk 111222.......removing aca id " +aca.getId());
						newAuditCage.getAuditCageAllowances().remove(aca);
						acaRepo.delete(aca);
					}
					continue;
				}
				if (aca.getPayCode().getPayCode().equals("HAUCA")) {
					RhLocation rhl = null;
					rhl=rhlRepo.getRhLocationList(aca.getDo2().getColumnC(), newAuditCage.getFromDate()); 
					if (rhl==null || (rhl!=null && rhl.getHhCode()!=null && rhl.getHhCode().equals("NA"))) { 
	//					System.out.println(".......hauca .......removing aca id " +aca.getId());
						newAuditCage.getAuditCageAllowances().remove(aca);
						acaRepo.delete(aca);	
					}  else if (rhl!=null && !rhl.getHhCode().trim().toUpperCase().equals(aca.getDo2().getColumnB().trim().toUpperCase())) {
	//					System.out.println(".......hauca 111.......removing aca id " +aca.getId());
						newAuditCage.getAuditCageAllowances().remove(aca);
						acaRepo.delete(aca);
					} else if (rhl!=null && aca.getDo2().getRate()!=null && aca.getDo2().getRate()>rhl.getAdmissiblePercentage()) {
	// 					System.out.println(".......hauca 111222.......removing aca id " +aca.getId());
						newAuditCage.getAuditCageAllowances().remove(aca);
						acaRepo.delete(aca);
					}
					continue;
				}
				continue;
			}
			newAuditCage.getAuditCageAllowances().remove(aca);
			acaRepo.delete(aca);			 
		}	   
	}

	static long totalTime = 0;

	static public void copyCurrentAllowancesRevised(AuditCage newAuditCage,
			AuditCage baseAuditCage) {
		HashMap<String, AuditCageAllowance> newAllowanceMap = new HashMap<String, AuditCageAllowance>();
		 HashMap<String, AuditCageAllowance> currentAllowanceMap = new HashMap<String, AuditCageAllowance>();
		List<AuditCageAllowance> newAuditCageAllowanceList =acaRepo.findAllByAuditCageId(newAuditCage.getId());
			 

		List<AuditCageAllowance> currentAuditCageAllowanceList = acaRepo.findAllByAuditCageId(baseAuditCage.getId());
			 
		 

		for (AuditCageAllowance aca : newAuditCageAllowanceList) {
			aca.setAuditCage(newAuditCage);
			newAllowanceMap.put(aca.getPayCode() + " " + aca.getAmount(), aca);
		}

		for (AuditCageAllowance aca : currentAuditCageAllowanceList) {
			aca.setAuditCage(baseAuditCage);
			currentAllowanceMap.put(aca.getPayCode() + " " + aca.getAmount(),aca);
		}

		for (AuditCageAllowance aca : currentAuditCageAllowanceList) {
			AuditCageAllowance newAca = newAllowanceMap.get(aca.getPayCode()
					+ " " + aca.getAmount());

			if (newAca != null) {
				newAca.setDo2(aca.getDo2());
				int alwAmount = getRevisedAllowanceRate(aca, newAuditCage);
				if (alwAmount != aca.getAmount()) {
					acaRepo.delete(newAca);
					 
					newAllowanceMap.remove(aca.getPayCode() + " "
							+ aca.getAmount());
				} else {
					continue;
				}
			}

			int alwAmount = getRevisedAllowanceRate(aca, newAuditCage);
			AuditCageAllowance allowanceNewRecord = new AuditCageAllowance();
			allowanceNewRecord.setAuditCage(newAuditCage);
			allowanceNewRecord.setPayCode(aca.getPayCode());
			allowanceNewRecord.setDo2(aca.getDo2());
			allowanceNewRecord.setAmount(alwAmount);
			allowanceNewRecord.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			acaRepo.save(allowanceNewRecord);
			 
			newAllowanceMap.put(aca.getPayCode() + " " + alwAmount,
					allowanceNewRecord);
		}

		for (AuditCageAllowance aca : newAuditCageAllowanceList) {
			if (currentAllowanceMap.get(aca.getPayCode() + " "
					+ aca.getAmount()) != null) {
				continue;
			}
			acaRepo.delete(aca);
			 
		}
		 

	}
	static public void reverseUpdateESDForPromotCancel(Do2 refDo2, char status) {
	 	List<EmployeeServiceDetail> employeeServiceDetailList = Utils.getEmployeeServiceDetailsForPromotCancel(refDo2, status);
		EmployeeServiceDetail base = null;
		EmployeeServiceDetail follow = null;
		EmployeeServiceDetail current = null;
		int listSize = employeeServiceDetailList.size();
		for (int i = 0; i < listSize; i++) {
			current = employeeServiceDetailList.get(i);
	 		if (current.getDo2().getId()==refDo2.getId()) {
	 			esdRepo.delete(current);
	 			if (i>0) {
	 				base = employeeServiceDetailList.get(i - 1);
	 			}
	 			if (i == (listSize - 1)) {
					base.setToDate(null);
				} else {
					follow = employeeServiceDetailList.get(i + 1);
					base.setToDate(DateManager.getPreviousDate(follow.getFromDate()));
				}
			}
		}
		 
	}
	static public Date getFieldAllowanceCeaseDateOnTD(Do2 do2, Employee employee) {
		
		List<Do2> fieldAllowanceCeaseDo2List  =do2Repo.findByDescListColAColCOrderByFromDate(employee.getId(),Do2Manager.PENDING,Do2Manager.STOP_FIELD_ALLOWANCE_DESCRIPTIONS_ON_TEMPORARY_DUTY_TWENTYONE_DAYS ,do2.getFromDate(),"CEASED","TD");
		 
		if (fieldAllowanceCeaseDo2List.size()>0) {
			return fieldAllowanceCeaseDo2List.get(0).getFromDate();
		} else {
			return do2.getFromDate();
		}		
	}
	
	
	static public AuditCage getTfrdepAuditCage(Do2 do2){
		   AuditCage deputationAuditCage=null;
		   List<Do2> do2List = do2Repo.findByDescColAFromDateLessThanEqual(do2.getEmployee().getId(),Do2Manager.VALID, "TFRDEP","IMTRAT",do2.getFromDate());
						   
		   if (do2List.size() >0){
			   deputationAuditCage = Utils.getAuditCage(do2List.get(0));
		   } else {
			   //LINES ADDED FOR DANTAK
			   List<Do2> do2List1 = do2Repo.findByDescColAFromDateLessThanEqual(do2.getEmployee().getId(),Do2Manager.VALID, "POSIN","DANTAK",do2.getFromDate());
					   
					   
			   if(do2List1.size()>0){
				   deputationAuditCage = Utils.getAuditCage(do2List1.get(0));
			   }
			   
		   }
		   return deputationAuditCage;
	}
	
	// this method to be activated after receipt of order for bhutan revision
	
/*	static public int getBhutanRate(Do2 do2){
		AuditCage depuAuditCage = getTfrdepAuditCage(do2);
		int basicPay=0;
		int bhutanSlabRate=0;
		int allowBhutanRate=0;
		int bhutanAmount=0;
		int economyCut=0;
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		Rank rank = Utils.getPborRank(do2.getArmyNo(), depuAuditCage.getFromDate(),"PAYFIX");
		GroupCategory groupCategory = Utils.getGroupCategory(depuAuditCage.getDo2(), "CUR");
		char group = groupCategory.getSoldierGroup();
		List<Allowance> allowances=null;
		if (!do2.getFromDate().before(DateManager.getSixthCpcDate())){
			basicPay = depuAuditCage.getOldBasicPay();
	  	} else {
	  	 	basicPay = depuAuditCage.getBasicPay();
	  	}
		if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
			List<BhutanSlab> bhutanSlabList = Utils.getEntityManager().createQuery(
				"SELECT bts FROM BhutanSlab bts WHERE bts.rank=:rnk AND bts.soldierGroup.soldierGroup=:grp " +
				"AND bts.basicPay=:basicPay")
					.setParameter("rnk", rank)
					.setParameter("grp",group)
					.setParameter("basicPay", basicPay)
					.getResultList();
		 	if (bhutanSlabList.size() > 0){
		 		bhutanSlabRate = bhutanSlabList.get(0).getSlabRate();	 		
		 	}
		} else {
				Auth : CGDA letter No.AT/I/O/74/IV dated 30.09.2014 intimating  removal of slab deduction
		       from BCA with effect from 1.9.2008 (GOI MOD lr No.4(3)/2011/D(Pay/Services) dt.5.9.2014)
			bhutanSlabRate = 0;
		}
	 	
	 	if (do2.getFromDate().before(DateManager.getBhutanRevisionDate())){
	 		allowances = Utils.getApplicableAllowanceRates("BHUTAN",do2.getFromDate(), do2.getFromDate(),basicPay);
	 	} else {
		 // Auth : CGDA letter No.AT/I/2474/BCA dated 25.02.2014. Amended in 04/2014.
		 	rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
	 	//	int grpay = depuAuditCage.getGradePay();
	 		int grpay = Utils.getAuditCage(do2).getGradePay();
		    allowances = Utils.getApplicableAllowanceRates(do2.getFromDate(),do2.getFromDate(),"BHUTAN",grpay);
	 	}
	 	if (allowances.size()> 0){ 
	 		allowBhutanRate = (int)allowances.get(0).getRateAmount();
	 	}
	 	if (depuAuditCage.getDeputationStatus() !=null && depuAuditCage.getDeputationStatus()!='K' ){
	 		economyCut = (allowBhutanRate*5)/100;
	 	}
	 	int freeFacility  = ((allowBhutanRate-bhutanSlabRate-economyCut)*4)/100;
	 	int netBhutanRate = (((allowBhutanRate - bhutanSlabRate) - economyCut) - freeFacility) + economyCut;
	 	return netBhutanRate;
	}*/
	
	// commented above lines and amended as given below due to implementation of sevencpc
	
	static public int getBhutanRate(Do2 do2){
		AuditCage depuAuditCage = getTfrdepAuditCage(do2);
		AuditCage atcg = null;
		int basicPay=0;
		int bhutanSlabRate=0;
		int allowBhutanRate=0;
		int bhutanAmount=0;
		int economyCut=0;
		int grpay = 0;
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		Rank rank = Utils.getPborRank(do2.getArmyNo(), depuAuditCage.getFromDate(),"PAYFIX");
		GroupCategory groupCategory = Utils.getGroupCategory(depuAuditCage.getDo2(), "CUR");
		String group = groupCategory.getSoldierGroup();
		List<Allowance> allowances=null;
		if (!do2.getFromDate().before(DateManager.getSixthCpcDate())){
			basicPay = depuAuditCage.getOldBasicPay();
	  	} else {
	  	 	basicPay = depuAuditCage.getBasicPay();
	  	}
		
		List<AuditCage> auditCageList =acRepo.findByFromDateLessThanEqualValidOrderByFromDateDesc(do2.getEmployee().getId(),do2.getFromDate());
		 

		if (auditCageList.size() > 0) {
			atcg = auditCageList.get(0);
		}
		
		if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())){
			List<BhutanSlab> bhutanSlabList = bsRepo.findByRankIdSgAndBp(rank.getId(), group, basicPay);
					
			 
		 	if (bhutanSlabList.size() > 0){
		 		bhutanSlabRate = bhutanSlabList.get(0).getSlabRate();	 		
		 	}
		} else {
			/*	Auth : CGDA letter No.AT/I/O/74/IV dated 30.09.2014 intimating  removal of slab deduction
		       from BCA with effect from 1.9.2008 (GOI MOD lr No.4(3)/2011/D(Pay/Services) dt.5.9.2014)*/
			bhutanSlabRate = 0;
		}
	 	
	 	if (do2.getFromDate().before(DateManager.getBhutanRevisionDate())){ // before 01-04-2010
	 		allowances = Utils.getApplicableAllowanceRates("BHUTAN",do2.getFromDate(), do2.getFromDate(),basicPay);
	 	} else {
		 // Auth : CGDA letter No.AT/I/2474/BCA dated 25.02.2014. Amended in 04/2014.
		 	rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
	 	//	int grpay = depuAuditCage.getGradePay();
		 	
		 	
		 	
		 /*	if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
		 		grpay = atcg.getGradePay();
		 	} else {
		 		grpay = atcg.getOldGradePay();
		 	}
	 		*/


		 	if(do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
				 		grpay = atcg.getGradePay();
				 	} else {
				 		if (atcg.getPayLevel().equals("0")) {
				 			grpay = atcg.getGradePay();
				 		} else {
				 			grpay = atcg.getOldGradePay();
				 		}
				 	}

		 	/*
		 	if(!do2.getFromDate().after(DateManager.getSeventhCpcDate())) {
				 		grpay = atcg.getGradePay();
				 	} else {
				 		if (atcg.getPayLevel().equals("0")) {
				 			grpay = atcg.getGradePay();
				 		} else {
				 			grpay = atcg.getOldGradePay();
				 		}
				 	}*/

		 	
		 	
		    allowances = Utils.getApplicableAllowanceRates(do2.getFromDate(),do2.getFromDate(),"BHUTAN",grpay);
	 	}
	 	if (allowances.size()> 0){ 
	 		allowBhutanRate = (int)allowances.get(0).getRateAmount();
	 	}
	 	if (depuAuditCage.getDeputationStatus() !=null && !depuAuditCage.getDeputationStatus().equals("K")){
	 		economyCut = (allowBhutanRate*5)/100;
	 	}
	 	int freeFacility  = ((allowBhutanRate-bhutanSlabRate-economyCut)*4)/100;
	 	// Following two lines added due to 
	 	// dispense with recovery of depression of 4% for PBORS from BCA payable to 
	 	// IMTRAT personel in lieu of free facilities.
	 	// AUTHORITY  GOI, MOD NO. PC-1(9)2000/D(Pay/Services), dated 22nd March 2019
	 	// Forwarded vide  HQRS letter No. AT/1/3474/VI dated 09-04-2019 
	 	if (!do2.getFromDate().before(DateManager.getBhutanDepressionRemovalDate())) {
	 		freeFacility = 0;
	 	}
	 	int netBhutanRate = (((allowBhutanRate - bhutanSlabRate) - economyCut) - freeFacility) + economyCut;
	 	return netBhutanRate;
	} 
	
	
/*public static int getFullMonthDaWithoutTransport(Do2 do2,AuditCage dueAuditCage) {				
		
		List<Allowance> allowanceList = Utils.getApplicableAllowanceRates("DA",
				dueAuditCage.getFromDate(), dueAuditCage.getFromDate());

		double da = 0.0;

		int variableComponents = 0;
		int xGroupPay = dueAuditCage.getXGroupPay() == null ? 0 : dueAuditCage
				.getXGroupPay();
		int gradePay = dueAuditCage.getGradePay() == null ? 0 : dueAuditCage
				.getGradePay();
		int dp = dueAuditCage.getDp() == null ? 0 : dueAuditCage.getDp();
		int classPay = dueAuditCage.getClassPay() == null ? 0 : dueAuditCage
				.getClassPay();
		int msPay = dueAuditCage.getMsPay() == null ? 0 : dueAuditCage
				.getMsPay();
		
		double daRate = 0.0;
		for (Allowance allowance : allowanceList) {
			daRate = allowance.getRatePercentage();
			if (!dueAuditCage.getFromDate().before(
					DateManager.getSixthCpcDate())
					&& dp != 0) {
				daRate = allowance.getRateAmount();
			}
			if (dueAuditCage.getFromDate().before(DateManager.getDPDate())) {
				variableComponents = classPay;
			} else if (dueAuditCage.getFromDate().before(
					DateManager.getSixthCpcDate())
					|| (!dueAuditCage.getFromDate().before(
							DateManager.getSixthCpcDate()) && dp != 0)) {
				variableComponents = dp;
			} else if (dueAuditCage.getFromDate().before(
					DateManager.getSixthCpcAllowanceDate())) {
				variableComponents = gradePay + xGroupPay;
			} else {
				variableComponents = gradePay + msPay + xGroupPay;
			}
			da += (dueAuditCage.getBasicPay() + variableComponents) * daRate;
		}
		return ((int) Math.round(da));
	}
*/
	

public static int getFullMonthDaWithoutTransport(Do2 do2,AuditCage dueAuditCage) {			
		
		Date sevencpcDate = DateManager.getSeventhCpcDate();
		List<Do2> do2List = do2Repo.findByDescColB(do2.getEmployee().getId(),Do2Manager.VALID,"OPTCPC","SEVENPC");
		 
		if (do2List.size()>0){
		   	sevencpcDate = do2List.get(0).getFromDate();
		}

		List<Allowance> allowanceList = Utils.getApplicableAllowanceRates("DA", dueAuditCage.getFromDate(), dueAuditCage.getFromDate());
		
		double da = 0.0;
		int variableComponents = 0;
		int basicPay = dueAuditCage.getBasicPay() == null ? 0 : dueAuditCage.getBasicPay();
		int xGroupPay = dueAuditCage.getXGroupPay() == null ? 0 : dueAuditCage.getXGroupPay();
		int gradePay = dueAuditCage.getGradePay() == null ? 0 : dueAuditCage.getGradePay();
		int dp = dueAuditCage.getDp() == null ? 0 : dueAuditCage.getDp();
		int classPay = dueAuditCage.getClassPay() == null ? 0 : dueAuditCage.getClassPay();
		int msPay = dueAuditCage.getMsPay() == null ? 0 : dueAuditCage.getMsPay();
		int oldGradePay = dueAuditCage.getOldGradePay() == null ? 0 : dueAuditCage.getOldGradePay();
		double daRate = 0.0;
		for (Allowance allowance : allowanceList) {
			daRate = allowance.getRatePercentage();
		/*	if (!dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) && dp != 0) {
				daRate = allowance.getRateAmount();
			} else if (dueAuditCage.getFromDate().before(sevencpcDate )  && 
					dueAuditCage.getPayLevel().equals("0") && !sevencpcDate.equals(DateManager.getSeventhCpcDate())) {
				daRate = allowance.getRateAmount();
			} else if (!dueAuditCage.getFromDate().before (DateManager.getSeventhCpcDate()) && dueAuditCage.getGradePay()!=0) {
				basicPay = dueAuditCage.getOldBasicPay();
				msPay = Utils.get6cpcMsPay(dueAuditCage);
				if (dueAuditCage.getXGroupPay()!=0)
					xGroupPay = Utils.get6cpcXGroupPay(dueAuditCage);				
				variableComponents = oldGradePay + msPay + xGroupPay;
			}
			if (dueAuditCage.getFromDate().before(DateManager.getDPDate())) {
				variableComponents = classPay;
			} else if (dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate())
					|| (!dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) && dp != 0)) {
				variableComponents = dp;
			} else if (dueAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				variableComponents = gradePay + xGroupPay;
			} else if (dueAuditCage.getFromDate().before(sevencpcDate)){
				variableComponents = gradePay + msPay + xGroupPay;
			} else if (dueAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())){
				basicPay = dueAuditCage.getOldBasicPay();
			 	msPay = Utils.get6cpcMsPay(dueAuditCage);
				if (dueAuditCage.getXGroupPay()!=0)
					xGroupPay = Utils.get6cpcXGroupPay(dueAuditCage);
				daRate = 1.25;	
				variableComponents = oldGradePay + msPay + xGroupPay;
			} else {
				variableComponents =  msPay + xGroupPay;
			} */
			
			if (!dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) && dp != 0) {
				daRate = allowance.getRateAmount();
			} else if (!sevencpcDate.equals(DateManager.getSeventhCpcDate()) 
					&& dueAuditCage.getFromDate().before(sevencpcDate) 
					&& !dueAuditCage.getFromDate().before(DateManager.getSeventhCpcDate()) 
					&& dueAuditCage.getPayLevel().equals("0")) {
				daRate = allowance.getRateAmount();
			} 
			
			if (dueAuditCage.getFromDate().before(DateManager.getDPDate())) {
				variableComponents = classPay;
			} else if (dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate())
					|| (!dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate()) && dp != 0)) {
				variableComponents = dp;
			} else if (!dueAuditCage.getFromDate().before(DateManager.getSixthCpcDate())
						&& dueAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				variableComponents = gradePay + xGroupPay;
			} else if (!dueAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())
						&& dueAuditCage.getFromDate().before(sevencpcDate)) {
				variableComponents = gradePay + msPay + xGroupPay;
			} else if (!dueAuditCage.getFromDate().before(DateManager.getSeventhCpcDate()) 
					&& dueAuditCage.getGradePay()!=0 
					&& dueAuditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
				basicPay = dueAuditCage.getOldBasicPay();
				msPay = Utils.get6cpcMsPay(dueAuditCage);
				daRate = 1.25;	
				if (dueAuditCage.getXGroupPay()!=0)
					xGroupPay = Utils.get6cpcXGroupPay(dueAuditCage);				
				variableComponents = oldGradePay + msPay + xGroupPay;
			} else {
				variableComponents =  msPay + xGroupPay;
			} 
			
			
		/*System.out.println("nnnnnnnnnnnnnnnnnnnnnnnnn   variable components + date  " + variableComponents 
				+"  "+dueAuditCage.getFromDate()+"  "+basicPay +"  " +daRate);*/
			 da += (basicPay + variableComponents) * daRate;
	 	}
		return ((int) Math.round(da));
	}
	//new Method added to decide the date of generation of enrmst
		//other do2s when muster is fed for TA Personnel
		public static boolean isBaseAfterEnrollDate(Do2 do2){
			List<AuditCage> atCageList = acRepo.findAllEffectiveAcOrderByFromDateToDateAsc(do2.getEmployee().getId());
				 
			Date enrolementDate= do2.getEmployee().getEnrolmentDate();
			if(enrolementDate==null){
				return true;
			}
	/*		System.out.println("muster proces  "+atCageList.get(0).getFromDate()+"  "+enrolementDate
					+"  "+atCageList.get(0).getFromDate().after(enrolementDate));
	*/		if(atCageList.get(0).getFromDate().after(enrolementDate)){
				return true;
			}
			return false;
					
		}
		//generates embodi ,disembodi do2 when tatrng is processed
		public static void generateDo2sForTatrng(Employee employee, Do2 do2, String description,Date fromDate) {
			String qe = Utils.getCurrentQuarter();
		 	Do2 tatrngdo2 = new Do2(); 
			tatrngdo2.setEmployee(employee);
			tatrngdo2.setArmyNo(employee.getArmyNo());
			tatrngdo2.setCheckDigit(employee.getCheckDigit());
			tatrngdo2.setUnitCode(employee.getUnit().getUnitCode());
			tatrngdo2.setUnit(employee.getUnit());
			tatrngdo2.setDo2Group("IA");
			tatrngdo2.setDo2Type(do2.getDo2Type());
			tatrngdo2.setDo2No(do2.getDo2No());
			tatrngdo2.setDo2ItemNo(do2.getDo2ItemNo());
			tatrngdo2.setDo2Date(do2.getDo2Date());
			tatrngdo2.setDescription(description);
			tatrngdo2.setFromDate(fromDate);
	 		tatrngdo2.setColumnC(do2.getDescription());
	 		tatrngdo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
			tatrngdo2.setAuditCage(true);
			tatrngdo2.setStatus(Do2Manager.PENDING);
			tatrngdo2.setQe(qe);
			tatrngdo2.setAcceptedQe(qe);
			tatrngdo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			do2Repo.save(tatrngdo2);
			
			
		}
		//generates cancel for embodi and disembodi when tatrng is cancelled
		public static void generateDo2sForTatrngCancel(Employee employee, Do2 do2, String description,Date fromDate) {
			String qe = Utils.getCurrentQuarter();
//		System.out.println("refdo2  "+do2.getEmployee()+"  "+do2.getRefDo2No());
		 	Do2 tatrngdo2 = new Do2(); 
			tatrngdo2.setEmployee(employee);
			tatrngdo2.setArmyNo(employee.getArmyNo());
			tatrngdo2.setCheckDigit(employee.getCheckDigit());
			tatrngdo2.setUnitCode(employee.getUnit().getUnitCode());
			tatrngdo2.setUnit(employee.getUnit());
			tatrngdo2.setDo2Group("IA");
			tatrngdo2.setDo2Type(do2.getDo2Type());
			tatrngdo2.setDo2No(do2.getDo2No());
			tatrngdo2.setDo2ItemNo(do2.getDo2ItemNo());
			tatrngdo2.setDo2Date(do2.getDo2Date());
			tatrngdo2.setDescription(Do2Manager.CANCEL);
			tatrngdo2.setFromDate(fromDate);
			tatrngdo2.setColumnA(description);
	 		tatrngdo2.setColumnC(do2.getDescription());
	 		tatrngdo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
			tatrngdo2.setAuditCage(true);
			tatrngdo2.setStatus(Do2Manager.PENDING);
			tatrngdo2.setQe(qe);
			tatrngdo2.setAcceptedQe(qe);
			tatrngdo2.setRefDo2No(do2.getRefDo2No());
			tatrngdo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			do2Repo.save(tatrngdo2);
			
			
		}
		
		//this method is for checking stoping allowances for FS
		//***method added on 09062012
		static public void removeAllowancesForFs(AuditCage newAuditCage) {
 
			List<AuditCageAllowance> auditCageAllowanceList =acaRepo.findAllByAuditCageId(newAuditCage.getId());
					
		
			/*for (AuditCageAllowance aca : auditCageAllowanceList) {
				if (aca.getPayCode().getPersonal()==false) {
					
					if (aca.getPayCode().getPayCode().equals("AVPAY") 
							|| aca.getPayCode().getPayCode().equals("AETECH")
							|| aca.getPayCode().getPayCode().equals("AIRWCA")
							|| aca.getPayCode().getPayCode().equals("APTPAY")
							|| aca.getPayCode().getPayCode().equals("CILQ")
							|| aca.getPayCode().getPayCode().equals("HRA")
							|| aca.getPayCode().getPayCode().equals("HRAFAM")
							|| aca.getPayCode().getPayCode().equals("FAA")) {
						continue;
					}
					entityManager.remove(aca);
				}
			}*/
			for (AuditCageAllowance aca : auditCageAllowanceList) {
				if (aca.getPayCode().getPersonal()==false) {
					if (!aca.getAuditCage().getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
						if (aca.getPayCode().getPayCode().equals("AVPAY") 
								|| aca.getPayCode().getPayCode().equals("AETECH")
								|| aca.getPayCode().getPayCode().equals("AIRWCA")
								|| aca.getPayCode().getPayCode().equals("APTPAY")
								|| aca.getPayCode().getPayCode().equals("HRA")) {
							continue;
						}
						acaRepo.delete(aca);
							 
							newAuditCage.getAuditCageAllowances().remove(aca);
					} else {
						if (aca.getPayCode().getPayCode().equals("AVPAY") 
								|| aca.getPayCode().getPayCode().equals("AETECH")
								|| aca.getPayCode().getPayCode().equals("AIRWCA")
								|| aca.getPayCode().getPayCode().equals("APTPAY")
								|| aca.getPayCode().getPayCode().equals("FLCHCA")
								|| aca.getPayCode().getPayCode().equals("CILQ")
								|| aca.getPayCode().getPayCode().equals("FAA")) {
							continue;
						}
						acaRepo.delete(aca);
						 
						newAuditCage.getAuditCageAllowances().remove(aca);
					}
				}
			}
				
			 
		}
		
/*		public static double getIslndaRate(Do2 do2, AuditCage auditCage) {
			List<Allowance> allowances = Utils.getApplicableAllowanceRates(
					do2.getDescription(), do2.getColumnB(), auditCage.getFromDate(), auditCage.getFromDate());
			double islandaRate=0.0;
			int basicPay = auditCage.getBasicPay() == null ? 0 : auditCage.getBasicPay();
			int classPay = auditCage.getClassPay() == null ? 0 : auditCage.getClassPay();
			int dp = (int) (basicPay * .50);
			int gradePay = auditCage.getGradePay() == null ? 0 : auditCage.getGradePay();
			int xPay = auditCage.getXGroupPay() == null ? 0 : auditCage.getXGroupPay();
			int msPay = auditCage.getMsPay() == null ? 0 : auditCage.getMsPay();
			int oldBasicPay = auditCage.getOldBasicPay() == null ? 0 : auditCage.getOldBasicPay();
			int dpforOldBasicPay = (int) (oldBasicPay * .50);
			int oldClassPay = auditCage.getOldClassPay() == null ? 0 : auditCage.getOldClassPay();
			for (Allowance allow : allowances) {
				if (auditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
					if (!auditCage.getFromDate().after(DateManager.getDPDate())) {
						islandaRate = (basicPay + classPay) * (allow.getRatePercentage());
					} else {
						islandaRate = (basicPay + dp + classPay)	* (allow.getRatePercentage());
					}
				} else if (!auditCage.getFromDate().before(DateManager.getSixthCpcDate())
						&& !auditCage.getFromDate().after(DateManager.getSixthCpcAllowanceDate())) {
					islandaRate = (oldBasicPay + oldClassPay + dpforOldBasicPay) * (allow.getRatePercentage());
				} else if (!auditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {	
						islandaRate = (basicPay + gradePay + xPay + msPay) * (allow.getRatePercentage());
				}

			}
	      return islandaRate;
		}*/

		// commented above lines and amended as given below due to implementation of sevencpc
		
		public static double getIslndaRate(Do2 do2, AuditCage auditCage) {
			List<Allowance> allowances = Utils.getApplicableAllowanceRates(
					do2.getDescription(), do2.getColumnB(), auditCage.getFromDate(), auditCage.getFromDate());
			double islandaRate=0.0;
			int basicPay = auditCage.getBasicPay() == null ? 0 : auditCage.getBasicPay();
			int classPay = auditCage.getClassPay() == null ? 0 : auditCage.getClassPay();
			int dp = (int) (basicPay * .50);
			int gradePay = auditCage.getGradePay() == null ? 0 : auditCage.getGradePay();
			int xPay = auditCage.getXGroupPay() == null ? 0 : auditCage.getXGroupPay();
			int msPay = auditCage.getMsPay() == null ? 0 : auditCage.getMsPay();
			int oldBasicPay = auditCage.getOldBasicPay() == null ? 0 : auditCage.getOldBasicPay();
			int dpforOldBasicPay = (int) (oldBasicPay * .50);
			int oldClassPay = auditCage.getOldClassPay() == null ? 0 : auditCage.getOldClassPay();
			int oldGradePay = auditCage.getOldGradePay() == null ? 0 : auditCage.getOldGradePay();
			for (Allowance allow : allowances) {
				
				if (auditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
					if (!auditCage.getFromDate().after(DateManager.getDPDate())) {
						islandaRate = (basicPay + classPay) * (allow.getRatePercentage());
					} else {
						islandaRate = (basicPay + dp + classPay)	* (allow.getRatePercentage());
					}
				} else if (auditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
					islandaRate = (oldBasicPay + oldClassPay + dpforOldBasicPay) * (allow.getRatePercentage());
				} else if (auditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
					islandaRate = (basicPay + gradePay + xPay + msPay) * (allow.getRatePercentage());
				} else if (auditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
					msPay = Utils.get6cpcMsPay(auditCage);
					xPay = Utils.get6cpcXGroupPay(auditCage);
					islandaRate = (oldBasicPay + oldGradePay + xPay + msPay) * (allow.getRatePercentage());
				} else {
					islandaRate = (basicPay) * (allow.getRatePercentage());
				}
				/*if (auditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
					if (auditCage.getFromDate().before(DateManager.getDPDate())) {
						islandaRate = (basicPay + classPay) * (allow.getRatePercentage());
					} else {
						islandaRate = (basicPay + dp + classPay)	* (allow.getRatePercentage());
					}
				} else if (!auditCage.getFromDate().before(DateManager.getSixthCpcDate())
						&& auditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
					islandaRate = (oldBasicPay + oldClassPay + dpforOldBasicPay) * (allow.getRatePercentage());
				} else if (!auditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())
						&& auditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {	
						islandaRate = (basicPay + gradePay + xPay + msPay) * (allow.getRatePercentage());
				} else if (!auditCage.getFromDate().before(DateManager.getSeventhCpcDate())){
					msPay = Utils.get6cpcMsPay(auditCage);
					xPay = Utils.get6cpcXGroupPay(auditCage);
					islandaRate = (oldBasicPay + oldGradePay + xPay + msPay) * (allow.getRatePercentage());							
				}				*/
			}
	      return islandaRate;
		}

	//  HRDALL emoluments  vide HQ letter no AT/I/3510/6thCPC/VI dated 01.08.2012
		
		
	/*			public static double getHrdallRate(Do2 do2,AuditCage auditCage) {
					
					double hrdallRate=0.0;
					
					int basicPay = auditCage.getBasicPay() == null ? 0 : auditCage.getBasicPay();
					int classPay = auditCage.getClassPay() == null ? 0 : auditCage.getClassPay();
					int dp = (int) (basicPay * .50);
					int gradePay = auditCage.getGradePay() == null ? 0 : auditCage.getGradePay();
					int xPay = auditCage.getXGroupPay() == null ? 0 : auditCage.getXGroupPay();
					int msPay = auditCage.getMsPay() == null ? 0 : auditCage.getMsPay();
					int oldBasicPay = auditCage.getOldBasicPay() == null ? 0 : auditCage.getOldBasicPay();
					int dpforOldBasicPay = (int) (oldBasicPay * .50);
					int oldClassPay = auditCage.getOldClassPay() == null ? 0 : auditCage.getOldClassPay();	
					
					if (auditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
						if (!auditCage.getFromDate().after(DateManager.getDPDate())) {
							hrdallRate = (basicPay + classPay);
						} else {
							hrdallRate = (basicPay + dp + classPay);
						}
					} else if (!auditCage.getFromDate().before(DateManager.getSixthCpcDate())
							&& !auditCage.getFromDate().after(DateManager.getSixthCpcAllowanceDate())) {
						hrdallRate = (oldBasicPay + oldClassPay + dpforOldBasicPay);

					} else if (!auditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
						//  HRDALL emoluments  vide HQ letter no AT/I/3510/6thCPC/VI dated 01.08.2012	
						// basic pay as per Rule 3(8) of CCS Revised Pay Rules, 2008 --> basic pay + applicable grade pay
							hrdallRate = (basicPay + gradePay);
					}
					
					if (do2.getColumnB() != null) {
						List<Allowance> allowances = new ArrayList<Allowance>();
						allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnB(), do2.getFromDate(),
								do2.getFromDate());
					
						for (Allowance allow : allowances) {
							hrdallRate = hrdallRate*allow.getRatePercentage();
						}
					} else if (do2.getColumnB() == null){
						hrdallRate = hrdallRate*do2.getRate()/100;
					}
					
					if (auditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
						if (!auditCage.getFromDate().after(DateManager.getDPDate())) {
							hrdallRate = (basicPay + classPay) * do2.getRate() / 100;
						} else {
							hrdallRate = (basicPay + dp + classPay)	* do2.getRate() / 100;
						}
					} else if (!auditCage.getFromDate().before(DateManager.getSixthCpcDate())
							&& !auditCage.getFromDate().after(DateManager.getSixthCpcAllowanceDate())) {
						hrdallRate = (oldBasicPay + oldClassPay + dpforOldBasicPay) * do2.getRate() / 100;

					} else if (!auditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
						//  HRDALL emoluments  vide HQ letter no AT/I/3510/6thCPC/VI dated 01.08.2012	
						// basic pay as per Rule 3(8) of CCS Revised Pay Rules, 2008 --> basic pay + applicable grade pay
							hrdallRate = (basicPay + gradePay) * do2.getRate() / 100;
					}
			      return hrdallRate;
				}*/


/*		// commented above lines and amended as given below due to implementation of sevencpc
		
				public static double getHrdallRate(Do2 do2,AuditCage auditCage) {
					double hrdallRate=0.0;
					int basicPay = auditCage.getBasicPay() == null ? 0 : auditCage.getBasicPay();
					int classPay = auditCage.getClassPay() == null ? 0 : auditCage.getClassPay();
					int dp = (int) (basicPay * .50);
					int gradePay = auditCage.getGradePay() == null ? 0 : auditCage.getGradePay();
					int xPay = auditCage.getXGroupPay() == null ? 0 : auditCage.getXGroupPay();
					int msPay = auditCage.getMsPay() == null ? 0 : auditCage.getMsPay();
					int oldBasicPay = auditCage.getOldBasicPay() == null ? 0 : auditCage.getOldBasicPay();
					int dpforOldBasicPay = (int) (oldBasicPay * .50);
					int oldClassPay = auditCage.getOldClassPay() == null ? 0 : auditCage.getOldClassPay();
					int oldGradePay = auditCage.getOldGradePay() == null ? 0 : auditCage.getOldGradePay();
					
					if (auditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
						if (!auditCage.getFromDate().after(DateManager.getDPDate())) {
							hrdallRate = (basicPay + classPay);
						} else {
							hrdallRate = (basicPay + dp + classPay);
						}
					} else if (auditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
						hrdallRate = (oldBasicPay + oldClassPay + dpforOldBasicPay);
					} else if (auditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
					//  HRDALL emoluments  vide HQ letter no AT/I/3510/6thCPC/VI dated 01.08.2012	
						// basic pay as per Rule 3(8) of CCS Revised Pay Rules, 2008 --> basic pay + applicable grade pay
							hrdallRate = (basicPay + gradePay);
					} else if (auditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
						hrdallRate = (oldBasicPay + oldGradePay);
					} else {
						hrdallRate = (basicPay);
					}
					
					if (auditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
						if (auditCage.getFromDate().before(DateManager.getDPDate())) {
							hrdallRate = (basicPay + classPay);
						} else {
							hrdallRate = (basicPay + dp + classPay);
						}
					} else if (!auditCage.getFromDate().before(DateManager.getSixthCpcDate())
							&& auditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
						hrdallRate = (oldBasicPay + oldClassPay + dpforOldBasicPay);

					} else if (!auditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate()) &&
							auditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
						//  HRDALL emoluments  vide HQ letter no AT/I/3510/6thCPC/VI dated 01.08.2012	
						// basic pay as per Rule 3(8) of CCS Revised Pay Rules, 2008 --> basic pay + applicable grade pay
							hrdallRate = (basicPay + gradePay);
					} else if (!auditCage.getFromDate().before(DateManager.getSeventhCpcDate())
							&& auditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())){
							hrdallRate = (oldBasicPay + oldGradePay);
					} else if (!auditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())){
						hrdallRate = (basicPay);
					}
					
					if (do2.getColumnB() != null) {
						List<Allowance> allowances = new ArrayList<Allowance>();
						allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getColumnB(), do2.getFromDate(),
								do2.getFromDate());
					
						for (Allowance allow : allowances) {
							hrdallRate = hrdallRate*allow.getRatePercentage();
						}
					} else if (do2.getColumnB() == null){
						hrdallRate = hrdallRate*do2.getRate()/100;
					}
					
					if (auditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
						if (!auditCage.getFromDate().after(DateManager.getDPDate())) {
							hrdallRate = (basicPay + classPay) * do2.getRate() / 100;
						} else {
							hrdallRate = (basicPay + dp + classPay)	* do2.getRate() / 100;
						}
					} else if (!auditCage.getFromDate().before(DateManager.getSixthCpcDate())
							&& !auditCage.getFromDate().after(DateManager.getSixthCpcAllowanceDate())) {
						hrdallRate = (oldBasicPay + oldClassPay + dpforOldBasicPay) * do2.getRate() / 100;

					} else if (!auditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
						//  HRDALL emoluments  vide HQ letter no AT/I/3510/6thCPC/VI dated 01.08.2012	
						// basic pay as per Rule 3(8) of CCS Revised Pay Rules, 2008 --> basic pay + applicable grade pay
							hrdallRate = (basicPay + gradePay) * do2.getRate() / 100;
					}
			      return hrdallRate;
			}	*/
		
		

		public static double getHrdallRate(Do2 do2, Do2 atcgDo2, AuditCage auditCage) {
					double hrdallRate=0.0;
					int basicPay = auditCage.getBasicPay() == null ? 0 : auditCage.getBasicPay();
					int classPay = auditCage.getClassPay() == null ? 0 : auditCage.getClassPay();
					int dp = (int) (basicPay * .50);
					int gradePay = auditCage.getGradePay() == null ? 0 : auditCage.getGradePay();
					int xPay = auditCage.getXGroupPay() == null ? 0 : auditCage.getXGroupPay();
					int msPay = auditCage.getMsPay() == null ? 0 : auditCage.getMsPay();
					int oldBasicPay = auditCage.getOldBasicPay() == null ? 0 : auditCage.getOldBasicPay();
					int dpforOldBasicPay = (int) (oldBasicPay * .50);
					int oldClassPay = auditCage.getOldClassPay() == null ? 0 : auditCage.getOldClassPay();
					int oldGradePay = auditCage.getOldGradePay() == null ? 0 : auditCage.getOldGradePay();
					
		
					
					if (auditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
						if (auditCage.getFromDate().before(DateManager.getDPDate())) {
							hrdallRate = (basicPay + classPay);
						} else {
							hrdallRate = (basicPay + dp + classPay);
						}
					} else if (!auditCage.getFromDate().before(DateManager.getSixthCpcDate())
							&& auditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
						hrdallRate = (oldBasicPay + oldClassPay + dpforOldBasicPay);

					} else if (!auditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate()) &&
							auditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
						//  HRDALL emoluments  vide HQ letter no AT/I/3510/6thCPC/VI dated 01.08.2012	
						// basic pay as per Rule 3(8) of CCS Revised Pay Rules, 2008 --> basic pay + applicable grade pay
							hrdallRate = (basicPay + gradePay);
					} else if (!auditCage.getFromDate().before(DateManager.getSeventhCpcDate())
							&& auditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())){
							hrdallRate = (oldBasicPay + oldGradePay);
					} else if (!auditCage.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())){
						hrdallRate = (basicPay);
					}
					
						
					double do2Rate = 0.0;
					String columnb = null;
					
					if (do2.getColumnB() != null) {
						 columnb = do2.getColumnB();
					} 
					
					if (do2.getRate()!=null) {
						do2Rate = do2.getRate();
					}
					
				
					if (atcgDo2.getColumnB() != null) {
						List<Allowance> allowances = new ArrayList<Allowance>();
						allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), columnb, do2.getFromDate(),
								do2.getFromDate());
						
					
						for (Allowance allow : allowances) {
							hrdallRate = hrdallRate*allow.getRatePercentage();
						}
					} else if (atcgDo2.getColumnB() == null){
						   do2Rate=do2.getRate();
						   if(!atcgDo2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())){
							   if(do2Rate==12.5) {
									do2Rate=10.0;
								} else if(do2Rate==15.0){
									do2Rate=12.0;
								}
							   hrdallRate = hrdallRate*do2Rate/100;
						   } else {
							   do2Rate=do2.getRate();
							 
							   hrdallRate = hrdallRate*do2Rate/100;
						   }
						 
					}
					
				      return hrdallRate;
			}	

				
				
		static public boolean isValidZcategory(Do2 do2){
			
			String[] zCategory = { "BARBER", "BHR", "BKR", "BSMITHU", "CARPU", "CHEFCOM",
				"CHEFHOSP", "CHEFMESS", "CHEFSPEL", "CSTEWD", "DSCGD", "DVRAT", "DVRMMA", "ER", "FARR", "KLN", 
				"MASALCHI", "MCHEF", "MUSICIAN", "PKR", "PNRGD", "PTRU", "ASH",
				"SDU", "SHGD", "SRCHEF", "STEWD", "SWALA", "SWRGD", "TLR", "TSMITH", "TXRE", "UPHOL", "WM" } ;
			final List<String> Z_CATEGORY = Arrays.asList(zCategory);
			String category = do2.getColumnC();
			String group = do2.getColumnA().substring(do2.getColumnA().length() -1);
			if(do2.getDescription().equals("ATTSAM") || do2.getDescription().equals("ATTSBM")){
				category = do2.getColumnB();
			}
			if(do2.getDescription().equals("PROMOT") && do2.getColumnA().equals("NBSUB")){
				group = do2.getColumnB().substring(do2.getColumnB().length() -1);
			}
			System.out.println("group "+group+"--"+category+"--"+Z_CATEGORY.contains(category));
			if(do2.getFromDate().before(DateManager.getSixthCpcDate())){
			      if(!group.equals("Z") && Z_CATEGORY.contains(category)){
			    	  	return false;
			      }
			}
			
			return true;
		}
//new method added on 30082012 to generate 5cpcincr when sixcpc is fed		
		public static void generateIncrIn5pcScales(Do2 do2){
        	Date previousIncrDate=null;
        	List<Do2> do2List = do2Repo.findByDescOrDescListFromDateLessThanEqual(do2.getEmployee().getId(),Do2Manager.VALID,"INCR",Do2Manager.getIncrCancelDescription(),DateManager.getSixthCpcDate());
        		 
        	  if(do2List.size() >0){
        		   previousIncrDate= do2List.get(0).getFromDate();
        	  }
     //         System.out.println("after 5cpc retrive "+previousIncrDate);
        	  if(previousIncrDate !=null){
        		    Date sixcpcDate = do2.getFromDate();
        			Date dni = new Date();
        			Calendar currentIncrCal = new GregorianCalendar();
        			Calendar nextIncrCal = new GregorianCalendar();

        			currentIncrCal.setTime(previousIncrDate);
        			int date = currentIncrCal.get(Calendar.DATE);
        			int year = currentIncrCal.get(Calendar.YEAR);
        			int month = currentIncrCal.get(Calendar.MONTH);
        			
        			nextIncrCal = new GregorianCalendar(year + 1, month, 1);
        			dni.setTime(nextIncrCal.getTimeInMillis());
        			
        			while(dni.before(sixcpcDate)){
        			    Date incrDate = new Date();
        			    incrDate.setTime(dni.getTime());
        				insertIncrmentDo2s(do2, incrDate, do2.getDescription());
        				
        				currentIncrCal.setTime(dni);
        			
            			int nextIncrdate = currentIncrCal.get(Calendar.DATE);
            			int nextIncryear = currentIncrCal.get(Calendar.YEAR);
            			int nextIncrmonth = currentIncrCal.get(Calendar.MONTH);
            			nextIncrCal = new GregorianCalendar(nextIncryear + 1, nextIncrmonth, 1);
            			dni.setTime(nextIncrCal.getTimeInMillis());
        			}

        		    
        		    
        	  }
        }
        
		//new method added on 30082012 to generate 6cpcincr when sixcpc is fed
	public static void generatesixcpcIncrment(Do2 do2) {
		List<Do2> promotList = do2Repo.findByDescListAndFromDateGreater(do2.getEmployee().getId(),Do2Manager.VALID, Do2Manager.getIncrCancelDescription(), do2.getFromDate());
			 
		Date toDate = Utils.getLastDayOfCurrentQuarter();
		if (promotList.size() > 0) {
			toDate = promotList.get(0).getFromDate();
		}
		Date xDate = do2.getFromDate();
		Calendar yy = new GregorianCalendar();
		yy.setTimeInMillis(xDate.getTime());
		int zz = yy.get(Calendar.YEAR);

		Calendar chkDate1 = new GregorianCalendar(zz, Calendar.JANUARY, 01);
		Calendar chkDate2 = new GregorianCalendar(zz, Calendar.JULY, 01);

		Date jan01 = chkDate1.getTime();
		Date jul01 = chkDate2.getTime();
		Date dni = new Date();
		Calendar currentIncrCal = new GregorianCalendar();
		Calendar nextIncrCal = new GregorianCalendar();
		currentIncrCal.setTime(do2.getFromDate());
		if(xDate.after(jan01) && xDate.before(jul01)){
			int date = currentIncrCal.get(Calendar.DATE);
			int year = currentIncrCal.get(Calendar.YEAR);
			int month =6;
			
			nextIncrCal = new GregorianCalendar(year + 1, month, 1);
			dni.setTime(nextIncrCal.getTimeInMillis());
		}else{
			int date = currentIncrCal.get(Calendar.DATE);
			int year = currentIncrCal.get(Calendar.YEAR);
			int month =6;
			
			nextIncrCal = new GregorianCalendar(year , month, 1);
			dni.setTime(nextIncrCal.getTimeInMillis());
		}
		
		while(dni.before(toDate)){
			Date incrDate = new Date();
			incrDate.setTime(dni.getTime());
			insertIncrmentDo2s(do2, incrDate, do2.getDescription());
			currentIncrCal.setTime(dni);
			
			int nextIncrdate = currentIncrCal.get(Calendar.DATE);
			int nextIncryear = currentIncrCal.get(Calendar.YEAR);
			int nextIncrmonth = currentIncrCal.get(Calendar.MONTH);
			 nextIncrCal = new GregorianCalendar(nextIncryear + 1, nextIncrmonth, 1);
			dni.setTime(nextIncrCal.getTimeInMillis());
			
		}


		
	     
    }
	//added on 30082012 to regenerate cancelled profix do2 when
	//sixcpc is cancelled
	static public void regenerateCancelledProfixDo2s(Do2 do2) {
		Do2 profixdo2 = new Do2();
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		profixdo2.setEmployee(employee);
		profixdo2.setArmyNo(employee.getArmyNo());
		profixdo2.setCheckDigit(employee.getCheckDigit());
		profixdo2.setUnitCode(employee.getUnit().getUnitCode());
		profixdo2.setUnit(employee.getUnit());
		profixdo2.setDo2Group(do2.getDo2Group());
		profixdo2.setDo2Type(do2.getDo2Type());
		profixdo2.setDo2No(do2.getDo2No());
		profixdo2.setDo2ItemNo(do2.getDo2ItemNo());
		profixdo2.setDo2Date(do2.getDo2Date());
		profixdo2.setDescription(do2.getDescription());
		profixdo2.setFromDate(do2.getFromDate());
		profixdo2.setColumnA(do2.getColumnA());
		profixdo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		profixdo2.setAuditCage(true);
		profixdo2.setStatus(Do2Manager.PENDING);
		profixdo2.setQe(Utils.getCurrentQuarter());
		profixdo2.setAcceptedQe(Utils.getCurrentQuarter());
		profixdo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		do2Repo.save(profixdo2);
	}
	
	//new method added on 30082012 to regenerate sixcpc increments on 
	 //sixcpc cancel
		public static void generateSixcpcCancel(Do2 do2) {
			List<Do2> promotList = do2Repo.findByDescListAndFromDateGreater(do2.getEmployee().getId(),Do2Manager.VALID, Do2Manager.getIncrCancelDescription(), do2.getFromDate());
				 
			Date toDate = Utils.getLastDayOfCurrentQuarter();
			Date dni = null;
			try {
				dni = DateManager.getDate("2006-07-01");
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (promotList.size() > 0) {
				toDate = promotList.get(0).getFromDate();
			}
			
			while (dni.before(toDate)){
				Date incrDate = new Date();
				incrDate.setTime(dni.getTime());
				insertIncrmentDo2s(do2, incrDate, do2.getDescription());
				dni = Utils.getNextIncrementDate(dni);
				
			}
	    }

		static public int getSfaRate(double allcAmt, Date fromDate, Rank rank) {
			double sfaAmount = allcAmt;
			if (fromDate.before(DateManager.getSeventhCpcAllowanceDate())) {
				int rate_order = 0;
				Rank prevRank = null;
				List<AllowanceRate >allowanceRateList = null;
				allowanceRateList = alwRateRepo.findByAlwNameRateFromdateLessThanEqual(sfaAmount, fromDate, "SFA");
				 
				if (allowanceRateList.size()>0) {
					prevRank = allowanceRateList.get(0).getRank();
				}
				allowanceRateList = alwRateRepo.findByAlwNameRankFromdateLessThanEqual(prevRank.getId(), fromDate, "SFA");
				 
				if (allowanceRateList.size()>0) {
					for (int i=0; i < allowanceRateList.size(); ++i){
						if (sfaAmount == allowanceRateList.get(i).getRateAmount()){
							rate_order=i;
						}
					}
				}
				allowanceRateList =alwRateRepo.findByAlwNameRankFromdateLessThanEqual(rank.getId(), fromDate, "SFA");
				 
				if (allowanceRateList.size()>0) {
					for (int i=0; i < allowanceRateList.size(); ++i){
						if ( i == rate_order)
							sfaAmount = allowanceRateList.get(i).getRateAmount();
					}
				}
			} else if (fromDate.before(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate7cpc())) {
				if (sfaAmount == 2000 || sfaAmount == 2400 || sfaAmount == 3600)  {
					sfaAmount=6000;
				} else if (sfaAmount == 3240 || sfaAmount == 3880 || sfaAmount == 5820) {
					sfaAmount=9700;
				}  
			} else {
				if ((sfaAmount == 6000)){
					sfaAmount = 7500;
				} else if ((sfaAmount == 9700)){
					sfaAmount = 12125;
				}
			}
			return (int) sfaAmount;
		}

		public static boolean isTdmergDo2Exist(Do2 do2) {
		 
			List<Do2> do2List = do2Repo.findByDescValidOrPending(do2.getEmployee().getId(),Do2Manager.VALID,Do2Manager.PENDING,"TDMERG");
			 

			return (do2List.size() != 0);
		}
		public static void createMergedTradeDo2(Do2 do2, String sldrClass, String previousTrade, String presentTrade) {
			Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
			Do2 mergedDo2 = new Do2();
			mergedDo2.setEmployee(employee);
			mergedDo2.setArmyNo(do2.getArmyNo());
			mergedDo2.setUnitCode(do2.getUnitCode());
			mergedDo2.setUnit(do2.getUnit());
			mergedDo2.setDo2Group("IA");
			mergedDo2.setCheckDigit(do2.getCheckDigit());
			mergedDo2.setDo2Type(do2.getDo2Type());
			mergedDo2.setDo2No(do2.getDo2No());
			mergedDo2.setDo2ItemNo(do2.getDo2ItemNo());
			mergedDo2.setDo2Date(DateManager.getCurrentDate());
			mergedDo2.setDescription("TDMERG");
			mergedDo2.setFromDate(do2.getFromDate());
			mergedDo2.setColumnA(sldrClass);
			mergedDo2.setColumnB(previousTrade);		
			mergedDo2.setColumnC(presentTrade);
			mergedDo2.setBatchNo(do2.getBatchNo());
			mergedDo2.setStatus(Do2Manager.PENDING);
			mergedDo2.setReason(null);
			mergedDo2.setQe(Utils.getCurrentQuarter());
			mergedDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			mergedDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
			mergedDo2.setAuditCage(true);
			do2Repo.save(mergedDo2);
		}		
		
	
		
		public static void createMergedTradeDo2(Do2 do2, String sldrClass, String previousTrade, String presentTrade, Date fromDate) {
			Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
			Do2 mergedDo2 = new Do2();
			mergedDo2.setEmployee(employee);
			mergedDo2.setArmyNo(do2.getArmyNo());
			mergedDo2.setUnitCode(do2.getUnitCode());
			mergedDo2.setUnit(do2.getUnit());
			mergedDo2.setDo2Group("IA");
			mergedDo2.setCheckDigit(do2.getCheckDigit());
			mergedDo2.setDo2Type(do2.getDo2Type());
			mergedDo2.setDo2No(do2.getDo2No());
			mergedDo2.setDo2ItemNo(do2.getDo2ItemNo());
			mergedDo2.setDo2Date(DateManager.getCurrentDate());
			mergedDo2.setDescription("TDMERG");
			mergedDo2.setFromDate(fromDate);
			mergedDo2.setColumnA(sldrClass);
			mergedDo2.setColumnB(previousTrade);		
			mergedDo2.setColumnC(presentTrade);
			mergedDo2.setBatchNo(do2.getBatchNo());
			mergedDo2.setStatus(Do2Manager.PENDING);
			mergedDo2.setReason(null);
			mergedDo2.setQe(Utils.getCurrentQuarter());
			mergedDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			mergedDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
			mergedDo2.setAuditCage(true);
			do2Repo.save(mergedDo2);
		}

		public static AuditCage regulateTransportAllowance(Do2 do2, AuditCage newAuditCage){
			Rank rank = null;
			int basicPay = 0;
			int gradePay = 0;
			double rateAmount = 0;
			double MINIMUM_6CPC_TPTH_RATE = 1000;
			List<Allowance> allowances = new ArrayList<Allowance>();
			rank =Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
			gradePay = newAuditCage.getGradePay();			
			basicPay = newAuditCage.getBasicPay();
			if (!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())&& newAuditCage.getTransport() > 0) {
				if ((gradePay > 1999 && gradePay < 4200) && basicPay >= 7440)
					gradePay = 4200;
	
				if (gradePay > 0){	
					allowances = Utils.getApplicableAllowanceRates("TPTL", do2.getFromDate(),
						         do2.getFromDate(), gradePay, "CCOTH");
				}
			
				if (allowances.size()>0) {
					rateAmount = allowances.get(0).getRateAmount();
				}
	
				if (do2.getDescription().equalsIgnoreCase(Do2Manager.TPTH)
					&& (rateAmount < MINIMUM_6CPC_TPTH_RATE)) {
					rateAmount = MINIMUM_6CPC_TPTH_RATE;
				}
				newAuditCage.setTransport((int) rateAmount);
			}
			return newAuditCage;
		}

		/*static public void insertIncrementDos2ForAttestMusterIn7cpc(Do2 do2, Date fromDate, String columnA) {
			System.out.println("1111111111111111111111111111111111111 ac helper 7th incr gen ");
			if (do2.getDescription().equals("ATTSBM") || do2.getDescription().equals("ATTSAM") || do2.getDescription().equals("MUSTER")) {
				while ((fromDate = getRetrospectiveIncrementDate(fromDate)).before(Utils.getLastDayOfCurrentQuarter())) {
					System.out.println("222222222222222222222222222222222222  " +fromDate);
					insertIncrmentDo2s(do2, fromDate, columnA);	
				}				 
			}
		}
				
		static public Date getRetrospectiveIncrementDate(Date fromDate)  {
			Calendar cal = new GregorianCalendar();
			cal.setTime(fromDate);
			int year = cal.get(Calendar.YEAR);
			Date jan01=null, jul01=null, nextJan01 = null, incrementDate=null;
			
			try {
				jan01 = DateManager.getDate(year, 1, 1);
				jul01 = DateManager.getDate(year,7,1);
				nextJan01 = DateManager.getDate(year+1, 1,1);
			} catch (Exception e) {
				Utils.handleException(e);				 
			}
			System.out.println("jjjjjjjjjjjjjjjjjjjjj    jan01 jul01 nextjan01 fromDate " + jan01 +"  "+jul01+"  "+nextJan01+"  "+fromDate);
			if (jan01!=null && jul01!=null && fromDate.after(jan01) && !fromDate.after(jul01)) {
				incrementDate = nextJan01;
			}
			
			if (jan01!=null && jul01!=null && fromDate.after(jul01) && !fromDate.after(nextJan01)) {
				try {
					incrementDate = DateManager.getDate(year+1,7,1);
				} catch (Exception e) {
					Utils.handleException(e);
				}			
			}
			System.out.println("From Date .... for increment  " +incrementDate);
			if (incrementDate==null)
				return Utils.getLastDayOfCurrentQuarter();
			else
				return incrementDate;
		}*/

		static public void insertIncrementDos2ForAttestMusterIn7cpc(Do2 do2, Date enrolmentDate, String columnA) {
		 	if (do2.getDescription().equals("ATTSBM") || do2.getDescription().equals("ATTSAM") || do2.getDescription().equals("MUSTER")) {
		 		Date incrementDate = enrolmentDate;
		 		incrementDate = getRetrospectiveFirstIncrementDate(incrementDate, enrolmentDate);		 	
		 		if (incrementDate.before(Utils.getLastDayOfCurrentQuarter())) {
		 			insertIncrmentDo2s(do2, incrementDate, columnA);
		 		}		 		
		 		System.out.println("ano enrolment incrdt do2fromdt cola " +do2.getArmyNo()+"  "+enrolmentDate+"  "+incrementDate 
		 				+"  "+do2.getFromDate()+"  "+columnA );
				while ((incrementDate = Utils.getNextIncrementDate(incrementDate)).before(Utils.getLastDayOfCurrentQuarter())) {
					System.out.println("@@@@@@@@@   ano enrolment incrdt do2fromdt cola " +do2.getArmyNo()+"  "+enrolmentDate+"  "+incrementDate 
			 				+"  "+do2.getFromDate()+"  "+columnA );
		 			insertIncrmentDo2s(do2, incrementDate, columnA);	
				}	
				do2.getEmployee().setIncrementDate(incrementDate);				
			}
		}
				
		static public Date getRetrospectiveFirstIncrementDate(Date incrementDate, Date enrolmentDate)  {
			Calendar cal = new GregorianCalendar();
			cal.setTime(incrementDate);
			int year = cal.get(Calendar.YEAR);
			Date jan01=null, jul01=null, nextJan01 = null, nextJul01 = null;
			
			try {
				jan01 = DateManager.getDate(year, 1, 1);
				jul01 = DateManager.getDate(year, 7, 1);
				nextJan01 = DateManager.getDate(year+1, 1, 1);
				nextJul01 = DateManager.getDate(year+1, 7, 1);
			} catch (Exception e) {
				Utils.handleException(e);				 
			}
	 		
			Date firstIncrementDate=null;
			if (enrolmentDate.equals(jan01)) {
				//return jul01;
				firstIncrementDate=jul01;
			}
			
			if (enrolmentDate.after(jan01) && !enrolmentDate.after(jul01)) {
				//return nextJan01;
				firstIncrementDate=nextJan01;
			}
			
	//		if (enrolmentDate.after(jul01) && enrolmentDate.before(nextJan01)) {
			if (enrolmentDate.after(jul01) && !enrolmentDate.after(nextJan01)) {
				//return nextJul01;	
				firstIncrementDate=nextJul01;
			}
			
			return firstIncrementDate;
			
			/*
			 * if (incrementDate.equals(jan01)) { return nextJan01; }
			 * 
			 * if (incrementDate.equals(jul01)) { return nextJul01; }
			 * 
			 * return nextJul01;
			 */
		}
		

		public static void generateIncrIn6cpcPayBand(Do2 do2){
  		    Date sevenPcOptionDate = do2.getFromDate();
 			Date dni = new Date();
 			Calendar currentIncrCal = new GregorianCalendar();
 			Calendar nextIncrCal = new GregorianCalendar();
 			currentIncrCal.setTime(sevenPcOptionDate); 
 			int year = currentIncrCal.get(Calendar.YEAR);
 			int month = currentIncrCal.get(Calendar.JULY)+1;			
 			nextIncrCal = new GregorianCalendar(year + 1, month, 1);
 			dni.setTime(nextIncrCal.getTimeInMillis());

 			while(dni.before(sevenPcOptionDate)){
 			    Date incrDate = new Date();
 			    incrDate.setTime(dni.getTime());
 				insertIncrmentDo2s(do2, incrDate, do2.getDescription());
 				currentIncrCal.setTime(dni);
		 		int nextIncryear = currentIncrCal.get(Calendar.YEAR);
     			int nextIncrmonth = currentIncrCal.get(Calendar.MONTH);
     			nextIncrCal = new GregorianCalendar(nextIncryear + 1, nextIncrmonth, 1);
     			dni.setTime(nextIncrCal.getTimeInMillis());
 			}
		}
			
	 	public static void generateSevenPcIncr(Do2 do2) {
	 		List<Do2> promotList = do2Repo.findByDescListAndFromDateGreater(do2.getEmployee().getId(),Do2Manager.VALID,Do2Manager.INCR_CANCEL_DESCRIPTIONS,do2.getFromDate());
	 		 
 			Date toDate = Utils.getLastDayOfCurrentQuarter();
 			if (promotList.size() > 0) {
 				toDate = promotList.get(0).getFromDate();
 			}
 			Date xDate = do2.getFromDate();
 			Calendar yy = new GregorianCalendar();
 			yy.setTimeInMillis(xDate.getTime());
 			int zz = yy.get(Calendar.YEAR);
 			Calendar chkDate1 = new GregorianCalendar(zz, Calendar.JANUARY, 01);
 			Calendar chkDate2 = new GregorianCalendar(zz, Calendar.JULY, 01);
 			Calendar chkDate3 = new GregorianCalendar(zz+1, Calendar.JANUARY, 01);
 			Date jan01 = chkDate1.getTime();
			Date jul01 = chkDate2.getTime();
 			Date nextJan01 = chkDate3.getTime();
 			Date dni = new Date();
 			Calendar currentIncrCal = new GregorianCalendar();
 			Calendar nextIncrCal = new GregorianCalendar();
 			currentIncrCal.setTime(do2.getFromDate());
 			if (xDate.equals(jan01)) {
 				int year = currentIncrCal.get(Calendar.YEAR);		
 				nextIncrCal = new GregorianCalendar(year, Calendar.JULY, 1);
 				dni.setTime(nextIncrCal.getTimeInMillis());
 			} else if (xDate.after(jan01) && !xDate.after(jul01)) { 
 				int year = currentIncrCal.get(Calendar.YEAR);		
 				nextIncrCal = new GregorianCalendar(year+1, Calendar.JANUARY, 1);
				dni.setTime(nextIncrCal.getTimeInMillis());
 			} else if (xDate.after(jul01) && !xDate.after(nextJan01)) { 
 				int year = currentIncrCal.get(Calendar.YEAR);
 				nextIncrCal = new GregorianCalendar(year+1, Calendar.JULY, 1);
 				dni.setTime(nextIncrCal.getTimeInMillis());
 			}	
 			while(dni.before(toDate)){
 				Date incrDate = new Date();
 				incrDate.setTime(dni.getTime());
 				insertIncrmentDo2s(do2, incrDate, do2.getDescription());
 				currentIncrCal.setTime(dni);				
 				int nextIncryear = currentIncrCal.get(Calendar.YEAR);
 				int nextIncrmonth = currentIncrCal.get(Calendar.MONTH);
 				nextIncrCal = new GregorianCalendar(nextIncryear + 1, nextIncrmonth, 1);
 				dni.setTime(nextIncrCal.getTimeInMillis());				
 			}	    
		} 
	 	

static public int reviseSpincRate(int allcAmt, Do2 do2, AuditCage newAuditCage) {
	 	//needs revision
	 		int chkAmt = allcAmt;
	 		List<Do2>do2List = do2Repo.findByDescStatusFromDateLessThan(do2.getEmployee().getId(), Do2Manager.VALID, "SPINC",DateManager.getSeventhCpcAllowanceDate());
	 			 
			int existingSpincRate = 0;
			int new6pcRate = 0;
			int new7pcRate = 0;
			for (Do2 d : do2List ) {
				int spinc6pcRate = 0;
				int revised6pcRate = new6pcRate;
				Rank rank = Utils.getPborRank(d.getArmyNo(), d.getFromDate(),"PAYFIX");
				existingSpincRate = calculate_Increment(d,newAuditCage);
	
				List<Allowance> alwList = Utils.getApplicableAllowanceRates("STRINC",DateManager.getSpincRevisedDateSixCpc(), DateManager.getSpincRevisedDateSixCpc(), rank);
					if (alwList.size() > 0) {
							if (d.getTjra() == 1) {					
								spinc6pcRate = (int) alwList.get(0).getRateAmount();
							} else {
								spinc6pcRate = (int) alwList.get(0).getRateAmount();
								spinc6pcRate = spinc6pcRate * d.getTjra();
							}
					}
					  revised6pcRate = revised6pcRate+spinc6pcRate;
					  new6pcRate = revised6pcRate;
					 					  
					int spinc7pcRate = 0;
					int revised7pcRate = new7pcRate;
					String payLevel=null;
					int gradePay = 0;
			
					payLevel = Do2Manager.getPayLevelPromot(rank.getRankCode());
					gradePay = Do2Manager.getSevenCpcGradepay(payLevel);
					
					List<Allowance> alwList1 =Utils.getApplicableAllowanceRates("SPINC", payLevel, gradePay, DateManager.getSeventhCpcAllowanceDate(),
							DateManager.getSeventhCpcAllowanceDate());
					if (alwList1.size() > 0) {
							if (d.getTjra() == 1) {					
								spinc7pcRate = (int) alwList1.get(0).getRateAmount();
							} else {
								spinc7pcRate = (int) alwList1.get(0).getRateAmount();
								spinc7pcRate = spinc7pcRate * d.getTjra();
							}
					}
			
					revised7pcRate = new7pcRate+spinc7pcRate;
				    new7pcRate =  revised7pcRate;
				 
			}
			
			
			
					
			if (newAuditCage.getFromDate().after(DateManager.getSpincRevisedDateSixCpc())) {
	 			List<AuditCageAllowance> newAuditCageAllowanceList = acaRepo.findAllByAuditCageId(newAuditCage.getId());
	 			 
				for (AuditCageAllowance aca : newAuditCageAllowanceList) {
					if (aca.getPayCode().getPayCode().equals("SPINC")) {
						Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
						List<Allowance> alwList = Utils.getApplicableAllowanceRates(
								"STRINC", newAuditCage.getFromDate(),
								newAuditCage.getFromDate(), rank);
						if (alwList.size() > 0) {
							chkAmt = new6pcRate;
						}
					}
				}
	 		}
	 		
	 		if (newAuditCage.getFromDate().after(DateManager.getSeventhCpcAllowanceDate())) {
	 			List<AuditCageAllowance> newAuditCageAllowanceList =acaRepo.findAllByAuditCageId(newAuditCage.getId());
	 				 
				for (AuditCageAllowance aca : newAuditCageAllowanceList) {
					if (aca.getPayCode().getPayCode().equals("SPINC")) {
						Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
						String payLevel=null;
						payLevel = Do2Manager.getPayLevelPromot(rank.getRankCode());
						List<Allowance> alwList=Utils.getApplicableAllowanceRates(Do2Manager.SPINC, Do2Manager.getSevenCpcGradepay(payLevel),payLevel, newAuditCage.getFromDate(), newAuditCage.getFromDate());
						if (alwList.size() > 0) {
							chkAmt = new7pcRate;
						}
					}
				}
	 		}
	 		
	 		return chkAmt;
		}
	 	
	 	
	 	
	 	static public int calculate_Increment(Do2 do2,AuditCage atCage) {
	 		Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
	 			int total_inc = 0;
	 			int increment_arrived_at = 0;
	 			
	 				if (do2.getFromDate().before(DateManager.getSpincRevisedDateSixCpc())) {
	 					if (do2.getTjra() == 1) {
	 						increment_arrived_at = getIncrementVcpc(do2);
	 					} else {
	 						increment_arrived_at = getIncrementVcpc(do2) * do2.getTjra();
	 					}
	 						total_inc = increment_arrived_at;			
	 				} else if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())){
	 					List<Allowance> alwList = Utils.getApplicableAllowanceRates("STRINC",
	 									do2.getFromDate(), do2.getFromDate(), rank);
	 					if (alwList.size() > 0) {
	 						if (do2.getTjra() == 1) {					
	 							increment_arrived_at = (int) alwList.get(0).getRateAmount();
	 							total_inc = increment_arrived_at;
	 					} else {
	 							increment_arrived_at = (int) alwList.get(0).getRateAmount();
	 							total_inc = increment_arrived_at * do2.getTjra();
	 						}
	 					}
	 					
	 				}  else {
	 					
	 					String payLevel = null;
	 					payLevel = atCage.getPayLevel();
	 					List<Allowance> alwList =Utils.getApplicableAllowanceRates(Do2Manager.SPINC, Do2Manager.getSevenCpcGradepay(payLevel),payLevel, do2.getFromDate(), do2.getFromDate());
	 			       
	 					if (alwList.size() > 0) {
	 						if (do2.getTjra() == 1) {					
	 							increment_arrived_at = (int) alwList.get(0).getRateAmount();
	 							total_inc = increment_arrived_at;
	 					} else {
	 							increment_arrived_at = (int) alwList.get(0).getRateAmount();
	 							total_inc = increment_arrived_at * do2.getTjra();
	 						}
	 					}
	 				}
	 		
	 			return total_inc;
	 		}
	 		
	 		static public int getIncrementVcpc(Do2 do2) {
	 			int incr = 0;
	 			Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
	 			Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
	 			String soldierGroup = employee.getGroupCategory().getSoldierGroup();
	 			if (employee.getRemusteringDate() != null
	 					&& do2.getFromDate().before(employee.getRemusteringDate())) {
	 				soldierGroup = employee.getPreviousGroupCategory().getSoldierGroup();
	 			}

	 			Date fifthCpcDate = null;
	 			try {
	 				fifthCpcDate = DateManager.getDate("2005-12-31");
	 			} catch (Exception e) {
	 				Utils.handleException(e);
	 			}
	 			
	 			List<PayScale> payList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleGreaterThanZero(rank.getId(), soldierGroup, fifthCpcDate);
	 		 
	 			if (payList.size() > 0) {
	 				incr = payList.get(0).getIncr0();
	 			}
	 			return incr;
	 		}
	 		
	 		public static AuditCage getAuditCageAsOnDate(Employee emp,Date fromDate) {
	 			 
	 			
	 			List<AuditCage> acList = null;
	 			
	 			acList=acRepo.findByFromDateEqualEffectiveOrderByBasicPayDesc(emp.getId(), fromDate);
	 			
	 			System.out.println("ac helper empid "+emp.getId()+"--"+fromDate+"--aclist--"+acList);
	 			
	 			if(acList!=null && acList.size()>0) {
	 				sortAuditCages(acList);
	 				return acList.get(0);
	 			}
	 					
	 			 acList=acRepo.findPreviousDateEffectiveAc(emp.getId(), fromDate);
	 			 if(acList!=null && acList.size()>0)
	 				 return acList.get(0);
	 			 
	 			return null;
	 		}
	 	
	 		private static void sortAuditCages(List<AuditCage> auditCageList) {
	 			
	 		   
	 		   for (AuditCage auditCage : auditCageList) {
	 			   String columnA = null;
	 	       if (auditCage.getDo2().getColumnA() != null) {
	 				  columnA = auditCage.getDo2().getColumnA().trim();
	 			  }	  
	 			  if (Do2Manager.priorityDescList.contains(auditCage.getDo2().getDescription().trim()) ){
	 			      auditCage.setPriority(Do2Manager.getPriority(auditCage.getDo2().getDescription().trim()));
	 			  } else if (columnA != null){
	 				 auditCage.setPriority(Do2Manager.getPriority(auditCage.getDo2().getDescription().trim()+ " " + columnA));
	 			  } else {
	 				  auditCage.setPriority(Do2Manager.getPriority(auditCage.getDo2().getDescription().trim()));
	 			  }
	 		   }
	 		   Collections.sort(auditCageList);
	 	}
	 	
	 		
	 		
	 		static public AuditCage getFirstAuditCage(Date fromDate, Do2 do2) {
	 			 
		 		
	 			List<AuditCage> atCageList =acRepo.findFirstAuditCage(do2.getEmployee().getId(), fromDate);
	 				 
	 			if (atCageList.size()>0) {
	 				return atCageList.get(0);	
	 			} 
	 			return null;
	 		}
	 		
	 		
	 		static public AuditCage getIncrAuditCage(Date fromDate, Do2 do2) {
	 			 
	 			List<AuditCage> atCageList = acRepo.findByFromDateLessThanEqualAndDpZeroAndGpNotZeroDesc(do2.getEmployee().getId(), fromDate);
	 				 
	 			if (atCageList.size()>0) {
	 				return atCageList.get(0);	
	 			} 
	 			return null;
	 		}
	 		
	 		
	 		static public AuditCage getProfixAuditCage(Date fromDate, Do2 do2) {
	 			 
	 			List<AuditCage> atCageList = acRepo.findByFromDateEqualProfixAuditCage(do2.getEmployee().getId(), fromDate);
	 				 
	 			if (atCageList.size()>0) {
	 				return atCageList.get(0);	
	 			} else {
	 				return null;
	 			}
	 		}
	 		
}
