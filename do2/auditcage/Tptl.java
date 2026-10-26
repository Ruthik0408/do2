package itsdc.dolphin.do2.auditcage;

import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

 

/*
 * @author N RAMACHANDRAN && K RAMADEVI  DATE:26-08-2009
 */

@RestController
public class Tptl extends AuditCageBase implements 
		Do2AuditCageProcessor {
	Employee employee = null;
	private Rank rank;
	String message;
	StringBuffer buffer = new StringBuffer();
	String status = "";
	boolean validity = true;
	double tptlRate = 0;
	List<Allowance> allowances = new ArrayList<Allowance>();
	double rateAmount = 0;

	static public final double MINIMUM_6CPC_TPTH_RATE = 1000;
	//static public final String NO_RATE_AVAILABLE = "AU:Rate not available in Allowance Rate table. \n";
	static public String RATES_NOT_MATCHING = "AU:Class of city and Rate do not match.\n";
	static public String PBOR_IS_RECRUIT = "AU:PBOR is still a recruit. \n";
	static public String IS_ON_DEPUTATION = "PBOR on deputation to IMTRAT/UNMISSION. \n";
	@Override
	public boolean isValid(Do2 do2) {
		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (do2.getTranscriptionType() != null
				&& do2.getTranscriptionType().equals("S")) {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason("");
			do2Repo.save(do2);
		return true;
		}

		if (do2.getColumnA().equals(Do2Manager.CEASED)) {
			do2.setStatus(Do2Manager.VALID);
			do2Repo.save(do2);
		return true;

		}
		
		if (employee!=null && employee.getSubstantiveRank()!=null 
				&& employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)) {
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(PBOR_IS_RECRUIT);
			do2Repo.save(do2);
			return false;
		}
		
		if (newDo2 != null && newDo2.getId()==do2.getId()) {
			if(!Utils.isValidAllowanceRate(do2.getDescription(), do2.getFromDate(), do2.getFromDate(), do2.getColumnC(), do2.getRate())){
				validity = false;
				buffer.append(RATES_NOT_MATCHING);
			}
		}
		
		if (!validity) {
			message = buffer.toString();
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(message);
		} else {
			do2.setStatus(Do2Manager.VALID);
			do2.setReason("");
		}

		do2Repo.save(do2);
    //  Utils.getEntityManager().flush();
		return validity;
	}

	/*private boolean isValidAmountInRateColumn(Do2 do2) {
		allowances = Utils.getApplicableAllowanceRates(do2.getDescription(),
				do2.getFromDate(), do2.getFromDate(), do2.getColumnC());
		for (Allowance allow : allowances) {
			if (allow.getRateAmount() == do2.getRate())
				return true;
		}
		return false;
	}
*/
	/*@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		int gradePay = 0;
		int basicPay = 0;
		String cityClass = null;
		rateAmount = 0;
		Do2 do2 = newAuditCage.getDo2();
		if (newAuditCage.getDo2().getColumnA().equals(Do2Manager.GRANTED)) {
			if (baseAuditCage.getDeputationStatus() !=null && 
			   (baseAuditCage.getDeputationStatus()=='T' || baseAuditCage.getDeputationStatus()=='U')){
            	newAuditCage.setTransport(0);
            	newAuditCage.getDo2().setReason(IS_ON_DEPUTATION);
            	return newAuditCage;
            }
			cityClass = do2.getColumnC();
			List<AuditCageAllowance> auditCageAllowanceList = Utils.getEntityManager().createQuery(
					"SELECT aca FROM AuditCageAllowance aca where aca.auditCage =:auditCage")
					.setParameter("auditCage", baseAuditCage).getResultList();
			if (auditCageAllowanceList.size() > 0) {
				for (AuditCageAllowance aca : auditCageAllowanceList){
					if (aca.getPayCode().getPayCode().equals("CFAA") || (aca.getPayCode().getPayCode().equals("CMFAA")) ||
						aca.getPayCode().getPayCode().equals("CHAFAA")){
						cityClass = "CCOTH";
					}
				}
			}

			if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
 				allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(),
 															   do2.getFromDate(), rank, cityClass);
 				if (allowances.size()>0) {
 					rateAmount = allowances.get(0).getRateAmount();
 				}

			} else {
				rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
				gradePay = newAuditCage.getGradePay();
				
				basicPay = newAuditCage.getBasicPay();

				if ((gradePay > 1999 && gradePay < 4200) && basicPay >= 7440)
					gradePay = 4200;
                if (gradePay==0){
                	allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), 
                			     DateManager.getPreviousDate(DateManager.getSixthCpcAllowanceDate()),
    						     DateManager.getPreviousDate(DateManager.getSixthCpcAllowanceDate()), rank, cityClass);
                } else {
                	allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(),
    						                                       do2.getFromDate(), gradePay, cityClass);
                }
				
				if (allowances.size()>0) {
					rateAmount = allowances.get(0).getRateAmount();
				}

				if (do2.getDescription().equalsIgnoreCase(Do2Manager.TPTH)
						&& (rateAmount < MINIMUM_6CPC_TPTH_RATE)) {
					rateAmount = MINIMUM_6CPC_TPTH_RATE;
				}
			}
			newAuditCage.setTransport((int) rateAmount);
		} else {
			newAuditCage.setTransport(0);
		}

		return newAuditCage;
	}*/

	//above lines commented and modified as stated below due to sevencpc
	
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		int gradePay = 0;
		int basicPay = 0;
		String cityClass = null;
		rateAmount = 0;
		Do2 do2 = newAuditCage.getDo2();
		if (newAuditCage.getDo2().getColumnA().equals(Do2Manager.GRANTED)) {
			if (baseAuditCage.getDeputationStatus() !=null && 
			   (baseAuditCage.getDeputationStatus().equals("T") || baseAuditCage.getDeputationStatus().equals("U"))){
            	newAuditCage.setTransport(0);
            	newAuditCage.getDo2().setReason(IS_ON_DEPUTATION);
            	return newAuditCage;
            }
			cityClass = do2.getColumnC();
			List<AuditCageAllowance> auditCageAllowanceList = acaRepo.findAllByAuditCageId(baseAuditCage.getId());
					
				//	Utils.getEntityManager().createQuery(
				//	"SELECT aca FROM AuditCageAllowance aca where aca.auditCage =:auditCage")
			//		.setParameter("auditCage", baseAuditCage).getResultList();
			if (auditCageAllowanceList.size() > 0) {
				for (AuditCageAllowance aca : auditCageAllowanceList){
		/*			if (aca.getPayCode().getPayCode().equals("CFAA") || (aca.getPayCode().getPayCode().equals("CMFAA")) ||
						aca.getPayCode().getPayCode().equals("CHAFAA")) {
		commented and added the following on 24.11.2023	*/			
					if (aca.getPayCode().getPayCode().equals("CFAA") || aca.getPayCode().getPayCode().equals("CMFAA") 
							|| aca.getPayCode().getPayCode().equals("CHAFAA") || aca.getPayCode().getPayCode().equals("RISK")
							|| aca.getPayCode().getPayCode().equals("SIACHN") || aca.getPayCode().getPayCode().equals("HAUCS")
							|| aca.getPayCode().getPayCode().equals("HAUCA")  || aca.getPayCode().getPayCode().equals("HAUCL")  
							|| aca.getPayCode().getPayCode().equals("BHUTAN")) {
						cityClass = "CCOTH";
					}
				}
			} 
			rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"ALLCE");
			if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate())) {
				
 				allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(),
 															   do2.getFromDate(), rank, cityClass);
 				if (allowances.size()>0) {
 					rateAmount = allowances.get(0).getRateAmount();
 				}

			} else {
			 
				if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
					gradePay = newAuditCage.getGradePay();
					basicPay = newAuditCage.getBasicPay();
				} else  {
					// the below code amended in view of OPTCPC do2
					/*if (newAuditCage.getPayLevel().equals("0") && newAuditCage.getOldGradePay()==0) {
						gradePay = newAuditCage.getGradePay();
						basicPay = newAuditCage.getBasicPay();
					} else {
						gradePay = newAuditCage.getOldGradePay();
						basicPay = newAuditCage.getOldBasicPay();
					}*/
					
					if (do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
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
					if (!do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate()) 
							&& do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
						gradePay = 4200;	
					}
				}
								
				
                if (do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate()) && gradePay==0) {
                	allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), 
                			     DateManager.getPreviousDate(DateManager.getSixthCpcAllowanceDate()),
    						     DateManager.getPreviousDate(DateManager.getSixthCpcAllowanceDate()), rank, cityClass);
                } else if (do2.getFromDate().before(DateManager.getSeventhCpcDate()) && gradePay==0) {                	
                	allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), 
           			     DateManager.getPreviousDate(DateManager.getSixthCpcAllowanceDate()),
						     DateManager.getPreviousDate(DateManager.getSixthCpcAllowanceDate()), rank, cityClass);
                } else if (gradePay>0 && !do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate()) 
                			&& do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
                	allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), do2.getFromDate(),
                            do2.getFromDate(), gradePay, cityClass);
                } else if (!do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate())) {
                	// grade pay is arrived based on pay_level in base auditcage after 01.07.2017 and sent as parameter
                	String payLevel = null;
                	if (newAuditCage.getPayLevel()!=null) {
                		if (newAuditCage.getPayLevel().length()<3 && Integer.parseInt(newAuditCage.getPayLevel())<3) {
                			if (newAuditCage.getBasicPay()>14600) {
                				payLevel = "3";	
                			} else {
                				newAuditCage.setTransport(0);
                				return newAuditCage;
                			}                			
                		} else {
                			payLevel = newAuditCage.getPayLevel();
                		}                		 
                	} else if (newAuditCage.getBasicPay()>14600) {
                		payLevel = "3";
                	} else {
                		newAuditCage.setTransport(0);
                		return newAuditCage;
                	}
                	
                	if (payLevel.equals("9")) {
                		payLevel="10";
                	}
       //           	System.out.println("yyyyyyyyyyyyyyyyyy  pay level " +payLevel + "  "+ do2.getDescription()+"  "+cityClass+"  "+Do2Manager.getSevenCpcGradepay(payLevel));
                    
                	allowances = Utils.getApplicableAllowanceRates(do2.getDescription(), payLevel, do2.getFromDate(),
                            do2.getFromDate(), Do2Manager.getSevenCpcGradepay(payLevel), cityClass);
      //             	System.out.println("CCCCCC pay level " +payLevel + "  "+allowances.size()+"  "+Do2Manager.getSevenCpcGradepay(payLevel));
                }
			
                if (allowances.size()>0) {
					rateAmount = allowances.get(0).getRateAmount();
				}

				if (gradePay>0 && !do2.getFromDate().before(DateManager.getSixthCpcAllowanceDate()) 
						&& do2.getFromDate().before(DateManager.getSeventhCpcAllowanceDate()) 
						&& do2.getDescription().equalsIgnoreCase(Do2Manager.TPTH) && (rateAmount < MINIMUM_6CPC_TPTH_RATE)) {
					rateAmount = MINIMUM_6CPC_TPTH_RATE;
				}
			}
			newAuditCage.setTransport((int) rateAmount);
		} else {
			newAuditCage.setTransport(0);
		}

		return newAuditCage;
	}
	
	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		return Do2Manager.SUCCESS;
	}
	
	
	
}
