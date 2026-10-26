package itsdc.dolphin.do2.auditcage;

 
import javax.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.GroupCategory;
import itsdc.dolphin.models.StatusCode;
import itsdc.dolphin.models.TransferInOut;
import itsdc.dolphin.models.Usr;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 
 * @author N. RAMACHANDRAN & K.RAMADEVI 
 * 
 */

@RestController
public class Tfrin extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {

//	@In(required = false)
	Usr usr;
	Employee employee;
	private StringBuffer buffer = new StringBuffer();
	List<EmployeeServiceDetail> esdList;
	static final public String NO_TFROUT_EXISTS = "AU:TFROUT do2 not available. \n";
	public static final String INVALID_CLASS_CATEGORY = "AU:Incorrect Group/class/category in Col.7. \n ";

	public Tfrin() {
		HttpSession sess = Utils.currentSession();
		if(sess != null) {
			usr = (Usr)sess.getAttribute("loggedInUsr");
		}
	}
	
	@Override
	public boolean isValid(Do2 do2) {

		boolean validity = true;
	
		if (!isTfroutDo2available(do2)) {			
			validity = false;
			buffer.append(NO_TFROUT_EXISTS);
			do2.setStatus(Do2Manager.INVALID);
		} 
		
		if (newDo2.getId() == do2.getId() ) {
			if (do2.getColumnA() != null) {			
				if (!isValidGroupClassCategory(do2)) {
					validity = false;
					buffer.append(INVALID_CLASS_CATEGORY);
					do2.setStatus(Do2Manager.INVALID);
				} 
			}
		}
		if (validity == true) {
			do2.setStatus(Do2Manager.VALID);	
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);

		return validity;
	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		
		if(newAuditCage.getEmployee().getRegularTaFlag().equals("T")) {
			newAuditCage.setAbsentFlag(true);
		} else {
			  newAuditCage.setAbsentFlag(false);
		}
      
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_TFRIN+"");
		AuditCageHelper.removeAllowances(newAuditCage);
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		employee.setFsDueDate(null);
		employee.setFsStatus(null);
		if (newDo2.getId() == do2.getId()) {
			insertTransferInOut(newAuditCage.getDo2());
		}
		
		return Do2Manager.SUCCESS;
	}
 
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		
		AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_TFRIN);
		
		return Do2Manager.SUCCESS;
	}

	public boolean isTfroutDo2available(Do2 do2) {
		
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		
		List<Do2> do2List = do2Repo.findByFromDateLessThanEqualTo(employee.getId(), "TFROUT", Do2Manager.VALID, do2.getFromDate());
				
		/*		Utils.getEntityManager().createQuery("SELECT d FROM Do2 d WHERE " +
				" d.description='TFROUT' and d.employee=:emp and d.status =:status and d.fromDate<=:fromDate")
				.setParameter("emp", employee)
				.setParameter("fromDate",do2.getFromDate())
				.setParameter("status", Do2Manager.VALID)
				.getResultList();*/
			
		return do2List.size()>0;
	}
	
	private void insertTransferInOut(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		TransferInOut tfrIn = new TransferInOut();
		tfrIn.setArmyNo(do2.getArmyNo());
		tfrIn.setCheckDigit(do2.getCheckDigit());
		tfrIn.setEmployee(employee);
		tfrIn.setDo2(do2);
		if (usr!=null)
			Utils.setActionDate(usr, tfrIn);
		tfrIn.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		if (usr!=null)
			tfrIn.setUsr(usr);
		tfrIn.setApproved(false);
		tfrIn.setTransferType("I");
		tfrIn.setPao(Utils.getPao());
		tfrIn.setQe(Utils.getCurrentQuarter());
		tioRepo.save(tfrIn);
	//	Utils.getEntityManager().persist(tfrIn);
	}
	
	public boolean isValidGroupClassCategory(Do2 do2) {
		Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
		List<GroupCategory> grpCatList = gcRepo.findBySoldierGroupAndSoldierCategory(do2.getColumnA().charAt(3)+"",
																						do2.getColumnC().trim());
				
			/*	Utils.getEntityManager().createQuery( "SELECT gc FROM GroupCategory gc WHERE gc.soldierGroup=:sgrp AND gc.soldierCategory=:scat")
				.setParameter("sgrp", do2.getColumnA().charAt(3))
				.setParameter("scat", do2.getColumnC().trim())
				.getResultList();*/
 		if (grpCatList.size() == 0) {
		 	return false;
		}		
 		
 		// validation of group category mentioned in TFRIN do2 with existing categroy in employee removed 
 		// as PAO PARA intimated through mail that the persons who were transferred to PAO PARA will be
 		// in INFSOL category which is not their original category when recruited. DUe to this PAO has to change the 
 		// group category which seems not correct. However, is employee group category has to be updated on 
 		// TFRIN do2 also to be get clarification from PAO PARA
 		
		
		/*
		 * if (!emp.getGroupCategory().equals(grpCatList.get(0))){ List<Do2> do2List =
		 * Utils.getEntityManager().
		 * createQuery("select d from Do2 d where d.employee=:emp and (d.status='V' or d.status='P') and "
		 * +
		 * " d.fromDate=:frdt and (d.description='MSTJCO' or d.description='MSTHAV') ")
		 * .setParameter("emp", emp).setParameter("frdt",
		 * do2.getFromDate()).getResultList(); if (do2List!=null && do2List.size()>0 &&
		 * do2List.get(0).getColumnC().trim().equals(emp.getGroupCategory().
		 * getSoldierCategory())) { return true; } else { return false; } } else {
		 * return true; }
		 */
 		return true;
	}
	
}
