package itsdc.dolphin.do2.auditcage;

 
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 


 
// ATTESTATION OF RECRUITS BEFORE MUSTERING
@RestController
public class Attsbm extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	Employee employee;
	StringBuffer buffer = new StringBuffer();
	//String trade = null;
	String gpcatg = null;
	
	public static final String INVALID_RANK = "AU:Invalid Rank on From date. \n";
	public static final String INVALID_GROUP = "AU:Invalid group on From date. \n";
	public static final String INVALID_CATEGORY = "AU:Invalid category on From date. \n";
	public static final String INCORRECT_PAY = "AU:Incorrect pay on Enrolment Date. \n";
	public static final String ALREADY_ATTESTED = "AU:Already attested as per Employee table. \n";
	public static final String ATTESN_AFTER_MUSTER = "AU:Attestation is after mustering. \n";
	public static final String PAY_SCALE_NOT_FOUND = "AU:Pay scale not found. \n";
	//public static final String ATTEST_MUSTER_GROUP_CATEGORY_DIFFERS = "AU:Mustering/Attestation Group/Category differs. \n ";
	public static final String INVALID_FROM_DATE = "VL:Attestation date is before enrolment date. \n";
	public static final String ENROLL_JCO = "AU: Enrolled as Direct JCO. Attestation do2 need not to be published. \n";
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			 			if ((do2.getColumnA()!=null 
			 					&& (do2.getColumnA().equals("GP-Y") || do2.getColumnA().equals("Y")))
			 					&& (do2.getColumnB()!=null && (do2.getColumnB().equals("DSCGD") || do2.getColumnB().equals("DSCSD")))
			 					&& (do2.getFromDate()!=null && do2.getFromDate().after(employee.getEnrolmentDate()))) {
			 				do2.setStatus(Do2Manager.VALID);
			 				do2.setReason(null);
			 				do2Repo.save(do2);
			 				return true;
			 			} else {
			 				do2.setStatus(Do2Manager.INVALID);
			 				do2.setReason("INVALID Column A/B OR Do2FromDate Before Enrollment Date.\n");
			 				do2Repo.save(do2);
			 				return false;
			 			}			
		 		}
		
		if(newDo2 != null && newDo2.getId()==do2.getId()){
			if(!AuditCageHelper.isValidZcategory(do2)){
				do2.setStatus("MR");
				do2.setReason("VL: Group Category Mismatch with column7A and  column 7B" + NOT_IN_APPENDIX_J_FORMAT);
				do2Repo.save(do2);
				return false;
			}
		}
		if (newDo2.getId()==do2.getId() || (do2.getStatus()!=null && !do2.getStatus().equals(Do2Manager.VALID))) {
			if (employee.getAttestationDate()!=null && !employee.getAttestationDate().equals(do2.getFromDate())) {
				buffer.append(ALREADY_ATTESTED);
				validity = false;
			}
			
			if (employee.getEnrolmentDate()!=null && do2.getFromDate().before(employee.getEnrolmentDate())) {
				buffer.append(INVALID_FROM_DATE);
				validity = false;
			}
 
			if (employee.getEnrolmentDate()!=null && employee.getNbsubDate()!=null && 
				 employee.getEnrolmentDate().equals(employee.getNbsubDate())) {
				 buffer.append(ENROLL_JCO);
				 validity = false;
			}
			
			if (!isValidGroup(do2)) {
				validity = false;
				buffer.append(INVALID_GROUP);
			}

			if (!(isValidCategory(do2))) {
				// do2.setStatus("MR"); commented on 14.06.2025
				buffer.append(INVALID_CATEGORY + NOT_IN_APPENDIX_J_FORMAT);
				validity = false;
			}
	// As per the decision taken in a meeting with ASDC in CGDA office on 23rd Aug, 2013)		
			/*if (employee.getInitialMusteringDate()!=null) {
				if (!isAttestMusterSameGroupCategory(do2, employee)) {
					validity = false;
					
				}
			}*/
			
			if (!validity) {
				if ((do2.getStatus() == null)||(do2.getStatus() != null && !do2.getStatus().equals(Do2Manager.PENDING))) {
					do2.setStatus(Do2Manager.INVALID);
				}
			} else {
				
				updateBdtInfo(do2);
				do2.setStatus(Do2Manager.VALID);
				employee=Utils.getCurrentEmployee(do2.getArmyNo());
				List<Do2> do2List = do2Repo.findByDescColC(employee.getId(),"INCR","ATTSBM");
			 
				if (newDo2.getDescription().equals("ATTSBM") && employee.getInitialMusteringDate() != null   && 
					employee.getInitialMusteringDate().after(do2.getFromDate()) && do2List.size() == 0) {
					attestIncrCal(do2);
				}				
				if (!isEnrMstDo2Exists(do2)) {
					if (employee.getSportHavildarFlag() != null && employee.getSportHavildarFlag()== true && 
						employee.getSportHavildarDate().equals(employee.getEnrolmentDate())) {
							;
					} else if (employee.getGraduateHavildarFlag() != null && employee.getGraduateHavildarFlag() == true && 
							employee.getGraduateHavildarDate().equals(employee.getEnrolmentDate())) {
							;
					} else {
						createSysDo2(do2);
					}
				}
				if (do2.getFromDate().after(DateManager.getSixthCpcAllowanceDate())) {
					if (!AuditCageHelper.isTdmergDo2Exist(do2)){
						String presentTrade = null;
						String sldrClass = null;
						String previousTrade=do2.getColumnB().trim();
						String group ="";
						if(do2.getColumnA()!=null && do2.getColumnA().length()>3){
							group = do2.getColumnA().charAt(3)+"";
						}
						List<GroupCategory> grpCatList = gcRepo.findBySoldierGroupAndSoldierCategory(group, previousTrade);
								
						 
						if ((grpCatList.size() > 0)	&& (previousTrade.equals(grpCatList.get(0).getSoldierCategory()))) {
							 if (grpCatList.get(0).getMergedAbbr() != null){				
									presentTrade = grpCatList.get(0).getMergedAbbr();
									AuditCageHelper.createMergedTradeDo2(do2, sldrClass, previousTrade, presentTrade);
							 }
						}				
					}
				}
			}
		}
		
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
						return newAuditCage;
 		}
		
		newAuditCage.setLra((int) Math.round(AuditCageHelper.getLraRate(newAuditCage)));
		
		newAuditCage.setPmha((int)AuditCageHelper.getPmhaRate(newAuditCage.getDo2(), newAuditCage));
		if(!newAuditCage.getDo2().getFromDate().before(DateManager.getSeventhCpcDate())) {
			newAuditCage.setMsPay(5200);
		}
		
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PROMOT+"");
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
						Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
			 			if (emp.getAttestationDate()==null) {
			 				emp.setAttestationDate(do2.getFromDate());
			 				empRepo.save(emp);
			 			}		
			 		}
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_PROMOT);
		 
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
						Employee emp = Utils.getCurrentEmployee(refDo2.getArmyNo());
			 			if (emp.getAttestationDate()!=null) {
			 				emp.setAttestationDate(null);
			 				empRepo.save(emp);
			 			}		
			 		} else {
			 			reverseEmployeeStatus(currentDo2, refDo2);
			 		}
		return Do2Manager.SUCCESS;
	}

	public Date attestIncrCal(Do2 do2) {
		Date attesDate = do2.getFromDate();
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (employee.getAttestationDate().before(employee.getInitialMusteringDate())) {
			employee.setIncrementDate(attesDate);
			if (!(Utils.isDateAfterPreviousQuarterEnding(employee.getIncrementDate()))) {
				boolean done = false;
				for (; !done;) {
					createSysIncrDo2(do2);
					if (Utils.isDateAfterPreviousQuarterEnding(employee.getIncrementDate())) {
						done = true;
					}
				}
			}
		}
		return null;
	}

	public void createSysIncrDo2(Do2 do2) {
		Date dni = DateManager.getToday();
		Calendar empIncrDate = new GregorianCalendar();
		empIncrDate.setTimeInMillis(employee.getIncrementDate().getTime());
		int year = empIncrDate.get(Calendar.YEAR) + 1;
		int month = 0;
		if (do2.getFromDate().before(DateManager.getSixthCpcDate())	&& year >= 2006) {
			month = 7;
		} else {
			month = empIncrDate.get(Calendar.MONTH) + 1;
		}
		int date = 01;
		if (empIncrDate.get(Calendar.DATE) != 01) {
			try {
				dni = DateManager.getDate(year, month, date);
			} catch (Exception e) {
				Utils.handleException(e);
			}
		} else {
			dni = employee.getIncrementDate();
		}
		List<Do2> duplicateDo2List = do2Repo.findByDescFromDateEqualValidAndPending(employee.getId(), Do2Manager.VALID,Do2Manager.INVALD, "INCR", dni);
			 
		if (duplicateDo2List == null || duplicateDo2List.size() == 0){
			Do2 incrDo2 = new Do2();
			incrDo2.setEmployee(employee);
			incrDo2.setArmyNo(do2.getArmyNo());
			incrDo2.setBatchNo(do2.getBatchNo());
			incrDo2.setUnitCode(do2.getUnitCode());
			incrDo2.setUnit(do2.getUnit());
			incrDo2.setDo2Group("IA");
			incrDo2.setCheckDigit(do2.getCheckDigit());
			incrDo2.setDo2Type("0");
			incrDo2.setDo2No("6666");
			incrDo2.setDo2ItemNo("666");
			incrDo2.setDo2Date(dni);
			incrDo2.setDescription("INCR");
			incrDo2.setFromDate(dni);
			incrDo2.setColumnA(null);
			incrDo2.setColumnB(null);
			incrDo2.setColumnC(do2.getDescription());
			incrDo2.setStatus(Do2Manager.PENDING);
			incrDo2.setReason(null);
			incrDo2.setQe(Utils.getCurrentQuarter());
			incrDo2.setInputQe(Utils.getCurrentQuarter());
	//		incrDo2.setAcceptedQe(Utils.getCurrentQuarter());
			incrDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
			incrDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
			incrDo2.setAuditCage(true);	
			incrDo2.setReviewed(false);
			do2Repo.save(incrDo2);
		}
		if (year == 2006) {
			try {
				employee.setIncrementDate(DateManager.getDate(year + 1, month, date));
			} catch (Exception e) {
				Utils.handleException(e);
			}
		} else {
			employee.setIncrementDate(Utils.getNextIncrementDate(employee.getIncrementDate()));
		}
		
		empRepo.save(employee);
	}

	private boolean isEnrMstDo2Exists(Do2 do2) {
		boolean enrmstdo2exists=false;
		Employee employee =do2.getEmployee();
		List<Do2> do2List =do2Repo.findByDescValidAndPending(employee.getId(),Do2Manager.VALID,Do2Manager.PENDING,"ENRMST");
		 
		if(do2List.size() > 0) {
			enrmstdo2exists=true;
		}
		/*if (do2List.size() > 0){
			for (Do2 enrmstdo2 : do2List){
				if (enrmstdo2.getColumnB()!= null && enrmstdo2.getColumnB().equals(do2.getColumnB())){
					enrmstdo2exists=true;
				}
			}
		}*/
		if (enrmstdo2exists == true) {
			return true;
		} else {
			return false;
		}
	}

	// *********************************************************************************
	public boolean isAlreadyAttested(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
	 	if (employee.getAttestationDate()==null	|| 
	 	   (employee.getAttestationDate() != null && employee.getAttestationDate().equals(do2.getFromDate())))
			return false;
		else
			return true;
	}

	public boolean isAttsnBeforeMuster(Do2 do2) {

		if (employee.getInitialMusteringDate() == null
				|| (employee.getInitialMusteringDate() != null && do2
						.getFromDate().before(
								employee.getInitialMusteringDate())))
			return true;
		else
			return false;
	}


	public boolean isValidGroup(Do2 do2) {
		char group = do2.getColumnA().charAt(3);
		if ((do2.getFromDate().equals(DateManager.getSixthCpcDate()) || do2.getFromDate().after(DateManager.getSixthCpcDate())
				&& (group == 'X' || group == 'Y')))
			return true;
		else {
			if (group == 'X' || group == 'Y' || group == 'Z')
				return true;
		}
		return false;
	}

	public boolean isValidCategory(Do2 do2) {
		List<GroupCategory> grpCatList = gcRepo.findBySoldierGroupAndSoldierCategory(do2.getColumnA().charAt(3)+"", do2.getColumnB().trim());
		if (grpCatList.size() > 0 ) {
			return true;
		}
			return false;
	}

	private void updateEmployee(Do2 do2) { 
		
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		Rank rank = null;
		if (employee.getGraduateHavildarFlag()!=null && employee.getGraduateHavildarFlag() == true && employee.getGraduateHavildarDate().equals(employee.getEnrolmentDate())) {
			rank=rankRepo.findByRankCode("40");
		} else if (employee.getSportHavildarFlag() !=null && employee.getSportHavildarFlag() == true && employee.getSportHavildarDate().equals(employee.getEnrolmentDate())) {
			rank=rankRepo.findByRankCode("40");
	    } else {
	    	rank=rankRepo.findByRankCode("20");
	    }		
		
		employee.setAttestationDate(do2.getFromDate());
		List<GroupCategory> grpCatList = gcRepo.findBySoldierGroupAndSoldierCategory( do2.getColumnA().charAt(3)+"", do2.getColumnB().trim());
		 
		if (grpCatList.size() > 0 ) {
			if (grpCatList.get(0).getRedesignatedAbbr() != null ) {
				gpcatg = grpCatList.get(0).getRedesignatedAbbr(); 
			} else if (grpCatList.get(0).getMergedAbbr() != null ) {
				gpcatg = grpCatList.get(0).getMergedAbbr();
			} else {
				gpcatg = grpCatList.get(0).getSoldierCategory();
			}
		}
		if (employee.getInitialMusteringDate()==null) {
			employee.setGroupCategory(gcRepo.findBySoldierCategory(gpcatg).get(0));
 			employee.setSubstantiveRank(rank);
			employee.setSubstantiveRankDate(do2.getFromDate());
 			employee.setStatusCode(scRepo.findByStatusCode("E"));
			employee.setTermsColourService(rank.getColorService());
			employee.setTermsMaxAgeRetention(rank.getMaximumAge());
		} else if (do2.getFromDate().before(employee.getInitialMusteringDate())) {
	 			employee.setSubstantiveRank(rank);
				employee.setSubstantiveRankDate(do2.getFromDate());
	 			employee.setStatusCode(scRepo.findByStatusCode("E"));
				employee.setTermsColourService(rank.getColorService());
				employee.setTermsMaxAgeRetention(rank.getMaximumAge());
		}
		empRepo.save(employee);
	}
 
	public void reverseEmployeeStatus(Do2 currentDo2, Do2 refDo2) {

		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());

		if (employee.getInitialMusteringDate()==null) {
			employee.setGroupCategory(null);
			Rank rank = null;
			if (employee.getGraduateHavildarFlag() != null && employee.getGraduateHavildarFlag() == true 
					&& employee.getGraduateHavildarDate() !=null && employee.getGraduateHavildarDate().equals(employee.getEnrolmentDate())) {
				rank=rankRepo.findByRankCode("40");
			} else if (employee.getSportHavildarFlag() != null  && employee.getSportHavildarFlag() == true 
					&& employee.getSportHavildarDate() != null && employee.getSportHavildarDate().equals(employee.getEnrolmentDate())) {
				rank=rankRepo.findByRankCode("40");
		    } else {
		    	rank=rankRepo.findByRankCode("10");
		    }
			employee.setSubstantiveRank(rank);
			employee.setSubstantiveRankDate(employee.getEnrolmentDate());
			employee.setStatusCode(scRepo.findByStatusCode("R"));
			employee.setStatusEffectDate(employee.getEnrolmentDate());
			employee.setTermsColourService(rank.getColorService());
			employee.setTermsMaxAgeRetention(rank.getMaximumAge());
		}
		employee.setAttestationDate(null);
		empRepo.save(employee);
	}

	private void createSysDo2(Do2 do2) {  
		// Generation Of ENRMST DoII To Start Sepoy Pay From Enrollment Date
		Employee employee =do2.getEmployee();
		 
		Do2 enrMstDo2 = new Do2();
		enrMstDo2.setDescription("ENRMST");
		
		//from date will be 01032012 for enrmst for TA for the do2 is before cut off date
		if (employee.getRegularTaFlag()!=null && employee.getRegularTaFlag().equals("T")){
			if(AuditCageHelper.isBaseAfterEnrollDate(do2)){
				enrMstDo2.setFromDate(DateManager.getNextDate(DateManager.getTaCutOffDate()));
			} else {
				enrMstDo2.setFromDate(employee.getEnrolmentDate());
			}
		} else {
			enrMstDo2.setFromDate(employee.getEnrolmentDate());	
		}
	//	enrMstDo2.setFromDate(employee.getEnrolmentDate());
		enrMstDo2.setEmployee(employee);
		enrMstDo2.setUnitCode(do2.getUnitCode());
		enrMstDo2.setUnit(do2.getUnit());
		enrMstDo2.setDo2Group("IA");
		enrMstDo2.setDo2Type(do2.getDo2Type());
		enrMstDo2.setDo2No(do2.getDo2No());
		enrMstDo2.setDo2ItemNo(do2.getDo2ItemNo());
		enrMstDo2.setDo2Date(do2.getDo2Date());
		if (do2.getBatchNo()!=null)
			enrMstDo2.setBatchNo(do2.getBatchNo());
		if (do2.getColumnA()!=null) 
			enrMstDo2.setColumnA(do2.getColumnA());
		if (do2.getColumnB()!=null)
			enrMstDo2.setColumnB(do2.getColumnB());
		if (do2.getColumnC()!=null)
			enrMstDo2.setColumnC(do2.getColumnC());
		enrMstDo2.setQe(Utils.getCurrentQuarter());
		enrMstDo2.setAcceptedQe(Utils.getCurrentQuarter());
		enrMstDo2.setStatus(Do2Manager.PENDING);
		enrMstDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
		enrMstDo2.setAuditCage(true);
		enrMstDo2.setArmyNo(do2.getArmyNo());
		enrMstDo2.setCheckDigit(do2.getCheckDigit());
		enrMstDo2.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		enrMstDo2.setRefDo2No(Utils.getReferenceDo2No(do2));
		enrMstDo2.setReviewed(false);
		do2Repo.save(enrMstDo2);
		if(employee.getGraduateHavildarFlag()!=null && employee.getGraduateHavildarFlag()==true && employee.getGraduateHavildarDate().equals(employee.getEnrolmentDate()) ){
			;
		}else if(employee.getSportHavildarFlag() !=null && employee.getSportHavildarFlag()==true && employee.getSportHavildarFlag().equals(employee.getEnrolmentDate()) ){
			;
		}
		else{
			AuditCageHelper.insertDaDo2s(employee, do2);
			AuditCageHelper.insertLraDo2s(employee, do2);
			System.out.println(" ATTSBM ...................... .. " );
			if (!employee.getRegularTaFlag().equals("T")) {
				if (!employee.getEnrolmentDate().before(DateManager.getSeventhCpcDate())) { System.out.println("7th cpc increment generation ....................... ");
					AuditCageHelper.insertIncrementDos2ForAttestMusterIn7cpc(do2, employee.getEnrolmentDate(), do2.getDescription());
					System.out.println("7th cpc increment generation ....................... ");
				}
			}
		}
		List<Do2> duplicateDo2List=null;
		// Below Statement Is For Generating ALWREV For MSPay (01.09.2008)
		if (employee.getAttestationDate().before(DateManager.getSixthCpcAllowanceDate())) {
			try {
				duplicateDo2List = do2Repo.findByDescFromDateEqualValidAndPending(employee.getId(),Do2Manager.VALID,Do2Manager.PENDING,"ALWREV",DateManager.getDate("2008-09-01"));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (duplicateDo2List == null || duplicateDo2List.size() == 0){
				AuditCageHelper.insertAllowanceRevisionDo2(employee, DateManager.getSixthCpcAllowanceDate(), do2);
			}
		}
		// Below Statement Is For Generating ALWREV For Dearness Pay
		// (01.04.2004)
		if (employee.getEnrolmentDate().before(DateManager.getDPDate())) {
				try {
					duplicateDo2List =do2Repo.findByDescFromDateEqualValidAndPending(employee.getId(),Do2Manager.VALID,Do2Manager.PENDING,"ALWREV",DateManager.getDate("2004-04-01"));
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				 
				if (duplicateDo2List == null || duplicateDo2List.size() == 0){
					AuditCageHelper.insertAllowanceRevisionDo2(employee, DateManager.getDPDate(), do2);
				}
		}
		// Below Statement Is For Generating PAYFIX (01.01.2006)
		if (!(employee.getEnrolmentDate().after(DateManager.getSixthCpcDate()))) {
			  try {
				duplicateDo2List = do2Repo.findByDescFromDateEqualValidAndPending(employee.getId(),Do2Manager.VALID,Do2Manager.PENDING,"PAYFIX",DateManager.getDate("2006-01-01"));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (duplicateDo2List == null || duplicateDo2List.size() == 0){
				AuditCageHelper.insertPayfixDo2(employee, DateManager.getSixthCpcDate());
			}
		}
		
		// Below Statement Is For Generating FIX7PC (01.01.2016)
		if (!(employee.getEnrolmentDate().after(DateManager.getSeventhCpcDate()))) {
			
			try {
				duplicateDo2List = do2Repo.findByDescFromDateEqualValidAndPending(employee.getId(),Do2Manager.VALID,Do2Manager.PENDING,"FIX7PC",DateManager.getDate("2016-01-01"));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (duplicateDo2List == null || duplicateDo2List.size() == 0){
				AuditCageHelper.insertFix7pcDo2(employee, DateManager.getSeventhCpcDate());
			}
		}
		
		// Below Statement Is For Generating ALWREV For Allowances (01.07.2017)
		if (!(employee.getEnrolmentDate().after(DateManager.getSeventhCpcAllowanceDate()))) {
			 
			try {
				duplicateDo2List = do2Repo.findByDescFromDateEqualValidAndPending(employee.getId(),Do2Manager.VALID,Do2Manager.PENDING,"ALWREV",DateManager.getDate("2017-07-01"));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
				 

			if (duplicateDo2List == null || duplicateDo2List.size() == 0){
				AuditCageHelper.insertAllowanceRevisionDo2(employee, DateManager.getSeventhCpcAllowanceDate(),do2);
			}
		}
	}

	public boolean isAttestMusterSameGroupCategory(Do2 do2, Employee employee) {
		
 		String empcatg = null;		
		if (employee.getGroupCategory()!=null) {
			if (employee.getGroupCategory().getMergedAbbr()!=null )  {
				empcatg = employee.getGroupCategory().getMergedAbbr();
			} else if (employee.getGroupCategory().getRedesignatedAbbr()!=null )  {
				empcatg = employee.getGroupCategory().getRedesignatedAbbr();
			} else {
				empcatg = employee.getGroupCategory().getSoldierCategory();
			}
		}
		
		List<GroupCategory> grpCatList = gcRepo.findBySoldierGroupAndSoldierCategory(do2.getColumnA().charAt(3)+"", do2.getColumnB().trim());
				 
		if (grpCatList.size() > 0 ) {
			if (grpCatList.get(0).getRedesignatedAbbr() != null ) {
				gpcatg = grpCatList.get(0).getRedesignatedAbbr(); 
			} else if (grpCatList.get(0).getMergedAbbr() != null ) {
				gpcatg = grpCatList.get(0).getMergedAbbr();
			} else {
				gpcatg = grpCatList.get(0).getSoldierCategory();
			}
		}
		
		if (empcatg == null ) {
			buffer.append("FK_GROUP_CATEGORY in employee table not available");
			return false;
		} else {
			if (empcatg.equals(gpcatg)) {
				return true;
			} else {
//				buffer.append(ATTEST_MUSTER_GROUP_CATEGORY_DIFFERS);
				return false;
			}
		}
	}
	
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
	}
}