package itsdc.dolphin.do2.auditcage;

 
import java.util.Arrays;
import java.sql.Date;
import java.util.List;
import org.springframework.web.bind.annotation.RestController;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

//ACTING ALLOWANCE - ACTGA in 7cpc
@RestController
public class Adpost extends AuditCageBase implements Do2AuditCageProcessor {
	Employee employee;	
	StringBuffer buffer = new StringBuffer();
	boolean honyOfficer=false;
	
	static public final String INVALID_COLUMN7A = "VL:Column7A Should Contain Either GRANTED Or CEASED.\n";
	static public final String NO_RATE_AVAILABLE = "AU:Rate not available in Allowance Rate table .\n";
	static public final String INVALID_FROMDT = "AU:FromDate Is Prior To Allowance Birth Date.\n";
	static public final String JCOs_ONLY = "AU:Admissible only to JUNIOR COMMISSIONED OFFICERS.\n";
	static public final String INVALID_COLUMN7B = "AU:Column7B Should Contain Either MAJ Or CAPT.\n";
	static public final String INVALID_GRANT = "AU:One Year Not Laped Since Cessation Of Previous Grant.\n";
	static public final String OVERLAP = "OL:Allowance Already Being Drawn On The Grant Date.\n";
	static public final String HONYOFFICER = "AU:JCO Is Already A Honarary Officer.\n";
	static public final String ADDLCHARGEDURATION="AU:Minimum Additional Charge Duration Of 45 Days Not Completed.\n";
	static public final String LEAVEABSENTDURING45DAYS="AU:Individual Is On Leave / Absent During Initial 45 days.\n";
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
		if (do2.getColumnA()==null || (do2.getColumnA()!=null && do2.getColumnA().length()==0)) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(INVALID_COLUMN7A);
			do2Repo.save(do2);
			return false;
		}

		if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			do2.setStatus(Do2Manager.VALID);
			do2Repo.save(do2);
			return true;
		}

		if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
			do2.setStatus(Do2Manager.INVALID);		
			buffer.append(INVALID_FROMDT);
			validity = false;
		}
			
		if (!isValidRank(do2)) {
			validity = false;
			buffer.append(JCOs_ONLY);
			do2.setStatus(Do2Manager.INVALID);					
		}
		
		if (do2.getColumnB()==null || (do2.getColumnB()!=null && !(do2.getColumnB().trim().equals("MAJ") || do2.getColumnB().trim().equals("CAPT")))) {
			buffer.append(INVALID_COLUMN7B);
			validity = false;
			do2.setStatus(Do2Manager.INVALID);	
		}

		if (!isOneYearCompletedSinceLastCessation(do2)) {
			buffer.append(INVALID_GRANT);
			validity = false;
			do2.setStatus(Do2Manager.INVALID);	
		}
 
		if (do2.getTranscriptionType().equals("D") && do2.getInputQe()!=null && Integer.parseInt(do2.getInputQe().substring(3)+do2.getInputQe().substring(0, 2))>202402) {
			if (DateManager.getNoOfDays(do2.getFromDate(), do2.getDo2Date())<45) {
				validity=false;
				do2.setStatus(Do2Manager.INVALID);
				buffer.append(ADDLCHARGEDURATION);
			}
 		}
		 
		if (isLeaveAbsentDuring45Days(do2)) {
			validity=false;
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(LEAVEABSENTDURING45DAYS);
		}
		
		if (isOverlap(do2)) {
			if (honyOfficer) {
				buffer.append(HONYOFFICER);
				validity = false;
				do2.setStatus(Do2Manager.INVALID);
			} else {
				if (do2.getId()==0) {
					buffer.append(OVERLAP);
					validity = false;
					do2.setStatus(Do2Manager.INVALID);
				}
			}			 
		}
		
		if (validity==true) {
			do2.setStatus(Do2Manager.VALID);
		}
		
		do2.setReason(buffer.toString());
		
		do2Repo.save(do2);
		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {		 
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		if (!do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate()) && do2.getColumnA().equals(Do2Manager.GRANTED)) {
			long allowanceAmount = 0;
			long basicPayTenPercent = Math.round(baseAuditCage.getBasicPay()*0.1);
	 		List<PayMatrix> payMatrixList = null;
	 		int increasedBasicPay = newAuditCage.getBasicPay();
	 		payMatrixList = matrixRepo.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(newAuditCage.getPayLevel(),newAuditCage.getBasicPay());
	 		 
	 		if (payMatrixList!=null && payMatrixList.size()>0) {
	 			increasedBasicPay=payMatrixList.get(0).getBasicPay();
	 			payMatrixList=null;
	 		}
			
	 		if (do2.getColumnB().trim().equals("CAPT")) {
	 			payMatrixList =matrixRepo.findByPayLevelAndBasicPayGreaterThanEqualOrderByBasicPayAsc("10B",increasedBasicPay);
	 			 
	 		}
	 		
	 		if (do2.getColumnB().trim().equals("MAJ")) {
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
	 			System.out.println("1111111111111 allowance amount " +allowanceAmount);
	 		}
	// 		System.out.println("222222222222222222222  allowance amount basic  "+newAuditCage.getEmployee()+"  "+allowanceAmount+"  "+newAuditCage.getBasicPay());
	 		
	 		AuditCageHelper.updateAuditCageAllowance(newAuditCage, do2.getDescription(), allowanceAmount);
			
 	 	} else {
			final String[] alwUnderProcess = { do2.getDescription() };
			final List<String> alwToStop = Arrays.asList(alwUnderProcess);

			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		}
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
	
	public boolean isOneYearCompletedSinceLastCessation(Do2 do2) {
		List<Do2> do2List =do2Repo.findByDescColAFromDateLessThanEqual(do2.getEmployee().getId(), Do2Manager.VALID, "ADPOST", "CEASED", do2.getFromDate());
		 
		
		if (do2List==null || (do2List!=null && do2List.size()==0)) {
			return true;
		}
		
		if ((DateManager.getNoOfDays(do2List.get(0).getFromDate(), do2.getFromDate()))<=365) {
			if (!do2List.get(0).getTranscriptionType().equals(Do2Manager.SYSTEM_GENERATED)) {
				return false;
			} else { // system generated ceased do2 comes first in reverse order, checking with second do2 
				if (do2List.size()>1) {
					if ((DateManager.getNoOfDays(do2List.get(1).getFromDate(), do2.getFromDate()))<=365) {
						return false;
					} else { // if more than 365 days, checking for granted do2 in between
						List<Do2> gtdDo2List =do2Repo.findByDescColAFromDateLessThanEqual(do2.getEmployee().getId(), Do2Manager.VALID, "ADPOST", "GRANTED", do2List.get(0).getFromDate());
				 		if (gtdDo2List!=null && gtdDo2List.size()>0) {
							if (do2List.get(1).getFromDate().before(gtdDo2List.get(0).getFromDate())) {
								return false;
							} else {
								return true;
							}
						} else {
							return false;
						}
					}
				} else {
					return false;
				}
			}
		}				
		return true;
	}
	
	public boolean isOverlap(Do2 do2) {
		List<AuditCage> acList =acRepo.findByFromDateLessThanEqualValidOrderByFromDateDescToDateDesc(do2.getEmployee().getId(),do2.getFromDate());
		 
		if (acList!=null && acList.size()>0) {
			if (acList.get(0).getPayLevel().trim().length()==1 && acList.get(0).getMsPay()!=15500) {
				List<AuditCageAllowance> acaList = acaRepo.findAllByAuditCageId(acList.get(0).getId());
					 
				for (AuditCageAllowance aca : acaList) {
					if (aca.getPayCode().getPayCode().trim().equals("ADPOST")) {
						return true;
					}
				}				
			} else {
				honyOfficer=true;
				return true;
			}
		}		
		return false;
	}	
	
	public boolean isValidRank(Do2 do2) {
		
		Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");		
		if (rank!=null && (rank.getId()==9 || rank.getId()==11 || rank.getId()==13)) {
			return true;
		}  			
		return false;		
	}
	
	public boolean isLeaveAbsentDuring45Days(Do2 do2) {
		List<String> leaveAbsentList = Arrays.asList(new String[] { "AL", "ADVAL", "EXTNLV", "BAL", "ACCMAL", 
				"EXTNAL", "EXTNCL", "EXTNSL", "PAL", "RL", "CL", "SL", "PATLVE", "OSL", "AWL", "DESN", "MILRI", "TFRDEP", "REVDEP", "HCOMMN"});
		Date dateLater45Days = DateManager.convertUtilDtToSqlDt(DateManager.getDate(do2.getFromDate(), 45));
		System.out.println("............ date later 45 days " +do2.getFromDate()+"  "+dateLater45Days+"  empid " +employee.getId());
		List<Do2> do2List = do2Repo.findByDescListFromDateBetweenOrToDateBetweenForAdpost(employee.getId(), Do2Manager.VALID, leaveAbsentList,
				do2.getFromDate(), dateLater45Days);
		System.out.println("...........do2list size " + do2List.size());
		return (do2List!=null && do2List.size()>0);
	}
}