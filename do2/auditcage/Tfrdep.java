package itsdc.dolphin.do2.auditcage;

import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.AuditCageAllowance;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.LpcDeputation;
import itsdc.dolphin.models.Unit;
import itsdc.dolphin.models.Usr;
import itsdc.dolphin.repositories.LpcDeputationRepository;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.persistence.EntityManager;

import org.springframework.web.bind.annotation.RestController;

 



@RestController
public class Tfrdep extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
				
	 
	Employee employee;
	private StringBuffer buffer = new StringBuffer();
	static final public String DUPLICATE = "AU:Duplicate Do2. Already TFRDEP do2 exists for the date.\n";
	static final public String TFRDEP = "AU:PBOR is on Already on Deputation. \n";

	
	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		
		if(isTfrDepExists(do2)){
			buffer.append(DUPLICATE);
			validity=false;
		}
		if(noRevDepExists(do2)){
			buffer.append(TFRDEP);
			validity=false;
		}
	
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		
		do2.setReason(buffer.toString());
	// As per the orders of PCDA Bangalore CGUARD are also exempted.
		if(newDo2.getId()==do2.getId() && !do2.getColumnA().equals("UNMISN")){
	/*	if(newDo2.getId()==do2.getId() && 
				(!do2.getColumnA().equals("UNMISN") || !do2.getColumnA().equals("CGUARD"))){*/
			if (!isPosinDo2Generated(do2)) {
				createPosinDo2(do2);
			}
		}
		
		do2Repo.save(do2);
		return validity;
	}
	
	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {

		if (newAuditCage.getDo2().getFromDate().after(DateManager.getCgguardDeputationDate())) {
			if (Do2Manager.getValidStopPayDeputationFromAug2016().contains(newAuditCage.getDo2().getColumnA())){
		 		newAuditCage.setAbsentFlag(true);	
		  	}else{
		  		newAuditCage.setAbsentFlag(false);
		  	}
		 	
		 	if (!Do2Manager.getValidStopPayDeputation().contains(newAuditCage.getDo2().getColumnA())){
		 		newAuditCage.setTransport(0);	
		  	}
		 	
		 	/*if (!Do2Manager.getValidStopPayDeputationFromAug2016().contains(newAuditCage.getDo2().getColumnA())){
		 		newAuditCage.setTransport(0);	
		  	}*/
		} else {
			if (Do2Manager.getValidStopPayDeputation().contains(newAuditCage.getDo2().getColumnA())){
		 		newAuditCage.setAbsentFlag(true);	
		  	}else{
		  		newAuditCage.setAbsentFlag(false);
		  	}
		 	
		 	if (!Do2Manager.getValidStopPayDeputation().contains(newAuditCage.getDo2().getColumnA())){
		 		newAuditCage.setTransport(0);	
		  	}
		 	
		 	/*if (!Do2Manager.getValidStopPayDeputation().contains(newAuditCage.getDo2().getColumnA())){
		 		newAuditCage.setTransport(0);	
		  	}*/
		}
			 
	 	
	 	newAuditCage.setDeputationStatus(Do2Manager.getDeputationStatusCodeMap().get(newAuditCage.getDo2().getColumnA()).charAt(0)+"");
	 	newAuditCage.setDeputationFlag(true);
	 	
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		Date revdepdate = null;
		List<Do2> do2List = null;
		List<Arrears> arrList = null;
		int totamount = 0;
		String paycode = "";
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		
		if (do2.getFromDate().after(DateManager.getCgguardDeputationDate())) {
			if (Do2Manager.getValidStopPayDeputationFromAug2016().contains(do2.getColumnA())){
				updateLpcDeputation(do2);
			}
		} else {
			if (Do2Manager.getValidStopPayDeputation().contains(do2.getColumnA())){
				 updateLpcDeputation(do2);
			}
		}
		
		if (do2.getColumnA().equals("UNMISN")) {
			stopAllAllowances(newAuditCage);
		} else  {
			 AuditCageHelper.stopAllAllowances(newAuditCage);	
		} 
		updateBdtInfo(do2);
		if (do2.getColumnA().equals("UNMISN") || do2.getColumnA().equals("IMTRAT")) {
			return Do2Manager.SUCCESS;
		}
		
		/*
		List<Do2> revdepList = do2Repo.findByFromDateGreaterThanAndMultipleStatusOrderByFromDate(do2.getEmployee().getId(),
																						"REVDEP", do2.getFromDate());*/
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE  " +
				" d.employee = :emp AND d.description = 'REVDEP' AND d.status in ('V', 'P') " +
				" AND d.fromDate > :fromDate order by d.fromDate")
				.setParameter("emp", do2.getEmployee())
				.setParameter("fromDate", do2.getFromDate()).getResultList();*/
		/*
		if (revdepList != null && revdepList.size()>0) {
			revdepdate = revdepList.get(0).getFromDate();
		} */
		/*if (revdepdate == null) {
			do2List = Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE  " +
					" d.employee = :emp AND d.auditCage = false AND d.status = 'V' " +
					" AND d.fromDate >= :fromDate order by d.fromDate")
					.setParameter("emp", do2.getEmployee())
					.setParameter("fromDate", do2.getFromDate()).getResultList();			
		} else {*/
			do2List = do2Repo.findByFromDateGreaterThanEqualToAndFromDateLessThanOrderByFromDate(do2.getEmployee().getId(),
																						do2.getFromDate(), revdepdate);
					
		/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE  " +
				" d.employee = :emp AND d.auditCage = false AND d.status = 'V' " +
				" AND d.fromDate >= :fromDate and d.toDate < :toDate order by d.fromDate")
				.setParameter("emp", do2.getEmployee())
				.setParameter("fromDate", do2.getFromDate())
				.setParameter("toDate", revdepdate).getResultList();*/
	//	}
			
			
			
			if (do2.getFromDate().after(DateManager.getCgguardDeputationDate())) {
				if (Do2Manager.getValidStopPayDeputationFromAug2016().contains(do2.getColumnA())){
					
					List<Do2> revdepList = do2Repo.findByFromDateGreaterThanAndMultipleStatusOrderByFromDate(do2.getEmployee().getId(),
																									"REVDEP", do2.getFromDate());
				
					
					if (revdepList != null && revdepList.size()>0) {
						revdepdate = revdepList.get(0).getFromDate();
					}
				
						do2List = do2Repo.findByFromDateGreaterThanEqualToAndFromDateLessThanOrderByFromDate(do2.getEmployee().getId(),
																									do2.getFromDate(), revdepdate);
								
				
					
				}
			} else {
				if (Do2Manager.getValidStopPayDeputation().contains(do2.getColumnA())){
					List<Do2> revdepList = do2Repo.findByFromDateGreaterThanAndMultipleStatusOrderByFromDate(do2.getEmployee().getId(),
							"REVDEP", do2.getFromDate());


						if (revdepList != null && revdepList.size()>0) {
								revdepdate = revdepList.get(0).getFromDate();
						}

						do2List = do2Repo.findByFromDateGreaterThanEqualToAndFromDateLessThanOrderByFromDate(do2.getEmployee().getId(),
							do2.getFromDate(), revdepdate);
						}
			}
			
			
		
		if (do2List != null && do2List.size()>0) {
			for (Do2 d : do2List ){
				totamount = 0;
				arrList = arrRepo.findByEmployeeIdAndDo2Id(do2.getEmployee().getId(), d.getId());
						
			//			Utils.getEntityManager().createQuery("Select a from Arrears a where a.employee = :emp and a.do2 = :id")
				//		.setParameter("emp", do2.getEmployee())
				//		.setParameter("id", d).getResultList();
				if (arrList.size() > 0){
					for (Arrears arr : arrList ){
						totamount += arr.getAmount();
					}
				}
				if (totamount == 0)
					continue;
				paycode = d.getDescription();
				if (paycode.equals(Do2Manager.ENROLL) || paycode.equals("RECALL")){
					paycode = Do2Manager.DAILY;
				} else if (paycode.equals("FINE") || paycode.equals("PEDEDN")) {
					paycode = Do2Manager.BASIC_PAY;
				} else if (paycode.equals("RL") || paycode.equals("PRPLVE")) {
					paycode = Do2Manager.NRA;
				} else if (paycode.equals("CEATWN") || paycode.equals("CEAHC")) {
					paycode = "CEA";
				} else if (paycode.equals("HSBTWN")){
					paycode = "HOSSUB";
				} else if (paycode.equals("DMALLC")){
					paycode = "DIPMNY";
				} else if (paycode.equals("DIVALL")){
					paycode = "DIVING";
				} else if (paycode.equals("FDAILY") || paycode.equals("RDAILY")){
					paycode = Do2Manager.DAILY;
				} else if (paycode.equals("ENCASH") && d.getColumnA().equals("LTC")){
					paycode = "FSCASH";
				} else if (paycode.equals("ENCFIN")){
					paycode = "FSCASH";
				}
				Arrears arrears = new Arrears();
				arrears.setDo2(d);
				arrears.setFromDate(d.getFromDate());
				arrears.setToDate(d.getToDate());
				arrears.setAmount((double) Math.round(totamount * -1));
				arrears.setPayCode(Utils.getPayCode(paycode));
				arrears.setQe(Utils.getCurrentQuarter());
				arrears.setDo2ReverseArrears(do2);
				arrears.setRoundOffAmount(false);
				arrears.setRegularPay(false);
				arrears.setEmployee(Utils.getCurrentEmployee(do2.getArmyNo()));
				arrears.setArrestPayment(false);
				arrears.setPaid(false);
				arrears.setCreatedAt(new Timestamp(System.currentTimeMillis()));
				arrRepo.save(arrears);
			//	Utils.getEntityManager().persist(arrears);
		//		Utils.getEntityManager().flush();
				d.setReviewQe(Utils.getCurrentQuarter());
			//	Utils.getEntityManager().merge(d);
			}
		}
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		List<Arrears> arrList = null;
		employee = Utils.getCurrentEmployee(refDo2.getArmyNo());
		employee.setDeputationFlag(false);
		employee.setFsDueDate(null);
		employee.setRecordStatus(Do2Manager.VALID_CHAR+"");
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2,Do2Manager.getDeputationStatusCodeMap().get(refDo2.getColumnA()).charAt(0));
		if (refDo2.getFromDate().after(DateManager.getCgguardDeputationDate())) {
			if (Do2Manager.getValidStopPayDeputationFromAug2016().contains(refDo2.getColumnA())){
				
				lpcDepRepo.deleteFromLpcDepForDo2(refDo2.getId());
		//		Utils.getEntityManager().createNativeQuery("DELETE FROM lpc_deputation where fk_do2=:do2")
			//			.setParameter("do2", refDo2).executeUpdate();
			
			}
		} else {
			if (Do2Manager.getValidStopPayDeputation().contains(refDo2.getColumnA())){
				lpcDepRepo.deleteFromLpcDepForDo2(refDo2.getId());
				
			//	Utils.getEntityManager().createNativeQuery("DELETE FROM lpc_deputation where fk_do2=:do2")
		//				.setParameter("do2", refDo2).executeUpdate();
			
			}
		}
		
		arrList = arrRepo.findByEmployeeIdAndDo2Id(refDo2.getEmployee().getId(), refDo2.getId());
				
	/*			Utils.getEntityManager().createQuery("Select a from Arrears a where a.employee = :emp" +
				" and a.do2ReverseArrears = :id")
				.setParameter("emp", refDo2.getEmployee())
				.setParameter("id", refDo2).getResultList();*/
		if (arrList.size() > 0){
			for (Arrears arr : arrList ){
				if (arr.getDo2().getAuditCage() == true)
					continue;
				Arrears arrears = new Arrears();
				arrears.setDo2(arr.getDo2());
				arrears.setFromDate(arr.getFromDate());
				arrears.setToDate(arr.getToDate());
				arrears.setAmount((double) Math.round(arr.getAmount() * -1));
				arrears.setPayCode(arr.getPayCode());
				arrears.setQe(Utils.getCurrentQuarter());
				arrears.setDo2ReverseArrears(currentDo2);
				arrears.setRoundOffAmount(false);
				arrears.setRegularPay(false);
				arrears.setEmployee(arr.getEmployee());
				arrears.setArrestPayment(false);
				arrears.setPaid(false);
				arrears.setCreatedAt(new Timestamp(System.currentTimeMillis()));
				arrRepo.save(arrears);
			//	Utils.getEntityManager().persist(arrears);
			//	Utils.getEntityManager().flush();
			}
		}
		return Do2Manager.SUCCESS;
	}

	public void updateEmployee(Do2 do2) {
	 	 employee = Utils.getCurrentEmployee(do2.getArmyNo(), do2.getCheckDigit());
	 	 employee.setDeputationFlag(true);
	 	 employee.setRecordStatus(Do2Manager.getDeputationStatusCodeMap().get(newAuditCage.getDo2().getColumnA()).charAt(0)+"");	 	 
	}
 
	public void updateLpcDeputation(Do2 do2) {
		LpcDeputation lpcDeputation = new LpcDeputation();
		lpcDeputation.setEmployee(Utils.getCurrentEmployee(do2.getArmyNo(),do2.getCheckDigit()));
		lpcDeputation.setDo2(do2);
		lpcDeputation.setLpcType("O");
		lpcDeputation.setRecordStatus("V");
		if (do2.getUsr()!=null) {
			lpcDeputation.setUsr(do2.getUsr());
			Utils.setActionDate(do2.getUsr(), lpcDeputation);
		}
		lpcDeputation.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		lpcDeputation.setArmyNo(do2.getArmyNo());
		lpcDeputation.setCheckDigit(do2.getCheckDigit());
		lpcDepRepo.save(lpcDeputation);
	//	Utils.getEntityManager().persist(lpcDeputation);
	}
		
	public void updateEmployeeServiceDetails(Do2 do2) {
	 	  AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.getDeputationStatusCodeMap().get(do2.getColumnA()).charAt(0)+"");
	}
	
	public void createPosinDo2(Do2 do2) {
		
		Do2 posinDo2 = new Do2();
		posinDo2.setArmyNo(do2.getArmyNo());
		posinDo2.setCheckDigit(do2.getCheckDigit());
		posinDo2.setDo2Group(do2.getDo2Group());
		posinDo2.setDo2Type(do2.getDo2Type());
		posinDo2.setDo2No(do2.getDo2No());
		posinDo2.setDo2ItemNo(do2.getDo2ItemNo());
		posinDo2.setDo2Date(do2.getDo2Date());
		posinDo2.setDescription("POSIN");
		posinDo2.setFromDate(do2.getFromDate());
	//	posinDo2.setColumnA(do2.getDescription());
	//	posinDo2.setColumnB(do2.getColumnA());
	//	posinDo2.setColumnC(do2.getColumnB());
		posinDo2.setStatus(Do2Manager.PENDING);
		posinDo2.setTranscriptionType("T");
		posinDo2.setQe(Utils.getCurrentQuarter());
//		posinDo2.setAcceptedQe(Utils.getCurrentQuarter());
		posinDo2.setAuditCage(true);
		posinDo2.setApproved(true);
		posinDo2.setInputQe(Utils.getCurrentQuarter());
		posinDo2.setEmployee(do2.getEmployee());
		posinDo2.setReason("SYSTEM GENERATED");
		
		if (do2.getColumnA().equals("ASSAM")) {
			posinDo2.setUnitCode("ASMRFL");
			Unit uc=unitRepo.findByUnitCode("ASMRFL");
			posinDo2.setUnit(uc); 
		//	posinDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", "ASMRFL")); 
		}
		
		if (do2.getColumnA().equals("IMTRAT")) {
			Unit uc=unitRepo.findByUnitCode("MIS001");
			posinDo2.setUnit(uc); 
			posinDo2.setUnitCode("MIS001");
			 
		//	posinDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", "MIS001")); 
		}
		
		
		/*if (do2.getColumnA().equals("CRPF")) {
			posinDo2.setUnitCode("CRPF");
			posinDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", "CRPF")); 
		}*/
		
		
		if (do2.getColumnA().equals("CGUARD")) {
			Unit uc=unitRepo.findByUnitCode("CGAURD");
			posinDo2.setUnit(uc); 
			posinDo2.setUnitCode("CGUARD");
		//	posinDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", "CGUARD")); 
		}
		
		if (do2.getColumnA().equals("EMBASSY")) {
			Unit uc=unitRepo.findByUnitCode("EMBASY");
			posinDo2.setUnit(uc); 
			posinDo2.setUnitCode("EMBASY");
		//	posinDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", "EMBASY")); 
		}
		
		if (do2.getColumnA().equals("BDDS")) {
			Unit uc=unitRepo.findByUnitCode("BDDS01");
			posinDo2.setUnit(uc); 
			posinDo2.setUnitCode("BDDS01");
		//	posinDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", "BDDS01")); 
		}
		
		if (do2.getColumnA().equals("STATEPOL")) {
			Unit uc=unitRepo.findByUnitCode("STAPOL");
			posinDo2.setUnit(uc); 
			posinDo2.setUnitCode("STAPOL");
		//	posinDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", "STAPOL")); 
		}
		
		if (do2.getColumnA().equals("HMI")) {
			Unit uc=unitRepo.findByUnitCode("HMI001");
			posinDo2.setUnit(uc); 
			posinDo2.setUnitCode("HMI001");
		//	posinDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", "HMI001")); 
		}
		
		if (do2.getColumnA().equals("MGTINST")) {
			Unit uc=unitRepo.findByUnitCode("MGTIST");
			posinDo2.setUnit(uc); 
			posinDo2.setUnitCode("MGTIST");
	//		posinDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", "MGTIST")); 
		}
		
		if (do2.getColumnA().equals("JIMWS")) {
			Unit uc=unitRepo.findByUnitCode("JIMWS");
			posinDo2.setUnit(uc); 
			posinDo2.setUnitCode("JIMWS");
		//	posinDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", "JIMWS")); 
		}
		
		if (do2.getColumnA().equals("IATT")) {
			Unit uc=unitRepo.findByUnitCode("IATT");
			posinDo2.setUnit(uc); 
			posinDo2.setUnitCode("IATT");
		//	posinDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", "IATT")); 
		}
		
		if (do2.getColumnA().equals("LBSNA")) {
			Unit uc=unitRepo.findByUnitCode("LBSNA");
			posinDo2.setUnit(uc);
			posinDo2.setUnitCode("LBSNA");
		//	posinDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", "LBSNA")); 
		}
		
		if (do2.getColumnA().equals("MEA")) {
			Unit uc=unitRepo.findByUnitCode("MEA");
			posinDo2.setUnit(uc);
			posinDo2.setUnitCode("MEA");
		//	posinDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", "MEA")); 
		}
		if (do2.getColumnA().equals("NTRO")) {
			Unit uc=unitRepo.findByUnitCode("NTRO");
			posinDo2.setUnit(uc);
			posinDo2.setUnitCode("NTRO");
	//		posinDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", "NTRO")); 
		}
		if (do2.getColumnA().equals("NSCS")) {
			Unit uc=unitRepo.findByUnitCode("NSCS");
			posinDo2.setUnit(uc);
			posinDo2.setUnitCode("NSCS");
		//	posinDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", "NSCS")); 
		}
		
		if (do2.getColumnA().equals("NIMAS")) {
			Unit uc=unitRepo.findByUnitCode("NIMAS");
			posinDo2.setUnit(uc);
			posinDo2.setUnitCode("NIMAS");
		//	posinDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", "NSCS")); 
		}
		
		if (do2.getColumnA().equals("MHA")) {
			Unit uc=unitRepo.findByUnitCode("MHA");
			posinDo2.setUnit(uc);
			posinDo2.setUnitCode("MHA");
		//	posinDo2.setUnit((Unit) Utils.getForeignKey(new Unit(), "unitCode", "NSCS")); 
		}
		do2Repo.save(posinDo2);
	//	Utils.getEntityManager().persist(posinDo2);	
	//	Utils.getEntityManager().flush();
	}
	
	public boolean isPosinDo2Generated(Do2 do2) {
		
		Employee emp=Utils.getCurrentEmployee(do2.getArmyNo());
		List<Do2> posinDo2List = do2Repo.findByDescFromDateEqualValidAndPending(emp.getId(),"V","P","POSIN",do2.getFromDate());
				
	/*			Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.armyNo=:ano " 
				+ " AND d.description='POSIN' AND d.fromDate=:fromDate AND (d.status='V' OR d.status='P')")
				.setParameter("ano", do2.getArmyNo()).setParameter("fromDate", do2.getFromDate()).getResultList();*/
		
		if (posinDo2List.size()>0) {
			return true;
		}
				
		return false;
	}
	
	
	public void updateBdtInfo(Do2 do2){
		updateEmployee(do2);
		updateEmployeeServiceDetails(do2);
	}
	
	public void stopAllAllowances(AuditCage newAuditCage) {
		 
		List<String> allceList = Arrays.asList("CILQ", "HRA");
		List<AuditCageAllowance> newAuditCageAllowanceList = acaRepo.findAllByAuditCageId(newAuditCage.getId());
				
		/*		em
				.createQuery(
						"SELECT aca FROM AuditCageAllowance aca WHERE aca.auditCage=:auditCage ")
				.setParameter("auditCage", newAuditCage).getResultList();*/
		

		for (AuditCageAllowance aca : newAuditCageAllowanceList) {
			if (aca.getPayCode().getPayCode().equals(Do2Manager.APTPAY)	|| aca.getPayCode().getPersonal() == false) {
				if (!allceList.contains(aca.getPayCode().getPayCode()))
					acaRepo.delete(aca);
			//	    em.remove(aca);
					newAuditCage.getAuditCageAllowances().remove(aca);  // added on 15.02.2021 
			}
		}
	//	Utils.getEntityManager().flush();
	}

	public boolean isTfrDepExists(Do2 do2){
		Employee emp = do2.getEmployee();
		List<Do2> duplicateDo2List = do2Repo.findByDescriptionStatusFromDateEqualToAndDo2IdNotEqualTo(emp.getId(), do2.getDescription(),
																					"V", do2.getFromDate(), do2.getId());
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.id != :id AND " +
				" d.employee = :emp AND d.description = :desc AND d.status = 'V' " +
				" AND d.fromDate = :fromDate ")
				.setParameter("emp", emp).setParameter("id", do2.getId())
				.setParameter("fromDate", do2.getFromDate())
				.setParameter("desc", do2.getDescription()).getResultList();*/
		if (duplicateDo2List != null && duplicateDo2List.size()>0) {
			return true;
		}
		return false;
	}
	
	public boolean noRevDepExists(Do2 do2) {
		
		Employee emp = do2.getEmployee();
		char status = ' ';
		List<Do2> tfrDepDo2List = do2Repo.findByFromDateLessThanEqualAndDo2idNotEqualAndDescOrderByFromDateDesc(emp.getId(),
																								"TFRDEP",do2.getFromDate(),"V",do2.getId());
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE d.id != :id AND " +
				" d.employee = :emp AND d.description ='TFRDEP' AND d.status = 'V' " +
				" AND d.fromDate <= :fromDate order by d.fromDate desc ")
				.setParameter("emp", emp).setParameter("id", do2.getId())
				.setParameter("fromDate", do2.getFromDate())
				.getResultList();*/
		
		for (Do2 do2List : tfrDepDo2List) {
			System.out.println ( " tfrDep Do2 List " +do2List.getFromDate());
		}
	
		if (tfrDepDo2List != null && tfrDepDo2List.size() > 0 ) {
			if (tfrDepDo2List.get(0).getColumnA().equals("ASSAM")) {
				status = Do2Manager.DEPU_ASSAM_RIFLES;
			}
			if (tfrDepDo2List.get(0).getColumnA().equals("BDDS")) {
				status = Do2Manager.DEPU_BDDS;
			}
			if (tfrDepDo2List.get(0).getColumnA().equals("CGUARD")) {
				status = Do2Manager.DEPU_COAST_GUARD;
			}
			if (tfrDepDo2List.get(0).getColumnA().equals("CRPF")) {
				status = Do2Manager.DEPU_CRPF;
			}
			if (tfrDepDo2List.get(0).getColumnA().equals("EMBASSY")) {
				status = Do2Manager.DEPU_EMBASSY;
			}
			if (tfrDepDo2List.get(0).getColumnA().equals("IMTRAT")) {
				status = Do2Manager.DEPU_IMTRAT_BHUTAN;
			}
			if (tfrDepDo2List.get(0).getColumnA().equals("UNMISN")) {
				status = Do2Manager.DEPU_UNMISSION;
			}
			
			if (tfrDepDo2List.get(0).getColumnA().equals("STATEPOL")) {
				status = Do2Manager.DEPU_STATEPOL;
			}
			
			if (tfrDepDo2List.get(0).getColumnA().equals("HMI")) {
				status = Do2Manager.DEPU_HMI;
			}
			
			if (tfrDepDo2List.get(0).getColumnA().equals("MGTINST")) {
				status = Do2Manager.DEPU_MGTINST;
			}
			
			if (tfrDepDo2List.get(0).getColumnA().equals("JIMWS")) {
				status = Do2Manager.DEPU_JIMWS;
			}
			
			if (tfrDepDo2List.get(0).getColumnA().equals("IATT")) {
				status = Do2Manager.DEPU_IATT;
			}
			
			if (tfrDepDo2List.get(0).getColumnA().equals("LBSNA")) {
				status = Do2Manager.DEPU_LBSNA;
			}
			
			if (tfrDepDo2List.get(0).getColumnA().equals("MEA")) {
				status = Do2Manager.DEPU_MEA;
			}
			
			if (tfrDepDo2List.get(0).getColumnA().equals("NTRO")) {
				status = Do2Manager.DEPU_NTRO;
			}
			
			if (tfrDepDo2List.get(0).getColumnA().equals("NIMAS")) {
				status = Do2Manager.DEPU_NIMAS;
			}
			
			if (tfrDepDo2List.get(0).getColumnA().equals("MHA")) {
				status = Do2Manager.DEPU_MHA;
			}
			
			List<EmployeeServiceDetail> esdList = Utils.getEmployeeServiceDetail(tfrDepDo2List.get(0), status+"");
			if (esdList != null  && esdList.size() > 0) {
				if (esdList.get(0).getToDate()==null ) {
					return true;
				}
			}
		}
		
		
		return false;
	}
}