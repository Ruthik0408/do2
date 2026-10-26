package itsdc.dolphin.do2.auditcage;

import java.util.Arrays;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 
 

@RestController
public class Aictgp extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	
	Employee employee;
	GroupCategory groupCategory;
	public String message;
	public StringBuffer buffer = new StringBuffer();	
	
	static public final String INVALID_CATEGORY = "AU:Not valid Category for Grant of Allowance. \n";
	static public final String INVALID_COLUMNABC = "AU:INVALID VALUES IN COLUMNS 7A / 7B / 7C. \n";
	static public final String INVALID_GROUP = "AU:Soldier Does Not Belongs To X-Group/Trade. \n";
	static public final String NOT_IN_7CPC_PAY = "AU:Soldier Not Migrated To 7CPC As On Date.\n";
	static public final String DUPLICATE_DO2="DU:AICTGP Already Adjusted For The Soldier. \n";
	
	String[] X1Trades = { "DHYG", "INSTRSPL", "AIG", "TIFC", "MTECAVEH", "MTECAVNAE", "MTECAVNAF", "MTECAVNAR", "MTECAVNCS", 
						  "MTECAVNEL", "MTECAVNIP", "MTECBVEH", "MTECCVEH", "MTECCOMN", "MTECELE", "MTECGCE", "MTECGUN", "MTECNW", "MTECO", "MTECRDR",
						  "MTECSA", "JENE", "OSS", "JESYS", "DED", "JEEM", "JECIV", "SURVCARTO", "RGR", "LABTECH", "AMTECH", "TIR","TIGM","TICC" } ;
	final List<String> X1_TRADES = Arrays.asList(X1Trades);
	
	@Override
	public boolean isValid(Do2 do2) {
		
		boolean validity = true;		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
		if (do2.getFromDate().before(DateManager.getSeventhCpcDate())) {
			do2.setStatus(Do2Manager.INVALID);
			do2.setReason("VL:Not Admissible before 01-01-2016");
			do2Repo.save(do2);
			return false;
		}
		
		if (do2.getColumnA()==null || (do2.getColumnA()!=null && do2.getColumnA().length()==0) 
				|| do2.getColumnB()==null || (do2.getColumnB()!=null && do2.getColumnB().length()==0) 
				|| do2.getColumnC()==null || (do2.getColumnC()!=null && do2.getColumnC().length()==0)) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(INVALID_COLUMNABC);
			validity = false;
		}
			
		if (!isValidCategory(do2)) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(INVALID_CATEGORY);
			validity = false;
		}
		
		if (!isAlreadyXGroupSoldier(do2, employee)) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(INVALID_GROUP);
			validity = false;
		}
		
		if (!isAlreadyDrawingPayAsPer7cpc(do2)) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(NOT_IN_7CPC_PAY);
			validity = false;
		}
		
		if (isDuplicate(do2, employee)) {
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(DUPLICATE_DO2);
			validity = false;
		}
		
		if (!validity) {
			message = buffer.toString();
			do2.setReason(message);
		} else {   
			do2.setStatus(Do2Manager.VALID);     
			do2.setReason(null);
		}
	
		do2Repo.save(do2);
		return validity;
			
	}   

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		//updateEmployee(do2);
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_HIGHER_XPAY+"");
		return Do2Manager.SUCCESS;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		newAuditCage.setXGroupPay(6200);
		return newAuditCage;
	}

/*	private void updateEmployee(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		String trade = null;
		if (newDo2!=null && newDo2.getId()==do2.getId()) {
			employee.setPreviousGroupCategory(employee.getGroupCategory());
		} else {
			trade = getTrade(do2);
			GroupCategory grpcat = new GroupCategory();
			grpcat =((GroupCategory) Utils.getForeignKey(grpcat, "soldierCategory", trade));
			if (employee.getGroupCategory() != null ) {
				if (grpcat.getId()!=employee.getGroupCategory().getId()) {
					employee.setPreviousGroupCategory(employee.getGroupCategory());
				}
			}
		}
		trade = getTrade(do2);
		GroupCategory newGrpcat = new GroupCategory();
		newGrpcat =((GroupCategory) Utils.getForeignKey(newGrpcat, "soldierCategory", trade));
		employee.setGroupCategory(newGrpcat);
		Utils.getEntityManager().persist(employee);
		Utils.getEntityManager().flush();
	}*/

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
/*		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());		 
		employee.setGroupCategory(employee.getPreviousGroupCategory());
		employee.setPreviousGroupCategory(null);
*/		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_HIGHER_XPAY);
		return Do2Manager.SUCCESS;
	}
	
	public String getTrade(Do2 do2) {
		String trade = null;
		List<GroupCategory> grpCatList =gcRepo.findBySoldierGroupAndSoldierCategory(do2.getColumnA().charAt(3)+"",do2.getColumnC());
			 
		if (grpCatList.size() > 0 ) {
			if (grpCatList.get(0).getRedesignatedAbbr() != null){
				trade = grpCatList.get(0).getRedesignatedAbbr();
			} else if (grpCatList.get(0).getMergedAbbr() != null) {
				trade = grpCatList.get(0).getMergedAbbr();
			} else {
				trade = grpCatList.get(0).getSoldierCategory();
			}
		}
			return trade;
	}
	
	
	private boolean isValidCategory(Do2 do2) {
		
		char group = do2.getColumnA().charAt(0);
		String trade = do2.getColumnC().trim();
		
		if (do2.getColumnA()!=null && do2.getColumnA().length()==4)
			 group = do2.getColumnA().charAt(3);
		
		List<GroupCategory> grpCatList =gcRepo.findBySoldierGroupAndSoldierCategory(group+"", trade);
				
		 
		
		if (grpCatList!=null && grpCatList.size()>0)  {
			if(do2.getColumnA().trim().contains("X") && do2.getColumnC()!=null && X1_TRADES.contains(do2.getColumnC().trim())){
				return true;
			}
		}
		return false;
	}
	
	private boolean isAlreadyXGroupSoldier(Do2 do2, Employee employee) {
		
		if (employee.getGroupCategory()==null)
			return false;
		
		GroupCategory groupCategory = Utils.getGroupCategory(do2,"CUR");
		String soldierGroup = "";
		String soldierCategory="";
		
		if (groupCategory!=null) {
			soldierGroup = groupCategory.getSoldierGroup();
			soldierCategory = groupCategory.getSoldierCategory();
		}
		
		//System.out.println ( " SOLDIER CATEGORY " +groupCategory.getSoldierCategory());
		
		
		
		// FOr EME HMT included in view of amendment NO.24  to appendix_j for description MSTHAV 
		// w.e.f 01-10-2014 HMT trade will not be published in MSTHAV/ PROMOT do2s Insted trades
		// mentioned in X1_TRADES will be given.However, AICTGP do2s are publsihed with
		// X1TRADES only but in employee if MSTHAV/ PROMOT do2s pertains to before 01-10-2014,
		// HMT trade will be availalbe.
		
		if (Utils.getPao().getPaoCode().equals("88")) {
			if (soldierGroup.equals("X")) {
				if (!employee.getGroupCategory().getSoldierCategory().equals(soldierCategory)) {
					if (employee.getNbsubDate()!=null && !do2.getFromDate().before(employee.getNbsubDate())) {
						if (employee.getGroupCategory()!=null) {
							soldierCategory = employee.getGroupCategory().getSoldierCategory();
						}
					} else {
						if (employee.getRemusteringDate()!=null && !do2.getFromDate().before(employee.getRemusteringDate())) {
							if (employee.getGroupCategory()!=null) {
								soldierCategory = employee.getGroupCategory().getSoldierCategory();
							}
						} else {
							soldierCategory = employee.getGroupCategory().getSoldierCategory();
						}
					}
				}
				
				
				if (X1_TRADES.contains(soldierCategory) || soldierCategory.equals("HMT")) {
					return true;
				}
			}
			
		} else {
			if (soldierGroup.equals("X")) {
				if (!employee.getGroupCategory().getSoldierCategory().equals(soldierCategory)) {
					if (employee.getNbsubDate()!=null && !do2.getFromDate().before(employee.getNbsubDate())) {
						if (employee.getGroupCategory()!=null) {
							soldierCategory = employee.getGroupCategory().getSoldierCategory();
						}
					} else {
						if (employee.getRemusteringDate()!=null && !do2.getFromDate().before(employee.getRemusteringDate())) {
							if (employee.getGroupCategory()!=null) {
								soldierCategory = employee.getGroupCategory().getSoldierCategory();
							}
						} else {
							soldierCategory = employee.getGroupCategory().getSoldierCategory();
						}
					}
				}
			} else {
				if (employee.getNbsubDate()!=null && !do2.getFromDate().before(employee.getNbsubDate())) {
					if (employee.getGroupCategory()!=null) {
						soldierCategory = employee.getGroupCategory().getSoldierCategory();
					}
				} else {
					if (employee.getRemusteringDate()!=null && !do2.getFromDate().before(employee.getRemusteringDate())) {
						if (employee.getGroupCategory()!=null) {
							soldierCategory = employee.getGroupCategory().getSoldierCategory();
						}
					} else {
						soldierCategory = employee.getGroupCategory().getSoldierCategory();
					}
				}
			}
			if (X1_TRADES.contains(soldierCategory) ){
				return true;
			}	
		}
					
	

		
		/*if (Utils.getPao().getPaoCode().equals("88")) {
			if (employee.getGroupCategory()!=null) {
				if (employee.getGroupCategory().getSoldierGroup()=='X') {
					if (X1_TRADES.contains(employee.getGroupCategory().getSoldierCategory().trim()) 
							|| employee.getGroupCategory().getSoldierCategory().equals("HMT")) {
						return true;
					}
				}
			}
		} else {
			if (employee.getGroupCategory()!=null 
					&& employee.getGroupCategory().getSoldierGroup()=='X' && X1_TRADES.contains(employee.getGroupCategory().getSoldierCategory().trim())) {
				
				return true;
			} 
		}
		*/
		
		
		
		return false;
	}
	
	private boolean isAlreadyDrawingPayAsPer7cpc(Do2 do2) {
		AuditCage baseAc = AuditCageHelper.getBaseAuditCage(do2.getFromDate(), do2);
		if (baseAc!=null && baseAc.getGradePay()!=null && baseAc.getGradePay()==0 && baseAc.getPayLevel()!=null && Integer.parseInt(baseAc.getPayLevel())>2) {
			return true;
		}		
		return false;
	}
	
	private boolean isDuplicate(Do2 do2, Employee emp) {
		List<Do2> do2List =do2Repo.findByDescIdNotEqual(emp.getId(),Do2Manager.VALID,"AICTGP",do2.getId());
		 
		
		if (do2List!=null && do2List.size()>0) {
			return true;
		}		
		return false;
	}
}
