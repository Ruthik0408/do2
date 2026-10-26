package itsdc.dolphin.do2;

 

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.models.Misc;
import itsdc.dolphin.repositories.MiscRepository;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 

@RestController
 public class BatchControl {

	 
	MiscRepository miscRepo;
	
	 
	
	
	public MiscRepository getMiscRepo() {
		return miscRepo;
	}
	 
	public void setMiscRepo(MiscRepository miscRepo) {
		this.miscRepo = miscRepo;
	}

	private String me, errorMessage;
	List<BatchControlObject> batchCtrlList = new ArrayList<BatchControlObject>();
	List<Misc> miscCurrentQuarterList;
	List<String> monthEndingList;
	private Date lastDate;
	SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
	public String getMe() {
		return me;
	}

	public void setMe(String me) {
		this.me = me;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}
	public Date getLastDate() {
		return lastDate;
	}

	public void setLastDate(Date lastDate) {
		this.lastDate = lastDate;
	}

	public BatchControl() {
		
	}
 
	 @Autowired
	public BatchControl(MiscRepository miscRepo) {
		 this.miscRepo=miscRepo;
		monthEndingList = new ArrayList<String>();
		miscCurrentQuarterList =miscRepo.findByKey1OrderByDate1(Utils.CURRENT_QUARTER);
			 
		if(monthEndingList != null && monthEndingList.size() > 0)
			monthEndingList.clear();
		for(Misc m : miscCurrentQuarterList) {
			if(m.getDate1() != null)
				monthEndingList.add(getMonthEndingFromDate(m.getDate1()));
		}
		getBatchControlList();
	}

	public List<BatchControlObject> getBatchControlList() {
		if(batchCtrlList != null && batchCtrlList.size() > 0)
			batchCtrlList.clear();
		if(getMe() == null) {
			for(Misc m : miscCurrentQuarterList) {
				BatchControlObject bcdo = new BatchControlObject();
				bcdo.setId((long)m.getId());
				bcdo.setToDate(m.getDate2());
				bcdo.setMe(getMonthEndingFromDate(m.getDate1()));
				batchCtrlList.add(bcdo);
			}
		} else {
			for(Misc m : miscCurrentQuarterList) {
				if(getMonthEndingFromDate(m.getDate1()).equals(getMe())) {
					BatchControlObject bcdo = new BatchControlObject();
					bcdo.setId((long)m.getId());
					bcdo.setToDate(m.getDate2());
					bcdo.setMe(getMonthEndingFromDate(m.getDate1()));
					batchCtrlList.add(bcdo);
				}
			}
		}
		if(batchCtrlList != null && batchCtrlList.size() > 0) {
			for(int i = 0 ; i < batchCtrlList.size() ; i++) {
				if(i > 0) {
					if(batchCtrlList.get(i-1).getToDate() != null) {
						batchCtrlList.get(i).setFromDate(DateManager.getNextDate(batchCtrlList.get(i-1).getToDate()));
					}
				}
			}
		}
		return batchCtrlList;
	}
	
	static public String getMonthEndingFromDate(Date dt) {
		String monthEnding = "";
		Calendar cal = new GregorianCalendar();
		cal.setTimeInMillis(dt.getTime());
		int month = cal.get(Calendar.MONTH) + 1;
		String monthStr = month < 10 ? "0" + month : month + "";
		monthEnding = monthStr + "/" + cal.get(Calendar.YEAR);
		return monthEnding;
	}
	
	public String updateLastDate() {
		String returnValue = "";
		if(batchCtrlList != null && batchCtrlList.size() > 0) {
			for(BatchControlObject bco : batchCtrlList) {
				if(bco.getMe().equals(getMe())) {
					if(bco.getToDate() != null) {
						/*FacesMessages.instance().add("Last Date already provided as : " + bco.getToDate() + 
								".  Cannot update with a new date.  Contact System Administrator.");*/
						returnValue = "FAIL";
						break;
					} else {
						Misc misc=miscRepo.findById(bco.getId().intValue()).get();
						misc.setDate2(getLastDate());
						miscRepo.save(misc); 
						
						//FacesMessages.instance().add("Last Date for receipt of Batch for Month Ending : " + 
							//getMe() + " recorded as " + sdf.format(getLastDate()));
						returnValue="PASS";
						setMe(null);
						break;
					}
				}
			}
		}
		return returnValue;
	}

}
