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
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 

@RestController
 public class Acp extends PromotBase implements Do2AuditCageProcessor, java.io.Serializable {
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
	char empGroup = ' ';
	StringBuffer buffer = new StringBuffer();

	public static final String INVALID_PROMOT_RANK = "AU:Invalid Rank for promotion to ACP Rank.\n";
	public static final String DUPLICATE_IN_ESD = "DU:ACP Promotion Already Adjusted. \n";
	public static final String INVALID_PAY_STAGE = "AU:Pay Stage Wrong For Fixation. \n";
	public static final String LESS_QS = "AU:Qualifying Service is less for ACP. \n";
	public static final String INVALID_DATE = "AU:From date is prior to 07-08-2003 or after 31.12.2005. \n";
	public static final String NOT_YET_ATTESTED = "AU:PBOR has not yet been attested. \n";

	@Override
	public boolean isValid(Do2 do2) {
		
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		empRank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
		
		//rejects the do2 if the pbor is ta 
		if(employee.getRegularTaFlag()!=null && employee.getRegularTaFlag().equals("T")){
			do2.setReason("No ACP for for TA Personnel");
			do2.setStatus(Do2Manager.INVALID);
			do2Repo.save(do2);
		   return false;	
			
		}
		if (empRank == null) {
			do2.setStatus(Do2Manager.PENDING);
			buffer.append(Do2Manager.SERVICE_DETAIL_MISSING);
			validity = false;
		}

		if (!isValidStartDate(do2)) {
			buffer.append(INVALID_DATE);
			validity = false;
		}
		if (do2.getId() == newDo2.getId()) {
			if (!(isDo2InESD(do2))) {
				buffer.append(DUPLICATE_IN_ESD);
				validity = false;
			}

			if (!isPborAttested(do2)) {
				buffer.append(NOT_YET_ATTESTED);
				validity = false;
			}

			if (!isValidRank(do2)) {
				buffer.append(INVALID_PROMOT_RANK);
				validity = false;
			} else if (!isQS(do2)) {
				buffer.append(LESS_QS);
				validity = false;
			}

			if (do2.getFromDate().before(DateManager.getSixthCpcDate())) {
				if (!(isValidPayStage(do2))) {
					buffer.append(INVALID_PAY_STAGE);
					validity = false;
				}
			}
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
			if (isProfixReg(do2)) {
				createProfixDo2(employee,do2);
			}
		}

		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		
		if (newAuditCage.getDo2().getToDate() != null &&
			newAuditCage.getDo2().getFromDate().before(DateManager.getSixthCpcDate())) {
			return newAuditCage;
		}
		
		employee = baseAuditCage.getDo2().getEmployee();
		previousEmployeeStatus = employee;
		if (newAuditCage.getDo2().getFromDate().before(DateManager.getSixthCpcDate())) {
			get5cpcPay(newAuditCage, baseAuditCage, employee);
		} else {
			get6cpcPay(newAuditCage, baseAuditCage);
		}
		if (!newAuditCage.getFromDate().before(
				DateManager.getSixthCpcDate())) {
			getOldBasicPay(newAuditCage, baseAuditCage,	previousEmployeeStatus);
		}

		return newAuditCage;

	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {

		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PROMOT+"");
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		final String[] alwNotAdmissible = { "APTPAY" };
		alwToStop = Arrays.asList(alwNotAdmissible);
		AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		updateBdtInfo(do2);
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
	/*	return !do2.getFromDate().before(DateManager.getAcpStartDate())
				&& !do2.getFromDate().after(DateManager.getSixthCpcAllowanceDate());*/
		
		// amended as stated below vide GOI, MOD letter No. 14(1)/99-D(AG) dated 25-07-2018
		// as MACP implementation date chagned from 01-09-2018 to 01-01-2006.
			if (do2.getFromDate().before(DateManager.getAcpStartDate()) || 
					(!do2.getFromDate().before(DateManager.getSixthCpcDate()))) {
				return false;
			}
		return true;
	}

	public boolean isProfixReg(Do2 do2) {
		Date acpDate = do2.getFromDate();
		Calendar cal = new GregorianCalendar();
		cal.setTimeInMillis(acpDate.getTime());
		int acpYear = cal.get(Calendar.YEAR);

		Calendar chkDate1 = new GregorianCalendar(acpYear, Calendar.JANUARY, 01);
		Calendar chkDate2 = new GregorianCalendar(acpYear, Calendar.JULY, 01);

		Date jan01 = chkDate1.getTime();
		Date jul01 = chkDate2.getTime();
		
		if (acpDate.before(DateManager.getSixthCpcDate()) && do2.getToDate()!=null) {
			return true;
		}
		
		if ((acpDate.after(jan01) && acpDate.before(jul01)) && acpDate.after(DateManager.getSixthCpcDate())) {
			return true;
		}
		return false;
	}

	public boolean isDo2InESD(Do2 do2) {		
		List<EmployeeServiceDetail> esdList = esdRepo.findAllByEmployeeIdAndStatusOrderByFromDateAsc(do2.getEmployee().getId(),Do2Manager.ESD_PROMOT+"");
		 
		if (esdList.size() > 0) {
			for (EmployeeServiceDetail esdp : esdList) {
				if (esdp.getDo2().getColumnA() != null
						&& esdp.getDo2().getColumnA().equals(do2.getColumnA()))
					return false;
			}
		}
		return true;
	}

	public boolean isPborAttested(Do2 do2) {
		return employee.getAttestationDate() != null;
	}

	public boolean isValidRank(Do2 do2) {
		Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PROMOT");
	 	System.out.println("  RANK  IN THE EMPLOYEE  :"+rank.getRankCode());
		if (do2.getColumnA().equals("NKSCALE")
				&& (rank.getRankCode().equals("20"))) {
			return true;
		}

		if (do2.getColumnA().equals("HAVSCALE")) {
			if (rank.getRankCode().equals("20")) {
				if (do2.getFromDate().equals(DateManager.getAcpStartDate())) {
					return true;
				} else if (do2.getFromDate().after(DateManager.getAcpStartDate())
						&& employee.getAcpnaikDate() != null && do2.getFromDate().after(employee.getAcpnaikDate())) {
					return true;
				}
			}
			if (rank.getRankCode().equals("30")) {			
				if (!do2.getFromDate().after(DateManager.getAcpStartDate())) {
					return true;
				} else {			
					 if (employee.getAcpnaikDate() != null && do2.getFromDate().after(employee.getAcpnaikDate())) {
						return true;
					}else  if (employee.getNaikDate() != null && do2.getFromDate().after(employee.getNaikDate())) {
						return true;
					}
				}
			}
		}

		return false;
	}

	public boolean isQS(Do2 do2) {

		if (do2.getColumnA().equals("NKSCALE")) {
			return (DateManager.getNoOfMonths(employee.getEnrolmentDate(), do2
					.getFromDate()) >= 120.0);
		}
		if (do2.getColumnA().equals("HAVSCALE")) {
			if (do2.getFromDate().equals(DateManager.getAcpStartDate())) {
				return (DateManager.getNoOfMonths(employee.getEnrolmentDate(),
						do2.getFromDate()) >= 240.0);
			} else if (do2.getFromDate().after(DateManager.getAcpStartDate())) {
				if (employee.getAcpnaikDate() != null) {
					if (employee.getAcpnaikDate().equals(
							DateManager.getAcpStartDate())) {
						return (DateManager.getNoOfMonths(employee
								.getEnrolmentDate(), do2.getFromDate()) >= 240.0);
					} else {
						return (DateManager.getNoOfMonths(employee
								.getEnrolmentDate(), do2.getFromDate()) >= 240.0)
								&& (DateManager.getNoOfMonths(employee
										.getAcpnaikDate(), do2.getFromDate()) >= 120.0);
					}

				} else {
					return (DateManager.getNoOfMonths(employee
							.getEnrolmentDate(), do2.getFromDate()) >= 240.0);
				}

			}
		}
		return false;
	}

	public boolean isValidPayStage(Do2 do2) {
		Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
		GroupCategory gp = Utils.getGroupCategory(do2, "PREV");
		try {
			previousPayList =psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(rank.getId(),gp.getSoldierGroup(), DateManager.getDate("1997-10-10"));
			
		} catch (Exception e) {
			Utils.handleException(e);
		}


		List<AuditCage> atcList =acRepo.findByFromDateLessThanAbsentFalseDescNotEqualOrderByFromDateDesc(do2.getEmployee().getId(),do2.getFromDate(),"REMUST");
		 
	
			for (int i = previousPayList.get(0).getStartScale(); i <= previousPayList
					.get(0).getScaleEnd0(); i += previousPayList.get(0).getIncr0()) {
				if (atcList.get(0).getBasicPay() == i)
					return true;
			}
	
		return false;
	}

	public void updateEmployee(Do2 do2) {
		String strRank = " ";
		
		employee = do2.getEmployee();
		strRank = do2.getColumnA().trim().equals("NKSCALE") ? "ACPNK": "ACPHAV";
		 rank =rankRepo.findByRankName(strRank.trim());
		 
		employee.setActingRank(rank);
		employee.setActingRankDate(do2.getFromDate());

		if (((do2.getColumnA().equals("NKSCALE")) || (do2.getColumnA().equals("LDSCALE"))))
			employee.setAcpnaikDate(do2.getFromDate());
		else
			employee.setAcphavDate(do2.getFromDate());
		
	}


	public void reverseUpdateEmployee(Do2 do2) {
		if (employee == null)
			employee = do2.getEmployee();
		esdPromotList = esdRepo.findAllByEmployeeIdAndStatusOrderByFromDateDesc(employee.getId(),Do2Manager.ESD_PROMOT+"");
				
		 
		String pRankName = null;
		if (esdPromotList.get(0).getDo2().getDescription().equals("MUSTER") ||
			esdPromotList.get(0).getDo2().getDescription().equals("ATTSBM") ||
			esdPromotList.get(0).getDo2().getDescription().equals("ATTSAM"))
			pRankName = "SEP";
		else if (esdPromotList.get(0).getDo2().getDescription().equals("MSTHAV"))
			pRankName = "HAV";
		else if (esdPromotList.get(0).getDo2().getDescription().equals(do2.getColumnA().trim()))
			pRankName = esdPromotList.get(0).getDo2().getColumnA().trim();

		employee.setActingRank(null);
		employee.setActingRankDate(null);

		if (((do2.getColumnA().equals("NKSCALE")) || (do2.getColumnA()
				.equals("LDSCALE"))))
			employee.setAcpnaikDate(null);
		else
			employee.setAcphavDate(null);
	}
	@Override 
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
	}
}