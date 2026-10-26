package itsdc.dolphin.do2.auditcage;
 
import javax.persistence.EntityManager;
import javax.transaction.SystemException;

import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.web.bind.annotation.RestController;

import itsdc.dolphin.do2.Do2Manager;
import itsdc.dolphin.models.AuditCage;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.PayMatrix;
import itsdc.dolphin.models.PayScale;
import itsdc.dolphin.models.Rank;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
 
@RestController
public class Bunch extends AuditCageBase implements Do2AuditCageProcessor, java.io.Serializable {
	 
	private StringBuffer buffer = new StringBuffer();
	private String message;
	private Calendar fromDateCal = new GregorianCalendar();
	boolean validity = true;
	boolean rateValidity = false;
	private List<AuditCage> acList;
	private List<PayScale> payList;
	private List<PayMatrix> payMatrixList;
	private List<Do2> do2List;
	private Integer basicPay;
	private Integer gradePay;
	private Integer newBasic;
	private String armyNo;
	
	Employee employee;
	Rank rank;
	String soldierGroup;
	 
	static public final String INVALID_FROM_DATE = "VL:Date should be 01.01.2016.\n";
	static public final String INVALID_RATE= "AU:Pay to be stepped is not in 5 CPC Pay scale / invalid stage as per 6CPC / PBOR already drawing more pay than proposed STEPUP pay. \n";
	static public final String INCORRECT_PAY_IN_COLA = "AU:Invalid / Incorrect Value In Column 7A.\n";	
	static public final String BLANK_COLUMNA = "VL:Column 7A Should Not Be Blank.\n";
	static public final String INVALID_COLUMNB = "VL:Column-7B Should Not Be blank.\n";
	static public final String INVALID_COLUMNC = "VL:Column-7C to be left blank.\n";
	static public final String DUPLICATE = "DU:Duplicate DOII.\n";
	static public final String INELIGIBLE_FOR_BUNCHING = "AU:Became Ineligible For Bunching.\n";
	static public final String INCORRECT_PAY_LEVEL_IN_COLB = "AU:Invalid / Incorrect Value In Column 7B.\n";	
	static public final String FIX7PC_DO2_NOT_FOUND = "AU:FIX7PC DOII Not Found / Migrated To 7CPC Later Than 01.01.2016.";
	
	@Override
	public boolean isValid(Do2 do2) {
		employee = Utils.getCurrentEmployee(do2.getArmyNo());		
	
		if (!do2.getFromDate().equals(DateManager.getSeventhCpcDate())) {
			buffer.append(INVALID_FROM_DATE);
			validity = false;
		} 
		
		if (do2.getColumnA()==null || (do2.getColumnA()!=null && do2.getColumnA().length()==0)) {
			buffer.append(BLANK_COLUMNA);
			validity = false;
		}
		
		if (do2.getColumnB() != null && do2.getColumnB().length() == 0){
			buffer.append(INVALID_COLUMNB);
			validity = false;
		}
		
		if (do2.getColumnC() != null && do2.getColumnC().length() != 0){
			buffer.append(INVALID_COLUMNC);
			validity = false;
		}
		
		if (do2.getColumnA()!=null && do2.getColumnA().length()>0 && !isValid7cpcPayInColumnA(do2, employee)) {
			buffer.append(INELIGIBLE_FOR_BUNCHING);
			validity=false;
		}
		List<AuditCage> acList=null;		
		List<Do2> promotMacpsDo2List=null;
		
		promotMacpsDo2List=do2Repo.getPromotDo2On01012016(employee.getId());
		
		if (promotMacpsDo2List!=null && promotMacpsDo2List.size()>0) {
			acList = acRepo.getFix7pcAuditcageForBunchWithPromotSameDate(employee.getId());
			if (acList!=null && acList.size()>0) {
				System.out.println ( " ac list xxx 1111111111111  " +acList.get(0).getBasicPay()   +" do2 columnA " +do2.getColumnA()  + " auditcage id " +acList.get(0).getId());
	 			if (Integer.parseInt(do2.getColumnA()) < acList.get(0).getBasicPay()) { 
					buffer.append(INCORRECT_PAY_IN_COLA);
					validity=false;
				}
			} else {
				buffer.append(FIX7PC_DO2_NOT_FOUND);
				validity=false;
			}		
		} else {
			if (do2.getColumnA()!=null && do2.getColumnA().length()>0) {
				System.out.println ( " do2.getEmployee " +do2.getEmployee().getId()   +" do2 from date " +do2.getFromDate()  +" do2.get id " +do2.getId())  ;
				if (do2.getId()!=null) {
					 acList =acRepo.findByFromDateLessThanEqualDo2IdNotEqualPlNotEqualGpZero(do2.getEmployee().getId(),do2.getFromDate(),do2.getId());
				} else {
					 acList =acRepo.findByFromDateLessThanEqualPlNotEqualGpZero(do2.getEmployee().getId(),do2.getFromDate());
				}
			
					System.out.println ( " ac List colA " +acList.size()  + " do2 columnA " +do2.getColumnA());
				if (acList.size()>0) {
					System.out.println ( " ac list xxx " +acList.get(0).getBasicPay()   +" do2 columnA " +do2.getColumnA()  + " auditcage id " +acList.get(0).getId());
				//	if (Integer.parseInt(do2.getColumnA()) <= acList.get(0).getBasicPay()) {  // commented on 01.11.2023
	 				if (Integer.parseInt(do2.getColumnA()) < acList.get(0).getBasicPay()) { 
						buffer.append(INCORRECT_PAY_IN_COLA);
						validity=false;
					}
				} else {
					buffer.append(INCORRECT_PAY_IN_COLA);
					validity=false;
				}			
			}
		 
			if (do2.getColumnB()!=null && do2.getColumnB().length()>0) {
			
				acList=null;
				if (do2.getId()!=null) {
					 acList =acRepo.findByFromDateLessThanEqualDo2IdNotEqualPlNotEqualGpZero(do2.getEmployee().getId(),do2.getFromDate(),do2.getId());
				} else {
					 acList =acRepo.findByFromDateLessThanEqualPlNotEqualGpZero(do2.getEmployee().getId(),do2.getFromDate());
				}
			
				System.out.println ( " ac List colB " +acList.size()  + " do2 columnB " +do2.getColumnB());
				if (acList.size()>0) {
					System.out.println ( " ac List " +acList.size()  + " do2 columnB " +do2.getColumnB() + " ac list get0 " +acList.get(0).getPayLevel()  + " ac list id " +acList.get(0).getId());
					if (!do2.getColumnB().equals(acList.get(0).getPayLevel())) {
						buffer.append(INCORRECT_PAY_LEVEL_IN_COLB);
						validity=false;
					}
				} else {
					buffer.append(INCORRECT_PAY_LEVEL_IN_COLB);
					validity=false;
				}			
			}
		}
		
		if (isDuplicate(do2, employee)) {		 
			buffer.append(DUPLICATE);
			validity=false;
		}
		
		if (!validity) {
			do2.setStatus(Do2Manager.INVALID);
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
	public AuditCage getNewAuditCage(AuditCage newAuditCage, AuditCage baseAuditCage) {		
		 newAuditCage.setBasicPay(Integer.parseInt(newAuditCage.getDo2().getColumnA()));	
	 	return newAuditCage;
	}
	
 	@Override
	public String process(Do2 bunchDo2, AuditCage baseAuditCage) {
 		AuditCageHelper.copyCurrentAllowances(newAuditCage, baseAuditCage);
		return Do2Manager.SUCCESS;
	}

	@Override
	public String processCancel(Do2 bunchDo2, Do2 refDo2) {		
		return Do2Manager.SUCCESS;
	}
	
	public boolean isDuplicate(Do2 do2, Employee emp) {		 
		List<Do2> do2List =do2Repo.findByDescriptionStatusNotEqualAndNotThisIdForBunch(do2.getEmployee().getId(),do2.getId());
			
		return (do2List.size()>1);
	}
	
	public boolean isValid7cpcPayInColumnA(Do2 do2, Employee emp) {
		  
			
		List<AuditCage> acList=null;
		try {
			acList = acRepo.findByFromDateEqualEffectiveOrderByBasicPayDesc(emp.getId(), DateManager.getDate("2016-01-01"));
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		 
	/*	if (acList==null || (acList!=null && acList.size()==0)) {
			acList=null;
			acList = Utils.getEntityManager().createQuery("select a from AuditCage a where a.effective=true and a.cancelled=false and a.recordStatus='V' and "
					+ " a.employee=:emp and a.fromDate='2016-01-01' order by id desc ").setParameter("emp", emp).getResultList();
			if (acList==null || (acList!=null && acList.size()==0)) {
				return false;	
			}		
		}
	*/	// commented on 05.11.2021.
		
		if (acList==null || (acList!=null && acList.size()==0)) {  // another condition as below added on 09.02.2022
			try {
				acList =acRepo.findByFromDateEqualEffectiveOrderByBasicPayDesc(emp.getId(),DateManager.getDate("2016-01-01"));
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
										
			if (acList==null || (acList!=null && acList.size()==0)) {
				return false;	
			}			
		}
		
		if (acList!=null && acList.size()>0) {
			if (acList.get(0).getXGroupPay()>0) {
				
				jdbcTemplate.execute("drop table if exists t; create temp table t as " + 
						" select distinct(basic_pay),  old_grade_pay, min(old_basic_pay + old_grade_pay) as minb, max(old_basic_pay + old_grade_pay) as maxb from audit_cage join do2 on do2.id=audit_cage.fk_do2 join bunching_data on " + 
						" bunching_data.grade_pay=audit_cage.old_grade_pay where audit_cage.old_grade_pay in (2000, 2400, 2800, 4200, 4600, 4800, 5400) and effective=true and not cancelled " + 
						" and record_status='V' and audit_cage.from_date='2016-01-01' and bunching_data.sevencpc_basic_pay=audit_cage.basic_pay " + 
						" and audit_cage.grade_pay=0 and audit_cage.x_group_pay>0 and do2.description='FIX7PC' and do2.status='V' and do2.from_date='2016-01-01'  " + 
						" group by basic_pay, old_grade_pay order by  old_grade_pay, basic_pay");
				
				 
				 	
				jdbcTemplate.execute(" drop table if exists t1; create temp table t1 as  \n" + 
						"				 select employee.army_no, rank.rank_name, unit.unit_code, unit.task, audit_cage.fk_employee, audit_cage.basic_pay, audit_cage.old_basic_pay, audit_cage.old_grade_pay,  " + 
						"				 t.minb,  t.maxb, (t.maxb-t.minb) as td, ((t.minb)*0.03) as inc from employee  inner join audit_cage  on audit_cage.fk_employee=employee.id  " + 
						"				 inner join do2 on do2.fk_employee=employee.id inner join rank on rank.id=employee.fk_substantive_rank inner join unit on unit.id=employee.fk_present_unit inner join bunching_data   " + 
						"				 on bunching_data.grade_pay=audit_cage.old_grade_pay  inner join t on t.basic_pay=bunching_data.sevencpc_basic_pay   " + 
	//					"				 where fk_status_code<5 and regular_ta_flag='R' and current_record=true and effective=true and audit_cage.record_status='V' and  cancelled=false and audit_cage.grade_pay=0   " +
						"				 where regular_ta_flag='R' and current_record=true and effective=true and audit_cage.record_status='V' and  cancelled=false and audit_cage.grade_pay=0   " +
						"				 and audit_cage.from_date='2016-01-01' and audit_cage.x_group_pay>0  and audit_cage.fk_employee='" +emp.getId()+"'"+ 
						"				 and (bunching_data.max_basic=(audit_cage.old_basic_pay+audit_cage.old_grade_pay))  and t.old_grade_pay=bunching_data.grade_pay and ((t.maxb-t.minb)>((t.minb*0.03)))  " + 
						"				 and do2.description='FIX7PC' and do2.status='V' and do2.from_date='2016-01-01'  " + 
						"				 group by  employee.army_no, rank.rank_name, unit.unit_code, unit.task, audit_cage.fk_employee, audit_cage.basic_pay, audit_Cage.old_basic_pay, audit_cage.old_grade_pay, diff_basic, minb, maxb  " + 
						"				 order by unit.task,unit.unit_code, employee.army_no,  old_grade_pay, basic_pay, old_basic_pay"  
						
						);  
		System.out.println("..................in x group validation");		
			} else {
				jdbcTemplate.execute("drop table if exists t; create temp table t as " + 
						" select distinct(basic_pay),  old_grade_pay, min(old_basic_pay + old_grade_pay) as minb, max(old_basic_pay + old_grade_pay) as maxb from audit_cage join do2 on do2.id=audit_cage.fk_do2 join bunching_data on " + 
						" bunching_data.grade_pay=audit_cage.old_grade_pay where audit_cage.old_grade_pay in (2000, 2400, 2800, 4200, 4600, 4800, 5400) and effective=true and not cancelled " + 
						" and record_status='V' and audit_cage.from_date='2016-01-01' and bunching_data.sevencpc_basic_pay=audit_cage.basic_pay " + 
						" and audit_cage.grade_pay=0 and audit_cage.x_group_pay=0 and do2.description='FIX7PC' and do2.status='V' and do2.from_date='2016-01-01'  " + 
						" group by basic_pay, old_grade_pay order by  old_grade_pay, basic_pay ");	
				 	
				jdbcTemplate.execute("drop table if exists t1; create temp table t1 as  \n" + 
						"				 select employee.army_no, rank.rank_name, unit.unit_code, unit.task, audit_cage.fk_employee, audit_cage.basic_pay, audit_cage.old_basic_pay, audit_cage.old_grade_pay,  " + 
						"				 t.minb,  t.maxb, (t.maxb-t.minb) as td, ((t.minb)*0.03) as inc from employee  inner join audit_cage  on audit_cage.fk_employee=employee.id  " + 
						"				 inner join do2 on do2.fk_employee=employee.id inner join rank on rank.id=employee.fk_substantive_rank inner join unit on unit.id=employee.fk_present_unit inner join bunching_data   " + 
						"				 on bunching_data.grade_pay=audit_cage.old_grade_pay  inner join t on t.basic_pay=bunching_data.sevencpc_basic_pay   " + 
		//				"				 where fk_status_code<5 and regular_ta_flag='R' and current_record=true and effective=true and audit_cage.record_status='V' and  cancelled=false and audit_cage.grade_pay=0   " + 
						"				 where current_record=true and effective=true and audit_cage.record_status='V' and  cancelled=false and audit_cage.grade_pay=0   " + 
						"				 and audit_cage.from_date='2016-01-01' and audit_cage.x_group_pay=0  and audit_cage.fk_employee='" +emp.getId()+"'"+ 
						"				 and (bunching_data.max_basic=(audit_cage.old_basic_pay+audit_cage.old_grade_pay))  and t.old_grade_pay=bunching_data.grade_pay and ((t.maxb-t.minb)>((t.minb*0.03)))  " + 
						"				 and do2.description='FIX7PC' and do2.status='V' and do2.from_date='2016-01-01'  " + 
						"				 group by  employee.army_no, rank.rank_name, unit.unit_code, unit.task, audit_cage.fk_employee, audit_cage.basic_pay, audit_Cage.old_basic_pay, audit_cage.old_grade_pay, diff_basic, minb, maxb  " + 
						"				 order by unit.task,unit.unit_code, employee.army_no,  old_grade_pay, basic_pay, old_basic_pay;"  
						);
				System.out.println("..................in y group validation");  // regular_ta_flag check removed on 23.01.2026 as per PARA TA cases request.
			}
		}
							
		List<Object[]>bunchingList = jdbcTemplate.query("select * from t1 ",new RowMapper<Object[]>() {
			 
			@Override
			public Object[] mapRow(ResultSet rs, int rowNum) throws SQLException {
				// TODO Auto-generated method stub
				Object [] obj=new Object[12];
				obj[0]=rs.getString(1);
				obj[1]=rs.getString(2);
				obj[2]=rs.getString(3);
				obj[3]=rs.getInt(4);
				obj[4]=rs.getLong(5);
				obj[5]=rs.getInt(6);
				obj[6]=rs.getInt(7);
				obj[7]=rs.getInt(8);
				obj[8]=rs.getInt(9);
				obj[9]=rs.getInt(10);
				obj[10]=rs.getInt(11);
				obj[11]=rs.getLong(12);
				return obj;
			}
			 
		 });
 
		if (bunchingList!=null && bunchingList.size()>0) {
			return true;
		}				
		return false;
	}	
}