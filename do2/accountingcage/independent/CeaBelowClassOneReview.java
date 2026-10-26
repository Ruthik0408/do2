package itsdc.dolphin.do2.accountingcage.independent;

 

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import javax.persistence.EntityManager;

import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import itsdc.dolphin.models.Arrears;
import itsdc.dolphin.models.Do2;
import itsdc.dolphin.models.Employee;
import itsdc.dolphin.models.PayCode;
import itsdc.dolphin.utils.DateManager;
import itsdc.dolphin.utils.Utils;
 
 
public class CeaBelowClassOneReview {
		 
	PayCode payCode;
 
	public void checkExcessAdjustment() {
	 
	 //maynot be in use 
		/*em.createNativeQuery("DROP TABLE IF EXISTS ceareviewtemp ").executeUpdate();
		 
		em.createNativeQuery("create temp table ceareviewtemp as " +
		 			" select id, army_no as ano, column_a as cola, substr(column_b,0,5) as colb from do2 where " +
					" description='CEA' and status='V' " +
					" and from_date>'2008-08-31' and (column_a like 'NUR%' or column_a like 'LKG%' or column_a like 'UKG%')" +
					" and (column_a not like 'CL%' and column_a not like 'PRY%' and column_a not like 'SEC%') " +
					" order by ano, colb, from_date, to_date").executeUpdate();*/
		
		List<Object[]> ceaReviewList = null;//em.createNativeQuery("SELECT * FROM ceareviewtemp").getResultList();
		
		int nurseryCount = 0;
		int lkgCount = 0;
		int ukgCount = 0;
		int total = 0;
		String armyNo = "";
		String columnB = "";
		int firstRecordCount = 0;
		
		payCode = null;
		 
		for (Object[] obj : ceaReviewList) {
			if (firstRecordCount==0) {
				armyNo=obj[1].toString();
				columnB=obj[3].toString();
			} 
	 		if (obj[1].toString().equals(armyNo)) {  
	 			if (obj[3].toString().equals(columnB)) {
					if (obj[2].toString().startsWith("NUR")) {
						nurseryCount=1;
					}
					if (obj[2].toString().startsWith("LKG")) {
						lkgCount=1;
					}
					if (obj[2].toString().startsWith("UKG")) {
						ukgCount=1;
					} 
					total=nurseryCount+lkgCount+ukgCount;
					if (total>2) {
		  				//insertRecordInAdjustmentCb(obj,em);
						nurseryCount=0;
						lkgCount=0;
						ukgCount=0;
					}
					columnB="";
				} else {
					nurseryCount=0;
					lkgCount=0;
					ukgCount=0;
					total=0;
					if (obj[2].toString().startsWith("NUR")) {
						nurseryCount=1;
					}
					if (obj[2].toString().startsWith("LKG")) {
						lkgCount=1;
					}
					if (obj[2].toString().startsWith("UKG")) {
						ukgCount=1;
					}
					columnB="";
				} 				
				armyNo="";
			} else {
				nurseryCount=0;
				lkgCount=0;
				ukgCount=0;
				total=0;
				columnB = obj[3].toString();
				if (obj[3].toString().equals(columnB)) {
					if (obj[2].toString().startsWith("NUR")) {
						nurseryCount=1;
					}
					if (obj[2].toString().startsWith("LKG")) {
						lkgCount=1;
					}
					if (obj[2].toString().startsWith("UKG")) {
						ukgCount=1;
					}
				}
				columnB="";
				armyNo="";
			}
	 		firstRecordCount++;
			armyNo = obj[1].toString();
			columnB = obj[3].toString();
		}
		//FacesMessages.instance().add("Review Completed ...................... ");
	}
	
	public void insertRecordInAdjustmentCb(Object[] originalObject, EntityManager em) {
		
		List<Object[]> recoverableList = em.createNativeQuery("select * from ceareviewtemp where ano=:ano " +
				" and cola=:cola and colb=:colb ").setParameter("ano", originalObject[1].toString())
				.setParameter("cola",originalObject[2]).setParameter("colb", originalObject[3].toString()).getResultList();
		
		for (Object[] object : recoverableList)  {
			Object adjustedAmountObject = em.createNativeQuery("select sum(amount) from arrears "
					+ " where fk_do2=:do2Id ").setParameter("do2Id", Integer.parseInt(object[0].toString()))
					.getSingleResult();
			
			Double adjustedAmount=0.0; 
			
			if (adjustedAmountObject!=null) {
				adjustedAmount = Double.parseDouble(adjustedAmountObject.toString());
			}
						
			if (adjustedAmount<=0) {
				continue;
			}
			
			Employee employee = Utils.getCurrentEmployee(originalObject[1].toString());
			Do2 do2 = (Do2) em.createQuery("select d from Do2 d where d.id=:do2Id")
						.setParameter("do2Id", Long.parseLong(object[0].toString())).getSingleResult();
			Arrears arrears = new Arrears();
			arrears.setDo2(do2);
			arrears.setFromDate(do2.getFromDate());
			arrears.setToDate(do2.getToDate());
  			arrears.setAmount((double) Math.round(adjustedAmount*-1));
			arrears.setPayCode(payCode);
			arrears.setQe(Utils.getCurrentQuarter());
			arrears.setRoundOffAmount(false);
			arrears.setRegularPay(false);
			arrears.setPaid(false);
			arrears.setEmployee(employee);
		 	arrears.setArrestPayment(false);
		 	arrears.setCreatedAt(DateManager.getCurrentTimestamp());
		 	em.persist(arrears);
		 	
		/*	System.out.println("*********  arrears recovery inserted .......................... for " 
					 + object[1].toString()+" and the amount is Rs." + adjustedAmount*-1 +"  " );
		*/	do2.setReviewQe(Utils.getCurrentQuarter());
			em.persist(do2);
		}
	}
}


/*
 * try { em.merge(ceaAdj);                                              SAMPLE CODE NOT TO BE DELETED
 * .................. em.flush(); } catch
 * (org.hibernate.validator.InvalidStateException e) {
 * InvalidValue[] invalidValues = e.getInvalidValues();
 * for(InvalidValue invalidValue : invalidValues) {
 * System.out.println("$$$$$$$$$$$$$$$$$$$$$$$  Invalid Value: #0" +
 * invalidValue.getPropertyName()); } }
 */
