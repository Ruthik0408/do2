package itsdc.dolphin.do2.auditcage;
 
 

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Arrays;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

@RestController
public class Hcommn extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
		 
	Employee currentEmployeeRecord, previousEmployeeRecord;
	EmployeeServiceDetail esd;
	private StringBuffer buffer = new StringBuffer();
	private boolean validity;
 	private Rank currentRank;
  	List<PayScale> payScaleList;
	
	static public final String NOT_YET_JCO    = "AU:PBOR is not SUB/RIS/SUBMAJ/RISMAJ.\n";
	static public final String NOT_YET_HONYLT = "AU:Honorary Lieutenant is not yet adjusted.\n";
	static public Date revised_appxj_impl_date ;
	static public List<String> colBList = Arrays.asList("SUB","SUBMAJ","RIS","RISMAJ");
	static public String NOT_IN_APPENDIX_J_FORMAT = "VL:Do2 is not in required Appendix J format.\n ";	
	public static final String PBOR_ON_LEAVE = "AU:Individual is on leave on the date of HCOMMN. \n";
	static public final String INCR_NOT_DUE_ON_OPTION_DATE  = "AU:Increment Not Due In Pre-Promoted/Pre-Upgraded Grade On Opted Date \n";
	static public final String MACPS_ALREADY_ADJUSTED = "VL:OPTION FOR PAY FIXATION NOT POSSIBLE AS MACPS ALREADY DRAWN.\n";
	
	@Override
	public boolean isValid(Do2 do2) {
		 
		validity = true;
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		currentEmployeeRecord = Utils.getCurrentEmployee(do2.getArmyNo());
		currentRank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PROMOT");   		
		if (currentRank == null) {
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(Do2Manager.SERVICE_DETAIL_MISSING);
			do2Repo.save(do2);
			return false;
		}
		
		if (isPborOnRegularLeave(do2, employee)) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(PBOR_ON_LEAVE);
			validity = false;
		}
		
		if (do2.getFromDate().after(DateManager.getSeventhCpcDate()) && do2.getColumnC()!=null && isMacpsAlreadyAdjusted(do2, employee)) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(MACPS_ALREADY_ADJUSTED);
			validity = false;
		}
		
		if (do2.getFromDate().after(DateManager.getSeventhCpcDate()) && do2.getColumnC()!=null)			
			if (!doesIncrExistsOnOptionDate(do2, employee)) {
				buffer.append(INCR_NOT_DUE_ON_OPTION_DATE);
				validity=false;
				do2.setStatus(Do2Manager.INVALID);
			}
		
		if (do2.getColumnA().equals("HONYLT") || (do2.getColumnA().equals("HONYCAPT"))) {
			validity = true; 
		} else {
			validity = false;
			do2.setStatus("MR");
			do2.setReason("Column 7A should consists HONYLT or HONYCAPT" + NOT_IN_APPENDIX_J_FORMAT);
		}
		
		revised_appxj_impl_date = DateManager.getRevisedAppendixJImplementationDate();
		if (do2.getColumnA().equals("HONYLT")) {
			if (!do2.getDo2Date().before(revised_appxj_impl_date)){
				if (!colBList.contains(do2.getColumnB())){
					do2.setStatus(Do2Manager.INVALID);
					do2.setReason("Rank in Col-7b should be SUB/RIS/SUBMAJ/RISMAJ");
					validity = false;
				}
			}
		}
		if (do2.getColumnA().equals("HONYLT")) {
			if (currentRank!=null && (currentRank.getRankCode().equals("60") 
					|| currentRank.getRankCode().equals("70"))) {
				;
		 	} else {
				do2.setStatus(Do2Manager.INVALID);
				do2.setReason(NOT_YET_JCO);
				validity = false;
			}			
		} else if (currentRank!=null && currentRank.getRankCode().equals("80")) {
				;
		} else {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(NOT_YET_HONYLT);
			validity = false;
		}
				
		if (validity == true) {
			do2.setStatus(Do2Manager.VALID);
			if (currentEmployeeRecord.getRegularTaFlag() != null && currentEmployeeRecord.getRegularTaFlag().equals("R")) {
				if (!do2.getFromDate().before(DateManager.getSixthCpcDate()) 
						&& do2.getFromDate().before(DateManager.getSeventhCpcDate()))	 {
					Date xDate = do2.getFromDate();
					Calendar yy = new GregorianCalendar();
					yy.setTimeInMillis(xDate.getTime());
					int zz = yy.get(Calendar.YEAR);

					Calendar chkDate1 = new GregorianCalendar(zz, Calendar.JANUARY, 01);
					Calendar chkDate2 = new GregorianCalendar(zz, Calendar.JULY, 01);

					Date jan01 = chkDate1.getTime();
					Date jul01 = chkDate2.getTime();
					//todate of do2 is superannuation date
					 if ((xDate.after(jan01)) && (xDate.before(jul01)) && 
								(do2.getToDate()!= null && do2.getToDate().after(jul01))
								&& !isHonyGradeBeforeCurrentDo2(do2)) {
									createProfixDo2(currentEmployeeRecord, do2);
							}
				} else if (do2.getFromDate().after(DateManager.getSeventhCpcDate()) && do2.getColumnC()!=null) {
					if (!isHonyGradeBeforeCurrentDo2(do2)) {
						createProfixDo2(currentEmployeeRecord, do2);
					}
				}
			}
		}
		
		if (validity==false) {
			do2.setStatus(Do2Manager.INVALID);
		}
		
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
			
		String rankCode = null;
		
		if (newAuditCage.getDo2().getColumnA().equals("HONYLT")){
			rankCode = "80";
		} else {
			rankCode = "90";
		}	
		
		Rank rank =rankRepo.findByRankCode(rankCode);
	 	
		if (rank!=null) {
			payScaleList =psRepo.findByRankIdFromDateBetweenOrFromDateGreaterThanEqual(rank.getId(),newAuditCage.getFromDate());
			 
		}
		
		if (payScaleList!=null && payScaleList.size()>0) {
			
			if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
				set5cpcPay(newAuditCage, baseAuditCage, payScaleList);
			} else if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {				
					set6cpcPay(newAuditCage, baseAuditCage, payScaleList);				
					if (newAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
						newAuditCage.setMsPay(0);
					} else {
						newAuditCage.setMsPay(getMsPay(newAuditCage.getDo2()));	
					}
					if (newAuditCage.getTransport()!=null && newAuditCage.getTransport()!=0) {
						newAuditCage.setTransport(newAuditCage.getTransport()*2);
					}
					if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
						setOldBasicpay(newAuditCage, rank);
					}
			} else if (!newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate()) && newAuditCage.getGradePay()==0) {
					set7cpcPay(newAuditCage, baseAuditCage);
					set6cpcOldPay(newAuditCage, baseAuditCage, payScaleList);
					newAuditCage.setPayLevel(Do2Manager.getPayLevelPromot(rankCode));
			} else if (newAuditCage.getPayLevel()!=null && newAuditCage.getPayLevel().equals("0") 
					&& !newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())) {
					set6cpcPay(newAuditCage, baseAuditCage, payScaleList);
					set6cpcOldPay(newAuditCage, baseAuditCage, payScaleList);
			} 		 				
		}
		newAuditCage.setLra(0);
		newAuditCage.setPmha(0);
		newAuditCage.setXGroupPay(0);
		newAuditCage.setClassPay(0);
		newAuditCage.setOldClassPay(0);
		newAuditCage.setMsPay(getMsPay(newAuditCage.getDo2()));
		
		
		// above lines commented and amended as below due to sevencpc
		/*
		if (payScaleList!=null && payScaleList.size()>0) {
			if (newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
				set5cpcPay(newAuditCage, baseAuditCage, payScaleList);
			} else if (newAuditCage.getFromDate().before(DateManager.getSeventhCpcDate())){
				set6cpcPay(newAuditCage, baseAuditCage,payScaleList);
				newAuditCage.setXGroupPay(0);
				if (newAuditCage.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
					newAuditCage.setMsPay(0);
				} else {
					newAuditCage.setMsPay(getMsPay(newAuditCage.getDo2()));	
				}
				if (newAuditCage.getTransport()!=null && newAuditCage.getTransport()!=0) {
					newAuditCage.setTransport(newAuditCage.getTransport()*2);
				}
				if (!newAuditCage.getFromDate().before(DateManager.getSixthCpcDate())){
					setOldBasicpay(newAuditCage, rank);
				}
			} else {
				set7cpcPay(newAuditCage, baseAuditCage);
				set6cpcOldPay(newAuditCage,baseAuditCage,payScaleList);
				
			}
			
			newAuditCage.setLra(0);
			newAuditCage.setPmha(0);
		} else {
			
		}
		*/
		return newAuditCage;
	}
	
	 
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		updateBdtInfo(do2);
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		stopInadmissibleAllow(newAuditCage);
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 do2, Do2 refDo2) {
		restorePreviousStatusInEmployee(refDo2);
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_PROMOT);
		return Do2Manager.SUCCESS;
	}

	public void updateEmployee(Do2 do2) {
	
	    String rankCode = null;
		if (do2.getColumnA().equalsIgnoreCase("HONYLT")){
			rankCode = "80";
		} else {
			rankCode = "90";
		}
		Rank rank =rankRepo.findByRankCode(rankCode); 
				 
	
		currentEmployeeRecord = Utils.getCurrentEmployee(do2.getArmyNo());
		currentEmployeeRecord.setSubstantiveRank(rank);
		currentEmployeeRecord.setSubstantiveRankDate(do2.getFromDate());
		currentEmployeeRecord.setActingRank(rank);
		currentEmployeeRecord.setActingRankDate(do2.getFromDate());

		if (do2.getColumnA().equalsIgnoreCase("HONYLT")) {
			currentEmployeeRecord.setHonyLieuDate(do2.getFromDate());
		} else {
			currentEmployeeRecord.setHonyCaptainDate(do2.getFromDate());
		}
        currentEmployeeRecord.setIncrementDate(getIncrementDate(do2));
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_PROMOT+"");
	}
	 
	public void stopInadmissibleAllow(AuditCage newAuditCage) {
		List<String> allowanceList = Arrays.asList("CILQ");
		AuditCageHelper.removeAllowances(allowanceList, newAuditCage);	
	}
	
	 
	public void set6cpcPay(AuditCage nAuditCage,AuditCage bAuditCage ,List<PayScale> payScaleList) {
		List<Do2>macpList=do2Repo.findByDescColA(nAuditCage.getEmployee().getId(),Do2Manager.VALID,"MACPS","HONYGRADE");
				
		 
		if (macpList.size() > 0){
			if (macpList.get(0).getFromDate() != null && !macpList.get(0).getFromDate().after(nAuditCage.getDo2().getFromDate())
				&& nAuditCage.getDo2().getColumnA().equals("HONYLT")){
				return;
			}
		}
		Date xDate = nAuditCage.getDo2().getFromDate();
		Calendar yy = new GregorianCalendar();
		yy.setTimeInMillis(xDate.getTime());
		int zz = yy.get(Calendar.YEAR);

		Calendar chkDate1 = new GregorianCalendar(zz, Calendar.JANUARY, 01);
		Calendar chkDate2 = new GregorianCalendar(zz, Calendar.JULY, 01);

		Date jan01 = chkDate1.getTime();
		Date jul01 = chkDate2.getTime();
		//todate of do2 is superannuation date
		if (xDate.after(jan01) && xDate.before(jul01)) {
			int nbasicpay = (bAuditCage.getBasicPay() + Utils.getIncrmentRoundedToTen(
						(bAuditCage.getBasicPay()+bAuditCage.getGradePay()+(bAuditCage.getXGroupPay()==null ? 0 : 
							bAuditCage.getXGroupPay())), bAuditCage.getFromDate()));
			int nbasicpay1 = (nbasicpay + Utils.getIncrmentRoundedToTen(
					(nbasicpay+bAuditCage.getGradePay()+(bAuditCage.getXGroupPay()==null ? 0 : 
						bAuditCage.getXGroupPay())), bAuditCage.getFromDate()));
			
			if (nAuditCage.getDo2().getToDate() != null && nAuditCage.getDo2().getToDate().after(jul01)){
				if (nbasicpay1 < payScaleList.get(0).getStartScale())
					nAuditCage.setBasicPay(payScaleList.get(0).getStartScale());
			}
			if (nAuditCage.getDo2().getToDate() == null || 
			   (nAuditCage.getDo2().getToDate() != null && nAuditCage.getDo2().getToDate().before(jul01))) {
				nAuditCage.setBasicPay(nbasicpay);
				if (nAuditCage.getBasicPay() < payScaleList.get(0).getStartScale())
					nAuditCage.setBasicPay(payScaleList.get(0).getStartScale());
			}
			
		} else {
			nAuditCage.setBasicPay((bAuditCage.getBasicPay() + Utils.getIncrmentRoundedToTen(
			(bAuditCage.getBasicPay()+bAuditCage.getGradePay()+(bAuditCage.getXGroupPay()==null ? 0 : bAuditCage.getXGroupPay())), bAuditCage.getFromDate())));

			if (nAuditCage.getBasicPay() < payScaleList.get(0).getStartScale())
				nAuditCage.setBasicPay(payScaleList.get(0).getStartScale());
		}	
		nAuditCage.setGradePay(payScaleList.get(0).getGradePay());
	}

	public void set5cpcPay(AuditCage nAuditCage,AuditCage bAuditCage, List<PayScale> payScaleList) {
		
		nAuditCage.setBasicPay(payScaleList.get(0).getStartScale());
		
		if (!nAuditCage.getFromDate().before(DateManager.getDPDate()) 
				&& nAuditCage.getFromDate().before(DateManager.getSixthCpcDate())) {
			nAuditCage.setDp((int) Math.round(nAuditCage.getBasicPay() * 0.50));
		}
	}
	 
	public void restorePreviousStatusInEmployee(Do2 refDo2) {
		Do2 previousRankDo2 = null;
		String rankCode = null;
	 
		Employee employee = Utils.getCurrentEmployee(refDo2.getArmyNo());
		List<EmployeeServiceDetail> esdList=esdRepo.findByDo2IdStatusFromDateEqual(refDo2.getEmployee().getId(),refDo2.getId(),Do2Manager.ESD_PROMOT+"", refDo2.getFromDate());
		if(esdList!=null && esdList.size()>0) {
			for(EmployeeServiceDetail esd:esdList) {
				esdRepo.delete(esd);
			}
		}
	 
		
		esdList =esdRepo.findByFromDateLessThanAndStatusOrderByFromDateDesc(refDo2.getEmployee().getId(),Do2Manager.ESD_PROMOT+"",refDo2.getFromDate()); 
		 
		if (esdList.size() > 0) {
			previousRankDo2 = esdList.get(0).getDo2();
			esdList.get(0).setToDate(null);
		}
		
		if (previousRankDo2.getDescription().equals("PROMOT")) {
	 		if (previousRankDo2.getColumnA().substring(2).equals("SUB")) {
	 			rankCode = "60";
	 		} else if (previousRankDo2.getColumnA().substring(2).equals("SUBMAJ")) {
	 			rankCode = "70";
	 		}
		} else if (previousRankDo2.getDescription().equals("HCOMMN")) {
	 		if (previousRankDo2.getColumnA().equals("HONYLT")) {
	 			rankCode = "80";
	 		}
		}

		Rank rank =rankRepo.findByRankCode(rankCode);

		
		if (refDo2.getColumnA().equalsIgnoreCase("HONYLT")) {
			employee.setHonyLieuDate(null);
			employee.setActingRank(rank);
			employee.setActingRankDate(previousRankDo2.getFromDate());
			employee.setSubstantiveRank(rank);
			employee.setSubstantiveRankDate(previousRankDo2.getFromDate());
		} else {
			employee.setHonyCaptainDate(null);
			employee.setActingRank(rank);
			employee.setActingRankDate(previousRankDo2.getFromDate());
			employee.setSubstantiveRank(rank);
			employee.setSubstantiveRankDate(previousRankDo2.getFromDate());
		}		
	}
	
	public void setOldBasicpay(AuditCage newAuditCage, Rank rank) {
		
		payScaleList =psRepo.findByRankIdFromDateBetweenOrFromDateGreaterThanEqual(rank.getId(),DateManager.get5CpcGroupChangeDate()) ;
		 
	 
		newAuditCage.setOldBasicPay(payScaleList.get(0).getStartScale());
	}
	
	@Override
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
	}
	 public  Date getIncrementDate(Do2 do2){
		Date nextIncrementDate = do2.getEmployee().getIncrementDate();
		Date xDate = do2.getFromDate();
		Calendar yy = new GregorianCalendar();
		yy.setTimeInMillis(xDate.getTime());
		int zz = yy.get(Calendar.YEAR);
		Calendar chkDate1 = new GregorianCalendar(zz, Calendar.JANUARY, 01);
		Calendar chkDate2 = new GregorianCalendar(zz, Calendar.JULY, 01);
		Date jan01 = chkDate1.getTime();
		Date jul01 = chkDate2.getTime();
		if (!do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			if (xDate.equals(jan01)) {
				nextIncrementDate = jul01;
			}
			chkDate1 = new GregorianCalendar(zz+1, Calendar.JANUARY, 01);
			if (xDate.after(jan01) && !xDate.after(jul01)) {
				nextIncrementDate = chkDate1.getTime();
			}
			chkDate2 = new GregorianCalendar(zz+1, Calendar.JULY, 01);
			if (xDate.after(jul01) && !xDate.after(chkDate1.getTime())) {
				nextIncrementDate = chkDate2.getTime();
			}
			if (do2.getToDate()!=null && do2.getToDate().before(nextIncrementDate)) {
				nextIncrementDate = do2.getEmployee().getIncrementDate();
			}			 
		} else if ((xDate.after(jan01)) && (xDate.before(jul01)) && 
			(do2.getToDate()!= null && do2.getToDate().after(jul01))) {
			nextIncrementDate = Utils.getNextIncrementDate(jan01);
		}
		return nextIncrementDate;
	
	}
	 public String createProfixDo2(Employee employee, Do2 do2) {
			 
			Date optionDate = DateManager.getCilqDate(do2.getFromDate());
			String colA = do2.getColumnA();
			List<Do2> chkFixDo2 = null;
			
			if (!do2.getFromDate().after(DateManager.getSeventhCpcDate())) {
				chkFixDo2 = do2Repo.findByDescFromDateEqualStatusNotEqual(employee.getId(),Do2Manager.CANCEL, "PROFIX", optionDate); 
			 	
			} else if (do2.getColumnC()!=null && do2.getColumnC().trim().length()==8) {
				optionDate = DateManager.getDateFromDdMmYyyy(do2.getColumnC());
				do2Repo.findByDescFromDateEqualStatusNotEqual(employee.getId(),Do2Manager.CANCEL, "PROFIX", optionDate);  
			}
			
			if (chkFixDo2!=null && chkFixDo2.size()>0) {
				return Do2Manager.FAILURE;	
			}
			
			if (optionDate!=null && do2.getToDate()!=null && optionDate.after(do2.getToDate())) {
				return Do2Manager.FAILURE;
			}
	     		
			Do2 profixDo2 = new Do2();
			
			if (do2.getBatchNo()!=null)
				profixDo2.setBatchNo(do2.getBatchNo());
			
			profixDo2.setTranscriptionType(Do2Manager.SYSTEM_GENERATED);
			profixDo2.setDo2Group(do2.getDo2Group());
			profixDo2.setDo2Type(do2.getDo2Type());
			profixDo2.setDo2No(do2.getDo2No());
		 	profixDo2.setDo2ItemNo(do2.getDo2ItemNo());
			profixDo2.setDo2Date(do2.getDo2Date());
			profixDo2.setDescription("PROFIX");
			if (do2.getUnit()!=null)
				profixDo2.setUnit(do2.getUnit());
			profixDo2.setUnitCode(do2.getUnitCode());
			profixDo2.setEmployee(employee);
			profixDo2.setArmyNo(do2.getArmyNo());
			profixDo2.setCheckDigit(do2.getCheckDigit());
			profixDo2.setFromDate(optionDate);
			profixDo2.setColumnA(colA);
			if (do2.getFromDate().after(DateManager.getSeventhCpcDate()) && do2.getColumnC()!=null) {
				profixDo2.setColumnB(do2.getColumnA());
			}
			profixDo2.setRefDo2No(Utils.getReferenceDo2No(do2));
			profixDo2.setStatus(Do2Manager.PENDING);
			profixDo2.setAuditCage(true);
			profixDo2.setQe(Utils.getCurrentQuarter());
			profixDo2.setAcceptedQe(Utils.getCurrentQuarter());
		 do2Repo.save(profixDo2);
	 		return Do2Manager.SUCCESS;
		}

	 
	
	 	public boolean isHonyGradeBeforeCurrentDo2(Do2 do2) {
	 		Date acpDate = null;
	 		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
	 		List<Do2> do2List =do2Repo.findByEmployeeIdAndDescriptionAndStatusAndColumnA(do2.getEmployee().getId(),"MACPS",Do2Manager.VALID,"HONYGRADE");
	 		
	 		if (do2List.size()>0){
	 			acpDate = do2List.get(0).getFromDate();
	 		}
	
	 		if (acpDate != null && acpDate.before(do2.getFromDate()))
	 			return true;

	 		return false;
	 	}
	 	public int getMsPay(Do2 do2){
	 		int mspay=0;
	 		if (!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())
	 				&& do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
	 			mspay=6000;
	 		} else if (!do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
	 			mspay=15500;
	 		}
	 		
			if (Utils.getPao().getPaoCode().equals(Do2Manager.AO_ESTTNO22)){
				mspay=0;
			}
			return mspay;
		}

	 	

	 	public void set7cpcPay(AuditCage nAuditCage,AuditCage bAuditCage) {
	 		String rankCode = null;
	 		Employee employee = Utils.getCurrentEmployee(nAuditCage.getDo2().getArmyNo());
	 		
	/*		List<Do2>macpList=Utils.getEntityManager().createQuery("select d from Do2 d where "
					+ " d.employee=:emp and d.description='MACPS' and d.columnA='HONYGRADE' and d.status='V'")
					.setParameter("emp", nAuditCage.getEmployee()).getResultList();
			
	 		if (macpList.size() > 0){
				if (macpList.get(0).getFromDate() != null && macpList.get(0).getFromDate().before(nAuditCage.getDo2().getFromDate())
					&& nAuditCage.getDo2().getColumnA().equals("HONYLT")) {
					return;
				}
			}  // Commented and Modified as Below To Handle Hqrs. Office Clarification On The Subject In 07/2020.
	*/
			if (nAuditCage.getDo2().getColumnA().equals("HONYLT"))
				rankCode = "80";
			else
				rankCode = "90";
			
			Rank promotRank =rankRepo.findByRankCode(rankCode);
			Integer newBasic = nAuditCage.getBasicPay();
		 
			Rank currentRank = Utils.getPborRank(nAuditCage.getDo2().getArmyNo(), nAuditCage.getDo2().getFromDate(), "PROMOT");   	
			
			String currentPayLevel = Do2Manager.getPayLevelPromot(currentRank.getRankCode());
			String promotPayLevel = Do2Manager.getPayLevelPromot(promotRank.getRankCode());
		
			System.out.println ( " Current pay level " +currentPayLevel   +" promot pay level " +promotPayLevel  +" new basic " +newBasic);
			// Added code for OPTFIX Line 582to 605
			
			List<Do2> optfixDo2List =do2Repo.findByDescFromDateEqual(nAuditCage.getEmployee().getId(),Do2Manager.VALID,"OPTFIX", nAuditCage.getFromDate()); 
				 
			
			
			if ((optfixDo2List!=null && optfixDo2List.size()>0) 
					|| (nAuditCage.getDo2().getColumnC()!=null && nAuditCage.getDo2().getColumnC().trim().length()>0)) {

				List<PayMatrix>paymatrixList1 = matrixRepo.findByPayLevelAndBasicPayGreaterThanEqualOrderByBasicPayAsc(promotPayLevel, newBasic);
				 
				
				if (paymatrixList1.size() > 0) {
					newBasic = paymatrixList1.get(0).getBasicPay();
				}
			
				nAuditCage.setBasicPay(newBasic);
				nAuditCage.setOldGradePay(Do2Manager.getSevenCpcGradepay(promotPayLevel));
				nAuditCage.setPayLevel(promotPayLevel);
				 acRepo.save(nAuditCage);
				return;
			} 
	//		List<Do2>macpList=do2Repo.findByDescStatusFromDateLessThan(null, currentPayLevel, promotPayLevel, revised_appxj_impl_date);
			List<Do2>macpList=do2Repo.findByDescColumnAStatusFromDateLessThan(nAuditCage.getEmployee().getId(), nAuditCage.getFromDate());
				 
	
		 	if (macpList!=null && macpList.size() > 0){
				if (macpList.get(0).getFromDate() != null && macpList.get(0).getFromDate().before(nAuditCage.getDo2().getFromDate())
					&& nAuditCage.getDo2().getColumnA().trim().equals("HONYLT")) {
					List<PayMatrix>paymatrixList1 =matrixRepo.findByPayLevelAndBasicPayGreaterThanEqualOrderByBasicPayAsc(promotPayLevel, newBasic); 
					 
					if (paymatrixList1.size() > 0) {
						newBasic = paymatrixList1.get(0).getBasicPay();
					}
				
					nAuditCage.setBasicPay(newBasic);
					nAuditCage.setOldGradePay(Do2Manager.getSevenCpcGradepay(promotPayLevel));
					nAuditCage.setPayLevel(promotPayLevel);
					acRepo.save(nAuditCage);
					return; 
				}
			}
			
			
	//		List<PayMatrix>paymatrixList =matrixRepo.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(currentPayLevel, newBasic); 
			List<PayMatrix>paymatrixList =matrixRepo.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(currentPayLevel, newBasic); 
					
				 
			if (paymatrixList.size() > 0) {
				newBasic = paymatrixList.get(0).getBasicPay();
			}
			
			List<PayMatrix>paymatrixList1 =matrixRepo.findByPayLevelAndBasicPayGreaterThanEqualOrderByBasicPayAsc(promotPayLevel, newBasic); 
		//	List<PayMatrix>paymatrixList1 =matrixRepo.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(promotPayLevel, newBasic); 
			
			if (paymatrixList1.size() > 0) {
				newBasic = paymatrixList1.get(0).getBasicPay();
			}
			System.out.println("yyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyy  newbasic  from Hcommn " +newBasic);
			nAuditCage.setBasicPay(newBasic);
			nAuditCage.setOldGradePay(Do2Manager.getSevenCpcGradepay(promotPayLevel));
			nAuditCage.setMsPay(getMsPay(nAuditCage.getDo2()));	
			nAuditCage.setPayLevel(promotPayLevel);
			acRepo.save(nAuditCage);
			employee.setIncrementDate(Utils.getSevenPcNextIncrementDate(nAuditCage.getDo2().getFromDate()));
			empRepo.save(employee);
			 
		}

	 
	 	public void set6cpcOldPay(AuditCage nAuditCage,AuditCage bAuditCage ,List<PayScale> payScaleList) {
			List<Do2>macpList=do2Repo.findByDescColA(nAuditCage.getEmployee().getId(),Do2Manager.VALID,"MACPS","HONYGRADE");
		 
			if (macpList.size() > 0){
				if (macpList.get(0).getFromDate() != null && !macpList.get(0).getFromDate().after(nAuditCage.getDo2().getFromDate())
					&& nAuditCage.getDo2().getColumnA().equals("HONYLT")){
					return;
				}
			}
			Date xDate = nAuditCage.getDo2().getFromDate();
			Calendar yy = new GregorianCalendar();
			yy.setTimeInMillis(xDate.getTime());
			int zz = yy.get(Calendar.YEAR);

			Calendar chkDate1 = new GregorianCalendar(zz, Calendar.JANUARY, 01);
			Calendar chkDate2 = new GregorianCalendar(zz, Calendar.JULY, 01);

			Date jan01 = chkDate1.getTime();
			Date jul01 = chkDate2.getTime();
			int xGroupPay = Utils.get6cpcXGroupPay(nAuditCage);
			//todate of do2 is superannuation date
			if (xDate.after(jan01) && xDate.before(jul01)) {
				int nbasicpay = (bAuditCage.getOldBasicPay() + Utils.getIncrmentRoundedToTen(
							(bAuditCage.getOldBasicPay()+bAuditCage.getOldGradePay()+xGroupPay), bAuditCage.getFromDate()));
				int nbasicpay1 = (nbasicpay + Utils.getIncrmentRoundedToTen(
						(nbasicpay+bAuditCage.getOldGradePay()+xGroupPay), bAuditCage.getFromDate()));
				
				if (nAuditCage.getDo2().getToDate() != null && nAuditCage.getDo2().getToDate().after(jul01)){
					if (nbasicpay1 < payScaleList.get(0).getStartScale())
						nAuditCage.setOldBasicPay(payScaleList.get(0).getStartScale());
				}
				if (nAuditCage.getDo2().getToDate() == null || 
				   (nAuditCage.getDo2().getToDate() != null && nAuditCage.getDo2().getToDate().before(jul01))) {
					nAuditCage.setOldBasicPay(nbasicpay);
					if (nAuditCage.getOldBasicPay() < payScaleList.get(0).getStartScale())
						nAuditCage.setOldBasicPay(payScaleList.get(0).getStartScale());
				}
				
			} else {
				nAuditCage.setOldBasicPay((bAuditCage.getOldBasicPay() + Utils.getIncrmentRoundedToTen(
				(bAuditCage.getOldBasicPay()+bAuditCage.getOldGradePay()+xGroupPay), bAuditCage.getFromDate())));

				if (nAuditCage.getOldBasicPay() < payScaleList.get(0).getStartScale())
					nAuditCage.setOldBasicPay(payScaleList.get(0).getStartScale());
			}	
			nAuditCage.setOldGradePay(payScaleList.get(0).getGradePay());
		}
	 	
	 	public boolean isPborOnRegularLeave(Do2 do2, Employee employee) {
	        Date adjustedQeDate = null; 
			Date dolphinStartDate = Utils.getDolphinStartDate();
			if (do2.getAcceptedQe() != null) {
				try {
					adjustedQeDate = DateManager.getDate(do2.getAcceptedQe().substring(3)  + "-" +do2.getAcceptedQe().substring(0,2) + "-01");
				} catch (Exception e) {
					Utils.handleException(e);
				}							 
			}
			if (adjustedQeDate != null && adjustedQeDate.before(dolphinStartDate)){
				return false;
			}
		 
			List<Do2> do2List =do2Repo.findLeaveDo2ForHcomm(do2.getEmployee().getId(),do2.getFromDate(),Do2Manager.VALID,Do2Manager.PENDING,"CL","EXTNCL");
					
			 

			for (Do2 leaveDo2 : do2List) {
				if (Do2Manager.getAllLeaveTypes().contains(leaveDo2.getDescription()))
					return true;
			}
			return false;
		}
	 	
	 	public boolean doesIncrExistsOnOptionDate(Do2 do2, Employee employee) {
	 		
	 		Date optionDate = null;
	 		if (do2.getColumnC()!=null && do2.getColumnC().trim().length()==8) {	 			
		 		optionDate = DateManager.getDateFromDdMmYyyy(do2.getColumnC());		 				
	 		}
	 		 
	 		if (optionDate==null) {
	 			return false;
	 		}	 		
	 	
			List<Do2> do2List =do2Repo.findByDescFromDateEqual(do2.getEmployee().getId(), Do2Manager.VALID, "INCR", optionDate); 
					
			 
			
			if (do2List!=null && do2List.size()>0) {
				return true;
			} else {
				do2List=null;
				do2List = do2Repo.findByDescStatusFromDateLessThanOrderByFromDateDesc(do2.getEmployee().getId(),Do2Manager.VALID,"INCR", optionDate); 
			 
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
			 		
					if (optionDate.equals(nextIncrDate.getTime())) {
						return true;
					} else {
						return false;
					}				
				} else {
					if (employee!=null && employee.getIncrementDate()!=null) {
						return (employee.getIncrementDate().equals(optionDate));
					} else {
						return false;
					}
				}		 
			}	 
		}
	 	
		public boolean isMacpsAlreadyAdjusted(Do2 do2, Employee employee) {
			
			 
			
			List<Do2>macpList= do2Repo.findByDescColAFromDateLessThan(do2.getEmployee().getId(),Do2Manager.VALID, do2.getFromDate(),"HONYGRADE", "MACPS");
			 
			
	 		if (macpList!=null && macpList.size() > 0) {
	 			return true;
	 		}
	 		
	 		return false;
		}

}
