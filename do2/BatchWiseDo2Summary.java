package itsdc.dolphin.do2;

 

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.repositories.Do2Repository;

 
@RestController
public class BatchWiseDo2Summary implements Serializable {
	
	private String batchNumber, quarterEnding;
	
	public String getBatchNumber() {
		return batchNumber;
	}

	public void setBatchNumber(String batchNumber) {
		this.batchNumber = batchNumber;
	}

	public String getQuarterEnding() {
		return quarterEnding;
	}

	public void setQuarterEnding(String quarterEnding) {
		this.quarterEnding = quarterEnding;
	}
	
	List<BatchObject> batchObjectList = new ArrayList<BatchObject>();

	public List<BatchObject> getBatchObjectList() {
		return batchObjectList;
	}

	public void setBatchObjectList(List<BatchObject> batchObjectList) {
		this.batchObjectList = batchObjectList;
	}

	Do2Repository do2Repo;
	
	public BatchWiseDo2Summary() {
	}
	
	public List<BatchObject> filteredBatchList() {
		int totalDo2s = 0;
		List<Integer> batchNoList = null;
		List<String> qeList = null;
		if(batchObjectList.size() > 0)
			batchObjectList.clear();
		List<Object[]> do2List = null;
		BatchObject bo = null;
		if((getBatchNumber() == null || getBatchNumber().trim().length() == 0) && 
				(getQuarterEnding() == null || getQuarterEnding().trim().length() == 0)) {
			batchNoList =do2Repo.findDistinctBatchNo(); 
					 
			qeList =do2Repo.findDistinctQe();
			 
			for(String qe : qeList) {
				for(Integer bno : batchNoList) {
					bo = new BatchObject();
					bo.setQe(qe);
					if(bno == null) {
						bo.setBatchNo(null);
					} else {
						bo.setBatchNo(String.valueOf(bno));
					}
					
					do2List = do2Repo.findIdCountAndStatus(bno, qe);
					 
					totalDo2s = populateBatchObject(bo, do2List);
				}
				bo.setTotalDo2s(totalDo2s);
				batchObjectList.add(bo);
			}
		} else if((getBatchNumber() != null || getBatchNumber().trim().length() > 0) && 
				(getQuarterEnding() == null || getQuarterEnding().trim().length() == 0)) {
			qeList = do2Repo.findDistinctQeByBatchNo(Integer.parseInt(getBatchNumber())); 
			 
			for(String qe : qeList) {
				 
				do2List =do2Repo.findIdCountAndStatus(Integer.parseInt(getBatchNumber()), qe) ;
				 
				if(do2List != null && do2List.size() > 0) {
					bo = new BatchObject();
					bo.setQe(qe);
					bo.setBatchNo(getBatchNumber());
					for (Object[] obj : do2List) {
						totalDo2s = populateBatchObject(bo, do2List);
					}
					bo.setTotalDo2s(totalDo2s);
					batchObjectList.add(bo);
				}
			}
		} else if((getBatchNumber() == null || getBatchNumber().trim().length() == 0) && 
				(getQuarterEnding() != null || getQuarterEnding().trim().length() > 0)) {
			batchNoList = do2Repo.findDistinctBatchNoByQe(getQuarterEnding());
				 
			for(Integer bno : batchNoList) {
				bo = new BatchObject();
				do2List =do2Repo.findIdCountAndStatus(bno, getQuarterEnding());
				 
				if(do2List != null && do2List.size() > 0) {
					if(bno == null) {
						bo.setBatchNo(null);
					} else {
						bo.setBatchNo(String.valueOf(bno));
					}
					bo.setQe(getQuarterEnding());
					for (Object[] obj : do2List) {
						totalDo2s = populateBatchObject(bo, do2List);
					}
					bo.setTotalDo2s(totalDo2s);
					batchObjectList.add(bo);
				}
			}
		} else if((getBatchNumber() != null || getBatchNumber().trim().length() > 0) && 
				(getQuarterEnding() != null || getQuarterEnding().trim().length() > 0)) {
				do2List = do2Repo.findIdCountAndStatus(Integer.parseInt(getBatchNumber()), getQuarterEnding());
				 
				if(do2List != null && do2List.size() > 0) {
					bo = new BatchObject();
					bo.setBatchNo(getBatchNumber());
					bo.setQe(getQuarterEnding());
					for (Object[] obj : do2List) {
						totalDo2s = populateBatchObject(bo, do2List);
					}
					bo.setTotalDo2s(totalDo2s);
					batchObjectList.add(bo);
				}
		}
		return batchObjectList;
	}
	
	public int populateBatchObject(BatchObject bo, List<Object[]> objList) {
		int total = 0;
		for(Object[] obj : objList) {
			total += Integer.parseInt(obj[0].toString());
			if (obj[1] == null || obj[1].equals(Do2Manager.PENDING)) {
				bo.setPendingDo2s(Integer.parseInt(obj[0].toString()));
			} else if (obj[1].equals(Do2Manager.VALID)) {
				bo.setValidDo2s(Integer.parseInt(obj[0].toString()));
			} else if (obj[1].equals(Do2Manager.INVALID)) {
				bo.setInvalidDo2s(Integer.parseInt(obj[0].toString()));
			} else if (obj[1].equals(Do2Manager.DO2_CANCEL_STATUS)) {
				bo.setCancelDo2s(Integer.parseInt(obj[0].toString()));
			} else if (obj[1].equals(Do2Manager.BACK_DATED)) {
				bo.setBackDatedDo2s(Integer.parseInt(obj[0].toString()));				
			} else if (obj[1].equals(Do2Manager.NONEFFECTIVE)) {
				bo.setNonEffectiveDo2s(Integer.parseInt(obj[0].toString()));				
			} else if (obj[1].equals(Do2Manager.MASTER_MISSING)) {
				bo.setMasterMissingDo2s(Integer.parseInt(obj[0].toString())); 
			} else {
				bo.setMrDo2s(Integer.parseInt(obj[0].toString()));
			}
		}
		return total;
	}
	
	public void resetResultList() {
		setBatchNumber(null);
		setQuarterEnding(null);
		if(batchObjectList != null && batchObjectList.size() > 0)
			batchObjectList.clear();
	}
}
