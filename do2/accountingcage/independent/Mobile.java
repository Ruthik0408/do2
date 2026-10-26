package itsdc.dolphin.do2.accountingcage.independent;

import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.AccountingCageBase;

import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

/**
 * 
 * @author IT & SDC,Secunderabad
 * 
 */
@RestController
public class Mobile extends AccountingCageBase implements java.io.Serializable {

	String message;
	private StringBuffer buffer = new StringBuffer();
	private static final String INVALID_COLUMNA = "VL:Col.5A should contain Valid Phone Number.\n";
	private static final String BLANK_COLUMNB = "VL:Col.5B Is Blank.\n";
	private static final String INVALID_COLUMNB = "VL:Col.5B should contain INDIA or NEPAL. \n";

	@Override
	public boolean isValid(Do2 do2) {
		boolean validity = true;
		if (!isValidA(do2)) {
			validity = false;
			buffer.append(INVALID_COLUMNA);
		}
		
		try {
			if (do2.getDo2Date().after(DateManager.getDate("2026-05-31"))) {
				if (do2.getColumnB()==null) {
					validity = false;
					buffer.append(BLANK_COLUMNB);
				} else if (do2.getColumnB().trim().length()!=5) {
						validity = false;
						buffer.append(INVALID_COLUMNB);
				} else {
					if (!(do2.getColumnB().trim().toUpperCase().equals("INDIA")
							|| do2.getColumnB().trim().toUpperCase().equals("NEPAL"))) {
						validity = false;
						buffer.append(INVALID_COLUMNB);
					}
				}
			}
		} catch (Exception e) {
			Utils.handleException(e);
		}
		 
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
		} else {
			do2.setStatus(Do2Manager.VALID);
			if (do2.getAcceptedQe() == null) {
				do2.setAcceptedQe(Utils.getCurrentQuarter());
			} else {
				if (!do2.getAcceptedQe().equals(Utils.getCurrentQuarter())) {
					do2.setReviewQe(Utils.getCurrentQuarter());
				}
			}
		}

		do2Repo.save(do2);
		message = buffer.toString();
		do2.setReason(message);

		return validity;
	}

	@Override
	public String process(Do2 do2) {
		Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
		emp.setMobileNo(do2.getColumnA().trim());
		try {
			emp.setMobileNo(Utils.getEncrypt(do2.getColumnA().trim()));
		} catch (Exception e) {
			Utils.handleException(e);
		}
		empRepo.save(emp); 
		return Do2Manager.SUCCESS;
	}
	 
	@Override
	public String processCancel(Do2 currentDo2, Do2 refDo2) {
		Employee emp = Utils.getCurrentEmployee(currentDo2.getArmyNo());
		emp.setMobileNo(null);		 
		empRepo.save(emp);
		return Do2Manager.SUCCESS;
	}
	
//	public boolean isValidA(Do2 do2) {
//		if (do2.getColumnA() == null) {
//			buffer.append("Column 5A is blank. \n");
//			return false;
//		}
//		boolean validity = true;
//		String mobileNumber = do2.getColumnA().trim();
//
//		Employee emp = Utils.getCurrentEmployee(do2.getArmyNo());
//		// List<Employee>employeeList =
//		// empRepo.findByCurrentRecordAndMobileNoAndQe(emp.getCurrentRecord(),emp.getMobileNo(),Utils.getCurrentQuarter());
//		List<Employee> employeeList = null;
//		employeeList = empRepo.findByCurrentRecordAndMobileNoAndQe(emp.getCurrentRecord(), mobileNumber,
//				Utils.getCurrentQuarter());
//
//		/*
//		 * Utils.getEntityManager().createQuery("select emp from Employee emp " +
//		 * " where emp.currentRecord=true and emp.mobileNo=:mobileNumber and emp.qe=:qe"
//		 * ) .setParameter("mobileNumber", mobileNumber) .setParameter("qe",
//		 * Utils.getCurrentQuarter()).getResultList();
//		 */
//
//		if (employeeList != null && employeeList.size() > 0) {
//			if (!do2.getArmyNo().equals(employeeList.get(0).getArmyNo())) {
//				buffer.append("Already Phone Number " + mobileNumber + " exists for the Army Number "
//						+ employeeList.get(0).getArmyNo());
//				validity = false;
//			}
//		}
//		if (mobileNumber.length() != 10) {
//			buffer.append("MOBILE NUMBER LENGTH SHOULD BE EQUAL TO 10.\n");
//			validity = false;
//		}
//
//		if (mobileNumber.length() == 10 && !isNumeric(mobileNumber.substring(0, 10))) {
//			buffer.append(" Mobile Number should be in numberics \n");
//			validity = false;
//		}
//
//		if (validity == false)
//			return false;
//		else
//			return true;
//	}
	
	//Changed on 17.07.2026
	public boolean isValidA(Do2 do2) {
	    if (do2.getColumnA() == null) {
	        buffer.append("Column 5A is blank. \n");
	        return false;
	    }
	    boolean validity = true;
	    String mobileNumber = do2.getColumnA().trim();

	    List<Employee> empList = empRepo.findByQeAndCurrentRecord(do2.getEmployee().getId(), Utils.getCurrentQuarter());

	    if (empList == null || empList.isEmpty()) {
	        buffer.append("No employee record found for Army Number " + do2.getArmyNo() + ". \n");
	        return false;
	    }

	    Employee emp = empList.get(0);

	    if (emp.getMobileNo() != null) {
	        String decryptedMobile = null;
	        try {
	            decryptedMobile = Utils.getDecrypt(emp.getMobileNo());
	        } catch (Exception ex) {
	            ex.printStackTrace();
	            buffer.append("Error decrypting stored mobile number. \n");
	            validity = false;
	        }

	        }

	    if (mobileNumber.length() != 10) {
	        buffer.append("MOBILE NUMBER LENGTH SHOULD BE EQUAL TO 10.\n");
	        validity = false;
	    }
	    if (mobileNumber.length() == 10 && !isNumeric(mobileNumber.substring(0, 10))) {
	        buffer.append(" Mobile Number should be in numberics \n");
	        validity = false;
	    }

	    return validity;
	}
	public boolean isAlphaUpper(final String s) {
		final char[] chars = s.toCharArray();
		for (int x = 0; x < chars.length; x++) {
			final char c = chars[x];
			if ((c >= 'A') && (c <= 'Z'))
				continue; // uppercase
			return false;
		}
		return true;
	}

	public boolean isNumeric(final String s) {
		final char[] chars = s.toCharArray();
		for (int x = 0; x < chars.length; x++) {
			final char c = chars[x];
			if ((c >= '0') && (c <= '9'))
				continue; // numeric
			return false;
		}
		return true;
	}

	public boolean isAlphaFirstLetter(final String s) {
		final char[] chars = s.toCharArray();
		for (int x = 0; x < chars.length; x++) {
			final char c = chars[x];
			if ((c >= 'A') && (c <= 'F'))
				continue; // uppercase
			return false;
		}
		return true;
	}
}
