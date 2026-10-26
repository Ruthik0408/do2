package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

 

@RestController
public class Profix extends AuditCageBase implements  Do2AuditCageProcessor, java.io.Serializable {
	Employee employee = null;
	Rank rank = null;
	List<Rank> rankList = null;
	public Date chkDate1 = null;
	public Date chkDate2 = null;
	Do2 prevPromotDo2 = null;
	StringBuffer buffer = new StringBuffer();
	public static final int MIN_HCOMMN_BASIC = 15600;
	public static final int MIN_JCO_BASIC = 9300;
	public static final String PROMOT_NOT_FOUND = "AU:PROMOT For Pay fixation not found.\n";
	public static final String DUPLICATE_PROFIX = "Du:Duplicate Profix Do2. \n";
	public static final String ACP_ALREADY_ADJUSTED = "AU:ACP/MACPS already adjusted. \n";
	public static final String INCORRECT_PROFIX_7CPC = "AU:Option Not Found Exercised For Pay Fixation On Promotion In 7cpc.\n";
	public static final String INCR_DO2_NOT_FOUND = "AU:Increment Not Found Adjusted On Option Date.\n";
	static public final String PAY_FIXATION_NOT_INVOLVED = "AU:Pay Fixation Not Involved For The Promotion.  Hence Option Not Possible \n";

	@Override
	public boolean isValid(Do2 do2) {
		//rejects if any profix part ii order is already generated for TA promot
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if(employee.getRegularTaFlag()!=null && employee.getRegularTaFlag().equals("T") && do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			do2.setReason("Pay Fixed on the Date of Promotion for TA. No Profix Do2 required");
			do2.setStatus(Do2Manager.INVALID);
			do2Repo.save(do2);			 
		   return false;	
		}
		boolean profixValidity = true;
		if (do2.getRefDo2No() == null){
			String refDo2No = Utils.getReferenceDo2No(do2);
			do2.setRefDo2No(refDo2No);
		}
		if (doesDuplicateProfixDo2Exist(do2)) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason(DUPLICATE_PROFIX);
			do2Repo.save(do2);			 
			profixValidity=false;
			return false;
		}
		
		if (!isPromotAvailable(do2)) {
			do2.setStatus(Do2Manager.PENDING);
	//		if (!buffer.equals(ACP_ALREADY_ADJUSTED))
			buffer.append(PROMOT_NOT_FOUND);
			profixValidity = false;
		}
		
		if (do2.getFromDate().after(DateManager.getSeventhCpcDate()) && !isValidProfixIn7cpc(do2)) {
			profixValidity=false;
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(INCORRECT_PROFIX_7CPC); 
		}
			
		if (do2.getFromDate().after(DateManager.getSeventhCpcDate())) {
			String incrdesc = "INCR";
			if (employee.getRegularTaFlag().equals("T")) {
				incrdesc="TAINCR";
			}
			List<Do2> do2List = do2Repo.findByDescFromDateEqual(employee.getId(), "V", incrdesc, do2.getFromDate());
					
			/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.description='INCR' "
			 		+ " and d.employee=:emp and d.status='V' and d.fromDate=:fromDate ")
					.setParameter("emp", employee).setParameter("fromDate", do2.getFromDate())
					.getResultList();*/
			
			if (do2List==null || (do2List!=null && do2List.size()==0)) {
				profixValidity=false;
				do2.setStatus(Do2Manager.INVALID);
				buffer.append(INCR_DO2_NOT_FOUND);
			}
		}
		
		if (!isPromotionBeneficial(do2, employee)) {
			buffer.append(PAY_FIXATION_NOT_INVOLVED);
			profixValidity=false;
		}
		//checks whether paidacting before substantive and profix generated
		// for substantive earlier and rejects the profix pertains to substantive added on 16042012
		
		if (isPaidActingBeforeSubstantive(do2)){
            profixValidity=false;
            do2.setStatus(Do2Manager.PENDING);
		}
		
		if (profixValidity ){
			do2.setStatus(Do2Manager.VALID);
		} else {
			do2.setStatus(Do2Manager.INVALID);
		}
		
 		do2.setReason(buffer.toString());
		do2Repo.save(do2);

 	/*	if (do2.getId()==0) {
		 do2Repo.save(do2);
 		}*/
		return profixValidity;
	}
 /*
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		employee=Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		List<Do2> promotDo2List = Utils.getReferenceDo2List(newAuditCage.getDo2());
		AuditCage promotPreviousAuditCage = null;
		Do2 promotDo2 = null;
		int gpay = 0;
		if (newAuditCage.getDo2().getColumnA().equals("PANK") || newAuditCage.getDo2().getColumnA().equals("SBNK") || newAuditCage.getDo2().getColumnA().equals("TSNK") || newAuditCage.getDo2().getColumnA().equals("SPHAV")){
			gpay=2000;
		} else if (newAuditCage.getDo2().getColumnA().equals("PAHAV") || newAuditCage.getDo2().getColumnA().equals("SBHAV") || newAuditCage.getDo2().getColumnA().equals("GRHAV")){
			gpay=2400;
		} else if (newAuditCage.getDo2().getColumnA().equals("NBSUB")){
			gpay=2800;
		} else if (newAuditCage.getDo2().getColumnA().equals("PASUB") || newAuditCage.getDo2().getColumnA().equals("SBSUB")){
			gpay=4200;
		} else if (newAuditCage.getDo2().getColumnA().equals("PASUBMAJ") || newAuditCage.getDo2().getColumnA().equals("SBSUBMAJ")){
			gpay=4600;		
		} else {
			gpay=2400;
		}
		for (Do2 do2InList : promotDo2List) {
			if (do2InList.getDescription().equals("PROMOT") || 
				do2InList.getDescription().equals("MSTHAV") ||
				do2InList.getDescription().equals("MSTJCO") ||
				do2InList.getDescription().equals("HCOMMN")) {
				promotDo2 = do2InList;
				break;
			}
		}
		if (promotDo2!=null && (promotDo2.getColumnA().equals("NBSUB") || promotDo2.getColumnA().equals("NBRIS"))) {
			AuditCage promotAuditCage = Utils.getAuditCage(promotDo2);
			if (promotAuditCage != null && promotAuditCage.getBasicPay() == MIN_JCO_BASIC){
				AuditCage auditCageEarlierToPromotDate= Utils.getEarlierAuditCageToPromot(newAuditCage, promotDo2.getFromDate());
				if ((auditCageEarlierToPromotDate.getXGroupPay() != null && 
					 auditCageEarlierToPromotDate.getXGroupPay() != 0 && 
					 auditCageEarlierToPromotDate.getBasicPay() < 8580)) {
					return newAuditCage;
				}
				if(auditCageEarlierToPromotDate.getBasicPay()<8600 && 
				   auditCageEarlierToPromotDate.getXGroupPay()==0){
					return newAuditCage;
				}
			}
		}
		if (promotDo2!=null && (promotDo2.getColumnA().equals("HONYLT"))) {
			AuditCage promotAuditCage = Utils.getAuditCage(promotDo2);
			if (promotAuditCage != null && promotAuditCage.getBasicPay() == MIN_HCOMMN_BASIC){
				return newAuditCage;
			}
		}
		   
		if (promotDo2 != null) {
			Date fromDate = promotDo2.getFromDate();
			promotPreviousAuditCage = AuditCageHelper.getPreviousDateAuditCage(fromDate, newAuditCage.getDo2());		
		}
		if (promotPreviousAuditCage!=null) {
			// Added to take care of MACPS condition - if the PBOR is ACPNK and getting MSTHAV, then there is no 3% pay fixation
			// only increase in grade pay (ie 2400 to 2800)
			if  ((promotDo2.getDescription().equals("MSTHAV") || 
				(promotDo2.getDescription().equals("PROMOT") && promotDo2.getColumnA().equals("SPHAV")) ||
				(promotDo2.getDescription().equals("PROMOT") && promotDo2.getColumnA().equals("GRHAV"))) && 
					employee.getNaikDate() == null &&
				   (employee.getAcpnaikDate()!= null && !(employee.getAcpnaikDate().before(DateManager.getSixthCpcAllowanceDate())) &&
					!employee.getAcpnaikDate().after(promotPreviousAuditCage.getDo2().getFromDate()))){
					;
					
			} // The following lines are commented to adjustment minimum pay on promotion to MSTJCO since change of pay band 
			   else if ((promotDo2.getDescription().equals("MSTJCO"))  && 
						employee.getNaikDate() == null &&
					   (employee.getAcpnaikDate()!= null && !(employee.getAcpnaikDate().before(DateManager.getSixthCpcAllowanceDate())) &&
						!employee.getAcpnaikDate().after(promotPreviousAuditCage.getDo2().getFromDate()))){
					;
			} else {
				if (promotDo2.getDescription().equals("MSTHAV") &&					
					promotPreviousAuditCage.getGradePay() == 2800) {
					;
				} else if (Utils.isPromotionAfterReversionAfter010106(newAuditCage.getDo2())){
                    ;
				} else if (promotDo2.getDescription().equals("HCOMMN")) {
					newAuditCage.setBasicPay(baseAuditCage.getBasicPay()
					+ Utils.getIncrmentRoundedToTen(baseAuditCage.getBasicPay() + promotPreviousAuditCage.getGradePay()
					+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	newAuditCage.getFromDate()));
					if (newAuditCage.getBasicPay() < MIN_HCOMMN_BASIC)
						newAuditCage.setBasicPay(MIN_HCOMMN_BASIC);
				} else if (promotDo2.getDescription().equals("MSTJCO")) {
					newAuditCage.setBasicPay(baseAuditCage.getBasicPay()
					+ Utils.getIncrmentRoundedToTen(baseAuditCage.getBasicPay() + promotPreviousAuditCage.getGradePay()
					+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	newAuditCage.getFromDate()));
					if (newAuditCage.getBasicPay() < MIN_JCO_BASIC)
						newAuditCage.setBasicPay(MIN_JCO_BASIC);
				} else if (promotDo2.getDescription().equals("MSTHAV")) {
					newAuditCage.setBasicPay(baseAuditCage.getBasicPay()
					+ Utils.getIncrmentRoundedToTen(baseAuditCage.getBasicPay() + promotPreviousAuditCage.getGradePay()
					+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	newAuditCage.getFromDate()));
				} else {
					newAuditCage.setBasicPay(baseAuditCage.getBasicPay()
					+ Utils.getIncrmentRoundedToTen(baseAuditCage.getBasicPay() + gpay 
					+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	newAuditCage.getFromDate()));
				}
			}
		}		  
		return newAuditCage;
	}*/

	// above method commented and modified as below due to sevencpc
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		employee=Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		List<Do2> promotDo2List = Utils.getReferenceDo2List(newAuditCage.getDo2());
		AuditCage promotPreviousAuditCage = null;
		Do2 promotDo2 = null;
		int gpay = 0;
		if (newAuditCage.getDo2().getFromDate().before(DateManager.getSeventhCpcDate())) {
			if (newAuditCage.getDo2().getColumnA().equals("PANK") || newAuditCage.getDo2().getColumnA().equals("SBNK") || newAuditCage.getDo2().getColumnA().equals("TSNK") || newAuditCage.getDo2().getColumnA().equals("SPHAV")){
				gpay=2000;
			} else if (newAuditCage.getDo2().getColumnA().equals("PAHAV") || newAuditCage.getDo2().getColumnA().equals("SBHAV") || newAuditCage.getDo2().getColumnA().equals("GRHAV")){
				gpay=2400;
			} else if (newAuditCage.getDo2().getColumnA().equals("NBSUB")){
				gpay=2800;
			} else if (newAuditCage.getDo2().getColumnA().equals("PASUB") || newAuditCage.getDo2().getColumnA().equals("SBSUB")){
				gpay=4200;
			} else if (newAuditCage.getDo2().getColumnA().equals("PASUBMAJ") || newAuditCage.getDo2().getColumnA().equals("SBSUBMAJ")){
				gpay=4600;		
			} else {
				gpay=2400;
			}
			for (Do2 do2InList : promotDo2List) {
				if (do2InList.getDescription().equals("PROMOT") || 
					do2InList.getDescription().equals("MSTHAV") ||
					do2InList.getDescription().equals("MSTJCO") ||
					do2InList.getDescription().equals("HCOMMN")) {
					promotDo2 = do2InList;
					break;
				}
			}
			
			
			if (promotDo2.getDescription().equals("PROMOT") && promotDo2.getColumnA().equals("SPHAV")) {
				if (promotDo2.getColumnC()!=null && promotDo2.getColumnC().equals("DIRECT")) {
					if (promotDo2.getFromDate().equals(employee.getEnrolmentDate())) {
						return newAuditCage;
					}
				}
			}
			
			if (promotDo2.getDescription().equals("PROMOT") && promotDo2.getColumnA().equals("SPHAV")) {
				if (promotDo2.getColumnC()!=null && promotDo2.getColumnC().equals("DIRECT")) {
					if (promotDo2.getFromDate().equals(employee.getEnrolmentDate())) {
						return newAuditCage;
					}
				}
			}
			
			
			if (promotDo2!=null && (promotDo2.getColumnA().equals("NBSUB") || promotDo2.getColumnA().equals("NBRIS"))) {
				AuditCage promotAuditCage = Utils.getAuditCage(promotDo2);
				if (promotAuditCage != null && promotAuditCage.getBasicPay() == MIN_JCO_BASIC){
					AuditCage auditCageEarlierToPromotDate= Utils.getEarlierAuditCageToPromot(newAuditCage, promotDo2.getFromDate());
					if ((auditCageEarlierToPromotDate.getXGroupPay() != null && 
						 auditCageEarlierToPromotDate.getXGroupPay() != 0 && 
						 auditCageEarlierToPromotDate.getBasicPay() < 8580)) {
						return newAuditCage;
					}
					if(auditCageEarlierToPromotDate.getBasicPay()<8600 && 
					   auditCageEarlierToPromotDate.getXGroupPay()==0){
						return newAuditCage;
					}
				} else {
					
				}
			}
			if (promotDo2!=null && (promotDo2.getColumnA().equals("HONYLT"))) {
				AuditCage promotAuditCage = Utils.getAuditCage(promotDo2);
				if (promotAuditCage != null && promotAuditCage.getBasicPay() == MIN_HCOMMN_BASIC){
					return newAuditCage;
				}
			}
			   
			if (promotDo2 != null) {
				Date fromDate = promotDo2.getFromDate();
				promotPreviousAuditCage = AuditCageHelper.getPreviousDateAuditCage(fromDate, newAuditCage.getDo2());		
			}
			if (promotPreviousAuditCage!=null) {
	 			// Added to take care of MACPS condition - if the PBOR is ACPNK and getting MSTHAV, then there is no 3% pay fixation
				// only increase in grade pay (ie 2400 to 2800)
				if  ((promotDo2.getDescription().equals("MSTHAV") || 
					(promotDo2.getDescription().equals("PROMOT") && promotDo2.getColumnA().equals("SPHAV")) ||
					(promotDo2.getDescription().equals("PROMOT") && promotDo2.getColumnA().equals("GRHAV"))) && 
						employee.getNaikDate() == null &&
					   (employee.getAcpnaikDate()!= null && !(employee.getAcpnaikDate().before(DateManager.getSixthCpcAllowanceDate())) &&
						!employee.getAcpnaikDate().after(promotPreviousAuditCage.getDo2().getFromDate()))){
						;
				} /*else if ((promotDo2.getDescription().equals("MSTJCO"))  && 
							employee.getNaikDate() == null &&
						   (employee.getAcpnaikDate()!= null && !(employee.getAcpnaikDate().before(DateManager.getSixthCpcAllowanceDate())) &&
							!employee.getAcpnaikDate().after(promotPreviousAuditCage.getDo2().getFromDate()))){
						;
				}*/ else {
					if (promotDo2.getDescription().equals("MSTHAV") &&					
						promotPreviousAuditCage.getGradePay() == 2800) {
						;
					} else if (Utils.isPromotionAfterReversionAfter010106(newAuditCage.getDo2())){
	                    ;
					} else if (promotDo2.getDescription().equals("HCOMMN")) {
						newAuditCage.setBasicPay(baseAuditCage.getBasicPay()
						+ Utils.getIncrmentRoundedToTen(baseAuditCage.getBasicPay() + promotPreviousAuditCage.getGradePay()
						+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	newAuditCage.getFromDate()));
						if (newAuditCage.getBasicPay() < MIN_HCOMMN_BASIC)
							newAuditCage.setBasicPay(MIN_HCOMMN_BASIC);
					} else if (promotDo2.getDescription().equals("MSTJCO")) {
						newAuditCage.setBasicPay(baseAuditCage.getBasicPay()
						+ Utils.getIncrmentRoundedToTen(baseAuditCage.getBasicPay() + promotPreviousAuditCage.getGradePay()
						+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	newAuditCage.getFromDate()));
						if (newAuditCage.getBasicPay() < MIN_JCO_BASIC)
							newAuditCage.setBasicPay(MIN_JCO_BASIC);
					} else if (promotDo2.getDescription().equals("MSTHAV")) {
						newAuditCage.setBasicPay(baseAuditCage.getBasicPay()
						+ Utils.getIncrmentRoundedToTen(baseAuditCage.getBasicPay() + promotPreviousAuditCage.getGradePay()
						+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	newAuditCage.getFromDate()));
					} else if (newAuditCage.getDo2().getColumnA().equals("SPHAV") )  {  // this para added to adjust minimum basic pay 8560 who promoted after 01-01-2006 on 01-11-2023
            			if (employee.getEnrolmentDate()!=null && employee.getHavDate()!=null
        	 					&& employee.getHavDate().after(employee.getEnrolmentDate())
        	 					&& employee.getHavDate().equals(employee.getSportHavildarDate()) && employee.getSportHavildarFlag()==true
        	 					&& employee.getNaikDate()==null && employee.getAcpnaikDate()==null &&  employee.getAcphavDate()==null  &&
        	 					employee.getHavDate().before(newAuditCage.getDo2().getFromDate())) {
                			 if(newAuditCage.getBasicPay()<=8560) {
                				 newAuditCage.setBasicPay(8560);
                			 }
            			}
            			if (employee.getEnrolmentDate()!=null && employee.getHavDate()!=null
        	 					&& employee.getHavDate().after(employee.getEnrolmentDate())
        	 					&& employee.getHavDate().equals(employee.getSportHavildarDate()) && employee.getSportHavildarFlag()==true
        	 					&& ((employee.getNaikDate()!=null && employee.getNaikDate().before(employee.getHavDate())) 
        	 					|| (employee.getAcpnaikDate()!=null && employee.getAcpnaikDate().before(employee.getHavDate()))) 
        	 					&&  employee.getAcphavDate()==null  && employee.getHavDate().before(newAuditCage.getDo2().getFromDate())) {
            				newAuditCage.setBasicPay(baseAuditCage.getBasicPay()
    								+ Utils.getIncrmentRoundedToTen(baseAuditCage.getBasicPay() + gpay 
    								+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	newAuditCage.getFromDate()));
            			}
            		} else {
						newAuditCage.setBasicPay(baseAuditCage.getBasicPay()
								+ Utils.getIncrmentRoundedToTen(baseAuditCage.getBasicPay() + gpay 
								+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	newAuditCage.getFromDate()));
						
						Date remustDate = Utils.getFromDateForRemust(newAuditCage.getDo2());
						if (remustDate!=null && !remustDate.before(DateManager.getSixthCpcDate())) {
						//if (remustDate!=null) {
							if (DateManager.getNoOfMonths(remustDate, newAuditCage.getDo2().getFromDate()) > 6) {
								System.out.println ( " AXXXXXX " +remustDate);
								AuditCage remustAuditCage = AuditCageHelper.getRemustAuditCage(remustDate, newAuditCage.getDo2());
								if (remustAuditCage!=null && remustAuditCage.getXGroupPay()!=0) {
										newAuditCage.setBasicPay(baseAuditCage.getBasicPay()
											+ Utils.getIncrmentRoundedToTen(baseAuditCage.getBasicPay() + gpay 
											+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	newAuditCage.getFromDate()));
								} else {
	        						System.out.println ( " XXXXXX AAAA " +newAuditCage.getDo2().getFromDate());
	        						
	        						AuditCage firstAuditCage = AuditCageHelper.getFirstAuditCage(newAuditCage.getDo2().getFromDate(), newAuditCage.getDo2());
	        						Date firstAuditCageDate = firstAuditCage.getFromDate();
	        						System.out.println ( " first auditcage date " +firstAuditCageDate);
	        						if (DateManager.getNoOfMonths(firstAuditCageDate, newAuditCage.getDo2().getFromDate()) >= 6) {		
	        							if (firstAuditCage.getXGroupPay()!=0) {
	        							newAuditCage.setBasicPay(baseAuditCage.getBasicPay()+Utils.getIncrmentRoundedToTen(baseAuditCage.getBasicPay()
	        									+ (promotPreviousAuditCage.getGradePay() == null ? 0: promotPreviousAuditCage.getGradePay())
	        									+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	promotPreviousAuditCage.getFromDate()));

	        						} else {
	        							newAuditCage.setBasicPay(baseAuditCage.getBasicPay()+Utils.getIncrmentRoundedToTen(baseAuditCage.getBasicPay()
		        								+ (promotPreviousAuditCage.getGradePay() == null ? 0: promotPreviousAuditCage.getGradePay())
		        								 ,promotPreviousAuditCage.getFromDate()));	
	        						}
	        							
	        						
	        									
	        						} else {
	        								newAuditCage.setBasicPay(baseAuditCage.getBasicPay()+Utils.getIncrmentRoundedToTen(baseAuditCage.getBasicPay()
        								+ (promotPreviousAuditCage.getGradePay() == null ? 0: promotPreviousAuditCage.getGradePay())
        								 ,promotPreviousAuditCage.getFromDate()));	
    						}
	        				}           	
								
							} else {
								newAuditCage.setBasicPay(baseAuditCage.getBasicPay()
										+ Utils.getIncrmentRoundedToTen(baseAuditCage.getBasicPay() + gpay,	newAuditCage.getFromDate()));
							}
						} else {
							
							if (!baseAuditCage.getDo2().getDescription().equals("INCR")) {
								AuditCage incrAuditCage = null;
								incrAuditCage = AuditCageHelper.getIncrAuditCage(newAuditCage.getFromDate(), newAuditCage.getDo2());
								newAuditCage.setBasicPay(incrAuditCage.getBasicPay()
										+ Utils.getIncrmentRoundedToTen(incrAuditCage.getBasicPay() + gpay
												+(promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	newAuditCage.getFromDate()));
							} else {
								newAuditCage.setBasicPay(baseAuditCage.getBasicPay()
										+ Utils.getIncrmentRoundedToTen(baseAuditCage.getBasicPay() + gpay
												+(promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	newAuditCage.getFromDate()));
							}
							
							/*
							newAuditCage.setBasicPay(baseAuditCage.getBasicPay()
									+ Utils.getIncrmentRoundedToTen(baseAuditCage.getBasicPay() + gpay
											+(promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	newAuditCage.getFromDate()));*/
						}
					}
				}
			}	 else {
				System.out.println ( " AAAAAAAAAAAAAAA ");
				// This condition should not arise -- 			  
						   Rank rank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(),newAuditCage.getDo2().getFromDate(),"ALLCE");
						   int xpay=baseAuditCage.getXGroupPay();	
						   if (rank.getRankCode().equals("30")
								   || rank.getRankCode().equals("29")){
							     gpay= 2000;
						   } else if(rank.getRankCode().equals("40")
								   || rank.getRankCode().equals("39")){
							   gpay=2400;
						   } else if (rank.getRankCode().equals("50")
								   || rank.getRankCode().equals("49")){
							   gpay=2800;
						   } else if (rank.getRankCode().equals("60")
								   || rank.getRankCode().equals("59")){
							   gpay=4200;
						   } else if (rank.getRankCode().equals("70")
								   || rank.getRankCode().equals("69")){
							   gpay=4600;
						   }
				 		   GroupCategory groupCategory= Utils.getGroupCategory(newAuditCage.getDo2(),newAuditCage.getFromDate());
						   if (groupCategory !=null) {
							   if (groupCategory.getSoldierGroup().equals("X")){
								   xpay=1400;
							   }
						   }			   
						   newAuditCage.setBasicPay((baseAuditCage.getBasicPay()+Utils.getIncrmentRoundedToTen(baseAuditCage.getBasicPay()+(gpay)+(xpay), newAuditCage.getDo2().getFromDate())));
			 		}	 	  
			
		} else {
			int newBasic = newAuditCage.getBasicPay();
			newAuditCage.setBasicPay(baseAuditCage.getBasicPay());
			
			if (newAuditCage.getDo2().getColumnA().equals("PANK") || newAuditCage.getDo2().getColumnA().equals("SBNK") || newAuditCage.getDo2().getColumnA().equals("TSNK") || newAuditCage.getDo2().getColumnA().equals("SPHAV")){
				gpay=2000;
			} else if (newAuditCage.getDo2().getColumnA().equals("PAHAV") || newAuditCage.getDo2().getColumnA().equals("SBHAV") || newAuditCage.getDo2().getColumnA().equals("GRHAV") || newAuditCage.getDo2().getColumnA().equals("MSTHAV")) {
				gpay=2400;
			} else if (newAuditCage.getDo2().getColumnA().equals("NBSUB")){
				gpay=2800;
			} else if (newAuditCage.getDo2().getColumnA().equals("PASUB") || newAuditCage.getDo2().getColumnA().equals("SBSUB")){
				gpay=4200;
			} else if (newAuditCage.getDo2().getColumnA().equals("PASUBMAJ") || newAuditCage.getDo2().getColumnA().equals("SBSUBMAJ")){
				gpay=4600;		
			} else if (newAuditCage.getDo2().getColumnA().equals("HONYLT")) {
				gpay=4800;
			} else if (newAuditCage.getDo2().getColumnA().equals("HONYCAPT")) {
				gpay=5400;
			}
			
			for (Do2 do2InList : promotDo2List) {
				if (do2InList.getDescription().equals("PROMOT") || 
					do2InList.getDescription().equals("MSTHAV") ||
					do2InList.getDescription().equals("MSTJCO") ||
					do2InList.getDescription().equals("HCOMMN")) {
					promotDo2 = do2InList;
					break;
				}
			}
			if (promotDo2!=null && (promotDo2.getColumnA().equals("NBSUB") || promotDo2.getColumnA().equals("NBRIS"))) {
				AuditCage promotAuditCage = Utils.getAuditCage(promotDo2);
				if (promotAuditCage != null && promotAuditCage.getBasicPay() == MIN_JCO_BASIC){
					AuditCage auditCageEarlierToPromotDate= Utils.getEarlierAuditCageToPromot(newAuditCage, promotDo2.getFromDate());
					if ((auditCageEarlierToPromotDate.getXGroupPay() != null && 
						 auditCageEarlierToPromotDate.getXGroupPay() != 0 && 
						 auditCageEarlierToPromotDate.getBasicPay() < 8580)) {
						return newAuditCage;
					}
					if(auditCageEarlierToPromotDate.getBasicPay()<8600 && 
					   auditCageEarlierToPromotDate.getXGroupPay()==0){
						return newAuditCage;
					}
				}
			}
			if (promotDo2!=null && (promotDo2.getColumnA().equals("HONYLT"))) {
				AuditCage promotAuditCage = Utils.getAuditCage(promotDo2);
				if (promotAuditCage != null && promotAuditCage.getBasicPay() == MIN_HCOMMN_BASIC){
					return newAuditCage;
				}
			}
			   
			if (promotDo2 != null) {
				Date fromDate = promotDo2.getFromDate();
				promotPreviousAuditCage = AuditCageHelper.getPreviousDateAuditCage(fromDate, newAuditCage.getDo2());		
			}
			if (promotPreviousAuditCage!=null) {
				// Added to take care of MACPS condition - if the PBOR is ACPNK and getting MSTHAV, then there is no 3% pay fixation
				// only increase in grade pay (ie 2400 to 2800)
				if  ((promotDo2.getDescription().equals("MSTHAV") || 
					(promotDo2.getDescription().equals("PROMOT") && promotDo2.getColumnA().equals("SPHAV")) ||
					(promotDo2.getDescription().equals("PROMOT") && promotDo2.getColumnA().equals("GRHAV"))) && 
						employee.getNaikDate() == null &&
					   (employee.getAcpnaikDate()!= null && !(employee.getAcpnaikDate().before(DateManager.getSixthCpcAllowanceDate())) &&
						!employee.getAcpnaikDate().after(promotPreviousAuditCage.getDo2().getFromDate()))){
						;
				} else if ((promotDo2.getDescription().equals("MSTJCO"))  && 
							employee.getNaikDate() == null &&
						   (employee.getAcpnaikDate()!= null && !(employee.getAcpnaikDate().before(DateManager.getSixthCpcAllowanceDate())) &&
							!employee.getAcpnaikDate().after(promotPreviousAuditCage.getDo2().getFromDate()))){
						;
				} else {
					if (promotDo2.getDescription().equals("MSTHAV") &&					
						promotPreviousAuditCage.getGradePay() == 2800) {
						;
					} else if (Utils.isPromotionAfterReversionAfter010106(newAuditCage.getDo2())){
	                    ;
					} else if (promotDo2.getDescription().equals("HCOMMN")) {
						newAuditCage.setOldBasicPay(baseAuditCage.getOldBasicPay()
						+ Utils.getIncrmentRoundedToTen(baseAuditCage.getOldBasicPay() + promotPreviousAuditCage.getOldGradePay()
						+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	newAuditCage.getFromDate()));
						if (newAuditCage.getOldBasicPay() < MIN_HCOMMN_BASIC)
							newAuditCage.setOldBasicPay(MIN_HCOMMN_BASIC);
					} else if (promotDo2.getDescription().equals("MSTJCO")) {
						newAuditCage.setOldBasicPay(baseAuditCage.getOldBasicPay()
						+ Utils.getIncrmentRoundedToTen(baseAuditCage.getBasicPay() + promotPreviousAuditCage.getOldGradePay()
						+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	newAuditCage.getFromDate()));
						if (newAuditCage.getOldBasicPay() < MIN_JCO_BASIC)
							newAuditCage.setOldBasicPay(MIN_JCO_BASIC);
					} else if (promotDo2.getDescription().equals("MSTHAV")) {
						newAuditCage.setOldBasicPay(baseAuditCage.getOldBasicPay()
						+ Utils.getIncrmentRoundedToTen(baseAuditCage.getOldBasicPay() + promotPreviousAuditCage.getOldGradePay()
						+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	newAuditCage.getFromDate()));
					} else {
						int sixcpcXpay = Utils.get6cpcXGroupPay(baseAuditCage);
						newAuditCage.setOldBasicPay(baseAuditCage.getOldBasicPay()
								+ Utils.getIncrmentRoundedToTen(baseAuditCage.getOldBasicPay() + gpay 
								+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	newAuditCage.getFromDate()));
					
						Date remustDate = null;
						remustDate = Utils.getFromDateForRemust(newAuditCage.getDo2());
						if (remustDate!=null) {
							if (DateManager.getNoOfMonths(remustDate, newAuditCage.getDo2().getFromDate()) > 6) {	
								AuditCage remustAuditCage = AuditCageHelper.getRemustAuditCage(remustDate, newAuditCage.getDo2());
								if (remustAuditCage!=null && remustAuditCage.getXGroupPay()!=null && remustAuditCage.getXGroupPay()!=0) {
									newAuditCage.setOldBasicPay(baseAuditCage.getOldBasicPay()
											+ Utils.getIncrmentRoundedToTen(baseAuditCage.getOldBasicPay() + gpay 
											+ (promotPreviousAuditCage.getXGroupPay() == null ? 0: promotPreviousAuditCage.getXGroupPay()),	newAuditCage.getFromDate()));
								}  else {
									newAuditCage.setOldBasicPay(baseAuditCage.getOldBasicPay()
											+ Utils.getIncrmentRoundedToTen(baseAuditCage.getOldBasicPay() + gpay,	newAuditCage.getFromDate()));
								}
							} 
						} else {
							newAuditCage.setOldBasicPay(baseAuditCage.getOldBasicPay()
									+ Utils.getIncrmentRoundedToTen(baseAuditCage.getOldBasicPay() + gpay + sixcpcXpay,	newAuditCage.getFromDate()));
						}
					}
				}
			}
			
		// Added code for OPTFIX Line 185 to 207 
	 		if (promotDo2!=null && ((promotDo2.getToDate()!=null && promotDo2.getToDate().equals(newAuditCage.getFromDate()))
					|| (promotDo2.getDescription().equals("HCOMMN") && promotDo2.getColumnC()!=null && promotDo2.getColumnC().length()==8 && promotDo2.getFromDate().after(DateManager.getSeventhCpcDate())))) {
	 	
	 			String promotPayLevel = Do2Manager.getPayLevel(gpay);
				newBasic = newAuditCage.getBasicPay();
	//	 		System.out.println("^^^^^^^^^^^^^^^profix ....  "  +gpay +"  "+newBasic + "  "+promotPayLevel);

				List<PayMatrix>paymatrixList = matrixRepo.findByPayLevelAndBasicPayGreaterThanOrderByBasicPayAsc(promotPayLevel, newBasic);
						
				/*		Utils.getEntityManager().createQuery( 
						 " select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND "
						+ " pm.basicPay>:sevencpcpay order by basicPay asc ")
						.setParameter("paylevel", promotPayLevel)
						.setParameter("sevencpcpay", newBasic)
						.getResultList();*/
				
				if (paymatrixList!=null && paymatrixList.size() > 0) {
					newBasic = paymatrixList.get(0).getBasicPay();
				}
	 			
				paymatrixList = null;
				promotPayLevel = newAuditCage.getPayLevel();
	 
				paymatrixList = matrixRepo.findByPayLevelAndBasicPayGreaterThanEqualOrderByBasicPayAsc(promotPayLevel,newBasic);
						
				/*		Utils.getEntityManager().createQuery(" select pm FROM PayMatrix pm WHERE pm.payLevel=:paylevel AND "
						+ " pm.basicPay>=:sevencpcpay order by basicPay asc ").setParameter("paylevel", promotPayLevel)
						.setParameter("sevencpcpay", newBasic).getResultList();*/
				
				if (paymatrixList!=null && paymatrixList.size() > 0) {
					newBasic = paymatrixList.get(0).getBasicPay();
				}
				
				newAuditCage.setBasicPay(newBasic); 
				if (newAuditCage.getDo2().getColumnA().equals("HCOMMN")) {
					newAuditCage.setMsPay(new Hcommn().getMsPay(newAuditCage.getDo2()));
				}
				 
	 		} 
		}
		
	// 	System.out.println("pppppppppppppppppppppp  profix newbasic  " + newAuditCage.getBasicPay());
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
               
	public boolean isPromotAvailable(Do2 do2) {
		List<Do2> promotDo2List = Utils.getReferenceDo2List(do2);
	//	System.out.println("11111111111111111111111 promotdo2list size  " +promotDo2List.size());
		if (promotDo2List==null || (promotDo2List!=null && promotDo2List.size()==0))
			return false;
		
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		for (Do2 promotDo2 : promotDo2List) {
			
			if (promotDo2.getDescription().equals("MSTHAV")) {
			   if (employee.getAcphavDate()!=null && employee.getAcphavDate().before(do2.getFromDate())){
					buffer.append(ACP_ALREADY_ADJUSTED);
					return false;
			   } else return true;
			}
			
			if (promotDo2.getDescription().equals("MSTJCO")) { 
			    if (employee.getAcpnbsubDate()!=null && employee.getAcpnbsubDate().before(do2.getFromDate())){
				  buffer.append(ACP_ALREADY_ADJUSTED);
				  return false;
			    } else return true;
			}
	//	System.out.println("2222222222222222222222222222223333333333333333333333  promotdesc  " +promotDo2.getDescription() +" promot cola  "+promotDo2.getColumnA() +" do2cola " +do2.getColumnA());
					
			if (promotDo2.getDescription().equals("PROMOT") &&
			   (promotDo2.getColumnA() != null && 
				promotDo2.getColumnA().trim().equals(do2.getColumnA().trim()))) {
				
				if (Utils.isAcpBeforeCurrentDo2(promotDo2)) {
					buffer.append(ACP_ALREADY_ADJUSTED);
					return false;
				}				
			 	return true;
			}
	//		System.out.println("222222222222222222222222222222  promotdesc  " +promotDo2.getDescription());
			if (promotDo2.getDescription().equals("HCOMMN") && 
			    (promotDo2.getToDate()==null || (promotDo2.getToDate() != null && !promotDo2.getToDate().before(do2.getFromDate()))) &&
			    promotDo2.getColumnA() != null && promotDo2.getColumnA().equals(do2.getColumnA())) {
				return true;
			}
		}
			
		return false;
	}   
	
	public boolean doesDuplicateProfixDo2Exist(Do2 do2) {
		Employee emp = do2.getEmployee();
		boolean validity = false;
		int rankcode = 0;
		List<Do2> duplicateDo2List = null;
		
		if (emp==null)
			 emp=Utils.getCurrentEmployee(do2.getArmyNo());
		if (do2.getColumnA() != null ) {
			if (do2.getColumnA().equals("PANK") || do2.getColumnA().equals("SBNK") || do2.getColumnA().equals("TSNK")){
				rankcode = 30;
			} else if (do2.getColumnA().equals("PAHAV") || do2.getColumnA().equals("SBHAV") || do2.getColumnA().equals("GRHAV") || do2.getColumnA().equals("SPHAV")){
				rankcode = 40;
			} else if (do2.getColumnA().equals("NBSUB")){
				rankcode = 50;
			} else if (do2.getColumnA().equals("PASUB") || do2.getColumnA().equals("SBSUB")){
				rankcode = 60;
			} else if (do2.getColumnA().equals("PASUBMAJ") || do2.getColumnA().equals("SBSUBMAJ")){
				rankcode = 70;
			} else if (do2.getColumnA().equals("HONYLT")){
				rankcode = 80;
			} else if (do2.getColumnA().equals("HONYCAPT")){
				rankcode = 90;
			} else {
				rankcode = 40;
			}
		}
		duplicateDo2List = do2Repo.findByDescFromDateEqualIdNotEqual(emp.getId(), "V", do2.getDescription(), do2.getFromDate(),do2.getId());
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.id != :id AND " +
							" d.employee = :emp AND d.description = :desc AND d.status = 'V' " +
							" AND d.fromDate = :fromDate ")
							.setParameter("emp", emp).setParameter("id", do2.getId())
							.setParameter("fromDate", do2.getFromDate())
							.setParameter("desc", do2.getDescription()).getResultList();*/
		
		if (duplicateDo2List != null && duplicateDo2List.size()>0) {			
			for(Do2 dupdo2 : duplicateDo2List){
				if ((dupdo2.getColumnA().equals("PANK") || dupdo2.getColumnA().equals("SBNK") || dupdo2.getColumnA().equals("TSNK")) && rankcode == 30){
					validity = true;
				} else if ((dupdo2.getColumnA().equals("PAHAV") || dupdo2.getColumnA().equals("SBHAV") ||
						    dupdo2.getColumnA().equals("SPHAV") || dupdo2.getColumnA().equals("GRHAV")) && rankcode == 40){
					validity = true;
				} else if ((dupdo2.getColumnA().equals("NBSUB")) && rankcode == 50){
					validity = true;
				} else if ((dupdo2.getColumnA().equals("PASUB") || dupdo2.getColumnA().equals("SBSUB")) && rankcode == 60){
					validity = true;
				} else if ((dupdo2.getColumnA().equals("PASUBMAJ") || dupdo2.getColumnA().equals("SBSUBMAJ")) && rankcode == 70){
					validity = true;
				} else if ((dupdo2.getColumnA().equals("HONYLT")) && rankcode == 80){
					validity = true;
				} else if ((dupdo2.getColumnA().equals("HONYCAPT")) && rankcode == 90){
					validity = true;
				} else if (rankcode == 40){
					validity = true;
				}
			}
		}
		return validity;
	}
	
	//checks whether paidacting is before substantive and rejects the profix
	//if it is already generated with reference to substantive do2
	//if the year of paidacting and substantive are same reset the do2 no and other values 
	//in the existing profix do2. added on 16042012
	public boolean isPaidActingBeforeSubstantive(Do2 do2){
		List<Do2> promotDo2List = Utils.getReferenceDo2List(do2);	
		for(Do2 promot : promotDo2List){
			
			if(promot.getDescription().equals("PROMOT") && promot.getColumnA().startsWith("SB")){
			
				Do2 substantiveDo2 = promot;
				List<Do2> paidActingDo2 = Utils.getPaidActingDo2(substantiveDo2);
				for(Do2 pado2 : paidActingDo2){
					
					if(pado2.getFromDate().before(substantiveDo2.getFromDate())){
						//Check whether any revert is available between earlier
						//paid acting and substantive rank
						if(isReversionAvailable(do2,pado2.getFromDate(),substantiveDo2.getFromDate())){
                    	   return false;
						}
					    if(isbetweenjanjune(pado2,substantiveDo2)){
							do2.setDo2No(pado2.getDo2No());
							do2.setDo2ItemNo(pado2.getDo2ItemNo());
							do2.setDo2Type(pado2.getDo2Type());
							do2.setColumnA(pado2.getColumnA());
							do2.setRefDo2No(Utils.getReferenceDo2No(pado2));
							return false;
						}
					
						String rejectreason="Generated while accepting SubStantive Promotion "+substantiveDo2.getFromDate()+" Do2 No "+substantiveDo2.getDo2No()+
								"  Do2 Item No "+substantiveDo2.getDo2ItemNo()+"  Do2 Date "+substantiveDo2.getDo2Date()+" PaidActing Do2 Date "+
								pado2.getFromDate();
						buffer.append(rejectreason);
						return true;	
					}
				}
			}
		}
		return false;
	}
	//This will return true if promot date is between 2ndjan and 30th june
	//where paidacting and substantive year are same and paidacting is
	//before substantive added on 16042012
	public boolean isbetweenjanjune(Do2 pado2,Do2 substantiveDo2){
		Date paidActingDate = pado2.getFromDate();
		Calendar paidActing = new GregorianCalendar();
		paidActing.setTimeInMillis(paidActingDate.getTime());
		int paidActingYear = paidActing.get(Calendar.YEAR);
		Date substantiveDate = substantiveDo2.getFromDate();
		Calendar sbpromot = new GregorianCalendar();
		sbpromot.setTimeInMillis(substantiveDate.getTime());
		int sbYear = sbpromot.get(Calendar.YEAR);
		Calendar chkDate1 = new GregorianCalendar(paidActingYear, Calendar.JANUARY, 01);
		Calendar chkDate2 = new GregorianCalendar(paidActingYear, Calendar.JULY, 01);

		Date jan01 = chkDate1.getTime();
		Date jul01 = chkDate2.getTime();
		
		if ((paidActingDate.after(jan01)) && (paidActingDate.before(jul01))){
			 if (paidActingYear == sbYear)
				 return true;
		}
      return false;
	}
	
	//new method to retrieve the revert do2 between paid acting and substantive
	public boolean isReversionAvailable(Do2 do2,Date paFromDate,Date sbFromDate){
		
		List<Do2> do2List = do2Repo.findByFromDateGreaterThanEqualToAndLessThanEqualToAndMultipleDescriptions(do2.getEmployee().getId(),
																					Do2Manager.VALID, paFromDate, sbFromDate, "REVERT","RANKS","LORANK");
				
	/*			Utils.getEntityManager()
		.createQuery("SELECT d FROM Do2 d where d.employee=:emp and d.status=:status " +
				"and d.fromDate>=:paFromDate and d.fromDate<=:sbFromDate and "+
				"(d.description=:desn or d.description=:desn1 or d.description=:desn2) ")
				.setParameter("emp",do2.getEmployee())
				.setParameter("status", Do2Manager.VALID)
				.setParameter("desn","REVERT")
				.setParameter("desn1","RANKS")
				.setParameter("desn2","LORANK")
				.setParameter("paFromDate",paFromDate)
				.setParameter("sbFromDate", sbFromDate)
				.getResultList();*/
				
		      return do2List.size() >0;
	}	
	
	public boolean isValidProfixIn7cpc(Do2 do2) {
		List<Do2> do2List = do2Repo.getValidProfixIn7CPC(do2.getEmployee().getId(), do2.getFromDate(),do2.getColumnA());
				
				
	/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:emp and d.status='V' " 
						+ " and (d.toDate!=null and d.toDate=:fromDate) and ((d.description='PROMOT' and d.columnA=:colA) "
						+ " OR d.description=:colA OR (d.description='OPTFIX' and d.columnB=:colA)) ")
						.setParameter("colA", do2.getColumnA()).setParameter("fromDate", do2.getFromDate())
						.setParameter("emp",do2.getEmployee()).getResultList();*/
		
		if (do2List!=null && do2List.size()>0) {
			return true;
		}
		
		do2List = null;
		String optionDate = DateManager.getStringDateWithoutSeparators(do2.getFromDate());
		do2List = do2Repo.findByDescriptionColAAndColC(do2.getEmployee().getId(), "HCOMMN",do2.getColumnA(), optionDate);
				
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d where d.employee=:emp and d.status='V' " 
				+ " and d.description='HCOMMN' and d.columnA=:colA and d.columnC!=null and d.columnC=:date ")
				.setParameter("colA", do2.getColumnA()).setParameter("date", optionDate)
				.setParameter("emp",do2.getEmployee()).getResultList();*/
		
		if (do2List!=null && do2List.size()>0) {
			return true;
		}
		
		return false;			
	}
	
	public boolean isPromotionBeneficial(Do2 do2, Employee employee) {		
		
		 		
		List<Do2> do2List = Utils.getReferenceDo2List(do2);
		System.out.println("..........do2list size .from profix .. "+ do2List.size());
		if (do2List==null || (do2List!=null && do2List.size()==0)) {
			return false;
		}
 		Do2	actualMacpsPromotDo2 = do2List.get(0);
 		for (Do2 d : do2List) {
 			if (d.getDescription().equals("PROMOT")) {
 				actualMacpsPromotDo2=d;
 				break;
 			}
 		}
		
 		if (actualMacpsPromotDo2!=null && actualMacpsPromotDo2.getDescription().equals("MSTHAV") ) {
			 if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
				 if (employee!=null && employee.getAcpnaikDate()!=null 
						 && !employee.getAcpnaikDate().before(DateManager.getSixthCpcDate()) && employee.getNaikDate()==null) {
						return false;
				 }
				 if (employee!=null && (employee.getAcphavDate()!=null 
						|| (employee.getHavDate()!=null && employee.getHavDate().before(actualMacpsPromotDo2.getFromDate())))) {  //Already ACPHAV/SBHAV before MSTHAV
					return false;
				 }
			 } else  {
				 if (employee!=null && employee.getAcpnaikDate()!=null && employee.getAcphavDate()!=null) {
					 return false;
				 }
			 }		 
		}

        if (actualMacpsPromotDo2!=null) {
            if (Utils.isAcpBeforeCurrentDo2(actualMacpsPromotDo2)) {
                return false;
            } else {
                // 6th CPC rule (2006-2015): profix only if PROMOT fromDate falls
                // between 02-Jan and 30-Jun of that year. ACP-already-adjusted
                // case is already handled above (isAcpBeforeCurrentDo2==false here).
                Calendar cal = new GregorianCalendar();
                cal.setTime(actualMacpsPromotDo2.getFromDate());
                int year = cal.get(Calendar.YEAR);

                if (year >= 2006 && year <= 2015) {
                    Calendar jan02 = new GregorianCalendar(year, Calendar.JANUARY, 2);
                    Calendar jun30 = new GregorianCalendar(year, Calendar.JUNE, 30);
                    Date promotFromDate = actualMacpsPromotDo2.getFromDate();

                    if (promotFromDate.before(jan02.getTime()) || promotFromDate.after(jun30.getTime())) {
                        return false;
                    }
                }
                return true;
            }
        }

        return false;
    }
	
}
