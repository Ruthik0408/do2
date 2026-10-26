package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.models.AuditCage;

import itsdc.dolphin.utils.Utils;
import itsdc.dolphin.utils.DateManager;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.do2.auditcage.AuditCageHelper;

 
/**
 * 
 * @author ITSDC, SEC'BAD.
 *
 */

@RestController
public class Hrafam extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	
	public List<Allowance> alwList;

	static public String INVALID_AGE = "AU:PBOR is less than 21  /  25 years of age.\n"; 
	static public String INVALID_HRAFAM = "AU:HRAFAM is not admissible for Unmarried PBOR.\n"; 
	static public String INVALID_FAA = "AU:FAA is not admissible for Unmarried PBOR.\n"; 
	static public String INVALID_REFDO2 = "AU:Reference Do2 not provided for Subsequent Claim.\n";
	static public String ALLOWANCE_RATE_NOT_AVAILABLE = "AU:Rate not available in Allowance Rate table.\n";
	
	public StringBuffer buffer = new StringBuffer();
	Rank rank;
	Employee emp;
	
	@Override
	public boolean isValid(Do2 do2) {
		
		if(do2.getColumnA() != null && do2.getColumnA().equals(Do2Manager.CEASED)){
			do2.setStatus(Do2Manager.VALID);
			do2.setReason(null);
			do2Repo.save(do2);
			return true;
		}
		
		boolean validity = true;
		
		emp = Utils.getCurrentEmployee(do2.getArmyNo());
		
		//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
		rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
		
		/*if(do2.getColumnB() == null) {
			if(!isValidRefDo2(do2)) {
				validity = false;
				buffer.append(INVALID_REFDO2);
			}
		}*/

		if(!isValidAge(do2)) {
			validity = false;
			buffer.append(INVALID_AGE);
		}

		/*if(!isMarried(do2)) {
			validity = false;
				buffer.append(INVALID_HRAFAM);
			
		}*/
		if(!isValidAllowanceRate(do2)) {
			validity = false;
			buffer.append(ALLOWANCE_RATE_NOT_AVAILABLE);
			do2.setStatus(Do2Manager.PENDING);
		}
		
		if(!validity) {
			if (do2.getStatus()!=null && !do2.getStatus().equals(Do2Manager.PENDING)) {
				do2.setStatus(Do2Manager.INVALID);
			}	
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		
		do2.setReason(buffer.toString());
		
		do2Repo.save(do2);
		
		createSysDo2(do2);
			
		return validity;
	}
	
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		boolean cilqToBeStopped=false;
		boolean hraToBeStopped=false;
		int hraFamAdmissible = 0;
		int famLfAdmissible = 0;
		emp = Utils.getCurrentEmployee(do2.getArmyNo(), do2.getCheckDigit());
		
		String columnC ="";
		
		
		if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			
			final String[] alwUnderProcess = { Do2Manager.HRAFAM, Do2Manager.FAMLF };
			final List<String> alwToStop = Arrays.asList(alwUnderProcess);
		 	AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
			
		}
		if(do2.getColumnA().equals(Do2Manager.GRANTED)) {
				//rank = Utils.getRank(do2.getArmyNo(), do2.getFromDate());
			
				rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
				
				if (do2.getColumnC().length() > 3){
					if (do2.getColumnC().startsWith("CCZ")){
						 columnC = "CCZ";				
					}
					if ( do2.getColumnC().startsWith("CCY")){
						 columnC = "CCY";				
					}
					if ( do2.getColumnC().startsWith("CCX")){
						 columnC = "CCX";				
					}
					alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(), do2.getFromDate(),columnC);
				} else {
					alwList = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(), do2.getFromDate(), do2.getColumnC());
				}
			/*	alwList = Utils.getApplicableAllowanceRates(Do2Manager.HRAFAM, do2.getFromDate(),
							do2.getFromDate(), do2.getColumnC());
			*/	
				if(alwList.size() > 0) {
					List<AuditCageAllowance> auditCageAllowanceList = acaRepo.findAllByAuditCageId(baseAuditCage.getId());
							
			/*				Utils
					.getEntityManager()
					.createQuery(
							"SELECT aca FROM AuditCageAllowance aca where aca.auditCage=:auditCage")
					.setParameter("auditCage", baseAuditCage)
					.getResultList();*/
					if(auditCageAllowanceList.size() > 0){
						for(AuditCageAllowance aca : auditCageAllowanceList){
							if(aca.getPayCode().getPayCode().equals(Do2Manager.CILQ) && aca.getDo2() !=null && !(aca.getDo2().getColumnC().equals("CCOTH") || aca.getDo2().getColumnC().equals("CCZ"))){
								cilqToBeStopped = true;
							}
							if(aca.getPayCode().getPayCode().equals(Do2Manager.HRA) && aca.getDo2() !=null && !(aca.getDo2().getColumnC().substring(0,3).equals("CCZ"))){
								hraToBeStopped = true;
							}
						}
						
					}
					if(cilqToBeStopped)
					{
						final String [] alwUnderProcess = { Do2Manager.CILQ,Do2Manager.FAA,Do2Manager.HRAFAM,Do2Manager.FAMLF};
						final List<String> alwToStop = Arrays.asList(alwUnderProcess);
						AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
					} else if(hraToBeStopped) {
						final String [] alwUnderProcess = { Do2Manager.HRA,Do2Manager.FAA,Do2Manager.HRAFAM,Do2Manager.FAMLF };
						final List<String> alwToStop = Arrays.asList(alwUnderProcess);
						AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
					} else {
						final String [] alwUnderProcess = {Do2Manager.FAA,Do2Manager.HRAFAM,Do2Manager.FAMLF };
						final List<String> alwToStop = Arrays.asList(alwUnderProcess);
						AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
					}
					int gradePay  = baseAuditCage.getGradePay() == null ? 0 : baseAuditCage.getGradePay();
					int msPay     = baseAuditCage.getMsPay() == null ? 0 : baseAuditCage.getMsPay();
					/*hraFamAdmissible = (int) Math.round(((newAuditCage.getBasicPay() + gradePay + msPay) * 
										alwList.get(0).getRatePercentage())); */
					
					// above line commented and following lines added due to sevencpc
					
					int oldGradePay = baseAuditCage.getOldGradePay() == null ? 0 : baseAuditCage.getOldGradePay();
					int basicPay = newAuditCage.getBasicPay() == null ? 0 : newAuditCage.getBasicPay();
					int oldBasicPay = newAuditCage.getOldBasicPay() == null ? 0: newAuditCage.getOldBasicPay();
					
					
					
					if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
						hraFamAdmissible = (int) Math.round(((basicPay + gradePay+ msPay) * 
								alwList.get(0).getRatePercentage())); 
					} else /*if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate()))*/ {
						msPay = Utils.get6cpcMsPay(newAuditCage);
						hraFamAdmissible = (int) Math.round(((oldBasicPay + oldGradePay+ msPay) * 
								alwList.get(0).getRatePercentage())); 
					} /*else {
						hraFamAdmissible = (int) Math.round(((basicPay) * alwList.get(0).getRatePercentage())); 
					}*/
					
					AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.HRAFAM, hraFamAdmissible);
					famLfAdmissible = 150; // appendix J SRA column - lowest rate
					AuditCageHelper.updateAuditCageAllowance(newAuditCage, Do2Manager.FAMLF, famLfAdmissible);
					
				}
			}
			return Do2Manager.SUCCESS;

	}
	
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
	public boolean isValidAllowanceRate(Do2 do2) {
		alwList = Utils.getApplicableAllowanceRates(Do2Manager.HRAFAM, do2.getFromDate(), do2.getFromDate(),do2.getColumnC());
		return (alwList != null);
	}
	public boolean isValidRefDo2(Do2 do2) {
		Do2 refdo2 = Utils.getReferenceDo2(do2);
		 return (refdo2 != null);
		}
	
	public boolean isValidAge(Do2 do2) {
		if (emp.getBirthDate()==null) {
			return false;
		}
		
		Date age25RelaxOrderDate = null;
		try {
			age25RelaxOrderDate=DateManager.getDate("2018-01-19");
		} catch (Exception e) {
			Utils.handleException(e);
		}
		
		if (do2.getFromDate().before(age25RelaxOrderDate)) {
			return ((DateManager.getNoOfMonths(emp.getBirthDate(), do2.getFromDate()) / 12) >= 25);
		} else {
			// Age reduced from 25 to 21 w.e.f. 19.01.2018
			// (Authority : GOI, MOD letter No. 3(1)/2015-S(Q&C)/pt dated 19-01-2018)
			return ((DateManager.getNoOfMonths(emp.getBirthDate(), do2.getFromDate()) / 12) >= 21);
		}	
	}

	public boolean isMarried(Do2 do2) {
		return (emp.getMarried() != null && emp.getMarried() == true);
	}
	private void createSysDo2(Do2 do2) {
		// Generation Of ALWREV DoII To Revise HRALF rate with effect from 11-05-2010
	 	Date alwrevDate = DateManager.getAlwrevDateForHraLFRevision();
	 	
		GregorianCalendar cal1 = new GregorianCalendar(2010, Calendar.JANUARY, 1);
		Date startDate = new Date();
		startDate.setTime(cal1.getTimeInMillis());
		
		emp = Utils.getCurrentEmployee(do2.getArmyNo());
	 	if (!do2.getFromDate().before(startDate) && do2.getFromDate().before(alwrevDate)) {
		 	if (!isAlwrevAlreadyGenerated(emp, alwrevDate)) {
		 		AuditCageHelper.insertAllowanceRevisionDo2(emp, alwrevDate, do2);
			}
		}

	}
	private boolean isAlwrevAlreadyGenerated(Employee emp, Date alwrevDate){
		
		List<Do2> do2List = do2Repo.findByDescFromDateEqualValidAndPending(emp.getId(),"V","P","ALWREV",alwrevDate);
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.employee=:employee " +
								 " AND d.description='ALWREV' AND d.fromDate=:fromDate AND (d.status='V' OR d.status='P') ")
								.setParameter("employee", emp) 
								.setParameter("fromDate", alwrevDate)
								.getResultList();*/
		return do2List.size()>0;
	}
}