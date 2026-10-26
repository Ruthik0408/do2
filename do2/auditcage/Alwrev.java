package itsdc.dolphin.do2.auditcage;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

 
import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Allowance;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AllowanceRate;
import itsdc.dolphin.models.AppointmentRate;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.ClassPayRate;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.Pao;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 

@RestController
public class Alwrev extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	Employee employee;
	static String IS_A_RECRUIT = "Recruit Status on This Date";
	StringBuffer buffer = new StringBuffer();
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (!(isRecruit(do2))) {
			validity = false;
			buffer.append(IS_A_RECRUIT);
		}
		if (!validity) {
			do2.setStatus(Do2Manager.PENDING);
			do2.setReason(buffer.toString());
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		do2Repo.save(do2);
		return validity;
	}
	
	public boolean isRecruit(Do2 do2) {
		employee = do2.getEmployee();
		
		if (do2.getColumnA()!=null && do2.getColumnA().equals("25PC") && (do2.getColumnB()!=null && do2.getColumnB().equals("7CPC"))
				&& !do2.getFromDate().before(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate7cpc())) {
			return true;
		}
		
		
		if (do2.getFromDate().before(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate())) {
			return employee.getSubstantiveRank()!=null && !employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT);		
		} 

		if (do2.getColumnA()!=null && do2.getColumnA().equals("25PC") 
				&& !do2.getFromDate().before(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate())) {
			return true;
		}
		if (do2.getFromDate().before(DateManager.get50pcAllowanceIncreaseOnDA100pcIncreaseDate())) {
			return employee.getSubstantiveRank()!=null && !employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT);		
		}
		if (do2.getColumnA()!=null && do2.getColumnA().equals("50PC") 
				&& !do2.getFromDate().before(DateManager.get50pcAllowanceIncreaseOnDA100pcIncreaseDate())) {
			return true;
		}
		
		
		return employee.getSubstantiveRank()!=null && !employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT);
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		List<PayScale> currentPayList = null;
		String[] zCategory = { "BARBER", "BHR", "BKR", "BSMITHU", "CARPU", "CHEFCOM",
				"CHEFHOSP", "CHEFMESS", "CHEFSPEL", "CSTEWD", "DSCGD", "DVRAT", "DVRMMA", "ER", "FARR", "KLN", 
				"MASALCHI", "MCHEF", "MUSICIAN", "PKR", "PNRGD", "PTRU", "ASH",
				"SDU", "SHGD", "SRCHEF", "STEWD", "SWALA", "SWRGD", "TLR", "TSMITH", "TXRE", "UPHOL", "WM" } ;
		final List<String> Z_CATEGORY = Arrays.asList(zCategory);
		if (newAuditCage.getDo2().getFromDate().equals(DateManager.getSixthCpcAllowanceDate())){
			Date effDate = DateManager.getNextDate(newAuditCage.getDo2().getFromDate());
			Rank rank = Utils.getPborRank(newAuditCage.getDo2().getArmyNo(), effDate, "PAYFIX");
			if (!rank.getRankCode().equals(Do2Manager.RECRUIT)) {
				GroupCategory gc = Utils.getGroupCategory(newAuditCage.getDo2(), "CUR");
				String catg = gc.getSoldierCategory();	
				List<GroupCategory> grpCatList = gcRepo.findByRedesgAbbrNotNull();
					 
				if (grpCatList.size() > 0 ) {
					for (GroupCategory gc1 : grpCatList) {
						if (gc1.getRedesignatedAbbr().equals(catg)){
							catg=gc1.getSoldierCategory();
						}
					}				
				}
				if (Z_CATEGORY.contains(catg)) {
					try {
						currentPayList = psRepo.findByRankIdSgFromDateBetweenOrFromDateGreaterThanEqualStartScaleNotNull(rank.getId(),gc.getSoldierGroup(), DateManager.getDate("1997-10-10"));
							 
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					int notionalPay = newAuditCage.getOldBasicPay();
					int newBasicPay = 0;
					for (int i = currentPayList.get(0).getStartScale(); i <= currentPayList.get(0).getScaleEnd0(); i += currentPayList.get(0).getIncr0()) {
						if (i >= notionalPay) {
							newBasicPay = i;
							break;
						}
					}
					if (newBasicPay < currentPayList.get(0).getStartScale()) {
						newAuditCage.setOldBasicPay(currentPayList.get(0).getStartScale());
					} else {
						newAuditCage.setOldBasicPay(newBasicPay);
					}
				}
			}
		}		
		if (employee.getSubstantiveRank()!=null && employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)) {
			if (newAuditCage.getDo2().getColumnA()!=null && newAuditCage.getDo2().getColumnA().trim().equals("25PC") 
					&& !newAuditCage.getFromDate().before(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate())) {
				if (newAuditCage.getBasicPay()==5700) {
					newAuditCage.setBasicPay(8550);
				}
			}
			if (newAuditCage.getDo2().getColumnA()!=null && newAuditCage.getDo2().getColumnA().trim().equals("50PC") 
					&& !newAuditCage.getFromDate().before(DateManager.get50pcAllowanceIncreaseOnDA100pcIncreaseDate())) {
				if (newAuditCage.getBasicPay()==8550) {
					newAuditCage.setBasicPay(11400);
				}
			}
			return newAuditCage;
		}
		
		if (newAuditCage.getDo2().getFromDate().equals(DateManager.getDPDate())) {
			newAuditCage.setDp((int) Math.round(newAuditCage.getBasicPay() * 0.50));
		}
		// ALWREV for M S Pay & other Allowances (Date : 01.09.2008)
		
		if ((newAuditCage.getDo2().getFromDate().equals(DateManager.getSixthCpcAllowanceDate()))) {
			if (newAuditCage.getGradePay()!=null && newAuditCage.getGradePay()==0) {
				newAuditCage.setMsPay(0);
			}else {			
				newAuditCage.setMsPay(2000);
			}
		}
		
	
		
		
		
		if(newAuditCage.getFromDate().equals(DateManager.getSixthCpcAllowanceDate())){
			
			newAuditCage.setPmha(getPmhaRate(newAuditCage.getDo2(),newAuditCage));
			
			if (baseAuditCage.getTransport() != null
					&& baseAuditCage.getTransport() != 0) {
			
				newAuditCage.setTransport(AuditCageHelper.getTransportRate(
						newAuditCage, baseAuditCage));
			}
			
			if (newAuditCage.getDo2().getColumnA()!=null &&  newAuditCage.getDo2().getColumnA().trim().equals("6CPC")) {
				if (baseAuditCage.getClassPay() != null
						&& baseAuditCage.getClassPay() != 0) {
					newAuditCage.setOldClassPay(newAuditCage.getClassPay());
					newAuditCage.setClassPay((int) getRevisedClasspayRate(newAuditCage));
					
				}
			}
			
			
			
		}
		
		
		if(newAuditCage.getFromDate().equals(DateManager.getRevisedTdmergDate())){
			
			if (newAuditCage.getDo2().getColumnA()!=null &&  newAuditCage.getDo2().getColumnA().trim().equals("CLPAYREV")) {
				if (baseAuditCage.getClassPay() != null
						&& baseAuditCage.getClassPay() != 0) {
					newAuditCage.setOldClassPay(newAuditCage.getClassPay());
					newAuditCage.setClassPay((int) getRevisedClasspayRateOnTdmergDate(newAuditCage));
					
				}
			}
						
		}
		
		
		if (newAuditCage.getDo2().getColumnA()!=null && newAuditCage.getDo2().getColumnA().trim().equals("7CPC")){
			newAuditCage.setPmha(getPmhaRate(newAuditCage.getDo2(),newAuditCage));
			if (baseAuditCage.getClassPay() != null
					&& baseAuditCage.getClassPay() != 0) {
				newAuditCage.setClassPay((int) getRevisedClasspayRate(newAuditCage));
			}
			
			if (baseAuditCage.getTransport() != null
					&& baseAuditCage.getTransport() != 0) {
			
				newAuditCage.setTransport(AuditCageHelper.getTransportRate(
						newAuditCage, baseAuditCage));
			}
			
		}
		
		if (newAuditCage.getFromDate().equals(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate())){			
			newAuditCage.setPmha(AuditCageHelper.getPmhaRate(newAuditCage.getDo2(),newAuditCage));
		}
		if (newAuditCage.getFromDate().equals(DateManager.get50pcAllowanceIncreaseOnDA100pcIncreaseDate())){			
			newAuditCage.setPmha(AuditCageHelper.getPmhaRate(newAuditCage.getDo2(),newAuditCage));
		}
		
		if (DateManager.getCurrentDateCalendar(newAuditCage.getFromDate()).equals(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate7cpc())){			
		//	System.out.println ( " Im here " +newAuditCage.getFromDate());
			newAuditCage.setPmha(getPmhaRate(newAuditCage.getDo2(),newAuditCage));
		}
		
		
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
        
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);		

		if (do2.getFromDate().equals(DateManager.getSixthCpcAllowanceDate())) {
			final String[] stopAlwList = new String[] { "CCA1", "CCA", "CCB1", "CCB2", "CCOTH", "SCAGN" };
			final List<String> alwToStop = Arrays.asList(stopAlwList);
			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		}
		// w.e.f 01-07-2017 CILQ, FAA, STRINC abolished.
		if (do2.getFromDate().equals(DateManager.getSeventhCpcAllowanceDate())) {
			final String[] stopAlwList = new String[] {"CILQ", "SNLQ", "FAA", "STRINC", "HCA", "FLCHCA", "ADALLC", "ACTGA",
					"COMNDO", "INSTAL", "OUTFIT", "HSINC", "HCWA", "CONDMT","BOUNTY", "RLCA1", "RLCA2", "RLCA3", "RLCA4", "DAILY", "APTPAY"};
			final List<String> alwToStop = Arrays.asList(stopAlwList);
			AuditCageHelper.removeAllowances(alwToStop, newAuditCage);
		}
		
		if (employee.getStrincRank() != null && do2.getFromDate().equals(DateManager.getSixthCpcAllowanceDate())) {
			reviseStrIncRate();
		}
		
		if (do2.getFromDate().equals(DateManager.getSeventhCpcAllowanceDate()) && (do2.getColumnA()!=null && do2.getColumnA().equals("7CPC"))) {
			reviseStrIncRate();
		}
		//to revise gspay on 1.9.2008
		if(newAuditCage.getFromDate().equals(DateManager.getSixthCpcAllowanceDate()) && (do2.getColumnA()!=null && do2.getColumnA().equals("6CPC"))){
			reviseGsPayRate(newAuditCage.getDo2().getEmployee());
		}
		// to revised gspay w.e.f 01-07-2017
		if(newAuditCage.getFromDate().equals(DateManager.getSeventhCpcAllowanceDate()) && (do2.getColumnA()!=null && do2.getColumnA().equals("7CPC"))){
			reviseGsPayRate(newAuditCage.getDo2().getEmployee());
		}
		//reviseGsPayRate(newAuditCage.getDo2().getEmployee());
		reviseAirwcaRate();
		
		// for revision of SFA rate w.e.f 01-07-2017
		reviseSfaRate();
		
		/* to stop the DEPA when SSA admitted w.e.f 01-07-2017. 
		 Authority : 	Directorate General of Security, Office of the Inspector General, 
						Special Frontier Force, East Block-V, LEVEL-IV 
						R.K.Puram, New Delhi- 110066 letter No. 30063/HQ SFF/Accts(CDA claim)/2017 09 Nov2017.*/
		reviseSsaRate(); 
		
		reviseForlag();
		
		if (do2.getFromDate().equals(DateManager.getSixthCpcAllowanceDate()) && (do2.getColumnA()!=null && do2.getColumnA().equals("6CPC"))) {
			getRevisedAppointmentRate(do2,newAuditCage);
		}
		
		
		
		if ((do2.getFromDate().equals(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate()) && 
				do2.getColumnA() !=null && do2.getColumnA().equals("25PC")) || 
			(do2.getFromDate().equals(DateManager.get50pcAllowanceIncreaseOnDA100pcIncreaseDate()) && 
						do2.getColumnA() !=null && do2.getColumnA().equals("50PC"))){
			 List<AuditCageAllowance> allowanceList =acaRepo.findAllByAuditCageId(newAuditCage.getId());
					 
				 
			 for(AuditCageAllowance ac : allowanceList){
				  if(ac.getPayCode().getPayCode().equals("ACTGA") && ac.getDo2() !=null){
					  Do2 atcDo2 = ac.getDo2();
					  List<Allowance> allowances = Utils.getApplicableAllowanceRates(atcDo2.getDescription(),
							  atcDo2.getColumnB(), do2.getFromDate(),
								do2.getFromDate());
					  if (allowances.size() >0){
						  acaRepo.delete(ac);
						 	 
						 	newAuditCage.getAuditCageAllowances().remove(ac);
						 	AuditCageAllowance allowanceNewRecord = new AuditCageAllowance();
							allowanceNewRecord.setAuditCage(newAuditCage);
							allowanceNewRecord.setPayCode(ac.getPayCode());
							allowanceNewRecord.setDo2(ac.getDo2());
							allowanceNewRecord.setAmount((int) allowances.get(0).getRateAmount());
							allowanceNewRecord.setCreatedAt(new Timestamp(System.currentTimeMillis()));
							acaRepo.save(allowanceNewRecord);
							 
					  }
				  }
			 }
		}
		
		if (do2.getFromDate().equals(DateManager.getAlwrevDateForHraLFRevision())) {
			List<AuditCageAllowance> acaList =acaRepo.findAllByAuditCageId(newAuditCage.getId());
				 List<String> descList=Arrays.asList(new String[] {"HRA","HRAFAM"});
			for (AuditCageAllowance aca : acaList) {
				if (aca.getPayCode().getPayCode().equals("HRALF") || aca.getPayCode().getPayCode().equals("FAMLF")) {
					List<Do2> do2List =do2Repo.findByDescListColAFromDateLessThan(do2.getEmployee().getId(),Do2Manager.VALID,do2.getFromDate(),"GRANTED", descList);
					 
					String columnC ="";
					List<Allowance> allowanceList=null;
					if (do2List.size()>0) {					
						
							if (do2List.get(0).getColumnC()!=null){
								if (do2List.get(0).getColumnC().startsWith("CCZ")){
									 columnC = "CCZ-1";				
								}
								if ( do2List.get(0).getColumnC().startsWith("CCY")){
									 columnC = "CCY-1";				
								}
								if ( do2List.get(0).getColumnC().startsWith("CCX")){
									 columnC = "CCX-1";				
								}
							}
							allowanceList = Utils.getApplicableAllowanceRates(do2List.get(0).getDescription().substring(0,3), 
									do2.getFromDate(), do2.getFromDate(), columnC);
							
							if (allowanceList.size()>0) {
							 	acaRepo.delete(aca);
							 	newAuditCage.getAuditCageAllowances().remove(aca);
							 	AuditCageAllowance allowanceNewRecord = new AuditCageAllowance();
								allowanceNewRecord.setAuditCage(newAuditCage);
								allowanceNewRecord.setPayCode(aca.getPayCode());
								allowanceNewRecord.setDo2(aca.getDo2());
								allowanceNewRecord.setAmount((int) allowanceList.get(0).getRateAmount());
								allowanceNewRecord.setCreatedAt(new Timestamp(System.currentTimeMillis()));
								acaRepo.save(allowanceNewRecord);
							}
					}	
				
				}
			}
		}
		if (do2.getFromDate().equals(DateManager.getAwardRevisionDate())	|| 
			do2.getFromDate().equals(DateManager.getAwardRevisionTwoThousandElevenDate()) || 
			do2.getFromDate().equals(DateManager.getAwardRevisionTwoThousandSeventeenDate())){
			List<AuditCageAllowance> acaList = acaRepo.findAllByAuditCageId(newAuditCage.getId());
			 
			 for(AuditCageAllowance aca : acaList){
				  if (Do2Manager.AWARD_LIST.contains(aca.getPayCode().getPayCode())){
					  String description="AWARD";
					  String allowanceName=aca.getPayCode().getPayCode();
					  if (aca.getPayCode().getPayCode().equals("BARSM")) {
						  allowanceName="SM";
					  }
					  if (aca.getPayCode().getPayCode().equals("BARSC")) {
						  allowanceName="SC";
					  }
					  List<Allowance>  allowanceRateList = Utils.getApplicableAllowanceRates(description, allowanceName, 
								do2.getFromDate(), do2.getFromDate());
					  if (allowanceRateList.size() > 0){
						  acaRepo.delete(aca);
						  
						  newAuditCage.getAuditCageAllowances().remove(aca);
						  AuditCageAllowance allowanceNewRecord = new AuditCageAllowance();
						  allowanceNewRecord.setAuditCage(newAuditCage);
						  allowanceNewRecord.setPayCode(aca.getPayCode());
						  allowanceNewRecord.setDo2(aca.getDo2());
						  if (aca.getPayCode().getPayCode().startsWith("BAR")) {
							  allowanceNewRecord.setAmount((int)allowanceRateList.get(0).getRateAmount()*2);
						  } else {
							  allowanceNewRecord.setAmount((int)allowanceRateList.get(0).getRateAmount());
						  }
						  allowanceNewRecord.setCreatedAt(new Timestamp(System.currentTimeMillis()));
						  acaRepo.save(allowanceNewRecord);
					  }					  
				  }
			 }		
		}
		
		return Do2Manager.SUCCESS;
	}

	public void updateValidLraDo2(Do2 do2) {
		do2.setReason(null);
		do2.setStatus(Do2Manager.VALID);
		do2Repo.save(do2);
	}
	public double getRevisedClasspayRate(AuditCage auditCage) {
		
		double classPay=auditCage.getClassPay();
		double drawnClassPay=0;
			employee = auditCage.getDo2().getEmployee();
		String soldierClass="";
		Date previousDate = DateManager.getDate(auditCage.getFromDate(),-1);
		List<AuditCage> atcageList =acRepo.findPreviousDateEffectiveAc(employee.getId(),auditCage.getFromDate());
				
			 
		if(atcageList.size() >0){
			drawnClassPay= atcageList.get(0).getClassPay();
		}
		List<ClassPayRate> classPayRateList1 = classPayRepo.findByGroupCatRateFromDateBetweenOrFromDateGreaterThan(Utils.getGroupCategory(auditCage.getDo2(), "CUR").getId(), classPay, previousDate);
				 
	
		if(classPayRateList1.size() >0){
			soldierClass = classPayRateList1.get(0).getClassPayCode()+"";
		}
				
		List<ClassPayRate> classPayRateList = classPayRepo.findByGroupCatClassPayCodeFromDateBetweenOrFromDateGreaterThan(Utils.getGroupCategory(auditCage.getDo2(),"CUR").getId(), soldierClass, auditCage.getDo2().getFromDate());
			 
		
		if (classPayRateList.size() > 0) {
	 		classPay =  classPayRateList.get(0).getRate();	 		
		}
		return classPay;

	}


public double getRevisedClasspayRateOnTdmergDate(AuditCage auditCage) {
		
		double classPay=auditCage.getClassPay();
		double drawnClassPay=0;
			employee = Utils.getCurrentEmployee(auditCage.getDo2().getArmyNo());
		String soldierClass="";

			
		Date previousDate = DateManager.getDate(auditCage.getFromDate(),-1);
		List<AuditCage> atcageList = acRepo.findPreviousDateEffectiveAc(employee.getId(),auditCage.getFromDate());
				
			 
		if(atcageList.size() >0){
			drawnClassPay= atcageList.get(0).getClassPay();
		}
		
		soldierClass= employee.getSoldierClass().toString();
		
				
		List<ClassPayRate> classPayRateList = classPayRepo.findByGroupCatClassPayCodeFromDateBetweenOrFromDateGreaterThan(Utils.getGroupCategory(auditCage.getDo2(),"CUR").getId(), soldierClass, auditCage.getDo2().getFromDate());
		 
	
		if (classPayRateList.size() > 0) {
		//	System.out.println ( " class pay Rate list  " +classPayRateList.get(0).getRate()   +" id"   +classPayRateList.get(0).getId());
	 		classPay =  classPayRateList.get(0).getRate();	 		
		}
		
		
		return classPay;

	}

	

	
	public void reviseStrIncRate() {
		List<AuditCageAllowance> newAuditCageAllowanceList = acaRepo.findAllByAuditCageId(newAuditCage.getId());
				
			 
		for (AuditCageAllowance aca : newAuditCageAllowanceList) {
			if (aca.getPayCode().getPayCode().equals("STRINC")) {
				Rank rank = employee.getStrincRank();
			if(newAuditCage.getDo2().getFromDate().equals(DateManager.getSixthCpcAllowanceDate())) {
				List<Allowance> alwList = Utils.getApplicableAllowanceRates(
						"STRINC", newAuditCage.getDo2().getFromDate(),
						newAuditCage.getDo2().getFromDate(), rank);
				if (alwList.size() > 0) {
				 	acaRepo.delete(aca);
				 	newAuditCage.getAuditCageAllowances().remove(aca);
				 	AuditCageAllowance allowanceNewRecord = new AuditCageAllowance();
					allowanceNewRecord.setAuditCage(newAuditCage);
					allowanceNewRecord.setPayCode(aca.getPayCode());
					allowanceNewRecord.setDo2(aca.getDo2());
					allowanceNewRecord.setAmount((int)alwList.get(0).getRateAmount());
					allowanceNewRecord.setCreatedAt(new Timestamp(System.currentTimeMillis()));
					acaRepo.save(allowanceNewRecord);
				}
			} else if(newAuditCage.getDo2().getFromDate().equals(DateManager.getSeventhCpcAllowanceDate())) {
				AuditCageHelper.removeAllowances(Arrays.asList(new String[]{ Do2Manager.STRINC }), newAuditCage);
			}
				
			}
		}
	}
	
	/*public void reviseSpincRate() {
		List<AuditCageAllowance> newAuditCageAllowanceList = Utils.getEntityManager()
				.createQuery(
						"SELECT aca FROM AuditCageAllowance aca WHERE aca.auditCage=:auditCage ")
				.setParameter("auditCage", newAuditCage).getResultList();
		for (AuditCageAllowance aca : newAuditCageAllowanceList) {
			if (aca.getPayCode().getPayCode().equals("SPINC")) {
				Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");

				List<Allowance> alwList = Utils.getApplicableAllowanceRates(
						"STRINC", newAuditCage.getDo2().getFromDate(),
						newAuditCage.getDo2().getFromDate(), rank);
				if (alwList.size() > 0) {
				 	Utils.getEntityManager().remove(aca);
				 	AuditCageAllowance allowanceNewRecord = new AuditCageAllowance();
					allowanceNewRecord.setAuditCage(newAuditCage);
					allowanceNewRecord.setPayCode(aca.getPayCode());
					allowanceNewRecord.setDo2(aca.getDo2());
					allowanceNewRecord.setAmount((int)alwList.get(0).getRateAmount());
					allowanceNewRecord.setCreatedAt(new Date());
					Utils.getEntityManager().persist(allowanceNewRecord);
					Utils.getEntityManager().flush();
				}
			}
		}
	}
	
	public int getIncrementVcpc(Do2 do2) {
		int incr = 0;
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		Rank rank = Utils.getPborRank(do2.getArmyNo(), do2.getFromDate(),"PAYFIX");
		char soldierGroup = employee.getGroupCategory().getSoldierGroup();
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
		
		List<PayScale> payList = Utils.getEntityManager().createQuery(
						"SELECT ps FROM PayScale AS ps WHERE ps.rank=:rank "
								+ " AND ps.startScale > 0 "
								+ " AND ps.soldierGroup.soldierGroup=:grp "
								+ " AND ((:fromDate between ps.fromDate and ps.toDate ) "
								+ " or (:fromDate >= ps.fromDate and ps.toDate is null ))")
				.setParameter("rank", rank).setParameter("grp", soldierGroup)
				.setParameter("fromDate", fifthCpcDate).getResultList();

		if (payList.size() > 0) {
			incr = payList.get(0).getIncr0();
		}
		return incr;
	}*/
	
	public void reviseAirwcaRate() {
		List<AuditCageAllowance> newAuditCageAllowanceList = acaRepo.findAllByAuditCageId(newAuditCage.getId());
			 
		for (AuditCageAllowance aca : newAuditCageAllowanceList) {
		    int chkAmt = 0;
			if (aca.getPayCode().getPayCode().equals("AIRWCA")){
				
				chkAmt = aca.getAmount();
				
				
				
				if (newAuditCage.getFromDate().equals(DateManager.getSixthCpcAllowanceDate()) && newAuditCage.getDo2().getColumnA().equals("6CPC")){
					if (aca.getAmount() == 75){
						chkAmt = 150;
					} else if (aca.getAmount() == 150){
						chkAmt = 300;
					} 
				} else if (newAuditCage.getFromDate().equals(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate()) &&
						   newAuditCage.getDo2().getColumnA() != null && newAuditCage.getDo2().getColumnA().equals("25PC")){
					if (aca.getAmount() == 150){
						chkAmt = 188;
					} else if (aca.getAmount() == 300){
						chkAmt = 375;
					}
				} else if (newAuditCage.getFromDate().equals(DateManager.get50pcAllowanceIncreaseOnDA100pcIncreaseDate()) &&
						   newAuditCage.getDo2().getColumnA() != null && newAuditCage.getDo2().getColumnA().equals("50PC")){
					if (aca.getAmount() == 188){
						chkAmt = 225;
					} else if (aca.getAmount() == 375){
						chkAmt = 450;
					}
				} else if (newAuditCage.getFromDate().equals(DateManager.getSeventhCpcAllowanceDate()) &&
						   newAuditCage.getDo2().getColumnA() != null && newAuditCage.getDo2().getColumnA().equals("7CPC")){
					if (aca.getAmount() == 225){
						chkAmt = 338;
					} else if (aca.getAmount() == 450){
						chkAmt = 675;
					}
				} else if (newAuditCage.getFromDate().equals(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate7cpc()) &&
						   newAuditCage.getDo2().getColumnA() != null && newAuditCage.getDo2().getColumnA().equals("25PC") && 
						   newAuditCage.getDo2().getColumnB() != null && newAuditCage.getDo2().getColumnB().equals("7CPC")){
					if (aca.getAmount() == 338){
						chkAmt = 422;
					} else if (aca.getAmount() == 675){
						chkAmt = 843;
					}
				}
				aca.setAmount(chkAmt);
				acaRepo.save(aca);
			}
		}
	}
	
	public void reviseGsPayRate(Employee employee) {
		
		List<Do2>gspayList = null;
		
		List<AuditCageAllowance> newAuditCageAllowanceList =acaRepo.findAllByAuditCageId(newAuditCage.getId());
			 
		for (AuditCageAllowance aca : newAuditCageAllowanceList) {
		   
			//Code included for revision of gspay from 1.9.2008 ,revision in auditcagehelper removed on 24122011
			if(aca.getPayCode().getPayCode().equals("GSPAY")){
					int gspayRate=getGsPayRate(newAuditCage);
					
					if (newAuditCage.getFromDate().equals(DateManager.getSixthCpcAllowanceDate()) && newAuditCage.getDo2().getColumnA().equals("6CPC")){
						
						gspayList=do2Repo.findByDescFromDateEqual(employee.getId(),Do2Manager.VALID,"GSPAY",DateManager.getSixthCpcAllowanceDate());
					 
						if (gspayList.size() > 0){
							return;
						}
										
						if (gspayRate==32){
							  gspayRate=64;
						} else if (gspayRate==64){
							  gspayRate =128;
						} else if (gspayRate==96){
							  gspayRate=192;
						}
						
				  } else if (newAuditCage.getFromDate().equals(DateManager.getSeventhCpcAllowanceDate()) && newAuditCage.getDo2().getColumnA().equals("7CPC")){
					
					gspayList=do2Repo.findByDescFromDateEqual(employee.getId(),Do2Manager.VALID,"GSPAY",DateManager.getSeventhCpcAllowanceDate());
					 
							if (gspayList.size() > 0){
								return;
							}
					if (gspayRate==64){
						  gspayRate=144;
					} else if (gspayRate==128){
						  gspayRate =288;
					} else if (gspayRate==192){
						  gspayRate=432;
					}
				}
			
			    acaRepo.delete(aca);
			    newAuditCage.getAuditCageAllowances().remove(aca);
			 	AuditCageAllowance allowanceNewRecord = new AuditCageAllowance();
				allowanceNewRecord.setAuditCage(newAuditCage);
				allowanceNewRecord.setPayCode(aca.getPayCode());
				allowanceNewRecord.setDo2(aca.getDo2());
				allowanceNewRecord.setAmount(gspayRate);
				allowanceNewRecord.setCreatedAt(new Timestamp(System.currentTimeMillis()));
				acaRepo.save(allowanceNewRecord);
			}
		 }
		
	}
	
	
	
    private AuditCage getAuditCageBeforeAlwrev(AuditCage auditcage){
    	List<AuditCage> auditCageList = acRepo.findPreviousDateValidEffectiveAc(auditcage.getDo2().getEmployee().getId(),auditcage.getDo2().getFromDate());
    	 
    	if (auditCageList == null && auditCageList.size() == 0){
    		return null;
    	} 
    	
    	if (auditCageList.size()==0) {
    		return null;
    	} else {
    		return auditCageList.get(0);
    	}

    }
	private int getGsPayRate(AuditCage auditcage){
		int gspay = 0;
		AuditCage ac = getAuditCageBeforeAlwrev(auditcage);
		if (ac == null) {
			return gspay;
		} else {
			List<AuditCageAllowance> auditCageAllowanceList = acaRepo.findAllByAuditCageId(ac.getId());
					
			 
			for (AuditCageAllowance aca : auditCageAllowanceList) {
				if(aca.getPayCode().getPayCode().equals("GSPAY")){
					  gspay=aca.getAmount();
				}
			}
		}
		return gspay;
	}
	private void getRevisedAppointmentRate(Do2 do2,AuditCage baseAuditCage) {
		List<AuditCageAllowance> newAuditCageAllowanceList =acaRepo.findAllByAuditCageId(newAuditCage.getId());
				 
		Pao pao =null;
	
		List<AppointmentRate> appointmentRateList=null;
		for (AuditCageAllowance aca : newAuditCageAllowanceList) {
			if (aca.getPayCode().getPayCode().equals("APTPAY")) {

				if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_NASIK))
				{
					pao = Utils.getPao();
				} else {
					pao = null;
				}
				Do2 apptDo2 = aca.getDo2();
				if(apptDo2 !=null){
					if(pao == null){
			        	appointmentRateList = apptRateRepo.findByApptNameFromDateBetweenOrFromDateGreater(apptDo2.getColumnA(),do2.getFromDate());
			        
			        }else{
			        	 appointmentRateList =apptRateRepo.findByApptNamePaoIdFromDateBetweenOrFromDateGreater(apptDo2.getColumnA(),pao.getId(),do2.getFromDate());
			        		 
			        }	
				}
		        
				if (appointmentRateList!=null && appointmentRateList.size() > 0) {
					 	acaRepo.delete(aca);
					 	newAuditCage.getAuditCageAllowances().remove(aca);
					 	AuditCageAllowance allowanceNewRecord = new AuditCageAllowance();
						allowanceNewRecord.setAuditCage(newAuditCage);
						allowanceNewRecord.setPayCode(aca.getPayCode());
						allowanceNewRecord.setDo2(aca.getDo2());
						allowanceNewRecord.setAmount((int) appointmentRateList.get(0).getRate());
						allowanceNewRecord.setCreatedAt(new Timestamp(System.currentTimeMillis()));
						acaRepo.save(allowanceNewRecord);
				} else {
					acaRepo.delete(aca);
					newAuditCage.getAuditCageAllowances().remove(aca);
				 	AuditCageAllowance allowanceNewRecord = new AuditCageAllowance();
					allowanceNewRecord.setAuditCage(newAuditCage);
					allowanceNewRecord.setPayCode(aca.getPayCode());
					allowanceNewRecord.setDo2(aca.getDo2());
					allowanceNewRecord.setAmount(aca.getAmount() *2);
					allowanceNewRecord.setCreatedAt(new Timestamp(System.currentTimeMillis()));
					acaRepo.save(allowanceNewRecord);
				}
			}
		}

	}


	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {

		return Do2Manager.SUCCESS;
	}
	public int getPmhaRate(Do2 do2, AuditCage auditCage) {	
		double pmhaAmount = auditCage.getPmha();
		String allowanceName = "PMHAL";
		Pao pao = null;
		if (Utils.getPao().getPaoCode().equals(Do2Manager.PAO_DSC)) {
			pao = Utils.getPao();
		}
		Employee employee = Utils.getCurrentEmployee(do2.getArmyNo());
		int gsoCode = 1;
		if(employee.getGsoCode() != null){
			gsoCode = employee.getGsoCode();
			if (gsoCode == 3){
				gsoCode = 1;
			}
		}
		
		if (employee.getSubstantiveRank().getRankCode().equals(Do2Manager.RECRUIT)) {
			return 0;
		}
		if ((employee.getSubstantiveRank().getRankCode().equals(Do2Manager.HONYCP) && 
			!employee.getHonyCaptainDate().after(do2.getFromDate())) ||
		   (employee.getSubstantiveRank().getRankCode().equals(Do2Manager.HONYLT) && 
			!employee.getHonyLieuDate().after(do2.getFromDate()))) {
			return 0;
		}
		List<Allowance> pmhaAlwList = null;
		Date fromDate = auditCage.getFromDate();
		
		AuditCage ac = getAuditCageBeforeAlwrev(auditCage);
		if (ac!=null) {
			pmhaAmount = ac.getPmha();
			fromDate = ac.getFromDate();
		}		
		List<Do2>pmhahList=do2Repo.findByDescFromDateEqual(do2.getEmployee().getId(),Do2Manager.VALID, "PMHAH", DateManager.getSixthCpcAllowanceDate() );
				
			 
		if (pmhahList.size() > 0){
			List<AuditCage> auditCageList =acRepo.findEffectiveAuditCageForADo2(do2.getEmployee().getId(), do2.getId());
			 
	    	if (auditCageList != null && auditCageList.size() > 0){
	    		ac=auditCageList.get(0);
	    		pmhaAmount=ac.getPmha();
	    		fromDate = ac.getFromDate();
	    	}
		}	
				
		List<AllowanceRate>allowanceRateList = alwRateRepo.findByAlwNameLikeAndRateFromdateLessThanEqual(pmhaAmount, fromDate, "PMHA");
		 

		
		if (allowanceRateList.size()>0) {
			allowanceName = allowanceRateList.get(0).getAllowanceName();
		}
		
		if (pao!=null && pao.getPaoCode().equals(Do2Manager.PAO_DSC)) {
			pmhaAlwList = Utils.getApplicableAllowanceRates(allowanceName,
					do2.getFromDate(), do2.getFromDate(),
					gsoCode, pao);
		} else {
			pmhaAlwList = Utils.getApplicableAllowanceRates(allowanceName,
					do2.getFromDate(), do2.getFromDate(),
					gsoCode, pao);
		}	   
		if (pmhaAlwList != null && pmhaAlwList.size()>0) {
			return (int) pmhaAlwList.get(0).getRateAmount();
		} 
		
		return (int) pmhaAmount;
	}
	
	
	public void reviseSsaRate() {
		List<AuditCageAllowance> newAuditCageAllowanceList = acaRepo.findAllByAuditCageId(newAuditCage.getId());
				
		for (AuditCageAllowance aca : newAuditCageAllowanceList) {
		    int chkAmt = 0;
			if (aca.getPayCode().getPayCode().equals("SSA")){
			//	System.out.println ( " AAAAAAAAAAAAAA   " +aca.getPayCode().getPayCode());
			 if (newAuditCage.getFromDate().equals(DateManager.getSeventhCpcAllowanceDate()) &&
						   newAuditCage.getDo2().getColumnA() != null && newAuditCage.getDo2().getColumnA().equals("7CPC")){
				 AuditCageHelper.removeAllowances(Arrays.asList(new String[]{ Do2Manager.DEPA }), newAuditCage);
			 }
		   }
		}
	}
	
	public void reviseSfaRate() {
	
			List<AuditCageAllowance> newAuditCageAllowanceList =acaRepo.findAllByAuditCageId(newAuditCage.getId());
			 
			for (AuditCageAllowance aca : newAuditCageAllowanceList) {
			    int chkAmt = 0;
				if (aca.getPayCode().getPayCode().equals("SFA")){
					chkAmt = aca.getAmount();
					 if (newAuditCage.getFromDate().equals(DateManager.getSeventhCpcAllowanceDate()) &&
							   newAuditCage.getDo2().getColumnA() != null && newAuditCage.getDo2().getColumnA().equals("7CPC")){
						if ((aca.getAmount() == 3240) || (aca.getAmount() == 3880) || (aca.getAmount() == 5820)){
							chkAmt = 9700;
						} else if ((aca.getAmount() == 2000) || (aca.getAmount() == 2400) || (aca.getAmount() == 3600)){
							chkAmt = 6000;
						}
					}else if (newAuditCage.getFromDate().equals(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate7cpc()) &&
							   newAuditCage.getDo2().getColumnA() != null && newAuditCage.getDo2().getColumnA().equals("25PC") && 
							   newAuditCage.getDo2().getColumnB() != null && newAuditCage.getDo2().getColumnB().equals("7CPC")){ 
					 
						if ((aca.getAmount() == 6000)){
							chkAmt = 7500;
						} else if ((aca.getAmount() == 9700)){
							chkAmt = 12125;
						}
					}
					aca.setAmount(chkAmt);
					acaRepo.save(aca); 
				}
			}
		}
	

	public void reviseForlag() {
		List<AuditCageAllowance> newAuditCageAllowanceList =acaRepo.findAllByAuditCageId(newAuditCage.getId());
				
		for (AuditCageAllowance aca : newAuditCageAllowanceList) {
		    int chkAmt = 0;
			if (aca.getPayCode().getPayCode().equals("FORLAG")){
				chkAmt = aca.getAmount();
				if (newAuditCage.getFromDate().equals(DateManager.getSixthCpcAllowanceDate()) && newAuditCage.getDo2().getColumnA().equals("6CPC")){
					if (aca.getAmount() == 200){
						chkAmt = 600;
					} else if (aca.getAmount() == 250){
						chkAmt = 750;
					} else if (aca.getAmount() == 300){
						chkAmt = 900;
					} 
				} else if (newAuditCage.getFromDate().equals(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate()) &&
						   newAuditCage.getDo2().getColumnA() != null && newAuditCage.getDo2().getColumnA().equals("25PC")){
					if (aca.getAmount() == 600){
						chkAmt = 750;
					} else if (aca.getAmount() == 750){
						chkAmt = 938;
					} else if (aca.getAmount() == 900){
						chkAmt = 1125;
					} 
				} else if (newAuditCage.getFromDate().equals(DateManager.get50pcAllowanceIncreaseOnDA100pcIncreaseDate()) &&
						   newAuditCage.getDo2().getColumnA() != null && newAuditCage.getDo2().getColumnA().equals("50PC")){
					if (aca.getAmount() == 750){
						chkAmt = 900;
					} else if (aca.getAmount() == 938){
						chkAmt = 1125;
					} else if (aca.getAmount() == 1125){
						chkAmt = 1350;
					} 
				} else if (newAuditCage.getFromDate().equals(DateManager.getSeventhCpcAllowanceDate()) &&
						   newAuditCage.getDo2().getColumnA() != null && newAuditCage.getDo2().getColumnA().equals("7CPC")){
					if (aca.getAmount() == 900){
						chkAmt = 1350;
					} else if (aca.getAmount() == 1125){
						chkAmt = 1689;
					} else if (aca.getAmount() == 1350){
						chkAmt = 2025;
					} 
				} else if (newAuditCage.getFromDate().equals(DateManager.get25pcAllowanceIncreaseOnDA50pcIncreaseDate7cpc()) &&
						   newAuditCage.getDo2().getColumnA() != null && newAuditCage.getDo2().getColumnA().equals("25PC") && 
						   newAuditCage.getDo2().getColumnB() != null && newAuditCage.getDo2().getColumnB().equals("7CPC")){
					if (aca.getAmount() == 1350){
						chkAmt = 1688;
					} else if (aca.getAmount() == 1689){
						chkAmt = 2111;
					} else if (aca.getAmount() == 2025){
						chkAmt = 2531;
					} 
				}
				aca.setAmount(chkAmt);
				acaRepo.save(aca);
			}
		}
	}
	
}
