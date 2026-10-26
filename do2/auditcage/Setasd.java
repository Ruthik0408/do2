package itsdc.dolphin.do2.auditcage;
 
 
import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.Utils;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.web.bind.annotation.RestController;

@RestController
public class Setasd extends AuditCageBase implements
		Do2AuditCageProcessor, java.io.Serializable {
				
	 
	private StringBuffer buffer = new StringBuffer();
	private String message;
	boolean validity = true;
	private List <Do2> do2List; 
	Employee employee;
	
	static public final String REF_DOII_NOT_CORRECT= "AU:Incorrect reference DOII - not referring to punishment DOII.\n";
	
	@Override
	public boolean isValid(Do2 do2) {
		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		do2.setStatus(Do2Manager.VALID);

		if (!isValidRefDo2(do2)) {
			validity = false;
			buffer.append("REF_DOII_NOT_CORRECT");
		}
		System.out.println (  " validity " +validity);
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
			message = buffer.toString();
			do2.setReason(message);
		}/*else{
			do2.setReason(null);
			if (do2.getRefDo2No()!=null) {
				Do2 validRanksDo2 = Utils.getReferenceDo2(do2);
				if (validRanksDo2.getStatus().equals(Do2Manager.VALID)) {
					validRanksDo2.setStatus(Do2Manager.DO2_CANCEL_STATUS);
					validRanksDo2.setRemarks("---CANCELLED Status Is Marked Based On SETASD Do2 Published In Respect Of The Individual--");
					do2Repo.save(validRanksDo2);
					System.out.println("........... updating ranks do2 status for setasd ");
				}
				if(do2.getColumnA().equals("FINE")) {
					System.out.println( " validRanksDo2 id " +validRanksDo2.getId());
					List<Arrears> arrearsList = arrRepo.findByDo2Id(validRanksDo2.getId()); 
					if (arrearsList.size() > 0) {
						for (Arrears arr : arrearsList) {
							System.out.println ( " IAM HERE SETASD");
							System.out.println ( " arr amount " +arr.getAmount());
							Utils.addArrearsForCancelDo2(do2, validRanksDo2, (arr.getAmount() * -1), validRanksDo2.getDescription(), arr.getFromDate(), arr.getToDate());
						}
					
				}
					
				}
			}
		}*/
				
		do2Repo.save(do2);
		return validity;
		}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		newAuditCage.setAbsentFlag(false);
		return newAuditCage;
	}
	
	@Transactional
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		do2List = Utils.getReferenceDo2List(do2);
		if (do2List!=null) {
			AuditCageHelper.reverseUpdateEmployeeServiceDetail(do2List.get(0), Do2Manager.ESD_PUNISHMENT);
			AuditCageHelper.reverseUpdateEmployeeServiceDetail(do2List.get(0), Do2Manager.ESD_NQS);
		}
		
		
		if (do2.getRefDo2No()!=null) {
			Do2 validRanksDo2 = Utils.getReferenceDo2(do2);
			if (validRanksDo2.getStatus().equals(Do2Manager.VALID)) {
				validRanksDo2.setStatus(Do2Manager.DO2_CANCEL_STATUS);
				validRanksDo2.setRemarks("---CANCELLED Status Is Marked Based On SETASD Do2 Published In Respect Of The Individual--");
				if(do2.getColumnA().equals("FINE")) {
					System.out.println( " validRanksDo2 id " +validRanksDo2.getId());
					List<Arrears> arrearsList = arrRepo.findByDo2Id(validRanksDo2.getId()); 
					if (arrearsList.size() > 0) {
						for (Arrears arr : arrearsList) {
							System.out.println ( " IAM HERE SETASD");
							System.out.println ( " arr amount " +arr.getAmount()  +" xx "  +arr.getAmount()*-1);
							Utils.addArrearsForCancelDo2(do2, validRanksDo2, (arr.getAmount() * -1), validRanksDo2.getDescription(), arr.getFromDate(), arr.getToDate());
							do2.setReviewQe(Utils.getCurrentQuarter());
						}
					
					}
					
				}
				
				do2Repo.save(validRanksDo2);
				System.out.println("........... updating ranks do2 status for setasd ");
			}
			
		}
	
		
		
		
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 do2, Do2 refDo2) {
		do2List = Utils.getReferenceDo2List(refDo2);
		
		for (Do2 referredDo2 : do2List) {
			if (referredDo2.getDescription().equals(do2.getColumnA())) {
				AuditCageHelper.updateEmployeeServiceDetail(referredDo2, Do2Manager.ESD_PUNISHMENT+"");
				AuditCageHelper.updateEmployeeServiceDetail(referredDo2, Do2Manager.ESD_NQS+"");
				break;
			}
			if (referredDo2.getDescription().equals("SETASD") && referredDo2.getColumnA().equals("RANKS")) {
				List<Do2> ranksDo2List = Utils.getReferenceDo2List(referredDo2);
				for (Do2 rankDo2 : ranksDo2List) {
					rankDo2.setStatus(Do2Manager.PENDING);
					rankDo2.setRemarks(referredDo2.getRemarks()+"###SETASD Do2 Is Cancelled And Hence Ranks Do2 Is Kept In 'P' Status.  Pl Process The Same###");
					do2Repo.save(rankDo2);
				}
			}
		}
		return Do2Manager.SUCCESS;
	}
	 
	public boolean isValidRefDo2(Do2 do2) {
		do2List = Utils.getReferenceDo2List(do2);
		System.out.println ( " do2 list " +do2List.size());
		return (do2List.size() > 0);		
	}
}
