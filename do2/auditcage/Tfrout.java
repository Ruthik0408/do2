package itsdc.dolphin.do2.auditcage;

 
import javax.servlet.http.HttpSession;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.EmployeeServiceDetail;
import itsdc.dolphin.models.StatusCode;
import itsdc.dolphin.models.TransferInOut;
import itsdc.dolphin.models.Usr;
import itsdc.dolphin.do2.Do2AccountingCageProcessor;
import itsdc.dolphin.do2.Do2Manager;
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
public class Tfrout extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
//	@In(required = false)
	Usr usr;
	Employee employee;
	boolean validity = true;
	private StringBuffer buffer = new StringBuffer();
	static final public String TFR_OUT = "Transfer Out date already updated in Employee table. Check the correctness.\n";
	
	public Tfrout() {
		HttpSession sess = Utils.currentSession();
		if(sess != null) {
			usr = (Usr)sess.getAttribute("loggedInUsr");
		}
	}
	
	@Override
	public boolean isValid(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
	//	if (newDo2.getId() == do2.getId() && employee.getFsDueDate() != null) {
		if (do2.getAcceptedQe() == null && employee.getFsDueDate() != null) {
			validity = false;
			do2.setStatus(Do2Manager.INVALID);
			buffer.append(TFR_OUT);

		} else {
			do2.setStatus(Do2Manager.VALID);
		}
		do2.setReason(buffer.toString());
		do2Repo.save(do2);
	return validity;

	}

	@Override
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {
		newAuditCage.setAbsentFlag(true);
		newAuditCage.setTransport(0);
		return newAuditCage;
	}

	@Override
	public String process(Do2 do2, AuditCage baseAuditCage) {
		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		employee = Utils.getCurrentEmployee(do2.getArmyNo());
		if (newDo2.getId() == do2.getId()) {
			employee.setFsDueDate(do2.getFromDate());
			StatusCode sc=scRepo.findByStatusCode("0");
			employee.setFsStatus(sc);
		//	employee.setFsStatus((StatusCode) Utils.getForeignKey(new StatusCode(), "statusCode", 'O'));
			if (newDo2.getId() == do2.getId()) {
				insertTransferInOut(do2);
			}
			
		}
		  
	    AuditCageHelper.stopAllAllowances(newAuditCage);
	    AuditCageHelper.updateEmployeeServiceDetail(do2, Do2Manager.ESD_TFROUT+"");
		/*
		 * if (newDo2.getId()==do2.getId()) insertTransferInOut(do2);
		 */
		
	    return Do2Manager.SUCCESS;
	}
	
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
	    employee = Utils.getCurrentEmployee(currentDo2.getArmyNo());
	    employee.setFsDueDate(null);
	    employee.setFsStatus(null);
	    empRepo.save(employee);
	 //   Utils.getEntityManager().persist(employee);
	    AuditCageHelper.reverseUpdateEmployeeServiceDetail(refDo2, Do2Manager.ESD_TFROUT);
	    tioRepo.deleteFromTransferInOutForDo2(refDo2);
	//	Utils.getEntityManager().createNativeQuery("DELETE FROM transfer_in_out where fk_do2=:do2")
	//	.setParameter("do2", refDo2).executeUpdate();
		return Do2Manager.SUCCESS;
	}
	
	private void insertTransferInOut(Do2 do2) {
		employee = Utils.getCurrentEmployee(newAuditCage.getDo2().getArmyNo());
		TransferInOut tfrOut = new TransferInOut();
		tfrOut.setArmyNo(do2.getArmyNo());
		tfrOut.setCheckDigit(do2.getCheckDigit());
		tfrOut.setEmployee(employee);
		tfrOut.setDo2(do2);
		tfrOut.setCreatedAt(new Timestamp(System.currentTimeMillis()));
		tfrOut.setUsr(do2.getUsr());
		if (do2.getUsr()!=null)
			Utils.setActionDate(do2.getUsr(), tfrOut);
		tfrOut.setApproved(false);
		tfrOut.setTransferType("O");
		if (do2.getUsr()!=null)
			tfrOut.setPao(do2.getUsr().getPao());
		tfrOut.setQe(Utils.getCurrentQuarter());
		tioRepo.save(tfrOut);
	//	Utils.getEntityManager().persist(tfrOut);
	}

	
}
